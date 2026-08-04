# NSIGHT Development Harness

요건 정의 → 분석 → 설계 → 구현 → 테스트를 Git 저장소 내부의 Markdown·JSON·로그·Diff와 수동 승인 Gate로 통제하는 Java 21·Spring Boot CLI 하네스 초기 구현이다.

## 1. 핵심 특징

- 고정 12문항 대화형 요건 수집
- 단계별 Markdown 산출물
- 모든 단계 수동 승인
- 전용 `harness/{작업ID}-{기능명}` 브랜치
- DAVIS CODER·Codex CLI·사내 LLM 명령어 어댑터
- 파일 기반 에이전트 입출력 계약
- 테스트 명령 자동 탐지 후 사용자 승인
- 테스트 실패 최대 3회 자동 수정 루프
- 로그·환경·Git Diff·변경 파일 증적
- JSON 상태원장과 JSONL 감사이력

## 2. 빠른 시작

### 2.1 빌드

```bash
# Gradle이 설치된 경우
./gradlew clean test bootJar

# Gradle이 없는 경우 먼저 설치
sh scripts/install-gradle.sh
sh scripts/generate-official-wrapper.sh   # 최초 1회, 공식 Wrapper 생성
./gradlew clean test bootJar
```

생성 파일:

```text
build/libs/nsight-development-harness.jar
```

초기 패키지의 `gradlew`는 검증되지 않은 Wrapper JAR를 임의로 포함하지 않는 텍스트형 부트스트랩이다. Gradle 설치 후 `scripts/generate-official-wrapper.sh`를 실행하여 공식 Wrapper 파일을 생성하고 체크섬을 검증한다.

인터넷이 없는 환경에서는 사내 Maven Proxy와 Gradle 배포본 경로를 사용하도록 `build.gradle`과 설치 스크립트를 조정한다.

### 2.2 대상 Git 저장소에서 작업 초기화

```bash
java -jar /path/nsight-development-harness.jar init \
  --repo /path/target-repository \
  --id REQ-20260802-001 \
  --title "고객정보 조회"
```

초기화 전에 대상 저장소가 깨끗한지 확인하고 전용 작업 브랜치를 생성한다. 자동 Push와 Merge는 수행하지 않는다.

### 2.3 요건 수집

```bash
java -jar harness.jar requirement next --repo /path/repo --id REQ-20260802-001

java -jar harness.jar requirement answer \
  --repo /path/repo \
  --id REQ-20260802-001 \
  --question REQ-Q01 \
  --text "고객 종합정보 조회"
```

12문항 완료 후:

```bash
java -jar harness.jar requirement next --repo /path/repo --id REQ-20260802-001
java -jar harness.jar approve --repo /path/repo --id REQ-20260802-001 \
  --stage REQUIREMENT --decision APPROVED --comment "업무팀 승인"
```

### 2.4 분석·설계·구현

```bash
java -jar harness.jar analyze --repo /path/repo --id REQ-20260802-001
java -jar harness.jar approve --repo /path/repo --id REQ-20260802-001 \
  --stage ANALYSIS --decision APPROVED

java -jar harness.jar design --repo /path/repo --id REQ-20260802-001
java -jar harness.jar approve --repo /path/repo --id REQ-20260802-001 \
  --stage DESIGN --decision APPROVED

java -jar harness.jar implement --repo /path/repo --id REQ-20260802-001
```

에이전트가 비활성화되어 있으면 실행계약 파일만 생성한다.

```text
.harness/work/{workItemId}/{stage}/
├─ prompt.md
├─ context.json
├─ result.md
├─ execution.json
├─ stdout.log
└─ stderr.log
```

### 2.5 에이전트 설정

`.harness/config/harness-config.json`:

```json
{
  "agent": {
    "enabled": true,
    "command": [
      "davis-coder",
      "run",
      "--prompt-file",
      "${promptFile}",
      "--output-file",
      "${resultFile}"
    ],
    "timeoutSeconds": 1800,
    "environment": {}
  },
  "test": {
    "maxAttempts": 3
  }
}
```

명령은 토큰을 인수로 직접 포함하지 않는다. 필요한 인증정보는 안전한 프로세스 환경 또는 사내 Credential Store에서 제공한다.

### 2.6 테스트

```bash
java -jar harness.jar test detect --repo /path/repo --id REQ-20260802-001

java -jar harness.jar test approve-command \
  --repo /path/repo \
  --id REQ-20260802-001 \
  --command-id UNIT_TEST \
  --command "./gradlew test" \
  --timeout 900

java -jar harness.jar test run --repo /path/repo --id REQ-20260802-001
```

## 3. 작업 문서

```text
docs/work-items/{workItemId}/
├─ README.md
├─ requirement.md
├─ analysis.md
├─ design.md
├─ execution-plan.md
├─ implementation-result.md
├─ test-evidence/
└─ closure.md
```

## 4. 오프라인 코어 검증

Spring Boot 의존성을 내려받을 수 없는 환경에서도 핵심 상태·Gate·CLI 코드를 JDK 21만으로 점검할 수 있다.

```bash
sh scripts/offline-smoke-test.sh
sh scripts/compile-syntax-self-test.sh
sh scripts/e2e-self-test.sh
sh scripts/validate-package.sh
```

- `offline-smoke-test.sh`: 상태·Gate·프롬프트·3회 재시도 코어 검증
- `compile-syntax-self-test.sh`: Spring Boot·JUnit API 최소 Stub을 이용한 전체 소스 구문 연결 검증
- `e2e-self-test.sh`: 임시 Git 저장소에서 요건부터 종료까지의 생명주기 검증
- `validate-package.sh`: 파일·Prompt Resource·JSON·링크·Placeholder 검증

이 검증은 실제 Spring Boot 패키징 검증을 대체하지 않는다. 운영 사용 전 반드시 사내 Maven 저장소를 통해 `./gradlew clean test bootJar`를 실행해야 한다.

## 5. MVP 제한사항

- 웹 UI와 중앙 DB 없음
- Pull Request·Push·Merge 자동화 없음
- 단일 범용 에이전트만 지원
- JSON Schema 자동 린터는 후속 과제
- UI·메트릭·분산 추적 기반 검증은 후속 과제

## 6. 주요 문서

- [아키텍처](./ARCHITECTURE.md)
- [에이전트 작업 지도](./AGENTS.md)
- [마스터 프롬프트](./harness/prompts/MASTER-HARNESS.md)
- [구현 설계서](./docs/superpowers/specs/2026-08-02-development-harness-design.md)
- [구현계획](./docs/superpowers/plans/2026-08-02-development-harness-implementation.md)
