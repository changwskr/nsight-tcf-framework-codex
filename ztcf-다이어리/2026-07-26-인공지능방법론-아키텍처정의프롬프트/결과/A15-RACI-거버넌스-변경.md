# A15 — RACI·거버넌스·변경 (확정)

| 구분 | 내용 |
| --- | --- |
| 단계 | A15 |
| 사용자 답변 | 승인자=1 AA / RACI·변경=1 |
| 확정사항 | 아래 절 |
| 가정·미확정 | — |
| 설계 영향 | A16 Gate · A17 승인 전제 |
| 원장 반영 | `_확정정보원장.md` |
| 다음 단계 | A16 — Architecture Gate |

---

## 승인자

Application Architect (AA) — A00과 동일.

## RACI (요약)

| 활동 | A | R | C | I |
| --- | --- | --- | --- | --- |
| Baseline·ADR·정의서 | AA | AA/작성자 | 개발·운영 | PMO |
| 코드·OM 반영 | AA | 개발 | AA | 운영 |
| 표준 예외 | AA+ADR | 요청자 | 보안/운영 | — |
| Drift 시정 | AA | 개발 | — | 운영 |

## 변경

호환성 파괴 → ADR. 폐기 → Explicit.

## Gate

- [x] RACI에 AA 역할
