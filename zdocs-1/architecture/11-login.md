# 11. 로그인 아키텍처

| 항목 | 내용 |
|------|------|
| 문서 번호 | 11 |
| 제목 | Login Architecture |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [10-session.md](10-session.md), [04-messaging.md](04-messaging.md), [05-exception.md](05-exception.md) |
| 구현 모듈 | `tcf-ui`, `tcf-uj`, `tcf-om`, `tcf-core`, `tcf-jwt`, `tcf-gateway` |
| 대상 | OM 포털/프레임워크 개발자 |

---

## 1. 개요

NSIGHT OM 로그인은 **TCF 표준 거래(`OM.Auth.login`) + Spring Session JDBC** 조합으로 동작한다.

| 계층 | 역할 |
|------|------|
| `tcf-ui` | 로그인 UI + Relay (`/api/relay/OM/online`, JWT 시 `/api/gateway/om/online`) |
| `tcf-om` | 인증 처리, 세션 생성/조회/종료 |
| `tcf-core` | STF/Dispatcher/ETF 표준 거래 파이프라인 |

핵심 특징:

1. 로그인도 일반 거래와 동일하게 `serviceId` 기반 처리
2. 세션 상태는 `SPRING_SESSION`에 저장
3. 브라우저 쿠키(`JSESSIONID`)는 UI Relay를 통해 투명 전달

---

## 2. 전체 로그인 흐름

```text
Browser (login.html)
  │  userId/password 입력
  ▼
om-admin.js login()
  │ POST /api/relay/OM/online
  │ body: { header(OM.Auth.login), body(userId,password) }
  ▼
tcf-ui TcfApiController / TransactionRelayService
  │ Cookie 전달, Set-Cookie 역전달
  ▼
tcf-om /om/online
  ▼
TCF.process()
  → STF
  → Dispatcher(OM.Auth.login → OmAuthHandler)
  → Facade/Service(OmAuthService.login)
  → ETF
  ▼
StandardResponse + sessionId
  ▼
Browser sessionStorage 저장 + JSESSIONID 쿠키 유지
```

---

## 3. 거래 식별자와 서비스 카탈로그

로그인 관련 표준 거래:

| serviceId | transactionCode | Handler | 용도 |
|-----------|-----------------|---------|------|
| `OM.Auth.login` | `OM-AUT-0002` | `OmAuthHandler` | 로그인 |
| `OM.Auth.logout` | `OM-AUT-0003` | `OmAuthHandler` | 로그아웃 |
| `OM.Auth.session` | `OM-AUT-0004` | `OmAuthHandler` | 현재 세션 조회 |

위 거래는 `OM_SERVICE_CATALOG`에 등록되어 운영/권한/타임아웃 정책과 함께 관리된다.

---

## 4. UI 레이어 아키텍처 (`tcf-ui`)

## 4.1 로그인 화면

`/om/admin/login.html`:

- 페이지 진입 시 `OmAdmin.call('authSession')`로 기존 로그인 상태 확인
- 이미 로그인 상태면 세션 화면으로 리다이렉트
- 로그인 제출 시 `OmAdmin.login(userId, password)` 호출

## 4.2 로그인 요청 조립 (`om-admin.js`)

요청 헤더:

- `businessCode: OM`
- `serviceId: OM.Auth.login`
- `transactionCode: OM-AUT-0002`
- `processingType: EXECUTE`
- `channelId: WEBTOP`

요청 바디:

```json
{ "userId": "...", "password": "..." }
```

성공 시:

- `payload.body.loggedIn === true` 확인
- `sessionStorage(nsight.om.session)`에 `userId/userName/branchId/authGroupId/sessionId` 저장

실패 시:

- `result.errorMessage`를 UI 에러 메시지로 표시

---

## 5. Relay 쿠키 전파 아키텍처

`TransactionRelayService`는 로그인 쿠키를 다음 방식으로 중계한다.

| 방향 | 처리 |
|------|------|
| 요청 | 브라우저 `Cookie`를 대상 WAS로 전달 |
| 응답 | 대상 WAS `Set-Cookie`를 `RelayResult`에 수집 |
| 최종 | `TcfApiController`가 `Set-Cookie`를 브라우저 응답에 재설정 |

결과:

- 로그인 후 발급된 `JSESSIONID`가 브라우저에 유지
- 이후 `authSession`, `session`, `user-auth` 등 호출에서 동일 세션 사용

---

## 6. 인증 도메인 아키텍처 (`tcf-om`)

## 6.1 `OmAuthService.login`

처리 순서:

1. `userId`, `password` 필수값 검증
2. 사용자 조회 (`dao.selectUserForLogin`)
3. `useYn` 체크(사용 가능 계정 여부)
4. `PasswordEncoder.matches`로 비밀번호 검증
5. `lastLoginTime` 갱신
6. `sessionSupport.createSession(user)`로 세션 생성
7. `sessionId` 포함 응답 바디 반환

## 6.2 `OmAuthService.logout`

- `sessionSupport.invalidateSession()` 호출
- `loggedOut=true` 반환

## 6.3 `OmAuthService.session`

- 현재 로그인 세션 사용자를 바디 형태로 반환
- UI의 자동 로그인 상태 확인/메뉴 보호에 사용

---

## 7. 세션 저장소 아키텍처

`tcf-om`은 Spring Session JDBC 설정을 사용한다.

| 설정 | 값(예) |
|------|--------|
| `spring.session.store-type` | `jdbc` |
| `spring.session.jdbc.table-name` | `SPRING_SESSION` |
| `spring.session.timeout` | `60m` |
| 쿠키명 | `JSESSIONID` |

저장 테이블:

- `SPRING_SESSION`
- `SPRING_SESSION_ATTRIBUTES`

로그인 성공 시 세션 행이 생성되고, 로그아웃/만료 시 삭제된다.

---

## 8. TCF 파이프라인과 로그인

로그인 거래도 일반 거래와 동일하게 STF/ETF를 통과한다.

```text
OM.Auth.login
  → STF (header normalize, guid/traceId)
  → Dispatcher (serviceId 라우팅)
  → Handler/Facade/Service (인증)
  → ETF (응답 조립, 로그/메트릭)
```

주의:

- `nsight.tcf.session-validation-enabled`가 `true`이면 STF의 `SessionValidator`가 `header.userId`를 검사한다.
- 로그인 거래는 인증 전 단계이므로 운영 설정 시 로그인/비로그인 거래 경계를 고려해야 한다.

---

## 9. 오류 처리 표준

| 오류 상황 | 코드 | 설명 |
|-----------|------|------|
| 아이디/비밀번호 불일치 | `E-OM-AUTH-0001` | 로그인 실패 공통 메시지 |
| 세션 검증 실패(옵션) | `E-COM-AUTH-0001` | STF `SessionValidator` |
| 시스템 오류 | `E-COM-SYS-0001` | ETF `systemError` 경로 |

응답 규약:

- HTTP 상태는 대체로 200
- `result.resultCode = E0001`
- 상세 원인은 `errorCode` + `errorMessage`로 전달

---

## 10. 보안 아키텍처 포인트

1. 비밀번호는 DB 해시(`passwordHash`)와 `PasswordEncoder`로 검증
2. 로그인 실패 메시지 통일(계정 존재 여부 노출 방지)
3. 쿠키 `HttpOnly` + `SameSite=Lax`
4. 운영 환경 HTTPS + `secure cookie=true` 권장
5. 세션 강제종료 기능(`OM.Session.delete`)으로 계정 탈취 대응

---

## 11. 운영 관제 연계

로그인/세션은 운영 대시보드와 연계된다.

- `tcf-batch`가 `SPRING_SESSION` 집계(`OM-PORTAL`) 수행
- `OM_SESSION_STATUS`로 활성/만료/유니크 사용자 수 제공
- OM 세션 화면에서 실시간 목록·강제종료 관리

---

## 12. 체크리스트

- [ ] `OM.Auth.login/logout/session` 서비스 카탈로그 등록 확인
- [ ] `PasswordEncoder` 검증 정상 동작 확인
- [ ] 로그인 성공 시 `JSESSIONID` 발급/유지 확인
- [ ] UI Relay 경유 쿠키 전달 누락 없는지 확인
- [ ] `SPRING_SESSION` 만료/정리 배치 주기 확인

---

## 13. 참고 소스

| 파일 | 설명 |
|------|------|
| `tcf-ui/src/main/resources/static/om/admin/login.html` | 로그인 화면 |
| `tcf-ui/src/main/resources/static/_shared/om-admin.js` | 로그인 요청/세션 저장 |
| `tcf-ui/.../service/TransactionRelayService.java` | Cookie/Set-Cookie 릴레이 |
| `tcf-om/.../service/OmAuthService.java` | 인증 핵심 로직 |
| `tcf-om/src/main/resources/application.yml` | Spring Session 설정 |
| `tcf-om/src/main/resources/data.sql` | 로그인 관련 서비스 카탈로그 초기 데이터 |

---

## 14. JWT 로그인 (보조 경로)

OM 쿠키 세션과 별도로 `tcf-jwt`가 Bearer 토큰을 발급합니다. Gateway는 `Authorization: Bearer` 검증을 담당합니다 (`nsight.gateway.auth.jwt`).

| 구분 | OM 로그인 | JWT 로그인 |
|------|-----------|------------|
| 모듈 | tcf-om | tcf-jwt |
| 상태 | JSESSIONID | accessToken (sessionStorage) |
| OM Admin relay | `/api/relay/om/online` | tcf-ui `callViaGateway` → `/api/gateway/om/online` |

상세: [zdoc/로그인.md](../../zdoc/로그인.md) §10 · [zdoc/SSO-TOKEN처리.md](../../zdoc/SSO-TOKEN처리.md)

---

## 15. 변경 이력

| 일자 | 변경 내용 |
|------|-----------|
| 2026-06 | 최초 작성 — TCF 거래 기반 로그인 + Spring Session + UI Relay 구조 정리 |
