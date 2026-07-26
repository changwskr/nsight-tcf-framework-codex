# 17. OS 및 VM 환경설정

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


**OS/VM** — ulimit, TCP, 로그·Dump 경로.

## 원문 기반 본문

4. VM 장애 시 서비스 영향도8CORE VM 8대 구성에서는 VM 1대 장애 시 전체 처리 용량의 12.5%만 손실된다. 반면 16CORE VM 4대 구성에서는 VM 1대 장애만으로 전체 용량의 25%가 사라진다. 피크 시간대에는 25% 용량 손실이 남은 서버의 Thread, DB Pool, CPU 사용률을 급격히 끌어올려 연쇄 장애로 이어질 수 있다.

항목8CORE × 8대16CORE × 4대운영 해석VM 1대 장애 비율12.5%25%16CORE는 장애 영향이 2배장애 시 잔여 VM7대3대8CORE가 흡수 여력 큼부하 재분산완만하게 분산소수 VM에 집중8CORE 안정Cascading Failure 위험상대적으로 낮음상대적으로 높음금융 온라인은 8CORE 유리

## 5. 백엔드 DB(RDW) 보호 및 Session 관리정보계 온라인 AP의 핵심 병목은 AP CPU만이 아니다. 실제 운영에서는 RDW SQL 시간, Hikari Pool 사용률, DB Session 총량이 서비스 안정성에 큰 영향을 준다. 8CORE 구조는 DB Connection Pool이 작은 단위로 분산되어 특정 VM의 폭주가 전체 RDW를 급격히 압박하지 않도록 제어하기 쉽다.

항목8CORE × 8대16CORE × 4대판단Pool 설계VM당 일반 50 / SingleView 60VM당 일반 80~100 / SingleView 100~12016CORE는 VM당 Pool 큼DB Session 증가 속도작은 단위로 완만소수 VM에서 급격8CORE가 RDW 보호 유리Pool 격리업무별·VM별 통제 용이한 VM 내 집중 가능8CORE 유리장애 시 DB 압력분산되어 완화잔여 VM Pool에 집중8CORE 유리따라서 온라인 AP의 DB Pool은 Thread와 1:1로 맞추는 것이 아니라, RDW를 보호하기 위한 통제 장치로 설계해야 한다. 특히 SingleView는 고객정보, 거래요약, 캠페인 이력 등 다중 조회가 발생할 수 있으므로 Pool을 크게 여는 방식보다 SQL 품질, Query Timeout, Pool 사용률 감시가 더 중요하다.

## 절별 상세

### 17.1 vCPU와 Memory 할당

본 절(**vCPU와 Memory 할당**)은 OS·VM 영역에서 **OS 튜닝** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 일반 AP | SV AP | VM |
|------|---------|-------|----|
| Heap (Xms=Xmx) | 12GB | 14GB | 8C/32G |
| -Xss | 512KB | 512KB | 500 Thread 기준 |
| GC | G1GC | G1GC | Pause 목표 200ms |
| Metaspace Max | 1GB | 1GB | 8C/32G |

#### 설정 예시

**설정 파일**: `sysctl·limits.conf·systemd` · **핵심 항목**: ulimit·TZ

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

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: sysctl·limits.conf·systemd

### 17.2 CPU Overcommit 기준

본 절(**CPU Overcommit 기준**)은 OS·VM 영역에서 **OS 튜닝** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | OS 튜닝 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | sysctl·limits.conf·systemd |
| 핵심 | ulimit·TZ |

#### 설정 예시

**설정 파일**: `sysctl·limits.conf·systemd` · **핵심 항목**: ulimit·TZ

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

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: sysctl·limits.conf·systemd

### 17.3 Memory Overcommit 기준

본 절(**Memory Overcommit 기준**)은 OS·VM 영역에서 **OS 튜닝** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 일반 AP | SV AP | VM |
|------|---------|-------|----|
| Heap (Xms=Xmx) | 12GB | 14GB | 8C/32G |
| -Xss | 512KB | 512KB | 500 Thread 기준 |
| GC | G1GC | G1GC | Pause 목표 200ms |
| Metaspace Max | 1GB | 1GB | 8C/32G |

#### 설정 예시

**설정 파일**: `sysctl·limits.conf·systemd` · **핵심 항목**: ulimit·TZ

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

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: sysctl·limits.conf·systemd

### 17.4 Swap 설정

본 절(**Swap 설정**)은 OS·VM 영역에서 **OS 튜닝** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | OS 튜닝 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | sysctl·limits.conf·systemd |
| 핵심 | ulimit·TZ |

#### 설정 예시

**설정 파일**: `sysctl·limits.conf·systemd` · **핵심 항목**: ulimit·TZ

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

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: sysctl·limits.conf·systemd

### 17.5 File Descriptor

본 절(**File Descriptor**)은 OS·VM 영역에서 **OS 튜닝** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | OS 튜닝 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | sysctl·limits.conf·systemd |
| 핵심 | ulimit·TZ |

#### 설정 예시

**설정 파일**: `sysctl·limits.conf·systemd` · **핵심 항목**: ulimit·TZ

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

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: sysctl·limits.conf·systemd

### 17.6 Process·Thread 제한

본 절(**Process·Thread 제한**)은 OS·VM 영역에서 **OS 튜닝** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 8C/32G 권고 | 산식/근거 |
|------|-------------|----------|
| maxThreads | 400~500 | 250×1.2×1.2≈360 |
| minSpareThreads | 100 | 피크 진입 지연 완화 |
| acceptCount | 300~500 | Queue 병목 은폐 금지 |
| maxConnections | 10,000 | L4 KeepAlive 정합 |

#### 설정 예시

```xml
<Connector port="8080" protocol="org.apache.coyote.http11.Http11NioProtocol"
  maxThreads="500" minSpareThreads="100" acceptCount="500"
  maxConnections="10000" connectionTimeout="8000"
  keepAliveTimeout="120000" maxKeepAliveRequests="200" />
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

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: sysctl·limits.conf·systemd

### 17.7 TCP Backlog

본 절(**TCP Backlog**)은 OS·VM 영역에서 **OS 튜닝** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | OS 튜닝 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | sysctl·limits.conf·systemd |
| 핵심 | ulimit·TZ |

#### 설정 예시

**설정 파일**: `sysctl·limits.conf·systemd` · **핵심 항목**: ulimit·TZ

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

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: sysctl·limits.conf·systemd

### 17.8 TCP Keepalive

본 절(**TCP Keepalive**)은 OS·VM 영역에서 **OS 튜닝** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | OS 튜닝 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | sysctl·limits.conf·systemd |
| 핵심 | ulimit·TZ |

#### 설정 예시

**설정 파일**: `sysctl·limits.conf·systemd` · **핵심 항목**: ulimit·TZ

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

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: sysctl·limits.conf·systemd

### 17.9 Ephemeral Port 범위

본 절(**Ephemeral Port 범위**)은 OS·VM 영역에서 **OS 튜닝** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | OS 튜닝 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | sysctl·limits.conf·systemd |
| 핵심 | ulimit·TZ |

#### 설정 예시

**설정 파일**: `sysctl·limits.conf·systemd` · **핵심 항목**: ulimit·TZ

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

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: sysctl·limits.conf·systemd

### 17.10 시간동기화와 Timezone

본 절(**시간동기화와 Timezone**)은 OS·VM 영역에서 **OS 튜닝** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | OS 튜닝 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | sysctl·limits.conf·systemd |
| 핵심 | ulimit·TZ |

#### 설정 예시

**설정 파일**: `sysctl·limits.conf·systemd` · **핵심 항목**: ulimit·TZ

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

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: sysctl·limits.conf·systemd

### 17.11 로그·Dump 디렉터리

본 절(**로그·Dump 디렉터리**)은 OS·VM 영역에서 **OS 튜닝** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | OS 튜닝 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | sysctl·limits.conf·systemd |
| 핵심 | ulimit·TZ |

#### 설정 예시

**설정 파일**: `sysctl·limits.conf·systemd` · **핵심 항목**: ulimit·TZ

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

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: sysctl·limits.conf·systemd

### 17.12 OS 설정 검증 명령

본 절(**OS 설정 검증 명령**)은 OS·VM 영역에서 **OS 튜닝** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | OS 튜닝 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | sysctl·limits.conf·systemd |
| 핵심 | ulimit·TZ |

#### 설정 예시

**설정 파일**: `sysctl·limits.conf·systemd` · **핵심 항목**: ulimit·TZ

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
