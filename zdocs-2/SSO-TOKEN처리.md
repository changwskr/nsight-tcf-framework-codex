# TCF SSO · JWT 토큰 처리 정리

NSIGHT TCF의 SSO 로그인은 **SSO 인증은 `tcf-om`이, JWT 발급은 `tcf-jwt`가 각각 책임**지고, 화면에는 **OM 세션과 JWT를 함께** 내려주는 구조입니다.

| 관점 | 역할 | 핵심 |
|------|------|------|
| **SSO 인증** | IdP 토큰 검증·사용자 조회 | `tcf-om` (`OM.Auth.ssoLogin`) |
| **OM 세션** | 로그인 상태 유지 | Spring Session JDBC + `SPRING_SESSION` |
| **JWT 발급** | Access/Refresh Token 생성·저장 | `tcf-jwt` (`JWT.Auth.ssoIssue`) |
| **화면 응답** | 세션 + 토큰 조립 | `tcf-om` |
| **이후 API** | 인증 수단 선택 | OM 세션 또는 `Authorization: Bearer` |
| **TCF 본체** | Header·거래통제·권한·로그·감사·Timeout | 기존 STF/BTF/ETF 유지 |

**핵심 원칙:** JWT는 TCF를 **대체하지 않습니다**. JWT는 **인증 수단**이고, TCF는 계속 Header 검증, 거래통제, 권한, 로그, 감사, Timeout을 담당합니다.

관련 문서: [로그인.md](로그인.md) · [세션관리.md](세션관리.md) · [솔루션환경구성.md](솔루션환경구성.md)

---

## 1. 최종 설계 결론

| 구분 | 설계 방향 |
|------|-----------|
| SSO 인증 검증 | `tcf-om`에서 수행 |
| OM 사용자 조회 | `tcf-om`의 `OM_USER` 기준 |
| OM 세션 생성 | `tcf-om`에서 `HttpSession` 생성 |
| 세션 저장 | Spring Session JDBC + SESSIONDB 기준 |
| JWT 토큰 생성 | `tcf-jwt`에서 수행 |
| SSO용 JWT 발급 거래 | `JWT.Auth.ssoIssue` 신규 추가 |
| 화면 응답 조립 | `tcf-om`에서 세션 정보 + JWT 토큰을 조립 |
| 이후 API 호출 | OM 세션 또는 JWT Bearer Token 사용 |
| Gateway 호출 | accessToken 기반 호출 가능 |
| 보안 핵심 | `tcf-om → tcf-jwt` 호출은 반드시 **내부 호출**로 제한 |

NSIGHT 세션관리 기준은 WAS 메모리 세션보다 **Spring Session JDBC + SESSIONDB 중심의 중앙 세션 관리**가 맞고, 모든 업무 WAR는 동일 SESSIONDB를 기준으로 세션을 검증하는 구조가 적합합니다.

---

## 2. 목표 아키텍처

```text
[화면 / WebTopSuite / DataEye / BI Portal]
        |
        | 1. SSO Login 클릭
        v
[SSO 서버 / IdP]
        |
        | 2. 사용자 인증
        | 3. SSO token / code / assertion 발급
        v
[화면]
        |
        | 4. OM.Auth.ssoLogin 호출
        v
[tcf-om]
        |
        | 5. SSO token 검증
        | 6. OM_USER 조회
        | 7. OM HttpSession 생성
        | 8. JWT.Auth.ssoIssue 내부 호출
        v
[tcf-jwt]
        |
        | 9. 내부 호출 검증
        | 10. userId 검증
        | 11. accessToken 생성
        | 12. refreshToken 생성
        | 13. JWT_TOKEN / REFRESH_TOKEN 저장
        v
[tcf-om]
        |
        | 14. OM 세션 + JWT 토큰 응답 조립
        v
[화면]
        |
        | 15. OM 세션 유지
        | 16. 필요 시 Authorization: Bearer accessToken으로 Gateway 호출
```

---

## 3. 역할 분리 기준

| 컴포넌트 | 책임 | 하지 말아야 할 일 |
|----------|------|-------------------|
| 화면 | SSO 로그인 시작, `OM.Auth.ssoLogin` 호출, 응답 토큰 보관 | SSO token 자체 검증, 사용자 권한 판단 |
| SSO 서버 / IdP | 사용자 인증, SSO assertion/code/token 발급 | OM 세션 생성, NSIGHT JWT 생성 |
| `tcf-om` | SSO 검증, `OM_USER` 조회, OM 세션 생성, JWT 발급 요청, 응답 조립 | JWT 서명키 직접 관리, refreshToken 저장 |
| `tcf-jwt` | Access/Refresh Token 생성, 토큰 저장, 토큰 폐기/갱신 관리 | SSO 원문 검증, OM 세션 생성 |
| `tcf-gateway` | JWT 또는 세션 확인, 업무코드 라우팅, Header 검증 | SSO 로그인 처리, 토큰 발급 |
| 업무 WAR | TCF 전처리에서 세션/JWT/권한/거래통제 확인 후 업무 처리 | 로그인/토큰 발급 중복 구현 |

NSIGHT TCF 구조는 모든 온라인 거래가 표준 전문으로 수신되고, STF 전처리에서 Header 검증, GUID/Trace 생성, 세션·권한·거래통제·Timeout을 수행한 뒤 `serviceId` 기준 Handler를 실행하는 구조입니다.

---

## 4. 신규 거래 정의

### 4.1 tcf-om 거래

| 항목 | 값 |
|------|-----|
| ServiceId | `OM.Auth.ssoLogin` |
| 거래코드 | `OM-AUT-0002` 또는 기존 기준에 맞춰 부여 |
| 역할 | SSO token 검증, OM 세션 생성, JWT 발급 요청 |
| 호출 주체 | 화면 |
| 인증 예외 여부 | 로그인성 거래이므로 사전 인증 예외 |
| 후속 처리 | `JWT.Auth.ssoIssue` 내부 호출 |

### 4.2 tcf-jwt 거래

| 항목 | 값 |
|------|-----|
| ServiceId | `JWT.Auth.ssoIssue` |
| 거래코드 | `JWT-AUT-0005` |
| 역할 | SSO 인증 완료 사용자를 대상으로 JWT token pair 발급 |
| 호출 주체 | `tcf-om` |
| 외부 호출 허용 여부 | **불가** |
| 내부 호출 검증 | **필수** |
| 저장 대상 | `JWT_TOKEN`, `REFRESH_TOKEN` |

---

## 5. `JWT.Auth.ssoIssue` 요청 전문

```json
{
  "userId": "admin01",
  "issuer": "OM-SSO",
  "ssoSubject": "admin01",
  "ssoAssertionId": "SSO-ASSERTION-20260629-000001"
}
```

권장 Header:

```http
X-NSIGHT-Internal-Call: true
X-NSIGHT-Internal-Service: tcf-om
X-NSIGHT-Internal-Signature: {HMAC signature}
X-GUID: {guid}
X-Trace-Id: {traceId}
```

---

## 6. `OM.Auth.ssoLogin` 응답 전문

```json
{
  "loggedIn": true,
  "loginType": "SSO",
  "sessionId": "A1B2C3D4E5F6",
  "userId": "admin01",
  "userName": "관리자",
  "branchId": "0001",
  "channelId": "WEBTOP",
  "accessToken": "...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

**운영 보안 기준**

- accessToken은 짧게, refreshToken은 서버 저장 방식으로 관리
- JWT에는 사용자ID, 지점, 채널, 권한그룹 등 **최소 정보만** 포함
- 고객정보, 계좌정보, 조회결과 같은 **민감 업무 데이터는 JWT에 넣으면 안 됨**

---

## 7. 내부 호출 보안 기준

`tcf-jwt`의 `JWT.Auth.ssoIssue`는 외부 화면이나 브라우저에서 직접 호출되면 안 됩니다. 반드시 `tcf-om`에서만 호출되어야 합니다.

| 보안 방식 | 적용 기준 |
|-----------|-----------|
| Shared Secret | 최소 필수. `tcf-om`과 `tcf-jwt`에 동일한 강한 secret 설정 |
| HMAC Signature | 요청 Body + timestamp + secret 기반 서명 |
| Timestamp 검증 | 1~3분 이내 요청만 허용 |
| Internal Header | `X-NSIGHT-Internal-Service=tcf-om` 확인 |
| IP Allowlist | `tcf-om` 서버 IP만 허용 |
| mTLS | 운영환경 권장 |
| Gateway 내부망 제한 | 외부망에서 `tcf-jwt` 직접 접근 차단 |
| Audit Log | 누가, 언제, 어떤 userId로 토큰을 발급했는지 기록 |

---

## 8. 토큰 저장 정책

| 항목 | Access Token | Refresh Token |
|------|--------------|---------------|
| 용도 | API 호출 인증 | Access Token 재발급 |
| 기본 만료 | 15분 | (정책에 따름) |
| 저장 위치 | `JWT_TOKEN` | `REFRESH_TOKEN` |
| 원문 저장 | 가능하면 지양, `jti` 중심 관리 | 원문 금지, Hash 저장 권장 |
| 폐기 관리 | `jti`, 만료시간, 상태값 | 동일 |
| 강제 로그아웃 | Access/Refresh 모두 폐기 | 동일 |
| 감사 대상 | 발급, 재발급, 폐기, 실패 이력 | 동일 |

---

## 9. 화면 저장 정책

| 저장 대상 | 권장 위치 | 설명 |
|-----------|-----------|------|
| OM 세션 | `JSESSIONID` Cookie | HttpOnly, Secure, SameSite 적용 |
| Access Token | 메모리 저장 우선 | XSS 위험을 줄이기 위해 LocalStorage 지양 |
| Refresh Token | 가능하면 HttpOnly Secure Cookie | 화면 JavaScript에서 직접 접근하지 않게 관리 |
| 사용자 기본정보 | 화면 상태 또는 세션 조회 API | 최소 정보만 유지 |
| 권한정보 | 서버 검증 우선 | 화면 권한은 표시 제어용, 최종 권한은 서버에서 검증 |

Spring Boot 운영 기준에서도 Cookie 보안, 세션 Timeout, Forward Header, 보안 설정, GUID/TraceId, Timeout을 `application.yml`에서 명확히 관리하도록 정의합니다. ([솔루션환경구성.md](솔루션환경구성.md))

---

## 10. 최종 처리 흐름

```text
1. 화면에서 SSO Login 클릭

2. SSO 서버가 사용자 인증 후 SSO token/code/assertion 발급

3. 화면이 OM.Auth.ssoLogin 호출

4. tcf-om이 SSO token 검증

5. tcf-om이 OM_USER 조회
   - 사용자 존재 여부
   - 사용 가능 상태
   - 권한그룹
   - 지점/부서
   - SSO subject 매핑 확인

6. tcf-om이 HttpSession 생성
   - userId
   - branchId
   - channelId
   - authGroup
   - loginType=SSO

7. tcf-om이 tcf-jwt의 JWT.Auth.ssoIssue 내부 호출

8. tcf-jwt가 내부 호출 검증
   - shared secret
   - internal header
   - timestamp
   - signature
   - IP allowlist 또는 mTLS

9. tcf-jwt가 accessToken / refreshToken 생성

10. tcf-jwt가 JWT_TOKEN / REFRESH_TOKEN 저장

11. tcf-jwt가 token pair를 tcf-om에 반환

12. tcf-om이 OM 세션 + JWT 토큰을 화면에 응답

13. 화면은 이후 API 호출 시 다음 중 하나 사용
    - OM 세션 기반 호출
    - Authorization: Bearer accessToken 기반 Gateway 호출
```

---

## 11. 설계 의사결정 포인트

| 의사결정 항목 | 권장안 |
|---------------|--------|
| SSO 검증 위치 | `tcf-om` |
| JWT 생성 위치 | `tcf-jwt` |
| `JWT.Auth.login` 재사용 여부 | **비권장** |
| SSO 전용 거래 추가 여부 | `JWT.Auth.ssoIssue` 추가 |
| OM 세션과 JWT 병행 여부 | **병행** |
| Gateway 인증 방식 | JWT 우선, 세션 보조 |
| Refresh Token 관리 | DB 저장 + Hash 저장 |
| 토큰 폐기 | `jti` 기준 폐기 |
| 내부 호출 보안 | shared secret + HMAC + mTLS 권장 |
| 거래통제 예외 | 로그인성 거래만 최소 예외 등록 |

---

## 12. 최종 권장안

**가장 좋은 구현 방식**은 `tcf-jwt`에 `JWT.Auth.ssoIssue` 서비스를 추가하고, `tcf-om`의 `OM.Auth.ssoLogin` 성공 시 해당 서비스를 내부 호출하여 accessToken과 refreshToken을 발급받은 뒤, OM 세션 정보와 함께 화면에 응답하는 구조입니다.

이 구조는 SSO 검증, 세션 생성, 토큰 발급, 화면 응답 책임을 명확히 분리할 수 있고, 향후 Gateway 기반 API 호출, 강제 로그아웃, 토큰 폐기, 감사로그, 운영관리 화면 확장에도 가장 안정적입니다.

---

## 13. 관련 serviceId 요약

| serviceId | 모듈 | 호출 주체 | 용도 |
|-----------|------|-----------|------|
| `OM.Auth.ssoLogin` | `tcf-om` | 화면 | SSO 검증 + 세션 생성 + JWT 발급 요청 |
| `JWT.Auth.ssoIssue` | `tcf-jwt` | `tcf-om` (내부) | SSO 완료 사용자 JWT pair 발급 |
| `OM.Auth.login` | `tcf-om` | 화면 | 일반 ID/PW 로그인 (기존) |
| `JWT.Auth.login` | `tcf-jwt` | 화면 | 일반 JWT 로그인 (기존, SSO와 분리) |
| `JWT.Auth.refresh` | `tcf-jwt` | 화면/Gateway | Access Token 재발급 |
| `JWT.Auth.revoke` | `tcf-jwt` | 화면/관리 | 토큰 폐기 |
