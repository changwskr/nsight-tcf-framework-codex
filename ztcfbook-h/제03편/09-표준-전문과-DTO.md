# 제9장. 표준 전문과 DTO

| 항목 | 내용 |
| --- | --- |
| **편** | 제3편 · 거래 개발 실무 |
| **에디션** | **Master** — 아키텍트·시니어·플랫폼 |
| **기반 원본** | [ztcfbook/제03편/09-표준-전문과-DTO.md](../ztcfbook/제03편/09-표준-전문과-DTO.md) |
| **입문서** | [ztcfbook-m](../ztcfbook-m/README.md) |
| **장** | 제9장 |
| **파일** | `제03편/09-표준-전문과-DTO.md` |
| **상태** | Master Edition (ztcfbook-h) |
| **목차** | [00-목차](../00-목차.md) |

---

## 아키텍처 뷰

```mermaid
flowchart LR
  REQ[StandardRequest Header+Body] --> STF[검증]
  STF --> BTF[Handler]
  BTF --> RES[StandardResponse Header+Result+Body]
```

---

## Master 해설

StandardRequest(header+body)와 StandardResponse(header+result+body)는 tcf-core message 패키지와 ETF 조립 규칙으로 고정됩니다. Header echo는 clientHeader 스냅샷을 ETF가 처리하므로 Handler에서 header를 수동 덮어쓰면 안 됩니다.

Body는 Map 또는 typed DTO로 Facade 진입 전후 변환할 수 있으나, Validation은 znsight-man DTO 기준과 Hibernate Validator 어노테이션으로 일관되게 적용해야 STF 이후 단계에서 NPE·형변환 오류가 줄어듭니다. Idempotency-Key 헤더는 STF 9단 IdempotencyChecker와 InMemory/DB 구현체에 연동됩니다.

result.success, result.errorCode, result.errorMessage는 ETF 세 경로(success/businessFail/systemError)에서만 설정됩니다. Handler가 errorCode를 직접 Response에 넣는 패턴은 금지입니다.

리뷰 시 부록 D JSON·tcf-ui sample-requests·실제 Handler 입출력 타입을 삼向 대조하고, 중복 요청 시 Idempotency 동작·processing 표시, 오류 시 E-TCF-* vs E-SV-* 코드가 부록 F와 일치하는지 확인하십시오.

---

## 구현 샘플 (코드베이스)

### SV 조회 요청 JSON

```json
{
  "header": {
    "systemId": "NSIGHT-MP",
    "businessCode": "SV",
    "serviceId": "SV.Sample.inquiry",
    "serviceName": "SV 샘플 조회",
    "transactionCode": "SV-INQ-0001",
    "processingType": "INQUIRY",
    "guid": "",
    "traceId": "",
    "channelId": "WEBTOP",
    "userId": "U123456",
    "branchId": "001234",
    "centerId": "DC1",
    "requestTime": "2026-06-14T10:30:00+09:00",
    "transactionIntime": "2026-06-14T10:30:00+09:00",
    "transactionOuttime": "",
    "systemDate": "20260614",
    "bizDate": "20260614",
    "clientIp": "10.10.10.10"
  },
  "body": {
    "pageNo": 1,
    "pageSize": 10,
    "sampleKey": "A00"
  }
}
```

원본: [`tcf-ui/src/main/resources/sample-requests/sv-sample-inquiry.json`](../tcf-ui/src/main/resources/sample-requests/sv-sample-inquiry.json)

### StandardRequest

```java
package com.nh.nsight.tcf.core.support.message;

import java.io.Serializable;

public class StandardRequest<T> implements Serializable {
    private StandardHeader header;
    private T body;

    public StandardRequest() {
    }

    public StandardRequest(StandardHeader header, T body) {
        this.header = header;
        this.body = body;
    }

    public StandardHeader getHeader() { return header; }
    public void setHeader(StandardHeader header) { this.header = header; }
    public T getBody() { return body; }
    public void setBody(T body) { this.body = body; }
}

```

원본: [`tcf-core/src/main/java/com/nh/nsight/tcf/core/support/message/StandardRequest.java`](../tcf-core/src/main/java/com/nh/nsight/tcf/core/support/message/StandardRequest.java)

---

## Master Deep Dive — 표준 전문과 DTO

- 요청 header+body, 응답 header+result+body(+error)
- Header echo — clientHeader 스냅샷 규칙
- Idempotency-Key 헤더와 STF 9단 연동
- Body Map vs typed DTO — Facade에서 변환

### 아키텍트 체크리스트

- 상단 **구현 샘플**을 실제 코드와 대조한다.
- **심화 참고**와 ztcfbook 본문 절 번호를 매핑한다.
- 운영·배포 관점은 ztcfbook-h Master 블록을 우선 본다.

---

## 심화 참고 (Master)

- [znsight-man/20-표준-전문-구조.md](../znsight-man/20-표준-전문-구조.md)
- [znsight-man/부록D-표준-전문-예시.md](../znsight-man/부록D-표준-전문-예시.md)
- [docs/architecture/36-ETF.md](../docs/architecture/36-ETF.md)
- [znsight-man/38-Idempotency-중복요청.md](../znsight-man/38-Idempotency-중복요청.md)

---

## 9.1 Request / Response 전문 구조

표준 전문은 NSIGHT 모든 온라인 거래의 공통 JSON 계약이다. 화면, WebTop, tcf-ui, Gateway, 업무 WAR, OM이 동일한 구조로 요청·응답을 주고받는다.

**StandardRequest** (요청)는 `header`와 `body`로 구성된다.

```json
{
  "header": {
    "guid": "G202607070001",
    "traceId": "T202607070001",
    "transactionId": "TX202607070001",
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "serviceName": "고객요약조회",
    "channelId": "WEBTOP",
    "userId": "U123456",
    "branchId": "001234",
    "clientIp": "10.10.10.10",
    "requestDateTime": "2026-07-07T09:00:00"
  },
  "body": {
    "customerNo": "CUST00000001"
  }
}
```

**StandardResponse** (응답)는 `header`, `result`, `body`, `error`(실패 시)로 구성된다.

```json
{
  "header": {
    "guid": "G202607070001",
    "traceId": "T202607070001",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "businessCode": "SV"
  },
  "result": {
    "code": "SUCCESS",
    "message": "정상 처리되었습니다."
  },
  "body": {
    "customerNo": "CUST00000001",
    "customerName": "홍길동",
    "grade": "VIP",
    "totalAsset": 150000000
  }
}
```

실패 응답 예시:

```json
{
  "header": { "guid": "G202607070001", "serviceId": "SV.Customer.selectSummary" },
  "result": {
    "code": "E-SV-0001",
    "message": "고객번호를 찾을 수 없습니다."
  },
  "error": {
    "detailCode": "E-SV-0001",
    "detailMessage": "customerNo=CUST99999999 not found"
  }
}
```

| 영역 | 역할 |
| --- | --- |
| header | 공통 통제·추적·식별 (STF 검증 대상) |
| body | 업무 데이터 (요청 입력 / 응답 출력) |
| result | 처리 결과 코드·메시지 (ETF 조립) |
| error | 실패 상세 (운영·디버깅용) |

---

## 9.2 Header 작성·검증·오류코드

Header는 공통 통제 정보이며, STF의 `StandardHeaderValidator`가 기동 시 설정된 Allow-List로 검증한다. 업무 개발자가 Header 검증 로직을 Handler에 작성하지 않는다.

필수 Header 항목과 작성 기준:

| 항목 | 작성 주체 | 기준 |
| --- | --- | --- |
| businessCode | 클라이언트(화면) | 요청 WAR BC와 일치 |
| serviceId | 클라이언트 | 실행할 Handler ID |
| transactionCode | 클라이언트 | 설계서 거래코드 |
| channelId | 클라이언트 | OM 공통코드 등록값 |
| userId | 클라이언트 또는 세션 | 로그인 사용자 |
| guid | 클라이언트(권장) 또는 STF 생성 | 멱등성·추적 |
| traceId | 클라이언트(권장) 또는 STF 생성 | E2E 추적 |

검증 실패 시 반환되는 공통 오류코드:

| 오류코드 | 원인 |
| --- | --- |
| E-COM-0001 | Header 필수 항목 누락 |
| E-COM-0002 | businessCode 형식 오류 |
| E-COM-0003 | serviceId와 businessCode 불일치 |
| E-COM-0004 | transactionCode 미등록 |
| E-COM-0005 | channelId 미허용 |

클라이언트(화면 JS)는 거래 호출 전 Header 객체를 조립하는 공통 함수를 사용한다. `serviceId`, `transactionCode`를 하드코딩할 때 설계서와 대조한다. `businessCode`는 WAR Context에서 자동 설정하는 패턴을 권장한다.

---

## 9.3 Body DTO · Validation

Body는 업무별 데이터 영역이다. Handler가 `StandardRequest.getBody()`를 Request DTO로 변환하고, Facade·Service의 결과를 Response DTO로 반환한다.

Request DTO 작성 절차: 설계서 Body 필드 정의 → `*Req` 클래스 생성 → Validation 어노테이션 선언 → Handler에서 `ObjectMapper` 또는 `convertValue`로 변환 → Rule에서 추가 업무 검증.

```java
public class SvCustomerSummaryReq {
  @NotBlank(message = "고객번호는 필수입니다.")
  @Size(max = 20, message = "고객번호는 20자 이내입니다.")
  private String customerNo;
}
```

Validation은 두 단계로 나뉜다. **형식 검증**은 Bean Validation(`@Valid`, `@NotBlank` 등)으로 Request DTO에 선언한다. Handler 또는 Facade 진입 시 `@Valid`로 트리거한다. **업무 규칙 검증**은 Rule 클래스에서 수행한다. "고객 상태가 정지이면 조회 불가" 같은 도메인 규칙이다.

Response DTO는 민감 정보 마스킹, null 필드 제외 정책을 적용한다. Jackson `@JsonInclude(NON_NULL)`을 Response DTO 또는 전역 설정에 적용할 수 있다. 목록 조회 Response는 `List<{Item}Res>`와 페이징 메타(`totalCount`, `pageNo`, `pageSize`)를 포함한다.

---

## 9.4 StandardResponse · ETF 조립

업무 Handler·Facade는 `StandardResponse`를 직접 조립하지 않는다. Facade는 Response DTO(또는 도메인 객체)를 반환하고, **ETF**가 `StandardResponse`를 조립한다.

ETF 조립 흐름:

```text
Handler 반환값 (Response DTO)
  ↓
ETF: result.code = SUCCESS (정상) 또는 BusinessException 코드 (실패)
ETF: result.message = OM 메시지 또는 기본 메시지
ETF: body = Handler 반환값 직렬화
ETF: header = 요청 header 에코 + 서버 보완 항목
  ↓
StandardResponse → JSON HTTP 응답
```

`ProcessingType`에 따라 result 코드 체계가 달라질 수 있다. 조회 성공은 `SUCCESS`, 데이터 없음은 `E-SV-0001`(업무 정의) 등이다. ETF는 `TransactionContext`의 실행 결과와 예외 유무를 판단하여 result를 설정한다.

업무 개발자가 금지하는 패턴:

```java
// 금지: Handler에서 StandardResponse 직접 조립
return StandardResponse.builder()
    .result(Result.success())
    .body(data)
    .build();
```

올바른 패턴:

```java
// Handler: Facade 결과만 반환
return customerFacade.selectSummary(req, context);
```

---

## 9.5 Idempotency · 중복 방지

멱등성(Idempotency)은 동일 `guid`로 중복 요청이 들어왔을 때 업무 로직을 재실행하지 않고 이전 결과를 반환하거나 거부하는 메커니즘이다. STF의 `IdempotencyChecker`가 처리한다.

동작 원리: STF가 `header.guid`를 확인 → IdempotencyChecker에 존재 여부 조회 → 이미 처리 완료된 guid이면 `E-COM-IDEMPOTENT_DUPLICATE` 반환 또는 캐시된 응답 반환 → 미처리 guid이면 "처리 중" 등록 후 Handler 실행 → ETF에서 "완료" 등록.

```text
요청 1: guid=G001 → 처리 → 성공 응답 → guid 완료 등록
요청 2: guid=G001 → STF 멱등성 검사 → 중복 차단 또는 동일 응답
요청 3: guid=G002 → 정상 처리
```

로컬 개발 환경은 `InMemoryIdempotencyChecker`를 사용한다. 운영 환경은 Redis 또는 DB 기반 구현으로 교체한다. 등록·변경·삭제 거래는 멱등성 적용을 **필수**로 설계한다. 조회 거래는 선택적이다.

클라이언트는 등록·변경 요청 시 **매 요청마다 새 guid**를 생성해야 한다. 재시도 시 동일 guid를 재사용하면 중복 방지가 동작한다. 화면 더블클릭 방지(UI debounce)와 서버 멱등성은 함께 적용한다.

---

## 장 요약 (Master)

표준 전문은 StandardRequest(header+body)와 StandardResponse(header+result+body+error) 구조를 따르며, Header는 STF가 검증하고 Body는 Request/Response DTO로 매핑한다. 업무 코드는 StandardResponse를 직접 조립하지 않고 ETF에 위임한다. 등록·변경 거래는 guid 기반 멱등성을 설계에 포함한다.

> Master Edition: **아키텍처 뷰** → **Master 해설** → **구현 샘플** → **Master Deep Dive** → **심화 참고** 순으로 본문과 함께 읽는다.

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [제8장 거래 설계 (설계 단계)](./08-거래-설계.md) |
| → 다음 | [제10장 TransactionHandler 개발](./10-TransactionHandler-개발.md) |

---

## 출처 색인 · Master 확장

| 구분 | 경로 |
| --- | --- |
| ztcfbook-h | 본 파일 |
| ztcfbook | `../ztcfbook/제03편/09-표준-전문과-DTO.md` |

### 원본 출처


- [znsight-man/20-표준-전문-구조.md](../../znsight-man/20-표준-전문-구조.md)
- [znsight-man/부록D-표준-전문-예시.md](../../znsight-man/부록D-표준-전문-예시.md)
- [znsight-man/21-Header-작성-기준.md](../../znsight-man/21-Header-작성-기준.md)
- [znsight-man/명명규칙-21-Header-항목.md](../../znsight-man/명명규칙-21-Header-항목.md)
- [znsight-man/18-DTO-작성-기준.md](../../znsight-man/18-DTO-작성-기준.md)
- [znsight-man/19-Validation-작성-기준.md](../../znsight-man/19-Validation-작성-기준.md)
- [docs/architecture/36-ETF.md](../../docs/architecture/36-ETF.md)
- [docs/architecture/04-messaging.md](../../docs/architecture/04-messaging.md)
- [znsight-man/38-Idempotency-중복요청.md](../../znsight-man/38-Idempotency-중복요청.md)
