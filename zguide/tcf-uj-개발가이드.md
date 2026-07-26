# tcf-uj 개발자 가이드

> **역할:** Gateway 경유 온라인 테스트 UI + OM/JWT Admin  
> **포트:** **8102** · Context **`/uj`** (ztomcat)

---

## 1. 이 모듈이 하는 일

브라우저는 tcf-uj만 호출하고, **모든 온라인 거래는 tcf-gateway를 경유**합니다.

```
브라우저 → tcf-uj (:8102)
  POST /api/relay/{code}/online  (Cookie: JSESSIONID)
→ tcf-gateway (:8100)
  POST /{code}/online
→ downstream WAS (예: sv :8086)
```

**예외:** `/api/updownload/*` → tcf-om **직접** (Gateway 미경유)

---

## 2. 5분 빠른 시작 (권장 순서)

```bash
gradle :sv-service:bootRun      # 8086
gradle :tcf-om:bootRun          # 8097
gradle :tcf-gateway:bootRun     # 8100
gradle :tcf-uj:bootRun          # 8102

# OM 로그인
http://localhost:8102/om/admin/login.html   # admin01 / nsight01!

# SV 거래 테스트
http://localhost:8102/sv/index.html
```

---

## 3. 실행

| | |
|---|---|
| bootRun | `gradle :tcf-uj:bootRun` |
| 스크립트 | `tcf-scripts/run-local.bat uj` |
| ztomcat | `http://localhost:8080/uj/` |

**메인:** `com.nh.nsight.tcf.uj.NsightTcfUjApplication`  
**설정 prefix:** `nsight.tcf-uj`

---

## 4. Relay API

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `GET` | `/api/business-modules` | 업무 목록 |
| `POST` | `/api/relay/{code}/online` | **Gateway 경유** Relay (Cookie) |
| `POST` | `/api/gateway/om/online` | OM JWT Bearer relay |
| `POST` | `/api/multi/relay/{code}/online` | 다중 거래 |
| `POST` | `/api/updownload/*` | tcf-om 직접 |

---

## 5. Gateway relay URL

| 배포 | 대상 |
|------|------|
| bootrun | `http://127.0.0.1:8100/{code}/online` |
| tomcat | `http://localhost:8080/gw/{code}/online` |

---

## 6. 화면

| 경로 | 설명 |
|------|------|
| `/` | 업무 허브 |
| `/{code}/index.html` | 단일 거래 |
| `/{code}/index-multi.html` | 다중 거래 |
| `/om/admin/*` | OM 운영 포털 |
| `/jwt/admin/*` | JWT Admin |
| `/ud/updownload.html` | 파일 UD |

---

## 7. 등록 업무

Gateway에 라우팅 등록된 업무링 Relay 성공: IC, PC, MS, SV, PD, EB, EP, SS, MG, OM, JWT 등.  
미등록 코드(CC, BC …)는 [tcf-gateway](../tcf-gateway/README.md) Catalog 추가 필요.

---

## 8. 설정 (application-local.yml)

```yaml
server:
  port: 8102

nsight:
  tcf-uj:
    deployment-mode: bootrun
    tomcat-gateway-url: http://localhost:8080
    bootrun-host: http://127.0.0.1
    om-gateway-enabled: true
```

---

## 9. tcf-ui와 비교

| | tcf-ui (8099) | tcf-uj (8102) |
|---|---------------|---------------|
| 업무 Relay | WAS 직접 | Gateway 경유 (Cookie) |
| OM JWT Gateway | `callViaGateway` (tcf-ui `om-admin.js`) | API만, JS 분기 없음 |
| JWT Admin | ✅ | ✅ |
| 용도 | 개발·단순 테스트 | **운영형·세션·Gateway 검증** |

---

## 10. 참고

| | |
|---|---|
| [tcf-uj/README.md](../tcf-uj/README.md) | |
| [tcf-gateway-개발가이드.md](./tcf-gateway-개발가이드.md) | |
| [tcf-ui-개발가이드.md](./tcf-ui-개발가이드.md) | 직접 Relay |
