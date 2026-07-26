# tcf-cicd local — WAR 배포 + (필요 시) Tomcat 재기동 + health verify
param(
    [switch]$SkipSync,
    [switch]$SkipVerify,
    [switch]$Restart,
    [switch]$Help,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Codes
)

$ErrorActionPreference = 'Stop'
$LocalZtomcat = Split-Path -Parent $MyInvocation.MyCommand.Path
$CicdRoot = (Resolve-Path (Join-Path $LocalZtomcat '../..')).Path
$FwRoot = (Resolve-Path (Join-Path $CicdRoot '..')).Path
$ZTomcatHome = Join-Path $FwRoot 'ztomcat'
$SyncScript = Join-Path $CicdRoot 'scripts/sync-to-framework.ps1'
$DeployScript = Join-Path (Join-Path $CicdRoot 'local/script') 'deploy-wars.ps1'

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

주의: tcf-cicd/local/ztomcat = PC 로컬 Tomcat 워크플로 폴더이며,
      Spring 프로파일 local(bootRun)과 다릅니다.
      Tomcat WAR는 sync·setenv 모두 dev 프로파일을 사용합니다.
      전체 재기동 시 tcf-batch(zz-batch.war)는 나머지 18개 WAR 기동 후 마지막에 배포됩니다.

Codes (생략 또는 all = 전체):
  ic pc ms sv pd eb ep ss mg oc om ui jwt batch
  (별칭: tcf-oc → oc, tcf-jwt → jwt, tcf-om → om, tcf-ui → ui, tcf-batch → batch)

Options:
  -SkipSync     dev config sync 생략
  -SkipVerify   health 폴링/검증 생략
  -Restart      일부 WAR만 배포해도 Tomcat 전체 재기동 (기본: autoDeploy)

Examples:
  .\deploy-restart.ps1
  .\deploy-restart.ps1 om ui
  .\deploy-restart.ps1 sv om -Restart
  .\deploy-restart.ps1 -SkipSync

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
    Write-Host "[local-ztomcat] waiting for batch context to finish redeploy (~$MinQuietSeconds s quiet) ..."
    Start-Sleep -Seconds $MinQuietSeconds
    $deadline = (Get-Date).AddMinutes($TimeoutMinutes)
    $okStreak = 0
    do {
        try {
            $health = Invoke-RestMethod -Uri "$BatchBase/actuator/health" -TimeoutSec 10
            if ($health.status -eq 'UP') { $okStreak++ } else { $okStreak = 0 }
        } catch { $okStreak = 0 }
        if ($okStreak -ge 2) {
            Write-Host '[local-ztomcat] batch context stable (health UP x2)'
            return $true
        }
        Start-Sleep -Seconds 10
    } while ((Get-Date) -lt $deadline)
    Write-Warning '[local-ztomcat] batch context not stable yet — dashboard collect may be skipped or partial'
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
        Write-Host "[local-ztomcat] waiting for tcf-batch context (~$MaxWaitSeconds s, Hikari up to 120s) ..."
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
        Write-Warning '[local-ztomcat] tcf-batch not UP yet — Tomcat catalina.out 또는 /batch/actuator/health 확인. 수동: POST /batch/jobs/db-status/run'
        return
    }

    if (-not (Wait-BatchContextStable -BatchBase $batchBase)) {
        Write-Warning '[local-ztomcat] skip dashboard collect — batch redeploy still in progress'
        return
    }

    foreach ($job in @('db-status', 'ap-status', 'session-status', 'deploy-status')) {
        try {
            Write-Host "[local-ztomcat] POST /batch/jobs/$job/run ..."
            Invoke-RestMethod -Method POST -Uri "$batchBase/jobs/$job/run" -TimeoutSec 120 | Out-Null
        } catch {
            Write-Warning "[local-ztomcat] batch job $job failed: $($_.Exception.Message)"
        }
    }
    Write-Host '[local-ztomcat] OM dashboard status tables refreshed.'
}

function Invoke-DeployWars {
    param(
        [string[]]$InputCodes,
        [switch]$SkipBuild,
        [switch]$ExcludeBatch,
        [switch]$SkipBatchCollect
    )
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
    & $DeployScript @deployArgs
}

function Invoke-StartTomcatSkipDeploy {
    & (Join-Path $LocalZtomcat 'start.ps1') -SkipSync -SkipDeploy
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
# 전체 재기동(코드 생략 또는 all)이면 batch는 2단계로 미룸
$batchLastDeploy = $fullRestart -and (
    (-not $Codes -or $Codes.Count -eq 0) -or
    (($Codes | ForEach-Object { $_.ToLowerInvariant() }) -contains 'all')
)

if ($fullRestart) {
    if ($batchLastDeploy) {
        Write-Host '[local-ztomcat] Full restart: stop -> clean -> deploy(18) -> start -> deploy(batch) -> verify'
    } else {
        Write-Host '[local-ztomcat] Full restart: stop -> clean -> deploy -> start -> verify'
    }
} else {
    Write-Host "[local-ztomcat] Rolling deploy ($($VerifyContexts -join ' ')): Tomcat 유지, autoDeploy -> verify"
}

if (-not $SkipSync) {
    if (-not (Test-Path $SyncScript)) { throw "sync script not found: $SyncScript" }
    Write-Host '[local-ztomcat] sync dev config -> framework'
    & $SyncScript -Profile dev
}

$tomcatRunning = Test-TomcatRunning

if ($fullRestart) {
    & (Join-Path $LocalZtomcat 'stop.ps1')
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

    $portWait = (Get-Date).AddSeconds(15)
    while ((Get-Date) -lt $portWait -and (Test-TomcatRunning)) {
        Start-Sleep -Seconds 1
    }
    if (Test-TomcatRunning) {
        Write-Warning '[local-ztomcat] port 8080 still listening after stop — batch collect may run prematurely'
    }

    & (Join-Path $ZTomcatHome 'clean-exploded.ps1')
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

if ($batchLastDeploy) {
    Write-Host "[local-ztomcat] Phase 1: deploy $($PreBatchContexts.Count) WARs (batch excluded) ..."
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
    Write-Host '[local-ztomcat] Tomcat not running — starting after deploy ...'
    Invoke-StartTomcatSkipDeploy
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} else {
    Write-Host '[local-ztomcat] Tomcat running — selected context(s) redeploy automatically (~15s).'
}

$pendingBatchCollect = $false

if ($batchLastDeploy) {
    Write-Host "[local-ztomcat] Waiting for $($PreBatchContexts.Count) WARs before batch deploy (~5 min) ..."
    if (-not (Wait-ContextsHealthy -TargetContexts $PreBatchContexts -Label 'pre-batch')) {
        Write-Warning '[local-ztomcat] Not all contexts UP — deploying batch anyway'
    }
    Write-Host '[local-ztomcat] Phase 2: deploy tcf-batch (zz-batch.war) last ...'
    Invoke-DeployWars -InputCodes @('batch') -SkipBuild -SkipBatchCollect
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    $pendingBatchCollect = $true
} elseif ($VerifyContexts -contains 'batch' -and -not $fullRestart) {
    Write-Host '[local-ztomcat] Tomcat autoDeploy — waiting before batch health (~20s) ...'
    Start-Sleep -Seconds 20
    if (-not (Wait-ContextsHealthy -TargetContexts @('batch') -Label 'batch' -TimeoutMinutes 4)) {
        Write-Warning '[local-ztomcat] batch context not UP within 4 min — check catalina log'
    }
    Invoke-BatchDashboardCollect -SkipWait
}

if ($SkipVerify) {
    Write-Host '[local-ztomcat] Done (verify skipped).'
    exit 0
}

$waitHint = if ($fullRestart) { '~5 min' } else { '~30s per context' }
Write-Host "[local-ztomcat] Waiting for WAR deployments ($waitHint for $($VerifyContexts.Count) context(s))..."
if (-not (Wait-ContextsHealthy -TargetContexts $VerifyContexts -Label 'final')) {
    Write-Warning '[local-ztomcat] Timeout waiting for all contexts — running health check anyway'
}

Write-Host '[local-ztomcat] Health check (GET /{context}/actuator/health)'
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

Write-Host "[local-ztomcat] Result: $ok OK, $fail FAIL (total $($VerifyContexts.Count))"
if ($fail -gt 0) { exit 1 }

if ($pendingBatchCollect) {
    Invoke-BatchDashboardCollect
}

Write-Host '[local-ztomcat] deploy-restart complete.'
