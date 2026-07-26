# tcf-cicd → framework (빌드 전 sync)
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('local', 'dev', 'prod')]
    [string]$Profile,
    [switch]$DryRun,
    [switch]$ZtomcatOnly,
    [switch]$SpringOnly
)

$ErrorActionPreference = 'Stop'
$CicdRoot = Split-Path -Parent $PSScriptRoot
$FwRoot = (Resolve-Path (Join-Path $CicdRoot '..')).Path

$Modules = @(
    'tcf-web', 'tcf-cache', 'tcf-core', 'tcf-om', 'tcf-batch', 'tcf-ui',
    'ic-service', 'pc-service', 'ms-service', 'sv-service',
    'pd-service', 'eb-service', 'ep-service', 'ss-service', 'mg-service'
)

$ProfileFiles = @{
    local = 'application-local.yml'
    dev   = 'application-dev.yml'
    prod  = 'application-prod.yml'
}

function Sync-IfExists {
    param([string]$Src, [string]$Dest, [bool]$Dry)
    if (-not (Test-Path $Src)) {
        Write-Warning "skip (no cicd file): $Src"
        return
    }
    if ($Dry) {
        Write-Host "[dry-run] $Src -> $Dest"
        return
    }
    $dir = Split-Path $Dest -Parent
    if ($dir) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    Copy-Item -Force -Path $Src -Destination $Dest
    Write-Host "sync -> $Dest"
}

$ymlName = $ProfileFiles[$Profile]

if (-not $ZtomcatOnly) {
    foreach ($mod in $Modules) {
        $src = Join-Path $CicdRoot "$Profile\spring\$mod\$ymlName"
        $dest = Join-Path $FwRoot "$mod\src\main\resources\$ymlName"
        Sync-IfExists $src $dest $DryRun.IsPresent
    }
}

if (-not $SpringOnly) {
    switch ($Profile) {
        'local' {
            Sync-IfExists (Join-Path $CicdRoot 'local\ztomcat\setenv.local.sh') (Join-Path $FwRoot 'ztomcat\setenv.local.sh') $DryRun.IsPresent
            Sync-IfExists (Join-Path $CicdRoot 'local\ztomcat\setenv.local.bat') (Join-Path $FwRoot 'ztomcat\setenv.local.bat') $DryRun.IsPresent
        }
        'dev' {
            Sync-IfExists (Join-Path $CicdRoot 'dev\ztomcat\setenv.sh') (Join-Path $FwRoot 'ztomcat\conf\setenv.sh') $DryRun.IsPresent
            Sync-IfExists (Join-Path $CicdRoot 'dev\ztomcat\setenv.bat') (Join-Path $FwRoot 'ztomcat\conf\setenv.bat') $DryRun.IsPresent
        }
        'prod' {
            Sync-IfExists (Join-Path $CicdRoot 'prod\ztomcat\setenv.sh') (Join-Path $FwRoot 'ztomcat\conf\setenv.prod.sh') $DryRun.IsPresent
            Sync-IfExists (Join-Path $CicdRoot 'prod\ztomcat\setenv.bat') (Join-Path $FwRoot 'ztomcat\conf\setenv.prod.bat') $DryRun.IsPresent
            Sync-IfExists (Join-Path $CicdRoot 'prod\apache\nsight-marketing-routing.conf') (Join-Path $FwRoot 'deploy\apache\nsight-marketing-routing.conf') $DryRun.IsPresent
        }
    }
}

Write-Host "Done sync-to-framework ($Profile)"
