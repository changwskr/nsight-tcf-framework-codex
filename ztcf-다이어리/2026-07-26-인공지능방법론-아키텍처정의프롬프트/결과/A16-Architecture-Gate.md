# A16 — Architecture Gate (확정)

| 구분 | 내용 |
| --- | --- |
| 단계 | A16 |
| 사용자 답변 | 판정=1 조건부 통과 |
| 확정사항 | **조건부 통과** — A17 허용 |
| 가정·미확정 | 아래 Open Issue |
| 설계 영향 | A17 정의서 생성 가능. AA 승인 전 운영 반영 금지 |
| 원장 반영 | `_확정정보원장.md` |
| 다음 단계 | A17 — 최종 아키텍처 정의서 |

---

## 판정문

**조건부 통과.**  
온라인 TCF Baseline(진입·계층·런타임·금지·Gate)은 확정.  
OM·NFR수치·Git·인증시범Gap·ztomcat/문서 Drift는 Explicit.  
운영 반영은 **AA 승인 후**.

## Gate 표

| ID | 영역 | 판정 |
| --- | --- | --- |
| G1 | 요구·범위 | OK |
| G2 | 구조 | OK |
| G3 | 런타임 | OK |
| G4 | 데이터·보안 | 조건부 |
| G5 | 운영·검증 | 조건부 |
| G6 | 거버넌스 | OK |

## Open Issue

OM 실등록 · NFR 수치 · Git 방식 · WAR/JWT ADR · ztomcat/문서 Gap · 시범 auth off

## Gate

- [x] 판정문 존재
- [x] 보완 시 A17 금지 — 해당 없음(조건부 통과)
