import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { createRequire } from 'node:module';
import { join } from 'node:path';

const require = createRequire(import.meta.url);
const root = require('../package.json');
const allowed = new Set([
  'MIT', 'Apache-2.0', 'BSD-2-Clause', 'BSD-3-Clause', 'ISC',
  'BlueOak-1.0.0', 'MPL-2.0', 'Python-2.0', 'Unlicense', '0BSD',
  'MIT AND Apache-2.0', '(MIT OR Apache-2.0)', '(MIT OR CC0-1.0)',
  '(BSD-3-Clause OR GPL-2.0)',
]);
let failed = false;
for (const name of Object.keys(root.dependencies)) {
  const path = require.resolve(`${name}/package.json`);
  const metadata = JSON.parse(readFileSync(path, 'utf8'));
  const license = typeof metadata.license === 'string' ? metadata.license : '';
  if (!allowed.has(license)) { console.error(`${name}: unreviewed direct license ${license || '(missing)'}`); failed = true; }
}
let reviewed = 0;
let dataExceptions = 0;
function scan(directory) {
  if (!existsSync(directory)) return;
  for (const name of readdirSync(directory)) {
    if (name === '.bin') continue;
    const packageDirectory = join(directory, name);
    if (name.startsWith('@')) { scan(packageDirectory); continue; }
    const metadataPath = join(packageDirectory, 'package.json');
    if (!existsSync(metadataPath)) continue;
    const metadata = JSON.parse(readFileSync(metadataPath, 'utf8'));
    const license = typeof metadata.license === 'string' ? metadata.license : '';
    if (metadata.name === 'caniuse-lite' && license === 'CC-BY-4.0') {
      // Browser support data used by build tooling, not a software library.
      dataExceptions += 1;
    } else if (!allowed.has(license)) {
      console.error(`${metadata.name}: unreviewed transitive license ${license || '(missing)'}`);
      failed = true;
    }
    reviewed += 1;
    scan(join(packageDirectory, 'node_modules'));
  }
}
scan('node_modules');
if (failed) process.exitCode = 1;
else console.log(`Checked ${Object.keys(root.dependencies).length} direct and ${reviewed} installed package licenses; ${dataExceptions} CC-BY browser-data exception.`);
