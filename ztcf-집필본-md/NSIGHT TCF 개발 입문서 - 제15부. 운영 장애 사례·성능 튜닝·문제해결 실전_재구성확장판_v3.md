<!-- source: ztcf-집필본/NSIGHT TCF 개발 입문서 - 제15부. 운영 장애 사례·성능 튜닝·문제해결 실전_재구성확장판_v3.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

나는 제15부. 운영 장애 사례·성능 튜닝·문제해결 실전의 개념을 코드·설정·로그·산출물 중 어디에서 확인할 수 있는가? 문제가 발생했을 때 어느 경계부터 조사해야 하는가?

읽기 전 질문

① 장의 학습 목표를 먼저 읽습니다. ② 기존 설명과 예제를 따라갑니다. ③ 실무 적용 절차를 자신의 프로젝트에 대입합니다. ④ 완료 기준으로 이해 여부를 확인합니다.

권장 학습법

프레임워크 내부, 아키텍처 의사결정, 장애 대응과 추적성 관리 역량을 강화합니다.

이 부의 학습 좌표

RESTRUCTURED & EXPANDED EDITION

5단계 · 아키텍처와 운영

**초보 IT 개발자를 위한 NSIGHT TCF 개발 입문서**

# **제15부. 운영 장애 사례·성능 튜닝·문제해결 실전**

# **1\. 도입 전 안내말**

제14부에서는 프로젝트 목표와 비기능 요구사항을 정의하고, 여러 아키텍처 대안을 비교하여 목표 아키텍처를 수립하는 방법을 배웠습니다.

아키텍처와 개발표준을 잘 정의했더라도 운영 장애를 완전히 없앨 수는 없습니다.

운영환경에서는 다음과 같은 문제가 발생할 수 있습니다.

특정 ServiceId의 응답시간이 갑자기 증가한다.

Tomcat Thread가 모두 사용되어 신규 요청이 대기한다.

DB Connection을 얻지 못해 Timeout이 발생한다.

SQL 하나가 DB 자원을 과도하게 사용한다.

JVM Heap이 계속 증가하여 OOM이 발생한다.

외부 시스템 응답 지연이 전체 업무로 확산된다.

특정 업무 WAR의 대량 작업 때문에 다른 업무도 느려진다.

신규 WAR 배포 후 일부 서버에서만 오류가 발생한다.

사용자는 실패 응답을 받았지만 DB에는 데이터가 저장되어 있다.

초보 개발자는 장애가 발생하면 다음과 같이 대응하기 쉽습니다.

Tomcat을 재기동한다.

Thread 수를 늘린다.

DB Pool 크기를 늘린다.

Timeout을 길게 변경한다.

서버를 추가한다.

문제가 된 SQL을 임시로 주석 처리한다.

이러한 조치는 일시적으로 증상을 줄일 수 있지만 실제 원인을 해결하지 못할 수 있습니다.

예를 들어 다음 상황을 살펴봅시다.

증상
고객 요약조회 Timeout 증가

Tomcat Busy Thread
95%

DB Pool
Active 120 / Maximum 120
Pending 380

Slow SQL
SV-CUST-SEL-001
평균 4.2초

이때 Tomcat Thread를 1,200개에서 2,000개로 늘리면 어떻게 될까요?

더 많은 요청이 동시에 유입된다.

더 많은 Thread가 DB Connection을 기다린다.

DB가 처리해야 할 SQL이 증가한다.

DB CPU와 I/O가 더 높아진다.

Timeout과 장애가 더 크게 확산될 수 있다.

실제 원인은 Thread 수가 아니라 Slow SQL과 DB Connection 장기 점유일 수 있습니다.

따라서 장애 대응은 다음과 같은 순서로 진행해야 합니다.

사용자 증상 확인

→ 영향범위 확인

→ ServiceId와 Trace 확보

→ Tomcat Thread 확인

→ JVM CPU·Heap·GC 확인

→ DB Pool 확인

→ SQL·Lock 확인

→ 외부연계 확인

→ 최근 변경 확인

→ 임시 완화

→ 근본 원인 수정

→ 재발방지

제15부의 핵심 원칙은 다음과 같습니다.

장애 시 재기동보다 증적 확보를 먼저 고려한다.

하나의 지표만 보고 원인을 확정하지 않는다.

Timeout은 원인이 아니라 결과일 수 있다.

Thread·DB Pool·SQL은 서로 연결해서 분석한다.

복구와 원인분석을 구분한다.

임시조치는 반드시 종료조건을 가진다.

장애 종료는 서비스 복구가 아니라
미확정 거래와 재발방지 조치까지 완료된 상태다.

# **2\. 제15부 개요**

## **2.1 목적**

제15부의 목적은 초보 개발자와 운영자가 NSIGHT 운영환경에서 발생할 수 있는 대표적인 장애를 체계적으로 진단하고, 영향을 최소화하면서 복구하며, 근본 원인을 제거하도록 돕는 것입니다.

학습을 마친 뒤에는 다음 작업을 수행할 수 있어야 합니다.

1.  장애·문제·증상·원인을 구분한다.
2.  사용자 영향과 기술적 영향범위를 확인한다.
3.  ServiceId와 TraceId로 장애 거래를 추적한다.
4.  Timeout 발생 위치를 계층별로 구분한다.
5.  Tomcat Thread 포화의 원인을 분석한다.
6.  HikariCP Connection 고갈 원인을 분석한다.
7.  Slow SQL과 DB Lock을 진단한다.
8.  JVM CPU·Heap·GC 상태를 해석한다.
9.  메모리 누수와 일시적인 메모리 증가를 구분한다.
10.  외부 시스템 장애의 전파를 통제한다.
11.  하나의 Tomcat에서 특정 WAR의 자원 독점을 분석한다.
12.  배포·설정·버전 불일치 장애를 진단한다.
13.  Timeout 후 미확정 거래를 확인한다.
14.  임시조치와 근본조치를 구분한다.
15.  장애 보고서와 재발방지 과제를 작성한다.
16.  장애훈련과 자동 품질 Gate를 설계한다.

## **2.2 적용범위**

| **영역** | **주요 내용** |
| --- | --- |
| 거래 | ServiceId·TraceId·거래로그 |
| Timeout | UI·Gateway·TCF·SQL·외부 호출 |
| Tomcat | Busy Thread·Queue·Rejected |
| JVM | CPU·Heap·GC·Metaspace |
| DB Pool | Active·Idle·Pending·Leak |
| SQL | Slow SQL·실행계획·조회건수 |
| DB Lock | Lock Wait·Deadlock·장기 트랜잭션 |
| 외부연계 | Timeout·Circuit Breaker·Retry |
| WAR | 업무별 자원 독점·장애 격리 |
| 배포 | WAR·설정·버전·Route |
| 데이터 | Idempotency·미확정 거래 |
| 운영 | 경보·Runbook·거래통제 |
| 사후관리 | RCA·재발방지·기술부채 |

## **2.3 대상 독자**

-   장애가 발생하면 Tomcat부터 재시작하는 개발자
-   Timeout 값을 늘리면 문제가 해결된다고 생각하는 개발자
-   Tomcat Thread와 DB Pool의 관계가 어려운 개발자
-   CPU가 낮으면 서버가 정상이라고 생각하는 개발자
-   Slow SQL과 Lock을 구분하기 어려운 개발자
-   특정 WAR가 자원을 독점하는지 확인하고 싶은 운영자
-   배포 후 일부 서버에서만 오류가 발생하는 원인을 찾는 개발자
-   장애 보고서와 재발방지 대책을 작성해야 하는 담당자

## **2.4 선행조건**

다음 내용을 이해하고 있어야 합니다.

ServiceId는 업무기능 식별자다.

TraceId는 하나의 실행 흐름을 연결한다.

Tomcat Thread는 HTTP 요청을 처리한다.

DB Pool은 DB Connection을 재사용한다.

Facade는 트랜잭션 경계를 가진다.

Timeout은 계층별로 다르게 적용된다.

OM은 ServiceId·Thread·JVM·DB Pool·SQL을 관측한다.

## **2.5 주요 용어**

| **용어** | **쉬운 설명** |
| --- | --- |
| Incident | 사용자나 서비스에 영향을 주는 운영 장애 |
| Problem | 하나 이상의 장애를 만든 근본 원인 |
| Symptom | 사용자가 경험하거나 지표에 나타난 현상 |
| Root Cause | 장애가 발생한 근본 이유 |
| Mitigation | 장애 영향을 줄이는 임시조치 |
| Recovery | 서비스를 정상상태로 복구 |
| RCA | Root Cause Analysis, 근본 원인 분석 |
| Saturation | 자원이 처리 한계에 가까운 상태 |
| Bottleneck | 전체 성능을 제한하는 가장 느린 지점 |
| Contention | 여러 작업이 동일 자원을 경쟁하는 상태 |
| Throttling | 요청량을 제한하는 처리 |
| Degradation | 일부 기능을 축소하여 서비스를 유지 |
| Unknown State | 처리 성공 여부를 확정하기 어려운 상태 |
| Evidence | 로그·Metric·Dump 등 판단 근거 |
| Runbook | 장애 유형별 점검·조치 절차 |
| Postmortem | 장애 종료 후 원인과 개선사항을 기록한 문서 |

# **제136장. 장애 대응의 기본원칙**

학습 목표 | 136장. 장애 대응의 기본원칙의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 실패를 설계하는 관점: 오류를 사용자 입력, 업무 규칙, 의존 시스템, 인프라 문제로 구분해야 복구 책임과 메시지가 선명해집니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **136.1 장애와 문제의 차이**

장애
\= 현재 사용자가 영향을 받고 있는 상태

문제
\= 장애를 발생시킨 근본 원인

예:

장애
고객조회 Timeout

문제
조회조건 누락으로 인한 Full Table Scan

장애는 우선 복구해야 하지만 문제는 근본적으로 수정해야 합니다.

## **136.2 장애 대응 목표**

장애 대응의 목표는 다음 네 단계로 나눌 수 있습니다.

탐지

→ 영향 제한

→ 서비스 복구

→ 재발 방지

운영팀이 장애를 빨리 발견했더라도 원인을 찾지 못하면 재발할 수 있습니다.

원인을 찾았더라도 서비스 복구가 늦으면 사용자 영향이 커집니다.

## **136.3 장애 대응 우선순위**

1\. 사용자와 데이터 보호

2\. 장애 확산 방지

3\. 서비스 복구

4\. 증적 보존

5\. 근본 원인 분석

6\. 재발방지

개인정보 유출이나 데이터 손상 가능성이 있다면 단순 가용성보다 보안과 데이터 보호를 우선합니다.

## **136.4 최초 확인사항**

장애 접수 후 다음 질문에 답합니다.

언제부터 발생했는가?

전체 사용자인가, 일부 사용자인가?

특정 화면인가, 전체 화면인가?

조회인가, 변경인가?

특정 ServiceId인가?

모든 서버인가, 한 서버인가?

오류인가, 지연인가?

최근 배포나 설정 변경이 있었는가?

## **136.5 영향범위 분류**

| **범위** | **예** |
| --- | --- |
| 사용자 | 특정 사용자·지점 |
| 화면 | 특정 화면 |
| 거래 | 특정 ServiceId |
| 업무 | SV 전체 |
| 서버 | sv-was-02 |
| 인스턴스 | 특정 Tomcat |
| 시스템 | 전체 NSIGHT |
| 데이터 | 일부 고객·특정 기간 |

## **136.6 대표 거래 확보**

장애를 대표하는 한 건 이상의 거래에서 다음 값을 확보합니다.

GUID

TraceId

ServiceId

거래코드

사용자·지점

요청시각

서버 ID

처리시간

오류코드

이 정보가 있어야 로그와 SQL을 정확하게 연결할 수 있습니다.

## **136.7 재기동 전 확인**

가능하면 재기동 전에 다음 증적을 확보합니다.

Thread Dump

JVM 상태

GC Log

Heap 사용량

DB Pool 상태

Slow SQL

DB Lock

오류로그

최근 배포정보

다만 사용자 영향이 매우 크거나 보안·데이터 위험이 있으면 즉시 격리와 복구를 우선할 수 있습니다.

## **136.8 복구와 분석의 분리**

장애 대응 인력을 다음처럼 나눌 수 있습니다.

복구 담당
→ 트래픽 우회·거래통제·Rollback

분석 담당
→ 로그·Dump·SQL·변경이력 분석

한 사람이 모든 작업을 수행하면 복구와 원인분석이 모두 늦어질 수 있습니다.

## **136.9 장애 시간기록**

반드시 실제 시각을 기록합니다.

10:31 최초 오류 발생

10:34 경보 발생

10:37 장애 인지

10:42 ServiceId 통제

10:48 오류율 정상화

11:20 원인 확인

14:30 근본 수정 배포

시간기록은 탐지·대응 프로세스 개선에 사용합니다.

## **136.10 장애 종료조건**

다음이 충족되어야 장애 종료를 검토할 수 있습니다.

핵심 거래 성공

오류율 정상

응답시간 정상

Thread·Pool 안정

미확정 거래 확인

데이터 정합성 확인

임시통제 해제 또는 유지계획

재발방지 담당자·기한 지정

## **제136장 요약**

학습 목표 | 136장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

장애 대응은 원인분석만의 활동이 아니다.

사용자 영향 제한과 서비스 복구를 우선하면서
필요한 증적을 확보해야 한다.

장애 종료 전 미확정 거래와 데이터 정합성을 확인한다.

# **제137장. Timeout 장애 진단**

학습 목표 | 137장. Timeout 장애 진단의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 실패를 설계하는 관점: 오류를 사용자 입력, 업무 규칙, 의존 시스템, 인프라 문제로 구분해야 복구 책임과 메시지가 선명해집니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **137.1 Timeout은 어디에서 발생할까요?**

하나의 거래에는 여러 Timeout이 존재할 수 있습니다.

UI Timeout

Gateway Timeout

Apache Proxy Timeout

TCF ServiceId Timeout

외부 Client Timeout

DB Connection Timeout

SQL Query Timeout

어느 계층에서 발생했는지 구분해야 합니다.

## **137.2 Timeout 계층 예**

UI
8초

Gateway
7초

TCF ServiceId
5초

SQL
2~3초

외부 Client
700ms

DB Connection 획득
3초

일반적으로 하위 계층의 Timeout을 상위보다 짧게 설계합니다.

## **137.3 UI Timeout**

증상:

화면에서는 Timeout 메시지

서버 거래로그는 SUCCESS

가능한 원인:

-   UI Timeout이 서버보다 짧음
-   네트워크 응답 지연
-   대용량 JSON
-   브라우저 처리 지연
-   응답 후 화면 Script 오류

이 경우 서버 처리시간만 확인하면 문제를 놓칠 수 있습니다.

## **137.4 Gateway Timeout**

증상:

Gateway는 504 반환

업무 WAR는 계속 처리 중

가능한 위험:

사용자는 실패로 인식

DB 변경은 Commit될 수 있음

재시도 시 중복 처리 가능

변경 거래는 Idempotency와 결과조회 기능이 필요합니다.

## **137.5 TCF Timeout**

TCF가 업무 실행시간을 제한합니다.

ServiceId Timeout
5초

업무 실행
6.2초

ETF
TIMEOUT 응답

하지만 실행 Thread를 중단했다고 DB와 외부 시스템 작업이 즉시 중단되는 것은 아닐 수 있습니다.

## **137.6 DB Connection Timeout**

오류 예:

Connection is not available,
request timed out after 3000ms

의미:

SQL을 실행하기 전
DB Connection을 얻지 못함

확인:

Pool Active

Pool Maximum

Pool Pending

Connection 사용시간

Slow SQL

Lock

## **137.7 SQL Query Timeout**

Connection을 얻은 뒤 SQL이 제한시간을 초과한 상황입니다.

Connection 획득
성공

SQL 실행
3초 초과

Query Timeout

Connection Timeout과 원인이 다릅니다.

## **137.8 외부 Client Timeout**

구분:

| **유형** | **의미** |
| --- | --- |
| Connect Timeout | 연결 자체를 못 함 |
| Read Timeout | 연결 후 응답을 못 받음 |
| Pool Acquire Timeout | Client Connection을 못 얻음 |
| Overall Timeout | 전체 호출시간 초과 |

## **137.9 Timeout 진단 순서**

어느 계층의 Timeout인가?

실제 처리시간은 얼마인가?

하위 작업이 계속 실행되는가?

DB Commit 여부는 무엇인가?

Idempotency 상태는 무엇인가?

동일 시간대 Thread·Pool 상태는 어떤가?

최근 Timeout 설정 변경이 있었는가?

## **137.10 Timeout을 늘리는 위험**

Timeout 3초
→ 10초로 증가

가능한 결과:

사용자 대기 증가

Thread 장기 점유

DB Connection 장기 점유

Queue 증가

장애 탐지 지연

Timeout 증가는 임시조치일 수 있지만 근본조치는 아닙니다.

## **137.11 정상적인 조치순서**

Slow 구간 확인

→ SQL·외부 호출 개선

→ 조회범위 제한

→ 비동기 전환 검토

→ 자원격리

→ 최종 Timeout 재산정

## **137.12 Timeout 후 미확정 거래**

변경 거래의 상태:

SUCCESS
실제 성공 확인

FAIL
Rollback 확인

TIMEOUT
처리 제한시간 초과

UNKNOWN
최종 결과 확인 불가

UNKNOWN은 운영자가 업무 데이터와 Idempotency 결과를 확인해야 합니다.

## **사례 137-A. 화면은 실패했지만 등록은 성공**

ServiceId
CM.Campaign.create

화면
8초 Timeout

Gateway
7초 Timeout

업무 WAR
6.8초에 DB Commit

Gateway 응답 전달
7초 초과로 실패

사용자가 저장 버튼을 다시 클릭하면 중복 캠페인이 생성될 수 있습니다.

개선:

Idempotency Key 적용

등록 결과조회 기능

하위 SQL 튜닝

Gateway·TCF Timeout 정합화

화면 중복 클릭 방지

## **제137장 요약**

학습 목표 | 137장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

Timeout은 하나가 아니라 계층별로 존재한다.

어느 계층에서 발생했는지 구분하고
하위 작업과 DB 반영 여부를 확인해야 한다.

Timeout 값을 늘리기 전에 느린 구간을 찾아야 한다.

# **제138장. Tomcat Thread 포화 장애**

학습 목표 | 138장. Tomcat Thread 포화 장애의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 실패를 설계하는 관점: 오류를 사용자 입력, 업무 규칙, 의존 시스템, 인프라 문제로 구분해야 복구 책임과 메시지가 선명해집니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **138.1 Thread 포화 상태**

예:

maxThreads
1,200

Busy Thread
1,190

Queue
850

Rejected
발생

새 요청을 즉시 처리할 Thread가 거의 없습니다.

## **138.2 주요 증상**

전체 또는 다수 거래 지연

Gateway Timeout 증가

Tomcat Queue 증가

Busy Thread 90% 이상 지속

일부 Connection Reset

응답시간 p95·p99 급증

## **138.3 Thread 포화 원인**

트래픽 급증

Slow SQL

DB Pool 대기

외부 시스템 지연

파일 다운로드

Lock 대기

무한 Loop

GC Pause 후 요청 적체

특정 ServiceId 반복 재시도

## **138.4 Thread Dump 분석**

Thread Dump에서 동일한 Stack이 많이 반복되는지 확인합니다.

예:

350개 Thread
HikariPool.getConnection 대기

280개 Thread
외부 HTTP read 대기

210개 Thread
특정 Mapper SQL 실행

100개 Thread
파일 Streaming

이 분포는 장애 원인 후보를 보여줍니다.

## **138.5 DB Pool 대기형 Thread 포화**

Thread
1,000개

DB Pool
120개

Pending
600개

Thread를 늘리면 Pending만 더 증가할 수 있습니다.

조치:

Slow SQL 확인

긴 트랜잭션 확인

요청량 제한

문제 ServiceId 통제

DB Pool과 DB 용량 재검토

## **138.6 외부연계 대기형 Thread 포화**

외부 Client Read Timeout
10초

동시 호출
500건

500개 Thread가 최대 10초 동안 대기할 수 있습니다.

개선:

외부 Timeout 단축

Circuit Breaker

Bulkhead

Fallback

비동기 전환

요청량 제한

## **138.7 파일 처리형 Thread 포화**

대용량 다운로드가 일반 온라인 Thread를 장기 점유할 수 있습니다.

개선:

전용 다운로드 Endpoint

Streaming

별도 Executor

전용 서버·Storage

동시 다운로드 제한

## **138.8 Thread 수 증가가 가능한 경우**

다음 조건을 모두 확인합니다.

CPU 여유

DB Pool 여유

DB 처리능력 여유

외부 시스템 여유

Heap·Thread Stack 여유

부하시험 결과

Thread 수 증가는 용량 검증 후 수행합니다.

## **138.9 Thread Stack 메모리**

Thread마다 Stack 메모리가 필요합니다.

예:

Thread 1,200

Thread Stack 1MB 가정

약 1.2GB

Thread 수 증가는 Native Memory에도 영향을 줍니다.

## **138.10 즉시 완화조치**

문제 ServiceId SUSPENDED

Gateway Rate Limit

파일·대량조회 차단

외부 Circuit Open

트래픽 일부 우회

비정상 서버 L4 제외

## **사례 138-A. 고객조회 때문에 전체 WAR 지연**

Tomcat
SV·IC·CM 다중 WAR

SV 고객조회 요청 급증

SV Slow SQL
5초

공유 Connector Thread
95% 사용

CM 캠페인 등록도 Timeout

근본 원인:

Slow SQL

공유 Thread Pool

업무별 동시성 제한 없음

개선:

SV SQL 튜닝

SV ServiceId Rate Limit

업무그룹별 Tomcat 분리

WAR별 Metric

대량조회 별도 처리

## **제138장 요약**

학습 목표 | 138장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

Thread 포화는 Thread 수만의 문제가 아니다.

Thread가 DB·외부 시스템·파일·Lock 중
어디에서 기다리는지 확인해야 한다.

하위 자원 여유 없이 Thread만 늘리면 장애가 악화될 수 있다.

# **제139장. DB Pool 고갈 장애**

학습 목표 | 139장. DB Pool 고갈 장애의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 실패를 설계하는 관점: 오류를 사용자 입력, 업무 규칙, 의존 시스템, 인프라 문제로 구분해야 복구 책임과 메시지가 선명해집니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **139.1 정상 Pool 상태**

Maximum
120

Active
45

Idle
75

Pending
0

## **139.2 고갈 상태**

Maximum
120

Active
120

Idle
0

Pending
300

모든 Connection이 사용 중이고 300개 Thread가 기다리는 상태입니다.

## **139.3 대표 원인**

Slow SQL

장기 트랜잭션

DB Lock

Connection 누수

트래픽 급증

외부 호출 중 Connection 보유

Pool 크기 과소

DB 장애

## **139.4 Connection 사용시간**

다음 지표가 중요합니다.

Connection Acquire Time

Connection Usage Time

SQL Execution Time

Transaction Duration

SQL은 짧은데 Connection Usage가 길다면 트랜잭션 안에서 다른 작업을 수행하고 있을 수 있습니다.

## **139.5 잘못된 트랜잭션 예**

트랜잭션 시작

→ DB 조회

→ 외부 API 8초 대기

→ DB 변경

→ Commit

외부 API를 기다리는 동안 Connection이 점유될 수 있습니다.

개선:

외부 호출을 트랜잭션 밖으로 이동

필요 데이터 선조회

짧은 DB 변경 트랜잭션

보상처리 적용

## **139.6 Connection 누수**

가능한 징후:

트래픽이 줄어도 Active가 감소하지 않음

Pending이 계속 증가

Leak Detection 로그 발생

특정 Stack에서 Connection 장기 보유

확인:

-   수동 JDBC close() 누락
-   Streaming Cursor 종료 누락
-   비정상 Transaction 관리
-   Thread 중단
-   장기 Batch

## **139.7 WAR별 Pool 총량**

예:

17개 WAR

서버 4대

WAR별 Maximum 120

이론상 최대:

17 × 4 × 120
\= 8,160 Connection

실제 DB 허용 세션을 크게 초과할 수 있습니다.

Pool은 서버 한 대의 설정이 아니라 전체 시스템 합계로 계산해야 합니다.

## **139.8 Pool 크기 증가 판단**

Pool 증가가 유효할 수 있는 상황:

SQL이 충분히 빠름

DB CPU·I/O 여유

Lock 문제 없음

짧은 순간 Peak

Pending이 소량 발생

DBA가 동시 세션 증가를 승인

## **139.9 Pool 증가 금지 상황**

DB CPU 95%

Slow SQL 다수

Lock Wait 증가

Connection Usage 장기화

DB 세션 상한 근접

## **139.10 즉시 완화**

문제 ServiceId 통제

대량 Batch 중지

조회기간 제한

Rate Limit

비정상 Connection 보유 거래 종료

DB Lock 해소

필요 시 서버 일부 격리

## **사례 139-A. DB Pool을 늘렸지만 장애 악화**

변경 전:

Pool 120

DB CPU 85%

Slow SQL 평균 3초

변경 후:

Pool 240

DB CPU 99%

Lock 증가

평균 SQL 7초

Timeout 증가

근본 조치:

Slow SQL 튜닝

조회범위 축소

동시 요청 제한

Pool 120으로 복구

DB 용량 재산정

## **제139장 요약**

학습 목표 | 139장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

Pool 고갈은 Connection 수 부족만의 문제가 아니다.

Slow SQL·Lock·긴 트랜잭션 때문에
Connection이 반환되지 않는 것이 원인일 수 있다.

Pool 증가 전 DB 전체 처리능력과 Pool 총합을 확인한다.

# **제140장. Slow SQL·DB Lock·데이터 처리 장애**

학습 목표 | 140장. Slow SQL·DB Lock·데이터 처리 장애의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 실패를 설계하는 관점: 오류를 사용자 입력, 업무 규칙, 의존 시스템, 인프라 문제로 구분해야 복구 책임과 메시지가 선명해집니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **140.1 Slow SQL이 만드는 연쇄 장애**

SQL 지연

→ Connection 장기 점유

→ DB Pool Active 증가

→ Pending 증가

→ Tomcat Thread 대기

→ Queue 증가

→ Timeout

SQL 하나가 전체 애플리케이션 장애로 확산될 수 있습니다.

## **140.2 Slow SQL 확인정보**

ServiceId

Mapper ID

SQL ID

평균·p95·최대 수행시간

호출건수

조회·영향 건수

실행계획

DB CPU·I/O

Lock Wait

## **140.3 대표 원인**

인덱스 누락

조회조건 누락

대량 기간조회

SELECT \*

선행 Wildcard

컬럼 함수 적용

잘못된 Join

통계정보 오류

정렬 과다

OFFSET 대량 페이징

Partition Pruning 실패

## **140.4 SQL은 빠른데 응답은 느린 경우**

예:

SQL
400ms

조회 행
500,000건

DTO 변환
2초

JSON 직렬화
3초

네트워크
4초

총 응답은 9초가 넘을 수 있습니다.

따라서 다음을 함께 봅니다.

SQL 시간

Row Count

응답 객체 수

응답 Byte 크기

직렬화 시간

네트워크 전송시간

## **140.5 Lock Wait**

Transaction A
고객 데이터 Update 후 미Commit

Transaction B
같은 고객 데이터 Update 대기

확인:

Blocking Session

Waiting Session

대상 Table·Row

SQL ID

트랜잭션 시작시각

Lock 대기시간

ServiceId

## **140.6 장기 트랜잭션 원인**

외부 호출 포함

사용자 입력 대기

대량 Update

Commit 누락

Batch Chunk 과대

한 트랜잭션에서 너무 많은 기능 처리

## **140.7 Deadlock**

Transaction A
Table X → Table Y 순서

Transaction B
Table Y → Table X 순서

서로 Lock을 기다리면 DB가 하나의 거래를 실패시킬 수 있습니다.

개선:

Update 순서 통일

트랜잭션 범위 축소

인덱스 개선

재시도 제한 적용

Batch·온라인 처리시간 분리

## **140.8 데이터 조회범위 제한**

온라인 조회는 다음을 적용합니다.

조회기간 최대값

페이지 크기 최대값

필수 검색조건

정렬 필드 화이트리스트

최대 결과건수

대용량 Export의 Batch 전환

## **140.9 실행계획 변화**

같은 SQL도 데이터 분포와 통계정보에 따라 실행계획이 달라질 수 있습니다.

확인:

통계정보 갱신시각

Bind 값 분포

Plan Hash

인덱스 변경

DB Patch

Partition 상태

## **140.10 튜닝 우선순위**

불필요한 SQL 제거

→ 조회조건 제한

→ 필요한 컬럼만 조회

→ 인덱스 개선

→ Join·실행계획 개선

→ 페이징·분할

→ Cache·Read Model

→ 인프라 증설

## **사례 140-A. 목록 조회 후 WAS 메모리 급증**

SQL 시간
1.2초

조회 건수
80만 건

응답 생성
5.5초

Heap
12GB 증가

GC Pause
2초

최종 Timeout

근본 원인:

목록 최대건수 없음

페이지 처리 없음

CLOB 포함

SELECT \*

개선:

목록·상세 분리

페이지 최대 100건

CLOB 제외

대량 Export Batch 전환

조회기간 최대 3개월

## **제140장 요약**

학습 목표 | 140장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

Slow SQL은 DB만의 문제가 아니라
Pool·Thread·JVM 장애로 확산될 수 있다.

SQL 시간뿐 아니라 조회건수와 응답 크기를 함께 본다.

Lock은 장기 트랜잭션과 처리순서까지 분석해야 한다.

# **제141장. JVM CPU·Heap·GC·OOM 장애**

학습 목표 | 141장. JVM CPU·Heap·GC·OOM 장애의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 실패를 설계하는 관점: 오류를 사용자 입력, 업무 규칙, 의존 시스템, 인프라 문제로 구분해야 복구 책임과 메시지가 선명해집니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **141.1 CPU가 높을 때**

가능한 원인:

트래픽 급증

복잡한 계산

무한 Loop

JSON 변환

압축·암호화

로그 생성

GC 과다

Thread Context Switching

## **141.2 CPU 분석**

확인:

Process CPU

System CPU

Load Average

CPU 사용 Thread

ServiceId별 실행시간

GC CPU 비율

최근 배포

CPU가 높은 Thread Stack을 확인하면 원인 코드에 접근할 수 있습니다.

## **141.3 CPU가 낮은데 느린 경우**

DB 대기

외부연계 대기

Lock 대기

파일 I/O

Connection Pool 대기

대기형 장애에서는 CPU가 낮을 수 있습니다.

## **141.4 Heap 정상 패턴**

Heap 증가

→ Young GC

→ 일부 감소

→ 반복

## **141.5 메모리 누수 의심 패턴**

Heap 증가

→ Full GC

→ 거의 감소하지 않음

→ 다시 증가

→ OOM

## **141.6 대표적인 누수 원인**

무제한 Cache

Static Collection

ThreadLocal 정리 누락

대용량 List 보관

비동기 Queue 적체

ClassLoader 누수

Listener 미해제

파일 byte\[\] 적재

## **141.7 ThreadLocal 누수**

Tomcat Thread는 재사용됩니다.

사용자 A 요청
Thread 101 사용

Context 정리 누락

사용자 B 요청
같은 Thread 101 사용

사용자 A의 Context가 남아 있을 수 있습니다.

필수:

finally {
TransactionContextHolder.clear();
AuthenticationContextHolder.clear();
TimeoutContextHolder.clear();
MDC.clear();
}

## **141.8 GC Pause**

GC Pause가 길면 다수 거래가 같은 시각에 동시에 지연됩니다.

특징:

여러 ServiceId p95 동시 증가

SQL 시간은 정상

DB Pool도 정상

GC Pause 시각과 지연시각 일치

## **141.9 Full GC 반복**

확인:

Heap Max가 너무 작은가?

Old 영역 객체가 계속 증가하는가?

Cache 최대크기가 없는가?

대형 객체가 생성되는가?

Metaspace가 증가하는가?

## **141.10 OOM 유형**

| **유형** | **의미** |
| --- | --- |
| Java Heap Space | Heap 부족 |
| Metaspace | Class Metadata 부족 |
| Direct Buffer Memory | Direct Memory 부족 |
| Unable to Create Native Thread | Thread·Native Memory 부족 |
| GC Overhead Limit | GC 대부분 사용 |

OOM 메시지에 따라 점검대상이 다릅니다.

## **141.11 Heap Dump 주의**

Heap Dump 생성 시 일시 정지 가능

파일 크기 매우 큼

개인정보 포함 가능

Disk 공간 필요

접근권한 제한 필요

## **141.12 즉시 복구**

OOM 발생 서버:

L4에서 제외

증적 확보

프로세스 재기동

트래픽 점진 복귀

다른 서버도 같은 원인으로 OOM이 발생할 수 있으므로 전체 인스턴스를 확인합니다.

## **사례 141-A. Local Cache 무제한 증가**

Cache Key
고객번호

TTL
없음

Maximum Size
없음

고객 요청
계속 증가

Heap
40GB 사용

Full GC 반복

OOM

개선:

Maximum Size

TTL

Eviction Metric

고객별 민감정보 Cache 재검토

Cache Provider 공통화

운영 임계값

## **제141장 요약**

학습 목표 | 141장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

CPU가 높으면 실행 중인 Thread와 GC를 확인한다.

Heap은 GC 이후 얼마나 감소하는지가 중요하다.

ThreadLocal·Cache·대용량 객체는
다중 WAR Tomcat에서 전체 JVM 장애를 만들 수 있다.

# **제142장. 외부연계 장애와 장애 확산 방지**

학습 목표 | 142장. 외부연계 장애와 장애 확산 방지의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 실패를 설계하는 관점: 오류를 사용자 입력, 업무 규칙, 의존 시스템, 인프라 문제로 구분해야 복구 책임과 메시지가 선명해집니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **142.1 외부 시스템 장애 유형**

DNS 실패

Connect Timeout

Read Timeout

HTTP 500

잘못된 전문

업무 오류

응답 지연

간헐적 연결종료

인증서 오류

## **142.2 외부 장애가 내부 장애가 되는 과정**

외부 응답 지연

→ 업무 Thread 대기

→ Client Pool 점유

→ Tomcat Thread 증가

→ Queue 증가

→ 전체 업무 Timeout

## **142.3 Timeout 기준**

연결시간

응답시간

전체 호출시간

재시도 포함 총시간

재시도 시간을 포함해 TCF ServiceId Timeout 안에 완료되어야 합니다.

## **142.4 Retry 적용 기준**

재시도 가능한 오류:

일시적 Network 오류

일부 5xx

짧은 연결 실패

Rate Limit 후 Retry-After

재시도하면 안 되는 오류:

Validation 오류

권한 오류

업무 거절

중복 불가 변경 거래

계약 오류

## **142.5 Retry Storm**

외부 시스템이 장애일 때 모든 서버가 동시에 재시도하면 더 큰 부하가 발생합니다.

개선:

재시도 횟수 제한

Exponential Backoff

Jitter

Circuit Breaker

전체 시간 예산 제한

## **142.6 Circuit Breaker**

상태:

CLOSED
정상 호출

OPEN
호출 차단

HALF\_OPEN
일부 시험호출

## **142.7 Bulkhead**

외부 시스템별로 자원을 분리합니다.

IC Client Pool

EAI Client Pool

보고서 Client Pool

한 외부 시스템 장애가 모든 Client Thread를 점유하지 않도록 합니다.

## **142.8 Fallback**

예:

고객 부가정보 조회 실패

→ 핵심 고객정보만 응답

부분 응답이 업무적으로 허용되는지 사전에 결정해야 합니다.

금지:

실패한 값을 정상값처럼 반환

## **142.9 비동기 전환**

장시간 처리:

대량 고객 조회

보고서 생성

대량 파일 전송

은 온라인 동기 거래 대신 Job ID 방식으로 전환할 수 있습니다.

## **사례 142-A. 외부 고객정보 시스템 지연**

IC Client Read Timeout
10초

SV ServiceId Timeout
5초

동시 요청
600건

문제:

하위 Timeout이 상위보다 길다.

TCF Timeout 후에도 외부 호출이 계속될 수 있다.

Thread가 장기 점유된다.

개선:

IC Client 700ms

제한된 Retry 1회

Circuit Breaker

Bulkhead 50개

부분 응답 정책

OM 외부호출 Metric

## **제142장 요약**

학습 목표 | 142장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

외부 시스템 장애는 내부 Thread와 Pool을 점유해
전체 시스템 장애로 확산될 수 있다.

Timeout·Retry·Circuit Breaker·Bulkhead를
하나의 정책으로 함께 설계해야 한다.

# **제143장. 다중 WAR 자원 독점과 장애 격리**

학습 목표 | 143장. 다중 WAR 자원 독점과 장애 격리의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 실패를 설계하는 관점: 오류를 사용자 입력, 업무 규칙, 의존 시스템, 인프라 문제로 구분해야 복구 책임과 메시지가 선명해집니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **143.1 공유 자원**

하나의 Tomcat에 여러 WAR가 배포되면 다음을 공유합니다.

JVM CPU

Heap

GC

Tomcat Connector Thread

Native Memory

Network

프로세스 장애

## **143.2 분리 자원**

설계에 따라 WAR별로 분리할 수 있습니다.

Hikari Pool

Cache

외부 Client Pool

업무 Executor

Log File

Metric

## **143.3 특정 WAR 독점 사례**

CM WAR
대량 대상자 목록을 메모리에 적재

Heap 15GB 사용

Full GC 발생

SV·IC 거래도 지연

## **143.4 WAR별 CPU 측정의 한계**

CPU와 Heap은 JVM 전체 지표입니다.

정확한 WAR별 CPU를 단순 JVM Metric으로 구분하기 어렵습니다.

대신 다음을 사용합니다.

WAR별 요청량

ServiceId별 처리시간

실행 Thread 수

SQL 시간

Client 시간

Cache Entry

대량 작업 여부

## **143.5 장애 격리 단계**

ServiceId 통제

→ Rate Limit

→ Bulkhead

→ WAR별 Pool

→ Connector·Executor 분리

→ Tomcat 분리

→ VM 분리

## **143.6 업무그룹별 분리**

예:

업무그룹 A
SV·IC·PC·MS

업무그룹 B
CM·PD·EB·EP

운영그룹
OM

다음 기준으로 분리합니다.

트래픽

장애 중요도

배포주기

대량 처리

외부연계

보안등급

운영조직

## **143.7 Batch와 온라인 분리**

같은 JVM에서 대량 Batch가 실행되면 온라인 Heap·CPU·Pool을 사용할 수 있습니다.

대안:

전용 Batch 인스턴스

별도 DB Pool

실행시간 분리

CPU·Memory 제한

Chunk 처리

## **143.8 파일 처리 분리**

대용량 파일 Upload·Download는 일반 온라인 Connector와 분리할 수 있습니다.

## **사례 143-A. 캠페인 Batch 때문에 고객조회 장애**

CM Batch
500만 건 처리

Chunk
10만 건

DB Pool
CM 120개 사용

Heap
20GB 증가

GC Pause
3초

SV 고객조회 p95
8초

개선:

Chunk 1,000건

Batch 전용 Pool

Batch 전용 인스턴스

온라인 피크시간 실행 금지

Checkpoint

WAR 그룹 재분리

## **제143장 요약**

학습 목표 | 143장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

다중 WAR는 배포단위가 나뉘어도
JVM·Thread·GC 장애를 공유할 수 있다.

ServiceId 통제부터 VM 분리까지
업무 중요도에 맞는 격리수준을 적용한다.

# **제144장. 배포·설정·버전 불일치 장애**

학습 목표 | 144장. 배포·설정·버전 불일치 장애의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 실패를 설계하는 관점: 오류를 사용자 입력, 업무 규칙, 의존 시스템, 인프라 문제로 구분해야 복구 책임과 메시지가 선명해집니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **144.1 배포 장애의 대표 유형**

WAR 기동 실패

Mapper XML 누락

환경설정 누락

DB Schema 미반영

OM Service Catalog 누락

Gateway Route 오류

서버별 WAR 버전 불일치

공통 JAR 버전 차이

## **144.2 일부 서버에서만 오류**

예:

WAS 1
sv-service 1.3.0

WAS 2
sv-service 1.2.9

사용자는 L4 분배에 따라 성공과 실패를 반복합니다.

징후:

동일 요청이 간헐적으로 실패

특정 Server ID에서만 오류

응답 필드가 서버마다 다름

## **144.3 환경설정 차이**

WAS 1
Timeout 3000ms

WAS 2
Timeout 10000ms

서버별 성능과 오류율이 달라집니다.

환경설정도 Version과 Hash를 관리해야 합니다.

## **144.4 Mapper 누락**

증상:

Invalid bound statement

Mapper Statement not found

원인:

Mapper XML이 WAR에 포함되지 않음

Namespace 불일치

SQL ID 불일치

Resource 경로 오류

## **144.5 DB Schema 순서 오류**

신규 WAR가 새 컬럼을 사용하는데 DB Script가 적용되지 않았습니다.

Invalid Identifier

Column Not Found

배포 순서와 호환성을 확인해야 합니다.

## **144.6 OM 등록 누락**

ServiceId 코드가 존재하지만 OM Catalog에 없습니다.

Fail-closed 정책이면 거래가 차단됩니다.

SERVICE\_NOT\_REGISTERED

배포 Pipeline에서 자동검증해야 합니다.

## **144.7 Route 오류**

/sv
→ ic-service로 잘못 전달

증상:

업무코드 불일치

ServiceId 미등록

404

권한 오류

## **144.8 배포 장애 진단 순서**

Release ID

Artifact Version

Checksum

Server별 Version

설정 Hash

DB Migration Version

OM Catalog

Gateway Route

Health·Readiness

기동 로그

## **144.9 Rollback 판단**

Rollback 조건:

기동 실패

핵심 거래 실패

오류율 급증

데이터 손상 가능성

보안 취약점

p95 심각한 악화

DB 변경이 하위 호환되지 않으면 WAR만 Rollback할 수 없을 수 있습니다.

## **사례 144-A. Rolling 배포 중 구·신 응답 불일치**

기존 응답:

{
"resultCode": "S0000"
}

신규 응답:

{
"result": {
"resultCode": "S0000"
}
}

Rolling 중 화면은 두 형식을 모두 받을 수 있습니다.

개선:

응답 하위 호환

신규 필드 추가 방식

계약 테스트

Blue-Green 검토

Major Version 관리

## **제144장 요약**

학습 목표 | 144장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

배포 장애는 코드보다 Version·설정·DB·Route의
불일치에서 발생할 수 있다.

서버별 Artifact Version과 설정 Hash를
운영에서 즉시 확인할 수 있어야 한다.

# **제145장. 데이터 정합성·중복·미확정 거래 장애**

학습 목표 | 145장. 데이터 정합성·중복·미확정 거래 장애의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 실패를 설계하는 관점: 오류를 사용자 입력, 업무 규칙, 의존 시스템, 인프라 문제로 구분해야 복구 책임과 메시지가 선명해집니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **145.1 기술적 성공과 업무 성공**

HTTP 200
≠ 업무 성공

DB INSERT 성공
≠ 전체 거래 성공

외부 응답 수신
≠ 내부 상태 확정

전체 유스케이스가 완료되어야 업무 성공입니다.

## **145.2 Master 성공·History 실패**

Master INSERT
성공

History INSERT
실패

같은 트랜잭션이면 전체 Rollback되어야 합니다.

별도 비동기 이력이라면 유실 대응정책이 필요합니다.

## **145.3 외부 성공·내부 실패**

외부 시스템
처리 성공

내부 DB
저장 실패

단순 DB Rollback으로 외부 작업을 취소할 수 없습니다.

필요:

보상 거래

상태 UNKNOWN

재조회

수동 정합성 처리

Saga·Outbox

## **145.4 중복 처리**

원인:

사용자 중복 클릭

Gateway Retry

Client Retry

Timeout 후 재요청

Batch 재실행

메시지 중복 전달

## **145.5 Idempotency**

Idempotency Key

요청 Hash

처리상태

결과

만료시각

같은 Key에 다른 요청내용이 들어오면 계약 오류로 차단합니다.

## **145.6 UNKNOWN 상태**

다음 상황에서 발생할 수 있습니다.

Commit 직후 프로세스 중단

외부 성공 후 응답 유실

Timeout 후 하위 처리 지속

ETF 기록 실패

## **145.7 UNKNOWN 처리절차**

Idempotency Key 조회

업무 Master 조회

History 조회

외부 결과조회

거래로그 비교

최종 상태 확정

재처리 또는 보상

## **145.8 재처리와 재시도**

재시도
\= 동일 실행 중 기술오류를 다시 호출

재처리
\= 장애 종료 후 업무단위로 다시 수행

재처리는 업무 승인과 대상 데이터 검증이 필요할 수 있습니다.

## **145.9 정합성 점검 SQL**

예:

Master 존재·History 없음

PROCESSING 상태 장기 지속

외부 성공·내부 결과 없음

동일 Idempotency Key 다중 Master

Version 역전

## **사례 145-A. 캠페인 등록 중복**

첫 요청
DB Commit 성공

응답 전 Gateway Timeout

사용자 재클릭

두 번째 요청
신규 Campaign ID 생성

개선:

Idempotency Key

기존 결과 반환

화면 버튼 중복 방지

등록 결과조회

UNKNOWN 운영화면

## **제145장 요약**

학습 목표 | 145장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

Timeout과 통신 실패 후에는
실제 업무처리 결과가 불명확할 수 있다.

Idempotency와 상태조회 기능으로
중복과 미확정 거래를 통제해야 한다.

# **제146장. 성능 튜닝의 올바른 순서**

학습 목표 | 146장. 성능 튜닝의 올바른 순서의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **146.1 성능 튜닝의 목표**

빠른 평균시간

만을 목표로 하지 않습니다.

p95·p99 개선

오류율 감소

자원 포화 방지

안정적인 처리량

예측 가능한 응답시간

을 함께 봅니다.

## **146.2 튜닝 전 Baseline**

TPS

평균

p95·p99

오류율

Busy Thread

DB Active·Pending

CPU

Heap·GC

SQL Top N

변경 전 값을 기록하지 않으면 개선 여부를 판단할 수 없습니다.

## **146.3 튜닝 순서**

불필요한 업무처리 제거

→ 조회범위 제한

→ SQL·Index 개선

→ 외부 호출 최적화

→ Cache 검토

→ Thread·Pool 조정

→ JVM 조정

→ Scale-out

## **146.4 가장 큰 병목부터**

예:

| **구간** | **시간** |
| --- | --- |
| Gateway | 20ms |
| TCF 공통 | 10ms |
| 업무 Rule | 15ms |
| SQL | 3,800ms |
| JSON | 100ms |

TCF 공통처리 10ms를 5ms로 줄이는 것보다 SQL을 개선하는 것이 효과적입니다.

## **146.5 성능 변경 한 번에 하나씩**

Thread·Pool·Heap·SQL을 동시에 변경하면 어떤 조치가 효과가 있었는지 알기 어렵습니다.

가설

→ 한 가지 변경

→ 동일 조건 재시험

→ 결과 비교

→ 다음 변경

## **146.6 평균만 보면 안 되는 이유**

평균 800ms

p95 2.9초

p99 15초

일부 사용자는 매우 긴 시간을 기다립니다.

## **146.7 Cache 튜닝**

Cache 도입 전 확인:

반복조회가 많은가?

원본 변경빈도는 낮은가?

오래된 데이터 허용시간은 얼마인가?

민감정보인가?

무효화가 가능한가?

Cache로 Slow SQL을 숨기기 전에 SQL과 데이터 모델을 먼저 검토합니다.

## **146.8 JVM 튜닝**

JVM 옵션은 마지막 단계에서 검토합니다.

Heap 적정성

GC 알고리즘

Pause 목표

Allocation Rate

Direct Memory

Thread Stack

업무코드의 대량 객체 생성을 그대로 둔 채 GC 옵션만 변경하지 않습니다.

## **146.9 Scale-out 전 확인**

무상태인가?

Session은 공유되는가?

DB가 추가 부하를 처리할 수 있는가?

Cache 정합성은 유지되는가?

L4가 트래픽을 분산하는가?

애플리케이션 서버를 늘려도 DB가 병목이면 성능이 개선되지 않을 수 있습니다.

## **제146장 요약**

학습 목표 | 146장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

성능 튜닝은 가장 큰 병목부터 시작한다.

변경 전 Baseline을 남기고
한 번에 하나의 가설을 검증한다.

인프라 증설은 코드·SQL·구조 개선 이후 검토한다.

# **제147장. 종합 장애대응 훈련**

학습 목표 | 147장. 종합 장애대응 훈련의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 실패를 설계하는 관점: 오류를 사용자 입력, 업무 규칙, 의존 시스템, 인프라 문제로 구분해야 복구 책임과 메시지가 선명해집니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **147.1 장애훈련이 필요한 이유**

실제 장애가 발생한 뒤 처음으로 Runbook을 실행하면 다음 문제가 생길 수 있습니다.

담당자를 찾지 못함

접근권한 없음

명령어 오류

Rollback 파일 없음

미확정 거래 조회방법 모름

경보가 동작하지 않음

## **147.2 훈련 시나리오 1: Slow SQL**

특정 SQL에 의도적 지연

→ DB Pool Pending 증가

→ p95 증가

→ 경보 발생

→ ServiceId 통제

→ SQL 원인 확인

→ 복구

## **147.3 훈련 시나리오 2: WAS 한 대 중단**

검증:

L4 제외

서비스 지속

Session·JWT 영향

처리 중 거래

오류율

복구 후 트래픽 복귀

## **147.4 훈련 시나리오 3: 외부 시스템 Timeout**

검증:

Client Timeout

Circuit Breaker

Bulkhead

Fallback

Thread 보호

경보

## **147.5 훈련 시나리오 4: 잘못된 WAR 배포**

검증:

Health 실패

트래픽 투입 차단

Rollback

Version 확인

배포 감사

## **147.6 훈련 시나리오 5: OOM**

검증:

경보

L4 격리

Heap Dump 정책

프로세스 재기동

다른 서버 상태

원인분석

## **147.7 훈련 평가항목**

| **항목** | **평가** |
| --- | --- |
| 탐지시간 | 경보까지 시간 |
| 인지시간 | 담당자 확인 |
| 영향분석 | 범위 특정 |
| 완화시간 | 사용자 영향 축소 |
| 복구시간 | 정상화 |
| 증적 | Dump·Log 확보 |
| 의사소통 | 보고 정확성 |
| 정합성 | 미확정 거래 확인 |

## **147.8 훈련 후 개선**

Runbook 수정

권한 사전 부여

자동 Script 개선

경보 임계값 조정

담당자 연락망 보완

Rollback 자동화

추적정보 추가

## **제147장 요약**

학습 목표 | 147장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

장애대응 절차는 문서만으로 검증되지 않는다.

Slow SQL·WAS 중단·외부 Timeout·배포 실패·OOM을
통제된 환경에서 실제로 훈련해야 한다.

# **제148장. 장애보고와 재발방지**

학습 목표 | 148장. 장애보고와 재발방지의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 실패를 설계하는 관점: 오류를 사용자 입력, 업무 규칙, 의존 시스템, 인프라 문제로 구분해야 복구 책임과 메시지가 선명해집니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **148.1 장애보고서 목적**

장애보고서는 책임자를 비난하기 위한 문서가 아닙니다.

무슨 일이 발생했는가?

왜 발생했는가?

왜 빨리 발견하지 못했는가?

왜 영향이 확산되었는가?

어떻게 재발을 막을 것인가?

에 답해야 합니다.

## **148.2 장애보고서 구조**

| **항목** | **내용** |
| --- | --- |
| 장애 ID | 식별자 |
| 발생·종료 | 정확한 시각 |
| 영향 | 사용자·업무 |
| 증상 | 오류·지연 |
| 탐지 | 경보·신고 |
| 직접 원인 | 기술적 원인 |
| 근본 원인 | 설계·프로세스 원인 |
| 완화조치 | 통제·우회 |
| 복구조치 | Rollback·수정 |
| 데이터 영향 | 중복·누락 |
| 재발방지 | Action Item |
| 책임자 | 담당자 |
| 기한 | 완료 예정일 |

## **148.3 직접 원인과 근본 원인**

예:

직접 원인
인덱스 누락으로 SQL Full Scan

근본 원인
SQL 실행계획 검증 Gate가 없었음
대량 데이터 성능시험이 없었음

## **148.4 다섯 번 왜 질문하기**

왜 Timeout이 발생했는가?
→ SQL이 느렸다.

왜 SQL이 느렸는가?
→ 인덱스가 없었다.

왜 인덱스가 없었는가?
→ DB 설계 검토에서 누락됐다.

왜 검토에서 누락됐는가?
→ 실제 데이터 규모 기반 시험이 없었다.

왜 시험이 없었는가?
→ 성능 Gate 진입조건이 정의되지 않았다.

## **148.5 재발방지 유형**

코드 수정

SQL·DB 수정

설정 변경

모니터링 추가

자동검증 추가

Runbook 개선

교육

아키텍처 변경

운영 프로세스 변경

## **148.6 좋은 재발방지**

나쁜 예:

개발자가 주의한다.

좋은 예:

SELECT \* 사용을 CI에서 차단한다.

목록조회 최대건수를 공통 Rule로 적용한다.

DB Pool 총합을 배포 Gate에서 계산한다.

ServiceId별 p95 경보를 추가한다.

## **148.7 완료 증적**

재발방지 과제는 “완료”라고 말하는 것만으로 끝나지 않습니다.

Source Commit

테스트 Report

배포 Version

경보 화면

Runbook

훈련 결과

등의 증적이 필요합니다.

## **제148장 요약**

학습 목표 | 148장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

장애보고는 직접 원인뿐 아니라
설계·검증·운영 프로세스의 근본 원인을 찾아야 한다.

재발방지는 사람의 주의보다
자동화·구조·품질 Gate로 전환해야 한다.

# **3\. 목표 장애대응 아키텍처**

\[사용자 증상·경보\]
│
▼
\[Incident 등록\]
│
▼
\[ServiceId·TraceId 확인\]
│
┌─────┼─────────────┐
▼ ▼ ▼
\[Thread\] \[JVM\] \[DB Pool\]
│ │ │
└─────┼──────┬──────┘
▼ ▼
\[SQL\] \[외부연계\]
│ │
└──┬───┘
▼
\[최근 변경\]
Release·설정·DB·정책
│
┌─────┴─────┐
▼ ▼
\[임시 완화\] \[근본 수정\]
거래통제·우회 코드·SQL·구조
│ │
└─────┬─────┘
▼
\[복구\]
│
▼
\[미확정 거래 확인\]
│
▼
\[RCA·재발방지\]
│
▼
\[자동 Gate·Runbook\]

# **4\. 표준 형식**

## **4.1 Incident 기본정보**

{
"incidentId": "INC20260717001",
"severity": "CRITICAL",
"detectedDtm": "2026-07-17T10:34:00+09:00",
"affectedBusiness": "SV",
"affectedServiceId": "SV.Customer.selectSummary",
"symptom": "Timeout rate increased",
"status": "INVESTIGATING",
"commander": "operations-user01"
}

## **4.2 증적 Snapshot**

{
"incidentId": "INC20260717001",
"serverId": "sv-was-02",
"warName": "sv-service",
"busyThreads": 1120,
"maxThreads": 1200,
"dbPoolActive": 120,
"dbPoolMaximum": 120,
"dbPoolPending": 380,
"heapUsedRate": 0.62,
"processCpuRate": 0.55,
"slowSqlId": "SV-CUST-SEL-001",
"slowSqlP95Ms": 4200
}

## **4.3 임시조치**

{
"operationType": "SERVICE\_RATE\_LIMIT",
"targetServiceId": "SV.Customer.selectSummary",
"previousValue": "unlimited",
"newValue": "50 requests/sec",
"reason": "Prevent DB saturation",
"effectiveTo": "2026-07-17T12:00:00+09:00",
"autoRestore": true
}

## **4.4 재발방지 과제**

{
"actionId": "ACT-INC20260717001-01",
"action": "Add index for customer summary query",
"owner": "SV-DB-Team",
"dueDate": "2026-07-24",
"verification": "Peak load test p95 under 1 second",
"status": "IN\_PROGRESS"
}

# **5\. 구성요소 및 속성**

| **구성요소** | **주요 속성** |
| --- | --- |
| Incident Registry | 심각도·상태 |
| Transaction Log | ServiceId·Trace |
| Runtime Snapshot | Thread·JVM·Pool |
| SQL Metric | Mapper·SQL ID |
| Change Registry | Release·설정 |
| Control Service | 중지·Rate Limit |
| Idempotency Store | 처리 결과 |
| Runbook | 진단·복구 |
| RCA Repository | 근본 원인 |
| Action Registry | 재발방지 |
| Architecture Gate | 자동 차단 |
| Audit Log | 운영조치 |

# **6\. 책임 경계와 RACI**

| **활동** | **OPS** | **DEV** | **FW** | **TA** | **DBA** | **SEC** | **QA** | **AA** | **PMO** |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 장애 탐지 | A/R | I | I | C | C | C | I | I | I |
| 영향 분석 | A | R/C | C | C | C | C | C | C | I |
| Service 통제 | A/R | C | C | C | I | C | I | C | I |
| Thread·JVM 분석 | C | C | C | A/R | I | I | I | C | I |
| DB Pool·SQL | C | R/C | I | C | A/R | I | C | C | I |
| 외부연계 | A/C | R | C | C | I | C | I | C | I |
| Rollback | A/R | C | C | C | C | C | C | C | I |
| 데이터 정합성 | C | R | I | I | A/C | C | C | C | I |
| RCA | A/C | R | R/C | R/C | R/C | C | C | A | I |
| 재발방지 | C | R | R | R | R | R/C | C | A | C |
| 종료 승인 | A | C | C | C | C | C | C | C | I |

# **7\. 정상 장애대응 흐름**

1\. 경보 또는 사용자 신고로 장애를 탐지한다.

2\. Incident ID와 대응 책임자를 지정한다.

3\. 사용자·업무·서버 영향범위를 확인한다.

4\. 대표 ServiceId와 TraceId를 확보한다.

5\. Thread·JVM·DB Pool·SQL을 확인한다.

6\. 최근 배포와 설정 변경을 확인한다.

7\. 장애 확산 방지 조치를 적용한다.

8\. 서비스 복구 또는 Rollback을 수행한다.

9\. 핵심 거래와 자원상태를 재검증한다.

10\. 미확정 거래와 데이터 정합성을 확인한다.

11\. 임시조치의 유지·해제 계획을 확정한다.

12\. RCA와 재발방지 과제를 등록한다.

13\. 증적 확인 후 Incident를 종료한다.

# **8\. 오류·Timeout·장애 흐름**

## **8.1 증적 확보 실패**

서버 재기동 완료

→ Thread Dump 없음

→ 직접 원인 확인 어려움

→ 재발방지 근거 부족

개선:

자동 Runtime Snapshot

장애 경보 시 Dump Script

운영 권한 사전 준비

## **8.2 잘못된 임시조치**

Pool 증가

→ DB 과부하 증가

→ 전체 업무 장애

즉시 원복하고 병목을 재분석합니다.

## **8.3 복구 후 재발**

Tomcat 재기동

→ 일시 정상

→ Cache·누수 원인 유지

→ 6시간 후 동일 OOM

근본 원인 수정 전 Incident를 완전히 종료하지 않습니다.

## **8.4 미확정 거래 누락**

오류율 정상화

→ 장애 종료

→ 다음 날 중복 등록 발견

변경 거래의 TIMEOUT·UNKNOWN을 반드시 확인합니다.

# **9\. 정상 예시**

증상
SV.Customer.selectSummary Timeout 증가

영향
전체 지점의 18%

Trace
대표 20건 확보

Thread
Hikari Connection 대기 430개

DB Pool
Active 120, Pending 380

SQL
SV-CUST-SEL-001 p95 4.2초

최근 변경
전일 조회조건 변경 배포

임시조치
조회기간 최대 1개월
ServiceId Rate Limit

복구
오류율 0.1% 이하
p95 1.4초

근본조치
인덱스 추가
SQL 조건 수정
성능 Gate 추가

# **10\. 금지 예시**

## **10.1 증적 없이 재기동**

느리다
→ Tomcat 재시작

## **10.2 Timeout 무조건 증가**

3초
→ 30초

## **10.3 Pool 무조건 증가**

DB 포화와 Slow SQL을 확인하지 않습니다.

## **10.4 모든 서버 동시 재시작**

가용성과 증적을 모두 잃을 수 있습니다.

## **10.5 장애 중 운영 DB 임의 수정**

승인·Backup·검증 없이 데이터를 수정합니다.

## **10.6 오류 거래 전체 재처리**

이미 성공한 거래까지 다시 처리하여 중복이 발생합니다.

## **10.7 임시통제 무기한 유지**

ServiceId가 장기간 중지된 상태로 남습니다.

## **10.8 개인 책임으로만 결론**

개발자의 실수

로 끝내고 구조적 Gate를 추가하지 않습니다.

# **11\. 연계 규칙**

경보
→ Incident Registry

Incident
→ ServiceId·Trace

Trace
→ WAR·Server

ServiceId
→ Mapper·SQL

Server
→ Thread·JVM·Pool

Release ID
→ Artifact·설정

Timeout 거래
→ Idempotency Store

운영조치
→ Audit Log

RCA
→ Action Registry

Action
→ Architecture Gate

# **12\. 데이터 및 상태관리**

## **12.1 Incident 상태**

OPEN
→ INVESTIGATING
→ MITIGATING
→ RECOVERING
→ MONITORING
→ RESOLVED
→ CLOSED

## **12.2 거래 확정상태**

SUCCESS
FAIL
TIMEOUT
UNKNOWN
RECONCILED

## **12.3 운영조치 상태**

REQUESTED
APPROVED
APPLIED
VERIFIED
RESTORED
FAILED

## **12.4 재발방지 상태**

OPEN
IN\_PROGRESS
IMPLEMENTED
VERIFIED
CLOSED
OVERDUE

# **13\. 성능·용량·확장성**

| **영역** | **운영 목표 예** |
| --- | --- |
| 주요 거래 | p95 3초 이하 |
| 시스템 오류율 | 0.1% 이하 |
| Timeout율 | 0.1% 이하 |
| Busy Thread | 70% 이하 지속 |
| DB Pool 사용률 | 70~80% 이하 |
| DB Pending | 지속 발생 없음 |
| Heap | GC 후 70% 이하 목표 |
| Full GC | 반복 발생 없음 |
| 일반 SQL | 평균 100~300ms 권고 |
| Slow SQL | Service별 기준 관리 |
| 외부 Client | 상위 Timeout보다 짧게 |
| 복구 | RTO 범위 내 |

수치는 성능시험과 업무 중요도에 따라 확정합니다.

# **14\. 보안·개인정보·감사**

장애 증적에 포함된 개인정보를 보호한다.

Heap Dump와 로그 접근권한을 제한한다.

운영 DB 직접 수정은 승인과 감사를 적용한다.

JWT와 Private Key를 장애로그에 기록하지 않는다.

ServiceId 중지·Timeout 변경·Rollback을 감사한다.

장애보고서에 불필요한 개인정보를 포함하지 않는다.

보안사고 가능성이 있으면 일반 장애보다 높은 통제를 적용한다.

# **15\. 운영·모니터링·장애 대응**

OM 대시보드의 장애 진단 흐름:

전체 시스템 상태

→ 업무 WAR 상태

→ ServiceId Top N

→ 실패 Trace

→ Thread·JVM·DB Pool

→ Mapper·SQL

→ 최근 Release

→ 운영 Control

필수 경보:

시스템 오류율

Timeout율

p95·p99

Busy Thread

DB Pending

GC Pause

Process Down

Slow SQL 급증

외부 Circuit Open

서버별 Version 불일치

# **16\. 자동검증 및 품질 Gate**

| **Gate** | **검증** |
| --- | --- |
| Timeout | 하위 < 상위 |
| Thread | 부하검증 결과 |
| DB Pool | 전체 합계 |
| SQL | 실행계획·최대건수 |
| Transaction | 외부호출 포함 여부 |
| Idempotency | 변경 거래 적용 |
| Memory | Cache 최대크기 |
| Context | ThreadLocal 정리 |
| Deployment | Version·Checksum |
| Configuration | 서버별 Hash |
| OM | ServiceId·Metric 등록 |
| Runbook | 장애유형별 존재 |
| Recovery | Rollback·장애훈련 |
| RCA | 재발방지 자동화 |

# **17\. 테스트 시나리오**

| **ID** | **시나리오** | **기대 결과** |
| --- | --- | --- |
| OPS-001 | UI Timeout | 서버 결과 확인 |
| OPS-002 | Gateway Timeout | 중복 방지 |
| OPS-003 | TCF Timeout | 표준 상태 |
| OPS-004 | Connection Timeout | Pool 대기 확인 |
| OPS-005 | SQL Timeout | Mapper 추적 |
| OPS-006 | Busy Thread 90% | 경보 발생 |
| OPS-007 | Thread Queue 증가 | Rate Limit |
| OPS-008 | DB Pool Active Max | 원인 분석 |
| OPS-009 | Connection 누수 | Leak 탐지 |
| OPS-010 | Slow SQL | ServiceId 연결 |
| OPS-011 | Lock Wait | 차단 Session 확인 |
| OPS-012 | DB Deadlock | 오류·재시도 정책 |
| OPS-013 | CPU 90% | CPU Thread 확인 |
| OPS-014 | Full GC 반복 | Heap 분석 |
| OPS-015 | Heap 누수 | Dump·원인 확인 |
| OPS-016 | OOM | 서버 격리·복구 |
| OPS-017 | 외부 Read Timeout | Circuit Open |
| OPS-018 | Retry Storm | 재시도 억제 |
| OPS-019 | 특정 WAR 대량처리 | 다른 WAR 보호 |
| OPS-020 | Batch Pool 포화 | 온라인 격리 |
| OPS-021 | WAR 버전 불일치 | 서버 식별 |
| OPS-022 | 설정 Hash 불일치 | 배포 실패 |
| OPS-023 | Mapper 누락 | Health 실패 |
| OPS-024 | OM 등록 누락 | Gate 차단 |
| OPS-025 | Timeout 후 Commit | 결과조회 |
| OPS-026 | 중복 재요청 | 기존 결과 반환 |
| OPS-027 | UNKNOWN 거래 | 정합성 처리 |
| OPS-028 | WAS 한 대 장애 | 서비스 지속 |
| OPS-029 | Rollback | 이전 버전 복구 |
| OPS-030 | 장애훈련 | RTO 충족 |

# **18\. 제15부 체크리스트**

## **18.1 장애 최초 대응**

| **점검 항목** | **확인** |
| --- | --- |
| Incident ID를 발급했는가? | □ |
| 장애 책임자를 지정했는가? | □ |
| 발생시각을 확인했는가? | □ |
| 영향 사용자를 확인했는가? | □ |
| 대상 ServiceId를 확인했는가? | □ |
| 대표 TraceId를 확보했는가? | □ |
| 최근 변경을 확인했는가? | □ |

## **18.2 Timeout**

| **점검 항목** | **확인** |
| --- | --- |
| 어느 계층의 Timeout인지 확인했는가? | □ |
| 하위 작업이 계속 실행되는가? | □ |
| DB 반영 여부를 확인했는가? | □ |
| Timeout 계층이 정합적인가? | □ |
| Timeout 증가에 종료조건이 있는가? | □ |
| Idempotency가 적용되어 있는가? | □ |

## **18.3 Thread·JVM**

| **점검 항목** | **확인** |
| --- | --- |
| Busy Thread와 Queue를 확인했는가? | □ |
| Thread Dump를 확보했는가? | □ |
| 동일 Stack을 분류했는가? | □ |
| CPU 사용 Thread를 확인했는가? | □ |
| GC Pause를 확인했는가? | □ |
| Heap이 GC 후 회복되는가? | □ |
| ThreadLocal 정리를 확인했는가? | □ |

## **18.4 DB Pool·SQL**

| **점검 항목** | **확인** |
| --- | --- |
| Active·Idle·Pending을 확인했는가? | □ |
| Connection 사용시간을 확인했는가? | □ |
| Slow SQL을 ServiceId와 연결했는가? | □ |
| 조회 건수를 확인했는가? | □ |
| DB Lock을 확인했는가? | □ |
| 긴 트랜잭션을 확인했는가? | □ |
| 전체 Pool 합계를 계산했는가? | □ |

## **18.5 외부연계·WAR 격리**

| **점검 항목** | **확인** |
| --- | --- |
| 외부 Timeout 유형을 구분했는가? | □ |
| Retry가 안전한가? | □ |
| Circuit Breaker가 동작하는가? | □ |
| Bulkhead가 적용되는가? | □ |
| 특정 WAR 트래픽을 구분할 수 있는가? | □ |
| Batch와 온라인 자원이 분리되는가? | □ |
| ServiceId 단위 통제가 가능한가? | □ |

## **18.6 배포·데이터**

| **점검 항목** | **확인** |
| --- | --- |
| 서버별 WAR Version을 확인했는가? | □ |
| 설정 Hash를 확인했는가? | □ |
| DB Migration Version을 확인했는가? | □ |
| OM Catalog를 확인했는가? | □ |
| Route를 확인했는가? | □ |
| 미확정 거래를 확인했는가? | □ |
| 중복·누락 데이터를 확인했는가? | □ |

## **18.7 복구·사후관리**

| **점검 항목** | **확인** |
| --- | --- |
| 핵심 거래가 정상인가? | □ |
| 오류율과 p95가 정상인가? | □ |
| Thread·Pool이 안정적인가? | □ |
| 임시통제 해제계획이 있는가? | □ |
| RCA를 작성했는가? | □ |
| 재발방지 담당자와 기한이 있는가? | □ |
| 재발방지 증적을 검증했는가? | □ |

# **19\. 변경·호환성·폐기 관리**

## **19.1 임계값 변경**

다음 값을 변경하면 기존 Baseline과 비교합니다.

Thread

DB Pool

Timeout

Heap

GC 옵션

Rate Limit

Circuit Breaker

## **19.2 임시 설정**

변경 전 값

임시 값

사유

적용시각

종료시각

자동 원복

승인자

를 기록합니다.

## **19.3 Runbook 변경**

장애훈련과 실제 장애 결과를 반영하여 Runbook을 갱신합니다.

과거 절차를 삭제하지 않고 변경이력을 관리합니다.

## **19.4 경보 폐기**

경보가 불필요해 보이더라도 단순 삭제하지 않습니다.

호출량 감소

시스템 폐기

대체 경보 존재

잔여 위험

을 확인합니다.

## **19.5 구조적 개선**

장애가 반복되면 단순 설정조정보다 아키텍처 변경을 검토합니다.

Tomcat 분리

DB Read Model

비동기 처리

Cache 구조 변경

외부연계 격리

업무 도메인 재분리

# **20\. 시사점**

## **20.1 핵심 아키텍처 판단**

제15부의 핵심은 다음과 같습니다.

Timeout 발생
\= Timeout 값이 너무 짧다

가 아닙니다.

Timeout 발생
\= 처리경로 중 하나가 시간예산 안에 끝나지 못했다

입니다.

또한 다음 판단도 주의해야 합니다.

Busy Thread 높음
≠ Thread 수 부족

DB Pending 높음
≠ Pool 크기 부족

CPU 높음
≠ 서버 수 부족

Heap 높음
≠ Heap 크기 부족

## **20.2 주요 위험**

| **위험** | **영향** |
| --- | --- |
| 증적 없이 재기동 | 원인 소실 |
| Timeout 무조건 증가 | 자원 장기 점유 |
| Thread 무조건 증가 | 하위 자원 포화 |
| Pool 무조건 증가 | DB 장애 악화 |
| 평균시간만 확인 | Tail 지연 누락 |
| SQL 시간만 확인 | 대량 응답 누락 |
| 외부 Retry 남용 | Retry Storm |
| 다중 WAR 공유자원 미고려 | 장애 확산 |
| 배포 버전 불일치 | 간헐 오류 |
| 미확정 거래 미점검 | 중복·누락 |
| 임시조치 무기한 유지 | 기술부채 고착 |
| 개인 실수로만 결론 | 구조적 재발 |

## **20.3 우선 보완 과제**

1\. ServiceId·Trace 기반 장애 추적

2\. Tomcat Thread Snapshot 자동수집

3\. JVM·GC·Heap 기준선 확보

4\. WAR별 DB Pool Metric

5\. Slow SQL·Mapper 연결

6\. Timeout 계층 정합성 검사

7\. 변경 거래 Idempotency 적용

8\. 외부 Client Circuit Breaker·Bulkhead

9\. 서버별 Version·설정 Hash 조회

10\. 미확정 거래 운영화면

11\. 장애훈련과 Runbook

12\. RCA 과제의 자동 Gate 전환

## **20.4 중장기 발전 방향**

수동 로그분석
→ Trace 자동연결

정적 임계값
→ Baseline 이상탐지

수동 거래통제
→ 승인형 자동 완화

장애 후 용량조정
→ 포화 예측

수동 미확정 거래조회
→ 자동 Reconciliation

일회성 장애보고
→ 반복 원인 Trend 분석

개인 경험 기반 대응
→ Runbook 자동 실행

자동 완화와 자동 복구는 오작동 시 정상 거래에 영향을 줄 수 있으므로 승인·범위·자동 원복을 함께 설계해야 합니다.

# **21\. 마무리말**

제15부에서 가장 중요하게 기억해야 할 장애 진단 순서는 다음과 같습니다.

사용자 증상

→ 영향범위

→ ServiceId

→ TraceId

→ 실행 서버·WAR

→ Tomcat Thread

→ JVM CPU·Heap·GC

→ DB Pool

→ Mapper·SQL·Lock

→ 외부연계

→ 최근 배포·설정

→ 임시 완화

→ 서비스 복구

→ 데이터 정합성

→ 근본 원인

→ 재발방지

각 지표의 의미는 다음과 같습니다.

Busy Thread
\= 현재 요청을 처리하거나 기다리는 작업자 수

DB Pool Active
\= 현재 사용 중인 Connection

DB Pool Pending
\= Connection을 기다리는 Thread

CPU
\= 실제 계산작업의 사용량

Heap
\= Java 객체 메모리

GC Pause
\= 객체 정리로 애플리케이션이 멈춘 시간

Slow SQL
\= DB Connection을 오래 점유하는 SQL

TraceId
\= 장애 거래의 전체 실행경로

초보 개발자와 운영자가 장애 발생 시 마지막으로 확인해야 할 질문은 다음과 같습니다.

전체 사용자가 영향을 받는가?

특정 ServiceId만 문제인가?

어느 서버와 WAR에서 발생하는가?

어느 계층에서 Timeout이 발생했는가?

Busy Thread가 어디에서 기다리는가?

DB Pool Pending이 증가했는가?

Connection을 오래 점유하는 SQL은 무엇인가?

DB Lock을 기다리는 거래가 있는가?

CPU가 계산 때문에 높은가, GC 때문에 높은가?

Heap이 GC 후에도 계속 증가하는가?

외부 시스템 장애가 Thread를 점유하고 있는가?

최근 WAR·설정·DB 변경이 있었는가?

서버별 Version이 동일한가?

Timeout 거래의 DB 반영 여부를 확인했는가?

중복 또는 누락된 업무 데이터가 있는가?

임시조치에 종료시각과 자동 원복이 있는가?

근본 원인을 자동검증과 Architecture Gate로 전환했는가?

이 질문에 답할 수 있다면 단순히 장애가 발생한 서버를 재기동하는 수준을 넘어, 거래·애플리케이션·JVM·DB·외부 시스템을 연결해 원인을 찾고 장애 확산을 통제하며 재발을 예방할 수 있는 운영형 개발자와 아키텍트로 성장할 수 있습니다.
