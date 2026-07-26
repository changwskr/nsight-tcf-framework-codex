# TCF (Transaction Control Framework) 정리

**TCF**는 NSIGHT 플랫폼의 **온라인 거래 처리 엔진**입니다. HTTP JSON 전문 1건을 **단일 파이프라인**으로 통제하고, 업무 개발자는 **`TransactionHandler` + `serviceId`** 등록만으로 거래를 확장합니다.

| 관점 | 역할 | 핵심 |
|------|------|------|
| **프레임워크** | NSIGHT TCF Framework 전체 | Gradle 멀티 모듈 (tcf-core, tcf-web, *-service …) |
| **TCF 엔진** | 온라인 1건 오케스트레이션 | `TCF.process()` — STF → Dispatcher → ETF |
| **STF** | 전처리 | 검증·세션·거래통제·Timeout·멱등·TX_START |
| **BTF** | 업무 처리 | Handler → Facade → Service → Rule → DAO |
| **ETF** | 후처리 | S0000/E0001 응답·감사·메트릭·TX_END |

---

## 1. TCF란?

**TCF(Transaction Control Framework)** = 온라인 거래 **1 HTTP Request**를 표준 계약으로 처리하는 **공통 파이프라인**.

```text
[ tcf-web ]   OnlineTransactionController / TcfGateway
       │  StandardRequest { header, body }
       ▼
[ tcf-core ]  TCF.process()
       STF → OnlineTimeout → Dispatcher → [ BTF ] → ETF
       ▼
  StandardResponse { header, result, body }
```

**핵심 설계 원칙 (README)**

1. **Handler 중심** — 업무 개발자는 Controller 대신 `TransactionHandler` 등록
2. **공통 파이프라인** — 검증·세션·거래통제·Timeout·로깅·응답은 STF/TCF/ETF
3. **업무 독립 WAR** — 9개 업무 + OM 동일 패턴 Spring Boot WAR
4. **이중 배포** — bootRun(포트 분리) · ztomcat(8080 게이트웨이)

---

## 2. 용어 — STF / TCF / ETF / BTF

| 약어 | 클래스 | 의미 |
|------|--------|------|
| **STF** | `STF` | **S**tandard **T**ransaction **F**ront — 전처리 |
| **TCF** | `TCF` | **T**ransaction **C**ontrol **F**ramework — 오케스트레이션 |
| **ETF** | `ETF` | **E**nd **T**ransaction **F**ramework — 후처리·응답 |
| **BTF** | (업무 계층) | **B**usiness **T**ransaction **F**ramework — Handler 이후 |

```text
┌── 1 HTTP Request ──────────────────────────────────────────┐
│ TCF 온라인 거래 (tcf-core)                                  │
│   STF ── Dispatcher(BTF: Handler→Facade→Service→DAO) ── ETF │
│         └─ Facade @Transactional ← Spring DB TX (선택)      │
└────────────────────────────────────────────────────────────┘
```

**"트랜잭션" 두 가지** — 혼동 주의:

| 용어 | 의미 |
|------|------|
| TCF 온라인 거래 | JSON 1건 파이프라인 (STF~ETF) |
| Spring DB TX | Facade `@Transactional` 커밋/롤백 |

상세: [온라인처리.md](온라인처리.md)

---

## 3. 모듈 구조

### 3.1 의존 방향

```text
tcf-util  →  tcf-core  →  tcf-web  →  tcf-cache(선택)
                              ↓
                    *-service / tcf-om / tcf-batch / tcf-jwt / tcf-gateway
```

| 모듈 | 산출물 | TCF 관련 역할 |
|------|--------|---------------|
| `tcf-util` | JAR | `GuidGenerator`, `DateTimeUtil` |
| **`tcf-core`** | JAR | **TCF 엔진** — STF/TCF/ETF, Dispatcher, 거래통제·Timeout |
| **`tcf-web`** | JAR | `/online` Controller, `TcfGateway`, 거래로그 JDBC |
| `tcf-cache` | JAR | EhCache / `@Cacheable` |
| `tcf-om` | WAR | OM Admin + 온라인 Handler |
| `*-service` | WAR | 업무 Handler·Facade·Service |
| `tcf-ui` | WAR | Relay — TCF **미포함**, `/online` 중계만 |
| `tcf-batch` | WAR | 수집 배치 — TCF **미사용** (`transaction-log-enabled: false`) |

### 3.2 tcf-core 패키지 맵

```text
com.nh.nsight.tcf.core/
├── processor/      TCF, STF, ETF
├── dispatch/       TransactionDispatcher
├── transaction/    TransactionHandler (SPI)
├── message/        StandardRequest/Response/Header
├── context/        TransactionContext, Holder
├── control/        TransactionControlService
├── timeout/        TimeoutPolicyService, OnlineTransactionTimeoutExecutor
├── validation/     StandardHeaderValidator
├── security/       SessionValidator, AuthorizationValidator
├── idempotency/    IdempotencyChecker
├── logging/        TransactionLogService, AuditLogService
├── error/          BusinessException, ErrorCode
└── config/         TcfProperties (nsight.tcf.*)
```

---

## 4. TCF.process() — 핵심 흐름

```35:85:tcf-core/src/main/java/com/nh/nsight/tcf/core/processor/TCF.java
    public StandardResponse<Object> process(StandardRequest<Map<String, Object>> request) {
        // ...
        context = stf.preProcess(request, clientHeader);
        Object body = onlineTransactionTimeoutExecutor.execute(
                () -> dispatcher.dispatch(request, dispatchContext));
        return etf.success(request, body, context, clientHeader);
        // BusinessException → businessFail
        // Timeout → businessFail (TIMEOUT)
        // 기타 Exception → systemError
        // finally: ContextHolder, TimeoutContextHolder, MDC clear
    }
```

| 단계 | 컴포넌트 | 결과 |
|------|----------|------|
| 1 | STF | `TransactionContext` + MDC + TX_START |
| 2 | OnlineTimeout + Dispatcher | Handler body |
| 3 | ETF | `StandardResponse` (S0000 / E0001) |

**설계 포인트**

- 예외를 HTTP 500으로 올리지 않고 **표준 JSON**으로 변환
- 성공 판별: **`result.resultCode == "S0000"`** (HTTP는 주로 200)
- `clientHeader`: 클라이언트 원본 header echo용 스냅샷

---

## 5. STF — 전처리 순서

| # | 처리 | 비고 |
|---|------|------|
| 1 | Header 필수값 검증 | `serviceId`, `businessCode`, … |
| 2 | guid / traceId 부여 | |
| 3 | TransactionContext + MDC | |
| 4 | 세션 검증 | 설정 시 — [세션관리.md](세션관리.md) |
| 5 | 권한 검증 | 설정 시 |
| 6 | **거래통제** | `TCF_TRANSACTION_CONTROL` — [타임아웃관리.md](타임아웃관리.md) |
| 7 | **Timeout 정책** | `TCF_SERVICE_TIMEOUT_POLICY` |
| 8 | 멱등성 | `idempotencyKey` 또는 `guid` |
| 9 | 거래 시작 로그 | TX_START |

---

## 6. Dispatcher — serviceId 라우팅

```text
기동:  @Component TransactionHandler 전체 → Map<serviceId, Handler>
요청:  handlerMap.get(serviceId) → handler.doHandle()
```

| 상황 | errorCode |
|------|-----------|
| serviceId 없음 | `E-COM-VALID-0001` |
| 미등록 serviceId | `E-COM-DISP-0001` |
| 중복 serviceId | 기동 시 `IllegalStateException` |

**serviceId 형식:** `{업무코드}.{도메인}.{처리유형}` — 예) `SV.Sample.inquiry`, `OM.Auth.login`

네이밍: [네이밍.md](네이밍.md)

---

## 7. BTF — 업무 확장 (Handler SPI)

업무 개발자가 구현하는 **유일한 TCF 진입점**:

```text
@Component Handler (serviceId 등록)
  → entry/facade (@Transactional)
    → application/service
      → application/rule
      → persistence/dao → mapper
```

| 규칙 | 내용 |
|------|------|
| Handler | thin — Facade 위임, **예외 catch 금지** |
| Facade | `@Transactional` 경계 — [AOP.md](AOP.md) |
| Rule | `BusinessException` — [예외처리.md](예외처리.md) |

6계층 상세: [어플리케이션계층.md](어플리케이션계층.md)

---

## 8. ETF — 후처리

| 분기 | trigger | resultCode |
|------|---------|------------|
| `success()` | 정상 | `S0000` |
| `businessFail()` | `BusinessException` | `E0001` + 업무 errorCode |
| `systemError()` | 기타 Exception | `E0001` + `E-COM-SYS-0001` |

공통: idempotency 종료, TX_END, audit, metric, `StandardResponse` 조립.

전문 형식: [전문관리.md](전문관리.md)

---

## 9. 프레임워크 역량 (STF~ETF 관통)

| 영역 | 테이블/구성 | 적용 시점 |
|------|-------------|-----------|
| 표준 전문 | `StandardRequest/Response` | 전 구간 |
| 거래통제 | `TCF_TRANSACTION_CONTROL` | STF |
| Timeout | `TCF_SERVICE_TIMEOUT_POLICY` | STF 조회 → Online/TX/Query |
| 세션·권한 | `SPRING_SESSION`, STF Validator | STF — [로그인.md](로그인.md) |
| 멱등성 | `IdempotencyChecker` | STF / ETF |
| 거래로그 | `TCF_TX_LOG` | STF START → ETF END |
| 캐시 | EhCache (`tcf-cache`) | Service `@Cacheable` — [캐시관리.md](캐시관리.md) |
| 오류코드 | `E-{DOMAIN}-{CATEGORY}-{NNNN}` | Rule / ETF |

```yaml
nsight.tcf:
  session-validation-enabled: false      # OM Admin 등에서 true
  authorization-validation-enabled: false
  idempotency-enabled: true
  transaction-log-enabled: true
  transaction-control-enabled: true
  timeout-policy-enabled: true
```

---

## 10. 세 경계 — TCF / Spring TX / 거래로그

```text
┌── TCF 온라인 거래 ─────────────────────────────────────┐
│ STF → Handler(Facade @Transactional) → ETF              │
│   · BusinessException → Facade rollback + ETF E0001     │
└─────────────────────────────────────────────────────────┘

┌── 거래로그 (별도 DS) ────────────────────────────────────┐
│ TCF_TX_LOG — Facade rollback과 무관하게 END 기록         │
└─────────────────────────────────────────────────────────┘
```

| 개념 | 롤백 |
|------|------|
| TCF 파이프라인 | 없음 — ETF가 결과 전문 생성 |
| Facade `@Transactional` | BusinessException 시 rollback |
| `TCF_TX_LOG` | auto-commit, 업무 TX 분리 |

---

## 11. 진입점

| 진입 | 모듈 | 용도 |
|------|------|------|
| `POST /{code}/online` | tcf-web | 표준 온라인 |
| `TcfGateway.invoke()` | tcf-web | multipart/REST → 표준 전문 |
| `/api/relay/{code}/online` | tcf-ui | Cookie 중계 |
| tcf-gateway | tcf-gateway | API Gateway + 세션 관문 |

온라인 상세: [온라인처리.md](온라인처리.md)

---

## 12. TCF가 **아닌** 것

| 구분 | 처리 방식 | 문서 |
|------|-----------|------|
| 스케줄·배치 | `@Scheduled`, CollectService | [스케줄러.md](스케줄러.md), [배치관리.md](배치관리.md) |
| UI Relay | HTTP 프록시 (TCF 미실행) | [세션관리.md](세션관리.md) |
| Gateway 관문 | 세션 DB 검증 (별도) | [세션관리.md](세션관리.md) |

---

## 13. End-to-End 예시

**요청:** `POST /sv/online`, `serviceId = SV.Sample.inquiry`

```text
1. OnlineTransactionController — businessCode, clientIp 보정
2. TCF.process()
3. STF — validate, guid, 거래통제, Timeout, idempotency, TX_START
4. OnlineTimeoutExecutor → Dispatcher → SvSampleHandler
5. Facade.inquiry [@Transactional] → Service → Rule → DAO
6. ETF.success — S0000, TX_END, TCF_TX_LOG
7. finally — ContextHolder.clear(), MDC.clear()
```

---

## 14. 확장·교체 (SPI)

| SPI | 기본 | 교체 |
|-----|------|------|
| `TransactionHandler` | 업무 `@Component` | 도메인당 1 Handler, `serviceIds()`에 거래 추가 |
| `TransactionLogRepository` | `JdbcTransactionLogRepository` | `@Bean` 오버라이드 |
| `IdempotencyChecker` | InMemory | Redis 등 `@Primary` |
| Session/Auth Validator | tcf-core 기본 | `@Component` 교체 |

**하지 않을 것**

- STF/ETF/TCF 상속·오버라이드
- Handler에서 `BusinessException` catch
- TCF 우회 Controller (표준 온라인)

---

## 15. 개발자 체크리스트

- [ ] `serviceId` 규칙 준수 + Handler `@Component` 등록?
- [ ] Handler thin, Facade `@Transactional`?
- [ ] 성공 = `resultCode S0000` (HTTP status 아님)?
- [ ] `BusinessException`을 Handler에서 삼키지 않았는가?
- [ ] 거래통제·Timeout 정책 OM 등록 (필요 시)?
- [ ] 기동 로그 `Registered NSIGHT handler. serviceId=` 확인?

---

## 16. zdoc 주제별 링크

| 주제 | 문서 |
|------|------|
| 온라인 파이프라인 | [온라인처리.md](온라인처리.md) |
| 표준 전문 | [전문관리.md](전문관리.md) |
| 6계층 | [어플리케이션계층.md](어플리케이션계층.md) |
| Timeout·거래통제 | [타임아웃관리.md](타임아웃관리.md) |
| 예외·resultCode | [예외처리.md](예외처리.md) |
| 세션·로그인 | [세션관리.md](세션관리.md), [로그인.md](로그인.md) |
| AOP·TX | [AOP.md](AOP.md) |
| DAO | [DAO처리.md](DAO처리.md) |
| 네이밍 | [네이밍.md](네이밍.md) |
| 캐시 | [캐시관리.md](캐시관리.md) |
| 배치·스케줄 | [배치관리.md](배치관리.md), [스케줄러.md](스케줄러.md) |

---

## 17. 공식 아키텍처 문서

| 문서 | 내용 |
|------|------|
| [docs/architecture/33-TCF.md](../docs/architecture/33-TCF.md) | TCF 엔진 소스 가이드 |
| [docs/architecture/03-transaction.md](../docs/architecture/03-transaction.md) | 트랜잭션 처리 |
| [docs/architecture/architecture.md](../docs/architecture/architecture.md) | 전체 아키텍처 |
| [tcf-core/README.md](../tcf-core/README.md) | tcf-core 모듈 |
| [README.md](../README.md) | 프로젝트 개요 |
