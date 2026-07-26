# 46. 서비스 연동 Contract 설계서

| 항목 | 내용 |
|------|------|
| 문서 번호 | 46 |
| 제목 | Service Integration Contract Design |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [02-junmun.md](02-junmun.md), [04-messaging.md](04-messaging.md), [05-exception.md](05-exception.md), [14-online-arc.md](14-online-arc.md), [44-observability.md](44-observability.md) |
| 상세 매뉴얼 | [31-서비스간-연동-개발.md](../../znsight-man/31-서비스간-연동-개발.md), [zdoc/서비스간연동.md](../../zdoc/서비스간연동.md) |
| 구현 모듈 | `tcf-eai`, 업무 `*-service` `client` 패키지 |
| 대상 | 업무·프레임워크 개발자, 연동 설계자 |

---

## 1. 문서 목적

본 문서는 NSIGHT TCF에서 **WAR(업무 서비스) 간 연동 계약(Contract)** 을 정의한다.

| 구분 | 담당 문서 |
|------|-----------|
| 전문 JSON 스키마 | [02-junmun.md](02-junmun.md) |
| 온라인 파이프라인 | [14-online-arc.md](14-online-arc.md) |
| 연동 개발 절차 | [31-서비스간-연동-개발.md](../../znsight-man/31-서비스간-연동-개발.md) |
| **연동 Contract (본 문서)** | 호출 방식·헤더·오류·Timeout·금지사항 |

핵심 문장:

> 서비스 간 연동은 **내부 Class 호출이 아니라**, 채널과 동일한 **표준 전문 HTTP API** 이다. 호출자·피호출자 모두 `POST /{businessCode}/online` + `serviceId` 계약을 따른다.

---

## 2. 연동 원칙

| # | 원칙 | 설명 |
|---|------|------|
| P1 | **동일 계약** | 외부 채널·UI Relay·서비스 간 호출이 같은 전문·엔드포인트 사용 |
| P2 | **WAR 격리** | 타 업무 WAR의 Java Class·Bean·Mapper **직접 참조 금지** |
| P3 | **DB 격리** | 타 업무 DB 직접 접근 금지 — 필요 데이터는 해당 서비스 API 호출 |
| P4 | **serviceId 라우팅** | URL이 아닌 `header.serviceId`가 Handler 매핑 키 |
| P5 | **상관관계 전파** | `guid`·`traceId`·`callerServiceId` 유지 ([44-observability.md](44-observability.md)) |
| P6 | **분산 TX 금지** | 원격 호출과 로컬 DB를 하나의 트랜잭션으로 묶지 않음 |
| P7 | **조회 우선** | 1차 도입은 **INQUIRY(조회)** 연동. 등록·변경은 별도 승인·설계 |

---

## 3. Transport Contract

### 3.1 엔드포인트

| 항목 | 값 |
|------|-----|
| Method | **POST** |
| Path | `/{businessCode}/online` (또는 context path 포함: `/sv/online`) |
| Content-Type | `application/json; charset=UTF-8` |
| Accept | `application/json; charset=UTF-8` |
| HTTP Status | 성공·실패 모두 **200 OK** (판별은 `result.resultCode`) |

### 3.2 URL 조립 (tcf-eai)

`nsight.integration.services.{BUSINESS_CODE}` 설정으로 조립한다.

```text
{base-url}{context-path}{online-path}
예: http://127.0.0.1:8086/sv/online   (bootRun)
예: http://localhost:8080/sv/online   (ztomcat)
```

구현: `TcfIntegrationProperties.ServiceEndpoint.resolveOnlineUrl()`

### 3.3 bootRun vs ztomcat

| 모드 | 호출 URL 예 (SV) |
|------|------------------|
| bootRun | `http://127.0.0.1:8086/sv/online` |
| ztomcat | `http://localhost:8080/sv/online` |

환경별 `application-{profile}.yml`에 `nsight.integration.services`를 분리 등록한다.

---

## 4. Message Contract

### 4.1 요청 (StandardRequest)

```json
{
  "header": {
    "systemId": "NSIGHT-MP",
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "serviceName": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0002",
    "processingType": "INQUIRY",
    "guid": "원거래-GUID-유지",
    "traceId": "원거래-traceId-유지",
    "channelId": "IC",
    "userId": "user01",
    "branchId": "000",
    "callerBusinessCode": "IC",
    "callerServiceId": "IC.Customer.selectDetail",
    "requestTime": "2026-07-07T12:00:00+09:00",
    "systemDate": "20260707",
    "bizDate": "20260707"
  },
  "body": {
    "customerNo": "C0001"
  }
}
```

| 필드 | 연동 시 규칙 |
|------|-------------|
| `businessCode` | **피호출** 업무코드 (대문자) |
| `serviceId` | **피호출** 거래 ID — OM·Handler에 등록된 값 |
| `transactionCode` | 화면·거래 코드 — OM `OM_SERVICE_CATALOG`와 정합 |
| `processingType` | 기본 `INQUIRY` — tcf-eai `IntegrationCallRequest` 기본값 |
| `guid` / `traceId` | 호출 측 `TransactionContext`에서 **전파** (신규 생성 금지, context null 시만 생성) |
| `callerBusinessCode` / `callerServiceId` | 호출 측 식별 — 감사·추적용 |
| `body` | 피호출 Handler가 기대하는 필드만 포함 |

상세 필드: [02-junmun.md](02-junmun.md) §3

### 4.2 응답 (StandardResponse)

```json
{
  "header": { "guid": "...", "serviceId": "SV.Customer.selectSummary", ... },
  "result": {
    "resultCode": "S0000",
    "message": "정상 처리되었습니다."
  },
  "body": {
    "customerNo": "C0001",
    "summaryText": "..."
  }
}
```

| 판정 | 조건 |
|------|------|
| 성공 | `result.resultCode == "S0000"` |
| 업무 실패 | `resultCode != S0000` — `errorCode`·`message` 참조 |
| 전문 오류 | `result` 노드 없음·JSON 파싱 실패 |

연동 Client(`ResponseResultValidator`)는 **S0000만 성공**으로 처리한다.

---

## 5. 식별자 Contract

### 5.1 serviceId

| 규칙 | 예 |
|------|-----|
| `{업무코드}.{도메인}.{동작}` | `SV.Customer.selectSummary` |
| Dispatcher 라우팅 키 | Handler `@ServiceId` 또는 등록 테이블과 1:1 |
| 미등록 시 | `E-COM-DISP-0001` ([05-exception.md](05-exception.md)) |

### 5.2 businessCode

| 코드 | WAR | context |
|------|-----|---------|
| IC, SV, CC, … | `*-service` | `/ic`, `/sv`, … |
| OM | tcf-om | `/om` |

`serviceId` 첫 토큰과 `businessCode`가 일치해야 한다 (`SV.*` → `SV`).

### 5.3 transactionCode

OM 기준정보·화면·샘플 JSON과 동일 코드 사용. 연동 Adapter 상수로 고정한다.

```java
private static final String TX_CUSTOMER_SUMMARY = "SV-INQ-0002";
```

---

## 6. 구현 아키텍처 (tcf-eai)

### 6.1 계층

```text
[ic-service Handler/Service]
        │
        ▼
SvIntegrationClient          ← 업무별 Adapter (ic-service/client)
        │
        ▼
TcfServiceClient             ← tcf-eai 공통 인터페이스
DefaultTcfServiceClient
  ├─ StandardRequestBuilder   ← header/body 조립
  ├─ HeaderPropagationHelper  ← GUID·caller 전파
  ├─ RestClient POST          ← HTTP 호출
  └─ ResponseResultValidator  ← S0000 검증
        │
        ▼ HTTP POST /sv/online
[sv-service TCF.process() → Handler]
```

### 6.2 모듈 의존

```gradle
implementation project(':tcf-eai')   // ic-service, sv-service 등
```

패키지: `com.nh.nsight.tcf.eai`

| 패키지 | 역할 |
|--------|------|
| `client` | `TcfServiceClient`, `DefaultTcfServiceClient` |
| `config` | `TcfIntegrationProperties`, `TcfIntegrationConfiguration` |
| `model` | `IntegrationCallRequest`, `IntegrationCallResult` |
| `support` | Builder, Header 전파, 응답 검증 |
| `exception` | `IntegrationException`, `IntegrationTimeoutException`, `IntegrationBusinessException` |

### 6.3 Adapter 패턴 (업무 모듈)

업무 서비스는 **도메인 DTO**만 노출하고, `serviceId`·`transactionCode`·body Map 변환은 Adapter에 캡슐화한다.

참고 구현:

| Adapter | 방향 | target serviceId |
|---------|------|------------------|
| `SvIntegrationClient` | IC → SV | `SV.Customer.selectSummary` |
| `IcIntegrationClient` | SV → IC | `IC.Sample.inquiry` |

호출 시 **반드시** `TransactionContext`를 전달하여 상관관계를 유지한다.

```java
Map<String, Object> body = tcfServiceClient.callForBody(
        "SV", "SV.Customer.selectSummary", "SV-INQ-0002",
        Map.of("customerNo", customerNo),
        callerContext);
```

---

## 7. 설정 Contract

### 7.1 application.yml

```yaml
nsight:
  integration:
    default-timeout-ms: 3000
    services:
      SV:
        base-url: http://127.0.0.1:8086
        context-path: /sv
        online-path: /online
        connect-timeout-ms: 1000
        read-timeout-ms: 3000
```

| 속성 | 기본 | 설명 |
|------|------|------|
| `default-timeout-ms` | 3000 | read timeout 미지정 시 |
| `base-url` | — | 호스트·포트 (필수) |
| `context-path` | `""` | WAR context (`/sv`) |
| `online-path` | `/online` | 온라인 엔드포인트 |
| `connect-timeout-ms` | 1000 | TCP 연결 |
| `read-timeout-ms` | default | 응답 대기 |

피호출 서비스가 추가되면 **호출 측 WAR** yml에 endpoint를 등록한다. OM 라우팅 테이블과 별도이다.

---

## 8. 오류 Contract

### 8.1 오류 계층

```text
[피호출 서비스 TCF/ETF]
  업무 오류 → result.resultCode=E0001, errorCode=E-xxx
        │
        ▼
[ResponseResultValidator]
  S0000 아님 → IntegrationBusinessException (resultCode·message·body 보존)
        │
[호출 측 Handler]
  catch → 업무 오류 매핑 또는 상위 전파
```

### 8.2 연동 계층 오류코드 (tcf-eai)

| 코드 | 의미 | 예외 타입 |
|------|------|-----------|
| `E-TCF-IF-0001` | Timeout | `IntegrationTimeoutException` |
| `E-TCF-IF-0002` | 연결 실패·시스템 | `IntegrationException` |
| `E-TCF-MSG-0001` | 응답 전문 파싱 실패 | `IntegrationException` |
| (피호출 `errorCode`) | 업무 실패 | `IntegrationBusinessException` |

호출 측 Handler는 **Timeout / 시스템 / 업무**를 구분해 처리한다. 사용자에게는 [05-exception.md](05-exception.md) 계약에 맞는 `StandardResponse`로 변환한다.

### 8.3 HTTP 4xx/5xx

`/online` 계약 위반이다. tcf-eai는 `ResourceAccessException` 등으로 `E-TCF-IF-0002`에 매핑한다. 정상 피호출은 HTTP 200을 반환해야 한다.

---

## 9. Timeout·Retry·트랜잭션

### 9.1 Timeout

| 구간 | 설정 |
|------|------|
| 연동 read | `nsight.integration.services.*.read-timeout-ms` |
| 온라인 전체 | OM 거래통제·`PolicyDrivenQueryTimeoutInterceptor`와 **별도** — 연동은 더 짧게 설정 권장 |
| connect | 1000ms 기본 |

Timeout 시: `IntegrationTimeoutException` → 호출 측에서 `E-TIMEOUT-*` 또는 연동 실패 메시지로 변환.

### 9.2 Retry

| 허용 | 조건 |
|------|------|
| 제한적 검토 | **INQUIRY** + 멱등 + 피호출 side-effect 없음 |
| 금지 | CREATE/UPDATE/DELETE, 이중 제출 위험 거래 |

Retry 구현 시 **동일 guid** 유지, 최대 1~2회, 지수 백오프. tcf-eai 기본 구현에는 Retry 없음 — Adapter에서 명시적 적용.

### 9.3 트랜잭션 경계

```text
[호출 측 @Transactional]
  ├─ 로컬 DB 작업
  ├─ tcf-eai HTTP 호출  ← 별도 트랜잭션 (원격 커밋 독립)
  └─ 로컬 DB 작업
```

- 원격 성공 후 로컬 실패 → **보상 트랜잭션** 또는 업무 설계로 정합성 확보
- Saga/Outbox는 본 Contract 범위 밖 — 필요 시 별도 설계서

---

## 10. 보안·인증 Contract

| 경로 | 인증 |
|------|------|
| 서버 간 직접 호출 (tcf-eai) | 내부망 전제 — `userId`·`branchId` header 전파 |
| Gateway 경유 | JWT/세션 — Gateway 검증 후 downstream Relay |
| JWT 모드 업무 WAR | `TcfJwtAuthenticationFilter` — 내부 호출 시 Bearer 전파 정책은 환경별 설정 ([42-jwt.md](42-jwt.md)) |

내부 연동 URL은 **외부 노출 금지** (방화벽·Apache 내부 라우팅).

---

## 11. 로깅·추적 Contract

### 11.1 tcf-eai 로그

```
[EAI] CALL start caller={callerServiceId} target={targetServiceId} url=... guid=...
[EAI] CALL end   caller=... target=... resultCode=S0000 elapsedMs=...
[EAI] CALL timeout / system-error / error ...
```

### 11.2 양쪽 TCF_TX_LOG

| 측 | 로그 |
|----|------|
| 호출 측 | 원 거래 `TCF_TX_LOG` (연동 호출 전후) |
| 피호출 측 | **독립** `TCF_TX_LOG` row — 동일 `guid`로 상관 |

조회: `guid` + `callerServiceId` / `serviceId` ([44-observability.md](44-observability.md))

---

## 12. 신규 연동 등록 체크리스트

피호출 서비스(SV)에 `SV.Domain.action` 추가 시:

- [ ] OM `OM_SERVICE_CATALOG` — serviceId·transactionCode 등록
- [ ] Handler 구현·Dispatcher 등록
- [ ] Body 필드 — `OM_MESSAGE_STRUCT` / 샘플 JSON
- [ ] 호출 측 WAR — `nsight.integration.services.{SV}` endpoint
- [ ] `{X}IntegrationClient` Adapter + DTO
- [ ] `processingType` — INQUIRY vs 변경 거래 승인
- [ ] Timeout 값 — 피호출 P95 + 여유
- [ ] Smoke — `tcf-ui` sample-requests 또는 curl
- [ ] 장애 시 — 호출 측 fallback·오류 메시지 정의

---

## 13. 호환성·버전

| 변경 유형 | 호환성 | 조치 |
|-----------|--------|------|
| body 필드 **추가** (optional) | 하위 호환 | 피호출 먼저 배포 |
| body 필드 **삭제·rename** | 비호환 | 양쪽 WAR 동시 배포 또는 serviceId 버전 분리 |
| serviceId 변경 | 비호환 | 신규 serviceId 등록, 구 ID deprecated 기간 운영 |
| resultCode 의미 변경 | 비호환 | OM 오류코드·문서 갱신 |

`serviceId`에 버전 접미사(`SV.Customer.selectSummary.v2`)는 최후 수단 — OM catalog 정리 우선.

---

## 14. 검증·테스트

| 수준 | 방법 |
|------|------|
| 단위 | `tcf-eai` — Builder·Validator 테스트 |
| 통합 | ic + sv bootRun 동시 기동 → Handler → IntegrationClient E2E |
| ztomcat | `deploy-wars` 후 context path URL로 연동 yml 수정 |
| 회귀 | sample-requests JSON + Smoke serviceId |

---

## 15. 관련 소스

| 경로 | 설명 |
|------|------|
| `tcf-eai/.../DefaultTcfServiceClient.java` | HTTP 호출 구현 |
| `tcf-eai/.../StandardRequestBuilder.java` | 요청 전문 조립 |
| `tcf-eai/.../HeaderPropagationHelper.java` | GUID·caller 전파 |
| `tcf-eai/.../ResponseResultValidator.java` | S0000 검증 |
| `ic-service/.../SvIntegrationClient.java` | IC→SV 샘플 |
| `sv-service/.../IcIntegrationClient.java` | SV→IC 샘플 |
| `ic-service/.../application-local.yml` | integration 설정 예 |

---

← [45-disaster-recovery.md](45-disaster-recovery.md) · [47-data-governance.md](47-data-governance.md) →
