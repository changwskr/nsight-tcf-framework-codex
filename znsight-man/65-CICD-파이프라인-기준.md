# 65. CI/CD 파이프라인 기준

> **NSIGHT TCF 개발 Manual** · 원본: [`znsight-guide-word`](../znsight-guide-word/) · 갱신: 2026-07-05

## 65. CI/CD 파이프라인 기준

### 65.1 도입 전 안내말

CI/CD 파이프라인은 개발자가 작성한 소스를 검증 가능한 배포 산출물로 만들고, 승인된 절차에 따라 Tomcat에 반영하는 자동화 흐름이다.
NSIGHT TCF Framework는 단순 Spring Boot 애플리케이션이 아니라 tcf-core, tcf-web, tcf-cache, 업무 WAR, tcf-om, tcf-gateway, tcf-jwt, tcf-batch, tcf-ui, tcf-cicd가 함께 구성된 멀티 모듈 Gradle 프로젝트이다. 기존 모듈 설계에서도 공통 프레임워크, 업무 서비스, 운영관리, 연계·보안, 운영지원, 배포·환경 모듈로 나누어 관리하도록 정리되어 있다.
따라서 CI/CD 기준의 핵심은 다음이다.
개발자는 소스를 Commit하고, CI/CD는 빌드·테스트·WAR 생성·Artifact 저장·배포·Health Check·Rollback을 표준 절차로 수행한다.

### 65.2 CI/CD 기본 원칙

| 원칙 | 기준 |
| --- | --- |
| Git 기준 배포 | 운영 배포는 Git에 등록된 소스와 Tag 기준으로만 수행 |
| Merge Request 기반 | develop, main, release 브랜치 직접 Push 금지 |
| Gradle Wrapper 사용 | Runner는 ./gradlew 기준으로 빌드 |
| 전체 검증 후 배포 | Compile, Unit Test, Mapper 검증, bootWar 성공 후 배포 |
| WAR 단위 배포 | 운영 배포는 Class 단위가 아니라 WAR 단위 |
| 공통 모듈 단독 배포 금지 | tcf-core, tcf-web, tcf-cache는 업무 WAR 내부 WEB-INF/lib에 포함 |
| 환경별 설정 분리 | local/dev/stg/prd 설정은 tcf-cicd 또는 외부 설정으로 분리 |
| 운영 배포 승인 필수 | 운영 배포는 수동 승인 또는 OM 승인 후 수행 |
| Health Check 필수 | 배포 후 Liveness, Readiness, Deep, Smoke Check 수행 |
| Rollback 가능 | 직전 WAR 백업과 Artifact 이력 보관 필수 |
| 이력 관리 | 배포 요청, 승인, 실행, Health Check, Rollback 이력을 OM에 기록 |

NSIGHT 배포관리 기준에서도 OM은 배포 요청·승인·이력·상태 조회·롤백 지시를 담당하고, CI/CD는 빌드·패키징·서버 배포·Health Check·Rollback 실행을 담당하는 구조로 정리되어 있다.

### 65.3 CI/CD 전체 흐름

개발자
```text
  ↓
Git Commit / Push
↓
Merge Request
↓

```

소스 검증
  - Branch 정책 확인
  - Secret 포함 여부 검사
  - 금지 파일 검사
```text
  ↓
```

Gradle Build
  - clean
  - compileJava
  - processResources
  - build
```text
  ↓
Test
```

  - Unit Test
  - Mapper Validation
  - Application Context Loading Test
  - TCF Transaction Test
```text
  ↓
```

Quality Gate
  - 정적 분석
  - 보안 취약점 검사
  - 코드 품질 기준 확인
```text
  ↓
```

bootWar 생성
```text
  ↓
```

Artifact Repository 저장
```text
  ↓
```

Dev 자동 배포
```text
  ↓
```

Staging 수동 승인 배포
```text
  ↓
```

운영 배포 승인
```text
  ↓
```

운영 Rolling Deploy
```text
  ↓
Health Check / Smoke Test
  ↓
OM 배포이력 기록
  ↓

```

서비스 오픈

기존 배포관리 설계에서도 파이프라인은 Source Checkout, 설정 동기화, Gradle Build, Unit Test, bootWar 생성, Artifact 저장, 배포 승인, WAR 백업, 신규 WAR 반영, Tomcat Reload 또는 Restart, Health Check, 서비스 복귀 순서로 정리되어 있다.

### 65.4 표준 Pipeline Stage

Stage
| 목적 | 주요 작업 |
| --- | --- |
| 실패 시 처리 | validate |
| 소스 반영 전 기본 검증 | 브랜치명, 금지 파일, Secret 검사 |
| 즉시 실패 | compile |
| Java 컴파일 검증 | ./gradlew clean compileJava |
| 즉시 실패 | test |
| 단위 테스트 검증 | ./gradlew test |
| 즉시 실패 | quality |
| 품질 Gate | 정적분석, 보안검사, 커버리지 확인 |
| 기준 미달 시 실패 | package |

WAR 생성
```text
./gradlew bootWar
```

| 실패 시 배포 불가 | publish | Artifact 저장 |
| --- | --- | --- |
| WAR, checksum, build-info 저장 | 저장 실패 시 배포 불가 | deploy-dev |
| 개발환경 배포 | Dev Tomcat 배포 | 실패 시 Dev 배포 중단 |

test-dev
개발환경 검증
Health Check, Smoke Test
실패 시 Rollback
deploy-stg

| 검증환경 배포 | 승인 후 Stg 배포 | 실패 시 Rollback |
| --- | --- | --- |
| approve-prd | 운영 승인 | 운영자/PMO 승인 |

승인 전 운영 배포 불가
deploy-prd

| 운영 배포 | Rolling Deploy | 실패 노드 Rollback |
| --- | --- | --- |
| post-check | 배포 후 검증 | Health, 로그, 오류율 확인 |

실패 시 전체 Rollback
rollback
복구
직전 WAR 복구
이력 기록
운영 표준은 GitLab Runner, Gradle, Unit Test, SonarQube, Nexus, WAR 생성, 배포서버, Tomcat 배포, 롤백까지 포함해야 하며, 현재 Gap 분석에서도 .gitlab-ci.yml 표준, 품질 Gate, Artifact 저장소, 자동 Rollback, 단계별 Health Check 보강이 필요하다고 정리되어 있다.

### 65.5 브랜치 기준

| 브랜치 | 용도 |
| --- | --- |
| 배포 가능 환경 | 승인 기준 |
| feature/* | 기능 개발 |
| 배포 불가 | 개발자 자체 빌드 |
| fix/* | 결함 수정 |
| 배포 불가 | 개발자 자체 빌드 |
| develop | 통합 개발 |
| Dev | MR 승인 후 Merge |
| release/* | 검증 릴리즈 |
| Stg | 검증 승인 |
| main 또는 master | 운영 기준 |
| Prd | 운영 승인 |
| hotfix/* | 긴급 수정 |
| Stg / Prd | 긴급 승인 |
| tag v* | 운영 배포 버전 |
| Prd | 공식 Release |

### 65.6 환경별 파이프라인 기준

| 환경 | 트리거 | 배포 방식 | 승인 |
| --- | --- | --- | --- |
| 목적 | Local | 개발자 수동 | bootRun, 로컬 bootWar |
| 없음 | 개발자 확인 | Dev | develop Merge |
| 자동 배포 가능 | 선택 | 통합 개발 확인 | Stg |
| release/* 또는 수동 | 수동 배포 | 필요 | 업무 검증, 성능/장애 검증 |
| Prd | 운영 Tag | 수동 승인 배포 | 필수 |
| 운영 반영 | Hotfix | hotfix/* | 제한적 긴급 배포 |

필수
운영 긴급 수정
운영 반영용 WAR는 개발자 PC에서 생성한 파일이 아니라 CI/CD Runner가 생성한 공식 Artifact를 사용해야 한다.

### 65.7 빌드 기준

| 항목 | 기준 |
| --- | --- |
| Build Tool | Gradle |
| 실행 명령 | ./gradlew clean build |

WAR 생성 명령
```text
./gradlew bootWar
```

| 테스트 포함 | 기본 포함 | 테스트 제외 |
| --- | --- | --- |
| 원칙적으로 금지, 긴급 상황에서만 승인 필요 | 공통 모듈 변경 | 전체 빌드 필수 |

업무 모듈 변경
해당 업무 WAR + 영향 업무 빌드
| 산출물 | 업무별 WAR | 저장 위치 |
| --- | --- | --- |
| Artifact Repository | 식별값 | Git Commit ID, Branch, Tag, Build Time, Checksum |

### 65.8 모듈별 빌드 기준

변경 대상
| 빌드 범위 | 이유 |
| --- | --- |
| tcf-util | 전체 빌드 |
| 모든 모듈 영향 가능 | tcf-core |
| 전체 빌드 | STF/TCF/ETF, Dispatcher 영향 |
| tcf-web | 전체 빌드 |
| /online, Gateway 진입점 영향 | tcf-cache |
| 전체 빌드 또는 Cache 사용 WAR | 공통코드, ServiceId, 정책 Cache 영향 |
| sv-service | sv-service + 연계 모듈 |
| SV 업무 영향 | tcf-om |
| tcf-om + tcf-ui | OM 관리 기능 영향 |
| tcf-gateway | tcf-gateway + 주요 업무 Smoke |
| 라우팅 영향 | tcf-jwt |
| tcf-jwt + 인증 연계 Smoke | 인증 영향 |
| tcf-batch | tcf-batch |
| 운영 수집 배치 영향 | tcf-ui |
| tcf-ui | 화면/Relay 영향 |

### 65.9 Artifact 기준

| 항목 | 기준 |
| --- | --- |
| Artifact 종류 | WAR |
| 저장소 | Nexus 또는 Artifact Repository |
| 파일명 | {module}-{version}-{commitShort}.war |
| 운영 배포명 | Context 기준 WAR명으로 변환 가능 |
| 무결성 | SHA-256 Checksum 생성 |
| 보관 기간 | 운영 기준 최소 3개 이상 버전 보관 |
| Rollback | 직전 정상 버전 즉시 복구 가능해야 함 |
| Metadata | Module, Version, Commit, Branch, Build Time, Builder, Checksum |

예시는 다음과 같다.
빌드 산출물:
sv-service-1.0.0-a1b2c3d.war

운영 배포명:
sv.war

Artifact Metadata:
module=sv-service
businessCode=SV
version=1.0.0
commitId=a1b2c3d
checksum=...
buildTime=2026-07-05 10:30:00

배포관리 설계에서도 OM_DEPLOY_ARTIFACT는 모듈명, WAR명, 버전, Git Commit ID, Checksum, 파일 크기, 저장 위치, 생성일시를 관리하도록 정의되어 있다.

### 65.10 배포 기준

운영 배포는 다음 구조를 따른다.
## 1. 대상 WAS를 L4 또는 Apache에서 제외

## 2. 현재 WAR 백업

## 3. 신규 WAR 반영

## 4. Tomcat Context Reload 또는 Tomcat 재기동

## 5. Liveness Check

## 6. Readiness Check

## 7. Deep Check

## 8. Smoke Test

## 9. 정상 확인 후 트래픽 복귀

## 10. 다음 WAS 반복

| 배포 방식 | 기준 |
| --- | --- |
| 사용 상황 | Rolling Deploy |
| WAS 1대씩 순차 배포 | 운영 표준 |
| Blue/Green | 신규 그룹 배포 후 전환 |
| 대규모 변경 | Full Stop Deploy |
| 전체 중지 후 배포 | 원칙적 지양 |
| Hotfix Deploy | 긴급 수정 배포 |
| 승인된 긴급 장애 | Config Deploy |
| 설정만 배포 | 환경설정 변경 |

### 65.11 Health Check 기준

CI/CD 파이프라인은 배포 후 단순히 프로세스가 살아 있는지만 보면 안 된다. NSIGHT 헬스체크 기준은 다음 4단계로 둔다.
| 단계 | 점검명 |
| --- | --- |
| 기준 | 예시 |
| 1 | Liveness Check |
| JVM/Spring 프로세스 생존 | /actuator/health/liveness |
| 2 | Readiness Check |
| 업무 요청 수신 가능 | /actuator/health/readiness |
| 3 | Deep Check |
| DB, SessionDB, Cache, 외부연계 확인 | /actuator/health/deep |
| 4 | Smoke Check |
| 실제 ServiceId 거래 수행 | SV.Customer.selectSummary 테스트 |

기존 헬스체크 설계에서도 /actuator/health 한 개로 끝내지 않고 Liveness, Readiness, Deep, Smoke 4단계 구조로 설계하는 것이 좋다고 정리되어 있다.

### 65.12 Rollback 기준

Rollback 조건
| 설명 | Tomcat 기동 실패 | Context가 올라오지 않음 | Liveness 실패 |
| --- | --- | --- | --- |
| JVM 또는 Spring Boot 비정상 | Readiness 실패 | 요청 수신 불가 | Deep Check 실패 |
| DB, SESSIONDB, Cache, 외부연계 비정상 | Smoke Test 실패 | 대표 ServiceId 거래 실패 | 오류율 급증 |
| 배포 후 오류율 기준 초과 | Timeout 급증 | 거래 Timeout 증가 | Heap/Thread/Pool 이상 |

자원 사용률 임계치 초과
운영자 판단
업무 영향 발생
Rollback 흐름은 다음과 같다.
## 1. 신규 WAR 서비스 제외

## 2. 신규 WAR 제거

## 3. 직전 정상 WAR 복구

## 4. Tomcat Context Reload 또는 Restart

## 5. Health Check

## 6. Smoke Test

## 7. 트래픽 복귀

## 8. OM 배포이력에 Rollback 기록

## 9. 장애 원인 분석

### 65.13 .gitlab-ci.yml 표준 예시

stages:
  - validate
  - build
  - test
  - quality
  - package
  - publish
  - deploy-dev
  - deploy-stg
  - approve-prd
  - deploy-prd
  - post-check
  - rollback

variables:
  GRADLE_OPTS: "-Dorg.gradle.daemon=false -Dfile.encoding=UTF-8"
  SPRING_PROFILES_ACTIVE: "ci"

before_script:
  - chmod +x gradlew
  - java -version
  - ./gradlew -v

validate:
  stage: validate
  script:
    - echo "Branch policy check"
    - echo "Secret scan"
    - echo "Forbidden file check"
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
    - if: '$CI_COMMIT_BRANCH'

build:
  stage: build
  script:
    - ./gradlew clean compileJava
  artifacts:
    when: always
    paths:
      - "**/build/reports"
    expire_in: 7 days

test:
  stage: test
  script:
    - ./gradlew test
  artifacts:
    when: always
    reports:
      junit:
        - "**/build/test-results/test/TEST-*.xml"
    expire_in: 7 days

quality:
  stage: quality
  script:
    - echo "Static analysis / quality gate"
    - echo "Mapper validation"
    - echo "Application context loading test"

package:
  stage: package
  script:
    - ./gradlew bootWar
    - mkdir -p artifacts
    - find . -path "*/build/libs/*.war" -exec cp {} artifacts/ \;
    - sha256sum artifacts/*.war > artifacts/checksum.txt
  artifacts:
    paths:
      - artifacts/*.war
      - artifacts/checksum.txt
    expire_in: 30 days

publish:
  stage: publish
  script:
    - echo "Upload WAR to Artifact Repository"
    - echo "Save module/version/commit/checksum metadata"

deploy-dev:
  stage: deploy-dev
  script:
    - echo "Deploy to DEV Tomcat"
    - echo "Backup old WAR"
    - echo "Copy new WAR"
    - echo "Restart or reload context"
  environment:
    name: dev
  rules:
    - if: '$CI_COMMIT_BRANCH == "develop"'

deploy-stg:
  stage: deploy-stg
  script:
    - echo "Deploy to STG Tomcat"
    - echo "Run health check"
  environment:
    name: stg
  when: manual
  rules:
    - if: '$CI_COMMIT_BRANCH =~ /^release\/.*$/'

approve-prd:
  stage: approve-prd
  script:
    - echo "Production approval required"
  when: manual
  rules:
    - if: '$CI_COMMIT_TAG =~ /^v.*/'

deploy-prd:
  stage: deploy-prd
  script:
    - echo "Rolling deploy to PROD"
    - echo "Drain traffic"
    - echo "Backup current WAR"
    - echo "Deploy new WAR"
    - echo "Run liveness/readiness/deep/smoke check"
    - echo "Restore traffic"
  environment:
    name: prod
  when: manual
  rules:
    - if: '$CI_COMMIT_TAG =~ /^v.*/'

post-check:
  stage: post-check
  script:
    - echo "Check health, logs, error rate, timeout, thread, heap, pool"
  when: on_success

rollback:
  stage: rollback
  script:
    - echo "Restore previous WAR"
    - echo "Restart context"
    - echo "Run health check"
    - echo "Record rollback history"
  when: manual

### 65.14 CI/CD 품질 Gate

| Gate | 기준 |
| --- | --- |
| Compile Gate | 컴파일 오류 없음 |
| Unit Test Gate | 단위 테스트 실패 없음 |
| Mapper Gate | Mapper Interface와 XML 일치 |
| Context Gate | Spring Application Context 기동 가능 |
| TCF Gate | 대표 ServiceId Dispatcher 테스트 성공 |
| Security Gate | Secret, Password, Private Key 포함 없음 |
| Dependency Gate | 취약 라이브러리 검출 시 차단 |
| Artifact Gate | WAR, Checksum, Build Metadata 생성 |
| Health Gate | 배포 후 Health Check 성공 |
| Smoke Gate | 대표 거래 성공 |

### 65.15 운영 배포 승인 기준

| 승인 항목 | 승인 내용 | 배포 대상 |
| --- | --- | --- |
| 업무코드, WAR명, 버전 확인 | 변경 사유 | 기능 개선, 결함 수정, 긴급 장애 등 |
| 영향 범위 | 업무, DB, 세션, Gateway, Batch 영향 확인 | 테스트 결과 |

Unit, Integration, Smoke 결과 확인
Rollback 계획
직전 버전, 복구 절차, 예상 시간 확인

| 배포 시간 | 업무 영향이 낮은 시간대 확인 | 담당자 |
| --- | --- | --- |
| 개발, 운영, 승인자 확인 | PMO 보고 여부 | 주요 변경 시 보고 필요 여부 확인 |

### 65.16 CI/CD 금지 사항

| 금지 항목 | 사유 |
| --- | --- |
| 운영 서버에서 직접 빌드 | 재현성, 승인, 이력 관리 불가 |
| 개발자 PC WAR 운영 반영 | 공식 Artifact 아님 |
| 테스트 실패 상태 배포 | 장애 유입 가능 |
| -x test 운영 배포 | 품질 Gate 우회 |
| Class 단위 배포 | 무결성, 롤백 불가 |
| Tomcat lib에 공통 JAR 수동 복사 | WAR 간 버전 충돌 |
| 운영 Secret Git 저장 | 보안 사고 위험 |
| Health Check 생략 | 장애 조기 감지 실패 |
| Rollback 파일 미보관 | 장애 시 복구 지연 |
| 승인 없는 운영 배포 | 감사·통제 위반 |

### 65.17 개발자 체크리스트

| 점검 항목 | 확인 |
| --- | --- |
| 작업 브랜치가 표준명으로 생성되었는가? |  |
| MR 전 로컬 ./gradlew clean build를 수행했는가? |  |
| 테스트를 제외하지 않았는가? |  |
| Mapper XML 변경 시 Mapper 테스트를 수행했는가? |  |
| ServiceId 신규 추가 시 OM 등록 기준을 확인했는가? |  |
| application-local.yml 외 운영 Secret을 Commit하지 않았는가? |  |
| WAR 파일명과 Context가 일치하는가? |  |
| 대표 /online 거래를 Smoke Test 했는가? |  |
| 변경 영향 범위를 MR 설명에 작성했는가? |  |
| Rollback 가능성을 고려했는가? |  |

### 65.18 운영자 체크리스트

| 점검 항목 | 확인 |
| --- | --- |
| 배포 요청과 승인 이력이 존재하는가? |  |
| 배포 대상 WAR 버전과 Commit ID가 확인되는가? |  |
| 직전 WAR가 백업되었는가? |  |
| 대상 WAS를 트래픽에서 제외했는가? |  |
| 신규 WAR 배포 후 Tomcat Context가 정상 기동했는가? |  |
| Liveness / Readiness / Deep Check가 성공했는가? |  |
| 대표 ServiceId Smoke Test가 성공했는가? |  |
| 오류율, Timeout, Thread, Heap, Hikari Pool이 정상인가? |  |
| OM 배포 이력이 기록되었는가? |  |
| 실패 시 Rollback 절차를 수행할 준비가 되어 있는가? |  |

### 65.19 마무리말

CI/CD 파이프라인은 개발 편의를 위한 자동화 도구가 아니라 운영 안정성을 보장하는 품질 통제 체계이다.
NSIGHT TCF Framework는 업무 WAR, 공통 TCF 모듈, Gateway, JWT, OM, Batch, UI가 함께 동작하므로 배포 자동화 기준이 없으면 업무 장애뿐 아니라 세션, 인증, 거래통제, Timeout, 로그, 운영관리까지 영향을 받을 수 있다.

### 65.20 시사점

NSIGHT의 CI/CD 기준은 다음 한 문장으로 정리할 수 있다.
소스는 Git으로 통제하고, 빌드는 Runner가 수행하며, 산출물은 Artifact로 보관하고, 운영 배포는 승인·Health Check·Rollback이 가능한 파이프라인으로만 수행한다.

## 소스·관련 문서

| 참고 |
|------|

> znsight-guide-word: `통합 (63).docx`

| [tcf-cicd-개발가이드.md](../zguide/tcf-cicd-개발가이드.md) |
| [21-CICD-배포.md](../zman/21-CICD-배포.md) |

## 코드베이스 정정 (develop 기준)

| 항목 | 값 |
|------|-----|
| 업무 WAR | ic, pc, ms, sv, pd, eb, ep, ss, mg + tcf-om |
| ztomcat deploy | `ztomcat/deploy-wars.sh` 13 WAR |
| buildZtomcatWars | 15 WAR |
| bootRun | gateway 8100, uj 8102, jwt 8110, ui 8099 |


---

← [64. WAR 생성 기준](./64-WAR-생성-기준.md) · [66. 배포 절차](./66-배포-절차.md) →