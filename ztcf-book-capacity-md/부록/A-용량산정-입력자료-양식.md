# 부록 A. 용량산정 입력자료 양식

> 원본: `znsight-capacity-word` · 23장 수준 템플릿 확장

## NSIGHT 1차 표준 전제

| 항목 | 기준값 |
|------|--------|
| 지점 수 | 3,600 |
| 지점당 사용자 | 6 |
| 전체 사용자 | 21,600 |
| 설계 세션 | 26,000~28,000 |
| TPS | 360 / 720 / 1,080 |
| 목표 응답 | p95 3초 이하 |
| 기준 VM | 8 vCPU / 32GB |
| VM당 TPS | 250 (보수) |
| AP 구조 | 2센터 Active-Active |

> 출처: `znsight-capacity-word` · [13단계 요약](../zNSIGHT-용량산정-전체-흐름.md)


## 원문 기반 본문

NSIGHT 마케팅플랫폼사용자 수 기반 용량산정 연계 가이드사용자 수 -> 동시 요청 수 -> TPS -> AP 수량 -> DB Pool 총량 -> DB Session 총량 -> 장애 시 잔여 처리량농협상호금융 NSIGHT 정보계 / IaaS VM 기반 산정 기준구분기준기준 사용자3,600개 지점 x 지점당 6명 = 21,600명설계 세션26,000~28,000 세션(20~30% 여유율 포함)목표 응답시간일반 온라인 p95 3초 이하기준 TPS360 TPS 기본 / 720 TPS 피크 / 1,080 TPS 스트레스기준 VMIaa

S VM 8 vCPU / 32GBAP 처리 기준일반 산정 250 TPS/VM, 업무 특성별 보정 가능운영 구조2센터 AP Active-Active, DB Active-Standby 전제본 문서는 용량산정 수치가 서로 끊기지 않도록 사용자 수에서 장애 시 잔여 처리량까지 하나의 산정 흐름으로 연결하는 가이드이다. 최종 확정값은 Single View 선도개발과 성능테스트 결과를 반영하여 보정한다.

## 1. 도입 전 안내말용량산정은 서버 대수만 계산하는 작업이 아니다. 사용자 수, 세션 수, 실제 동시 요청률, 목표 응답시간, AP 처리량, DB Connection Pool, DB Session 총량, 장애 시 잔여 처리량이 하나의 체인으로 연결되어야 한다.

특히 정보계 온라인 AP는 계정계와 달리 RDW 조회, 권한 확인, 마스킹, GUID 로그, 전문 조립, 일부 연계 호출이 함께 수행된다. 따라서 전체 사용자 수를 곧바로 TPS로 환산하면 과대 산정되거나, 반대로 DB Session 한계를 놓칠 수 있다.

산정 단계핵심 질문주요 산출값

## 1. 사용자 수누가 사용할 것인가?전체 사용자, 설계 세션

## 2. 동시 요청 수동시에 몇 명이 실제 요청을 보낼 것인가?동시 요청자

## 3. TPS목표 응답시간 내 몇 건을 처리해야 하는가?기준 TPS, 피크 TPS, 스트레스 TPS4. AP 수량몇 대의 AP가 필요한가?최소/권장/센터 장애 감당 AP 수

## 5. DB Pool 총량AP 전체가 DB에 열 수 있는 최대 연결 수는 얼마인가?Hikari Pool 총량

## 6. DB Session 총량DB가 실제 감당해야 할 전체 세션 수는 얼마인가?RDW/ADW별 총 Session7. 장애 시 잔여 처리량AP 또는 센터 장애 후에도 목표 TPS를 감당하는가?잔여 TPS, 잔여 Pool, 여유율

## 2. 공통 산정 전제

본 가이드는 NSIGHT 마케팅플랫폼의 1차 표준 기준선을 다음과 같이 둔다. 프로젝트 환경이나 선도개발 실측값이 변경되면 같은 산식으로 재계산한다.

항목기준값설명지점 수3,600개상호금융 기준 지점 수지점당 사용자6명지점 업무 사용자 기준전체 사용자21,600명3,600 x 6설계 세션26,000~28,000전체 사용자에 20~30% 여유율 적용일반 온라인 응답목표p95 3초 이하성능 목표 및 TPS 산정 기준기준 VM8 vCPU / 32GBIaaS VM Scale-Out 기본 단위VM당 처리량250 TPS/VMCore당 30~40 TPS 보수 기준AP 운영 방식2센터 Active-Active평상시 분산 처리, 센터 장애 시 잔여 센터 처리DB 운영 방식Active-Standby정합성 및 복구 안정성 우선

## 3. 전체 산정 흐름[1] 전체 사용자 수 = 지점 수 x 지점당 사용자[2] 설계 세션 수 = 전체 사용자 수 x (1 + 세션 여유율)[3] 동시 요청자 수 = 전체 사용자 수 x 동시 요청률[4] 목표 TPS = 동시 요청자 수 / 목표 응답시간[5] A

P 수량 = ceil(목표 TPS / VM당 처리 TPS)[6] DB Pool 총량 = AP 수량 x AP당 Hikari maximumPoolSize[7] DB Session 총량 = DB Pool 총량 + 배치/BI/운영/관

리 Session + 여유율[8] 장애 시 잔여 처리량 = 잔여 AP 수 x VM당 처리 TPS주의할 점은 세션 수와 TPS는 같은 값이 아니라는 점이다. 세션 수는 로그인 상태 유지 규모이고, TPS와 AP 수량은 실제 요청을 동시에 보내는 사용자 수를 기준으로 산정한다.

4. 1단계: 사용자 수 및 설계 세션 산정사용자 수 산정은 가장 먼저 확정해야 하는 기준값이다. 이 값이 흔들리면 세션 용량, 동시 요청자 수, TPS, AP 수량, DB Session 총량이 모두 흔들린다.


| 산정 항목 | 산식 | 계산값 | 설명 |
|-----------|------|--------|------|
| 전체 사용자 | 3,600개 지점 x 6명 | 21,600명 | 전체 등록 또는 사용 가능 사용자 |
| 최대 로그인 세션 | 전체 사용자 100% | 21,600 세션 | 모든 사용자가 로그인한 상태 |
| 20% 여유율 세션 | 21,600 x 1.2 | 25,920 세션 | 운영 여유 반영 |
| 30% 여유율 세션 | 21,600 x 1.3 | 28,080 세션 | 보수적 설계 기준 |
| 설계 세션 기준 | — | 약 26,000~28,000 | 권장 기준 |

설계 세션은 Tomcat Session, Spring Session, L4 Sticky Timeout, WebTopSuite Center 유지 정책과 함께 검토해야 한다. 세션이 길어질수록 TPS가 직접 증가하는 것은 아니지만 Active Session 수와 Heap, 세션 복제 부담은 증가한다.5. 2단계: 동시 요청자 수 산정동시 요청자는 전체 사용자 중 같은 순간 실제로 거래 요청을 보내는 사용자 수이다. 정보계는 전체 사용자가 동시에 버튼을 누르는 구조가 아니므로 동시 요청률 시나리오를 분리한다.


| 시나리오 | 전체 사용자 | 동시 요청률 | 동시 요청자 | 의미 |
|----------|-------------|-------------|-------------|------|
| 낮은 부하 | 21,600명 | 3% | 648명 | 평상시 또는 낮은 업무 집중도 |
| 기본 운영 | 21,600명 | 5% | 1,080명 | 일반 피크 기준 |
| 피크 설계 | 21,600명 | 10% | 2,160명 | 업무 집중 및 캠페인 집중 기준 |
| 스트레스 | 21,600명 | 15% | 3,240명 | 한계 검증 및 성능시험 기준 |

동시 요청률은 업무팀·현업 사용 패턴·피크 시간대·캠페인 이벤트·BI 조회 집중 시간대를 고려하여 보정한다. 선도개발 이후에는 APM과 성능테스트 결과를 기준으로 보정한다.6. 3단계: TPS 산정TPS는 동시 요청자를 목표 응답시간으로 나누어 산정한다. 일반 온라인 트랜잭션 목표 응답시간을 3초로 둘 경우 다음과 같다.

TPS = 동시 요청자 수 / 목표 응답시간(초)

| 시나리오 | 동시 요청자 | 목표 응답시간 | 산정 TPS | 적용 기준 |
|----------|-------------|---------------|----------|-----------|
| 낮은 부하 | 648명 | 3초 | 216 TPS | 평상시 참고 |
| 기본 운영 | 1,080명 | 3초 | 360 TPS | 기본 운영 기준 |
| 피크 설계 | 2,160명 | 3초 | 720 TPS | 설계 대표 기준 |
| 스트레스 | 3,240명 | 3초 | 1,080 TPS | 한계 검증 기준 |

3초는 모든 거래의 평균 시간이 아니라 p95 목표 응답시간으로 관리하는 것이 적절하다. 실제 평균 응답시간은 1.0~1.5초 수준으로 관리되어야 AP 1대당 250 TPS 기준이 현실성을 가진다.7. 4단계: AP 수량 산정AP 수량은 목표 TPS를 VM당 처리 기준으로 나누어 산정한다. NSIGHT 1차 기준은 8 vCPU / 32GB VM당 250 TPS이다. 단, 일반 마케팅 AP는 외부 연계 대기시간이 많으면 150~200 TPS/VM으로 낮춰 잡고, Single View AP는 RDW SQL 성능이 확보될 때 250 TPS/VM을 적용한다.

필요 AP 수 = ceil(목표 TPS / VM당 처리 TPS)구분보수 기준일반 기준Single View 기준적용 판단VM당 처리 TPS150 TPS200 TPS250 TPS업무 특성별 선택주요 병목연계 대기/로그/마스킹일반 조회/처

리RDW SQL/DB Pool선도개발 실측으로 보정권장 사용처CruzAPIM 동기호출 많은 업무일반 마케팅 AP고빈도 Single View 조회혼합 업무는 낮은 값 적용

### 7.1 AP 수량 산정표 - 250 TPS/VM 기준시나리오목표 TPS단순 최소 APA-A 최소 배치센터 장애 감당 AP장애 후 잔여 TPS권장안기본 운영360 TPS2대4대(센터당 2)4대(센터당 2)500 TPS4대 / 최소 센터 장애 감당피크 설계720 TPS3대4대(센터당 2)6대(센터당 3)750 TPS8대 / 권장: 센터당 4대(N+1 여유)스트레스1080 TPS5대6대(센터당 3)10대(센터당 5)1250 TPS12대 / 권장: 센터당 6대(N+1 여유)A-A 최소 배치는 평상시 양 센터 분산 처리를 위한 최소 수량이다. 센터 장애 감당 AP는 1개 센터가 전체 목표 TPS를 단독 처리할 수 있도록 산정한 수량이다. 운영 안정성을 위해 720 TPS 이상에서는 N+1 여유를 권장한다.8. 5단계: DB Pool 총량 산정DB Pool 총량은 AP 전체가 DB에 동시에 열 수 있는 최대 Connection 수이다. 이 값은 AP 성능만 보고 크게 잡으면 DB Session 폭증을 유발하고, 너무 작게 잡으면 Hikari Connection Timeout과 AP Thread 대기를 유발한다.

DB Pool 총량 = AP 수량 x AP당 Hikari maximumPoolSizeAP 유형권장 maximumPoolSize권장 minimumIdle적용 DB비고일반 마케팅 AP40~5010~15RDW / 업무DB연계 대기시간이 많으면

40부터 시작Single View AP50~6015~20RDWRDW 고빈도 조회 기준실시간 처리 AP20~405~10업무DB / 로그DB비동기·이벤트 중심EBM 엔진 AP30~5010~15RDW / 업무DBRule 판단과 조회량에 따라 보정Batch/ETL별도 산정별도 산정RDW / ADW온라인 AP Pool과 분리Hikari connectionTimeout은 SQL 실행시간이 아니라 Connection Pool에서 DB Connection을 빌리기 위해 기다리는 시간이다. SQL 실행시간은 MyBatis statement timeout 또는 JDBC query timeout으로 별도 통제한다.

### 8.1 DB Pool 산정 예시 - 일반 AP Pool 50 기준시나리오목표 TPS센터 장애 감당 APAP당 Pool전체 Pool 총량센터 장애 후 잔여 Pool기본 운영360 TPS4대(센터당 2)50200100피크 설계720 TPS6대(센터당 3)50300150스트레스1080 TPS10대(센터당 5)5050025

### 08.2 DB Pool 산정 예시 - Single View AP Pool 60 기준시나리오목표 TPS센터 장애 감당 APAP당 Pool전체 Pool 총량센터 장애 후 잔여 Pool기본 운영360 TPS4대(센터당 2)60240120피크 설계720 TPS6대(센터당 3)60360180스트레스1080 TPS10대(센터당 5)606003009. 6단계: DB Session 총량 산정DB Session 총량은 온라인 AP의 Hikari Pool만 의미하지 않는다. RDW와 ADW는 역할이 다르므로 각각 별도로 산정해야 하며, 온라인 AP, 배치, BI, ETL, 운영자, DBA, 모니터링 세션을 모두 포함해야 한다.

DB Session 총량 = Σ(AP 수 x AP당 Pool x DataSource 수) + Batch Session + BI Session + ETL Session + 운영/관리 Session + 여유율구분RDW 포함 여부ADW 포함 여부산

정 방식주의점마케팅 AP포함예외적 포함AP 수 x RDW PoolADW 직접 분석 조회 금지Single View AP포함미포함 원칙AP 수 x RDW Pool고빈도 조회로 별도 관리BI Portal미포함 원칙포함BI 동시조회 x Query 특성ADW 전용 원칙ETL/DataStage포함포함Job 병렬도 x DB 세션온라인 시간대 충돌 방지Kafka/CDC 처리일부 포함일부 포함Consumer/Writer 병렬도 기준Lag와 재처리 고려운영/관리/모니터링포함포함고정 여유 세션DBA, APM, Health Check 포함

### 9.1 RDW Session 총량 예시아래는 피크 설계 720 TPS 기준으로 Single View AP를 센터당 3대, 총 6대로 구성하고 AP당 RDW Pool 60을 적용한 예시이다.

항목산식Session 수설명Single View AP RDW Pool6대 x 60360센터 장애 감당 최소 구성일반 마케팅 AP RDW Pool4대 x 50200일반 업무 별도 AP 가정운영/모니터링 Session고정값30Health Check, APM, 운영 조회DBA/관리 Session고정값20운영 관리용온라인 RDW 소계360 + 200 + 30 + 20610배치/ETL 제외여유율 20%610 x 20%122피크 변동성 반영권장 RDW Session 확보610 + 122732온라인 기준 최소 권장이 예시는 산정 방식 설명용이다. 실제 최종값은 업무 서버 분리 수량, AP별 DataSource 수, 배치/ETL 병렬도, BI 접속 방식, DBMS max sessions 정책을 반영하여 확정해야 한다.10. 7단계: 장애 시 잔여 처리량 산정장애 시 잔여 처리량은 AP 1대 장애와 센터 장애를 분리해서 계산한다. AP Active-Active 구조에서는 평상시 처리량보다 장애 후 단일 센터 또는 잔여 노드 처리량이 더 중요하다.

AP 1대 장애 후 잔여 TPS = (전체 AP 수 - 1) x VM당 처리 TPS센터 장애 후 잔여 TPS = 잔여 센터 AP 수 x VM당 처리 TPS센터 장애 후 잔여 DB Pool = 잔여 센터 AP 수 x AP당 Pool시나리오센터별 AP전

체 AP목표 TPS센터 장애 후 AP잔여 TPS판정360 TPS2대/센터4대3602대500 TPS충족720 TPS 최소3대/센터6대7203대750 TPS충족이나 여유 작음720 TPS 권장4대/센터8대7204대1,000 TPS충분1,080 TPS 최소5대/센터10대1,0805대1,250 TPS충족1,080 TPS 권장6대/센터12대1,0806대1,500 TPS충분

### 10.1 장애 시 DB Pool 잔여량 예시시나리오센터별 APAP당 Pool 50 잔여AP당 Pool 60 잔여검토 포인트360 TPS2대100120기본 업무에는 충분하나 SQL 지연 시 모니터링 필요720 TPS 최소3대150180DB 평균 SQL 시간이 길면 Pool Wait 가능720 TPS 권장4대200240센터 장애 후에도 여유 확보1,080 TPS 최소5대250300DB max sessions와 I/O 병목 동시 검증 필요1,080 TPS 권장6대300360스트레스 및 장애전환 검증 기준

## 11. DB Connection 요구량 산정 보정식AP당 Pool은 단순히 TPS와 같은 값으로 잡지 않는다. 실제로 DB Connection이 점유되는 시간, 요청당 SQL 수, 운영 여유율을 기준으로 산정한다.

필요 DB Connection 수 = TPS x 평균 DB Connection 점유시간(초) x 요청당 SQL 수 x 안전계수업무 유형TPS/VMDB 점유시간요청당 SQL 수안전계수계산값권장 Pool일반 마케팅 AP2000.10초1.21.53640~50

Single View AP2500.12초1.21.55450~60연계 대기 많은 AP1500.08초1.01.51825~40복합 조회 AP2000.15초1.51.56860~80 또는 SQL 튜닝 필요계산값이 권장 Pool을 크게 초과하면 Pool을 무조건 늘리기보다 SQL 튜닝, 조회 분리, 캐시, 비동기화, ADW 전환, 화면 조회 조건 제한을 먼저 검토해야 한다.

## 12. 최종 승인용 산정표 템플릿최종 승인 문서에는 아래 표처럼 모든 값이 하나의 행으로 연결되어야 한다. 이 표가 있어야 PMO, 인프라, DBA, 업무팀이 같은 숫자를 기준으로 판단할 수 있다.

구분전체 사용자동시 요청률동시 요청자TPSVM당 TPS센터별 AP총 APAP당 PoolDB Pool 총량장애 후 TPS판정기본 운영21,6005%1,0803602502450/60200/240500충족피크 최소21,60010%2,1607202503650/60300/360750조건부 충족피크 권장21,60010%2,1607202504850/60400/4801,000권장스트레스 최소21,60015%3,2401,08025051050/60500/6001,250충족스트레스 권장21,60015%3,2401,08025061250/60600/7201,500권장표의 DB Pool 총량은 AP당 Pool 50 또는 60을 적용한 온라인 AP 기준 예시이다. RDW/ADW별 실제 DB Session 총량은 배치, ETL, BI, 운영 세션을 별도로 더해야 한다.

## 13. 운영 검증 체크리스트검증 항목검증 질문합격 기준사용자 기준지점 수와 지점당 사용자 수가 업무팀과 합의되었는가?전체 사용자 21,600명 기준 승인세션 기준세션 수와 동시 요청자 수를 분리했는가?설계 세션 26,000~28,000 별도 관리TPS 기준360/720/1,080 TPS가 산식으로 연결되는가?동시 요청률과 3초 응답시간 근거 명시AP 수량센터 장애 시 잔여 센터가 목표 TPS를 처리하는가?잔여 TPS >= 목표 TPSDB PoolAP 수 증가에 따른 Pool 총량이 계산되었는가?AP 수 x Pool 수 명시DB SessionRDW/ADW별 전체 Session 총량을 계산했는가?온라인+배치+BI+운영+여유율 포함TimeoutDB Query Timeout < Transaction Timeout < Client Timeout 순서인가?계층 정합성 확보장애 검증AP 1대 장애, 센터 장애, DB 지연을 시험했는가?성능/장애 테스트 결과 확보운영 감시DB Pool Wait, Active Connection, CPU, GC, SQL Time을 감시하는가?APM/DB 모니터링 지표 등록

## 14. 마무리말

이 가이드의 핵심은 용량산정을 평균값이나 서버 대수 중심으로 끝내지 않는 것이다. 사용자 수에서 시작한 값은 반드시 동시 요청자, TPS, AP 수량, DB Pool, DB Session, 장애 시 잔여 처리량까지 연결되어야 한다.

NSIGHT 마케팅플랫폼은 RDW 기반 Single View, WebTopSuite 단말, AP Active-Active, DB Active-Standby, FAST/DEEP 데이터 흐름 분리라는 특성을 가진다. 따라서 최종 용량산정은 운영 가능한 안정성과 장애 격리를 기준으로 검증되어야 한다.

## 15.

### 시사점 요약

| 시사점 | 내용 |
|--------|------|
| 세션과 TPS는 다르다 | 세션은 로그인 유지 규모이고 TPS는 실제 동시 요청자 기준이다.

AP 수량보다 잔여 처리량이 중요하다평상시 대수보다 센터 장애 후 남은 센터가 목표 TPS를 감당하는지가 핵심이다.

DB Pool은 크게 잡는 것이 능사가 아니다Pool 확대는 DB Session 폭증을 만들 수 있으므로 SQL 시간과 DB 점유시간 기준으로 산정해야 한다.

DB Session 총량은 DB별로 산정해야 한다RDW와 ADW는 역할이 다르므로 온라인, 배치, BI, ETL 세션을 분리 계산해야 한다.

최종값은 선도개발로 보정해야 한다Single View 대표거래 성능테스트로 TPS/Thread/Pool/GC/SQL Time을 실측해야 한다.

> **용도**: CAP-010 산정 입력 · **연관 본문**: 8, 45장

## 용량산정 입력자료 양식 — 실무 템플릿

본 부록은 **용량산정 입력** 영역의 표준 템플릿입니다. 상단 **NSIGHT 1차 표준 전제**와 함께 사용합니다.

### CAP-010 · ENV-002 필드 1:1 매핑 (코드 기준)

> 화면: `/oc/capacity.html` (CAP-010~050) · `/oc/env-002.html` (ENV-002)  
> API: `POST /api/oc/capacity/calculate` · `POST /api/oc/env/analyze`

#### CAP-010 (CapacityCalculationCDTO)

| 부록 A 필드 | DTO (CapacityCalculationCDTO) | UI id (capacity.html) | 화면 라벨 | 타입 | 기본값 |
|-------------|------------------------------|------------------------|----------|------|--------|
| projectName | projectName | `projectName` | 프로젝트명 | string | 6,000지점 표준 |
| branchCount | branchCount | `branchCount` | 지점 수 | int | 6000 | · 부록 A·ENV 기본 3600 — 화면 기본값 상이
| userPerBranch | userPerBranch | `userPerBranch` | 지점당 사용자 | int | 6 |
| totalUsers | resolvedTotalUsers() | `totalUsers` | 전체 사용자 | int (readonly) | branchCount×userPerBranch |
| designedSessions | (산출) | `designedSessions` | 설계 세션 수 | int (readonly) | totalUsers×(1+sessionMarginRate) |
| sessionMarginRate | sessionMarginRate | `sessionMarginRate` | 세션 여유율 | double | 0.30 |
| sessionTimeoutMin | sessionTimeoutMin | `sessionTimeout` | 세션 타임아웃 | int | 60 |
| concurrentRequestRates | concurrentRequestRates | `rate` | 동시요청률 | List<Double> | 0.03,0.05,0.10,0.15 | · UI는 % 정수, API는 소수
| targetResponseTimes | targetResponseTimes | `timeout` | 목표 응답(초) | List<Integer> | 3,4,5 |
| vmSpecCode | vmSpecCode | `vmSpec` | VM 사양 | string | 8C64G |
| tpsPerCore | tpsPerCore | `tpsPerCore` | Core당 TPS | int | 35 | · TPMC 가이드 행 클릭 시 연동
| tpmcPerTps | tpmcPerTps | `tpmcPerTps` | 1 TPS당 TPMC | int | 3000 |
| avgThreadHoldSec | avgThreadHoldSec | `avgThreadHoldSec` | 평균 Thread 점유(초) | double | 1.2 |
| threadMarginRate | threadMarginRate | `threadMarginRate` | Thread 여유율 | double | 1.2 |
| maxThreadMarginRate | maxThreadMarginRate | `maxThreadMarginRate` | maxThreads 배율 | double | 1.3 |
| apType | apType | `apType` | AP 유형 | enum | GENERAL |
| avgDbConnectionHoldSec | avgDbConnectionHoldSec | `avgDbConnectionHoldSec` | DB Connection 점유(초) | double | 0.15/0.20 | · AP 유형별 기본
| dbTransactionUsageRatio | dbTransactionUsageRatio | `dbTransactionUsageRatio` | DB 사용 거래 비율 | double | 1.0 |
| poolSafetyFactor | poolSafetyFactor | `poolSafetyFactor` | Pool 안전계수 | double | 1.3 |
| threadDbUsageRatio | threadDbUsageRatio | `threadDbUsageRatio` | Thread→DB 사용 비율 | double | 0.30 |
| minPoolPerVm | minPoolPerVm | `minPoolPerVm` | 최소 Pool/VM | int | 30 |
| activeActive | activeActive | `activeActive` | 2센터 Active-Active | boolean | true |
| drValidation | drValidation | `drValidation` | DR·잔여 TPS | boolean | true |
| validateDbPool | validateDbPool | `validateDbPool` | DB Session 한도 검증 | boolean | true |
| dbSessionLimit | dbSessionLimit | `dbSessionLimit` | DB Session 한도 | int | 500 |
| calculationStep | calculationStep | `(내부)` | 산정 단계 | string | ALL |

#### ENV-002 (CapacityPlannerRequest)

| 부록 A 필드 | DTO (CapacityPlannerRequest) | UI id (env-002.html) | 화면 라벨 | 타입 | 기본값 |
|-------------|-------------------------------|----------------------|----------|------|--------|
| scenarioName | scenarioName | `(자동)` | 시나리오명 | string | ENV-002 산정 |
| branchCount | branchCount | `capBranchCount` | 지점 수 | int | 3600 |
| usersPerBranch | usersPerBranch | `capUsersPerBranch` | 지점당 사용자 | int | 6 | · CAP DTO는 userPerBranch(단수)
| totalUsers | totalUsers | `capTotalUsers` | 전체 사용자 | int | 21600 |
| vmProfileId | vmProfileId | `capVm` | VM 프로파일 | string | 8CORE-32GB |
| customVm | customVm | `capVm=CUSTOM` | 커스텀 VM | boolean | false |
| customCore | customCore | `capCustomCore` | 커스텀 Core | int | 8 |
| customMemoryGb | customMemoryGb | `capCustomMemory` | 커스텀 메모리(GB) | int | 64 |
| tpsPerCoreMin | tpsPerCoreMin | `capTpsPerCoreMin` | Core TPS Min | int | 30 |
| tpsPerCoreBase | tpsPerCoreBase | `capTpsPerCoreBase` | Core TPS Base | int | 35 |
| tpsPerCoreMax | tpsPerCoreMax | `capTpsPerCoreMax` | Core TPS Max | int | 40 |
| tpmcPerTps | tpmcPerTps | `capTpmcPerTps` | 1 TPS당 TPMC | int | 3000 |
| manualCoreTps | manualCoreTps | `capManualCoreTps` | Core TPS 수동 | boolean | false |
| actualRequestPercents | actualRequestPercents | `capPercent` | 동시요청률 | List<Integer> | 3,5,10,15 |
| responseTimeoutSeconds | responseTimeoutSeconds | `capTimeout` | 목표 응답(초) | List<Integer> | 3,4,5 |
| sessionIdleMinutes | sessionIdleMinutes | `capSession` | 세션 Idle(분) | List<Integer> | 60 |
| activeActive | activeActive | `capActiveActive` | Active-Active | boolean | true |
| drValidation | drValidation | `capDrValidation` | DR 검증 | boolean | true |
| validateDbPool | validateDbPool | `capValidateDbPool` | DB Pool 검증 | boolean | true |
| includeSettingExamples | includeSettingExamples | `capIncludeExamples` | 설정 예시 포함 | boolean | true |
| hikariPoolPerVm | hikariPoolPerVm | `(산출 입력)` | Hikari Pool/VM | int | 0 | · ENV-003/004 Grid 연동
| dbSessionLimit | dbSessionLimit | `(baseline)` | DB Session 한도 | int | 500 |

#### 부록 A 통합 대조

| 부록 A | CAP-010 DTO | CAP UI | ENV-002 DTO | ENV UI | 비고 |
|--------|-------------|--------|-------------|--------|------|
| branchCount | branchCount | branchCount | branchCount | capBranchCount | CAP 기본 6000, ENV 3600 |
| userPerBranch | userPerBranch | userPerBranch | — | — | 단수 userPerBranch vs 복수 usersPerBranch |
| totalUsers | resolvedTotalUsers() | totalUsers | totalUsers | capTotalUsers | DTO명: resolvedTotalUsers() / totalUsers |
| sessionMarginRate | sessionMarginRate | sessionMarginRate | — | — | — |
| sessionTimeoutMin | sessionTimeoutMin | sessionTimeout | — | — | — |
| concurrentRequestRates | concurrentRequestRates | rate | — | — | — |
| targetResponseTimes | targetResponseTimes | timeout | — | — | — |
| vmSpecCode | vmSpecCode | vmSpec | — | — | — |
| tpsPerCore | tpsPerCore | tpsPerCore | — | — | — |
| tpmcPerTps | tpmcPerTps | tpmcPerTps | tpmcPerTps | capTpmcPerTps | — |
| avgThreadHoldSec | avgThreadHoldSec | avgThreadHoldSec | — | — | — |
| activeActive | activeActive | activeActive | activeActive | capActiveActive | — |
| drValidation | drValidation | drValidation | drValidation | capDrValidation | — |
| validateDbPool | validateDbPool | validateDbPool | validateDbPool | capValidateDbPool | — |
| dbSessionLimit | dbSessionLimit | dbSessionLimit | dbSessionLimit | (baseline) | — |
| avgDbConnectionHoldSec | avgDbConnectionHoldSec | avgDbConnectionHoldSec | — | — | — |
| poolSafetyFactor | poolSafetyFactor | poolSafetyFactor | — | — | — |
| minPoolPerVm | minPoolPerVm | minPoolPerVm | — | — | — |
| projectName | projectName | projectName | — | — | — |
| designedSessions | (산출) | designedSessions | — | — | — |
| threadMarginRate | threadMarginRate | threadMarginRate | — | — | — |
| maxThreadMarginRate | maxThreadMarginRate | maxThreadMarginRate | — | — | — |
| apType | apType | apType | — | — | — |
| dbTransactionUsageRatio | dbTransactionUsageRatio | dbTransactionUsageRatio | — | — | — |
| threadDbUsageRatio | threadDbUsageRatio | threadDbUsageRatio | — | — | — |
| calculationStep | calculationStep | (내부) | — | — | — |
| scenarioName | — | — | scenarioName | (자동) | — |
| usersPerBranch | — | — | usersPerBranch | capUsersPerBranch | 단수 userPerBranch vs 복수 usersPerBranch |
| vmProfileId | — | — | vmProfileId | capVm | — |
| customVm | — | — | customVm | capVm=CUSTOM | — |
| customCore | — | — | customCore | capCustomCore | — |
| customMemoryGb | — | — | customMemoryGb | capCustomMemory | — |
| tpsPerCoreMin | — | — | tpsPerCoreMin | capTpsPerCoreMin | — |
| tpsPerCoreBase | — | — | tpsPerCoreBase | capTpsPerCoreBase | — |
| tpsPerCoreMax | — | — | tpsPerCoreMax | capTpsPerCoreMax | — |
| manualCoreTps | — | — | manualCoreTps | capManualCoreTps | — |
| actualRequestPercents | — | — | actualRequestPercents | capPercent | — |
| responseTimeoutSeconds | — | — | responseTimeoutSeconds | capTimeout | — |
| sessionIdleMinutes | — | — | sessionIdleMinutes | capSession | — |
| includeSettingExamples | — | — | includeSettingExamples | capIncludeExamples | — |
| hikariPoolPerVm | — | — | hikariPoolPerVm | (산출 입력) | — |

#### 명칭 차이 (작성 시 주의)

| 부록 A 관례 | CAP DTO | ENV DTO | UI |
|-------------|---------|---------|-----|
| userPerBranch | userPerBranch | usersPerBranch | capUsersPerBranch |
| concurrentRate | concurrentRequestRates (0.03…) | actualRequestPercents (3…) | rate / capPercent |
| targetResponseSec | targetResponseTimes | responseTimeoutSeconds | timeout / capTimeout |
| vmSpec | vmSpecCode (8C64G) | vmProfileId (8CORE-32GB) | vmSpec / capVm |
| sessionTimeoutMin | sessionTimeoutMin | sessionIdleMinutes | sessionTimeout / capSession |

#### 산출 필드 (부록 A 산출 항목 ↔ API 응답)

| 부록 A 산출 | CapacityCalculationDDTO | 화면 위치 |
|-------------|-------------------------|----------|
| totalUsers | totalUsers | #totalUsers (readonly) |
| designedSessions | designedSessions | #designedSessions |
| vmTpsAtBase | vmTpsAtBase | VM 카드 TPS 배너 |
| profilePoolCap | profilePoolCap | CAP-050 결과 |


### 산출 항목

| 산출 | 산식 |
|------|------|
| totalUsers | branchCount × userPerBranch |
| designedSessions | totalUsers × (1 + sessionMarginRate) |
| 동시요청자 | totalUsers × concurrentRequestRates |
| TPS | ⌈동시요청자 ÷ targetResponseTimes⌉ |
| vmTpsAtBase | vmCores × tpsPerCore |
| AP 대수 | CAP-030 (A-A·DR 반영) |
| Pool/VM | CAP-050 max(30, min(②,③)) |
| DB Session | Σ(AP×Pool) + 배치 + 20% |

### 적용 절차

| 단계 | 작업 | 담당 |
|------|------|------|
| 1 | 권고값 초안 작성 | 아키텍처·WAS |
| 2 | DEV 환경 적용·단위 검증 | 개발 |
| 3 | STG 360/720 TPS 시험 | 성능시험 |
| 4 | 확정값 PRD 반영 | 운영·TA |
| 5 | 변경관리 이력 등록(부록 AB) | 운영 |

### 환경별 설정 차이

| 항목 | DEV | STG | PRD |
|------|-----|-----|-----|
| 수치 | 완화 가능 | 권고값 | **확정값** |
| leakDetection | 60s | 60s | 선택 |
| Actuator | 전체 | metrics+health | 제한 노출 |
| 로그 레벨 | DEBUG | INFO | INFO/WARN |

### 체크리스트

| # | 확인 |
|---|------|
| 1 | NSIGHT 1차 표준(21,600명·720 TPS) 전제 반영 |
| 2 | Timeout 계층 정합 (M 부록) |
| 3 | Pool 합산 ≤ DB max (V 부록) |
| 4 | 360/720 TPS 시험 합격 (X·Z 부록) |
| 5 | ENV rule-check 통과 |

### 트러블슈팅

| 증상 | 점검 | 조치 |
|------|------|------|
| p95 급증 | Thread·Pool·SQL | GUID Trace |
| Pool Pending | SQL p95 vs Pool 크기 | SQL 튜닝 우선 |
| Timeout 다발 | 계층 역전 여부 | M 부록 대조 |
| 센터 장애 | 잔여 TPS | W 부록 |

## 산정 공식 참조

```
동시요청자 = 전체사용자 × 동시요청률
TPS = 동시요청자 ÷ 3
AP = ⌈TPS ÷ 250⌉ (A-A)
Pool = max(30, min(TPS×0.15×1.3, Thread×30%))
```

## 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70% |
| 720 TPS | Thread/Pool 고갈 없음 |
| 1,080 TPS | Fail-fast 정상 |
| 센터 장애 | 잔여 TPS≥720 |

## CAP/ENV 연동

- 용량산정: `/oc/capacity.html` · `/api/oc/capacity/*`
- 환경설정: `/oc/env-002.html` · `/api/oc/env/rule-check`

## 연관 본문

| 본문 챕터 | 내용 |
|----------|------|
| 8, 45 | 용량산정 입력 상세 |

### 연관 부록

| 부록 | 내용 |
|------|------|
| A~B | 산정 입력·TPS |
| G~L | 솔루션 템플릿 |
| M | Timeout 매트릭스 |
| V~W | DB·센터 장애 |
| X~Z | 시험·검증 |
| AA~AB | 전환·변경 |

### 720 TPS 실무 예시

| 항목 | 산출 | 설정 연결 |
|------|------|----------|
| 사용자 | 21,600 | — |
| 동시요청(10%) | 2,160 | — |
| TPS | 720 | — |
| AP | 8 (A-A) | 8C/32G VM |
| Thread | 400~500 | maxThreads |
| Pool/VM | 50 | HikariCP |
| DB Session | 400 | max sessions |
| 잔여(센터 Down) | 1,000 | W 부록 |

### 작성·승인

| 역할 | 담당 | 산출물 |
|------|------|--------|
| PMO·업무 | 입력값 합의 | A 부록 |
| 아키텍처 | 산정·권고값 | 본 부록 |
| 성능시험 | 실측·확정값 | Z 부록 |
| 운영·TA | PRD 반영 | AB 부록 |


## 절별 상세

### A.1 입력 항목 정의

본 절은 **부록 A** — **입력 항목 정의** (용량산정 입력자료 양식) NSIGHT 1차 표준 적용 기준입니다.

| 부록 A | CAP-010 DTO | CAP UI | ENV-002 DTO | ENV UI | 비고 |
|--------|-------------|--------|-------------|--------|------|
| branchCount | branchCount | branchCount | branchCount | capBranchCount | CAP 기본 6000, ENV 3600 |
| userPerBranch | userPerBranch | userPerBranch | — | — | 단수 userPerBranch vs 복수 usersPerBranch |
| totalUsers | resolvedTotalUsers() | totalUsers | totalUsers | capTotalUsers | DTO명: resolvedTotalUsers() / totalUsers |
| sessionMarginRate | sessionMarginRate | sessionMarginRate | — | — | — |
| sessionTimeoutMin | sessionTimeoutMin | sessionTimeout | — | — | — |
| concurrentRequestRates | concurrentRequestRates | rate | — | — | — |
| targetResponseTimes | targetResponseTimes | timeout | — | — | — |
| vmSpecCode | vmSpecCode | vmSpec | — | — | — |
| tpsPerCore | tpsPerCore | tpsPerCore | — | — | — |
| tpmcPerTps | tpmcPerTps | tpmcPerTps | tpmcPerTps | capTpmcPerTps | — |
| avgThreadHoldSec | avgThreadHoldSec | avgThreadHoldSec | — | — | — |
| activeActive | activeActive | activeActive | activeActive | capActiveActive | — |
| drValidation | drValidation | drValidation | drValidation | capDrValidation | — |
| validateDbPool | validateDbPool | validateDbPool | validateDbPool | capValidateDbPool | — |
| dbSessionLimit | dbSessionLimit | dbSessionLimit | dbSessionLimit | (baseline) | — |
| avgDbConnectionHoldSec | avgDbConnectionHoldSec | avgDbConnectionHoldSec | — | — | — |
| poolSafetyFactor | poolSafetyFactor | poolSafetyFactor | — | — | — |
| minPoolPerVm | minPoolPerVm | minPoolPerVm | — | — | — |
| projectName | projectName | projectName | — | — | — |
| designedSessions | (산출) | designedSessions | — | — | — |
| threadMarginRate | threadMarginRate | threadMarginRate | — | — | — |
| maxThreadMarginRate | maxThreadMarginRate | maxThreadMarginRate | — | — | — |
| apType | apType | apType | — | — | — |
| dbTransactionUsageRatio | dbTransactionUsageRatio | dbTransactionUsageRatio | — | — | — |
| threadDbUsageRatio | threadDbUsageRatio | threadDbUsageRatio | — | — | — |
| calculationStep | calculationStep | (내부) | — | — | — |
| scenarioName | — | — | scenarioName | (자동) | — |
| usersPerBranch | — | — | usersPerBranch | capUsersPerBranch | 단수 userPerBranch vs 복수 usersPerBranch |
| vmProfileId | — | — | vmProfileId | capVm | — |
| customVm | — | — | customVm | capVm=CUSTOM | — |
| customCore | — | — | customCore | capCustomCore | — |
| customMemoryGb | — | — | customMemoryGb | capCustomMemory | — |
| tpsPerCoreMin | — | — | tpsPerCoreMin | capTpsPerCoreMin | — |
| tpsPerCoreBase | — | — | tpsPerCoreBase | capTpsPerCoreBase | — |
| tpsPerCoreMax | — | — | tpsPerCoreMax | capTpsPerCoreMax | — |
| manualCoreTps | — | — | manualCoreTps | capManualCoreTps | — |
| actualRequestPercents | — | — | actualRequestPercents | capPercent | — |
| responseTimeoutSeconds | — | — | responseTimeoutSeconds | capTimeout | — |
| sessionIdleMinutes | — | — | sessionIdleMinutes | capSession | — |
| includeSettingExamples | — | — | includeSettingExamples | capIncludeExamples | — |
| hikariPoolPerVm | — | — | hikariPoolPerVm | (산출 입력) | — |

핵심 필수: branchCount, userPerBranch, concurrentRequestRates, targetResponseTimes, vmSpecCode/vmProfileId.

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70% |
| 720 TPS | Thread/Pool 고갈 없음 |
| 1,080 TPS | Fail-fast 정상 |
| 센터 장애 | 잔여 TPS≥720 |

#### 주의사항

- 권고값은 출발점 — 선도개발·시험 후 확정
- 세션≠TPS, Pool≠Thread

### A.2 산출 항목

본 절은 **부록 A** — **산출 항목** (용량산정 입력자료 양식) NSIGHT 1차 표준 적용 기준입니다.

| 산출 | DTO 필드 | 산식 | 단위 |
|------|----------|------|------|
| totalUsers | totalUsers | branch×user | 명 |
| designedSessions | designedSessions | total×(1+margin) | 세션 |
| tps | scenarioResults | concurrent÷응답 | TPS |
| apCount | cap030 | TPS÷vmTps×A-A | 대 |

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70% |
| 720 TPS | Thread/Pool 고갈 없음 |
| 1,080 TPS | Fail-fast 정상 |
| 센터 장애 | 잔여 TPS≥720 |

#### 주의사항

- 권고값은 출발점 — 선도개발·시험 후 확정
- 세션≠TPS, Pool≠Thread

### A.3 CAP 화면 매핑

본 절은 **부록 A** — **CAP 화면 매핑** (용량산정 입력자료 양식) NSIGHT 1차 표준 적용 기준입니다.

#### CAP-010 (`/oc/capacity.html`)

| 부록 A 필드 | DTO (CapacityCalculationCDTO) | UI id (capacity.html) | 화면 라벨 | 타입 | 기본값 |
|-------------|------------------------------|------------------------|----------|------|--------|
| projectName | projectName | `projectName` | 프로젝트명 | string | 6,000지점 표준 |
| branchCount | branchCount | `branchCount` | 지점 수 | int | 6000 | · 부록 A·ENV 기본 3600 — 화면 기본값 상이
| userPerBranch | userPerBranch | `userPerBranch` | 지점당 사용자 | int | 6 |
| totalUsers | resolvedTotalUsers() | `totalUsers` | 전체 사용자 | int (readonly) | branchCount×userPerBranch |
| designedSessions | (산출) | `designedSessions` | 설계 세션 수 | int (readonly) | totalUsers×(1+sessionMarginRate) |
| sessionMarginRate | sessionMarginRate | `sessionMarginRate` | 세션 여유율 | double | 0.30 |
| sessionTimeoutMin | sessionTimeoutMin | `sessionTimeout` | 세션 타임아웃 | int | 60 |
| concurrentRequestRates | concurrentRequestRates | `rate` | 동시요청률 | List<Double> | 0.03,0.05,0.10,0.15 | · UI는 % 정수, API는 소수
| targetResponseTimes | targetResponseTimes | `timeout` | 목표 응답(초) | List<Integer> | 3,4,5 |
| vmSpecCode | vmSpecCode | `vmSpec` | VM 사양 | string | 8C64G |
| tpsPerCore | tpsPerCore | `tpsPerCore` | Core당 TPS | int | 35 | · TPMC 가이드 행 클릭 시 연동
| tpmcPerTps | tpmcPerTps | `tpmcPerTps` | 1 TPS당 TPMC | int | 3000 |
| avgThreadHoldSec | avgThreadHoldSec | `avgThreadHoldSec` | 평균 Thread 점유(초) | double | 1.2 |
| threadMarginRate | threadMarginRate | `threadMarginRate` | Thread 여유율 | double | 1.2 |
| maxThreadMarginRate | maxThreadMarginRate | `maxThreadMarginRate` | maxThreads 배율 | double | 1.3 |
| apType | apType | `apType` | AP 유형 | enum | GENERAL |
| avgDbConnectionHoldSec | avgDbConnectionHoldSec | `avgDbConnectionHoldSec` | DB Connection 점유(초) | double | 0.15/0.20 | · AP 유형별 기본
| dbTransactionUsageRatio | dbTransactionUsageRatio | `dbTransactionUsageRatio` | DB 사용 거래 비율 | double | 1.0 |
| poolSafetyFactor | poolSafetyFactor | `poolSafetyFactor` | Pool 안전계수 | double | 1.3 |
| threadDbUsageRatio | threadDbUsageRatio | `threadDbUsageRatio` | Thread→DB 사용 비율 | double | 0.30 |
| minPoolPerVm | minPoolPerVm | `minPoolPerVm` | 최소 Pool/VM | int | 30 |
| activeActive | activeActive | `activeActive` | 2센터 Active-Active | boolean | true |
| drValidation | drValidation | `drValidation` | DR·잔여 TPS | boolean | true |
| validateDbPool | validateDbPool | `validateDbPool` | DB Session 한도 검증 | boolean | true |
| dbSessionLimit | dbSessionLimit | `dbSessionLimit` | DB Session 한도 | int | 500 |
| calculationStep | calculationStep | `(내부)` | 산정 단계 | string | ALL |

#### ENV-002 (`/oc/env-002.html`)

| 부록 A 필드 | DTO (CapacityPlannerRequest) | UI id (env-002.html) | 화면 라벨 | 타입 | 기본값 |
|-------------|-------------------------------|----------------------|----------|------|--------|
| scenarioName | scenarioName | `(자동)` | 시나리오명 | string | ENV-002 산정 |
| branchCount | branchCount | `capBranchCount` | 지점 수 | int | 3600 |
| usersPerBranch | usersPerBranch | `capUsersPerBranch` | 지점당 사용자 | int | 6 | · CAP DTO는 userPerBranch(단수)
| totalUsers | totalUsers | `capTotalUsers` | 전체 사용자 | int | 21600 |
| vmProfileId | vmProfileId | `capVm` | VM 프로파일 | string | 8CORE-32GB |
| customVm | customVm | `capVm=CUSTOM` | 커스텀 VM | boolean | false |
| customCore | customCore | `capCustomCore` | 커스텀 Core | int | 8 |
| customMemoryGb | customMemoryGb | `capCustomMemory` | 커스텀 메모리(GB) | int | 64 |
| tpsPerCoreMin | tpsPerCoreMin | `capTpsPerCoreMin` | Core TPS Min | int | 30 |
| tpsPerCoreBase | tpsPerCoreBase | `capTpsPerCoreBase` | Core TPS Base | int | 35 |
| tpsPerCoreMax | tpsPerCoreMax | `capTpsPerCoreMax` | Core TPS Max | int | 40 |
| tpmcPerTps | tpmcPerTps | `capTpmcPerTps` | 1 TPS당 TPMC | int | 3000 |
| manualCoreTps | manualCoreTps | `capManualCoreTps` | Core TPS 수동 | boolean | false |
| actualRequestPercents | actualRequestPercents | `capPercent` | 동시요청률 | List<Integer> | 3,5,10,15 |
| responseTimeoutSeconds | responseTimeoutSeconds | `capTimeout` | 목표 응답(초) | List<Integer> | 3,4,5 |
| sessionIdleMinutes | sessionIdleMinutes | `capSession` | 세션 Idle(분) | List<Integer> | 60 |
| activeActive | activeActive | `capActiveActive` | Active-Active | boolean | true |
| drValidation | drValidation | `capDrValidation` | DR 검증 | boolean | true |
| validateDbPool | validateDbPool | `capValidateDbPool` | DB Pool 검증 | boolean | true |
| includeSettingExamples | includeSettingExamples | `capIncludeExamples` | 설정 예시 포함 | boolean | true |
| hikariPoolPerVm | hikariPoolPerVm | `(산출 입력)` | Hikari Pool/VM | int | 0 | · ENV-003/004 Grid 연동
| dbSessionLimit | dbSessionLimit | `(baseline)` | DB Session 한도 | int | 500 |

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70% |
| 720 TPS | Thread/Pool 고갈 없음 |
| 1,080 TPS | Fail-fast 정상 |
| 센터 장애 | 잔여 TPS≥720 |

#### 주의사항

- 권고값은 출발점 — 선도개발·시험 후 확정
- 세션≠TPS, Pool≠Thread

### A.4 검증·판정

본 절은 **부록 A** — **검증·판정** (용량산정 입력자료 양식) NSIGHT 1차 표준 적용 기준입니다.

| 항목 | 권고 | 비고 |
|------|------|------|
| 기준 VM | 8 vCPU / 32GB | Scale-Out |
| TPS | 360/720/1080 | 피크 720 |
| p95 | 3초 이하 | SLA |
| 검증 | 360/720/1080 | 성능시험 |

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70% |
| 720 TPS | Thread/Pool 고갈 없음 |
| 1,080 TPS | Fail-fast 정상 |
| 센터 장애 | 잔여 TPS≥720 |

#### 주의사항

- 권고값은 출발점 — 선도개발·시험 후 확정
- 세션≠TPS, Pool≠Thread

### A.5 작성 예시

본 절은 **부록 A** — **작성 예시** (용량산정 입력자료 양식) NSIGHT 1차 표준 적용 기준입니다.

| 항목 | 권고 | 비고 |
|------|------|------|
| 기준 VM | 8 vCPU / 32GB | Scale-Out |
| TPS | 360/720/1080 | 피크 720 |
| p95 | 3초 이하 | SLA |
| 검증 | 360/720/1080 | 성능시험 |

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70% |
| 720 TPS | Thread/Pool 고갈 없음 |
| 1,080 TPS | Fail-fast 정상 |
| 센터 장애 | 잔여 TPS≥720 |

#### 주의사항

- 권고값은 출발점 — 선도개발·시험 후 확정
- 세션≠TPS, Pool≠Thread


---

[← 목차](../00-목차.md)
