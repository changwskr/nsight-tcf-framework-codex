# 부록 F. 오류코드 형식

| **부록** | F |
| **상태** | 집필 완료 |
| **원본** | [ztcfbook 부록 F](../ztcfbook/부록/F-오류코드-표준표.md) |

---

## 그림으로 보기

```mermaid
flowchart LR
  BE[BusinessException] --> ETF[ETF Result]
  ETF --> OM[OM ErrorCode CRUD]
```

---

## 형식 — 한 줄

```text
E-{DOMAIN}-{CATEGORY}-{NNNN}
```

| 예 | 의미 |
| --- | --- |
| `E-TCF-HDR-0001` | Header 검증 실패 |
| `E-SV-BIZ-0001` | SV 업무 오류 |
| `E-COM-VAL-0001` | 공통 Validation |
| `E-OM-AUTHZ-0001` | OM 권한 없음 |

---

## DOMAIN (자주 쓰는 것)

| DOMAIN | 어디 |
| --- | --- |
| TCF | STF·Dispatcher·통제 |
| COM | 공통 Validator |
| SV, IC, … | **업무 WAR** (BC와 같음) |
| OM | 운영 Admin |

---

## CATEGORY (대표)

| CATEGORY | 의미 |
| --- | --- |
| HDR | Header |
| VAL | Validation |
| BIZ | 업무 규칙 |
| DB | DB 오류 |
| AUTH / AUTHZ | 인증·권한 |
| TIMEOUT | Timeout |

---

## 코드에서

```java
throw new BusinessException("E-SV-BIZ-0001", "고객번호가 없습니다.");
```

메시지는 **OM_ERROR_CODE**에도 등록 (운영).

---

## ⚠️ 초보자 실수

| 실수 | |
| --- | --- |
| `RuntimeException("오류")` | **추적 불가** |
| `ERROR01` 임의 문자열 | **형식 위반** |
| 오류코드만 던지고 OM 미등록 | 운영 **메시지 없음** |

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [부록 E Mapper](./E-Mapper-XML-기본.md) |
| → 다음 | [21장 테스트](../제07편/21-테스트-어떻게-하나.md) |

---

## 📘 원본

- [ztcfbook/부록/F-오류코드-표준표.md](../ztcfbook/부록/F-오류코드-표준표.md)
