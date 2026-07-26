# tcf-ai-crud-meoy — NSIGHT CRUD 절차 위저드

[tcf-ai-methodology](../tcf-ai-methodology/) 패턴의 Spring Boot 도구 모듈입니다.  
CRUD 프롬프트(C-MASTER · C00~C18)를 **LLM 없이** 화면에서 질문→확정→원장 저장으로 진행합니다.

| 항목 | 값 |
|------|-----|
| 버전 | 0.1.0 |
| 포트 | `8788` |
| 저장소 | H2 (`~/nsight-crud-meoy/sessions-db`) |
| 프롬프트 SoT | `src/main/resources/prompts/` (원본: `ztcf-다이어리/2026-07-26-인공지능방법론-CRUD개발프롬프트`) |

## 무엇을 하는가

1. 세션 생성
2. C-MASTER → C00…C18 단계별 구조화 질문 답변
3. 확정정보 원장 갱신
4. **C14 Gate** 통과/조건부만 C15 이후 unlock
5. `결과/*.md` + 원장 ZIP Export (코드 WAR ZIP 생성 없음)
6. **업무도메인 원장 조회** — `BusinessModuleDefinitions` + Handler ServiceId 스캔 결과
7. **단계 세션 조회** — 테이블 `crud_step_session` (세션×단계) + LN 샘플 시드

## 단계별 세션 테이블

| 컬럼 | 설명 |
|------|------|
| `crud_step_session` | session_id + step_id 단위 저장 |
| status | `IN_PROGRESS` / `DONE` |
| answers_json | 해당 단계 답변 스냅샷 |
| summary_md | 단계 완료 확정표 |

- UI: 사이드바 **단계 세션 조회**
- API: `GET /api/step-sessions`, `GET /api/step-sessions/{id}`
- 샘플: 기동 시 `[샘플] LN.CustomerContact 조회` 자동 적재 (없으면) · `POST /api/samples/ln-customer-contact`
- **템플릿 복제**: 선택 세션 → `POST /api/sessions/{id}/clone-as-template` · UI **템플릿으로 복제**
  (답변·원장·단계세션·Gate 상태 복사, `sampleFlag=false`)

## 업무도메인 원장

| 항목 | 값 |
|------|-----|
| 데이터 | `src/main/resources/data/domain-ledger.json` |
| 재생성 | `py -3 tcf-ai-crud-meoy/scripts/generate-domain-ledger.py` |
| UI | 사이드바 **업무도메인 원장** |
| API | `GET /api/domains`, `/api/domains/summary`, `/api/domains/{BC}` |

상태: `ACTIVE`(Handler 존재) · `CATALOG_ONLY`(UI 카탈로그만) · `MODULE_EMPTY`(모듈은 있으나 도메인 없음)

## 관련 소스 조회 (C15~C18 이후 기준소스 확인)

세션의 업무코드(BC)·도메인코드로 저장소를 스캔해 관련 소스를 읽기 전용으로 보여줍니다.

| 항목 | 값 |
|------|-----|
| 스캔 대상 | `{c00.baseModule 또는 {bc}-service}/src` 중 파일명에 도메인코드 포함, `tcf-ui/static/{bc}/` 전체, `sample-requests/{bc}-{domain-kebab}-*` |
| UI | 상단 툴바 **관련 소스** (세션 선택 시 활성) |
| API | `GET /api/sessions/{id}/sources`, `GET /api/sources/content?path=` |
| 루트 | `nsight.crud-meoy.repo-root` (비어 있으면 `settings.gradle` 위치 자동 탐지) |

예: LN.CustomerContact 세션 → `LnCustomerContactHandler/Facade/Service/Dao/Mapper(.java/.xml)`, DTO 5종, `static/ln/contact-list.html`, 샘플 요청 JSON 2건.

## 실행

```bat
run.bat
```

또는 저장소 루트:

```bash
./gradlew :tcf-ai-crud-meoy:bootRun
```

- UI: http://127.0.0.1:8788  
- H2 Console: http://127.0.0.1:8788/h2-console  

### IDE 실행 주의

소스의 초록 Run이 `jdt.ls-java-project` classpath를 쓰면  
`SpringApplication cannot be resolved` 가 납니다.  
**Gradle bootRun** / `run.bat` / Run and Debug의  
`tcf-ai-crud-meoy (Gradle project)` 를 사용하세요.  
(필요 시 Command Palette → `Java: Clean Java Language Server Workspace`)

## API 요약

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/health` | 헬스 |
| GET | `/api/steps` | 단계 catalog |
| GET | `/api/steps/{id}` | 단계 + 프롬프트 md |
| GET/POST | `/api/sessions` | 세션 목록/생성 |
| POST | `/api/sessions/{id}/answers` | 답변 확정 |
| POST | `/api/sessions/{id}/steps/{stepId}/complete` | 단계 완료 |
| POST | `/api/sessions/{id}/gate` | Gate 판정 |
| GET | `/api/sessions/{id}/export.zip` | 결과 ZIP |
| GET | `/api/domains/summary` | 도메인 원장 요약 |
| GET | `/api/domains?q=&group=&status=&businessCode=` | 도메인/ServiceId 조회 |
| GET | `/api/domains/{businessCode}` | 업무코드 상세 |
| GET | `/api/sessions/{id}/sources` | 세션 관련 소스 목록 |
| GET | `/api/sources/content?path=` | 소스 파일 내용 (읽기 전용) |

## 프롬프트 동기화

원본 다이어리 프롬프트를 수정했다면:

```text
Copy-Item "ztcf-다이어리\2026-07-26-인공지능방법론-CRUD개발프롬프트\C*.md" `
  "tcf-ai-crud-meoy\src\main\resources\prompts\" -Force
```

`catalog.json`의 구조화 질문도 필요 시 함께 갱신합니다.

## 비범위 (v1)

- LLM/Cursor API 연동
- Handler/Mapper 코드 ZIP 자동생성 (methodology Model Studio 영역)
- 다이어리 `결과/` 디렉터리 직접 쓰기
