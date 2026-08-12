# Architect Workbench UI Analysis

- 작성일: 2026-08-10
- 기준 문서: `zdiary/.../26-08-10-10-Architect_Workbench_설계서.md` §35 Phase 0
- 대상: `tcf-ontology-service`

---

## 1. 결론 (스택 결정)

| 항목 | 현황 | 결정 |
|------|------|------|
| React / Vue | **없음** | 도입하지 않음 |
| Thymeleaf | **없음** (의존성·templates 없음) | 도입하지 않음 |
| 기존 UI 패턴 (모노레포) | `pdmg-ui` 등 **static HTML + vanilla JS + CSS** | **동일 패턴 채택** |
| Graph Library | 없음 | 1차는 **리스트/체인 텍스트** (과도한 그래프 시각화 금지) |
| 인증/권한 | ontology-service에 UI 인증 없음 | 1차는 **미적용** (로컬 Architect 콘솔) |

**1차 UI 구현 방식:**  
`src/main/resources/static/workbench/` 아래 Hash SPA (Home / Search / Impact / Gate)  
실제 `/api/ontology/**` 호출, Mock 금지.

---

## 2. 현재 프로젝트 UI 상태

| 점검 | 결과 |
|------|------|
| `src/main/resources/static/**` | **없음** |
| `templates/` | **없음** |
| `build.gradle` UI 관련 | `spring-boot-starter-web` only (Thymeleaf/WebFlux UI 없음) |
| Router | REST Controllers only |
| API Client | 없음 (서버만 존재) |
| 공통 Layout / Table | 없음 |
| Graph Library | 없음 |

`HealthController`가 `GET /` 과 `/health` 에 JSON health를 반환한다.  
→ Workbench는 **`/workbench/`** 에 두어 root API health와 충돌하지 않게 한다.

기본 포트: **8098** (`application.yml`).

---

## 3. 재사용 API 매핑 (1차)

| 화면 | API | 비고 |
|------|-----|------|
| Home | `GET /api/ontology/catalog` | `graph.conceptCount`, `relationCount`, programs/services |
| Home | `GET /api/ontology/consistency` | 품질/정합 요약 |
| Home / Gate | `GET /api/ontology/validate/rules` | 설계서의 POST와 달리 **현재 구현은 GET** |
| Search | `GET /api/ontology/query/service/{id}/structure` | Golden: `mgcoa8888S0` |
| Search | `GET /api/ontology/query/service/{id}/tables` | |
| Search | `GET /api/ontology/query/program/{id}/services` | |
| Search | `GET /api/ontology/query/handler/{h}/services` | |
| Search | `GET /api/ontology/v1/concept/{id}` | Evidence |
| Impact | `GET /api/ontology/impact/table/{table}` | Golden: `TB_FW_IMAGE_LOG`; `table.type` 반드시 TABLE |
| Impact | (선택) `GET /api/ontology/impact?from=` | Facade unify |
| Gate | `GET /api/ontology/validate/rules` | RULE-001~006 |
| Runtime tab | `GET /api/ontology/runtime/tx-chain` | Search Runtime 탭 |

---

## 4. 구현 리스크 / 주의

1. **`/` vs Workbench** — root는 health JSON 유지, UI는 `/workbench/index.html`.
2. **validate HTTP method** — UI는 실제 **GET** 사용 (설계서 POST 문구와 불일치, Backend 변경 없음).
3. **Impact TABLE alias** — Core 1.0에서 수정 완료. UI는 `table.type !== 'TABLE'` 이면 오류 배너.
4. **CORS** — 동일 origin(static + API same port)이므로 추가 CORS 불필요.
5. **2차 범위 금지** — AI Chat, ADR, Graph Editor, Concept CRUD 미구현.

---

## 5. 제안 Route

```text
/workbench/                  → index.html
#/home                       → Architect Home
#/search?q=mgcoa8888S0       → Architecture Search
#/impact?table=TB_FW_IMAGE_LOG → Impact Analysis
#/gate                       → Architecture Gate
```

공통: Header + Side Menu + Global Search + Evidence Drawer.

---

## 6. Phase 0 완료 기준

- [x] 기술스택 분석
- [x] 기존 UI 부재 확인
- [x] 모노레포 static 패턴 채택 결정
- [x] API 매핑 / 경로 충돌 정리
- [x] 본 문서 작성

→ Phase 1 화면 구현으로 진행.
