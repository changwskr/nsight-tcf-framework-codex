# NSIGHT 용량산정 가이드 Markdown 생성
$ErrorActionPreference = 'Stop'
$bookDir = Split-Path $PSScriptRoot -Parent
$tocFile = Join-Path $bookDir '_tmp-toc\toc-lines.txt'
$lines = Get-Content $tocFile -Encoding UTF8

$common = @'
## NSIGHT 1차 표준 전제

| 항목 | 기준값 |
|------|--------|
| 지점 수 | 3,600 |
| 지점당 사용자 | 6 |
| 전체 사용자 | 21,600 |
| 설계 세션 | 26,000~28,000 |
| TPS | 360 / 720 / 1,080 |
| 기준 VM | 8 vCPU / 32GB, 250 TPS/VM |

> 출처: znsight-capacity-word
'@

function Slugify([string]$t) {
    $s = $t -replace '^\d+\.\s*','' -replace '[·/\\:*?"<>|]','-' -replace '\s+','-' -replace '-+','-'
    $s.Trim('-')
}

$part = $null; $cur = $null
$chapters = [ordered]@{}; $appendices = [ordered]@{}

foreach ($line in $lines) {
    $line = $line.Trim()
    if (-not $line) { continue }
    if ($line -match '^제\d+부\.') { $part = $line; continue }
    if ($line -match '^(\d+)\.\s+(.+)$' -and $line -notmatch '^\d+\.\d+') {
        $n = [int]$Matches[1]
        $cur = @{ Num=$n; Title=$Matches[2]; Part=$part; Sections=@() }
        $chapters[$n] = $cur; continue
    }
    if ($line -match '^(\d+)\.(\d+)\s+(.+)$' -and $cur -and [int]$Matches[1] -eq $cur.Num) {
        $cur.Sections += @{ Num="$($Matches[1]).$($Matches[2])"; Title=$Matches[3] }; continue
    }
    if ($line -match '^([A-Z]{1,2})\.\s+(.+)$') { $appendices[$Matches[1]] = $Matches[2] }
}

$extras = @{
    1 = "용량산정은 서버 대수만이 아닙니다. 사용자수->동시요청->TPS->AP->DB Pool->장애 잔여량이 연결되어야 합니다."
    7 = "전환흐름: 사용자->동시요청->TPS->VM->Thread->JVM->Pool->Timeout->임계치"
    8 = "전체 사용자 21,600, 설계 세션 26,000~28,000, 동시요청률 5/10/15%"
    9 = "TPS = 동시요청자 / 3초. 기본 360, 피크 720, 스트레스 1080"
    10 = "TPMC 3000/TPS, Core당 35 TPS, 8Core 250 TPS/VM"
    11 = "8CORE-32GB 표준. 피크 720 TPS 권장 AP 8대(A-A). 8CORE Scale-Out 권장"
    16 = "Timeout: DB3s < Pool3s < Tx4-5s < Proxy10s < Web15s < L4 120s"
    22 = "maxThreads 400-500, acceptCount 500, maxConnections 10000"
    23 = "Heap 12-14GB, G1GC, MaxGCPauseMillis 200"
    26 = "Pool 산정: TPS x DB점유 x 1.3, 일반 50 / SV 60"
    37 = "8C/32G 프로파일: 250 TPS, Thread 500, Heap 12G, Pool 50"
    47 = "시험: 360/720/1080 TPS, 센터/AP 장애, Slow SQL, GC"
    49 = "임계치: p95>3s, Heap>70%, BusyThread>70%, Hikari Active>70%"
}

$idx = @("# NSIGHT 통합 용량산정 및 솔루션 환경설정 가이드 — 목차","","$common","","## 본문","")
$lastPart = ''
foreach ($n in $chapters.Keys) {
    $ch = $chapters[$n]
    if ($ch.Part -and $ch.Part -ne $lastPart) { $idx += "### $($ch.Part)"; $idx += ''; $lastPart = $ch.Part }
    $fn = '{0:D2}-{1}.md' -f $n, (Slugify $ch.Title)
    $idx += "- [$n. $($ch.Title)](./$fn)"
    $body = @("# $n. $($ch.Title)",'')
    if ($ch.Part) { $body += "> $($ch.Part)"; $body += '' }
    $body += $common; $body += ''
    if ($extras.ContainsKey($n)) { $body += "## 핵심 요약"; $body += ''; $body += $extras[$n]; $body += '' }
    foreach ($s in $ch.Sections) {
        $body += "## $($s.Num) $($s.Title)"; $body += ''
        $body += 'znsight-capacity-word 원본 및 NSIGHT 1차 표준을 기준으로 작성합니다.'; $body += ''
    }
    $body += '---'; $body += ''; $body += '[← 목차](./00-목차.md)'
    ($body -join "`n") | Out-File (Join-Path $bookDir $fn) -Encoding utf8
}
$idx += ''; $idx += '## 부록'; $idx += ''
$appDir = Join-Path $bookDir '부록'
New-Item -ItemType Directory -Force -Path $appDir | Out-Null
foreach ($k in $appendices.Keys) {
    $fn = "$k-$(Slugify $appendices[$k]).md"
    $idx += "- [$k. $($appendices[$k])](./부록/$fn)"
    $ab = @("# 부록 $k. $($appendices[$k])",'','> znsight-capacity-word 참조','')
    if ($k -eq 'B') { $ab += '|시나리오|TPS|'; $ab += '|기본|360|'; $ab += '|피크|720|' }
    if ($k -eq 'M') { $ab += 'DB3s < Pool3s < Tx5s < Proxy10s < Web15s' }
    if ($k -eq 'Y') { $ab += 'Warning: p95>3s, Heap>70%. Critical: p95>5s, Heap>85%' }
    $ab += ''; $ab += '---'; $ab += ''; $ab += '[← 목차](../00-목차.md)'
    ($ab -join "`n") | Out-File (Join-Path $appDir $fn) -Encoding utf8
}
($idx -join "`n") | Out-File (Join-Path $bookDir '00-목차.md') -Encoding utf8
$readme = @"
# NSIGHT 통합 용량산정 가이드

- [00-목차.md](./00-목차.md)
- [zNSIGHT-용량산정-전체-흐름.md](./zNSIGHT-용량산정-전체-흐름.md)

원본: znsight-capacity-word ($($chapters.Count) chapters, $($appendices.Count) appendices)
"@
$readme | Out-File (Join-Path $bookDir 'README.md') -Encoding utf8
Write-Host "Done: $($chapters.Count) chapters"
