# NSIGHT-TCF-FRAMEWORK 기반 AI·LLM SI 개발방법론

| 구분 | 내용 |
| --- | --- |
| 문서 상태 | M0~M17 확정 통합본 (M18) |
| 근거 | `결과/M0` ~ `결과/M17` |
| SoT | 현재 작업 Branch 실제 소스 (M3) |
| 시범 | `av-service` / AV / Sample / `AV_SAMPLE` |

---

## 미충족·미확정 체크리스트 (본문 선행)

완료 판정 항목은 **골격상 충족**이나, 아래는 기준서에 `미확정`으로 남긴다. 임의로 채우지 않았다.

| # | 항목 | 상태 |
| ---: | --- | --- |
| 1 | Git 반영 방식 (Commit / PR / 자동PR) | 미확정 (M16) |
| 2 | NFR 수치 (TPS, p95, Timeout 초) | 미확정 (M7) — 란은 필수 |
| 3 | CUD ServiceId 정식 명칭 | 미확정 (M2·M12) |
| 4 | 개인정보·감사·보존기간 상세 | 미확정 (M6) |
| 5 | OM Catalog AV 등록 Gap 해소 | Gap (M3) |
| 6 | M1 3순위 문제 | 미선정 |
| 7 | 정량 KPI (설계시간 %) | 미확정 |
| 8 | 기준 Branch 이름 | 미확정 |

→ 위가 남아 있어도 **방법론 절차·경계·Gate는 확정**되어 있으므로 본문을 발행한다.  
운영 착수 전 1·2·3·5를 우선 해소한다.

---

## 1. 도입 전 안내말

### 왜 AI·LLM 기반 방법론이 필요한가

NSIGHT-TCF에서 화면·테이블 기반 CRUD는 반복이 크다.  
설계서 작성과 동일 패턴 구현에 시간이 쓰이고, 문서·소스·OM이 어긋나기 쉽다.  
AI는 **확인된 설계정보를 반복 산출물로 변환**하는 데 쓰고, 설계 결정은 사람이 한다.

### 기존 SI 개발방식의 문제 (확정 우선순위)

1. 설계서 작성시간 과다  
2. 반복 CRUD 개발비용  

### NSIGHT-TCF 적용 방향

- 공통 `/online` + ServiceId Dispatcher  
- 업무코드별 WAR, 6계층 패키지  
- SoT = 현재 Branch 소스  
- 선도: 조회 → 성공 후 CRUD  

### 자동화의 목적과 한계

| 목적 | 한계 |
| --- | --- |
| 설계서 초안·반복 필드, (승인 후) 코드 골격 | AI가 업무규칙·소유권·Timeout·SQL성능을 결정·승인하지 않음 |
| 승인 없이 사용 가능 = **문서 골격만** | 코드·완성 문서·OM은 AA 승인 후 |

---

## 2. 문서 개요

| 항목 | 내용 |
| --- | --- |
| 목적 | AV 샘플 CRUD를 설계→생성→검증까지 반복 가능한 절차로 문서화 |
| 적용범위 | 화면·테이블 기반 CRUD (최종 시범). 1차 선도=조회 |
| 제외 | 배치·파일, Gateway 신규 라우팅 |
| 보류 | 신규 WAR, 타 WAR·외부 연계 |
| 대상 독자 | 업무 개발자, AA, AI·방법론 설계자 |
| 선행조건 | NSIGHT-TCF 소스 접근, M00 대화원칙, AA 지정 |
| 용어 | ServiceId=`{업무코드}.{Domain}.{action}`; SoT=사실 기준; Baseline=AA 승인 설계 |

---

## 3. 본문

### 3.1 문제 정의 및 설계 요건

- 문제: 설계서 시간·CRUD 반복 비용  
- AI 1차 축소: 설계서 초안·반복 필드  
- 사람 유지: Timeout·TX·오류 / Rule / SQL 성능·인덱스  
- KPI: 조회→(이후 CRUD)를 절차로 문서화·증적

### 3.2 현행 구조와 문제점

| 구분 | 내용 |
| --- | --- |
| 현재 구현 | `av-service`에 inquiry 경로 존재 |
| Gap | CUD 미구현, 설계서·OM·테스트를 1차 기준으로 쓰지 않음(M3) |
| 위험 | 문서만으로 설계하면 소스와 Drift |

### 3.3 요구사항과 제약조건

**기능:** F-AV-01~04 조회·등록·변경·삭제, 화면 1개.  
**NFR(설계 란 필수):** 응답시간·TPS, Timeout·재시도 (수치 미확정).  
**제약:** SoT=소스; Gateway/배치 제외; Baseline 전 코드 생성 금지.

### 3.4 설계 원칙

1. 코드 생성부터 시작하지 않는다.  
2. 사실 / 목표 표준 / Gap / 미확정을 구분한다.  
3. AI는 설계를 대신 결정하지 않는다.  
4. 데이터 변경은 소유 도메인만; 타 WAR는 ServiceId만.  
5. 업무 Controller·Service→Mapper 직호출·타 업무 테이블 직접 참조 금지.

### 3.5 대안 비교 및 의사결정

| 주제 | 선택 | 기각 |
| --- | --- | --- |
| 시범 단위 | CRUD 세트(최종) + 선도는 조회 | 처음부터 전체 SI |
| SoT | 현재 Branch 소스 | 설계서 우선 |
| 승인 | AA 단일 A | 영역별 복수(미채택) |
| 모델 단위 | ServiceId 1건 ×4 + Screen | 거래만/복합 전부 |
| 승인 없이 사용 | 문서 골격만 | CRUD 전체 자동 사용 |
| 자동 Gate | ~구조·명명 | 배포까지 자동 |
| Git | 미확정 | — |

### 3.6 목표 아키텍처

- 배포: 업무코드별 WAR (`av.war`, `/av`, 모듈 `av-service`)  
- 패키지: `com.nh.nsight.marketing.av.{계층}`  
- 계층: entry/handler → facade → service → rule → dao → mapper  
- 플랫폼: tcf-core / tcf-web; UI 중계: tcf-ui  

### 3.7 표준 형식

- Endpoint: `POST /av/online`  
- ServiceId: `AV.Sample.{action}`  
- 전문: 표준 Request/Response 헤더+바디  
- Artifact 공통 메타: M11 전부 필수  

### 3.8 구성요소 및 속성

화면·거래·프로그램·데이터·SQL 필수 필드 = M11 전부.  
템플릿 카탈로그 = 전체 유지; AI 초안 1차 = 화면·거래·프로그램·테이블·SQL·추적성.

### 3.9 책임 경계와 RACI

| 산출물 | R | A | C | I |
| --- | --- | --- | --- | --- |
| 설계 Baseline | 개발자(+AI) | **AA** | 프레임워크 | PMO |
| 코드·Mapper | 개발자(+AI) | **AA** | 프레임워크 | — |
| OM 초안 | 개발자 | **AA** | 운영(해당시) | — |
| 검증 증적 | 개발자 | **AA** | — | — |

### 3.10 정상 처리 흐름

```text
화면 → 표준 전문 → JWT Filter(1차; Gateway 신규 제외)
→ OnlineTransactionController → TCF → STF → TimeoutExecutor
→ Dispatcher → Handler → Facade → Service → Rule → DAO → Mapper → DB
→ ETF → 응답 → 거래로그
```

### 3.11 오류·Timeout·장애 흐름

- 업무 오류: Rule/Service → 표준 오류코드 → ETF  
- Timeout: NFR-02 + TimeoutExecutor (초 미확정)  
- Rollback: Facade TX; DB·OM은 코드 롤백만으로 부족  

### 3.12 정상 예시

- 선도: `AV.Sample.inquiry` → 목록/조회 성공, GUID 로그  
- 확장: create/update/delete (명칭 미확정) 동일 구조  

### 3.13 금지 예시

- 업무별 Controller  
- Service→Mapper 직호출  
- 타 WAR DAO/Mapper/Table 직접 접근  
- Baseline 전 코드 생성·머지  
- AI 단독으로 Rule/Timeout/SQL성능/소유권 승인  
- “알아서/적절히” 완료 선언  

### 3.14 연계 규칙

1차 타 WAR·외부 연계 없음. 필요 시 ServiceId 표준 거래만 (Java 직접호출 금지).

### 3.15 데이터 및 상태관리

| 테이블 | 소유 | 변경 | 타 WAR 직접 |
| --- | --- | --- | --- |
| `AV_SAMPLE` | AV/Sample | AV만 | 금지 |

Artifact 상태: 초안 → Baseline승인 → 생성·구현 → 검증 → Drift확인 → 완료.

### 3.16 성능·용량·확장성

설계서에 응답시간·TPS 란 필수. 수치·용량 산정은 미확정. SQL 성능·인덱스는 사람 승인.

### 3.17 보안·개인정보·감사

인증·권한·개인정보·감사 상세는 미확정. 거래 템플릿에 인증·권한·로그·감사 란은 필수. AI가 정책 자동 승인 금지.

### 3.18 운영·모니터링·장애 대응

- 배포 개념: `ztomcat` / `tcf-scripts`  
- health·스모크 후 관찰  
- Rollback: WAR + DB + OM  
- Gateway 신규 없음  
- Git 반영 방식: 미확정  

### 3.19 자동검증 및 품질 Gate

| 자동 | 수동(완료 필수) |
| --- | --- |
| 컴파일, 단위테스트, 구조·명명 | DB·거래 POST, 배포·운영, 증적, AA A |

컴파일만으로 완료 금지.

### 3.20 테스트 시나리오

1. inquiry 표준 POST 정상  
2. (2차) CUD 각 거래 정상·업무오류  
3. Timeout 시나리오 (수치 확정 후)  
4. Drift: 소스 ↔ 모델 ↔ 문서 ↔ OM  

### 3.21 체크리스트

- [ ] 공통 메타·필수 필드 기입 (`미확정` 허용, 공란 금지)  
- [ ] Baseline AA 승인  
- [ ] 자동 Gate ①~③  
- [ ] 표준 POST 증적  
- [ ] Drift 통과  
- [ ] Rollback 계획(DB·OM)  
- [ ] 제외 범위 미침범  

### 3.22 변경·호환성·폐기 관리

- 재생성 시 수동·보호 구간 덮어쓰기 금지  
- As-Designed vs As-Built Drift 검증  
- Artifact 폐기 여부 메타 필수  
- 변경 시 영향: ServiceId·OM·화면·SQL·문서 동시 검토  

---

## 4. 시사점

- 핵심 판단: **소스 SoT + AA Gate + ServiceId 단위 모델 + 조회 선도 후 CRUD**.  
- 주요 위험: OM Gap, Git 미정, NFR 수치 공란, CUD 명칭 미정.  
- 우선 보완: Git 방식, OM 등록, Timeout/TPS 수치, CUD ServiceId.  
- 중장기: Model Studio 능력과 목표 분리 유지, 타 WAR·배치는 별도 방법론 확장.

---

## 5. 마무리말

본 방법론은 NSIGHT-TCF에서 AI·LLM을 **복붙 코드 생성기**가 아니라  
**설계 Baseline 이후의 반복 변환기**로 쓴다.  
M0~M17에서 확정한 경계만 공식으로 삼고, 미확정은 해소 전까지 채우지 않는다.

실전 작업 시: 작업용 프롬프트 `P0 + Pn` + 본 기준서의 확정표를 함께 적용한다.

---

# 부록

## 부록1. 전체 개발 생명주기

```text
요구·도메인·데이터 → 화면·ServiceId·전문 → 프로그램·SQL
→ Baseline(AA) → 생성·규칙·보완 → 테스트·bootRun·POST
→ 역분석·As-Built·Drift → 시험 → 배포·운영
```

## 부록2. Wizard 22단계 요약

`결과/M14-개발-실행절차.md` 표와 동일. **11 이전 코드 생성 금지.**

## 부록3. 표준 템플릿 카탈로그

`결과/M10-설계-템플릿-체계.md` — 전체 유지, AI 초안=핵심 6종.

## 부록4. 템플릿별 필수 속성

`결과/M11-템플릿-속성-입력정보.md` — 공통 메타 전부 + 화면·거래·프로그램·SQL 전부.

## 부록5. 메타모델

기본 단위 = ServiceId 1건. CRUD = 모델 4 + Screen 참조.  
객체·관계: `결과/M12-메타모델-객체관계.md`.

## 부록6. AI·사람 경계

| 승인 없이 | AA 승인 후 | 사람 필수 |
| --- | --- | --- |
| 문서 골격 | 설계 초안·코드·OM | Rule, Timeout/TX, SQL 성능, 소유권 |

## 부록7. 아키텍처 구성도

```text
tcf-ui ──relay──► av-service(/av/online)
                      │
                 tcf-web / tcf-core
                      │
                   AV_SAMPLE
```

## 부록8. 추적성

```text
ScreenEvent → ServiceId → Handler → Facade → Service → Rule
 → DAO → Mapper → SQL → Table(AV_SAMPLE)
 → OperationalPolicy / Test / TxLog
```

## 부록9. RACI

§3.9와 동일.

## 부록10. Architecture Gate

1. SoT와 설계의 현재/목표/Gap 구분  
2. M1 사람 유지 항목 미승인 통과 금지  
3. 금지 패턴 없음  
4. M0 제외 범위 미침범  

## 부록11. 자동검증 규칙

자동: compile / test / 구조·명명·금지패턴·Mapper 일치.  
그 이상: 수동.

## 부록12. 테스트 시나리오

§3.20.

## 부록13. 선도개발 계획

1차 `AV.Sample.inquiry` → 2차 CRUD. 성공·중단: `결과/M17`.

## 부록14. 확산 로드맵

조회 → CRUD → 다중테이블·Rule → … → 배치·파일은 1차 범위 밖.

## 부록15. Risk·Gap

OM Gap, Git 미정, NFR 수치, CUD 명칭, 개인정보 분류, verify-deploy에 av 등록 여부.

## 부록16. 완료 증적 목록

자동 Gate 통과 로그, POST 요청·응답, GUID 거래로그, Commit ID, 모델·문서 버전, AA 승인 기록, (해당 시) OM·배포 health.

---

## 완료 판정 (M18)

| 판정 항목 | 결과 |
| --- | --- |
| 프로젝트 목표 / 적용범위·개발유형 | 충족 |
| 업무·도메인·데이터 소유권 | 충족 |
| 기능·비기능 Baseline | 충족 (수치 미확정) |
| 목표 앱 아키텍처 / 표준 거래 | 충족 |
| 템플릿·속성 / 메타모델 | 충족 |
| AI·사람 / Wizard·승인 | 충족 |
| 생성범위 / Quality Gate | 충족 |
| CI/CD·배포·운영 | 부분 (Git 미확정, 롤백·배포개념 충족) |
| 선도개발·성공기준 | 충족 |
| 변경·재생성·Drift·폐기 | 충족 (최소) |

**M18 통과(미확정 목록 명시).**  
근거 단계 파일: `결과/M0` ~ `결과/M17`.
