# ztomcat — WAR 배포 + (필요 시) Tomcat 재기동 + health verify
param(
    [switch]$SkipVerify,
    [switch]$Restart,
    [switch]$Help,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Codes
)

$ErrorActionPreference = 'Stop'
$ZTomcatHome = Split-Path -Parent $MyInvocation.MyCommand.Path
$FwRoot = (Resolve-Path (Join-Path $ZTomcatHome '..')).Path
$CicdDeployScript = Join-Path $FwRoot 'tcf-cicd\local\script\deploy-wars.ps1'

function Normalize-DeployCode {
    param([string]$Code)
    $c = $Code.ToLowerInvariant()
    switch ($c) {
        'tcf-jwt' { return 'jwt' }
        'tcf-oc' { return 'oc' }
        'tcf-om' { return 'om' }
        'tcf-ui' { return 'ui' }
        'tcf-batch' { return 'batch' }
        default { return $c }
    }
}

function Get-ZtomcatContextList {
    return @('ic', 'pc', 'ms', 'sv', 'pd', 'eb', 'ep', 'ss', 'mg', 'oc', 'om', 'ui', 'jwt', 'batch')
}

function Show-Help {
    @"

Usage: deploy-restart.ps1 [codes...] [options]

전체(19 WAR): stop -> clean -> deploy(18) -> start -> deploy(batch) -> health 확인
일부 WAR:     Tomcat 유지 -> deploy -> autoDeploy (~15s) -> health 확인

Codes (생략 또는 all = 전체):
  ic pc ms sv pd eb ep ss mg oc om ui jwt batch
  (별칭: tcf-oc → oc, tcf-jwt → jwt, tcf-om → om, tcf-ui → ui, tcf-batch → batch)

Options:
  -SkipVerify   health 폴링/검증 생략
  -Restart      일부 WAR만 배포해도 Tomcat 전체 재기동 (기본: autoDeploy)

Examples:
  .\deploy-restart.ps1
  .\deploy-restart.ps1 oc om ui
  .\deploy-restart.ps1 batch
  .\deploy-restart.ps1 sv om -Restart

Tomcat: $(Join-Path $ZTomcatHome 'apache-tomcat-10.1.34')

"@
}

if ($Help) { Show-Help; exit 0 }

function Resolve-Contexts {
    param([string[]]$InputCodes)
    $all = Get-ZtomcatContextList
    if (-not $InputCodes -or $InputCodes.Count -eq 0) { return @($all) }
    $normalized = @($InputCodes | ForEach-Object { Normalize-DeployCode $_ })
    if ($normalized -contains 'all') { return @($all) }
    $ordered = @()
    foreach ($code in $all) {
        if ($code -in $normalized) { $ordered += $code }
    }
    foreach ($code in $normalized) {
        if ($code -notin $all) {
            throw "Unknown code: $code. Valid: $($all -join ' ')"
        }
    }
    return @($ordered)
}

function Test-TomcatRunning {
    try {
        $conn = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
        return $null -ne $conn
    } catch {
        return $false
    }
}

function Wait-BatchContextStable {
    param(
        [string]$BatchBase = 'http://localhost:8080/batch',
        [int]$MinQuietSeconds = 40,
        [int]$TimeoutMinutes = 4
    )
    Write-Host "[ztomcat] waiting for batch context to finish redeploy (~$MinQuietSeconds s quiet) ..."
    Start-Sleep -Seconds $MinQuietSeconds
    $deadline = (Get-Date).AddMinutes($TimeoutMinutes)
    $okStreak = 0
    do {
        try {
            $health = Invoke-RestMethod -Uri "$BatchBase/actuator/health" -TimeoutSec 10
            if ($health.status -eq 'UP') {
                $okStreak++
            } else {
                $okStreak = 0
            }
        } catch {
            $okStreak = 0
        }
        if ($okStreak -ge 2) {
            Write-Host '[ztomcat] batch context stable (health UP x2)'
            return $true
        }
        Start-Sleep -Seconds 10
    } while ((Get-Date) -lt $deadline)
    Write-Warning '[ztomcat] batch context not stable yet — dashboard collect may be skipped or partial'
    return $false
}

function Invoke-BatchDashboardCollect {
    param(
        [int]$MaxWaitSeconds = 180,
        [switch]$SkipWait
    )
    $batchBase = 'http://localhost:8080/batch'
    $up = $false
    if (-not $SkipWait) {
        Write-Host "[ztomcat] waiting for tcf-batch context (~$MaxWaitSeconds s, Hikari up to 120s) ..."
        $deadline = (Get-Date).AddSeconds($MaxWaitSeconds)
        do {
            Start-Sleep -Seconds 5
            try {
                $health = Invoke-RestMethod -Uri "$batchBase/actuator/health" -TimeoutSec 10
                if ($health.status -eq 'UP') { $up = $true; break }
            } catch { }
        } while ((Get-Date) -lt $deadline)
    } else {
        try {
            $health = Invoke-RestMethod -Uri "$batchBase/actuator/health" -TimeoutSec 10
            $up = ($health.status -eq 'UP')
        } catch { }
    }

    if (-not $up) {
        Write-Warning '[ztomcat] tcf-batch not UP yet — POST /batch/jobs/db-status/run 로 수동 갱신하세요.'
        return
    }

    if (-not (Wait-BatchContextStable -BatchBase $batchBase)) {
        Write-Warning '[ztomcat] skip dashboard collect — batch redeploy still in progress'
        return
    }

    foreach ($job in @('db-status', 'ap-status', 'session-status', 'deploy-status')) {
        try {
            Write-Host "[ztomcat] POST /batch/jobs/$job/run ..."
            Invoke-RestMethod -Method POST -Uri "$batchBase/jobs/$job/run" -TimeoutSec 120 | Out-Null
        } catch {
            Write-Warning "[ztomcat] batch job $job failed: $($_.Exception.Message)"
        }
    }
    Write-Host '[ztomcat] OM dashboard status tables refreshed.'
}

function Invoke-DeployWars {
    param(
        [string[]]$InputCodes,
        [switch]$SkipBuild,
        [switch]$ExcludeBatch,
        [switch]$SkipBatchCollect
    )
    if (Test-Path $CicdDeployScript) {
        $deployArgs = @{
            SkipSync    = $true
            SyncProfile = 'dev'
        }
        if ($SkipBuild) { $deployArgs['SkipBuild'] = $true }
        if ($ExcludeBatch) { $deployArgs['ExcludeBatch'] = $true }
        if ($SkipBatchCollect) { $deployArgs['SkipBatchCollect'] = $true }
        if ($InputCodes -and $InputCodes.Count -gt 0) {
            $deployArgs['Codes'] = @($InputCodes)
        }
        & $CicdDeployScript @deployArgs
        return
    }
    $deployBat = Join-Path $ZTomcatHome 'deploy-wars.bat'
    if ($InputCodes.Count -gt 0) {
        & $deployBat @InputCodes
    } else {
        & $deployBat
    }
}

function Invoke-StartTomcatSkipDeploy {
    $prev = $env:ZTOMCAT_SKIP_DEPLOY
    $env:ZTOMCAT_SKIP_DEPLOY = '1'
    try {
        & (Join-Path $ZTomcatHome 'start.ps1')
    } finally {
        if ($null -eq $prev) {
            Remove-Item Env:ZTOMCAT_SKIP_DEPLOY -ErrorAction SilentlyContinue
        } else {
            $env:ZTOMCAT_SKIP_DEPLOY = $prev
        }
    }
}

function Wait-ContextsHealthy {
    param(
        [string[]]$TargetContexts,
        [string]$Label,
        [int]$TimeoutMinutes = 6
    )
    $targets = @($TargetContexts)
    if ($targets.Count -eq 0) {
        Write-Warning "  $Label health skipped — no target contexts"
        return $false
    }
    $base = 'http://localhost:8080'
    $deadline = (Get-Date).AddMinutes($TimeoutMinutes)
    do {
        Start-Sleep -Seconds 15
        $up = 0
        foreach ($ctx in $targets) {
            try {
                $null = Invoke-WebRequest -Uri "$base/$ctx/actuator/health" -UseBasicParsing -TimeoutSec 5
                $up++
            } catch { }
        }
        Write-Host "  $Label health OK: $up / $($targets.Count)"
        if ($up -eq $targets.Count) { return $true }
    } while ((Get-Date) -lt $deadline)
    return $false
}

$VerifyContexts = @(Resolve-Contexts -InputCodes $Codes)
$PreBatchContexts = @(Get-ZtomcatContextList | Where-Object { $_ -ne 'batch' })
$deployAll = (-not $Codes -or $Codes.Count -eq 0) -or (($Codes | ForEach-Object { $_.ToLowerInvariant() }) -contains 'all')
$fullRestart = $deployAll -or $Restart
$batchLastDeploy = $fullRestart -and (
    (-not $Codes -or $Codes.Count -eq 0) -or
    (($Codes | ForEach-Object { $_.ToLowerInvariant() }) -contains 'all')
)

if ($fullRestart) {
    if ($batchLastDeploy) {
        Write-Host '[ztomcat] Full restart: stop -> clean -> deploy(18) -> start -> deploy(batch) -> verify'
    } else {
        Write-Host '[ztomcat] Full restart: stop -> clean -> deploy -> start -> verify'
    }
} else {
    Write-Host "[ztomcat] Rolling deploy ($($VerifyContexts -join ' ')): Tomcat 유지, autoDeploy -> verify"
}

$tomcatRunning = Test-TomcatRunning

if ($fullRestart) {
    & (Join-Path $ZTomcatHome 'stop.ps1')
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

    $portWait = (Get-Date).AddSeconds(15)
    while ((Get-Date) -lt $portWait -and (Test-TomcatRunning)) {
        Start-Sleep -Seconds 1
    }

    & (Join-Path $ZTomcatHome 'clean-exploded.ps1')
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

if ($batchLastDeploy) {
    Write-Host "[ztomcat] Phase 1: deploy $($PreBatchContexts.Count) WARs (batch excluded) ..."
    Invoke-DeployWars -ExcludeBatch -SkipBatchCollect
} elseif ($Codes.Count -gt 0) {
    $skipCollect = ($Codes.Count -eq 1) -and (($Codes | ForEach-Object { $_.ToLowerInvariant() }) -contains 'batch')
    if ($skipCollect) {
        Invoke-DeployWars -InputCodes $Codes -SkipBatchCollect
    } else {
        Invoke-DeployWars -InputCodes $Codes
    }
} else {
    Invoke-DeployWars -InputCodes @()
}
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if ($fullRestart) {
    Invoke-StartTomcatSkipDeploy
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} elseif (-not $tomcatRunning) {
    Write-Host '[ztomcat] Tomcat not running — starting after deploy ...'
    Invoke-StartTomcatSkipDeploy
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} else {
    Write-Host '[ztomcat] Tomcat running — selected context(s) redeploy automatically (~15s).'
}

$pendingBatchCollect = $false

if ($batchLastDeploy) {
    Write-Host "[ztomcat] Waiting for $($PreBatchContexts.Count) WARs before batch deploy (~5 min) ..."
    if (-not (Wait-ContextsHealthy -TargetContexts $PreBatchContexts -Label 'pre-batch')) {
        Write-Warning '[ztomcat] Not all contexts UP — deploying batch anyway'
    }
    Write-Host '[ztomcat] Phase 2: deploy tcf-batch (zz-batch.war) last ...'
    Invoke-DeployWars -InputCodes @('batch') -SkipBuild -SkipBatchCollect
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    $pendingBatchCollect = $true
} elseif ($VerifyContexts -contains 'batch' -and -not $fullRestart) {
    Write-Host '[ztomcat] Tomcat autoDeploy — waiting before batch health (~20s) ...'
    Start-Sleep -Seconds 20
    if (-not (Wait-ContextsHealthy -TargetContexts @('batch') -Label 'batch' -TimeoutMinutes 4)) {
        Write-Warning '[ztomcat] batch context not UP within 4 min — check catalina log'
    }
    Invoke-BatchDashboardCollect -SkipWait
}

if ($SkipVerify) {
    Write-Host '[ztomcat] Done (verify skipped).'
    exit 0
}

$waitHint = if ($fullRestart) { '~5 min' } else { '~30s per context' }
Write-Host "[ztomcat] Waiting for WAR deployments ($waitHint for $($VerifyContexts.Count) context(s))..."
if (-not (Wait-ContextsHealthy -TargetContexts $VerifyContexts -Label 'final')) {
    Write-Warning '[ztomcat] Timeout waiting for all contexts — running health check anyway'
}

Write-Host '[ztomcat] Health check (GET /{context}/actuator/health)'
$base = 'http://localhost:8080'
$ok = 0
$fail = 0
foreach ($ctx in $VerifyContexts) {
    $url = "$base/$ctx/actuator/health"
    try {
        $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 30
        Write-Host "  OK   $ctx -> $($r.StatusCode)"
        $ok++
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code) {
            Write-Host "  FAIL $ctx -> HTTP $code"
        } else {
            Write-Host "  FAIL $ctx -> $($_.Exception.Message)"
        }
        $fail++
    }
}

Write-Host "[ztomcat] Result: $ok OK, $fail FAIL (total $($VerifyContexts.Count))"
if ($fail -gt 0) { exit 1 }

if ($pendingBatchCollect) {
    Invoke-BatchDashboardCollect
}

Write-Host '[ztomcat] deploy-restart complete.'
