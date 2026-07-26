# 제9장. JSON 요청·응답 만들기

| 항목 | 내용 |
| --- | --- |
| **편** | 제3편 |
| **상태** | 집필 완료 |
| **원본** | [ztcfbook 제9장](../ztcfbook/제03편/09-표준-전문과-DTO.md) · [부록 D](../ztcfbook/부록/D-표준-전문-JSON-예시.md) |

---

## 그림으로 보기

```mermaid
flowchart LR
  REQ[StandardRequest Header+Body] --> STF[검증]
  STF --> BTF[Handler]
  BTF --> RES[StandardResponse Header+Result+Body]
```

---

## 9.1 요청 = header + body

**StandardRequest** 는 JSON 객체 **두 칸**입니다.

```json
{
  "header": {
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "processingType": "INQUIRY",
    "channelId": "WEBTOP",
    "userId": "U123456",
    "branchId": "001234"
  },
  "body": {
    "customerNo": "CUST0000001"
  }
}
```

- **header** = 공통 정보 (누가, 어떤 거래, 어떤 기능)
- **body** = 업무 데이터 (고객번호, 검색조건 …)

---

## 9.2 응답 = header + result + body

성공 예:

```json
{
  "header": { "...": "요청과 비슷하게 + responseTime" },
  "result": {
    "resultCode": "S0000",
    "resultStatus": "SUCCESS",
    "resultMessage": "정상 처리되었습니다."
  },
  "body": {
    "customerNo": "CUST0000001",
    "customerName": "홍길동"
  }
}
```

**HTTP 200이어도** `result.resultStatus`가 FAIL이면 **실패**입니다.

---

## 9.3 실패 응답

```json
{
  "result": {
    "resultCode": "E0001",
    "resultStatus": "FAIL",
    "errorCode": "E-SV-BIZ-0001",
    "errorMessage": "조회 결과가 없습니다."
  },
  "body": null
}
```

Handler에서 **에러 JSON을 직접 만들지 않습니다.** 예외를 던지면 ETF가 **이 형식**으로 바꿉니다.

---

## 9.4 목록 조회 body (참고)

```json
"body": {
  "searchCondition": { "campaignName": "우대" },
  "page": { "pageNo": 1, "pageSize": 100 }
}
```

응답 `body.list` + `body.page` 로 돌아옵니다. (23장)

---

## 9.5 ⚠️ 초보자 실수

| 실수 | 대신 |
| --- | --- |
| body만 보냄 | **header 필수** |
| businessCode `sv` 소문자 | **대문자 SV** |
| 성공을 HTTP 404로 | **200 + result** |

---

## 요약

- 요청 **header + body**, 응답 **header + result + body**.
- 성공/실패는 **result** 를 본다.

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [8장 설계 체크리스트](./08-새-거래-설계-체크리스트.md) |
| → 다음 | [10장 Handler](./10-Handler-만드는-법.md) |

---

## 📘 원본에서 더 보기

- [ztcfbook/부록/D-표준-전문-JSON-예시.md](../ztcfbook/부록/D-표준-전문-JSON-예시.md)
