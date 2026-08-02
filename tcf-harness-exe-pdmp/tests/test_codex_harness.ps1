[CmdletBinding()]
param(
    [ValidateSet('Preservation')]
    [string]$Mode = 'Preservation'
)

$ErrorActionPreference = 'Stop'

function Test-Preservation {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Root
    )

    $manifestPath = Join-Path $Root 'preservation-manifest.json'
    $verifierPath = Join-Path $Root 'scripts/verify_codex_harness.ps1'

    if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) {
        throw "Missing preservation manifest: $manifestPath"
    }

    if (-not (Test-Path -LiteralPath $verifierPath -PathType Leaf)) {
        throw "Missing preservation verifier: $verifierPath"
    }

    $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
    if ($manifest.algorithm -ne 'SHA256') {
        throw "Unknown preservation hash algorithm: $($manifest.algorithm)"
    }

    foreach ($property in $manifest.files.PSObject.Properties) {
        $relativePath = $property.Name
        $expectedHash = [string]$property.Value
        $fullPath = Join-Path $Root ($relativePath -replace '/', [System.IO.Path]::DirectorySeparatorChar)

        if (-not (Test-Path -LiteralPath $fullPath -PathType Leaf)) {
            throw "Missing preserved file: $relativePath"
        }

        if ($expectedHash -notmatch '^[0-9A-Fa-f]{64}$') {
            throw "Unknown preservation hash for: $relativePath"
        }

        $actualHash = (Get-FileHash -LiteralPath $fullPath -Algorithm SHA256).Hash
        if (-not [string]::Equals($actualHash, $expectedHash, [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "Preservation hash mismatch: $relativePath"
        }
    }

    $temporaryParent = Join-Path ([System.IO.Path]::GetTempPath()) ("tcf-harness-preservation-" + [System.Guid]::NewGuid().ToString('N'))
    $temporaryRoot = Join-Path $temporaryParent 'tcf-harness-exe-pdmp'

    try {
        New-Item -ItemType Directory -Path $temporaryParent | Out-Null
        Copy-Item -LiteralPath $Root -Destination $temporaryParent -Recurse

        $mutatedFile = Join-Path $temporaryRoot 'CLAUDE.md'
        $stream = [System.IO.File]::Open($mutatedFile, [System.IO.FileMode]::Append, [System.IO.FileAccess]::Write)
        try {
            $stream.WriteByte(0x21)
        }
        finally {
            $stream.Dispose()
        }

        $previousErrorActionPreference = $ErrorActionPreference
        try {
            $ErrorActionPreference = 'Continue'
            & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $temporaryRoot 'scripts/verify_codex_harness.ps1') -Root $temporaryRoot 2>$null
            $verifierExitCode = $LASTEXITCODE
        }
        finally {
            $ErrorActionPreference = $previousErrorActionPreference
        }

        if ($verifierExitCode -eq 0) {
            throw 'Mutated preservation copy was accepted by the verifier.'
        }
    }
    finally {
        if (Test-Path -LiteralPath $temporaryParent) {
            Remove-Item -LiteralPath $temporaryParent -Recurse -Force
        }
    }
}

switch ($Mode) {
    'Preservation' {
        $root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
        Test-Preservation -Root $root
        Write-Output 'Preservation checks passed.'
    }
}
