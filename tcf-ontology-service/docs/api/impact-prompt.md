# Phase 3 — Impact & Prompt Context

## Impact

```text
GET /api/ontology/impact?from={serviceId|programId|FQCN|table|uiPath|sqlId}
```

예:

- `/api/ontology/impact?from=mgcoa9001S0`
- `/api/ontology/impact?from=TB_MG_TX_CONTROL`
- `/api/ontology/impact?from=nhnis.mg.co.a.application.service.mgcoa9001Service`

응답 핵심:

| 필드 | 의미 |
|------|------|
| nodes/edges | 프로그램 주변 지식 그래프 |
| blastRadius | 아키·개발·데이터·운영 변경 후보 파일 |
| rules | Handler/TX 경계 규칙 |

## Prompt Context (CRUD 연동)

`32.범용CRUD프롬프트.md` 앞에 붙일 컨텍스트:

```text
GET /api/ontology/prompt/{programId}        # JSON
GET /api/ontology/prompt/{programId}.md     # Markdown
```

스크립트:

```bat
scripts\export\prompt-context.bat mgcoa9001
```

산출물: `test-data/queries/prompt-context-mgcoa9001.md`

## 권장 사용 흐름

```text
1) ontology prompt context 생성
2) 32.범용CRUD프롬프트 요구사항 양식 작성
3) impact?from=programId 로 영향 파일 확인
4) 승인 후 구현
5) validate/pdmg 로 드리프트 점검
```
