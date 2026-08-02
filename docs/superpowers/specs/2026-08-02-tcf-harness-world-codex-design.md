# TCF Harness World Codex 전용 전환 설계

## 1. 목적

`harness-service-world`의 에이전트 팀 설계 패턴과 오케스트레이션 예제를 기반으로, Claude 전용 실행 계약을 제거하고 Codex에서 직접 사용할 수 있는 `tcf-harness-world`를 만든다.

완료된 결과는 다음 조건을 만족해야 한다.

- 하네스 실행과 설치에 Claude CLI, `.claude/`, `CLAUDE.md`, `.claude-plugin`이 필요하지 않다.
- Codex의 `AGENTS.md`, `SKILL.md`, 멀티에이전트 협업 도구를 기준으로 사용법을 설명한다.
- Analyst → Builder → QA 흐름을 Codex에서 재현할 수 있다.
- Windows PowerShell과 POSIX shell에서 구조 및 잔존 의존성을 검증할 수 있다.
- 기존 `harness-service-world`와 다른 사용자 미커밋 변경을 수정하지 않는다.

## 2. 범위

### 포함

- `tcf-harness-world`의 문서, 역할 정의, 스킬, 참조 문서, 샘플 오케스트레이터 정비
- Codex 프로젝트 전용 설치를 기본으로 하는 `AGENTS.md` 및 사용 안내
- 재사용 가능한 Codex 스킬 형태의 디렉터리 구조
- Codex 멀티에이전트 도구에 대응하는 실행 계약
- Claude 전용 잔존 참조와 필수 파일을 검사하는 검증 스크립트
- 샘플 시뮬레이션의 결정적이고 재실행 가능한 동작

### 제외

- `harness-service-world` 또는 `harness-service` 원본 수정
- Java/Gradle 업무 모듈 변경
- Codex 자체 설치 또는 사용자 홈 디렉터리 자동 변경
- 실제 외부 LLM API 호출
- Claude와 Codex를 동시에 지원하는 호환 계층

## 3. 전환 원칙

기계적인 이름 치환이 아니라 실행 계약을 Codex 모델에 맞게 변환한다.

| Claude 중심 요소 | Codex 전용 대응 |
|---|---|
| `CLAUDE.md` | `AGENTS.md` |
| `.claude/skills/{name}/SKILL.md` | 저장소의 `skills/{name}/SKILL.md`와 Codex 설치 안내 |
| `.claude/agents/{name}.md` | `agents/{name}.md` 역할 계약 |
| Claude Agent/Team 호출 | `spawn_agent`, `send_message`, `followup_task`, `wait_agent` |
| Claude 플러그인 매니페스트 | 제거 |
| Claude CLI 설치·실행 명령 | Codex 프로젝트/스킬 사용 절차 |

제품 전환 이력을 설명하는 `docs/migration-from-claude.md`와 잔존 참조 검사 규칙을 제외하고, 실행 문서와 스킬 계약에는 Claude 의존 표현을 두지 않는다.

## 4. 목표 구조

```text
tcf-harness-world/
├── AGENTS.md
├── README.md
├── docs/
│   ├── quickstart.md
│   └── migration-from-claude.md
├── agents/
│   ├── analyst.md
│   ├── builder.md
│   └── qa.md
├── skills/
│   └── harness/
│       ├── SKILL.md
│       ├── references/
│       │   ├── agent-design-patterns.md
│       │   ├── orchestrator-template.md
│       │   └── codex-tool-mapping.md
│       └── orchestrator-sample/
│           ├── SKILL.md
│           ├── scripts/
│           └── _workspace/
├── scripts/
│   ├── verify-codex-harness.ps1
│   └── verify-codex-harness.sh
└── tests/
    └── 검증 시나리오 또는 픽스처
```

생성 로그와 임시 산출물은 소스 계약과 분리한다. 저장소에 예시 산출물을 유지할 경우 고정된 샘플임을 표시하고, 실행 시에는 안전하게 갱신하거나 별도 실행 디렉터리를 사용한다.

## 5. 구성요소

### 5.1 `AGENTS.md`

`tcf-harness-world` 범위에 적용되는 Codex 작업 규칙을 정의한다. 역할 파일의 위치, 산출물 소유권, 협업 프로토콜, 검증 명령, 사용자 변경 보존 원칙을 포함한다. 루트 `AGENTS.md`와 충돌하지 않으며 하위 범위의 구체화만 담당한다.

### 5.2 Harness 스킬

`skills/harness/SKILL.md`는 요청을 분석해 필요한 역할과 스킬을 설계하는 메타 스킬이다. 다음 단계를 명시한다.

1. 기존 `AGENTS.md`, 역할, 스킬 탐색
2. 중복 및 책임 경계 확인
3. 역할별 입력·출력·오류 계약 작성
4. 독립 작업만 병렬 에이전트로 분리
5. 산출물 통합과 QA
6. 변경 이력과 검증 결과 기록

### 5.3 역할 계약

- Analyst: 요구사항, 제약, 위험, 완료 기준을 구조화한다.
- Builder: 승인된 분석과 설계만 구현하고 산출물 경로를 보고한다.
- QA: 요구사항 추적성, 구조, 잔존 의존성, 스크립트 결과를 검증한다.

각 역할 파일은 핵심 역할, 입력, 출력, 금지 사항, 오류 처리, 협업 대상을 독립적으로 설명한다. 역할 파일은 설명 자료이며 실제 에이전트 생성은 Codex 협업 도구가 담당한다.

### 5.4 오케스트레이터 샘플

샘플은 Analyst → Builder → QA의 순차 의존성을 보여준다. 구현 예시는 Codex 도구 매핑과 오프라인 시뮬레이션 두 층으로 나눈다.

- Codex 실행 계약: 어떤 역할 파일을 읽고 어떤 작업을 `spawn_agent`에 전달하는지 문서화한다.
- 오프라인 시뮬레이션: 외부 모델 호출 없이 PowerShell/bash로 산출물 생성과 QA 판정을 재현한다.

## 6. 데이터 및 제어 흐름

```text
사용자 요청
  → Harness 스킬이 범위와 기존 구조 분석
  → Analyst가 analysis-summary.md 작성
  → Builder가 승인된 분석을 입력으로 구현 계획/산출물 작성
  → QA가 요구사항과 파일을 대조
  → 실패 시 해당 역할에 제한된 수정 요청
  → 통과 시 최종 결과와 검증 증거 보고
```

역할 간 전달은 암묵적 대화 상태보다 명시적 파일과 작업 메시지를 우선한다. 각 산출물에는 입력 출처, 상태, 미해결 항목을 기록해 재시작 가능성을 확보한다.

## 7. 오류 처리

- 필수 입력이 없으면 후속 역할을 실행하지 않고 누락 항목을 보고한다.
- 역할 산출물이 정해진 경로에 없거나 비어 있으면 QA 실패로 처리한다.
- 재시도는 유한 횟수로 제한하며 같은 원인이 반복되면 실패 상태와 근거를 남긴다.
- 임시 파일 작성 후 같은 파일시스템 내 이동으로 산출물을 완성해 부분 파일 노출을 줄인다.
- 스크립트는 실패 시 0이 아닌 종료 코드를 반환한다.
- 기존 산출물을 덮어쓸 때의 정책을 명시하며 사용자 작성 파일은 기본적으로 보존한다.

## 8. 검증 설계

### 정적 검증

- 필수 파일과 frontmatter 존재 여부
- 로컬 상대 링크 유효성
- 실행 범위에서 금지된 Claude 전용 경로·명령·매니페스트 부재
- 역할 파일의 필수 섹션 존재 여부
- PowerShell/bash 스크립트 구문과 실행 권한

### 동적 검증

- 깨끗한 임시 작업공간에서 시뮬레이션 실행
- Analyst 및 Builder 산출물 생성 확인
- QA PASS 기록과 정상 종료 코드 확인
- 필수 산출물 누락 시 QA FAIL 및 비정상 종료 확인
- 연속 실행 시 결과가 손상되거나 중복 누적되지 않는지 확인

### 저장소 검증

- `rg`로 금지 참조 재검사
- `git diff --check`
- 대상 디렉터리 검증 스크립트 실행
- 변경 파일이 `tcf-harness-world`와 승인된 설계/계획 문서에 한정되는지 확인

## 9. 호환성과 롤백

`tcf-harness-world`는 신규 미추적 디렉터리이므로 기존 공개 계약을 변경하지 않는다. 롤백은 이 디렉터리와 관련 설계 문서의 변경만 제거하는 방식으로 가능하다. 구현 과정에서는 원본을 자동 동기화하지 않으며, 향후 원본 변경을 가져올 때 명시적인 재검토와 변환을 거친다.

## 10. 완료 기준

- `tcf-harness-world`만으로 Codex 하네스의 목적, 설치, 실행, 역할 및 검증을 이해할 수 있다.
- Codex 전용 협업 도구 매핑이 문서화되어 있다.
- Claude 전용 실행 의존성이 검증 스크립트에서 발견되지 않는다.
- PowerShell 시뮬레이션과 가능한 경우 bash 시뮬레이션이 통과한다.
- 실패 시나리오가 비정상 종료와 명확한 메시지로 확인된다.
- 기존 사용자 변경은 유지된다.
