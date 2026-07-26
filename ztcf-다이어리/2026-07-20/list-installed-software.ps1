#Requires -Version 5.1
<#
.SYNOPSIS
  Windows installed software list -> Korean-header CSV

.PARAMETER OutFile
  Output CSV path

.PARAMETER Open
  Open CSV after save

.EXAMPLE
  .\list-installed-software.ps1
  .\list-installed-software.ps1 -Open
#>

[CmdletBinding()]
param(
    [string]$OutFile = "",
    [switch]$Open
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if ([string]::IsNullOrWhiteSpace($OutFile)) {
    $stamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $OutFile = Join-Path $ScriptDir ("installed_software_{0}.csv" -f $stamp)
}

# Korean column headers (UTF-8 source file)
$H_Name = [string]([char]0xD504) + [char]0xB85C + [char]0xADF8 + [char]0xB7A8 + [char]0xBA85
$H_Ver  = [string]([char]0xC124) + [char]0xCE58 + " " + [char]0xBC84 + [char]0xC804
$H_Pub  = [string]([char]0xC81C) + [char]0xC870 + [char]0xC0AC
$H_Date = [string]([char]0xC124) + [char]0xCE58 + [char]0xC77C
$H_Loc  = [string]([char]0xC124) + [char]0xCE58 + " " + [char]0xC704 + [char]0xCE58
$H_Size = [string]([char]0xB300) + [char]0xB7B5 + [char]0xC801 + [char]0xC778 + " " + [char]0xC124 + [char]0xCE58 + [char]0xC6A9 + [char]0xB7C9
$H_Bit  = "32" + [char]0xBE44 + [char]0xD2B8 + [char]0x00B7 + "64" + [char]0xBE44 + [char]0xD2B8 + " " + [char]0xD504 + [char]0xB85C + [char]0xADF8 + [char]0xB7A8 + " " + [char]0xAD6C + [char]0xBD84

function Format-InstallDate([string]$raw) {
    if ([string]::IsNullOrWhiteSpace($raw)) { return "" }
    if ($raw -match '^\d{8}$') {
        return "{0}-{1}-{2}" -f $raw.Substring(0, 4), $raw.Substring(4, 2), $raw.Substring(6, 2)
    }
    return $raw
}

function Format-ApproxSize($estimatedSizeKb) {
    if ($null -eq $estimatedSizeKb -or $estimatedSizeKb -eq "") { return "" }
    try {
        $kb = [double]$estimatedSizeKb
        if ($kb -le 0) { return "" }
        if ($kb -ge 1024) { return ("{0:N1} MB" -f ($kb / 1024)) }
        return ("{0:N0} KB" -f $kb)
    } catch {
        return ""
    }
}

function Resolve-Bitness([string]$registryPath, [string]$installLocation) {
    $label32 = "32" + [char]0xBE44 + [char]0xD2B8
    $label64 = "64" + [char]0xBE44 + [char]0xD2B8
    $labelUn = [string]([char]0xBBF8) + [char]0xD655 + [char]0xC778

    if ($registryPath -match "WOW6432Node") { return $label32 }
    if ($installLocation -match "(?i)\\Program Files \(x86\)\\") { return $label32 }
    if ($installLocation -match "(?i)\\Program Files\\") { return $label64 }
    if ($registryPath -match "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall") {
        return $label64
    }
    return $labelUn
}

$sources = @(
    "HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*",
    "HKLM:\Software\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall\*",
    "HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*"
)

Write-Host "[installed-software] scanning uninstall registry..."

$collected = New-Object System.Collections.Generic.List[object]

foreach ($regPath in $sources) {
    Get-ItemProperty -Path $regPath -ErrorAction SilentlyContinue |
        Where-Object { $_.DisplayName } |
        ForEach-Object {
            $psPath = [string]$_.PSPath
            $loc = [string]$_.InstallLocation
            $obj = [pscustomobject]@{
                Name      = [string]$_.DisplayName
                Version   = [string]$_.DisplayVersion
                Publisher = [string]$_.Publisher
                Date      = (Format-InstallDate ([string]$_.InstallDate))
                Location  = $loc
                Size      = (Format-ApproxSize $_.EstimatedSize)
                Bitness   = (Resolve-Bitness -registryPath $psPath -installLocation $loc)
            }
            [void]$collected.Add($obj)
        }
}

$deduped = $collected |
    Sort-Object Name, Version, Bitness |
    Group-Object Name, Version, Bitness |
    ForEach-Object { $_.Group | Select-Object -First 1 }

$export = foreach ($row in $deduped) {
    $o = New-Object psobject
    Add-Member -InputObject $o -NotePropertyName $H_Name -NotePropertyValue $row.Name
    Add-Member -InputObject $o -NotePropertyName $H_Ver  -NotePropertyValue $row.Version
    Add-Member -InputObject $o -NotePropertyName $H_Pub  -NotePropertyValue $row.Publisher
    Add-Member -InputObject $o -NotePropertyName $H_Date -NotePropertyValue $row.Date
    Add-Member -InputObject $o -NotePropertyName $H_Loc  -NotePropertyValue $row.Location
    Add-Member -InputObject $o -NotePropertyName $H_Size -NotePropertyValue $row.Size
    Add-Member -InputObject $o -NotePropertyName $H_Bit  -NotePropertyValue $row.Bitness
    $o
}

$outDir = Split-Path -Parent $OutFile
if ($outDir -and -not (Test-Path -LiteralPath $outDir)) {
    New-Item -ItemType Directory -Path $outDir -Force | Out-Null
}

$export | Export-Csv -LiteralPath $OutFile -NoTypeInformation -Encoding UTF8

Write-Host ("[installed-software] count = {0}" -f @($export).Count)
Write-Host ("[installed-software] saved = {0}" -f $OutFile)

if ($Open) {
    Invoke-Item -LiteralPath $OutFile
}