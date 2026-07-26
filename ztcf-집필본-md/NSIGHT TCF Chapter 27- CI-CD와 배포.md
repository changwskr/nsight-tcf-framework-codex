<!-- source: ztcf-집필본/NSIGHT TCF Chapter 27- CI-CD와 배포.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제27장. CI/CD와 배포

## 도입 전 안내말

제25장에서는 단위·통합·TCF 거래 테스트를 통해 변경 기능을 검증하는 방법을 살펴보았다.

제26장에서는 코드 리뷰와 품질 Gate를 통해 계층·트랜잭션·보안·운영 위험이 있는 변경을 Merge 전에 차단하는 방법을 정리했다.

이번 장에서는 검증을 통과한 소스를 실제 실행환경에 안전하게 반영하는 **CI/CD와 배포**를 다룬다.

초보 개발자는 배포를 다음과 같이 생각하기 쉽다.

WAR 파일을 만든다.
→ Tomcat webapps에 복사한다.
→ Tomcat을 재기동한다.
→ 화면이 열리면 끝난다.

그러나 엔터프라이즈 시스템의 배포는 파일 복사보다 훨씬 넓은 작업이다.

어느 Commit을 배포했는가?

누가 변경을 승인했는가?

어떤 JDK·Gradle·의존성으로 빌드했는가?

단위·통합·보안 검사가 모두 통과했는가?

생성된 WAR가 빌드 후 변경되지 않았는가?

동일한 WAR가 개발·검증·운영으로 승격됐는가?

운영 Secret이 WAR 안에 포함되지 않았는가?

DB Schema는 애플리케이션보다 먼저 반영해야 하는가?

기존 WAR와 신규 WAR가 동시에 실행돼도 호환되는가?

배포 중 사용자 요청은 어느 서버가 처리하는가?

새 WAR가 기동됐다는 사실과 실제 업무가 가능한 상태는 같은가?

배포 후 오류율과 Timeout이 증가하지 않았는가?

문제가 발생하면 어떤 Artifact로 되돌릴 것인가?

DB에 이미 반영된 데이터는 어떻게 복구할 것인가?

배포·승인·실행·검증·Rollback 이력이 남는가?

CI/CD는 다음 두 개념으로 나뉜다.

CI
Continuous Integration
지속적 통합

CD
Continuous Delivery 또는
Continuous Deployment
지속적 전달·배포

CI의 핵심은 개발자가 작성한 변경을 공통 기준으로 반복 검증하는 것이다.

Source Checkout

Compile

Unit Test

Integration Test

Architecture Test

Security Scan

WAR Packaging

Artifact 생성

CD의 핵심은 검증된 Artifact를 승인된 환경에 반복 가능한 방식으로 전달하고 배포하는 것이다.

Artifact 선택

환경설정 결합

배포 승인

트래픽 제외

기존 버전 백업

신규 버전 반영

Health Check

Smoke Test

트래픽 복귀

배포 이력 기록

CI/CD를 구축하는 목적은 “사람이 명령을 입력하지 않도록 하는 것”만이 아니다.

누가 수행해도 같은 결과

같은 Commit은 같은 Artifact

같은 Artifact를 환경별로 승격

승인되지 않은 변경 차단

배포 절차 누락 방지

장애 시 신속한 원복

모든 과정의 감사 추적

을 보장하는 것이 핵심이다.

좋지 않은 배포 구조는 다음과 같다.

개발자 PC
→ WAR 생성

메신저
→ 운영자에게 WAR 전달

운영자
→ 파일명 변경

운영 서버
→ webapps에 수동 복사

설정파일
→ 서버에서 직접 수정

문제 발생
→ 이전 WAR 위치를 찾지 못함

이 방식에서는 다음을 증명하기 어렵다.

어느 소스인지

테스트된 파일인지

전달 중 변경되지 않았는지

운영 설정이 무엇인지

누가 승인했는지

이전 버전이 무엇인지

NSIGHT TCF의 배포 목표는 다음과 같다.

Git Commit
↓
CI 품질 Gate
↓
bootWar
↓
Checksum·Metadata·SBOM
↓
Artifact Repository
↓
환경별 승인
↓
동일 Artifact 승격
↓
Rolling·Blue/Green 배포
↓
Liveness·Readiness·Deep·Smoke
↓
운영 Metric 확인
↓
정상 완료 또는 자동·수동 Rollback

WAR는 개발자 PC가 아니라 승인된 CI/CD에서 생성하고, Git이 아닌 Artifact Repository에 Commit·Build·Checksum과 연결해 보관하는 것이 배포 재현성의 기본이다.

# 문서 개요

## 목적

본 장의 목적은 NSIGHT TCF의 소스 변경을 재현 가능한 배포 Artifact로 생성하고 개발·검증·운영환경에 안전하게 반영하기 위한 CI/CD와 배포 기준을 정의하는 것이다.

세부 목적은 다음과 같다.

Git 기반 변경 추적

Branch·Tag·Release Version 관리

Gradle 멀티모듈 빌드 표준화

단위·통합·아키텍처·보안 검사 자동화

업무별 WAR 패키징

Artifact 무결성·서명·Metadata 관리

환경과 Artifact 분리

Secret 외부화

DB·설정·애플리케이션 배포순서 통제

Rolling·Blue/Green·Canary 방식 선택

Health Check와 Smoke Test 표준화

배포 승인과 직무분리

Rollback Artifact와 복구절차 확보

배포이력·감사·운영 증적 관리

운영전환과 초기 안정화 기준 정의

## 적용범위

| 구분 | 적용 대상 |
| --- | --- |
| 소스관리 | Git·Branch·Tag·Pull Request |
| 빌드 | Gradle·JDK·의존성 |
| 품질 | Test·Architecture·Security Gate |
| 패키징 | WAR·JAR·설정·DB Script |
| Artifact | Repository·Checksum·Signature·SBOM |
| 설정 | application.yml·Profile·Tomcat·Apache |
| Secret | DB Password·JWT Key·API Secret·인증서 |
| 배포 | 개발·통합·검증·운영·DR |
| WAS | 단일·다중 Tomcat·다중 WAR |
| 라우팅 | L4·Apache·Gateway |
| 데이터 | DDL·DML·Migration·Seed |
| 검증 | Health·Smoke·Regression·Metric |
| 복구 | WAR·설정·DB·Route Rollback |
| 운영 | OM 배포관리·이력·감사·Runbook |

## 대상 독자

업무 개발자

프레임워크 개발자

DevOps·CI/CD 담당자

애플리케이션 아키텍트

기술·인프라 아키텍트

DBA·데이터 아키텍트

보안 담당자

QA·테스트 담당자

운영·배포 담당자

PMO·변경관리 담당자

서비스 책임자

## 선행조건

승인된 Git 저장소

Branch·Merge 정책

Gradle Wrapper

JDK 표준 Version

업무·플랫폼 모듈 목록

WAR·Context Path 표준

환경별 구성정보

Artifact Repository

CI Runner

배포 대상 서버

Health Check Endpoint

배포 승인체계

Rollback 기준

# 핵심 관점

배포의 핵심은
서버에 새 파일을 복사하는 것이 아니다.

승인된 소스로 만든
검증된 하나의 Artifact를

어떤 환경에서도 변경하지 않고 승격하며,

문제가 생기면
이전 안정상태로 되돌릴 수 있음을
증명하는 것이다.

# 학습 목표

이 장을 마치면 다음 내용을 설명하고 적용할 수 있어야 한다.

| 번호 | 학습 목표 |
| --- | --- |
| 1 | CI와 CD의 차이를 설명한다. |
| 2 | Source·Build·Artifact·Deployment를 구분한다. |
| 3 | 운영 서버에서 직접 빌드하면 안 되는 이유를 설명한다. |
| 4 | Gradle Wrapper로 빌드환경을 통제한다. |
| 5 | 공통 모듈과 실행 WAR의 Build 차이를 설명한다. |
| 6 | bootRun, build, bootWar의 차이를 설명한다. |
| 7 | 변경 모듈과 공통 영향 모듈의 Build 범위를 결정한다. |
| 8 | Compile·Test·Quality·Package Gate를 구성한다. |
| 9 | WAR 내부의 클래스·Mapper·공통 JAR를 검증한다. |
| 10 | Artifact와 Commit·Version·Build를 연결한다. |
| 11 | SHA-256·서명·SBOM의 목적을 설명한다. |
| 12 | 동일 Artifact를 환경별로 승격한다. |
| 13 | Profile과 Secret을 WAR에서 분리한다. |
| 14 | tcf-cicd를 환경설정의 단일 원천으로 관리한다. |
| 15 | 환경설정 Drift를 탐지한다. |
| 16 | DB Migration과 WAR 배포 순서를 결정한다. |
| 17 | Rolling·Blue/Green·Canary 배포를 구분한다. |
| 18 | 다중 WAR Tomcat 배포 위험을 설명한다. |
| 19 | 배포 전 점검과 배포 후 점검을 구분한다. |
| 20 | Liveness·Readiness·Deep·Smoke를 구분한다. |
| 21 | Traffic Drain과 Warm-up의 목적을 설명한다. |
| 22 | 배포 실패 자동판정 기준을 정의한다. |
| 23 | 애플리케이션·설정·DB Rollback을 구분한다. |
| 24 | 데이터 변경을 단순 WAR Rollback으로 복구할 수 없음을 설명한다. |
| 25 | OM 배포요청·승인·실행·이력을 관리한다. |
| 26 | 운영전환과 초기 안정화 완료조건을 정의한다. |
| 27 | 배포 로그와 Metric으로 배포 결과를 증명한다. |
| 28 | 배포 Pipeline 장애와 운영 장애를 구분한다. |
| 29 | 긴급 Hotfix도 표준 Pipeline을 통과시킨다. |
| 30 | 배포·Rollback·설정 변경을 감사할 수 있다. |

# 핵심 용어

| 용어 | 의미 |
| --- | --- |
| CI | 소스 변경을 자동 통합·검증하는 과정 |
| Continuous Delivery | 언제든 배포 가능한 Artifact를 만드는 체계 |
| Continuous Deployment | 승인 조건 충족 시 자동 운영 배포 |
| Pipeline | CI/CD 작업을 순서와 조건으로 연결한 흐름 |
| Runner·Agent | Pipeline 명령을 실행하는 서버·프로세스 |
| Artifact | 빌드로 만들어진 배포 산출물 |
| Artifact Repository | 승인 Artifact를 저장하는 저장소 |
| Immutable Artifact | 생성 후 내용을 변경하지 않는 Artifact |
| Build Number | CI 실행을 식별하는 번호 |
| Release Version | 운영 배포 버전 |
| Commit ID | Artifact의 원본 소스 식별자 |
| Tag | Release Commit에 부여하는 고정 이름 |
| Checksum | 파일 변경 여부를 확인하는 Hash |
| Signature | 승인된 빌드주체가 만든 Artifact인지 확인하는 전자서명 |
| SBOM | Artifact에 포함된 소프트웨어 구성목록 |
| Provenance | Artifact가 어떤 소스와 절차로 생성됐는지에 대한 증명 |
| Profile | 환경별 설정 그룹 |
| Secret | 비밀번호·키·Token·인증서 비밀정보 |
| Configuration Drift | 승인 기준과 실제 서버 설정이 달라진 상태 |
| Rolling Deployment | 서버를 한 대씩 순차 배포 |
| Blue/Green | 기존·신규 환경을 병행 후 트래픽 전환 |
| Canary | 일부 트래픽에 신규 버전을 먼저 적용 |
| Traffic Drain | 신규 요청 유입을 중단하고 기존 요청 종료를 대기 |
| Liveness | 프로세스가 살아 있는지 확인 |
| Readiness | 요청을 받을 준비가 됐는지 확인 |
| Deep Check | DB·Cache·연계 등 핵심 의존성을 확인 |
| Smoke Test | 대표 실제 업무 거래를 수행하는 최소 검증 |
| Rollback | 이전 안정 버전으로 되돌리는 작업 |
| Roll-forward | 신규 수정버전으로 문제를 해결하는 방식 |
| Go/No-Go | 운영 전환 가능 여부에 대한 최종 판단 |
| Change Freeze | 전환 전후 변경을 제한하는 기간 |

# 전체 목표 CI/CD 구조

개발자
↓
Feature Branch
↓
Pull Request
↓
┌───────────────────────────────────────────┐
│ CI │
│ │
│ Checkout │
│ → Branch·Secret 검사 │
│ → Gradle Compile │
│ → Unit·Integration Test │
│ → Architecture·Mapper Gate │
│ → SAST·SCA·License │
│ → bootWar │
│ → WAR 내부검사 │
│ → Checksum·SBOM·Signature │
└──────────────────────┬────────────────────┘
▼
┌───────────────────────────────────────────┐
│ Artifact Repository │
│ │
│ WAR │
│ Metadata │
│ Checksum │
│ SBOM │
│ Test Report │
│ Approval Evidence │
└──────────────────────┬────────────────────┘
▼
DEV 자동 배포
↓
STG 승인 배포
↓
PRD 운영 승인
↓
┌───────────────────────────────────────────┐
│ CD │
│ │
│ Pre-check │
│ → Traffic Drain │
│ → Backup │
│ → Config·DB Migration │
│ → WAR Deploy │
│ → Restart·Reload │
│ → Liveness │
│ → Readiness │
│ → Deep Check │
│ → Smoke Test │
│ → Metric Observation │
│ → Traffic Restore │
└──────────────────────┬────────────────────┘
▼
정상 완료 또는 Rollback

# 현재 구현과 목표 구조

## 현재 기준 소스에서 확인되는 구성

현재 tcf-cicd는 환경설정과 배포 스크립트를 관리하는 모듈로 구성돼 있다.

tcf-cicd
├─ manifest.yaml
├─ local
│ ├─ spring
│ └─ ztomcat
├─ dev
│ ├─ spring
│ └─ ztomcat
├─ prod
│ ├─ spring
│ ├─ ztomcat
│ └─ apache
└─ scripts
├─ cicd-deploy
├─ cicd-build
├─ sync-to-framework
├─ pull-from-framework
└─ apply-tomcat-config

주요 기능:

Profile
local·dev·prod

Action
full·sync·build·deploy·config

옵션
Dry Run
선택 모듈
Build 생략
Restart
Health Check
Artifact Directory
운영 설정 적용

tcf-cicd는 Build·Deploy·Profile 기준을 담당하는 개발·배포 모듈로 분류된다.

## 현재 설정 단일 원천

manifest.yaml은 다음 정보를 관리한다.

환경 Profile

환경별 설정파일명

플랫폼 모듈

업무 모듈

Tomcat 설정

Apache 설정

운영 Runtime Mount 경로

현재 확인되는 설정 관리 대상은 다음과 같다.

플랫폼
tcf-web
tcf-cache
tcf-core
tcf-om
tcf-batch
tcf-ui

업무
ic-service
pc-service
ms-service
sv-service
pd-service
eb-service
ep-service
ss-service
mg-service

Gateway·JWT·UJ 등 다른 실행 모듈도 전체 저장소에 존재하므로 공식 배포 Manifest에서 포함 여부와 배포 위치를 명시적으로 확정해야 한다.

## 현재 실행 스크립트

현재 cicd-deploy는 다음 흐름을 제공한다.

sync
→ tcf-cicd 설정을 Framework에 반영

build
→ 전체 또는 선택 모듈 bootWar

deploy
→ local·dev Tomcat webapps 복사

config
→ prod Tomcat 외부 설정 반영

full
→ sync + build + deploy

운영 Profile에서는 로컬 webapps 직접 배포를 생략하고 Artifact 생성 또는 원격 전송을 별도로 수행하도록 구성돼 있다.

이는 운영 서버 직접 배포를 현재 스크립트에 결합하지 않은 안전한 출발점이지만, 다음 운영 자동화는 추가 구현이 필요하다.

Artifact Repository Upload

원격 서버 배포 Agent

트래픽 Drain·복귀

Rolling 순서 제어

Checksum 검증

Artifact 서명검증

운영 승인 연계

실패 자동 Rollback

## 현재 OM 배포관리

tcf-om에는 다음 업무가 확인된다.

배포 요청

배포 승인

배포 실행

배포 이력 조회

배포 로그 조회

Health Check

Rollback 요청

관리자 감사기록

대표 상태:

REQUESTED

APPROVED

FAIL

배포 실행 상태

좋은 점:

요청·승인·실행을 분리한다.

환경코드를 검증한다.

요청사유·승인사유·실행사유를 받는다.

사용자와 시각을 기록한다.

Rollback 요청을 별도 이력으로 만든다.

보완할 점:

요청자와 승인자 직무분리 강제

동일 Artifact ID·Checksum 사용

PRD 다중 승인

실제 CI Pipeline API 호출

로컬 ProcessBuilder 기반 Gradle 실행 제거

서버 파일 직접 복사 권한 제거

Artifact Repository 기반 배포

배포서버 Service Account 최소권한

승인 Timeout·취소·재승인

자동 Health·Metric 판정

현재 OM의 로컬 Gradle 실행과 WAR 파일 복사는 개발·시범환경에서는 유용하지만, 운영에서는 CI/CD Orchestrator가 실행하고 OM은 요청·승인·상태·감사를 관리하는 구조로 분리하는 것이 안전하다.

## 현재 배포 대상 수의 혼재

현재 자료에는 다음 수가 함께 등장한다.

설정 Manifest
→ 플랫폼 6 + 업무 9

배포 안내
→ 12·13개 Context

통합 스크립트 설명
→ 19개 Context

프로젝트 목표
→ 업무코드 확장 구조

이 차이는 다음이 혼재돼 있기 때문이다.

현재 실제 Workspace 모듈

선택 배포 대상

bootRun 전용 모듈

향후 추가 업무코드

Tomcat 외부 분리 모듈

레거시 모듈

따라서 배포대상은 스크립트 설명문이 아니라 하나의 공식 Deployment Manifest에서 확정해야 한다.

moduleName

businessCode

artifactName

deployWarName

contextPath

targetGroup

deployOrder

healthUrl

smokeServiceId

rollbackArtifact

## 현재 구현 상태 판단

| 항목 | 상태 | 판단 |
| --- | --- | --- |
| Profile별 설정 디렉터리 | 구현 확인 | 단일 원천 유지 |
| 설정 Sync | 구현 확인 | Drift 검증 보완 |
| 선택 모듈 Build | 구현 확인 | 영향도 자동산정 보완 |
| bootWar | 구현 확인 | 공식 Artifact화 필요 |
| local·dev 배포 | 구현 확인 | 통합검증 용도 |
| Dry Run | 구현 확인 | 운영변경 사전검증에 유용 |
| Restart 옵션 | 구현 확인 | Rolling 제어 보완 |
| 단순 Health Check | 구현 확인 | 4단계 Check로 확장 |
| Prod 설정 Mount | 구현 확인 | 승인·Version 관리 보완 |
| OM 배포 요청·승인 | 구현 확인 | 직무분리 강화 |
| OM Rollback 요청 | 구현 확인 | 실제 Artifact 선택 연계 필요 |
| Artifact Repository | 설계 기준 | 실제 연계 확인 필요 |
| SHA-256 | 설계 기준 | Pipeline 적용 필요 |
| Artifact 서명 | 보완 필요 | 공급망 보안 |
| SBOM | 보완 필요 | Release Gate |
| Rolling Deploy | 설계 기준 | 실제 자동화 필요 |
| Blue/Green | 권장 대안 | 대규모 변경 적용 |
| DB Migration | 보완 필요 | Pipeline 통합 |
| 자동 Rollback | 보완 필요 | Metric 기반 판정 |
| 운영 CI 서버 정의 | 프로젝트 확인 필요 | GitLab·Jenkins 등 확정 |
| Root Gradle Task | 문서 확인 | 현재 Branch 파일 재검증 필요 |

# 설계 원칙

## 원칙 1. Build Once, Deploy Many

한 번 Build

동일 WAR
→ DEV
→ STG
→ PRD

환경마다 다시 Build하지 않는다.

금지:

DEV용 WAR Build

STG용 WAR Build

PRD용 WAR Build

이 경우 테스트한 파일과 운영파일이 다를 수 있다.

## 원칙 2. Artifact는 불변이다

Artifact Repository에 등록한 WAR를 다음처럼 수정하지 않는다.

WAR 압축 해제

application-prod.yml 수정

다시 압축

같은 Version으로 배포

Checksum이 달라져야 하며 신규 Artifact로 취급해야 한다.

## 원칙 3. 환경차이는 외부 설정이다

동일 WAR
+
DEV Config

동일 WAR
+
STG Config

동일 WAR
+
PRD Config

## 원칙 4. 운영 서버에서는 Build하지 않는다

운영 서버의 역할:

승인 Artifact 수신

Checksum 검증

설정 결합

기동

Health 검증

운영 서버 역할이 아닌 것:

Git Pull

Gradle Build

소스 수정

Dependency 다운로드

Mapper XML 수동 교체

## 원칙 5. 배포와 DB 변경은 함께 계획한다

DDL만 먼저 반영

WAR만 먼저 반영

중 어느 것이 안전한지 호환성을 기준으로 결정한다.

## 원칙 6. 배포 완료는 업무검증까지다

파일 복사 성공
≠ 배포 완료

Tomcat Process 실행
≠ 배포 완료

Health UP
≠ 업무 정상

대표 ServiceId Smoke Test와 운영지표 확인까지 완료해야 한다.

# 27.1 빌드·검사·패키징 파이프라인

## 27.1.1 빌드 입력

공식 Build 입력:

Git Repository

Commit ID

Release Tag

Gradle Wrapper

JDK Version

Dependency Repository

Build Script

승인된 환경변수

CI Runner Image

Build 시점에 외부에서 임의 파일을 복사하지 않는다.

## 27.1.2 Branch와 Pipeline

예시:

| Branch·Tag | 목적 | Pipeline |
| --- | --- | --- |
| feature/\* | 기능 개발 | 빠른 Unit |
| develop | 개발 통합 | 전체 CI·DEV |
| release/\* | 검증 | STG·회귀 |
| main | 운영 기준 | Release Artifact |
| v1.4.2 | 고정 Release | PRD 승인 |
| hotfix/\* | 긴급 수정 | 긴급 Full Gate |

실제 Branch 전략은 프로젝트 표준으로 확정한다.

## 27.1.3 Checkout 검증

Submodule

Git LFS

Generated File

Line Ending

파일 인코딩

대소문자

Commit Signature

Tag Signature

을 확인한다.

CI는 Clean Workspace에서 시작해야 한다.

이전 Build 파일

기존 WAR

개발자 로컬 설정

Gradle 임시파일

이 산출물에 섞이지 않아야 한다.

## 27.1.4 Gradle Wrapper

권장 명령:

./gradlew clean build

단일 모듈:

./gradlew :sv-service:build

WAR:

./gradlew :sv-service:bootWar

운영 CI에서 시스템에 설치된 임의 Gradle Version을 직접 사용하지 않는다.

gradle build

보다 Repository에 포함된 Wrapper를 우선한다.

## 27.1.5 JDK Version

Build와 실행 JDK의 호환성을 확인한다.

Compile JDK

Target Compatibility

Tomcat Runtime JDK

CI Runner JDK

개발자 JDK

예:

Build
JDK 21

Tomcat
JDK 21

배포로그에 JDK Version을 기록한다.

## 27.1.6 멀티모듈 Build

NSIGHT TCF의 공통 모듈:

tcf-util

tcf-core

tcf-web

tcf-cache

tcf-eai

는 일반적으로 단독 운영 배포 대상이 아니다.

업무 WAR의 WEB-INF/lib에 포함된다.

따라서 공통 모듈 변경 시 다음 WAR가 영향을 받을 수 있다.

모든 업무 WAR

tcf-om

tcf-batch

인증·연계 모듈

공통 모듈 변경을 단일 업무 WAR 테스트만으로 승인하지 않는다.

## 27.1.7 빌드 범위 선택

| 변경 | Build 범위 |
| --- | --- |
| 업무 Rule | 해당 업무 WAR |
| 업무 Mapper | 해당 업무 WAR |
| 공개 Contract | Provider·Consumer |
| tcf-core | 전체 의존 WAR |
| tcf-web | 모든 Online WAR |
| tcf-cache | Cache 사용 WAR |
| tcf-eai | 연계 사용 WAR |
| StandardHeader | 전체 거래 모듈 |
| Root Gradle | 전체 Build |
| JDK·Plugin | 전체 Build |

안전한 기본은 전체 Build다.

규모가 커지면 Dependency Graph를 이용한 영향 Build를 적용한다.

## 27.1.8 파이프라인 단계

Validate

Compile

Unit Test

Integration Test

Architecture Test

Mapper Validation

Security Scan

Dependency Scan

Contract Test

Package

Artifact Inspection

Publish

Deploy

각 단계는 앞 단계 성공을 전제로 한다.

## 27.1.9 Validate 단계

Branch 정책

Commit 규칙

금지 파일

Secret

Private Key

운영 URL

Snapshot Dependency

Dynamic Version

License

실패 시 Compile 전에 빠르게 종료한다.

## 27.1.10 Compile 단계

./gradlew clean compileJava

검증:

Java 문법

Module Dependency

Generated Source

Annotation Processing

Resource Encoding

## 27.1.11 Test 단계

./gradlew test

필수 Report:

JUnit XML

HTML Test Report

Coverage

Flaky 여부

실패 Stack

테스트 Skip:

\-x test

를 운영 Artifact 생성에 사용하지 않는다.

## 27.1.12 Architecture·Quality 단계

Handler → Facade

Rule → DB 금지

ServiceId 중복

Handler 등록

OM Catalog

Mapper Namespace

Transaction 위치

Secret

취약 Dependency

오류응답

거래로그

를 검증한다.

## 27.1.13 보안 검사

### SAST

SQL Injection

Path Traversal

SSRF

약한 암호화

인증 우회

민감정보 로그

### SCA

Open Source CVE

License

지원종료 Library

Transitive Dependency

### Secret Scan

DB Password

API Key

Private Key

JWT

Access Token

Credential

Critical 보안결함이나 비밀정보 노출은 운영 배포 차단 기준이다.

## 27.1.14 WAR 생성

./gradlew :sv-service:bootWar

대표 위치:

sv-service/build/libs/sv.war

통합 Task 예:

buildBusinessWars

buildZtomcatWars

정확한 Task 대상과 WAR 수는 현재 Release Branch의 Root Gradle 정의로 확인한다.

## 27.1.15 WAR 내부검사

jar tf sv.war

확인:

WEB-INF/classes

업무 Class

Mapper XML

application.yml

WEB-INF/lib/tcf-core-\*.jar

WEB-INF/lib/tcf-web-\*.jar

Spring Boot Bootstrap

Manifest

금지:

테스트 Class

로그파일

Heap Dump

개인 설정

운영 Secret

Private Key

백업파일

불필요한 중복 JAR

WAR 내부의 Mapper XML과 공통 TCF JAR 포함 여부는 Runtime 오류를 막기 위한 필수 패키징 검증이다.

## 27.1.16 WAR 파일명

운영 Context와 일치시키는 방식:

sv.war
→ /sv

ic.war
→ /ic

om.war
→ /om

batch.war
→ /batch

Artifact Repository 파일명은 Version을 포함할 수 있다.

sv-service-1.4.2-a1b2c3d.war

배포 시:

sv.war

로 변환하더라도 원 Artifact ID와 Checksum을 이력에 유지한다.

## 27.1.17 Artifact Metadata

artifact:
module: sv-service
businessCode: SV
version: 1.4.2
commitId: a1b2c3d4
branch: release/1.4.2
tag: v1.4.2
buildNumber: BUILD-20260718-0142
buildTime: 2026-07-18T17:30:00+09:00
jdk: 21
gradle: 8.x
fileName: sv-service-1.4.2-a1b2c3d.war
checksum: sha256:...

## 27.1.18 Checksum

sha256sum sv-service-1.4.2-a1b2c3d.war

검증시점:

Artifact 등록 전

Artifact 다운로드 후

배포 서버 반영 전

Rollback 전

Checksum 불일치 시 배포하지 않는다.

## 27.1.19 Artifact 서명

Checksum은 파일 변경 여부는 확인하지만 누가 만들었는지는 증명하지 않는다.

서명은 다음을 보완한다.

승인된 CI가 생성했는가?

Artifact가 위조되지 않았는가?

전달 중 바뀌지 않았는가?

운영에서는 전자서명·사내 Code Signing 정책을 검토한다.

## 27.1.20 SBOM

SBOM 항목:

Library 이름

Version

License

Package URL

Hash

취약점 상태

운영 배포 후 신규 CVE가 발견됐을 때 어떤 WAR가 영향받는지 역추적할 수 있다.

## 27.1.21 Reproducible Build

같은 Commit으로 Build했을 때 동일한 Artifact Hash가 나오도록 하는 것이 이상적이다.

방해요소:

Build Time을 Binary에 직접 삽입

파일순서 비결정성

동적 Dependency

Snapshot

환경별 Resource 포함

Runner OS 차이

Random ID

완전 동일 Hash가 어렵더라도 Build 입력과 Provenance를 기록해야 한다.

## 27.1.22 Artifact Publish

WAR

Checksum

Signature

SBOM

Manifest

Test Report

Coverage

Security Report

Migration Script

Release Note

를 Release 단위로 묶는다.

## 27.1.23 Artifact 보관

권장 보관:

현재 운영버전

직전 정상버전

그 이전 안정버전

감사 요구기간 Release

최소 개수만 정하는 것보다 서비스 중요도와 감사정책에 따른 기간을 정의한다.

## 27.1.24 Build 실패 흐름

Compile 실패
↓
Artifact 생성 금지
↓
PR·Commit 상태 FAIL
↓
배포 Stage 미실행
↓
개발자 수정
↓
처음부터 재실행

실패한 Build의 일부 WAR만 수동으로 배포하지 않는다.

# 27.2 환경별 설정과 비밀정보

## 27.2.1 Artifact와 설정 분리

Artifact
→ 프로그램 로직

Configuration
→ 환경별 실행값

Secret
→ 보호가 필요한 인증정보

예:

프로그램 로직
Connection Pool 기능

환경설정
maximumPoolSize=120

Secret
DB Password

## 27.2.2 Profile

현재 구조:

application.yml
→ 공통 기본값

application-local.yml
→ 개발자 PC

application-dev.yml
→ 개발·통합

application-prod.yml
→ 운영

검증환경이 별도라면 stg Profile을 공식 추가한다.

local

dev

stg

prod

dr

## 27.2.3 설정 단일 원천

권장:

tcf-cicd
\= 환경설정 Source of Truth

설정 흐름:

tcf-cicd/prod
↓
승인
↓
배포 Pipeline
↓
Tomcat 외부 Config 경로
↓
Spring Boot Runtime

각 업무 모듈의 application-prod.yml을 개발자가 개별 수정하고 tcf-cicd와 수동 동기화하는 구조는 Drift 위험이 있다.

## 27.2.4 설정 Sync

현재 제공되는 개념:

pull-from-framework
→ 기존 Framework 설정을 tcf-cicd로 수집

sync-to-framework
→ tcf-cicd 설정을 Framework에 반영

apply-tomcat-config
→ 운영 Tomcat Runtime에 설정 반영

운영 Pipeline에서는 양방향 Sync보다 단방향 배포를 권장한다.

Git 승인 설정
→ 서버

서버에서 수정
→ Git 역반영

방식은 긴급 상황 외에는 금지한다.

## 27.2.5 외부 설정 주입

예:

\-Dspring.profiles.active=prod
\-Dspring.config.additional-location=file:/app/nsight/config/sv/

장점:

WAR 재Build 없이 환경값 변경

동일 Artifact 승격

Secret 분리

환경별 승인

설정 Rollback

## 27.2.6 설정 우선순위

Spring 설정은 여러 원천에서 합쳐질 수 있다.

application.yml

Profile yml

외부 yml

환경변수

JVM System Property

Command Line

같은 Key가 여러 곳에 있으면 최종값을 운영자가 알기 어렵다.

따라서 다음을 문서화한다.

설정 Key

기본값

환경값 원천

최종 우선순위

변경 Owner

재기동 여부

## 27.2.7 설정 Schema 검증

기동 시 다음을 검증한다.

필수값 누락

허용범위

URL 형식

Timeout 양수

Pool 상한

환경코드

업무코드

Secret Reference

Profile

잘못된 설정으로 기동한 뒤 런타임에서 실패하는 것보다 기동을 차단하는 것이 안전하다.

## 27.2.8 Profile 오배포 방지

금지:

운영에서 local Profile

운영에서 H2

운영에서 Debug Endpoint

운영에서 인증 우회

운영에서 전체 SQL 로그

기동 Gate:

env=PRD

activeProfile=prod

datasource=승인 Oracle

security.strict=true

debug=false

불일치 시 기동 실패를 검토한다.

## 27.2.9 Secret

Secret 예:

DB Password

JWT Private Key

OAuth Client Secret

API Key

HMAC Secret

Key Store Password

Session 암호키

SSH Key

저장 금지:

Git

WAR

일반 DB Table

배포로그

메신저

Wiki 평문

Shell History

## 27.2.10 Secret Reference

설정에는 원문이 아니라 참조값을 둔다.

spring:
datasource:
username: ${NSIGHT\_DB\_USER}
password: ${NSIGHT\_DB\_PASSWORD}

security:
jwt:
private-key-ref: vault://nsight/jwt/signing-key

## 27.2.11 Secret 공급

대안:

Vault Agent

KMS·HSM

CI Protected Variable

OS Secret File

Container Secret

승인된 암호화 저장소

환경변수도 Process 목록·Dump·오류로그에 노출되지 않도록 주의한다.

## 27.2.12 Secret Rotation

신규 Secret 생성

소비 시스템 병행 허용

신규 Secret 배포

연결 검증

구 Secret 사용량 0 확인

구 Secret 폐기

감사 기록

Secret 교체와 WAR 배포를 반드시 동시에 묶을 필요는 없다.

## 27.2.13 설정 변경과 재기동

| 설정 | 일반적인 반영 |
| --- | --- |
| JVM Heap | Process 재기동 |
| Tomcat Thread | Connector 재기동 |
| DB Pool | Application 재기동 또는 동적 정책 |
| Timeout | Cache 갱신·재기동 정책 |
| 로그 Level | 동적 변경 가능 |
| Gateway Route | Route Refresh 가능 |
| Secret | Client 재생성·재기동 |
| Spring Bean 조건 | 재기동 |
| Apache Route | Reload |

실제 구현별 반영방식을 확인한다.

## 27.2.14 설정 Drift

Drift 예:

Git prod 설정
maximumPoolSize=120

운영 서버
maximumPoolSize=200

탐지:

설정 Hash

서버 파일 비교

기동 시 Config Version 로그

OM 수집

주기적 Drift Scan

## 27.2.15 Config Version

configVersion
CFG-PRD-20260718-003

artifactVersion
1.4.2

deployId
DEP-20260718-017

를 함께 기록한다.

같은 Artifact라도 설정이 다르면 동작이 달라질 수 있다.

## 27.2.16 환경별 설정 변경 절차

설정 변경 요청

영향분석

보안·용량 검토

Pull Request

자동 Schema 검사

승인

환경 배포

재기동·Refresh

Health·Metric 확인

이력 기록

운영 서버에서 직접 수정하고 나중에 Git에 반영하는 방식을 원칙적으로 금지한다.

# 27.3 배포 전 후 확인

## 27.3.1 배포는 하나의 Change다

배포 단위:

Application Artifact

Configuration

DB Migration

Route

OM Catalog

권한

Cache

Batch Schedule

운영 Runbook

이 서로 연결될 수 있다.

WAR만 배포하고 OM ServiceId를 등록하지 않으면 STF에서 차단될 수 있다.

## 27.3.2 배포 전 확인

### 변경 범위

요구사항

ServiceId

업무 WAR

공통 모듈

DB

설정

연계

Route

권한

Cache

Batch

### 승인

코드 리뷰

QA

보안

DBA

AA

운영

업무 Owner

### Artifact

Version

Commit

Checksum

Signature

SBOM

Test Result

## 27.3.3 배포 계획서

| 항목 | 내용 |
| --- | --- |
| Change ID | 변경 식별자 |
| 배포 ID | 배포 실행 식별자 |
| 대상 환경 | DEV·STG·PRD |
| 대상 서비스 | SV·IC 등 |
| Artifact ID | 저장소 Artifact |
| Commit·Tag | 원본 소스 |
| 설정 Version | 환경설정 |
| DB Script | Forward·Rollback |
| 배포 순서 | 서버·WAR 순서 |
| 예상시간 | 단계별 |
| 서비스 영향 | 무중단·일시중지 |
| 검증 | Health·Smoke |
| Rollback 조건 | 오류율·기동 실패 |
| 담당자 | 실행·승인·검증 |
| 비상연락 | 운영·개발·DBA |

## 27.3.4 의존 배포순서

예:

DB 확장 DDL

OM ServiceId·오류코드

Provider Contract V2

Consumer WAR

Gateway Route

화면

구버전과 신버전이 일정 기간 공존할 수 있어야 한다.

## 27.3.5 DB Migration

DB 변경 유형:

Table 추가

Column 추가

Index 추가

Constraint 추가

데이터 이관

Column 삭제

타입 변경

위험도는 서로 다르다.

## 27.3.6 Expand–Migrate–Contract

안전한 변경:

1\. 신규 Column 추가
2\. 구 WAR와 호환 유지
3\. 신 WAR 배포
4\. 신규 Column 데이터 이관
5\. 모든 소비자 전환
6\. 구 Column 사용량 0 확인
7\. 구 Column 제거

금지:

Column 삭제
→ 신 WAR 배포

두 작업 사이에 구 WAR가 실행되면 장애가 발생할 수 있다.

## 27.3.7 DDL 실행 위험

확인:

Table Lock

수행시간

Redo·Undo

Index 생성부하

대량 데이터 갱신

Replication

Backup

Rollback 가능성

대형 DDL은 업무시간 외 실행하거나 Online 방식 지원 여부를 검토한다.

## 27.3.8 배포 방식 비교

| 방식 | 구조 | 장점 | 위험 |
| --- | --- | --- | --- |
| Rolling | 서버 한 대씩 | 자원효율 | 구·신 공존 |
| Blue/Green | 두 그룹 | 빠른 전환·원복 | 자원 2배 |
| Canary | 일부 트래픽 | 위험 조기탐지 | 라우팅 복잡 |
| Full Stop | 전체 중지 | 단순 | 서비스 중단 |
| Hot Deploy | WAR 교체 | 빠름 | ClassLoader Leak |
| In-place Restart | 동일 서버 교체 | 구조 단순 | 서버별 중단 |

## 27.3.9 Rolling Deploy

WAS 1
Traffic Drain
→ Deploy
→ Health
→ Smoke
→ Traffic 복귀

WAS 2
같은 절차 반복

조건:

서버 2대 이상

구·신 Version 호환

Session·JWT 연속성

DB Schema 호환

Route 제어

상태 없는 처리 또는 세션 공유

## 27.3.10 Blue/Green

Blue
현재 운영

Green
신규 Version

절차:

Green 전체 배포

Green 내부 검증

일부 검증 트래픽

L4·Apache 전환

Metric 관찰

Blue 유지

안정 후 Blue 종료

빠른 Rollback:

Route를 Blue로 복귀

단, DB가 비호환 변경됐다면 Route만 되돌려도 복구되지 않는다.

## 27.3.11 Canary

신규 Version
5% 트래픽
↓
오류율·p95 확인
↓
25%
↓
50%
↓
100%

사용자·지점·Header 기준으로 고정 라우팅할 수 있다.

금융 업무에서는 동일 사용자의 요청이 서로 다른 Version으로 이동하지 않도록 Sticky 기준을 검토한다.

## 27.3.12 다중 WAR Tomcat 배포

하나의 Tomcat에 여러 WAR가 배포돼도 다음 자원은 공유된다.

JVM Process

Heap

Metaspace

GC

Connector Thread

프로세스 장애영역

WAR는 배포단위로 분리되지만 실행·자원·프로세스 장애영역은 공유된다.

따라서 특정 WAR 배포 시 다음을 확인한다.

Tomcat 전체 재기동이 필요한가?

다른 WAR 요청에 영향이 있는가?

공통 Library Version이 바뀌는가?

Metaspace가 회수되는가?

공유 Connector Thread가 안정적인가?

WAR별 DB Pool 합계가 변하는가?

## 27.3.13 Hot Deploy 주의

Tomcat autoDeploy로 WAR를 반복 교체하면 다음 문제가 발생할 수 있다.

ClassLoader Leak

Thread 미종료

JDBC Driver 등록 잔존

Scheduler 잔존

MBean 잔존

Metaspace 증가

File Lock

운영에서는 반복 Hot Deploy보다 검증된 Context Reload·Process Restart·Rolling을 권장한다.

## 27.3.14 Traffic Drain

L4·Apache에서 대상 제거

신규 요청 중지

기존 요청 종료 대기

Busy Thread 확인

Batch·Scheduler 정지

배포 시작

즉시 Process를 종료하면 처리 중 거래가 중단될 수 있다.

## 27.3.15 Graceful Shutdown

확인:

신규 요청 차단

처리 중 요청 대기시간

Transaction 종료

Executor Shutdown

Scheduler 중지

Connection Pool Close

로그 Flush

임시파일 정리

Shutdown Timeout을 초과하면 강제 종료 여부를 결정한다.

## 27.3.16 현재 WAR 백업

백업 대상:

현재 Artifact ID

WAR

Checksum

설정 Version

Tomcat Config

Apache Config

DB Migration 이전상태

서버의 현재 WAR를 복사해 백업할 수도 있지만 공식 Rollback은 Artifact Repository의 이전 승인본을 사용하는 것이 좋다.

## 27.3.17 배포 실행

Artifact 다운로드

Checksum·서명 검증

배포경로 확인

기존 Context 제거·보존 정책

WAR 원자적 교체

소유자·권한 설정

Tomcat Reload·Restart

로그 관찰

부분 복사 상태의 WAR가 Tomcat에 감지되지 않도록 임시 파일에 복사 후 원자적으로 Rename하는 방식을 검토한다.

## 27.3.18 Warm-up

Spring Context가 시작됐다고 첫 거래가 빠른 것은 아니다.

Warm-up 대상:

Class Loading

JIT

DB Pool Connection

Mapper 초기화

Cache

JWKS

외부 DNS

Template

정책 Cache

Readiness는 Warm-up이 끝난 뒤 UP으로 전환한다.

## 27.3.19 Health Check 4단계

### Liveness

JVM과 Spring Process가 살아 있는가?

### Readiness

신규 업무 요청을 받을 준비가 됐는가?

### Deep Check

필수 DB

Session Store

Cache

필수 연계

설정

### Smoke Test

실제 대표 ServiceId가 성공하는가?

/actuator/health 한 건만으로 배포를 완료하지 않는다.

## 27.3.20 Deep Check 주의

모든 외부 시스템을 Health Check에 넣으면 외부 장애 때문에 전체 서비스가 Readiness DOWN이 될 수 있다.

구분:

필수 의존

선택 의존

선택 외부 시스템은 Health Detail이나 별도 Metric으로 관리할 수 있다.

## 27.3.21 Smoke Test

대표 거래 선정기준:

DB 조회

TCF 진입

STF

Dispatcher

Handler

Mapper

ETF

거래로그

예:

SV.Customer.selectSummary

OM.Service.selectCatalog

JWT JWKS 조회

변경 거래를 운영 Smoke로 실행한다면 전용 점검 데이터와 정리절차가 필요하다.

## 27.3.22 배포 후 Metric

요청량

오류율

업무 오류율

시스템 오류율

Timeout

p50·p95·p99

Busy Thread

Heap·GC

DB Pool Active·Pending

Connection Timeout

Slow SQL

외부 연계 오류

Circuit 상태

배포 전 Baseline과 비교한다.

## 27.3.23 관찰시간

Health Check 직후 정상이어도 일정 시간이 지난 뒤 문제가 나타날 수 있다.

Memory Leak

Connection Leak

Cache Miss 폭증

Batch 충돌

오류율 증가

성능저하

서비스 중요도별 관찰시간을 정의한다.

## 27.3.24 배포 후 확인표

| 구분 | 확인 |
| --- | --- |
| Artifact | Version·Checksum |
| Process | PID·JDK·Profile |
| Context | 정상 기동 |
| Health | Live·Ready·Deep |
| Smoke | 대표 ServiceId |
| Log | 배포버전·오류 없음 |
| Metric | 오류율·p95 |
| DB | Migration·Connection |
| Cache | 상태·Evict |
| Batch | 중복 실행 없음 |
| Route | 대상 서버 정상 |
| Audit | 배포이력 저장 |

## 27.3.25 배포 완료 판정

Artifact 반영

\+ 4단계 Health

\+ Smoke Test

\+ Metric 정상

\+ 거래로그 확인

\+ 트래픽 복귀

\+ 배포이력 기록

이 모두 완료돼야 SUCCESS로 판정한다.

# 27.4 롤백과 운영 전환

## 27.4.1 Rollback이 필요한 이유

배포 전 테스트로 모든 운영조건을 재현할 수는 없다.

운영 데이터 분포

실제 사용자 동시성

실제 외부 시스템 지연

운영 네트워크

실제 Cache

실제 Batch Schedule

때문에 운영에서만 문제가 나타날 수 있다.

## 27.4.2 Rollback 유형

| 유형 | 대상 |
| --- | --- |
| Application Rollback | WAR |
| Configuration Rollback | yml·Tomcat·Apache |
| Route Rollback | L4·Gateway·Apache |
| DB Schema Rollback | DDL |
| Data Rollback | DML·업무 데이터 |
| Cache Rollback | Evict·재구축 |
| Batch Rollback | Schedule·Job |
| Secret Rollback | Key·Password |
| Full Environment | Blue·Green 전환 |

## 27.4.3 Rollback 조건

Tomcat 기동 실패

Context 기동 실패

Liveness DOWN

Readiness DOWN

Deep Check 실패

Smoke Test 실패

오류율 임계치 초과

Timeout 급증

p95 임계치 초과

DB Pool 대기 급증

Heap·GC 비정상

중요 업무 결함

데이터 정합성 위험

보안사고 징후

판단기준은 배포 전에 확정한다.

## 27.4.4 자동 Rollback과 수동 Rollback

### 자동 Rollback

적합:

기동 실패

Health 실패

Smoke 실패

즉시 명확한 Metric 악화

### 수동 판단

적합:

업무 결과 오류

부분 사용자 영향

데이터 이관 문제

외부 시스템 계약

보상 필요

데이터 문제를 Health Check만으로 자동 판단하기는 어렵다.

## 27.4.5 Application Rollback 흐름

대상 서버 트래픽 제외

신규 Context 중지

신규 WAR 제거

직전 승인 Artifact 다운로드

Checksum 검증

이전 WAR 배포

기동

Health·Smoke

Metric 확인

트래픽 복귀

Rollback 이력

## 27.4.6 설정 Rollback

이전 Config Version 복원

Secret Version 복원 가능 여부

Tomcat·Application 재기동

최종 Effective Config 확인

Health·Smoke

Secret 폐기 후에는 이전 Secret으로 단순 복귀할 수 없을 수 있다.

## 27.4.7 DB Rollback의 어려움

다음 DDL은 단순 복구가 어렵다.

Column 삭제

Table 삭제

타입 축소

데이터 대량 갱신

Constraint 추가 후 데이터 변경

따라서 DB는 Rollback보다 Roll-forward가 안전한 경우가 많다.

신규 수정 Script

호환 View

임시 Column

데이터 복구

보상 DML

## 27.4.8 WAR Rollback과 DB 호환성

신 WAR
→ 신규 Column 사용

구 WAR
→ 신규 Column을 몰라도 정상

이면 WAR Rollback 가능성이 높다.

반대로:

구 Column 삭제

신 WAR 배포

문제 발생

구 WAR Rollback
→ Column 없음
→ 기동·거래 실패

가 된다.

배포 전 구 WAR Rollback 호환성을 반드시 테스트한다.

## 27.4.9 데이터 Rollback

업무 데이터 복구:

변경 전 Snapshot

Audit History

Backup

보상거래

대사

수동 승인

단순 SQL로 이전 값을 덮어쓰면 후속 거래를 손상시킬 수 있다.

## 27.4.10 Roll-forward

다음 상황에서는 이전 Version 복구보다 수정 Version을 배포하는 것이 안전할 수 있다.

DB 비호환 변경 완료

대량 데이터 이관 완료

외부 계약 신규 Version 전환

보안 Key 교체 완료

그러나 Hotfix도 다음을 거쳐야 한다.

Commit

Review

Test

Artifact

승인

배포

운영 서버에서 직접 Class를 수정하지 않는다.

## 27.4.11 OM 배포 상태

권장 상태모델:

DRAFT

REQUESTED

APPROVED

SCHEDULED

DEPLOYING

VERIFYING

SUCCESS

PARTIAL

FAIL

ROLLBACK\_REQUESTED

ROLLING\_BACK

ROLLED\_BACK

ROLLBACK\_FAIL

CANCELLED

## 27.4.12 직무분리

| 활동 | 사용자 |
| --- | --- |
| 변경 개발 | 개발자 |
| PR 승인 | Reviewer |
| 배포 요청 | 개발·릴리즈 담당 |
| 운영 승인 | 서비스 Owner·운영 |
| 배포 실행 | CI Service Account |
| 결과 검증 | QA·운영 |
| Rollback 승인 | 운영 책임자 |
| 감사 검토 | 보안·감사 |

한 사람이 개발·승인·운영배포를 모두 수행하지 않도록 한다.

## 27.4.13 배포 이력

deployId

changeId

requestId

requestType

environment

businessCode

moduleName

artifactId

version

commitId

checksum

configVersion

dbMigrationVersion

requestUser

approveUser

executeServiceAccount

startTime

endTime

result

healthResult

smokeResult

rollbackArtifact

reason

## 27.4.14 Rollback 요청

Rollback은 신규 배포요청과 별도 이력으로 만든다.

원 배포 ID

Rollback 대상 Artifact

사유

영향

승인자

실행자

결과

현재 OM도 원본 배포요청을 참조해 별도의 Rollback 요청 ID를 생성하는 구조를 가진다.

## 27.4.15 운영전환

운영전환은 첫 운영 배포 한 번만 의미하지 않는다.

운영 인프라 준비

계정·권한

배포 Pipeline

모니터링

로그

Runbook

백업·DR

업무 지원

비상연락

초기 안정화

를 포함한다.

## 27.4.16 Go/No-Go

### Go

Critical 결함 0

필수 테스트 PASS

성능 목표 충족

보안 승인

DB Migration 검증

Rollback Artifact 확보

운영 Runbook 완료

담당자 대기

모니터링 준비

### No-Go

Rollback 불가

핵심 Smoke 실패

Critical 보안결함

미해결 데이터 정합성

환경설정 불명확

운영 Owner 부재

DR·Backup 검증 실패

중요 외부연계 미검증

## 27.4.17 Change Freeze

전환 직전:

기능 변경 제한

DB 변경 제한

설정 변경 제한

긴급 변경 승인 강화

를 적용해 배포 Baseline을 안정화한다.

## 27.4.18 Cutover 계획

T-7일
최종 Release Candidate

T-3일
운영 설정·Artifact 검증

T-1일
백업·담당자·연락망 확인

T-0
Traffic 통제·DB·WAR 전환

T+1시간
Smoke·Metric

T+1일
대사·업무 확인

T+1주
안정화 종료 검토

프로젝트 일정에 맞게 조정한다.

## 27.4.19 초기 안정화

관리:

오류율

Timeout

p95

Slow SQL

DB Pool

Heap·GC

사용자 문의

데이터 대사

Batch 적체

외부연계 오류

평상시보다 강화된 관측과 대응인력을 운영한다.

## 27.4.20 안정화 종료조건

Critical 장애 0

오류율 정상범위

성능 목표 충족

미확인 거래 0

데이터 대사 완료

Batch 정상

운영 문의 안정

임시 설정 제거

잔여 이슈 Owner 지정

표준·Runbook 갱신

# 배포 대안 비교 및 의사결정

| 기준 | Rolling | Blue/Green | Canary |
| --- | --- | --- | --- |
| 추가 자원 | 낮음 | 높음 | 중간 |
| 구·신 공존 | O | O | O |
| 전환속도 | 순차 | 빠름 | 단계적 |
| 원복 | 서버별 | Route 복귀 | Traffic 축소 |
| 위험 조기탐지 | 중간 | 전환 후 | 높음 |
| DB 호환 필요 | 높음 | 높음 | 매우 높음 |
| 운영 복잡도 | 중간 | 중간 | 높음 |
| 권장 | 일반 배포 | 대규모 변경 | 고위험 변경 |

NSIGHT 기본 권장:

일반 업무 WAR
→ Rolling

대규모 Framework·인증 변경
→ Blue/Green 검토

고위험 신규 기능
→ Feature Flag·Canary 검토

# 표준 Artifact 구조

release/
├─ artifacts
│ ├─ sv-service-1.4.2-a1b2c3d.war
│ ├─ om-service-1.4.2-a1b2c3d.war
│ └─ checksum.sha256
├─ metadata
│ ├─ manifest.yaml
│ ├─ sbom.json
│ └─ provenance.json
├─ database
│ ├─ forward
│ ├─ rollback
│ └─ validation
├─ config
│ └─ config-version.txt
├─ tests
│ ├─ junit
│ ├─ security
│ └─ performance
└─ operations
├─ release-note.md
├─ deploy-runbook.md
└─ rollback-runbook.md

# 정상 배포 흐름

1\. Release Tag가 생성된다.
2\. CI가 Clean Checkout을 수행한다.
3\. Branch·Secret·Dependency 검사를 수행한다.
4\. Gradle Compile과 전체 테스트를 실행한다.
5\. Architecture·Mapper·OM Gate를 수행한다.
6\. 업무별 bootWar를 생성한다.
7\. WAR 내부파일과 Secret을 검사한다.
8\. SHA-256·Signature·SBOM을 생성한다.
9\. Artifact Repository에 등록한다.
10\. DEV에서 동일 Artifact를 검증한다.
11\. STG에서 통합·성능·보안 검증을 수행한다.
12\. 운영 배포요청과 승인을 등록한다.
13\. 대상 WAS를 트래픽에서 제외한다.
14\. 기존 요청 종료를 기다린다.
15\. 현재 Artifact·설정·DB 상태를 백업한다.
16\. 승인된 DB Migration을 실행한다.
17\. 승인된 설정과 WAR를 반영한다.
18\. Tomcat을 기동한다.
19\. Liveness·Readiness·Deep Check를 수행한다.
20\. 대표 ServiceId Smoke Test를 실행한다.
21\. 오류율·p95·Thread·Pool을 관찰한다.
22\. 정상 서버를 트래픽에 복귀시킨다.
23\. 다음 WAS에서 같은 절차를 반복한다.
24\. 전체 배포이력과 증적을 저장한다.

# 배포 실패 흐름

신규 WAR 반영
↓
Readiness DOWN
↓
트래픽 복귀 금지
↓
배포 Stage FAIL
↓
신규 WAR 제거
↓
직전 Artifact 복원
↓
Health·Smoke
↓
트래픽 복귀
↓
Rollback 이력
↓
원인 분석

# 배포 후 성능 악화 흐름

Health UP

Smoke PASS
↓
운영 p95 급증
↓
DB Pool Pending 증가
↓
신규 Version 영향 확인
↓
Canary·신규 서버 트래픽 제거
↓
Rollback 또는 설정 복구
↓
성능 원인 분석

# DB Migration 실패 흐름

DDL 실행
↓
중간 실패
↓
WAR 배포 중단
↓
적용된 객체 확인
↓
Rollback Script 또는 Roll-forward
↓
DB 상태 검증
↓
Go/No-Go 재판정

DDL 실패 후 상태를 확인하지 않고 동일 Script를 다시 실행하지 않는다.

# 정상 예시

Change ID
CHG-20260718-014

Artifact
sv-service-1.4.2-a1b2c3d.war

Commit
a1b2c3d

Checksum
정상

Config
CFG-PRD-20260718-003

DB
신규 선택 Column 추가
구 WAR 호환

배포
WAS 1 → WAS 2 Rolling

검증
Liveness UP
Readiness UP
Deep UP
SV.Customer.selectSummary 성공

Metric
오류율 정상
p95 1.2초
Pool Pending 0

결과
SUCCESS

# 금지 예시

개발자 PC에서 만든 WAR를 운영에 반영한다.

운영 서버에서 Git Pull과 Gradle Build를 수행한다.

테스트를 Skip하고 WAR를 생성한다.

같은 Version의 WAR 내용을 수정한다.

개발·검증·운영에서 각각 다시 Build한다.

WAR에 운영 DB Password를 포함한다.

Private Key를 application-prod.yml에 저장한다.

환경별 설정을 WAR 내부에서 직접 수정한다.

운영 서버에서 설정을 변경하고 Git에는 남기지 않는다.

Snapshot Dependency로 운영 Artifact를 만든다.

Checksum 없이 파일을 전송한다.

Artifact Repository 없이 공유폴더만 사용한다.

Class 파일만 교체한다.

Mapper XML만 부분 교체한다.

Tomcat 공용 lib에 TCF JAR를 수동 복사한다.

배포 중 처리 중 요청을 강제 종료한다.

구·신 WAR가 비호환인데 Rolling 배포한다.

Column을 삭제하고 즉시 신 WAR를 배포한다.

DB Migration 실패 후 상태확인 없이 재실행한다.

Health UP만 보고 배포를 완료한다.

대표 ServiceId Smoke Test를 생략한다.

한 WAR 배포 후 다른 WAR 영향을 확인하지 않는다.

Hot Deploy를 무제한 반복한다.

Rollback Artifact를 준비하지 않는다.

Rollback 조건을 배포 후 결정한다.

DB 데이터 변경을 WAR Rollback으로 복구할 수 있다고 가정한다.

운영 장애 시 서버에서 소스를 직접 수정한다.

Hotfix를 리뷰·테스트 없이 배포한다.

배포요청자와 승인자가 같은 사람이다.

Rollback 사유와 결과를 기록하지 않는다.

배포 후 오류율·p95를 관찰하지 않는다.

# 연계 규칙

## Git 연계

Requirement ID

Pull Request

Commit ID

Release Tag

Artifact ID

Deploy ID

가 연결돼야 한다.

## OM 연계

ServiceId

거래통제

Timeout

오류코드

권한

Gateway Route

배포요청

배포상태

를 Release와 함께 반영한다.

## DB 연계

Migration Version

Forward Script

Rollback Script

Validation SQL

실행결과

대사

를 배포이력에 연결한다.

## 모니터링 연계

배포 Event를 Metric과 로그에 기록한다.

deployId

artifactVersion

configVersion

instanceId

deployedAt

장애 발생 시 배포 전후를 비교할 수 있다.

# 책임 경계와 RACI

| 활동 | 개발 | DevOps | AA | DBA | 보안 | QA | 운영 | 업무 Owner |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Source Commit | A/R | I | I | I | I | I | I | I |
| CI Pipeline | C | A/R | C | I | C | C | I | I |
| Build 기준 | C | R | A | I | C | C | I | I |
| WAR Packaging | C | A/R | C | I | C | C | I | I |
| Artifact 저장 | I | A/R | C | I | C | I | C | I |
| 환경설정 | C | R | C | C | A/C | I | A/R | I |
| Secret | I | R/C | C | I | A/R | I | C | I |
| DB Migration | C | C | C | A/R | I | C | R/C | C |
| 배포 요청 | R | C | C | C | C | C | C | A/C |
| 배포 승인 | I | C | A/C | C | C | C | A/R | A/C |
| 배포 실행 | I | A/R | I | R/C | C | I | R/C | I |
| Health·Smoke | C | R | C | C | I | A/R | A/R | C |
| Rollback 판단 | C | C | A/C | C | C | C | A/R | A/C |
| 운영 인수 | I | C | C | C | C | C | A/R | A/C |
| 감사이력 | I | R | C | C | A/C | I | A/R | I |

# 데이터 및 상태관리

## Artifact 상태

BUILT

TESTED

APPROVED

PUBLISHED

DEPLOYED\_DEV

DEPLOYED\_STG

APPROVED\_PRD

DEPLOYED\_PRD

RETIRED

REVOKED

## 배포 상태

REQUESTED

APPROVED

SCHEDULED

DRAINING

DEPLOYING

STARTING

VERIFYING

SUCCESS

PARTIAL

FAIL

ROLLING\_BACK

ROLLED\_BACK

ROLLBACK\_FAIL

## 서버별 배포 상태

instanceId

previousVersion

targetVersion

trafficStatus

deployStatus

healthStatus

smokeStatus

startTime

endTime

# 성능·용량·확장성

## Pipeline 실행시간

확인:

Checkout

Dependency Download

Compile

Test

Static Scan

WAR Package

Upload

Deploy

공통 Dependency Cache를 사용할 수 있지만 Clean Build 재현성을 주기적으로 검증한다.

## CI Runner 용량

동시 Build 수

JDK Heap

Gradle Worker

Disk

Artifact 크기

Network

Test DB

공통 모듈 변경으로 모든 WAR를 병렬 Build하면 Runner 메모리가 부족할 수 있다.

## Artifact 크기

WAR 크기 급증 원인:

중복 Library

대형 Resource

테스트 데이터

Node Modules

로그·Dump

중복 공통 JAR

이전 Version 대비 증감 임계치를 둔다.

## 배포시간

WAR 전송시간

압축 해제시간

Spring Context 초기화

DB Pool Warm-up

Cache Warm-up

Health 대기

서버 수

를 포함한다.

## 다중 WAR 재기동

한 Tomcat 전체 재기동 시 모든 WAR가 동시에 초기화한다.

DB Connection 생성 폭증

Cache Load

JWKS 조회

Class Loading

Heap 증가

CPU 증가

Warm-up 순서와 Connection Pool 기동속도를 관리한다.

# 보안·개인정보·감사

| 영역 | 기준 |
| --- | --- |
| CI Runner | 최소권한·격리 |
| Repository | Branch 보호 |
| Artifact | Checksum·서명 |
| Secret | Vault·Protected Variable |
| 배포계정 | 운영 최소권한 |
| SSH | 개인키·접근통제 |
| 로그 | Secret 마스킹 |
| SBOM | 구성요소 추적 |
| 승인 | 직무분리 |
| 운영 설정 | 변경이력 |
| Artifact | 수정 불가 |
| Rollback | 승인·감사 |
| DB Script | 작성·검토·실행자 분리 |
| 운영 접근 | Bastion·접속기록 |
| 긴급배포 | 사후감사·기한 |

# 운영·모니터링·장애 대응

## Pipeline Metric

cicd.pipeline.count

cicd.pipeline.success.rate

cicd.pipeline.duration

cicd.build.failure.count

cicd.test.failure.count

cicd.security.gate.failure.count

cicd.deploy.success.count

cicd.deploy.failure.count

cicd.rollback.count

cicd.mean.time.to.recovery

## 배포 Metric

application.info{
version,
commit,
build,
configVersion
}

deploy.event

health.status

smoke.status

transaction.error.rate

transaction.timeout.rate

## 배포 로그

event=DEPLOY\_COMPLETED
deployId=DEP-20260718-017
environment=PRD
businessCode=SV
artifactVersion=1.4.2
commitId=a1b2c3d
configVersion=CFG-PRD-20260718-003
instanceId=sv-was-01
previousVersion=1.4.1
result=SUCCESS
health=UP
smoke=PASS
elapsedMs=184000

## 장애 점검 순서

1\. Deploy ID 확인
2\. 대상 Instance 확인
3\. Artifact·Checksum 확인
4\. Active Profile·Config Version 확인
5\. Tomcat·Spring 기동로그 확인
6\. Liveness·Readiness 확인
7\. DB·Cache·연계 Deep Check
8\. 대표 ServiceId Smoke 확인
9\. 오류율·Timeout·p95 비교
10\. WAR·설정·DB 변경 구분
11\. Rollback 가능성 판단
12\. 원복 후 재검증

# 자동검증 및 품질 Gate

## 1\. Source Gate

승인 Branch

Commit Signature

미추적 파일 없음

생성 Binary 없음

Secret 없음

## 2\. Build Gate

Gradle Wrapper

JDK Version

Clean Compile

Dependency Lock

Snapshot 금지

Test Skip 금지

## 3\. Test Gate

Unit

Integration

TCF 거래

Contract

Rollback

Security

## 4\. Architecture Gate

계층

ServiceId

Handler

OM

Mapper

Transaction

금지 의존

## 5\. Artifact Gate

WAR 존재

파일명

Mapper XML

공통 JAR

Secret 없음

Checksum

Signature

SBOM

Metadata

## 6\. Config Gate

Profile

필수 Key

Secret Reference

운영 URL

Pool·Timeout 범위

Config Version

## 7\. Database Gate

Forward Script

Rollback·Roll-forward

호환성

Lock

Validation SQL

Backup

## 8\. Deployment Gate

승인

Artifact ID

Checksum

대상 서버

Traffic Drain

Backup

Rollback Artifact

## 9\. Post Check Gate

Liveness

Readiness

Deep

Smoke

오류율

p95

Pool

거래로그

## 10\. 배포 차단

ServiceId 중복

Handler 미등록

OM Catalog 누락

Critical 취약점

Secret 노출

필수 테스트 실패

DB Migration 실패

Rollback Artifact 없음

Smoke 실패

중 하나가 발생하면 운영 배포를 차단한다.

# 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| CICD-001 | 정상 PR Pipeline | Artifact 생성 |
| CICD-002 | Compile 실패 | Package 미실행 |
| CICD-003 | Unit Test 실패 | 배포 차단 |
| CICD-004 | Integration 실패 | 배포 차단 |
| CICD-005 | ArchUnit 실패 | 배포 차단 |
| CICD-006 | Secret 발견 | 즉시 차단 |
| CICD-007 | Critical CVE | Release 차단 |
| CICD-008 | Snapshot Dependency | 운영 Package 차단 |
| CICD-009 | JDK Version 불일치 | Build 실패 |
| CICD-010 | Gradle Wrapper 변조 | 검증 실패 |
| CICD-011 | 전체 업무 WAR Build | 모두 성공 |
| CICD-012 | 단일 WAR Build | 대상만 생성 |
| CICD-013 | 공통 모듈 변경 | 영향 WAR 전체 Build |
| CICD-014 | Mapper XML 누락 | Artifact Gate 실패 |
| CICD-015 | TCF JAR 누락 | Artifact Gate 실패 |
| CICD-016 | WAR 내 Secret | Artifact 폐기 |
| CICD-017 | Checksum 불일치 | 배포 차단 |
| CICD-018 | Artifact 서명 불일치 | 배포 차단 |
| CICD-019 | SBOM 누락 | Release Gate 실패 |
| CICD-020 | 동일 Commit 재Build | Provenance 확인 |
| CICD-021 | DEV·PRD 각각 Build | 정책 실패 |
| CICD-022 | 동일 Artifact 승격 | 정상 |
| CICD-023 | local Profile 운영 배포 | 기동 차단 |
| CICD-024 | 운영 H2 설정 | Config Gate 실패 |
| CICD-025 | Secret Reference 없음 | Config 실패 |
| CICD-026 | Config Drift | 경보 |
| CICD-027 | 설정 Rollback | 이전값 복원 |
| CICD-028 | 신규 Column 선반영 | 구 WAR 정상 |
| CICD-029 | Column 삭제 후 구 WAR | 호환성 Gate 실패 |
| CICD-030 | DDL Lock 장기화 | 배포 중단 |
| CICD-031 | DB Migration 부분 실패 | WAR 미배포 |
| CICD-032 | 정상 Rolling WAS 2대 | 무중단 |
| CICD-033 | 첫 WAS Health 실패 | 다음 WAS 배포 중단 |
| CICD-034 | Traffic Drain | 신규 요청 없음 |
| CICD-035 | Graceful Timeout | 정책대로 종료 |
| CICD-036 | WAR 원자적 교체 | 부분파일 없음 |
| CICD-037 | Tomcat 기동 실패 | Rollback |
| CICD-038 | Liveness 실패 | Rollback |
| CICD-039 | Readiness 실패 | 트래픽 미복귀 |
| CICD-040 | Deep DB 실패 | 배포 실패 |
| CICD-041 | 선택 외부연계 실패 | 정책상 Ready 판정 |
| CICD-042 | Smoke 실패 | Rollback |
| CICD-043 | Smoke 성공·p95 악화 | 관찰·Rollback |
| CICD-044 | 오류율 급증 | Rollback |
| CICD-045 | Pool Pending 급증 | Rollback 판단 |
| CICD-046 | Blue/Green 정상 전환 | Green 운영 |
| CICD-047 | Green 오류 | Blue 복귀 |
| CICD-048 | Canary 5% 정상 | 단계 확대 |
| CICD-049 | Canary 오류 | 신규 Traffic 0 |
| CICD-050 | 다중 WAR 한 개 배포 | 다른 업무 정상 |
| CICD-051 | 공통 JAR 변경 | 전체 영향 회귀 |
| CICD-052 | WAR 20회 Hot Deploy | Metaspace 안정 |
| CICD-053 | Executor 미종료 | Leak 탐지 |
| CICD-054 | Scheduler 중복 | 배포 실패 |
| CICD-055 | Session 사용자 Rolling | 세션 유지 |
| CICD-056 | JWT 키 호환 | 기존 Token 정상 |
| CICD-057 | 이전 Artifact Rollback | 정상 복구 |
| CICD-058 | Rollback Checksum 오류 | 복구 차단·대체본 |
| CICD-059 | 설정만 Rollback | 정상 |
| CICD-060 | DB 비호환 Rollback | No-Go |
| CICD-061 | Roll-forward Hotfix | 표준 Pipeline |
| CICD-062 | 운영 서버 직접 Build | 감사·정책 실패 |
| CICD-063 | Class 파일 직접 교체 | 정책 실패 |
| CICD-064 | Mapper만 교체 | 정책 실패 |
| CICD-065 | 배포 요청자=승인자 | 승인 차단 |
| CICD-066 | 승인 없는 PRD 배포 | 차단 |
| CICD-067 | Rollback 사유 없음 | 요청 실패 |
| CICD-068 | 배포 이력 누락 | 운영 Gate 실패 |
| CICD-069 | Deploy ID 로그 | 추적 가능 |
| CICD-070 | Config Version 로그 | 추적 가능 |
| CICD-071 | Artifact Repository 장애 | 배포 미실행 |
| CICD-072 | CI Runner 장애 | Build 재시도 |
| CICD-073 | Repository 장애 중 운영 | 기존 서비스 유지 |
| CICD-074 | 인증서 만료 배포 | 보안 Gate 실패 |
| CICD-075 | Apache Route Reload 실패 | Route Rollback |
| CICD-076 | 배포 중 Batch 실행 | 통제 확인 |
| CICD-077 | 초기 안정화 Metric | 기준 충족 |
| CICD-078 | UNKNOWN 거래 발생 | 안정화 미종료 |
| CICD-079 | DR Artifact 승격 | 동일 Checksum |
| CICD-080 | 전체 증적 재현 | 감사 PASS |

# 따라 하는 실무 절차

## 1단계. Release 범위를 확정한다

요구사항

ServiceId

모듈

DB

설정

연계

운영 영향

## 2단계. Release Version과 Tag를 만든다

1.4.2

v1.4.2

## 3단계. Clean CI Build를 수행한다

Checkout

Compile

Test

Quality

Security

## 4단계. WAR와 Metadata를 생성한다

Artifact

Commit

Checksum

SBOM

Signature

## 5단계. Artifact Repository에 등록한다

등록 후 내용을 수정하지 않는다.

## 6단계. 동일 Artifact를 DEV·STG에서 검증한다

새로 Build하지 않는다.

## 7단계. 운영 설정과 Secret을 준비한다

Config Version

Secret Reference

Profile

Drift

## 8단계. DB·WAR·Route 순서를 작성한다

구·신 Version 공존을 검증한다.

## 9단계. 배포요청과 승인을 등록한다

요청자

승인자

Artifact

사유

시간

## 10단계. 대상 서버를 Drain한다

기존 요청 종료와 Batch 상태를 확인한다.

## 11단계. 배포하고 4단계 Health를 확인한다

Live

Ready

Deep

Smoke

## 12단계. Metric을 확인하고 트래픽을 복귀한다

문제 시 즉시 Rollback한다.

# 완료 체크리스트

## Source·Build

| 확인 항목 | 완료 |
| --- | --- |
| 승인된 Commit·Tag다. | □ |
| Gradle Wrapper를 사용했다. | □ |
| JDK Version이 일치한다. | □ |
| Clean Build를 수행했다. | □ |
| Unit·Integration Test가 성공했다. | □ |
| Architecture Gate가 성공했다. | □ |
| Security Scan이 성공했다. | □ |
| Snapshot Dependency가 없다. | □ |
| 테스트를 Skip하지 않았다. | □ |

## Artifact

| 확인 항목 | 완료 |
| --- | --- |
| bootWar가 성공했다. | □ |
| WAR 파일명이 표준이다. | □ |
| Mapper XML이 포함됐다. | □ |
| TCF 공통 JAR가 포함됐다. | □ |
| 테스트·로그·임시파일이 없다. | □ |
| Secret이 포함되지 않았다. | □ |
| Checksum이 있다. | □ |
| Signature가 있다. | □ |
| SBOM이 있다. | □ |
| Artifact Repository에 등록됐다. | □ |
| 직전 정상 Artifact가 있다. | □ |

## 환경설정

| 확인 항목 | 완료 |
| --- | --- |
| 동일 Artifact를 승격한다. | □ |
| Profile이 환경과 일치한다. | □ |
| 외부 Config를 사용한다. | □ |
| Config Version이 있다. | □ |
| Secret은 Vault에 있다. | □ |
| 운영 URL·DB가 정확하다. | □ |
| Timeout·Pool 범위가 승인됐다. | □ |
| Config Drift가 없다. | □ |
| 재기동 여부가 명확하다. | □ |

## 배포 전

| 확인 항목 | 완료 |
| --- | --- |
| Change·Deploy ID가 있다. | □ |
| 영향 범위를 확인했다. | □ |
| 필수 승인자가 승인했다. | □ |
| DB Migration을 검증했다. | □ |
| 구·신 Version이 호환된다. | □ |
| Rollback 조건이 있다. | □ |
| Rollback Artifact가 있다. | □ |
| Runbook·연락망이 있다. | □ |
| Traffic Drain 방법이 있다. | □ |
| 백업을 확인했다. | □ |

## 배포 후

| 확인 항목 | 완료 |
| --- | --- |
| Artifact·Checksum이 일치한다. | □ |
| Active Profile이 맞다. | □ |
| Liveness가 UP이다. | □ |
| Readiness가 UP이다. | □ |
| Deep Check가 정상이다. | □ |
| Smoke Test가 성공했다. | □ |
| 오류율이 정상이다. | □ |
| p95가 정상이다. | □ |
| Thread·Heap·Pool이 정상이다. | □ |
| 거래로그가 정상이다. | □ |
| 트래픽이 복귀됐다. | □ |
| 배포이력이 저장됐다. | □ |

## Rollback·운영전환

| 확인 항목 | 완료 |
| --- | --- |
| Application Rollback을 시험했다. | □ |
| Config Rollback을 시험했다. | □ |
| DB 복구방식이 있다. | □ |
| 데이터 대사방식이 있다. | □ |
| Rollback 승인자가 있다. | □ |
| Rollback 결과가 감사된다. | □ |
| 초기 안정화 기간이 있다. | □ |
| 안정화 종료조건이 있다. | □ |
| 잔여 이슈 Owner가 있다. | □ |
| 운영 인수가 완료됐다. | □ |

# 변경·호환성·폐기 관리

## Pipeline 변경

다음 변경도 애플리케이션 변경처럼 관리한다.

Runner Image

JDK

Gradle

Plugin

Test Stage

Security Scanner

배포 Script

Health Check

Pipeline 변경은 별도 테스트 환경에서 검증한다.

## Artifact 정책 변경

파일명

Version

Checksum

보관기간

서명방식

Repository

변경 시 Rollback Tool과 운영 Script 호환성을 확인한다.

## Profile 변경

dev

stg

prod

dr

추가·통합·폐기 시 모든 모듈의 설정 누락을 검사한다.

## 배포대상 모듈 변경

신규 업무 WAR 추가:

Gradle Module

manifest.yaml

Artifact Name

Context Path

Target Group

Health URL

Smoke ServiceId

로그 경로

DB Pool

Rollback

를 함께 등록한다.

## 배포 Script 폐기

신규 Pipeline 적용

병행기간

호출량 확인

구 Script 실행권한 제거

문서 제거

Archive

감사 이력 유지

개발자 개인 Script가 운영에서 계속 사용되지 않도록 한다.

## Artifact 폐기

취약점·키 유출 등으로 사용 금지된 Artifact는 REVOKED 처리한다.

신규 배포 금지

현재 사용 Instance 확인

대체 Version 배포

Artifact 보존·접근제한

사유 기록

# 시사점

## 핵심 아키텍처 판단

첫째, CI/CD는 Build 명령을 자동으로 실행하는 도구가 아니라 소스·검증·Artifact·승인·배포·Rollback을 연결하는 통제체계다.

둘째, 운영 Artifact는 반드시 승인된 CI에서 한 번 생성하고 개발·검증·운영환경에 동일하게 승격해야 한다.

셋째, WAR와 환경설정·Secret을 분리해야 Artifact의 재현성과 보안을 함께 확보할 수 있다.

넷째, 배포 완료조건은 Tomcat 기동이 아니다.

Liveness

Readiness

Deep Check

Smoke Test

운영 Metric

이 모두 정상이어야 한다.

다섯째, 하나의 Tomcat에 여러 WAR를 배포하면 WAR는 독립 배포단위지만 JVM·Thread·Heap·프로세스 장애영역은 공유된다.

여섯째, 애플리케이션 Rollback과 DB·데이터 Rollback은 서로 다르다.

WAR 복구
≠ 데이터 복구

일곱째, Rollback 가능성은 장애 발생 후가 아니라 설계와 DB 변경 단계에서 확보해야 한다.

여덟째, OM은 배포를 직접 수행하는 단순 화면보다 요청·승인·Artifact·진행상태·검증·감사를 관리하는 운영 통제점으로 발전해야 한다.

## 주요 위험

| 위험 | 결과 |
| --- | --- |
| 개인 PC Build | 재현성·감사 상실 |
| 환경별 재Build | 테스트 Artifact와 운영 Artifact 불일치 |
| Artifact 수정 | 무결성 상실 |
| Secret 내장 | 비밀정보 유출 |
| 운영 직접 설정수정 | Configuration Drift |
| Snapshot 운영배포 | Version 재현 불가 |
| 부분 Class 교체 | 코드·SQL 불일치 |
| Health 단일 확인 | 실제 업무장애 누락 |
| DB 호환성 미검토 | Rollback 실패 |
| Traffic Drain 없음 | 처리 중 거래 중단 |
| Hot Deploy 반복 | ClassLoader·Metaspace Leak |
| 다중 WAR 전체 재기동 | 업무 전체 영향 |
| Rollback 본 없음 | 장애 장기화 |
| 배포 후 Metric 미확인 | 성능퇴행 누락 |
| 요청·승인 직무 미분리 | 내부통제 위반 |
| 배포이력 누락 | 장애·감사 추적 불가 |
| Pipeline 자체 미검증 | 자동화된 장애 |
| 모듈목록 혼재 | 일부 WAR 누락·오배포 |

## 우선 보완 과제

1.  실제 배포대상 모듈·WAR·Context를 하나의 공식 Manifest로 통합한다.
2.  운영 Artifact를 CI에서만 생성하도록 운영 서버 Build 권한을 제거한다.
3.  Artifact Repository와 Commit·Version·Checksum 연계를 구현한다.
4.  WAR 서명과 SBOM 생성을 Release Gate에 추가한다.
5.  동일 Artifact의 DEV→STG→PRD 승격을 강제한다.
6.  tcf-cicd 설정을 단일 원천으로 만들고 양방향 수동 Sync를 줄인다.
7.  Secret을 Vault·KMS·HSM 참조방식으로 외부화한다.
8.  환경별 Config Schema와 Drift 검사를 자동화한다.
9.  OM 배포 승인과 실제 CI Pipeline 실행을 API로 연계한다.
10.  OM의 로컬 Gradle·WAR 복사 기능을 운영경로와 분리한다.
11.  Liveness·Readiness·Deep·Smoke 4단계 검증을 구현한다.
12.  L4·Apache Traffic Drain과 Rolling Deploy를 자동화한다.
13.  DB Expand–Migrate–Contract와 Migration Version을 Pipeline에 통합한다.
14.  배포 후 오류율·p95·Thread·Pool 기반 자동 판정을 추가한다.
15.  Application·Config·DB·Route Rollback 훈련을 정기적으로 수행한다.

## 중장기 발전 방향

개발자 수동 Build
↓
표준 CI Build

서버 파일 복사
↓
Artifact Repository

환경별 재Build
↓
Build Once, Deploy Many

WAR 내부 설정
↓
외부 Config·Vault

단순 Tomcat 기동
↓
4단계 Health·Smoke

전체 중지 배포
↓
Rolling·Blue/Green

수동 확인
↓
Metric 기반 자동판정

WAR Rollback
↓
Application·Config·DB 통합복구

배포 Script
↓
OM 승인형 Deployment Orchestrator

배포 이력
↓
요구사항–Commit–Artifact–Instance–거래로그 추적

# 마무리말

CI/CD와 배포를 설계하는 과정은 다음 질문에 답하는 일이다.

어느 Commit이 운영 Artifact의 원본인가?

누가 어떤 환경에서 Artifact를 만들었는가?

같은 Commit으로 같은 결과를 재현할 수 있는가?

단위·통합·아키텍처·보안 Gate가 통과했는가?

WAR 안에 Mapper와 공통 TCF JAR가 포함됐는가?

Secret과 운영정보가 Artifact에 포함되지 않았는가?

Artifact의 Checksum과 Signature는 무엇인가?

DEV에서 검증한 Artifact와 PRD Artifact가 같은가?

환경별 설정은 어디에서 관리되는가?

운영 서버 설정이 Git 기준과 일치하는가?

DB와 WAR 중 무엇을 먼저 배포해야 하는가?

구 WAR와 신 WAR가 동시에 실행돼도 되는가?

사용자 트래픽을 어떻게 제외하고 복귀시키는가?

하나의 Tomcat에서 다른 WAR에 영향이 없는가?

Liveness·Readiness·Deep·Smoke가 모두 정상인가?

배포 후 오류율과 p95는 이전과 같은가?

어떤 조건에서 자동으로 Rollback할 것인가?

직전 안정 Artifact는 즉시 사용할 수 있는가?

WAR를 되돌렸을 때 DB와 데이터도 호환되는가?

누가 배포를 요청·승인·실행·검증했는가?

운영자는 Deploy ID로 전체 과정을 추적할 수 있는가?

제27장의 핵심 흐름은 다음과 같다.

Git Commit
↓
CI 검증
↓
WAR 패키징
↓
Artifact·Checksum·SBOM
↓
환경별 동일 Artifact 승격
↓
배포 승인
↓
DB·Config·WAR 전환
↓
Health·Smoke·Metric
↓
트래픽 복귀
↓
정상 완료 또는 Rollback

가장 중요한 원칙은 다음과 같다.

배포 자동화의 수준은
명령이 얼마나 적은지로 판단하지 않는다.

어떤 소스에서 만들어졌고,
어떤 검증을 통과했으며,
어떤 설정으로 실행되고,
문제 시 어디까지 복구할 수 있는지를

누구나 같은 절차와 증적으로
확인할 수 있어야

안전하고 반복 가능한
CI/CD 체계가 된다.
