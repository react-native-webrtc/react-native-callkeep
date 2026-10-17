#!/usr/bin/env node
// Checks that every event is declared the same way in actions.js, index.d.ts and the README
// events table: same JS names, and the native event names typed in index.d.ts are the ones
// actions.js listens to.
const fs = require('fs');
const path = require('path');

const root = path.join(__dirname, '..');
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8');
const actions = read('actions.js');
const typings = read('index.d.ts');
const readme = read('README.md');

const block = (source, start, file) => {
  const match = source.match(start);
  if (!match) throw new Error(`${file}: block not found (${start})`);
  return match[1];
};

// actions.js: `const RNCallKeepX = 'RNCallKeepX';` and the exported `listeners` object.
const nativeNames = new Set([...actions.matchAll(/^const \w+ = ['"](\w+)['"];/gm)].map((m) => m[1]));
const listeners = block(actions, /export const listeners = \{([\s\S]*?)\};/, 'actions.js')
  .split(',').map((name) => name.trim()).filter(Boolean);

// index.d.ts: `export type NativeEvents = { jsName: 'NativeName'; ... }`.
const typed = [...block(typings, /export type NativeEvents = \{([\s\S]*?)\n\s*\}/, 'index.d.ts')
  .matchAll(/(\w+)\s*:\s*['"](\w+)['"]/g)].map((m) => ({ event: m[1], native: m[2] }));

// README: rows of the table under "## Events".
const table = block(readme, /\n## Events\n([\s\S]*?)\n#/, 'README.md');
const documented = [...table.matchAll(/^\| \[(\w+)\]/gm)].map((m) => m[1]);

const errors = [];
const typedEvents = new Set(typed.map((t) => t.event));
for (const { event, native } of typed) {
  if (!nativeNames.has(native)) errors.push(`index.d.ts: NativeEvents.${event} is '${native}', which actions.js never listens to`);
}
for (const event of listeners) {
  if (!typedEvents.has(event)) errors.push(`index.d.ts: NativeEvents has no '${event}' (exported by actions.js)`);
  if (!documented.includes(event)) errors.push(`README.md: the events table has no '${event}' (exported by actions.js)`);
}
for (const event of typedEvents) {
  if (!listeners.includes(event)) errors.push(`actions.js: listeners has no '${event}' (declared in index.d.ts)`);
}
for (const event of documented) {
  if (!listeners.includes(event)) errors.push(`README.md: '${event}' is in the events table but actions.js does not export it`);
}

if (errors.length) {
  console.error(errors.join('\n'));
  process.exit(1);
}
console.log(`${listeners.length} events consistent across actions.js, index.d.ts and README.md`);
