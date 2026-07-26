<!-- source: ztcf-집필본/NSIGHT TCF Chapter 부록 H. Runbook H.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 확장 부록 H. 장애 대응 Runbook

## 도입 전 안내말

장애 대응의 목적은 장애 원인을 가장 먼저 맞히는 것이 아니다.

첫 번째 목적은 사용자와 데이터에 미치는 피해를 더 이상 확대하지 않는 것이다.

두 번째 목적은 서비스를 안전하게 복구하는 것이다.

세 번째 목적은 복구 과정에서 발생할 수 있는 중복·부분 반영·보안 문제를 확인하는 것이다.

근본 원인 분석은 서비스가 안정된 뒤 수행한다.

장애 대응 우선순위

1\. 사람과 보안 보호
2\. 데이터 손상 확대 방지
3\. 신규 장애 거래 유입 통제
4\. 서비스 복구
5\. 데이터 정합성 확인
6\. 정상 운영 확인
7\. 근본 원인 분석
8\. 재발 방지

장애가 발생했을 때 가장 위험한 행동은 충분한 증거 없이 다음 조치를 반복하는 것이다.

모든 서버 재기동

모든 WAR 재배포

Timeout 일괄 증가

DB Pool 일괄 확대

Gateway 우회

인증·권한 검사 해제

실패 거래 무조건 재처리

운영 DB 직접 수정

이러한 조치는 일시적으로 증상을 사라지게 할 수 있지만 다음 문제를 남긴다.

장애 원인 증거 소실

중복 처리

부분 Commit

보안 우회

다른 업무로 장애 확산

복구시간 증가

같은 장애 재발

NSIGHT TCF 장애 대응은 다음의 공통 식별자를 중심으로 수행한다.

Incident ID

업무코드

화면 ID·이벤트 ID

ServiceId

거래코드

GUID·Correlation ID

TraceId

Instance ID

WAR·Artifact Version

SQL ID

외부 대상 시스템

하나의 대표 실패 거래를 확보한 뒤 다음 계층을 순서대로 비교한다.

사용자·화면

→ L4·Apache

→ tcf-gateway

→ JWT·Session

→ 업무 WAR·Tomcat

→ TCF·STF·Dispatcher

→ Handler·Facade·Service

→ DAO·Mapper·DB

→ 외부 연계

→ 거래로그·감사로그·Metric

장애 대응의 핵심 원칙은 다음과 같다.

증상보다 범위를 먼저 확인한다.

재기동보다 증거를 먼저 확보한다.

원인 분석보다 피해 통제를 먼저 수행한다.

복구 성공과 데이터 정상은 별도로 확인한다.

장애 종료와 근본 원인 해결을 구분한다.

# 문서 개요

## 목적

본 Runbook의 목적은 NSIGHT TCF 운영 중 발생할 수 있는 애플리케이션·Gateway·JWT·Tomcat·JVM·DB·외부 연계·Cache·Batch·배포 장애에 대해 운영자가 반복 가능하고 승인 가능한 절차로 대응하도록 하는 것이다.

세부 목적은 다음과 같다.

장애의 조기 인지

영향 범위와 심각도 판정

대표 실패 거래 확보

계층별 원인 범위 축소

거래통제·Traffic Drain을 통한 피해 제한

Rollback·재기동·우회 판단 표준화

UNKNOWN·부분 반영·중복 거래 대사

복구 완료조건 표준화

장애 커뮤니케이션 일관성 확보

보안·개인정보 증거 보호

PIR과 재발 방지 과제 관리

장애훈련과 자동검증 기준 제공

## 적용범위

| 영역 | 적용 대상 |
| --- | --- |
| 채널 | WebTopSuite·Browser·업무 UI |
| Network | DNS·L4·Apache·SSL |
| Gateway | tcf-gateway Route·JWT·Proxy |
| 인증 | SSO·JWT·Session·JWKS |
| 온라인 거래 | 업무 WAR의 TCF 거래 |
| Framework | STF·Dispatcher·Timeout·ETF |
| WAS | Tomcat·Thread·JVM·GC |
| 데이터 | HikariCP·Oracle·SQL·Lock |
| 외부 연계 | tcf-eai·타 업무 WAR·외부기관 |
| Cache | Ehcache·공통코드·Service Catalog |
| 운영관리 | tcf-om·거래통제·Timeout 정책 |
| 관측 | 거래로그·감사로그·Metric·Dashboard |
| Batch | tcf-batch·Scheduler·Job |
| 배포 | WAR·설정·Route·DB Migration |
| DR | 인스턴스·센터·DB 전환 |
| 보안 | Token·Key·계정·관리 Endpoint 사고 |

## 대상 독자

운영자

WAS·인프라 담당자

애플리케이션 아키텍트

Framework 개발자

업무 개발자

DBA

보안 담당자

DevOps 담당자

QA·성능 담당자

업무 책임자

PMO·Incident Manager

## 선행조건

24시간 비상연락망

Incident Commander 지정기준

업무·Module Owner 목록

ServiceId·거래코드 관리대장

Gateway Route 관리대장

WAR·Context·Instance 목록

모니터링 Dashboard와 Alert

거래로그·감사로그 조회권한

OM 거래통제 권한

배포·Rollback Runbook

DB 백업·복구 Runbook

검증된 직전 Artifact

대표 Smoke Test 목록

데이터 대사 SQL

보안사고 대응체계

# 핵심 관점

장애는 하나의 Error Log가 아니다.

사용자가 경험한 증상,
거래가 실패한 계층,
자원이 고갈된 원인,
데이터가 남은 상태를

하나의 시간축으로 연결해야 한다.

# 핵심 용어

| 용어 | 정의 |
| --- | --- |
| Incident | 서비스·데이터·보안에 실제 또는 잠재적 영향을 주는 사건 |
| Alert | 임계치·규칙에 따라 생성된 이상 신호 |
| Event | 시스템에서 발생한 상태변화 |
| SEV | 장애의 업무영향 심각도 |
| Incident Commander | 장애 대응 전체 지휘·의사결정 책임자 |
| War Room | 장애 대응 인력이 협업하는 통합 채널 |
| Containment | 장애 확대를 막는 임시 통제 |
| Mitigation | 사용자 영향을 감소시키는 임시 조치 |
| Recovery | 서비스를 정상 상태로 복구하는 행위 |
| Remediation | 장애 원인을 영구적으로 제거하는 조치 |
| Workaround | 근본 원인을 해결하지 않고 우회하는 임시 방법 |
| Traffic Drain | 특정 Instance에 신규 요청을 보내지 않는 조치 |
| Transaction Control | 특정 거래·사용자·채널 등을 차단하는 운영 통제 |
| Rollback | 직전 검증된 소프트웨어·설정 상태로 복원 |
| Failover | 대체 시스템·센터·DB로 처리 주체를 전환 |
| Failback | 장애 복구 후 원래 주 시스템으로 복귀 |
| UNKNOWN | 응답은 실패했지만 실제 처리결과를 확정하지 못한 상태 |
| Data Reconciliation | Master·History·외부 결과를 비교해 정합성을 확인하는 작업 |
| RTO | 서비스 복구 목표시간 |
| RPO | 허용 가능한 데이터 손실 기준 |
| PIR | Post Incident Review, 장애 사후 분석 |
| RCA | Root Cause Analysis, 근본 원인 분석 |
| MTTD | 장애 발생부터 탐지까지의 평균시간 |
| MTTA | 탐지부터 대응 시작까지의 평균시간 |
| MTTR | 장애 발생부터 복구까지의 평균시간 |

# H.1 문제 정의 및 설계 배경

## H.1.1 NSIGHT 장애의 특성

NSIGHT TCF는 여러 업무 WAR와 공통 Platform Module이 연결된 구조다.

사용자

→ L4·Apache

→ Gateway

→ 업무 WAR

→ TCF

→ DB·외부 시스템

→ OM·로그·관측

따라서 하나의 사용자 증상이 여러 원인에서 발생할 수 있다.

예:

화면이 느리다.

가능한 원인

Gateway Connection Pool

Tomcat Busy Thread

JVM GC

Hikari Connection 대기

Slow SQL

DB Lock

외부 연계 지연

Cache Stampede

특정 WAR의 자원독점

또한 같은 원인이라도 영향범위가 다를 수 있다.

하나의 ServiceId만 장애

하나의 업무 WAR만 장애

하나의 Tomcat Instance만 장애

업무그룹 전체 장애

Gateway 전체 장애

DB 전체 장애

센터 전체 장애

따라서 오류 메시지만 보고 대응하면 잘못된 계층을 조치할 가능성이 높다.

## H.1.2 장애 대응 실패의 대표 원인

영향범위를 확인하지 않는다.

대표 GUID를 확보하지 않는다.

최근 배포·설정 변경을 확인하지 않는다.

장애 증거 수집 전에 재기동한다.

단일 Instance 문제를 전체 시스템 문제로 판단한다.

DB Lock을 애플리케이션 문제로 판단한다.

외부 시스템 Timeout을 TCF Timeout 증가로 해결한다.

응답 실패를 실제 Transaction 실패로 간주한다.

서비스 복구 후 데이터 대사를 생략한다.

복구 담당과 의사결정 담당이 분리되지 않는다.

# H.2 현행 구조와 운영 준비상태

## H.2.1 현재 확인되는 운영 기반

| 기능 | 구현·설계 상태 | Runbook 활용 |
| --- | --- | --- |
| ServiceId 거래통제 | tcf-om 기반 | 특정 거래 차단 |
| Service Timeout 정책 | tcf-om 기반 | 거래별 제한 확인 |
| 배포·Rollback 관리 | tcf-om 기반 | 배포이력·복구 |
| 거래로그 | TCF 시작·종료 기반 | GUID 추적 |
| 감사로그 | 기본 구조 | 중요 행위 확인 |
| AP·DB·배포상태 수집 | tcf-batch 기반 | Dashboard |
| Liveness·Health | Spring Actuator 기반 | 기동상태 확인 |
| Gateway 거래로그 | 구조 존재 | Route·인증 추적 |
| JWT·JWKS | tcf-jwt 기반 | 인증 장애 분석 |
| Hikari·JVM 진단 | 관리·도구 기반 | Resource 원인 분석 |

## H.2.2 추가 확인이 필요한 항목

모든 WAR의 Readiness·Deep Health 구현 여부

업무별 Thread·Pool 격리 여부

자동 Alert 임계치

분산 Trace 연계 수준

감사로그 저장 실패 정책

거래로그 DB 장애 시 보정절차

UNKNOWN 거래 자동대사

Gateway 우회 승인절차

DR 센터 실제 리허설 결과

모든 업무의 대표 Smoke 목록

이 항목은 운영전환 전에 확정해야 한다.

# H.3 요구사항과 제약조건

## H.3.1 장애 대응 요구사항

장애 발생 5분 이내 Incident를 개설한다.

대표 실패 거래를 최소 한 건 확보한다.

영향 업무·사용자·지역·시간을 분류한다.

복구조치 전에 증거를 확보한다.

Blocker·Critical 장애는 War Room을 개설한다.

데이터 변경거래는 복구 후 정합성 대사를 수행한다.

보안사고 가능성이 있으면 일반 장애보다 보안통제를 우선한다.

장애 종료 전 Smoke·Data·Monitoring을 모두 확인한다.

중대한 장애는 PIR을 수행한다.

## H.3.2 제약조건

운영 명령은 승인된 계정으로만 수행한다.

운영 DB 직접 DML은 승인과 2인 확인이 필요하다.

전체 시스템 재기동은 최후 수단이다.

인증·권한 우회는 보안 책임자 승인이 필요하다.

Timeout 증가를 최초 대응으로 사용하지 않는다.

운영 실제 고객정보를 장애 채널에 게시하지 않는다.

Heap Dump·Thread Dump는 보호된 경로에 저장한다.

원인 불명 상태에서 동일 실패 거래를 무조건 재처리하지 않는다.

# H.4 장애 대응 설계 원칙

## 원칙 1. 범위 우선

모든 사용자인가?

특정 지역인가?

특정 업무인가?

특정 ServiceId인가?

특정 Instance인가?

특정 데이터인가?

범위가 작을수록 복구조치도 작게 적용한다.

## 원칙 2. 증거 우선

최소 증거:

장애 시각

대표 GUID·TraceId

ServiceId

오류코드

Instance ID

Artifact Version

최근 변경

Thread·Pool·DB 상태

## 원칙 3. 최소 변경

한 Instance Drain

\> 한 WAR Rollback

\> 한 업무 거래통제

\> 업무그룹 재기동

\> 전체 재기동

가장 영향이 작은 복구수단을 우선한다.

## 원칙 4. 서비스 복구와 원인 해결 분리

복구
사용자 영향 제거

RCA
왜 발생했는지 확인

Remediation
다시 발생하지 않도록 수정

## 원칙 5. 데이터 상태 별도 검증

화면 정상

≠ DB 정합성 정상

Health UP

≠ 업무 거래 정상

Rollback 성공

≠ 데이터 Rollback 성공

## 원칙 6. 명확한 지휘체계

기술적으로 가장 잘 아는 사람이 반드시 전체 장애를 지휘하는 것은 아니다.

Incident Commander는 우선순위·의사결정·커뮤니케이션을 담당하고, 기술 담당자는 진단과 복구를 수행한다.

## 원칙 7. 모든 조치는 기록

누가

언제

무엇을

왜

어떤 승인을 받고

어떤 결과로 수행했는가

를 타임라인에 기록한다.

# H.5 목표 장애 대응 아키텍처

\[Alert·사용자 신고\]
│
▼
\[Incident Management\]
├─ Incident ID
├─ SEV 판정
├─ War Room
└─ 역할 배정
│
▼
\[Observability Plane\]
├─ Gateway Log
├─ TCF Transaction Log
├─ JVM·Thread
├─ Hikari·SQL
├─ External Call
└─ Audit·Security Log
│
▼
\[Control Plane\]
├─ L4·Apache Drain
├─ Gateway Route
├─ OM Transaction Control
├─ Timeout Policy
├─ Deployment·Rollback
└─ Cache·Batch Control
│
▼
\[Data Plane\]
├─ 업무 WAR
├─ DB
├─ 외부 시스템
└─ Batch·File
│
▼
\[Recovery Validation\]
├─ Liveness
├─ Readiness
├─ Deep Check
├─ Smoke
├─ Data Reconciliation
└─ Monitoring Stability

# H.6 장애 심각도 분류

## H.6.1 SEV 등급

| 등급 | 기준 | 예 | 대응 |
| --- | --- | --- | --- |
| SEV-1 | 전면 서비스 중단·보안·데이터 손상 | 전체 업무 불가, 대규모 개인정보 유출 | 즉시 War Room·최고 책임자 |
| SEV-2 | 핵심 업무 다수 영향·복구 우회 제한 | 특정 업무그룹 전체 장애 | 10분 내 War Room |
| SEV-3 | 일부 업무·사용자 영향, 우회 가능 | 특정 ServiceId 장애 | 담당팀 대응·주기보고 |
| SEV-4 | 경미한 오류·운영 불편 | Dashboard 일부 지연 | 일반 결함관리 |

## H.6.2 심각도 판정요소

영향 사용자 수

영향 업무 중요도

거래 실패율

지속시간

데이터 손실·중복 여부

보안·개인정보 영향

대체 업무 가능 여부

복구 예상시간

법규·감사 영향

## H.6.3 심각도 승격조건

영향범위 확대

복구 예상시간 초과

데이터 손상 확인

보안사고 가능성 확인

동일 장애 반복

우회수단 실패

외부기관·경영진 보고 필요

# H.7 장애 상태

| 상태 | 의미 |
| --- | --- |
| DETECTED | 장애 신호 발생 |
| ACKNOWLEDGED | 담당자가 인지 |
| INVESTIGATING | 원인범위 분석 |
| CONTAINING | 장애 확대 차단 |
| MITIGATING | 사용자 영향 완화 |
| RECOVERING | 복구 실행 |
| MONITORING | 복구 후 안정성 관찰 |
| RESOLVED | 서비스 정상 |
| CLOSED | 데이터·PIR 포함 종료 |
| REOPENED | 동일 증상 재발 |

# H.8 지휘체계와 역할

## H.8.1 필수 역할

| 역할 | 책임 |
| --- | --- |
| Incident Commander | 전체 지휘·우선순위·복구 승인 |
| Technical Lead | 기술 분석과 복구안 제시 |
| Operations Lead | 서버·Network·WAS 조치 |
| Application Lead | TCF·업무 WAR 분석 |
| DBA Lead | DB·SQL·Lock·복구 |
| Security Lead | 보안·개인정보 영향 판단 |
| Data Reconciliation Lead | 부분 반영·중복 대사 |
| Communications Lead | 사용자·업무·경영진 공지 |
| Scribe | 타임라인·결정·조치 기록 |
| Business Owner | 업무 영향·우회절차 판단 |

한 사람이 여러 역할을 수행할 수 있지만 Incident Commander와 실제 명령 수행자는 가능한 한 분리한다.

## H.8.2 War Room 기본 채널

음성·화상 회의

실시간 Chat

Incident 문서

Monitoring Dashboard

보호된 증적 저장소

결정·승인 기록

## H.8.3 War Room 규칙

한 명의 지휘자만 의사결정한다.

모든 조치는 실행 전에 선언한다.

조치 후 결과를 즉시 보고한다.

추측과 확인된 사실을 구분한다.

민감정보를 공개 채널에 게시하지 않는다.

동시에 여러 변경을 수행하지 않는다.

복구 전 증거를 먼저 확보한다.

# H.9 Incident 표준 형식

## H.9.1 Incident ID

INC-{YYYYMMDD}-{NNN}

예:

INC-20260718-003

## H.9.2 Incident Header

| 항목 | 기록 |
| --- | --- |
| Incident ID |  |
| 제목 |  |
| SEV |  |
| 상태 |  |
| 최초 발생 추정 |  |
| 탐지시각 |  |
| 대응 시작 |  |
| 서비스 복구 |  |
| 최종 종료 |  |
| Incident Commander |  |
| 영향 업무 |  |
| 영향 사용자 |  |
| 영향 지역·지점 |  |
| 대표 ServiceId |  |
| 대표 GUID |  |
| 영향 Instance |  |
| 최근 Release |  |
| 데이터 영향 |  |
| 보안 영향 |  |

## H.9.3 타임라인 형식

| 시각 | 사실·조치 | 수행자 | 결과·다음 행동 |
| --- | --- | --- | --- |
| 10:01 | p95 Alert 발생 | Monitoring | Incident 개설 |
| 10:04 | SV 목록조회 실패 확인 | OPS | GUID 확보 |
| 10:08 | AP02 Hikari Pending 증가 | AA | AP02 Drain 검토 |
| 10:12 | AP02 Traffic Drain | OPS | 오류율 감소 |

## H.9.4 사실과 가설 구분

FACT
AP02의 Hikari Pending이 80이다.

HYPOTHESIS
AP02의 Connection Leak가 원인일 수 있다.

ACTION
Thread Dump와 Pool 상태를 수집한다.

RESULT
특정 외부 호출이 Connection을 장기 보유했다.

# H.10 원본 핵심 8단계 Runbook

| 단계 | 행동 | 산출물 |
| --- | --- | --- |
| 1\. 감지 | Alert 시각·증상·최초 신고를 기록한다. | 장애 타임라인 시작 |
| 2\. 범위 | 업무·사용자·지역·거래·시간 범위를 분류한다. | 영향도 등급 |
| 3\. 대표 거래 | Correlation ID가 있는 실패 거래를 확보한다. | 재현 가능한 증적 |
| 4\. 계층 추적 | Gateway→TCF→업무→DB·외부 순서로 지연과 오류를 비교한다. | 의심 계층 |
| 5\. 복구 결정 | 우회·차단·Rollback·재기동의 위험을 비교한다. | 승인된 복구안 |
| 6\. 데이터 확인 | 부분 반영·중복·미처리 데이터를 대사한다. | 정합성 결과 |
| 7\. 종료 | 서비스·데이터·모니터링 정상 조건을 확인한다. | 종료 시각 |
| 8\. 사후 분석 | 근본 원인·탐지·대응 문제와 재발 방지를 기록한다. | PIR·개선 과제 |

# H.11 확장 장애 대응 12단계

## 단계 0. 사전 준비

장애 발생 전 다음이 준비돼야 한다.

비상연락망

War Room Template

대표 Smoke 거래

Rollback Artifact

거래통제 절차

대사 SQL

Thread·Heap 수집 절차

보안 사고 Escalation

DR 전환 절차

산출물:

운영 준비도 체크리스트

Runbook 승인본

장애훈련 결과

## 단계 1. 장애 감지와 Incident 개설

수행:

Alert·사용자 신고 확인

Incident ID 발급

최초 증상 기록

발생 추정시각과 탐지시각 분리

Incident Commander 지정

초기 SEV 판정

최초 질문:

지금도 발생 중인가?

오류인가 지연인가?

전 사용자에게 발생하는가?

최근 변경이 있었는가?

보안사고 가능성이 있는가?

금지:

“잠깐 지켜보자”며 Incident 개설 지연

장애 사실을 개인 Chat에서만 공유

근거 없이 SEV를 낮게 분류

## 단계 2. 영향범위 판정

다음 축으로 범위를 분류한다.

| 축 | 질문 |
| --- | --- |
| 사용자 | 전체·일부·특정 사용자 |
| 지역 | 전체·특정 지점·Network 구간 |
| 업무 | 전체·업무코드·도메인 |
| 거래 | ServiceId·거래코드 |
| Instance | 전체·특정 VM·Tomcat |
| 시간 | 지속·간헐·특정 시간대 |
| 데이터 | 조회만·변경·중복·손실 |
| 보안 | 인증·권한·개인정보 |

범위 축소 예:

전체 사용자는 아님

→ CT 업무만

→ CT.Reservation.selectList만

→ AP02 Instance만

→ 최근 배포 후부터

## 단계 3. 대표 거래 확보

대표 실패 거래는 다음 조건을 만족해야 한다.

정확한 실패시각

ServiceId

GUID·TraceId

사용자·지점 Masking 값

결과코드

Instance ID

응답시간

Artifact Version

가능하면 다음을 확보한다.

실패 거래 1건

정상 거래 1건

다른 Instance의 동일 거래 1건

이 세 건을 비교하면 원인 범위를 빠르게 줄일 수 있다.

## 단계 4. 최근 변경 확인

확인대상:

WAR 배포

환경설정

Gateway Route

JWT Key

OM 거래통제

Timeout 정책

DB DDL·Index

기준정보

Batch Schedule

Network·Firewall

인증서

외부 시스템 변경

장애 시작시각과 변경시각을 비교한다.

변경 직후 장애

→ Rollback 후보

변경과 무관한 장기 증가

→ 용량·Leak·데이터 증가 후보

상관관계는 원인 확정이 아니다.

## 단계 5. 계층별 추적

Client

→ Network

→ Gateway

→ Authentication

→ Tomcat·JVM

→ TCF

→ 업무

→ DB

→ 외부 연계

→ 운영 로그

각 계층의 처리시간과 오류를 비교한다.

## 단계 6. 피해 확대 통제

통제수단:

특정 Instance Traffic Drain

특정 ServiceId 거래통제

특정 채널·사용자 차단

외부 연계 Circuit Open

Batch 중지

파일 업로드 중지

Gateway Route 제한

Read-only 전환

통제는 복구가 아니다.

통제 목적:

신규 실패 거래 감소

중복·부분반영 방지

장애 계층 보호

데이터 대사 범위 고정

## 단계 7. 복구 대안 비교와 승인

후보:

Traffic Drain

설정 원복

WAR Rollback

단일 Instance 재기동

Cache Evict

외부연계 우회

DB Failover

DR 전환

모든 복구안은 다음을 평가한다.

영향범위

복구 예상시간

데이터 위험

보안 위험

Rollback 가능성

추가 증거 소실 여부

다른 업무 영향

## 단계 8. 복구 실행

원칙:

한 번에 하나의 변경

한 Instance씩 수행

실행 전 승인

실행시각 기록

실행 후 즉시 검증

실패하면 중단·원복

## 단계 9. 복구 검증

검증은 다음 순서로 수행한다.

Liveness

→ Readiness

→ Deep Health

→ 대표 조회 Smoke

→ 대표 변경 Smoke

→ 데이터 정합성

→ Monitoring 안정

## 단계 10. 데이터 대사

확인:

부분 Commit

중복 등록

미처리 요청

PROCESSING 상태 잔존

Master·History 불일치

외부 성공·내부 실패

내부 성공·응답 실패

거래로그 공백

## 단계 11. 장애 종료

종료조건:

서비스 정상

오류율 정상

p95 정상

Thread·Heap·Pool 정상

데이터 대사 완료

보안 영향 확인

사용자 공지

잔여 위험 Owner 지정

서비스 복구시각과 Incident 종료시각을 분리한다.

## 단계 12. PIR과 재발 방지

PIR에는 다음을 포함한다.

발생 원인

영향 범위

탐지 지연

대응 지연

잘된 점

잘못된 점

근본 원인

기여 요인

재발 방지

Owner·기한

ADR·Runbook 변경

# H.12 최초 5분·15분·30분 대응

## 최초 5분

Incident 개설

SEV 초안

Incident Commander 지정

대표 증상 기록

최근 변경 유무 확인

기본 Dashboard 확인

불필요한 변경 금지 선언

## 최초 15분

대표 GUID 확보

영향 업무·Instance 분류

Gateway·TCF·DB 구간 확인

증거 수집

War Room 개설

초기 업무 공지

피해 통제 필요 여부 판단

## 최초 30분

의심 계층 확정

복구 대안 비교

Traffic Drain·거래통제

Rollback·재기동 승인

데이터 영향 초안

다음 공지시각 확정

## 최초 60분

서비스 복구 또는 DR 승격

대사 범위 확정

외부기관·경영진 보고

장기 장애 운영체계 전환

교대 인력 확보

# H.13 계층별 장애 추적 지도

## H.13.1 사용자·화면

확인:

브라우저 Console

Network HTTP 상태

요청 ServiceId

응답 GUID

발생 사용자·지점

모든 화면 또는 특정 화면

주요 증상:

| 증상 | 후보 |
| --- | --- |
| 화면 자체가 열리지 않음 | Web·Apache·Network |
| 특정 버튼만 실패 | ServiceId·권한·업무 |
| 계속 Loading | Timeout·Client 처리 |
| 일부 사용자만 실패 | 권한·Session·데이터 Scope |

## H.13.2 L4·Apache

확인:

Backend Health

Traffic Distribution

SSL 인증서

Proxy Error

Connection 수

특정 Instance 편중

증상:

502
Backend 연결 실패

503
사용 가능한 Backend 없음

504
Backend 응답 Timeout

## H.13.3 Gateway

확인:

Gateway Liveness

Route 조회

Target URL

JWT 검증

Connection Pool

Downstream 응답시간

Gateway 거래로그

분류:

Gateway에 요청 없음
→ L4·Apache 이전

Gateway 요청 있음, 업무 WAR 없음
→ Route·JWT·Proxy

Gateway와 업무 WAR 모두 있음
→ 업무·DB·외부

## H.13.4 JWT·Session

확인:

Authorization Header 존재

Token 만료

issuer

audience

kid

JWKS 접근

서버 시각

Session Store

Denylist·폐기상태

특징:

모든 업무 401
→ JWT·Gateway 공통 가능성

특정 WAR만 401
→ 업무 WAR Filter·설정 가능성

특정 사용자만 401
→ Token·Session 가능성

## H.13.5 Tomcat·JVM

확인:

Process 생존

Port Listen

Tomcat Busy Thread

Thread State

CPU

Heap

GC

Metaspace

Open File

로그 Disk

대표 징후:

| 징후 | 후보 |
| --- | --- |
| CPU 100% | 무한 Loop·GC·암호화·대량 직렬화 |
| Heap 지속 증가 | Memory Leak·대형 Cache |
| Full GC 반복 | Heap 부족·Leak |
| Thread 모두 WAITING | 외부·DB 대기 |
| Thread 모두 BLOCKED | Lock·동기화 |
| 한 WAR만 지연 | 업무 Resource 독점 |

## H.13.6 TCF

확인:

STF TX\_START

Header Validation

거래통제

권한

Timeout 정책

Dispatcher Handler 등록

ETF TX\_END

결과 유형

분류:

TX\_START 없음
→ TCF 이전

TX\_START 있음, Handler 로그 없음
→ STF·Dispatcher

Handler 있음, TX\_END 없음
→ 업무·DB·외부·Timeout

TX\_END 있음
→ 응답전송·Client 처리

## H.13.7 업무 계층

확인:

Handler 분기

Facade Transaction

Service 단계

Rule 오류

DAO 호출

외부 Client 호출

처리 단계별 시간

금지:

한 줄의 “Service 시작” 로그만으로 원인 판단

## H.13.8 HikariCP

핵심 지표:

Active

Idle

Maximum

Pending

Connection Timeout

Connection 보유시간

Connection 생성 실패

판단:

| 상태 | 의미 |
| --- | --- |
| Active 낮음·Pending 0 | Pool 정상 |
| Active Maximum·Pending 증가 | Connection 고갈 |
| DB는 빠름·보유시간 김 | 애플리케이션 Connection Leak |
| SQL 지연과 함께 Active 증가 | Slow SQL |
| Connection 생성 실패 | DB·Network·계정 |

## H.13.9 DB·SQL

확인:

DB CPU

Active Session

Wait Event

Lock

Slow SQL

실행계획

Index

Connection 수

Archive·Disk

최근 DDL

SQL 원인 후보:

Index 미사용

통계정보 변화

Bind 값 분포

Lock 대기

대량 Result

Full Scan

Sort·Temp 부족

Network 전송

## H.13.10 외부 연계

확인:

Connect 시간

Read 시간

HTTP 상태

대상 오류코드

Retry 횟수

Circuit 상태

상대 시스템 장애 공지

GUID·TraceId 전달

외부 장애를 내부 DB·Thread 문제와 구분한다.

## H.13.11 Cache

확인:

Hit·Miss

TTL

Value 크기

Evict

Instance별 Version

원본 DB 상태

Cache Load 실패

특정 Instance만 결과가 다르면 Local Cache 차이를 확인한다.

## H.13.12 OM·거래로그·Batch

확인:

OM 자체 Health

Service Catalog

거래통제 상태

Timeout 정책

거래로그 적재

감사로그 적재

Batch 마지막 수집시각

Dashboard 데이터 Freshness

Dashboard 값이 오래됐을 수 있으므로 마지막 수집시각을 확인한다.

# H.14 빠른 진단 의사결정도

모든 업무 장애인가?
│
├─ 예
│ ├─ Gateway Health 실패?
│ │ ├─ 예 → Gateway·JVM·Route
│ │ └─ 아니오
│ ├─ JWT 401 급증?
│ │ ├─ 예 → JWKS·Key·Time
│ │ └─ 아니오
│ ├─ DB 공통 장애?
│ │ ├─ 예 → DBA·Failover
│ │ └─ 아니오 → L4·Tomcat 공통
│
└─ 아니오
├─ 특정 업무인가?
│ ├─ 특정 Instance만?
│ │ ├─ 예 → Drain·Artifact·JVM
│ │ └─ 아니오 → 업무 WAR·DB·외부
│
└─ 특정 ServiceId인가?
├─ 최근 배포?
│ ├─ 예 → Rollback 검토
│ └─ 아니오
├─ SQL 지연?
│ ├─ 예 → DB·Index·Lock
│ └─ 아니오
└─ 외부 연계?
├─ 예 → Timeout·Circuit
└─ 아니오 → 업무 Logic·데이터

# H.15 복구수단 비교

| 복구수단 | 적합한 상황 | 장점 | 위험 |
| --- | --- | --- | --- |
| Traffic Drain | 특정 Instance 장애 | 영향 최소 | 용량 감소 |
| 거래통제 | 특정 ServiceId 결함 | 데이터 피해 차단 | 업무 일부 중단 |
| 설정 원복 | 잘못된 정책·Route | 빠름 | 설정 Drift |
| WAR Rollback | 배포 직후 회귀 | 검증본 복구 | DB 호환성 |
| 단일 Instance 재기동 | Leak·Deadlock | 자원 초기화 | 증거 소실 |
| Cache Evict | Stale·불일치 | 데이터 갱신 | Stampede |
| Scale-out | 순수 부하 증가 | 처리량 증가 | DB 부하 증가 |
| Timeout 증가 | 일시적 정상 지연 | 실패 감소 가능 | Thread 점유 증가 |
| Gateway 우회 | Gateway 장기 장애 | 업무 일부 복구 | 중대한 보안위험 |
| DB Failover | DB 장애 | 데이터 서비스 복구 | RPO·Failback |
| DR 전환 | 센터 장애 | 업무 연속성 | 복잡도·데이터 동기 |

# H.16 복구 결정 기준

## Traffic Drain

적용:

특정 Instance만 오류

Artifact·설정 Drift

JVM Resource 이상

동일 거래가 다른 Instance에서는 정상

순서:

대체 용량 확인

→ L4·Apache Drain

→ 신규 Connection 0 확인

→ 증거 수집

→ 복구·재기동

## 거래통제

적용:

특정 변경 ServiceId에서 중복·부분반영 위험

외부 시스템 장애로 지속 실패

잘못된 업무 Logic

데이터 손상 가능성

거래통제에는 다음이 필요하다.

통제 대상

시작시각

사유

승인자

사용자 안내

해제조건

감사로그

## Rollback

우선 검토:

장애가 최근 배포 직후 시작

직전 Artifact 검증완료

DB Schema 하위호환

설정 원복 가능

Rollback 예상시간이 원인 수정시간보다 짧음

Rollback 금지·주의:

신규 DB Column을 구 WAR가 이해하지 못함

비가역 Data Migration

구 WAR가 신규 데이터 형식을 손상

직전 Artifact도 결함

Rollback 증적 없음

## 재기동

재기동 전 확보:

Thread Dump 3회

JVM Heap·GC

Process 정보

Open Connection

최근 로그

Artifact·환경변수

재기동은 다음 상황에서 사용할 수 있다.

Deadlock·Thread Pool 고착

Memory Leak로 서비스 불가

정상 Shutdown 불가

복구시간을 위해 임시 자원초기화 필요

재기동만 수행하고 RCA를 종료하지 않는다.

## Timeout 증가

다음 조건을 모두 만족할 때만 검토한다.

Downstream은 정상 처리 중

처리시간 증가 원인이 명확

Thread·Pool 여유 존재

부하시험으로 영향 확인

전체 Timeout Budget 재계산

임시 적용기간과 원복조건 존재

# H.17 시나리오 Runbook 1 — 전체 업무 502·504

## 증상

모든 업무 화면 실패

Gateway·Apache에서 502 또는 504

업무 WAR 거래로그 없음 또는 지연

## 확인순서

1\. L4·Apache Backend 상태
2\. Gateway Health
3\. Gateway Process·Port
4\. Gateway Downstream Connection
5\. 업무 Tomcat Health
6\. Network·Firewall
7\. 최근 Route·인증서 변경

## 통제

비정상 Gateway Instance Drain

정상 Gateway로 Traffic 집중

장기화 시 승인된 대체 Route 검토

## 복구

Gateway 설정 원복

직전 Gateway Artifact Rollback

Instance 순차 재기동

Network 정책 복구

## 완료조건

Gateway Health 정상

전체 업무 대표 Route 성공

401·502·504 정상 수준

업무 GUID와 Gateway GUID 연결

15분 이상 안정

# H.18 시나리오 Runbook 2 — 특정 ServiceId Timeout

## 증상

특정 ServiceId p95 급증

E-TIMEOUT 증가

다른 거래 정상

## 확인순서

1\. 대표 GUID 확보
2\. STF Timeout 정책 확인
3\. Handler 진입 확인
4\. Service 단계별 시간
5\. Hikari Pending
6\. SQL 처리시간
7\. 외부 Client 시간
8\. 동일 데이터·Instance 비교

## 판정

SQL 지연
→ DBA·SQL

외부 지연
→ tcf-eai·상대 시스템

Pool 대기
→ Connection 보유·DB

업무 CPU
→ Logic·대량 데이터

특정 Instance
→ Drain·JVM

## 통제

변경거래이고 UNKNOWN 위험이 있으면 해당 ServiceId를 우선 통제한다.

## 금지

원인 확인 없이 TCF Timeout만 증가

Client Timeout보다 서버 Timeout을 크게 설정

실패 거래를 무조건 재호출

# H.19 시나리오 Runbook 3 — 한 Tomcat Instance만 장애

## 확인

정상 Instance와 비교한다.

WAR Checksum

Git·Artifact Version

Profile

환경변수

Route

JVM Option

Cache Version

DB URL

Thread·Heap·Pool

## 조치

1\. 해당 Instance Drain
2\. 증거 수집
3\. 직전 Artifact·설정 비교
4\. 필요 시 재배포 또는 재기동
5\. Health·Smoke
6\. Traffic 10% 점진 복구
7\. 지표 확인 후 전체 복구

## 완료조건

정상 Instance와 Artifact 동일

p95·오류율 동일

Cache·Route 동일

Traffic 복구 후 안정

# H.20 시나리오 Runbook 4 — DB Pool 고갈

## 증상

Hikari Active=Maximum

Pending 지속 증가

Connection Timeout

Tomcat Thread WAITING

## 확인

DB 자체 장애 여부

Slow SQL

Lock

Connection 보유시간

Transaction 장기 실행

Connection Leak

Pool 설정 변경

업무별 사용량

## 조치 우선순위

장기 SQL·Lock 해소

→ 특정 거래 통제

→ 비정상 Instance Drain

→ 필요 시 순차 재기동

→ Pool 증가는 마지막 검토

Pool을 증가시키면 DB Session을 더 고갈시킬 수 있다.

## 데이터 확인

Connection Timeout 이전·이후 Transaction 상태를 확인한다.

# H.21 시나리오 Runbook 5 — Slow SQL·DB Lock

## 증상

특정 SQL ID 지연

DB Wait 증가

Lock 대기

UPDATE Timeout

## 확인

SQL ID

실행계획

Bind 값

Lock Holder

대상 Table

영향 행

최근 DDL·통계

배치·대량작업

## 조치

문제 거래 통제

장기 Lock Holder 확인

업무·DBA 승인 후 Session 처리

Index·통계 원복

문제 Batch 중지

SQL 수정·Rollback

DB Session 강제 종료는 Transaction Rollback과 데이터 영향을 확인한 뒤 수행한다.

# H.22 시나리오 Runbook 6 — 외부 연계 장애

## 증상

Connect Timeout

Read Timeout

502·503

상대 업무 오류 급증

## 확인

대상 시스템

호출 ServiceId

Connect·Read 시간

Retry 횟수

Circuit 상태

전체 Timeout 잔여시간

상대 장애 공지

## 통제

Circuit Open

해당 변경거래 통제

조회 Fallback

Stale 표시

Retry 제한

## 금지

외부 변경 API 자동 Retry

외부 실패를 빈 성공결과로 변환

상대 오류 원문을 사용자에게 노출

# H.23 시나리오 Runbook 7 — JWT·인증 장애

## 증상

401 급증

모든 업무 로그인 실패

Token 발급 실패

JWKS 오류

## 확인

tcf-jwt Health

JWKS Endpoint

kid

Public Key

Private Key 주입

issuer·audience

Token 만료

서버 시간

Gateway Cache

## 조치

잘못된 Key·설정 원복

구·신 Key 공존 확인

JWT Service Rollback

JWKS Cache 갱신

필요 시 사용자 재로그인 안내

## 비상 우회

Session-only 또는 Gateway 우회는 보안 책임자와 Incident Commander 승인, 적용기한, 감사로그가 필요하다.

# H.24 시나리오 Runbook 8 — ServiceId 미등록

## 증상

E-COM-DISP-0001

Handler not found

배포 후 특정 거래만 실패

## 확인

요청 ServiceId 철자

Handler serviceIds()

Spring Bean 등록

Package Scan

중복 ServiceId

OM Service Catalog

WAR 내부 Class

## 복구

오타·설정 수정

정상 Artifact Rollback

OM Catalog 정합화

재배포 후 Smoke

OM Catalog 등록만으로 실제 Handler가 생성되지는 않는다.

# H.25 시나리오 Runbook 9 — 거래로그·감사로그 장애

## 거래로그 장애

영향:

사용자 거래는 성공 가능

운영 추적 공백

장애·감사 위험

확인:

TXLOG DB

DataSource

Pool

Disk

권한

적재 실패 로그

조치:

거래는 정책에 따라 계속

즉시 Alert

DataSource 복구

공백구간 식별

보정 가능성 검토

## 감사로그 장애

중요 변경 감사가 필수라면 Fail Closed 또는 Durable Outbox 정책을 적용한다.

감사로그 장애를 무시한 채 관리자·개인정보 변경을 지속해서는 안 된다.

# H.26 시나리오 Runbook 10 — Cache 불일치

## 증상

Instance별 결과 다름

정책 변경 후 일부 서버만 반영

잘못된 공통코드

## 확인

Cache Name

Cache Version

TTL

Instance별 Entry

원본 DB 값

Evict 실행결과

## 조치

영향 거래 통제

전체 Instance Cache Evict

원본 DB 확인

점진 Traffic 복구

Stampede 모니터링

# H.27 시나리오 Runbook 11 — Batch 중복·실패

## 확인

Job Instance ID

업무일자

Lock

Scheduler 중복

read·write·skip·rollback Count

Checkpoint

부분 처리 데이터

## 조치

중복 Scheduler 중지

Job Lock 확인

부분 처리대사

재시작 가능 Step 확인

동일 업무일자 재처리 승인

Batch 재실행은 멱등성과 처리대상 범위를 확인한 후 수행한다.

# H.28 시나리오 Runbook 12 — 배포 직후 장애

## 확인

배포 시각과 장애 시각

변경 WAR

설정

DB Migration

Gateway Route

OM Seed

Artifact Checksum

## 기본 판단

배포 전 정상

배포 후 즉시 장애

직전 Artifact 정상

→ Rollback 우선

## Rollback 검증

Liveness

Readiness

Deep Health

대표 조회

대표 변경

데이터 대사

오류율·p95

동일 장애 Artifact를 수정 없이 재배포하지 않는다.

# H.29 시나리오 Runbook 13 — UNKNOWN·부분 반영

## 대표 상황

DB Commit 성공

→ 응답 전송 실패

→ 사용자 Timeout

또는:

Master 저장 성공

→ History 저장 실패

→ Transaction 정책 오류

## 즉시 조치

해당 변경 ServiceId 통제

대사 대상 시간범위 고정

GUID·Idempotency Key 확보

신규 재처리 중지

## 대사

거래로그

Master

History

Idempotency

외부 대상 결과

감사로그

## 결과분류

| 상태 | 판정 |
| --- | --- |
| Master·History 존재 | SUCCESS |
| 모두 없음 | FAIL |
| 일부만 존재 | PARTIAL·데이터 장애 |
| 외부만 성공 | 보상 필요 |
| 판단 불가 | UNKNOWN 유지 |

## 재처리

조회로 결과 확정

→ 기존 성공결과 반환

또는

→ 보상·복구

→ 승인 후 재처리

# H.30 시나리오 Runbook 14 — 보안사고

## 대상

JWT Private Key 유출

Token 탈취

관리자 계정 오남용

개인정보 로그 노출

Gateway 우회

무권한 DB 접근

## 우선순위

증거 보존

접근 차단

Key·Token 폐기

영향범위 확인

보안 책임자 Escalation

법적·감사 보고

## 금지

일반 장애처럼 로그 삭제

침해 시스템 무조건 재기동

증거 파일 수정

Incident Chat에 Token·개인정보 게시

## Key 유출 예

1\. 발급 중지
2\. Private Key 폐기
3\. 신규 Key 생성
4\. JWKS 구·신 Key 전략 결정
5\. 영향 Token 폐기
6\. Gateway·WAR Key 갱신
7\. 전체 인증 Test
8\. 사고 영향 조사

# H.31 시나리오 Runbook 15 — 센터·DR 전환

## 전환조건

센터 전체 Network 불가

핵심 인프라 장기 장애

Primary DB 복구시간 RTO 초과 예상

Incident Commander·인프라 책임자 승인

## 전환순서

1\. DR 선언
2\. Primary Traffic 차단
3\. DR DB 상태·RPO 확인
4\. DB Promote
5\. Gateway·JWT·OM 기동
6\. 업무 WAR 기동
7\. Route·Secret·Certificate 확인
8\. Liveness·Readiness·Deep
9\. 전체 업무 Smoke
10\. 데이터 정합성
11\. Traffic 점진 개방
12\. 집중 모니터링

Failback은 별도 변경으로 취급한다.

# H.32 데이터 대사 기준

## H.32.1 대사 대상

변경거래

Timeout 거래

Retry 거래

배포 중 처리 거래

장애시간 Batch

외부 연계 거래

파일 업·다운로드

## H.32.2 대사 기준정보

| 정보 | 확인 |
| --- | --- |
| Incident 시간범위 | 시작·통제·복구 |
| ServiceId | 영향 거래 |
| GUID | 개별 거래 |
| Idempotency Key | 반복 요청 |
| Master | 최종 업무상태 |
| History | 변경이력 |
| 거래로그 | 시작·종료 결과 |
| 감사로그 | 수행자·행위 |
| 외부 시스템 | 상대 처리결과 |

## H.32.3 정합성 유형

| 유형 | 예 | 대응 |
| --- | --- | --- |
| 정상 성공 | Master+History | 완료 |
| 정상 실패 | 데이터 없음 | 완료 |
| 중복 | Master 다건 | 중복 해소 |
| 부분 반영 | Master만 존재 | 보상·복구 |
| 미처리 | 요청 존재·데이터 없음 | 재처리 검토 |
| UNKNOWN | 결과 불명 | 결과조회·대사 |
| 로그 공백 | 데이터 정상·TXLOG 없음 | 운영 증적 보정 |

## H.32.4 데이터 수정 원칙

대상목록 확정

업무 Owner 승인

DBA 검토

Backup

수정 Script

Dry Run

2인 확인

수행

대사

감사기록

운영 DB에서 즉석 SQL을 작성해 수정하지 않는다.

# H.33 복구 검증 6단계

## 1\. Liveness

Process와 JVM이 살아 있는가?

## 2\. Readiness

DB·필수 Dependency에 연결 가능한가?

## 3\. Deep Health

Gateway Route·DB Query·Cache·JWT·외부 핵심기능이 정상인가?

## 4\. Smoke

대표 ServiceId가 정상 처리되는가?

## 5\. Data Verification

Master·History·로그가 일치하는가?

## 6\. Monitoring Stability

오류율·p95·Thread·Heap·Pool이 일정시간 정상인가?

Health가 UP이더라도 Smoke가 실패하면 복구 완료가 아니다.

# H.34 장애 종료 기준

## 서비스

대표 업무 성공

오류율 Baseline

p95 목표

Traffic 정상

## 자원

Tomcat Busy Thread 기준 이내

Heap 기준 이내

GC 정상

Hikari Pending 지속 0

DB Wait 정상

## 데이터

부분 반영 대사

중복 대사

UNKNOWN 확정

필요한 보정 완료

## 보안

Token·Key 조치

권한 우회 제거

개인정보 영향 확인

감사증적 보존

## 운영

Alert 해제

Dashboard 정상

업무 공지

잔여위험 등록

Owner 지정

# H.35 장애 커뮤니케이션

## H.35.1 최초 공지

\[장애 발생 안내\]

Incident ID: INC-20260718-003
발생 시각: 2026-07-18 10:01
영향: 상담예약 조회 일부 지연
현재 상태: 원인 분석 및 영향범위 확인 중
사용자 조치: 반복 등록 요청을 중단해 주십시오.
다음 안내: 10:20

## H.35.2 진행 공지

\[장애 진행 상황\]

현재 CT 상담예약 조회 거래에서 DB Connection 대기가 확인됐습니다.
영향 Instance를 Traffic에서 제외했고 오류율은 감소 중입니다.
데이터 변경거래의 부분 반영 여부를 확인하고 있습니다.
다음 안내는 10:40입니다.

## H.35.3 복구 공지

\[서비스 복구 안내\]

상담예약 서비스가 10:32 복구됐습니다.
대표 조회·등록·수정 거래와 데이터 정합성 검증을 완료했습니다.
현재 집중 모니터링 중이며 근본 원인과 재발 방지 대책은 PIR로 공유하겠습니다.

## H.35.4 공지 원칙

확인된 사실만 전달한다.

원인 미확정이면 “분석 중”이라고 표현한다.

복구 예상시간을 근거 없이 단정하지 않는다.

개인정보·내부 URL·보안정보를 포함하지 않는다.

다음 공지시각을 반드시 제시한다.

# H.36 장애 타임라인 예시

| 시각 | 내용 |
| --- | --- |
| 10:01 | CT.Reservation.selectList p95 8초 Alert |
| 10:03 | INC-20260718-003 개설, SEV-2 |
| 10:05 | 대표 GUID G-... 확보 |
| 10:08 | AP02 Hikari Active 160·Pending 42 확인 |
| 10:10 | AP01 동일 거래 정상 |
| 10:12 | AP02 Traffic Drain 승인·실행 |
| 10:15 | 전체 오류율 12%→2% 감소 |
| 10:18 | AP02 Thread Dump 수집 |
| 10:22 | 외부 고객조회 Read Timeout Thread 확인 |
| 10:25 | CT 등록 거래 일시 통제 |
| 10:28 | 외부 연계 Circuit Open |
| 10:32 | 서비스 정상화 |
| 10:40 | UNKNOWN 거래 12건 대사 시작 |
| 11:05 | 데이터 정합성 완료 |
| 11:20 | Incident RESOLVED |
| 익일 | PIR 수행 |

# H.37 정상 처리 흐름

Alert 감지

→ Incident 개설

→ 영향범위 분류

→ 대표 GUID 확보

→ 계층별 비교

→ 특정 Instance 원인 확인

→ Traffic Drain

→ 증거 수집

→ 직전 Artifact Rollback

→ Health 4단계

→ 대표 거래 Smoke

→ 데이터 대사

→ 모니터링 안정

→ 복구 공지

→ PIR

# H.38 오류·Timeout·장애 흐름

## 잘못된 복구조치 실패

원인 불명

→ 전체 Tomcat 재기동

→ 일시 복구

→ Thread Dump 없음

→ 같은 장애 재발

→ RCA 불가

개선:

Drain

→ 증거 수집

→ Instance 단위 복구

→ 재발 시 비교 가능

## Rollback 실패

구 WAR 배포

→ 신규 DB Schema와 비호환

→ 기동 실패

대응:

Traffic 차단 유지

→ Roll-forward Artifact 적용

→ DB 호환성 대사

→ 배포 설계 개선

## 대사 실패

장애 시간범위 불명확

→ 중복·미처리 대상 식별 불가

대응:

TX\_START·Gateway Log·DB Updated DTM으로 범위 재구성

# H.39 정상 예시

장애
AP02에서 CT 조회 지연

감지
p95 Alert

범위
CT 업무
AP02
selectList

대표 거래
GUID 확보

진단
Hikari Pending 증가
외부 조회 Thread 대기

통제
AP02 Traffic Drain
CT 등록 일시 통제

증거
Thread Dump 3회
Pool 상태
외부 호출시간

복구
외부 Circuit Open
AP02 재기동

검증
Liveness
Readiness
Deep
조회·등록 Smoke

데이터
UNKNOWN 12건 대사
중복·부분 반영 없음

종료
지표 30분 안정

PIR
외부 Timeout Budget와 Bulkhead 개선

# H.40 금지 예시

Incident ID 없이 개인적으로 대응한다.

영향범위를 확인하지 않고 전체 장애라고 공지한다.

대표 GUID 없이 로그를 무작정 검색한다.

장애 증거 수집 전에 서버를 재기동한다.

한 Instance 문제로 전체 Tomcat을 재기동한다.

DB 장애인데 WAR를 반복 재배포한다.

Timeout 발생 시 설정값부터 증가한다.

Hikari Pool을 무조건 두 배로 늘린다.

실패 거래를 사용자에게 반복 실행하게 한다.

변경거래 Timeout을 자동 Retry한다.

Gateway 인증을 승인 없이 해제한다.

업무 WAR를 외부에 직접 노출한다.

보안사고 로그를 삭제한다.

운영 DB를 즉석 SQL로 수정한다.

Rollback 전 DB 호환성을 확인하지 않는다.

Health UP만 보고 복구 완료로 판단한다.

서비스 복구 후 데이터 대사를 생략한다.

결함이 남았는데 장애를 CLOSED로 처리한다.

확인되지 않은 원인을 확정해 공지한다.

PIR에서 개인의 실수만 원인으로 기록한다.

# H.41 연계 규칙

## Runbook 연결대상

| 대상 | 연결 |
| --- | --- |
| Service Catalog | 업무 Owner·ServiceId |
| 거래통제 | 장애 시 차단 |
| Timeout Policy | 실제 적용값 |
| 배포이력 | 최근 Release |
| Artifact Repository | Rollback WAR |
| Gateway Route | 대상 Instance |
| 거래로그 | GUID 추적 |
| 감사로그 | 운영조치 |
| Monitoring | Alert·Dashboard |
| CMDB | VM·Tomcat·WAR |
| RTM | 장애 Test |
| ADR | 설계 판단 |
| PIR | 재발 방지 |

## Incident와 변경관리

장애 중 수행한 임시 설정은 장애 종료 후 정식 변경관리로 전환한다.

임시 Timeout 변경

임시 Route

임시 거래통제

임시 Scale-out

임시 보안예외

기한 없이 남겨두지 않는다.

# H.42 데이터 및 상태관리

## Incident Registry

| 속성 | 설명 |
| --- | --- |
| Incident ID | 고유번호 |
| SEV | 심각도 |
| Status | 대응상태 |
| Affected Services | 영향 ServiceId |
| Start·Detect·Recover | 주요시각 |
| Owner | 책임자 |
| Root Cause | 근본 원인 |
| Data Impact | 데이터 영향 |
| Security Impact | 보안 영향 |
| Recovery Action | 복구조치 |
| PIR ID | 사후 분석 |
| Action Items | 개선과제 |

## 거래통제 상태

ACTIVE

DISABLED

EMERGENCY\_BLOCKED

RECOVERY\_TEST

RESTORED

통제 상태 변경은 감사로그와 Incident ID를 포함한다.

## 대사 상태

IDENTIFIED

ANALYZING

CONFIRMED\_SUCCESS

CONFIRMED\_FAIL

PARTIAL

COMPENSATED

REPROCESSED

CLOSED

# H.43 성능·용량·확장성

## 장애 시 용량 감소

Instance Drain 시 남은 시스템이 부하를 감당할 수 있어야 한다.

예:

정상 4대

1대 Drain

→ 잔여 3대가 피크 부하를 감당할 수 있는가?

N+1 용량을 고려한다.

## Scale-out 판단

Scale-out이 적합한 경우:

오류 없이 CPU·Thread 포화

DB 여유

부하 증가가 원인

Application 결함 없음

부적합한 경우:

Memory Leak

Slow SQL

DB Connection 고갈

외부 장애

Deadlock

결함을 Scale-out하면 장애 규모만 커질 수 있다.

## 관측 부하

장애 중 다음을 과도하게 수행하지 않는다.

Heap Dump 반복

전체 로그 Download

DB Full Scan 대사

모든 Thread Debug Log

초단위 대규모 Metric 수집

# H.44 보안·개인정보·감사

## 증거 보호

Thread Dump

Heap Dump

Application Log

DB Export

Token 정보

사용자 요청

에는 개인정보와 인증정보가 포함될 수 있다.

관리:

보호된 저장소

접근권한

암호화

보존기간

파기

접근감사

## 운영조치 감사

감사대상:

거래통제

Timeout 변경

Gateway Route 변경

Cache Evict

Batch 강제실행

WAR Rollback

DB 데이터 보정

Token·Key 폐기

관리자 권한 변경

## 보안 우선원칙

보안사고에서는 서비스 복구보다 침해 차단과 증거 보존이 우선될 수 있다.

# H.45 운영·모니터링·장애 대응 지표

## 장애관리 지표

MTTD

MTTA

MTTR

재발률

SEV별 장애건수

Rollback 성공률

UNKNOWN 거래 수

데이터 보정 건수

PIR 과제 완료율

## 기술 지표

거래 오류율

p95·p99

Tomcat Busy Thread

JVM CPU·Heap·GC

Hikari Active·Pending

DB Wait·Lock

외부 Timeout

Gateway 4xx·5xx

거래로그 적재 실패

## 지표 해석

단일 지표로 판단하지 않는다.

CPU 낮음

\+ Thread 대기 높음

\+ Hikari Pending 높음

→ DB 대기 가능성

CPU 높음

\+ Full GC 증가

\+ Heap 회수 안 됨

→ Memory Leak 가능성

# H.46 자동검증 및 품질 Gate

## 운영전환 전 Gate

비상연락망 존재

Incident 역할 정의

대표 Smoke 목록

Rollback Artifact

대사 SQL

SEV 기준

War Room Template

PIR Template

장애훈련 완료

## 배포 자동검증

Artifact Checksum

Health

Representative ServiceId

Gateway Route

OM Catalog

Timeout 정책

권한

Rollback Artifact

## 관측성 Gate

GUID 전파

TX\_START·TX\_END

Instance ID

Artifact Version

SQL ID

External Target

Error Type

## 장애 대응 자동화

Alert에서 Incident 자동생성

ServiceId Owner 조회

최근 배포 자동연결

대표 GUID 자동추출

Instance·Artifact 비교

Runbook 링크 제공

자동조치는 안전범위 안에서만 적용한다.

자동 재기동

자동 Failover

자동 거래차단

은 오동작 위험과 승인정책을 반드시 검토한다.

# H.47 장애훈련 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| DRL-001 | 특정 WAR Process 종료 | Drain·복구 |
| DRL-002 | Gateway Route 오등록 | 404 탐지·원복 |
| DRL-003 | JWT Key 불일치 | 401·Key 복구 |
| DRL-004 | Hikari Pool 고갈 | 거래통제·원인확인 |
| DRL-005 | Slow SQL | SQL ID·DBA 대응 |
| DRL-006 | DB Lock | Lock Holder 식별 |
| DRL-007 | 외부 Read Timeout | Circuit·표준 오류 |
| DRL-008 | ServiceId 미등록 | Handler·Catalog 확인 |
| DRL-009 | Cache 불일치 | 전체 Evict |
| DRL-010 | TXLOG DB 장애 | 거래 지속·Alert |
| DRL-011 | 감사로그 장애 | Fail 정책 |
| DRL-012 | Batch 중복 실행 | Lock·대사 |
| DRL-013 | 배포 후 Smoke 실패 | Rollback |
| DRL-014 | Master·History 부분 반영 | Transaction·보정 |
| DRL-015 | 응답 유실 UNKNOWN | 결과조회 |
| DRL-016 | 개인정보 로그 노출 | 보안사고 대응 |
| DRL-017 | Instance 1대 Drain | 잔여용량 확인 |
| DRL-018 | Tomcat OOM | Dump·순차 복구 |
| DRL-019 | DB Failover | RTO·정합성 |
| DRL-020 | DR 센터 전환 | 전체 Smoke·Failback |

# H.48 장애훈련 결과서

| 항목 | 기록 |
| --- | --- |
| 훈련 ID |  |
| 시나리오 |  |
| 대상 환경 |  |
| 시작·종료 |  |
| 참가자·역할 |  |
| 주입 방법 |  |
| 탐지시간 |  |
| 대응 시작시간 |  |
| 복구시간 |  |
| RTO 충족 |  |
| 데이터 결과 |  |
| 잘된 점 |  |
| 미흡한 점 |  |
| Runbook 변경 |  |
| 개선과제 |  |

# H.49 책임 경계와 RACI

| 활동 | IC | OPS | AA·FW | 업무 | DBA | 보안 | DevOps | QA | PMO |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Incident 개설 | A | R | C | I | I | I | I | I | C |
| SEV 판정 | A/R | C | C | C | C | C | I | I | C |
| 영향범위 | A | R | R | C | C | C | C | C | I |
| 증거수집 | C | R | R | C | R | C | C | I | I |
| 거래통제 | A | R | C | A/C | I | C | I | I | I |
| Traffic Drain | A | R | C | I | I | I | C | I | I |
| WAR Rollback | A | C | C | C | I | I | R | C | I |
| DB Failover | A | C | C | I | R | C | C | I | I |
| 보안 차단 | C | C | C | I | C | A/R | C | I | I |
| 데이터 대사 | A | C | R | A/C | R | C | I | C | I |
| 사용자 공지 | A | C | C | A/R | I | C | I | I | R/C |
| 종료 승인 | A | C | C | C | C | C | C | C | I |
| PIR | A | R/C | R/C | C | C | C | C | C | R/C |
| 개선과제 | A | R | R | R | R | R | R | C | C |

# H.50 완료 체크리스트

## 사전 준비

| 점검 | 완료 |
| --- | --- |
| 비상연락망이 최신이다. | □ |
| Incident Commander 기준이 있다. | □ |
| SEV 기준이 승인됐다. | □ |
| War Room Template이 있다. | □ |
| 대표 Smoke 거래가 있다. | □ |
| 직전 Artifact가 보존된다. | □ |
| 거래통제 권한이 있다. | □ |
| 대사 SQL이 준비됐다. | □ |
| DR 훈련을 수행했다. | □ |

## 장애 초기

| 점검 | 완료 |
| --- | --- |
| Incident ID를 생성했다. | □ |
| 발생·탐지시각을 기록했다. | □ |
| SEV를 판정했다. | □ |
| 영향범위를 분류했다. | □ |
| 대표 GUID를 확보했다. | □ |
| 최근 변경을 확인했다. | □ |
| 역할을 배정했다. | □ |
| 최초 공지를 했다. | □ |

## 진단

| 점검 | 완료 |
| --- | --- |
| 정상 거래와 실패 거래를 비교했다. | □ |
| 정상 Instance와 장애 Instance를 비교했다. | □ |
| Gateway·TCF·DB 구간을 비교했다. | □ |
| Thread·Heap·Pool을 확인했다. | □ |
| SQL·Lock을 확인했다. | □ |
| 외부 연계를 확인했다. | □ |
| 확인된 사실과 가설을 구분했다. | □ |

## 통제·복구

| 점검 | 완료 |
| --- | --- |
| 피해 확대를 통제했다. | □ |
| 복구 대안을 비교했다. | □ |
| 승인 후 조치했다. | □ |
| 한 번에 하나씩 변경했다. | □ |
| 증거 수집 후 재기동했다. | □ |
| Rollback 호환성을 확인했다. | □ |
| 조치결과를 기록했다. | □ |

## 복구 검증

| 점검 | 완료 |
| --- | --- |
| Liveness가 정상이다. | □ |
| Readiness가 정상이다. | □ |
| Deep Health가 정상이다. | □ |
| 조회 Smoke가 정상이다. | □ |
| 변경 Smoke가 정상이다. | □ |
| 오류율·p95가 정상이다. | □ |
| Thread·Heap·Pool이 안정적이다. | □ |

## 데이터·보안

| 점검 | 완료 |
| --- | --- |
| UNKNOWN을 확인했다. | □ |
| 중복 거래를 확인했다. | □ |
| 부분 반영을 확인했다. | □ |
| Master·History를 대사했다. | □ |
| 외부 결과를 대사했다. | □ |
| 개인정보 영향을 확인했다. | □ |
| 운영조치 감사로그가 있다. | □ |

## 종료·PIR

| 점검 | 완료 |
| --- | --- |
| 서비스 복구시각을 기록했다. | □ |
| Incident 종료시각을 기록했다. | □ |
| 사용자 공지를 완료했다. | □ |
| 잔여위험에 Owner가 있다. | □ |
| PIR 일정을 확정했다. | □ |
| 근본 원인을 기록했다. | □ |
| 개선과제와 기한이 있다. | □ |
| Runbook·ADR을 갱신했다. | □ |

# H.51 PIR 표준

## PIR 구성

Incident Summary

업무 영향

시간대별 Timeline

기술적 Root Cause

기여 요인

탐지 평가

대응 평가

복구 평가

데이터·보안 영향

잘된 점

개선할 점

Action Item

ADR·Runbook 변경

## Root Cause 작성원칙

좋지 않은 예:

담당자의 실수

개발자의 확인 부족

서버 문제

좋은 예:

Gateway Route 변경을 운영에 반영할 때
사전 Contract Test와 자동 Rollback Gate가 없었고,

단일 Instance에 잘못된 Target URL이 반영됐으나
Instance별 Route Drift Alert가 없어
장애가 사용자 신고 후 탐지됐다.

## 5 Why 예

왜 거래가 실패했는가?
Gateway가 잘못된 URL을 호출했다.

왜 잘못된 URL이 적용됐는가?
운영 Route 값이 수동 입력됐다.

왜 수동 오류가 배포됐는가?
자동 검증이 없었다.

왜 자동 검증이 없었는가?
Route가 CI/CD 관리대상에 포함되지 않았다.

왜 포함되지 않았는가?
Gateway Route의 Source of Truth가 정의되지 않았다.

# H.52 변경·호환성·폐기 관리

## Runbook 변경조건

신규 Module 추가

새 ServiceId 도입

Gateway Route 변경

DB 구조 변경

JWT·Session 정책 변경

Monitoring 도구 변경

장애 발생

장애훈련 결과

조직·연락망 변경

## Version 관리

Runbook Version

작성일

승인일

Owner

변경사유

관련 Incident

적용 Release

## 구 Runbook 폐기

신규 절차 승인

→ 운영자 교육

→ 장애훈련

→ 구 문서 Deprecated

→ 호출 0 확인

→ Archive

구 Runbook은 장애 이력 확인을 위해 삭제하지 않고 Archive한다.

## 명령 호환성

Runbook의 명령은 다음 변경에 영향을 받는다.

JDK

Tomcat

Linux Distribution

DB Version

경로

Module명

Log Pattern

Monitoring 도구

정기적으로 실제 실행 가능 여부를 확인한다.

# 시사점

## 핵심 아키텍처 판단

첫째, 장애 대응은 원인분석 활동이 아니라 사용자·데이터·보안 영향을 통제하고 서비스를 복구하는 운영 의사결정 과정이다.

둘째, 장애 증상보다 업무·ServiceId·Instance·시간 범위를 먼저 확인해야 불필요한 전체 재기동과 장애 확산을 막을 수 있다.

셋째, 대표 GUID가 없으면 Gateway·TCF·업무·DB·외부 로그를 하나의 거래로 연결할 수 없다.

넷째, Traffic Drain과 거래통제는 장애 확대를 막는 가장 중요한 초기 수단이며 복구와 별도로 관리해야 한다.

다섯째, 재기동은 자원을 초기화하는 임시 복구수단일 뿐 근본 원인 해결이 아니다.

여섯째, Timeout은 실제 처리결과가 불명확한 UNKNOWN 거래를 만들 수 있으므로 서비스 복구 후 반드시 데이터 대사를 수행해야 한다.

일곱째, Rollback은 WAR만 되돌리는 작업이 아니라 설정·Gateway Route·DB Schema·Cache·OM 기준정보의 호환성을 함께 검증하는 작업이다.

여덟째, Health Check가 성공해도 대표 ServiceId와 데이터 정합성이 실패하면 서비스는 복구된 것이 아니다.

아홉째, OM·거래로그·Batch 관측계층 자체가 장애일 수 있으므로 Dashboard의 최신 시각과 원천 시스템을 함께 확인해야 한다.

열째, 장애 대응 역량은 문서 존재가 아니라 실제 장애훈련에서 RTO·데이터 정합성·역할 수행을 검증함으로써 확보된다.

## 주요 위험

| 위험 | 결과 |
| --- | --- |
| Incident 개설 지연 | 대응·보고 지연 |
| 범위 미분류 | 과도한 복구조치 |
| GUID 미확보 | 원인 추적 실패 |
| 증거 전 재기동 | RCA 불가 |
| 전체 재기동 | 장애 확대 |
| Timeout 무조건 증가 | Thread·Pool 고갈 |
| Pool 무조건 증가 | DB Session 고갈 |
| 변경거래 Retry | 중복 처리 |
| Rollback 호환성 미확인 | 2차 장애 |
| Health만 확인 | 업무 장애 잔존 |
| 데이터 대사 누락 | 부분 반영 방치 |
| 보안 우회 | 침해 위험 |
| 개인정보 증적 공유 | 2차 보안사고 |
| Owner 없는 개선과제 | 재발 |
| Runbook 미훈련 | 실제 대응 실패 |

## 우선 보완 과제

1.  전 업무 ServiceId Owner·연락망을 OM Service Catalog와 연결한다.
2.  Alert 발생 시 최근 배포·Instance·대표 GUID를 자동 제공한다.
3.  Gateway·TCF·외부 호출의 TraceId 전파를 완성한다.
4.  업무별 대표 조회·변경 Smoke Script를 구축한다.
5.  변경거래 UNKNOWN 대사 Query와 상태조회 기능을 제공한다.
6.  거래통제·Traffic Drain·Rollback 승인절차를 표준화한다.
7.  Thread·JVM·Hikari·Slow SQL 통합 Dashboard를 구축한다.
8.  Instance별 Artifact·Profile·Cache Drift 검사를 자동화한다.
9.  감사로그·거래로그 장애정책과 Alert를 확정한다.
10.  Gateway 우회와 Session-only 비상모드의 보안통제를 정의한다.
11.  배포 전 Rollback과 데이터 호환성 Test를 필수 Gate로 적용한다.
12.  SEV-1·SEV-2 장애훈련을 정기 수행한다.
13.  PIR Action Item의 완료율을 Architecture Review에서 관리한다.
14.  장애 Runbook을 Incident·ADR·RTM과 연결한다.
15.  자동 복구는 충분한 안전조건이 검증된 범위부터 단계적으로 적용한다.

# 마무리말

장애 대응을 완료했다고 판단하려면 다음 질문에 답할 수 있어야 한다.

장애가 언제 시작됐는가?

누가 언제 탐지했는가?

어떤 사용자와 업무가 영향을 받았는가?

특정 ServiceId인가 전체 업무인가?

특정 Instance인가 전체 시스템인가?

대표 GUID와 TraceId가 있는가?

최근 배포·설정 변경이 있었는가?

Gateway·TCF·업무·DB·외부 중 어느 계층인가?

서비스 복구 전에 어떤 증거를 확보했는가?

피해 확대를 어떻게 통제했는가?

왜 해당 복구안을 선택했는가?

Rollback·재기동의 위험을 검토했는가?

Liveness뿐 아니라 Smoke까지 성공했는가?

부분 반영·중복·UNKNOWN 거래를 대사했는가?

보안·개인정보 영향이 없는가?

복구 후 지표가 안정적으로 유지되는가?

사용자와 업무에 정상복구를 공지했는가?

근본 원인과 기여 요인을 확인했는가?

재발 방지 과제에 Owner와 완료일이 있는가?

Runbook과 ADR이 최신화됐는가?

부록 H의 핵심 흐름은 다음과 같다.

감지

→ Incident 개설

→ 범위·SEV 판정

→ 대표 거래 확보

→ 계층별 추적

→ 피해 통제

→ 복구 결정

→ 복구 실행

→ Health·Smoke 검증

→ 데이터 대사

→ 안정화·종료

→ PIR·재발 방지

가장 중요한 원칙은 다음과 같다.

장애 대응은
서버를 다시 켜는 작업이 아니다.

무엇이 영향을 받고 있는지 확인하고,
더 큰 피해가 발생하지 않도록 통제하며,

서비스와 데이터를
안전하고 검증 가능한 상태로 되돌리는 과정이다.

장애 중 수행한 모든 판단과 조치가 기록되고,

복구 후 데이터 정합성과
재발 방지까지 확인될 때

비로소 하나의 Incident가
완전히 종료됐다고 말할 수 있다.
