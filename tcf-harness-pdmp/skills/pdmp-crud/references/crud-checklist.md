# PDMP CRUD Checklist

| Action | Required contract |
| --- | --- |
| list | Read-only query, pagination/count contract, and parameter-bound criteria. |
| detail | Read-only lookup by the complete key and `MP0404` when the approved service contract says not found. |
| create | Validate input in Service, reject duplicate keys with `MP0409`, and bind insert values. |
| update | Preserve `TRT_BRC`, `TRTMN_ENO`, `SALZ_TIP_KDC`, and `BAS_DT`; bind changed values and return not-found on zero rows. |
| delete | Obtain the approved safe delete decision before coding; bind the complete key and return not-found on zero rows. |

## Before GREEN

- Controller -> Service -> DAO -> MyBatis remains intact.
- DAO method names equal MyBatis statement IDs.
- Every SQL value uses MyBatis parameter binding (`#{...}`); no request value is concatenated into SQL.
- Tests cover the changed observable boundary on H2.
- Oracle syntax, hints, dates, collation, driver behavior, and optimizer assumptions are recorded as unverified unless tested in Oracle.
