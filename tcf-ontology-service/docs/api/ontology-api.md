# Ontology API

Base: `http://localhost:8098/api/ontology`

| Method | Path | 설명 |
|--------|------|------|
| GET | `/catalog` | 로드된 bundle/program/service 목록 |
| GET | `/service/{serviceId}` | 4축 조회 (architecture/development/data/operations) |
| GET | `/program/{programId}` | 프로그램 매핑 원본 |
| GET | `/bundle/{path}` | 예: `/bundle/business/classification.yml` |
| POST | `/reload` | classpath ontology 재로딩 |
| GET | `/inventory/pdmg` | pdmg 소스 스캔 결과 |
| POST | `/import/pdmg` | 스캔 + YAML 저장 |
| POST | `/validate/pdmg` | 스캔+shapes/mappings 검증 (FAIL=422) |
| GET | `/impact?from=` | 영향도(nodes/edges/blastRadius, HAS_SERVICE 등) |
| GET | `/path?system&business&function&program` | MG→CO→A→service 계층 조회 |
| GET | `/meta-model` | Meta Model |
| GET | `/relations` | 관계 vocabulary |
| GET/POST | `/recommend` | 신규 시스템 패턴 추천 |
| GET | `/prompt/{id}` | CRUD 프롬프트 JSON 컨텍스트 |
| GET | `/prompt/{id}.md` | CRUD 프롬프트 Markdown 컨텍스트 |

상세: [impact-prompt.md](./impact-prompt.md)

## 데모

```text
GET /api/ontology/service/mgcoa9001S0
GET /api/ontology/impact?from=TB_MG_TX_CONTROL
GET /api/ontology/prompt/mgcoa9001.md
POST /api/ontology/validate/pdmg
```
