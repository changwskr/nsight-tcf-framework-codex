<!-- source: ztcf-집필본/NSIGHT TCF Chapter 16- Transaction Registration and Local Call Flow.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제16장. 거래 등록과 로컬 호출

## 이 장을 시작하며

제13장에서는 자연어 요구사항을 실행 가능한 거래로 구조화했다.

제14장에서는 Request·Query·Result·Response DTO를 분리하고 입력 형식과 업무 규칙을 검증했다.

제15장에서는 Service·Rule·DAO·Mapper를 구현하고, 조회 결과가 없을 때의 의미와 트랜잭션 경계를 정의했다.

이제 작성한 프로그램을 TCF가 실행할 수 있는 거래로 등록하고 실제 로컬 환경에서 호출해야 한다.

요구사항과 프로그램 구현
↓
ServiceId 확정
↓
Handler에 ServiceId 등록
↓
Spring Bean 등록
↓
Dispatcher Registry 생성
↓
OM Service Catalog 등록
↓
거래통제·Timeout·권한·감사 등록
↓
업무 WAR 로컬 기동
↓
표준 요청 호출
↓
TCF 전체 경로 실행
↓
표준 응답·로그·SQL·DB 결과 확인

초보 개발자는 다음 상태를 거래 개발 완료로 오해하기 쉽다.

Java Compile 성공

Service 단위테스트 성공

Mapper SQL 실행 성공

화면에서 결과 표시

애플리케이션 Started 로그 확인

그러나 이 상태만으로는 TCF 거래가 완성된 것이 아니다.

클래스가 존재한다.
≠ Spring Bean으로 등록됐다.

Handler Bean이 존재한다.
≠ ServiceId가 Dispatcher에 등록됐다.

ServiceId가 코드에 있다.
≠ OM에서 실행 가능한 거래로 등록됐다.

애플리케이션이 기동됐다.
≠ 실제 거래가 정상 처리된다.

HTTP 200을 받았다.
≠ 업무 거래가 성공했다.

TCF 거래의 완료 여부는 다음 연결관계로 판단한다.

화면 이벤트
↔ ServiceId
↔ Handler
↔ Facade
↔ Service
↔ Rule
↔ DAO
↔ Mapper
↔ SQL·Table
↔ OM Catalog
↔ 거래통제
↔ Timeout
↔ 권한·감사
↔ 테스트
↔ 거래로그

ServiceId는 화면 요청과 서버 처리기를 연결하는 논리적 식별자이며, Dispatcher는 애플리케이션 기동 시 Handler가 선언한 ServiceId를 수집해 Registry를 만든다. 중복 ServiceId가 발견되면 임의의 Handler를 선택하지 않고 기동을 실패시키는 것이 기준이다.

## 핵심 관점

첫 거래의 완료 기준은
화면에 값이 보이는 것이 아니다.

같은 ServiceId가
소스·운영 설정·요청·응답·로그에서 일치하고,

정상·경계·실패 상황이
예상한 정책대로 재현되며,

GUID 하나로
Controller부터 SQL과 거래 종료까지
추적 가능한 상태여야 한다.

## 학습 목표

이 장을 마치면 다음 내용을 직접 수행할 수 있어야 한다.

| 번호 | 학습 목표 |
| --- | --- |
| 1 | ServiceId의 역할과 거래코드의 역할을 구분한다. |
| 2 | Handler가 담당 ServiceId를 등록하는 방법을 설명한다. |
| 3 | serviceId()와 serviceIds() 방식의 차이를 설명한다. |
| 4 | ServiceId에서 담당 업무 WAR와 도메인을 식별한다. |
| 5 | Handler가 Spring Bean으로 등록됐는지 확인한다. |
| 6 | Dispatcher Registry 생성 과정을 설명한다. |
| 7 | ServiceId 누락·미등록·중복 오류를 구분한다. |
| 8 | Handler 등록과 OM Catalog 등록의 차이를 설명한다. |
| 9 | 거래통제·Timeout·권한·감사 등록정보를 확인한다. |
| 10 | 로컬 Profile로 업무 WAR를 기동한다. |
| 11 | 실제 Port와 Context Path를 확인한다. |
| 12 | 표준 Header와 Body를 가진 요청을 작성한다. |
| 13 | curl 또는 API Client로 거래를 호출한다. |
| 14 | 표준 응답의 HTTP 상태와 업무 결과를 구분한다. |
| 15 | GUID·TraceId·ServiceId로 로그를 추적한다. |
| 16 | Mapper Statement ID와 SQL 실행 결과를 확인한다. |
| 17 | 입력 오류·업무 오류·미등록 거래·시스템 오류를 재현한다. |
| 18 | TCF·DB·외부 연계 Timeout을 구분한다. |
| 19 | 정상·경계·실패 테스트의 완료 증적을 작성한다. |
| 20 | 첫 거래 완료 체크리스트를 수행한다. |
| 21 | 코드·OM·테스트 등록 집합을 자동 비교한다. |
| 22 | 거래 변경과 폐기 시 호환성 영향을 판단한다. |

# 한눈에 보는 거래 등록과 호출 흐름

┌────────────────────────────────────────────────────────────┐
│ 1. 거래 식별 │
│ SV.Customer.selectSummary │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 2. Handler 등록 │
│ SvCustomerHandler.serviceIds() │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 3. Spring Context │
│ Handler Bean 생성 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 4. TransactionDispatcher │
│ ServiceId → Handler Registry │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 5. OM 운영 등록 │
│ Catalog·통제·Timeout·권한·감사 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 6. 로컬 기동 │
│ local Profile·Port·DB·Mapper·Handler │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 7. 표준 요청 │
│ POST /sv/online │
│ ServiceId = SV.Customer.selectSummary │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 8. TCF 실행 │
│ Controller → STF → Dispatcher → Handler │
│ → Facade → Service → DAO → Mapper → ETF │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 9. 검증 │
│ 응답·GUID·SQL·DB·거래로그 │
└────────────────────────────────────────────────────────────┘

# 현재 구현과 목표 구조

## 현재 대표 거래

업무 WAR
sv-service

ServiceId
SV.Customer.selectSummary

Handler
SvCustomerHandler

Facade
SvCustomerFacade

Service
SvCustomerService

DAO
SvCustomerDao

Mapper
SvCustomerMapper

SQL
selectCustomerSummary

실행 경로:

POST /sv/online
↓
OnlineTransactionController
↓
TCF.process()
↓
STF.preProcess()
↓
OnlineTransactionTimeoutExecutor
↓
TransactionDispatcher
↓
SvCustomerHandler
↓
SvCustomerFacade
↓
SvCustomerService
↓
SvCustomerDao
↓
SvCustomerMapper
↓
ETF

로컬 대표 거래는 sv-service를 실행한 뒤 /sv/online으로 SV.Customer.selectSummary를 호출하고, GUID·TraceId·Mapper SQL ID·DB 결과와 거래로그 종료를 확인하는 방식으로 검증한다.

## 구현 상태 구분

| 항목 | 상태 | 설명 |
| --- | --- | --- |
| 공통 Online Controller | 구현 확인 | TCF 공통 진입점 |
| ServiceId 기반 Dispatcher | 구현 확인 | Handler Registry 사용 |
| Handler의 ServiceId 선언 | 구현 확인 | 도메인 Handler 방식 |
| 중복 ServiceId 검사 | 구현 확인 | 기동 실패 기준 |
| 미등록 ServiceId 오류 | 구현 확인 | SERVICE\_NOT\_FOUND |
| Facade Transaction | 구현 확인 | 조회 거래 적용 |
| DAO·Mapper 연결 | 구현 확인 | 대표 거래 존재 |
| 로컬 bootRun | 구현 확인 | 업무 모듈 단위 실행 가능 |
| OM Catalog 실운영 연계 | 프로젝트 확인 필요 | 환경별 등록 확인 |
| 거래통제·Timeout Seed | 프로젝트 확인 필요 | 운영 기준정보 대조 필요 |
| 구조화 Handler 등록 로그 | 권장 확장 | 운영 System.out 금지 |
| 코드–OM 자동 대조 | 권장 확장 | CI/CD Gate 적용 |
| 거래로그 완결성 자동검증 | 권장 확장 | 통합테스트 필요 |
| Cross-WAR Trace | 부분 구현 | 하위 호출 Context 전파 확인 |

# 16.1 ServiceId와 Handler 연결

## 16.1.1 ServiceId란 무엇인가

ServiceId는 서버에서 실행할 업무 거래를 식별하는 논리적인 키다.

권장 형식:

{업무코드}.{도메인}.{행위}

예:

SV.Customer.selectSummary
│ │ │
│ │ └─ 고객요약을 조회하는 행위
│ └───────────── Customer 도메인
└──────────────────── SV 업무

ServiceId는 다음 목적에 동시에 사용된다.

| 사용 영역 | 역할 |
| --- | --- |
| 화면·채널 | 실행 거래 지정 |
| TCF Header | 요청 거래 식별 |
| Dispatcher | Handler 탐색 |
| Handler | Facade Method 분기 |
| OM Catalog | 운영 기준정보 |
| 거래통제 | 실행 허용·차단 |
| Timeout | 제한시간 조회 |
| 권한 | 기능 실행권한 |
| 감사 | 사용자 행위 분류 |
| 로그 | 장애 추적 |
| 통계 | 거래량·응답시간 집계 |
| 테스트 | 거래별 시나리오 연결 |

## 16.1.2 ServiceId와 거래코드의 차이

| 구분 | ServiceId | 거래코드 |
| --- | --- | --- |
| 목적 | 실행할 Handler 선택 | 운영·통계·감사 분류 |
| 예 | SV.Customer.selectSummary | SV-INQ-0001 |
| 주요 사용처 | Dispatcher | OM·거래로그·통계 |
| 의미 | 업무 유스케이스 | 거래 유형·관리번호 |
| 변경 영향 | 코드·화면·Catalog | 운영정책·통계·감사 |
| 중복 | 절대 금지 | 표준에 따라 금지 |
| 대소문자 | 정확히 일치 | 프로젝트 표준 적용 |

다음 두 값이 같은 목적을 가지는 것은 아니다.

ServiceId
\= 어느 프로그램을 실행할 것인가

거래코드
\= 운영에서 어떤 거래로 분류할 것인가

## 16.1.3 Handler의 책임

Handler는 ServiceId와 업무 Facade를 연결한다.

ServiceId 확인
↓
요청 Body를 Request DTO로 변환
↓
최소한의 거래 분기
↓
Facade 호출
↓
업무 결과 반환

Handler가 하지 않아야 할 일:

SQL 실행

Mapper 직접 호출

트랜잭션 선언

복잡한 업무 규칙 판단

JWT 원문 파싱

거래통제 재구현

Timeout 값 결정

StandardResponse 직접 생성

거래로그 직접 시작·종료

TCF 설계에서 Handler는 담당 ServiceId 선언, DTO 변환, 최소 분기와 Facade 호출을 담당하며, SQL·Mapper·트랜잭션·표준 응답은 직접 처리하지 않는 것이 기준이다.

## 16.1.4 Handler 하나와 거래 하나

단일 거래 Handler 예:

@Component
@RequiredArgsConstructor
public class SvCustomerSummaryHandler
implements TransactionHandler {

private static final String SERVICE\_ID =
"SV.Customer.selectSummary";

private final SvCustomerFacade facade;
private final TransactionBodyConverter bodyConverter;

@Override
public Set<String> serviceIds() {
return Set.of(SERVICE\_ID);
}

@Override
public Object handle(
StandardRequest<?> request,
TransactionContext context) {

CustomerSummaryRequest body =
bodyConverter.convertAndValidate(
request.getBody(),
CustomerSummaryRequest.class
);

return facade.selectCustomerSummary(
body,
context
);
}
}

적합한 경우:

거래가 독립적이다.

복잡한 분기가 없다.

별도의 운영·배포 책임이 있다.

Handler 크기가 작고 역할이 명확하다.

## 16.1.5 도메인 Handler와 여러 ServiceId

현재 NSIGHT TCF에서는 도메인 Handler가 여러 ServiceId를 담당하는 구조를 사용할 수 있다.

@Component
@RequiredArgsConstructor
public class SvCustomerHandler
implements TransactionHandler {

private static final String SELECT\_SUMMARY =
"SV.Customer.selectSummary";

private static final String SELECT\_PRODUCTS =
"SV.Customer.selectProducts";

private static final String UPDATE\_MEMO =
"SV.Customer.updateMemo";

private final SvCustomerFacade facade;
private final TransactionBodyConverter bodyConverter;

@Override
public Set<String> serviceIds() {
return Set.of(
SELECT\_SUMMARY,
SELECT\_PRODUCTS,
UPDATE\_MEMO
);
}

@Override
public Object handle(
StandardRequest<?> request,
TransactionContext context) {

String serviceId =
context.getHeader().getServiceId();

return switch (serviceId) {
case SELECT\_SUMMARY ->
handleSelectSummary(request, context);

case SELECT\_PRODUCTS ->
handleSelectProducts(request, context);

case UPDATE\_MEMO ->
handleUpdateMemo(request, context);

default ->
throw new BusinessException(
ErrorCode.SERVICE\_NOT\_FOUND
);
};
}
}

적합한 경우:

같은 업무 도메인이다.

공통 DTO 변환 기준을 사용한다.

동일한 담당 조직이 관리한다.

Handler 분기가 과도하게 커지지 않는다.

## 16.1.6 Handler 분리 기준

| 기준 | 동일 Handler | 별도 Handler |
| --- | --- | --- |
| 업무 도메인 | 동일 | 다름 |
| 담당 조직 | 동일 | 다름 |
| 변경 주기 | 유사 | 크게 다름 |
| 권한 정책 | 유사 | 별도 정책 |
| 거래 수 | 소수 | 과다 |
| 코드 크기 | 관리 가능 | 과도 |
| 배포 단위 | 동일 WAR | 다른 WAR |
| 공통 처리 | 존재 | 거의 없음 |

Handler에 수십 개 ServiceId를 넣어 거대한 switch 문을 만들지 않는다.

## 16.1.7 Spring Bean 등록

Handler 클래스가 존재해도 Bean으로 등록되지 않으면 Dispatcher가 찾을 수 없다.

확인할 Annotation:

@Component

또는:

@Bean
public TransactionHandler svCustomerHandler(...) {
return new SvCustomerHandler(...);
}

확인할 추가 조건:

Component Scan 범위

Profile

Conditional Property

Bean 이름 충돌

Gradle Build 포함 여부

실행 모듈 의존성

실패 예:

클래스 존재
\+ Compile 성공
\- Spring Bean 등록
\= 거래 실행 불가

## 16.1.8 Dispatcher Registry 생성

개념 흐름:

Spring Context 기동
↓
TransactionHandler Bean 목록 수집
↓
각 Handler의 serviceIds() 조회
↓
ServiceId 유효성 검사
↓
ServiceId → Handler Map 등록
↓
중복 검사
↓
Registry 완성

개념 코드:

for (TransactionHandler handler : handlers) {

for (String serviceId : handler.serviceIds()) {

TransactionHandler previous =
handlerMap.putIfAbsent(
serviceId,
handler
);

if (previous != null) {
throw new IllegalStateException(
"Duplicate serviceId: "
\+ serviceId
);
}
}
}

## 16.1.9 중복 ServiceId

상황:

SvCustomerHandler
→ SV.Customer.selectSummary

SvLegacyCustomerHandler
→ SV.Customer.selectSummary

결과:

Dispatcher 초기화 중 중복 발견
↓
IllegalStateException
↓
Spring Context 기동 실패

중복 ServiceId에서 임의로 마지막 Handler를 선택해서는 안 된다.

중복 거래 소유자
\= 아키텍처 오류

해결 순서:

1.  전체 저장소에서 ServiceId 검색
2.  실제 Build 대상 확인
3.  Legacy·Test Handler 구분
4.  업무 소유권 확인
5.  폐기 코드 제거
6.  화면·OM·테스트 정합성 수정
7.  Context Test 재수행

ServiceId 이름 뒤에 2, New, Temp를 붙여 기동만 성공시키지 않는다.

## 16.1.10 ServiceId 누락

요청:

{
"header": {
"businessCode": "SV"
},
"body": {
"customerNo": "C000001"
}
}

흐름:

Header.serviceId 없음
↓
STF 또는 Dispatcher 검증 실패
↓
INVALID\_HEADER
↓
Handler 미실행

확인:

UI 요청 생성

표준 Header Schema

Controller Body 변환

Gateway Header 보정

TCF Header Validation

## 16.1.11 미등록 ServiceId

요청:

SV.Customer.selectSummery

Summary의 오타다.

흐름:

ServiceId 값 존재
↓
Registry 조회
↓
Handler 없음
↓
SERVICE\_NOT\_FOUND
↓
업무 오류 응답

미등록 오류는 다음 경우에 발생한다.

UI 오타

Handler 미구현

Handler Bean 미등록

다른 Profile에서 비활성

업무 WAR 잘못 호출

폐기 ServiceId 호출

배포 버전 불일치

## 16.1.12 Handler와 Facade Method 연결

| ServiceId | Handler | Facade Method |
| --- | --- | --- |
| SV.Customer.selectSummary | SvCustomerHandler | selectCustomerSummary() |
| SV.Customer.selectProducts | SvCustomerHandler | selectCustomerProducts() |
| SV.Customer.updateMemo | SvCustomerHandler | updateCustomerMemo() |

권장 원칙:

ServiceId 하나
↔ Facade 유스케이스 Method 하나

이 관계가 명확하면 다음을 쉽게 판단할 수 있다.

트랜잭션 경계

Timeout

업무 오류

테스트 범위

변경 영향

## 16.1.13 Handler 등록과 OM Catalog 등록

Handler 등록
\= 프로그램이 실행 가능하다.

OM Catalog 등록
\= 운영에서 관리하는 공식 거래다.

| 코드 Handler | OM Catalog | 판정 |
| --- | --- | --- |
| O | O | 정상 등록 후보 |
| O | X | 코드만 존재·운영정책 누락 |
| X | O | 운영정보만 존재·실행 불가 |
| X | X | 존재하지 않는 거래 |

Handler와 Catalog 둘 다 존재해야 하지만 그것만으로도 충분하지 않다.

## 16.1.14 거래 운영 등록정보

| 등록 영역 | 필수 내용 |
| --- | --- |
| Service Catalog | ServiceId·업무코드·설명 |
| 거래코드 | 처리유형·통계 분류 |
| 거래통제 | 사용 여부·기간·채널 |
| Timeout | 전체 처리 제한시간 |
| 기능권한 | 실행 가능 권한 |
| 데이터권한 | 지점·조직·대상 범위 |
| 감사 | 조회·변경·다운로드 여부 |
| 개인정보 | 마스킹·조회사유 |
| 오류코드 | 사용자 메시지 |
| 성능 | 목표 p95·Slow 기준 |
| 담당자 | 업무·개발·운영 담당 |
| 배포정보 | 최초·현재 적용 버전 |
| 폐기정보 | 대체 ServiceId·폐기일 |

## 16.1.15 Timeout 정합성

Gateway Read Timeout
\>
TCF Online Timeout
\>
외부 Client·DB Query Timeout

예:

| 계층 | 예시 |
| --- | --- |
| Gateway Read Timeout | 4,000ms |
| TCF Online Timeout | 3,000ms |
| Facade Transaction Timeout | 3초 |
| 외부 Client Timeout | 2,000ms |
| MyBatis Query Timeout | 2초 |
| Hikari Connection Timeout | 1,000ms |

실제 값은 거래 특성과 성능시험 결과로 확정한다.

하위 Timeout이 전체 Timeout보다 길면 다음 문제가 발생할 수 있다.

TCF는 이미 Timeout 응답
↓
DB·외부 호출은 계속 실행
↓
Connection·Thread 장기 점유
↓
후속 거래 영향

## 16.1.16 ServiceId 전체 정합성

화면 정의서
↕
UI 요청
↕
Handler serviceIds()
↕
Dispatcher Registry
↕
OM Service Catalog
↕
거래통제
↕
Timeout
↕
권한
↕
테스트
↕
로그

대소문자와 점 하나의 차이도 다른 ServiceId로 처리될 수 있다.

# 16.2 요청·응답 로그 확인

## 16.2.1 로그를 보는 목적

로그는 많이 출력하는 것이 목적이 아니다.

다음 질문에 답할 수 있어야 한다.

누가 요청했는가?

어떤 ServiceId였는가?

언제 시작했는가?

어느 단계까지 성공했는가?

어떤 SQL이나 외부 시스템을 호출했는가?

어디에서 시간이 오래 걸렸는가?

어떤 오류로 종료됐는가?

데이터 변경은 Commit됐는가?

사용자에게 어떤 결과를 반환했는가?

## 16.2.2 로그·Metric·Trace의 차이

| 수단 | 답하는 질문 |
| --- | --- |
| 로그 | 해당 거래에서 무슨 일이 일어났는가 |
| Metric | 오류·지연이 얼마나 자주 발생하는가 |
| Trace | 여러 시스템 중 어디에서 지연됐는가 |
| 거래로그 | 거래가 어떤 상태로 종료됐는가 |
| 감사로그 | 누가 어떤 중요 데이터에 접근했는가 |

세 수단은 서로 대체하지 않는다.

## 16.2.3 필수 추적 식별자

| 식별자 | 역할 |
| --- | --- |
| GUID | 거래 한 건의 기본 식별자 |
| TraceId | 시스템·WAR 간 전체 호출 추적 |
| SpanId | 개별 처리 구간 |
| ParentSpanId | 상위 호출 구간 |
| ServiceId | 실행 유스케이스 |
| 거래코드 | 운영·통계 분류 |
| 업무코드 | 소유 업무 |
| 사용자 ID | 요청 사용자 |
| 지점 ID | 조직·데이터권한 |
| ChannelId | 요청 채널 |
| App Version | 실행 배포버전 |
| Instance ID | 처리 서버·인스턴스 |

## 16.2.4 요청 시작 로그

예:

event=TCF\_TRANSACTION\_STARTED
businessCode=SV
serviceId=SV.Customer.selectSummary
transactionCode=SV-INQ-0001
guid=G-20260718-000001
traceId=T-20260718-000001
userId=U12345
branchId=001234
channelId=WEBTOP
appVersion=sv-1.4.0
instanceId=sv-local-01

기록하지 않아야 할 값:

JWT 원문

비밀번호

Refresh Token

주민등록번호

계좌번호 전체

고객명 원문

전체 Request Body

## 16.2.5 단계 로그

권장 단계:

CONTROLLER\_RECEIVED

STF\_VALIDATED

TIMEOUT\_EXECUTION\_STARTED

HANDLER\_DISPATCHED

FACADE\_STARTED

SERVICE\_STARTED

MAPPER\_EXECUTED

ETF\_COMPLETED

예:

event=HANDLER\_DISPATCHED
guid=G-20260718-000001
serviceId=SV.Customer.selectSummary
handler=SvCustomerHandler
elapsedMs=12

## 16.2.6 Mapper 실행 로그

event=MAPPER\_EXECUTED
guid=G-20260718-000001
serviceId=SV.Customer.selectSummary
mapperStatement=
com.nh.nsight.marketing.sv.persistence.mapper
.SvCustomerMapper.selectCustomerSummary
sqlType=SELECT
elapsedMs=84
rowCount=1
result=SUCCESS

SQL Parameter 전체를 로그에 출력하지 않는다.

필요한 경우:

parameterPresent=true
parameterCount=3
maskedCustomerKey=C0\*\*\*\*01

## 16.2.7 응답 종료 로그

정상:

event=TCF\_TRANSACTION\_COMPLETED
guid=G-20260718-000001
serviceId=SV.Customer.selectSummary
resultStatus=SUCCESS
resultCode=0000
elapsedMs=142

업무 오류:

event=TCF\_TRANSACTION\_COMPLETED
guid=G-20260718-000002
serviceId=SV.Customer.selectSummary
resultStatus=FAIL
errorType=BUSINESS
errorCode=SV-CUS-002
elapsedMs=98

시스템 오류:

event=TCF\_TRANSACTION\_COMPLETED
guid=G-20260718-000003
serviceId=SV.Customer.selectSummary
resultStatus=FAIL
errorType=SYSTEM
errorCode=TCF-SYS-001
elapsedMs=367

## 16.2.8 현재 샘플의 System.out

현재 일부 샘플 소스에 System.out.println()이 있을 수 있다.

이는 다음 용도로만 제한한다.

교육

로컬 디버깅

단기 진단

운영에서는 구조화 Logging을 사용한다.

금지:

System.out.println(
"request=" + request
);

문제:

민감정보 노출

로그 Level 제어 불가

JSON 검색 어려움

수집 시스템 연계 어려움

비동기 출력 순서 불명확

## 16.2.9 표준 요청 예

대표 요청:

{
"header": {
"businessCode": "SV",
"serviceId": "SV.Customer.selectSummary",
"transactionCode": "SV-INQ-0001",
"channelId": "WEBTOP",
"clientRequestId": "LOCAL-20260718-0001"
},
"body": {
"customerNo": "C000001",
"baseDate": "2026-07-18",
"includeProducts": true
}
}

인증 환경에서는 HTTP Header도 필요하다.

Authorization: Bearer {access-token}
Content-Type: application/json
X-Trace-Id: {trace-id}

로컬 인증 우회 정책이 별도로 존재하더라도 운영 인증 구조와 동일한 경로를 검증하는 테스트를 반드시 수행한다.

## 16.2.10 표준 성공 응답

{
"header": {
"businessCode": "SV",
"serviceId": "SV.Customer.selectSummary",
"transactionCode": "SV-INQ-0001",
"guid": "G-20260718-000001",
"traceId": "T-20260718-000001"
},
"result": {
"resultStatus": "SUCCESS",
"resultCode": "0000",
"message": "정상 처리되었습니다."
},
"body": {
"customerNo": "C0\*\*\*\*01",
"customerName": "홍\*동",
"customerGrade": "VIP",
"activeProductCount": 3,
"dataBaseTime": "2026-07-18T07:00:00+09:00",
"masked": true
},
"error": null
}

## 16.2.11 HTTP 200과 업무 성공

HTTP 200
≠ 업무 성공

UI는 다음을 확인해야 한다.

result.resultStatus

result.resultCode

error.errorType

예:

{
"result": {
"resultStatus": "FAIL",
"resultCode": "SV-CUS-002"
}
}

HTTP 요청 자체는 정상적으로 전달됐지만 업무는 실패한 상태일 수 있다.

## 16.2.12 입력 오류 응답

{
"header": {
"serviceId": "SV.Customer.selectSummary",
"guid": "G-20260718-000004"
},
"result": {
"resultStatus": "FAIL",
"resultCode": "SV-CUS-001",
"message": "입력값을 확인해 주세요."
},
"error": {
"errorType": "VALIDATION",
"fieldErrors": \[
{
"field": "customerNo",
"code": "required",
"message": "고객번호를 입력해 주세요."
}
\]
}
}

## 16.2.13 시스템 오류 응답

{
"header": {
"serviceId": "SV.Customer.selectSummary",
"guid": "G-20260718-000005"
},
"result": {
"resultStatus": "FAIL",
"resultCode": "TCF-SYS-001",
"message": "처리 중 오류가 발생했습니다."
},
"body": null,
"error": {
"errorType": "SYSTEM",
"errorCode": "TCF-SYS-001"
}
}

포함 금지:

Stack Trace

SQL

DB 계정

서버 IP

파일 경로

Java 전체 클래스명

## 16.2.14 curl 로컬 호출

Windows PowerShell 예:

curl.exe \`
\-X POST \`
"http://localhost:8086/sv/online" \`
\-H "Content-Type: application/json" \`
\-H "Authorization: Bearer LOCAL\_TEST\_TOKEN" \`
\--data-binary "@customer-summary-request.json"

Linux·macOS 예:

curl -X POST \\
"http://localhost:8086/sv/online" \\
\-H "Content-Type: application/json" \\
\-H "Authorization: Bearer ${TOKEN}" \\
\--data-binary @customer-summary-request.json

## 16.2.15 직접 JSON 호출 예

curl -X POST \\
"http://localhost:8086/sv/online" \\
\-H "Content-Type: application/json" \\
\-d '{
"header": {
"businessCode": "SV",
"serviceId": "SV.Customer.selectSummary",
"transactionCode": "SV-INQ-0001",
"channelId": "LOCAL"
},
"body": {
"customerNo": "C000001",
"baseDate": "2026-07-18"
}
}'

Shell별 따옴표 처리 차이 때문에 요청 파일을 사용하는 방법이 더 안전할 수 있다.

## 16.2.16 로컬 Endpoint 확인

두 구조를 구분한다.

### 내장 Tomcat·Root Context

Controller Mapping
/{businessCode}/online

URL
http://localhost:8086/sv/online

### 외부 Tomcat·WAR Context

WAR Context
/sv

Controller Mapping
/online

URL
http://localhost:8080/sv/online

최종 URL이 같더라도 구성 원리는 다를 수 있으므로 다음을 확인한다.

server.servlet.context-path

Controller RequestMapping

WAR 파일명

Tomcat Context 설정

Apache·Gateway Route

## 16.2.17 Breakpoint 위치

OnlineTransactionController

TCF.process()

STF.preProcess()

OnlineTransactionTimeoutExecutor

TransactionDispatcher.dispatch()

SvCustomerHandler.handle()

SvCustomerFacade.selectCustomerSummary()

SvCustomerService.selectCustomerSummary()

SvCustomerDao.findCustomerSummary()

SvCustomerMapper.selectCustomerSummary()

ETF.success() 또는 오류 처리

모든 메서드에 Breakpoint를 설정하지 않는다.

거래 경계와 책임 전환 지점에 설정한다.

## 16.2.18 Thread 전환 확인

Timeout Executor가 별도 Thread에서 Handler 이하를 실행할 수 있다.

확인:

Controller Thread

Timeout Executor Thread

MDC 전달

TransactionContext 전달

Security Context 전달

요청 종료 후 Context clear

Breakpoint에서 오래 멈추면 인위적인 Timeout이 발생할 수 있다.

디버깅 Timeout과 실제 업무 Timeout을 구분한다.

## 16.2.19 로그 추적 절차

1\. 응답에서 GUID 확인
2\. GUID로 전체 로그 검색
3\. 거래 시작 로그 확인
4\. STF 완료 여부 확인
5\. Handler Dispatch 확인
6\. Facade Transaction 확인
7\. Mapper Statement 확인
8\. ETF 종료 확인
9\. 전체 처리시간 비교
10\. 거래로그 최종 상태 확인

# 16.3 정상·경계·실패 테스트

## 16.3.1 테스트 기준

거래 테스트는 구현된 메서드 호출 횟수보다 계약과 업무 결과를 검증한다.

요청 계약

공통 통제

업무 규칙

데이터 결과

오류 의미

트랜잭션

Timeout

운영 추적

원본 제16장도 정상 사례만이 아니라 경계값·권한·동시성·의존 시스템 실패를 검증하고, 요구사항 ID와 테스트를 연결하도록 정의한다.

## 16.3.2 테스트 계층

| 계층 | 검증 대상 |
| --- | --- |
| DTO | 필수·길이·형식 |
| Rule | 권한·상태·업무조건 |
| Service | 처리순서·결과 없음 |
| Mapper | SQL·Result Mapping |
| Facade | Transaction·Rollback |
| Handler | ServiceId 분기 |
| TCF 통합 | STF·Dispatcher·ETF |
| 운영 | Catalog·통제·Timeout |
| End-to-End | 화면·로그·DB |

## 16.3.3 정상 테스트

### 정상 고객조회

입력:

유효한 Token
정상 ServiceId
정상 고객번호
조회권한
거래 사용 상태
정상 DB

기대:

Handler 실행

Facade Transaction 시작·종료

고객 1건 조회

권한별 마스킹

SUCCESS 응답

거래로그 SUCCESS

GUID 전체 추적

## 16.3.4 경계 테스트

대표 경계:

고객번호 최소길이

고객번호 최대길이

기준일 오늘

선택 필드 미전송

상품 0건

등급 없음

목록 최대 반환건수

응답시간 Timeout 직전

경계값은 정상과 실패의 경계가 정책대로 작동하는지 검증한다.

## 16.3.5 입력 오류 테스트

| 시나리오 | 기대 |
| --- | --- |
| 고객번호 누락 | Validation 오류 |
| 고객번호 공백 | Validation 오류 |
| 고객번호 길이 초과 | Validation 오류 |
| 날짜 형식 오류 | 타입 오류 |
| 미래 기준일 | 업무·Validation 오류 |
| Body 없음 | Body 필수 오류 |
| Header 없음 | STF 오류 |

확인:

Handler 이하 실행 여부

DB 호출 여부

오류코드

Field Error

개인정보 노출

## 16.3.6 업무 오류 테스트

| 시나리오 | 기대 |
| --- | --- |
| 고객 미존재 | SV-CUS-002 |
| 비활성 고객 | 승인된 업무 오류 |
| 기능권한 없음 | 권한 오류 |
| 데이터권한 없음 | 권한·미존재 정책 |
| 조회 불가 상태 | 업무 거절 |
| 기준일 정책 위반 | 업무 오류 |

업무 오류는 시스템 장애처럼 알림을 발생시키지 않을 수 있지만 발생 추세를 Metric으로 확인해야 한다.

## 16.3.7 미등록 ServiceId 테스트

요청:

SV.Customer.unknown

기대:

STF 공통검증 통과 가능
↓
Dispatcher Registry 조회
↓
Handler 없음
↓
SERVICE\_NOT\_FOUND
↓
Facade·Service·DB 미실행

확인:

오류코드

GUID

Handler 미실행 로그

거래로그 종료

## 16.3.8 중복 ServiceId 테스트

Spring Context Test에서 두 Handler가 같은 ServiceId를 선언하도록 구성한다.

기대:

Context 기동 실패

Duplicate serviceId 메시지

어떤 Handler끼리 충돌했는지 확인 가능

운영 호출 단계까지 중복 문제가 전달돼서는 안 된다.

## 16.3.9 거래통제 테스트

OM 또는 Test Policy에서 거래를 중지한다.

ServiceId 사용중지
↓
요청 호출
↓
STF 거래통제
↓
Handler 미실행
↓
CONTROL 오류

확인:

DB 호출 없음

통제 사유

정책 적용시각

운영 변경 감사로그

## 16.3.10 Timeout 테스트

방법:

Test SQL 지연

DB Lock

Test Client 지연

테스트용 낮은 Timeout

기대:

TCF Timeout 오류

Transaction Rollback

Connection 반환

Thread·Context 정리

후속 정상 거래 성공

Timeout Executor는 STF 이후 Dispatcher 이하 업무 실행구간에 제한시간을 적용하고, 완료·취소 후 Context를 정리하는 책임을 갖는다.

## 16.3.11 DB 오류 테스트

대표:

잘못된 SQL

DB Connection 실패

Query Timeout

Unique Constraint

Data Type 오류

기대:

업무 미존재 오류로 변환하지 않음

시스템 오류 응답

원인 예외 로그 보존

사용자에게 SQL 미노출

Transaction 종료

## 16.3.12 권한 테스트

| 시나리오 | 기대 |
| --- | --- |
| Token 없음 | 인증 오류 |
| Token 만료 | 인증 오류 |
| Header 사용자 불일치 | 거래 차단 |
| 기능권한 없음 | Handler 전·업무 초기에 차단 |
| 타 지점 고객 | 데이터권한 정책 |
| 일반권한 | 마스킹 |
| 원문조회 권한 | 승인 범위만 표시 |

## 16.3.13 로그 완결성 테스트

한 거래에 다음 로그가 모두 존재해야 한다.

STARTED

STF\_COMPLETED

HANDLER\_DISPATCHED

MAPPER\_EXECUTED

ETF\_COMPLETED

실패 거래는 실패 위치 이후의 성공 로그가 없어야 하지만, 최종 종료 상태는 반드시 기록되어야 한다.

거래 시작 있음
\+ 최종 종료 없음
\= 로그 완결성 결함

## 16.3.14 DB 결과 검증

조회 거래:

응답 필드
↔ Mapper Result
↔ 실제 DB Row

변경 거래:

응답 성공
↔ 영향 행 수
↔ DB 최종 상태
↔ 감사로그

화면 응답만 보고 데이터 처리를 확정하지 않는다.

## 16.3.15 재현성 테스트

다른 개발자가 다음 정보만으로 동일 결과를 재현할 수 있어야 한다.

Branch·Commit

JDK·Gradle

Profile

실행 명령

Port

요청 JSON

Test Data

기대 결과

GUID 검색방법

## 16.3.16 회귀 테스트

신규 ServiceId 추가로 다음이 영향을 받지 않는지 확인한다.

기존 Handler Registry

기존 ServiceId

공통 STF

공통 ETF

업무 WAR 기동시간

Mapper Scan

OM Catalog

권한 정책

거래로그

# 16.4 첫 거래 완료 체크리스트

## 16.4.1 완료의 정의

첫 거래가 완료됐다는 것은 다음 항목이 연결된 상태를 의미한다.

요구사항

화면 이벤트

ServiceId

거래코드

Handler

Facade

Service·Rule

DAO·Mapper

SQL·DB

OM 기준정보

테스트

로그·감사

배포·롤백

원본은 첫 거래의 완료 기준을 코드·설정·로그·데이터로 증명하고, 정상·경계·실패 테스트와 운영 추적을 함께 확인하도록 정의한다.

## 16.4.2 Definition of Ready

개발·통합 호출 전 조건:

| 영역 | 준비 조건 |
| --- | --- |
| 요구사항 | 승인 완료 |
| ServiceId | 명명·중복 확인 |
| 거래코드 | 채번 완료 |
| DTO | 요청·응답 확정 |
| 오류 | 오류코드 확정 |
| 권한 | 기능·데이터권한 확정 |
| DB | Table·SQL·Test Data 준비 |
| Timeout | 계층별 예산 확정 |
| 감사 | 대상 여부 확정 |
| 미결사항 | Blocker 없음 |

## 16.4.3 코드 등록 체크

| 확인 항목 | 완료 |
| --- | --- |
| ServiceId 상수가 정의됐다. | □ |
| Handler의 serviceIds()에 등록했다. | □ |
| Handler가 Spring Bean이다. | □ |
| Handler가 올바른 Component Scan 범위에 있다. | □ |
| Handler가 Facade만 호출한다. | □ |
| ServiceId와 Facade Method가 연결된다. | □ |
| ServiceId 중복 Context Test가 성공한다. | □ |
| 미등록 ServiceId 오류가 표준화됐다. | □ |
| ServiceId 명명규칙을 준수한다. | □ |
| 업무코드와 WAR 소유권이 일치한다. | □ |

## 16.4.4 프로그램 구현 체크

| 확인 항목 | 완료 |
| --- | --- |
| Request DTO가 타입화됐다. | □ |
| 입력 Validation이 있다. | □ |
| Facade 트랜잭션이 정의됐다. | □ |
| Service 업무 흐름이 명확하다. | □ |
| Rule 업무 판단이 분리됐다. | □ |
| DAO와 Mapper가 분리됐다. | □ |
| Row와 Response가 분리됐다. | □ |
| 결과 없음 정책이 있다. | □ |
| Query Timeout이 있다. | □ |
| 개인정보 마스킹이 적용됐다. | □ |

## 16.4.5 OM 운영 등록 체크

| 확인 항목 | 완료 |
| --- | --- |
| Service Catalog에 등록됐다. | □ |
| 사용 상태가 올바르다. | □ |
| 거래코드가 등록됐다. | □ |
| 거래통제 정책이 있다. | □ |
| Timeout 정책이 있다. | □ |
| 기능권한이 등록됐다. | □ |
| 데이터권한 정책이 있다. | □ |
| 감사 대상 여부가 등록됐다. | □ |
| 오류코드·메시지가 등록됐다. | □ |
| 담당 조직·담당자가 등록됐다. | □ |
| 목표 응답시간이 등록됐다. | □ |
| 적용 버전이 기록됐다. | □ |

Handler 등록은 프로그램의 실행 가능성을 의미하고, OM Catalog 등록은 운영 통제 대상이라는 의미다. 코드와 운영 등록 중 하나라도 누락되면 완전한 거래로 볼 수 없다.

## 16.4.6 로컬 환경 체크

| 확인 항목 | 완료 |
| --- | --- |
| JDK 버전이 표준과 같다. | □ |
| Gradle JVM이 동일하다. | □ |
| 기준 Branch·Commit을 기록했다. | □ |
| local Profile을 사용했다. | □ |
| 예상 Port로 기동했다. | □ |
| Application Name을 확인했다. | □ |
| Datasource가 로컬용이다. | □ |
| Hikari Pool이 정상 시작됐다. | □ |
| Mapper XML이 로딩됐다. | □ |
| Handler 등록 수를 확인했다. | □ |
| 기동 Warning을 검토했다. | □ |
| Secret이 로그에 노출되지 않았다. | □ |

## 16.4.7 호출 체크

| 확인 항목 | 완료 |
| --- | --- |
| 실제 Endpoint를 확인했다. | □ |
| Content-Type을 설정했다. | □ |
| 인증 Token 정책을 확인했다. | □ |
| businessCode가 일치한다. | □ |
| ServiceId가 정확하다. | □ |
| 거래코드가 정확하다. | □ |
| channelId가 유효하다. | □ |
| Body가 계약과 일치한다. | □ |
| 요청 JSON을 증적으로 보존했다. | □ |
| 응답 JSON을 증적으로 보존했다. | □ |

## 16.4.8 정상 테스트 체크

정상 고객

정상 권한

정상 거래통제

정상 DB

정상 응답시간

증적:

HTTP 응답

StandardResponse

GUID

거래로그 SUCCESS

Mapper Statement 로그

DB 결과

## 16.4.9 경계 테스트 체크

최소·최대 길이

선택값 미전송

결과 0건

최대 목록

Timeout 직전

마스킹 경계

## 16.4.10 실패 테스트 체크

Header 누락

ServiceId 미등록

중복 ServiceId

입력 오류

업무 데이터 없음

권한 없음

거래 차단

DB 오류

Timeout

## 16.4.11 로그·추적 체크

| 확인 항목 | 완료 |
| --- | --- |
| 응답에 GUID가 있다. | □ |
| TraceId가 있다. | □ |
| 시작 로그가 있다. | □ |
| STF 완료 로그가 있다. | □ |
| Handler Dispatch 로그가 있다. | □ |
| Mapper Statement 로그가 있다. | □ |
| 종료 로그가 있다. | □ |
| 전체 처리시간을 확인했다. | □ |
| 실패 원인 예외가 보존됐다. | □ |
| 개인정보 원문이 없다. | □ |

## 16.4.12 데이터 체크

| 확인 항목 | 완료 |
| --- | --- |
| Test Data 조건을 기록했다. | □ |
| 조회 결과가 DB와 일치한다. | □ |
| 결과 없음 정책이 일치한다. | □ |
| 마스킹 전·후 값이 정책과 일치한다. | □ |
| 데이터 기준시각이 표시된다. | □ |
| 변경 거래라면 영향 행 수를 확인했다. | □ |
| Rollback 후 DB 상태를 확인했다. | □ |
| 감사로그가 생성됐다. | □ |

## 16.4.13 자동 품질 Gate

배포 전에 다음 집합을 비교한다.

A = Handler에 등록된 ServiceId

B = OM Service Catalog의 ServiceId

C = 거래통제 정책의 ServiceId

D = Timeout 정책의 ServiceId

E = 권한 등록의 ServiceId

F = 통합테스트의 ServiceId

정상 기준 예:

A = B = C = D = E = F

예외가 있다면 명시적 사유와 승인정보가 필요하다.

코드 Handler 목록, OM Catalog, 거래통제와 Timeout 정책 집합을 자동 비교하는 것은 TCF CI/CD 핵심 검증 항목이다.

## 16.4.14 배포 준비 체크

| 확인 항목 | 완료 |
| --- | --- |
| 전체 Build가 성공했다. | □ |
| 업무 WAR가 생성됐다. | □ |
| Mapper XML이 WAR에 포함됐다. | □ |
| 환경별 설정이 분리됐다. | □ |
| OM Seed·Script가 준비됐다. | □ |
| 배포 순서가 정의됐다. | □ |
| Smoke Test가 정의됐다. | □ |
| Rollback Artifact가 있다. | □ |
| DB 변경 Rollback이 있다. | □ |
| 운영 모니터링 검색조건이 있다. | □ |

## 16.4.15 최종 Definition of Done

설계 승인
\+ 코드 구현
\+ Handler 등록
\+ OM 등록
\+ 정상 테스트
\+ 경계 테스트
\+ 실패 테스트
\+ Timeout 테스트
\+ 로그 완결성
\+ DB 결과 검증
\+ 코드리뷰
\+ CI/CD Gate
\+ 배포·Rollback 준비

# 대표 로컬 실행 절차

## 1단계. Branch와 환경을 확인한다

git status
git branch --show-current
git log -1 --oneline
java -version
./gradlew --version

Windows:

git status
git branch --show-current
git log -1 --oneline
java -version
.\\gradlew.bat --version

## 2단계. 업무 모듈을 빌드한다

./gradlew :sv-service:clean \\
:sv-service:test \\
:sv-service:build

## 3단계. 로컬 Profile로 실행한다

./gradlew :sv-service:bootRun \\
\--args='--spring.profiles.active=local'

Windows:

.\\gradlew.bat :sv-service:bootRun \`
\--args="--spring.profiles.active=local"

## 4단계. 기동 로그를 확인한다

Active Profile

Application Name

Port

Context Path

Datasource

Hikari Pool

Mapper 수

Handler 수

ServiceId 수

Started 시간

Warning

## 5단계. Handler Registry를 확인한다

권장 로그:

event=HANDLER\_REGISTRY\_INITIALIZED
application=sv-service
handlerCount=4
serviceIdCount=12

개별 Handler:

event=HANDLER\_REGISTERED
handler=SvCustomerHandler
serviceIds=\[
SV.Customer.selectSummary,
SV.Customer.selectProducts,
SV.Customer.updateMemo
\]

## 6단계. Health Check를 수행한다

curl \\
"http://localhost:8086/actuator/health"

Health UP은 애플리케이션 기본 상태만 의미한다.

Health UP
≠ 거래 정상

## 7단계. 정상 거래를 호출한다

curl -X POST \\
"http://localhost:8086/sv/online" \\
\-H "Content-Type: application/json" \\
\--data-binary @customer-summary-request.json

## 8단계. 응답 결과를 확인한다

HTTP Status

resultStatus

resultCode

guid

traceId

body

elapsedTime

## 9단계. 로그를 추적한다

GUID 검색
↓
STARTED
↓
STF
↓
DISPATCH
↓
HANDLER
↓
FACADE
↓
MAPPER
↓
ETF
↓
COMPLETED

## 10단계. SQL과 DB를 확인한다

Mapper Namespace

Statement ID

SQL Type

처리시간

Row Count

실제 DB 결과

## 11단계. 오류를 재현한다

고객번호 누락

고객 미존재

미등록 ServiceId

거래 차단

DB 오류

Timeout

## 12단계. 완료 증적을 작성한다

Branch·Commit

실행환경

요청 JSON

응답 JSON

GUID

로그

SQL ID

DB 결과

테스트 결과

결함·조치

# 정상 처리 흐름

업무 WAR 기동
↓
Handler Bean 생성
↓
Dispatcher Registry 등록
↓
OM 정책 로딩
↓
표준 요청 수신
↓
STF 검증
↓
Timeout Executor 시작
↓
Handler Dispatch
↓
Facade Transaction
↓
Service·Rule
↓
DAO·Mapper
↓
Response DTO
↓
ETF 성공
↓
거래로그 SUCCESS

# 미등록 ServiceId 흐름

요청 수신
↓
Header 검증
↓
Dispatcher Registry 조회
↓
Handler 없음
↓
SERVICE\_NOT\_FOUND
↓
Facade·DB 미실행
↓
ETF 업무 오류
↓
거래로그 FAIL

# 거래통제 흐름

요청 수신
↓
STF
↓
OM 거래 사용 상태 확인
↓
중지 상태
↓
Handler 미실행
↓
CONTROL 오류
↓
거래로그 FAIL

# Timeout 흐름

STF 완료
↓
Timeout Executor
↓
Handler·Facade 실행
↓
Slow SQL·DB Lock
↓
제한시간 초과
↓
Transaction Rollback
↓
Context·Connection 정리
↓
ETF Timeout 응답
↓
거래로그 TIMEOUT

# 정상 예시

Branch
feature/REQ-SV-CUS-001

Commit
a12bc34

Module
sv-service

Profile
local

Port
8086

Endpoint
POST /sv/online

ServiceId
SV.Customer.selectSummary

거래코드
SV-INQ-0001

Handler
SvCustomerHandler

Facade
selectCustomerSummary()

Mapper
SvCustomerMapper.selectCustomerSummary

응답
SUCCESS

처리시간
142ms

증적
GUID·TraceId
Mapper Statement
DB Row
거래로그 SUCCESS

# 금지 예시

ServiceId를 화면마다 임의로 새로 만든다.

유사 ServiceId 이름을 보고 담당 Handler를 추측한다.

Handler Bean 등록 여부를 확인하지 않는다.

중복 ServiceId 뒤에 숫자를 붙여 기동만 성공시킨다.

Handler가 Mapper를 직접 호출한다.

Handler에서 @Transactional을 선언한다.

코드에 Handler만 추가하고 OM 등록을 생략한다.

Catalog만 등록하고 Handler를 구현하지 않는다.

거래통제와 Timeout을 기본값에만 의존한다.

Health UP만 보고 거래 완료로 판단한다.

TCF 우회 Controller로만 테스트한다.

HTTP 200을 업무 성공으로 판단한다.

Request·Response 전체를 System.out으로 출력한다.

JWT와 개인정보를 로그에 남긴다.

오류 발생 즉시 증거 없이 재기동한다.

미등록 ServiceId 테스트를 생략한다.

정상 테스트만 수행한다.

Timeout 후 DB Connection 반환을 확인하지 않는다.

응답만 확인하고 DB 결과를 확인하지 않는다.

수동 테스트 결과만 있고 자동 회귀테스트가 없다.

로컬 Port와 운영 Route를 혼동한다.

Breakpoint로 발생한 Timeout을 실제 성능 문제로 판단한다.

# 책임 경계와 RACI

| 활동 | 업무분석 | UI | 업무개발 | FW | OM·운영 | 보안 | QA | AA |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| ServiceId 설계 | C | C | R | C | C | I | I | A |
| 거래코드 설계 | R | I | C | C | R | C | I | A |
| Handler 구현 | I | I | R/A | C | I | I | C | C |
| Dispatcher 관리 | I | I | C | R/A | I | I | C | C |
| Catalog 등록 | C | I | C | C | R/A | C | I | C |
| 거래통제 | C | I | C | C | R | C | C | A |
| Timeout | C | I | R | C | R | I | C | A |
| 기능권한 | C | C | C | C | R | A/R | C | C |
| 감사정책 | C | I | C | C | R | A/R | C | C |
| 로컬 환경 | I | I | R | C | I | I | C | A/C |
| 거래 테스트 | C | C | R | C | C | C | A/R | C |
| 로그 표준 | I | I | C | R | R | C | C | A |
| 운영 전환 | C | I | C | C | R | C | R | A |

# 데이터 및 상태관리

## Handler Registry 상태

애플리케이션 기동 시 생성

기동 이후 읽기 전용

ServiceId 중복 금지

런타임 임의 변경 금지

## OM 정책 상태

DRAFT

APPROVED

ACTIVE

SUSPENDED

DEPRECATED

정책 변경에는 다음이 필요하다.

변경자

승인자

변경사유

적용시각

이전값

신규값

감사로그

## 거래 상태

RECEIVED

PROCESSING

SUCCESS

BUSINESS\_FAIL

SYSTEM\_FAIL

TIMEOUT

CONTROLLED

거래 시작 후 최종 상태가 없는 거래를 탐지해야 한다.

# 성능·용량·확장성

## Registry

Handler Registry는 기동 시 한 번 구성하고 실행 시 Map 조회로 Handler를 탐색한다.

Reflection 기반 동적 Method 탐색
→ 금지

ServiceId Map 조회
→ 권장

## 로그

모든 요청·응답 Body를 기록하면 다음 문제가 발생한다.

Disk I/O 증가

로그 저장비용 증가

개인정보 위험

GC 부하

검색 성능 저하

구조화된 식별자와 처리시간을 중심으로 기록한다.

## 로컬과 운영 차이

| 영역 | 로컬 | 운영 |
| --- | --- | --- |
| DB | H2·개발 DB | Oracle·운영 DB |
| 인증 | Test 정책 가능 | JWT 필수 |
| Catalog | Seed·Mock | OM 기준정보 |
| 로그 | Console 가능 | 중앙 수집 |
| 인스턴스 | 단일 | 다중 |
| Route | 직접 호출 | Gateway·Apache |
| Timeout | 개발값 | 성능시험 확정값 |

로컬 성공을 운영 성공으로 간주하지 않는다.

# 보안·개인정보·감사

| 영역 | 기준 |
| --- | --- |
| Token | 원문 로그 금지 |
| 사용자정보 | 인증 Context 우선 |
| 요청로그 | 민감 Body 제외 |
| 응답로그 | 개인정보 원문 제외 |
| 오류 | 내부 구조 미노출 |
| 권한 | ServiceId 기능권한 |
| 데이터권한 | Service·Rule 검증 |
| 감사 | 중요 조회·변경 기록 |
| OM 변경 | 관리자 감사로그 |
| Local 우회 | 운영 Profile 사용 금지 |
| Endpoint | 진단·Actuator 접근제한 |

# 운영·모니터링·장애 대응

## 운영 대시보드 주요 항목

ServiceId별 호출량

성공률

업무 오류율

시스템 오류율

Timeout율

평균·p95 응답시간

Slow SQL

거래 중지 상태

적용 App Version

## 거래 장애 확인 순서

응답 GUID
↓
거래로그 상태
↓
최종 성공 단계
↓
Handler 존재
↓
OM 정책
↓
Mapper SQL
↓
DB Pool·Lock
↓
애플리케이션 버전

## 기동 장애 확인 순서

Spring Context Error
↓
Duplicate ServiceId
↓
Bean Scan
↓
Mapper Namespace
↓
Datasource
↓
Port 충돌

# 자동검증 및 품질 Gate

## 1\. ServiceId 형식

정규식 예:

^\[A-Z\]{2,5}\\.\[A-Z\]\[A-Za-z0-9\]\*\\.\[a-z\]\[A-Za-z0-9\]\*$

정상:

SV.Customer.selectSummary

오류:

sv.customer.selectsummary

SV-Customer-select

SV.Customer.SelectSummary

SV.Customer.selectSummary.v2

## 2\. 중복 검사

전체 Handler Bean 수집
→ ServiceId 추출
→ 빈 값 검사
→ 중복 검사
→ 업무코드 검사

## 3\. Handler Bean 검사

Handler 클래스 존재

Spring Bean 존재

Profile 조건 확인

Component Scan 포함

Facade 의존성 정상

## 4\. OM 정합성 검사

Handler ServiceId
↔ Catalog
↔ 거래통제
↔ Timeout
↔ 권한
↔ 테스트

## 5\. 계층 검사

Handler → Mapper 금지

Handler → DAO 금지

Rule → Persistence 금지

@Transactional → Facade만 허용

## 6\. Smoke Gate

Health UP

대표 정상 거래

입력 오류 거래

미등록 ServiceId

거래통제

Timeout

거래로그 종료

## 7\. 로그 Gate

GUID 존재

ServiceId 존재

시작·종료 상태

처리시간

오류코드

민감정보 없음

## 8\. 거래로그 완결 Gate

STARTED 건수
\=
SUCCESS
\+ BUSINESS\_FAIL
\+ SYSTEM\_FAIL
\+ TIMEOUT
\+ CONTROLLED

미종료 거래가 있으면 원인 조사 후 배포한다.

# 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| REG-001 | 정상 Handler 등록 | Registry 포함 |
| REG-002 | Handler Bean 미등록 | 거래 미등록 |
| REG-003 | 빈 ServiceId | Context Gate 실패 |
| REG-004 | 잘못된 형식 | 품질 Gate 실패 |
| REG-005 | 중복 ServiceId | 기동 실패 |
| REG-006 | Handler·업무코드 불일치 | Gate 실패 |
| REG-007 | Handler O·Catalog O | 정상 |
| REG-008 | Handler O·Catalog X | 배포 차단 |
| REG-009 | Handler X·Catalog O | 배포 차단 |
| REG-010 | 거래통제 누락 | 배포 차단·기본정책 검토 |
| REG-011 | Timeout 누락 | 배포 차단·기본정책 검토 |
| REG-012 | 권한 누락 | 실행 차단 |
| REG-013 | 정상 고객조회 | SUCCESS |
| REG-014 | 고객번호 누락 | Validation 오류 |
| REG-015 | 고객 미존재 | 업무 오류 |
| REG-016 | ServiceId 오타 | SERVICE\_NOT\_FOUND |
| REG-017 | 거래 중지 | Handler 미실행 |
| REG-018 | Token 없음 | 인증 오류 |
| REG-019 | 기능권한 없음 | 권한 오류 |
| REG-020 | 타 지점 고객 | 데이터권한 오류 |
| REG-021 | 일반권한 | 마스킹 |
| REG-022 | Mapper SQL 오류 | 시스템 오류 |
| REG-023 | DB 연결 실패 | 시스템 오류 |
| REG-024 | Query Timeout | Timeout 오류 |
| REG-025 | 전체 TCF Timeout | Timeout·Rollback |
| REG-026 | Timeout 후 후속 거래 | 정상 실행 |
| REG-027 | GUID 로그 추적 | 전 경로 연결 |
| REG-028 | Mapper Statement 로그 | SQL 식별 |
| REG-029 | 거래 종료 로그 | 최종 상태 존재 |
| REG-030 | 개인정보 로그 검사 | 원문 없음 |
| REG-031 | HTTP 200·업무 실패 | UI가 FAIL 인식 |
| REG-032 | Local Profile | 로컬 설정 적용 |
| REG-033 | Prod Profile 로컬 실행 | 정책상 차단 |
| REG-034 | Port 충돌 | 기동 실패·원인 확인 |
| REG-035 | Mapper XML 누락 | Context·호출 실패 |
| REG-036 | 잘못된 Context Path | 404 |
| REG-037 | 응답과 DB 결과 | 일치 |
| REG-038 | 감사대상 조회 | 감사로그 생성 |
| REG-039 | 동일 Commit 재실행 | 동일 결과 |
| REG-040 | 기존 ServiceId 회귀 | 영향 없음 |
| REG-041 | App Version 로그 | 배포버전 식별 |
| REG-042 | System.out 민감값 | 품질 Gate 실패 |
| REG-043 | TCF 우회 Controller | 운영 Gate 실패 |
| REG-044 | Catalog 정책 변경 | 감사로그 생성 |
| REG-045 | Rollback Artifact | 복구 가능 |

# 변경·호환성·폐기 관리

## ServiceId 변경

ServiceId는 외부 계약이며 단순한 리팩터링 문자열이 아니다.

기존
SV.Customer.selectSummary

변경
SV.Customer.getSummary

영향:

UI

다른 WAR Client

OM Catalog

거래통제

Timeout

권한

감사

테스트

로그 검색조건

기존 ServiceId를 바로 변경하지 않고 신규 ServiceId를 추가한 뒤 점진적으로 전환한다.

## Handler 이동

ServiceId 소유 Handler를 변경할 때 확인한다.

도메인 소유권

Facade Transaction

Spring Bean 중복

구버전 Handler

배포 순서

OM 담당정보

테스트

이전·신규 Handler가 동시에 배포되면 중복 ServiceId로 기동이 실패할 수 있다.

## Request·Response 변경

호환 가능성이 높은 변경:

선택 필드 추가

선택 응답 필드 추가

비호환 가능성이 높은 변경:

필수 필드 추가

필드 삭제

타입 변경

의미 변경

오류코드 의미 변경

## Timeout 변경

Timeout 변경은 설정 변경처럼 보이지만 다음에 영향을 준다.

사용자 응답

DB 부하

외부 연계

재시도

Thread·Pool

장애 판단 기준

성능시험과 운영 승인 후 적용한다.

## ServiceId 폐기

신규 호출 금지
↓
소비자 조사
↓
대체 ServiceId 제공
↓
Deprecated 표시
↓
호출량 0 확인
↓
OM 비활성
↓
Handler 등록 제거
↓
코드·테스트·문서 제거

폐기 순서를 반대로 하면 구버전 화면이 SERVICE\_NOT\_FOUND를 받을 수 있다.

# 시사점

## 핵심 아키텍처 판단

첫째, ServiceId 등록은 Java 상수 한 줄을 추가하는 작업이 아니다.

코드 실행 등록
\+ 운영 정책 등록
\+ 테스트 등록
\+ 로그 추적 등록

이 함께 완료돼야 한다.

둘째, Handler Registry는 런타임 라우팅 테이블이므로 중복과 누락을 기동·배포 단계에서 차단해야 한다.

셋째, Handler 등록과 OM Catalog 등록은 서로 다른 책임이다.

Handler
\= 실행 가능성

OM
\= 운영 통제 가능성

넷째, 로컬 호출 완료는 HTTP 응답만 확인하는 것이 아니라 GUID·SQL·DB·거래로그를 함께 검증하는 것이다.

다섯째, 정상 거래보다 실패 거래가 운영 품질을 더 잘 보여 준다.

미등록 ServiceId

권한 오류

DB 오류

Timeout

이 안전하게 종료되고 추적돼야 한다.

## 주요 위험

| 위험 | 결과 |
| --- | --- |
| Handler Bean 누락 | 거래 미등록 |
| 중복 ServiceId | WAR 기동 실패 |
| UI ServiceId 오타 | 거래 실행 실패 |
| 코드만 등록 | 운영정책 누락 |
| OM만 등록 | 실행 프로그램 없음 |
| Timeout 불일치 | 하위 작업 잔존 |
| HTTP 200 오판 | 업무 실패 미처리 |
| 로그 식별자 누락 | 장애 추적 불가 |
| 요청 원문 로그 | 개인정보 유출 |
| TCF 우회 테스트 | 운영 흐름 미검증 |
| Health만 확인 | 실제 거래 결함 누락 |
| 정상만 테스트 | 장애 대응 미검증 |
| DB 결과 미확인 | 부분 처리 누락 |
| 거래로그 미종료 | 운영 통계 오류 |
| ServiceId 직접 변경 | 구 소비자 장애 |

## 우선 보완 과제

1.  Handler ServiceId 등록 목록을 구조화 로그로 출력한다.
2.  중복·빈 ServiceId Context Test를 필수화한다.
3.  코드·Catalog·통제·Timeout 집합을 CI에서 비교한다.
4.  업무코드와 ServiceId Prefix 정합성을 검사한다.
5.  거래별 기능권한·감사 등록을 자동 확인한다.
6.  로컬 표준 요청 JSON 템플릿을 제공한다.
7.  정상·미등록·통제·Timeout Smoke Test를 자동화한다.
8.  Mapper Statement ID와 ServiceId를 자동 연결한다.
9.  거래 시작·종료 로그 완결성을 자동 검증한다.
10.  System.out과 민감정보 로그를 정적 분석으로 차단한다.
11.  TCF 우회 Controller의 운영 Profile 활성화를 금지한다.
12.  App Version·Instance ID를 모든 거래로그에 포함한다.
13.  Timeout 후 Thread·Connection 정리를 검증한다.
14.  폐기 ServiceId 호출량을 OM에서 제공한다.
15.  테스트 결과를 요구사항·ServiceId와 연결한다.

## 중장기 발전 방향

수동 Handler 등록
↓
ServiceId Annotation·Registry 표준화
↓
코드–OM 자동 정합성 검사
↓
계약·권한·Timeout 자동 Seed 생성
↓
로컬 Smoke Test 자동화
↓
배포 후 ServiceId 자동 검증
↓
거래로그 기반 미사용·이상 거래 탐지
↓
ServiceId 전체 생명주기 자동관리

# 마무리말

거래를 등록하고 로컬에서 호출하는 과정은 다음 질문에 답하는 일이다.

이 거래의 ServiceId는 무엇인가?

어느 Handler가 소유하는가?

Handler는 Spring Bean으로 등록됐는가?

Dispatcher Registry에 포함됐는가?

중복 ServiceId는 없는가?

OM Catalog와 거래통제에 등록됐는가?

Timeout과 권한은 정의됐는가?

어느 URL과 Port로 호출하는가?

어떤 요청 전문을 보내는가?

어떤 표준 응답을 받는가?

GUID로 전체 로그를 추적할 수 있는가?

어떤 Mapper SQL이 실행됐는가?

DB 결과가 응답과 일치하는가?

정상·경계·실패가 모두 재현되는가?

무엇이 증명되면 첫 거래가 완료되는가?

제16장의 핵심 흐름은 다음과 같다.

ServiceId
↓
Handler 등록
↓
Dispatcher Registry
↓
OM 운영 등록
↓
로컬 기동
↓
표준 요청
↓
TCF 전체 실행
↓
표준 응답
↓
GUID·SQL·DB 검증
↓
정상·경계·실패 테스트
↓
첫 거래 완료

가장 중요한 원칙은 다음과 같다.

프로그램이 존재한다고
거래가 등록된 것은 아니다.

응답이 보인다고
거래가 완성된 것도 아니다.

소스·운영정책·테스트·로그·데이터가
같은 ServiceId로 연결될 때
비로소 운영 가능한 거래가 된다.
