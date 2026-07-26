<!-- source: ztcf-집필본/NSIGHT TCF Chapter 5- PC 개발환경 준비.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제2부. 내 PC에 개발환경을 준비하다

## 도입 전 안내말

제1부에서는 NSIGHT TCF의 전체 구조, 거래 처리 흐름, 프로그램 계층과 거래 식별 체계를 살펴보았다.

제2부에서는 실제 소스를 개발자 PC로 가져와 빌드하고 실행하기 위한 환경을 준비한다.

개발환경 구성은 프로그램을 설치하는 작업처럼 보이지만, 실제 목적은 다음과 같다.

모든 개발자가
같은 소스를 받고,
같은 명령을 실행하고,
같은 결과를 얻으며,
문제가 발생했을 때
설정 차이를 근거로 설명할 수 있게 한다.

개발자 A의 PC에서는 실행되지만 개발자 B의 PC에서는 컴파일되지 않는다면 그 프로젝트는 표준화된 개발환경을 갖추지 못한 것이다.

“내 PC에서는 됩니다.”
\= 개발 완료 증거가 아니다.

“깨끗한 표준 환경에서
같은 명령으로 재현됩니다.”
\= 개발 완료 증거다.

NSIGHT TCF 자료는 개발자 입문서, 표준 개발·운영 가이드, 아키텍처 문서, 환경설정 실값과 구축방법론을 계층적으로 제공하고 있다. 따라서 개발환경도 개인의 IDE 설정에 의존하지 않고 프로젝트 표준과 자동검증을 기준으로 구성해야 한다.

## 제2부의 목표

| 학습 영역 | 제2부를 마친 뒤 할 수 있어야 하는 것 |
| --- | --- |
| 버전 관리 | JDK·Gradle·IDE 버전을 확인하고 기록한다. |
| 환경 일치 | 명령행·IDE·Gradle·Tomcat의 JDK를 일치시킨다. |
| 소스 관리 | Git 저장소를 정상적으로 Clone하고 브랜치를 확인한다. |
| 빌드 | Gradle 멀티모듈을 깨끗한 환경에서 빌드한다. |
| 실행 | 지정 업무 WAR를 로컬에서 기동한다. |
| 인코딩 | UTF-8·MS949·줄바꿈 차이를 설명하고 해결한다. |
| 진단 | 환경 문제와 소스 문제를 구분한다. |
| 재현성 | 다른 개발자가 같은 절차로 결과를 재현하도록 증적을 남긴다. |

## 제2부 전체 학습 흐름

| 구분 | 제5장 | 제6장 | 제7장 | 제8장 |
| --- | --- | --- | --- | --- |
| 핵심 질문 | PC 환경이 표준과 같은가? | 소스를 어떻게 가져오는가? | 어떻게 빌드하는가? | 어떻게 실행·디버깅하는가? |
| 주요 대상 | JDK·IDE·인코딩 | Git·브랜치·커밋 | Gradle·의존성·산출물 | Profile·Port·Breakpoint |
| 확인 명령 | java -version | git status | gradle build | bootRun |
| 주요 실패 | JDK·인코딩 불일치 | 잘못된 브랜치·충돌 | 의존성·테스트 실패 | Port·DB·환경설정 오류 |
| 완료 증적 | 환경점검표 | Git 상태 | Build Report | 기동·거래 로그 |

### 그림으로 보는 제2부 학습 여정

\[개발환경 표준화\]
JDK·IDE·인코딩 확인
↓
\[소스 확보\]
Git Clone·브랜치 확인
↓
\[빌드 검증\]
Gradle Clean Build
↓
\[로컬 실행\]
업무 모듈 bootRun
↓
\[첫 거래 실행\]
StandardRequest 호출
↓
\[디버깅\]
Breakpoint·로그·DB 결과 확인

# 제5장. JDK·IDE·인코딩 설정

## 이 장을 시작하며

개발환경 문제는 업무 소스 문제처럼 보이는 경우가 많다.

예를 들어 다음 증상은 모두 코드 결함처럼 보일 수 있다.

지원하지 않는 Java 문법 오류
Gradle Plugin 적용 실패
Lombok 메서드 인식 실패
한글 주석 컴파일 오류
YAML 설정값 깨짐
Mapper XML 파싱 실패
배치 파일 실행 실패
Git에서 수백 개 파일이 변경됨
IDE에서는 성공하지만 명령행 빌드는 실패
로컬에서는 성공하지만 CI에서 실패

그러나 원인은 업무 로직이 아니라 다음 중 하나일 수 있다.

| 원인 영역 | 대표 원인 |
| --- | --- |
| JDK | 버전·Vendor·설치 경로 불일치 |
| PATH | 다른 Java 실행파일 우선 |
| Gradle | Gradle JVM과 명령행 JDK 불일치 |
| IDE | Project SDK·Language Level 불일치 |
| 인코딩 | UTF-8·MS949·BOM 혼재 |
| 줄바꿈 | CRLF·LF 자동변환 |
| Build Cache | 이전 JDK 산출물 잔존 |
| Annotation Processing | Lombok 처리 비활성 |
| Profile | local·dev·prod 설정 혼동 |
| Console | 출력 인코딩과 파일 인코딩 불일치 |

이 장에서는 개발환경을 다음 다섯 층으로 나누어 확인한다.

운영체제
↓
JDK·환경변수
↓
Gradle·Compiler
↓
IDE
↓
소스·리소스·콘솔·로그 인코딩

## 핵심 관점

IDE에서 보이는 설정만 믿지 않는다.

명령행 Java,
Gradle이 사용하는 Java,
IDE Project SDK,
IDE Gradle JVM,
Tomcat이 사용하는 Java를
각각 확인하고 서로 비교한다.

## 학습 목표

이 장을 마치면 다음을 수행할 수 있어야 한다.

| 번호 | 학습 목표 |
| --- | --- |
| 1 | JDK와 JRE의 차이를 설명한다. |
| 2 | 프로젝트 표준 JDK 버전을 확인한다. |
| 3 | JAVA\_HOME과 PATH의 역할을 구분한다. |
| 4 | 여러 JDK가 설치된 PC에서 실제 사용 중인 JDK를 찾는다. |
| 5 | 명령행 Java와 Gradle JVM을 비교한다. |
| 6 | IDE의 Project SDK와 Gradle JVM을 일치시킨다. |
| 7 | UTF-8과 MS949의 차이를 설명한다. |
| 8 | 소스 파일·콘솔·로그 인코딩을 구분한다. |
| 9 | CRLF와 LF로 인한 대량 Diff를 방지한다. |
| 10 | BOM으로 인한 스크립트·설정 오류를 진단한다. |
| 11 | 깨끗한 환경에서 동일한 빌드 결과를 재현한다. |
| 12 | 개발환경 점검 결과를 증적으로 기록한다. |

# 한눈에 보는 개발환경 구성

┌──────────────────────────────────────────────────────────┐
│ 개발자 PC │
│ │
│ OS 환경변수 │
│ ├─ JAVA\_HOME │
│ ├─ PATH │
│ ├─ GRADLE\_HOME │
│ └─ SPRING\_PROFILES\_ACTIVE │
│ │
│ JDK │
│ ├─ java │
│ ├─ javac │
│ └─ keytool │
│ │
│ Gradle │
│ ├─ Gradle Version │
│ ├─ Gradle JVM │
│ ├─ Java Toolchain │
│ └─ Compiler Encoding │
│ │
│ IDE │
│ ├─ Project SDK │
│ ├─ Language Level │
│ ├─ Gradle JVM │
│ ├─ File Encoding │
│ ├─ Line Separator │
│ └─ Annotation Processing │
│ │
│ Git │
│ ├─ .gitattributes │
│ ├─ .gitignore │
│ └─ core.autocrlf │
└──────────────────────────┬───────────────────────────────┘
▼
Clean Gradle Build
▼
Local bootRun
▼
Standard Transaction

# 개발환경의 기준정보 우선순위

개발환경 값이 여러 문서와 설정에 동시에 존재할 수 있다.

다음 순서로 기준을 판단한다.

| 우선순위 | 기준정보 | 예 |
| --- | --- | --- |
| 1 | 승인된 프로젝트 Baseline | 개발환경 표준서 |
| 2 | CI/CD 실행환경 | Jenkins Agent·Runner Image |
| 3 | 빌드 설정 | build.gradle, Toolchain |
| 4 | Gradle Wrapper | gradle-wrapper.properties |
| 5 | 운영 실행환경 | Tomcat JAVA\_HOME, setenv |
| 6 | 개발 스크립트 | run-local.bat, env-jdk21.bat |
| 7 | IDE 설정 | Project SDK·Gradle JVM |
| 8 | 개인 PC 기본값 | PATH의 Java·Gradle |

개인 PC 기본값이
프로젝트 표준을 결정하지 않는다.

# 현재 프로젝트에서 확인해야 할 기준

업로드 자료 기준으로 NSIGHT TCF 개발환경의 대표 기준은 다음과 같다.

| 항목 | 기준 또는 확인값 | 상태 |
| --- | --- | --- |
| Java | JDK 21 | 기준 확인 |
| Gradle | 8.x, 자료 예시 8.10.1 | 기준 확인 |
| Spring Boot | 3.x 계열 | 프로젝트 확인 |
| Tomcat | 10.1 계열 | 프로젝트 확인 |
| 소스 인코딩 | UTF-8 | 기준 확인 |
| 실행 기본 인코딩 | UTF-8 | 기준 확인 |
| Windows 개발 콘솔 | UTF-8 또는 명시된 MS949 | 환경별 확인 |
| 시간대 | Asia/Seoul | 실행환경 확인 |
| 기본 프로파일 | local | 로컬 실행 기준 |
| 개발 프로파일 | dev | 통합 Tomcat 기준 |
| 운영 프로파일 | prod | 운영 기준 |

프로젝트 자료에는 JDK 21과 Gradle 8.x를 로컬 개발·빌드 기준으로 사용하는 설명이 포함되어 있다. 다만 Wrapper 사용 여부와 실제 실행 스크립트가 혼재할 수 있으므로 저장소의 실행파일과 CI 정책을 최종 기준으로 대조해야 한다.

# 5.1 표준 버전과 설치 확인

## 5.1.1 JDK란 무엇인가

JDK는 Java 프로그램을 개발하고 실행하는 도구 모음이다.

| 구성요소 | 역할 |
| --- | --- |
| java | 컴파일된 Java 프로그램 실행 |
| javac | Java 소스 컴파일 |
| jar | JAR 파일 생성·조회 |
| javadoc | API 문서 생성 |
| keytool | 인증서·KeyStore 관리 |
| jcmd | JVM 진단 명령 |
| jstack | Thread Dump |
| jmap | Heap 정보 |
| jlink | Runtime Image 생성 |

개발자 PC에는 단순 실행환경보다 컴파일러와 진단도구를 포함한 JDK가 필요하다.

## 5.1.2 Java 버전은 왜 중요한가

Java 버전은 단순히 문법 사용 가능 범위만 결정하지 않는다.

Java 버전
├─ 소스 문법
├─ Bytecode 버전
├─ Spring Boot 호환성
├─ Gradle 호환성
├─ Tomcat 호환성
├─ 라이브러리 호환성
├─ GC 동작
├─ TLS·보안 알고리즘
└─ 운영 JVM 진단방식

### 버전 불일치 증상

| 증상 | 가능한 원인 |
| --- | --- |
| Unsupported class file major version | Gradle·Plugin이 Java 버전을 지원하지 않음 |
| UnsupportedClassVersionError | 더 높은 JDK로 빌드한 클래스를 낮은 JVM에서 실행 |
| invalid source release: 21 | 컴파일 JDK가 21보다 낮음 |
| NoSuchMethodError | 라이브러리·JDK 조합 불일치 |
| IDE 빨간 줄, CLI 성공 | IDE SDK 불일치 |
| CLI 실패, IDE 성공 | JAVA\_HOME 또는 PATH 불일치 |
| CI만 실패 | Runner JDK가 로컬과 다름 |

## 5.1.3 설치 확인 명령

### Windows 명령 프롬프트

java -version
javac -version
where java
where javac
echo %JAVA\_HOME%

### Windows PowerShell

java -version
javac -version
Get-Command java
Get-Command javac
$env:JAVA\_HOME

### Linux·macOS

java -version
javac -version
which java
which javac
echo "$JAVA\_HOME"
readlink -f "$(which java)"

### 기대 결과 예시

java version
\= 21.x

javac version
\= 21.x

JAVA\_HOME
\= JDK 21 설치 루트

java 실행 위치
\= JAVA\_HOME/bin/java

## 5.1.4 java -version만 확인하면 부족한 이유

java와 javac가 서로 다른 경로를 가리킬 수 있다.

PATH 첫 번째 Java
\= JDK 17 java.exe

JAVA\_HOME
\= JDK 21

IDE Project SDK
\= JDK 21

Gradle JVM
\= JDK 17

이 상태에서는 IDE 컴파일, 명령행 빌드와 Gradle 실행 결과가 다를 수 있다.

### 반드시 비교할 값

| 비교 대상 | 확인 방법 |
| --- | --- |
| 명령행 Java | java -version |
| 명령행 Compiler | javac -version |
| Java 실제 경로 | where java, which java |
| JAVA\_HOME | 환경변수 출력 |
| Gradle JVM | gradle --version |
| IDE Project SDK | IDE Project Settings |
| IDE Gradle JVM | IDE Gradle Settings |
| Tomcat JVM | 기동 로그·Process Command |
| CI JVM | Pipeline Log |

## 5.1.5 Gradle 버전 확인

gradle --version

또는 Wrapper 정책을 사용하는 저장소라면 다음을 실행한다.

./gradlew --version

Windows:

gradlew.bat --version

### 확인 항목

Gradle Version
Kotlin Version
Groovy Version
JVM Version
JVM Vendor
Daemon JVM
Operating System

특히 Gradle 출력의 JVM이 java -version 결과와 같은지 확인한다.

## 5.1.6 Gradle Wrapper와 시스템 Gradle

두 방식의 차이는 다음과 같다.

| 구분 | Gradle Wrapper | 시스템 Gradle |
| --- | --- | --- |
| 실행 | gradlew | gradle |
| 버전 | 저장소가 고정 | PC 설치값 |
| 재현성 | 높음 | 개인별 차이 가능 |
| 초기 설정 | Wrapper 파일 필요 | Gradle 별도 설치 |
| CI 적용 | 편리 | Agent 설정 필요 |
| 권장도 | 일반적으로 권장 | 내부망 제약 시 사용 |

### Wrapper 필수 파일

gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties

일부 파일만 존재하면 완전한 Wrapper라고 보기 어렵다.

### 프로젝트 확인 포인트

현재 자료에는 Gradle Wrapper 속성 파일과 시스템 Gradle 사용 설명이 함께 존재할 수 있다.

이 경우 다음 중 하나로 기준을 확정해야 한다.

대안 A
Wrapper 4종 파일을 완전하게 관리하고
모든 개발·CI가 gradlew를 사용한다.

대안 B
Wrapper를 사용하지 않고
승인된 Gradle 설치 경로와 버전을
개발환경·CI에서 동일하게 관리한다.

Wrapper 흔적과 시스템 Gradle 정책을 혼재시키는 것은 피한다.

## 5.1.7 설치 경로 표준

### 권장 예시

C:\\DevTools\\
├─ jdk-21
├─ gradle-8.10.1
├─ git
└─ workspace

### 피해야 할 경로

사용자별 임시 다운로드 경로
한글·특수문자가 과도한 경로
너무 긴 경로
괄호가 포함된 경로
OneDrive 자동동기화 경로
관리자 권한이 필요한 경로

경로에 공백이나 괄호가 있다고 반드시 실패하는 것은 아니다. 그러나 오래된 배치 파일과 인용부호 처리가 미흡한 스크립트에서는 오류 가능성이 커진다.

## 5.1.8 JDK Vendor

Java 표준 사양이 같더라도 배포 Vendor가 다를 수 있다.

예:

Eclipse Temurin
Oracle JDK
Amazon Corretto
Microsoft Build of OpenJDK
Red Hat OpenJDK

### Vendor를 통일해야 하는 이유

| 이유 | 설명 |
| --- | --- |
| 보안패치 | 적용 시점과 배포정책 차이 |
| 인증 | 내부 보안 승인 제품 차이 |
| 지원 | 운영지원 계약 차이 |
| 진단 | 배포별 기본옵션 차이 가능 |
| 재현성 | 개발·CI·운영 조합 통일 |

개발·CI·운영의 Major Version은 반드시 일치시킨다. Vendor와 Patch Version도 프로젝트 승인 기준에 따라 통제한다.

## 5.1.9 Java Toolchain

Gradle Java Toolchain을 사용하면 빌드에 필요한 Java 버전을 선언할 수 있다.

java {
toolchain {
languageVersion = JavaLanguageVersion.of(21)
}
}

### 장점

| 장점 | 설명 |
| --- | --- |
| 버전 명시 | 프로젝트가 JDK 21을 요구함을 코드로 표현 |
| IDE 연계 | IDE가 Toolchain을 인식 가능 |
| CI 일치 | Runner의 Java 선택 명확화 |
| 오류 조기발견 | 낮은 JDK 사용 차단 |

그러나 Toolchain만 선언했다고 운영 Tomcat의 JVM까지 자동으로 바뀌는 것은 아니다.

Build Toolchain
≠ Runtime JVM

운영 JVM은 별도로 검증해야 한다.

## 5.1.10 개발환경 버전 매트릭스

| 구간 | Java | Gradle | 실행방식 | 검증 증적 |
| --- | --- | --- | --- | --- |
| 개발자 CLI | 21 | 승인된 8.x | gradle·gradlew | Version 출력 |
| IDE | 21 | 동일 | IDE Gradle | 설정 캡처 |
| 단위 테스트 | 21 | 동일 | Gradle Test | Test Report |
| CI | 21 | 동일 | Pipeline | Build Log |
| 로컬 bootRun | 21 | 동일 | 업무별 bootRun | 기동 로그 |
| 통합 Tomcat | 21 | N/A | WAR | Tomcat Log |
| 운영 Tomcat | 21 | N/A | WAR | JVM 증적 |

# 5.2 JAVA\_HOME과 여러 JDK 관리

## 5.2.1 JAVA\_HOME의 역할

JAVA\_HOME은 JDK 설치 루트를 가리킨다.

정상 예:

JAVA\_HOME=C:\\DevTools\\jdk-21

잘못된 예:

JAVA\_HOME=C:\\DevTools\\jdk-21\\bin
JAVA\_HOME=C:\\DevTools\\jdk-21\\bin\\java.exe

PATH에는 다음을 추가한다.

%JAVA\_HOME%\\bin

Linux:

export JAVA\_HOME=/opt/jdk-21
export PATH="$JAVA\_HOME/bin:$PATH"

## 5.2.2 JAVA\_HOME과 PATH의 차이

| 항목 | 역할 |
| --- | --- |
| JAVA\_HOME | Java 기반 도구가 참조하는 JDK 루트 |
| PATH | 명령 입력 시 실행파일을 찾는 순서 |
| IDE SDK | IDE 내부 컴파일·코드분석 JDK |
| Gradle JVM | Gradle Daemon이 실행되는 JDK |
| Toolchain | Java Compile·Test에 사용할 JDK |

JAVA\_HOME이 JDK 21이어도 PATH 앞쪽에 JDK 17이 있으면 java -version은 17이 나올 수 있다.

## 5.2.3 여러 JDK 충돌 예시

C:\\Java\\jdk-17\\bin
C:\\Windows\\System32
C:\\DevTools\\jdk-21\\bin

PATH는 위에서 아래 순서로 찾는다.

따라서 첫 번째 Java가 실행된다.

### 점검

where java

예:

C:\\Java\\jdk-17\\bin\\java.exe
C:\\DevTools\\jdk-21\\bin\\java.exe

첫 번째 결과가 실제 실행 대상이다.

## 5.2.4 Windows 환경변수 적용 순서

1\. JDK 21 설치
2\. JAVA\_HOME 등록
3\. PATH의 기존 Java 항목 확인
4\. %JAVA\_HOME%\\bin을 적절한 위치에 등록
5\. 기존 터미널 종료
6\. 새 터미널 실행
7\. java·javac·Gradle 버전 확인

열려 있던 터미널은 이전 환경변수를 계속 사용할 수 있다.

환경변수를 수정한 뒤에는 새 터미널을 열어야 한다.

## 5.2.5 JDK 전환 스크립트

여러 프로젝트가 서로 다른 Java 버전을 사용한다면 전환 스크립트를 사용할 수 있다.

### Windows 예시

@echo off
set "JAVA\_HOME=C:\\DevTools\\jdk-21"
set "PATH=%JAVA\_HOME%\\bin;%PATH%"

echo JAVA\_HOME=%JAVA\_HOME%
java -version
javac -version

### PowerShell 예시

$env:JAVA\_HOME = "C:\\DevTools\\jdk-21"
$env:Path = "$env:JAVA\_HOME\\bin;$env:Path"

java -version
javac -version

### Linux 예시

export JAVA\_HOME=/opt/jdk-21
export PATH="$JAVA\_HOME/bin:$PATH"

java -version
javac -version

## 5.2.6 스크립트의 절대경로 문제

다음처럼 개인 PC의 Gradle 경로를 스크립트에 직접 고정하면 다른 개발자가 실행하기 어렵다.

set "GRADLE\_HOME=C:\\Programming\\gradle-8.10.1"

### 문제

| 문제 | 영향 |
| --- | --- |
| 개인 경로 의존 | 다른 PC에서 즉시 실패 |
| 버전 변경 어려움 | 여러 스크립트 수정 |
| CI 재사용 어려움 | Agent 경로 별도 대응 |
| 표준 확인 어려움 | 실제 사용 버전 불명확 |

### 개선 순서

1\. 명시적 Override 환경변수
2\. 프로젝트 표준 환경변수
3\. Wrapper
4\. PATH 탐색
5\. 없으면 명확한 오류

### 개선 예시

@echo off
setlocal

if defined GRADLE\_HOME\_OVERRIDE (
set "GRADLE\_HOME=%GRADLE\_HOME\_OVERRIDE%"
)

if exist "%~dp0..\\gradlew.bat" (
call "%~dp0..\\gradlew.bat" %\*
exit /b %errorlevel%
)

where gradle.bat >nul 2>&1
if errorlevel 1 (
echo \[ERROR\] Gradle을 찾을 수 없습니다.
exit /b 1
)

call gradle.bat %\*

## 5.2.7 Gradle JVM 확인

gradle --version

출력에서 다음을 확인한다.

JVM: 21.x
Java home: 승인된 JDK 21 경로

Gradle Daemon은 이미 실행 중인 이전 JDK를 사용할 수 있다.

설정 변경 후에는 Daemon을 종료한다.

gradle --stop

Wrapper:

./gradlew --stop

그 후 다시 실행한다.

## 5.2.8 IDE와 명령행 불일치

### 대표 사례

IDE Project SDK JDK 21
IDE Gradle JVM JDK 17
JAVA\_HOME JDK 21
PATH java JDK 11
CI JDK 21
Tomcat JDK 17

이 상태에서는 실행 위치마다 다른 결과가 발생한다.

### 목표 상태

Project SDK
\= Language Level
\= Gradle JVM
\= JAVA\_HOME
\= CI JVM
\= Tomcat Runtime JVM
\= JDK 21

## 5.2.9 JDK 버전 변경 시 처리

JDK를 변경한 뒤 다음 작업을 수행한다.

Gradle Daemon 종료
↓
IDE 재시작
↓
Project Reload
↓
Build 디렉터리 삭제
↓
Gradle Cache 영향 확인
↓
Clean Build
↓
단위 테스트
↓
대표 거래 실행

명령 예:

gradle --stop
gradle clean build

필요한 경우 IDE 내부 Cache를 재구성하되, Cache 삭제를 모든 문제의 기본 해결책으로 사용하지 않는다.

## 5.2.10 정상·경계·실패 예시

| 구분 | 상황 | 판단 |
| --- | --- | --- |
| 정상 | CLI·IDE·Gradle·CI가 모두 JDK 21 | 표준 충족 |
| 경계 | JDK 21이지만 Vendor·Patch가 다름 | 프로젝트 정책 확인 |
| 경계 | IDE는 21, Tomcat은 17 | 운영 실행 실패 위험 |
| 실패 | PATH가 JDK 11을 우선 | 수정 필요 |
| 실패 | JAVA\_HOME이 bin을 가리킴 | 수정 필요 |
| 실패 | 개인 Gradle 경로 하드코딩 | 재현성 부족 |
| 실패 | 빌드 JDK와 운영 JDK Major 불일치 | 배포 금지 |

# 5.3 UTF-8·MS949·줄바꿈 문제

## 5.3.1 인코딩이란 무엇인가

인코딩은 문자를 바이트로 표현하는 규칙이다.

같은 바이트를 다른 인코딩으로 해석하면 문자가 깨진다.

한글 문자열
↓ UTF-8로 저장
UTF-8 바이트
↓ MS949로 잘못 해석
깨진 문자열

## 5.3.2 UTF-8과 MS949

| 항목 | UTF-8 | MS949 |
| --- | --- | --- |
| 범위 | 전 세계 문자 | 한국어 중심 |
| 호환성 | Java·Web·Linux 표준 | Windows 레거시 |
| 영문 크기 | 1 Byte | 1 Byte |
| 한글 크기 | 일반적으로 3 Byte | 2 Byte |
| 권장 사용 | 소스·설정·HTTP·로그 파일 | 일부 Windows 콘솔·레거시 파일 |
| 위험 | BOM·도구 설정 | 국제문자·Linux 호환성 |

NSIGHT TCF의 신규 소스와 리소스는 UTF-8을 기본으로 두는 것이 적절하다.

## 5.3.3 인코딩은 한 가지가 아니다

개발자가 “UTF-8로 설정했다”고 말할 때 어느 계층인지 분명해야 한다.

| 인코딩 계층 | 예 |
| --- | --- |
| 소스 파일 | .java, .yml, .xml |
| Gradle Compile | options.encoding |
| JVM 기본 Charset | file.encoding |
| 표준 출력 | stdout.encoding |
| 표준 오류 | stderr.encoding |
| HTTP 요청·응답 | Content-Type Charset |
| JSON | 일반적으로 UTF-8 |
| 로그 Console | Console Appender Charset |
| 로그 File | File Appender Charset |
| DB | DB Character Set |
| JDBC | Driver 변환 |
| 파일 업로드 | 입력파일 Charset |
| Git | Working Tree 변환 |

한 영역만 UTF-8로 바꿔서는 전체 문제가 해결되지 않는다.

## 5.3.4 소스와 리소스 기준

### UTF-8 권장 대상

\*.java
\*.groovy
\*.gradle
\*.yml
\*.yaml
\*.properties
\*.xml
\*.sql
\*.json
\*.md
\*.html
\*.js
\*.css

### 주의 대상

\*.bat
\*.cmd
\*.ps1
\*.sh
레거시 인터페이스 파일
대외기관 고정길이 전문
MS949 CSV

레거시 파일은 무조건 UTF-8로 일괄 변환하지 않는다. 외부 계약과 소비 프로그램이 어떤 Charset을 요구하는지 먼저 확인한다.

## 5.3.5 Gradle Compile Encoding

tasks.withType(JavaCompile).configureEach {
options.encoding = 'UTF-8'
}

테스트와 Javadoc에도 적용할 수 있다.

tasks.withType(Javadoc).configureEach {
options.encoding = 'UTF-8'
options.charSet = 'UTF-8'
}

### Resource Filtering 주의

Properties·YAML 리소스를 필터링하거나 복사할 때도 Charset을 명시한다.

processResources {
filteringCharset = 'UTF-8'
}

## 5.3.6 JVM 기본 인코딩

로컬 실행 예:

\-Dfile.encoding=UTF-8
\-Dstdout.encoding=UTF-8
\-Dstderr.encoding=UTF-8

Windows 배치 예:

set "JAVA\_TOOL\_OPTIONS=-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"

JDK 버전에 따라 기본 Charset 정책이 달라질 수 있으므로 중요한 시스템에서는 암묵적 기본값보다 명시적 설정을 사용한다.

## 5.3.7 Windows 콘솔

Windows 콘솔 코드페이지를 UTF-8로 전환할 때 다음을 사용할 수 있다.

chcp 65001

MS949 코드페이지는 일반적으로 다음과 같다.

chcp 949

그러나 코드페이지 변경만으로 Java 출력이 모두 정상화되는 것은 아니다.

콘솔 코드페이지
\+ JVM stdout Charset
\+ 로그 Appender Charset
\+ 터미널 Font

이 네 조건을 함께 확인한다.

## 5.3.8 Console Log와 File Log

개발 Windows 콘솔은 MS949를 사용하고 로그 파일은 UTF-8을 사용할 수도 있다.

| 출력대상 | 권장 |
| --- | --- |
| 운영 로그 파일 | UTF-8 |
| Linux Console | UTF-8 |
| 개발 Windows Console | UTF-8 권장, 레거시 환경은 MS949 명시 |
| 감사로그 | UTF-8 |
| 거래로그 DB | DB Charset 기준 |

### 중요한 원칙

Console Charset
≠ File Log Charset

두 값을 각각 설정하고 문서화한다.

## 5.3.9 UTF-8 BOM

UTF-8 BOM은 파일 처음의 특수 바이트다.

일부 편집기는 이를 표시하지 않는다.

### BOM으로 인한 문제

| 파일 | 증상 |
| --- | --- |
| Shell Script | 첫 줄 명령 인식 실패 |
| YAML | 첫 Key 파싱 문제 가능 |
| Properties | 첫 Key에 보이지 않는 문자 포함 |
| SQL | 첫 Token 인식 문제 |
| Batch | 일부 실행환경에서 첫 명령 문제 |
| JSON | 엄격한 Parser 오류 가능 |

### 권장 기준

| 파일 | 권장 |
| --- | --- |
| Java·YAML·XML·JSON·SQL | UTF-8 without BOM |
| Shell Script | UTF-8 without BOM |
| Batch | 프로젝트 실행환경 검증 후 통일 |
| PowerShell | PowerShell 버전에 맞춘 기준 |
| CSV | 소비 시스템 계약에 따름 |

## 5.3.10 줄바꿈 문자

운영체제별 대표 줄바꿈은 다음과 같다.

| 운영체제 | 줄바꿈 |
| --- | --- |
| Windows | CRLF |
| Linux·Unix | LF |
| 구형 macOS | CR |

Java와 Git은 대부분 양쪽을 처리할 수 있지만, Shell Script와 Git Diff에서는 문제가 될 수 있다.

## 5.3.11 줄바꿈 권장 기준

| 파일 | 권장 줄바꿈 |
| --- | --- |
| Java·YAML·XML·SQL·JSON | LF |
| Shell Script | LF |
| Markdown | LF |
| Batch·CMD | CRLF |
| PowerShell | 팀 표준, 일반적으로 CRLF 가능 |
| 외부 고정길이 파일 | 계약 기준 |

## 5.3.12 .gitattributes

예시:

\* text=auto

\*.java text eol=lf
\*.groovy text eol=lf
\*.gradle text eol=lf
\*.yml text eol=lf
\*.yaml text eol=lf
\*.xml text eol=lf
\*.sql text eol=lf
\*.json text eol=lf
\*.md text eol=lf
\*.sh text eol=lf

\*.bat text eol=crlf
\*.cmd text eol=crlf
\*.ps1 text eol=crlf

\*.jar binary
\*.war binary
\*.png binary
\*.jpg binary
\*.docx binary
\*.xlsx binary
\*.pdf binary

### 효과

| 효과 | 설명 |
| --- | --- |
| 대량 Diff 방지 | 줄바꿈 변경만으로 전체 파일이 바뀌는 현상 방지 |
| Shell 안정성 | Linux에서 CRLF 실행 오류 방지 |
| 일관성 | OS가 달라도 저장소 기준 유지 |
| 리뷰 품질 | 실제 코드 변경만 확인 |

## 5.3.13 .editorconfig

예시:

root = true

\[\*\]
charset = utf-8
end\_of\_line = lf
insert\_final\_newline = true
trim\_trailing\_whitespace = true

\[\*.{java,groovy,gradle,yml,yaml,xml,sql,json,md}\]
indent\_style = space

\[\*.{bat,cmd,ps1}\]
end\_of\_line = crlf

\[\*.md\]
trim\_trailing\_whitespace = false

.editorconfig는 IDE 편집 기준을 통일하고 .gitattributes는 Git 저장 기준을 통제한다.

.editorconfig
\= 편집기 규칙

.gitattributes
\= 저장소 변환 규칙

## 5.3.14 core.autocrlf

개별 개발자가 임의로 다음 값을 변경하면 팀 내 Diff가 달라질 수 있다.

git config core.autocrlf true
git config core.autocrlf input
git config core.autocrlf false

프로젝트에서는 .gitattributes를 우선 기준으로 삼고 개인 Git 설정에 지나치게 의존하지 않는 것이 좋다.

현재 값 확인:

git config --show-origin --get core.autocrlf

## 5.3.15 줄바꿈만 변경된 대량 Diff

### 증상

파일 내용은 바꾸지 않았는데
수백 개 파일이 Modified로 표시된다.

### 원인 후보

IDE Line Separator 변경
core.autocrlf 변경
.gitattributes 신규 적용
파일 전체 포맷팅
인코딩 일괄 변환
BOM 추가·제거

### 금지 행동

원인을 확인하지 않고 전체 파일 Commit

### 복구 절차

1\. 작업 내용 별도 보관
2\. git diff --ignore-space-at-eol 확인
3\. file·인코딩·줄바꿈 확인
4\. .gitattributes 기준 확인
5\. 의도한 파일만 복구·재적용
6\. 변경 범위를 작은 Commit으로 분리

## 5.3.16 한글 깨짐 진단 순서

어느 화면에서 깨지는가?
↓
원본 파일은 정상인가?
↓
파일 실제 Charset은 무엇인가?
↓
컴파일·리소스 복사 Charset은 무엇인가?
↓
JVM 기본 Charset은 무엇인가?
↓
HTTP Header Charset은 무엇인가?
↓
DB 저장값은 정상인가?
↓
Console·Log Appender Charset은 무엇인가?

### 깨지는 위치별 후보

| 위치 | 원인 후보 |
| --- | --- |
| IDE 편집기 | 파일 Charset 오인 |
| Compiler | Compile Encoding |
| YAML 설정 | BOM·Charset |
| Console | Codepage·stdout Charset |
| 로그 파일 | Appender Charset |
| DB | DB Charset·Column Type |
| HTTP 응답 | Content-Type |
| CSV 다운로드 | 생성 Charset·Excel 해석 |
| 외부 연계 | 상대 시스템 계약 |

## 5.3.17 레거시 MS949 파일 처리

외부 시스템이 MS949 파일을 요구한다면 경계에서 명시적으로 변환한다.

try (BufferedReader reader = Files.newBufferedReader(
inputPath,
Charset.forName("MS949"))) {

// 명시적 처리
}

출력:

try (BufferedWriter writer = Files.newBufferedWriter(
outputPath,
Charset.forName("MS949"))) {

// 명시적 처리
}

다음은 피한다.

new FileReader(file);
new FileWriter(file);

기본 Charset에 의존하기 때문이다.

# 5.4 IDE 프로젝트 설정 점검

## 5.4.1 IDE의 역할

IDE는 소스 편집, 컴파일 도움, 테스트, 디버깅과 Git 기능을 제공한다.

그러나 IDE는 프로젝트 빌드 기준 자체가 아니다.

IDE
\= 개발 편의 도구

Gradle Build
\= 프로젝트 공식 빌드

IDE에서 성공했더라도 Gradle Clean Build가 실패하면 완료로 보지 않는다.

## 5.4.2 공통 IDE 점검 항목

| 영역 | 점검값 |
| --- | --- |
| Project SDK | JDK 21 |
| Language Level | Java 21 |
| Gradle JVM | JDK 21 |
| Build Tool | Gradle |
| File Encoding | UTF-8 |
| Properties Encoding | 프로젝트 기준 |
| Line Separator | LF 기본 |
| Annotation Processing | 필요 시 활성화 |
| Lombok Plugin | 사용 IDE에 설치 |
| Git Root | 프로젝트 Root |
| Working Directory | 프로젝트 Root 또는 모듈 기준 |
| Active Profile | local |
| Console Charset | 명시적 UTF-8 |
| Test Runner | Gradle 기준 검토 |

## 5.4.3 IntelliJ IDEA 설정

### Project SDK

File
→ Project Structure
→ Project
→ SDK
→ JDK 21

### Language Level

Project language level
→ 21

### Gradle JVM

Settings
→ Build, Execution, Deployment
→ Build Tools
→ Gradle
→ Gradle JVM
→ JDK 21

### Encoding

Settings
→ Editor
→ File Encodings

Global Encoding : UTF-8
Project Encoding : UTF-8
Properties : 프로젝트 기준

### Line Separator

Editor 우측 하단
→ LF

### Annotation Processing

Settings
→ Build, Execution, Deployment
→ Compiler
→ Annotation Processors
→ Enable

## 5.4.4 Eclipse 설정

### Installed JRE

Window
→ Preferences
→ Java
→ Installed JREs
→ JDK 21 선택

### Compiler Compliance

Project
→ Properties
→ Java Compiler
→ Compiler compliance level 21

### Project JRE

Project
→ Properties
→ Java Build Path
→ Libraries
→ JRE System Library
→ JavaSE-21

### Encoding

Project
→ Properties
→ Resource
→ Text file encoding
→ UTF-8

### Gradle Buildship

Project 우클릭
→ Gradle
→ Refresh Gradle Project

Eclipse 내부 JRE와 Buildship이 사용하는 JVM이 다르지 않은지 확인한다.

## 5.4.5 VS Code 설정

필요한 경우 다음 항목을 확인한다.

java.jdt.ls.java.home
java.configuration.runtimes
files.encoding
files.eol
java.import.gradle.java.home

예:

{
"files.encoding": "utf8",
"files.eol": "\\n",
"java.jdt.ls.java.home": "C:\\\\DevTools\\\\jdk-21",
"java.import.gradle.java.home": "C:\\\\DevTools\\\\jdk-21"
}

개인 절대경로가 포함된 설정은 저장소 공용 설정과 개인 설정을 구분한다.

## 5.4.6 Gradle 프로젝트 가져오기

프로젝트는 일반 Java 프로젝트가 아니라 Gradle 프로젝트로 가져온다.

### IntelliJ

프로젝트 Root의 settings.gradle 또는 build.gradle 열기
→ Gradle Project로 Import

### Eclipse

File
→ Import
→ Gradle
→ Existing Gradle Project
→ 프로젝트 Root 선택

### 잘못된 방식

각 모듈 src 폴더를
별도의 Java Project로 Import

이렇게 하면 Project Dependency, Source Set, Annotation Processor와 Resource 설정을 잃을 수 있다.

## 5.4.7 프로젝트 Root 판단

다음 파일이 있는 위치를 Root로 본다.

settings.gradle
build.gradle
gradle.properties
gradlew 또는 Gradle 실행정책

모듈 폴더의 src/main/java를 Root로 선택하지 않는다.

## 5.4.8 IDE Build와 Gradle Build

| 방식 | 특징 | 사용 |
| --- | --- | --- |
| IDE Compiler | 빠른 편집 피드백 | 개발 중 |
| Gradle Build | 공식 의존성·Task 적용 | 완료 검증 |
| Gradle Test | CI와 동일한 테스트 | 필수 |
| bootRun | Spring 실행 설정 적용 | 로컬 실행 |

### 완료 판단

IDE 오류 없음
\+ Gradle Clean Build 성공
\+ Test 성공
\+ bootRun 성공
\= 개발환경 정상

## 5.4.9 Lombok 문제

### 증상

IDE에서 Getter·Constructor가 없다고 표시
Gradle Build는 성공

### 원인

Lombok Plugin 누락
Annotation Processing 비활성
IDE Cache 불일치
의존성 누락

### 확인 순서

build.gradle 의존성
↓
Lombok Plugin
↓
Annotation Processing
↓
Gradle Refresh
↓
Clean Build

업무 코드에 임시 Getter·Constructor를 수동 추가해 문제를 숨기지 않는다.

## 5.4.10 Mapper XML 인식 문제

IDE에서 Mapper XML을 찾지 못해도 Gradle 실행 시 정상일 수 있다.

반대로 IDE가 인식하지만 빌드 산출물에 포함되지 않을 수도 있다.

### 확인

src/main/resources/mapper
↓
processResources
↓
build/resources/main
↓
WAR의 WEB-INF/classes/mapper

### 완료 증적

| 증적 | 확인 |
| --- | --- |
| Mapper XML 위치 | 표준 Resources 경로 |
| Namespace | Java Mapper와 일치 |
| Build 산출물 | XML 포함 |
| 실행 | Mapper Bean 등록 |
| 거래 | 실제 SQL 실행 |

## 5.4.11 Run Configuration

업무 모듈 실행 시 다음을 확인한다.

| 항목 | 예 |
| --- | --- |
| Gradle Task | :sv-service:bootRun |
| Working Directory | 프로젝트 Root |
| JDK | 21 |
| Profile | local |
| VM Option | \-Dfile.encoding=UTF-8 |
| Environment | 필요한 DB·JWT 설정 |
| Port | 업무별 로컬 Port |
| Context | /sv 등 |

개인 DB 비밀번호와 Private Key를 IDE 공용 설정파일에 Commit하지 않는다.

## 5.4.12 환경설정 저장 범위

| 설정 | 저장소 Commit | 이유 |
| --- | --- | --- |
| .editorconfig | O | 팀 공통 |
| .gitattributes | O | Git 기준 |
| Gradle Toolchain | O | 빌드 기준 |
| 공통 Run Template | 검토 | 비밀정보 제외 |
| 개인 SDK 경로 | X | PC별 다름 |
| 개인 DB 비밀번호 | X | 비밀정보 |
| IDE Cache | X | 생성파일 |
| Workspace Metadata | 원칙적 X | 개인 상태 |
| .idea | 프로젝트 정책 | 공용·개인 파일 구분 |
| .settings | 프로젝트 정책 | 표준화 필요 시 일부 |

# 개발환경 책임 경계

| 구성 | 프로젝트 | 개발자 | DevOps | 운영 |
| --- | --- | --- | --- | --- |
| 표준 JDK 결정 | A | I | C | C |
| JDK 설치 | C | R | R | R |
| Gradle 버전 | A | I | R | I |
| Toolchain 설정 | A/R | I | C | I |
| IDE 설정 가이드 | A/R | R | I | I |
| .editorconfig | A/R | C | C | I |
| .gitattributes | A/R | C | C | I |
| CI JVM | C | I | A/R | I |
| Tomcat JVM | C | I | C | A/R |
| Charset 정책 | A | R | C | C |
| 비밀정보 관리 | C | R | A/R | A/R |

R은 수행, A는 최종 책임, C는 협의, I는 공유를 의미한다.

# 정상 처리 흐름

프로젝트 표준 확인
↓
JDK 21 설치
↓
JAVA\_HOME·PATH 설정
↓
java·javac 버전 확인
↓
Gradle JVM 확인
↓
IDE SDK·Language Level 설정
↓
UTF-8·LF 설정
↓
Gradle Project Import
↓
Gradle Daemon 종료
↓
Clean Build
↓
단위 테스트
↓
대표 업무 bootRun
↓
표준 거래 호출
↓
개발환경 점검표 기록

# 오류 흐름

## JDK 불일치

IDE JDK 21
Gradle JVM 17
↓
Compile Task 실패
↓
gradle --version 확인
↓
Gradle JVM 변경
↓
Daemon 종료
↓
Clean Build

## 인코딩 불일치

소스 UTF-8
Compiler MS949
↓
한글 주석·문자열 컴파일 오류
↓
파일 Charset 확인
↓
Gradle options.encoding 확인
↓
IDE Encoding 확인
↓
변환 범위 검토

## 줄바꿈 대량 변경

Git Clone
↓
IDE가 CRLF로 일괄 변환
↓
수백 파일 Modified
↓
Commit 중지
↓
.gitattributes·core.autocrlf 확인
↓
실제 변경만 재적용

## Wrapper 불완전

gradle-wrapper.properties 존재
gradlew 실행파일 없음
↓
신규 개발자 실행 실패
↓
Wrapper 4종 복구
또는
시스템 Gradle 정책으로 정리

# 정상 예시

java -version
→ 21

javac -version
→ 21

JAVA\_HOME
→ C:\\DevTools\\jdk-21

gradle --version
→ Gradle 8.x
→ JVM 21

IDE Project SDK
→ 21

IDE Gradle JVM
→ 21

Project Encoding
→ UTF-8

Java·YAML·XML
→ LF

Batch
→ CRLF

gradle clean build
→ SUCCESS

:sv-service:bootRun
→ 정상 기동

# 금지 예시

JAVA\_HOME=C:\\Java\\jdk-21\\bin

PATH의 첫 Java=JDK 11

IDE Gradle JVM=JDK 17

Gradle 경로를 개인 디렉터리로 하드코딩

모든 파일을 확인 없이 UTF-8로 일괄 변환

줄바꿈만 바뀐 수백 개 파일 Commit

개인 DB 비밀번호를 Run Configuration에 저장 후 Commit

IDE Build 성공만으로 완료 처리

운영 Tomcat JDK 확인 없이 WAR 배포

# 자동검증 및 품질 Gate

## 1\. 환경 사전검사 스크립트

### PowerShell 예시

$ErrorActionPreference = "Stop"

Write-Host "JAVA\_HOME=$env:JAVA\_HOME"

java -version
javac -version
gradle --version

$javaVersion = (& java -version 2>&1 | Select-Object -First 1)

if ($javaVersion -notmatch '"21') {
throw "JDK 21이 아닙니다: $javaVersion"
}

Write-Host "\[OK\] Java environment"

## 2\. CI 환경검사

Pipeline 시작 시 다음 정보를 출력한다.

java -version
javac -version
gradle --version
locale
file.encoding
user.timezone
Git Commit ID

환경정보 출력에는 비밀번호와 Token을 포함하지 않는다.

## 3\. Gradle 검증

java {
toolchain {
languageVersion = JavaLanguageVersion.of(21)
}
}

tasks.withType(JavaCompile).configureEach {
options.encoding = 'UTF-8'
}

## 4\. 줄바꿈 검사

CI에서 다음을 검사할 수 있다.

| 검사 | 실패 조건 |
| --- | --- |
| Shell CRLF | .sh에 CRLF 존재 |
| BOM | 금지 파일에 BOM 존재 |
| Encoding | UTF-8이 아닌 신규 소스 |
| Trailing Space | 불필요한 공백 |
| Final Newline | 마지막 줄바꿈 누락 |
| Binary 오인 | Binary가 Text로 관리 |

## 5\. 환경 Gate

다음 조건 중 하나라도 충족되지 않으면 개발환경 준비를 완료로 보지 않는다.

JDK Major 일치
Gradle 버전 일치
IDE Gradle JVM 일치
UTF-8 정책 적용
줄바꿈 정책 적용
Clean Build 성공
Test 성공
대표 업무 기동 성공
비밀정보 미포함
점검표 저장

# 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| DEV-ENV-001 | JDK 21 정상 설정 | Build 성공 |
| DEV-ENV-002 | PATH를 JDK 17로 변경 | 사전검사 실패 |
| DEV-ENV-003 | Gradle JVM 17 사용 | 명확한 버전 오류 |
| DEV-ENV-004 | JAVA\_HOME을 bin으로 설정 | 점검 스크립트 실패 |
| DEV-ENV-005 | IDE SDK 21·Gradle JVM 17 | 불일치 탐지 |
| DEV-ENV-006 | Java 파일 UTF-8 | 한글 정상 컴파일 |
| DEV-ENV-007 | Java 파일 MS949 | Encoding 검사 실패 |
| DEV-ENV-008 | YAML에 BOM 삽입 | Parser·검사 실패 |
| DEV-ENV-009 | Shell CRLF | CI 검사 실패 |
| DEV-ENV-010 | Batch CRLF | Windows 정상 실행 |
| DEV-ENV-011 | Console UTF-8 | 한글 로그 정상 |
| DEV-ENV-012 | Console Charset 불일치 | 깨짐 재현·원인 식별 |
| DEV-ENV-013 | Wrapper 파일 일부 누락 | 사전검사 실패 |
| DEV-ENV-014 | 개인 Gradle 경로 없는 PC | 표준 방식으로 실행 |
| DEV-ENV-015 | Gradle Daemon 이전 JDK 사용 | \--stop 후 정상화 |
| DEV-ENV-016 | Lombok Processing 비활성 | IDE 오류 재현 |
| DEV-ENV-017 | Clean Build | 기존 산출물 없이 성공 |
| DEV-ENV-018 | CI와 로컬 비교 | Version Matrix 일치 |
| DEV-ENV-019 | 비밀정보 포함 Run 설정 | 보안 검사 실패 |
| DEV-ENV-020 | 대표 거래 호출 | 응답·로그·DB 결과 정상 |

# 따라 하는 실무 절차

## 1단계. 프로젝트 표준을 찾는다

확인 대상:

개발환경 표준서
build.gradle
settings.gradle
gradle-wrapper.properties
CI Pipeline
Tomcat setenv
실행 스크립트

기록:

| 항목 | 표준값 | 근거 |
| --- | --- | --- |
| JDK | 21 | Build·문서 |
| Gradle | 승인된 8.x | Wrapper·CI |
| Encoding | UTF-8 | Build·EditorConfig |
| Line Ending | LF 중심 | GitAttributes |
| Profile | local | bootRun 설정 |

## 2단계. 명령행 Java를 확인한다

java -version
javac -version
where java
echo %JAVA\_HOME%

Linux:

java -version
javac -version
which java
echo "$JAVA\_HOME"

## 3단계. Gradle JVM을 확인한다

gradle --version

또는:

./gradlew --version

JVM 값이 JDK 21인지 확인한다.

## 4단계. IDE를 설정한다

Project SDK
Language Level
Gradle JVM
File Encoding
Line Separator
Annotation Processing

모두 점검표에 기록한다.

## 5단계. Git 인코딩 정책을 확인한다

git config --show-origin --get core.autocrlf
git status
git diff --check

그리고 다음 파일을 확인한다.

.gitattributes
.editorconfig
.gitignore

## 6단계. 깨끗하게 빌드한다

gradle --stop
gradle clean build

업무 모듈만 먼저 확인할 수도 있다.

gradle clean :sv-service:build

## 7단계. 대표 업무를 기동한다

gradle :sv-service:bootRun

확인:

JDK
Profile
Port
Context Path
Datasource
TCF Bean
Handler Registry
Encoding

## 8단계. 한글 데이터를 포함한 거래를 호출한다

예:

{
"header": {
"businessCode": "SV",
"serviceId": "SV.Customer.selectSummary",
"transactionCode": "SV-INQ-0001"
},
"body": {
"customerName": "홍길동"
}
}

확인 위치:

HTTP 요청
Controller
애플리케이션 로그
SQL Parameter
DB 결과
표준 응답

## 9단계. 환경점검표를 저장한다

| 항목 | 확인값 | 판정 |
| --- | --- | --- |
| OS | Windows·Linux | 정상 |
| JDK | 21 | 정상 |
| Java 경로 | 승인 경로 | 정상 |
| Gradle | 표준 8.x | 정상 |
| Gradle JVM | 21 | 정상 |
| IDE SDK | 21 | 정상 |
| Encoding | UTF-8 | 정상 |
| Line Ending | 정책 일치 | 정상 |
| Clean Build | Success | 정상 |
| Test | Success | 정상 |
| bootRun | Success | 정상 |
| 대표 거래 | Success | 정상 |

# 완료 체크리스트

## JDK·Gradle

| 확인 항목 | 완료 |
| --- | --- |
| 프로젝트 표준 JDK 버전을 확인했다. | □ |
| JDK Vendor와 Patch 정책을 확인했다. | □ |
| JAVA\_HOME이 JDK 루트를 가리킨다. | □ |
| PATH의 첫 Java 경로를 확인했다. | □ |
| java와 javac 버전이 일치한다. | □ |
| Gradle JVM이 JDK 21이다. | □ |
| Wrapper 또는 시스템 Gradle 정책이 명확하다. | □ |
| Gradle Daemon을 재기동했다. | □ |

## IDE

| 확인 항목 | 완료 |
| --- | --- |
| Project SDK가 JDK 21이다. | □ |
| Language Level이 21이다. | □ |
| Gradle JVM이 JDK 21이다. | □ |
| Gradle Project로 Import했다. | □ |
| Annotation Processing을 확인했다. | □ |
| File Encoding이 UTF-8이다. | □ |
| 기본 Line Separator를 확인했다. | □ |
| 개인 비밀정보가 공용 설정에 없다. | □ |

## 인코딩·줄바꿈

| 확인 항목 | 완료 |
| --- | --- |
| 소스·리소스 Charset 기준이 UTF-8이다. | □ |
| Gradle Compile Encoding을 확인했다. | □ |
| JVM 기본 Charset을 확인했다. | □ |
| Console과 File Log Charset을 구분했다. | □ |
| .gitattributes가 존재한다. | □ |
| .editorconfig가 존재한다. | □ |
| Shell은 LF를 사용한다. | □ |
| Batch는 승인된 CRLF 기준을 사용한다. | □ |
| 금지 파일에 BOM이 없다. | □ |
| 대량 줄바꿈 Diff가 없다. | □ |

## 재현성

| 확인 항목 | 완료 |
| --- | --- |
| clean build가 성공했다. | □ |
| 단위 테스트가 성공했다. | □ |
| 대표 업무가 기동된다. | □ |
| 한글 요청·응답이 정상이다. | □ |
| 로컬과 CI 버전이 일치한다. | □ |
| 로컬과 운영 Java Major가 일치한다. | □ |
| 환경점검 결과를 저장했다. | □ |
| 다른 개발자가 같은 절차를 재현했다. | □ |

# 변경·호환성 관리

## JDK Upgrade

JDK 변경은 개인 개발도구 업데이트가 아니라 플랫폼 변경이다.

JDK Upgrade 제안
↓
Spring·Gradle·Tomcat·Library 호환성 검토
↓
ADR 작성
↓
개발·CI 환경 병행 구성
↓
전체 Build·Test
↓
성능·GC·보안 시험
↓
통합 Tomcat 검증
↓
운영 전환
↓
구버전 폐기

### JDK 변경 시 필수 검증

| 영역 | 검증 |
| --- | --- |
| Compile | 전체 모듈 |
| Test | 단위·통합 |
| Reflection | Framework 기능 |
| MyBatis | Mapper 동작 |
| JWT·TLS | 암호 알고리즘 |
| Tomcat | WAR 기동 |
| GC | Pause·Heap |
| Native Library | 호환성 |
| Script | JAVA\_HOME |
| CI | Agent·Cache |

## Gradle Upgrade

Gradle Wrapper·설치 버전 변경
↓
Plugin 호환성 확인
↓
Deprecation Warning 제거
↓
전체 Task 실행
↓
CI 적용
↓
개발자 환경 전환

Gradle 버전을 개인이 임의로 변경하지 않는다.

## 인코딩 변경

기존 MS949 파일을 UTF-8로 전환할 때는 다음을 분리한다.

1\. 파일 형식 변환 Commit
2\. 실제 업무 코드 변경 Commit

두 변경을 한 Commit에 섞으면 코드 리뷰가 불가능해진다.

# 제5장의 핵심 정리

첫째,
개발환경의 기준은 개인 IDE가 아니라
프로젝트 Build와 CI다.

둘째,
JAVA\_HOME, PATH, Gradle JVM,
IDE SDK와 Tomcat JVM은 서로 다른 설정이다.

셋째,
NSIGHT TCF의 기준 Java 버전은
프로젝트 Baseline에 따라 JDK 21로 통일한다.

넷째,
IDE에서 성공하더라도
Gradle Clean Build가 실패하면 완료가 아니다.

다섯째,
UTF-8 설정은 소스 파일뿐 아니라
Compiler·JVM·Console·로그·HTTP·DB를 구분해야 한다.

여섯째,
LF와 CRLF 차이는
대량 Git Diff와 Shell 실행 실패를 만들 수 있다.

일곱째,
.gitattributes와 .editorconfig를 사용해
개인 설정 의존도를 줄인다.

여덟째,
환경 차이는 추측하지 말고
버전 명령·경로·빌드 로그로 증명한다.

# 시사점

## 핵심 아키텍처 판단

개발환경은 개발자의 개인 편의 영역이 아니라 소프트웨어 공급망의 시작점이다.

개발자 PC
↓
소스·의존성
↓
Gradle Build
↓
CI Artifact
↓
Tomcat Runtime
↓
운영 서비스

첫 단계의 JDK·Gradle·인코딩이 다르면 최종 운영 산출물도 달라질 수 있다.

## 주요 위험

| 위험 | 영향 |
| --- | --- |
| 여러 JDK 혼재 | 개발자별 빌드 결과 차이 |
| IDE·Gradle JVM 불일치 | IDE 성공·CLI 실패 |
| 시스템 Gradle 개인 설치 | 버전 편차 |
| Wrapper 불완전 | 신규 환경 실행 실패 |
| 개인 경로 하드코딩 | 재현성 훼손 |
| 인코딩 일괄변환 | 소스·전문 손상 |
| 줄바꿈 대량변경 | 리뷰·Merge 곤란 |
| BOM 혼재 | Script·YAML 오류 |
| 운영 JVM 미검증 | 배포 후 기동 실패 |
| 비밀정보 IDE 저장 | 보안사고 |

## 우선 보완 과제

1.  Java 21과 Gradle 버전을 공식 Baseline으로 고정한다.
2.  Wrapper 사용 여부를 하나의 정책으로 확정한다.
3.  개인 절대경로가 포함된 실행 스크립트를 제거한다.
4.  .gitattributes와 .editorconfig를 기준본으로 관리한다.
5.  Java Toolchain과 Compile Encoding을 Build에 선언한다.
6.  CI 시작 단계에서 Java·Gradle 버전을 자동검사한다.
7.  개발·CI·Tomcat JVM 버전 매트릭스를 관리한다.
8.  UTF-8 BOM과 줄바꿈 검사를 품질 Gate에 추가한다.
9.  공통 IDE 설정 가이드와 점검표를 배포한다.
10.  깨끗한 PC·CI Agent에서 재현시험을 수행한다.

## 중장기 발전 방향

수동 설치 안내
↓
환경점검 스크립트
↓
Gradle Toolchain
↓
표준 Wrapper
↓
Dev Container·표준 VM
↓
재현 가능한 Build Image
↓
SBOM·Artifact 서명
↓
개발부터 운영까지 공급망 추적

# 마무리말

개발환경 문제는 프로그램을 만들기 전에 해결해야 한다.

환경이 표준화되지 않은 상태에서 업무 개발을 시작하면 문제 발생 시 다음 두 가지를 구분할 수 없다.

업무 코드가 잘못된 것인가?

개발환경이 다른 것인가?

제5장에서 가장 중요한 확인 명령은 복잡하지 않다.

java -version
javac -version
where 또는 which java
JAVA\_HOME
gradle --version
IDE Project SDK
IDE Gradle JVM
git status
gradle clean build

이 결과를 기록하고 서로 비교하면 대부분의 환경 문제를 빠르게 분류할 수 있다.

같은 소스
\+ 같은 JDK
\+ 같은 Gradle
\+ 같은 인코딩
\+ 같은 빌드 명령
\= 재현 가능한 개발환경

다음 장에서는 표준화된 PC 환경에서 Git 저장소를 Clone하고, 브랜치·커밋·Pull Request와 비밀정보 관리 기준을 학습한다.
