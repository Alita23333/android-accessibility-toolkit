// ui-tree.cjs — Phase 8 track 1 (runbook §3): semantic-tree normalization and
// rendering. The Kotlin walk produces a compact raw node list (§3.1); THIS
// module owns every rule the model-facing output obeys (N1–N12, §2.4 grammar,
// §3.4 budgets, the role map, selector matching). The raw list never reaches
// the model; with files/uilab.enabled it is dumped to the workspace instead.
//
// Statelessness: generations/frame-cache/stale matching live in the Kotlin
// worker (W1/W4); this module is a pure function of (rawSnapshot, options).

const UI_DUMP_MAX_NODES = 300;        // emitted nodes per ui_dump (default = cap)
const UI_DUMP_MAX_BYTES = 20480;      // 20 KiB rendered text + structured envelope
const UI_LABEL_MAX_CHARS = 80;
const UI_LABEL_BUDGET_BYTES = 8192;   // all labels in one snapshot
const UI_SNAPSHOT_MAX_NODES = 60;     // compact snapshot inside action results
const UI_SNAPSHOT_MAX_BYTES = 4096;

// §3.2 N5 role map — closed set, first matching suffix wins, else Other.
const ROLE_SUFFIX_MAP = [
  ["Button", ["button", "imagebutton", "materialbutton", "appcompatbutton"]],
  ["Switch", ["switch", "switchcompat", "togglebutton", "switchpreference"]],
  ["Check", ["checkbox", "checkedtextview", "checkboxpreference"]],
  ["Edit", ["edittext", "textinputedittext", "autocompletetextview", "searchview"]],
  ["Seek", ["seekbar", "progressbar", "slider"]],
  ["Image", ["imageview", "image", "icon"]],
  ["Text", ["textview", "text"]],
  ["Tab", ["tabwidget", "tablayout", "tab"]],
  ["Web", ["webview"]],
  ["List", ["recyclerview", "listview", "scrollview", "nestedscrollview", "horizontalscrollview"]],
];
function roleOf(cls, clickable, hasLabel) {
  if (!cls) return "Other";
  const short = String(cls).split(".").pop().toLowerCase();
  for (const [role, suffixes] of ROLE_SUFFIX_MAP) {
    if (suffixes.some((s) => short === s || short.endsWith(s))) return role;
  }
  // N5 Row: a clickable, label-bearing plain container reads as a row
  if (hasLabel && clickable &&
      ["linearlayout", "relativelayout", "framelayout", "preference", "constraintlayout"].includes(short)) {
    return "Row";
  }
  if (short === "preference") return "Row";
  return "Other";
}

// N6 label rules: precedence text > cd > stateDescription > hint; whitespace
// collapsed; escaping; control chars stripped; 80-char truncation; pwd masked.
function labelOf(n) {
  if (n.pwd) return "•••";
  const raw = n.t || n.cd || n.sd || n.hint || "";
  let s = String(raw)
    .replace(/[\u0000-\u001f\u007f-\u009f\ufffe\uffff]/g, "")
    .replace(/\s+/g, " ")
    .trim();
  s = s.replace(/\\/g, "\\\\").replace(/"/g, '\\"');
  if (s.includes("\n")) s = s.replace(/\n/g, "⏎");
  if (s.length > UI_LABEL_MAX_CHARS) s = s.slice(0, UI_LABEL_MAX_CHARS) + "…+" + (s.length - UI_LABEL_MAX_CHARS);
  return s;
}

function fmtBounds(b) {
  if (!Array.isArray(b) || b.length !== 4) return "[0,0][0,0]";
  return `[${b[0]},${b[1]}][${b[2]},${b[3]}]`;
}

// ---- normalization walk (N1 visibility, N2/N3 wrapper collapse) -------------
// Emits flat DFS-ordered normalized nodes with per-node emitted ids (#idx).
function normalize(raw) {
  const out = [];
  const state = { labelBytes: 0, omitted: 0, rawCount: raw.nodes ? raw.nodes.length : 0 };
  const app = raw.app || (raw.window && raw.window.pkg) || "";
  // Private raw preorder identity survives wrapper collapse and pruning.
  const rawIndices = new WeakMap();
  let rawIndex = 0;
  const indexRaw = (n) => { if (!n) return; rawIndices.set(n, ++rawIndex); for (const c of n.children || []) indexRaw(c); };
  for (const n of raw.nodes || []) indexRaw(n);

  const walk = (n) => {
    if (!n) return false; // whether this node (or a descendant) was emitted
    if (n.vis === false) { state.omitted++; return false; }       // N1 subtree prune
    const label = labelOf(n);
    const role = roleOf(n.cls, !!n.cl, label !== "");
    const actionable = !!(n.cl || n.lc || n.chkable || n.editable || n.scroll || n.sel || n.foc);
    const semanticContainer = !!(n.coll || n.head || n.pane);
    // N2 wrapper collapse: elide (re-parent children) when the node carries no
    // semantics at all. The root is never elided.
    if (!n.root && label === "" && !actionable && !semanticContainer) {
      let any = false;
      for (const c of (n.children || [])) { if (walk(c)) any = true; }
      if (!any) state.omitted++;
      return any;
    }
    // N7 text sub-budget: structure always survives a chatty screen
    let labelOut = label;
    if (state.labelBytes + label.length > UI_LABEL_BUDGET_BYTES && label.length > 24) {
      labelOut = label.slice(0, 24) + "…";
    }
    state.labelBytes += labelOut.length;
    const node = {
      role, text: labelOut,
      id: n.vid ? String(n.vid).split(":id/").pop() : undefined,
      // structuredContent dialect (N9) — nothing else may leave
      ...(n.pwd ? { pwd: true } : {}),
      ...(labelOut === "" ? {} : {}),
    };
    node.checked = n.chk === true ? true : undefined;
    node.selected = n.sel === true ? true : undefined;
    node.enabled = n.en === false ? false : undefined;
    node.clickable = n.cl === true ? true : undefined;
    node.editable = n.editable === true ? true : undefined;
    node.scrollable = n.scroll === true ? true : undefined;
    node.bounds = n.b || [0, 0, 0, 0];
    // carry the raw key for the worker's fingerprint/stale matching (host-side
    // only; stripped before the block is rendered — kept in structured nodes)
    node._key = { cls: n.cls || "", vid: n.vid || null, label, b: n.b || null, cc: (n.children || []).length };
    node._heading = !!n.head;
    node._rawIndex = n._rawIndex || rawIndices.get(n);
    node._pkg = n.pkg || null;
    node._children = [];
    const entry = { node, children: node._children };
    out.push(entry);
    for (const c of (n.children || [])) walk(c);
    return true;
  };

  // the raw list is a tree whose first entry is the window root; tolerate
  // extra top-level entries (multi-root capture) by walking them all
  const list = raw.nodes || [];
  for (let i = 0; i < list.length; i++) walk({ ...list[i], root: i === 0, _rawIndex: rawIndices.get(list[i]) });
  return { entries: out, state, app };
}

// ---- rendering (§2.4 grammar) -----------------------------------------------
function renderSnapshot(raw, opts) {
  opts = opts || {};
  const maxNodes = Math.min(opts.maxNodes || UI_DUMP_MAX_NODES, UI_DUMP_MAX_NODES); // A14 clamp
  const includeBounds = opts.includeBounds !== false;
  const { entries, state, app } = normalize(raw);

  const lines = [];
  const w = raw.window || {};
  lines.push(`Snapshot: g${raw.generation}  ${raw.renderedAt || new Date().toISOString()}  age=${raw.ageMs || 0}ms`);
  lines.push(`App: ${app || "(unknown)"}`);
  const screenBits = [raw.screen || "(unknown)"];
  if (w.title) screenBits[0] += `  title="${w.title}"`;
  if (raw.screenStale) screenBits[0] += "  (stale-event)";
  lines.push(`Screen: ${screenBits[0]}`);
  if (w.id !== undefined) {
    lines.push(`Win: #${w.id} type=${w.type || "application"} active=${w.active !== false}${w.focused !== undefined ? " focused=" + w.focused : ""} layer=${w.layer ?? 0} bounds=${fmtBounds(w.bounds)}`);
  }

  let bytes = lines.join("\n").length;
  let kept = 0;
  let truncated = false;
  const fromIdx = opts.from ? parseInt(String(opts.from).replace("#", "").split(":").pop(), 10) || 1 : 1;
  const nodesOut = [];
  let labelBudget = state.labelBytes;

  for (let i = fromIdx - 1; i < entries.length; i++) {
    const { node } = entries[i];
    if (kept >= maxNodes) { truncated = true; break; }
    const idx = i + 1; // consecutive DFS index of emitted nodes, from 1
    let line = `#${idx} ${node.role} "${node.text}"`;
    if (node.id) line += ` id=${node.id}`;
    if (node.checked) line += " checked";
    if (node.selected) line += " sel";
    if (node.focused) line += " foc";
    if (node.enabled === false) line += " dis";
    if (node.editable) line += " edit";
    if (node.scrollable) line += " scroll";
    if (node.clickable) line += " clickable";
    if (node._heading) line += " h";
    if (node.pwd) line += " pwd";
    if (node._pkg && node._pkg !== app) line += ` pkg=${node._pkg}`;
    if (includeBounds) line += ` ${fmtBounds(node.bounds)}`;
    if (bytes + line.length + 1 > UI_DUMP_MAX_BYTES) { truncated = true; break; }
    lines.push(line);
    bytes += line.length + 1;
    kept++;
    // structured dialect per N9
    const snode = { id: idx, role: node.role };
    if (node.text) snode.text = node.text;
    if (node.id) snode.viewId = node.id;
    if (node.checked) snode.checked = true;
    if (node.selected) snode.selected = true;
    if (node.enabled === false) snode.enabled = false;
    if (node.clickable) snode.clickable = true;
    if (node.editable) snode.editable = true;
    if (node.scrollable) snode.scrollable = true;
    snode.bounds = node.bounds;
    nodesOut.push(snode);
  }

  if (truncated) {
    lines.push(`… TRUNCATED after ${kept}/${entries.length} nodes at #${kept + fromIdx - 1} (use ui_dump {from:"#${kept + fromIdx}"})`);
  }
  if (nodesOut.length === 0 && !truncated) {
    lines.push("(no visible nodes)");
  }

  return {
    text: lines.join("\n"),
    nodes: nodesOut,
    nodeTargets: entries.map(({ node }, i) => ({ id: i + 1, rawIndex: node._rawIndex })),
    bytes,
    nodeCount: nodesOut.length,
    totalNodes: entries.length,
    truncated,
    omitted: state.omitted,
    rawCount: state.rawCount,
    truncatedRaw: !!raw.truncatedRaw,
    // A14 accept-and-clamp: only a value ABOVE the clamp (and within the schema
    // max, enforced by the caller) reports clamped:true — a value outside the
    // schema maximum is ui_invalid_input upstream of this module.
    clamped: Number.isInteger(opts.maxNodes) && opts.maxNodes > UI_DUMP_MAX_NODES,
    clampedTo: Number.isInteger(opts.maxNodes) && opts.maxNodes > UI_DUMP_MAX_NODES ? UI_DUMP_MAX_NODES : undefined,
  };
}

// ---- compact snapshot for action results (§3.4 UI_SNAPSHOT_*) ---------------
function compactSnapshot(full) {
  const capped = full.nodes.slice(0, UI_SNAPSHOT_MAX_NODES);
  let bytes = 0;
  const out = [];
  for (const n of capped) {
    const s = JSON.stringify(n);
    if (bytes + s.length > UI_SNAPSHOT_MAX_BYTES) break;
    bytes += s.length;
    out.push(n);
  }
  return out;
}

// ---- selector matching (ui_find; pure over the rendered nodes) --------------
function matchesSelector(n, sel) {
  if (!sel || typeof sel !== "object") return false;
  if (sel.text !== undefined) {
    const t = String(sel.text).toLowerCase();
    const hay = `${n.text || ""}`.toLowerCase();
    if (!hay.includes(t)) return false;
  }
  if (sel.viewId !== undefined) {
    const want = String(sel.viewId).includes(":id/")
      ? String(sel.viewId).split(":id/").pop() : String(sel.viewId);
    if ((n.viewId || "") !== want) return false;
  }
  if (sel.role !== undefined && n.role !== sel.role) return false;
  if (sel.clickable !== undefined && !!n.clickable !== !!sel.clickable) return false;
  if (sel.editable !== undefined && !!n.editable !== !!sel.editable) return false;
  return true;
}

function findNodes(nodes, sel) {
  return nodes.filter((n) => matchesSelector(n, sel));
}

module.exports = {
  renderSnapshot, compactSnapshot, findNodes, matchesSelector, roleOf, labelOf,
  UI_DUMP_MAX_NODES, UI_DUMP_MAX_BYTES, UI_LABEL_MAX_CHARS, UI_LABEL_BUDGET_BYTES,
  UI_SNAPSHOT_MAX_NODES, UI_SNAPSHOT_MAX_BYTES,
};
