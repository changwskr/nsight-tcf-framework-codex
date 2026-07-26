# tcf-gateway — 업무 라우팅 API Gateway

TCF 온라인 전문(`POST /{업무코드}/online`)을 단일 진입점에서 수신하고, **`TCF_GATEWAY_ROUTE` 라우팅 테이블**로 Target URL을 결정한 뒤 downstream 업무 WAS로 프록시합니다.

| 항목 | 값 |
|------|-----|
| Gradle 모듈 | `tcf-gateway` |
| 메인 클래스 | `com.nh.nsight.gateway.NsightGatewayApplication` |
| bootRun 포트 | **8100** |
| WAR | `gw.war` → ztomcat `/gw` |
| 라우팅 DB (로컬) | H2 `data/gateway-route` (`schema.sql` + `data.sql`) |
| 라우팅 DB (운영) | Oracle `TCF_GATEWAY_ROUTE` |

## 역할

`tcf-uj`(또는 기타 클라이언트)가 업무별 WAS에 직접 붙지 않고 gateway를 경유하도록 합니다.

```text
브라우저
  → tcf-uj (:8102)  POST /api/relay/{code}/online  (Cookie: JSESSIONID)
                 또는 POST /api/gateway/om/online   (Authorization: Bearer)
  → tcf-gateway (:8100 또는 /gw)  POST /{code}/online
       ↓ TCF_GATEWAY_ROUTE (ENV_CODE + BUSINESS_CODE)
  → downstream WAS  POST /{code}/online  (예: http://127.0.0.1:8086/sv/online)
```

> **세션 원칙:** Gateway는 **세션을 소유하지 않습니다.** SESSIONDB(`SPRING_SESSION` + `TCF_USER_SESSION`) 기반으로 검증·전달·통제하는 **관문** 역할만 수행합니다. 클라이언트 쿠키(`JSESSIONID`/`NSIGHTSID`)를 downstream에 그대로 전달하고, downstream `Set-Cookie`를 클라이언트에 반환합니다.

> **인증:** Gateway는 **이중 인증 경로**를 지원합니다. `Authorization: Bearer`가 있으면 **JWT 검증**(tcf-jwt JWKS), 없으면 SESSIONDB 쿠키 **4단계 검증**을 수행합니다. JWT **발급**은 tcf-jwt가 담당하며 Gateway는 발급하지 않습니다.

## 처리 흐름 (TCF 패턴)

```text
XxxProxyController
  → BusinessRouteService (entry/facade)
    → GRF.forwardOnline (support)
      → GSF.preProcess
          ① TCF_GATEWAY_ROUTE 조회 (ENV_CODE + BUSINESS_CODE) — 유일한 라우팅 기준
          ② 미등록 → 404 (GatewayRouteNotFoundException)
          ③ GatewayAuthenticationService (application/service)
               · Bearer 있음 → GatewayJwtValidator (JWKS, issuer/audience)
               · 없음       → GatewaySessionValidator (SESSIONDB 4단계)
          ④ GatewaySessionRequestEnricher (support — 세션/JWT claim 기준 header 보정)
      → GatewayRouteDispatcher.dispatch (client — RestClient POST + Cookie·Authorization 전달)
      → GEF.success / routeNotFound / authFail / httpError / connectionError (support)
      → GatewayTransactionLogRecorder (application/service — TCF_GATEWAY_TX_LOG)
```

업무별 개별 RouteService는 두지 않습니다. **`BusinessRouteService` 하나**가 `TCF_GATEWAY_ROUTE` 기준으로 모든 업무를 라우팅합니다.

## 세션 관문 (SESSIONDB)

Gateway는 **다수 업무 WAR·OM·다중 WAS·장애 전환** 환경에서 세션 기준을 일관되게 유지하기 위해 세션을 **소유하지 않고** SESSIONDB만 참조합니다. 현재 ztomcat의 전체 배포 대상은 16 WAR입니다.

```text
클라이언트 ──Cookie(JSESSIONID/NSIGHTSID)──► Gateway ──동일 Cookie 전달──► 업무 WAS
                ▲                                    │
                └── Set-Cookie (downstream 그대로) ──┘

Gateway 검증 (GSF):
  1단계  Cookie 존재 (JSESSIONID / NSIGHTSID)           [필수]
  2단계  SPRING_SESSION 존재·만료                       [권장]
  3단계  TCF_USER_SESSION STATUS (ACTIVE 등)            [권장]
  4단계  header.userId vs SESSIONDB userId               [Gateway 권장]

로그인 (GEF): OM.Auth.login 성공 시 TCF_USER_SESSION 등록만 수행 (HttpSession 미생성)
```

| DB | 역할 | Gateway 연결 |
|----|------|----------------|
| `SPRING_SESSION` | Spring Session 저장소 (업무 WAS 공유 SESSIONDB) | `nsight.gateway.session-datasource` |
| `TCF_USER_SESSION` | 사용자 세션 레지스트리 | gateway-route H2 / Oracle |

| 설정 | 설명 |
|------|------|
| `nsight.gateway.auth.session-validation.*` | 2~4단계 on/off |
| `nsight.gateway.session-datasource.url` | SPRING_SESSION 조회 DB (local: tcf-om H2 공유) |
| `server.servlet.session.tracking-modes: []` | Gateway 자체 JSESSIONID 미발급 |
| `nsight.gateway.user-session-sync.*` | TCF_USER_SESSION ↔ SPRING_SESSION 주기 동기화 (기본 10초) |

## JWT 관문 (Bearer, 선택)

`nsight.gateway.auth.jwt.enabled=true`일 때 `Authorization: Bearer` 토큰을 JWKS로 검증합니다.

```text
클라이언트 ──Authorization: Bearer <accessToken>──► Gateway ──동일 헤더──► 업무 WAS
                (+ Cookie 있으면 함께 전달)

Gateway JWT 검증 (GatewayJwtValidator):
  ① Bearer 추출
  ② JwtDecoder — RS256, issuer=NSIGHT-AUTH, audience=NSIGHT-MP
  ③ claim userId / branchId / channelId → GatewaySessionContext
  ④ (선택) header.userId vs JWT userId — header-user-strict=true 시

검증 후 GatewaySessionRequestEnricher가 header.userId 등을 JWT claim 기준으로 보정합니다.
```

| 설정 | 설명 | 기본 |
|------|------|------|
| `nsight.gateway.auth.jwt.enabled` | JWT 검증 on/off | `false` (local 프로필 `true`) |
| `nsight.gateway.auth.jwt.jwk-set-uri` | tcf-jwt JWKS URL | local: `http://127.0.0.1:8110/.well-known/jwks.json` |
| `nsight.gateway.auth.jwt.issuer` | issuer claim | `NSIGHT-AUTH` |
| `nsight.gateway.auth.jwt.audience` | audience claim | `NSIGHT-MP` |
| `nsight.gateway.auth.jwt.header-user-strict` | header.userId ≠ JWT userId 시 401 | `false` |

> **Denylist:** `TCF_TOKEN_DENYLIST`(tcf-jwt)는 Gateway에 **아직 연동되지 않음**. 폐기 토큰 차단은 추후 추가 예정.

로컬 JWT Gateway 테스트: `tcf-jwt`(:8110) + `tcf-gateway`(:8100, `local` 프로필) 기동 필요.

## 패키지 구조

| 패키지 | 주요 클래스 | 설명 |
|--------|-------------|------|
| `entry/web` | `*ProxyController`, `AbstractBusinessProxyController`, `Gateway*AdminController` | 업무별 `POST /{code}/online` · 관리 REST |
| `entry/facade` | `BusinessRouteService` | GRF 진입 파사드 |
| `application/service` | `GatewayRouteAdminService`, `GatewayAuthenticationService`, `GatewayJwtValidator`, `GatewaySessionRegistry`, `GatewayTransactionLogService` | 라우팅·인증·세션·거래로그 |
| `application/rule` | `GatewaySessionValidator`, `GatewayAuthExemptions` | SESSIONDB 세션 검증 |
| `application/scheduler` | `GatewayUserSessionSyncScheduler` | TCF_USER_SESSION ↔ SPRING_SESSION 동기화 |
| `client` | `GatewayRouteDispatcher` | downstream WAS RestClient |
| `config` | `GatewayProperties`, `GatewayJwtConfiguration`, `GatewaySecurityConfiguration` | gateway 설정·JWT·Security |
| `persistence/dao` | `GatewayRouteDao`, `UserSessionDao`, `GatewayTransactionLogDao` | JDBC Dao |
| `support` | `GRF`, `GSF`, `GEF`, `GatewayBusinessModules`, `GatewayProxyTrace` | GSF/GRF/GEF 파이프라인·유틸 |

## 라우팅 테이블 (TCF_GATEWAY_ROUTE)

### 조회 기준

```text
WHERE ENV_CODE = :envCode        ← nsight.gateway.env-code (LOCAL / DEV / PRD)
  AND BUSINESS_CODE = :businessCode
  AND USE_YN = 'Y'
```

### Target URL 조립

```text
TARGET_URL = TARGET_BASE_URL + CONTEXT_PATH + ONLINE_PATH
```

### 환경별 예시 (SV)

| ENV_CODE | Profile | 예시 Target URL |
|----------|---------|-----------------|
| LOCAL | `local` | `http://127.0.0.1:8086/sv/online` |
| DEV | `dev` | `http://dev-msa-b-service:9090/sv/online` |
| PRD | `prod` | `http://msa-b-service:9090/sv/online` |

### LOCAL bootRun 포트 (시드 기준)

> 아래 목록은 Gateway 라우팅 시드의 전체 카탈로그입니다. `CC`, `BC`처럼 현재 저장소에 대응 서비스 모듈이 없는 레거시·확장용 경로도 포함합니다.

| 업무 | 포트 | Target URL |
|------|------|------------|
| CC | 8081 | `http://127.0.0.1:8081/cc/online` |
| IC | 8082 | `http://127.0.0.1:8082/ic/online` |
| PC | 8083 | `http://127.0.0.1:8083/pc/online` |
| BC | 8084 | `http://127.0.0.1:8084/bc/online` |
| MS | 8085 | `http://127.0.0.1:8085/ms/online` |
| SV | 8086 | `http://127.0.0.1:8086/sv/online` |
| PD | 8087 | `http://127.0.0.1:8087/pd/online` |
| EB | 8089 | `http://127.0.0.1:8089/eb/online` |
| EP | 8090 | `http://127.0.0.1:8090/ep/online` |
| SS | 8093 | `http://127.0.0.1:8093/ss/online` |
| MG | 8096 | `http://127.0.0.1:8096/mg/online` |
| OM | 8097 | `http://127.0.0.1:8097/om/online` |
| JWT | 8110 | `http://127.0.0.1:8110/online` |

> DEV/PRD의 `msa-a-service`, `dev-msa-b-service` 등 호스트명은 K8s Service·DNS에서 해석됩니다. Gateway DB에는 URL만 등록합니다.

### 문서·시드·관리

| 구분 | 경로 |
|------|------|
| 설계 문서 | [docs/ROUTING_TABLE.md](docs/ROUTING_TABLE.md) |
| Oracle DDL | [sql/oracle/TCF_GATEWAY_ROUTE.sql](sql/oracle/TCF_GATEWAY_ROUTE.sql) |
| Oracle 시드 | [sql/oracle/TCF_GATEWAY_ROUTE_DATA.sql](sql/oracle/TCF_GATEWAY_ROUTE_DATA.sql) |
| H2 시드 | `src/main/resources/schema.sql`, `data.sql` |
| 관리 화면 | `http://localhost:8100/admin/routes.html`, `http://localhost:8100/admin/transaction-log.html`, `http://localhost:8100/admin/sessions.html` |
| REST API | `/api/admin/routes`, `/api/admin/transaction-log`, `/api/admin/sessions` |

관리 화면에서 **업무그룹·업무코드는 코드성 데이터**(select)로 선택하며, LOCAL 환경 신규 등록 시 bootRun Base URL이 자동 채워집니다.

`CONNECT_TIMEOUT_MS` / `READ_TIMEOUT_MS`는 `GatewayRouteDispatcher` Relay 시 **실제 HTTP timeout**으로 적용됩니다 (기본 3000/5000ms).

## 프록시 엔드포인트

| 업무코드 | Gateway 경로 | ProxyController |
|----------|--------------|-----------------|
| CC | `POST /cc/online` | `CcProxyController` |
| BC | `POST /bc/online` | `BcProxyController` |
| IC, PC, MS, SV, PD | `POST /{code}/online` | `*ProxyController` |
| OM, EB, EP, MG, SS | `POST /{code}/online` | `*ProxyController` |
| JWT | `POST /jwt/online` | `JwtProxyController` |

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `GET` | `/actuator/health` | 헬스체크 |
| `GET` | `/admin/routes.html` | 라우팅 테이블 관리 화면 |
| `GET` | `/admin/transaction-log.html` | Gateway 거래로그 조회 화면 |
| `GET` | `/admin/sessions.html` | TCF_USER_SESSION 세션 관리 화면 |
| `GET/POST/PUT/DELETE` | `/api/admin/routes` | 라우팅 REST API |
| `GET/DELETE` | `/api/admin/transaction-log` | 거래로그 REST API |
| `GET/DELETE` | `/api/admin/sessions` | 세션 REST API |
| `GET` | `/h2-console` | H2 콘솔 (로컬) |

### Query 파라미터 (하위 호환)

`tcf-uj` relay가 `deploymentMode`, `bootrunHost`, `tomcatGatewayUrl`을 전달할 수 있으나,
**Target URL 결정에는 사용하지 않습니다.** 라우팅은 `TCF_GATEWAY_ROUTE`만 참조합니다.

## 실행

### Profile별 스크립트

| 스크립트 | Profile | env-code | 설명 |
|----------|---------|----------|------|
| `scripts/run-local.bat` | `local` | LOCAL | 업무별 bootRun 포트 (H2 시드) |
| `scripts/run-dev.bat` | `dev` | DEV | DEV 라우팅 테이블 (K8s/Tomcat URL) |

Gradle 직접 실행 (동일 효과):

```bash
gradle :tcf-gateway:bootRun -PspringProfilesActive=local
gradle :tcf-gateway:bootRun -PspringProfilesActive=dev
```

### 로컬 bootRun 전체 테스트 순서

```bash
gradle :sv-service:bootRun    # 8086
gradle :tcf-om:bootRun        # 8097 (세션 발급)
tcf-gateway\scripts\run-local.bat   # 8100
gradle :tcf-uj:bootRun        # 8102
```

### 빌드·배포

```bash
tcf-gateway\scripts\build.bat          # gw.war
tcf-gateway\scripts\deploy.bat         # ztomcat /gw
```

Tomcat 호출 예:

```text
POST http://localhost:8080/gw/sv/online
Cookie: JSESSIONID=...
Content-Type: application/json
```

## tcf-uj 연동

```text
tcf-uj GatewayRelayService
  → bootRun: http://127.0.0.1:8100/{code}/online
  → Tomcat:  http://localhost:8080/gw/{code}/online
```

> **미경유:** `/api/updownload/*`는 gateway를 거치지 않고 tcf-om(8097)에 직접 연결합니다.

## 설정

### application.yml (공통)

```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/gateway-route;MODE=Oracle;AUTO_SERVER=TRUE

nsight:
  gateway:
    auth:
      login-required: true
      jwt:
        enabled: false
        issuer: NSIGHT-AUTH
        audience: NSIGHT-MP
```

### application-local.yml

```yaml
server:
  port: 8100

nsight:
  gateway:
    env-code: LOCAL
    route-table:
      cache-enabled: false
    auth:
      jwt:
        enabled: true
        jwk-set-uri: http://127.0.0.1:8110/.well-known/jwks.json
        issuer: NSIGHT-AUTH
        audience: NSIGHT-MP
```

### application-dev.yml

```yaml
server:
  port: 8100

nsight:
  gateway:
    env-code: DEV
    route-table:
      cache-enabled: true
      cache-ttl-seconds: 30
```

### application-prod.yml

```yaml
nsight:
  gateway:
    env-code: PRD
    route-table:
      cache-enabled: true
      cache-ttl-seconds: 60
```

## curl 예

```bash
# SV 온라인 (세션 — Cookie)
curl -X POST http://localhost:8100/sv/online \
  -H "Content-Type: application/json" \
  -H "Cookie: JSESSIONID=<세션값>" \
  -d @sv-service/src/main/resources/sample-requests/sv-sample-inquiry.json

# OM 온라인 (JWT — Bearer, local 프로필·tcf-jwt 기동 필요)
curl -X POST http://localhost:8100/om/online \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" \
  -d @tcf-om/src/main/resources/sample-requests/om-sample-inquiry.json

# 라우팅 메타
curl http://localhost:8100/api/admin/routes/meta

# LOCAL 라우팅 목록
curl "http://localhost:8100/api/admin/routes?envCode=LOCAL"
```

## H2 데이터 갱신

라우팅 시드(`data.sql`) 변경 후 기존 URL이 그대로면 Gateway 재기동 또는 H2 파일 삭제 후 재기동:

```text
data/gateway-route.mv.db   (프로젝트 루트)
```

## 관련 모듈

| 모듈 | README |
|------|--------|
| tcf-uj (Relay UI) | [tcf-uj/README.md](../tcf-uj/README.md) |
| tcf-jwt (JWT 발급·JWKS) | [tcf-jwt/README.md](../tcf-jwt/README.md) |
| TCF 처리 순서 | [docs/TCF_FRAMEWORK_GUIDE.md](../zdocs-1/TCF_FRAMEWORK_GUIDE.md) |
