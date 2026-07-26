# 26. HikariCP 환경설정

> 제4부. 솔루션별 환경설정 가이드

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

> 출처: `znsight-capacity-word` · [13단계 요약](./zNSIGHT-용량산정-전체-흐름.md)


**HikariCP** — Pool 산정 4단계 공식.

## 원문 기반 본문

맞습니다. DB Pool Size는 단순히 AP당 40개, 60개처럼 정하면 안 되고, 다음 관계로 산정해야 합니다.

동시요청자 → TPS → WAS 실행쓰레드 → DB 점유시간 → DB Pool Size핵심은 이것입니다.

DB Pool Size는 “동시에 DB Connection을 점유하는 요청 수”를 수용할 만큼만 잡아야 한다.

너무 작으면 Hikari Pool 대기가 발생하고, 너무 크면 DB Session이 폭증합니다.

## 1. 기본 산정 흐름NSIGHT 기준에서도 세션 수는 로그인 유지 규모이고, TPS·Thread·DB Pool·서버 수는 동시 요청자 기준으로 분리해야 한다고 정리했습니다.

또한 목표 TPS는 동시 요청자 ÷ 목표 응답시간으로 산정합니다.① 동시요청자 산정② TPS 산정③ WAS 실행쓰레드 산정④ 거래 1건당 DB Connection 점유시간 산정⑤ DB Pool Size 산정⑥ 전체 AP 수량 기준 DB Session 총량 검증

## 2. DB Pool Size 핵심 산정식

### 2.1 기본 공식DB Pool Size =TPS × 거래 1건당 DB Connection 점유시간(초) × DB 사용 거래 비율 × 여유율즉,DB Pool = TPS × DB_Connection_Hold_Time × DB_Usage_Ratio × Safety_Factor여기서 각 의미는 다음과 같습니다.

항목의미예시TPSAP 1대 또는 전체 기준 초당 요청 처리량250 TP

S/VMDB Connection 점유시간거래 1건이 DB Connection을 빌려 쓰는 시간0.10초~0.30초DB 사용 거래 비율전체 요청 중 DB를 사용하는 비율0.8~1.0여유율순간 피크, SQL 편차, GC, 네트워크 지연 보정1.2~1.

### 52.2 WAS 실행쓰레드까지 반영한 공식DB Pool은 WAS 실행쓰레드보다 클 필요가 없습니다.

WAS Thread가 300개인데 DB Pool을 500개로 잡아도 동시에 DB를 요청할 수 있는 요청은 Thread 수를 넘을 수 없습니다.

따라서 실제 적용 공식은 이렇게 잡는 것이 좋습니다.

권장 DB Pool Size =min(    WAS 실행쓰레드 수 × DB 사용 요청 비율,    TPS × DB Connection 점유시간 × DB 사용 거래 비율 × 여유율)다만 이 식은 너무 작게 나올 수 있으므로, 운영 최소값을 둡니다.

최종 DB Pool Size =max(    최소 운영 Pool,    min(WAS 실행쓰레드 기반 상한, TPS 기반 산정값))

## 3. 실무형 최종 공식제가 판단하는 NSIGHT용 최종 산정식은 아래입니다.① TPS = 동시요청자 ÷ 목표응답시간② AP당 TPS = 전체 TPS ÷ AP 수량③ 산정 WAS Thread =   AP당 TPS × 평균 응답시간 × Thread 여유율④ DB Pool 산정값 =   AP당 TPS × 평

균 DB Connection 점유시간 × DB 사용 거래 비율 × Pool 여유율⑤ DB Pool 상한 =   WAS 실행쓰레드 수 × DB 동시진입 비율⑥ 최종 DB Pool =   max(최소 Pool, min(DB Pool 산정값, DB Pool 상

한))

## 4. 각 변수의 권장 기준변수권장 기준설명목표 응답시간p95 3초사용자 체감 SLA평균 응답시간1.0~1.2초Thread 산정에는 p95가 아니라 평균값 사용DB Connection 점유시간0.10~0.30초SQL 수행 + Fetch + Mapping 중 Connection 점유 시간DB 사용 거래 비율0.8~1.0조회성 업무는 보통 1.0에 가깝게 적용Pool 여유율1.2~1.5순간 피크와 SQL 편차 보정DB 동시진입 비율20~40%WAS Thread 중 동시에 DB에 들어가는 비율최소 Pool20~30운영 안정성 기준최대 PoolDB Session 총량 기준으로 제한AP 증가 시 폭증 방지Tomcat Thread는 기존 기준에서도 TPS × 평균응답시간 × 여유율로 산정하고, p95 3초를 직접 곱하지 않는다고 정리했습니다.

## 5. 예시 1: 8 Core VM, 250 TPS 기준

### 5.1 전제항목값AP당 TPS250 TPS평균 응답시간1.2초Thread 여유율1.2평균 DB Connection 점유시간0.15초DB 사용 거래 비율1.0Pool 여유율1.3WAS 실행쓰레드360개DB 동시진입 비율30%최소 Pool3

### 05.2 WAS 실행쓰레드 산정WAS 실행쓰레드 =250 TPS × 1.2초 × 1.2= 360 Thread즉, Tomcat maxThreads는 운영 여유를 포함해 400~500 정도가 적정합니다. 기존 Tomcat 기준에서도 8Core는 maxThreads 400~500, 산정 Thread 300~360 수준으로 정리했습니다.

### 5.3 DB Pool 산정DB Pool 산정값 =250 TPS × 0.15초 × 1.0 × 1.3= 48.75≈ 50즉, AP 1대당 DB Pool은 약 50개가 됩니다.

### 5.4 WAS Thread 기반 상한DB Pool 상한 =360 Thread × 30%= 108그러면 최종값은 다음과 같습니다.

최종 DB Pool =max(30, min(50, 108))= 50따라서 8Core / 250 TPS / 평균 DB 점유 150ms 기준이면 DB Pool 50개가 합리적입니다.

기존 HikariCP 기준에서도 일반 AP 8Core는 maximumPoolSize 50, SingleView는 60 정도로 정리되어 있습니다.

## 6. 예시 2: Single View처럼 DB 점유시간이 긴 경우Single View는 RDW 고빈도 조회, 다중 SQL, 마스킹, Fetch가 포함될 수 있습니다.

항목값AP당 TPS250 TPS평균 DB Connection 점유시간0.20초DB 사용 거래 비율1.0Pool 여유율1.3DB Pool =250 × 0.20 × 1.0 × 1.3= 65이 경우 DB Pool은 60~70개가 됩니다.

즉,일반 마케팅 AP: 40~50Single View AP: 60~70정도가 더 현실적입니다.

## 7. 예시 3: DB SQL이 느린 경우만약 평균 DB Connection 점유시간이 0.5초까지 늘어나면 어떻게 될까요?DB Pool =250 TPS × 0.5초 × 1.0 × 1.3= 162.5이렇게 되면 AP 1대당 DB Pool이 160개 이상 필요하다는 계산이 나옵니다.

하지만 이건 Pool을 늘릴 문제가 아니라, SQL 성능 문제입니다.

이 경우 판단은 이렇게 해야 합니다.

현상판단DB Pool 산정값이 100개 이상으로 커짐SQL 또는 Fetch 시간이 긴 것Hikari 대기 발생Pool 부족 또는 SQL 지연DB Session 급증Pool 과다 설정 위험해결 방향Pool 증설보다 SQL 튜닝, Fetch Size 조정, 조회 범위 제한Hikari connectionTimeout은 SQL 실행시간이 아니라 Pool에서 Connection을 얻기 위해 기다리는 시간입니다. 따라서 Pool Wait이 발생하면 “DB가 느린지, Pool이 작은지, SQL이 오래 잡고 있는지”를 분리해서 봐야 합니다.

## 8. 화면설계서에 넣을 산정 항목용량산정 화면에는 DB Pool 산정을 위해 최소한 아래 입력값이 필요합니다.

입력 항목설명기본값전체 사용자 수지점 수 × 지점당 사용자36,000동시 요청률3%, 5%, 10%, 15%5%목표 응답시간TPS 산정 기준3초AP 수량전체 TPS를 나눌 대상자동계산VM 사양8Core / 16Core / 32Core8CoreAP당 TPS전체 TPS ÷ AP 수량자동계산평균 응답시간Thread 산정 기준1.2초평균 DB 점유시간Connection Hold Time0.15초DB 사용 거래 비율DB를 사용하는 요청 비율1.0Pool 여유율피크 보정1.3WAS 실행쓰레드산정값 또는 직접 입력자동계산DB 동시진입 비율Thread 중 DB 진입 비율30%최소 Pool운영 최소값30DB 최대 세션 한도DBA 제공값입력

## 9. 화면 결과값화면에서는 아래 값을 자동으로 보여줘야 합니다.

결과 항목산정식동시 요청자전체 사용자 × 동시 요청률전체 TPS동시 요청자 ÷ 목표 응답시간AP당 TPS전체 TPS ÷ AP 수량WAS 실행쓰레드AP당 TPS × 평균응답시간 × Thread 여유율DB Pool 산정값AP당 TPS × DB점유시간 × DB사용비율 × Pool여유율DB Pool 상한WAS 실행쓰레드 × DB동시진입비율최종 DB Poolmax(최소Pool, min(DB Pool 산정값, DB Pool 상한))DB Session 총량최종 DB Pool × AP 수량 × DataSource 수장애 시 DB Session최종 DB Pool × 장애 후 잔여 AP 수량판정정상 / 주의 / 위험

## 10. 판정 기준판정기준조치정상DB Pool 사용률 70% 이하유지주의DB Pool 사용률 70~85%SQL 점검, Pool 보정위험DB Pool 사용률 85% 초과Pool 증설보다 병목 분석 우선위험Hikari 대기 빈번SQL 시간, Pool 크기, DB Session 확인위험DB Session 총량 초과AP 수량, Pool 상한, DataSource 분리 재검토

## 11. 최종 권장 산식문서에는 아래 공식으로 넣는 것이 가장 좋습니다.[DB Pool Size 산정식]TPS = 동시요청자 ÷ 목표응답시간AP당 TPS = 전체 TPS ÷ AP 수량WAS 실행쓰레드 =AP당 TPS × 평균응답시간 × Thread 여유율DB Pool 산정값 =AP당 TPS × 평균 DB Connection 점유

시간 × DB 사용 거래 비율 × Pool 여유율DB Pool 상한 =WAS 실행쓰레드 × DB 동시진입 비율최종 DB Pool Size =max(최소 Pool, min(DB Pool 산정값, DB Pool 상한))결론DB Pool Size는 다음 3가지를 동시에 만족해야 합니다.1. TPS를 처리할 수 있을 만큼 충분해야 한다.2. WAS 실행쓰레드보다 과도하게 커지면 안 된다.3. DB Session 총량을 초과하면 안 된다.

따라서 NSIGHT 기준의 최종 판단식은 다음입니다.

DB Pool Size =AP당 TPS × 평균 DB Connection 점유시간 × DB 사용 거래 비율 × Pool 여유율그리고 운영 통제식은 다음입니다.

최종 DB Pool Size =max(최소 Pool, min(DB Pool 산정값, WAS Thread 기반 상한))이렇게 잡으면 동시요청자, 실행쓰레드, 응답시간, DB 점유시간, DB Session 총량이 모두 연결됩니다.

## 절별 상세

### 26.1 Pool 산정 기본식

본 절(**Pool 산정 기본식**)은 HikariCP 영역에서 **DB Pool** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 일반 AP | SV AP | 산식 |
|------|---------|-------|------|
| maximumPoolSize | 50 | 60 | TPS×0.15×1.3 |
| minimumIdle | 15 | 15 | max의 20~30% |
| connectionTimeout | 3초 | 3초 | ≠ SQL timeout |
| maxLifetime | 30분 | 30분 | DB Idle보다 짧게 |

#### 설정 예시

```yaml
spring.datasource.hikari:
  pool-name: marketing-pool
  maximum-pool-size: 50
  minimum-idle: 15
  connection-timeout: 3000
  idle-timeout: 600000
  max-lifetime: 1800000
  auto-commit: false
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: application.yml

### 26.2 maximumPoolSize

본 절(**maximumPoolSize**)은 HikariCP 영역에서 **DB Pool** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 일반 AP | SV AP | 산식 |
|------|---------|-------|------|
| maximumPoolSize | 50 | 60 | TPS×0.15×1.3 |
| minimumIdle | 15 | 15 | max의 20~30% |
| connectionTimeout | 3초 | 3초 | ≠ SQL timeout |
| maxLifetime | 30분 | 30분 | DB Idle보다 짧게 |

#### 설정 예시

```yaml
spring.datasource.hikari:
  pool-name: marketing-pool
  maximum-pool-size: 50
  minimum-idle: 15
  connection-timeout: 3000
  idle-timeout: 600000
  max-lifetime: 1800000
  auto-commit: false
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: application.yml

### 26.3 minimumIdle

본 절(**minimumIdle**)은 HikariCP 영역에서 **DB Pool** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | DB Pool 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | application.yml |
| 핵심 | Pool 50/60 |

#### 설정 예시

```yaml
spring.datasource.hikari:
  pool-name: marketing-pool
  maximum-pool-size: 50
  minimum-idle: 15
  connection-timeout: 3000
  idle-timeout: 600000
  max-lifetime: 1800000
  auto-commit: false
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: application.yml

### 26.4 connectionTimeout

본 절(**connectionTimeout**)은 HikariCP 영역에서 **DB Pool** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 계층 | 권고 | 의미 |
|------|------|------|
| Hikari | 3초 | Pool 획득 |
| Tomcat | 8초 | 연결 대기 |
| L4 Connect | 2~3초 | 백엔드 연결 |

#### 설정 예시

**SQL 실행시간과 Pool connectionTimeout은 별개**.

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: application.yml

### 26.5 validationTimeout

본 절(**validationTimeout**)은 HikariCP 영역에서 **DB Pool** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 계층 | 권고 | 설정 위치 |
|------|------|----------|
| SQL | 3초 | MyBatis / Mapper |
| Pool 획득 | 3초 | Hikari connectionTimeout |
| Transaction | 5초 | @Transactional |
| TCF ServiceId | 4~5초 | TCF 설정 |
| Proxy | 10초 | Apache / Gateway |
| WebTop | 15초 | 단말 |
| L4 Idle | 120초 | L4 Profile |

#### 설정 예시

```yaml
spring.datasource.hikari:
  pool-name: marketing-pool
  maximum-pool-size: 50
  minimum-idle: 15
  connection-timeout: 3000
  idle-timeout: 600000
  max-lifetime: 1800000
  auto-commit: false
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: application.yml

### 26.6 idleTimeout

본 절(**idleTimeout**)은 HikariCP 영역에서 **DB Pool** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 권고 |
|------|------|
| idleTimeout | 10분 |
| maxLifetime | 30분 |
| keepaliveTime | 2~5분 |

#### 설정 예시

DB·L4 Idle보다 **짧게**.

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: application.yml

### 26.7 maxLifetime

본 절(**maxLifetime**)은 HikariCP 영역에서 **DB Pool** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 권고 |
|------|------|
| idleTimeout | 10분 |
| maxLifetime | 30분 |
| keepaliveTime | 2~5분 |

#### 설정 예시

DB·L4 Idle보다 **짧게**.

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: application.yml

### 26.8 keepaliveTime

본 절(**keepaliveTime**)은 HikariCP 영역에서 **DB Pool** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 권고 |
|------|------|
| idleTimeout | 10분 |
| maxLifetime | 30분 |
| keepaliveTime | 2~5분 |

#### 설정 예시

DB·L4 Idle보다 **짧게**.

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: application.yml

### 26.9 leakDetectionThreshold

본 절(**leakDetectionThreshold**)은 HikariCP 영역에서 **DB Pool** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 환경 | threshold |
|------|----------|
| DEV/STG | 60s |
| PRD | 선택(오버헤드) |

#### 설정 예시

Connection 미반환 조기 탐지.

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: application.yml

### 26.10 autoCommit

본 절(**autoCommit**)은 HikariCP 영역에서 **DB Pool** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 권고 |
|------|------|
| autoCommit | false |
| Tx | @Transactional |

#### 설정 예시

```yaml
auto-commit: false
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: application.yml

### 26.11 Pool Name

본 절(**Pool Name**)은 HikariCP 영역에서 **DB Pool** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 일반 AP | SV AP | 산식 |
|------|---------|-------|------|
| maximumPoolSize | 50 | 60 | TPS×0.15×1.3 |
| minimumIdle | 15 | 15 | max의 20~30% |
| connectionTimeout | 3초 | 3초 | ≠ SQL timeout |
| maxLifetime | 30분 | 30분 | DB Idle보다 짧게 |

#### 설정 예시

```yaml
spring.datasource.hikari:
  pool-name: marketing-pool
  maximum-pool-size: 50
  minimum-idle: 15
  connection-timeout: 3000
  idle-timeout: 600000
  max-lifetime: 1800000
  auto-commit: false
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: application.yml

### 26.12 업무 WAR별 Pool 분리

본 절(**업무 WAR별 Pool 분리**)은 HikariCP 영역에서 **DB Pool** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 일반 AP | SV AP | 산식 |
|------|---------|-------|------|
| maximumPoolSize | 50 | 60 | TPS×0.15×1.3 |
| minimumIdle | 15 | 15 | max의 20~30% |
| connectionTimeout | 3초 | 3초 | ≠ SQL timeout |
| maxLifetime | 30분 | 30분 | DB Idle보다 짧게 |

#### 설정 예시

```yaml
spring.datasource.hikari:
  pool-name: marketing-pool
  maximum-pool-size: 50
  minimum-idle: 15
  connection-timeout: 3000
  idle-timeout: 600000
  max-lifetime: 1800000
  auto-commit: false
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: application.yml

### 26.13 Tomcat 전체 Pool 합산

본 절(**Tomcat 전체 Pool 합산**)은 HikariCP 영역에서 **DB Pool** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 일반 AP | SV AP | 산식 |
|------|---------|-------|------|
| maximumPoolSize | 50 | 60 | TPS×0.15×1.3 |
| minimumIdle | 15 | 15 | max의 20~30% |
| connectionTimeout | 3초 | 3초 | ≠ SQL timeout |
| maxLifetime | 30분 | 30분 | DB Idle보다 짧게 |

#### 설정 예시

```yaml
spring.datasource.hikari:
  pool-name: marketing-pool
  maximum-pool-size: 50
  minimum-idle: 15
  connection-timeout: 3000
  idle-timeout: 600000
  max-lifetime: 1800000
  auto-commit: false
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: application.yml

### 26.14 센터 전체 DB Connection 합산

본 절(**센터 전체 DB Connection 합산**)은 HikariCP 영역에서 **DB Pool** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | DB Pool 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | application.yml |
| 핵심 | Pool 50/60 |

#### 설정 예시

```yaml
spring.datasource.hikari:
  pool-name: marketing-pool
  maximum-pool-size: 50
  minimum-idle: 15
  connection-timeout: 3000
  idle-timeout: 600000
  max-lifetime: 1800000
  auto-commit: false
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: application.yml

### 26.15 DB 최대 Session 보호

본 절(**DB 최대 Session 보호**)은 HikariCP 영역에서 **DB Pool** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | DB Pool 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | application.yml |
| 핵심 | Pool 50/60 |

#### 설정 예시

```yaml
spring.datasource.hikari:
  pool-name: marketing-pool
  maximum-pool-size: 50
  minimum-idle: 15
  connection-timeout: 3000
  idle-timeout: 600000
  max-lifetime: 1800000
  auto-commit: false
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: application.yml

### 26.16 HikariCP 설정 예시

본 절(**HikariCP 설정 예시**)은 HikariCP 영역에서 **DB Pool** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 일반 AP | SV AP | 산식 |
|------|---------|-------|------|
| maximumPoolSize | 50 | 60 | TPS×0.15×1.3 |
| minimumIdle | 15 | 15 | max의 20~30% |
| connectionTimeout | 3초 | 3초 | ≠ SQL timeout |
| maxLifetime | 30분 | 30분 | DB Idle보다 짧게 |

#### 설정 예시

```yaml
spring.datasource.hikari:
  pool-name: marketing-pool
  maximum-pool-size: 50
  minimum-idle: 15
  connection-timeout: 3000
  idle-timeout: 600000
  max-lifetime: 1800000
  auto-commit: false
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

DEV→STG→PRD 동일 항목·환경별 수치만 변경. PRD 확정값은 변경관리(53장) 준수.

### 26.17 HikariCP 설정 검증

본 절(**HikariCP 설정 검증**)은 HikariCP 영역에서 **DB Pool** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 일반 AP | SV AP | 산식 |
|------|---------|-------|------|
| maximumPoolSize | 50 | 60 | TPS×0.15×1.3 |
| minimumIdle | 15 | 15 | max의 20~30% |
| connectionTimeout | 3초 | 3초 | ≠ SQL timeout |
| maxLifetime | 30분 | 30분 | DB Idle보다 짧게 |

#### 설정 예시

```yaml
spring.datasource.hikari:
  pool-name: marketing-pool
  maximum-pool-size: 50
  minimum-idle: 15
  connection-timeout: 3000
  idle-timeout: 600000
  max-lifetime: 1800000
  auto-commit: false
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

검증 도구: APM, `jcmd`, `jstat`, Hikari Metrics, Access Log(GUID), ENV rule-check.

---

[← 목차](./00-목차.md)
