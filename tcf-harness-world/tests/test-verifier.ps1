param(
    [ValidateSet('Contracts', 'Skills', 'Simulation', 'Verifier', 'All')]
    [string]$Mode = 'All'
)

$ErrorActionPreference = 'Stop'
$script:Failures = [System.Collections.Generic.List[string]]::new()
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

function Assert-True {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) { $script:Failures.Add($Message) }
}

function Assert-FileContains {
    param([string]$Path, [string[]]$Patterns)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        $script:Failures.Add("Missing file: $Path")
        return
    }
    $content = Get-Content -Raw -Encoding UTF8 -LiteralPath $Path
    foreach ($pattern in $Patterns) {
        if ($content -notmatch [regex]::Escape($pattern)) {
            $script:Failures.Add("Missing '$pattern' in $Path")
        }
    }
}

function Test-Contracts {
    $required = @(
        'AGENTS.md', 'README.md', 'docs/quickstart.md',
        'docs/migration-from-claude.md', 'agents/analyst.md',
        'agents/builder.md', 'agents/qa.md'
    )
    foreach ($relative in $required) {
        Assert-True (Test-Path -LiteralPath (Join-Path $Root $relative) -PathType Leaf) "Missing file: $relative"
    }
    foreach ($role in @('analyst', 'builder', 'qa')) {
        $rolePath = Join-Path $Root "agents/$role.md"
        if (-not (Test-Path -LiteralPath $rolePath -PathType Leaf)) { continue }
        $content = Get-Content -Raw -Encoding UTF8 -LiteralPath $rolePath
        $count = ([regex]::Matches($content, '(?m)^## [^#\r\n]+$')).Count
        Assert-True ($count -eq 6) "Expected 6 role sections in $rolePath, found $count"
    }
    Assert-FileContains (Join-Path $Root 'README.md') @('Codex', 'AGENTS.md', 'SKILL.md', 'verify-codex-harness.ps1')
}

switch ($Mode) {
    'Contracts' { Test-Contracts }
    'All' { Test-Contracts }
    default { $script:Failures.Add("Test mode is not implemented yet: $Mode") }
}

if ($script:Failures.Count -gt 0) {
    $script:Failures | ForEach-Object { Write-Error $_ -ErrorAction Continue }
    exit 1
}
Write-Host 'Contract checks passed.'
exit 0
