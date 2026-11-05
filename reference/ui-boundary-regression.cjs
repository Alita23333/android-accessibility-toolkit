const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { EventEmitter } = require('node:events');
const runtime = path.resolve(__dirname, '../../app/src/main/assets/zcode-runtime');
const tree = require(path.join(runtime, 'ui-tree.cjs'));
const raw = { nodes: [{ cls: 'android.widget.FrameLayout', children: [
  { cls: 'android.widget.LinearLayout', children: [
    { cls: 'android.widget.TextView', t: 'hidden', vis: false },
    { cls: 'android.widget.Button', t: 'Increment', cl: true },
    { cls: 'android.widget.EditText', t: 'entry', editable: true },
  ] },
] }] };
const rendered = tree.renderSnapshot(raw, {});
assert.equal(rendered.nodes.find(n => n.text === 'Increment').id, 2);
assert.equal(rendered.nodeTargets.find(n => n.id === 2).rawIndex, 4);
assert.equal(rendered.nodeTargets.find(n => n.id === 3).rawIndex, 5);
assert.equal(JSON.stringify(rendered.nodes).includes('rawIndex'), false);
const continuation = tree.renderSnapshot(raw, { from: '#3' });
assert.equal(continuation.nodes[0].id, 3);
assert.equal(continuation.nodeTargets.find(n => n.id === 3).rawIndex, 5);
const bootstrap = fs.readFileSync(path.join(runtime, 'bootstrap.cjs'), 'utf8');
const start = bootstrap.indexOf('if (url.pathname === "/carrier/next")');
const end = bootstrap.indexOf('if (url.pathname === "/carrier/respond"', start);
const handler = `(function () { ${bootstrap.slice(start, end)} })()`;
const state = {};
const timers = new Map();
let nextTimer = 0;
const deliveries = [];
const invoke = (res) => vm.runInNewContext(handler, {
  url: { pathname: '/carrier/next' }, state, res,
  send: (code, body) => deliveries.push({ res, body }),
  setTimeout: (callback) => { const id = ++nextTimer; timers.set(id, callback); return id; },
  clearTimeout: (id) => timers.delete(id),
});
const old = new EventEmitter();
invoke(old);
assert.equal(timers.size, 1);
old.destroyed = true;
old.emit('close');
assert.equal(timers.size, 0);
state.carrierQueue = [{ callId: 'live-call' }];
const live = new EventEmitter();
invoke(live);
assert.equal(deliveries.length, 1);
assert.equal(deliveries[0].res, live);
assert.equal(deliveries[0].body.callId, 'live-call');
console.log('PASS: normalized ids survive wrapper collapse, hidden subtree and pagination; disconnected poll cannot consume next call');
