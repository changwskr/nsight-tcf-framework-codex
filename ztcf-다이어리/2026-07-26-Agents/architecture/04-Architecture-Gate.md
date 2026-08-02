# 04 — Architecture Gate

기준본: [00](./00-CRUD-Prompt-Architecture.md) · ADR: [01](./01-Architecture-Decision-Records.md) · 추적: [02](./02-Requirements-Traceability.md) · Runbook: [03](./03-Operations-Runbook.md)

판정: `PASS` / `CONDITIONAL` / `FAIL`  
미확인은 확정 체크하지 않는다.

---

## A. 구현 전 Gate (문서 패키지)

아키텍처 문서 작성·확정 시점에 사용한다.

| # | 점검 | 결과 | 근거 |
| --- | --- | --- | --- |
| A1 | 정의서와 ADR 결정이 일치 | PASS | 00 §10 ↔ ADR-001~006 |
| A2 | 구성요소 책임 중복 없음 | PASS | prompts=실행, architecture=설명, C00~C18=지식 SoT |
| A3 | 모든 상태 입·출·전이·실패 정의 | PASS | 00 §6·§8, 03 |
| A4 | 보안·보호경로·미커밋 정책 Explicit | PASS | 00 §8, MASTER Hard Gate |
| A5 | 구현 계획이 To-Be와 매핑 | PASS | 패키지 스펙 + prompts 실재 |
| A6 | Framework 전체 아키텍처 미복제 | PASS | 00 §1 참조만 |
| A7 | 5문서 책임 분리·상호 링크 | PASS | 본 패키지 |

**구현 전 종합:** `PASS` (2026-07-26 자체점검)

---

## B. 구현 후 Gate (프롬프트·운영)

프롬프트 패키지와 대표 시나리오 검증 후 사용한다.

| # | 점검 | 결과 | 근거 |
| --- | --- | --- | --- |
| B1 | 실제 프롬프트와 정의서 상태·Gate 일치 | PASS | MASTER 상태 머신 = 00 §6 |
| B2 | README에 신규·재개·호환 실행법 | PASS | [../README.md](../README.md) |
| B3 | 정상·조건부·실패 시나리오 | CONDITIONAL | 결과-1 CONDITIONAL+승인 검증됨; FAIL 전이·신규 자유입력은 문서상만 |
| B4 | 추적성 매트릭스 누락 없음 | PASS | 02 R01~R20 |
| B5 | 미확인 ≠ 확정값 | PASS | REPORT CONDITIONAL, OM Gap Explicit |
| B6 | C15 호환 유지 | PASS | prompts/C15 유지, ADR-003 |
| B7 | MASTER↔STANDALONE 기준 일치 | PASS | 동일 8상태·Gate·완료 |
| B8 | 상대 링크·UTF-8 | CONDITIONAL | 작성 UTF-8; 전수 링크 자동화 미실행 |

**구현 후 종합:** `CONDITIONAL`

Open Issue:

1. FAIL Gate → DESIGN 회귀의 **실실행 증적** 보강
2. 신규 자유 입력 end-to-end 시나리오 증적
3. 링크 전수 검증 자동화 (R18 Partial)
4. 중복 규칙 SoT 링크 정리 (R20 Open)

---

## C. 사용 방법

1. 문서만 바꿀 때 → **A** 재실행
2. 프롬프트/README를 바꿀 때 → **B** 재실행
3. FAIL 항목이 있으면 해당 문서·프롬프트를 고친 뒤 같은 Gate 재실행
4. CONDITIONAL은 Open Issue를 README 또는 02에 남기고 운영 가능 여부를 사용자가 판단

---

## D. 판정 기록 템플릿

```markdown
## Architecture Gate 기록
- 일자:
- 점검자:
- A 종합: PASS / CONDITIONAL / FAIL
- B 종합: PASS / CONDITIONAL / FAIL
- Open Issue:
- 다음 조치:
```
