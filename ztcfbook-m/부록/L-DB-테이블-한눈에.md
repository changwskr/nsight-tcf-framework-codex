# 부록 L. DB·테이블 한눈에

| **부록** | L |
| **상태** | 집필 완료 |
| **원본** | [ztcfbook 부록 L](../ztcfbook/부록/L-TCF-핵심-테이블-DDL-요약.md) |

---

## 그림으로 보기

```mermaid
flowchart TB
  OM23[OM 23 tables] --> CAT[OM_SERVICE_CATALOG]
  TXL[TCF_TX_LOG] --> ALL[모든 WAR]
```

---

## DB 3종류 (18장 복습)

| 이름 | 뭐 들어있나 | 로컬 파일 대략 |
| --- | --- | --- |
| **업무 H2** | 업무 SQL (mem) | `jdbc:h2:mem:nsight_sv` |
| **OMDB** | Catalog·사용자·코드 | `./data/nsight-txlog/nsight_om` |
| **거래로그** | TCF_TX_LOG | **OM과 같은 file** often |

업무 WAR는 **업무 DB + 거래로그 DS** 둘 다 씁니다.

---

## 접두어 규칙

| 접두어 | 의미 |
| --- | --- |
| `TCF_` | 프레임워크 (로그·통제) |
| `OM_` | 운영 Admin |
| `UD_` | 파일 업·다운 |
| `SPRING_SESSION` | 로그인 세션 |
| `{BC}_` | 업무 전용 (예: `EB_USER`) |

---

## 외울 테이블 3개

| 테이블 | 용도 |
| --- | --- |
| `OM_SERVICE_CATALOG` | **serviceId 등록부** |
| `TCF_TRANSACTION_CONTROL` | **거래통제** (Header 7항) |
| `TCF_TX_LOG` | **거래 이력** (GUID 추적) |

Catalog 없으면 → **실행 차단**

---

## OM 그룹 (개념만)

| 그룹 | 예 |
| --- | --- |
| 권한·메뉴 | OM_USER, OM_MENU |
| 코드·오류 | OM_COMMON_CODE, OM_ERROR_CODE |
| 모니터링 | OM_AP_STATUS (batch가 채움) |
| 배치 | OM_BATCH_JOB, OM_BATCH_HISTORY |

전체 23개 → **원본 부록 L §L.3**

---

## TCF_TX_LOG — 자주 보는 컬럼

| 컬럼 | 의미 |
| --- | --- |
| GUID | 거래 추적 ID |
| SERVICE_ID | 실행 기능 |
| RESULT_STATUS | SUCCESS / FAIL |
| BUSINESS_CODE | SV, OM … |

OM Admin → **거래로그 조회**

---

## ⚠️ 초보자 실수

| 실수 | |
| --- | --- |
| 업무 Mapper에서 OM_USER JOIN | **DB 경계** |
| `./data/nsight-txlog` 폴더 삭제 | OM·로그 **초기화** |
| 로그 테이블 직접 INSERT | **ETF가 INSERT** |

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [부록 K 포트](./K-포트-모듈-한눈에.md) |
| → 다음 | [부록 M 명명규칙](./M-명명규칙-21주제-한눈에.md) |

---

## 📘 원본

- [ztcfbook/부록/L-TCF-핵심-테이블-DDL-요약.md](../ztcfbook/부록/L-TCF-핵심-테이블-DDL-요약.md)
