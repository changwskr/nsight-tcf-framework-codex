# 54. 정기 Capacity Review

> 제8부. 변경·운영·폐기 관리

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


**Capacity Review** — 일간·주간·월간 Baseline.

## 원문 기반 본문

## 4. 시스템 설정 및 최적화 가이드라인

### 4.1 Tomcat 및 JVM 설정고부하 환경에서의 안정적인 처리를 위한 환경 설정값이다.

구분항목권장 설정값TomcatmaxThreads500 (VM당)acceptCount200~500maxConnections8,192~10,000keepAliveTimeout60초 (L4 Idle Timeout보다 짧게 설정)JVMHeap (일반 AP)12GBHeap (SingleView)14GBGC AlgorithmG1GC (MaxGCPauseMillis=200)

### 4.2 데이터베이스(HikariCP) 설정연결 고갈 방지를 위해 업무 성격에 따라 Pool을 분리하여 관리한다.

대상 업무maximumPoolSizeminimumIdle비고일반 마케팅 AP5010RDW 데이터소스 사용SingleView AP6015RDW 전용 데이터소스

## 5. 타임아웃(Timeout) 정합성 설계시스템 간 연계 시 병목 현상을 차단하기 위해 타임아웃은 하위 자원에서 상위 서비스 방향으로 길어지도록 설계한다.

DB Query Timeout: 2~3초 (SQL 장기 실행 차단)Hikari Connection Timeout: 3초 (Pool 대기 한도)Spring Transaction Timeout: 4~5초 (업무 트랜잭션 상한)WebTopSuite Request Timeout: 6~8초 (사용자 응답 대기)Tomcat connectionTimeout: 10초L4 Idle Timeout: 70~90초 (Tomcat KeepAlive보다 길게 유지)

## 6. Runtime별 용량산정 및 관리 원칙

### 6.1 자원 경합 차단NSIGHT는 업무 유형에 따라 Runtime을 분리하여 상호 간섭을 최소화한다.

RDW: 현행성·정합성 중심, Single View 조회 전용.

ADW: 분석·집계·통계 및 BI 조회 전용.

자원 분리: 온라인 조회와 대량 배치/분석 SQL이 동일 자원을 점유하지 않도록 Pool 및 인스턴스를 분리한다.

### 6.2 데이터 흐름 기반 산정FAST 흐름 (CDC/Kafka): 지연시간(Latency)과 Consumer Lag을 중심으로 산정.

DEEP 흐름 (ETL/DataStage): 적재 데이터량과 배치 윈도우(시간 내 완료 여부)를 중심으로 산정.

데이터 보관: 초기 데이터량 외에 5년 성장률 및 인덱스/파티션 오버헤드를 반영하여 저장 공간을 확보한다.

### 6.3 운영 임계치 기준관리 영역정상 (Normal)경고 (Warning)심각 (Critical)Tomcat Busy Thread70% 이하80% 이상 지속90% 이상 지속JVM Heap 사용률70% 이하70% 이상 지속80% 이상 또는 Full GCDB SQL 평균 속도100~300ms300~500ms1초 이상 반복Hikari Pool 사용률70~80% 이하80% 이상 지속90% 이상 또는 Pending 증가최종

> **결론**: 본 설계안은 VM당 250 TPS 성능을 전제로 피크 시 8대 구성을 제안하며, 이는 평균 응답시간 1.0~1.2초 및 DB SQL 300ms 이하라는 성능 지표가 충족될 때 유효하다. 최종 수량은 Single View 성능테스트 결과에 따라 보정되어야 한다.

## 절별 상세

### 54.1 일간 운영지표

본 절(**일간 운영지표**)은 Capacity Review 영역에서 **Baseline** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 지표 | Warning | Critical | 조치 |
|------|---------|----------|------|
| CPU | 70% | 85% | Scale-Out·SQL |
| Heap | 70% | 85% | Dump·GC 분석 |
| Hikari Active | 70% | 90% | Pool·SQL |
| Hikari Pending | >0 1분 | >0 5분 | 즉시 분석 |
| GC Pause p95 | 200ms | 500ms | Heap·객체 |
| p95 응답 | 2.5초 | 3초 | E2E Trace |

#### 설정 예시

**설정 파일**: `월간 리포트` · **핵심 항목**: 일·주·월

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
- 운영 반영 전 **ENV rule-check** 및 성능시험 합격 필수

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: 월간 리포트

### 54.2 주간 병목 검토

본 절(**주간 병목 검토**)은 Capacity Review 영역에서 **Baseline** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | Baseline 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | 월간 리포트 |
| 핵심 | 일·주·월 |

#### 설정 예시

**설정 파일**: `월간 리포트` · **핵심 항목**: 일·주·월

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
- 운영 반영 전 **ENV rule-check** 및 성능시험 합격 필수

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: 월간 리포트

### 54.3 월간 Capacity Review

본 절(**월간 Capacity Review**)은 Capacity Review 영역에서 **Baseline** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | Baseline 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | 월간 리포트 |
| 핵심 | 일·주·월 |

#### 설정 예시

**설정 파일**: `월간 리포트` · **핵심 항목**: 일·주·월

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
- 운영 반영 전 **ENV rule-check** 및 성능시험 합격 필수

#### 운영 참고

변경 흐름: 요청→영향도→승인→STG 검증→PRD 반영→이력(부록 AB).

### 54.4 분기별 성장률 검토

본 절(**분기별 성장률 검토**)은 Capacity Review 영역에서 **Baseline** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | Baseline 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | 월간 리포트 |
| 핵심 | 일·주·월 |

#### 설정 예시

**설정 파일**: `월간 리포트` · **핵심 항목**: 일·주·월

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
- 운영 반영 전 **ENV rule-check** 및 성능시험 합격 필수

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: 월간 리포트

### 54.5 사용자 증가 반영

본 절(**사용자 증가 반영**)은 Capacity Review 영역에서 **Baseline** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 시나리오 | 동시요청률 | 동시요청자 | TPS |
|----------|-----------|-----------|-----|
| 기본 | 5% | 1,080 | 360 |
| 피크 | 10% | 2,160 | 720 |
| 스트레스 | 15% | 3,240 | 1080 |

| VM | VM당 TPS | 피크 AP 권장 |
|----|----------|-------------|
| 8C/32G | 250 | 8대(A-A) |

#### 설정 예시

```
동시요청자 = 전체사용자 × 동시요청률
TPS = 동시요청자 ÷ 목표응답시간(3초)
AP 대수 = ⌈TPS ÷ 250⌉ (A-A 배치)
Thread = AP당TPS × 1.2초 × 1.2
Pool = max(30, min(TPS×0.15×1.3, Thread×30%))
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
- 운영 반영 전 **ENV rule-check** 및 성능시험 합격 필수

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: 월간 리포트

### 54.6 신규 업무·WAR 추가 반영

본 절(**신규 업무·WAR 추가 반영**)은 Capacity Review 영역에서 **Baseline** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | Baseline 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | 월간 리포트 |
| 핵심 | 일·주·월 |

#### 설정 예시

**설정 파일**: `월간 리포트` · **핵심 항목**: 일·주·월

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
- 운영 반영 전 **ENV rule-check** 및 성능시험 합격 필수

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: 월간 리포트

### 54.7 SQL 변경 영향 반영

본 절(**SQL 변경 영향 반영**)은 Capacity Review 영역에서 **Baseline** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | Baseline 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | 월간 리포트 |
| 핵심 | 일·주·월 |

#### 설정 예시

**설정 파일**: `월간 리포트` · **핵심 항목**: 일·주·월

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
- 운영 반영 전 **ENV rule-check** 및 성능시험 합격 필수

#### 운영 참고

변경 흐름: 요청→영향도→승인→STG 검증→PRD 반영→이력(부록 AB).

### 54.8 인프라 사양 변경 반영

본 절(**인프라 사양 변경 반영**)은 Capacity Review 영역에서 **Baseline** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | Baseline 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | 월간 리포트 |
| 핵심 | 일·주·월 |

#### 설정 예시

**설정 파일**: `월간 리포트` · **핵심 항목**: 일·주·월

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
- 운영 반영 전 **ENV rule-check** 및 성능시험 합격 필수

#### 운영 참고

변경 흐름: 요청→영향도→승인→STG 검증→PRD 반영→이력(부록 AB).

### 54.9 운영 Baseline 갱신

본 절(**운영 Baseline 갱신**)은 Capacity Review 영역에서 **Baseline** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | Baseline 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | 월간 리포트 |
| 핵심 | 일·주·월 |

#### 설정 예시

**설정 파일**: `월간 리포트` · **핵심 항목**: 일·주·월

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
- 운영 반영 전 **ENV rule-check** 및 성능시험 합격 필수

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: 월간 리포트

## 정기 Capacity Review — 실무 요약

본 장은 **Capacity Review**(Baseline)의 핵심을 23장(JVM)과 동일한 깊이로 요약합니다.

### E2E 용량산정 체인

```
[1] 사용자 21,600 → [2] 동시요청자 1,080~3,240
→ [3] TPS 360/720/1080 → [4] AP 8대 권장
→ [5] Thread 400~500 → [6] Pool 50/60
→ [7] DB Session Σ(AP×Pool) → [8] 장애 잔여 TPS≥720
```

### NSIGHT 1차 표준 요약

| 항목 | NSIGHT 1차 권고 |
|------|----------------|
| 사용자 | 21,600명 (3600×6) |
| 세션 | 26,000~28,000 |
| TPS | 360 / 720 / 1080 |
| VM | 8 vCPU / 32GB |
| VM당 TPS | 250 |
| p95 | 3초 이하 |

### Timeout 계층

| 계층 | 권고 | 설정 위치 |
|------|------|----------|
| SQL | 3초 | MyBatis / Mapper |
| Pool 획득 | 3초 | Hikari connectionTimeout |
| Transaction | 5초 | @Transactional |
| TCF ServiceId | 4~5초 | TCF 설정 |
| Proxy | 10초 | Apache / Gateway |
| WebTop | 15초 | 단말 |
| L4 Idle | 120초 | L4 Profile |

### 산정 공식

```
동시요청자 = 전체사용자 × 동시요청률
TPS = 동시요청자 ÷ 목표응답시간(3초)
AP 대수 = ⌈TPS ÷ 250⌉ (A-A 배치)
Thread = AP당TPS × 1.2초 × 1.2
Pool = max(30, min(TPS×0.15×1.3, Thread×30%))
```

### 검증 시나리오

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

### 연관 챕터

| 영역 | 챕터 |
|------|------|
| 산정 | 8~11, 45 |
| 연결 | 7 |
| 설정 | 16~36, 37 |
| 검증 | 47~48, 50 |
| 운영 | 49~54 |

### CAP/ENV 연동

- 용량산정: `/oc/capacity.html` · `/api/oc/capacity/*`
- 환경설정: `/oc/env-002.html` · `/api/oc/env/*` · rule-check


---

[← 목차](./00-목차.md)
