# 00 — 통합 CRUD 개발 프롬프트 아키텍처 정의서

| 항목 | 내용 |
| --- | --- |
| 문서 ID | CRUD-PROMPT-ARCH-00 |
| 상태 | As-Built (프롬프트 패키지 구현 후 문서화) |
| 기준일 | 2026-07-26 |
| 설계 스펙 | [2026-07-26-crud-prompt-architecture-docs-design.md](../../../docs/superpowers/specs/2026-07-26-crud-prompt-architecture-docs-design.md) |
| 패키지 스펙 | [2026-07-26-nsight-crud-prompt-package-design.md](../../../docs/superpowers/specs/2026-07-26-nsight-crud-prompt-package-design.md) |

부속: [01 ADR](./01-Architecture-Decision-Records.md) · [02 추적성](./02-Requirements-Traceability.md) · [03 Runbook](./03-Operations-Runbook.md) · [04 Gate](./04-Architecture-Gate.md)

---

## 1. 목적·독자·용어

### 1.1 목적

통합 CRUD 개발 프롬프트 패키지를 개발자와 AI 에이전트 운영자가 **실행·검증·중단·재개**할 수 있도록 As-Is / To-Be / 전환 / 검증 기준을 정의한다.

NSIGHT TCF Framework 전체 아키텍처는 복제하지 않는다. 참조: `zdocs-1/architecture/`, `xdoc/architecture/system-architecture.md`.

### 1.2 독자

- 주: CRUD 프롬프트 사용자(개발자), Codex/Cursor 운영자
- 보조: AA, 품질, 문서 검토자

### 1.3 용어

| 용어 | 의미 |
| --- | --- |
| 결과폴더 | `결과*/` — C00~C18 MD와 `_확정정보원장.md` |
| 원장 | `_확정정보원장.md` — 확정값 SoT |
| Gate 승인 | C14 PASS/CONDITIONAL 후 구현 진행 허가 |
| MASTER | `prompts/MASTER-CRUD-DEVELOPER.md` 단일 진입점 |
| C15 호환 | `prompts/C15-실행프롬프트.md` — 생성만, 목록 승인 |

---

## 2. As-Is

### 2.1 흐름

```text
C00~C14 설계 프롬프트
  → 결과 MD · 원장
  → 별도 C15 실행 프롬프트 (파일 목록 승인)
  → 코드 생성
  → C16~C18 검증·추적·완료
```

근거 경로: `ztcf-다이어리/2026-07-26-인공지능방법론-CRUD개발프롬프트/`, `.../Agents/prompts/C15-실행프롬프트.md`

### 2.2 유지할 자산

- 단계별 질문·완료 조건 (C00~C18)
- `_확정정보원장.md`
- C14 설계 Gate, C15 생성 규칙, C16~C18 기준
- `AGENTS.md`, `xdoc/agents/*` 역할 지침

### 2.3 개선 대상

1. 신규 요구와 기존 결과폴더의 **진입점 분리**
2. 현재 단계·승인 상태를 복원할 **단일 실행 계약 부족**
3. Codex↔Cursor **중단·재개·인계 규칙 분산**
4. 완료 판정·오류 복구가 **여러 문서에 분산**

---

## 3. To-Be — Context (C4-lite)

```text
[사용자]
   │ 붙여넣기 / 명령 (Gate 승인, 현황, …)
   ▼
[Codex 또는 Cursor]  ← 동일 실행 계약
   │ 읽기/쓰기
   ▼
[저장소]
   ├─ prompts (MASTER / STANDALONE / TEMPLATE / C15)
   ├─ AGENTS.md · xdoc/agents/*
   ├─ C00~C18 · 결과폴더 · 원장
   ├─ 업무 소스 · 설정 · 테스트
   └─ Gradle (검증)
```

---

## 4. To-Be — 구성요소와 책임

| 구성요소 | 책임 | 경로 |
| --- | --- | --- |
| MASTER | 모드 판별·상태 전이·Gate 후 자동 구현 | `prompts/MASTER-CRUD-DEVELOPER.md` |
| STANDALONE | 문서 접근 어려울 때 핵심 규칙 내장 | `prompts/STANDALONE-CRUD-DEVELOPER.md` |
| REQUEST TEMPLATE | 선택 입력 양식 | `prompts/CRUD-REQUEST-TEMPLATE.md` |
| C15 호환 | 생성만 · 목록 승인 | `prompts/C15-실행프롬프트.md` |
| C00~C18 | 상세 설계·구현·검증 지식 SoT | CRUD개발프롬프트/ |
| 원장 | 확정값·가정·Open Issue | `결과*/_확정정보원장.md` |
| AGENTS·역할 | 계층·보안·검증 규칙 | `AGENTS.md`, `xdoc/agents/` |
| 소스·테스트 | 구현 사실 근거 | `*-service` 등 |
| Codex/Cursor | 동일 계약 수행 환경 | — |

### 4.1 정보 우선순위

```text
1. 사용자 현재 지시
2. AGENTS.md
3. 승인된 원장·C00~C14
4. 역할 문서
5. 실제 코드·설정·테스트
6. 프롬프트 기본값
```

원장↔구현 충돌 시 자동 선택 금지 → `DESIGN_CONFLICT` ([01 ADR-002](./01-Architecture-Decision-Records.md)).

---

## 5. 파일 구조와 책임 경계

```text
ztcf-다이어리/2026-07-26-Agents/
├─ README.md                 ← 실행법 인덱스
├─ CRUD-Codegen-Agent.md     ← C15 역할 요약
├─ prompts/                  ← 실행 진입점
└─ architecture/             ← 본 패키지 (설명·운영·판정)
```

- **prompts/** = 실행 계약 (채팅에 붙임)
- **architecture/** = 이해·복구·Gate (설명서)
- **C00~C18** = 단계 지식 SoT (삭제하지 않음)
- Framework 전체 아키텍처 문서와 중복 작성 금지

---

## 6. 상태 모델과 단계 계약

```text
INPUT → DISCOVERY → DESIGN → GATE
  → (Gate 승인) → IMPLEMENT → VERIFY → TRACE → REPORT
```

| 상태 | 입력 | 핵심 처리 | 출력 |
| --- | --- | --- | --- |
| INPUT | 자연어 / 템플릿 / 결과폴더 | 모드 판별 | 작업 범위 |
| DISCOVERY | 작업 범위 | git·유사구현·설정 조사 | 근거 목록 |
| DESIGN | 근거·요구 | 누락 결정만 질의 | C00~C13·원장 |
| GATE | 설계 결과 | 정합·위험 판정 | PASS / CONDITIONAL / FAIL |
| IMPLEMENT | Gate 승인 | C15 구현 | 코드·설정·문서 |
| VERIFY | 변경 | C16 테스트·빌드 | 검증 증적 |
| TRACE | 요구·설계·변경·시험 | C17 연결 | 추적 매트릭스 |
| REPORT | 모든 증적 | C18 판정 | COMPLETE / CONDITIONAL / BLOCKED |

### 6.1 전이 규칙

1. 결과폴더가 충분하면 DESIGN 생략 가능
2. GATE 전 업무 소스 수정 금지 (설계 MD만 가능)
3. PASS/CONDITIONAL → 사용자 `Gate 승인` 후에만 IMPLEMENT
4. FAIL → DESIGN으로 회귀
5. 구현 결함 검증 실패 → IMPLEMENT 회귀 후 재검증
6. 환경/필수결정 문제 → 체크포인트 기록 후 중단
7. COMPLETE는 요청 동작 + 관련 검증 확인 시에만

상태 보고 필수 필드: 현재 상태, 확정값, 가정, 충돌, Open Issue, 다음 전이 조건.

상세 오류·재개: [03 Runbook](./03-Operations-Runbook.md).

---

## 7. 실행 흐름

### 7.1 신규 요구사항

```text
자유 요구 / 템플릿
→ INPUT → DISCOVERY → DESIGN(누락만)
→ GATE → Gate 승인
→ IMPLEMENT → VERIFY → TRACE → REPORT
```

### 7.2 기존 결과폴더

```text
결과폴더 경로
→ INPUT → DISCOVERY (원장·C14·소스 정합)
→ DESIGN 생략 또는 최소 보완
→ GATE → Gate 승인
→ IMPLEMENT(갭) → VERIFY → TRACE → REPORT
```

**검증된 실행 예:** `결과-1` + MASTER → CONDITIONAL Gate → 승인(OM 미포함) → C17/C18·`:av-service:test` → REPORT `CONDITIONAL`.

---

## 8. 오류 모델과 안전 통제

| 오류 | 의미 | 처리 |
| --- | --- | --- |
| `DESIGN_CONFLICT` | BC/ServiceId/테이블/근거 충돌 | 충돌표·선택 영향 제시 |
| `WORKTREE_CONFLICT` | 미커밋·보호경로 충돌 | 수정 중단·사용자 판단 |
| `IMPLEMENTATION_FAILURE` | 컴파일·테스트·계층 위반 | 수정 후 동일 검증 재실행 |
| `ENVIRONMENT_FAILURE` | DB·네트워크·권한·환경 | 명령·오류·미검증·재개법 기록 |

자동 진행 금지:

- Schema/공개계약 파괴적 변경
- 인증·권한 면제 확대
- 운영 Secret 생성·기록
- PII를 로그·오류·샘플에 평문 노출
- 미커밋 변경 덮어쓰기·되돌리기

결정 출처는 반드시: 사용자 답변 | 저장소 근거 | 명시적 가정.

---

## 9. 검증과 완료 판정

1. 문서·링크·경로
2. MASTER ↔ STANDALONE 상태·Gate·완료 기준 일치
3. 신규 / 결과폴더 재개 시나리오
4. PASS / CONDITIONAL / FAIL 전이
5. 미커밋·환경 실패 복구
6. 요구 → 구성요소 → 규칙 → 검증 추적 ([02](./02-Requirements-Traceability.md))
7. C00~C18 및 C15 호환

완료 판정:

| 판정 | 조건 |
| --- | --- |
| COMPLETE | 요청 동작 + 관련 검증 확인 |
| CONDITIONAL | 구현됐으나 핵심 외부 검증 잔여 |
| BLOCKED | 필수 결정·충돌·안전으로 불가 |

파일 생성만으로 COMPLETE 금지. 배포·운영 승인으로 단정 금지.

---

## 10. As-Is → To-Be 전환

```text
1. 아키텍처 문서 패키지 확정          ← 본 문서군
2. MASTER·STANDALONE·TEMPLATE 작성   ← 완료
3. C00~C18 링크·우선순위 연결        ← README·MASTER에 반영
4. README 신규·재개 실행법           ← 완료
5. 대표 시나리오 검증                ← 결과-1 MASTER 실행
6. C15를 호환 진입점으로 유지        ← 유지
7. 안정화 후 중복 규칙을 SoT 링크로 정리 (후속)
```

C15 제거 여부는 본 범위에서 결정하지 않음 ([ADR-003](./01-Architecture-Decision-Records.md)).

---

## 11. 관련 링크

| 종류 | 경로 |
| --- | --- |
| 작업 공간 README | [../README.md](../README.md) |
| MASTER | [../prompts/MASTER-CRUD-DEVELOPER.md](../prompts/MASTER-CRUD-DEVELOPER.md) |
| CRUD Codegen 역할 | [../../../xdoc/agents/crud-codegen-agent.md](../../../xdoc/agents/crud-codegen-agent.md) |
| AGENTS | [../../../AGENTS.md](../../../AGENTS.md) |
