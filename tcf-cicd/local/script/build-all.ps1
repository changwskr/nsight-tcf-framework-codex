# local — 전체 프로젝트 빌드 (Windows PowerShell)
param(
    [ValidateSet('all', 'wars', 'business', 'framework', 'fast')]
    [string]$Target = 'all',
    [switch]$SkipSync,
    [switch]$NoGradleStop,
    [switch]$Help
)

$ErrorActionPreference = 'Stop'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$CicdRoot = (Resolve-Path (Join-Path $ScriptDir '../..')).Path
$FwRoot = (Resolve-Path (Join-Path $CicdRoot '..')).Path
$SyncScript = Join-Path $CicdRoot 'scripts/sync-to-framework.ps1'

function Show-Help {
    @"

Usage: build-all.ps1 [-Target <mode>] [-SkipSync] [-NoGradleStop]

local 프로파일 yml 동기화 후 nsight-tcf-framework 전 모듈을 빌드합니다.

Targets:
  all        (기본) gradle build — compile + test + jar/war (전체 24 모듈)
  wars       gradle buildZtomcatWars — 19 WAR (업무 16 + tcf-om + batch + ui)
  business   gradle buildBusinessWars — 17 WAR (업무 16 + tcf-om)
  framework  tcf-util/core/cache/web + tcf-om/batch/ui jar/war (업무 WAR 제외)
  fast       gradle build -x test

Options:
  -SkipSync      tcf-cicd/local -> framework sync 생략
  -NoGradleStop  gradle --stop 생략

Examples:
  .\build-all.ps1
  .\build-all.ps1 -Target wars
  .\build-all.ps1 -Target fast -SkipSync

Framework root: $FwRoot

"@
}

if ($Help) { Show-Help; exit 0 }

function Resolve-Gradle {
    $candidates = @()
    if ($env:GRADLE_HOME_OVERRIDE) {
        $candidates += (Join-Path $env:GRADLE_HOME_OVERRIDE 'bin\gradle.bat')
    }
    if ($env:GRADLE_HOME) {
        $candidates += (Join-Path $env:GRADLE_HOME 'bin\gradle.bat')
    }
    $cmd = Get-Command gradle -ErrorAction SilentlyContinue
    if ($cmd) { $candidates += $cmd.Source }

    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path -LiteralPath $candidate)) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }
    throw 'gradle not found. Set GRADLE_HOME (quote paths with parentheses) or add gradle to PATH.'
}

if (-not $SkipSync) {
    if (-not (Test-Path $SyncScript)) { throw "sync script not found: $SyncScript" }
    Write-Host '[build-all] sync local config -> framework'
    $syncParams = @{ Profile = 'local' }
    & $SyncScript @syncParams
}

$Gradle = Resolve-Gradle
Push-Location $FwRoot
try {
    if (-not $NoGradleStop) {
        Write-Host '[build-all] gradle --stop'
        & $Gradle --stop 2>$null | Out-Null
    }

    $tasks = switch ($Target) {
        'all'       { @('build') }
        'wars'      { @('buildZtomcatWars') }
        'business'  { @('buildBusinessWars') }
        'framework' {
            @(
                ':tcf-util:build', ':tcf-core:build', ':tcf-cache:build', ':tcf-web:build',
                ':tcf-om:build', ':tcf-batch:build', ':tcf-ui:build'
            )
        }
        'fast'      { @('build', '-x', 'test') }
    }

    Write-Host "[build-all] gradle $($tasks -join ' ')"
    $gradleArgs = [string[]]$tasks
    & $Gradle @gradleArgs
    if ($null -ne $LASTEXITCODE -and $LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    Write-Host "[build-all] Done ($Target)"
}
finally {
    Pop-Location
}
