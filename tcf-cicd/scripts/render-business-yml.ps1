# 업무 WAR application-{profile}.yml 일괄 생성 (tcf-cicd SoT -> framework)
# values 파일만 수정 후 실행, 또는 sync-to-framework 로 framework 반영
param(
    [ValidateSet('local', 'dev', 'prod', 'all')]
    [string]$Profile = 'all'
)

$ErrorActionPreference = 'Stop'
$CicdRoot = Split-Path -Parent $PSScriptRoot
$FwRoot = Resolve-Path (Join-Path $CicdRoot '..')
$ValuesPath = Join-Path $CicdRoot 'business\values.yaml'

if (-not (Test-Path $ValuesPath)) {
    Write-Host "values not found: $ValuesPath — framework gen-business-profiles.ps1 실행"
    & (Join-Path $FwRoot 'scripts\gen-business-profiles.ps1')
    & (Join-Path $CicdRoot 'scripts\pull-from-framework.ps1') -Profile all
    exit 0
}

# values.yaml 기반 렌더 (확장 포인트)
Write-Host "render-business-yml: values.yaml 기반 렌더는 추후 확장. 현재는 pull-from-framework 사용."
& (Join-Path $CicdRoot 'scripts\pull-from-framework.ps1') -Profile $Profile
