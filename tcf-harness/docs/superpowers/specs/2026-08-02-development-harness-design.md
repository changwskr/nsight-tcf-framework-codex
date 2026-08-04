# 하네스 기반 SI 개발 자동화 플랫폼 설계서

## 1. 도입 전 안내말

본 설계는 SI 개발 생명주기인 요건 정의, 분석, 설계, 구현, 테스트를 저장소 내부의 버전 관리 가능한 산출물과 수동 승인 Gate로 통제하는 최소형 CLI 하네스를 정의한다.

사람은 요건과 승인 기준을 결정하고, 에이전트는 저장소 탐색, 문서 작성, 구현, 테스트, 로그 보존을 수행한다. 거대한 단일 지침 대신 짧은 마스터 프롬프트와 단계별 프롬프트를 사용하며, 저장소의 Markdown·JSON·로그·Diff를 기록 시스템으로 취급한다.

## 2. 확정 설계

| 항목 | 결정 |
|---|---|
| 실행 형태 | 로컬 CLI |
| 요건 입력 | 고정 12문항 대화형 |
| 승인 Gate | 전 단계 수동 승인 |
| 구현 위치 | 현재 Git 저장소 직접 수정 |
| Git 격리 | `harness/{작업ID}-{기능명}` 전용 브랜치 |
| 구현 기술 | Java 21, Spring Boot, Gradle, `CommandLineRunner` |
| 상태 저장 | `.harness/state/{workItemId}.json` |
| 감사 이력 | `.harness/audit/{workItemId}.jsonl` |
| 에이전트 호출 | 명령어 어댑터 |
| 입출력 계약 | 파일 기반 |
| 프롬프트 | 마스터 + 단계별 독립 프롬프트 |
| 에이전트 | 단일 범용 에이전트 |
| 테스트 실패 | 최대 3회 자동 수정·재실행 |
| 테스트 명령 | 자동 탐지 후 사용자 승인 |
| 테스트 증적 | 로그·결과·Diff·환경정보 |
| 작업 문서 | `docs/work-items/{workItemId}/` 통합 디렉터리 |

## 3. 핵심 흐름

```text
init
  → requirement(12문항)
  → REQUIREMENT 승인
  → analyze
  → ANALYSIS 승인
  → design
  → DESIGN 승인
  → implement
  → IMPLEMENTATION 승인
  → test(최대 3회 수정 루프)
  → TEST 승인
  → close
```

## 4. 상태 원칙

각 단계는 `NOT_STARTED`, `IN_PROGRESS`, `REVIEW`, `APPROVED`, `REVISION_REQUIRED`, `REJECTED`, `FAILED`, `NEEDS_HUMAN_REVIEW` 중 하나로 관리한다. 승인된 선행 산출물은 읽기 전용 기준선이며 변경이 필요하면 해당 단계로 되돌아가 재승인한다.

## 5. 저장소 구조

```text
AGENTS.md
ARCHITECTURE.md
harness/prompts/
harness/templates/
harness/schemas/
docs/work-items/{workItemId}/
.harness/state/
.harness/audit/
.harness/work/{workItemId}/{stage}/
```

## 6. MVP 제외범위

- 웹 UI
- 중앙 DB
- 원격 Push·Merge 자동화
- 복수 전문 에이전트 오케스트레이션
- 클라우드 관측성 스택
- 조직 계정·권한 연동

## 7. 성공 기준

1. 신규 작업을 초기화하고 전용 브랜치를 생성한다.
2. 12문항 답변으로 `requirement.md`를 생성한다.
3. 미승인 단계는 다음 단계 진입을 차단한다.
4. 단계별 프롬프트와 실행 계약 파일을 생성한다.
5. 승인된 테스트 명령을 실행하고 증적을 보존한다.
6. 테스트 실패 시 최대 3회 수정 루프를 수행하거나 사람 검토로 전환한다.
7. 상태·감사·문서·Diff를 작업 ID로 추적한다.
