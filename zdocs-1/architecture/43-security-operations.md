# 43. 보안 운영 설계서

| 항목 | 내용 |
|------|------|
| 문서 번호 | 43 |
| 제목 | Security Operations Design |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [10-session.md](10-session.md), [11-login.md](11-login.md), [42-jwt.md](42-jwt.md), [16-deploy.md](16-deploy.md) |
| 상세 매뉴얼 | [79-TCF-GATEWAY-JWT-개발-매뉴얼.md](../../znsight-man/79-TCF-GATEWAY-JWT-개발-매뉴얼.md), [42-보안-코딩-기준.md](../../znsight-man/42-보안-코딩-기준.md) |
| 구현 모듈 | `tcf-jwt`, `tcf-gateway`, `tcf-web`, `tcf-om`, `tcf-ui` |
| 대상 | 보안·운영·아키텍트 담당자 |

---

## 1. 문서 목적

본 문서는 NSIGHT TCF의 **인증·토큰·비밀정보·폐기·장애 대응**을 운영 관점에서 정의한다.

개발 구현은 [42-jwt.md](42-jwt.md), 코딩 규칙은 `znsight-man/42-보안-코딩-기준.md`를 따르고, 본 문서는 **운영 절차·책임·점검 기준**을 다룬다.

| 구분 | 담당 문서 |
|------|-----------|
| 아키텍처·처리 흐름 | [42-jwt.md](42-jwt.md) |
| API·claim·화면 상세 | [79-TCF-GATEWAY-JWT-개발-매뉴얼.md](../../znsight-man/79-TCF-GATEWAY-JWT-개발-매뉴얼.md) |
| 운영·보안 절차 | **본 문서 (43)** |

---

## 2. 보안 운영 범위

```text
[비밀정보]  RSA 개인키, DB 비밀번호, SSO Client Secret
     │
     ▼
[tcf-jwt]   발급 · JWKS · Denylist DB
     │
     ▼
[tcf-gateway / tcf-web]  JwtDecoder 검증 (서명·exp·iss·aud)
     │
     ▼
[tcf-core STF]  claim ↔ Header 정합성 · 권한 · 거래통제
     │
     ▼
[OM]  강제 로그아웃 · 토큰 조회·폐기 · 감사
```

운영 보안의 4대 축:

1. **비밀정보 관리** — 키·Secret 저장·로테이션
2. **토큰 생명주기** — 발급·갱신·폐기·Denylist
3. **검증 계층 운영** — Gateway / WAR / STF 각각의 on·off 정책
4. **장애·침해 대응** — 키 유출·대량 폐기·검증 장애 시 절차

---

## 3. 비밀정보(Secrets) 관리

### 3.1 관리 대상

| 자산 | 보관 주체 | 저장 금지 |
|------|-----------|-----------|
| JWT RS256 **개인키** | `tcf-jwt` 전용 Secret Store | Git, 로그, OM 화면 |
| JWT **공개키** | JWKS (`/.well-known/jwks.json`) | — (공개 가능) |
| DB 접속 정보 | 환경변수 / Secret Manager | `application.yml` 평문 커밋 |
| SSO Client Secret | `tcf-om` | 프론트엔드·로그 |
| Refresh Token **원문** | 클라이언트(HttpOnly Cookie 권장) | DB·로그 |

### 3.2 환경별 원칙

| 환경 | 원칙 |
|------|------|
| local | 개발용 키·H2 — 운영 키 사용 금지 |
| dev | 운영과 분리된 키·DB |
| prod | Secret Manager 또는 배포 파이프라인 주입, 파일 마운트 최소화 |

### 3.3 키 로테이션 절차 (RS256)

**목표:** 서비스 중단 없이 서명 키를 교체하고, 구 키로 발급된 토큰은 자연 만료까지 허용한다.

| 단계 | 작업 | 담당 |
|------|------|------|
| 1 | 신규 키쌍 생성 (`kid` 신규 부여) | 보안·플랫폼 |
| 2 | JWKS에 **신·구 키 동시 노출** (멀티 키) | tcf-jwt 배포 |
| 3 | 발급은 **신규 kid**만 사용 | tcf-jwt 설정 |
| 4 | Gateway·업무 WAR `jwk-set-uri` 캐시 TTL 확인 (재조회 가능) | 운영 |
| 5 | 구 키 Access Token 만료 대기 (기본 15분) + Refresh 정책 검토 | 운영 |
| 6 | JWKS에서 구 키 제거 | tcf-jwt 배포 |
| 7 | 로테이션 이력·감사 로그 보관 | OM·감사 |

**롤백:** 신규 kid 발급 중단 → JWKS를 구 키만 남기고 재배포.

### 3.4 로그·감사 금지 항목

다음은 **어떤 로그에도 출력하지 않는다.**

- Access/Refresh Token 원문
- RSA 개인키, DB 비밀번호
- SSO assertion 원문
- 주민번호·계좌번호 등 업무 민감정보 전문 Body

허용 로그 예: `userId`, `jti`, `serviceId`, `guid`, `resultCode`, 검증 실패 **사유 코드** (`E-JWT-AUTH-*`).

---

## 4. 토큰 생명주기 운영

### 4.1 토큰 종류·만료 (기본값)

| 토큰 | 기본 TTL | 저장 | 폐기 |
|------|----------|------|------|
| Access | 15분 | `TCF_JWT_TOKEN` 이력 | `JWT.Auth.revoke` → Denylist |
| Refresh | 8시간 | `TCF_REFRESH_TOKEN` (Hash) | `JWT.Auth.logout` / Rotation 시 폐기 |

정책 변경: `JWT.SecurityPolicy.inquiry/update` (OM) 또는 `JwtRuntimePolicy` 런타임 반영 — 변경 시 **감사 로그** 필수.

### 4.2 Denylist 운영

| 항목 | 내용 |
|------|------|
| 키 | `issuer` + `jti` |
| 저장 | `TCF_TOKEN_DENYLIST` |
| 등록 경로 | `JWT.Auth.revoke`, OM 강제 폐기, 로그아웃 연쇄 |
| 검사 | `tcf-jwt` `JwtTokenStore.isDenied()` (`denylist-check-enabled`) |

**현재 구현 상태 (운영 시 확인):**

| 검증 계층 | Denylist 연동 |
|-----------|---------------|
| tcf-jwt (발급·갱신 시) | 구현됨 |
| tcf-gateway | **미연동** — 서명·exp·iss·aud만 검증 |
| tcf-web Filter | **미연동** — 동일 |

운영 원칙:

- Denylist **즉시 차단**이 필요하면 Gateway/WAR에 denylist 조회 연동 완료 전까지 **짧은 Access TTL(15분)** 유지.
- 대량 침해 시: OM에서 사용자별 `revoke` + 세션 무효화(`OM.Auth.logout`) 병행.

### 4.3 Refresh Token Rotation

| 이벤트 | 동작 |
|--------|------|
| `JWT.Auth.refresh` 성공 | 신규 Access + 신규 Refresh, 구 Refresh Hash 무효화 |
| 재사용된 Refresh 감지 | 해당 `tokenFamilyId` 전체 무효화 (정책 활성 시) |

운영 점검: Rotation 실패율, 동일 family 중복 refresh 시도 — 보안 이벤트로 모니터링.

---

## 5. 검증 계층별 운영 정책

### 5.1 권장 운영 모드

| 모드 | Gateway JWT | WAR JWT Filter | STF 정합성 | 용도 |
|------|-------------|----------------|------------|------|
| A. Gateway 전용 | ON | OFF | ON | DMZ에서 Gateway만 노출 |
| B. 이중 방어 | ON | ON | ON | Tomcat 직접 노출 가능 환경 **(권장 prod)** |
| C. 세션 전환기 | OFF | OFF | OFF | 레거시 Cookie만 (단기 전환) |
| D. OM JWT 필수 | ON (OM Bearer 필수) | ON (OM 경로) | ON | OM Admin JWT 전환 완료 후 |

### 5.2 설정 키 체크리스트

**Gateway**

```yaml
nsight.gateway.auth.login-required: true
nsight.gateway.auth.jwt.enabled: true
nsight.gateway.auth.jwt.jwk-set-uri: <tcf-jwt JWKS URL>
# issuer, audience — JwtDecoder 검증과 일치 필수
```

**업무 WAR (`tcf-web`)**

```yaml
nsight.tcf.web.jwt.enabled: true
nsight.tcf.web.jwt.required-for-online: true
nsight.tcf.web.jwt.jwk-set-uri: <동일 JWKS>
nsight.tcf.authentication-context-validation-enabled: true
```

**tcf-jwt**

```yaml
nsight.security.jwt.denylist-check-enabled: true
```

### 5.3 장애 시 강등(Degradation) 정책

| 장애 | 허용 행동 | 금지 행동 |
|------|-----------|-----------|
| JWKS URL 불가 | 신규 로그인·발급 중단, 기존 세션 Cookie 경로만 제한적 허용(정책 승인 시) | JWT 검증 완전 우회 |
| tcf-jwt 다운 | SSO 로그인 후 세션-only 모드(임시) | 개인키를 Gateway에 복사 |
| Gateway 다운 | UI Relay 직접 경로 — WAR Filter 필수 ON | Filter 없이 Header만 신뢰 |
| STF 정합성 검증 오류 급증 | claim/Header 매핑 버그 조사 | `authentication-context-validation-enabled=false` 상시 유지 |

모든 강등은 **변경관리·시간 제한·사후 감사**를 원칙으로 한다.

---

## 6. OM·운영 관리 절차

### 6.1 일상 운영

| 작업 | 경로 | 감사 |
|------|------|------|
| 토큰 발급 이력 조회 | OM JWT 토큰 관리 | 조회 로그 |
| 사용자 강제 로그아웃 | OM 세션 / JWT 폐기 | 필수 |
| 보안 정책 변경 | `JWT.SecurityPolicy.update` | 필수 |
| Denylist 등록 | `JWT.Auth.revoke` / OM | 사유 코드 기록 |

### 6.2 침해·유출 대응 런북 (요약)

| 단계 | 조치 |
|------|------|
| 1 | 영향 `userId`·`jti`·시간대 식별 (거래로그·`TCF_JWT_TOKEN`) |
| 2 | 해당 사용자 Access revoke + Refresh 무효화 + OM 세션 종료 |
| 3 | 유출 의심 키 **즉시 로테이션** (§3.3) |
| 4 | Gateway·WAR JWKS 캐시 무효화·재기동 |
| 5 | 동일 IP/채널 이상 패턴 모니터링 |
| 6 | 사후 보고·재발방지 (로그 마스킹·Denylist 연동 등) |

### 6.3 정기 점검 (월간 권장)

- [ ] JWKS URL 가용성·인증서(키) 만료일
- [ ] Access/Refresh TTL이 보안 정책과 일치
- [ ] Denylist 테이블 증가량·만료 정리 배치
- [ ] `E-JWT-AUTH-*` 오류율 추이
- [ ] 운영 환경 Secret이 Git에 없음
- [ ] Gateway 우회 경로(직접 Tomcat) 차단 여부

---

## 7. 역할·책임 (RACI 요약)

| 활동 | 개발 | 운영 | 보안 | OM 관리자 |
|------|:----:|:----:|:----:|:---------:|
| 키 로테이션 | C | R | A | I |
| JWT 정책 변경 | C | R | A | R |
| 강제 폐기 | I | R | C | R |
| Gateway JWT on/off | R | A | C | I |
| 침해 대응 | C | R | A | R |

R=실행, A=승인, C=협의, I=통보

---

## 8. 구현 갭·로드맵

운영 설계 대비 **현재 코드베이스에서 추가 구현이 필요한 항목:**

| 항목 | 상태 | 권장 |
|------|------|------|
| Gateway Denylist 조회 | 미구현 | tcf-jwt API 또는 공유 DB 조회 |
| tcf-web Filter Denylist | 미구현 | 발급 모듈과 동일 소스 |
| JWKS 멀티 키 로테이션 | 설계만 | kid 복수 지원·발급 kid 전환 |
| Secret Manager 연동 | 환경별 | prod 필수 |

갭이 해소되기 전 운영 시 §5.3 강등 정책과 짧은 Access TTL로 리스크를 완화한다.

---

## 9. 관련 소스

| 파일 | 역할 |
|------|------|
| `tcf-jwt/.../JwtTokenStore.java` | Denylist·Refresh Hash |
| `tcf-jwt/.../JwtRuntimePolicy.java` | 런타임 보안 정책 |
| `tcf-gateway/.../GatewayJwtValidator.java` | Gateway JWT 검증 |
| `tcf-web/.../TcfJwtAuthenticationFilter.java` | WAR JWT 검증 |
| `tcf-core/.../AuthenticationContextValidator.java` | STF 2차 정합성 |

---

← [42-jwt.md](42-jwt.md) · [44-observability.md](44-observability.md) →
