<!-- source: ztcf-집필본/NSIGHT TCF Chapter 2- Transaction Journey.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제2장. 거래 한 건의 전체 여행

## 이 장을 시작하며

제1장에서는 TCF가 왜 필요한지 살펴보았다.

이제부터는 사용자가 화면에서 조회 버튼을 한 번 누른 순간부터 결과가 다시 화면에 표시될 때까지, 거래 한 건이 실제로 어떤 구성요소를 지나가는지 따라가 본다.

초보 개발자는 흔히 다음과 같이 생각한다.

화면에서 조회 버튼을 누른다.
→ Service가 실행된다.
→ SQL이 실행된다.
→ 결과가 화면으로 돌아온다.

큰 방향은 맞다. 하지만 실제 NSIGHT TCF의 거래는 이보다 훨씬 많은 경계를 통과한다.

화면 이벤트
→ 표준 요청 전문 생성
→ 접근 경로 결정
→ JWT 인증
→ Gateway 라우팅
→ 업무 WAR 진입
→ 공통 Controller
→ TCF
→ STF 사전 처리
→ Timeout 실행영역
→ Dispatcher
→ Handler
→ Facade
→ Service
→ Rule
→ DAO
→ Mapper
→ DB
→ ETF 사후 처리
→ 표준 응답
→ 화면 결과 처리

이 과정은 단순히 복잡하게 만들기 위해 존재하는 것이 아니다.

각 단계는 서로 다른 책임을 수행한다.

| 단계 | 해결하는 문제 |
| --- | --- |
| 화면 | 사용자가 원하는 행위를 정의 |
| Gateway | 외부 요청의 인증과 목적지 결정 |
| 업무 WAR | 업무 애플리케이션의 실행 경계 |
| TCF | 공통 거래 처리 순서 통제 |
| STF | 업무 실행 전 검증과 준비 |
| Dispatcher | ServiceId에 맞는 Handler 선택 |
| Handler 이하 | 실제 업무 처리 |
| ETF | 결과·오류·로그·감사 종료 |
| 화면 응답 처리 | 사용자의 다음 행동 결정 |

이 장의 핵심은 클래스 이름을 외우는 것이 아니다.

거래가 어느 경계를 지나는가?

각 경계는 무엇을 책임지는가?

어디까지 성공했는가?

어느 단계에서 실패했는가?

그 사실을 어떤 로그와 데이터로 증명할 수 있는가?

이 질문에 답할 수 있어야 한다.

## 핵심 관점

운영 가능한 시스템은
성공 경로만 정의하지 않는다.

실패가 어디에서 발생했고,
어디까지 처리되었으며,
무엇을 되돌려야 하고,
누가 복구해야 하는지도 함께 정의한다.

## 학습 목표

이 장을 마치면 다음을 수행할 수 있어야 한다.

| 번호 | 학습 목표 |
| --- | --- |
| 1 | 화면 이벤트부터 DB까지의 전체 처리 경로를 설명한다. |
| 2 | Gateway, TCF, 업무 WAR의 책임을 구분한다. |
| 3 | ServiceId가 Handler와 업무 프로그램을 연결하는 방식을 설명한다. |
| 4 | 정상·업무 오류·시스템 오류의 처리 차이를 설명한다. |
| 5 | Timeout이 어느 구간에서 적용되는지 설명한다. |
| 6 | 요청 Header의 주요 식별정보가 어디에서 사용되는지 설명한다. |
| 7 | GUID와 TraceId로 한 거래의 로그를 연결한다. |
| 8 | 장애 발생 시 현재 처리 단계를 판단한다. |
| 9 | 직접 Controller 호출과 TCF 경유 호출의 차이를 설명한다. |
| 10 | 거래 완료 여부를 응답·로그·DB 결과로 증명한다. |

# 한눈에 보는 전체 흐름

┌─────────────────────────────────────────────────────────────┐
│ 1. 사용자·화면 │
│ 조회 버튼 클릭, 검색조건 입력 │
└──────────────────────────┬──────────────────────────────────┘
│ StandardRequest + JWT
▼
┌─────────────────────────────────────────────────────────────┐
│ 2. Gateway 또는 업무 WAR JWT Filter │
│ Token 검증, 사용자 식별, 업무 목적지 결정 │
└──────────────────────────┬──────────────────────────────────┘
│ 인증 문맥 + 표준 요청
▼
┌─────────────────────────────────────────────────────────────┐
│ 3. OnlineTransactionController │
│ Header 보완, Client IP 확인, TCF.process() 호출 │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 4. STF │
│ Header·인증·권한·거래통제·Timeout·멱등성·거래로그 시작 │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 5. Timeout Executor │
│ 제한시간 안에서 업무 거래 실행 │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 6. TransactionDispatcher │
│ ServiceId → TransactionHandler │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 7. 업무 프로그램 │
│ Handler → Facade → Service → Rule·DAO → Mapper │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 8. DB·Cache·파일·외부 시스템 │
│ 조회·등록·변경·삭제·연계 │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 9. ETF │
│ 성공·업무 오류·시스템 오류, 로그·감사·Metric 종료 │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 10. 표준 응답·화면 │
│ Result 확인, 데이터 표시, 사용자 메시지 처리 │
└─────────────────────────────────────────────────────────────┘

NSIGHT TCF의 기준 실행 흐름은 OnlineTransactionController → TCF → STF → TimeoutExecutor → Dispatcher → Handler → 업무 계층 → ETF이며, STF에서 사전 처리가 실패하면 업무 Handler는 실행되지 않는다.

# 거래 여행의 전체 단계표

| 단계 | 입력 | 주요 처리 | 출력 | 실패 시 주요 증상 |
| --- | --- | --- | --- | --- |
| 1 | 화면 입력값 | 요청 Body 생성 | 화면 요청 객체 | 필수 입력 누락 |
| 2 | 사용자 인증정보 | JWT 첨부 | Bearer Token | Token 누락 |
| 3 | URL·업무코드 | Gateway 라우팅 | 대상 WAR URL | 라우트 미등록 |
| 4 | JWT·요청 전문 | JWT 검증 | 인증 문맥 | 401·검증 실패 |
| 5 | HTTP 요청 | Controller 수신 | TCF 호출 | JSON 역직렬화 오류 |
| 6 | StandardRequest | Header 검증 | 정규화된 Header | 필수 Header 오류 |
| 7 | Header | GUID·TraceId 생성 | TransactionContext | 추적정보 미생성 |
| 8 | 인증 문맥 | 사용자·조직 정합성 | 검증된 사용자 | 사용자 불일치 |
| 9 | ServiceId | 거래통제·권한 확인 | 실행 허용 | 거래 중지·권한 없음 |
| 10 | ServiceId | Timeout 정책 결정 | TimeoutContext | 정책 오류 |
| 11 | 멱등성 Key | 중복 요청 확인 | 처리 중 상태 | 중복 거래 |
| 12 | Context | 거래로그 시작 | 시작 로그 | 로그 저장 실패 |
| 13 | ServiceId | Handler 탐색 | TransactionHandler | 미등록 ServiceId |
| 14 | 업무 요청 | 업무 규칙·DB 처리 | 업무 결과 | 업무 오류·DB 오류 |
| 15 | 처리 결과 | ETF 종료 처리 | StandardResponse | 후처리 오류 |
| 16 | 표준 응답 | 화면 결과 표시 | 사용자 화면 | 응답 해석 오류 |

# 핵심 소스 지도

| 구분 | 대표 소스 | 역할 |
| --- | --- | --- |
| Gateway 인증 | GatewayAuthenticationService | 인증 필요 여부와 예외 거래 판단 |
| Gateway JWT 검증 | GatewayJwtValidator | Bearer Token과 Claim 검증 |
| Gateway 라우팅 | GatewayRouteResolver | 업무코드별 목적지 조회 |
| Gateway 전달 | GatewayRouteDispatcher | 업무 WAR로 HTTP 요청 전달 |
| 업무 WAR JWT 검증 | TcfJwtAuthenticationFilter | Gateway가 없는 경우 JWT 검증 |
| 공통 Controller | OnlineTransactionController | /online 요청을 TCF에 위임 |
| 거래 실행 엔진 | TCF | STF·Dispatcher·ETF 순서 통제 |
| 사전 처리 | STF | 검증·권한·통제·Timeout·로그 시작 |
| 제한시간 실행 | OnlineTransactionTimeoutExecutor | Future 기반 제한시간 적용 |
| 거래 분배 | TransactionDispatcher | ServiceId로 Handler 탐색 |
| Handler 계약 | TransactionHandler | 단일 또는 복수 ServiceId 등록 |
| 후처리 | ETF | 성공·업무 실패·시스템 실패 처리 |
| 요청 전문 | StandardRequest | Header와 Body |
| 응답 전문 | StandardResponse | Header, Result, Body |

# 2.1 화면에서 데이터베이스까지

## 2.1.1 화면 이벤트가 거래의 시작점이다

거래는 Java 프로그램에서 시작되지 않는다.

사용자가 화면에서 어떤 행동을 했는지가 거래의 시작점이다.

예를 들어 고객 종합정보 화면이 있다고 가정하자.

| 화면 항목 | 예시 |
| --- | --- |
| 화면 ID | SV-CUS-0001 |
| 화면명 | 고객 종합정보 조회 |
| 이벤트 ID | SV-CUS-0001-E01 |
| 이벤트 | 조회 버튼 클릭 |
| ServiceId | SV.Customer.selectSummary |
| 거래코드 | SV-INQ-0001 |
| 업무코드 | SV |
| Context Path | /sv |
| Endpoint | POST /sv/online |

화면 ID와 ServiceId는 같은 것이 아니다.

화면 ID
\= 사용자가 보는 화면을 식별

이벤트 ID
\= 화면 안의 특정 행동을 식별

ServiceId
\= 서버에서 실행할 논리 거래를 식별

거래코드
\= 운영통제·감사·통계를 위한 거래 분류

하나의 화면에는 여러 이벤트가 존재할 수 있다.

고객 종합정보 화면
├─ 화면 최초 조회
├─ 고객 검색
├─ 상품목록 조회
├─ 상담메모 저장
├─ 엑셀 다운로드
└─ 상세 팝업 호출

따라서 화면과 거래는 반드시 일대일 관계가 아니다.

| 관계 | 예시 |
| --- | --- |
| 한 화면 → 여러 ServiceId | 조회·저장·다운로드 |
| 여러 화면 → 하나의 ServiceId | 공통 고객검색 |
| 한 이벤트 → 여러 ServiceId | 고객요약과 접촉이력 동시 조회 |
| 한 ServiceId → 여러 SQL | 고객기본·등급·상품 조회 |

화면 이벤트와 ServiceId, 프로그램, SQL, 테이블은 추적 가능한 형태로 관리해야 한다.

## 2.1.2 화면은 표준 요청 전문을 만든다

TCF 거래 요청은 StandardRequest 구조를 사용한다.

StandardRequest
├─ header
└─ body

### 요청 예시

{
"header": {
"systemId": "NSIGHT-MP",
"businessCode": "SV",
"serviceId": "SV.Customer.selectSummary",
"serviceName": "고객 종합정보 조회",
"transactionCode": "SV-INQ-0001",
"processingType": "INQUIRY",
"channelId": "WEBTOP",
"userId": "U10001",
"branchId": "001",
"idempotencyKey": null
},
"body": {
"customerNo": "C000001"
}
}

### Header와 Body의 차이

| 구분 | Header | Body |
| --- | --- | --- |
| 목적 | 거래 공통 문맥 | 업무별 입력 데이터 |
| 변경 빈도 | 비교적 낮음 | 거래마다 다름 |
| 사용 주체 | Gateway·STF·Dispatcher·ETF·OM | Handler 이하 업무 프로그램 |
| 예시 | ServiceId, 사용자, 채널 | 고객번호, 조회조건 |
| 보안 | 인증 Claim과 정합성 확인 | 개인정보·업무값 검증 |
| 로그 | 식별정보 중심 기록 | 원문 전체 출력 금지 |

### Header 주요 속성

| 속성 | 설명 | 주요 사용 위치 |
| --- | --- | --- |
| systemId | 호출 시스템 | 통계·연계 구분 |
| businessCode | 업무영역 | Gateway·WAR·로그 |
| serviceId | 논리 거래 | Dispatcher·OM |
| transactionCode | 운영 거래코드 | 통제·감사·통계 |
| processingType | 조회·변경 유형 | 권한·감사 |
| guid | 전 구간 추적 ID | 시스템 간 로그 |
| traceId | 내부 추적 ID | 호출 단계 로그 |
| channelId | 호출 채널 | 채널별 정책 |
| userId | 사용자 | 인증·감사 |
| branchId | 지점·조직 | 데이터권한 |
| centerId | 센터 | 운영·DR |
| requestTime | 요청시각 | 지연 분석 |
| clientIp | 원 클라이언트 IP | 보안·감사 |
| idempotencyKey | 중복 요청 식별 | 변경 거래 보호 |

### 중요한 보안 원칙

화면이 Header에 userId를 넣었다고 해서 서버가 그 값을 바로 신뢰해서는 안 된다.

화면 Header userId
\= 사용자가 주장하는 값

검증된 JWT userId
\= 인증 서버가 보증한 값

Gateway 또는 업무 WAR의 JWT Filter에서 JWT를 검증한 뒤, STF에서 JWT 기반 인증 문맥과 요청 Header의 사용자 정보를 비교해야 한다.

## 2.1.3 Gateway를 통해 업무 WAR로 이동한다

Gateway가 적용된 구조에서는 화면 요청이 업무 WAR로 직접 전달되지 않는다.

화면
↓
Gateway
├─ 업무코드 확인
├─ 로그인 예외 거래 확인
├─ Bearer Token 검증
├─ 사용자 Claim 추출
├─ Header 사용자 정합성 확인
├─ 업무코드별 대상 URL 조회
└─ 업무 WAR로 요청 전달

현재 Gateway 소스는 다음 역할을 분리한다.

| 구성요소 | 역할 |
| --- | --- |
| GatewayAuthenticationService | 인증 필요 여부 판단 |
| GatewayJwtValidator | JWT와 사용자 Claim 검증 |
| GatewayRequestUserReader | 요청 전문에서 ServiceId·사용자 조회 |
| GatewayRouteResolver | 업무코드별 활성 Route 조회 |
| GatewayRouteDispatcher | 대상 URL로 요청 전달 |
| GatewaySessionContext | 검증된 사용자 문맥 보관 |

### Gateway Route 예시

| 업무코드 | 대상 URL | Connect Timeout | Read Timeout |
| --- | --- | --- | --- |
| SV | http://sv-service:8080/sv/online | 3초 | 5초 |
| IC | http://ic-service:8080/ic/online | 3초 | 5초 |
| MG | http://mg-service:8080/mg/online | 3초 | 5초 |
| OM | http://om-service:8080/om/online | 3초 | 5초 |

Gateway의 Read Timeout과 TCF의 거래 Timeout은 같은 개념이 아니다.

| 구분 | Gateway Timeout | TCF Timeout |
| --- | --- | --- |
| 적용 위치 | Gateway → 업무 WAR HTTP 호출 | 업무 WAR 내부 거래 실행 |
| 목적 | 네트워크·대상 서버 대기 제한 | Handler 이하 실행시간 제한 |
| 대표 설정 | Connect·Read Timeout | ServiceId별 Online Timeout |
| 오류 관찰 위치 | Gateway | TCF·ETF |
| 설계 원칙 | TCF Timeout보다 약간 길게 검토 | 업무 SLA 기준 |

예를 들어 TCF Timeout이 3초인데 Gateway Read Timeout이 2초라면, TCF가 표준 Timeout 응답을 만들기 전에 Gateway가 연결을 끊을 수 있다.

## 2.1.4 업무 WAR에 진입한다

업무 WAR는 업무별 배포·설정·Spring Context를 가진다.

sv.war
→ /sv

ic.war
→ /ic

mg.war
→ /mg

하나의 Tomcat에 여러 WAR가 배포되면 WAR별 Spring Context와 Hikari Pool은 분리할 수 있지만, JVM·Heap·GC·Tomcat Connector Thread는 공유한다.

### 업무 WAR의 논리 구성

sv-service
├─ 기동·설정
├─ Handler
├─ Facade
├─ Service
├─ Rule
├─ DAO
├─ Mapper
├─ DTO
└─ Mapper XML

업무 WAR는 실제 고객·상품·캠페인 같은 업무 로직을 소유한다.

TCF는 업무 WAR 안에 포함되거나 공통 모듈 의존성으로 제공되지만, 업무 데이터의 의미를 직접 판단하지 않는다.

## 2.1.5 Controller가 요청을 수신한다

공통 OnlineTransactionController는 다음 두 경로를 제공한다.

| Endpoint | 설명 |
| --- | --- |
| POST /online | Context Path를 통해 업무가 이미 구분된 경우 |
| POST /{businessCode}/online | URL에서 업무코드를 함께 전달하는 경우 |

Controller의 실제 책임은 제한적이다.

요청 수신
→ Header가 없으면 빈 Header 생성
→ URL 업무코드를 Header에 보완
→ Client IP 확인
→ tcf.process(request)
→ StandardResponse 반환

Controller는 다음을 수행하지 않는다.

고객 존재 여부 판단
상품 가입 가능 여부 판단
Mapper 직접 호출
업무별 Timeout 계산
거래로그 직접 종료

### Client IP 처리

Proxy 또는 Gateway가 존재하면 직접 연결 IP는 Gateway 주소일 수 있다.

Controller는 X-Forwarded-For가 있으면 첫 번째 값을 사용하고, 없으면 getRemoteAddr()를 사용한다.

X-Forwarded-For: 10.1.1.10, 10.1.2.20
└───────┘
원 클라이언트 IP

단, X-Forwarded-For는 신뢰된 Proxy가 덮어쓰도록 구성해야 한다. 외부 사용자가 임의 Header를 보낼 수 있는 구조라면 위조가 가능하다.

## 2.1.6 TCF가 거래 실행을 시작한다

TCF.process()는 전체 거래의 실행 순서를 통제한다.

클라이언트 Header 원본 복사
↓
STF.preProcess()
↓
OnlineTransactionTimeoutExecutor.execute()
↓
TransactionDispatcher.dispatch()
↓
ETF.success()

오류가 발생하면 다음처럼 처리한다.

BusinessException
→ ETF.businessFail()

Timeout 관련 Exception
→ TimeoutExceptionResolver
→ BusinessException으로 변환
→ ETF.businessFail()

그 밖의 Exception
→ ETF.systemError()

마지막에는 성공·실패와 관계없이 Thread Local 정보를 정리한다.

TransactionContextHolder.clear()
AuthenticationContextHolder.clear()
TimeoutContextHolder.clear()
MDC.clear()

Tomcat Thread는 다음 요청에서 재사용된다. 따라서 이전 요청의 사용자·GUID·Timeout 문맥이 남지 않도록 반드시 정리해야 한다.

## 2.1.7 STF가 업무 실행 전 조건을 확인한다

STF의 실제 처리 순서는 다음과 같다.

| 순서 | 처리 | 주요 목적 |
| --- | --- | --- |
| 1 | headerValidator.validate() | 필수 Header·형식 검증 |
| 2 | GUID 생성 | 전 구간 거래 추적 |
| 3 | TraceId 생성 | 내부 로그 추적 |
| 4 | TransactionContext 생성 | 거래 문맥 보관 |
| 5 | MDC 설정 | 로그 자동 상관관계 |
| 6 | 인증 문맥 검증 | JWT 사용자와 Header 정합성 |
| 7 | 권한 검증 | 기능 실행 권한 확인 |
| 8 | 거래통제 확인 | 운영 중지 거래 차단 |
| 9 | Timeout 정책 적용 | 제한시간 결정 |
| 10 | 멱등성 확인 | 중복 요청 방지 |
| 11 | 거래로그 시작 | 처리 시작 증적 |

### STF 처리 흐름

표준 Header 검증
↓
GUID·TraceId 생성
↓
TransactionContext 생성
↓
인증정보 정합성
↓
기능·데이터 권한
↓
거래 실행 가능 여부
↓
Timeout 정책
↓
중복 요청
↓
거래로그 START

이 중 하나라도 실패하면 Handler는 실행되지 않는다.

### STF 실패와 데이터 변경 여부

| 실패 단계 | Handler 실행 | DB 변경 | 거래로그 |
| --- | --- | --- | --- |
| Header 검증 | 아니오 | 없음 | 시작 전일 수 있음 |
| 인증 검증 | 아니오 | 없음 | 정책에 따라 |
| 권한 검증 | 아니오 | 없음 | 정책에 따라 |
| 거래통제 | 아니오 | 없음 | 통제 이력 필요 |
| 멱등성 | 아니오 | 없음 | 중복 이력 필요 |
| 거래로그 시작 실패 | 원칙적으로 중단 검토 | 없음 | 저장소 장애 |

## 2.1.8 Timeout Executor가 실행시간을 제한한다

OnlineTransactionTimeoutExecutor는 다음 방식으로 동작한다.

Timeout 정책 활성 여부 확인
↓
ServiceId별 Timeout 값 확인
↓
ExecutorService에 업무 실행 제출
↓
Future.get(timeoutSec)
├─ 제한시간 내 완료 → 결과 반환
├─ Timeout → Future 취소
├─ Interrupted → 시스템 오류
└─ 업무 Exception → 원래 Exception 전달

### Timeout의 실제 의미

future.cancel(true)가 호출되더라도 모든 하위 작업이 즉시 중단되는 것은 아니다.

| 하위 작업 | 즉시 중단 가능성 |
| --- | --- |
| Java 연산 | Interrupt를 확인하면 중단 가능 |
| JDBC SQL | Driver·Query Timeout 설정에 의존 |
| 외부 HTTP 호출 | Client Read Timeout에 의존 |
| DB Lock 대기 | DB·JDBC 정책에 의존 |
| 파일 I/O | 구현 방식에 따라 다름 |

따라서 Timeout은 계층별로 맞춰야 한다.

Gateway Read Timeout
\> TCF Online Timeout
\> 외부 Client Timeout 또는 SQL Timeout

예시는 다음과 같다.

| 계층 | 예시값 |
| --- | --- |
| Gateway Read Timeout | 4초 |
| TCF Online Timeout | 3초 |
| 외부 Client Timeout | 2초 |
| MyBatis Query Timeout | 2초 |
| Hikari Connection Timeout | 1~3초 |

실제 값은 거래 특성과 성능시험 결과를 기준으로 확정해야 한다.

## 2.1.9 Dispatcher가 Handler를 선택한다

TransactionDispatcher는 기동 시 모든 TransactionHandler Bean을 조회한다.

Spring Bean 목록 조회
↓
Handler별 serviceIds() 확인
↓
ServiceId → Handler Map 생성
↓
중복 ServiceId 검사

### 기동 시 등록 예시

SV.Customer.selectSummary
→ SvCustomerHandler

SV.Customer.selectGrade
→ SvCustomerHandler

SV.Customer.updateMemo
→ SvCustomerHandler

현재 Handler 계약은 두 방식을 지원한다.

| 방식 | 설명 | 적합한 경우 |
| --- | --- | --- |
| serviceId() | Handler 하나가 거래 하나 처리 | 단순·독립 거래 |
| serviceIds() | 도메인 Handler가 여러 거래 처리 | 고객·상품 등 도메인 묶음 |

### 실행 시 처리

Header에서 serviceId 추출
↓
값이 없으면 INVALID\_HEADER
↓
handlerMap.get(serviceId)
↓
없으면 SERVICE\_NOT\_FOUND
↓
handler.handle(request, context)

### Dispatcher 오류 유형

| 오류 | 발생 시점 | 결과 |
| --- | --- | --- |
| ServiceId 없음 | 거래 실행 시 | 업무 오류 |
| 미등록 ServiceId | 거래 실행 시 | SERVICE\_NOT\_FOUND |
| 중복 ServiceId | 애플리케이션 기동 시 | 기동 실패 |
| Handler Bean 미등록 | 기동·실행 | 미등록 거래 |
| Handler의 빈 ServiceId 목록 | 기동 시 | 경고 후 제외 가능 |

중복 ServiceId를 임의로 하나 선택하지 않고 기동을 실패시키는 것은 잘못된 거래 실행을 사전에 막는 중요한 품질 통제다.

## 2.1.10 Handler 이하에서 업무가 처리된다

업무 처리 계층은 다음 순서를 따른다.

Handler
↓
Facade
↓
Service
├─ Rule
├─ DAO
│ ↓
│ Mapper
│ ↓
│ SQL
│ ↓
│ DB
└─ Client
↓
외부 시스템

### 계층별 역할

| 계층 | 주요 책임 | 입력 | 출력 |
| --- | --- | --- | --- |
| Handler | ServiceId 분기·DTO 변환 | StandardRequest | 업무 요청 DTO |
| Facade | 유스케이스 조립·트랜잭션 | 요청 DTO | 유스케이스 결과 |
| Service | 업무 처리 흐름 | 업무 객체 | 업무 결과 |
| Rule | 업무조건 검증 | 상태·값 | 통과·업무 오류 |
| DAO | 데이터 접근 추상화 | Query·Command | Result |
| Mapper | SQL 실행 | Parameter | DB 결과 |
| Client | 타 시스템 계약 호출 | 연계 DTO | 연계 응답 |

### 트랜잭션 경계

NSIGHT TCF 기준에서는 일반적으로 Facade를 유스케이스와 DB 트랜잭션의 경계로 본다.

Facade 시작
↓
@Transactional
↓
Service
↓
DAO·Mapper
↓
정상 종료 → Commit
예외 전파 → Rollback

Handler가 SQL을 직접 실행하면 트랜잭션 경계가 불명확해진다.

Rule이 Mapper를 호출하면 업무규칙과 데이터 접근이 결합된다.

DAO에서 사용자 메시지를 결정하면 데이터 계층과 화면 정책이 섞인다.

## 2.1.11 Mapper가 DB를 호출한다

Mapper는 MyBatis를 통해 SQL을 실행한다.

DAO Method
↓
Mapper Interface Method
↓
Mapper XML SQL ID
↓
SQL
↓
Table·View

### 추적 예시

| 구분 | 예시 |
| --- | --- |
| ServiceId | SV.Customer.selectSummary |
| Handler | SvCustomerHandler |
| Facade Method | selectCustomerSummary() |
| Service Method | selectSummary() |
| DAO Method | selectCustomerSummary() |
| Mapper Namespace | SvCustomerMapper |
| SQL ID | selectCustomerSummary |
| Table | SV\_CUSTOMER\_SUMMARY |

DB 객체와 프로그램은 ServiceId에서 Mapper·SQL·테이블까지 추적 가능해야 한다.

## 2.1.12 DB 결과가 업무 응답으로 변환된다

DB 결과를 그대로 화면에 반환해서는 안 된다.

DB Result
↓
Result DTO
↓
업무 Response DTO
↓
StandardResponse Body

### 객체 분리 기준

| 객체 | 목적 |
| --- | --- |
| Request DTO | 화면·외부 입력 계약 |
| Command DTO | 변경 업무 명령 |
| Query DTO | 조회조건 |
| Result DTO | DB 조회 결과 |
| Response DTO | 화면에 제공할 결과 |
| Domain Object | 업무 상태와 규칙 |

DB 컬럼이 변경되었다고 화면 계약이 무조건 변경되어서는 안 된다.

화면에 필드가 추가되었다고 DB 테이블에 같은 컬럼이 반드시 추가되는 것도 아니다.

각 경계에서 필요한 형태로 변환한다.

## 2.1.13 ETF가 결과를 표준화한다

업무 처리가 완료되면 ETF가 응답과 운영 기록을 종료한다.

### 성공 시

업무 결과 반환
↓
멱등성 성공 표시
↓
거래로그 정상 종료
↓
감사로그 기록
↓
Metric 기록
↓
StandardResponse.success()

### 응답 구조

StandardResponse
├─ header
├─ result
└─ body

### 성공 응답 예시

{
"header": {
"businessCode": "SV",
"serviceId": "SV.Customer.selectSummary",
"guid": "G-20260718-000001",
"traceId": "T-20260718-000001"
},
"result": {
"success": true,
"code": "S0000",
"message": "정상 처리되었습니다."
},
"body": {
"customerNo": "C000001",
"customerName": "홍길동",
"customerGrade": "A",
"productCount": 3
}
}

## 2.1.14 화면이 응답을 해석한다

화면은 HTTP 통신 성공만 보고 거래 성공으로 판단해서는 안 된다.

HTTP 200
≠ 업무 처리 성공

TCF 표준 전문에서는 HTTP 전송이 성공했어도 result가 실패일 수 있다.

| HTTP | Result | 의미 |
| --- | --- | --- |
| 200 | 성공 | 정상 거래 |
| 200 | 업무 실패 | 업무조건 불충족 |
| 200 또는 정책값 | 시스템 실패 | 서버 처리 오류 |
| 401 | 없음 또는 인증 오류 | Gateway·Filter 인증 실패 |
| 403 | 없음 또는 권한 오류 | 접근 거절 |
| 404 | Gateway 오류 | Route·Endpoint 오류 |
| 503 | Gateway 오류 | JWT 검증기·대상 서비스 불가 |

화면은 결과에 따라 사용자의 다음 행동을 안내해야 한다.

| 오류 유형 | 화면 행동 |
| --- | --- |
| 입력 오류 | 해당 필드 강조 |
| 인증 만료 | 재로그인 또는 토큰 갱신 |
| 권한 없음 | 접근 제한 안내 |
| 업무 오류 | 조건 수정 안내 |
| Timeout | 중복 실행 주의와 재조회 안내 |
| 시스템 오류 | 상관관계 ID를 포함한 문의 안내 |

## 2.1 전체 흐름표

| 순서 | 구성요소 | 핵심 처리 | 대표 증적 |
| --- | --- | --- | --- |
| 1 | 화면 이벤트 | ServiceId 선택 | 화면 설계서 |
| 2 | UI 요청 모듈 | StandardRequest 생성 | Network Payload |
| 3 | Gateway | 인증·라우팅 | Gateway 로그 |
| 4 | 업무 WAR Filter | JWT 검증 | 인증 로그 |
| 5 | Controller | TCF 위임 | Controller 로그 |
| 6 | STF | 공통 사전 처리 | GUID·거래 시작 로그 |
| 7 | Timeout Executor | 제한시간 적용 | Timeout 정책 |
| 8 | Dispatcher | Handler 선택 | ServiceId 등록 로그 |
| 9 | Handler | Facade 호출 | Handler 로그 |
| 10 | Facade | 트랜잭션 시작 | Transaction 로그 |
| 11 | Service·Rule | 업무 판단 | 업무 오류코드 |
| 12 | DAO·Mapper | SQL 실행 | Mapper ID·SQL 로그 |
| 13 | DB | 데이터 처리 | 영향 행 수 |
| 14 | ETF | 응답·로그 종료 | 거래 종료 로그 |
| 15 | 화면 | 결과 표시 | 화면 결과 |

# 2.2 Gateway·TCF·업무 WAR의 역할

## 2.2.1 세 구성요소를 한곳에 섞으면 안 된다

Gateway, TCF, 업무 WAR는 모두 요청 처리에 참여하지만 목적이 다르다.

Gateway
\= 외부 요청이 어느 내부 시스템으로 갈지 통제

TCF
\= 내부 업무 거래가 어떤 절차로 실행될지 통제

업무 WAR
\= 실제 업무가 무엇을 판단하고 처리할지 구현

### 책임 비교표

| 관점 | Gateway | TCF | 업무 WAR |
| --- | --- | --- | --- |
| 주된 위치 | 시스템 외부 경계 | 업무 WAR 내부 공통 계층 | 업무 애플리케이션 |
| 핵심 식별자 | 업무코드·Route | ServiceId | 도메인·유스케이스 |
| 인증 | JWT 1차 검증 | 인증 문맥 방어 검증 | 상세 데이터권한 |
| 라우팅 | 대상 WAR 결정 | Handler 결정 | 내부 업무 호출 |
| Timeout | HTTP 전달 제한 | 거래 실행 제한 | SQL·외부 호출 제한 |
| 거래통제 | 일반적으로 위임 | 공통 정책 적용 | 업무 상태 판단 |
| 로그 | Proxy·인증 로그 | 거래·감사·Metric | 업무 상세 로그 |
| 데이터 접근 | 금지 | 금지 | DAO·Mapper 수행 |
| 업무 판단 | 금지 | 금지 | Service·Rule 수행 |

## 2.2.2 Gateway의 역할

Gateway는 다음 질문에 답한다.

이 요청은 인증되었는가?

어느 업무코드의 요청인가?

어느 업무 WAR로 전달해야 하는가?

전달 대상은 현재 사용 가능한가?

사용자 Claim과 요청 Header가 일치하는가?

### Gateway 인증 흐름

Authorization Header 확인
↓
로그인 예외 ServiceId 확인
↓
Bearer Token 존재 확인
↓
JwtDecoder로 서명·만료 검증
↓
userId·branchId·channelId 추출
↓
요청 Header 사용자와 비교
↓
GatewaySessionContext 생성

Gateway JWT 검증은 외부 요청의 1차 보안 경계다. 토큰 발급은 tcf-jwt, Gateway는 Public Key 또는 JWKS 기반 검증을 담당하는 구조가 적절하다.

### Gateway가 해서는 안 되는 일

고객 가입 가능 여부 판단
캠페인 대상자 선정
DB Mapper 직접 호출
업무 트랜잭션 시작
업무 오류 메시지 결정

## 2.2.3 TCF의 역할

TCF는 다음 질문에 답한다.

요청 전문이 표준에 맞는가?

검증된 사용자 문맥이 존재하는가?

이 사용자에게 실행 권한이 있는가?

이 거래는 현재 실행 가능한가?

몇 초 안에 끝나야 하는가?

동일 요청이 이미 처리 중인가?

어느 Handler가 이 ServiceId를 담당하는가?

결과를 어떤 형식으로 종료할 것인가?

TCF는 업무 로직을 구현하지 않는다.

업무가 실행될 **조건·순서·기록 방식**을 통제한다.

## 2.2.4 업무 WAR의 역할

업무 WAR는 다음 질문에 답한다.

고객이 존재하는가?

해당 고객을 조회할 수 있는가?

현재 상태에서 변경이 가능한가?

어떤 테이블을 조회·변경해야 하는가?

어떤 외부 업무 서비스를 호출해야 하는가?

어떤 업무 오류코드를 반환해야 하는가?

### 업무 WAR 내부 책임

| 영역 | 예시 |
| --- | --- |
| 고객 도메인 | 고객 조회·상태·등급 |
| 상품 도메인 | 상품정보·가입조건 |
| 캠페인 도메인 | 대상자·실행·반응 |
| 접촉 도메인 | 상담·접촉 이력 |
| 관리 도메인 | 기준정보·권한 |

## 2.2.5 Gateway가 없는 경우

Gateway가 없으면 각 업무 WAR 진입부에서 JWT를 직접 검증해야 한다.

사용자
↓
L4·Apache
↓
업무 WAR
↓
TcfJwtAuthenticationFilter
↓
OnlineTransactionController
↓
TCF·STF

역할은 다음처럼 분리한다.

| 검증 | 담당 |
| --- | --- |
| JWT 서명·만료·Issuer·Audience | 업무 WAR JWT Filter |
| Claim → 인증 문맥 생성 | JWT Filter |
| Header 사용자 정합성 | STF |
| 기능 권한 | STF·공통 권한 |
| 상세 데이터권한 | 업무 Service·Rule |

Gateway가 없는 환경에서도 JWT 검증 코드를 Handler나 Service에 작성하지 않는다. 공통 tcf-web Filter를 사용해야 한다.

## 2.2.6 업무 WAR 직접 접근 예외

실제 소스에는 /sv/direct/online 형태의 TCF 우회 샘플 Controller가 존재한다.

이 Controller는 주석에서 다음 사실을 명확히 밝힌다.

STF·ETF 전처리와 후처리를 수행하지 않는다.

거래통제·멱등성 등
TCF 파이프라인을 수행하지 않는다.

Facade → Service 업무 체인만 직접 사용한다.

이 구조는 교육·외부 직접 인입 샘플로는 의미가 있지만, 일반 운영 거래의 기본 구조로 사용해서는 안 된다.

### 직접 인입과 TCF 경유 비교

| 항목 | TCF 경유 | 직접 인입 |
| --- | --- | --- |
| 표준 Header 검증 | 적용 | 직접 구현 필요 |
| 인증 문맥 검증 | 적용 | 별도 구현 필요 |
| 권한 | 적용 | 별도 구현 필요 |
| 거래통제 | 적용 | 미적용 가능 |
| Timeout 정책 | 적용 | 별도 구현 |
| 멱등성 | 적용 | 미적용 가능 |
| 거래로그 | 적용 | 별도 구현 |
| 감사·Metric | 적용 | 별도 구현 |
| Handler 등록 | 적용 | Controller 직접 분기 |
| 운영 통제 | 높음 | 낮음 |

### 직접 인입이 허용될 수 있는 조건

| 조건 | 필수 조치 |
| --- | --- |
| 내부 헬스체크 | 업무 데이터 처리 금지 |
| 제한된 운영 API | 관리자 인증·망분리 |
| Streaming·특수 파일 API | TCF Gateway를 통한 공통 통제 연결 검토 |
| 외부 호환 API | 인증·Timeout·로그를 동일 수준으로 구현 |
| 임시 전환 API | 종료일·제거계획·예외승인 |

예외 구조를 사용한다면 다음을 문서화해야 한다.

왜 TCF를 우회하는가?
어떤 공통 기능을 대체 구현하는가?
누가 승인했는가?
언제 제거하는가?
장애 시 누가 책임지는가?

## 2.2.7 다중 WAR 환경의 책임

하나의 Tomcat에 여러 WAR가 배포되면 다음 자원을 공유한다.

| 자원 | 공유 여부 |
| --- | --- |
| JVM Process | 공유 |
| Heap | 공유 |
| Metaspace | 공유 |
| GC | 공유 |
| Tomcat Connector | 공유 |
| Connector Thread Pool | 공유 |
| Spring Context | WAR별 분리 |
| ClassLoader | WAR별 분리 |
| Hikari Pool | 일반적으로 WAR별 |
| Mapper | WAR별 |
| Cache | 설정에 따라 분리 |

특정 WAR의 Slow SQL이 Thread를 장기간 점유하면 다른 WAR도 요청을 처리하지 못할 수 있다.

따라서 업무 WAR는 코드·배포 단위뿐 아니라 자원·장애 영향까지 고려해 배치해야 한다.

## 2.2.8 책임 경계 RACI

| 활동 | Gateway팀 | TCF팀 | 업무팀 | 운영팀 | 보안팀 |
| --- | --- | --- | --- | --- | --- |
| 업무코드 Route | R | C | C | A | I |
| JWT 검증 | R | C | I | C | A |
| ServiceId 등록 | I | C | R | A | I |
| Handler 구현 | I | C | R/A | I | I |
| 거래통제 엔진 | I | R | C | A | C |
| 거래통제 값 | I | C | C | R/A | C |
| Timeout 엔진 | I | R | C | C | I |
| 거래별 Timeout | C | C | R | A | I |
| 업무 규칙 | I | I | R/A | I | C |
| SQL | I | I | R | C | I |
| 감사 정책 | C | R | C | R | A |
| 장애 대응 | C | C | R | R/A | I |

# 2.3 ServiceId로 연결되는 처리 흐름

## 2.3.1 ServiceId는 서버의 논리 주소다

URL이 물리적인 진입 경로라면 ServiceId는 논리적인 업무 주소다.

/sv/online
\= SV 업무 WAR의 공통 온라인 진입점

SV.Customer.selectSummary
\= 고객 종합정보 조회 거래

같은 URL에서 여러 ServiceId를 처리할 수 있다.

POST /sv/online
├─ SV.Customer.selectSummary
├─ SV.Customer.selectGrade
├─ SV.Customer.selectProductList
└─ SV.Customer.updateMemo

## 2.3.2 주요 식별자의 관계

| 식별자 | 무엇을 식별하는가 | 대표 사용처 |
| --- | --- | --- |
| 화면 ID | 화면 | UI·메뉴·권한 |
| 이벤트 ID | 화면 행동 | 화면설계·테스트 |
| 업무코드 | 업무영역·WAR | Gateway·배포 |
| Context Path | 웹 애플리케이션 | Apache·Tomcat |
| ServiceId | 논리 거래 | Dispatcher·OM |
| 거래코드 | 통제·감사·통계 | OM·로그 |
| Handler | 업무 진입 프로그램 | Spring·Dispatcher |
| GUID | 전체 거래 인스턴스 | 로그·연계 |
| TraceId | 내부 호출 흐름 | 로그 |
| SQL ID | 데이터 실행 단위 | MyBatis·DB 분석 |

### 전체 식별 관계

화면 ID
↓
이벤트 ID
↓
업무코드
↓
ServiceId
↓
Handler
↓
Facade·Service·Rule
↓
DAO·Mapper
↓
SQL ID
↓
DB 객체

## 2.3.3 ServiceId 표준 형식

권장 형식은 다음과 같다.

{업무코드}.{도메인}.{행위}

예:

| ServiceId | 의미 |
| --- | --- |
| SV.Customer.selectSummary | 고객요약 조회 |
| SV.Customer.updateMemo | 고객메모 변경 |
| CM.Campaign.createCampaign | 캠페인 생성 |
| CT.Contact.selectHistory | 접촉이력 조회 |
| OM.ServiceCatalog.updatePolicy | 서비스 정책 변경 |

### 행위어 기준

| 유형 | 권장 행위어 |
| --- | --- |
| 단건 조회 | select, get |
| 목록 조회 | selectList, search |
| 등록 | create, insert, register |
| 변경 | update, modify |
| 삭제 | delete, remove |
| 승인 | approve |
| 취소 | cancel |
| 실행 | execute, run |
| 검증 | validate, check |
| 다운로드 | download |

프로젝트 전체에서 동의어를 무분별하게 혼용하지 않는다.

## 2.3.4 Handler 등록 과정

### 애플리케이션 기동 시

Spring Component Scan
↓
TransactionHandler Bean 생성
↓
TransactionDispatcher 생성
↓
각 Handler의 serviceIds() 호출
↓
handlerMap 등록
↓
중복 ServiceId 검사

### 도메인 Handler 예시

@Component
public class SvCustomerHandler implements TransactionHandler {

@Override
public Collection<String> serviceIds() {
return List.of(
"SV.Customer.selectSummary",
"SV.Customer.selectGrade",
"SV.Customer.updateMemo"
);
}

@Override
public Object handle(
StandardRequest<Map<String, Object>> request,
TransactionContext context) {

return switch (request.getHeader().getServiceId()) {
case "SV.Customer.selectSummary" ->
customerFacade.selectSummary(request.getBody(), context);

case "SV.Customer.selectGrade" ->
customerFacade.selectGrade(request.getBody(), context);

case "SV.Customer.updateMemo" ->
customerFacade.updateMemo(request.getBody(), context);

default ->
throw new BusinessException(
ErrorCode.SERVICE\_NOT\_FOUND,
"지원하지 않는 고객 거래입니다."
);
};
}
}

실제 클래스와 메서드명은 프로젝트 소스를 기준으로 적용한다.

## 2.3.5 런타임 분배 과정

StandardHeader.serviceId
↓
TransactionDispatcher.dispatch()
↓
handlerMap.get(serviceId)
↓
TransactionHandler.handle()
↓
ServiceId별 Facade Method

### 런타임 분배 표

| 입력 ServiceId | Handler | Facade Method |
| --- | --- | --- |
| SV.Customer.selectSummary | SvCustomerHandler | selectSummary() |
| SV.Customer.selectGrade | SvCustomerHandler | selectGrade() |
| SV.Customer.updateMemo | SvCustomerHandler | updateMemo() |
| CT.Contact.selectHistory | CtContactHandler | selectHistory() |

## 2.3.6 화면–프로그램–SQL 추적성 예시

| 추적 ID | 화면 이벤트 | ServiceId | Handler | Service | SQL ID | 테이블 |
| --- | --- | --- | --- | --- | --- | --- |
| SV-CUS-0001-E01-C01-D01 | 고객요약 조회 | SV.Customer.selectSummary | SvCustomerHandler | selectSummary | selectSummary | SV\_CUSTOMER\_SUMMARY |
| SV-CUS-0001-E01-C01-D02 | 고객요약 조회 | SV.Customer.selectSummary | SvCustomerHandler | selectSummary | selectGrade | SV\_CUSTOMER\_GRADE |
| SV-CUS-0001-E02-C01-D01 | 상품 조회 | SV.Customer.selectProducts | SvCustomerHandler | selectProducts | selectProductList | SV\_CUSTOMER\_PRODUCT |

이 매트릭스가 있으면 다음과 같은 역추적이 가능하다.

SV\_CUSTOMER\_GRADE 테이블 변경
↓
selectGrade SQL 영향
↓
selectSummary Service 영향
↓
SV.Customer.selectSummary 거래 영향
↓
SV-CUS-0001 화면 영향

## 2.3.7 ServiceId 오류 흐름

### ServiceId 누락

Header.serviceId 없음
↓
Dispatcher가 INVALID\_HEADER 발생
↓
TCF가 BusinessException 처리
↓
ETF.businessFail()

### 미등록 ServiceId

ServiceId 존재
↓
handlerMap 조회
↓
Handler 없음
↓
SERVICE\_NOT\_FOUND
↓
업무 오류 응답

### 중복 ServiceId

Handler A가 동일 ServiceId 등록
Handler B도 동일 ServiceId 등록
↓
Dispatcher 초기화 중 중복 발견
↓
IllegalStateException
↓
애플리케이션 기동 실패

### 오류 비교표

| 오류 | 발견 시점 | 영향 범위 | 권장 조치 |
| --- | --- | --- | --- |
| 누락 | 요청 실행 | 해당 거래 | 화면 전문 수정 |
| 미등록 | 요청 실행 | 해당 거래 | Handler·OM 등록 |
| 중복 | 기동 | WAR 전체 | ServiceId 소유권 조정 |
| 오타 | 요청 실행 | 해당 화면 | 추적 매트릭스 검증 |
| 폐기 거래 호출 | 요청 실행 | 구버전 화면 | 호환·폐기 정책 적용 |

## 2.3.8 ServiceId는 OM 기준정보와 연결되어야 한다

소스에 Handler가 등록되어 있다고 해서 운영 준비가 끝난 것은 아니다.

| 연계 대상 | 필요한 정보 |
| --- | --- |
| OM Service Catalog | ServiceId, 업무코드, 담당자 |
| 거래통제 | 실행 여부·기간·채널 |
| Timeout 정책 | Online Timeout |
| 권한 | 기능권한·데이터권한 |
| 감사 | 조회·변경·다운로드 여부 |
| 모니터링 | 목표 응답시간·임계치 |
| 테스트 | 거래별 테스트케이스 |
| 배포 | 최초 버전·폐기 버전 |

### 완전한 등록 조건

화면에 존재
\+ ServiceId가 요청에 사용됨
\+ Handler에 등록됨
\+ OM Catalog에 등록됨
\+ Timeout 정책 존재
\+ 테스트가 존재
\+ 로그로 추적 가능

# 2.4 성공·업무 오류·시스템 오류 흐름

## 2.4.1 오류를 왜 구분해야 하는가

모든 실패가 같은 의미를 가지지 않는다.

| 구분 | 예시 | 사용자가 할 일 |
| --- | --- | --- |
| 입력 오류 | 고객번호 형식 오류 | 입력 수정 |
| 업무 오류 | 고객이 존재하지 않음 | 조건 확인 |
| 인증 오류 | Token 만료 | 다시 로그인 |
| 권한 오류 | 조회권한 없음 | 권한 요청 |
| Timeout | 처리가 너무 오래 걸림 | 상태 확인 후 재시도 |
| 시스템 오류 | DB 장애·NullPointer | 운영 문의 |

모든 오류를 “시스템 오류”로 반환하면 사용자는 무엇을 해야 할지 알 수 없다.

반대로 DB 오류를 “조회 결과 없음”으로 숨기면 장애를 놓칠 수 있다.

## 2.4.2 성공 흐름

Handler 정상 종료
↓
업무 결과 반환
↓
ETF.success()
↓
Idempotency 성공 표시
↓
거래로그 S0000 종료
↓
감사로그 기록
↓
Metric 성공 기록
↓
StandardResponse.success()

### 성공 판정 기준

| 기준 | 확인 내용 |
| --- | --- |
| 프로그램 | 예외 없이 Handler 종료 |
| 트랜잭션 | Commit 완료 |
| 데이터 | 예상 결과·영향 행 수 |
| 로그 | 거래 시작과 정상 종료 |
| 응답 | result.success=true |
| 운영 | Metric 성공 기록 |
| 멱등성 | 성공 상태로 전환 |

## 2.4.3 업무 오류 흐름

업무 오류는 시스템이 고장 난 것이 아니라, 정상적인 업무 판단 결과 요청을 처리할 수 없는 경우다.

예:

고객이 존재하지 않음
중복 등록
현재 상태에서 변경 불가
승인 권한 없음
거래 중지 상태
허용 한도 초과

### 처리 흐름

Service·Rule에서 업무조건 위반
↓
BusinessException 발생
↓
트랜잭션 Rollback
↓
TCF catch (BusinessException)
↓
ETF.businessFail()
↓
멱등성 실패 표시
↓
거래로그 업무 실패 종료
↓
감사·Metric 실패 기록
↓
업무 오류코드·메시지 반환

### 업무 오류 설계 원칙

| 원칙 | 설명 |
| --- | --- |
| 의미 있는 코드 | 사용자가 취할 행동을 구분 |
| 안전한 메시지 | 내부 SQL·테이블명 노출 금지 |
| 예측 가능성 | 테스트 가능한 조건 |
| 예외 전파 | Handler에서 숨기지 않음 |
| Rollback | 변경 거래는 원자성 보장 |
| 로그 구분 | 시스템 장애와 분리 |

### 업무 오류 응답 예시

{
"header": {
"serviceId": "SV.Customer.selectSummary",
"guid": "G-20260718-000002"
},
"result": {
"success": false,
"code": "SV-CUSTOMER-0001",
"message": "조회 대상 고객이 존재하지 않습니다."
},
"body": null
}

## 2.4.4 시스템 오류 흐름

시스템 오류는 예상하지 못한 프로그램·DB·네트워크·인프라 장애다.

예:

NullPointerException
SQL 문법 오류
DB Connection 실패
외부 시스템 연결 실패
파일 시스템 오류
메모리 부족
설정 누락

### 처리 흐름

예상하지 못한 Exception
↓
트랜잭션 Rollback
↓
TCF catch (Exception)
↓
Timeout 여부 확인
├─ Timeout이면 업무 실패 경로
└─ 그 밖의 오류면 ETF.systemError()
↓
서버 로그에 원인 Exception 기록
↓
멱등성 실패 표시
↓
거래로그 시스템 실패 종료
↓
감사·Metric 실패 기록
↓
안전한 공통 메시지 반환

### ETF 시스템 오류 처리

ETF는 서버 로그에 ServiceId와 원인 예외를 기록하고, 클라이언트에는 공통 시스템 오류를 반환한다.

서버 로그
\= 상세 Stack Trace와 원인

클라이언트 응답
\= 안전한 오류코드·메시지·GUID

### 금지 응답

{
"message": "ORA-00942: table or view does not exist",
"sql": "SELECT \* FROM OM\_USER",
"stackTrace": "..."
}

SQL·테이블·서버 경로·Stack Trace를 사용자에게 반환하면 보안과 운영 측면에서 위험하다.

## 2.4.5 Timeout 흐름

현재 TCF는 온라인 제한시간 초과를 BusinessException 형태로 변환하여 업무 실패 경로로 종료한다.

TimeoutExecutor.future.get()
↓ 제한시간 초과
Future.cancel(true)
↓
TIMEOUT\_ONLINE BusinessException
↓
ETF.businessFail()

### Timeout 후 반드시 확인할 사항

| 확인 항목 | 이유 |
| --- | --- |
| DB 작업이 실제 종료되었는가 | Thread 취소만으로 SQL이 멈추지 않을 수 있음 |
| 변경이 Commit되었는가 | 응답 실패와 DB 성공이 불일치할 수 있음 |
| 중복 재요청 가능성이 있는가 | 사용자가 다시 버튼을 누를 수 있음 |
| 외부 시스템은 처리했는가 | 호출자는 Timeout이어도 상대는 성공 가능 |
| Connection이 반환되었는가 | Pool 고갈 방지 |
| 멱등성 상태가 무엇인가 | 재처리 판단 |

변경 거래는 Timeout 시 “실패했으니 다시 실행”이라고 단정하면 안 된다.

사용자 응답 실패
≠ 실제 업무 처리 실패

상태조회·멱등성·후속 확인 절차가 필요하다.

## 2.4.6 인증 오류는 TCF 전에 발생할 수 있다

Gateway 또는 JWT Filter에서 인증이 실패하면 Controller와 TCF에 도달하지 않는다.

Bearer Token 없음
↓
Gateway 또는 Filter
↓
HTTP 401
↓
TCF 미실행
↓
거래로그가 없을 수 있음

따라서 장애를 찾을 때 거래로그만 검색해서는 안 된다.

| 실패 위치 | 우선 확인 로그 |
| --- | --- |
| Gateway 인증 전 | Gateway Access 로그 |
| Gateway JWT 검증 | Gateway 인증 로그 |
| 업무 WAR Filter | JWT Filter 로그 |
| STF 이후 | TCF 거래로그 |
| Handler 이후 | 업무·SQL 로그 |

## 2.4.7 오류 유형 비교

| 구분 | 발생 위치 | Exception | ETF 경로 | Rollback | 사용자 메시지 |
| --- | --- | --- | --- | --- | --- |
| 입력 오류 | STF | BusinessException | businessFail | 변경 없음 | 입력 안내 |
| 인증 오류 | Gateway·Filter | 인증 오류 | TCF 미진입 가능 | 없음 | 재인증 |
| 권한 오류 | STF | BusinessException | businessFail | 변경 없음 | 접근 제한 |
| 거래통제 | STF | BusinessException | businessFail | 변경 없음 | 거래 중지 |
| 업무 오류 | Rule·Service | BusinessException | businessFail | 예 | 업무 메시지 |
| Timeout | Executor | Timeout 변환 | businessFail | 확인 필요 | 시간 초과 |
| DB 오류 | Mapper·DB | Exception | systemError | 예 | 시스템 오류 |
| 프로그램 오류 | 업무 코드 | Exception | systemError | 예 | 시스템 오류 |
| ETF 오류 | 후처리 | Exception | 별도 보호 필요 | 업무 완료 가능 | 운영 확인 |

# 거래 한 건의 정상 여행 예시

## 대상 거래

| 항목 | 값 |
| --- | --- |
| 화면 | 고객 종합정보 |
| 화면 ID | SV-CUS-0001 |
| 이벤트 | 조회 |
| ServiceId | SV.Customer.selectSummary |
| 거래코드 | SV-INQ-0001 |
| 업무 WAR | sv-service |
| SQL | SvCustomerMapper.selectSummary |

## 시간순 여행

| 시각 | 단계 | 처리 |
| --- | --- | --- |
| 10:00:00.000 | 화면 | 조회 버튼 클릭 |
| 10:00:00.010 | UI | StandardRequest 생성 |
| 10:00:00.020 | Gateway | JWT 검증 |
| 10:00:00.025 | Gateway | SV Route 조회 |
| 10:00:00.035 | WAR | Controller 진입 |
| 10:00:00.040 | STF | GUID 생성 |
| 10:00:00.045 | STF | 인증·권한 확인 |
| 10:00:00.050 | STF | Timeout 3초 적용 |
| 10:00:00.055 | Dispatcher | Handler 선택 |
| 10:00:00.060 | Handler | Facade 호출 |
| 10:00:00.065 | Facade | 트랜잭션 시작 |
| 10:00:00.080 | Mapper | 고객 SQL 실행 |
| 10:00:00.120 | DB | 결과 반환 |
| 10:00:00.130 | ETF | 성공 종료 |
| 10:00:00.145 | Gateway | 응답 전달 |
| 10:00:00.160 | 화면 | 고객정보 표시 |

### 전체 처리시간

사용자 체감시간
\= 화면 요청 생성
\+ 네트워크
\+ Gateway
\+ WAR
\+ TCF
\+ 업무 처리
\+ DB
\+ 응답 렌더링

DB SQL 시간만 보고 전체 응답시간을 판단할 수 없는 이유다.

# 거래 한 건의 장애 여행 예시

## 사례: DB Pool 대기 후 Timeout

화면 조회
↓
Gateway 인증 성공
↓
STF 성공
↓
Dispatcher·Handler 성공
↓
DAO가 Connection 요청
↓
Hikari Pool Connection 없음
↓
Connection 대기
↓
TCF Timeout 3초 초과
↓
업무 Timeout 응답

### 운영 점검 순서

| 순서 | 점검 | 확인값 |
| --- | --- | --- |
| 1 | ServiceId | 어떤 거래인가 |
| 2 | Current Step | DB Connection 대기인가 |
| 3 | Hikari Active | 사용 중 Connection 수 |
| 4 | Hikari Pending | 대기 Thread 수 |
| 5 | Slow SQL | Connection을 오래 점유한 SQL |
| 6 | DB Lock | 장기 대기 원인 |
| 7 | WAR 영향 | 특정 WAR 집중 여부 |
| 8 | Thread | Connector 포화 여부 |

운영 진단은 Tomcat 인스턴스 → WAR → ServiceId → SQL 순서로 원인을 좁히는 방식이 적절하다.

# 정상·경계·실패 사례

| 사례 | 조건 | 기대 결과 | 판단 |
| --- | --- | --- | --- |
| 정상 | 유효 Token·ServiceId·고객 | 성공 응답 | 완료 |
| 경계 | 고객번호 최대길이 | 정책에 따른 처리 | 검증 필요 |
| 경계 | 결과 0건 | 빈 목록 또는 업무 오류 | 정책 필요 |
| 경계 | 동일 조회 반복 | 정상 처리 | 조회 멱등 |
| 경계 | 동일 등록 반복 | 중복 차단 | 멱등성 |
| 실패 | Token 없음 | 401 | TCF 미진입 |
| 실패 | ServiceId 누락 | Header 오류 | Handler 미실행 |
| 실패 | ServiceId 미등록 | 업무 오류 | Catalog 확인 |
| 실패 | 거래통제 중지 | 업무 오류 | Handler 미실행 |
| 실패 | 권한 없음 | 접근 거절 | 보안 확인 |
| 실패 | Slow SQL | Timeout | SQL 분석 |
| 실패 | DB 장애 | 시스템 오류 | Rollback |
| 실패 | ETF 로그 저장 장애 | 업무결과와 로그 정합성 확인 | 운영 위험 |

# 금지 예시

## 금지 1. JWT를 URL Query String에 전달

/sv/online?token=eyJhbGci...

URL은 브라우저 이력·Proxy 로그·Access 로그에 남을 수 있다.

JWT는 Authorization: Bearer Header로 전달한다.

## 금지 2. 화면 Header의 userId를 그대로 신뢰

String userId = request.getHeader().getUserId();
customerService.selectForUser(userId);

검증된 인증 문맥과 정합성을 확인해야 한다.

## 금지 3. Gateway에서 업무 SQL 실행

Gateway가 업무 데이터를 조회하기 시작하면 라우팅 경계가 업무 계층으로 오염된다.

## 금지 4. Handler에서 Mapper 직접 호출

Handler → Mapper

Facade·Service·DAO 경계를 우회하면 트랜잭션과 테스트 구조가 무너진다.

## 금지 5. Timeout 후 무조건 재실행

변경 거래는 실제 DB나 외부 시스템에서 성공했을 수 있다.

멱등성 Key와 상태조회로 처리 여부를 확인해야 한다.

## 금지 6. 시스템 오류를 결과 없음으로 변환

DB 연결 실패
→ 고객 없음 반환

데이터 없음과 시스템 장애는 반드시 구분한다.

# 따라 하는 실무 절차

## 실습 1. 한 거래의 전체 소스 경로 찾기

| 순서 | 수행 내용 | 검색어 |
| --- | --- | --- |
| 1 | 화면 요청 ServiceId 확인 | serviceId |
| 2 | Gateway Route 확인 | businessCode |
| 3 | Controller 확인 | /online |
| 4 | TCF 실행 순서 확인 | tcf.process |
| 5 | STF 처리 확인 | preProcess |
| 6 | Handler 등록 확인 | serviceIds() |
| 7 | Facade Method 확인 | Handler 호출부 |
| 8 | DAO·Mapper 확인 | Mapper Method |
| 9 | SQL ID 확인 | Mapper XML |
| 10 | 테이블 확인 | FROM, UPDATE |

## 실습 2. 로그로 거래 추적하기

다음 식별자를 기준으로 로그를 모은다.

GUID
ServiceId
TraceId
UserId
BranchId

### 로그 추적표

| 로그 구간 | 찾아야 할 내용 |
| --- | --- |
| Gateway | JWT 통과·Target URL |
| Controller | ServiceId·Client IP |
| STF | GUID 생성·정책 통과 |
| Dispatcher | Handler 선택 |
| Handler | Facade 진입 |
| Mapper | SQL ID·처리시간 |
| ETF | 성공·실패·오류코드 |
| Gateway 응답 | HTTP 상태·전체시간 |

## 실습 3. 실패 지점을 분류하기

거래로그가 전혀 없다
→ Gateway·Filter 확인

거래 시작 로그는 있다
Handler 로그는 없다
→ STF·Dispatcher 확인

Handler 로그는 있다
SQL 로그는 없다
→ Facade·Service·Rule 확인

SQL 시작은 있다
종료가 없다
→ DB Lock·Slow SQL 확인

업무 성공 로그는 있다
화면은 오류다
→ ETF·Gateway·화면 응답 처리 확인

# 운영·모니터링 기준

## 반드시 수집해야 할 시간

| 구간 | 측정값 |
| --- | --- |
| Gateway 인증 | JWT 검증시간 |
| Gateway 라우팅 | Route 조회시간 |
| Gateway 전달 | 대상 WAR 왕복시간 |
| STF | 사전 처리시간 |
| Dispatcher | Handler 탐색시간 |
| 업무 Service | 업무 처리시간 |
| DB | SQL별 실행시간 |
| 외부 연계 | 대상별 응답시간 |
| ETF | 사후 처리시간 |
| 전체 | End-to-End 응답시간 |

## 반드시 식별해야 할 대상

Center
Host
Tomcat Instance
WAR
BusinessCode
ServiceId
GUID
TraceId
Mapper ID
External System

# 보안·개인정보·감사 기준

| 항목 | 기준 |
| --- | --- |
| JWT | URL·로그 원문 저장 금지 |
| 사용자 | JWT Claim을 우선 신뢰 |
| 개인정보 | 요청·응답 전체 로그 금지 |
| SQL Parameter | 주민번호·계좌번호 마스킹 |
| Client IP | 신뢰된 Proxy 기준으로 확보 |
| 감사 | 중요 조회·변경·다운로드 기록 |
| 관리자 거래 | 일반 업무보다 강한 감사 적용 |
| 직접 API | 인증·망통제·예외승인 |
| 오류 응답 | Stack Trace·DB 정보 노출 금지 |

# 자동검증 및 품질 Gate

| 검증 항목 | 검증 방식 | 실패 시 |
| --- | --- | --- |
| 화면 이벤트–ServiceId 매핑 | 추적성 매트릭스 검사 | 설계 보완 |
| Handler 등록 | Spring Context 테스트 | 빌드 실패 |
| ServiceId 중복 | Dispatcher 초기화 | 기동 실패 |
| OM 미등록 | Catalog 대조 | 배포 차단 |
| Timeout 미등록 | 정책 검사 | 경고·차단 |
| Mapper 미연결 | 정적 분석 | 코드리뷰 실패 |
| 계층 위반 | ArchUnit | CI 실패 |
| 직접 WAR 의존 | Gradle·ArchUnit | CI 실패 |
| JWT 원문 로그 | 정적·동적 로그 검사 | 보안 실패 |
| 민감정보 응답 | 보안 테스트 | 배포 차단 |
| 거래로그 미종료 | 통합 테스트 | 운영 전환 불가 |

# 테스트 시나리오

| ID | 테스트 | 조건 | 기대 결과 |
| --- | --- | --- | --- |
| CH02-001 | 정상 조회 | 유효 요청 | 성공·S0000 |
| CH02-002 | Header 없음 | Header null | 표준 검증 결과 |
| CH02-003 | 업무코드 URL 보완 | URL에 SV | Header에 SV 적용 |
| CH02-004 | Client IP | XFF 존재 | 첫 번째 IP 사용 |
| CH02-005 | JWT 없음 | Authorization 없음 | 401 |
| CH02-006 | JWT 위변조 | 잘못된 서명 | 401 |
| CH02-007 | 사용자 불일치 | JWT와 Header 다름 | 인증 실패 |
| CH02-008 | 권한 없음 | 기능권한 미보유 | 업무 실패 |
| CH02-009 | 거래 중지 | 통제 비활성 | Handler 미실행 |
| CH02-010 | ServiceId 없음 | 빈 값 | INVALID\_HEADER |
| CH02-011 | ServiceId 미등록 | Unknown ID | SERVICE\_NOT\_FOUND |
| CH02-012 | ServiceId 중복 | 두 Handler 등록 | 기동 실패 |
| CH02-013 | 업무 오류 | 고객 없음 | 업무 오류 |
| CH02-014 | DB 오류 | SQL 실패 | 시스템 오류 |
| CH02-015 | Timeout | 3초 초과 | TIMEOUT\_ONLINE |
| CH02-016 | 중복 등록 | 동일 멱등 Key | 중복 차단 |
| CH02-017 | Rollback | 변경 중 오류 | 전체 Rollback |
| CH02-018 | 로그 추적 | GUID 검색 | 전체 단계 연결 |
| CH02-019 | Thread Context | 연속 사용자 요청 | 문맥 잔존 없음 |
| CH02-020 | Gateway Route 없음 | 미등록 업무코드 | 라우팅 실패 |
| CH02-021 | Gateway 장애 | 대상 WAR 불가 | 명확한 Proxy 오류 |
| CH02-022 | 직접 인입 | TCF 우회 API | 통제 누락 확인 |
| CH02-023 | ETF 실패 | 로그 저장 장애 | 데이터·로그 정합성 확인 |
| CH02-024 | Timeout 후 상태 | 변경 처리 지연 | 멱등·상태조회 확인 |

# 완료 체크리스트

## 구조 이해

| 확인 항목 | 완료 |
| --- | --- |
| 화면에서 DB까지의 전체 경로를 설명할 수 있다. | □ |
| Gateway·TCF·업무 WAR의 역할을 구분할 수 있다. | □ |
| Controller가 업무 Service를 직접 호출하지 않는 이유를 설명할 수 있다. | □ |
| STF의 처리 순서를 설명할 수 있다. | □ |
| Dispatcher가 Handler를 찾는 방식을 설명할 수 있다. | □ |
| Facade 트랜잭션 경계를 설명할 수 있다. | □ |
| ETF의 세 가지 종료 경로를 설명할 수 있다. | □ |

## 소스 확인

| 확인 항목 | 완료 |
| --- | --- |
| GatewayAuthenticationService를 확인했다. | □ |
| GatewayJwtValidator를 확인했다. | □ |
| OnlineTransactionController를 확인했다. | □ |
| TCF.process()를 확인했다. | □ |
| STF.preProcess()를 확인했다. | □ |
| OnlineTransactionTimeoutExecutor를 확인했다. | □ |
| TransactionDispatcher를 확인했다. | □ |
| TransactionHandler.serviceIds()를 확인했다. | □ |
| ETF.success/businessFail/systemError를 확인했다. | □ |
| TCF 우회 샘플 Controller를 확인했다. | □ |

## 실행 검증

| 확인 항목 | 완료 |
| --- | --- |
| 정상 거래 한 건을 실행했다. | □ |
| 미등록 ServiceId를 호출했다. | □ |
| 업무 오류를 재현했다. | □ |
| 시스템 오류를 재현했다. | □ |
| Timeout을 재현했다. | □ |
| GUID로 Gateway부터 ETF까지 추적했다. | □ |
| SQL ID와 테이블을 연결했다. | □ |
| Rollback 결과를 DB에서 확인했다. | □ |

# 제2장의 핵심 정리

첫째,
거래는 화면 이벤트에서 시작해
표준 요청·Gateway·TCF·업무 계층·DB를 거쳐
표준 응답으로 돌아온다.

둘째,
Gateway는 외부 요청의 인증과 목적지를 통제하고,
TCF는 거래 실행 절차를 통제하며,
업무 WAR는 실제 업무를 처리한다.

셋째,
ServiceId는 화면 요청과 Handler를 연결하는
논리적 거래 주소다.

넷째,
STF가 실패하면 Handler는 실행되지 않으며,
업무 프로그램이 성공해야 ETF가 정상 종료를 수행한다.

다섯째,
업무 오류와 시스템 오류는
사용자 행동·Rollback·운영 대응이 다르므로
반드시 구분해야 한다.

여섯째,
Timeout 응답이 발생했다고
실제 데이터 처리가 반드시 실패한 것은 아니다.

일곱째,
한 거래의 완료 여부는
응답뿐 아니라 GUID 로그·트랜잭션·DB 결과로 증명한다.

# 시사점

## 핵심 아키텍처 판단

거래 흐름을 설계할 때 가장 중요한 것은 계층을 많이 만드는 것이 아니다.

각 경계의 책임을 명확히 하고, 경계를 통과할 때 필요한 계약과 증적을 남기는 것이다.

Gateway
\= 신뢰 경계

TCF
\= 실행 통제 경계

Facade
\= 업무 유스케이스·트랜잭션 경계

DAO·Mapper
\= 데이터 접근 경계

ETF
\= 거래 종료 경계

## 주요 위험

| 위험 | 영향 |
| --- | --- |
| TCF 직접 우회 | 거래통제·로그·감사 누락 |
| Gateway와 TCF Timeout 불일치 | 표준 응답 전 연결 종료 |
| ServiceId 미관리 | 잘못된 프로그램 실행 |
| JWT 사용자 불일치 | 사용자 위·변조 |
| Timeout 후 무조건 재실행 | 중복 처리 |
| 업무·시스템 오류 혼합 | 복구 판단 실패 |
| 다중 WAR 자원 공유 무시 | 장애 확산 |
| Thread Local 정리 누락 | 사용자 문맥 오염 |
| ETF 실패 미대응 | 데이터·로그 정합성 훼손 |

## 우선 보완 과제

1.  화면 이벤트–ServiceId–Handler–SQL 추적성 매트릭스를 공식 산출물로 관리한다.
2.  ServiceId와 OM Catalog의 자동 대조 기능을 만든다.
3.  Gateway·TCF·SQL·외부 Client Timeout 계층을 정합화한다.
4.  Timeout 후 처리상태 확인과 멱등성 정책을 구체화한다.
5.  직접 인입 API를 식별하고 예외승인·폐기계획을 수립한다.
6.  거래 단계별 처리시간과 Current Step을 운영 화면에 제공한다.
7.  학습용 System.out 로그를 구조화된 운영 Logger로 전환한다.

## 중장기 발전 방향

단순 거래로그
↓
GUID 기반 End-to-End 추적
↓
ServiceId별 단계시간 측정
↓
Current Step·Slow SQL 식별
↓
원인 후보 자동 판정
↓
운영 정책 자동 최적화

# 마무리말

거래 한 건은 단순히 Controller에서 Service를 호출하는 짧은 흐름이 아니다.

사용자의 요청은 여러 신뢰 경계와 실행 경계를 통과하며, 각 단계에서 검증·통제·기록된다.

화면은 무엇을 요청할지 결정한다.

Gateway는 어디로 보낼지 결정한다.

TCF는 실행해도 되는지와
어떤 순서로 실행할지 결정한다.

ServiceId는 누가 처리할지 결정한다.

업무 프로그램은 무엇을 처리할지 결정한다.

ETF는 거래가 어떻게 끝났는지를 기록한다.

다음 장에서는 이 여행을 가능하게 하는 프로젝트의 실제 구조를 살펴본다. Gradle 멀티모듈, tcf-core, tcf-web, 업무 WAR, Handler·Facade·Service·DAO·Mapper가 소스 저장소 안에서 어떻게 나뉘는지 학습한다.
