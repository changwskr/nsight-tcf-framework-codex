# NSIGHT Development Harness 아키텍처

## 1. 도입 전 안내말

본 하네스는 코드 생성기가 아니라 SI 개발 생명주기의 의도, 산출물, 승인, 구현, 시험 증적을 연결하는 제어 시스템이다.

## 2. 전체 구조

```text
사용자
  ↓ CLI 명령·수동 승인
HarnessCommandRouter
  ├─ WorkspaceService
  ├─ RequirementService
  ├─ GateService
  ├─ LifecycleService
  ├─ TestExecutionService
  └─ GitService
        ↓
파일 기록 시스템
  ├─ docs/work-items/{id}
  ├─ .harness/state/{id}.json
  ├─ .harness/audit/{id}.jsonl
  └─ .harness/work/{id}/{stage}
        ↓
GenericCommandAdapter
  ↓
DAVIS CODER / Codex CLI / 사내 LLM
```

## 3. 책임 경계

| 구성요소 | 책임 | 금지 |
|---|---|---|
| `HarnessCommandRouter` | 명령 해석·결과 출력 | 업무 규칙 직접 구현 |
| `GateService` | 선행 승인·상태전이 | 산출물 작성 |
| `RequirementService` | 12문항·요건서 생성 | 요건 추정 |
| `PromptService` | 파일 기반 실행계약 생성 | 에이전트 결과 판단 |
| `GenericCommandAdapter` | 외부 명령 실행·로그 | 무제한 재시도 |
| `TestExecutionService` | 승인 명령·3회 반복·증적 | 테스트 완화 |
| `GitService` | 상태·브랜치·Diff | Push·Merge |

## 4. 상태전이

```text
NOT_STARTED → IN_PROGRESS → REVIEW
                              ├─ APPROVED
                              ├─ REVISION_REQUIRED → IN_PROGRESS
                              └─ REJECTED

실행 실패 → NEEDS_HUMAN_REVIEW
```

## 5. 단계별 선행조건

| 단계 | 선행조건 |
|---|---|
| REQUIREMENT | 없음 |
| ANALYSIS | REQUIREMENT 승인 |
| DESIGN | ANALYSIS 승인 |
| IMPLEMENTATION | DESIGN 승인 |
| TEST | IMPLEMENTATION 승인 |
| CLOSE | TEST 승인 |

## 6. 에이전트 계약

```text
prompt.md
context.json
result.md
execution.json
stdout.log
stderr.log
```

## 7. 장애와 복구

- 외부 에이전트 Timeout은 종료코드 124와 `timedOut=true`로 기록한다.
- 상태파일은 임시파일 작성 후 원자적 교체를 시도한다.
- 테스트는 최대 3회까지만 자동 수정한다.
- 설계 충돌, 보안 위험, 데이터 손상 가능성은 즉시 사람 검토로 전환한다.

## 8. 변경관리

프롬프트·상태 스키마·상태전이는 호환성 대상이다. 필드 삭제나 상태 의미 변경은 버전 상승과 마이그레이션 계획을 동반해야 한다.
