<!-- source: ztcf-집필본/NSIGHT TCF Chapter 32- 성능 분석과 튜닝.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제32장. 성능 분석과 튜닝

## 도입 전 안내말

제31장에서는 장애가 발생했을 때 영향 범위와 대표 거래를 확보하고, Timeout·Thread·DB Pool·SQL·외부 연계를 분석하여 서비스를 복구하는 방법을 살펴보았다.

이번 장에서는 장애가 발생하기 전과 후에 시스템 성능을 어떻게 측정하고 개선할 것인지를 다룬다.

초보 개발자는 성능 튜닝을 다음과 같이 생각하기 쉽다.

\`\`\`text id=“perf32001” 화면이 느리다.

→ SQL에 Index를 추가한다.

→ Tomcat Thread를 늘린다.

→ DB Pool을 늘린다.

→ JVM Heap을 늘린다.

→ Timeout을 늘린다.



이러한 변경이 실제 병목과 맞으면 성능이 좋아질 수 있다.

그러나 원인을 측정하지 않고 값을 변경하면 오히려 다음과 같은 문제가 발생할 수 있다.

\`\`\`text id="perf32002"
필요하지 않은 Index가 증가해
INSERT·UPDATE 성능이 저하된다.

Tomcat Thread가 증가해
DB Connection 대기 Thread만 많아진다.

Hikari Pool을 늘려
DB Session과 Slow SQL 부하가 증가한다.

JVM Heap을 지나치게 크게 설정해
Old GC 시간이 길어진다.

Timeout을 늘려
느린 거래가 자원을 더 오래 점유한다.

Cache를 무분별하게 적용해
오래된 데이터를 빠르게 반환한다.

성능 튜닝은 설정값을 크게 만드는 작업이 아니다.

\`\`\`text id=“perf32003” 현재 처리량과 지연을 측정한다.

느린 구간을 분해한다.

실제 병목을 식별한다.

하나의 변경을 적용한다.

같은 조건으로 다시 시험한다.

개선효과와 부작용을 비교한다.



성능 분석의 핵심 질문은 다음과 같다.

\`\`\`text id="perf32004"
몇 TPS를 처리해야 하는가?

현재 몇 TPS를 안정적으로 처리하는가?

평균이 아니라 p95·p99는 얼마인가?

오류와 Timeout이 발생하기 시작하는 지점은 어디인가?

CPU·Heap·GC·Thread·DB Pool 중
어느 자원이 먼저 포화되는가?

응답시간 중
DB·외부연계·업무코드가 각각 얼마를 사용하는가?

튜닝 후 같은 부하에서 실제로 개선됐는가?

성능이 좋아진 대신
정합성·보안·운영성이 나빠지지 않았는가?

성능시험은 최대 TPS 숫자 하나를 얻는 활동이 아니다.

\`\`\`text id=“perf32005” TPS

-   p50·p95·p99
-   오류율
-   Timeout율
-   CPU·Heap·GC
-   Tomcat Thread
-   DB Pool
-   SQL
-   외부연계
-   데이터 정합성



을 함께 분석해야 한다.

NSIGHT TCF의 운영지표도 ServiceId별 TPS, 평균·p95·p99 응답시간, 업무·시스템 오류율, Timeout과 미종료 거래를 함께 관리하도록 정의한다.

\---

\# 문서 개요

\## 목적

본 장의 목적은 NSIGHT TCF에서 성능 목표를 정의하고, 거래 처리시간과 시스템 자원을 계층별로 측정하여 병목을 식별하며, 개선 전후를 동일 조건으로 검증하기 위한 성능 분석·튜닝 기준을 수립하는 것이다.

세부 목적은 다음과 같다.

\`\`\`text id="perf32006"
성능 목표와 측정 기준 정의

사용자·동시성·TPS 관계 정리

ServiceId별 성능 기준 수립

평균·p95·p99·최대값 구분

응답시간 Budget 분해

Gateway·TCF·업무·SQL·연계 구간 측정

Slow SQL과 실행계획 분석

Index 적용·변경·폐기 기준 수립

Tomcat Thread·Queue 튜닝

Hikari Connection Pool 튜닝

JVM Heap·GC·CPU 분석

Cache·Batch·파일의 온라인 영향 분석

점증·Peak·Stress·Spike·Soak 시험

장애·자원고갈·DR 성능 검증

튜닝 전후 비교와 회귀검증

운영 임계값·Alert·Runbook 연결

자동검증과 Performance Gate 적용

## 적용범위

| 영역 | 분석 대상 |
| --- | --- |
| 사용자 | 사용자 수·동시 사용자·Think Time |
| 거래 | 업무코드·ServiceId·거래유형 |
| 응답시간 | 평균·p50·p95·p99·최대 |
| 처리량 | TPS·분당 건수·시간당 건수 |
| 오류 | 업무 오류·시스템 오류·Timeout |
| Gateway | 인증·라우팅·연계시간 |
| TCF | STF·Dispatcher·ETF·Timeout |
| 업무코드 | Handler·Service·Rule·변환 |
| DB | Connection·SQL·Lock·I/O |
| JVM | CPU·Heap·GC·Thread·Metaspace |
| Tomcat | maxThreads·Busy·Queue·Connection |
| Hikari | Max·Active·Idle·Pending·Acquire |
| Cache | Hit·Miss·Load·Heap |
| 외부 연계 | Connect·Read·Retry·Circuit |
| Batch | Worker·Pool·DB·온라인 경합 |
| 파일 | 메모리·대역폭·Streaming |
| 인프라 | VM·Disk·Network·L4 |
| 배포 | Version별 성능 회귀 |
| DR | 센터 장애 상태의 처리능력 |

## 대상 독자

\`\`\`text id=“perf32007” 업무 개발자

프레임워크 개발자

애플리케이션 아키텍트

기술·인프라 아키텍트

DBA·SQL 튜너

성능시험 담당자

QA·테스트 담당자

운영·관제 담당자

DevOps·배포 담당자

PMO·서비스 책임자


\## 선행조건

\`\`\`text id="perf32008"
공식 사용자 수

Peak 업무시간

목표 TPS

목표 응답시간

업무별 호출비율

테스트 데이터 규모

운영과 유사한 DB 통계

ServiceId별 거래로그

Thread·JVM·Pool Metric

SQL ID·실행계획

외부시스템 모의환경

성능시험 전용 환경

성능결과 Baseline

# 핵심 관점

\`\`\`text id=“perf32009” 성능 튜닝은 느리다는 느낌을 없애는 작업이 아니다.

동일한 입력과 부하조건에서 어느 구간이 얼마만큼 느린지를 측정하고,

가장 먼저 포화되는 자원을 개선한 뒤 처리량·지연·오류·자원 사용이 실제로 좋아졌음을 증명하는 작업이다.


\---

\# 학습 목표

이 장을 마치면 다음 내용을 설명하고 적용할 수 있어야 한다.

| 번호 | 학습 목표 |
|---:|---|
| 1 | 성능 목표와 용량 목표를 구분한다. |
| 2 | 사용자 수·동시 사용자·동시 요청·TPS를 구분한다. |
| 3 | Little’s Law를 성능 분석에 적용한다. |
| 4 | 평균·p50·p95·p99의 차이를 설명한다. |
| 5 | 처리량과 응답시간을 함께 분석한다. |
| 6 | Baseline 없는 튜닝을 금지한다. |
| 7 | ServiceId별 성능을 측정한다. |
| 8 | 응답시간을 Gateway·업무·SQL·외부 구간으로 분해한다. |
| 9 | 실제 병목과 단순 증상을 구분한다. |
| 10 | SQL 실행계획을 읽고 주요 비용을 식별한다. |
| 11 | Index의 선택도·컬럼 순서·DML 비용을 설명한다. |
| 12 | Full Scan이 항상 잘못된 것은 아님을 설명한다. |
| 13 | N+1·과다 Join·불필요한 정렬을 식별한다. |
| 14 | Query Timeout과 성능개선을 구분한다. |
| 15 | Tomcat Thread와 DB Pool 역할을 구분한다. |
| 16 | Pool을 크게 한다고 처리량이 무조건 증가하지 않음을 설명한다. |
| 17 | Hikari Active·Idle·Pending·Acquire를 해석한다. |
| 18 | CPU·GC·Thread·Pool 포화를 구분한다. |
| 19 | Heap 확대 전 객체 할당과 GC를 확인한다. |
| 20 | Cache·Batch·파일이 온라인 성능에 주는 영향을 분석한다. |
| 21 | Load·Peak·Stress·Spike·Soak 시험을 구분한다. |
| 22 | Open Model과 Closed Model 부하를 구분한다. |
| 23 | Warm-up과 본 측정구간을 분리한다. |
| 24 | 테스트 데이터·Index 통계를 운영과 유사하게 구성한다. |
| 25 | 한 번에 하나의 변경만 적용한다. |
| 26 | 동일 조건에서 개선 전후를 비교한다. |
| 27 | 성능개선의 부작용과 정합성을 검증한다. |
| 28 | 성능 결과를 운영 임계값과 Alert에 반영한다. |
| 29 | Performance Gate와 변경관리를 적용한다. |

\---

\# 핵심 용어

| 용어 | 의미 |
|---|---|
| Performance | 목표 부하에서 응답·처리·오류 기준을 만족하는 능력 |
| Capacity | 시스템이 안정적으로 처리할 수 있는 최대 업무량 |
| Throughput | 단위시간에 완료한 처리량 |
| TPS | 초당 완료 거래 건수 |
| Concurrency | 동시에 진행 중인 작업 수 |
| Concurrent User | 같은 시간대에 시스템을 사용하는 사용자 |
| In-flight Request | 현재 처리 중인 HTTP 요청 |
| Think Time | 사용자가 다음 요청까지 기다리는 시간 |
| Response Time | 요청부터 응답 완료까지 걸린 시간 |
| Service Time | 실제 자원을 사용해 처리한 시간 |
| Wait Time | Queue·Lock·Connection 등에서 기다린 시간 |
| Latency | 응답 지연 |
| p50 | 전체 거래 중 50%가 이하인 응답시간 |
| p95 | 전체 거래 중 95%가 이하인 응답시간 |
| p99 | 전체 거래 중 99%가 이하인 응답시간 |
| Baseline | 개선 전 기준 결과 |
| Bottleneck | 전체 처리량을 제한하는 가장 포화된 자원 |
| Saturation | 처리능력보다 요청이 많아 대기가 증가하는 상태 |
| Utilization | 자원 사용률 |
| Queue | 처리 전 대기 중인 요청 |
| Little’s Law | 동시 처리량 = 처리율 × 평균 체류시간 |
| Execution Plan | DB가 SQL을 실행하는 방법 |
| Cardinality | 처리할 것으로 예상되는 Row 수 |
| Selectivity | 조건이 데이터를 얼마나 좁히는지 나타내는 정도 |
| Index | 검색범위를 줄이는 DB 접근구조 |
| Full Table Scan | Table 전체 Block을 읽는 방식 |
| N+1 | 목록 1회 후 각 행마다 추가 SQL을 수행하는 문제 |
| Connection Pool | 재사용 가능한 DB Connection 집합 |
| Thread Pool | 작업을 실행하는 Thread 집합 |
| GC | 사용하지 않는 Java 객체를 회수하는 작업 |
| Allocation Rate | 단위시간에 생성되는 객체 크기 |
| Load Test | 목표부하에서 성능을 검증하는 시험 |
| Stress Test | 한계를 넘겨 포화·실패지점을 찾는 시험 |
| Spike Test | 부하가 급격히 증가할 때의 반응 시험 |
| Soak Test | 장시간 실행해 누수·누적 문제를 찾는 시험 |
| Scalability Test | 자원·서버 증가에 따른 처리량 증가를 검증 |
| Profiling | CPU·Memory·Method 실행비용을 상세 분석 |
| Performance Regression | 변경 후 성능이 이전보다 나빠진 상태 |

\---

\# 성능 목표

\## 프로젝트 기준과 측정값 구분

현재 프로젝트 문서에서 자주 사용하는 설계 참고값은 다음과 같다.

\`\`\`text id="perf32010"
사용자
36,000명

목표 TPS
720

목표 p95
3초 이내

가용성
99.99%

Tomcat Busy Thread
70% 이하 권장

JVM Heap
70% 이하 안정영역

Hikari Pool
70~80% 이하 권장

이는 목표·가정값이며 실제 운영 설정은 성능시험으로 검증해야 한다. 성능시험은 최대 TPS만 확인하는 것이 아니라 CPU·Heap·GC·Thread·DB Pool·SQL·외부연계를 함께 확인해야 한다.

## 업무별 목표 분리

모든 ServiceId에 동일한 응답시간을 적용하기 어렵다.

| 거래등급 | 예 | p95 예시 |
| --- | --- | --- |
| 단순 기준조회 | 공통코드 | 0.5초 |
| 일반 단건조회 | 고객요약 | 1~3초 |
| 복합조회 | 통합 고객분석 | 3초 |
| 변경거래 | 등록·승인 | 3초 |
| 외부연계 | 기관 조회 | 계약 기준 |
| 대량조회 | 비동기 전환 | 온라인 기준 제외 |
| 파일 | 전송량 기준 | 별도 SLA |
| Batch | 완료시간 | 시간창 기준 |

표의 값은 예시이며 공식 NFR에서 확정한다.

# 사용자·동시성·TPS 관계

## 사용자 수와 TPS는 같지 않다

\`\`\`text id=“perf32011” 등록 사용자 36,000명

동시 로그인 사용자 10,000명

동시에 화면을 조작하는 사용자 3,600명

실제 처리 중 요청 1,200건

완료 TPS 720



각 값은 서로 다른 의미다.

\## Little’s Law

안정된 상태에서 다음 관계를 사용할 수 있다.

\`\`\`text id="perf32012"
동시 처리 요청 수
\=
TPS
× 평균 응답시간

예:

\`\`\`text id=“perf32013” 720 TPS

평균 응답시간 1.5초

평균 In-flight 1,080건



또 다른 예:

\`\`\`text id="perf32014"
동시 처리 요청
3,600건

평균 응답시간
3초

필요 처리량
1,200 TPS

주의할 점:

\`\`\`text id=“perf32015” 동시 사용자 10% ≠ 항상 동시 요청 10%

p95 3초 ≠ 평균 응답시간 3초



사용자는 화면을 보고 입력하는 Think Time이 있으므로 등록 사용자와 In-flight 요청을 직접 동일하게 계산해서는 안 된다.

\## 용량 가정 정합성

다음 항목은 하나의 산정서에서 일관돼야 한다.

\`\`\`text id="perf32016"
사용자 수

동시 로그인률

업무 활성률

요청 빈도

Think Time

거래 Mix

평균 응답시간

목표 TPS

목표 TPS가 720이고 동시요청이 3,600건이라면 평균 체류시간은 5초가 된다.

text id="perf32017" 3,600 ÷ 720 = 5초

그러나 p95 목표가 3초라면 다음 중 무엇을 의미하는지 다시 확인해야 한다.

\`\`\`text id=“perf32018” 동시요청률 10%가 실제 In-flight가 아닌가?

720 TPS가 전체가 아니라 센터·업무그룹 기준인가?

Think Time이 별도로 반영됐는가?

일부 거래의 응답목표가 다른가?

설계 TPS에 안전율이 별도로 있는가?



계산값이 다르다는 이유만으로 어느 한 값이 틀렸다고 단정하지 않고 용어와 산정조건을 재확인한다.

\---

\# 성능 분석 전체 흐름

\`\`\`text id="perf32019"
요구사항·NFR
↓
업무량·거래 Mix
↓
Baseline 측정
↓
응답시간 구간 분해
↓
병목 후보 식별
↓
한 가지 개선
↓
동일 부하 재시험
↓
효과·부작용 비교
↓
운영 설정·Alert 반영

# 성능 분석 계층

\`\`\`text id=“perf32020” 사용자 체감시간

├─ Network·L4·Apache ├─ Gateway ├─ TCF·STF·Dispatcher·ETF ├─ 업무 코드 ├─ DB Connection 대기 ├─ SQL 실행 ├─ 내부·외부 연계 ├─ Queue·Lock 대기 └─ 응답 직렬화·전송


\---

\# 현재 구현과 목표 구조

\## 현재 기준 소스의 공통 설정

현재 업무 WAR의 공통 \`application.yml\`에는 다음 개발·기본 설정이 확인된다.

\`\`\`text id="perf32021"
Hikari maximumPoolSize
10

minimumIdle
2

connectionTimeout
3초

validationTimeout
3초

idleTimeout
10분

maxLifetime
30분

keepaliveTime
5분

autoCommit
false

MyBatis statementTimeout
3초

defaultFetchSize
500

Transaction defaultTimeout
5초

Online Timeout
5초

DB Query Timeout
3초

이 값은 공통 시작값·개발용 기본값이며 운영 최종값으로 확정된 값이 아니다.

현재 application-prod.yml은 주로 DB URL·계정·Pool 이름을 외부화하며, Pool 크기는 별도 운영 설정으로 덮어쓰지 않으면 공통값 10을 사용할 수 있다.

따라서 운영 전 다음을 확인해야 한다.

\`\`\`text id=“perf32022” 실제 Effective Configuration

tcf-cicd 외부 설정

환경변수

JVM System Property

업무 WAR별 Pool 값

Tomcat Instance 전체 Pool 합계


\## 현재 JVM 설정 샘플

현재 \`tcf-cicd/prod/ztomcat/setenv.sh\` 샘플에는 다음 값이 확인된다.

\`\`\`text id="perf32023"
\-Xms1024m

\-Xmx4096m

Java 21

Timezone Asia/Seoul

이 값도 운영용량이 확정된 최종값이 아니라 Git 참조용 샘플이다.

프로젝트 용량 기준에서 검토 중인 32Core·256GB VM과 Heap 32~48GB 같은 값은 실제 성능·GC 시험을 거쳐 별도로 확정해야 한다.

## 현재 모니터링 구현

현재 OM·Batch 소스에서는 다음 값을 수집할 수 있다.

\`\`\`text id=“perf32024” Process CPU

Heap 사용률

Live Thread

Hikari Active Connection

Hikari Maximum Pool

DB Health

Actuator Metric



부분적으로 부족한 항목:

\`\`\`text id="perf32025"
Tomcat Busy Thread

Tomcat Queue

Hikari Idle

Hikari Pending

Connection Acquire p95

GC Pause

Allocation Rate

SQL별 p95

외부 Interface별 p95

ServiceId별 실제 Timer

## 현재 Metric 구현 판단

현재 거래 종료시간과 처리시간은 거래로그로 확인할 수 있다.

다만 ServiceId별 실시간 TPS·p95·p99를 운영하려면 Debug 문자열 로그가 아니라 실제 Micrometer Counter·Timer 계측이 필요하다.

\`\`\`text id=“perf32026” 현재 기반 거래로그 elapsedMs

목표 tcf.transaction.duration tcf.transaction.count tcf.transaction.error tcf.transaction.timeout


\## 현재 Timeout Executor

온라인 거래를 Timeout으로 제어하는 Executor가 무제한 확장 가능한 Cached Thread Pool이라면, 하위 시스템 지연 시 Thread 수가 증가해 성능장애를 확대할 수 있다.

목표:

\`\`\`text id="perf32027"
Core Thread

Max Thread

Bounded Queue

Rejected Policy

Active Metric

Queue Metric

Graceful Shutdown

## 구현 상태 판단

| 항목 | 상태 | 판단 |
| --- | --- | --- |
| 거래 처리시간 | 구현 확인 | ServiceId 통계 확대 |
| 거래 오류·Timeout | 구현 확인 | Metric 연계 |
| CPU·Heap·Live Thread | 구현 확인 | 상세 GC·Busy 보완 |
| Hikari Active·Max | 구현 확인 | Idle·Pending 보완 |
| Query Timeout | 구현 확인 | SQL 등급별 검증 |
| Fetch Size | 기본값 존재 | SQL별 적합성 검증 |
| Pool Size 10 | 기본값 | 운영값 아님 |
| Heap 1~4GB | 배포 샘플 | 운영값 아님 |
| p95·p99 Timer | 부분 | Micrometer 구현 필요 |
| SQL ID 추적 | 설계 기반 | Runtime 연결 강화 |
| Execution Plan Gate | 설계 기준 | CI 적용 필요 |
| 부하 Script | 프로젝트 확인 필요 | 표준 Script 필요 |
| Profiling | 별도 도구 필요 | 제한 환경 적용 |
| Capacity 자동산정 | 설계·화면 기반 | 실측 검증 필요 |

# 설계 원칙

## 원칙 1. 측정하지 않은 문제를 튜닝하지 않는다

\`\`\`text id=“perf32028” “DB가 느린 것 같다.”

“Thread가 부족한 것 같다.”

“Heap이 작은 것 같다.”



라는 추정만으로 설정을 변경하지 않는다.

\## 원칙 2. 평균만 보지 않는다

\`\`\`text id="perf32029"
평균
0.8초

p95
2.8초

p99
12초

평균은 좋아 보여도 일부 사용자는 매우 느릴 수 있다.

## 원칙 3. 처리량과 지연을 함께 본다

\`\`\`text id=“perf32030” TPS 증가

응답시간 안정 → 정상 확장

TPS 증가

응답시간 급증 → 포화 시작

TPS 정체

오류 증가 → 한계 초과


\## 원칙 4. 가장 먼저 포화되는 자원을 개선한다

\`\`\`text id="perf32031"
CPU 정상

GC 정상

Thread Busy 90%

Hikari Pending 80

이면 JVM Heap보다 DB Connection 대기를 먼저 분석한다.

## 원칙 5. 한 번에 하나의 변경을 적용한다

\`\`\`text id=“perf32032” Index 추가

Pool 증가

Thread 증가

Heap 증가



를 동시에 하면 어떤 변경이 효과를 냈는지 알 수 없다.

\## 원칙 6. 성능과 정합성을 함께 검증한다

성능이 빨라졌어도 다음이 발생하면 실패다.

\`\`\`text id="perf32033"
잘못된 데이터

중복 처리

권한 우회

Timeout 미작동

Rollback 누락

Stale Cache

## 원칙 7. Scale-out 전에 병목의 공유 여부를 확인한다

\`\`\`text id=“perf32034” AP가 병목 → Scale-out 효과 가능

DB가 병목 → AP 증가가 DB 부하만 증가


\---

\# 성능 개선 우선순위

\`\`\`text id="perf32035"
1\. 불필요한 작업 제거
2\. 조회 데이터량 축소
3\. SQL·Index 개선
4\. Transaction 범위 축소
5\. 외부 호출·Timeout 개선
6\. Cache·비동기 적용
7\. Thread·Pool 조정
8\. JVM·GC 조정
9\. Scale-out·Scale-up
10\. 구조적 아키텍처 변경

설정 확대보다 수행할 작업 자체를 줄이는 것이 우선이다.

# 32.1 측정 없는 튜닝을 피하는 이유

## 32.1.1 Baseline

튜닝 전 반드시 기준값을 남긴다.

| 구분 | 측정값 |
| --- | --- |
| 테스트 Version |  |
| 설정 Version |  |
| 데이터 건수 |  |
| 동시 사용자 |  |
| 목표 TPS |  |
| 실제 TPS |  |
| 평균 |  |
| p95 |  |
| p99 |  |
| 오류율 |  |
| Timeout율 |  |
| CPU |  |
| Heap |  |
| GC Pause |  |
| Busy Thread |  |
| Pool Active·Pending |  |
| Slow SQL |  |

Baseline이 없으면 개선 여부를 증명할 수 없다.

## 32.1.2 측정구간

\`\`\`text id=“perf32036” Warm-up

Ramp-up

Steady State

Ramp-down



본 결과는 Steady State 구간을 기준으로 판단한다.

Warm-up 중에는 다음이 발생한다.

\`\`\`text id="perf32037"
Class Loading

JIT Compilation

DB Pool 생성

Cache Miss

DNS

JWKS Load

SQL Cursor 준비

Warm-up 데이터를 본 측정에 섞지 않는다.

## 32.1.3 응답시간 구간 분해

예:

| 구간 | 시간 |
| --- | --- |
| L4·Apache | 30ms |
| Gateway | 40ms |
| TCF 공통 | 30ms |
| 업무 코드 | 50ms |
| DB Connection 대기 | 1,800ms |
| SQL 실행 | 200ms |
| 응답 처리 | 30ms |
| 전체 | 2,180ms |

이 경우 SQL 자체보다 Connection 대기가 병목이다.

## 32.1.4 성능 Budget

p95 3초 목표 예시:

| 구간 | Budget 예 |
| --- | --- |
| Network·Proxy | 150ms |
| Gateway | 150ms |
| TCF 공통 | 200ms |
| 업무 코드 | 400ms |
| DB | 1,200ms |
| 외부 연계 | 500ms |
| 여유 | 400ms |
| 합계 | 3,000ms |

이는 예시일 뿐이며 ServiceId 특성에 따라 다르게 정의한다.

한 구간이 Budget을 초과하면 다른 구간이 빨라도 위험하다.

## 32.1.5 ServiceId 중심 분석

text id="perf32038" 전체 평균 응답시간 1.2초

만으로는 문제 거래를 찾기 어렵다.

\`\`\`text id=“perf32039” SV.Customer.selectSummary p95 1.1초

SV.Customer.searchHistory p95 8.4초

CM.Campaign.approve p95 0.9초



ServiceId별로 분리한다.

\---

\## 32.1.6 거래 Mix

실제 업무비율을 반영해야 한다.

| 거래 | 비율 |
|---|---:|
| 단건조회 | 45% |
| 목록조회 | 25% |
| 복합조회 | 15% |
| 등록·변경 | 10% |
| 외부연계 | 5% |

조회 100% 시험만 통과하고 실제 변경·연계가 포함되면 결과가 달라질 수 있다.

\---

\## 32.1.7 Open Model과 Closed Model

\### Closed Model

\`\`\`text id="perf32040"
가상 사용자

요청

응답 대기

Think Time

다음 요청

응답이 느려지면 요청 발생률도 감소한다.

### Open Model

text id="perf32041" 초당 720건을 응답과 관계없이 계속 유입

운영 Peak 유입률과 Queue 포화를 확인하는 데 유용하다.

둘의 결과를 혼동하지 않는다.

## 32.1.8 사용자 Think Time

\`\`\`text id=“perf32042” 화면 결과 확인

입력

메뉴 이동

다음 요청



Think Time 없이 모든 가상 사용자가 즉시 반복 요청하면 실제보다 과도하거나 다른 유형의 부하가 된다.

반대로 Think Time을 지나치게 길게 두면 목표 TPS에 도달하지 못한다.

\---

\## 32.1.9 측정오차

영향요소:

\`\`\`text id="perf32043"
부하 발생기 CPU 부족

부하 발생기 Network 부족

클라이언트 Connection 재사용 차이

테스트 데이터 Cache

DB 통계 차이

Background Batch

다른 팀 시험

로그 DEBUG 활성화

안티바이러스·보안 Agent

부하 발생기 자체가 병목인지 확인한다.

## 32.1.10 Cold와 Warm 성능

### Cold

\`\`\`text id=“perf32044” 재기동 직후

Cache 비어 있음

Pool 초기화 중


\### Warm

\`\`\`text id="perf32045"
Cache 적재

JIT 완료

Pool 안정화

운영 배포 직후 성능과 평상시 성능을 각각 시험한다.

## 32.1.11 단일 거래와 복합 부하

단일 거래시험:

text id="perf32046" SV.Customer.selectSummary만 720 TPS

복합시험:

\`\`\`text id=“perf32047” 17개 업무 WAR

실제 거래 Mix

Batch·Cache·로그 포함



단일 거래 결과만으로 전체 운영 용량을 판단하지 않는다.

\---

\## 32.1.12 정상 상태와 장애 상태

\`\`\`text id="perf32048"
정상 DB

Slow DB

외부 2초 지연

Cache 전체 Miss

AP 1대 장애

센터 1개 장애

에서 목표 수준을 검증한다.

## 32.1.13 잘못된 측정 사례

text id="perf32049" 한 번 curl 실행 → 0.2초 → 성능 PASS

text id="perf32050" 개발 H2에서 1,000 TPS → 운영 Oracle도 가능

text id="perf32051" 서버 CPU 30% → 아직 충분한 여유

CPU가 낮아도 DB Pool·외부 시스템이 병목일 수 있다.

## 32.1.14 튜닝 가설

모든 튜닝에는 가설이 있어야 한다.

\`\`\`text id=“perf32052” 관찰 Connection Acquire p95 2.5초

가설 Slow SQL로 Pool Connection이 장기 점유됨

변경 SQL Index와 조회범위 개선

예상 SQL p95 2초 → 300ms Pool Pending 20 → 0 전체 p95 4초 → 1초


\---

\## 32.1.15 한 가지 변경

\`\`\`text id="perf32053"
시험 A
기준값

시험 B
Index만 추가

시험 C
필요 시 Pool 변경

시험 B와 C를 분리한다.

## 32.1.16 결과 판정

개선:

\`\`\`text id=“perf32054” TPS 증가

p95 감소

오류율 동일·감소

자원 사용 안정

데이터 결과 동일



부분 개선:

\`\`\`text id="perf32055"
평균 감소

p99 증가

GC 증가

이는 완전한 개선이 아니다.

# 32.2 느린 SQL과 인덱스

## 32.2.1 SQL 성능 추적

\`\`\`text id=“perf32056” ServiceId

→ Handler

→ Service

→ DAO

→ Mapper Namespace

→ SQL ID

→ 실행계획

→ Table·Index



SQL에서 역추적할 때 Mapper ID, 호출 Repository·Service와 최종 ServiceId를 연결해야 한다.

\---

\## 32.2.2 SQL 실행시간 구성

\`\`\`text id="perf32057"
DB Connection 획득

\+ SQL Parse

\+ Execution

\+ DB I/O

\+ Lock Wait

\+ Network Fetch

\+ Java Mapping

거래로그에 SQL 실행시간만 기록하면 Connection 대기와 Result Mapping 시간을 놓칠 수 있다.

## 32.2.3 Slow SQL 기준

다음 기준을 조합한다.

\`\`\`text id=“perf32058” 실행시간 임계치

ServiceId Budget 대비 비율

읽은 Row 수

반환 Row 수

실행 빈도

DB CPU·I/O

Lock 시간



1초 SQL 한 건과 50ms SQL 10만 건 중 후자가 DB에 더 큰 부하를 줄 수 있다.

\---

\## 32.2.4 실행계획 확인항목

\`\`\`text id="perf32059"
Access Path

Join Method

Join Order

Estimated Rows

Actual Rows

Cost

Predicate

Sort

Temporary Space

Partition Pruning

Index 사용 여부

예상 Row와 실제 Row 차이가 크면 통계·분포·Bind 문제를 확인한다.

## 32.2.5 Full Table Scan

Full Scan이 항상 잘못된 것은 아니다.

적합할 수 있는 경우:

\`\`\`text id=“perf32060” Table이 작다.

대부분의 Row를 조회한다.

Batch 전체처리다.

Sequential I/O가 더 효율적이다.



부적합 가능성이 높은 경우:

\`\`\`text id="perf32061"
대형 Table

온라인 단건조회

선택도가 높은 조건

Peak 시 반복 실행

## 32.2.6 Index 선택도

예:

text id="perf32062" USE\_YN Y 99% N 1%

USE\_YN='Y' 단독 Index는 선택도가 낮아 효과가 작을 수 있다.

복합 업무조건을 함께 검토한다.

\`\`\`text id=“perf32063” BUSINESS\_CODE

SERVICE\_ID

USE\_YN


\---

\## 32.2.7 복합 Index 컬럼 순서

예 SQL:

\`\`\`sql id="perf32064"
SELECT CUSTOMER\_NO,
SUMMARY\_SCORE
FROM SV\_CUSTOMER\_SUMMARY
WHERE BRANCH\_ID = :branchId
AND CUSTOMER\_NO = :customerNo;

후보 Index:

text id="perf32065" (BRANCH\_ID, CUSTOMER\_NO)

컬럼 순서는 다음을 고려한다.

\`\`\`text id=“perf32066” Equal 조건

범위조건

선택도

정렬

다른 조회패턴

DML 비용



선택도만으로 기계적으로 순서를 결정하지 않는다.

\---

\## 32.2.8 Function과 암묵적 변환

금지 예:

\`\`\`sql id="perf32067"
WHERE TO\_CHAR(CREATED\_AT, 'YYYYMMDD')
\= :businessDate

대안:

sql id="perf32068" WHERE CREATED\_AT >= :startAt AND CREATED\_AT < :endAt

금지 예:

sql id="perf32069" WHERE CUSTOMER\_NO = 123456

Column이 문자열이면 암묵적 타입 변환으로 Index 사용이 불안정할 수 있다.

## 32.2.9 앞부분 Wildcard

sql id="perf32070" WHERE CUSTOMER\_NAME LIKE '%홍길동%'

일반 B-Tree Index를 사용하기 어렵다.

대안:

\`\`\`text id=“perf32071” 검색요건 제한

전문검색

검색용 정규화 Column

Prefix 검색

별도 Search Engine


\---

\## 32.2.10 OR 조건

\`\`\`sql id="perf32072"
WHERE CUSTOMER\_NO = :customerNo
OR PHONE\_NO = :phoneNo

데이터 분포와 Index에 따라 비효율적일 수 있다.

대안으로 UNION ALL을 검토할 수 있지만 중복·실행계획을 실제 측정해야 한다.

## 32.2.11 SELECT \*

금지 이유:

\`\`\`text id=“perf32073” 불필요한 Column I/O

Network 전송 증가

Java 객체 증가

Covering Index 활용 저하

Table 변경 영향 증가



필요한 Column만 조회한다.

\---

\## 32.2.12 N+1

나쁜 예:

\`\`\`java id="perf32074"
List<Customer> customers =
customerMapper.selectCustomers(query);

for (Customer customer : customers) {
List<Account> accounts =
accountMapper.selectByCustomer(
customer.customerNo()
);
}

100명의 고객이면 SQL이 101번 실행될 수 있다.

대안:

\`\`\`text id=“perf32075” Join

IN Batch 조회

두 번 조회 후 Memory Merge

Pre-aggregated View

업무별 Read Model


\---

\## 32.2.13 과도한 Join

많은 Table을 하나의 SQL에 결합하면 다음 문제가 발생할 수 있다.

\`\`\`text id="perf32076"
Cardinality 추정 오류

Join 순서 복잡

중복 Row

정렬·Hash Memory 증가

변경 영향 확대

반대로 SQL을 지나치게 나누면 N+1과 Network Round Trip이 증가한다.

실제 실행계획과 처리량으로 결정한다.

## 32.2.14 불필요한 DISTINCT

DISTINCT로 중복을 제거하기 전에 왜 중복이 발생하는지 확인한다.

\`\`\`text id=“perf32077” 잘못된 Join 조건

1:N 관계

권한 Table 중복

History 중복



원인을 숨기기 위해 \`DISTINCT\`를 넣지 않는다.

\---

\## 32.2.15 정렬

\`\`\`sql id="perf32078"
ORDER BY CREATED\_AT DESC

대량 결과 정렬은 메모리·Temporary 공간을 사용한다.

검토:

\`\`\`text id=“perf32079” 정렬이 업무상 필요한가?

Index 순서를 활용할 수 있는가?

조회 건수 상한이 있는가?

안정적 Tie-breaker가 있는가?


\---

\## 32.2.16 페이징

Offset Paging:

\`\`\`sql id="perf32080"
OFFSET 900000 ROWS
FETCH NEXT 100 ROWS ONLY

깊은 Page에서 비용이 증가할 수 있다.

Keyset:

sql id="perf32081" WHERE ( CREATED\_AT < :lastCreatedAt OR ( CREATED\_AT = :lastCreatedAt AND ID < :lastId ) ) ORDER BY CREATED\_AT DESC, ID DESC FETCH FIRST 100 ROWS ONLY

대량 목록은 Keyset을 검토한다.

## 32.2.17 Count SQL

목록마다 전체 Count를 수행하면 Count SQL이 실제 목록보다 더 느릴 수 있다.

대안:

\`\`\`text id=“perf32082” Count 생략

다음 페이지 존재 여부

비동기 Count

추정 Count

검색조건 제한



화면 요구사항과 합의한다.

\---

\## 32.2.18 Index의 DML 비용

Index를 추가하면 조회는 빨라질 수 있지만 다음 비용이 증가한다.

\`\`\`text id="perf32083"
INSERT

UPDATE

DELETE

Redo·Undo

Storage

통계수집

Batch

Index 튜닝 결과는 조회와 변경거래를 함께 시험한다.

## 32.2.19 중복 Index

\`\`\`text id=“perf32084” Index A (CUSTOMER\_NO)

Index B (CUSTOMER\_NO, CREATED\_AT)



Index B가 A의 역할을 대체할 수 있는지 실제 실행계획을 확인한다.

불필요한 Index는 DML 비용만 증가시킨다.

\---

\## 32.2.20 Covering Index

조회 Column까지 Index에 포함하면 Table 접근을 줄일 수 있다.

그러나 Index가 커지고 DML 비용이 증가한다.

모든 Column을 Index에 포함하지 않는다.

\---

\## 32.2.21 통계정보

실행계획은 통계에 의존한다.

\`\`\`text id="perf32085"
Table Row 수

Column 분포

Null 비율

Histogram

Index Cardinality

Partition 통계

테스트 DB가 운영 데이터량·분포와 다르면 실행계획도 달라질 수 있다.

## 32.2.22 Bind 변수와 데이터 편향

\`\`\`text id=“perf32086” branchId=A 전체의 60%

branchId=Z 전체의 0.01%



같은 SQL도 Bind 값에 따라 적합한 실행계획이 다를 수 있다.

대표값·최악값·경계값을 모두 시험한다.

\---

\## 32.2.23 Lock과 Transaction

SQL 자체는 빠르지만 Lock 대기로 느릴 수 있다.

\`\`\`text id="perf32087"
실행계획 정상

CPU 정상

Elapsed 10초

DB Lock Wait 9.8초

Index 튜닝보다 Transaction 범위와 Lock 순서를 개선해야 한다.

## 32.2.24 Query Timeout

Query Timeout은 장애 전파를 제한한다.

text id="perf32088" 느린 SQL → 3초 후 중단

그러나 Query Timeout은 SQL을 빠르게 만들지 않는다.

text id="perf32089" 성능개선 ≠ Timeout 증가·감소

Slow SQL 원인은 별도로 제거한다.

## 32.2.25 Fetch Size

현재 기본값 예:

text id="perf32090" defaultFetchSize 500

너무 작으면 Network Round Trip이 증가한다.

너무 크면 메모리 사용과 응답 시작 지연이 증가할 수 있다.

온라인 단건조회·목록·Batch에 동일 값을 강제하지 않는다.

## 32.2.26 Result Mapping

SQL은 빠르지만 Java Mapping이 느릴 수 있다.

\`\`\`text id=“perf32091” 대량 Map

Reflection

중첩 객체

JSON 직렬화

불필요한 변환



SQL 실행시간과 전체 Mapper·Service 시간을 구분한다.

\---

\## 32.2.27 SQL 튜닝 절차

\`\`\`text id="perf32092"
1\. Slow ServiceId 확보
2\. 대표 GUID 선택
3\. SQL ID 연결
4\. 실행시간·횟수 확인
5\. 실행계획 확인
6\. 실제·예상 Cardinality 비교
7\. 데이터분포 확인
8\. SQL·Index 대안 작성
9\. 단건·부하 시험
10\. DML·Batch 영향 확인
11\. 동일 조건 재시험
12\. 변경이력·Rollback 기록

# 32.3 Thread·Connection Pool

## 32.3.1 Thread와 Connection 역할

\`\`\`text id=“perf32093” Tomcat Thread → HTTP 요청을 처리

TCF Worker Thread → Timeout 제어 영역에서 업무 실행

DB Connection → DB SQL 실행

외부 Client Connection → 외부 HTTP 호출



Thread 하나가 항상 DB Connection 하나를 사용하는 것은 아니다.

하지만 DB 처리 중에는 일반적으로 Thread와 Connection을 함께 점유한다.

\---

\## 32.3.2 처리 흐름

\`\`\`text id="perf32094"
HTTP 요청

→ Tomcat Thread 획득

→ TCF Worker 획득

→ DB Connection 획득

→ SQL 실행

→ Connection 반환

→ 응답

→ Thread 반환

대기 가능 지점:

\`\`\`text id=“perf32095” Tomcat Queue

TCF Executor Queue

Hikari Pending

DB Lock

외부 Client Pool

Network


\---

\## 32.3.3 Tomcat Connector

주요 설정:

\`\`\`text id="perf32096"
maxThreads

minSpareThreads

maxConnections

acceptCount

connectionTimeout

keepAliveTimeout

각 값의 의미를 구분한다.

### maxThreads

동시에 요청을 실행할 수 있는 최대 Thread 수다.

### maxConnections

동시에 유지할 수 있는 Network Connection 수다.

### acceptCount

모든 처리 Thread가 사용 중일 때 대기할 연결 수다.

Queue가 크면 실패 대신 대기시간이 길어진다.

## 32.3.4 Thread 산정

기본 관계:

text id="perf32097" 필요 동시 처리 Thread ≈ TPS × 평균 응답시간

예:

\`\`\`text id=“perf32098” 720 TPS

평균 1.5초

약 1,080개 In-flight



그러나 모든 In-flight 요청이 동일 Tomcat Instance에 있지 않다.

\`\`\`text id="perf32099"
전체 4대 균등분산

Instance당
약 270건

안전율·편향·장애 시 잔여용량을 추가한다.

## 32.3.5 Thread를 늘리면 좋은 경우

\`\`\`text id=“perf32100” CPU 여유

DB Pool 여유

외부 시스템 여유

Queue 대기 존재

처리 작업이 주로 I/O 대기



실제 시험으로 효과를 확인한다.

\---

\## 32.3.6 Thread를 늘리면 나쁜 경우

\`\`\`text id="perf32101"
CPU 이미 포화

DB Pool 포화

DB가 병목

외부 Rate Limit

Memory 부족

Lock 경합

Thread만 늘리면 Context Switching과 대기만 증가할 수 있다.

## 32.3.7 Busy Thread

text id="perf32102" Busy Thread = 현재 실행 중인 요청 Thread

판단 예:

\`\`\`text id=“perf32103” Busy 40% 응답 정상 → 여유

Busy 90% Queue 증가 → 포화

Busy 90% CPU 낮음 → I/O·Pool·외부 대기

Busy 90% CPU 높음 → 계산·GC 가능성


\---

\## 32.3.8 acceptCount

큰 Queue:

\`\`\`text id="perf32104"
요청 거절 감소

사용자 대기 증가

Timeout 후 불필요한 처리 지속

작은 Queue:

\`\`\`text id=“perf32105” 빠른 실패

상위 시스템 Retry 가능

Burst 흡수 부족



전체 Timeout·Retry 정책과 함께 정한다.

\---

\## 32.3.9 Hikari Pool

주요 설정:

\`\`\`text id="perf32106"
maximumPoolSize

minimumIdle

connectionTimeout

validationTimeout

idleTimeout

maxLifetime

keepaliveTime

autoCommit

## 32.3.10 Pool 산정 원칙

잘못된 방식:

\`\`\`text id=“perf32107” Tomcat maxThreads 1,200

따라서 DB Pool도 1,200



DB는 1,200개 SQL을 효율적으로 동시에 실행하지 못할 수 있다.

Pool은 다음을 기준으로 한다.

\`\`\`text id="perf32108"
DB 처리능력

업무별 DB 사용비율

SQL 평균 보유시간

Peak TPS

WAR 수

Instance 수

DB 최대 Session

장애 시 잔여 서버

Batch·관리 Session 예약

## 32.3.11 Connection 동시 사용량

근사식:

text id="perf32109" 필요 Active Connection ≈ DB 사용 TPS × 평균 Connection 보유시간

예:

\`\`\`text id=“perf32110” 720 TPS

80%가 DB 사용 = 576 TPS

평균 Connection 보유시간 0.2초

평균 Active 약 115개



이는 전체 시스템 평균이다.

Peak·p95·편향·장애 안전율을 추가하되 DB 부하시험으로 검증해야 한다.

\---

\## 32.3.12 전체 Pool 합계

다중 WAR·다중 Instance 구조:

\`\`\`text id="perf32111"
WAR 9개

각 WAR Pool 120

Tomcat 4대

이론 최대
4,320 Connections

DB가 이를 수용할 수 있는지 확인해야 한다.

공식 계산:

text id="perf32112" 전체 최대 Pool = Σ( Instance별 × WAR별 × DataSource별 maximumPoolSize )

추가로 예약한다.

\`\`\`text id=“perf32113” DBA 관리 Session

Batch

Monitoring

Backup

Replication

긴급 여유


\---

\## 32.3.13 Pool 사용률만으로 부족하다

\`\`\`text id="perf32114"
Active
90%

Pending
0

Acquire p95
20ms

이면 정상 Peak일 수 있다.

\`\`\`text id=“perf32115” Active 70%

Pending 30

Acquire p95 2.5초



이면 Connection 생성·DB 응답·Pool 설정 문제를 확인해야 한다.

\---

\## 32.3.14 Hikari Pending

\`\`\`text id="perf32116"
Pending > 0 지속

은 사용자 Thread가 Connection을 기다리고 있다는 의미다.

원인:

\`\`\`text id=“perf32117” Pool 과소

Slow SQL

Lock

Connection Leak

Transaction 장기화

DB 장애


\---

\## 32.3.15 Connection Acquire Time

\`\`\`text id="perf32118"
전체 응답
3초

Connection Acquire
2초

SQL
100ms

SQL Index를 추가해도 효과가 작다.

Pool을 즉시 늘리기 전에 Connection 장기 점유 원인을 찾는다.

## 32.3.16 Connection Leak

징후:

\`\`\`text id=“perf32119” 거래 종료 후 Active 감소 안 됨

시간에 따라 Active 증가

TPS가 낮아도 Pool 포화

재기동 후 일시 정상



Spring Transaction과 MyBatis를 표준 사용하면 직접 Connection 관리보다 Leak 위험이 낮다.

직접 \`getConnection()\` 사용을 코드 Gate로 제한한다.

\---

\## 32.3.17 Transaction 범위

나쁜 구조:

\`\`\`text id="perf32120"
Transaction 시작

DB 조회

외부 API 3초

파일 처리

DB Update

Commit

Connection과 Lock을 오래 점유한다.

권장:

\`\`\`text id=“perf32121” 외부 조회

필요 데이터 준비

짧은 DB Transaction

Commit

후속 비동기 처리



업무 원자성을 유지하는 범위 안에서 축소한다.

\---

\## 32.3.18 Pool 크기 변경

예:

\`\`\`text id="perf32122"
maximumPoolSize
120 → 160

검증:

\`\`\`text id=“perf32123” DB Session 총합

DB CPU

SQL p95

Pool Pending

전체 TPS

오류율

다른 WAR 영향



Pool Pending이 줄었지만 DB CPU와 SQL p95가 급증하면 적절한 개선이 아니다.

\---

\## 32.3.19 minimumIdle

너무 작으면 Burst 시 Connection 생성지연이 발생할 수 있다.

너무 크면 사용하지 않는 DB Session이 많아진다.

운영 Peak와 Connection 생성비용을 측정한다.

\---

\## 32.3.20 maxLifetime

DB·Firewall·Network의 Connection 유효시간보다 짧게 설정한다.

모든 Connection이 동시에 폐기되지 않도록 Hikari의 분산동작을 활용하고 DB 정책과 정합성을 확인한다.

\---

\## 32.3.21 connectionTimeout

현재 기본 예:

\`\`\`text id="perf32124"
3초

전체 온라인 Timeout이 5초라면 Connection 획득에 3초를 소비하고 SQL·업무 처리를 위한 시간이 부족할 수 있다.

Timeout Budget을 함께 설계한다.

## 32.3.22 별도 Pool

다음 업무는 일반 온라인 Pool과 분리를 검토한다.

\`\`\`text id=“perf32125” Batch

대량 다운로드

운영 로그

감사로그

통계조회

외부 시스템 상태수집



분리하지 않으면 비핵심 작업이 온라인 거래 Connection을 점유할 수 있다.

\---

\## 32.3.23 Read·Write Pool

RDW·ADW 또는 Read·Write DataSource가 분리된 경우:

\`\`\`text id="perf32126"
조회 Pool

변경 Pool

Batch Pool

로그 Pool

을 각각 산정한다.

Pool 분리가 데이터 소유권이나 Transaction 문제를 자동 해결하지는 않는다.

## 32.3.24 JVM CPU

CPU 사용률:

\`\`\`text id=“perf32127” 낮음 + 응답 느림 → I/O·Lock·Pool 대기

높음 + RUNNABLE Thread 많음 → CPU 병목

높음 + GC 증가 → 메모리 할당·GC



VM CPU 평균만 보지 말고 Process CPU와 Steal·Load Average를 함께 확인한다.

\---

\## 32.3.25 Heap

Heap 확대 전에 확인:

\`\`\`text id="perf32128"
After-GC Heap

Old Gen 추세

Allocation Rate

Object 종류

Cache 크기

대형 byte\[\]

Session

ThreadLocal

ClassLoader

Heap 사용률이 높다는 이유만으로 무조건 확대하지 않는다.

## 32.3.26 GC

확인:

\`\`\`text id=“perf32129” GC Pause

GC Time Ratio

Young GC 빈도

Old GC

Full GC

Allocation Rate

Promotion Rate



목표는 GC 횟수를 0으로 만드는 것이 아니다.

사용자 응답과 처리량에 영향을 주지 않는 수준으로 관리한다.

\---

\## 32.3.27 큰 Heap의 위험

\`\`\`text id="perf32130"
Heap 확대

→ GC 빈도 감소 가능

→ 한 번의 Old GC 시간이 증가 가능

→ Dump·재기동 시간 증가

→ 문제 은폐

GC 알고리즘과 객체 수명을 함께 분석한다.

## 32.3.28 JVM 공유구조

하나의 Tomcat에 여러 WAR가 있으면 다음은 공유된다.

\`\`\`text id=“perf32131” CPU

Heap

GC

Metaspace

Connector Thread

Process



WAR별 JVM Heap을 합산하거나 각각 독립값처럼 해석하지 않는다.

업무별 지표는 다음으로 추정한다.

\`\`\`text id="perf32132"
ServiceId TPS

Active 거래

업무 Pool

Slow 거래

객체 할당 Profile

로그

## 32.3.29 Scale-out

AP 추가 전 확인:

\`\`\`text id=“perf32133” AP CPU가 병목인가?

L4 분산이 균등한가?

Session이 확장 가능한가?

DB가 추가 부하를 받을 수 있는가?

Cache Warm-up을 견디는가?

외부 Rate Limit이 있는가?


\---

\## 32.3.30 Scale-up

VM Core·Memory 증가:

\`\`\`text id="perf32134"
CPU 병목

GC 여유

License

NUMA

JVM Heap

운영비용

을 검토한다.

Scale-up 후에도 단일 장애영역이 커질 수 있다.

# 32.4 부하 테스트와 개선 검증

## 32.4.1 성능시험 유형

| 시험 | 목적 |
| --- | --- |
| Smoke | Script·환경 정상 확인 |
| Baseline | 단일·저부하 기준 측정 |
| Load | 목표부하 검증 |
| Peak | 예상 최고부하 |
| Stress | 한계·실패지점 |
| Spike | 순간 급증 |
| Soak | 장시간 안정성 |
| Scalability | 서버·자원 증가 효과 |
| Degradation | DB·외부 지연 |
| Failover | Instance·센터 장애 |
| Recovery | 부하 후 정상 복귀 |
| Batch Coexistence | Batch와 온라인 경합 |
| Deployment | Rolling 중 성능 |
| Volume | 대량 데이터·파일 |

## 32.4.2 시험 단계

\`\`\`text id=“perf32135” Smoke

→ Baseline

→ 점증부하

→ 목표부하

→ Peak

→ Stress

→ Soak

→ 장애·복구

→ 개선 재시험



바로 최대 부하를 가하면 어느 단계에서 포화가 시작됐는지 알기 어렵다.

\---

\## 32.4.3 점증부하

예:

\`\`\`text id="perf32136"
10%

25%

50%

75%

100%

120%

150%

각 구간에서 일정 시간 안정적으로 유지한다.

확인:

\`\`\`text id=“perf32137” TPS

p95

오류율

CPU

Thread

Pool

SQL


\---

\## 32.4.4 부하곡선

정상구간:

\`\`\`text id="perf32138"
요청 증가

TPS 비례 증가

응답시간 완만

오류 0

포화구간:

\`\`\`text id=“perf32139” 요청 증가

TPS 정체

p95 급증

Queue 증가

오류 증가



한계는 TPS가 가장 높았던 단일 순간이 아니라 안정적으로 유지 가능한 구간으로 판단한다.

\---

\## 32.4.5 Peak 시험

예상 최대 업무량을 일정 시간 유지한다.

\`\`\`text id="perf32140"
목표 TPS
720

유지
30~60분

다음이 안정적이어야 한다.

\`\`\`text id=“perf32141” p95

오류율

Heap

GC

Thread

Pool

DB CPU

외부 오류


\---

\## 32.4.6 Stress 시험

목적:

\`\`\`text id="perf32142"
처리한계

포화자원

실패형태

자동복구

Back Pressure

Stress 시험의 성공은 장애가 나지 않는 것이 아니다.

\`\`\`text id=“perf32143” 한계를 알 수 있다.

안전하게 실패한다.

회복할 수 있다.



가 성공조건이다.

\---

\## 32.4.7 Spike 시험

\`\`\`text id="perf32144"
100 TPS

5초 이내
1,000 TPS

30초 유지

100 TPS 복귀

확인:

\`\`\`text id=“perf32145” Queue

Connection 생성

Cache Stampede

Auto Scaling

오류

복구시간


\---

\## 32.4.8 Soak 시험

예:

\`\`\`text id="perf32146"
70~100% 목표부하

8시간·24시간·72시간

탐지:

\`\`\`text id=“perf32147” Memory Leak

Connection Leak

Thread Leak

Log Disk 증가

Cache Entry 증가

File Descriptor

Session 누적

Metric Cardinality


\---

\## 32.4.9 Failover 시험

\`\`\`text id="perf32148"
전체 4대

720 TPS 정상
↓
1대 중단
↓
잔여 3대에서 목표 유지

센터 장애:

\`\`\`text id=“perf32149” A센터 중단

B센터만으로 필수 목표 처리



DR 상태에서는 정상 상태와 다른 성능목표를 승인할 수 있지만 공식 기준이 필요하다.

\---

\## 32.4.10 장애주입

\`\`\`text id="perf32150"
DB 지연

DB Pool 제한

외부 5초 지연

외부 50% 오류

Cache 장애

AP 1대 중단

Network 지연

Disk Full 경고

확인:

\`\`\`text id=“perf32151” Timeout

Circuit

Bulkhead

거래통제

오류 응답

자원 반환

복구


\---

\## 32.4.11 시험환경

운영과 유사해야 하는 항목:

\`\`\`text id="perf32152"
CPU·Memory

AP 대수

Tomcat 구조

WAR 수

JDK·JVM 옵션

DB Engine·Version

데이터 건수·분포

Index·통계

Network

외부 지연

보안 Agent

로그 Level

완전 동일환경이 불가능하면 차이와 보정방법을 기록한다.

## 32.4.12 테스트 데이터

\`\`\`text id=“perf32153” 평균 사용자

고객 데이터 많은 사용자

History 많은 고객

특정 지점 편향

Null·경계값

중복·권한

최악 검색조건



균일한 가짜 데이터만 사용하면 운영의 편향된 분포를 재현하지 못한다.

\---

\## 32.4.13 데이터 Cache 효과

시험 반복 시 DB Buffer Cache·애플리케이션 Cache가 Warm해진다.

\`\`\`text id="perf32154"
Cold 시험

Warm 시험

을 분리한다.

각 시험 전 Cache 초기화 여부를 기록한다.

## 32.4.14 부하 발생기

확인:

\`\`\`text id=“perf32155” 발생기 CPU

Memory

Network

Connection 수

오류율

시간동기화



부하 발생기 CPU가 100%라면 목표 TPS를 만들지 못할 수 있다.

분산 부하 발생기를 사용한다.

\---

\## 32.4.15 테스트 Script

Script는 실제 다음 흐름을 반영한다.

\`\`\`text id="perf32156"
로그인·Token

Header

ServiceId

거래 Mix

Think Time

데이터 상관관계

멱등 Key

오류 검증

Logout

HTTP 200만으로 성공을 판단하지 않는다.

\`\`\`text id=“perf32157” HTTP 200

Body resultCode E-SV-0001



일 수 있다.

\---

\## 32.4.16 성공 판정

\`\`\`text id="perf32158"
HTTP Status

표준 결과코드

업무 결과

데이터 반영

오류율

Timeout

중복·Rollback

을 함께 확인한다.

## 32.4.17 변경거래 부하

등록·변경거래는 다음을 검증한다.

\`\`\`text id=“perf32159” Idempotency

Unique Constraint

Version 충돌

Lock

Commit

Rollback

감사로그

데이터 증가량



성능을 높이기 위해 정합성 검증을 제거하지 않는다.

\---

\## 32.4.18 로그 부하

성능시험에서도 운영과 동일한 수준의 거래로그·감사로그·Metric을 활성화해야 한다.

\`\`\`text id="perf32160"
성능시험
로그 비활성

운영
로그 활성

이면 결과가 달라질 수 있다.

DEBUG 전체 로그는 별도 진단시험에서만 사용한다.

## 32.4.19 외부 모의시스템

외부 연계 Stub은 다음을 제어할 수 있어야 한다.

\`\`\`text id=“perf32161” 응답시간

오류율

Timeout

Connection Reset

중복 응답

지연 Callback



항상 즉시 성공하는 Stub만 사용하지 않는다.

\---

\## 32.4.20 개선 전후 비교

| 항목 | Before | After | 변화 |
|---|---:|---:|---:|
| TPS | 600 | 760 | +26.7% |
| p95 | 4.2초 | 1.8초 | -57.1% |
| p99 | 12초 | 3.5초 | -70.8% |
| 오류율 | 1.2% | 0.1% | 개선 |
| CPU | 55% | 62% | 증가 |
| Pool Pending | 40 | 0 | 개선 |
| SQL p95 | 3.3초 | 0.4초 | 개선 |
| DML p95 | 0.3초 | 0.5초 | 일부 악화 |

조회는 개선됐지만 DML이 악화됐으므로 Index 비용을 추가 검토한다.

\---

\## 32.4.21 통계적 반복

한 번의 시험 결과만 사용하지 않는다.

\`\`\`text id="perf32162"
동일 시나리오

3회 이상

중앙값·편차 비교

다른 Job·백업·통계수집 등 외부요인을 기록한다.

## 32.4.22 성능 회귀

CI에서 모든 대규모 부하시험을 매 Commit 실행하기 어렵다.

단계화한다.

\`\`\`text id=“perf32163” PR → Micro Benchmark·SQL Plan·간단 Smoke

Nightly → 주요 ServiceId Load

Release → 전체 Mix·Peak·Soak

운영 전환 → 장애·DR 포함


\---

\## 32.4.23 Performance Gate

운영 후보 Release는 다음을 충족해야 한다.

\`\`\`text id="perf32164"
목표 TPS

p95·p99

오류율

Timeout율

CPU·Heap·GC

Busy Thread

Pool Pending

Slow SQL

외부연계

정합성

장애 회복

성능·부하·장시간·Pool·장애 시험은 운영 후보 Release의 공식 검증 산출물로 관리해야 한다.

## 32.4.24 미달 처리

\`\`\`text id=“perf32165” 목표 미달

→ 병목 계층 분리

→ 설정문제·코드문제·구조문제 구분

→ 용량가정 재검토

→ 개선안 적용

→ 동일 시나리오 재시험



성능 미달 시 CPU·GC·Thread·Pool·SQL·연계 원인을 분해하고 같은 조건으로 다시 시험해야 한다.

\---

\# 성능시험 표준 시나리오

| ID | 시나리오 | 부하 | 목적 |
|---|---|---:|---|
| PERF-BASE-01 | 단건조회 Baseline | 1 User | 단일 지연 |
| PERF-BASE-02 | 복합조회 Baseline | 1 User | 구간분해 |
| PERF-LOAD-01 | 25% 부하 | 180 TPS | 초기 추세 |
| PERF-LOAD-02 | 50% 부하 | 360 TPS | 중간 추세 |
| PERF-LOAD-03 | 75% 부하 | 540 TPS | 포화 전 |
| PERF-LOAD-04 | 목표 부하 | 720 TPS | NFR |
| PERF-PEAK-01 | 설계 Peak | 목표 기준 | Peak 유지 |
| PERF-STRESS-01 | 120% | 864 TPS | 한계 |
| PERF-STRESS-02 | 150% | 1,080 TPS | 실패형태 |
| PERF-SPIKE-01 | 순간 급증 | 2배 | Burst |
| PERF-SOAK-01 | 장시간 | 70% | 누수 |
| PERF-SOAK-02 | 장시간 Peak | 100% | 안정성 |
| PERF-FAIL-01 | AP 1대 중단 | 720 TPS | Failover |
| PERF-FAIL-02 | 외부 지연 | 720 TPS | 격리 |
| PERF-FAIL-03 | DB 지연 | 720 TPS | Pool |
| PERF-CACHE-01 | Cache Cold | 목표 | Cold Start |
| PERF-BATCH-01 | Batch 동시 | 목표 | 자원경합 |
| PERF-DEPLOY-01 | Rolling | 목표 | 배포 영향 |
| PERF-DR-01 | 단일센터 | 기준 | DR 용량 |

TPS는 최종 승인 기준으로 변경한다.

\---

\# 정상 분석 흐름

\`\`\`text id="perf32166"
1\. 느린 ServiceId를 식별한다.
2\. 대표 GUID를 확보한다.
3\. 전체 응답시간을 계층별로 분해한다.
4\. CPU·GC·Thread·Pool 상태를 같은 시간축으로 확인한다.
5\. DB Connection 대기와 SQL 실행시간을 구분한다.
6\. SQL ID와 실행계획을 확인한다.
7\. 외부 호출과 Cache·Batch 영향을 확인한다.
8\. 가장 먼저 포화된 자원을 결정한다.
9\. 개선 가설과 예상효과를 기록한다.
10\. 한 가지 변경을 적용한다.
11\. 같은 데이터·부하·시간으로 재시험한다.
12\. 처리량·지연·오류·자원·정합성을 비교한다.
13\. 운영 설정과 Alert를 갱신한다.

# 성능 미달 흐름

\`\`\`text id=“perf32167” 목표 720 TPS

실제 580 TPS

p95 4.5초 ↓ Pool Pending 0

CPU 92%

RUNNABLE Thread 증가 ↓ CPU Profile ↓ JSON 변환·대량 객체 생성 ↓ DTO 축소·변환 개선 ↓ 동일 시험 ↓ TPS 750 p95 2.2초


\---

\# DB Pool 병목 흐름

\`\`\`text id="perf32168"
p95 증가
↓
Busy Thread 증가
↓
Hikari Active=max
Pending 증가
↓
Connection Acquire 2초
↓
Slow SQL·Lock·Leak 구분
↓
원인 SQL 개선
↓
Pending 0

# SQL 튜닝 흐름

text id="perf32169" Slow ServiceId ↓ Mapper SQL ID ↓ 실행계획 ↓ 예상·실제 Row 차이 ↓ 검색조건·Index 개선 ↓ 조회·DML 부하시험 ↓ Plan·p95 비교

# JVM 병목 흐름

\`\`\`text id=“perf32170” p99 급증

CPU 증가

GC Pause 일치 ↓ Allocation Profile ↓ 대형 List·byte\[\] 확인 ↓ Streaming·Paging 적용 ↓ GC·p99 재시험


\---

\# 정상 예시

\`\`\`text id="perf32171"
요구사항
SV.Customer.selectSummary
목표 p95 3초

Baseline
720 TPS
p95 4.1초
p99 9.8초
오류율 0.8%

관찰
CPU 48%
Heap 58%
GC 정상
Busy Thread 82%
Hikari Active 120/120
Pending 35
Connection Acquire p95 2.6초
SQL p95 1.9초

원인
고객이력 SQL Full Scan
Connection 장기점유

개선
조회기간 필수화
복합 Index 추가
불필요한 Column 제거

재시험
720 TPS
p95 1.7초
p99 2.9초
오류율 0.05%
Pending 0
SQL p95 320ms

부작용
변경 SQL p95 70ms 증가

판정
조회 개선효과가 크며
변경 성능은 허용기준 이내

결과
PASS

# 금지 예시

\`\`\`text id=“perf32172” 사용자 한 명이 빠르다고 성능 PASS 처리한다.

평균 응답시간만 확인한다.

p95와 p99를 측정하지 않는다.

HTTP 200만 성공으로 계산한다.

오류 거래를 응답시간 통계에서 제외한다.

Warm-up 구간을 본 결과로 사용한다.

운영과 다른 H2 결과를 운영 성능으로 인정한다.

테스트 데이터가 100건인데 운영 1억 건 성능을 판단한다.

부하 발생기 CPU가 100%인데 서버 한계로 판단한다.

거래 Mix 없이 단건조회만 시험한다.

ServiceId 구분 없이 전체 평균만 본다.

느린 구간을 측정하지 않고 Index부터 추가한다.

Full Scan이라는 이유만으로 무조건 Index를 만든다.

Index 추가 후 INSERT·UPDATE 성능을 확인하지 않는다.

SELECT \*를 그대로 사용한다.

N+1 SQL을 다수의 빠른 SQL이라고 허용한다.

Query Timeout을 늘려 Slow SQL을 해결했다고 판단한다.

Tomcat Thread와 Hikari Pool을 같은 크기로 설정한다.

DB 처리능력 확인 없이 모든 WAR Pool을 120으로 설정한다.

Pool 사용률만 보고 Pending을 확인하지 않는다.

Pool 고갈 시 maximumPoolSize를 즉시 두 배로 늘린다.

CPU 포화상태에서 Thread를 계속 늘린다.

Heap을 늘려 Memory Leak을 숨긴다.

튜닝 중 Index·Pool·Heap을 동시에 변경한다.

튜닝 전 Baseline을 저장하지 않는다.

개선 후 다른 부하조건으로 시험한다.

성능이 좋아졌다는 이유로 정합성 테스트를 생략한다.

Stress 시험에서 장애가 발생했다는 이유만으로 실패 처리한다.

Soak 시험 없이 Memory·Connection Leak이 없다고 판단한다.

AP Scale-out 후 DB 부하를 확인하지 않는다.

성능결과에 Version·설정·데이터 조건을 기록하지 않는다.


\---

\# 책임 경계와 RACI

| 활동 | 업무개발 | FW | AA | DBA | TA | 성능QA | 운영 | 외부연계 |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| 성능 NFR | C | C | A | C | C | R | C | C |
| 거래 Mix | A/R | I | C | I | I | R/C | C | C |
| 테스트 Script | R/C | C | C | I | I | A/R | I | C |
| SQL 분석 | R | I | C | A/R | I | C | C | I |
| Index 설계 | C | I | C | A/R | I | C | I | I |
| Thread 설정 | C | R/C | A/C | I | A/R | C | R/C | I |
| Hikari Pool | C | R/C | A | A/R | C | C | R/C | I |
| JVM·GC | I | C | C | I | A/R | R/C | R | I |
| 외부성능 | C | C | A/C | I | C | C | C | A/R |
| Cache·Batch 영향 | R/C | R/C | A | C | C | C | R | I |
| 시험 실행 | C | C | C | C | C | A/R | R/C | C |
| 결과 판정 | C | C | A | C | C | R | C | C |
| 운영 임계치 | C | C | A/C | C | R/C | C | A/R | C |
| 설정 변경 | C | R | A | C | A/R | C | R/C | I |
| Performance Gate | C | R/C | A | C | C | R | C | C |

\---

\# 성능 Metric

\## 거래

\`\`\`text id="perf32173"
tcf.transaction.count

tcf.transaction.duration

tcf.transaction.error

tcf.transaction.timeout

tcf.transaction.inflight

## Tomcat

\`\`\`text id=“perf32174” tomcat.threads.busy

tomcat.threads.current

tomcat.threads.max

tomcat.connections.current

tomcat.queue.size


\## Hikari

\`\`\`text id="perf32175"
hikaricp.connections.active

hikaricp.connections.idle

hikaricp.connections.pending

hikaricp.connections.max

hikaricp.connections.acquire

hikaricp.connections.timeout

## JVM

\`\`\`text id=“perf32176” process.cpu.usage

system.cpu.usage

jvm.memory.used

jvm.memory.max

jvm.gc.pause

jvm.threads.live

jvm.classes.loaded

process.files.open


\## SQL

\`\`\`text id="perf32177"
sql.execution.count

sql.execution.duration

sql.timeout

sql.rows.read

sql.rows.affected

sql.error

## 외부연계

\`\`\`text id=“perf32178” integration.call.count

integration.call.duration

integration.timeout

integration.retry

integration.circuit.open


\---

\# 운영 임계값 예

| 지표 | 정상 | 주의 | 위험 |
|---|---:|---:|---:|
| p95 | 목표 이내 | 목표 80~100% | 목표 초과 |
| 오류율 | Baseline | 증가 추세 | SLA 초과 |
| Busy Thread | <70% | 70~85% | >85% 지속 |
| Heap After GC | <70% | 70~80% | >80% 지속 |
| GC Time Ratio | <5% | 5~10% | >10% |
| Hikari 사용률 | <70% | 70~85% | >85% |
| Hikari Pending | 0 | 순간 발생 | 지속 발생 |
| Acquire p95 | Baseline | 증가 | Timeout 근접 |
| CPU | <70% | 70~85% | >85% 지속 |
| SQL p95 | Budget 이내 | 근접 | 초과 |

예시값이며 성능시험 Baseline으로 확정한다.

\---

\# 자동검증 및 품질 Gate

\## 1. SQL Gate

\`\`\`text id="perf32179"
SELECT \*

조건 없는 UPDATE·DELETE

최대 조회건수 없음

Query Timeout 없음

불안정 정렬

N+1 패턴

함수 적용 Index Column

암묵적 형변환

대량 Offset

실행계획 증적 없음

## 2\. Thread Gate

\`\`\`text id=“perf32180” 무제한 Executor

newCachedThreadPool

Queue 상한 없음

Rejected Policy 없음

Graceful Shutdown 없음

Metric 없음


\---

\## 3. Pool Gate

\`\`\`text id="perf32181"
WAR별 maximumPoolSize

Instance별 Pool 합계

DB 최대 Session 초과

connectionTimeout Budget 위반

Pool 이름 중복

Pending Metric 없음

Batch·온라인 Pool 미분리

## 4\. JVM Gate

\`\`\`text id=“perf32182” Heap 산정 근거

GC Log

OOM Dump 경로

Container·VM Memory 초과

Metaspace 상한

Direct Memory

File Descriptor


\---

\## 5. 시험 Gate

\`\`\`text id="perf32183"
시험 Version

환경 차이

데이터 규모

거래 Mix

Warm-up

목표부하

p95·p99

오류·Timeout

자원 Metric

데이터 정합성

반복시험

## 6\. 변경 Gate

성능 관련 설정 변경 시:

\`\`\`text id=“perf32184” 변경 전 값

변경 후 값

근거

예상효과

부작용

시험결과

Rollback


\---

\# 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
|---|---|---|
| PERF-001 | 단일 사용자 Baseline | 구간별 시간 확보 |
| PERF-002 | 25% 목표부하 | 선형 증가 |
| PERF-003 | 50% 목표부하 | 안정 |
| PERF-004 | 75% 목표부하 | 안정 |
| PERF-005 | 100% 목표부하 | NFR 충족 |
| PERF-006 | 120% Stress | 포화지점 확인 |
| PERF-007 | 150% Stress | 안전 실패 |
| PERF-008 | Spike 2배 | Queue·복구 확인 |
| PERF-009 | 8시간 Soak | 누수 없음 |
| PERF-010 | 24시간 Soak | 자원 안정 |
| PERF-011 | AP 1대 중단 | 잔여 처리 |
| PERF-012 | 센터 1개 중단 | DR 기준 |
| PERF-013 | Cold Start | 초기 성능 |
| PERF-014 | Warm 상태 | 정상 성능 |
| PERF-015 | Cache 전체 Miss | DB 보호 |
| PERF-016 | Batch 동시 실행 | 온라인 NFR |
| PERF-017 | 파일 다운로드 동시 | Thread·대역폭 |
| PERF-018 | 외부 2초 지연 | Timeout Budget |
| PERF-019 | 외부 50% 오류 | Circuit |
| PERF-020 | DB 1초 지연 | Pool 반응 |
| PERF-021 | Hikari Max 10 | 한계 측정 |
| PERF-022 | Pool 증가 | DB 영향 |
| PERF-023 | Pool 과대 | Session·Idle 확인 |
| PERF-024 | Pending 발생 | 원인 분석 |
| PERF-025 | Connection Leak | Soak 탐지 |
| PERF-026 | Tomcat Thread 부족 | Queue 증가 |
| PERF-027 | Thread 증가 | 효과·CPU 비교 |
| PERF-028 | CPU 포화 | Thread 증가 금지 |
| PERF-029 | 무제한 Executor | Thread 폭증 |
| PERF-030 | Bounded Executor | Reject 제어 |
| PERF-031 | Heap 4GB | Baseline |
| PERF-032 | Heap 확대 | GC 비교 |
| PERF-033 | Memory Leak | After-GC 증가 |
| PERF-034 | 대형 byte\[\] | Allocation 증가 |
| PERF-035 | Streaming 전환 | Heap 감소 |
| PERF-036 | Full Scan 소형 Table | 허용 |
| PERF-037 | Full Scan 대형 Table | 실패 |
| PERF-038 | Index 추가 | 조회 개선 |
| PERF-039 | Index 추가 후 DML | 비용 검증 |
| PERF-040 | 중복 Index | 제거 검토 |
| PERF-041 | N+1 101회 SQL | 탐지 |
| PERF-042 | Batch IN 조회 | SQL 감소 |
| PERF-043 | SELECT \* | Gate 실패 |
| PERF-044 | 필요한 Column만 | 전송 감소 |
| PERF-045 | 함수 조건 | Index 미사용 |
| PERF-046 | 범위조건 변경 | Plan 개선 |
| PERF-047 | 암묵적 형변환 | Plan 불안정 |
| PERF-048 | Bind 편향 | 실행계획 비교 |
| PERF-049 | 통계 오래됨 | Cardinality 오류 |
| PERF-050 | 통계 갱신 | Plan 검증 |
| PERF-051 | Offset 깊은 Page | 지연 증가 |
| PERF-052 | Keyset Page | 안정 |
| PERF-053 | Count SQL 병목 | 대안 적용 |
| PERF-054 | Lock 대기 | SQL Plan과 구분 |
| PERF-055 | Transaction 외부호출 | Pool 장기점유 |
| PERF-056 | Transaction 축소 | Pending 개선 |
| PERF-057 | Query Timeout 3초 | 안전 중단 |
| PERF-058 | Timeout 10초 확대 | 자원 악화 |
| PERF-059 | Fetch Size 10 | Round Trip 증가 |
| PERF-060 | Fetch Size 과대 | Heap 증가 |
| PERF-061 | Mapper 변환 병목 | Java Profile |
| PERF-062 | JSON 대형응답 | 직렬화 지연 |
| PERF-063 | 응답 필드 축소 | 지연 개선 |
| PERF-064 | Gateway 인증 부하 | Gateway Scale |
| PERF-065 | JWT Key 조회 반복 | JWKS Cache |
| PERF-066 | 로그 DEBUG 전체 | 성능 영향 |
| PERF-067 | 운영 로그 Level | 기준 성능 |
| PERF-068 | Metric Cardinality 증가 | 관측 부하 |
| PERF-069 | 부하발생기 CPU 포화 | 시험 무효 |
| PERF-070 | 분산 부하발생기 | 목표 달성 |
| PERF-071 | HTTP 200 업무실패 | 실패 집계 |
| PERF-072 | 변경거래 중복 | 멱등성 |
| PERF-073 | Rollback 부하 | 정합성 |
| PERF-074 | Rolling 배포 | NFR 유지 |
| PERF-075 | 신규 Version 회귀 | Gate 실패 |
| PERF-076 | 개선 전후 3회 반복 | 결과 신뢰 |
| PERF-077 | 다른 데이터로 재시험 | 차이 기록 |
| PERF-078 | Metric 누락 | Gate 실패 |
| PERF-079 | Performance Report | 증적 완료 |
| PERF-080 | 운영 Baseline 반영 | Alert 갱신 |

\---

\# 따라 하는 실무 절차

\## 1단계. 성능 목표를 확정한다

\`\`\`text id="perf32185"
사용자

TPS

거래 Mix

p95·p99

오류율

가용성

완료시간

## 2단계. Baseline을 측정한다

\`\`\`text id=“perf32186” Version

설정

데이터

거래

자원


\## 3단계. 대표 Slow ServiceId를 선택한다

GUID로 Gateway부터 SQL·외부까지 추적한다.

\## 4단계. 처리시간을 분해한다

\`\`\`text id="perf32187"
Queue

Thread

Connection

SQL

외부

업무

응답

## 5단계. 가장 먼저 포화된 자원을 식별한다

CPU·GC·Thread·Pool·SQL·외부를 비교한다.

## 6단계. 튜닝 가설을 작성한다

\`\`\`text id=“perf32188” 관찰

원인

변경

예상효과

부작용


\## 7단계. 한 가지 변경을 적용한다

\## 8단계. 같은 부하로 재시험한다

데이터·Script·환경·측정구간을 동일하게 유지한다.

\## 9단계. 결과와 정합성을 비교한다

\`\`\`text id="perf32189"
TPS

p95·p99

오류

자원

데이터

## 10단계. 운영 설정과 Alert를 반영한다

승인·Rollback과 함께 관리한다.

# 완료 체크리스트

## 목표·Baseline

| 확인 항목 | 완료 |
| --- | --- |
| 등록 사용자와 동시 요청을 구분했다. | □ |
| Think Time이 정의됐다. | □ |
| 목표 TPS가 있다. | □ |
| 거래 Mix가 있다. | □ |
| p95·p99 목표가 있다. | □ |
| 오류·Timeout 기준이 있다. | □ |
| Baseline Version이 있다. | □ |
| 테스트 데이터 규모가 있다. | □ |
| 환경 차이를 기록했다. | □ |

## 분석

| 확인 항목 | 완료 |
| --- | --- |
| ServiceId별 성능을 측정한다. | □ |
| 대표 GUID를 확보했다. | □ |
| 처리시간을 구간별로 분해했다. | □ |
| CPU·Heap·GC를 확인했다. | □ |
| Busy Thread·Queue를 확인했다. | □ |
| Pool Active·Idle·Pending을 확인했다. | □ |
| Connection Acquire를 확인했다. | □ |
| SQL ID·실행계획을 확인했다. | □ |
| 외부 연계시간을 확인했다. | □ |
| Cache·Batch 영향을 확인했다. | □ |

## SQL·Index

| 확인 항목 | 완료 |
| --- | --- |
| 실제·예상 Cardinality를 비교했다. | □ |
| 데이터분포를 확인했다. | □ |
| SELECT \*가 없다. | □ |
| N+1이 없다. | □ |
| 최대 조회건수가 있다. | □ |
| 정렬·페이징이 안정적이다. | □ |
| Query Timeout이 있다. | □ |
| Index의 DML 비용을 검증했다. | □ |
| 중복 Index를 확인했다. | □ |
| 실행계획 증적을 보존했다. | □ |

## Thread·Pool·JVM

| 확인 항목 | 완료 |
| --- | --- |
| Thread 산정근거가 있다. | □ |
| acceptCount 근거가 있다. | □ |
| Executor Queue 상한이 있다. | □ |
| Rejected Policy가 있다. | □ |
| WAR별 Pool이 식별된다. | □ |
| 전체 Pool 합계가 DB 한도 이내다. | □ |
| Batch·온라인 Pool을 검토했다. | □ |
| Heap 산정근거가 있다. | □ |
| GC Log를 분석했다. | □ |
| Scale-out 시 DB 영향을 검증했다. | □ |

## 시험·검증

| 확인 항목 | 완료 |
| --- | --- |
| Warm-up과 본 측정을 구분했다. | □ |
| 점증부하를 수행했다. | □ |
| 목표 Peak를 유지했다. | □ |
| Stress 한계를 확인했다. | □ |
| Spike 복구를 확인했다. | □ |
| Soak 누수를 확인했다. | □ |
| 장애·Failover를 시험했다. | □ |
| 개선 전후 조건이 같다. | □ |
| 3회 이상 반복했다. | □ |
| 데이터 정합성을 검증했다. | □ |
| 결과서와 Raw Data를 보존했다. | □ |

# 성능시험 결과서 표준

## 기본정보

| 항목 | 값 |
| --- | --- |
| 시험 ID |  |
| Release Version |  |
| Commit ID |  |
| Config Version |  |
| 시험환경 |  |
| 시험일시 |  |
| 담당자 |  |
| 데이터 기준일 |  |

## 부하조건

| 항목 | 값 |
| --- | --- |
| 목표 TPS |  |
| 동시 사용자 |  |
| Think Time |  |
| Ramp-up |  |
| 유지시간 |  |
| 거래 Mix |  |
| AP 대수 |  |
| DB 구성 |  |

## 결과

| 지표 | 목표 | 결과 | 판정 |
| --- | --- | --- | --- |
| TPS |  |  |  |
| p50 |  |  |  |
| p95 |  |  |  |
| p99 |  |  |  |
| 오류율 |  |  |  |
| Timeout율 |  |  |  |
| CPU |  |  |  |
| Heap |  |  |  |
| GC Pause |  |  |  |
| Busy Thread |  |  |  |
| Pool Pending | 0 |  |  |
| SQL p95 |  |  |  |

## 병목과 조치

| 병목 | 근거 | 조치 | 개선 결과 |
| --- | --- | --- | --- |
|  |  |  |  |

# 변경·호환성·폐기 관리

## Thread 변경

\`\`\`text id=“perf32190” 변경 전 maxThreads

변경 후 maxThreads

목표 TPS

CPU·Pool 영향

Queue

Rollback


\## Pool 변경

\`\`\`text id="perf32191"
WAR별 값

Instance 수

총합

DB Session 한도

Peak Active

Pending

Rollback

## JVM 변경

\`\`\`text id=“perf32192” Xms·Xmx

GC Algorithm

Pause 목표

Metaspace

Direct Memory

Container·VM Memory

재기동


\## Index 변경

\`\`\`text id="perf32193"
대상 SQL

기존 Plan

신규 Plan

조회효과

DML 영향

Storage

통계

Rollback DDL

## SQL 변경

Contract·정렬·Null·영향 행 수가 바뀌지 않는지 기능회귀를 수행한다.

## Baseline 변경

운영 Version·데이터 증가·업무량 변화 시 Baseline을 갱신한다.

기존 결과를 덮어쓰지 않고 Version별로 보존한다.

## Metric 폐기

Dashboard와 Alert 호출이 모두 전환된 뒤 폐기한다.

## 성능 예외 승인

목표를 충족하지 못한 채 운영해야 한다면 다음을 기록한다.

\`\`\`text id=“perf32194” 미달 항목

사용자 영향

임시 통제

거래통제·Rate Limit

Owner

개선기한

승인자

만료일


\---

\# 시사점

\## 핵심 아키텍처 판단

첫째, 성능 튜닝의 출발점은 설정 변경이 아니라 Baseline과 대표 Slow 거래 확보다.

둘째, TPS·응답시간·오류율·자원 사용은 하나의 세트로 판단해야 한다.

셋째, 평균 응답시간만으로는 Tail 지연과 일부 사용자 장애를 발견할 수 없으므로 p95·p99가 필요하다.

넷째, Thread와 Connection Pool은 서로 다른 자원이며 같은 크기로 설정해서는 안 된다.

다섯째, Pool을 크게 만들면 애플리케이션 대기는 줄 수 있지만 DB가 처리할 수 없는 SQL을 더 많이 전달해 전체 장애를 확대할 수 있다.

여섯째, Index는 조회성능과 DML 비용을 교환하는 구조이므로 읽기와 쓰기를 함께 시험해야 한다.

일곱째, Scale-out은 AP 병목에는 효과적이지만 DB·외부시스템 병목에는 효과가 없거나 부하를 악화시킬 수 있다.

여덟째, 성능시험은 목표부하 통과뿐 아니라 포화지점·실패형태·회복능력·장시간 누수까지 설명해야 한다.

\---

\## 주요 위험

| 위험 | 결과 |
|---|---|
| Baseline 없음 | 개선효과 증명 불가 |
| 평균만 사용 | Tail 지연 누락 |
| 거래 Mix 오류 | 운영 재현 실패 |
| 테스트 데이터 부족 | 잘못된 실행계획 |
| 부하발생기 병목 | 서버 결과 왜곡 |
| Warm-up 혼입 | 결과 변동 |
| SQL ID 추적 없음 | 병목 확인 불가 |
| Index 남용 | DML 저하 |
| N+1 | DB 호출 폭증 |
| Pool 과대 | DB Session 고갈 |
| Pool 과소 | Pending·Timeout |
| Thread 과대 | Context Switching·대기 |
| 무제한 Executor | Thread 폭증 |
| Heap 과대 | 긴 GC·복구 지연 |
| Heap 과소 | OOM·GC 압박 |
| Scale-out 남용 | DB 장애 확대 |
| Timeout 확대 | 장기 자원점유 |
| 동시 다중 튜닝 | 원인·효과 불명 |
| Soak 미실행 | Leak 운영 발견 |
| 정합성 미검증 | 빠른 잘못된 처리 |

\---

\## 우선 보완 과제

1\. ServiceId별 Micrometer Counter·Timer를 실제 구현한다.
2\. p50·p95·p99·오류율·Timeout Dashboard를 구축한다.
3\. Tomcat Busy Thread·Queue·Connection 지표를 수집한다.
4\. Hikari Idle·Pending·Acquire Time·Timeout Count를 수집한다.
5\. SQL ID별 Count·p95·Row Count를 ServiceId와 연결한다.
6\. 현재 공통 Pool 10과 Heap 1~4GB 샘플을 운영 최종값과 명확히 분리한다.
7\. WAR·Instance·DataSource별 전체 DB Pool 합계 검증기능을 구현한다.
8\. 무제한 Timeout Executor를 Bounded Pool로 전환한다.
9\. SQL 실행계획·대량조회·SELECT \*·Query Timeout을 CI Gate로 적용한다.
10\. 36,000명·동시성·720 TPS·p95 3초의 산정 가정을 하나의 용량모델에서 재검증한다.
11\. 실제 거래 Mix 기반 표준 성능 Script를 작성한다.
12\. Peak·Stress·Spike·Soak·Failover 시나리오를 자동화한다.
13\. 운영과 유사한 데이터량·분포·DB 통계를 구성한다.
14\. 튜닝 변경대장에 Before·After·부작용·Rollback을 기록한다.
15\. Performance Gate 결과를 운영전환 Go/No-Go와 연결한다.

\---

\## 중장기 발전 방향

\`\`\`text id="perf32195"
평균 응답시간
↓
ServiceId p95·p99

거래 전체시간
↓
구간별 Span·Budget

Active·Max Pool
↓
Idle·Pending·Acquire

CPU·Heap 단순 상태
↓
GC·Allocation·Profile

수동 실행계획 확인
↓
SQL Performance Gate

개별 부하시험
↓
자동 회귀·Nightly Test

최대 TPS
↓
안정 TPS·포화곡선

단일 환경시험
↓
Failover·DR·Batch 공존

경험 기반 튜닝
↓
측정·가설·검증 기반 튜닝

설정값 문서
↓
실측 기반 Configuration Baseline

# 마무리말

성능 분석과 튜닝을 수행하는 과정은 다음 질문에 답하는 일이다.

\`\`\`text id=“perf32196” 등록 사용자와 실제 동시 요청은 얼마인가?

목표 TPS와 응답시간의 계산조건이 일치하는가?

어떤 ServiceId가 전체 부하의 몇 퍼센트를 차지하는가?

평균·p95·p99와 오류율은 얼마인가?

응답시간 중 어느 구간에서 시간이 소요되는가?

DB Connection을 기다리는가, SQL을 실행하는가?

어떤 Mapper SQL ID가 느린가?

실행계획의 예상 Row와 실제 Row가 일치하는가?

Index를 추가하면 DML과 Batch에는 어떤 영향이 있는가?

Tomcat Thread가 실제 병목인가?

Hikari Pending과 Acquire Time은 얼마인가?

모든 WAR Pool 합계가 DB Session 한도를 초과하지 않는가?

CPU가 낮은데 느리다면 무엇을 기다리고 있는가?

Heap 확대보다 먼저 어떤 객체가 증가하는지 확인했는가?

Cache·Batch·파일이 온라인 성능을 저하시키지 않는가?

목표부하에서 얼마나 오래 안정적으로 유지되는가?

어느 부하에서 처리량이 정체되고 오류가 증가하는가?

AP 한 대나 센터 한 곳이 중단돼도 목표를 유지하는가?

튜닝 전후 시험조건이 정말 같은가?

성능이 개선된 뒤 데이터 정합성과 보안도 유지되는가?

운영자는 같은 지표로 성능저하를 조기에 탐지할 수 있는가?



제32장의 핵심 흐름은 다음과 같다.

\`\`\`text id="perf32197"
목표·가정
↓
Baseline
↓
구간별 측정
↓
병목 식별
↓
SQL·Thread·Pool·JVM 개선
↓
동일 조건 재시험
↓
효과·부작용 검증
↓
운영 Baseline·Alert

가장 중요한 원칙은 다음과 같다.

\`\`\`text id=“perf32198” 성능은 설정값을 크게 만든다고 좋아지지 않는다.

느린 거래를 식별하고, 실제 대기구간과 포화자원을 측정한 뒤,

가장 큰 병목 하나를 개선하고 같은 조건에서 결과를 다시 증명해야 한다.

빠른 시스템이란 한 번 빠르게 응답한 시스템이 아니라,

목표 부하와 장애조건에서도 일관된 응답과 정확한 데이터를 지속적으로 제공하는 시스템이다. \`\`\`
