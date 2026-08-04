# NSIGHT Development Harness 검증 보고서

## 1. 도입 전 안내말

본 보고서는 하네스 초기 구현의 소스, 상태전이, 프롬프트 계약, 보안 차단, 테스트 재시도와 전체 생명주기 동작을 검증한 결과를 기록한다.

## 2. 검증 환경

| 항목 | 값 |
|---|---|
| 검증일 | 2026-08-02 |
| Java | OpenJDK 21.0.10 |
| OS | Linux 컨테이너 |
| Git 작업 Branch | `harness/REQ-20260802-001-initial-harness` |
| Gradle | 실행환경에 미설치 |
| 외부 인터넷 | 실행 프로세스에서 DNS 접근 불가 |

## 3. TDD Red–Green 증적

### 3.1 RED

생산 클래스 작성 전 `OfflineHarnessSmokeTest`를 컴파일하여 다음 누락 오류를 확인했다.

```text
package com.nh.nsight.harness.domain does not exist
package com.nh.nsight.harness.service does not exist
package com.nh.nsight.harness.storage does not exist
```

증적:

- `verification/red/offline-smoke.exitcode`
- `verification/red/offline-smoke.stderr.log`

### 3.2 GREEN

핵심 도메인과 서비스를 구현한 뒤 동일 Smoke Test를 실행해 다음 결과를 확인했다.

```text
OFFLINE_SMOKE_TEST_PASS
```

검증 범위:

- JSON 상태 저장·복원
- 고정 요건 12문항
- 요건 미승인 분석 차단
- 요건 승인 후 분석 진입
- Classpath 프롬프트 Fallback
- Gradle 테스트 명령 후보 탐지
- Credential Literal 명령 차단
- 테스트 실패 최대 3회 제한

증적:

- `verification/green/offline-smoke-run.stdout.log`
- `scripts/offline-smoke-test.sh`

## 4. 컴파일 검증

### 4.1 의존성 없는 전체 코어

다음 범위를 JDK 21 `javac -Xlint:all`로 컴파일했다.

```text
src/main/java 전체
- HarnessApplication.java 제외
```

결과:

```text
경고 0건
오류 0건
```

증적:

- `verification/green/javac-xlint.stdout.log`
- `verification/green/javac-xlint.stderr.log`

### 4.2 Spring Boot 진입점 구문 검증

Spring Boot API의 최소 검증용 Stub을 사용하여 `HarnessApplication`을 포함한 전체 Main 소스의 Java 구문·타입 연결을 컴파일했다.

결과:

```text
FULL_MAIN_SYNTAX_PASS
```

증적:

- `verification/green/compile-syntax-self-test.stdout.log`
- `verification/green/compile-syntax-self-test.stderr.log`

### 4.3 JUnit 테스트 소스 구문 검증

JUnit·AssertJ 최소 검증용 Stub을 사용하여 전체 테스트 소스의 구문과 Main API 연결을 컴파일했다.

결과:

```text
FULL_TEST_SYNTAX_PASS
```

증적:

- `verification/green/compile-syntax-self-test.stdout.log`
- `verification/green/compile-syntax-self-test.stderr.log`

> Stub 컴파일은 실제 Spring Boot·JUnit 실행을 대체하지 않는다. 사내 Maven Proxy 또는 인터넷 연결이 가능한 환경에서 Gradle 전체 테스트를 추가 실행해야 한다.

## 5. End-to-End 생명주기 검증

임시 Git 저장소를 생성하여 다음 전체 절차를 실행했다.

```text
Git 초기화·Commit
→ 전용 Harness Branch 생성
→ 12문항 답변
→ REQUIREMENT 승인
→ ANALYSIS 생성·수동 Review·승인
→ DESIGN 생성
→ 실행계획 누락 Review 차단 확인
→ 실행계획 추가·승인
→ IMPLEMENTATION 생성·승인
→ Credential 포함 Test Command 차단 확인
→ 승인된 Smoke Test 실행
→ TEST 승인
→ CLOSE 생성·승인
```

최종 상태:

```text
REQUIREMENT     APPROVED
ANALYSIS        APPROVED
DESIGN          APPROVED
IMPLEMENTATION  APPROVED
TEST            APPROVED
CLOSE           APPROVED
```

추가 단언:

- UTF-8 한글 제목이 상태 JSON에 보존됨
- 테스트 증적 `test-summary.md` 생성됨
- 실행계획이 없으면 DESIGN Review가 차단됨
- Bearer Token Literal이 포함된 명령은 승인이 차단됨

증적:

- `verification/final-e2e/workflow.stdout.log`
- `verification/final-e2e/workflow.stderr.log`
- `verification/final-e2e/missing-plan.stderr.log`
- `verification/final-e2e/secret-command.stderr.log`

## 6. 패키지 정합성 검증

`scripts/validate-package.sh`가 다음 항목을 검증한다.

- 필수 파일 존재
- Top-level Prompt와 Classpath Resource 일치
- JSON Schema 문법
- 주요 Markdown 내부 링크
- Placeholder·TODO 잔존 여부
- Offline Smoke Test
- Git Diff 공백 오류

결과:

```text
JSON_AND_LINK_VALIDATION_PASS
OFFLINE_SMOKE_TEST_PASS
PACKAGE_VALIDATION_PASS
```

## 7. Gradle 전체 빌드 상태

다음 명령을 시도했다.

```bash
./gradlew clean test bootJar
```

현재 검증 컨테이너에는 Gradle이 설치되어 있지 않고 외부 배포본·Maven 의존성을 내려받을 네트워크가 없어 실행되지 않았다.

```text
Gradle 8.10.2 is not installed.
Run scripts/install-gradle.sh or set GRADLE_HOME, then retry.
```

따라서 다음 항목은 사용자 또는 사내 CI 환경에서 반드시 추가 검증해야 한다.

```bash
sh scripts/install-gradle.sh   # 또는 사내 표준 Gradle 배포본 사용
./gradlew clean test bootJar
java -jar build/libs/nsight-development-harness.jar help
```

## 8. 구현 규모

| 구분 | 수량 |
|---|---:|
| Main Java | 41개 |
| JUnit Test | 6개 |
| Offline Smoke Test | 1개 |
| Prompt | 8개 |
| Markdown Template | 6개 |
| JSON Schema | 2개 |

## 9. 최종 판단

| 영역 | 판단 |
|---|---|
| 코어 상태·Gate·CLI | JDK 21 환경에서 실행 검증 완료 |
| 전체 생명주기 | 임시 Git 저장소 E2E 검증 완료 |
| 보안 명령 차단 | 검증 완료 |
| 테스트 3회 제한 | 검증 완료 |
| Spring Boot 전체 Gradle Build | 환경 제약으로 미검증 |
| 운영 적용 | 사내 Gradle·Maven 환경에서 전체 Build 후 적용 필요 |
