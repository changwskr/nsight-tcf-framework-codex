# tcf-jwt 개발자 가이드

> **JWT 인증·토큰 발급** · 포트 **8110** · 업무코드 **`JWT`**

---

## 1. 이 모듈이 하는 일

RS256 JWT **Access/Refresh Token** 발급·갱신·폐기. Gateway 세션과 **별도** 인증 경로.

| serviceId | 설명 |
|-----------|------|
| `JWT.Auth.login` | 로그인 → Token pair |
| `JWT.Auth.ssoIssue` | SSO 완료 사용자 JWT (tcf-om 내부 호출) |
| `JWT.Auth.refresh` | Refresh Rotation |
| `JWT.Auth.revoke` | Access 폐기 (Denylist) |
| `JWT.Auth.logout` | Access + Refresh 폐기 |

---

## 2. 5분 빠른 시작

```bash
gradle :tcf-jwt:bootRun

curl -X POST http://localhost:8110/online ^
  -H "Content-Type: application/json" ^
  -d @tcf-jwt/src/main/resources/sample-requests/jwt-auth-login.json
```

JWK: `GET http://localhost:8110/.well-known/jwks.json`  
테스트 계정: `admin01` / `nsight01!`

---

## 3. 실행

| | |
|---|---|
| bootRun | `gradle :tcf-jwt:bootRun` |
| 스크립트 | `tcf-jwt/scripts/run-local.bat` |
| ztomcat | `http://localhost:8080/jwt/online` |
| Gateway 경유 | `POST http://localhost:8100/jwt/online` |

**메인:** `com.nh.nsight.auth.jwt.NsightJwtServiceApplication`

---

## 4. 테이블

| 테이블 | 역할 |
|--------|------|
| `TCF_JWT_TOKEN` | Access 이력 |
| `TCF_REFRESH_TOKEN` | Refresh (해시) |
| `TCF_TOKEN_DENYLIST` | 폐기 JTI |
| `OM_USER` | 사용자 원장 |

---

## 5. 패키지 구조

```
com.nh.nsight.auth.jwt
├── entry/handler/    JwtAuthLoginHandler, JwtAuthRefreshHandler, …
├── entry/web/        JwkSetController
├── application/service/ JwtAuthService
└── support/          JwtTokenIssuer, JwtTokenStore
```

---

## 6. tcf-om · Gateway 연동

- tcf-om: `OmJwtSsoClient` → `JWT.Auth.ssoIssue`
- Gateway: `JwtProxyController` → `POST /jwt/online` (라우팅)
- Gateway JWT **검증**: `GatewayAuthenticationService` + `GatewayJwtValidator` — [tcf-gateway-개발가이드.md](./tcf-gateway-개발가이드.md)
- tcf-ui / tcf-uj: `/jwt/admin/*` JWT Admin UI

---

## 7. 참고

| | |
|---|---|
| [tcf-jwt/README.md](../tcf-jwt/README.md) | |
| [tcf-om-개발가이드.md](./tcf-om-개발가이드.md) | SSO |
| [tcf-gateway-개발가이드.md](./tcf-gateway-개발가이드.md) | JWT 프록시 |
| [zdoc/로그인.md](../zdoc/로그인.md) | 인증 흐름 |
