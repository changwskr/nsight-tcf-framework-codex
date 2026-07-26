/**
 * tcf-ui static 파일 중 CP949(EUC-KR)로 저장된 HTML/JS를 UTF-8로 변환합니다.
 * 사용: node tcf-ui/scripts/fix-cp949-to-utf8.mjs
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import iconv from 'iconv-lite';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.join(__dirname, '../src/main/resources/static');

function walk(dir, out = []) {
  for (const name of fs.readdirSync(dir)) {
    const full = path.join(dir, name);
    const st = fs.statSync(full);
    if (st.isDirectory()) walk(full, out);
    else if (/\.(html|js|css)$/i.test(name)) out.push(full);
  }
  return out;
}

function hangulCount(text) {
  return (text.match(/[\uAC00-\uD7A3]/g) || []).length;
}

function shouldConvert(buf) {
  const utf8 = buf.toString('utf8');
  const cp949 = iconv.decode(buf, 'cp949');
  const hUtf8 = hangulCount(utf8);
  const hCp949 = hangulCount(cp949);
  const hasRepl = utf8.includes('\uFFFD');
  if (hCp949 === 0 && !hasRepl) return false;
  if (hUtf8 > hCp949 && !hasRepl) return false;
  return hCp949 >= hUtf8 || hasRepl;
}

const files = walk(root);
let converted = 0;
for (const file of files) {
  const buf = fs.readFileSync(file);
  if (!shouldConvert(buf)) continue;
  const text = iconv.decode(buf, 'cp949');
  fs.writeFileSync(file, text, 'utf8');
  converted++;
  console.log('UTF-8 converted:', path.relative(root, file));
}
console.log(`Done. ${converted}/${files.length} file(s) converted.`);
