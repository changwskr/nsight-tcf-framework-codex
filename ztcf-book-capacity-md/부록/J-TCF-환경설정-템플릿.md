# 부록 J. TCF 환경설정 템플릿

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

아래처럼 SPRING 영역 설정 기준표로 정리하면 됩니다.


| 영역 | 설정 항목 | 권고값 | 설정 위치 | 설명 |
|------|-----------|--------|-----------|------|
| SPRING | session.timeout | 60m | application.yml | 유휴 세션 |
| SPRING | cookie.http-only | true | application.yml | XSS 방지 |
| SPRING | cookie.secure | true | application.yml | HTTPS |
| SPRING | Transaction Timeout | 4~5초 |

```java
@Transactional | 업무 한도 |
| SPRING | Retry | 기본 금지 | Resilience4j | 부하 증폭 방지 |
| SPRING | Circuit Breaker | 필수 | Resilience4j | 연계 격리 |
| SPRING | GUID/MDC | 필수 | Filter/Logback | 추적 |
| SPRING | CruzAPIM Connect | 3s | WebClient | 연결 |
| SPRING | CruzAPIM Read | 5s | WebClient | 응답 |
```


application.yml 예시server:  servlet:    session:      timeout: 60m      cookie:        http-only: true        secure: true        same-site: Laxspring:  transaction:    default-timeout: 5nsight:  cruzapim:    connect-timeout: 2000    read-timeout: 5000코드 적용 예시

```java
@Transactional(readOnly = true, timeout = 5)public CustomerView getCustomerView(String customerId) {    return singleViewService.findCustomer
```


View(customerId);}핵심은 세션은 길게 유지할 수 있어도, 거래 트랜잭션과 연계 Timeout은 짧게 끊는 것입니다.

즉, DB Query Timeout < Transaction Timeout < CruzAPIM/Web Timeout < Client Timeout 순서가 맞아야 합니다.

> **용도**: TCF 설정 · **연관 본문**: 25장

## TCF 환경설정 템플릿 — 실무 템플릿

본 부록은 **ServiceId·GUID** 영역의 표준 템플릿입니다. 상단 **NSIGHT 1차 표준 전제**와 함께 사용합니다.

### TCF 환경설정 템플릿

```yaml
nsight:
  tcf:
    service-timeout:
      default: 5
      inquiry: 4
      register: 5
    guid:
      required: true
      mdc-key: GUID
    idempotency:
      enabled: true
      ttl: 300
    audit:
      enabled: true
spring:
  transaction:
    default-timeout: 5
nsight:
  cruzapim:
    connect-timeout: 2000
    read-timeout: 5000
```

| 영역 | 항목 | 권고 | 위치 |
|------|------|------|------|
| TCF | ServiceId Timeout | 4~5초 | TCF 설정 |
| TCF | GUID/MDC | 필수 | Filter |
| TCF | Idempotency | 활성 | TCF |
| Spring | Tx Timeout | 5초 | @Transactional |
| 연계 | CruzAPIM Connect | 3s | WebClient |
| 연계 | CruzAPIM Read | 5s | WebClient |
| Resilience | CB·Bulkhead | 필수 | Resilience4j |
| Retry | 동기거래 | **금지** | — |

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
| 25 | ServiceId·GUID 상세 |

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

### TCF 코드 적용 예시

```java
@Transactional(readOnly = true, timeout = 5)
public CustomerView getCustomerView(String customerId) {
    return singleViewService.findCustomerView(customerId);
}
```

Timeout 계층: SQL(3s) < Tx(5s) < CruzAPIM(5s) < Proxy(10s) < Web(15s)


## 절별 상세

### J.1 ServiceId Timeout

본 절은 **부록 J** — **ServiceId Timeout** (TCF 환경설정 템플릿) NSIGHT 1차 표준 적용 기준입니다.

| ServiceId | Timeout |
|-----------|--------|
| 조회 | 4s |
| 등록 | 5s |
| 연계 | 5s |

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

### J.2 GUID·MDC

본 절은 **부록 J** — **GUID·MDC** (TCF 환경설정 템플릿) NSIGHT 1차 표준 적용 기준입니다.

GUID·TraceId **MDC 필수**. Access Log·APM·거래로그 상관관계.

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

### J.3 Idempotency

본 절은 **부록 J** — **Idempotency** (TCF 환경설정 템플릿) NSIGHT 1차 표준 적용 기준입니다.

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

### J.4 CruzAPIM

본 절은 **부록 J** — **CruzAPIM** (TCF 환경설정 템플릿) NSIGHT 1차 표준 적용 기준입니다.

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

### J.5 검증

본 절은 **부록 J** — **검증** (TCF 환경설정 템플릿) NSIGHT 1차 표준 적용 기준입니다.

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
