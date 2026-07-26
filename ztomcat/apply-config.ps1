$ErrorActionPreference = 'Stop'

$ZTomcatHome = Split-Path -Parent $MyInvocation.MyCommand.Path
$CatalinaHome = Join-Path $ZTomcatHome 'apache-tomcat-10.1.34'
$ServerXml = Join-Path $CatalinaHome 'conf\server.xml'
$SetenvSrc = Join-Path $ZTomcatHome 'conf\setenv.bat'
$SetenvDst = Join-Path $CatalinaHome 'bin\setenv.bat'

if (-not (Test-Path $ServerXml)) {
    Write-Host '[ztomcat] server.xml not found. Run install-tomcat.bat first.'
    exit 1
}

Copy-Item -Path $SetenvSrc -Destination $SetenvDst -Force

$LocalhostConfDst = Join-Path $CatalinaHome 'conf\Catalina\localhost'
if (-not (Test-Path $LocalhostConfDst)) {
    New-Item -ItemType Directory -Path $LocalhostConfDst -Force | Out-Null
}

$staleOm = Join-Path $LocalhostConfDst '00-om.xml'
if (Test-Path $staleOm) {
    Remove-Item $staleOm -Force
    Write-Host '[ztomcat] Removed stale Catalina/localhost/00-om.xml'
}

$staleBatchDesc = Join-Path $LocalhostConfDst 'zz-batch.xml'
if (Test-Path $staleBatchDesc) {
    Remove-Item $staleBatchDesc -Force
    Write-Host '[ztomcat] Removed stale Catalina/localhost/zz-batch.xml'
}

$warsDir = Join-Path $ZTomcatHome 'wars'
New-Item -ItemType Directory -Force -Path $warsDir | Out-Null
$legacyWebappsWar = Join-Path $CatalinaHome 'webapps\zz-batch.war'
$targetWar = Join-Path $warsDir 'zz-batch.war'
if ((Test-Path $legacyWebappsWar) -and -not (Test-Path $targetWar)) {
    Copy-Item -Force -Path $legacyWebappsWar -Destination $targetWar
    Write-Host '[ztomcat] Migrated webapps/zz-batch.war -> ztomcat/wars/zz-batch.war'
}
foreach ($legacy in @('zz-batch.war', 'zz-batch', 'batch.war', 'batch')) {
    $p = Join-Path $CatalinaHome "webapps\$legacy"
    if (Test-Path $p) {
        Remove-Item -LiteralPath $p -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "[ztomcat] Removed legacy webapps/$legacy"
    }
}

# Removed business modules — stale WARs cause H2 lock errors on startup
foreach ($legacy in @('bc', 'bd', 'bp', 'cc', 'cm', 'cs', 'ct')) {
    foreach ($suffix in @('', '.war')) {
        $p = Join-Path $CatalinaHome "webapps\$legacy$suffix"
        if (Test-Path $p) {
            Remove-Item -LiteralPath $p -Recurse -Force -ErrorAction SilentlyContinue
            Write-Host "[ztomcat] Removed retired webapps/$legacy$suffix"
        }
    }
}

$warPath = $targetWar.Replace('\', '/')
$batchXml = @"
<?xml version="1.0" encoding="UTF-8"?>
<Context docBase="$warPath" />
"@
$batchDescDst = Join-Path $LocalhostConfDst 'batch.xml'
Set-Content -Path $batchDescDst -Value $batchXml -Encoding UTF8
$batchDescSrc = Join-Path $ZTomcatHome 'conf\Catalina\localhost\batch.xml'
Set-Content -Path $batchDescSrc -Value $batchXml -Encoding UTF8
Write-Host "[ztomcat] batch.xml -> /batch (docBase=$warPath)"

$LoggingProps = Join-Path $CatalinaHome 'conf\logging.properties'
if (Test-Path $LoggingProps) {
    $logging = Get-Content -Path $LoggingProps -Raw -Encoding UTF8
    $handlers = @(
        '1catalina.org.apache.juli.AsyncFileHandler',
        '2localhost.org.apache.juli.AsyncFileHandler',
        '3manager.org.apache.juli.AsyncFileHandler',
        '4host-manager.org.apache.juli.AsyncFileHandler',
        'java.util.logging.ConsoleHandler'
    )
    $changed = $false
    foreach ($handler in $handlers) {
        $key = "${handler}.encoding"
        if ($logging -notmatch [regex]::Escape($key)) {
            $logging = $logging + "`n${key} = UTF-8"
            $changed = $true
        } elseif ($logging -notmatch "${handler}\.encoding\s*=\s*UTF-8") {
            $logging = $logging -replace "(${handler}\.encoding\s*=\s*).+", '${1}UTF-8'
            $changed = $true
        }
    }
    if ($changed) {
        Set-Content -Path $LoggingProps -Value $logging -Encoding UTF8
        Write-Host '[ztomcat] Applied UTF-8 encoding to logging.properties'
    }
}

$content = Get-Content -Path $ServerXml -Raw -Encoding UTF8
if ($content -notmatch 'copyXML="true"') {
    $content = $content -replace '(<Host name="localhost"\s+appBase="webapps"\s+unpackWARs="true"\s+autoDeploy="true")', '$1 copyXML="true"'
    Set-Content -Path $ServerXml -Value $content -Encoding UTF8 -NoNewline
    Write-Host '[ztomcat] Enabled Host copyXML=true'
    $content = Get-Content -Path $ServerXml -Raw -Encoding UTF8
}
if ($content -notmatch 'deployIgnore=') {
    $content = $content -replace '(<Host name="localhost"\s+appBase="webapps"\s+unpackWARs="true"\s+autoDeploy="true"\s+copyXML="true")', '$1 deployIgnore="^zz-batch(\.war)?$"'
    Set-Content -Path $ServerXml -Value $content -Encoding UTF8 -NoNewline
    Write-Host '[ztomcat] Host deployIgnore=^zz-batch(\.war)?$ (autoDeploy only via batch.xml -> /batch)'
    $content = Get-Content -Path $ServerXml -Raw -Encoding UTF8
}
if ($content -notmatch 'URIEncoding="UTF-8"') {
    $content = $content -replace '(<Connector port="8080" protocol="HTTP/1\.1"\s*)', '$1URIEncoding="UTF-8" useBodyEncodingForURI="true" '
    Set-Content -Path $ServerXml -Value $content -Encoding UTF8 -NoNewline
    Write-Host '[ztomcat] Applied UTF-8 Connector settings to server.xml'
}

Write-Host '[ztomcat] Encoding config applied.'
