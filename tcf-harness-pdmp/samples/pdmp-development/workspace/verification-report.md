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
