# 제26장. tcf-gateway · tcf-jwt

| 항목 | 내용 |
| --- | --- |
| **편** | 제9편 · 모듈별 레퍼런스 (Quick Start) |
| **에디션** | **Master** — 아키텍트·시니어·플랫폼 |
| **기반 원본** | [ztcfbook/제09편/26-tcf-gateway-jwt.md](../ztcfbook/제09편/26-tcf-gateway-jwt.md) |
| **입문서** | [ztcfbook-m](../ztcfbook-m/README.md) |
| **장** | 제26장 |
| **파일** | `제09편/26-tcf-gateway-jwt.md` |
| **상태** | Master Edition (ztcfbook-h) |
| **목차** | [00-목차](../00-목차.md) |

---

## 아키텍처 뷰

```mermaid
flowchart LR
  Client --> GW[tcf-gateway :8100]
  JWT[tcf-jwt :8110] -->|JWKS| GW
  GW -->|Proxy| WAR["/{bc}/online"]
```

---

## Master 해설

tcf-gateway GatewayRouteDispatcher와 AbstractBusinessProxyController 하위 SvProxyController 등은 businessCode별 downstream `/{bc}/online` HTTP 프록시입니다. tcf-jwt(:8110) JwkSetController가 제공하는 JWKS는 GatewayJwtValidator가 fetch·cache하여 Bearer 검증에 쓰며, ztomcat context /gw·/jwt는 bootRun root URL과 다릅니다.

GatewayRouteCatalog·ROUTING_TABLE.md·tcf-cicd Apache config 삼각 불일치가 404·502 burst의 최빈 원인입니다. Session+JWT 이중 인증 path에서 anonymous health check와 authenticated online 거래를 route table에서 분리해야 합니다.

JWT claim businessCode·userId가 StandardHeader에 매핑되는지 STF integration test로 검증하고, auth.jwt.enabled=false profile에서도 Gateway-only smoke를 유지합니다. JWT key rollover·JWKS cache TTL·Proxy read timeout vs OM TimeoutPolicy 불일치는 E-JWT-AUTH-* runbook과 연결됩니다.

운영 rotation 절차: key 발급→Gateway JWKS refresh→ztomcat /gw smoke→claim mapping regression.

---

## 구현 샘플 (코드베이스)

### GatewayRouteDispatcher

```java
package com.nh.nsight.gateway.client;

import com.nh.nsight.gateway.application.service.GatewayForwardResponse;
import com.nh.nsight.gateway.support.RouteContext;
import com.nh.nsight.gateway.support.GatewayProxyTrace;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class GatewayRouteDispatcher {
    private static final String PHASE = "GatewayRouteDispatcher.dispatch";
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 3000;
    private static final int DEFAULT_READ_TIMEOUT_MS = 5000;

    public GatewayForwardResponse dispatch(RouteContext context, String cookieHeader) {
        GatewayProxyTrace.start(PHASE);
        try {
            int connectTimeoutMs = effectiveTimeout(context.connectTimeoutMs(), DEFAULT_CONNECT_TIMEOUT_MS);
            int readTimeoutMs = effectiveTimeout(context.readTimeoutMs(), DEFAULT_READ_TIMEOUT_MS);
            GatewayProxyTrace.log(PHASE, "targetUrl=" + context.targetUrl()
                    + " connectTimeoutMs=" + connectTimeoutMs
                    + " readTimeoutMs=" + readTimeoutMs);
            GatewayProxyTrace.log(PHASE, "restClient.post");
            RestClient restClient = restClient(connectTimeoutMs, readTimeoutMs);
            return restClient.post()
                    .uri(URI.create(context.targetUrl()))
                    .headers(headers -> {
                        if (StringUtils.hasText(cookieHeader)) {
                            headers.set(HttpHeaders.COOKIE, cookieHeader);
                        }
                        if (StringUtils.hasText(context.authorizationHeader())) {
                            headers.set(HttpHeaders.AUTHORIZATION, context.authorizationHeader());
                        }
                    })
                    .body(context.enrichedBody())
                    .exchange((request, response) -> {
                        String responseBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                        List<String> setCookies = response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE);
                        return new GatewayForwardResponse(
                                response.getStatusCode().value(),
                                System.currentTimeMillis() - context.startedAtMillis(),
                                responseBody == null ? "" : responseBody,
                                setCookies
                        );
                    });
        } catch (RuntimeException e) {
```

원본: [`tcf-gateway/src/main/java/com/nh/nsight/gateway/client/GatewayRouteDispatcher.java`](../tcf-gateway/src/main/java/com/nh/nsight/gateway/client/GatewayRouteDispatcher.java)

### SvProxyController

```java
package com.nh.nsight.gateway.entry.web;

import com.nh.nsight.gateway.entry.facade.BusinessRouteService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sv")
public class SvProxyController extends AbstractBusinessProxyController {
    public SvProxyController(BusinessRouteService routeService) {
        super(routeService, "SV");
    }
}

```

원본: [`tcf-gateway/src/main/java/com/nh/nsight/gateway/entry/web/SvProxyController.java`](../tcf-gateway/src/main/java/com/nh/nsight/gateway/entry/web/SvProxyController.java)

### JwkSetController

```java
package com.nh.nsight.auth.jwt.entry.web;

import com.nimbusds.jose.jwk.JWKSet;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JwkSetController {
    private final JWKSet jwkSet;

    public JwkSetController(JWKSet jwtJwkSet) {
        this.jwkSet = jwtJwkSet;
    }

    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> jwks() {
        return jwkSet.toJSONObject(true);
    }
}

```

원본: [`tcf-jwt/src/main/java/com/nh/nsight/auth/jwt/entry/web/JwkSetController.java`](../tcf-jwt/src/main/java/com/nh/nsight/auth/jwt/entry/web/JwkSetController.java)

---

## Master Deep Dive — tcf-gateway · jwt

- ztomcat context `/gw`, `/jwt`
- GatewayRouteCatalog + ROUTING_TABLE.md
- ProxyController per BC — AbstractBusinessProxyController
- Session + JWT 이중 인증 path

### 아키텍트 체크리스트

- 상단 **구현 샘플**을 실제 코드와 대조한다.
- **심화 참고**와 ztcfbook 본문 절 번호를 매핑한다.
- 운영·배포 관점은 ztcfbook-h Master 블록을 우선 본다.

---

## 심화 참고 (Master)

- [zguide/tcf-gateway-개발가이드.md](../zguide/tcf-gateway-개발가이드.md)
- [zguide/tcf-jwt-개발가이드.md](../zguide/tcf-jwt-개발가이드.md)
- [docs/architecture/51-api-gateway.md](../docs/architecture/51-api-gateway.md)
- [tcf-gateway/docs/ROUTING_TABLE.md](../tcf-gateway/docs/ROUTING_TABLE.md)

---

## 26.1 tcf-gateway — API Gateway

| 항목 | 값 |
| --- | --- |
| 포트 | 8100 |
| Context (ztomcat) | `/gw` |
| 메인 | `com.nh.nsight.gateway.NsightGatewayApplication` |

### 역할

TCF 온라인 전문 **단일 진입 API Gateway** — 라우팅·세션 관문·프록시 거래로그.

```text
브라우저 (tcf-uj)
  → POST /{code}/online  (Cookie: JSESSIONID)
  → POST /api/gateway/om/online  (Bearer — JWT 모드)
  → TCF_GATEWAY_ROUTE 조회 (ENV + BUSINESS_CODE)
  → downstream WAS  POST /{code}/online
```

- **세션 비소유** — SESSIONDB 검증·전달만
- **이중 인증** — Bearer 있으면 JWT(JWKS), 없으면 쿠키 4단계 검증
- Target URL은 **라우팅 테이블만** 참조

### 처리 흐름

```text
*ProxyController
  → BusinessRouteService
    → GRF.forwardOnline
      → GSF.preProcess (라우팅 + GatewayAuthenticationService)
      → GatewayRouteDispatcher (RestClient + Cookie·Authorization 전달)
      → GEF (응답 변환)
```

### Quick Start

```bash
gradle :sv-service:bootRun
gradle :tcf-om:bootRun
gradle :tcf-gateway:bootRun

curl "http://localhost:8100/api/admin/routes?envCode=LOCAL"
# ztomcat: POST http://localhost:8080/gw/sv/online
```

### LOCAL 시드 라우팅

| 업무 | Target |
| --- | --- |
| IC | http://127.0.0.1:8082/ic/online |
| SV | http://127.0.0.1:8086/sv/online |
| OM | http://127.0.0.1:8097/om/online |
| JWT | http://127.0.0.1:8110/online |

### 관리 API

| URL | 설명 |
| --- | --- |
| `/admin/routes.html` | 라우팅 테이블 UI |
| `/api/admin/routes` | REST CRUD |

---

## 26.2 tcf-jwt — JWT 발급·갱신·폐기

| 항목 | 값 |
| --- | --- |
| 포트 | **8110** |
| Context | `/jwt` (bootRun) |

### 역할

- Access/Refresh Token **발급·갱신·폐기**
- JWKS 공개키 제공
- Denylist(폐기 Token) 관리
- Gateway·tcf-web JWT Filter가 **검증만** 수행 — 발급은 tcf-jwt

### Quick Start

```bash
gradle :tcf-jwt:bootRun

# Admin UI (tcf-ui Relay)
http://localhost:8099/jwt/admin/login.html
```

### JWT ↔ Gateway ↔ tcf-uj

```text
1) tcf-jwt: Token 발급
2) tcf-uj: Authorization: Bearer {accessToken}
3) tcf-gateway: JWKS 검증 → downstream Cookie/Header 전달
4) 업무 WAR: tcf-web JWT Filter (선택)
```

### 설정 핵심

```yaml
nsight:
  jwt:
    issuer: nsight-tcf
    access-token-ttl: 900s
    refresh-token-ttl: 86400s
```

상세: [znsight-man/79-TCF-GATEWAY-JWT-개발-매뉴얼.md](../../znsight-man/79-TCF-GATEWAY-JWT-개발-매뉴얼.md), [80-JWT-TCF-WEB-개발-매뉴얼.md](../../znsight-man/80-JWT-TCF-WEB-개발-매뉴얼.md)

---

## 26.3 운영 체크리스트

| 항목 | 확인 |
| --- | --- |
| Route DB | ENV별 TCF_GATEWAY_ROUTE 동기화 |
| 세션 | SESSIONDB JDBC, Gateway 4단계 검증 |
| JWT | JWKS URL, Token TTL, Denylist |
| Timeout | Gateway → downstream Timeout |
| 로그 | Gateway 거래로그 + downstream TCF_TX_LOG GUID 연계 |
| Smoke | tcf-uj → Gateway → SV/OM 대표 serviceId |

---

## 장 요약 (Master)

**tcf-gateway**는 채널의 단일 진입점으로 라우팅·세션 관문·프록시 로그를 담당하고, **tcf-jwt**는 Token 생명주기(발급·폐기·JWKS)를 담당합니다. 운영 아키텍처는 tcf-uj → Gateway → 업무 WAR 흐름이 표준이며, 로컬은 bootRun 8100/8110 또는 ztomcat `/gw`로 검증합니다.

> Master Edition: **아키텍처 뷰** → **Master 해설** → **구현 샘플** → **Master Deep Dive** → **심화 참고** 순으로 본문과 함께 읽는다.

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [제25장 tcf-om · tcf-ui · tcf-uj](./25-tcf-om-ui-uj.md) |
| → 다음 | [제27장 tcf-eai · tcf-cache · tcf-batch](./27-tcf-eai-cache-batch.md) |

---

## 출처 색인 · Master 확장

| 구분 | 경로 |
| --- | --- |
| ztcfbook-h | 본 파일 |
| ztcfbook | `../ztcfbook/제09편/26-tcf-gateway-jwt.md` |

### 원본 출처


| 절 | 출처 |
| --- | --- |
| 26.1 | [zguide/tcf-gateway-개발가이드.md](../../zguide/tcf-gateway-개발가이드.md), [docs/architecture/51-api-gateway.md](../../docs/architecture/51-api-gateway.md) |
| 26.2 | [zguide/tcf-jwt-개발가이드.md](../../zguide/tcf-jwt-개발가이드.md), [docs/architecture/42-jwt.md](../../docs/architecture/42-jwt.md) |
| 26.3 | [docs/NSIGHT-FINAL-ARCHITECTURE-DECISION.md](../../docs/architecture/NSIGHT-FINAL-ARCHITECTURE-DECISION.md) |
