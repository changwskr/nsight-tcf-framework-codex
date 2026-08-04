---
name: pdmp-tcf
description: Validate PDMP TCF transaction metadata, headers, and trace context.
---

# PDMP TCF

Own `serviceId`, `transactionCode`, `processingType`, `@TcfTransaction`, standard header, request/response, and MDC checks. Confirm the controller method uses the approved `MP.{Domain}.{action}` service ID, matching transaction code and processing type before implementation.

Inspect `TcfTransaction`, `STF`, `TCFAspect`, `StandardHeaderDto`, `TcfContextHolder`, and `TcfMdcKeys` in the PDMP root. Verify request headers and trace/MDC context are populated and cleaned by the existing framework paths; do not duplicate framework transaction control in business code.

Search the complete candidate identifier across controllers, tests, configuration, mapper resources, and catalogs before adoption. A missing approved identifier or a header/MDC lifecycle uncertainty is a stop condition.
