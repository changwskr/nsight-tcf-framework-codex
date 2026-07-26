# 7. Git 저장소 및 브랜치 사용 기준

> **NSIGHT TCF 개발 Manual** · 원본: [`znsight-guide-word`](../znsight-guide-word/) · 갱신: 2026-07-05

## 7. Git 저장소 및 브랜치 사용 기준

### 7.1 도입 전 안내말

본 장은 NSIGHT TCF Framework 개발자가 Git 저장소를 어떻게 사용하고, 어떤 브랜치에서 개발하며, Merge Request를 통해 어떤 기준으로 소스를 반영해야 하는지 정의한다.
NSIGHT TCF Framework는 단일 애플리케이션이 아니라 공통 Framework, 업무 서비스, OM, Gateway, JWT, Batch, UI, CI/CD 설정이 함께 구성되는 멀티 모듈 프로젝트이다. 따라서 Git 브랜치 운영 기준이 명확하지 않으면 다음 문제가 발생한다.
| 문제 | 설명 |
| --- | --- |
| 공통 모듈 영향 확산 | tcf-core, tcf-web 변경이 전체 업무 WAR에 영향을 줄 수 있음 |
| 업무별 개발 충돌 | SV, IC, OM 등 여러 업무팀이 같은 모듈을 동시에 수정할 수 있음 |
| 배포 기준 불명확 | 어떤 소스가 개발, 검증, 운영 배포 대상인지 구분하기 어려움 |
| 롤백 어려움 | 장애 발생 시 어떤 Commit 또는 Tag로 복구해야 하는지 추적 어려움 |
| 품질 저하 | 테스트, 코드 리뷰, 보안 점검 없이 소스가 반영될 수 있음 |

NSIGHT CI/CD 운영 기준은 GitLab, GitLab Runner, Gradle, WAR 배포를 기준으로 개발 표준화, 형상관리 표준화, 빌드 자동화, 배포 자동화, 롤백 자동화를 목표로 한다. 또한 NSIGHT Java 코딩 스타일 가이드는 Spring Boot, MyBatis, 로그, 예외, 트랜잭션, 보안, 테스트 기준을 통합 개발 표준으로 정의한다.

### 7.2 Git 저장소 운영 목표

Git 저장소 운영의 목표는 다음과 같다.
| 목표 | 설명 |
| --- | --- |
| 개발 이력 추적 | 누가, 언제, 왜 수정했는지 Commit 기준으로 추적 |
| 브랜치 안정성 확보 | 개발, 검증, 운영 배포 소스를 명확히 분리 |
| 병렬 개발 지원 | 업무별 기능 개발을 독립 브랜치에서 수행 |
| 품질 게이트 적용 | Merge 전 빌드, 테스트, 코드 리뷰, 보안 점검 수행 |
| 배포 자동화 연계 | Merge 또는 Tag 기준으로 CI/CD Pipeline 실행 |
| 롤백 가능성 확보 | 운영 배포 버전은 Tag로 관리하여 즉시 복구 가능하게 함 |

### 7.3 Git 저장소 기본 구조

NSIGHT TCF Framework 저장소는 다음 구조를 기준으로 운영한다.
nsight-tcf-framework
```text
 ├─ tcf-util
 ├─ tcf-core
 ├─ tcf-web
 ├─ tcf-cache
 ├─ tcf-om
 ├─ tcf-gateway
 ├─ tcf-jwt
 ├─ tcf-batch
 ├─ tcf-ui
 ├─ sv-service
 ├─ ic-service
 ├─ pc-service
 ├─ (미포함·확장 예정)
 ├─ mg-service
 ├─ build.gradle
 ├─ settings.gradle
 └─ .gitlab-ci.yml
```

| 영역 | 관리 기준 |
| --- | --- |
| 공통 모듈 | 공통 개발자 또는 아키텍처 승인 필요 |
| 업무 서비스 모듈 | 업무 개발팀 책임 |
| OM 모듈 | 운영관리 개발팀 책임 |
| Gateway / JWT | 보안·공통 영역으로 별도 승인 필요 |
| Batch | 운영 배치 담당자 승인 필요 |
| CI/CD 설정 | DevOps 담당자 승인 필요 |
| 문서 / 가이드 | 아키텍처 또는 PMO 승인 필요 |

### 7.4 브랜치 전략 결론

NSIGHT는 다음 브랜치 전략을 표준으로 사용한다.
main
```text
 └─ 운영 배포 기준 브랜치
develop
 └─ 통합 개발 기준 브랜치
```

feature/*
```text
 └─ 기능 개발 브랜치
```

release/*
```text
 └─ 배포 준비 브랜치
```

hotfix/*
```text
 └─ 운영 긴급 수정 브랜치
```

| 브랜치 | 용도 | 생성 기준 | Merge 대상 | 삭제 여부 |
| --- | --- | --- | --- | --- |
| main | 운영 배포 기준 | 최초 생성 | 없음 | 삭제 금지 |
| develop | 통합 개발 기준 | 최초 생성 | main, release/* | 삭제 금지 |
| feature/* | 기능 개발 | 개발자별 생성 | develop | Merge 후 삭제 |
| release/* | 배포 준비 | 배포 단위 생성 | main, develop | 배포 후 삭제 가능 |
| hotfix/* | 운영 긴급 수정 | 장애 또는 긴급 결함 시 생성 | main, develop | 반영 후 삭제 |
| docs/* | 문서 수정 | 문서 작업 시 생성 | develop | Merge 후 삭제 |
| chore/* | 빌드·설정 변경 | Gradle, CI/CD 등 변경 시 생성 | develop | Merge 후 삭제 |

### 7.5 브랜치별 역할 상세

#### 7.5.1 main 브랜치

main은 운영 배포 기준 브랜치이다.
| 항목 | 기준 |
| --- | --- |
| 용도 | 운영 배포 완료 소스 보관 |
| 직접 Commit | 금지 |
| Merge 방식 | release/* 또는 hotfix/*에서만 Merge |
| CI/CD | 운영 배포 Pipeline 연계 |
| 보호 설정 | 필수 |
| Tag 생성 | 운영 배포 시 필수 |
| 롤백 기준 | 운영 Tag 기준 |
main에는 검증되지 않은 기능 개발 소스가 직접 들어가면 안 된다.

develop은 통합 개발 기준 브랜치이다.

| 7.5.2 develop 브랜치 | |

| 항목 | 기준 |
| --- | --- |
| 용도 | 개발 완료 기능 통합 |
| 직접 Commit | 원칙적 금지 |
| Merge 방식 | feature/*, docs/*, chore/*에서 Merge |
| CI/CD | 개발 빌드 및 테스트 Pipeline 실행 |
| 테스트 기준 | 전체 빌드 또는 변경 모듈 빌드 성공 |
| 품질 기준 | 코드 리뷰 승인 후 Merge |

개발자는 develop에서 직접 작업하지 않고, 반드시 기능 브랜치를 생성하여 작업한다.
git checkout develop
git pull origin develop
git checkout -b feature/sv-customer-summary

#### 7.5.3 feature 브랜치

feature/*는 신규 기능 개발 브랜치이다.
| 항목 | 기준 |
| --- | --- |
| 생성 기준 | 신규 ServiceId, 화면, Mapper, Rule, Service 개발 |
| 생성 위치 | develop |
| Merge 대상 | develop |
| 브랜치명 | feature/{업무코드}-{기능명} |
| 예시 | feature/sv-customer-summary |
| 삭제 | Merge 후 삭제 |

예시는 다음과 같다.
git checkout develop
git pull origin develop
git checkout -b feature/sv-customer-summary

작업 완료 후 원격 저장소에 Push한다.
git push origin feature/sv-customer-summary

#### 7.5.4 release 브랜치

release/*는 운영 배포 준비 브랜치이다.
| 항목 | 기준 |
| --- | --- |
| 생성 기준 | 배포 후보 확정 시 |
| 생성 위치 | develop |
| Merge 대상 | main, develop |
| 용도 | 배포 전 결함 수정, 설정 확인, 최종 테스트 |
| 브랜치명 | release/v{버전} |
| 예시 | release/v1.0.0 |
| Tag | 운영 배포 후 생성 |

배포 준비 브랜치에서는 신규 기능 개발을 하지 않는다.오직 배포 전 결함 수정, 설정 보정, 문서 보완만 허용한다.
git checkout develop
git pull origin develop
git checkout -b release/v1.0.0

#### 7.5.5 hotfix 브랜치

hotfix/*는 운영 장애 또는 긴급 결함 수정 브랜치이다.
| 항목 | 기준 |
| --- | --- |
| 생성 기준 | 운영 장애, 보안 결함, 긴급 오류 |
| 생성 위치 | main |
| Merge 대상 | main, develop |
| 브랜치명 | hotfix/{장애번호}-{수정내용} |
| 예시 | hotfix/inc-20260704-timeout-error |
| Tag | 수정 배포 후 Patch Tag 생성 |
Hotfix는 반드시 main에서 생성한다.

| git checkout main | |
| git pull origin main | git checkout -b hotfix/inc-20260704-timeout-error |

수정 후에는 main에 먼저 반영하고, 동일 수정 사항을 반드시 develop에도 반영한다.
hotfix/*
```text
   ↓
main
   ↓
```

운영 배포 Tag
```text
   ↓
```

develop 반영

### 7.6 브랜치 명명 규칙

브랜치명은 다음 형식을 따른다.
{유형}/{업무코드}-{기능명}

| 유형 | 용도 | 예시 |
| --- | --- | --- |
| feature | 신규 기능 | feature/sv-customer-summary |
| fix | 개발 결함 수정 | fix/sv-customer-summary-null |
| hotfix | 운영 긴급 수정 | hotfix/inc-20260704-session-error |
| release | 배포 준비 | release/v1.0.0 |
| docs | 문서 수정 | docs/developer-guide-branch |
| chore | 빌드·환경 변경 | chore/gradle-version-update |
| refactor | 구조 개선 | refactor/tcf-dispatcher-cleanup |
| test | 테스트 보강 | test/sv-customer-service |

업무코드는 대문자 대신 브랜치에서는 소문자로 사용한다.
| 업무코드 | 브랜치 표기 |
| --- | --- |
| SV | sv |
| OM | om |
| IC | ic |
| Gateway | gateway |
| JWT | jwt |
| Batch | batch |
| 공통 TCF | tcf |

### 7.7 Commit 메시지 기준

Commit 메시지는 다음 형식을 따른다.
{type}({scope}): {message}

예시는 다음과 같다.
feat(sv): 고객 요약 조회 Handler 추가
fix(tcf): serviceId 미등록 시 오류코드 보정
docs(guide): Git 브랜치 사용 기준 추가
test(sv): 고객 요약 조회 Service 테스트 추가
chore(ci): GitLab Runner 빌드 단계 수정

| Type | 의미 | 예시 |
| --- | --- | --- |
| feat | 기능 추가 | 신규 ServiceId 개발 |
| fix | 오류 수정 | Null 오류, SQL 오류 수정 |
| docs | 문서 수정 | 개발자 가이드 수정 |
| style | 포맷 수정 | 들여쓰기, 공백 수정 |
| refactor | 리팩토링 | 구조 개선, 책임 분리 |
| test | 테스트 추가·수정 | JUnit, Mapper 테스트 |
| chore | 빌드·환경 작업 | Gradle, CI/CD 수정 |
| perf | 성능 개선 | SQL 개선, Cache 적용 |
| security | 보안 수정 | 마스킹, 권한 검증 보완 |

Scope는 업무코드 또는 모듈명을 사용한다.
| Scope | 의미 |
| --- | --- |
| tcf | TCF 공통 |
| core | tcf-core |
| web | tcf-web |
| cache | tcf-cache |
| om | 운영관리 |
| sv | Single View |
| ic | IC 업무 |
| gateway | Gateway |
| jwt | JWT |
| batch | Batch |
| ci | CI/CD |

### 7.8 Commit 단위 기준

Commit은 너무 크거나 너무 작지 않게 작성한다.
| 좋은 Commit | 나쁜 Commit |
| --- | --- |
| 하나의 ServiceId Handler 추가 | 여러 업무 기능을 한 번에 수정 |
| Mapper SQL 한 건 수정 | SQL, 화면, 공통 모듈을 섞어서 수정 |
| 오류코드 매핑 보정 | “수정”, “작업중” 같은 모호한 메시지 |
| 테스트 코드 추가 | 기능 개발과 테스트 삭제를 함께 Commit |
| 설정 파일 분리 | 운영 설정과 로컬 설정을 섞어서 Commit |

Commit 단위는 다음 기준을 권장한다.
1 Commit = 1개의 의미 있는 변경

예를 들어 SV.Customer.selectSummary 신규 개발 시 Commit을 다음처럼 나눌 수 있다.
feat(sv): 고객 요약 조회 DTO 추가
feat(sv): 고객 요약 조회 Handler 및 Facade 추가
feat(sv): 고객 요약 조회 Service Rule DAO 추가
feat(sv): 고객 요약 조회 Mapper SQL 추가
test(sv): 고객 요약 조회 테스트 추가

### 7.9 Merge Request 기준

모든 소스 반영은 Merge Request 기준으로 수행한다.
feature/*
```text
   ↓
Merge Request
↓
Code Review
↓
CI Build / Test
↓
승인
↓
develop

```

| 항목 | 기준 |
| --- | --- |
| MR 제목 | 변경 목적이 드러나야 함 |
| MR 설명 | 변경 내용, 영향 범위, 테스트 결과 작성 |
| Reviewer | 업무 리더 또는 공통 담당자 지정 |
| Approver | 영향도에 따라 아키텍트 또는 파트장 승인 |
| CI 결과 | 성공 필수 |
| Conflict | 개발자가 직접 해결 |
| Merge 방식 | Squash 또는 Merge Commit 정책에 따름 |
| Merge 후 브랜치 | 삭제 권장 |

MR 설명에는 다음 항목을 포함한다.
| 항목 | 내용 |
| --- | --- |
| 변경 목적 | 왜 수정했는가 |
| 변경 대상 | 어떤 모듈, 클래스, Mapper가 바뀌었는가 |
| ServiceId | 신규 또는 변경 ServiceId |
| DB 영향 | 테이블, 컬럼, SQL 변경 여부 |
| 설정 영향 | application.yml, Timeout, Cache 변경 여부 |
| 보안 영향 | 권한, 세션, JWT, 개인정보 영향 여부 |
| 테스트 결과 | 단위 테스트, API 테스트, Mapper 테스트 결과 |
| 배포 영향 | WAR 재배포 필요 여부 |
| 롤백 방법 | 문제 발생 시 되돌리는 방법 |

### 7.10 Merge Request 템플릿

MR 작성 시 다음 템플릿을 사용한다.
## 1. 변경 개요
- 변경 목적:
- 관련 업무코드:
- 관련 ServiceId:
- 관련 거래코드:

## 2. 변경 파일
- Handler:
- Facade:
- Service / Rule:
- DAO / Mapper:
- application.yml:
- 기타:

## 3. 영향 범위
- 업무 영향:
- 공통 모듈 영향:
- DB 영향:
- 배포 영향:
- 보안 영향:

## 4. 테스트 결과
- 단위 테스트:
- 통합 테스트:
- 표준 전문 호출 테스트:
- Mapper SQL 테스트:

## 5. 체크리스트
- [ ] local profile 실행 확인
- [ ] Gradle build 성공
- [ ] 테스트 성공
- [ ] ServiceId 등록 기준 확인
- [ ] 거래코드 기준 확인
- [ ] 오류코드 기준 확인
- [ ] 개인정보 로그 출력 없음
- [ ] 운영 설정 직접 수정 없음

## 6. 롤백 방법
- 롤백 대상:
- 롤백 절차:

### 7.11 브랜치 보호 기준

다음 브랜치는 보호 브랜치로 지정한다.
| 브랜치 | 보호 수준 |
| --- | --- |
| main | 직접 Push 금지, MR 필수, 승인 필수 |
| develop | 직접 Push 금지, MR 필수 |
| release/* | 배포 담당자만 Push 가능 |
| hotfix/* | 긴급 수정 담당자만 Push 가능 |

보호 브랜치 기준은 다음과 같다.
| 항목 | main | develop | release/* |
| --- | --- | --- | --- |
| 직접 Push | 금지 | 금지 | 제한 |
| Force Push | 금지 | 금지 | 금지 |
| MR 필수 | 필수 | 필수 | 필수 |
| 승인자 수 | 2명 이상 권장 | 1명 이상 | 2명 이상 권장 |
| CI 성공 | 필수 | 필수 | 필수 |
| Tag 생성 | 배포 시 필수 | 선택 | 배포 준비 시 선택 |

### 7.12 모듈별 승인 기준

모듈 변경 영향도에 따라 승인자를 다르게 지정한다.
| 변경 영역 | 승인 기준 |
| --- | --- |
| sv-service, ic-service 등 업무 모듈 | 업무 리더 승인 |
| tcf-core | 공통 프레임워크 담당자 + 아키텍트 승인 |
| tcf-web | 공통 프레임워크 담당자 + 아키텍트 승인 |
| tcf-cache | 공통 담당자 승인 |
| tcf-om | OM 담당자 승인 |
| tcf-gateway | Gateway 담당자 + 보안 담당자 승인 |
| tcf-jwt | 보안 담당자 + 아키텍트 승인 |
| tcf-batch | 배치 담당자 승인 |
| .gitlab-ci.yml | DevOps 담당자 승인 |
| application-prd.yml | 운영 담당자 + 아키텍트 승인 |
| DB DDL | DBA 승인 |
| 권한·인증 관련 코드 | 보안 담당자 승인 |

### 7.13 Tag 사용 기준

운영 배포 버전은 Tag로 관리한다.
v{major}.{minor}.{patch}

예시는 다음과 같다.
| Tag | 의미 |
| --- | --- |
| v1.0.0 | 최초 운영 배포 |
| v1.1.0 | 기능 추가 배포 |
| v1.1.1 | Patch 수정 배포 |
| v2.0.0 | 구조 변경 또는 대규모 변경 |

운영 배포 시 Tag 생성 예시는 다음과 같다.
git checkout main
git pull origin main
git tag -a v1.0.0 -m "NSIGHT TCF Framework v1.0.0 운영 배포"
git push origin v1.0.0

Tag에는 다음 정보를 남긴다.
| 항목 | 설명 |
| --- | --- |
| 배포 버전 | v1.0.0 |
| 배포 일자 | 운영 반영 일자 |
| 배포 대상 | 업무 WAR, OM, Gateway, Batch 등 |
| 주요 변경 | 기능 추가, 오류 수정, 설정 변경 |
| Rollback Tag | 이전 안정 버전 |
| 승인 정보 | 배포 승인자 |

### 7.14 환경별 브랜치와 배포 관계

브랜치와 배포 환경은 다음 관계를 따른다.
feature/*
```text
   ↓
develop
   ↓
```

개발환경 배포

release/*
```text
   ↓
```

검증환경 / 스테이징 배포

main + tag
```text
   ↓
```

운영환경 배포

hotfix/*
```text
   ↓
```

운영 긴급 배포
```text
   ↓
```

develop 재반영

| 환경 | 기준 브랜치 | 배포 방식 |
| --- | --- | --- |
| Local | feature/* | 개발자 PC bootRun |
| Dev | develop | CI/CD 자동 또는 수동 배포 |
| STG | release/* | 배포 승인 후 배포 |
| PRD | main + Tag | 운영 승인 후 배포 |
| Hotfix | hotfix/* → main | 긴급 승인 후 배포 |

### 7.15 Git 작업 표준 흐름

신규 기능 개발의 표준 흐름은 다음과 같다.
## 1. develop 최신화

## 2. feature 브랜치 생성

## 3. 기능 개발

## 4. 로컬 테스트

## 5. Commit

## 6. Push

## 7. Merge Request 생성

## 8. Code Review

## 9. CI Build / Test 확인

## 10. develop Merge

## 11. 개발환경 배포

명령 예시는 다음과 같다.
git checkout develop
git pull origin develop

git checkout -b feature/sv-customer-summary

# 개발 작업 수행

git status
git add .
git commit -m "feat(sv): 고객 요약 조회 서비스 추가"
git push origin feature/sv-customer-summary

### 7.16 공통 모듈 변경 기준

tcf-core, tcf-web, tcf-util, tcf-cache 변경은 전체 업무에 영향을 줄 수 있으므로 별도 기준을 따른다.
| 항목 | 기준 |
| --- | --- |
| 변경 전 검토 | 영향도 분석 필수 |
| 승인자 | 공통 담당자 + 아키텍트 |
| 테스트 | 전체 모듈 빌드 및 주요 업무 Smoke Test |
| 문서 | 개발자 가이드 또는 설계서 반영 |
| 배포 | 전체 업무 WAR 영향 여부 확인 |
| Rollback | 이전 공통 모듈 버전 복구 절차 필요 |
공통 모듈 변경 시 MR에는 반드시 다음 내용을 작성한다.

| 항목 | |
| 설명 | 변경 이유 |
| 왜 공통 모듈을 수정해야 하는가 | 영향 모듈 |
| 어떤 업무 WAR에 영향이 있는가 | 호환성 |
| 기존 Handler, DTO, Mapper와 호환되는가 | 테스트 범위 |
| 어떤 업무 ServiceId로 검증했는가 | 장애 가능성 |
| 실패 시 어떤 문제가 발생할 수 있는가 | 롤백 방법 |
| 이전 버전으로 되돌릴 수 있는가 |  |

### 7.17 업무 모듈 변경 기준

업무 모듈 변경은 해당 업무팀 책임으로 수행하되, TCF 표준을 반드시 준수한다.

| 변경 대상 | 기준 | Handler |
| --- | --- | --- |
| ServiceId와 1:1 매핑 | Facade | 트랜잭션 경계 관리 |

| Service | 업무 흐름 조립 | Rule |
| --- | --- | --- |
| 업무 규칙과 검증 | DAO | Mapper 호출 캡슐화 |
| Mapper XML | SQL ID 표준 준수 | DTO |

Request / Response 분리

| 오류코드 | 업무코드 기반 오류코드 사용 | 테스트 |
| --- | --- | --- |
업무 모듈 변경 시 MR에는 반드시 ServiceId와 거래코드를 명시한다.

| Rule, Service, Mapper 테스트 필수 | ServiceId: SV.Customer.selectSummary |

TransactionCode: SV-INQ-0001
BusinessCode: SV

### 7.18 충돌 해결 기준

Merge Conflict가 발생하면 브랜치 작성자가 해결한다.

| 충돌 유형 | 해결 기준 | 업무 소스 충돌 |
| --- | --- | --- |
| 업무 개발자가 해결 | 공통 모듈 충돌 | 공통 담당자와 협의 |

| Mapper XML 충돌 | SQL ID 중복 여부 확인 |
| --- | --- |
| application.yml 충돌 | 환경 설정 담당자 확인 |

build.gradle 충돌
DevOps 담당자 확인
문서 충돌
문서 작성자 확인
충돌 해결 후에는 반드시 다시 빌드와 테스트를 수행한다.
```text
./gradlew clean build
```

### 7.19 금지사항

| 금지사항 | 사유 |
| --- | --- |
| main 직접 Push | 운영 소스 오염 위험 |
| develop 직접 Push | 통합 개발 기준 훼손 |
| Force Push | 이력 손실 위험 |
| 운영 Secret Commit | 보안 사고 위험 |
| 운영 DB 접속정보 Commit | 정보 유출 위험 |
| 테스트 실패 상태 Merge | 품질 저하 |
| 리뷰 없이 Merge | 책임 추적 불가 |
| 여러 기능을 한 MR에 포함 | 영향도 분석 어려움 |
| 공통 모듈 무단 변경 | 전체 업무 장애 가능 |
| Tag 재생성 또는 삭제 | 배포 이력 훼손 |
| 운영 장애 수정 후 develop 미반영 | 동일 장애 재발 위험 |

### 7.20 Git 사용 체크리스트

| 점검 항목 | 확인 |
| --- | --- |
| develop 최신 상태에서 브랜치를 생성했는가 | □ |
| 브랜치명이 표준을 따르는가 | □ |
| Commit 메시지가 표준 형식을 따르는가 | □ |
| 하나의 MR에 하나의 목적만 포함했는가 | □ |
| ServiceId와 거래코드를 MR에 작성했는가 | □ |
| 로컬 빌드를 수행했는가 | □ |
| 단위 테스트를 수행했는가 | □ |
| Mapper SQL 테스트를 수행했는가 | □ |
| 개인정보 또는 Secret이 Commit되지 않았는가 | □ |
| 공통 모듈 변경 시 영향도 분석을 작성했는가 | □ |
| CI Pipeline이 성공했는가 | □ |
| Reviewer 승인을 받았는가 | □ |
| Merge 후 feature 브랜치를 삭제했는가 | □ |

### 7.21 마무리말

Git 저장소와 브랜치 기준은 단순한 개발 편의 규칙이 아니다.NSIGHT TCF Framework에서는 Git 브랜치가 곧 개발, 검증, 배포, 운영, 롤백의 기준선이 된다.
특히 tcf-core, tcf-web, tcf-gateway, tcf-jwt 같은 공통·보안 모듈은 작은 수정도 전체 업무 WAR에 영향을 줄 수 있다. 따라서 모든 변경은 기능 브랜치에서 시작하고, Merge Request, Code Review, CI Build, Test를 통과한 뒤에만 기준 브랜치에 반영해야 한다.

### 7.22 시사점

NSIGHT 개발자는 Git을 단순 소스 저장소로 사용하면 안 된다.Git은 다음 세 가지 역할을 동시에 수행해야 한다.
| 역할 | 의미 |
| --- | --- |
| 개발 통제 | 누가 어떤 기능을 어떤 브랜치에서 개발했는지 관리 |
| 품질 통제 | Merge 전 빌드, 테스트, 리뷰를 강제 |
| 운영 통제 | 운영 배포 버전과 롤백 기준을 Tag로 보장 |

결론적으로 NSIGHT의 Git 운영 기준은 다음 한 문장으로 정리할 수 있다.
모든 개발은 feature 브랜치에서 시작하고, 모든 반영은 Merge Request로 수행하며, 모든 운영 배포는 main Tag로 추적한다.

## 소스·관련 문서

| 참고 |
|------|

> znsight-guide-word: `통합 (12).docx`

| [21-CICD-배포.md](../zman/21-CICD-배포.md) |

## 코드베이스 정정 (develop 기준)

| 항목 | 값 |
|------|-----|
| 업무 WAR | ic, pc, ms, sv, pd, eb, ep, ss, mg + tcf-om |
| ztomcat deploy | `ztomcat/deploy-wars.sh` 13 WAR |
| buildZtomcatWars | 15 WAR |
| bootRun | gateway 8100, uj 8102, jwt 8110, ui 8099 |


---

← [6. 로컬 개발환경 구성](./06-로컬-개발환경-구성.md) · [8. Gradle 멀티 모듈 구조](./08-Gradle-멀티모듈.md) →