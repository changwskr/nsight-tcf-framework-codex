# WAR Deployment Acceptance Report

- 작성일: 2026-08-10
- 기준: 지시서 Phase 2
- 빌드: `gradlew.bat clean test war` (재실행 `test war` 포함)

---

## 판정: **PASS**

---

## WAR 내용 검사

파일: `build/libs/tcf-ontology-service.war`

| 경로 | 결과 |
|------|------|
| `WEB-INF/classes/static/workbench/index.html` | OK |
| `.../workbench/css/workbench.css` | OK |
| `.../workbench/js/api.js` | OK |
| `.../workbench/js/app.js` | OK |
| `.../workbench/js/design.js` | OK |
| `DesignRecommendationService.class` | OK |
| `YamlGraphLoader.class` | OK |

---

## Context Path

### BootRoot (`http://127.0.0.1:8105`)
| 경로 | 결과 |
|------|------|
| `/health` | 200, `workbench=workbench/index.html` (상대) |
| `/workbench/index.html` | 200 |
| `/workbench/js/api.js` | 200, `resolveContextPath` 포함 |
| `/api/ontology/catalog` | 200 |
| `POST /api/ontology/design/recommend` | 200 |

Google Fonts 외부망 의존: **없음**

### Tomcat Context (`http://127.0.0.1:8106/tcf-ontology-service`)
| 경로 | 결과 |
|------|------|
| `/health` | 200 |
| `/workbench/index.html` | 200 |
| `/workbench/js/*`, `/css/*` | 200 |
| `/api/ontology/catalog` | 200 |
| `POST .../design/recommend` | CREATE→`mgcoa9000C0` |
| `POST .../reload` | Registry+Store 동시 재적재 (`graphStore=OK`, concepts=101) |

`api.js`는 pathname의 `/workbench/` 기준으로 context prefix를 자동 반영한다.

---

## Health Workbench URL

- 절대 `/workbench/...` 아님
- 상대 `workbench/index.html` 반환 → context path에서도 해석 가능
