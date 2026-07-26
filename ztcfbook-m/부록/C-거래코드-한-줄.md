# 부록 C. 거래코드 한 줄

| **부록** | C |
| **상태** | 집필 완료 |
| **원본** | [ztcfbook 부록 C](../ztcfbook/부록/C-거래코드-명명규칙.md) |

---

## 그림으로 보기

```mermaid
flowchart LR
  TX["BC-TYPE-NNNN"] --> HDR[Header transactionCode]
  HDR --> CTRL[거래통제·감사]
```

---

## serviceId vs 거래코드

| | 역할 | 예 |
| --- | --- | --- |
| **serviceId** | **어떤 Handler** 실행? | `SV.Customer.selectSummary` |
| **transactionCode** | **로그·감사·통제** 키 | `SV-INQ-0001` |

둘 다 JSON **header**에 넣습니다. [부록 D](./D-JSON-예시.md) 참고.

---

## 형식

```text
{업무코드}-{거래유형}-{일련번호}
SV-INQ-0001
```

| 부분 | 규칙 | 예 |
| --- | --- | --- |
| 업무코드 | 대문자 2~3자 | SV, OM |
| 거래유형 | **3자** 대문자 | INQ, CRT, UPD |
| 일련번호 | **4자리** 숫자 | 0001, 0002 |

---

## 거래유형 (자주 씀)

| 코드 | 뜻 | processingType |
| --- | --- | --- |
| INQ | 조회 | INQUIRY |
| CRT | 등록 | CREATE |
| UPD | 수정 | UPDATE |
| DEL | 삭제 | DELETE |
| SND | 발송 | (EXECUTE 등) |
| ADM | OM 관리 | INQUIRY 등 |
| EXE | 실행·배치 | EXECUTE |

**Header의 `processingType`과 거래유형이 맞아야** 합니다.  
예: `SV-INQ-0001` + `processingType: INQUIRY` ✅

---

## SV 실습 세트

| serviceId | 거래코드 | processingType |
| --- | --- | --- |
| SV.Customer.selectSummary | SV-INQ-0001 | INQUIRY |
| SV.Customer.selectList | SV-INQ-0002 | INQUIRY |

일련번호는 **업무+유형**마다 0001부터. **번호 재사용 금지**.

---

## ⚠️ 초보자 실수

| 잘못된 예 | 문제 |
| --- | --- |
| `sv-inq-0001` | 소문자 |
| `SV-SELECT-0001` | 비표준 유형 |
| `SV-INQ-1` | 4자리 아님 |
| INQ인데 `processingType: CREATE` | **STF 오류** |
| Catalog만 있고 거래코드 안 맞음 | **로그·통제** 깨짐 |

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [부록 B ServiceId·거래코드](./B-ServiceId-거래코드-표.md) |
| → 다음 | [부록 D JSON](./D-JSON-예시.md) |

---

## 📘 원본

- [ztcfbook/부록/C-거래코드-명명규칙.md](../ztcfbook/부록/C-거래코드-명명규칙.md)
