# 부록 D. JSON 예시 모음

| **부록** | D |
| **상태** | 집필 완료 |
| **원본** | [ztcfbook 부록 D](../ztcfbook/부록/D-표준-전문-JSON-예시.md) |

---

## 그림으로 보기

```mermaid
flowchart LR
  REQ["Request JSON"] --> API["POST /online"]
  API --> RES["Response success/fail"]
```

---

## 조회 요청 (SV)

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

## 성공 응답 (요약)

```json
{
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

## 실패 응답 (요약)

```json
{
  "result": {
    "resultStatus": "FAIL",
    "errorCode": "E-SV-BIZ-0001",
    "errorMessage": "조회 결과가 없습니다."
  },
  "body": null
}
```

오류·Timeout·권한 JSON 전체는 **원본 부록 D** 참고.

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [부록 C 거래코드](./C-거래코드-한-줄.md) |
| → 다음 | [부록 E Mapper XML](./E-Mapper-XML-기본.md) |

---

## 📘 원본

- [ztcfbook/부록/D-표준-전문-JSON-예시.md](../ztcfbook/부록/D-표준-전문-JSON-예시.md)
