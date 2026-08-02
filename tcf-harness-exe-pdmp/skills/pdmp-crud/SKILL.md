---
name: pdmp-crud
description: Guide approved PDMP list, detail, create, update, and delete work.
---

# PDMP CRUD

Own list, detail, create, update, and delete patterns only. Keep `Controller -> Service -> DAO -> MyBatis`: controller HTTP and `@TcfTransaction` adaptation, service validation and transaction ownership, DAO declarations, and MyBatis parameter-bound SQL.

Before code, confirm endpoint, DTO, table, composite key, error behavior, service ID, transaction code, processing type, and delete semantics from approved analysis. Write and run a focused RED test, make the minimal change, then retain GREEN evidence.

Never concatenate request values into mapper SQL. Updates do not alter composite keys. A delete is logical or physical only when the approved contract says which; otherwise stop for a decision. Treat H2 as local evidence and report Oracle compatibility as unverified unless verified in an approved environment.
