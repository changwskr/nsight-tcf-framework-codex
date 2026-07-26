param(
    [ValidateSet('start', 'stop', 'status', 'restart')]
    [string]$Action = 'start',
    [switch]$Help
)

$ErrorActionPreference = 'Stop'

if ($Help) {
    @"
Usage: .\tcf-h2\scripts\run-h2.ps1 [start|stop|status|restart]

Examples:
  .\tcf-h2\scripts\run-h2.ps1
  .\tcf-h2\scripts\run-h2.ps1 start
  .\tcf-h2\scripts\run-h2.ps1 status
  .\tcf-h2\scripts\run-h2.ps1 stop
"@
    exit 0
}

$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$target = Join-Path $root 'ztomcat\h2-txlog.ps1'

if (-not (Test-Path $target)) {
    Write-Host "[tcf-h2] Script not found: $target"
    exit 1
}

& $target -Action $Action
exit $LASTEXITCODE
