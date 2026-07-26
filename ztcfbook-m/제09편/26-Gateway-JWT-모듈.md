# 제26장. Gateway·JWT 모듈

| 항목 | 내용 |
| --- | --- |
| **편** | 제9편 |
| **상태** | 집필 완료 |
| **원본** | [ztcfbook 제26장](../ztcfbook/제09편/26-tcf-gateway-jwt.md) |

---

## 그림으로 보기

```mermaid
flowchart LR
  Client --> GW[tcf-gateway :8100]
  JWT[tcf-jwt :8110] -->|JWKS| GW
  GW -->|Proxy| WAR["/{bc}/online"]
```

---

## 26.1 tcf-gateway — 정문 (8100)

채널에서 들어온 요청을 **Route 테이블** 보고 업무 WAR로 **넘깁니다**.

```text
tcf-uj(8102) → Gateway(8100) → sv-service(8086)
```

| 하는 일 | 안 하는 일 |
| --- | --- |
| 라우팅·세션 검증 | 업무 로직 |
| JWT 또는 쿠키 확인 | Token **발급** (→ tcf-jwt) |

LOCAL Route 예:

| BC | Target |
| --- | --- |
| SV | http://127.0.0.1:8086/sv/online |
| OM | http://127.0.0.1:8097/om/online |

---

## 26.2 tcf-jwt — Token 발급소 (8110)

| | |
| --- | --- |
| 역할 | Access/Refresh **발급·폐기·JWKS** |
| Admin | http://localhost:8099/jwt/admin/login.html |

Gateway·tcf-web은 **검증만** 합니다. Token 만들기는 **tcf-jwt**.

---

## 26.3 기동 순서 (uj 테스트)

```bash
gradle :sv-service:bootRun
gradle :tcf-om:bootRun
gradle :tcf-gateway:bootRun
gradle :tcf-uj:bootRun
# JWT 모드면 tcf-jwt도
```

---

## 26.4 ⚠️ 초보자 실수

| 실수 | |
| --- | --- |
| Route 없이 Gateway 호출 | **404** — STF까지 안 감 |
| JWT 발급을 Gateway에서 찾음 | **tcf-jwt 8110** |
| dev `login-required=false`를 prod에 | **보안 사고** |

---

## 요약

- **Gateway** = 라우팅 + 인증 관문
- **tcf-jwt** = Token 생명주기
- 13장·16장 내용의 **모듈 버전**

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [25장 OM·UI·uj](./25-OM-UI-uj-모듈.md) |
| → 다음 | [27장 연동·캐시·배치](./27-연동-캐시-배치-모듈.md) |

---

## 📘 원본에서 더 보기

- [ztcfbook/제09편/26-tcf-gateway-jwt.md](../ztcfbook/제09편/26-tcf-gateway-jwt.md)
