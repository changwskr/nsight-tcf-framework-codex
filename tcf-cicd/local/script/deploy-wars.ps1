# local — Tomcat deploy-wars (12 WAR 빌드 + webapps 배포)
param(
    [switch]$SkipSync,
    [switch]$SkipBuild,
    [switch]$NoGradleStop,
    [switch]$Restart,
    [switch]$ExcludeBatch,
    [switch]$SkipBatchCollect,
    [switch]$Help,
    [ValidateSet('dev', 'local')]
    [string]$SyncProfile = 'dev',
    [Parameter(Position = 0, ValueFromRemainingArguments = $true)]
    [string[]]$Codes = @()
)

$ErrorActionPreference = 'Stop'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$CicdRoot = (Resolve-Path (Join-Path $ScriptDir '../..')).Path
$FwRoot = (Resolve-Path (Join-Path $CicdRoot '..')).Path
$ZTomcatHome = Join-Path $FwRoot 'ztomcat'
$CatalinaHome = Join-Path $ZTomcatHome 'apache-tomcat-10.1.34'
$Webapps = Join-Path $CatalinaHome 'webapps'
$BatchWarsDir = Join-Path $ZTomcatHome 'wars'
$SyncScript = Join-Path $CicdRoot 'scripts/sync-to-framework.ps1'

# module:buildWar:deployWar:context
$AllModules = @(
    @{ Module = 'ic-service';  Src = 'ic.war';       Dest = 'ic.war';       Ctx = 'ic' }
    @{ Module = 'pc-service';  Src = 'pc.war';       Dest = 'pc.war';       Ctx = 'pc' }
    @{ Module = 'ms-service';  Src = 'ms.war';       Dest = 'ms.war';       Ctx = 'ms' }
    @{ Module = 'sv-service';  Src = 'sv.war';       Dest = 'sv.war';       Ctx = 'sv' }
    @{ Module = 'pd-service';  Src = 'pd.war';       Dest = 'pd.war';       Ctx = 'pd' }
    @{ Module = 'eb-service';  Src = 'eb.war';       Dest = 'eb.war';       Ctx = 'eb' }
    @{ Module = 'ep-service';  Src = 'ep.war';       Dest = 'ep.war';       Ctx = 'ep' }
    @{ Module = 'ss-service';  Src = 'ss.war';       Dest = 'ss.war';       Ctx = 'ss' }
    @{ Module = 'mg-service';  Src = 'mg.war';       Dest = 'mg.war';       Ctx = 'mg' }
    @{ Module = 'tcf-oc';      Src = 'oc.war';       Dest = 'oc.war';       Ctx = 'oc' }
    @{ Module = 'tcf-om';      Src = 'tcf-om.war';   Dest = 'om.war';       Ctx = 'om' }
    @{ Module = 'tcf-ui';      Src = 'tcf-ui.war';   Dest = 'ui.war';       Ctx = 'ui' }
    @{ Module = 'tcf-uj';      Src = 'tcf-uj.war';   Dest = 'uj.war';       Ctx = 'uj' }
    @{ Module = 'tcf-jwt';     Src = 'jwt.war';      Dest = 'jwt.war';      Ctx = 'jwt' }
    @{ Module = 'tcf-gateway'; Src = 'gw.war';       Dest = 'gw.war';       Ctx = 'gw' }
    @{ Module = 'tcf-batch';   Src = 'tcf-batch.war'; Dest = 'zz-batch.war'; Ctx = 'batch' }
)

$ValidCodes = $AllModules | ForEach-Object { $_.Ctx }

function Test-ModulePresent {
    param([hashtable]$ModuleEntry)
    $buildFile = Join-Path $FwRoot "$($ModuleEntry.Module)/build.gradle"
    return Test-Path -LiteralPath $buildFile
}

function Filter-DeployableModules {
    param(
        [array]$Modules,
        [switch]$RequireAllPresent
    )
    $deployable = @()
    $skipped = @()
    foreach ($m in $Modules) {
        if (Test-ModulePresent $m) {
            $deployable += $m
        } else {
            $skipped += $m
        }
    }
    if ($RequireAllPresent -and $skipped.Count -gt 0) {
        $names = ($skipped | ForEach-Object { $_.Ctx }) -join ', '
        throw "Module source missing in workspace: $names. Restore with: git restore $($skipped.Module -join ' ')"
    }
    if ($skipped.Count -gt 0) {
        $names = ($skipped | ForEach-Object { "$($_.Ctx) ($($_.Module))" }) -join ', '
        Write-Warning "[deploy-wars] skip not in workspace ($($skipped.Count)): $names"
        Write-Warning '[deploy-wars] full 12 WAR: git restore ic-service pc-service ms-service sv-service pd-service eb-service ep-service ss-service mg-service'
    }
    return @($deployable)
}

function Show-Help {
    @"

Usage: deploy-wars.ps1 [codes...] [options]

tcf-cicd 설정 sync -> WAR 빌드 -> ztomcat webapps 배포 (최대 13 context, workspace에 있는 모듈만).

Codes (생략 또는 all = 전체):
  ic pc ms sv pd eb ep ss mg oc om ui uj jwt gw batch
  (별칭: tcf-oc → oc, tcf-jwt → jwt, tcf-gateway → gw, tcf-om → om, tcf-ui → ui, tcf-uj → uj, tcf-batch → batch)

Options:
  -SyncProfile dev|local   framework sync 프로파일 (기본 dev — Tomcat setenv)
  -SkipSync                yml/setenv sync 생략
  -SkipBuild               기존 WAR만 복사 (Gradle 생략)
  -NoGradleStop            gradle --stop 생략
  -Restart                 배포 후 ztomcat/start.ps1 실행
  -ExcludeBatch            zz-batch.war 복사·기동 생략 (deploy-restart 1단계)
  -SkipBatchCollect        batch dashboard 수집 POST 생략

Examples:
  .\deploy-wars.ps1
  .\deploy-wars.ps1 all
  .\deploy-wars.ps1 sv om batch
  .\deploy-wars.ps1 -SkipSync -SkipBuild

Tomcat: $CatalinaHome
Webapps: $Webapps

"@
}

if ($Help) { Show-Help; exit 0 }

function Sync-BatchContextDescriptor {
    $descDir = Join-Path $CatalinaHome 'conf\Catalina\localhost'
    if (-not (Test-Path $descDir)) {
        New-Item -ItemType Directory -Path $descDir -Force | Out-Null
    }
    $stale = Join-Path $descDir 'zz-batch.xml'
    if (Test-Path $stale) {
        Remove-Item -LiteralPath $stale -Force
        Write-Host '  removed stale conf/Catalina/localhost/zz-batch.xml'
    }
    New-Item -ItemType Directory -Force -Path $BatchWarsDir | Out-Null
    $warPath = (Join-Path $BatchWarsDir 'zz-batch.war').Replace('\', '/')
    $xml = @"
<?xml version="1.0" encoding="UTF-8"?>
<Context docBase="$warPath" />
"@
    $dst = Join-Path $descDir 'batch.xml'
    Set-Content -Path $dst -Value $xml -Encoding UTF8
    $src = Join-Path $ZTomcatHome 'conf\Catalina\localhost\batch.xml'
    Set-Content -Path $src -Value $xml -Encoding UTF8
}

function Remove-LegacyBatchArtifacts {
    foreach ($name in @('batch.war', 'batch', 'zz-batch.war', 'zz-batch')) {
        $path = Join-Path $Webapps $name
        Remove-WebappPathIfSafe -Path $path -Label "legacy webapps/$name" | Out-Null
    }
}

function Remove-LegacyOmArtifacts {
    foreach ($name in @('00-om.war', '00-om')) {
        $path = Join-Path $Webapps $name
        Remove-WebappPathIfSafe -Path $path -Label "legacy $name" | Out-Null
    }
}

function Remove-LegacyRetiredBusinessArtifacts {
    foreach ($ctx in @('bc', 'bd', 'bp', 'cc', 'cm', 'cs', 'ct')) {
        foreach ($suffix in @('', '.war')) {
            $path = Join-Path $Webapps "$ctx$suffix"
            Remove-WebappPathIfSafe -Path $path -Label "retired webapps/$ctx$suffix" | Out-Null
        }
    }
}

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

function Normalize-CicdDeployCode {
    param([string]$Code)
    $c = $Code.ToLowerInvariant()
    switch ($c) {
        'tcf-jwt' { return 'jwt' }
        'tcf-gateway' { return 'gw' }
        'gateway' { return 'gw' }
        'tcf-oc' { return 'oc' }
        'tcf-om' { return 'om' }
        'tcf-ui' { return 'ui' }
        'tcf-uj' { return 'uj' }
        'tcf-batch' { return 'batch' }
        default { return $c }
    }
}

function Resolve-Modules {
    param([string[]]$InputCodes)
    if (-not $InputCodes -or $InputCodes.Count -eq 0) {
        return @(Filter-DeployableModules -Modules $AllModules)
    }
    $normalized = @($InputCodes | ForEach-Object { Normalize-CicdDeployCode $_ } | Where-Object {
        $_ -and -not $_.StartsWith('-')
    })
    if ($normalized -contains 'all') {
        return @(Filter-DeployableModules -Modules $AllModules)
    }

    $selected = @()
    foreach ($code in $normalized) {
        $matches = @($AllModules | Where-Object { $_.Ctx -eq $code })
        if ($matches.Count -eq 0) {
            throw "Unknown code: $code. Valid: $($ValidCodes -join ' ')"
        }
        $selected += $matches
    }
    return @(Filter-DeployableModules -Modules $selected -RequireAllPresent)
}

function Test-TomcatRunning {
    try {
        $conn = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
        return $null -ne $conn
    } catch {
        return $false
    }
}

function Remove-WebappPathIfSafe {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [string]$Label = $null
    )
    if (-not (Test-Path -LiteralPath $Path)) {
        return $true
    }
    $name = if ($Label) { $Label } else { Split-Path -Leaf $Path }
    if (Test-TomcatRunning) {
        Write-Host "  skip remove $name (Tomcat running — WAR overwrite triggers autoDeploy)"
        return $true
    }
    for ($attempt = 1; $attempt -le 3; $attempt++) {
        try {
            Remove-Item -LiteralPath $Path -Recurse -Force -ErrorAction Stop
            Write-Host "  removed $name"
            return $true
        } catch {
            if ($attempt -lt 3) {
                Write-Host "  retry remove $name ($attempt/3) ..."
                Start-Sleep -Seconds 2
            } else {
                Write-Warning "  FAIL remove $name — $($_.Exception.Message)"
                return $false
            }
        }
    }
    return $false
}

function Invoke-BatchDashboardCollect {
    $batchBase = 'http://localhost:8080/batch'
    $rollingRedeploy = Test-TomcatRunning
    if ($rollingRedeploy) {
        Write-Host '[deploy-wars] Tomcat autoDeploy — initial wait 20s before batch health poll ...'
        Start-Sleep -Seconds 20
    }
    Write-Host '[deploy-wars] waiting for tcf-batch context (~180s, Hikari init up to 120s) ...'
    $deadline = (Get-Date).AddSeconds(180)
    $up = $false
    do {
        Start-Sleep -Seconds 5
        try {
            $health = Invoke-RestMethod -Uri "$batchBase/actuator/health" -TimeoutSec 10
            if ($health.status -eq 'UP') { $up = $true; break }
        } catch { }
    } while ((Get-Date) -lt $deadline)

    if (-not $up) {
        Write-Warning '[deploy-wars] tcf-batch not UP yet — Health Check DB/AP는 스케줄(5분) 또는 수동 POST /batch/jobs/db-status/run 후 갱신됩니다.'
        return
    }

    Write-Host '[deploy-wars] waiting for batch context to finish redeploy (~40s quiet) ...'
    Start-Sleep -Seconds 40
    $okStreak = 0
    $stableDeadline = (Get-Date).AddMinutes(3)
    do {
        try {
            $health = Invoke-RestMethod -Uri "$batchBase/actuator/health" -TimeoutSec 10
            if ($health.status -eq 'UP') { $okStreak++ } else { $okStreak = 0 }
        } catch { $okStreak = 0 }
        if ($okStreak -ge 2) { break }
        Start-Sleep -Seconds 10
    } while ((Get-Date) -lt $stableDeadline)
    if ($okStreak -lt 2) {
        Write-Warning '[deploy-wars] skip dashboard collect — batch redeploy still in progress'
        return
    }

    foreach ($job in @('db-status', 'ap-status', 'session-status', 'deploy-status')) {
        try {
            Write-Host "[deploy-wars] POST /batch/jobs/$job/run ..."
            Invoke-RestMethod -Method POST -Uri "$batchBase/jobs/$job/run" -TimeoutSec 120 | Out-Null
        } catch {
            Write-Warning "[deploy-wars] batch job $job failed: $($_.Exception.Message)"
        }
    }
    Write-Host '[deploy-wars] OM dashboard status tables refreshed (OM_DB_STATUS 등).'
}

if (-not (Test-Path (Join-Path $CatalinaHome 'bin/catalina.bat')) -and
    -not (Test-Path (Join-Path $CatalinaHome 'bin/catalina.sh'))) {
    throw "Tomcat not found. Run ztomcat/install-tomcat first: $CatalinaHome"
}

if (-not $SkipSync) {
    if (-not (Test-Path $SyncScript)) { throw "sync script not found: $SyncScript" }
    Write-Host "[deploy-wars] sync $SyncProfile config -> framework"
    & $SyncScript -Profile $SyncProfile
}

Remove-LegacyRetiredBusinessArtifacts

$selected = @(Resolve-Modules -InputCodes $Codes)
$requestAll = (-not $Codes -or $Codes.Count -eq 0) -or (($Codes | ForEach-Object { $_.ToLowerInvariant() }) -contains 'all')

if ($ExcludeBatch) {
    Remove-LegacyBatchArtifacts
    $selected = @($selected | Where-Object { $_.Ctx -ne 'batch' })
    Write-Host '[deploy-wars] batch deferred — zz-batch.war will not be copied yet'
}

$Gradle = Resolve-Gradle

Push-Location $FwRoot
try {
    if (-not $SkipBuild) {
        if (-not $NoGradleStop) {
            Write-Host '[deploy-wars] gradle --stop'
            & $Gradle --stop 2>$null | Out-Null
        }

        if ($requestAll) {
            Write-Host '[deploy-wars] gradle buildZtomcatWars'
            & $Gradle buildZtomcatWars
        } else {
            $tasks = [string[]]@($selected | ForEach-Object { ":$($_.Module):bootWar" })
            if ($ExcludeBatch -and ($Codes | ForEach-Object { $_.ToLowerInvariant() }) -contains 'batch') {
                # batch-only phase after defer: ensure tcf-batch is built
                if ($tasks -notcontains ':tcf-batch:bootWar') {
                    $tasks += ':tcf-batch:bootWar'
                }
            }
            Write-Host "[deploy-wars] gradle $($tasks -join ' ')"
            $gradleArgs = [string[]]$tasks
            & $Gradle @gradleArgs
        }
        if ($null -ne $LASTEXITCODE -and $LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    }

    Write-Host '[deploy-wars] removing stale exploded directories ...'
    if ($selected | Where-Object { $_.Ctx -eq 'om' }) {
        Remove-LegacyOmArtifacts
    }
    if ($selected | Where-Object { $_.Ctx -eq 'batch' }) {
        Sync-BatchContextDescriptor
        Remove-LegacyBatchArtifacts
    }
    $removeFailed = @()
    if ($requestAll -and -not $ExcludeBatch) {
        if (Test-TomcatRunning) {
            Write-Host '[deploy-wars] Tomcat running — skip clean-exploded (WAR overwrite triggers autoDeploy)'
        } else {
            & (Join-Path $ZTomcatHome 'clean-exploded.ps1')
            if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        }
    } else {
        foreach ($m in $selected) {
            $dirs = @($m.Ctx)
            if ($m.Ctx -eq 'batch') { $dirs = @('batch', 'zz-batch') }
            foreach ($name in $dirs) {
                $dir = Join-Path $Webapps $name
                if (-not (Remove-WebappPathIfSafe -Path $dir -Label "$name/")) {
                    $removeFailed += $name
                }
            }
        }
    }
    if ($removeFailed.Count -gt 0) {
        throw "Could not remove exploded directories: $($removeFailed -join ', '). Stop Tomcat and retry: ztomcat\stop.ps1"
    }

    Write-Host "[deploy-wars] deploying $($selected.Count) WAR(s) (workspace present) ..."
    $missing = @()
    foreach ($m in $selected) {
        $from = Join-Path $FwRoot "$($m.Module)/build/libs/$($m.Src)"
        if ($m.Ctx -eq 'batch') {
            New-Item -ItemType Directory -Force -Path $BatchWarsDir | Out-Null
            $to = Join-Path $BatchWarsDir $m.Dest
            $destLabel = "wars/$($m.Dest)"
        } else {
            $to = Join-Path $Webapps $m.Dest
            $destLabel = $m.Dest
        }
        if (Test-Path $from) {
            Copy-Item -Force -Path $from -Destination $to
            Write-Host "  deployed $destLabel (from $($m.Src))"
            if ($m.Ctx -eq 'batch') {
                Sync-BatchContextDescriptor
                $batchDesc = Join-Path $CatalinaHome 'conf\Catalina\localhost\batch.xml'
                if (Test-Path $batchDesc) {
                    (Get-Item $batchDesc).LastWriteTime = Get-Date
                }
            }
        } else {
            Write-Host "  missing $($m.Src) in $($m.Module)"
            $missing += $from
        }
    }

    if ($missing.Count -gt 0) {
        $list = ($missing | ForEach-Object { $_ }) -join "`n  "
        throw @"
WAR build output missing for module(s) present in workspace. Run without -SkipBuild or fix Gradle errors.
  $list
"@
    }

    if ($selected.Count -eq 0) {
        throw 'No deployable modules in workspace. Restore service modules or pass explicit codes (e.g. sv om ui batch).'
    }

    $batchDeployed = $selected | Where-Object { $_.Ctx -eq 'batch' }
    if ($batchDeployed -and -not $Restart -and -not $SkipBatchCollect -and -not $ExcludeBatch) {
        if (Test-TomcatRunning) {
            Invoke-BatchDashboardCollect
        } else {
            Write-Host '[deploy-wars] Tomcat not running — skip batch dashboard collect (deploy-restart will collect later)'
        }
    }
}
finally {
    Pop-Location
}

if ($Restart) {
    Write-Host '[deploy-wars] restarting Tomcat ...'
    $startPs1 = Join-Path (Join-Path $CicdRoot 'local/ztomcat') 'start.ps1'
    if (Test-Path $startPs1) {
        & $startPs1 -SkipSync -SkipDeploy
    } else {
        Write-Warning "start.ps1 not found: $startPs1"
    }
} elseif ($selected.Count -eq @($AllModules | Where-Object { Test-ModulePresent $_ }).Count) {
    Write-Host '[deploy-wars] Done. Restart Tomcat if it is already running.'
} else {
    Write-Host '[deploy-wars] Done. Running Tomcat redeploys context automatically (~15s).'
}
