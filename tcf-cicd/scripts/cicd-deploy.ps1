# tcf-cicd — CI/CD 배포 파이프라인 (sync → build → deploy)
param(
    [Parameter(Position = 0)]
    [ValidateSet('full', 'sync', 'build', 'deploy', 'config')]
    [string]$Action = 'full',

    [ValidateSet('local', 'dev', 'prod')]
    [string]$Profile = 'dev',

    [switch]$DryRun,
    [switch]$SkipSync,
    [switch]$SkipBuild,
    [switch]$SkipDeploy,
    [switch]$NoGradleStop,
    [switch]$Restart,
    [switch]$ApplyConfig,
    [switch]$HealthCheck,
    [string]$ArtifactDir = '',
    [string]$GatewayBase = 'http://127.0.0.1:8080',
    [switch]$Help,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Codes = @()
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'cicd-common.ps1')

if ($Help) { Show-CicdDeployHelp; exit 0 }

$requestAll = (-not $Codes -or $Codes.Count -eq 0) -or (($Codes | ForEach-Object { $_.ToLowerInvariant() }) -contains 'all')
$selected = @(Resolve-CicdModules -InputCodes $Codes)
$syncProfile = if ($Profile -eq 'local') { 'local' } else { $Profile }

function Invoke-CicdSync {
    if ($SkipSync) {
        Write-Host '[cicd] skip sync'
        return
    }
    if (-not (Test-Path $SyncScript)) {
        throw "sync script not found: $SyncScript"
    }
    Write-Host "[cicd] sync-to-framework ($syncProfile)"
    if ($DryRun) {
        Write-Host "[dry-run] & $SyncScript -Profile $syncProfile"
        return
    }
    & $SyncScript -Profile $syncProfile
}

function Invoke-CicdBuildStep {
    if ($SkipBuild) {
        Write-Host '[cicd] skip build'
        return
    }
    $Gradle = Resolve-CicdGradle
    $tasks = if ($requestAll) {
        @('buildZtomcatWars')
    } else {
        [string[]]@($selected | ForEach-Object { ":$($_.Module):bootWar" })
    }
    Invoke-CicdGradle -Gradle $Gradle -Tasks $tasks -DryRun:$DryRun -NoGradleStop:$NoGradleStop

    if ($ArtifactDir -and -not $DryRun) {
        Copy-CicdArtifacts -Modules $selected -ArtifactDir $ArtifactDir
    }
}

function Invoke-CicdDeployStep {
    if ($SkipDeploy) {
        Write-Host '[cicd] skip deploy'
        return
    }
    if ($Profile -eq 'prod') {
        Write-Host '[cicd] prod profile — webapps deploy skipped (use -ArtifactDir + remote scp)'
        if ($ApplyConfig) {
            Invoke-CicdConfigStep
        }
        return
    }

    if (-not (Test-Path $DeployWarsScript)) {
        throw "deploy-wars script not found: $DeployWarsScript"
    }

    $deployParams = @{
        SkipSync    = $true
        SyncProfile = $syncProfile
    }
    if ($SkipBuild) { $deployParams.SkipBuild = $true }
    if ($NoGradleStop) { $deployParams.NoGradleStop = $true }
    if ($Restart) { $deployParams.Restart = $true }

    if ($Codes -and $Codes.Count -gt 0 -and -not $requestAll) {
        $deployParams.Codes = $Codes
    }

    $argSummary = ($deployParams.GetEnumerator() | ForEach-Object {
        if ($_.Value -is [array]) { "-$($_.Key) $($_.Value -join ' ')" }
        elseif ($_.Value -is [bool]) { if ($_.Value) { "-$($_.Key)" } }
        else { "-$($_.Key) $($_.Value)" }
    }) -join ' '
    Write-Host "[cicd] deploy-wars $argSummary"
    if ($DryRun) {
        Write-Host "[dry-run] & $DeployWarsScript $argSummary"
        return
    }
    & $DeployWarsScript @deployParams
    if ($null -ne $LASTEXITCODE -and $LASTEXITCODE -ne 0) {
        throw "deploy-wars failed (exit $LASTEXITCODE)"
    }
}

function Invoke-CicdConfigStep {
    if (-not $ApplyConfig -and $Action -ne 'config') { return }

    if (-not (Test-Path $ApplyTomcatConfigScript)) {
        throw "apply-tomcat-config not found: $ApplyTomcatConfigScript"
    }
    if (-not $env:CATALINA_BASE -and -not $env:CATALINA_HOME) {
        throw 'CATALINA_BASE or CATALINA_HOME required for apply-tomcat-config'
    }
    Write-Host '[cicd] apply-tomcat-config prod'
    if ($DryRun) {
        Write-Host "[dry-run] bash $ApplyTomcatConfigScript prod"
        return
    }
    & bash $ApplyTomcatConfigScript prod
    if ($LASTEXITCODE -ne 0) { throw "apply-tomcat-config failed (exit $LASTEXITCODE)" }
}

Write-Host "[cicd] action=$Action profile=$Profile modules=$($selected.Count)"

switch ($Action) {
    'sync' {
        Invoke-CicdSync
    }
    'build' {
        if (-not $SkipSync) { Invoke-CicdSync }
        Invoke-CicdBuildStep
        if ($Profile -eq 'prod' -and $ApplyConfig) { Invoke-CicdConfigStep }
    }
    'deploy' {
        Invoke-CicdDeployStep
        if ($HealthCheck -and -not $DryRun) {
            Invoke-CicdHealthCheck -Modules $selected -GatewayBase $GatewayBase
        }
    }
    'config' {
        $ApplyConfig = $true
        Invoke-CicdConfigStep
    }
    'full' {
        Invoke-CicdSync
        Invoke-CicdBuildStep

        if ($Profile -in @('local', 'dev')) {
            # build 단계에서 이미 WAR 생성 — deploy는 복사만
            $SkipBuild = $true
            Invoke-CicdDeployStep
        } elseif ($Profile -eq 'prod') {
            if (-not $ArtifactDir) {
                Write-Host '[cicd] prod full: WAR built. Copy artifacts to server or pass -ArtifactDir'
            }
            if ($ApplyConfig) { Invoke-CicdConfigStep }
        }

        if ($HealthCheck -and -not $DryRun -and $Profile -in @('local', 'dev')) {
            Invoke-CicdHealthCheck -Modules $selected -GatewayBase $GatewayBase
        }
    }
}

Write-Host '[cicd] Done.'
