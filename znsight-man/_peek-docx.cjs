#!/usr/bin/env node
'use strict';
const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const guideDir = path.join(__dirname, '..', 'znsight-guide');
const nums = [1, 2, 6, 7, 11, 40, 45, 50, 57, 60, 63, 66, 69, 73, 75, 78];

const ps = `
Add-Type -AssemblyName System.IO.Compression.FileSystem
function Get-DocxText([string]$path) {
  $zip = [System.IO.Compression.ZipFile]::OpenRead($path)
  try {
    $entry = $zip.Entries | Where-Object { $_.FullName -eq 'word/document.xml' } | Select-Object -First 1
    $sr = New-Object System.IO.StreamReader($entry.Open())
    try { $xml = $sr.ReadToEnd() } finally { $sr.Close() }
  } finally { $zip.Dispose() }
  ($xml -replace '</w:p>', [char]10 -replace '<[^>]+>', '').Trim()
}
$guide = '${guideDir.replace(/\\/g, '\\\\')}'
${nums.map((n) => {
  const glob = `*통합 (${n}).docx`;
  return `$f = Get-ChildItem $guide -Filter '${glob}' | Select-Object -First 1
if ($f) {
  $t = Get-DocxText $f.FullName
  $first = ($t -split "\\n" | Where-Object { $_.Trim() } | Select-Object -First 1)
  Write-Output "MAIN_${n}|$($f.Length)|$first"
}`;
}).join('\n')}
`;

const out = execSync(`powershell -NoProfile -Command "${ps.replace(/"/g, '\\"').replace(/\n/g, '; ')}"`, {
  encoding: 'utf8',
  maxBuffer: 10 * 1024 * 1024,
});
console.log(out);
