# 01 — Architecture Decision Records

기준본: [00-CRUD-Prompt-Architecture.md](./00-CRUD-Prompt-Architecture.md)

결정만 기록한다. 상태 계약·Runbook 절차는 기준본·03을 참조한다.

---

## ADR-001 — 단일 진입점 MASTER

| 항목 | 내용 |
| --- | --- |
| 상태 | Accepted |
| 일자 | 2026-07-26 |

### 맥락

신규 요구와 기존 결과폴더의 진입점이 분리되어 운영자가 어떤 프롬프트를 붙일지 혼동했다.

### 대안

1. C-MASTER + C15를 계속 분리 운영
2. **MASTER 하나로 INPUT~REPORT 통합** (채택)
3. IDE 전용 Agent UI에만 의존

### 결정

`MASTER-CRUD-DEVELOPER.md`를 저장소 연동형 기준 진입점으로 한다. STANDALONE은 문서 접근이 어려운 환경용이다.

### 영향

- README·AGENTS가 MASTER를 권장
- C15는 호환 진입점으로 유지 (ADR-003)

---

## ADR-002 — 충돌 시 자동 선택 금지

| 항목 | 내용 |
| --- | --- |
| 상태 | Accepted |

### 맥락

원장 BC와 C06 ServiceId prefix, Handler명이 어긋날 수 있다 (`결과-1` 초기 LN/AV 혼재).

### 대안

1. 원장 우선 자동 적용
2. 코드 우선 자동 적용
3. **충돌표 제시 후 사용자 선택** (채택)

### 결정

정보 우선순위는 유지하되, SoT끼리 충돌하면 `DESIGN_CONFLICT`로 중단하고 선택 영향을 보여 준다.

### 영향

- IMPLEMENT 전 정합 필수
- Gate 보고에 충돌·가정 Explicit

---

## ADR-003 — C15 호환 유지, 제거는 후속

| 항목 | 내용 |
| --- | --- |
| 상태 | Accepted |

### 맥락

기존 습관·스크립트가 C15 목록 승인 흐름에 의존할 수 있다.

### 결정

C15 실행 프롬프트를 삭제하지 않는다. 제거 여부는 안정화 후 별도 결정.

### 영향

- MASTER: Gate 승인 1회 → 자동 구현
- C15: 파일 목록 승인 후 구현
- 두 계약의 차이를 README에 명시

---

## ADR-004 — Gate 승인 후 파일 목록 승인 생략

| 항목 | 내용 |
| --- | --- |
| 상태 | Accepted |

### 맥락

패키지 스펙은 Gate 승인 후 목록부터 검증까지 자동 진행을 요구한다.

### 결정

MASTER에서 `Gate 승인`만으로 IMPLEMENT→VERIFY→TRACE→REPORT를 진행한다. 보호 경로·미커밋 충돌은 자동 중단 조건으로 남긴다.

### 영향

- C15 역할 문서의 “목록 승인”과 MASTER 계약이 다름 → 진입점별로 따름
- 사용자는 `보호: path`로 덮어쓰기 금지 가능

---

## ADR-005 — STANDALONE은 AGENTS.md 우선

| 항목 | 내용 |
| --- | --- |
| 상태 | Accepted |

### 결정

STANDALONE에 핵심 규칙을 내장하되, 저장소에서 `AGENTS.md`를 발견하면 내장 규칙보다 AGENTS를 우선한다.

### 영향

MASTER와 STANDALONE의 상태·Gate·완료 기준을 동일하게 유지 ([00 §9](./00-CRUD-Prompt-Architecture.md)).

---

## ADR-006 — 완료 판정은 검증 증적 기반

| 항목 | 내용 |
| --- | --- |
| 상태 | Accepted |

### 결정

COMPLETE / CONDITIONAL / BLOCKED만 사용한다. 파일 생성·컴파일만으로 COMPLETE 금지. 미실행 테스트를 성공으로 쓰지 않는다.

### 근거

`결과-1` MASTER REPORT는 단위 테스트 PASS·OM/실거래 미검증으로 `CONDITIONAL` 판정 (확인된 실행 증적).
