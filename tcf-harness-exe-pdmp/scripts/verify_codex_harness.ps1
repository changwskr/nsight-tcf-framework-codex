[CmdletBinding()]
param(
    [string]$Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
)

$ErrorActionPreference = 'Stop'

try {
    $manifestPath = Join-Path $Root 'preservation-manifest.json'
    if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) {
        throw "Missing preservation manifest: $manifestPath"
    }

    $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
    if ($manifest.algorithm -ne 'SHA256') {
        throw "Unknown preservation hash algorithm: $($manifest.algorithm)"
    }

    if ($null -eq $manifest.files) {
        throw 'Missing preservation file hashes.'
    }

    foreach ($property in $manifest.files.PSObject.Properties) {
        $relativePath = $property.Name
        $expectedHash = [string]$property.Value
        if ($expectedHash -notmatch '^[0-9A-Fa-f]{64}$') {
            throw "Unknown preservation hash for: $relativePath"
        }

        $fullPath = Join-Path $Root ($relativePath -replace '/', [System.IO.Path]::DirectorySeparatorChar)
        if (-not (Test-Path -LiteralPath $fullPath -PathType Leaf)) {
            throw "Missing preserved file: $relativePath"
        }

        $actualHash = (Get-FileHash -LiteralPath $fullPath -Algorithm SHA256).Hash
        if (-not [string]::Equals($actualHash, $expectedHash, [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "Preservation hash mismatch: $relativePath"
        }
    }

    exit 0
}
catch {
    Write-Error $_
    exit 1
}
