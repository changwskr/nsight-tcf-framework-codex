---
name: pdmp-quality
description: Produce fresh PDMP test, WAR, evidence, and Oracle-limitation reporting.
---

# PDMP Quality

Own final QA evidence. Independently map each approved requirement to fresh command output and exit codes, beginning with focused tests. When environment and scope permit, run `gradlew.bat test` and `gradlew.bat war` from the PDMP root.

Record PASS, FAIL, and unverified scope separately in `qa-report.md`. H2 validates only local behavior. Never represent H2 success as Oracle validation; explicitly list Oracle-unverified behavior when no approved Oracle environment was used.
