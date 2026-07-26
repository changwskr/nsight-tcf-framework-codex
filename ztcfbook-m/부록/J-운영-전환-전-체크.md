# 부록 J. 운영 전환 전 체크

| **부록** | J |
| **상태** | 집필 완료 |
| **원본** | [ztcfbook 부록 J](../ztcfbook/부록/J-운영-전환-체크리스트.md) |

---

## 그림으로 보기

```mermaid
flowchart LR
  BUILD[WAR 빌드] --> OMSEED[OM prod seed]
  OMSEED --> DEPLOY[배포]
  DEPLOY --> SMOKE[Smoke]
  SMOKE -->|fail| RB[Rollback]
```

---

## “개발 끝” ≠ “운영 OK”

운영 반영 전에는 **WAR + OM + DB + Rollback**까지 준비돼야 합니다.

한 줄 기준:

> 운영자가 장애 때 **guid로 찾고**, **WAR 되돌릴 수** 있어야 Go.

---

## 전환 흐름

```text
부록 H (개발 완료)
  → 부록 I (코드 리뷰)
  → build·test·Sonar
  → stg Smoke
  → 부록 J (이 표)
  → prod 배포 → Health → Smoke
```

---

## 10영역 — 한 번에 훑기

| □ | 영역 | 통과 기준 (한 줄) |
| --- | --- | --- |
| □ | 1 빌드 | Git Tag, **gradlew build** green |
| □ | 2 TCF 거래 | Catalog **ACTIVE**, Smoke SUCCESS |
| □ | 3 DB | DDL 승인, SQL **EXPLAIN** |
| □ | 4 OM 기준 | Catalog·**통제**·Timeout·오류코드 |
| □ | 5 보안 | 세션/JWT, 권한, **마스킹** |
| □ | 6 로그 | GUID, 거래로그 SUCCESS/FAIL |
| □ | 7 성능 | p95·Pool·Timeout (stg) |
| □ | 8 배포 | 순서·백업·**Smoke 5건** |
| □ | 9 Rollback | **N-1 WAR** 준비 |
| □ | 10 인수 | runbook·담당자 연락처 |

---

## No-Go — 하나라도면 **배포 중단**

| 조건 |
| --- |
| WAR 빌드·배포 실패 |
| 대표 serviceId **호출 실패** |
| 미권한 사용자 **업무 실행 가능** |
| 로그·응답에 **주민번호 평문** |
| Health Check **실패** |
| Rollback **방법 없음** |

---

## 배포 당일 3단계

| 시점 | 할 일 |
| --- | --- |
| **직후** | Health + Smoke curl |
| **30분** | 거래로그 FAIL 비율 |
| **익일** | Dashboard·Batch·문의 |

20장 **롤백·장애 triage**와 같이 봅니다.

---

## 최종 판정

| | 의미 |
| --- | --- |
| **Go** | 배포 진행 |
| **Conditional Go** | 경미한 보완 후 |
| **Hold / No-Go** | **배포 보류** |

---

## ⚠️ 초보자 실수

| 실수 | |
| --- | --- |
| Catalog만 있고 **거래통제** 없음 | Header 7항 **차단** |
| prod yml에 비밀번호 | **Vault·env** |
| Rollback WAR 없이 배포 | **N-1 필수** |
| stg Smoke 생략 | prod **첫 테스트** 금지 |

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [부록 I MR 리뷰](./I-코드-리뷰-체크.md) |
| → 다음 | [부록 K 포트](./K-포트-모듈-한눈에.md) |

---

## 📘 원본

- [ztcfbook/부록/J-운영-전환-체크리스트.md](../ztcfbook/부록/J-운영-전환-체크리스트.md)
- [ztcfbook/제06편/20-CICD-릴리즈-DR.md](../ztcfbook/제06편/20-CICD-릴리즈-DR.md)
