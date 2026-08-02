# PDMP Architecture Contract

## Target and boundaries

The harness targets `pdmp-service`, a Java 21 Gradle WAR application. Preserve the current program boundary:

```text
mpcoa8888Controller -> mpcoa8888Service -> mpcoa8888Dao -> mpcoa8888-ORA.xml
```

All program APIs are `POST` endpoints under `/api/mp/co/a/8888`. Use `StandardRequestDto` for input and `StandardResponseDto` for output. Controllers adapt HTTP and call Services only; Services own validation, business behavior, and Spring transactions; DAOs declare MyBatis operations; and mapper XML owns SQL with parameter binding.

## Program and transaction metadata

Use the program identifier `mpcoa8888` and the `nhnis.mp.co.a` package family. Every controller method declares `@TcfTransaction` with `serviceId`, `transactionCode`, `processingType`, and `serviceName`.

| Operation | Endpoint | serviceId | transactionCode | ProcessingType |
|---|---|---|---|---|
| List | `/list` | `MP.SalesTip8888.list` | `MP-INQ-8881` | `INQUIRY` |
| Detail | `/detail` | `MP.SalesTip8888.detail` | `MP-INQ-8882` | `INQUIRY` |
| Create | `/create` | `MP.SalesTip8888.create` | `MP-CRT-8883` | `CREATE` |
| Update | `/update` | `MP.SalesTip8888.update` | `MP-UPD-8884` | `UPDATE` |
| Delete | `/delete` | `MP.SalesTip8888.delete` | `MP-DEL-8885` | `DELETE` |

For new programs, allocate a non-conflicting `MP.{Domain}.{action}` service ID and transaction code before implementation; do not invent identifiers after code has been written.

## Data and transaction rules

The `mpcoa8888` target is `TB_CR_AH_SALES_TIP_RACT` with the composite key `TRT_BRC`, `TRTMN_ENO`, `SALZ_TIP_KDC`, and `BAS_DT`. List and detail are read-only; create, update, and delete use a four-second transaction timeout. Not-found operations raise `MP0404` and duplicate creation raises `MP0409`.

DAO method names and MyBatis statement IDs must agree. Bind every value with MyBatis parameters; never concatenate request values into SQL. Do not change the composite key during update. Confirm deletion semantics from the approved design rather than assuming a logical or physical delete.

## Database environments

H2 is the local development and automated-test database. Oracle is the operational database. Keep compatible SQL and test the H2 path, but record Oracle-only syntax, optimizer hints, drivers, collation, date behavior, or other limitations that have not been verified against an approved Oracle environment.

## Security contract

`/api/mp/co/a/8888/**` remains authenticated through the existing JWT security chain. Existing authenticated users may call the APIs; do not add an unapproved administrator role restriction or expand `permitAll`. Preserve CORS and authentication-failure handling, avoid logging secrets, tokens, sessions, or personal data, and do not expose internal exceptions or stack traces.