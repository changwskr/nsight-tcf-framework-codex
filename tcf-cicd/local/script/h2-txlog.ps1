# local — nsight_om H2 TCP 서버 (port 9092)
param(
    [ValidateSet('start', 'stop', 'status', 'restart')]
    [string]$Action = 'start',
    [switch]$Help
)

$ErrorActionPreference = 'Stop'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$CicdRoot = (Resolve-Path (Join-Path $ScriptDir '../..')).Path
$FwRoot = (Resolve-Path (Join-Path $CicdRoot '..')).Path
$ZTomcatHome = Join-Path $FwRoot 'ztomcat'
$H2Script = Join-Path $ZTomcatHome 'h2-txlog.ps1'

function Show-Help {
    @"

Usage: h2-txlog.bat [start|stop|status|restart]

nsight_om 공유 H2 file DB를 TCP 9092로 기동합니다 (Tomcat과 분리).

Actions:
  start    (기본) H2 TCP 서버 기동
  stop     H2 TCP 서버 중지
  status   9092 포트 상태 확인
  restart  중지 후 재기동

Examples:
  .\h2-txlog.bat
  .\h2-txlog.bat start
  .\h2-txlog.bat status
  .\h2-txlog.bat stop

Data dir: $FwRoot\data\nsight-txlog
Script:   $H2Script

"@
}

if ($Help) { Show-Help; exit 0 }

if (-not (Test-Path $H2Script)) {
    Write-Host "[h2-txlog] Script not found: $H2Script"
    exit 1
}

if (-not $env:NSIGHT_TXLOG_PATH) {
    $env:NSIGHT_TXLOG_PATH = Join-Path $FwRoot 'data\nsight-txlog'
}

& $H2Script -Action $Action
exit $LASTEXITCODE
