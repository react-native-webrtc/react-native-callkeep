#!/usr/bin/env node
// Checks that the relative links and images in the repository's Markdown files point to files that
// exist, and that their #anchors match a heading of the target file, the way GitHub builds them.
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');

const root = path.join(__dirname, '..');
const files = execFileSync('git', ['ls-files', '--cached', '--others', '--exclude-standard', '*.md'], { cwd: root, encoding: 'utf8' })
  .split('\n').filter((file) => file && fs.existsSync(path.join(root, file)));

const withoutFences = (text) => text.replace(/^[ \t]*(```|~~~)[\s\S]*?^[ \t]*\1/gm, '');
const withoutCode = (text) => withoutFences(text).replace(/`[^`\n]*`/g, '');

// GitHub's heading anchors: lowercase, punctuation removed, spaces turned into hyphens, and -1, -2...
// for repeated headings. HTML anchors (`<a name="...">`, `id="..."`) count too.
const anchorsCache = new Map();
const anchors = (file) => {
  if (!anchorsCache.has(file)) {
    const text = fs.readFileSync(file, 'utf8');
    const found = new Set([...text.matchAll(/<a\s+(?:name|id)="([^"]+)"/g)].map((m) => m[1]));
    const seen = new Map();
    for (const [, heading] of withoutFences(text).matchAll(/^#{1,6}\s+(.+?)\s*#*$/gm)) {
      const slug = heading
        .replace(/\[([^\]]*)\]\([^)]*\)/g, '$1')
        .replace(/<[^>]+>/g, '')
        .toLowerCase()
        .replace(/[^\p{L}\p{M}\p{N}_\- ]/gu, '')
        .replace(/ /g, '-');
      const count = seen.get(slug) || 0;
      found.add(count ? `${slug}-${count}` : slug);
      seen.set(slug, count + 1);
    }
    anchorsCache.set(file, found);
  }
  return anchorsCache.get(file);
};

const errors = [];
let checked = 0;
for (const file of files) {
  const text = withoutCode(fs.readFileSync(path.join(root, file), 'utf8'));
  const targets = [
    ...[...text.matchAll(/\]\(<?([^)\s>]+)>?(?:\s+"[^"]*")?\)/g)].map((m) => m[1]),
    ...[...text.matchAll(/^\[[^\]]+\]:\s*(\S+)/gm)].map((m) => m[1]),
    ...[...text.matchAll(/\s(?:src|href)="([^"]+)"/g)].map((m) => m[1]),
  ];
  for (const target of targets) {
    if (/^[a-z][a-z0-9+.-]*:/i.test(target) || target.startsWith('//')) continue;
    checked += 1;
    const [link, anchor] = target.split('#');
    const dest = link ? path.resolve(path.dirname(path.join(root, file)), decodeURIComponent(link)) : path.join(root, file);
    if (path.relative(root, dest).startsWith('..')) {
      errors.push(`${file}: ${target} points outside the repository`);
    } else if (!fs.existsSync(dest)) {
      errors.push(`${file}: ${target} does not exist`);
    } else if (anchor && dest.endsWith('.md') && !anchors(dest).has(anchor)) {
      errors.push(`${file}: ${target} has no heading for #${anchor}`);
    }
  }
}

if (errors.length) {
  console.error(errors.join('\n'));
  process.exit(1);
}
console.log(`${checked} relative links resolve in ${files.length} Markdown files`);
