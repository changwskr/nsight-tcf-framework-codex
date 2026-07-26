# tcf-cicd local — Tomcat 기동
param(
    [switch]$SkipSync,
    [switch]$SkipDeploy,
    [switch]$DeployAll,
    [switch]$Help
)

$ErrorActionPreference = 'Stop'
$LocalZtomcat = Split-Path -Parent $MyInvocation.MyCommand.Path
$CicdRoot = (Resolve-Path (Join-Path $LocalZtomcat '../..')).Path
$FwRoot = (Resolve-Path (Join-Path $CicdRoot '..')).Path
$ZTomcatHome = Join-Path $FwRoot 'ztomcat'
$CatalinaHome = Join-Path $ZTomcatHome 'apache-tomcat-10.1.34'
$SyncScript = Join-Path $CicdRoot 'scripts/sync-to-framework.ps1'
$DeployScript = Join-Path (Join-Path $CicdRoot 'local/script') 'deploy-wars.ps1'

function Show-Help {
    @"

Usage: start.ps1 [options]

1) tcf-cicd/dev -> framework sync (Spring dev + ztomcat setenv)
2) local setenv.local -> ztomcat/
3) apply-config
4) (선택) WAR 배포
5) catalina start

Options:
  -SkipSync     dev config sync 생략
  -SkipDeploy   WAR 빌드/배포 생략
  -DeployAll    19 WAR 전체 배포 (기본: batch + ui 만)

Examples:
  .\start.ps1
  .\start.ps1 -DeployAll
  .\start.ps1 -SkipSync -SkipDeploy

Tomcat: $CatalinaHome

"@
}

if ($Help) { Show-Help; exit 0 }

if (-not $SkipSync) {
    if (-not (Test-Path $SyncScript)) { throw "sync script not found: $SyncScript" }
    Write-Host '[local-ztomcat] sync dev config -> framework'
    & $SyncScript -Profile dev
}

Copy-Item -Force (Join-Path $LocalZtomcat 'setenv.local.sh') (Join-Path $ZTomcatHome 'setenv.local.sh')
Copy-Item -Force (Join-Path $LocalZtomcat 'setenv.local.bat') (Join-Path $ZTomcatHome 'setenv.local.bat')

if (-not (Test-Path (Join-Path $CatalinaHome 'bin/catalina.bat'))) {
    Write-Host '[local-ztomcat] Tomcat not found. Run ztomcat/install-tomcat first.'
    exit 1
}

$jdk21 = Join-Path $env:USERPROFILE '.jdks\temurin-21.0.4'
if (Test-Path $jdk21) {
    $env:JAVA_HOME = $jdk21
} elseif (-not $env:JAVA_HOME) {
    Write-Host '[local-ztomcat] JDK 21 not found. Set JAVA_HOME.'
    exit 1
}

$env:CATALINA_HOME = $CatalinaHome
$env:CATALINA_BASE = $CatalinaHome

& (Join-Path $ZTomcatHome 'apply-config.ps1')

& (Join-Path $ZTomcatHome 'h2-txlog.ps1') -Action start
if ($null -ne $LASTEXITCODE -and $LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if (-not $SkipDeploy) {
    if ($DeployAll) {
        Write-Host '[local-ztomcat] Deploying all 19 WARs ...'
        & $DeployScript -SkipSync
        if ($null -ne $LASTEXITCODE -and $LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    } elseif ($env:ZTOMCAT_SKIP_DEPLOY -ne '1') {
        Write-Host '[local-ztomcat] Deploying batch.war, ui.war ...'
        & (Join-Path $ZTomcatHome 'deploy-wars.bat') batch ui
        if ($null -ne $LASTEXITCODE -and $LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    } else {
        Write-Host '[local-ztomcat] Skip WAR deploy (ZTOMCAT_SKIP_DEPLOY=1)'
    }
}

Write-Host '[local-ztomcat] Starting Tomcat on http://localhost:8080'
Write-Host '[local-ztomcat]   batch -> http://localhost:8080/batch'
Write-Host '[local-ztomcat]   ui    -> http://localhost:8080/ui/om/admin/login.html'
& (Join-Path $CatalinaHome 'bin/catalina.bat') start

Write-Host '[local-ztomcat] Started Tomcat :8080 (profile dev via conf/setenv)'
