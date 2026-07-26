# 13. 네트워크 및 Connection 용량산정

> 제2부. 업무부하 및 시스템 용량산정

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


**네트워크·Connection** — L4·Apache·Tomcat maxConnections 정합.

## 원문 기반 본문

8. 5단계: DB Pool 총량 산정DB Pool 총량은 AP 전체가 DB에 동시에 열 수 있는 최대 Connection 수이다. 이 값은 AP 성능만 보고 크게 잡으면 DB Session 폭증을 유발하고, 너무 작게 잡으면 Hikari Connection Timeout과 AP Thread 대기를 유발한다.

DB Pool 총량 = AP 수량 x AP당 Hikari maximumPoolSizeAP 유형권장 maximumPoolSize권장 minimumIdle적용 DB비고일반 마케팅 AP40~5010~15RDW / 업무DB연계 대기시간이 많으면

40부터 시작Single View AP50~6015~20RDWRDW 고빈도 조회 기준실시간 처리 AP20~405~10업무DB / 로그DB비동기·이벤트 중심EBM 엔진 AP30~5010~15RDW / 업무DBRule 판단과 조회량에 따라 보정Batch/ETL별도 산정별도 산정RDW / ADW온라인 AP Pool과 분리Hikari connectionTimeout은 SQL 실행시간이 아니라 Connection Pool에서 DB Connection을 빌리기 위해 기다리는 시간이다. SQL 실행시간은 MyBatis statement timeout 또는 JDBC query timeout으로 별도 통제한다.

### 8.1 DB Pool 산정 예시 - 일반 AP Pool 50 기준시나리오목표 TPS센터 장애 감당 APAP당 Pool전체 Pool 총량센터 장애 후 잔여 Pool기본 운영360 TPS4대(센터당 2)50200100피크 설계720 TPS6대(센터당 3)50300150스트레스1080 TPS10대(센터당 5)5050025

### 08.2 DB Pool 산정 예시 - Single View AP Pool 60 기준시나리오목표 TPS센터 장애 감당 APAP당 Pool전체 Pool 총량센터 장애 후 잔여 Pool기본 운영360 TPS4대(센터당 2)60240120피크 설계720 TPS6대(센터당 3)60360180스트레스1080 TPS10대(센터당 5)606003009. 6단계: DB Session 총량 산정DB Session 총량은 온라인 AP의 Hikari Pool만 의미하지 않는다. RDW와 ADW는 역할이 다르므로 각각 별도로 산정해야 하며, 온라인 AP, 배치, BI, ETL, 운영자, DBA, 모니터링 세션을 모두 포함해야 한다.

DB Session 총량 = Σ(AP 수 x AP당 Pool x DataSource 수) + Batch Session + BI Session + ETL Session + 운영/관리 Session + 여유율구분RDW 포함 여부ADW 포함 여부산

정 방식주의점마케팅 AP포함예외적 포함AP 수 x RDW PoolADW 직접 분석 조회 금지Single View AP포함미포함 원칙AP 수 x RDW Pool고빈도 조회로 별도 관리BI Portal미포함 원칙포함BI 동시조회 x Query 특성ADW 전용 원칙ETL/DataStage포함포함Job 병렬도 x DB 세션온라인 시간대 충돌 방지Kafka/CDC 처리일부 포함일부 포함Consumer/Writer 병렬도 기준Lag와 재처리 고려운영/관리/모니터링포함포함고정 여유 세션DBA, APM, Health Check 포함

### 9.1 RDW Session 총량 예시아래는 피크 설계 720 TPS 기준으로 Single View AP를 센터당 3대, 총 6대로 구성하고 AP당 RDW Pool 60을 적용한 예시이다.

항목산식Session 수설명Single View AP RDW Pool6대 x 60360센터 장애 감당 최소 구성일반 마케팅 AP RDW Pool4대 x 50200일반 업무 별도 AP 가정운영/모니터링 Session고정값30Health Check, APM, 운영 조회DBA/관리 Session고정값20운영 관리용온라인 RDW 소계360 + 200 + 30 + 20610배치/ETL 제외여유율 20%610 x 20%122피크 변동성 반영권장 RDW Session 확보610 + 122732온라인 기준 최소 권장이 예시는 산정 방식 설명용이다. 실제 최종값은 업무 서버 분리 수량, AP별 DataSource 수, 배치/ETL 병렬도, BI 접속 방식, DBMS max sessions 정책을 반영하여 확정해야 한다.

---

## 3. 트래픽 및 용량 산정 기준

본 가이드는 6,000개 지점, 지점당 사용자 6명, 전체 사용자 36,000명을 기준으로 한다. 세션 수와 TPS는 분리한다. 전체 사용자는 로그인 세션 또는 잠재 사용자 규모이며, L4 처리량은 실제 동시 요청자와 TPS를 기준으로 산정한다.

구분값L4 설계 의미전체 지점 수6,000개접속 지역·권역 분산 및 피크 시간대 고려지점당 사용자6명지점 단말 기반 동시 로그인 규모 산정전체 사용자36,000명세션 및 인증 기반 최대 사용자 규모여유율 포함 세션43,000~47,000 세션Persistence Table, 세션 유지, 모니터링 기준일반 기준600 TPS일상 피크 운영 기준피크 기준1,200 TPS업무 집중 시간 및 센터 장애 감당 기준스트레스 기준1,800 TPS성능시험 및 한계 검증 기준목표 응답시간p95 3초 이하L4는 업무 Timeout보다 길고, 장애 감지는 짧게

> **주의**: L4의 초당 요청 처리량은 1,200 TPS만 보지 말고 KeepAlive 연결 수, 동시 연결 수, 세션 Persistence Table 크기, Health Check 대상 수, SSL 처리 위치까지 함께 봐야 한다.

## 절별 상세

### 13.1 동시 Connection

본 절(**동시 Connection**)은 네트워크 영역에서 **Connection** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

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

**설정 파일**: `L4·Apache·Tomcat` · **핵심 항목**: maxConnections

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

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: L4·Apache·Tomcat

### 13.2 HTTP 요청·응답 크기

본 절(**HTTP 요청·응답 크기**)은 네트워크 영역에서 **Connection** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | Connection 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | L4·Apache·Tomcat |
| 핵심 | maxConnections |

#### 설정 예시

**설정 파일**: `L4·Apache·Tomcat` · **핵심 항목**: maxConnections

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

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: L4·Apache·Tomcat

### 13.3 Keep-Alive Connection

본 절(**Keep-Alive Connection**)은 네트워크 영역에서 **Connection** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | Connection 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | L4·Apache·Tomcat |
| 핵심 | maxConnections |

#### 설정 예시

**설정 파일**: `L4·Apache·Tomcat` · **핵심 항목**: maxConnections

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

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: L4·Apache·Tomcat

### 13.4 L4 Connection

본 절(**L4 Connection**)은 네트워크 영역에서 **Connection** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | Connection 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | L4·Apache·Tomcat |
| 핵심 | maxConnections |

#### 설정 예시

| L4 항목 | 권고 |
|--------|------|
| LB Method | Round Robin |
| Sticky | JSESSIONID, 70분 |
| Health | GET /actuator/health, 5s/2s/Fail3 |
| Idle Timeout | 120초 |
| Connect Timeout | 2~3초 |

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

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: L4·Apache·Tomcat

### 13.5 WEB Proxy Connection

본 절(**WEB Proxy Connection**)은 네트워크 영역에서 **Connection** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | Connection 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | L4·Apache·Tomcat |
| 핵심 | maxConnections |

#### 설정 예시

```apache
ProxyTimeout 10
ProxyConnectTimeout 3
KeepAlive On
KeepAliveTimeout 120
RequestHeader set X-GUID %{GUID}e
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

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: L4·Apache·Tomcat

### 13.6 Gateway Connection

본 절(**Gateway Connection**)은 네트워크 영역에서 **Connection** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | Connection 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | L4·Apache·Tomcat |
| 핵심 | maxConnections |

#### 설정 예시

**설정 파일**: `L4·Apache·Tomcat` · **핵심 항목**: maxConnections

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

### 13.7 DB Connection

본 절(**DB Connection**)은 네트워크 영역에서 **Connection** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | Connection 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | L4·Apache·Tomcat |
| 핵심 | maxConnections |

#### 설정 예시

**설정 파일**: `L4·Apache·Tomcat` · **핵심 항목**: maxConnections

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

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: L4·Apache·Tomcat

### 13.8 외부 API Connection

본 절(**외부 API Connection**)은 네트워크 영역에서 **Connection** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | Connection 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | L4·Apache·Tomcat |
| 핵심 | maxConnections |

#### 설정 예시

**설정 파일**: `L4·Apache·Tomcat` · **핵심 항목**: maxConnections

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

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: L4·Apache·Tomcat

### 13.9 파일 전송 대역폭

본 절(**파일 전송 대역폭**)은 네트워크 영역에서 **Connection** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 권고 |
|------|------|
| max-file-size | 업무별 10~50MB |
| 대용량 | 전용 서버 |

#### 설정 예시

```yaml
spring.servlet.multipart.max-file-size: 10MB
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

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: L4·Apache·Tomcat

### 13.10 센터 장애 시 네트워크 수용량

본 절(**센터 장애 시 네트워크 수용량**)은 네트워크 영역에서 **Connection** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| TPS | AP 권장 | 잔여(1센터 Down) |
|-----|----------|------------------|
| 720 | 8대 | 4대×250=1000 |

#### 설정 예시

잔여 TPS ≥ **720** 필수.

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

장애 분석: GUID E2E Trace → Thread Dump → Hikari Pending → SQL p95 → GC Log 순.

## 네트워크 및 Connection 용량산정 — 실무 요약

본 장은 **네트워크**(Connection)의 핵심을 23장(JVM)과 동일한 깊이로 요약합니다.

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
