# tcf-jwt — JWT 인증·토큰 발급 서비스

RS256 JWT Access/Refresh Token 발급·갱신·폐기를 담당하는 TCF 독립 실행 모듈입니다. **검증(Resource Server)** 은 Gateway(`tcf-gateway`) 또는 각 업무 WAR가 JWKS를 참조해 수행합니다.

| 항목 | 값 |
|------|-----|
| Gradle 모듈 | `tcf-jwt` |
| 업무코드 | `JWT` |
| 메인 클래스 | `com.nh.nsight.auth.jwt.NsightJwtServiceApplication` |
| bootRun 포트 | **8110** (local — gateway :8100 과 분리) |
| WAR | `jwt.war` → ztomcat `/jwt` |

## 역할 분리

| 역할 | 담당 | 비고 |
|------|------|------|
| 토큰 **발급**·갱신·폐기 | **tcf-jwt** | `JWT.Auth.*` |
| JWKS **제공** | **tcf-jwt** | `GET /.well-known/jwks.json` |
| Bearer **검증** (Gateway) | **tcf-gateway** | `nsight.gateway.auth.jwt.*` |
| SSO 후 JWT pair 요청 | **tcf-om** → tcf-jwt | `JWT.Auth.ssoIssue` (내부 호출) |

## ServiceId

| serviceId | 설명 |
|-----------|------|
| `JWT.Auth.login` | 로그인 → Access + Refresh Token 발급 |
| `JWT.Auth.ssoIssue` | SSO 완료 사용자 JWT pair 발급 (`tcf-om` 내부 호출 전용) |
| `JWT.Auth.refresh` | Refresh Token 갱신 (Rotation) |
| `JWT.Auth.revoke` | Access Token 폐기 (Denylist) |
| `JWT.Auth.logout` | Access + Refresh Token 폐기 |

## 토큰 정책·Claim

| 항목 | 기본값 |
|------|--------|
| 알고리즘 | RS256 |
| issuer | `NSIGHT-AUTH` |
| audience | `NSIGHT-MP` |
| Access 유효기간 | 15분 |
| Refresh 유효기간 | 8시간 |

Access Token claim (최소): `userId`, `branchId`, `channelId`, `authGroupId`, `jti` — StandardRequest Header와 일치해야 합니다.

## 테이블

- `TCF_JWT_TOKEN` — Access Token 이력
- `TCF_REFRESH_TOKEN` — Refresh Token (해시 저장)
- `TCF_TOKEN_DENYLIST` — 폐기 JTI 목록 (tcf-jwt에서 관리; **Gateway denylist 연동은 미구현**)
- `OM_USER` — 사용자 원장 (JWT 발급 시 조회)

## 실행

```bash
# 스크립트 (모듈 scripts/)
tcf-jwt\scripts\build.bat
tcf-jwt\scripts\build.bat run
tcf-jwt\scripts\run-local.bat
tcf-jwt\scripts\deploy.bat

gradle :tcf-jwt:bootRun
curl -X POST http://localhost:8110/online \
  -H "Content-Type: application/json" \
  -d @tcf-jwt/src/main/resources/sample-requests/jwt-auth-login.json
```

## JWK (Resource Server용)

`GET /.well-known/jwks.json` — RS256 공개키

| 소비자 | 설정 |
|--------|------|
| tcf-gateway (local) | `nsight.gateway.auth.jwt.jwk-set-uri: http://127.0.0.1:8110/.well-known/jwks.json` |
| Gateway 경유 호출 | `POST http://localhost:8100/jwt/online` (라우팅만, 검증은 별도) |

로컬 테스트 계정: `admin01` / `nsight01!`

## 패키지 구조

```text
com.nh.nsight.auth.jwt
├── NsightJwtServiceApplication      # extends NsightWarBootstrap
├── application/service/     JwtAuthService, JwtAdminService
├── config/                  JwtKeyConfiguration, JwtSecurityConfiguration, JwtSchemaInitializer
├── entry/
│   ├── handler/       JwtAuthLoginHandler, JwtAuthRefreshHandler, JwtAuthRevokeHandler, …
│   ├── facade/        JwtAuthFacade, JwtAdminFacade
│   └── web/           JwkSetController (/.well-known/jwks.json)
├── persistence/
│   ├── dao/           JwtTokenDao, JwtAdminDao
│   └── mapper/        JwtTokenMapper, JwtAdminMapper
└── support/           JwtTokenIssuer, JwtTokenStore, JwtSupport

처리 흐름: entry/handler → entry/facade → application/service → persistence
```

## 관련 모듈

| 모듈 | README |
|------|--------|
| tcf-gateway (JWT 검증) | [tcf-gateway/README.md](../tcf-gateway/README.md) |
| tcf-om (SSO → ssoIssue) | [tcf-om/README.md](../tcf-om/README.md) |
| SSO·토큰 처리 설계 | [zdoc/SSO-TOKEN처리.md](../zdocs-2/SSO-TOKEN처리.md) |
