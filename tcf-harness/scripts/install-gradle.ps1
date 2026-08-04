$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$Version = "8.10.2"
$Target = Join-Path $Root ".gradle-bootstrap"
$Zip = Join-Path $Target "gradle-$Version-bin.zip"
New-Item -ItemType Directory -Force -Path $Target | Out-Null
if (-not (Test-Path $Zip)) {
    Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$Version-bin.zip" -OutFile $Zip
}
$Extract = Join-Path $Target "gradle-$Version"
if (Test-Path $Extract) { Remove-Item -Recurse -Force $Extract }
Expand-Archive -Path $Zip -DestinationPath $Target -Force
Write-Host "Installed Gradle $Version at $Extract"
