# Markdown -> Word docx (Naming 21 Header) - ASCII-only PS1; Korean via env vars
param(
    [string]$MdPath = '',
    [string]$OutPath = ''
)

$ErrorActionPreference = 'Stop'
$scriptDir = $PSScriptRoot
$repoRoot = Split-Path $scriptDir -Parent

if (-not $MdPath) {
    $MdPath = (Get-ChildItem -LiteralPath $scriptDir -Filter '*-21-Header-*.md' | Select-Object -First 1).FullName
}
if (-not $OutPath) {
    $OutPath = $env:OUT_PATH
}
if (-not $MdPath -or -not (Test-Path -LiteralPath $MdPath)) {
    throw "Markdown source not found"
}
if (-not $OutPath) {
    throw "Output path not set"
}

$docTitle = $env:DOC_TITLE
if (-not $docTitle) { $docTitle = 'Header Field Naming Standard' }

function Add-Paragraph($doc, $text, [int]$fontSize = 11, [bool]$bold = $false) {
    $range = $doc.Content
    $range.Collapse(0)
    $range.Text = $text + [char]13
    $range.Font.Name = 'Malgun Gothic'
    $range.Font.Size = $fontSize
    $range.Font.Bold = [int]$bold
    $range.ParagraphFormat.SpaceAfter = 6
}

function Add-CodeBlock($doc, [string[]]$lines) {
    $text = ($lines -join [char]13) + [char]13
    $range = $doc.Content
    $range.Collapse(0)
    $range.Text = $text
    $range.Font.Name = 'Consolas'
    $range.Font.Size = 9
    $range.ParagraphFormat.SpaceAfter = 6
    $range.ParagraphFormat.LeftIndent = 18
}

function Parse-MdTable($lines, [ref]$idx) {
    $headerLine = $lines[$idx.Value].Trim()
    if ($headerLine -notmatch '^\|') { return $null }
    $idx.Value++
    if ($lines[$idx.Value].Trim() -notmatch '^\|\s*[-:]') { return $null }
    $idx.Value++
    $headers = ($headerLine.Trim('|') -split '\|') | ForEach-Object { $_.Trim() } | Where-Object { $_ -and $_ -ne '---' }
    $rows = @()
    while ($idx.Value -lt $lines.Count) {
        $line = $lines[$idx.Value].Trim()
        if ($line -notmatch '^\|') { break }
        $cells = ($line.Trim('|') -split '\|') | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne '---' }
        if ($cells.Count -ge 1) {
            while ($cells.Count -lt $headers.Count) { $cells += '' }
            if ($cells.Count -gt $headers.Count) { $cells = $cells[0..($headers.Count - 1)] }
            $rows += ,@($cells)
        }
        $idx.Value++
    }
    return @{ Headers = $headers; Rows = $rows }
}

function Add-WordTable($doc, $tableData) {
    $headers = $tableData.Headers
    $rows = $tableData.Rows
    $colCount = $headers.Count
    $rowCount = $rows.Count + 1
    $range = $doc.Content
    $range.Collapse(0)
    $table = $doc.Tables.Add($range, $rowCount, $colCount)
    try { $table.Style = -41 } catch { }
    for ($c = 0; $c -lt $colCount; $c++) {
        $cell = $table.Cell(1, $c + 1)
        $cell.Range.Text = $headers[$c]
        $cell.Range.Font.Bold = 1
        $cell.Range.Font.Name = 'Malgun Gothic'
        $cell.Range.Font.Size = 10
    }
    for ($r = 0; $r -lt $rows.Count; $r++) {
        for ($c = 0; $c -lt $colCount; $c++) {
            $cell = $table.Cell($r + 2, $c + 1)
            $cell.Range.Text = $rows[$r][$c]
            $cell.Range.Font.Name = 'Malgun Gothic'
            $cell.Range.Font.Size = 10
        }
    }
    $doc.Content.InsertParagraphAfter() | Out-Null
}

function Should-SkipLine([string]$t) {
    if ($t -match '^>\s') { return $true }
    if ($t -match '^#\s') { return $true }
    if ($t -eq '---') { return $true }
    if ($t -match '^## [^0-9]') { return $true }
    if ($t -match 'znsight-guide-word') { return $true }
    if ($t -match '_naming-21-header-body') { return $true }
    if ($t -match '^\[21') { return $true }
    if ($t -match '^\*\*NSIGHT TCF') { return $true }
    if ($t -match 'docx \(') { return $true }
    return $false
}

$content = [System.IO.File]::ReadAllText($MdPath, [System.Text.Encoding]::UTF8)
$lines = $content -split "`r?`n"
$startIdx = 0
for ($i = 0; $i -lt $lines.Count; $i++) {
    if ($lines[$i].Trim() -eq $docTitle) { $startIdx = $i; break }
}
$endIdx = $lines.Count - 1
for ($i = $startIdx; $i -lt $lines.Count; $i++) {
    if ($lines[$i].Trim() -eq '---' -and $i -gt ($startIdx + 50)) { $endIdx = $i - 1; break }
}
$bodyLines = $lines[$startIdx..$endIdx]

$word = New-Object -ComObject Word.Application
$word.Visible = $false
$doc = $word.Documents.Add()
try {
    Add-Paragraph $doc $docTitle 16 $true
    $i = 1
    while ($i -lt $bodyLines.Count) {
        $line = $bodyLines[$i].TrimEnd()
        if ([string]::IsNullOrWhiteSpace($line)) { $i++; continue }
        $trim = $line.Trim()
        if (Should-SkipLine $trim) { $i++; continue }
        if ($trim -match '^##\s+(\d+\.\s+.+)$') {
            Add-Paragraph $doc $Matches[1] 14 $true
            $i++; continue
        }
        if ($trim -match '^###\s+(\d+\.\d+\s+.+)$') {
            Add-Paragraph $doc $Matches[1] 12 $true
            $i++; continue
        }
        if ($trim -match '^```') {
            $codeLines = @()
            $i++
            while ($i -lt $bodyLines.Count -and $bodyLines[$i].Trim() -notmatch '^```') {
                $codeLines += $bodyLines[$i]
                $i++
            }
            Add-CodeBlock $doc $codeLines
            $i++; continue
        }
        if ($trim -match '^\|') {
            $idxRef = [ref]$i
            $tableData = Parse-MdTable $bodyLines $idxRef
            if ($tableData -and $tableData.Rows.Count -gt 0) { Add-WordTable $doc $tableData }
            $i = $idxRef.Value; continue
        }
        if ($trim -match '\(21\).*docx') { $i++; continue }
        Add-Paragraph $doc $trim
        $i++
    }
    $outDir = Split-Path $OutPath -Parent
    if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }
    if (Test-Path $OutPath) { Remove-Item -LiteralPath $OutPath -Force }
    $doc.SaveAs([ref]$OutPath, [ref]16)
    Write-Output "Created: $OutPath"
}
finally {
    $doc.Close($false)
    $word.Quit()
    [System.Runtime.InteropServices.Marshal]::ReleaseComObject($doc) | Out-Null
    [System.Runtime.InteropServices.Marshal]::ReleaseComObject($word) | Out-Null
    [GC]::Collect()
    [GC]::WaitForPendingFinalizers()
}
