param([string]$GuideDir, [string]$OutDir)
Add-Type -AssemblyName System.IO.Compression.FileSystem
if (-not (Test-Path $OutDir)) { New-Item -ItemType Directory -Path $OutDir | Out-Null }

function Get-DocxText([string]$path) {
    $zip = [System.IO.Compression.ZipFile]::OpenRead($path)
    try {
        $entry = $zip.Entries | Where-Object { $_.FullName -eq 'word/document.xml' } | Select-Object -First 1
        $sr = New-Object System.IO.StreamReader($entry.Open())
        try { $xml = $sr.ReadToEnd() } finally { $sr.Close() }
    } finally { $zip.Dispose() }
    $text = $xml -replace '</w:p>', "`n" -replace '<[^>]+>', ''
    $text -replace '&amp;','&' -replace '&lt;','<' -replace '&gt;','>'
}

function Write-TextFile([string]$outPath, [string]$text) {
    [System.IO.File]::WriteAllText($outPath, $text, [System.Text.UTF8Encoding]::new($false))
}

$main = @{}
$naming = @{}

Get-ChildItem (Join-Path $GuideDir '*.docx') | ForEach-Object {
    if ($_.Name -notmatch '\((\d+)\)\.docx$') { return }
    $n = [int]$Matches[1]
    if ($_.Name -match '명명규칙') {
        $naming[$n] = $_
    } elseif ($_.Name -match '통합') {
        $main[$n] = $_
    }
}

foreach ($kv in ($main.GetEnumerator() | Sort-Object { [int]$_.Key })) {
    $n = [int]$kv.Key
    $text = Get-DocxText $kv.Value.FullName
    $out = Join-Path $OutDir ("docx-{0}.txt" -f $n)
    Write-TextFile $out $text
    Write-Host ("Extracted docx-{0}.txt from 통합 ({0}) ({1} bytes)" -f $n, $text.Length)
}

foreach ($kv in ($naming.GetEnumerator() | Sort-Object { [int]$_.Key })) {
    $n = [int]$kv.Key
    $text = Get-DocxText $kv.Value.FullName
    $out = Join-Path $OutDir ("naming-{0}.txt" -f $n)
    Write-TextFile $out $text
    Write-Host ("Extracted naming-{0}.txt from 명명규칙 ({0}) ({1} bytes)" -f $n, $text.Length)
}
