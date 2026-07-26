# 14. 온라인 아키텍처

| 항목 | 내용 |
|------|------|
| 문서 번호 | 14 |
| 제목 | Online Transaction Architecture |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [02-junmun.md](02-junmun.md), [03-transaction.md](03-transaction.md), [04-messaging.md](04-messaging.md), [11-login.md](11-login.md) |
| 구현 모듈 | `tcf-web`, `tcf-core`, `*-service`, `tcf-om`, `tcf-ui` |
| 대상 | 온라인 거래 개발자/운영자 |

---

## 1. 개요

NSIGHT TCF의 온라인 아키텍처는 **표준 JSON 전문 기반 동기 요청 처리**를 중심으로 설계된다.

핵심 목표:

1. 모든 온라인 거래를 `TCF.process()`로 수렴
2. 업무 확장은 `serviceId` 단위 Handler 추가로 해결
3. 오류/성공을 HTTP 상태가 아닌 `resultCode` 계약으로 통일

---

## 2. 온라인 처리 대상

| 구분 | 설명 |
|------|------|
| 온라인 거래 | `/online`, `/{businessCode}/online` POST 요청 |
| 주 사용자 | 채널 UI, OM Admin, 외부 연계 API, 내부 Gateway |
| 데이터 형태 | `StandardRequest<Map<String,Object>>` / `StandardResponse<Object>` |
| 성공 판별 | `result.resultCode == "S0000"` |

---

## 3. End-to-End 구조

```text
Client / Channel
   │
   ├─ 직접 호출: /{code}/online
   └─ UI 릴레이: /api/relay/{code}/online (tcf-ui)
           │
           ▼
OnlineTransactionController (or TcfGateway)
           │
           ▼
TCF.process()
  ├─ STF (전처리/검증/컨텍스트)
  ├─ Dispatcher (serviceId 라우팅)
  ├─ Handler → Facade → Service → Rule → DAO/Mapper
  └─ ETF (응답/로그/메트릭)
           │
           ▼
StandardResponse JSON
```

---

## 4. 진입점 아키텍처

## 4.1 표준 진입점: `OnlineTransactionController`

엔드포인트:

- `POST /online`
- `POST /{businessCode}/online`

보완 처리:

1. `header`가 없으면 빈 `StandardHeader` 생성
2. path의 `{businessCode}`를 header에 보정
3. `clientIp`를 `X-Forwarded-For` 또는 `remoteAddr`에서 보정

보완 후 `TCF.process(request)`로 전달한다.

## 4.2 비표준 진입점: `TcfGateway`

multipart/REST 등 비표준 요청도 `TcfInvokeRequest`를 통해 표준 전문으로 변환한다.

기본값 예:

- `systemId = NSIGHT-MP`
- `channelId = WEBTOP`(미지정 시)
- `businessCode = serviceId 첫 토큰`

## 4.3 UI 릴레이 진입점: `tcf-ui`

`tcf-ui`는 온라인 요청을 업무 WAS로 중계한다.

```text
Browser → /api/relay/{code}/online
       → TransactionRelayService
       → {target}/online
       → RelayResult(responseBody, httpStatus, elapsedMs, setCookies)
```

특징:

- 요청/응답 전문 본문은 변환 없이 투명 전달
- Cookie/Set-Cookie를 함께 전달해 세션 유지

---

## 5. TCF 온라인 파이프라인 상세

## 5.1 STF (Standard Transaction Front)

순서:

1. Header 검증 (`serviceId`, `businessCode`, `transactionCode`, `processingType`, `channelId`)
2. Header 정규화 (`systemId`, `requestTime` 등)
3. `guid`, `traceId` 생성
4. `TransactionContext` 생성 + ThreadLocal 등록
5. MDC 적재 (`guid`, `traceId`, `serviceId`, `userId`, `branchId`)
6. 세션/권한/멱등성 검사(설정 기반)
7. 거래 시작 로그(`TX_START`)

## 5.2 Dispatcher

`header.serviceId`로 Handler를 조회한다.

- 미등록 serviceId → `E-COM-DISP-0001`
- 누락 serviceId → `E-COM-VALID-0001`

## 5.3 Handler 이후 계층

```text
Handler (거래 어댑터)
  → Facade (@Transactional 경계)
    → Service (도메인 로직)
      → Rule (검증)
      → DAO/Mapper (영속)
```

## 5.4 ETF (End Transaction Framework)

- `success()` → `S0000`
- `businessFail()` → `E0001 + 업무 errorCode`
- `systemError()` → `E0001 + E-COM-SYS-0001`

공통 후처리:

- idempotency 상태 종료
- 거래 종료 로그(`TX_END`)
- 감사 로그
- 메트릭 기록

---

## 6. 온라인 계약(Contract) 아키텍처

## 6.1 요청/응답 계약

요청:

```json
{ "header": { ... }, "body": { ... } }
```

응답:

```json
{ "header": { ... }, "result": { ... }, "body": { ... } }
```

## 6.2 HTTP/Result 이중 규약

| 항목 | 규칙 |
|------|------|
| HTTP Method | POST only |
| HTTP Status | 성공/업무오류 모두 주로 200 |
| 실제 성공 여부 | `resultCode` (`S0000` / `E0001`) |
| 상세 원인 | `errorCode`, `errorMessage`, `errorDetail` |

---

## 7. 온라인 트랜잭션 경계

온라인 거래 1건은 다음 두 경계를 함께 가진다.

| 경계 | 설명 |
|------|------|
| TCF 거래 경계 | STF 시작 ~ ETF 종료 |
| DB 트랜잭션 경계 | Facade `@Transactional` |

관계:

- 비즈니스 예외 발생 시 DB 롤백 + ETF 실패응답 동시 수행
- 거래 로그(`TCF_TX_LOG`)는 별도 커밋으로 남아 추적 가능

---

## 8. 온라인 보안/권한 아키텍처

## 8.1 세션/권한 검증 플래그

- `nsight.tcf.session-validation-enabled`
- `nsight.tcf.authorization-validation-enabled`

기본은 false이며, 환경별로 활성화한다.

## 8.2 로그인 연계

OM 포털은 `OM.Auth.login`으로 세션을 만들고, 이후 온라인 호출에 세션 정보를 활용한다.

- UI Relay가 쿠키를 전달해 로그인 상태 유지
- 필요 시 STF의 SessionValidator가 header userId를 검증

---

## 9. 온라인 성능/타임아웃 아키텍처

온라인 지연 제어는 다층으로 적용된다.

| 레이어 | 예 |
|--------|----|
| Facade 트랜잭션 | `@Transactional(timeout=5/10/30)` |
| SQL | `mybatis.default-statement-timeout=3` |
| 커넥션 | Hikari `connection-timeout=3000ms` |
| 운영 관측 | `ELAPSED_TIME_MS` 기반 timeoutCount |

결과적으로 “실행 제어”와 “운영 관측”이 동시에 동작한다.

---

## 10. 온라인 로깅/추적 아키텍처

## 10.1 입력/출력 로그

- `TCF.logClientRequest` (입력 전문)
- `TCF.logClientResponse` (출력 전문)

## 10.2 거래 로그

- STF `TX_START`
- ETF `TX_END`
- `TCF_TX_LOG` DB 영속

## 10.3 상관키

- `guid`
- `traceId`
- `serviceId`

이 키로 앱로그, 거래로그, 대시보드 지표를 교차 추적한다.

---

## 11. 배포 토폴로지별 온라인 경로

## 11.1 bootRun

```text
UI(:8099) → OM(:8097)/SV(:8086)/... 직접 포트 호출
```

## 11.2 ztomcat

```text
UI(:8080/ui) → /api/relay/{code}/online → :8080/{context}/online
```

19개 WAR가 단일 게이트웨이 8080 하위 context로 라우팅된다. (`deploy-wars.sh` 기준 **13 WAR** 배포, uj·gateway는 bootRun)

---

## 12. 장애 대응 관점 온라인 흐름

1. `serviceId`, `guid`, `traceId` 확인
2. 입력 전문 로그로 요청 유효성 검토
3. `TCF_TX_LOG`에서 result/error/elapsed 확인
4. Handler/Facade/Service 레이어에서 도메인 원인 추적
5. 필요 시 릴레이(`RelayResult`) 상태와 HTTP 연결오류 분리 분석

---

## 13. 온라인 아키텍처 체크리스트

- [ ] 신규 거래가 `serviceId` 규약을 따르는가
- [ ] Handler가 비즈니스 로직 없이 Facade에 위임하는가
- [ ] Facade에 적정 `@Transactional timeout`이 선언됐는가
- [ ] 실패 응답이 `StandardResponse` 규약을 따르는가
- [ ] 릴레이 경로에서 Cookie/Set-Cookie가 유지되는가
- [ ] `guid/traceId` 기반 추적이 가능한가

---

## 14. 참고 소스

| 파일 | 설명 |
|------|------|
| `tcf-web/.../controller/OnlineTransactionController.java` | 표준 온라인 진입점 |
| `tcf-web/.../gateway/TcfGateway.java` | 비표준 요청의 온라인 파이프라인 위임 |
| `tcf-core/.../processor/TCF.java` | 온라인 처리 오케스트레이터 |
| `tcf-core/.../processor/STF.java` | 전처리/검증 |
| `tcf-core/.../processor/ETF.java` | 결과 조립/로그 |
| `tcf-ui/.../service/TransactionRelayService.java` | 릴레이/쿠키 전달 |
| `docs/architecture/03-transaction.md` | 트랜잭션 상세 |
| `docs/architecture/04-messaging.md` | 메시지/릴레이 상세 |

---

## 15. 변경 이력

| 일자 | 변경 내용 |
|------|-----------|
| 2026-06 | 최초 작성 — 온라인 처리 E2E 아키텍처 상세화 |
