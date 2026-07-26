<!-- source: ztcf-집필본/NSIGHT TCF Chapter 6- Git으로 프로젝트 가져오기.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제6장. Git으로 프로젝트 가져오기

## 이 장을 시작하며

제5장에서는 JDK, Gradle JVM, IDE와 인코딩 설정을 점검하여 재현 가능한 개발환경을 준비했다.

이제 개발할 소스를 Git 저장소에서 가져와야 한다.

초보 개발자는 Git을 다음과 같이 생각하기 쉽다.

Git 저장소를 Clone한다.
→ IDE에서 프로젝트를 연다.
→ 코드를 수정한다.
→ Commit하고 Push한다.

큰 흐름은 맞다.

그러나 실제 프로젝트에서 Git은 단순한 파일 보관 도구가 아니다.

요구사항
↓
설계 변경
↓
소스·설정·DB 변경
↓
Commit
↓
Pull Request
↓
자동검증·코드리뷰
↓
승인
↓
빌드 산출물
↓
배포
↓
운영 장애·변경 이력 추적

Git 이력에는 단순히 “어떤 파일이 바뀌었는가”만 남는 것이 아니다.

다음 질문에 답할 수 있어야 한다.

왜 이 코드를 변경했는가?

어떤 요구사항과 ServiceId에 해당하는가?

어느 화면과 프로그램에 영향을 주는가?

DB·설정·운영 기준정보도 함께 바뀌는가?

어떤 테스트를 수행했는가?

문제가 생기면 어떻게 되돌릴 것인가?

누가 검토하고 승인했는가?

NSIGHT TCF는 화면 이벤트, ServiceId, Handler, Facade, Service, Rule, DAO, Mapper, SQL과 DB 객체를 연결해 관리하는 구조다. 따라서 Git 변경 이력도 이 추적관계를 보존해야 한다.

## 핵심 관점

Git의 목적은
파일을 저장하는 것이 아니다.

변경 이유와 변경 범위를 남기고,
다른 사람이 안전하게 검토하며,
문제가 생겼을 때 원인을 찾고
이전 상태로 복구할 수 있게 하는 것이다.

## 학습 목표

이 장을 마치면 다음 내용을 직접 수행할 수 있어야 한다.

| 번호 | 학습 목표 |
| --- | --- |
| 1 | Git 저장소, Working Tree, Index와 Commit의 차이를 설명한다. |
| 2 | HTTPS와 SSH 방식의 차이를 설명한다. |
| 3 | 저장소를 안전한 경로에 Clone한다. |
| 4 | Clone 직후 Remote·Branch·Commit·상태를 점검한다. |
| 5 | 프로젝트 Root와 Gradle 멀티모듈 구조를 확인한다. |
| 6 | main, develop, 기능 브랜치의 책임을 구분한다. |
| 7 | 하나의 논리적 변경 단위로 Commit한다. |
| 8 | Pull Request에 영향 범위와 테스트 결과를 기록한다. |
| 9 | Merge와 Rebase의 차이 및 적용 주의사항을 설명한다. |
| 10 | 충돌 발생 파일을 분석하고 안전하게 해결한다. |
| 11 | 비밀번호·Token·Private Key의 Commit을 방지한다. |
| 12 | Build·Log·IDE 생성파일을 저장소에서 제외한다. |
| 13 | 실수로 Commit된 비밀정보의 대응 절차를 설명한다. |
| 14 | Git 변경과 ServiceId·설계서·테스트 결과를 연결한다. |

# 한눈에 보는 Git 작업 흐름

┌─────────────────────────────────────────────────────────────┐
│ 1. 중앙 Git 저장소 │
│ main · develop · feature branches │
└──────────────────────────┬──────────────────────────────────┘
│ clone / fetch
▼
┌─────────────────────────────────────────────────────────────┐
│ 2. Local Repository │
│ Commit 이력 · Branch · Remote 정보 │
└──────────────────────────┬──────────────────────────────────┘
│ checkout / switch
▼
┌─────────────────────────────────────────────────────────────┐
│ 3. Working Tree │
│ 개발자가 실제로 편집하는 파일 │
└──────────────────────────┬──────────────────────────────────┘
│ git add
▼
┌─────────────────────────────────────────────────────────────┐
│ 4. Staging Area │
│ 다음 Commit에 포함할 변경 │
└──────────────────────────┬──────────────────────────────────┘
│ git commit
▼
┌─────────────────────────────────────────────────────────────┐
│ 5. Local Commit │
│ 변경 이유와 결과를 기록한 복구 가능한 단위 │
└──────────────────────────┬──────────────────────────────────┘
│ git push
▼
┌─────────────────────────────────────────────────────────────┐
│ 6. Pull Request │
│ 자동검증 · 코드리뷰 · 승인 · Merge │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 7. CI/CD │
│ Build · Test · 품질 Gate · Artifact 생성 │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 8. 배포·운영 │
│ Commit ID · Artifact Version · 배포 이력 · 장애 추적 │
└─────────────────────────────────────────────────────────────┘

# Git의 핵심 영역

| 영역 | 의미 | 주요 명령 |
| --- | --- | --- |
| Remote Repository | 팀이 공유하는 중앙 저장소 | clone, fetch, push |
| Local Repository | 개발자 PC의 Git 이력 저장소 | log, branch, show |
| Working Tree | 실제 편집 중인 파일 | status, diff |
| Staging Area | 다음 Commit 대상 | add, restore --staged |
| Commit | 변경 이유와 내용을 고정한 시점 | commit, show |
| Branch | 독립적인 변경 흐름 | switch, branch |
| Tag | 배포·릴리스 시점 식별 | tag |
| Pull Request | 변경 검토와 승인 단위 | 저장소 웹 기능 |
| Artifact | Commit으로부터 생성된 배포 파일 | JAR·WAR |

# Git과 NSIGHT TCF 추적성

요구사항 ID
↓
화면 ID·이벤트 ID
↓
ServiceId·거래코드
↓
변경 프로그램
↓
Commit ID
↓
Pull Request
↓
빌드 번호·Artifact Version
↓
배포 이력
↓
거래로그·장애 이력

예:

요구사항
REQ-SV-CUS-014

화면
SV-CUS-0001

ServiceId
SV.Customer.selectSummary

변경 프로그램
SvCustomerHandler
SvCustomerFacade
SvCustomerService
SvCustomerMapper.xml

Commit
a12bc34

Pull Request
PR-127

배포
sv-service-1.4.2.war

운영 추적
serviceId=SV.Customer.selectSummary
appVersion=1.4.2
commitId=a12bc34

이 구조가 유지되면 운영 오류가 발생했을 때 어떤 변경이 해당 거래에 반영되었는지를 역추적할 수 있다.

# 6.1 저장소 Clone과 최초 점검

## 6.1.1 Clone이란 무엇인가

Clone은 중앙 저장소의 현재 파일만 복사하는 작업이 아니다.

다음 정보를 함께 가져온다.

소스 파일
\+ 디렉터리 구조
\+ Commit 이력
\+ Branch 정보
\+ Tag
\+ Remote 설정
\+ Git 관리 속성

ZIP 파일을 내려받아 압축을 푸는 방식과 다르다.

| 구분 | Git Clone | ZIP 다운로드 |
| --- | --- | --- |
| Commit 이력 | 포함 | 일반적으로 없음 |
| Branch 전환 | 가능 | 불가 |
| 원격 변경 수신 | 가능 | 다시 다운로드 |
| Push | 가능 | 불가 |
| 변경 비교 | 정확 | 별도 도구 필요 |
| 복구 | Commit 기준 | 백업 파일 필요 |
| 협업 | 적합 | 부적합 |

프로젝트 개발은 공식 저장소를 Clone하는 방식으로 시작한다.

## 6.1.2 Clone 전 확인사항

저장소 주소를 받았다고 즉시 Clone하지 않는다.

먼저 다음 정보를 확인한다.

| 확인 항목 | 질문 |
| --- | --- |
| 공식 저장소 | 이 저장소가 프로젝트 기준본인가? |
| 접근 권한 | 읽기·쓰기 권한이 필요한가? |
| 기본 Branch | main인가, develop인가? |
| 개발 시작 Branch | 어느 Branch에서 분기해야 하는가? |
| 인증 방식 | HTTPS·SSH 중 무엇을 사용하는가? |
| VPN·사내망 | 사내망 연결이 필요한가? |
| Proxy | Git Proxy 설정이 필요한가? |
| 인증서 | 사내 CA 인증서 설치가 필요한가? |
| 대용량 파일 | Git LFS를 사용하는가? |
| Submodule | 별도 저장소가 포함되는가? |
| Clone 경로 | 공백·한글·동기화 폴더를 피했는가? |
| 비밀정보 | 별도 설정을 어디에서 받아야 하는가? |

## 6.1.3 HTTPS와 SSH

### HTTPS

git clone https://git-server/group/nsight-tcf-framework.git

특징:

| 항목 | 설명 |
| --- | --- |
| 인증 | 계정·Personal Access Token 등 |
| 방화벽 | 일반 HTTPS Port 사용 |
| 설정 | 비교적 단순 |
| Token | 비밀번호 대신 Token 정책 가능 |
| 주의 | URL에 Token을 직접 넣지 않음 |

금지 예:

https://user:token@git-server/project.git

명령 이력, 로그와 설정에 Token이 노출될 수 있다.

### SSH

git clone git@git-server:group/nsight-tcf-framework.git

특징:

| 항목 | 설명 |
| --- | --- |
| 인증 | SSH Private Key |
| 비밀번호 반복 | 줄일 수 있음 |
| 공개키 등록 | Git 서버에 Public Key 등록 |
| Private Key | 개발자 PC에서 안전하게 보관 |
| 주의 | Private Key를 저장소에 Commit하지 않음 |

SSH 연결 점검:

ssh -T git@git-server

실제 Host와 명령은 프로젝트 Git 서버 기준을 사용한다.

## 6.1.4 SSH Key 관리

SSH Private Key는 다음 위치에 두지 않는다.

프로젝트 디렉터리
Git 저장소
공유폴더
메일 첨부
메신저
문서관리 시스템
소스 주석

권장:

사용자 홈의 .ssh 디렉터리
\+ OS 파일 권한 제한
\+ Passphrase
\+ 승인된 Key Algorithm
\+ 주기적 교체

대표 파일:

~/.ssh/id\_ed25519
~/.ssh/id\_ed25519.pub
~/.ssh/config
~/.ssh/known\_hosts

.pub는 공개키이며 Private Key와 구분해야 한다.

## 6.1.5 안전한 Clone 경로

Windows 예:

C:\\workspace\\nsight-tcf-framework

Linux 예:

/home/developer/workspace/nsight-tcf-framework

피해야 할 예:

C:\\Users\\사용자\\OneDrive\\바탕 화면\\새 폴더 (1)\\소스 최종
네트워크 공유 드라이브
자동 백신 검사로 파일 잠금이 잦은 경로
OS 관리자 권한이 필요한 경로

문제가 될 수 있는 요소:

| 요소 | 위험 |
| --- | --- |
| OneDrive 동기화 | .git 파일 충돌·잠금 |
| 너무 긴 경로 | Windows 경로 제한 |
| 한글·특수문자 | 일부 스크립트 오류 |
| 관리자 폴더 | Build·삭제 권한 오류 |
| 네트워크 드라이브 | 느린 Build·파일 잠금 |
| 압축 해제 중첩 | 프로젝트 Root 혼동 |

## 6.1.6 Clone 실행

git clone <공식 저장소 주소>

특정 디렉터리명:

git clone <공식 저장소 주소> nsight-tcf-framework

특정 Branch:

git clone -b develop <공식 저장소 주소>

그러나 기본 Branch 정책을 모른 채 -b를 임의로 지정하지 않는다.

## 6.1.7 Clone 직후 최초 점검

프로젝트 디렉터리로 이동한다.

cd nsight-tcf-framework

### 현재 상태

git status

기대 결과:

On branch develop
Your branch is up to date with 'origin/develop'.

nothing to commit, working tree clean

working tree clean은 Clone 이후 사용자가 변경한 파일이 없다는 의미다.

### Remote 확인

git remote -v

확인:

fetch 주소
push 주소
공식 저장소 여부
개인 Fork 여부

예:

origin git@git-server:group/nsight-tcf-framework.git (fetch)
origin git@git-server:group/nsight-tcf-framework.git (push)

### Branch 확인

git branch
git branch -a

또는:

git status -sb

확인할 것:

현재 Branch
원격 추적 Branch
main·develop 존재 여부
release·feature Branch
삭제된 Branch의 잔존 여부

### 최근 Commit 확인

git log --oneline --decorate -10

확인:

| 항목 | 확인 내용 |
| --- | --- |
| 최근 Commit | 프로젝트가 예상 시점의 버전인가 |
| Branch | 올바른 Branch를 보고 있는가 |
| Tag | 배포 Tag가 있는가 |
| 작성자 | 변경 주체가 식별되는가 |
| 메시지 | 변경 목적이 이해되는가 |

### 저장소 Root 확인

git rev-parse --show-toplevel

Gradle Root 파일도 확인한다.

settings.gradle
build.gradle
gradle.properties
gradlew 또는 Gradle 실행정책

NSIGHT TCF는 다수의 플랫폼 모듈과 업무 WAR로 구성된 멀티모듈 구조이므로, 하위 src 디렉터리가 아니라 전체 저장소 Root를 IDE와 Gradle의 기준점으로 사용한다.

## 6.1.8 Clone 후 디렉터리 확인

대표적인 구조는 다음과 같다.

nsight-tcf-framework
├─ settings.gradle
├─ build.gradle
├─ gradle.properties
├─ tcf-util
├─ tcf-core
├─ tcf-web
├─ tcf-cache
├─ tcf-eai
├─ tcf-gateway
├─ tcf-jwt
├─ tcf-om
├─ tcf-batch
├─ sv-service
├─ ic-service
├─ pc-service
└─ ...

### 주요 점검

| 대상 | 확인 |
| --- | --- |
| settings.gradle | 참여 모듈 |
| Root build.gradle | 공통 Plugin·Version |
| 모듈 build.gradle | 모듈별 의존성 |
| .gitignore | 생성파일 제외 |
| .gitattributes | 줄바꿈 정책 |
| .editorconfig | IDE 편집 기준 |
| README | 실행·빌드 지침 |
| CI 파일 | 공식 빌드 방식 |
| 환경 예제 | application-local-example.yml 등 |
| Mapper 경로 | Resource 포함 여부 |

## 6.1.9 Submodule 확인

저장소가 Git Submodule을 사용한다면 다음 파일이 존재할 수 있다.

.gitmodules

점검:

git submodule status

초기화:

git submodule update --init --recursive

Clone과 동시에:

git clone --recurse-submodules <저장소 주소>

Submodule은 별도 저장소의 특정 Commit을 가리킨다.

따라서 Submodule 디렉터리에서 임의로 최신 Branch로 이동하면 전체 저장소가 의도한 버전과 달라질 수 있다.

## 6.1.10 Git LFS 확인

대용량 Binary를 Git LFS로 관리하는 프로젝트에서는 다음을 확인한다.

git lfs version
git lfs status

.gitattributes 예:

\*.zip filter=lfs diff=lfs merge=lfs -text
\*.jar filter=lfs diff=lfs merge=lfs -text

다만 JAR·WAR·Build 산출물을 Git LFS로 관리할 것인지, Artifact Repository로 관리할 것인지는 프로젝트 정책에 따른다.

일반적으로 빌드 가능한 산출물은 Git보다 Nexus·Artifactory 등 Artifact 저장소가 적합하다.

## 6.1.11 Clone 직후 Build

소스를 수정하기 전에 기준 Branch가 원래부터 정상인지 확인한다.

gradle clean build

Wrapper 사용 시:

./gradlew clean build

Windows:

gradlew.bat clean build

### 왜 수정 전에 빌드하는가

Clone 직후 Build 실패
\= 기준 Branch·환경·의존성 문제 가능

소스 수정 후 Build 실패
\= 기존 문제인지 내 변경 문제인지 구분 어려움

따라서 최초 Build 결과를 보존해야 한다.

| 항목 | 기록 |
| --- | --- |
| Branch | develop |
| Commit ID | 전체 Hash 또는 Short Hash |
| JDK | Version·Vendor |
| Gradle | Version |
| 실행 명령 | clean build |
| 결과 | Success·Failure |
| 실패 Task | Task명 |
| 실행 시각 | 로컬 시간 |
| 환경 | OS·Profile |

## 6.1.12 Commit ID 확인

git rev-parse HEAD

짧은 값:

git rev-parse --short HEAD

Commit ID는 최초 빌드, 테스트 결과와 배포 산출물에 함께 기록하는 것이 좋다.

예:

branch=develop
commit=4f72a9c
jdk=21.0.x
gradle=8.x
build=SUCCESS

## 6.1.13 최초 점검 체크

공식 저장소 확인
↓
올바른 Branch 확인
↓
Remote 확인
↓
Working Tree Clean 확인
↓
Commit ID 기록
↓
Submodule·LFS 확인
↓
JDK·Gradle 확인
↓
Clean Build
↓
IDE Import
↓
업무 모듈 기동

# 6.2 브랜치·커밋·Pull Request 기본

## 6.2.1 Branch란 무엇인가

Branch는 단순한 소스 복사본이 아니다.

Commit 이력에서 독립적인 변경 흐름을 가리키는 이름이다.

main
── A ── B ── C

develop
└─ D ── E

feature/customer-summary
└─ F ── G

각 Branch는 목적과 책임이 달라야 한다.

## 6.2.2 권장 Branch 역할

| Branch | 목적 | 직접 Push |
| --- | --- | --- |
| main | 운영 또는 공식 릴리스 기준 | 원칙적 금지 |
| develop | 다음 통합 버전 기준 | 원칙적 금지 |
| feature/\* | 개별 기능 개발 | 허용 |
| fix/\* | 일반 결함 수정 | 허용 |
| hotfix/\* | 운영 긴급 수정 | 승인 필요 |
| release/\* | 릴리스 안정화 | 제한 |
| chore/\* | 빌드·문서·도구 변경 | 허용 |
| refactor/\* | 기능 변화 없는 구조 개선 | 허용 |

실제 프로젝트의 Branch 정책이 우선이며, 위 형식은 대표 예시다.

## 6.2.3 Branch 이름

권장 형식:

{유형}/{요구사항ID}-{간단한-설명}

예:

feature/REQ-SV-014-customer-summary
fix/DEF-231-timeout-message
hotfix/INC-20260718-jwt-validation
refactor/SV-customer-handler
chore/update-gradle-config

### 좋은 Branch 이름

| 예 | 장점 |
| --- | --- |
| feature/REQ-SV-014-customer-summary | 요구사항 추적 가능 |
| fix/DEF-231-null-customer-no | 결함과 원인 식별 |
| hotfix/INC-1452-token-expiry | 운영 사고와 연결 |

### 좋지 않은 Branch 이름

test
new
final
mybranch
customer
kim
20260718

변경 목적과 책임을 알기 어렵다.

## 6.2.4 Branch 생성

기준 Branch 최신화:

git switch develop
git fetch origin
git pull --ff-only

기능 Branch 생성:

git switch -c feature/REQ-SV-014-customer-summary

확인:

git status -sb

## 6.2.5 fetch와 pull

| 명령 | 역할 |
| --- | --- |
| git fetch | 원격 변경정보만 가져옴 |
| git pull | Fetch 후 Merge 또는 Rebase |
| git pull --ff-only | Fast-forward만 허용 |
| git pull --rebase | 로컬 Commit을 원격 뒤에 재배치 |

초보자는 pull 결과로 자동 Merge Commit이 생길 수 있다는 점을 알아야 한다.

안전한 확인 순서:

git fetch origin
git status
git log --oneline --graph --decorate --all -20

상태를 본 뒤 Merge 또는 Rebase를 선택한다.

## 6.2.6 Commit의 의미

Commit은 작업 중간 저장 버튼이 아니다.

하나의 논리적 변경과 그 이유를 기록하는 단위다.

좋은 Commit
\= 한 가지 목적
\+ 검토 가능한 크기
\+ Build 가능한 상태
\+ 의미 있는 메시지
\+ 민감정보 없음

## 6.2.7 Atomic Commit

하나의 Commit은 가능한 한 하나의 목적만 포함한다.

### 권장 분리

Commit 1
CustomerSummaryRequest 필드·검증 추가

Commit 2
고객요약 Mapper SQL 추가

Commit 3
Service·Facade 처리 구현

Commit 4
단위·통합 테스트 추가

또는 기능이 작다면 하나의 완결된 Commit으로 묶을 수 있다.

중요한 것은 Commit을 되돌렸을 때 기능이 일관되게 되돌아가야 한다는 것이다.

## 6.2.8 한 Commit에 섞지 말아야 할 변경

업무 기능 구현
\+ 전체 파일 포맷팅
\+ 줄바꿈 변환
\+ 패키지 일괄 이동
\+ Gradle Upgrade
\+ 라이브러리 버전 변경
\+ 무관한 결함 수정

이렇게 섞이면 코드리뷰와 장애 원인 분석이 어려워진다.

## 6.2.9 Commit 전 점검

git status
git diff
git diff --check

Staging:

git add <파일>

부분 Staging:

git add -p

Staged 변경 확인:

git diff --staged

실수로 Staging한 파일 제외:

git restore --staged <파일>

## 6.2.10 Commit 메시지

권장 기본 형식:

{유형}: {변경 요약}

{변경 이유와 주요 내용}

Refs: {요구사항·결함 ID}
ServiceId: {관련 ServiceId}

예:

feat: 고객 종합정보 조회 거래 추가

\- 고객번호 필수 검증 추가
\- 고객요약 조회 Mapper와 Service 구현
\- 조회 Timeout 및 오류 테스트 추가

Refs: REQ-SV-014
ServiceId: SV.Customer.selectSummary

## 6.2.11 Commit 유형 예

| 유형 | 의미 |
| --- | --- |
| feat | 신규 기능 |
| fix | 결함 수정 |
| refactor | 기능 변경 없는 구조 개선 |
| test | 테스트 추가·수정 |
| docs | 문서 변경 |
| build | Build·Dependency |
| ci | Pipeline 변경 |
| chore | 도구·정리 |
| perf | 성능 개선 |
| security | 보안 개선 |
| revert | 이전 Commit 취소 |

프로젝트 표준 유형이 있다면 해당 기준을 우선한다.

## 6.2.12 좋지 않은 Commit 메시지

수정
테스트
작업
최종
다시 수정
오류 수정
커밋
0720

이 메시지로는 어떤 거래가 왜 변경되었는지 알 수 없다.

## 6.2.13 Commit 작성자 설정

git config user.name
git config user.email

설정:

git config --global user.name "홍길동"
git config --global user.email "hong@example.com"

사내 정책에 맞는 이름과 이메일을 사용한다.

공용 서버 계정이나 타인의 계정으로 Commit하지 않는다.

## 6.2.14 Push

최초 Push:

git push -u origin feature/REQ-SV-014-customer-summary

이후:

git push

Push 전에 확인:

Branch가 맞는가?
Commit이 모두 포함되었는가?
비밀정보가 없는가?
Build와 Test가 성공했는가?
원격 최신 변경을 반영했는가?

## 6.2.15 Pull Request의 목적

Pull Request는 Merge 버튼을 누르기 위한 형식적인 화면이 아니다.

다음 세 가지를 수행한다.

1\. 변경 이유를 설명한다.
2\. 다른 사람이 변경을 검증한다.
3\. 자동검증과 승인 증적을 남긴다.

## 6.2.16 Pull Request 필수 내용

| 항목 | 작성 내용 |
| --- | --- |
| 제목 | 변경 목적을 한 문장으로 표현 |
| 요구사항 | 요구사항·결함·사고 ID |
| 업무범위 | 업무코드·도메인 |
| 화면 | 화면 ID·이벤트 ID |
| 거래 | ServiceId·거래코드 |
| 변경 프로그램 | 주요 클래스·Mapper |
| 데이터 | 테이블·컬럼·SQL 영향 |
| 설정 | YML·환경변수·OM 등록 |
| 보안 | 권한·개인정보·감사 영향 |
| 테스트 | 단위·통합·오류·성능 결과 |
| 호환성 | 기존 소비자 영향 |
| 배포 | 배포 순서·선행조건 |
| 롤백 | 되돌리는 방법 |
| 위험 | 미해결 사항·잔여 위험 |

## 6.2.17 Pull Request 예시

제목
\[SV\] 고객 종합정보 조회 거래 추가

요구사항
REQ-SV-014

화면·거래
\- 화면 ID: SV-CUS-0001
\- 이벤트 ID: SV-CUS-0001-E01
\- ServiceId: SV.Customer.selectSummary
\- 거래코드: SV-INQ-0001

주요 변경
\- SvCustomerHandler에 ServiceId 등록
\- SvCustomerFacade 조회 트랜잭션 추가
\- SvCustomerService 고객요약 조립
\- SvCustomerMapper.selectSummary 추가
\- OM Service Catalog·Timeout 등록자료 추가

영향 범위
\- sv-service
\- SV\_CUSTOMER\_SUMMARY 조회
\- 고객정보 조회권한
\- 감사로그 조회대상

테스트
\- 정상 고객 조회 PASS
\- 미존재 고객 업무오류 PASS
\- 권한 없음 PASS
\- SQL Timeout PASS
\- 회귀테스트 PASS

롤백
\- WAR 이전 버전 재배포
\- 신규 Service Catalog 비활성
\- DB 변경 없음

## 6.2.18 리뷰어가 확인할 내용

| 관점 | 확인 |
| --- | --- |
| 기능 | 요구사항을 충족하는가 |
| 구조 | Handler→Facade→Service→DAO→Mapper 준수 |
| 트랜잭션 | 경계와 Rollback이 적절한가 |
| 오류 | 업무·시스템·Timeout 구분 |
| 보안 | 권한·입력검증·민감정보 |
| 데이터 | SQL·Index·영향 행 |
| 운영 | ServiceId·GUID·TraceId·로그 |
| 설정 | 환경별 값·기본값 |
| 호환성 | 기존 호출자 영향 |
| 시험 | 정상·경계·실패 검증 |
| 배포 | 순서와 롤백 가능성 |

## 6.2.19 승인되지 않은 직접 Push

다음 Branch는 보호하는 것이 일반적이다.

main
develop
release/\*

권장 통제:

직접 Push 금지
Pull Request 필수
최소 승인자 수
CI 성공 필수
미해결 대화 금지
Force Push 금지
Branch 삭제 정책

## 6.2.20 Merge 방식

| 방식 | 특징 | 주의 |
| --- | --- | --- |
| Merge Commit | Branch 이력 보존 | 이력이 복잡할 수 있음 |
| Squash Merge | 여러 Commit을 하나로 통합 | 상세 Commit 이력 축약 |
| Rebase Merge | 선형 이력 | Commit Hash 변경 |
| Fast-forward | 분기 없이 포인터 이동 | 조건 충족 시만 가능 |

프로젝트는 하나의 기준을 정하고 일관되게 적용해야 한다.

# 6.3 충돌을 줄이는 작업 습관

## 6.3.1 충돌이란 무엇인가

Git 충돌은 두 변경을 자동으로 합칠 수 없을 때 발생한다.

예:

개발자 A
Timeout 3초 → 5초

개발자 B
Timeout 3초 → 2초

같은 줄을 서로 다르게 변경했기 때문에 Git은 어떤 값이 맞는지 결정할 수 없다.

## 6.3.2 충돌은 오류가 아니다

충돌은 Git 고장이 아니다.

서로 다른 의도를 가진 변경이
같은 위치에 존재하므로
사람의 판단이 필요하다는 신호다.

문제는 충돌 자체보다 내용을 이해하지 않고 임의로 한쪽을 선택하는 것이다.

## 6.3.3 충돌을 줄이는 기본 습관

| 습관 | 효과 |
| --- | --- |
| 작은 Branch | 변경 범위 축소 |
| 짧은 작업기간 | 기준 Branch와 차이 축소 |
| 자주 Fetch | 원격 변경 조기 인지 |
| 작은 Commit | 충돌 원인 분리 |
| 소유 파일 협의 | 동일 파일 동시 변경 감소 |
| 포맷 변경 분리 | 불필요한 충돌 방지 |
| 생성파일 제외 | 의미 없는 충돌 제거 |
| 설계 선확정 | 같은 문제를 다른 방식으로 구현하는 상황 방지 |
| 공용 설정 변경 공유 | 전체 개발자 영향 사전 인지 |
| DB 변경 조율 | SQL·Mapper·DDL 충돌 방지 |

## 6.3.4 작업 시작 전 최신화

git switch develop
git fetch origin
git pull --ff-only
git switch -c feature/REQ-SV-014-customer-summary

이미 Branch가 있다면:

git switch feature/REQ-SV-014-customer-summary
git fetch origin

기준 Branch와 차이를 확인한다.

git log --oneline --graph --decorate --all -20

## 6.3.5 자주 Commit하되 무의미하게 쪼개지 않는다

좋은 중간 Commit:

DTO와 Validation 구현
Service 업무 흐름 구현
Mapper·SQL 구현
테스트 추가

좋지 않은 중간 Commit:

저장
다시 저장
컴파일
오타
퇴근 전

Commit은 검토 가능한 의미를 가져야 한다.

## 6.3.6 포맷팅 변경 분리

다음 작업은 기능 변경과 분리한다.

전체 Import 정리
전체 코드 포맷
줄바꿈 변경
패키지 일괄 이동
파일명 일괄 변경
인코딩 변환

기능 PR에 전체 포맷 변경이 포함되면 실제 업무 변경을 찾기 어렵다.

## 6.3.7 Merge와 Rebase

### Merge

git merge origin/develop

내 Branch 이력
\+ develop 변경
→ Merge Commit

장점:

-   기존 Commit Hash 유지
-   이력의 실제 분기구조 보존
-   공유 Branch에서 상대적으로 안전

단점:

-   Merge Commit 증가
-   이력 복잡 가능

### Rebase

git rebase origin/develop

내 Commit을
최신 develop 뒤로 다시 적용

장점:

-   선형 이력
-   PR 검토가 단순할 수 있음

단점:

-   Commit Hash 변경
-   공유된 Commit을 Rebase하면 다른 개발자 이력과 충돌

### 핵심 원칙

혼자 사용하는 기능 Branch
→ Rebase 가능

여러 사람이 공유한 Branch
→ 임의 Rebase·Force Push 금지

## 6.3.8 Rebase 전 안전점검

git status

Working Tree가 깨끗해야 한다.

필요한 경우 Commit하거나 임시 저장한다.

git stash push -m "WIP before rebase"

그 후:

git fetch origin
git rebase origin/develop

## 6.3.9 충돌 발생 시 상태 확인

git status

충돌 파일에는 다음 표시가 생길 수 있다.

<<<<<<< HEAD
현재 기준 Branch 내용
\=======
적용하려는 Commit 내용
\>>>>>>> commit-id

이 표시를 단순 삭제하는 것이 아니라 최종적으로 어떤 코드가 맞는지 결정해야 한다.

## 6.3.10 충돌 해결 절차

1\. 충돌 파일 목록 확인
2\. 양쪽 변경 목적 확인
3\. 관련 요구사항·PR 확인
4\. 최종 코드 작성
5\. 충돌 표식 제거 확인
6\. Compile·Test
7\. git add
8\. Merge 또는 Rebase 계속
9\. 전체 회귀검증

### Merge 중

git add <해결한 파일>
git commit

### Rebase 중

git add <해결한 파일>
git rebase --continue

중단:

git rebase --abort

Merge 중단:

git merge --abort

## 6.3.11 충돌 해결 시 확인해야 할 의미

예를 들어 application.yml 충돌이 발생했다면 값만 합치는 것이 아니라 다음을 확인한다.

어느 환경 값인가?
Property 이름이 변경되었는가?
Default가 중복되는가?
Secret이 포함되었는가?
Config Binding 클래스와 일치하는가?
운영 설정도 바뀌어야 하는가?

Mapper XML 충돌:

SQL ID 중복 여부
ResultMap 정합성
동적 SQL 조건
Parameter 이름
Index 사용 가능성
트랜잭션 영향

Handler 충돌:

serviceIds() 등록 중복
switch 분기 누락
Facade 호출 일치
Request DTO 변환
오류 처리

## 6.3.12 컴파일 성공만으로 부족한 이유

충돌을 해결한 뒤 Build가 성공해도 업무 의미가 잘못될 수 있다.

예:

A 변경
고객 미존재 시 업무 오류

B 변경
고객 미존재 시 빈 응답

충돌 해결
둘 중 하나를 임의 선택

결과
컴파일 성공
업무 정책 위반

따라서 충돌 후에는 관련 요구사항과 테스트를 다시 확인한다.

## 6.3.13 Binary 파일 충돌

Word, Excel, 이미지, JAR, ZIP 같은 Binary는 줄 단위 Merge가 어렵다.

대응:

소유자 지정
동시 편집 사전 협의
문서 버전 분리
원본과 산출물 구분
변경 내용을 별도 기록
필요 시 문서관리 시스템 사용

Binary 문서를 Git으로 관리하더라도 Pull Request에서 변경 내용을 자동 비교하기 어렵다는 점을 고려해야 한다.

## 6.3.14 Stash 사용

임시 작업을 보관할 때:

git stash push -m "WIP customer summary"

목록:

git stash list

복원:

git stash pop

또는:

git stash apply stash@{0}

주의:

Stash는 장기 백업이 아니다.

Stash에 중요한 작업을 장기간 방치하지 않는다.

## 6.3.15 잘못된 파일 복구

Working Tree 변경 취소:

git restore <파일>

Staging 취소:

git restore --staged <파일>

특정 Commit 내용 확인:

git show <commit-id>:<파일경로>

삭제 파일 복원:

git restore <파일>

복구 전에 변경 내용을 반드시 확인한다.

## 6.3.16 Commit 취소 방식

### 공개되지 않은 최근 Commit 수정

git commit --amend

### 공유되지 않은 로컬 이력 재작성

git rebase -i

### 이미 공유된 Commit 취소

git revert <commit-id>

revert는 기존 Commit을 삭제하지 않고 반대 변경을 새 Commit으로 남긴다.

공유 Branch에서는 일반적으로 reset --hard와 Force Push보다 revert가 안전하다.

## 6.3.17 Force Push

다음 명령은 이력을 바꾼다.

git push --force

공유 Branch에서는 금지하는 것이 원칙이다.

개인 기능 Branch에서 불가피할 경우에도 다음을 우선 검토한다.

git push --force-with-lease

\--force-with-lease도 안전을 완전히 보장하지는 않는다. 프로젝트 정책과 협업자 상태를 확인해야 한다.

## 6.3.18 Cherry-pick

특정 Commit만 다른 Branch에 적용할 때 사용한다.

git cherry-pick <commit-id>

운영 Hotfix를 develop에도 반영하는 경우 등에 사용할 수 있다.

주의:

동일 변경이 다른 Hash로 중복될 수 있음
충돌 가능
후속 Merge에서 이력 혼란 가능

Hotfix 반영 절차를 프로젝트에서 표준화해야 한다.

## 6.3.19 충돌 예방을 위한 업무 분할

좋지 않은 작업 배분:

개발자 A: CustomerService 수정
개발자 B: CustomerService 수정
개발자 C: CustomerService 수정

개선 예:

개발자 A: 요청·응답 DTO와 Validation
개발자 B: Service·Rule
개발자 C: DAO·Mapper·SQL

그러나 프로그램 계층을 사람별로 나누면 통합 책임이 불명확할 수 있으므로 작은 유스케이스는 한 명이 End-to-End로 수행하는 것이 더 나을 수 있다.

핵심은 작업 전에 변경 파일과 인터페이스를 공유하는 것이다.

# 6.4 비밀정보와 생성파일 관리

## 6.4.1 저장소에 들어가면 안 되는 정보

다음 정보는 Git 저장소에 원문으로 Commit하면 안 된다.

사용자 비밀번호
DB 계정 비밀번호
Personal Access Token
JWT Access·Refresh Token
JWT Private Key
SSH Private Key
TLS Private Key
API Key
Client Secret
암호화 Master Key
운영 서버 주소 중 비공개 정보
개인정보 포함 테스트 데이터
운영 DB Dump
운영 로그 원문
실제 주민번호·계좌번호·전화번호

특히 JWT Private Key는 토큰을 위조할 수 있는 핵심 비밀이므로 application.yml, Git 저장소, WAR와 Build Image에 평문으로 포함하면 안 된다.

## 6.4.2 비밀정보와 설정정보 구분

| 정보 | Git 저장 가능 | 관리 방법 |
| --- | --- | --- |
| Property 이름 | O | application.yml |
| 기본 비민감값 | O | 설정파일 |
| 환경별 Endpoint 예시 | 조건부 | 보안 검토 |
| DB 비밀번호 | X | Secret Store |
| JWT Private Key | X | KMS·HSM·Vault |
| Public Key·JWKS URL | 일반적으로 O | 설정·인프라 기준 |
| 암호화된 Secret | 정책에 따름 | 복호화 키 별도 |
| 로컬 예제 값 | O | 가짜 값만 |
| 실제 사용자 데이터 | X | Masking·가상 데이터 |

## 6.4.3 환경설정 권장 구조

저장소:

spring:
datasource:
url: ${DB\_URL}
username: ${DB\_USERNAME}
password: ${DB\_PASSWORD}

tcf:
jwt:
jwks-uri: ${JWT\_JWKS\_URI}

실제 값:

환경변수
Secret Manager
Vault
KMS
배포도구의 보호변수
운영 설정 저장소

저장소에는 Property 구조만 남기고 Secret 원문은 외부에서 주입한다.

## 6.4.4 로컬 설정

권장 예:

application-local-example.yml
.env.example

예제:

spring:
datasource:
url: jdbc:oracle:thin:@localhost:1521/XEPDB1
username: sample\_user
password: change\_me

실제 개인 설정:

application-local-private.yml
.env

실제 파일은 .gitignore로 제외한다.

## 6.4.5 .gitignore

대표 예:

\# Gradle
.gradle/
build/
\*\*/build/

\# Java
\*.class
\*.jar
\*.war

\# IDE
.idea/
\*.iml
.classpath
.project
.settings/
.vscode/

\# OS
.DS\_Store
Thumbs.db

\# Logs
logs/
\*.log
gc.log\*
heapdump/
\*.hprof

\# Runtime
tmp/
temp/
work/
data/

\# Local configuration
.env
.env.\*
!.env.example
application-local-private.yml
application-secret.yml
secrets/

\# Keys and certificates
\*.key
\*.pem
\*.p12
\*.pfx
\*.jks
\*.keystore

\# Test reports
\*\*/test-results/
\*\*/reports/

\# Node
node\_modules/
dist/

프로젝트가 인증서 파일을 정상적으로 관리해야 하는 특별한 경우에는 무조건 확장자로 제외하지 않고 저장 대상과 비밀 대상의 경계를 별도로 정의한다.

## 6.4.6 이미 추적 중인 파일

.gitignore에 추가해도 이미 Git이 추적 중인 파일은 자동으로 제외되지 않는다.

추적 해제:

git rm --cached <파일>

디렉터리:

git rm -r --cached <디렉터리>

그 후 Commit한다.

주의:

현재 Commit에서 제거
≠ 과거 Commit 이력에서 제거

비밀정보가 과거 Commit에 들어갔다면 별도 사고대응이 필요하다.

## 6.4.7 비밀정보를 Commit했을 때

즉시 다음 순서로 대응한다.

1\. Push 여부 확인
2\. 해당 Secret 즉시 폐기·교체
3\. 보안·저장소 관리자에게 보고
4\. 영향 범위 확인
5\. Git 이력 정리 필요성 판단
6\. CI Cache·Artifact·Log 확인
7\. 관련 시스템 접근로그 확인
8\. 재발방지 검사 추가

가장 중요한 것은 Git 이력 삭제가 아니라 Secret 교체다.

Git에서 문자열 삭제
≠ 유출된 Secret이 안전해짐

이미 복제·Cache·로그에 남았을 가능성이 있으므로 비밀번호·Token·Key를 즉시 폐기한다.

## 6.4.8 Commit 전 비밀정보 검사

검색 예:

git grep -n -i "password"
git grep -n -i "secret"
git grep -n -i "private key"
git grep -n -i "access\_token"

하지만 단순 문자열 검색만으로 충분하지 않다.

CI에서는 승인된 Secret Scanner를 적용하는 것이 좋다.

검사 대상:

고정 패턴
고Entropy 문자열
Private Key Header
Cloud Access Key
Personal Access Token
JWT
DB Connection String
비밀번호 Property

## 6.4.9 JWT 원문 로그 금지

다음 로그는 금지한다.

log.info("authorization={}", authorizationHeader);
log.debug("jwt={}", accessToken);

대신 필요한 식별정보만 남긴다.

userId
jti
issuer
serviceId
guid
traceId
검증 결과
오류코드

Token 전체 값은 마스킹 후에도 불필요하다면 남기지 않는다.

## 6.4.10 생성파일이란 무엇인가

소스와 설정으로 다시 만들 수 있는 파일을 의미한다.

대표 예:

build/
.gradle/
\*.class
\*.jar
\*.war
test report
coverage report
generated source
IDE metadata
log
heap dump
temporary file
node\_modules
dist

## 6.4.11 생성파일을 Git에 넣지 않는 이유

| 문제 | 영향 |
| --- | --- |
| 저장소 크기 증가 | Clone·Fetch 지연 |
| 불필요한 Diff | 리뷰 품질 저하 |
| OS·JDK 차이 | 동일 소스인데 Binary 달라짐 |
| Merge 불가 | Binary 충돌 |
| 산출물 출처 불명 | 어느 Commit에서 빌드했는지 모름 |
| 보안 | 빌드 과정의 Secret 포함 가능 |
| 재현성 | 빌드 대신 Binary 복사에 의존 |

## 6.4.12 배포 산출물 관리

WAR와 JAR는 Git이 아니라 Artifact Repository에 보관하는 것이 적절하다.

Git Commit
↓
CI Build
↓
Test·품질 Gate
↓
WAR 생성
↓
Checksum·SBOM·서명
↓
Artifact Repository 저장
↓
배포 승인

Artifact 메타데이터:

| 항목 | 예 |
| --- | --- |
| Artifact | sv-service-1.4.2.war |
| Commit ID | 4f72a9c |
| Branch | release/1.4.2 |
| Build No | BUILD-20260718-0142 |
| JDK | 21 |
| Gradle | 8.x |
| Checksum | SHA-256 |
| 테스트 결과 | PASS |
| 승인 | 배포 승인 ID |

## 6.4.13 로그·Dump·테스트 데이터

다음은 Git에 Commit하지 않는다.

운영 애플리케이션 로그
거래 원문
SQL Parameter 원문
Thread Dump
Heap Dump
GC Log
DB Dump
개인정보 포함 CSV
운영 화면 캡처

필요한 장애 증적은 승인된 보안 저장소나 이슈 관리 시스템에 접근 통제를 적용해 보관한다.

소스 저장소에는 민감정보를 제거한 재현 절차와 요약만 남긴다.

## 6.4.14 데이터베이스 변경파일

DDL과 Migration Script는 생성파일이 아니라 변경 소스다.

따라서 저장소에서 관리할 수 있다.

예:

db/
├─ migration
│ ├─ V1.4.2\_001\_\_add\_customer\_index.sql
│ └─ V1.4.2\_002\_\_add\_customer\_column.sql
└─ rollback
├─ R1.4.2\_001\_\_drop\_customer\_index.sql
└─ R1.4.2\_002\_\_drop\_customer\_column.sql

관리 기준:

변경 목적
적용 순서
대상 스키마
사전조건
Rollback
예상 Lock
데이터 변환
검증 SQL
승인 정보

실제 운영 데이터나 DB Dump는 포함하지 않는다.

## 6.4.15 생성 코드

OpenAPI, Query DSL 또는 기타 도구가 생성하는 코드의 Commit 여부는 프로젝트 정책으로 결정한다.

| 방식 | 장점 | 주의 |
| --- | --- | --- |
| 생성 코드 Commit | 소비자가 생성도구 없이 Build 가능 | 대량 Diff·충돌 |
| Build 시 생성 | 단일 원천 유지 | Build Tool·Version 필수 |
| Artifact 제공 | 소비 편리 | Source 추적성 필요 |

결정 시 다음을 기록한다.

생성 원천
생성 도구 버전
재생성 명령
수동 수정 금지 여부
Commit 대상
검증 방식

# Git 작업 책임 경계

| 활동 | 개발자 | 리뷰어 | 아키텍처 | DevOps | 보안 |
| --- | --- | --- | --- | --- | --- |
| 저장소 Clone | R | I | I | C | I |
| Branch 생성 | R | I | I | C | I |
| Commit 작성 | R/A | I | I | I | I |
| 기능 리뷰 | C | R/A | C | I | C |
| 구조 검토 | C | C | R/A | I | I |
| CI 구성 | C | I | C | R/A | C |
| Secret Scan | R | C | I | R | A |
| Branch 보호 | I | I | C | R | C |
| Merge 승인 | C | R | C | C | 조건부 |
| Artifact 생성 | I | I | I | R/A | I |
| 배포 이력 | I | I | C | R/A | I |
| 사고 대응 | R | C | C | R | A |

# 정상 Git 작업 흐름

요구사항 확인
↓
화면·ServiceId·영향 범위 식별
↓
develop 최신화
↓
기능 Branch 생성
↓
작은 논리 단위 개발
↓
Commit 전 Diff·Secret 확인
↓
Clean Build·Test
↓
Commit
↓
원격 Branch Push
↓
Pull Request 작성
↓
CI·Secret Scan·구조검증
↓
코드리뷰
↓
수정·재검증
↓
승인·Merge
↓
Artifact 생성
↓
배포·운영 추적정보 연결

# 오류 흐름

## 잘못된 Branch에서 개발

main에서 직접 수정
↓
Commit 전 발견
↓
기능 Branch 생성
↓
변경 이동
↓
main Working Tree 정리

변경이 아직 Commit되지 않았다면 현재 변경을 유지한 채 새 Branch를 만들 수 있다.

git switch -c feature/REQ-SV-014-customer-summary

## 기준 Branch가 오래된 경우

오래된 develop에서 Branch 생성
↓
장기간 개발
↓
PR 시 대규모 충돌
↓
기준 변경 분석
↓
Merge·Rebase
↓
전체 회귀테스트

예방:

짧은 개발주기
자주 Fetch
작은 PR
공용 인터페이스 변경 사전 공유

## 비밀정보 Push

DB 비밀번호 Commit·Push
↓
비밀번호 즉시 교체
↓
보안사고 등록
↓
저장소·CI·Artifact 영향 조사
↓
이력 정리
↓
Secret Scan 강화

## 생성파일 대량 Commit

build·log·IDE 파일 Staging
↓
Commit 전 git status 확인
↓
Staging 취소
↓
.gitignore 보완
↓
필요 파일만 재등록

## Force Push로 다른 작업 삭제

공유 Branch Rebase
↓
Force Push
↓
다른 개발자 Commit 유실
↓
Reflog·Remote 이력 확인
↓
복구 Branch 생성
↓
작업 복원
↓
Branch 보호 강화

# 정상 예시

Branch
feature/REQ-SV-014-customer-summary

Commit
feat: 고객 종합정보 조회 거래 추가

관련 식별자
REQ-SV-014
SV-CUS-0001-E01
SV.Customer.selectSummary
SV-INQ-0001

변경파일
SvCustomerHandler.java
SvCustomerFacade.java
SvCustomerService.java
SvCustomerMapper.java
SvCustomerMapper.xml
CustomerSummaryRequest.java
CustomerSummaryResponse.java
CustomerSummaryTest.java

검증
clean build PASS
unit test PASS
integration test PASS
secret scan PASS
architecture test PASS

PR
영향 범위·테스트·롤백 기록
리뷰 2명 승인
CI 성공 후 Merge

# 금지 예시

main에 직접 Push한다.

Branch 이름을 test, final, new로 만든다.

Commit 메시지를 “수정”으로 작성한다.

업무 변경과 전체 포맷팅을 한 Commit에 섞는다.

충돌 내용을 이해하지 않고 한쪽 파일 전체를 선택한다.

공유 Branch를 Rebase한 뒤 Force Push한다.

DB 비밀번호를 application.yml에 기록한다.

JWT Private Key를 Git에 Commit한다.

운영 로그와 개인정보 CSV를 저장소에 올린다.

build 디렉터리와 WAR를 매번 Commit한다.

IDE Build 성공만 확인하고 PR을 올린다.

요구사항·ServiceId·테스트 결과 없이 Merge를 요청한다.

# 자동검증 및 품질 Gate

## 1\. Commit 전 검사

Git 상태
Diff
줄바꿈
Build
Test
Secret
개인정보
생성파일

대표 명령:

git status
git diff
git diff --staged
git diff --check

## 2\. Pre-commit Hook

검사 후보:

Trailing Whitespace
BOM
금지 확장자
대용량 파일
Private Key Pattern
Token Pattern
컴파일
단위 테스트
포맷 검사

Hook은 개발자 편의를 돕지만 중앙 CI 검증을 대체하지 않는다.

로컬 Hook
\= 빠른 사전 피드백

중앙 CI
\= 공식 품질 Gate

## 3\. Branch 이름 검사

정규식 예:

^(feature|fix|hotfix|release|refactor|chore)/\[A-Za-z0-9.\_-\]+$

프로젝트 요구사항 ID를 포함하도록 강화할 수 있다.

## 4\. Commit 메시지 검사

예:

^(feat|fix|refactor|test|docs|build|ci|chore|perf|security|revert): .+

Commit 메시지 자동검사는 형식만 확인한다.

변경 이유의 적절성은 리뷰어가 판단해야 한다.

## 5\. Pull Request Gate

다음 조건을 만족해야 Merge할 수 있도록 구성한다.

| Gate | 기준 |
| --- | --- |
| Branch | 보호 Branch 직접 Push 금지 |
| Build | 전체 또는 영향 모듈 Build 성공 |
| Test | 단위·통합 테스트 성공 |
| 구조 | ArchUnit·의존성 검사 성공 |
| 보안 | Secret·취약점 검사 성공 |
| 품질 | 정적분석 기준 충족 |
| 추적성 | 요구사항·ServiceId 기재 |
| 리뷰 | 필수 승인자 수 충족 |
| 대화 | 미해결 리뷰 없음 |
| DB | DDL·Rollback 검토 완료 |
| 설정 | 환경별 변경 승인 |
| 운영 | 로그·모니터링 영향 검토 |
| 롤백 | 복구 절차 기록 |

## 6\. NSIGHT 추적성 검사

자동검증 후보:

ServiceId가 Handler에 등록되었는가?
중복 ServiceId가 없는가?
OM Catalog 등록자료가 존재하는가?
거래코드가 정의되었는가?
화면 이벤트와 ServiceId가 연결되었는가?
Mapper Namespace가 Java Mapper와 일치하는가?
테스트 ID가 ServiceId와 연결되는가?
변경된 설정의 기본값이 정의되었는가?

# 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| GIT-001 | 공식 저장소 정상 Clone | 전체 이력과 Remote 생성 |
| GIT-002 | 잘못된 권한으로 Clone | 명확한 인증 오류 |
| GIT-003 | SSH Key 미등록 | 연결 실패 및 조치 가능 |
| GIT-004 | Clone 후 git status | Working Tree Clean |
| GIT-005 | 올바른 develop Branch | 원격 추적 정상 |
| GIT-006 | Clone 직후 Clean Build | 성공 |
| GIT-007 | 기능 Branch 생성 | 기준 Branch에서 정상 분기 |
| GIT-008 | 요구사항 없는 Branch 이름 | CI 또는 정책 검사 실패 |
| GIT-009 | 하나의 기능 Commit | 리뷰 가능한 Diff |
| GIT-010 | 포맷과 기능 혼합 | 리뷰에서 수정 요청 |
| GIT-011 | Commit 메시지 규칙 위반 | Gate 실패 |
| GIT-012 | PR 필수항목 누락 | Merge 차단 |
| GIT-013 | 최신 develop 반영 | 회귀테스트 성공 |
| GIT-014 | 동일 Java 파일 충돌 | 의미 기반 해결 |
| GIT-015 | Mapper XML 충돌 | SQL·Namespace 검증 |
| GIT-016 | YAML 충돌 | 환경·Secret 검증 |
| GIT-017 | Rebase 중 충돌 | 해결 후 이력 정상 |
| GIT-018 | Rebase 중 오류 | Abort 후 원상복구 |
| GIT-019 | 공유 Branch Force Push | 권한 차단 |
| GIT-020 | 비밀번호 Commit | Secret Scan 실패 |
| GIT-021 | Private Key Commit | 즉시 차단 |
| GIT-022 | Build 디렉터리 Staging | .gitignore로 제외 |
| GIT-023 | 운영 로그 Commit | 보안 Gate 실패 |
| GIT-024 | 대용량 Binary 추가 | 크기 검사 실패 |
| GIT-025 | Tag와 Artifact 연결 | Commit·Build 추적 가능 |
| GIT-026 | Commit Revert | 안전한 취소 Commit 생성 |
| GIT-027 | Hotfix Cherry-pick | 대상 Branch 검증 성공 |
| GIT-028 | Clone 후 Submodule 미초기화 | 사전검사 탐지 |
| GIT-029 | Git LFS 미설치 | 대용량 파일 오류 탐지 |
| GIT-030 | PR Merge 후 Branch 삭제 | 원격 정리 정상 |

# 따라 하는 실무 절차

## 1단계. 공식 저장소와 Branch를 확인한다

기록:

| 항목 | 값 |
| --- | --- |
| 저장소 | 공식 Repository |
| Clone 방식 | SSH·HTTPS |
| 기본 Branch | main |
| 개발 기준 Branch | develop |
| Branch 정책 | 프로젝트 기준 |
| 리뷰 정책 | 승인자 수 |
| CI | 필수 Pipeline |
| Artifact 저장소 | 프로젝트 기준 |

## 2단계. 안전한 경로에 Clone한다

cd C:/workspace
git clone <공식 저장소 주소>
cd nsight-tcf-framework

## 3단계. 최초 상태를 확인한다

git status
git remote -v
git branch -a
git log --oneline --decorate -10
git rev-parse --short HEAD

## 4단계. 프로젝트 구조를 확인한다

settings.gradle
build.gradle
gradle.properties
.gitignore
.gitattributes
.editorconfig
README
CI Pipeline
업무 모듈

## 5단계. 기준 Branch를 빌드한다

gradle clean build

기록:

Branch
Commit ID
JDK
Gradle
Build 결과
Test 결과

## 6단계. 기능 Branch를 만든다

git switch develop
git fetch origin
git pull --ff-only
git switch -c feature/REQ-SV-014-customer-summary

## 7단계. 변경 범위를 먼저 작성한다

요구사항
화면·이벤트
ServiceId
거래코드
변경 모듈
변경 클래스
SQL·DB 영향
설정 영향
운영 영향
테스트 범위

## 8단계. 개발하고 Diff를 검토한다

git status
git diff
git diff --check

필요 파일만 Staging한다.

git add -p

## 9단계. Build와 Test를 수행한다

gradle clean :sv-service:build

또는 전체 영향이 크다면:

gradle clean build

## 10단계. Commit한다

git commit

예:

feat: 고객 종합정보 조회 거래 추가

Refs: REQ-SV-014
ServiceId: SV.Customer.selectSummary

## 11단계. Push하고 Pull Request를 만든다

git push -u origin feature/REQ-SV-014-customer-summary

PR에 다음을 포함한다.

변경 이유
요구사항·ServiceId
영향 범위
테스트 결과
보안·데이터 영향
배포·롤백
잔여 위험

## 12단계. 리뷰를 반영한다

리뷰 지적사항을 이해하고 코드와 문서를 함께 수정한다.

단순히 “수정 완료”라고 답하지 않는다.

어떤 지적을
어떻게 수정했고
어떤 테스트로 확인했는지

기록한다.

## 13단계. Merge 후 확인한다

CI 성공
Merge Commit·Squash Commit 확인
develop 반영 확인
기능 Branch 삭제
Artifact 생성 여부 확인
배포 대상 Version 확인

# 완료 체크리스트

## Clone·최초 점검

| 확인 항목 | 완료 |
| --- | --- |
| 공식 저장소 주소를 확인했다. | □ |
| SSH·HTTPS 인증방식을 확인했다. | □ |
| 안전한 경로에 Clone했다. | □ |
| Remote 주소를 확인했다. | □ |
| 현재 Branch를 확인했다. | □ |
| Commit ID를 기록했다. | □ |
| Working Tree가 Clean하다. | □ |
| Submodule·LFS 적용 여부를 확인했다. | □ |
| 저장소 Root를 확인했다. | □ |
| Clone 직후 Clean Build를 수행했다. | □ |

## Branch·Commit

| 확인 항목 | 완료 |
| --- | --- |
| 기준 Branch를 최신화했다. | □ |
| 승인된 이름으로 기능 Branch를 만들었다. | □ |
| 한 Commit에 하나의 논리적 목적을 담았다. | □ |
| Commit 전에 Diff를 직접 확인했다. | □ |
| 생성파일과 비밀정보를 제외했다. | □ |
| 요구사항 ID를 Commit과 연결했다. | □ |
| 관련 ServiceId를 기록했다. | □ |
| 의미 있는 Commit 메시지를 작성했다. | □ |
| Build 가능한 상태로 Commit했다. | □ |
| 공유 Branch 이력을 임의로 변경하지 않았다. | □ |

## Pull Request

| 확인 항목 | 완료 |
| --- | --- |
| PR 제목이 변경 목적을 설명한다. | □ |
| 요구사항·결함 ID를 기록했다. | □ |
| 화면·ServiceId·거래코드를 기록했다. | □ |
| 변경 프로그램과 DB 영향을 기록했다. | □ |
| 설정·보안·운영 영향을 기록했다. | □ |
| 정상·경계·실패 테스트 결과를 기록했다. | □ |
| 배포 순서와 Rollback을 기록했다. | □ |
| 필수 CI가 성공했다. | □ |
| 리뷰 지적사항을 모두 처리했다. | □ |
| 승인 후 Merge했다. | □ |

## 충돌

| 확인 항목 | 완료 |
| --- | --- |
| 작업 시작 전에 기준 Branch를 최신화했다. | □ |
| 작업 Branch를 장기간 방치하지 않았다. | □ |
| 포맷 변경을 기능 변경과 분리했다. | □ |
| 충돌 양쪽의 변경 목적을 확인했다. | □ |
| 충돌 표식을 모두 제거했다. | □ |
| 충돌 후 Compile·Test를 수행했다. | □ |
| 공유 Branch를 임의 Rebase하지 않았다. | □ |
| Force Push 정책을 준수했다. | □ |
| Binary 파일 동시편집을 조율했다. | □ |
| 충돌 해결 결과를 PR에 기록했다. | □ |

## 보안·생성파일

| 확인 항목 | 완료 |
| --- | --- |
| 비밀번호·Token이 저장소에 없다. | □ |
| JWT·SSH Private Key가 없다. | □ |
| 실제 개인정보 테스트 데이터가 없다. | □ |
| 로컬 비밀 설정이 .gitignore에 있다. | □ |
| Build·Log·Dump가 제외되어 있다. | □ |
| IDE 개인파일이 제외되어 있다. | □ |
| Secret Scan을 통과했다. | □ |
| 비밀정보 유출 대응절차를 알고 있다. | □ |
| WAR·JAR는 Artifact Repository로 관리한다. | □ |
| Artifact와 Commit ID가 연결된다. | □ |

# 변경·호환성·폐기 관리

## Branch 정책 변경

Branch 전략을 변경할 때 다음을 함께 검토한다.

보호 Branch
직접 Push 권한
Merge 방식
리뷰 승인자
CI 필수조건
Release Tag
Hotfix 흐름
Branch 삭제
장기 Branch 처리

## 저장소 분리·통합

멀티모듈 저장소를 여러 저장소로 분리하거나 반대로 통합할 때는 다음 영향이 있다.

| 영역 | 검토 |
| --- | --- |
| Gradle | Project Dependency |
| 공통 모듈 | Version 배포 방식 |
| CI | Pipeline 분리·연결 |
| Tag | 릴리스 기준 |
| 이력 | Commit 보존 |
| Issue·PR | 참조 연결 |
| 권한 | 저장소별 접근 |
| Artifact | Version 정책 |
| 배포 | 모듈 간 호환 |
| 운영 | Commit 추적 |

## 파일 폐기

더 이상 사용하지 않는 설정과 코드는 단순 삭제하지 않는다.

사용처 검색
↓
Runtime 참조 확인
↓
영향 ServiceId 확인
↓
대체 경로 준비
↓
사용중단 공지
↓
삭제
↓
Build·회귀테스트
↓
문서·OM 갱신

## Tag와 Release 관리

권장 예:

v1.4.2
sv-service-v1.4.2
release-2026.07.18

Tag 기준은 프로젝트가 결정한다.

Tag에는 다음 정보가 연결되어야 한다.

Commit ID
Release Note
Artifact
배포승인
DB Script
설정 변경
Rollback Version

# 제6장의 핵심 정리

첫째,
Git Clone은 파일 복사가 아니라
이력과 Branch를 포함한 저장소 복제다.

둘째,
Clone 직후 소스를 수정하기 전에
Branch·Remote·Commit·Build 상태를 기록한다.

셋째,
Branch 이름에는 변경 목적과
요구사항을 식별할 수 있는 정보를 담는다.

넷째,
Commit은 하나의 논리적 변경과
복구 가능한 단위여야 한다.

다섯째,
Pull Request에는 코드뿐 아니라
영향 범위·테스트·배포·롤백을 기록한다.

여섯째,
충돌은 한쪽 파일을 선택하는 작업이 아니라
두 변경의 업무 의미를 통합하는 작업이다.

일곱째,
공유된 Branch의 이력을
Rebase·Force Push로 임의 변경하지 않는다.

여덟째,
비밀번호·Token·Private Key와
실제 개인정보는 Git에 저장하지 않는다.

아홉째,
Build·Log·WAR 같은 생성파일은
Git이 아니라 적절한 저장소에서 관리한다.

열째,
Commit ID는 ServiceId·Artifact·배포·운영로그와
연결되어야 한다.

# 시사점

## 핵심 아키텍처 판단

Git은 개발자 도구이면서 동시에 아키텍처 변경관리 도구다.

아키텍처 원칙
↓
패키지·의존성
↓
소스 변경
↓
Pull Request 검증
↓
Artifact
↓
배포
↓
운영 결과

TCF 계층을 우회하거나 업무 WAR 간 직접 의존을 추가한 변경은 Git Pull Request 단계에서 발견되어야 한다.

운영 이후에 발견하면 이미 배포·데이터·장애 범위가 확대된 상태다.

## 주요 위험

| 위험 | 영향 |
| --- | --- |
| 잘못된 Branch 개발 | 배포 대상 혼란 |
| Clone 직후 미검증 | 기존 결함과 신규 결함 혼동 |
| 대형 Commit | 리뷰·복구 곤란 |
| 의미 없는 메시지 | 변경 이유 추적 불가 |
| 직접 Push | 검증·승인 우회 |
| 장기 기능 Branch | 충돌·통합위험 증가 |
| 공유 Branch Rebase | Commit 유실 |
| 비밀정보 Commit | 인증·보안 사고 |
| 생성파일 Commit | 저장소 비대화 |
| 운영 로그 Commit | 개인정보·보안 침해 |
| Commit과 Artifact 미연결 | 장애 버전 식별 불가 |
| PR 추적정보 누락 | 요구사항·ServiceId 영향분석 불가 |

## 우선 보완 과제

1.  공식 저장소와 Branch 전략을 기준문서로 확정한다.
2.  main·develop에 Branch 보호 정책을 적용한다.
3.  Branch와 Commit 메시지 규칙을 자동검증한다.
4.  Pull Request 표준 템플릿을 제공한다.
5.  PR에 요구사항·화면·ServiceId·거래코드를 필수화한다.
6.  Secret Scan을 Commit과 CI에 적용한다.
7.  .gitignore, .gitattributes, .editorconfig를 기준화한다.
8.  Binary·WAR·로그의 저장 위치를 분리한다.
9.  Artifact에 Commit ID와 Build 번호를 삽입한다.
10.  운영 배포이력에서 Commit·PR·Artifact를 조회할 수 있게 한다.
11.  Hotfix와 Revert 절차를 표준화한다.
12.  Force Push 권한을 최소화한다.

## 중장기 발전 방향

개인별 Git 사용
↓
Branch·Commit 표준
↓
Pull Request 템플릿
↓
자동 Build·Test
↓
Secret·의존성·구조 검사
↓
ServiceId 추적성 검사
↓
Artifact 서명·SBOM
↓
Commit부터 운영까지 공급망 추적

# 마무리말

Git을 잘 사용한다는 것은 명령을 많이 외우는 것이 아니다.

다음 상태를 유지하는 것이 중요하다.

현재 어느 Branch에 있는가?

무엇을 변경했는가?

왜 변경했는가?

어느 요구사항과 ServiceId인가?

무엇을 테스트했는가?

누가 검토했는가?

어느 Artifact로 배포되었는가?

문제가 생기면 어떻게 복구하는가?

제6장에서 가장 자주 사용할 명령은 다음과 같다.

git status
git remote -v
git branch -a
git log --oneline --decorate
git fetch
git switch
git diff
git add
git commit
git push

그러나 명령보다 더 중요한 것은 변경의 품질이다.

작은 변경
\+ 명확한 이유
\+ 깨끗한 Commit
\+ 재현 가능한 테스트
\+ 검토 가능한 PR
\+ 안전한 비밀정보 관리
\= 운영 가능한 Git 이력

다음 장에서는 Clone한 NSIGHT TCF 멀티모듈 프로젝트를 Gradle로 분석하고, 전체 Build와 특정 업무 모듈 Build, 의존성 충돌과 WAR 산출물 확인 방법을 학습한다.
