param(
    [ValidateSet('Contracts', 'Skills')]
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

    # This Task 1 check scans only the Task 1 contract files listed above.
    # Tasks 2 through 4 own legacy references in skills, scripts, and samples.
    $fixture = 'tcf-harness-world'
    foreach ($relativePath in @('AGENTS.md', 'README.md') + $roles + $docs) {
        $path = Join-Path $Root $relativePath
        if ((Test-Path -LiteralPath $path -PathType Leaf) -and
            (Select-String -LiteralPath $path -SimpleMatch $fixture -Quiet -Encoding UTF8)) {
            $script:Failures.Add("Stale world path in Task 1 contract: $relativePath")
        }
    }
}

function Test-Skills {
    $skills = @(
        'skills/pdmp-development/SKILL.md',
        'skills/pdmp-crud/SKILL.md',
        'skills/pdmp-tcf/SKILL.md',
        'skills/pdmp-security/SKILL.md',
        'skills/pdmp-quality/SKILL.md'
    )

    foreach ($relativePath in $skills) {
        Assert-FileContains $relativePath @(
            '---',
            'name:',
            'description:'
        )
    }

    Assert-FileContains 'skills/pdmp-development/SKILL.md' @(
        'references/pdmp-project-map.md',
        'references/handoff-protocol.md',
        'agents/pdmp-analyst.md',
        'agents/pdmp-builder.md',
        'agents/pdmp-security-reviewer.md',
        'agents/pdmp-qa.md',
        'pdmp-crud',
        'pdmp-tcf',
        'pdmp-security',
        'pdmp-quality'
    )
    Assert-FileContains 'skills/pdmp-development/references/pdmp-project-map.md' @(
        'pdmp-service',
        'mpcoa8888',
        'Controller -> Service -> DAO -> MyBatis',
        'src/main',
        'src/test'
    )
    Assert-FileContains 'skills/pdmp-development/references/handoff-protocol.md' @(
        'analysis-summary.md',
        'verification-report.md',
        'security-review.md',
        'qa-report.md'
    )
    Assert-FileContains 'skills/pdmp-crud/SKILL.md' @(
        'references/crud-checklist.md',
        'list',
        'detail',
        'create',
        'update',
        'delete',
        'safe delete'
    )
    Assert-FileContains 'skills/pdmp-crud/references/crud-checklist.md' @(
        'list',
        'detail',
        'create',
        'update',
        'delete',
        'MyBatis parameter binding'
    )
    Assert-FileContains 'skills/pdmp-tcf/SKILL.md' @(
        '@TcfTransaction',
        'serviceId',
        'transactionCode',
        'processingType',
        'serviceName',
        'MP.{Domain}.{action}'
    )
    Assert-FileContains 'skills/pdmp-security/SKILL.md' @(
        'JWT',
        'CORS',
        'SQL binding',
        'logs',
        '/api/mp/co/a/8888/**'
    )
    Assert-FileContains 'skills/pdmp-quality/SKILL.md' @(
        'gradlew.bat test',
        'gradlew.bat war',
        'H2',
        'Oracle'
    )

    Assert-True (-not (Test-Path -LiteralPath (Join-Path $Root 'skills/harness'))) 'Legacy skills/harness directory must be removed'
    Assert-True (-not (Test-Path -LiteralPath (Join-Path $Root 'skills/harness/orchestrator-sample'))) 'Legacy orchestrator-sample must be removed'
}

switch ($Mode) {
    'Contracts' { Test-Contracts }
    'Skills' { Test-Skills }
}

if ($script:Failures.Count -gt 0) {
    $script:Failures | ForEach-Object { Write-Error $_ -ErrorAction Continue }
    exit 1
}

Write-Host "$Mode checks passed."
exit 0
