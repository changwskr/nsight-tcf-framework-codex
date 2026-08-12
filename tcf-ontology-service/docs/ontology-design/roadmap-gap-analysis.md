# Step 1 — 현재 tcf-ontology-service 분석

기준 로드맵(조회·검증·추천 서비스):

| 단계 | 목표 | 현재 | 판정 |
| -- | -- | -- | -- |
| 1 | 현재 구조 분석 | 본 문서 | ✅ |
| 2 | Ontology Meta Model | `ontology/core/meta-model.yml` | ✅ |
| 3 | 관계 모델 | `ontology/core/relations.yml` (HAS_SERVICE, HANDLED_BY…) | ✅ |
| 4 | PDMG 샘플 | mappings 5종 (8888/9000/9001/5530/9999) | ✅ |
| 5 | 조회 API | `/service`, `/program`, `/catalog`, `/path` | ✅ |
| 6 | 영향도 API | `/impact?from=` (표준 predicate) | ✅ |
| 7 | 규칙 검증 | `/validate/pdmg`, shapes | ✅ |
| 8 | 신규 시스템 추천 | `/recommend` | ✅ |

## 현재 구조

```text
tcf-ontology-service/
├─ ontology/          기계 정본 (YAML)
│  ├─ core/ business/ technical/ shapes/ rules/ mappings/ versions/
├─ src/.../ontology   Registry 로더
├─ scan/ seed/        소스 스캔·매핑 초안
├─ validate/          shapes + drift
├─ impact/ prompt/    영향도·CRUD 컨텍스트
└─ web/               REST API
```

능력 매핑:

| 미션 | 구현 |
|------|------|
| 축적 | ontology/** + seedPdmg |
| 조회 | service/program/catalog |
| 검증 | validatePdmg |
| 추천 | impact + prompt (부분) |

## 부족한 부분 (우선순위)

1. **Meta Model 미선언** — System / Business / Program / ServiceId / Table 이 YAML 필드로는 있으나, 공식 타입·필수속성 스키마가 없다.
2. **관계 vocabulary 미고정** — `HAS_SERVICE`, `HANDLED_BY`, `PERSISTS_TO` 등 표준 predicate 목록이 문서/코드에 없다. Impact는 ad-hoc 문자열.
3. **계층 경로 질의 약함** — `MG → CO → A → mgcoa8888S0` 를 path API로 뚫는 전용 조회가 없다.
4. **추천 API 공백** — 신규 프로젝트에 “이 패턴 재사용”을 반환하는 `/recommend` 없음.
5. **그래프 저장소 없음** — 현재는 Map 인덱스; 관계 탐색·추론은 메모리 ad-hoc.

## 권장 다음 작업 순서

```text
2 Meta Model 정식 정의 (ontology/core/meta-model.yml)
  → 3 관계 vocabulary (HAS_BUSINESS, HAS_FUNCTION, HAS_PROGRAM, HAS_SERVICE, HANDLED_BY, PERSISTS_TO…)
  → 4 샘플을 관계 인스턴스로 재표현 (또는 mappings에서 유도)
  → 5 path 조회 API 보강
  → 6 impact predicate를 vocabulary에 맞춤
  → 7 검증을 meta/relation 규칙으로 확장
  → 8 POST /api/ontology/recommend
```

## 이미 쓸 수 있는 것

- `GET /api/ontology/service/mgcoa8888S0` — 4축 조회
- `GET /api/ontology/impact?from=TB_FW_IMAGE_LOG` — Table→Program 역추적
- `POST /api/ontology/validate/pdmg` — ServiceId 11자·패키지축·매핑 드리프트
- `GET /api/ontology/prompt/mgcoa8888.md` — 구축 컨텍스트(추천 전단계)
