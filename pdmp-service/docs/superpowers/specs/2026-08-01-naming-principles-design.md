# Naming Principles Documentation Design

## Goal

Create `docs/네이밍원칙.md` as the single authoritative naming and architecture convention for `pdmp-service`, consolidating and replacing the encoding-damaged `docs/네이밍규칙.md`.

## Scope

- Document Java naming for packages, types, methods, fields, constants, booleans, collections, generics, annotations, enums, exceptions, and tests.
- Define uppercase initialism rules for `TCF`, `STF`, `ETF`, `DTO`, `JWT`, `MDC`, `SQL`, HTTP, IP, and ID, including project-specific exceptions such as `TcfTransaction` and `TcfContext`.
- Document architectural layer naming for controllers, services, DAOs, DTOs, configuration, filters, aspects, exception handlers, and transaction components.
- Document PDMP screen/program naming, including `mpcoa9999`, `nhnis.mp.co.a`, `DtoIn`, `DtoOut`, and `ListResponseDto` compatibility conventions.
- Document API, JSON, HTTP header, database, MyBatis XML, SQL ID, configuration property, Spring bean, logging, error code, resource, and document naming.
- Provide required, recommended, prohibited, good/bad examples, and a new-code review checklist.
- Remove `docs/네이밍규칙.md` after its valid content is incorporated.

## Authority and Compatibility

- `docs/네이밍원칙.md` becomes the canonical document for new code and reviews.
- Existing externally coupled identifiers are not renamed merely to satisfy ideal Java style.
- Existing identifiers such as lowercase-leading `mpcoa9999Controller` remain documented compatibility exceptions.
- The document distinguishes mandatory rules from recommendations and legacy exceptions.

## Architecture Principles Covered

- Framework code stays under `nhnis.fw`; business code stays under `nhnis.mp`.
- Controllers handle transport, services own business rules and transactions, DAOs/MyBatis own persistence, and DTOs carry data only.
- TCF orchestration remains `TCFAspect` calling `STF` before the controller and `ETF` after it.
- Standard errors use `BizException`, `ETF`, `GlobalExceptionHandler`, and error codes defined in `exceptionCode.yml`.
- Logging correlation names remain `guid`, `traceId`, `userId`, `serviceId`, `sqlId`, and `errCode`.

## Verification

- Confirm `docs/네이밍원칙.md` exists in UTF-8 and `docs/네이밍규칙.md` no longer exists.
- Check that the document contains every planned section and references names that exist in current source.
- Search for encoding replacement characters and incomplete placeholders.
- No Java or runtime behavior changes are required.

## Out of Scope

- Renaming existing Java classes, packages, APIs, database objects, or MyBatis statements.
- Modifying `pdmp-ui` or projects outside `pdmp-service`.
- Adding automated naming enforcement tools.
