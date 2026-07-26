
## 초보 IT 개발자를 위한 NSIGHT TCF 개발 입문서

## 제13부. TCF Framework 내부구조와 공통모듈 심화

## 1. 도입 전 안내말

제12부에서는 화면과 테이블 정보만 주어진 상태에서 요구사항 분석부터 구현, 테스트, OM 등록과 배포까지 하나의 업무기능을 완성했습니다.

업무 개발자는 일반적으로 다음 계층을 구현합니다.

```
Handler
→ Facade
→ Service
→ Rule
→ DAO
→ Mapper
```

그런데 업무 프로그램이 실행되려면 그 앞뒤에서 많은 공통기능이 동작해야 합니다.

```
HTTP 요청 수신

JSON 전문 변환

JWT 검증

ServiceId 확인

거래통제

Timeout 적용

Idempotency 확인

거래로그 시작

Handler 탐색

예외 변환

표준 응답 생성

거래로그 종료
```

이 기능을 모든 업무 WAR가 각각 구현하면 다음 문제가 발생합니다.

```
업무별 구현방식이 달라진다.

오류코드와 응답 형식이 달라진다.

보안 검증이 일부 업무에서 누락된다.

Timeout과 거래통제가 일관되지 않다.

공통 기능 수정 시 모든 업무 WAR를 변경해야 한다.

운영자가 업무별 차이를 모두 알아야 한다.
```

따라서 NSIGHT TCF는 공통 기능을 여러 모듈로 분리합니다.

```
tcf-util
tcf-core
tcf-web
tcf-jwt
tcf-gateway
tcf-cache
tcf-eai
tcf-batch
tcf-om
```

각 모듈은 서로 다른 책임을 가집니다.

```
tcf-core
= 거래 처리 엔진

tcf-web
= HTTP 요청 진입과 응답 연결

tcf-jwt
= 토큰 발급·검증 공통기능

tcf-gateway
= 외부 요청 인증·라우팅

tcf-cache
= Cache 공통 추상화

tcf-eai
= 외부 시스템 연계 공통기능

tcf-batch
= Batch 실행 공통기반

tcf-om
= 운영 기준정보와 관측·통제
```

Framework를 이해할 때 가장 중요한 원칙은 다음과 같습니다.

```
공통모듈은 업무를 대신하지 않는다.

업무모듈은 공통기능을 다시 구현하지 않는다.

공통모듈은 확장점을 제공하고,
업무모듈은 정해진 계약에 맞춰 확장한다.

공통모듈 변경은
모든 업무 WAR에 미치는 영향을 검토해야 한다.
```

## 2. 제13부 개요

### 2.1 목적

제13부의 목적은 초보 개발자가 NSIGHT TCF 공통모듈의 역할과 실행 흐름을 이해하고, Framework를 우회하거나 과도하게 변경하지 않으면서 안전하게 기능을 확장하도록 돕는 것입니다.

학습을 마친 뒤에는 다음 작업을 수행할 수 있어야 합니다.

- TCF 공통모듈의 전체 구성을 설명한다.
- tcf-core의 거래 처리 흐름을 이해한다.
- STF와 ETF의 책임을 구분한다.
- Dispatcher와 Handler Registry의 역할을 설명한다.
- tcf-web의 Controller와 Filter 역할을 이해한다.
- tcf-jwt의 발급·검증·키 관리 구조를 이해한다.
- tcf-gateway의 인증·라우팅·Header 재작성 책임을 이해한다.
- tcf-cache를 이용해 Cache 구현기술과 업무코드를 분리한다.
- tcf-eai의 Adapter와 외부 오류 변환 구조를 이해한다.
- tcf-batch의 Job 실행과 운영 Metadata를 이해한다.
- tcf-om의 기준정보·관측·통제 책임을 이해한다.
- 공통모듈 간 허용 의존성과 금지 의존성을 구분한다.
- Framework 확장점과 업무 확장점을 구분한다.
- 공통모듈 변경 시 호환성·배포 영향을 분석한다.
- Architecture Gate로 모듈 경계를 자동검증한다.

### 2.2 적용범위

| 모듈 | 주요 내용 |
| --- | --- |
| tcf-util | 순수 Utility·공통 값 객체 |
| tcf-core | TCF·STF·ETF·Dispatcher |
| tcf-web | Controller·Filter·Web 예외 |
| tcf-jwt | JWT 발급·검증·JWKS |
| tcf-gateway | 인증·라우팅·접근통제 |
| tcf-cache | Cache 추상화·정책 |
| tcf-eai | 외부연계·전문 변환 |
| tcf-batch | Job·Step·재시작 |
| tcf-om | Catalog·정책·Metric·Control |
| 업무 WAR | Handler 이하 업무 구현 |
| 공통 계약 | Context·Header·Response·Exception |
| 품질 | 모듈 의존성·호환성·자동검증 |

### 2.3 대상 독자

- TCF 요청이 Handler까지 어떻게 도달하는지 궁금한 개발자
- 공통 Controller를 수정하려는 개발자
- 업무코드에서 JWT를 직접 해석하는 개발자
- Gateway와 업무 WAR의 인증 책임이 혼란스러운 개발자
- Cache와 EAI 공통모듈을 어떻게 사용해야 할지 모르는 개발자
- Framework 공통기능을 추가해야 하는 개발자
- 공통 JAR 변경이 여러 WAR에 미치는 영향을 분석해야 하는 개발자
- TCF 내부 장애를 진단해야 하는 운영 개발자

### 2.4 선행조건

다음 내용을 이해하고 있어야 합니다.

```
표준 요청은 Header와 Body로 구성된다.

ServiceId는 실행할 업무기능을 식별한다.

Handler는 Facade를 호출한다.

TransactionContext는 거래 공통정보를 보관한다.

STF는 거래 시작 전 검증을 수행한다.

ETF는 거래 종료 후 표준 응답을 생성한다.

OM은 ServiceId별 운영정책을 관리한다.
```

### 2.5 주요 용어

| 용어 | 쉬운 설명 |
| --- | --- |
| Framework | 여러 업무가 공통으로 사용하는 실행 기반 |
| Core Engine | 거래 처리의 중심 실행부 |
| Pipeline | 정해진 순서대로 처리하는 구조 |
| Hook | 특정 시점에 추가 기능을 연결하는 확장점 |
| Registry | 구현체를 등록하고 찾는 저장소 |
| Dispatcher | ServiceId에 맞는 Handler를 선택 |
| Context | 거래 실행 중 공유하는 정보 |
| Filter | Controller 전에 HTTP 요청을 처리 |
| Interceptor | 메서드 실행 전후에 공통처리 |
| Adapter | 서로 다른 시스템 형식을 변환 |
| SPI | 구현체를 교체·추가할 수 있는 계약 |
| Backward Compatibility | 기존 사용자가 계속 동작하는 호환성 |
| Binary Compatibility | 재컴파일 없이 기존 코드가 동작하는 호환성 |
| Semantic Compatibility | 형식뿐 아니라 의미가 유지되는 호환성 |
| Fail-fast | 잘못된 설정을 기동 단계에서 즉시 차단 |

## 제114장. TCF 공통모듈 전체 지도

### 114.1 전체 모듈 구조

```
사용자·외부 시스템
        │
        ▼
   tcf-gateway
        │
        ▼
     업무 WAR
        │
   ┌────┴────┐
   ▼         ▼
tcf-web   업무 Handler
   │
   ▼
tcf-core
   │
   ├─ tcf-jwt
   ├─ tcf-cache
   ├─ tcf-eai
   ├─ tcf-batch
   └─ tcf-om 연계
```

### 114.2 모듈별 핵심 책임

| 모듈 | 핵심 책임 |
| --- | --- |
| tcf-util | 공통 Utility와 기반형 |
| tcf-core | 거래 Pipeline과 Context |
| tcf-web | HTTP·JSON·Controller |
| tcf-jwt | Token 발급·검증 |
| tcf-gateway | 진입 인증·Route |
| tcf-cache | Cache 공통 API |
| tcf-eai | 외부기관 연계 |
| tcf-batch | 대량 Job 실행 |
| tcf-om | 운영정책·관측·통제 |
| 업무 WAR | 실제 업무기능 |

### 114.3 업무 WAR가 직접 구현할 것

```
업무 DTO

Handler

Facade

Service

Rule

DAO

Mapper

업무별 Client

업무 오류코드

업무 테스트
```

### 114.4 업무 WAR가 다시 구현하지 않을 것

```
공통 온라인 Controller

JWT Signature 검증

ServiceId Registry

거래통제 Engine

공통 Timeout Executor

표준 오류 응답

거래로그 공통형식

공통 Cache Provider 선택

공통 EAI 연결관리
```

### 114.5 공통과 업무의 경계

다음 질문을 사용합니다.

```
모든 업무가 동일하게 사용해야 하는가?

업무 의미 없이 기술적으로 설명할 수 있는가?

보안·운영 일관성이 필요한가?

구현 차이가 장애나 감사 문제를 일으키는가?
```

대부분 예라면 공통모듈 후보입니다.

반대로 다음 질문에 해당하면 업무모듈이 담당합니다.

```
특정 업무상태를 판단하는가?

특정 테이블과 업무용어를 사용하는가?

업무 담당자의 승인이 필요한가?

업무별로 결과가 달라질 수 있는가?
```

### 114.6 공통모듈의 위험

공통모듈 오류는 여러 업무에 동시에 영향을 줍니다.

예:

```
tcf-core Timeout 오류
→ 모든 업무 거래 영향

tcf-jwt 검증 오류
→ 전체 인증 실패

tcf-web JSON 변환 오류
→ 모든 온라인 요청 실패
```

따라서 공통모듈은 업무코드보다 더 엄격한 테스트와 배포 통제가 필요합니다.

### 114.7 공통모듈 배포 방식

공통 JAR가 각 업무 WAR 내부에 포함되는 방식:

```
sv-service.war
└─ WEB-INF/lib/tcf-core-1.5.0.jar

ic-service.war
└─ WEB-INF/lib/tcf-core-1.5.0.jar
```

공통모듈 변경 후 실제 운영 반영을 위해서는 영향을 받는 WAR를 다시 빌드·배포해야 합니다.

### 114.8 공통 JAR를 Tomcat 공용 lib에 둘 경우

```
Tomcat/lib
└─ tcf-core.jar
```

장점:

- WAR별 중복 감소
- 공통 버전 통일
위험:

- 모든 WAR가 동일 버전에 강제 결합
- 공통 JAR 교체 시 Tomcat 전체 재기동
- ClassLoader 충돌
- 업무별 독립 Rollback 어려움
초기에는 WAR 내부 포함 방식이 독립성과 추적성 측면에서 관리하기 쉬울 수 있습니다.

### 제114장 요약

```
공통모듈은 기술 표준과 운영 일관성을 담당한다.

업무모듈은 업무 규칙과 데이터를 담당한다.

공통모듈 오류는 영향범위가 크므로
변경·테스트·배포를 더 엄격하게 관리한다.
```

## 제115장. tcf-util 기반모듈

### 115.1 tcf-util의 목적

tcf-util은 다른 공통모듈과 업무모듈에서 사용할 수 있는 가장 낮은 수준의 기반기능을 제공합니다.

예:

```
문자열 처리

날짜 변환

Masking

Hash

ID 생성 기반

불변 값 객체

공통 상수
```

### 115.2 tcf-util에 적합한 코드

```
public final class MaskingUtils {

    private MaskingUtils() {
    }

    public static String maskCustomerNo(
            String customerNo) {

        if (customerNo == null
                || customerNo.length() < 4) {
            return "****";
        }

        return customerNo.substring(0, 2)
            + "****"
            + customerNo.substring(
                customerNo.length() - 2
            );
    }
}
```

다음 조건을 만족해야 합니다.

```
Spring Context가 없어도 동작한다.

DB에 접근하지 않는다.

특정 업무코드를 알지 않는다.

상태를 보유하지 않는다.

입력에 대해 예측 가능한 결과를 반환한다.
```

### 115.3 tcf-util에 넣으면 안 되는 코드

```
고객등급 계산

캠페인 승인 판단

ServiceId 거래통제

DB 조회

JWT 검증

Spring Bean 의존 기능
```

이름이 Utility라고 해서 책임이 불명확한 코드를 모으는 장소로 사용하면 안 됩니다.

### 115.4 Common과 Util 차이

```
Util
= 상태 없는 기술 보조기능

Common Service
= 업무적으로 공통인 기능
```

예:

```
날짜 문자열 변환
→ tcf-util

공통코드 조회
→ cc-service 또는 공통 업무 Service

ServiceId 정책조회
→ tcf-core·tcf-om
```

### 115.5 의존성 원칙

tcf-util은 상위 공통모듈에 의존하지 않아야 합니다.

권장:

```
tcf-core
→ tcf-util
```

금지:

```
tcf-util
→ tcf-core
```

순환 의존을 방지합니다.

### 115.6 Utility 예외

Utility에서 업무 예외를 발생시키지 않습니다.

금지:

```
throw new BusinessException(
    "E-SV-CUST-0001",
    "고객번호 오류"
);
```

Utility는 일반적인 예외나 검증 결과를 제공하고, 업무 오류코드 변환은 상위 Rule·Service가 담당합니다.

### 제115장 요약

```
tcf-util은 가장 낮은 기술 기반모듈이다.

업무코드·DB·Spring Bean에 의존하지 않는다.

상태 없는 순수 기능만 제공하고
공통업무 Service와 혼동하지 않는다.
```

## 제116장. tcf-core 거래 처리 엔진

### 116.1 tcf-core의 역할

tcf-core는 TCF 거래 처리의 중심입니다.

```
표준 요청 수신

TransactionContext 생성

STF 실행

Timeout 적용

Handler 탐색

업무 실행

ETF 실행

표준 결과 생성
```

### 116.2 전체 처리 흐름

```
TCF.process()
    │
    ▼
STF.preProcess()
    │
    ▼
TimeoutExecutor.execute()
    │
    ▼
TransactionDispatcher.dispatch()
    │
    ▼
TransactionHandler.handle()
    │
    ▼
Facade·Service·DAO
    │
    ▼
ETF.postProcess()
    │
    ▼
StandardResponse
```

### 116.3 TCF 진입점

개념 예:

```
public class TcfEngine {

    private final StandardTransactionFacade stf;
    private final TransactionTimeoutExecutor timeoutExecutor;
    private final TransactionDispatcher dispatcher;
    private final EndTransactionFacade etf;

    public StandardResponse process(
            StandardRequest request) {

        TransactionContext context = null;

        try {
            context = stf.preProcess(request);

            Object result =
                timeoutExecutor.execute(
                    context.getTimeoutMillis(),
                    () -> dispatcher.dispatch(
                        request,
                        context
                    )
                );

            return etf.completeSuccess(
                context,
                result
            );

        } catch (BusinessException e) {
            return etf.completeBusinessFailure(
                context,
                e
            );

        } catch (TransactionTimeoutException e) {
            return etf.completeTimeout(
                context,
                e
            );

        } catch (Exception e) {
            return etf.completeSystemFailure(
                context,
                e
            );

        } finally {
            TransactionContextHolder.clear();
            AuthenticationContextHolder.clear();
            MDC.clear();
        }
    }
}
```

실제 클래스명과 Signature는 구현 기준에 따라 달라질 수 있습니다.

### 116.4 TransactionContext

거래 실행 중 공통으로 사용하는 정보입니다.

예:

```
public class TransactionContext {

    private String guid;
    private String traceId;
    private String serviceId;
    private String transactionCode;
    private String businessCode;
    private String userId;
    private String branchId;
    private String channelId;
    private long timeoutMillis;
    private Instant startTime;
    private AuthenticationContext authentication;
}
```

### 116.5 Context의 목적

업무계층이 다음 정보를 반복해서 Header에서 해석하지 않도록 합니다.

```
인증 사용자

지점

ServiceId

거래코드

TraceId

Timeout

채널
```

### 116.6 Context 변경 금지

업무코드가 TransactionContext의 인증 사용자와 ServiceId를 임의로 변경하면 안 됩니다.

금지:

```
context.setUserId(
    request.userId()
);
```

Context는 Framework가 검증한 신뢰정보입니다.

### 116.7 Pipeline 확장

새 공통단계를 추가하고 싶다고 가정합니다.

예:

```
개인정보 조회 감사 사전검증
```

무조건 TCF.process() 안에 코드를 직접 추가하기보다 Processor Chain 또는 Hook을 검토합니다.

```
public interface PreTransactionProcessor {

    int order();

    void process(
        StandardRequest request,
        TransactionContext context
    );
}
```

등록:

```
Header Validation

Context Initialization

Authentication

Authorization

Transaction Control

Timeout Policy

Idempotency

Transaction Log
```

### 116.8 Processor 순서

순서는 업무 결과에 영향을 줍니다.

예:

```
인증 전 거래로그 시작
→ 인증 실패도 추적 가능

권한검증 전 Idempotency 확인
→ 다른 사용자의 결과노출 위험
```

따라서 순서를 Architecture Decision으로 관리합니다.

권장 개념:

```
Header 검증

GUID·Trace 생성

Context 설정

인증

권한

거래통제

Timeout

Idempotency

거래로그 시작
```

### 116.9 tcf-core에서 하지 않을 일

```
HTTP Header 직접 읽기

Servlet API 직접 사용

특정 업무 DB 조회

화면 Redirect

특정 업무 오류 메시지 작성

외부 시스템별 전문 변환
```

tcf-core는 Web이나 업무기술에 최대한 독립적으로 유지합니다.

### 116.10 Fail-fast 등록검증

애플리케이션 기동 시 다음을 검증합니다.

```
ServiceId 중복

Handler 미등록

필수 Processor 누락

OM 정책 Provider 누락

Timeout Executor 누락

ETF 구현체 누락
```

요청이 들어온 후 처음 발견하는 것보다 기동 단계에서 차단하는 것이 안전합니다.

### 제116장 요약

```
tcf-core는 거래 Pipeline과 Context를 관리한다.

업무코드는 TCF Engine을 우회하지 않는다.

공통단계를 확장할 때는 Hook·Processor 계약을 사용하고
처리순서를 명시적으로 관리한다.
```

## 제117장. STF·Dispatcher·ETF 내부구조

### 117.1 STF란 무엇인가요?

STF는 Standard Transaction Front의 개념으로 거래 실행 전에 공통 준비와 검증을 수행합니다.

```
요청을 받을 수 있는가?

누가 호출했는가?

실행 권한이 있는가?

현재 거래가 허용되는가?

얼마나 실행할 수 있는가?

중복 요청인가?
```

### 117.2 STF 단계

```
StandardHeaderValidator

GUID·TraceId 생성

TransactionContext 설정

Session·Authentication 확인

Authorization 확인

Transaction Control 확인

Timeout 정책 조회

Idempotency 확인

거래로그 시작
```

### 117.3 Header Validator

검증항목:

```
businessCode 필수

serviceId 필수

transactionCode 필수

처리유형 형식

업무코드와 ServiceId Prefix 일치

Context Path와 업무코드 일치

허용 Channel
```

### 117.4 인증과 권한

```
Authentication
= 호출자가 누구인가

Authorization
= 해당 ServiceId를 실행할 수 있는가
```

STF는 검증된 AuthenticationContext를 사용합니다.

### 117.5 거래통제

OM 정책:

```
ACTIVE
SUSPENDED
READ_ONLY
DISABLED
MAINTENANCE
```

예:

```
처리유형 UPDATE
+ Service 상태 READ_ONLY
→ 실행 차단
```

### 117.6 Timeout 정책

정책 우선순위 예:

```
ServiceId별 Timeout

업무별 기본 Timeout

시스템 기본 Timeout
```

Header에서 임의로 보낸 Timeout을 그대로 사용하지 않습니다.

### 117.7 Dispatcher

Dispatcher는 ServiceId에 맞는 Handler를 찾습니다.

```
public Object dispatch(
        StandardRequest request,
        TransactionContext context) {

    TransactionHandler handler =
        handlerRegistry.find(
            context.getServiceId()
        );

    return handler.handle(
        context.getServiceId(),
        request.getBody(),
        context
    );
}
```

### 117.8 Handler Registry

기동 시 Handler의 serviceIds()를 수집합니다.

```
CtContactHandler
→ CT.Contact.selectList
→ CT.Contact.selectDetail
→ CT.Contact.create
```

중복 ServiceId가 있으면 기동 실패로 처리합니다.

### 117.9 동적 등록 주의

운영 중 새 Handler를 동적으로 등록하는 방식은 다음 위험이 있습니다.

```
서버별 Registry 불일치

ClassLoader 문제

승인되지 않은 기능 실행

Rollback 어려움
```

일반적으로 코드 배포와 기동검증을 통해 등록합니다.

### 117.10 ETF란 무엇인가요?

ETF는 End Transaction Facade의 개념으로 거래 결과를 최종 정리합니다.

```
결과상태 확정

오류코드 변환

표준 응답 생성

거래로그 종료

Metric 기록

Idempotency 결과 저장

Context 정리
```

### 117.11 ETF 성공 처리

```
resultStatus = SUCCESS

resultCode = S0000

Body = 업무 결과

거래로그 = SUCCESS

Metric = success 증가
```

### 117.12 ETF 업무 오류

```
resultStatus = FAIL

errorType = BUSINESS

resultCode = 업무 오류코드

재시도 여부 = false 또는 정책값
```

내부 Stack Trace는 사용자 응답에 포함하지 않습니다.

### 117.13 ETF 시스템 오류

```
resultStatus = ERROR

errorType = SYSTEM

외부 메시지 = 표준 시스템 오류

내부 로그 = 예외 상세
```

### 117.14 ETF Timeout

```
resultStatus = ERROR

errorType = TIMEOUT

거래로그 = TIMEOUT

Idempotency = TIMEOUT 또는 UNKNOWN
```

변경 거래는 실제 반영 여부가 불명확할 수 있습니다.

### 117.15 ETF 실패 자체

ETF가 거래로그 DB 장애 때문에 실패할 수도 있습니다.

정책을 분리합니다.

```
필수 감사로그 실패
→ 거래 실패 검토

운영 상세로그 실패
→ 업무 결과 유지
→ 비동기 재기록·경보
```

### 제117장 요약

```
STF는 거래 전 인증·권한·통제·Timeout을 담당한다.

Dispatcher는 ServiceId에 맞는 Handler를 찾는다.

ETF는 성공·업무 오류·시스템 오류·Timeout을
표준 응답과 운영기록으로 마무리한다.
```

## 제118장. tcf-web HTTP 진입계층

### 118.1 tcf-web의 역할

tcf-web은 HTTP 세계와 tcf-core 거래엔진을 연결합니다.

```
Servlet·HTTP 요청

JSON 변환

공통 Controller

Web Filter

HTTP 상태

표준 응답 직렬화
```

### 118.2 공통 Controller

```
@RestController
@RequiredArgsConstructor
public class OnlineTransactionController {

    private final TcfEngine tcfEngine;

    @PostMapping("/{businessCode}/online")
    public StandardResponse process(
            @PathVariable String businessCode,
            @RequestBody StandardRequest request) {

        request.getHeader()
            .setPathBusinessCode(businessCode);

        return tcfEngine.process(request);
    }
}
```

실제 Endpoint는 업무 Context Path와 배포구조에 따라 달라질 수 있습니다.

### 118.3 업무 Controller를 만들지 않는 이유

```
인증·권한 우회 가능

표준 Header 누락

거래통제 미적용

Timeout 미적용

거래로그 누락

오류응답 형식 차이
```

파일 업로드, Health Check 등 표준 JSON 거래와 특성이 다른 기능은 별도 공통 Endpoint를 사용할 수 있습니다.

### 118.4 Web Filter

Filter 후보:

```
Request Size 제한

Correlation ID

보안 Header

JWT 사전검증

CORS

Character Encoding

Request Wrapper

관리 Endpoint 접근통제
```

### 118.5 Filter 순서

예:

```
Encoding

Correlation ID

Security Header

JWT Authentication

Request Size

Controller
```

순서가 잘못되면 인증 실패 로그에 TraceId가 없거나 Body가 이미 소비되는 문제가 발생할 수 있습니다.

### 118.6 Request Body 재사용

HTTP Body는 일반적으로 한 번만 읽을 수 있습니다.

Logging Filter가 Body를 먼저 읽으면 Controller가 읽지 못할 수 있습니다.

필요하면 안전한 Caching Wrapper를 사용하되 대용량 Body를 메모리에 복사하지 않도록 제한합니다.

### 118.7 JSON 표준

tcf-web은 다음을 표준화할 수 있습니다.

```
날짜·시간 형식

Null 처리

알 수 없는 필드

숫자 정밀도

Enum 변환

문자 인코딩
```

### 118.8 알 수 없는 필드

정책 선택:

```
무시
→ 하위 호환성에 유리

오류
→ 계약오류 조기 발견
```

외부 공개 API와 내부 거래의 요구가 다를 수 있습니다.

### 118.9 HTTP 상태와 업무 결과

다음 두 정책 중 하나를 선택할 수 있습니다.

#### HTTP 상태 활용

```
400 Validation

401 인증

403 권한

500 시스템 오류
```

#### HTTP 200 + 표준 Result

```
HTTP 통신 성공
→ 업무 결과는 Body의 resultStatus
```

프로젝트 표준을 통일해야 합니다.

Gateway, 화면, 모니터링이 같은 기준으로 해석해야 합니다.

### 118.10 Web 예외 처리

Controller 이전의 오류:

```
JSON 형식 오류

Body 크기 초과

지원하지 않는 Content-Type

필수 Header 누락

인증 Filter 실패
```

이 오류도 가능한 범위에서 표준 오류 응답으로 변환합니다.

### 118.11 관리 Endpoint

```
/health

/ready

/internal/runtime

/internal/cache
```

관리 Endpoint는 일반 사용자 Endpoint와 분리하고 내부망·인증·권한을 적용합니다.

### 제118장 요약

```
tcf-web은 HTTP·JSON과 tcf-core를 연결한다.

업무별 Controller를 새로 만들어 공통 Pipeline을 우회하지 않는다.

Filter·JSON·HTTP 상태정책은
모든 WAR에서 동일하게 유지해야 한다.
```

## 제119장. tcf-jwt 토큰 공통모듈

### 119.1 tcf-jwt의 역할

```
Access Token 발급

Refresh Token 발급

JWT Signature 생성

JWT 검증

Claim 표준화

JWKS 제공

Key Rotation

Token 오류 변환
```

### 119.2 발급과 검증 책임

발급자:

```
Private Key 보유

Token 서명

issuer·audience·exp 설정
```

검증자:

```
Public Key 보유

Signature 검증

issuer·audience·exp 확인
```

### 119.3 Token Issuer

개념 예:

```
public interface JwtTokenIssuer {

    TokenPair issue(
        AuthenticatedUser user,
        TokenIssuePolicy policy
    );

    TokenPair refresh(
        String refreshToken
    );
}
```

### 119.4 표준 Claim

| Claim | 의미 |
| --- | --- |
| sub | 사용자 식별자 |
| iss | 발급자 |
| aud | 대상 시스템 |
| iat | 발급시각 |
| exp | 만료시각 |
| jti | Token ID |
| roles | 역할 |
| branchId | 지점 |
| tokenType | Access·Refresh |

민감정보 전체를 Claim에 넣지 않습니다.

### 119.5 검증 순서

```
Token 존재

형식 확인

허용 Algorithm 확인

kid로 Public Key 선택

Signature 검증

issuer 확인

audience 확인

exp·nbf 확인

Token Type 확인

필수 Claim 확인

사용자 상태 확인
```

### 119.6 Algorithm 고정

Token Header의 Algorithm을 무조건 신뢰하면 안 됩니다.

```
서버 정책
RS256만 허용
```

Token이 none이나 다른 Algorithm을 요청해도 거부합니다.

### 119.7 JWKS

JWKS는 Public Key 정보를 제공합니다.

```
kid

kty

alg

n

e
```

검증자는 kid로 적절한 Key를 선택합니다.

### 119.8 Key Rotation

```
신규 Private Key 생성

신규 kid 발급

JWKS에 구·신 Public Key 병행

신규 Token은 새 Key로 발급

기존 Token 만료 대기

구 Key 제거
```

구 Key를 즉시 제거하면 아직 유효한 기존 Token이 모두 실패합니다.

### 119.9 Refresh Token

Access Token보다 긴 수명을 가질 수 있으므로 더 강하게 보호합니다.

```
저장 위치 제한

Rotation

재사용 탐지

폐기

사용자 로그아웃 처리

탈취 대응
```

### 119.10 업무코드의 JWT 직접 해석 금지

금지:

```
Claims claims =
    jwtParser.parse(token);

String userId =
    claims.get("sub");
```

업무코드는 AuthenticationContext를 사용합니다.

JWT Library와 Claim 구조 변경이 업무코드에 전파되지 않도록 합니다.

### 119.11 Token 오류

| 상황 | 오류 |
| --- | --- |
| Token 없음 | 인증 필요 |
| Signature 오류 | 위조 Token |
| 만료 | 재로그인·Refresh |
| Audience 오류 | 잘못된 대상 |
| kid 없음 | Key 갱신 필요 |
| Claim 누락 | 계약 오류 |
| Refresh 재사용 | 보안사고 후보 |

### 제119장 요약

```
tcf-jwt는 Token 기술과 Claim 표준을 공통화한다.

Private Key는 발급자만 보유하고,
업무 WAR는 Public Key로 검증한다.

업무코드는 JWT를 직접 파싱하지 않고
검증된 AuthenticationContext를 사용한다.
```

## 제120장. tcf-gateway 진입·인증·라우팅

### 120.1 Gateway의 역할

```
외부 요청 수신

인증

Route

Header 정리

Rate Limit

접근통제

공통 로그

하위 응답 전달
```

### 120.2 Gateway 흐름

```
Client
→ Gateway Controller
→ RouteService
→ Gateway Authentication
→ JWT Validator
→ Header Rebuild
→ 대상 업무 WAR
```

### 120.3 Route 기준

예:

```
/sv/**
→ sv-service

/ic/**
→ ic-service

/cm/**
→ cm-service
```

또는 Header의 업무코드를 함께 확인합니다.

```
Path = /sv
businessCode = SV
serviceId Prefix = SV
```

불일치하면 차단합니다.

### 120.4 Header 제거와 재작성

외부 Client가 다음 Header를 위조할 수 있습니다.

```
X-Authenticated-User

X-Internal-Role

X-Branch-Id
```

Gateway는 외부에서 들어온 내부 신뢰 Header를 제거하고 검증된 Token 기준으로 다시 생성합니다.

### 120.5 Gateway가 하면 안 되는 일

```
고객 상태 판단

캠페인 승인 Rule

업무 DB 조회

업무 응답 조합

업무 Mapper 호출
```

Gateway는 공통 진입통제와 Route만 담당합니다.

### 120.6 Gateway와 업무 WAR 2차 검증

Gateway가 검증했더라도 업무 WAR는 최소 검증을 유지합니다.

```
Signature·issuer·audience 확인

내부 Header 정합성

직접 접근 차단

ServiceId 권한
```

Gateway 우회 경로가 존재할 수 있기 때문입니다.

### 120.7 Gateway 없는 환경

각 업무 WAR 앞단에 공통 JWT Filter를 둡니다.

```
Client
→ Apache·L4
→ 업무 WAR JWT Filter
→ tcf-web
→ tcf-core
```

모든 WAR가 동일한 tcf-jwt 검증기능을 사용해야 합니다.

### 120.8 Gateway 장애

Gateway가 단일 장애점이 되지 않도록 합니다.

```
다중 인스턴스

L4 Health Check

짧은 내부 Timeout

무상태 구성

Route 설정 복제

Key Cache
```

### 120.9 Route 변경

Route는 운영영향이 큰 설정입니다.

필수 관리:

```
변경 전후

대상 WAR

승인

적용시각

Health Check

Rollback

감사로그
```

### 120.10 Rate Limit

```
사용자별

채널별

ServiceId별

업무별
```

기준으로 적용할 수 있습니다.

Rate Limit은 정상 사용자를 과도하게 차단하지 않도록 성능시험과 업무특성을 반영합니다.

### 제120장 요약

```
tcf-gateway는 외부 요청의 인증과 Route를 담당한다.

외부 Header를 신뢰하지 않고
검증된 Token 기준으로 내부 Header를 재작성한다.

업무 규칙과 업무 DB 처리는 Gateway에 넣지 않는다.
```

## 제121장. tcf-cache와 tcf-eai

### 121.1 tcf-cache의 목적

업무코드가 Cache 구현기술에 직접 의존하지 않도록 공통 계약을 제공합니다.

```
public interface TcfCache<K, V> {

    V get(K key);

    void put(
        K key,
        V value,
        Duration ttl
    );

    void evict(K key);

    void clear();
}
```

### 121.2 Cache Provider

구현체 예:

```
Caffeine Provider

Ehcache Provider

Redis Provider

No-op Provider
```

업무코드는 동일한 API를 사용합니다.

### 121.3 Cache Registry

Cache별 정책:

```
Cache Name

Key Prefix

TTL

Maximum Size

Negative Cache

Fallback

민감정보 여부

무효화 Event
```

### 121.4 Cache 확장 시 주의

업무코드가 Provider 전용 API를 직접 사용하면 교체가 어려워집니다.

금지:

```
RedisTemplate<String, Object>
```

를 모든 업무 Service에 직접 주입합니다.

업무별 특수기능이 정말 필요한 경우 Adapter를 통해 제한합니다.

### 121.5 Cache 장애정책

```
조회 Cache 장애
→ 원본 조회

권한 Cache 장애
→ 보안정책에 따라 Fail-closed

운영정책 Cache 장애
→ 마지막 정상값 또는 제한 모드
```

Cache 종류별로 정책을 분리합니다.

### 121.6 tcf-eai의 목적

외부기관과 레거시 시스템의 차이를 공통화합니다.

```
연결관리

인증서

전문 Header

인코딩

Timeout

오류코드

송수신 로그

재처리
```

### 121.7 EAI Client 구조

```
업무 Service
→ 업무별 External Client
→ tcf-eai 공통 Client
→ Adapter
→ EAI·외부기관
```

### 121.8 공통 EAI Client

```
public interface TcfEaiClient {

    <Q, R> R send(
        String interfaceId,
        Q request,
        Class<R> responseType,
        EaiCallContext context,
        EaiPolicy policy
    );
}
```

### 121.9 업무 Adapter

```
@Component
public class ExternalCustomerAdapter {

    public ExternalCustomerRequest toExternal(
            CustomerVerificationRequest request) {
        // 외부 전문 변환
    }

    public CustomerVerificationResult toDomain(
            ExternalCustomerResponse response) {
        // 내부 결과 변환
    }
}
```

### 121.10 EAI 기술 성공과 업무 성공

```
HTTP 200
= 통신 성공

외부 응답코드 E102
= 업무 실패
```

두 결과를 구분해야 합니다.

### 121.11 외부 오류 변환

```
연결 오류
→ ExternalConnectionException

Read Timeout
→ ExternalTimeoutException

외부 업무 오류
→ 업무별 BusinessException

전문 형식 오류
→ ContractViolationException
```

### 121.12 EAI 원문 로그

원문 전문에는 개인정보가 포함될 수 있습니다.

```
전체 원문 저장 금지

필드별 마스킹

암호화 보관

접근권한

보관기간

추적 ID
```

### 제121장 요약

```
tcf-cache는 Cache 구현기술을 업무코드에서 분리한다.

tcf-eai는 연결·전문·오류·로그를 공통화한다.

업무모듈은 Cache Provider나 외부 프로토콜에
직접 결합하지 않는다.
```

## 제122장. tcf-batch와 tcf-om

### 122.1 tcf-batch의 역할

```
Job Launcher

Job Registry

Job Parameter

Step 실행

Checkpoint

Restart

중복실행 방지

Job Metric

운영 Control
```

### 122.2 Batch 공통계약

```
public interface TcfBatchJob {

    String jobName();

    JobExecutionResult execute(
        JobParameters parameters,
        BatchExecutionContext context
    );
}
```

실제 구현은 Spring Batch 기반일 수 있습니다.

### 122.3 업무 Batch의 책임

```
대상 선정

업무 Rule

Reader Query

Processor

Writer

오류 데이터 의미

업무 재처리 기준
```

### 122.4 tcf-batch의 책임

```
실행 Metadata

Job 상태

Checkpoint 저장

중복 실행 Lock

공통 Retry·Skip 기반

Metric

운영 API
```

### 122.5 Scheduler와 분리

```
Scheduler
→ 언제 실행할지 결정

tcf-batch
→ 어떻게 실행·상태관리할지 제공

업무 Job
→ 무엇을 처리할지 구현
```

### 122.6 tcf-om의 역할

tcf-om은 운영정보를 집중 관리합니다.

```
Service Catalog

Timeout Policy

거래통제

권한 연계

Batch Job Registry

Runtime Snapshot

거래로그 조회

Slow SQL

배포이력

운영감사
```

### 122.7 OM과 업무 WAR 연계

```
tcf-om
→ 정책 변경 Event

업무 WAR
→ 정책 Cache 갱신

업무 WAR
→ Runtime Snapshot

tcf-om
→ 상태 수집·대시보드
```

### 122.8 OM 제어와 실행 분리

OM이 직접 업무 DB를 수정하지 않습니다.

예:

```
OM
→ ServiceId SUSPENDED 정책 변경

업무 WAR STF
→ 다음 요청부터 차단
```

### 122.9 OM 정책 Provider

tcf-core는 OM 구현에 직접 강결합하지 않고 Provider 계약을 사용할 수 있습니다.

```
public interface ServicePolicyProvider {

    ServicePolicy find(
        String serviceId
    );
}
```

구현:

```
CachedOmServicePolicyProvider
```

### 122.10 OM 장애 시

```
업무 WAR
→ 마지막 정상 Cache 사용

정책 변경
→ 일시 불가

관리 UI
→ 제한

업무 거래
→ 정책에 따라 지속
```

### 122.11 OM 수집 부하

OM이 Runtime 정보를 과도하게 수집하면 업무에 영향을 줄 수 있습니다.

원칙:

```
짧은 Timeout

낮은 우선순위

한 번의 Snapshot

10~30초 주기

업무 Thread와 분리

수집 실패가 업무 실패로 전파되지 않음
```

### 제122장 요약

```
tcf-batch는 Job 실행과 상태·재시작 기반을 제공한다.

업무 Job은 실제 데이터 처리와 업무 규칙을 담당한다.

tcf-om은 정책·관측·통제를 관리하지만
업무 실행경로의 단일 장애점이 되어서는 안 된다.
```

## 제123장. 모듈 의존성·확장·호환성 관리

### 123.1 권장 의존방향

```
tcf-util
↑
tcf-core
↑
tcf-web
↑
업무 WAR
```

추가 모듈:

```
tcf-core
← tcf-jwt Adapter

업무 WAR
→ tcf-cache API

업무 WAR
→ tcf-eai API

tcf-om
↔ 관리 계약
```

### 123.2 금지 의존성

```
tcf-util
→ 업무 WAR

tcf-core
→ 특정 업무 Service

tcf-jwt
→ tcf-web Controller

tcf-gateway
→ 업무 Mapper

tcf-om
→ 업무 DB 직접 Update

업무 WAR A
→ 업무 WAR B의 Mapper
```

### 123.3 순환 의존

금지:

```
tcf-core
→ tcf-web
→ tcf-core
```

개선:

```
공통 계약을 하위 모듈로 이동

구현체를 상위 모듈에서 주입
```

### 123.4 SPI 확장

Framework가 구현체를 요구할 때 Interface를 제공합니다.

예:

```
public interface TransactionIdGenerator {

    String generateGuid();

    String generateTraceId();
}
```

기본 구현:

```
UuidTransactionIdGenerator
```

업무환경 구현:

```
NsightTransactionIdGenerator
```

### 123.5 Hook 확장 시 원칙

```
실행순서 명확화

실패 정책 명확화

Timeout 영향 검토

중복 실행 방지

Metric 제공

기본 구현 제공

호환성 유지
```

### 123.6 공통모듈 변경 분류

#### Patch

```
오류 수정

기존 계약 유지
```

#### Minor

```
하위 호환 기능 추가

새 Hook·옵션
```

#### Major

```
Interface 변경

응답 의미 변경

기존 설정 제거

호환되지 않는 처리순서 변경
```

### 123.7 Interface 변경 위험

기존:

```
Object handle(
    String serviceId,
    Object body,
    TransactionContext context
);
```

변경:

```
TransactionResult handle(
    TransactionRequest request
);
```

모든 Handler 구현체가 영향을 받을 수 있습니다.

대안:

```
신규 Interface 추가

Adapter 제공

병행기간 운영

기존 Interface Deprecated

점진적 이관
```

### 123.8 설정 호환성

기존 설정:

```
tcf:
  timeout: 3000
```

신규 설정:

```
tcf:
  transaction:
    default-timeout-ms: 3000
```

기존 Key를 일정 기간 읽거나 Migration 오류를 명확히 제공해야 합니다.

### 123.9 응답 호환성

기존:

```
{
  "resultCode": "S0000"
}
```

신규:

```
{
  "result": {
    "resultCode": "S0000"
  }
}
```

화면과 연계 시스템에 큰 영향을 줍니다.

표준 응답 변경은 Major 변경으로 관리해야 합니다.

### 123.10 공통모듈 테스트

필수 테스트:

```
단위 테스트

Processor 순서 테스트

Handler Registry 테스트

표준 응답 테스트

JWT 오류 테스트

Gateway Route 테스트

Cache Provider 계약 테스트

EAI Timeout 테스트

Batch Restart 테스트

OM 정책 Cache 테스트
```

### 123.11 호환성 매트릭스

| 업무 WAR | tcf-core | tcf-web | tcf-jwt | 상태 |
| --- | --- | --- | --- | --- |
| SV 1.3.0 | 1.5.0 | 1.5.0 | 1.2.0 | 지원 |
| IC 1.2.2 | 1.4.2 | 1.4.2 | 1.2.0 | 전환 예정 |
| CM 2.0.0 | 2.0.0 | 2.0.0 | 1.3.0 | 지원 |

### 123.12 공통모듈 Release

```
공통모듈 Build

계약 테스트

대표 업무 WAR Build

Regression Test

호환성 검증

Candidate 배포

성능·장애 테스트

업무 WAR 재빌드

단계적 운영 배포
```

### 제123장 요약

```
공통모듈 의존성은 하위 기반에서 상위 업무 방향으로 흐른다.

확장은 SPI·Hook·Provider 계약을 사용한다.

Interface·설정·응답 변경은
모든 업무 WAR의 호환성을 검토해야 한다.
```

## 3. 목표 아키텍처

```
                       [사용자·외부 시스템]
                                │
                                ▼
                        [tcf-gateway]
                    인증·Route·Rate Limit
                                │
                                ▼
                       [업무 WAR / tcf-web]
                     Filter·Controller·JSON
                                │
                                ▼
                          [tcf-core]
              ┌─────────────────┼─────────────────┐
              │                 │                 │
              ▼                 ▼                 ▼
            [STF]          [Dispatcher]          [ETF]
     인증·권한·통제       Handler Registry     결과·로그·Metric
              │                 │                 │
              └─────────────────┼─────────────────┘
                                ▼
                        [업무 Handler 이하]
              Handler → Facade → Service → DAO
                                │
         ┌──────────────┬───────┼────────┬──────────────┐
         ▼              ▼       ▼        ▼              ▼
     [tcf-jwt]    [tcf-cache] [tcf-eai] [tcf-batch] [tcf-om]
       Token        Cache       연계       Job          운영
```

## 4. 모듈별 표준 계약

### 4.1 tcf-core 계약

```
StandardRequest

StandardResponse

TransactionContext

TransactionHandler

PreTransactionProcessor

PostTransactionProcessor

ServicePolicyProvider
```

### 4.2 tcf-web 계약

```
OnlineTransactionController

Request Filter

Web Error Mapper

JSON Configuration

Health·Runtime Endpoint
```

### 4.3 tcf-jwt 계약

```
JwtTokenIssuer

JwtTokenValidator

JwtClaimsMapper

JwksProvider

KeyRotationPolicy
```

### 4.4 tcf-cache 계약

```
TcfCache

CacheProvider

CachePolicy

CacheInvalidationEvent
```

### 4.5 tcf-eai 계약

```
TcfEaiClient

EaiAdapter

EaiPolicy

EaiCallContext

ExternalErrorMapper
```

### 4.6 tcf-batch 계약

```
TcfBatchJob

JobRegistry

JobLauncher

BatchExecutionContext

CheckpointRepository
```

### 4.7 tcf-om 계약

```
ServiceCatalog

ServicePolicy

RuntimeSnapshot

ServiceMetric

OperationCommand

AuditRecord
```

## 5. 구성요소 및 속성

| 구성요소 | 주요 속성 |
| --- | --- |
| TCF Engine | Pipeline·오류 처리 |
| STF | 인증·권한·통제 |
| Dispatcher | ServiceId·Handler |
| ETF | 응답·거래 종료 |
| Context | 사용자·Trace·Timeout |
| Web Layer | HTTP·JSON |
| JWT Module | Key·Claim·검증 |
| Gateway | Route·Header |
| Cache Module | Provider·TTL |
| EAI Module | 전문·Timeout |
| Batch Module | Job·Checkpoint |
| OM Module | 정책·관측 |
| SPI | 확장 Interface |
| Compatibility Matrix | 지원 버전 |

## 6. 책임 경계와 RACI

| 활동 | AA | FW | 업무 DEV | SEC | TA | DBA | OM | QA |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Core Pipeline | A/C | A/R | I | C | C | I | C | C |
| Handler 계약 | C | A/R | R/C | I | I | I | I | C |
| Web Controller | C | A/R | I | C | C | I | I | C |
| JWT | C | R | I | A/R | C | I | C | C |
| Gateway | A/C | R | I | C | A/C | I | C | C |
| Cache 공통 | C | A/R | C | C | C | C | C | C |
| EAI 공통 | C | R/C | C | C | C | I | C | C |
| Batch 공통 | C | A/R | C | I | C | C | C | C |
| OM 공통 | A/C | R | C | C | C | C | A/R | C |
| 호환성 | A | R | C | C | C | I | C | R/C |
| Release | C | A/R | C | C | C | I | C | C |

## 7. 정상 처리 흐름

```
1. Gateway가 JWT와 Route를 검증한다.

2. tcf-web Filter가 요청 Context를 준비한다.

3. Controller가 StandardRequest를 생성한다.

4. tcf-core가 STF를 실행한다.

5. STF가 정책과 인증정보를 검증한다.

6. Timeout Executor가 실행시간을 제한한다.

7. Dispatcher가 ServiceId Handler를 찾는다.

8. 업무 Handler가 Facade를 호출한다.

9. 업무 Service가 Cache·EAI·DB를 사용한다.

10. ETF가 결과와 거래로그를 확정한다.

11. tcf-web이 JSON 응답을 반환한다.

12. tcf-om이 Metric과 상태를 집계한다.
```

## 8. 오류·Timeout·장애 흐름

### 8.1 JWT 오류

```
Gateway 또는 업무 Filter
→ Token 검증 실패
→ tcf-jwt 표준 오류
→ Handler 미실행
```

### 8.2 ServiceId 중복

```
기동 중 Handler Registry 생성
→ 중복 발견
→ 애플리케이션 기동 실패
```

### 8.3 OM 정책 조회 실패

```
ServicePolicyProvider
→ OM 조회 실패
→ 마지막 Cache 사용
→ 수집·정책 경보
```

### 8.4 Cache Provider 장애

```
Cache 오류
→ 정책에 따라 원본 조회
→ 업무 지속 또는 Fail-closed
```

### 8.5 EAI Timeout

```
업무 Client
→ tcf-eai Timeout
→ ExternalTimeoutException
→ ETF 표준 오류
```

### 8.6 ETF 오류

```
업무 결과 생성
→ 거래로그 종료 실패
→ 필수성 정책에 따라
   거래 실패 또는 비동기 재기록
```

## 9. 정상 예시

```
요청
CT.Contact.create

Gateway
JWT 정상

tcf-web
StandardRequest 변환

STF
권한·거래통제·Timeout 정상

Dispatcher
CtContactHandler 선택

업무
Master·History 저장

ETF
SUCCESS·거래로그 종료

OM
처리시간 320ms 집계

응답
S0000
```

## 10. 금지 예시

### 10.1 업무코드에서 JWT 직접 파싱

```
JwtParser.parse(token);
```

### 10.2 업무별 Controller 생성

```
@PostMapping("/campaign/create")
```

### 10.3 tcf-core에서 업무 DB 조회

```
tcf-core
→ CM_CAMPAIGN_MASTER
```

### 10.4 Gateway에 업무 Rule 구현

```
Gateway
→ 고객 상태 판단
```

### 10.5 OM에서 업무 테이블 직접 수정

```
tcf-om
→ UPDATE CT_CONTACT_MASTER
```

### 10.6 Provider 전용 API 직접 사용

모든 업무 Service가 Redis·EAI 제품 API를 직접 호출합니다.

### 10.7 공통 Interface 즉시 삭제

기존 Handler 전체가 컴파일되지 않습니다.

## 11. 연계 규칙

```
tcf-web
→ tcf-core

업무 WAR
→ tcf-core 계약

업무 WAR
→ tcf-cache API

업무 WAR
→ tcf-eai API

tcf-gateway
→ tcf-jwt

tcf-core
→ ServicePolicyProvider

tcf-om
→ 정책·관측 계약
```

금지:

```
tcf-core
→ 업무 WAR

tcf-util
→ 상위 공통모듈

Gateway
→ 업무 DAO

OM
→ 업무 Mapper
```

## 12. 데이터 및 상태관리

### 12.1 거래 상태

```
RECEIVED
PROCESSING
SUCCESS
BUSINESS_FAIL
SYSTEM_ERROR
TIMEOUT
REJECTED
UNKNOWN
```

### 12.2 Token 상태

```
ACTIVE
EXPIRED
REVOKED
REUSED
INVALID
```

### 12.3 정책 상태

```
ACTIVE
SUSPENDED
READ_ONLY
DISABLED
```

### 12.4 Job 상태

```
STARTING
PROCESSING
COMPLETED
FAILED
STOPPED
UNKNOWN
```

### 12.5 모듈 상태

```
SUPPORTED
DEPRECATED
END_OF_SUPPORT
RETIRED
```

## 13. 성능·용량·확장성

| 영역 | 기준 |
| --- | --- |
| STF | DB 직접조회 최소화 |
| Policy | Cache 사용 |
| Registry | 기동 시 생성 |
| JWT | Public Key Cache |
| Gateway | 무상태·다중화 |
| Cache | Entry·TTL 제한 |
| EAI | Connection Pool·Timeout |
| Batch | 온라인 자원 분리 |
| OM | 낮은 수집 부하 |
| Metric | 고카디널리티 방지 |
| 공통 JAR | Version 통제 |
| Hook | 처리시간 측정 |

## 14. 보안·개인정보·감사

```
Private Key는 tcf-jwt 발급영역에서만 관리한다.

Gateway와 업무 WAR는 Public Key로 검증한다.

내부 신뢰 Header는 외부 입력을 제거한 뒤 재생성한다.

TransactionContext의 사용자정보를 업무코드가 변경하지 않는다.

공통 로그에서 JWT와 개인정보를 마스킹한다.

OM 운영조치와 Key Rotation을 감사한다.

관리 Endpoint는 내부망·인증·권한을 적용한다.
```

## 15. 운영·모니터링·장애 대응

공통모듈별 운영지표:

| 모듈 | 주요 지표 |
| --- | --- |
| tcf-core | 거래량·오류·Timeout |
| tcf-web | HTTP 오류·Body 크기 |
| tcf-jwt | 검증 실패·Key 갱신 |
| tcf-gateway | Route·401·403·Rate Limit |
| tcf-cache | Hit·Miss·Eviction |
| tcf-eai | 외부 응답·Timeout |
| tcf-batch | Job 성공·실패·적체 |
| tcf-om | 수집 성공률·정책 반영 |

## 16. 자동검증 및 품질 Gate

| Gate | 검증 |
| --- | --- |
| 모듈 의존성 | 순환·역방향 의존 금지 |
| Core | 업무 Package 참조 금지 |
| Web | 업무 Controller 금지 |
| JWT | Algorithm·Claim 테스트 |
| Gateway | Route·Header 정합성 |
| Cache | Provider 계약 테스트 |
| EAI | Timeout·오류 매핑 |
| Batch | Restart·중복실행 |
| OM | 정책 Cache·자동 원복 |
| Handler | ServiceId 중복 |
| Context | finally 정리 |
| API | 하위 호환성 |
| 설정 | 기존 Key 호환 |
| Release | 대표 WAR Regression |

## 17. 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| FW-001 | 정상 TCF 거래 | SUCCESS |
| FW-002 | Header 누락 | STF 차단 |
| FW-003 | ServiceId 중복 | 기동 실패 |
| FW-004 | Handler 없음 | 표준 오류 |
| FW-005 | 권한 없음 | Handler 미실행 |
| FW-006 | 거래 SUSPENDED | Handler 미실행 |
| FW-007 | Timeout | ETF TIMEOUT |
| FW-008 | BusinessException | 업무 오류 |
| FW-009 | SystemException | 시스템 오류 |
| FW-010 | Context 정리 | 다음 요청 오염 없음 |
| FW-011 | JWT 위조 | 인증 실패 |
| FW-012 | 잘못된 Audience | 인증 실패 |
| FW-013 | 신규 kid | JWKS 갱신 |
| FW-014 | Gateway Header 위조 | 제거·재생성 |
| FW-015 | Route 불일치 | 차단 |
| FW-016 | Cache Miss | 원본 조회 |
| FW-017 | Cache 장애 | 정책별 Fallback |
| FW-018 | EAI Timeout | 표준 연계 오류 |
| FW-019 | EAI 업무 오류 | 업무코드 매핑 |
| FW-020 | Batch 중복실행 | 1회만 실행 |
| FW-021 | Batch Restart | Checkpoint 재개 |
| FW-022 | OM 장애 | 마지막 정책 유지 |
| FW-023 | OM 정책 변경 | 전체 WAR 반영 |
| FW-024 | Interface 하위 호환 | 기존 Handler 정상 |
| FW-025 | 설정 Key Migration | 기존 설정 지원 |
| FW-026 | 공통 JAR Upgrade | 대표 WAR Regression |
| FW-027 | 순환 의존 | Build 실패 |
| FW-028 | 관리 Endpoint 외부 접근 | 차단 |
| FW-029 | 민감정보 로그 | 마스킹 |
| FW-030 | Rollback | 이전 공통버전 정상 |

## 18. 제13부 체크리스트

### 18.1 공통모듈 경계

| 점검 항목 | 확인 |
| --- | --- |
| 공통과 업무 책임이 분리되는가? | □ |
| tcf-core가 업무코드를 참조하지 않는가? | □ |
| 업무 WAR가 공통기능을 재구현하지 않는가? | □ |
| 순환 의존성이 없는가? | □ |
| 공통 Interface가 명확한가? | □ |

### 18.2 tcf-core

| 점검 항목 | 확인 |
| --- | --- |
| STF 순서가 명시되어 있는가? | □ |
| Context가 신뢰정보로 관리되는가? | □ |
| ServiceId 중복을 기동 시 차단하는가? | □ |
| Timeout이 공통 적용되는가? | □ |
| ETF가 모든 종료상태를 처리하는가? | □ |
| ThreadLocal을 finally에서 정리하는가? | □ |

### 18.3 tcf-web·Gateway

| 점검 항목 | 확인 |
| --- | --- |
| 업무별 Controller를 만들지 않는가? | □ |
| Filter 순서가 정의되어 있는가? | □ |
| Body 크기를 제한하는가? | □ |
| Route와 업무코드가 일치하는가? | □ |
| 내부 Header를 재작성하는가? | □ |
| Gateway 우회 접근을 차단하는가? | □ |

### 18.4 JWT

| 점검 항목 | 확인 |
| --- | --- |
| 허용 Algorithm을 고정하는가? | □ |
| issuer·audience·exp를 검증하는가? | □ |
| Private Key가 발급영역에만 있는가? | □ |
| JWKS와 kid를 사용하는가? | □ |
| Key Rotation 병행기간이 있는가? | □ |
| 업무코드가 JWT를 직접 해석하지 않는가? | □ |

### 18.5 Cache·EAI

| 점검 항목 | 확인 |
| --- | --- |
| Cache Provider가 추상화되어 있는가? | □ |
| TTL·최대크기가 등록되는가? | □ |
| Cache 장애정책이 있는가? | □ |
| 업무별 EAI Adapter가 있는가? | □ |
| 외부 오류가 표준 오류로 변환되는가? | □ |
| 외부 원문이 마스킹되는가? | □ |

### 18.6 Batch·OM

| 점검 항목 | 확인 |
| --- | --- |
| Batch 업무와 공통 실행기반이 분리되는가? | □ |
| Job 중복실행을 차단하는가? | □ |
| Checkpoint와 Restart가 가능한가? | □ |
| OM 장애가 업무 장애로 전파되지 않는가? | □ |
| 정책 Cache가 있는가? | □ |
| 운영조치가 감사되는가? | □ |

### 18.7 호환성

| 점검 항목 | 확인 |
| --- | --- |
| Interface 변경 영향이 분석되었는가? | □ |
| 기존 설정 Key를 지원하는가? | □ |
| 응답 계약이 하위 호환되는가? | □ |
| 대표 업무 WAR Regression을 수행하는가? | □ |
| 호환성 매트릭스를 관리하는가? | □ |
| 이전 공통버전 Rollback이 가능한가? | □ |

## 19. 변경·호환성·폐기 관리

### 19.1 공통모듈 변경 전 확인

```
영향받는 업무 WAR

Interface 변경

설정 변경

응답 변경

DB 변경

운영정책 변경

성능 영향

보안 영향

Rollback 가능성
```

### 19.2 Deprecation

```
@Deprecated(
    since = "2.0",
    forRemoval = true
)
public interface LegacyTransactionHandler {
}
```

폐기 예정 기능에는 다음을 제공합니다.

```
대체 API

전환 예제

지원 종료일

사용 모듈 목록

Migration Guide
```

### 19.3 공통 설정 폐기

```
기존 Key 사용 경고

신규 Key 병행지원

호출·사용현황 확인

Release Note 안내

지원 종료

기존 Key 제거
```

### 19.4 응답 필드 폐기

기존 필드를 즉시 삭제하지 않습니다.

```
신규 필드 추가

구 필드 유지

호출자 전환

구 필드 Deprecated

호출자 0 확인

제거
```

### 19.5 모듈 폐기

예:

```
기존 om-service
→ 신규 tcf-om
```

확인:

```
기능 목록

DB Schema

운영 UI

배치·정책

호출자

로그 보관

Rollback

전환기간
```

## 20. 시사점

### 20.1 핵심 아키텍처 판단

제13부의 핵심은 다음과 같습니다.

```
Framework를 안다
= 내부 코드를 모두 수정할 수 있다
```

가 아닙니다.

```
Framework를 안다
= 어느 기능이 어느 모듈의 책임인지 알고,
  정해진 확장점을 통해 안전하게 연결한다
```

입니다.

### 20.2 주요 위험

| 위험 | 영향 |
| --- | --- |
| 공통·업무 책임 혼합 | 유지보수 어려움 |
| 업무별 Controller | TCF 우회 |
| JWT 직접 해석 | 보안 불일치 |
| Gateway 업무로직 | 진입계층 비대화 |
| OM 업무 DB 수정 | 책임경계 붕괴 |
| Provider 직접결합 | 기술 교체 어려움 |
| Processor 순서 변경 | 보안·결과 변화 |
| Interface 즉시 변경 | 전체 WAR 장애 |
| 공통 JAR 버전 혼재 | 서버별 동작 차이 |
| ThreadLocal 정리 누락 | 사용자정보 오염 |
| 공통모듈 테스트 부족 | 전체 서비스 장애 |
| OM 단일 의존 | 정책 장애 전파 |

### 20.3 우선 보완 과제

```
1. 공통모듈 책임·의존성 문서화

2. tcf-core Pipeline 순서 확정

3. Handler Registry 기동검증

4. TransactionContext 불변성 강화

5. JWT·Gateway 계약 표준화

6. Cache Provider 추상화

7. EAI Adapter·오류 매핑 표준

8. Batch Job 공통 Metadata

9. OM Policy Provider와 Cache

10. 공통모듈 호환성 매트릭스

11. 대표 업무 Regression Suite

12. 공통모듈 Release·Rollback 절차
```

### 20.4 중장기 발전 방향

```
직접 Class 결합
→ SPI·Provider 계약

수동 Handler 등록
→ Source Scan·기동검증

단일 Pipeline
→ 순서가 관리되는 Processor Chain

업무별 JWT 코드
→ 통합 인증 Context

제품별 Cache·EAI 결합
→ 공통 Adapter

수동 호환성 확인
→ 자동 Contract Test

공통 JAR 일괄 변경
→ 호환성 기반 단계적 Upgrade

장애 후 분석
→ 모듈별 Metric·Trace 자동 연결
```

Framework 자동화가 증가할수록 기본 동작과 실패정책을 더 명확하게 문서화해야 합니다.

## 21. 마무리말

제13부에서 가장 중요하게 기억해야 할 내용은 다음과 같습니다.

```
tcf-util
= 가장 낮은 기술 기반

tcf-core
= 거래 처리 엔진

STF
= 거래 전 검증과 준비

Dispatcher
= ServiceId와 Handler 연결

ETF
= 거래 결과와 종료 처리

tcf-web
= HTTP와 JSON 진입계층

tcf-jwt
= Token 발급·검증

tcf-gateway
= 외부 인증과 Route

tcf-cache
= Cache 기술 추상화

tcf-eai
= 외부연계 공통화

tcf-batch
= 대량 Job 실행기반

tcf-om
= 운영정책·관측·통제
```

전체 실행 흐름은 다음과 같습니다.

```
Client

→ Gateway

→ tcf-web

→ tcf-core

→ STF

→ Dispatcher

→ 업무 Handler

→ Facade·Service

→ Cache·EAI·DB

→ ETF

→ 표준 응답

→ OM 관측
```

초보 개발자가 Framework 내부를 변경하거나 확장하기 전에 마지막으로 확인해야 할 질문은 다음과 같습니다.

```
이 기능은 모든 업무가 공통으로 사용하는가?

업무 규칙이 포함되어 있지는 않은가?

기존 Hook이나 Provider로 확장할 수 있는가?

TCF Pipeline을 우회하고 있지는 않은가?

처리순서가 인증·권한 결과에 영향을 주는가?

TransactionContext의 신뢰정보를 변경하고 있지는 않은가?

업무코드가 JWT나 Cache 제품 API에 직접 의존하는가?

공통 Interface 변경이 모든 WAR에 미치는 영향은 무엇인가?

기존 설정과 응답이 계속 호환되는가?

대표 업무 WAR로 Regression Test를 수행했는가?

공통모듈 장애 시 영향범위와 Rollback 방법이 준비되어 있는가?

OM이나 Gateway가 단일 장애점이 되지는 않는가?
```

이 질문에 답할 수 있다면 단순히 Framework API를 사용하는 수준을 넘어, 공통모듈의 책임과 영향을 이해하고 NSIGHT TCF를 안전하게 확장·운영할 수 있는 개발자가 될 수 있습니다.

