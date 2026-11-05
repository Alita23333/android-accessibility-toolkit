const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { EventEmitter } = require('node:events');
const runtime = path.resolve(__dirname, '../runtime');
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

console.log('PASS original raw/normalized node mapping and continuation assertions');
