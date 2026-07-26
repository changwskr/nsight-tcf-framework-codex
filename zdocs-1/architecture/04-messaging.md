# 04. 메시지 처리 아키텍처

| 항목 | 내용 |
|------|------|
| 문서 번호 | 04 |
| 제목 | Message Processing Architecture |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [02-junmun.md](02-junmun.md), [03-transaction.md](03-transaction.md), [01-application-layer.md](01-application-layer.md) |
| 구현 모듈 | `tcf-core`, `tcf-web`, `tcf-ui` |
| 대상 | 채널·업무·프레임워크 개발자 |

---

## 1. 개요

NSIGHT TCF에서 **메시지 처리(Message Processing)** 란, 채널에서 작성된 JSON 전문이 WAS에 도달해 **역직렬화·보완·검증·라우팅·응답 조립·직렬화**를 거쳐 클라이언트로 돌아가기까지의 **엔드투엔드 흐름**을 말한다.

| 문서 | 초점 |
|------|------|
| [02-junmun.md](02-junmun.md) | 전문 **구조**(필드·JSON 스키마) |
| [03-transaction.md](03-transaction.md) | 거래 **파이프라인**(STF/TCF/ETF·Context·로그) |
| **본 문서** | 메시지 **이동·변환·채널·진입점** |

```text
┌──────────┐   HTTP/JSON    ┌─────────────┐   Java 객체    ┌──────────┐
│  Channel │ ────────────► │  Transport  │ ─────────────► │ Semantic │
│ (Browser │               │ Deserialize │                │ STF/ETF  │
│  curl…)  │ ◄──────────── │ Serialize   │ ◄───────────── │ Handler  │
└──────────┘   HTTP/JSON    └─────────────┘   StandardResp └──────────┘
```

현재 프레임워크는 **동기 HTTP/JSON** 기반이다. Kafka·JMS 등 비동기 메시지 브로커는 코어에 포함되지 않으며, 확장 지점으로만 언급한다.

---

## 2. 메시지 처리 4계층

### 2.1 계층 정의

| 계층 | 담당 | 주요 기술 |
|------|------|-----------|
| **Transport** | HTTP 전송, 인코딩, 상태 코드 | Spring MVC, `Content-Type: application/json;charset=UTF-8` |
| **Serialization** | JSON ↔ Java 객체 | Jackson (`StandardRequest`, `StandardResponse`) |
| **Semantic** | Header/Body/Result 계약, 검증·조립 | `tcf-core.message`, STF, ETF |
| **Adaptation** | 비표준 진입·릴레이·REST 래핑 | `TcfGateway`, `TransactionRelayService`, UD REST |

### 2.2 계층별 책임

```text
Transport     POST /{code}/online, Cookie, X-Forwarded-For
     │
Serialization @RequestBody StandardRequest → Jackson 역직렬화
     │          StandardResponse → Jackson 직렬화 (HTTP 200)
     │
Semantic      header.normalize(), guid 부여, result 조립
     │
Adaptation    (선택) REST/multipart → StandardRequest 변환
              (선택) tcf-ui Relay → 업무 WAR로 프록시
```

---

## 3. 진입점 유형

모든 온라인 거래는 최종적으로 `TCF.process(StandardRequest)`에 수렴하거나, TCF와 **유사한 응답 형태**로 클라이언트에 반환된다.

| 유형 | 진입 컴포넌트 | 입력 형태 | TCF 파이프라인 |
|------|---------------|-----------|----------------|
| **A. 표준 JSON** | `OnlineTransactionController` | `StandardRequest` JSON | ● 전체 (STF→ETF) |
| **B. 프로그램 위임** | `TcfGateway.invoke()` | `TcfInvokeRequest` (Java) | ● 전체 |
| **C. UI 릴레이** | `TransactionRelayService` | JSON 문자열 (투명 전달) | ○ 대상 WAR에서 처리 |
| **D. 하이브리드 REST** | `OmUpdownloadFileController` 등 | multipart/REST | ✕ (TCF 미경유, 응답만 유사 형태) |

### 3.1 A. 표준 JSON 진입

```text
POST /online
POST /{businessCode}/online
Content-Type: application/json; charset=UTF-8

{ "header": { ... }, "body": { ... } }
```

`OnlineTransactionController`가 수행하는 **메시지 보완**:

| 보완 | 조건 |
|------|------|
| 빈 `header` 생성 | `header == null` |
| `businessCode` 설정 | URL path에 있고 Header에 없을 때 |
| `clientIp` 설정 | Header 비어 있을 때 `X-Forwarded-For` / `remoteAddr` |

이후 `@RequestBody`로 역직렬화된 `StandardRequest`가 `TCF.process()`로 전달된다.

### 3.2 B. TcfGateway — 어댑터 진입

REST Controller·multipart API 등 **JSON 전문을 직접 받지 않는** 코드에서 TCF 파이프라인을 재사용한다.

```java
StandardResponse<Object> response = tcfGateway.invoke(
    TcfGateway.TcfInvokeRequest.builder("UD.File.list", "UD-LST-0001", "INQUIRY")
        .body(bodyMap)
        .userId(userId)
        .clientIp(clientIp)
        .build()
);
```

`buildHeader()`가 조립하는 Header 기본값:

| 필드 | 값 |
|------|-----|
| `systemId` | `NSIGHT-MP` |
| `businessCode` | serviceId 첫 토큰 (`UD.File.list` → `UD`) |
| `channelId` | `WEBTOP` (미지정 시) |

→ `StandardRequest` 생성 후 `TCF.process()`와 **동일 파이프라인**.

### 3.3 C. tcf-ui 릴레이 — 투명 프록시

브라우저·테스트 UI는 업무 WAR에 직접 붙지 않고 `tcf-ui`를 경유한다.

```text
Browser
  │ POST /api/relay/{code}/online  (StandardRequest JSON)
  ▼
TcfApiController
  ▼
TransactionRelayService.relay()
  │ RestClient POST → {targetUrl}/online
  │ Cookie 헤더 전달, Set-Cookie 역전달
  ▼
업무 WAR OnlineTransactionController → TCF.process()
  ▼
RelayResult { httpStatus, elapsedMs, responseBody, setCookies }
  ▼
Browser — responseBody를 JSON.parse → result.resultCode 판별
```

**릴레이는 메시지를 변환하지 않는다.** 요청 JSON을 그대로 전달하고, 응답 JSON을 `RelayResult.responseBody`에 담아 반환한다.

대상 URL 해석 (`TransactionRelayService.resolveTargetUrl`):

| deploymentMode | targetUrl 예 |
|----------------|--------------|
| `bootrun` | `http://127.0.0.1:{localPort}/{contextPath}/online` |
| `tomcat` | `http://localhost:8080/{contextPath}/online` |

OM Admin (`om-admin.js`)은 `POST /api/relay/OM/online`을 사용한다.

### 3.4 D. 하이브리드 REST — UD 파일 API

파일 업·다운로드는 **multipart·바이너리** 특성상 TCF JSON 파이프라인을 타지 않는다.

```text
tcf-ui UpdownloadRelayService
  → tcf-om /ud/files/*  (REST)
  → OmUpdownloadResponseSupport 로 header/result/body 형태 Map 조립
```

`OmUpdownloadResponseSupport.success()`는 `StandardResponse`와 **유사한 JSON 구조**를 수동 조립한다.  
단, TCF STF/ETF·거래로그·멱등성 검사는 적용되지 않는다.

---

## 4. 메시지 생명주기 (표준 경로)

### 4.1 전체 흐름

```text
[1] Compose     채널에서 header + body 작성
[2] Send        HTTP POST, UTF-8 JSON
[3] Receive     Spring DispatcherServlet
[4] Deserialize Jackson → StandardRequest<Map<String,Object>>
[5] Enrich      Controller — businessCode, clientIp
[6] Validate    STF — StandardHeaderValidator, normalize()
[7] Augment     STF — guid, traceId, MDC
[8] Route       Dispatcher — serviceId → Handler
[9] Process     Handler — body 추출, Facade/Service 처리
[10] Assemble   ETF — StandardResponse.success/fail
[11] Serialize  Jackson → JSON
[12] Respond    HTTP 200 + StandardResponse
[13] Parse      클라이언트 — result.resultCode == "S0000"
```

### 4.2 시퀀스 (표준 + 릴레이)

```text
Client(UI)     tcf-ui Relay      업무 WAR           TCF
    │               │                │                │
    │ relay JSON    │                │                │
    ├──────────────►│ POST /sv/online│                │
    │               ├───────────────►│ deserialize    │
    │               │                ├───────────────►│ STF→Handler→ETF
    │               │                │◄───────────────┤ StandardResponse
    │               │◄───────────────┤ JSON           │
    │◄──────────────┤ RelayResult    │                │
    │ parse payload │                │                │
```

### 4.3 요청 메시지 변환 포인트

| 단계 | 입력 | 출력 | 변환 내용 |
|------|------|------|-----------|
| Jackson 역직렬화 | JSON 문자열 | `StandardRequest` | snake_case 없음, camelCase 필드 |
| `header.normalize()` | 원본 Header | 정규 Header | `systemId`, `requestTime` 기본값, 대문자 변환 |
| guid/traceId 부여 | 빈 guid | 채워진 Header | `GuidGenerator` |
| Handler | `request.getBody()` | `Map` | body만 업무 계층으로 전달 |
| ETF | Service 반환값 | `StandardResponse` | `Result` + body 조립 |

### 4.4 응답 메시지 조립

ETF가 호출하는 팩토리:

```java
// 성공
StandardResponse.success(header, body);
// → result.resultCode = "S0000"

// 실패
StandardResponse.fail(header, errorCode, message, detail);
// → result.resultCode = "E0001", result.errorCode = errorCode
```

응답 Header는 요청 Header의 **guid·traceId를 유지**한다 (Context 또는 request Header 참조).

---

## 5. 채널별 메시지 작성 패턴

### 5.1 채널 분류

| channelId | 채널 | 메시지 작성 위치 |
|-----------|------|------------------|
| `WEBTOP` | 거래 테스트 UI | `online-multi.js`, sample-requests JSON |
| `OM-PORTAL` | OM Admin | `om-admin.js` — `buildStandardHeader()` |
| (외부) | curl·Postman | `tcf-ui/sample-requests/*.json` |
| (서버) | TcfGateway | Java `TcfInvokeRequest` |

### 5.2 OM Admin 메시지 조립 (`om-admin.js`)

```javascript
// 표준 Header 빌드
function buildStandardHeader(options) {
  return {
    systemId: 'NSIGHT-MP',
    businessCode: 'OM',
    serviceId: options.serviceId,
    transactionCode: options.transactionCode,
    processingType: 'INQUIRY',
    guid: newGuid(),
    channelId: 'WEBTOP',
    userId: session.userId || 'GUEST',
    requestTime: nowIsoKst(),
    ...
  };
}

// 거래 호출
async function call(txKey, body, processingType) {
  const request = { header: buildHeader(TX[txKey], processingType), body };
  // → /api/relay/OM/online
}
```

`TX` 맵에 serviceId·transactionCode가 등록되어 있어 **화면 코드와 전문 Header가 1:1 매핑**된다.

### 5.3 거래 테스트 UI (`online-multi.js`)

```text
GET /api/multi/business-modules/{code}/transactions/{id}  → 샘플 JSON 로드
POST /api/multi/relay/{code}/online                       → 릴레이 호출
```

`BusinessTransactionCatalog`가 업무별 샘플 전문 경로를 제공한다.

### 5.4 공통 전문 조립기

OM Admin `message-composer.html`에서 Header·Body를 수동 편집 후 동일 릴레이 API로 전송한다.  
신규 거래 검증·디버깅용 **범용 메시지 에디터** 역할.

---

## 6. 직렬화·역직렬화

### 6.1 Java 모델

| 클래스 | JSON 루트 필드 | Body 타입 |
|--------|----------------|-----------|
| `StandardRequest<T>` | `header`, `body` | 업무: `Map<String, Object>` |
| `StandardResponse<T>` | `header`, `result`, `body` | 업무: `Map` 또는 `null`(실패 시) |
| `StandardHeader` | `header` 내부 | — |
| `Result` | `result` 내부 | — |

모든 메시지 클래스는 `Serializable`을 구현한다.

### 6.2 Body 관례

| 방향 | 타입 | 설명 |
|------|------|------|
| 요청 | `Map<String, Object>` | 화면·채널별 유연 필드, Jackson이 중첩 Object/List 지원 |
| 응답 | Service가 조립한 `Map` | 목록 조회 시 `items`, `totalCount` 등 관례적 키 |
| 실패 | `body` 생략 가능 | `result.errorCode`, `result.errorMessage`로 판별 |

Handler는 `request.getBody()`만 Facade에 넘기고, **Header는 `TransactionContext`로 접근**한다.

### 6.3 ProcessingType과 메시지 의미

`ProcessingType` enum은 Header의 `processingType` 문자열과 대응한다.

| 값 | 메시지 의미 |
|----|-------------|
| `INQUIRY` | 조회 — body에 검색 조건 |
| `CREATE` | 등록 |
| `UPDATE` | 수정 |
| `DELETE` | 삭제 |
| `EXECUTE` | 실행(로그인·배치 트리거 등) |
| `DOWNLOAD` / `UPLOAD` | 파일 전송 (UD REST와 병행 사용) |

processingType은 Dispatcher 라우팅 키가 **아니며**, 감사·로그·업무 규칙에서 참조한다. 라우팅 키는 `serviceId`이다.

---

## 7. 오류 메시지 처리

메시지 처리 관점에서 오류는 **3계층**으로 반환된다.

### 7.1 TCF 파이프라인 내부 (정상 경로)

```text
BusinessException / Exception
  → ETF.businessFail() / systemError()
  → StandardResponse { result: { resultCode: "E0001", errorCode, errorMessage } }
  → HTTP 200
```

### 7.2 Controller 밖 (파이프라인 진입 전)

`GlobalStandardExceptionHandler`:

| 예외 | 반환 |
|------|------|
| `MethodArgumentNotValidException` | `E-COM-VALID-0001` |
| `HttpRequestMethodNotSupportedException` | GET 접근 차단 안내 |
| 기타 | `E-COM-SYS-0001` |

Header가 없을 수 있어 `StandardResponse.fail(null, ...)` 형태.

### 7.3 릴레이·연결 오류

`TransactionRelayService` 연결 실패 시 **비표준 JSON**:

```json
{"error":"...","targetUrl":"...","hint":"대상 WAS(포트 8096)가 기동 중인지 확인하세요."}
```

클라이언트(`parseRelayResponse`)는 `RelayResult.responseBody` 유무와 `httpStatus`로 1차 판별한다.

### 7.4 클라이언트 성공 판별 규칙

```text
1. relay.httpStatus < 400  (릴레이 사용 시)
2. payload.result.resultCode === "S0000"
3. (선택) payload.body 비즈니스 플래그
```

HTTP 상태만으로 성공을 판단하지 않는다.

---

## 8. 릴레이 상세 아키텍처

### 8.1 RelayResult

```java
public record RelayResult(
    String businessCode,
    String targetUrl,
    int httpStatus,
    long elapsedMs,
    String responseBody,   // 업무 WAR가 반환한 JSON 원문
    List<String> setCookies
) {}
```

UI는 `responseBody`를 파싱해 실제 `StandardResponse`를 얻는다.  
`elapsedMs`는 릴레이 구간 RTT이며, 업무 WAS 내부 소요 시간과는 다르다.

### 8.2 Cookie·세션 전파

| 방향 | 처리 |
|------|------|
| 요청 | Browser `Cookie` → Relay `HttpHeaders.COOKIE` |
| 응답 | 업무 `Set-Cookie` → `TcfApiController.applySetCookies()` |

OM 로그인(`OM.Auth.login`) 시 Spring Session 쿠키가 이 경로로 전파된다.

### 8.3 배포 모드

`nsight.tcf-ui.deployment-mode`:

| 값 | 릴레이 대상 |
|----|-------------|
| `bootrun` | `bootrunHost:localPort` |
| `tomcat` | `tomcatGatewayUrl/{contextPath}/online` |

쿼리 파라미터 `deploymentMode`, `bootrunHost`, `tomcatGatewayUrl`로 런타임 오버라이드 가능.

### 8.4 UD 전용 릴레이

`UpdownloadRelayService`는 JSON 전문 릴레이가 아닌 **REST 프록시**:

| UI API | 대상 |
|--------|------|
| `POST /api/updownload/upload` | `POST /om/ud/files/upload` (multipart) |
| `GET /api/updownload/files` | `GET /om/ud/files` |
| `GET .../download` | 바이너리 스트림 |

Tomcat 모드: `tomcatGatewayUrl + "/om"`.  
bootRun 모드: `tcf-om:8097`.

---

## 9. 로깅·추적과 메시지

### 9.1 MDC 연동

STF가 Header에서 MDC를 적재한다.

```text
guid, traceId, serviceId, userId, branchId
```

`GuidMdcCleanupFilter`와 TCF `finally`가 요청 종료 시 `MDC.clear()` — 로그 라인에 거래 키가 자동 포함된다.

### 9.2 메시지 ↔ 거래로그

STF `start()` / ETF `end()`가 `TCF_TX_LOG`에 메시지 Header 핵심 필드를 적재한다.  
상세: [03-transaction.md §10](03-transaction.md).

---

## 10. 배포 토폴로지별 메시지 경로

### 10.1 bootRun (개발)

```text
tcf-ui :8085  ─relay─►  sv-service :8086/sv/online
om-admin.js ─relay─►  tcf-om :8097/om/online
```

### 10.2 Tomcat ztomcat (통합)

```text
Browser → http://localhost:8080/ui/api/relay/SV/online
       → http://localhost:8080/sv/online
       → sv.war TCF.process()
```

단일 게이트웨이(8080)에서 context path로 업무 분기.

### 10.3 직접 호출 (연동 테스트)

```bash
curl -X POST http://localhost:8080/sv/online \
  -H "Content-Type: application/json" \
  -d @tcf-ui/src/main/resources/sample-requests/sv-sample-inquiry.json
```

릴레이 없이 **표준 경로 A**만 사용.

---

## 11. 문서·코드 역할 분담

```text
02-junmun.md     "무엇을 보내는가"  — 필드 정의, 샘플 JSON
04-messaging.md  "어떻게 흐르는가"  — 채널, 릴레이, 변환, 오류
03-transaction.md "어떻게 처리되는가" — STF/ETF, Context, 멱등성
01-application-layer.md "누가 처리하는가" — Handler 이후 계층
```

---

## 12. 확장 지점

| 영역 | 현재 | 확장 방향 |
|------|------|-----------|
| Transport | HTTP 동기 | gRPC, WebSocket |
| Adaptation | `TcfGateway` | 외부 XML/SOAP → StandardRequest 변환기 |
| Relay | RestClient 동기 | 로드밸런서·서킷브레이커 |
| Async | 없음 | Kafka Consumer → `TCF.process()` 위임 |
| Body 타입 | `Map` | DTO 클래스 + Bean Validation (선택) |

---

## 13. 참고 소스 (읽는 순서)

| 순서 | 파일 |
|------|------|
| 1 | `tcf-web/.../controller/OnlineTransactionController.java` |
| 2 | `tcf-web/.../gateway/TcfGateway.java` |
| 3 | `tcf-core/.../message/StandardRequest.java`, `StandardResponse.java` |
| 4 | `tcf-core/.../validation/StandardHeaderValidator.java` |
| 5 | `tcf-web/.../exception/GlobalStandardExceptionHandler.java` |
| 6 | `tcf-ui/.../service/TransactionRelayService.java` |
| 7 | `tcf-ui/.../controller/TcfApiController.java` |
| 8 | `tcf-ui/.../static/_shared/om-admin.js` |
| 9 | `tcf-om/.../updownload/support/OmUpdownloadResponseSupport.java` |

---

## 14. 관련 문서

| 문서 | 설명 |
|------|------|
| [architecture.md](architecture.md) | 전체 아키텍처 |
| [02-junmun.md](02-junmun.md) | 표준 전문 구조 |
| [03-transaction.md](03-transaction.md) | 트랜잭션 파이프라인 |
| [05-exception.md](05-exception.md) | 예외 처리 표준 |
| [01-application-layer.md](01-application-layer.md) | Handler 이후 계층 |
| [tcf-ui/sample-requests](../../tcf-ui/src/main/resources/sample-requests/) | 업무별 샘플 전문 |

---

## 15. 변경 이력

| 일자 | 변경 내용 |
|------|-----------|
| 2026-06 | 최초 작성 — 4계층·진입점·릴레이·채널 패턴 |
