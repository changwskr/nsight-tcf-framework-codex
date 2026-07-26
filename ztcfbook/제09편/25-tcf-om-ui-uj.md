# 제25장. tcf-om · tcf-ui · tcf-uj

| 항목 | 내용 |
| --- | --- |
| **편** | 제9편 · 모듈별 레퍼런스 (Quick Start) |
| **장** | 제25장 |
| **파일** | `제09편/25-tcf-om-ui-uj.md` |
| **상태** | 집필 완료 |
| **목차** | [00-목차](../00-목차.md) |

---

## 25.1 tcf-om — 운영 관리 본체

| 항목 | 값 |
| --- | --- |
| 포트 | 8097 |
| Context | `/om` |
| 업무코드 | OM |
| WAR | `tcf-om.war` / `om.war` |

### 역할

NSIGHT **운영 기준정보 원장** — 사용자·권한·메뉴·ServiceId Catalog·거래통제·Timeout·오류코드·세션·거래로그·배치·배포·Cache·대시보드·파일(UD).

업무 WAR와 **동일 TCF 6계층**이지만 OM 전용 Handler **24개**, serviceId **80+**.

### 5분 Quick Start

```bash
gradle :tcf-om:bootRun

curl -X POST http://127.0.0.1:8097/om/online \
  -H "Content-Type: application/json" \
  -d @tcf-ui/src/main/resources/sample-requests/om-sample-inquiry.json
```

⚠ **om-service와 포트 8097 충돌** — 동시 기동 금지. 신규 개발은 **tcf-om만** 사용.

### 주요 Handler

| Handler | 영역 |
| --- | --- |
| OmAuthHandler | login, logout, ssoLogin |
| OmServiceCatalogHandler | **ServiceId Catalog** |
| OmTransactionControlHandler | **거래통제** |
| OmTimeoutPolicyHandler | Timeout |
| OmTransactionLogHandler | 거래로그 조회 |
| OmCacheHandler | Cache Evict |
| OmDashboardHandler | 운영 대시보드 |

### Catalog · Seed (중요)

업무 serviceId 등록 시 반드시 반영:

- `tcf-om/src/main/resources/data.sql`
- `ServiceCatalogSeedData.java`
- 필드 `HANDLER_CLASS` = `{Domain}Handler` 클래스명

---

## 25.2 tcf-ui — 거래·OM 테스트 UI

| 항목 | 값 |
| --- | --- |
| 포트 | 8097 (Relay) / **8099** (UI 서버) |
| Context (ztomcat) | `/ui` |

### 역할

- 업무 WAR **거래 테스트** HTML (sv, ic, om 등)
- OM Admin Relay — `http://localhost:8099/om/admin/login.html`
- JWT Admin Relay — `/jwt/admin/*` (tcf-jwt 8110 필요)

### Quick Start

```bash
gradle :sv-service:bootRun    # 대상 WAR
gradle :tcf-ui:bootRun        # 8099

# SV 테스트 UI
http://localhost:8099/sv/index.html
# OM Admin
http://localhost:8099/om/admin/login.html
```

ztomcat: `http://localhost:8080/ui/sv/index.html`

### Relay 원리

tcf-ui는 브라우저 요청을 **동일 Origin**으로 받아 백엔드 WAR(`8086/sv/online` 등)로 프록시합니다. CORS·세션 쿠키 이슈를 줄입니다.

---

## 25.3 tcf-uj — Gateway 경유 UI

| 항목 | 값 |
| --- | --- |
| 포트 | **8102** |
| Context (ztomcat) | `/uj` |

### 역할

**tcf-gateway(8100)를 반드시 경유**하는 채널 UI. 운영 아키텍처(FAD)와 동일한 End-to-End 검증용.

```text
브라우저 → tcf-uj(8102) → tcf-gateway(8100) → sv-service(8086)
```

### Quick Start

```bash
gradle :sv-service:bootRun
gradle :tcf-om:bootRun         # 세션
gradle :tcf-gateway:bootRun    # 8100
gradle :tcf-uj:bootRun         # 8102

http://localhost:8102/sv/index.html
http://localhost:8102/om/admin/login.html
```

JWT 모드: Bearer Token + Gateway JWT Filter. [제26장](./26-tcf-gateway-jwt.md) 참고.

---

## 25.4 OM ↔ UI 접속 URL 요약

| 모드 | OM Admin 로그인 |
| --- | --- |
| tcf-ui bootRun | http://localhost:8099/om/admin/login.html |
| tcf-ui ztomcat | http://localhost:8080/ui/om/admin/login.html |
| tcf-uj bootRun | http://localhost:8102/om/admin/login.html |
| tcf-uj ztomcat | http://localhost:8080/uj/om/admin/login.html |

---

## 장 요약

**tcf-om**은 Service Catalog·거래통제·Timeout 등 운영 메타의 SoT이고, **tcf-ui**는 로컬 직결 Relay 테스트 UI, **tcf-uj**는 Gateway 경유 운영 동형 테스트 UI입니다. 업무 serviceId는 OM Catalog + Seed에 등록하고, Admin 화면은 tcf-ui 또는 tcf-uj로 접속합니다.

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [제24장 tcf-core · tcf-web · tcf-util](./24-tcf-core-web-util.md) |
| → 다음 | [제26장 tcf-gateway · tcf-jwt](./26-tcf-gateway-jwt.md) |

---

## 출처 색인

| 절 | 출처 |
| --- | --- |
| 25.1 | [zguide/tcf-om-개발가이드.md](../../zguide/tcf-om-개발가이드.md), [zarchitecture/05-운영관리-OM-아키텍처.md](../../zarchitecture/05-운영관리-OM-아키텍처.md) |
| 25.2 | [zguide/tcf-ui-개발가이드.md](../../zguide/tcf-ui-개발가이드.md), [zarchitecture/13-UI-채널-아키텍처.md](../../zarchitecture/13-UI-채널-아키텍처.md) |
| 25.3 | [zguide/tcf-uj-개발가이드.md](../../zguide/tcf-uj-개발가이드.md) |
| 25.4 | [docs/설계자료/README.md](../../docs/설계자료/README.md) |
