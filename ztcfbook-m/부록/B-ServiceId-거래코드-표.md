# 부록 B. ServiceId·거래코드 표

| **부록** | B |
| **상태** | 집필 완료 |
| **원본** | [부록 B](../ztcfbook/부록/B-ServiceId-명명규칙.md) · [부록 C](../ztcfbook/부록/C-거래코드-명명규칙.md) |

---

## 그림으로 보기

```mermaid
flowchart LR
  FMT["BC.Domain.action"] --> REG[Handler.serviceIds]
  REG --> OM[OM Catalog]
```

---

## ServiceId 형식

```text
{업무코드}.{대상}.{행동}
SV.Customer.selectSummary
```

## 거래코드 형식

```text
{업무코드}-{유형}-{번호}
SV-INQ-0001
```

상세·processingType → [부록 C](./C-거래코드-한-줄.md)

## 자주 쓰는 예

| serviceId | 거래코드 | 설명 |
| --- | --- | --- |
| SV.Customer.selectSummary | SV-INQ-0001 | 고객 요약 |
| CM.Campaign.selectList | CM-INQ-0001 | 캠페인 목록 |
| CM.Campaign.create | CM-CRT-0001 | 캠페인 등록 |
| MG.Message.send | MG-SND-0001 | 메시지 발송 |
| OM.User.inquiry | OM-ADM-0001 | OM 사용자 조회 |

## 유형 코드

| 코드 | 뜻 |
| --- | --- |
| INQ | 조회 |
| CRT | 등록 |
| UPD | 수정 |
| DEL | 삭제 |
| SND | 발송 |
| ADM | 운영 |

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [부록 A 업무코드](./A-업무코드-표.md) |
| → 다음 | [부록 C 거래코드](./C-거래코드-한-줄.md) |

---

## 📘 원본

- [ztcfbook/부록/B-ServiceId-명명규칙.md](../ztcfbook/부록/B-ServiceId-명명규칙.md)
- [ztcfbook/부록/C-거래코드-명명규칙.md](../ztcfbook/부록/C-거래코드-명명규칙.md)
