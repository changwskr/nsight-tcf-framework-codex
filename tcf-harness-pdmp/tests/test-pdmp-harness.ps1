param(
    [ValidateSet('Contracts')]
    [string]$Mode = 'Contracts'
)

$ErrorActionPreference = 'Stop'
$script:Failures = [System.Collections.Generic.List[string]]::new()
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

function Assert-True {
    param([bool]$Condition, [string]$Message)

    if (-not $Condition) {
        $script:Failures.Add($Message)
    }
}

function Assert-FileContains {
    param([string]$RelativePath, [string[]]$Tokens)

    $path = Join-Path $Root $RelativePath
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        $script:Failures.Add("Missing file: $RelativePath")
        return
    }

    $content = Get-Content -LiteralPath $path -Raw -Encoding UTF8
    foreach ($token in $Tokens) {
        Assert-True ($content.Contains($token)) "Missing '$token' in $RelativePath"
    }
}

function Test-Contracts {
    $roles = @(
        'agents/pdmp-analyst.md',
        'agents/pdmp-builder.md',
        'agents/pdmp-security-reviewer.md',
        'agents/pdmp-qa.md'
    )
    $docs = @('docs/quickstart.md', 'docs/pdmp-architecture.md', 'docs/workflow.md')

    foreach ($relativePath in @('AGENTS.md', 'README.md') + $roles + $docs) {
        Assert-True (Test-Path -LiteralPath (Join-Path $Root $relativePath) -PathType Leaf) "Missing file: $relativePath"
    }

    foreach ($role in $roles) {
        $path = Join-Path $Root $role
        if (Test-Path -LiteralPath $path -PathType Leaf) {
            $sectionCount = ([regex]::Matches(
                    (Get-Content -LiteralPath $path -Raw -Encoding UTF8),
                    '(?m)^## [^#\r\n]+$')).Count
            Assert-True ($sectionCount -eq 6) "Expected 6 second-level sections in $role, found $sectionCount"
        }
    }

    Assert-FileContains 'README.md' @(
        'TCF Harness PDMP',
        'pdmp-service',
        'verify-pdmp-harness.ps1'
    )
    Assert-FileContains 'AGENTS.md' @(
        'pdmp-service',
        'Controller -> Service -> DAO -> MyBatis',
        '@TcfTransaction',
        'MP.{Domain}.{action}'
    )
    Assert-FileContains 'docs/pdmp-architecture.md' @(
        'mpcoa8888Controller -> mpcoa8888Service -> mpcoa8888Dao -> mpcoa8888-ORA.xml',
        'MP.SalesTip8888.list',
        'MP-INQ-8881',
        'MP-DEL-8885',
        'H2',
        'Oracle',
        'MP0404',
        'MP0409',
        '/api/mp/co/a/8888/**'
    )
    Assert-FileContains 'docs/workflow.md' @(
        'approved design',
        'RED',
        'GREEN',
        'pdmp-security-reviewer.md',
        'pdmp-qa.md'
    )

    # This fixture is the only permitted occurrence of the legacy package name.
    $fixture = 'tcf-harness-world'
    foreach ($relativePath in @('AGENTS.md', 'README.md') + $roles + $docs) {
        $path = Join-Path $Root $relativePath
        if ((Test-Path -LiteralPath $path -PathType Leaf) -and
            (Select-String -LiteralPath $path -SimpleMatch $fixture -Quiet -Encoding UTF8)) {
            $script:Failures.Add("Stale world path: $relativePath")
        }
    }
}

switch ($Mode) {
    'Contracts' { Test-Contracts }
}

if ($script:Failures.Count -gt 0) {
    $script:Failures | ForEach-Object { Write-Error $_ -ErrorAction Continue }
    exit 1
}

Write-Host 'Contracts checks passed.'
exit 0
