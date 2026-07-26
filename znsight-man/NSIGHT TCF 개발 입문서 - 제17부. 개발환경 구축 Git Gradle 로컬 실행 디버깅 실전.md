
## 초보 IT 개발자를 위한 NSIGHT TCF 개발 입문서

## 제17부. 개발환경 구축·Git·Gradle·로컬 실행·디버깅 실전

## 1. 도입 전 안내말

앞의 제1부부터 제16부까지는 NSIGHT TCF의 구조, 개발표준, 업무 구현, 테스트, 운영, 아키텍처와 산출물 관리방법을 배웠습니다.

이제 개발자가 실제 소스를 내려받아 실행해야 합니다.

처음 프로젝트에 참여한 개발자는 보통 다음과 같은 어려움을 겪습니다.

```
어느 JDK를 설치해야 하는가?

Git 저장소에서 어느 Branch를 받아야 하는가?

Gradle을 별도로 설치해야 하는가?

전체 프로젝트를 한 번에 Build해야 하는가?

Eclipse에는 어떤 방식으로 Import해야 하는가?

tcf-core와 업무 WAR 중 무엇을 실행해야 하는가?

application.yml은 어디에 작성해야 하는가?

운영 DB 정보를 로컬 환경에 그대로 넣어도 되는가?

bootRun과 외부 Tomcat 실행은 무엇이 다른가?

WAR를 Tomcat에 배포했는데 왜 404가 발생하는가?

ServiceId가 어느 Handler로 연결되는지 어떻게 확인하는가?

Mapper XML의 SQL까지 중단점을 걸 수 있는가?

실행은 되지만 요청이 계속 인증 오류로 실패하는 이유는 무엇인가?
```

개발환경 구축은 프로그램을 실행하기 위한 단순 준비작업처럼 보일 수 있습니다.

그러나 개발환경이 표준화되지 않으면 다음 문제가 발생합니다.

```
개발자마다 JDK Version이 다르다.

IDE에서는 성공하지만 CI에서는 Build가 실패한다.

개발자가 임의로 Gradle Version을 설치한다.

운영 Secret이 개인 PC에 저장된다.

로컬 설정이 Git에 Commit된다.

업무 WAR마다 실행방법이 다르다.

Mapper XML이 Build 결과에 포함되지 않는다.

로컬에서는 Session을 사용하지만 운영에서는 JWT를 사용한다.

디버깅을 위해 공통 Framework를 임의로 수정한다.

환경문제를 업무 프로그램 오류로 잘못 판단한다.
```

NSIGHT 개발환경은 다음 원칙으로 구축합니다.

```
JDK와 Gradle Version은 프로젝트 기준을 따른다.

Gradle Wrapper를 사용한다.

소스 변경은 개인 Branch에서 수행한다.

로컬 Secret과 공통 설정을 분리한다.

업무 WAR는 공통 TCF Pipeline을 통과해 실행한다.

로컬 실행과 운영 배포의 차이를 명확히 구분한다.

디버깅은 ServiceId 실행경로를 따라 단계적으로 수행한다.

개발 PC와 CI의 Build 조건을 최대한 동일하게 유지한다.
```

개발자가 최종적으로 확인해야 할 실행 흐름은 다음과 같습니다.

```
Git Clone

→ Gradle Build

→ IDE Import

→ Local Profile 설정

→ 업무 WAR 실행

→ POST /{업무코드}/online

→ OnlineTransactionController

→ TCF.process()

→ STF

→ Dispatcher

→ Handler

→ Facade

→ Service

→ Rule

→ DAO

→ Mapper

→ DB

→ ETF

→ StandardResponse
```

## 2. 제17부 개요

### 2.1 목적

제17부의 목적은 초보 개발자가 NSIGHT TCF 프로젝트를 자신의 PC에 구성하고, 공통모듈과 업무모듈을 Build하며, 대표 ServiceId를 실행·디버깅할 수 있도록 하는 것입니다.

이 부를 학습한 뒤에는 다음 작업을 수행할 수 있어야 합니다.

- 개발 PC에 필요한 도구를 설치한다.
- JDK와 환경변수를 올바르게 설정한다.
- Git 저장소를 Clone하고 Branch를 생성한다.
- Gradle Wrapper로 멀티모듈 프로젝트를 Build한다.
- Eclipse 또는 IntelliJ에 Gradle 프로젝트를 등록한다.
- 공통모듈과 업무 WAR의 관계를 이해한다.
- 로컬 Profile과 Secret을 분리한다.
- 로컬 DB와 Mapper를 설정한다.
- Spring Boot 방식으로 업무모듈을 실행한다.
- WAR를 외부 Tomcat에 배포한다.
- 표준 요청 전문으로 첫 ServiceId를 호출한다.
- Controller부터 Mapper까지 중단점을 설정한다.
- 인증·권한·ServiceId·Mapper 오류를 구분한다.
- Thread·Transaction·SQL 실행상태를 확인한다.
- IDE와 CI Build 결과의 차이를 제거한다.
- 개발환경 점검표와 품질 Gate를 적용한다.

### 2.2 적용범위

| 영역 | 주요 내용 |
| --- | --- |
| 운영체제 | Windows 중심, Linux 명령 병행 |
| JDK | 설치·Version·환경변수 |
| Git | Clone·Branch·Commit·Merge |
| Gradle | Wrapper·멀티모듈·Task |
| IDE | Eclipse·IntelliJ Import |
| 설정 | Profile·환경변수·Secret |
| DB | Local DB·Oracle 접속·Pool |
| 실행 | bootRun·JUnit·WAR·Tomcat |
| 호출 | 표준 HTTP·JSON 거래 |
| 디버깅 | Controller부터 SQL까지 |
| 오류 | Build·기동·인증·Mapper |
| 품질 | Format·Test·Architecture Gate |
| 보안 | 계정·Secret·개인정보 |
| 변경 | Version·호환성·폐기 |

### 2.3 대상 독자

- NSIGHT 프로젝트에 처음 참여한 개발자
- Git과 Gradle 사용경험이 부족한 개발자
- Eclipse에서 멀티모듈 프로젝트를 처음 다루는 개발자
- Spring Boot와 외부 Tomcat 실행 차이가 어려운 개발자
- Handler와 Mapper까지 디버깅하고 싶은 개발자
- 로컬 설정과 운영 설정을 분리해야 하는 개발자
- IDE에서는 성공하지만 CI에서 실패하는 원인을 찾는 개발자
- 신규 개발자 온보딩 절차를 표준화하는 담당자

### 2.4 선행조건

다음 내용을 이해하고 있어야 합니다.

```
업무기능은 ServiceId로 식별한다.

Handler는 ServiceId를 분기한다.

Facade는 트랜잭션 경계를 담당한다.

Service는 업무 흐름을 담당한다.

DAO와 Mapper는 DB 접근을 담당한다.

공통 온라인 거래는 TCF Pipeline을 통과한다.

업무모듈은 공통 tcf-* 모듈을 의존한다.
```

### 2.5 주요 용어

| 용어 | 설명 |
| --- | --- |
| JDK | Java 프로그램 개발·실행 도구 |
| JVM | Java Bytecode 실행환경 |
| Git | 소스 Version 관리도구 |
| Repository | Git이 관리하는 소스 저장소 |
| Branch | 독립적인 소스 변경 흐름 |
| Commit | 변경내용을 Git에 저장한 단위 |
| Remote | 원격 Git 저장소 |
| Gradle | Build 자동화도구 |
| Wrapper | 프로젝트가 지정한 Gradle Version 실행파일 |
| Multi-module | 여러 하위 모듈로 구성된 프로젝트 |
| Task | Gradle이 수행하는 Build 작업 |
| Dependency | 모듈이 사용하는 다른 모듈·Library |
| Profile | 환경별 설정 묶음 |
| bootRun | Spring Boot 애플리케이션 실행 Task |
| WAR | 외부 WAS에 배포하는 Web Archive |
| Hot Swap | 디버깅 중 일부 코드 변경 반영 |
| Breakpoint | 실행을 일시 중지하는 지점 |
| Watch | 변수·표현식을 관찰하는 기능 |
| Step Into | 호출 메서드 내부로 진입 |
| Step Over | 현재 줄을 실행하고 다음 줄로 이동 |
| Step Return | 현재 메서드 실행을 끝내고 호출자로 복귀 |

## 제161장. 개발환경 전체 구성 이해하기

### 161.1 표준 개발환경 구성

```
개발자 PC
├─ JDK
├─ Git
├─ Gradle Wrapper
├─ Eclipse 또는 IntelliJ
├─ HTTP Client
├─ Local DB 또는 개발 DB
├─ Local Tomcat
└─ NSIGHT Source
```

### 161.2 권장 도구 분류

| 구분 | 도구 |
| --- | --- |
| Java | 프로젝트 표준 JDK |
| Version 관리 | Git |
| Build | Gradle Wrapper |
| IDE | Eclipse·IntelliJ |
| API 테스트 | Postman·Bruno·curl |
| DB 접속 | DBeaver·SQL Developer |
| WAS | Spring Boot Embedded Tomcat·외부 Tomcat |
| 로그 | IDE Console·Log Viewer |
| 비교 | Git Diff·IDE Compare |
| 정적분석 | Checkstyle·SpotBugs·ArchUnit |
| 테스트 | JUnit·MockMvc |

도구의 제품명보다 프로젝트가 지정한 Version과 설정을 따르는 것이 중요합니다.

### 161.3 개발환경 구성 순서

```
1. 프로젝트 기준 Version 확인

2. JDK 설치

3. Git 설치

4. 저장소 Clone

5. Gradle Wrapper 확인

6. 명령행 Build

7. IDE Import

8. Local Profile 설정

9. DB·외부연계 설정

10. 단위 테스트

11. 업무모듈 실행

12. 대표 거래 호출

13. Debug 실행
```

IDE Import 전에 명령행 Build를 먼저 수행하는 이유는 IDE 문제와 프로젝트 문제를 구분하기 위해서입니다.

### 161.4 확인해야 할 프로젝트 기준

```
JDK Version

Gradle Wrapper Version

Spring Boot Version

Tomcat Version

Source Encoding

Line Ending

기본 Branch

업무모듈명

실행 Profile

DB 종류

필수 환경변수

로컬 인증방식
```

이 정보는 개발환경 표준서 또는 프로젝트 README에 기록되어야 합니다.

### 161.5 권장 디렉터리

Windows 예:

```
C:\dev
├─ workspace
├─ repositories
├─ tools
├─ tomcat
├─ logs
└─ local-config
```

소스 경로 예:

```
C:\dev\repositories\nsight-tcf-framework
```

권장사항:

```
경로에 한글과 공백을 최소화한다.

지나치게 깊은 경로를 피한다.

Tomcat과 Source를 분리한다.

Secret 파일을 Source 하위에 저장하지 않는다.
```

### 161.6 개발환경과 운영환경 차이

| 항목 | 로컬 | 운영 |
| --- | --- | --- |
| DB | H2·개발 Oracle | 운영 Oracle |
| 인증 | Mock·개발 JWT | SSO·운영 JWT |
| Secret | 환경변수·Local Vault | 보안 저장소 |
| 로그 | Console 중심 | 중앙수집 |
| 배포 | bootRun·Local WAR | CI/CD |
| 데이터 | 테스트 데이터 | 운영 데이터 |
| 모니터링 | 제한적 | OM·통합관측 |
| 보안 | 개발망 정책 | 운영망 통제 |

로컬 편의를 위해 운영 보안구조를 완전히 제거하지 않습니다.

### 제161장 요약

```
개발환경은 JDK·Git·Gradle·IDE만 설치하는 작업이 아니다.

Profile·DB·인증·실행·디버깅 조건을
프로젝트 표준에 맞춰 구성해야 한다.

IDE Import 전에 명령행 Build를 먼저 확인한다.
```

## 제162장. JDK·환경변수·인코딩 설정

### 162.1 JDK Version 확인

프로젝트가 사용하는 Java Version은 다음 위치에서 확인할 수 있습니다.

```
build.gradle

gradle.properties

README

CI Pipeline

Dockerfile

Tomcat 실행 Script
```

Gradle 설정 예:

```
java {
    toolchain {
        languageVersion =
            JavaLanguageVersion.of(17)
    }
}
```

이 예에서는 JDK 17이 필요합니다.

실제 프로젝트의 설정값을 우선합니다.

### 162.2 설치 확인

Windows PowerShell:

```
java -version
javac -version
```

예상 결과:

```
java version "17.x"
javac 17.x
```

java는 있지만 javac이 없다면 JRE만 설치했을 가능성이 있습니다.

### 162.3 JAVA_HOME

Windows 예:

```
JAVA_HOME=C:\Program Files\Java\jdk-17
```

Path에 다음을 추가합니다.

```
%JAVA_HOME%\bin
```

확인:

```
echo $env:JAVA_HOME
where.exe java
where.exe javac
```

Linux·macOS:

```
echo $JAVA_HOME
which java
which javac
```

### 162.4 여러 JDK가 설치된 경우

다음 값이 서로 다를 수 있습니다.

```
명령행 Java

Gradle Daemon Java

Eclipse Installed JRE

Tomcat Java

CI Java
```

모두 프로젝트 표준 Version으로 맞춰야 합니다.

Gradle 확인:

```
.\gradlew.bat --version
```

Linux:

```
./gradlew --version
```

출력의 JVM Version을 확인합니다.

### 162.5 인코딩 표준

NSIGHT 신규 소스는 일반적으로 UTF-8을 기준으로 관리하는 것이 바람직합니다.

확인대상:

```
Java Source

Properties

YAML

Mapper XML

SQL Script

Gradle Console

Tomcat Log

DB 문자셋
```

### 162.6 MS949와 UTF-8 혼재

기존 파일이 MS949로 작성되어 있다면 무조건 저장하지 않습니다.

잘못 저장하면 한글이 깨진 상태로 Commit될 수 있습니다.

전환절차:

```
원본 인코딩 확인

Backup

UTF-8 변환

한글 비교

Build·Test

Git Diff 확인

승인 후 Commit
```

### 162.7 Gradle 인코딩 설정 예

```
tasks.withType(JavaCompile).configureEach {
    options.encoding = "UTF-8"
}

tasks.withType(Test).configureEach {
    systemProperty "file.encoding", "UTF-8"
}
```

JVM 전체 인코딩을 무조건 강제하기 전에 외부 전문과 Legacy 연계 인코딩 요구를 확인합니다.

### 162.8 Line Ending

Windows와 Linux에서 줄바꿈이 다를 수 있습니다.

```
Windows
CRLF

Linux
LF
```

.gitattributes 예:

```
*.java text eol=lf
*.gradle text eol=lf
*.yml text eol=lf
*.yaml text eol=lf
*.xml text eol=lf
*.bat text eol=crlf
*.sh text eol=lf
```

### 162.9 잘못된 JDK 증상

```
Unsupported class file major version

Invalid source release

Could not determine java version

ClassNotFoundException

Tomcat 기동 실패
```

JDK Version과 Build 결과의 Target Version을 확인합니다.

### 제162장 요약

```
명령행·Gradle·IDE·Tomcat이 동일한 JDK를 사용해야 한다.

인코딩과 Line Ending도 Build 환경의 일부다.

기존 MS949 파일은 확인 없이 UTF-8로 저장하지 않는다.
```

## 제163장. Git 저장소 Clone과 최초 점검

### 163.1 Git 설치 확인

```
git --version
```

사용자 정보 설정:

```
git config --global user.name "개발자명"
git config --global user.email "developer@example.com"
```

회사 정책에 따라 회사 계정과 이메일을 사용합니다.

### 163.2 저장소 Clone

예:

```
cd C:\dev\repositories
git clone <저장소 주소> nsight-tcf-framework
cd nsight-tcf-framework
```

Linux:

```
cd ~/dev/repositories
git clone <저장소 주소> nsight-tcf-framework
cd nsight-tcf-framework
```

### 163.3 Clone 직후 확인

```
git status
git remote -v
git branch -a
git log -5 --oneline
```

확인사항:

```
올바른 Remote인가?

기본 Branch가 맞는가?

Working Tree가 Clean한가?

최근 Commit을 받았는가?

Submodule이 있는가?
```

### 163.4 Submodule이 있는 경우

```
git submodule update --init --recursive
```

Submodule을 받지 않으면 일부 공통모듈이나 문서가 누락될 수 있습니다.

### 163.5 저장소 기본 구조 확인

예:

```
nsight-tcf-framework
├─ settings.gradle
├─ build.gradle
├─ gradlew
├─ gradlew.bat
├─ gradle
│  └─ wrapper
├─ tcf-util
├─ tcf-core
├─ tcf-web
├─ tcf-jwt
├─ tcf-cache
├─ tcf-eai
├─ tcf-batch
├─ tcf-om
├─ sv-service
├─ ic-service
├─ cm-service
└─ docs
```

실제 모듈은 저장소 Version에 따라 다를 수 있습니다.

### 163.6 settings.gradle 확인

예:

```
rootProject.name = "nsight-tcf-framework"

include(
    "tcf-util",
    "tcf-core",
    "tcf-web",
    "tcf-jwt",
    "tcf-cache",
    "tcf-eai",
    "tcf-batch",
    "tcf-om",
    "sv-service",
    "ic-service",
    "cm-service"
)
```

이 파일은 멀티모듈 구성의 기준입니다.

### 163.7 최초 Build 전 금지사항

```
IDE가 생성한 설정파일을 바로 Commit하지 않는다.

Gradle Version을 개인이 변경하지 않는다.

운영 application.yml을 복사하지 않는다.

공통모듈 Source를 임의 수정하지 않는다.

Build 실패를 피하려고 Test를 삭제하지 않는다.
```

### 163.8 저장소 신뢰성 확인

다음 파일이 포함되어야 합니다.

```
gradlew

gradlew.bat

gradle-wrapper.jar

gradle-wrapper.properties

settings.gradle

공통 Build Script

README 또는 개발환경 가이드
```

Wrapper 파일이 없다면 프로젝트 공식 Build 방식부터 확인해야 합니다.

### 제163장 요약

```
Clone 후 바로 IDE를 열지 말고
Remote·Branch·모듈·Wrapper를 먼저 확인한다.

settings.gradle은 전체 멀티모듈 구조의 기준이다.
```

## 제164장. Git Branch·Commit·협업 실전

### 164.1 기본 Branch

일반적인 예:

```
main
= 운영 또는 안정 Release

develop
= 통합 개발

feature/*
= 기능 개발

release/*
= Release 준비

hotfix/*
= 운영 긴급 수정
```

실제 Branch 정책은 프로젝트 표준을 사용합니다.

### 164.2 기능 Branch 생성

예:

```
git switch develop
git pull --ff-only
git switch -c feature/ct-contact-create
```

구버전 Git:

```
git checkout develop
git pull --ff-only
git checkout -b feature/ct-contact-create
```

### 164.3 Branch 이름

권장 개념:

```
feature/{업무코드}-{기능}

fix/{업무코드}-{오류}

hotfix/{장애ID}-{내용}
```

예:

```
feature/ct-contact-create

fix/sv-customer-timeout

hotfix/inc-20260717-jwt-validation
```

### 164.4 작업 전 동기화

```
git fetch --all --prune
git status
git pull --ff-only
```

--ff-only는 예상하지 못한 자동 Merge Commit을 방지하는 데 도움이 됩니다.

### 164.5 변경 확인

```
git status
git diff
git diff --staged
```

Commit 전에 반드시 확인합니다.

```
운영 계정이 포함되어 있지 않은가?

Secret이 포함되어 있지 않은가?

불필요한 IDE 파일이 포함되지 않았는가?

대용량 Log나 Build 결과가 포함되지 않았는가?

관련 없는 파일이 함께 변경되지 않았는가?
```

### 164.6 Commit 단위

좋은 Commit:

```
하나의 목적

Build 가능

Test 가능

설명 가능한 변경
```

나쁜 Commit:

```
상담 등록 구현

JDK 설정 변경

공통 JWT 수정

문서 전체 정렬

불필요 파일 삭제
```

서로 다른 목적을 한 Commit에 섞습니다.

### 164.7 Commit 메시지 예

```
feat(ct): 상담이력 등록 ServiceId 구현

- CT.Contact.create Handler 분기 추가
- Master와 History 동일 트랜잭션 적용
- 등록 Rule 및 단위 테스트 추가
```

수정 예:

```
fix(sv): 고객요약 조회기간 제한 누락 수정
```

### 164.8 Merge 전 최신화

```
git fetch origin
git rebase origin/develop
```

또는 프로젝트 정책에 따라 Merge를 사용합니다.

Rebase 중 충돌:

```
git status
```

충돌파일 수정 후:

```
git add <파일>
git rebase --continue
```

중단:

```
git rebase --abort
```

### 164.9 충돌 해결 원칙

```
내 변경을 무조건 선택하지 않는다.

상대 변경을 무조건 덮어쓰지 않는다.

업무 의미와 최신 설계를 확인한다.

Mapper XML·YAML 충돌은 특히 주의한다.

충돌 해결 후 전체 Build와 Test를 수행한다.
```

### 164.10 .gitignore

예:

```
.gradle/
build/
out/
bin/
logs/
*.log

.settings/
.classpath
.project

.idea/
*.iml

application-local-secret.yml
.env
```

IDE 파일 관리정책은 팀 표준에 따라 달라질 수 있습니다.

### 164.11 Secret Commit 사고

Secret을 Commit했다가 삭제해도 Git 이력에는 남을 수 있습니다.

즉시 조치:

```
보안 담당 통보

Secret 폐기·재발급

저장소 이력 정리 검토

영향시스템 확인

사고 기록
```

파일만 삭제하고 같은 Secret을 계속 사용하면 안 됩니다.

### 제164장 요약

```
작업은 개인 Feature Branch에서 수행한다.

Commit은 하나의 목적과 검증 가능한 단위로 만든다.

Merge 충돌 해결 후에는 전체 Build와 Test를 다시 수행한다.

Secret은 Git에 한 번도 저장되지 않도록 해야 한다.
```

## 제165장. Gradle 멀티모듈 Build 이해하기

### 165.1 Gradle Wrapper 사용

Windows:

```
.\gradlew.bat --version
```

Linux·macOS:

```
./gradlew --version
```

별도로 설치한 gradle 명령보다 Wrapper를 우선합니다.

```
gradlew
= Linux·macOS용

gradlew.bat
= Windows용
```

### 165.2 Wrapper를 사용하는 이유

```
모든 개발자가 동일 Gradle Version 사용

CI와 개발 PC의 Build 차이 감소

별도 Gradle 설치 불필요

프로젝트 Version 변경이 명확함
```

### 165.3 전체 Build

Windows:

```
.\gradlew.bat clean build
```

Linux:

```
./gradlew clean build
```

이 작업은 일반적으로 다음을 수행합니다.

```
Compile

Resource 복사

단위 테스트

정적분석

JAR·WAR 생성
```

프로젝트 설정에 따라 Task 구성은 달라질 수 있습니다.

### 165.4 특정 모듈 Build

```
.\gradlew.bat :ct-service:clean :ct-service:build
```

업무모듈이 공통모듈을 의존하면 필요한 공통모듈도 함께 Build됩니다.

### 165.5 테스트 제외 금지

임시 확인:

```
.\gradlew.bat :ct-service:build -x test
```

이 명령은 로컬 원인분석에 제한적으로 사용할 수 있지만, 정상 완료 기준으로 사용하면 안 됩니다.

정식 Build는 테스트를 포함해야 합니다.

### 165.6 주요 Gradle Task

| Task | 역할 |
| --- | --- |
| clean | 이전 Build 결과 삭제 |
| classes | Main Source Compile |
| testClasses | Test Compile |
| test | 단위 테스트 |
| build | 검증과 Artifact 생성 |
| bootRun | Spring Boot 실행 |
| bootWar | 실행 가능한 WAR 생성 |
| war | 일반 WAR 생성 |
| dependencies | 의존성 조회 |
| tasks | 사용 가능한 Task 조회 |

### 165.7 Task 목록

```
.\gradlew.bat tasks
```

특정 모듈:

```
.\gradlew.bat :ct-service:tasks
```

### 165.8 의존성 확인

```
.\gradlew.bat :ct-service:dependencies
```

특정 Configuration:

```
.\gradlew.bat :ct-service:dependencies `
  --configuration runtimeClasspath
```

확인:

```
동일 Library의 여러 Version

예상하지 못한 Spring Security

중복 Logging 구현체

Tomcat 내장·외장 충돌

Oracle Driver 범위
```

### 165.9 의존성 충돌 분석

```
.\gradlew.bat :ct-service:dependencyInsight `
  --dependency jackson-databind `
  --configuration runtimeClasspath
```

### 165.10 멀티모듈 의존성 예

ct-service/build.gradle 개념:

```
dependencies {
    implementation project(":tcf-util")
    implementation project(":tcf-core")
    implementation project(":tcf-web")
    implementation project(":tcf-jwt")
}
```

금지:

```
dependencies {
    implementation project(":sv-service")
}
```

업무 WAR끼리 직접 Project Dependency를 맺으면 배포경계가 무너질 수 있습니다.

업무 간 연계는 공개 Contract 또는 Client로 처리합니다.

### 165.11 JAR와 WAR

공통모듈:

```
tcf-core
→ JAR
```

업무모듈:

```
ct-service
→ WAR
```

WAR 내부 예:

```
WEB-INF/classes
→ 업무 Class·설정·Mapper XML

WEB-INF/lib
→ tcf-core.jar 등 의존 JAR
```

### 165.12 Build 결과 확인

```
ct-service
└─ build
   ├─ classes
   ├─ resources
   ├─ reports
   ├─ test-results
   └─ libs
      └─ ct-service-1.0.0.war
```

### 165.13 Gradle Daemon 문제

확인:

```
.\gradlew.bat --status
```

중지:

```
.\gradlew.bat --stop
```

JDK를 변경했는데 이전 Daemon이 계속 사용될 때 중지 후 재실행합니다.

### 165.14 Cache 초기화 주의

```
.\gradlew.bat clean
```

은 프로젝트 Build 결과만 삭제합니다.

사용자 Home의 Gradle Cache를 무조건 삭제하면 모든 Dependency를 다시 받아야 하며 원인분석이 어려워질 수 있습니다.

Cache 삭제는 손상 여부를 확인한 뒤 제한적으로 수행합니다.

### 제165장 요약

```
Gradle은 반드시 프로젝트 Wrapper로 실행한다.

전체 Build와 특정 모듈 Build의 차이를 이해한다.

업무 WAR끼리 직접 Project Dependency를 만들지 않는다.

Build 결과의 WAR와 포함 Resource를 직접 확인한다.
```

## 제166장. Eclipse·IntelliJ 프로젝트 등록

### 166.1 Eclipse 준비

권장 구성:

```
Eclipse IDE for Enterprise Java

Buildship Gradle Integration

프로젝트 표준 JDK

UTF-8 Workspace
```

Workspace는 Source 저장소와 분리합니다.

```
C:\dev\workspace\nsight
```

### 166.2 Eclipse Gradle Import

메뉴 개념:

```
File

→ Import

→ Gradle

→ Existing Gradle Project

→ Root Directory 선택

→ Gradle Wrapper 선택

→ Finish
```

하위 업무모듈 하나가 아니라 Root Project를 선택합니다.

### 166.3 Eclipse JDK 설정

```
Window

→ Preferences

→ Java

→ Installed JREs

→ 프로젝트 표준 JDK 선택
```

Compiler Level도 확인합니다.

```
Java

→ Compiler

→ Compiler Compliance Level
```

### 166.4 Eclipse 인코딩

```
Window

→ Preferences

→ General

→ Workspace

→ Text File Encoding

→ UTF-8
```

프로젝트별 설정도 확인합니다.

```
Project

→ Properties

→ Resource

→ Text File Encoding
```

### 166.5 Gradle Refresh

Build Script나 모듈이 변경된 경우:

```
프로젝트 우클릭

→ Gradle

→ Refresh Gradle Project
```

단순 Eclipse Project Refresh와 Gradle Refresh는 다릅니다.

### 166.6 Eclipse Build Path 직접수정 금지

Gradle 프로젝트에서 Library를 Eclipse Build Path에 수동 추가하지 않습니다.

다음 파일에서 의존성을 관리합니다.

```
build.gradle

build.gradle.kts

공통 Gradle Script
```

IDE 수동설정은 CI에 반영되지 않습니다.

### 166.7 IntelliJ Import

개념:

```
Open

→ Root build.gradle 또는 프로젝트 Root 선택

→ Gradle Wrapper 사용

→ Project SDK 설정
```

Gradle Build와 Run을 IDE 방식으로 할지 Gradle 방식으로 할지 팀 표준을 맞춥니다.

### 166.8 Annotation Processing

Lombok 등을 사용하는 경우 IDE Plugin과 Annotation Processing 설정이 필요할 수 있습니다.

증상:

```
소스에는 Getter가 있는데 IDE 오류

@RequiredArgsConstructor 필드 초기화 오류

Build는 성공하지만 IDE만 빨간색
```

Gradle Build 결과와 IDE 오류를 구분합니다.

### 166.9 IDE 생성파일

다음 파일을 Git에서 관리할지는 팀 정책에 따릅니다.

```
.project

.classpath

.settings

.idea

*.iml
```

개인 경로와 JDK 경로가 포함되는 파일은 일반적으로 제외합니다.

### 166.10 IDE 오류와 실제 Build 오류 구분

우선 명령행에서 확인합니다.

```
.\gradlew.bat clean build
```

명령행은 성공하지만 IDE만 오류라면 다음을 확인합니다.

```
Gradle Refresh

JDK

Annotation Processing

Workspace Cache

Source Folder

Generated Source
```

### 제166장 요약

```
멀티모듈 프로젝트는 Root에서 Gradle Project로 Import한다.

Library와 Source Path를 IDE에서 직접 수정하지 않는다.

IDE 오류와 Gradle Build 오류를 구분한다.
```

## 제167장. 로컬 Profile·환경설정·Secret 관리

### 167.1 설정 분리 원칙

```
공통 기본값

환경별 설정

개인 로컬 설정

Secret
```

을 분리합니다.

### 167.2 설정 파일 예

```
application.yml

application-local.yml

application-dev.yml

application-test.yml
```

민감정보가 없는 공통값은 Git에서 관리할 수 있습니다.

Secret은 환경변수나 외부 저장소로 주입합니다.

### 167.3 Profile 실행

Windows PowerShell:

```
$env:SPRING_PROFILES_ACTIVE="local"
.\gradlew.bat :ct-service:bootRun
```

한 번만 전달:

```
.\gradlew.bat :ct-service:bootRun `
  --args="--spring.profiles.active=local"
```

Linux:

```
SPRING_PROFILES_ACTIVE=local \
./gradlew :ct-service:bootRun
```

### 167.4 환경변수 예

```
CT_DB_URL

CT_DB_USERNAME

CT_DB_PASSWORD

JWT_JWKS_URI

IC_SERVICE_BASE_URL

LOCAL_SERVER_PORT
```

PowerShell:

```
$env:CT_DB_URL="jdbc:h2:mem:ctdb"
$env:CT_DB_USERNAME="sa"
$env:CT_DB_PASSWORD=""
```

### 167.5 YAML 예

```
spring:
  datasource:
    url: ${CT_DB_URL}
    username: ${CT_DB_USERNAME}
    password: ${CT_DB_PASSWORD}

server:
  port: ${LOCAL_SERVER_PORT:8080}

tcf:
  transaction:
    default-timeout-ms: 3000

clients:
  ic:
    base-url: ${IC_SERVICE_BASE_URL:http://localhost:8081}
```

### 167.6 Secret 파일

개인 로컬 Secret 파일을 사용할 경우:

```
C:\dev\local-config\ct-service-secret.yml
```

Source 밖에 저장합니다.

실행 예:

```
.\gradlew.bat :ct-service:bootRun `
  --args="--spring.config.additional-location=file:C:/dev/local-config/ct-service-secret.yml"
```

### 167.7 운영 설정 복사 금지

운영 설정에는 다음 정보가 포함될 수 있습니다.

```
운영 DB 계정

Private Key 경로

내부 시스템 주소

운영 관리자 계정

운영 Certificate
```

로컬 편의를 위해 복사하지 않습니다.

### 167.8 설정 우선순위

Spring 환경에서는 여러 설정원천이 우선순위에 따라 덮어쓸 수 있습니다.

```
기본 application.yml

Profile 설정

외부 설정파일

환경변수

명령행 Argument
```

실제 최종값을 확인해야 합니다.

### 167.9 설정 로그 주의

기동 시 다음을 전체 출력하지 않습니다.

```
DB Password

JWT Token

Secret Key

Private Key

API Key
```

민감하지 않은 설정은 마스킹하여 확인할 수 있습니다.

### 167.10 로컬 인증 방식

대안:

```
개발용 JWT 발급기

Mock Authentication Provider

개발 SSO 연계

고정 Test Token
```

원칙:

```
local Profile에서만 활성화한다.

운영 Profile에서는 기동되지 않아야 한다.

실제 운영 사용자정보를 사용하지 않는다.

권한 검증 자체는 가능하면 유지한다.
```

### 제167장 요약

```
공통설정·환경설정·개인설정·Secret을 분리한다.

Secret은 Git 저장소 밖에서 주입한다.

로컬 인증 편의기능은 local Profile에서만 활성화하고
운영 Build에서 자동 차단해야 한다.
```

## 제168장. 로컬 DB·DataSource·Mapper 준비

### 168.1 로컬 DB 선택

| 방식 | 장점 | 주의사항 |
| --- | --- | --- |
| H2 Memory | 빠른 실행 | Oracle 문법 차이 |
| H2 File | 데이터 유지 | 동시 실행 주의 |
| 개발 Oracle | 운영과 유사 | 접속권한·공유데이터 |
| Container DB | 격리·재현 | 도구·자원 필요 |
| Mock DAO | 단위 테스트 | SQL 검증 불가 |

### 168.2 H2와 Oracle 차이

다음 기능은 차이가 날 수 있습니다.

```
Data Type

Sequence

Pagination

Date Function

MERGE

Hint

Index

CLOB

Oracle 전용 함수
```

H2에서 성공했다고 운영 Oracle에서도 반드시 성공하는 것은 아닙니다.

Mapper·SQL은 개발 Oracle 또는 Oracle 호환 테스트에서도 검증해야 합니다.

### 168.3 DataSource 설정 예

```
spring:
  datasource:
    url: jdbc:h2:mem:ctdb;MODE=Oracle
    username: sa
    password:
    driver-class-name: org.h2.Driver

  h2:
    console:
      enabled: true
```

H2 Console은 local Profile에서만 사용합니다.

### 168.4 HikariCP 로컬 설정

```
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 3000
      validation-timeout: 1000
      idle-timeout: 600000
      max-lifetime: 1800000
```

로컬 PC에서 운영 Pool 크기를 그대로 사용할 필요는 없습니다.

### 168.5 Schema 초기화

예:

```
src/test/resources/schema.sql

src/test/resources/data.sql
```

또는 DB Migration Tool을 사용할 수 있습니다.

원칙:

```
테이블 생성 Script Version 관리

테스트 데이터와 운영 데이터 분리

반복 실행 가능

초기화 순서 명확화
```

### 168.6 Mapper Scan

설정 예:

```
@MapperScan(
    "com.nh.nsight.marketing.ct.**.mapper"
)
```

또는 Mapper Interface에 @Mapper를 사용합니다.

프로젝트 표준을 하나로 통일합니다.

### 168.7 Mapper XML 위치

예:

```
src/main/resources
└─ mapper
   └─ ct
      └─ contact
         └─ CtContactMapper.xml
```

Build 후 확인:

```
build/resources/main/mapper/ct/contact/CtContactMapper.xml
```

### 168.8 MyBatis 설정 예

```
mybatis:
  mapper-locations:
    - classpath*:mapper/**/*.xml
  type-aliases-package:
    com.nh.nsight.marketing.ct
  configuration:
    map-underscore-to-camel-case: true
```

### 168.9 DB 접속 점검

기동 전 확인:

```
URL

Host·Port

Service Name 또는 SID

계정

Schema

Network 접근

Driver

DB Timezone
```

### 168.10 DB 연결 오류 구분

| 오류 | 원인 후보 |
| --- | --- |
| Connection refused | Host·Port·DB Down |
| Authentication failed | 계정·Password |
| Unknown host | DNS |
| Driver not found | JDBC Driver 누락 |
| Schema object not found | Schema·권한 |
| Pool Timeout | Connection 고갈·DB 지연 |

### 168.11 SQL 로그

로컬 개발 시 SQL 로그를 제한적으로 사용할 수 있습니다.

주의:

```
고객번호·개인정보 출력

대량 SQL 로그

운영 Profile 활성화

성능 왜곡
```

Parameter를 마스킹하고 운영환경에서는 비활성화합니다.

### 제168장 요약

```
로컬 DB 방식은 빠른 개발과 운영 호환성 사이의 선택이다.

H2 결과만으로 Oracle SQL을 확정하지 않는다.

Mapper XML이 Build 결과에 포함되는지 직접 확인한다.
```

## 제169장. Spring Boot 방식으로 로컬 실행

### 169.1 실행 가능한 모듈 확인

모든 공통모듈을 실행하는 것은 아닙니다.

```
tcf-core
= Library JAR

tcf-web
= Library JAR

ct-service
= 실행 가능한 업무 WAR
```

Spring Boot Main Class가 있는 업무모듈을 실행합니다.

### 169.2 Main Class 예

```
@SpringBootApplication
public class CtServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(
            CtServiceApplication.class,
            args
        );
    }
}
```

외부 Tomcat WAR도 지원한다면 다음 구조가 추가될 수 있습니다.

```
@Override
protected SpringApplicationBuilder configure(
        SpringApplicationBuilder builder) {

    return builder.sources(
        CtServiceApplication.class
    );
}
```

### 169.3 bootRun

Windows:

```
.\gradlew.bat :ct-service:bootRun `
  --args="--spring.profiles.active=local"
```

Linux:

```
./gradlew :ct-service:bootRun \
  --args="--spring.profiles.active=local"
```

### 169.4 Port 지정

```
$env:LOCAL_SERVER_PORT="8082"

.\gradlew.bat :ct-service:bootRun `
  --args="--spring.profiles.active=local"
```

또는:

```
.\gradlew.bat :ct-service:bootRun `
  --args="--spring.profiles.active=local --server.port=8082"
```

### 169.5 여러 업무모듈 실행

예:

```
ct-service
8082

ic-service
8081

tcf-om
8090
```

각 모듈의 Port가 충돌하지 않도록 합니다.

업무 간 Client URL:

```
clients:
  ic:
    base-url: http://localhost:8081
```

### 169.6 기동 로그 확인

정상 확인항목:

```
Active Profile

Server Port

Context Path

DataSource 연결

Mapper Scan

Handler Registry

ServiceId 등록수

Security Filter

Health 상태
```

### 169.7 Health Check

예:

```
GET http://localhost:8082/actuator/health
```

또는 프로젝트 관리 Endpoint:

```
GET http://localhost:8082/health
```

예상:

```
{
  "status": "UP"
}
```

Health가 UP이라고 업무거래가 반드시 정상인 것은 아닙니다.

Readiness와 대표 ServiceId Smoke Test가 필요합니다.

### 169.8 Context Path

설정 예:

```
server:
  servlet:
    context-path: /ct
```

이 경우 Endpoint가 다음과 같이 될 수 있습니다.

```
POST http://localhost:8082/ct/online
```

공통 Controller의 Mapping과 Context Path를 함께 확인합니다.

### 169.9 Port 충돌

오류:

```
Web server failed to start.
Port 8080 was already in use.
```

Windows 확인:

```
netstat -ano | findstr :8080
```

Process 확인:

```
tasklist /FI "PID eq <PID>"
```

기존 Process를 무조건 종료하기 전에 어떤 애플리케이션인지 확인합니다.

### 169.10 기동 직후 확인

```
오류로그 없음

Handler Registry 정상

ServiceId 중복 없음

DB Pool 초기화

외부 Client 설정 정상

Local Security 활성화

Health UP

대표 조회거래 성공
```

### 제169장 요약

```
공통 JAR가 아니라 실행 가능한 업무모듈을 bootRun한다.

여러 업무를 실행할 때 Port와 Client URL을 분리한다.

Health Check 후 대표 ServiceId를 호출해야
실제 준비상태를 확인할 수 있다.
```

## 제170장. 외부 Tomcat에 WAR 배포하기

### 170.1 bootRun과 외부 Tomcat 차이

| 구분 | bootRun | 외부 Tomcat |
| --- | --- | --- |
| 목적 | 빠른 로컬 개발 | 운영 유사 검증 |
| Tomcat | Embedded | 별도 설치 |
| 배포물 | Classpath 실행 | WAR |
| 재기동 | Gradle Process | Tomcat |
| Context | 설정 | WAR·Context 설정 |
| ClassLoader | 단순 | Container 구조 |
| 다중 WAR | 제한적 | 가능 |

### 170.2 WAR 생성

```
.\gradlew.bat :ct-service:clean :ct-service:bootWar
```

또는 프로젝트 설정에 따라:

```
.\gradlew.bat :ct-service:war
```

결과:

```
ct-service/build/libs/ct-service-1.0.0.war
```

### 170.3 WAR 내용 확인

WAR는 ZIP 형식이므로 압축도구로 확인할 수 있습니다.

```
WEB-INF/classes

WEB-INF/lib

META-INF
```

확인:

```
업무 Class

application.yml

Mapper XML

tcf-core JAR

필수 JDBC Driver
```

### 170.4 Tomcat 디렉터리

```
tomcat
├─ bin
├─ conf
├─ lib
├─ logs
├─ temp
├─ webapps
└─ work
```

### 170.5 배포 방식

단순 개발환경:

```
WAR 파일을 webapps에 복사
```

예:

```
C:\dev\tomcat\webapps\ct.war
```

Context:

```
/ct
```

파일명이 ct-service-1.0.0.war이면 Context가 Version을 포함할 수 있으므로 명시적인 Context 설정을 권장합니다.

### 170.6 Context 설정

예:

```
<Context
    path="/ct"
    docBase="C:/dev/deploy/ct-service.war"
    reloadable="false" />
```

Tomcat Version과 운영표준에 따라 Context 설정방식은 달라질 수 있습니다.

### 170.7 setenv 설정

Windows setenv.bat 예:

```
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "CATALINA_OPTS=-Xms2g -Xmx2g"
set "CATALINA_OPTS=%CATALINA_OPTS% -Dspring.profiles.active=local"
set "CATALINA_OPTS=%CATALINA_OPTS% -Dfile.encoding=UTF-8"
```

Linux setenv.sh:

```
export JAVA_HOME=/opt/jdk-17

export CATALINA_OPTS="$CATALINA_OPTS -Xms2g -Xmx2g"
export CATALINA_OPTS="$CATALINA_OPTS -Dspring.profiles.active=local"
export CATALINA_OPTS="$CATALINA_OPTS -Dfile.encoding=UTF-8"
```

### 170.8 Tomcat 실행

Windows:

```
cd C:\dev\tomcat\bin
.\startup.bat
```

Console 실행:

```
.\catalina.bat run
```

Console 실행은 로컬 오류 확인에 유용합니다.

Linux:

```
./startup.sh
```

### 170.9 다중 WAR 배포 확인

```
Tomcat
├─ ct.war
├─ sv.war
├─ ic.war
└─ om.war
```

주의:

```
동일 Port 사용

공유 Connector Thread

공유 JVM Heap·GC

WAR별 DataSource 분리

WAR별 Context 분리

공통 Library 충돌
```

### 170.10 404 오류 원인

```
Context Path 오류

Controller Mapping 오류

WAR 기동 실패

배포파일명과 URL 불일치

Spring Context 초기화 실패

L4·Apache Route 오류
```

Tomcat Manager 화면만 보고 판단하지 말고 기동 로그를 확인합니다.

### 170.11 WAR가 배포됐지만 ServiceId가 없는 경우

```
HTTP Endpoint는 존재

ServiceId Handler 미등록

→ SERVICE_NOT_FOUND
```

Controller와 Handler Registry는 서로 다른 단계입니다.

### 170.12 ClassLoader 충돌

Tomcat lib와 WAR WEB-INF/lib에 같은 Library의 다른 Version이 있으면 문제가 발생할 수 있습니다.

```
NoSuchMethodError

ClassCastException

LinkageError
```

Tomcat 공용 Library와 WAR Library 정책을 명확히 합니다.

### 제170장 요약

```
bootRun은 빠른 개발용이고 외부 Tomcat은 WAR 배포 검증용이다.

WAR의 Context Path와 내부 Resource를 확인한다.

다중 WAR는 JVM·Thread·ClassLoader 영향을 공유한다.
```

## 제171장. 첫 번째 ServiceId 거래 호출

### 171.1 호출 전 확인

```
업무모듈 실행

Health UP

대상 ServiceId 등록

DB Schema 준비

권한 준비

JWT 또는 Local 인증 준비

Endpoint 확인
```

### 171.2 대표 거래

```
ServiceId
CT.Contact.selectList

거래코드
CT-INQ-0001

Endpoint
POST /ct/online
```

### 171.3 표준 요청 예

```
{
  "header": {
    "businessCode": "CT",
    "serviceId": "CT.Contact.selectList",
    "transactionCode": "CT-INQ-0001",
    "processingType": "INQUIRY",
    "channelId": "WEB"
  },
  "body": {
    "customerNo": "TEST000001",
    "fromDate": "20260701",
    "toDate": "20260717",
    "pageNumber": 1,
    "pageSize": 20
  }
}
```

### 171.4 curl 호출

PowerShell에서는 JSON Quote 처리 때문에 파일 사용이 편리합니다.

request.json 저장 후:

```
curl.exe `
  -X POST `
  "http://localhost:8082/ct/online" `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer <개발용 토큰>" `
  --data-binary "@request.json"
```

Linux:

```
curl -X POST \
  "http://localhost:8082/ct/online" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <개발용 토큰>" \
  --data-binary @request.json
```

### 171.5 성공 응답 예

```
{
  "result": {
    "resultStatus": "SUCCESS",
    "resultCode": "S0000"
  },
  "body": {
    "items": [],
    "pageNumber": 1,
    "pageSize": 20,
    "totalCount": 0
  }
}
```

빈 목록은 일반적으로 정상 성공입니다.

### 171.6 요청 오류 확인순서

```
HTTP 연결 가능?

Context Path 정확?

JSON 형식 정상?

JWT 정상?

businessCode 일치?

ServiceId 존재?

거래코드 등록?

권한 존재?

DB 연결 정상?
```

### 171.7 HTTP 404

확인:

```
Server Port

Context Path

Controller Mapping

WAR 기동상태
```

### 171.8 HTTP 401

확인:

```
Authorization Header

Token 만료

Signature

issuer

audience

kid
```

### 171.9 HTTP 403

확인:

```
인증은 성공

ServiceId 권한 없음

사용자 역할

데이터권한
```

### 171.10 ServiceId 오류

예:

```
SERVICE_NOT_FOUND

SERVICE_NOT_REGISTERED

SERVICE_SUSPENDED
```

구분:

| 오류 | 확인 |
| --- | --- |
| Handler 없음 | Source·Registry |
| OM 미등록 | Service Catalog |
| 거래중지 | 운영정책 |
| 업무코드 불일치 | Path·Header |

### 171.11 요청 Collection 관리

Postman·Bruno Collection에는 다음을 함께 관리할 수 있습니다.

```
환경별 Base URL

개발용 Token 변수

표준 Header

대표 정상 요청

대표 오류 요청

기대 응답
```

Secret Token을 Collection 파일에 직접 Commit하지 않습니다.

### 제171장 요약

```
첫 거래는 Health Check 이후 표준 JSON 전문으로 호출한다.

404·401·403·ServiceId 오류는 서로 다른 계층의 문제다.

요청 Collection에는 Secret을 저장하지 않는다.
```

## 제172장. Controller부터 Mapper까지 디버깅

### 172.1 디버깅 목표

첫 거래에서 다음 흐름을 직접 확인합니다.

```
OnlineTransactionController

→ TCF.process()

→ STF.preProcess()

→ TransactionTimeoutExecutor

→ TransactionDispatcher

→ CtContactHandler

→ CtContactFacade

→ CtContactService

→ CtContactRule

→ CtContactDao

→ CtContactMapper

→ ETF
```

### 172.2 권장 Breakpoint 순서

#### 1단계: Web 진입

```
OnlineTransactionController.process()
```

확인:

```
Path businessCode

Request Header

Request Body

Authorization Context
```

#### 2단계: TCF

```
TCF.process()
```

확인:

```
Context 생성

처리 시작시각

예외 흐름

finally 정리
```

#### 3단계: STF

```
STF.preProcess()
```

확인:

```
인증 사용자

지점

권한

Timeout

거래통제 상태
```

#### 4단계: Dispatcher

```
TransactionDispatcher.dispatch()
```

확인:

```
ServiceId

선택 Handler

Registry 내용
```

#### 5단계: 업무 Handler

```
CtContactHandler.handle()
```

확인:

```
switch 분기

Request DTO 변환

Facade 호출
```

#### 6단계: Facade·Service

```
CtContactFacade.selectList()

CtContactService.selectList()
```

확인:

```
Transaction 활성화

Rule 결과

Query 값

Context 사용자·지점
```

#### 7단계: DAO·Mapper

```
CtContactDao.selectContactList()
```

확인:

```
Mapper Parameter

Paging Offset

조회기간

데이터권한
```

### 172.3 Debug 실행

Eclipse:

```
Run

→ Debug Configurations

→ Spring Boot App 또는 Java Application

→ Profile·환경변수 설정

→ Debug
```

Gradle Debug 실행은 프로젝트 방식에 따라 구성합니다.

### 172.4 Step 명령

| 기능 | 설명 |
| --- | --- |
| Resume | 다음 Breakpoint까지 실행 |
| Step Into | 메서드 내부 진입 |
| Step Over | 메서드 호출 후 다음 줄 |
| Step Return | 현재 메서드 종료 |
| Drop to Frame | 현재 Stack Frame 재시작 |
| Terminate | 실행 종료 |

Drop to Frame은 DB 변경이나 외부 호출이 이미 수행된 상태에서는 중복 실행 위험이 있습니다.

### 172.5 변수 확인

관찰대상:

```
serviceId

transactionCode

guid

traceId

timeoutMillis

userId

branchId

request DTO

Command

Mapper Parameter

영향 건수
```

### 172.6 Expression 평가 주의

IDE Evaluate Expression에서 업무 메서드를 실행하면 실제 DB 변경이 발생할 수 있습니다.

안전한 확인:

```
Getter

단순 Boolean 표현식

Collection 크기

문자열 값
```

위험:

```
dao.insert()

service.create()

client.call()
```

### 172.7 트랜잭션 확인

확인할 수 있는 항목:

```
Facade에 @Transactional 존재

readOnly 여부

Transaction 활성상태

예외 발생 후 Rollback

영향 건수
```

주의:

```
같은 클래스 내부에서 자신의 @Transactional 메서드를 직접 호출하면
Proxy를 거치지 않아 트랜잭션이 적용되지 않을 수 있다.
```

### 172.8 Mapper SQL 확인

MyBatis Plugin 동작은 Runtime Proxy를 사용하므로 Mapper Interface 내부로 직접 Step Into가 되지 않을 수 있습니다.

대신 다음을 확인합니다.

```
DAO 호출 직전 Parameter

MyBatis Log

DB Session SQL

Mapper XML SQL ID

결과 DTO
```

### 172.9 SQL 실행 직전 Breakpoint

다음 위치가 유용합니다.

```
DAO Method

MyBatis Interceptor

SQL Logging Interceptor

Exception Translator
```

업무개발자는 일반적으로 DAO에서 Parameter를 확인하고 SQL 로그와 DB 도구로 실행계획을 확인합니다.

### 172.10 비동기·Timeout Debug 주의

Breakpoint에서 오래 멈추면 TCF Timeout이 발생할 수 있습니다.

로컬 Debug Profile에서만 다음을 검토할 수 있습니다.

```
Timeout 임시 증가

Timeout Executor 비활성화

특정 ServiceId Debug 정책
```

그러나 운영 동작과 차이가 생기므로 Debug 완료 후 원복합니다.

### 172.11 멀티 Thread 디버깅

동시성 문제에서는 Breakpoint가 전체 JVM을 멈추게 할 수 있습니다.

IDE 설정에서 다음 차이를 확인합니다.

```
Suspend All Threads

Suspend Current Thread
```

Lock·동시성 테스트에서는 중단방식이 결과에 영향을 줄 수 있습니다.

### 172.12 ThreadLocal 확인

거래 종료 후 다음 값이 제거되는지 확인합니다.

```
TransactionContextHolder

AuthenticationContextHolder

MDC

TimeoutContext
```

다음 거래에서 이전 사용자정보가 남으면 심각한 보안오류입니다.

### 172.13 오류흐름 디버깅

예외 Breakpoint를 설정합니다.

```
BusinessException

SystemException

TransactionTimeoutException

DataAccessException
```

잡힌 예외와 잡히지 않은 예외를 구분하면 ETF 변환 위치를 확인할 수 있습니다.

### 제172장 요약

```
디버깅은 Controller부터 Mapper까지 실행경로 순서로 진행한다.

Mapper Proxy 내부보다 DAO Parameter·SQL Log·DB 실행정보를 확인한다.

Breakpoint는 Timeout과 동시성에 영향을 줄 수 있다.
```

## 제173장. 로그·Trace·SQL을 함께 이용한 디버깅

### 173.1 디버깅과 로그의 차이

```
Debugger
= 현재 실행 중인 한 거래를 세밀하게 확인

Log·Trace
= 여러 거래와 시간 흐름을 확인
```

운영과 유사한 문제는 로그와 Metric이 더 효과적일 수 있습니다.

### 173.2 필수 로그 Context

```
GUID

TraceId

ServiceId

거래코드

업무코드

WAR

Server

사용자 마스킹값

처리시간

오류코드
```

### 173.3 MDC 예

```
MDC.put("traceId", context.getTraceId());
MDC.put("serviceId", context.getServiceId());
MDC.put("businessCode", context.getBusinessCode());
```

거래 종료:

```
finally {
    MDC.clear();
}
```

### 173.4 계층별 로그 기준

| 계층 | 로그 |
| --- | --- |
| Controller | 요청 수신·크기 |
| STF | 인증·통제 결과 |
| Dispatcher | 선택 Handler |
| Service | 주요 업무 분기 |
| Client | 대상·시간·결과 |
| DAO | SQL ID·시간 |
| ETF | 최종 상태·시간 |

모든 메서드 진입·종료를 무조건 로그로 남기면 로그량이 과도해집니다.

### 173.5 민감정보 금지

```
JWT 원문

Password

Private Key

상담내용 전체

고객번호 원문

주민등록번호

계좌번호 원문
```

### 173.6 SQL 로그 기준

권장:

```
SQL ID

Mapper ID

수행시간

조회·영향 건수

Timeout 여부
```

주의:

```
전체 Parameter 원문

대량 SQL Text 반복

운영환경 DEBUG Log
```

### 173.7 Trace 기반 문제 확인

```
TraceId

→ Controller Log

→ STF Log

→ Service Log

→ Client Log

→ SQL Log

→ ETF Log
```

한 거래의 전체 처리시간을 구간별로 비교합니다.

### 173.8 처리시간 분해 예

| 구간 | 시간 |
| --- | --- |
| Gateway | 20ms |
| STF | 15ms |
| Service | 40ms |
| IC Client | 700ms |
| SQL | 200ms |
| JSON | 50ms |
| 전체 | 1,025ms |

### 173.9 로컬 Log Level

예:

```
logging:
  level:
    com.nh.nsight.marketing.ct: DEBUG
    org.mybatis: INFO
    org.springframework.transaction: TRACE
```

전체 Root를 DEBUG로 설정하면 로그가 지나치게 많아질 수 있습니다.

대상 Package만 제한적으로 설정합니다.

### 173.10 Debug 종료 후 확인

```
DEBUG Log 원복

민감정보 출력 없음

임시 SQL Log 제거

임시 Timeout 원복

테스트용 Authentication 제거

불필요한 Breakpoint 제거
```

### 제173장 요약

```
Debugger는 한 거래의 내부상태를 확인하고,
Trace는 전체 처리구간을 연결한다.

로그에는 SQL 원문보다 SQL ID·시간·건수를 우선 기록한다.

민감정보와 임시 Debug 설정은 Commit하지 않는다.
```

## 제174장. 자주 발생하는 Build·기동·실행 오류

### 174.1 JAVA_HOME 오류

증상:

```
JAVA_HOME is not set

Java executable not found
```

확인:

```
echo $env:JAVA_HOME
where.exe java
```

### 174.2 Java Version 오류

증상:

```
Unsupported class file major version

Invalid source release
```

확인:

```
명령행 Java

Gradle JVM

IDE JDK

Tomcat JDK
```

### 174.3 Dependency 다운로드 실패

원인 후보:

```
사내 Repository 접속

Proxy

인증서

Offline Mode

잘못된 Version

Repository 권한
```

확인:

```
.\gradlew.bat build --info
```

민감한 Repository 계정이 Log에 노출되지 않도록 주의합니다.

### 174.4 Module을 찾을 수 없음

오류:

```
Project with path ':tcf-core' could not be found
```

확인:

```
settings.gradle include

모듈 디렉터리명

Project Dependency 이름

Branch에 모듈 존재
```

### 174.5 Spring Bean 중복

```
NoUniqueBeanDefinitionException
```

원인:

```
동일 Interface 구현체 복수

Profile 조건 오류

Component Scan 중복

Test Bean 충돌
```

### 174.6 Bean 없음

```
NoSuchBeanDefinitionException
```

확인:

```
@Component·@Service

Component Scan 범위

Profile

Conditional 설정

Module Dependency
```

### 174.7 ServiceId 중복

기동 시:

```
Duplicate ServiceId:
CT.Contact.selectList
```

두 Handler의 serviceIds()를 확인합니다.

기동을 강제로 진행하지 않고 중복 책임을 제거합니다.

### 174.8 ServiceId 없음

```
SERVICE_NOT_FOUND
```

확인:

```
Handler Bean 등록

serviceIds() 반환값

대소문자

업무코드

배포된 WAR Version
```

### 174.9 Mapper Statement 없음

```
Invalid bound statement
```

확인:

```
Mapper Namespace

Java Method명

XML id

mapper-locations

Resource 포함 여부
```

### 174.10 DB Column 없음

```
Invalid identifier

Column not found
```

확인:

```
DB Migration

접속 Schema

SQL Alias

Column Version

서버별 설정
```

### 174.11 Transaction 미적용

증상:

```
Master는 저장됐지만 History 실패 후 남아 있음
```

확인:

```
@Transactional 위치

Public Method

Proxy 호출 여부

예외를 잡아버렸는가?

Rollback 대상 예외인가?

DataSource가 같은가?
```

### 174.12 401 인증 오류

확인:

```
Token 만료

개발용 발급자

issuer·audience

JWKS URI

서버시간

Profile
```

### 174.13 403 권한 오류

인증은 성공했지만 실행권한이 없는 상태입니다.

확인:

```
권한 Claim

OM 권한 매핑

ServiceId

사용자 역할

지점 데이터권한
```

### 174.14 Port 충돌

```
Port 8080 already in use
```

다른 업무모듈과 Port를 분리합니다.

### 174.15 Circular Dependency

```
Bean A → Bean B → Bean A
```

개선:

```
책임 재분리

공통 계약 추출

Event 사용

양방향 호출 제거
```

@Lazy는 임시 우회가 될 수 있지만 구조적 원인을 확인해야 합니다.

### 174.16 IDE에서는 성공, CI에서는 실패

가능한 원인:

```
IDE Build Path 수동 추가

미Commit 파일

대소문자 파일명

Line Ending

로컬 Cache

환경변수

테스트 순서 의존

운영체제 경로
```

명령행 Clean Build로 재현합니다.

### 제174장 요약

```
오류 메시지를 먼저 분류한다.

Build·기동·인증·ServiceId·Mapper·DB 오류는
각기 다른 계층에서 발생한다.

IDE 수동설정으로 문제를 숨기지 않는다.
```

## 제175장. IDE와 CI/CD 환경 정합성

### 175.1 개발 완료의 기준

```
IDE에서 실행됨
```

만으로 완료가 아닙니다.

```
Gradle Clean Build 성공

단위 테스트 성공

Architecture Gate 성공

동일 Artifact 생성

CI Pipeline 성공
```

이 필요합니다.

### 175.2 로컬과 CI를 맞출 항목

| 항목 | 정합성 |
| --- | --- |
| JDK | 동일 Major Version |
| Gradle | Wrapper |
| Encoding | UTF-8 |
| Timezone | 테스트 기준 |
| Profile | Test 전용 |
| DB | 재현 가능한 Schema |
| Secret | CI Secret Store |
| Test | 순서 독립 |
| OS | 경로·대소문자 고려 |
| Artifact | 동일 Task |

### 175.3 로컬 최종검증

```
git status

.\gradlew.bat clean build
```

추가:

```
.\gradlew.bat check
```

프로젝트에 별도 품질 Task가 있다면 실행합니다.

### 175.4 Test 순서 의존 제거

나쁜 테스트:

```
1번 테스트가 데이터를 등록

2번 테스트가 그 데이터를 사용
```

개별 실행 시 실패합니다.

권장:

```
각 테스트가 자신의 데이터 준비

Transaction Rollback

고유 Test ID

실행순서와 무관
```

### 175.5 시간 의존 테스트

현재시간을 직접 사용하면 실행시각에 따라 실패할 수 있습니다.

권장:

```
Clock fixedClock =
    Clock.fixed(
        Instant.parse("2026-07-17T00:00:00Z"),
        ZoneId.of("Asia/Seoul")
    );
```

업무 Service에 Clock을 주입합니다.

### 175.6 환경 의존 테스트

금지:

```
개발자 PC의 특정 파일경로

개인 DB 계정

현재 실행 중인 다른 서비스

개인 JWT Token
```

Test Profile과 Test Container·Mock을 사용합니다.

### 175.7 Artifact 동일성

```
Build Once

→ 동일 WAR를 개발·검증·운영으로 승격
```

환경마다 다시 Build하면 다른 Artifact가 만들어질 수 있습니다.

설정과 Secret은 외부 주입합니다.

### 175.8 CI 실패 대응

```
실패 단계 확인

→ Compile·Test·Static Analysis·Packaging 구분

→ 로컬 동일 명령 재현

→ 환경차이 확인

→ 원인 수정

→ Pipeline 재실행
```

CI를 통과시키기 위해 검증 Task를 임의로 비활성화하지 않습니다.

### 175.9 Definition of Done

```
Source Commit

Code Review

Clean Build

Unit Test

Integration Test

Architecture Test

설계서·RTM 반영

OM 등록정보 반영

Release Note

Rollback 검토
```

### 제175장 요약

```
개발환경의 최종 목표는 개인 PC에서 실행하는 것이 아니라
CI와 동일한 조건으로 재현 가능한 Build를 만드는 것이다.

테스트는 순서·시간·개인환경에 의존하지 않아야 한다.
```

## 제176장. 신규 개발자 첫날부터 첫 거래까지

### 176.1 1단계: 환경 기준 확인

```
JDK

Gradle

IDE

Git Branch

실행 Profile

업무모듈

대표 ServiceId
```

### 176.2 2단계: 저장소 Clone

```
git clone <저장소 주소>
cd nsight-tcf-framework
git status
```

### 176.3 3단계: 명령행 Build

```
.\gradlew.bat clean build
```

실패하면 IDE Import 전에 해결합니다.

### 176.4 4단계: IDE Import

```
Root Gradle Project Import

JDK 설정

UTF-8 설정

Gradle Refresh
```

### 176.5 5단계: Local 설정

```
Profile

DB URL

개발용 JWT

업무 Port

외부 Client URL
```

### 176.6 6단계: 업무모듈 실행

```
.\gradlew.bat :ct-service:bootRun `
  --args="--spring.profiles.active=local --server.port=8082"
```

### 176.7 7단계: Health 확인

```
GET /health
```

### 176.8 8단계: 첫 거래 호출

```
POST /ct/online

ServiceId
CT.Contact.selectList
```

### 176.9 9단계: Breakpoint 설정

```
Controller

TCF

STF

Dispatcher

Handler

Facade

Service

DAO
```

### 176.10 10단계: SQL과 응답 확인

```
Mapper Parameter

SQL ID

조회 건수

ETF 결과

TraceId
```

### 176.11 첫날 완료 기준

```
전체 Build 성공

IDE 오류 없음

업무모듈 기동

Health UP

대표 ServiceId 성공

Controller부터 DAO까지 Debug

SQL 확인

Git Branch 생성

개발환경 체크리스트 완료
```

### 제176장 요약

```
신규 개발자의 첫 목표는 코드를 수정하는 것이 아니다.

표준 Build와 실행환경을 재현하고,
대표 거래의 전체 경로를 직접 확인하는 것이 먼저다.
```

## 3. 목표 개발환경 아키텍처

```
                    [Git Repository]
                           │
                           ▼
                   [Feature Branch]
                           │
                           ▼
                   [Gradle Wrapper]
                           │
             ┌─────────────┴─────────────┐
             ▼                           ▼
       [공통모듈 Build]              [업무 WAR Build]
  tcf-util·core·web·jwt             ct·sv·ic-service
             │                           │
             └─────────────┬─────────────┘
                           ▼
                       [IDE]
                Eclipse·IntelliJ Debug
                           │
                           ▼
                   [Local Profile]
          DB·JWT·Client URL·Server Port
                           │
                ┌──────────┴──────────┐
                ▼                     ▼
          [bootRun 실행]        [외부 Tomcat]
                │                     │
                └──────────┬──────────┘
                           ▼
                    [표준 거래 호출]
                           │
                           ▼
 Controller → TCF → STF → Handler → Service → Mapper
                           │
                           ▼
                    [Test·Quality Gate]
                           │
                           ▼
                    [CI Build·Artifact]
```

## 4. 표준 형식

### 4.1 개발환경 기준정보

| 항목 | 값 예 |
| --- | --- |
| JDK | 프로젝트 표준 Version |
| Gradle | Wrapper Version |
| IDE | Eclipse·IntelliJ |
| Encoding | UTF-8 |
| Git 기본 Branch | develop |
| 업무 Branch | feature/* |
| Local Profile | local |
| Test Profile | test |
| Build Task | clean build |
| 실행 Task | :{module}:bootRun |
| 배포물 | WAR |
| Local DB | H2·개발 Oracle |
| API Client | curl·Postman |
| Debug 기준 | ServiceId 흐름 |

### 4.2 로컬 실행 설정

```
spring:
  profiles:
    active: local

server:
  port: 8082
  servlet:
    context-path: /ct

tcf:
  transaction:
    default-timeout-ms: 3000
```

실제 Secret과 환경값은 환경변수로 주입합니다.

### 4.3 개발환경 점검결과

```
개발자 ID

PC·OS

JDK Version

Gradle Version

Git Commit

Branch

업무모듈

Build 결과

대표 ServiceId

실행일

점검자
```

## 5. 구성요소 및 속성

| 구성요소 | 주요 속성 |
| --- | --- |
| JDK | Version·JAVA_HOME |
| Git | Remote·Branch·Commit |
| Gradle Wrapper | Version·Task |
| IDE | JDK·Encoding·Plugin |
| Local Profile | Port·DB·인증 |
| DataSource | URL·Pool |
| Mapper | Namespace·Resource |
| 업무모듈 | Main·WAR |
| Tomcat | Context·JVM |
| API Client | Request·Token |
| Debugger | Breakpoint·Watch |
| Log·Trace | ServiceId·TraceId |
| CI | Build·Test·Artifact |

## 6. 책임 경계와 RACI

| 활동 | 개발자 | 모듈리더 | FW | TA | DBA | 보안 | QA | DevOps |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| JDK·IDE 설치 | R | C | C | C | I | I | I | I |
| Git 권한 | R | C | I | I | I | C | I | A/C |
| Branch 정책 | R | A | C | I | I | I | C | C |
| Gradle 표준 | C | C | A/R | C | I | I | C | C |
| Local Profile | R | A/C | C | C | C | C | I | I |
| DB 접속 | R | C | I | I | A/R | C | I | I |
| 인증 설정 | C | C | R | I | I | A/R | C | I |
| 업무모듈 실행 | R | A/C | C | I | C | I | I | I |
| Debug | R | A/C | C | C | C | I | I | I |
| CI Pipeline | C | C | C | C | I | C | C | A/R |
| 개발환경 Gate | R | A | C | C | C | C | R/C | C |

## 7. 정상 처리 흐름

```
1. 프로젝트 표준 Version을 확인한다.

2. JDK와 Git을 설치한다.

3. 저장소를 Clone한다.

4. Gradle Wrapper로 전체 Build한다.

5. Root 프로젝트를 IDE에 Import한다.

6. Local Profile과 Secret을 설정한다.

7. DB와 외부 Client 연결을 확인한다.

8. 업무모듈을 bootRun한다.

9. Health와 Handler Registry를 확인한다.

10. 대표 ServiceId를 호출한다.

11. Controller부터 DAO까지 Debug한다.

12. SQL과 표준 응답을 확인한다.

13. Feature Branch에서 기능을 개발한다.

14. Clean Build와 Test를 수행한다.

15. Commit·Push·Code Review를 수행한다.

16. CI Pipeline과 Artifact를 확인한다.
```

## 8. 오류·Timeout·장애 흐름

### 8.1 Build 실패

```
오류 단계 확인

→ JDK·Dependency·Compile·Test 구분

→ 명령행 재현

→ 원인 수정

→ Clean Build
```

### 8.2 기동 실패

```
Profile

→ Port

→ DB

→ Bean

→ Mapper

→ ServiceId Registry

→ Security
```

순서로 확인합니다.

### 8.3 거래 실패

```
HTTP 연결

→ 인증

→ 권한

→ ServiceId

→ Handler

→ 업무 Validation

→ DB

→ ETF
```

순서로 좁혀갑니다.

### 8.4 Debug Timeout

```
Breakpoint 장시간 정지

→ TCF Timeout

→ Transaction Rollback 또는 UNKNOWN

→ Debug 전용 정책 적용

→ Debug 종료 후 원복
```

### 8.5 IDE·CI 불일치

```
IDE Build 성공

CI 실패

→ Gradle Clean Build

→ JDK·환경변수·미Commit 파일 확인

→ IDE 수동설정 제거
```

## 9. 정상 예시

```
Branch
feature/ct-contact-create

Build
.\gradlew.bat clean build 성공

Module
ct-service

Profile
local

Port
8082

Context
/ct

ServiceId
CT.Contact.selectList

Endpoint
POST /ct/online

Debug 경로
Controller
→ TCF
→ STF
→ CtContactHandler
→ CtContactService
→ CtContactDao

SQL ID
CT-CNT-SEL-001

결과
SUCCESS
```

## 10. 금지 예시

### 10.1 시스템 Gradle 사용

```
gradle build
```

개인 설치 Version을 사용합니다.

권장:

```
gradlew build
```

### 10.2 운영 설정 복사

운영 DB 계정과 Secret을 로컬에 저장합니다.

### 10.3 IDE Build Path 수동 추가

Gradle 의존성을 우회합니다.

### 10.4 업무 WAR 직접 의존

```
ct-service
→ ic-service Project Dependency
```

### 10.5 공통 Controller 우회

별도 업무 Controller로 거래를 실행합니다.

### 10.6 테스트 제외 Commit

```
build -x test
```

결과만 확인하고 정식 Build 없이 Merge합니다.

### 10.7 Debug용 보안 해제 Commit

```
permitAll()

JWT Filter 비활성화
```

를 공통 Branch에 반영합니다.

### 10.8 Secret Commit

.env, Password, Token을 저장소에 올립니다.

### 10.9 Breakpoint 상태로 성능 측정

Debugger가 실행시간에 영향을 주므로 성능시험 결과로 사용할 수 없습니다.

## 11. 연계 규칙

```
Git Branch

→ Source Commit

→ Gradle Build

→ 업무 WAR

→ Local Profile

→ ServiceId 실행

→ Debug Trace

→ Test Result

→ CI Build

→ Artifact Version
```

개발환경 정보는 다음 기준정보와 연결합니다.

```
JDK Version

Gradle Wrapper Version

Git Commit

Module Version

DB Migration Version

OM Service Catalog Version
```

## 12. 데이터 및 상태관리

### 12.1 개발환경 상태

```
REQUESTED

INSTALLING

CONFIGURED

BUILD_VERIFIED

RUN_VERIFIED

DEBUG_VERIFIED

APPROVED
```

### 12.2 Branch 상태

```
CREATED

DEVELOPING

REVIEWING

MERGED

DELETED
```

### 12.3 Build 상태

```
QUEUED

RUNNING

SUCCESS

FAILED

CANCELED
```

### 12.4 Local 거래 상태

```
RECEIVED

PROCESSING

SUCCESS

BUSINESS_FAIL

SYSTEM_ERROR

TIMEOUT
```

## 13. 성능·용량·확장성

로컬 개발환경에서도 다음을 주의합니다.

```
여러 업무모듈 동시 실행에 따른 Heap 사용

업무별 Port 충돌

H2 Memory DB 크기

Gradle Daemon Memory

IDE Heap

Tomcat 다중 WAR Heap

과도한 DEBUG Log

대량 Test Data
```

개발 PC 성능이 부족하면 전체 모듈을 모두 실행하지 않고 필요한 업무와 연계 Mock만 실행합니다.

## 14. 보안·개인정보·감사

```
운영 데이터와 운영 Secret을 개발 PC에 저장하지 않는다.

개발용 JWT는 제한된 사용자와 짧은 만료시간을 사용한다.

Local 보안 우회기능은 local Profile에서만 활성화한다.

Git에 Token·Password·Private Key를 Commit하지 않는다.

SQL Log와 Debug 변수에서 개인정보를 보호한다.

개발 DB 접속과 데이터 Export를 감사정책에 따라 관리한다.

폐기된 개발 PC와 저장장치의 Source·Secret을 안전하게 삭제한다.
```

## 15. 운영·모니터링·장애 대응

개발환경에서도 최소한 다음 정보를 확인할 수 있어야 합니다.

```
실행 Profile

Server Port

Context Path

Git Commit

Artifact Version

ServiceId 등록수

DB Pool 상태

대표 거래 Trace

Mapper SQL ID

오류 Stack
```

환경문제 발생 시 Runbook:

```
JDK 확인

→ Gradle 확인

→ Git 상태

→ Profile

→ Port

→ DB

→ Mapper

→ 인증

→ ServiceId

→ 업무 Source
```

## 16. 자동검증 및 품질 Gate

| Gate | 합격기준 |
| --- | --- |
| JDK | 프로젝트 표준 Version |
| Gradle | Wrapper 사용 |
| Encoding | UTF-8 |
| Git | Clean Working Tree |
| Secret | 저장소 미포함 |
| Build | clean build 성공 |
| Test | 실패 0 |
| Module | 의존성 규칙 준수 |
| Mapper | Interface·XML 일치 |
| ServiceId | Handler 등록 |
| Local Security | 운영 Profile 비활성 |
| WAR | Resource 포함 |
| CI | 동일 명령 성공 |
| Artifact | Version·Checksum 생성 |

## 17. 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| DEV-001 | JDK Version 확인 | 표준 Version |
| DEV-002 | JAVA_HOME 오류 | 기동 차단 |
| DEV-003 | Wrapper Build | 성공 |
| DEV-004 | 시스템 Gradle 차이 | Wrapper 우선 |
| DEV-005 | Root Import | 전체 모듈 인식 |
| DEV-006 | Local Profile 실행 | 정상 기동 |
| DEV-007 | Port 충돌 | 원인 Process 확인 |
| DEV-008 | DB 접속 실패 | 명확한 오류 |
| DEV-009 | Mapper XML 누락 | Build·기동 실패 |
| DEV-010 | ServiceId 중복 | 기동 실패 |
| DEV-011 | JWT 없음 | 401 |
| DEV-012 | 권한 없음 | 403 |
| DEV-013 | 정상 ServiceId | SUCCESS |
| DEV-014 | Handler Breakpoint | 정상 진입 |
| DEV-015 | Rule 오류 | BusinessException |
| DEV-016 | SQL 오류 | SystemException |
| DEV-017 | Transaction Rollback | 데이터 미반영 |
| DEV-018 | Debug Timeout | Timeout 확인 |
| DEV-019 | WAR Tomcat 배포 | Context 정상 |
| DEV-020 | 서버별 JDK 차이 | Gate 실패 |
| DEV-021 | Secret Commit | 보안 Gate 실패 |
| DEV-022 | IDE Build Path 수동 의존 | CI 실패 탐지 |
| DEV-023 | Test 순서 변경 | 동일 성공 |
| DEV-024 | CI Clean Build | 성공 |
| DEV-025 | Artifact Checksum | 추적 가능 |

## 18. 제17부 체크리스트

### 18.1 도구·Version

| 점검 항목 | 확인 |
| --- | --- |
| 프로젝트 표준 JDK를 설치했는가? | □ |
| JAVA_HOME이 정확한가? | □ |
| Gradle JVM이 같은 JDK인가? | □ |
| Git Version을 확인했는가? | □ |
| IDE에 표준 JDK를 등록했는가? | □ |
| Encoding이 UTF-8인가? | □ |

### 18.2 Git

| 점검 항목 | 확인 |
| --- | --- |
| 올바른 Remote인가? | □ |
| 기본 Branch가 정확한가? | □ |
| Feature Branch를 만들었는가? | □ |
| Working Tree가 Clean한가? | □ |
| Secret이 포함되지 않았는가? | □ |
| Commit이 한 목적 단위인가? | □ |
| Merge 전 최신화했는가? | □ |

### 18.3 Gradle

| 점검 항목 | 확인 |
| --- | --- |
| Wrapper를 사용하는가? | □ |
| 전체 clean build가 성공하는가? | □ |
| 대상 모듈 Build가 성공하는가? | □ |
| 테스트를 제외하지 않았는가? | □ |
| 의존성 충돌을 확인했는가? | □ |
| 업무 WAR 간 직접 의존이 없는가? | □ |
| WAR 내부 Resource를 확인했는가? | □ |

### 18.4 IDE

| 점검 항목 | 확인 |
| --- | --- |
| Root Gradle Project를 Import했는가? | □ |
| Gradle Refresh를 수행했는가? | □ |
| Build Path를 수동 수정하지 않았는가? | □ |
| Annotation Processing이 정상인가? | □ |
| 개인 IDE 파일이 Git에 포함되지 않았는가? | □ |
| 명령행 Build와 결과가 같은가? | □ |

### 18.5 설정·DB

| 점검 항목 | 확인 |
| --- | --- |
| Local Profile을 사용하는가? | □ |
| Secret을 환경변수로 주입하는가? | □ |
| 운영 설정을 복사하지 않았는가? | □ |
| DB URL과 Schema가 정확한가? | □ |
| Hikari Pool이 로컬 수준인가? | □ |
| Mapper XML이 포함되는가? | □ |
| Oracle 호환 테스트를 수행하는가? | □ |

### 18.6 실행·호출

| 점검 항목 | 확인 |
| --- | --- |
| 실행 가능한 업무모듈을 선택했는가? | □ |
| Port와 Context가 정확한가? | □ |
| Health가 정상인가? | □ |
| 대표 ServiceId가 등록되는가? | □ |
| JWT 또는 Local 인증이 준비되었는가? | □ |
| 표준 요청으로 거래가 성공하는가? | □ |
| TraceId를 확인할 수 있는가? | □ |

### 18.7 디버깅

| 점검 항목 | 확인 |
| --- | --- |
| Controller에 진입하는가? | □ |
| STF 인증정보를 확인했는가? | □ |
| Dispatcher Handler를 확인했는가? | □ |
| Facade Transaction을 확인했는가? | □ |
| Rule 결과를 확인했는가? | □ |
| Mapper Parameter를 확인했는가? | □ |
| 예외가 ETF에서 변환되는가? | □ |
| ThreadLocal이 정리되는가? | □ |
| Debug용 설정을 원복했는가? | □ |

### 18.8 CI 정합성

| 점검 항목 | 확인 |
| --- | --- |
| 로컬과 CI JDK가 같은가? | □ |
| 동일 Gradle Task를 사용하는가? | □ |
| Test가 순서에 독립적인가? | □ |
| 개인환경 경로를 사용하지 않는가? | □ |
| Source·설계서가 함께 변경되었는가? | □ |
| CI Pipeline이 성공하는가? | □ |
| 동일 Artifact를 승격하는가? | □ |

## 19. 변경·호환성·폐기 관리

### 19.1 JDK Upgrade

```
호환성 조사

→ Gradle·Spring Boot 지원 확인

→ 공통모듈 Build

→ 전체 업무 Regression

→ 외부 Tomcat 검증

→ CI 전환

→ 개발자 환경 전환

→ 구 JDK 폐기
```

### 19.2 Gradle Upgrade

Wrapper만 변경하고 끝내지 않습니다.

확인:

```
Plugin 호환성

Deprecated 설정

Dependency Resolution

Test 실행

WAR 구조

CI Cache
```

### 19.3 IDE Upgrade

IDE Upgrade는 Source 계약 변경은 아니지만 다음 영향을 확인합니다.

```
JDK 인식

Gradle Import

Annotation Processing

Encoding

Formatter

Git Plugin
```

### 19.4 Local Profile 변경

```
신규 설정 Key

기존 기본값

환경변수명

Secret 위치

README

CI Test Profile
```

을 함께 변경합니다.

### 19.5 개발용 보안 우회기능 폐기

```
사용현황 확인

→ 운영 Profile 차단 확인

→ 대체 개발 JWT 제공

→ Source 제거

→ Security Test

→ 문서 폐기
```

### 19.6 구 Tomcat 폐기

```
배포 WAR 없음

설정 Backup

Log 보존

Secret 제거

Service 등록 해제

디렉터리 삭제

자산대장 반영
```

## 20. 시사점

### 20.1 핵심 아키텍처 판단

제17부의 핵심은 다음과 같습니다.

```
개발환경 구축
= IDE에서 프로젝트가 열리는 상태
```

가 아닙니다.

```
개발환경 구축
= 프로젝트 표준 Version으로
  Build·실행·거래호출·디버깅·테스트·CI 재현이
  모두 가능한 상태
```

입니다.

### 20.2 주요 위험

| 위험 | 영향 |
| --- | --- |
| 개발자별 JDK 차이 | Build 불일치 |
| 시스템 Gradle 사용 | 재현성 저하 |
| IDE 수동 Library | CI 실패 |
| 운영 설정 복사 | 보안사고 |
| Local 보안 우회 유출 | 인증 무력화 |
| H2만으로 SQL 확정 | 운영 DB 오류 |
| 업무 WAR 직접 의존 | 배포경계 붕괴 |
| Mapper Resource 누락 | Runtime 오류 |
| Debug 중 Timeout | 잘못된 분석 |
| SQL Parameter 전체 로그 | 개인정보 노출 |
| 테스트 제외 Build | 결함 유입 |
| 개인환경 의존 Test | CI 불안정 |
| 동일 Artifact 미사용 | 환경별 차이 |

### 20.3 우선 보완 과제

```
1. 개발환경 Version Matrix 확정

2. Gradle Wrapper 표준화

3. 신규 개발자 자동 점검 Script

4. Local Profile Template

5. Secret 주입 표준

6. 개발용 JWT 발급절차

7. Local DB 초기화 Script

8. 업무모듈 실행 Template

9. 대표 ServiceId Smoke Test

10. Mapper Resource 자동검증

11. Debug·Trace 가이드

12. IDE–CI 정합성 Gate
```

### 20.4 중장기 발전 방향

```
수동 도구 설치
→ 자동 개발환경 구성

개인별 Profile 작성
→ 표준 Local Template

수동 DB 초기화
→ Migration 자동 적용

개발자별 실행방법
→ 공통 Run Configuration

수동 Smoke Test
→ 자동 거래검증

개인 경험 기반 Debug
→ Trace 기반 진단도구

로컬·CI 차이
→ Containerized Development Environment
```

개발환경 자동화가 도입되더라도 프로젝트 구조와 실행 흐름을 이해하지 못한 채 도구만 사용하는 상태가 되지 않도록 교육이 병행되어야 합니다.

## 21. 마무리말

제17부에서 가장 중요하게 기억해야 할 개발환경 구축 순서는 다음과 같습니다.

```
프로젝트 기준 확인

→ JDK 설치

→ Git Clone

→ Gradle Wrapper Build

→ IDE Import

→ Local Profile

→ DB·인증 설정

→ 업무모듈 bootRun

→ Health Check

→ 표준 ServiceId 호출

→ Controller부터 Mapper까지 Debug

→ Clean Build·Test

→ Commit·Code Review

→ CI Pipeline

→ 동일 Artifact 생성
```

개발자가 거래를 디버깅할 때 따라가야 할 순서는 다음과 같습니다.

```
HTTP 요청

→ Controller

→ TCF.process()

→ STF

→ Timeout Executor

→ Dispatcher

→ Handler

→ Facade

→ Service

→ Rule

→ DAO

→ Mapper

→ DB

→ ETF

→ 표준 응답
```

초보 개발자가 개발환경 구축을 완료하기 전에 마지막으로 확인해야 할 질문은 다음과 같습니다.

```
명령행·Gradle·IDE·Tomcat이 같은 JDK를 사용하는가?

시스템 Gradle이 아니라 Wrapper를 사용하는가?

전체 clean build가 성공하는가?

Root 멀티모듈 프로젝트를 올바르게 Import했는가?

로컬 Secret이 Git 저장소 밖에 있는가?

운영 계정과 운영 Key를 사용하지 않는가?

업무모듈과 공통모듈의 실행 역할을 구분하는가?

업무별 Port와 Context Path가 정확한가?

Health뿐 아니라 대표 ServiceId도 성공하는가?

ServiceId가 어느 Handler로 연결되는지 확인했는가?

Facade의 트랜잭션이 실제로 적용되는가?

Mapper XML과 SQL ID가 올바르게 포함되는가?

Debug 중 Timeout과 중복 DB 실행을 주의했는가?

민감정보가 Console과 SQL Log에 출력되지 않는가?

로컬 Clean Build와 CI 결과가 동일한가?

Source·Test·설계서가 함께 변경되었는가?
```

이 질문에 답할 수 있다면 단순히 IDE에서 소스를 열어 보는 수준을 넘어, NSIGHT TCF 프로젝트를 표준 방식으로 Build하고 실행하며, 첫 거래의 전체 흐름을 직접 추적하고 문제를 스스로 해결할 수 있는 개발자가 될 수 있습니다.

