param(
    [switch]$Help,
    [switch]$Quiet
)

$ErrorActionPreference = 'Stop'

if ($Help) {
    @"
Usage: .\tcf-h2\scripts\check-h2.ps1 [-Quiet]

H2 TCP 서버(nsight_om) 기동 여부를 확인합니다.
기본 포트: 127.0.0.1:9092

Exit code:
  0 — UP (포트 LISTEN 중)
  1 — DOWN

Examples:
  .\tcf-h2\scripts\check-h2.ps1
  .\tcf-h2\scripts\check-h2.ps1 -Quiet
  tcf-h2\scripts\check-h2.bat
"@
    exit 0
}

$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$TcpHost = '127.0.0.1'
$TcpPort = 9092
$PidFile = Join-Path $root 'ztomcat\run\h2-txlog.pid'
$JdbcUrl = "jdbc:h2:tcp://${TcpHost}:$TcpPort/./nsight_om;MODE=Oracle;DATABASE_TO_UPPER=false"

function Test-PortListening([int]$Port) {
    try {
        $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
        return $null -ne $conn
    } catch {
        return $false
    }
}

function Get-H2ProcessInfo {
    if (Test-Path $PidFile) {
        $procId = [int]((Get-Content $PidFile -Raw).Trim())
        $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
        if ($proc) {
            return @{
                ProcessId = $proc.Id
                Source = 'pid-file'
            }
        }
    }

    $match = Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
        Where-Object {
            $_.CommandLine -like '*org.h2.tools.Server*' -and
            $_.CommandLine -like "*-tcpPort $TcpPort*"
        } |
        Select-Object -First 1

    if ($match) {
        return @{
            ProcessId = $match.ProcessId
            Source = 'process-scan'
        }
    }

    return $null
}

$listening = Test-PortListening $TcpPort
$procInfo = if ($listening) { Get-H2ProcessInfo } else { $null }

if ($listening) {
    if (-not $Quiet) {
        $pidText = if ($procInfo) { "PID $($procInfo.ProcessId)" } else { 'PID unknown' }
        Write-Host "[tcf-h2] UP - H2 TCP ${TcpHost}:$TcpPort ($pidText)"
        Write-Host "[tcf-h2] JDBC $JdbcUrl"
    }
    exit 0
}

if (-not $Quiet) {
    Write-Host "[tcf-h2] DOWN - H2 TCP ${TcpHost}:$TcpPort is not listening"
    Write-Host '[tcf-h2] Start: .\tcf-h2\scripts\run-h2.ps1 start'
}
exit 1
