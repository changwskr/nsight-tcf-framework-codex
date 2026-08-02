---
name: pdmp-tcf
description: Use when a pdmp-service change introduces or changes TCF request handling, @TcfTransaction metadata, service IDs, or transaction boundaries.
---

# PDMP TCF

## Required controller metadata

Each PDMP controller method accepts `StandardRequestDto`, returns `StandardResponseDto`, and declares all `@TcfTransaction` fields:

```java
@TcfTransaction(
    serviceId = "MP.SalesTip8888.list",
    transactionCode = "MP-INQ-8881",
    processingType = ProcessingType.INQUIRY,
    serviceName = "Sales tip list",
    businessCode = "MP"
)
```

`businessCode` is optional and has `default ""`. The current STF implementation fills a blank request-header business code from a nonblank annotation `businessCode`; when both are blank, it derives the first dot-delimited token of `serviceId` (for example, `MP` from `MP.SalesTip8888.list`). A nonblank request-header business code is preserved.

Use the approved `MP.{Domain}.{action}` service ID and transaction code before code is written. Do not create an identifier after implementation to make a test pass.

## Identifier validation

1. Validate every service ID against `^MP\.[A-Za-z][A-Za-z0-9]*\.[a-z][A-Za-z0-9]*$`.
2. Run a duplicate search for the complete candidate in controllers, tests, configuration, SQL/XML resources, and service catalogs before accepting it. For example: `rg -n -F 'MP.SalesTip8888.list' pdmp-service/src`.
3. Validate every transaction code against `^MP-(INQ|CRT|UPD|DEL)-[0-9]{4}$`, then run the same repository-wide duplicate search for the complete code.
4. Check action and ProcessingType consistency:
   - `list` and `detail` use `INQ` and `ProcessingType.INQUIRY`.
   - `create` uses `CRT` and `ProcessingType.CREATE`.
   - `update` uses `UPD` and `ProcessingType.UPDATE`.
   - `delete` uses `DEL` and `ProcessingType.DELETE`.

## Transaction ownership

Controllers adapt standard HTTP only. Services own validation, business errors, and Spring transaction boundaries. List and detail are read-only; approved create, update, and delete operations use the specified four-second timeout. Preserve existing TCF STF/ETF error handling instead of duplicating it.

## Standard header and traceability

Verify standard header handling through the real STF/ETF path:

- Blank `serviceId`, `transactionCode`, `processingType`, and `serviceName` values are filled from `@TcfTransaction`; nonblank client header values are preserved.
- A client-supplied header still satisfies required `businessCode` and `channelId` validation. A synthesized header receives the annotation/default business code without inventing client-only fields.
- `guid` and `traceId` are generated or preserved once, returned in the response/header path, and remain the same identifiers across HTTP, STF, ETF, and error logs.
- MDC contains `guid`, `traceId`, `serviceId`, authenticated `userId`, and client IP while the transaction runs, and the owning filter clears MDC on every success and exception exit.

## Verify

Test each changed controller annotation field, the service transaction behavior, the standard request/response path, blank and nonblank standard header handling, and MDC traceability on success and failure. Record the duplicate-search command and result. Treat any change to serviceId, transactionCode, processingType, or serviceName as a public-contract compatibility risk.
