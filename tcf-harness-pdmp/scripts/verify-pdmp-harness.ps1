param(
    [string]$Root = (Join-Path $PSScriptRoot '..'),
    [string]$TargetRoot
)

$ErrorActionPreference = 'Stop'
$script:Failures = [System.Collections.Generic.List[string]]::new()

try {
    $Root = (Resolve-Path -LiteralPath $Root -ErrorAction Stop).Path
}
catch {
    [Console]::Error.WriteLine("Invalid harness root: $Root")
    exit 1
}

if ([string]::IsNullOrWhiteSpace($TargetRoot)) {
    $TargetRoot = Join-Path (Split-Path -Parent $Root) 'pdmp-service'
}

try {
    $TargetRoot = (Resolve-Path -LiteralPath $TargetRoot -ErrorAction Stop).Path
}
catch {
    [Console]::Error.WriteLine("Invalid pdmp-service target: $TargetRoot")
    exit 1
}

function Add-Failure {
    param([string]$Message)

    $script:Failures.Add($Message)
}

function Test-RequiredFile {
    param([string]$RelativePath)

    if (-not (Test-Path -LiteralPath (Join-Path $Root $RelativePath) -PathType Leaf)) {
        Add-Failure "Missing file: $RelativePath"
    }
}

function Test-RoleSections {
    param([string]$RelativePath)

    $path = Join-Path $Root $RelativePath
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        return
    }

    $sectionCount = ([regex]::Matches(
            (Get-Content -LiteralPath $path -Raw -Encoding UTF8),
            '(?m)^## [^#\r\n]+$')).Count
    if ($sectionCount -ne 6) {
        Add-Failure "Expected 6 second-level sections in $RelativePath, found $sectionCount"
    }
}

function Test-SkillFrontmatter {
    param([string]$RelativePath)

    $path = Join-Path $Root $RelativePath
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        return
    }

    $content = Get-Content -LiteralPath $path -Raw -Encoding UTF8
    if ($content -notmatch '(?sm)^---\s*\r?\n.*?^name:\s*[^\r\n]+\r?\n.*?^description:\s*[^\r\n]+\r?\n---\s*(?:\r?\n|$)') {
        Add-Failure "Invalid skill frontmatter: $RelativePath"
    }
}

function Test-MarkdownLinks {
    $linkPattern = '\[[^\]]+\]\((?!https?://|#)([^)]+)\)'

    foreach ($file in Get-ChildItem -LiteralPath $Root -Recurse -File -Filter '*.md') {
        $content = Get-Content -LiteralPath $file.FullName -Raw -Encoding UTF8
        foreach ($match in [regex]::Matches($content, $linkPattern)) {
            $linkTarget = ($match.Groups[1].Value -split '#', 2)[0]
            if ([string]::IsNullOrWhiteSpace($linkTarget)) {
                continue
            }

            try {
                $linkTarget = [uri]::UnescapeDataString($linkTarget)
            }
            catch {
                Add-Failure "Invalid Markdown link: $($file.FullName.Substring($Root.Length + 1)) -> $linkTarget"
                continue
            }

            $resolved = Join-Path $file.DirectoryName $linkTarget
            if (-not (Test-Path -LiteralPath $resolved)) {
                Add-Failure "Broken Markdown link: $($file.FullName.Substring($Root.Length + 1)) -> $linkTarget"
            }
        }
    }
}

function Test-ForbiddenCopiedSurface {
    foreach ($relativePath in @(
            'docs/migration-from-claude.md',
            'agents/analyst.md',
            'agents/builder.md',
            'agents/qa.md',
            'skills/harness',
            'samples/orchestrator-sample',
            'scripts/verify-codex-harness.ps1',
            'scripts/verify-codex-harness.sh',
            'tests/test-verifier.ps1'
        )) {
        if (Test-Path -LiteralPath (Join-Path $Root $relativePath)) {
            Add-Failure "Copied legacy path remains: $relativePath"
        }
    }

    $forbidden = 'tcf-harness-world|skills/harness|orchestrator-sample|verify-codex-harness|migration-from-claude|agents/(analyst|builder|qa)\.md'
    foreach ($file in Get-ChildItem -LiteralPath $Root -Recurse -File) {
        if ($file.Extension -notin @('.md', '.ps1', '.bat', '.sh')) {
            continue
        }

        $relativePath = $file.FullName.Substring($Root.Length + 1).Replace('\', '/')
        if ($relativePath.StartsWith('tests/') -or $relativePath -in @(
                'scripts/verify-pdmp-harness.ps1',
                'scripts/verify-pdmp-harness.sh'
            )) {
            continue
        }

        if (Select-String -LiteralPath $file.FullName -Pattern $forbidden -Encoding UTF8 -Quiet) {
            Add-Failure "Forbidden copied reference: $relativePath"
        }
    }
}

function Test-TargetFiles {
    foreach ($relativePath in @(
            'gradlew.bat',
            'build.gradle',
            'settings.gradle',
            'src/main/java/nhnis/mp/PdmpApplication.java',
            'src/main/java/nhnis/mp/config/SecurityConfig.java',
            'src/main/java/nhnis/mp/co/a/controller/mpcoa8888Controller.java',
            'src/main/java/nhnis/mp/co/a/service/mpcoa8888Service.java',
            'src/main/java/nhnis/mp/co/a/dao/mpcoa8888Dao.java',
            'src/main/resources/rdw.mp.co.a/mpcoa8888-ORA.xml',
            'src/test/java/nhnis/mp/co/a/controller/mpcoa8888ControllerTest.java',
            'src/test/java/nhnis/mp/co/a/service/mpcoa8888ServiceTest.java',
            'src/test/java/nhnis/mp/co/a/dao/mpcoa8888DaoIntegrationTest.java'
        )) {
        if (-not (Test-Path -LiteralPath (Join-Path $TargetRoot $relativePath) -PathType Leaf)) {
            Add-Failure "Missing pdmp-service target file: $relativePath"
        }
    }
}

foreach ($relativePath in @(
        'AGENTS.md',
        'README.md',
        'docs/quickstart.md',
        'docs/pdmp-architecture.md',
        'docs/workflow.md',
        'agents/pdmp-analyst.md',
        'agents/pdmp-builder.md',
        'agents/pdmp-security-reviewer.md',
        'agents/pdmp-qa.md',
        'skills/pdmp-development/SKILL.md',
        'skills/pdmp-development/references/pdmp-project-map.md',
        'skills/pdmp-development/references/handoff-protocol.md',
        'skills/pdmp-crud/SKILL.md',
        'skills/pdmp-crud/references/crud-checklist.md',
        'skills/pdmp-tcf/SKILL.md',
        'skills/pdmp-security/SKILL.md',
        'skills/pdmp-quality/SKILL.md',
        'samples/pdmp-development/scripts/run-simulation.ps1',
        'samples/pdmp-development/scripts/run-simulation.bat',
        'samples/pdmp-development/scripts/run-simulation.sh',
        'samples/pdmp-development/workspace/analysis-summary.md',
        'samples/pdmp-development/workspace/verification-report.md',
        'samples/pdmp-development/workspace/security-review.md',
        'samples/pdmp-development/workspace/qa-report.md'
    )) {
    Test-RequiredFile $relativePath
}

foreach ($role in @(
        'agents/pdmp-analyst.md',
        'agents/pdmp-builder.md',
        'agents/pdmp-security-reviewer.md',
        'agents/pdmp-qa.md'
    )) {
    Test-RoleSections $role
}

foreach ($skill in @(
        'skills/pdmp-development/SKILL.md',
        'skills/pdmp-crud/SKILL.md',
        'skills/pdmp-tcf/SKILL.md',
        'skills/pdmp-security/SKILL.md',
        'skills/pdmp-quality/SKILL.md'
    )) {
    Test-SkillFrontmatter $skill
}

Test-MarkdownLinks
Test-ForbiddenCopiedSurface
Test-TargetFiles

if ($script:Failures.Count -gt 0) {
    $script:Failures | ForEach-Object { [Console]::Error.WriteLine($_) }
    exit 1
}

Write-Host 'PDMP harness verification passed.'
exit 0
