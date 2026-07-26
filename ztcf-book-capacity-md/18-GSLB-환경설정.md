# 18. GSLB 환경설정

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


**GSLB** — TTL, Health, 센터 라우팅, 장애 전환.

## 원문 기반 본문

5. GSLB 설정 가이드

### 5.1 GSLB 기본 원칙GSLB는 센터 간 접근 경로를 결정하고, L4는 센터 내부 서버 분산을 담당한다.

GSLB는 장애 센터의 VIP를 응답하지 않아야 하며, 정상 센터 VIP만 반환해야 한다.

DNS TTL은 장애 전환성과 DNS 질의 부하 사이의 균형으로 30초를 기본값으로 둔다.

GSLB Health Check는 단순 ICMP가 아니라 서비스 포트 또는 상태 확인 URL을 기준으로 구성한다.


| 설정 항목 | 권장값 | 설정 예시 | 검증 방법 |
|-----------|--------|-----------|-----------|
| Wide IP / 서비스 도메인 | 업무 서비스 도메인 | mkt.nh.local → DC1/DC2 VIP | nslookup/dig 결과 확인 |
| Pool 구성 | 센터별 L4 VIP 등록 | DC1_L4_VIP, DC2_L4_VIP | 정상 센터만 응답 확인 |
| DNS TTL | 30초 | ttl=30 | 장애 시 재조회 전환시간 측정 |
| Load Balance Method | Priority 또는 Round Robin | Active-Active Round Robin | 센터별 유입 비율 확인 |
| Health Check | HTTPS 443 + URL | GET /health/l4 | 장애 VIP DNS 응답 제외 |
| Failure Policy | 정상 Pool만 반환 | return only available VIP | 강제 장애 테스트 |

GSLB 논리 예시Wide IP  : mkt.nsight.nh.localPool     : DC1_L4_VIP(10.10.10.100), DC2_L4_VIP(10.20.10.100)TTL      : 30sMonitor  : HTTPS 443 /health/l4Policy   : 정상 VIP만 DNS 응답, 장애 VIP 응답 제외

## 절별 상세

### 18.1 DNS TTL

본 절(**DNS TTL**)은 GSLB 영역에서 **센터 라우팅** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 유형 | TTL | 비고 |
|------|-----|------|
| Local | 1~5분 | Stampede 방지 |
| Redis | 5~30분 | fallback |

#### 설정 예시

```yaml
cache:
  ttl: 300s
  max-size: 10000
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

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: GSLB Profile

### 18.2 Health Check 주기

본 절(**Health Check 주기**)은 GSLB 영역에서 **센터 라우팅** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 계층 | URI | Interval |
|------|-----|----------|
| L4 | /actuator/health/l4 | 5s |
| GSLB | 센터 VIP | 10s |

#### 설정 예시

Fail 3회 → Member 제외.

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

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: GSLB Profile

### 18.3 Health Check Timeout

본 절(**Health Check Timeout**)은 GSLB 영역에서 **센터 라우팅** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 계층 | URI | Interval |
|------|-----|----------|
| L4 | /actuator/health/l4 | 5s |
| GSLB | 센터 VIP | 10s |

#### 설정 예시

Fail 3회 → Member 제외.

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

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: GSLB Profile

### 18.4 Fail Count

본 절(**Fail Count**)은 GSLB 영역에서 **센터 라우팅** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 센터 라우팅 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | GSLB Profile |
| 핵심 | TTL 30s |

#### 설정 예시

**설정 파일**: `GSLB Profile` · **핵심 항목**: TTL 30s

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

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: GSLB Profile

### 18.5 센터 분산비율

본 절(**센터 분산비율**)은 GSLB 영역에서 **센터 라우팅** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 센터 라우팅 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | GSLB Profile |
| 핵심 | TTL 30s |

#### 설정 예시

**설정 파일**: `GSLB Profile` · **핵심 항목**: TTL 30s

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

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: GSLB Profile

### 18.6 센터 장애 전환 정책

본 절(**센터 장애 전환 정책**)은 GSLB 영역에서 **센터 라우팅** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

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

### 18.7 복구 후 원복 정책

본 절(**복구 후 원복 정책**)은 GSLB 영역에서 **센터 라우팅** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 센터 라우팅 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | GSLB Profile |
| 핵심 | TTL 30s |

#### 설정 예시

**설정 파일**: `GSLB Profile` · **핵심 항목**: TTL 30s

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

### 18.8 GSLB 설정 검증

본 절(**GSLB 설정 검증**)은 GSLB 영역에서 **센터 라우팅** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 권고 |
|------|------|
| TTL | 30초 |
| Health | 센터 VIP |
| Failover | 자동 |

#### 설정 예시

센터 장애 시 WebTopSuite 재조회 정책.

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

## GSLB 환경설정 — 실무 요약

본 장은 **GSLB**(센터 라우팅)의 핵심을 23장(JVM)과 동일한 깊이로 요약합니다.

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

### GSLB 권고값 요약

| 항목 | 권고 | 검증 |
|------|------|------|
| DNS TTL | 30초 | 장애 전환 ≤60s |
| Health Check | HTTPS /health/l4 | VIP 제외 확인 |
| LB Method | Round Robin (A-A) | 센터별 유입 50:50 |
| Failure Policy | available VIP only | 강제 장애 테스트 |

### 센터 장애 시나리오

| 단계 | 동작 | 용량 관점 |
|------|------|----------|
| 1 | DC1 Health Fail | GSLB가 DC2로 라우팅 |
| 2 | TTL 만료 | 클라이언트 재조회 |
| 3 | DC2 AP | 잔여 TPS ≥ 720 |
| 4 | Sticky | 세션 재로그인 허용 |

### GSLB ↔ L4 ↔ AP 정합

- GSLB TTL(30s) < L4 Sticky(70m) — 센터 전환과 세션 정책 분리 이해
- Health URL은 Tomcat Readiness와 동일 엔드포인트 권장
- Wide IP 장애 시 **잔여 센터 AP·Pool·DB Session** 사전 검증

### 설정 예시

```
Wide IP: mkt.nh.local
Pool: DC1_L4_VIP, DC2_L4_VIP
Health: GET /actuator/health/l4 interval=5s timeout=2s
TTL: 30
```

### 검증 체크리스트

| # | 확인 |
|---|------|
| 1 | DC1 강제 Down 시 DNS가 DC2만 응답 |
| 2 | 전환 시간 ≤ 60초 (TTL+Health) |
| 3 | 잔여 AP로 720 TPS 합격 |
| 4 | Sticky 영향·재로그인 UX 합의 |

### 장애 전환 타임라인

| 시각 | 이벤트 | 확인 |
|------|--------|------|
| T+0 | DC1 Health Fail | GSLB Pool 제외 |
| T+30s | DNS TTL 만료 | nslookup 재확인 |
| T+60s | DC2 트래픽 100% | APM TPS·p95 |
| T+5m | 잔여 TPS 안정 | ≥720 합격 |


---

[← 목차](./00-목차.md)
