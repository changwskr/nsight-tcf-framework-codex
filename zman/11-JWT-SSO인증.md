# 11장. JWT / SSO 인증 구조 — 설명

## 설계서 절 목차

11.1~11.3 개요 · 11.4~11.6 SSO·JWT 흐름 · 11.7~11.9 Claim·Token정책 · 11.10~11.12 Gateway·STF · 11.13~11.14 테이블·설정 · 11.15~11.19 오류·보안·운영

---

## 핵심 결론

> **SSO = 인증** · **JWT = 결과 전달** · **TCF = 거래 허용 판단**

JWT는 TCF **대체 아님**.

---

## 전체 흐름 (11.3)

```
1. WebTopSuite → SSO IdP
2. SSO Token → 화면
3. POST /om/online  OM.Auth.ssoLogin
4. tcf-om: Token 검증, OM_USER, HttpSession → SESSIONDB
5. tcf-om → tcf-jwt  JWT.Auth.ssoIssue (Internal Call)
6. Access + Refresh Token
7. 화면: Cookie + Authorization: Bearer
8. Gateway/업무 WAR → STF: Claim ↔ Header, 권한, 거래통제
```

## 구성요소 (11.4)

| 구성 | 역할 |
|------|------|
| IdP/SSO | 최초 인증 |
| tcf-om | SSO 검증, 세션, 사용자 |
| tcf-jwt | Token 발급·JWKS (8110, /jwt) — **검증은 tcf-gateway** |
| STF | Header+JWT+세션 교차검증 |

## Token 정책 (11.8)

| Token | 정책 |
|-------|------|
| Access | 단기, Bearer API |
| Refresh | DB 저장, 갱신 |

## Claim 표준 (11.7)

userId, branchId, channelId, roles — **StandardRequest Header와 일치** 필수

## 인증 방식 (11.9)

| 시나리오 | 수단 |
|----------|------|
| 브라우저 업무 | Cookie (JSESSIONID) |
| API/Gateway | Bearer JWT |
| OM Admin | Cookie + (선택) JWT |

## Internal Call (tcf-om → tcf-jwt)

```
X-Internal-Call, X-Internal-Service, Timestamp, Signature
→ JwtInternalCallValidator
```

구현: `OmJwtSsoClient`, `NsightInternalCallSupport`

## ServiceId (11.12)

| serviceId | 모듈 |
|-----------|------|
| OM.Auth.login, ssoLogin, logout | tcf-om / OmAuthHandler |
| JWT.Auth.ssoIssue, login, refresh, revoke, logout | tcf-jwt |

## 테이블 (11.13)

- TCF_JWT_TOKEN, TCF_REFRESH_TOKEN, TCF_TOKEN_DENYLIST

## 인증 오류 (11.15)

- Token 만료·서명 실패
- Claim ≠ Header user/branch
- E-TCF-AUTH-*, E-OM-SSO-*

## 보안 운영 (11.16)

- Refresh DB, Denylist
- HTTPS, Secret 외부화 (`application-jwt.yml`)
- Internal Call 서명 필수

## 코드베이스

- `tcf-jwt/` — JWT 모듈
- `tcf-om/client/OmJwtSsoClient.java`
- `zdoc/SSO-TOKEN처리.md`, `zdoc/로그인.md`

## 이전 · 다음

← [10장 세션](./10-세션관리.md) · [12장 OM](./12-OM운영관리.md) →
