# 제16장. Gateway·UI 채널

| 항목 | 내용 |
| --- | --- |
| **편** | 제5편 |
| **상태** | 집필 완료 |
| **원본** | [ztcfbook 제16장](../ztcfbook/제05편/16-API-Gateway-UI-채널.md) |

---

## 그림으로 보기

```mermaid
flowchart LR
  Browser --> UI[tcf-ui Relay]
  Browser --> UJ[tcf-uj]
  UJ --> GW[Gateway GEF]
  GW --> SV["sv-service /bc/online"]
```

---

## 16.1 채널 3종류

| 모듈 | 포트 | 역할 (한 줄) |
| --- | --- | --- |
| **tcf-ui** | 8099 | 브라우저 HTML + **Relay** (쿠키 전달) |
| **tcf-uj** | 8102 | UI → **Gateway** 경유 Relay |
| **tcf-gateway** | 8100 | **인증 + 업무 WAR로 전달** |

```text
[브라우저]
   ├─ tcf-ui ──Relay──► sv-service / tcf-om
   └─ tcf-uj ──► Gateway ──► sv-service
```

---

## 16.2 Relay URL

| 용도 | URL 패턴 |
| --- | --- |
| UI → 업무 WAR | `POST /api/relay/{bc}/online` |
| UI → Gateway | `POST /api/gateway/{bc}/online` |

`{bc}` = `sv`, `om` … (소문자 업무코드)

---

## 16.3 Gateway vs 업무 TCF (다시)

| | Gateway | sv-service TCF |
| --- | --- | --- |
| 들어온 키 | **businessCode** | **serviceId** |
| 실패 | HTTP 401, 404 | JSON result FAIL |
| 로그 | Gateway TX Log | **TCF_TX_LOG** |

---

## 16.4 Route 테이블

Gateway는 DB **`TCF_GATEWAY_ROUTE`** 를 봅니다.

| ENV | BC | Target 예 |
| --- | --- | --- |
| LOCAL | SV | `http://127.0.0.1:8086/sv/online` |
| LOCAL | OM | `http://127.0.0.1:8097/om/online` |

Route 없으면 **404** — STF까지 **안 감**.

---

## 16.5 테스트 화면

| URL | 용도 |
| --- | --- |
| `http://localhost:8099/sv/index.html` | SV 거래 테스트 |
| `http://localhost:8099/om/admin/` | OM Admin |

22장 curl 대신 **버튼**으로 JSON 보낼 때 사용.

---

## 16.6 ⚠️ 초보자 실수

| 실수 | |
| --- | --- |
| tcf-ui를 WAR처럼 배포 | **JAR + Relay** |
| Gateway 없는데 uj만 사용 | Route·JWT **선행** |
| dev `login-required=false`를 prod에 | **보안** |

---

## 요약

- **tcf-ui** = 화면 + Cookie Relay
- **Gateway** = 정문 + Route
- 실습은 **직접 8086** 또는 **8099 UI**

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [15장 OM](./15-OM이-하는-일.md) |
| → 다음 | [17장 배치](./17-배치-모니터링.md) |

---

## 📘 원본에서 더 보기

- [ztcfbook/제05편/16-API-Gateway-UI-채널.md](../ztcfbook/제05편/16-API-Gateway-UI-채널.md)
