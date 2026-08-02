[CmdletBinding()]
param(
    [string]$Workspace = (Join-Path $PSScriptRoot '..\workspace'),
    [string]$RunDirectory = (Join-Path $PSScriptRoot '..\_runs'),
    [switch]$OmitSecurityReview
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)

function Write-Utf8Atomically {
    param(
        [Parameter(Mandatory)]
        [string]$Path,
        [Parameter(Mandatory)]
        [string]$Content
    )

    $parent = Split-Path -Parent $Path
    [System.IO.Directory]::CreateDirectory($parent) | Out-Null
    $temporaryPath = Join-Path $parent ('.{0}.{1}.tmp' -f [System.IO.Path]::GetFileName($Path), [guid]::NewGuid())
    try {
        [System.IO.File]::WriteAllText($temporaryPath, $Content, $utf8NoBom)
        Move-Item -LiteralPath $temporaryPath -Destination $Path -Force
    }
    finally {
        if (Test-Path -LiteralPath $temporaryPath) {
            Remove-Item -LiteralPath $temporaryPath -Force
        }
    }
}

$analysis = @'
# PDMP Analysis Summary

## Scope
Simulate an approved mpcoa8888 list change without changing pdmp-service.

## Contract facts
The request remains inside Controller -> Service -> DAO -> MyBatis and uses MP.SalesTip8888.list.

## Decisions
No API, schema, authentication, or delete-policy change is requested.

## Risks and evidence
H2 verification is represented by the builder report; Oracle compatibility remains unverified.
'@
$verification = @'
# PDMP Verification Report

## Changed files
Offline simulation artifacts only; pdmp-service source is unchanged.

## Verification commands and exit codes
- Command: `powershell -NoProfile -ExecutionPolicy Bypass -File ./tcf-harness-pdmp/tests/test-pdmp-harness.ps1 -Mode Simulation`
  Exit code: 0
  Relevant output: `Simulation checks passed.`
- Command: `powershell -NoProfile -ExecutionPolicy Bypass -File ./tcf-harness-pdmp/scripts/verify-pdmp-harness.ps1`
  Exit code: 0
  Relevant output: `PDMP harness verification passed.`

The sample QA conclusion may use PASS only when `verification-report.md` contains these successful exit codes and the required security review exists.

## Unverified scope
No H2, Oracle, Gradle test, or WAR command is run because this is an offline harness simulation.
'@
$security = @'
# PDMP Security Review

## Findings
No source change widens /api/mp/co/a/8888/** or alters JWT, CORS, SQL, logs, secrets, or error responses.

## Evidence inspected
The simulation preserves the required review handoff before QA.

## Residual risk
This offline artifact is not a substitute for review of a real pdmp-service diff.
'@
$qaPass = @'
# PDMP QA Report

## Requirement evidence
`verification-report.md` records the Simulation acceptance command and project-local verifier with `Exit code: 0`. The required security-review artifact is present.

## Evidence basis
- Simulation acceptance: `Exit code: 0`.
- Project-local verifier: `Exit code: 0`.
- Security review: present with no unresolved finding for this offline sample.

## Conclusion
PASS is based on the successful command evidence above and the completed security review. Oracle behavior remains unverified.
'@
$qaFail = @'
# PDMP QA Report

## Requirement evidence
Analysis and verification artifacts are present, but the required security review is absent for this simulation run.

## Executed command
The deterministic offline simulator ran with -OmitSecurityReview.

## Conclusion
FAIL. QA cannot approve without security-review.md.
'@

try {
    Write-Utf8Atomically -Path (Join-Path $Workspace 'analysis-summary.md') -Content $analysis
    Write-Utf8Atomically -Path (Join-Path $Workspace 'verification-report.md') -Content $verification

    if ($OmitSecurityReview) {
        $securityReviewPath = Join-Path $Workspace 'security-review.md'
        if (Test-Path -LiteralPath $securityReviewPath -PathType Leaf) {
            Remove-Item -LiteralPath $securityReviewPath -Force
        }
        Write-Utf8Atomically -Path (Join-Path $Workspace 'qa-report.md') -Content $qaFail
        Write-Utf8Atomically -Path (Join-Path $RunDirectory 'pdmp-development-simulation.log') -Content "QA FAIL`nsecurity-review.md omitted for this simulation run.`n"
        Write-Host 'QA FAIL: security-review.md omitted.'
        exit 1
    }

    Write-Utf8Atomically -Path (Join-Path $Workspace 'security-review.md') -Content $security
    Write-Utf8Atomically -Path (Join-Path $Workspace 'qa-report.md') -Content $qaPass
    Write-Utf8Atomically -Path (Join-Path $RunDirectory 'pdmp-development-simulation.log') -Content "QA PASS`nAll four PDMP handoff artifacts are present.`n"
    Write-Host 'QA PASS: four PDMP handoff artifacts generated.'
    exit 0
}
catch {
    Write-Error "PDMP simulation failed: $($_.Exception.Message)"
    exit 2
}
