param(
    [string]$Workspace = (Join-Path $PSScriptRoot '..\_workspace'),
    [string]$RunDirectory = (Join-Path $PSScriptRoot '..\..\..\..\_runs')
)
$ErrorActionPreference='Stop'
New-Item -ItemType Directory -Force -Path $Workspace,$RunDirectory | Out-Null
$analysis=Join-Path $Workspace 'analysis-summary.md';$plan=Join-Path $Workspace 'poc-plan.md';$log=Join-Path $RunDirectory 'orchestrator-simulation.log'
function Write-AtomicUtf8{param([string]$Path,[string[]]$Lines)$temp="$Path.$([guid]::NewGuid()).tmp";try{[System.IO.File]::WriteAllLines($temp,$Lines,(New-Object System.Text.UTF8Encoding($false)));Move-Item -LiteralPath $temp -Destination $Path -Force}finally{if(Test-Path $temp){Remove-Item $temp -Force}}}
Write-AtomicUtf8 $analysis @('# Analysis Summary','','Status: complete','Scope: Codex harness requirements and completion criteria.')
Write-AtomicUtf8 $plan @('# PoC Plan','','Status: complete','Artifacts: analysis-summary.md, poc-plan.md','Stages: analyze, build, verify.')
$pass=(Test-Path $analysis) -and (Test-Path $plan) -and ((Get-Item $analysis).Length -gt 0) -and ((Get-Item $plan).Length -gt 0)
$result=if($pass){'QA PASS'}else{'QA FAIL'}
Write-AtomicUtf8 $log @('TCF Harness Orchestrator Simulation',"Analyst: $analysis","Builder: $plan","QA: $result")
Write-Host $result
if(-not $pass){exit 1};exit 0
