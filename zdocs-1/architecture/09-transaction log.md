# 09. 트랜잭션 입력/출력 로그 처리 아키텍처

| 항목 | 내용 |
|------|------|
| 문서 번호 | 09 |
| 제목 | Transaction Input/Output Logging Architecture |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [03-transaction.md](03-transaction.md), [04-messaging.md](04-messaging.md), [05-exception.md](05-exception.md), [08-timeout.md](08-timeout.md), [37-transaction-log.md](37-transaction-log.md) |
| 구현 모듈 | `tcf-core`, `tcf-web`, `tcf-om` |
| 대상 | 프레임워크·업무·운영 개발자 |

---

## 1. 개요

NSIGHT TCF의 트랜잭션 로그는 크게 2종류로 분리된다.

| 로그 유형 | 목적 | 저장 위치 |
|-----------|------|----------|
| **입출력(전문) 로그** | 요청/응답 payload 추적, 디버깅 | 콘솔/애플리케이션 로그 (`TCF.logClientRequest/Response`) |
| **거래 요약 로그** | 거래 이력, 통계, 운영 조회 | `TCF_TX_LOG` 테이블 + `transaction.log` 로거 |

즉, “무엇이 들어오고 나갔는지”와 “거래가 어떻게 끝났는지”를 분리해 기록한다.

---

## 2. 전체 처리 흐름

```text
HTTP Request(StandardRequest)
   │
   ▼
TCF.process()
   ├─ logClientRequest()                ← 입력 전문 로그
   ├─ STF.preProcess()
   │    └─ TransactionLogService.start  ← TX_START 로그
   ├─ Dispatcher → Handler
   ├─ ETF.success/businessFail/systemError
   │    └─ TransactionLogService.end    ← TX_END + DB persist
   ├─ logClientResponse()               ← 출력 전문 로그
   └─ cleanup(Context/MDC)
```

핵심 포인트:

1. 입력 로그는 **처리 시작 시점**에 남는다.
2. 출력 로그는 **응답 생성 직후**에 남는다.
3. DB 거래로그는 ETF에서 **성공/실패 공통**으로 남는다.

---

## 3. 입력/출력 전문 로그 아키텍처

## 3.1 입력 로그 (`logClientRequest`)

`TCF.process()` 시작 직후 호출되어 다음을 기록한다.

- Header 전체 주요 필드  
  (`systemId`, `businessCode`, `serviceId`, `transactionCode`, `processingType`, `guid`, `traceId`, `channelId`, `userId`, `branchId`, `clientIp`, `idempotencyKey`)
- Body payload (`Map` pretty 형식)

목적:

- “클라이언트가 실제로 보낸 값”을 초기 상태로 보존
- Header 누락/오염/정규화 전후 추적 기반 제공

## 3.2 출력 로그 (`logClientResponse`)

ETF에서 응답 생성 후 호출되어 다음을 기록한다.

- 응답 Header 핵심 필드 (`businessCode`, `serviceId`, `transactionCode`, `guid`, `traceId`)
- Result (`resultCode`, `resultMessage`, `errorCode`, `errorMessage`)
- Body payload

목적:

- 요청 대비 응답의 결과·오류코드·payload 차이를 빠르게 비교
- 장애 분석 시 DB 로그 없이도 1차 진단 가능

## 3.3 로거 채널

전문 입출력은 `TcfConsoleLog` 기반 콘솔/앱 로그 경로를 사용한다.  
운영에서는 로그 수집기(파일비트/에이전트)로 수집해 검색하는 패턴을 권장한다.

---

## 4. 거래 시작/종료 로그 아키텍처 (`TransactionLogService`)

### 4.1 TX_START

`STF.preProcess()`에서 호출:

```text
TX_START guid=... traceId=... serviceId=... txCode=... userId=... branchId=... channelId=...
```

역할:

- 거래가 파이프라인에 정상 진입했음을 표준 포맷으로 기록
- guid/traceId 기반 상관관계 추적 시작점

### 4.2 TX_END

`ETF.success/businessFail/systemError`에서 공통 호출:

```text
TX_END guid=... traceId=... serviceId=... resultCode=... errorCode=... elapsedMs=...
```

역할:

- 거래 종료 상태(`S0000`/`E0001`)와 소요시간 기록
- 이후 DB 영속화(`persist`) 트리거

---

## 5. 거래 로그 DB 영속화 아키텍처

## 5.1 저장 조건

`TransactionLogService.persist()`는 아래 조건에서만 DB에 저장한다.

1. `nsight.tcf.transaction-log-enabled = true`
2. `TransactionLogRepository` 빈이 존재

빈 미존재 시 WARN 로그 후 skip (서비스 처리 자체는 계속).

## 5.2 저장 레코드 구조

`TransactionLogRecord` 주요 필드:

| 필드 | 값 소스 |
|------|---------|
| `LOG_ID` | `GuidGenerator.newGuid()` |
| `TX_TIME` | `DateTimeUtil.nowKst()` |
| `BUSINESS_CODE`, `SERVICE_ID`, `TRANSACTION_CODE` | Header |
| `GUID`, `TRACE_ID`, `USER_ID`, `BRANCH_ID` | Header |
| `RESULT_STATUS` | `resultCode == S0000 ? SUCCESS : FAIL` |
| `RESULT_CODE` | `S0000` or `E0001` |
| `ERROR_CODE` | 업무/시스템 오류코드 |
| `ELAPSED_TIME_MS` | `context.elapsedMillis()` |

## 5.3 물리 테이블

- 테이블명 표준: `TCF_TX_LOG` (`TcfTransactionLogConstants.TABLE_NAME`)
- 기본 경로: `data/nsight-txlog/nsight_om` (H2 file)
- OM 조회/TCF 적재가 동일 테이블 공유

---

## 6. 독립 커밋 설계 (`JdbcTransactionLogRepository`)

`JdbcTransactionLogRepository.save()`는 INSERT 시 `connection.setAutoCommit(true)`를 사용한다.

의도:

- 업무 트랜잭션(`@Transactional`, auto-commit false)과 **분리**
- 업무 롤백이 발생해도 거래로그는 남김

```text
업무 DB TX 롤백  ─┐
                  ├─ TCF_TX_LOG INSERT는 별도 커밋
거래로그 보존    ─┘
```

이 설계 덕분에 장애/실패 분석 시 “실패한 거래”도 로그에서 유실되지 않는다.

---

## 7. 성공/실패 경로별 로그 시퀀스

### 7.1 성공 경로

```text
logClientRequest
→ TX_START
→ Handler 처리
→ ETF.success
   → idempotency markSuccess
   → TX_END(resultCode=S0000)
   → TCF_TX_LOG INSERT(SUCCESS)
→ logClientResponse
```

### 7.2 비즈니스 실패 경로

```text
logClientRequest
→ TX_START
→ Handler/Rule에서 BusinessException
→ ETF.businessFail
   → idempotency markFail
   → TX_END(resultCode=E0001, errorCode=업무코드)
   → TCF_TX_LOG INSERT(FAIL)
→ logClientResponse
```

### 7.3 시스템 오류 경로

```text
logClientRequest
→ TX_START
→ Exception(NPE/SQL 등)
→ ETF.systemError
   → error 로그 stack trace
   → TX_END(resultCode=E0001, errorCode=E-COM-SYS-0001)
   → TCF_TX_LOG INSERT(FAIL)
→ logClientResponse
```

---

## 8. 트랜잭션 로그와 모니터링 연계

OM은 `TCF_TX_LOG`를 직접 조회해 운영 지표를 만든다.

예:

- `totalCount`, `successCount`, `errorCount`
- `timeoutCount` (`ELAPSED_TIME_MS >= 5000`)
- 평균 응답시간 `avgElapsedMs`
- 에러 상위 `serviceId/errorCode`

즉, 거래로그는 단순 이력 테이블이 아니라 대시보드/장애대응의 핵심 데이터 소스다.

---

## 9. MDC·추적 키 연동

STF는 `guid`, `traceId`, `serviceId`, `userId`, `branchId`를 MDC에 넣고,  
TCF finally + Filter에서 정리한다.

효과:

- 전문 로그(TCF 콘솔) + 거래로그(transaction.log/DB) + 에러로그(SLF4J)를 guid/traceId로 교차 추적 가능

---

## 10. 보안/개인정보 관점 운영 원칙

현재 `logClientRequest`는 Body를 그대로 출력한다. 운영 시 다음 가드를 권장한다.

1. 비밀번호/토큰/민감정보 마스킹 필터 적용
2. 대용량 Body 로그 제한(길이/필드 화이트리스트)
3. 로그 보관주기·접근권한 분리
4. 감사로그(`audit.log`)와 거래로그(`transaction.log`) 용도 분리 유지

---

## 11. 장애 대응 플레이북 (권장)

1. `guid` 또는 `traceId`로 `transaction.log`에서 TX_START/TX_END 확인
2. 동일 guid로 앱 로그에서 입력/출력 전문 비교
3. `TCF_TX_LOG`에서 `RESULT_CODE`, `ERROR_CODE`, `ELAPSED_TIME_MS` 확인
4. `ERROR_CODE` 기준 OM 오류코드 정책/업무코드 정책 매핑
5. 필요 시 DB/외부연동 로그로 확장 추적

---

## 12. 운영 체크리스트

- [ ] `transaction-log-enabled`가 환경별 의도대로 설정되었는가
- [ ] `TransactionLogRepository` 빈이 정상 주입되는가
- [ ] `TCF_TX_LOG` 스키마/인덱스가 준비되었는가
- [ ] `guid/traceId`가 응답과 로그에서 일관되는가
- [ ] 고용량 Body 로그에 대한 마스킹/샘플링 정책이 있는가

---

## 13. 참고 소스

| 파일 | 설명 |
|------|------|
| `tcf-core/.../processor/TCF.java` | 요청/응답 전문 로그 출력 |
| `tcf-core/.../processor/ETF.java` | 종료 분기 + `transactionLogService.end` 호출 |
| `tcf-core/.../logging/TransactionLogService.java` | TX_START/TX_END + DB persist |
| `tcf-web/.../logging/JdbcTransactionLogRepository.java` | 독립 auto-commit INSERT |
| `tcf-core/.../logging/TcfTransactionLogConstants.java` | 테이블/기본 DS 경로 상수 |
| `tcf-om/.../mapper/om/OmOperationMapper.xml` | 거래로그 조회/집계 SQL |

---

## 14. 변경 이력

| 일자 | 변경 내용 |
|------|-----------|
| 2026-06 | 최초 작성 — 입력/출력 전문 로그 + 거래 DB 로그 이중 아키텍처 정리 |
