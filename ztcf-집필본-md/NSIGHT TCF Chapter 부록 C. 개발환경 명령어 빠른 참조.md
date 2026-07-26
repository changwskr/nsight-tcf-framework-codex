<!-- source: ztcf-집필본/NSIGHT TCF Chapter 부록 C. 개발환경 명령어 빠른 참조.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 확장 부록 C. 개발환경 명령어 빠른 참조

## 도입 전 안내말

이 부록은 NSIGHT TCF 개발 과정에서 자주 사용하는 명령을 빠르게 찾기 위한 실무 참조표다.

대상 작업은 다음과 같다.

\`\`\`text id=“cmdC001” JDK·Gradle 버전 확인

Git Clone·Branch·Diff

멀티모듈 Build·Test

업무 WAR bootRun

표준 거래 호출

Breakpoint·Remote Debug

WAR 생성·내용 확인

Tomcat 배포·기동

로그 검색

Port·Process 확인

Thread·Heap·GC 진단

DB 연결·검증

CI/CD Script 실행

Artifact Checksum 확인

장애 원인 범위 축소



명령어는 외우는 것이 목적이 아니다.

\`\`\`text id="cmdC002"
어떤 환경에서

어떤 기준 Version으로

어느 디렉터리에서

어떤 Module을 대상으로

무슨 결과를 확인하기 위해

해당 명령을 실행하는가

를 이해해야 한다.

명령이 성공했다는 사실만으로 업무 개발이 완료된 것도 아니다.

\`\`\`text id=“cmdC003” BUILD SUCCESSFUL ≠ 업무 거래 정상

Process 실행 ≠ ServiceId 정상

Health UP ≠ 실제 SQL 정상

WAR 복사 ≠ 운영 배포 완료



명령 실행 후에는 반드시 다음 증거를 함께 확인한다.

\`\`\`text id="cmdC004"
Exit Code

Console Log

Application Log

HTTP 결과

DB 결과

거래 GUID·TraceId

생성 Artifact

Checksum

Test Report

# 문서 개요

## 목적

본 부록의 목적은 NSIGHT TCF 개발자가 소스 수령부터 빌드·로컬 실행·디버깅·WAR 배포·장애 진단까지 반복적으로 사용하는 명령을 운영 가능한 기준으로 제공하는 것이다.

## 적용범위

| 영역 | 적용 대상 |
| --- | --- |
| 운영체제 | Windows 10·11, Linux, macOS |
| Shell | CMD, PowerShell, Bash |
| Java | JDK 21 기준 |
| Build | Gradle 멀티모듈 |
| IDE | Eclipse·STS 계열 |
| 실행 | Spring Boot bootRun |
| 통합검증 | Tomcat WAR |
| 호출 | curl·PowerShell |
| 데이터 | Oracle·H2·SQL Client |
| 진단 | JVM·Thread·Port·Log |
| 배포 | tcf-scripts, tcf-cicd |
| 형상관리 | Git |
| 품질 | Test·Artifact·Checksum |

## 대상 독자

\`\`\`text id=“cmdC005” 신규 개발자

업무 개발자

프레임워크 개발자

테스트 담당자

DevOps 담당자

WAS 운영자

장애 분석 담당자


\## 선행조건

\`\`\`text id="cmdC006"
프로젝트 저장소 접근권한

JDK 21 설치

Git 설치

승인된 Gradle 설치 또는 Gradle Wrapper

Eclipse·STS 설치

업무 DB 또는 로컬 H2 설정

포트 사용권한

환경별 Secret 주입방법

# 핵심 관점

text id="cmdC007" 명령어의 정확성보다 중요한 것은 같은 소스·같은 JDK·같은 Gradle·같은 Profile로 다른 개발자도 같은 결과를 재현하는 것이다.

# 현재 프로젝트 명령 사용 시 우선 확인사항

## 1\. 현재 Branch를 기준으로 판단한다

문서보다 다음 파일을 우선한다.

\`\`\`text id=“cmdC008” settings.gradle

Root build.gradle

각 Module build.gradle

gradle-wrapper.properties

application-\*.yml

tcf-scripts

tcf-cicd

현재 Branch의 README


\## 2. Wrapper 존재 여부를 확인한다

Gradle Wrapper의 정상 구성은 다음 네 종류다.

\`\`\`text id="cmdC009"
gradlew

gradlew.bat

gradle/wrapper/gradle-wrapper.jar

gradle/wrapper/gradle-wrapper.properties

gradle-wrapper.properties만 있고 실행파일·JAR가 없으면 Wrapper가 완전하지 않다.

이 경우 현재 소스 Script처럼 시스템 Gradle을 사용할 수 있지만, 팀 표준 Version을 별도로 통제해야 한다.

## 3\. 현재 Script의 Gradle 탐색순서

현재 run-local·deploy Script의 기본 탐색순서는 다음과 같다.

\`\`\`text id=“cmdC010” GRADLE\_HOME\_OVERRIDE

→ GRADLE\_HOME

→ PATH의 gradle



일부 Windows Module Script에는 특정 PC의 Gradle 경로가 기본값으로 들어 있을 수 있다.

\`\`\`text id="cmdC011"
C:\\...\\gradle-8.10.1

해당 개인 경로를 그대로 복사하지 말고 환경변수로 재정의한다.

## 4\. 현재 JDK 기준

현재 개발 Script는 JDK 21 환경을 맞추는 env-jdk21.bat를 포함한다.

따라서 다음 네 위치의 Java Version을 일치시킨다.

\`\`\`text id=“cmdC012” 명령행 Java

Gradle JVM

Eclipse Project JRE

Tomcat JAVA\_HOME


\## 5. Module 개수는 Script 설명보다 실제 목록을 확인한다

현재 자료에는 Script·문서별로 다음 표현이 혼재할 수 있다.

\`\`\`text id="cmdC013"
9개 업무 Service

10개 업무·OM WAR

17개 Context

19개 전체 WAR

실제 대상은 다음으로 확인한다.

\`\`\`text id=“cmdC014” gradle projects

settings.gradle

배포 Script의 Module 목록

Artifact Directory

Tomcat webapps



Script에 표시된 설명 문구의 개수만 신뢰하지 않는다.

\---

\# C.1 Shell별 기본 표기

\## C.1.1 명령 구분

| 구분 | Windows CMD | PowerShell | Linux·macOS Bash |
|---|---|---|---|
| 현재 경로 | \`cd\` | \`Get-Location\` | \`pwd\` |
| 파일 목록 | \`dir\` | \`Get-ChildItem\` | \`ls -la\` |
| 환경변수 조회 | \`%JAVA\_HOME%\` | \`$env:JAVA\_HOME\` | \`$JAVA\_HOME\` |
| 환경변수 설정 | \`set "A=B"\` | \`$env:A="B"\` | \`export A=B\` |
| PATH 구분자 | \`;\` | \`;\` | \`:\` |
| 경로 구분 | \`\\\` | \`\\\`·\`/\` | \`/\` |
| 줄 계속 | \`^\` | \`\` \` \`\` | \`\\\` |
| 명령 성공코드 | \`%ERRORLEVEL%\` | \`$LASTEXITCODE\` | \`$?\`·\`$PIPESTATUS\` |

\## C.1.2 프로젝트 Root 이동

\### CMD

\`\`\`bat id="cmdC015"
cd /d C:\\workspace\\nsight-tcf-framework

### PowerShell

powershell id="cmdC016" Set-Location C:\\workspace\\nsight-tcf-framework

### Bash

bash id="cmdC017" cd ~/workspace/nsight-tcf-framework

Gradle 명령은 특별한 이유가 없다면 프로젝트 Root에서 실행한다.

# C.2 JDK 환경 확인

## C.2.1 Java Version

공통:

bash id="cmdC018" java -version javac -version

기대:

\`\`\`text id=“cmdC019” java 21.x

javac 21.x



Java Runtime과 Compiler Version이 달라서는 안 된다.

\---

\## C.2.2 Java 실행파일 위치

\### CMD

\`\`\`bat id="cmdC020"
where java
where javac
echo %JAVA\_HOME%

### PowerShell

powershell id="cmdC021" where.exe java where.exe javac $env:JAVA\_HOME Get-Command java

### Bash

bash id="cmdC022" which java which javac echo "$JAVA\_HOME" readlink -f "$(which java)"

여러 경로가 표시되면 PATH 우선순위를 확인한다.

## C.2.3 현재 Shell에서 JDK 21 설정

### CMD

\`\`\`bat id=“cmdC023” set “JAVA\_HOME=C:FilesAdoptium.4” set “PATH=%JAVA\_HOME%;%PATH%”

java -version javac -version


\### PowerShell

\`\`\`powershell id="cmdC024"
$env:JAVA\_HOME = "C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.4"
$env:Path = "$env:JAVA\_HOME\\bin;$env:Path"

java -version
javac -version

### Bash

\`\`\`bash id=“cmdC025” export JAVA\_HOME=/opt/jdk-21 export PATH=“PATH”

java -version javac -version



Shell에서 설정한 값은 일반적으로 해당 Terminal Session에만 적용된다.

\---

\## C.2.4 프로젝트 제공 Windows Script

\`\`\`bat id="cmdC026"
call scripts\\env-jdk21.bat
java -version

call을 생략하고 Batch Script를 실행하면 제어가 돌아오지 않거나 환경변수가 현재 Script에 정상 적용되지 않을 수 있다.

## C.2.5 Gradle이 사용하는 JVM 확인

bash id="cmdC027" gradle --version

Wrapper가 정상 구성된 경우:

### Windows

bat id="cmdC028" gradlew.bat --version

### Bash

bash id="cmdC029" ./gradlew --version

확인:

\`\`\`text id=“cmdC030” Gradle Version

Launcher JVM

Daemon JVM

Java Home

Operating System



명령행의 \`java -version\`이 21이어도 Gradle JVM이 17일 수 있다.

\---

\## C.2.6 Gradle Daemon 재기동

JDK·환경변수 변경 후:

\`\`\`bash id="cmdC031"
gradle --stop
gradle --status
gradle --version

Wrapper:

bash id="cmdC032" ./gradlew --stop ./gradlew --status

# C.3 Gradle 환경 설정

## C.3.1 Gradle 위치 확인

### CMD

bat id="cmdC033" where gradle echo %GRADLE\_HOME% echo %GRADLE\_HOME\_OVERRIDE%

### PowerShell

powershell id="cmdC034" where.exe gradle $env:GRADLE\_HOME $env:GRADLE\_HOME\_OVERRIDE

### Bash

bash id="cmdC035" which gradle echo "$GRADLE\_HOME" echo "$GRADLE\_HOME\_OVERRIDE"

## C.3.2 Session 단위 Gradle 설정

### CMD

bat id="cmdC036" set "GRADLE\_HOME\_OVERRIDE=C:\\tools\\gradle-8.10.1" set "PATH=%GRADLE\_HOME\_OVERRIDE%\\bin;%PATH%" gradle --version

### PowerShell

powershell id="cmdC037" $env:GRADLE\_HOME\_OVERRIDE = "C:\\tools\\gradle-8.10.1" $env:Path = "$env:GRADLE\_HOME\_OVERRIDE\\bin;$env:Path" gradle --version

### Bash

bash id="cmdC038" export GRADLE\_HOME\_OVERRIDE=/opt/gradle-8.10.1 export PATH="$GRADLE\_HOME\_OVERRIDE/bin:$PATH" gradle --version

프로젝트가 승인한 Gradle Version은 현재 Branch의 Wrapper 또는 CI 설정을 기준으로 확정한다.

# C.4 Git 빠른 참조

## C.4.1 Git 환경 확인

bash id="cmdC039" git --version git config --global --list

사용자정보:

bash id="cmdC040" git config --global user.name git config --global user.email

설정:

bash id="cmdC041" git config --global user.name "홍길동" git config --global user.email "user@example.com"

사내 정책에 맞는 이름과 이메일을 사용한다.

## C.4.2 Clone

bash id="cmdC042" git clone <repository-url> cd nsight-tcf-framework

특정 Branch:

bash id="cmdC043" git clone -b develop <repository-url>

Clone 후:

bash id="cmdC044" git remote -v git branch --show-current git status

## C.4.3 Remote 최신화

bash id="cmdC045" git fetch --all --prune

Remote Branch 목록:

bash id="cmdC046" git branch -r

전체 Branch:

bash id="cmdC047" git branch -a

## C.4.4 Branch 생성·이동

bash id="cmdC048" git switch develop git pull --ff-only git switch -c feature/REQ-CT-RSV-003-create

기존 Git Version:

bash id="cmdC049" git checkout develop git checkout -b feature/REQ-CT-RSV-003-create

## C.4.5 현재 변경 확인

bash id="cmdC050" git status git diff git diff --staged

파일별 요약:

bash id="cmdC051" git diff --stat

변경 파일명:

bash id="cmdC052" git diff --name-status

## C.4.6 Commit

bash id="cmdC053" git add <file> git status git commit -m "feat(ct): add reservation create transaction"

모든 변경 추가:

bash id="cmdC054" git add .

git add . 전에 git status와 git diff로 Secret·로그·Build 산출물이 포함되지 않았는지 확인한다.

## C.4.7 Push

bash id="cmdC055" git push -u origin feature/REQ-CT-RSV-003-create

이후:

bash id="cmdC056" git push

## C.4.8 Commit 이력

bash id="cmdC057" git log --oneline --decorate --graph -20

특정 파일:

bash id="cmdC058" git log --oneline -- path/to/file

파일 변경내용 포함:

bash id="cmdC059" git log -p -- path/to/file

누가 변경했는지:

bash id="cmdC060" git blame path/to/file

git blame은 비난 목적이 아니라 변경배경과 관련 Commit을 찾는 데 사용한다.

## C.4.9 다른 Branch와 비교

bash id="cmdC061" git diff develop...HEAD

변경 파일:

bash id="cmdC062" git diff --name-status develop...HEAD

Commit 차이:

bash id="cmdC063" git log --oneline develop..HEAD

## C.4.10 임시 보관

bash id="cmdC064" git stash push -u -m "WIP reservation update" git stash list git stash show -p stash@{0} git stash pop

stash pop 충돌 시 자동으로 다시 실행하지 말고 충돌파일을 확인한다.

## C.4.11 변경 복원

작업 디렉터리 파일 복원:

bash id="cmdC065" git restore path/to/file

Stage 취소:

bash id="cmdC066" git restore --staged path/to/file

이전 Git:

bash id="cmdC067" git checkout -- path/to/file

## C.4.12 위험 명령

bash id="cmdC068" git reset --hard git clean -fd git push --force

위 명령은 작업과 파일을 복구하기 어렵게 만들 수 있다.

실행 전:

bash id="cmdC069" git status git diff git stash push -u

를 수행하고 팀 정책을 확인한다.

공유 Branch에는 --force 대신 필요한 경우 다음을 검토한다.

bash id="cmdC070" git push --force-with-lease

## C.4.13 줄바꿈 설정 확인

bash id="cmdC071" git config --show-origin --get core.autocrlf git check-attr -a -- path/to/file

변경하지 않은 수백 개 파일이 수정된 것으로 보이면 .gitattributes, Encoding, CRLF·LF를 먼저 확인한다.

# C.5 소스 탐색 명령

## C.5.1 Git 추적 파일에서 검색

bash id="cmdC072" git grep -n "CT.Reservation.create"

Class 검색:

bash id="cmdC073" git grep -n "class CtReservationHandler"

Mapper ID 검색:

bash id="cmdC074" git grep -n "selectReservationList"

Table 검색:

bash id="cmdC075" git grep -n "CT\_CONTACT\_RESERVATION"

git grep은 build, 로그, 외부 Library를 제외하고 Git 관리대상에서 검색하는 데 유용하다.

## C.5.2 ripgrep

bash id="cmdC076" rg -n "CT\\.Reservation\\.create" rg -n "selectReservationList" rg -n "CT\_CONTACT\_RESERVATION"

Java만:

bash id="cmdC077" rg -n "Reservation" -g "\*.java"

Mapper XML만:

bash id="cmdC078" rg -n "selectReservationList" -g "\*.xml"

설정파일:

bash id="cmdC079" rg -n "server.port|context-path|profiles" -g "\*.yml" -g "\*.yaml"

## C.5.3 Windows 기본 검색

### CMD

bat id="cmdC080" findstr /S /N /I /C:"CT.Reservation.create" \*.java \*.xml \*.yml

### PowerShell

powershell id="cmdC081" Get-ChildItem -Recurse -File | Select-String -Pattern "CT\\.Reservation\\.create"

Java만:

powershell id="cmdC082" Get-ChildItem -Recurse -Filter \*.java | Select-String -Pattern "CtReservationHandler"

## C.5.4 Linux grep

bash id="cmdC083" grep -RIn --include="\*.java" \\ "CtReservationHandler" .

로그·Build·Git 제외:

bash id="cmdC084" grep -RIn \\ --exclude-dir=.git \\ --exclude-dir=build \\ --exclude-dir=logs \\ "CT.Reservation.create" .

# C.6 Gradle Project 구조 확인

## C.6.1 참여 Module

bash id="cmdC085" gradle projects

Wrapper:

bash id="cmdC086" ./gradlew projects

현재 Branch에 ct-service가 없다면 다음 명령은 실패한다.

bash id="cmdC087" gradle :ct-service:build

먼저 settings.gradle 편입 여부를 확인한다.

## C.6.2 Task 목록

전체:

bash id="cmdC088" gradle tasks --all

특정 Module:

bash id="cmdC089" gradle :sv-service:tasks --all

Task가 존재하는지 확인:

bash id="cmdC090" gradle tasks --all | grep bootWar

PowerShell:

powershell id="cmdC091" gradle tasks --all | Select-String "bootWar"

## C.6.3 Task 실행계획

bash id="cmdC092" gradle :sv-service:build --dry-run

Dry Run의 SKIPPED는 실제 Task 실패가 아니라 실행계획만 출력했다는 의미다.

# C.7 Gradle Build·Test 빠른 참조

## C.7.1 전체 Clean Build

bash id="cmdC093" gradle clean build

Wrapper:

bash id="cmdC094" ./gradlew clean build

Windows Wrapper:

bat id="cmdC095" gradlew.bat clean build

공통 Module 변경이나 CI 재현 시 사용한다.

## C.7.2 단일 Module Build

bash id="cmdC096" gradle :sv-service:build

Clean 포함:

bash id="cmdC097" gradle clean :sv-service:build

주의:

\`\`\`text id=“cmdC098” Root clean → 모든 Module의 build 디렉터리 삭제

:sv-service:clean → SV Module만 삭제



단일 Module Clean:

\`\`\`bash id="cmdC099"
gradle :sv-service:clean :sv-service:build

## C.7.3 공통 Module 변경 후 Build

bash id="cmdC100" gradle \\ :tcf-util:build \\ :tcf-core:build \\ :tcf-web:build \\ :sv-service:build

실제 Gradle Task Graph가 필요한 의존 Module을 자동 처리하더라도, 검증 목적상 대상 Module을 명시할 수 있다.

## C.7.4 전체 Test

bash id="cmdC101" gradle test

단일 Module:

bash id="cmdC102" gradle :sv-service:test

## C.7.5 특정 Test Class

bash id="cmdC103" gradle :sv-service:test \\ --tests "\*CtReservationServiceTest"

특정 Method:

bash id="cmdC104" gradle :sv-service:test \\ --tests "\*CtReservationServiceTest.createReservation"

Windows CMD 한 줄:

bat id="cmdC105" gradle :sv-service:test --tests "\*CtReservationServiceTest"

## C.7.6 Test 강제 재실행

Gradle이 Up-to-date로 판단했지만 다시 실행해야 하는 경우:

bash id="cmdC106" gradle :sv-service:test --rerun-tasks

## C.7.7 Test Report

일반 위치:

text id="cmdC107" {module}/build/reports/tests/test/index.html

Windows:

bat id="cmdC108" start sv-service\\build\\reports\\tests\\test\\index.html

PowerShell:

powershell id="cmdC109" Start-Process \` .\\sv-service\\build\\reports\\tests\\test\\index.html

macOS:

bash id="cmdC110" open sv-service/build/reports/tests/test/index.html

Linux:

bash id="cmdC111" xdg-open sv-service/build/reports/tests/test/index.html

## C.7.8 Test 제외

bash id="cmdC112" gradle :sv-service:build -x test

이 명령은 Build 오류 범위를 분리하는 임시 진단용으로만 사용할 수 있다.

다음에는 사용하지 않는다.

\`\`\`text id=“cmdC113” 운영 Artifact

Release Build

Merge 승인

품질 완료 증거


\---

\## C.7.9 상세 Build Log

\`\`\`bash id="cmdC114"
gradle :sv-service:build --stacktrace

더 상세:

bash id="cmdC115" gradle :sv-service:build --info

전체 Stack Trace:

bash id="cmdC116" gradle :sv-service:build --full-stacktrace

최대 상세:

bash id="cmdC117" gradle :sv-service:build --debug

\--debug 출력에는 경로·환경변수·접속정보가 포함될 수 있으므로 외부 공유 전 마스킹한다.

## C.7.10 Dependency 확인

bash id="cmdC118" gradle :sv-service:dependencies

Runtime Classpath:

bash id="cmdC119" gradle :sv-service:dependencies \\ --configuration runtimeClasspath

특정 Library 유입경로:

bash id="cmdC120" gradle :sv-service:dependencyInsight \\ --dependency jackson-databind \\ --configuration runtimeClasspath

## C.7.11 Dependency Cache

강제 재조회:

bash id="cmdC121" gradle :sv-service:build \\ --refresh-dependencies

Offline:

bash id="cmdC122" gradle :sv-service:build --offline

근거 없이 매번 --refresh-dependencies를 사용하지 않는다.

## C.7.12 Gradle Daemon

bash id="cmdC123" gradle --status gradle --stop

Daemon 사용 금지 실행:

bash id="cmdC124" gradle :sv-service:build --no-daemon

CI 재현 또는 Daemon 의심 시 사용한다.

# C.8 WAR 생성과 확인

## C.8.1 단일 WAR 생성

bash id="cmdC125" gradle :sv-service:bootWar

상담예약 Module 편입 후:

bash id="cmdC126" gradle :ct-service:bootWar

현재 Branch에 ct-service가 등록되지 않았다면 실행하지 않는다.

## C.8.2 업무 WAR 일괄 Build

현재 Root Task가 존재하는 경우:

bash id="cmdC127" gradle buildBusinessWars

통합 Tomcat 전체 대상 Task가 존재하는 경우:

bash id="cmdC128" gradle buildZtomcatWars

정확한 포함 Module은 다음으로 확인한다.

bash id="cmdC129" gradle buildBusinessWars --dry-run gradle buildZtomcatWars --dry-run

## C.8.3 생성 WAR 확인

### Windows

bat id="cmdC130" dir sv-service\\build\\libs

### PowerShell

powershell id="cmdC131" Get-ChildItem \` .\\sv-service\\build\\libs

### Bash

bash id="cmdC132" ls -lh sv-service/build/libs

## C.8.4 WAR 내부 파일 확인

bash id="cmdC133" jar tf sv-service/build/libs/sv.war

Mapper 포함 여부:

### PowerShell

powershell id="cmdC134" jar tf .\\sv-service\\build\\libs\\sv.war | Select-String "Mapper.xml"

### Bash

bash id="cmdC135" jar tf sv-service/build/libs/sv.war | grep "Mapper.xml"

Class 포함 여부:

bash id="cmdC136" jar tf sv-service/build/libs/sv.war | grep "SvCustomerHandler.class"

설정 포함 여부:

bash id="cmdC137" jar tf sv-service/build/libs/sv.war | grep "application"

WAR 안에 운영 Secret이 포함되지 않았는지도 확인한다.

## C.8.5 Checksum

### Windows PowerShell

powershell id="cmdC138" Get-FileHash \` .\\sv-service\\build\\libs\\sv.war \` -Algorithm SHA256

### Linux·macOS

bash id="cmdC139" sha256sum sv-service/build/libs/sv.war

macOS 대안:

bash id="cmdC140" shasum -a 256 \\ sv-service/build/libs/sv.war

# C.9 로컬 bootRun

## C.9.1 단일 업무 실행

bash id="cmdC141" gradle :sv-service:bootRun

현재 Script 기준 SV 포트:

text id="cmdC142" 8086

표준 거래 URL 예:

text id="cmdC143" http://localhost:8086/sv/online

실제 Port와 Context는 application-local.yml을 확인한다.

## C.9.2 Module 제공 Script

### Windows

bat id="cmdC144" sv-service\\scripts\\run-local.bat

### Bash

bash id="cmdC145" chmod +x sv-service/scripts/run-local.sh ./sv-service/scripts/run-local.sh

Script는 Gradle 위치와 프로젝트 Root를 찾아 해당 Module의 bootRun을 실행한다.

## C.9.3 통합 실행 Script

### Windows

bat id="cmdC146" tcf-scripts\\run-local.bat sv

두 개 업무:

bat id="cmdC147" tcf-scripts\\run-local.bat sv ic

전체 업무와 OM:

bat id="cmdC148" tcf-scripts\\run-local.bat all

### Bash

bash id="cmdC149" ./tcf-scripts/run-local.sh sv

복수:

bash id="cmdC150" ./tcf-scripts/run-local.sh sv ic

전체:

bash id="cmdC151" ./tcf-scripts/run-local.sh all

전체 실행은 많은 JVM·Port·DB Connection을 사용하므로 PC 자원을 확인한다.

## C.9.4 주요 로컬 포트

현재 Script·문서에서 확인되는 대표값이다.

| 업무·Module | bootRun Port | 기본 Context |
| --- | --- | --- |
| ic-service | 8082 | /ic |
| pc-service | 8083 | /pc |
| ms-service | 8085 | /ms |
| sv-service | 8086 | /sv |
| pd-service | 8087 | /pd |
| eb-service | 8089 | /eb |
| ep-service | 8090 | /ep |
| ss-service | 8093 | /ss |
| mg-service | 8096 | /mg |
| tcf-om | 8097 | /om |
| tcf-batch | 8098 | /batch 기준 확인 |
| tcf-ui | 8099 | UI |
| tcf-gateway | 8100 | Gateway |
| tcf-uj | 8102 | Gateway 경유 UI |
| tcf-jwt | 8110 | JWT·JWKS |

ct-service Port는 현재 배포·실행 목록에 공식 배정된 값이 확인된 후 사용한다.

임의로 다른 Module과 같은 Port를 사용하지 않는다.

## C.9.5 Profile 지정

bash id="cmdC152" gradle :sv-service:bootRun \\ --args="--spring.profiles.active=local"

환경변수 방식:

### CMD

bat id="cmdC153" set "SPRING\_PROFILES\_ACTIVE=local" gradle :sv-service:bootRun

### PowerShell

powershell id="cmdC154" $env:SPRING\_PROFILES\_ACTIVE = "local" gradle :sv-service:bootRun

### Bash

bash id="cmdC155" export SPRING\_PROFILES\_ACTIVE=local gradle :sv-service:bootRun

실행로그에서 실제 활성 Profile을 확인한다.

## C.9.6 Port 임시 변경

bash id="cmdC156" gradle :sv-service:bootRun \\ --args="--server.port=18086"

Context가 /sv라면:

text id="cmdC157" http://localhost:18086/sv/online

Port를 임시 변경했으면 UI·Gateway·외부 URL 설정도 함께 맞춰야 한다.

## C.9.7 실행 종료

Foreground Terminal:

text id="cmdC158" Ctrl + C

정상 종료로그와 DB Connection 반환을 확인한다.

Terminal 창을 강제 종료하는 것보다 Graceful Shutdown을 우선한다.

# C.10 Gateway·JWT·UI 통합 실행

## C.10.1 업무 직접 호출 구조

\`\`\`text id=“cmdC159” sv-service :8086

→ POST /sv/online



명령:

\`\`\`bash id="cmdC160"
gradle :sv-service:bootRun

## C.10.2 Gateway 경유 구조

대표 실행순서:

bash id="cmdC161" gradle :sv-service:bootRun gradle :tcf-om:bootRun gradle :tcf-jwt:bootRun gradle :tcf-gateway:bootRun gradle :tcf-uj:bootRun

각 명령은 별도 Terminal에서 실행한다.

현재 대표 Port:

text id="cmdC162" SV 8086 OM 8097 Gateway 8100 UJ 8102 JWT 8110

## C.10.3 Module Script

Gateway:

### Windows

bat id="cmdC163" tcf-gateway\\scripts\\run-local.bat

### Bash

bash id="cmdC164" ./tcf-gateway/scripts/run-local.sh

JWT:

bat id="cmdC165" tcf-jwt\\scripts\\run-local.bat

bash id="cmdC166" ./tcf-jwt/scripts/run-local.sh

OM:

bat id="cmdC167" tcf-om\\scripts\\run-local.bat

bash id="cmdC168" ./tcf-om/scripts/run-local.sh

# C.11 HTTP·curl 빠른 참조

## C.11.1 제공 Sample Script

### Windows

bat id="cmdC169" tcf-scripts\\curl-sample.bat sv

### Bash

bash id="cmdC170" ./tcf-scripts/curl-sample.sh sv

Script는 현재 업무코드별 Port와 Sample JSON을 사용한다.

## C.11.2 curl 기본 호출

Linux·macOS·Git Bash:

bash id="cmdC171" curl -sS \\ -X POST \\ "http://localhost:8086/sv/online" \\ -H "Content-Type: application/json" \\ --data-binary "@request.json"

Windows PowerShell에서는 curl이 Alias일 수 있으므로 명시적으로 curl.exe를 사용하는 것이 안전하다.

powershell id="cmdC172" curl.exe -sS \` -X POST \` "http://localhost:8086/sv/online" \` -H "Content-Type: application/json" \` --data-binary "@request.json"

Windows CMD:

bat id="cmdC173" curl.exe -sS -X POST ^ "http://localhost:8086/sv/online" ^ -H "Content-Type: application/json" ^ --data-binary "@request.json"

## C.11.3 상담예약 요청 예

json id="cmdC174" { "header": { "businessCode": "CT", "serviceId": "CT.Reservation.selectList", "transactionCode": "CT-INQ-0101", "guid": "G-LOCAL-CT-0001", "traceId": "T-LOCAL-CT-0001", "channelId": "DEV" }, "body": { "customerNo": "C000001", "fromDate": "2026-07-01", "toDate": "2026-07-31", "pageSize": 20 } }

ct-service가 실제로 편입되고 Port·Context가 확정된 후 호출한다.

## C.11.4 Header Token 포함

bash id="cmdC175" curl -sS \\ -X POST \\ "http://localhost:8100/sv/online" \\ -H "Content-Type: application/json" \\ -H "Authorization: Bearer ${ACCESS\_TOKEN}" \\ --data-binary "@request.json"

Token 원문을 Shell History·공유 문서·화면 캡처에 남기지 않는다.

## C.11.5 HTTP Status 포함

bash id="cmdC176" curl -sS \\ -o response.json \\ -w "\\nHTTP\_STATUS=%{http\_code}\\n" \\ -X POST \\ "http://localhost:8086/sv/online" \\ -H "Content-Type: application/json" \\ --data-binary "@request.json"

## C.11.6 상세 연결정보

bash id="cmdC177" curl -v \\ -X POST \\ "http://localhost:8086/sv/online" \\ -H "Content-Type: application/json" \\ --data-binary "@request.json"

\-v 출력에는 Cookie·Authorization 등이 포함될 수 있으므로 공유 전 제거한다.

## C.11.7 요청시간 측정

bash id="cmdC178" curl -sS \\ -o /dev/null \\ -w "connect=%{time\_connect}\\nstart=%{time\_starttransfer}\\ntotal=%{time\_total}\\nstatus=%{http\_code}\\n" \\ "http://localhost:8086/sv/actuator/health"

Windows에서는 출력 대상 /dev/null 대신 NUL을 사용할 수 있다.

## C.11.8 PowerShell Invoke-RestMethod

\`\`powershell id="cmdC179" $body = Get-Content ..json \` -Raw

$response = Invoke-RestMethod -Method Post -Uri “http://localhost:8086/sv/online” -ContentType "application/json; charset=utf-8" -Body $body

$response | ConvertTo-Json -Depth 20


\---

\## C.11.9 JSON 보기

\`jq\`가 설치된 경우:

\`\`\`bash id="cmdC180"
curl -sS \\
\-X POST \\
"http://localhost:8086/sv/online" \\
\-H "Content-Type: application/json" \\
\--data-binary "@request.json" |
jq .

PowerShell:

powershell id="cmdC181" Get-Content .\\response.json -Raw | ConvertFrom-Json | ConvertTo-Json -Depth 20

# C.12 Health Check

## C.12.1 기본 Health

bash id="cmdC182" curl -sS \\ "http://localhost:<port>/<context>/actuator/health"

예:

bash id="cmdC183" curl -sS \\ "http://localhost:8086/sv/actuator/health"

실제 Management Base Path와 Context를 설정파일에서 확인한다.

## C.12.2 Liveness

bash id="cmdC184" curl -sS \\ "http://localhost:<port>/<context>/actuator/health/liveness"

## C.12.3 Readiness

bash id="cmdC185" curl -sS \\ "http://localhost:<port>/<context>/actuator/health/readiness"

## C.12.4 응답 대기 제한

bash id="cmdC186" curl --connect-timeout 2 \\ --max-time 5 \\ -sS \\ "http://localhost:8086/sv/actuator/health"

Health가 UP이어도 ServiceId·Mapper·Table·권한은 별도 Smoke Test로 확인한다.

# C.13 Eclipse·STS 작업 참조

## C.13.1 Gradle Project Import

메뉴:

\`\`\`text id=“cmdC187” File

→ Import

→ Gradle

→ Existing Gradle Project

→ 프로젝트 Root 선택

→ Gradle JVM JDK 21 확인

→ Finish


\## C.13.2 Import 후 확인

\`\`\`text id="cmdC188"
Project Explorer에 모든 Module 표시

Gradle Tasks 표시

JRE System Library 21

Project Encoding UTF-8

Build 오류 없음

## C.13.3 Gradle 새로고침

\`\`\`text id=“cmdC189” 프로젝트 우클릭

→ Gradle

→ Refresh Gradle Project



Build 파일·Dependency·Module 변경 후 수행한다.

\## C.13.4 Project Clean

\`\`\`text id="cmdC190"
Project

→ Clean

→ Clean all projects

Eclipse Clean은 Gradle clean과 같은 작업이 아니다.

| Eclipse Clean | Gradle clean |
| --- | --- |
| IDE Compile 상태 재작성 | build/ 산출물 삭제 |
| IDE Builder | Gradle Task |
| IDE 오류 정리 | CI·명령행 재현 |

## C.13.5 Breakpoint

\`\`\`text id=“cmdC191” Java Editor 왼쪽 줄번호 영역 클릭

또는

Ctrl+Shift+B



권장 Breakpoint 위치:

\`\`\`text id="cmdC192"
OnlineTransactionController

TCF.process

STF.preProcess

TransactionDispatcher

업무 Handler

Facade

Service

Rule

DAO

Mapper 호출 직전

ETF

## C.13.6 Debug View 주요 기능

| 기능 | 단축키 |
| --- | --- |
| Resume | F8 |
| Step Into | F5 |
| Step Over | F6 |
| Step Return | F7 |
| Terminate | Ctrl+F2 |
| Toggle Breakpoint | Ctrl+Shift+B |

## C.13.7 조건부 Breakpoint

예:

text id="cmdC193" serviceId.equals("CT.Reservation.create")

모든 거래에서 멈추지 않고 대상 거래만 추적한다.

## C.13.8 Exception Breakpoint

\`\`\`text id=“cmdC194” Run

→ Add Java Exception Breakpoint

→ BusinessException 또는 DataAccessException



Caught Exception에서 지나치게 자주 멈추는 경우 조건과 Scope를 제한한다.

\---

\# C.14 Remote Debug

\## C.14.1 bootRun Debug

\`\`\`bash id="cmdC195"
gradle :sv-service:bootRun --debug-jvm

일반적으로 Application이 Debug 연결을 기다린다.

대표 Port:

text id="cmdC196" 5005

Console에 실제 Listening Port와 Suspend 상태가 표시되는지 확인한다.

## C.14.2 Eclipse Remote 연결

\`\`\`text id=“cmdC197” Run

→ Debug Configurations

→ Remote Java Application

→ Host localhost

→ Port 5005

→ Debug


\## C.14.3 다중 Module 주의

여러 Application을 동시에 Debug하면 모두 5005를 사용할 수 없다.

각 Module의 Debug Port를 다르게 구성하거나 한 번에 하나만 \`--debug-jvm\`으로 실행한다.

\## C.14.4 운영 Remote Debug 금지

운영 JVM에 외부 Debug Port를 무단 개방하지 않는다.

위험:

\`\`\`text id="cmdC198"
원격 코드실행

서비스 정지

민감정보 조회

Thread 장기정지

운영 진단은 jcmd, 로그, Metric, 승인된 관리도구를 우선한다.

# C.15 Tomcat 빠른 참조

## C.15.1 Tomcat 환경

### Windows

bat id="cmdC199" echo %CATALINA\_HOME% echo %CATALINA\_BASE% echo %JAVA\_HOME%

### Bash

bash id="cmdC200" echo "$CATALINA\_HOME" echo "$CATALINA\_BASE" echo "$JAVA\_HOME"

CATALINA\_HOME은 Tomcat 설치 Binary, CATALINA\_BASE는 Instance별 설정·로그·webapps 경로로 분리할 수 있다.

## C.15.2 Version 확인

### Windows

bat id="cmdC201" %CATALINA\_HOME%\\bin\\version.bat

### Bash

bash id="cmdC202" "$CATALINA\_HOME/bin/version.sh"

확인:

\`\`\`text id=“cmdC203” Tomcat Version

JVM Version

JAVA\_HOME

CATALINA\_BASE

OS


\---

\## C.15.3 Foreground 기동

Foreground 실행은 초기 오류 확인에 유용하다.

\### Windows

\`\`\`bat id="cmdC204"
%CATALINA\_HOME%\\bin\\catalina.bat run

### Bash

bash id="cmdC205" "$CATALINA\_HOME/bin/catalina.sh" run

Console에서 Spring Context·Mapper·DB 오류를 바로 확인할 수 있다.

## C.15.4 Background 기동

### Windows

bat id="cmdC206" %CATALINA\_HOME%\\bin\\startup.bat

### Bash

bash id="cmdC207" "$CATALINA\_HOME/bin/startup.sh"

## C.15.5 종료

### Windows

bat id="cmdC208" %CATALINA\_HOME%\\bin\\shutdown.bat

### Bash

bash id="cmdC209" "$CATALINA\_HOME/bin/shutdown.sh"

종료 후 Process와 Port가 실제로 내려갔는지 확인한다.

## C.15.6 프로젝트 Tomcat 배포 Script

### Windows

bat id="cmdC210" tcf-scripts\\deploy.bat sv

복수:

bat id="cmdC211" tcf-scripts\\deploy.bat sv om

업무·OM 기본 전체:

bat id="cmdC212" tcf-scripts\\deploy.bat all

### Bash

bash id="cmdC213" ./tcf-scripts/deploy.sh sv

복수:

bash id="cmdC214" ./tcf-scripts/deploy.sh sv om

전체:

bash id="cmdC215" ./tcf-scripts/deploy.sh all

Script는 일반적으로 다음을 수행한다.

\`\`\`text id=“cmdC216” 공통 Module Build

→ 업무 bootWar

→ 기존 Exploded Context 제거

→ webapps에 WAR 복사

→ 파일 존재 검증



자동 Redeploy와 Tomcat Restart 필요 여부는 실행상태와 공통 Library 변경 여부에 따라 판단한다.

\---

\## C.15.7 webapps 경로 재정의

\### Windows CMD

\`\`\`bat id="cmdC217"
set "TOMCAT\_WEBAPPS=D:\\tomcat\\webapps"
tcf-scripts\\deploy.bat sv

### PowerShell

powershell id="cmdC218" $env:TOMCAT\_WEBAPPS = "D:\\tomcat\\webapps" .\\tcf-scripts\\deploy.bat sv

### Bash

bash id="cmdC219" export TOMCAT\_WEBAPPS=/opt/tomcat/webapps ./tcf-scripts/deploy.sh sv

## C.15.8 수동 WAR 복사

프로젝트 Script를 우선 사용한다.

수동으로 검증할 때:

### Windows

bat id="cmdC220" copy /Y ^ sv-service\\build\\libs\\sv.war ^ %CATALINA\_BASE%\\webapps\\sv.war

### Bash

bash id="cmdC221" cp -f \\ sv-service/build/libs/sv.war \\ "$CATALINA\_BASE/webapps/sv.war"

다음도 확인한다.

\`\`\`text id=“cmdC222” 기존 Exploded Directory

WAR 소유권·권한

Checksum

Context 중복

Tomcat 재기동 필요성


\---

\# C.16 CI/CD Script 빠른 참조

\## C.16.1 도움말

\### Windows

\`\`\`bat id="cmdC223"
tcf-cicd\\scripts\\cicd-deploy.bat -Help

PowerShell Parameter 형식에 따라:

powershell id="cmdC224" .\\tcf-cicd\\scripts\\cicd-deploy.bat \` -Action full \` -Profile dev \` sv om

### Bash

bash id="cmdC225" ./tcf-cicd/scripts/cicd-deploy.sh --help

## C.16.2 Dry Run

bash id="cmdC226" ./tcf-cicd/scripts/cicd-deploy.sh \\ --dry-run \\ --profile dev \\ sv om

실제 파일복사·배포 전에 선택 Module과 Task를 확인한다.

## C.16.3 Build만

bash id="cmdC227" ./tcf-cicd/scripts/cicd-deploy.sh \\ --action build \\ --profile dev \\ sv

운영 Artifact Directory:

bash id="cmdC228" ./tcf-cicd/scripts/cicd-deploy.sh \\ --action build \\ --profile prod \\ --artifact-dir ./artifacts \\ sv om

현재 Script는 prod Profile에서 운영서버 webapps로 직접 복사하기보다 Artifact를 생성·전달하는 방식을 사용한다.

## C.16.4 Sync만

bash id="cmdC229" ./tcf-cicd/scripts/cicd-deploy.sh \\ --action sync \\ --profile dev

tcf-cicd 환경설정을 Framework Module로 동기화할 때 사용한다.

## C.16.5 Dev 전체 흐름

bash id="cmdC230" ./tcf-cicd/scripts/cicd-deploy.sh \\ --profile dev \\ sv om \\ --restart \\ --health-check

실행 단계:

\`\`\`text id=“cmdC231” Sync

→ Build

→ Deploy

→ 필요 시 Restart

→ Health Check


\---

\## C.16.6 단계 생략

\`\`\`bash id="cmdC232"
./tcf-cicd/scripts/cicd-deploy.sh \\
\--profile dev \\
\--skip-sync \\
\--skip-build \\
sv

단계 생략은 해당 결과가 이미 승인·검증됐을 때만 사용한다.

## C.16.7 운영 설정 적용

현재 Script 기준:

bash id="cmdC233" export CATALINA\_BASE=/opt/tomcat-instance ./tcf-cicd/scripts/cicd-deploy.sh \\ --action config \\ --profile prod \\ --apply-config

운영 설정 적용은 개발자가 임의로 수행하지 않고 변경승인과 운영권한을 따른다.

# C.17 로그 조회·검색

## C.17.1 Linux 실시간 로그

bash id="cmdC234" tail -F logs/nsight-app.log

최근 200줄:

bash id="cmdC235" tail -n 200 logs/nsight-app.log

여러 파일:

bash id="cmdC236" tail -F \\ sv-service/logs/nsight-app.log \\ sv-service/logs/tcf-console.log

## C.17.2 PowerShell 실시간 로그

powershell id="cmdC237" Get-Content \` .\\sv-service\\logs\\nsight-app.log \` -Tail 200 \` -Wait

## C.17.3 CMD 최근 로그

bat id="cmdC238" powershell -Command ^ "Get-Content '.\\sv-service\\logs\\nsight-app.log' -Tail 200 -Wait"

## C.17.4 GUID 검색

### Bash

bash id="cmdC239" grep -RIn \\ "G-LOCAL-CT-0001" \\ logs sv-service/logs

### PowerShell

powershell id="cmdC240" Get-ChildItem \` .\\logs,.\\sv-service\\logs \` -Recurse \` -File | Select-String "G-LOCAL-CT-0001"

## C.17.5 ServiceId 검색

bash id="cmdC241" grep -RIn \\ "CT.Reservation.create" \\ logs

PowerShell:

powershell id="cmdC242" Get-ChildItem .\\logs -Recurse -File | Select-String "CT\\.Reservation\\.create"

## C.17.6 오류 검색

bash id="cmdC243" grep -EIn \\ "ERROR|Exception|Caused by|TIMEOUT" \\ logs/nsight-app.log

PowerShell:

powershell id="cmdC244" Select-String \` -Path .\\logs\\nsight-app.log \` -Pattern "ERROR|Exception|Caused by|TIMEOUT"

첫 Caused by 하나만 보지 말고 가장 아래쪽 Root Cause와 앞선 업무 문맥을 함께 확인한다.

## C.17.7 압축 로그

bash id="cmdC245" zgrep -n \\ "G-LOCAL-CT-0001" \\ logs/\*.gz

Windows PowerShell은 압축해제 후 검색하거나 승인된 압축검색 도구를 사용한다.

## C.17.8 특정 시간 범위

구조화 Timestamp가 있을 때:

bash id="cmdC246" awk '$0 >= "2026-07-18 10:00:00" && $0 <= "2026-07-18 10:10:00"' \\ logs/nsight-app.log

로그 형식에 따라 Script를 조정한다.

# C.18 Port·Process 확인

## C.18.1 Windows Port 확인

CMD:

bat id="cmdC247" netstat -ano | findstr :8086

PowerShell:

powershell id="cmdC248" Get-NetTCPConnection \` -LocalPort 8086 \` -ErrorAction SilentlyContinue

Listening만:

powershell id="cmdC249" Get-NetTCPConnection \` -LocalPort 8086 \` -State Listen

## C.18.2 PID Process 확인

CMD:

bat id="cmdC250" tasklist /FI "PID eq 12345"

PowerShell:

powershell id="cmdC251" Get-Process -Id 12345

Java Command Line:

powershell id="cmdC252" Get-CimInstance Win32\_Process \` -Filter "ProcessId=12345" | Select-Object ProcessId, CommandLine

## C.18.3 Linux Port 확인

bash id="cmdC253" ss -lntp | grep ':8086'

대안:

bash id="cmdC254" lsof -iTCP:8086 -sTCP:LISTEN

Process:

bash id="cmdC255" ps -fp <pid>

Command Line:

bash id="cmdC256" tr '\\0' ' ' < /proc/<pid>/cmdline

## C.18.4 Process 종료

### Windows PowerShell

정상 종료를 먼저 수행한다.

강제종료가 필요한 경우:

powershell id="cmdC257" Stop-Process -Id 12345

강제:

powershell id="cmdC258" Stop-Process -Id 12345 -Force

### Linux

정상 종료:

bash id="cmdC259" kill <pid>

일정 시간 후에도 종료되지 않을 때만:

bash id="cmdC260" kill -9 <pid>

kill -9는 Shutdown Hook·Transaction 정리·로그 Flush를 수행하지 못하게 할 수 있다.

# C.19 JVM 진단

## C.19.1 Java Process 목록

bash id="cmdC261" jps -lv

jps에 보이지 않으면 실행 사용자와 권한을 확인한다.

## C.19.2 JVM 기본정보

bash id="cmdC262" jcmd <pid> VM.version jcmd <pid> VM.command\_line jcmd <pid> VM.flags jcmd <pid> VM.system\_properties

## C.19.3 Heap 정보

bash id="cmdC263" jcmd <pid> GC.heap\_info

Class Histogram:

bash id="cmdC264" jcmd <pid> GC.class\_histogram

Live Histogram:

bash id="cmdC265" jmap -histo:live <pid>

live Histogram은 Full GC를 유발할 수 있으므로 운영에서는 승인 후 사용한다.

## C.19.4 Thread Dump

bash id="cmdC266" jcmd <pid> Thread.print -l

파일 저장:

bash id="cmdC267" jcmd <pid> Thread.print -l \\ > thread-$(date +%Y%m%d-%H%M%S).txt

Windows PowerShell:

powershell id="cmdC268" jcmd 12345 Thread.print -l | Out-File \` .\\thread-20260718-103000.txt \` -Encoding utf8

대안:

bash id="cmdC269" jstack -l <pid> > thread.txt

Thread Dump는 5~10초 간격으로 3회 정도 수집해야 지속 대기인지 순간 상태인지 비교하기 쉽다.

## C.19.5 GC 상태

bash id="cmdC270" jstat -gcutil <pid> 1000 10

의미:

\`\`\`text id=“cmdC271” 1초 간격

10회 출력



확인:

\`\`\`text id="cmdC272"
Old 사용률

Young·Full GC 횟수

GC 시간 증가

회수 후 Heap 감소 여부

## C.19.6 Flight Recording

JDK 21에서 승인된 경우:

bash id="cmdC273" jcmd <pid> JFR.start \\ name=nsight-diagnostic \\ settings=profile \\ duration=60s \\ filename=nsight-diagnostic.jfr

상태:

bash id="cmdC274" jcmd <pid> JFR.check

중지:

bash id="cmdC275" jcmd <pid> JFR.stop \\ name=nsight-diagnostic

운영 JFR은 부하·파일크기·민감정보 정책을 확인한다.

## C.19.7 Heap Dump

bash id="cmdC276" jcmd <pid> GC.heap\_dump \\ /secure/path/heap-20260718.hprof

주의:

\`\`\`text id=“cmdC277” 파일이 매우 클 수 있음

I/O 부하 발생

개인정보 포함 가능

보안 저장·전송 필요



Disk 여유공간과 승인 없이 운영에서 실행하지 않는다.

\---

\# C.20 Thread·DB Pool·SQL 진단 순서

\`\`\`text id="cmdC278"
거래 GUID 확보

→ ServiceId 처리시간 확인

→ Tomcat Busy Thread

→ JVM CPU·GC

→ Hikari Active·Pending

→ SQL 실행시간

→ DB Lock·Wait

→ 외부 연계시간

→ 최근 배포·설정

명령 하나만 보고 원인을 단정하지 않는다.

# C.21 DB 연결·검증

## C.21.1 SQL\*Plus 접속 예

bash id="cmdC279" sqlplus <user>@//<host>:<port>/<service\_name>

Password를 명령행에 직접 작성하지 않는다.

금지:

bash id="cmdC280" sqlplus user/password@host:1521/service

Process 목록·Shell History에 Password가 노출될 수 있다.

## C.21.2 기본 확인

sql id="cmdC281" SELECT SYSDATE FROM DUAL;

현재 사용자:

sql id="cmdC282" SELECT USER FROM DUAL;

DB Version은 권한이 허용된 경우 확인한다.

sql id="cmdC283" SELECT \* FROM V$VERSION;

## C.21.3 현재 Schema 객체 확인

sql id="cmdC284" SELECT TABLE\_NAME FROM USER\_TABLES WHERE TABLE\_NAME LIKE 'CT\_%' ORDER BY TABLE\_NAME;

Column:

sql id="cmdC285" SELECT COLUMN\_NAME, DATA\_TYPE, DATA\_LENGTH, NULLABLE FROM USER\_TAB\_COLUMNS WHERE TABLE\_NAME = 'CT\_CONTACT\_RESERVATION' ORDER BY COLUMN\_ID;

## C.21.4 Constraint

sql id="cmdC286" SELECT CONSTRAINT\_NAME, CONSTRAINT\_TYPE, STATUS FROM USER\_CONSTRAINTS WHERE TABLE\_NAME = 'CT\_CONTACT\_RESERVATION';

## C.21.5 Index

sql id="cmdC287" SELECT INDEX\_NAME, UNIQUENESS, STATUS FROM USER\_INDEXES WHERE TABLE\_NAME = 'CT\_CONTACT\_RESERVATION';

Index Column:

sql id="cmdC288" SELECT INDEX\_NAME, COLUMN\_NAME, COLUMN\_POSITION FROM USER\_IND\_COLUMNS WHERE TABLE\_NAME = 'CT\_CONTACT\_RESERVATION' ORDER BY INDEX\_NAME, COLUMN\_POSITION;

## C.21.6 데이터 건수

sql id="cmdC289" SELECT STATUS\_CD, COUNT(\*) AS CNT FROM CT\_CONTACT\_RESERVATION GROUP BY STATUS\_CD ORDER BY STATUS\_CD;

History 대사:

sql id="cmdC290" SELECT r.RESERVATION\_ID FROM CT\_CONTACT\_RESERVATION r WHERE NOT EXISTS ( SELECT 1 FROM CT\_CONTACT\_RESERVATION\_HISTORY h WHERE h.RESERVATION\_ID = r.RESERVATION\_ID );

업무 요구에 따라 생성된 모든 Master가 반드시 History를 가져야 하는지 확인한다.

## C.21.7 Transaction 수동 검증

sql id="cmdC291" SET AUTOCOMMIT OFF;

조회·변경 후:

sql id="cmdC292" ROLLBACK;

확정:

sql id="cmdC293" COMMIT;

공유 개발 DB에서 임의 DML을 실행하지 않는다.

테스트 전용 Schema와 승인된 데이터만 사용한다.

## C.21.8 실행계획

sql id="cmdC294" EXPLAIN PLAN FOR SELECT ... FROM CT\_CONTACT\_RESERVATION WHERE ...;

조회:

sql id="cmdC295" SELECT \* FROM TABLE( DBMS\_XPLAN.DISPLAY );

실제 실행 통계가 필요하면 DBA 승인 방식과 SQL Monitor·Cursor Plan을 사용한다.

EXPLAIN PLAN의 예상치만으로 운영성능을 확정하지 않는다.

# C.22 H2 로컬 확인

로컬 Profile이 H2를 사용하는 경우 설정을 먼저 찾는다.

bash id="cmdC296" rg -n \\ "jdbc:h2|h2.console|spring.datasource" \\ -g "\*.yml" \\ -g "\*.yaml"

PowerShell:

powershell id="cmdC297" Get-ChildItem -Recurse \` -Include \*.yml,\*.yaml | Select-String \` "jdbc:h2|h2.console|spring.datasource"

확인:

\`\`\`text id=“cmdC298” Memory DB인가?

File DB인가?

AUTO\_SERVER 설정이 있는가?

업무 DB와 OM DB가 분리되는가?

여러 Process가 같은 파일을 사용하는가?



Local H2의 결과가 Oracle 운영환경의 SQL·Lock·Type 동작을 완전히 보장하지 않는다.

\---

\# C.23 설정·환경변수 확인

\## C.23.1 활성 Profile

로그 검색:

\`\`\`bash id="cmdC299"
grep -i \\
"profile" \\
logs/nsight-app.log

PowerShell:

powershell id="cmdC300" Select-String \` -Path .\\logs\\nsight-app.log \` -Pattern "profile"

## C.23.2 환경변수 목록

### Windows CMD

bat id="cmdC301" set

특정 Prefix:

bat id="cmdC302" set NSIGHT\_ set SPRING\_ set CT\_

### PowerShell

powershell id="cmdC303" Get-ChildItem Env: | Sort-Object Name

특정 Prefix:

powershell id="cmdC304" Get-ChildItem Env: | Where-Object Name -Like "NSIGHT\_\*"

### Bash

bash id="cmdC305" env | sort env | grep '^NSIGHT\_'

환경변수 전체 출력에는 Secret이 포함될 수 있으므로 공유하지 않는다.

## C.23.3 Spring 설정 위치 검색

bash id="cmdC306" find . \\ -name "application\*.yml" \\ -o -name "application\*.yaml" \\ -o -name "application\*.properties"

PowerShell:

powershell id="cmdC307" Get-ChildItem -Recurse \` -Include application\*.yml, application\*.yaml, application\*.properties

## C.23.4 설정값 검색

bash id="cmdC308" rg -n \\ "maximum-pool-size|connection-timeout|default-statement-timeout|online-transaction" \\ -g "\*.yml" \\ -g "\*.yaml"

운영값은 Repository의 기본값이 아니라 외부 주입 후의 **Effective Configuration**을 확인해야 한다.

# C.24 인코딩·줄바꿈

## C.24.1 Windows Console UTF-8

CMD:

bat id="cmdC309" chcp 65001

PowerShell:

\`\`\`powershell id=“cmdC310” [Console](:OutputEncoding%20=)::InputEncoding = \[System.Text.UTF8Encoding\]::new()

\[System.Text.UTF8Encoding\]::new()



Console Encoding 변경이 소스파일 Encoding 자체를 변경하지는 않는다.

\---

\## C.24.2 Linux 파일 Encoding

\`\`\`bash id="cmdC311"
file -bi path/to/file

변환:

bash id="cmdC312" iconv \\ -f MS949 \\ -t UTF-8 \\ input.txt \\ > output.txt

원본 Backup·Diff 검토 없이 전체 파일을 일괄 변환하지 않는다.

## C.24.3 Git 줄바꿈 확인

bash id="cmdC313" git diff --check git status

Whitespace 오류:

bash id="cmdC314" git diff --check

.gitattributes 확인:

bash id="cmdC315" cat .gitattributes

PowerShell:

powershell id="cmdC316" Get-Content .\\.gitattributes

# C.25 대표 오류별 진단 명령

## C.25.1 java를 찾을 수 없음

확인:

\`\`\`text id=“cmdC317” JAVA\_HOME

PATH

JDK 설치경로

Shell 재시작 여부



명령:

\`\`\`bash id="cmdC318"
java -version

Windows:

bat id="cmdC319" where java echo %JAVA\_HOME%

## C.25.2 gradle not found

\`\`\`text id=“cmdC320” GRADLE\_HOME\_OVERRIDE

GRADLE\_HOME

PATH

Wrapper 완전성



명령:

\`\`\`bash id="cmdC321"
gradle --version

Windows:

bat id="cmdC322" where gradle

## C.25.3 Unsupported class file major version

원인:

text id="cmdC323" Compile JDK와 실행 JDK 불일치

확인:

bash id="cmdC324" java -version javac -version gradle --version

Daemon 종료 후 Clean Build:

bash id="cmdC325" gradle --stop gradle clean build

## C.25.4 Port 사용 중

Windows:

bat id="cmdC326" netstat -ano | findstr :8086 tasklist /FI "PID eq <pid>"

Linux:

bash id="cmdC327" ss -lntp | grep ':8086' ps -fp <pid>

기존 정상 Process를 무조건 종료하지 말고 어느 Application인지 확인한다.

## C.25.5 Spring Context 기동 실패

검색:

bash id="cmdC328" grep -EIn \\ "APPLICATION FAILED|Caused by|BeanCreationException" \\ logs/nsight-app.log

확인순서:

\`\`\`text id=“cmdC329” 첫 실패 Bean

→ 가장 아래 Root Cause

→ Profile

→ DataSource

→ Mapper

→ Port

→ Missing Class

→ Secret


\---

\## C.25.6 Mapper \`Invalid bound statement\`

검색:

\`\`\`bash id="cmdC330"
rg -n \\
"MapperName|sqlMethodName" \\
module/src

WAR 확인:

bash id="cmdC331" jar tf module/build/libs/module.war | grep "Mapper.xml"

검사:

\`\`\`text id=“cmdC332” Mapper Interface Method

XML id

Namespace

Resource 경로

Component Scan


\---

\## C.25.7 DB Connection 실패

설정 검색:

\`\`\`bash id="cmdC333"
rg -n \\
"jdbc:|username|maximum-pool-size|connection-timeout" \\
\-g "\*.yml"

Network:

### Windows

powershell id="cmdC334" Test-NetConnection \` -ComputerName <db-host> \` -Port 1521

### Linux

bash id="cmdC335" nc -vz <db-host> 1521

DB Port 연결 성공은 계정·Service Name·Schema 정상까지 보장하지 않는다.

## C.25.8 HTTP 404

확인:

\`\`\`text id=“cmdC336” Port

Context Path

Endpoint

WAR 배포명

Gateway Route



예:

\`\`\`text id="cmdC337"
bootRun
http://localhost:8086/sv/online

Tomcat
http://localhost:8080/sv/online

## C.25.9 HTTP 405

text id="cmdC338" GET·POST Method 불일치

표준 온라인 거래는 일반적으로 POST JSON이다.

bash id="cmdC339" curl -X POST ...

## C.25.10 HTTP 401

확인:

\`\`\`text id=“cmdC340” Authorization Header

Token 만료

Signature

Issuer

Audience

JWKS

System Time



Token 원문을 로그로 복사하지 않는다.

\---

\## C.25.11 HTTP 403

확인:

\`\`\`text id="cmdC341"
기능권한

Role Claim

OM 권한등록

데이터권한

지점·소유자

권한 검증을 임시 비활성화하지 않는다.

## C.25.12 ServiceId 미등록

검색:

bash id="cmdC342" git grep -n \\ "CT.Reservation.create"

확인:

\`\`\`text id=“cmdC343” Handler serviceIds

Dispatcher 등록

Spring Bean

OM Service Catalog

거래통제 Allow List

Cache Evict


\---

\## C.25.13 Timeout

검색:

\`\`\`bash id="cmdC344"
grep -EIn \\
"TIMEOUT|QueryTimeout|connection timeout" \\
logs/nsight-app.log

함께 확인:

\`\`\`text id=“cmdC345” TCF elapsed

SQL elapsed

외부 호출 elapsed

Hikari Pending

Tomcat Busy Thread

JVM GC



Timeout 값을 먼저 늘리지 않는다.

\---

\## C.25.14 Test가 실행되지 않음

\`\`\`bash id="cmdC346"
gradle :sv-service:test --info

확인:

\`\`\`text id=“cmdC347” Test Class 명명

JUnit Platform

Source Set

Exclude

Up-to-date

Test Report



강제:

\`\`\`bash id="cmdC348"
gradle :sv-service:test --rerun-tasks

# C.26 정상 개발 흐름

\`\`\`text id=“cmdC349” 1. Git Branch 확인

1.  JDK 21 확인
2.  Gradle JVM 확인
3.  Module 목록 확인
4.  변경 전 Clean 상태 확인
5.  단일 Module Build
6.  Unit·Mapper Test
7.  업무 bootRun
8.  표준 ServiceId 호출
9.  GUID로 로그 추적
10.  DB 결과 확인
11.  bootWar 생성
12.  WAR 내부 Resource 확인
13.  Checksum 기록
14.  전체 회귀 Build
15.  MR·CI 결과 제출



대표 명령:

\`\`\`bash id="cmdC350"
git status
java -version
gradle --version
gradle projects
gradle :sv-service:build
gradle :sv-service:test
gradle :sv-service:bootRun
./tcf-scripts/curl-sample.sh sv
gradle :sv-service:bootWar
jar tf sv-service/build/libs/sv.war
sha256sum sv-service/build/libs/sv.war
git diff develop...HEAD

# C.27 오류 진단 흐름

\`\`\`text id=“cmdC351” 실패 명령 그대로 보존

→ Exit Code 확인

→ 최초 실패 Task 확인

→ Root Cause 확인

→ 환경·Compile·Dependency·Test·Runtime·DB 분류

→ Module 범위로 축소

→ 동일 명령 재현

→ 한 가지 원인만 변경

→ 축소 Test

→ 전체 회귀

→ 결과와 근거 기록


\---

\# C.28 정상 예시

\`\`\`text id="cmdC352"
Branch
feature/REQ-CT-RSV-003-create

JDK
21

Gradle
승인된 8.x

명령
gradle :ct-service:test

결과
Test PASS

명령
gradle :ct-service:bootRun

호출
CT.Reservation.create

응답
SUCCESS

DB
Master 1건
History 1건

명령
gradle :ct-service:bootWar

Artifact
ct.war

Checksum
SHA-256 기록

추적
REQ-CT-RSV-003
→ ServiceId
→ SQL
→ Test
→ Artifact

ct-service가 실제 프로젝트에 편입된 이후의 목표 예다.

# C.29 금지 예시

\`\`\`text id=“cmdC353” 프로젝트 Root가 아닌 임의 폴더에서 명령을 실행한다.

JDK Version을 확인하지 않고 Build한다.

Gradle Version이 달라도 그대로 진행한다.

Wrapper가 불완전한데 Wrapper가 있다고 보고한다.

Build 실패를 무조건 Cache 문제로 판단한다.

Test 실패를 -x test로 숨긴다.

환경마다 WAR를 다시 Build한다.

개발자 PC의 WAR를 운영에 배포한다.

Hard-coded 개인 Gradle 경로를 팀 표준으로 사용한다.

Port를 점유한 Process를 확인 없이 종료한다.

모든 Java Process를 taskkill·kill -9 한다.

Health UP만 확인하고 업무 Smoke를 생략한다.

curl -v 결과의 Authorization을 공유한다.

환경변수 전체를 출력해 Secret을 노출한다.

운영 DB Password를 명령행 인수에 작성한다.

운영에서 Remote Debug Port를 개방한다.

승인 없이 Heap Dump를 생성한다.

상담메모·고객번호를 검색 결과와 함께 공유한다.

Timeout 발생 시 근거 없이 Timeout 값부터 늘린다.

오류가 발생하면 모든 Tomcat을 먼저 재기동한다.

DB 변경 후 Commit·Rollback 상태를 확인하지 않는다.

Git reset –hard·clean -fd를 확인 없이 실행한다.

공유 Branch에 무조건 force push한다.

Artifact Checksum 없이 배포한다.

로그 한 줄만 보고 Root Cause를 단정한다.


\---

\# C.30 책임 경계와 RACI

| 활동 | 개발자 | 개발리더 | AA | DevOps | DBA | 운영 | 보안 | QA |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| JDK 설치 | R | C | A/C | C | I | I | I | I |
| Gradle 표준 | C | C | A | R | I | I | I | I |
| IDE 설정 | R | A/C | C | I | I | I | I | I |
| Git Branch | R | A | C | I | I | I | I | C |
| Local Build | R | A/C | C | C | I | I | I | C |
| Test | R | C | C | C | C | I | C | A/R |
| DB 접속 | C | I | I | I | A/R | C | C | C |
| Artifact | C | C | C | A/R | I | C | C | C |
| Dev 배포 | C | I | C | A/R | C | C | I | C |
| 운영 진단 | C | I | C | C | C | A/R | C | C |
| JVM Dump | C | I | C | C | I | A/R | A/C | I |
| Secret | R/C | C | C | A/R | C | A/R | A | I |
| 명령 표준화 | R/C | C | A | A/R | C | C | C | C |

\---

\# C.31 성능·용량·확장성 주의

\## Build

\`\`\`text id="cmdC354"
전체 Parallel Build

Test JVM 수

Gradle Daemon Heap

Dependency Download

Disk I/O

개발 PC가 부족하면 전체 Build를 무조건 병렬화하지 않는다.

## 전체 bootRun

\`\`\`text id=“cmdC355” 9개 이상 JVM

업무별 DB Pool

로그 파일

H2 File Lock

Port

Heap



전체 실행보다 필요한 Module만 실행하는 것을 우선한다.

\## 진단 명령

다음은 운영부하를 만들 수 있다.

\`\`\`text id="cmdC356"
Heap Dump

Live Histogram

장시간 JFR

대규모 Log grep

전체 Table COUNT

실행계획 수집

압축 로그 전체 해제

# C.32 보안·개인정보·감사

\`\`\`text id=“cmdC357” 명령행에 Password를 넣지 않는다.

Token 원문을 Shell History에 남기지 않는다.

환경변수 전체 출력물을 공유하지 않는다.

Heap Dump를 일반 파일공유에 올리지 않는다.

Thread Dump에도 업무값이 포함될 수 있음을 인지한다.

운영 명령은 승인계정과 Bastion을 사용한다.

강제종료·배포·DB DML은 감사기록을 남긴다.

운영 로그를 로컬 PC로 무단 반출하지 않는다.

curl·Build·Debug 로그를 공유할 때 민감정보를 제거한다.



Shell History:

\### Bash

\`\`\`bash id="cmdC358"
history | tail -50

PowerShell:

powershell id="cmdC359" Get-History

민감명령을 실행했다면 단순 History 삭제만으로 노출이 모두 제거됐다고 가정하지 않는다.

# C.33 자동검증 및 품질 Gate

## 환경 Gate

\`\`\`text id=“cmdC360” JDK 21

승인 Gradle

UTF-8

현재 Branch

Clean Git 상태 또는 승인된 변경

Profile 확인


\## Build Gate

\`\`\`text id="cmdC361"
Compile PASS

Test PASS

Mapper Resource 포함

bootWar PASS

Artifact 생성

Checksum

## 실행 Gate

\`\`\`text id=“cmdC362” Port Listen

Context 기동

Health

대표 ServiceId

로그 GUID

DB 결과


\## 배포 Gate

\`\`\`text id="cmdC363"
CI 생성 Artifact

Tag·Commit

Checksum

DB Migration

OM 등록

Smoke

Rollback Artifact

# C.34 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| CMD-001 | Java·Javac 21 | PASS |
| CMD-002 | Gradle JVM 17 | Gate 실패 |
| CMD-003 | Gradle 미설치 | 명확한 오류 |
| CMD-004 | Wrapper 일부 누락 | Wrapper 실패 탐지 |
| CMD-005 | 프로젝트 Root가 아님 | Module 미발견 |
| CMD-006 | gradle projects | Module 목록 |
| CMD-007 | 단일 Module Build | PASS |
| CMD-008 | 공통 Module 변경 | 업무 회귀 |
| CMD-009 | 특정 Test | 대상만 실행 |
| CMD-010 | Test 실패 | Artifact 차단 |
| CMD-011 | \-x test Release | Gate 차단 |
| CMD-012 | bootRun Port 정상 | Listen |
| CMD-013 | Port 충돌 | PID 식별 |
| CMD-014 | Profile local | 로그 확인 |
| CMD-015 | 잘못된 Context | 404 |
| CMD-016 | GET 호출 | 405 |
| CMD-017 | 표준 POST | SUCCESS |
| CMD-018 | ServiceId 오타 | Dispatcher 오류 |
| CMD-019 | JWT 없음 | 401 |
| CMD-020 | 권한 없음 | 403 |
| CMD-021 | GUID 로그검색 | 전체 경로 |
| CMD-022 | Mapper XML 미포함 | WAR 검사 실패 |
| CMD-023 | WAR Checksum | 기록 일치 |
| CMD-024 | Tomcat Foreground | 기동 오류 확인 |
| CMD-025 | Health UP | Component 정상 |
| CMD-026 | Smoke 실패 | 배포 실패 |
| CMD-027 | DB Network 실패 | 접속 오류 구분 |
| CMD-028 | DB Schema 누락 | SQL 오류 |
| CMD-029 | Thread Dump 3회 | 비교 가능 |
| CMD-030 | Heap Dump 공간 부족 | 실행 차단 |
| CMD-031 | 전체 bootRun 자원부족 | 대상 축소 |
| CMD-032 | curl Token 로그 | 보안 실패 |
| CMD-033 | Secret 환경출력 | 공유 금지 |
| CMD-034 | CI/CD Dry Run | 대상 확인 |
| CMD-035 | Prod Artifact Build | Repository용 생성 |
| CMD-036 | 잘못된 WAR 직접복사 | 배포 Gate 실패 |
| CMD-037 | Git 줄바꿈 대량변경 | Commit 중단 |
| CMD-038 | 강제종료 | 승인 필요 |
| CMD-039 | Timeout 증가 | 원인분석 우선 |
| CMD-040 | 다른 개발자 재현 | 동일 결과 |

# C.35 완료 체크리스트

## 개발환경

| 점검 | 완료 |
| --- | --- |
| Java와 Javac이 21이다. | □ |
| Gradle JVM이 21이다. | □ |
| Eclipse JRE가 21이다. | □ |
| Tomcat JAVA\_HOME이 21이다. | □ |
| Gradle Version이 승인값이다. | □ |
| Encoding이 UTF-8이다. | □ |
| Wrapper 완전성을 확인했다. | □ |

## Git

| 점검 | 완료 |
| --- | --- |
| 올바른 Remote다. | □ |
| 올바른 Branch다. | □ |
| 변경 전 상태를 확인했다. | □ |
| Diff에 Secret·로그가 없다. | □ |
| Commit 메시지가 요구사항과 연결된다. | □ |
| 강제 Push를 사용하지 않았다. | □ |

## Build·Test

| 점검 | 완료 |
| --- | --- |
| Module 목록을 확인했다. | □ |
| 단일 Module Build가 성공한다. | □ |
| 필수 Test가 성공한다. | □ |
| 전체 회귀 Build가 성공한다. | □ |
| 의존성 충돌을 확인했다. | □ |
| Test Report를 보존했다. | □ |
| \-x test 결과를 완료로 사용하지 않는다. | □ |

## 실행·호출

| 점검 | 완료 |
| --- | --- |
| 활성 Profile을 확인했다. | □ |
| Port·Context를 확인했다. | □ |
| Health가 성공한다. | □ |
| 대표 ServiceId가 성공한다. | □ |
| GUID로 로그를 찾을 수 있다. | □ |
| DB 결과가 기대와 일치한다. | □ |
| 종료 시 자원이 반환된다. | □ |

## Artifact·Tomcat

| 점검 | 완료 |
| --- | --- |
| bootWar가 성공한다. | □ |
| WAR 내부 Class·Mapper가 있다. | □ |
| Secret이 WAR에 없다. | □ |
| Checksum이 있다. | □ |
| Tomcat Version·JVM을 확인했다. | □ |
| Context 중복이 없다. | □ |
| 배포 후 Smoke가 성공한다. | □ |

## 진단·보안

| 점검 | 완료 |
| --- | --- |
| Port와 PID를 연결할 수 있다. | □ |
| Thread Dump를 수집할 수 있다. | □ |
| GC 상태를 확인할 수 있다. | □ |
| 위험 진단명령의 부하를 이해한다. | □ |
| Password를 명령행에 쓰지 않는다. | □ |
| Token·개인정보를 공유하지 않는다. | □ |
| 운영 강제명령에 승인이 있다. | □ |

# C.36 변경·호환성·폐기 관리

## JDK 변경

\`\`\`text id=“cmdC364” CI JVM

Gradle Toolchain

Eclipse JRE

Tomcat JAVA\_HOME

Library 호환성

GC Option

전체 Test



를 함께 변경한다.

\## Gradle 변경

\`\`\`text id="cmdC365"
Wrapper

CI Runner

개발 Script

Plugin

Build Cache

Artifact 재현성

을 확인한다.

## Port 변경

\`\`\`text id=“cmdC366” application-local.yml

Gateway Downstream

UI Relay

curl Script

Health Check

문서



를 함께 수정한다.

\## Module 추가

\`\`\`text id="cmdC367"
settings.gradle

build.gradle

run-local Script

deploy Script

tcf-cicd Manifest

Port

Context

WAR

Health

를 추가한다.

상담예약 ct-service는 이 항목 전체가 완료돼야 공식 명령대상으로 인정한다.

## Script 폐기

다음 절차를 따른다.

\`\`\`text id=“cmdC368” 신규 Script 제공

→ 호출문서 전환

→ CI·운영 사용여부 확인

→ Deprecated 표시

→ 호출 0 확인

→ 삭제



개인 PC 경로를 포함한 Script는 표준 환경변수 기반으로 전환한다.

\---

\# 시사점

\## 핵심 아키텍처 판단

첫째, 개발환경 명령은 개인 PC 편의를 위한 도구가 아니라 동일 결과를 반복 생성하기 위한 표준 실행계약이다.

둘째, 현재 프로젝트는 JDK 21을 기준으로 하며 명령행·Gradle·IDE·Tomcat의 Java Version을 모두 일치시켜야 한다.

셋째, Gradle Wrapper가 완전하지 않은 경우 시스템 Gradle을 사용할 수 있지만 개발자·CI 간 Version Drift를 별도로 차단해야 한다.

넷째, \`bootRun\`은 빠른 로컬 개발에 적합하고 \`bootWar\`와 Tomcat 배포는 실제 WAR 구조·ClassLoader·Context를 검증하기 위한 별도 단계다.

다섯째, Health Check는 Application 생존을 확인하지만 ServiceId·권한·Mapper·DB의 실제 성공은 Smoke Test로 확인해야 한다.

여섯째, Port·Process·Thread·Heap·DB 명령은 증상을 확인하는 도구이며 단일 결과만으로 Root Cause를 결정해서는 안 된다.

일곱째, 배포·진단 명령은 운영 시스템의 데이터와 가용성에 영향을 줄 수 있으므로 권한·승인·감사절차를 적용해야 한다.

여덟째, 명령어 빠른 참조는 현재 Branch의 Module·Profile·Script와 함께 유지돼야 하며 소스와 다른 오래된 명령은 오히려 장애를 만든다.

\---

\## 주요 위험

| 위험 | 결과 |
|---|---|
| JDK 불일치 | Compile·Runtime 오류 |
| Gradle Version Drift | 재현성 상실 |
| Wrapper 불완전 | 신규 개발자 Build 실패 |
| 개인 경로 Hard Coding | 다른 PC 실행 실패 |
| Root 외 Build | Module 미식별 |
| Test 제외 | 결함 Artifact |
| Port 무단 종료 | 다른 업무 중단 |
| 전체 JVM 강제종료 | 데이터·로그 유실 |
| Health만 확인 | 업무 장애 누락 |
| Token 명령행 노출 | 보안사고 |
| Heap Dump 무단생성 | 개인정보·Disk 장애 |
| 운영 DB DML | 데이터 손상 |
| Script Module 목록 불일치 | 배포 누락 |
| Artifact Checksum 없음 | 배포 무결성 상실 |
| 진단 후 근거 미기록 | 재발 분석 실패 |

\---

\## 우선 보완 과제

1\. Gradle Wrapper 네 개 파일을 정상 상태로 관리할지 시스템 Gradle 정책으로 통일할지 결정한다.
2\. JDK·Gradle Version을 CI와 개발자 환경에서 자동검증한다.
3\. Module·Port·Context 목록을 \`settings.gradle\`과 실행 Script에서 단일 Source로 생성한다.
4\. 개인 Gradle 경로를 포함한 Module Script를 환경변수 기반으로 정리한다.
5\. \`ct-service\` 추가 시 Build·Run·Deploy·Health Script를 동시에 갱신한다.
6\. 대표 ServiceId Smoke Script를 업무별로 제공한다.
7\. WAR 내부 Mapper·설정·Secret 검사를 CI Gate에 추가한다.
8\. Port·PID·Thread·Heap 진단 Runbook을 운영승인 절차와 연결한다.
9\. curl·Build Log의 Token·개인정보 마스킹 검사를 자동화한다.
10\. 이 부록의 명령을 Branch별 자동 테스트와 함께 관리한다.

\---

\# 마무리말

개발환경 명령을 제대로 사용할 수 있다는 것은 명령을 많이 외우는 것이 아니다.

다음 질문에 답할 수 있어야 한다.

\`\`\`text id="cmdC369"
현재 어떤 JDK와 Gradle이 사용되는가?

어떤 Module이 Build에 참여하는가?

어느 Task가 실패했는가?

Test가 실제로 실행됐는가?

어떤 Profile과 Port로 기동됐는가?

어느 ServiceId를 호출했는가?

해당 거래를 GUID로 찾을 수 있는가?

DB에는 어떤 결과가 남았는가?

운영 Tomcat에 배포할 WAR는 무엇인가?

WAR 안에 필요한 Mapper와 설정이 포함됐는가?

Artifact가 어느 Commit에서 만들어졌는가?

오류가 환경·코드·설정·DB 중 어느 범위인가?

이 명령을 다른 개발자도 같은 결과로 재현할 수 있는가?

부록 C의 핵심 흐름은 다음과 같다.

\`\`\`text id=“cmdC370” 환경 확인

→ Git 소스 확보

→ Gradle Build·Test

→ bootRun

→ 표준 거래 호출

→ 로그·DB 검증

→ bootWar·Checksum

→ Tomcat·CI/CD 배포

→ Health·Smoke

→ 장애 진단·복구



가장 중요한 원칙은 다음과 같다.

\`\`\`text id="cmdC371"
명령 실행의 성공은
화면에 SUCCESS가 출력된 사실이 아니다.

사용한 환경과 소스가 명확하고,
처리결과가 로그·데이터·Artifact로 증명되며,

다른 개발자가 같은 절차로
같은 결과를 재현할 수 있어야 한다.

그때 비로소 해당 명령은
개인 작업방법이 아니라
프로젝트 개발표준이 된다.
