# 농협 상호금융 NSIGHT 아키텍처 의사결정 인사이트

> 문서 성격: 아키텍처 의사결정 포트폴리오 분석 및 실행 제안  
> 분석 기준일: 2026-08-02  
> 분석 대상: 14개 아키텍처 영역, 221개 TASK, P0 우선결정 20건, 권장 방안서 15건과 개별 상세·전문가 문서

## 1. 이 문서의 목적

이 문서는 아키텍처 의사결정 목록을 다시 요약하는 문서가 아니다. 221개 TASK를 함께 놓고 보았을 때 드러나는 구조적 패턴, 선후관계, 집중 위험과 실행 전략을 설명한다.

핵심 질문은 다음과 같다.

- 221개 중 어떤 결정을 먼저 닫아야 개발 재작업을 줄일 수 있는가?
- 서로 다른 영역의 결정은 실제로 어떻게 연결되는가?
- 문서가 많아졌지만 실행 통제가 되지 않을 위험은 무엇인가?
- NSIGHT가 “문서 중심 아키텍처”에서 “지속 검증되는 실행 아키텍처”로 가려면 무엇이 필요한가?
- 현재 저장소의 프레임워크 구조를 활용해 어떤 결정을 자동화할 수 있는가?

기준 문서:

- [아키텍처 의사결정 사항 목록](./농협%20상호금융%20NSIGHT%20아키텍처%20의사결정%20사항%20목록.md)
- [TASK별 통합 방안서](./2026-08-02-아키테처-의사결정-TASK-상세.md)
- [P0 우선 확정 대상](./15_우선_확정_대상/README.md)
- [권장 방안서 작성 순서](./16_권장_방안서_작성_순서/README.md)
- [아키텍처 의사결정 관리 원칙](./17_관리_원칙/README.md)

## 2. Executive Insight

NSIGHT의 핵심 문제는 “결정사항이 부족한 것”이 아니다. 이미 221개 TASK가 식별되어 있다. 더 큰 문제는 이 TASK들이 개별 문서로만 소비될 경우 다음 세 가지가 발생한다는 점이다.

1. 선행결정이 닫히지 않은 상태에서 후행 구현이 시작된다.
2. 동일한 정책이 UI, Gateway, TCF, 업무 WAR, EAI, DB와 운영에서 서로 다르게 구현된다.
3. 문서 승인을 완료로 오인해 실제 코드·설정·OM·테스트의 Drift가 누적된다.

따라서 NSIGHT가 관리해야 할 단위는 개별 TASK만이 아니라 다음의 **결정 사슬(Decision Chain)**이다.

```text
의사결정 권한
  → 식별자와 계약
    → 신뢰 경계와 실행 경로
      → 데이터·트랜잭션 정합성
        → 장애 격리와 복구
          → 관측·증적
            → CI/CD 자동 차단
              → 운영 피드백과 기준 개정
```

이 사슬 중 앞 단계가 열려 있으면 뒤 단계의 산출물은 안정적인 Baseline이 될 수 없다.

## 3. 가장 중요한 12가지 인사이트

### Insight 1. 221개 TASK는 221개의 독립 의사결정이 아니다

표면적으로는 GOV, APP, STD, UI, SEC 등 14개 영역으로 나뉘지만 실제로는 강한 의존관계를 가진다. 예를 들어 ServiceId 하나만 보더라도 다음 결정이 연결된다.

```text
STD-03 ServiceId 형식
  → APP-07 Handler 책임
  → MCA-06 채널 라우팅
  → INT-02 다른 WAR 호출
  → REL-04 업무별 Timeout
  → OPS-07 OM Service Catalog
  → QLT-07 ServiceId 중복검사
```

따라서 개별 TASK를 순서 없이 승인하면 같은 개념이 서로 다른 전제에서 확정될 수 있다. 의사결정은 “ID별 완료율”보다 “연결된 결정 사슬의 폐쇄율”로 관리해야 한다.

### Insight 2. 첫 번째 아키텍처 산출물은 기술 설계서가 아니라 결정 운영체계다

GOV-01~12가 먼저 닫히지 않으면 다른 영역의 결정은 승인 주체, 기준선, 변경 절차와 예외 만료를 갖지 못한다. 특히 다음 항목은 모든 후행결정의 제어면(Control Plane)이다.

- GOV-01 아키텍처 의사결정 절차
- GOV-02 RACI
- GOV-03 ARB
- GOV-04 공식 기준문서
- GOV-05 ADR 번호체계
- GOV-06 예외 승인
- GOV-10 Architecture Gate

즉, 거버넌스는 부가적인 PMO 절차가 아니라 나머지 아키텍처가 작동하기 위한 런타임과 같다.

### Insight 3. ServiceId는 단순한 명명규칙이 아니라 NSIGHT의 핵심 제어 키다

ServiceId는 요청을 Handler에 연결하는 라우팅 키이면서 거래통제, Timeout, 로그, 권한, OM과 테스트를 연결하는 운영 키다.

```text
화면 이벤트
  ↔ StandardHeader.serviceId
  ↔ TransactionDispatcher
  ↔ TransactionHandler.serviceIds()
  ↔ Facade·Service
  ↔ OM_SERVICE_CATALOG
  ↔ Timeout·거래통제
  ↔ 거래로그·Slow Service
  ↔ CI 중복·누락 검사
```

이 관점에서 STD-03, APP-07, OPS-07과 QLT-07은 별도 TASK가 아니라 하나의 통합 통제로 다뤄야 한다. ServiceId가 소스에 존재하지만 OM에 없거나, OM에는 있지만 Handler가 없으면 배포 성공과 거래 실행 가능성이 달라진다.

### Insight 4. TCF 우회 방지는 아키텍처 일관성의 핵심 방어선이다

NSIGHT의 공통 기능은 STF/TCF/ETF 흐름을 통과한다는 전제에서 작동한다. 업무별 Controller나 직접 Service 호출이 늘어나면 다음 통제가 동시에 약해진다.

- Header와 인증 문맥 검증
- 거래통제와 Timeout 정책
- 거래 시작·종료 로그
- 표준 오류 응답
- 감사·메트릭·TraceId
- Handler·ServiceId 추적

따라서 APP-05, APP-06, APP-19는 Endpoint 스타일 문제가 아니라 보안·운영·정합성 문제다. 예외 Endpoint는 White List와 명시적 소유자를 가져야 하며, CI에서 일반 업무 Controller와 TCF 우회 의존을 탐지해야 한다.

### Insight 5. 개인정보 통제의 최종 경계는 화면이 아니라 서버 응답 조립 지점이다

UI 마스킹만으로는 Network 응답, 브라우저 메모리, 개발자 도구와 파일 다운로드에서 원문을 보호할 수 없다. 개인정보 통제는 다음과 같이 계층화되어야 한다.

```text
데이터 분류와 소유권
  → 저장 암호화·키관리
  → 업무 목적·사용자·데이터권한 검증
  → 서버 원문·부분마스킹·필드제거 결정
  → 화면 표시 보조통제
  → 파일·엑셀 별도 권한
  → 로그 마스킹
  → 원문조회·복호화 감사
```

SEC-01~15는 개별 보안 기능이 아니라 하나의 데이터 노출 생명주기다. 특히 SEC-02, SEC-04, SEC-05, SEC-11과 SEC-13을 함께 설계해야 “화면에서는 가렸지만 로그와 파일에는 남는” 통제 공백을 막을 수 있다.

### Insight 6. JWT 검증보다 더 어려운 문제는 Claim–Header–업무권한의 정합성이다

JWT 서명이 유효하다는 사실은 요청 Header의 userId, branchId, channelId와 업무 대상 데이터가 정당하다는 뜻이 아니다.

```text
SSO·IdP 신원
  → JWT Claim
  → Gateway/WAR 서명·수명 검증
  → STF의 Claim–StandardHeader 대조
  → ServiceId 기능권한
  → Service·Rule의 조직·지점·고객 데이터권한
  → 감사로그
```

AUTH-03~08은 하나의 인증·인가 파이프라인으로 검증해야 한다. 가장 중요한 보안시험은 정상 로그인보다 Header 위변조, 권한 없는 ServiceId 호출, 타 지점 고객 접근과 인증 예외 URL 우회다.

### Insight 7. Timeout은 설정값이 아니라 종단 시간예산이다

Timeout 관련 실패는 상위 요청만 끝나고 하위 작업이 계속되는 경우에 가장 위험하다. 사용자는 실패로 인식했지만 DB나 외부 시스템에서는 처리가 완료되어 중복 요청과 데이터 불일치가 발생할 수 있다.

```text
UI Timeout
  > MCA/Gateway Timeout
    > Online Transaction Timeout
      > Transaction Timeout
        > DB Query / 외부호출 Timeout
```

REL-03~06, INT-06~09와 INF-04~05는 함께 검토해야 한다. Timeout 결정을 할 때에는 시간값뿐 아니라 취소 가능성, Thread·Connection 회수, 실제 처리상태 조회, 멱등키, 재시도 적격성, 보상과 대사까지 닫아야 한다.

### Insight 8. 트랜잭션 경계는 Facade 책임만의 문제가 아니라 외부연계 정합성의 시작점이다

Facade에 `@Transactional`을 두는 것만으로 외부 시스템과 원자성이 보장되지는 않는다. 로컬 DB Commit과 외부 처리 결과 사이에는 분산 트랜잭션이 아닌 상태기계·멱등성·보상·대사가 필요하다.

```text
Facade Transaction
  ├─ Local DB: Commit / Rollback
  └─ External Call: 요청됨 / 처리중 / 성공 / 실패 / 불명확
                         ↓
                    상태조회·대사·보상
```

APP-13, INT-07~10과 REL-07~10을 하나의 “변경 거래 안정성” 방안서로 통합하면 부분성공을 업무팀별 예외처리로 흩어지지 않게 할 수 있다.

### Insight 9. 운영 가능성은 개발 완료 이후 추가하는 기능이 아니다

OPS 영역은 마지막 단계의 모니터링 작업이 아니라 설계 입력이다. TraceId, 오류분류, 거래상태와 감사 이벤트가 설계되지 않으면 운영 도구가 수집할 의미 있는 정보가 없다.

개발 완료 정의에는 최소 다음 연결이 필요하다.

```text
ServiceId
  + GUID·TraceId
  + 시작·종료·오류 상태
  + Mapper SQL ID·외부 Interface ID
  + 배포 버전
  + OM Catalog·Timeout·통제
  + 장애 Runbook
```

OPS-01~09를 선도 거래 단계에서 먼저 검증하면 통합시험 후반의 “로그는 있지만 원인을 찾을 수 없는” 문제를 줄일 수 있다.

### Insight 10. CI/CD Gate는 문서 준수율을 코드로 변환하는 과정이다

문서로만 통제하기 어려운 항목은 Pipeline에서 자동화해야 한다.

| 규칙 | 자동화 후보 |
|---|---|
| 계층·WAR 경계 | ArchUnit과 Gradle 의존성 검사 |
| ServiceId | Handler Registry·OM Catalog·샘플 요청 대조 |
| 명명 | Checkstyle·정규식·DDL 검사 |
| 전문 계약 | Schema·Contract Test |
| Secret·취약점 | Secret Scan·SAST·SCA |
| Timeout | 소스 ServiceId–OM Timeout 정책 대조 |
| 배포 | Artifact Manifest·Checksum·Health·Smoke Test |
| 문서 Drift | 설계·설정·OM·소스 값 비교 |

QLT TASK의 진짜 목표는 검사 도구를 많이 설치하는 것이 아니라, 반복 가능한 위반을 사람의 기억에서 제거하는 것이다.

### Insight 11. 단일 Tomcat 다중 WAR 모델은 개발 편의와 운영 장애격리를 분리해 판단해야 한다

로컬 ztomcat 모델은 통합 Context와 WAR 배포를 검증하는 데 유용하지만, 이를 운영 배치의 안전성 근거로 그대로 사용할 수는 없다. 동일 JVM에서 WAR가 Heap, GC, Thread와 일부 공통 자원을 공유하면 한 업무의 지연·누수가 다른 업무로 확산될 수 있다.

INF-01~14는 다음 두 질문을 분리해야 한다.

1. 개발·통합환경에서 무엇을 한 인스턴스에 모아 검증할 것인가?
2. 운영환경에서 어떤 부하·장애·조직 경계를 기준으로 JVM·Tomcat·WAR를 분리할 것인가?

운영 배치는 WAR 수가 아니라 목표 TPS, p95, 장애 시 잔여용량, Hikari Pool 합계, 배포 독립성과 Blast Radius를 기준으로 결정해야 한다.

### Insight 12. 최종 목표는 ADR 저장소가 아니라 실행 가능한 의사결정 그래프다

장기적으로 각 ADR은 문서 파일 하나가 아니라 다음 관계를 가져야 한다.

```text
Requirement
  ↔ TASK / ADR
  ↔ 화면·이벤트·ServiceId
  ↔ 프로그램·설정·SQL·Schema
  ↔ 테스트·CI Gate
  ↔ Artifact·배포
  ↔ OM·로그·KPI·Runbook
  ↔ 예외·Risk·기술부채·대체 ADR
```

이 연결이 형성되면 “문서가 최신인가?”를 사람이 전부 읽지 않고도 Drift 검사와 운영 데이터로 판단할 수 있다.

## 4. 의사결정 포트폴리오의 네 개 축

### 4.1 Control Plane — 누가 무엇을 결정하는가

관련 영역: GOV, QLT

핵심 산출물은 ADR 템플릿보다 결정권, 차단조건, 예외 만료와 증적 기준이다. 이 축이 약하면 다른 모든 결정은 권고사항에 머문다.

### 4.2 Trust and Contract Plane — 요청을 무엇으로 신뢰하는가

관련 영역: STD, MCA, AUTH, SEC, UI

ServiceId, Header, JWT Claim, 전문 Schema와 개인정보 등급이 같은 의미체계를 가져야 한다. 서로 다른 소유조직이 관리하므로 가장 먼저 통합 용어와 계약을 확정해야 한다.

### 4.3 Execution and Consistency Plane — 업무가 어떻게 정확히 처리되는가

관련 영역: APP, INT, DATA, REL, BFC

TCF 경유, 계층 책임, 트랜잭션, 외부연계, Lock, Timeout, 멱등성과 배치 재시작이 포함된다. 정상경로보다 부분실패와 최종 상태를 중심으로 설계해야 한다.

### 4.4 Evidence and Operations Plane — 지켜졌음을 어떻게 증명하는가

관련 영역: OPS, INF, QLT

로그·메트릭·감사·OM·Pipeline·배포 Manifest와 Runbook이 포함된다. 개발팀의 “적용했다”는 설명 대신 자동검사와 운영 증적을 완료 조건으로 사용한다.

## 5. 결정 의존관계와 Critical Path

### 5.1 Critical Path

```text
GOV-01~06 결정 운영체계
  → APP-01~04 업무·WAR·계층 경계
    → STD-01~14 식별자·추적체계
      → MCA-01~03 표준 전문·필드
        → AUTH-01~08 인증·권한·Header
          → APP-13 + DATA-01~08 트랜잭션·데이터
            → INT-04~10 + REL-03~10 Timeout·멱등·부분성공
              → OPS-01~09 운영 추적
                → QLT-03~18 자동검증·배포 Gate
```

이 경로의 앞쪽 P0가 미확정이면 뒤쪽 문서와 구현은 변경될 가능성이 높다.

### 5.2 병렬 진행 가능한 트랙

다음은 공통 전제가 확정되면 병렬화할 수 있다.

- UI 공통 컴포넌트·접근성·브라우저 호환
- DB 물리모델·인덱스·페이징 검증
- 배치 Job·파일·캐시 세부기준
- JVM·GC·Pool 용량 Baseline 측정
- 로그 수집·Dashboard·Alert·Runbook 설계

단, ServiceId, Header, 개인정보 등급, 오류체계와 TraceId 계약은 병렬 트랙의 공통 입력으로 먼저 제공되어야 한다.

## 6. P0 20개를 다루는 더 좋은 방법

P0 20개를 20회의 독립 회의로 처리하기보다 다음 6개의 Decision Package로 묶는 것이 효율적이다.

| Package | 포함 결정 | 결과 |
|---|---|---|
| DP-01 Governance | ADR·RACI·ARB·예외·Gate | 누가 언제 무엇을 승인하는지 확정 |
| DP-02 Identity & Contract | 업무코드·화면 ID·ServiceId·전문·오류 | 화면부터 운영까지 공통 언어 확정 |
| DP-03 Execution Boundary | TCF·6계층·DTO·Validation·Transaction | 업무 구현과 공통처리 경계 확정 |
| DP-04 Security Context | 개인정보·마스킹·암호화·JWT·권한 | 신원과 데이터 노출 통제 확정 |
| DP-05 Consistency & Resilience | EAI·Timeout·Retry·멱등·부분성공·데이터 소유 | 실패 시 최종 상태와 복구 확정 |
| DP-06 Runtime Evidence | Tomcat·Pool·로그·감사·CI/CD | 운영 가능성과 자동 차단 확정 |

각 Package는 ADR 하나로 합치는 것이 아니라, 여러 ADR이 공유하는 전제·용어·검증 시나리오를 같은 회의와 선도 구현에서 닫는 단위다.

## 7. 현재 가장 큰 구조적 위험

### 7.1 결정량 과다로 인한 승인 병목

221개를 동일 깊이로 한 번에 심의하면 ARB가 병목이 된다. 모든 TASK가 같은 수준의 중앙 승인을 필요로 하지 않는다.

권고:

- 프로젝트 공통·보안·외부계약·데이터 소유권: ARB 승인
- 영역 내부 구현 선택: 영역 아키텍트 승인
- 가역적·저위험 구현: 표준과 자동검증 안에서 팀 자율
- 예외·비가역 변경: 상위 승인과 만료 필수

### 7.2 문서 계층 증가로 인한 Source of Truth 혼란

요약본, 상세본, 전문가본이 모두 존재하므로 같은 결정을 세 문서에서 각각 수정하면 Drift가 발생한다.

권고 Source of Truth:

```text
관리대장: ID·제목·RACI·우선순위·산출물
ADR: 최종 선택·근거·영향·예외·폐기조건
전문가본: 심의 분석과 품질·통제·증적 모델
상세본: 교육·수행 절차
요약본: 빠른 탐색과 체크리스트
```

최종 결정값은 ADR만 소유하고 다른 문서는 이를 참조해야 한다.

### 7.3 “자동검증 가능”과 “실제로 자동검증됨”의 혼동

문서에 ArchUnit, Checkstyle, Contract Test가 언급되어도 Pipeline에 연결되지 않았다면 통제가 아니다. 각 자동화 항목은 다음 상태로 관리해야 한다.

```text
후보 → 설계 → 구현 → Shadow 검증 → Warning → Blocking → 운영 측정
```

### 7.4 현재 구현과 목표 아키텍처 혼재

일부 문서는 목표 확장, 권장 구조와 현재 코드 상태를 함께 설명한다. 이는 아키텍처 방향에는 유용하지만 실행계획에는 위험하다.

모든 핵심 ADR은 다음을 분리해야 한다.

- As-Is: 현재 코드와 설정으로 확인된 사실
- To-Be: 승인하려는 목표
- Gap: 변경 대상과 영향
- Transition: 순서·호환·Rollback
- Evidence: 완료를 증명할 테스트·운영값

## 8. 선도 거래로 먼저 검증할 것

문서 221개를 모두 승인한 뒤 구현을 시작하기보다 조회 1건과 변경 1건을 Golden Sample로 선정해 결정 사슬을 종단 검증하는 편이 효과적이다.

### 조회 거래

- ServiceId와 Handler 등록
- JWT Claim–Header–기능·데이터권한
- 서버 마스킹과 로그 마스킹
- readOnly Transaction
- 페이징·조회 제한
- Timeout과 Slow Service 기준
- TraceId–Mapper SQL–OM 추적

### 변경 거래

- Validation과 업무 Rule
- Facade Transaction과 Rollback
- 멱등키와 중복요청
- 외부연계 Timeout·재시도
- 부분성공 상태·보상·대사
- 개인정보 감사 이벤트
- 배포·Smoke·Rollback

선도 거래가 통과하면 가이드뿐 아니라 복제 가능한 코드, 테스트와 CI 규칙이 남아야 한다.

## 9. 추천 90일 실행 로드맵

### 0~15일 — 결정 운영체계와 공통 언어

- GOV-01~06, GOV-10 확정
- P0 Decision Package 책임자와 기한 지정
- 업무코드·ServiceId·화면 ID·거래코드 사전 확정
- 현행–목표–Gap 템플릿과 ADR Baseline 저장소 지정
- 예외·Risk·기술부채 원장 통합

Exit Criteria:

- P0에 단일 주관·승인자·기한 존재
- 구두결정의 TASK 전환
- 공식 기준문서와 상태코드 확정

### 16~30일 — 계약·보안·실행 경계

- 표준 요청·응답 전문과 오류체계
- TCF 공통 실행과 6계층 책임
- JWT Claim–Header–권한 모델
- 개인정보 분류·서버 마스킹·암호화 경계
- 데이터 소유권과 WAR 간 호출 기준

Exit Criteria:

- 조회·변경 Golden Sample 설계
- Contract와 Threat 시나리오 승인
- TCF 예외 Endpoint 목록 확정

### 31~50일 — 정합성·복구 선도 구현

- Transaction·Rollback·Timeout Budget
- 멱등성·재시도·부분성공 상태기계
- Mapper·SQL·Lock·Migration 검증
- TraceId·거래로그·감사 이벤트
- 대표 외부연계 장애·대사 시나리오

Exit Criteria:

- 정상·오류·Timeout·Rollback·중복·부분실패 시험 통과
- Critical Gap 0건 또는 승인된 기한부 예외

### 51~70일 — 자동검증과 운영화

- 계층·ServiceId·명명·Secret 검사
- 소스–OM Catalog–Timeout 대조
- Contract·통합·보안 테스트 Pipeline
- Dashboard·Alert·Runbook
- Artifact Manifest·Health·Smoke·Rollback

Exit Criteria:

- 주요 위반이 Warning 또는 Blocking 상태로 자동 탐지
- 운영자가 대표 장애를 Runbook으로 복구

### 71~90일 — 확대와 Baseline 보정

- 업무팀 단계적 적용
- 예외·적용률·Drift 측정
- 부하·Stress·Soak와 Pool 합산 검증
- 운영 KPI·SLO Baseline 확정
- Golden Sample과 표준 개정

Exit Criteria:

- P0 적용률 100% 또는 승인된 예외
- Blocker·Critical 0건
- 업무팀·운영 인수와 다음 재검토일 확정

## 10. Architecture Dashboard로 관리할 지표

### 결정 흐름

- P0 미결정 수와 기한 초과율
- TASK 등록부터 ADR 승인까지 Lead Time
- 조건부 승인 조치 완료율
- 만료·재승인 대기 예외 수
- 대체 ADR 양방향 연결률

### 구현 정합성

- 소스 ServiceId–Handler–OM Catalog 일치율
- 계층·WAR 직접참조 위반 수
- 설정·Route·Port·Timeout Drift 수
- Contract 호환 실패 수
- 데이터 소유권 위반 수

### 품질·운영

- 정상·오류·Timeout·Rollback 시나리오 통과율
- p95/p99 응답시간과 Timeout율
- 중복처리·부분성공·대사 불일치 건수
- MTTD·MTTR과 TraceId 종단 추적 성공률
- 배포·Rollback·DR 리허설 성공률

### 보안·감사

- 원문 개인정보 노출 결함
- 로그·파일 마스킹 위반
- 평문 Secret 검출 수
- 권한 우회·Claim/Header 불일치 탐지
- 중요 행위 감사 누락률

## 11. 문서 체계를 운영 가능한 지식체계로 바꾸는 방법

### 11.1 문서 역할 고정

| 문서 | 유일하게 소유할 정보 |
|---|---|
| 관리대장 | TASK ID, 제목, RACI, 우선순위, 상태 |
| ADR | 최종 결정값, 근거, 영향, 예외, 대체·폐기 |
| 전문가본 | 심의 논리, Trade-off, 품질 시나리오, 통제·증적 |
| 상세본 | 교육, 수행 절차, 작성 예시 |
| 요약본 | 빠른 탐색, 기본 체크리스트 |
| 코드·설정·OM | 실행되는 기준 |
| CI/CD·테스트 | 기준 준수 증명 |

### 11.2 문서에서 기계 판독 가능한 메타데이터 분리

TASK와 ADR의 핵심 필드는 YAML Front Matter나 별도 Registry로 관리하면 자동 대조가 쉬워진다.

```yaml
taskId: STD-03
adrId: ADR-STD-0003
status: approved
owner: FW
approver: AA
priority: P0
effectiveDate: 2026-00-00
reviewDate: 2026-00-00
controls:
  - service-id-registry-check
  - om-catalog-drift-check
evidence:
  - ci://service-id-check
  - om://service-catalog
```

### 11.3 문서 링크보다 관계 검증

링크가 열린다는 사실만으로 추적성이 완성되지는 않는다. ServiceId 문자열, 설정 Key, Handler 등록과 OM 값이 실제로 같은지를 비교해야 한다. 장기적으로는 문서 링크 검사와 의미 기반 Drift 검사를 분리해 운영해야 한다.

## 12. 추가로 도출되는 고급 인사이트

### 12.1 아키텍처 의사결정에는 만료일뿐 아니라 재검토 Trigger가 필요하다

시간 기반 재검토 외에도 다음 사건이 발생하면 ADR을 재검토해야 한다.

- 처리량이나 사용자 수가 승인 가정을 초과
- 외부기관 전문·보안 정책 변경
- JDK·Spring·DBMS 주요 버전 전환
- 반복 장애·보안사고·감사 지적
- 동일 예외가 여러 업무에서 반복
- 운영 KPI가 연속적으로 목표를 벗어남

### 12.2 예외의 반복은 팀 문제가 아니라 표준 문제일 수 있다

예외를 단순 위반으로만 보면 현장의 제약을 놓친다. 동일 사유의 예외가 반복되면 다음 중 하나다.

- 표준이 실제 업무를 수용하지 못함
- 공통 컴포넌트 사용성이 낮음
- 전환비용이 과도함
- 교육·샘플이 부족함
- 자동검증의 오탐이 큼

예외 원장을 표준 개선 Backlog의 입력으로 사용해야 한다.

### 12.3 운영 KPI는 아키텍처 결정의 유효성 시험이다

ADR은 승인 당시의 가정 위에서 선택된다. 운영 데이터는 그 가정을 검증한다. Timeout 증가, Pool 고갈, 감사 누락이나 예외 증가는 해당 ADR의 전제가 틀렸다는 신호일 수 있다. 운영 KPI와 ADR Review를 연결하면 아키텍처가 일회성 설계가 아니라 피드백 시스템이 된다.

### 12.4 모든 규칙을 중앙화하면 오히려 아키텍처가 느려질 수 있다

중앙 통제가 필요한 것은 보안, 공개 계약, 식별자, 데이터 소유권과 공통 처리처럼 편차 비용이 큰 항목이다. 도메인 내부 알고리즘과 가역적인 구현 선택은 경계 계약 안에서 팀 자율로 두는 편이 낫다.

권장 모델:

```text
중앙 고정: 신뢰 경계·공개 계약·식별자·감사·배포 Gate
영역 소유: 데이터 모델·업무 규칙·운영 정책
팀 자율: 계약 내부의 가역적 구현
예외 승인: 외부 제약·단계적 전환·검증된 레거시
```

### 12.5 문서의 완성도보다 결정의 폐쇄성이 중요하다

긴 문서라도 다음이 비어 있으면 결정은 닫히지 않았다.

- 구체적인 선택값
- 실행 위치와 소유자
- 실패·복구 의미
- 측정 가능한 수용기준
- 기존 구현의 전환·Rollback
- 자동 또는 수동 검증 증적
- 예외·폐기조건

반대로 짧은 ADR이라도 위 항목이 닫혀 있고 실행 통제와 연결되면 더 강한 아키텍처 결정이다.

## 13. 내가 제안하는 최우선 10개 실행 과제

1. P0 20개를 6개 Decision Package로 재편한다.
2. GOV-01~06과 Architecture Gate를 가장 먼저 승인한다.
3. ServiceId–Handler–OM–Timeout–로그 통합 Registry를 정의한다.
4. 조회·변경 Golden Sample을 한 건씩 선정한다.
5. Claim–Header–기능·데이터권한 종단 보안시험을 만든다.
6. 서버 마스킹–파일–로그–복호화 감사 정책을 하나로 묶는다.
7. Transaction–Timeout–멱등–부분성공 상태기계를 선도 거래로 검증한다.
8. ArchUnit·ServiceId·Secret·Contract 검사를 CI의 Shadow 단계로 시작한다.
9. 문서–소스–설정–OM Drift Dashboard를 만든다.
10. 운영 KPI와 예외 반복 패턴을 분기별 ADR 재검토에 연결한다.

## 14. 최종 판단

NSIGHT 아키텍처 의사결정 체계의 강점은 필요한 결정영역이 이미 넓고 구체적으로 식별되어 있다는 점이다. 가장 큰 위험은 이 풍부한 목록이 방대한 문서 저장소로만 남고 실제 개발·배포·운영의 차단조건으로 전환되지 않는 것이다.

따라서 다음 식을 프로젝트의 완료 기준으로 삼는 것이 적절하다.

```text
좋은 아키텍처 의사결정
= 명확한 선택과 근거
+ 닫힌 책임·계약·실패 의미
+ 코드·설정·OM 반영
+ 재현 가능한 테스트와 자동검증
+ 운영 KPI·Runbook·감사 증적
+ 만료 가능한 예외와 폐기조건
```

NSIGHT가 이 방향으로 진행하면 221개 TASK는 문서 부담이 아니라 프로젝트 전체의 변경 위험을 조기에 발견하고 운영 경험으로 계속 개선하는 **Architecture Control System**이 될 수 있다.

