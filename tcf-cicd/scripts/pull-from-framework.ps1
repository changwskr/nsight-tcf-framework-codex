# framework → tcf-cicd (초기 bootstrap / 역방향 sync)
param(
    [ValidateSet('local', 'dev', 'prod', 'all')]
    [string]$Profile = 'all'
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

function Copy-IfExists {
    param([string]$Src, [string]$Dest)
    if (-not (Test-Path $Src)) {
        Write-Warning "missing: $Src"
        return
    }
    $dir = Split-Path $Dest -Parent
    if ($dir) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    Copy-Item -Force -Path $Src -Destination $Dest
    Write-Host "pull -> $Dest"
}

$selected = if ($Profile -eq 'all') { @('local', 'dev', 'prod') } else { @($Profile) }

foreach ($prof in $selected) {
    $ymlName = $ProfileFiles[$prof]

    foreach ($mod in $Modules) {
        $src = Join-Path $FwRoot "$mod\src\main\resources\$ymlName"
        $dest = Join-Path $CicdRoot "$prof\spring\$mod\$ymlName"
        Copy-IfExists $src $dest
    }

    switch ($prof) {
        'local' {
            Copy-IfExists (Join-Path $FwRoot 'ztomcat\setenv.local.sh') (Join-Path $CicdRoot 'local\ztomcat\setenv.local.sh')
            Copy-IfExists (Join-Path $FwRoot 'ztomcat\setenv.local.bat') (Join-Path $CicdRoot 'local\ztomcat\setenv.local.bat')
        }
        'dev' {
            Copy-IfExists (Join-Path $FwRoot 'ztomcat\conf\setenv.sh') (Join-Path $CicdRoot 'dev\ztomcat\setenv.sh')
            Copy-IfExists (Join-Path $FwRoot 'ztomcat\conf\setenv.bat') (Join-Path $CicdRoot 'dev\ztomcat\setenv.bat')
        }
        'prod' {
            Copy-IfExists (Join-Path $FwRoot 'ztomcat\conf\setenv.prod.sh') (Join-Path $CicdRoot 'prod\ztomcat\setenv.sh')
            Copy-IfExists (Join-Path $FwRoot 'ztomcat\conf\setenv.prod.bat') (Join-Path $CicdRoot 'prod\ztomcat\setenv.bat')
            Copy-IfExists (Join-Path $FwRoot 'deploy\apache\nsight-marketing-routing.conf') (Join-Path $CicdRoot 'prod\apache\nsight-marketing-routing.conf')
        }
    }
}

Write-Host "Done pull-from-framework ($Profile)"
