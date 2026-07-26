# OM_FUNCTION_AUTH 시드 (51건) — H2 TCP 9092
param(
    [string]$TxlogPath = '',
    [int]$TcpPort = 9092
)

$ErrorActionPreference = 'Stop'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$FwRoot = (Resolve-Path (Join-Path $ScriptDir '..\..\..')).Path

if (-not $TxlogPath) {
    $TxlogPath = Join-Path $FwRoot 'data\nsight-txlog'
}
$TxlogPath = (Resolve-Path $TxlogPath).Path

function Resolve-H2Jar {
    $bundled = Get-ChildItem (Join-Path $FwRoot 'ztomcat\lib\h2-*.jar') -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike '*javadoc*' } | Select-Object -First 1
    if ($bundled) { return $bundled.FullName }
    return (Get-ChildItem "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1\com.h2database\h2" -Recurse -Filter 'h2-*.jar' |
        Where-Object { $_.Name -notlike '*javadoc*' -and $_.Name -notlike '*sources*' } |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1).FullName
}

$menus = @(
    'OM_DASH','OM_TX','OM_SVC','OM_AUTH','OM_AUDIT','OM_SES',
    'OM_ERR','OM_BAT','OM_HLT','OM_CFG','OM_FIL','OM_DPL',
    'OM_CDC','OM_FAU','OM_DAU','OM_AHT','OM_CCH'
)

$sql = @()
$sql += 'CREATE TABLE IF NOT EXISTS OM_FUNCTION_AUTH ('
$sql += '  AUTH_ID VARCHAR(64) NOT NULL, AUTH_GROUP_ID VARCHAR(50) NOT NULL, MENU_ID VARCHAR(50),'
$sql += '  CAN_INQUIRY CHAR(1) DEFAULT ''N'', CAN_REGISTER CHAR(1) DEFAULT ''N'','
$sql += '  CAN_UPDATE CHAR(1) DEFAULT ''N'', CAN_DELETE CHAR(1) DEFAULT ''N'', CAN_DOWNLOAD CHAR(1) DEFAULT ''N'','
$sql += '  PRIMARY KEY (AUTH_ID));'
$sql += "DELETE FROM OM_FUNCTION_AUTH WHERE AUTH_ID IN ('FA-001','FA-002','FA-003','FA-004');"

foreach ($m in $menus) {
    $id = "FA-ROLE_ADMIN-$m"
    $sql += "MERGE INTO OM_FUNCTION_AUTH KEY (AUTH_ID) VALUES ('$id','ROLE_ADMIN','$m','Y','Y','Y','Y','Y');"
}

$op = @{
    OM_DASH='Y,N,N,N,Y'; OM_TX='Y,N,N,N,Y'; OM_SVC='Y,N,N,N,N'; OM_AUTH='N,N,N,N,N'
    OM_AUDIT='Y,N,N,N,Y'; OM_SES='Y,N,Y,Y,N'; OM_ERR='Y,N,N,N,N'; OM_BAT='Y,N,Y,N,N'
    OM_HLT='Y,N,N,N,N'; OM_CFG='Y,N,N,N,N'; OM_FIL='Y,Y,N,N,Y'; OM_DPL='Y,N,N,N,N'
    OM_CDC='N,N,N,N,N'; OM_FAU='N,N,N,N,N'; OM_DAU='Y,N,N,N,N'; OM_AHT='Y,N,N,N,N'
    OM_CCH='Y,N,Y,N,N'
}
foreach ($m in $menus) {
    $p = $op[$m].Split(',')
    $id = "FA-ROLE_OPERATOR-$m"
    $sql += "MERGE INTO OM_FUNCTION_AUTH KEY (AUTH_ID) VALUES ('$id','ROLE_OPERATOR','$m','$($p[0])','$($p[1])','$($p[2])','$($p[3])','$($p[4])');"
}

$view = @{
    OM_DASH='Y,N,N,N,N'; OM_TX='Y,N,N,N,N'; OM_SVC='Y,N,N,N,N'; OM_AUTH='N,N,N,N,N'
    OM_AUDIT='Y,N,N,N,N'; OM_SES='N,N,N,N,N'; OM_ERR='N,N,N,N,N'; OM_BAT='N,N,N,N,N'
    OM_HLT='Y,N,N,N,N'; OM_CFG='Y,N,N,N,N'; OM_FIL='N,N,N,N,N'; OM_DPL='N,N,N,N,N'
    OM_CDC='N,N,N,N,N'; OM_FAU='N,N,N,N,N'; OM_DAU='Y,N,N,N,N'; OM_AHT='Y,N,N,N,N'
    OM_CCH='N,N,N,N,N'
}
foreach ($m in $menus) {
    $p = $view[$m].Split(',')
    $id = "FA-ROLE_VIEWER-$m"
    $sql += "MERGE INTO OM_FUNCTION_AUTH KEY (AUTH_ID) VALUES ('$id','ROLE_VIEWER','$m','$($p[0])','$($p[1])','$($p[2])','$($p[3])','$($p[4])');"
}

# ROLE_ADMIN/OPERATOR/VIEWER 외 권한그룹(예: ROLE-TEST) — 조회자 기본 권한
$customGroups = @()
try {
    $groupSql = "SELECT AUTH_GROUP_ID FROM OM_AUTH_GROUP WHERE COALESCE(USE_YN,'Y')='Y' AND AUTH_GROUP_ID NOT IN ('ROLE_ADMIN','ROLE_OPERATOR','ROLE_VIEWER');"
    $groupFile = Join-Path $env:TEMP 'seed-function-auth-groups.sql'
    [System.IO.File]::WriteAllText($groupFile, $groupSql + "`n", [System.Text.UTF8Encoding]::new($false))
    $h2 = Resolve-H2Jar
    $url = "jdbc:h2:tcp://127.0.0.1:$TcpPort/./nsight_om"
    $out = & java -cp $h2 org.h2.tools.Shell -url $url -user sa -script $groupFile 2>&1
    $customGroups = $out | Where-Object { $_ -match '^ROLE-' } | ForEach-Object { $_.Trim() }
} catch {
    Write-Host "[seed-function-auth] WARN: custom auth groups skipped: $_"
}
foreach ($gid in $customGroups) {
    foreach ($m in $menus) {
        $p = $view[$m].Split(',')
        $id = "FA-$gid-$m"
        $sql += "MERGE INTO OM_FUNCTION_AUTH KEY (AUTH_ID) VALUES ('$id','$gid','$m','$($p[0])','$($p[1])','$($p[2])','$($p[3])','$($p[4])');"
    }
    Write-Host "[seed-function-auth] custom group: $gid ($($menus.Count) menus)"
}

$sql += 'SELECT COUNT(*) AS FUNCTION_AUTH_ROWS FROM OM_FUNCTION_AUTH;'
$sql += 'SELECT AUTH_GROUP_ID, COUNT(*) AS C FROM OM_FUNCTION_AUTH GROUP BY AUTH_GROUP_ID;'

$sqlFile = Join-Path $env:TEMP 'seed-function-auth.sql'
[System.IO.File]::WriteAllText($sqlFile, ($sql -join "`n") + "`n", [System.Text.UTF8Encoding]::new($false))

$h2 = Resolve-H2Jar
$url = "jdbc:h2:tcp://127.0.0.1:$TcpPort/./nsight_om"
Write-Host "[seed-function-auth] txlog=$TxlogPath url=$url"

& java -cp $h2 org.h2.tools.RunScript -url $url -user sa -showResults -script $sqlFile
if ($LASTEXITCODE -ne 0) { throw "seed-function-auth failed (exit $LASTEXITCODE)" }
Write-Host '[seed-function-auth] done'
