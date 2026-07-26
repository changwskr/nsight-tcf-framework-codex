# 제13장. JWT·Gateway (쉽게)

| 항목 | 내용 |
| --- | --- |
| **편** | 제4편 |
| **상태** | 집필 완료 |
| **원본** | [ztcfbook 제13장](../ztcfbook/제04편/13-JWT-SSO-Gateway.md) |

---

## 그림으로 보기

```mermaid
flowchart LR
  JWT[tcf-jwt :8110 JWKS] --> GW[tcf-gateway :8100]
  GW -->|Bearer 검증| WAR[업무 WAR]
  UJ[tcf-uj] --> GW
```

---

## 13.1 JWT — “디지털 출입증”

**JWT(JSON Web Token)** 는 쿠키 대신 쓰는 **출입증 문자열**입니다.

```http
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

| | 세션(쿠키) | JWT |
| --- | --- | --- |
| 저장 | 브라우저 쿠키 | 보통 **메모리/헤더** |
| 발급 | tcf-om 로그인 | **tcf-jwt** (8110) |
| 검증 | SESSIONDB | **서명 + JWKS** |

**중요:** JWT가 있어도 **모든 거래가 허용되는 것은 아닙니다.**  
TCF STF가 여전히 serviceId·권한·거래통제를 검사합니다.

---

## 13.2 tcf-jwt — 토큰 공장

| 모듈 | 포트 | 역할 |
| --- | --- | --- |
| **tcf-jwt** | 8110 | Access/Refresh **발급·갱신·폐기** |
| JWKS | `/.well-known/jwks.json` | 공개키 목록 (검증용) |

토큰 **원문은 로그에 남기지 않습니다.** userId, jti 정도만.

---

## 13.3 Gateway — “안내 데스크 + 경비”

**tcf-gateway** (8100)는 업무 WAR **앞**에 서는 **중간 문**입니다.

```text
클라이언트
  → Gateway (8100)
      ├─ 세션 쿠키 또는 JWT 확인
      ├─ businessCode 보고 어디로 보낼지 결정 (SV → 8086 …)
      └─ POST /sv/online 으로 Relay
  → sv-service
      └─ TCF (serviceId로 Handler)
```

| Gateway | 업무 WAR TCF |
| --- | --- |
| **businessCode**로 라우팅 | **serviceId**로 Handler |
| 401/404 HTTP 오류 | 보통 200 + result FAIL |

**비유:** Gateway = **건물 정문 경비**, TCF = **각 사무실 안내 데스크**.

---

## 13.4 로컬에서 Gateway 없이

학습·디버깅 때는 **8086에 직접** `POST /sv/online` 해도 됩니다. (22장)

Gateway·JWT는 **통합·운영** 단계에서 붙입니다.

---

## 13.5 ⚠️ 초보자 실수

| 실수 | |
| --- | --- |
| “JWT 있으면 STF 검사 불필요” | **항상 TCF 검사** |
| Body에 토큰 넣기 | **Authorization 헤더**만 |
| Gateway 401 = serviceId 오류 | **인증** 문제일 수 있음 |

---

## 요약

- **JWT** = Bearer 토큰, tcf-jwt가 발급
- **Gateway** = 인증 + **업무 WAR로 전달**
- 실행 기준은 여전히 **serviceId + STF**

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [12장 세션·로그인](./12-세션-로그인-쉽게.md) |
| → 다음 | [14장 거래통제](./14-거래통제-쉽게.md) |

---

## 📘 원본에서 더 보기

- [ztcfbook/제04편/13-JWT-SSO-Gateway.md](../ztcfbook/제04편/13-JWT-SSO-Gateway.md)
