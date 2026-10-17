#!/usr/bin/env node
// Checks what `npm publish` would ship: the files apps need are in, development files are out.
const { execFileSync } = require('child_process');
const path = require('path');

const root = path.join(__dirname, '..');
const [pack] = JSON.parse(execFileSync('npm', ['pack', '--dry-run', '--json', '--ignore-scripts'], { cwd: root, encoding: 'utf8' }));
const files = pack.files.map((file) => file.path);

const required = [
  'index.js',
  'actions.js',
  'index.d.ts',
  'package.json',
  'RNCallKeep.podspec',
  'ios/RNCallKeep/RNCallKeep.h',
  'ios/RNCallKeep/RNCallKeep.m',
  'ios/PrivacyInfo.xcprivacy',
  'android/build.gradle',
  'android/src/main/AndroidManifest.xml',
  'android/src/main/java/io/wazo/callkeep/RNCallKeepModule.java',
];
const excluded = ['example/', 'tests/', 'scripts/', 'docs/', '.github/'];
// Files that ended up in 4.4.0 by mistake: IDE project files and the release notes the workflow writes.
const excludedFiles = [/\.iml$/, /^notes\.md$/];

const errors = [
  ...required.filter((file) => !files.includes(file)).map((file) => `missing from the package: ${file}`),
  ...files.filter((file) => excluded.some((dir) => file.startsWith(dir)) || excludedFiles.some((pattern) => pattern.test(file)))
    .map((file) => `should not be published: ${file}`),
];
if (errors.length) {
  console.error(errors.join('\n'));
  process.exit(1);
}
console.log(`${pack.name}@${pack.version}: ${files.length} files, ${(pack.size / 1024).toFixed(0)} kB packed`);
