<!-- source: ztcf-집필본/NSIGHT TCF Chapter 35-운영 전환 절차.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제36장. 리뷰·배포·운영 전환

## 도입 전 안내말

제33장에서는 상담예약 CRUD의 요구사항·화면·Aggregate·상태전이·ServiceId·DTO·테스트 계획을 설계했다.

제34장에서는 다음 거래를 구현했다.

\`\`\`text id=“rel36001” CT.Reservation.selectList

CT.Reservation.selectDetail

CT.Reservation.create



제35장에서는 다음 변경거래를 구현했다.

\`\`\`text id="rel36002"
CT.Reservation.update

CT.Reservation.cancel

그리고 다음 핵심 통제를 적용했다.

\`\`\`text id=“rel36003” Version 기반 동시성

상태별 변경권한

Idempotency

논리 취소

Master·History 원자성

변경 전후 이력

오류·보안 검증



소스코드가 완성되고 개발자 PC에서 정상 실행됐다고 상담예약 기능이 완료된 것은 아니다.

운영환경에서 서비스를 제공하려면 다음 질문에 답할 수 있어야 한다.

\`\`\`text id="rel36004"
요구사항과 구현이 정확히 일치하는가?

계층 책임과 Transaction 경계가 표준에 맞는가?

정상뿐 아니라 권한·동시성·Timeout·Rollback을 시험했는가?

DB DDL과 Index가 운영에 안전하게 반영되는가?

ServiceId가 Handler와 OM에 모두 등록됐는가?

운영 환경설정에 ct-service가 포함됐는가?

운영 WAR가 어느 Commit에서 만들어졌는가?

Artifact가 개발자 PC에서 임의로 만들어진 것은 아닌가?

배포 후 JVM이 살아 있는지만 확인하는가?

실제 상담예약 거래도 성공하는가?

배포 실패 시 어느 시점에 Rollback하는가?

DB Schema가 이전 WAR와 호환되는가?

Rollback 후 데이터 정합성을 확인할 수 있는가?

운영자는 어떤 Metric과 Alert를 확인하는가?

장애 발생 시 누구에게 연락하고 어떤 순서로 복구하는가?

개발 완료와 운영 준비 완료는 다르다.

\`\`\`text id=“rel36005” 개발 완료 = 소스 작성 + 개발 테스트

# 운영 전환 완료

개발 완료 + 리뷰 + 품질 Gate + 불변 Artifact + 배포 승인 + DB·설정·OM 반영 + Health·Smoke + 모니터링 + Rollback + 운영인수



운영 전환은 WAR 파일을 Tomcat에 복사하는 작업이 아니다.

\`\`\`text id="rel36006"
승인된 소스와 설정으로
재현 가능한 Artifact를 만들고,

서비스·데이터·보안·관측성·복구절차를
운영자가 책임질 수 있는 상태로 넘기는 과정

이다.

현재 프로젝트의 운영 전환 기준도 개발 완료를 단순 배포가 아니라 기능·성능·보안·관측성·DR·Runbook·OM 기준정보·교육이 완료된 상태로 정의한다.

# 문서 개요

## 목적

본 장의 목적은 상담예약 CRUD의 설계와 구현을 리뷰하고, 자동 품질검증·배포 승인·운영 배포·Rollback·안정화·운영인수를 수행하기 위한 최종 기준을 정의하는 것이다.

세부 목적은 다음과 같다.

\`\`\`text id=“rel36007” 요구사항과 소스의 정합성 검토

계층 책임과 패키지 의존 검토

Transaction·동시성·멱등성 검토

SQL·Index·DB Migration 검토

보안·개인정보·감사 검토

오류·Timeout·관측성 검토

자동 품질 Gate 정의

배포 차단조건 정의

Git Tag와 Artifact의 불변 연결

환경별 설정 Source of Truth 확립

상담예약 ct-service의 배포 구성 편입

DB Expand·Migrate·Contract 전략 적용

단계적 WAR 배포와 트래픽 제어

Liveness·Readiness·Deep·Smoke 검증

배포 실패·Timeout·부분 반영 흐름 정의

Rollback과 Roll-forward 판단

운영 Dashboard·Alert·Runbook 준비

안정화 기간과 운영인수 기준 정의

초보 개발자의 독립 수행 역량 검증


\## 적용범위

| 영역 | 적용 대상 |
|---|---|
| 상담예약 기능 | 목록·상세·등록·수정·취소 |
| 업무 WAR | 목표 \`ct-service\` |
| DB 객체 | Master·History·History Detail·Index |
| 기준정보 | ServiceId·거래코드·권한·Timeout·오류코드 |
| 구성관리 | \`local·dev·prod\` Profile |
| CI | Validate·Build·Test·Quality·Package |
| CD | Dev·Staging·Production 배포 |
| Artifact | WAR·설정 Bundle·DB Script |
| 인프라 | Apache·Tomcat·L4·DB |
| 운영검증 | Health·Smoke·Metric·Log |
| 복구 | WAR·설정·DB·기준정보 Rollback |
| 운영인수 | 교육·Runbook·연락망·안정화 |
| 변경관리 | Release·Hotfix·Deprecated·폐기 |

\## 대상 독자

\`\`\`text id="rel36008"
상담예약 개발자

업무 개발 리더

코드 리뷰어

애플리케이션 아키텍트

데이터 아키텍트·DBA

보안 담당자

QA·성능시험 담당자

DevOps·CI/CD 담당자

WAS·인프라 운영자

OM 운영관리 담당자

업무 담당자

PMO·품질 담당자

운영 전환 승인자

## 선행조건

\`\`\`text id=“rel36009” 요구사항 승인

상태·권한·중복정책 승인

코드 구현 완료

DB DDL·Index 초안

단위·통합 테스트

운영 환경정보

Git Branch·Tag 정책

CI Runner

Artifact Repository

배포·Rollback Script

OM 기준정보 등록절차

운영 Dashboard·Alert

비상연락망

운영 전환 승인체계


\---

\# 핵심 관점

\`\`\`text id="rel36010"
운영 전환의 핵심은
새 버전을 서버에 올리는 것이 아니다.

같은 소스와 설정으로
언제든 동일한 Artifact를 재현하고,

문제가 발생하면
영향 범위를 제한해 이전 상태로 복구하며,

운영자가 로그·메트릭·Runbook으로
서비스와 데이터의 정상 여부를
스스로 판단할 수 있게 하는 것이다.

# 학습 목표

이 장을 마치면 다음 내용을 설명하고 수행할 수 있어야 한다.

| 번호 | 학습 목표 |
| --- | --- |
| 1 | 개발 완료와 운영 전환 완료를 구분한다. |
| 2 | 코드리뷰의 우선순위를 설명한다. |
| 3 | 요구사항·설계·코드·SQL·테스트를 추적한다. |
| 4 | 계층 의존 위반을 식별한다. |
| 5 | Transaction·Version·Idempotency 구현을 검토한다. |
| 6 | DB Migration의 호환성을 검토한다. |
| 7 | 기능·보안·성능·운영 테스트 결과를 판정한다. |
| 8 | Blocker·Critical·Major를 구분한다. |
| 9 | 자동 품질 Gate를 구성한다. |
| 10 | 배포를 차단해야 하는 조건을 설명한다. |
| 11 | Git Tag와 WAR Artifact를 연결한다. |
| 12 | Artifact Checksum의 목적을 설명한다. |
| 13 | 환경설정과 Secret을 소스에서 분리한다. |
| 14 | ct-service를 빌드·배포 구성에 편입한다. |
| 15 | DB와 WAR의 배포순서를 결정한다. |
| 16 | Rolling·Blue-Green·일괄 배포를 비교한다. |
| 17 | Liveness·Readiness·Deep·Smoke를 구분한다. |
| 18 | 실제 상담예약 Smoke Test를 설계한다. |
| 19 | 배포 중 Traffic Drain을 수행한다. |
| 20 | 배포 실패 시 Rollback 기준을 판단한다. |
| 21 | DB Rollback과 Application Rollback을 구분한다. |
| 22 | Roll-forward가 더 안전한 상황을 설명한다. |
| 23 | 배포 후 안정화 지표를 관찰한다. |
| 24 | 운영 Runbook과 연락망을 검증한다. |
| 25 | 운영인수 완료 증적을 작성한다. |
| 26 | 초보 개발자의 최종 독립 수행능력을 평가한다. |

# 핵심 용어

| 용어 | 의미 |
| --- | --- |
| Code Review | 코드와 설계·운영 요구의 적합성을 검토하는 활동 |
| Pull Request·MR | 변경내용을 리뷰하고 병합하기 위한 요청 |
| Quality Gate | 다음 단계 진입을 허용하기 위한 필수 통과조건 |
| Blocker | 해결 전 배포가 불가능한 치명적 문제 |
| Critical | 보안·데이터·서비스에 중대한 위험이 있는 문제 |
| Major | 운영 전에 수정해야 하는 중요 문제 |
| Minor | 위험이 낮지만 개선이 필요한 문제 |
| CI | 코드 검증·빌드·테스트·패키징 자동화 |
| CD | 승인된 Artifact의 환경별 배포 자동화 |
| Artifact | 빌드 결과로 만들어진 불변 배포 산출물 |
| Artifact Repository | 승인된 Artifact를 버전별 보관하는 저장소 |
| Checksum | Artifact 변조 여부를 확인하는 Hash |
| Release | 운영에 전달할 코드·설정·DB 변경의 묶음 |
| Tag | 특정 Commit을 표시하는 변경 불가능한 버전표식 |
| Deployment | Artifact를 실행환경에 반영하는 행위 |
| Promotion | 같은 Artifact를 Dev에서 Staging·Production으로 승격 |
| Rolling Deployment | 인스턴스를 순차적으로 교체하는 배포 |
| Blue-Green | 구·신 환경을 별도로 준비하고 Traffic을 전환하는 방식 |
| Canary | 일부 Traffic에 먼저 신규 버전을 적용하는 방식 |
| Drain | 신규 Traffic을 막고 기존 요청 종료를 기다리는 조치 |
| Liveness | Process가 살아 있는지 확인 |
| Readiness | 신규 업무 요청을 받을 준비가 됐는지 확인 |
| Deep Check | DB·Session·Cache·연계 등 의존성 확인 |
| Smoke Test | 실제 대표 업무 거래가 성공하는지 확인 |
| Rollback | 이전 안정 버전으로 복구 |
| Roll-forward | 문제를 수정한 새 버전으로 전진 복구 |
| Expand-Migrate-Contract | 호환 가능한 DB 변경을 단계적으로 적용하는 전략 |
| Cut-over | 신규 시스템·버전으로 공식 전환하는 시점 |
| Go-Live | 운영 서비스 개시 |
| Go·No-Go | 전환 진행 또는 중단을 결정하는 승인 |
| Stabilization | 운영 전환 후 집중 관찰기간 |
| Operational Acceptance | 운영 조직이 서비스 책임을 공식 인수하는 절차 |
| Release Manifest | 배포 대상과 버전·Checksum·설정을 기록한 명세 |

# 상담예약 운영 전환 목표 구조

text id="rel36011" 개발자 ↓ feature Branch ↓ Merge Request ↓ 코드리뷰 ↓ CI Quality Gate ├─ Compile ├─ Unit ├─ Architecture ├─ Mapper·DB ├─ Security ├─ Integration └─ Package ↓ Immutable CT WAR + DB Migration + Config Bundle + OM Seed + Release Note ↓ Artifact Repository ↓ Dev 자동 배포 ↓ Staging 승인 배포 ↓ 기능·성능·보안·Rollback 시험 ↓ Go·No-Go ↓ Production Rolling Deploy ↓ Liveness → Readiness → Deep → Smoke ↓ Traffic 복귀 ↓ 안정화 모니터링 ↓ 운영인수

# 문제 정의 및 설계 배경

운영 장애는 코드 결함만으로 발생하지 않는다.

다음과 같은 **구성요소 간 불일치** 때문에 자주 발생한다.

\`\`\`text id=“rel36012” ServiceId는 소스에 있으나 OM에 없음

Handler는 있으나 Component Scan에서 누락

Mapper Interface는 있으나 XML Namespace가 다름

WAR는 배포됐으나 Gateway Route가 없음

DB Column은 추가됐으나 구 WAR가 읽지 못함

운영 Profile에 DataSource 설정이 없음

권한코드가 배포됐으나 기능권한이 등록되지 않음

Timeout 정책이 없어 기본값으로 실행

Health는 UP이지만 실제 업무 SQL이 실패

신규 WAR는 정상이나 직전 WAR를 찾을 수 없음

Rollback했으나 DB Schema는 신규 상태로 남음



따라서 운영 전환은 구성요소 하나의 정상 여부가 아니라 다음 연결의 정합성을 검증해야 한다.

\`\`\`text id="rel36013"
요구사항

↔ 화면 이벤트

↔ ServiceId

↔ Handler

↔ Facade·Service·Rule

↔ DAO·Mapper·SQL

↔ DB Schema

↔ OM Catalog·권한·Timeout

↔ 배포 Artifact

↔ 로그·Metric·Runbook

# 현행 구조와 문제점

## 현재 tcf-cicd 기반

현재 프로젝트에는 다음 CI/CD 기반이 존재한다.

\`\`\`text id=“rel36014” local·dev·prod Profile

프로파일별 Spring YML

Tomcat setenv

운영 Apache 설정

manifest.yaml

환경설정 동기화 Script

WAR Build·Deploy Script

선택 Module 배포

배포 후 Health Check 옵션



\`tcf-cicd\`는 프로파일별 설정을 관리하는 Source of Truth 역할을 수행하도록 구성돼 있다.

일반 흐름:

\`\`\`text id="rel36015"
tcf-cicd 설정

→ Framework Module로 Sync

→ Gradle Build

→ WAR Deploy

→ Restart·Health Check

## 현재 상담예약 배포 Gap

상담예약은 목표 업무코드 CT를 사용하지만 현재 분석 기준에서는 다음이 운영 배포 구성에 명시적으로 편입된 것으로 확인되지 않는다.

\`\`\`text id=“rel36016” ct-service 실행 Module

ct.war Build 대상

tcf-cicd manifest의 CT Module

local·dev·prod application Profile

배포 Script Module 목록

Apache Context Route

Gateway Route

OM Service Catalog Seed

CT 로그 경로

CT Health 수집대상



따라서 상담예약 Java 소스만 작성해서는 배포할 수 없다.

\`\`\`text id="rel36017"
상담예약 기능 완료

≠ CT 운영 WAR 완료

운영 전환 전 다음 편입작업이 필요하다.

\`\`\`text id=“rel36018” Gradle Module

→ WAR Artifact

→ Profile 설정

→ 배포 Module

→ Context Path

→ Route

→ OM Catalog

→ Health·Metric

→ 운영 Runbook


\## 운영 전환 주요 문제

| 문제 | 운영 결과 |
|---|---|
| CT Module 미등록 | CI가 WAR를 만들지 않음 |
| Profile 누락 | 운영 DB 연결 실패 |
| Context 중복 | 잘못된 WAR가 기동 |
| Route 누락 | 화면 404·502 |
| ServiceId Seed 누락 | 거래통제에서 차단 |
| 권한 Seed 누락 | 403 |
| DB Migration 누락 | SQL 오류 |
| Rollback Artifact 없음 | 장애 장기화 |
| Health만 확인 | 실제 거래장애 미발견 |
| 모든 WAS 동시 배포 | 전체 서비스 중단 |
| Secret WAR 포함 | 보안사고 |
| 변경이력 미기록 | 감사·원인분석 불가 |

\---

\# 요구사항과 제약조건

\## 기능 전환 요구

\`\`\`text id="rel36019"
목록·상세·등록·수정·취소 거래 정상

상태·Version·권한 통제 정상

Master·History 정합성

Idempotency 반복요청 통제

개인정보 마스킹·감사

오류코드·Timeout 정상

GUID·TraceId 추적

## 배포 요구

\`\`\`text id=“rel36020” CI Runner가 Artifact 생성

동일 Artifact 환경 Promotion

Tag·Commit·Checksum 연결

운영 Secret 외부화

Rolling 배포

배포 전 직전본 확보

배포 후 4단계 검증

실패 시 Rollback 가능


\## 운영 제약

\`\`\`text id="rel36021"
Apache·Tomcat·L4 구조

다중 WAR 공유 JVM 가능성

Oracle Schema 변경

운영 변경시간

보안 승인

DBA 승인

업무 정상확인

Rollback 목표시간

운영 접근권한 분리

# 설계 원칙

## 원칙 1. 리뷰는 코딩 스타일보다 위험을 우선한다

검토 순서:

\`\`\`text id=“rel36022” 데이터 손상

→ 보안·권한

→ Transaction·동시성

→ 오류·Timeout

→ 성능·용량

→ 운영·배포

→ 가독성·스타일


\## 원칙 2. Build once, deploy many

\`\`\`text id="rel36023"
Dev용 WAR

Staging용 WAR

운영용 WAR

를 각각 다시 빌드하지 않는다.

같은 WAR와 환경별 외부설정을 사용한다.

## 원칙 3. 운영 서버에서 빌드하지 않는다

운영 Artifact는 CI Runner가 생성하고 Repository에 보관한다.

## 원칙 4. 배포는 불변 Artifact로 수행한다

같은 Version의 WAR 내용을 다시 바꾸지 않는다.

## 원칙 5. Health와 업무 성공을 구분한다

\`\`\`text id=“rel36024” Process UP

≠ DB 정상

≠ ServiceId 등록

≠ 상담예약 거래 성공


\## 원칙 6. DB 변경은 하위호환을 우선한다

구 WAR와 신 WAR가 일시적으로 공존할 수 있어야 한다.

\## 원칙 7. Rollback은 배포 전에 시험한다

운영 장애 중 처음 Rollback Script를 실행해서는 안 된다.

\## 원칙 8. 한 번에 전체 인스턴스를 교체하지 않는다

순차배포와 Traffic Drain을 적용한다.

\## 원칙 9. 전환 완료는 운영자가 판정한다

개발자가 “정상입니다”라고 말하는 것만으로 완료되지 않는다.

\## 원칙 10. 잔여위험은 만료일이 있는 승인으로 관리한다

무기한 예외는 허용하지 않는다.

\---

\# 대안 비교 및 의사결정

\## 배포방식

| 방식 | 장점 | 위험 | 상담예약 판단 |
|---|---|---|---|
| 전체 일괄 | 단순 | 전체 중단 | 금지 |
| Rolling | 기존 인프라 활용 | 구·신 호환 필요 | 기본 |
| Blue-Green | 빠른 전환·복귀 | 환경 2배 | 중요 Release 검토 |
| Canary | 일부 사용자 검증 | Route·분석 복잡 | 중장기 |
| Hot Deploy | 빠름 | ClassLoader·상태 위험 | 운영 금지 |
| Class 교체 | 매우 빠름 | 무결성·Rollback 불가 | 금지 |

\## DB 변경방식

| 방식 | 특징 | 판단 |
|---|---|---|
| App와 동시에 Breaking DDL | 단순하지만 위험 | 금지 |
| Expand-Migrate-Contract | 단계적 호환 | 기본 |
| Forward-only Migration | 운영 단순화 | Roll-forward 준비 |
| DDL 직접수정 | 이력 부족 | 긴급 승인 외 금지 |

\## 복구방식

| 상황 | 우선 |
|---|---|
| 신 WAR 코드 결함 | WAR Rollback |
| 신규 설정 결함 | 설정 Rollback |
| Route·OM Seed 오류 | 기준정보 원복·수정 |
| Additive DB 변경 | 구 WAR Rollback 가능 |
| 파괴적 DB 변경 | Roll-forward 가능성 높음 |
| 데이터가 신규 형식으로 이미 변경 | 보상·Forward Fix 검토 |
| 단일 Instance 결함 | Traffic 제외 후 분석 |

\---

\# 목표 Release 구성요소

상담예약 Release는 WAR 하나가 아니라 다음 묶음이다.

\`\`\`text id="rel36025"
ct.war

DB Migration Script

DB 검증 SQL

DB Rollback·보상 SQL

application-prod.yml 변경

Apache·Gateway Route 변경

OM Service Catalog Seed

기능권한 Seed

오류코드 Seed

Timeout 정책 Seed

Release Note

Smoke Test Script

Rollback Runbook

Checksum

테스트 결과서

# Release Manifest 표준

\`\`\`yaml id=“rel36026” release: name: ct-reservation version: 1.0.0 tag: v1.0.0 commit: abcdef123456 buildNumber: 20260718.15

artifacts: - name: ct.war sha256: “” contextPath: /ct targetTomcatGroup: business-b

database: migrations: - V20260718\_01\_\_create\_ct\_reservation.sql - V20260718\_02\_\_create\_ct\_reservation\_indexes.sql - V20260718\_03\_\_seed\_ct\_reservation\_codes.sql validation: - Q20260718\_01\_\_validate\_ct\_reservation.sql rollback: - R20260718\_01\_\_rollback\_ct\_reservation.sql

operations: serviceIds: - CT.Reservation.selectList - CT.Reservation.selectDetail - CT.Reservation.create - CT.Reservation.update - CT.Reservation.cancel health: liveness: /ct/actuator/health/liveness readiness: /ct/actuator/health/readiness deep: /ct/actuator/health smoke: script: smoke/ct-reservation-smoke.sh

approvals: architecture: required database: required security: required operations: required business: required


\---

\# 배포 상태

\`\`\`text id="rel36027"
REQUESTED

REVIEWING

APPROVED

BUILDING

BUILT

PUBLISHED

DEPLOYING\_DEV

VERIFIED\_DEV

DEPLOYING\_STG

VERIFIED\_STG

READY\_FOR\_PROD

DEPLOYING\_PROD

MONITORING

COMPLETED

FAILED

ROLLBACK\_REQUESTED

ROLLING\_BACK

ROLLED\_BACK

CANCELLED

# 36.1 코드 리뷰 체크리스트

## 36.1.1 리뷰 목적

코드리뷰는 다음을 확인하는 활동이다.

\`\`\`text id=“rel36028” 요구사항을 잘못 이해하지 않았는가?

업무규칙이 올바른 계층에 있는가?

데이터가 중복·유실될 가능성이 없는가?

권한을 우회할 수 없는가?

오류와 Timeout 후 복구할 수 있는가?

운영에서 추적하고 통제할 수 있는가?

변경이 다른 업무에 미치는 영향은 무엇인가?



리뷰를 변수명·들여쓰기·주석에만 집중하지 않는다.

\---

\## 36.1.2 리뷰 우선순위

\`\`\`text id="rel36029"
1\. 요구사항과 데이터 정합성
2\. 보안·개인정보
3\. Transaction·동시성·멱등성
4\. 계층 책임·의존성
5\. 오류·Timeout·복구
6\. SQL·성능
7\. 운영·로그·Metric
8\. 테스트
9\. 배포·호환성
10\. 가독성·명명

## 36.1.3 리뷰 입력자료

\`\`\`text id=“rel36030” MR 설명

요구사항 ID

화면 이벤트

ServiceId 목록

변경 파일

DB Migration

설정 변경

OM Seed

테스트 결과

성능 결과

보안 결과

배포·Rollback 계획



MR에는 코드 Diff만 올리지 않는다.

\---

\## 36.1.4 MR 표준 설명

\`\`\`text id="rel36031"
변경 목적

대상 요구사항

영향 화면·ServiceId

변경 Table·Index

외부 연계

Transaction 영향

보안·개인정보

설정·배포 영향

테스트 증적

Rollback 방법

미결·잔여위험

## 36.1.5 요구사항·추적성 리뷰

| 점검 | 확인 |
| --- | --- |
| REQ-CT-RSV-\*가 구현과 연결되는가 | □ |
| 화면 이벤트 5개가 ServiceId와 일치하는가 | □ |
| ServiceId가 Handler에 등록됐는가 | □ |
| Mapper SQL ID가 설계서와 일치하는가 | □ |
| DB 객체가 프로그램 설계서와 일치하는가 | □ |
| 요구사항별 Test ID가 존재하는가 | □ |
| 삭제·변경된 요구사항이 코드에 남아 있지 않은가 | □ |

## 36.1.6 계층 구조 리뷰

| 계층 | 확인사항 |
| --- | --- |
| Controller | 업무 전용 Controller 없음 |
| Handler | Facade만 호출 |
| Facade | 유스케이스·Transaction |
| Service | 업무흐름 조립 |
| Rule | 상태·권한·입력 판단 |
| DAO | Mapper 호출·영향 행 전달 |
| Mapper | SQL 실행 |
| Client | 외부 고객계약 변환 |
| DTO | Request·Query·Command·Row·Response 분리 |

패키지 리뷰에서는 Handler가 Mapper를 직접 호출하지 않는지, Rule에 DB·외부 호출이 없는지, Transaction이 Facade에 있는지 등을 자동·수동으로 검증해야 한다.

## 36.1.7 상담예약 구조 리뷰

\`\`\`text id=“rel36032” CtReservationHandler

→ CtReservationFacade

→ CtReservationService

→ CtReservationRule

→ CtReservationDao

→ CtReservationMapper



금지:

\`\`\`text id="rel36033"
Handler → Mapper

Rule → DAO

Mapper → Service

CT → IC 구현 Class 직접 참조

Service → HTTP Controller

## 36.1.8 Transaction 리뷰

### 등록

\`\`\`text id=“rel36034” Master INSERT

-   History INSERT

→ 같은 Transaction


\### 수정

\`\`\`text id="rel36035"
Version 조건 UPDATE

\+ History Header

\+ History Detail

→ 같은 Transaction

### 취소

\`\`\`text id=“rel36036” 상태 CANCELED

-   Active Key 해제
-   History

→ 같은 Transaction



검토:

\`\`\`text id="rel36037"
Facade에 Transaction이 있는가?

History가 REQUIRES\_NEW가 아닌가?

예외를 Catch 후 숨기지 않는가?

BusinessException Rollback 정책이 맞는가?

외부 고객호출을 장시간 Transaction 안에서 기다리지 않는가?

## 36.1.9 동시성 리뷰

\`\`\`text id=“rel36038” 상세 Response에 Version

수정·취소 Request에 Version

사전 Version 검증

UPDATE SQL Version 조건

영향 행 0건 오류

성공 시 Version+1

동시 수정 Test



Version 검증이 Java에만 있고 SQL에 없으면 Blocker다.

\---

\## 36.1.10 멱등성 리뷰

\`\`\`text id="rel36039"
등록·수정·취소 대상

Idempotency Key Scope

Request Fingerprint

동일 Key 기존결과

PROCESSING 처리

UNKNOWN 대사

Key 보존기간

민감정보 미저장

## 36.1.11 상태전이 리뷰

\`\`\`text id=“rel36040” 없음 → READY

READY → READY 수정

READY → COMPLETED

READY → CANCELED



금지 상태전이가 코드·SQL·화면에서 모두 차단되는지 확인한다.

\---

\## 36.1.12 권한 리뷰

\`\`\`text id="rel36041"
STF 기능권한

\+ 업무 데이터권한

\+ SQL Scope

\+ 화면 버튼

확인:

\`\`\`text id=“rel36042” Request의 userId·branchId를 신뢰하지 않는가?

타 지점 데이터 존재 여부를 노출하지 않는가?

상세조회 개인정보 감사를 남기는가?

수정·취소 수행자를 인증문맥에서 가져오는가?


\---

\## 36.1.13 DTO 리뷰

| 항목 | 기준 |
|---|---|
| Request | 화면 입력만 포함 |
| Command | 서버 결정값 포함 |
| Query | 검증된 조회조건 |
| Row | DB 결과 |
| Response | 공개할 정보만 |
| 사용자·지점 | Request에서 제외 |
| 상태 | 변경 Request에서 제외 |
| 민감정보 | 최소화·마스킹 |

다음은 Major 또는 Critical이다.

\`\`\`text id="rel36043"
BeanUtils로 Request 전체를 Entity에 복사

Map을 Mapper에 직접 전달

DB Row를 Response로 직접 반환

상담메모를 목록에 반환

## 36.1.14 SQL 리뷰

### 목록

\`\`\`text id=“rel36044” 조회기간 제한

Page Size 제한

안정적 정렬

Tie-breaker

지점 Scope

SELECT \* 금지

N+1 없음

Query Timeout


\### 등록

\`\`\`text id="rel36045"
서버 생성 ID

업무 중복 Unique

영향 행 1건

History 1건

### 수정·취소

\`\`\`text id=“rel36046” PK

상태

Version

권한 Scope

영향 행

History


\---

\## 36.1.15 DB 리뷰

| 항목 | 확인 |
|---|---|
| PK·Unique | 중복·식별 보장 |
| Check Constraint | 상태·Version |
| Index | 조회패턴과 일치 |
| Column 길이 | DTO·화면과 일치 |
| Null 정책 | 업무규칙과 일치 |
| Timestamp | Timezone 기준 |
| History | 보존·Partition |
| Migration | 하위호환 |
| Rollback | 실행 가능성 |
| 권한 | 최소 DB 권한 |

\---

\## 36.1.16 오류 리뷰

\`\`\`text id="rel36047"
Validation

Not Found

권한

상태

동시성

중복

외부 연계

DB

Timeout

UNKNOWN

확인:

\`\`\`text id=“rel36048” 사용자 메시지는 안전한가?

원인 예외는 보존되는가?

Stack Trace가 응답되지 않는가?

오류코드가 OM에 등록되는가?

사용자 다음 행동이 명확한가?


\---

\## 36.1.17 로그·관측성 리뷰

필수:

\`\`\`text id="rel36049"
guid

traceId

serviceId

transactionCode

resultType

errorCode

elapsedMs

instanceId

artifactVersion

금지:

\`\`\`text id=“rel36050” 상담메모

고객번호 원문

JWT

Authorization

Idempotency Key 원문

DB Password


\---

\## 36.1.18 성능 리뷰

\`\`\`text id="rel36051"
목록 최대 3개월

Page Size 100

Index 실행계획

SQL Timeout

고객 N+1 없음

History 증가량

DB Pool 영향

응답 크기

## 36.1.19 테스트 리뷰

테스트가 Method 호출 여부만 검증해서는 안 된다.

필수:

\`\`\`text id=“rel36052” 정상

빈 결과

경계값

권한

상태

중복

동시성

Idempotency

Rollback

Timeout

개인정보

성능

운영 Smoke


\---

\## 36.1.20 배포 영향 리뷰

\`\`\`text id="rel36053"
신규 ct-service Module

Context /ct

ct.war

application-\*.yml

Apache·Gateway Route

OM Service Catalog

기능권한

Timeout

오류코드

DB Migration

로그·Metric

Rollback

## 36.1.21 좋은 리뷰 의견

\`\`\`text id=“rel36054” “updateReservation SQL이 RESERVATION\_ID만 조건으로 사용합니다.

제35장 동시성 기준에 따라 STATUS\_CD=’READY’와 VERSION\_NO 조건을 추가하고, 영향 행 0건 동시성 테스트를 보완해야 합니다.

현재 상태로 병합하면 Lost Update 위험이 있으므로 Blocker로 판정합니다.”



좋은 리뷰는 다음을 포함한다.

\`\`\`text id="rel36055"
문제 위치

위험

적용 기준

수정방향

심각도

## 36.1.22 좋지 않은 리뷰 의견

\`\`\`text id=“rel36056” “이 코드 별로입니다.”

“다시 만들어 주세요.”

“왜 이렇게 했나요?”

“저라면 다르게 구현합니다.”



근거와 위험이 없는 취향 중심 리뷰는 피한다.

\---

\## 36.1.23 리뷰 심각도

| 등급 | 예 |
|---|---|
| Blocker | Version 조건 없음, Secret 포함 |
| Critical | 권한우회, 부분 Commit |
| Major | OM 미등록, Rollback Test 없음 |
| Minor | 명명·중복코드 |
| Suggestion | 향후 구조 개선 |

\---

\## 36.1.24 코드리뷰 완료조건

\`\`\`text id="rel36057"
모든 Blocker 해결

Critical 0건

Major 해결 또는 만료일 있는 승인

필수 Reviewer 승인

테스트 재실행

설계서·RTM 갱신

DBA·보안 검토 완료

배포 영향 확인

# 36.2 품질 Gate와 배포 승인

## 36.2.1 표준 Pipeline

\`\`\`text id=“rel36058” Validate

→ Compile

→ Unit Test

→ Architecture Test

→ Mapper·DB Test

→ Integration Test

→ Security Scan

→ Static Quality

→ Package WAR

→ Checksum·SBOM

→ Artifact Publish

→ Dev Deploy

→ Health·Smoke

→ Staging Deploy

→ 성능·장애·Rollback

→ 운영 승인

→ Production Deploy

→ 안정화


\---

\## 36.2.2 Validate Gate

검사:

\`\`\`text id="rel36059"
허용 Branch

승인된 JDK·Gradle

금지 파일

Secret Pattern

License·Dependency 정책

버전 형식

ServiceId 중복

DB Script 명명

## 36.2.3 Build Gate

\`\`\`text id=“rel36060” clean compile

processResources

Mapper XML 포함

bootWar

테스트 Class 제외

WAR 파일명 ct.war

Manifest Version

Build 재현성


\---

\## 36.2.4 Architecture Gate

자동검증:

\`\`\`text id="rel36061"
업무 Controller 생성 금지

Handler → Facade만 허용

Rule → DAO 금지

DAO 업무 판단 금지

업무 WAR 간 구현 직접 참조 금지

Facade Transaction

DTO 전 계층 재사용 금지

순환 의존 금지

ArchUnit·정적검사로 자동화한다.

## 36.2.5 ServiceId Gate

\`\`\`text id=“rel36062” ServiceId 중복 없음

Handler 등록

업무코드 CT 일치

거래코드 존재

OM Catalog Seed

거래통제 Allow List

Timeout 정책

권한코드

오류코드



ServiceId가 설계서·소스·OM·로그에서 동일해야 한다.

\---

\## 36.2.6 Test Gate

| Test | 필수 |
|---|:---:|
| Rule Unit | O |
| Service Unit | O |
| Mapper Integration | O |
| Transaction Rollback | O |
| Version Concurrency | O |
| Idempotency | O |
| Security | O |
| TCF Transaction | O |
| Performance Baseline | O |
| Smoke | O |

\---

\## 36.2.7 DB Gate

\`\`\`text id="rel36063"
DDL Syntax

Object 중복

Column·DTO 길이

PK·Unique·Check

Index Plan

권한 Script

Seed Script

검증 SQL

Rollback·보상 SQL

운영 데이터 영향

Lock 예상시간

## 36.2.8 보안 Gate

\`\`\`text id=“rel36064” Critical 취약점 0

High 위험 승인

Secret 0

JWT 검증

기능권한

데이터권한

SQL Injection

XSS

개인정보 마스킹

감사로그

관리 Endpoint 접근제한


\---

\## 36.2.9 성능 Gate

\`\`\`text id="rel36065"
목표 TPS

목록 p95

상세 p95

변경 p95

오류율

Timeout율

Hikari Pending

SQL Plan

History Write 부하

동시성 충돌률

## 36.2.10 Artifact Gate

필수 Artifact:

\`\`\`text id=“rel36066” ct.war

SHA-256

Build Metadata

Commit ID

Tag

Dependency 목록

DB Script

Config Diff

OM Seed Diff

Release Note



Artifact Repository에 업로드된 뒤 내용을 수정하지 않는다.

\---

\## 36.2.11 배포 차단조건

다음 중 하나라도 있으면 운영 배포를 차단한다.

\`\`\`text id="rel36067"
ServiceId 중복

Handler 미등록

CT Module 빌드대상 누락

OM Catalog 누락

Critical 보안취약점

Secret 노출

필수 테스트 실패

DB Migration 실패

DB 검증 SQL 실패

Rollback Artifact 없음

Health Check 실패

Smoke Test 실패

Master·History 정합성 실패

Version 동시성 Test 실패

운영 Runbook 없음

프로젝트 아키텍처 점검기준도 ServiceId 중복, Handler·OM 미등록, Critical 취약점, Migration·Smoke 실패와 Rollback Artifact 부재를 배포 차단사유로 정의한다.

## 36.2.12 상담예약 Module 편입

### Gradle

\`\`\`text id=“rel36068” settings.gradle

include ‘:ct-service’



의존성:

\`\`\`text id="rel36069"
ct-service

→ tcf-web

→ tcf-core

→ tcf-eai

→ tcf-util

→ tcf-cache 필요 시

### WAR

\`\`\`text id=“rel36070” bootWar archiveFileName ct.war

Context /ct


\### Profile

\`\`\`text id="rel36071"
tcf-cicd/local/spring/ct-service/application-local.yml

tcf-cicd/dev/spring/ct-service/application-dev.yml

tcf-cicd/prod/spring/ct-service/application-prod.yml

### Manifest

yaml id="rel36072" modules: business: - ct-service

### 배포 Script

\`\`\`text id=“rel36073” Module ct-service

Src ct.war

Dest ct.war

Ctx ct


\### Route

\`\`\`text id="rel36074"
Apache
/ct

Gateway Target
CT → /ct

## 36.2.13 환경설정 Source of Truth

공통 설정:

text id="rel36075" ct-service/src/main/resources/application.yml

환경별 설정:

text id="rel36076" tcf-cicd/{profile}/spring/ct-service/ application-{profile}.yml

Secret:

\`\`\`text id=“rel36077” 환경변수

Vault·Secret Manager

운영 권한파일



Git·WAR에 실제 비밀번호와 Private Key를 넣지 않는다.

\---

\## 36.2.14 DB Expand-Migrate-Contract

\### Expand

기존 WAR와 호환되는 객체를 먼저 추가한다.

\`\`\`text id="rel36078"
신규 Table

신규 Nullable Column

신규 Index

신규 Seed

### Migrate

\`\`\`text id=“rel36079” 신규 WAR 배포

데이터 생성

필요한 Backfill

구·신 버전 공존 검증


\### Contract

\`\`\`text id="rel36080"
구 코드 호출 0

구 Column·Index 폐기

Deprecated 제거

상담예약 신규 Table은 기존 기능 영향이 적지만 공통코드·Route·권한 변경은 구 버전 호환을 확인한다.

## 36.2.15 DB Migration 순서

text id="rel36081" 1. Backup·복구점 확인 2. DDL Dry-run 3. Table·Sequence 생성 4. Constraint 생성 5. Index 생성 6. 권한 부여 7. Seed 반영 8. 검증 SQL 9. App 배포 10. Smoke 후 데이터 확인

## 36.2.16 DB Script 명명

\`\`\`text id=“rel36082” V20260718\_01\_\_create\_ct\_reservation.sql

V20260718\_02\_\_create\_ct\_reservation\_history.sql

V20260718\_03\_\_create\_ct\_reservation\_indexes.sql

V20260718\_04\_\_seed\_ct\_reservation\_om.sql

Q20260718\_01\_\_validate\_ct\_reservation.sql

R20260718\_01\_\_rollback\_ct\_reservation.sql


\---

\## 36.2.17 배포 승인체계

| 승인 | 확인사항 |
|---|---|
| 개발 | 소스·테스트·Artifact |
| 아키텍처 | 구조·Transaction·호환성 |
| DBA | DDL·Index·Migration·Rollback |
| 보안 | 권한·Secret·개인정보 |
| QA | 기능·회귀·성능·장애 |
| 운영 | 배포·Health·Runbook |
| 업무 | 대표 업무 정상 |
| PMO | 전환 절차·잔여위험 |

\---

\## 36.2.18 Go·No-Go 판정

\### Go

\`\`\`text id="rel36083"
Blocker 0

Critical 0

필수 Gate PASS

Rollback Drill 성공

Smoke Script 준비

운영 인력·연락망 준비

잔여위험 승인

### Conditional Go

\`\`\`text id=“rel36084” 서비스·보안·데이터에 영향 없는 Major

Owner

완료일

임시 통제

승인자

만료일


\### No-Go

\`\`\`text id="rel36085"
데이터 손상 가능성

권한우회

Rollback 불가

DB 호환 불가

대표 거래 실패

모니터링·Runbook 부재

## 36.2.19 운영 배포 절차

text id="rel36086" 1. 전환 시작 공지 2. Release Manifest 확인 3. Artifact Checksum 확인 4. DB Backup·복구점 확인 5. 호환 DDL·Seed 반영 6. DB 검증 SQL 수행 7. 대상 WAS Traffic 제외 8. 기존 요청 Drain 9. 기존 WAR·설정 백업 10. ct.war 배포 11. Tomcat Context 기동 12. Liveness 13. Readiness 14. Deep Check 15. 상담예약 Smoke 16. 로그·Metric 확인 17. Traffic 일부 복귀 18. 오류·p95 확인 19. Traffic 전체 복귀 20. 다음 WAS 반복 21. 전체 기능·데이터 대사 22. 안정화 모니터링 23. 전환 완료 승인

## 36.2.20 다중 WAR Tomcat 주의

하나의 Tomcat에 여러 업무 WAR가 배포된 경우 다음은 공유될 수 있다.

\`\`\`text id=“rel36087” JVM Process

Heap·GC

Connector Thread

Tomcat Restart 영향

공통 Library Version



CT WAR만 교체해도 Tomcat Restart가 필요하면 다른 업무에 영향이 발생한다.

검토:

\`\`\`text id="rel36088"
Context Reload 가능 여부

ClassLoader Leak

공통 JAR 변경 여부

전체 재기동 필요 여부

Drain 대상 업무

잔여 Instance 용량

다중 WAR 배포에서는 Context별 Health, 대표 ServiceId Smoke, WAR별 Rollback 본과 공통 Library 변경 시 전체 영향 검증이 필요하다.

## 36.2.21 공통 모듈 변경

tcf-core, tcf-web, tcf-util 변경이 포함되면 CT만 시험해서는 안 된다.

\`\`\`text id=“rel36089” 영향 업무 WAR 전체 Build

대표 업무 회귀

JWT·Timeout·로그

Mapper·Transaction

Tomcat 기동

공통 Version 정합성


\---

\## 36.2.22 Rolling 배포

\`\`\`text id="rel36090"
AP01 Traffic 제외

→ CT 배포

→ Health·Smoke

→ Traffic 복귀

→ AP02 반복

한 번에 한 인스턴스 또는 승인된 Batch Size만 교체한다.

## 36.2.23 Canary

가능한 경우:

\`\`\`text id=“rel36091” 내부 테스트 사용자

특정 지점

일부 Traffic



에 먼저 신규 버전을 적용한다.

Canary 판정:

\`\`\`text id="rel36092"
오류율

p95

동시성 충돌

DB Pool

History 실패

사용자 피드백

## 36.2.24 Rollback 기준

즉시 Rollback 후보:

\`\`\`text id=“rel36093” Context 기동 실패

Readiness 실패

대표 Smoke 실패

Critical 오류 증가

권한우회

데이터 부분 반영

History 저장 실패

p95 급증

DB Pool 고갈

신규 Version만 오류


\---

\## 36.2.25 Rollback 실행

\`\`\`text id="rel36094"
1\. 신규 Traffic 차단
2\. 대표 GUID·Metric 증거 확보
3\. Rollback 승인
4\. 신규 WAR 제거
5\. 직전 WAR 복원
6\. 직전 설정 복원
7\. Tomcat 기동·Reload
8\. Liveness
9\. Readiness
10\. Deep
11\. Smoke
12\. 데이터 정합성 확인
13\. Traffic 복귀
14\. OM 배포이력 기록
15\. 장애·원인분석

## 36.2.26 DB Rollback 주의

다음은 단순 DROP TABLE로 복구할 수 없다.

\`\`\`text id=“rel36095” 신규 상담예약 데이터가 생성됨

History가 저장됨

다른 기능이 신규 Column을 사용

운영 사용자가 거래 수행



대안:

\`\`\`text id="rel36096"
신규 WAR만 Rollback

신규 Table 유지

기능 거래통제

데이터 보존

수정 Version Roll-forward

DB Rollback은 데이터 유실 위험을 별도로 승인해야 한다.

## 36.2.27 Roll-forward

Roll-forward가 적합한 경우:

\`\`\`text id=“rel36097” DB Migration이 비가역적

데이터가 신규 형식으로 이미 저장됨

결함 범위가 작고 수정이 명확

이전 WAR가 신규 Schema와 호환되지 않음



긴급 수정도 다음을 지킨다.

\`\`\`text id="rel36098"
Hotfix Branch

Review

필수 Test

새 Tag

새 Artifact

Health·Smoke

사후 Back-merge

# 36.3 모니터링과 장애 시나리오

## 36.3.1 4단계 검증

### Liveness

text id="rel36099" JVM·Spring Context 생존

예:

text id="rel36100" GET /ct/actuator/health/liveness

### Readiness

\`\`\`text id=“rel36101” 신규 요청 수신 가능

Handler·필수 초기화 완료


\### Deep Check

\`\`\`text id="rel36102"
업무 DB

OM DB

고객 연계

Cache

필수 설정

### Smoke Check

\`\`\`text id=“rel36103” 실제 ServiceId

실제 TCF 흐름

실제 SQL

실제 표준응답



운영 전환 기준은 단일 \`/actuator/health\` 확인이 아니라 Liveness·Readiness·Deep·Smoke의 단계적 검증을 요구한다.

\---

\## 36.3.2 상담예약 Smoke 범위

\### 읽기 Smoke

\`\`\`text id="rel36104"
CT.Reservation.selectList

CT.Reservation.selectDetail

### 변경 Smoke

\`\`\`text id=“rel36105” CT.Reservation.create

CT.Reservation.update

CT.Reservation.cancel



변경 Smoke는 전용 고객·예약 데이터를 사용한다.

\---

\## 36.3.3 Smoke 데이터

\`\`\`text id="rel36106"
전용 테스트 고객

전용 지점

전용 사용자

전용 상담목적

고유 Idempotency Key

테스트 식별 Prefix

예:

\`\`\`text id=“rel36107” customerNo SMOKE\_CT\_001

reservationId SMOKE-RSV-{timestamp}



운영 실제 고객 데이터를 무단 사용하지 않는다.

\---

\## 36.3.4 Smoke Test 흐름

\`\`\`text id="rel36108"
목록조회

→ 예약 등록

→ 상세조회

→ Version 확인

→ 예약 수정

→ Version 증가 확인

→ 예약 취소

→ CANCELED 확인

→ History 건수 확인

→ 로그·감사 확인

→ 테스트 데이터 정리·보존정책 적용

## 36.3.5 Smoke 판정

| 검증 | 성공기준 |
| --- | --- |
| HTTP | 표준 Status |
| 결과코드 | SUCCESS |
| Body | 예약 ID·상태·Version |
| Master | 정확히 1건 |
| History | 기대 건수 |
| Version | 1→2→3 |
| 권한 | 테스트 사용자 기준 |
| 거래로그 | GUID 전체 추적 |
| Metric | Count·Duration 증가 |
| 오류로그 | 신규 오류 없음 |

## 36.3.6 배포 후 주요 Metric

\`\`\`text id=“rel36109” ct.reservation.transaction.count

ct.reservation.transaction.duration

ct.reservation.error.count

ct.reservation.timeout.count

ct.reservation.concurrent.conflict.count

ct.reservation.duplicate.count

ct.reservation.history.failure.count

ct.reservation.unknown.count



시스템:

\`\`\`text id="rel36110"
Tomcat Busy Thread

JVM CPU·Heap·GC

Hikari Active·Idle·Pending

SQL p95

DB Lock

외부 고객연계 p95

## 36.3.7 배포 전후 비교

\`\`\`text id=“rel36111” 배포 전 30분 Baseline

배포 중

배포 후 30~60분

업무 Peak



비교:

\`\`\`text id="rel36112"
오류율

p95·p99

Timeout

Pool Pending

CPU·GC

SQL

사용자 문의

## 36.3.8 안정화 기간

예:

\`\`\`text id=“rel36113” 전환 당일 실시간 집중 관제

1주 일일 점검·결함회의

2~4주 안정화 종료 판단



실제 기간은 Release 위험도와 사용자 규모에 따라 승인한다.

\---

\## 36.3.9 안정화 일일보고

\`\`\`text id="rel36114"
거래량

성공·업무오류·시스템오류

p95·p99

Timeout

동시성 충돌

중복 등록

UNKNOWN

DB Pool

Slow SQL

장애·문의

조치·잔여위험

## 36.3.10 장애 시나리오 1 — CT Context 기동 실패

증상:

\`\`\`text id=“rel36115” Liveness 무응답

Tomcat Deploy Error

Spring Context Fail



확인:

\`\`\`text id="rel36116"
WAR 파일

Java Version

Bean 등록

Mapper Namespace

Profile 설정

DataSource

Port·Context

로그

조치:

\`\`\`text id=“rel36117” Traffic 제외 유지

원인 Snapshot

직전 WAR 복원

Health·Smoke

Traffic 복귀


\---

\## 36.3.11 장애 시나리오 2 — Health UP, 거래 실패

증상:

\`\`\`text id="rel36118"
Actuator UP

CT.Reservation.selectList 실패

원인 후보:

\`\`\`text id=“rel36119” ServiceId OM 미등록

Handler 미등록

Mapper XML 누락

DB Table 없음

권한 Seed 없음

Gateway Route 오류



따라서 Smoke Test가 필수다.

\---

\## 36.3.12 장애 시나리오 3 — Mapper Namespace 오류

증상:

\`\`\`text id="rel36120"
Invalid bound statement

Statement not found

조치:

\`\`\`text id=“rel36121” Mapper Interface

XML Namespace

SQL ID

WAR 내부 XML

Build Resource

ClassLoader



를 확인한다.

배포 전 Mapper Context Test에서 차단해야 한다.

\---

\## 36.3.13 장애 시나리오 4 — DB Migration 실패

증상:

\`\`\`text id="rel36122"
Table 일부 생성

Index 실패

Seed 미반영

원칙:

\`\`\`text id=“rel36123” Application 배포 중단

Migration 상태 확인

부분 반영 정리

검증 SQL 재수행

DBA 승인 전 진행 금지


\---

\## 36.3.14 장애 시나리오 5 — OM Catalog 누락

증상:

\`\`\`text id="rel36124"
E-TCF-CTL

미등록 ServiceId

거래통제 차단

조치:

\`\`\`text id=“rel36125” Catalog Seed

Allow List

Timeout

권한

Cache Evict

다중 Instance 반영


\---

\## 36.3.15 장애 시나리오 6 — 권한 오류

증상:

\`\`\`text id="rel36126"
모든 사용자 403

특정 권한그룹만 403

확인:

\`\`\`text id=“rel36127” 기능권한 Seed

Role Mapping

JWT Claim

STF 권한

사용자·지점 정보

Cache Version



보안을 우회하기 위해 권한검증을 비활성화하지 않는다.

\---

\## 36.3.16 장애 시나리오 7 — 신규 Version Slow SQL

증상:

\`\`\`text id="rel36128"
목록 p95 증가

Hikari Active 증가

Pending 증가

SQL ID CT-RSV-SEL-001 지연

조치:

\`\`\`text id=“rel36129” 신규 Traffic 제한

실행계획 비교

신규 SQL·Index 확인

Rollback·거래통제 판단

데이터분포 분석


\---

\## 36.3.17 장애 시나리오 8 — History 실패

증상:

\`\`\`text id="rel36130"
등록·수정·취소 모두 시스템 오류

Master Rollback

확인:

\`\`\`text id=“rel36131” History Table

권한

Column 길이

Sequence·ID

Transaction Rollback

DB Error



Master가 반영되고 History만 실패했다면 Critical 데이터 장애다.

\---

\## 36.3.18 장애 시나리오 9 — Version 충돌 급증

증상:

\`\`\`text id="rel36132"
E-CT-RSV-0006 증가

원인 후보:

\`\`\`text id=“rel36133” 화면 Version 갱신 누락

자동 재시도

다중 Tab

업무 소유권 충돌

장시간 편집

신규 버전 Response 오류



시스템 오류율과 업무 충돌률을 구분한다.

\---

\## 36.3.19 장애 시나리오 10 — Timeout·UNKNOWN

증상:

\`\`\`text id="rel36134"
화면 Timeout

DB에는 변경 존재 가능

확인:

\`\`\`text id=“rel36135” Idempotency 상태

Master Version

History TraceId

거래로그

DB Commit 시각



사용자에게 즉시 신규 Key 재실행을 안내하지 않는다.

\---

\## 36.3.20 장애 시나리오 11 — 한 WAS만 오류

\`\`\`text id="rel36136"
AP01 CT 오류

AP02 정상

확인:

\`\`\`text id=“rel36137” Artifact Checksum

Config Version

Route

Cache

DataSource

JVM 상태



AP01만 Traffic에서 제외하고 전체 시스템 재기동을 피한다.

\---

\## 36.3.21 장애 시나리오 12 — Rollback 실패

증상:

\`\`\`text id="rel36138"
직전 WAR 없음

구 설정 없음

DB 호환 불가

Health 실패

이 상황은 배포 전 Gate 실패다.

Rollback 실패 시:

\`\`\`text id=“rel36139” Traffic 차단

잔여 정상 Instance 유지

Roll-forward Hotfix

업무 거래통제

장애관리체계 가동


\---

\## 36.3.22 자동 Rollback 기준

자동 Rollback 적합:

\`\`\`text id="rel36140"
Liveness 실패

Readiness 지속 실패

Smoke 확정 실패

Context 기동 실패

수동 승인 필요:

\`\`\`text id=“rel36141” 업무 오류율 증가

성능 저하

데이터 정합성 의심

보안 이상

DB Migration 문제



데이터 영향이 있는 상황에서 기계적으로 자동 Rollback하지 않는다.

\---

\## 36.3.23 운영 Runbook

필수 Runbook:

\`\`\`text id="rel36142"
CT WAR 배포

CT WAR Rollback

DB Migration

OM Catalog 반영

권한 오류

Slow SQL

DB Pool 고갈

History 실패

Version 충돌 급증

Timeout UNKNOWN

개인정보 사고

## 36.3.24 Runbook 형식

\`\`\`text id=“rel36143” 증상

탐지조건

영향범위

확인 명령·화면

대표 GUID

임시조치

Rollback 기준

데이터 확인

담당자

종료조건


\---

\## 36.3.25 모니터링 정상화 기준

\`\`\`text id="rel36144"
오류율 Baseline

p95 목표

Timeout 정상

Pool Pending 0

History 실패 0

UNKNOWN 0 또는 Owner 지정

Alert 정상

Dashboard 최신

Smoke 재통과

# 36.4 초보 개발자 최종 역량 점검

## 36.4.1 최종 목표

초보 개발자의 최종 목표는 코드를 혼자 많이 작성하는 것이 아니다.

\`\`\`text id=“rel36145” 요구사항을 구조화하고

기존 TCF 흐름을 찾고

표준 계층에 구현하고

정상·실패를 시험하며

배포·운영·복구까지 증거로 설명할 수 있는 상태



다.

\---

\## 36.4.2 역량 단계

| 단계 | 수준 |
|---|---|
| Level 1 | 기존 거래를 찾고 실행한다. |
| Level 2 | 표준을 따라 단순 거래를 수정한다. |
| Level 3 | 신규 CRUD를 설계·구현·테스트한다. |
| Level 4 | 배포·운영·장애까지 독립 수행한다. |

제36장의 완료목표는 Level 4의 기초다.

\---

\## 36.4.3 요구사항 역량

개발자는 다음을 할 수 있어야 한다.

\`\`\`text id="rel36146"
원시 요구사항을 기능·비기능으로 분리

결정·가정·미결 구분

상태전이 정의

기능권한·데이터권한 정의

완료조건 작성

RTM 작성

## 36.4.4 구조 이해 역량

\`\`\`text id=“rel36147” OnlineTransactionController

→ TCF

→ STF

→ Dispatcher

→ Handler

→ Facade

→ Service

→ Rule·DAO

→ Mapper

→ ETF



를 실제 소스에서 찾을 수 있어야 한다.

\---

\## 36.4.5 ServiceId 역량

\`\`\`text id="rel36148"
화면 이벤트

↔ ServiceId

↔ Handler

↔ 거래코드

↔ OM Catalog

↔ 권한·Timeout

↔ 로그

의 정합성을 검증할 수 있어야 한다.

## 36.4.6 데이터 역량

\`\`\`text id=“rel36149” Aggregate

상태전이

Transaction

Version

영향 행

중복

History

논리 삭제

Paging

Index



를 설명하고 구현할 수 있어야 한다.

\---

\## 36.4.7 오류·장애 역량

\`\`\`text id="rel36150"
Validation

업무 오류

권한 오류

시스템 오류

Timeout

UNKNOWN

Rollback

대표 GUID

Pool·Thread·SQL

을 구분할 수 있어야 한다.

## 36.4.8 보안 역량

\`\`\`text id=“rel36151” 인증과 인가

JWT 검증

사용자·지점 신뢰경계

기능·데이터권한

SQL Injection

XSS

개인정보 마스킹

감사로그

Secret 관리


\---

\## 36.4.9 테스트 역량

\`\`\`text id="rel36152"
정상

경계

빈 값

최댓값

권한

상태

중복

동시성

Rollback

Timeout

성능

장애

를 요구사항과 연결해 작성할 수 있어야 한다.

## 36.4.10 배포 역량

\`\`\`text id=“rel36153” Git Tag

CI Build

Artifact

Checksum

Profile

DB Migration

OM Seed

Rolling Deploy

Health

Smoke

Rollback



을 설명할 수 있어야 한다.

\---

\## 36.4.11 운영 역량

\`\`\`text id="rel36154"
거래로그

Metric

Dashboard

Alert

Runbook

Incident

데이터 대사

안정화

운영인수

## 36.4.12 최종 실기 과제

상담예약 기능 하나를 대상으로 다음을 제출한다.

text id="rel36155" 1. 요구사항 정의 2. 화면 이벤트–ServiceId 표 3. Aggregate·상태전이 4. 프로그램 설계서 5. DTO 명세 6. DDL·Index 7. Handler·Facade·Service·Rule·DAO·Mapper 8. 오류코드 9. OM Seed 10. 단위·통합·동시성 Test 11. CI 결과 12. WAR·Checksum 13. DB Migration 14. Smoke Script 15. Rollback Runbook 16. 운영 Dashboard 증적

## 36.4.13 최종 평가표

| 영역 | 배점 |
| --- | --- |
| 요구사항·추적성 | 10 |
| 아키텍처·계층 | 10 |
| 데이터·Transaction | 15 |
| 동시성·멱등성 | 10 |
| 오류·Timeout | 10 |
| 보안·개인정보 | 10 |
| 테스트 | 15 |
| 배포·Rollback | 10 |
| 운영·관측성 | 10 |
| 합계 | 100 |

판정 예:

\`\`\`text id=“rel36156” 90 이상 독립 수행 가능

80~89 리뷰 하 수행

70~79 핵심 보완 필요

70 미만 기초 과정 재학습



단, 다음이 있으면 점수와 관계없이 실패다.

\`\`\`text id="rel36157"
권한우회

데이터 부분 반영

Version 누락

Secret 노출

Rollback 불가

필수 Test 미실행

## 36.4.14 증적 Portfolio

\`\`\`text id=“rel36158” MR 링크

Review 결과

CI 로그

Test Report

Coverage

SQL 실행계획

Security 결과

Artifact URI

Checksum

Deploy ID

Health 결과

Smoke 결과

거래 GUID

Dashboard 화면

Rollback Drill

운영 승인



PASS라는 한 단어만 기록하지 않는다.

\---

\## 36.4.15 초보 개발자가 자주 하는 오해

| 오해 | 실제 기준 |
|---|---|
| 테스트가 통과하면 완료 | 운영·Rollback 필요 |
| Health UP이면 정상 | Smoke 필요 |
| 개발 WAR도 동일 | CI Artifact만 운영 |
| DB는 DBA가 알아서 함 | App 호환성 공동책임 |
| 권한은 화면에서 처리 | 서버가 최종통제 |
| Timeout은 크게 하면 됨 | 자원점유 증가 |
| 로그가 많으면 좋음 | 구조화·민감정보 제한 |
| Rollback은 WAR만 복사 | 설정·DB·Seed 포함 |
| 배포는 DevOps 책임 | 개발도 배포영향 책임 |
| 운영은 운영팀 책임 | 설계단계부터 공동책임 |

\---

\## 36.4.16 독립 수행 확인 질문

\`\`\`text id="rel36159"
요구사항 ID에서 테스트까지 이동할 수 있는가?

ServiceId가 어디에 등록되는지 아는가?

Handler가 선택되지 않을 때 어디를 확인하는가?

Transaction이 어디서 시작·종료되는가?

UPDATE 0건의 의미를 설명할 수 있는가?

동일 요청 재전송을 어떻게 막는가?

DB Migration이 구 WAR와 호환되는가?

WAR가 어느 Commit에서 만들어졌는가?

운영 Secret이 어디에서 주입되는가?

Health와 Smoke의 차이를 설명하는가?

Rollback 전에 어떤 증거를 확보하는가?

Rollback 후 어떤 데이터를 확인하는가?

배포 후 어떤 Metric을 얼마 동안 보는가?

운영 장애 시 누구에게 연락하는가?

# 표준 코드리뷰 체크리스트

## 요구사항·추적성

| 점검 | 완료 |
| --- | --- |
| 요구사항 ID가 있다. | □ |
| 화면 이벤트가 있다. | □ |
| ServiceId가 있다. | □ |
| 프로그램·SQL·Table이 연결된다. | □ |
| 테스트 ID가 연결된다. | □ |
| 설계서와 소스가 일치한다. | □ |

## 구조

| 점검 | 완료 |
| --- | --- |
| 업무 Controller가 없다. | □ |
| Handler는 Facade만 호출한다. | □ |
| Facade가 Transaction을 담당한다. | □ |
| Rule에 DB 호출이 없다. | □ |
| DAO에 업무 판단이 없다. | □ |
| 다른 WAR 구현을 직접 참조하지 않는다. | □ |
| DTO가 역할별로 분리됐다. | □ |

## 데이터

| 점검 | 완료 |
| --- | --- |
| Aggregate가 명확하다. | □ |
| 상태전이가 명확하다. | □ |
| Version 조건이 있다. | □ |
| 영향 행을 확인한다. | □ |
| Master·History가 원자적이다. | □ |
| DB Unique가 있다. | □ |
| Migration이 호환된다. | □ |

## 보안

| 점검 | 완료 |
| --- | --- |
| 인증 사용자정보를 사용한다. | □ |
| 기능·데이터권한이 있다. | □ |
| SQL Bind를 사용한다. | □ |
| XSS를 방지한다. | □ |
| 개인정보를 마스킹한다. | □ |
| Secret이 없다. | □ |
| 감사로그가 있다. | □ |

## 운영

| 점검 | 완료 |
| --- | --- |
| 오류코드가 있다. | □ |
| Timeout이 있다. | □ |
| GUID·TraceId가 있다. | □ |
| Metric이 있다. | □ |
| OM Catalog가 있다. | □ |
| Health·Smoke가 있다. | □ |
| Rollback이 있다. | □ |

# 표준 품질 Gate Matrix

| Gate | 자동 | 승인 | 실패 시 |
| --- | --- | --- | --- |
| Branch·Secret | O | \- | Build 차단 |
| Compile | O | \- | Build 차단 |
| Unit | O | \- | 병합 차단 |
| Architecture | O | AA | 병합 차단 |
| Mapper·DB | O | DBA | 병합 차단 |
| Integration | O | QA | Artifact 차단 |
| Security | O | 보안 | 운영 차단 |
| Package | O | DevOps | Publish 차단 |
| Dev Smoke | O | 개발 | Staging 차단 |
| Staging 기능 | O·수동 | QA | 운영 차단 |
| Performance | 수동·자동 | AA·QA | 운영 차단 |
| Rollback Drill | 수동 | 운영 | 운영 차단 |
| Go·No-Go | \- | 공동 | 전환 결정 |

# 정상 배포 흐름

text id="rel36160" 1. 상담예약 MR이 승인된다. 2. CI가 Compile·Test·Quality를 통과한다. 3. CT WAR를 한 번 빌드한다. 4. Tag·Commit·Checksum을 저장한다. 5. Artifact Repository에 게시한다. 6. Dev에 같은 Artifact를 배포한다. 7. Health·Smoke를 통과한다. 8. Staging에 승격한다. 9. 기능·성능·보안·Rollback을 검증한다. 10. Go 승인을 받는다. 11. 운영 DB에 호환 Migration을 적용한다. 12. AP01을 Traffic에서 제외한다. 13. CT WAR를 배포한다. 14. 4단계 Health를 수행한다. 15. 상담예약 전체 Smoke를 수행한다. 16. AP01 Traffic을 복귀한다. 17. AP02를 같은 방식으로 배포한다. 18. 전체 데이터와 Metric을 확인한다. 19. 안정화 관찰을 수행한다. 20. 운영인수와 완료보고를 승인한다.

# 오류·Timeout·장애 흐름

## CI 실패

\`\`\`text id=“rel36161” Test 실패

→ Artifact 생성·게시 금지

→ 기존 운영 Version 유지

→ 결함 수정 후 Pipeline 재실행


\## DB Migration 실패

\`\`\`text id="rel36162"
Migration 중단

→ App 배포 금지

→ 부분 반영 분석

→ Rollback·Forward Fix

→ DB 검증 후 재승인

## Readiness 실패

\`\`\`text id=“rel36163” Traffic 제외 유지

→ DB·설정·Handler 확인

→ 제한시간 초과 시 WAR Rollback


\## Smoke 실패

\`\`\`text id="rel36164"
Health UP

→ ServiceId 거래 실패

→ Traffic 복귀 금지

→ 증거 확보

→ 원인 수정 또는 Rollback

## 배포 후 Timeout 급증

\`\`\`text id=“rel36165” 신규 Version Traffic 제한

→ SQL·Pool·외부 구간 분석

→ 데이터 UNKNOWN 확인

→ Rollback·거래통제 판단


\## Rollback 후 데이터 불일치

\`\`\`text id="rel36166"
서비스 복귀

하지만 Master·History 불일치

→ 장애 미종료

→ 데이터 대사·보상

→ 업무 승인 후 종료

# 정상 예시

\`\`\`text id=“rel36167” Release CT Reservation 1.0.0

Tag v1.0.0

Commit abc123

Artifact ct.war

Checksum 검증 성공

DB Table·History·Index·Seed 정상

Gate Unit·Mapper·Concurrency·Security·Performance PASS

배포 AP01 → AP02 Rolling

Health Liveness·Readiness·Deep PASS

Smoke 목록·등록·상세·수정·취소 PASS

데이터 Master 상태 CANCELED Version 3 History 3건

운영 오류율 정상 p95 1.4초 Pool Pending 0

Rollback Staging Drill PASS

판정 Go-Live 완료


\---

\# 금지 예시

\`\`\`text id="rel36168"
개발자 PC에서 만든 WAR를 운영에 복사한다.

운영 서버에서 Gradle Build를 수행한다.

같은 Version WAR의 내용을 다시 바꾼다.

Git Tag 없이 운영에 배포한다.

Checksum을 확인하지 않는다.

Secret을 application-prod.yml에 실제 값으로 Commit한다.

ct-service를 Gradle에만 추가하고 tcf-cicd에 등록하지 않는다.

ServiceId를 소스에만 만들고 OM에 등록하지 않는다.

DB Migration 없이 WAR부터 배포한다.

Breaking Column 변경을 구·신 WAR 혼재 중 적용한다.

Mapper XML만 운영서버에서 교체한다.

Class 파일 하나만 Hot Patch한다.

모든 WAS를 동시에 중지한다.

Traffic Drain 없이 WAR를 교체한다.

Actuator UP만 보고 완료 처리한다.

변경 Smoke Test를 생략한다.

운영 실제 고객으로 무단 Smoke를 수행한다.

Master만 확인하고 History를 확인하지 않는다.

Rollback WAR를 준비하지 않는다.

Rollback Script를 운영에서 처음 실행한다.

DB에 운영 데이터가 있는데 Table을 DROP해 Rollback한다.

권한 오류 때문에 보안 Filter를 임시 비활성화한다.

Timeout이 증가했다고 즉시 값을 크게 늘린다.

배포 후 오류율과 p95를 관찰하지 않는다.

운영 Runbook과 비상연락망 없이 오픈한다.

잔여위험에 Owner·기한·만료일이 없다.

장애 발생 후 증거 없이 모든 Tomcat을 재기동한다.

# 연계 규칙

## Git·Artifact

\`\`\`text id=“rel36169” Commit

↔ Tag

↔ Build Number

↔ WAR

↔ Checksum

↔ Deploy ID


\## ServiceId

\`\`\`text id="rel36170"
화면 이벤트

↔ ServiceId

↔ Handler

↔ OM Catalog

↔ 권한·Timeout

↔ 거래로그

## DB

\`\`\`text id=“rel36171” Release

↔ Migration Version

↔ Schema

↔ 검증 SQL

↔ Rollback·보상 SQL


\## 운영

\`\`\`text id="rel36172"
Deploy ID

↔ Incident ID

↔ GUID

↔ Artifact Version

↔ Metric

↔ Rollback

# 책임 경계와 RACI

| 활동 | 개발 | 개발리더 | AA | DBA | 보안 | QA | DevOps | 운영 | 업무 | PMO |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 코드 작성 | R | A | C | I | C | C | I | I | C | I |
| 코드 리뷰 | R/C | A | A/C | C | C | C | I | I | C | I |
| Architecture Gate | C | C | A/R | C | C | C | I | I | I | C |
| DB Review | C | I | C | A/R | C | C | I | C | C | I |
| 보안 Review | C | I | C | C | A/R | C | I | C | I | I |
| 테스트 | R | C | C | C | C | A/R | C | I | C | I |
| Artifact 생성 | I | C | I | I | I | C | A/R | I | I | I |
| 배포 승인 | C | C | A/C | C | C | C | R/C | A/C | A/C | A |
| DB 반영 | I | I | C | A/R | I | C | C | C | I | I |
| 운영 배포 | I | I | C | C | I | C | R | A/R | I | I |
| Smoke | R/C | C | C | C | I | A/R | C | R/C | A/C | I |
| Rollback | C | I | C | C | C | C | R | A/R | C | I |
| 안정화 | R/C | C | C | C | C | R/C | C | A/R | A/C | C |
| 운영인수 | C | I | C | C | C | C | C | A/R | A/C | A |

# 데이터 및 상태관리

## Release 정보

\`\`\`text id=“rel36173” Release ID

Version

Commit

Tag

Build Number

Artifact URI

Checksum

Config Version

DB Migration Version

OM Seed Version

Deploy Status

Approvals


\## 배포 이력

\`\`\`text id="rel36174"
Deploy ID

대상 환경

대상 Instance

시작·종료시각

Artifact Version

실행자

승인자

Health

Smoke

결과

Rollback Deploy ID

## 데이터 검증

상담예약 배포 후:

\`\`\`text id=“rel36175” Master 건수

History 건수

상태별 건수

Version 이상값

Orphan History

Active Key 중복

Smoke 데이터


\---

\# 성능·용량·확장성

\## 배포 중 잔여 용량

Rolling 배포에서 한 대를 제외하면 잔여 인스턴스가 전체 Traffic을 처리해야 한다.

\`\`\`text id="rel36176"
평상시 4대

배포 중 3대

센터 장애와 겹치면 2대

다음 조건에서 NFR을 검증한다.

\`\`\`text id=“rel36177” N-1 Instance

Peak TPS

Batch 동시

DB Pool

CPU·Thread

p95


\## Startup 부하

신규 WAR 기동 시:

\`\`\`text id="rel36178"
Class Loading

Spring Bean

Mapper Parse

DB Pool

Cache Warm-up

JWKS

Code Load

가 발생한다.

Readiness는 Warm-up 완료 후 UP으로 전환한다.

## 로그·Metric 부하

운영과 동일한 로그·감사·Metric을 활성화한 상태로 성능시험한다.

# 보안·개인정보·감사

| 영역 | 기준 |
| --- | --- |
| Artifact | 접근통제·Checksum |
| Secret | 외부화·최소권한 |
| 배포계정 | 개발계정과 분리 |
| 승인 | 요청자·승인자 분리 |
| 운영접근 | Bastion·관리망 |
| DB Script | DBA 승인 |
| 개인정보 | Smoke 전용 데이터 |
| 로그 | 메모·고객번호 원문 금지 |
| 감사 | 배포·Rollback·설정·권한 변경 |
| 취약점 | Critical 0 |
| Actuator | 관리망 제한 |
| Artifact 보존 | 감사기간 유지 |

# 운영·모니터링·장애 대응

## 운영인수 산출물

\`\`\`text id=“rel36179” 운영 매뉴얼

기동·중지 절차

배포 Runbook

Rollback Runbook

DB Runbook

장애 Runbook

Dashboard 목록

Alert 목록

로그 위치

연락망

권한 Matrix

일일점검표

FAQ

잔여위험


\## 운영자 교육

\`\`\`text id="rel36180"
ServiceId 검색

GUID 추적

OM 거래통제

Timeout 확인

Health 판독

Pool·Thread 확인

Slow SQL 확인

Rollback 요청

UNKNOWN 대사

장애보고

## 운영인수 완료조건

\`\`\`text id=“rel36181” 운영자가 직접 배포·Rollback Drill 수행

대표 장애 Runbook 실행

Dashboard·Alert 수신

권한·계정 확인

문의·장애 연락체계 확인

업무 정상승인

잔여위험 인수


\---

\# 자동검증 및 품질 Gate

\## 필수 자동검증

\`\`\`text id="rel36182"
ServiceId 중복

Handler 등록

패키지 의존

Transaction 위치

Mapper Namespace

SQL ID

SELECT \*

문자열 치환

Version 조건

영향 행 검사

Secret Scan

Dependency Scan

Unit·Integration

WAR 내용

Checksum

## 배포 자동검증

\`\`\`text id=“rel36183” Artifact Version

Config Version

DB Migration 상태

Context Health

Readiness

Smoke

Metric 오류율

Rollback Artifact


\---

\# 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
|---|---|---|
| REL-001 | 요구사항·RTM 리뷰 | 누락 없음 |
| REL-002 | Handler→Mapper 직접 참조 | Gate 실패 |
| REL-003 | Rule→DAO 참조 | Gate 실패 |
| REL-004 | Facade Transaction 누락 | Gate 실패 |
| REL-005 | Version SQL 누락 | Blocker |
| REL-006 | Secret Commit | Build 차단 |
| REL-007 | ServiceId 중복 | Build 차단 |
| REL-008 | Mapper Namespace 오류 | Context Test 실패 |
| REL-009 | OM Catalog 누락 | 배포 차단 |
| REL-010 | CT Profile 누락 | 배포 차단 |
| REL-011 | Unit Test 실패 | Artifact 미생성 |
| REL-012 | Concurrency Test 실패 | 운영 차단 |
| REL-013 | Rollback Test 실패 | 운영 차단 |
| REL-014 | Security Critical | 운영 차단 |
| REL-015 | Performance 미달 | No-Go |
| REL-016 | CI Runner WAR 생성 | Artifact 정상 |
| REL-017 | 개발자 PC WAR | 운영 거절 |
| REL-018 | Checksum 불일치 | 배포 중단 |
| REL-019 | 동일 Artifact Dev→Stg | 일치 |
| REL-020 | 동일 Artifact Stg→Prod | 일치 |
| REL-021 | Additive DDL | 구 WAR 정상 |
| REL-022 | Breaking DDL | Gate 실패 |
| REL-023 | DB Migration 부분 실패 | App 배포 중단 |
| REL-024 | DB 검증 SQL 실패 | 배포 중단 |
| REL-025 | AP01 Traffic Drain | 기존 거래 종료 |
| REL-026 | AP01 CT 배포 | Context 정상 |
| REL-027 | Liveness 실패 | Rollback |
| REL-028 | Readiness 실패 | Traffic 복귀 금지 |
| REL-029 | Deep DB 실패 | Traffic 복귀 금지 |
| REL-030 | Health UP·Smoke 실패 | Rollback 판단 |
| REL-031 | 목록 Smoke | 성공 |
| REL-032 | 등록 Smoke | Master·History |
| REL-033 | 수정 Smoke | Version 증가 |
| REL-034 | 취소 Smoke | CANCELED |
| REL-035 | 권한 Smoke | 정상 차단 |
| REL-036 | Idempotency Smoke | 중복 없음 |
| REL-037 | GUID 추적 | End-to-End |
| REL-038 | Metric 증가 | Dashboard 반영 |
| REL-039 | AP01 Traffic 복귀 | 오류 없음 |
| REL-040 | AP02 Rolling | 서비스 지속 |
| REL-041 | 배포 중 Peak | NFR 유지 |
| REL-042 | 신규 SQL 지연 | Alert |
| REL-043 | History 실패 | Rollback·Critical |
| REL-044 | OM Seed 누락 | 거래 차단 |
| REL-045 | 권한 Seed 누락 | 403 |
| REL-046 | Gateway Route 누락 | 404·502 |
| REL-047 | 설정 Drift | Instance 오류 탐지 |
| REL-048 | WAR Checksum Drift | Traffic 제외 |
| REL-049 | 응답 유실 | 결과조회 |
| REL-050 | Timeout UNKNOWN | 대사 |
| REL-051 | 직전 WAR Rollback | Health·Smoke |
| REL-052 | 설정 Rollback | 정상 |
| REL-053 | DB Rollback 불가 | Roll-forward |
| REL-054 | Rollback 후 Master 대사 | 정상 |
| REL-055 | Rollback 후 History 대사 | 정상 |
| REL-056 | 전체 WAS 동시 배포 시도 | 절차 차단 |
| REL-057 | 운영 서버 Build 시도 | 차단 |
| REL-058 | Class 단위 반영 | 차단 |
| REL-059 | Secret 로그 노출 | Security 실패 |
| REL-060 | Actuator 외부 접근 | 차단 |
| REL-061 | 안정화 30분 | 지표 정상 |
| REL-062 | Peak 시간 안정화 | 지표 정상 |
| REL-063 | 운영 Alert 수신 | 담당자 확인 |
| REL-064 | 장애 연락망 | 연결 성공 |
| REL-065 | 운영자 Rollback Drill | 성공 |
| REL-066 | Runbook 명령 오류 | 인수 차단 |
| REL-067 | 잔여위험 Owner 없음 | 인수 차단 |
| REL-068 | Conditional Go 만료 | 재승인 필요 |
| REL-069 | Hotfix | 새 Tag·Artifact |
| REL-070 | 폐기 ServiceId 호출 0 | 제거 가능 |

\---

\# 따라 하는 실무 절차

\## 1단계. MR과 RTM을 완성한다

완료 증적:

\`\`\`text id="rel36184"
요구사항

화면

ServiceId

프로그램

SQL

테스트

## 2단계. 코드·DB·보안 리뷰를 수행한다

Blocker·Critical을 모두 해결한다.

## 3단계. CI Quality Gate를 통과한다

\`\`\`text id=“rel36185” Build

Test

Architecture

Security

Package


\## 4단계. Release Artifact를 고정한다

\`\`\`text id="rel36186"
Tag

WAR

Checksum

DB Script

Config

OM Seed

## 5단계. Dev·Staging에서 검증한다

기능·성능·장애·Rollback을 수행한다.

## 6단계. Go·No-Go 승인을 받는다

잔여위험과 운영 준비를 확인한다.

## 7단계. 운영 DB를 호환 방식으로 반영한다

Backup·검증 SQL을 수행한다.

## 8단계. Rolling 배포한다

\`\`\`text id=“rel36187” Drain

Deploy

4단계 Health

Smoke

Traffic 복귀


\## 9단계. 안정화 모니터링한다

오류·p95·Pool·데이터를 확인한다.

\## 10단계. 운영인수와 완료보고를 수행한다

Runbook·교육·잔여위험을 넘긴다.

\---

\# 완료 체크리스트

\## 코드리뷰

| 점검 항목 | 완료 |
|---|:---:|
| 요구사항·RTM이 완성됐다. | □ |
| 계층 책임이 적합하다. | □ |
| Transaction이 적합하다. | □ |
| Version·Idempotency가 적합하다. | □ |
| 권한·개인정보가 적합하다. | □ |
| SQL·Index가 적합하다. | □ |
| 오류·Timeout이 적합하다. | □ |
| 로그·Metric이 적합하다. | □ |
| 배포 영향이 정리됐다. | □ |
| Blocker·Critical이 0건이다. | □ |

\## 품질 Gate

| 점검 항목 | 완료 |
|---|:---:|
| Compile이 성공했다. | □ |
| Unit·Integration이 성공했다. | □ |
| Architecture Test가 성공했다. | □ |
| Mapper·DB Test가 성공했다. | □ |
| Security Scan이 성공했다. | □ |
| Performance 기준을 충족했다. | □ |
| Rollback Drill이 성공했다. | □ |
| ServiceId·OM 검증이 성공했다. | □ |
| WAR가 CI에서 생성됐다. | □ |
| Artifact Checksum이 있다. | □ |

\## CT 배포 편입

| 점검 항목 | 완료 |
|---|:---:|
| \`ct-service\` Module이 있다. | □ |
| \`ct.war\`가 생성된다. | □ |
| \`/ct\` Context가 고유하다. | □ |
| Profile YML 3종이 있다. | □ |
| tcf-cicd Manifest에 등록됐다. | □ |
| 배포 Script에 등록됐다. | □ |
| Apache·Gateway Route가 있다. | □ |
| CT 로그 경로가 있다. | □ |
| CT Health 수집대상이다. | □ |

\## DB·기준정보

| 점검 항목 | 완료 |
|---|:---:|
| DDL이 승인됐다. | □ |
| Index Plan이 검증됐다. | □ |
| Migration이 하위호환이다. | □ |
| 검증 SQL이 있다. | □ |
| Rollback·보상 SQL이 있다. | □ |
| DB Backup을 확인했다. | □ |
| ServiceId Seed가 있다. | □ |
| 권한·Timeout·오류 Seed가 있다. | □ |
| Cache 반영절차가 있다. | □ |

\## 배포

| 점검 항목 | 완료 |
|---|:---:|
| Release Manifest가 있다. | □ |
| Tag·Commit·WAR가 연결된다. | □ |
| 직전 Artifact가 있다. | □ |
| Traffic Drain이 가능하다. | □ |
| Rolling 순서가 있다. | □ |
| Liveness가 성공한다. | □ |
| Readiness가 성공한다. | □ |
| Deep Check가 성공한다. | □ |
| Smoke가 성공한다. | □ |
| Traffic 복귀 기준이 있다. | □ |

\## 운영전환

| 점검 항목 | 완료 |
|---|:---:|
| Dashboard가 있다. | □ |
| Alert가 있다. | □ |
| Runbook이 있다. | □ |
| 비상연락망이 있다. | □ |
| 운영자 교육을 수행했다. | □ |
| Rollback Drill을 수행했다. | □ |
| 안정화 계획이 있다. | □ |
| 데이터 대사방법이 있다. | □ |
| 잔여위험이 승인됐다. | □ |
| 운영인수가 완료됐다. | □ |

\## 최종 개발자 역량

| 점검 항목 | 완료 |
|---|:---:|
| 요구사항을 구조화할 수 있다. | □ |
| 거래경로를 소스에서 찾을 수 있다. | □ |
| 신규 ServiceId를 구현할 수 있다. | □ |
| Transaction·동시성을 설명할 수 있다. | □ |
| Mapper·SQL을 검증할 수 있다. | □ |
| 권한·오류·Timeout을 구현할 수 있다. | □ |
| 테스트와 RTM을 작성할 수 있다. | □ |
| CI 실패 원인을 찾을 수 있다. | □ |
| Health·Smoke를 수행할 수 있다. | □ |
| Rollback과 운영 장애를 대응할 수 있다. | □ |

\---

\# 변경·호환성·폐기 관리

\## Release 변경

운영 승인 이후 Release 내용이 바뀌면 기존 승인은 무효다.

\`\`\`text id="rel36188"
새 Commit

→ 새 Build

→ 새 Checksum

→ 영향 Test

→ 재승인

## 설정 변경

WAR 변경 없이 운영 설정만 바꾸더라도 변경관리와 Rollback이 필요하다.

\`\`\`text id=“rel36189” 기존 값

신규 값

근거

적용 Instance

검증

원복


\## DB 변경

Column 삭제·축소·NOT NULL 전환은 구 버전 호환과 기존 데이터 품질을 먼저 검증한다.

\## ServiceId 변경

ServiceId 이름을 바꾸면 다음을 함께 변경한다.

\`\`\`text id="rel36190"
화면

Handler

OM

권한

Timeout

거래통제

로그·Dashboard

테스트

Runbook

가능하면 신규 ServiceId 추가 후 구 ServiceId를 단계적으로 폐기한다.

## Artifact 폐기

Artifact Repository의 운영·감사 보존기간 이전에는 삭제하지 않는다.

## CT 기능 폐기

\`\`\`text id=“rel36191” 신규 사용 중지

→ 호출량 확인

→ 화면·메뉴 전환

→ OM 비활성화

→ Route 차단

→ 코드 제거

→ 데이터 보존·파기

→ 문서·테스트 제거


\---

\# 시사점

\## 핵심 아키텍처 판단

첫째, 코드리뷰는 코딩 취향을 맞추는 활동이 아니라 데이터·보안·운영 위험을 배포 전에 제거하는 활동이다.

둘째, 운영에 배포하는 대상은 소스코드가 아니라 Tag·Commit·Checksum이 연결된 불변 Artifact다.

셋째, 상담예약 기능이 운영되려면 Java 코드뿐 아니라 CT Module·Profile·Route·OM·DB·로그·Metric이 하나의 Release로 편입돼야 한다.

넷째, Health Check는 프로세스 생존을 확인할 뿐 실제 상담예약 거래의 성공을 증명하지 않으므로 Smoke Test가 반드시 필요하다.

다섯째, Rolling 배포는 서비스 중단을 줄이지만 구·신 WAR와 DB Schema의 호환성이 전제돼야 한다.

여섯째, Rollback은 직전 WAR만 복사하는 작업이 아니라 설정·DB·기준정보·데이터 상태를 함께 복구하는 절차다.

일곱째, 운영 전환 완료는 개발자의 정상판정이 아니라 업무·운영·DBA·보안·품질의 공동승인으로 결정해야 한다.

여덟째, 초보 개발자의 최종 역량은 코드를 작성하는 능력보다 요구사항부터 운영 장애와 복구까지 전체 흐름을 증거로 연결하는 능력이다.

\---

\## 주요 위험

| 위험 | 결과 |
|---|---|
| 코드 Diff만 리뷰 | 운영 Gap 누락 |
| Version 조건 누락 | Lost Update |
| DB Migration 비호환 | 구·신 버전 장애 |
| CT Module 미편입 | WAR 미생성 |
| OM Seed 누락 | 거래 차단 |
| Secret 포함 | 보안사고 |
| 개발자 PC Artifact | 재현성·감사 실패 |
| 전체 WAS 동시배포 | 전면 장애 |
| Health만 확인 | 업무 장애 누락 |
| Rollback 미시험 | 복구 실패 |
| DB Drop Rollback | 데이터 유실 |
| 공통 모듈 영향 누락 | 다른 WAR 장애 |
| 안정화 미관찰 | 지연 장애 누락 |
| Runbook 미검증 | 장애 대응 지연 |
| 잔여위험 무기한 승인 | 기술부채 고착 |
| 운영인수 미완료 | 책임 공백 |

\---

\## 우선 보완 과제

1\. 상담예약 \`ct-service\` 실행 Module을 실제 프로젝트에 생성한다.
2\. \`ct-service\`를 Gradle·WAR Build·tcf-cicd Manifest에 편입한다.
3\. local·dev·prod CT Profile을 작성한다.
4\. CT Context·Apache·Gateway Route를 등록한다.
5\. 상담예약 ServiceId·권한·Timeout·오류코드 OM Seed를 작성한다.
6\. 상담예약 DDL·Index·검증·Rollback Script를 확정한다.
7\. Architecture·Mapper·Concurrency·Rollback Test를 CI Gate로 만든다.
8\. CT WAR Tag·Checksum·Artifact Repository 기준을 적용한다.
9\. 4단계 Health와 상담예약 Smoke Script를 자동화한다.
10\. Staging에서 Rolling·Rollback Drill을 수행한다.
11\. History 실패·Timeout UNKNOWN·Version 충돌 Alert를 구성한다.
12\. 운영자 교육·Runbook·비상연락망을 검증한다.
13\. 운영 전환 Go·No-Go 승인표를 작성한다.
14\. 안정화 일일보고와 종료기준을 확정한다.
15\. 배포 스크립트·Manifest·문서의 Module 목록을 하나의 Source of Truth로 일치시킨다.

\---

\## 중장기 발전 방향

\`\`\`text id="rel36192"
수동 리뷰

→ 자동 Architecture Gate

개발자 PC 빌드

→ CI 불변 Artifact

환경별 재빌드

→ Build Once·Promote

전체 배포

→ Rolling·Canary

Health 단일 확인

→ Liveness·Readiness·Deep·Smoke

WAR 단독 Rollback

→ 설정·DB·Seed 통합복구

수동 전환표

→ OM 배포 승인·이력

사후 모니터링

→ Release 자동 비교 Dashboard

개발 완료

→ Operational Acceptance

개인 경험

→ 표준 Runbook·훈련

# 마무리말

리뷰·배포·운영 전환을 완료하려면 다음 질문에 답할 수 있어야 한다.

\`\`\`text id=“rel36193” 요구사항과 테스트가 연결돼 있는가?

Handler·Facade·Service·Rule·DAO·Mapper의 책임이 분리됐는가?

Transaction·Version·Idempotency가 안전한가?

권한과 개인정보 통제가 서버에서 수행되는가?

DB Migration은 구·신 WAR와 호환되는가?

ct-service가 전체 빌드·배포 구성에 등록됐는가?

운영 WAR는 어떤 Commit과 Tag에서 만들어졌는가?

Artifact Checksum을 확인할 수 있는가?

운영 Secret이 Git과 WAR에서 분리돼 있는가?

ServiceId·권한·Timeout·오류코드가 OM에 등록됐는가?

한 인스턴스를 제외해도 잔여 용량이 충분한가?

Liveness와 Readiness의 차이를 설명할 수 있는가?

DB와 외부연계를 포함한 Deep Check가 있는가?

실제 상담예약 Smoke Test가 성공하는가?

Master·History·Version까지 확인하는가?

배포 실패 시 어느 조건에서 Rollback하는가?

직전 WAR와 설정을 즉시 복구할 수 있는가?

DB Rollback이 데이터 유실을 만들지 않는가?

배포 후 오류율·p95·Pool·SQL을 관찰하는가?

운영자가 직접 Runbook을 수행할 수 있는가?

잔여위험에 Owner와 만료일이 있는가?

운영 조직이 서비스 책임을 공식 인수했는가?



제36장의 핵심 흐름은 다음과 같다.

\`\`\`text id="rel36194"
요구사항·코드 리뷰

→ 자동 품질 Gate

→ 불변 Artifact

→ DB·설정·OM 준비

→ 배포 승인

→ Rolling Deploy

→ 4단계 Health

→ 업무 Smoke

→ 안정화 모니터링

→ Rollback 가능성 확인

→ 운영인수

가장 중요한 원칙은 다음과 같다.

\`\`\`text id=“rel36195” 개발은 코드가 동작할 때 끝나지 않는다.

승인된 Artifact가 안전한 절차로 배포되고,

실제 업무 거래와 데이터가 정상이며, 장애 시 이전 상태로 복구할 수 있고,

운영자가 로그·메트릭·Runbook으로 서비스를 책임질 수 있을 때 완료된다.

요구사항에서 운영인수까지 끊김 없이 연결할 수 있는 개발자가 NSIGHT TCF 업무를 독립적으로 수행할 수 있는 개발자다. \`\`\`
