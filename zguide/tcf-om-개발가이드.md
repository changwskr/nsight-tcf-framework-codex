# tcf-om 개발자 가이드

> **Operation Management (OM)** · 포트 **8097** · Context **`/om`** · 업무코드 **`OM`**

---

## 1. 이 모듈이 하는 일

NSIGHT **운영 기준정보 원장** — 사용자·권한·메뉴·ServiceId Catalog·거래통제·Timeout·오류코드·세션·로그·배치·배포·Cache·대시보드·파일(UD).

**업무 WAR와 동일 TCF 패턴**이지만 OM 전용 Handler 24개, serviceId 80+.

---

## 2. 5분 빠른 시작

```bash
gradle :tcf-om:bootRun

# OM 샘플
curl -X POST http://127.0.0.1:8097/om/online ^
  -H "Content-Type: application/json" ^
  -d @tcf-ui/src/main/resources/sample-requests/om-sample-inquiry.json

# OM Admin UI (tcf-ui)
# http://localhost:8099/om/admin/login.html
```

---

## 3. 실행

| | |
|---|---|
| bootRun | `gradle :tcf-om:bootRun` |
| 스크립트 | `tcf-scripts/run-local.bat tcf-om` |
| WAR | `om.war` / `tcf-om.war` → `/om` |

⚠ **om-service와 포트 8097 충돌** — 동시 기동 금지. 신규 개발은 **tcf-om만** 사용.

---

## 4. 구조

```
tcf-om/.../marketing/om/
├── entry/handler/    Om*Handler (24개)
├── entry/facade/
├── entry/web/        OmUpdownloadFileController (/ud)
├── application/service/
├── client/           OmBatchRemoteClient, OmJwtSsoClient
├── persistence/
└── support/          OmDatabaseMigration, ServiceCatalogSeedData
```

---

## 5. 주요 Handler (일부)

| Handler | 영역 |
|---------|------|
| OmAuthHandler | login, logout, ssoLogin |
| OmUserHandler | 사용자 |
| OmServiceCatalogHandler | **ServiceId Catalog** |
| OmTransactionControlHandler | **거래통제** |
| OmTimeoutPolicyHandler | Timeout |
| OmTransactionLogHandler | 거래로그 조회 |
| OmCacheHandler | Cache 관리 |
| OmDashboardHandler | 대시보드 |

전체: [zman/12-OM운영관리.md](../zman/12-OM운영관리.md)

---

## 6. Catalog · Seed (중요)

업무 serviceId 등록 시 **반드시** 반영:

- `tcf-om/src/main/resources/data.sql`
- `ServiceCatalogSeedData.java`

필드 `HANDLER_CLASS` = `{Domain}Handler` 클래스명

---

## 7. 의존성

```gradle
tcf-util, tcf-core, tcf-web, tcf-cache
Spring Session JDBC, MyBatis
```

---

## 8. 연계 모듈

| 모듈 | 연계 |
|------|------|
| tcf-ui / tcf-uj | OM Admin Relay |
| tcf-batch | Dashboard 수집 (`OmBatchRemoteClient`) |
| tcf-jwt | SSO Issue (`OmJwtSsoClient`) |
| tcf-gateway | SESSIONDB 세션 관문 |

---

## 9. 참고

| | |
|---|---|
| [tcf-om/README.md](../tcf-om/README.md) | |
| [zman/12-OM운영관리.md](../zman/12-OM운영관리.md) | |
| [zman/13-거래통제.md](../zman/13-거래통제.md) | |
| [tcf-ui-개발가이드.md](./tcf-ui-개발가이드.md) | Admin UI |
