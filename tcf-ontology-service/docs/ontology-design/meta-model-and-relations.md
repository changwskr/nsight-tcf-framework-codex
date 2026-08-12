# Meta Model & Relations (Step 2–3)

- Meta: [`ontology/core/meta-model.yml`](../../ontology/core/meta-model.yml)
- Relations: [`ontology/core/relations.yml`](../../ontology/core/relations.yml)

## Types

System → Business → Function → Program → ServiceId  
(+ JavaClass, Table, MapperXml, SqlId, UiRoute, …)

## Key predicates

| predicate | from → to |
|-----------|-----------|
| HAS_BUSINESS | System → Business |
| HAS_FUNCTION | Business → Function |
| HAS_PROGRAM | Function → Program |
| HAS_SERVICE | Program → ServiceId |
| HANDLED_BY | ServiceId → Handler |
| PERSISTS_TO | Program → Table |

## APIs

```text
GET  /api/ontology/meta-model
GET  /api/ontology/relations
GET  /api/ontology/path?system=MG&business=CO&function=A&program=mgcoa8888S0
GET  /api/ontology/impact?from=TB_FW_IMAGE_LOG
GET  /api/ontology/recommend?system=MG&business=CO&function=A&intent=crud
POST /api/ontology/recommend   { "system":"MG","business":"CO","function":"A","intent":"crud" }
```

예: `MG → CO → A → mgcoa8888 → mgcoa8888S0`
