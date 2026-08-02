---
name: pdmp-crud
description: Use when an approved pdmp-service change adds or changes list, detail, create, update, or delete behavior for a PDMP program.
---

# PDMP CRUD

## Boundary

Keep Controllers as HTTP and `@TcfTransaction` adapters, Services as validation and transaction owners, DAOs as persistence declarations, and MyBatis XML as parameter-bound SQL. Read [the CRUD checklist](references/crud-checklist.md) with the approved analysis before changing a program.

## Workflow

1. Confirm the endpoint, DTO, database object, composite key, error code, service ID, and transaction code from the approved design.
2. Add the smallest focused test and run it for RED before production edits.
3. Implement only the required list, detail, create, update, or delete path. Do not move business rules into a Controller or DAO.
4. Require every value in mapper XML to use MyBatis parameter binding; never concatenate request data into SQL.
5. Run the same focused test for GREEN and retain command evidence.

## Safe delete decision

The safe delete decision is an approved contract, not an implementation guess.
Do not assume logical or physical deletion. The approved design must explicitly name the delete semantics. If it does not, stop for a decision. For an approved physical delete, bind every four-part key and treat zero affected rows as the specified not-found behavior; for a logical delete, bind the key and the approved update fields.

## Completion checks

Verify the DAO method and mapper statement IDs agree, updates do not change the composite key, not-found and duplicate errors are explicit, and the H2 result is not presented as Oracle validation.
