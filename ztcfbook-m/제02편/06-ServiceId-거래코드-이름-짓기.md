# 제6장. ServiceId·거래코드 이름 짓기

| 항목 | 내용 |
| --- | --- |
| **편** | 제2편 |
| **상태** | 집필 완료 |
| **원본** | [ztcfbook 제6장](../ztcfbook/제02편/06-식별자-명명규칙.md) · [부록 B](../ztcfbook/부록/B-ServiceId-명명규칙.md) |

---

## 그림으로 보기

```mermaid
flowchart LR
  SID["ServiceId BC.Domain.action"] --> DISPATCH[Dispatcher]
  TXC["거래코드 BC-TYPE-NNNN"] --> CTRL[거래통제/감사]
```

---

## 6.1 ServiceId — 세 덩어리

```text
SV . Customer . selectSummary
│     │           └─ 뭘 한다 (동사, camelCase)
│     └─ 대상 (Customer, Campaign …)
└─ 업무코드 (대문자 2자)
```

| 맞는 예 | 틀린 예 | 이유 |
| --- | --- | --- |
| `SV.Customer.selectSummary` | `selectCustomer` | 업무코드 없음 |
| `OM.User.inquiry` | `OM.User.manage` | 행동이 **모호** |
| `MG.Message.send` | `MG.SendMessage.send` | 중복·지저분 |

### 자주 쓰는 행동(마지막 칸)

| 행동 | 언제 |
| --- | --- |
| `selectList` | 목록 |
| `selectDetail` | 상세 1건 |
| `selectSummary` | 요약 |
| `save` | 등록+수정 한 화면 |
| `create` / `update` | 등록·수정 **분리** |
| `send` | 메시지 발송 |

---

## 6.2 거래코드 — 로그에 남는 번호

```text
SV - INQ - 0001
│    │      └─ 일련번호 (4자리)
│    └─ 유형 (3글자)
└─ 업무코드
```

| 유형 | 뜻 | 예 |
| --- | --- | --- |
| INQ | 조회 | SV-INQ-0001 |
| CRT | 등록 | CM-CRT-0001 |
| UPD | 수정 | CM-UPD-0001 |
| SND | 발송 | MG-SND-0001 |
| ADM | 운영 | OM-ADM-0001 |

**serviceId vs 거래코드**

| | serviceId | 거래코드 |
| --- | --- | --- |
| 역할 | **어떤 프로그램** 실행? | **어떤 거래**로 기록? |
| 예 | `SV.Customer.selectSummary` | `SV-INQ-0001` |

화면·JSON **header 둘 다** 넣어서 보냅니다.

---

## 6.3 ⚠️ 초보자 실수

| 실수 | 올바른 방법 |
| --- | --- |
| URL에 기능 넣기 `/sv/customer/summary` | `/sv/online` + serviceId |
| 거래코드 소문자 `sv-inq-0001` | **대문자** `SV-INQ-0001` |
| 같은 INQ 번호 재사용 | **새 거래 = 새 번호** |

---

## 요약

- **ServiceId** = `업무.대상.행동`
- **거래코드** = `업무-유형-번호`
- 둘 다 header에 넣고, **OM Catalog**에 등록

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [5장 개발 표준](./05-개발-표준-한-줄-로-이어지게.md) |
| → 다음 | [7장 클래스·패키지](./07-클래스-패키지-이름-규칙.md) |

---

## 📘 원본에서 더 보기

- [ztcfbook/부록/B-ServiceId-명명규칙.md](../ztcfbook/부록/B-ServiceId-명명규칙.md)
- [ztcfbook/부록/C-거래코드-명명규칙.md](../ztcfbook/부록/C-거래코드-명명규칙.md)
