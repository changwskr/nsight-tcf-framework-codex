# Git hooks 설치 (Windows)
$ErrorActionPreference = 'Stop'
$Root = Resolve-Path (Join-Path $PSScriptRoot '..\..')
Set-Location $Root
git config core.hooksPath .githooks
Write-Host "Installed git hooks -> .githooks (core.hooksPath)"
Write-Host "Pre-commit: gradle :tcf-help:verifyHelp when HELP-related files change"
