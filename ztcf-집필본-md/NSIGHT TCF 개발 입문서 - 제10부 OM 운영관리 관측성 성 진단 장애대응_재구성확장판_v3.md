<!-- source: ztcf-집필본/NSIGHT TCF 개발 입문서 - 제10부 OM 운영관리 관측성 성 진단 장애대응_재구성확장판_v3.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

나는 제10부. OM 운영관리·관측성·성능진단·장애대응의 개념을 코드·설정·로그·산출물 중 어디에서 확인할 수 있는가? 문제가 발생했을 때 어느 경계부터 조사해야 하는가?

읽기 전 질문

① 장의 학습 목표를 먼저 읽습니다. ② 기존 설명과 예제를 따라갑니다. ③ 실무 적용 절차를 자신의 프로젝트에 대입합니다. ④ 완료 기준으로 이해 여부를 확인합니다.

권장 학습법

테스트·배포·운영·명명규칙을 실제 프로젝트 산출물과 연결해 적용합니다.

이 부의 학습 좌표

RESTRUCTURED & EXPANDED EDITION

4단계 · 품질과 실전

**초보 IT 개발자를 위한 NSIGHT TCF 개발 입문서**

# **제10부. OM 운영관리·관측성·성능진단·장애대응**

# **1\. 도입 전 안내말**

제9부에서는 테스트, 품질 Gate, CI/CD, 배포와 운영전환을 배웠습니다.

운영 배포가 끝나면 이제 실제 사용자가 시스템을 사용하기 시작합니다.

운영 중에는 다음과 같은 문의가 발생합니다.

고객조회 화면이 느립니다.

특정 시간대에만 Timeout이 발생합니다.

캠페인 조회는 정상인데 등록만 실패합니다.

Tomcat Thread가 부족한 것 같습니다.

DB Connection을 얻지 못한다는 오류가 발생합니다.

CPU는 낮은데 거래가 느립니다.

특정 업무 WAR만 응답이 늦습니다.

어느 SQL 때문에 장애가 발생했는지 모르겠습니다.

이때 운영자가 단순히 다음 정보만 볼 수 있다면 원인을 찾기 어렵습니다.

서버 CPU 45%

메모리 60%

Tomcat 실행 중

서버가 살아 있다는 사실만으로 업무 거래가 정상이라고 판단할 수 없기 때문입니다.

운영자는 다음 관계를 연결해서 볼 수 있어야 합니다.

사용자 화면
→ ServiceId
→ 업무 WAR
→ Tomcat Thread
→ Facade·Service
→ DAO·Mapper
→ SQL
→ DB Connection
→ 응답 결과

예를 들어 다음과 같은 상황을 생각해 봅시다.

SV.Customer.selectSummary
p95 응답시간 4.8초

Tomcat Busy Thread
정상 범위

JVM CPU
정상 범위

DB Pool Pending
급증

Slow SQL
SvCustomerMapper.selectCustomerSummary
평균 3.9초

이 경우 단순히 Tomcat Thread 수를 늘리는 것은 올바른 해결이 아닙니다.

실제 원인
→ Slow SQL로 DB Connection 장기 점유

Thread 증가
→ 더 많은 SQL이 동시에 DB로 유입
→ DB 부하 악화

따라서 운영진단은 다음 순서로 진행해야 합니다.

증상 확인
→ 영향범위 확인
→ ServiceId 확인
→ TraceId 확보
→ WAR·Thread 확인
→ JVM 확인
→ DB Pool 확인
→ SQL·Lock 확인
→ 최근 변경 확인
→ 완화·복구

OM은 단순한 관리자 화면이 아닙니다.

OM
\= Operation Management

서비스 기준정보 관리
\+ 거래통제
\+ Timeout 정책
\+ 거래로그 조회
\+ 자원상태 관측
\+ 장애진단
\+ 운영조치
\+ 감사

제10부에서는 Prometheus 같은 별도 관측 플랫폼이 없어도 tcf-om에서 최소한 다음 질문에 답할 수 있는 구조를 설명합니다.

현재 거래가 왜 느린가?

Tomcat Thread가 부족한가?

JVM CPU 또는 GC가 문제인가?

DB Connection을 얻지 못해 대기하는가?

특정 업무 WAR가 자원을 독점하는가?

어떤 ServiceId와 SQL에서 장애가 발생하는가?

# **2\. 제10부 개요**

## **2.1 목적**

제10부의 목적은 초보 개발자와 운영자가 OM을 이용해 거래·애플리케이션·JVM·DB 상태를 연결하고, 장애 원인을 단계적으로 좁혀 가도록 돕는 것입니다.

학습을 마친 뒤에는 다음 작업을 수행할 수 있어야 합니다.

1.  OM의 운영관리 책임을 설명한다.
2.  Service Catalog와 거래통제 기준을 관리한다.
3.  GUID·TraceId·ServiceId로 거래를 추적한다.
4.  거래로그에서 성공·업무 오류·시스템 오류·Timeout을 구분한다.
5.  Tomcat Thread 상태를 해석한다.
6.  JVM CPU·Heap·GC 상태를 해석한다.
7.  HikariCP Active·Idle·Pending 값을 해석한다.
8.  Slow SQL과 Mapper ID를 ServiceId에 연결한다.
9.  특정 WAR의 자원 독점 여부를 판단한다.
10.  Metric·Log·Trace의 차이를 설명한다.
11.  경보 임계값과 지속시간을 설계한다.
12.  장애 시 우선 확인 순서를 적용한다.
13.  거래통제·Rate Limit·Rollback으로 장애를 완화한다.
14.  운영 조치를 감사로그로 남긴다.
15.  장애 이후 재발방지 과제를 도출한다.

## **2.2 적용범위**

| **영역** | **주요 내용** |
| --- | --- |
| OM 기준정보 | ServiceId, 거래코드, 권한 |
| 거래통제 | 실행 허용·중지·읽기 전용 |
| Timeout | ServiceId·SQL·외부 호출 |
| 거래로그 | 시작·종료·오류·처리시간 |
| Trace | GUID·TraceId·호출관계 |
| Tomcat | Thread·Queue·Connector |
| JVM | CPU·Heap·Metaspace·GC |
| DB Pool | Active·Idle·Pending |
| SQL | Mapper ID·수행시간·건수 |
| WAR | 업무별 처리량·오류율 |
| Metric | Counter·Gauge·Histogram |
| 경보 | 임계값·지속시간·심각도 |
| 장애대응 | 영향분석·완화·복구 |
| 감사 | 운영자 조치·설정 변경 |
| 자동검증 | 운영 설정과 코드 정합성 |

## **2.3 대상 독자**

-   운영 로그를 어디서부터 봐야 할지 모르는 개발자
-   CPU가 높으면 무조건 서버를 증설하려는 개발자
-   Tomcat maxThreads만 늘리면 성능이 좋아진다고 생각하는 개발자
-   Hikari Pool의 Active와 Pending 의미가 어려운 개발자
-   특정 ServiceId와 SQL을 연결하고 싶은 개발자
-   Prometheus 없이 간단한 OM 대시보드를 만들려는 개발자
-   장애 시 서버 재시작부터 수행하는 운영자
-   운영 기준정보와 감사체계를 설계해야 하는 아키텍트

## **2.4 선행조건**

다음 내용을 이해하고 있어야 합니다.

ServiceId는 기능 실행 식별자다.

거래코드는 운영·감사 분류 식별자다.

GUID는 전체 거래를 연결한다.

TraceId는 내부 실행 로그를 연결한다.

STF는 거래 시작 전 공통검증을 수행한다.

ETF는 거래 종료상태를 확정한다.

Facade는 트랜잭션 경계를 가진다.

## **2.5 주요 용어**

| **용어** | **쉬운 설명** |
| --- | --- |
| OM | 운영관리 시스템 |
| 관측성 | 외부에서 내부 상태와 원인을 추론할 수 있는 능력 |
| Metric | 수치로 집계한 상태정보 |
| Log | 특정 시점에 발생한 상세 기록 |
| Trace | 하나의 요청이 거친 전체 경로 |
| Counter | 계속 증가하는 누적값 |
| Gauge | 현재 상태값 |
| Histogram | 처리시간 분포 |
| p95 | 전체 요청의 95%가 이 시간 안에 완료됨 |
| Busy Thread | 현재 요청을 처리 중인 Thread |
| Queue | Thread를 기다리는 요청 |
| Heap | Java 객체가 저장되는 메모리 |
| GC | 사용하지 않는 객체를 정리하는 작업 |
| GC Pause | GC 때문에 애플리케이션이 멈추는 시간 |
| DB Pool | 재사용 가능한 DB Connection 모음 |
| Active Connection | 현재 사용 중인 Connection |
| Idle Connection | 대기 중인 사용 가능한 Connection |
| Pending | Connection을 기다리는 Thread 수 |
| Slow SQL | 기준시간보다 오래 걸린 SQL |
| Lock Wait | 다른 트랜잭션의 Lock 해제를 기다리는 상태 |
| Saturation | Thread·Pool·CPU 같은 자원이 한계에 가까운 상태 |
| Runbook | 장애 상황별 점검·조치 절차 |

# **제75장. OM 운영관리 구조 이해하기**

학습 목표 | 75장. OM 운영관리 구조 이해하기의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **75.1 OM이 필요한 이유**

운영환경에서 코드와 설정은 서로 분리되어 있습니다.

개발자가 코드에 다음 값을 직접 작성하면 운영이 어렵습니다.

if ("SV.Customer.selectSummary".equals(serviceId)) {
timeout = 3000;
}

Timeout을 바꾸려면 코드 수정과 재배포가 필요하기 때문입니다.

OM을 사용하면 운영 기준정보를 코드와 분리할 수 있습니다.

ServiceId
→ Timeout
→ 실행상태
→ 권한
→ 감사여부
→ 운영 담당자

## **75.2 OM의 주요 기능**

서비스 관리
거래통제
Timeout 관리
권한 관리
오류코드 관리
거래로그 조회
운영 대시보드
Batch·Scheduler 관리
배포이력 관리
운영 감사

## **75.3 Service Catalog**

Service Catalog는 시스템이 제공하는 기능의 운영 대장입니다.

예:

| **항목** | **값** |
| --- | --- |
| 업무코드 | SV |
| ServiceId | SV.Customer.selectSummary |
| 거래코드 | SV-INQ-0001 |
| 서비스명 | 고객 요약조회 |
| 처리유형 | INQUIRY |
| 대상 WAR | sv-service |
| Handler | SvCustomerHandler |
| 기본 Timeout | 3000ms |
| 필요 권한 | SV\_CUSTOMER\_VIEW |
| 감사 대상 | Y |
| 상태 | ACTIVE |
| 운영 담당 | SV 운영팀 |

## **75.4 Service Catalog가 누락되면**

가능한 정책:

### **미등록 거래 차단**

ServiceId 미등록
→ 실행 거부

장점:

-   비인가 기능 실행 차단
-   운영 통제 가능
-   등록 누락 조기 발견

### **기본정책으로 허용**

ServiceId 미등록
→ 기본 Timeout·권한 적용

장점:

-   가용성 우선

단점:

-   통제되지 않은 기능 실행 가능

금융권 중요 거래는 등록되지 않은 ServiceId를 차단하는 Fail-closed 정책을 우선 검토합니다.

## **75.5 운영상태**

| **상태** | **의미** |
| --- | --- |
| ACTIVE | 정상 실행 |
| SUSPENDED | 일시 중지 |
| DISABLED | 사용 중지 |
| READ\_ONLY | 조회만 허용 |
| MAINTENANCE | 점검 중 |
| DEPRECATED | 폐기 예정 |

## **75.6 OM과 실행 코드의 관계**

OM DB
→ 정책 Cache
→ STF
→ Handler 실행 여부 결정

모든 요청마다 OM DB를 직접 조회하면 부하와 장애 의존성이 커질 수 있습니다.

따라서 업무 WAR는 정책을 Cache하고, OM 변경 Event로 무효화할 수 있습니다.

## **75.7 최소 OM 구성**

초기 단계에서 다음 기능부터 구현할 수 있습니다.

Service Catalog 조회

ServiceId 상태 변경

Timeout 변경

거래로그 검색

WAR별 상태 조회

Thread·JVM·DB Pool 조회

Slow ServiceId 조회

복잡한 분석 플랫폼을 처음부터 만들기보다 장애 진단에 직접 필요한 기능부터 구성합니다.

## **75.8 OM 자체의 장애**

OM 관리화면이 장애라고 모든 업무 거래가 중단되어서는 안 됩니다.

다음 영역을 분리합니다.

OM 관리 UI
→ 장애 가능

업무 WAR 정책 Cache
→ 마지막 정상 정책 유지

운영 변경 기능
→ 일시 중단

단, 새로운 ServiceId나 새 정책은 OM이 복구될 때까지 반영되지 않을 수 있습니다.

## **75.9 OM 권한**

운영 권한은 목적별로 분리합니다.

| **권한** | **기능** |
| --- | --- |
| OM\_SERVICE\_VIEW | 서비스 조회 |
| OM\_SERVICE\_CONTROL | 거래중지 |
| OM\_TIMEOUT\_UPDATE | Timeout 변경 |
| OM\_BATCH\_CONTROL | Batch 실행·중지 |
| OM\_LOG\_VIEW | 거래로그 조회 |
| OM\_SECURITY\_AUDIT | 보안감사 조회 |

한 명의 운영자에게 모든 권한을 부여하지 않습니다.

## **제75장 요약**

학습 목표 | 75장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

OM은 단순 모니터링 화면이 아니다.

ServiceId 실행정책과 운영상태를 관리하고,
거래·자원·오류정보를 연결하는 운영 통제점이다.

업무 WAR는 OM 장애 시에도
마지막 정상 정책으로 제한적 운영이 가능해야 한다.

# **제76장. 거래로그와 ServiceId 추적**

학습 목표 | 76장. 거래로그와 ServiceId 추적의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 식별 가능성과 일관성: 이름은 단순 표기가 아니라 소스, 거래, 로그, 운영 설정과 산출물을 연결하는 공통 키입니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **76.1 거래로그의 목적**

거래로그는 다음 질문에 답해야 합니다.

어떤 기능이 호출되었는가?

누가 호출했는가?

언제 시작하고 끝났는가?

성공했는가?

어느 오류코드로 실패했는가?

얼마나 오래 걸렸는가?

어느 서버에서 처리했는가?

## **76.2 거래 시작과 종료**

STF:

거래로그 시작
status = PROCESSING

ETF:

거래로그 종료
status =
SUCCESS
BUSINESS\_FAIL
SYSTEM\_ERROR
TIMEOUT
REJECTED

거래가 시작됐지만 종료기록이 없다면 서버 중단이나 비정상 종료 가능성을 확인해야 합니다.

## **76.3 거래로그 기본항목**

| **항목** | **설명** |
| --- | --- |
| GUID | 전체 거래 식별 |
| TraceId | 실행 흐름 식별 |
| ServiceId | 기능 식별 |
| 거래코드 | 운영 분류 |
| 업무코드 | 대상 업무 |
| WAR | 실행 WAR |
| Server ID | 실행 서버 |
| 사용자 | 인증 사용자 |
| 지점 | 사용자 지점 |
| 채널 | WEB·내부·Batch |
| 시작시각 | 요청 시작 |
| 종료시각 | 요청 종료 |
| 처리시간 | elapsedMs |
| 결과상태 | 성공·실패 |
| 결과코드 | 표준 코드 |
| Timeout | 적용 제한시간 |
| Retry Count | 재시도 횟수 |

## **76.4 GUID와 TraceId**

예:

사용자 요청
GUID = G001

Gateway
TraceId = T-GW-001

SV WAR
TraceId = T-SV-001

IC 호출
TraceId = T-IC-001

GUID는 여러 시스템을 하나의 거래로 연결합니다.

TraceId는 각 실행구간의 로그를 연결합니다.

## **76.5 화면에서 SQL까지 추적**

화면 ID
SV-CUS-0001

이벤트
SV-CUS-0001-E01

ServiceId
SV.Customer.selectSummary

Handler
SvCustomerHandler

Mapper
SvCustomerMapper.selectCustomerSummary

SQL ID
SV-CUST-SEL-001

테이블
SV\_CUSTOMER\_SUMMARY

이 추적관계를 관리하면 장애 발생 시 화면에서 SQL까지 빠르게 이동할 수 있습니다.

## **76.6 느린 거래 조회**

OM 검색조건:

시간대
ServiceId
업무코드
WAR
서버
처리시간 이상
결과상태
오류코드
사용자·지점

예:

최근 10분
elapsedMs > 3000
status = TIMEOUT

## **76.7 오류코드 Top N**

최근 1시간 오류코드

1\. E-TCF-TIME-0001 1,250건
2\. E-SV-DB-0001 320건
3\. E-COM-AUTH-0001 110건

오류코드 증가 추세를 통해 장애범위를 빠르게 파악할 수 있습니다.

## **76.8 미종료 거래**

조건:

status = PROCESSING
현재시각 - 시작시각 > 최대 허용시간

가능한 원인:

-   WAS 비정상 종료
-   ETF 실행 전 장애
-   비동기 기록 실패
-   거래로그 저장소 장애
-   실제 처리 중인 장시간 Job

온라인 거래와 Batch Job을 같은 기준으로 판단하지 않습니다.

## **76.9 거래로그 저장 부하**

모든 로그를 동기 DB Insert로 처리하면 거래시간이 증가할 수 있습니다.

대안:

-   최소 필수항목 동기 기록
-   상세로그 비동기 기록
-   Buffer·Queue 사용
-   로그 파일과 집계 DB 분리
-   Sampling
-   오류 거래 상세 보관

중요 거래로그의 유실 허용 여부는 업무·감사 요구에 따라 결정합니다.

## **76.10 거래로그 보관**

| **구분** | **보관 예** |
| --- | --- |
| 온라인 거래 요약 | 운영정책 기간 |
| 오류 상세 | 상대적으로 장기 |
| 개인정보 조회 감사 | 법적 기준 |
| DEBUG 로그 | 단기 |
| Metric 집계 | 장기 추세 |
| 원문 전문 | 최소·제한 |

개인정보와 요청 Body 전체를 무조건 저장하지 않습니다.

## **제76장 요약**

학습 목표 | 76장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

거래로그는 ServiceId 단위로
시작·종료·처리시간·결과를 기록한다.

GUID는 전체 거래를,
TraceId는 세부 실행경로를 연결한다.

화면·ServiceId·Mapper·SQL 추적성을
운영에서 조회할 수 있어야 한다.

# **제77장. Tomcat Thread 상태 분석**

학습 목표 | 77장. Tomcat Thread 상태 분석의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **77.1 Thread란 무엇인가요?**

Tomcat Thread는 사용자의 HTTP 요청을 처리하는 작업자입니다.

요청
→ 사용 가능한 Thread 배정
→ 업무 처리
→ 응답
→ Thread 반환

Thread가 모두 사용 중이면 새 요청은 Queue에서 기다립니다.

## **77.2 주요 지표**

| **지표** | **의미** |
| --- | --- |
| maxThreads | 최대 처리 Thread |
| currentThreadsBusy | 현재 요청 처리 중 |
| currentThreadCount | 현재 생성된 Thread |
| acceptCount | 대기 Queue 허용량 |
| maxConnections | 최대 연결 수 |
| Queue Length | Thread 대기 요청 |
| Rejected Count | 거절된 요청 |

## **77.3 Busy Thread 비율**

개념:

Busy Thread 비율
\= currentThreadsBusy ÷ maxThreads

예:

maxThreads = 1,200
Busy = 600
→ 50%

운영 기준은 성능시험을 통해 확정해야 합니다.

참고 관점:

지속적으로 70% 이상
→ 주의

지속적으로 85% 이상
→ 포화 위험

100%
→ 신규 요청 대기·거절 가능

단시간의 순간 상승과 장시간 지속을 구분합니다.

## **77.4 Busy Thread가 높을 때**

가능한 원인:

트래픽 급증

Slow SQL

외부 시스템 지연

DB Pool 대기

파일 다운로드 장기 점유

무한 반복

Lock 대기

GC Pause 이후 요청 적체

Thread 수가 부족하다고 바로 판단하지 않습니다.

## **77.5 Busy Thread는 낮은데 느린 경우**

가능한 원인:

특정 ServiceId만 느림

요청량 자체가 적음

Gateway·Apache 구간 지연

사용자 네트워크 문제

DB 처리시간이 길지만 동시요청 적음

비동기 결과 대기

전체 평균 Thread만 보면 특정 거래의 문제를 놓칠 수 있습니다.

## **77.6 Thread를 늘리면 좋은가요?**

Thread 수를 늘리면 동시에 더 많은 요청을 받을 수 있습니다.

하지만 하위 자원이 부족하면 문제가 악화됩니다.

Thread 1,200
DB Pool 120

동시에 500개 SQL 요청
→ 120개 실행
→ 380개 Connection 대기

Thread 수는 다음 자원과 함께 설계합니다.

CPU Core

DB Pool

DB 동시처리 능력

외부 Client Pool

Heap

응답시간

## **77.7 Thread Dump**

Thread Dump는 각 Thread가 무엇을 하고 있는지 보여줍니다.

주요 상태:

| **상태** | **의미** |
| --- | --- |
| RUNNABLE | 실행 중 또는 I/O 수행 |
| WAITING | 조건을 무기한 기다림 |
| TIMED\_WAITING | 일정 시간 기다림 |
| BLOCKED | Java Monitor Lock 대기 |

## **77.8 Thread Dump에서 찾을 것**

같은 Stack이 다수 반복되는가?

JDBC 호출에서 대기하는가?

HikariPool.getConnection에서 대기하는가?

외부 HTTP Client에서 대기하는가?

동일 Lock에서 BLOCKED되는가?

무한 반복 코드가 있는가?

특정 ServiceId Thread가 집중되는가?

## **77.9 Thread와 ServiceId 연결**

Thread 이름 또는 MDC에 다음 정보를 연결할 수 있습니다.

Thread
http-nio-8080-exec-157

TraceId
T202607170001

ServiceId
SV.Customer.selectSummary

OM에서 Thread 목록을 조회할 때 현재 처리 중인 ServiceId와 처리시간을 표시하면 진단이 쉬워집니다.

## **77.10 Long-running Thread**

예:

| **Thread** | **ServiceId** | **실행시간** |
| --- | --- | --- |
| exec-101 | 고객 요약조회 | 350ms |
| exec-102 | 캠페인 목록조회 | 4,800ms |
| exec-103 | 파일 다운로드 | 92,000ms |

온라인 거래와 파일 Streaming을 구분해서 해석합니다.

## **77.11 Deadlock**

두 Thread가 서로의 Lock을 기다리면 진행되지 않습니다.

Thread A
Lock 1 보유
Lock 2 대기

Thread B
Lock 2 보유
Lock 1 대기

JVM Deadlock 탐지와 Thread Dump를 통해 확인합니다.

DB Deadlock과 Java Deadlock은 서로 다른 문제입니다.

## **제77장 요약**

학습 목표 | 77장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

Busy Thread가 높다는 것은
요청을 처리 중인 Thread가 많다는 뜻이다.

원인은 트래픽·SQL·외부연계·DB Pool 등 다양하다.

Thread 수를 늘리기 전에
Thread가 어디에서 기다리는지 확인해야 한다.

# **제78장. JVM CPU·Heap·GC 분석**

학습 목표 | 78장. JVM CPU·Heap·GC 분석의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **78.1 JVM에서 확인할 것**

Process CPU

System CPU

Heap Used

Heap Max

Metaspace

Thread Count

GC Count

GC Pause

Allocation Rate

Class Count

## **78.2 CPU가 높을 때**

가능한 원인:

복잡한 업무 계산

JSON 변환 과다

대량 반복문

암호화·압축

GC 과다

무한 Loop

과도한 로그 문자열 생성

동시 요청 급증

CPU가 높다는 이유만으로 서버 증설을 먼저 하지 않습니다.

어느 Thread와 ServiceId가 CPU를 사용하는지 확인합니다.

## **78.3 CPU가 낮은데 거래가 느린 경우**

가능한 원인:

DB I/O 대기

외부 시스템 대기

Lock 대기

Connection Pool 대기

파일 I/O

네트워크 지연

CPU가 낮다는 것은 시스템이 빠르다는 뜻이 아닙니다.

대기 중심 장애에서는 CPU가 낮을 수 있습니다.

## **78.4 Heap**

Heap에는 Java 객체가 저장됩니다.

요청 DTO

응답 DTO

조회 결과 List

Cache

파일 byte\[\]

로그 Buffer

Heap 사용률이 지속적으로 증가하고 GC 후에도 내려오지 않으면 메모리 누수를 의심할 수 있습니다.

## **78.5 정상적인 Heap 패턴**

객체 생성
→ Heap 증가
→ GC
→ Heap 감소
→ 반복

톱니 모양으로 증감할 수 있습니다.

## **78.6 비정상 패턴**

Heap 증가
→ GC
→ 거의 감소하지 않음
→ 다시 증가
→ OOM

가능한 원인:

-   무제한 Cache
-   Static Collection
-   ThreadLocal 정리 누락
-   대용량 파일 메모리 적재
-   응답 List 과다
-   ClassLoader 누수
-   로그 Queue 적체

## **78.7 GC란 무엇인가요?**

GC는 더 이상 사용하지 않는 객체를 정리합니다.

GC 자체는 정상 기능입니다.

문제는 다음과 같은 경우입니다.

GC가 너무 자주 발생

한 번의 GC Pause가 너무 김

Full GC 반복

GC 후 Heap 회복이 적음

## **78.8 GC 지표**

| **지표** | **설명** |
| --- | --- |
| Young GC Count | 짧은 수명 객체 정리 횟수 |
| Young GC Time | 누적 정리시간 |
| Old·Full GC Count | 오래된 영역 정리 |
| Max Pause | 최대 멈춤시간 |
| p95 Pause | 대부분의 GC 멈춤시간 |
| Promotion Rate | Old 영역 이동속도 |
| Allocation Rate | 객체 생성속도 |

## **78.9 GC Pause와 거래 지연**

GC 동안 애플리케이션 Thread가 멈추면 여러 ServiceId가 동시에 느려질 수 있습니다.

특징:

여러 업무 WAR 거래가 같은 시각에 지연

DB SQL 시간은 정상

Thread Dump에서 특별한 대기 없음

GC Pause 시각과 p95 상승 시각 일치

## **78.10 Heap 크기**

Heap을 무조건 크게 설정한다고 좋은 것은 아닙니다.

장점:

-   객체 수용량 증가
-   GC 빈도 감소 가능

단점:

-   큰 GC Pause 가능
-   메모리 문제 발견 지연
-   OS Page 관리 부담

Heap 크기는 다음을 고려합니다.

동시 요청

Cache 용량

대용량 처리

응답 객체 크기

GC 알고리즘

서버 전체 메모리

성능시험

## **78.11 Metaspace**

Metaspace에는 Class Metadata가 저장됩니다.

지속적으로 증가하면 다음을 확인합니다.

-   반복 배포
-   ClassLoader 누수
-   동적 Proxy 과다
-   Runtime Class 생성
-   WAR 재배포 방식

Tomcat에서 WAR를 반복 재배포할 때 ClassLoader가 정리되지 않으면 Metaspace가 증가할 수 있습니다.

## **78.12 ThreadLocal 누수**

TCF는 TransactionContext, AuthenticationContext, MDC를 ThreadLocal에 저장할 수 있습니다.

반드시 요청 종료 후 정리해야 합니다.

finally {
TransactionContextHolder.clear();
AuthenticationContextHolder.clear();
TimeoutContextHolder.clear();
MDC.clear();
}

정리하지 않으면 이전 사용자의 정보가 다음 요청에 남거나 메모리 누수가 발생할 수 있습니다.

## **78.13 Heap Dump**

OOM 또는 메모리 누수 분석 시 Heap Dump를 사용합니다.

주의:

-   파일 크기가 매우 큼
-   개인정보 포함 가능
-   생성 중 일시 정지 가능
-   접근권한 통제 필요
-   운영 Storage 용량 확인

Heap Dump는 제한된 담당자만 접근해야 합니다.

## **제78장 요약**

학습 목표 | 78장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

CPU가 높으면 계산·GC·반복처리를 확인한다.

CPU가 낮아도 DB·외부연계 대기 때문에
거래는 느릴 수 있다.

Heap은 GC 이후 얼마나 회복되는지 확인하고,
Cache·ThreadLocal·대용량 객체를 점검한다.

# **제79장. DB Pool 상태 분석**

학습 목표 | 79장. DB Pool 상태 분석의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 데이터 일관성: 화면 동작보다 데이터 소유권, 상태 전이, 동시성, 트랜잭션 경계를 먼저 결정해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **79.1 DB Pool이 필요한 이유**

DB Connection 생성은 비용이 큽니다.

HikariCP는 Connection을 미리 만들고 재사용합니다.

요청
→ Pool에서 Connection 획득
→ SQL 실행
→ Connection 반환

## **79.2 주요 지표**

| **지표** | **의미** |
| --- | --- |
| Maximum Pool Size | 최대 Connection |
| Minimum Idle | 최소 대기 Connection |
| Active | 현재 사용 중 |
| Idle | 사용 가능한 대기 Connection |
| Total | Active + Idle |
| Pending | Connection을 기다리는 Thread |
| Acquire Time | 획득 소요시간 |
| Timeout Count | 획득 실패 횟수 |
| Usage Time | Connection 사용시간 |

## **79.3 정상 상태**

예:

Maximum = 120
Active = 45
Idle = 75
Pending = 0

Connection 여유가 있고 대기 요청이 없습니다.

## **79.4 포화 상태**

Maximum = 120
Active = 120
Idle = 0
Pending = 250

의미:

모든 Connection 사용 중

250개 Thread가 Connection을 기다림

원인 후보:

-   Slow SQL
-   장기 트랜잭션
-   Connection 누수
-   Lock Wait
-   트래픽 급증
-   Pool 크기 부족
-   DB 장애

## **79.5 Pool 크기를 늘리면 해결될까요?**

일부 경우에는 도움이 됩니다.

하지만 DB가 이미 포화 상태라면 더 많은 Connection은 부하를 악화시킬 수 있습니다.

Pool 120
→ 240으로 증가

DB
동시 SQL 두 배

결과
CPU·I/O·Lock 증가

Pool 크기는 애플리케이션 서버만의 설정이 아닙니다.

DB 전체 Connection 허용량과 동시 SQL 처리능력을 함께 고려합니다.

## **79.6 Pending이 증가할 때**

진단 순서:

Active가 Maximum에 도달했는가?

Connection 사용시간이 긴가?

Slow SQL이 있는가?

DB Lock이 있는가?

트랜잭션 범위가 긴가?

외부 호출 중 Connection을 보유하는가?

Connection이 반환되지 않는가?

## **79.7 Connection 누수**

Connection을 반환하지 않으면 Pool이 점차 고갈됩니다.

Spring·MyBatis의 정상적인 Transaction 관리에서는 자동 반환되지만 다음을 주의합니다.

-   수동 JDBC 자원 미정리
-   비정상 Thread 종료
-   장시간 Streaming Cursor
-   Transaction 종료 누락
-   외부 호출 중 Connection 보유

Hikari Leak Detection을 제한적으로 사용할 수 있습니다.

운영환경에서 너무 낮은 기준은 정상 장기 SQL도 누수로 오탐할 수 있습니다.

## **79.8 Connection Timeout**

connectionTimeout
\= Pool에서 Connection을 얻기 위해 기다리는 최대시간

SQL 실행 Timeout과 다릅니다.

예:

Connection 획득 대기 3초 초과
→ Hikari Timeout

Connection 획득 성공
→ SQL 실행 5초
→ Query Timeout

오류코드와 로그를 구분해야 합니다.

## **79.9 Transaction과 Pool**

다음 구조는 Connection을 장시간 점유할 수 있습니다.

트랜잭션 시작
→ DB 조회
→ 외부 API 10초 대기
→ DB Update
→ Commit

외부 API 대기 중에도 Connection을 보유할 수 있습니다.

가능하면 외부 호출과 DB 트랜잭션 경계를 분리합니다.

## **79.10 WAR별 Pool**

여러 WAR가 각자 DataSource를 가지면 WAR별 Pool을 관측해야 합니다.

sv-service Pool
ic-service Pool
cm-service Pool

특정 WAR가 Connection을 과도하게 사용해 DB 전체를 압박할 수 있습니다.

## **79.11 Pool 합계**

예:

17개 WAR
각 Maximum 120

이론상 최대
2,040 Connection

DB 허용 Connection보다 클 수 있습니다.

모든 WAR의 Pool 최대값 합계와 서버 인스턴스 수를 함께 계산해야 합니다.

## **79.12 Pool 상태 화면**

OM 예:

| **WAR** | **Max** | **Active** | **Idle** | **Pending** | **Avg Acquire** |
| --- | --- | --- | --- | --- | --- |
| SV | 120 | 115 | 5 | 32 | 850ms |
| IC | 100 | 30 | 70 | 0 | 5ms |
| CM | 80 | 21 | 59 | 0 | 4ms |

이 경우 SV Pool과 SV SQL을 우선 확인합니다.

## **제79장 요약**

학습 목표 | 79장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

Active는 사용 중인 Connection이고,
Pending은 Connection을 기다리는 Thread다.

Pending 증가 원인은
Pool 크기뿐 아니라 Slow SQL·Lock·긴 트랜잭션일 수 있다.

모든 WAR의 Pool 합계를 DB 용량과 함께 검토해야 한다.

# **제80장. Slow SQL·Lock·Mapper 분석**

학습 목표 | 80장. Slow SQL·Lock·Mapper 분석의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 데이터 일관성: 화면 동작보다 데이터 소유권, 상태 전이, 동시성, 트랜잭션 경계를 먼저 결정해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **80.1 SQL과 ServiceId 연결**

운영자는 다음 연결을 볼 수 있어야 합니다.

ServiceId
SV.Customer.selectSummary

Mapper
SvCustomerMapper.selectCustomerSummary

SQL ID
SV-CUST-SEL-001

평균시간
2,850ms

p95
4,200ms

호출건수
10,000건

## **80.2 Slow SQL 기준**

예시 기준:

300ms 미만
→ 일반

300~1,000ms
→ 관찰

1,000ms 초과
→ Slow SQL 후보

3,000ms 초과
→ 온라인 거래 위험

모든 SQL에 동일 기준을 적용하지 않습니다.

단건 Key 조회와 복잡한 분석 조회는 기준이 다를 수 있습니다.

## **80.3 SQL 로그 항목**

| **항목** | **설명** |
| --- | --- |
| TraceId | 거래 연결 |
| ServiceId | 호출 기능 |
| Mapper ID | SQL 식별 |
| SQL ID | 표준 SQL 코드 |
| Start Dtm | 시작 |
| Elapsed | 수행시간 |
| Row Count | 조회·영향 건수 |
| Result | 성공·실패 |
| DB Error | 오류코드 |
| Timeout | 제한시간 |

Parameter 원문은 개인정보와 보안을 고려해 마스킹합니다.

## **80.4 느린 SQL 원인**

인덱스 없음

조건 없는 대량조회

선행 Wildcard 검색

함수 적용으로 인덱스 미사용

잘못된 Join 순서

통계정보 부정확

Sort·Hash 과다

대량 OFFSET

Lock 대기

DB I/O 부족

잘못된 실행계획

## **80.5 조회 건수**

SQL이 빠르더라도 너무 많은 데이터를 반환하면 WAS가 느려질 수 있습니다.

DB SQL
500ms

반환 행
100만 건

JSON 변환
4초

응답 크기
200MB

SQL 수행시간뿐 아니라 Row Count와 응답 직렬화 시간을 함께 확인합니다.

## **80.6 DB Lock**

변경 거래는 다른 트랜잭션의 Lock을 기다릴 수 있습니다.

사용자 A
고객 데이터 Update
Commit 지연

사용자 B
같은 데이터 Update
Lock 대기

OM에서 다음을 확인할 수 있습니다.

-   대기 세션
-   차단 세션
-   Lock 대상 테이블
-   SQL ID
-   대기시간
-   사용자·ServiceId
-   트랜잭션 시작시각

## **80.7 Lock의 원인**

긴 트랜잭션

사용자 입력을 기다리는 트랜잭션

외부 호출 포함

대량 Update

Commit 누락

Batch와 온라인 동시 변경

잘못된 처리순서

## **80.8 Deadlock**

DB가 순환 Lock을 감지하면 하나의 트랜잭션을 실패시킬 수 있습니다.

처리:

Deadlock 오류 수집

관련 SQL과 테이블 확인

Update 순서 통일

트랜잭션 범위 축소

재시도 가능성 검토

변경 거래 자동 재시도는 멱등성과 전체 시간 예산을 확인한 뒤 제한적으로 적용합니다.

## **80.9 실행계획**

DBA와 다음을 확인합니다.

Full Table Scan

Index Range Scan

Join Method

Estimated Rows

Actual Rows

Sort Cost

Partition Pruning

실행계획만 보고 판단하지 않고 실제 처리건수와 데이터 분포를 함께 확인합니다.

## **80.10 SQL 튜닝 우선순위**

1\. 불필요한 조회 제거

2\. 검색조건·조회기간 제한

3\. 필요한 컬럼만 조회

4\. 적절한 인덱스

5\. Join·실행계획 개선

6\. 페이징·분할

7\. Cache·Read Model 검토

8\. 서버 증설

인프라 증설보다 SQL과 조회범위 개선이 먼저일 수 있습니다.

## **80.11 SQL Timeout 이후**

SQL Timeout이 발생해도 DB에서 작업이 즉시 종료되지 않는 경우를 확인해야 합니다.

필요한 정보:

-   JDBC Statement 취소 여부
-   DB 세션 상태
-   Rollback 완료 여부
-   Connection 반환 여부
-   장기 Query 잔존 여부

## **제80장 요약**

학습 목표 | 80장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

Slow SQL은 Mapper ID와 ServiceId에 연결해야 한다.

SQL 시간뿐 아니라
조회 건수·Lock·직렬화 시간도 확인한다.

튜닝은 조회범위와 SQL 구조 개선을 우선하고,
증설은 이후에 검토한다.

# **제81장. 업무 WAR별 자원과 장애 격리**

학습 목표 | 81장. 업무 WAR별 자원과 장애 격리의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 실패를 설계하는 관점: 오류를 사용자 입력, 업무 규칙, 의존 시스템, 인프라 문제로 구분해야 복구 책임과 메시지가 선명해집니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **81.1 다중 WAR 구조**

하나의 Tomcat에 여러 업무 WAR가 배포될 수 있습니다.

Tomcat
├─ sv-service
├─ ic-service
├─ cm-service
├─ pd-service
└─ om-service

프로세스는 하나이지만 애플리케이션은 여러 개입니다.

## **81.2 공유하는 자원**

같은 Tomcat에 배포된 WAR는 다음 자원을 공유할 수 있습니다.

JVM CPU

Heap

GC

Tomcat Connector Thread

OS File Descriptor

Network

프로세스 장애 영향

WAR별로 DataSource가 분리되어도 JVM과 Tomcat은 공유됩니다.

## **81.3 특정 WAR 자원 독점**

예:

CM 대량조회
→ Heap 10GB 사용
→ GC 증가
→ SV·IC 거래도 지연

또는:

SV 요청 급증
→ Tomcat Thread 대부분 점유
→ 다른 WAR 신규 요청 대기

## **81.4 WAR별 지표**

| **지표** | **필요성** |
| --- | --- |
| 요청 건수 | 트래픽 |
| Busy Request | 동시 처리 |
| 평균·p95 | 응답시간 |
| 오류율 | 안정성 |
| Timeout율 | 지연 |
| DB Pool | DB 자원 |
| Cache Entry | 메모리 |
| 파일 처리 | I/O |
| 외부 호출 | 연계 부하 |
| Thread 사용 추정 | 점유도 |

JVM CPU와 Heap은 프로세스 공유이므로 WAR별 정확한 분리가 어렵습니다.

ServiceId 실행시간과 객체·Thread 사용량으로 간접 분석할 수 있습니다.

## **81.5 WAR별 Thread 격리**

모든 WAR가 하나의 Connector Thread Pool을 공유하면 특정 업무가 독점할 수 있습니다.

대안:

-   Tomcat 인스턴스 분리
-   업무그룹별 Port·Connector
-   Gateway Rate Limit
-   ServiceId 동시성 제한
-   외부 Client Bulkhead
-   대량 업무 비동기 전환

## **81.6 업무그룹 분리**

예:

업무그룹 A
SV·IC·PC·MS

업무그룹 B
CM·PD·EB·EP

트래픽·장애 특성에 따라 Tomcat과 VM을 분리하면 격리가 강화됩니다.

## **81.7 분리 판단 기준**

트래픽 규모 차이

장애 영향도

배포주기

Heap 사용량

DB Pool 특성

외부연계 의존성

대량 파일·Batch

보안등급

운영 담당조직

## **81.8 하나의 Tomcat 다중 WAR 장점**

-   서버 수 감소
-   구성 단순
-   공통 운영
-   자원 활용도 향상
-   초기 구축비용 감소

## **81.9 단점**

-   Tomcat 재기동 시 전체 WAR 영향
-   JVM 장애 공유
-   GC 영향 공유
-   Thread 독점 가능
-   독립 배포 제약
-   장애 원인 분리 어려움

## **81.10 장애 격리 수준**

ServiceId 통제
→ 기능 단위

Circuit Breaker
→ 연계 단위

Thread Pool 분리
→ 자원 단위

WAR 분리
→ 애플리케이션 단위

Tomcat 분리
→ 프로세스 단위

VM 분리
→ OS·자원 단위

중요도와 비용에 맞게 격리 수준을 결정합니다.

## **제81장 요약**

학습 목표 | 81장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

같은 Tomcat의 여러 WAR는
CPU·Heap·GC·Connector Thread를 공유한다.

특정 WAR의 대량 작업이
다른 WAR에 영향을 줄 수 있다.

ServiceId 통제부터 VM 분리까지
필요한 장애 격리 수준을 선택해야 한다.

# **제82장. Metric·Log·Trace와 OM 대시보드**

학습 목표 | 82장. Metric·Log·Trace와 OM 대시보드의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **82.1 세 가지 관측 정보**

### **Metric**

현재 오류율은 몇 %인가?

p95는 몇 초인가?

Busy Thread는 몇 개인가?

### **Log**

왜 실패했는가?

어느 예외가 발생했는가?

어떤 설정이 적용되었는가?

### **Trace**

요청이 어느 시스템을 거쳤는가?

어느 구간이 느렸는가?

## **82.2 Metric 종류**

### **Counter**

계속 증가하는 누적값입니다.

요청 건수

오류 건수

Timeout 건수

### **Gauge**

현재 상태값입니다.

Busy Thread

Heap Used

DB Active

Queue Depth

### **Histogram**

값의 분포를 측정합니다.

응답시간

SQL 수행시간

Connection 획득시간

## **82.3 p95 이해하기**

요청 100건의 응답시간을 빠른 순서로 정렬했을 때 95번째 값입니다.

p95 = 2.8초

의미:

100건 중 약 95건은
2.8초 안에 끝남

나머지 약 5건은 더 오래 걸릴 수 있습니다.

## **82.4 평균의 한계**

99건
100ms

1건
30초

평균은 약 399ms입니다.

평균만 보면 빨라 보이지만 한 사용자는 30초를 기다렸습니다.

따라서 평균과 p95·p99를 함께 봅니다.

## **82.5 최소 OM 대시보드**

### **시스템 요약**

전체 TPS

성공률

시스템 오류율

Timeout율

전체 p95

운영 중지 ServiceId

### **WAR 상태**

WAR별 요청량

WAR별 p95

WAR별 오류율

DB Pool

Cache 상태

### **JVM·Tomcat**

CPU

Heap

GC Pause

Busy Thread

Queue

### **Slow Top N**

느린 ServiceId Top 10

Slow SQL Top 10

오류코드 Top 10

외부 Timeout Top 10

## **82.6 Prometheus 없이 구현하기**

초기에는 애플리케이션 내부 수집기와 OM REST API를 사용할 수 있습니다.

각 업무 WAR
→ Runtime Metrics Collector
→ 내부 관리 Endpoint

tcf-om
→ 주기적 수집
→ 최근 상태 저장
→ 대시보드 표시

수집 대상:

-   Tomcat Thread
-   JVM Memory
-   GC
-   Hikari Pool
-   ServiceId 통계
-   Slow SQL 통계

## **82.7 단순 수집 구조 주의**

tcf-om이 모든 WAR를 너무 자주 호출하면 자체 부하가 발생할 수 있습니다.

예:

WAR 17개
서버 8대
1초마다 수집
여러 지표 Endpoint

대안:

-   10~30초 주기
-   한 번의 Snapshot API
-   변경분·집계값 수집
-   Timeout 짧게 적용
-   수집 실패가 업무에 영향 없도록 분리
-   최근 정상값 표시

## **82.8 Metric 저장**

단순 OM은 최근 값과 짧은 이력부터 시작할 수 있습니다.

현재값
최근 1시간
최근 24시간 집계

장기 추세와 고해상도 분석이 필요해지면 Prometheus 같은 전문 시계열 저장소를 연계할 수 있습니다.

## **82.9 Label 주의**

다음 값을 Metric Label로 사용하면 안 됩니다.

customerNo

userId

TraceId

전체 URL Parameter

값 종류가 너무 많아 저장공간과 성능이 악화됩니다.

권장 Label:

businessCode

serviceId

resultType

warName

serverId

ServiceId 수도 관리 가능한 범위인지 확인합니다.

## **82.10 경보와 대시보드 차이**

대시보드는 사람이 봅니다.

경보는 사람이 보지 않아도 이상을 알려야 합니다.

대시보드
→ 현재 상태 탐색

경보
→ 즉시 조치가 필요한 이상 알림

모든 지표에 경보를 설정하지 않습니다.

## **제82장 요약**

학습 목표 | 82장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

Metric은 수치,
Log는 상세 사건,
Trace는 요청 경로를 보여준다.

OM 최소 대시보드는
ServiceId·Thread·JVM·DB Pool·SQL을 연결해야 한다.

간단 수집으로 시작하고
장기 추세가 필요하면 전문 관측 도구와 연계할 수 있다.

# **제83장. 경보와 운영 임계값**

학습 목표 | 83장. 경보와 운영 임계값의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **83.1 임계값만으로 경보하면 안 된다**

CPU가 80%를 1초 동안 기록했다고 즉시 장애 경보를 보내면 오탐이 많습니다.

경보에는 다음 요소가 필요합니다.

임계값

지속시간

발생 횟수

영향범위

심각도

자동 복구 여부

## **83.2 경보 예**

| **지표** | **예시 조건** | **심각도** |
| --- | --- | --- |
| 시스템 오류율 | 5분간 1% 초과 | Critical |
| Timeout율 | 5분간 0.5% 초과 | Critical |
| p95 | 10분간 3초 초과 | Warning |
| Busy Thread | 5분간 70% 초과 | Warning |
| Busy Thread | 3분간 90% 초과 | Critical |
| DB Pending | 1분 이상 발생 | Warning |
| DB Pending | 지속 증가 | Critical |
| Heap | GC 후 80% 이상 지속 | Warning |
| Full GC | 반복 발생 | Critical |
| Disk | 85% 초과 | Warning |
| Process Down | 즉시 | Critical |

수치는 예시이며 성능시험과 운영경험으로 확정합니다.

## **83.3 증상 경보와 원인 경보**

### **증상 경보**

p95 증가

Timeout 증가

오류율 증가

사용자 영향과 직접 연결됩니다.

### **원인 후보 경보**

DB Pending 증가

GC Pause 증가

Slow SQL 증가

원인 분석에 도움을 줍니다.

운영 우선순위는 사용자 영향 경보를 중심으로 구성합니다.

## **83.4 정적 임계값의 한계**

업무시간과 새벽시간의 정상값은 다를 수 있습니다.

오전 9시
TPS 1,000 정상

새벽 3시
TPS 1,000 비정상

장기적으로 시간대별 Baseline과 이상탐지를 적용할 수 있습니다.

## **83.5 경보 폭주**

DB 장애 하나로 다음 경보가 동시에 발생할 수 있습니다.

DB Connection 실패

DB Pending

ServiceId Timeout

오류율 증가

Thread 증가

외부 Gateway 오류

같은 원인의 경보를 묶어 Incident 하나로 관리할 수 있습니다.

## **83.6 경보 억제**

점검시간이나 승인된 Batch 실행 중 예상 가능한 경보를 제한할 수 있습니다.

주의:

-   억제 시작·종료시각
-   대상 시스템
-   승인자
-   억제 사유
-   중요 경보 예외
-   자동 해제

경보를 끈 채 복구하지 않는 상황을 방지합니다.

## **83.7 알림 채널**

-   운영 화면
-   SMS
-   메신저
-   이메일
-   장애관리 시스템
-   당직 호출

심각도에 따라 채널을 다르게 사용합니다.

## **83.8 경보 메시지**

나쁜 예:

오류 발생

좋은 예:

\[Critical\]
SV.Customer.selectSummary
최근 5분 Timeout율 3.2%
영향 서버 sv-was-02
DB Pool Pending 84
최초 발생 10:31

운영자가 첫 행동을 판단할 수 있어야 합니다.

## **제83장 요약**

학습 목표 | 83장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

경보는 임계값뿐 아니라
지속시간·영향범위·심각도를 함께 정의한다.

사용자 영향 경보와 원인 후보 경보를 구분한다.

경보 폭주와 점검시간 억제도 운영정책으로 관리한다.

# **제84장. 장애 진단 Runbook**

학습 목표 | 84장. 장애 진단 Runbook의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 실패를 설계하는 관점: 오류를 사용자 입력, 업무 규칙, 의존 시스템, 인프라 문제로 구분해야 복구 책임과 메시지가 선명해집니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **84.1 장애가 발생했을 때 첫 행동**

잘못된 첫 행동:

Tomcat 재시작

Thread 수 증가

Timeout 증가

DB Pool 증가

올바른 첫 행동:

영향범위 확인

증적 확보

원인 후보 축소

장애 확산 방지

## **84.2 1단계: 영향범위 확인**

전체 사용자인가?

특정 지점인가?

특정 업무코드인가?

특정 ServiceId인가?

조회와 변경 중 어느 쪽인가?

모든 서버인가, 한 서버인가?

언제부터 발생했는가?

## **84.3 2단계: 사용자 영향 확인**

호출 건수

성공률

시스템 오류율

Timeout율

p95·p99

미처리 거래

장애의 심각도를 판단합니다.

## **84.4 3단계: Trace 확보**

대표 실패 거래의 다음 정보를 확보합니다.

GUID

TraceId

ServiceId

거래코드

서버 ID

처리시간

오류코드

## **84.5 4단계: 자원 상태 확인**

Tomcat Busy Thread

Queue

JVM CPU

Heap

GC Pause

DB Pool Active·Pending

Slow SQL

외부 호출 상태

## **84.6 5단계: 최근 변경 확인**

최근 WAR 배포

환경설정 변경

Timeout 변경

DB Script

인덱스 변경

통계정보 변경

거래통제 변경

외부기관 변경

Batch 실행

장애 시작시각과 변경시각을 비교합니다.

## **84.7 증상별 Runbook**

### **상황 A: 모든 거래가 느리다**

확인:

JVM CPU·GC

Tomcat Busy Thread

전체 DB 상태

네트워크

최근 공통 배포

공통 인증·Gateway

### **상황 B: 특정 ServiceId만 느리다**

확인:

해당 Mapper·SQL

외부 Client

처리건수

최근 업무 변경

ServiceId Timeout

Cache Miss 급증

### **상황 C: DB Pool Pending 증가**

확인:

Active = Max인가?

Connection 사용시간

Slow SQL

Lock Wait

긴 트랜잭션

Connection 누수

### **상황 D: CPU 높음**

확인:

CPU 사용 Thread

GC 비율

대량 계산

무한 Loop

압축·암호화

로그 폭증

### **상황 E: Heap 지속 증가**

확인:

Cache Entry 증가

대용량 List

파일 byte\[\]

ThreadLocal

ClassLoader

비동기 Queue 적체

## **84.8 장애 완화**

원인이 완전히 해결되지 않아도 영향을 줄일 수 있습니다.

문제 ServiceId SUSPENDED

대량조회 차단

Rate Limit

외부 Circuit Open

Batch 중지

트래픽 우회

신규 서버 투입

이전 WAR Rollback

## **84.9 거래통제 기준**

특정 ServiceId를 중지할 때 확인합니다.

대체 기능이 있는가?

조회도 중지할 것인가?

변경만 중지할 것인가?

기존 진행 거래는 어떻게 되는가?

사용자 메시지는 무엇인가?

자동 해제시각이 있는가?

## **84.10 서버 재시작 전 증적**

가능하면 다음을 확보합니다.

-   Thread Dump
-   GC Log
-   JVM 상태
-   Heap 사용량
-   거래로그
-   Slow SQL
-   DB Session·Lock
-   최근 배포정보
-   오류로그

OOM 등 즉시 복구가 필요한 경우 증적 확보와 복구 우선순위를 운영정책에 따라 결정합니다.

## **84.11 복구 확인**

Health 정상

대표 ServiceId 성공

오류율 정상화

p95 정상화

DB Pending 0

Busy Thread 안정

GC Pause 정상

미확정 거래 확인

## **84.12 점진적 복구**

거래통제 일부 해제

소량 트래픽 허용

지표 확인

전체 트래픽 확대

한 번에 전체 통제를 해제하면 장애가 재발할 수 있습니다.

## **84.13 미확정 거래**

Timeout이나 서버 중단 후 다음 상태를 확인합니다.

PROCESSING

TIMEOUT

UNKNOWN

응답 실패·DB 성공

외부 성공·내부 실패

Idempotency Key와 업무 데이터를 대조하여 재처리 여부를 판단합니다.

## **84.14 장애 보고**

| **항목** | **내용** |
| --- | --- |
| 장애번호 | Incident ID |
| 시작·종료시각 | 정확한 시각 |
| 영향범위 | 사용자·ServiceId |
| 증상 | 오류율·지연 |
| 직접 원인 | SQL·Pool·배포 |
| 근본 원인 | 설계·운영 Gap |
| 임시조치 | 통제·Rollback |
| 복구조치 | 튜닝·설정 |
| 미확정 거래 | 건수·처리 |
| 재발방지 | Action Item |
| 담당자 | 책임자 |
| 완료기한 | Due Date |

## **제84장 요약**

학습 목표 | 84장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

장애 시 서버 재시작부터 하지 않는다.

영향범위·Trace·Thread·JVM·DB Pool·SQL 순서로
원인 후보를 좁힌다.

복구 후에는 미확정 거래와
재발방지 과제까지 확인해야 한다.

# **제85장. 운영조치·감사·변경관리**

학습 목표 | 85장. 운영조치·감사·변경관리의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **85.1 운영조치도 업무 거래다**

다음 작업은 시스템에 큰 영향을 줍니다.

ServiceId 중지

Timeout 변경

Batch 중지

Cache 초기화

사용자 권한 변경

배포 Rollback

DB Session 종료

따라서 단순한 관리자 버튼이 아니라 통제된 운영 거래로 설계합니다.

## **85.2 운영조치 기본정보**

| **항목** | **설명** |
| --- | --- |
| Operation ID | 작업 식별자 |
| 대상 | ServiceId·WAR·Job |
| 작업유형 | 중지·변경·재시작 |
| 변경 전 | 이전 상태 |
| 변경 후 | 신규 상태 |
| 작업사유 | 장애·점검 |
| 요청자 | 운영자 |
| 승인자 | 필요 시 |
| 적용시각 | 실제 반영 |
| 종료시각 | 원복·해제 |
| Incident ID | 장애 연결 |
| 결과 | 성공·실패 |

## **85.3 중요 작업 승인**

예:

일반 조회
→ 승인 불필요 가능

ServiceId 중지
→ 운영 승인

대량 데이터 재처리
→ 업무 승인

개인정보 파일 다운로드
→ 관리자 승인

Private Key 교체
→ 보안 승인

## **85.4 긴급 변경**

장애 중에는 사전 승인을 기다리기 어려울 수 있습니다.

긴급절차:

긴급 작업자 권한 확인

작업 사유 기록

최소 범위 변경

즉시 결과 확인

사후 승인

원복·정식 변경계획

긴급변경이 일상적인 우회 절차가 되어서는 안 됩니다.

## **85.5 Timeout 변경 감사**

기존
3000ms

변경
10000ms

사유
장애 임시 완화

유효기간
2시간

자동 원복
3000ms

임시 변경에는 종료일시와 자동 원복이 필요합니다.

## **85.6 Cache 초기화**

Cache 전체 초기화는 원본 DB에 순간 부하를 줄 수 있습니다.

전체 Cache 삭제
→ 모든 요청 Cache Miss
→ DB 부하 급증

가능하면 대상 Key만 무효화하거나 순차 갱신합니다.

## **85.7 DB Session 종료**

DB Lock을 해소하기 위해 Session을 강제 종료할 수 있습니다.

확인:

-   어떤 ServiceId인가?
-   트랜잭션이 무엇을 변경했는가?
-   Rollback 시간은 얼마나 걸리는가?
-   다른 세션 영향은 없는가?
-   업무 담당자 승인이 필요한가?

## **85.8 운영 직접 DB 수정**

원칙적으로 애플리케이션 ServiceId 또는 승인된 Script를 사용합니다.

긴급 직접 수정 시:

영향분석

Backup

승인

검증 SQL

수정 SQL

Commit 단위

결과 확인

감사기록

재발방지

## **85.9 변경 호환성**

OM 정책구조가 변경되면 모든 업무 WAR가 영향을 받을 수 있습니다.

예:

기존 상태
ACTIVE·SUSPENDED

신규 상태
READ\_ONLY 추가

구 버전 WAR가 신규 상태를 어떻게 처리하는지 확인해야 합니다.

## **85.10 폐기 관리**

ServiceId 폐기:

ACTIVE
→ DEPRECATED
→ DISABLED
→ REMOVED

OM 기준정보, 코드, 테스트, 화면, 문서, 경보를 함께 제거합니다.

## **제85장 요약**

학습 목표 | 85장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

운영조치도 시스템 상태를 변경하는 중요 거래다.

작업자·승인자·사유·변경 전후를 기록해야 한다.

임시 Timeout과 거래통제에는
종료일시와 자동 원복을 적용한다.

# **3\. 목표 아키텍처**

\[사용자·화면\]
│
▼
\[Gateway·Apache\]
│
▼
\[업무 WAR / TCF\]
├─ STF
├─ Handler
├─ Facade
├─ Service
├─ DAO·Mapper
└─ ETF
│
├──────────────────────────────┐
│ │
▼ ▼
\[Transaction Metrics\] \[Runtime Metrics\]
ServiceId Tomcat Thread
Result JVM CPU·Heap·GC
Elapsed Hikari Pool
Error Code Cache
SQL Time External Client
│ │
└──────────────┬───────────────┘
▼
\[tcf-om Collector\]
│
┌───────────┼────────────┐
▼ ▼ ▼
\[대시보드\] \[거래 조회\] \[운영 경보\]
│ │ │
└───────────┼────────────┘
▼
\[운영 Control\]
ServiceId·Timeout·Batch
│
▼
\[감사로그\]

# **4\. 표준 형식**

## **4.1 Runtime Snapshot**

{
"serverId": "sv-was-01",
"warName": "sv-service",
"collectedDtm": "2026-07-17T10:30:00+09:00",
"tomcat": {
"maxThreads": 1200,
"busyThreads": 560,
"queueLength": 0
},
"jvm": {
"processCpuRate": 0.48,
"heapUsedBytes": 21474836480,
"heapMaxBytes": 51539607552,
"gcPauseMaxMs": 85
},
"dbPool": {
"maximum": 120,
"active": 72,
"idle": 48,
"pending": 0
}
}

## **4.2 ServiceId Metric**

{
"serviceId": "SV.Customer.selectSummary",
"warName": "sv-service",
"window": "5m",
"requestCount": 12500,
"successRate": 0.992,
"businessErrorRate": 0.005,
"systemErrorRate": 0.002,
"timeoutRate": 0.001,
"averageMs": 420,
"p95Ms": 1180,
"p99Ms": 2200
}

## **4.3 Slow SQL Metric**

{
"serviceId": "SV.Customer.selectSummary",
"mapperId": "SvCustomerMapper.selectCustomerSummary",
"sqlId": "SV-CUST-SEL-001",
"executionCount": 4200,
"averageMs": 850,
"p95Ms": 2800,
"maxMs": 5600,
"averageRowCount": 1
}

## **4.4 운영조치 요청**

{
"operationType": "SERVICE\_SUSPEND",
"targetServiceId": "SV.Customer.selectSummary",
"reason": "DB 부하 확산 방지",
"incidentId": "INC20260717001",
"effectiveFrom": "2026-07-17T10:40:00+09:00",
"effectiveTo": "2026-07-17T11:10:00+09:00",
"autoRestore": true
}

# **5\. 구성요소 및 속성**

| **구성요소** | **주요 속성** |
| --- | --- |
| Service Catalog | ServiceId, WAR, Timeout |
| Transaction Log | 결과, 처리시간, Trace |
| Runtime Collector | Thread, JVM, Pool |
| SQL Collector | Mapper, 시간, Row |
| OM Aggregator | 집계 Window |
| Dashboard | 상태·Top N |
| Alert Engine | 임계값·지속시간 |
| Control Service | 중지·변경 |
| Audit Log | 작업자·승인자 |
| Incident Registry | 장애정보 |
| Runbook | 진단·복구절차 |
| Retention Policy | 로그 보관기간 |

# **6\. 책임 경계와 RACI**

| **활동** | **AA** | **FW** | **DEV** | **TA** | **DBA** | **OM** | **SEC** | **QA** | **OPS** |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| OM 구조 설계 | A | R | C | C | C | C | C | C | I |
| Service Catalog | A/C | C | R | I | I | R | C | C | I |
| 거래 Metric | C | A/R | C | C | I | C | I | C | C |
| Tomcat·JVM 수집 | C | C | I | A/R | I | C | I | C | R/C |
| DB Pool 수집 | C | C | C | C | A/C | C | I | C | R |
| SQL 진단 | C | C | R | C | A/R | C | I | C | C |
| 경보 기준 | A | C | C | R | C | C | C | C | R |
| 거래통제 | A/C | C | I | C | I | R | C | C | A |
| 보안감사 | C | C | I | I | I | C | A/R | C | C |
| 장애 대응 | C | C | C | R | R | C | C | C | A |
| 재발방지 | A | R/C | R | R | R | C | C | C | C |

# **7\. 정상 처리 흐름**

## **7.1 거래 관측**

1\. STF가 GUID·TraceId 생성

2\. 거래로그 PROCESSING 시작

3\. ServiceId Metric 요청 건수 증가

4\. Handler 이하 업무 실행

5\. Mapper 수행시간 기록

6\. DB Connection 사용시간 기록

7\. ETF가 종료상태 확정

8\. 응답시간 Histogram 기록

9\. OM이 Window 단위 집계

10\. 대시보드와 경보에 반영

## **7.2 Runtime 수집**

1\. 업무 WAR가 Runtime Snapshot 생성

2\. Thread·JVM·Pool 상태 수집

3\. tcf-om이 짧은 Timeout으로 조회

4\. 최근 정상값 저장

5\. 서버별·WAR별 상태 집계

6\. 임계값 판단

7\. 이상 시 경보

# **8\. 오류·Timeout·장애 흐름**

## **8.1 수집 Endpoint 실패**

OM 수집 실패
→ 업무 요청에는 영향 없음
→ 마지막 정상값 표시
→ 수집 실패 경보

## **8.2 거래로그 기록 실패**

업무 처리 성공
→ 거래로그 저장 실패

감사상 필수 여부에 따라 다음을 선택합니다.

필수 로그
→ 거래 실패·Rollback 검토

운영 상세로그
→ 업무 성공 유지
→ 별도 경보·재기록

## **8.3 경보 Engine 장애**

Metric 수집 정상
→ 경보 발송 실패
→ 경보 Queue·재시도
→ 운영자에게 자체 장애 알림

## **8.4 OM DB 장애**

업무 WAR
→ 마지막 정책 Cache 사용

OM 변경
→ 일시 중단

운영 대시보드
→ 제한 상태

## **8.5 Runtime 지표 수집 부하**

수집주기 과도
→ 업무 WAR 관리 Endpoint 부하
→ 수집주기 완화
→ Snapshot 통합

# **9\. 정상 예시**

사용자 증상
고객조회 느림

OM 확인
SV.Customer.selectSummary p95 4.5초

Thread
Busy 45%
Queue 0

JVM
CPU 38%
GC 정상

DB Pool
Active 120/120
Pending 84

Slow SQL
SvCustomerMapper.selectCustomerSummary
p95 4.1초

DB
Lock Wait 없음
Full Scan 확인

판단
SQL 성능으로 Connection 장기 점유

조치
문제 조회조건 제한
인덱스 검토
ServiceId 임시 Rate Limit

# **10\. 금지 예시**

## **10.1 CPU만 보고 장애 판단**

CPU 90%
→ 무조건 서버 증설

## **10.2 Thread만 증가**

Slow SQL 해결 없이
maxThreads 1200 → 2400

## **10.3 DB Pool만 증가**

DB 포화 확인 없이
Pool 120 → 300

## **10.4 장애 시 즉시 재시작**

Thread Dump와 SQL 상태를 확인하지 않고 Tomcat을 재시작합니다.

## **10.5 모든 로그 원문 저장**

고객정보·JWT·대용량 전문을 무조건 기록합니다.

## **10.6 모든 지표 1초 수집**

OM 수집 자체가 업무 서버에 부하를 발생시킵니다.

## **10.7 경보 무기한 억제**

점검 종료 후에도 경보가 복구되지 않습니다.

# **11\. 연계 규칙**

업무 WAR
→ Runtime Snapshot API

업무 WAR
→ 거래·SQL Metric

tcf-om
→ 서버별 수집

OM Control
→ 정책변경 Event

정책 Event
→ 업무 WAR Cache 무효화

장애관리
→ Incident ID

배포시스템
→ 최근 Release 정보

DB 관리
→ Lock·Session 정보

각 시스템의 식별자는 다음 값으로 연결합니다.

ServiceId
TraceId
Server ID
WAR Name
Release ID
Incident ID
SQL ID

# **12\. 데이터 및 상태관리**

## **12.1 서버 상태**

UP
DEGRADED
DOWN
UNKNOWN
MAINTENANCE

## **12.2 ServiceId 상태**

ACTIVE
SUSPENDED
READ\_ONLY
DISABLED
DEPRECATED

## **12.3 Incident 상태**

OPEN
INVESTIGATING
MITIGATED
RESOLVED
CLOSED

## **12.4 운영조치 상태**

REQUESTED
APPROVED
APPLIED
FAILED
RESTORED

# **13\. 성능·용량·확장성**

| **영역** | **기준** |
| --- | --- |
| 수집주기 | 지표 중요도별 차등 |
| Snapshot | 한 번에 묶어 수집 |
| Metric 저장 | 집계 Window |
| Log 저장 | 유형별 보관기간 |
| Trace 저장 | 오류·Slow 거래 중심 |
| OM DB | 조회 인덱스 |
| Dashboard | Top N 제한 |
| 경보 | 지속시간 적용 |
| WAR 수 | 서버·인스턴스 증가 고려 |
| ServiceId 수 | Label 규모 관리 |
| SQL Metric | Sampling·집계 |
| 감사로그 | 별도 보호·보관 |

# **14\. 보안·개인정보·감사**

OM 관리 Endpoint는 내부망과 인증을 적용한다.

운영자는 권한 범위 내 서버와 로그만 조회한다.

거래로그의 개인정보는 마스킹한다.

JWT와 Private Key를 로그에 기록하지 않는다.

Heap Dump와 Thread Dump 접근을 통제한다.

ServiceId 중지·Timeout 변경·재처리를 감사한다.

운영 로그 삭제와 경보 억제도 감사 대상이다.

# **15\. 운영·모니터링·장애 대응**

운영 화면의 핵심 메뉴 예:

1\. 시스템 종합현황

2\. WAR별 상태

3\. ServiceId 성능

4\. 거래 상세조회

5\. Thread 상태

6\. JVM·GC 상태

7\. DB Pool 상태

8\. Slow SQL

9\. 외부연계 상태

10\. Batch·Scheduler

11\. 거래통제

12\. 운영 감사

장애 화면에서는 한 화면에서 다음 정보로 이동할 수 있어야 합니다.

오류율 증가
→ ServiceId
→ 실패 Trace
→ 실행 서버
→ Thread
→ Mapper
→ SQL

# **16\. 자동검증 및 품질 Gate**

| **Gate** | **검증** |
| --- | --- |
| ServiceId | OM Catalog 등록 |
| Timeout | 정책 누락 없음 |
| 권한 | 권한코드 연결 |
| 거래로그 | 시작·종료경로 |
| Trace | GUID·TraceId 존재 |
| Context | ThreadLocal 정리 |
| Metric | ServiceId 기본지표 |
| SQL | Mapper ID·수행시간 |
| Runtime | Thread·JVM·Pool Endpoint |
| 보안 | 관리 Endpoint 인증 |
| 로그 | 개인정보 마스킹 |
| 경보 | 핵심 ServiceId 기준 |
| Control | 승인·감사 |
| Runbook | 장애유형별 절차 |

# **17\. 테스트 시나리오**

| **ID** | **시나리오** | **기대 결과** |
| --- | --- | --- |
| OM-001 | 정상 거래 | 거래로그 SUCCESS |
| OM-002 | 업무 오류 | BUSINESS\_FAIL |
| OM-003 | 시스템 오류 | SYSTEM\_ERROR |
| OM-004 | Timeout | TIMEOUT |
| OM-005 | 거래통제 | Handler 미실행 |
| OM-006 | 미등록 ServiceId | 정책대로 차단 |
| OM-007 | Trace 검색 | SQL까지 연결 |
| OM-008 | Busy Thread 증가 | 경보 |
| OM-009 | Thread Queue 증가 | 포화 경보 |
| OM-010 | GC Pause 증가 | 성능 경보 |
| OM-011 | Heap 회복 없음 | 누수 경보 |
| OM-012 | DB Active Max | 포화 탐지 |
| OM-013 | DB Pending 증가 | 대기 탐지 |
| OM-014 | Slow SQL | Mapper·ServiceId 연결 |
| OM-015 | DB Lock | 차단·대기 세션 확인 |
| OM-016 | 특정 WAR 요청 급증 | WAR별 표시 |
| OM-017 | 수집 API 실패 | 마지막 값 유지 |
| OM-018 | OM DB 장애 | 정책 Cache 유지 |
| OM-019 | 거래중지 적용 | 전체 서버 반영 |
| OM-020 | 거래중지 자동 해제 | 원상복구 |
| OM-021 | Timeout 임시 변경 | 만료 후 원복 |
| OM-022 | Cache 전체 초기화 | DB 부하 경보 |
| OM-023 | 경보 억제 종료 | 자동 복구 |
| OM-024 | 운영 권한 없음 | 변경 차단 |
| OM-025 | 운영조치 | 감사로그 기록 |
| OM-026 | 미종료 거래 | STALE 탐지 |
| OM-027 | 서버 재시작 | 이전 증적 확보 |
| OM-028 | 장애 복구 | Metric 정상화 |
| OM-029 | 개인정보 로그 | 마스킹 |
| OM-030 | Heap Dump 접근 | 권한 통제 |

# **18\. 제10부 체크리스트**

## **18.1 OM 기준정보**

| **점검 항목** | **확인** |
| --- | --- |
| 모든 ServiceId가 등록되어 있는가? | □ |
| 거래코드와 WAR가 연결되어 있는가? | □ |
| Timeout이 등록되어 있는가? | □ |
| 권한코드가 연결되어 있는가? | □ |
| 감사 대상 여부가 정의되어 있는가? | □ |
| 운영 담당자가 지정되어 있는가? | □ |

## **18.2 거래 추적**

| **점검 항목** | **확인** |
| --- | --- |
| GUID가 존재하는가? | □ |
| TraceId가 존재하는가? | □ |
| 거래 시작·종료가 기록되는가? | □ |
| ServiceId와 Mapper가 연결되는가? | □ |
| SQL 처리시간을 확인할 수 있는가? | □ |
| 미종료 거래를 탐지할 수 있는가? | □ |

## **18.3 Tomcat·Thread**

| **점검 항목** | **확인** |
| --- | --- |
| maxThreads를 확인할 수 있는가? | □ |
| Busy Thread를 확인할 수 있는가? | □ |
| Queue를 확인할 수 있는가? | □ |
| 장기 실행 Thread를 조회할 수 있는가? | □ |
| Thread와 ServiceId를 연결할 수 있는가? | □ |
| Thread Dump 절차가 있는가? | □ |

## **18.4 JVM**

| **점검 항목** | **확인** |
| --- | --- |
| CPU를 모니터링하는가? | □ |
| Heap Used·Max를 확인하는가? | □ |
| GC Count·Pause를 확인하는가? | □ |
| Metaspace를 확인하는가? | □ |
| ThreadLocal이 정리되는가? | □ |
| Heap Dump 보안절차가 있는가? | □ |

## **18.5 DB Pool·SQL**

| **점검 항목** | **확인** |
| --- | --- |
| Active·Idle·Pending을 확인하는가? | □ |
| Connection 획득시간을 측정하는가? | □ |
| WAR별 Pool을 구분하는가? | □ |
| Slow SQL 기준이 있는가? | □ |
| Mapper ID와 ServiceId가 연결되는가? | □ |
| DB Lock을 조회할 수 있는가? | □ |
| 모든 WAR Pool 합계를 검토했는가? | □ |

## **18.6 경보**

| **점검 항목** | **확인** |
| --- | --- |
| 사용자 영향 경보가 있는가? | □ |
| 지속시간이 적용되는가? | □ |
| Warning·Critical이 구분되는가? | □ |
| 경보 폭주를 통제하는가? | □ |
| 점검 억제가 자동 해제되는가? | □ |
| 경보에 ServiceId와 서버가 포함되는가? | □ |

## **18.7 장애대응**

| **점검 항목** | **확인** |
| --- | --- |
| 영향범위부터 확인하는가? | □ |
| 재시작 전 증적을 확보하는가? | □ |
| 거래통제 절차가 있는가? | □ |
| 복구 후 Smoke Test를 수행하는가? | □ |
| 미확정 거래를 확인하는가? | □ |
| 장애 보고와 재발방지를 기록하는가? | □ |

## **18.8 운영감사**

| **점검 항목** | **확인** |
| --- | --- |
| ServiceId 중지를 감사하는가? | □ |
| Timeout 변경을 감사하는가? | □ |
| Batch 재처리를 감사하는가? | □ |
| 경보 억제를 감사하는가? | □ |
| 운영 직접 DB 수정을 감사하는가? | □ |
| 임시 변경이 자동 원복되는가? | □ |

# **19\. 변경·호환성·폐기 관리**

## **19.1 Metric 변경**

Metric 이름이나 Label이 변경되면 Dashboard와 Alert가 영향을 받습니다.

Metric 추가
→ 하위 호환 가능

Metric 이름 변경
→ Dashboard·Alert 동시 변경

Label 의미 변경
→ 집계 왜곡 가능

## **19.2 로그 형식 변경**

구조화 로그 필드 변경 시 다음을 확인합니다.

로그 수집기

검색 Query

대시보드

감사 보고서

장애 Runbook

기존 필드를 즉시 삭제하지 않고 병행기간을 둘 수 있습니다.

## **19.3 임계값 변경**

임계값 변경 전 확인:

최근 Baseline

피크 시간

오탐 건수

미탐 가능성

업무 중요도

성능시험 결과

## **19.4 OM 정책 Schema 변경**

구·신 업무 WAR가 동시에 운영될 수 있으므로 정책 Schema의 하위 호환성을 유지해야 합니다.

필드 추가
→ 기본값 제공

상태 추가
→ 구 버전 처리정책

필드 삭제
→ 병행기간 후 제거

## **19.5 ServiceId 폐기**

호출량 확인

호출 화면·연계 확인

DEPRECATED 표시

신규 호출 차단 안내

DISABLED

코드·OM·경보 제거

# **20\. 시사점**

## **20.1 핵심 아키텍처 판단**

제10부의 핵심 판단은 다음과 같습니다.

거래가 느리다
≠ Thread 부족

Timeout 발생
≠ Timeout 값이 작음

DB Pending 증가
≠ Pool 크기만 부족

CPU 높음
≠ 서버 수만 부족

정확한 진단은 다음 관계를 함께 보는 것입니다.

ServiceId
\+ Thread
\+ JVM
\+ DB Pool
\+ SQL
\+ 최근 변경

## **20.2 주요 위험**

| **위험** | **영향** |
| --- | --- |
| ServiceId 기준정보 누락 | 통제 불가 |
| 거래 종료로그 누락 | 미확정 거래 |
| TraceId 누락 | 장애 추적 불가 |
| Busy Thread만 보고 증설 | DB 장애 악화 |
| Pool 무조건 증가 | DB 포화 |
| CPU만 모니터링 | I/O 대기 장애 누락 |
| WAR별 지표 없음 | 자원 독점 미탐지 |
| 모든 로그 원문 저장 | 개인정보·용량 문제 |
| 경보 지속시간 없음 | 오탐 폭주 |
| 재시작 전 증적 미확보 | 원인 소실 |
| 임시 설정 자동 원복 없음 | 장기 위험 |
| OM 장애가 업무 장애로 전파 | 전체 서비스 영향 |

## **20.3 우선 보완 과제**

1\. Service Catalog 완성

2\. 거래로그 시작·종료 표준화

3\. GUID·TraceId 전파

4\. ServiceId별 p95·오류율 수집

5\. Tomcat Thread Snapshot

6\. JVM·GC Snapshot

7\. WAR별 Hikari Pool 수집

8\. Mapper·SQL 수행시간 연결

9\. 최소 OM 대시보드

10\. 핵심 경보와 Runbook

11\. 거래통제·Timeout 자동 원복

12\. 장애 보고·재발방지 절차

## **20.4 중장기 발전 방향**

단순 상태조회
→ ServiceId Metric

수동 로그검색
→ Trace 기반 연결

정적 임계값
→ 시간대별 Baseline

수동 장애분석
→ 원인 후보 자동 제시

수동 거래통제
→ 승인형 자동 완화

단일 OM 저장
→ 전문 시계열·로그 플랫폼 연계

사후 장애대응
→ 예방형 용량·성능 예측

자동 완화는 잘못된 통제가 정상 거래를 중지할 수 있으므로 승인·범위·자동 원복을 갖춘 뒤 적용해야 합니다.

# **21\. 마무리말**

제10부에서 가장 중요하게 기억해야 할 내용은 다음과 같습니다.

ServiceId
\= 어느 기능인가

TraceId
\= 어느 실행인가

Thread
\= 요청이 어디에서 대기하는가

JVM
\= CPU·메모리·GC가 정상인가

DB Pool
\= Connection을 얻을 수 있는가

SQL
\= 어느 데이터 처리가 느린가

OM
\= 이 정보를 연결하고 통제하는 곳

운영진단의 기본 순서는 다음과 같습니다.

사용자 증상
→ 영향범위
→ ServiceId
→ TraceId
→ WAR
→ Thread
→ JVM
→ DB Pool
→ SQL·Lock
→ 최근 변경
→ 완화
→ 복구
→ 재발방지

초보 개발자와 운영자가 장애를 확인할 때 마지막으로 점검해야 할 질문은 다음과 같습니다.

모든 사용자가 느린가, 일부 사용자만 느린가?

모든 ServiceId가 느린가, 특정 기능만 느린가?

어느 서버와 WAR에서 발생하는가?

Busy Thread와 Queue가 증가했는가?

CPU가 높은가, 아니면 I/O를 기다리는가?

GC Pause가 응답 지연시간과 일치하는가?

DB Pool Active가 Max에 도달했는가?

Connection을 기다리는 Pending이 있는가?

어느 Mapper와 SQL이 오래 걸리는가?

DB Lock을 기다리고 있는가?

최근 배포와 설정 변경이 있었는가?

문제 ServiceId만 안전하게 중지할 수 있는가?

재시작 전에 증적을 확보했는가?

Timeout 이후 미확정 거래를 확인했는가?

운영조치가 감사로그에 남았는가?

이 질문에 답할 수 있다면 단순히 서버가 살아 있는지 확인하는 수준을 넘어, 거래·애플리케이션·JVM·DB를 연결해 장애 원인을 진단하고 서비스 영향을 통제할 수 있는 운영형 개발자가 될 수 있습니다.
