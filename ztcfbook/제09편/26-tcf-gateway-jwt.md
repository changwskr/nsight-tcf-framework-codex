# 제26장. tcf-gateway · tcf-jwt

| 항목 | 내용 |
| --- | --- |
| **편** | 제9편 · 모듈별 레퍼런스 (Quick Start) |
| **장** | 제26장 |
| **파일** | `제09편/26-tcf-gateway-jwt.md` |
| **상태** | 집필 완료 |
| **목차** | [00-목차](../00-목차.md) |

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

## 장 요약

**tcf-gateway**는 채널의 단일 진입점으로 라우팅·세션 관문·프록시 로그를 담당하고, **tcf-jwt**는 Token 생명주기(발급·폐기·JWKS)를 담당합니다. 운영 아키텍처는 tcf-uj → Gateway → 업무 WAR 흐름이 표준이며, 로컬은 bootRun 8100/8110 또는 ztomcat `/gw`로 검증합니다.

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [제25장 tcf-om · tcf-ui · tcf-uj](./25-tcf-om-ui-uj.md) |
| → 다음 | [제27장 tcf-eai · tcf-cache · tcf-batch](./27-tcf-eai-cache-batch.md) |

---

## 출처 색인

| 절 | 출처 |
| --- | --- |
| 26.1 | [zguide/tcf-gateway-개발가이드.md](../../zguide/tcf-gateway-개발가이드.md), [docs/architecture/51-api-gateway.md](../../docs/architecture/51-api-gateway.md) |
| 26.2 | [zguide/tcf-jwt-개발가이드.md](../../zguide/tcf-jwt-개발가이드.md), [docs/architecture/42-jwt.md](../../docs/architecture/42-jwt.md) |
| 26.3 | [docs/NSIGHT-FINAL-ARCHITECTURE-DECISION.md](../../docs/architecture/NSIGHT-FINAL-ARCHITECTURE-DECISION.md) |
