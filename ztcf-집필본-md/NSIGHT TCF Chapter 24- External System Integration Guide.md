<!-- source: ztcf-집필본/NSIGHT TCF Chapter 24- External System Integration Guide.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제24장. 외부 시스템 호출

## 도입 전 안내말

제23장에서는 NSIGHT 내부 업무 간 연계를 다루었다.

같은 WAR에서는 공개 Application Contract를 사용하고, 다른 업무 WAR에서는 TcfServiceClient와 ServiceId 기반 표준 전문을 사용하며, 데이터 변경은 반드시 소유 업무가 수행해야 한다는 원칙을 정리했다.

이번 장에서는 NSIGHT 경계 밖에 있는 시스템을 호출하는 방법을 다룬다.

외부 시스템의 예는 다음과 같다.

\`\`\`text id=“jtx001” 신용정보 시스템

고객정보·상품정보 원천 시스템

캠페인·메시지 발송 시스템

전자문서·보고서 시스템

사내 공통 인증·SSO 시스템

파일 송수신 시스템

외부 기관 API

레거시 기간계

전문 중계·EAI·ESB

클라우드 SaaS

문자·이메일·알림 발송 서비스



초보 개발자는 외부 호출을 다음처럼 구현하기 쉽다.

\`\`\`java id="jtx002"
String response =
restClient.post()
.uri(externalUrl)
.body(request)
.retrieve()
.body(String.class);

정상 상황에서는 동작할 수 있다.

그러나 운영 시스템에서는 다음 질문에 답해야 한다.

\`\`\`text id=“jtx003” 외부 시스템 호출이 반드시 동기 방식이어야 하는가?

외부 시스템이 느리면 사용자 거래는 얼마나 기다리는가?

연결 실패와 응답 Timeout을 구분하는가?

외부 시스템이 HTTP 200으로 업무 실패를 반환하면 어떻게 판단하는가?

HTTP 500이 발생하면 재시도해도 되는가?

등록 요청의 응답이 유실되면 외부 시스템에는 이미 반영된 것인가?

같은 요청을 다시 보내면 중복 등록되지 않는가?

Circuit Breaker가 열리면 어떤 응답을 반환하는가?

외부 시스템이 복구됐다는 사실을 어떻게 확인하는가?

Fallback 데이터가 오래됐거나 부정확할 수 있지 않은가?

로컬 DB를 변경한 뒤 외부 호출이 실패하면 어떻게 복구하는가?

외부 시스템에는 어떤 인증정보를 전달하는가?

인증서와 API Key는 어디에 저장하는가?

요청·응답 전문에 개인정보가 포함되지 않는가?

외부 시스템의 거래번호와 NSIGHT GUID를 어떻게 연결하는가?

외부 시스템의 오류코드를 어떤 내부 오류코드로 변환하는가?

운영자는 어느 팀에 연락해야 하는가?

외부 계약이 변경되면 어떤 거래가 영향을 받는가?



외부 시스템 호출의 핵심 위험은 \*\*외부 시스템의 장애가 내부 시스템의 장애로 전파되는 것\*\*이다.

\`\`\`text id="jtx004"
외부 시스템 응답 지연
↓
NSIGHT 호출 Thread 대기
↓
Tomcat Busy Thread 증가
↓
HTTP Connection Pool 고갈
↓
상위 업무 거래 Timeout
↓
사용자 재시도 증가
↓
외부 시스템에 추가 부하
↓
전체 장애 확대

이 상황에서 무조건 재시도하면 장애가 더 커질 수 있다.

text id="jtx005" 외부 시스템이 느리다. → 실패 요청을 3번씩 재시도한다. → 원래 부하의 4배가 전달된다.

반대로 모든 외부 실패를 즉시 시스템 오류로 처리하면 일시적인 네트워크 문제를 흡수하지 못한다.

따라서 외부 연계에는 다음 통제가 함께 필요하다.

\`\`\`text id=“jtx006” 명확한 계약

Client Adapter

계층별 Timeout

제한된 Retry

Circuit Breaker

Bulkhead

Rate Limit

Idempotency

상태조회

보상·Outbox

오류 변환

Trace·Metric

운영 Runbook



프로젝트 기준에서도 외부 연계 계약은 데이터 형식뿐 아니라 Timeout, 오류 의미, 재시도, 멱등성, Circuit Breaker와 복구조건을 포함해야 하며, 외부 장애가 내부 Thread와 Pool로 확산되지 않도록 설계해야 한다.

\---

\# 문서 개요

\## 목적

본 장의 목적은 NSIGHT TCF에서 외부 시스템 호출을 설계·개발·테스트·운영하기 위한 표준을 정의하는 것이다.

세부 목적은 다음과 같다.

\`\`\`text id="jtx007"
외부 시스템 경계와 책임 정의

동기·비동기·파일 연계 선택

외부 공개계약과 내부 모델 분리

External Client Adapter 표준화

Endpoint·인증·인증서·비밀정보 관리

Connect·Read·Write·Overall Timeout 설계

Retry 가능 오류와 금지 오류 구분

Circuit Breaker·Bulkhead·Rate Limit 적용

변경 요청 멱등성과 중복방지

Timeout 후 결과 불명 상태관리

오류코드와 사용자 메시지 변환

GUID·TraceId·외부 거래번호 연결

로컬 Transaction과 외부 변경 분리

Outbox·보상·재처리·대사 설계

성능·용량·Connection Pool 관리

개인정보·감사·침해사고 대응

계약 버전·호환성·폐기 관리

## 적용범위

| 적용 영역 | 적용 내용 |
| --- | --- |
| 동기 HTTP | REST·HTTP/JSON·전문 호출 |
| 동기 TCP | 전문 중계·Socket 기반 호출 |
| 비동기 메시지 | Queue·Topic·Event |
| 파일 연계 | 송수신·대형 파일·대사 |
| Callback | 외부 시스템의 결과 통보 |
| Batch 연계 | 대량 송수신·재처리 |
| 인증 | mTLS·OAuth2·HMAC·API Key |
| 복원력 | Timeout·Retry·Circuit·Bulkhead |
| 데이터 변경 | 멱등성·상태조회·보상 |
| 운영 | Metric·Trace·알림·Runbook |
| 변경관리 | 계약 Version·인증서·Endpoint 전환 |

## 대상 독자

\`\`\`text id=“jtx008” 업무 개발자

External Client·Adapter 개발자

애플리케이션 아키텍트

인터페이스·EAI 아키텍트

보안 아키텍트

인프라·네트워크 운영자

외부 연계 운영 담당자

QA·성능·장애 테스트 담당자

데이터 아키텍트·DBA

감사·내부통제 담당자

PMO·업무 분석가


\## 선행조건

\`\`\`text id="jtx009"
표준 Request·Response 이해

Service·Client 계층 이해

Transaction·Rollback 이해

Timeout Budget 이해

멱등성과 Idempotency Key 이해

GUID·TraceId 이해

오류코드·예외계층 이해

JWT·mTLS·Secret 관리 기본 이해

Batch·Outbox 기본 이해

# 핵심 관점

\`\`\`text id=“jtx010” 외부 시스템 호출의 성공은 HTTP 응답을 받는 것이 아니다.

상대 업무가 의도한 결과를 처리했고, 실패·Timeout·중복·재시도 상황에서도 내부 데이터의 상태를 설명할 수 있으며,

운영자가 양쪽 시스템의 거래를 하나의 흐름으로 추적할 수 있어야 외부 연계가 완료된 것이다.


\---

\# 학습 목표

이 장을 마치면 다음 내용을 설명하고 구현할 수 있어야 한다.

| 번호 | 학습 목표 |
|---:|---|
| 1 | 외부 시스템과 내부 업무 WAR 연계의 차이를 설명한다. |
| 2 | 동기·비동기·파일 방식을 선택한다. |
| 3 | 필수 의존과 선택 의존을 구분한다. |
| 4 | External Client Adapter의 책임을 설명한다. |
| 5 | Service가 HTTP·인증서 세부 구현을 알지 않게 한다. |
| 6 | 외부 Contract DTO와 내부 Domain 모델을 분리한다. |
| 7 | Endpoint·인증·Timeout 설정을 환경별로 관리한다. |
| 8 | 비밀정보를 코드와 설정파일에 저장하지 않는다. |
| 9 | Connect·Read·Write·Overall Timeout을 구분한다. |
| 10 | 전체 거래 Timeout보다 외부 호출 Timeout을 짧게 설정한다. |
| 11 | Remaining Timeout Budget을 계산한다. |
| 12 | 일시 오류와 영구 오류를 구분한다. |
| 13 | Retry 가능 요청과 Retry 금지 요청을 구분한다. |
| 14 | 지수 Backoff와 Jitter의 목적을 설명한다. |
| 15 | Retry Storm을 방지한다. |
| 16 | Circuit Breaker의 CLOSED·OPEN·HALF\_OPEN 상태를 설명한다. |
| 17 | Circuit Breaker와 Retry의 적용순서를 판단한다. |
| 18 | Bulkhead와 Connection Pool을 설계한다. |
| 19 | Fallback의 정확성과 데이터 시점을 검증한다. |
| 20 | 변경 요청에 Idempotency Key를 적용한다. |
| 21 | 외부 Request ID와 업무 키를 구분한다. |
| 22 | Timeout 후 외부 처리결과를 상태조회한다. |
| 23 | Callback 중복을 차단한다. |
| 24 | 로컬 DB와 외부 변경의 분산 일관성을 설명한다. |
| 25 | Outbox·보상·Saga를 적용할 상황을 판단한다. |
| 26 | 외부 오류를 내부 표준 오류로 변환한다. |
| 27 | 외부 원문 오류를 사용자에게 노출하지 않는다. |
| 28 | GUID와 외부 거래번호를 연결한다. |
| 29 | 요청·응답 전문의 개인정보를 마스킹한다. |
| 30 | 인증서 만료와 키 교체 절차를 운영한다. |
| 31 | 정상·지연·중복·부분 성공·장애 테스트를 작성한다. |
| 32 | 외부 계약의 버전·폐기 영향을 관리한다. |

\---

\# 핵심 용어

| 용어 | 의미 |
|---|---|
| External System | NSIGHT 관리·배포 경계 밖의 시스템 |
| External Client | 외부 시스템 호출을 추상화한 인터페이스 |
| Adapter | 외부 계약과 내부 모델을 변환하는 구현체 |
| Provider | 외부 기능을 제공하는 시스템 |
| Consumer | 외부 기능을 호출하는 NSIGHT 업무 |
| Synchronous | 호출자가 결과를 기다리는 방식 |
| Asynchronous | 결과를 즉시 기다리지 않는 방식 |
| Callback | 외부 시스템이 처리결과를 다시 호출하는 방식 |
| Connect Timeout | TCP 연결을 맺기까지 기다리는 시간 |
| TLS Timeout | TLS Handshake 완료까지의 제한 |
| Write Timeout | 요청 데이터를 전송하는 제한시간 |
| Read Timeout | 응답 데이터 대기시간 |
| Overall Timeout | 재시도를 포함한 전체 외부 호출 제한시간 |
| Retry | 일시적 실패 후 같은 요청을 다시 수행 |
| Backoff | 재시도 사이의 대기시간 |
| Jitter | 동시 재시도를 분산하기 위한 무작위 지연 |
| Circuit Breaker | 반복 실패 시 호출을 일시 차단하는 보호장치 |
| Bulkhead | 대상별 Thread·Connection을 격리하는 장치 |
| Rate Limiter | 단위시간당 요청 수를 제한하는 장치 |
| Idempotency Key | 동일 변경 요청을 식별하는 키 |
| External Request ID | 외부 시스템 거래를 식별하는 값 |
| Fallback | 외부 실패 시 사용하는 대체 처리 |
| DLQ | 재시도 초과 메시지를 보관하는 Queue |
| Compensation | 이미 성공한 변경을 반대 거래로 상쇄 |
| Reconciliation | 내부·외부 결과를 대조 |
| mTLS | Client와 Server가 상호 인증서를 검증하는 TLS |
| HMAC | 공유 Secret으로 요청 위변조를 검증하는 서명 |
| Secret Vault | Key·Token·비밀번호를 안전하게 보관하는 저장소 |

\---

\# 외부 호출의 목표 계층

\`\`\`text id="jtx011"
Handler
↓
Facade
↓
Service·Orchestrator
↓
External Client Interface
↓
External Client Adapter
├─ Contract 변환
├─ 인증
├─ Header 생성
├─ Timeout
├─ Retry
├─ Circuit Breaker
├─ 오류 변환
├─ Trace
└─ 전문 마스킹
↓
Transport Client
├─ RestClient
├─ WebClient
├─ TCP Client
├─ Message Producer
└─ File Transfer
↓
외부 시스템

금지:

\`\`\`text id=“jtx012” Handler → RestClient

Rule → 외부 API

DAO → 외부 API

Mapper → HTTP 호출

Controller → 외부 API 직접 호출


\---

\# 현재 구현과 목표 구조

\## 현재 \`tcf-eai\`에서 확인되는 구조

현재 기준 소스의 \`tcf-eai\`는 다음 기능을 제공한다.

\`\`\`text id="jtx013"
TcfServiceClient

DefaultTcfServiceClient

TcfIntegrationProperties

IntegrationCallRequest

IntegrationCallResult

StandardRequestBuilder

HeaderPropagationHelper

ResponseResultValidator

IntegrationBusinessException

IntegrationTimeoutException

IntegrationException

기본 호출 방식:

text id="jtx014" 업무 Client Adapter ↓ TcfServiceClient ↓ RestClient POST ↓ 대상 /{context}/online ↓ 표준 Response 검증

주요 구현 기능:

| 기능 | 상태 |
| --- | --- |
| Spring RestClient | 구현 확인 |
| 업무별 Endpoint 설정 | 구현 확인 |
| Connect Timeout | 구현 확인 |
| Read Timeout | 구현 확인 |
| GUID·TraceId 전파 | 구현 확인 |
| 호출자 ServiceId 전파 | 구현 확인 |
| S0000 성공 판정 | 구현 확인 |
| Timeout 예외 분리 | 구현 확인 |
| 업무 오류 예외 분리 | 구현 확인 |
| 응답 전문 Parsing 오류 | 구현 확인 |
| 업무별 Client Adapter | 샘플 구현 확인 |

## 현재 구조의 해석

현재 tcf-eai는 **NSIGHT 내부 업무 WAR 간 표준 HTTP 호출의 기초 구현**에 가깝다.

외부기관·외부 솔루션마다 다른 다음 계약을 모두 지원하는 범용 외부연계 플랫폼으로 보기는 어렵다.

\`\`\`text id=“jtx015” OAuth2 Client Credentials

mTLS

HMAC 서명

API Key

외부 전문 암호화

특수 HTTP Header

기관별 오류코드

Callback

상태조회

Circuit Breaker

Retry

Rate Limit

비동기 Queue

파일 연계

인증서 Rotation



따라서 목표 구조는 다음과 같이 분리해야 한다.

\`\`\`text id="jtx016"
tcf-eai 공통 정책
├─ Timeout
├─ Trace
├─ Error Model
├─ Retry·Circuit 공통
├─ Secret 참조
└─ Metric

업무별 External Adapter
├─ 외부 Contract
├─ 인증
├─ Request·Response 변환
├─ 외부 오류 매핑
├─ 상태조회
└─ 보상

## 현재 구현의 주요 Gap

| 항목 | 현재 상태 | 목표 판단 |
| --- | --- | --- |
| Retry | 공통 구현 확인 안 됨 | 오류·멱등성별 정책 필요 |
| Circuit Breaker | 구현 확인 안 됨 | 외부 대상별 적용 필요 |
| Bulkhead | 구현 확인 안 됨 | 대상별 Thread·Connection 격리 |
| Rate Limiter | 구현 확인 안 됨 | 상대 한도 대응 |
| Overall Timeout | Connect·Read 중심 | 재시도 포함 전체 예산 필요 |
| Remaining Budget | 확인 안 됨 | TCF 잔여시간 반영 |
| 인증 추상화 | 확인 안 됨 | OAuth·mTLS·HMAC 지원 |
| Secret 관리 | 설정 외 별도 확인 필요 | Vault 연계 |
| Connection Pool | 명시적 정책 확인 필요 | 대상별 최대 연결 관리 |
| External Request ID | 공통 구조 확인 필요 | 저장·Trace 연결 |
| 상태조회 | 공통 구조 없음 | 변경 연계 필수 검토 |
| Callback | 공통 구조 없음 | 서명·중복·순서 관리 |
| DLQ | 공통 구조 없음 | 비동기 실패 보관 |
| Contract Version | 명시적 검증 부족 | Header·Schema 필요 |
| 응답 필드 | 표준 필드 정합성 점검 | resultMessage/errorMessage 일치 필요 |
| 외부 오류 메시지 | 예외 메시지 포함 가능 | 사용자 노출 금지 |
| Timeout 분류 | 일부 Exception Type 중심 | Connect·TLS·Read 세분화 |
| 동적 설정 | Client Cache 사용 | Timeout 변경 시 재생성 검토 |
| Metric | 기본 호출 로그 중심 | 오류율·Circuit·Pool Metric 필요 |

현재 설계자료에서도 tcf-eai 기본 구현에는 Retry가 없으며, 적용 시 동일 GUID 유지, 제한 횟수, 지수 Backoff와 전체 Timeout을 함께 고려하도록 규정한다.

# 24.1 동기·비동기 방식 선택

## 24.1.1 연계 방식은 기술보다 업무 완료조건으로 선택한다

다음 질문으로 시작한다.

\`\`\`text id=“jtx017” 사용자가 외부 처리결과를 즉시 알아야 하는가?

외부 결과가 없으면 현재 업무를 완료할 수 없는가?

몇 초까지 기다릴 수 있는가?

외부 시스템이 중단돼도 내부 업무를 접수할 수 있는가?

처리 순서가 중요한가?

대량 데이터인가?

결과를 나중에 조회하거나 Callback으로 받을 수 있는가?

부분 성공을 허용할 수 있는가?


\---

\## 24.1.2 동기 호출

\`\`\`text id="jtx018"
NSIGHT 요청
↓
외부 시스템 호출
↓
외부 응답 대기
↓
NSIGHT 응답

적합:

\`\`\`text id=“jtx019” 즉시 조회결과가 필요하다.

외부 결과로 다음 업무 Rule을 판단한다.

처리시간이 짧고 안정적이다.

응답 없이는 사용자가 다음 단계로 진행할 수 없다.



예:

\`\`\`text id="jtx020"
실시간 고객 자격 조회

상품 유효성 검증

외부 코드 검증

짧은 상태조회

실시간 한도 조회

주의:

\`\`\`text id=“jtx021” 사용자 응답시간이 외부 SLA에 종속된다.

외부 장애가 내부 Thread로 전파된다.

재시도 시 응답시간이 증가한다.

변경 호출은 결과 불명 상태가 생긴다.


\---

\## 24.1.3 비동기 호출

\`\`\`text id="jtx022"
NSIGHT 요청
↓
내부 DB 상태·Outbox 저장
↓
사용자에게 접수 응답
↓
메시지 발행
↓
외부 처리
↓
Callback·상태조회
↓
최종 상태 갱신

적합:

\`\`\`text id=“jtx023” 처리가 수 초 이상 걸린다.

외부 시스템의 가용성이 낮다.

대량 요청이다.

사용자가 즉시 최종 결과를 알 필요가 없다.

재처리와 순서 보장이 필요하다.

내부 DB Lock을 오래 유지하면 안 된다.



예:

\`\`\`text id="jtx024"
캠페인 발송 요청

대량 파일 생성

고객 데이터 송신

보고서 생성

외부 심사 요청

Batch 적재

## 24.1.4 비동기 결과 상태

\`\`\`text id=“jtx025” RECEIVED

QUEUED

SENT

ACCEPTED

PROCESSING

SUCCESS

BUSINESS\_FAIL

SYSTEM\_FAIL

TIMEOUT

UNKNOWN

RETRY\_WAIT

COMPENSATING

COMPENSATED



사용자에게 “요청이 접수됐다”와 “외부 처리가 완료됐다”를 구분해 보여 준다.

\---

\## 24.1.5 동기 접수 + 비동기 완료

외부 시스템이 요청 접수만 동기로 반환하는 구조가 있다.

\`\`\`text id="jtx026"
NSIGHT
→ 외부 요청

외부 응답
→ 접수번호 EXT-001
→ 처리상태 ACCEPTED

나중에
→ Callback 또는 상태조회

이 경우 동기 HTTP 200은 최종 업무 성공이 아니다.

\`\`\`text id=“jtx027” HTTP 성공 = 요청 접수 성공

업무 완료 = 외부 최종 상태 SUCCESS


\---

\## 24.1.6 Callback

\`\`\`text id="jtx028"
외부 시스템
↓
NSIGHT Callback Endpoint
↓
서명·인증 검증
↓
외부 Request ID 조회
↓
중복 Callback 확인
↓
상태 전이
↓
감사·이벤트

Callback 설계항목:

\`\`\`text id=“jtx029” Callback URL

인증방식

외부 거래번호

Callback Event ID

Timestamp

서명

중복방지

순서

재전송

응답코드

최대 Payload

감사


\---

\## 24.1.7 파일 연계

파일이 적합한 경우:

\`\`\`text id="jtx030"
수십만·수백만 건

대형 Binary

업무시간 외 처리

건수·금액 대사 필요

온라인 API 크기 제한 초과

필수 항목:

\`\`\`text id=“jtx031” 파일명 표준

파일 ID

업무일자

Sequence

크기

Hash

암호화

압축

전송 완료 표시

재전송

부분 파일

대사

보존·파기


\---

\## 24.1.8 방식 비교

| 기준 | 동기 API | 비동기 메시지 | Callback | 파일 |
|---|---|---|---|---|
| 즉시 결과 | O | X | 나중 | 나중 |
| 처리량 | 중간 | 높음 | 중간 | 매우 높음 |
| 장애 격리 | 낮음 | 높음 | 중간 | 높음 |
| 구현 복잡도 | 낮음 | 중·높음 | 높음 | 높음 |
| 재처리 | 제한 | 용이 | 필요 | Checkpoint |
| 순서 | 호출순서 | Partition 필요 | Event 기준 | 파일순서 |
| 대사 | 선택 | 필요 | 필요 | 필수 |
| 중복 | Retry 위험 | 소비 중복 | Callback 중복 | 재전송 중복 |
| 사용자 대기 | O | X | X | X |
| 적합 대상 | 실시간 조회 | 후속 처리 | 외부 장기 처리 | 대량 데이터 |

\---

\## 24.1.9 필수 의존과 선택 의존

\### 필수 의존

\`\`\`text id="jtx032"
외부 신용조회 결과가 없으면
대출 추천 결과를 만들 수 없다.

외부 실패:

text id="jtx033" 상위 거래 실패

### 선택 의존

text id="jtx034" 외부 부가상품 추천은 고객 기본조회에 필수가 아니다.

외부 실패:

\`\`\`text id=“jtx035” 기본 결과 반환

부가영역 unavailable

Warning 기록


\---

\## 24.1.10 Fallback

Fallback 종류:

\`\`\`text id="jtx036"
Cache된 이전 데이터

내부 기본값

기능 축소

대체 외부 시스템

접수 후 비동기 전환

사용자 재시도 안내

Fallback 사용 조건:

\`\`\`text id=“jtx037” 데이터의 기준시각을 표시한다.

업무적으로 오래된 값을 허용한다.

개인정보·권한이 유효하다.

중요 의사결정에 잘못 사용되지 않는다.

Fallback 사용 사실을 로그·응답에 표시한다.



금지:

\`\`\`text id="jtx038"
외부 잔액조회 실패
→ 어제 잔액을 현재 잔액처럼 반환

## 24.1.11 Adapter 계층

\`\`\`java id=“jtx039” public interface CreditInformationClient {

CreditInquiryResult inquire(
CreditInquiryCommand command,
ExternalCallContext context
);

}



외부 구현:

\`\`\`java id="jtx040"
@Component
@RequiredArgsConstructor
class CreditInformationHttpAdapter
implements CreditInformationClient {

private final CreditInformationTransport transport;
private final CreditContractMapper mapper;
private final ExternalCallPolicy policy;

@Override
public CreditInquiryResult inquire(
CreditInquiryCommand command,
ExternalCallContext context) {

ExternalCreditRequest request =
mapper.toExternalRequest(
command,
context
);

ExternalCreditResponse response =
transport.call(request, context);

return mapper.toDomainResult(response);
}
}

Service는 URL·OAuth Token·JSON 필드명을 알지 않는다.

## 24.1.12 Contract DTO와 내부 모델

외부 계약:

java id="jtx041" public record ExternalCreditRequest( String trxNo, String requestDate, String customerIdentifier, String inquiryPurposeCode ) {}

내부 모델:

java id="jtx042" public record CreditInquiryCommand( CustomerNo customerNo, InquiryPurpose purpose ) {}

변환 이유:

\`\`\`text id=“jtx043” 외부 필드명 변경 격리

외부 코드값 변환

날짜·금액 단위 변환

마스킹

계약 Version 대응

테스트 용이성


\---

\## 24.1.13 외부 연계 ID

권장:

\`\`\`text id="jtx044"
{소비업무}-{외부시스템}-{방식}-{4자리}

예:

\`\`\`text id=“jtx045” SV-CREDIT-SYNC-0001

CM-MESSAGE-ASYNC-0001

MG-REPORT-FILE-0001



관리대장:

| 항목 | 예 |
|---|---|
| Interface ID | \`SV-CREDIT-SYNC-0001\` |
| 소비 업무 | SV |
| 대상 시스템 | CREDIT-HUB |
| 목적 | 신용요약 조회 |
| 방식 | 동기 HTTP |
| 계약 버전 | V1 |
| 인증 | mTLS + OAuth2 |
| 전체 Timeout | 1,200ms |
| Retry | 503·Connect Reset 1회 |
| Circuit | 실패율 50% |
| 멱등성 | 조회 |
| Fallback | 없음 |
| 개인정보 | 고유식별정보 |
| 담당자 | SV·연계운영·상대팀 |
| SLA | p95 700ms |
| 폐기일 | 미정 |

\---

\# 24.2 Timeout·재시도·Circuit Breaker

\## 24.2.1 Timeout 종류

| Timeout | 의미 |
|---|---|
| DNS Timeout | 호스트명 해석 제한 |
| Connection Pool Acquire | Client Pool에서 Connection 대기 |
| Connect Timeout | TCP 연결 수립 제한 |
| TLS Handshake Timeout | 인증서·암호협상 제한 |
| Write Timeout | 요청 전문 전송 제한 |
| Read Timeout | 응답 데이터 대기 제한 |
| Idle Timeout | 연결 유휴 제한 |
| Overall Timeout | Retry를 포함한 전체 호출 제한 |
| TCF Online Timeout | 사용자 거래 전체 제한 |
| Transaction Timeout | 로컬 DB Transaction 제한 |

\---

\## 24.2.2 Timeout 계층

권장 방향:

\`\`\`text id="jtx046"
사용자·채널 Timeout
\>
Gateway Timeout
\>
TCF Online Timeout
\>
외부 Overall Timeout
\>
외부 Read Timeout
\>
DB Query Timeout

안쪽 Timeout이 먼저 발생해야 어느 계층이 실패했는지 구분하기 쉽다.

프로젝트 Architecture Gate도 Gateway·TCF·HTTP·DB의 Timeout 순서가 합리적이어야 한다고 규정한다.

## 24.2.3 Timeout Budget 예

전체 사용자 응답 목표:

text id="jtx047" 3,000ms

예산:

| 처리 구간 | 예산 |
| --- | --- |
| Gateway·STF | 200ms |
| 자체 DB 조회 | 900ms |
| 외부 호출 전체 | 1,200ms |
| 결과 조립·ETF | 300ms |
| 안전 여유 | 400ms |
| 합계 | 3,000ms |

외부 호출:

\`\`\`text id=“jtx048” Pool Acquire 100ms

Connect 250ms

TLS 200ms

Write 100ms

Read 500ms

Retry 여유 50ms



실제 설정은 대상 SLA와 측정결과로 확정한다.

\---

\## 24.2.4 잘못된 Timeout

\`\`\`text id="jtx049"
TCF Online Timeout
3초

외부 Read Timeout
5초

Retry
2회

최악:

text id="jtx050" 외부 5초 + Backoff + 외부 5초 + Backoff + 외부 5초

상위 거래가 먼저 Timeout돼도 하위 호출이 계속 실행될 수 있다.

## 24.2.5 Remaining Timeout

\`\`\`java id=“jtx051” long remainingMs = context.deadlineEpochMs() - clock.millis();

long externalTimeoutMs = Math.min( policy.maxOverallTimeoutMs(), remainingMs - policy.safetyMarginMs() );

if (externalTimeoutMs <= 0) { throw new ExternalTimeoutBudgetExceededException(); }



호출 시점마다 잔여시간을 다시 계산한다.

\---

\## 24.2.6 Timeout 오류 분류

\`\`\`text id="jtx052"
Pool Acquire Timeout

Connect Timeout

TLS Timeout

Write Timeout

Read Timeout

Overall Timeout

TCF Timeout

를 하나의 “연계 Timeout”으로만 기록하지 않는다.

운영 로그:

\`\`\`text id=“jtx053” timeoutStage=CONNECT

configuredTimeoutMs=300

elapsedMs=304

targetSystem=CREDIT-HUB


\---

\## 24.2.7 Retry의 목적

Retry는 다음과 같은 짧은 일시 오류를 흡수하기 위한 것이다.

\`\`\`text id="jtx054"
Connection Reset

일시 DNS 실패

HTTP 502·503·504

상대 Rate Limit의 짧은 Retry-After

순간 Network 단절

일시 Lock·Busy 응답

Retry는 오류를 해결하는 기능이 아니다.

\`\`\`text id=“jtx055” SQL 문법 오류

잘못된 요청

권한 없음

계약 Version 오류

업무 거절

인증서 만료



를 반복해도 성공하지 않는다.

\---

\## 24.2.8 Retry 판단표

| 오류 | 조회 | 변경 |
|---|:---:|:---:|
| Connect 실패 | 제한적 가능 | 멱등성 있을 때 검토 |
| Read Timeout | 제한적 | 결과 불명확으로 기본 금지 |
| HTTP 502 | 제한적 | 멱등성 필수 |
| HTTP 503 | 제한적 | 상대 계약에 따라 |
| HTTP 429 | \`Retry-After\` 준수 | 멱등성·전체예산 |
| HTTP 400 | 금지 | 금지 |
| HTTP 401 | Token 재발급 1회 검토 | 중복 실행 주의 |
| HTTP 403 | 금지 | 금지 |
| 업무 오류 | 금지 | 금지 |
| Parsing 오류 | 금지 | 금지 |
| 인증서 오류 | 금지 | 금지 |
| Circuit Open | 금지 | 금지 |

\---

\## 24.2.9 변경 Retry의 위험

\`\`\`text id="jtx056"
NSIGHT
→ 외부 등록 요청

외부
→ 등록 Commit

응답
→ Network 유실

NSIGHT
→ Read Timeout

다시 전송하면:

text id="jtx057" 등록 2건

이 될 수 있다.

따라서 변경 Retry에는 다음 조건이 필요하다.

\`\`\`text id=“jtx058” 동일 Idempotency Key

외부 시스템의 멱등성 보장

상태조회 API

외부 업무 키

기존 성공응답 재반환

Timeout 상태 UNKNOWN 처리


\---

\## 24.2.10 Retry 횟수

기본 예:

\`\`\`text id="jtx059"
최초 호출
\+ 최대 1회 Retry

3회 이상의 Retry는 예외 승인 대상으로 볼 수 있다.

횟수는 다음을 고려한다.

\`\`\`text id=“jtx060” 전체 Timeout

상대 SLA

요청량

오류 지속시간

상대 Rate Limit

멱등성

사용자 응답 목표


\---

\## 24.2.11 Backoff

고정 Backoff:

\`\`\`text id="jtx061"
100ms
100ms
100ms

지수 Backoff:

text id="jtx062" 100ms 200ms 400ms

Jitter:

\`\`\`text id=“jtx063” 100ms ± 무작위값

200ms ± 무작위값



Jitter는 여러 인스턴스가 동시에 재시도하는 현상을 줄인다.

\---

\## 24.2.12 Retry Storm

\`\`\`text id="jtx064"
외부 장애
↓
모든 NSIGHT 인스턴스 실패
↓
각 요청 3회 Retry
↓
사용자도 재클릭
↓
Batch도 재처리
↓
외부 부하 폭증

방어:

\`\`\`text id=“jtx065” Retry 상한

Jitter

Circuit Breaker

Rate Limit

Bulkhead

거래통제

사용자 중복요청 방지


\---

\## 24.2.13 Circuit Breaker 상태

\`\`\`text id="jtx066"
CLOSED
→ 정상 호출

실패율·Slow Call 임계치 초과
→ OPEN

대기시간 경과
→ HALF\_OPEN

시험 호출 성공
→ CLOSED

시험 호출 실패
→ OPEN

## 24.2.14 Circuit 기준

설정 예:

\`\`\`text id=“jtx067” Sliding Window 100건

최소 호출 수 20건

실패율 50%

Slow Call 기준 800ms

Slow Call 비율 50%

Open 유지 30초

Half-Open 시험 5건



값은 대상 시스템의 정상 지연분포와 SLA를 기준으로 확정한다.

\---

\## 24.2.15 Circuit 실패로 집계할 오류

집계 후보:

\`\`\`text id="jtx068"
Connect 실패

Read Timeout

HTTP 5xx

계약 Parsing 오류

상대 시스템 장애코드

제외 후보:

\`\`\`text id=“jtx069” 사용자 입력 오류

업무 거절

권한 없음

데이터 없음

NSIGHT 내부 Validation



업무 오류를 Circuit 실패로 집계하면 정상 업무 거절 때문에 Circuit이 열릴 수 있다.

\---

\## 24.2.16 Circuit Open 응답

필수 의존:

\`\`\`text id="jtx070"
외부 시스템을 현재 이용할 수 없습니다.
잠시 후 다시 시도해 주세요.

선택 의존:

text id="jtx071" 외부 부가정보를 제외하고 기본 결과 반환

변경 요청:

text id="jtx072" 외부 호출을 실행하지 않았으므로 접수 실패 또는 Queue 전환

Circuit Open을 일반 Read Timeout처럼 Logging하지 않는다.

## 24.2.17 Half-Open

모든 요청을 시험 호출로 사용하지 않는다.

\`\`\`text id=“jtx073” 제한된 3~5건만 허용

나머지는 빠른 실패

성공률 충족 후 Close



복구 직후 전체 트래픽이 한 번에 몰리지 않도록 한다.

\---

\## 24.2.18 Retry와 Circuit 적용 순서

일반적인 개념:

\`\`\`text id="jtx074"
호출
↓
Circuit 허용 확인
↓
Retry 정책 실행
↓
각 실패를 Circuit에 기록

라이브러리 Decorator 순서에 따라 Metric과 동작이 달라질 수 있으므로 테스트로 검증한다.

잘못된 구성:

text id="jtx075" Retry 한 요청의 3번 실패를 Circuit에서 3개의 독립 사용자 요청처럼 집계

정책상 허용 여부를 명확히 한다.

## 24.2.19 Bulkhead

외부 대상별 자원을 격리한다.

\`\`\`text id=“jtx076” CREDIT-HUB Thread 최대 30 Queue 50

MESSAGE-HUB Thread 최대 20 Queue 100



하나의 외부 장애가 모든 External Client Thread를 점유하지 않게 한다.

\---

\## 24.2.20 Connection Pool

관리항목:

\`\`\`text id="jtx077"
대상별 최대 Connection

대상 Host당 Connection

Connection Acquire Timeout

Keep-Alive

Idle Eviction

DNS 갱신

TLS Session

응답 Body 완전 소비

Connection Leak

무제한 새 Connection 생성은 Port 고갈과 상대 시스템 부하를 유발할 수 있다.

## 24.2.21 Rate Limiter

상대 계약:

\`\`\`text id=“jtx078” 초당 100건

분당 3,000건



NSIGHT 내부 Fan-Out을 포함해 제한한다.

\`\`\`text id="jtx079"
사용자 거래 50 TPS
× 거래당 외부 호출 3회
\= 150 TPS

## 24.2.22 Timeout·Retry·Circuit 표준 설정 예

\`\`\`yaml id=“jtx080” nsight: external: systems: CREDIT-HUB: base-url: https://credit.internal connect-timeout-ms: 300 read-timeout-ms: 700 overall-timeout-ms: 1200

retry:
enabled: true
max-attempts: 2
initial-backoff-ms: 100
multiplier: 2.0
jitter: 0.2
retryable-statuses:
\- 502
\- 503
\- 504

circuit-breaker:
enabled: true
minimum-number-of-calls: 20
sliding-window-size: 100
failure-rate-threshold: 50
slow-call-duration-ms: 800
slow-call-rate-threshold: 50
open-state-wait-ms: 30000
half-open-permitted-calls: 5

bulkhead:
max-concurrent-calls: 30
max-wait-ms: 50



운영값은 OM·Vault·환경설정의 공식 원천에서 관리한다.

\---

\# 24.3 멱등성과 중복 처리

\## 24.3.1 외부 변경의 중복 원인

\`\`\`text id="jtx081"
사용자 중복 클릭

Gateway 재전송

NSIGHT Retry

외부 Proxy Retry

Network 응답 유실

Batch 재시작

Callback 재전송

운영자 수동 재처리

Message Consumer 재전달

화면 버튼 비활성화만으로 차단할 수 없다.

## 24.3.2 업무 키와 Idempotency Key

업무 키:

text id="jtx082" 동일 업무 데이터인가?

예:

text id="jtx083" campaignId + recipientId + messageType

Idempotency Key:

text id="jtx084" 동일 요청의 재전송인가?

예:

text id="jtx085" IDEMP-CM-MSG-20260718-0001

외부 Request ID:

text id="jtx086" 외부 시스템이 부여한 거래번호 EXT-TRX-98123

세 값을 구분한다.

## 24.3.3 외부 변경 요청

json id="jtx087" { "requestId": "NSIGHT-REQ-20260718-0001", "idempotencyKey": "IDEMP-CM-MSG-20260718-0001", "campaignId": "CMP-20260718-000001", "recipientId": "C000001", "messageTemplateId": "TMPL-001" }

외부 시스템이 Idempotency Header를 지원한다면:

http id="jtx088" Idempotency-Key: IDEMP-CM-MSG-20260718-0001 X-Correlation-Id: G-20260718-000701

## 24.3.4 외부 시스템이 멱등성을 지원하는 경우

\`\`\`text id=“jtx089” 첫 요청 → 외부 처리 → SUCCESS

같은 Key 재요청 → 신규 처리 없음 → 기존 결과 반환



계약으로 확인할 항목:

\`\`\`text id="jtx090"
Key 범위

Key 최대 길이

보관기간

같은 Key 다른 Body 처리

SUCCESS 응답 재반환

PROCESSING 응답

FAIL 후 재시도

Timeout 상태조회

## 24.3.5 외부 시스템이 멱등성을 지원하지 않는 경우

대안:

\`\`\`text id=“jtx091” NSIGHT Outbox에서 한 번만 발행

외부 업무 키 Unique 활용

사전 상태조회

내부 전송 Ledger

외부 요청번호 고정

Callback 중복 차단

운영 대사



그러나 네트워크 Timeout 이후 외부 반영 여부를 완벽히 알 수 없다면 자동 Retry를 금지해야 한다.

\---

\## 24.3.6 External Call Ledger

\`\`\`sql id="jtx092"
CREATE TABLE TCF\_EXTERNAL\_CALL (
EXTERNAL\_CALL\_ID VARCHAR2(50) NOT NULL,
INTERFACE\_ID VARCHAR2(50) NOT NULL,
GUID VARCHAR2(100) NOT NULL,
TRACE\_ID VARCHAR2(100),
IDEMPOTENCY\_KEY VARCHAR2(200),
REQUEST\_ID VARCHAR2(100) NOT NULL,
EXTERNAL\_TRANSACTION\_ID VARCHAR2(100),
BUSINESS\_KEY VARCHAR2(300),
REQUEST\_HASH VARCHAR2(128),
CALL\_STATUS VARCHAR2(30) NOT NULL,
ATTEMPT\_COUNT NUMBER(5) NOT NULL,
RETRYABLE\_YN CHAR(1) NOT NULL,
LAST\_ERROR\_CODE VARCHAR2(50),
NEXT\_RETRY\_AT TIMESTAMP,
REQUESTED\_AT TIMESTAMP NOT NULL,
COMPLETED\_AT TIMESTAMP,
UPDATED\_AT TIMESTAMP NOT NULL,
CONSTRAINT PK\_TCF\_EXTERNAL\_CALL
PRIMARY KEY (EXTERNAL\_CALL\_ID)
);

Unique:

sql id="jtx093" CREATE UNIQUE INDEX UX\_TCF\_EXTERNAL\_CALL\_01 ON TCF\_EXTERNAL\_CALL ( INTERFACE\_ID, IDEMPOTENCY\_KEY );

## 24.3.7 외부 호출 상태

\`\`\`text id=“jtx094” READY

SENDING

ACCEPTED

PROCESSING

SUCCESS

BUSINESS\_FAIL

SYSTEM\_FAIL

TIMEOUT

UNKNOWN

RETRY\_WAIT

CANCEL\_REQUESTED

CANCELLED

COMPENSATION\_REQUIRED

COMPENSATING

COMPENSATED


\---

\## 24.3.8 변경 Timeout

\`\`\`text id="jtx095"
요청 전송 완료
↓
외부 처리
↓
응답 대기 중 Read Timeout

결과는 다음 중 하나다.

\`\`\`text id=“jtx096” 외부 미수신

외부 수신·미처리

외부 처리 중

외부 성공

외부 실패

응답만 유실



따라서:

\`\`\`text id="jtx097"
TIMEOUT
→ UNKNOWN 가능
→ 상태조회
→ 대사
→ 확정 후 재처리

## 24.3.9 상태조회

\`\`\`text id=“jtx098” NSIGHT Request ID

외부 Transaction ID

업무 키

Idempotency Key



중 승인된 값을 이용한다.

상태조회 결과:

\`\`\`text id="jtx099"
NOT\_FOUND

RECEIVED

PROCESSING

SUCCESS

FAIL

CANCELLED

UNKNOWN

NOT\_FOUND가 반드시 미처리를 의미하는지도 외부 계약으로 확인해야 한다.

외부 시스템의 조회 DB가 지연될 수 있기 때문이다.

## 24.3.10 Timeout 후 처리 예

text id="jtx100" 1. 변경 요청 Read Timeout 2. External Call 상태 UNKNOWN 3. 신규 Key 자동 Retry 금지 4. 같은 Request ID로 상태조회 5. 외부 SUCCESS 확인 6. 내부 상태 SUCCESS 보정 7. 사용자·운영자에게 결과 제공

## 24.3.11 Callback 중복

외부 시스템은 응답을 받지 못하면 Callback을 반복할 수 있다.

text id="jtx101" Callback Event ID EXT-EVT-001

내부 중복 저장:

sql id="jtx102" CREATE UNIQUE INDEX UX\_EXTERNAL\_CALLBACK\_01 ON TCF\_EXTERNAL\_CALLBACK ( EXTERNAL\_SYSTEM\_ID, CALLBACK\_EVENT\_ID );

## 24.3.12 Callback 순서

\`\`\`text id=“jtx103” 이벤트 1 PROCESSING

이벤트 2 SUCCESS



Network로 인해 순서가 바뀔 수 있다.

\`\`\`text id="jtx104"
SUCCESS 먼저 수신

PROCESSING 나중 수신

상태 전이 규칙:

text id="jtx105" SUCCESS → PROCESSING으로 역행 금지

외부 Event Sequence·Version·OccurredAt을 검증한다.

## 24.3.13 메시지 중복

Queue는 일반적으로 At-Least-Once 전달일 수 있다.

text id="jtx106" 메시지 한 건 → Consumer에 여러 번 전달 가능

Consumer:

\`\`\`text id=“jtx107” eventId Unique 확인

업무 키 확인

처리이력 확인

동일 결과 반환


\---

\## 24.3.14 로컬 DB와 외부 변경

위험:

\`\`\`text id="jtx108"
로컬 DB UPDATE 성공

외부 변경 성공

로컬 후속 SQL 실패

로컬 Rollback

외부 변경은 자동 Rollback되지 않는다.

## 24.3.15 분산 일관성 대안

### Outbox

text id="jtx109" 로컬 업무 변경 + Outbox INSERT ↓ Commit ↓ 외부 전송

### Saga

\`\`\`text id=“jtx110” 단계 1 성공

단계 2 성공

단계 3 실패

단계 2 보상

단계 1 보상


\### 상태머신

\`\`\`text id="jtx111"
REQUESTED
→ EXTERNAL\_PROCESSING
→ COMPLETED

또는
→ COMPENSATION\_REQUIRED

### 대사

text id="jtx112" 내부 성공목록 ↔ 외부 성공목록

## 24.3.16 Outbox 구조

sql id="jtx113" CREATE TABLE TCF\_EXTERNAL\_OUTBOX ( OUTBOX\_ID VARCHAR2(50) NOT NULL, INTERFACE\_ID VARCHAR2(50) NOT NULL, EVENT\_TYPE VARCHAR2(100) NOT NULL, AGGREGATE\_ID VARCHAR2(100) NOT NULL, IDEMPOTENCY\_KEY VARCHAR2(200) NOT NULL, PAYLOAD CLOB NOT NULL, OUTBOX\_STATUS VARCHAR2(20) NOT NULL, ATTEMPT\_COUNT NUMBER(5) NOT NULL, NEXT\_ATTEMPT\_AT TIMESTAMP, CREATED\_AT TIMESTAMP NOT NULL, PUBLISHED\_AT TIMESTAMP, CONSTRAINT PK\_TCF\_EXTERNAL\_OUTBOX PRIMARY KEY (OUTBOX\_ID) );

업무 Table 변경과 Outbox INSERT를 같은 Transaction으로 처리한다.

## 24.3.17 보상

보상은 DB Rollback이 아니다.

\`\`\`text id=“jtx114” 발송 요청 성공 → 발송 취소 요청

등록 성공 → 취소·무효화 요청

예약 확정 → 예약 취소



보상 설계항목:

\`\`\`text id="jtx115"
보상 Service·Interface

보상 가능 상태

보상 기한

보상 Idempotency

보상 실패

수동조치

감사

대사

## 24.3.18 대사

건수:

text id="jtx116" NSIGHT 요청 건수 = 외부 성공 + 외부 실패 + 처리 중 + 미확인

금액:

text id="jtx117" NSIGHT 요청 금액 ↔ 외부 처리 금액

식별:

text id="jtx118" NSIGHT Request ID ↔ 외부 Transaction ID

대사 차이는 운영 경보와 재처리 대상이다.

# 24.4 오류 변환과 추적 ID

## 24.4.1 외부 오류 분류

\`\`\`text id=“jtx119” REQUEST\_VALIDATION

AUTHENTICATION

AUTHORIZATION

BUSINESS\_REJECT

NOT\_FOUND

DUPLICATE

RATE\_LIMIT

CONNECT\_TIMEOUT

READ\_TIMEOUT

CONNECTION\_ERROR

TLS\_ERROR

HTTP\_4XX

HTTP\_5XX

CONTRACT\_ERROR

PARSING\_ERROR

CIRCUIT\_OPEN

BULKHEAD\_FULL

UNKNOWN


\---

\## 24.4.2 외부 업무 오류와 기술 오류

외부 업무 오류:

\`\`\`text id="jtx120"
고객 없음

한도 부족

이미 처리됨

현재 상태에서 불가

요청일자 오류

외부 기술 오류:

\`\`\`text id=“jtx121” Connection Refused

TLS 인증서 실패

Read Timeout

HTTP 500

JSON Parsing 실패

필수 응답필드 없음



두 유형은 사용자 행동과 운영 대응이 다르다.

\---

\## 24.4.3 오류 변환 계층

\`\`\`text id="jtx122"
Transport
→ HTTP·Socket·TLS 예외

External Adapter
→ 외부 계약과 오류코드 해석

Service
→ 업무 의미에 따라 상위 오류 결정

TCF·ETF
→ StandardResponse

Transport가 사용자 메시지를 결정하지 않는다.

## 24.4.4 외부 오류 매핑표

| 외부 조건 | 내부 오류 | Retry | 사용자 처리 |
| --- | --- | --- | --- |
| 고객 없음 | E-SV-EXT-0001 | X | 대상 확인 |
| 업무상 거절 | E-SV-EXT-0002 | X | 조건 확인 |
| 중복 요청 | E-SV-EXT-0003 | X | 기존 결과 확인 |
| HTTP 429 | E-TCF-EXT-0004 | 제한 | 잠시 후 |
| Connect Timeout | E-TCF-TMO-0005 | 제한 | 시스템 안내 |
| Read Timeout 조회 | E-TCF-TMO-0006 | 제한 | 재시도 가능 |
| Read Timeout 변경 | E-TCF-TMO-0006 | X | 상태확인 |
| HTTP 500 | E-TCF-EXT-0005 | 제한 | 시스템 안내 |
| Parsing 실패 | E-TCF-MSG-0001 | X | 계약 장애 |
| Circuit Open | E-TCF-EXT-0006 | X | 빠른 실패 |
| 인증서 만료 | E-TCF-SEC-0001 | X | 긴급 운영조치 |

## 24.4.5 외부 원인 보존

java id="jtx123" catch (SocketTimeoutException exception) { throw new ExternalReadTimeoutException( "E-TCF-TMO-0006", targetSystem, interfaceId, exception ); }

Exception 속성:

\`\`\`text id=“jtx124” 내부 errorCode

targetSystem

interfaceId

externalErrorCode

HTTP status

retryable

failureStage

externalTransactionId

cause


\---

\## 24.4.6 외부 메시지 사용자 노출 금지

금지:

\`\`\`text id="jtx125"
SSLHandshakeException:
PKIX path building failed

Connection refused:
credit-db.internal:1521

External NullPointerException

SQLSTATE 08006

사용자:

text id="jtx126" 외부 정보 조회 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.

운영자:

text id="jtx127" target=CREDIT-HUB interface=SV-CREDIT-SYNC-0001 failureStage=TLS\_HANDSHAKE cause=PKIX\_PATH\_BUILDING\_FAILED

## 24.4.7 GUID와 외부 거래번호

\`\`\`text id=“jtx128” NSIGHT GUID G-20260718-000701

NSIGHT External Request ID NS-REQ-0001

외부 Transaction ID EXT-TRX-98123



세 값을 연계이력에 저장한다.

운영 추적:

\`\`\`text id="jtx129"
사용자 문의 GUID
↓
NSIGHT 거래로그
↓
External Call Ledger
↓
External Request ID
↓
상대 시스템 로그

## 24.4.8 Trace Header

외부 시스템이 지원한다면:

http id="jtx130" X-Correlation-Id: G-20260718-000701 traceparent: 00-{traceId}-{spanId}-01 X-Request-Id: NS-REQ-0001

외부 계약에 없는 임의 Header를 무단 추가하지 않는다.

## 24.4.9 외부 응답 검증

\`\`\`text id=“jtx131” HTTP Status

Content-Type

Body 크기

JSON·XML Parsing

계약 Version

필수 필드

코드값

금액·날짜 범위

응답 서명

외부 Request ID 일치

중복 응답

개인정보



HTTP 200만 보고 성공 처리하지 않는다.

\---

\## 24.4.10 Contract 오류

예:

\`\`\`text id="jtx132"
필수 resultCode 없음

날짜 형식 변경

금액 String → Object

Enum 신규값

응답 Body가 HTML 오류 페이지

gzip 손상

업무 오류가 아니라 계약·시스템 오류다.

## 24.4.11 응답 크기 제한

외부 시스템이 예상보다 큰 응답을 반환할 수 있다.

\`\`\`text id=“jtx133” 정상 예상 100KB

실제 500MB



방어:

\`\`\`text id="jtx134"
최대 Body 크기

Streaming

압축 해제 크기 제한

JSON Depth

배열 최대 건수

파일 연계 전환

## 24.4.12 로그 기준

권장:

text id="jtx135" event=EXTERNAL\_CALL\_COMPLETED guid=G-20260718-000701 traceId=T-20260718-000701 spanId=S-EXT-001 serviceId=SV.Credit.selectSummary interfaceId=SV-CREDIT-SYNC-0001 targetSystem=CREDIT-HUB method=POST result=SUCCESS httpStatus=200 externalResultCode=0000 elapsedMs=382 retryCount=0 circuitState=CLOSED externalTransactionId=EXT-TRX-98123

## 24.4.13 기록 금지

\`\`\`text id=“jtx136” Authorization Header

OAuth Client Secret

API Key

Private Key

전체 인증서 비밀번호

Refresh Token

주민번호 전체

계좌번호 전체

요청·응답 전문 전체

외부 URL Query의 개인정보

서명 원문


\---

\## 24.4.14 전문 Logging

필요 시 다음 방식으로 제한한다.

\`\`\`text id="jtx137"
필드 Allow List

마스킹

Body Hash

전문 길이

개발환경 한정

승인된 Trace Sampling

암호화 저장

접근권한

짧은 보존기간

## 24.4.15 외부 호출 실패 로그 Level

| 오류 | Level |
| --- | --- |
| 외부 업무 거절 | INFO·WARN |
| 외부 데이터 없음 | INFO |
| Rate Limit | WARN |
| Connect Timeout | WARN·ERROR |
| Read Timeout | WARN·ERROR |
| Circuit Open | WARN, 집계 |
| TLS 인증서 실패 | ERROR |
| Parsing·계약 오류 | ERROR |
| 외부 5xx | WARN·ERROR |
| 재시도 성공 | INFO·Metric |
| 보상 실패 | ERROR·긴급 알림 |

같은 오류 Stack Trace를 모든 Retry마다 반복 기록하지 않는다.

# 표준 External Client 구조

text id="jtx138" {business}.{domain}.client ├─ CreditInformationClient.java ├─ adapter │ └─ CreditInformationHttpAdapter.java ├─ contract │ ├─ CreditExternalRequest.java │ └─ CreditExternalResponse.java ├─ mapper │ └─ CreditExternalContractMapper.java ├─ policy │ └─ CreditExternalErrorMapper.java └─ config └─ CreditExternalClientProperties.java

외부 계약이 복잡하다면 별도 Adapter 모듈을 사용할 수 있다.

# 표준 External Call Context

java id="jtx139" public record ExternalCallContext( String interfaceId, String guid, String traceId, String spanId, String userId, String branchId, String serviceId, long remainingTimeoutMs, String idempotencyKey, Instant requestedAt ) {}

# 외부 Client 예

\`\`\`java id=“jtx140” @Component @RequiredArgsConstructor class CreditInformationHttpAdapter implements CreditInformationClient {

private final RestClient restClient;
private final ExternalPolicyExecutor policyExecutor;
private final CreditContractMapper mapper;
private final CreditErrorMapper errorMapper;

@Override
public CreditInquiryResult inquire(
CreditInquiryCommand command,
ExternalCallContext context) {

ExternalCreditRequest request =
mapper.toRequest(command, context);

try {
ExternalCreditResponse response =
policyExecutor.executeInquiry(
"CREDIT-HUB",
context,
() -> restClient.post()
.uri("/v1/credit/inquiry")
.header(
"X-Correlation-Id",
context.guid()
)
.body(request)
.retrieve()
.body(
ExternalCreditResponse.class
)
);

return mapper.toResult(response);

} catch (ExternalProviderException exception) {
throw errorMapper.map(exception);
}
}

}


\---

\# 정상 동기 조회 흐름

\`\`\`text id="jtx141"
1\. 화면 요청이 TCF에 진입한다.
2\. Service가 외부 조회 필요성을 판단한다.
3\. External Client Adapter가 내부 Command를 외부 Request로 변환한다.
4\. GUID·TraceId·외부 Request ID를 생성·전파한다.
5\. 잔여 Timeout을 계산한다.
6\. Circuit Breaker가 호출 가능 상태인지 확인한다.
7\. Bulkhead가 실행 슬롯을 확보한다.
8\. OAuth·mTLS 등 인증정보를 적용한다.
9\. HTTP 요청을 전송한다.
10\. 외부 시스템이 업무 결과를 반환한다.
11\. Adapter가 HTTP·업무코드·계약을 검증한다.
12\. 외부 Response를 내부 Result로 변환한다.
13\. Service가 업무 응답을 조립한다.
14\. ETF가 사용자에게 정상 결과를 반환한다.
15\. Metric과 Trace에 외부 처리시간을 기록한다.

# 정상 비동기 변경 흐름

text id="jtx142" 1. 사용자가 외부 처리 요청을 실행한다. 2. Facade가 로컬 Transaction을 시작한다. 3. 내부 업무 상태를 REQUESTED로 저장한다. 4. Outbox Event와 Idempotency Key를 저장한다. 5. 로컬 Transaction을 Commit한다. 6. 사용자에게 접수번호를 반환한다. 7. Publisher가 Outbox를 읽는다. 8. Circuit·Rate Limit·Retry 정책에 따라 외부 요청을 전송한다. 9. 외부 시스템이 접수번호를 반환한다. 10. External Call Ledger를 ACCEPTED로 갱신한다. 11. Callback 또는 상태조회로 최종 결과를 확인한다. 12. 내부 상태를 SUCCESS·FAIL로 전환한다. 13. 결과 이벤트와 감사로그를 생성한다. 14. 대사를 수행한다.

# Read Timeout 흐름

## 조회

text id="jtx143" 외부 조회 ↓ Read Timeout ↓ Retry 가능 오류인지 확인 ↓ 잔여예산 충분 ↓ 동일 GUID·신규 Attempt ↓ 최대 1회 Retry ↓ 성공 또는 연계 Timeout

## 변경

text id="jtx144" 외부 변경 ↓ Read Timeout ↓ 실제 반영 여부 불명 ↓ UNKNOWN ↓ 상태조회 ↓ 확정 후 성공·실패·보상

# Circuit Open 흐름

\`\`\`text id=“jtx145” 호출 요청 ↓ Circuit OPEN ↓ 네트워크 호출 미실행 ↓ 필수 의존 → 빠른 실패

선택 의존 → Fallback·Partial

비동기 변경 → Outbox RETRY\_WAIT


\---

\# Callback 흐름

\`\`\`text id="jtx146"
외부 Callback
↓
mTLS·HMAC 검증
↓
Timestamp·Nonce 확인
↓
Callback Event ID Unique
↓
외부 Request ID 매핑
↓
상태 전이 검증
↓
업무 상태 갱신
↓
이력·감사·응답

# 오류·Timeout·장애 흐름

## 인증서 만료

text id="jtx147" TLS Handshake ↓ Certificate Expired ↓ 외부 호출 전부 실패 ↓ Circuit Open ↓ ERROR 경보 ↓ 인증서 교체·재배포·검증

## 외부 503

text id="jtx148" HTTP 503 ↓ Retryable 여부 확인 ↓ Backoff·Jitter ↓ 1회 Retry ↓ 지속 실패 ↓ Circuit 실패 Count

## 응답 계약 오류

text id="jtx149" HTTP 200 ↓ resultCode 없음 ↓ ContractException ↓ Retry 금지 ↓ 배포버전·외부 변경 확인

## 로컬 성공·외부 실패

text id="jtx150" 로컬 업무 Commit ↓ 외부 전송 실패 ↓ Outbox RETRY\_WAIT ↓ 제한 재시도 ↓ 최종 실패 ↓ MANUAL\_REVIEW·보상

# 정상 예시

\`\`\`text id=“jtx151” Interface ID SV-CREDIT-SYNC-0001

ServiceId SV.Credit.selectSummary

외부 시스템 CREDIT-HUB

방식 동기 조회

인증 mTLS + OAuth2 Client Credentials

TCF 전체 Timeout 3,000ms

외부 Overall Timeout 1,200ms

Connect Timeout 300ms

Read Timeout 700ms

Retry HTTP 503·Connect Reset 최대 1회

Circuit 실패율 50%, Open 30초

Fallback 없음

GUID 상대 Header로 전달

결과 HTTP 200 + 외부 resultCode 0000

내부 결과 SUCCESS


\---

\# 금지 예시

\`\`\`text id="jtx152"
Handler에서 RestClient를 직접 호출한다.

Rule에서 외부 시스템을 호출한다.

DAO·Mapper에서 HTTP 호출을 수행한다.

외부 DTO를 내부 Domain 객체로 그대로 사용한다.

외부 Endpoint를 Java 코드에 하드코딩한다.

API Key와 비밀번호를 application.yml에 평문 저장한다.

Private Key를 Git에 저장한다.

모든 외부 호출에 같은 Timeout을 적용한다.

외부 Read Timeout이 TCF 전체 Timeout보다 길다.

Retry 횟수를 무제한으로 설정한다.

모든 HTTP 4xx·5xx를 Retry한다.

업무 오류를 Retry한다.

비멱등 등록 요청을 자동 Retry한다.

변경 Timeout 후 신규 Request ID로 다시 호출한다.

Retry마다 새로운 Idempotency Key를 만든다.

Circuit Breaker가 열려 있는데 실제 호출을 계속한다.

업무 오류를 Circuit 실패로 집계한다.

Circuit Open을 시스템 정상으로 숨긴다.

Fallback 데이터의 기준시각을 표시하지 않는다.

외부 잔액 실패 시 오래된 잔액을 현재값처럼 반환한다.

외부 변경을 로컬 DB Transaction으로 Rollback할 수 있다고 가정한다.

로컬 DB Lock을 보유한 채 느린 외부 호출을 기다린다.

Callback 인증을 IP 주소만으로 판단한다.

Callback Event ID 중복을 확인하지 않는다.

Callback의 오래된 상태가 최신 상태를 덮어쓰게 한다.

HTTP 200이면 무조건 성공 처리한다.

외부 Exception 메시지를 사용자에게 반환한다.

요청·응답 전문 전체를 로그에 저장한다.

Authorization Header를 로그에 기록한다.

GUID와 외부 Transaction ID를 저장하지 않는다.

외부 시스템 장애 시 담당 조직과 복구절차가 없다.

인증서 만료일을 수동 기억에 의존한다.

# 연계 계약 표준

| 구분 | 필수 항목 |
| --- | --- |
| 식별 | Interface ID·소비 업무·대상 시스템 |
| 목적 | 조회·검증·등록·발송·상태조회 |
| 방식 | 동기·비동기·Callback·파일 |
| Endpoint | URL·Method·Content-Type |
| 계약 | Request·Response·Schema Version |
| 인증 | OAuth·mTLS·HMAC·API Key |
| 추적 | GUID·TraceId·외부 Request ID |
| Timeout | Connect·Read·Overall |
| Retry | 대상 오류·횟수·Backoff |
| Circuit | 임계치·Open·Half-Open |
| 격리 | Bulkhead·Pool·Rate Limit |
| 변경 | Idempotency·상태조회·보상 |
| 오류 | 외부·내부 매핑 |
| 데이터 | 개인정보·암호화·마스킹 |
| 운영 | SLA·담당자·알림·Runbook |
| 변경 | Version·배포순서·폐기일 |

# 보안·인증 설계

## 인증 방식 비교

| 방식 | 적합 상황 | 주의 |
| --- | --- | --- |
| mTLS | 서버 간 강한 상호인증 | 인증서 운영 |
| OAuth2 Client Credentials | API 기반 서비스 인증 | Token Cache·Secret |
| HMAC | 전문 위변조 검증 | Secret Rotation·재전송 |
| API Key | 단순 Provider | 단독 사용 한계 |
| JWT Service Token | 사내 표준 서비스 신원 | Audience·키 회전 |
| 전용망·IP | 보조 통제 | 단독 인증 금지 |

## OAuth2 Token

관리:

\`\`\`text id=“jtx153” Client ID

Client Secret

Token Endpoint

Audience·Scope

Token TTL

Refresh 정책

Token Cache

Token 발급 장애

Secret Rotation



OAuth Token을 매 외부 호출마다 새로 발급하지 않는다.

만료 전 Cache하고 동시 재발급을 제어한다.

\---

\## mTLS

관리:

\`\`\`text id="jtx154"
Client Certificate

Private Key

Trust Store

Server Name 검증

인증서 만료일

Rotation

비밀번호

HSM·Vault

폐기

인증서 만료 90일·60일·30일 전에 단계별 경보를 구성한다.

## HMAC

Canonical 대상:

\`\`\`text id=“jtx155” HTTP Method

Path

Timestamp

Nonce

Body Hash

Request ID



검증:

\`\`\`text id="jtx156"
Timestamp 허용범위

Nonce 재사용

서명 비교

Secret Version

Body Hash

# 데이터 및 상태관리

## 외부 시스템 기준정보

sql id="jtx157" CREATE TABLE OM\_EXTERNAL\_SYSTEM ( EXTERNAL\_SYSTEM\_ID VARCHAR2(50) NOT NULL, EXTERNAL\_SYSTEM\_NAME VARCHAR2(200) NOT NULL, SYSTEM\_TYPE VARCHAR2(30) NOT NULL, OWNER\_ORG VARCHAR2(100) NOT NULL, OPERATOR\_CONTACT VARCHAR2(200), SLA\_AVAILABILITY NUMBER(5,2), DEFAULT\_AUTH\_TYPE VARCHAR2(30), USE\_YN CHAR(1) NOT NULL, CREATED\_AT TIMESTAMP NOT NULL, UPDATED\_AT TIMESTAMP NOT NULL, CONSTRAINT PK\_OM\_EXTERNAL\_SYSTEM PRIMARY KEY (EXTERNAL\_SYSTEM\_ID) );

## 외부 Interface 기준정보

sql id="jtx158" CREATE TABLE OM\_EXTERNAL\_INTERFACE ( INTERFACE\_ID VARCHAR2(50) NOT NULL, EXTERNAL\_SYSTEM\_ID VARCHAR2(50) NOT NULL, BUSINESS\_CODE VARCHAR2(20) NOT NULL, SERVICE\_ID VARCHAR2(100) NOT NULL, INTERFACE\_NAME VARCHAR2(200) NOT NULL, CALL\_TYPE VARCHAR2(20) NOT NULL, CONTRACT\_VERSION VARCHAR2(20) NOT NULL, CONNECT\_TIMEOUT\_MS NUMBER(10), READ\_TIMEOUT\_MS NUMBER(10), OVERALL\_TIMEOUT\_MS NUMBER(10), RETRY\_POLICY\_ID VARCHAR2(50), CIRCUIT\_POLICY\_ID VARCHAR2(50), IDEMPOTENCY\_REQUIRED\_YN CHAR(1) NOT NULL, USE\_YN CHAR(1) NOT NULL, CONSTRAINT PK\_OM\_EXTERNAL\_INTERFACE PRIMARY KEY (INTERFACE\_ID) );

비밀정보 원문은 기준정보 테이블에 저장하지 않는다.

text id="jtx159" secretReference certificateAlias vaultPath

만 관리한다.

# 성능·용량·확장성

## 외부 호출 TPS

text id="jtx160" 업무 TPS × 거래당 외부 호출 수 × Retry 배수

예:

\`\`\`text id=“jtx161” 업무 100 TPS

외부 호출 거래당 2회

Retry 평균 1.1배

외부 요청 220 TPS


\---

\## 동시 대기 Thread

\`\`\`text id="jtx162"
동시 호출 수
≈ TPS × 평균 외부 응답시간

예:

text id="jtx163" 220 TPS × 0.5초 = 약 110개 동시 호출

Thread·Connection Pool과 외부 상대 한도를 함께 계산한다.

## Retry 포함 Peak

장애 시:

\`\`\`text id=“jtx164” 평상시 220 TPS

1회 Retry → 최대 440 TPS



Circuit·Rate Limit이 없으면 장애 부하가 확대된다.

\---

\## 비동기 Queue 용량

\`\`\`text id="jtx165"
유입 TPS
\-
외부 처리 TPS
\=
Queue 증가속도

확인:

\`\`\`text id=“jtx166” 최대 적체 건수

Disk 용량

보관시간

재처리시간

RTO

외부 복구 후 Drain 속도

온라인 부하 영향


\---

\## Response 크기

\`\`\`text id="jtx167"
최대 요청 Byte

최대 응답 Byte

압축 기준

Streaming 기준

JSON 배열 최대 수

파일 전환 기준

## Connection Pool 격리

\`\`\`text id=“jtx168” 외부 A Pool

외부 B Pool

내부 업무 WAR 호출 Pool



을 구분할 수 있다.

하나의 느린 대상이 모든 HTTP Connection을 점유하지 않게 한다.

\---

\# 보안·개인정보·감사

| 영역 | 기준 |
|---|---|
| Secret | Vault·KMS·HSM |
| 인증서 | 만료·Rotation·폐기 |
| 통신 | TLS 1.2 이상 등 승인 정책 |
| 서버명 | Hostname 검증 |
| 요청 | 개인정보 최소화 |
| 응답 | 허용 필드만 사용 |
| 로그 | 전문 원문 금지 |
| Trace | GUID·Request ID |
| 변경 요청 | 사용자·서비스 신원 |
| Callback | mTLS·HMAC·Nonce |
| 파일 | 암호화·Hash·악성코드 |
| 감사 | 외부 변경·재처리·보상 |
| 접근통제 | 외부 Endpoint Allow List |
| DNS | 승인된 Host만 |
| Redirect | 자동 Redirect 제한 |
| SSRF | 사용자 입력 URL 호출 금지 |

\---

\## SSRF 방지

금지:

\`\`\`java id="jtx169"
restClient.get()
.uri(userRequest.getUrl())

외부 URL은 승인된 Interface Catalog에서 조회한다.

\`\`\`text id=“jtx170” 사용자 입력 → 대상 URL이 아니라 업무 식별자

서버 → 승인된 Endpoint로 변환


\---

\## Redirect

외부 서버의 Redirect를 자동으로 따라갈 때 인증 Header가 다른 Host로 전달될 수 있다.

정책:

\`\`\`text id="jtx171"
Redirect 금지 또는 제한

동일 Host만 허용

인증 Header 재전송 금지

최대 횟수

# 운영·모니터링·장애 대응

## 권장 Metric

\`\`\`text id=“jtx172” external.call.count

external.call.duration

external.call.success.count

external.call.business.fail.count

external.call.system.fail.count

external.call.connect.timeout.count

external.call.read.timeout.count

external.call.retry.count

external.call.retry.success.count

external.call.circuit.open.count

external.call.bulkhead.rejected.count

external.call.rate.limit.count

external.call.unknown.count

external.callback.duplicate.count

external.reconciliation.difference


\---

\## 권장 Dashboard

\`\`\`text id="jtx173"
대상 시스템별 호출량

성공률

p50·p95·p99

Connect·Read Timeout

HTTP 상태

외부 업무코드

Retry율

Circuit 상태

Bulkhead 사용률

Connection Pool

UNKNOWN 건수

Queue 적체

Callback 지연

대사 차이

인증서 만료일

## 장애 점검 순서

text id="jtx174" 1. 사용자 GUID 확인 2. NSIGHT ServiceId 확인 3. Interface ID와 대상 시스템 확인 4. External Call Ledger 확인 5. DNS·Network·TLS 확인 6. Connect·Read Timeout 구분 7. HTTP Status·외부 오류코드 확인 8. Retry 횟수 확인 9. Circuit·Bulkhead 상태 확인 10. 외부 Request ID로 상대 로그 확인 11. 내부 Transaction 결과 확인 12. 멱등성·UNKNOWN 상태 확인 13. Callback·상태조회 확인 14. 내부·외부 데이터 대사 15. 재처리·보상 결정

## 외부 장애 시 임시 조치

대안:

\`\`\`text id=“jtx175” Circuit Open

거래통제

선택 기능 비활성화

비동기 Queue 전환

Fallback

Retry 일시 중지

호출량 제한

대체 Endpoint

업무시간 이후 재처리



위험을 비교하고 승인 후 적용한다.

무조건 애플리케이션을 재기동하지 않는다.

\---

\## 인증서 만료 Runbook

\`\`\`text id="jtx176"
1\. 만료 대상 Interface 식별
2\. 신규 인증서 발급
3\. Chain·SAN·용도 검증
4\. Vault·Key Store 등록
5\. 개발·검증환경 시험
6\. 양쪽 적용순서 협의
7\. 병행 인증서 기간 운영
8\. 운영 교체
9\. Smoke Test
10\. 구 인증서 폐기
11\. 감사 증적 보존

## UNKNOWN 거래 Runbook

text id="jtx177" 1. 신규 재요청 차단 2. External Request ID 확인 3. 외부 상태조회 4. 외부 운영팀 로그 확인 5. 내부 DB 상태 확인 6. Idempotency 상태 확인 7. 성공·실패 확정 8. 내부 상태 보정 9. 필요 시 보상 10. 대사·감사 종료

# 책임 경계와 RACI

| 활동 | 업무개발 | EAI·연계 | AA | 보안 | 인프라 | 운영 | QA | 외부기관 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 연계 목적 | A/R | C | C | C | I | I | C | C |
| 방식 선택 | C | R | A | C | C | C | C | C |
| 업무 Contract | A/R | R/C | C | C | I | I | C | A/C |
| Client Adapter | R | A/C | C | C | I | I | C | C |
| Timeout | C | R | A | I | R/C | C | C | C |
| Retry·Circuit | C | R | A | I | C | R/C | C | C |
| 멱등성 | A/R | R/C | A/C | I | I | C | C | C |
| 상태조회·보상 | A/R | R | A | I | I | R/C | C | C |
| 인증·Secret | C | C | C | A/R | R/C | C | C | C |
| 인증서 운영 | I | C | C | A/C | A/R | R | C | C |
| 오류 매핑 | R | R | C | I | I | C | C | C |
| Trace·Metric | C | R | A | I | C | A/R | C | C |
| 장애 Runbook | C | C | C | C | R | A/R | C | R/C |
| Contract Test | R | R | C | C | I | I | A/R | R/C |
| 폐기·전환 | C | A/R | A | C | C | R/C | C | A/R |

# 자동검증 및 품질 Gate

## 1\. 외부 연계 등록 Gate

\`\`\`text id=“jtx178” Interface ID

대상 시스템

소유 업무

호출 방식

Contract Version

Timeout

Retry

Circuit

인증

개인정보

담당자

Runbook



미등록 외부 Endpoint 호출을 금지한다.

\---

\## 2. 계층 Gate

\`\`\`text id="jtx179"
Handler → External Client 금지

Rule → External Client 금지

DAO → External Client 금지

Mapper → External Client 금지

Service·Orchestrator → 승인된 Client만 허용

## 3\. URL·Secret Gate

검출:

\`\`\`text id=“jtx180” http://·https:// 하드코딩 URL

API Key 문자열

Client Secret

BEGIN PRIVATE KEY

Key Store Password

사용자 입력 URL



승인된 테스트 Fixture는 Allow List로 관리한다.

\---

\## 4. Timeout Gate

\`\`\`text id="jtx181"
Connect > 0

Read > Connect

Overall ≥ Read

Overall < Remaining TCF Timeout

Retry 포함 최대시간 < Overall

## 5\. Retry Gate

\`\`\`text id=“jtx182” Retry 대상 오류 명시

최대 횟수

Backoff

Jitter

전체 Timeout

멱등성

Retry Metric



변경 거래에 멱등성 근거가 없으면 Retry 설정을 차단한다.

\---

\## 6. Circuit Gate

\`\`\`text id="jtx183"
최소 호출 수

실패율

Slow Call

Open 시간

Half-Open 호출 수

Fallback

알림

복구 테스트

## 7\. 변경 연계 Gate

\`\`\`text id=“jtx184” Idempotency Key

Request Hash

External Request ID

업무 키

상태조회

UNKNOWN

보상

대사


\---

\## 8. Contract Gate

\`\`\`text id="jtx185"
HTTP Status

응답 업무코드

필수 필드

Enum

날짜·금액 단위

최대 Body

Content-Type

Contract Version

## 9\. 보안 Gate

\`\`\`text id=“jtx186” TLS

Hostname 검증

Secret Vault

인증서 만료

Callback 서명

SSRF

Redirect

전문 마스킹


\---

\## 10. 운영 Gate

Architecture Gate에서는 외부 연계 계약, 재시도 가능 거래, Circuit Breaker, Timeout 계층, 보상·재처리, GUID·TraceId 전파를 모두 검증 대상으로 둔다.

\`\`\`text id="jtx187"
Metric

Dashboard

Alert

담당 조직

Runbook

대사

재처리 권한

# 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| EXT-001 | 정상 동기 조회 | 성공 |
| EXT-002 | 정상 비동기 접수 | ACCEPTED |
| EXT-003 | 정상 Callback | 최종 SUCCESS |
| EXT-004 | 외부 업무 오류 | 내부 업무 오류 매핑 |
| EXT-005 | HTTP 400 | Retry 없음 |
| EXT-006 | HTTP 401 | 인증정책 처리 |
| EXT-007 | HTTP 403 | Retry 없음 |
| EXT-008 | HTTP 404 | 계약·업무 정책 |
| EXT-009 | HTTP 429 | Retry-After 적용 |
| EXT-010 | HTTP 500 | 연계 시스템 오류 |
| EXT-011 | HTTP 502 | 제한 Retry |
| EXT-012 | HTTP 503 | Retry·Circuit |
| EXT-013 | HTTP 504 | Timeout 처리 |
| EXT-014 | DNS 실패 | Connect 계열 오류 |
| EXT-015 | Connection Refused | 제한 Retry |
| EXT-016 | TLS 인증서 만료 | 즉시 실패·경보 |
| EXT-017 | Hostname 불일치 | 차단 |
| EXT-018 | Connect Timeout | 단계별 오류 |
| EXT-019 | Read Timeout 조회 | 제한 Retry |
| EXT-020 | Read Timeout 변경 | UNKNOWN |
| EXT-021 | Pool Acquire Timeout | Bulkhead·Pool 오류 |
| EXT-022 | TCF Timeout 선발생 | 설정 Gate 실패 |
| EXT-023 | Remaining Budget 없음 | 호출 전 차단 |
| EXT-024 | Retry 첫 호출 실패·두 번째 성공 | 성공·Retry Metric |
| EXT-025 | Retry 모두 실패 | 최종 오류 |
| EXT-026 | 업무 오류 Retry 설정 | Gate 실패 |
| EXT-027 | 비멱등 변경 Retry | Gate 실패 |
| EXT-028 | Backoff 없음 | 정책 실패 |
| EXT-029 | 다중 인스턴스 동시 Retry | Jitter 분산 |
| EXT-030 | 실패율 임계치 | Circuit OPEN |
| EXT-031 | Circuit OPEN 신규 호출 | 네트워크 미실행 |
| EXT-032 | Half-Open 성공 | CLOSED |
| EXT-033 | Half-Open 실패 | OPEN 유지 |
| EXT-034 | 업무 오류 다수 | Circuit 미개방 |
| EXT-035 | Slow Call 임계치 | Circuit 정책 |
| EXT-036 | Bulkhead 동시성 초과 | 빠른 거절 |
| EXT-037 | 외부 Rate Limit | 내부 제한 |
| EXT-038 | Connection Leak | Pool 회복 실패 탐지 |
| EXT-039 | 정상 멱등 변경 | 1건 처리 |
| EXT-040 | 동일 Key 재요청 | 기존 결과 |
| EXT-041 | 같은 Key 다른 Body | 차단 |
| EXT-042 | 외부 멱등 미지원 | 자동 Retry 금지 |
| EXT-043 | 변경 응답 유실 | 상태조회 |
| EXT-044 | 상태조회 SUCCESS | 내부 성공 보정 |
| EXT-045 | 상태조회 FAIL | 실패 확정 |
| EXT-046 | 상태조회 NOT\_FOUND | 계약대로 처리 |
| EXT-047 | Callback 중복 | 1회 반영 |
| EXT-048 | Callback 순서 역전 | 상태 역행 차단 |
| EXT-049 | Callback 서명 오류 | 401·보안로그 |
| EXT-050 | Callback Timestamp 만료 | 차단 |
| EXT-051 | Queue 중복 메시지 | 멱등 처리 |
| EXT-052 | DLQ 이동 | 운영 알림 |
| EXT-053 | 로컬 Commit·외부 실패 | Outbox 재시도 |
| EXT-054 | 외부 성공·로컬 실패 | 보상 대상 |
| EXT-055 | 보상 성공 | COMPENSATED |
| EXT-056 | 보상 실패 | MANUAL\_REVIEW |
| EXT-057 | 입력·출력 건수 대사 | 차이 0 |
| EXT-058 | 금액 대사 불일치 | 경보 |
| EXT-059 | HTTP 200·업무 실패 | 오류 매핑 |
| EXT-060 | HTTP 200·필수 필드 없음 | Contract 오류 |
| EXT-061 | 응답 Enum 신규값 | 호환정책 |
| EXT-062 | 응답 Body 초과 | 크기 제한 |
| EXT-063 | 압축 폭탄 | 차단 |
| EXT-064 | Redirect 다른 Host | 차단 |
| EXT-065 | 사용자 입력 URL | SSRF Gate 실패 |
| EXT-066 | API Key 로그 | 보안 Gate 실패 |
| EXT-067 | 전문 개인정보 로그 | 보안 Gate 실패 |
| EXT-068 | GUID 전파 | 상대 로그 연결 |
| EXT-069 | 외부 거래번호 저장 | 양방향 추적 |
| EXT-070 | 인증서 Rotation | 무중단 |
| EXT-071 | OAuth Token 만료 | 1회 갱신 |
| EXT-072 | OAuth 발급서버 장애 | Fail Closed·Cache |
| EXT-073 | 36,000명 Peak | Thread·Pool 기준 |
| EXT-074 | 외부 장기 장애 | 내부 업무 격리 |
| EXT-075 | Fallback Cache | 기준시각 표시 |
| EXT-076 | 중요정보에 부정확 Fallback | Gate 실패 |
| EXT-077 | Contract V1·V2 병행 | 호환 |
| EXT-078 | Provider 선배포 | 기존 소비자 정상 |
| EXT-079 | 폐기 Interface 호출 | 경고·대체 |
| EXT-080 | 장애 Runbook 훈련 | 증적 완료 |

# 따라 하는 실무 절차

## 1단계. 외부 연계 목적을 정의한다

\`\`\`text id=“jtx188” 어떤 업무 결과가 필요한가?

즉시 결과가 필요한가?

조회인가 변경인가?

대량인가?

부분 성공이 가능한가?


\---

\## 2단계. 방식을 선택한다

\`\`\`text id="jtx189"
동기

비동기

Callback

파일

Batch

ADR에 선택 이유와 대안을 기록한다.

## 3단계. Interface Catalog를 등록한다

완료 증적:

\`\`\`text id=“jtx190” Interface ID

대상 시스템

담당자

SLA

Endpoint

계약 Version


\---

\## 4단계. 계약을 확정한다

\`\`\`text id="jtx191"
Request·Response

업무코드

HTTP 상태

오류코드

최대 크기

Null·필수

상태조회

## 5단계. 인증을 확정한다

\`\`\`text id=“jtx192” mTLS

OAuth

HMAC

Secret Vault

인증서 Rotation


\---

\## 6단계. Timeout Budget을 작성한다

\`\`\`text id="jtx193"
TCF 전체

Connect

Read

Overall

Retry

안전여유

## 7단계. Retry·Circuit·Bulkhead를 설계한다

\`\`\`text id=“jtx194” Retryable 오류

횟수

Backoff

Circuit 임계치

Half-Open

대상별 동시성


\---

\## 8단계. 멱등성과 상태조회를 설계한다

\`\`\`text id="jtx195"
업무 키

Idempotency Key

Request ID

외부 거래번호

UNKNOWN

상태조회

## 9단계. Transaction과 보상을 설계한다

\`\`\`text id=“jtx196” 로컬 Commit

Outbox

외부 호출

보상

대사


\---

\## 10단계. Client Adapter를 구현한다

\`\`\`text id="jtx197"
내부 Command

외부 Request

인증

호출

오류 매핑

내부 Result

## 11단계. 정상·장애 테스트를 수행한다

\`\`\`text id=“jtx198” 지연

Timeout

Retry

Circuit

중복

Callback

보상

계약 변경


\---

\## 12단계. 운영 증적을 확인한다

\`\`\`text id="jtx199"
GUID

External Request ID

Metric

Circuit

Pool

인증서

대사

Runbook

# 완료 체크리스트

## 방식·계약

| 확인 항목 | 완료 |
| --- | --- |
| 동기·비동기 선택 근거가 있다. | □ |
| 필수·선택 의존을 구분했다. | □ |
| Interface ID가 있다. | □ |
| 외부 시스템 Owner가 있다. | □ |
| Request·Response 계약이 있다. | □ |
| Contract Version이 있다. | □ |
| 최대 전문 크기가 있다. | □ |
| 업무 성공조건이 명확하다. | □ |
| Callback·상태조회 여부가 명확하다. | □ |

## 계층·구현

| 확인 항목 | 완료 |
| --- | --- |
| Service가 External Client Interface만 사용한다. | □ |
| Handler·Rule·DAO가 외부를 호출하지 않는다. | □ |
| 외부 DTO와 내부 모델을 분리했다. | □ |
| URL이 코드에 하드코딩되지 않았다. | □ |
| 환경별 Endpoint가 분리됐다. | □ |
| Contract Mapper가 있다. | □ |
| 오류 Mapper가 있다. | □ |
| 응답 필수필드를 검증한다. | □ |

## Timeout·복원력

| 확인 항목 | 완료 |
| --- | --- |
| Connect Timeout이 있다. | □ |
| Read Timeout이 있다. | □ |
| Overall Timeout이 있다. | □ |
| TCF 잔여예산을 반영한다. | □ |
| Retryable 오류를 정의했다. | □ |
| 최대 Retry 횟수가 있다. | □ |
| Backoff·Jitter가 있다. | □ |
| Circuit Breaker가 있다. | □ |
| Half-Open 시험이 있다. | □ |
| Bulkhead·Rate Limit이 있다. | □ |
| Connection Pool 상한이 있다. | □ |

## 변경·멱등성

| 확인 항목 | 완료 |
| --- | --- |
| 변경 요청을 식별했다. | □ |
| Idempotency Key가 있다. | □ |
| 같은 Key에 같은 Payload를 보장한다. | □ |
| 외부 Request ID가 있다. | □ |
| 외부 거래번호를 저장한다. | □ |
| Timeout 시 UNKNOWN을 사용한다. | □ |
| 상태조회가 있다. | □ |
| Callback 중복을 차단한다. | □ |
| Callback 순서를 검증한다. | □ |
| Outbox·보상정책이 있다. | □ |
| 내부·외부 대사를 수행한다. | □ |

## 보안

| 확인 항목 | 완료 |
| --- | --- |
| TLS를 사용한다. | □ |
| Hostname을 검증한다. | □ |
| Secret을 Vault에 저장한다. | □ |
| 인증서 만료경보가 있다. | □ |
| 인증서 Rotation 절차가 있다. | □ |
| Callback 서명을 검증한다. | □ |
| SSRF를 차단한다. | □ |
| Redirect 정책이 있다. | □ |
| 개인정보 최소화를 적용했다. | □ |
| 전문 원문 로그가 없다. | □ |
| 인증정보 원문 로그가 없다. | □ |

## 운영·품질

| 확인 항목 | 완료 |
| --- | --- |
| 대상별 성공률·지연 Metric이 있다. | □ |
| Retry·Circuit Metric이 있다. | □ |
| Pool·Bulkhead Metric이 있다. | □ |
| UNKNOWN Dashboard가 있다. | □ |
| 외부 담당 연락처가 있다. | □ |
| 장애 Runbook이 있다. | □ |
| 인증서 Runbook이 있다. | □ |
| 대사·재처리 권한이 있다. | □ |
| Contract Test가 있다. | □ |
| 장애 주입 테스트를 수행했다. | □ |

# 변경·호환성·폐기 관리

## Endpoint 변경

\`\`\`text id=“jtx200” 기존 api-old.external

신규 api-new.external



검증:

\`\`\`text id="jtx201"
DNS

인증서 SAN

Firewall

mTLS

Routing

Timeout

성능

Rollback

병행 호출 또는 점진 전환을 검토한다.

## Contract Version 변경

하위호환:

text id="jtx202" 선택 응답 필드 추가

비호환:

\`\`\`text id=“jtx203” 필수 필드 추가

필드 삭제

타입 변경

코드 의미 변경

인증방식 변경

업무 성공조건 변경



V1·V2 병행기간과 소비자 전환계획이 필요하다.

\---

\## 인증방식 변경

\`\`\`text id="jtx204"
API Key
→ OAuth2 + mTLS

영향:

\`\`\`text id=“jtx205” Secret

인증서

Token Cache

Firewall

배포순서

장애정책

성능


\---

\## Timeout 변경

Timeout 축소:

\`\`\`text id="jtx206"
빠른 실패

오류율 증가 가능

Timeout 확대:

\`\`\`text id=“jtx207” 성공률 증가 가능

Thread·Pool 점유 증가



실측 p95·p99와 전체 거래 Budget으로 판단한다.

\---

\## Retry 변경

\`\`\`text id="jtx208"
0회
→ 2회

영향:

\`\`\`text id=“jtx209” 외부 부하

최대 응답시간

중복 처리

Circuit 집계

사용자 체감

비용


\---

\## 외부 시스템 폐기

\`\`\`text id="jtx210"
신규 호출 중지
↓
대체 Interface 제공
↓
소비 ServiceId 조사
↓
V1 Deprecated
↓
호출량 0 확인
↓
Queue·UNKNOWN·DLQ 정리
↓
대사 완료
↓
인증서·Secret 폐기
↓
Firewall 제거
↓
코드·설정 제거
↓
이력 보존

# 시사점

## 핵심 아키텍처 판단

첫째, 외부 시스템 호출 방식은 개발 편의가 아니라 업무 완료시점과 장애 허용수준을 기준으로 선택해야 한다.

둘째, 외부 호출은 Service 내부의 HTTP 코드가 아니라 독립된 Client Adapter 경계로 구현해야 한다.

셋째, Timeout은 하나의 숫자가 아니다.

\`\`\`text id=“jtx211” Connect

TLS

Write

Read

Overall

TCF 전체



를 계층적으로 설계해야 한다.

넷째, Retry는 일시적 오류와 멱등한 요청에만 제한적으로 적용해야 한다.

다섯째, Circuit Breaker는 외부 시스템을 복구하는 기능이 아니라 외부 장애가 내부 Thread와 Pool을 고갈시키지 않도록 하는 보호장치다.

여섯째, 변경 연계의 Read Timeout은 실패 확정이 아니라 \*\*결과 불명 상태\*\*일 수 있다.

\`\`\`text id="jtx212"
TIMEOUT
≠ 외부 미처리

일곱째, 로컬 DB Transaction은 외부 시스템의 Commit을 Rollback할 수 없으므로 Outbox·보상·대사와 상태머신이 필요하다.

여덟째, 사용자 응답에는 안전한 내부 오류만 제공하고, 운영로그에는 GUID·외부 Request ID·외부 거래번호·원인 예외를 연결해야 한다.

## 주요 위험

| 위험 | 결과 |
| --- | --- |
| 동기 호출 남용 | 사용자 거래 지연 |
| 외부 호출 계층 혼재 | 표준 적용 불가 |
| Timeout 과대 | Thread·Pool 고갈 |
| Timeout 과소 | 불필요한 실패 증가 |
| 무제한 Retry | 장애 부하 증폭 |
| 비멱등 변경 Retry | 중복 처리 |
| Circuit 없음 | 장애 전파 |
| 업무 오류 Circuit 집계 | 정상 거절로 Circuit Open |
| Bulkhead 없음 | 한 대상이 전체 자원 점유 |
| 오래된 Fallback | 잘못된 업무 판단 |
| 결과 불명 미관리 | 중복·누락 |
| Callback 중복 | 상태 중복 변경 |
| Callback 순서 미검증 | 상태 역행 |
| 로컬 Rollback 오해 | 내부·외부 불일치 |
| 상태조회 없음 | Timeout 복구 불가 |
| 외부 오류 원문 노출 | 보안정보 노출 |
| Secret 하드코딩 | 인증정보 유출 |
| 인증서 만료 미관리 | 전면 연계 장애 |
| 전문 전체 로그 | 개인정보 유출 |
| 대사 없음 | 처리결과 증명 불가 |

## 우선 보완 과제

1.  외부 시스템·Interface Catalog를 공식 기준정보로 관리한다.
2.  외부 호출을 전용 Client Adapter 계층으로 통일한다.
3.  현재 tcf-eai를 내부 Service 호출과 외부 Adapter 공통정책으로 분리한다.
4.  Connect·Read·Overall Timeout과 Remaining Budget을 구현한다.
5.  Retry 가능 오류와 금지 오류 Matrix를 표준화한다.
6.  Circuit Breaker·Bulkhead·Rate Limit 공통기능을 추가한다.
7.  변경 연계에 Idempotency Key·External Request ID를 필수화한다.
8.  Timeout·UNKNOWN 거래의 상태조회 표준을 만든다.
9.  External Call Ledger와 대사 구조를 구현한다.
10.  Outbox·보상·재처리 상태모델을 적용한다.
11.  Callback 인증·중복·순서 검증을 공통화한다.
12.  OAuth·mTLS·HMAC와 Secret Vault 연계를 표준화한다.
13.  외부 응답의 Schema·크기·Content-Type 검증을 추가한다.
14.  GUID·TraceId·외부 거래번호를 통합 Dashboard에 연결한다.
15.  인증서 만료·외부 장기 장애·결과 불명 거래에 대한 운영훈련을 수행한다.

## 중장기 발전 방향

\`\`\`text id=“jtx213” 개별 RestClient 호출 ↓ 표준 External Client Adapter

Connect·Read Timeout ↓ End-to-End Timeout Budget

수동 Retry ↓ 오류 유형 기반 Retry·Jitter

장애 지속 호출 ↓ Circuit·Bulkhead·Rate Limit

단순 변경 호출 ↓ Idempotency·상태조회

로컬 Transaction 중심 ↓ Outbox·Saga·보상

개별 로그 ↓ GUID·외부거래 통합 Trace

수동 대사 ↓ 자동 대사·재처리

설정파일 Secret ↓ Vault·인증서 자동 Rotation

개별 운영 ↓ OM 외부연계 Catalog·Dashboard


\---

\# 마무리말

외부 시스템 호출을 설계하는 과정은 다음 질문에 답하는 일이다.

\`\`\`text id="jtx214"
외부 결과가 즉시 필요한가?

동기·비동기·Callback·파일 중 무엇이 적합한가?

외부 계약과 내부 모델은 분리됐는가?

누가 Endpoint와 인증정보를 관리하는가?

외부 시스템을 얼마나 기다릴 것인가?

Connect·Read·Overall Timeout은 각각 얼마인가?

재시도 가능한 오류는 무엇인가?

해당 요청은 멱등한가?

Retry 횟수와 Backoff는 전체 Timeout 안에 있는가?

외부 장애가 지속되면 Circuit은 언제 열리는가?

Circuit이 열렸을 때 Fallback은 안전한가?

대상별 Thread와 Connection은 격리됐는가?

변경 응답이 유실되면 실제 처리결과를 어떻게 확인하는가?

Idempotency Key와 외부 Request ID는 무엇인가?

Callback 중복과 순서를 어떻게 통제하는가?

로컬 DB와 외부 변경이 불일치하면 어떻게 보상하는가?

내부·외부 처리결과를 어떻게 대사하는가?

외부 오류를 사용자에게 어떻게 안전하게 변환하는가?

GUID와 외부 거래번호로 양쪽 로그를 연결할 수 있는가?

인증서와 Secret을 안전하게 교체할 수 있는가?

외부 시스템이 장시간 중단돼도 NSIGHT 핵심 기능을 보호할 수 있는가?

제24장의 핵심 흐름은 다음과 같다.

text id="jtx215" 연계 목적 정의 ↓ 동기·비동기 방식 선택 ↓ 외부 Contract·Adapter ↓ 인증·Secret ↓ Timeout Budget ↓ Retry·Circuit·Bulkhead ↓ 멱등성·상태조회 ↓ 오류 변환·Trace ↓ Outbox·보상·대사 ↓ 운영 복구

가장 중요한 원칙은 다음과 같다.

\`\`\`text id=“jtx216” 외부 시스템이 정상일 때 호출에 성공하는 것만으로는 부족하다.

외부 시스템이 느리고, 응답이 유실되고, 같은 요청이 반복되고, 장시간 중단되더라도

내부 자원을 보호하고, 데이터 중복과 누락을 방지하며, 실제 처리결과를 설명할 수 있어야

운영 가능한 외부 연계가 된다. \`\`\`
