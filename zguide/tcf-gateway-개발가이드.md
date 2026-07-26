# tcf-gateway 개발자 가이드

> **역할:** TCF 온라인 전문 **단일 진입 API Gateway** · 라우팅·세션 관문  
> **포트:** **8100** · Context **`/gw`** (ztomcat)

---

## 1. 이 모듈이 하는 일

```
브라우저 (tcf-uj)
  → POST /{code}/online  (Cookie: JSESSIONID)
  → POST /api/gateway/om/online  (Authorization: Bearer — JWT 모드)
  → TCF_GATEWAY_ROUTE 조회 (ENV + BUSINESS_CODE)
  → downstream WAS  POST /{code}/online
```

- **세션을 소유하지 않음** — SESSIONDB(`SPRING_SESSION` + `TCF_USER_SESSION`) 검증·전달만
- **이중 인증** — `Bearer` 있으면 JWT(JWKS), 없으면 쿠키 4단계 검증. JWT **발급**은 tcf-jwt
- Target URL은 **라우팅 테이블만** 참조 (query 파라미터로 URL 결정 ❌)

---

## 2. 5분 빠른 시작

```bash
gradle :sv-service:bootRun     # 8086
gradle :tcf-om:bootRun         # 8097 (세션)
gradle :tcf-gateway:bootRun    # 8100

# 라우팅 목록
curl "http://localhost:8100/api/admin/routes?envCode=LOCAL"
```

전체 UI 테스트: [tcf-uj-개발가이드.md](./tcf-uj-개발가이드.md)

---

## 3. 실행

| | |
|---|---|
| bootRun local | `gradle :tcf-gateway:bootRun -PspringProfilesActive=local` |
| 스크립트 | `tcf-gateway/scripts/run-local.bat` |
| ztomcat | `POST http://localhost:8080/gw/sv/online` |

**메인:** `com.nh.nsight.gateway.NsightGatewayApplication`  
**라우팅 DB:** H2 `data/gateway-route` (local) / Oracle `TCF_GATEWAY_ROUTE` (운영)

---

## 4. 처리 흐름

```
*ProxyController
  → BusinessRouteService (facade)
    → GRF.forwardOnline
      → GSF.preProcess (라우팅 조회 + GatewayAuthenticationService)
      → GatewayRouteDispatcher (RestClient + Cookie·Authorization 전달)
      → GEF (응답 변환)
```

---

## 5. 관리 화면·API

| URL | 설명 |
|-----|------|
| `/admin/routes.html` | 라우팅 테이블 |
| `/admin/sessions.html` | TCF_USER_SESSION |
| `/admin/transaction-log.html` | Gateway 거래로그 |
| `/api/admin/routes` | REST CRUD |

---

## 6. LOCAL bootRun 라우팅 (시드)

| 업무 | Target |
|------|--------|
| IC | `http://127.0.0.1:8082/ic/online` |
| SV | `http://127.0.0.1:8086/sv/online` |
| OM | `http://127.0.0.1:8097/om/online` |
| JWT | `http://127.0.0.1:8110/online` |

전체: [tcf-gateway/README.md](../tcf-gateway/README.md)

---

## 7. 설정 핵심

```yaml
nsight:
  gateway:
    env-code: LOCAL          # LOCAL / DEV / PRD
    auth:
      login-required: true
      session-validation:    # 2~4단계 on/off
      jwt:                     # Bearer 검증 (발급은 tcf-jwt)
        enabled: false         # local 프로필: true
        jwk-set-uri: http://127.0.0.1:8110/.well-known/jwks.json
        issuer: NSIGHT-AUTH
        audience: NSIGHT-MP
    session-datasource:      # SPRING_SESSION (tcf-om H2 공유)
```

---

## 8. H2 시드 갱신

`data.sql` 변경 후 URL이 안 바뀌면:

```
data/gateway-route.mv.db 삭제 → Gateway 재기동
```

---

## 9. 참고

| | |
|---|---|
| [tcf-gateway/README.md](../tcf-gateway/README.md) | **상세** |
| [tcf-gateway/docs/ROUTING_TABLE.md](../tcf-gateway/docs/ROUTING_TABLE.md) | 라우팅 설계 |
| [tcf-uj-개발가이드.md](./tcf-uj-개발가이드.md) | Relay UI |
| [zman/09-Gateway라우팅.md](../zman/09-Gateway라우팅.md) | 설계 요약 |
