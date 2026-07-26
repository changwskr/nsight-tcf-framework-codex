# 부록 H. 개발 끝나기 전 체크

| **부록** | H |
| **상태** | 집필 완료 |
| **원본** | [ztcfbook 부록 H](../ztcfbook/부록/H-개발-완료-체크리스트.md) |

---

## 그림으로 보기

```mermaid
flowchart LR
  ID[ServiceId·거래코드] --> CODE[6계층+SQL]
  CODE --> TEST[curl 통과]
  TEST --> OM[OM 등록]
```

---

## MR 올리기 전 — 15초 점검

| □ | 질문 |
| --- | --- |
| □ | serviceId 형식 `SV.대상.행동` 맞나? |
| □ | 거래코드 `SV-INQ-0001` 형식 맞나? |
| □ | Handler가 **Facade만** 부르나? |
| □ | SQL은 **Mapper XML**에 있나? |
| □ | OM Catalog에 serviceId **등록**했나? |
| □ | curl/tcf-ui로 **SUCCESS** 봤나? |
| □ | customerNo 등 **필수값 오류** 테스트 했나? |
| □ | 로그에 **주민번호 전체** 안 남기나? |
| □ | `gradlew build` **통과**하나? |

---

## 로컬만

| □ | local profile 사용 |
| □ | `POST /{bc}/online` 으로 테스트 |
| □ | 운영 DB·prd 설정 **미사용** |

---

## 더 많은 항목

- MR 리뷰 → [부록 I](./I-코드-리뷰-체크.md)
- 운영 배포 전 → [부록 J](./J-운영-전환-전-체크.md)
- 전체 상세 → [ztcfbook 부록 H·I·J](../ztcfbook/부록/README.md)

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [부록 G yml](./G-application-yml-기본.md) |
| → 다음 | [부록 I MR 리뷰](./I-코드-리뷰-체크.md) |

---

## 📘 원본

- [ztcfbook/부록/H-개발-완료-체크리스트.md](../ztcfbook/부록/H-개발-완료-체크리스트.md)
- [ztcfbook/부록/I-코드-리뷰-체크리스트.md](../ztcfbook/부록/I-코드-리뷰-체크리스트.md)
