<!-- source: ztcf-집필본/NSIGHT TCF Chapter 25- Test Strategy Chapter 25.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제25장. 테스트 전략

## 도입 전 안내말

제21장부터 제24장까지는 업무 거래가 실패하거나 다른 시스템과 연계될 때 필요한 기준을 다루었다.

\`\`\`text id=“tst25001” 제21장 → 표준 오류처리

제22장 → 인증·인가·세션·JWT

제23장 → 내부 업무 연계

제24장 → 외부 시스템 호출



설계와 구현이 아무리 정교해 보여도 실제로 검증되지 않았다면 운영 가능한 시스템이라고 할 수 없다.

초보 개발자는 테스트를 다음과 같이 생각하기 쉽다.

\`\`\`text id="tst25002"
프로그램을 실행한다.

화면에서 정상 결과가 나온다.

테스트가 끝났다.

그러나 정상 화면 한 건이 동작한다는 사실만으로 다음 항목은 증명되지 않는다.

\`\`\`text id=“tst25003” 필수값이 없을 때 DB가 실행되지 않는가?

권한이 없는 사용자가 요청하면 차단되는가?

같은 요청이 반복되면 중복 등록되지 않는가?

두 사용자가 동시에 수정하면 한 사용자의 변경이 유실되지 않는가?

Detail 등록 중 실패하면 Master도 Rollback되는가?

SQL이 제한시간을 초과하면 Connection이 반환되는가?

외부 시스템이 느릴 때 Tomcat Thread가 고갈되지 않는가?

JWT의 사용자와 Header 사용자가 다르면 차단되는가?

삭제 데이터가 일반 조회에 노출되지 않는가?

오류 응답에 SQL과 개인정보가 포함되지 않는가?

거래로그에 시작과 종료가 모두 남는가?

GUID 하나로 Gateway부터 Mapper까지 추적되는가?

배포 후에도 기존 거래가 정상 동작하는가?



따라서 테스트는 단순히 “코드가 실행된다”를 확인하는 작업이 아니다.

\`\`\`text id="tst25004"
요구사항

업무 규칙

입출력 계약

계층 책임

데이터 정합성

보안

성능

장애 복구

운영 추적성

을 반복적으로 증명하는 활동이다.

좋은 테스트 전략은 테스트 수를 많이 만드는 전략이 아니다.

\`\`\`text id=“tst25005” 어떤 위험을

어느 테스트 수준에서

어떤 데이터로

어떤 결과와 증적으로

얼마나 자주 검증할 것인가



를 정의하는 전략이다.

제25장의 핵심 흐름은 다음과 같다.

\`\`\`text id="tst25006"
요구사항
↓
위험 식별
↓
테스트 수준 선택
↓
정상·경계·실패 설계
↓
테스트 데이터 준비
↓
자동 실행
↓
결과·DB·로그 검증
↓
품질 Gate
↓
회귀 범위 관리

프로젝트 기준에서도 개발 완료는 코드 작성만을 의미하지 않는다.

\`\`\`text id=“tst25007” 설계 완료

-   코드 완료
-   OM 기준정보 등록
-   단위 테스트
-   통합 테스트
-   SQL 검증
-   코드 리뷰
-   로그·오류·감사 확인
-   CI/CD Gate 통과



가 함께 충족돼야 한다.

\---

\# 문서 개요

\## 목적

본 장의 목적은 NSIGHT TCF의 설계·개발·배포·운영 전 과정에서 적용할 테스트 전략을 정의하는 것이다.

세부 목적은 다음과 같다.

\`\`\`text id="tst25008"
요구사항과 테스트 추적성 확보

단위·통합·거래 테스트 책임 분리

TCF 공통 처리 검증

정상·경계·실패 시나리오 표준화

테스트 데이터의 독립성과 재현성 확보

트랜잭션·Rollback·동시성 검증

인증·권한·개인정보 검증

내부·외부 연계 장애 검증

SQL·DB·Timeout 검증

로그·Metric·감사 증적 확인

변경 영향 기반 회귀 범위 결정

CI/CD 자동 품질 Gate 구축

운영 장애 재현 테스트 자산화

## 적용범위

| 적용 영역 | 검증 내용 |
| --- | --- |
| DTO | 필수·길이·형식·타입 |
| Handler | ServiceId 분기·DTO 변환 |
| Facade | 트랜잭션·Rollback·Timeout |
| Service | 업무 흐름·결과 조립 |
| Rule | 상태·권한·업무 규칙 |
| DAO | Mapper 호출·결과 변환 |
| Mapper·SQL | SQL·영향 행 수·실행계획 |
| TCF | STF·Dispatcher·ETF 전체 흐름 |
| 인증 | JWT·Header 정합성·권한 |
| 거래통제 | 사용중지·채널·시간대 |
| 멱등성 | 중복·재전송·상태 |
| 내부 연계 | 계약·Timeout·오류 매핑 |
| 외부 연계 | Retry·Circuit·보상 |
| Cache | Hit·Miss·Evict·일관성 |
| Batch | Chunk·Checkpoint·재시작 |
| 파일 | 크기·Hash·중단·재개 |
| 로그 | GUID·오류코드·마스킹 |
| 운영 | Metric·Alert·Runbook |
| 배포 | Smoke·회귀·Rollback |

## 대상 독자

\`\`\`text id=“tst25009” 업무 개발자

프레임워크 개발자

QA·테스트 담당자

애플리케이션 아키텍트

DBA·데이터 아키텍트

보안 담당자

성능 테스트 담당자

DevOps·CI/CD 담당자

운영·장애 대응 담당자

업무 분석가·PMO


\## 선행조건

\`\`\`text id="tst25010"
요구사항 ID

화면 이벤트

ServiceId·거래코드

요청·응답 계약

오류코드

업무 규칙

사용 Table·SQL

Timeout 정책

권한 정책

연계 계약

운영 완료조건

이 중 하나라도 명확하지 않으면 테스트 기대결과를 결정하기 어렵다.

# 핵심 관점

\`\`\`text id=“tst25011” 테스트는 구현한 코드를 한 번 실행해 보는 활동이 아니다.

같은 입력과 조건에서 같은 결과가 반복되고,

실패했을 때 데이터와 로그가 정해진 상태로 남는지를

자동으로 증명하는 실행 가능한 설계서다.


\---

\# 학습 목표

이 장을 마치면 다음 내용을 설명하고 적용할 수 있어야 한다.

| 번호 | 학습 목표 |
|---:|---|
| 1 | 테스트 전략과 테스트 케이스의 차이를 설명한다. |
| 2 | 단위·통합·거래·End-to-End 테스트를 구분한다. |
| 3 | 정적검사와 실행 테스트를 구분한다. |
| 4 | Rule을 DB 없이 단위 테스트한다. |
| 5 | Service의 업무 흐름을 Mock으로 검증한다. |
| 6 | Handler의 ServiceId 분기와 DTO 변환을 검증한다. |
| 7 | DAO·Mapper를 실제 SQL 환경에서 검증한다. |
| 8 | H2 테스트의 장점과 Oracle 호환성 한계를 설명한다. |
| 9 | TCF 거래를 \`/online\` 표준 전문으로 검증한다. |
| 10 | STF·Dispatcher·Handler·ETF의 실행 결과를 확인한다. |
| 11 | 정상·경계·실패 테스트를 구분한다. |
| 12 | 입력값 경계와 업무 상태 경계를 구분한다. |
| 13 | 권한·중복·동시성·Timeout 실패를 검증한다. |
| 14 | Transaction Rollback을 DB 결과로 확인한다. |
| 15 | 테스트 데이터가 다른 테스트에 영향을 주지 않게 한다. |
| 16 | 고정 Clock과 고정 ID를 이용해 테스트를 재현한다. |
| 17 | 운영 개인정보를 테스트 데이터로 사용하지 않는다. |
| 18 | 외부 시스템을 Stub·Mock Server로 격리한다. |
| 19 | 병렬 테스트의 데이터 충돌을 방지한다. |
| 20 | 요구사항과 테스트 ID를 연결한다. |
| 21 | 변경 파일에서 회귀 범위를 도출한다. |
| 22 | 공통 모듈 변경 시 전체 회귀가 필요한 이유를 설명한다. |
| 23 | Contract 변경의 Provider·Consumer 회귀를 수행한다. |
| 24 | Flaky Test를 방치하면 안 되는 이유를 설명한다. |
| 25 | 코드 Coverage의 한계를 설명한다. |
| 26 | 자동 품질 Gate와 수동 승인 항목을 구분한다. |
| 27 | 테스트 결과·DB·로그·Metric을 완료 증적으로 남긴다. |
| 28 | 운영 장애를 재현 테스트로 전환한다. |

\---

\# 핵심 용어

| 용어 | 의미 |
|---|---|
| Test Strategy | 위험·수준·환경·데이터·완료기준을 정의한 전체 계획 |
| Test Case | 특정 조건·입력·절차·기대결과 |
| Test Scenario | 업무 흐름 중심의 검증 상황 |
| Unit Test | 하나의 클래스·규칙을 격리해 검증 |
| Slice Test | Web·DB 등 특정 계층만 로딩해 검증 |
| Integration Test | 여러 실제 구성요소를 연결해 검증 |
| Transaction Test | TCF 표준 거래 전체 경로를 검증 |
| Contract Test | 제공자와 소비자 간 요청·응답 계약 검증 |
| End-to-End Test | 사용자 진입부터 실제 연계·DB까지 검증 |
| Smoke Test | 배포 직후 핵심 기능의 최소 검증 |
| Regression Test | 변경으로 기존 기능이 깨지지 않았는지 검증 |
| Acceptance Test | 업무 요구사항 충족 여부 검증 |
| Performance Test | TPS·응답시간·자원 사용량 검증 |
| Security Test | 인증·인가·위변조·민감정보 검증 |
| Resilience Test | 장애·지연·재시도·복구 검증 |
| Fixture | 테스트에 필요한 고정 객체·데이터 |
| Stub | 정해진 응답을 반환하는 대체 객체 |
| Mock | 호출 여부·횟수·Parameter를 검증하는 대체 객체 |
| Fake | 단순화된 실제 동작 구현 |
| Test Double | Stub·Mock·Fake의 상위 개념 |
| Test Isolation | 테스트 간 상태와 데이터가 영향을 주지 않는 성질 |
| Flaky Test | 같은 코드에서 성공과 실패가 반복되는 불안정 테스트 |
| Coverage | 테스트가 실행한 코드의 비율 |
| Mutation Test | 코드 조건을 의도적으로 변형해 테스트의 탐지력을 확인 |
| Quality Gate | 기준 미달 시 Merge·배포를 차단하는 통제 |
| Test Evidence | 결과 리포트·로그·DB 결과·스크린샷 등의 증적 |

\---

\# 전체 테스트 포트폴리오

\`\`\`text id="tst25012"
정적검사
├─ Checkstyle
├─ ArchUnit
├─ Dependency 검사
├─ Secret Scan
└─ SQL·설정 검사

단위 테스트
├─ Rule
├─ Service
├─ Handler
├─ DTO
└─ Utility

계층·통합 테스트
├─ Mapper·DAO
├─ Spring Context
├─ Transaction
├─ Cache
└─ Client Adapter

TCF 거래 테스트
├─ StandardRequest
├─ STF
├─ Dispatcher
├─ Handler
├─ Facade·Service
├─ Mapper·DB
└─ ETF·거래로그

비기능 테스트
├─ 보안
├─ 성능
├─ 동시성
├─ 장애
├─ 복구
└─ DR

배포 검증
├─ Smoke
├─ 핵심 회귀
├─ Metric
└─ Rollback

# 테스트 피라미드

text id="tst25013" 소수 End-to-End 성능·장애·DR TCF 거래·통합 Contract·Slice Test 다수의 빠른 단위 테스트

원칙:

\`\`\`text id=“tst25014” 빠른 테스트는 많이

느리고 비싼 테스트는 위험 중심으로

운영환경과 유사한 테스트는 핵심 경로에 집중



모든 검증을 End-to-End 테스트로 수행하면 다음 문제가 생긴다.

\`\`\`text id="tst25015"
실행시간 증가

원인 위치 불명확

외부 환경 의존

데이터 준비 복잡

Flaky 증가

개발 피드백 지연

반대로 단위 테스트만 있으면 실제 Spring 설정·SQL·Transaction·TCF 경로 오류를 찾지 못한다.

# 테스트 수준별 책임

| 테스트 수준 | 주요 대상 | 실제 구성 | 주요 목적 |
| --- | --- | --- | --- |
| 정적검사 | 소스·설정 | 실행 없음 | 구조·금지패턴 |
| 단위 | Rule·Service | Mock 중심 | 업무 판단 |
| Web Slice | Controller·Filter | 제한 Context | JSON·HTTP |
| DB Slice | DAO·Mapper | Test DB | SQL·매핑 |
| Integration | 여러 Bean·DB | Spring Context | 연결 정합성 |
| TCF 거래 | /online 전체 | TCF+DB | 표준 거래 |
| Contract | Client·Provider | Stub·Schema | 계약 호환성 |
| E2E | 실제 시스템 체계 | 환경 의존 | 사용자 흐름 |
| 성능 | 핵심 거래 | 부하환경 | TPS·p95 |
| 장애 | DB·연계 지연 | 장애주입 | 격리·복구 |
| DR | 센터 전환 | DR 환경 | RTO·RPO |

# 현재 소스와 목표 테스트 구조

## 현재 기준 소스에서 확인되는 테스트

현재 기준 소스에서는 다음 영역의 테스트가 확인된다.

\`\`\`text id=“tst25016” tcf-core ├─ 거래통제 ├─ 거래로그 ├─ 표준 Header·Response ├─ 인증문맥 검증 ├─ Timeout Executor ├─ Timeout 예외 분류 └─ 표준 메시지 Catalog

tcf-web ├─ 정책 기반 Transaction Attribute ├─ 정책 기반 Query Timeout ├─ JDBC 거래통제 Repository └─ DataSource URL 처리

tcf-cache └─ Cache 설정

sv-service └─ SpringBootTest + MockMvc + H2 기반 거래로그 통합 테스트



SV 통합 테스트는 실제 \`/online\` 요청을 실행하고 거래로그 테이블의 건수 증가를 검증하는 형태다.

\`\`\`text id="tst25017"
MockMvc POST /online
↓
TCF 거래 실행
↓
TCF\_TX\_LOG INSERT
↓
처리 전후 건수 비교

이는 단순 HTTP 상태만 확인하지 않고 운영 증적까지 확인한다는 점에서 좋은 출발점이다.

프로젝트 설계자료도 통합 테스트에서 표준 요청 → TCF → Handler → Facade → Service → Rule → DAO → Mapper → DB → ETF 응답을 연결하고, ServiceId·트랜잭션·SQL·마스킹·오류코드·거래로그를 함께 검증하도록 제시한다.

## 현재 구현 상태 판단

| 항목 | 상태 | 판단 |
| --- | --- | --- |
| JUnit 5 | 구현 확인 | 표준 유지 |
| Mockito | 구현 확인 | 단위 테스트 활용 |
| AssertJ | 구현 확인 | 가독성 높은 검증 |
| Spring Boot Test | 구현 확인 | 통합 테스트 기반 |
| MockMvc | 구현 확인 | 온라인 Endpoint 검증 |
| H2 Test DB | 구현 확인 | 빠른 개발 테스트 |
| 거래로그 통합 검증 | 구현 확인 | 운영 증적 검증 |
| Timeout 단위 테스트 | 구현 확인 | 공통 기능 검증 |
| 거래통제 테스트 | 구현 확인 | STF 정책 검증 |
| 인증문맥 테스트 | 구현 확인 | Claim·Header 검증 |
| 전체 업무 WAR 테스트 | 부족 | 업무별 확대 필요 |
| Handler 단위 테스트 | 일부 문서 기준 | 코드 적용 확대 |
| Rule 단위 테스트 | 업무별 부족 | 핵심 규칙 우선 |
| Mapper SQL 통합 테스트 | 제한적 | Oracle 호환환경 필요 |
| Transaction Rollback 테스트 | 확대 필요 | Master·Detail 검증 |
| 멱등성 동시 테스트 | 확대 필요 | 다중 Thread·인스턴스 |
| JWT·Gateway 통합 테스트 | 확대 필요 | 인증 경계 전체 |
| 내부·외부 Contract Test | 확대 필요 | Provider·Consumer |
| ArchUnit | 설계 기준 존재 | 실제 적용 범위 확인 필요 |
| Testcontainers | 확인되지 않음 | 운영 DB 유사성 보완 |
| 성능 자동기준 | 별도 검증 필요 | p95·TPS Gate |
| 장애주입 | 별도 검증 필요 | DB·외부 지연 |
| Coverage Gate | 확인 필요 | 위험 기반 기준 |
| Mutation Test | 권장 확장 | 핵심 Rule 우선 |

프로젝트 CI/CD 검증기준에는 계층 의존성, ServiceId 형식·중복, Handler 등록, OM Catalog, 거래통제, Timeout, 표준 전문, Transaction 위치, Context 정리, 오류 응답과 거래로그 완결 검사가 포함된다.

# 표준 테스트 패키지 구조

text id="tst25018" src/test/java └─ com.nh.nsight.marketing.sv ├─ customer │ ├─ entry │ │ └─ SvCustomerHandlerTest.java │ ├─ application │ │ ├─ SvCustomerFacadeIntegrationTest.java │ │ └─ SvCustomerServiceTest.java │ ├─ domain │ │ └─ SvCustomerRuleTest.java │ ├─ persistence │ │ ├─ SvCustomerDaoTest.java │ │ └─ SvCustomerMapperIntegrationTest.java │ └─ transaction │ └─ SvCustomerInquiryTransactionTest.java ├─ contract │ └─ SvCustomerContractTest.java └─ architecture └─ SvArchitectureTest.java

테스트 패키지는 운영 패키지와 최대한 유사하게 구성한다.

# 테스트 ID 표준

권장 형식:

text id="tst25019" {테스트수준}-{업무코드}-{거래코드}-{일련번호}

예:

\`\`\`text id=“tst25020” UT-SV-INQ-0001-001

IT-SV-INQ-0001-001

TX-SV-INQ-0001-001

SEC-SV-INQ-0001-001

PERF-SV-INQ-0001-001

FAIL-SV-INQ-0001-001



또는 클래스 수준의 간결한 ID를 사용할 수 있다.

\`\`\`text id="tst25021"
UT-RUL-001

UT-SVC-001

IT-MAP-001

프로젝트 전체에서 하나의 규칙을 선택한다.

# 표준 테스트 케이스 형식

| 항목 | 설명 |
| --- | --- |
| 테스트 ID | 유일한 식별자 |
| 요구사항 ID | 검증 대상 요구사항 |
| 화면 ID | 관련 화면 |
| ServiceId | 대상 거래 |
| 거래코드 | 운영 거래 식별 |
| 테스트 수준 | 단위·통합·거래·성능 |
| 목적 | 검증하려는 위험 |
| 선행조건 | 데이터·권한·설정 |
| 입력 | Header·Body·파일 |
| 실행절차 | 호출·동시성·장애주입 |
| 기대 응답 | 결과코드·Body |
| 기대 DB | Row·상태·Version |
| 기대 로그 | GUID·오류코드 |
| 기대 Metric | Count·시간 |
| 정리절차 | Rollback·삭제 |
| 자동화 여부 | 자동·수동 |
| 증적 | Report·로그·SQL 결과 |
| Owner | 작성·유지 책임 |

# 요구사항–테스트 추적성

text id="tst25022" 요구사항 REQ-SV-014 ↓ 화면 SV-CUS-0001 ↓ ServiceId SV.Customer.selectSummary ↓ 프로그램 Handler·Facade·Service·Mapper ↓ 테스트 UT·IT·TX·SEC·PERF ↓ 증적 Test Report·GUID·DB·Log

추적성 매트릭스:

| 요구사항 | ServiceId | 주요 프로그램 | 테스트 | 결과 |
| --- | --- | --- | --- | --- |
| 고객번호 조회 | SV.Customer.selectSummary | Handler·Service·Mapper | TX-SV-INQ-0001-001 | PASS |
| 고객번호 필수 | 동일 | Request·Rule | UT-SV-INQ-0001-002 | PASS |
| 개인정보 마스킹 | 동일 | Service·Masking | SEC-SV-INQ-0001-006 | PASS |
| p95 3초 | 동일 | 전체 | PERF-SV-INQ-0001-001 | PASS |

# 25.1 단위·통합·거래 테스트

## 25.1.1 단위 테스트

단위 테스트는 하나의 클래스나 규칙을 다른 구성요소와 분리해 검증한다.

특징:

\`\`\`text id=“tst25023” 빠르다.

DB 없이 실행 가능하다.

실패 원인이 명확하다.

많이 실행할 수 있다.

업무 규칙을 문서처럼 표현한다.



단위 테스트의 주요 대상:

\`\`\`text id="tst25024"
Rule

Service 업무 흐름

Handler 분기

DTO Validation

오류 매핑

Contract Mapper

Utility

상태 전이

멱등성 상태판단

## 25.1.2 Rule 테스트

Rule은 가능한 한 순수 함수처럼 테스트한다.

운영 코드:

\`\`\`java id=“tst25025” @Component public class CampaignStateRule {

public void validateTransition(
CampaignStatus current,
CampaignAction action) {

if (!isAllowed(current, action)) {
throw new BusinessException(
"E-CM-BIZ-0004",
"현재 상태에서는 처리할 수 없습니다."
);
}
}

}



테스트:

\`\`\`java id="tst25026"
class CampaignStateRuleTest {

private final CampaignStateRule rule =
new CampaignStateRule();

@Test
void draftCampaignCanRequestApproval() {
assertThatCode(() ->
rule.validateTransition(
CampaignStatus.DRAFT,
CampaignAction.REQUEST\_APPROVAL
)
).doesNotThrowAnyException();
}

@Test
void completedCampaignCannotBeModified() {
BusinessException exception =
assertThrows(
BusinessException.class,
() -> rule.validateTransition(
CampaignStatus.COMPLETED,
CampaignAction.UPDATE
)
);

assertThat(exception.getErrorCode())
.isEqualTo("E-CM-BIZ-0004");
}
}

Rule 테스트에서는 DB·Spring Context를 사용하지 않는 것이 기본이다.

## 25.1.3 Parameterized Test

상태 전이표를 반복 테스트로 표현할 수 있다.

\`\`\`java id=“tst25027” @ParameterizedTest @CsvSource({ “DRAFT, UPDATE, true”, “DRAFT, REQUEST\_APPROVAL, true”, “REQUESTED, APPROVE, true”, “APPROVED, UPDATE, false”, “RUNNING, DELETE, false”, “COMPLETED, UPDATE, false” }) void validateCampaignTransition( CampaignStatus status, CampaignAction action, boolean allowed) {

if (allowed) {
assertThatCode(() ->
rule.validateTransition(status, action))
.doesNotThrowAnyException();
} else {
assertThatThrownBy(() ->
rule.validateTransition(status, action))
.isInstanceOf(BusinessException.class);
}

}



상태 전이표와 테스트 데이터가 일치해야 한다.

\---

\## 25.1.4 Service 테스트

Service 테스트는 업무 처리 순서와 협력 객체 호출을 검증한다.

\`\`\`java id="tst25028"
@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

@Mock
CampaignDao campaignDao;

@Mock
CampaignStateRule stateRule;

@Mock
CampaignHistoryService historyService;

@InjectMocks
CampaignService campaignService;

@Test
void updateCampaignChecksCurrentStateBeforeUpdate() {

CampaignCurrentResult current =
CampaignFixtures.draftCampaign(
3L
);

when(campaignDao.findCurrent("CMP-001"))
.thenReturn(Optional.of(current));

when(campaignDao.updateCampaign(any()))
.thenReturn(1);

CampaignUpdateResponse response =
campaignService.updateCampaign(
CampaignFixtures.updateRequest(
"CMP-001",
3L
),
TestTransactionContexts.user()
);

InOrder order = inOrder(
campaignDao,
stateRule,
historyService
);

order.verify(campaignDao)
.findCurrent("CMP-001");

order.verify(stateRule)
.validateAction(
CampaignStatus.DRAFT,
CampaignAction.UPDATE
);

order.verify(campaignDao)
.updateCampaign(any());

order.verify(historyService)
.recordUpdate(any());

assertThat(response.versionNo())
.isEqualTo(4L);
}
}

Service 단위 테스트는 SQL 자체보다 다음을 검증한다.

\`\`\`text id=“tst25029” 조회 순서

Rule 적용

DAO Parameter

결과 0·1·2건 처리

예외 변환

응답 조립

부수효과 호출


\---

\## 25.1.5 Mock 과다 사용 주의

다음처럼 Mock이 지나치게 많으면 Service 책임이 과도할 수 있다.

\`\`\`text id="tst25030"
Service Test

Mock 15개

When 30개

Verify 40개

검토:

\`\`\`text id=“tst25031” Service가 너무 많은 업무를 조정하는가?

도메인 규칙이 Rule로 분리됐는가?

읽기와 변경 유스케이스가 분리됐는가?

외부 의존이 Adapter로 분리됐는가?



Mock 수를 줄이기 위해 실제 DB를 단위 테스트에 넣기보다 설계를 먼저 점검한다.

\---

\## 25.1.6 Handler 테스트

Handler의 책임:

\`\`\`text id="tst25032"
ServiceId 지원

Body DTO 변환

Validation

Facade 호출

예외 전파

예:

\`\`\`java id=“tst25033” @ExtendWith(MockitoExtension.class) class SvCustomerHandlerTest {

@Mock
SvCustomerFacade facade;

@Mock
TransactionBodyConverter converter;

@InjectMocks
SvCustomerHandler handler;

@Test
void supportedServiceIdCallsFacade() {

StandardRequest<?> request =
TestRequests.customerInquiry();

SvCustomerInquiryRequest body =
new SvCustomerInquiryRequest(
"C000001",
LocalDate.of(2026, 7, 18)
);

when(converter.convertAndValidate(
request.getBody(),
SvCustomerInquiryRequest.class))
.thenReturn(body);

handler.handle(
request,
TestTransactionContexts.user()
);

verify(facade).selectSummary(
body,
any()
);
}

}


\---

\## 25.1.7 Facade 테스트

Facade는 트랜잭션 경계다.

단순 Unit Test만으로 실제 Rollback을 증명하기 어렵다.

다음 두 수준을 함께 사용한다.

\`\`\`text id="tst25034"
단위 테스트
→ Service 위임 여부

통합 테스트
→ 실제 Transaction Commit·Rollback

## 25.1.8 DAO 테스트

DAO 테스트:

\`\`\`text id=“tst25035” Mapper 호출

Parameter 변환

Optional 변환

영향 행 수 반환

기술 예외 변환



DAO가 단순 Mapper Wrapper라면 Mock 단위 테스트의 가치가 낮을 수 있다.

대신 Mapper 통합 테스트에 집중할 수 있다.

\---

\## 25.1.9 Mapper·SQL 테스트

검증항목:

\`\`\`text id="tst25036"
SQL 문법

Parameter Binding

Result Mapping

Null

날짜·숫자 타입

동적 SQL

정렬

페이징

논리삭제

Version 조건

영향 행 수

Query Timeout

Execution Plan

프로젝트 테스트 시나리오도 DAO·Mapper에서 Optional 결과, 최대 건수와 SQL Timeout을 확인하도록 제시한다.

## 25.1.10 H2 테스트의 장점

현재 SV 통합 테스트는 Oracle Mode의 H2 In-Memory DB를 사용한다.

장점:

\`\`\`text id=“tst25037” 빠른 실행

별도 DB 설치 불필요

테스트 종료 후 초기화

CI 적용 용이

Schema Fixture 관리 용이


\---

\## 25.1.11 H2 테스트의 한계

H2 Oracle Mode가 실제 Oracle과 완전히 같지는 않다.

차이가 발생할 수 있는 영역:

\`\`\`text id="tst25038"
Oracle 전용 함수

MERGE 세부문법

Sequence

Hint

Partition

CLOB·BLOB

날짜·Timestamp

빈 문자열과 null

Lock

Execution Plan

Isolation

Query Timeout

대소문자·Identifier

Stored Procedure

DB Link

따라서 테스트 단계를 나눈다.

\`\`\`text id=“tst25039” 개발·PR → H2 빠른 테스트

통합환경 → 실제 Oracle 또는 호환 컨테이너

성능환경 → 운영과 유사한 Oracle 구성


\---

\## 25.1.12 Testcontainers 권장

운영 DB와 유사한 DB를 Container로 실행할 수 있는 환경이라면 Testcontainers를 검토한다.

\`\`\`text id="tst25040"
테스트 시작
↓
DB Container 생성
↓
Schema·Seed 적용
↓
Mapper·Transaction 테스트
↓
Container 제거

장점:

\`\`\`text id=“tst25041” 개발자별 독립 DB

CI 재현성

환경 오염 감소

실제 DB 동작에 근접



사내 보안정책과 Oracle Image 사용권한을 확인해야 한다.

\---

\## 25.1.13 Spring Context 테스트

검증:

\`\`\`text id="tst25042"
필수 Bean 등록

Handler 등록

ServiceId 중복

Mapper Namespace

DataSource

Transaction Manager

Cache Manager

JWT Decoder

설정 Binding

애플리케이션이 기동돼야만 발견되는 오류를 CI에서 먼저 찾는다.

## 25.1.14 TCF 거래 테스트

TCF 거래 테스트는 표준 요청이 실제 공통 거래 흐름을 통과하는지 확인한다.

text id="tst25043" StandardRequest ↓ OnlineTransactionController ↓ TCF.process() ↓ STF.preProcess() ↓ Timeout Executor ↓ Dispatcher ↓ Handler ↓ Facade ↓ Service·Rule ↓ DAO·Mapper ↓ ETF ↓ StandardResponse

## 25.1.15 거래 테스트에서 확인할 항목

| 구간 | 확인 항목 |
| --- | --- |
| Controller | /online 진입 |
| Header | 업무코드·ServiceId·거래코드 |
| STF | Validation·인증·권한·통제 |
| Dispatcher | 올바른 Handler |
| Timeout | 정책 적용 |
| Facade | Transaction |
| Service | 업무 규칙 |
| Mapper | SQL·영향 행 |
| ETF | 성공·업무·시스템 오류 |
| 응답 | StandardResponse |
| 로그 | GUID·결과·오류코드 |
| 감사 | 중요 변경 |
| 멱등성 | 상태 전환 |
| Context | 요청 종료 후 정리 |

## 25.1.16 MockMvc 거래 테스트 예

\`\`\`java id=“tst25044” @SpringBootTest @AutoConfigureMockMvc class SvCustomerTransactionTest {

@Autowired
MockMvc mockMvc;

@Test
void customerInquiryReturnsStandardResponse()
throws Exception {

mockMvc.perform(
post("/online")
.contentType(
MediaType.APPLICATION\_JSON)
.content(
TestResources.read(
"sv-customer-inquiry.json"
)
)
)
.andExpect(status().isOk())
.andExpect(
jsonPath("$.result.resultCode")
.value("S0000")
)
.andExpect(
jsonPath("$.header.serviceId")
.value(
"SV.Customer.selectSummary"
)
)
.andExpect(
jsonPath("$.header.guid")
.isNotEmpty()
);
}

}



HTTP 200만 검증하지 않는다.

\---

\## 25.1.17 거래로그 검증

\`\`\`java id="tst25045"
TransactionLogRow log =
transactionLogRepository
.findByGuid(guid);

assertThat(log.serviceId())
.isEqualTo(
"SV.Customer.selectSummary"
);

assertThat(log.resultStatus())
.isEqualTo("SUCCESS");

assertThat(log.startedAt())
.isNotNull();

assertThat(log.endedAt())
.isNotNull();

거래로그 완결성은 TCF 테스트의 핵심 완료 증적이다.

## 25.1.18 Transaction Rollback 테스트

\`\`\`java id=“tst25046” @Test void detailFailureRollsBackMaster() throws Exception {

failureInjector.failOnDetailInsert();

mockMvc.perform(
post("/online")
.contentType(
MediaType.APPLICATION\_JSON)
.content(
TestResources.read(
"campaign-create.json"
)
)
).andExpect(status().isOk());

assertThat(
campaignJdbcTemplate.queryForObject(
"""
SELECT COUNT(\*)
FROM CM\_CAMPAIGN
WHERE CAMPAIGN\_ID = ?
""",
Integer.class,
"CMP-ROLLBACK-001"
)
).isZero();

assertThat(
campaignJdbcTemplate.queryForObject(
"""
SELECT COUNT(\*)
FROM CM\_CAMPAIGN\_TARGET
WHERE CAMPAIGN\_ID = ?
""",
Integer.class,
"CMP-ROLLBACK-001"
)
).isZero();

}



응답 오류만 확인하지 않고 DB 원상복구를 확인한다.

\---

\## 25.1.19 Contract 테스트

내부·외부 연계 계약은 다음을 검증한다.

\`\`\`text id="tst25047"
Request 필드

Response 필드

필수·선택

Null

코드값

오류코드

Timeout

Version

추가 필드 호환성

Provider와 Consumer가 서로 다른 시점에 배포될 수 있으므로 양쪽 테스트가 필요하다.

## 25.1.20 End-to-End 테스트

E2E:

text id="tst25048" 브라우저 → SSO → Gateway → 업무 WAR → DB → 내부·외부 연계 → 화면 결과

적합:

\`\`\`text id=“tst25049” 핵심 사용자 여정

운영 전환

주요 업무 시나리오

다중 시스템 계약

배포 Smoke



전체 케이스를 E2E로 구현하지 않는다.

\---

\## 25.1.21 비기능 테스트

\### 보안

\`\`\`text id="tst25050"
JWT 누락·만료·위변조

Header 사용자 불일치

기능권한

데이터권한

로그 개인정보

SQL Injection

SSRF

파일 악성코드

### 성능

\`\`\`text id=“tst25051” 목표 TPS

p50·p95·p99

Error Rate

Thread

Heap·GC

DB Pool

SQL

외부 연계


\### 장애

\`\`\`text id="tst25052"
DB 지연

DB Connection 고갈

외부 Timeout

Gateway 중단

JWKS 장애

Cache 장애

Batch 중단

네트워크 단절

### 복구

\`\`\`text id=“tst25053” Rollback

재시작

Idempotency

Checkpoint

보상

DR 전환


\---

\# 25.2 정상·경계·실패 케이스

\## 25.2.1 정상 테스트만으로 부족한 이유

정상 테스트:

\`\`\`text id="tst25054"
올바른 고객번호

정상 권한

DB 정상

외부 시스템 정상

동시 사용자 없음

운영 장애는 주로 정상 조건 밖에서 발생한다.

\`\`\`text id=“tst25055” 빈 값

최대값

중복

동시 처리

부분 실패

지연

설정 누락

권한 변경

외부 장애


\---

\## 25.2.2 세 가지 기본 분류

| 분류 | 의미 | 예 |
|---|---|---|
| 정상 | 대표 업무 흐름 | 고객 1건 조회 |
| 경계 | 허용범위 끝·상태 전환점 | pageSize 500 |
| 실패 | 계약·업무·시스템 오류 | DB Timeout |

\---

\## 25.2.3 입력 정상·경계·실패

| 분류 | 시나리오 |
|---|---|
| 정상 | 고객번호 정상 |
| 경계 | 최소 길이·최대 길이 |
| 경계 | 목록 0건·최대 건수 |
| 경계 | 시작일=종료일 |
| 실패 | 필수값 없음 |
| 실패 | 타입 오류 |
| 실패 | 허용하지 않는 Enum |
| 실패 | JSON Parsing 오류 |

\---

\## 25.2.4 업무 규칙 정상·경계·실패

캠페인 상태:

| 분류 | 현재 상태 | 요청 | 기대 |
|---|---|---|---|
| 정상 | \`DRAFT\` | 수정 | 성공 |
| 정상 | \`REQUESTED\` | 승인 | 성공 |
| 경계 | \`REJECTED\` | 수정 | 정책 확인 |
| 실패 | \`APPROVED\` | 일반 수정 | 상태 오류 |
| 실패 | \`RUNNING\` | 삭제 | 상태 오류 |
| 실패 | \`COMPLETED\` | 승인 | 상태 오류 |

\---

\## 25.2.5 동시성 테스트

\`\`\`text id="tst25056"
Version 3 데이터

Thread A
→ Version 3 UPDATE

Thread B
→ Version 3 UPDATE

기대:

\`\`\`text id=“tst25057” 한 요청 → UPDATE 1건·Version 4

다른 요청 → UPDATE 0건·동시성 오류



두 요청 모두 성공하면 Lost Update 결함이다.

\---

\## 25.2.6 동시성 테스트 예

\`\`\`java id="tst25058"
@Test
void onlyOneConcurrentUpdateSucceeds()
throws Exception {

ExecutorService executor =
Executors.newFixedThreadPool(2);

CountDownLatch ready =
new CountDownLatch(2);

CountDownLatch start =
new CountDownLatch(1);

Callable<String> task = () -> {
ready.countDown();
start.await();

return campaignClient.update(
"CMP-001",
3L
).errorCode();
};

Future<String> first =
executor.submit(task);

Future<String> second =
executor.submit(task);

ready.await();
start.countDown();

List<String> results =
List.of(
first.get(),
second.get()
);

assertThat(results)
.containsExactlyInAnyOrder(
null,
"E-CM-CON-0001"
);
}

테스트 종료 후 Executor를 반드시 정리한다.

## 25.2.7 중복 요청 테스트

| 상태 | 기대 결과 |
| --- | --- |
| 첫 요청 | 처리 시작 |
| 같은 Key 처리 중 | PROCESSING |
| 같은 Key 성공 후 | 기존 결과 |
| 같은 Key 다른 Body | 오류 |
| 다른 Key 같은 업무 키 | DB Unique·업무 중복 |
| Timeout Key 재요청 | 상태조회 |

## 25.2.8 권한 테스트

\`\`\`text id=“tst25059” Token 없음

Token 만료

서명 위변조

JWT 사용자와 Header 사용자 불일치

기능권한 없음

타 지점 데이터

본인 작성건 본인 승인

삭제 데이터 관리자 권한 없음



프로젝트 거래 테스트 기준도 Token·Header·기능권한·데이터권한과 TCF 통제 시나리오를 별도로 검증하도록 구성한다.

\---

\## 25.2.9 Transaction 실패 테스트

\`\`\`text id="tst25060"
Master INSERT 성공

Detail 1 INSERT 성공

Detail 2 INSERT 실패

History 미실행

결과
전체 Rollback

검증:

\`\`\`text id=“tst25061” Master 0건

Detail 0건

History 0건

Outbox 0건

거래로그 FAIL

Idempotency FAIL·UNKNOWN 정책


\---

\## 25.2.10 영향 행 수 테스트

| SQL 결과 | 기대 |
|---:|---|
| INSERT 1 | 성공 |
| INSERT 0 | 정합성 오류 |
| UPDATE 1 | 성공 |
| UPDATE 0 | 미존재·Version·상태 분류 |
| UPDATE 2 이상 | 심각한 정합성 오류 |
| DELETE 1 | 정책상 성공 |
| DELETE 0 | 상태·Version 오류 |

\---

\## 25.2.11 Timeout 테스트

다음 계층을 구분한다.

\`\`\`text id="tst25062"
Query Timeout

Transaction Timeout

TCF Online Timeout

내부 HTTP Read Timeout

외부 HTTP Connect Timeout

외부 HTTP Read Timeout

검증:

\`\`\`text id=“tst25063” 정해진 시간 내 오류

Transaction Rollback

Connection 반환

Thread 종료

후속 거래 정상

오류코드 정확

Timeout Metric 증가


\---

\## 25.2.12 외부 연계 실패 테스트

| 실패 | 기대 |
|---|---|
| Connect 실패 | 제한 Retry |
| Read Timeout 조회 | 정책상 Retry |
| Read Timeout 변경 | UNKNOWN |
| HTTP 400 | Retry 없음 |
| HTTP 503 | Retry·Circuit |
| Circuit Open | 네트워크 미호출 |
| 응답 필드 누락 | Contract 오류 |
| 외부 업무 거절 | 업무 오류 매핑 |
| Callback 중복 | 1회 반영 |
| Callback 순서 역전 | 상태 역행 차단 |

\---

\## 25.2.13 Cache 테스트

\`\`\`text id="tst25064"
첫 조회
→ DB 실행·Cache 저장

두 번째 조회
→ Cache Hit

데이터 변경
→ Commit 후 Evict

변경 Rollback
→ 기존 Cache 유지 또는 정책

TTL 만료
→ DB 재조회

## 25.2.14 Batch 테스트

\`\`\`text id=“tst25065” 정상 Chunk

Chunk 중간 실패

Checkpoint 저장

재시작

중복 Item

Skip 상한

Retry 초과

입출력 대사

PARTIAL

Job Lock


\---

\## 25.2.15 로그·감사 테스트

정상:

\`\`\`text id="tst25066"
GUID

ServiceId

거래코드

사용자

시작·종료시각

결과

처리시간

실패:

\`\`\`text id=“tst25067” 오류코드

오류유형

실패단계

원인 예외

Rollback 결과



금지정보:

\`\`\`text id="tst25068"
JWT 원문

비밀번호

주민번호 전체

계좌번호 전체

SQL 전체

Stack Trace 응답

## 25.2.16 정상·경계·실패 Matrix

| 영역 | 정상 | 경계 | 실패 |
| --- | --- | --- | --- |
| 입력 | 일반값 | 최소·최대 | 누락·형식 |
| 조회 | 1건 | 0건·최대건 | DB 오류 |
| 변경 | Version 일치 | 상태 전환 | Version 충돌 |
| 권한 | 정상 지점 | 관리자 범위 | 타 지점 |
| 멱등성 | 첫 요청 | 처리 중 | Key 재사용 |
| Timeout | 목표 이내 | 임계 근접 | 초과 |
| 연계 | 정상 응답 | 느린 응답 | 중단 |
| Batch | 정상 Chunk | 마지막 Chunk | 중간 실패 |
| Cache | Hit | TTL 만료 | 저장소 장애 |
| 로그 | 정상 완결 | Partial | 누락·민감정보 |

# 25.3 테스트 데이터와 격리

## 25.3.1 테스트 데이터가 중요한 이유

같은 테스트가 실행할 때마다 결과가 달라지는 주요 원인은 코드보다 데이터인 경우가 많다.

\`\`\`text id=“tst25069” 이전 테스트 데이터가 남아 있다.

현재 날짜가 달라졌다.

다른 테스트가 같은 고객을 수정했다.

외부 Stub 상태가 바뀌었다.

Random ID가 충돌했다.

테스트 실행순서에 의존한다.



좋은 테스트 데이터는 다음 특성을 가져야 한다.

\`\`\`text id="tst25070"
독립적이다.

반복 가능하다.

의미가 명확하다.

최소한이다.

민감정보가 없다.

정리 가능하다.

## 25.3.2 운영 데이터 사용 금지

금지:

\`\`\`text id=“tst25071” 운영 DB 복사본을 개발자 PC에 사용

실제 고객 주민번호 사용

운영 Access Token 사용

운영 API Key 사용

운영 파일을 그대로 테스트



불가피하게 운영 유사 데이터가 필요한 경우:

\`\`\`text id="tst25072"
비식별화

마스킹

승인

접근권한

보존기간

파기

반출통제

감사

를 적용한다.

## 25.3.3 Synthetic Data

테스트 목적에 맞는 인공 데이터를 만든다.

예:

\`\`\`text id=“tst25073” CUST-NORMAL-001

CUST-NOTFOUND-001

CUST-BRANCH-DENY-001

CMP-DRAFT-V3-001

CMP-DELETED-001

BATCH-FAIL-ITEM-001



식별자만 봐도 테스트 목적을 알 수 있게 한다.

\---

\## 25.3.4 Test Fixture Builder

\`\`\`java id="tst25074"
public final class CampaignFixtures {

public static CampaignCurrentResult
draftCampaign(long version) {

return new CampaignCurrentResult(
"CMP-TEST-001",
CampaignStatus.DRAFT,
version,
"001234",
"U-TEST-001",
false
);
}

public static CampaignUpdateRequest
updateRequest(
String campaignId,
long version) {

return new CampaignUpdateRequest(
campaignId,
CampaignStatus.DRAFT,
version,
"변경 캠페인",
LocalDate.of(2026, 8, 1),
LocalDate.of(2026, 8, 31),
"테스트"
);
}

private CampaignFixtures() {
}
}

Fixture Builder는 테스트의 의미를 숨기는 거대한 공통 Utility가 되어서는 안 된다.

## 25.3.5 고정 Clock

운영 코드:

\`\`\`java id=“tst25075” @Component public class BusinessClock {

private final Clock clock;

public LocalDateTime now() {
return LocalDateTime.now(clock);
}

}



테스트:

\`\`\`java id="tst25076"
Clock fixedClock =
Clock.fixed(
Instant.parse(
"2026-07-18T00:00:00Z"
),
ZoneId.of("Asia/Seoul")
);

다음을 안정적으로 테스트할 수 있다.

\`\`\`text id=“tst25077” 만료

기준일

보존기간

영업일

Timeout Deadline

JWT exp·iat

Batch 업무일자


\---

\## 25.3.6 고정 ID 생성기

운영에서는 UUID·Sequence를 사용하더라도 테스트에서는 예측 가능한 ID를 주입한다.

\`\`\`java id="tst25078"
IdGenerator idGenerator =
() -> "CMP-TEST-000001";

응답·DB·로그의 식별자를 정확히 검증할 수 있다.

## 25.3.7 DB 테스트 격리 방식

### Transaction Rollback

java id="tst25079" @Transactional @Test void mapperTest() { ... }

테스트 종료 시 Rollback한다.

주의:

\`\`\`text id=“tst25080” 운영 코드가 REQUIRES\_NEW 사용

비동기 Thread

별도 DataSource

외부 Transaction

Commit 후 Listener



는 테스트 Transaction 밖에서 Commit될 수 있다.

\---

\### 테스트별 데이터 삭제

\`\`\`text id="tst25081"
@BeforeEach
→ 필요한 데이터 INSERT

@AfterEach
→ 생성 데이터 DELETE

실패 시 정리가 누락되지 않게 해야 한다.

### 테스트별 Schema·DB

\`\`\`text id=“tst25082” Test Class A → Schema A

Test Class B → Schema B



병렬 실행에 유리하지만 생성비용이 있다.

\---

\### Testcontainers

테스트별 또는 Suite별 DB Container를 사용한다.

\---

\## 25.3.8 테스트 순서 의존 금지

금지:

\`\`\`text id="tst25083"
테스트 1
→ 고객 등록

테스트 2
→ 테스트 1 고객 조회

테스트 3
→ 테스트 2 고객 삭제

테스트 2를 단독 실행하면 실패한다.

각 테스트는 필요한 데이터를 직접 준비해야 한다.

## 25.3.9 병렬 테스트

병렬 실행 시 충돌 대상:

\`\`\`text id=“tst25084” 같은 PK

같은 업무 키

같은 Idempotency Key

같은 파일경로

같은 Cache Key

같은 Batch Job ID

같은 Port

같은 외부 Stub 상태



대안:

\`\`\`text id="tst25085"
테스트별 Unique Prefix

격리된 DB

동적 Port

독립 Stub Scenario

병렬 금지 Tag

Resource Lock

## 25.3.10 H2 DB 초기화

현재 SV 테스트처럼 다음 설정을 사용할 수 있다.

\`\`\`text id=“tst25086” schema.sql

data.sql

In-Memory DB

DB\_CLOSE\_DELAY=-1



주의:

\`\`\`text id="tst25087"
테스트 클래스 간 DB가 유지될 수 있음

초기화 순서

동일 PK

테스트 후 상태

Context 재사용

테스트가 실행순서에 의존하지 않는지 확인한다.

## 25.3.11 외부 시스템 격리

단위 테스트:

text id="tst25088" External Client Interface → Mock

Adapter 통합 테스트:

text id="tst25089" HTTP Stub Server → WireMock·MockWebServer 등

E2E:

text id="tst25090" 실제 검증용 외부 환경

각 수준의 목적을 구분한다.

## 25.3.12 Stub 시나리오

\`\`\`text id=“tst25091” 정상 200

업무 실패 200

HTTP 400

HTTP 503

Connect Timeout

Read Timeout

느린 Body

잘못된 JSON

필수 필드 누락

중복 Callback



외부 성공 한 건만 Stub으로 만들지 않는다.

\---

\## 25.3.13 시간·재시도 테스트

실제 \`Thread.sleep(30\_000)\`을 사용하면 테스트가 느리고 불안정해진다.

대안:

\`\`\`text id="tst25092"
가상 Clock

조정 가능한 Scheduler

짧은 Test Timeout

Backoff Strategy 주입

Awaitility 등 조건 대기

고정된 긴 Sleep을 피한다.

## 25.3.14 Cache 격리

각 테스트 전:

\`\`\`text id=“tst25093” Cache Clear

고유 Cache Key

고정 TTL

Cache Manager 격리



Cache 테스트가 다른 테스트의 DB 호출 횟수에 영향을 주지 않게 한다.

\---

\## 25.3.15 파일 테스트 격리

\`\`\`java id="tst25094"
@TempDir
Path tempDirectory;

검증:

\`\`\`text id=“tst25095” 파일명

경로 Traversal

크기

Hash

중단 파일

정리

권한



고정 \`/tmp/test.dat\` 경로를 여러 테스트가 공유하지 않는다.

\---

\## 25.3.16 Batch 데이터 격리

\`\`\`text id="tst25096"
고유 Job Instance Key

고유 업무일자

고유 입력파일 ID

고유 Checkpoint

Job Lock 초기화

Error Table 정리

재시작 테스트에서는 실패 시점의 데이터를 보존하되 다른 테스트와 구분한다.

## 25.3.17 테스트 Profile

\`\`\`yaml id=“tst25097” spring: profiles: active: test

nsight: tcf: default-timeout-ms: 1000

external: credit: base-url: http://localhost:${wiremock.server.port}



금지:

\`\`\`text id="tst25098"
test Profile이 운영 URL을 참조한다.

운영 DB Credential을 사용한다.

운영 Kafka Topic을 사용한다.

운영 SSO를 호출한다.

## 25.3.18 테스트 데이터 Owner

| 데이터 | Owner |
| --- | --- |
| 업무 Fixture | 업무개발 |
| 공통 Header Fixture | FW |
| 인증 Token Fixture | 보안·FW |
| DB Schema Fixture | DBA·업무 |
| 외부 Stub | 연계·업무 |
| 성능 데이터 | 성능·DBA |
| 비식별 데이터 | 보안·데이터 |
| 정리 정책 | QA·운영 |

# 25.4 회귀 테스트 범위

## 25.4.1 회귀 테스트란 무엇인가

회귀 테스트는 신규 기능이 동작하는지만 확인하는 것이 아니다.

text id="tst25099" 이번 변경 때문에 기존에 정상 동작하던 기능이 깨지지 않았는지 확인한다.

## 25.4.2 모든 변경에서 전체 테스트를 실행할 수 있는가

이상적으로 전체 테스트가 빠르고 안정적이라면 매번 실행하는 것이 좋다.

그러나 다음 테스트는 비용이 크다.

\`\`\`text id=“tst25100” 전체 E2E

대규모 성능

장애·DR

외부 실제 연계

장시간 Batch



따라서 다음을 조합한다.

\`\`\`text id="tst25101"
빠른 전체 Unit·Static Test

변경 영향 기반 Integration Test

핵심 거래 Smoke

주기적 전체 E2E·성능·장애 Test

## 25.4.3 변경 영향 기반 범위

변경 파일:

text id="tst25102" SvCustomerMapper.xml

회귀 대상:

\`\`\`text id=“tst25103” 해당 Mapper Statement

DAO

Service

관련 ServiceId

목록·단건·다운로드

Batch

Read Model

실행계획

관련 화면


\---

\## 25.4.4 공통 모듈 변경

\`\`\`text id="tst25104"
tcf-core

tcf-web

tcf-util

StandardHeader

StandardResponse

BusinessException

TransactionContext

Dispatcher

변경 시 영향 범위가 모든 WAR로 확대될 수 있다.

기준:

\`\`\`text id=“tst25105” 전체 Unit

전체 Spring Context

대표 업무 WAR 거래

오류 경로

Timeout

로그

인증

호환성


\---

\## 25.4.5 StandardHeader 변경

영향:

\`\`\`text id="tst25106"
화면 요청

Gateway

tcf-eai

업무 WAR

거래로그

외부 연계

Batch

문서

JSON Schema

필드 하나 추가라도 직렬화·역직렬화·Echo·로그·기존 Client를 회귀 테스트한다.

## 25.4.6 ServiceId 변경

\`\`\`text id=“tst25107” Handler

화면

내부 Client

Gateway Route

OM Catalog

거래통제

Timeout

로그 Dashboard

테스트 Fixture



코드만 변경하면 안 된다.

\---

\## 25.4.7 오류코드 변경

회귀:

\`\`\`text id="tst25108"
사용자 메시지

화면 분기

Client 오류 매핑

Metric

Alert

Runbook

기존 Test Assertion

오류 의미 변경은 신규 코드로 처리한다.

## 25.4.8 DB 변경

### 컬럼 추가

\`\`\`text id=“tst25109” INSERT

SELECT Mapping

Null

기본값

구버전 프로그램

Batch

BI


\### 타입 변경

\`\`\`text id="tst25110"
Java 타입

JSON 타입

Mapper

정렬

Index

성능

기존 데이터

### Index 변경

\`\`\`text id=“tst25111” 기능 결과

Execution Plan

TPS

Lock

DML 비용


\---

\## 25.4.9 공통 SQL 변경

SQL Fragment·View·공통 Mapper 변경은 사용처를 모두 역추적한다.

\`\`\`text id="tst25112"
SQL ID
↓
Mapper Method
↓
DAO
↓
Service
↓
ServiceId
↓
화면·Batch

## 25.4.10 인증 변경

\`\`\`text id=“tst25113” JWT Claim

issuer

audience

kid

JWKS

Header 정합성

권한 Cache

로그아웃

Refresh



정상 로그인만 테스트하지 않는다.

\---

\## 25.4.11 내부·외부 계약 변경

Provider 변경:

\`\`\`text id="tst25114"
Provider Contract Test

구버전 Consumer Test

신버전 Consumer Test

오류코드

Timeout

추가 Enum

Null

## 25.4.12 설정 변경

\`\`\`text id=“tst25115” Timeout

Pool

Cache TTL

Batch Chunk

Retry

Circuit

Session Timeout

JWT 수명



코드 변경이 없어도 회귀 테스트 대상이다.

설정은 런타임 동작을 변경하는 코드와 같다.

\---

\## 25.4.13 회귀 범위 등급

| 등급 | 변경 예 | 회귀 범위 |
|---|---|---|
| R1 | 문구·주석 | 관련 Unit |
| R2 | 단일 업무 Rule | 해당 업무 Unit·거래 |
| R3 | Mapper·DB | 업무 Integration·SQL |
| R4 | 공개 Contract | Provider·Consumer |
| R5 | TCF 공통 | 전체 WAR 대표 거래 |
| R6 | 인증·Transaction | 전체 핵심 경로 |
| R7 | 인프라·DB·Gateway | E2E·성능·장애 |
| R8 | 대규모 전환 | Full Regression·DR |

\---

\## 25.4.14 회귀 테스트 선택표

| 변경 대상 | 필수 테스트 |
|---|---|
| DTO | Validation·Contract |
| Rule | Unit·상태 Matrix |
| Service | Unit·Transaction |
| Mapper | DB Integration·Plan |
| Facade | Rollback |
| Handler | ServiceId·DTO |
| TCF | 전체 거래 |
| JWT | 인증·권한 |
| tcf-eai | Contract·Timeout |
| Cache | Hit·Evict |
| Batch | Restart·Reconciliation |
| 로그 | GUID·마스킹 |
| DB Pool | 성능·장애 |
| 배포 Script | Smoke·Rollback |

\---

\## 25.4.15 코드 Coverage

Coverage는 참고지표다.

\`\`\`text id="tst25116"
Line Coverage 90%
≠ 업무위험 90% 검증

다음 테스트는 Coverage가 높아도 누락될 수 있다.

\`\`\`text id=“tst25117” 동시 수정

Timeout

Rollback

권한 우회

같은 Key 다른 Body

Callback 순서 역전

운영로그 누락



권장:

\`\`\`text id="tst25118"
핵심 Rule·Service
→ 높은 Branch Coverage

DTO·Getter
→ Coverage 목표 완화

위험 시나리오
→ 명시적 테스트

## 25.4.16 Mutation Test

Mutation Test는 조건을 반대로 바꾸거나 코드를 제거했을 때 테스트가 실패하는지 확인한다.

예:

\`\`\`text id=“tst25119” 원래 version != expectedVersion이면 오류

변형 version == expectedVersion이면 오류



테스트가 이 변형을 탐지하지 못하면 동시성 규칙 검증이 약한 것이다.

핵심 Rule·권한·금액 계산에 선택적으로 적용한다.

\---

\## 25.4.17 Flaky Test

Flaky 원인:

\`\`\`text id="tst25120"
실행순서 의존

현재시간 의존

Random

공유 DB

공유 Port

고정 Sleep

외부 시스템

Thread 종료 누락

비동기 완료 대기 부족

금지 대응:

\`\`\`text id=“tst25121” 실패하면 다시 실행한다.

가끔 실패하므로 무시한다.

CI에서 제외한다.



권장:

\`\`\`text id="tst25122"
격리

고정 Clock

조건 대기

동적 Port

Root Cause 기록

Owner 지정

수정 전 배포 Gate 유지

## 25.4.18 실패 테스트의 재실행

CI에서 자동 재실행은 환경 순간 오류를 구분하는 보조 수단일 수 있다.

그러나 다음을 숨겨서는 안 된다.

\`\`\`text id=“tst25123” 첫 실행 FAIL

두 번째 PASS



결과는 \`FLAKY\`로 기록하고 결함으로 관리한다.

\---

\## 25.4.19 운영 장애를 회귀 테스트로 전환

\`\`\`text id="tst25124"
운영 장애
↓
재현조건 정리
↓
실패 테스트 작성
↓
코드 수정
↓
테스트 PASS
↓
회귀 Suite 편입

같은 장애가 다시 발생하지 않도록 한다.

# 테스트 환경 전략

| 환경 | 목적 | 외부 연계 | 데이터 |
| --- | --- | --- | --- |
| 개발자 Local | 빠른 Unit·H2 | Mock·Stub | Synthetic |
| CI | 자동 Unit·Integration | Stub | 초기화 |
| 통합 | WAR·Oracle·연계 | 검증환경 | 비식별 |
| 성능 | TPS·용량 | Stub·일부 실제 | 대량 Synthetic |
| 보안 | 취약점·권한 | 격리 | 비식별 |
| 장애 | 지연·중단 | 장애주입 | 전용 |
| UAT | 업무 승인 | 실제 검증계 | 업무 시나리오 |
| 운영 Smoke | 배포 확인 | 운영계 | 안전한 점검 데이터 |
| DR | 전환·복구 | DR 연계 | 복제 데이터 |

# 정상 테스트 흐름

text id="tst25125" 1. 요구사항과 ServiceId를 확인한다. 2. 테스트 Fixture를 생성한다. 3. 정상 권한과 Header를 만든다. 4. 표준 요청을 \`/online\`으로 전송한다. 5. STF·Dispatcher·Handler가 정상 실행된다. 6. Service·Rule·Mapper가 기대 경로를 수행한다. 7. 응답 \`resultCode=S0000\`을 확인한다. 8. 응답 Body와 Header를 확인한다. 9. DB Row·상태·Version을 확인한다. 10. 거래로그 시작·종료를 확인한다. 11. 감사·Metric을 확인한다. 12. 테스트 데이터를 정리한다.

# 실패 테스트 흐름

text id="tst25126" 1. 실패 조건을 명시적으로 주입한다. 2. 거래를 실행한다. 3. 기대 오류코드를 확인한다. 4. Transaction Rollback을 확인한다. 5. 부분 반영 여부를 확인한다. 6. 멱등성 상태를 확인한다. 7. 거래로그 실패상태를 확인한다. 8. 원인 예외와 사용자 메시지를 구분한다. 9. 민감정보 노출 여부를 확인한다. 10. 후속 정상 거래가 가능한지 확인한다.

# Timeout 테스트 흐름

text id="tst25127" Slow SQL·Slow Stub ↓ 지정 Timeout 초과 ↓ 정확한 Timeout 예외 ↓ Transaction Rollback ↓ Thread·Connection 반환 ↓ 거래로그 TIMEOUT ↓ Metric 증가 ↓ 후속 거래 성공

# 정상 예시

\`\`\`text id=“tst25128” 요구사항 REQ-CM-021

ServiceId CM.Campaign.update

현재 상태 DRAFT

현재 Version 3

요청 Version 3

기대 응답 S0000

기대 DB Version 4

기대 이력 DRAFT 상태 수정 1건

기대 거래로그 SUCCESS

기대 감사 변경 전후 값

결과 모든 증적 일치


\---

\# 금지 예시

\`\`\`text id="tst25129"
화면에서 한 번 실행하고 테스트 완료로 처리한다.

정상 케이스만 작성한다.

테스트 기대값을 현재 구현결과에 맞춘다.

운영 DB를 단위 테스트에서 사용한다.

실제 고객 개인정보를 Fixture로 사용한다.

테스트 실행순서에 의존한다.

현재시간을 직접 사용한다.

랜덤값을 검증 없이 사용한다.

Thread.sleep으로 비동기 완료를 기다린다.

모든 객체를 Mock 처리해 실제 연결을 전혀 검증하지 않는다.

모든 테스트를 SpringBootTest로 작성한다.

Mapper SQL을 H2 결과만으로 운영 적합하다고 판단한다.

HTTP 200만 확인한다.

오류 응답만 확인하고 DB Rollback을 보지 않는다.

Transaction 테스트에 @Transactional을 붙여 운영 Commit 동작을 숨긴다.

외부 연계 정상 응답만 Stub으로 만든다.

Timeout 테스트에서 Thread·Connection 반환을 확인하지 않는다.

같은 테스트가 가끔 실패해도 재실행으로 숨긴다.

Coverage 수치만으로 품질을 승인한다.

공통 모듈 변경 후 단일 업무만 테스트한다.

오류코드 변경 후 화면·연계 회귀를 생략한다.

설정 변경을 테스트 대상에서 제외한다.

테스트 실패를 주석 처리하고 Merge한다.

운영 장애 수정 후 재현 테스트를 추가하지 않는다.

테스트 결과 Report와 GUID 증적을 남기지 않는다.

# 책임 경계와 RACI

| 활동 | 업무개발 | FW | QA | AA | DBA | 보안 | 성능 | 운영 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 업무 시나리오 | A/R | I | C | C | I | C | I | I |
| 단위 테스트 | A/R | R/C | C | I | I | I | I | I |
| TCF 공통 테스트 | C | A/R | C | C | I | C | I | C |
| Mapper 테스트 | R | C | C | C | A/R | I | C | I |
| 거래 통합 테스트 | R | C | A/R | C | C | C | I | C |
| Contract Test | R | R/C | A/C | C | I | C | I | I |
| 보안 테스트 | C | C | C | C | I | A/R | I | C |
| 성능 테스트 | C | C | C | A/C | R/C | I | A/R | C |
| 장애 테스트 | C | R/C | C | A | C | C | C | A/R |
| 테스트 데이터 | R | C | A/R | I | R/C | C | C | I |
| 회귀 범위 | R | C | A/R | A/C | C | C | C | C |
| 품질 Gate | C | R | R | A | C | C | C | C |
| 운영 Smoke | C | C | C | I | C | C | I | A/R |
| 장애 재현 | R | R | C | C | C | C | C | A/R |

# 성능·용량·확장성

## 테스트 실행시간

권장 목표 예:

\`\`\`text id=“tst25130” Unit·Static 5분 이내

PR Integration 15분 이내

핵심 거래 회귀 30분 이내

전체 E2E 정기 실행

성능·DR 별도 승인 실행



정확한 시간은 프로젝트 환경에서 확정한다.

\---

\## 병렬 실행

병렬화 대상:

\`\`\`text id="tst25131"
독립 Unit

독립 Contract

격리 DB Integration

병렬화 주의:

\`\`\`text id=“tst25132” 공유 DB

공유 Cache

동일 Port

동일 Batch Job

동일 외부 Stub Scenario

순서 기반 데이터


\---

\## 테스트 데이터 용량

성능 테스트 데이터는 운영 규모를 반영한다.

\`\`\`text id="tst25133"
사용자
36,000명

동시요청률
10%

업무 WAR
다중 배포

목표 p95
3초

단순 100건 데이터로 Execution Plan과 Index 선택을 판단하지 않는다.

## 테스트 로그 용량

대량 성능 테스트에서 전문 전체 로그를 켜면 테스트 대상보다 로그 I/O가 병목이 될 수 있다.

운영과 동일한 Sampling·Log Level을 적용한다.

# 보안·개인정보·감사

| 영역 | 기준 |
| --- | --- |
| 테스트 데이터 | Synthetic·비식별 |
| 운영 정보 | 반출 금지 |
| Token | 테스트 전용 키 |
| Secret | CI Secret Store |
| 로그 | 개인정보 검사 |
| 인증 테스트 | 계정 잠금 영향 격리 |
| 권한 | 최소권한 계정 |
| 파일 | 악성코드 Fixture 통제 |
| 외부 연계 | 운영 Credential 금지 |
| Report | 민감정보 제거 |
| 증적 | 접근권한·보존기간 |
| 파기 | 테스트 데이터 자동 정리 |

# 운영·모니터링·장애 대응

## 테스트 결과 Metric

\`\`\`text id=“tst25134” test.total.count

test.pass.count

test.fail.count

test.skipped.count

test.flaky.count

test.duration

coverage.branch

mutation.score

regression.scope.count

quality.gate.fail.count


\---

\## 운영 증적

거래 테스트 한 건의 완료 증적:

\`\`\`text id="tst25135"
Test Report

요청 JSON

응답 JSON

GUID

거래로그

DB 검증 SQL 결과

오류코드

감사로그

Metric

실행 Build Version

## 실패 분석 순서

text id="tst25136" 1. 첫 실패 Assertion 확인 2. Unit·Integration 수준 구분 3. Test Data 확인 4. 실행순서 의존 확인 5. 최근 변경 Diff 확인 6. Transaction·Context 확인 7. 외부 Stub 상태 확인 8. 실제 결함·테스트 결함 분리 9. 재현 최소화 10. 결함 수정 후 회귀 편입

# 자동검증 및 품질 Gate

## 1\. 정적 구조 Gate

\`\`\`text id=“tst25137” Handler → DAO·Mapper 금지

Rule → DAO·Mapper 금지

Transaction → Facade

업무 WAR 간 직접 의존 금지

금지 Controller

순환 의존

ServiceId 형식


\---

\## 2. Service Catalog Gate

\`\`\`text id="tst25138"
코드 Handler ServiceId

OM Service Catalog

거래통제

Timeout

오류코드

권한

감사정책

집합을 비교한다.

## 3\. Unit Test Gate

\`\`\`text id=“tst25139” 핵심 Rule 정상·실패

Service 0·1·다건 결과

Handler ServiceId 분기

오류 매핑

DTO Validation


\---

\## 4. Transaction Gate

\`\`\`text id="tst25140"
Master·Detail Rollback

이력 Rollback

Outbox Rollback

Version 충돌

영향 행 0·2건

Timeout Rollback

## 5\. Security Gate

\`\`\`text id=“tst25141” JWT 만료·위변조

Header 불일치

기능·데이터권한

Token 로그

개인정보 응답

SQL Injection

SSRF


\---

\## 6. Contract Gate

\`\`\`text id="tst25142"
Schema

필수 필드

Null

추가 필드

Enum

오류코드

Version

Timeout

## 7\. SQL Gate

\`\`\`text id=“tst25143” Mapper 기동

실제 DB 문법

영향 행 수

논리삭제

Version

최대건수

실행계획

Query Timeout


\---

\## 8. 거래로그 Gate

\`\`\`text id="tst25144"
시작 로그

종료 로그

GUID

ServiceId

결과상태

오류코드

처리시간

민감정보 없음

## 9\. Coverage Gate

권장 접근:

\`\`\`text id=“tst25145” 전체 Line Coverage 참고 기준

핵심 Rule Branch Coverage 필수 기준

변경 코드 Coverage PR Gate

미검증 위험 리뷰 근거



숫자 하나로 모든 모듈을 평가하지 않는다.

\---

\## 10. 배포 차단 기준

다음 중 하나라도 발생하면 Merge·배포를 차단한다.

\`\`\`text id="tst25146"
컴파일 실패

Unit 실패

Integration 실패

ArchUnit 실패

ServiceId 중복

OM 정합성 실패

Critical 보안결함

Transaction Rollback 실패

Contract 비호환

개인정보 로그 검출

핵심 Smoke 실패

Flaky 미조치

# 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| TEST-001 | Rule 정상 조건 | 통과 |
| TEST-002 | Rule 상태 오류 | 업무 오류 |
| TEST-003 | Rule 권한 오류 | 권한 오류 |
| TEST-004 | Service 정상 조립 | 응답 생성 |
| TEST-005 | DAO 결과 없음 | 정책대로 처리 |
| TEST-006 | DAO 결과 2건 | 정합성 오류 |
| TEST-007 | Handler 정상 ServiceId | Facade 호출 |
| TEST-008 | Handler 미지원 ServiceId | 서비스 오류 |
| TEST-009 | DTO 필수값 누락 | Validation |
| TEST-010 | DTO 최대 길이 | 정상 |
| TEST-011 | DTO 길이 초과 | Validation |
| TEST-012 | Mapper 정상 조회 | 매핑 성공 |
| TEST-013 | Mapper 0건 | 빈 결과 |
| TEST-014 | Mapper 동적 조건 | SQL 정확 |
| TEST-015 | Oracle 전용 SQL | 실제 DB 검증 |
| TEST-016 | Mapper Namespace 오류 | 기동 실패 |
| TEST-017 | Spring Context | Bean 정상 |
| TEST-018 | ServiceId 중복 | 기동 실패 |
| TEST-019 | OM 미등록 ServiceId | Gate 실패 |
| TEST-020 | 표준 Header 정상 | 거래 실행 |
| TEST-021 | Header 누락 | STF 차단 |
| TEST-022 | Token 없음 | 인증 오류 |
| TEST-023 | Token 만료 | 인증 오류 |
| TEST-024 | Token 위변조 | 인증 오류 |
| TEST-025 | Header 사용자 불일치 | 차단 |
| TEST-026 | 기능권한 없음 | 권한 오류 |
| TEST-027 | 데이터권한 없음 | 권한 오류 |
| TEST-028 | 거래통제 차단 | Handler 미실행 |
| TEST-029 | 정상 /online 거래 | S0000 |
| TEST-030 | 업무 오류 거래 | businessFail |
| TEST-031 | 시스템 오류 거래 | systemError |
| TEST-032 | 거래로그 SUCCESS | 시작·종료 |
| TEST-033 | 거래로그 FAIL | 오류코드 |
| TEST-034 | 거래로그 저장 실패 | 정책 확인 |
| TEST-035 | Master·Detail 정상 | Commit |
| TEST-036 | Detail 실패 | 전체 Rollback |
| TEST-037 | History 실패 | 전체 Rollback |
| TEST-038 | Outbox 실패 | 전체 Rollback |
| TEST-039 | UPDATE 0건 | 원인 분류 |
| TEST-040 | UPDATE 2건 | 정합성 오류 |
| TEST-041 | 동시 Version 수정 | 한 건 성공 |
| TEST-042 | 첫 멱등 요청 | 처리 |
| TEST-043 | PROCESSING 재요청 | 처리 중 |
| TEST-044 | SUCCESS 재요청 | 기존 결과 |
| TEST-045 | 같은 Key 다른 Body | 오류 |
| TEST-046 | Query Timeout | 제한시간 종료 |
| TEST-047 | Transaction Timeout | Rollback |
| TEST-048 | TCF Timeout | 표준 오류 |
| TEST-049 | Timeout 후 Connection | 반환 |
| TEST-050 | Timeout 후 정상 거래 | 성공 |
| TEST-051 | 내부 연계 정상 | Contract 성공 |
| TEST-052 | 내부 연계 Timeout | 오류 매핑 |
| TEST-053 | 내부 연계 순환 | 차단 |
| TEST-054 | 외부 HTTP 503 | Retry·Circuit |
| TEST-055 | 외부 업무 거절 | 업무 오류 |
| TEST-056 | 외부 변경 Timeout | UNKNOWN |
| TEST-057 | 외부 같은 Key 재요청 | 기존 결과 |
| TEST-058 | Circuit Open | 네트워크 미호출 |
| TEST-059 | Callback 중복 | 1회 반영 |
| TEST-060 | Callback 순서 역전 | 상태 유지 |
| TEST-061 | Cache 첫 조회 | Miss·DB |
| TEST-062 | Cache 재조회 | Hit |
| TEST-063 | Commit 후 Evict | 최신 조회 |
| TEST-064 | Rollback 시 Cache | 정합성 유지 |
| TEST-065 | Batch 정상 Chunk | Commit |
| TEST-066 | Batch Chunk 실패 | 해당 Chunk Rollback |
| TEST-067 | Batch 재시작 | Checkpoint부터 |
| TEST-068 | Batch 중복 실행 | 하나만 실행 |
| TEST-069 | Batch 대사 | 차이 0 |
| TEST-070 | 논리삭제 후 일반 조회 | 제외 |
| TEST-071 | 관리자 삭제 조회 | 권한 시 조회 |
| TEST-072 | 복구 | 상태·Version 증가 |
| TEST-073 | 로그 Token 원문 | Gate 실패 |
| TEST-074 | 응답 Stack Trace | Gate 실패 |
| TEST-075 | 개인정보 마스킹 | 정상 |
| TEST-076 | 타임존 경계 | 기대 날짜 |
| TEST-077 | 윤년·월말 | 정상 |
| TEST-078 | 테스트 순서 변경 | 동일 결과 |
| TEST-079 | 병렬 실행 | 데이터 충돌 없음 |
| TEST-080 | 고정 Clock | 재현 가능 |
| TEST-081 | 외부 Stub 정상 | 통과 |
| TEST-082 | 외부 Stub 지연 | Timeout |
| TEST-083 | 운영 URL Test Profile | Gate 실패 |
| TEST-084 | Secret 소스 포함 | Gate 실패 |
| TEST-085 | 공통 모듈 변경 | 전체 대표 회귀 |
| TEST-086 | DTO 필드 추가 | 구 Client 호환 |
| TEST-087 | DTO 필드 삭제 | Contract 실패 |
| TEST-088 | 오류코드 변경 | 소비자 회귀 |
| TEST-089 | DB Index 변경 | Plan·성능 검증 |
| TEST-090 | Pool 설정 변경 | 부하·장애 검증 |
| TEST-091 | Flaky Test | 격리·수정 |
| TEST-092 | Coverage만 높고 Rule 누락 | 승인 실패 |
| TEST-093 | 운영 장애 재현 | 실패 테스트 |
| TEST-094 | 수정 후 장애 재현 | PASS |
| TEST-095 | 배포 Smoke 정상 | 운영전환 |
| TEST-096 | Smoke 실패 | 자동 Rollback·중단 |
| TEST-097 | DR 전환 | RTO 내 복구 |
| TEST-098 | DR 데이터 | RPO 충족 |
| TEST-099 | GUID 전 구간 추적 | 로그 연결 |
| TEST-100 | 다른 개발자 재실행 | 동일 결과 |

# 따라 하는 실무 절차

## 1단계. 요구사항과 위험을 연결한다

\`\`\`text id=“tst25147” 요구사항 ID

ServiceId

데이터

권한

Transaction

Timeout

연계

운영 위험



완료 증적:

\`\`\`text id="tst25148"
요구사항–테스트 추적성 표

## 2단계. 테스트 수준을 선택한다

\`\`\`text id=“tst25149” Rule → Unit

SQL → Mapper Integration

TCF 경로 → Transaction Test

계약 → Contract Test

성능 → Load Test


\---

\## 3단계. 정상·경계·실패를 작성한다

\`\`\`text id="tst25150"
대표 정상

최솟값

최댓값

빈 값

중복

권한

동시성

Timeout

장애

## 4단계. Fixture와 환경을 준비한다

\`\`\`text id=“tst25151” Synthetic Data

Fixed Clock

Fixed ID

Test Profile

Stub

DB 초기화


\---

\## 5단계. 단위 테스트를 작성한다

\`\`\`text id="tst25152"
Rule

Service

Handler

Mapper 변환

오류 매핑

## 6단계. DB·통합 테스트를 작성한다

\`\`\`text id=“tst25153” Schema

Seed

SQL

영향 행

Rollback

로그


\---

\## 7단계. TCF 거래를 실행한다

\`\`\`text id="tst25154"
표준 Request

/online

StandardResponse

GUID

거래로그

## 8단계. 실패와 Rollback을 확인한다

\`\`\`text id=“tst25155” 오류코드

DB 0건·원상태

Idempotency

Audit

Outbox


\---

\## 9단계. 회귀 범위를 결정한다

\`\`\`text id="tst25156"
변경파일

사용처

ServiceId

소비자

설정

운영

## 10단계. 자동 Gate를 실행한다

\`\`\`text id=“tst25157” Gradle Test

ArchUnit

Schema

Secret Scan

OM 정합성

Coverage


\---

\## 11단계. 증적을 저장한다

\`\`\`text id="tst25158"
Test Report

Build ID

Commit ID

GUID

DB 결과

로그

승인

## 12단계. 실패 테스트를 회귀 Suite에 편입한다

운영·통합시험에서 발견된 결함은 재현 테스트로 남긴다.

# 완료 체크리스트

## 전략·추적성

| 확인 항목 | 완료 |
| --- | --- |
| 테스트 전략이 문서화됐다. | □ |
| 요구사항 ID가 있다. | □ |
| ServiceId와 테스트가 연결됐다. | □ |
| 위험별 테스트 수준을 선택했다. | □ |
| 자동·수동 범위를 구분했다. | □ |
| 완료 증적을 정의했다. | □ |
| Owner가 지정됐다. | □ |

## 단위 테스트

| 확인 항목 | 완료 |
| --- | --- |
| 핵심 Rule을 테스트한다. | □ |
| 정상·실패 상태를 테스트한다. | □ |
| Service 흐름을 검증한다. | □ |
| Handler 분기를 검증한다. | □ |
| DAO 예외를 검증한다. | □ |
| Mock이 과도하지 않다. | □ |
| 테스트 이름이 업무 의미를 표현한다. | □ |
| 테스트가 빠르고 독립적이다. | □ |

## 통합·거래 테스트

| 확인 항목 | 완료 |
| --- | --- |
| Spring Context가 기동된다. | □ |
| Mapper SQL을 실행한다. | □ |
| 실제 Transaction을 검증한다. | □ |
| /online 표준 거래를 실행한다. | □ |
| STF 통제를 검증한다. | □ |
| Dispatcher·Handler를 검증한다. | □ |
| ETF 응답을 검증한다. | □ |
| 거래로그를 검증한다. | □ |
| DB 결과를 검증한다. | □ |
| Rollback을 검증한다. | □ |

## 정상·경계·실패

| 확인 항목 | 완료 |
| --- | --- |
| 대표 정상 사례가 있다. | □ |
| 최소·최대 경계가 있다. | □ |
| 0건·다건 결과가 있다. | □ |
| 중복 요청이 있다. | □ |
| 동시 수정이 있다. | □ |
| 인증·권한 실패가 있다. | □ |
| Timeout이 있다. | □ |
| DB 장애가 있다. | □ |
| 연계 장애가 있다. | □ |
| 부분 실패가 있다. | □ |

## 테스트 데이터

| 확인 항목 | 완료 |
| --- | --- |
| 운영 개인정보를 사용하지 않는다. | □ |
| Synthetic Data를 사용한다. | □ |
| Fixture가 의미를 표현한다. | □ |
| Fixed Clock을 사용한다. | □ |
| Fixed ID를 사용할 수 있다. | □ |
| 테스트가 순서에 의존하지 않는다. | □ |
| 테스트 후 데이터가 정리된다. | □ |
| 병렬 실행 충돌이 없다. | □ |
| 운영 URL·Secret을 사용하지 않는다. | □ |

## 회귀·품질 Gate

| 확인 항목 | 완료 |
| --- | --- |
| 변경 영향 범위를 작성했다. | □ |
| 공통 모듈 변경을 전체 회귀했다. | □ |
| DB 변경을 SQL·Plan으로 검증했다. | □ |
| Contract 소비자를 회귀했다. | □ |
| 설정 변경을 테스트했다. | □ |
| Flaky Test가 없다. | □ |
| Coverage를 위험과 함께 판단한다. | □ |
| ArchUnit이 통과한다. | □ |
| OM 정합성이 통과한다. | □ |
| Critical 실패 시 배포가 차단된다. | □ |

## 운영 증적

| 확인 항목 | 완료 |
| --- | --- |
| Test Report가 보존된다. | □ |
| Commit·Build ID가 있다. | □ |
| 요청·응답 증적이 있다. | □ |
| GUID가 있다. | □ |
| DB 검증결과가 있다. | □ |
| 오류·감사로그가 있다. | □ |
| 성능 Metric이 있다. | □ |
| 운영 장애가 회귀 테스트로 남는다. | □ |

# 변경·호환성·폐기 관리

## 테스트 케이스 변경

기대결과를 변경할 때 다음을 확인한다.

\`\`\`text id=“tst25159” 요구사항이 변경됐는가?

구현이 잘못돼 기대값을 맞추는 것은 아닌가?

업무 담당자가 승인했는가?

기존 소비자에 영향이 있는가?



코드를 통과시키기 위해 Assertion을 약하게 바꾸지 않는다.

\---

\## 테스트 데이터 변경

공통 Fixture 변경은 많은 테스트에 영향을 줄 수 있다.

\`\`\`text id="tst25160"
기존 Fixture 유지

신규 Fixture 추가

호환기간

의미 명확화

를 우선 검토한다.

## 테스트 Framework Upgrade

\`\`\`text id=“tst25161” JUnit

Mockito

Spring Boot Test

H2

Testcontainers

Gradle



업그레이드 시:

\`\`\`text id="tst25162"
전체 테스트 실행

Deprecated API

병렬 실행

Context Cache

DB 호환성

Report 형식

CI Runner

을 검증한다.

## H2에서 실제 DB 테스트로 전환

text id="tst25163" 1. 핵심 Mapper 선정 2. 실제 DB 환경 준비 3. Schema Script 분리 4. 데이터 Fixture 변환 5. SQL 차이 확인 6. CI 실행시간 측정 7. H2 빠른 테스트 병행 8. 운영 DB 회귀범위 확대

H2를 즉시 제거할 필요는 없다.

빠른 검증과 실제 DB 검증의 역할을 분리한다.

## 테스트 폐기

테스트는 다음 조건에서 폐기할 수 있다.

\`\`\`text id=“tst25164” 관련 기능이 공식 폐기됨

ServiceId 호출량 0

대체 테스트 존재

과거 장애 추적 필요성 검토

업무·QA 승인



기능은 남아 있는데 테스트만 삭제해서는 안 된다.

\---

\## 회귀 Suite 변경

\`\`\`text id="tst25165"
신규 핵심 거래

운영 장애

공통 Framework 변경

보안 정책 변경

DB 전환

이 발생하면 회귀 Suite를 갱신한다.

# 시사점

## 핵심 아키텍처 판단

첫째, 테스트는 개발의 마지막 단계가 아니라 요구사항과 설계를 구체화하는 시작 단계다.

둘째, 단위 테스트·통합 테스트·TCF 거래 테스트는 서로 대체하지 않는다.

\`\`\`text id=“tst25166” 단위 → 업무 판단

통합 → 구성요소 연결

거래 → 실제 표준 흐름



을 각각 증명한다.

셋째, 정상 케이스보다 경계·실패·Rollback 테스트에서 시스템의 신뢰성이 드러난다.

넷째, 테스트 성공은 응답코드만으로 판단하지 않는다.

\`\`\`text id="tst25167"
응답

DB

로그

감사

Metric

자원 반환

이 함께 기대상태여야 한다.

다섯째, 테스트 데이터가 독립적이지 않으면 테스트 결과를 신뢰할 수 없다.

여섯째, H2는 빠른 개발 테스트에 유용하지만 실제 Oracle의 문법·Lock·Execution Plan·Timeout을 완전히 대신하지 못한다.

일곱째, 회귀 범위는 변경 파일 수가 아니라 데이터·계약·공통 모듈·운영설정의 영향범위로 결정해야 한다.

여덟째, Coverage 수치는 테스트의 실행범위를 보여 줄 뿐 업무위험이 검증됐음을 보장하지 않는다.

## 주요 위험

| 위험 | 결과 |
| --- | --- |
| 정상 테스트만 수행 | 운영 경계 결함 |
| 모든 테스트를 E2E로 작성 | 느림·Flaky |
| 모든 의존을 Mock | 실제 연동 결함 |
| H2만 사용 | Oracle 차이 누락 |
| 응답만 검증 | 부분 Commit 은폐 |
| 테스트 순서 의존 | 재현 불가 |
| 운영 데이터 사용 | 개인정보 사고 |
| 현재시간 의존 | 날짜별 실패 |
| 고정 Sleep | Flaky·지연 |
| 병렬 데이터 충돌 | 불규칙 실패 |
| 외부 정상 Stub만 사용 | 장애 처리 미검증 |
| Coverage 수치 중심 | 핵심 위험 누락 |
| Flaky 재실행 은폐 | CI 신뢰도 붕괴 |
| 공통 변경의 부분 회귀 | 다수 WAR 장애 |
| 설정 변경 미검증 | 운영 동작 변경 |
| 증적 미보존 | 완료 증명 불가 |
| 운영 결함 테스트 미추가 | 장애 재발 |

## 우선 보완 과제

1.  모든 업무 WAR에 최소 Handler·Service·Rule·거래 테스트를 추가한다.
2.  핵심 Mapper는 H2와 실제 Oracle 유사환경에서 이중 검증한다.
3.  Master·Detail·History·Outbox Rollback 테스트를 공통화한다.
4.  멱등성·Version 동시성 테스트를 다중 Thread로 자동화한다.
5.  JWT·Gateway·STF 인증 흐름의 통합 테스트를 확대한다.
6.  내부·외부 연계 Contract Test와 지연·Timeout Stub을 구축한다.
7.  ArchUnit 계층 검증을 실제 CI Gate에 적용한다.
8.  코드 ServiceId와 OM Catalog·거래통제·Timeout을 자동 대조한다.
9.  테스트용 Fixed Clock·ID·AuthenticationContext Fixture를 공통 제공한다.
10.  운영정보를 사용하지 않는 Synthetic Test Data Catalog를 구축한다.
11.  Flaky Test를 별도 품질결함으로 관리한다.
12.  변경 영향 기반 회귀 선택과 공통 모듈 전체 회귀 규칙을 정한다.
13.  거래 테스트에서 응답·DB·로그·Metric을 함께 검증한다.
14.  성능·보안·장애 테스트 결과를 배포 승인 Gate에 연결한다.
15.  운영 장애마다 재현 테스트를 추가하는 절차를 정착시킨다.

## 중장기 발전 방향

\`\`\`text id=“tst25168” 개별 개발자 수동 테스트 ↓ JUnit 단위 테스트

단위 테스트 중심 ↓ Mapper·TCF 통합 테스트

H2 단일 검증 ↓ 실제 DB 호환 테스트

정상 시나리오 중심 ↓ 경계·동시성·장애 테스트

수동 계약 확인 ↓ Provider·Consumer Contract Test

Coverage 중심 ↓ 위험·Mutation 기반 품질

고정 전체 회귀 ↓ 변경 영향 기반 회귀

테스트 결과 파일 ↓ 요구사항·GUID·배포 승인 통합

운영 장애 대응 ↓ 장애 재현 회귀 자산


\---

\# 마무리말

테스트 전략을 수립하는 과정은 다음 질문에 답하는 일이다.

\`\`\`text id="tst25169"
이 요구사항의 가장 큰 실패 위험은 무엇인가?

이 위험은 Unit·Integration·거래 테스트 중 어디에서 검증할 것인가?

정상값뿐 아니라 최소·최대·빈 값은 무엇인가?

업무 상태가 바뀌는 경계는 어디인가?

중복 요청과 동시 수정은 어떻게 재현할 것인가?

실패 시 Transaction이 실제로 Rollback됐는가?

응답뿐 아니라 DB·로그·Metric도 맞는가?

테스트 데이터는 다른 테스트와 격리됐는가?

현재시간·Random·외부 시스템에 의존하지 않는가?

H2 결과가 실제 Oracle에서도 동일한가?

인증·권한·개인정보를 검증했는가?

Timeout 후 Thread와 Connection이 반환됐는가?

외부 변경 결과가 불명확할 때 상태조회를 검증했는가?

이번 변경이 어떤 기존 ServiceId에 영향을 주는가?

공통 Framework 변경의 전체 회귀범위는 무엇인가?

테스트가 가끔 실패하지 않는가?

테스트 결과를 다른 개발자가 같은 환경에서 재현할 수 있는가?

요구사항부터 테스트 결과와 운영로그까지 추적할 수 있는가?

제25장의 핵심 흐름은 다음과 같다.

text id="tst25170" 요구사항·위험 ↓ 테스트 수준 선택 ↓ 정상·경계·실패 ↓ 독립된 테스트 데이터 ↓ 자동 실행 ↓ 응답·DB·로그 검증 ↓ 회귀 범위 ↓ 품질 Gate ↓ 운영 증적

가장 중요한 원칙은 다음과 같다.

\`\`\`text id=“tst25171” 테스트가 통과했다는 사실보다 무엇을 증명했는지가 중요하다.

정상 응답뿐 아니라 실패와 Rollback, 권한과 동시성, Timeout과 운영로그까지

반복해서 같은 결과로 증명할 수 있어야 변경을 안전하게 배포할 수 있다. \`\`\`
