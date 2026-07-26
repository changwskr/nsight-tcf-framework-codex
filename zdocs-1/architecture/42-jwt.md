# 42. JWT 인증 아키텍처

| 항목 | 내용 |
|------|------|
| 문서 번호 | 42 |
| 제목 | JWT Authentication Architecture |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [10-session.md](10-session.md), [11-login.md](11-login.md), [33-TCF.md](33-TCF.md), [34-STF.md](34-STF.md) |
| 상세 매뉴얼 | [79-TCF-GATEWAY-JWT-개발-매뉴얼.md](../../znsight-man/79-TCF-GATEWAY-JWT-개발-매뉴얼.md), [80-JWT-TCF-WEB-개발-매뉴얼.md](../../znsight-man/80-JWT-TCF-WEB-개발-매뉴얼.md) |
| 구현 모듈 | `tcf-jwt`, `tcf-gateway`, `tcf-web`, `tcf-core`, `tcf-om`, `tcf-ui` |
| 대상 | Gateway·업무 WAR·프레임워크 개발자 |

---

## 1. 개요

NSIGHT TCF에서 JWT는 **업무 실행 권한을 대체하지 않는다**. JWT는 “누가 요청했는지”를 증명하고, TCF STF는 **전문 Header·권한·거래통제·Timeout**으로 “그 거래를 실행해도 되는지”를 최종 판단한다.

| 역할 | 담당 |
|------|------|
| 토큰 발급·갱신·폐기 | `tcf-jwt` (RS256, JWKS) |
| Gateway 1차 검증 | `tcf-gateway` (`GatewayJwtValidator`) |
| 업무 WAR 1차 검증 (Gateway 우회 대비) | `tcf-web` (`TcfJwtAuthenticationFilter`) |
| Claim vs Header 2차 정합성 | `tcf-core` STF (`AuthenticationContextValidator`) |
| SSO·세션 원천 | `tcf-om` + Spring Session JDBC |

핵심 원칙:

1. **Bearer Header만** 검증 대상 — Body에 토큰을 넣지 않는다.
2. **JwtDecoder는 HTTP 계층**(`tcf-gateway`, `tcf-web`)에 둔다. `tcf-core`는 전문 엔진이다.
3. Gateway를 우회해 `/{businessCode}/online`에 직접 들어올 수 있으면 **업무 WAR도 JWT 검증**이 필요하다.

---

## 2. 전체 처리 흐름

```text
[화면 tcf-ui]
  │ SSO / OM.Auth.login 또는 OM.Auth.ssoLogin
  ▼
[tcf-om] ──(내부 호출)──► [tcf-jwt] Access/Refresh 발급, JWKS
  │
  │ 업무 호출: Authorization: Bearer {accessToken}
  ▼
[tcf-gateway]  (선택 경로)
  │ GatewayAuthenticationService
  │   OM + jwt.enabled → Bearer 필수
  │   기타 업무 + Bearer → JWT 검증
  │   그 외 → SESSIONDB 4단계 세션 검증
  ▼
[업무 WAR / tcf-om]
  │ TcfJwtAuthenticationFilter  (/online, nsight.tcf.web.jwt.enabled=true)
  │   JwtDecoder → AuthenticatedUserContext + AuthenticationContextHolder
  ▼
OnlineTransactionController → TCF.process()
  ▼
STF.preProcess()
  │ SessionValidator / AuthorizationValidator
  │ AuthenticationContextValidator  (JWT claim ↔ StandardHeader)
  │ 거래통제 · Timeout · 멱등성
  ▼
TransactionDispatcher → Handler
```

로그 키워드 (로컬 디버그):

| 구간 | 로그 prefix |
|------|-------------|
| Gateway JWT | `******* [GW-JWT]` |
| Gateway 인증 분기 | `******* [GW-AUTH-JWT]` |
| 업무 WAR Filter | `******* [TCF-WEB-JWT]` |
| STF 2차 정합성 | `******* [TCF-AUTH-CTX]` |

---

## 3. 모듈별 책임

### 3.1 `tcf-jwt`

- Access Token / Refresh Token 발급 (`JWT.Auth.*`)
- RS256 서명, `/.well-known/jwks.json` 공개키 제공
- Refresh Token은 DB Hash 저장 (원문 저장 금지)
- jti 기준 Denylist·강제 폐기 (OM 연계)

### 3.2 `tcf-gateway`

`GatewayAuthenticationService` 인증 분기:

| 조건 | 동작 |
|------|------|
| `auth.login-required=false` | 검증 생략 |
| 로그인 면제 `serviceId` | 검증 생략 |
| `jwt.enabled` + `businessCode=OM` | **Bearer JWT 필수** |
| `jwt.enabled` + Bearer 존재 | JWT 검증 |
| 그 외 | `GatewaySessionValidator` (쿠키·SESSIONDB) |

구현: `GatewayJwtValidator` — `JwtDecoder`, iss/aud/userId 검증.

### 3.3 `tcf-web`

`TcfJwtAuthenticationFilter` (`nsight.tcf.web.jwt.enabled=true`):

| 항목 | 값 |
|------|-----|
| 대상 URI | `*/online` (actuator/health 제외) |
| Header | `Authorization: Bearer` (설정 가능) |
| 성공 시 | `AuthenticatedUserContextHolder` + `AuthenticationContextHolder` |
| 실패 시 | HTTP 401 JSON (`E-JWT-AUTH-*`) |

Gateway를 통과하지 않고 Tomcat에 직접 붙는 환경에서 **정문 경비** 역할.

### 3.4 `tcf-core` (STF)

`AuthenticationContextValidator` — **2차 검증**:

| claim | Header 필드 | 불일치 시 |
|-------|-------------|-----------|
| `userId` | `header.userId` | `E-JWT-AUTH-0009` (`JWT_HEADER_CLAIM_MISMATCH`) |
| `branchId` | `header.branchId` | 동일 |
| `channelId` | `header.channelId` | 동일 |

- `nsight.tcf.authentication-context-validation-enabled` (기본 `true`)
- 앞단에서 인증 컨텍스트가 없으면 skip (세션-only 거래 호환)
- `jti`는 `TransactionContext`·MDC에 기록

`SessionValidator` / `AuthorizationValidator`는 기존과 같이 Header 존재 여부를 확인한다. JWT 도입 후에도 **권한·거래통제는 STF 책임**이다.

---

## 4. 설정 요약

### 4.1 Gateway (`application-local.yml` 예)

```yaml
nsight:
  gateway:
    auth:
      login-required: true
      jwt:
        enabled: true
        jwk-set-uri: http://127.0.0.1:8110/.well-known/jwks.json
        issuer: ...
        audience: ...
```

### 4.2 업무 WAR (`tcf-web`)

```yaml
nsight:
  tcf:
    web:
      jwt:
        enabled: true
        required-for-online: true
        jwk-set-uri: http://127.0.0.1:8110/.well-known/jwks.json
    authentication-context-validation-enabled: true
```

로컬 포트 참고: `tcf-jwt` **8110**, `tcf-gateway` **8100**, `tcf-ui` **8099**.

---

## 5. 세션 vs JWT

| 관점 | 세션 (Cookie) | JWT (Bearer) |
|------|---------------|--------------|
| 저장 | `SPRING_SESSION` | 클라이언트 보관 (메모리/HttpOnly) |
| Gateway 검증 | SESSIONDB 4단계 | `JwtDecoder` |
| OM Admin (로컬) | Relay + Cookie | Gateway + Bearer (`om-admin.js`) |
| 업무 WAR | Filter 미사용 시 Header만 검증 | Filter + STF 2차 정합성 권장 |

두 방식은 **공존**한다. Gateway는 Bearer가 있으면 JWT, 없으면 세션으로 분기한다.

---

## 6. 오류 코드 (JWT 관련)

| 코드 | 발생 구간 | 의미 |
|------|-----------|------|
| `E-JWT-AUTH-0001` | tcf-web Filter | Bearer 없음 |
| `E-JWT-AUTH-0004` | tcf-web Filter | 토큰 decode/서명 실패 |
| `E-JWT-AUTH-0008` | tcf-web Filter | userId claim 없음 |
| `E-JWT-AUTH-0009` | tcf-core STF | claim vs Header 불일치 |
| `E-JWT-JWKS-0001` | tcf-web Filter | JwtDecoder 미구성 |

Gateway는 HTTP 401 + 메시지 본문으로 거부한다.

---

## 7. 구현 시 주의

1. **토큰 원문 로그 금지** — userId, jti, serviceId 수준만.
2. **Body JWT 금지** — `Authorization` Header만.
3. **tcf-core에 JwtDecoder 직접 배치 금지** — Filter/ Gateway에 두고 STF는 정합성만.
4. **Denylist** — Gateway 연동은 설계상 준비; 운영 연동 전 README·매뉴얼 확인.
5. 상세 API·claim 표·OM 화면: [79-TCF-GATEWAY-JWT-개발-매뉴얼.md](../../znsight-man/79-TCF-GATEWAY-JWT-개발-매뉴얼.md).

---

## 8. 관련 소스 (우선 참조)

| 파일 | 역할 |
|------|------|
| `tcf-jwt/.../JwtAuthService.java` | 토큰 발급 |
| `tcf-gateway/.../GatewayAuthenticationService.java` | Gateway 인증 분기 |
| `tcf-gateway/.../GatewayJwtValidator.java` | JWT decode·claim 검증 |
| `tcf-web/.../TcfJwtAuthenticationFilter.java` | 업무 WAR 1차 JWT |
| `tcf-core/.../support/security/AuthenticationContextValidator.java` | STF 2차 정합성 |
| `tcf-core/.../support/processor/STF.java` | 전처리 파이프라인 |
| `tcf-ui/.../om-admin.js` | OM Admin Bearer 호출 |

---

← [41-service-timeout-policy.md](41-service-timeout-policy.md) · [43-security-operations.md](43-security-operations.md) →
