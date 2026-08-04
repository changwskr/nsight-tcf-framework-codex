---
name: pdmp-security
description: Review PDMP JWT, CORS, authorization, SQL, secrets, privacy, logging, and error exposure.
---

# PDMP Security

Own security review for JWT, CORS, authorization, SQL binding, secrets, privacy, logging, and error exposure. Inspect `SecurityConfig` and `JwtAuthenticationFilter` before modifying protected routes or authentication behavior.

Verify protected endpoints remain authenticated; do not broaden `permitAll` or add bypasses. Require parameter binding for every SQL value. Do not expose stack traces or log credentials, JWTs, session identifiers, secrets, or personal data. Report findings and a pass/remediate decision in `security-review.md`; block QA until material findings are resolved or explicitly accepted.
