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
    serviceName = "Sales tip list"
)
```

Use the approved `MP.{Domain}.{action}` service ID and transaction code before code is written. Do not create an identifier after implementation to make a test pass.

## Transaction ownership

Controllers adapt standard HTTP only. Services own validation, business errors, and Spring transaction boundaries. List and detail are read-only; approved create, update, and delete operations use the specified four-second timeout. Preserve existing TCF STF/ETF error handling instead of duplicating it.

## Verify

Test each changed controller annotation field, service transaction behavior, and standard response path. Record any change to service ID, transaction code, processingType, or serviceName as a public-contract compatibility risk.
