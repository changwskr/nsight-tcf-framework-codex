# NSIGHT TCF 개발자 핵심 지식 상세판 (개선판)

> 목적: 신규 개발자가 NSIGHT TCF의 구조를 이해하고 거래 하나를 설계·구현·테스트·배포·장애분석할 수 있게 하는 실무 문서
> 기준 우선순위: 현재 소스/Gradle → 루트·모듈 README → 상세 Markdown → 집필본·샘플
> 기준일: 2026-07-25 · 원본: `NSIGHT_TCF_개발자_핵심지식_상세판.md`의 자기 비평·개선판

## 개선판에서 달라진 점

| 구분 | 원본의 문제 | 개선 내용 |
|------|-------------|-----------|
| 구조 | 41개 장이 평면 나열되어 학습 목표와 장의 대응을 알 수 없음 | 7개 Part로 재편, 학습목표↔장 매핑표 추가 (§0.2) |
| 논리 | Timeout 우선순위를 "설정으로 확인해야 한다"고 판단을 독자에게 떠넘김 | 소스(`PolicyDrivenTransactionAttributeSource`) 근거로 우선순위를 확정 서술 (§18) |
| 논리 | STF 11단계를 항상 실행되는 것처럼 서술했으나 실제로는 설정 토글로 생략됨 | 단계별 활성화 프로퍼티를 명시 (§6.2) |
| 용어 | "TCF"가 플랫폼 이름과 오케스트레이터 클래스 양쪽으로 혼용됨 | 용어 정의 박스 추가 (§0.1) |
| 근거 | "현재 소스에 콘솔 출력이 있다" 등 주장에 파일 근거가 없음 | 실제 파일 경로 인용으로 교체 |
| 예제 | 요청 예제의 `userId`가 §7의 "클라이언트 신원값 불신" 원칙과 충돌 | 예제에 주석·경고 명시 (§8) |
| 가독성 | Batch·파일·보안 등 여러 장이 주어·서술어 없는 키워드 나열 | 행동 가능한 문장으로 재서술 (§25, §26, §33) |

## 0.1 용어 정의 (혼동 방지)

| 용어 | 의미 | 주의 |
|------|------|------|
| **TCF (플랫폼)** | Transaction Control Framework 전체. 이 저장소의 거래 처리 체계 | 넓은 의미 |
| **`TCF` (클래스)** | `tcf-core`의 오케스트레이터. `TCF.process()`가 STF→Dispatcher→ETF를 지휘 | 좁은 의미. 이 문서에서 코드체(`TCF.process()`)로 쓰면 클래스를 뜻함 |
| **STF** | Standard Transaction Front. 거래 전처리(검증·통제·로그 시작) | 클래스명이자 단계명 |
| **ETF** | End Transaction Framework. 거래 마감(응답 조립·로그 종료) | 클래스명이자 단계명 |
| **Dispatcher** | `TransactionDispatcher`. serviceId → Handler 라우팅 | |
| **OM** | 운영관리(Operation Management). Catalog·통제·Timeout·오류코드의 기준정보 저장소 | `tcf-om` 모듈 |

## 0.2 학습목표 ↔ 장 매핑

이 문서를 읽은 개발자는 다음 질문에 답할 수 있어야 한다. 각 질문에 대응하는 장을 함께 표시한다.

| # | 질문 | 대응 장 |
|---|------|---------|
| 1 | 요청이 `/sv/online`에 도착한 뒤 어떤 검증을 거쳐 Handler가 실행되는가? | §4, §6 |
| 2 | URL, ServiceId, 거래코드, GUID는 각각 무엇을 식별하는가? | §3, §7, §9 |
| 3 | Handler~Mapper 6계층의 책임은 어떻게 다른가? | §10, §11 |
| 4 | 새 거래를 만들 때 코드 외에 OM에 무엇을 등록해야 하는가? | §12, §20 |
| 5 | Online, Transaction, DB Query Timeout은 어떤 관계인가? | §18 |
| 6 | 미등록 ServiceId, 거래 차단, DB 지연을 어떤 순서로 진단하는가? | §32 |
| 7 | bootRun과 통합 Tomcat 검증은 무엇이 다른가? | §27 |

**최소 필독 경로(신규 입사자)**: §2 → §4 → §6 → §7 → §10 → §11 → §12. 나머지 장은 해당 업무를 맡을 때 참조한다.

---

# Part I. 왜 TCF인가 (§2~§5)

## 2. 먼저 기억할 핵심

- NSIGHT는 URL 중심 REST 묶음이 아니라 **ServiceId 중심 거래 처리 플랫폼**이다.
- 요청은 `header + body` 표준 전문으로 들어온다.
- 공통 흐름은 `STF → Dispatcher/Handler → ETF`다.
- STF는 Header, 세션, 인증, 권한, 거래통제, Timeout, 멱등성, 거래로그를 처리한다. **단, 세션·권한 검증 등 일부 단계는 프로퍼티로 켜고 끈다(§6.2).**
- Dispatcher는 `header.serviceId`로 `TransactionHandler`를 찾는다.
- 업무 흐름은 `Handler → Facade → Service → Rule → DAO/Mapper`다.
- ETF는 성공·업무실패·시스템실패를 표준 응답으로 만들고 거래로그·감사·메트릭을 마감한다.
- ServiceId와 Handler만 추가해서 끝나는 것이 아니다. Catalog, 통제, 권한, Timeout, 오류코드가 함께 맞아야 한다.
- 장애 추적의 연결키는 `GUID/TraceId + ServiceId + TransactionCode + SQL ID`다.
- 현재 루트 빌드는 JDK 21과 Spring Boot BOM 3.3.5를 사용한다 (`build.gradle`).

## 3. TCF를 사용하는 이유

일반 Spring MVC는 URL이 Controller 메서드를 선택하고 Controller가 응답을 만든다. 금융 정보계의 수많은 거래에 같은 운영 기준을 적용하면 다음 문제가 생긴다.

- Controller마다 Header·권한 검증 방식이 달라진다.
- 거래를 URL, 클래스명, 화면명 중 무엇으로 추적해야 할지 불명확하다.
- 거래 차단이나 Timeout 변경을 위해 재배포하게 된다.
- 업무 개발자가 예외 응답을 제각각 만든다.
- Timeout 후 이미 처리된 변경 요청을 재시도해 중복 처리될 수 있다.
- 사용자 요청, 업무 프로그램, SQL, 감사로그를 연결하기 어렵다.

TCF는 모든 온라인 거래를 같은 파이프라인으로 통과시켜 이 문제를 해결한다. 식별자 4종의 역할 분담이 그 출발점이다.

| 식별자 | 답하는 질문 |
|--------|-------------|
| HTTP 경로 | 어느 업무 WAR로 보낼 것인가 |
| ServiceId | 업무 WAR 안에서 어떤 기능을 실행할 것인가 |
| 거래코드 | 운영·감사·통제 관점에서 어떤 거래인가 |
| GUID/TraceId | 한 번의 실행을 여러 시스템과 로그에서 어떻게 연결할 것인가 |

"API 하나 개발"은 다음 전체 연결을 만든다는 뜻이다.

```text
업무 요구사항
→ 업무코드 / ServiceId / 거래코드
→ 표준 요청·응답
→ Handler 이하 업무 계층
→ DB·외부 연동
→ 오류코드
→ 권한·거래통제·Timeout·멱등성
→ 거래로그·감사·메트릭
→ 테스트·WAR·배포·Rollback
```

## 4. 전체 아키텍처

```text
사용자 / Web / 외부 API
  ↓
GSLB → L4 → Apache HTTPD
  ↓
Gateway 또는 업무 WAR
  ↓
POST /{businessCode}/online
  ↓
OnlineTransactionController / TcfGateway
  ↓
TCF.process()                    ← tcf-core의 오케스트레이터 클래스
  ├─ STF.preProcess()
  ├─ OnlineTransactionTimeoutExecutor
  │    └─ TransactionDispatcher
  │         └─ TransactionHandler
  │              └─ Facade → Service → Rule → DAO/Mapper
  └─ ETF.success / businessFail / systemError
  ↓
StandardResponse
```

배포 관점:

- 업무별 독립 WAR가 각 Context를 가진다.
- 개발 시 `bootRun`으로 모듈별 별도 JVM과 포트를 쓴다.
- 로컬 통합 검증은 `ztomcat`의 8080에 여러 WAR를 올린다 (현재 배포 대상 16 WAR).
- 확장 구성은 Apache sticky session과 Tomcat Cluster를 사용할 수 있다.
- WAR의 `WEB-INF/lib`에 필요한 `tcf-*` JAR를 포함한다 (Tomcat `lib/` 공유 배치 아님).

## 5. 모듈 구성과 의존 방향

| 구분 | 모듈 | 핵심 책임 |
|---|---|---|
| 유틸 | `tcf-util` | 문자열, 날짜, 마스킹 |
| 거래 엔진 | `tcf-core` | 전문, STF/TCF/ETF, Dispatcher, Context, Timeout |
| Web | `tcf-web` | `/online`, Controller, Filter, WAR bootstrap |
| 연동 | `tcf-eai` | 표준 전문 기반 업무 간 HTTP/JSON 호출 |
| Cache | `tcf-cache` | 공통코드와 정책 Cache |
| OM | `tcf-om` | 사용자·권한·ServiceId·통제·Timeout·오류·파일 |
| 용량/환경 | `tcf-oc` | 용량 산정과 환경설정 |
| 인증 관문 | `tcf-gateway`, `tcf-jwt` | 라우팅·검증, 토큰 발급·JWKS |
| 운영지원 | `tcf-batch`, `tcf-ui`, `tcf-uj` | 배치, 관리자/테스트 UI |
| 업무 | `ic/pc/ms/sv/pd/eb/ep/ss/mg-service` | 업무 거래 구현 |
| 통합 실행 | `ztomcat` | 여러 WAR를 8080에서 검증 |

`om-service`는 레거시이며 현재는 `tcf-om`을 기준으로 한다 (`settings.gradle`에는 등록되어 있으나 일괄 빌드·배포 대상에서 제외).

```text
tcf-util
  → tcf-core
    → tcf-web
      → tcf-cache / tcf-eai
        → 업무 WAR / OM / Batch / Gateway / JWT
```

금지할 의존:

- 공통 모듈이 업무 모듈 참조
- 업무 WAR가 다른 업무 WAR의 Service 직접 참조
- 업무가 Gateway/JWT 구현체 직접 참조
- `tcf-util`에 Spring Web, DB, 업무 규칙 추가

업무 간 호출은 표준 전문 기반 Client로 수행해야 배포 독립성과 통제·추적이 유지된다.

---

# Part II. 요청 처리 파이프라인 (§6~§8)

## 6. 요청 처리 순서

### 6.1 TCF.process()

현재 `TCF.process()`(`tcf-core/.../processor/TCF.java`)의 순서:

1. 클라이언트 Header 복사본 생성
2. 요청 진입 로그
3. STF 전처리
4. 런타임 Hook 거래 시작
5. Online Timeout 안에서 Dispatcher 실행
6. 성공 시 ETF 성공 처리
7. `BusinessException`이면 업무실패 처리
8. 기타 예외가 Timeout인지 판별
9. Timeout은 표준 Timeout 오류로 변환
10. 나머지는 시스템오류 처리
11. 모든 Thread Context와 MDC 정리

정리 대상은 `TransactionContextHolder`, `AuthenticationContextHolder`, `TimeoutContextHolder`, SLF4J MDC다. WAS Thread는 재사용되므로 Context를 지우지 않으면 이전 사용자의 GUID나 인증정보가 다음 요청에 섞일 수 있다.

### 6.2 STF — 단계와 활성화 조건

STF의 실행 순서와, **각 단계가 항상 실행되는지 설정에 따라 생략되는지**를 함께 봐야 한다. 로컬에서 문서와 다른 동작을 보면 대부분 아래 토글 때문이다.

| 순서 | 단계 | 활성화 조건 (`nsight.tcf.*`) |
|---|---|---|
| 1 | `StandardHeaderValidator` | 항상 |
| 2 | GUID/TraceId 생성 | 항상 |
| 3 | `TransactionContext` 생성·저장 | 항상 |
| 4 | MDC 등록 | 항상 |
| 5 | `SessionValidator` | `session-validation-enabled` |
| 6 | `AuthenticationContextValidator` | 세션/인증 설정에 종속 |
| 7 | `AuthorizationValidator` | `authorization-validation-enabled` |
| 8 | `TransactionControlService` | `transaction-control-enabled` |
| 9 | `TimeoutPolicyService` | `timeout-policy-enabled` |
| 10 | `IdempotencyChecker` | `idempotency-enabled` |
| 11 | `TransactionLogService.start` | `transaction-log-enabled` |

예: `eb-service/src/main/resources/application.yml`은 로컬에서 `session-validation-enabled: false`, `authorization-validation-enabled: false`로 두어 세션·권한 단계를 생략한다. **로컬에서 권한 오류가 재현되지 않는 것은 정상이며, 운영 프로파일의 토글 상태를 반드시 별도로 확인해야 한다.**

단계 설계의 의도:

- 구조가 잘못된 요청을 먼저 거절한다.
- GUID를 먼저 확보해 모든 실패를 추적한다.
- 인증·권한을 통과한 요청만 업무 코드에 도달시킨다.
- 차단 거래를 실행 전에 막는다.
- 실행 전에 Timeout 예산을 Context에 적용한다.
- 중복 키를 처리 중 상태로 표시하고 거래로그를 시작한다.

### 6.3 Dispatcher

기동 시 모든 `TransactionHandler.serviceIds()`를 읽어 `Map<ServiceId, TransactionHandler>`를 만든다.

요청 시:

```text
header.serviceId
  → 없음: INVALID_HEADER
  → Map 조회 실패: SERVICE_NOT_FOUND
  → 성공: handler.handle(request, context)
```

Handler 클래스가 있어도 실행되지 않는 경우:

- Spring Bean 등록이 안 됨 (`@Component` 누락)
- Package Scan 범위 밖
- `serviceIds()` 문자열 불일치
- 다른 WAR에 배포
- 이전 WAR 버전 실행

**OM Catalog는 운영정책의 기준이고 Dispatcher Map은 Java 실행의 기준이다. 둘 다 맞아야 한다.** Catalog에만 있으면 코드가 없어서 실패하고, 코드에만 있으면 통제·권한·감사 기준이 비어 운영이 실패한다.

### 6.4 ETF

성공:

```text
멱등성 SUCCESS → 거래로그 S0000 종료 → 감사로그 → 메트릭 → StandardResponse.success
```

업무실패:

```text
멱등성 FAIL → 거래로그 E0001 + 상세 오류코드 → 감사로그 → 메트릭 → StandardResponse.fail
```

시스템실패도 거래 상태를 닫고 외부에는 내부 StackTrace 대신 표준 시스템 오류를 준다. **Handler가 표준 응답이나 거래로그를 직접 조립하면 안 된다.** 그 순간 실패 경로마다 응답 형식이 갈라지고 거래 마감 규칙이 무너진다.

## 7. StandardHeader 상세

| 필드 | 의미 | 주의사항 |
|---|---|---|
| `systemId` | 호출 시스템 | 서버 기본값 가능 |
| `businessCode` | 대상 업무 | Context·ServiceId Prefix와 일치 |
| `serviceId` | 실행 기능 | Dispatcher Key |
| `serviceName` | 서비스명 | 통제 정책 필수값 가능 |
| `transactionCode` | 운영 거래 | Catalog·통제·로그와 일치 |
| `processingType` | 처리 유형 | 대문자 정규화 |
| `guid` | End-to-End 추적 | 하위 시스템 전달 |
| `traceId` | 내부 추적 | Span 정책과 구분 |
| `channelId` | 호출 채널 | 통제 조건 |
| `userId` | 사용자 | **인증 Context에서 확정 — 클라이언트 전송값 신뢰 금지** |
| `branchId` | 지점 | 데이터 권한 |
| `centerId` | 센터 | 장애 분석 |
| `requestTime` | 요청 시각 | ISO Offset DateTime |
| `clientIp` | 원 IP | Gateway에서 신뢰값 구성 |
| `idempotencyKey` | 중복 요청 Key | 변경 거래에서 중요 |

현재 `normalize()`는 기본 `systemId`, 현재 `requestTime`, 대문자 `businessCode/processingType`을 보완한다.

**신원 정보의 신뢰 경계**: 브라우저가 보낸 `userId`, `branchId`, `clientIp`는 참고값일 뿐이다. 권한 판단에 쓰는 신원은 인증된 Gateway 또는 Session Context가 확정한다. (§8 예제의 주석 참조)

논리 정합:

```text
businessCode = ServiceId 첫 구간
businessCode = 요청 Context의 업무
transactionCode 첫 구간 = businessCode
요청 ServiceId = Catalog ServiceId = Handler serviceIds()
요청 거래코드 = Catalog·통제 거래코드
processingType = 거래코드 유형과 호환
```

## 8. 표준 전문 예제

### 요청

```json
{
  "header": {
    "systemId": "NSIGHT-WEB",
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "serviceName": "고객요약조회",
    "transactionCode": "SV-INQ-0002",
    "processingType": "INQUIRY",
    "guid": "",
    "traceId": "",
    "channelId": "WEB",
    "userId": "u001",
    "branchId": "001",
    "requestTime": "2026-07-25T13:00:00+09:00"
  },
  "body": {
    "customerNo": "CUST00000001",
    "baseDate": "20260725"
  }
}
```

> **주의**: 예제의 `userId`/`branchId`는 전문 형식을 보여주기 위한 것이다. 서버는 이 값을 그대로 신뢰하지 않고, 세션/JWT 인증 Context에서 확정된 값으로 권한을 판단한다(§7, §33). `guid`/`traceId`를 빈 값으로 보내면 STF가 생성한다.

### 성공

```json
{
  "header": {
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0002",
    "guid": "generated-guid",
    "traceId": "generated-trace"
  },
  "result": {
    "resultCode": "S0000",
    "resultMessage": "정상 처리되었습니다."
  },
  "body": {
    "customerNo": "CUST00000001",
    "customerGrade": "GOLD"
  }
}
```

### 업무실패

```json
{
  "header": {
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "guid": "same-guid"
  },
  "result": {
    "resultCode": "E0001",
    "resultMessage": "처리 중 오류가 발생했습니다.",
    "errorCode": "E-SV-BIZ-0001",
    "errorMessage": "조회된 고객 정보가 없습니다.",
    "errorSystemId": "NSIGHT-MP",
    "errorDateTime": "2026-07-25T13:00:01+09:00"
  },
  "body": null
}
```

`resultCode`는 큰 성공/실패 분류(S0000/E0001 수준)이고, `errorCode`는 원인·조치·감사·모니터링용 세부 식별자다. 화면은 `resultCode`로 분기하고, 운영자는 `errorCode`로 원인을 찾는다.

---

# Part III. 식별자와 업무 계층 (§9~§11)

## 9. 식별자와 명명

| 항목 | 규칙 | 예 |
|---|---|---|
| 업무코드 | 대문자 2~3자리 | `SV` |
| Context | 업무코드 소문자 | `/sv` |
| WAR | Context 이름 | `sv.war` |
| Package | 현재 소스 기준 | `com.nh.nsight.marketing.sv` |
| Endpoint | 공통 온라인 진입 | `POST /sv/online` |

ServiceId:

```text
{BusinessCode}.{Domain}.{action}

SV.Customer.selectSummary
OM.User.inquiry
MG.Message.send
```

- BusinessCode는 대문자, Domain은 명사 PascalCase, action은 동사 camelCase
- `get`, `proc`, `manage` 같은 모호한 표현과 약어 남용 금지

거래코드:

```text
{업무코드}-{처리유형}-{4자리 일련번호}
```

| 유형 | 의미 | 예 |
|---|---|---|
| `INQ` | 조회 | `SV-INQ-0002` |
| `REG` | 등록 | `OM-REG-0001` |
| `UPD` | 수정 | `OM-UPD-0001` |
| `DEL` | 삭제 | `OM-DEL-0001` |
| `EXE` | 실행 | `BT-EXE-0001` |
| `UPL`/`DWN` | 파일 | `UD-DWN-0001` |
| `APR` | 승인 | `OM-APR-0001` |
| `SND` | 발송 | `MG-SND-0001` |

**ServiceId는 실행 기능, 거래코드는 운영 거래다.** 하나의 기능(ServiceId)이 여러 운영 거래로 분류될 일은 없도록 1:1을 유지하되, 관점(실행 vs 운영·감사)이 다르므로 식별자를 분리한다.

## 10. 업무 6계층

```text
com.nh.nsight.marketing.{business}
├─ entry/
│  ├─ handler/        ServiceId 진입점
│  ├─ facade/         유스케이스·트랜잭션
│  └─ web/            별도 REST 진입
├─ application/
│  ├─ service/        처리 절차
│  ├─ rule/           검증·판단·계산
│  └─ scheduler/      스케줄
├─ persistence/
│  ├─ dao/            Mapper 호출 경계
│  └─ mapper/         MyBatis
├─ client/            외부·타업무 연동
├─ config/            Spring 설정
└─ support/           내부 도우미
```

| 계층 | 할 일 | 하지 않을 일 |
|---|---|---|
| Handler | ServiceId 등록, Body 전달, Facade 호출 | SQL, 복잡한 규칙, 응답 조립 |
| Facade | DTO 변환, 유스케이스, 트랜잭션 | SQL 직접 실행 |
| Service | Rule/DAO/Client 호출 순서 | HTTP 응답 생성 |
| Rule | 검증, 상태 전이, 계산 | Mapper 직접 호출 남발 |
| DAO | Mapper 호출과 영속성 경계 | 업무 판단 |
| Mapper/XML | SQL과 결과 매핑 | 화면·업무 정책 |

## 11. SV 고객요약 구현 해설

Handler (`sv-service/.../entry/handler/SvCustomerHandler.java`):

```java
@Component
public class SvCustomerHandler implements TransactionHandler {
    private static final String SELECT_SUMMARY =
        "SV.Customer.selectSummary";

    @Override
    public Collection<String> serviceIds() {
        return List.of(SELECT_SUMMARY);
    }

    @Override
    public Object doHandle(
            StandardRequest<Map<String, Object>> request,
            TransactionContext context) {
        return switch (context.getHeader().getServiceId()) {
            case SELECT_SUMMARY ->
                facade.selectCustomerSummary(request.getBody(), context);
            default ->
                throw new BusinessException(
                    ErrorCode.SERVICE_NOT_FOUND,
                    "미지원 serviceId");
        };
    }
}
```

- `@Component`라서 Spring과 Dispatcher가 발견한다.
- `serviceIds()`가 Dispatcher Map의 Key다.
- 같은 도메인의 여러 ServiceId를 한 Handler에 묶을 수 있다.
- 실제 업무 처리는 Facade로 위임한다.

Facade:

```java
@Transactional(readOnly = true, timeout = 3)
public Map<String, Object> selectCustomerSummary(
        Map<String, Object> body,
        TransactionContext context) {
    CustomerSummaryRequest request =
        CustomerSummaryRequest.fromMap(body);
    return service.selectCustomerSummary(request, context).toMap();
}
```

외부 Map을 DTO로 바꾸고 트랜잭션 경계를 연다. **Annotation의 `timeout = 3`은 OM Timeout 정책이 활성인 거래에서는 정책값으로 덮어써진다(§18의 우선순위 규칙 참조).** 따라서 annotation timeout은 "정책이 없을 때의 안전 기본값"으로 이해한다.

Service:

```java
public CustomerSummaryResponse selectCustomerSummary(
        CustomerSummaryRequest request,
        TransactionContext context) {
    var criteria = rule.buildSummaryCriteria(request);
    CustomerSummaryRow row = dao.selectCustomerSummary(criteria);
    rule.validateSummaryResult(row);
    return CustomerSummaryResponse.of(context, row);
}
```

"조건 생성 → 조회 → 결과 검증 → 응답 변환" 절차가 코드에 그대로 드러난다.

현재 Rule의 오류코드:

- 고객번호 필수: `E-SV-VAL-0001`
- 최대 20자: `E-SV-VAL-0002`
- 결과 없음: `E-SV-BIZ-0001`

DAO와 Mapper:

```java
public CustomerSummaryRow selectCustomerSummary(
        CustomerSummaryCriteria criteria) {
    return mapper.selectCustomerSummary(criteria);
}
```

```xml
<mapper namespace=
 "com.nh.nsight.marketing.sv.persistence.mapper.SvCustomerMapper">
  <select id="selectCustomerSummary"
          parameterType="..."
          resultType="..."
          timeout="3">
    ...
  </select>
</mapper>
```

Java 메서드명, XML `id`, namespace, 파라미터, 결과 타입 중 하나라도 다르면 기동 또는 실행 시 오류가 난다.

---

# Part IV. 구현 정책 (§12~§19)

## 12. 신규 거래 구현 순서

1. 거래 성격을 조회/등록/변경/삭제/승인/발송으로 구분한다.
2. 업무코드, ServiceId, 거래코드를 확정한다.
3. Request/Response와 Validation 표를 만든다.
4. 기능·데이터 권한, 감사, 멱등성 요구를 정한다.
5. Online/TX/DB/연동 Timeout 예산을 정한다.
6. Handler `serviceIds()`를 등록하고 Facade로 위임한다.
7. Facade에서 DTO 변환과 트랜잭션 경계를 둔다.
8. Service에서 유스케이스를 조립한다.
9. Rule에 형식·업무 규칙·상태 전이를 둔다.
10. DAO/Mapper와 제한된 SQL을 구현한다.
11. 표준 업무/시스템 오류코드를 연결한다.
12. OM Catalog, 통제, 권한, Timeout, 오류를 등록한다.
13. 정상·검증·권한·DB·Timeout·중복 테스트를 작성한다.
14. bootRun과 통합 Tomcat에서 Smoke Test한다.

1~5는 설계, 6~11은 구현, 12는 운영 등록, 13~14는 검증이다. **12를 빼먹으면 코드는 완성돼도 거래는 완성되지 않는다.**

## 13. 조회·등록·변경·삭제 설계

| 유형 | 반드시 설계할 것 |
|------|------------------|
| 조회 | `readOnly = true` · 목록 Pagination과 최대 건수 · "결과 없음"의 의미(오류인가 정상인가) · 실행계획과 Query Timeout · Cache TTL/Evict |
| 등록 | 중복 판단 Key와 `idempotencyKey` · DB Unique Constraint · 생성자·생성시각 · 감사로그 · 외부 호출 실패 시 보상/상태 확인 |
| 변경 | 허용 상태 전이 정의 · 낙관적 잠금(Version) 또는 조건부 Update · 변경 전/후 감사 · Timeout 후 Commit 결과 확인 절차 · 동일 요청 재전송 정책 |
| 삭제 | 물리/논리 삭제 선택 · 보존 기간과 감사 · 참조 무결성 · 승인 절차 필요 여부 · Cache·외부 동기화 |

## 14. Validation 설계

| 종류 | 예 | 위치 |
|---|---|---|
| 전문 구조 | Header/Body 없음 | TCF Validator |
| Header | ServiceId 없음 | STF |
| 형식 | 날짜, 길이 | DTO/Rule |
| 코드 | 유효 공통코드 | Rule/코드 서비스 |
| 인증 | Session/JWT | Gateway/TCF |
| 기능 권한 | ServiceId 실행 | Authorization |
| 데이터 권한 | 담당 지점 고객 | Rule/권한 서비스 |
| 상태 전이 | DRAFT에서만 승인 | Rule |
| 무결성 | Unique, FK | DB Constraint |

```java
if (!StringUtils.hasText(request.getCustomerNo())) {
    throw new BusinessException(
        "E-SV-VAL-0001",
        "고객번호는 필수입니다.");
}
```

사용자가 고칠 수 있는 메시지를 주되 SQL·테이블·내부 정책을 노출하지 않는다.

## 15. MyBatis와 SQL

```text
src/main/java/.../persistence/mapper/SvCustomerMapper.java
src/main/resources/mapper/sv/SvCustomerMapper.xml
```

기준:

- 필요한 컬럼만 조회한다.
- 조건 컬럼의 인덱스와 선택도를 확인한다.
- `${}` 직접 치환을 금지하고 `#{}` 바인딩을 쓴다.
- 동적 정렬 컬럼은 Allow-List로 제한한다.
- 안정적인 정렬 기준과 Pagination을 명시한다.
- 대량 `IN`, 깊은 Offset Paging을 주의한다.
- 복잡한 결과는 명시적 `resultMap`을 쓴다.
- SQL ID를 GUID/ServiceId와 연결할 수 있게 로그에 남긴다.
- "결과 없음"과 "DB 장애"를 구분해 처리한다.

**Full Scan은 Timeout만 늘려 해결하지 않는다.** 실행계획, 통계, 인덱스, 조회 범위를 먼저 고친다.

## 16. 서비스 간 연동

업무 WAR끼리 Java 클래스를 직접 호출하면 배포 독립성이 깨지고 공통 Timeout·인증·추적을 우회한다. `tcf-eai` 또는 표준 Client를 사용한다. (실전 예: `eb-service`의 `EpOnlineClient`가 EP 표준 전문을 POST하고 `result.resultCode == "S0000"`으로 성공을 판정)

전달 정보:

```text
대상 businessCode / ServiceId / transactionCode
GUID/TraceId
인증된 사용자·채널 Context
요청 Body
Connect/Read Timeout
```

Timeout 예산 예:

```text
온라인 전체 5.0초
  자체 검증/DB 1.0초
  외부 호출    2.5초
  응답/여유    1.5초
```

재시도가 비교적 안전한 경우: 멱등적인 조회, 연결 전에 실패한 경우, 상대 시스템이 재시도 가능으로 명시한 경우.

위험한 경우: 등록·발송처럼 이미 처리됐을 수 있는 경우, Read Timeout 후 상대 Commit 여부를 모르는 경우, 같은 멱등성 Key를 보존하지 않은 재시도.

## 17. 트랜잭션

- 일반적으로 Facade를 경계로 둔다.
- 조회는 `readOnly = true`.
- 외부 HTTP 호출을 DB 트랜잭션 안에 오래 포함하지 않는다.
- 여러 DB/외부 시스템을 로컬 트랜잭션처럼 오해하지 않는다.
- 등록·변경은 멱등성과 결과 조회 방식을 설계한다.
- Runtime 예외의 Rollback 규칙과 Checked 예외 정책을 확인한다.
- 큰 트랜잭션은 Lock, Connection 점유, Rollback 비용을 키운다.

외부 호출과 DB 변경이 함께 있을 때 선택지:

| 방식 | 결과 | 대표 사례 |
|------|------|-----------|
| DB 먼저 → 외부 호출 | 외부 실패 시 DB 보상 필요 | — |
| 외부 먼저 → DB | DB 실패 시 외부 보상 필요 | — |
| Outbox/Event | 최종 일관성, 별도 재처리 필요 | `eb-service`: `EB_EVENT` 테이블 적재 후 스케줄러가 EP로 발행 (READY→SENT/FAIL) |

업무 중요도와 일관성 요구에 따라 설계하고, 단순히 `@Transactional`을 붙였다고 해결됐다고 생각하지 않는다.

## 18. Timeout — 세 계층과 우선순위 (확정)

| 계층 | 정책 컬럼 | 적용 지점 |
|---|---|---|
| Online | `ONLINE_TIMEOUT_SEC` | `OnlineTransactionTimeoutExecutor` — Handler 전체 실행 |
| Transaction | `TX_TIMEOUT_SEC` | Spring `@Transactional` |
| DB Query | `DB_QUERY_TIMEOUT_SEC` | MyBatis Statement |

fallback 기본 상수 (`tcf-core/.../timeout/TcfServiceTimeoutConstants.java`):

```text
Online      5초
Transaction 5초
DB Query    3초
```

**TX Timeout의 우선순위는 소스에서 확정된다.** `tcf-web`의 `PolicyDrivenTransactionAttributeSource`는 `@Transactional` 속성을 읽은 뒤, `timeout-policy-enabled=true`이고 현재 거래의 `TimeoutContextHolder`에 정책이 있으면 **annotation의 timeout 값을 정책의 `TX_TIMEOUT_SEC`으로 덮어쓴다.**

```text
우선순위 (높음 → 낮음)
1. OM Timeout 정책 (TimeoutPolicyService가 STF에서 조회, Context 저장)
2. @Transactional(timeout = N)   ← 정책 없거나 기능 비활성 시에만 적용
3. Spring/드라이버 기본값
```

DB Query Timeout도 같은 방식으로 `PolicyDrivenQueryTimeoutInterceptor`가 MyBatis Statement에 정책값을 주입한다.

권장 관계:

```text
DB/외부 개별 제한 < Transaction < Online < Apache/Client < L4 Idle
```

**주의**: 상위 Online Timeout(Future 취소)이 하위 DB 작업을 반드시 중단시키지는 않는다. Online Timeout이 나도 DB 쿼리는 JDBC Statement Timeout까지 계속 실행될 수 있다. 그래서 세 계층을 모두 설정해야 하며, "응답은 Timeout인데 DB에는 Commit된" 상황이 가능함을 변경 거래 설계(§13, §19)에 반영해야 한다.

## 19. 멱등성과 중복 요청

```text
클라이언트 idempotencyKey
→ STF 상태 조회
→ 최초: PROCESSING
→ 업무 처리
→ 성공: SUCCESS
→ 재요청: 기존 결과 또는 명확한 중복 응답
```

상태: `PROCESSING`(처리 중) / `SUCCESS`(이미 성공) / `FAIL`(실패, 재시도 가능성 판단) / `UNKNOWN`(결과 확인 필요)

**현재 구현체는 `InMemoryIdempotencyChecker`(`tcf-core/.../idempotency/`)로, `ConcurrentHashMap`에 상태를 저장한다.** 즉:

- 단일 JVM(bootRun, 단일 Tomcat)에서는 동작한다.
- **다중 WAS에서는 노드 간 상태가 공유되지 않아 중복 차단이 보장되지 않는다.**
- 운영 다중화 전에 DB 또는 분산 저장소 기반 `IdempotencyChecker` 구현으로 교체해야 한다. 이는 "확인할 사항"이 아니라 **현재 코드의 확정된 제약**이다.

---

# Part V. 운영 정책 (§20~§28)

## 20. Service Catalog와 거래통제

Catalog 연결정보: ServiceId, 업무코드, 거래코드, Handler, 처리유형, 권한코드, Timeout, 감사 여부, 사용 상태, 담당 조직.

불일치 시 증상:

- 코드는 있지만 Catalog 미등록 → 통제 정책에 따라 차단
- Catalog는 있지만 Handler 없음 → `SERVICE_NOT_FOUND`
- 잘못된 거래코드 → 감사·통계 오염
- 잘못된 Timeout → 조기 차단 또는 지연 방치
- 권한 과다/누락

거래통제는 ServiceId, 거래코드, 업무코드, 사용자, 채널, 지점, IP 등을 조건으로 전체/부분 차단할 수 있다. **통제 저장소 장애 시 Fail-open(통과)인지 Fail-closed(차단)인지를 업무 위험도에 따라 사전에 명시해야 한다.** 조회성 거래는 Fail-open, 자금·승인성 거래는 Fail-closed가 일반적 출발점이다.

## 21. 오류 처리

형식: `E-{DOMAIN}-{CATEGORY}-{NNNN}`

```text
E-TCF-HDR-0001   Header 구조
E-TCF-SVC-0001   ServiceId 미등록
E-SV-VAL-0001    업무 Validation
E-SV-BIZ-0001    업무 규칙
E-TCF-DB-0001    DB
E-TCF-TIME-0001  Timeout
E-TCF-SYS-9999   미처리 시스템 예외
```

구분: Validation(필수값·형식·코드) / Business(결과 없음, 허용되지 않은 상태) / Authentication·Authorization / DB·Interface·Timeout / System(미처리 예외).

업무 계층은 의미 있는 `BusinessException`을 발생시키고 ETF가 표준 응답으로 변환한다. 사용자 메시지에 StackTrace, SQL, 서버명, 개인정보를 노출하지 않는다. 신규 오류코드는 OM에 등록하고 번호를 재사용하지 않는다.

## 22. 로그와 추적

최소 로그 Context:

```text
GUID / TraceId / ServiceId / TransactionCode / BusinessCode
User / Branch / Channel / SQL ID / Elapsed Time / Result·Error Code
```

금지:

- 비밀번호, JWT, Session ID
- 주민번호·계좌번호 원문
- 전체 요청 Body 무조건 출력
- 운영 `System.out` 의존

**실제 사례**: 현재 `eb-service`의 `EbEventPublishService`, `EbUserService`, `EpOnlineClient` 등에 학습·진단용 `System.out.println` 블록이 다수 존재한다. 이는 로컬 학습용이므로, 운영 적용 전 구조화 로그(SLF4J + 마스킹)로 교체하는 작업이 필요하다.

## 23. 세션·JWT·Gateway

책임 분리:

```text
JWT       토큰 발급·서명·만료·JWKS
Gateway   토큰 검증·라우팅·신뢰 Header 구성
TCF       세션·인증 Context·기능 권한·거래통제
업무 Rule  고객·지점·조직 데이터 권한
```

Sticky Session은 세 값이 일치해야 동작한다:

```text
JSESSIONID=....tc01  ←  Apache route=tc01  ←  Tomcat jvmRoute=tc01
```

DeltaManager 사용 시 주의:

- 세션 객체는 `Serializable`이어야 한다.
- 대형 DTO/파일을 세션에 저장하지 않는다.
- 배포 버전 간 세션 클래스 호환성을 확인한다.
- 복제 트래픽과 메모리를 측정한다.
- 센터 간에는 복제되지 않으므로 센터 전환 시 재로그인이 발생할 수 있다.

## 24. Cache

| 판단 | 대상 |
|------|------|
| 적합 | 공통코드, Service Catalog, 거래통제·Timeout 정책, 작고 변경 빈도가 낮은 기준정보 |
| 신중 | 대량 고객 데이터, 즉시 정합성이 필요한 상태, 사용자별 민감정보 |

정의할 항목:

```text
Key / 크기 / TTL / 최대 Entry
갱신 주체 / Evict / Reload
DB 장애 시 오래된 값 사용 여부
다중 노드 동기화
hit/miss/eviction 지표
```

**OM에서 정책을 바꿨는데 반영되지 않으면** DB 값뿐 아니라 Cache TTL, Evict 실행 여부, 노드별 Cache 상태를 순서대로 확인한다. 다중 노드에서는 한 노드만 갱신된 "부분 반영"이 가장 흔한 함정이다.

## 25. Batch와 Scheduler

핵심 원칙을 문장으로 정리하면:

- **Chunk 단위로 처리한다.** 전체 데이터를 한 트랜잭션으로 처리하면 Lock과 Rollback 비용이 커진다. Chunk 크기는 Commit 비용과 실패 시 재처리 범위를 함께 고려해 정한다.
- **Job 실행 이력을 남긴다.** 어떤 Job Instance가 언제 어디까지 처리했는지 기록해야 재시작 위치를 정할 수 있다.
- **중복 실행을 Lock으로 방지한다.** 스케줄러 다중화 또는 수동 실행과의 충돌을 막는다.
- **실패 건은 격리한다.** 한 건의 실패가 배치 전체를 멈추지 않도록 실패 목록을 분리 적재하고 별도 재처리한다.
- **온라인 DB 부하를 고려한다.** 업무 시간대의 대량 배치는 온라인 거래의 응답시간을 잠식한다.
- **Scheduler는 트리거일 뿐이다.** 업무 로직은 Job/Service에 두고, 스케줄러 클래스는 호출만 한다. (예: `EbEventPublishScheduler`는 `@Scheduled` 트리거만, 실제 처리는 `EbEventPublishService`)

## 26. 파일 처리

업로드에서 지켜야 할 것:

- 확장자뿐 아니라 실제 MIME/Signature를 검사한다.
- 파일명에서 Path Traversal(`../`)을 제거한다.
- 크기 제한과 바이러스 검사를 적용한다.
- 임시 저장 후 검증을 통과하면 확정 저장한다.
- 보존기간과 Metadata(업로더·시각·용도)를 기록한다.

다운로드에서 지켜야 할 것:

- 경로가 아니라 **파일 ID로 조회**한다.
- 기능 권한과 데이터 권한을 모두 검사한다.
- 저장 경로를 응답에 노출하지 않는다.
- 대용량은 Streaming으로 전송한다.
- 다운로드 감사로그를 남긴다.

```text
나쁜 예: GET /download?path=C:\data\secret.txt   ← 경로 노출·Traversal 위험
권장:    GET /files/{fileId} → Metadata 조회 → 권한 검사 → 저장소 Key로 Stream
```

## 27. 빌드와 실행

현재 루트 기준:

| 항목 | 값 |
|---|---|
| Java Toolchain | 21 |
| Spring Boot BOM | 3.3.5 |
| H2 | 2.2.224 |
| Test | JUnit Platform |
| 기본 bootRun Profile | `local` |

```powershell
.\gradlew.bat :sv-service:test
.\gradlew.bat :sv-service:bootRun
.\gradlew.bat :sv-service:bootWar
.\gradlew.bat buildBusinessWars    # 업무 11 WAR (9 *-service + tcf-om + tcf-oc)
.\gradlew.bat buildZtomcatWars     # 16 WAR (+ batch, ui, uj, jwt, gateway)
```

| 모드 | 용도 | 예 |
|---|---|---|
| bootRun | 단일 모듈 개발 (모듈별 포트) | `localhost:8086/sv/online` |
| ztomcat | 통합 WAR 검증 (전부 8080) | `localhost:8080/sv/online` |

bootRun은 빠른 반복에, ztomcat은 "WAR 패키징·context 경로·프로파일이 운영과 같은 방식으로 동작하는가"를 확인하는 데 쓴다. 문서의 고정 WAR 개수보다 현재 `build.gradle` 태스크 의존성과 실제 산출물을 기준으로 한다. `-x test`는 원인 분리용이지 공식 배포 기준이 아니다.

## 28. 환경설정

일반적인 우선순위 (높음 → 낮음):

```text
명령행 → 환경변수 → System Property
→ application-{profile}.yml → application.yml → 코드 기본값
```

문서 예시와 실행값이 다를 수 있으므로 기동 로그와 승인된 환경 설정을 확인한다. Secret은 Git, 평문 YAML, 로그, 공개 Actuator에 노출하지 않는다.

---

# Part VI. 품질과 안정성 (§29~§34)

## 29. 테스트 전략

| 구분 | 검증 |
|---|---|
| Rule 단위 | 필수값·형식·상태 전이 |
| Service 단위 | 호출 순서·분기·응답 변환 |
| Handler | ServiceId 등록·Facade 위임 |
| Mapper | namespace·SQL ID·파라미터·결과 |
| 통합 | Bean·트랜잭션·DB·예외 변환 |
| 거래 | 실제 `/online` 정상·실패 |
| 통제 | 미등록·차단·권한 없음 |
| 안정성 | 지연·Timeout·중복 |
| 보안 | 마스킹·권한 우회·입력 변조 |
| 배포 | WAR Context·Health·Smoke |

Rule 테스트 예:

```java
@Test
void customerNo가_없으면_검증오류() {
    var request = new CustomerSummaryRequest();

    var ex = assertThrows(
        BusinessException.class,
        () -> rule.buildSummaryCriteria(request));

    assertEquals("E-SV-VAL-0001", ex.getErrorCode());
}
```

거래 실패 테스트는 응답뿐 아니라 **거래로그와 멱등성 상태가 종료됐는지**까지 확인한다. 응답은 실패인데 상태가 PROCESSING으로 남으면 재요청이 계속 중복 차단된다.

공통 변경 시 회귀 범위:

- `tcf-core`, `tcf-web` 변경: 주요 업무 전체 회귀
- Gateway/JWT 변경: 인증·라우팅 End-to-End
- Mapper 변경: 실행계획과 거래 통합
- 설정 변경: 환경별 기동·Health·Rollback

## 30. 성능과 용량

요청은 다음 자원 사슬을 통과하며, **한 Pool만 늘리면 다음 계층이 병목이 된다.**

```text
동시 요청 → Tomcat Thread → Timeout Executor Thread
→ Hikari Connection → DB Session/CPU/IO
```

예: Tomcat Thread 300, Hikari Max 20이면 Connection 대기 → 응답 지연 → Timeout → 재시도 부하의 악순환이 생긴다.

필요 지표: Peak TPS, 평균/p95/p99 응답시간, 요청당 SQL 수와 점유시간, 외부 호출 비율, Session 크기, Heap·GC Pause, Thread/Connection 대기, Timeout·오류율.

직관 공식:

```text
평균 동시 처리량 ≈ TPS × 평균 응답시간(초)
```

100 TPS × 평균 0.2초 = 평균 동시 처리량 약 20. Peak와 꼬리 지연(p99)을 포함해 여유를 둔다.

## 31. 관측성

| 영역 | 수집 지표 |
|------|-----------|
| 애플리케이션 | ServiceId별 TPS · 성공/업무오류/시스템오류/Timeout 비율 · 평균·p95·p99 · Thread active/queue/reject |
| DB | Hikari active/idle/pending · Connection 획득시간 · SQL ID별 시간·호출수 · Lock·Deadlock·Long TX |
| JVM/WAS | Heap·Old Gen·GC Pause · Thread와 Blocked · Session 수·평균 크기 · Context Health |
| 인프라 | Apache Backend 오류 · Sticky route 분포 · Gateway Downstream Timeout · L4 Health |

**정상 기준선을 먼저 수집해야 장애 수치와 비교할 수 있다.** "지금 p99가 1.2초"라는 값은 평상시 p99를 모르면 판단 불가능하다.

## 32. 장애 분석

기본 순서:

1. 환경과 bootRun/Tomcat 구분
2. 실패 모듈·URL·ServiceId·거래코드 특정
3. GUID/TraceId로 계층 로그 연결
4. STF/Dispatcher/업무/ETF 중 실패 지점 식별
5. 최근 WAR·설정·DB·OM·Cache 변경 확인
6. 복구와 Rollback 판단

**증상 1 — ServiceId 미등록.** 다음 순서로 좁힌다:

```text
요청 철자 → Header serviceId → Handler serviceIds() 문자열
→ @Component 유무 → Package Scan 범위 → Dispatcher Map 로딩
→ OM Catalog 등록 → 배포 WAR 버전
```

**증상 2 — 거래 차단.** 오류코드 확인 → 통제 기능 사용 여부(§6.2 토글) → 일치 조건(ServiceId/거래코드/사용자/채널/지점/IP) → Rule 우선순위 → Cache 반영 → 비상 차단 이력.

**증상 3 — SQL은 빠른데 Timeout.** 개별 SQL만 보지 말고 GUID Timeline을 만든다. 후보: Connection 대기, SQL 전후 외부 호출, 여러 SQL 합산, Executor Queue, 응답 직렬화, Lock, GC Pause.

**증상 4 — 로그인은 되는데 권한 없음.** 인증 성공 ≠ ServiceId 기능 권한 ≠ 고객/지점 데이터 권한. 세 단계 중 어디서 거절됐는지 오류코드로 구분한다.

**증상 5 — 노드 장애 후 세션 유실.** JSESSIONID route 값 → Apache route와 Tomcat jvmRoute 일치 → `<distributable/>` → 직렬화 오류 로그 → Cluster Membership → 센터 간 전환 여부.

## 33. 보안 코딩

입력: 길이·허용문자·범위를 검증하고, Enum/코드는 Allow-List로 제한하며, SQL은 바인딩(`#{}`)만 사용하고, 파일 경로는 정규화하며, HTML 출력은 Context별로 Encoding한다.

인증정보: JWT·Session ID를 로그에 남기지 않는다. Secret을 평문으로 Git/YAML에 넣지 않는다. Key Rotation과 Refresh Token 폐기 절차를 둔다.

개인정보: 수집을 최소화하고, 외부 DTO는 필드를 명시적으로 나열하며(전체 Entity 직렬화 금지), 로그는 마스킹하고, Cache·세션 저장을 최소화하며, 파일 다운로드는 감사를 남긴다.

권한 판단의 원칙:

- **사용자가 Body/Header에 보낸 사용자·지점 값을 권한 판단에 사용하지 않는다.** 인증 Context의 확정값만 쓴다.
- Gateway 인증, TCF 기능권한, 업무 데이터권한을 분리해 각각 검증한다.

## 34. 코드 리뷰 질문

구조: ServiceId에서 Handler와 SQL까지 찾기 쉬운가 · Handler가 얇은가 · 규칙이 Rule/Service에 읽기 좋게 표현됐는가 · 업무 간 직접 의존이 없는가

데이터: Request/Response/DB Row가 분리됐는가 · 목록 최대 건수와 정렬이 있는가 · SQL Injection·Full Scan 위험이 없는가 · 트랜잭션이 과도하게 크지 않은가

안정성: Timeout이 계층별로 일관적인가 · 외부 호출 부분 실패를 처리하는가 · 재시도가 안전한가 · 중복 요청을 막는가

운영: 오류코드와 조치가 구체적인가 · GUID로 SQL·연동 로그가 연결되는가 · OM 등록과 Cache 반영 절차가 있는가 · Rollback 단위가 명확한가

보안: 클라이언트 신원 값을 신뢰하지 않는가 · 민감정보가 로그·응답·세션에 없는가 · 기능권한과 데이터권한을 모두 검증하는가

---

# Part VII. 실무 절차와 참고 (§35~§41)

## 35. 기존 프로그램 역추적

화면에서 출발:

```text
화면 이벤트 → URL → header.serviceId → Handler.serviceIds() → doHandle
→ Facade → Service → Rule → DAO/Mapper → XML/SQL → 테이블
```

ServiceId에서 출발:

```text
문자열 전체 검색 → 요청 샘플 → Handler → OM Catalog/통제/Timeout → 거래로그 → 테스트
```

테이블에서 출발:

```text
테이블명 → Mapper XML → SQL ID → Mapper Interface → DAO → Service → Handler → ServiceId → 화면/호출자
```

변경 영향도를 볼 때의 축: 호출자 / ServiceId / Java 계층 / SQL·테이블 / 공통 모듈 / OM 정책 / Cache / 로그·감사 / 테스트 / 배포 WAR.

## 36. 신규 거래 설계 템플릿

```markdown
# 거래명

## 식별자
- 업무코드: / Context/WAR: / ServiceId: / 거래코드: / 처리유형:

## 입출력
- Request: / Response: / 필수값: / 민감정보:

## 업무 흐름
1. / 2. / 3.

## 계층
- Handler: / Facade: / Service/Rule: / DAO/Mapper: / Client:

## 데이터
- DB/테이블: / SQL ID: / 인덱스/예상 건수: / Paging:

## 운영정책
- 기능/데이터 권한: / 거래통제: / Online/TX/DB Timeout: / 멱등성: / 감사로그: / 오류코드:

## 테스트
- 정상/Validation/업무오류: / DB/연동/Timeout/중복/권한:

## 배포
- WAR: / 설정: / OM/Cache: / Smoke: / Rollback:
```

## 37. 개발 완료 체크리스트

설계·등록:

- [ ] 업무코드·Context·WAR·Package 정합
- [ ] ServiceId·거래코드 표준
- [ ] Handler와 Catalog 일치
- [ ] 권한·통제·Timeout·오류 등록

구현:

- [ ] Handler가 얇음
- [ ] Request/Response/DB Row 분리
- [ ] Rule에 입력·업무 검증
- [ ] Facade 트랜잭션 적절
- [ ] Mapper 제한 조회
- [ ] 타 업무 표준 연동
- [ ] 변경 거래 멱등성

품질·운영:

- [ ] 정상·검증·업무·DB·Timeout 테스트
- [ ] GUID·ServiceId·거래코드·SQL ID 추적
- [ ] 개인정보·인증정보 마스킹
- [ ] WAR Context와 Health
- [ ] 배포·Rollback 절차

## 38. 문서 차이 해석

자료 작성시점이 달라 다음 값이 충돌할 수 있다: JDK 17 표기 vs 현재 JDK 21, 업무/관리 WAR 개수, 샘플 거래코드 일련번호, 과거 Package vs 현재 `com.nh.nsight.marketing.*`, bootRun/단일 Tomcat/Cluster 토폴로지, 예시 Timeout vs 환경 정책값.

판단 순서:

1. 현재 `settings.gradle`, `build.gradle`
2. 현재 Java 소스, Mapper XML, `application-*.yml`
3. 루트·모듈 README
4. OM 현재 기준정보와 배포 설정
5. 설계 Markdown의 원칙
6. 집필본·샘플의 예시

예시 계정·주소·수치를 운영값으로 간주하지 않는다.

## 39. 권장 학습 순서

| 단계 | 읽을 것 | 도달 목표 |
|------|---------|-----------|
| 1 | 이 문서 Part I~III, 루트 README, `TCF`·`STF`·`ETF`·`TransactionDispatcher` 소스 | 요청이 어떻게 검증되고 Handler를 찾는지 설명할 수 있다 |
| 2 | `SvCustomerHandler`→`Facade`→`Service`→`Rule`→`Dao`→`Mapper.xml` | `SV.Customer.selectSummary`를 Header에서 SQL까지 추적할 수 있다 |
| 3 | Catalog, 거래통제, Timeout, 오류코드, 거래/감사로그, 세션·권한 (Part IV~V) | 코드가 정상인데 실행되지 않는 이유를 진단할 수 있다 |
| 4 | 조회 거래 1건 직접 추가: Rule·Mapper 테스트 → 표준 전문 통합 테스트 → OM 등록 → Tomcat Smoke | 각 계층을 나눈 이유를 설명하면서 구현할 수 있다 |

## 40. 최종 요약

NSIGHT TCF 개발의 핵심은 Java 클래스를 많이 만드는 것이 아니다. 한 업무 요청을 식별자, 코드, 데이터, 운영정책, 로그, 테스트, 배포까지 끊기지 않게 연결하는 것이다.

```text
업무 요구사항
  ↓ 업무코드 / ServiceId / 거래코드
  ↓ 표준 Header + Body
  ↓ STF 공통 검증과 통제
  ↓ Handler → Facade → Service → Rule → DAO/Mapper
  ↓ ETF 표준 응답과 거래 마감
  ↓ OM 정책 / 로그 / 감사 / 모니터링
  ↓ 테스트 / WAR / 배포 / Rollback
```

항상 세 질문에 답해야 한다.

1. 이 요청은 어떤 ServiceId와 거래코드로 식별되는가?
2. 이 로직은 어느 계층의 책임이며 어디까지가 트랜잭션인가?
3. 실패하거나 느려졌을 때 GUID로 원인과 처리 결과를 확인할 수 있는가?

## 41. 근거 자료

- 소스 확정 근거: `tcf-core`(TCF/STF/ETF/Dispatcher/`InMemoryIdempotencyChecker`/`TcfServiceTimeoutConstants`), `tcf-web`(`PolicyDrivenTransactionAttributeSource`, `PolicyDrivenQueryTimeoutInterceptor`), `eb-service`·`sv-service` 소스와 `application-*.yml`
- 빌드 기준: 루트 `README.md`, `settings.gradle`, `build.gradle`
- 문서 코퍼스: `ztcf-집필본-md`, `zarchitecture`, `zdocs-1`, `zdocs-2`, `zguide`, `zman`, `znsight-man`, `ztcfbook(-h/-m)`, `ztcf-book-capacity-md`, `znsight-config-info`, `znsight-config-value-word`, `ztcf-engine-config-info`, `ztcf-다이어리`
- Word 파일만 존재하거나 비 Word 자료가 없는 폴더는 요약에서 제외했다.
