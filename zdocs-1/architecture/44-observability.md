# 44. 관측성(Observability) 설계서

| 항목 | 내용 |
|------|------|
| 문서 번호 | 44 |
| 제목 | Observability Design (Log · Metric · Trace) |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [09-transaction log.md](09-transaction%20log.md), [37-transaction-log.md](37-transaction-log.md), [33-TCF.md](33-TCF.md), [43-security-operations.md](43-security-operations.md) |
| 상세 매뉴얼 | [34-로그-작성-기준.md](../../znsight-man/34-로그-작성-기준.md), [35-거래로그-감사로그-기준.md](../../znsight-man/35-거래로그-감사로그-기준.md) |
| 구현 모듈 | `tcf-core`, `tcf-web`, `tcf-gateway`, `tcf-eai`, `tcf-batch`, `tcf-om` |
| 대상 | 개발·운영·SRE 담당자 |

---

## 1. 문서 목적

NSIGHT TCF에서 **관측성**은 온라인 거래의 시작부터 종료까지를 **동일 상관키**로 추적하고, 장애·성능·감사 요구에 대응할 수 있게 하는 표준이다.

| 축 | NSIGHT TCF에서의 의미 |
|----|------------------------|
| **Log** | 파일·DB 거래로그, 감사로그, 구간별 DEBUG |
| **Metric** | 거래 처리시간, Actuator·배치 수집 지표 |
| **Trace** | GUID / TraceId / serviceId 기반 요청 상관 (분산 Trace ID 표준은 §8 로드맵) |

핵심 문장:

> 로그는 많이 남기는 것이 아니라, **장애 시 원인을 찾을 수 있게** 남긴다. 모든 운영 로그는 **GUID·TraceId·serviceId**로 연결한다.

---

## 2. 상관키(Correlation) 표준

### 2.1 식별자 정의

| 키 | 생성·전파 | 용도 |
|----|-----------|------|
| `guid` | STF `GuidGenerator` (미입력 시) | 거래 단위 ID, `TCF_TX_LOG` PK 성격 |
| `traceId` | 요청 Header 또는 guid 복사 | 동일 사용자 흐름·연동 체인 |
| `serviceId` | 표준 전문 Header | 업무·Handler 식별 |
| `transactionCode` | 표준 전문 Header | 거래코드·Timeout·통제 |
| `userId` / `branchId` / `channelId` | Header (JWT claim과 정합성 검증) | 사용자·채널 추적 |
| `jti` | JWT (있을 때 STF MDC) | 토큰 단위 감사·보안 추적 |

### 2.2 MDC 매핑 (Logback)

`logback-nsight-base.xml` 패턴:

```text
guid=%X{guid} traceId=%X{traceId} serviceId=%X{serviceId} userId=%X{userId} branchId=%X{branchId}
```

| 시점 | 설정 위치 |
|------|-----------|
| STF `preProcess` | `guid`, `traceId`, `serviceId`, `userId`, `branchId` |
| JWT 2차 검증 후 | `jti` (선택) |
| 요청 종료 | `GuidMdcCleanupFilter` → `MDC.clear()` |

### 2.3 서비스 간 전파

```text
[호출 WAR]  TransactionContext.header
      │  HeaderPropagationHelper (tcf-eai)
      ▼
[대상 WAR]  StandardRequest.header.guid / traceId 동일 유지
```

EAI 로그: `[EAI] CALL start caller={} target={} url={} guid={}`

Gateway 프록시: 요청/응답 Body에서 `guid`·`traceId` 파싱 → `TCF_GATEWAY_TX_LOG` 기록.

---

## 3. 로그 아키텍처

### 3.1 로그 유형

| 유형 | Logger / 채널 | 저장소 | 목적 |
|------|---------------|--------|------|
| Application | root / `com.nh.nsight.*` | `nsight-app.log` | 일반 동작·오류 |
| Transaction | `transaction.log` | 파일 + `TCF_TX_LOG` | 거래 요약 |
| Audit | `audit.log` | 파일 + `OM_AUDIT_LOG` | 감사·관리 변경 |
| TCF Console | `tcf.console` | `tcf-console.log` | UTF-8 콘솔 전용 (`TcfConsoleLog`) |
| Gateway | `GatewayProxyTrace` / `[GW-*]` | Gateway 로그 | 프록시·JWT 인증 |
| JWT 디버그 | `******* [GW-JWT]` 등 | stdout | 로컬·통합 디버그 (prod는 레벨 제한) |

### 3.2 거래 파이프라인 로그

```text
STF.preProcess()
  │ TransactionLogService → TX_START (guid, traceId, serviceId, …)
  ▼
Handler / Facade / Service
  │ (업무·SQL·연동 로그 — MDC 자동 포함)
  ▼
ETF.postProcess()
  │ TX_END (resultCode, errorCode, elapsedMs)
  │ AuditLogService (필요 시)
  │ TransactionMetricService (DEBUG TCF_METRIC)
  ▼
TransactionLogRepository → TCF_TX_LOG (별도 DS, 실패 시 warn만)
```

**TX_START / TX_END** 형식 (`TransactionLogService`):

```text
TX_START guid={} traceId={} serviceId={} txCode={} userId={} branchId={} channelId={}
TX_END   guid={} traceId={} serviceId={} resultCode={} errorCode={} elapsedMs={}
```

### 3.3 로그 작성 규칙 (요약)

| 규칙 | 내용 |
|------|------|
| 필수 포함 | `guid`, `traceId`, `serviceId` (운영 로그) |
| 금지 | 토큰 원문, 비밀번호, 주민번호, 계좌번호, SQL 바인드 전체 |
| 레벨 | 운영 INFO, 상세 DEBUG는 dev/local |
| 정리 | 요청 스레드 종료 시 `MDC.clear()` 필수 |
| 오류 | `BusinessException` → `resultCode`/`errorCode` in TX_END |

상세: [34-로그-작성-기준.md](../../znsight-man/34-로그-작성-기준.md)

---

## 4. DB 거래로그·감사

### 4.1 `TCF_TX_LOG` (요약)

| 컬럼군 | 예 |
|--------|-----|
| 식별 | `GUID`, `TRACE_ID`, `SERVICE_ID`, `TRANSACTION_CODE` |
| 사용자 | `USER_ID`, `BRANCH_ID`, `CHANNEL_ID` |
| 결과 | `RESULT_CODE`, `ERROR_CODE`, `ELAPSED_MS` |
| 시간 | `START_TIME`, `END_TIME` |

- INSERT는 업무 TX와 **분리** (autocommit / 별도 DataSource)
- 실패 시 거래 롤백에 영향 없음 — `warn` 로그만

상세: [09-transaction log.md](09-transaction%20log.md), [37-transaction-log.md](37-transaction-log.md)

### 4.2 OM 조회

| 화면 | 용도 |
|------|------|
| OM 거래로그 | `guid` / `traceId` / `serviceId` / 기간 검색 |
| OM 감사로그 | 관리자 변경·권한·다운로드 |

Gateway 자체 로그: `TCF_GATEWAY_TX_LOG` (프록시 구간).

---

## 5. 메트릭(Metric)

### 5.1 현재 구현

| 구성요소 | 내용 |
|----------|------|
| `TransactionMetricService` | `TCF_METRIC serviceId resultCode elapsedMs` (DEBUG) |
| Spring Boot Actuator | `tcf-batch` 세션·Actuator metric 수집 |
| OM 대시보드 | `OM_*_STATUS` 스냅샷 (배치 수집) |

운영에서 **즉시 알람**에 쓰려면 Actuator + Prometheus/Micrometer 연동이 필요하다 (§8).

### 5.2 권장 수집 지표

| 지표 | 태그 | 용도 |
|------|------|------|
| `tcf.transaction.count` | serviceId, resultCode | 처리량 |
| `tcf.transaction.duration` | serviceId | p95 지연 |
| `tcf.transaction.error` | errorCode | 오류율 |
| `tcf.timeout.count` | serviceId | Timeout |
| `tcf.gateway.proxy.duration` | businessCode | Gateway 지연 |
| `jwt.validation.failure` | layer(gw/web) | JWT 실패 |

### 5.3 성능 구간 분리 (설계 목표)

| 구간 | 측정 위치 |
|------|-----------|
| AP 처리 | `TransactionContext.elapsedMillis()` |
| DB | MyBatis interceptor / JDBC (정책 문서화) |
| 연동 | `[EAI] CALL … elapsedMs=` |

---

## 6. 장애 분석 플레이북

### 6.1 사용자 신고 → 추적 순서

| 단계 | 조치 |
|------|------|
| 1 | 신고 시각·`serviceId`·`userId` 수집 |
| 2 | OM 거래로그 또는 `TCF_TX_LOG`에서 `guid` 확보 |
| 3 | 동일 `traceId`로 연동·후속 거래 검색 |
| 4 | 해당 `guid`로 애플리케이션 로그 grep (`nsight-app.log`) |
| 5 | Gateway 경유 시 `TCF_GATEWAY_TX_LOG` / `[GW-AUTH]` 로그 확인 |
| 6 | JWT 이슈 시 `jti` + Denylist (`TCF_TOKEN_DENYLIST`) |

### 6.2 자주 쓰는 로그 키워드

| 키워드 | 의미 |
|--------|------|
| `TX_START` / `TX_END` | 거래 시작·종료 |
| `TCF_METRIC` | 처리시간 DEBUG |
| `AUDIT` | 감사 이벤트 |
| `[EAI] CALL` | 서비스 간 연동 |
| `******* [GW-JWT]` | Gateway JWT |
| `******* [TCF-WEB-JWT]` | 업무 WAR JWT |
| `******* [TCF-AUTH-CTX]` | claim vs Header |
| `Failed to persist transaction log` | 거래로그 DB 실패 (거래 자체는 성공 가능) |

### 6.3 알람 임계치 (권장 초안)

| 조건 | 심각도 | 조치 |
|------|--------|------|
| 동일 `serviceId` 오류율 > 5% (5분) | Warning | 최근 배포·DB·연동 확인 |
| `E-TIMEOUT-*` 급증 | Critical | Timeout 정책·DB·외부 연동 |
| `TX_END` 없이 `TX_START`만 다수 | Critical | JVM hang·스레드 풀 |
| 거래로그 INSERT 실패 warn 연속 | Warning | TXLOG DB·DS 설정 |
| JWT 검증 실패 급증 | Warning | JWKS·시계·키 로테이션 ([43-security-operations.md](43-security-operations.md)) |

---

## 7. 환경별 설정

### 7.1 Logback

| 파일 | 역할 |
|------|------|
| `logback-nsight-base.xml` | MDC 패턴·rolling·transaction/audit logger |
| `logback-tcf-console.xml` | `tcf-console.log` (UTF-8) |

```yaml
logging:
  file.name: ${CATALINA_BASE}/logs/nsight-app.log
  charset.console: UTF-8   # Windows Tomcat은 setenv로 MS949 가능
```

### 7.2 거래로그 경로

`NSIGHT_TXLOG_PATH` — Transaction Log 전용 DataSource (업무 DB와 분리).

### 7.3 로컬 디버그

- JWT: `******* [GW-JWT]` 등 stdout — **prod에서는 제거·레벨 제한**
- `GuidMdcCleanupFilter` System.out — 개발용; 운영 노이즈 주의

---

## 8. 구현 갭·로드맵

| 항목 | 현재 | 목표 |
|------|------|------|
| 분산 Trace (W3C `traceparent`) | 미적용 | Gateway→WAR 헤더 전파 |
| Micrometer → Prometheus | 제한적 | 표준 대시보드·알람 |
| `TransactionMetricService` | DEBUG 로그만 | MeterRegistry 등록 |
| 구조화 로그 (JSON) | 패턴 텍스트 | ELK/Loki 연동 시 JSON encoder |
| Step 로그 (`TCF_TX_STEP_LOG`) | 설계·일부 | Handler 구간 자동 기록 |

갭 해소 전에도 **GUID·TraceId·TX_START/END**만으로 대부분의 온라인 장애는 추적 가능하다.

---

## 9. 역할·책임

| 활동 | 개발 | 운영 | SRE |
|------|:----:|:----:|:---:|
| MDC·TX_START/END 준수 | R | I | C |
| 로그 보관·로테이션 | C | R | A |
| 알람 규칙 | C | R | A |
| OM 거래로그 조회 | I | R | C |
| Trace 표준 도입 | R | I | A |

---

## 10. 관련 소스

| 파일 | 역할 |
|------|------|
| `tcf-core/.../processor/STF.java` | MDC 설정 |
| `tcf-core/.../logging/TransactionLogService.java` | TX_START/END |
| `tcf-core/.../logging/AuditLogService.java` | AUDIT |
| `tcf-core/.../metrics/TransactionMetricService.java` | TCF_METRIC |
| `tcf-web/.../GuidMdcCleanupFilter.java` | MDC 정리 |
| `tcf-eai/.../DefaultTcfServiceClient.java` | 연동 로그 |
| `tcf-gateway/.../GatewayTransactionLogRecorder.java` | Gateway TX 로그 |
| `tcf-core/src/main/resources/logback-nsight-base.xml` | 로그 패턴 |

---

← [43-security-operations.md](43-security-operations.md) · [45-disaster-recovery.md](45-disaster-recovery.md) →
