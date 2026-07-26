$ErrorActionPreference = 'Stop'

$ZTomcatHome = Split-Path -Parent $MyInvocation.MyCommand.Path
$CatalinaHome = Join-Path $ZTomcatHome 'apache-tomcat-10.1.34'

if (-not (Test-Path (Join-Path $CatalinaHome 'bin\catalina.bat'))) {
    Write-Host '[ztomcat] Tomcat not found. Run install-tomcat.bat first.'
    exit 1
}

$jdk21 = Join-Path $env:USERPROFILE '.jdks\temurin-21.0.4'
if (Test-Path $jdk21) {
    $env:JAVA_HOME = $jdk21
} elseif (-not $env:JAVA_HOME) {
    Write-Host '[ztomcat] JDK 21 not found at' $jdk21
    exit 1
}

$env:CATALINA_HOME = $CatalinaHome
$env:CATALINA_BASE = $CatalinaHome

& (Join-Path $ZTomcatHome 'apply-config.ps1')

& (Join-Path $ZTomcatHome 'h2-txlog.ps1') -Action start
if ($null -ne $LASTEXITCODE -and $LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if ($env:ZTOMCAT_SKIP_DEPLOY -ne '1') {
    Write-Host '[ztomcat] Building and deploying batch.war, ui.war to Tomcat webapps ...'
    $DeployScript = Join-Path (Split-Path $ZTomcatHome -Parent) 'tcf-cicd\local\script\deploy-wars.ps1'
    if (-not (Test-Path $DeployScript)) {
        Write-Host "[ztomcat] deploy script not found: $DeployScript"
        exit 1
    }
    & $DeployScript -SkipSync -SyncProfile dev -SkipBatchCollect batch ui
    if ($null -ne $LASTEXITCODE -and $LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} else {
    Write-Host '[ztomcat] Skip WAR deploy (ZTOMCAT_SKIP_DEPLOY=1)'
}

Write-Host '[ztomcat] Starting Tomcat on http://localhost:8080'
Write-Host '[ztomcat]   batch → http://localhost:8080/batch'
Write-Host '[ztomcat]   ui    → http://localhost:8080/ui/om/admin/login.html'
& (Join-Path $CatalinaHome 'bin\catalina.bat') start

Write-Host '[ztomcat] Started Tomcat :8080 (tcf-batch /batch, tcf-ui /ui WAR)'
