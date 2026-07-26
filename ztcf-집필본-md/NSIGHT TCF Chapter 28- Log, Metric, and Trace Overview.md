<!-- source: ztcf-집필본/NSIGHT TCF Chapter 28- Log, Metric, and Trace Overview.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제28장. 로그·메트릭·추적

## 도입 전 안내말

제27장에서는 승인된 Commit으로 동일한 Artifact를 생성하고, 환경별 설정을 결합하여 배포한 뒤 Health Check·Smoke Test·운영 지표를 통해 배포 결과를 검증하는 방법을 살펴보았다.

배포가 완료된 이후부터는 시스템의 실제 상태를 지속적으로 확인해야 한다.

운영 중 다음과 같은 문의와 장애가 발생할 수 있다.

\`\`\`text id=“obs28001” 고객조회 버튼을 눌렀는데 화면이 계속 대기한다.

특정 지점에서만 오류가 발생한다.

같은 ServiceId인데 일부 서버에서만 느리다.

Gateway에서는 성공인데 업무 WAR에서는 실패한다.

Tomcat Thread가 부족한지 DB Connection이 부족한지 알 수 없다.

SQL이 느린지 외부 시스템이 느린지 판단하기 어렵다.

사용자가 데이터를 변경했는데 누가 언제 무엇을 바꿨는지 확인할 수 없다.

배포 후 오류율이 증가했지만 어떤 Version 때문인지 알 수 없다.

Timeout이 발생했는데 실제 업무 데이터는 반영돼 있다.

여러 WAR를 호출한 거래 중 어느 구간에서 시간이 소요됐는지 알 수 없다.



이러한 질문에 답하려면 세 가지 수단이 함께 필요하다.

\`\`\`text id="obs28002"
로그
→ 한 사건의 상세한 맥락

메트릭
→ 시스템 상태와 변화의 추세

추적
→ 여러 시스템과 호출구간의 연결관계

세 수단은 서로 대체하지 않는다.

예를 들어 특정 ServiceId의 p95 응답시간이 증가했다는 사실은 메트릭으로 빠르게 확인할 수 있다.

\`\`\`text id=“obs28003” transaction.duration{ serviceId=“SV.Customer.selectSummary” }

p95 1.2초 → 4.8초



그러나 왜 느려졌는지는 메트릭만으로 알기 어렵다.

대표 GUID를 선택해 로그와 추적정보를 확인해야 한다.

\`\`\`text id="obs28004"
GUID
G-20260718-000801

Gateway
180ms

SV STF·Dispatcher
20ms

SV SQL
3,900ms

응답 조립
40ms

이제 병목이 SQL 구간이라는 사실을 판단할 수 있다.

반대로 상세 로그만 있고 메트릭이 없다면 수많은 거래 중 어떤 ServiceId에서 문제가 증가하고 있는지 알기 어렵다.

\`\`\`text id=“obs28005” 로그 수백만 건

질문 오늘 오전 10시 이후 어떤 업무의 Timeout이 증가했는가?



로그를 한 줄씩 검색하는 방식만으로는 운영 상태를 빠르게 판단하기 어렵다.

관측성의 목표는 로그를 많이 생성하는 것이 아니다.

\`\`\`text id="obs28006"
현재 무슨 일이 일어나고 있는가?

어디에서 문제가 발생했는가?

어떤 사용자와 업무가 영향을 받는가?

언제부터 시작됐는가?

배포·설정 변경과 관계가 있는가?

데이터는 정상인가?

임시 복구 후에도 문제가 재발하는가?

에 증거를 가지고 답하는 것이다.

NSIGHT TCF에서는 다음 식별자가 관측성의 중심이 된다.

\`\`\`text id=“obs28007” 화면 ID

이벤트 ID

업무코드

ServiceId

거래코드

GUID

TraceId

SpanId

SQL ID

외부 Interface ID

배포 ID

Instance ID



이 식별자가 서로 연결돼야 다음 추적이 가능하다.

\`\`\`text id="obs28008"
화면 이벤트
↓
ServiceId
↓
Gateway
↓
TCF·STF
↓
Handler·Facade·Service
↓
DAO·Mapper·SQL
↓
DB Table
↓
내부·외부 연계
↓
응답·거래로그·감사로그

화면 이벤트–ServiceId–프로그램–SQL–DB 객체의 연결은 개발 산출물뿐 아니라 장애·성능·감사 추적의 기반이다.

# 문서 개요

## 목적

본 장의 목적은 NSIGHT TCF에서 로그·메트릭·분산추적을 일관된 식별체계로 연결하고, 정상 거래·오류·성능저하·보안사건·운영변경을 재현 가능하게 분석하기 위한 기준을 정의하는 것이다.

세부 목적은 다음과 같다.

\`\`\`text id=“obs28009” 애플리케이션 로그 표준화

거래로그·감사로그·보안로그 책임 분리

GUID·TraceId·SpanId 표준화

Gateway·업무 WAR·내부·외부 연계 추적

MDC 생성·전파·정리

ServiceId 중심 거래 메트릭 정의

JVM·Tomcat·DB Pool·SQL 메트릭 정의

오류율·Timeout·지연 알림 기준 정의

로그 개인정보·Token·Secret 보호

로그 위변조·삭제·접근 통제

로그 보존·압축·파기 기준

배포 Version과 운영 현상 연결

OM Dashboard와 장애대응 절차 연결

자동검증·품질 Gate 구축


\## 적용범위

| 영역 | 적용 대상 |
|---|---|
| Gateway | 진입·인증·라우팅·응답 로그 |
| TCF | STF·Dispatcher·ETF 거래 흐름 |
| 업무 WAR | Handler·Service·Rule·DAO |
| DB | Mapper SQL·Connection Pool |
| 내부 연계 | 호출 ServiceId·하위 GUID·Timeout |
| 외부 연계 | Interface ID·외부 거래번호 |
| 인증 | JWT·SSO·권한 거절 |
| Cache | Hit·Miss·Evict·장애 |
| Batch | Job·Step·Chunk·Checkpoint |
| 파일 | 파일 ID·크기·Hash·처리상태 |
| 배포 | Artifact·Version·Deploy ID |
| 인프라 | JVM·Thread·Heap·GC·CPU |
| 운영관리 | OM 거래로그·감사로그·Dashboard |
| 보안 | 접근·위변조·민감정보 |
| 감사 | 중요 데이터 변경·관리자 행위 |

\## 대상 독자

\`\`\`text id="obs28010"
업무 개발자

프레임워크 개발자

애플리케이션 아키텍트

운영·관제 담당자

보안·감사 담당자

DBA·성능 담당자

DevOps·배포 담당자

QA·장애 테스트 담당자

PMO·서비스 책임자

## 선행조건

\`\`\`text id=“obs28011” 업무코드

ServiceId·거래코드

표준 Header

오류코드

TransactionContext

인증·권한 정책

Timeout 정책

배포 Version

서버·WAR·Instance 식별체계

로그 보존·접근 정책


\---

\# 핵심 관점

\`\`\`text id="obs28012"
로그가 많다고
관측성이 좋은 것은 아니다.

한 거래의 식별자가
모든 계층과 시스템에서 유지되고,

메트릭으로 이상을 발견한 뒤
대표 거래의 로그와 Trace로 원인을 좁히며,

데이터와 운영 상태까지 검증할 수 있어야
실제 관측성이 완성된다.

# 학습 목표

이 장을 마치면 다음 내용을 설명하고 적용할 수 있어야 한다.

| 번호 | 학습 목표 |
| --- | --- |
| 1 | 로그·메트릭·추적의 역할을 구분한다. |
| 2 | 애플리케이션·거래·감사·보안로그를 구분한다. |
| 3 | 운영 로그의 필수 필드를 정의한다. |
| 4 | GUID·TraceId·SpanId의 차이를 설명한다. |
| 5 | ServiceId와 거래코드의 추적 역할을 설명한다. |
| 6 | Gateway에서 최초 Correlation ID를 생성한다. |
| 7 | 클라이언트 Correlation ID의 신뢰정책을 정의한다. |
| 8 | STF에서 MDC를 설정하고 요청 종료 시 제거한다. |
| 9 | 비동기 Thread에 Trace Context를 전달한다. |
| 10 | 내부·외부 호출에 Trace 정보를 전달한다. |
| 11 | Batch·파일·Callback의 추적 ID를 설계한다. |
| 12 | 한 거래의 시작·종료·실패 로그를 연결한다. |
| 13 | 서비스별 TPS·오류율·지연시간을 계측한다. |
| 14 | p50·p95·p99의 의미를 설명한다. |
| 15 | JVM·Thread·DB Pool 메트릭을 해석한다. |
| 16 | Slow SQL과 ServiceId를 연결한다. |
| 17 | Metric Label의 Cardinality 위험을 설명한다. |
| 18 | 증상 기반 Alert와 원인 기반 Alert를 구분한다. |
| 19 | Warning·Critical·지속시간 조건을 정의한다. |
| 20 | Alert Storm과 중복 알림을 방지한다. |
| 21 | 개인정보·Token·Secret의 로그 기록을 차단한다. |
| 22 | 로그 마스킹과 Hash의 차이를 설명한다. |
| 23 | 로그 접근·보존·파기 정책을 적용한다. |
| 24 | 감사로그의 위변조 방지 기준을 설명한다. |
| 25 | Actuator Endpoint의 노출범위를 통제한다. |
| 26 | 운영 장애를 로그·메트릭·Trace로 분석한다. |
| 27 | 관측성 품질 Gate와 테스트를 작성한다. |
| 28 | 배포 Version과 장애 발생시점을 연결한다. |

# 핵심 용어

| 용어 | 의미 |
| --- | --- |
| Observability | 외부에서 수집한 신호로 내부 상태를 설명하는 능력 |
| Log | 특정 사건의 상세 기록 |
| Metric | 시간에 따른 수치형 상태 |
| Trace | 하나의 요청이 거친 전체 호출경로 |
| Span | Trace 안의 하나의 처리구간 |
| Event | 특정 시점에 발생한 상태변화 |
| Correlation ID | 관련 로그를 연결하는 식별자 |
| GUID | NSIGHT 표준 거래 식별자 |
| TraceId | 분산 호출 전체를 묶는 식별자 |
| SpanId | 개별 호출구간 식별자 |
| ParentSpanId | 상위 호출구간 식별자 |
| MDC | Log Thread에 공통 필드를 자동 결합하는 문맥 |
| Transaction Log | 거래 상태·결과·성능을 기록한 로그 |
| Audit Log | 중요 사용자 행위와 변경 사실을 증명하는 로그 |
| Security Log | 인증·인가·위변조·접근통제 기록 |
| Access Log | HTTP 요청·응답·상태·전송량 기록 |
| Application Log | 코드 실행과 기술 예외의 상세 기록 |
| Structured Log | 필드를 Key·Value 또는 JSON으로 기록한 로그 |
| Cardinality | Metric Label 조합 수 |
| Counter | 누적 증가값 |
| Gauge | 현재 상태값 |
| Timer | 호출 건수와 처리시간 분포 |
| Histogram | 값의 구간별 분포 |
| Percentile | 전체 값 중 일정 비율이 이하인 지점 |
| SLI | 실제 측정되는 서비스 수준 지표 |
| SLO | 목표 서비스 수준 |
| Alert | 운영 조치가 필요한 상태의 통보 |
| Sampling | 전체 중 일부 Trace·Log만 저장하는 방식 |
| Retention | 로그·Metric을 보존하는 기간 |
| Legal Hold | 감사·법적 사유로 파기를 중지하는 조치 |

# 로그·메트릭·추적 비교

| 구분 | 로그 | 메트릭 | 추적 |
| --- | --- | --- | --- |
| 핵심 질문 | 무슨 일이 있었는가 | 얼마나 자주·얼마나 느린가 | 어디에서 시간이 걸렸는가 |
| 데이터 형태 | Text·JSON | 숫자 시계열 | Span Tree |
| 검색 기준 | GUID·ServiceId·오류코드 | 시간·Label | TraceId |
| 장점 | 상세 원인 | 빠른 추세·알림 | 구간별 지연 |
| 단점 | 저장량·검색비용 | 상세 맥락 부족 | 계측·저장비용 |
| 대표 도구 | Logback·검색시스템 | Actuator·Micrometer | OpenTelemetry |
| 보존 | 상대적으로 길거나 계층화 | 집계 중심 | Sampling 중심 |

# 목표 관측성 아키텍처

text id="obs28013" 사용자·전용 브라우저 │ │ X-Correlation-Id·traceparent ▼ L4·Apache │ ▼ Gateway ├─ Access Log ├─ 인증·라우팅 로그 ├─ Gateway 거래로그 ├─ Trace Root Span └─ HTTP Metric │ ▼ 업무 WAR ├─ TCF·STF·ETF 로그 ├─ Transaction Log ├─ Audit Log ├─ ServiceId Metric ├─ JVM·Tomcat·Pool Metric └─ Business Span │ ├─ Mapper·SQL Span ├─ 내부 Service Span └─ 외부 Interface Span │ ▼ ┌────────────────────────────────────────────┐ │ 관측성 저장·분석 계층 │ │ │ │ Log Store │ │ Metric Store │ │ Trace Store │ │ Audit Store │ │ OM Dashboard │ │ Alert Manager │ └────────────────────────────────────────────┘

# 현재 구현과 목표 구조

## 현재 기준 소스에서 확인되는 기능

현재 기준 소스에는 다음 관측성 기능이 존재한다.

| 영역 | 확인된 기능 |
| --- | --- |
| STF | GUID·TraceId 생성 |
| MDC | guid, traceId, serviceId, userId, branchId 설정 |
| Context 정리 | TCF finally와 Web Filter에서 MDC·인증문맥 제거 |
| 거래 시작 | TX\_START 애플리케이션 로그 |
| 거래 종료 | TX\_END와 처리시간 |
| 거래로그 DB | TCF\_TX\_LOG 저장 |
| 거래로그 필드 | 업무·ServiceId·거래코드·GUID·TraceId·사용자·결과·시간 |
| 감사로그 | audit.log Logger |
| Gateway | Gateway 거래로그 DB 저장 |
| OM | 거래로그·감사로그 조회 |
| Logback | MDC 포함 공통 Pattern |
| Rolling File | 크기·일자 기반 회전 |
| Actuator | Health·Info·Metrics·Prometheus·Thread Dump 설정 |
| AP Metric 수집 | CPU·Heap·Live Thread |
| DB Metric 수집 | Hikari Active·Max·Pool 사용률 |
| 배포 Metric | Health·Version·Process Start Time |
| OM Runtime | CPU·Heap·Thread·DB Pool Snapshot |

## 현재 Logback 설정

현재 공통 Log Pattern은 다음 항목을 포함한다.

\`\`\`text id=“obs28014” 시간

Level

Thread

GUID

TraceId

ServiceId

UserId

BranchId

Logger

Message



현재 File Rolling 예:

\`\`\`text id="obs28015"
파일 최대 크기
50MB

보존
14일

전체 크기 상한
500MB

이 값은 개발·현재 설정값이며 운영 거래량과 감사 요구를 기반으로 재산정해야 한다.

## 현재 거래로그 구조

현재 거래 종료 시 저장되는 대표 필드는 다음과 같다.

\`\`\`text id=“obs28016” logId

txTime

businessCode

serviceId

transactionCode

guid

traceId

userId

branchId

resultStatus

resultCode

errorCode

elapsedTimeMs



거래로그가 일반 애플리케이션 로그와 분리되고 ServiceId·결과·처리시간을 구조화해 보관하는 방향은 적절하다. 거래로그·감사로그·애플리케이션 로그는 서로 목적이 다르므로 하나의 로그로 통합해서는 안 된다.

\## 현재 구현의 주요 보완점

| 항목 | 현재 상태 | 목표 판단 |
|---|---|---|
| Correlation ID 생성 | STF Header 검증 이후 | Gateway·Ingress에서 우선 생성 |
| Client ID 신뢰 | 명확한 정책 보완 필요 | 허용 형식·재생성·원 ID 보존 |
| SpanId | 구현 확인 안 됨 | 분산추적 필수 |
| \`traceparent\` | 구현 확인 안 됨 | OpenTelemetry 표준 검토 |
| 거래 시작 DB 저장 | 시작 시 Console Log만 | START 상태 INSERT·종료 UPDATE 검토 |
| Process 강제종료 | 종료 로그 미저장 가능 | 미완료 거래 탐지 필요 |
| 거래로그 저장 | 종료 시 별도 Auto Commit | 별도 전용 DataSource 권장 |
| 감사로그 | 기본 거래단위 항목 중심 | 변경 전후·대상·사유 확대 |
| Metric | \`TCF\_METRIC\` Debug 로그 | 실제 Micrometer Counter·Timer 필요 |
| Prometheus | Endpoint 설정 존재 | Registry 의존성과 보안 확인 필요 |
| Gateway Trace | Body에서 GUID 추출 가능 | Header 우선·Gateway 생성 필요 |
| 비동기 Context | 공통 전파기 확인 필요 | Executor TaskDecorator |
| SQL Trace | SQL ID 표준은 존재 | ServiceId·Span 연결 보완 |
| 로그 형식 | Key·Value Text | 중앙수집용 JSON 검토 |
| \`System.out\` | TCF·STF·ETF 등에 다수 | 운영 전 제거 |
| 요청·응답 출력 | 전체 Body 출력 코드 존재 | 개인정보 위험, 운영 금지 |
| MDC 정리 | 이중 정리 구현 | 유지·비동기 누수 테스트 |
| Actuator 노출 | 다수 Endpoint 설정 | 내부망·인증·Port 분리 |
| 로그 전체 삭제 | OM 기능 존재 | 보존정책·이중승인·감사 강화 |
| 로그 무결성 | 별도 구현 확인 필요 | WORM·Hash Chain 검토 |
| OpenTelemetry | 구현 확인 안 됨 | 중장기 적용 |
| Sampling | 정책 없음 | 정상·오류 구분 정책 필요 |

\## 핵심 Gap 1: 시작 거래의 영속성

현재 거래로그 DB 저장은 주로 거래 종료 시 수행된다.

\`\`\`text id="obs28017"
STF
→ TX\_START 일반 로그

ETF
→ TX\_END
→ DB 거래로그 저장

다음 상황에서는 DB 거래로그가 남지 않을 수 있다.

\`\`\`text id=“obs28018” JVM 강제종료

서버 전원 장애

Process Kill

OutOfMemory

ETF 이전 치명적 오류



목표 대안:

\`\`\`text id="obs28019"
STF
→ START Row INSERT

ETF
→ 동일 Row SUCCESS·FAIL UPDATE

또는:

\`\`\`text id=“obs28020” TX\_START Event Append

TX\_END Event Append

미완료 Pair 탐지



시작·종료 방식은 거래량과 로그 DB 부하를 고려해 결정한다.

\## 핵심 Gap 2: 실제 메트릭 계측

현재 \`TransactionMetricService\`는 다음 성격에 가깝다.

\`\`\`text id="obs28021"
TCF\_METRIC
serviceId
resultCode
elapsedMs

→ Debug Log

이것만으로는 다음을 효율적으로 계산하기 어렵다.

\`\`\`text id=“obs28022” ServiceId별 TPS

오류율

p95·p99

Timeout 증가율

Instance별 성능

배포 전후 비교



목표:

\`\`\`java id="obs28023"
Timer.builder("tcf.transaction.duration")
.tag("businessCode", businessCode)
.tag("serviceId", normalizedServiceId)
.tag("result", resultType)
.register(meterRegistry)
.record(elapsed, TimeUnit.MILLISECONDS);

## 핵심 Gap 3: System.out과 전문 출력

현재 진단 목적의 System.out과 Request·Response Body 출력은 개발 중 호출경로 확인에는 도움이 된다.

운영에서는 다음 문제를 유발한다.

\`\`\`text id=“obs28024” 개인정보 노출

Token·업무데이터 노출

로그 용량 폭증

Level 제어 불가

구조화 검색 불가

중복 출력

성능 저하



운영 전 구조화 SLF4J Logging과 마스킹 정책으로 교체해야 한다.

\---

\# 설계 원칙

\## 원칙 1. 하나의 사건은 하나의 식별체계로 연결한다

\`\`\`text id="obs28025"
화면 이벤트

ServiceId

GUID

TraceId

SpanId

SQL ID

외부 Request ID

를 서로 변환하거나 연결할 수 있어야 한다.

## 원칙 2. 로그는 결과가 아니라 증거다

사용자 응답과 로그의 목적을 분리한다.

\`\`\`text id=“obs28026” 사용자 → 이해 가능한 안전한 메시지

운영자 → 오류코드·실패단계·원인 예외

감사자 → 사용자·대상·행위·결과


\## 원칙 3. 메트릭 Label은 제한한다

금지 Label:

\`\`\`text id="obs28027"
guid

traceId

userId

customerNo

accountNo

fileName

errorMessage

이 값은 조합 수가 매우 커 Metric 저장소를 고갈시킬 수 있다.

## 원칙 4. 오류 거래는 Sampling하지 않는다

정상 Trace는 일부 Sampling할 수 있다.

\`\`\`text id=“obs28028” 정상 거래 → 1%·5% 등 Sampling

오류·Timeout → 100% 보존

중요 변경 → 100% 감사


\## 원칙 5. 로그 실패가 업무를 무조건 실패시키지는 않는다

일반 거래로그 저장 실패:

\`\`\`text id="obs28029"
업무 성공 유지

로그 실패 Metric·Alert

Fallback File·Queue 검토

중요 감사로그 저장 실패:

\`\`\`text id=“obs28030” 중요 개인정보 원문 조회

권한 변경

관리자 강제종료

감사 저장 실패 시 → 거래 차단 검토



로그 종류별 Fail Open·Fail Closed 정책을 분리한다.

\---

\# 28.1 운영에서 필요한 로그

\## 28.1.1 로그 종류

| 로그 | 목적 | 대표 항목 |
|---|---|---|
| Access Log | HTTP 진입 확인 | URI·Status·Byte·시간 |
| Gateway Log | 인증·라우팅 | Target·Phase·HTTP Status |
| Application Log | 코드 실행·예외 분석 | Class·Method·Cause |
| Transaction Log | 거래 상태·성능 | ServiceId·결과·시간 |
| Audit Log | 중요 행위 증명 | 사용자·대상·변경 전후 |
| Security Log | 보안사건 분석 | 인증 실패·권한 거절 |
| SQL Log | DB 성능 분석 | Mapper ID·SQL ID·시간 |
| Integration Log | 시스템 연계 | Target·Interface ID·Timeout |
| Batch Log | 대량처리 상태 | Job·Step·Chunk·건수 |
| File Log | 파일 처리 | File ID·Hash·크기 |
| Deployment Log | 변경 추적 | Version·Deploy ID·Instance |
| Configuration Log | 설정 변경 | Config Version·변경자 |

\---

\## 28.1.2 애플리케이션 로그

애플리케이션 로그는 개발자와 운영자가 기술적인 실행상태를 확인하기 위한 로그다.

좋은 예:

\`\`\`text id="obs28031"
event=CAMPAIGN\_UPDATE\_FAILED
guid=G-20260718-000801
serviceId=CM.Campaign.update
campaignIdHash=7AC3...
stage=UPDATE\_HISTORY
errorCode=E-COM-DB-0001

나쁜 예:

\`\`\`text id=“obs28032” 오류 발생

처리 실패

여기 들어옴

값 확인: {전체 요청 Body}


\---

\## 28.1.3 거래로그

거래로그는 한 ServiceId의 시작·종료·결과·성능을 기록한다.

필수 항목:

| 분류 | 항목 |
|---|---|
| 식별 | logId·GUID·TraceId |
| 업무 | businessCode·serviceId·transactionCode |
| 사용자 | userId·branchId·channelId |
| 시스템 | host·instanceId·WAR·version |
| 시간 | requestTime·startTime·endTime |
| 결과 | status·resultCode·errorCode |
| 성능 | elapsedMs |
| 트랜잭션 | commit·rollback·unknown |
| 연계 | parentServiceId·targetSystem |
| 배포 | artifactVersion·configVersion |

\---

\## 28.1.4 거래로그 상태

\`\`\`text id="obs28033"
STARTED

SUCCESS

BUSINESS\_FAIL

SYSTEM\_FAIL

TIMEOUT

CANCELLED

UNKNOWN

ABORTED

FAIL 하나만 사용하면 실패 의미를 구분하기 어렵다.

## 28.1.5 감사로그

감사로그는 단순 거래 실행 사실보다 구체적인 중요 행위를 기록한다.

대표 대상:

\`\`\`text id=“obs28034” 개인정보 원문 조회

대량 다운로드

고객정보 변경

캠페인 승인

데이터 삭제·복구

권한 부여·회수

사용자 잠금·해제

Token 강제 폐기

배포 승인·Rollback

운영 설정 변경

로그 조회·삭제


\---

\## 28.1.6 감사로그 필수 항목

\`\`\`text id="obs28035"
auditId

eventTime

eventType

userId

actorType

branchId

sourceIp

serviceId

targetType

targetIdHash

action

beforeValue

afterValue

reason

approvalId

resultStatus

guid

traceId

변경 전후 값을 저장할 때 개인정보를 그대로 저장하지 않는다.

\`\`\`text id=“obs28036” 전체 객체 → 저장 금지

승인된 주요 변경 필드 → 마스킹·요약 저장


\---

\## 28.1.7 보안로그

대표 이벤트:

\`\`\`text id="obs28037"
LOGIN\_SUCCESS

LOGIN\_FAIL

JWT\_EXPIRED

JWT\_SIGNATURE\_FAIL

JWT\_HEADER\_MISMATCH

PERMISSION\_DENIED

DIRECT\_WAR\_ACCESS

REFRESH\_REUSE

SECRET\_ACCESS\_FAIL

ADMIN\_FORCE\_LOGOUT

보안로그에는 공격 분석에 필요한 Source IP·User-Agent Hash·인증 실패사유를 기록하되 Token 원문은 기록하지 않는다.

## 28.1.8 SQL 로그

SQL 원문 전체를 항상 기록하지 않는다.

권장 필드:

\`\`\`text id=“obs28038” guid

traceId

serviceId

mapperNamespace

sqlId

databaseId

elapsedMs

rowCount

timeoutMs

result

exceptionType



Parameter는 기본적으로 기록하지 않는다.

필요한 경우:

\`\`\`text id="obs28039"
Parameter 개수

타입

Hash

범위

마스킹된 업무 키

만 기록한다.

## 28.1.9 내부·외부 연계 로그

\`\`\`text id=“obs28040” callerServiceId

targetServiceId·interfaceId

targetSystem

callType

attempt

elapsedMs

httpStatus

providerResultCode

timeoutStage

circuitState

externalTransactionId

result



상위 GUID와 TraceId를 반드시 유지한다.

\---

\## 28.1.10 Batch 로그

\`\`\`text id="obs28041"
jobInstanceId

jobName

businessDate

stepName

chunkNo

readCount

writeCount

skipCount

retryCount

checkpoint

status

startedAt

endedAt

Scheduler 실행 성공과 Batch 업무 처리 성공을 구분한다.

## 28.1.11 배포로그

\`\`\`text id=“obs28042” deployId

artifactVersion

commitId

configVersion

instanceId

previousVersion

targetVersion

startTime

endTime

healthResult

smokeResult

result



배포 전후의 오류율과 p95를 비교할 수 있어야 한다.

\---

\## 28.1.12 로그 Level

| Level | 사용 기준 |
|---|---|
| ERROR | 운영 조치가 필요한 시스템 실패 |
| WARN | 업무는 지속되나 위험·저하 존재 |
| INFO | 중요 상태변화·거래 요약 |
| DEBUG | 개발·상세 진단 |
| TRACE | 제한된 심층 분석 |

금지:

\`\`\`text id="obs28043"
정상 거래마다 ERROR

업무상 고객 없음마다 Stack Trace

모든 Request Body INFO

운영 전체 SQL DEBUG

## 28.1.13 Stack Trace 기록

원칙:

\`\`\`text id=“obs28044” 원인 예외는 보존한다.

최종 처리경계에서 한 번 기록한다.

같은 예외를 계층마다 반복 출력하지 않는다.



예:

\`\`\`java id="obs28045"
log.error(
"TCF system failure. serviceId={} stage={}",
serviceId,
stage,
exception
);

## 28.1.14 구조화 로그

Key·Value:

text id="obs28046" event=TX\_END guid=G-001 serviceId=SV.Customer.selectSummary result=SUCCESS elapsedMs=182

JSON:

json id="obs28047" { "timestamp": "2026-07-18T18:00:00.123+09:00", "level": "INFO", "event": "TX\_END", "guid": "G-001", "traceId": "T-001", "serviceId": "SV.Customer.selectSummary", "result": "SUCCESS", "elapsedMs": 182, "instanceId": "sv-was-01", "version": "1.4.2" }

중앙 로그 수집을 위해 JSON 형식을 검토한다.

## 28.1.15 시간

모든 서버는 시간 동기화가 필요하다.

\`\`\`text id=“obs28048” NTP

Timezone

Offset

Timestamp 정밀도



권장:

\`\`\`text id="obs28049"
저장·전송
→ ISO-8601 Offset 또는 UTC

화면·운영
→ Asia/Seoul 변환

서버 시간이 다르면 Trace 순서가 뒤바뀔 수 있다.

## 28.1.16 로그 실패 정책

| 로그 | 실패 시 정책 |
| --- | --- |
| Application File | 업무 지속·경보 |
| 일반 거래로그 | 업무 지속·경보·Fallback |
| 중요 감사로그 | Fail Closed 검토 |
| 보안로그 | 중요행위 차단 검토 |
| Metric | 업무 지속·수집 실패 경보 |
| Trace Export | 업무 지속·Queue·Drop 정책 |

## 28.1.17 거래로그 저장소

현재처럼 업무 Transaction과 분리된 거래로그 저장은 업무 Rollback 이후에도 실패 사실을 남길 수 있다는 장점이 있다.

주의:

\`\`\`text id=“obs28050” 업무 DataSource Connection의 autoCommit 값을 임시 변경하면

Transaction Context와 Pool 상태에 영향을 줄 수 있다.



운영 권장:

\`\`\`text id="obs28051"
거래로그 전용 DataSource

또는 비동기 Log Queue

또는 Append 전용 저장소

거래량·유실 허용범위·장애정책으로 결정한다.

# 28.2 Correlation ID와 거래 추적

## 28.2.1 식별자 구분

| 식별자 | 범위 |
| --- | --- |
| GUID | NSIGHT 논리 거래 |
| TraceId | 전체 분산 호출 |
| SpanId | 하나의 처리구간 |
| ParentSpanId | 상위 Span |
| Request ID | 특정 HTTP 요청 |
| Idempotency Key | 동일 변경 요청 |
| External Transaction ID | 외부기관 거래 |
| Job Instance ID | Batch 실행 |
| Deploy ID | 배포 실행 |

서로 목적이 다르므로 하나의 ID로 모두 대체하지 않는다.

## 28.2.2 GUID 생성 위치

목표:

text id="obs28052" Gateway·Ingress → GUID 존재 확인 → 없으면 생성 → 형식 검증 → 내부 Header 전달

업무 STF에 도달하기 전에 다음 오류가 발생할 수 있기 때문이다.

\`\`\`text id=“obs28053” JSON Parsing 오류

Header Validation 오류

JWT 오류

Route 오류

Request 크기 초과



이 오류에도 Correlation ID가 있어야 한다.

\---

\## 28.2.3 클라이언트가 GUID를 보낸 경우

대안:

\### 신뢰하고 유지

내부 신뢰 Client에만 제한할 수 있다.

\### 새 GUID 생성

외부 Client가 보낸 값은 \`clientRequestId\`로 별도 보존한다.

권장:

\`\`\`text id="obs28054"
외부 Client ID
→ clientRequestId

서버 생성 ID
→ guid·traceId

이유:

\`\`\`text id=“obs28055” 형식 오류

중복 ID

로그 주입

지나치게 긴 값

다른 사용자의 ID 재사용


\---

\## 28.2.4 ID 형식

예:

\`\`\`text id="obs28056"
GUID
G-20260718-000801-7A2F

TraceId
32자리 Hex

SpanId
16자리 Hex

요구사항:

\`\`\`text id=“obs28057” 충분한 유일성

길이 제한

허용문자

로그 안전성

시스템 간 호환


\---

\## 28.2.5 Trace 구조

\`\`\`text id="obs28058"
TraceId=T-001

Span A
Gateway
│
└─ Span B
SV Transaction
│
├─ Span C
│ SV SQL
│
└─ Span D
IC Internal Call
│
└─ Span E
IC SQL

모든 Span은 같은 TraceId를 사용한다.

각 구간은 다른 SpanId를 사용한다.

## 28.2.6 표준 전파 Header

내부 표준 Header:

json id="obs28059" { "guid": "G-20260718-000801", "traceId": "8d7a...", "spanId": "12ab...", "parentSpanId": "45cd...", "serviceId": "SV.Customer.selectSummary", "callerServiceId": "IC.Customer.selectIntegratedProfile", "callDepth": 1 }

HTTP 표준:

http id="obs28060" traceparent: 00-{traceId}-{spanId}-01 tracestate: nsight=... X-Correlation-Id: G-20260718-000801

## 28.2.7 Gateway 추적

Gateway는 다음을 기록한다.

\`\`\`text id=“obs28061” 진입시각

인증 결과

Route 선택

Target URL

HTTP Status

업무 결과코드

처리시간

실패 Phase



현재 Gateway 거래로그에는 업무코드·ServiceId·거래코드·GUID·TraceId·Target URL·HTTP Status·결과·처리시간을 기록할 수 있는 구조가 있다.

목표 보완:

\`\`\`text id="obs28062"
Gateway SpanId

Target Instance

Retry Count

Connection Time

Response Time

전송 Byte

인증 실패 사유

## 28.2.8 STF와 MDC

STF에서 검증된 Header를 이용해 MDC를 설정한다.

java id="obs28063" MDC.put("guid", header.getGuid()); MDC.put("traceId", header.getTraceId()); MDC.put("serviceId", header.getServiceId()); MDC.put("userId", maskedUserId); MDC.put("branchId", header.getBranchId()); MDC.put("instanceId", instanceId); MDC.put("version", version);

MDC는 업무 코드가 매 로그 호출마다 GUID를 직접 전달하지 않아도 공통 Pattern에 자동 포함되게 한다.

## 28.2.9 MDC 정리

Tomcat Thread는 재사용된다.

MDC를 제거하지 않으면 다음 사용자 요청 로그에 이전 GUID가 남을 수 있다.

\`\`\`text id=“obs28064” Thread http-nio-8080-exec-10

요청 A userId=U001

MDC 미정리

요청 B 로그에 userId=U001 표시



따라서 다음 두 경계에서 정리하는 현재 방향은 적절하다.

\`\`\`text id="obs28065"
TCF finally

Web Filter finally

다만 테스트로 실제 누수가 없는지 검증해야 한다.

## 28.2.10 비동기 Thread

MDC는 기본적으로 다른 Thread로 자동 전달되지 않는다.

\`\`\`text id=“obs28066” HTTP Thread ↓ Executor Thread

MDC 없음



대안:

\`\`\`text id="obs28067"
TaskDecorator

Context Snapshot

OpenTelemetry Context

명시적 TransactionContext 전달

실행 후 반드시 정리한다.

## 28.2.11 Timeout Executor

TCF 거래를 별도 Executor에서 실행하면 다음 문맥을 함께 전달해야 한다.

\`\`\`text id=“obs28068” GUID

TraceId

MDC

AuthenticationContext

Timeout Deadline

ServiceId



전파 누락 시 Timeout Thread의 로그에 식별자가 없거나 다른 거래 문맥이 남을 수 있다.

\---

\## 28.2.12 내부 업무 연계

\`\`\`text id="obs28069"
IC
→ SV

전파:

\`\`\`text id=“obs28070” GUID 동일 유지

TraceId 동일 유지

SpanId 신규 생성

ParentSpanId IC Span

callerServiceId IC ServiceId

targetServiceId SV ServiceId


\---

\## 28.2.13 외부 연계

외부 시스템이 Trace 표준을 지원하지 않더라도 다음을 전달한다.

\`\`\`text id="obs28071"
X-Correlation-Id

NSIGHT Request ID

외부 계약상 거래번호

저장:

text id="obs28072" GUID ↔ NSIGHT Request ID ↔ External Transaction ID

## 28.2.14 비동기 메시지

메시지 Header:

\`\`\`text id=“obs28073” eventId

correlationId

causationId

traceId

parentSpanId

producerServiceId

occurredAt

schemaVersion



\`causationId\`는 어떤 사건 때문에 현재 사건이 발생했는지를 나타낸다.

\---

\## 28.2.15 Callback

외부 Callback은 최초 요청과 다른 HTTP 거래다.

\`\`\`text id="obs28074"
최초 요청 GUID
G-001

외부 Callback GUID
G-002

correlationId
G-001

신규 요청 식별자와 원 업무 식별자를 모두 유지한다.

## 28.2.16 Batch

\`\`\`text id=“obs28075” Job Instance ID

Execution ID

Step ID

Chunk ID

Item Business Key

원 이벤트 Correlation ID



한 Batch Job의 모든 Item에 같은 GUID만 사용하면 특정 실패 Item을 구분하기 어렵다.

\---

\## 28.2.17 화면 추적

화면에서 사용자 문의가 들어올 경우 다음 정보가 필요하다.

\`\`\`text id="obs28076"
화면 ID

이벤트 ID

사용자 수행시각

표시된 GUID

오류코드

오류 화면에 GUID를 표시하면 운영자가 거래를 빠르게 찾을 수 있다.

## 28.2.18 정방향 추적

text id="obs28077" 화면 ID ↓ 이벤트 ID ↓ ServiceId ↓ Handler ↓ Facade ↓ Service ↓ DAO ↓ Mapper SQL ID ↓ Table

## 28.2.19 역방향 추적

text id="obs28078" Slow Table·SQL ID ↓ Mapper ↓ DAO ↓ Service ↓ ServiceId ↓ 화면 이벤트 ↓ 사용자 영향

ServiceId와 SQL·화면을 함께 연결해야 성능·장애 영향분석이 가능하다.

## 28.2.20 분산추적 도입

목표 대안:

\`\`\`text id=“obs28079” OpenTelemetry SDK

Java Agent

OTLP Exporter

Trace Collector

Trace Backend



우선 적용 대상:

\`\`\`text id="obs28080"
Gateway

TCF Online

내부 HTTP Client

외부 HTTP Client

MyBatis·JDBC

Batch

Message Producer·Consumer

## 28.2.21 Trace Sampling

예:

\`\`\`text id=“obs28081” 정상 거래 5%

Slow 거래 100%

오류 거래 100%

중요 관리자 거래 100%



Tail Sampling은 결과를 확인한 뒤 오류·Slow Trace를 우선 보존할 수 있다.

\---

\# 28.3 핵심 메트릭과 알림

\## 28.3.1 메트릭 설계 관점

온라인 서비스는 RED 관점을 사용할 수 있다.

\`\`\`text id="obs28082"
Rate
요청량

Errors
오류율

Duration
처리시간

자원은 USE 관점을 사용할 수 있다.

\`\`\`text id=“obs28083” Utilization 사용률

Saturation 대기·포화

Errors 자원 오류


\---

\## 28.3.2 거래 메트릭

\`\`\`text id="obs28084"
tcf.transaction.count

tcf.transaction.duration

tcf.transaction.error.count

tcf.transaction.timeout.count

tcf.transaction.inflight

권장 Label:

\`\`\`text id=“obs28085” businessCode

serviceId

resultType

instanceId

channelId

version



주의:

\`\`\`text id="obs28086"
ServiceId 수가 지나치게 많아지면
Metric Series도 증가한다.

폐기 ServiceId를 Metric Label에서 계속 유지하지 않도록 관리한다.

## 28.3.3 결과 유형

\`\`\`text id=“obs28087” SUCCESS

BUSINESS\_FAIL

SYSTEM\_FAIL

TIMEOUT

CONTROLLED

AUTH\_FAIL

UNKNOWN



\`resultCode\` 전체를 Label로 사용하면 코드 수가 많아질 수 있다.

상위 \`resultType\`은 Label로 사용하고 상세 오류코드는 로그·거래로그에서 확인하는 방식을 검토한다.

\---

\## 28.3.4 응답시간

\`\`\`text id="obs28088"
평균
→ 전체 경향

p50
→ 일반 사용자의 중앙값

p95
→ 대부분 사용자의 상한

p99
→ Tail 지연

평균만 보면 일부 매우 느린 거래를 놓칠 수 있다.

## 28.3.5 현재 프로젝트 목표 예

현재 프로젝트에서 자주 사용하는 운영 목표 예:

\`\`\`text id=“obs28089” 온라인 응답 p95 3초 이내

Tomcat Busy Thread 70% 이하 권장

Heap 사용률 70% 이하 안정영역

DB Pool 사용률 70~80% 이하 권장

Connection Pending 지속 발생 금지



이 값은 모든 거래에 동일하게 적용하는 절대값이 아니라 프로젝트 Baseline과 거래등급에 따라 확정한다.

\---

\## 28.3.6 JVM 메트릭

\`\`\`text id="obs28090"
process.cpu.usage

system.cpu.usage

jvm.memory.used

jvm.memory.max

jvm.gc.pause

jvm.gc.memory.promoted

jvm.threads.live

jvm.threads.peak

jvm.classes.loaded

process.uptime

## 28.3.7 Heap 해석

\`\`\`text id=“obs28091” Heap 사용률 상승 + GC 후 회복 → 정상 가능

Heap 사용률 지속 상승 + Full GC 증가 + 회복 안 됨 → Memory Leak 의심



한 시점의 Heap 값만으로 장애를 판단하지 않는다.

\---

\## 28.3.8 GC 메트릭

확인:

\`\`\`text id="obs28092"
Pause Count

Pause Time

Young GC

Old·Full GC

Allocation Rate

Promotion Rate

GC Pause 증가가 p95 지연과 같은 시점에 발생하는지 비교한다.

## 28.3.9 Tomcat Thread

\`\`\`text id=“obs28093” tomcat.threads.current

tomcat.threads.busy

tomcat.threads.config.max

tomcat.connections.current

tomcat.connections.keepalive.current



판단:

\`\`\`text id="obs28094"
Busy Thread 높음
\+ CPU 낮음
→ DB·외부 대기 가능성

Busy Thread 높음
\+ CPU 높음
→ 계산·GC·Loop 가능성

## 28.3.10 DB Pool

\`\`\`text id=“obs28095” hikaricp.connections.active

hikaricp.connections.idle

hikaricp.connections.pending

hikaricp.connections.max

hikaricp.connections.timeout

hikaricp.connections.acquire



현재 \`tcf-batch\`와 OM 지원코드는 Actuator를 이용해 Hikari Active와 Max를 조회하고 사용률을 계산할 수 있다.

보완:

\`\`\`text id="obs28096"
Pending

Acquire Time

Timeout Count

Connection Creation

Leak Detection

## 28.3.11 SQL 메트릭

\`\`\`text id=“obs28097” sql.execution.count

sql.execution.duration

sql.timeout.count

sql.rows.read

sql.rows.affected

sql.error.count



Label:

\`\`\`text id="obs28098"
businessCode

serviceId

mapperId

sqlId

result

SQL 원문을 Label로 사용하지 않는다.

## 28.3.12 내부·외부 연계 메트릭

\`\`\`text id=“obs28099” integration.call.count

integration.call.duration

integration.call.timeout.count

integration.call.retry.count

integration.call.circuit.open

integration.call.bulkhead.rejected



Label:

\`\`\`text id="obs28100"
callerBusiness

targetSystem

interfaceId

resultType

## 28.3.13 Cache 메트릭

\`\`\`text id=“obs28101” cache.get.count

cache.hit.count

cache.miss.count

cache.put.count

cache.evict.count

cache.load.duration

cache.error.count



Hit Rate만 높다고 좋은 Cache는 아니다.

\`\`\`text id="obs28102"
오래된 데이터가 계속 Hit
→ 높은 Hit Rate
→ 잘못된 결과

정합성 오류와 Evict 실패도 함께 확인한다.

## 28.3.14 Batch 메트릭

\`\`\`text id=“obs28103” batch.job.started

batch.job.completed

batch.job.failed

batch.item.read

batch.item.write

batch.item.skip

batch.retry

batch.duration

batch.queue.lag


\---

\## 28.3.15 업무 메트릭

기술 메트릭만으로 서비스 상태를 설명하기 어렵다.

예:

\`\`\`text id="obs28104"
고객조회 성공 건수

캠페인 승인 건수

발송 요청 건수

대량 다운로드 건수

중복 요청 건수

보상 처리 건수

UNKNOWN 거래 건수

업무 메트릭은 개인정보를 Label로 사용하지 않는다.

## 28.3.16 배포 메트릭

\`\`\`text id=“obs28105” deployment.info{ version=“1.4.2”, commit=“a1b2c3d”, configVersion=“CFG-003” }

deployment.success.count

deployment.rollback.count



장애 발생시점과 배포시점을 Dashboard에 함께 표시한다.

\---

\## 28.3.17 Actuator Endpoint

현재 여러 모듈 설정에는 다음 Endpoint 노출이 포함돼 있다.

\`\`\`text id="obs28106"
health

info

metrics

prometheus

threaddump

주의:

text id="obs28107" 설정에 이름이 있다고 실제 Endpoint와 Exporter가 모두 동작한다고 단정할 수 없다.

실제 Actuator·Micrometer Registry 의존성, 보안 설정과 네트워크 접근을 검증해야 한다.

## 28.3.18 Actuator 보안

공개 허용 후보:

\`\`\`text id=“obs28108” Liveness

Readiness



내부 제한:

\`\`\`text id="obs28109"
metrics

prometheus

threaddump

heapdump

env

configprops

loggers

특히 다음 Endpoint는 민감정보와 내부 구조를 노출할 수 있다.

\`\`\`text id=“obs28110” env

heapdump

threaddump

mappings

beans



별도 관리 Port·내부망·인증·방화벽을 적용한다.

\---

\## 28.3.19 알림 원칙

좋은 알림:

\`\`\`text id="obs28111"
사용자 영향이 있다.

운영자가 조치할 수 있다.

담당 조직이 명확하다.

Runbook이 있다.

중복이 억제된다.

나쁜 알림:

\`\`\`text id=“obs28112” CPU가 1초 동안 81%였다.

단일 거래가 3초를 넘었다.

INFO 로그에 특정 문자열이 나왔다.


\---

\## 28.3.20 증상 기반 Alert

사용자 영향에 가까운 지표:

\`\`\`text id="obs28113"
오류율

Timeout율

p95·p99

가용성

거래 성공률

Queue 지연

원인 후보 지표:

\`\`\`text id=“obs28114” CPU

Heap

GC

Thread

Pool

SQL



일반적으로 증상 Alert를 우선하고 원인 Alert를 보조로 사용한다.

\---

\## 28.3.21 Alert 등급

| 등급 | 의미 | 대응 |
|---|---|---|
| INFO | 상태변경 | 기록 |
| WARNING | 저하 가능성 | 근무시간 점검 |
| MAJOR | 사용자 영향 확대 | 즉시 분석 |
| CRITICAL | 핵심 기능 중단 | 장애 선언 |

\---

\## 28.3.22 지속시간 조건

예:

\`\`\`text id="obs28115"
DB Pool 사용률 80% 이상
5분 지속
→ WARNING

95% 이상
1분 지속
→ CRITICAL

짧은 순간 Peak와 지속 포화를 구분한다.

## 28.3.23 복합 Alert

\`\`\`text id=“obs28116” Busy Thread > 80%

AND

DB Pool Pending > 0

AND

p95 > 3초



이면 DB Connection 대기 가능성이 높다.

복합조건은 오탐을 줄일 수 있다.

\---

\## 28.3.24 Alert Storm 방지

\`\`\`text id="obs28117"
동일 원인 Alert Grouping

상위·하위 Alert Suppression

Cooldown

Deduplication

Maintenance Window

배포시간 Mute 정책

배포 중 모든 Alert를 무조건 끄지 않는다.

핵심 오류·가용성 Alert는 유지한다.

## 28.3.25 Alert와 Runbook

모든 Critical Alert에는 다음이 연결돼야 한다.

\`\`\`text id=“obs28118” Alert ID

설명

영향

확인 Dashboard

조회 Query

대표 로그 키워드

임시 복구

에스컬레이션

종료조건


\---

\# 28.4 개인정보와 로그 보안

\## 28.4.1 로그는 개인정보 저장소가 될 수 있다

Request·Response를 그대로 출력하면 로그에 다음 정보가 쌓일 수 있다.

\`\`\`text id="obs28119"
주민등록번호

계좌번호

전화번호

주소

고객명

신용정보

상담내용

캠페인 대상정보

로그는 복사·수집·백업·장기보존되므로 업무 DB보다 더 넓게 노출될 수 있다.

## 28.4.2 원문 기록 금지

\`\`\`text id=“obs28120” 비밀번호

Access Token

Refresh Token

SSO Assertion

Private Key

API Key

HMAC Secret

주민등록번호 전체

계좌번호 전체

카드번호 전체

인증서 비밀번호

Cookie 전체

Authorization Header

요청·응답 Body 전체


\---

\## 28.4.3 마스킹 예

\`\`\`text id="obs28121"
홍길동
→ 홍\*동

900101-1234567
→ 900101-1\*\*\*\*\*\*

123-456-789012
→ 123-\*\*\*-\*\*\*\*\*\*

010-1234-5678
→ 010-\*\*\*\*-5678

마스킹 규칙은 로그마다 임의로 구현하지 않고 공통 Library로 제공한다.

## 28.4.4 Hash

Hash는 동일 대상의 반복 발생 여부를 비교할 때 사용할 수 있다.

text id="obs28122" customerNo → HMAC 또는 Salt Hash

일반 단순 Hash는 사전대입 공격에 취약할 수 있다.

민감 업무 키는 승인된 HMAC·Salt 정책을 사용한다.

## 28.4.5 사용자 ID

userId도 내부 개인정보·식별정보일 수 있다.

운영 추적에 필요하면 접근통제된 거래로그에 저장하고 일반 Application Log에는 Masking·Hash를 검토한다.

## 28.4.6 로그 주입

사용자 입력값:

text id="obs28123" customerName = "홍길동\\nERROR 인증 우회 성공"

그대로 로그에 기록하면 가짜 로그 행이 생성될 수 있다.

방어:

\`\`\`text id=“obs28124” CR·LF 제거

길이 제한

구조화 Encoder

허용문자 검증

JSON Escape


\---

\## 28.4.7 오류 메시지

외부 Exception 메시지나 SQL을 사용자 응답에 넣지 않는다.

\`\`\`text id="obs28125"
ORA-00942

jdbc:oracle:thin:@10.1.1.20

PKIX path building failed

File path /app/secret

는 운영로그에 제한적으로 기록하고 사용자에게는 표준 메시지를 반환한다.

## 28.4.8 로그 접근통제

역할별 접근:

| 역할 | 접근범위 |
| --- | --- |
| 개발자 | 개발·비식별 로그 |
| 운영자 | 운영 거래·기술 로그 |
| 보안 | 보안로그 |
| 감사 | 승인된 감사로그 |
| DBA | DB·SQL 성능 |
| 업무담당 | 제한된 거래조회 |
| 관리자 | 권한승인·조회감사 |

운영로그 조회 자체도 감사 대상이 될 수 있다.

## 28.4.9 로그 전송 보안

\`\`\`text id=“obs28126” TLS

Server 인증서 검증

수집 Agent 인증

전송 실패 Queue

재전송

전문 압축

무결성



로그 수집 실패 때문에 애플리케이션 Thread가 장시간 대기하지 않도록 한다.

\---

\## 28.4.10 저장 암호화

\`\`\`text id="obs28127"
Disk Encryption

DB TDE

Object Storage 암호화

Backup 암호화

Key Rotation

암호키는 로그 저장소와 분리한다.

## 28.4.11 로그 위변조 방지

중요 감사로그는 다음을 검토한다.

\`\`\`text id=“obs28128” Append Only

DB 권한 분리

WORM Storage

Hash Chain

전자서명

삭제권한 제한

관리자 작업 감사



업무 애플리케이션 계정이 감사로그를 UPDATE·DELETE할 수 없게 하는 것이 바람직하다.

\---

\## 28.4.12 로그 삭제

OM에 전체 로그 삭제 기능이 있더라도 운영에서는 매우 강한 통제가 필요하다.

\`\`\`text id="obs28129"
이중 승인

삭제 사유

대상 기간

건수

백업 여부

Legal Hold 확인

삭제 실행자

삭제 결과 감사

기본 운영은 수동 전체 삭제보다 보존기간 기반 자동 파기다.

## 28.4.13 보존기간

로그 유형별로 다르게 정의한다.

| 로그 | 보존 예시 |
| --- | --- |
| Debug | 매우 짧게 |
| Application | 운영 분석기간 |
| Transaction | 업무·감사 기준 |
| Audit | 법규·감사 기준 |
| Security | 침해 분석 기준 |
| Metric Raw | 단기 |
| Metric Rollup | 장기 |
| Trace | Sampling·오류 중심 |
| Deployment | Release 수명 이상 |

정확한 기간은 법규·내규·업무 중요도에 따라 승인한다.

## 28.4.14 계층형 보존

\`\`\`text id=“obs28130” Hot → 최근 로그, 빠른 검색

Warm → 중기 보존, 압축

Cold → 감사·장기 보관

Delete → 승인된 파기


\---

\## 28.4.15 Legal Hold

조사·분쟁·감사가 진행 중인 로그는 일반 보존기간이 도래해도 삭제하지 않는다.

\`\`\`text id="obs28131"
holdId

대상 기간

대상 사용자·ServiceId

승인자

시작·종료일

해제 사유

## 28.4.16 Metric 개인정보

Metric Label에 다음을 넣지 않는다.

\`\`\`text id=“obs28132” userId

customerNo

accountNo

guid

traceId

IP 전체

오류 메시지



Metric은 집계 신호이고 개별 거래 식별은 로그·Trace에서 수행한다.

\---

\## 28.4.17 Trace 개인정보

Span Attribute에도 Body·SQL Parameter·Token을 넣지 않는다.

권장:

\`\`\`text id="obs28133"
serviceId

sqlId

targetSystem

resultType

errorCode

rowCount

elapsed

# 표준 로그 필드

\`\`\`text id=“obs28134” timestamp

level

event

systemId

environment

businessCode

serviceId

transactionCode

guid

traceId

spanId

parentSpanId

userIdMasked

branchId

channelId

instanceId

warName

artifactVersion

configVersion

thread

stage

resultType

resultCode

errorCode

elapsedMs



모든 로그에서 모든 필드를 강제할 필요는 없지만 공통 필드 이름과 의미는 통일한다.

\---

\# 표준 거래로그 테이블 예

\`\`\`sql id="obs28135"
CREATE TABLE TCF\_TRANSACTION\_LOG (
LOG\_ID VARCHAR2(50) NOT NULL,
GUID VARCHAR2(100) NOT NULL,
TRACE\_ID VARCHAR2(100),
ROOT\_SPAN\_ID VARCHAR2(50),
BUSINESS\_CODE VARCHAR2(20) NOT NULL,
SERVICE\_ID VARCHAR2(100) NOT NULL,
TRANSACTION\_CODE VARCHAR2(50),
USER\_ID VARCHAR2(100),
BRANCH\_ID VARCHAR2(30),
CHANNEL\_ID VARCHAR2(30),
INSTANCE\_ID VARCHAR2(100),
ARTIFACT\_VERSION VARCHAR2(50),
STARTED\_AT TIMESTAMP NOT NULL,
ENDED\_AT TIMESTAMP,
RESULT\_STATUS VARCHAR2(30) NOT NULL,
RESULT\_CODE VARCHAR2(30),
ERROR\_CODE VARCHAR2(50),
ELAPSED\_MS NUMBER(15),
TX\_OUTCOME VARCHAR2(20),
CREATED\_AT TIMESTAMP NOT NULL,
CONSTRAINT PK\_TCF\_TRANSACTION\_LOG
PRIMARY KEY (LOG\_ID)
);

권장 Index:

\`\`\`sql id=“obs28136” CREATE UNIQUE INDEX UX\_TCF\_TX\_LOG\_GUID ON TCF\_TRANSACTION\_LOG (GUID);

CREATE INDEX IX\_TCF\_TX\_LOG\_SERVICE\_TIME ON TCF\_TRANSACTION\_LOG ( SERVICE\_ID, STARTED\_AT );

CREATE INDEX IX\_TCF\_TX\_LOG\_ERROR\_TIME ON TCF\_TRANSACTION\_LOG ( ERROR\_CODE, STARTED\_AT );


\---

\# 표준 감사로그 테이블 예

\`\`\`sql id="obs28137"
CREATE TABLE TCF\_AUDIT\_LOG (
AUDIT\_ID VARCHAR2(50) NOT NULL,
EVENT\_TIME TIMESTAMP NOT NULL,
EVENT\_TYPE VARCHAR2(50) NOT NULL,
USER\_ID VARCHAR2(100) NOT NULL,
BRANCH\_ID VARCHAR2(30),
SOURCE\_IP VARCHAR2(64),
SERVICE\_ID VARCHAR2(100) NOT NULL,
TARGET\_TYPE VARCHAR2(50) NOT NULL,
TARGET\_ID\_HASH VARCHAR2(128),
ACTION\_TYPE VARCHAR2(30) NOT NULL,
CHANGE\_SUMMARY VARCHAR2(2000),
ACTION\_REASON VARCHAR2(1000),
APPROVAL\_ID VARCHAR2(100),
RESULT\_STATUS VARCHAR2(30) NOT NULL,
GUID VARCHAR2(100) NOT NULL,
TRACE\_ID VARCHAR2(100),
PREVIOUS\_HASH VARCHAR2(128),
RECORD\_HASH VARCHAR2(128),
CONSTRAINT PK\_TCF\_AUDIT\_LOG
PRIMARY KEY (AUDIT\_ID)
);

# 정상 거래 흐름

text id="obs28138" 1. Gateway가 요청을 수신한다. 2. GUID·TraceId가 없으면 생성한다. 3. Gateway Root Span을 시작한다. 4. 인증·Route 결과를 Gateway 로그에 기록한다. 5. 업무 WAR가 GUID·Trace Context를 수신한다. 6. STF가 TransactionContext와 MDC를 설정한다. 7. 거래로그를 STARTED 상태로 기록한다. 8. Dispatcher가 Handler를 실행한다. 9. Service·SQL·연계별 Child Span을 생성한다. 10. 성공 결과와 처리시간을 Metric에 기록한다. 11. ETF가 거래로그를 SUCCESS로 종료한다. 12. 응답 Header에 GUID를 반환한다. 13. TCF·Filter가 MDC와 ThreadLocal을 정리한다.

# 업무 오류 흐름

text id="obs28139" 업무 Rule 오류 ↓ BusinessException ↓ 거래로그 BUSINESS\_FAIL ↓ 오류코드 Metric 증가 ↓ Stack Trace는 기본 미출력 ↓ 사용자 표준 메시지

# 시스템 오류 흐름

text id="obs28140" Mapper SQL 오류 ↓ 원인 예외 보존 ↓ 최종 경계 Stack Trace 1회 ↓ 거래로그 SYSTEM\_FAIL ↓ sql.error·transaction.error Metric ↓ GUID 포함 표준 응답

# Timeout 흐름

text id="obs28141" TCF Deadline 초과 ↓ timeoutStage 기록 ↓ 거래로그 TIMEOUT ↓ tcf.transaction.timeout 증가 ↓ Thread·Connection 반환 확인 ↓ 필요 시 결과 UNKNOWN 대사

# 비동기 흐름

text id="obs28142" 온라인 요청 Trace ↓ Outbox Event ├─ eventId ├─ correlationId └─ causationId ↓ Message Consumer 신규 Span ↓ 외부 처리 ↓ 상태 변경·감사

Trace 저장기간보다 비동기 처리 지연이 길 수 있으므로 Correlation ID를 업무 상태 테이블에도 저장한다.

# 장애 분석 예

## 증상

text id="obs28143" SV 고객조회 p95 1.2초 → 5.1초

## 분석

text id="obs28144" 1. ServiceId Metric에서 증가 확인 2. 배포 Event와 시각 비교 3. Slow Trace 3건 선택 4. Span별 시간 확인 5. SQL Span 4.5초 확인 6. SQL ID로 Mapper 확인 7. DB Plan·Lock·Pool 확인 8. 데이터량·Index 변경 확인

## 결론 예

\`\`\`text id=“obs28145” 신규 검색조건에서 Index 미사용

Full Scan 증가

DB Connection 점유 증가

Tomcat Busy Thread 증가



이처럼 메트릭→Trace→로그→SQL 순서로 범위를 좁힌다.

\---

\# 정상 예시

\`\`\`text id="obs28146"
화면
SV-CUS-0001

이벤트
고객요약 조회

ServiceId
SV.Customer.selectSummary

GUID
G-20260718-000801

TraceId
8d7a...

Gateway
SUCCESS 24ms

SV Transaction
SUCCESS 182ms

SQL
SvCustomerMapper.selectSummary 85ms

결과
S0000

거래로그
SUCCESS

Metric
tcf.transaction.duration 기록

사용자 응답
GUID 포함

# 금지 예시

\`\`\`text id=“obs28147” System.out으로 운영로그를 출력한다.

Request·Response Body 전체를 INFO로 출력한다.

Authorization Header를 기록한다.

JWT와 Refresh Token을 기록한다.

주민번호와 계좌번호를 원문으로 기록한다.

오류마다 모든 계층에서 Stack Trace를 출력한다.

고객 없음 업무 오류를 ERROR로 기록한다.

GUID 없이 오류를 반환한다.

Gateway와 업무 WAR가 서로 다른 TraceId를 생성한다.

내부 호출에서 상위 TraceId를 버린다.

모든 호출에 같은 SpanId를 사용한다.

비동기 Thread에 MDC를 전달하지 않는다.

비동기 실행 후 MDC를 제거하지 않는다.

Metric Label에 GUID와 userId를 넣는다.

Metric Label에 오류 메시지를 넣는다.

평균 응답시간만 관찰한다.

단일 CPU 순간 Peak로 Critical Alert를 발생시킨다.

Circuit Open을 일반 Timeout과 구분하지 않는다.

배포 Version을 Metric과 로그에 기록하지 않는다.

거래로그 저장 실패를 아무 기록 없이 무시한다.

감사로그를 일반 업무계정이 삭제할 수 있게 한다.

OM에서 사유·승인 없이 전체 로그를 삭제한다.

Actuator metrics·threaddump를 외부망에 공개한다.

로그 보존기간 없이 무기한 저장한다.

개인정보 파기 요청을 로그에는 적용하지 않는다.

NTP가 맞지 않는 서버의 시간을 그대로 사용한다.


\---

\# 책임 경계와 RACI

| 활동 | 업무개발 | FW | 운영 | 보안 | DBA | DevOps | 감사 | AA |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| 업무 로그 | A/R | C | C | C | I | I | I | C |
| 거래로그 | C | A/R | R/C | C | C | C | I | A/C |
| 감사로그 | R/C | C | C | A/C | I | I | A/R | C |
| 보안로그 | C | C | C | A/R | I | C | C | C |
| Trace 표준 | C | R | C | C | I | R/C | I | A |
| Metric 표준 | C | R | A/R | I | C | C | I | A/C |
| JVM·Pool 수집 | I | C | A/R | I | R/C | R/C | I | C |
| Alert 기준 | C | C | A/R | R/C | C | C | I | A/C |
| 로그 마스킹 | R | R/C | C | A/R | I | C | C | C |
| 보존·파기 | I | C | R | A/C | C | C | A/R | C |
| 접근권한 | I | I | R/C | A/R | I | C | A/C | I |
| 장애분석 | R/C | R/C | A/R | C | R/C | C | I | C |
| 품질 Gate | R | A/R | C | C | C | C | I | A |

\---

\# 성능·용량·확장성

\## 로그 발생량 산정

\`\`\`text id="obs28148"
일 로그량
\=
TPS
× 거래당 로그 건수
× 평균 로그 크기
× 운영시간

예:

\`\`\`text id=“obs28149” 100 TPS

거래당 8줄

평균 500Byte

1시간 ≈ 1.44GB



업무 Peak와 다중 WAR·Gateway 로그를 모두 포함해야 한다.

\---

\## 거래로그 DB 부하

\`\`\`text id="obs28150"
온라인 TPS
100

종료 시 INSERT
100 TPS 추가 DML

START와 END를 각각 DB에 저장하면 최대 200 TPS가 될 수 있다.

검토:

\`\`\`text id=“obs28151” 전용 DB·Table

Partition

Batch Insert

비동기 Queue

Index 수

보존·Archive

장애 시 Buffer


\---

\## Index 과다 주의

거래로그 Table에 검색 편의를 위해 Index를 너무 많이 만들면 매 거래 INSERT 비용이 증가한다.

운영 조회패턴을 기준으로 최소 Index를 설계한다.

\---

\## 비동기 Appender

장점:

\`\`\`text id="obs28152"
업무 Thread의 File I/O 대기 감소

주의:

\`\`\`text id=“obs28153” Queue 가득 참

Process 강제종료 시 유실

오류 로그 Drop

메모리 사용



오류·감사로그의 유실정책을 별도로 정의한다.

\---

\## Sampling

정상 DEBUG 로그:

\`\`\`text id="obs28154"
기본 비활성

일시 진단 시 대상 Instance·ServiceId 한정

Trace:

\`\`\`text id=“obs28155” 정상 Sampling

오류·Slow 100%


\---

\## Metric Series 수

\`\`\`text id="obs28156"
Series
\=
Metric 수
× Label 조합

예:

text id="obs28157" 100 ServiceId × 10 Instance × 6 Result × 5 Channel = 30,000 Series

Label을 추가할 때 비용을 계산한다.

## Dashboard 성능

대규모 로그 전체 검색보다 다음 집계 Table·Metric을 사용한다.

\`\`\`text id=“obs28158” ServiceId별 분당 건수

오류코드별 건수

p95·p99

Pool 사용률

Slow Top N


\---

\# 보안·개인정보·감사 기준

| 영역 | 기준 |
|---|---|
| Token | 원문 기록 금지 |
| 개인정보 | 마스킹·Hash |
| 로그 전송 | TLS |
| 로그 저장 | 암호화 |
| 로그 조회 | 역할 기반 |
| 감사로그 | Append Only |
| 삭제 | 승인·사유·감사 |
| 보존 | 유형별 기간 |
| Legal Hold | 파기 중지 |
| Actuator | 내부망·인증 |
| Metric Label | 개인정보 금지 |
| Trace Attribute | Body·Parameter 금지 |
| Backup | 암호화·접근통제 |

\---

\# 운영·모니터링·장애 대응

\## 기본 Dashboard

\### 서비스 현황

\`\`\`text id="obs28159"
TPS

성공률

업무 오류율

시스템 오류율

Timeout율

p95·p99

### WAS 현황

\`\`\`text id=“obs28160” CPU

Heap

GC Pause

Live Thread

Busy Thread

Uptime

Version


\### DB 현황

\`\`\`text id="obs28161"
Pool Active

Pool Idle

Pool Pending

Connection Timeout

Slow SQL

DB Health

### 연계 현황

\`\`\`text id=“obs28162” 호출량

오류율

Timeout

Retry

Circuit

외부 p95


\### Batch 현황

\`\`\`text id="obs28163"
Job 상태

적체

실패

Skip

대사

## 장애 점검 순서

text id="obs28164" 1. 사용자 GUID·발생시각 확인 2. ServiceId와 업무코드 확인 3. 거래로그 결과·처리시간 확인 4. Gateway와 업무 WAR Trace 연결 5. 실패 Span·Slow Span 확인 6. Tomcat Thread·JVM 상태 확인 7. DB Pool·Slow SQL 확인 8. 내부·외부 연계 확인 9. 배포·설정 변경시각 비교 10. 데이터 정합성 확인 11. 임시 복구 12. 원인 제거·재발방지

## 증거 보존

재기동 전 가능하면 다음을 보존한다.

\`\`\`text id=“obs28165” 대표 GUID

Thread Dump

JVM 상태

Heap·GC

DB Pool

Slow SQL

외부 호출상태

배포 Version

설정 Version

오류로그 구간



증거 없이 재기동하면 근본 원인을 잃을 수 있다.

\---

\# 자동검증 및 품질 Gate

\## 1. Logging API Gate

검출:

\`\`\`text id="obs28166"
System.out

System.err

printStackTrace

직접 FileWriter 로그

전체 Object \`toString()\`

운영 코드에서 발견되면 차단한다.

## 2\. 민감정보 Gate

금지 Key:

\`\`\`text id=“obs28167” password

secret

token

authorization

residentNumber

accountNumber

privateKey

cookie



로그문·DTO \`toString()\`·Exception 메시지를 검사한다.

\---

\## 3. Correlation Gate

\`\`\`text id="obs28168"
Ingress GUID 생성

응답 GUID

MDC 설정

MDC 정리

내부 호출 전파

외부 Request ID 연결

비동기 Context 전파

## 4\. 거래로그 Gate

\`\`\`text id=“obs28169” ServiceId

거래코드

GUID

결과

오류코드

처리시간

Instance

Version

시작·종료 완결


\---

\## 5. 감사로그 Gate

중요 ServiceId에 대해:

\`\`\`text id="obs28170"
감사 대상 등록

사용자

대상

행위

사유

결과

GUID

가 존재해야 한다.

## 6\. Metric Gate

\`\`\`text id=“obs28171” Counter·Timer 존재

Label Allow List

GUID·userId Label 금지

오류·Timeout Metric

Histogram·Percentile 설정

Instance·Version


\---

\## 7. Alert Gate

\`\`\`text id="obs28172"
Metric 존재

Threshold 근거

지속시간

등급

담당자

Runbook

중복 억제

## 8\. Actuator Gate

\`\`\`text id=“obs28173” 외부망 노출 금지

Health 세부정보 제한

metrics·prometheus 인증

threaddump 접근 제한

heapdump 비활성 또는 강한 통제


\---

\## 9. 보존 Gate

\`\`\`text id="obs28174"
로그 유형

보존기간

Archive

파기

Legal Hold

삭제 권한

파기 증적

# 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| OBS-001 | 정상 거래 | START·END 연결 |
| OBS-002 | GUID 없는 요청 | 서버 GUID 생성 |
| OBS-003 | 잘못된 GUID | 재생성·Client ID 보존 |
| OBS-004 | 응답 Header | GUID 반환 |
| OBS-005 | Gateway→WAR | TraceId 유지 |
| OBS-006 | 내부 업무 호출 | Child Span 생성 |
| OBS-007 | 외부 호출 | Request ID 연결 |
| OBS-008 | 비동기 Executor | MDC 전달 |
| OBS-009 | Executor 완료 | MDC 정리 |
| OBS-010 | 다음 사용자 요청 | 이전 MDC 없음 |
| OBS-011 | 업무 오류 | BUSINESS\_FAIL |
| OBS-012 | 시스템 오류 | SYSTEM\_FAIL |
| OBS-013 | Timeout | TIMEOUT·단계 기록 |
| OBS-014 | 거래 중 JVM Kill | 미완료 거래 탐지 |
| OBS-015 | 거래로그 DB 장애 | 업무·경보 정책 |
| OBS-016 | 감사로그 장애 | 중요 거래 차단정책 |
| OBS-017 | 거래로그 중복 GUID | Unique 정책 |
| OBS-018 | ServiceId 누락 | Gate 실패 |
| OBS-019 | 거래코드 누락 | Gate 실패 |
| OBS-020 | Instance Version | 로그 포함 |
| OBS-021 | 배포 후 Version | Dashboard 표시 |
| OBS-022 | 요청 Body 로그 | 보안 Gate 실패 |
| OBS-023 | Authorization 로그 | Gate 실패 |
| OBS-024 | JWT 로그 | Gate 실패 |
| OBS-025 | 주민번호 로그 | 마스킹 |
| OBS-026 | 계좌번호 로그 | 마스킹 |
| OBS-027 | CRLF 로그 주입 | Escape |
| OBS-028 | 긴 사용자 입력 | Truncate |
| OBS-029 | 업무 오류 Stack | 기본 미출력 |
| OBS-030 | 시스템 오류 Stack | 최종 경계 1회 |
| OBS-031 | 같은 예외 중복로그 | Gate 실패 |
| OBS-032 | 거래 Timer 정상 | Count·Duration 증가 |
| OBS-033 | 성공 거래 | SUCCESS Counter |
| OBS-034 | Timeout 거래 | Timeout Counter |
| OBS-035 | p95 계산 | 예상 범위 |
| OBS-036 | Metric Label GUID | Gate 실패 |
| OBS-037 | Metric Label userId | Gate 실패 |
| OBS-038 | 100 ServiceId | Series 용량 확인 |
| OBS-039 | CPU Peak 1회 | Alert 없음 |
| OBS-040 | CPU 지속 초과 | Warning |
| OBS-041 | Busy Thread 지속 | Critical 정책 |
| OBS-042 | Pool Active 80% | Warning |
| OBS-043 | Pool Pending 지속 | Critical |
| OBS-044 | Heap 상승·GC 회복 | 정상 판단 |
| OBS-045 | Heap 지속 상승 | Leak Alert |
| OBS-046 | Full GC 증가 | 성능 Alert |
| OBS-047 | Slow SQL | SQL ID 연결 |
| OBS-048 | 외부 Timeout | Interface Metric |
| OBS-049 | Circuit Open | 별도 Metric |
| OBS-050 | Cache Hit·Miss | 비율 계산 |
| OBS-051 | Batch 실패 | Job Alert |
| OBS-052 | Queue 적체 | Lag Alert |
| OBS-053 | 정상 Trace Sampling | 정책 비율 |
| OBS-054 | 오류 Trace | 100% 보존 |
| OBS-055 | Slow Trace | 100% 보존 |
| OBS-056 | Trace Exporter 장애 | 업무 지속 |
| OBS-057 | Log Collector 장애 | Buffer·Drop 정책 |
| OBS-058 | Async Queue 가득 참 | 경보 |
| OBS-059 | 로그 회전 | 크기·일자 정상 |
| OBS-060 | 보존기간 도래 | 자동 Archive·삭제 |
| OBS-061 | Legal Hold 로그 | 삭제 제외 |
| OBS-062 | 감사로그 UPDATE 시도 | 권한 거절 |
| OBS-063 | 감사로그 전체 삭제 | 이중승인 |
| OBS-064 | 로그 조회 | 조회 감사 |
| OBS-065 | Actuator health 공개 | 최소정보 |
| OBS-066 | Actuator metrics 외부 접근 | 차단 |
| OBS-067 | Thread Dump 미인가 | 차단 |
| OBS-068 | NTP 오차 | 경보 |
| OBS-069 | 화면 GUID 문의 | 거래 추적 성공 |
| OBS-070 | Table→화면 역추적 | 매트릭스 연결 |
| OBS-071 | 배포 직후 오류율 증가 | Deploy Event 연결 |
| OBS-072 | 오류율 정상화 | Alert 종료 |
| OBS-073 | Alert 중복 | 한 Incident로 Group |
| OBS-074 | Runbook 없는 Critical | 운영 Gate 실패 |
| OBS-075 | 담당자 없는 Alert | Gate 실패 |
| OBS-076 | Dashboard Query 과부하 | 집계 최적화 |
| OBS-077 | 거래로그 대량 INSERT | 목표 TPS 충족 |
| OBS-078 | 로그 Storage Full | 서비스 보호 |
| OBS-079 | Backup 로그 복구 | 무결성 확인 |
| OBS-080 | 전체 거래 E2E | 화면부터 SQL까지 추적 |

# 따라 하는 실무 절차

## 1단계. 대표 ServiceId를 선택한다

text id="obs28175" SV.Customer.selectSummary

## 2단계. 추적 식별자를 확인한다

\`\`\`text id=“obs28176” 화면 ID

ServiceId

GUID

TraceId

SQL ID


\## 3단계. 정상 거래를 실행한다

완료 증적:

\`\`\`text id="obs28177"
요청

응답

거래로그

Application Log

Metric

## 4단계. 업무 오류를 실행한다

\`\`\`text id=“obs28178” 오류코드

거래상태

Stack Trace 정책

사용자 메시지


\## 5단계. 시스템 오류·Timeout을 실행한다

\`\`\`text id="obs28179"
실패단계

원인 예외

Rollback

Thread·Connection 반환

## 6단계. 내부·외부 호출을 추적한다

\`\`\`text id=“obs28180” Parent Span

Child Span

Target

처리시간


\## 7단계. Metric을 확인한다

\`\`\`text id="obs28181"
Count

Error

Timeout

p95

Pool

Thread

## 8단계. Alert를 발생시킨다

Threshold·지속시간·담당자·Runbook을 검증한다.

## 9단계. 민감정보를 점검한다

\`\`\`text id=“obs28182” Token

비밀번호

개인정보

Request Body


\## 10단계. 로그 보존·삭제를 점검한다

\`\`\`text id="obs28183"
Rotation

Archive

Legal Hold

파기 증적

# 완료 체크리스트

## 로그

| 확인 항목 | 완료 |
| --- | --- |
| 로그 유형별 목적이 구분된다. | □ |
| 구조화 필드명이 통일됐다. | □ |
| GUID·TraceId·ServiceId가 있다. | □ |
| Instance·Version이 있다. | □ |
| START·END가 연결된다. | □ |
| 오류코드·처리시간이 있다. | □ |
| Stack Trace가 중복되지 않는다. | □ |
| System.out이 없다. | □ |
| 요청·응답 원문 로그가 없다. | □ |

## 거래·감사

| 확인 항목 | 완료 |
| --- | --- |
| 거래 상태가 세분화됐다. | □ |
| 거래 시작·종료 완결성을 확인한다. | □ |
| JVM 강제종료 거래를 탐지한다. | □ |
| 중요 행위 감사대상이 정의됐다. | □ |
| 감사로그에 대상·사유·결과가 있다. | □ |
| 감사로그 변경·삭제가 제한된다. | □ |
| 로그 조회·삭제도 감사된다. | □ |

## 추적

| 확인 항목 | 완료 |
| --- | --- |
| Ingress에서 GUID를 생성한다. | □ |
| TraceId·SpanId를 구분한다. | □ |
| Gateway와 업무 WAR가 연결된다. | □ |
| 내부 호출 Child Span이 있다. | □ |
| 외부 거래번호를 연결한다. | □ |
| 비동기 Context가 전달된다. | □ |
| MDC가 요청 종료 시 제거된다. | □ |
| 화면에서 GUID를 확인할 수 있다. | □ |
| SQL ID까지 추적된다. | □ |

## 메트릭·알림

| 확인 항목 | 완료 |
| --- | --- |
| ServiceId별 Count·Timer가 있다. | □ |
| 오류·Timeout Metric이 있다. | □ |
| p95·p99를 확인한다. | □ |
| JVM·GC·Thread가 수집된다. | □ |
| Hikari Active·Pending이 수집된다. | □ |
| SQL·연계·Cache Metric이 있다. | □ |
| 고 Cardinality Label이 없다. | □ |
| Alert 지속시간이 있다. | □ |
| Alert 담당자와 Runbook이 있다. | □ |
| 중복 알림이 억제된다. | □ |

## 보안·보존

| 확인 항목 | 완료 |
| --- | --- |
| Token·Secret을 기록하지 않는다. | □ |
| 개인정보가 마스킹된다. | □ |
| 로그 주입을 차단한다. | □ |
| 로그 전송이 암호화된다. | □ |
| 저장소 접근권한이 분리된다. | □ |
| Actuator가 내부 통제된다. | □ |
| 유형별 보존기간이 있다. | □ |
| Legal Hold가 있다. | □ |
| 파기 결과가 감사된다. | □ |

# 변경·호환성·폐기 관리

## 로그 필드 추가

선택 필드 추가는 비교적 호환 가능하다.

그러나 다음을 확인한다.

\`\`\`text id=“obs28184” Parser

Dashboard

검색 Index

저장용량

개인정보

기존 Agent


\---

\## 로그 필드명 변경

\`\`\`text id="obs28185"
elapsedTime
→ elapsedMs

영향:

\`\`\`text id=“obs28186” 수집 Pipeline

Dashboard

Alert

검색 Query

운영 Runbook

감사 Report



병행기간을 두거나 Mapping을 지원한다.

\---

\## GUID 형식 변경

Gateway·업무 WAR·화면·외부 연계·DB Column 길이에 영향을 준다.

신규 형식과 기존 형식을 일정 기간 모두 허용할 수 있어야 한다.

\---

\## Metric 이름 변경

기존 Alert가 동작하지 않을 수 있다.

\`\`\`text id="obs28187"
신규 Metric 병행

Dashboard 전환

Alert 전환

구 Metric 호출량 0

폐기

## Label 추가

Label 추가 전 Series 증가량을 계산한다.

\`\`\`text id=“obs28188” 기존 10,000 Series

Label 20개 값 추가 → 최대 200,000 Series


\---

\## 보존기간 변경

보존기간 단축 전:

\`\`\`text id="obs28189"
법규

감사

사고 조사

Backup

Legal Hold

를 확인한다.

## OpenTelemetry 전환

text id="obs28190" 1. 기존 GUID 유지 2. TraceId·SpanId 병행 3. Gateway·내부 Client 우선 적용 4. 오류·Slow Trace 검증 5. Dashboard 전환 6. 수동 Trace 로그 축소

기존 운영 검색방식을 한 번에 제거하지 않는다.

## 로그 저장소 폐기

\`\`\`text id=“obs28191” 신규 수집 중지

보존 데이터 이관

Hash·건수 검증

조회권한 전환

Legal Hold 확인

구 저장소 읽기전용

보존기간 후 파기


\---

\# 시사점

\## 핵심 아키텍처 판단

첫째, 로그·메트릭·추적은 각각 다른 질문에 답하며 하나의 수단이 나머지를 대체할 수 없다.

둘째, NSIGHT 관측성의 핵심 식별자는 \`ServiceId\`, \`GUID\`, \`TraceId\`, \`SpanId\`다.

셋째, GUID는 업무 WAR에 도착한 뒤가 아니라 Gateway·Ingress 경계에서 생성돼야 Parsing·인증·라우팅 실패도 추적할 수 있다.

넷째, 거래로그는 정상 거래 통계뿐 아니라 실패·Timeout·미완료 거래를 증명할 수 있어야 한다.

다섯째, 현재의 Debug Log형 \`TCF\_METRIC\`은 실제 시계열 메트릭으로 확장해야 ServiceId별 TPS·오류율·p95를 운영할 수 있다.

여섯째, \`System.out\`과 Request·Response 원문 출력은 개발 진단에는 사용할 수 있지만 운영에서는 반드시 제거해야 한다.

일곱째, Metric Label에 GUID·userId 같은 고 Cardinality 값을 넣으면 관측 시스템 자체가 장애 원인이 될 수 있다.

여덟째, 로그 보안은 마스킹만의 문제가 아니라 전송·저장·접근·무결성·보존·파기 전 과정의 통제다.

\---

\## 주요 위험

| 위험 | 결과 |
|---|---|
| GUID 생성이 늦음 | 초기 오류 추적 불가 |
| Span 없음 | 구간별 지연 분석 불가 |
| MDC 미정리 | 사용자 로그 혼입 |
| 비동기 Context 누락 | Trace 단절 |
| 거래 시작 미저장 | 강제종료 거래 누락 |
| 로그 DB가 업무 DB에 종속 | 장애 시 증거 유실 |
| Debug 로그형 Metric | 추세·알림 불가 |
| \`System.out\` | 제어·구조화·보안 문제 |
| Body 전체 로그 | 개인정보 유출 |
| Stack 중복 | 로그 폭증 |
| Metric 고 Cardinality | 저장소 장애 |
| 평균만 관찰 | Tail 지연 누락 |
| Actuator 과다노출 | 내부정보 유출 |
| 감사로그 삭제권한 | 증적 훼손 |
| 보존정책 부재 | 비용·법적 위험 |
| Alert Storm | 중요 경보 누락 |
| NTP 불일치 | Trace 순서 왜곡 |
| 배포 Version 미기록 | 변경 원인 추적 실패 |

\---

\## 우선 보완 과제

1\. Gateway·Ingress에서 GUID·TraceId를 생성하도록 표준화한다.
2\. SpanId·ParentSpanId와 \`traceparent\` 전파를 구현한다.
3\. TCF 거래 시작 상태 영속화와 미완료 거래 탐지를 구현한다.
4\. \`TransactionMetricService\`를 Micrometer Counter·Timer로 전환한다.
5\. ServiceId·오류·Timeout·p95 Metric을 공통 제공한다.
6\. Tomcat Busy Thread·Hikari Pending·SQL 지연 Metric을 추가한다.
7\. TCF·STF·ETF·Filter의 \`System.out\`을 구조화 Logger로 교체한다.
8\. Request·Response 원문 출력과 Token·개인정보 로그를 정적검사로 차단한다.
9\. 비동기 Executor·Timeout Thread에 Context 전파와 정리 기능을 적용한다.
10\. 거래로그 전용 DataSource 또는 비동기 저장구조를 검토한다.
11\. 감사로그에 대상·변경 전후·사유·승인정보를 확대한다.
12\. Actuator Endpoint를 관리망·인증·별도 Port로 제한한다.
13\. 배포 Event와 Artifact·Config Version을 Dashboard에 표시한다.
14\. Alert별 담당자·지속시간·Runbook을 연결한다.
15\. 로그 보존·Legal Hold·파기·위변조 방지 정책을 공식화한다.

\---

\## 중장기 발전 방향

\`\`\`text id="obs28192"
문자열 로그
↓
구조화 JSON 로그

GUID 단일 추적
↓
TraceId·SpanId 분산추적

Debug Metric 로그
↓
Micrometer·시계열 Metric

개별 서버 파일
↓
중앙 로그 수집

수동 검색
↓
ServiceId Dashboard

단순 거래로그
↓
시작·종료·미완료 상태관리

일반 감사파일
↓
Append Only·무결성 감사저장소

정적 Threshold
↓
SLO·Baseline 기반 Alert

장애 후 로그 검색
↓
메트릭→Trace→로그 자동연결

개별 관측
↓
OM 통합 운영관제

# 마무리말

로그·메트릭·추적을 설계하는 과정은 다음 질문에 답하는 일이다.

\`\`\`text id=“obs28193” 한 거래의 식별자는 어디에서 생성되는가?

Gateway 이전 오류도 GUID로 찾을 수 있는가?

GUID와 TraceId·SpanId의 역할은 무엇인가?

내부·외부·비동기 호출에서 ID가 유지되는가?

MDC는 어느 시점에 설정되고 언제 제거되는가?

거래 시작과 종료가 모두 기록되는가?

서버 강제종료 거래를 찾을 수 있는가?

애플리케이션·거래·감사·보안로그는 구분되는가?

ServiceId별 TPS·오류율·p95를 확인할 수 있는가?

Tomcat Thread·JVM·DB Pool 상태를 함께 볼 수 있는가?

Slow SQL이 어떤 ServiceId와 화면에서 호출됐는가?

배포 Version과 오류 증가시점을 연결할 수 있는가?

Alert가 실제 사용자 영향과 운영 조치에 연결되는가?

GUID·userId가 Metric Label에 들어가 있지 않은가?

Request·Response·Token·개인정보가 로그에 남지 않는가?

감사로그가 위변조되거나 임의 삭제될 수 없는가?

로그 보존과 파기·Legal Hold 기준이 있는가?

운영자는 하나의 GUID로 처음부터 끝까지 거래를 설명할 수 있는가?



제28장의 핵심 흐름은 다음과 같다.

\`\`\`text id="obs28194"
사용자 요청
↓
Ingress Correlation ID
↓
Gateway Trace
↓
TCF MDC·거래로그
↓
Service·SQL·연계 Span
↓
응답·감사
↓
Metric·Alert
↓
운영 분석·복구

가장 중요한 원칙은 다음과 같다.

\`\`\`text id=“obs28195” 관측성은 로그의 양으로 평가하지 않는다.

이상을 메트릭으로 발견하고, Trace로 느린 구간을 찾으며, 로그와 데이터로 원인을 증명하고,

사용자 영향과 복구 결과까지 하나의 식별자로 설명할 수 있어야

운영자가 신뢰할 수 있는 로그·메트릭·추적 체계가 된다. \`\`\`
