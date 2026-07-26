# 51. API Gateway 설계서

| 항목 | 내용 |
|------|------|
| 문서 번호 | 51 |
| 제목 | API Gateway Architecture Design |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [04-messaging.md](04-messaging.md), [10-session.md](10-session.md), [42-jwt.md](42-jwt.md), [43-security-operations.md](43-security-operations.md), [44-observability.md](44-observability.md) |
| 상세 매뉴얼 | [zarchitecture/06-API-Gateway-아키텍처.md](../../zarchitecture/06-API-Gateway-아키텍처.md), [tcf-gateway/docs/ROUTING_TABLE.md](../../tcf-gateway/docs/ROUTING_TABLE.md), [명명규칙-18-Gateway-라우팅.md](../../znsight-man/명명규칙-18-Gateway-라우팅.md) |
| 구현 모듈 | `tcf-gateway` (독립 WAR — `tcf-core` 미의존) |
| 대상 | Gateway·UI·보안·운영 담당자 |

---

## 1. 문서 목적

본 문서는 **`tcf-gateway`** 의 역할·처리 파이프라인·라우팅·세션 관문·JWT·로깅을 정의한다.

| 구분 | 담당 문서 |
|------|-----------|
| JWT 발급·STF 2차 검증 | [42-jwt.md](42-jwt.md) |
| 세션·로그인 | [10-session.md](10-session.md), [11-login.md](11-login.md) |
| 업무 serviceId 라우팅 | [33-TCF.md](33-TCF.md), [14-online-arc.md](14-online-arc.md) |
| **Gateway (본 문서)** | businessCode Relay·관문·Route DB |

핵심 문장:

> Gateway는 **L4/Apache를 대체하지 않는다**. 업무 WAR 앞에서 **HTTP Relay + 세션/JWT 1차 관문**을 수행하고, 업무 라우팅 키는 **`businessCode`** 이다 (`serviceId`는 WAR 내부 Dispatcher).

---

## 2. Gateway 역할 경계

### 2.1 Gateway가 하는 일

| 항목 | 설명 |
|------|------|
| **라우팅** | `ENV_CODE` + `businessCode` → `TCF_GATEWAY_ROUTE` → Target URL |
| **인증 관문** | Cookie 세션 4단계 또는 Bearer JWT (JWKS) |
| **Header 보정** | SESSIONDB `userId`·`branchId`·`channelId` → 요청 전문 header |
| **Relay** | `RestClient` POST downstream `/online`, Cookie·Authorization 전달 |
| **Set-Cookie** | downstream 응답 쿠키를 클라이언트에 **그대로** 전달 |
| **Gateway 로그** | `TCF_GATEWAY_TX_LOG` |

### 2.2 Gateway가 하지 않는 일

| 항목 | 담당 |
|------|------|
| SSL 종료 | Apache / L4 |
| Sticky Session | L4 / Apache |
| JWT **발급** | `tcf-jwt` |
| `serviceId` → Handler | 업무 WAR `TCF.process()` |
| 거래통제·Timeout | STF (업무 WAR) |
| Denylist 실시간 연동 | **미구현** — [43-security-operations.md](43-security-operations.md) |

---

## 3. 배포·엔드포인트

| 항목 | 값 |
|------|-----|
| 모듈 | `tcf-gateway` |
| bootRun | **8100** |
| WAR | `gw.war` |
| ztomcat | `/gw` (8080) — **deploy-wars 기본 13 WAR에는 미포함**, bootRun 전용 |
| 패키지 | `com.nh.nsight.gateway` |

### 3.1 프록시 URL

| 호출자 | URL 예 |
|--------|--------|
| tcf-uj (bootRun) | `http://127.0.0.1:8100/sv/online` |
| tcf-uj (ztomcat) | `http://localhost:8080/gw/sv/online` |
| 직접 | `POST /{businessCode}/online` |

업무별 Controller: `SvProxyController`, `IcProxyController`, `OmProxyController`, `JwtProxyController` 등 — `AbstractBusinessProxyController` 상속.

---

## 4. End-to-End 흐름

```text
[Browser / tcf-ui / tcf-uj]
        │ POST /{code}/online  JSON + Cookie [+ Bearer]
        ▼
[*ProxyController]
        ▼
[BusinessRouteService] → [GRF.forwardOnline]
        │
        ├─ [GSF.preProcess]
        │     ① GatewayRouteResolver → TCF_GATEWAY_ROUTE
        │     ② GatewayAuthenticationService (JWT / 세션)
        │     ③ GatewaySessionRequestEnricher (header 보정)
        │
        ├─ [GatewayRouteDispatcher] → RestClient POST Target
        │
        ├─ [GEF] success | authFail | routeNotFound | httpError | connectionError
        │
        └─ [GatewayTransactionLogRecorder] → TCF_GATEWAY_TX_LOG
        ▼
[downstream WAR] POST /{code}/online → TCF.process()
        ▼
StandardResponse + Set-Cookie → Gateway → 클라이언트
```

---

## 5. 처리 파이프라인 (GRF / GSF / GEF)

TCF의 STF/BTF/ETF와 **대칭** naming:

| 단계 | 클래스 | 역할 |
|------|--------|------|
| **GRF** | `GRF` | 오케스트레이션 — preProcess → dispatch → postProcess → TX log |
| **GSF** | `GSF` | 전처리 — Route 조회·인증·body enrich |
| **GEF** | `GEF` | 후처리 — HTTP 상태·JSON 오류 body·OM 로그인 시 `TCF_USER_SESSION` 등록 |

### 5.1 패키지 구조

| 패키지 | 주요 클래스 |
|--------|-------------|
| `entry.web` | `*ProxyController`, `Gateway*AdminController` |
| `entry.facade` | `BusinessRouteService` |
| `application.service` | `GatewayAuthenticationService`, `GatewayJwtValidator`, `GatewayRouteResolver`, `GatewaySessionValidationService` |
| `application.rule` | `GatewaySessionValidator`, `GatewayAuthExemptions` |
| `client` | `GatewayRouteDispatcher` |
| `persistence.dao` | `GatewayRouteDao`, `SpringSessionDao`, `GatewayTransactionLogDao` |
| `support` | `GRF`, `GSF`, `GEF`, `GatewayRoute`, `GatewaySessionContext` |

---

## 6. 라우팅 (`TCF_GATEWAY_ROUTE`)

### 6.1 조회 키

```sql
WHERE ENV_CODE = :envCode      -- LOCAL | DEV | PRD (nsight.gateway.env-code)
  AND BUSINESS_CODE = :businessCode
  AND USE_YN = 'Y'
```

DB: 로컬 H2 `./data/gateway-route` — `GatewayRouteSchemaInitializer`  
상세 DDL·시드: [tcf-gateway/docs/ROUTING_TABLE.md](../../tcf-gateway/docs/ROUTING_TABLE.md)

### 6.2 Target URL 조립

```text
TARGET_URL = TARGET_BASE_URL + CONTEXT_PATH + ONLINE_PATH
예 (bootRun SV): http://127.0.0.1:8086 + /sv + /online
```

| 환경 | 예 (SV) |
|------|---------|
| LOCAL bootRun | `http://127.0.0.1:8086/sv/online` |
| DEV/PRD | K8s Service 호스트 등 (`tcf-cicd` 운영 설정) |

### 6.3 Route Cache

| Profile | `env-code` | Cache |
|---------|------------|-------|
| local | LOCAL | off |
| dev | DEV | TTL 30s (설정) |
| prod | PRD | TTL 60s (설정) |

`nsight.gateway.route-table.cache-enabled`

### 6.4 Target 선택 (설계·확장)

`TCF_GATEWAY_ROUTE_TARGET` — WEIGHT, STATUS, Health Check, Round Robin. 현행은 **단일 Target** 행 기준.

### 6.5 OM 연계

운영 Route CRUD: `/api/admin/routes`, Admin UI `/admin/routes.html`. OM 기준정보와 **동기 정책**은 운영 절차에서 확정 ([47-data-governance.md](47-data-governance.md)).

---

## 7. 인증 관문

### 7.1 `GatewayAuthenticationService` 분기

```text
loginRequired=false → 스킵
serviceId ∈ GatewayAuthExemptions → 스킵 (로그인 면제 거래)
OM + jwt.enabled → Bearer JWT 필수
기타 + Bearer present + jwt.enabled → JWT 검증
그 외 → 세션 4단계 (GatewaySessionValidationService)
```

구현: `GatewayAuthenticationService.authenticate()`

### 7.2 세션 4단계 (SESSIONDB)

Gateway는 **HttpSession을 생성하지 않음** (`server.servlet.session.tracking-modes: []`).

| 단계 | 검증 | 설정 키 |
|------|------|---------|
| 1 | Cookie (`JSESSIONID` / `NSIGHTSID`) | 필수 |
| 2 | `SPRING_SESSION` 존재·만료 | `session-validation.spring-session-check` |
| 3 | `TCF_USER_SESSION` STATUS | `user-session-check` |
| 4 | `header.userId` vs 세션 userId | `header-user-check` / `header-user-strict` |

| DataSource | 용도 |
|------------|------|
| `spring.datasource` | `TCF_GATEWAY_ROUTE`, `TCF_USER_SESSION`, `TCF_GATEWAY_TX_LOG` |
| `nsight.gateway.session-datasource` | `SPRING_SESSION` (tcf-om과 **동일** `nsight_om` file) |

`GatewayUserSessionSyncScheduler` — `TCF_USER_SESSION` ↔ `SPRING_SESSION` 주기 동기 (기본 10초).

### 7.3 JWT 1차 검증

| 항목 | 값 |
|------|-----|
| 설정 | `nsight.gateway.auth.jwt.enabled` |
| JWKS | `jwk-set-uri` (예: `http://127.0.0.1:8110/.well-known/jwks.json`) |
| issuer | `NSIGHT-AUTH` |
| audience | `NSIGHT-MP` |
| 구현 | `GatewayJwtValidator` |

**OM 업무**는 JWT 모드 시 Bearer **필수**. 업무 WAR는 Gateway 우회 시 `TcfJwtAuthenticationFilter` + STF 2차 검증 — [42-jwt.md](42-jwt.md).

### 7.4 Header 보정

`GatewaySessionRequestEnricher` — 세션 userId와 header.userId 불일치 시 **세션 값으로 보정** (위변조 완화). strict 모드에서는 4단계에서 차단.

---

## 8. Downstream Relay

### 8.1 `GatewayRouteDispatcher`

- `RestClient` POST `RouteContext.targetUrl`
- **Cookie** 헤더 downstream 전달
- **Authorization** (Bearer) 전달
- Timeout: Route 행 `CONNECT_TIMEOUT_MS` / `READ_TIMEOUT_MS` (기본 3000 / 5000)

### 8.2 응답 처리 (`GEF.success`)

- downstream `Set-Cookie` → 클라이언트 응답에 포함
- OM 로그인 성공 시 `GatewaySessionRegistry.tryRegisterOmLogin` — `TCF_USER_SESSION` 등록

### 8.3 Query 파라미터 (하위 호환)

`deploymentMode`, `bootrunHost`, `tomcatGatewayUrl` — **Target 결정에 미사용** (tcf-uj 전달용만).

---

## 9. Gateway vs 업무 TCF

| | Gateway | 업무 WAR TCF |
|---|---------|----------------|
| 라우팅 키 | `businessCode` | `header.serviceId` |
| 설정 | `TCF_GATEWAY_ROUTE` | Handler + OM Catalog |
| 로그 테이블 | `TCF_GATEWAY_TX_LOG` | `TCF_TX_LOG` |
| 인증 | 1차 (세션/JWT) | STF 세션·권한·거래통제 |
| 실패 HTTP | 401/404 등 Gateway JSON | 대부분 200 + `resultCode` |

동일 `guid`로 양쪽 로그 상관 — [44-observability.md](44-observability.md).

---

## 10. 오류·응답 Contract

| 상황 | GEF | HTTP | 비고 |
|------|-----|------|------|
| Route 미등록 | `routeNotFound` | 404 | JSON 오류 body |
| 인증 실패 | `authFail` | 401 | 세션/JWT |
| downstream 4xx/5xx | `httpError` | downstream 상태 | body 전달 |
| 연결 실패 | `connectionError` | 502/503 | Target DOWN |
| 정상 | `success` | downstream 상태 | Set-Cookie 유지 |

Gateway 오류는 **TCF StandardResponse가 아닐 수 있음** — 채널은 Gateway JSON·HTTP 상태로 처리.

---

## 11. Admin·운영 API

| URL | 용도 |
|-----|------|
| `/admin/routes.html` | Route 관리 UI |
| `/admin/sessions.html` | 세션 조회 |
| `/admin/transaction-log.html` | Gateway 거래 로그 |
| `/api/admin/routes` | Route REST CRUD |
| `/api/admin/user-sessions` | 사용자 세션 |

---

## 12. 설정 요약

`application-local.yml` 예:

```yaml
server:
  port: 8100
nsight:
  gateway:
    env-code: LOCAL
    route-table:
      cache-enabled: false
    auth:
      login-required: true
      jwt:
        enabled: true
        jwk-set-uri: http://127.0.0.1:8110/.well-known/jwks.json
        issuer: NSIGHT-AUTH
        audience: NSIGHT-MP
      session-validation:
        spring-session-check: true
        user-session-check: true
        header-user-check: true
    session-datasource:
      url: jdbc:h2:file:./data/nsight-txlog/nsight_om;...
```

프로파일별 SoT: `tcf-cicd/{local,dev,prod}/` — [25-env-profile.md](25-env-profile.md).

---

## 13. 운영 체크리스트

- [ ] `ENV_CODE`별 Route 전 업무코드 등록·Smoke
- [ ] `session-datasource` URL = tcf-om `SPRING_SESSION` DB
- [ ] JWT `jwk-set-uri` 가용 (tcf-jwt 기동)
- [ ] Target Health·`READ_TIMEOUT_MS` 여유
- [ ] Gateway TX Log ↔ 업무 `TCF_TX_LOG` guid 상관 샘플 확인
- [ ] Route seed 변경 시 `data/gateway-route` H2 갱신
- [ ] 운영 `login-required`·4단계 on (dev 완화 금지)

---

## 14. 현행 vs 목표 (Gap)

| 항목 | 현행 | 목표 |
|------|------|------|
| ztomcat 배포 | bootRun 위주 | `/gw` 통합 배포 옵션 |
| Multi-Target | 단일 Route 행 | WEIGHT·Health 기반 선택 |
| Denylist | JWT DB만 | Gateway 실시간 연동 |
| GitLab Pipeline | 스크립트 | 자동 deploy gw.war |
| OM Route 동기화 | Admin API | OM Handler 단일 SoT |

---

## 15. 관련 소스

| 경로 | 설명 |
|------|------|
| `tcf-gateway/.../GRF.java` | 파이프라인 진입 |
| `tcf-gateway/.../GSF.java` | Route·인증·enrich |
| `tcf-gateway/.../GatewayAuthenticationService.java` | JWT/세션 분기 |
| `tcf-gateway/.../GatewaySessionValidationService.java` | 4단계 세션 |
| `tcf-gateway/.../GatewayRouteDispatcher.java` | HTTP Relay |
| `tcf-gateway/docs/ROUTING_TABLE.md` | DDL·시드 |
| `tcf-gateway/build.gradle` | 독립 스택 (tcf-core 없음) |

---

← [50-test-architecture.md](50-test-architecture.md) · [52-om-operations.md](52-om-operations.md) →
