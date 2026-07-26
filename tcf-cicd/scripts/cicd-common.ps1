# tcf-cicd 공통 경로·Gradle·모듈 정의 (dot-source)
$ErrorActionPreference = 'Stop'

function Set-CicdJdk21 {
    $candidates = @(
        (Join-Path $env:USERPROFILE '.jdks\temurin-21.0.4')
    )
    $candidates += Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue |
        ForEach-Object { $_.FullName }

    foreach ($jdk in $candidates) {
        if (Test-Path (Join-Path $jdk 'bin\java.exe')) {
            $env:JAVA_HOME = $jdk
            if ($env:Path -notlike "*$jdk\bin*") {
                $env:Path = "$jdk\bin;$env:Path"
            }
            return
        }
    }
    if (-not $env:JAVA_HOME) {
        Write-Warning '[cicd] JDK 21 not found. Set JAVA_HOME or install temurin-21.0.4.'
    }
}

Set-CicdJdk21

if (-not $PSScriptRoot) {
    $PSScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
}

$script:CicdRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$script:FwRoot = (Resolve-Path (Join-Path $CicdRoot '..')).Path
$script:ZTomcatHome = Join-Path $FwRoot 'ztomcat'
$script:CatalinaHome = Join-Path $ZTomcatHome 'apache-tomcat-10.1.34'
$script:WebappsDir = Join-Path $CatalinaHome 'webapps'
$script:BatchWarsDir = Join-Path $ZTomcatHome 'wars'
$script:SyncScript = Join-Path $PSScriptRoot 'sync-to-framework.ps1'
$script:DeployWarsScript = Join-Path $CicdRoot 'local\script\deploy-wars.ps1'
$script:ApplyTomcatConfigScript = Join-Path $PSScriptRoot 'apply-tomcat-config.sh'

# module:buildWar:deployWar:context (OmCicdClientService·deploy-wars.ps1 와 동일)
$script:CicdAllModules = @(
    @{ Module = 'ic-service';  Src = 'ic.war';        Dest = 'ic.war';        Ctx = 'ic' }
    @{ Module = 'pc-service';  Src = 'pc.war';        Dest = 'pc.war';        Ctx = 'pc' }
    @{ Module = 'ms-service';  Src = 'ms.war';        Dest = 'ms.war';        Ctx = 'ms' }
    @{ Module = 'sv-service';  Src = 'sv.war';        Dest = 'sv.war';        Ctx = 'sv' }
    @{ Module = 'pd-service';  Src = 'pd.war';        Dest = 'pd.war';        Ctx = 'pd' }
    @{ Module = 'eb-service';  Src = 'eb.war';        Dest = 'eb.war';        Ctx = 'eb' }
    @{ Module = 'ep-service';  Src = 'ep.war';        Dest = 'ep.war';        Ctx = 'ep' }
    @{ Module = 'ss-service';  Src = 'ss.war';        Dest = 'ss.war';        Ctx = 'ss' }
    @{ Module = 'mg-service';  Src = 'mg.war';        Dest = 'mg.war';        Ctx = 'mg' }
    @{ Module = 'tcf-oc';      Src = 'oc.war';        Dest = 'oc.war';        Ctx = 'oc' }
    @{ Module = 'tcf-om';      Src = 'tcf-om.war';    Dest = 'om.war';        Ctx = 'om' }
    @{ Module = 'tcf-ui';      Src = 'tcf-ui.war';    Dest = 'ui.war';        Ctx = 'ui' }
    @{ Module = 'tcf-jwt';     Src = 'jwt.war';       Dest = 'jwt.war';       Ctx = 'jwt' }
    @{ Module = 'tcf-batch';   Src = 'tcf-batch.war'; Dest = 'zz-batch.war';   Ctx = 'batch' }
)

function Get-CicdValidCodes {
    ,@($script:CicdAllModules | ForEach-Object { $_.Ctx })
}

function Test-CicdModulePresent {
    param([hashtable]$ModuleEntry)
    Test-Path -LiteralPath (Join-Path $script:FwRoot "$($ModuleEntry.Module)/build.gradle")
}

function Get-CicdDeployableModules {
    param(
        [array]$Modules,
        [switch]$RequireAllPresent
    )
    $deployable = @()
    $skipped = @()
    foreach ($m in $Modules) {
        if (Test-CicdModulePresent $m) {
            $deployable += $m
        } else {
            $skipped += $m
        }
    }
    if ($RequireAllPresent -and $skipped.Count -gt 0) {
        $names = ($skipped | ForEach-Object { $_.Ctx }) -join ', '
        throw "Module source missing in workspace: $names"
    }
    if ($skipped.Count -gt 0) {
        $names = ($skipped | ForEach-Object { "$($_.Ctx) ($($_.Module))" }) -join ', '
        Write-Warning "[cicd] skip not in workspace ($($skipped.Count)): $names"
    }
    return @($deployable)
}

function Resolve-CicdModules {
    param([string[]]$InputCodes)
    if (-not $InputCodes -or $InputCodes.Count -eq 0) {
        return @(Get-CicdDeployableModules -Modules $script:CicdAllModules)
    }
    $normalized = @($InputCodes | ForEach-Object {
        $c = $_.ToLowerInvariant()
        switch ($c) {
            'tcf-oc' { 'oc' }
            'tcf-jwt' { 'jwt' }
            'tcf-om' { 'om' }
            'tcf-ui' { 'ui' }
            'tcf-batch' { 'batch' }
            default { $c }
        }
    })
    if ($normalized -contains 'all') {
        return @(Get-CicdDeployableModules -Modules $script:CicdAllModules)
    }
    $selected = @()
    foreach ($code in $normalized) {
        $matches = @($script:CicdAllModules | Where-Object { $_.Ctx -eq $code })
        if ($matches.Count -eq 0) {
            throw "Unknown code: $code. Valid: $((Get-CicdValidCodes) -join ' ')"
        }
        $selected += $matches
    }
    return @(Get-CicdDeployableModules -Modules $selected -RequireAllPresent)
}

function Resolve-CicdGradle {
    $isWin = $env:OS -match 'Windows' -or ($PSVersionTable.PSPlatform -eq 'Win')
    $binName = if ($isWin) { 'gradle.bat' } else { 'gradle' }

    $candidates = @()
    if ($env:GRADLE_HOME_OVERRIDE) {
        $candidates += (Join-Path $env:GRADLE_HOME_OVERRIDE "bin\$binName")
    }
    if ($env:GRADLE_HOME) {
        $candidates += (Join-Path $env:GRADLE_HOME "bin\$binName")
    }
    $cmd = Get-Command gradle -ErrorAction SilentlyContinue
    if ($cmd) { $candidates += $cmd.Source }

    $parent = Split-Path $script:FwRoot -Parent
    if (Test-Path $parent) {
        Get-ChildItem -LiteralPath $parent -Directory -Filter 'gradle-*' -ErrorAction SilentlyContinue |
            ForEach-Object { Join-Path $_.FullName "bin\$binName" } |
            ForEach-Object { $candidates += $_ }
    }

    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path -LiteralPath $candidate)) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }
    throw 'gradle not found. Set GRADLE_HOME (quote paths with parentheses) or add gradle to PATH.'
}

function Invoke-CicdGradle {
    param(
        [string]$Gradle,
        [string[]]$Tasks,
        [switch]$DryRun,
        [switch]$NoGradleStop
    )
    if (-not $NoGradleStop) {
        Write-Host '[cicd] gradle --stop'
        if (-not $DryRun) {
            & $Gradle --stop 2>$null | Out-Null
        }
    }
    $taskLine = $Tasks -join ' '
    Write-Host "[cicd] gradle $taskLine"
    if ($DryRun) { return }

    $isWin = $Gradle.ToLowerInvariant().EndsWith('.bat')
    Push-Location $script:FwRoot
    try {
        if ($isWin) {
            $projectDir = $script:FwRoot
            $cmd = "call `"$Gradle`" -p `"$projectDir`" $($Tasks -join ' ')"
            & cmd.exe /c $cmd
        } else {
            & $Gradle -p $script:FwRoot @Tasks
        }
        if ($null -ne $LASTEXITCODE -and $LASTEXITCODE -ne 0) {
            throw "Gradle failed (exit $LASTEXITCODE): $taskLine"
        }
    }
    finally {
        Pop-Location
    }
}

function Copy-CicdArtifacts {
    param(
        [array]$Modules,
        [string]$ArtifactDir
    )
    if (-not $ArtifactDir) { return }
    $destRoot = $ArtifactDir
    if (-not (Test-Path $destRoot)) {
        New-Item -ItemType Directory -Path $destRoot -Force | Out-Null
    }
    Write-Host "[cicd] staging WAR artifacts -> $destRoot"
    foreach ($m in $Modules) {
        $from = Join-Path $script:FwRoot "$($m.Module)/build/libs/$($m.Src)"
        if (-not (Test-Path -LiteralPath $from)) {
            throw "WAR not found for artifact staging: $from"
        }
        $to = Join-Path $destRoot $m.Dest
        Copy-Item -Force -LiteralPath $from -Destination $to
        Write-Host "  $($m.Dest)"
    }
}

function Invoke-CicdHealthCheck {
    param(
        [array]$Modules,
        [string]$GatewayBase = 'http://127.0.0.1:8080',
        [int]$TimeoutSec = 120
    )
    $base = $GatewayBase.TrimEnd('/')
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    $failed = @()

    foreach ($m in $Modules) {
        if ($m.Ctx -eq 'batch') { continue }
        $url = "$base/$($m.Ctx)/actuator/health"
        $up = $false
        do {
            try {
                $resp = Invoke-RestMethod -Uri $url -TimeoutSec 10
                if ($resp.status -eq 'UP') { $up = $true; break }
            } catch { }
            Start-Sleep -Seconds 3
        } while ((Get-Date) -lt $deadline)

        if ($up) {
            Write-Host "[cicd] health UP  $url"
        } else {
            Write-Warning "[cicd] health DOWN $url"
            $failed += $m.Ctx
        }
    }
    if ($failed.Count -gt 0) {
        throw "Health check failed: $($failed -join ', ')"
    }
}

function Show-CicdDeployHelp {
    @"

Usage: cicd-deploy.ps1 [-Action <stage>] [-Profile <env>] [codes...] [options]

CI/CD 파이프라인 (sync -> build -> deploy -> health).

Actions:
  full     (기본) sync + build + deploy (local/dev) 또는 sync + build + artifact (prod)
  sync     tcf-cicd -> framework yml/setenv 동기화
  build    sync(옵션) + Gradle WAR 빌드
  deploy   기존 WAR를 ztomcat webapps에 배포 (local/dev)
  config   prod Spring yml -> Tomcat conf/nsight (apply-tomcat-config.sh)

Profiles:
  dev      (기본) ztomcat 통합 검증 — sync dev + webapps 배포
  local    개발 PC — sync local
  prod     운영 — sync prod, WAR artifact staging, runtime config

Codes (생략 또는 all = workspace 내 전체 12 context):
  ic pc ms sv pd eb ep ss mg om ui batch

Options:
  -DryRun              sync/build/deploy 명령만 출력
  -SkipSync            sync 생략
  -SkipBuild           Gradle 생략
  -SkipDeploy          webapps 복사 생략
  -NoGradleStop        gradle --stop 생략
  -Restart             배포 후 ztomcat 기동 (local/dev)
  -ApplyConfig         prod: apply-tomcat-config.sh 실행 (CATALINA_BASE 필요)
  -ArtifactDir <path>  빌드 WAR를 CI artifact 디렉터리에 복사
  -HealthCheck         배포 후 gateway health poll
  -GatewayBase <url>   health check base (기본 http://127.0.0.1:8080)

Examples:
  .\cicd-deploy.ps1
  .\cicd-deploy.ps1 -Profile dev -Action full
  .\cicd-deploy.ps1 sv om -Restart
  .\cicd-deploy.ps1 -Profile prod -Action build -ArtifactDir .\artifacts
  .\cicd-deploy.ps1 -Profile prod -Action config -ApplyConfig
  .\cicd-deploy.ps1 -Action sync -Profile local

Framework: $script:FwRoot
Tomcat:    $script:CatalinaHome

"@
}
