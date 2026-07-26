# tcf-cicd — sync + Gradle 빌드 (배포 없음)
param(
    [ValidateSet('local', 'dev', 'prod')]
    [string]$Profile = 'dev',

    [ValidateSet('wars', 'business', 'all', 'fast')]
    [string]$Target = 'wars',

    [switch]$DryRun,
    [switch]$SkipSync,
    [switch]$NoGradleStop,
    [string]$ArtifactDir = '',
    [switch]$Help,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Codes = @()
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'cicd-common.ps1')

if ($Help) {
    @"

Usage: cicd-build.ps1 [-Profile <env>] [-Target <mode>] [options]

sync-to-framework 후 Gradle 빌드만 수행 (webapps 배포 없음).

Targets:
  wars      (기본) buildZtomcatWars — 12 WAR
  business  buildBusinessWars — 업무 9 + tcf-om
  all       build — 전 모듈 compile + test
  fast      build -x test

Options:
  -SkipSync -DryRun -NoGradleStop
  -ArtifactDir <path>   빌드 WAR를 artifact 디렉터리에 복사
  codes...              wars 모드에서 선택 빌드 (ic pc ... om ui batch)

Examples:
  .\cicd-build.ps1
  .\cicd-build.ps1 -Profile local -Target wars
  .\cicd-build.ps1 sv om -ArtifactDir .\out

"@
    exit 0
}

$syncProfile = if ($Profile -eq 'local') { 'local' } else { $Profile }

if (-not $SkipSync) {
    Write-Host "[cicd-build] sync ($syncProfile)"
    if ($DryRun) {
        Write-Host "[dry-run] sync-to-framework -Profile $syncProfile"
    } else {
        & $SyncScript -Profile $syncProfile
    }
}

$Gradle = Resolve-CicdGradle
$tasks = switch ($Target) {
    'wars' {
        $requestAll = (-not $Codes -or $Codes.Count -eq 0) -or (($Codes | ForEach-Object { $_.ToLowerInvariant() }) -contains 'all')
        if ($requestAll) {
            @('buildZtomcatWars')
        } else {
            $mods = @(Resolve-CicdModules -InputCodes $Codes)
            [string[]]@($mods | ForEach-Object { ":$($_.Module):bootWar" })
        }
    }
    'business' { @('buildBusinessWars') }
    'all'      { @('build') }
    'fast'     { @('build', '-x', 'test') }
}

Invoke-CicdGradle -Gradle $Gradle -Tasks $tasks -DryRun:$DryRun -NoGradleStop:$NoGradleStop

if ($ArtifactDir -and $Target -eq 'wars' -and -not $DryRun) {
    $mods = if ($Codes -and $Codes.Count -gt 0) {
        @(Resolve-CicdModules -InputCodes $Codes)
    } else {
        @(Get-CicdDeployableModules -Modules $CicdAllModules)
    }
    Copy-CicdArtifacts -Modules $mods -ArtifactDir $ArtifactDir
}

Write-Host '[cicd-build] Done.'
