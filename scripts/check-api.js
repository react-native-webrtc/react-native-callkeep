#!/usr/bin/env node
// Checks that every public method of the default export is declared the same way in index.js,
// index.d.ts and the methods table of docs/api.md, and that every method section of docs/api.md
// is about a method that exists. Methods marked `// @deprecated` in index.js are left out.
const fs = require('fs');
const path = require('path');

const root = path.join(__dirname, '..');
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8');
const source = read('index.js');
const typings = read('index.d.ts');
const apiDoc = read('docs/api.md');

const block = (text, start, file) => {
  const match = text.match(start);
  if (!match) throw new Error(`${file}: block not found (${start})`);
  return match[1];
};

// index.js: the members of `class RNCallKeep`, either `name = (...) =>` or `name() {`.
const classBody = block(source, /\nclass RNCallKeep \{([\s\S]*?)\n\}/, 'index.js');
const implemented = [...classBody.matchAll(/(\/\/ @deprecated\n)?^  (?:async )?(\w+)\s*(?:=|\()/gm)]
  .filter((m) => !m[1] && m[2] !== 'constructor' && !m[2].startsWith('_'))
  .map((m) => m[2]);

// index.d.ts: the `static` members of `export default class RNCallKeep`.
const typed = [...block(typings, /export default class RNCallKeep \{([\s\S]*?)\n  \}/, 'index.d.ts')
  .matchAll(/^\s*static (\w+)/gm)].map((m) => m[1]);

// docs/api.md: rows of the methods table, and the `### name` sections before `## Constants`.
const table = block(apiDoc, /\n(\| Method[\s\S]*?)\n\n/, 'docs/api.md');
const documented = [...table.matchAll(/^\| \[`(\w+)\(\)`\]/gm)].map((m) => m[1]);
const sections = [...block(apiDoc, /\n## Methods\n([\s\S]*?)\n## /, 'docs/api.md')
  .matchAll(/^### (\w+)$/gm)].map((m) => m[1]);

const errors = [];
for (const method of implemented) {
  if (!typed.includes(method)) errors.push(`index.d.ts: RNCallKeep has no '${method}' (in index.js)`);
  if (!documented.includes(method)) errors.push(`docs/api.md: the methods table has no '${method}' (in index.js)`);
}
for (const method of typed) {
  if (!implemented.includes(method)) errors.push(`index.js: RNCallKeep has no '${method}' (declared in index.d.ts)`);
}
for (const method of documented) {
  if (!implemented.includes(method)) errors.push(`docs/api.md: '${method}' is in the methods table but index.js does not have it`);
}
for (const method of sections) {
  if (!implemented.includes(method)) errors.push(`docs/api.md: section '${method}' is about a method index.js does not have`);
}
for (const method of new Set(documented.filter((m, i) => documented.indexOf(m) !== i))) {
  errors.push(`docs/api.md: '${method}' is in the methods table twice`);
}

if (errors.length) {
  console.error(errors.join('\n'));
  process.exit(1);
}
console.log(`${implemented.length} methods consistent across index.js, index.d.ts and docs/api.md`);
