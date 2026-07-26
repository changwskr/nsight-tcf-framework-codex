<!-- source: ztcf-집필본/NSIGHT TCF Chapter 부록 E. RTM과 테스트 결과서 예시.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 확장 부록 E. RTM과 테스트 결과서 예시

## 도입 전 안내말

개발이 완료됐다는 판단은 다음 문장만으로 내릴 수 없다.

소스코드를 작성했습니다.

화면에서 정상 동작했습니다.

단위 테스트가 통과했습니다.

테스트 결과는 PASS입니다.

운영 가능한 기능으로 완료됐음을 증명하려면 다음 질문에 답할 수 있어야 한다.

어떤 요구사항을 구현했는가?

요구사항은 어느 화면과 이벤트에 반영됐는가?

어떤 ServiceId와 프로그램이 처리하는가?

어떤 SQL과 DB 객체를 사용하는가?

어떤 테스트가 해당 요구사항을 검증하는가?

어느 환경과 데이터로 시험했는가?

기대 결과와 실제 결과가 무엇인가?

오류·로그·DB 결과를 확인했는가?

결함이 발생했다면 수정 후 재시험했는가?

현재 운영에 배포할 수 있는 상태인가?

이를 위해 사용하는 핵심 산출물이 다음 두 가지다.

RTM
Requirements Traceability Matrix

테스트 결과서
Test Execution Result Report

RTM은 요구사항과 구현·테스트 사이의 연결을 증명한다.

요구사항

→ 설계

→ 화면 이벤트

→ ServiceId

→ 프로그램

→ SQL·DB

→ 테스트

→ 결과·결함·증적

테스트 결과서는 해당 연결이 실제 환경에서 검증됐다는 사실을 증명한다.

테스트 환경

\+ 사전조건

\+ 테스트 데이터

\+ 수행절차

\+ 기대 결과

\+ 실제 결과

\+ 로그·DB·화면 증적

\+ 결함·재시험 결과

따라서 다음과 같은 결과서는 충분하지 않다.

TC-CT-001 PASS

이 한 줄만으로는 다음 사실을 확인할 수 없다.

어느 Version을 시험했는가?

어떤 데이터로 시험했는가?

실제로 어떤 결과가 나왔는가?

DB와 History는 정상인가?

GUID와 거래로그는 남았는가?

PASS를 누가 어떤 근거로 판정했는가?

본 부록은 제33장부터 제36장까지 설계·구현한 상담예약 CRUD를 기준으로 RTM과 테스트 결과서의 작성 예를 제공한다.

# 문서 개요

## 목적

본 부록의 목적은 NSIGHT TCF 프로젝트에서 요구사항의 구현 누락과 불필요 구현을 방지하고, 테스트 결과의 재현성·객관성·운영 추적성을 확보할 수 있는 표준 RTM과 테스트 결과서 양식을 제공하는 것이다.

세부 목적은 다음과 같다.

요구사항의 설계·구현·테스트 연결

화면 이벤트와 ServiceId 추적

프로그램과 SQL·DB 객체 추적

요구사항별 정상·오류·경계 테스트 식별

테스트 환경과 Artifact Version 기록

기대 결과와 실제 결과 비교

화면·로그·DB·Metric 증적 관리

결함과 재시험 결과 연결

테스트 진행률과 미검증 범위 파악

배포 품질 Gate 판정

변경 영향과 회귀 범위 산정

운영 배포 후 Smoke·Rollback 결과 관리

## 적용범위

| 구분 | 적용 대상 |
| --- | --- |
| 요구사항 | 기능·비기능·보안·운영 요구 |
| 설계 | 화면·거래·프로그램·SQL·DB 설계 |
| 구현 | Handler·Facade·Service·Rule·DAO·Mapper |
| 데이터 | Master·History·Index·Constraint |
| 테스트 | 단위·Mapper·통합·TCF 거래·연계 |
| 품질 | 보안·성능·장애·회귀 |
| 배포 | Health·Smoke·Rollback |
| 운영 | 로그·Metric·Alert·Runbook |
| 결함 | 등록·수정·재시험·종료 |
| 변경 | 요구사항 변경·호환·폐기 |

## 대상 독자

업무 분석가

화면 설계자

업무 개발자

테스트 담당자

QA 리더

애플리케이션 아키텍트

DBA·데이터 담당자

보안 담당자

성능시험 담당자

운영·DevOps 담당자

PMO·품질 담당자

업무 승인자

## 선행조건

승인된 요구사항 ID

화면·이벤트 ID

ServiceId·거래코드

프로그램 설계서

SQL·DB 설계서

테스트 ID 체계

테스트 환경정보

테스트 데이터 기준

오류코드 대장

결함 관리체계

Artifact·Git Version 관리

증적 저장소

# 핵심 관점

RTM의 목적은
표를 많이 작성하는 것이 아니다.

요구사항 하나를 선택했을 때
설계·소스·SQL·테스트·결과까지 이동할 수 있고,

실패한 테스트 하나를 선택했을 때
영향 요구사항과 배포위험으로 돌아갈 수 있어야 한다.

# 핵심 용어

| 용어 | 정의 |
| --- | --- |
| RTM | 요구사항과 설계·구현·테스트·결과를 연결한 추적 Matrix |
| 정방향 추적 | 요구사항에서 구현·테스트로 이동하는 추적 |
| 역방향 추적 | 소스·SQL·결함에서 영향 요구사항으로 돌아가는 추적 |
| Coverage | 요구사항·코드·분기 등이 테스트된 범위 |
| Test Case | 하나의 조건과 기대 결과를 검증하는 시험 명세 |
| Test Scenario | 여러 Test Case를 묶는 업무 흐름 |
| Test Suite | 실행 목적별 Test Case 집합 |
| Test Run | 특정 환경·Artifact로 수행한 한 번의 시험 실행 |
| Evidence | 결과를 입증하는 로그·화면·DB·Report |
| Defect | 기대 결과와 실제 결과가 일치하지 않는 결함 |
| Retest | 결함 수정 후 동일 조건으로 다시 수행하는 시험 |
| Regression Test | 변경으로 기존 기능이 손상되지 않았는지 확인하는 시험 |
| Blocked | 외부 조건 때문에 시험을 수행할 수 없는 상태 |
| Not Run | 아직 시험을 수행하지 않은 상태 |
| Conditional Pass | 잔여위험과 조건부 승인을 전제로 한 통과 |
| Exit Criteria | 테스트 단계를 종료할 수 있는 기준 |
| Test Baseline | 시험대상 요구사항·Artifact·환경의 승인 Version |
| Test Data Set | 재현 가능한 테스트 데이터 묶음 |
| Trace Evidence | GUID·ServiceId·SQL ID 등으로 연결된 실행 증적 |

# 문제 정의 및 설계 배경

## RTM이 없을 때 발생하는 문제

요구사항은 있는데 구현되지 않는다.

구현은 있는데 어느 요구사항인지 알 수 없다.

화면 이벤트와 서버 거래가 다르게 연결된다.

ServiceId는 있는데 테스트가 없다.

SQL이 변경됐지만 영향 화면을 찾지 못한다.

오류 테스트가 정상 테스트에 묻힌다.

PASS라고 기록했지만 증적이 없다.

결함 수정 후 회귀범위를 판단하지 못한다.

배포 후 어떤 거래를 Smoke해야 하는지 알 수 없다.

## 테스트 결과서가 부실할 때 발생하는 문제

다른 사람이 같은 결과를 재현할 수 없다.

개발환경과 운영 유사환경 결과가 섞인다.

테스트 데이터 오염으로 결과가 달라진다.

기대 결과와 실제 결과의 차이를 알 수 없다.

화면 성공만 보고 DB 부분 반영을 놓친다.

Timeout 응답만 보고 실제 Commit 결과를 놓친다.

결함이 수정됐는지 확인할 수 없다.

배포 승인자가 근거 없이 승인한다.

# 현행 구조와 보완방향

현재 NSIGHT TCF 테스트 구조는 다음 피라미드를 기준으로 한다.

Smoke·E2E
배포 후 대표 거래
▲
통합·TCF 거래
POST /{businessCode}/online
▲
Mapper·SQL
Test DB·H2·Oracle 검증
▲
단위 테스트
Handler·Service·Rule·Policy 중심

목표 비율 예시는 다음과 같다.

| 유형 | 목표 비중 | 주요 시점 |
| --- | --- | --- |
| 단위 테스트 | 70% 이상 | 개발·MR·CI |
| Mapper·SQL | 약 10% | SQL 변경 시 |
| 통합·TCF 거래 | 약 15% | 기능 완료·Merge |
| Smoke·E2E | 약 5% | 배포 전후 |

현재 기준 소스에는 다음 테스트 기반이 존재한다.

tcf-core
TransactionControl·Timeout·Header·Security·거래로그

tcf-web
Transaction Policy·Query Timeout·Control Repository

tcf-cache
Cache Configuration

sv-service
TCF 거래로그 통합 Test

보완이 필요한 부분:

업무 WAR별 Handler·Rule Test

신규 ServiceId별 TCF 거래 Test

Mapper SQL Test

Version 동시성 Test

Idempotency Test

Master·History Rollback Test

보안·권한 Test

성능·장애·Rollback 결과서

요구사항과 Test ID 연결

# 요구사항과 제약조건

## RTM 요구사항

모든 승인 요구사항에 고유 ID가 있어야 한다.

하나의 요구사항은 하나 이상의 Test와 연결돼야 한다.

모든 ServiceId는 요구사항 또는 공통 설계근거와 연결돼야 한다.

모든 변경 SQL은 Test와 연결돼야 한다.

FAIL·BLOCKED·NOT\_RUN 상태를 숨기지 않아야 한다.

폐기 요구사항과 Test는 이력을 유지해야 한다.

## 테스트 결과서 요구사항

실행 환경을 기록해야 한다.

Git Commit과 Artifact Version을 기록해야 한다.

사전조건과 데이터를 기록해야 한다.

기대 결과와 실제 결과를 분리해야 한다.

로그·DB·화면 증적을 연결해야 한다.

결함 ID와 재시험 결과를 기록해야 한다.

판정자와 수행일시를 기록해야 한다.

## 제약조건

운영 개인정보를 테스트 데이터로 무단 사용하지 않는다.

운영환경에서는 승인된 Smoke만 수행한다.

Test 결과에 Token·Password를 포함하지 않는다.

화면 캡처만으로 DB 정합성을 판단하지 않는다.

H2 결과만으로 Oracle 호환을 확정하지 않는다.

Mock Test만으로 Transaction·동시성을 확정하지 않는다.

PASS 비율만으로 배포 가능성을 판정하지 않는다.

# 설계 원칙

## 원칙 1. 요구사항 ID를 기준으로 연결한다

REQ

→ DES

→ SCR·EVT

→ SVC

→ PGM

→ SQL

→ DB

→ TC

→ DEF·EVD

## 원칙 2. 요약 RTM과 상세 RTM을 구분한다

요약 RTM:

요구사항별 진행상태와 품질 판정

상세 RTM:

요구사항–테스트 Case별 개별 연결

하나의 Cell에 TC-001~050을 무조건 넣으면 어떤 조건이 무엇을 검증하는지 알 수 없다.

## 원칙 3. PASS는 증적이 있을 때만 사용한다

Expected = Actual

\+ 증적 존재

\+ 환경·Version 식별

\+ Reviewer 확인

## 원칙 4. 테스트 유형별 책임을 구분한다

Rule 업무판단
→ 단위 Test

SQL Mapping
→ Mapper Test

TCF 전체 흐름
→ 거래 Test

운영 배포
→ Smoke Test

## 원칙 5. 테스트 데이터도 Version으로 관리한다

같은 Test ID에 매번 다른 임의 데이터를 사용하지 않는다.

## 원칙 6. 결함 수정은 재시험과 회귀시험을 요구한다

결함 수정

→ 원 Test 재시험

→ 영향 요구사항 회귀

→ RTM·결과서 갱신

## 원칙 7. 정상경로만으로 완료 판정하지 않는다

정상

\+ 입력 오류

\+ 권한

\+ 상태

\+ 중복

\+ 동시성

\+ Timeout

\+ Rollback

\+ 장애복구

# 대안 비교 및 의사결정

## RTM 관리방식

| 방식 | 장점 | 위험 | 판단 |
| --- | --- | --- | --- |
| Word 표 | 작성 쉬움 | 동시편집·검색 제한 | 소규모 보조 |
| Excel·Sheet | 필터·집계 용이 | 수동 정합성 | 기본 관리 가능 |
| ALM 도구 | 요구·Test·결함 연결 | 도구 비용 | 권장 |
| Git Markdown | 변경이력 명확 | 업무 사용자 접근성 | 기술 RTM |
| 코드 Annotation | 자동화 가능 | 전체 요구 추적 한계 | 보조 |
| 자동 생성 | 정합성 우수 | 기반 데이터 필요 | 중장기 목표 |

권장 구조:

공식 요구사항·결함 관리도구

\+ Git 기반 기술 RTM

\+ CI 자동검증

\+ 배포 승인용 Summary

# E.1 원본 RTM 예시

원본 부록의 기본 예시는 다음과 같다.

| 요구사항 ID | 설계 ID | 구현 위치 | 테스트 ID | 결과 |
| --- | --- | --- | --- | --- |
| REQ-CT-001 | DES-CT-001 | ReservationService.create | TC-CT-001~005 | PASS |
| REQ-CT-002 | DES-CT-002 | ReservationMapper.selectList | TC-CT-006~010 | PASS |
| REQ-CT-003 | DES-CT-003 | ReservationService.cancel | TC-CT-011~016 | 검토 중 |

이 표는 요약현황으로 사용할 수 있다.

그러나 운영 품질 Gate에 사용하려면 다음 정보가 추가돼야 한다.

요구사항 설명

화면·이벤트

ServiceId·거래코드

프로그램 계층

SQL·DB 객체

Test 유형

실행 결과

결함

증적

Baseline Version

승인자

# E.2 RTM ID 표준

## 요구사항 ID

REQ-{BC}-{SUB}-{NNN}

예:

REQ-CT-RSV-001

## 설계 ID

| 설계 | 형식 | 예 |
| --- | --- | --- |
| 화면 | DES-SCR-{BC}-{SUB}-{NNN} | DES-SCR-CT-RSV-001 |
| 거래 | DES-TX-{BC}-{SUB}-{NNN} | DES-TX-CT-RSV-001 |
| 프로그램 | DES-PGM-{BC}-{SUB}-{NNN} | DES-PGM-CT-RSV-001 |
| SQL | DES-SQL-{BC}-{SUB}-{NNN} | DES-SQL-CT-RSV-001 |
| DB | DES-DB-{BC}-{SUB}-{NNN} | DES-DB-CT-RSV-001 |
| 보안 | DES-SEC-{BC}-{SUB}-{NNN} | DES-SEC-CT-RSV-001 |
| 운영 | DES-OPS-{BC}-{SUB}-{NNN} | DES-OPS-CT-RSV-001 |

## 테스트 ID

TC-{BC}-{SUB}-{NNN}

예:

TC-CT-RSV-001

테스트 유형을 ID에 포함해야 하는 프로젝트에서는 다음 형식도 사용할 수 있다.

UT-CT-RSV-001
IT-CT-RSV-001
ST-CT-RSV-001
PT-CT-RSV-001

단, 한 프로젝트 안에서는 하나의 방식으로 통일한다.

## 결함 ID

DEF-{BC}-{SUB}-{NNN}

예:

DEF-CT-RSV-001

## 증적 ID

EVD-{BC}-{SUB}-{NNN}

# E.3 요구사항 정의 예시

상담예약 요구사항을 다음과 같이 정의한다.

| ID | 요구사항 | 유형 | 중요도 | 완료조건 |
| --- | --- | --- | --- | --- |
| REQ-CT-RSV-001 | 사용자는 기간·고객·상태 조건으로 상담예약 목록을 조회한다. | 기능 | High | 목록·Paging·권한 |
| REQ-CT-RSV-002 | 사용자는 상담예약 상세를 조회한다. | 기능 | High | 상세·마스킹·감사 |
| REQ-CT-RSV-003 | 사용자는 유효한 고객의 상담예약을 등록한다. | 기능 | Critical | Master·History |
| REQ-CT-RSV-004 | 동일 고객·동일 시간의 활성예약은 중복 등록할 수 없다. | 데이터 | Critical | DB Unique |
| REQ-CT-RSV-005 | READY 예약만 수정할 수 있다. | 업무 | Critical | 상태·Version |
| REQ-CT-RSV-006 | READY 예약만 취소할 수 있다. | 업무 | Critical | 논리 취소 |
| REQ-CT-RSV-007 | 다른 사용자의 변경을 덮어쓰지 않아야 한다. | 동시성 | Critical | Version |
| REQ-CT-RSV-008 | 동일 변경요청의 반복 처리로 중복효과가 발생하지 않아야 한다. | 멱등성 | Critical | Idempotency |
| REQ-CT-RSV-009 | Master와 History는 함께 Commit 또는 Rollback돼야 한다. | 정합성 | Critical | Transaction |
| REQ-CT-RSV-010 | 타 지점 예약은 조회·변경할 수 없다. | 보안 | Critical | 데이터권한 |
| REQ-CT-RSV-011 | 상담메모와 고객정보가 로그에 원문으로 남지 않아야 한다. | 보안 | Critical | Masking |
| REQ-CT-RSV-012 | 조회 p95는 목표 부하에서 3초 이하여야 한다. | 성능 | High | 성능 결과 |
| REQ-CT-RSV-013 | Timeout 발생 후 실제 처리결과를 확인할 수 있어야 한다. | 복원력 | Critical | UNKNOWN 대사 |
| REQ-CT-RSV-014 | 배포 후 대표 CRUD 거래를 Smoke Test해야 한다. | 운영 | High | Smoke 증적 |
| REQ-CT-RSV-015 | 장애 시 직전 Artifact로 복구할 수 있어야 한다. | 운영 | Critical | Rollback Drill |

# E.4 RTM 요약 Matrix 예시

| 요구사항 | 설계 | 화면·이벤트 | ServiceId | 구현 | 테스트 | 결과 | 결함 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-001 목록조회 | DES-TX-001 | E02 | selectList | Handler·Mapper | TC-001~008 | PASS | \- |
| REQ-002 상세조회 | DES-TX-002 | E03 | selectDetail | Service·SQL | TC-009~014 | PASS | \- |
| REQ-003 등록 | DES-TX-003 | E04 | create | 전체 계층 | TC-015~024 | PASS | DEF-001 종료 |
| REQ-004 중복방지 | DES-DB-001 | E04 | create | Rule·UK | TC-025~029 | PASS | \- |
| REQ-005 수정 | DES-TX-004 | E05 | update | Version UPDATE | TC-030~038 | PASS | \- |
| REQ-006 취소 | DES-TX-005 | E06 | cancel | 상태전이 | TC-039~046 | PASS | \- |
| REQ-007 동시성 | DES-DB-002 | E05·E06 | update·cancel | Version | TC-047~052 | PASS | \- |
| REQ-008 멱등성 | DES-TX-006 | 변경 이벤트 | 변경 Service | Idempotency | TC-053~059 | PARTIAL | DEF-004 |
| REQ-009 정합성 | DES-DB-003 | 변경 이벤트 | 변경 Service | Transaction | TC-060~065 | PASS | \- |
| REQ-010 데이터권한 | DES-SEC-001 | 전체 | 전체 | STF·Rule·SQL | TC-066~072 | PASS | \- |
| REQ-011 로그보안 | DES-SEC-002 | 전체 | 전체 | Masking | TC-073~078 | PASS | \- |
| REQ-012 p95 3초 | DES-OPS-001 | 조회 | 조회 Service | SQL·Pool | PT-001~005 | NOT RUN | \- |
| REQ-013 UNKNOWN | DES-TX-007 | 변경 | 변경 Service | 결과조회 | FT-001~004 | BLOCKED | DEF-007 |
| REQ-014 Smoke | DES-OPS-002 | 전체 | 5개 Service | Script | SMK-001~005 | PASS | \- |
| REQ-015 Rollback | DES-OPS-003 | \- | 대표 거래 | 배포 Script | RLB-001~003 | PASS | \- |

요약표의 REQ-001 표기는 가독성을 위한 축약이다. 공식 저장정보에는 전체 ID인 REQ-CT-RSV-001을 사용한다.

# E.5 RTM 상세 Matrix 예시

하나의 Test Case를 한 행으로 작성하는 방식이 가장 명확하다.

| 요구사항 ID | 화면 이벤트 | ServiceId | 프로그램 | SQL ID | DB 객체 | Test ID | 유형 | 기대 | 결과 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-003 | E04 | create | CtReservationService.create | INS-001 | Master | TC-015 | 단위 | 정상 Command | PASS |
| REQ-003 | E04 | create | CtReservationHandler | \- | \- | TC-016 | Handler | Facade 호출 | PASS |
| REQ-003 | E04 | create | 전체 흐름 | INS-001·002 | Master·History | TC-017 | 통합 | 두 행 Commit | PASS |
| REQ-004 | E04 | create | Rule | SEL-003 | Master | TC-025 | 단위 | 중복 오류 | PASS |
| REQ-004 | E04 | create | Mapper | INS-001 | UK | TC-026 | DB | 1건만 성공 | PASS |
| REQ-007 | E05 | update | Service·Mapper | UPD-001 | Master | TC-047 | 동시성 | A 성공·B 충돌 | PASS |
| REQ-009 | E05 | update | Facade | UPD-001·INS-003 | Master·History | TC-060 | Rollback | History 실패 시 전체 원복 | PASS |
| REQ-010 | E03 | selectDetail | Rule·Mapper | SEL-002 | Master | TC-066 | 보안 | 타 지점 조회 불가 | PASS |

# E.6 RTM 필수 Column

## 기본 Column

| Column | 설명 |
| --- | --- |
| 요구사항 ID | 공식 요구사항 |
| 요구사항 설명 | 검증할 업무 내용 |
| 유형 | 기능·보안·성능·운영 |
| 중요도 | Critical·High·Medium·Low |
| 설계 ID | 화면·거래·프로그램·DB |
| 화면 ID | 대상 화면 |
| 이벤트 ID | 사용자 동작 |
| ServiceId | 실행 거래 |
| 거래코드 | 운영 식별 |
| 프로그램 위치 | Class·Method |
| SQL ID | Mapper·관리 SQL |
| DB 객체 | Table·Index·Constraint |
| 테스트 ID | 검증 Case |
| 테스트 유형 | Unit·Integration 등 |
| 결과 | PASS·FAIL 등 |
| 결함 ID | 실패 연결 |
| 증적 ID | 실행 증거 |
| Baseline | 요구·Artifact Version |
| Owner | 구현·검증 책임 |
| 비고 | 미결·제약 |

## 확장 Column

보안등급

개인정보 포함 여부

감사대상 여부

Timeout 정책

성능 목표

배포 Smoke 여부

Rollback 대상 여부

자동화 여부

최종 실행일

재시험일

승인자

# E.7 RTM 상태 정의

| 상태 | 의미 |
| --- | --- |
| DRAFT | 요구사항·연결 초안 |
| DESIGNED | 설계 연결 완료 |
| IMPLEMENTED | 구현 위치 확인 |
| TEST\_READY | 테스트 준비 완료 |
| PASS | 모든 필수 Case 성공 |
| PARTIAL | 일부 Case 성공·미완료 존재 |
| FAIL | 필수 Case 실패 |
| BLOCKED | 환경·데이터·연계로 실행 불가 |
| NOT\_RUN | 미실행 |
| DEFERRED | 승인 후 차기 Release 이관 |
| DEPRECATED | 폐기 예정 |
| CLOSED | 최종 승인 완료 |

# E.8 테스트 유형별 RTM 연결

| 테스트 유형 | 검증 대상 | 요구사항 연결 |
| --- | --- | --- |
| 단위 | Rule·Service 분기 | 업무규칙 |
| Handler | ServiceId·Facade 위임 | 거래 설계 |
| Mapper·SQL | Parameter·Result·SQL | 데이터 요구 |
| 통합 | Spring·DB·Transaction | 기능·정합성 |
| TCF 거래 | STF·Dispatcher·ETF·TX Log | 표준 거래 |
| 연계 | 외부 계약·Timeout·Mapping | 연계 요구 |
| 보안 | 인증·인가·Injection·Masking | 보안 요구 |
| 성능 | TPS·p95·자원 | 비기능 요구 |
| 장애 | Timeout·Failover·Rollback | 복원력 |
| Smoke | 배포 후 대표 거래 | 운영 요구 |
| Rollback | 직전 Version 복귀 | 운영 요구 |

# E.9 테스트 케이스 명세서 표준

## 기본 Header

| 항목 | 예 |
| --- | --- |
| Test ID | TC-CT-RSV-047 |
| Test명 | 동일 Version 동시 수정 |
| 요구사항 | REQ-CT-RSV-007 |
| 설계 | DES-TX-CT-RSV-004 |
| 테스트 유형 | 동시성 통합 |
| 중요도 | Critical |
| 자동화 | Y |
| 담당자 | 홍길동 |
| Reviewer | 김검토 |
| 작성일 | 2026-07-18 |

## 실행 조건

| 항목 | 내용 |
| --- | --- |
| 환경 | STG |
| Application | ct-service 1.0.0 |
| Git Commit | abcdef123456 |
| Artifact SHA-256 | <checksum> |
| JDK | 21 |
| DB | Oracle Test Schema |
| Profile | stg |
| 선행 데이터 | READY·Version 3 예약 |
| 테스트 사용자 | 지점 B001 담당자 2명 |
| 외부 Stub | 고객조회 정상 |

## Test Step

| 순서 | 수행 | 기대 결과 |
| --- | --- | --- |
| 1 | 사용자 A가 상세조회 | Version 3 |
| 2 | 사용자 B가 상세조회 | Version 3 |
| 3 | A가 수정 요청 | 성공·Version 4 |
| 4 | B가 Version 3으로 수정 | 동시성 오류 |
| 5 | Master 조회 | A의 변경만 존재 |
| 6 | History 조회 | A의 이력만 존재 |
| 7 | 거래로그 조회 | A 성공·B 업무 오류 |

# E.10 테스트 결과 상태

| 결과 | 정의 |
| --- | --- |
| PASS | 기대 결과와 실제 결과가 모두 일치 |
| FAIL | 하나 이상의 기대 결과 불일치 |
| BLOCKED | 환경·데이터·연계 문제로 실행 불가 |
| NOT\_RUN | 실행하지 않음 |
| PARTIAL | 일부 Step만 검증 |
| CONDITIONAL\_PASS | 승인된 잔여위험이 존재 |
| N/A | 대상 기능에 적용되지 않음 |

## PASS 판정조건

모든 Step 수행

기대 결과와 일치

필수 증적 존재

결함 없음 또는 종료

Baseline 식별

Reviewer 확인

## FAIL 판정조건

다음 중 하나라도 해당하면 FAIL이다.

응답코드 불일치

DB 결과 불일치

History 누락

로그·GUID 누락

권한 우회

Timeout 결과 불명

예상하지 못한 오류

환경·Version 불명확

# E.11 단위 테스트 결과서 예시

## Test 개요

| 항목 | 내용 |
| --- | --- |
| Suite ID | UT-CT-RSV-001 |
| 대상 | CtReservationRule |
| 실행 명령 | gradle :ct-service:test |
| Test Class | CtReservationRuleTest |
| 실행일 | 2026-07-18 |
| 결과 | PASS |

## Case 결과

| Test ID | 검증 내용 | 기대 | 실제 | 결과 |
| --- | --- | --- | --- | --- |
| TC-001 | 고객번호 필수 | 오류코드 | 동일 | PASS |
| TC-002 | 예약일시 과거 | 오류 | 동일 | PASS |
| TC-003 | Page Size 101 | 오류 | 동일 | PASS |
| TC-004 | READY 수정 | 허용 | 허용 | PASS |
| TC-005 | COMPLETED 수정 | 상태 오류 | 동일 | PASS |
| TC-006 | Version 불일치 | 충돌 오류 | 동일 | PASS |

## 증적

build/reports/tests/test/index.html

CtReservationRuleTest.xml

CI Pipeline #20260718-152

Git Commit abcdef123456

# E.12 Mapper·SQL 테스트 결과서 예시

## 대상 SQL

| 항목 | 내용 |
| --- | --- |
| Mapper | CtReservationMapper |
| Method | selectReservationList |
| 관리 SQL ID | CT-RSV-SEL-001 |
| 대상 DB | Oracle STG |
| 데이터 건수 | Master 1,000,000건 |
| Index | IX\_CT\_RSV\_BRANCH\_DTM |

## 검증 결과

| 검증 | 기대 | 실제 | 결과 |
| --- | --- | --- | --- |
| Bind Parameter | #{} 사용 | 사용 | PASS |
| 지점 Scope | 필수 | 포함 | PASS |
| Page Size | 최대 100 | 적용 | PASS |
| 정렬 | DTM+ID | 동일 | PASS |
| 빈 결과 | 빈 List | 동일 | PASS |
| 실행계획 | Index Range Scan | 동일 | PASS |
| SQL p95 | 500ms 이하 | 218ms | PASS |
| Query Timeout | 2초 | 적용 | PASS |

## 증적

EVD-CT-RSV-021\_execution-plan.txt

EVD-CT-RSV-022\_sql-monitor.html

EVD-CT-RSV-023\_mapper-test-report.xml

# E.13 TCF 거래 통합 결과서 예시

## 대상

ServiceId
CT.Reservation.create

Endpoint
POST /ct/online

거래코드
CT-REG-0101

## 검증범위

OnlineTransactionController

→ TCF.process

→ STF

→ Dispatcher

→ Handler

→ Facade

→ Service·Rule

→ DAO·Mapper

→ ETF

→ TCF\_TX\_LOG

## 결과

| 항목 | 기대 | 실제 | 결과 |
| --- | --- | --- | --- |
| HTTP | 표준 정책 | 200 | PASS |
| resultCode | 성공 | S0000 | PASS |
| GUID | 생성 | 생성됨 | PASS |
| ServiceId | 일치 | 일치 | PASS |
| Master | 1건 | 1건 | PASS |
| History | 1건 | 1건 | PASS |
| 거래로그 | 시작·종료 | 존재 | PASS |
| 감사로그 | 생성 | 존재 | PASS |
| 개인정보 | 마스킹 | 원문 없음 | PASS |

## 대표 증적

Request
EVD-CT-RSV-030\_request.json

Response
EVD-CT-RSV-031\_response.json

거래로그
EVD-CT-RSV-032\_txlog.json

DB 검증
EVD-CT-RSV-033\_db-result.txt

감사로그
EVD-CT-RSV-034\_audit.json

Token과 고객정보 원문은 증적에서 제거한다.

# E.14 동시성 테스트 결과서 예시

## 시나리오

동일 예약 Version 3

사용자 A와 B가 동시 수정

## 실행 결과

| 항목 | A | B |
| --- | --- | --- |
| Request Version | 3 | 3 |
| 시작시각 | 동일 구간 | 동일 구간 |
| UPDATE 영향 행 | 1 | 0 |
| 결과 | SUCCESS | CONFLICT |
| 최종 Version | 4 | 4 |
| History | 1건 | 0건 |

## 최종 DB

Master
A의 변경값

Version
4

History
A 변경 1건

B 변경
없음

## 판정

Lost Update 없음

부분 반영 없음

동시성 오류코드 정상

화면 재조회 안내 정상

PASS

# E.15 Idempotency 테스트 결과서 예시

| 실행 | Key | Request | 기대 | 결과 |
| --- | --- | --- | --- | --- |
| 1 | K-001 | 예약 등록 A | 신규 생성 | PASS |
| 2 | K-001 | 동일 요청 | 기존 결과 | PASS |
| 3 | K-001 | 다른 시간 | Key 재사용 오류 | PASS |
| 4 | K-002 | 같은 업무값 | 업무 중복 오류 | PASS |
| 5 | K-003 | 처리 중 반복 | PROCESSING | PASS |
| 6 | K-004 | 응답 유실 | 결과조회 | PARTIAL |

검증:

Master 1건

History 1건

Idempotency SUCCESS 1건

중복 업무효과 없음

Key 원문 로그 없음

# E.16 Transaction·Rollback 결과서 예시

## History 실패 주입

| 단계 | 결과 |
| --- | --- |
| Master UPDATE | 실행 |
| History INSERT | 강제 오류 |
| Transaction | Rollback |
| Master 최종값 | 변경 전 |
| Version | 변경 전 |
| History | 0건 |
| 응답 | 시스템 오류 |
| 거래로그 | FAIL |
| Alert | 발생 |

판정:

부분 Commit 없음

원인 예외 보존

사용자 내부정보 미노출

PASS

# E.17 보안 테스트 결과서 예시

| Test ID | 공격·조건 | 기대 | 실제 | 결과 |
| --- | --- | --- | --- | --- |
| SEC-001 | Token 없음 | 401 | 401 | PASS |
| SEC-002 | 만료 Token | 401 | 401 | PASS |
| SEC-003 | 타 지점 예약 | 접근 차단 | 차단 | PASS |
| SEC-004 | Body userId 위조 | 무시 | 무시 | PASS |
| SEC-005 | SQL Injection 문자열 | Bind | 차단 | PASS |
| SEC-006 | XSS 메모 | 출력 Encoding | 정상 | PASS |
| SEC-007 | Token 로그검색 | 없어야 함 | 없음 | PASS |
| SEC-008 | 상담메모 로그검색 | 없어야 함 | 없음 | PASS |
| SEC-009 | Actuator 외부 접근 | 차단 | 차단 | PASS |
| SEC-010 | 권한 없는 취소 | 403 | 403 | PASS |

# E.18 성능 테스트 결과서 예시

## 환경

| 항목 | 내용 |
| --- | --- |
| Profile | STG |
| WAS | 운영 유사 4대 |
| VM | 승인 사양 |
| DB | 운영 유사 데이터 |
| 데이터 건수 | 예약 1억건 |
| Artifact | ct-service 1.0.0 |
| 부하도구 | JMeter·nGrinder 등 |
| 시험시간 | 60분 |
| 거래 Mix | 조회 80%·변경 20% |

## 결과 Summary

| 지표 | 목표 | 실제 | 판정 |
| --- | --- | --- | --- |
| TPS | 1,200 이상 | 1,246 | PASS |
| 전체 p95 | 3초 이하 | 2.41초 | PASS |
| 목록 p95 | 3초 이하 | 2.72초 | PASS |
| 상세 p95 | 2초 이하 | 1.15초 | PASS |
| 오류율 | 1% 이하 | 0.18% | PASS |
| Busy Thread | 70% 이하 | 64% | PASS |
| Hikari 사용률 | 80% 이하 | 72% | PASS |
| Pool Pending | 지속 0 | 최대 2·일시 | CONDITIONAL |
| Heap | 70% 이하 | 63% | PASS |
| SQL p95 | 500ms 이하 | 438ms | PASS |

## Conditional 항목

Pool Pending 2건 발생

Owner
DB·Application 개발

조치
목록 SQL과 Connection 보유시간 점검

기한
운영 전 완료

재시험
PT-CT-RSV-006

# E.19 장애 테스트 결과서 예시

## 외부 고객조회 Timeout

| 항목 | 기대 | 실제 | 결과 |
| --- | --- | --- | --- |
| 외부 Read Timeout | 설정값 내 | 1.5초 | PASS |
| 전체 거래 Timeout | 5초 이내 | 2.1초 | PASS |
| Master | 미생성 | 미생성 | PASS |
| History | 미생성 | 미생성 | PASS |
| 오류코드 | 연계 Timeout | 일치 | PASS |
| 거래로그 | FAIL·구간 식별 | 일치 | PASS |
| Retry | 변경거래 자동 Retry 없음 | 없음 | PASS |
| 복구 후 거래 | 성공 | 성공 | PASS |

## DB 응답 유실·UNKNOWN

| 항목 | 결과 |
| --- | --- |
| Client 응답 | Timeout |
| DB Commit | 성공 |
| Master | 존재 |
| History | 존재 |
| Idempotency | SUCCESS |
| 결과조회 | 기존 성공 반환 |
| 중복 재실행 | 없음 |
| 판정 | PASS |

# E.20 배포·Smoke 결과서 예시

## 배포정보

| 항목 | 내용 |
| --- | --- |
| Release | CT Reservation 1.0.0 |
| Tag | v1.0.0 |
| Commit | abcdef123456 |
| WAR | ct.war |
| SHA-256 | <checksum> |
| Deploy ID | DEP-CT-20260718-001 |
| 대상 | AP01, AP02 |
| 방식 | Rolling |

## 4단계 검증

| Instance | Liveness | Readiness | Deep | Smoke | 결과 |
| --- | --- | --- | --- | --- | --- |
| AP01 | PASS | PASS | PASS | PASS | 정상 |
| AP02 | PASS | PASS | PASS | PASS | 정상 |

## Smoke 거래

| Test ID | ServiceId | 기대 | 결과 |
| --- | --- | --- | --- |
| SMK-001 | selectList | 성공 | PASS |
| SMK-002 | selectDetail | 성공 | PASS |
| SMK-003 | create | Master·History | PASS |
| SMK-004 | update | Version 증가 | PASS |
| SMK-005 | cancel | CANCELED | PASS |

# E.21 Rollback 결과서 예시

| 항목 | 결과 |
| --- | --- |
| 신규 Traffic 차단 | 성공 |
| 직전 WAR 복원 | 성공 |
| 설정 복원 | 성공 |
| Liveness | PASS |
| Readiness | PASS |
| Deep | PASS |
| 대표 조회 | PASS |
| 대표 변경 | PASS |
| 데이터 대사 | 정상 |
| 복구시간 | 12분 |
| 목표 RTO | 30분 |
| 판정 | PASS |

Rollback 결과서에는 단순히 Script 종료코드만 기록하지 않는다.

서비스 복구

\+ 업무 거래

\+ 데이터

\+ 로그·Metric

\+ Traffic

\+ 복구시간

을 함께 확인한다.

# E.22 테스트 실행 결과서 표준 양식

## 문서 Header

| 항목 | 내용 |
| --- | --- |
| 문서명 | 상담예약 기능 테스트 결과서 |
| 문서 Version | 1.0 |
| Release | CT 1.0.0 |
| 작성자 |  |
| 검토자 |  |
| 승인자 |  |
| 시험 시작 |  |
| 시험 종료 |  |
| 작성일 |  |

## 시험대상

요구사항 Baseline

설계 Baseline

Git Branch

Git Commit

Artifact Version

DB Migration Version

OM Seed Version

Profile

시험환경

## 결과 Summary

| 유형 | 계획 | 실행 | PASS | FAIL | BLOCKED | 미실행 |
| --- | --- | --- | --- | --- | --- | --- |
| 단위 | 120 | 120 | 120 | 0 | 0 | 0 |
| Mapper | 20 | 20 | 19 | 1 | 0 | 0 |
| 통합 | 35 | 35 | 34 | 1 | 0 | 0 |
| 보안 | 25 | 25 | 25 | 0 | 0 | 0 |
| 성능 | 10 | 8 | 7 | 0 | 1 | 2 |
| 장애 | 12 | 10 | 9 | 1 | 0 | 2 |
| Smoke | 5 | 5 | 5 | 0 | 0 | 0 |
| 합계 | 227 | 223 | 219 | 3 | 1 | 4 |

## 결함 Summary

| 등급 | Open | 수정완료 | 재시험완료 | 이관 |
| --- | --- | --- | --- | --- |
| Blocker | 0 | 0 | 0 | 0 |
| Critical | 1 | 1 | 1 | 0 |
| Major | 2 | 1 | 1 | 1 |
| Minor | 5 | 3 | 3 | 2 |

## 최종 판정

PASS

CONDITIONAL PASS

FAIL

NO-GO

판정 근거를 문장으로 작성한다.

예:

Critical 결함은 모두 수정·재시험 완료했습니다.

성능 Test 2건은 외부 성능환경 미준비로 미실행 상태이며,
해당 요구사항은 운영 전 필수 Gate이므로
현재 판정은 NO-GO입니다.

# E.23 테스트 Case 실행결과 상세 양식

| 항목 | 내용 |
| --- | --- |
| Test ID |  |
| Test명 |  |
| 요구사항 ID |  |
| ServiceId |  |
| Test 유형 |  |
| 환경 |  |
| Artifact |  |
| 실행자 |  |
| 실행일시 |  |
| 사전조건 |  |
| 테스트 데이터 |  |
| 기대 결과 |  |
| 실제 결과 |  |
| HTTP 결과 |  |
| 오류코드 |  |
| DB 결과 |  |
| History 결과 |  |
| 거래로그 |  |
| 감사로그 |  |
| Metric |  |
| 증적 ID |  |
| 결함 ID |  |
| 결과 |  |
| Reviewer |  |

# E.24 결함 관리

## 결함 필수정보

| 항목 | 설명 |
| --- | --- |
| 결함 ID | 고유 식별 |
| 발견 Test | Test ID |
| 영향 요구사항 | REQ ID |
| 제목 | 증상을 명확히 표현 |
| 환경 | Profile·Instance |
| Artifact | Version·Commit |
| 재현절차 | 순서별 작성 |
| 기대 결과 | 설계 기준 |
| 실제 결과 | 발생 결과 |
| 오류코드 | 응답 |
| GUID·TraceId | 로그 추적 |
| DB 영향 | Master·History |
| 심각도 | Blocker~Minor |
| 원인 | 분석 결과 |
| 수정 Commit | 변경 Version |
| 재시험 | Retest ID |
| 회귀범위 | 영향 Test |
| 상태 | Open~Closed |

## 결함 상태

OPEN

ANALYZING

ASSIGNED

FIXING

FIXED

READY\_FOR\_RETEST

RETEST\_FAILED

RETEST\_PASSED

CLOSED

DEFERRED

REJECTED

DUPLICATE

## 결함 예시

| 항목 | 내용 |
| --- | --- |
| ID | DEF-CT-RSV-001 |
| 발견 Test | TC-CT-RSV-060 |
| 제목 | 수정 History 실패 시 Master가 Commit됨 |
| 심각도 | Critical |
| 기대 | 전체 Rollback |
| 실제 | Master Version 증가 |
| 원인 | REQUIRES\_NEW History Transaction |
| 수정 | History를 Facade Transaction 참여로 변경 |
| Retest | RT-CT-RSV-001 |
| 결과 | PASS |
| 회귀 | 등록·수정·취소 Rollback Test |
| 상태 | CLOSED |

# E.25 재시험과 회귀시험

## 재시험

동일 Test 조건

동일 또는 승인된 데이터

수정 Artifact

기대 결과 재검증

결함 현상 미발생 확인

## 회귀시험

결함 수정 영향범위를 기준으로 선정한다.

예:

History Transaction 수정

→ 등록 History

→ 수정 History

→ 취소 History

→ 오류 Rollback

→ Timeout UNKNOWN

→ 거래로그 결과

## 재시험 결과

| 결함 | 원 Test | Retest | 결과 | 회귀 Suite |
| --- | --- | --- | --- | --- |
| DEF-001 | TC-060 | RT-001 | PASS | REG-TX-001 |
| DEF-002 | SEC-005 | RT-002 | FAIL | 보안 Suite |
| DEF-003 | PT-004 | RT-003 | PASS | 조회 성능 |

결함을 FIXED 상태만으로 종료하지 않는다.

FIXED
→ 개발자 수정 완료

CLOSED
→ QA 재시험 완료

# E.26 테스트 증적 관리

## 증적 유형

| 유형 | 예 |
| --- | --- |
| 요청·응답 | JSON |
| 화면 | Screenshot |
| 로그 | GUID 검색결과 |
| DB | 검증 SQL 결과 |
| 거래로그 | TCF\_TX\_LOG |
| 감사 | Audit Event |
| 성능 | JMeter·APM Report |
| 보안 | Scan Report |
| 장애 | 장애주입·복구 Log |
| 배포 | Deploy Log |
| Artifact | SHA-256 |
| CI | Pipeline URL |
| Test | JUnit XML·HTML |

## 증적 파일명

{EvidenceId}\_{TestId}\_{type}\_{timestamp}.{ext}

예:

EVD-CT-RSV-032\_TC-CT-RSV-017\_txlog\_20260718.json

## 증적 보안

Token 제거

Password 제거

고객정보 마스킹

운영 실제 데이터 제거

파일경로 보호

접근권한 설정

보존기간 적용

## Screenshot 주의

화면 캡처는 다음을 증명하지 못한다.

DB Commit

History 저장

동시성

거래로그

Timeout 후 결과

다른 사용자 권한

따라서 화면 증적과 DB·로그 증적을 함께 사용한다.

# E.27 테스트 데이터 관리

## 데이터 분류

| 분류 | 용도 |
| --- | --- |
| 정상 데이터 | 정상 흐름 |
| 경계 데이터 | 최소·최대 |
| 오류 데이터 | 형식·상태 오류 |
| 권한 데이터 | 지점·Role |
| 중복 데이터 | Unique 검증 |
| 동시성 데이터 | Version 충돌 |
| 성능 데이터 | 운영 유사 분포 |
| 장애 데이터 | 실패·재처리 |
| Smoke 데이터 | 운영 전용 식별 |

## 데이터 식별

TEST\_CT\_RSV\_\*

SMOKE\_CT\_RSV\_\*

PERF\_CT\_RSV\_\*

## 데이터 상태

PREPARED

IN\_USE

CONSUMED

RESET

ARCHIVED

DELETED

## 데이터 원칙

운영 개인정보 무단 복제 금지

Masking·가명처리

Test 간 데이터 독립

재실행 가능

기대 상태 명시

정리 Script 제공

Smoke 데이터 구분

# E.28 정상 테스트 흐름

1\. 요구사항 Baseline을 확정한다.
2\. RTM에서 요구사항별 Test ID를 확인한다.
3\. 승인된 Artifact와 환경을 배포한다.
4\. 테스트 데이터를 준비한다.
5\. 사전조건을 검증한다.
6\. Test Step을 수행한다.
7\. 응답 결과를 확인한다.
8\. DB Master·History를 확인한다.
9\. GUID로 거래·감사로그를 확인한다.
10\. Metric과 오류 여부를 확인한다.
11\. 기대 결과와 실제 결과를 비교한다.
12\. 증적을 등록한다.
13\. PASS·FAIL을 판정한다.
14\. FAIL이면 결함을 등록한다.
15\. 수정 후 재시험·회귀시험한다.
16\. RTM과 결과 Summary를 갱신한다.

# E.29 오류·Timeout·장애 흐름

## 테스트 환경 장애

DB 접속 불가

→ Test BLOCKED

→ Application 결함으로 기록하지 않음

→ 환경결함 별도 등록

→ 복구 후 재실행

## 테스트 데이터 오류

사전 상태가 READY가 아님

→ 해당 Test 결과 무효

→ 데이터 재준비

→ Test 재실행

## Timeout Test

Client Timeout

→ DB·History·Idempotency 확인

→ 실제 결과 판정

→ UNKNOWN이면 결과조회 Test 수행

## 결함 수정 실패

RETEST\_FAILED

→ 결함 Reopen

→ 수정 원인 재분석

→ 영향 Test 재선정

## 증적 누락

기능은 정상으로 보임

하지만 필수 증적 없음

→ PASS 불가

→ PARTIAL 또는 재실행

# E.30 정상 예시

요구사항
REQ-CT-RSV-007
Lost Update 방지

설계
Version 기반 낙관적 Lock

화면
CT-RSV-0001-E05

ServiceId
CT.Reservation.update

프로그램
CtReservationService.update

SQL
CT-RSV-UPD-001

DB
VERSION\_NO

테스트
TC-CT-RSV-047

환경
STG
ct-service 1.0.0

기대
A 성공
B 동시성 오류

실제
A 영향 행 1
B 영향 행 0

DB
A의 변경만 존재
Version 4

History
A 1건

증적
Request·Response·DB·TX Log

판정
PASS

# E.31 금지 예시

요구사항 ID 없이 테스트를 작성한다.

한 Test Case가 어떤 요구사항을 검증하는지 기록하지 않는다.

Test ID를 실행할 때마다 새로 만든다.

모든 Test 결과를 PASS라고 입력한다.

실행하지 않은 Test를 PASS로 기록한다.

환경 문제를 Application PASS로 처리한다.

화면 캡처만으로 DB Transaction을 검증한다.

Mock Test로 실제 SQL이 정상이라고 판정한다.

H2 Test만으로 Oracle 운영호환을 확정한다.

단일 사용자 Test로 동시성 PASS를 판정한다.

Timeout 응답만 확인하고 DB 결과를 보지 않는다.

결함 수정 후 원 Test를 재실행하지 않는다.

결함 상태 FIXED를 CLOSED로 간주한다.

FAIL Test를 RTM에서 삭제한다.

폐기 요구사항 ID를 다른 기능에 재사용한다.

Test 데이터의 초기상태를 기록하지 않는다.

Git Commit·Artifact Version을 기록하지 않는다.

Token·고객정보가 포함된 증적을 공유한다.

PASS 비율만으로 운영 Go를 결정한다.

Critical 요구사항 미실행 상태에서 Conditional PASS를 남발한다.

Test Range \`001~100\`만 기록하고 개별 연결을 생략한다.

배포 후 Health만 확인하고 업무 Smoke를 생략한다.

# E.32 책임 경계와 RACI

| 활동 | 업무 | 분석 | 개발 | QA | AA | DBA | 보안 | 운영 | DevOps | PMO |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 요구사항 ID | A | R | C | C | C | I | C | I | I | C |
| RTM 작성 | C | R | R | A/R | C | C | C | C | I | C |
| 단위 Test | I | I | A/R | C | C | I | I | I | I | I |
| Mapper Test | I | I | R | C | C | A/C | I | I | I | I |
| 통합 Test | C | I | R | A/R | C | C | C | C | C | I |
| 보안 Test | I | I | C | R | C | I | A/R | C | C | I |
| 성능 Test | C | I | C | A/R | A/C | C | I | C | C | I |
| 장애 Test | I | I | C | R | A/C | C | C | A/R | C | I |
| Smoke | A/C | I | C | R | C | C | I | A/R | R | I |
| 결함 수정 | I | I | A/R | C | C | C | C | I | I | I |
| 재시험 | I | I | C | A/R | I | C | C | C | I | I |
| Go·No-Go | A/C | I | C | C | A/C | C | C | A/C | C | A |
| 증적 보존 | I | I | C | A/R | I | C | C | C | C | C |

# E.33 성능·용량·확장성

## RTM 규모

대규모 프로젝트에서는 다음 수량이 발생할 수 있다.

요구사항 2,000개

ServiceId 1,500개

프로그램 5,000개

SQL 10,000개

Test Case 30,000개

결함 5,000개

단일 Word 표로 관리하기 어렵다.

권장:

중앙 데이터 저장

고유 ID

관계형 연결

자동 집계

변경이력

Dashboard

Export

## 증적 용량

화면 이미지

JMeter 결과

APM Export

로그 파일

DB 결과

Video

가 누적되므로 다음을 정의한다.

파일크기 제한

압축

중복 제거

보존기간

접근권한

Archive

## 자동 Test

Test Suite가 커질수록 실행시간이 증가한다.

MR
빠른 단위·변경 Module

Nightly
전체 회귀

Release
통합·보안·성능·장애

Post Deploy
Smoke

로 계층화한다.

# E.34 보안·개인정보·감사

## Test 환경 보안

운영계정 사용 금지

테스트 전용 Role

최소 DB 권한

Secret 외부화

관리망 접근

테스트 종료 후 계정 폐기

## Test 데이터

실명·주민번호·연락처 원문 금지

가명처리

Masking

재식별 가능성 검토

보존·파기

## 증적

Token Redaction

화면 개인정보 Masking

DB 결과 최소화

접근기록

다운로드 통제

공유기한

## 테스트 감사

다음 행위는 감사대상이 될 수 있다.

운영 Smoke

관리자 권한 Test

대량 데이터 조회

파일 Download

장애 주입

DB DML

Rollback 실행

# E.35 자동검증 및 품질 Gate

## RTM 자동검증

승인 요구사항 중 Test 없는 항목

Test ID 중복

ServiceId 중복

구현 위치 없는 요구사항

FAIL인데 PASS로 집계된 항목

폐기 요구사항을 참조하는 Test

결함 없는 FAIL

증적 없는 PASS

Critical NOT\_RUN

Baseline 불일치

## CI Gate

Compile

Unit Test

Architecture Test

Mapper Test

Integration Test

Security Scan

Coverage

Artifact

Test Report Publish

## 배포 Gate

Critical 요구사항 100% 실행

Blocker·Critical Open 0

필수 성능 PASS

보안 Critical 0

Rollback Drill PASS

Smoke Script 준비

RTM 최신화

## 결과 Gate 예

| 조건 | 판정 |
| --- | --- |
| 필수 Test PASS | Go 후보 |
| Critical FAIL | No-Go |
| Critical NOT\_RUN | No-Go |
| Blocker Open | No-Go |
| 증적 없는 PASS | 재시험 |
| Major 1건 | Conditional 검토 |
| Rollback 미시험 | No-Go |

# E.36 테스트 품질 Metric

요구사항 Test Coverage

자동화율

PASS율

결함 발견율

결함 재오픈율

Test 실행률

Critical 미실행 수

Regression 실패 수

결함 평균 종료시간

Smoke 성공률

Flaky Test 비율

## 계산 예

요구사항 Coverage

\=
Test와 연결된 요구사항 수
÷ 전체 요구사항 수
× 100

Test 실행률

\=
실행 Test 수
÷ 계획 Test 수
× 100

PASS율은 품질의 유일한 지표가 아니다.

쉬운 Test만 많이 수행

→ PASS율 높음

Critical 동시성 Test 미실행

→ 실제 위험 큼

# E.37 Flaky Test 관리

Flaky Test란 같은 코드와 환경에서도 결과가 반복적으로 바뀌는 Test다.

원인:

시간 의존

Test 순서 의존

공유 데이터

비동기 대기

Network 불안정

고정되지 않은 Random

Port 충돌

외부 시스템

금지:

실패하면 다시 실행해 PASS로 변경

Flaky Test를 무기한 Ignore

관리:

Flaky 표시

Owner

원인

격리

수정기한

반복 실행 통계

해결 후 정상 Suite 복귀

# E.38 완료 체크리스트

## RTM

| 점검 | 완료 |
| --- | --- |
| 모든 요구사항에 고유 ID가 있다. | □ |
| 설계 ID가 연결된다. | □ |
| 화면·이벤트가 연결된다. | □ |
| ServiceId·거래코드가 연결된다. | □ |
| 프로그램·SQL·DB가 연결된다. | □ |
| 모든 요구사항에 Test가 있다. | □ |
| 모든 Test에 요구사항이 있다. | □ |
| 정방향·역방향 추적이 가능하다. | □ |
| 폐기·변경 이력이 보존된다. | □ |
| RTM Baseline이 승인됐다. | □ |

## 테스트 명세

| 점검 | 완료 |
| --- | --- |
| Test 목적이 명확하다. | □ |
| 사전조건이 있다. | □ |
| 테스트 데이터가 명확하다. | □ |
| Step이 재현 가능하다. | □ |
| 기대 결과가 구체적이다. | □ |
| 정상·오류·경계를 포함한다. | □ |
| 환경과 Artifact를 기록한다. | □ |
| 자동화 여부가 있다. | □ |

## 실행 결과

| 점검 | 완료 |
| --- | --- |
| 실제 결과를 기록했다. | □ |
| 응답코드를 확인했다. | □ |
| DB 결과를 확인했다. | □ |
| History를 확인했다. | □ |
| GUID·거래로그를 확인했다. | □ |
| 감사·Metric을 확인했다. | □ |
| 필수 증적이 있다. | □ |
| 수행자·일시가 있다. | □ |
| Reviewer가 판정했다. | □ |

## 결함·재시험

| 점검 | 완료 |
| --- | --- |
| FAIL에 결함 ID가 있다. | □ |
| 결함이 요구사항과 연결된다. | □ |
| 수정 Commit을 기록했다. | □ |
| 원 Test를 재시험했다. | □ |
| 영향 회귀시험을 했다. | □ |
| 재시험 증적이 있다. | □ |
| QA 확인 후 종료했다. | □ |

## 배포·운영

| 점검 | 완료 |
| --- | --- |
| Critical Test가 모두 실행됐다. | □ |
| 보안 Test가 통과했다. | □ |
| 성능 목표를 충족했다. | □ |
| 장애·UNKNOWN을 시험했다. | □ |
| Smoke Test가 준비됐다. | □ |
| Rollback Drill이 완료됐다. | □ |
| Blocker·Critical Open이 없다. | □ |
| Go·No-Go 근거가 있다. | □ |

# E.39 변경·호환성·폐기 관리

## 요구사항 변경

요구사항이 변경되면 다음을 갱신한다.

요구사항 Version

설계

화면·ServiceId

프로그램·SQL

Test Case

기대 결과

회귀범위

배포 영향

기존 PASS 결과는 변경된 요구사항을 자동으로 증명하지 않는다.

## Test Case 변경

Test의 기대 결과나 Step이 변경되면 Revision을 관리한다.

TC-CT-RSV-047 Rev.1

→ Rev.2

이전 실행결과는 당시 Revision과 연결한다.

## ServiceId 폐기

구 ServiceId Test 유지

→ 신규 ServiceId Test 추가

→ 호출 전환

→ 구 Service 호출량 0

→ Deprecated Test 종료

→ 이력 보존

## DB Schema 변경

Mapper Test

Migration Test

구·신 호환 Test

데이터 Backfill Test

Rollback·Roll-forward Test

를 추가한다.

## 테스트 데이터 폐기

Test 완료

→ 보존 필요성 확인

→ 개인정보 파기

→ Smoke 전용 데이터 유지

→ 삭제 증적

## 결함 이관

차기 Release로 이관하려면 다음이 필요하다.

위험

영향 요구사항

임시 통제

Owner

완료기한

승인자

만료일

Critical 결함은 원칙적으로 이관하지 않는다.

# 시사점

## 핵심 아키텍처 판단

첫째, RTM은 요구사항 문서의 부록이 아니라 설계·구현·시험·배포를 연결하는 프로젝트 통제장치다.

둘째, 요구사항에서 Test로 이동하는 정방향 추적과 SQL·결함에서 요구사항으로 돌아가는 역방향 추적이 모두 가능해야 한다.

셋째, 하나의 요구사항에 여러 Test가 연결될 수 있으며 정상·오류·권한·동시성·Timeout Test를 분리해야 한다.

넷째, PASS는 실행환경·Artifact·기대 결과·실제 결과·증적이 모두 확인됐을 때만 사용할 수 있다.

다섯째, 화면 성공만으로 DB·History·거래로그·감사로그의 정합성을 증명할 수 없다.

여섯째, Mock 단위 Test와 실제 Transaction·동시성·성능 Test는 서로 다른 위험을 검증하므로 대체할 수 없다.

일곱째, 결함 수정 완료는 개발자의 FIXED 선언이 아니라 QA 재시험과 영향 회귀시험 완료로 판정해야 한다.

여덟째, 테스트 결과서는 배포 승인자료이자 향후 장애·감사·변경 영향 분석의 기준자료로 사용된다.

아홉째, Critical 요구사항의 미실행·BLOCKED 상태는 높은 전체 PASS율로 상쇄할 수 없다.

열째, RTM과 테스트 결과서는 수동 문서에 머물지 않고 CI/CD·결함관리·배포 Gate와 자동으로 연결돼야 한다.

## 주요 위험

| 위험 | 결과 |
| --- | --- |
| 요구사항 ID 없음 | 추적 불가 |
| Test 없는 요구사항 | 구현 누락 |
| 요구사항 없는 구현 | 불필요 기능 |
| Range만 기록 | 상세 검증 불명 |
| 증적 없는 PASS | 신뢰성 부족 |
| 환경·Version 누락 | 재현 불가 |
| 화면만 확인 | 데이터 오류 누락 |
| H2만 검증 | Oracle 호환 오류 |
| 단일 사용자 Test | 동시성 결함 누락 |
| Timeout 결과 미확인 | 중복 처리 |
| 결함 재시험 누락 | 미수정 배포 |
| Critical NOT\_RUN | 중대위험 미검증 |
| Test 데이터 오염 | 결과 왜곡 |
| 개인정보 증적 | 보안사고 |
| Flaky Test 방치 | CI 신뢰 상실 |
| RTM 미갱신 | 잘못된 영향분석 |

## 우선 보완 과제

1.  요구사항·설계·ServiceId·Test ID의 공식 명명체계를 확정한다.
2.  상담예약 요구사항 15개를 상세 RTM으로 등록한다.
3.  모든 신규 ServiceId에 Handler·Rule·TCF 거래 Test를 연결한다.
4.  Mapper 변경 시 SQL ID·DB 객체·실행계획 Test를 의무화한다.
5.  Version·Idempotency·Rollback Test를 Critical Gate로 지정한다.
6.  테스트 결과서에 Git Commit·Artifact Checksum을 필수화한다.
7.  PASS 결과에 화면·로그·DB 증적을 연결한다.
8.  결함 수정 후 Retest·Regression을 자동 요구하도록 구성한다.
9.  Critical NOT\_RUN·BLOCKED를 배포 차단조건으로 설정한다.
10.  운영 Smoke와 Rollback 결과를 Release RTM에 포함한다.
11.  테스트 데이터의 생성·Reset·파기 Script를 표준화한다.
12.  RTM 누락과 증적 없는 PASS를 CI·ALM에서 자동검증한다.
13.  Flaky Test를 별도 관리하고 해결기한을 부여한다.
14.  테스트 Summary와 Go·No-Go 판단을 자동 Dashboard로 제공한다.
15.  요구사항 변경 시 영향 Test를 자동 추천하는 구조로 발전시킨다.

# 마무리말

RTM과 테스트 결과서 작성을 완료하려면 다음 질문에 답할 수 있어야 한다.

모든 요구사항에 고유 ID가 있는가?

각 요구사항은 설계와 구현에 연결되는가?

화면 이벤트와 ServiceId가 연결되는가?

프로그램과 SQL·DB 객체를 찾을 수 있는가?

모든 요구사항에 하나 이상의 Test가 있는가?

모든 Test는 검증할 요구사항이 명확한가?

정상뿐 아니라 오류·권한·동시성·Timeout을 시험했는가?

어느 환경과 Artifact를 시험했는가?

테스트 데이터를 재현할 수 있는가?

기대 결과와 실제 결과가 분리돼 있는가?

화면 외에 DB·History·거래로그를 확인했는가?

PASS 결과에 증적이 있는가?

FAIL 결과에 결함 ID가 있는가?

결함 수정 후 재시험과 회귀시험을 했는가?

Critical 요구사항 중 미실행 항목이 없는가?

성능·보안·장애·Smoke·Rollback 결과가 있는가?

요구사항에서 Test까지 정방향으로 추적할 수 있는가?

결함과 SQL에서 요구사항으로 역추적할 수 있는가?

현재 결과로 운영 배포를 승인할 수 있는가?

부록 E의 핵심 흐름은 다음과 같다.

요구사항 Baseline

→ RTM 연결

→ Test 명세

→ 환경·데이터 준비

→ Test 실행

→ 응답·DB·로그 검증

→ 증적 등록

→ 결함·재시험

→ 결과 Summary

→ 품질 Gate

→ 배포·운영 승인

가장 중요한 원칙은 다음과 같다.

테스트 결과의 가치는
PASS라는 글자에 있지 않다.

어떤 요구사항을
어떤 환경과 Version에서

어떤 절차와 데이터로 검증했고,
실제 결과가 무엇이며,

그 사실을 어떤 증거로
다시 확인할 수 있는지가 중요하다.

요구사항부터 운영 배포까지
모든 판단을 하나의 추적 사슬로 연결할 때

RTM과 테스트 결과서는
형식적인 문서가 아니라
품질과 운영전환을 증명하는 실행 가능한 기준이 된다.
