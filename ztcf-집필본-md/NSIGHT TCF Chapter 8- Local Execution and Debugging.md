<!-- source: ztcf-집필본/NSIGHT TCF Chapter 8- Local Execution and Debugging.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제8장. 로컬 실행과 첫 디버깅

## 이 장을 시작하며

제7장에서는 Gradle을 이용해 NSIGHT TCF 멀티모듈 프로젝트를 빌드하고, 업무 모듈의 의존성과 JAR·WAR 산출물을 확인했다.

이제 빌드한 업무 애플리케이션을 실제로 실행하고 거래 한 건을 디버깅해야 한다.

초보 개발자는 애플리케이션이 다음 로그를 출력하면 실행이 완료되었다고 생각하기 쉽다.

Started NsightSvServiceApplication

그러나 프로세스가 기동되었다는 사실과 업무 거래가 정상적으로 처리된다는 사실은 다르다.

애플리케이션 프로세스 기동
≠ Spring Bean 전체 정상
≠ Handler 등록 정상
≠ DB 연결 정상
≠ Mapper SQL 정상
≠ TCF 거래 정상

로컬 실행 완료는 다음 항목을 함께 확인해야 한다.

JDK·Gradle 정상
↓
올바른 Profile 활성화
↓
예상 Port 기동
↓
Datasource·HikariCP 정상
↓
MyBatis Mapper 등록
↓
TCF·STF·ETF Bean 생성
↓
ServiceId·Handler 등록
↓
대표 거래 호출
↓
표준 응답 확인
↓
로그·SQL·DB 결과 확인

디버깅도 단순히 오류가 발생한 줄에 Breakpoint를 두는 작업이 아니다.

요청이 어디에서 들어왔는가?

어떤 Profile과 설정값이 적용되었는가?

STF 전처리를 통과했는가?

어떤 ServiceId가 선택되었는가?

어느 Handler와 Service가 실행되었는가?

트랜잭션은 어디에서 시작되었는가?

어떤 SQL이 실행되었는가?

오류가 어디에서 변환되었는가?

응답과 로그에 어떤 식별자가 남았는가?

이 질문에 근거를 가지고 답할 수 있어야 한다.

NSIGHT TCF의 기준 호출 구조는 OnlineTransactionController → TCF.process() → STF.preProcess() → TimeoutExecutor → TransactionDispatcher → TransactionHandler → Facade → Service → Rule·DAO → Mapper → ETF다. 따라서 디버깅도 이 실행 순서를 따라가는 것이 가장 효과적이다.

## 핵심 관점

로컬 실행의 목적은
프로세스를 띄우는 것이 아니다.

프로젝트 표준 환경에서
요청 한 건이 예상한 코드·설정·데이터 경로를 지나
동일한 결과로 재현되는지를 증명하는 것이다.

## 학습 목표

이 장을 마치면 다음 내용을 직접 수행할 수 있어야 한다.

| 번호 | 학습 목표 |
| --- | --- |
| 1 | local, dev, prod Profile의 역할을 구분한다. |
| 2 | Spring 설정값의 우선순위를 설명한다. |
| 3 | 현재 활성 Profile과 실제 적용된 Port를 확인한다. |
| 4 | 업무 모듈의 Main Class와 bootRun Task를 찾는다. |
| 5 | 로컬 Datasource와 운영 Datasource를 구분한다. |
| 6 | 기동 로그에서 HikariCP·MyBatis·Handler 등록 상태를 확인한다. |
| 7 | 애플리케이션 기동 완료와 거래 정상 처리를 구분한다. |
| 8 | 표준 요청 전문으로 대표 ServiceId를 호출한다. |
| 9 | OnlineTransactionController부터 Mapper까지 Breakpoint를 설정한다. |
| 10 | Step Into·Step Over·Step Return의 차이를 설명한다. |
| 11 | Call Stack과 Local Variable을 이용해 실행 경로를 확인한다. |
| 12 | GUID·TraceId·ServiceId로 로그를 연결한다. |
| 13 | 비동기 Timeout 실행영역의 Thread 전환을 확인한다. |
| 14 | 업무 오류·시스템 오류·Timeout을 구분한다. |
| 15 | Port·Profile·DB·Bean·Mapper 오류를 순서대로 진단한다. |
| 16 | 로컬에서 성공한 거래가 외부 Tomcat에서도 동작하는지 검증한다. |

# 한눈에 보는 로컬 실행과 디버깅 흐름

┌─────────────────────────────────────────────────────────────┐
│ 1. 개발환경 │
│ JDK 21 · Gradle · Git Branch · Commit │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 2. Gradle bootRun │
│ :sv-service:bootRun │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 3. Spring Profile │
│ local 설정 로딩 · Port · H2 · HikariCP │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 4. Spring Context │
│ TCF · STF · ETF · Handler · Mapper Bean 생성 │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 5. 표준 거래 호출 │
│ POST /sv/online │
│ ServiceId = SV.Customer.selectSummary │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 6. Breakpoint │
│ Controller → TCF → STF → Dispatcher → Handler │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 7. 업무 처리 │
│ Facade → Service → Rule → DAO → Mapper → DB │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 8. ETF·표준 응답 │
│ 성공 · 업무 오류 · 시스템 오류 · Timeout │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 9. 완료 증적 │
│ 응답 · 로그 · SQL · DB 결과 · GUID · TraceId │
└─────────────────────────────────────────────────────────────┘

# 로컬 실행의 완료 단계

| 단계 | 확인 대상 | 완료 증적 |
| --- | --- | --- |
| 1 | Build | BUILD SUCCESSFUL |
| 2 | 기동 | Started ...Application |
| 3 | 설정 | Profile·Port·Datasource |
| 4 | 등록 | Handler·Mapper·Spring Bean |
| 5 | 연결 | DB·외부 연계 |
| 6 | 호출 | HTTP 응답 |
| 7 | 업무 | 예상 Body·오류코드 |
| 8 | 데이터 | 조회·변경 결과 |
| 9 | 운영 | GUID·TraceId·ServiceId 로그 |
| 10 | 재현 | 재기동 후 같은 결과 |

# 주요 실행 대상

| 구분 | 대표 위치·대상 |
| --- | --- |
| Main Class | Nsight{업무코드}ServiceApplication |
| Gradle Task | :{module}:bootRun |
| 기본 설정 | application.yml |
| 로컬 설정 | application-local.yml |
| 개발 설정 | application-dev.yml |
| 운영 설정 | application-prod.yml |
| 공통 Controller | OnlineTransactionController |
| 실행 엔진 | TCF, STF, ETF |
| 거래 라우터 | TransactionDispatcher |
| 업무 진입 | 도메인 TransactionHandler |
| 데이터 접근 | DAO·Mapper |
| 로컬 DB | H2 또는 프로젝트 지정 DB |
| 거래로그 | 로컬 H2·파일 또는 지정 저장소 |

# 8.1 환경별 설정과 Profile

## 8.1.1 Profile이란 무엇인가

Profile은 같은 애플리케이션을 환경별로 다르게 실행하기 위한 설정 구분이다.

같은 Java 코드
├─ local 설정
├─ dev 설정
└─ prod 설정

환경에 따라 달라지는 대표 항목은 다음과 같다.

| 설정 | local | dev | prod |
| --- | --- | --- | --- |
| Port | 업무별 개발 Port | 통합 Tomcat Port | 운영 Port |
| DB | H2·개발 DB | 개발 Oracle 등 | 운영 Oracle |
| Schema 초기화 | 자동 가능 | 제한 | 원칙적 금지 |
| 인증 | 개발 편의정책 | 통합 인증 | 운영 인증 |
| 로그 | 상세 Debug 가능 | 검증 중심 | 표준 운영 로그 |
| 외부 연계 | Mock·로컬 URL | 개발 연계 | 운영 URL |
| 거래통제 | 완화 가능 | 운영 유사 | 필수 |
| Secret | 가짜·로컬 값 | 보호변수 | Vault·KMS 등 |
| 데이터 | 가상 데이터 | 개발 데이터 | 운영 데이터 |

## 8.1.2 NSIGHT Profile 기준

대표 기준은 다음과 같다.

local
\= 개발자 PC의 bootRun

dev
\= 개발 통합환경·공용 Tomcat

prod
\= 운영환경

프로젝트 자료에서도 로컬 실행은 local, 통합 Tomcat은 dev, 운영 Tomcat은 prod로 구분하도록 안내한다.

## 8.1.3 기본 설정 파일

src/main/resources
├─ application.yml
├─ application-local.yml
├─ application-dev.yml
└─ application-prod.yml

### application.yml

모든 환경에 공통으로 적용할 기본값을 둔다.

예:

server:
encoding:
charset: UTF-8

spring:
profiles:
default: local

mybatis:
mapper-locations:
\- classpath:/mapper/\*\*/\*.xml
configuration:
map-underscore-to-camel-case: true

### application-local.yml

개발자 PC에서만 사용하는 값이다.

server:
port: 8086

spring:
application:
name: nsight-sv-service
datasource:
url: jdbc:h2:mem:nsight\_sv

현재 자료의 SV 고객요약 실습은 로컬 주소를 POST http://localhost:8086/sv/online으로 설명한다.

## 8.1.4 설정 병합

Spring Boot는 공통 설정과 활성 Profile 설정을 병합한다.

application.yml
+
application-local.yml
↓
최종 Environment

예:

\# application.yml
server:
servlet:
encoding:
charset: UTF-8

spring:
datasource:
username: sa

\# application-local.yml
server:
port: 8086

spring:
datasource:
url: jdbc:h2:mem:nsight\_sv

최종 적용:

Port = 8086
Encoding = UTF-8
DB URL = jdbc:h2:mem:nsight\_sv
DB User = sa

## 8.1.5 설정 우선순위

대표적인 우선순위는 다음과 같이 이해할 수 있다.

명령행 인수
↓
JVM 시스템 속성
↓
OS 환경변수
↓
application-{profile}.yml
↓
application.yml
↓
Spring 기본값

예:

gradle :sv-service:bootRun \\
\--args='--server.port=18086'

application-local.yml에 8086이 있어도 명령행에서 지정한 18086이 우선할 수 있다.

## 8.1.6 활성 Profile 지정

### 명령행 인수

gradle :sv-service:bootRun \\
\--args='--spring.profiles.active=local'

### JVM 시스템 속성

gradle :sv-service:bootRun \\
\-Dspring.profiles.active=local

Gradle과 Shell의 인수 전달 방식은 실행환경에 따라 달라질 수 있으므로 실제 실행 로그에서 확인한다.

### 환경변수

Windows PowerShell:

$env:SPRING\_PROFILES\_ACTIVE = "local"
gradle :sv-service:bootRun

Linux:

export SPRING\_PROFILES\_ACTIVE=local
gradle :sv-service:bootRun

## 8.1.7 활성 Profile 확인

기동 로그에서 다음 문구를 찾는다.

The following 1 profile is active: "local"

또는:

No active profile set, falling back to default profile: "local"

두 문장은 의미가 다르다.

| 로그 | 의미 |
| --- | --- |
| Profile active | 명시적으로 활성화됨 |
| Default profile | 명시값이 없어 기본값 사용 |
| No profile | 공통 설정만 적용될 수 있음 |
| 복수 Profile | 여러 설정이 병합됨 |

운영에서는 Profile을 암묵적인 기본값에 의존하지 않고 명시하는 것이 안전하다.

## 8.1.8 Profile 오인 사례

개발자는 local로 실행했다고 생각함
실제 Profile은 dev

결과:
개발 공용 DB 연결
개발 공용 거래로그 사용
공용 외부 서비스 호출
예상하지 못한 데이터 변경

반대 사례:

통합환경을 dev로 기동해야 함
실제 Profile은 local

결과:
메모리 H2 사용
Schema 자동생성
거래로그가 로컬 파일에 기록
다른 시스템과 데이터 불일치

## 8.1.9 환경별 설정 비교표

실행 전에 표로 기록한다.

| 설정 항목 | local | dev | prod |
| --- | --- | --- | --- |
| spring.application.name |  |  |  |
| server.port |  |  |  |
| Context Path |  |  |  |
| DB URL |  |  |  |
| DB User |  |  |  |
| Hikari Pool Name |  |  |  |
| SQL 초기화 |  |  |  |
| Mapper 위치 |  |  |  |
| 거래로그 경로 |  |  |  |
| 인증 검증 |  |  |  |
| 외부 연계 URL |  |  |  |
| Timeout |  |  |  |
| 로그 Level |  |  |  |

비밀번호와 Token 원문은 표에 기록하지 않는다.

## 8.1.10 Main Class 확인

업무 모듈의 기동 클래스를 찾는다.

검색:

@SpringBootApplication
public static void main
SpringBootServletInitializer

개념 예:

@SpringBootApplication(scanBasePackages = "com.nh.nsight")
@MapperScan("com.nh.nsight.marketing.sv.persistence.mapper")
public class NsightSvServiceApplication {

public static void main(String\[\] args) {
SpringApplication.run(
NsightSvServiceApplication.class,
args
);
}
}

확인할 것:

| 항목 | 판단 |
| --- | --- |
| scanBasePackages | TCF와 업무 Bean이 포함되는가 |
| @MapperScan | 업무 Mapper 범위가 맞는가 |
| Main Class | 해당 업무 모듈인가 |
| Servlet Initializer | 외부 WAR 배포 가능한가 |
| 중복 Main | bootRun 대상이 모호하지 않은가 |

## 8.1.11 Run Configuration

IDE Run Configuration의 핵심 항목은 다음과 같다.

| 항목 | SV 예 |
| --- | --- |
| 유형 | Gradle 또는 Spring Boot |
| Task | :sv-service:bootRun |
| Working Directory | Git 저장소 Root |
| JDK | 21 |
| Profile | local |
| Port | 8086 |
| VM Option | \-Dfile.encoding=UTF-8 |
| Program Argument | Profile·Port Override |
| Environment | DB·연계 설정 |
| Debug | 활성화 |

업무 모듈 실행 설정의 기준 항목으로 Gradle Task, Project Root, JDK 21, local Profile, UTF-8와 업무별 Port를 확인해야 한다.

## 8.1.12 Gradle 실행

gradle :sv-service:bootRun

실행 스크립트가 있다면 다음과 같이 호출할 수 있다.

Windows:

sv-service\\scripts\\run-local.bat

Linux:

./sv-service/scripts/run-local.sh

실행 스크립트는 다음을 확인해야 한다.

프로젝트 Root 계산
JDK 21 설정
Gradle 경로 확인
업무 모듈 지정
bootRun 호출
오류코드 반환

개인 PC의 절대 Gradle 경로가 Script에 고정된 경우 다른 개발자의 환경에서 실패할 수 있으므로 Override 환경변수, Wrapper 또는 PATH 기반 탐색으로 개선하는 것이 바람직하다.

## 8.1.13 로컬 DB

로컬 개발에서는 H2를 사용할 수 있다.

장점
\- 별도 DB 설치 없이 실행 가능
\- 테스트 데이터 초기화 쉬움
\- 개발자별 독립 데이터

주의
\- Oracle과 문법·자료형 차이
\- 실행계획·Lock 특성 차이
\- 대용량 성능 검증 불가
\- 운영 SQL의 완전한 대체가 아님

H2 Oracle Mode를 사용해도 Oracle과 동일하지는 않다.

H2 성공
≠ Oracle 성공

## 8.1.14 Schema와 초기 데이터

로컬 설정에서 다음 기능을 사용할 수 있다.

spring:
sql:
init:
mode: always
schema-locations: classpath:schema.sql
data-locations: classpath:data.sql

실행 순서:

Datasource 생성
↓
schema.sql
↓
data.sql
↓
MyBatis 거래 실행

### 주의

local에서 mode=always
→ 사용 가능

prod에서 mode=always
→ 운영 테이블 변경 위험

운영 Schema 변경은 승인된 Migration·DDL 절차를 사용한다.

## 8.1.15 HikariCP 확인

로컬에서도 Connection Pool을 사용한다.

확인 항목:

Pool Name
Minimum Idle
Maximum Pool Size
Connection Timeout
Validation Timeout
Auto Commit
JDBC URL

Pool 이름은 업무별로 구분해야 로그에서 어느 업무의 DB Pool인지 식별할 수 있다.

예:

nsight-sv-hikari-local
nsight-ic-hikari-local

## 8.1.16 거래로그 경로

로컬 거래로그를 파일 H2 등에 저장한다면 물리 경로를 명확히 해야 한다.

프로젝트 A의 bootRun
프로젝트 B의 bootRun
통합 Tomcat
Batch
OM

이들이 같은 H2 파일을 동시에 열면 Lock과 데이터 혼선이 발생할 수 있다.

확인:

nsight.txlog.path
NSIGHT\_TXLOG\_PATH
현재 Working Directory
물리 H2 파일

## 8.1.17 외부 업무 연계

SV가 IC를 호출하는 로컬 구성 예:

SV local : localhost:8086
↓
IC local : localhost:8082

다음 항목을 확인한다.

| 항목 | 확인 |
| --- | --- |
| 대상 애플리케이션 기동 | IC가 실행 중인가 |
| Base URL | Port가 정확한가 |
| Context Path | /ic 여부 |
| Endpoint | /online 여부 |
| Connect Timeout | 연결 제한시간 |
| Read Timeout | 응답 제한시간 |
| ServiceId | IC Handler 등록 여부 |
| 인증 | 로컬 인증정책 |

## 8.1.18 Secret 관리

다음 값을 application-local.yml에 실제 값으로 Commit하지 않는다.

운영 DB 비밀번호
개발 공용 DB 비밀번호
JWT Private Key
SSO Client Secret
API Key
개인 Access Token

저장소에는 Placeholder 또는 가짜 값을 둔다.

spring:
datasource:
password: ${NSIGHT\_SV\_DB\_PASSWORD:}

## 8.1.19 설정값 확인 방법

### 기동 로그

가장 먼저 확인한다.

### Debugger

Spring Environment를 주입받아 확인할 수 있다.

environment.getActiveProfiles();
environment.getProperty("server.port");

### Actuator

관리 Endpoint를 사용할 수 있지만 운영 노출 범위를 통제해야 한다.

health
info
metrics

env, configprops는 민감한 설정이 노출될 수 있으므로 외부 공개를 금지하거나 강한 접근통제를 적용한다.

## 8.1.20 Profile 정상 판정

Branch·Commit 확인
\+ local Profile 확인
\+ 업무 Port 확인
\+ 예상 DB URL 확인
\+ 예상 Hikari Pool 확인
\+ 예상 외부 URL 확인
\+ Secret 미노출 확인
\= Profile 정상

# 8.2 애플리케이션 기동 로그 읽기

## 8.2.1 기동 로그가 중요한 이유

애플리케이션이 기동되는 동안 Spring은 다음 작업을 수행한다.

Configuration 읽기
↓
Profile 선택
↓
AutoConfiguration
↓
Bean 정의
↓
Component Scan
↓
Mapper Scan
↓
Datasource 생성
↓
HikariCP 초기화
↓
Handler Registry 생성
↓
Tomcat Port Binding
↓
Application Ready

기동 로그를 읽으면 어느 단계까지 성공했는지 알 수 있다.

## 8.2.2 기동 단계표

| 단계 | 주요 확인 |
| --- | --- |
| 1 | Java·Spring Boot Version |
| 2 | Application Name |
| 3 | Active Profile |
| 4 | Repository·Configuration |
| 5 | Datasource·HikariCP |
| 6 | SQL Schema 초기화 |
| 7 | MyBatis Mapper |
| 8 | Security Filter |
| 9 | TCF Bean |
| 10 | Handler 등록 |
| 11 | Tomcat Port |
| 12 | Started 로그 |

## 8.2.3 시작 배너

대표 로그:

Starting NsightSvServiceApplication

확인:

Main Class
Process ID
Working Directory
Java Version
Host

잘못된 Main Class를 실행하면 다른 업무 Port와 설정이 적용될 수 있다.

## 8.2.4 Profile 로그

The following profile is active: local

다음과 같다면 주의한다.

No active profile set

기본 Profile이 설정되어 있더라도 의도한 환경인지 확인한다.

## 8.2.5 Datasource·HikariCP 로그

대표 흐름:

HikariPool - Starting
HikariPool - Added connection
HikariPool - Start completed

확인:

| 항목 | 판단 |
| --- | --- |
| Pool 이름 | 업무가 맞는가 |
| URL | H2·Oracle 대상이 맞는가 |
| Driver | 환경과 맞는가 |
| Connection | 최소 1개 이상 생성되는가 |
| Timeout | 과도하게 길지 않은가 |
| Schema | 예상 Schema인가 |

## 8.2.6 Schema 초기화 로그

오류 예:

Failed to execute SQL script statement
Table already exists
Table not found
Syntax error

진단:

어느 SQL 파일인가?
몇 번째 Statement인가?
H2와 Oracle 문법 차이인가?
실행 순서가 맞는가?
대소문자 설정이 맞는가?

## 8.2.7 MyBatis 초기화

확인할 것:

@MapperScan 범위
Mapper Interface
Mapper XML 경로
Namespace
SQL ID
Type Alias
ResultMap

기동은 성공하지만 거래 시점에 Mapper 오류가 발생할 수도 있다.

Spring Bean 등록 성공
≠ 모든 SQL ID 정상

따라서 대표 거래를 반드시 호출한다.

## 8.2.8 Handler Registry

TransactionDispatcher는 기동 시 TransactionHandler를 수집하여 ServiceId와 연결한다.

TransactionHandler 목록
↓
handler.serviceIds()
↓
serviceId → Handler Map

확인 로그 예:

Registered NSIGHT handler
serviceId=SV.Customer.selectSummary
handler=SvCustomerHandler

TCF Dispatcher는 ServiceId를 기준으로 Handler를 선택하며, 중복 ServiceId가 있으면 기동 단계에서 실패하도록 설계하는 것이 적절하다.

## 8.2.9 Handler 등록 수 확인

기동 시 다음을 확인한다.

예상 ServiceId 수
실제 등록 ServiceId 수
누락 ServiceId
중복 ServiceId
Handler가 선언하지 않은 ServiceId

예:

| 구분 | 예상 | 실제 | 판정 |
| --- | --- | --- | --- |
| SV Customer | 5 | 5 | 정상 |
| SV Sample | 2 | 1 | 누락 |
| SV Product | 3 | 4 | 검토 |

## 8.2.10 중복 ServiceId

오류 예:

Duplicate serviceId detected:
SV.Customer.selectSummary

의미:

두 개 이상의 Handler가
같은 거래를 소유한다고 선언

해결:

중복 Handler 검색
ServiceId 소유권 확인
폐기 코드 제거
설계서·OM Catalog 비교
테스트 수정

ServiceId 이름을 임의로 바꿔 기동만 성공시키지 않는다.

## 8.2.11 Tomcat Port 로그

대표:

Tomcat started on port 8086

확인:

Port
Protocol
Context Path
Application Name

로컬 요청 URL은 이 값으로 구성한다.

http://localhost:{port}/{businessCode}/online

SV 예:

http://localhost:8086/sv/online

## 8.2.12 Context Path 해석

다음 설정을 구분한다.

server:
servlet:
context-path: /

이 상태에서 Controller가 /{businessCode}/online을 제공하면 URL은 다음과 같다.

/sv/online

외부 Tomcat에서 sv.war가 /sv Context로 배포되고 Controller가 /online을 제공하는 구조와 URL 결과는 같을 수 있지만 내부 구성은 다르다.

bootRun
Root Context + /sv/online Mapping

외부 Tomcat
/sv Context + /online Mapping

프로젝트의 실제 Mapping과 WAR Context를 확인한다.

## 8.2.13 최종 Started 로그

Started NsightSvServiceApplication in 4.321 seconds

의미:

Spring Context 생성 완료
Tomcat Port Binding 완료
Application Ready Event 발생 가능

그러나 다음은 별도 확인이다.

대표 Handler 실행
SQL 실행
외부 시스템 호출
거래로그 저장

## 8.2.14 Health Check

예:

GET /actuator/health

응답:

{
"status": "UP"
}

### Health 단계

| 단계 | 의미 |
| --- | --- |
| Liveness | 프로세스가 살아 있는가 |
| Readiness | 요청을 받을 준비가 되었는가 |
| Dependency | DB·외부 시스템이 사용 가능한가 |
| Business Smoke | 대표 거래가 정상인가 |

UP만으로 모든 업무기능이 정상이라고 판단하지 않는다.

## 8.2.15 기동 로그에서 Warning 읽기

Warning은 기동 성공을 방해하지 않을 수 있지만 운영 장애의 사전 신호일 수 있다.

대표 Warning:

Deprecated Configuration
Open Session in View
Duplicate Bean 후보
Default Password
Mapper 미등록
Connection Leak 경고
Thread 미종료
Unknown Property

각 Warning을 다음으로 분류한다.

| 등급 | 처리 |
| --- | --- |
| 즉시 오류 가능 | 기동 전 조치 |
| 운영 위험 | 배포 전 조치 |
| 기술부채 | 담당자·완료일 지정 |
| 의도된 예외 | 근거 기록 |
| 무관 | 검토 후 제외 근거 |

## 8.2.16 현재 구현의 Console 출력

개발단계에서는 TCF 처리구간을 쉽게 확인하기 위해 다음과 같은 Console 진단 출력이 존재할 수 있다.

OnlineTransactionController.handle START
TCF.process START
STF START
DISPATCHER START
ETF START
TCF.process cleanup

이는 초보자가 실행 순서를 이해하는 데 유용하다.

그러나 운영 코드에서는 다음 이유로 System.out.println에 의존하지 않는다.

| 문제 | 영향 |
| --- | --- |
| Log Level 없음 | 동적 제어 불가 |
| 구조화 불가 | 검색·집계 어려움 |
| 비동기 처리 미흡 | 성능 저하 가능 |
| 민감정보 | Payload 노출 가능 |
| MDC 연계 부족 | GUID 추적 어려움 |
| 출력량 | Disk·Console 부하 |

### 운영 권장

SLF4J 기반 구조화 로그
\+ GUID
\+ TraceId
\+ ServiceId
\+ 단계명
\+ 처리시간
\+ 오류코드
\+ 민감정보 마스킹

## 8.2.17 요청·응답 원문 출력 주의

금지:

log.info("request={}", request);
log.info("authorization={}", authorizationHeader);

요청 Body에는 고객번호, 계좌번호와 개인정보가 포함될 수 있다.

권장:

serviceId
guid
traceId
resultCode
elapsedMs
bodySize
maskedCustomerNo

## 8.2.18 기동 완료 증적

| 항목 | 증적 |
| --- | --- |
| Branch | Git 상태 |
| Commit | Commit ID |
| JDK | java -version |
| Gradle | gradle --version |
| Profile | 기동 로그 |
| Application | Main Class 로그 |
| Port | Tomcat 로그 |
| DB Pool | Hikari 로그 |
| Handler | 등록 로그 |
| Health | Health 응답 |
| 거래 | 대표 ServiceId 응답 |

# 8.3 Breakpoint와 호출 스택

## 8.3.1 Breakpoint란 무엇인가

Breakpoint는 프로그램 실행을 지정 위치에서 일시 중지시키는 기능이다.

요청 실행
↓
Breakpoint 도달
↓
Thread 일시 정지
↓
변수·Call Stack 확인
↓
한 단계씩 실행

Breakpoint를 많이 설정하는 것이 좋은 디버깅은 아니다.

검증하려는 가설
→ 필요한 위치만 Breakpoint

## 8.3.2 첫 디버깅의 목표

첫 번째 디버깅에서는 다음을 확인한다.

요청 Header가 올바른가?

Profile과 Port가 예상과 같은가?

Controller가 호출되는가?

STF가 Header를 어떻게 보완하는가?

Dispatcher가 어느 Handler를 선택하는가?

Handler가 Body를 어떻게 DTO로 바꾸는가?

Facade에서 트랜잭션이 시작되는가?

Service가 어떤 Rule과 DAO를 호출하는가?

Mapper에 어떤 Parameter가 전달되는가?

ETF가 응답을 어떻게 생성하는가?

## 8.3.3 대표 Breakpoint 위치

### 1단계: HTTP 진입

OnlineTransactionController.handle()

확인:

businessCode
request.header
request.body
clientIp

### 2단계: TCF 시작

TCF.process()

확인:

clientHeader 복사본
StandardRequest
context 초기값

### 3단계: STF 전처리

STF.preProcess()

확인:

Header 검증
GUID
TraceId
TransactionContext
인증 문맥
거래통제
Timeout
멱등성 Key
거래로그 시작

### 4단계: Timeout 실행

OnlineTransactionTimeoutExecutor.execute()

확인:

Timeout 값
업무 실행 Thread
예외 전달 방식
취소 처리

### 5단계: Dispatcher

TransactionDispatcher.dispatch()

확인:

serviceId
handlerMap
선택 Handler

### 6단계: Handler

TransactionHandler.handle()
업무 Handler.doHandle()

확인:

ServiceId 분기
Body 변환
요청 DTO
Facade 호출

### 7단계: Facade

SvCustomerFacade.selectSummary()

확인:

트랜잭션 Proxy
readOnly
timeout
유스케이스 호출 순서

### 8단계: Service

SvCustomerService.selectSummary()

확인:

업무 흐름
Rule 호출
DAO 호출
외부 업무 연계
결과 조립

### 9단계: Rule

SvCustomerInquiryRule.validate()

확인:

필수값
형식
업무조건
권한
상태

### 10단계: DAO·Mapper

SvCustomerDao.selectSummary()
SvCustomerMapper.selectSummary()

확인:

Query DTO
Parameter
SQL ID
Result DTO
결과 건수

### 11단계: ETF

ETF.success()
ETF.businessFail()
ETF.systemError()

확인:

응답 Header
Result Code
Error Code
Body
거래로그 종료
감사로그
처리시간

## 8.3.4 권장 Breakpoint 순서

Controller
↓
TCF
↓
STF 종료 직전
↓
Dispatcher
↓
업무 Handler
↓
Facade
↓
Service
↓
DAO
↓
ETF

첫 실행부터 모든 메서드에 Breakpoint를 두면 흐름을 놓치기 쉽다.

## 8.3.5 표준 요청 예

{
"header": {
"systemId": "NSIGHT-MP",
"businessCode": "SV",
"serviceId": "SV.Customer.selectSummary",
"transactionCode": "SV-INQ-0001",
"processingType": "INQUIRY",
"channelId": "WEBTOP",
"userId": "dev01",
"branchId": "001234"
},
"body": {
"customerNo": "CUST0000001"
}
}

NSIGHT 초보자 실습에서도 SV.Customer.selectSummary, SV-INQ-0001과 POST http://localhost:8086/sv/online을 대표 거래로 사용한다.

## 8.3.6 curl 호출 예

curl -X POST "http://localhost:8086/sv/online" \\
\-H "Content-Type: application/json" \\
\-d '{
"header": {
"systemId": "NSIGHT-MP",
"businessCode": "SV",
"serviceId": "SV.Customer.selectSummary",
"transactionCode": "SV-INQ-0001",
"processingType": "INQUIRY",
"channelId": "WEBTOP",
"userId": "dev01",
"branchId": "001234"
},
"body": {
"customerNo": "CUST0000001"
}
}'

Windows CMD·PowerShell에서는 Quote 문법이 다를 수 있으므로 Postman이나 IDE HTTP Client를 사용할 수 있다.

## 8.3.7 Step 명령

| 기능 | 의미 | 사용 상황 |
| --- | --- | --- |
| Step Over | 현재 줄 실행 후 다음 줄 | 내부 구현 불필요 |
| Step Into | 호출 메서드 내부로 이동 | 실행경로 확인 |
| Force Step Into | Library·Proxy 내부 진입 | 고급 분석 |
| Step Return | 현재 메서드 종료까지 실행 | 빠르게 상위로 복귀 |
| Resume | 다음 Breakpoint까지 실행 | 구간 이동 |
| Run to Cursor | 커서 위치까지 실행 | 중간 위치 이동 |

## 8.3.8 Call Stack

Call Stack은 현재 메서드가 어떤 경로로 호출되었는지 보여준다.

예:

SvCustomerMapper.selectSummary
SvCustomerDao.selectSummary
SvCustomerService.selectSummary
SvCustomerFacade.selectSummary
SvCustomerHandler.doHandle
TransactionHandler.handle
TransactionDispatcher.dispatch
OnlineTransactionTimeoutExecutor.execute
TCF.process
OnlineTransactionController.handle

이 Stack을 기록하면 추측이 아니라 실제 실행경로를 증명할 수 있다.

## 8.3.9 Proxy가 포함된 Call Stack

Spring은 다음 기능을 Proxy로 처리할 수 있다.

@Transactional
@Cacheable
@Async
Security
AOP Logging

따라서 Stack에 다음과 같은 클래스가 나타날 수 있다.

CglibAopProxy
ReflectiveMethodInvocation
TransactionInterceptor

이들은 오류가 아니라 Spring AOP 처리경로일 수 있다.

## 8.3.10 Facade와 Transaction Proxy

@Transactional은 일반적으로 Spring Proxy를 통해 동작한다.

확인:

Facade Bean이 Spring Bean인가?
외부 Bean에서 호출되었는가?
메서드가 public인가?
실제 Transaction이 활성화되었는가?

### Self Invocation 문제

public void outer() {
inner();
}

@Transactional
public void inner() {
}

같은 객체 내부에서 직접 호출하면 Proxy를 거치지 않아 기대한 Transaction이 적용되지 않을 수 있다.

## 8.3.11 변수 확인

각 단계에서 확인할 핵심 변수:

| 단계 | 변수 |
| --- | --- |
| Controller | Header·Body·Client IP |
| STF | GUID·TraceId·Timeout |
| Dispatcher | serviceId, handler |
| Handler | Request DTO |
| Facade | Transaction 상태 |
| Service | 업무 중간값 |
| Rule | 검증 조건 |
| DAO | Query DTO |
| Mapper | SQL Parameter |
| ETF | Result·Error Code |

## 8.3.12 Watch

반복 확인할 표현을 Watch에 등록한다.

예:

request.getHeader().getServiceId()
context.getHeader().getGuid()
context.getHeader().getTraceId()
request.getBody()
handler.getClass().getName()

민감정보를 화면 캡처나 공유 문서에 남기지 않는다.

## 8.3.13 Evaluate Expression

실행 중 값을 계산할 수 있다.

예:

request.getHeader().getServiceId()

주의:

상태를 변경하는 메서드 실행 금지
DB 변경 메서드 호출 금지
외부 시스템 호출 금지

Evaluate는 조회 용도로 사용한다.

## 8.3.14 조건부 Breakpoint

특정 ServiceId만 중지:

request.getHeader().getServiceId()
.equals("SV.Customer.selectSummary")

특정 고객번호:

"CUST0000001".equals(
request.getBody().get("customerNo")
)

장점:

다른 거래 Thread는 통과
원하는 거래만 정지

## 8.3.15 Exception Breakpoint

다음 예외에 Breakpoint를 설정할 수 있다.

BusinessException
SQLException
DataAccessException
TimeoutException
NullPointerException

### Caught와 Uncaught

| 구분 | 의미 |
| --- | --- |
| Caught | 코드가 잡아 처리하는 예외 |
| Uncaught | 처리되지 않고 전파되는 예외 |

TCF는 업무 오류와 시스템 오류를 잡아 ETF로 변환하므로 Caught Exception 설정이 유용할 수 있다.

## 8.3.16 Method Breakpoint 주의

Method Breakpoint는 클래스 로딩과 실행을 느리게 할 수 있다.

권장:

일반 Line Breakpoint 우선
Method Breakpoint는 대상이 명확할 때만

## 8.3.17 Thread 확인

Timeout Executor가 별도 Thread에서 업무를 실행하면 Controller Thread와 Handler Thread가 다를 수 있다.

http-nio-8086-exec-1
↓
timeout-executor-1

확인:

Thread 이름
TransactionContext 전달
MDC 전달
AuthenticationContext 전달
TimeoutContext 전달

ThreadLocal 문맥을 새 Thread에 전달하지 못하면 다음 문제가 발생한다.

GUID 누락
사용자 정보 누락
Timeout 정보 누락
거래로그 연결 단절

## 8.3.18 Breakpoint와 Timeout

Breakpoint에서 장시간 멈추면 실제 Timeout이 발생할 수 있다.

거래 Timeout 5초
Breakpoint 정지 20초
→ TIMEOUT

해결 방법:

디버깅 전용 Timeout 조정
Timeout Breakpoint 위치 변경
업무 실행 Thread만 Suspend
단계별로 짧게 확인

디버깅 편의를 위해 운영 Timeout 정책을 코드에서 삭제하지 않는다.

## 8.3.19 Suspend 정책

Breakpoint 설정:

| 방식 | 영향 |
| --- | --- |
| Suspend All | JVM의 모든 Thread 정지 |
| Suspend Thread | 해당 Thread만 정지 |

다중 요청·Scheduler·H2 File Lock이 있는 환경에서는 Suspend Thread가 더 안전할 수 있다.

다만 다른 Thread가 데이터 상태를 변경할 수 있으므로 상황에 따라 선택한다.

## 8.3.20 SQL 디버깅

Mapper Interface는 Proxy이므로 실제 SQL은 XML과 MyBatis Executor에서 실행된다.

확인 순서:

DAO 입력
↓
Mapper Method
↓
Mapper Namespace.SQL ID
↓
동적 SQL
↓
Binding Parameter
↓
JDBC 실행
↓
Result Mapping

SQL 원문에 개인정보 Parameter를 그대로 출력하지 않는다.

## 8.3.21 SQL 결과 확인

조회 SQL
→ Result 건수·주요 식별자

변경 SQL
→ 영향 행 수

등록 SQL
→ 생성 Key

삭제 SQL
→ 실제 삭제·논리 삭제 여부

변경 거래는 디버거 변수뿐 아니라 DB 상태를 확인해야 한다.

## 8.3.22 업무 오류 디버깅

흐름:

Rule·Service
↓
BusinessException
↓
TCF catch
↓
ETF.businessFail
↓
표준 업무 오류 응답

확인:

오류코드
사용자 메시지
GUID
거래로그 상태
Rollback

## 8.3.23 시스템 오류 디버깅

흐름:

SQLException·NullPointerException
↓
TCF catch Exception
↓
ETF.systemError
↓
안전한 사용자 메시지
↓
원인 예외는 서버 로그

사용자 응답에 Stack Trace·SQL·DB 계정정보를 노출하지 않는다.

## 8.3.24 Timeout 디버깅

확인:

어느 Timeout인가?
온라인 전체 Timeout인가?
DB Statement Timeout인가?
외부 연계 Read Timeout인가?
Breakpoint 때문에 발생했는가?
실제 업무 지연인가?

Timeout은 하나가 아니다.

| 계층 | 예 |
| --- | --- |
| HTTP Client | Connect·Read Timeout |
| TCF | Online Transaction Timeout |
| Facade | Transaction Timeout |
| MyBatis | Statement Timeout |
| Hikari | Connection Timeout |
| Tomcat | Connection·Async Timeout |

## 8.3.25 Remote Debug

외부 Tomcat이나 별도 JVM을 Debug할 때 JDWP를 사용할 수 있다.

개념 예:

\-agentlib:jdwp=transport=dt\_socket,
server=y,
suspend=n,
address=\*:5005

### 보안 원칙

운영망 외부 노출 금지
방화벽 제한
관리망에서만 접속
작업 후 즉시 비활성화
접속 이력 관리

Remote Debug Port는 인증되지 않은 코드 실행과 정보 노출 위험이 있으므로 운영 상시 개방을 금지한다.

## 8.3.26 Gradle Debug 실행

Gradle bootRun을 Debug 대기 상태로 실행하는 방식도 사용할 수 있다.

gradle :sv-service:bootRun --debug-jvm

일반적으로 지정된 Debug Port에서 IDE 연결을 기다린다.

실제 Port와 동작은 사용하는 Gradle·Spring Boot Plugin 설정을 확인한다.

## 8.3.27 디버깅 완료 증적

| 증적 | 내용 |
| --- | --- |
| 요청 | Header·Body |
| URL | Host·Port·Path |
| Profile | local |
| Call Stack | Controller부터 Mapper |
| ServiceId | 실제 값 |
| Handler | 선택 클래스 |
| SQL | Mapper ID |
| 응답 | Result·Error |
| 로그 | GUID·TraceId |
| DB | 결과 데이터 |
| 처리시간 | 시작·종료 |

# 8.4 자주 만나는 로컬 실행 오류

## 8.4.1 진단 기본 원칙

증상
≠ 원인

예:

화면에서 500 오류

가능한 원인:

Port 오류
Profile 오류
JWT 오류
ServiceId 미등록
DB 연결 오류
Mapper SQL 오류
외부 시스템 오류
Timeout

따라서 단계별로 범위를 좁힌다.

## 8.4.2 표준 진단 순서

1\. 프로세스가 실행 중인가?
2\. 예상 Port가 열렸는가?
3\. 예상 Profile인가?
4\. Health는 UP인가?
5\. HTTP 요청이 Controller에 도달하는가?
6\. STF를 통과하는가?
7\. ServiceId Handler가 등록되었는가?
8\. 업무 Service까지 도달하는가?
9\. SQL·외부 연계가 성공하는가?
10\. ETF 응답이 정상인가?

## 8.4.3 Port 사용 중

오류:

Port 8086 was already in use

Windows:

netstat -ano | findstr 8086
tasklist | findstr {PID}

Linux:

ss -lntp | grep 8086

대응:

기존 프로세스 식별
→ 정상 프로세스인지 확인
→ 불필요한 경우 종료
→ 표준 Port로 재기동

Port를 임의로 변경하면 UI·Gateway·업무 연계 URL도 함께 변경해야 한다.

## 8.4.4 잘못된 Profile

증상:

예상 Port가 아님
H2가 아닌 Oracle 접속
Schema 자동생성 안 됨
외부 개발 서버를 호출함

확인:

기동 로그
SPRING\_PROFILES\_ACTIVE
\-Dspring.profiles.active
IDE Run Configuration
Gradle bootRun 설정

## 8.4.5 Gradle을 찾지 못함

오류:

gradle not found

확인:

gradle --version

Windows:

where gradle

해결:

GRADLE\_HOME\_OVERRIDE
GRADLE\_HOME
PATH
프로젝트 Wrapper

실행 스크립트에 개인 절대경로가 고정되어 있다면 공통 실행환경 기준으로 개선한다.

## 8.4.6 JDK 불일치

오류:

Unsupported class file major version
release version 21 not supported

확인:

java -version
javac -version
gradle --version

Gradle Daemon 종료:

gradle --stop

## 8.4.7 DB 연결 실패

오류:

Connection refused
Invalid username/password
Unable to acquire JDBC Connection

확인:

Profile
JDBC URL
Driver
DB 프로세스
Network
계정
Schema
Hikari Timeout

## 8.4.8 H2 파일 잠금

오류 예:

Database may be already in use
File is locked

원인:

두 bootRun 프로세스가 같은 H2 파일 사용
통합 Tomcat과 로컬 실행이 같은 거래로그 경로 사용
이전 JVM 비정상 종료

대응:

사용 프로세스 확인
H2 경로 분리
정상 종료
AUTO\_SERVER 설정 검토
파일 강제삭제 전 데이터 용도 확인

## 8.4.9 Schema 초기화 실패

오류:

Table already exists
Column not found
Syntax error in SQL statement

확인:

schema.sql
data.sql
H2 Mode
초기화 순서
대소문자
운영 전용 문법

## 8.4.10 Handler 중복 등록

오류:

Duplicate serviceId detected

대응:

serviceIds() 검색
동일 ServiceId Handler 확인
이전 샘플·폐기 코드 확인
업무 소유권 확정

기동을 위해 임의의 ServiceId를 새로 만들지 않는다.

## 8.4.11 Handler 미등록

응답:

SERVICE\_NOT\_FOUND
등록되지 않은 serviceId

확인:

요청 Header 오타
대소문자
Handler Bean 등록
serviceIds() 반환값
Component Scan
현재 모듈
현재 Branch

## 8.4.12 Header 오류

응답:

INVALID\_HEADER

확인:

businessCode
serviceId
transactionCode
processingType
channelId
userId
branchId

STF에서 실패하면 업무 Handler에 Breakpoint가 걸리지 않는 것이 정상이다.

## 8.4.13 Bean을 찾을 수 없음

오류:

NoSuchBeanDefinitionException
UnsatisfiedDependencyException

확인:

@Component·@Service
Package 위치
scanBasePackages
생성자 의존성
Profile 조건
Configuration 조건

## 8.4.14 중복 Bean

오류:

BeanDefinitionOverrideException
expected single matching bean but found 2

원인:

동일 Configuration 중복
Legacy·신규 구현 동시 등록
업무 WAR Package Scan 과다
테스트 Mock 중복

## 8.4.15 Mapper Bean 미등록

오류:

No qualifying bean of type '...Mapper'

확인:

@Mapper
@MapperScan
패키지 위치
업무 Main Class
모듈 Dependency

## 8.4.16 SQL ID 미등록

오류:

Invalid bound statement
Mapped Statements collection does not contain value

확인:

Mapper Interface FQCN
XML Namespace
Method 이름
SQL ID
XML Resource 포함

## 8.4.17 ResultMap 오류

증상:

필드가 null
Type 변환 실패
Setter 오류

확인:

컬럼 Alias
camelCase 설정
DTO 필드
자료형
Null 처리
ResultMap

## 8.4.18 CORS 오류

브라우저 Console:

Blocked by CORS policy

확인:

UI Origin
업무 API Origin
Preflight OPTIONS
Allowed Headers
Authorization Header
Cookie 정책

Postman 호출은 성공하지만 브라우저에서 실패한다면 CORS 가능성이 높다.

보안상 allowedOrigins=\*를 무조건 적용하지 않는다.

## 8.4.19 JWT 401

증상:

Unauthorized
Invalid Token
Expired Token

확인:

Authorization: Bearer
Token 만료
issuer
audience
Public Key
JWT Filter 활성 여부
로컬 인증 우회정책

업무 Header의 userId만 넣었다고 인증이 완료되는 것은 아니다.

## 8.4.20 권한 403

인증 성공
권한 실패

확인:

사용자 권한
업무코드
기능권한
지점권한
데이터 범위
STF·Rule 검증

## 8.4.21 외부 연계 Connection Refused

오류:

Connection refused: localhost:8082

확인:

대상 IC 기동 여부
Port
Context Path
Endpoint
방화벽
Connect Timeout

로컬 전체 거래를 시험하려면 하위 업무 서비스도 함께 기동하거나 Mock을 제공해야 한다.

## 8.4.22 외부 연계 Timeout

Connect Timeout
Read Timeout
TCF Online Timeout

Timeout 계층을 구분한다.

연결도 못 함
→ Connect Timeout

연결했지만 응답 없음
→ Read Timeout

전체 거래 제한 초과
→ TCF Timeout

## 8.4.23 Breakpoint 때문에 Timeout

판정:

Breakpoint 제거 후 정상
Debug에서만 Timeout
Thread가 중지된 시간이 제한시간 초과

대응:

로컬 디버깅 Timeout 조정
조건부 Breakpoint
Suspend Thread
관찰 구간 축소

## 8.4.24 Transaction이 적용되지 않음

증상:

오류가 발생했지만 일부 데이터 Commit

확인:

@Transactional 위치
Facade가 Spring Bean인가
Self Invocation인가
Checked Exception Rollback 정책
다중 Datasource인가
Auto Commit인가

## 8.4.25 Rollback이 되지 않음

업무 오류 발생
→ 일부 UPDATE 반영

확인:

BusinessException이 RuntimeException인가
rollbackFor 설정
예외를 catch 후 삼켰는가
트랜잭션 밖에서 변경했는가
외부 시스템 변경인가

외부 시스템의 변경은 로컬 DB Transaction으로 Rollback되지 않는다.

## 8.4.26 오류를 숨기는 Catch

금지:

try {
service.execute();
} catch (Exception e) {
return null;
}

문제:

ETF가 시스템 오류를 알 수 없음
Rollback 정책 불명확
로그 원인 소실
화면이 정상으로 오판

예외는 표준 오류체계로 전파한다.

## 8.4.27 NullPointerException

NPE가 발생한 줄만 수정하지 않는다.

질문:

왜 null이 들어왔는가?
입력 검증이 누락되었는가?
DB 결과 없음인가?
Mapper 매핑 실패인가?
외부 응답 누락인가?
업무상 null이 허용되는가?

## 8.4.28 인코딩 오류

증상:

한글 깨짐
YAML 파싱 실패
Mapper XML 오류
로그 메시지 깨짐

확인:

파일 UTF-8
BOM
JVM file.encoding
Console Encoding
HTTP Encoding
DB Character Set

## 8.4.29 변경이 반영되지 않음

확인 순서:

현재 Branch
현재 Commit
실행 중 PID
실행 모듈
Gradle Build 결과
IDE 자동 Build
기존 JVM
Build Cache

대응:

gradle --stop
gradle :sv-service:clean
gradle :sv-service:bootRun

## 8.4.30 다른 업무 애플리케이션이 실행됨

증상:

예상 Port와 다름
Application Name이 다름
Handler 목록이 다름

확인:

Run Configuration Main Class
Gradle Task
Working Directory
Module Classpath

## 8.4.31 TCF 우회 Endpoint

현재 소스나 샘플에는 TCF를 통하지 않고 Facade로 직접 들어가는 진단용 Endpoint가 존재할 수 있다.

직접 Controller
↓
Facade
↓
Service

이 방식은 다음을 우회할 수 있다.

STF
거래통제
Timeout 정책
멱등성
거래로그
ETF 표준 오류

TCF 우회 Endpoint는 구조 비교·진단 목적의 제한된 샘플로만 사용하고 운영 진입경로로 사용하지 않는다. TCF 우회 API는 통제 누락을 검증하는 별도 테스트 대상으로 관리해야 한다.

## 8.4.32 Health는 UP인데 거래 실패

/actuator/health = UP
POST /sv/online = FAIL

가능한 원인:

Handler 미등록
Mapper SQL 오류
업무 데이터 없음
권한 오류
외부 연계 오류
ServiceId 오타

Health와 Business Smoke Test를 분리한다.

## 8.4.33 직접 재기동부터 하지 않는다

장애가 발생하면 먼저 증거를 수집한다.

요청
응답
시간
GUID
TraceId
ServiceId
오류코드
Thread
Stack Trace
DB 상태

그 후 재기동한다.

무조건 재기동
→ 증상이 사라짐
→ 원인 증거 소실
→ 재발

# 로컬 오류 진단표

| 증상 | 1차 확인 | 주요 원인 |
| --- | --- | --- |
| 프로세스 미기동 | JDK·Gradle | 환경 |
| Port Binding 실패 | PID | 중복 프로세스 |
| DB Pool 실패 | URL·계정 | Profile·DB |
| Context 실패 | Bean | Scan·설정 |
| 404 | URL·Context | Mapping |
| 401 | JWT | 인증 |
| 403 | 권한 | 인가 |
| INVALID\_HEADER | Header | 전문 |
| SERVICE\_NOT\_FOUND | Handler Map | ServiceId |
| SQL ID 오류 | Mapper XML | Resource |
| 500 | Stack Trace | 시스템 오류 |
| Timeout | 처리 단계 | SQL·연계·Breakpoint |
| 데이터 일부 반영 | Transaction | Rollback |
| 로그 연결 불가 | GUID·MDC | 문맥 전달 |
| 로컬만 성공 | 환경 비교 | 숨은 의존성 |

# 디버깅 의사결정 트리

애플리케이션이 기동되는가?
├─ 아니오
│ └─ JDK·Gradle·Profile·Bean·DB 확인
│
└─ 예
↓
예상 Port가 열렸는가?
├─ 아니오
│ └─ Port·Context·PID 확인
│
└─ 예
↓
Health가 UP인가?
├─ 아니오
│ └─ DB·Dependency 확인
│
└─ 예
↓
Controller Breakpoint에 도달하는가?
├─ 아니오
│ └─ URL·Gateway·CORS·Security 확인
│
└─ 예
↓
STF를 통과하는가?
├─ 아니오
│ └─ Header·인증·권한·통제 확인
│
└─ 예
↓
Handler가 선택되는가?
├─ 아니오
│ └─ ServiceId·등록 상태 확인
│
└─ 예
↓
Service까지 도달하는가?
├─ 아니오
│ └─ Handler 분기·DTO 변환 확인
│
└─ 예
↓
SQL·외부 연계가 성공하는가?
├─ 아니오
│ └─ Mapper·DB·Client·Timeout 확인
│
└─ 예
↓
ETF 응답이 정상인가?
├─ 아니오
│ └─ 오류 변환·로그 종료 확인
│
└─ 예
└─ 거래 정상 완료

# 정상 처리 흐름

JDK·Gradle 확인
↓
올바른 Branch·Commit
↓
local Profile로 bootRun
↓
Port·DB·Handler 로그 확인
↓
대표 요청 전송
↓
Controller Breakpoint
↓
TCF·STF
↓
Dispatcher·Handler
↓
Facade·Service·DAO·Mapper
↓
ETF
↓
표준 응답
↓
GUID·TraceId 로그
↓
DB 결과 확인

# 오류 처리 흐름

거래 실패
↓
응답의 오류코드·GUID 확인
↓
같은 GUID로 로그 검색
↓
최종 성공 단계 확인
↓
Controller·STF·Handler·SQL 구간 분류
↓
같은 요청으로 Breakpoint 재현
↓
Root Cause 확인
↓
수정
↓
정상·경계·실패 회귀
↓
전체 Build

# 정상 예시

Branch
feature/REQ-SV-014-customer-summary

Commit
a12bc34

실행
gradle :sv-service:bootRun

Profile
local

Application
nsight-sv-service

Port
8086

요청
POST /sv/online

ServiceId
SV.Customer.selectSummary

Call Stack
OnlineTransactionController
→ TCF
→ STF
→ TransactionDispatcher
→ SvCustomerHandler
→ SvCustomerFacade
→ SvCustomerService
→ SvCustomerDao
→ SvCustomerMapper
→ ETF

응답
SUCCESS

증적
GUID·TraceId 로그
Mapper SQL ID
고객요약 결과
거래로그 정상 종료

# 금지 예시

어떤 Profile인지 확인하지 않고 실행한다.

기동 로그의 Warning을 모두 무시한다.

Started 로그만 보고 개발 완료 처리한다.

다른 업무 모듈의 Port를 임의로 사용한다.

운영 DB 비밀번호를 local YML에 기록한다.

Handler가 실행되지 않는데 Service부터 디버깅한다.

모든 메서드에 무조건 Breakpoint를 건다.

Breakpoint로 발생한 Timeout을 업무 오류로 판단한다.

Evaluate Expression에서 DB 변경 메서드를 실행한다.

오류를 catch하고 null을 반환한다.

시스템 오류 Stack Trace를 사용자에게 반환한다.

요청·응답 전체와 JWT를 Console에 출력한다.

Health UP만 확인하고 대표 거래를 시험하지 않는다.

TCF 우회 Controller를 운영 API로 사용한다.

오류 발생 즉시 증거 없이 재기동한다.

# 책임 경계와 RACI

| 활동 | 프레임워크팀 | 업무개발팀 | DevOps | DBA | 운영 |
| --- | --- | --- | --- | --- | --- |
| Profile 구조 | R/A | C | C | I | I |
| 업무 Local YML | C | R/A | C | C | I |
| 로컬 Port 표준 | C | R | A | I | I |
| Main Class | C | R/A | I | I | I |
| TCF 기동 로그 | R/A | C | C | I | C |
| Handler 등록 | C | R/A | I | I | C |
| Mapper 등록 | C | R/A | I | C | I |
| 로컬 DB | I | R | C | A/C | I |
| Breakpoint 분석 | C | R/A | I | C | C |
| Timeout 분석 | R | R | C | C | C |
| 운영 Remote Debug | C | C | R | I | A |
| Secret 관리 | C | R | R/A | C | A/C |
| 기동 Runbook | C | R | R | C | A |

# 자동검증 및 품질 Gate

## 1\. Profile Gate

승인된 Profile만 사용
prod 기본 활성화 금지
local Schema Init의 prod 적용 금지
환경별 Secret 분리
설정 Placeholder 검증

## 2\. Port Gate

업무별 로컬 Port 중복 검사
Gateway·JWT·OM·Batch Port 검사
설정표와 YML 비교

## 3\. Context Gate

Main Class 존재
Component Scan 정상
Mapper Scan 정상
중복 Bean 없음
필수 TCF Bean 존재

## 4\. Handler Gate

ServiceId 중복 없음
ServiceId 누락 없음
OM Catalog 정합성
Handler Bean 등록

## 5\. Resource Gate

application-local.yml 존재
Mapper XML 포함
schema.sql·data.sql 검증
UTF-8
비밀정보 없음

## 6\. Smoke Gate

Health UP
대표 조회 거래 성공
업무 오류 응답 정상
미등록 ServiceId 응답 정상
Timeout 응답 정상
거래로그 종료

## 7\. 보안 Gate

JWT 원문 로그 금지
DB 비밀번호 Commit 금지
Remote Debug 외부 노출 금지
Actuator 민감 Endpoint 제한
개인정보 마스킹

# 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| DBG-001 | local Profile 기동 | local 설정 적용 |
| DBG-002 | Profile 미지정 | 승인된 기본 Profile |
| DBG-003 | dev 오기동 | 설정 차이 탐지 |
| DBG-004 | SV Port 8086 | 정상 기동 |
| DBG-005 | Port 중복 | 기동 실패 |
| DBG-006 | H2 Datasource | Pool 시작 |
| DBG-007 | DB 계정 오류 | 명확한 Pool 실패 |
| DBG-008 | Schema 초기화 | 테이블·데이터 생성 |
| DBG-009 | Schema 문법 오류 | Statement 위치 표시 |
| DBG-010 | Mapper Bean 등록 | 정상 |
| DBG-011 | Mapper XML 누락 | 거래 실패 탐지 |
| DBG-012 | Handler 등록 | 등록 로그 확인 |
| DBG-013 | ServiceId 중복 | 기동 실패 |
| DBG-014 | 정상 대표 거래 | SUCCESS |
| DBG-015 | Header 필수값 누락 | INVALID\_HEADER |
| DBG-016 | 미등록 ServiceId | SERVICE\_NOT\_FOUND |
| DBG-017 | 업무 오류 | Business Fail 응답 |
| DBG-018 | SQL 오류 | System Error 응답 |
| DBG-019 | Online Timeout | 표준 Timeout |
| DBG-020 | Breakpoint 장시간 정지 | Debug Timeout 식별 |
| DBG-021 | Transaction Rollback | 데이터 미반영 |
| DBG-022 | 외부 IC 미기동 | Connection Refused |
| DBG-023 | 외부 IC 응답지연 | Read Timeout |
| DBG-024 | JWT 누락 | 401 |
| DBG-025 | 권한 없음 | 403 |
| DBG-026 | GUID 로그 검색 | 전체 단계 연결 |
| DBG-027 | Thread 전환 | MDC·Context 유지 |
| DBG-028 | TCF 우회 API | 공통 통제 누락 확인 |
| DBG-029 | Health UP·거래 실패 | Business Smoke 필요 |
| DBG-030 | 운영 Secret Commit | 보안 Gate 실패 |
| DBG-031 | Actuator 민감정보 | 외부 접근 차단 |
| DBG-032 | Remote Debug Port | 관리망만 접근 |
| DBG-033 | 재기동 후 재현 | 같은 결과 |
| DBG-034 | WAR 외부 Tomcat | 동일 대표 거래 성공 |
| DBG-035 | 로컬·CI Profile 비교 | 설정 정합성 확인 |

# 따라 하는 실무 절차

## 1단계. 현재 기준을 기록한다

git status -sb
git rev-parse --short HEAD
java -version
gradle --version

## 2단계. 업무 설정을 확인한다

application.yml
application-local.yml
Main Class
build.gradle
run-local Script

기록:

| 항목 | 값 |
| --- | --- |
| Module | sv-service |
| Main Class |  |
| Profile | local |
| Port | 8086 |
| DB | H2 |
| ServiceId | 대표 거래 |
| 외부 연계 | IC 등 |

## 3단계. Clean Build를 수행한다

gradle clean :sv-service:build

## 4단계. Debug Mode로 실행한다

IDE에서 Gradle Task:

:sv-service:bootRun

또는:

gradle :sv-service:bootRun --debug-jvm

## 5단계. 기동 로그를 기록한다

Profile
Application Name
Port
Hikari Pool
Handler 수
Started 시간
Warning

## 6단계. Breakpoint를 설정한다

OnlineTransactionController
TCF.process
STF.preProcess
TransactionDispatcher.dispatch
업무 Handler
Facade
Service
DAO
ETF

## 7단계. 대표 거래를 호출한다

POST http://localhost:8086/sv/online
ServiceId: SV.Customer.selectSummary

## 8단계. Call Stack을 기록한다

실제 클래스
실제 메서드
Thread
ServiceId
GUID
TraceId

## 9단계. SQL과 DB 결과를 확인한다

Mapper Namespace
SQL ID
입력 Parameter
조회·변경 건수
DB 결과

## 10단계. 오류를 재현한다

최소:

필수 Header 누락
미등록 ServiceId
업무 데이터 없음
DB 오류
Timeout

## 11단계. 로그로 다시 추적한다

GUID 검색
↓
Controller
↓
STF
↓
Handler
↓
SQL
↓
ETF

## 12단계. 외부 Tomcat과 비교한다

Profile
Context Path
Port
Datasource
JDK
WAR Resource
대표 거래

# 완료 체크리스트

## Profile·설정

| 확인 항목 | 완료 |
| --- | --- |
| local, dev, prod 차이를 설명할 수 있다. | □ |
| 활성 Profile을 기동 로그에서 확인했다. | □ |
| 설정 우선순위를 확인했다. | □ |
| 업무 Port를 확인했다. | □ |
| Datasource URL을 확인했다. | □ |
| Hikari Pool 이름을 확인했다. | □ |
| 거래로그 경로를 확인했다. | □ |
| 외부 연계 URL을 확인했다. | □ |
| Secret이 저장소에 없는지 확인했다. | □ |
| Schema 자동초기화 범위를 확인했다. | □ |

## 기동 로그

| 확인 항목 | 완료 |
| --- | --- |
| Main Class를 확인했다. | □ |
| Application Name을 확인했다. | □ |
| Profile 로그를 확인했다. | □ |
| HikariCP 시작을 확인했다. | □ |
| Mapper Scan을 확인했다. | □ |
| Handler 등록을 확인했다. | □ |
| 중복 ServiceId가 없는지 확인했다. | □ |
| Tomcat Port를 확인했다. | □ |
| Warning을 분류했다. | □ |
| Health 응답을 확인했다. | □ |

## 디버깅

| 확인 항목 | 완료 |
| --- | --- |
| Controller Breakpoint에 도달했다. | □ |
| TCF·STF 호출을 확인했다. | □ |
| Dispatcher의 ServiceId를 확인했다. | □ |
| 선택된 Handler를 확인했다. | □ |
| Facade Transaction을 확인했다. | □ |
| Service·Rule 호출을 확인했다. | □ |
| DAO·Mapper Parameter를 확인했다. | □ |
| ETF 응답 생성을 확인했다. | □ |
| Call Stack을 기록했다. | □ |
| Thread 전환 여부를 확인했다. | □ |

## 거래·오류

| 확인 항목 | 완료 |
| --- | --- |
| 정상 거래를 실행했다. | □ |
| Header 오류를 실행했다. | □ |
| 미등록 ServiceId를 실행했다. | □ |
| 업무 오류를 실행했다. | □ |
| 시스템 오류를 실행했다. | □ |
| Timeout을 실행했다. | □ |
| Rollback을 DB에서 확인했다. | □ |
| GUID로 로그를 연결했다. | □ |
| 민감정보가 로그에 없는지 확인했다. | □ |
| 재기동 후 결과를 재현했다. | □ |

# 변경·호환성·폐기 관리

## Profile 추가

신규 Profile을 추가할 때 다음을 정의한다.

목적
사용 환경
설정 소유자
DB
외부 연계
Secret 주입
로그 Level
배포 방식
테스트 범위

의미 없는 test2, temp, real Profile을 만들지 않는다.

## Port 변경

Port 변경 영향:

IDE Run Configuration
UI API URL
Gateway Route
업무 간 연계 URL
Health Check
방화벽
테스트 Script
문서

## Datasource 변경

Driver
URL
Schema
Hikari
SQL 문법
Transaction
Test 데이터
Migration

H2에서 Oracle로 전환할 때 전체 Mapper SQL을 검증한다.

## Main Class 변경

확인:

Component Scan
Mapper Scan
WAR Bootstrap
bootRun
bootWar
외부 Tomcat

## Handler 폐기

ServiceId 사용처 검색
화면 이벤트 확인
OM Catalog 확인
업무 연계 확인
Handler 제거
회귀테스트
운영 등록 폐기

## 로컬 우회 Endpoint 폐기

TCF 우회 샘플은 운영반영 전에 다음을 결정한다.

완전 제거
개발 Profile 전용
관리망 제한
인증 적용
사용 이력 감사

운영 외부에서 접근 가능한 상태로 남기지 않는다.

# 시사점

## 핵심 아키텍처 판단

로컬 실행은 단순 개발 편의 기능이 아니다.

설계
↓
소스
↓
환경설정
↓
Spring Context
↓
실행 흐름
↓
로그·DB 결과

가 실제로 연결되는지 검증하는 첫 번째 아키텍처 시험이다.

문서에는 TCF를 거친다고 적혀 있지만 Breakpoint Call Stack이 업무 Controller에서 Service로 직접 이동한다면 실제 구현은 설계와 다르다.

문서상의 아키텍처
≠ 실제 Call Stack

실제 Call Stack
\= 실행 아키텍처

## 주요 위험

| 위험 | 영향 |
| --- | --- |
| Profile 오인 | 잘못된 DB·외부 시스템 호출 |
| Port 중복 | 기동 실패·잘못된 서비스 호출 |
| H2 과신 | 운영 DB 오류 미탐지 |
| Handler 미등록 | ServiceId 실행 불가 |
| TCF 우회 | 거래통제·로그·Timeout 누락 |
| Breakpoint 장시간 정지 | 가짜 Timeout |
| System.out 의존 | 운영 추적·성능 문제 |
| Payload 원문 로그 | 개인정보 노출 |
| Transaction Proxy 오인 | Rollback 실패 |
| Health만 확인 | 업무 장애 미탐지 |
| Remote Debug 개방 | 보안 사고 |
| 재기동 우선 | 원인 증거 소실 |

## 우선 보완 과제

1.  모든 업무 모듈의 표준 Run Configuration을 제공한다.
2.  업무별 로컬 Port 관리표를 기준화한다.
3.  Profile별 설정 차이표를 자동 생성한다.
4.  Secret이 없는 application-local-example.yml을 제공한다.
5.  기동 시 Application·Profile·Port·Commit ID를 구조화 로그로 남긴다.
6.  Handler 등록 ServiceId를 기동 시 검증한다.
7.  중복 ServiceId는 기동 실패로 처리한다.
8.  대표 ServiceId Smoke Test를 자동화한다.
9.  Mapper XML과 SQL ID 정합성을 Build Gate에서 검사한다.
10.  GUID·TraceId·ServiceId를 모든 업무 로그에 포함한다.
11.  현재 Console 진단 출력을 구조화 Logging으로 전환한다.
12.  TCF 우회 Endpoint를 제거하거나 개발 Profile로 제한한다.
13.  Remote Debug 운영절차와 방화벽 기준을 수립한다.
14.  로컬 H2와 Oracle 통합 Test를 분리 운영한다.
15.  로컬 실행 결과와 CI·외부 Tomcat 결과를 비교하는 Gate를 만든다.

## 중장기 발전 방향

개인별 수동 bootRun
↓
표준 Run Configuration
↓
자동 Profile 검증
↓
대표 거래 Smoke Test
↓
컨테이너 기반 로컬 환경
↓
DB·외부 서비스 Test Container
↓
분산 Trace 연계
↓
실행 아키텍처 자동 검증

# 마무리말

로컬 실행과 디버깅의 목적은 프로그램을 한 번 띄워 보는 것이 아니다.

다음 질문에 답할 수 있어야 한다.

어느 Branch와 Commit을 실행했는가?

어떤 Profile이 활성화되었는가?

실제 Port와 DB는 무엇인가?

어떤 Handler가 ServiceId를 처리하는가?

실제 Call Stack은 설계와 일치하는가?

Transaction은 어디에서 시작되는가?

어떤 SQL이 실행되었는가?

오류는 업무·시스템·Timeout 중 무엇인가?

GUID로 전체 로그를 다시 찾을 수 있는가?

같은 환경에서 결과를 다시 재현할 수 있는가?

제8장에서 가장 자주 확인할 대상은 다음과 같다.

application.yml
application-local.yml
Main Application Class
OnlineTransactionController
TCF
STF
TransactionDispatcher
TransactionHandler
Facade
Service
DAO
Mapper
ETF

가장 중요한 원칙은 다음과 같다.

Started Application
\= 프로세스 기동

Health UP
\= 기본 준비상태

대표 ServiceId 성공
\+ 로그 추적
\+ DB 결과
\+ 오류 시나리오
\= 로컬 실행 검증 완료

다음 장에서는 처음 보는 NSIGHT TCF 저장소에서 ServiceId, Handler, Facade, Service, Mapper와 설정파일을 빠르게 찾고, 기존 정상 거래를 기준으로 소스 지도를 만드는 방법을 학습한다.
