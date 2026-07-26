<!-- source: ztcf-집필본/NSIGHT TCF Chapter 1- 학습 가이드.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제1부. NSIGHT TCF를 처음 만나다

## 도입 전 안내말

처음 NSIGHT TCF 프로젝트에 참여한 개발자는 대개 다음 질문부터 하게 된다.

어느 Controller를 수정해야 하지?

Service 클래스는 어디에 있지?

SQL은 어느 Mapper에 작성하지?

화면에서 보낸 요청은 어떤 프로그램으로 연결되지?

오류가 발생했는데 어느 로그부터 봐야 하지?

이 질문들은 모두 중요하다. 그러나 개별 클래스부터 찾기 시작하면 전체 구조를 이해하지 못한 채 기존 코드를 복사하게 될 가능성이 높다.

NSIGHT TCF를 처음 배울 때는 클래스 이름보다 먼저 **거래 한 건의 전체 이동 경로**를 이해해야 한다.

사용자가 화면에서 버튼을 누른다.
↓
표준 요청 전문이 만들어진다.
↓
인증된 요청이 업무 애플리케이션에 들어온다.
↓
TCF가 공통 검증과 거래통제를 수행한다.
↓
ServiceId를 보고 담당 Handler를 찾는다.
↓
업무 프로그램이 규칙을 판단하고 데이터를 처리한다.
↓
처리 결과가 표준 응답으로 변환된다.
↓
거래로그·감사로그·성능정보가 남는다.
↓
화면이 성공 또는 오류 결과를 표시한다.

NSIGHT TCF는 단순히 Java 프로그램을 실행하는 프레임워크가 아니다.

입력 형식을 통일하고,
거래 실행 순서를 통제하며,
업무 프로그램의 책임을 분리하고,
오류를 표준화하고,
운영에서 거래를 다시 찾을 수 있게 하는
온라인 거래 처리 기반이다.

이 부에서는 아직 CRUD 프로그램을 직접 만들지 않는다. 대신 뒤에서 만들 모든 프로그램이 어느 위치에 놓이고, 어떤 순서로 실행되며, 누가 어떤 책임을 갖는지를 먼저 배운다.

## 이 부의 목표

| 학습 영역 | 이 부를 마친 뒤 할 수 있어야 하는 것 |
| --- | --- |
| 구조 이해 | 화면 요청부터 DB 처리까지의 전체 경로를 설명한다. |
| 역할 이해 | Gateway, TCF, 업무 WAR, ServiceId의 역할을 구분한다. |
| 소스 탐색 | 요청을 처리하는 Controller, TCF, Handler를 소스에서 찾는다. |
| 책임 구분 | 공통 프레임워크와 업무 개발자의 책임을 구분한다. |
| 오류 이해 | 입력 오류, 업무 오류, 시스템 오류의 차이를 설명한다. |
| 추적 이해 | GUID, TraceId, ServiceId로 거래를 추적하는 이유를 설명한다. |
| 표준 이해 | 화면 ID, ServiceId, 거래코드가 왜 함께 관리되어야 하는지 설명한다. |

## 제1부 전체 학습 흐름

| 단계 | 제1장 | 제2장 | 제3장 | 제4장 |
| --- | --- | --- | --- | --- |
| 핵심 질문 | TCF가 왜 필요한가? | 거래는 어떻게 이동하는가? | 소스는 어떻게 나뉘는가? | 거래를 어떻게 식별하는가? |
| 주요 대상 | TCF의 존재 이유 | End-to-End 처리 흐름 | 모듈·계층·DTO | 업무코드·ServiceId·거래코드 |
| 소스 출발점 | OnlineTransactionController | TCF, STF, ETF | Gradle 모듈과 업무 패키지 | TransactionDispatcher |
| 학습 결과 | 공통 통제의 필요성 이해 | 성공·오류 흐름 이해 | 클래스 책임 구분 | 화면과 프로그램 연결 |
| 다음 단계 | 전체 흐름 추적 | 소스 구조 탐색 | 기존 거래 분석 | 개발환경 준비 |

### 그림으로 보는 제1부 학습 여정

\[왜 필요한가\]
↓
TCF가 해결하는 문제 이해
↓
\[어떻게 움직이는가\]
↓
한 거래의 전체 실행 경로 이해
↓
\[어디에 구현되어 있는가\]
↓
모듈·패키지·계층 구조 이해
↓
\[어떻게 서로 연결되는가\]
↓
화면 ID·ServiceId·거래코드 이해

# 제1장. TCF는 왜 필요한가

## 이 장을 시작하며

웹 프로그램은 TCF가 없어도 만들 수 있다.

Controller가 요청을 받고 Service를 호출한 뒤 Mapper로 SQL을 실행하면 조회·등록·수정·삭제 기능을 구현할 수 있다. 작은 프로그램이나 사용자 수가 적은 시스템에서는 이 구조만으로도 충분할 수 있다.

그러나 시스템과 개발조직이 커지면 단순한 기능 구현 외에 다음 문제가 발생한다.

| 발생 문제 | 개발 현장에서 나타나는 모습 |
| --- | --- |
| 요청 형식 불일치 | 화면마다 Header와 Body 구조가 다르다. |
| 인증 구현 중복 | 업무별 Controller에서 사용자 확인을 반복한다. |
| 권한 구현 중복 | 같은 권한 규칙이 여러 Service에 복사된다. |
| Timeout 불일치 | 어떤 거래는 3초, 어떤 거래는 무제한으로 실행된다. |
| 오류 형식 불일치 | 화면마다 성공·실패 판단 방식이 다르다. |
| 로그 추적 단절 | 화면 요청과 SQL 로그를 연결하기 어렵다. |
| 거래통제 부재 | 장애가 발생해도 특정 거래만 차단하기 어렵다. |
| 중복 요청 문제 | 사용자가 버튼을 여러 번 눌러 데이터가 중복 등록된다. |
| 계층 책임 혼합 | Controller에서 업무 판단과 SQL 호출까지 수행한다. |
| 운영 기준 불일치 | 개발자는 성공이라 하지만 운영자는 거래를 찾지 못한다. |

TCF는 이 문제들을 모든 업무 프로그램이 각자 해결하지 않도록 **공통 거래 처리 절차**를 제공한다.

## 핵심 관점

TCF의 목적은 업무를 대신 구현하는 것이 아니다.

업무 프로그램이
정해진 규칙과 순서 안에서
안전하고 추적 가능하게 실행되도록 통제하는 것이다.

## 학습 목표

이 장을 마치면 다음 내용을 자신의 말로 설명할 수 있어야 한다.

| 번호 | 학습 목표 |
| --- | --- |
| 1 | 일반 웹 애플리케이션과 TCF 방식의 차이를 설명한다. |
| 2 | TCF가 해결해야 하는 공통 문제를 다섯 가지 이상 설명한다. |
| 3 | tcf-web과 tcf-core의 역할을 구분한다. |
| 4 | 업무 개발자와 프레임워크 개발자의 책임을 구분한다. |
| 5 | 실제 소스에서 TCF의 시작점과 주요 처리 클래스를 찾는다. |
| 6 | 표준을 우회한 구현이 운영에 어떤 문제를 만드는지 설명한다. |
| 7 | 한 거래가 정상 처리되었다는 것을 로그와 결과로 증명한다. |

## 한눈에 보는 흐름

\[사용자 화면\]
│
│ 표준 요청
▼
\[Gateway 또는 업무 WAR 진입부\]
│
│ 인증된 사용자 문맥
▼
\[OnlineTransactionController\]
│
│ tcf.process(request)
▼
\[TCF\]
├─ STF : 사전 검증과 거래 준비
├─ Timeout Executor
├─ Dispatcher : ServiceId로 Handler 선택
└─ ETF : 응답·로그·감사·Metric 종료
│
▼
\[업무 Handler\]
▼
\[Facade → Service → Rule·DAO → Mapper\]
│
▼
\[DB·캐시·파일·외부 시스템\]

## 소스 기반 전체 책임 지도

| 처리 영역 | 실제 소스 위치 | 핵심 책임 |
| --- | --- | --- |
| HTTP 요청 수신 | tcf-web/.../OnlineTransactionController.java | /online 요청 수신과 TCF 호출 |
| 비표준 API의 TCF 연결 | tcf-web/.../TcfGateway.java | 파일·REST API도 동일한 TCF 파이프라인으로 위임 |
| 전체 실행 조정 | tcf-core/.../processor/TCF.java | STF → Dispatcher → ETF 실행 |
| 사전 처리 | tcf-core/.../processor/STF.java | 검증·인증·권한·통제·Timeout·중복·로그 시작 |
| 거래 분배 | tcf-core/.../dispatch/TransactionDispatcher.java | ServiceId에 해당하는 Handler 탐색 |
| 업무 진입 계약 | tcf-core/.../transaction/TransactionHandler.java | Handler의 공통 실행 계약 |
| 사후 처리 | tcf-core/.../processor/ETF.java | 정상·업무 실패·시스템 실패 응답 |
| 표준 요청 | tcf-core/.../message/StandardRequest.java | Header와 Body 구조 |
| 표준 응답 | tcf-core/.../message/StandardResponse.java | Header, Result, Body 구조 |
| 인증 필터 | tcf-web/.../TcfJwtAuthenticationFilter.java | Gateway가 없을 때 업무 WAR 진입 전 JWT 검증 |
| Timeout 실행 | tcf-core/.../OnlineTransactionTimeoutExecutor.java | 거래 실행 제한시간 적용 |

# 1.1 일반 웹 애플리케이션과 TCF 방식

## 일반 웹 애플리케이션의 기본 구조

일반적인 Spring 웹 애플리케이션은 다음처럼 구현할 수 있다.

화면
↓
CustomerController
↓
CustomerService
↓
CustomerRepository 또는 CustomerMapper
↓
DB

예를 들어 고객을 조회하는 API라면 URL을 기준으로 Controller 메서드가 선택된다.

POST /customers/search
↓
CustomerController.search()
↓
CustomerService.search()
↓
CustomerMapper.selectCustomers()

이 구조는 이해하기 쉽다. 기능과 URL의 대응도 명확하다.

그러나 업무가 늘어나면 Controller마다 다음 코드가 반복되기 시작한다.

사용자 확인
권한 확인
요청값 검증
거래 시작 로그
Timeout 확인
중복 요청 확인
예외 변환
감사로그 작성
성능 측정
응답 형식 생성

각 업무팀이 이 기능을 조금씩 다르게 구현하면 시스템 전체의 일관성이 무너진다.

## TCF 방식의 기본 구조

NSIGHT TCF에서는 업무별 Controller가 각 Service를 직접 호출하는 방식보다 다음 구조를 기본으로 한다.

화면
↓
공통 OnlineTransactionController
↓
TCF.process()
↓
ServiceId 기반 Dispatcher
↓
업무 Handler
↓
Facade
↓
Service
├─ Rule
├─ DAO → Mapper → DB
└─ 외부 시스템 Client

핵심 차이는 **URL이 아니라 ServiceId가 업무 거래를 식별한다는 점**이다.

예를 들어 화면에서 고객요약을 조회한다고 가정해 보자.

Endpoint
POST /sv/online

ServiceId
SV.Customer.selectSummary

거래코드
SV-INQ-0001

/sv/online이라는 URL은 여러 SV 업무 거래가 함께 사용할 수 있다. 실제로 어느 업무 프로그램을 실행할 것인지는 요청 Header의 serviceId가 결정한다.

## 두 방식의 상세 비교

| 비교 항목 | 일반 웹 방식 | NSIGHT TCF 방식 |
| --- | --- | --- |
| 업무 식별 기준 | URL과 Controller 메서드 | ServiceId |
| 공통 진입점 | 업무별 Controller | OnlineTransactionController |
| 업무 프로그램 선택 | Spring URL Mapping | TransactionDispatcher |
| 공통 검증 | Controller·Filter에 분산 | STF에서 표준화 |
| Timeout | API별 개별 구현 가능 | ServiceId 정책으로 통제 |
| 거래통제 | 별도 구현 필요 | 공통 거래통제 서비스 적용 |
| 중복 요청 | 업무별 구현 | Idempotency 공통 처리 |
| 오류 응답 | Controller별 차이 가능 | ETF와 표준 응답 사용 |
| 거래 추적 | URL·로그 조합 | GUID·TraceId·ServiceId |
| 운영 차단 | URL·서버 차단 중심 | 거래 단위 통제 가능 |
| 감사 | 업무별 구현 | 공통 감사 처리와 연결 |
| 프로그램 구조 | 팀별 차이 가능 | Handler 이하 표준 계층 |
| 변경 영향 분석 | URL과 코드 검색 | 화면·ServiceId·Handler·SQL 연결 |

## 어느 방식이 더 좋은가

TCF 방식이 모든 상황에서 무조건 우월한 것은 아니다.

| 상황 | 권장 판단 |
| --- | --- |
| 단순한 소규모 내부 도구 | 일반 Controller 방식으로도 충분할 수 있다. |
| 거래 종류가 적고 운영통제가 단순함 | 과도한 프레임워크 도입을 피할 수 있다. |
| 다수 업무팀이 공동 개발 | TCF 방식의 표준화 가치가 커진다. |
| 거래통제·감사·추적이 중요함 | TCF 방식이 적합하다. |
| 동일한 오류·로그·Timeout 정책 필요 | 공통 TCF 적용이 유리하다. |
| 업무별 독립 URL이 필수인 외부 API | URL API를 유지하되 TcfGateway로 TCF에 위임할 수 있다. |
| 대용량 파일·Streaming | 일반 온라인 전문과 별도 진입점을 사용하되 공통 통제 연계를 검토한다. |

현재 소스의 TcfGateway는 일반 /online 전문이 아닌 REST·파일 API도 내부적으로 StandardRequest를 생성하여 tcf.process()를 호출할 수 있게 한다.

즉, TCF는 모든 API의 URL을 하나로 만들기 위한 기술이 아니다.

진입 형식은 다를 수 있지만,
공통 통제가 필요한 거래는
동일한 TCF 실행 파이프라인을 거치게 하는 것이 핵심이다.

## 그림형 비교표

| 처리 단계 | 일반 웹 방식 | TCF 방식 | TCF 적용 효과 |
| --- | --- | --- | --- |
| 1\. 요청 수신 | 업무 Controller | 공통 Controller 또는 TCF Gateway | 진입 기준 통일 |
| 2\. 사용자 확인 | Controller·Filter | JWT Filter와 STF | 검증 책임 분리 |
| 3\. 거래 식별 | URL·메서드 | ServiceId | 논리 거래 식별 |
| 4\. 실행 허용 | 개별 구현 | 거래통제 정책 | 장애 거래 차단 |
| 5\. 제한시간 | 개별 Timeout | ServiceId별 정책 | 무제한 실행 방지 |
| 6\. 업무 선택 | 메서드 직접 연결 | Dispatcher → Handler | 업무 등록 표준화 |
| 7\. 오류 처리 | 예외 Handler별 처리 | ETF에서 표준화 | 화면 처리 일관성 |
| 8\. 운영 기록 | 로그 구현에 의존 | 거래·감사·Metric 공통 종료 | 추적성 향상 |

## 초보자가 실제 소스에서 확인할 것

### ① 공통 진입 URL

OnlineTransactionController에는 다음 두 진입점이 있다.

| URL | 용도 |
| --- | --- |
| POST /online | Context Path에서 바로 온라인 거래 호출 |
| POST /{businessCode}/online | URL 경로로 업무코드를 함께 전달 |

Controller는 요청 Header가 없으면 빈 Header를 만들고, Header의 업무코드가 비어 있으면 URL의 businessCode를 보완한다.

또한 X-Forwarded-For가 있으면 첫 번째 IP를 사용하고, 없으면 직접 연결된 원격 IP를 clientIp로 설정한다.

마지막으로 업무 Service를 직접 호출하지 않고 다음 한 줄로 TCF에 위임한다.

StandardResponse<Object> response = tcf.process(request);

### ② URL과 ServiceId의 차이

URL
\= 어느 시스템·업무 WAR로 들어갈 것인가

ServiceId
\= 그 WAR 안에서 어느 업무 거래를 실행할 것인가

이 둘을 혼동하면 URL을 거래 식별자로 사용하거나, ServiceId 없이 Controller 메서드만 늘리는 구조가 만들어진다.

# 1.2 프레임워크가 해결하는 문제

## TCF가 처리하는 실제 공통 기능

현재 소스의 STF.preProcess()는 업무 Handler가 실행되기 전에 다음 순서로 공통 처리를 수행한다.

표준 Header 검증
↓
GUID·TraceId 생성
↓
TransactionContext 생성
↓
인증 문맥 정합성 검증
↓
권한 검증
↓
거래통제 확인
↓
Timeout 정책 결정
↓
중복 요청 확인
↓
거래로그 시작

이 중 하나라도 실패하면 Dispatcher와 업무 Handler가 실행되어서는 안 된다.

## 문제 1. 요청 형식이 업무마다 달라지는 문제

TCF는 StandardRequest<T>를 사용한다.

StandardRequest
├─ header
└─ body

Header에는 다음과 같은 공통 정보가 포함된다.

| Header 항목 | 의미 | 주요 사용처 |
| --- | --- | --- |
| systemId | 호출 시스템 | 시스템 식별·통계 |
| businessCode | 업무코드 | WAR·업무 영역 식별 |
| serviceId | 업무 거래 식별자 | Dispatcher·로그·OM |
| serviceName | 거래 표시명 | 운영 화면·문서 |
| transactionCode | 통제·감사 거래코드 | 거래통제·통계 |
| processingType | 조회·변경 등 처리유형 | 권한·감사·정책 |
| guid | 전 구간 거래 추적 ID | 시스템 간 추적 |
| traceId | 내부 호출 추적 ID | 로그 상관분석 |
| channelId | WEBTOP·배치·연계 등 채널 | 채널 정책 |
| userId | 사용자 식별 | 인증·감사 |
| branchId | 영업점·조직 식별 | 데이터권한 |
| centerId | 센터 식별 | 운영·DR 추적 |
| requestTime | 요청 시각 | 지연·재처리 판단 |
| clientIp | 클라이언트 IP | 보안·감사 |
| idempotencyKey | 중복요청 식별 | 중복 등록 방지 |

StandardHeader.normalize()는 systemId 기본값과 요청 시각을 보완하고, 업무코드와 처리유형을 대문자로 정규화한다.

## 문제 2. 거래를 나중에 찾을 수 없는 문제

일반 로그에 다음 내용만 있다고 가정해 보자.

고객조회 시작
SQL 실행
고객조회 종료

동시에 수백 건의 고객조회가 실행되면 어느 시작 로그와 어느 SQL 로그가 같은 요청인지 판단하기 어렵다.

TCF는 STF에서 guid와 traceId를 생성하고 MDC에 다음 정보를 넣는다.

guid
traceId
serviceId
userId
branchId

이를 이용하면 다음과 같이 한 거래를 연결할 수 있다.

화면 요청
GUID=G-20260718-000001
↓
TCF 시작
GUID=G-20260718-000001
↓
Handler 실행
GUID=G-20260718-000001
↓
Mapper SQL
GUID=G-20260718-000001
↓
ETF 종료
GUID=G-20260718-000001

### GUID와 TraceId의 역할

| 식별자 | 주요 목적 | 사용 예 |
| --- | --- | --- |
| GUID | 한 거래를 시스템 간 연결 | UI → Gateway → SV → IC |
| TraceId | 내부 호출·세부 실행 추적 | Facade → Service → DAO |
| ServiceId | 무슨 거래인지 식별 | SV.Customer.selectSummary |
| 거래코드 | 통제·감사·통계 분류 | SV-INQ-0001 |

프로젝트에서는 GUID와 TraceId의 정확한 범위를 표준으로 확정해야 한다. 중요한 것은 이름 자체가 아니라 모든 로그와 연계 호출이 같은 규칙으로 전달하는 것이다.

## 문제 3. 장애가 발생한 거래만 차단하기 어려운 문제

특정 거래의 SQL이 느려 전체 DB Pool을 고갈시킨다고 가정해 보자.

거래통제 기능이 없으면 운영자는 다음 방법을 사용할 가능성이 높다.

전체 서버 재기동
전체 업무 WAR 중지
DB 계정 잠금
화면 메뉴 임시 제거

이 방법들은 문제가 없는 거래까지 중단한다.

TCF의 거래통제는 다음 단위로 정책을 적용할 수 있도록 설계된다.

| 통제 단위 | 예시 |
| --- | --- |
| ServiceId | 특정 고객조회 거래 차단 |
| 거래코드 | 특정 변경성 거래 중단 |
| 업무코드 | SV 업무 전체 통제 |
| 채널 | WEBTOP 채널만 차단 |
| 사용자·조직 | 특정 권한 또는 지점 제한 |
| 운영시간 | 점검시간 실행 제한 |

STF는 Dispatcher보다 먼저 transactionControlService.check(header)를 실행한다. 따라서 거래가 차단되면 Handler와 DB 처리는 시작되지 않는다.

## 문제 4. 느린 거래가 자원을 계속 점유하는 문제

업무 프로그램이 DB 또는 외부 시스템 응답을 오랫동안 기다리면 다음 자원을 점유할 수 있다.

Tomcat Thread
DB Connection
JVM 메모리
외부 연계 Connection
사용자 화면 대기시간

TCF는 TimeoutPolicyService로 정책을 결정하고, OnlineTransactionTimeoutExecutor 안에서 Dispatcher를 실행한다.

STF
↓ Timeout 정책 결정
Timeout Executor
↓ 제한시간 안에서 실행
Dispatcher
↓
Handler와 업무 프로그램

Timeout은 단순히 화면에 오류를 보여주는 기능이 아니다.

| Timeout의 목적 | 설명 |
| --- | --- |
| 자원 보호 | 무기한 Thread 점유 방지 |
| 장애 격리 | 느린 외부 시스템의 영향 축소 |
| 사용자 경험 | 일정 시간 안에 명확한 결과 반환 |
| 운영 판단 | ServiceId별 지연 현황 측정 |
| 정책 통제 | 중요도에 따라 제한시간 차등 적용 |

단, Java Thread Timeout이 DB 작업을 항상 즉시 중단시키는 것은 아니다. MyBatis Query Timeout, JDBC Driver, DBMS 정책, 외부 Client Timeout과 함께 계층적으로 설계해야 한다.

## 문제 5. 동일 요청이 중복 처리되는 문제

사용자가 등록 버튼을 여러 번 누르거나 네트워크 재시도로 같은 요청이 반복되면 데이터가 중복 등록될 수 있다.

STF는 IdempotencyChecker를 사용하여 처리 시작 상태를 표시한다.

ETF는 결과에 따라 다음 상태로 변경한다.

| 처리 결과 | ETF 동작 |
| --- | --- |
| 성공 | markSuccess() |
| 업무 오류 | markFail() |
| 시스템 오류 | markFail() |

중복 요청 방지는 모든 조회 거래에 동일하게 적용할 필요는 없다. 등록·결제·파일 생성·외부 전송처럼 재실행 시 부작용이 있는 거래를 중심으로 적용한다.

## 문제 6. 성공과 실패 응답이 제각각인 문제

StandardResponse<T>는 다음 구조를 가진다.

StandardResponse
├─ header
├─ result
└─ body

ETF는 처리 결과를 세 가지 경로로 구분한다.

| 처리 경로 | 발생 조건 | 응답 처리 |
| --- | --- | --- |
| success() | 업무 처리 정상 완료 | 성공 Result와 Body 반환 |
| businessFail() | 예상 가능한 업무 거절 | 업무 오류코드와 메시지 반환 |
| systemError() | 예상하지 못한 시스템 예외 | 표준 시스템 오류 반환 |

### 성공 흐름

업무 Handler 정상 종료
↓
Idempotency 성공 처리
↓
거래로그 정상 종료
↓
감사로그 기록
↓
Metric 기록
↓
StandardResponse.success()

### 업무 오류 흐름

BusinessException 발생
↓
Idempotency 실패 처리
↓
거래로그 업무 실패 종료
↓
감사로그·Metric 기록
↓
업무 오류코드 반환

### 시스템 오류 흐름

예상하지 못한 Exception 발생
↓
서버 로그에 원인 예외 기록
↓
Idempotency 실패 처리
↓
거래로그 시스템 실패 종료
↓
안전한 공통 오류 메시지 반환

## 프레임워크가 해결하는 문제 종합표

| 문제 | TCF 구성요소 | 개발자가 얻게 되는 효과 | 운영자가 얻게 되는 효과 |
| --- | --- | --- | --- |
| 요청 형식 불일치 | StandardRequest·Header | DTO 구조 통일 | 거래 정보 일관성 |
| 거래 식별 어려움 | ServiceId | 프로그램 연결 명확화 | 거래 단위 조회 |
| 로그 연결 어려움 | GUID·TraceId·MDC | 디버깅 용이 | End-to-End 추적 |
| 인증정보 불일치 | JWT Filter·인증 검증 | 업무 코드에서 인증 분리 | 보안 정책 통일 |
| 무권한 실행 | AuthorizationValidator | 권한 코드 중복 제거 | 접근통제 증적 |
| 장애 거래 확산 | 거래통제 | 업무 코드 수정 없이 통제 | 특정 거래 차단 |
| 무제한 실행 | Timeout 정책 | 제한시간 표준화 | 자원 보호 |
| 중복 등록 | Idempotency | 중복 방지 구현 공통화 | 재처리 판단 |
| 오류 형식 불일치 | ETF·StandardResponse | 예외 처리 단순화 | 장애 분류 |
| 운영 증적 부족 | 거래·감사·Metric | 구현 완료 증명 | 모니터링·감사 |

# 1.3 업무 개발자와 공통 프레임워크의 책임

TCF 프로젝트에서 가장 중요한 설계 원칙 중 하나는 **공통 기능과 업무 기능의 책임을 섞지 않는 것**이다.

프레임워크가 업무를 너무 많이 알면 업무 변경 때마다 공통 모듈을 수정해야 한다. 반대로 업무 프로그램이 공통 기능을 직접 구현하면 모든 업무가 서로 다른 방식으로 동작한다.

## 책임 경계 그림

┌───────────────────────────────────────────────┐
│ 공통 프레임워크 책임 │
│ │
│ 요청 수신·표준 전문·인증 문맥·권한 기반 │
│ 거래통제·Timeout·중복요청·거래로그 │
│ ServiceId 분배·오류 표준화·감사·Metric │
└──────────────────────┬────────────────────────┘
│ 표준 계약
▼
┌───────────────────────────────────────────────┐
│ 업무 애플리케이션 책임 │
│ │
│ 고객·상품·캠페인 등 업무 규칙 │
│ 조회·등록·변경·삭제 유스케이스 │
│ 업무 데이터 검증·트랜잭션·SQL·외부 업무 연계 │
└───────────────────────────────────────────────┘

## 구성요소별 책임

| 구성요소 | 책임 | 해서는 안 되는 일 |
| --- | --- | --- |
| JWT Filter | Token 검증과 인증 문맥 생성 | 고객 상태·상품 규칙 판단 |
| Controller | 요청 수신과 TCF 위임 | Mapper 직접 호출 |
| TCF | 전체 실행 순서 통제 | 업무 테이블 조회 |
| STF | 공통 사전 검증 | 고객 등급 계산 |
| Dispatcher | ServiceId로 Handler 탐색 | 업무 처리 순서 결정 |
| Handler | 거래 분기와 Facade 호출 | SQL 작성·복잡한 업무 판단 |
| Facade | 유스케이스 조립·트랜잭션 경계 | HTTP 응답 직접 생성 |
| Service | 업무 처리 흐름 | JWT 원문 파싱 |
| Rule | 부작용 없는 업무규칙 판단 | DB 변경·외부 호출 |
| DAO | 데이터 접근 추상화 | 화면 메시지 생성 |
| Mapper | SQL 실행 | 업무 프로세스 제어 |
| ETF | 표준 응답과 거래 종료 | 업무 성공 여부 임의 판단 |
| OM | 운영 기준정보 관리 | 개별 업무 SQL 구현 |

## 업무 개발자가 구현해야 하는 것

| 영역 | 업무 개발자 책임 |
| --- | --- |
| 요구사항 | 화면 기능을 업무 거래로 정의 |
| ServiceId | 표준 명명규칙에 따라 거래 식별 |
| DTO | 요청·응답·조회조건·결과 데이터 정의 |
| Handler | 담당 ServiceId 등록과 Facade 호출 |
| Facade | 유스케이스 조립과 트랜잭션 경계 |
| Service | 업무 처리 순서 구현 |
| Rule | 업무 상태·중복·자격·조건 검증 |
| DAO·Mapper | 데이터 조회·등록·변경·삭제 |
| 외부 연계 | 승인된 Client 계약으로 호출 |
| 업무 오류 | 표준 오류코드로 BusinessException 발생 |
| 테스트 | 정상·경계·업무 오류·DB 오류 검증 |
| 운영 등록 | ServiceId·거래코드·Timeout 등 기준정보 확인 |

## 업무 개발자가 직접 구현하지 않아야 하는 것

Handler에서 JWT 문자열 파싱
Service에서 GUID 생성
DAO에서 사용자 권한 판단
Mapper에서 거래로그 기록
업무 코드에서 전체 Timeout Executor 생성
Controller마다 다른 오류 JSON 생성
각 업무가 자체 거래통제 테이블 조회
System.out으로 개인정보 전체 출력

이러한 코드는 당장은 동작할 수 있다. 그러나 공통 정책 변경 시 모든 업무 소스를 수정해야 하고, 운영 기준도 통일할 수 없다.

## 공통 프레임워크 개발자의 책임

| 영역 | 프레임워크팀 책임 |
| --- | --- |
| 표준 전문 | Header·Request·Response 구조 제공 |
| 실행 엔진 | TCF·STF·ETF와 실행 순서 유지 |
| Dispatcher | Handler 등록과 중복 ServiceId 차단 |
| Timeout | 정책 조회·적용·예외 변환 |
| 거래통제 | ServiceId 기반 실행 허용 여부 확인 |
| 인증 연계 | 인증 문맥과 Header 정합성 검증 |
| 로그 | GUID·TraceId·ServiceId 기반 공통 로그 |
| 감사·Metric | 거래 종료 시 공통 기록 |
| 자동설정 | 업무 WAR가 공통 기능을 쉽게 적용하도록 구성 |
| 호환성 | 공통 모듈 변경 시 기존 업무 영향 관리 |
| 품질 Gate | 표준 우회와 계층 위반 검출 지원 |

프레임워크팀도 업무 규칙을 직접 구현해서는 안 된다.

금지 예:
STF에서 고객 등급을 확인한다.
TCF에서 캠페인 참여 가능 여부를 판단한다.
ETF에서 상품 유형별 메시지를 만든다.
Dispatcher가 업무 데이터 상태에 따라 Handler를 바꾼다.

## RACI 책임표

| 활동 | 아키텍처 | 프레임워크팀 | 업무개발팀 | 운영·OM | 보안 |
| --- | --- | --- | --- | --- | --- |
| TCF 실행 순서 정의 | A | R | C | C | C |
| ServiceId 표준 | A | R | R | C | I |
| Handler 구현 | C | C | R/A | I | I |
| 거래통제 기능 | A | R | C | R | C |
| 거래통제 정책값 | C | C | C | R/A | C |
| Timeout 엔진 | A | R | C | C | I |
| ServiceId별 Timeout 값 | C | C | R | A | I |
| 업무 규칙 | C | I | R/A | I | C |
| SQL·DB 처리 | C | I | R | C | I |
| JWT 검증 구조 | A | R | I | C | R/A |
| 감사 기준 | C | R | C | R | A |
| 장애 분석 | C | C | R | R/A | I |

R은 실행 책임, A는 최종 승인 책임, C는 협의, I는 결과 공유를 의미한다.

# 1.4 초보자가 기억할 세 가지 원칙

## 원칙 1. URL보다 ServiceId를 먼저 본다

초보 개발자는 화면 오류가 발생하면 먼저 브라우저의 URL을 확인한다. URL은 어느 애플리케이션으로 들어갔는지 알려주지만, 실제 어떤 업무 거래가 실행되었는지는 ServiceId가 알려준다.

URL
/sv/online

ServiceId
SV.Customer.selectSummary

같은 /sv/online URL로 다음 거래들이 호출될 수 있다.

SV.Customer.selectSummary
SV.Customer.selectGrade
SV.Customer.selectProducts
SV.Customer.updateMemo

따라서 소스를 추적할 때는 다음 순서가 좋다.

요청 JSON의 serviceId 확인
↓
TransactionDispatcher 등록정보 확인
↓
담당 Handler 확인
↓
Facade·Service·DAO·Mapper 추적

### ServiceId 추적표

| 확인 대상 | 확인할 내용 |
| --- | --- |
| 화면·요청 | 어떤 ServiceId를 전송했는가 |
| Dispatcher | 해당 ServiceId가 등록되었는가 |
| Handler | serviceIds()에 포함되어 있는가 |
| OM Catalog | 운영 대상 거래로 등록되었는가 |
| 로그 | 같은 ServiceId가 시작·종료 로그에 있는가 |
| Timeout | 거래별 정책이 적용되었는가 |
| 테스트 | ServiceId 기준 테스트가 존재하는가 |

현재 TransactionHandler는 두 가지 등록 방식을 지원한다.

단일 거래 Handler
→ serviceId() 한 개 등록

도메인 단위 Handler
→ serviceIds()로 여러 거래 등록

TransactionDispatcher는 기동할 때 전체 Handler의 ServiceId를 등록한다. 중복 ServiceId가 발견되면 IllegalStateException을 발생시켜 서버 기동 단계에서 오류를 드러낸다.

이는 매우 중요한 품질 통제다.

중복 ServiceId를 운영 중 임의로 선택하지 않고,
애플리케이션 시작 단계에서 실패시킨다.

## 원칙 2. 어느 계층이 책임지는 문제인지 먼저 구분한다

오류가 발생했다고 무조건 Service 코드를 수정해서는 안 된다.

| 증상 | 우선 확인할 책임 영역 |
| --- | --- |
| Bearer Token 없음 | JWT Filter·보안 설정 |
| Header 필수값 누락 | StandardHeaderValidator |
| 사용자가 다름 | 인증 문맥 정합성 검증 |
| 권한 없음 | AuthorizationValidator |
| 거래 중지 상태 | TransactionControlService |
| 제한시간 초과 | Timeout 정책·SQL·외부 연계 |
| ServiceId 없음 | 요청 Header·Dispatcher |
| Handler 미등록 | Handler Bean·serviceIds() |
| 고객 상태 부적합 | 업무 Service·Rule |
| SQL 문법 오류 | DAO·Mapper |
| 응답 형식 오류 | ETF·StandardResponse |
| 거래로그 누락 | STF·ETF·로그 설정 |

### 문제 분류 흐름

요청이 업무 Handler까지 도달했는가?
│
├─ 아니오
│ └─ 인증·Header·권한·통제·Timeout 준비 단계 확인
│
└─ 예
↓
Handler가 선택되었는가?
│
├─ 아니오
│ └─ ServiceId와 Dispatcher 등록 확인
│
└─ 예
↓
업무 오류인가?
│
├─ 예 → Service·Rule·데이터 상태 확인
└─ 아니오 → DAO·SQL·외부 시스템·인프라 확인

이 원칙을 지키면 원인과 무관한 코드를 수정하는 실수를 줄일 수 있다.

## 원칙 3. 구현 완료는 코드가 아니라 증적으로 판단한다

코드를 작성했고 화면에서 한 번 성공했다고 개발이 끝난 것은 아니다.

완료를 판단하려면 다음 증적이 필요하다.

| 완료 항목 | 필요한 증적 |
| --- | --- |
| 요청 정의 | 화면 항목과 Request DTO 매핑 |
| 거래 식별 | 화면 ID·이벤트 ID·ServiceId·거래코드 |
| 프로그램 연결 | Handler·Facade·Service 호출 경로 |
| 데이터 처리 | Mapper ID·SQL·테이블 |
| 정상 결과 | 표준 성공 응답과 결과 데이터 |
| 업무 오류 | 오류코드와 안전한 사용자 메시지 |
| 시스템 오류 | 원인 로그와 표준 시스템 응답 |
| 거래 추적 | GUID·TraceId로 연결된 로그 |
| Timeout | 설정값과 초과 시 결과 |
| 거래통제 | 중지 정책 적용 결과 |
| 데이터 정합성 | Commit·Rollback과 영향 행 수 |
| 테스트 | 정상·경계·실패 시나리오 결과 |

“제 PC에서는 됩니다.”
\= 완료 증적이 아니다.

“이 ServiceId의 요청·응답·로그·DB 결과와
오류 테스트 결과가 재현됩니다.”
\= 완료 증적이다.

# 실제 소스로 따라가는 TCF 처리 절차

## 1단계. 요청이 Controller에 들어온다

OnlineTransactionController는 요청을 StandardRequest<Map<String, Object>>로 받는다.

| Controller 처리 | 설명 |
| --- | --- |
| Header 확인 | Header가 없으면 빈 Header 생성 |
| 업무코드 보완 | URL의 업무코드를 Header에 반영 |
| Client IP 보완 | X-Forwarded-For 또는 Remote IP 사용 |
| TCF 호출 | tcf.process(request) 실행 |
| 응답 반환 | StandardResponse<Object> 반환 |

Controller는 업무 내용을 판단하지 않는다.

## 2단계. TCF가 전체 실행 순서를 통제한다

TCF.process()의 실제 처리 순서는 다음과 같다.

클라이언트 요청 Header 복사
↓
STF.preProcess()
↓
Timeout Executor
↓
TransactionDispatcher.dispatch()
↓
ETF.success()

오류가 발생하면 다음처럼 분기한다.

BusinessException
→ ETF.businessFail()

Timeout 관련 예외
→ 업무 오류 형태로 변환
→ ETF.businessFail()

그 밖의 Exception
→ ETF.systemError()

마지막에는 성공·실패와 관계없이 다음 Thread Local 정보를 정리한다.

TransactionContextHolder
AuthenticationContextHolder
TimeoutContextHolder
MDC

이 정리 과정이 누락되면 Tomcat Thread가 재사용될 때 이전 사용자의 거래 문맥이 다음 요청에 남을 수 있다.

## 3단계. STF가 업무 실행 가능 여부를 확인한다

| 순서 | STF 처리 | 실패하면 |
| --- | --- | --- |
| 1 | 표준 Header 검증 | Handler 미실행 |
| 2 | GUID·TraceId 생성 | 추적정보 생성 실패 |
| 3 | TransactionContext 생성 | 거래 문맥 없음 |
| 4 | 인증 문맥 검증 | 인증 오류 |
| 5 | 권한 검증 | 접근 거절 |
| 6 | 거래통제 확인 | 거래 실행 중지 |
| 7 | Timeout 정책 적용 | 정책 오류 또는 기본값 적용 |
| 8 | 중복 요청 확인 | 중복 처리 거절 |
| 9 | 거래로그 시작 | 운영 추적 문제 |

STF가 성공해야 Dispatcher가 실행된다.

## 4단계. Dispatcher가 Handler를 찾는다

TransactionDispatcher는 애플리케이션 기동 시 Spring이 관리하는 모든 TransactionHandler를 받는다.

Handler 목록 수집
↓
각 Handler의 serviceIds() 조회
↓
serviceId → Handler Map 생성
↓
중복 ServiceId 검사

거래 실행 시에는 다음 순서로 동작한다.

요청 Header의 serviceId 조회
↓
값이 없으면 INVALID\_HEADER
↓
Map에서 Handler 탐색
↓
없으면 SERVICE\_NOT\_FOUND
↓
handler.handle()

## 5단계. ETF가 거래를 종료한다

| 구분 | 성공 | 업무 오류 | 시스템 오류 |
| --- | --- | --- | --- |
| Idempotency | 성공 표시 | 실패 표시 | 실패 표시 |
| 거래로그 | 정상 종료 | 업무 실패 종료 | 시스템 실패 종료 |
| 감사로그 | 기록 | 기록 | 기록 |
| Metric | 성공 코드 | 실패 코드 | 실패 코드 |
| 사용자 메시지 | 정상 결과 | 업무 메시지 | 공통 안전 메시지 |
| 원인 예외 | 해당 없음 | 업무 오류코드 | 서버 로그에 상세 기록 |

# 정상 처리 예시

## 요청

{
"header": {
"systemId": "NSIGHT-MP",
"businessCode": "SV",
"serviceId": "SV.Customer.selectSummary",
"transactionCode": "SV-INQ-0001",
"processingType": "INQUIRY",
"channelId": "WEBTOP",
"userId": "U10001",
"branchId": "001"
},
"body": {
"customerNo": "C000001"
}
}

## 예상 처리 흐름

| 순서 | 구성요소 | 확인 내용 |
| --- | --- | --- |
| 1 | Controller | 요청 수신, Client IP 설정 |
| 2 | STF | Header와 인증 문맥 검증 |
| 3 | STF | GUID·TraceId 생성 |
| 4 | STF | 거래통제와 Timeout 확인 |
| 5 | Dispatcher | SV.Customer.selectSummary 탐색 |
| 6 | Handler | 고객 도메인 거래 분기 |
| 7 | Service | 고객 조회 업무 수행 |
| 8 | Mapper | 고객요약 SQL 실행 |
| 9 | ETF | 거래로그·감사·Metric 종료 |
| 10 | 화면 | 성공 결과 표시 |

## 정상 판정 기준

요청의 ServiceId와 Handler 등록값이 같다.
GUID가 전체 로그에서 동일하다.
업무 결과가 예상 데이터와 같다.
거래로그가 성공으로 종료된다.
시스템 오류가 남지 않는다.

# 경계 사례

| 경계 조건 | 기대 결과 |
| --- | --- |
| Header는 있으나 serviceId가 없음 | Header 오류로 Handler 실행 안 함 |
| 존재하지 않는 ServiceId | SERVICE\_NOT\_FOUND 업무 오류 |
| GUID가 없음 | STF가 생성 |
| 인증 사용자와 Header 사용자가 다름 | 인증 문맥 검증 실패 |
| 거래가 운영에서 중지됨 | Handler 실행 전 거래 거절 |
| 동일 Idempotency Key 재요청 | 정책에 따라 중복 거절 또는 기존 결과 |
| Timeout 제한시간 초과 | 표준 Timeout 오류 |
| 고객번호 형식은 맞지만 고객이 없음 | 업무 오류 또는 빈 결과 정책 적용 |

# 금지 예시

## 금지 1. 업무별 Controller에서 Service 직접 호출

@PostMapping("/customer/search")
public CustomerResponse search(@RequestBody CustomerRequest request) {
return customerService.search(request);
}

### 문제점

| 문제 | 영향 |
| --- | --- |
| TCF 우회 | 거래통제·Timeout 미적용 |
| STF 우회 | 표준 검증·인증 문맥 확인 누락 |
| Dispatcher 우회 | ServiceId 추적 불가 |
| ETF 우회 | 응답·거래로그·감사 불일치 |
| 운영 영향 | OM에서 거래를 통제하기 어려움 |

비표준 URL이 필요하다면 TcfGateway를 통해 TCF에 위임하는 방식을 검토한다.

## 금지 2. Handler에서 SQL 직접 실행

public Object doHandle(...) {
return customerMapper.selectCustomer(...);
}

Handler는 ServiceId 분기와 Facade 호출에 집중해야 한다. SQL을 직접 실행하면 업무 흐름·트랜잭션·데이터 접근 책임이 섞인다.

## 금지 3. 모든 예외를 시스템 오류로 변환

catch (Exception e) {
throw new RuntimeException("처리 실패");
}

고객 미존재, 중복 등록, 상태 부적합과 같은 업무 조건은 업무 오류로 구분해야 한다. 모든 오류를 시스템 오류로 처리하면 사용자는 다시 시도해야 할지 입력을 수정해야 할지 알 수 없다.

## 금지 4. 민감정보를 전체 출력

현재 학습용·분석용 소스에는 처리 단계를 쉽게 확인하기 위한 상세 콘솔 출력이 존재한다. 운영 적용 시에는 다음 정보를 그대로 출력하지 않도록 보완해야 한다.

비밀번호
JWT 원문
주민등록번호
계좌번호 전체
고객 요청 Body 전체
개인정보가 포함된 SQL Parameter

운영 로그는 구조화된 Logger와 마스킹 기준을 사용해야 한다.

# 오류·Timeout·장애 흐름

## 오류 분류표

| 오류 분류 | 대표 사례 | 사용자 행동 | 운영 행동 |
| --- | --- | --- | --- |
| 입력 오류 | 필수값 없음·형식 오류 | 입력 수정 | 반복 발생 화면 확인 |
| 인증 오류 | Token 없음·만료 | 재인증 | 인증 서버·JWKS 확인 |
| 권한 오류 | 기능·데이터 권한 없음 | 권한 요청 | 권한정책 확인 |
| 거래통제 | 운영 중지 거래 | 이후 재시도 | 통제 사유와 해제 확인 |
| 업무 오류 | 고객 없음·중복·상태 부적합 | 조건 수정 | 업무 데이터 확인 |
| Timeout | SQL·외부연계 지연 | 재시도 여부 안내 | Thread·Pool·SQL 분석 |
| 시스템 오류 | NullPointer·DB 장애 | 상관관계 ID 전달 | 원인 예외와 영향 분석 |

## Timeout 원인 추적 흐름

Timeout 발생
↓
어느 ServiceId인가?
↓
현재 실행 단계는 어디인가?
├─ DB Connection 획득 전
│ → Hikari Pool Pending 확인
├─ Mapper SQL 실행 중
│ → SQL ID·DB Lock·실행계획 확인
├─ 외부 연계 중
│ → 대상 시스템과 Client Timeout 확인
└─ 업무 연산 중
→ CPU·반복문·대량 객체 확인

# 따라 하는 실무 절차

## 실습 목표

소스에서 TCF의 주요 처리 경로를 직접 찾고, 각 클래스의 책임을 한 줄로 정리한다.

| 순서 | 수행 작업 | 완료 증적 |
| --- | --- | --- |
| 1 | OnlineTransactionController를 찾는다. | /online Mapping 위치 |
| 2 | tcf.process() 호출 위치를 찾는다. | Controller 호출 코드 |
| 3 | TCF.process()의 실행 순서를 기록한다. | STF·Dispatcher·ETF 순서 |
| 4 | STF.preProcess() 항목을 기록한다. | 검증·통제 목록 |
| 5 | Dispatcher의 Handler 등록 방식을 찾는다. | serviceIds()와 Map |
| 6 | ETF의 세 가지 종료 경로를 찾는다. | success·businessFail·systemError |
| 7 | StandardHeader 필드를 분류한다. | 식별·인증·추적·통제 필드표 |
| 8 | 업무 Handler 한 개를 찾아 ServiceId를 확인한다. | ServiceId와 클래스 경로 |
| 9 | 테스트 또는 요청 샘플을 실행한다. | 요청·응답·로그 |
| 10 | GUID로 전체 로그를 추적한다. | 시작부터 종료까지 로그 |

# 자동검증 및 품질 Gate

| 검증 항목 | 자동검증 방법 | 실패 시 판단 |
| --- | --- | --- |
| ServiceId 중복 | Dispatcher 기동 검사 | 애플리케이션 기동 실패 |
| ServiceId 누락 | 단위·통합 테스트 | 배포 차단 |
| Handler 미등록 | ApplicationContext 테스트 | 배포 차단 |
| 표준 Header 누락 | 요청 Validation 테스트 | 거래 실행 차단 |
| 계층 위반 | ArchUnit | CI 실패 |
| 업무 WAR 간 직접 참조 | Gradle·ArchUnit | CI 실패 |
| Mapper Namespace 불일치 | MyBatis 기동 테스트 | 빌드·기동 실패 |
| Timeout 미등록 | 기본 정책·정책 검사 | 경고 또는 Gate 실패 |
| OM Catalog 미등록 | 소스–Catalog 대조 | 운영 반영 금지 |
| 민감정보 로그 | 정적 분석·로그 검사 | 보안 Gate 실패 |

# 테스트 시나리오

| ID | 시나리오 | 입력 조건 | 기대 결과 |
| --- | --- | --- | --- |
| TCF-CH01-01 | 정상 거래 | 유효 Header와 ServiceId | 성공 응답·거래로그 정상 종료 |
| TCF-CH01-02 | Header 없음 | header=null | Controller 보완 후 Validator 정책 적용 |
| TCF-CH01-03 | ServiceId 없음 | 빈 ServiceId | Handler 미실행·표준 오류 |
| TCF-CH01-04 | 미등록 ServiceId | 존재하지 않는 값 | SERVICE\_NOT\_FOUND |
| TCF-CH01-05 | 중복 ServiceId | 두 Handler가 같은 값 선언 | 기동 실패 |
| TCF-CH01-06 | 인증 불일치 | JWT 사용자와 Header 사용자 다름 | 인증 오류 |
| TCF-CH01-07 | 거래 중지 | 통제 정책 비활성 | Handler 미실행 |
| TCF-CH01-08 | 업무 오류 | 존재하지 않는 고객 | 업무 오류 응답 |
| TCF-CH01-09 | 시스템 오류 | Mapper 예외 유도 | 공통 시스템 오류·원인 로그 |
| TCF-CH01-10 | Timeout | 제한시간보다 긴 처리 | Timeout 표준 오류 |
| TCF-CH01-11 | 중복 요청 | 동일 Idempotency Key | 정책에 따른 중복 차단 |
| TCF-CH01-12 | Thread 문맥 정리 | 연속된 서로 다른 사용자 요청 | 이전 사용자 문맥 잔존 없음 |

# 완료 체크리스트

## 이해 체크

| 확인 항목 | 완료 |
| --- | --- |
| 일반 웹 방식과 TCF 방식의 차이를 설명할 수 있다. | □ |
| URL과 ServiceId의 차이를 설명할 수 있다. | □ |
| STF·Dispatcher·ETF의 역할을 구분할 수 있다. | □ |
| 업무 오류와 시스템 오류의 차이를 설명할 수 있다. | □ |
| 업무팀과 프레임워크팀의 책임을 구분할 수 있다. | □ |

## 소스 체크

| 확인 항목 | 완료 |
| --- | --- |
| OnlineTransactionController를 직접 열어 보았다. | □ |
| TCF.process()의 실행 순서를 확인했다. | □ |
| STF.preProcess()의 공통 검증 순서를 확인했다. | □ |
| TransactionDispatcher의 ServiceId 등록 방식을 확인했다. | □ |
| ETF의 성공·업무 실패·시스템 실패 흐름을 확인했다. | □ |
| StandardHeader의 주요 필드를 분류했다. | □ |

## 실행 체크

| 확인 항목 | 완료 |
| --- | --- |
| 정상 요청 한 건을 실행했다. | □ |
| 미등록 ServiceId 요청을 실행했다. | □ |
| 업무 오류와 시스템 오류 응답을 비교했다. | □ |
| GUID 또는 TraceId로 로그를 연결했다. | □ |
| Handler 실행 전 실패하는 사례를 확인했다. | □ |

# 제1장의 핵심 정리

첫째,
TCF는 업무를 대신 구현하는 프레임워크가 아니다.
업무 거래가 실행되는 방법을 통제하는 프레임워크다.

둘째,
화면 요청과 업무 프로그램을 연결하는 핵심 식별자는
URL이 아니라 ServiceId다.

셋째,
STF가 공통 사전 검증을 수행하고,
Dispatcher가 Handler를 선택하며,
ETF가 응답과 운영 기록을 종료한다.

넷째,
업무 개발자는 업무규칙과 데이터 처리를 구현하고,
프레임워크는 인증·통제·Timeout·로그·오류 표준을 제공한다.

다섯째,
개발 완료는 코드 작성 여부가 아니라
요청·응답·로그·DB 결과와 오류 테스트 증적으로 판단한다.

# 시사점

## 핵심 아키텍처 판단

NSIGHT TCF에서 가장 중요한 구조적 판단은 모든 기능을 공통 모듈에 넣는 것이 아니다. 여러 업무가 반드시 같은 방식으로 처리해야 하는 기능만 TCF가 통제하고, 고객·상품·캠페인과 같은 업무 판단은 업무 애플리케이션에 남겨야 한다.

## 주요 위험

| 위험 | 결과 |
| --- | --- |
| Controller에서 TCF 우회 | 거래통제·Timeout·감사 누락 |
| ServiceId 관리 부실 | 잘못된 Handler 실행 또는 미등록 오류 |
| 공통·업무 책임 혼합 | 변경 영향 확대 |
| 로그 문맥 정리 누락 | 사용자 정보가 다른 요청에 잔존 |
| 예외를 하나로 통합 | 사용자 조치와 운영 대응 불가능 |
| 민감정보 과다 로그 | 개인정보·보안 사고 |
| 정책과 소스 불일치 | 개발 성공·운영 실패 |

## 우선 보완 과제

현재 소스의 상세 System.out 출력은 학습과 흐름 확인에는 유용하다. 운영 적용 전에는 구조화된 Logger, 민감정보 마스킹, 로그 레벨, 비동기 로그, 거래량에 따른 출력 통제가 필요하다.

또한 ServiceId·Handler·OM Catalog·Timeout 정책·거래통제 정책을 자동 대조하는 품질 Gate를 구축해야 한다.

## 중장기 발전 방향

소스 내부 표준화
↓
ServiceId·Handler 자동검증
↓
OM 기준정보 자동 동기화
↓
거래·SQL·외부연계 통합 추적
↓
장애 원인 자동 분석
↓
운영 데이터 기반 정책 최적화

# 마무리말

TCF를 처음 접하면 클래스와 용어가 많아 복잡하게 느껴진다.

그러나 이 장에서 기억해야 할 핵심은 단순하다.

화면에서 요청이 들어오면
TCF가 공통 규칙을 확인하고,
ServiceId로 업무 프로그램을 찾고,
업무 처리 결과를 표준 응답과 운영 기록으로 남긴다.

다음 장에서는 이 구조를 한 단계 더 구체화하여, **화면에서 시작된 거래 한 건이 Gateway, TCF, Handler, Service, Mapper, DB를 지나 다시 화면으로 돌아오는 전체 여행**을 따라간다.
