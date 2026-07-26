$ErrorActionPreference = 'Stop'

$ZTomcatHome = Split-Path -Parent $MyInvocation.MyCommand.Path
$CatalinaHome = Join-Path $ZTomcatHome 'apache-tomcat-10.1.34'

if (-not (Test-Path (Join-Path $CatalinaHome 'bin\catalina.bat'))) {
    Write-Host '[ztomcat] Tomcat not found.'
    exit 1
}

$jdk21 = Join-Path $env:USERPROFILE '.jdks\temurin-21.0.4'
if (Test-Path $jdk21) {
    $env:JAVA_HOME = $jdk21
}

$env:CATALINA_HOME = $CatalinaHome
$env:CATALINA_BASE = $CatalinaHome

function Test-PortListening([int]$Port) {
    try {
        $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
        return $null -ne $conn
    } catch {
        return $false
    }
}

Write-Host '[ztomcat] Stopping Tomcat...'
$prevEap = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
& (Join-Path $CatalinaHome 'bin\catalina.bat') stop 2>&1 | Out-Null
$ErrorActionPreference = $prevEap

$deadline = (Get-Date).AddSeconds(45)
while ((Get-Date) -lt $deadline) {
    if (-not (Test-PortListening 8080) -and -not (Test-PortListening 8005)) {
        Write-Host '[ztomcat] Tomcat stopped (ports 8080/8005 closed).'
        & (Join-Path $ZTomcatHome 'h2-txlog.ps1') -Action stop
        exit 0
    }
    Start-Sleep -Seconds 2
}

Write-Host '[ztomcat] WARN: Tomcat still listening — forcing Java process shutdown...'
Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
    Where-Object { $_.CommandLine -like "*$($CatalinaHome.Replace('\', '\\'))*" } |
    ForEach-Object {
        Write-Host "  kill PID $($_.ProcessId)"
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
    }

Start-Sleep -Seconds 3
if (Test-PortListening 8080) {
    Write-Host '[ztomcat] ERROR: port 8080 still in use. Stop Tomcat manually before redeploy.'
    exit 1
}
Write-Host '[ztomcat] Tomcat stopped (forced).'
& (Join-Path $ZTomcatHome 'h2-txlog.ps1') -Action stop
