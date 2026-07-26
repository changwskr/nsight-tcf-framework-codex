# 10. 세션 아키텍처

| 항목 | 내용 |
|------|------|
| 문서 번호 | 10 |
| 제목 | Session Architecture |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [03-transaction.md](03-transaction.md), [04-messaging.md](04-messaging.md), [08-timeout.md](08-timeout.md) |
| 구현 모듈 | `tcf-om`, `tcf-core`, `tcf-ui`, `tcf-uj`, `tcf-gateway`, `tcf-jwt`, `tcf-batch` |
| 대상 | OM 포털/프레임워크/운영 개발자 |

---

## 1. 개요

NSIGHT TCF의 세션은 단일 방식이 아니라, 목적에 따라 3가지 관점으로 구성된다.

| 관점 | 역할 | 핵심 저장소/소스 |
|------|------|------------------|
| **인증 세션** | OM 로그인 상태 유지 | `SPRING_SESSION` (JDBC) |
| **거래 헤더 세션 검증** | TCF 거래 요청의 로그인 유효성 확인 | `SessionValidator` (`header.userId`) |
| **운영 모니터링 세션 지표** | 대시보드 세션 상태 집계 | `SPRING_SESSION` + Actuator metric |

즉, “로그인 유지”, “거래 검증”, “운영 관제”가 분리되어 설계되어 있다.

---

## 2. 전체 구조

```text
Browser (OM Admin)
  │ Cookie: JSESSIONID  또는  Authorization: Bearer (JWT)
  ▼
tcf-ui / tcf-uj
  ├─ /api/relay/OM/online  ── Cookie ──► tcf-om (직접)
  └─ /api/gateway/om/online ── Bearer ──► tcf-gateway ──► tcf-om
  ▼
tcf-om (/om/online, OM.Auth.*)
  │
  ├─ Spring Session JDBC (SPRING_SESSION)
  ├─ OmAuthSessionSupport (세션 생성/조회/무효화)
  └─ TCF STF SessionValidator (선택)

tcf-gateway (uj 업무 relay / ui·uj JWT OM)
  ├─ GatewayAuthenticationService (JWT 또는 SESSIONDB 4단계)
  └─ GatewaySessionRequestEnricher

tcf-batch
  ├─ SPRING_SESSION 집계 (OM-PORTAL)
  └─ 각 WAR Actuator 세션 metric 집계 ({CODE}-AP)
```

---

## 3. 인증 세션 아키텍처 (OM 포털)

## 3.1 저장 방식

`tcf-om`은 Spring Session JDBC를 사용한다.

- `spring.session.store-type: jdbc`
- `spring.session.jdbc.table-name: SPRING_SESSION`
- `spring.session.timeout: 60m`
- `server.servlet.session.timeout: 60m`

쿠키 설정:

- 이름: `JSESSIONID`
- `HttpOnly: true`
- `SameSite: Lax`

## 3.2 로그인/로그아웃/세션조회

OM 거래 서비스:

| serviceId | 역할 |
|-----------|------|
| `OM.Auth.login` | 인증 성공 시 세션 생성 |
| `OM.Auth.logout` | 현재 세션 무효화 |
| `OM.Auth.session` | 현재 세션 사용자 정보 반환 |

`OmAuthService` 흐름:

1. 사용자/비밀번호 검증
2. `sessionSupport.createSession(user)`로 세션 생성
3. `sessionId`, `userId`, `branchId`, `authGroupId` 등 반환

---

## 4. UI Relay 쿠키 전파 아키텍처

`tcf-ui`는 세션 자체를 저장하지 않고 **쿠키 전달자** 역할을 수행한다.

`TransactionRelayService` 동작:

- 요청: 브라우저의 `Cookie` 헤더를 대상 WAS로 전달
- 응답: 대상 WAS의 `Set-Cookie`를 `RelayResult`에 담아 반환
- `TcfApiController.applySetCookies()`가 브라우저 응답 헤더에 다시 세팅

효과:

- 브라우저는 `tcf-ui` 경유 호출에서도 `JSESSIONID`를 유지
- OM Admin 로그인 이후 세션 기반 API 연속 호출 가능

---

## 5. TCF 세션 검증 아키텍처 (`SessionValidator`)

`tcf-core` STF 단계에서 세션 검증이 가능하다.

```text
STF.preProcess()
  → SessionValidator.validate(header)
```

검증 규칙:

- `nsight.tcf.session-validation-enabled = true`일 때만 동작
- `header.userId`가 비어 있으면 `E-COM-AUTH-0001` (`SESSION_INVALID`)

주의:

- 현재 검증은 “헤더 기반 최소 검증”이며, `SPRING_SESSION` 직접 조회형은 아님
- 운영 강화 시 Spring Session/권한 테이블 연계 확장 가능

---

## 6. 세션 관리(운영 기능) 아키텍처

`OM.Session.inquiry` / `OM.Session.delete`는 `SPRING_SESSION`을 직접 조회/관리한다.

### 6.1 조회

`OmSessionService.inquiry`:

- 사용자/활성여부 조건으로 세션 목록 조회
- 활성 세션 건수, 현재 로그인 세션 정보 반환

사용 SQL:

- `searchSpringSessions`
- `countSpringSessions`
- `countActiveSpringSessions`

### 6.2 강제 종료

`OmSessionService.delete`:

1. 대상 세션 존재 확인 (`selectSpringSessionById`)
2. 삭제 (`deleteSpringSession`)
3. 현재 세션을 지운 경우 `sessionSupport.invalidateSession()`
4. 감사/권한이력 기록

실패 시 `E-OM-BIZ-0002` 반환.

---

## 7. 세션 정리(Cleanup) 아키텍처

OM은 만료 세션 정리 기능을 가진다.

- 배치/수동 정리 엔트리 존재 (`OmSessionCleanupService`, `OmBatchService` 연계)
- `nsight.om.session-cleanup.fixed-rate-ms`로 주기 제어 (예: 10000ms)

핵심 목적:

- `SPRING_SESSION` 테이블 누적 방지
- 만료 세션/활성 세션 지표 품질 유지

---

## 8. 세션 모니터링 아키텍처 (`tcf-batch`)

`tcf-batch`는 세션을 운영 관점으로 집계해 `OM_SESSION_STATUS`에 적재한다.

## 8.1 수집 소스

| scope | source-type | 의미 |
|-------|-------------|------|
| `OM-PORTAL` | `spring-session` | `SPRING_SESSION` 직접 집계 |
| `{CODE}-AP` | `actuator` | 각 WAR의 `tomcat.sessions.active.current` metric |

## 8.2 수집 항목

- `activeCount`
- `expiredCount`
- `totalCount`
- `uniqueUserCount`
- `healthStatus` (`NORMAL/WARN/FAIL`)

`SessionMetricsClient`는 외부 WAR metric 미존재 시에도 health UP이면 `0`으로 수집하는 완충 로직을 갖는다.

---

## 9. 세션 데이터 모델

### 9.1 Spring Session 핵심 컬럼

| 테이블 | 주요 컬럼 |
|--------|-----------|
| `SPRING_SESSION` | `SESSION_ID`, `CREATION_TIME`, `LAST_ACCESS_TIME`, `MAX_INACTIVE_INTERVAL`, `EXPIRY_TIME`, `PRINCIPAL_NAME` |
| `SPRING_SESSION_ATTRIBUTES` | 세션 속성 직렬화 데이터 |

### 9.2 운영 상태 테이블

| 테이블 | 용도 |
|--------|------|
| `OM_SESSION_STATUS` | 세션 모니터링 스냅샷 (scope별) |

---

## 10. 타임아웃과 세션 수명주기

세션 관련 주요 시간 설정:

| 설정 | 기본값(예) | 설명 |
|------|------------|------|
| `server.servlet.session.timeout` | 60m | WAS 세션 만료 |
| `spring.session.timeout` | 60m | JDBC 세션 만료 |
| `nsight.om.session-cleanup.fixed-rate-ms` | 10000 | 만료 세션 정리 주기 |

관점 분리:

- **세션 만료 시간** = 인증 유지 정책
- **온라인 거래 timeout** = 요청 처리 상한 (세션과 별개)

---

## 11. 오류 처리 표준

| 상황 | 오류코드 | 처리 위치 |
|------|----------|-----------|
| 헤더 userId 없음(검증 활성 시) | `E-COM-AUTH-0001` | `SessionValidator` |
| 로그인 실패 | `E-OM-AUTH-0001` | `OmAuthService` |
| 삭제 대상 세션 없음 | `E-OM-BIZ-0002` | `OmSessionService` |

응답은 표준 거래 규약대로 `result.resultCode`/`errorCode`로 전달된다.

---

## 12. 보안/운영 권장사항

1. 운영 환경에서 `secure cookie` + HTTPS 적용
2. `session-validation-enabled`와 권한 검증 플래그를 환경별 명확히 분리
3. 세션 강제종료/권한변경 시 감사로그 필수 기록
4. UI Relay 경유 쿠키 전달 경로(`Cookie`, `Set-Cookie`) 장애 모니터링
5. `SPRING_SESSION` 인덱스/정리주기 점검으로 조회 성능 유지

---

## 13. 체크리스트

- [ ] `tcf-om`의 Spring Session JDBC가 활성화되어 있는가
- [ ] OM 로그인/세션조회/로그아웃 거래(`OM.Auth.*`)가 정상 동작하는가
- [ ] UI Relay가 Cookie/Set-Cookie를 누락 없이 전달하는가
- [ ] 세션관리 화면(`OM.Session.*`)에서 조회/강제종료가 가능한가
- [ ] `tcf-batch`가 `OM-PORTAL` 세션 지표를 주기적으로 수집하는가

---

## 14. 참고 소스

| 파일 | 설명 |
|------|------|
| `tcf-core/.../security/SessionValidator.java` | STF 세션 검증 |
| `tcf-om/src/main/resources/application.yml` | Spring Session/JSESSIONID 설정 |
| `tcf-om/.../service/OmAuthService.java` | 로그인/로그아웃/세션조회 |
| `tcf-om/.../service/OmSessionService.java` | 세션 조회/강제종료 |
| `tcf-om/.../mapper/om/OmOperationMapper.xml` | `SPRING_SESSION` 조회/삭제 SQL |
| `tcf-ui/.../service/TransactionRelayService.java` | 쿠키 릴레이 |
| `tcf-batch/.../client/SessionMetricsClient.java` | 세션 상태 수집 |

---

## 15. 변경 이력

| 일자 | 변경 내용 |
|------|-----------|
| 2026-06 | 최초 작성 — 인증/검증/관제 3계층 세션 아키텍처 정리 |
