# TCF Framework 아키텍처 상세 가이드

| 항목 | 내용 |
|------|------|
| 문서명 | TCF Framework Architecture Deep Dive |
| 시스템 | `nsight-tcf-framework` |
| 기술 스택 | Java 21, Spring Boot 3.3, MyBatis, H2, Ehcache |
| 대상 | 프레임워크/업무/운영 개발자 |
| 최종 갱신 | 2026-06 |

---

## 1. 한눈에 보는 TCF

TCF(Transaction Control Framework)는 온라인 거래를 표준 전문(JSON)으로 통일 처리하는 실행 프레임워크다.

핵심 아이디어:

1. 모든 온라인 요청을 `TCF.process()`로 수렴
2. 확장은 `serviceId` 기반 Handler 추가로 해결
3. 공통 관심사(검증/멱등/로그/응답)는 프레임워크가 처리

```text
Client/Channel
   │ POST /online or /{code}/online
   ▼
OnlineTransactionController / TcfGateway
   ▼
TCF.process()
  ├─ STF (전처리)
  ├─ Dispatcher (serviceId 라우팅)
  ├─ BTF: Handler → Facade → Service → Rule → DAO/Mapper
  └─ ETF (후처리)
   ▼
StandardResponse JSON
```

---

## 2. 모듈 아키텍처

## 2.1 Foundation

| 모듈 | 역할 |
|------|------|
| `tcf-util` | GUID/시간/공통 유틸 |
| `tcf-core` | TCF 엔진(STF/Dispatcher/ETF), 표준 메시지, 예외, 컨텍스트 |
| `tcf-web` | `/online` 엔드포인트, Gateway, 자동설정 |
| `tcf-cache` | 공통 캐시(Ehcache + Spring Cache) |

## 2.2 Platform

| 모듈 | 역할 |
|------|------|
| `tcf-om` | 운영관리 포털 API, 로그인/권한/세션/거래로그 조회 |
| `tcf-batch` | AP/DB/세션/배포 상태 수집 배치 |
| `tcf-ui` | 테스트 UI + OM Admin + Relay |

## 2.3 Business

- `cc-service` ~ `mg-service` **목표 17업무** — 현재 저장소 **9개** (`ic` … `mg`)
- 동일한 BTF 패턴 (Handler/Facade/Service/Rule/DAO/Mapper) — [35-BTF.md](35-BTF.md)

---

## 3. 온라인 처리 아키텍처 (핵심 런타임)

## 3.1 진입점

| 진입점 | 설명 |
|--------|------|
| `OnlineTransactionController` | 표준 JSON 요청 수신 (`/online`, `/{code}/online`) |
| `TcfGateway` | 비표준 REST/multipart 요청을 표준 전문으로 변환 위임 |
| `tcf-ui Relay` | 브라우저 요청을 각 WAR `/online`으로 중계 |

## 3.2 STF (Standard Transaction Front)

전처리 순서:

1. Header 필수값 검증
2. Header normalize
3. `guid`/`traceId` 생성
4. `TransactionContext` 생성 + ThreadLocal 저장
5. MDC 적재
6. Session/Auth/Idempotency 검사(설정 기반)
7. TX_START 로그

## 3.3 Dispatcher

- `serviceId` → `TransactionHandler` 매핑
- 미등록 시 `E-COM-DISP-0001`
- 기동 시 중복 `serviceId`는 실패 처리

## 3.4 업무 계층

```text
Handler (얇은 어댑터)
  → Facade (트랜잭션 경계)
    → Service (도메인 처리)
      → Rule (검증)
      → DAO/Mapper (영속)
```

## 3.5 ETF (End Transaction Framework)

상세: [36-ETF.md](36-ETF.md)

| 분기 | 조건 | resultCode |
|------|------|------------|
| `success()` | 정상 | `S0000` |
| `businessFail()` | `BusinessException` | `E0001` + 업무 errorCode |
| `systemError()` | 기타 예외 | `E0001` + `E-COM-SYS-0001` |

공통 후처리:

- idempotency 상태 종료
- TX_END 로그 + 거래로그 저장
- 감사 로그/메트릭 기록

---

## 4. 메시지/전문 아키텍처

## 4.1 표준 전문

요청:

```json
{ "header": { ... }, "body": { ... } }
```

응답:

```json
{ "header": { ... }, "result": { ... }, "body": { ... } }
```

## 4.2 계약 규칙

| 항목 | 규칙 |
|------|------|
| Method | POST only |
| Content-Type | `application/json` |
| 성공 판별 | HTTP 상태가 아니라 `result.resultCode` |
| 라우팅 키 | `header.serviceId` |

## 4.3 채널 경로

- 직접: `/{code}/online`
- 릴레이: `/api/relay/{code}/online` → 대상 `/online`

---

## 5. 예외/오류 아키텍처

## 5.1 예외 계층

| 예외 | 의미 |
|------|------|
| `BusinessException` | 예상 가능한 업무 오류 |
| 기타 `Exception` | 시스템 오류 |

## 5.2 오류코드 체계

```text
E-{영역}-{분류}-{번호}
예) E-COM-VALID-0001, E-OM-AUTH-0001
```

## 5.3 변환 위치

- 파이프라인 내부: ETF에서 표준 응답 변환
- 파이프라인 외부: `GlobalStandardExceptionHandler` 안전망

---

## 6. 트랜잭션/로그 아키텍처

## 6.1 경계

| 경계 | 위치 |
|------|------|
| TCF 거래 경계 | STF 시작 ~ ETF 종료 |
| DB 트랜잭션 경계 | Facade `@Transactional` |

## 6.2 로그 2축

| 로그 유형 | 목적 |
|-----------|------|
| 입력/출력 전문 로그 | 요청/응답 payload 디버깅 |
| 거래 요약 로그 | `TCF_TX_LOG` 기반 운영/통계/추적 |

## 6.3 거래로그 저장 특징

- `TransactionLogService`가 TX_START/TX_END 기록
- `JdbcTransactionLogRepository`가 `TCF_TX_LOG`에 INSERT
- 업무 TX와 분리된 auto-commit 설계로 실패 거래도 보존

---

## 7. 세션/로그인 아키텍처

## 7.1 로그인

표준 거래:

- `OM.Auth.login`
- `OM.Auth.logout`
- `OM.Auth.session`

처리:

1. 계정/비밀번호 검증
2. Spring Session 생성(JDBC)
3. `JSESSIONID` 쿠키 발급

## 7.2 세션 저장

- `SPRING_SESSION`, `SPRING_SESSION_ATTRIBUTES`
- `spring.session.store-type: jdbc`
- `spring.session.timeout: 60m`

## 7.3 세션 검증

- STF `SessionValidator` (옵션)
- `session-validation-enabled=true` 시 `header.userId` 검증

---

## 8. 캐시 아키텍처

기반:

- Spring Cache + Ehcache 3 (JCache)

주요 캐시:

- `commonCode` (30분)
- `serviceCatalog` (60분)
- `sessionRegion` (10분)

운영 제어:

- `OM.Cache.inquiry` / `OM.Cache.delete`
- `TcfCacheSupport.snapshot/evict`

---

## 9. 배치/운영관제 아키텍처

`tcf-batch`가 주기적으로 상태를 수집해 OM 테이블에 upsert:

| Job | 저장 테이블 |
|-----|-------------|
| AP 상태 | `OM_AP_STATUS` |
| DB 상태 | `OM_DB_STATUS` |
| 세션 상태 | `OM_SESSION_STATUS` |
| 배포 상태 | `OM_DEPLOY_STATUS` |

실행 이력은 `OM_BATCH_HISTORY`에 기록되고, OM 대시보드가 이를 조회한다.

---

## 10. 타임아웃 아키텍처

다층 타임아웃 구조:

| 레이어 | 예시 |
|--------|------|
| 트랜잭션 | `@Transactional(timeout=5/10/30)` |
| SQL | `mybatis.default-statement-timeout=3` |
| 커넥션 | Hikari `connection-timeout=3000` |
| 원격 호출 | connect/read timeout 명시 |
| 관측 | `ELAPSED_TIME_MS` 기반 timeoutCount |

---

## 11. 배포 아키텍처

| 모드 | 특징 |
|------|------|
| `bootRun` | 모듈별 개별 포트 개발/디버깅 |
| `ztomcat` | 8080 단일 게이트웨이 + **13 WAR** 통합 검증 (`deploy-wars.sh`) |

예:

- bootRun: `http://localhost:8086/online`
- tomcat: `http://localhost:8080/sv/online`

---

## 12. 확장 모델

신규 거래 추가 최소 단위:

1. `serviceId` 정의 (`{BC}.{Domain}.{action}`)
2. `TransactionHandler` 구현
3. Facade/Service/Rule/DAO/Mapper 연결
4. `transactionCode`, 권한, timeout 카탈로그 등록
5. 샘플 전문/테스트 케이스 추가

---

## 13. 운영 진단 가이드

장애 시 추적 순서:

1. `guid/traceId/serviceId` 확인
2. 입력/출력 전문 로그 확인
3. `TCF_TX_LOG`에서 `resultCode/errorCode/elapsed` 확인
4. 해당 Handler/Facade/Service 로직 추적
5. 필요 시 배치 상태(AP/DB/세션/배포)와 교차 분석

---

## 14. 관련 상세 문서

상세 분해 문서:

- `docs/architecture/01-application-layer.md`
- `docs/architecture/02-junmun.md`
- `docs/architecture/03-transaction.md`
- `docs/architecture/04-messaging.md`
- `docs/architecture/05-exception.md`
- `docs/architecture/06-naming.md`
- `docs/architecture/07-DAO.md`
- `docs/architecture/26-mybatis.md`
- `docs/architecture/27-paging.md`
- `docs/architecture/28-tcf-framework-ref.md`
- `docs/architecture/29-facade.md`
- `docs/architecture/30-springboot.md`
- `docs/architecture/31-autoconfiguration.md`
- `docs/architecture/32-AOP.md`
- `docs/architecture/33-TCF.md`
- `docs/architecture/34-STF.md`
- `docs/architecture/35-BTF.md`
- `docs/architecture/36-ETF.md`
- `docs/architecture/37-transaction-log.md`
- `docs/architecture/38-script.md`
- `docs/architecture/08-timeout.md`
- `docs/architecture/09-transaction log.md`
- `docs/architecture/10-session.md`
- `docs/architecture/11-login.md`
- `docs/architecture/12-cache.md`
- `docs/architecture/13-batch.md`
- `docs/architecture/14-online-arc.md`

---

## 15. 변경 이력

| 일자 | 변경 내용 |
|------|-----------|
| 2026-06 | 최초 작성 — TCF 전체 아키텍처 통합 상세 가이드 |
