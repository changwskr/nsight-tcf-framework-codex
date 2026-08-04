# Development Harness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Java 21·Spring Boot 기반 CLI 하네스로 요건→분석→설계→구현→테스트 생명주기를 수동 Gate와 파일 증적으로 통제한다.

**Architecture:** 의존성 없는 코어 도메인과 파일 저장소를 중심으로 구성하고, Spring Boot `CommandLineRunner`는 CLI 진입점과 객체 조립만 담당한다. 에이전트와 테스트 프로세스는 파일 기반 계약 및 `ProcessBuilder` 어댑터를 통해 실행한다.

**Tech Stack:** Java 21, Spring Boot 3.3.x, Gradle 8.x, JUnit 5, Git CLI, Markdown, JSON/JSONL

## Global Constraints

- 기준 브랜치 직접 수정, 자동 Push, 자동 Merge를 금지한다.
- 승인된 선행 산출물을 후속 단계에서 임의로 변경하지 않는다.
- 상태원장은 JSON, 감사기록은 JSONL로 저장한다.
- 테스트 삭제·비활성화·검증 축소를 자동 수정으로 허용하지 않는다.
- 테스트 자동 수정은 최대 3회다.
- 비밀번호·토큰·개인정보·Private Key를 프롬프트와 로그에 기록하지 않는다.

---

### Task 1: 코어 상태 모델과 JSON 저장소

**Files:**
- Create: `src/main/java/com/nh/nsight/harness/domain/*.java`
- Create: `src/main/java/com/nh/nsight/harness/json/SimpleJson.java`
- Create: `src/main/java/com/nh/nsight/harness/storage/JsonStateRepository.java`
- Test: `src/test/java/com/nh/nsight/harness/storage/JsonStateRepositoryTest.java`

**Interfaces:**
- Produces: `WorkItemState`, `Stage`, `StageStatus`, `JsonStateRepository.load/save`

- [ ] Write a failing round-trip state repository test.
- [ ] Run the focused test and confirm failure because classes are missing.
- [ ] Implement the minimum domain and JSON codec.
- [ ] Run the focused test and confirm pass.

### Task 2: 작업공간·요건·Gate 서비스

**Files:**
- Create: `src/main/java/com/nh/nsight/harness/service/WorkspaceService.java`
- Create: `src/main/java/com/nh/nsight/harness/service/RequirementService.java`
- Create: `src/main/java/com/nh/nsight/harness/service/GateService.java`
- Test: `src/test/java/com/nh/nsight/harness/service/RequirementAndGateServiceTest.java`

**Interfaces:**
- Consumes: `JsonStateRepository`
- Produces: 작업 초기화, 12문항 진행, 승인·차단 상태전이

- [ ] Write failing tests for 12 questions and blocked stage transition.
- [ ] Verify failures.
- [ ] Implement minimum services.
- [ ] Verify passes.

### Task 3: Git 안전 서비스

**Files:**
- Create: `src/main/java/com/nh/nsight/harness/git/GitService.java`
- Test: `src/test/java/com/nh/nsight/harness/git/GitServiceTest.java`

**Interfaces:**
- Produces: clean check, branch creation, commit SHA, diff capture

- [ ] Write a failing temporary-repository branch test.
- [ ] Verify failure.
- [ ] Implement Git CLI wrapper.
- [ ] Verify pass.

### Task 4: 프롬프트·에이전트 파일 계약

**Files:**
- Create: `src/main/java/com/nh/nsight/harness/agent/*.java`
- Create: `src/main/java/com/nh/nsight/harness/prompt/PromptService.java`
- Test: `src/test/java/com/nh/nsight/harness/agent/AgentContractTest.java`

**Interfaces:**
- Produces: `prompt.md`, `context.json`, `result.md`, `execution.json`, stdout/stderr logs

- [ ] Write failing contract-file test.
- [ ] Verify failure.
- [ ] Implement prompt rendering and process adapter.
- [ ] Verify pass.

### Task 5: 테스트 탐지·승인·실행·재시도

**Files:**
- Create: `src/main/java/com/nh/nsight/harness/testexec/*.java`
- Test: `src/test/java/com/nh/nsight/harness/testexec/TestExecutionServiceTest.java`

**Interfaces:**
- Produces: candidate detection, approved command execution, evidence, max-three retry state

- [ ] Write failing detector and retry tests.
- [ ] Verify failure.
- [ ] Implement minimum detector/executor.
- [ ] Verify pass.

### Task 6: CLI 라우터와 Spring Boot 진입점

**Files:**
- Create: `src/main/java/com/nh/nsight/harness/cli/*.java`
- Create: `src/main/java/com/nh/nsight/harness/HarnessApplication.java`
- Test: `src/test/java/com/nh/nsight/harness/cli/HarnessCommandRouterTest.java`

**Interfaces:**
- Produces: `init`, `requirement`, `approve`, `analyze`, `design`, `implement`, `test`, `status`, `close`

- [ ] Write failing help and init command tests.
- [ ] Verify failure.
- [ ] Implement router and Spring wiring.
- [ ] Verify pass.

### Task 7: 프롬프트·템플릿·문서·패키징

**Files:**
- Create: `harness/prompts/*.md`
- Create: `harness/templates/*.md`
- Create: `harness/schemas/*.json`
- Create: `README.md`, `AGENTS.md`, `ARCHITECTURE.md`
- Create: `build.gradle`, `settings.gradle`, Gradle wrapper metadata

**Interfaces:**
- Produces: 사용자 배포 가능한 독립 프로젝트와 ZIP

- [ ] Validate required files and internal Markdown links.
- [ ] Run offline core smoke test.
- [ ] Record verification report.
- [ ] Package ZIP excluding `.git`, build output, and secrets.
