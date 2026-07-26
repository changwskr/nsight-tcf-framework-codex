# 08. 타임아웃 관리 아키텍처

| 항목 | 내용 |
|------|------|
| 문서 번호 | 08 |
| 제목 | Timeout Management Architecture |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [03-transaction.md](03-transaction.md), [07-DAO.md](07-DAO.md), [05-exception.md](05-exception.md) |
| 구현 모듈 | `*-service`, `tcf-om`, `tcf-core`, `tcf-ui` |
| 대상 | 프레임워크·업무·운영 개발자 |

---

## 1. 개요

NSIGHT TCF의 타임아웃은 단일 값이 아니라, **요청-트랜잭션-DB-연동-세션** 각 계층에 분산된 다층 구조로 관리된다.

| 계층 | 목적 |
|------|------|
| HTTP/온라인 거래 | 과도한 응답 지연 방지 |
| Spring 트랜잭션 | DB 작업 장기 점유 방지 |
| MyBatis SQL | 단일 쿼리 지연 차단 |
| DB 커넥션 풀(Hikari) | 연결 획득 대기 제한 |
| 원격 호출(OM→Batch) | 외부 서비스 장애 전파 차단 |
| 세션 | 유휴 세션 정리 |
| 운영 모니터링 | 지연 거래 관측·분석 |

---

## 2. 전체 구조

```text
Client
  │ POST /online
  ▼
TCF Transaction (STF → Handler → ETF)
  │
  ├─ Facade @Transactional(timeout=...)
  │    └─ Service → DAO → Mapper(SQL)
  │         └─ MyBatis default-statement-timeout
  │
  ├─ DataSource(Hikari connection-timeout)
  │
  └─ ETF 종료 시 ELAPSED_TIME_MS 기록 (TCF_TX_LOG)
          └─ OM Dashboard timeoutCount 집계
```

핵심은 **실행 제어 타임아웃**(실제로 작업을 끊는 값)과 **관측 타임아웃**(느린 거래를 집계하는 임계값)을 구분하는 것이다.

---

## 3. 타임아웃 레이어별 표준값

현재 코드/설정 기준 대표값:

| 레이어 | 설정 키/위치 | 기본값(예) | 의미 |
|--------|--------------|------------|------|
| 온라인 거래 기준 | `nsight.timeout.online-transaction-seconds` | `5` | 운영 화면 표시용 임계값 |
| DB 쿼리 기준 | `nsight.timeout.db-query-seconds` | `3` | DB 쿼리 임계값(설정 레벨) |
| 트랜잭션 기본 | `spring.transaction.default-timeout` | `5`초 | `@Transactional` 미지정 시 기본 |
| 트랜잭션 개별 | `@Transactional(timeout=...)` | `5/10/30`초 | 유스케이스별 경계 |
| MyBatis SQL | `mybatis.configuration.default-statement-timeout` | `3`초 | Statement 실행 제한 |
| Hikari 연결 | `spring.datasource.hikari.connection-timeout` | `3000`ms | 커넥션 획득 대기 제한 |
| Hikari 검증 | `validation-timeout` | `3000`ms | 커넥션 검증 제한 |
| HTTP 세션 | `server.servlet.session.timeout` | `60m` | 웹 세션 만료 |
| Spring Session | `spring.session.timeout` | `60m` | JDBC 세션 만료 |
| 원격 호출(OM→Batch) | `RestTemplateBuilder.setReadTimeout` | `30`초 | Batch API 응답 대기 |
| 원격 호출(OM→Batch) | `RestTemplateBuilder.setConnectTimeout` | `3`초 | Batch API 연결 대기 |

---

## 4. 트랜잭션 타임아웃 설계

### 4.1 Facade 중심 제어

타임아웃 경계는 Facade에서 선언한다. AOP(`@Transactional`) 동작: [32-AOP.md](32-AOP.md)

```java
@Transactional(readOnly = true, timeout = 5)   // 조회
@Transactional(timeout = 10)                   // 등록/수정/삭제
```

OM에서는 업무 특성에 따라 `10`초/`30`초도 사용한다(예: 배치 실행, 대량 조회).

### 4.2 권장 기준

| 처리 유형 | 권장 timeout |
|-----------|--------------|
| 단순 조회 | 5초 |
| 일반 CUD | 10초 |
| 배치 트리거/대기형 | 30초 (필요 시) |

### 4.3 실패 처리

트랜잭션 타임아웃 포함 예외는 TCF에서 `systemError` 또는 `businessFail` 경로로 표준 응답화된다.

- HTTP는 기본적으로 200 유지
- `result.resultCode = E0001`
- `errorCode`는 시스템/업무 코드로 매핑

---

## 5. DAO/SQL 타임아웃 설계

### 5.1 MyBatis statement timeout

`mybatis.configuration.default-statement-timeout: 3`초를 기본으로 둔다.

의미:

- 단일 SQL이 장기 점유하는 상황을 조기에 차단
- 상위 트랜잭션 timeout(5~10초)보다 짧게 배치해 DB 지연을 먼저 감지

### 5.2 DAO 설계 원칙

- DAO는 timeout 값을 임의로 하드코딩하지 않는다
- DAO는 Mapper 호출에 집중하고, timeout 정책은 설정/트랜잭션에서 통제
- 장시간 쿼리는 페이징/집계 분리로 구조 개선

---

## 6. 커넥션 풀 타임아웃

Hikari 설정(대표):

- `connection-timeout: 3000`
- `validation-timeout: 3000`
- `idle-timeout: 600000`
- `max-lifetime: 1800000`

설계 의도:

1. 연결 자체가 막히는 장애를 3초 내 감지
2. 트랜잭션/SQL timeout보다 하위 레이어에서 빠르게 실패
3. 장애 전파 시간을 줄여 스레드 고갈을 방지

---

## 7. 외부 연동 타임아웃 (OM → tcf-batch)

`OmBatchRemoteClient`는 명시적 HTTP timeout을 사용한다.

```java
this.restTemplate = builder
    .setConnectTimeout(Duration.ofSeconds(3))
    .setReadTimeout(Duration.ofSeconds(30))
    .build();
```

| 항목 | 값 | 의도 |
|------|----|------|
| connect timeout | 3초 | 대상 서버 다운/네트워크 단절 빠른 감지 |
| read timeout | 30초 | 배치 실행 응답 대기 상한 |

실패 시 `BusinessException("E-OM-BIZ-0003", "... 호출 실패")`로 표준화한다.

---

## 8. 카탈로그 기반 타임아웃 메타

`OM_SERVICE_CATALOG.TIMEOUT_SEC` 컬럼으로 거래별 timeout 메타를 관리한다.

| 컬럼 | 의미 |
|------|------|
| `TIMEOUT_SEC` | 거래 카탈로그 기준 timeout(초) |
| 기본값 | 5 |

현재 상태:

- `OmServiceCatalogService`에서 `timeoutSec`을 CRUD
- OM Admin에서 조회/수정 가능
- 실행 엔진(`@Transactional`)에 자동 바인딩되는 구조는 아직 제한적

즉, **운영 메타 관리**는 존재하며, 일부는 **관측/정책 기준값**으로 사용한다.

---

## 9. 관측(Observability) 타임아웃

### 9.1 거래 로그 기반 지연 분석

`TCF_TX_LOG.ELAPSED_TIME_MS`를 기준으로 OM에서 timeout 건수를 집계한다.

예시 SQL 기준:

```sql
SUM(CASE WHEN ELAPSED_TIME_MS >= 5000 THEN 1 ELSE 0 END) AS timeoutCount
```

### 9.2 실행 제어 vs 관측 임계값

| 구분 | 값 예 | 용도 |
|------|-------|------|
| 실행 제어 | 트랜잭션 5초, SQL 3초 | 실제 중단/롤백 유도 |
| 관측 임계값 | `ELAPSED_TIME_MS >= 5000` | 느린 거래 통계 표시 |

운영 화면의 Timeout 카운트는 “실패 건수”가 아니라 “지연 임계 초과 건수”임을 명확히 구분한다.

---

## 10. 세션 타임아웃

세션은 온라인 거래 타임아웃과 별개로 관리한다.

| 설정 | 값 예 | 설명 |
|------|------|------|
| `server.servlet.session.timeout` | 60m | 서블릿 세션 만료 |
| `spring.session.timeout` | 60m | JDBC 세션 만료 |

OM 포털은 세션 기반 인증을 사용하므로, 세션 타임아웃은 사용자 경험/보안과 직결된다.

---

## 11. 표준 운영 정책 (권장)

### 11.1 기본 정책

1. SQL timeout < 트랜잭션 timeout < 원격 read timeout
2. 조회와 CUD의 timeout 분리
3. 긴 작업은 동기 온라인 거래가 아니라 배치/비동기로 전환
4. timeout 값 변경 시 OM 카탈로그·문서 동시 갱신

### 11.2 권장 베이스라인

| 항목 | 권장값 |
|------|--------|
| `default-statement-timeout` | 3초 |
| 조회 트랜잭션 | 5초 |
| CUD 트랜잭션 | 10초 |
| 외부 연동 read timeout | 30초 |
| timeoutCount 임계 | 5초(환경별 조정 가능) |

---

## 12. 장애 시나리오와 대응

| 시나리오 | 감지 지점 | 대응 |
|----------|-----------|------|
| DB 잠금으로 쿼리 지연 | MyBatis statement timeout | 쿼리 튜닝, 인덱스, 트랜잭션 범위 축소 |
| 커넥션 풀 고갈 | Hikari connection timeout | 풀 크기/슬로우쿼리 개선, 트래픽 제어 |
| 배치 서버 다운 | OM Remote connect timeout | Batch 상태 확인, fallback 메시지 제공 |
| 특정 거래만 반복 지연 | `TCF_TX_LOG` timeoutCount 증가 | `serviceId`별 병목 분석, 카탈로그 timeout 재평가 |

---

## 13. 구현 체크리스트

- [ ] Facade 메서드마다 timeout을 명시했는가
- [ ] `spring.transaction.default-timeout`와 충돌하지 않는가
- [ ] `mybatis.default-statement-timeout`가 환경별 적정한가
- [ ] 외부 HTTP 호출에 connect/read timeout이 명시됐는가
- [ ] timeout 관측 기준(5초)이 운영 정책과 일치하는가
- [ ] OM 카탈로그(`TIMEOUT_SEC`)와 코드 정책이 어긋나지 않는가

---

## 14. 참고 소스

| 파일 | 설명 |
|------|------|
| `tcf-om/src/main/resources/application.yml` | timeout 관련 통합 설정 |
| `sv-service/src/main/resources/application.yml` | 업무 모듈 기본 timeout 설정 |
| `tcf-om/src/main/java/com/nh/nsight/marketing/om/support/OmBatchRemoteClient.java` | 외부 연동 timeout |
| `tcf-om/src/main/resources/schema.sql` | `OM_SERVICE_CATALOG.TIMEOUT_SEC` |
| `tcf-om/src/main/resources/mapper/om/OmOperationMapper.xml` | timeoutCount 집계 SQL |
| `docs/architecture/03-transaction.md` | 트랜잭션 경계와 ETF 처리 |

---

## 15. 변경 이력

| 일자 | 변경 내용 |
|------|-----------|
| 2026-06 | 최초 작성 — 다층 타임아웃 정책 및 운영 기준 정리 |
