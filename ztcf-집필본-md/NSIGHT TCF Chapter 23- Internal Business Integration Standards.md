<!-- source: ztcf-집필본/NSIGHT TCF Chapter 23- Internal Business Integration Standards.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제23장. 내부 업무 연계

## 도입 전 안내말

제22장에서는 SSO·JWT·Gateway·업무 WAR·STF·업무 Rule을 연결하여 사용자의 신원과 권한을 검증하는 구조를 살펴보았다.

이번 장에서는 인증된 하나의 업무가 다른 업무의 데이터나 기능을 필요로 할 때 **어떤 방식으로 안전하게 호출해야 하는지**를 다룬다.

내부 업무 연계는 단순히 다른 Service 클래스를 호출하는 작업이 아니다.

\`\`\`text id=“7f1vnn” IC 업무에서 SV 고객요약 데이터가 필요하다.

→ SvCustomerService를 직접 호출한다.



같은 방식으로 구현하면 처음에는 간단해 보일 수 있다.

그러나 업무가 증가하면 다음과 같은 구조가 만들어질 수 있다.

\`\`\`text id="f8sh7b"
IC Service
↓
SV Service
↓
PD Service
↓
CM Service
↓
IC DAO

또는 다음처럼 다른 업무의 DB를 직접 변경할 수도 있다.

text id="gkf2lm" IC Service ↓ SV Mapper ↓ SV\_CUSTOMER UPDATE

이러한 구조에서는 다음을 판단하기 어렵다.

\`\`\`text id=“1mjwpz” 데이터의 실제 소유 업무는 어디인가?

누가 변경 규칙을 책임지는가?

호출자의 Transaction이 하위 업무까지 적용되는가?

하위 업무가 실패하면 상위 업무는 어떻게 처리하는가?

하위 업무의 Timeout은 몇 초인가?

같은 변경 요청이 다시 전달되면 중복 처리되지 않는가?

SV가 IC를 호출하고 IC가 다시 SV를 호출하지 않는가?

장애가 발생했을 때 어느 업무팀이 원인을 확인하는가?

제공 업무의 DTO가 바뀌면 소비 업무는 언제 배포해야 하는가?



NSIGHT TCF의 업무 계층은 다음 구조를 기본으로 한다.

\`\`\`text id="98nakb"
Handler
↓
Facade
↓
Service
├─ Rule
├─ DAO
│ ↓
│ Mapper
└─ Client

여기서 Client는 단순한 HTTP 호출 Utility가 아니다.

\`\`\`text id=“tfmt2p” 다른 업무가 공개한 계약을 소비 업무의 모델로 변환하고,

Timeout·오류·인증·추적정보를 표준에 따라 적용하는 업무 경계 Adapter다.



내부 업무 연계의 기본 원칙은 다음과 같다.

\`\`\`text id="jxdypr"
같은 도메인 내부
→ 내부 계층 호출

같은 WAR의 다른 도메인
→ 공개 Application Contract

다른 업무 WAR
→ ServiceId 기반 TCF 표준 전문

느슨한 후속 처리
→ 이벤트·Outbox

복합·대량 조회
→ Read Model 검토

다른 업무의 DAO·Mapper·Table 직접 사용
→ 금지

도메인 연동 기준에서도 동일 WAR는 공개 Application Contract를 사용하고, 다른 WAR는 TCF/EAI ServiceId를 통해 호출하며, 데이터 변경은 반드시 데이터 소유 도메인이 수행하도록 정의한다.

# 문서 개요

## 목적

본 장의 목적은 NSIGHT TCF에서 업무 도메인과 업무 WAR 사이의 내부 연계방식을 표준화하는 것이다.

세부 목적은 다음과 같다.

\`\`\`text id=“jn1pqw” 데이터 소유권 보호

다른 업무 내부 구현 직접 참조 방지

동일 WAR 공개 Contract 표준화

업무 WAR 간 ServiceId 호출 표준화

표준 Header·GUID·TraceId 전파

Timeout Budget 관리

하위 업무 오류 매핑

조회·변경 연계의 재시도 구분

변경 연계의 멱등성과 상태확인

순환 의존과 호출 Loop 차단

로컬 Transaction과 원격 Transaction 분리

부분 성공·보상·최종 일관성 정의

계약 버전·호환성·폐기 관리

운영 추적과 장애 대응


\## 적용범위

| 적용 영역 | 적용 내용 |
|---|---|
| 동일 도메인 내부 | Service·Rule·DAO·Mapper 계층 호출 |
| 동일 WAR 도메인 간 | 공개 Java Application Contract |
| 다른 업무 WAR 간 | TCF 표준 전문·ServiceId 호출 |
| 동기 조회 | 즉시 결과가 필요한 데이터 조회 |
| 동기 변경 | 즉시 업무 결과가 필요한 상태 변경 |
| 비동기 연계 | 이벤트·메시지·Outbox |
| 통합조회 | 여러 도메인의 조회 결과 조립 |
| Read Model | 반복되는 통합조회 최적화 |
| Batch 연계 | 대량 처리·재처리·상태 전달 |
| 보안 | 사용자 위임·서비스 인증·개인정보 |
| 운영 | Timeout·Circuit·로그·Metric·Runbook |
| 품질 | Contract Test·ArchUnit·Gradle Gate |

다음 항목은 제24장에서 보다 상세히 다룬다.

\`\`\`text id="t1jw0k"
외부기관·외부솔루션 호출

HTTP·전문 제품별 Adapter

Circuit Breaker 세부설정

외부 오류코드 매핑

외부 재시도

외부 연계 보상

## 대상 독자

\`\`\`text id=“zoyn5x” 업무 개발자

애플리케이션 아키텍트

프레임워크 개발자

데이터 아키텍트·DBA

보안 아키텍트

QA·통합테스트 담당자

운영·장애 대응 담당자

DevOps·품질관리 담당자

업무 분석가·PMO


\## 선행조건

\`\`\`text id="6l7u2c"
업무코드·도메인 정의

데이터 소유권 정의

ServiceId 명명규칙

표준 Header·Response 이해

Handler·Facade·Service·Rule 구조

Transaction·멱등성 이해

GUID·TraceId 이해

인증·인가·AuthenticationContext 이해

오류코드·Timeout 정책 이해

OM Service Catalog 이해

# 핵심 관점

\`\`\`text id=“7q89yd” 내부 시스템이라는 이유로 직접 참조해도 되는 것은 아니다.

같은 회사, 같은 DB, 같은 Tomcat 안에 있어도

업무 책임과 데이터 소유권이 다르면 공개된 계약을 통해 호출해야 한다.


\---

\# 학습 목표

이 장을 마치면 다음 내용을 설명하고 구현할 수 있어야 한다.

| 번호 | 학습 목표 |
|---:|---|
| 1 | 소비 도메인과 제공 도메인을 구분한다. |
| 2 | 데이터 소유권과 조회 사용권을 구분한다. |
| 3 | 다른 업무의 DAO·Mapper 직접 호출이 금지되는 이유를 설명한다. |
| 4 | 공개 계약과 내부 구현을 구분한다. |
| 5 | 같은 WAR와 다른 WAR의 연계방식을 구분한다. |
| 6 | 동일 WAR Application Contract를 설계한다. |
| 7 | 다른 WAR의 Java 모듈 직접 의존을 금지한다. |
| 8 | \`TcfServiceClient\`를 통한 ServiceId 호출 흐름을 설명한다. |
| 9 | 대상 업무코드·ServiceId·거래코드의 역할을 설명한다. |
| 10 | 표준 Header에 GUID·TraceId·호출자 정보를 전파한다. |
| 11 | 사용자 인증문맥과 서비스 호출자 신원을 구분한다. |
| 12 | 내부 호출에서 Header 사용자값만 신뢰하면 안 되는 이유를 설명한다. |
| 13 | Parent·Child 호출 관계와 Span을 설계한다. |
| 14 | 전체 거래 Timeout과 하위 호출 Timeout을 계산한다. |
| 15 | 순차 호출과 병렬 호출의 Timeout Budget 차이를 설명한다. |
| 16 | 하위 업무 오류를 상위 업무 오류로 매핑한다. |
| 17 | 필수 의존과 선택 의존을 구분한다. |
| 18 | 선택 의존 실패 시 부분 성공을 설계한다. |
| 19 | 조회 연계와 변경 연계의 재시도 정책을 구분한다. |
| 20 | 변경 연계에 Idempotency Key를 전파한다. |
| 21 | 변경 연계 Timeout 후 결과를 UNKNOWN으로 처리할 수 있다. |
| 22 | 원격 업무 변경이 상위 로컬 Transaction에 포함되지 않음을 설명한다. |
| 23 | Outbox·보상·최종 일관성을 적용할 상황을 설명한다. |
| 24 | 순환 의존과 호출 Loop를 탐지한다. |
| 25 | N+1 원격 호출의 문제를 설명한다. |
| 26 | 반복 통합조회에 Read Model을 검토한다. |
| 27 | Contract Version과 하위호환성을 관리한다. |
| 28 | 연계 호출을 로그·Metric·Trace로 추적한다. |
| 29 | Contract Test와 장애 테스트를 작성한다. |
| 30 | 연계 계약 폐기와 소비자 전환을 관리한다. |

\---

\# 핵심 용어

| 용어 | 의미 |
|---|---|
| 소비 도메인 | 다른 도메인의 기능·데이터를 사용하는 영역 |
| 제공 도메인 | 공개 계약으로 기능·데이터를 제공하는 영역 |
| 데이터 소유권 | 데이터의 생성·변경·삭제 규칙과 품질을 책임지는 권한 |
| 공개 계약 | 외부 도메인이 사용할 수 있도록 승인된 Interface·DTO·ServiceId |
| 내부 구현 | Service 구현·Rule·DAO·Mapper·Table 구조 |
| Application Contract | 동일 WAR 도메인 간 Java 공개 계약 |
| Contract DTO | 도메인·업무 경계를 넘는 요청·응답 객체 |
| Client Adapter | 제공 업무 계약을 소비 업무 모델로 변환하는 Adapter |
| Provider | 연계 기능을 제공하는 업무 |
| Consumer | 연계 기능을 사용하는 업무 |
| Orchestrator | 여러 업무 호출을 조합하는 유스케이스 책임자 |
| 필수 의존 | 하위 호출 실패 시 상위 업무도 실패하는 관계 |
| 선택 의존 | 하위 호출 실패 시 부분 결과를 허용하는 관계 |
| Timeout Budget | 전체 응답시간 중 각 처리단계에 배분한 시간 |
| Call Depth | 내부 호출의 깊이 |
| Parent ServiceId | 현재 호출을 시작한 상위 ServiceId |
| Span ID | Trace 안의 개별 호출 구간 식별자 |
| Contract Version | 요청·응답 계약의 버전 |
| Outbox | DB 변경과 후속 이벤트를 같은 Transaction으로 기록하는 방식 |
| Read Model | 여러 업무 데이터를 조회 목적에 맞게 미리 구성한 모델 |
| 보상 | 이미 성공한 원격 변경을 반대 업무로 상쇄하는 처리 |
| 최종 일관성 | 여러 업무 데이터가 일정 시간 후 일치하는 방식 |

\---

\# 내부 연계 방식 선택표

| 상황 | 기본 방식 | 판단 |
|---|---|---|
| 동일 도메인 내부 | Java 내부 호출 | 허용 |
| 동일 WAR 다른 도메인 조회 | Application Contract | 권장 |
| 동일 WAR 다른 도메인 변경 | 공개 Command Contract | 신중 적용 |
| 다른 WAR 조회 | \`TcfServiceClient\`·ServiceId | 권장 |
| 다른 WAR 변경 | ServiceId + 멱등성 | 필수 통제 |
| 여러 업무 반복 통합조회 | Read Model | 검토 |
| 느슨한 후속 처리 | Event·Outbox | 권장 |
| 대량 데이터 전달 | Batch·File·Message | 온라인 호출 비권장 |
| 다른 업무 Table 직접 조회 | 원칙적 금지 | 예외 승인 필요 |
| 다른 업무 Table 직접 변경 | 금지 | 소유권 위반 |
| 다른 WAR Java Import | 금지 | 독립 배포 훼손 |

도메인 간 연계 방식은 공개 Facade·Application Contract, TCF/EAI ServiceId, 이벤트, Read Model 순으로 업무 상황에 맞게 선택하며 DB 직접 접근은 원칙적으로 사용하지 않는다.

\---

\# 전체 목표 아키텍처

\`\`\`text id="j2v8bb"
화면
↓
IC.Customer.selectIntegratedProfile
↓
IC Handler
↓
IC Facade
↓
IC Service·Orchestrator
├─ IC 자체 DAO
│ ↓
│ IC DB
│
├─ SvCustomerProfileClient
│ ↓
│ TcfServiceClient
│ ↓
│ SV /online
│ ↓
│ SV.Customer.selectSummary
│ ↓
│ SV Handler·Facade·Service·DAO
│
└─ ProductHoldingClient
↓
PD.Product.selectHoldingList
↓
PD 업무 처리
↓
IC 통합 Response 조립
↓
ETF

핵심:

\`\`\`text id=“hgdqz6” IC는 SV Mapper를 호출하지 않는다.

IC는 SV 업무 Response를 그대로 화면에 노출하지 않는다.

IC는 SV 공개 계약을 자신의 업무 모델로 변환한다.


\---

\# 현재 구현과 목표 구조

\## 현재 소스에서 확인되는 구성

현재 \`tcf-eai\`에는 다음 내부 서비스 호출 구조가 구현돼 있다.

\`\`\`text id="l2hqh5"
TcfServiceClient
↓
DefaultTcfServiceClient
↓
Spring RestClient
↓
POST /{업무 Context}/online
↓
표준 Header·Body
↓
대상 ServiceId

대표 업무 Adapter:

\`\`\`text id=“nptwbs” IC → SvIntegrationClient → SV.Customer.selectSummary

SV → IcIntegrationClient → IC.Sample.inquiry



현재 구현의 주요 기능:

| 기능 | 상태 |
|---|---|
| 대상 업무코드별 Endpoint | 구현 확인 |
| 대상 ServiceId·거래코드 | 구현 확인 |
| 표준 Header 조립 | 구현 확인 |
| GUID 전파 | 구현 확인 |
| TraceId 전파 | 구현 확인 |
| 사용자·지점·채널 전파 | 구현 확인 |
| 호출자 업무코드·ServiceId | 구현 확인 |
| Connect Timeout | 구현 확인 |
| Read Timeout | 구현 확인 |
| 표준 성공코드 판정 | 구현 확인 |
| 하위 업무 실패 예외 | 구현 확인 |
| 연결·Timeout 예외 분리 | 구현 확인 |
| 업무별 Client Adapter | 샘플 구현 확인 |
| 호출시간 로그 | 구현 확인 |

다른 업무 WAR는 Java Bean을 직접 참조하지 않고 \`TcfServiceClient → 표준 전문 → 대상 업무 /online → ServiceId\` 구조로 호출하는 것이 프로젝트 표준이다.

\---

\## 현재 구현의 주요 보완점

| 항목 | 현재 상태 | 보완 판단 |
|---|---|---|
| Body 형식 | \`Map<String,Object>\` 중심 | Typed Contract DTO 권장 |
| 응답 변환 | \`fromMap()\` 수동 변환 | Schema·필수필드 검증 필요 |
| Contract Version | 명시 없음 | 버전 Header·Schema 필요 |
| 처리유형 기본값 | 미지정 시 \`INQUIRY\` | 변경 거래는 명시 강제 |
| Idempotency Key | 전파 확인 안 됨 | 변경 호출 필수 |
| Remaining Timeout | 전파 안 됨 | 상위 잔여예산 전달 필요 |
| Span ID | GUID·TraceId만 유지 | Child Span 필요 |
| Call Depth | 관리 안 됨 | 최대 깊이·Loop 차단 필요 |
| 내부 인증 | 사용자 Header 중심 | 서비스 인증·사용자 위임 필요 |
| Authorization Header | 공통 Client 전파 확인 필요 | 직접 WAR 인증과 정합화 |
| Retry | 구현 확인 안 됨 | 조회·멱등 요청에 제한 |
| Circuit Breaker | 구현 확인 안 됨 | 중요 연계 적용 검토 |
| Bulkhead | 구현 확인 안 됨 | 대상별 동시호출 제한 |
| 선택 의존 | 공통 모델 없음 | Partial Result 표준 필요 |
| 하위 오류 매핑 | 하위 결과코드 중심 | errorCode·유형·Retryable 보존 |
| Response 필드 | \`message\` 의존 | 표준 Response 필드 정합성 점검 |
| URL 관리 | 업무코드별 설정 | L4·Service Discovery 기준 확정 |
| 민감정보 로그 | 메시지 중심 | Body·Parameter 출력 금지 |
| Client Contract Test | 확인 필요 | 제공자·소비자 CI 필수 |

\---

\## 현재 구조의 중요한 판단

현재 \`HeaderPropagationHelper\`는 원 거래의 \`guid\`, \`traceId\`, 사용자·지점·채널과 호출 ServiceId를 하위 전문에 전달한다.

이 방향은 추적성 확보에 적절하다.

그러나 다음 사항은 추가로 필요하다.

\`\`\`text id="v8tnsq"
원 거래 GUID
→ 유지

TraceId
→ 유지

하위 호출 SpanId
→ 새로 생성

ParentSpanId
→ 상위 Span 전달

callDepth
→ 1 증가

callSequence
→ 같은 상위 거래 내 호출순번

remainingTimeoutMs
→ 잔여시간 전달

idempotencyKey
→ 변경 연계 시 파생·전파

# 23.1 데이터 소유권과 공개 계약

## 23.1.1 데이터 소유권

데이터 소유권은 특정 테이블을 누가 만들었는지를 뜻하지 않는다.

\`\`\`text id=“c4hd4f” 어떤 업무가 데이터의 의미를 정의하는가?

어떤 업무가 생성·변경·삭제 규칙을 결정하는가?

어떤 업무가 품질과 정합성을 책임지는가?

어떤 업무가 장애와 오류를 해결하는가?



예:

| 데이터 | 소유 업무 | 소비 업무 |
|---|---|---|
| 통합 고객 기본정보 | IC | SV·CM·CT |
| 고객 Single View | SV | IC·SS |
| 캠페인 | CM | SV·EP |
| 상품정보 | PD | SV·CM |
| 이벤트 수신정보 | EP | SV·CM |
| 사용자·권한정보 | OM·공통 | 전체 업무 |

\---

\## 23.1.2 조회 사용권과 변경 소유권

다른 업무가 데이터를 조회할 수 있다는 사실은 그 데이터를 변경할 수 있다는 뜻이 아니다.

\`\`\`text id="uykn8o"
SV가 IC 고객정보를 조회한다.
→ 허용된 공개 계약 사용

SV가 IC\_CUSTOMER Table을 UPDATE한다.
→ 금지

변경은 소유 업무가 수행해야 한다.

text id="jd1rxp" SV ↓ IC.Customer.updateContact ↓ IC 업무 Rule ↓ IC DAO·Mapper ↓ IC\_CUSTOMER UPDATE

## 23.1.3 다른 업무 Table 직접 접근의 문제

### 직접 조회

sql id="59ojzw" SELECT CUSTOMER\_NAME FROM IC\_CUSTOMER WHERE CUSTOMER\_NO = #{customerNo}

SV Mapper에서 직접 실행하면 다음 문제가 발생한다.

\`\`\`text id=“nokkha” IC의 논리삭제 조건 누락

IC의 개인정보 마스킹 우회

IC의 상태조건 우회

IC Schema 변경이 SV에 전파

IC 장애가 SV SQL 장애로 표현

IC 데이터 품질 책임 불명확


\### 직접 변경

\`\`\`sql id="w48uda"
UPDATE IC\_CUSTOMER
SET CUSTOMER\_NAME = #{customerName}
WHERE CUSTOMER\_NO = #{customerNo}

더 심각한 문제:

\`\`\`text id=“u229we” 업무 Rule 우회

Version·동시성 우회

업무이력 누락

감사로그 누락

Cache·Read Model 미갱신

외부 이벤트 미발행



다른 도메인의 DAO·Mapper·Table을 직접 사용하는 것은 도메인 설계의 금지사항이다.

\---

\## 23.1.4 직접 조회 예외

다음 경우에는 승인된 Read Model·View를 사용할 수 있다.

\`\`\`text id="zsjbb9"
통합 분석 조회

대규모 화면 조회

실시간 원격 호출 비용이 과도함

동일 데이터가 여러 업무에 반복 사용됨

최종 일관성을 허용할 수 있음

조건:

\`\`\`text id=“5w0lp5” 읽기 전용

소유 업무 명시

갱신 주기 명시

지연 허용시간 명시

데이터 품질·대사 책임

개인정보·권한 적용

직접 DML 금지


\---

\## 23.1.5 공개 계약이란 무엇인가

공개 계약은 단순 DTO 파일이 아니다.

다음 내용을 모두 포함한다.

\`\`\`text id="b6s5dy"
계약 이름

제공 업무

소비 업무

ServiceId 또는 Java Interface

요청 필드

응답 필드

필수·선택·Null 의미

코드값

정렬·페이징

정상 결과

업무 오류

시스템 오류

Timeout

재시도

멱등성

권한

개인정보

감사

버전

폐기일

## 23.1.6 공개 계약과 내부 구현

| 공개 가능 | 공개 금지 |
| --- | --- |
| Contract Interface | Service 구현체 |
| Contract Request·Response | DAO |
| ServiceId | Mapper |
| 업무 오류코드 | SQL |
| 코드값 의미 | DB Entity |
| Timeout 정책 | 내부 Table 구조 |
| 버전·호환성 | 내부 Utility |
| 담당 업무 | 구현 클래스 전체 |

소비 업무는 제공 업무의 내부 구현을 알아서는 안 된다.

## 23.1.7 계약 DTO

금지:

\`\`\`text id=“rsdgcx” SV → IC의 DB Row DTO 사용

SV → IcCustomerEntity 사용

SV → IcCustomerMapperResult 사용



권장:

\`\`\`text id="tap06p"
IC 공개 응답
→ CustomerProfileContractResponse

SV 내부 모델
→ SvCustomerProfile

변환:

\`\`\`java id=“dh1a8j” public SvCustomerProfile toDomain( IcCustomerProfileResponse response) {

return new SvCustomerProfile(
response.customerNo(),
response.customerName(),
response.customerGrade()
);

}


\---

\## 23.1.8 Contract DTO 설계

\`\`\`java id="p35dei"
public record CustomerProfileInquiryRequest(
String customerNo,
LocalDate baseDate
) {}

java id="rvxk3q" public record CustomerProfileInquiryResponse( String customerNo, String maskedCustomerName, String customerGradeCode, String branchCode, LocalDate baseDate, long dataVersion ) {}

원칙:

\`\`\`text id=“2n8vw6” 불변 객체

업무 의미 이름

DB 컬럼명 노출 최소화

화면 컴포넌트명 금지

개인정보 최소화

버전·기준일 포함

Null 의미 명시


\---

\## 23.1.9 계약 버전

대안:

\`\`\`text id="0i750j"
ServiceId 버전
IC.Customer.selectProfileV2

Header contractVersion
2.0

Schema Version
customer-profile-2.0

일반적으로 필드 추가는 하위호환으로 관리할 수 있지만 필드 삭제·타입변경·의미변경은 신규 버전이 필요하다.

## 23.1.10 하위호환 변경

비교적 호환 가능:

\`\`\`text id=“76t9as” 선택 응답 필드 추가

선택 Request 필드 추가

신규 오류코드 추가

Enum 코드 추가



주의:

\`\`\`text id="38p56b"
소비자가 알 수 없는 Enum을 실패 처리할 수 있음

엄격한 JSON Parser가 추가 필드를 거부할 수 있음

Contract Test가 필요하다.

## 23.1.11 비호환 변경

\`\`\`text id=“eyql3j” 필수 Request 필드 추가

응답 필드 삭제

String → Number 타입 변경

코드 의미 변경

Null 허용 → 금지

정렬 기준 변경

업무 오류를 정상으로 변경

Timeout 축소

마스킹 정책 변경



신규 Contract Version 또는 ServiceId를 사용한다.

\---

\## 23.1.12 연동 관리대장

| 항목 | 예 |
|---|---|
| 연동 ID | \`IF-IC-SV-001\` |
| 소비 업무 | IC |
| 제공 업무 | SV |
| 호출 ServiceId | \`SV.Customer.selectSummary\` |
| 거래코드 | \`SV-INQ-0002\` |
| 호출유형 | 동기 조회 |
| 필수·선택 | 필수 |
| 계약 버전 | 1.0 |
| Timeout | 1,200ms |
| 재시도 | 조회 1회 |
| 멱등성 | 조회 해당 없음 |
| 개인정보 | 고객명 마스킹 |
| 오류 매핑표 | 등록 |
| 담당자 | IC·SV 업무팀 |
| 운영 대시보드 | 연계 Metric |
| 폐기일 | 미정 |

연동 관리대장, ServiceId, Schema, Client Adapter, OM Catalog, Timeout, 오류 매핑표와 Contract Test가 함께 연결돼야 한다.

\---

\# 23.2 같은 WAR 내부 Contract

\## 23.2.1 같은 WAR라고 내부 구현을 직접 호출해도 되는가

같은 WAR는 하나의 JVM·Spring Context·배포 단위를 공유한다.

따라서 Java 호출은 빠르고 단순하다.

그러나 도메인이 다르면 내부 구현까지 공개해서는 안 된다.

금지:

\`\`\`text id="v4jsl6"
CustomerService
→ ProductServiceImpl

CustomerService
→ ProductDao

CustomerRule
→ ProductMapper

권장:

text id="zepmep" CustomerViewService → ProductHoldingQueryContract → ProductHoldingQueryContractImpl → ProductHoldingService

## 23.2.2 Application Contract

예:

\`\`\`java id=“ox2we6” public interface ProductHoldingQueryContract {

ProductHoldingContractResponse
selectHoldings(
ProductHoldingContractRequest request
);

}



구현:

\`\`\`java id="f6akjn"
@Component
@RequiredArgsConstructor
class ProductHoldingQueryContractImpl
implements ProductHoldingQueryContract {

private final ProductHoldingService service;

@Override
public ProductHoldingContractResponse
selectHoldings(
ProductHoldingContractRequest request) {

return service.selectForContract(request);
}
}

외부 도메인은 Interface와 Contract DTO만 참조한다.

## 23.2.3 Contract 패키지

권장:

text id="jq34md" com.nh.nsight.marketing.sv.product.contract ├─ ProductHoldingQueryContract.java └─ dto ├─ ProductHoldingContractRequest.java └─ ProductHoldingContractResponse.java

내부 구현:

text id="bdpvph" com.nh.nsight.marketing.sv.product ├─ service ├─ rule ├─ dao ├─ mapper └─ contract.internal

internal 구현은 다른 도메인에서 직접 참조하지 않는다.

## 23.2.4 계약 이름

좋은 이름:

\`\`\`text id=“xuk2vx” CustomerProfileQueryContract

ProductEligibilityContract

CampaignTargetValidationContract

ContactHistoryQueryContract



나쁜 이름:

\`\`\`text id="je1lr9"
CommonService

SharedService

UtilService

ProductService

Manager

공개 계약 이름은 제공하는 업무 능력을 표현해야 한다.

## 23.2.5 같은 WAR 조회 Contract 흐름

text id="3oaw2p" CustomerViewFacade ↓ CustomerViewService ↓ ProductHoldingQueryContract ↓ ProductHoldingService ↓ ProductHoldingDao ↓ Product Mapper

장점:

\`\`\`text id=“29qcxl” HTTP 직렬화 비용 없음

같은 Transaction Context 사용 가능

디버깅 단순

컴파일 시 계약 확인

제공 도메인 내부 구현 보호


\---

\## 23.2.6 같은 WAR 변경 Contract

변경 계약도 가능하지만 더 신중해야 한다.

\`\`\`java id="b4a77r"
public interface CustomerContactCommandContract {

ContactChangeContractResponse
changeContact(
ContactChangeContractRequest request
);
}

확인:

\`\`\`text id=“sz06np” 누가 유스케이스 Transaction을 소유하는가?

제공 도메인 업무 Rule이 모두 적용되는가?

호출자가 제공 도메인 상태를 임의 변경하지 않는가?

멱등성은 어디에서 관리하는가?

이력·감사는 누가 기록하는가?

제공 기능이 다른 WAR로 분리될 가능성이 있는가?


\---

\## 23.2.7 같은 WAR Transaction

기본:

\`\`\`text id="cl4km8"
상위 Facade
→ Transaction 시작

Application Contract
→ 같은 Transaction 참여

제공 Service
→ 변경

상위 Facade 정상 종료
→ 함께 Commit

장점:

text id="0drj47" 같은 DB Transaction Manager라면 원자성 확보 가능

주의:

\`\`\`text id=“l60pty” 두 도메인의 변경 이유가 항상 같은가?

하나의 Transaction으로 묶는 것이 장기 결합을 만들지 않는가?

향후 WAR 분리 시 계약 의미가 바뀌지 않는가?


\---

\## 23.2.8 독립 Transaction이 필요한 경우

다음과 같은 이유만으로 무분별하게 \`REQUIRES\_NEW\`를 사용하지 않는다.

\`\`\`text id="7f7f2m"
다른 도메인이므로 별도 Transaction

로그를 반드시 남겨야 하므로 별도 Commit

별도 Transaction이 필요하면 다음을 문서화한다.

\`\`\`text id=“13f0qs” 상위 Rollback 시 하위 Commit 유지 여부

부분 성공 상태

보상 방법

사용자 응답

이력·감사

재처리


\---

\## 23.2.9 같은 WAR라도 ServiceId 호출이 적합한 경우

다음 요구가 있다면 같은 WAR에서도 TCF 공개 거래 호출을 검토할 수 있다.

\`\`\`text id="aksvb1"
독립 Timeout

독립 거래통제

독립 권한

독립 거래로그

향후 WAR 분리 예정

동일 계약을 다른 WAR도 소비

운영 ServiceId 단위 관측 필요

단순히 계층을 복잡하게 만들기 위해 로컬 HTTP 호출을 사용하지 않는다.

## 23.2.10 Contract Exception

공개 Contract는 내부 예외를 그대로 노출하지 않는다.

\`\`\`java id=“qzdx4e” public class ProductEligibilityContractException extends RuntimeException {

private final String errorCode;

}



또는 표준 \`BusinessException\`을 사용하되 제공 도메인의 내부 기술예외는 숨긴다.

\---

\## 23.2.11 Null·빈 결과 계약

조회 결과 없음:

\`\`\`text id="d0avjr"
null

Optional.empty()

빈 List

NOT\_FOUND 오류

중 어떤 의미인지 계약으로 정한다.

권장 예:

\`\`\`text id=“9m6iqk” 단건 필수 조회 → NOT\_FOUND 업무 오류

선택 부가정보 → Optional.empty()

목록 → 빈 List


\---

\## 23.2.12 순환 Package 의존 방지

금지:

\`\`\`text id="scgnfk"
customer.contract
→ product.contract

product.contract
→ customer.contract

공통 Value가 필요하다면 플랫폼 공통이 아니라 명확한 상위 Contract 모듈 또는 기본형을 사용한다.

업무 모델 전체를 common으로 이동해 순환을 숨기지 않는다.

# 23.3 트랜잭션과 순환 의존

## 23.3.1 다른 WAR의 Transaction

text id="jvdlkg" IC Facade Transaction ↓ HTTP ↓ SV Facade Transaction

두 Transaction은 서로 독립적이다.

text id="1nhqoh" IC Transaction Rollback ≠ SV Transaction Rollback

상위 Transaction이 실패해도 이미 Commit된 하위 업무 변경은 자동 취소되지 않는다.

## 23.3.2 조회 연계의 Transaction

조회 연계:

text id="rc2afr" IC Transaction ↓ SV 조회 Transaction ↓ 응답

SV 조회는 데이터를 변경하지 않으므로 분산 Rollback 문제는 작다.

그러나 다음 문제는 여전히 존재한다.

\`\`\`text id=“2kwjly” 서로 다른 조회시점

하위 Timeout

응답 지연

데이터 Version 차이

하위 장애

개인정보 권한


\---

\## 23.3.3 변경 연계의 Transaction

\`\`\`text id="ow1v74"
IC DB 변경
↓
SV 변경 호출
↓
SV Commit
↓
IC 후속 SQL 실패
↓
IC Rollback

결과:

\`\`\`text id=“mqzkby” SV → 변경 완료

IC → 변경 취소

전체 업무 → 불일치



따라서 원격 변경을 하나의 로컬 Transaction처럼 설계하면 안 된다.

\---

\## 23.3.4 변경 연계 대안

\### 대안 1. 소유 업무 한 곳으로 유스케이스 이동

\`\`\`text id="jx8fza"
복합 변경을
실제 핵심 데이터 소유 업무에서 처리

가장 단순할 수 있다.

### 대안 2. Outbox·이벤트

text id="y971q5" IC DB 변경 + Outbox INSERT ↓ Commit ↓ SV 변경 이벤트 ↓ SV 처리

최종 일관성을 적용한다.

### 대안 3. 보상 거래

\`\`\`text id=“m6363t” SV 변경 성공

IC 실패

→ SV 변경취소 ServiceId 호출



보상도 실패할 수 있으므로 상태관리가 필요하다.

\### 대안 4. Saga·Orchestrator

\`\`\`text id="kvihn2"
업무 단계별 상태

성공 단계

실패 단계

보상 단계

재처리

를 명시적으로 관리한다.

### 대안 5. JTA·XA

기술적으로 가능할 수 있지만 다음을 검토한다.

\`\`\`text id=“7st59b” DB·자원 지원

운영 복잡성

장애복구

Heuristic 상태

성능

연계 HTTP 포함 불가



기본 선택으로 사용하지 않는다.

\---

\## 23.3.5 Timeout Budget

상위 거래 전체 Timeout이 3초라고 가정한다.

\`\`\`text id="v98m4d"
TCF 전체
3,000ms

예산 예:

\`\`\`text id=“4mdcfz” STF·Dispatcher 200ms

IC 자체 DB 500ms

SV 호출 1,200ms

응답 조립·ETF 300ms

안전 여유 800ms



하위 Timeout이 상위 Timeout보다 길어서는 안 된다.

금지:

\`\`\`text id="sr33yv"
상위 TCF Timeout
3초

SV Read Timeout
5초

상위 거래가 먼저 종료되고 하위 호출은 계속 실행될 수 있다.

## 23.3.6 순차 호출 Timeout

\`\`\`text id=“h0hpp6” SV 호출 1,000ms

PD 호출 1,000ms

CM 호출 1,000ms

자체 처리 500ms



최대 3.5초 이상이므로 상위 3초를 초과한다.

다음 중 하나를 선택한다.

\`\`\`text id="8ukdr5"
하위 Timeout 축소

호출 수 축소

병렬 호출

Read Model

상위 SLA 변경

선택 데이터 제외

## 23.3.7 병렬 호출 Timeout

세 조회를 병렬로 호출하면 이론적 시간은 합이 아니라 가장 느린 호출에 가까워질 수 있다.

\`\`\`text id=“tsdq1v” SV 800ms

PD 1,000ms

CM 500ms

병렬 → 약 1,000ms + 조립비용



그러나 다음을 검토한다.

\`\`\`text id="nkz6dw"
Executor Thread

Context 전파

MDC

하위 시스템 동시부하

DB Pool

부분 실패

모든 Future 취소

상위 잔여시간

## 23.3.8 Remaining Timeout

권장:

\`\`\`text id=“pba78s” 거래 시작 deadline = start + 3000ms

SV 호출 전 remaining = deadline - now

하위 Read Timeout = min( SV 정책 Timeout, remaining - 안전여유 )



하위 호출마다 전체 기본 Timeout을 새로 부여하지 않는다.

\---

\## 23.3.9 호출 깊이

\`\`\`text id="ef5uh0"
IC
→ SV
→ PD
→ CM
→ EP

호출 깊이가 증가하면 다음이 커진다.

\`\`\`text id=“ewi41u” 응답시간

장애 전파

Thread 점유

원인 분석

배포 결합

순환 가능성



프로젝트 기준 예:

\`\`\`text id="s3jjne"
권장 호출 깊이
2단계 이내

최대 호출 깊이
3단계

초과
→ 아키텍처 승인

정확한 수치는 프로젝트에서 확정한다.

## 23.3.10 순환 호출

text id="ok736v" IC.Customer.compose ↓ SV.Customer.selectSummary ↓ IC.Customer.selectBasic ↓ SV.Customer.selectSummary ↓ 반복

결과:

\`\`\`text id=“gtvop0” Thread 고갈

Timeout

과도한 로그

Stack·HTTP 호출 반복

장애 범위 확대


\---

\## 23.3.11 순환 호출 차단

Header:

\`\`\`text id="lsuq0n"
callerServiceId

parentServiceId

callDepth

callPath

예:

text id="d4g9v2" callPath IC.Customer.compose > SV.Customer.selectSummary > IC.Customer.selectBasic

현재 호출하려는 ServiceId가 이미 callPath에 존재하면 차단할 수 있다.

text id="m81v39" E-TCF-IF-LOOP-0001 내부 업무 순환 호출이 감지되었습니다.

## 23.3.12 구조적 순환 의존

코드 의존:

\`\`\`text id=“fg0ig0” ic-service → sv-service

sv-service → ic-service



Gradle 순환 의존이 발생하거나 공통 모듈로 우회하려는 문제가 생긴다.

금지:

\`\`\`gradle id="8m3n72"
dependencies {
implementation project(":sv-service")
}

업무 WAR 간 implementation project() 의존은 품질 Gate에서 차단한다.

## 23.3.13 순환 해결 대안

### 상위 Orchestrator

text id="49qxhb" IC ↔ SV

양방향 관계를 다음처럼 바꾼다.

text id="8d1v03" CustomerView Orchestrator ├─ IC Query └─ SV Query

### 이벤트

text id="l9taxy" IC 변경 → CustomerChanged Event → SV Read Model 갱신

### Read Model

text id="sjrz3u" 반복 상호 조회 → 통합 조회모델

### 책임 재정의

서로의 핵심 기능이 반복해서 필요하다면 도메인 경계 자체가 잘못됐는지 검토한다.

## 23.3.14 Retry

조회 연계:

\`\`\`text id=“4sucx5” 일시 Connection 오류

HTTP 503

Read Timeout



에 제한적으로 재시도를 적용할 수 있다.

조건:

\`\`\`text id="fhk7ka"
멱등 조회

전체 Timeout Budget 내

짧은 Backoff

최대 1회 등 제한

Circuit 상태 고려

## 23.3.15 변경 연계 Retry

변경 호출은 무조건 재시도하지 않는다.

필수 조건:

\`\`\`text id=“rk5gxy” 같은 Idempotency Key

제공 업무의 멱등성 저장소

업무 키 Unique

SUCCESS 재응답

TIMEOUT·UNKNOWN 상태조회



새로운 Idempotency Key로 재시도하면 새로운 변경으로 처리될 수 있다.

\---

\## 23.3.16 Circuit Breaker

내부 업무도 독립 WAR라면 장애가 전파될 수 있다.

\`\`\`text id="cpnp5s"
SV 장애
↓
IC Thread가 계속 SV 호출
↓
IC Thread·Pool 고갈
↓
IC 자체 기능도 장애

Circuit Open:

text id="8s0w99" SV 호출 실패율 임계치 초과 ↓ 일정 시간 원격 호출 중단 ↓ 즉시 선택 대체·실패

정상 복구:

text id="let0ti" OPEN → HALF\_OPEN → 제한 시험호출 → CLOSED

설정은 제24장에서 상세히 다룬다.

## 23.3.17 Bulkhead

대상별 동시호출 수를 제한한다.

\`\`\`text id=“a9qjd6” SV 호출 동시성 최대 50

PD 호출 동시성 최대 30



하나의 하위 업무 장애가 소비 업무의 전체 Thread를 점유하지 않게 한다.

\---

\# 23.4 조회 연계와 변경 연계

\## 23.4.1 조회 연계

조회 연계의 기본 특성:

\`\`\`text id="4uvqd7"
데이터 변경 없음

재시도 상대적으로 안전

부분 결과 허용 가능

병렬화 가능

Cache·Read Model 가능

전체 응답시간 영향

대표 예:

text id="iys6is" IC 고객 통합조회 ├─ IC 고객 기본 ├─ SV 고객요약 ├─ PD 보유상품 └─ CT 상담내역

## 23.4.2 필수 조회와 선택 조회

### 필수 조회

text id="qegaiu" SV 고객요약 없이는 IC 통합응답을 만들 수 없음

SV 실패:

text id="vctfm1" 상위 거래 전체 실패

### 선택 조회

text id="75ol7d" 최근 캠페인 정보는 부가정보임

CM 실패:

\`\`\`text id=“bix19r” 기본 고객정보는 반환

캠페인 영역은 unavailable

Warning 포함


\---

\## 23.4.3 부분 성공 Response

\`\`\`json id="7jb2x8"
{
"customer": {
"customerNo": "C000001",
"customerName": "홍\*동"
},
"productHoldings": \[\],
"campaigns": null,
"warnings": \[
{
"dependency": "CM",
"code": "W-IC-IF-0001",
"message": "캠페인 정보는 현재 조회할 수 없습니다."
}
\],
"partial": true
}

부분 성공은 사용자와 운영자가 알 수 있어야 한다.

\`\`\`text id=“d8ftf0” 빈 데이터 ≠ 실제 데이터 없음

null·unavailable = 하위 조회 실패


\---

\## 23.4.4 조회 연계 N+1

금지:

\`\`\`text id="qd23jb"
고객 100건 조회

for 고객:
SV.Customer.selectSummary 호출

총 100회 원격 호출

문제:

\`\`\`text id=“okrr1a” 응답시간 증가

하위 Thread·Pool 부하

네트워크 증가

Timeout 가능성

로그 폭증



대안:

\`\`\`text id="tpeoq5"
Bulk 조회 ServiceId

한 번에 고객번호 목록 전달

Read Model

Batch 선적재

화면 조회구조 변경

## 23.4.5 Bulk 조회 Contract

java id="af6mn6" public record CustomerSummaryBulkRequest( Set<String> customerNos, LocalDate baseDate ) {}

제한:

\`\`\`text id=“oftpqz” 최대 고객 수

Request 크기

Response 크기

개인정보 권한

Timeout

부분 실패



무제한 목록을 전달하지 않는다.

\---

\## 23.4.6 조회 Cache

같은 데이터가 반복 조회되고 짧은 지연을 허용할 수 있다면 Cache를 검토한다.

Cache Key:

\`\`\`text id="q3zjpb"
providerServiceId

customerNo

baseDate

permissionScope

contractVersion

주의:

\`\`\`text id=“ml1t22” 개인정보 Cache

권한별 결과

마스킹 결과

갱신 이벤트

TTL

삭제·권한변경


\---

\## 23.4.7 변경 연계

변경 연계의 기본 특성:

\`\`\`text id="25tx5h"
업무 상태 변경

중복 위험

부분 Commit 가능

재시도 위험

감사 필요

상태조회 필요

보상 가능성

대표 예:

text id="o2cf0v" CM 캠페인 승인 ↓ EP 실행대상 등록

## 23.4.8 변경 요청 Contract

json id="gw3i09" { "header": { "businessCode": "EP", "serviceId": "EP.Campaign.registerExecution", "transactionCode": "EP-CMD-0001", "processingType": "COMMAND", "guid": "G-20260718-000601", "traceId": "T-20260718-000601", "parentServiceId": "CM.Campaign.approve", "idempotencyKey": "IDEMP-CM-APP-001:EP-REGISTER" }, "body": { "campaignId": "CMP-20260718-000001", "campaignVersion": 5, "approvedAt": "2026-07-18T16:00:00+09:00" } }

## 23.4.9 파생 Idempotency Key

상위 Key:

text id="beaw4p" IDEMP-CM-APP-001

하위 단계별 Key:

\`\`\`text id=“7qotmh” IDEMP-CM-APP-001:EP-REGISTER

IDEMP-CM-APP-001:OM-AUDIT

IDEMP-CM-APP-001:NOTIFY



같은 하위 작업 재시도 시 같은 파생 Key를 사용한다.

\---

\## 23.4.10 처리유형 명시

현재 공통 호출 모델이 처리유형 미지정 시 조회형 기본값을 사용할 수 있다.

변경 연계에서는 반드시 명시한다.

\`\`\`java id="ym7b17"
IntegrationCallRequest request =
IntegrationCallRequest.builder()
.targetBusinessCode("EP")
.targetServiceId(
"EP.Campaign.registerExecution")
.targetTransactionCode(
"EP-CMD-0001")
.processingType("COMMAND")
.body(body)
.build();

변경 거래가 INQUIRY로 잘못 등록되면 다음 정책이 누락될 수 있다.

\`\`\`text id=“av0ei0” 멱등성

감사

Transaction

재시도 통제

중요 거래 분류


\---

\## 23.4.11 변경 연계 정상 흐름

\`\`\`text id="l0i9cd"
CM 승인 Transaction
↓
CM 상태 APPROVED
↓
Outbox EP\_REGISTER INSERT
↓
CM Commit
↓
Publisher
↓
EP.Campaign.registerExecution
↓
EP Idempotency PROCESSING
↓
EP 등록 Transaction
↓
EP Commit
↓
Idempotency SUCCESS
↓
Outbox 전송 완료

즉시 결과가 반드시 필요하지 않다면 이 구조가 안정적이다.

## 23.4.12 동기 변경 연계

동기 변경이 필요한 경우:

\`\`\`text id=“6b8lwl” 하위 변경 성공 여부 없이는 상위 응답을 확정할 수 없음

사용자가 즉시 결과를 알아야 함

보상 또는 상태 관리가 준비됨



처리:

\`\`\`text id="qxvh68"
상위 상태
PROCESSING

하위 변경 호출

성공
→ 상위 SUCCESS

실패
→ 상위 FAIL·보상

Timeout
→ 상위 UNKNOWN

## 23.4.13 변경 연계 Timeout

\`\`\`text id=“y60v07” CM → EP 변경 호출

Read Timeout



가능한 실제 결과:

\`\`\`text id="m0sssr"
EP 미처리

EP 처리 중

EP Commit 성공

응답만 유실

따라서:

text id="gzqa91" Timeout → 신규 Key 재호출 금지 → 같은 Key 상태조회 → 업무 키 조회 → UNKNOWN 상태

## 23.4.14 상태조회 ServiceId

변경 연계에는 상태조회 계약을 준비하는 것이 좋다.

text id="qtgpp6" EP.Campaign.selectExecutionStatus

요청:

json id="joqx9x" { "idempotencyKey": "IDEMP-CM-APP-001:EP-REGISTER", "campaignId": "CMP-20260718-000001" }

응답:

\`\`\`text id=“g4qedc” PROCESSING

SUCCESS

FAIL

UNKNOWN

NOT\_FOUND


\---

\## 23.4.15 오류 매핑

하위 오류를 무조건 상위 시스템 오류로 바꾸지 않는다.

예:

| 하위 오류 | 상위 처리 |
|---|---|
| \`E-SV-NFD-0001\` 고객 없음 | IC 고객 없음으로 매핑 |
| \`E-PD-BIZ-0003\` 상품 없음 | 빈 보유상품 또는 업무 오류 |
| \`E-CM-AUT-0001\` 권한 없음 | 상위 권한 오류 |
| \`E-TCF-TMO-0001\` Timeout | 상위 연계 Timeout |
| \`E-COM-SYS-0001\` 시스템 오류 | 상위 공통 시스템 오류 |
| 응답 Parsing 실패 | 연계 전문 오류 |

\---

\## 23.4.16 하위 오류 보존

운영로그에는 다음을 함께 기록한다.

\`\`\`text id="bw5f93"
consumerErrorCode

providerErrorCode

providerServiceId

providerGuid

traceId

elapsedMs

retryable

failureType

사용자 응답에는 하위 내부 구조와 시스템명을 과도하게 노출하지 않는다.

## 23.4.17 현재 Response 검증 보완

표준 응답 판정은 resultCode=S0000을 기준으로 수행한다.

실패 시 다음 항목을 읽어야 한다.

\`\`\`text id=“6hsoev” resultCode

errorCode

errorMessage

errorSystemId

guid

body

retryable 정책



단순 \`message\` 한 필드만 읽으면 제공 서비스의 실제 오류 의미를 잃을 수 있다.

\---

\## 23.4.18 내부 인증

내부 호출에는 두 신원이 존재한다.

\`\`\`text id="r2uchu"
원 사용자
U12345

호출 서비스
IC Service

제공 업무는 다음을 구분해야 한다.

\`\`\`text id=“c5xscj” 이 호출은 승인된 내부 서비스에서 왔는가?

원 사용자는 누구인가?

서비스가 사용자를 위임할 권한이 있는가?

어떤 권한으로 처리하는가?


\---

\## 23.4.19 인증 방식 대안

\### 사용자 JWT 전달

\`\`\`text id="7sbxx6"
IC
→ 원 Access Token 전달
→ SV가 사용자 권한 검증

장점:

text id="utnh20" 원 사용자 권한 유지

주의:

\`\`\`text id=“0qdye5” Audience

Token 전달 범위

하위 서비스 사용권한

Token 원문 로그


\### Service Token + 사용자 위임정보

\`\`\`text id="m8wdd3"
IC Service Token

\+ 검증된 원 사용자 Context

\+ 내부 서명

제공 업무는 서비스 신원과 사용자 신원을 모두 확인한다.

### mTLS + 내부 서명

네트워크·서비스 신원 확인에 사용한다.

사용자 데이터권한은 별도로 전달·검증한다.

## 23.4.20 사용자 Header만 전달하는 방식의 위험

\`\`\`text id=“n0nhkq” userId=U12345

branchId=001234



만 전달하면 제공 업무는 다음을 증명할 수 없다.

\`\`\`text id="ukw7pj"
누가 이 Header를 만들었는가?

중간에서 변경되지 않았는가?

호출 서비스가 승인됐는가?

사용자의 Token이 유효한가?

따라서 내부 호출자 인증과 사용자 위임방식을 확정해야 한다.

# 표준 내부 호출 Header

\`\`\`json id=“pohfe5” { “systemId”: “NSIGHT-MP”, “businessCode”: “SV”, “serviceId”: “SV.Customer.selectSummary”, “serviceName”: “고객 요약 조회”, “transactionCode”: “SV-INQ-0002”, “processingType”: “INQUIRY”,

“guid”: “G-20260718-000601”, “traceId”: “T-20260718-000601”, “spanId”: “S-0002”, “parentSpanId”: “S-0001”,

“callerBusinessCode”: “IC”, “callerServiceId”: “IC.Customer.selectIntegratedProfile”, “callDepth”: 1, “callSequence”: 2,

“userId”: “U12345”, “branchId”: “001234”, “channelId”: “WEBTOP”,

“requestTime”: “2026-07-18T16:10:00+09:00”, “remainingTimeoutMs”: 1200, “contractVersion”: “1.0” }



변경 거래 추가:

\`\`\`json id="pv7rxj"
{
"idempotencyKey":
"IDEMP-CM-APP-001:EP-REGISTER"
}

# Typed Client Adapter 예

\`\`\`java id=“4ob92m” @Component @RequiredArgsConstructor public class SvCustomerProfileClient {

private static final String TARGET\_BUSINESS =
"SV";

private static final String SERVICE\_ID =
"SV.Customer.selectSummary";

private static final String TRANSACTION\_CODE =
"SV-INQ-0002";

private final TcfServiceClient client;
private final ObjectMapper objectMapper;

public SvCustomerSummary
selectCustomerSummary(
CustomerNo customerNo,
TransactionContext context) {

IntegrationCallRequest request =
IntegrationCallRequest.builder()
.targetBusinessCode(
TARGET\_BUSINESS)
.targetServiceId(
SERVICE\_ID)
.targetTransactionCode(
TRANSACTION\_CODE)
.processingType("INQUIRY")
.body(
Map.of(
"customerNo",
customerNo.value()
)
)
.build();

IntegrationCallResult result =
client.call(
request,
context
);

SvCustomerSummaryContractResponse response =
objectMapper.convertValue(
result.getBody(),
SvCustomerSummaryContractResponse.class
);

validateContract(response);

return SvCustomerSummary.from(response);
}

}



목표 개선:

\`\`\`text id="vzpr7e"
Map 직접 사용 최소화

Contract DTO 자동변환

필수 필드 검증

Contract Version 검증

오류 매핑 분리

# 정상 처리 흐름

## 다른 WAR 조회

text id="13ui8s" 1. IC 화면 요청이 TCF로 진입한다. 2. IC Handler가 IC Facade를 호출한다. 3. IC Service가 SV 고객요약이 필요하다고 판단한다. 4. \`SvCustomerProfileClient\`가 Contract Request를 만든다. 5. \`TcfServiceClient\`가 대상 업무코드·ServiceId·거래코드를 확인한다. 6. 원 GUID·TraceId를 유지한다. 7. 신규 Span과 호출순번을 생성한다. 8. 잔여 Timeout Budget을 계산한다. 9. 검증된 사용자·서비스 신원을 전달한다. 10. SV \`/online\` Endpoint를 호출한다. 11. SV TCF가 Header·인증·권한·거래통제를 검증한다. 12. SV Handler·Facade·Service가 고객요약을 조회한다. 13. SV ETF가 표준 응답을 생성한다. 14. Client가 \`S0000\`과 Contract Version을 검증한다. 15. SV 응답을 IC 내부 모델로 변환한다. 16. IC가 자체 데이터와 조립한다. 17. IC ETF가 최종 응답을 반환한다.

## 같은 WAR Contract 조회

text id="f4tg4i" CustomerViewService ↓ ProductHoldingQueryContract ↓ ProductHoldingService ↓ Product DAO ↓ Contract Response ↓ Customer View Model

HTTP·표준 전문을 사용하지 않지만 공개 계약과 데이터 소유권은 유지한다.

# 오류·Timeout·장애 흐름

## 하위 업무 오류

text id="xo0c9v" SV Service ↓ 고객 없음 ↓ E-SV-NFD-0001 ↓ SV ETF 응답 ↓ IC Client 오류 해석 ↓ IC 업무정책에 따라 ├─ E-IC-NFD-0001 변환 └─ 선택 데이터 없음 처리

## 하위 시스템 오류

text id="m8zfq2" SV Mapper SQL 오류 ↓ SV systemError ↓ 표준 시스템 오류 응답 ↓ IC IntegrationSystemException ↓ IC Transaction Rollback 또는 부분 성공 ↓ GUID·TraceId 연결

## 하위 Timeout

\`\`\`text id=“d05r6r” IC 전체 잔여시간 1,200ms ↓ SV Read Timeout 1,000ms ↓ Timeout ↓ Client 호출 취소 ↓ 필수 의존 → IC 전체 실패

선택 의존 → Partial Response


\---

\## Circuit Open

\`\`\`text id="en7ubw"
SV 장애율 임계치 초과
↓
Circuit OPEN
↓
IC 신규 호출
↓
SV 네트워크 호출 없이 즉시 차단
↓
선택 대체 또는 빠른 실패

## 변경 Timeout

text id="u41ohu" CM → EP 변경 호출 ↓ Read Timeout ↓ EP 실제 Commit 여부 불명확 ↓ CM 상태 UNKNOWN ↓ 같은 Idempotency Key 상태조회 ↓ 운영 대사·재처리 판단

# 정상 예시

## 사례

\`\`\`text id=“beq6sk” 상위 ServiceId IC.Customer.selectIntegratedProfile

하위 ServiceId SV.Customer.selectSummary

호출 유형 동기 조회

필수 여부 필수

상위 Timeout 3,000ms

하위 Timeout 1,000ms

GUID 상위·하위 동일

TraceId 상위·하위 동일

Span 하위 신규

결과 SV S0000

상위 처리 SV Contract DTO를 IC 모델로 변환

최종 결과 SUCCESS


\---

\# 금지 예시

\`\`\`text id="auyswk"
다른 업무의 Mapper를 직접 호출한다.

다른 업무의 Table을 직접 UPDATE한다.

다른 WAR를 Gradle project 의존으로 참조한다.

제공 업무의 ServiceImpl을 직접 Import한다.

DB Entity를 공개 Contract DTO로 사용한다.

화면 DTO를 내부 업무 연계 Contract로 그대로 사용한다.

Map 구조만 사용하고 필수 필드를 검증하지 않는다.

다른 업무 Response를 변환 없이 화면에 반환한다.

계약 Version 없이 필드 의미를 변경한다.

같은 WAR라는 이유로 모든 Service를 public으로 노출한다.

상위 Transaction이 원격 하위 Transaction까지 Rollback한다고 가정한다.

원격 변경 성공 후 상위 실패에 대한 보상정책이 없다.

하위 Timeout을 상위 Timeout보다 길게 설정한다.

하위 호출마다 전체 Timeout을 다시 부여한다.

상위 GUID를 버리고 하위 GUID만 생성한다.

모든 내부 호출에서 같은 Span을 사용한다.

Call Depth와 Call Path를 관리하지 않는다.

IC → SV → IC 순환 호출을 허용한다.

조회목록의 각 Row마다 원격 호출한다.

변경 요청을 \`INQUIRY\` 처리유형으로 호출한다.

변경 연계에 Idempotency Key를 전달하지 않는다.

변경 Timeout 후 신규 Key로 다시 실행한다.

내부 사용자 Header만 전달하고 호출자 인증을 하지 않는다.

내부망이라는 이유로 JWT·mTLS·서명을 모두 생략한다.

하위 업무 오류를 모두 시스템 오류로 변환한다.

하위 시스템 오류 메시지를 사용자에게 그대로 노출한다.

선택 의존 실패를 빈 정상 데이터로 숨긴다.

Circuit Open 상태에서 계속 네트워크 호출한다.

Contract Test 없이 제공·소비 업무를 독립 배포한다.

# 연계 표준 속성

| 구분 | 필수 속성 |
| --- | --- |
| 식별 | 연동 ID·소비업무·제공업무 |
| 거래 | ServiceId·거래코드·처리유형 |
| 계약 | Request·Response·Version |
| 보안 | 사용자 위임·서비스 인증·권한 |
| 추적 | GUID·TraceId·SpanId·Parent |
| 시간 | 전체·Connect·Read Timeout |
| 실패 | 오류 매핑·Retryable·Partial |
| 변경 | Idempotency·상태조회·보상 |
| 운영 | Owner·SLA·Dashboard·Runbook |
| 변경관리 | 호환성·배포순서·폐기일 |

# 오류 매핑표 예

| 제공 오류 | 제공 의미 | 소비 오류 | 소비 처리 |
| --- | --- | --- | --- |
| E-SV-NFD-0001 | 고객 없음 | E-IC-NFD-0001 | 전체 실패 |
| E-SV-AUT-0001 | 데이터권한 없음 | E-IC-AUT-0002 | 권한 실패 |
| E-SV-BIZ-0004 | 기준일 오류 | E-IC-VAL-0003 | 입력 안내 |
| E-TCF-TMO-0001 | 하위 Timeout | E-IC-IF-0001 | 필수 실패 |
| E-COM-SYS-0001 | 하위 시스템 오류 | E-IC-IF-0002 | 시스템 실패 |
| Contract Parsing 오류 | 계약 불일치 | E-IC-IF-0003 | 배포·계약 점검 |

# 성능·용량·확장성

## 호출 수 계산

\`\`\`text id=“lijqht” 상위 TPS 100

거래당 SV 호출 1회

SV 유입 TPS 100



만약 거래당 10회 호출하면:

\`\`\`text id="0yfjoz"
SV 유입 TPS
1,000

상위 TPS만 보고 하위 용량을 산정하면 안 된다.

## Fan-Out

text id="6g7qs6" IC 한 거래 ├─ SV 1회 ├─ PD 1회 ├─ CT 1회 └─ CM 1회

상위 100 TPS:

text id="9aspsm" 내부 연계 최대 400 TPS

각 대상의 Thread·Pool·DB 용량에 반영한다.

## Connection Pool

원격 호출 중 상위 DB Transaction을 열어 두면 다음 자원이 동시에 점유될 수 있다.

\`\`\`text id=“20y3dt” 상위 DB Connection

상위 Tomcat Thread

하위 Tomcat Thread

하위 DB Connection



조회 조립에서는 가능하면 원격 호출 전후 Transaction 범위를 최소화한다.

\---

\## 병렬 Executor

확인:

\`\`\`text id="4rqodk"
최대 Thread

Queue 크기

Task Timeout

Context 전파

MDC Clear

취소

대상별 Bulkhead

서버 종료 처리

공용 무제한 Executor를 사용하지 않는다.

## Response 크기

내부 시스템이라고 대형 Response를 허용하지 않는다.

\`\`\`text id=“xouw0c” 최대 Row

최대 Byte

최대 목록 수

대형 CLOB·BLOB 제외

파일은 별도 경로


\---

\# 데이터 및 상태관리

\## 연계 호출 상태

\`\`\`text id="zxqybi"
REQUESTED

PROCESSING

SUCCESS

BUSINESS\_FAIL

SYSTEM\_FAIL

TIMEOUT

UNKNOWN

COMPENSATING

COMPENSATED

모든 조회 호출을 DB에 저장할 필요는 없다.

다음 변경 연계는 상태 저장을 검토한다.

\`\`\`text id=“d1s0yg” 등록

변경

승인

삭제

외부 전송

금액·실적 변경

대량 처리


\---

\## 내부 연계 이력 예

\`\`\`sql id="ok7wx8"
CREATE TABLE OM\_INTERNAL\_CALL\_HISTORY (
CALL\_ID VARCHAR2(50) NOT NULL,
GUID VARCHAR2(100) NOT NULL,
TRACE\_ID VARCHAR2(100) NOT NULL,
SPAN\_ID VARCHAR2(100) NOT NULL,
PARENT\_SPAN\_ID VARCHAR2(100),
CALLER\_SERVICE\_ID VARCHAR2(100) NOT NULL,
TARGET\_SERVICE\_ID VARCHAR2(100) NOT NULL,
CALL\_TYPE VARCHAR2(20) NOT NULL,
CALL\_STATUS VARCHAR2(20) NOT NULL,
ERROR\_CODE VARCHAR2(50),
ELAPSED\_MS NUMBER(10),
RETRY\_COUNT NUMBER(5),
CREATED\_AT TIMESTAMP NOT NULL,
CONSTRAINT PK\_OM\_INTERNAL\_CALL\_HISTORY
PRIMARY KEY (CALL\_ID)
);

모든 호출을 DB 동기 INSERT하면 부하가 증가할 수 있으므로 거래로그·Metric·Trace 저장방식을 함께 검토한다.

# 보안·개인정보·감사

| 영역 | 기준 |
| --- | --- |
| 사용자 신원 | 검증된 AuthenticationContext |
| 서비스 신원 | Service Token·mTLS·서명 |
| Header | 사용자 입력값 단독 신뢰 금지 |
| 개인정보 | 계약에 필요한 최소필드 |
| 마스킹 | 제공 업무가 기본 적용 |
| 원문 권한 | 소비·제공 양쪽 확인 |
| Token | 로그 기록 금지 |
| 내부 호출 | Allow List |
| ServiceId | 승인된 호출관계 |
| 변경 연계 | 감사·멱등성 |
| 대량 조회 | 별도 권한·감사 |
| 오류 | 하위 시스템 내부정보 노출 금지 |

# 운영·모니터링·장애 대응

## 권장 로그

text id="ohnvmj" event=INTERNAL\_SERVICE\_CALL\_COMPLETED guid=G-20260718-000601 traceId=T-20260718-000601 spanId=S-0002 parentSpanId=S-0001 callerServiceId=IC.Customer.selectIntegratedProfile targetServiceId=SV.Customer.selectSummary callType=INQUIRY result=SUCCESS providerResultCode=S0000 elapsedMs=187 retryCount=0

## 권장 Metric

\`\`\`text id=“40i9lb” tcf.internal.call.count

tcf.internal.call.duration

tcf.internal.call.timeout.count

tcf.internal.call.business.fail.count

tcf.internal.call.system.fail.count

tcf.internal.call.circuit.open.count

tcf.internal.call.retry.count

tcf.internal.call.partial.count

tcf.internal.call.depth

tcf.internal.call.byTarget



Metric Label:

\`\`\`text id="ful5me"
callerServiceId

targetServiceId

resultType

targetBusinessCode

contractVersion

GUID·userId를 Metric Label에 넣지 않는다.

## 장애 점검 순서

text id="0mohks" 1. 상위 GUID·ServiceId 확인 2. 하위 호출 로그 확인 3. Target ServiceId 확인 4. 연계 Endpoint·Route 확인 5. Connect·Read Timeout 확인 6. 하위 거래로그 확인 7. 하위 업무·시스템 오류 구분 8. Circuit·Bulkhead 상태 확인 9. Idempotency 상태 확인 10. 상위 Transaction·부분 반영 확인 11. 재처리·보상·대사 판단

## 장애별 운영 조치

| 장애 | 운영 조치 |
| --- | --- |
| 대상 WAR 중단 | Route·배포·Health 확인 |
| 하위 Timeout | Slow 거래·DB Pool·Thread 확인 |
| 계약 Parsing 오류 | 제공·소비 배포버전 확인 |
| 순환 호출 | Trace Call Path 확인 |
| 부분 성공 증가 | 선택 의존 상태와 SLA 확인 |
| 중복 변경 | Idempotency Key 확인 |
| UNKNOWN 증가 | 상태조회·대사 수행 |
| Circuit Open | 하위 복구 후 Half-Open 확인 |
| N+1 호출 | 호출 수·화면 Query 구조 개선 |

도메인 연계 운영기준도 Timeout, 제공 WAR 중단, 계약 오류, 이벤트 적체, 중복 처리와 순환 호출을 주요 장애유형으로 관리한다.

# 책임 경계와 RACI

| 활동 | 소비업무 | 제공업무 | AA | FW·EAI | 보안 | 운영 | QA | DA |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 연계 필요 정의 | A/R | C | C | I | C | I | C | C |
| 데이터 소유권 | C | A/R | C | I | C | I | I | A/R |
| 공개 Contract | C | A/R | C | C | C | I | C | C |
| Client Adapter | A/R | C | C | C | I | I | C | I |
| ServiceId | C | A/R | C | C | I | C | C | I |
| Timeout Budget | R | C | A | C | I | R/C | C | I |
| 오류 매핑 | A/R | R/C | C | C | I | C | C | I |
| 멱등성 | R | A/R | C | C | I | C | C | C |
| 서비스 인증 | C | C | C | R | A/R | C | C | I |
| 데이터권한 | C | A/R | C | I | A/C | I | C | C |
| Contract Test | R | R | C | C | C | I | A/R | I |
| 장애 Runbook | C | C | C | C | I | A/R | C | I |
| 폐기·전환 | C | A/R | A/C | C | C | R/C | C | I |

# 자동검증 및 품질 Gate

## 1\. Package 의존 Gate

검출:

\`\`\`text id=“u1slwt” 다른 도메인 DAO 참조

다른 도메인 Mapper 참조

다른 WAR 구현 클래스 참조

Rule → Client

Handler → Client

업무 WAR 간 project dependency

순환 Package 의존



ArchUnit 등으로 자동검증한다.

\---

\## 2. SQL 소유권 Gate

Mapper SQL에서 다른 업무 소유 Table의 다음 구문을 검출한다.

\`\`\`text id="bya5bt"
INSERT

UPDATE

DELETE

MERGE

다른 업무 Table 조회도 승인된 Read Model·View가 아니면 리뷰 대상으로 등록한다.

## 3\. Contract Gate

\`\`\`text id=“op2w2f” Request Schema

Response Schema

필수·선택

Null

코드값

Version

오류코드

Timeout

개인정보

멱등성


\---

\## 4. Header Gate

\`\`\`text id="a1cchz"
guid

traceId

callerServiceId

targetServiceId

userContext

remainingTimeout

processingType

변경 거래 idempotencyKey

## 5\. Timeout Gate

text id="0tu4er" Connect Timeout < Read Timeout < 상위 Remaining Timeout < 전체 TCF Timeout

순차 호출 총예산을 자동 또는 리뷰로 확인한다.

## 6\. 변경 연계 Gate

\`\`\`text id=“4twt99” processingType=COMMAND

Idempotency Key

업무 Key

상태조회

Timeout UNKNOWN

보상·Outbox

감사


\---

\## 7. 순환 호출 Gate

\`\`\`text id="0ru3tt"
정적 Package Cycle

Gradle Cycle

ServiceId Call Graph Cycle

Runtime callPath Cycle

최대 callDepth

## 8\. Contract Test Gate

제공자 테스트:

\`\`\`text id=“gxdy4z” 요청 형식 수용

응답 필드

오류코드

Null·빈 결과

하위호환



소비자 테스트:

\`\`\`text id="u7hizh"
응답 Parsing

신규 필드 허용

알 수 없는 Enum

오류 매핑

Timeout·Partial

계약 테스트와 장애 시험은 내부 연계의 필수 품질 Gate다.

# 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| INT-001 | 같은 도메인 내부 호출 | 정상 |
| INT-002 | 동일 WAR 공개 Query Contract | 정상 |
| INT-003 | 동일 WAR 구현 Service 직접 참조 | Gate 실패 |
| INT-004 | 다른 도메인 Mapper 참조 | Gate 실패 |
| INT-005 | 다른 도메인 Table UPDATE | Gate 실패 |
| INT-006 | 다른 WAR Java Import | Gate 실패 |
| INT-007 | 업무 WAR Gradle 직접 의존 | Gate 실패 |
| INT-008 | 정상 ServiceId 호출 | 성공 |
| INT-009 | 대상 업무코드 설정 없음 | 설정 오류 |
| INT-010 | 대상 URL 오류 | 연계 시스템 오류 |
| INT-011 | GUID 전파 | 상·하위 동일 |
| INT-012 | TraceId 전파 | 상·하위 동일 |
| INT-013 | Child Span 생성 | Parent 연결 |
| INT-014 | callerServiceId | 정확히 전달 |
| INT-015 | 사용자 Context | 검증값 전달 |
| INT-016 | 사용자 Header 위변조 | 차단 |
| INT-017 | 내부 서비스 인증 없음 | 차단 |
| INT-018 | 허용되지 않은 호출 업무 | 권한 오류 |
| INT-019 | 정상 Contract Version | 성공 |
| INT-020 | 지원하지 않는 Version | 계약 오류 |
| INT-021 | 응답 필수필드 누락 | 계약 오류 |
| INT-022 | 응답 추가필드 | 하위호환 |
| INT-023 | 알 수 없는 Enum | 정책대로 처리 |
| INT-024 | 하위 업무 오류 | 매핑된 상위 오류 |
| INT-025 | 하위 시스템 오류 | 연계 시스템 오류 |
| INT-026 | 하위 Timeout | 연계 Timeout |
| INT-027 | Connect Timeout | 빠른 실패 |
| INT-028 | Read Timeout | 예산 내 실패 |
| INT-029 | 하위 Timeout > 상위 | Gate 실패 |
| INT-030 | Remaining Timeout 부족 | 호출 전 차단 |
| INT-031 | 필수 의존 실패 | 상위 전체 실패 |
| INT-032 | 선택 의존 실패 | Partial 응답 |
| INT-033 | Partial Warning | 화면 표시 |
| INT-034 | 빈 정상 데이터 | unavailable과 구분 |
| INT-035 | 조회 일시 오류 | 제한 Retry |
| INT-036 | 조회 Retry 성공 | 정상 |
| INT-037 | Retry 예산 초과 | 즉시 실패 |
| INT-038 | 변경 호출 정상 | 한 번 반영 |
| INT-039 | 변경 처리유형 누락 | Gate 실패 |
| INT-040 | 변경 Idempotency 누락 | 호출 차단 |
| INT-041 | 같은 변경 재요청 | 기존 결과 |
| INT-042 | 같은 Key 다른 Body | 차단 |
| INT-043 | 변경 Timeout | UNKNOWN |
| INT-044 | Timeout 신규 Key 재호출 | 금지 |
| INT-045 | 상태조회 SUCCESS | 재실행 없음 |
| INT-046 | 상태조회 FAIL | 정책상 재처리 |
| INT-047 | 원격 성공·상위 Rollback | 보상 대상 |
| INT-048 | 보상 성공 | 정합성 복구 |
| INT-049 | 보상 실패 | 수동조치 |
| INT-050 | Outbox 정상 | 최종 일관성 |
| INT-051 | 중복 이벤트 | 한 번 처리 |
| INT-052 | IC→SV→IC 호출 | Loop 차단 |
| INT-053 | 최대 Call Depth 초과 | 차단 |
| INT-054 | 100건 N+1 호출 | 성능 Gate 실패 |
| INT-055 | Bulk 조회 | 1회 호출 |
| INT-056 | 병렬 3개 조회 | 예산 내 성공 |
| INT-057 | 병렬 Context | MDC·인증 유지 |
| INT-058 | Circuit Open | 즉시 실패·대체 |
| INT-059 | Circuit Half-Open | 제한 복구 |
| INT-060 | Bulkhead 초과 | 빠른 제한 |
| INT-061 | 제공자 신버전·소비자 구버전 | 하위호환 |
| INT-062 | 필수 필드 삭제 | 계약 Gate 실패 |
| INT-063 | 개인정보 과다 응답 | 보안 Gate 실패 |
| INT-064 | Token 원문 로그 | 보안 Gate 실패 |
| INT-065 | 호출 Metric | 대상별 집계 |
| INT-066 | GUID 장애 추적 | 상·하위 연결 |
| INT-067 | 제공 WAR 중단 | 영향 범위 확인 |
| INT-068 | 배포 순서 역전 | 호환성 확인 |
| INT-069 | Deprecated 계약 호출 | 경고 |
| INT-070 | 계약 폐기 후 호출 | 대체 코드 안내 |

# 따라 하는 실무 절차

## 1단계. 데이터 소유권을 확인한다

완료 증적:

\`\`\`text id=“c51yyw” 소비 업무

제공 업무

소유 Table

조회·변경 권한

업무 담당자


\---

\## 2단계. 연계방식을 선택한다

\`\`\`text id="d9rzjg"
동일 WAR Contract

다른 WAR ServiceId

Event·Outbox

Read Model

Batch

선택 근거를 ADR에 기록한다.

## 3단계. 공개 계약을 작성한다

\`\`\`text id=“5jhoy9” Request

Response

필수·선택

오류

Timeout

권한

개인정보

Version


\---

\## 4단계. 호출 관계를 등록한다

\`\`\`text id="cdp89n"
연동 ID

callerServiceId

targetServiceId

필수·선택

SLA

담당자

## 5단계. Client Adapter를 구현한다

\`\`\`text id=“425cka” Typed Request

표준 전문 생성

Response 변환

오류 매핑

Body 로그 금지


\---

\## 6단계. 추적정보를 전파한다

\`\`\`text id="t3rksi"
GUID

TraceId

SpanId

ParentSpanId

callDepth

callerServiceId

## 7단계. Timeout Budget을 작성한다

\`\`\`text id=“u4ltck” 전체 Timeout

자체 처리

하위 호출

안전 여유

재시도


\---

\## 8단계. 조회·변경 정책을 구분한다

\`\`\`text id="uuv6lm"
조회
→ Retry·Partial

변경
→ Idempotency·UNKNOWN·보상

## 9단계. 보안 경계를 검증한다

\`\`\`text id=“0ae41k” 서비스 인증

사용자 위임

기능권한

데이터권한

개인정보


\---

\## 10단계. Contract Test를 작성한다

\`\`\`text id="08cd9o"
정상

Null

신규 필드

오류코드

Version

Timeout

## 11단계. 장애 테스트를 수행한다

\`\`\`text id=“wn215k” 대상 중단

Slow 응답

Circuit

부분 성공

순환 호출

원격 변경 Timeout


\---

\## 12단계. 운영 증적을 확인한다

\`\`\`text id="ic7pq2"
상위 로그

하위 로그

GUID·Trace

Metric

Idempotency

대사

Runbook

# 완료 체크리스트

## 데이터 소유권

| 확인 항목 | 완료 |
| --- | --- |
| 소비·제공 업무를 구분했다. | □ |
| 데이터 소유 업무가 명확하다. | □ |
| 다른 업무 Table DML이 없다. | □ |
| 다른 업무 Mapper 호출이 없다. | □ |
| 승인된 Read Model 여부를 확인했다. | □ |
| 변경은 소유 업무 ServiceId를 사용한다. | □ |

## 공개 계약

| 확인 항목 | 완료 |
| --- | --- |
| 공개 계약 이름이 업무 능력을 표현한다. | □ |
| 내부 구현이 노출되지 않는다. | □ |
| Contract DTO가 별도다. | □ |
| 필수·선택·Null 의미가 있다. | □ |
| 오류코드가 정의됐다. | □ |
| 개인정보 분류가 있다. | □ |
| Contract Version이 있다. | □ |
| 하위호환 정책이 있다. | □ |

## 같은 WAR

| 확인 항목 | 완료 |
| --- | --- |
| Application Contract를 사용한다. | □ |
| 구현 Service를 직접 참조하지 않는다. | □ |
| DAO·Mapper를 참조하지 않는다. | □ |
| Transaction 참여정책이 명확하다. | □ |
| 순환 Package 의존이 없다. | □ |
| 향후 WAR 분리 가능성을 검토했다. | □ |

## 다른 WAR

| 확인 항목 | 완료 |
| --- | --- |
| TcfServiceClient를 사용한다. | □ |
| 업무코드·ServiceId·거래코드가 정확하다. | □ |
| 처리유형이 명확하다. | □ |
| Endpoint 설정이 있다. | □ |
| GUID·TraceId가 전파된다. | □ |
| Child Span이 있다. | □ |
| 호출자 ServiceId가 있다. | □ |
| 내부 호출자 인증이 있다. | □ |
| 사용자 위임정책이 있다. | □ |

## Timeout·장애

| 확인 항목 | 완료 |
| --- | --- |
| 전체 Timeout Budget이 있다. | □ |
| Connect·Read Timeout이 있다. | □ |
| 하위 Timeout이 상위보다 짧다. | □ |
| 잔여 Timeout을 계산한다. | □ |
| 필수·선택 의존을 구분했다. | □ |
| Partial Response 정책이 있다. | □ |
| Retryable 오류를 정의했다. | □ |
| Circuit·Bulkhead를 검토했다. | □ |
| 순환 호출을 차단한다. | □ |
| 최대 Call Depth가 있다. | □ |

## 변경 연계

| 확인 항목 | 완료 |
| --- | --- |
| COMMAND 처리유형이다. | □ |
| Idempotency Key를 전달한다. | □ |
| 같은 재시도에서 같은 Key를 사용한다. | □ |
| 상태조회 ServiceId가 있다. | □ |
| Timeout 시 UNKNOWN을 처리한다. | □ |
| 보상·Outbox 정책이 있다. | □ |
| 원격 Transaction 독립성을 이해했다. | □ |
| 변경 감사로그가 있다. | □ |

## 운영·품질

| 확인 항목 | 완료 |
| --- | --- |
| 연동 관리대장이 있다. | □ |
| 오류 매핑표가 있다. | □ |
| Contract Test가 있다. | □ |
| 장애 테스트가 있다. | □ |
| N+1 원격 호출이 없다. | □ |
| Metric이 대상 ServiceId별로 집계된다. | □ |
| GUID로 상·하위 로그를 연결한다. | □ |
| 운영 Runbook이 있다. | □ |
| 폐기·전환 계획이 있다. | □ |

# 변경·호환성·폐기 관리

## 응답 필드 추가

text id="l4hsi3" 기존 소비자가 추가 필드를 무시하는지 확인한다.

엄격한 Parser·DTO 설정을 Contract Test로 검증한다.

## 필수 Request 필드 추가

비호환 변경이다.

text id="gvqp3w" 기존 소비자 → 신규 필드 미전송 → Validation 실패

신규 Contract Version 또는 ServiceId를 만든다.

## 오류코드 변경

제공 오류코드가 바뀌면 소비 업무의 매핑표와 화면 분기가 영향을 받는다.

기존 오류코드의 의미를 변경하지 않는다.

## Timeout 축소

\`\`\`text id=“jwhq23” 기존 1,500ms

신규 700ms



소비 업무의 전체 SLA와 오류율이 달라진다.

성능·장애시험과 소비자 승인이 필요하다.

\---

\## 조회에서 변경으로 의미 변경

기존 조회 ServiceId에 데이터 변경을 추가하면 안 된다.

\`\`\`text id="bav1c0"
INQUIRY
→ COMMAND

신규 ServiceId를 만든다.

## 동일 WAR에서 다른 WAR로 분리

\`\`\`text id=“kgnqbh” 기존 Application Contract

신규 TCF ServiceId



영향:

\`\`\`text id="0grtbq"
Transaction 원자성 상실

직렬화

Timeout

인증

오류 매핑

배포

호환성

보상

단순 Package 이동이 아니다.

## 계약 폐기

text id="0t9jbw" 신규 소비자 등록 중지 ↓ DEPRECATED 표시 ↓ 대체 ServiceId 제공 ↓ 소비자 목록 확인 ↓ 호출량 모니터링 ↓ 소비자 전환 ↓ 호출량 0 확인 ↓ Provider Handler 비활성 ↓ Client Adapter 제거 ↓ 계약 이력 보존

# 시사점

## 핵심 아키텍처 판단

첫째, 내부 업무 연계의 출발점은 URL이나 Java 클래스가 아니라 **데이터 소유권**이다.

둘째, 동일 WAR에서는 Java 호출을 사용할 수 있지만 다른 도메인의 내부 Service·DAO·Mapper를 직접 참조하지 않고 공개 Application Contract를 사용해야 한다.

셋째, 다른 WAR는 Java 모듈 의존이 아니라 ServiceId 기반 표준 전문으로 호출해야 한다.

넷째, 원격 업무의 Transaction은 호출자의 로컬 Transaction에 포함되지 않는다.

\`\`\`text id=“n3plcr” 원격 변경 → 독립 Commit

상위 실패 → 자동 Rollback 불가



다섯째, 조회와 변경은 재시도 정책이 다르다.

\`\`\`text id="osn03o"
조회
→ 제한적 재시도 가능

변경
→ 같은 Idempotency Key와 상태확인 필수

여섯째, 내부 연계 성능은 상위 TPS가 아니라 거래당 Fan-Out과 호출 깊이를 포함해 산정해야 한다.

일곱째, 순환 호출은 코드 순환 의존뿐 아니라 Runtime ServiceId 호출경로에서도 차단해야 한다.

여덟째, 반복되는 복합조회는 원격 호출을 계속 추가하기보다 Read Model을 검토해야 한다.

## 주요 위험

| 위험 | 결과 |
| --- | --- |
| 다른 업무 Mapper 직접 호출 | 데이터 소유권 훼손 |
| 다른 WAR Java 의존 | 독립 배포 불가 |
| Contract 없는 Service 호출 | 내부 변경 전파 |
| Map 중심 계약 | Runtime 오류 증가 |
| Contract Version 없음 | 배포 시 장애 |
| Header 사용자 신뢰 | 신원 위변조 |
| 내부 서비스 인증 없음 | 비인가 호출 |
| 하위 Timeout 과대 | 상위 거래 잔존 |
| 다단계 동기 호출 | Thread·Pool 고갈 |
| 원격 N+1 | 처리량 급감 |
| 무분별한 Retry | 장애 부하 증폭 |
| 변경 Idempotency 없음 | 중복 변경 |
| Timeout 후 신규 Key | 이중 처리 |
| 원격 변경 Rollback 오해 | 업무 불일치 |
| Partial 결과 은폐 | 잘못된 사용자 판단 |
| 순환 ServiceId 호출 | Timeout·전체 장애 |
| 하위 오류 전부 시스템화 | 업무 의미 손실 |
| 계약 폐기 미관리 | 소비자 장애 |

## 우선 보완 과제

1.  전체 업무 데이터 소유권 표를 작성한다.
2.  Cross-Domain Service·DAO·Mapper 참조를 정적 분석한다.
3.  업무 WAR 간 Gradle 직접 의존을 제거한다.
4.  동일 WAR 공개 Contract 패키지 표준을 적용한다.
5.  TcfServiceClient의 Map 중심 계약을 Typed DTO 중심으로 개선한다.
6.  Contract Version과 Schema 검증을 추가한다.
7.  변경 연계에 Processing Type과 Idempotency Key를 필수화한다.
8.  GUID·TraceId 외 Span·ParentSpan·Call Depth를 추가한다.
9.  상위 잔여 Timeout Budget을 하위 Client에 전달한다.
10.  내부 서비스 인증과 사용자 위임방식을 확정한다.
11.  하위 errorCode·errorMessage·Retryable 정보를 보존한다.
12.  필수·선택 의존과 Partial Response 표준을 정의한다.
13.  Runtime ServiceId 호출 Loop 검증을 구현한다.
14.  원격 N+1 호출을 성능 Gate에서 탐지한다.
15.  Contract Test·Timeout·Circuit·보상 테스트를 CI/CD에 포함한다.

## 중장기 발전 방향

\`\`\`text id=“rrrpnj” 직접 Service·Mapper 참조 ↓ 공개 Application Contract

Map 기반 원격 호출 ↓ Typed Contract DTO

GUID 단일 추적 ↓ Trace·Span·Call Graph

개별 Timeout ↓ End-to-End Timeout Budget

동기 변경 호출 ↓ Outbox·Saga·보상

반복 Fan-Out 조회 ↓ Read Model

수동 계약 관리 ↓ Schema Registry·Contract Test

개별 로그 분석 ↓ OM 내부 호출 그래프·장애 영향 분석


\---

\# 마무리말

내부 업무 연계를 설계하는 과정은 다음 질문에 답하는 일이다.

\`\`\`text id="5zxayo"
필요한 데이터는 어느 업무가 소유하는가?

소비 업무는 조회만 하는가, 변경까지 하는가?

같은 WAR인가, 다른 WAR인가?

공개된 계약은 무엇인가?

내부 Service·DAO·Mapper가 노출되지 않았는가?

ServiceId와 거래코드는 무엇인가?

요청·응답 계약의 Version은 무엇인가?

GUID·TraceId·Span은 어떻게 전달되는가?

원 사용자와 호출 서비스 신원은 어떻게 검증되는가?

상위 거래의 남은 Timeout은 얼마인가?

하위 호출은 필수인가, 선택인가?

하위 업무 오류를 상위에서 어떻게 해석하는가?

조회 재시도와 변경 재시도는 어떻게 다른가?

변경 연계의 Idempotency Key는 무엇인가?

Timeout 후 실제 처리상태를 어떻게 확인하는가?

상위 Rollback 후 원격 변경을 어떻게 보상하는가?

순환 호출과 N+1 호출을 어떻게 차단하는가?

계약 변경 시 어떤 소비자가 영향을 받는가?

운영자는 GUID 하나로 전체 호출경로를 볼 수 있는가?

제23장의 핵심 흐름은 다음과 같다.

text id="k45gdp" 데이터 소유권 확인 ↓ 공개 계약 정의 ↓ 동일 WAR·다른 WAR 방식 선택 ↓ Client Adapter ↓ 보안·추적 Context 전파 ↓ Timeout Budget ↓ 오류·Partial·Retry 정책 ↓ 변경 멱등성·보상 ↓ Contract Test·운영 추적

가장 중요한 원칙은 다음과 같다.

\`\`\`text id=“1mognc” 내부 호출이라고 해서 내부 구현을 공유해서는 안 된다.

같은 데이터베이스라고 해서 다른 업무의 데이터를 직접 변경해서도 안 된다.

데이터 소유권과 공개 계약, 트랜잭션과 Timeout, 멱등성과 추적성이 함께 정의돼야

업무가 늘어나도 서로의 변경과 장애를 견딜 수 있는 안전한 내부 연계가 된다. \`\`\`
