param(
    [string[]]$Contexts = @('ic','pc','ms','sv','pd','eb','ep','ss','mg','om','ui','batch','zz-batch','00-om')
)

$ErrorActionPreference = 'Stop'
$ZTomcatHome = Split-Path -Parent $MyInvocation.MyCommand.Path
$Webapps = Join-Path $ZTomcatHome 'apache-tomcat-10.1.34\webapps'

function Test-TomcatRunning {
    try {
        $conn = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
        return $null -ne $conn
    } catch {
        return $false
    }
}

if (Test-TomcatRunning) {
    Write-Host '[ztomcat] ERROR: Tomcat is running on port 8080.'
    Write-Host '[ztomcat] Stop Tomcat first: ztomcat\stop.ps1'
    Write-Host '[ztomcat] Or use rolling deploy (WAR copy only): ztomcat\deploy-restart.bat sv om'
    exit 1
}

Write-Host '[ztomcat] Removing exploded WAR directories...'
$failed = @()
foreach ($ctx in $Contexts) {
    $dir = Join-Path $Webapps $ctx
    if (-not (Test-Path $dir)) { continue }
    try {
        Remove-Item -LiteralPath $dir -Recurse -Force
        Write-Host "  removed $ctx/"
    } catch {
        $failed += $ctx
        Write-Host "  FAIL remove $ctx/ — $($_.Exception.Message)"
    }
}

if ($failed.Count -gt 0) {
    Write-Host "[ztomcat] ERROR: could not remove: $($failed -join ', '). Stop Tomcat and retry."
    exit 1
}
