<!-- source: ztcf-집필본/NSIGHT TCF Chapter 31- 장애 대응 기본.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제31장. 장애 대응 기본

## 도입 전 안내말

제28장에서는 로그·메트릭·추적정보를 이용해 한 거래가 어느 시스템과 계층을 거쳤는지 확인하는 방법을 살펴보았다.

제29장에서는 Cache 장애가 원본 DB로 확산되지 않도록 정합성·무효화·Fallback을 설계하는 방법을 다루었다.

제30장에서는 파일·Batch·Scheduler의 중단·재시작·중복 처리·대사 방법을 정리했다.

이번 장에서는 이러한 운영정보를 실제 장애 상황에서 어떻게 사용해야 하는지를 설명한다.

장애가 발생하면 초보 운영자나 개발자는 다음 행동부터 하기 쉽다.

\`\`\`text id=“inc31001” 화면이 느리다.

Tomcat을 재기동한다.

DB Pool을 늘린다.

Timeout 값을 크게 바꾼다.

오류가 난 거래를 다시 실행한다.

로그파일을 삭제한다.

외부 시스템에 재전송한다.



이 조치가 우연히 증상을 완화할 수는 있다.

그러나 원인을 확인하지 않고 수행하면 다음 문제가 발생한다.

\`\`\`text id="inc31002"
재기동하면서 Thread·Heap·Pool 증거가 사라진다.

DB Pool을 늘려 DB Connection 고갈을 심화시킨다.

Timeout을 늘려 느린 거래가 Thread를 더 오래 점유한다.

변경 거래를 재실행해 데이터가 중복 반영된다.

외부기관에는 이미 처리됐는데 다시 요청한다.

부분 반영 데이터를 확인하지 못한다.

같은 장애가 며칠 뒤 다시 발생한다.

장애 대응의 목적은 서버를 빨리 재기동하는 것이 아니다.

\`\`\`text id=“inc31003” 사용자 영향을 줄이고

서비스를 안전하게 복구하며

데이터 정합성을 확인하고

원인을 증거로 설명하며

같은 장애가 다시 발생하지 않게 하는 것



이 목적이다.

장애 대응은 다음 다섯 단계로 진행한다.

\`\`\`text id="inc31004"
증상 감지
↓
영향 범위 분류
↓
대표 거래와 증거 확보
↓
서비스 복구
↓
원인 분석·재발 방지

원본 가이드의 장애 대응 Runbook도 감지, 범위 분류, 대표 Correlation ID 확보, Gateway부터 DB·외부시스템까지 계층 추적, 복구 결정, 데이터 확인, 종료, 사후 분석 순서로 구성한다.

장애 대응에서 반드시 분리해야 하는 두 질문이 있다.

\`\`\`text id=“inc31005” 첫 번째 질문 지금 서비스를 어떻게 정상화할 것인가?

두 번째 질문 왜 장애가 발생했는가?



서비스 복구는 빠르게 수행해야 한다.

근본 원인 분석은 정확하게 수행해야 한다.

두 활동은 동시에 진행할 수 있지만 목적과 책임자가 다르다.

\`\`\`text id="inc31006"
복구팀
→ 사용자 영향 축소
→ 우회·차단·Rollback·재기동

분석팀
→ 로그·Metric·Thread·SQL·데이터 분석
→ 근본 원인과 재발방지

증거를 확보하기 전 모든 인스턴스를 재기동하면 분석팀은 원인을 확인할 수 없다.

반대로 완벽한 원인을 찾을 때까지 서비스를 복구하지 않으면 사용자 영향이 확대된다.

따라서 장애 대응은 다음 원칙을 따른다.

\`\`\`text id=“inc31007” 최소한의 증거를 먼저 확보한다.

안전한 복구를 신속하게 수행한다.

복구와 원인 분석을 병렬로 진행한다.

데이터 정합성을 별도로 확인한다.

모니터링 정상화까지 확인한 뒤 종료한다.


\---

\# 문서 개요

\## 목적

본 장의 목적은 NSIGHT TCF 운영환경에서 장애를 신속하고 일관되게 탐지·분류·복구하고, 데이터 정합성과 재발 방지를 증명하기 위한 장애 대응 기준을 정의하는 것이다.

세부 목적은 다음과 같다.

\`\`\`text id="inc31008"
장애와 단순 문의의 구분

장애 심각도와 영향 범위 분류

최초 감지시각과 장애 시작시각 구분

대표 실패 거래와 정상 거래 확보

GUID·TraceId 기반 전체 경로 추적

Gateway·TCF·업무·DB·외부 원인 구분

서비스 복구와 근본 원인 분석 분리

재기동 전 증거 보존

Timeout 계층 식별

Tomcat Thread·JVM·DB Pool 진단

Slow SQL·Lock·외부 지연 식별

부분 반영·중복·미처리 데이터 대사

거래통제·우회·Rollback·재기동 판단

장애 타임라인과 의사결정 기록

War Room·보고·에스컬레이션 표준화

장애 종료조건과 사후분석 기준 정의

재발방지 과제의 품질 Gate 반영

## 적용범위

| 영역 | 장애 대상 |
| --- | --- |
| 채널·화면 | 접속불가·오류·응답지연 |
| L4·Apache | Route·Connection·Backend 장애 |
| Gateway | 인증·라우팅·Timeout |
| TCF | STF·Dispatcher·ETF·거래통제 |
| 업무 WAR | Handler·Service·Transaction |
| JVM | CPU·Heap·GC·Metaspace |
| Tomcat | Connector Thread·Queue·Context |
| DB Pool | Active·Idle·Pending·Timeout |
| DB | Slow SQL·Lock·Deadlock·장애 |
| Cache | Stale·Miss 폭증·Version 불일치 |
| 내부 연계 | 업무 WAR 간 지연·실패 |
| 외부 연계 | Timeout·오류·결과 불명확 |
| Batch | 중복·중단·지연·대사 실패 |
| 파일 | 업로드·다운로드·Storage 장애 |
| 인증 | JWT·SSO·권한·키 장애 |
| 배포 | 신규 Version·설정·DB Migration |
| 인프라 | VM·Disk·Network·센터 장애 |
| 운영관리 | OM·로그·Metric 수집 장애 |
| 보안 | 인증우회·정보유출·악성행위 |

## 대상 독자

\`\`\`text id=“inc31009” 운영·관제 담당자

애플리케이션 운영자

업무 개발자

프레임워크 개발자

애플리케이션 아키텍트

기술·인프라 아키텍트

DBA·데이터 아키텍트

보안 담당자

DevOps·배포 담당자

서비스 책임자

PMO·장애관리 담당자

고객지원·업무 담당자


\## 선행조건

\`\`\`text id="inc31010"
ServiceId·거래코드

GUID·TraceId

거래로그

오류코드

JVM·Thread·DB Pool Metric

배포 Version

Instance·WAR 식별자

Timeout 정책

거래통제 기능

Rollback Artifact

비상연락망

장애 대응 Runbook

데이터 대사방법

# 핵심 관점

\`\`\`text id=“inc31011” 장애 대응은 원인을 추측하는 활동이 아니다.

영향 범위를 줄이고, 대표 거래와 시스템 상태를 증거로 확보한 뒤,

서비스 복구와 데이터 정합성, 근본 원인과 재발 방지를

각각의 완료기준으로 관리하는 운영 통제 절차다.


\---

\# 학습 목표

이 장을 마치면 다음 내용을 설명하고 적용할 수 있어야 한다.

| 번호 | 학습 목표 |
|---:|---|
| 1 | 장애·결함·문의·알림을 구분한다. |
| 2 | 장애 심각도와 업무 영향도를 구분한다. |
| 3 | 최초 감지시각과 실제 장애 시작시각을 구분한다. |
| 4 | 영향을 받는 업무·사용자·지역·거래를 분류한다. |
| 5 | 대표 실패 GUID와 정상 GUID를 확보한다. |
| 6 | 화면부터 SQL·외부 연계까지 정방향 추적한다. |
| 7 | Table·SQL에서 영향 ServiceId를 역추적한다. |
| 8 | 복구와 근본 원인 분석을 분리한다. |
| 9 | 재기동 전에 필요한 증거를 확보한다. |
| 10 | 거래통제·우회·Rollback·재기동을 비교한다. |
| 11 | 변경 거래 재시도의 중복 위험을 판단한다. |
| 12 | Timeout이 발생한 계층을 구분한다. |
| 13 | 전체 Timeout Budget과 하위 Timeout을 비교한다. |
| 14 | DB Pool Active·Idle·Pending을 해석한다. |
| 15 | DB Pool 고갈과 Slow SQL을 구분한다. |
| 16 | CPU·Heap·GC·Thread 장애를 구분한다. |
| 17 | 특정 WAR와 ServiceId의 자원점유를 추적한다. |
| 18 | 외부 시스템 지연과 내부 장애를 구분한다. |
| 19 | 배포·설정 변경과 장애시점을 연결한다. |
| 20 | 부분 Commit·중복·미처리 데이터를 확인한다. |
| 21 | 장애 타임라인과 의사결정을 기록한다. |
| 22 | War Room 역할과 보고체계를 운영한다. |
| 23 | 장애 종료조건과 모니터링 안정화 조건을 정의한다. |
| 24 | 근본 원인·기여 요인·탐지 실패를 구분한다. |
| 25 | 장애 후 개선과제를 Owner·기한·Gate로 관리한다. |
| 26 | 장애훈련과 복구시험을 수행한다. |

\---

\# 핵심 용어

| 용어 | 의미 |
|---|---|
| Incident | 서비스·데이터·보안에 실제 영향을 주는 장애 사건 |
| Event | 운영상 관찰된 하나의 상태변화 |
| Alert | 임계조건 충족을 알리는 통보 |
| Defect | 코드·설계·설정의 결함 |
| Problem | 하나 이상의 장애를 유발하는 근본 원인 |
| Symptom | 사용자나 시스템에서 보이는 현상 |
| Impact | 장애로 영향을 받은 사용자·업무·데이터 범위 |
| Severity | 장애의 심각도 |
| Priority | 조치 우선순위 |
| Detection Time | 장애를 처음 인지한 시각 |
| Start Time | 실제 장애가 시작된 것으로 판단되는 시각 |
| Acknowledge Time | 담당자가 장애를 인지하고 대응을 시작한 시각 |
| Recovery Time | 서비스가 정상화된 시각 |
| Resolution Time | 근본조치까지 완료된 시각 |
| MTTA | 평균 인지시간 |
| MTTD | 평균 탐지시간 |
| MTTR | 평균 복구시간 |
| Correlation ID | 관련 거래와 로그를 연결하는 식별자 |
| Evidence | 로그·Metric·Dump·DB 결과 등의 증거 |
| Workaround | 근본원인 제거 전 영향만 줄이는 임시조치 |
| Mitigation | 장애 영향 축소 조치 |
| Recovery | 서비스 정상화 |
| Root Cause | 장애를 발생시킨 근본 원인 |
| Contributing Factor | 장애를 확대하거나 탐지를 늦춘 요인 |
| PIR | 장애 사후검토 |
| RCA | 근본 원인 분석 |
| War Room | 장애 대응 인력이 공동 판단하는 협업체계 |
| Incident Commander | 장애 대응 의사결정 총괄 |
| Scribe | 타임라인·판단·조치를 기록하는 담당자 |
| Reconciliation | 데이터 건수·금액·상태 대사 |
| Failover | 다른 인스턴스·센터로 서비스 전환 |
| Rollback | 이전 Version·설정으로 복구 |
| Roll-forward | 수정된 신규 Version으로 복구 |
| Containment | 장애·보안 영향의 확산 차단 |

\---

\# 장애 대응 전체 흐름

\`\`\`text id="inc31012"
알림·사용자 신고
↓
장애 접수
↓
심각도·영향 분류
↓
Incident Commander 지정
↓
대표 실패·정상 거래 확보
↓
증거 Snapshot
↓
계층별 원인 후보 분석
↓
복구안 비교·승인
↓
거래통제·우회·Rollback·재기동
↓
서비스 정상 확인
↓
데이터 정합성 확인
↓
모니터링 정상화
↓
장애 종료
↓
PIR·RCA
↓
재발방지 과제·품질 Gate

# 장애와 단순 오류의 구분

| 상황 | 분류 |
| --- | --- |
| 사용자가 필수값을 입력하지 않음 | 업무 입력 오류 |
| 권한 없는 사용자가 기능 요청 | 정상 권한 거절 |
| 한 거래에서 예상하지 못한 예외 | 결함 후보 |
| 다수 거래가 연속 실패 | 장애 |
| p95가 지속적으로 목표 초과 | 성능 장애 |
| 특정 지점만 지속 실패 | 부분 장애 |
| 데이터가 중복·누락됨 | 데이터 장애 |
| 운영 Token·Secret 노출 | 보안사고 |
| 모니터링 수집만 실패 | 관측성 장애 |
| 예정 Batch 미실행 | Batch 장애 |
| 운영자 문의지만 서비스 정상 | 운영 문의 |

# 장애 심각도

| 등급 | 판단 기준 | 대표 예 | 대응 |
| --- | --- | --- | --- |
| SEV-1 | 핵심 서비스 전면 중단·중대 데이터·보안 | 전체 접속불가, 원장 훼손 | 즉시 War Room |
| SEV-2 | 주요 업무 다수 사용자 영향 | 특정 업무 WAR 전체 실패 | 즉시 대응 |
| SEV-3 | 제한된 사용자·기능 영향 | 특정 지점·ServiceId 오류 | 신속 분석 |
| SEV-4 | 경미한 저하·우회 가능 | 비핵심 화면 지연 | 계획 조치 |
| INFO | 실제 영향 없음 | 일시 경고·예방 알림 | 기록·관찰 |

심각도는 기술 오류 메시지보다 업무 영향을 기준으로 판단한다.

\`\`\`text id=“inc31013” CPU 95% 하지만 서비스 정상 → 바로 SEV-1 아님

CPU 40% 하지만 고객 전체 로그인 불가 → SEV-1 가능


\---

\# 영향도 평가축

\`\`\`text id="inc31014"
사용자 범위

업무 중요도

지역·지점 범위

거래 실패율

서비스 중단시간

데이터 손상

보안·개인정보

외부기관 영향

복구 우회 가능성

법적·감사 영향

# 현재 구현과 목표 구조

## 현재 구현에서 확인되는 운영기능

현재 기준 소스에는 다음 기반이 존재한다.

| 영역 | 구현 상태 |
| --- | --- |
| 거래로그 | ServiceId·GUID·결과·처리시간 |
| 거래요약 | 전체·오류·Timeout·평균시간 |
| 오류 Top | 최근 오류코드·거래 집계 |
| Slow 거래 | 최근 Slow Transaction Top |
| AP 상태 | CPU·Heap·Thread 수집 |
| DB 상태 | Health·Hikari Active·Max·사용률 |
| 배포 상태 | Health·Version·기동상태 |
| Health Check | Actuator 기반 |
| Timeout 정책 | 온라인·Transaction·SQL·외부 |
| Query Timeout | MyBatis 정책 적용 |
| Timeout Context | 워커 Thread 문맥 전파 |
| 거래통제 | 업무 거래 차단 |
| 배포·Rollback | OM 요청·승인·이력 |
| Thread Dump | Actuator Endpoint 설정 |
| 데이터소스 종료 | Context 종료 시 Hikari Close |

## 현재 OM Runtime 상태 수집

현재 OmRuntimeHealthSupport는 다음 값을 수집한다.

\`\`\`text id=“inc31015” OM Local Process CPU

OM Heap 사용률

Live Thread 수

OM DataSource Active Connection

OM DataSource Maximum Pool Size

DB Ping 결과



현재 Health 판정 예:

\`\`\`text id="inc31016"
AP CPU·Heap
85% 이상 WARN
95% 이상 FAIL

DB Pool
80% 이상 WARN
95% 이상 FAIL

이는 기본 상태판정 기반으로 활용할 수 있다.

다만 현재 구현은 다음 정보를 충분히 제공하지 않는다.

\`\`\`text id=“inc31017” Tomcat Connector Busy Thread

Thread Queue

Hikari Pending Thread

Connection 획득시간

GC Pause

Deadlock

현재 실행 중 ServiceId

현재 처리단계

현재 SQL ID

외부 대기 대상

WAR별 Active 거래

Instance별 Slow SQL


\## 현재 수집의 범위

\`tcf-batch\`는 등록된 AP·DB·배포 대상을 Actuator 또는 JDBC로 조회해 \`OM\_AP\_STATUS\`, \`OM\_DB\_STATUS\`, \`OM\_DEPLOY\_STATUS\`에 최신 상태를 저장한다.

OM Dashboard는 최근 15분의 오류 Top과 Slow Transaction Top을 제공한다.

이 구조는 운영상태의 기본 현황에는 유용하지만 다음 수준의 원인판정에는 한계가 있다.

\`\`\`text id="inc31018"
Thread가 왜 바쁜가?

Connection을 기다리는 거래는 무엇인가?

어떤 Mapper SQL이 실행 중인가?

어느 외부 시스템을 기다리는가?

특정 WAR가 Connector Thread를 얼마나 점유하는가?

## 목표 런타임 진단구조

text id="inc31019" 각 업무 WAR └─ TcfRuntimeMonitor ├─ ActiveTransactionRegistry ├─ Current Step ├─ SQL ID·실행시간 ├─ 외부 호출 대상 ├─ Hikari Active·Idle·Pending ├─ JVM·Thread └─ 최근 Slow 거래 ↓ 내부 진단 API ↓ tcf-om ├─ RuntimeStatusCollector ├─ RuntimeCauseAnalyzer ├─ Incident Manager ├─ Evidence Snapshot ├─ Runbook └─ Dashboard

이 목표 구조에서는 JVM·Connector Thread 같은 Tomcat 전체 지표와 ServiceId·Hikari Pool·SQL 같은 WAR별 지표를 구분해야 한다. 하나의 Tomcat에서 여러 WAR가 실행될 경우 JVM CPU·Heap·GC를 WAR별 정확한 수치처럼 나누어 표시해서는 안 된다.

## 구현 상태 판단

| 항목 | 상태 | 판단 |
| --- | --- | --- |
| 거래로그 검색 | 구현 확인 | 대표 거래 확보 가능 |
| Slow 거래 Top | 구현 확인 | 과거 Slow 거래 분석 |
| Timeout Count | 구현 확인 | 장애 탐지 기반 |
| AP CPU·Heap·Thread | 부분 구현 | Busy·State·Deadlock 보완 |
| Hikari Active·Max | 부분 구현 | Idle·Pending·Acquire 보완 |
| DB Ping | 구현 확인 | DB 생존 확인 |
| 배포 Version | 부분 구현 | 장애시점 연결 |
| Active Transaction Registry | 목표 설계 | 구현 필요 |
| Current Step | 목표 설계 | 구현 필요 |
| Slow SQL Registry | 목표 설계 | 구현 필요 |
| 외부 대기 추적 | 목표 설계 | 구현 필요 |
| Runtime Cause Analyzer | 목표 설계 | 구현 필요 |
| Incident 관리 Table | 확인되지 않음 | 구현 필요 |
| Evidence Snapshot | 확인되지 않음 | 구현 필요 |
| War Room Timeline | 문서 기준 | OM 기능화 검토 |
| Hikari Pending | 확인되지 않음 | 운영 필수 |
| Connector Busy Thread | 확인되지 않음 | 운영 필수 |
| Deadlock 감지 | 확인되지 않음 | 운영 필수 |
| GC 상세 | 확인되지 않음 | 운영 필수 |
| Thread Dump | Endpoint 설정 | 접근통제 필요 |
| Timeout Executor | Cached Thread Pool | 상한·Queue 보완 필요 |

## Timeout Executor 위험

현재 온라인 Timeout 워커는 newCachedThreadPool() 기반이다.

\`\`\`text id=“inc31020” 느린 거래 증가

→ Worker Thread 계속 생성

→ Thread 증가

→ Context Switching 증가

→ JVM 자원고갈



Timeout은 거래를 빨리 종료하기 위한 기능이지만, 무제한 Worker 구조에서는 대규모 지연 시 장애를 확대할 수 있다.

목표:

\`\`\`text id="inc31021"
Bounded Thread Pool

Core·Max Thread

Bounded Queue

Rejected Policy

Active·Queue Metric

Shutdown Timeout

# 설계 원칙

## 원칙 1. 증상과 원인을 구분한다

\`\`\`text id=“inc31022” 증상 Timeout 증가

원인 후보 Slow SQL DB Pool 대기 외부 지연 CPU GC Thread 고갈



Timeout 자체가 근본 원인은 아니다.

\## 원칙 2. 대표 거래를 확보한다

모든 로그를 한꺼번에 분석하지 않는다.

\`\`\`text id="inc31023"
대표 실패 거래 3건

대표 정상 거래 1건

장애 전 정상 거래 1건

을 비교한다.

## 원칙 3. 재기동 전 최소 증거를 확보한다

\`\`\`text id=“inc31024” GUID

시간범위

Metric Snapshot

Thread Dump

Pool 상태

Slow SQL

배포·설정 Version


\## 원칙 4. 서비스 복구와 데이터 복구를 구분한다

\`\`\`text id="inc31025"
화면이 다시 열린다
≠ 데이터가 정상이다

## 원칙 5. 원인 후보와 확정 원인을 구분한다

| 표현 | 의미 |
| --- | --- |
| 원인 후보 | 한 지표가 가능성을 나타냄 |
| 강한 원인 후보 | 여러 지표가 같은 원인을 나타냄 |
| 확정 원인 | 재현·코드·DB·설정 증거로 확인 |

## 원칙 6. 장애 대응 중 변경을 최소화한다

동시에 여러 설정을 바꾸면 어떤 변경이 복구에 영향을 줬는지 알 수 없다.

\`\`\`text id=“inc31026” Pool 증가

Timeout 증가

Thread 증가

SQL Hint 추가

Tomcat 재기동



을 한 번에 수행하지 않는다.

\## 원칙 7. 모든 조치는 되돌릴 수 있어야 한다

\`\`\`text id="inc31027"
누가

언제

무엇을

왜

어떤 값에서 어떤 값으로

어떻게 원복할 수 있는지

를 기록한다.

# 31.1 영향 범위와 대표 거래 확보

## 31.1.1 최초 접수

장애를 처음 인지하면 다음을 기록한다.

\`\`\`text id=“inc31028” 접수 시각

신고자

증상

화면 ID

ServiceId

오류코드

발생시각

사용자·지점

GUID

스크린샷

재현 여부



장애 접수시각과 장애 시작시각은 다를 수 있다.

\`\`\`text id="inc31029"
10:20
사용자 최초 신고

Metric 분석 결과
09:47부터 오류율 증가

장애 시작시각
09:47

## 31.1.2 영향 범위 분류

### 업무 범위

\`\`\`text id=“inc31030” SV 전체

SV.Customer 조회만

캠페인 승인 거래만

파일 다운로드만

전체 로그인


\### 사용자 범위

\`\`\`text id="inc31031"
전체 사용자

특정 지점

특정 권한그룹

특정 채널

특정 브라우저 Version

### 인프라 범위

\`\`\`text id=“inc31032” 센터 전체

AP01만

특정 Tomcat

특정 WAR

특정 Hikari Pool

특정 DB Instance


\### 시간 범위

\`\`\`text id="inc31033"
계속 발생

간헐 발생

특정 시간대

배포 직후

Batch 실행 중

## 31.1.3 영향도 Matrix

| 축 | 질문 | 증거 |
| --- | --- | --- |
| 업무 | 어떤 ServiceId인가 | 거래로그 |
| 사용자 | 몇 명·어느 지점인가 | 사용자 신고·로그 |
| 실패율 | 전체 중 몇 %인가 | Metric |
| 성능 | p95·p99가 얼마인가 | Timer |
| 데이터 | 부분 반영이 있는가 | DB 대사 |
| 인프라 | 어느 Instance인가 | AP 상태 |
| 연계 | 특정 외부만 문제인가 | 연계로그 |
| 배포 | 신규 Version인가 | Deploy ID |
| 보안 | 개인정보·권한 영향인가 | 감사·보안로그 |

## 31.1.4 대표 실패 거래

좋은 대표 거래:

\`\`\`text id=“inc31034” 실패시각이 명확하다.

GUID·TraceId가 있다.

재현 가능하다.

요청조건을 확인할 수 있다.

영향 사용자를 대표한다.

관련 로그가 보존돼 있다.



대표 실패 거래는 한 건보다 3건 정도 확보하는 것이 좋다.

\`\`\`text id="inc31035"
실패 거래 A
특정 지점

실패 거래 B
다른 지점

실패 거래 C
다른 Instance

공통점을 찾는다.

## 31.1.5 대표 정상 거래

실패 거래만 보면 정상 기준을 알기 어렵다.

비교 대상:

\`\`\`text id=“inc31036” 같은 ServiceId의 장애 전 정상 거래

같은 시각의 다른 ServiceId 정상 거래

다른 Instance에서 처리된 정상 거래



비교:

| 항목 | 정상 | 실패 |
|---|---:|---:|
| Gateway | 20ms | 22ms |
| STF | 8ms | 9ms |
| DB Connection | 10ms | 2,800ms |
| SQL | 150ms | 160ms |
| 전체 | 220ms | Timeout |

이 경우 SQL보다 Connection 획득 대기가 원인 후보가 된다.

\---

\## 31.1.6 정방향 추적

\`\`\`text id="inc31037"
화면 ID

→ 이벤트 ID

→ ServiceId

→ Gateway Route

→ TCF

→ Handler

→ Facade

→ Service

→ DAO·Mapper

→ DB

→ 내부·외부 연계

각 구간에서 다음을 비교한다.

\`\`\`text id=“inc31038” 시작시각

종료시각

처리시간

결과코드

오류코드

대상 Instance

배포 Version


\---

\## 31.1.7 역방향 추적

Slow SQL 또는 Table에서 시작할 수도 있다.

\`\`\`text id="inc31039"
Table Lock

→ SQL ID

→ Mapper

→ DAO

→ Service

→ ServiceId

→ 화면

→ 사용자 영향

직접 호출관계가 없어도 공유 Table·공통 설정·공통 Library를 통해 간접 영향이 발생할 수 있다.

원본도 영향 범위를 수정 파일 목록으로 한정하지 않고 호출자·데이터·외부 연계·배포·운영통제까지 포함하도록 요구한다.

## 31.1.8 Instance 비교

\`\`\`text id=“inc31040” AP01 오류율 30%

AP02 오류율 0%



가능성:

\`\`\`text id="inc31041"
AP01 설정 오류

AP01 배포 Version 불일치

AP01 Hikari Pool 문제

AP01 Network 문제

AP01 Cache 불일치

전체 시스템 문제가 아니라 Instance 국소장애일 수 있다.

이 경우 AP01만 트래픽에서 제외하면 빠르게 복구할 수 있다.

## 31.1.9 Version 비교

\`\`\`text id=“inc31042” AP01 Version 1.4.2

AP02 Version 1.4.1



장애가 1.4.2에서만 발생하면 신규 배포가 강한 원인 후보다.

다음도 함께 확인한다.

\`\`\`text id="inc31043"
Artifact Version

Commit ID

Config Version

DB Migration Version

Cache Version

JWT Key Version

## 31.1.10 특정 지점만 실패

우선 확인:

\`\`\`text id=“inc31044” 지점코드

데이터권한

지점 Mapping

Header

해당 지점 데이터

Cache Key Scope

외부기관 지점등록



전체 서버를 재기동할 이유가 없을 수 있다.

\---

\## 31.1.11 특정 사용자만 실패

확인:

\`\`\`text id="inc31045"
사용자 상태

Token Claim

권한그룹

Session

기능권한

데이터권한

Cache된 권한 Version

계정 잠금

## 31.1.12 특정 ServiceId만 실패

확인:

\`\`\`text id=“inc31046” OM Service Catalog

거래통제

Timeout Policy

Handler 등록

Mapper SQL

관련 Table

외부 Interface

최근 코드변경


\---

\## 31.1.13 전체 서비스가 느림

확인 우선순위:

\`\`\`text id="inc31047"
L4·Apache Connection

Tomcat Busy Thread

JVM CPU·GC

DB Pool Pending

DB 전체 부하

공통 외부시스템

공통 Cache 장애

최근 공통 모듈 배포

## 31.1.14 증거 Snapshot

장애시점에 다음을 하나의 Evidence ID로 묶는다.

\`\`\`text id=“inc31048” Incident ID

수집시각

대상 Instance

거래로그

Metric Snapshot

Thread Dump

Heap·GC

Hikari Pool

Slow SQL

DB Lock

배포정보

설정 Version

외부 상태


\---

\## 31.1.15 대표 거래 확보 실패

GUID가 없는 경우:

\`\`\`text id="inc31049"
사용자

발생시각

화면 ID

ServiceId

지점

오류코드

대상 Instance

를 조합해 거래를 찾는다.

이 상황은 관측성 결함으로 별도 개선과제를 등록한다.

# 31.2 복구와 원인 분석 분리

## 31.2.1 두 개의 Workstream

\`\`\`text id=“inc31050” Workstream A 서비스 복구

Workstream B 원인 분석·증거 보존


\### 복구팀

\`\`\`text id="inc31051"
거래통제

트래픽 제외

우회

Rollback

Failover

재기동

기능 축소

### 분석팀

\`\`\`text id=“inc31052” 대표 GUID 분석

Thread·Pool·DB 분석

코드·설정 Diff

배포 Version 비교

데이터 대사

재현시험


\---

\## 31.2.2 Incident Commander

Incident Commander의 책임:

\`\`\`text id="inc31053"
심각도 확정

War Room 소집

역할 지정

복구안 승인

변경 충돌 방지

보고 주기 결정

장애 종료 승인

Incident Commander가 모든 기술분석을 직접 수행할 필요는 없다.

의사결정과 조정을 담당한다.

## 31.2.3 증거 확보 최소 세트

재기동 전 최소한 다음을 확보한다.

\`\`\`text id=“inc31054” 대표 GUID 3건

대상 Instance·WAR

현재 오류율·p95

Tomcat Thread 상태

JVM CPU·Heap·GC

Hikari Active·Idle·Pending

최근 Slow SQL

배포·설정 Version

DB Lock·Session

외부 연계 상태



SEV-1에서는 모든 증거를 기다리지 않고 1~3분 안에 핵심 Snapshot만 확보한 뒤 복구할 수 있다.

\---

\## 31.2.4 Thread Dump

권장 수집:

\`\`\`text id="inc31055"
5~10초 간격

3회

같은 Thread가
같은 Stack에서 대기하는지 비교

한 번의 Thread Dump만으로 일시 상태와 지속 병목을 구분하기 어렵다.

Thread Dump에는 개인정보가 직접 포함될 가능성은 낮지만 SQL·URL·Parameter가 Stack에 포함될 수 있으므로 접근을 제한한다.

## 31.2.5 Heap Dump

Heap Dump는 매우 크고 개인정보·Token·Request 객체를 포함할 수 있다.

\`\`\`text id=“inc31056” 무조건 수집 금지

OOM·Memory Leak 분석에 필요한 경우

보안 승인

암호화 저장

접근통제

보존기간

파기



를 적용한다.

\---

\## 31.2.6 복구 대안 비교

| 대안 | 효과 | 주요 위험 | 적용 |
|---|---|---|---|
| 거래통제 | 특정 거래 차단 | 사용자 기능 제한 | 특정 ServiceId |
| 트래픽 제외 | 문제 Instance 격리 | 잔여 용량 부족 | 국소 장애 |
| 외부 우회 | 의존성 제거 | 데이터 최신성 | 선택 연계 |
| Cache Evict | Stale 해소 | DB 부하 | Cache 문제 |
| 설정 Rollback | 잘못된 값 복원 | 재기동 필요 | 설정 장애 |
| WAR Rollback | 신규 결함 제거 | DB 호환성 | 배포 장애 |
| Scale-out | 처리용량 증가 | DB 병목 확대 | CPU·Thread |
| 재기동 | 상태 초기화 | 증거 손실·재발 | Leak·고착 |
| DB Session Kill | Lock 해제 | Transaction Rollback | DBA 승인 |
| Failover | 다른 센터·DB | 데이터 지연 | 인프라 장애 |

\---

\## 31.2.7 거래통제

특정 ServiceId만 장애를 유발한다면 전체 시스템 재기동보다 해당 거래를 차단하는 것이 안전할 수 있다.

\`\`\`text id="inc31057"
SV.Customer.searchByName
→ 사용중지

SV.Customer.selectSummary
→ 정상 유지

거래통제 시 기록:

\`\`\`text id=“inc31058” Incident ID

ServiceId

차단사유

시작시각

승인자

사용자 안내

해제조건


\---

\## 31.2.8 문제 Instance 격리

\`\`\`text id="inc31059"
AP01 오류

AP02 정상

절차:

\`\`\`text id=“inc31060” L4·Apache에서 AP01 제외

기존 요청 Drain

증거 확보

AP02 잔여 용량 확인

AP01 분석·복구

Health·Smoke

트래픽 복귀


\---

\## 31.2.9 재기동 판단

재기동이 효과적인 경우:

\`\`\`text id="inc31061"
ClassLoader·Thread Leak

고착된 내부 상태

일시적 Resource Leak

설정 적용 필요

OOM 후 불안정 상태

재기동만으로 해결되지 않는 경우:

\`\`\`text id=“inc31062” Slow SQL

DB Lock

외부 시스템 장애

잘못된 업무 데이터

잘못된 배포코드

Pool 크기 부족

Disk Full


\---

\## 31.2.10 전체 재기동 금지

하나의 Tomcat에 여러 WAR가 배포된 경우 Process 재기동은 모든 업무 WAR에 영향을 준다.

\`\`\`text id="inc31063"
SV 장애

Tomcat 재기동

→ IC·PC·MS·PD·EB 등도 중단

재기동 전 장애영역과 공유자원을 확인한다.

## 31.2.11 Pool 즉시 확대 주의

DB Pool 고갈 시 maximumPoolSize를 늘리고 싶을 수 있다.

그러나 다음을 먼저 확인한다.

\`\`\`text id=“inc31064” DB 최대 Session

전체 WAR Pool 합계

Slow SQL

Connection Leak

Transaction 장기화

DB CPU

Lock



Slow SQL 때문에 Connection이 오래 점유되는 상황에서 Pool을 늘리면 더 많은 Slow SQL이 DB로 유입돼 장애가 확대될 수 있다.

\---

\## 31.2.12 Timeout 즉시 확대 주의

\`\`\`text id="inc31065"
현재 Timeout
3초

장애 중
30초로 증가

결과:

\`\`\`text id=“inc31066” 사용자 대기 증가

Thread 점유 증가

Connection 점유 증가

재시도 겹침

장애 확산



Timeout 확대는 원인 제거가 아니라 대기시간 확대일 수 있다.

\---

\## 31.2.13 Rollback 판단

확인:

\`\`\`text id="inc31067"
장애가 배포 직후 발생했는가?

구 Version에서 재현되지 않는가?

DB Schema가 구 Version과 호환되는가?

설정 Version을 함께 복구해야 하는가?

이미 신규 데이터가 생성됐는가?

## 31.2.14 외부 변경 거래 재시도

\`\`\`text id=“inc31068” 외부기관 Timeout

내부에서는 실패로 보임

외부기관에서는 성공 가능



금지:

\`\`\`text id="inc31069"
새 Request ID로 즉시 재전송

권장:

\`\`\`text id=“inc31070” 동일 멱등 Key

상태조회

외부 거래번호 확인

UNKNOWN 상태

대사 후 재처리


\---

\## 31.2.15 데이터 정합성 확인

복구 후 반드시 확인한다.

\`\`\`text id="inc31071"
부분 Commit

중복 등록

미처리

UNKNOWN

Master·Detail 불일치

Outbox 미발행

Cache Stale

Batch Checkpoint

외부기관 결과

## 31.2.16 데이터 대사

예:

\`\`\`text id=“inc31072” 장애시간 요청 1,000건

SUCCESS 870건

BUSINESS\_FAIL 50건

SYSTEM\_FAIL 30건

TIMEOUT 40건

UNKNOWN 10건



다음이 필요하다.

\`\`\`text id="inc31073"
UNKNOWN 10건 상태조회

TIMEOUT 변경 거래 중복확인

부분 반영 0건 확인

재처리 대상 확정

## 31.2.17 임시복구와 근본조치

| 구분 | 예 |
| --- | --- |
| 임시복구 | 문제 Instance 제외 |
| 근본조치 | Connection Leak 수정 |
| 임시복구 | ServiceId 거래통제 |
| 근본조치 | SQL Index 추가 |
| 임시복구 | Cache 전체 Evict |
| 근본조치 | Evict Event 누락 수정 |
| 임시복구 | 외부 기능 Fallback |
| 근본조치 | Timeout·Circuit 적용 |

임시조치를 영구해결로 종료하지 않는다.

## 31.2.18 장애 중 변경관리

모든 운영 변경은 타임라인에 기록한다.

\`\`\`text id=“inc31074” 11:05 AP01 L4 제외

11:09 SV.Customer.search 거래통제

11:15 Version 1.4.1 Rollback 시작

11:23 Smoke Test 성공



변경마다 예상효과와 원복방법을 적는다.

\---

\## 31.2.19 커뮤니케이션

최초 공지:

\`\`\`text id="inc31075"
발생시각

영향업무

사용자 증상

현재 대응상태

다음 업데이트 예정시각

중간 공지:

\`\`\`text id=“inc31076” 영향변화

수행조치

복구예상

데이터 영향



종료 공지:

\`\`\`text id="inc31077"
복구시각

정상 확인범위

데이터 확인결과

잔여 위험

후속 계획

확인되지 않은 원인을 단정해 공지하지 않는다.

# 31.3 Timeout과 DB Pool 진단

## 31.3.1 Timeout은 여러 계층에 존재한다

\`\`\`text id=“inc31078” 브라우저 Timeout

Apache·Proxy Timeout

Gateway Connect Timeout

Gateway Read Timeout

TCF Online Timeout

Transaction Timeout

DB Query Timeout

Hikari Connection Timeout

내부 HTTP Timeout

외부 HTTP Timeout

Batch Job Timeout



오류메시지에 \`Timeout\`이 있다고 같은 원인이 아니다.

\---

\## 31.3.2 Timeout Budget

예:

\`\`\`text id="inc31079"
화면 목표
3초

Gateway
2.8초

TCF Online
2.5초

Transaction
2.2초

DB Query
1.5초

외부 Read
1.2초

하위 Timeout은 상위 Timeout보다 짧아야 한다.

\`\`\`text id=“inc31080” 상위 TCF Timeout 3초

DB Query Timeout 10초



이면 TCF는 먼저 Timeout 응답을 반환하지만 DB Query는 계속 실행될 가능성이 있다.

\---

\## 31.3.3 Timeout 진단 순서

\`\`\`text id="inc31081"
1\. 어느 계층의 Timeout 코드인지 확인
2\. 전체 처리시간 확인
3\. 현재 처리단계 확인
4\. Connection 획득시간 확인
5\. SQL 실행시간 확인
6\. 외부 호출시간 확인
7\. Thread·CPU·GC 확인
8\. 상·하위 Timeout 설정 비교
9\. Timeout 후 작업이 계속 실행되는지 확인
10\. 데이터 상태 확인

## 31.3.4 Timeout 증상 Matrix

| 증상 | 원인 후보 |
| --- | --- |
| Hikari Pending 증가 | Connection Pool 부족·장기 점유 |
| Active=max, SQL Slow | Slow SQL |
| Active=max, SQL 짧음 | Pool 과소·Transaction 장기화 |
| Pending 0, Busy Thread 높음 | 외부·CPU·Lock |
| CPU 높음, Pending 0 | 계산·Loop·GC |
| GC Pause 증가 | Heap·Allocation |
| 특정 외부만 느림 | 외부 시스템 |
| 특정 Instance만 Timeout | 설정·Network·Pool |
| 모든 ServiceId Timeout | 공통 자원 |
| 변경 거래 Timeout | 결과 UNKNOWN 가능 |

## 31.3.5 Hikari Pool 지표

\`\`\`text id=“inc31082” Maximum Pool 최대 Connection

Active 사용 중 Connection

Idle 사용 가능한 Connection

Pending Connection을 기다리는 Thread

Acquire Time Connection 획득시간

Timeout Count 획득 실패 건수


\---

\## 31.3.6 Pool 상태 해석

\### 정상

\`\`\`text id="inc31083"
Maximum
120

Active
45

Idle
75

Pending
0

### 포화

\`\`\`text id=“inc31084” Maximum 120

Active 120

Idle 0

Pending 80


\### Pool 과대 가능성

\`\`\`text id="inc31085"
Maximum
300

Active
20

Idle
280

불필요한 DB Session을 장기간 점유할 수 있다.

## 31.3.7 DB Pool 고갈 원인

\`\`\`text id=“inc31086” Slow SQL

Connection Leak

Transaction 장기화

외부 호출을 Transaction 안에서 수행

Batch의 Pool 독점

Pool 크기 과소

DB 응답지연

Lock 대기

동시요청 급증



Pool 크기만 보고 원인을 확정하지 않는다.

\---

\## 31.3.8 DB Pool과 Tomcat Thread

\`\`\`text id="inc31087"
DB Connection 없음

→ 요청 Thread 대기

→ Busy Thread 증가

→ Tomcat Queue 증가

→ 신규 요청 지연

→ 전체 Timeout

따라서 Thread와 Pool을 함께 본다.

## 31.3.9 Connection 획득 대기

목표 Runtime 단계:

text id="inc31088" WAIT\_DB\_CONNECTION

이 단계의 거래가 많고 Pending이 증가하면 DB Pool 대기가 강한 원인 후보다.

현재 소스는 Active·Maximum 기반 사용률은 제공하지만 Pending·Acquire Time과 거래별 대기단계는 추가 구현이 필요하다.

## 31.3.10 Slow SQL

특징:

\`\`\`text id=“inc31089” Connection 획득은 빠름

SQL 실행단계가 길다.

특정 Mapper ID가 반복된다.

Pool Active가 증가한다.

DB CPU·I/O·Lock이 증가할 수 있다.



확인:

\`\`\`text id="inc31090"
ServiceId

Mapper Namespace

SQL ID

실행시간

실행계획

읽은 Row

영향 행 수

Bind 분포

Lock

운영 진단화면에는 SQL Parameter 원문과 개인정보를 표시하지 않는다.

## 31.3.11 DB Lock

증상:

\`\`\`text id=“inc31091” 특정 변경 거래 지연

CPU는 높지 않음

SQL 실행시간 증가

DB Session이 Lock 대기



확인:

\`\`\`text id="inc31092"
Blocking Session

Waiting Session

SQL ID

Table·Row

Transaction 시작시각

업무 ServiceId

DB Session을 종료하기 전에 Transaction 영향과 Rollback 범위를 확인한다.

## 31.3.12 Deadlock

Deadlock은 DB 또는 JVM Thread에서 발생할 수 있다.

### DB Deadlock

text id="inc31093" 서로 다른 Transaction이 서로의 Lock을 대기

### JVM Deadlock

\`\`\`text id=“inc31094” Thread A Lock 1 보유 Lock 2 대기

Thread B Lock 2 보유 Lock 1 대기



JVM Deadlock은 \`ThreadMXBean.findDeadlockedThreads()\`로 감지할 수 있다.

현재 기본 Health 수집에는 Deadlock 판정이 없으므로 추가해야 한다.

\---

\## 31.3.13 CPU 과부하

\`\`\`text id="inc31095"
CPU 90% 이상 지속

DB Pending 없음

Thread RUNNABLE 다수

특정 ServiceId 집중

원인 후보:

\`\`\`text id=“inc31096” 무한 Loop

과도한 JSON 처리

암호화 연산

대량 메모리 복사

정규식 폭주

Busy Waiting

GC


\---

\## 31.3.14 GC 압박

\`\`\`text id="inc31097"
Heap 상승

GC Pause 증가

CPU 증가

응답시간 증가

Full GC 후 일시 회복

확인:

\`\`\`text id=“inc31098” Heap Used·Max

Allocation Rate

Young GC

Old GC

Pause Time

Promotion

Metaspace


\---

\## 31.3.15 Thread 부족

Tomcat Connector Thread:

\`\`\`text id="inc31099"
maxThreads

currentThreads

busyThreads

acceptCount

queuedRequests

문제:

\`\`\`text id=“inc31100” Busy Thread 100%

Queue 증가

신규 요청 대기

Timeout



원인:

\`\`\`text id="inc31101"
DB Pool 대기

Slow SQL

외부 HTTP

파일 다운로드

Thread Sleep

동기 Lock

CPU 연산

Thread 수를 늘리기 전에 대기 원인을 확인한다.

## 31.3.16 무제한 Timeout Worker

현재 온라인 Timeout Executor는 Cached Thread Pool이므로 장애 시 Thread 수가 증가할 수 있다.

운영 진단에 추가할 Metric:

\`\`\`text id=“inc31102” tcf.timeout.executor.active

tcf.timeout.executor.pool.size

tcf.timeout.executor.queue

tcf.timeout.executor.rejected

tcf.timeout.executor.completed


\---

\## 31.3.17 외부 시스템 지연

특징:

\`\`\`text id="inc31103"
DB Pool 정상

CPU 정상

GC 정상

WAIT\_EXTERNAL 증가

특정 Interface ID 집중

Read Timeout 증가

확인:

\`\`\`text id=“inc31104” 대상 시스템

Interface ID

Connect Time

Read Time

HTTP Status

Retry Count

Circuit State

외부 거래번호


\---

\## 31.3.18 Cache 장애

\`\`\`text id="inc31105"
Cache Hit Rate 급감

DB 조회 TPS 급증

Pool Active 증가

p95 증가

원인 후보:

\`\`\`text id=“inc31106” Region 전체 Evict

Cache Version 변경

Provider 장애

TTL 동시 만료

배포 후 Cold Start


\---

\## 31.3.19 Batch 영향

\`\`\`text id="inc31107"
온라인 Peak 중 Batch 실행

Batch Pool과 온라인 Pool 공유

대량 SQL

DB I/O 증가

온라인 Timeout 증가

Batch Job ID와 장애시각을 비교한다.

## 31.3.20 장애 판정 의사결정표

| 관측값 | 1순위 원인 후보 |
| --- | --- |
| Pending↑, Active=max | DB Pool 대기 |
| Active↑, 특정 SQL Slow | Slow SQL |
| DB 정상, WAIT\_EXTERNAL↑ | 외부 지연 |
| CPU↑, RUNNABLE Thread↑ | CPU 과부하 |
| GC Pause↑, Heap 회복 안 됨 | Memory Leak |
| Busy Thread↑, Pending 0, CPU 낮음 | 외부·Lock 대기 |
| 특정 Instance만 오류 | 배포·설정·국소 자원 |
| 전체 Version에서 동일 | 공통 DB·외부·인프라 |
| 배포 직후 신규 Version만 | 배포 결함 |
| Cache Miss↑, DB TPS↑ | Cache 장애 |
| PROCESSING 거래 장기 잔존 | 종료·강제종료·고착 |

현재 운영 점검기준도 ServiceId 오류율과 응답시간에서 시작해 현재 실행단계, DB Pool Pending, Slow SQL, JVM CPU·GC·Thread, GUID·TraceId 순으로 확인하도록 제시한다.

# 31.4 장애 타임라인과 종료 기준

## 31.4.1 타임라인의 목적

장애 타임라인은 단순 일지가 아니다.

다음을 증명한다.

\`\`\`text id=“inc31108” 언제 장애가 시작됐는가?

언제 감지했는가?

누가 어떤 판단을 했는가?

무슨 조치를 수행했는가?

어떤 조치 후 상태가 바뀌었는가?

언제 서비스와 데이터가 정상화됐는가?


\---

\## 31.4.2 타임라인 형식

| 시각 | 역할 | 관찰·행동 | 근거 | 결과 |
|---|---|---|---|---|
| 09:47 | 관제 | Timeout 증가 | Metric | 후보 장애 |
| 09:52 | 운영 | SEV-2 선언 | 오류율 25% | War Room |
| 09:55 | 개발 | 대표 GUID 확보 | 거래로그 | Pool 대기 |
| 09:58 | DBA | Blocking SQL 확인 | DB Session | 원인 후보 |
| 10:02 | IC | 문제 거래 통제 승인 | 영향분석 | 신규 유입 차단 |
| 10:08 | DBA | Blocking Session 종료 | 승인 | Pool 회복 |
| 10:12 | QA | Smoke 성공 | 거래결과 | 서비스 복구 |
| 10:20 | 업무 | 데이터 대사 완료 | SQL 결과 | 데이터 정상 |

\---

\## 31.4.3 장애 상태

\`\`\`text id="inc31109"
DETECTED

ACKNOWLEDGED

INVESTIGATING

MITIGATING

MONITORING

RECOVERED

RESOLVED

CLOSED

REOPENED

### RECOVERED

서비스가 정상화된 상태다.

### RESOLVED

근본 원인 또는 확정 조치가 완료된 상태다.

### CLOSED

사후검토와 개선과제 등록까지 완료된 상태다.

## 31.4.4 역할

### Incident Commander

\`\`\`text id=“inc31110” 전체 대응 총괄

심각도

우선순위

복구 승인

보고


\### Technical Lead

\`\`\`text id="inc31111"
원인 분석 조정

기술팀 역할 분배

증거 검토

복구안 제시

### Operations Lead

\`\`\`text id=“inc31112” 인프라·배포·트래픽 조치

모니터링

Runbook 실행


\### DBA

\`\`\`text id="inc31113"
DB Session·Lock·SQL·Pool 분석

DB 복구

데이터 대사 지원

### Business Owner

\`\`\`text id=“inc31114” 업무 영향

우회 가능성

데이터 결과

사용자 공지


\### Security Lead

\`\`\`text id="inc31115"
보안사건 분류

증거보존

침해확산 차단

법적 보고

### Scribe

\`\`\`text id=“inc31116” 타임라인

판단

조치

승인

결과


\---

\## 31.4.5 War Room 규칙

\`\`\`text id="inc31117"
한 명의 Incident Commander

하나의 공식 대화방·회의

한 명의 기록담당자

조치 전 소유자·예상효과·원복방법 공유

동시에 상충되는 변경 금지

확인된 사실과 추정을 구분

정기적인 상태보고

## 31.4.6 사실과 추정

\`\`\`text id=“inc31118” 사실 AP01 Pool Pending 48

추정 DB Pool 부족 가능성

확정 Connection Leak으로 Connection 반환 누락



보고서에는 세 수준을 구분한다.

\---

\## 31.4.7 장애 종료조건

장애 종료는 단순히 오류가 보이지 않는 상태가 아니다.

다음 다섯 영역을 확인한다.

\`\`\`text id="inc31119"
서비스

데이터

자원

모니터링

운영

## 31.4.8 서비스 종료조건

\`\`\`text id=“inc31120” 핵심 ServiceId Smoke 성공

오류율 정상범위

Timeout 정상범위

p95 목표범위

영향 사용자 재확인

트래픽 정상


\---

\## 31.4.9 데이터 종료조건

\`\`\`text id="inc31121"
부분 반영 0 또는 조치 완료

중복 건수 확인

UNKNOWN 거래 0 또는 Owner 지정

입력·출력 대사

외부기관 결과 대사

Cache 최신화

Batch Checkpoint 정상

## 31.4.10 자원 종료조건

\`\`\`text id=“inc31122” Busy Thread 정상

Hikari Pending 0

Pool 사용률 정상

CPU·Heap 안정

GC Pause 정상

Slow SQL 해소

Disk·Network 정상


\---

\## 31.4.11 모니터링 종료조건

\`\`\`text id="inc31123"
Alert 정상화

수집 누락 없음

거래로그 정상

배포 Version 표시

Dashboard 데이터 최신

장애 재발 감시시간 통과

## 31.4.12 운영 종료조건

\`\`\`text id=“inc31124” 임시 거래통제 해제 또는 유지계획

우회설정 원복

긴급 권한 회수

장애 공지

잔여 위험 Owner

PIR 일정

증거 보존


\---

\## 31.4.13 안정화 관찰

복구 직후 즉시 종료하지 않는다.

예:

\`\`\`text id="inc31125"
SEV-1
60분 이상 집중 관찰

SEV-2
30분 이상

SEV-3
15분 이상

정확한 시간은 서비스 특성과 재발 가능성에 따라 정한다.

## 31.4.14 사후검토 PIR

PIR 구성:

\`\`\`text id=“inc31126” 장애 요약

사용자 영향

타임라인

탐지 과정

복구 과정

근본 원인

기여 요인

잘된 점

개선할 점

데이터 영향

재발방지 과제


\---

\## 31.4.15 근본 원인

좋지 않은 원인:

\`\`\`text id="inc31127"
DB가 느렸다.

서버 문제였다.

개발자 실수였다.

트래픽이 많았다.

좋은 원인:

\`\`\`text id=“inc31128” SV.Customer.searchByName의 신규 조건이 복합 Index를 사용하지 못해 Full Scan을 발생시켰고,

평균 SQL 시간이 120ms에서 8.2초로 증가하면서 SV Hikari Pool 120개가 모두 점유돼 Connection Pending과 Tomcat Busy Thread가 증가했다.


\---

\## 31.4.16 기여 요인

\`\`\`text id="inc31129"
성능 회귀 테스트에 실제 데이터분포가 없었다.

Slow SQL Alert가 없었다.

Cache 전체 Evict와 Batch가 같은 시간에 실행됐다.

Pool Pending을 수집하지 않았다.

Rollback 판단이 늦었다.

근본 원인 하나만 수정하고 기여 요인을 방치하면 유사 장애가 재발한다.

## 31.4.17 탐지 실패 분석

\`\`\`text id=“inc31130” 왜 사용자 신고가 먼저였는가?

어떤 Metric이 없었는가?

Alert 임계치가 너무 높았는가?

Alert 담당자가 지정되지 않았는가?

Dashboard 데이터가 오래됐는가?


\---

\## 31.4.18 대응 실패 분석

\`\`\`text id="inc31131"
대표 거래를 늦게 확보했는가?

War Room 소집이 늦었는가?

승인권자가 없었는가?

Runbook이 부정확했는가?

Rollback Artifact를 찾지 못했는가?

증거 확보 없이 재기동했는가?

## 31.4.19 개선과제

모든 과제는 다음을 가진다.

\`\`\`text id=“inc31132” Action ID

문제

개선내용

우선순위

Owner

완료일

검증방법

품질 Gate

상태


\---

\## 31.4.20 개선과제 유형

\`\`\`text id="inc31133"
코드 수정

설정 수정

모니터링 추가

Alert 수정

Runbook 수정

테스트 추가

용량 증설

아키텍처 변경

교육·권한 개선

운영절차 개선

## 31.4.21 품질 Gate 반영

예:

\`\`\`text id=“inc31134” 장애 Handler에서 외부 호출 Timeout 누락

재발방지 모든 HTTP Client에 Timeout 검사

Gate Connect·Read Timeout 미설정 시 Build 실패


\`\`\`text id="inc31135"
장애
Slow SQL로 Pool 고갈

재발방지
Mapper별 Query Timeout·Plan 검증

Gate
신규 대량 SQL 실행계획 증적 필수

# 장애관리 데이터 모델

## Incident Table

sql id="inc31136" CREATE TABLE OM\_INCIDENT ( INCIDENT\_ID VARCHAR2(64) NOT NULL, TITLE VARCHAR2(500) NOT NULL, SEVERITY VARCHAR2(20) NOT NULL, STATUS VARCHAR2(30) NOT NULL, DETECTED\_AT TIMESTAMP NOT NULL, STARTED\_AT TIMESTAMP, ACKNOWLEDGED\_AT TIMESTAMP, RECOVERED\_AT TIMESTAMP, RESOLVED\_AT TIMESTAMP, CLOSED\_AT TIMESTAMP, AFFECTED\_BUSINESS VARCHAR2(500), AFFECTED\_SERVICE\_ID VARCHAR2(500), AFFECTED\_INSTANCES VARCHAR2(1000), IMPACT\_SUMMARY VARCHAR2(2000), INCIDENT\_COMMANDER VARCHAR2(100), ROOT\_CAUSE\_CODE VARCHAR2(100), ROOT\_CAUSE\_SUMMARY VARCHAR2(4000), CREATED\_BY VARCHAR2(100) NOT NULL, CREATED\_AT TIMESTAMP NOT NULL, UPDATED\_AT TIMESTAMP NOT NULL, VERSION\_NO NUMBER(18) NOT NULL, CONSTRAINT PK\_OM\_INCIDENT PRIMARY KEY (INCIDENT\_ID) );

## Incident Timeline

sql id="inc31137" CREATE TABLE OM\_INCIDENT\_EVENT ( EVENT\_ID VARCHAR2(64) NOT NULL, INCIDENT\_ID VARCHAR2(64) NOT NULL, EVENT\_TIME TIMESTAMP NOT NULL, EVENT\_TYPE VARCHAR2(50) NOT NULL, ACTOR\_ID VARCHAR2(100) NOT NULL, OBSERVATION VARCHAR2(4000), ACTION\_TAKEN VARCHAR2(4000), EVIDENCE\_REF VARCHAR2(1000), RESULT\_SUMMARY VARCHAR2(2000), CREATED\_AT TIMESTAMP NOT NULL, CONSTRAINT PK\_OM\_INCIDENT\_EVENT PRIMARY KEY (EVENT\_ID) );

## Evidence

sql id="inc31138" CREATE TABLE OM\_INCIDENT\_EVIDENCE ( EVIDENCE\_ID VARCHAR2(64) NOT NULL, INCIDENT\_ID VARCHAR2(64) NOT NULL, EVIDENCE\_TYPE VARCHAR2(50) NOT NULL, INSTANCE\_ID VARCHAR2(100), COLLECTED\_AT TIMESTAMP NOT NULL, STORAGE\_REF VARCHAR2(1000) NOT NULL, SHA256\_HASH VARCHAR2(64), SECURITY\_LEVEL VARCHAR2(30) NOT NULL, RETENTION\_UNTIL TIMESTAMP, COLLECTED\_BY VARCHAR2(100) NOT NULL, CONSTRAINT PK\_OM\_INCIDENT\_EVIDENCE PRIMARY KEY (EVIDENCE\_ID) );

# 정상 장애 대응 흐름

text id="inc31139" 1. Alert 또는 사용자 신고를 접수한다. 2. Incident ID를 발급한다. 3. 심각도와 영향 범위를 분류한다. 4. Incident Commander를 지정한다. 5. 대표 실패·정상 GUID를 확보한다. 6. Runtime·DB·배포 Snapshot을 수집한다. 7. 원인 후보를 계층별로 분류한다. 8. 서비스 복구안과 위험을 비교한다. 9. 승인된 최소 변경을 수행한다. 10. Health·Smoke·Metric으로 복구를 확인한다. 11. 데이터 부분 반영·중복·UNKNOWN을 대사한다. 12. 안정화 관찰 후 RECOVERED로 변경한다. 13. RCA와 재발방지 과제를 작성한다. 14. 과제·Gate 등록 후 CLOSED 처리한다.

# Timeout 장애 흐름

text id="inc31140" Timeout Alert ↓ ServiceId·GUID 확보 ↓ Timeout 계층 식별 ↓ Current Step 확인 ├─ WAIT\_DB\_CONNECTION ├─ EXECUTING\_SQL ├─ WAIT\_EXTERNAL └─ CPU·GC ↓ Pool·Thread·SQL·외부 비교 ↓ 거래통제·격리·Rollback ↓ 데이터 UNKNOWN 대사

# DB Pool 장애 흐름

text id="inc31141" Pool Active=max Pending 증가 ↓ Connection 획득 대기 거래 확인 ↓ Slow SQL·Lock·Leak 구분 ↓ 신규 거래 제한 ↓ Blocking 원인 제거 ↓ Pending 0 확인 ↓ 온라인 거래 복구

# 배포 장애 흐름

text id="inc31142" 배포 직후 오류 증가 ↓ 구·신 Version 비교 ↓ 신규 Version만 오류 확인 ↓ DB 호환성 확인 ↓ 문제 Instance 트래픽 제외 ↓ 직전 Artifact Rollback ↓ Smoke·데이터 확인

# 외부 시스템 장애 흐름

text id="inc31143" 외부 Timeout 증가 ↓ 내부 CPU·Pool 정상 확인 ↓ 특정 Interface 집중 확인 ↓ Circuit·Fallback ↓ 변경 거래 UNKNOWN 분리 ↓ 외부기관 상태조회·대사

# 정상 예시

\`\`\`text id=“inc31144” Incident INC-20260718-003

증상 SV 고객요약 조회 Timeout 증가

영향 전체 요청의 28% AP01·AP02 모두 영향

대표 GUID G-001 G-002 G-003

관찰 Hikari Active 120/120 Pending 64 CPU 41% GC 정상

Slow SQL SvCustomerMapper.selectCustomerHistory 평균 7.8초

복구 해당 조회조건 거래통제 Blocking Batch 중지 Index 적용 전 우회 SQL 배포

결과 Pending 0 p95 1.4초 오류율 정상

데이터 조회 거래이므로 변경 데이터 영향 없음

근본 원인 대량 이력조건에서 Index 미사용

재발방지 실행계획 Gate Pool Pending Alert 대량 데이터 성능 회귀시험


\---

\# 실패 예시

\`\`\`text id="inc31145"
사용자 신고
화면이 느림

조치
모든 Tomcat 재기동

결과
일시 정상

문제
Thread Dump 없음

Pool 상태 없음

Slow SQL 없음

재기동 후 다시 장애

데이터 확인 없음

원인
확인 불가

# 금지 예시

\`\`\`text id=“inc31146” 오류메시지만 보고 원인을 확정한다.

대표 GUID 없이 전체 로그부터 검색한다.

장애 접수시각을 실제 시작시각으로 단정한다.

사용자 한 명의 오류를 전체 장애로 선언한다.

반대로 전체 오류를 단순 사용자 문의로 처리한다.

재기동 전에 Thread·Pool·배포정보를 확보하지 않는다.

모든 인스턴스를 동시에 재기동한다.

특정 WAR 장애로 공용 Tomcat 전체를 재기동한다.

DB Pool 고갈 시 원인 확인 없이 Pool을 늘린다.

Timeout 장애 시 Timeout을 10배로 늘린다.

Slow SQL 장애에 Tomcat Thread만 늘린다.

외부 변경 거래를 신규 Request ID로 다시 실행한다.

Cache 장애 시 전체 Cache를 반복 삭제한다.

장애 중 여러 설정을 동시에 변경한다.

운영 서버에서 코드를 직접 수정한다.

승인 없이 DB Session을 강제 종료한다.

임시복구를 근본해결로 종료한다.

화면 정상만 확인하고 데이터 대사를 생략한다.

PARTIAL·UNKNOWN 거래를 SUCCESS로 처리한다.

장애 종료 후 임시 거래통제를 해제하지 않는다.

확인되지 않은 추정을 사용자에게 원인으로 공지한다.

타임라인에 변경자와 변경값을 기록하지 않는다.

RCA를 개인의 실수로만 결론내린다.

재발방지 과제에 Owner와 완료일이 없다.

동일 장애가 발생해도 테스트와 Gate를 추가하지 않는다.


\---

\# 연계 규칙

\## 거래로그 연계

\`\`\`text id="inc31147"
Incident ID

↔ GUID

↔ TraceId

↔ ServiceId

↔ 오류코드

↔ 배포 Version

## 배포 연계

\`\`\`text id=“inc31148” Incident ID

↔ Deploy ID

↔ Artifact Version

↔ Config Version

↔ DB Migration


\## 데이터 연계

\`\`\`text id="inc31149"
Incident ID

↔ 대사 Query

↔ UNKNOWN 거래

↔ 재처리 ID

↔ 보상 거래

## 변경관리 연계

\`\`\`text id=“inc31150” Incident ID

↔ 결함 ID

↔ Pull Request

↔ 테스트

↔ Release

↔ 품질 Gate


\---

\# 책임 경계와 RACI

| 활동 | 운영 | 업무개발 | FW | AA | DBA | TA | 보안 | DevOps | 업무 Owner |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| 장애 감지 | A/R | C | C | I | C | R/C | C | C | I |
| 장애 선언 | R | C | C | A/C | C | C | C | C | A/C |
| 영향 분석 | R | A/R | C | A/C | C | C | C | C | A/C |
| 대표 거래 | R/C | A/R | C | C | I | I | I | I | C |
| 증거 수집 | A/R | C | R/C | C | R/C | R/C | C | C | I |
| 기술 원인 | C | R | R | A | R | R | R/C | C | I |
| 거래통제 | R | C | C | A/C | I | I | C | I | A/C |
| 트래픽 격리 | R/C | I | I | C | I | A/R | I | C | I |
| DB 조치 | C | C | I | C | A/R | I | C | I | I |
| Rollback | R/C | C | C | A/C | C | C | C | A/R | A/C |
| 데이터 대사 | C | A/R | C | C | A/R | I | C | I | A/C |
| 대외 공지 | R | C | I | C | I | I | C | I | A/R |
| 종료 승인 | R | C | C | A/C | C | C | C | C | A/R |
| RCA | R/C | R | R | A | R/C | R/C | R/C | C | C |
| 재발방지 | C | R | R | A | R | R | R | R | C |

\---

\# 성능·용량·확장성

\## 진단 수집 부하

진단 기능이 업무 장애를 일으키면 안 된다.

\`\`\`text id="inc31151"
상태 Snapshot

→ 짧은 Timeout

→ 제한된 응답 크기

→ 개인정보 제외

→ 수집 실패 시 UNKNOWN

→ 업무 거래 계속

운영 진단 설계도 진단 등록·수집 실패가 업무 거래에 영향을 주지 않고 해당 항목만 UNKNOWN으로 표시하도록 요구한다.

## Snapshot 주기

예:

| 상태 | 수집주기 |
| --- | --- |
| 정상 | 30~60초 |
| Warning | 10~30초 |
| 장애 | 5~10초 |
| 상세 거래 | 화면 요청 시 |
| Thread Dump | 수동 승인 |

너무 짧은 주기로 모든 Thread와 SQL을 수집하면 업무 자원을 소모할 수 있다.

## Active Transaction Registry

보관:

\`\`\`text id=“inc31152” 현재 실행 중 거래

최근 Slow 거래 100~1,000건

최근 Slow SQL 100~1,000건



무제한 보관하지 않는다.

\## Incident Evidence 용량

\`\`\`text id="inc31153"
Thread Dump

Heap Dump

로그 묶음

DB Report

스크린샷

은 별도 보안 Storage와 보존정책이 필요하다.

# 보안·개인정보·감사

| 영역 | 기준 |
| --- | --- |
| 진단 API | 관리망·OM만 허용 |
| 인증 | 관리자 JWT·내부인증 |
| Route | 외부 Gateway 등록 금지 |
| SQL | ID만, Parameter 원문 금지 |
| 거래 | 요청·응답 Body 원문 금지 |
| 사용자 | 마스킹·권한별 표시 |
| Thread Dump | 접근제한 |
| Heap Dump | 고보안·암호화 |
| Incident | 조회·변경 감사 |
| DB Session Kill | 승인·감사 |
| 거래통제 | 사유·승인·해제 |
| Rollback | 승인·이력 |
| Evidence | Hash·보존·파기 |
| 보안사고 | 증거보존 우선 |
| 보고서 | 개인정보 제거 |

내부 런타임 진단 API는 일반 브라우저와 외부망에 공개하지 않고, SQL Parameter·JWT·Session ID·고객정보를 노출하지 않아야 한다.

# 운영·모니터링·장애 대응

## 장애관리 Metric

\`\`\`text id=“inc31154” incident.detected.count

incident.active.count

incident.severity.count

incident.mtta

incident.mttd

incident.mttr

incident.reopened.count

incident.data.unknown.count

incident.rollback.count

incident.recurrence.count


\## Runtime Metric

\`\`\`text id="inc31155"
tomcat.threads.busy

tomcat.queue.size

jvm.cpu

jvm.heap

jvm.gc.pause

hikari.active

hikari.idle

hikari.pending

hikari.acquire

transaction.inflight

transaction.timeout

sql.slow

external.wait

## 장애 Dashboard

첫 화면에서 다음 문장을 제공하는 것이 목표다.

\`\`\`text id=“inc31156” CENTER1 AP01의 SV WAR에서 DB Connection Pool이 모두 사용 중입니다.

48개 Thread가 Connection을 기다리고 있으며, 주요 영향 거래는 SV.Customer.selectSummary입니다.

CPU와 GC는 정상입니다.



운영자가 여러 그래프를 직접 조합하기 전에 원인 후보와 근거를 한 화면에서 확인할 수 있어야 한다.

\---

\# 자동검증 및 품질 Gate

\## 1. 관측성 Gate

\`\`\`text id="inc31157"
ServiceId

GUID·TraceId

시작·종료 로그

오류코드

처리시간

Instance

Version

Metric

## 2\. Runtime Gate

\`\`\`text id=“inc31158” Thread Metric

Hikari Active·Idle·Pending

Connection Acquire

GC

Slow SQL ID

외부 Interface ID

Active Transaction


\## 3. Timeout Gate

\`\`\`text id="inc31159"
상위·하위 Timeout 순서

Query Timeout

HTTP Connect·Read

Bounded Executor

Queue·Rejected Metric

변경 거래 UNKNOWN

## 4\. Runbook Gate

\`\`\`text id=“inc31160” 장애 유형

탐지조건

확인순서

임시복구

원복

담당자

종료조건


\## 5. 배포 Gate

\`\`\`text id="inc31161"
Rollback Artifact

Config Rollback

DB 호환성

Health·Smoke

Incident 연락망

## 6\. 데이터 Gate

\`\`\`text id=“inc31162” 부분 반영 Test

중복 Test

Timeout UNKNOWN

대사 Query

재처리 절차


\## 7. 운영 준비도 Gate

운영 전환 단계에서는 장애 대응 Runbook, 배포·Rollback, 백업·복구, 비상연락망, 운영자 교육과 Go/No-Go 승인을 함께 준비해야 한다.

\---

\# 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
|---|---|---|
| INC-001 | 사용자 1명 입력 오류 | 장애 미선언 |
| INC-002 | 전체 로그인 실패 | SEV-1 |
| INC-003 | 특정 ServiceId 30% 실패 | SEV-2 |
| INC-004 | 특정 지점만 실패 | 부분 영향 분류 |
| INC-005 | AP01만 오류 | Instance 격리 |
| INC-006 | 장애 시작시각 역산 | Metric 기준 |
| INC-007 | 대표 실패 GUID 확보 | 전체 Trace |
| INC-008 | 정상 GUID 비교 | 병목구간 식별 |
| INC-009 | GUID 없음 | 대체조건 검색·Gap |
| INC-010 | 배포 직후 오류 | Version 연계 |
| INC-011 | Config Drift | Instance 차이 |
| INC-012 | Cache Version 불일치 | 국소 오류 확인 |
| INC-013 | 거래통제 적용 | 신규 요청 차단 |
| INC-014 | 문제 Instance Drain | 잔여 서비스 정상 |
| INC-015 | 증거 전 재기동 요청 | 최소 Snapshot 선행 |
| INC-016 | Thread Dump 3회 | 지속 대기 식별 |
| INC-017 | Heap Dump 수집 | 보안 승인 |
| INC-018 | 전체 Tomcat 재기동 | 영향도 검토 |
| INC-019 | WAR Rollback | DB 호환 확인 |
| INC-020 | 설정 Rollback | Config Version 복구 |
| INC-021 | 외부 조회 Timeout | 멱등 재시도 |
| INC-022 | 외부 변경 Timeout | UNKNOWN |
| INC-023 | 중복 재전송 | 차단 |
| INC-024 | 부분 Commit | 데이터 오류 탐지 |
| INC-025 | Master·Detail 대사 | 불일치 0 |
| INC-026 | Timeout율 급증 | 장애 Alert |
| INC-027 | Browser Timeout | 서버 거래 상태 확인 |
| INC-028 | Gateway Connect Timeout | Backend 상태 확인 |
| INC-029 | Gateway Read Timeout | 하위 지연 확인 |
| INC-030 | TCF Timeout | Current Step |
| INC-031 | Query Timeout | SQL 중단 확인 |
| INC-032 | Hikari Connection Timeout | Pending 확인 |
| INC-033 | Active=max·Pending↑ | Pool 고갈 |
| INC-034 | Active=max·Slow SQL | SQL 원인 |
| INC-035 | Active=max·SQL 정상 | Pool·Transaction 분석 |
| INC-036 | Connection Leak | Active 미반환 |
| INC-037 | 외부 호출 Transaction 내부 | 장기 점유 |
| INC-038 | Batch Pool 독점 | Batch 중지 |
| INC-039 | DB Lock | Blocking Session |
| INC-040 | DB Deadlock | Transaction 오류 |
| INC-041 | JVM Deadlock | Thread 탐지 |
| INC-042 | CPU 95% 지속 | CPU 장애 |
| INC-043 | Heap 지속 상승 | Memory Leak |
| INC-044 | Full GC 증가 | GC 장애 |
| INC-045 | Busy Thread 100% | Queue 증가 |
| INC-046 | Busy↑·Pending 0·CPU 낮음 | 외부·Lock 분석 |
| INC-047 | 외부 WAIT 증가 | Interface 장애 |
| INC-048 | Cache Miss 폭증 | DB 보호 |
| INC-049 | Timeout Worker 폭증 | Executor Alert |
| INC-050 | Pool 즉시 확대 | 위험 검토 |
| INC-051 | Timeout 10배 확대 | 변경 차단 |
| INC-052 | DB Session Kill | 승인·감사 |
| INC-053 | Smoke 성공 | 서비스 복구 후보 |
| INC-054 | Smoke 성공·오류율 높음 | 미복구 |
| INC-055 | 서비스 정상·데이터 불일치 | 미종료 |
| INC-056 | UNKNOWN 잔존 | Owner 지정 |
| INC-057 | Alert 정상화 | 관찰 유지 |
| INC-058 | 안정화 30분 | RECOVERED |
| INC-059 | 임시 거래통제 잔존 | 운영조치 필요 |
| INC-060 | 타임라인 누락 | 종료 차단 |
| INC-061 | Incident Commander 없음 | 운영 실패 |
| INC-062 | 상충 변경 동시 수행 | War Room 통제 |
| INC-063 | 사실·추정 혼재 | 보고 수정 |
| INC-064 | 사용자 공지 | 영향·다음 시각 포함 |
| INC-065 | 원인 미확정 종료 | RCA 후속 |
| INC-066 | PIR 작성 | 타임라인·원인 |
| INC-067 | 개인실수만 원인 | 시스템 요인 보완 |
| INC-068 | 탐지 실패 | Alert 개선 |
| INC-069 | 대응 지연 | Runbook 개선 |
| INC-070 | 재발방지 Owner 없음 | Close 차단 |
| INC-071 | 개선 테스트 추가 | 회귀 PASS |
| INC-072 | 정적 Gate 추가 | 위반 차단 |
| INC-073 | 동일 장애 재훈련 | MTTR 단축 |
| INC-074 | Runtime API 장애 | 업무 지속 |
| INC-075 | Snapshot 일부 누락 | UNKNOWN 표시 |
| INC-076 | 진단 API 외부 접근 | 차단 |
| INC-077 | SQL Parameter 노출 | Security Gate 실패 |
| INC-078 | Heap Dump 미인가 조회 | 차단 |
| INC-079 | Evidence Hash 검증 | 무결성 PASS |
| INC-080 | 전체 Runbook 훈련 | 종료조건 충족 |

\---

\# 따라 하는 실무 절차

\## 1단계. Incident ID를 발급한다

\`\`\`text id="inc31163"
INC-YYYYMMDD-NNN

## 2단계. 심각도와 영향을 분류한다

\`\`\`text id=“inc31164” 업무

사용자

지점

실패율

데이터

보안


\## 3단계. 대표 거래를 확보한다

\`\`\`text id="inc31165"
실패 GUID 3건

정상 GUID 1건

장애 전 정상 1건

## 4단계. 증거 Snapshot을 저장한다

\`\`\`text id=“inc31166” Thread

JVM

Pool

SQL

배포

설정

외부


\## 5단계. 원인 후보를 분류한다

\`\`\`text id="inc31167"
Gateway

TCF

업무

DB Pool

SQL

JVM

외부

배포

## 6단계. 복구안을 비교한다

\`\`\`text id=“inc31168” 효과

위험

데이터 영향

원복 가능성

승인자


\## 7단계. 최소 변경으로 복구한다

한 번에 한 조치씩 수행하고 결과를 기록한다.

\## 8단계. 서비스와 데이터를 확인한다

\`\`\`text id="inc31169"
Smoke

오류율

p95

대사

UNKNOWN

## 9단계. 안정화 후 종료한다

임시조치·Alert·잔여 위험을 확인한다.

## 10단계. PIR과 재발방지를 등록한다

\`\`\`text id=“inc31170” Root Cause

기여 요인

Action

Owner

기한

Gate


\---

\# 완료 체크리스트

\## 접수·영향

| 확인 항목 | 완료 |
|---|:---:|
| Incident ID가 있다. | □ |
| 접수·시작시각을 구분했다. | □ |
| 심각도를 확정했다. | □ |
| 영향 업무·ServiceId가 있다. | □ |
| 사용자·지점 범위를 확인했다. | □ |
| 데이터·보안 영향을 확인했다. | □ |
| Incident Commander가 있다. | □ |
| 비상연락망이 작동한다. | □ |

\## 증거·분석

| 확인 항목 | 완료 |
|---|:---:|
| 대표 실패 GUID를 확보했다. | □ |
| 정상 거래와 비교했다. | □ |
| 대상 Instance·Version을 확인했다. | □ |
| Thread 상태를 확보했다. | □ |
| CPU·Heap·GC를 확보했다. | □ |
| Pool Active·Idle·Pending을 확인했다. | □ |
| Slow SQL·Lock을 확인했다. | □ |
| 외부 연계상태를 확인했다. | □ |
| 배포·설정 변경을 확인했다. | □ |
| 사실·추정·확정을 구분했다. | □ |

\## 복구

| 확인 항목 | 완료 |
|---|:---:|
| 복구안의 효과와 위험을 비교했다. | □ |
| 원복방법이 있다. | □ |
| 조치 승인자가 있다. | □ |
| 필요한 거래만 통제했다. | □ |
| 문제 Instance만 격리했다. | □ |
| 재기동 전 증거를 확보했다. | □ |
| Pool·Timeout 무분별한 확대를 피했다. | □ |
| 외부 변경 거래를 중복 재전송하지 않았다. | □ |
| 모든 조치를 타임라인에 기록했다. | □ |

\## 종료

| 확인 항목 | 완료 |
|---|:---:|
| 핵심 Smoke가 성공했다. | □ |
| 오류율·Timeout이 정상이다. | □ |
| p95가 정상이다. | □ |
| Thread·Pool·JVM이 정상이다. | □ |
| 부분 반영을 확인했다. | □ |
| 중복·미처리를 확인했다. | □ |
| UNKNOWN 거래를 조치했다. | □ |
| Alert가 정상화됐다. | □ |
| 임시조치를 정리했다. | □ |
| 안정화 관찰을 완료했다. | □ |

\## 사후관리

| 확인 항목 | 완료 |
|---|:---:|
| 장애 타임라인이 완전하다. | □ |
| 근본 원인이 증거로 설명된다. | □ |
| 기여 요인을 작성했다. | □ |
| 탐지·대응 문제를 작성했다. | □ |
| 개선과제 Owner가 있다. | □ |
| 완료기한이 있다. | □ |
| 테스트를 추가했다. | □ |
| 품질 Gate를 개선했다. | □ |
| Runbook을 수정했다. | □ |
| 장애훈련 계획이 있다. | □ |

\---

\# 변경·호환성·폐기 관리

\## 장애등급 변경

심각도 기준을 변경할 때 다음을 함께 수정한다.

\`\`\`text id="inc31171"
Alert

보고체계

소집대상

보고주기

승인권자

SLA

## Runbook 변경

\`\`\`text id=“inc31172” 장애 사례

변경 이유

신규 절차

검증훈련

승인

적용일



Runbook은 문서만 수정하지 않고 실제 훈련으로 검증한다.

\## Timeout 변경

\`\`\`text id="inc31173"
기존값

변경값

전체 Budget

Thread·Pool 영향

부하시험

Rollback

## Pool 변경

\`\`\`text id=“inc31174” WAR별 Pool

Tomcat별 합계

VM별 합계

DB 최대 Session

Peak TPS

부하 결과


\## 진단 API 변경

구 OM과 신 업무 WAR가 혼재할 수 있으므로 Version과 선택 필드 호환성을 유지한다.

\`\`\`text id="inc31175"
runtime-api/v1

runtime-api/v2

## Metric 폐기

기존 Alert와 Dashboard 호출량이 0인 것을 확인한 뒤 폐기한다.

## Incident 증거 폐기

\`\`\`text id=“inc31176” 보존기간

감사·법적 Hold

개인정보

암호화

승인된 파기


\---

\# 시사점

\## 핵심 아키텍처 판단

첫째, 장애 대응의 출발점은 재기동이 아니라 영향 범위와 대표 거래 확보다.

둘째, 복구와 원인 분석은 목적과 책임을 분리하되 증거와 타임라인으로 연결해야 한다.

셋째, Timeout은 증상이며 실제 원인은 DB Pool, Slow SQL, 외부 지연, CPU, GC, Thread 등일 수 있다.

넷째, DB Pool 사용률만으로 장애를 판단할 수 없고 Active·Idle·Pending·Connection 획득시간과 실행 중인 ServiceId·SQL을 연결해야 한다.

다섯째, 하나의 Tomcat에 여러 WAR가 배포된 구조에서는 JVM·Connector Thread 장애가 여러 업무에 동시에 영향을 줄 수 있다.

여섯째, 화면이 다시 열리는 것은 서비스 복구일 뿐 데이터 복구 완료를 의미하지 않는다.

일곱째, 장애 종료는 서비스·데이터·자원·모니터링·운영 다섯 영역이 모두 정상화됐을 때 가능하다.

여덟째, 장애 사후분석은 책임자를 찾는 활동이 아니라 재발을 가능하게 만든 기술·절차·탐지체계를 개선하는 활동이다.

\---

\## 주요 위험

| 위험 | 결과 |
|---|---|
| 증상과 원인 혼동 | 잘못된 조치 |
| 대표 GUID 없음 | 분석 지연 |
| 증거 전 재기동 | 근본 원인 소실 |
| 전체 재기동 | 영향 범위 확대 |
| Pool 무분별 확대 | DB 과부하 |
| Timeout 확대 | Thread 장기점유 |
| 외부 재전송 | 중복 거래 |
| 복구·분석 역할 혼재 | 의사결정 지연 |
| 데이터 대사 없음 | 숨은 오류 잔존 |
| 타임라인 누락 | 조치 효과 판단 불가 |
| 사실·추정 혼재 | 잘못된 보고 |
| 종료조건 부족 | 조기 종료·재발 |
| 개인실수 중심 RCA | 시스템 개선 누락 |
| 개선과제 Owner 없음 | 재발방지 미완료 |
| 런타임 진단 부족 | 원인판정 불가 |
| 진단 API 과다노출 | 보안사고 |
| 무제한 Timeout Executor | Thread 자원고갈 |

\---

\## 우선 보완 과제

1\. 업무 WAR별 \`ActiveTransactionRegistry\`와 Current Step을 구현한다.
2\. Hikari Active·Idle·Pending·Connection Acquire Time을 수집한다.
3\. Tomcat Connector Busy Thread와 Queue를 수집한다.
4\. Mapper SQL ID·외부 Interface ID를 현재 거래에 연결한다.
5\. Runtime Cause Analyzer에 \`DB\_POOL\_WAIT\`, \`SLOW\_SQL\`, \`EXTERNAL\_WAIT\`, \`CPU\_OVERLOAD\`, \`GC\_PRESSURE\` 판정규칙을 구현한다.
6\. \`newCachedThreadPool()\` 기반 Timeout Executor를 상한이 있는 Pool로 전환한다.
7\. OM에 Incident·Timeline·Evidence·Action Item 관리기능을 구축한다.
8\. 배포·설정·Cache Version을 Incident 화면에 연결한다.
9\. 대표 GUID·Thread·Pool·SQL을 자동 수집하는 Evidence Snapshot 기능을 구현한다.
10\. 변경 거래 Timeout의 \`UNKNOWN\` 상태와 대사 기능을 구축한다.
11\. 서비스·데이터·자원·모니터링·운영 종료조건을 Runbook에 반영한다.
12\. 장애등급별 War Room·보고주기·승인권자를 확정한다.
13\. 운영 전 장애주입·재기동·Rollback·Pool 고갈 훈련을 수행한다.
14\. 장애 후 개선과제를 테스트·CI Gate에 연결한다.
15\. 진단 API를 관리망·OM 인증으로 제한하고 민감정보 노출을 차단한다.

\---

\## 중장기 발전 방향

\`\`\`text id="inc31177"
사용자 신고 중심
↓
Metric 자동감지

로그 수작업 검색
↓
GUID 대표 거래 자동확보

CPU·Heap 단순 상태
↓
Thread·Pool·SQL·ServiceId 통합

원인 추측
↓
근거 기반 Cause Analyzer

재기동 중심
↓
거래통제·격리·Rollback

서비스 복구
↓
데이터 대사 포함 복구

장애 일지
↓
OM Incident Timeline

사후 보고
↓
PIR·RCA·Action Gate

일회성 대응
↓
정기 장애훈련·지속 개선

# 마무리말

장애에 대응하는 과정은 다음 질문에 답하는 일이다.

\`\`\`text id=“inc31178” 실제 사용자 영향은 무엇인가?

어떤 업무와 ServiceId가 영향을 받는가?

전체 장애인가, 특정 지점·Instance 장애인가?

장애는 언제 시작됐는가?

대표 실패 GUID와 정상 GUID가 있는가?

Gateway부터 DB·외부까지 어느 구간이 느린가?

Timeout은 어느 계층에서 발생했는가?

DB Connection을 기다리는가, SQL을 실행 중인가?

Tomcat Thread가 왜 바쁜가?

CPU와 GC는 정상인가?

어떤 배포·설정 변경 이후 발생했는가?

재기동 전에 어떤 증거를 확보해야 하는가?

거래통제와 Instance 격리 중 어느 것이 안전한가?

Pool과 Timeout을 변경하면 장애가 확대되지 않는가?

외부 변경 거래가 이미 성공했을 가능성은 없는가?

부분 반영·중복·미처리 데이터가 있는가?

서비스가 정상화된 뒤 데이터도 정상인가?

Alert와 Dashboard가 정상화됐는가?

임시조치와 우회설정을 언제 해제할 것인가?

근본 원인과 장애를 확대시킨 기여 요인은 무엇인가?

어떤 테스트·Metric·Gate를 추가해야 재발하지 않는가?



제31장의 핵심 흐름은 다음과 같다.

\`\`\`text id="inc31179"
장애 감지
↓
영향 범위
↓
대표 거래
↓
증거 Snapshot
↓
원인 후보
↓
안전한 복구
↓
데이터 대사
↓
종료조건
↓
RCA·재발방지

가장 중요한 원칙은 다음과 같다.

\`\`\`text id=“inc31180” 장애 대응은 서버를 다시 살리는 것만으로 끝나지 않는다.

사용자 영향을 줄이고, 데이터가 정확함을 확인하며, 원인을 증거로 설명하고,

같은 장애를 막는 테스트와 통제를 시스템에 다시 반영해야 한다.

빠른 복구와 정확한 분석, 서비스 정상과 데이터 정상, 임시조치와 근본조치를

구분해서 관리할 수 있어야 운영 가능한 시스템이 된다. \`\`\`
