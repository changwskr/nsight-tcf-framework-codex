# 2026-07-26 Agents — 통합 CRUD 개발 에이전트

CRUD를 **설계 → Gate → 구현 → 검증 → 추적 → 보고**까지 돌리는 실행 프롬프트 작업 공간이다.

설계 스펙: [`docs/superpowers/specs/2026-07-26-nsight-crud-prompt-package-design.md`](../../docs/superpowers/specs/2026-07-26-nsight-crud-prompt-package-design.md)  
아키텍처 문서 스펙: [`docs/superpowers/specs/2026-07-26-crud-prompt-architecture-docs-design.md`](../../docs/superpowers/specs/2026-07-26-crud-prompt-architecture-docs-design.md)

## 문서 관계 (한눈에)

```text
스펙
  …-nsight-crud-prompt-package-design.md      ← 실행 프롬프트 패키지
  …-crud-prompt-architecture-docs-design.md  ← 아키텍처 문서 패키지
        │
        ▼
architecture/                                 ← 이해·운영·Gate
  00 Architecture · 01 ADR · 02 Trace · 03 Runbook · 04 Gate
        │
        ▼
실행 프롬프트 (채팅에 붙임)
  prompts/MASTER-CRUD-DEVELOPER.md     ← 권장 진입점
  prompts/STANDALONE-CRUD-DEVELOPER.md
  prompts/CRUD-REQUEST-TEMPLATE.md
  prompts/C15-실행프롬프트.md          ← 호환
        │
        ▼
역할/규칙 · 결과폴더
```

| 문서 | 역할 |
| --- | --- |
| [architecture/00-CRUD-Prompt-Architecture.md](./architecture/00-CRUD-Prompt-Architecture.md) | 아키텍처 기준본 (As-Is/To-Be/상태) |
| [architecture/01-Architecture-Decision-Records.md](./architecture/01-Architecture-Decision-Records.md) | ADR |
| [architecture/02-Requirements-Traceability.md](./architecture/02-Requirements-Traceability.md) | 요구 추적 |
| [architecture/03-Operations-Runbook.md](./architecture/03-Operations-Runbook.md) | 시작·중단·재개·복구 |
| [architecture/04-Architecture-Gate.md](./architecture/04-Architecture-Gate.md) | 구현 전/후 Gate |
| [MASTER-CRUD-DEVELOPER.md](./prompts/MASTER-CRUD-DEVELOPER.md) | 저장소 연동형 **통합 실행** |
| [STANDALONE-CRUD-DEVELOPER.md](./prompts/STANDALONE-CRUD-DEVELOPER.md) | 핵심 규칙 내장형 실행 |
| [CRUD-REQUEST-TEMPLATE.md](./prompts/CRUD-REQUEST-TEMPLATE.md) | 구조화 입력 템플릿 |
| [C15-실행프롬프트.md](./prompts/C15-실행프롬프트.md) | C15만 · 목록 승인 (호환) |
| [CRUD-Codegen-Agent.md](./CRUD-Codegen-Agent.md) | C15 역할 요약 |
| [결과-1-파일목록초안.md](./결과-1-파일목록초안.md) | 갭 모드 샘플 기록 |

## MASTER vs C15

| | MASTER | C15-실행프롬프트 |
| --- | --- | --- |
| 범위 | 설계~보고 전체 | 생성(C15)만 |
| 사람 승인 | `Gate 승인` 1회 | 파일 목록 `승인` |
| 입력 | 자연어 / 템플릿 / 결과폴더 | 결과폴더 중심 |
| 권장 | **신규 작업은 MASTER** | 기존 습관·호환 |

## 사용법

### A. 신규 요구사항

1. [MASTER-CRUD-DEVELOPER.md](./prompts/MASTER-CRUD-DEVELOPER.md) 붙여넣기 블록을 채팅에 붙인다.
2. `CRUD 개발 시작: …` 또는 [CRUD-REQUEST-TEMPLATE.md](./prompts/CRUD-REQUEST-TEMPLATE.md)를 채운다.
3. 에이전트가 DISCOVERY→DESIGN→GATE까지 진행한다.
4. `Gate 승인`을 입력하면 구현·검증·보고까지 자동 진행한다.

### B. 기존 결과 폴더

1. MASTER 블록을 붙인다.
2. `기존 결과로 개발: <결과폴더경로>`
3. 원장·C14·소스 정합만 검사한 뒤 Gate를 제시한다.
4. `Gate 승인` 후 구현(또는 갭 보완)한다.

### C. C15만 (구형)

1. [C15-실행프롬프트.md](./prompts/C15-실행프롬프트.md)를 붙인다.
2. 파일 목록을 보고 `승인`한다.

## 명령

| 명령 | 의미 |
| --- | --- |
| `CRUD 개발 시작: …` | 자유 요구로 INPUT 시작 |
| `기존 결과로 개발: …` | 결과폴더로 시작 |
| `현황` | 현재 상태·원장 요약 |
| `수정: 항목=값` | 확정값 변경 |
| `다음` | 다음 단계 |
| `Gate 승인` | C14 후 구현 자동 진행 |
| `보호: 경로` | 해당 파일 덮어쓰기 금지 |
| `검증` | VERIFY 강제 |
| `중단` | 요약 후 종료 |

## 상태 머신

```text
INPUT → DISCOVERY → DESIGN → GATE → (Gate 승인)
  → IMPLEMENT → VERIFY → TRACE → REPORT
```

기존 결과가 충분하면 DESIGN 생략.

## 정식 SoT

- [xdoc/agents/crud-codegen-agent.md](../../xdoc/agents/crud-codegen-agent.md)
- [Business Agent](../../xdoc/agents/business-agent.md)
- [공통 개발 지침](../../xdoc/agents/development-agent-guide.md)
- [AGENTS.md](../../AGENTS.md)
