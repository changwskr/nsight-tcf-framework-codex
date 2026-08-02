[CmdletBinding()]
param(
    [ValidateSet('Preservation')]
    [string]$Mode = 'Preservation'
)

$ErrorActionPreference = 'Stop'

function Assert-PreservationVerifierRejects {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Root,

        [Parameter(Mandatory = $true)]
        [string]$MutationDescription
    )

    $previousErrorActionPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $Root 'scripts/verify_codex_harness.ps1') -Root $Root 2>$null
        $verifierExitCode = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }

    if ($verifierExitCode -eq 0) {
        throw "$MutationDescription was accepted by the verifier."
    }
}

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

        Assert-PreservationVerifierRejects -Root $temporaryRoot -MutationDescription 'Mutated preservation copy'

        $manifestMutationRoot = Join-Path $temporaryParent 'manifest-mutation'
        Copy-Item -LiteralPath $Root -Destination $manifestMutationRoot -Recurse

        $manifestMutationPath = Join-Path $manifestMutationRoot 'preservation-manifest.json'
        $manifestMutation = Get-Content -LiteralPath $manifestMutationPath -Raw | ConvertFrom-Json
        [void]$manifestMutation.files.PSObject.Properties.Remove('docs/UI_GUIDE.md')
        $manifestMutation | ConvertTo-Json -Depth 3 | Set-Content -LiteralPath $manifestMutationPath -Encoding utf8

        Assert-PreservationVerifierRejects -Root $manifestMutationRoot -MutationDescription 'Missing manifest entry copy'
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
