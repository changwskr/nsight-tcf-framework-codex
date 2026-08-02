#!/usr/bin/env sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
sample_root=$(CDPATH= cd -- "$script_dir/.." && pwd)
workspace="$sample_root/workspace"
run_directory="$sample_root/_runs"
omit_security_review=''

if [ "$#" -ge 1 ]; then workspace=$1; fi
if [ "$#" -ge 2 ]; then run_directory=$2; fi
if [ "$#" -ge 3 ]; then omit_security_review=$3; fi

temporary_files=''

cleanup() {
    if [ -n "$temporary_files" ]; then
        rm -f -- $temporary_files
    fi
}
trap cleanup EXIT HUP INT TERM

write_atomically() {
    destination=$1
    directory=$(dirname -- "$destination")
    filename=$(basename -- "$destination")
    mkdir -p -- "$directory"
    temporary_path=$(mktemp "$directory/.$filename.XXXXXX")
    temporary_files="$temporary_files $temporary_path"
    cat > "$temporary_path"
    mv -f -- "$temporary_path" "$destination"
    temporary_files=$(printf '%s' "$temporary_files" | sed "s| $temporary_path||")
}

write_atomically "$workspace/analysis-summary.md" <<'EOF'
# PDMP Analysis Summary

## Scope
Simulate an approved mpcoa8888 list change without changing pdmp-service.

## Contract facts
The request remains inside Controller -> Service -> DAO -> MyBatis and uses MP.SalesTip8888.list.

## Decisions
No API, schema, authentication, or delete-policy change is requested.

## Risks and evidence
H2 verification is represented by the builder report; Oracle compatibility remains unverified.
EOF

write_atomically "$workspace/verification-report.md" <<'EOF'
# PDMP Verification Report

## Changed files
Offline simulation artifacts only; pdmp-service source is unchanged.

## RED and GREEN evidence
The focused simulation test first fails without the simulator and then passes after generation.

## Unverified scope
No H2, Oracle, Gradle test, or WAR command is run because this is an offline harness simulation.
EOF

if [ "$omit_security_review" = "--omit-security-review" ]; then
    write_atomically "$workspace/qa-report.md" <<'EOF'
# PDMP QA Report

## Requirement evidence
Analysis and verification artifacts are present, but the required security review is absent for this simulation run.

## Executed command
The deterministic offline simulator ran with the omitted-security switch.

## Conclusion
FAIL. QA cannot approve without security-review.md.
EOF
    write_atomically "$run_directory/pdmp-development-simulation.log" <<'EOF'
QA FAIL
security-review.md omitted for this simulation run.
EOF
    printf '%s\n' 'QA FAIL: security-review.md omitted.'
    exit 1
fi

write_atomically "$workspace/security-review.md" <<'EOF'
# PDMP Security Review

## Findings
No source change widens /api/mp/co/a/8888/** or alters JWT, CORS, SQL, logs, secrets, or error responses.

## Evidence inspected
The simulation preserves the required review handoff before QA.

## Residual risk
This offline artifact is not a substitute for review of a real pdmp-service diff.
EOF

write_atomically "$workspace/qa-report.md" <<'EOF'
# PDMP QA Report

## Requirement evidence
Analysis, verification, and security-review artifacts are present for the required handoffs.

## Executed command
The deterministic offline simulator generated this report.

## Conclusion
PASS. Oracle behavior remains unverified.
EOF

write_atomically "$run_directory/pdmp-development-simulation.log" <<'EOF'
QA PASS
All four PDMP handoff artifacts are present.
EOF
printf '%s\n' 'QA PASS: four PDMP handoff artifacts generated.'
