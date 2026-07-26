# nsight_om shared H2 TCP server (port 9092) — Tomcat WAR lifecycle과 분리
param(
    [ValidateSet('start', 'stop', 'status', 'restart')]
    [string]$Action = 'start',
    [switch]$Help
)

$ErrorActionPreference = 'Stop'
$ZTomcatHome = Split-Path -Parent $MyInvocation.MyCommand.Path
$FwRoot = (Resolve-Path (Join-Path $ZTomcatHome '..')).Path
$RunDir = Join-Path $ZTomcatHome 'run'
$LogDir = Join-Path $ZTomcatHome 'logs'
$PidFile = Join-Path $RunDir 'h2-txlog.pid'
$LogFile = Join-Path $LogDir 'h2-txlog.log'
$TcpPort = 9092

function Show-Help {
    @"
Usage: h2-txlog.ps1 [start|stop|status|restart]

ztomcat dev — nsight_om file DB를 TCP 9092로 제공 (모든 WAR가 TCP 클라이언트).

"@
}

if ($Help) { Show-Help; exit 0 }

function Get-TxlogDir {
    if ($env:NSIGHT_TXLOG_PATH) {
        return (Resolve-Path $env:NSIGHT_TXLOG_PATH).Path
    }
    $default = Join-Path $FwRoot 'data\nsight-txlog'
    New-Item -ItemType Directory -Force -Path $default | Out-Null
    return (Resolve-Path $default).Path
}

function Test-PortListening([int]$Port) {
    try {
        $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
        return $null -ne $conn
    } catch {
        return $false
    }
}

function Resolve-H2Jar {
    $libDir = Join-Path $ZTomcatHome 'lib'
    New-Item -ItemType Directory -Force -Path $libDir | Out-Null
    $bundled = Get-ChildItem -Path $libDir -Filter 'h2-*.jar' -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($bundled) { return $bundled.FullName }

    $war = Join-Path $FwRoot 'tcf-om\build\libs\tcf-om.war'
    if (Test-Path $war) {
        $temp = Join-Path $env:TEMP 'nsight-h2-extract'
        New-Item -ItemType Directory -Force -Path $temp | Out-Null
        Push-Location $temp
        try {
            jar xf $war WEB-INF/lib/h2-2.2.224.jar 2>$null
            $extracted = Join-Path $temp 'WEB-INF\lib\h2-2.2.224.jar'
            if (Test-Path $extracted) {
                $dest = Join-Path $libDir 'h2-2.2.224.jar'
                Copy-Item $extracted $dest -Force
                return $dest
            }
        } finally {
            Pop-Location
            Remove-Item $temp -Recurse -Force -ErrorAction SilentlyContinue
        }
    }

    $cached = Get-ChildItem -Path (Join-Path $env:USERPROFILE '.gradle\caches\modules-2\files-2.1\com.h2database\h2') `
        -Recurse -Filter 'h2-*.jar' -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($cached) { return $cached.FullName }

    throw 'H2 jar not found. Run: gradle :tcf-om:bootWar'
}

function Resolve-JavaExe {
    $jdk21 = Join-Path $env:USERPROFILE '.jdks\temurin-21.0.4'
    if (Test-Path $jdk21) { return Join-Path $jdk21 'bin\java.exe' }
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
        return Join-Path $env:JAVA_HOME 'bin\java.exe'
    }
    throw 'JDK not found. Set JAVA_HOME or install temurin-21.0.4.'
}

function Warmup-TcpDatabase {
    param(
        [string]$H2Jar,
        [string]$Java
    )
    $url = "jdbc:h2:tcp://127.0.0.1:$TcpPort/./nsight_om;MODE=Oracle;DATABASE_TO_UPPER=false"
    $sqlFile = Join-Path $env:TEMP 'nsight-h2-warmup.sql'
    $scriptArg = $sqlFile.Replace('\', '/')
    $prevEap = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        Set-Content -Path $sqlFile -Value 'SELECT 1 FROM DUAL;' -Encoding ASCII
        & $Java -cp $H2Jar org.h2.tools.RunScript -url $url -user sa -script $scriptArg 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Write-Host '[h2-txlog] Warmed up ./nsight_om via TCP'
        } else {
            Write-Host '[h2-txlog] Warmup skipped (non-fatal)'
        }
    } catch {
        Write-Host "[h2-txlog] Warmup skipped (non-fatal): $($_.Exception.Message)"
    } finally {
        $ErrorActionPreference = $prevEap
        Remove-Item -LiteralPath $sqlFile -Force -ErrorAction SilentlyContinue
    }
}

function Start-H2Server {
    if (Test-PortListening $TcpPort) {
        Write-Host "[h2-txlog] Already listening on port $TcpPort"
        return
    }

    New-Item -ItemType Directory -Force -Path $RunDir, $LogDir | Out-Null
    $txlogDir = Get-TxlogDir
    $h2Jar = Resolve-H2Jar
    $java = Resolve-JavaExe
    $baseDir = $txlogDir.Replace('\', '/')

    Write-Host "[h2-txlog] Starting TCP server on 127.0.0.1:$TcpPort (baseDir=$baseDir)"

    $args = @(
        '-cp', $h2Jar,
        'org.h2.tools.Server',
        '-tcp', '-tcpPort', "$TcpPort",
        '-tcpAllowOthers',
        '-ifNotExists',
        '-baseDir', $baseDir
    )

    $proc = Start-Process -FilePath $java -ArgumentList $args `
        -RedirectStandardOutput $LogFile -RedirectStandardError "${LogFile}.err" `
        -WindowStyle Hidden -PassThru

    Set-Content -Path $PidFile -Value $proc.Id -Encoding ascii

    $deadline = (Get-Date).AddSeconds(15)
    while ((Get-Date) -lt $deadline) {
        if (Test-PortListening $TcpPort) {
            Write-Host "[h2-txlog] Ready (PID $($proc.Id))"
            Warmup-TcpDatabase -H2Jar $h2Jar -Java $java
            return
        }
        if ($proc.HasExited) {
            throw "H2 server exited early. See $LogFile"
        }
        Start-Sleep -Milliseconds 500
    }
    throw "H2 server did not open port $TcpPort within 15s. See $LogFile"
}

function Stop-H2Server {
    if (Test-Path $PidFile) {
        $procId = [int](Get-Content $PidFile -Raw).Trim()
        if (Get-Process -Id $procId -ErrorAction SilentlyContinue) {
            Write-Host "[h2-txlog] Stopping PID $procId"
            Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
        }
        Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
    }

    Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
        Where-Object { $_.CommandLine -like '*org.h2.tools.Server*' -and $_.CommandLine -like "*-tcpPort $TcpPort*" } |
        ForEach-Object {
            Write-Host "[h2-txlog] Stopping H2 PID $($_.ProcessId)"
            Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
        }

    Start-Sleep -Seconds 1
    if (Test-PortListening $TcpPort) {
        Write-Host "[h2-txlog] WARN: port $TcpPort still in use"
    } else {
        Write-Host '[h2-txlog] Stopped'
    }
}

function Show-H2Status {
    if (Test-PortListening $TcpPort) {
        Write-Host "[h2-txlog] UP — 127.0.0.1:$TcpPort"
        exit 0
    }
    Write-Host "[h2-txlog] DOWN — 127.0.0.1:$TcpPort"
    exit 1
}

switch ($Action) {
    'start' { Start-H2Server; exit 0 }
    'stop' { Stop-H2Server; exit 0 }
    'restart' { Stop-H2Server; Start-H2Server; exit 0 }
    'status' { Show-H2Status }
}
