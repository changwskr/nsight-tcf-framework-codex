# tcf-ui — NSIGHT 온라인 거래 테스트 UI

WebTopSuite/Client 없이 브라우저에서 표준 HTTP/JSON 전문을 작성·전송·응답 확인하기 위한 Spring Boot 애플리케이션입니다. **OM 운영관리 포털**도 이 모듈에서 제공합니다.

| 항목 | 값 |
|------|-----|
| Gradle 모듈 | `tcf-ui` |
| 메인 클래스 | `com.nh.nsight.tcf.ui.NsightTcfUiApplication` |
| bootRun 산출물 | `tcf-ui.jar` (포트 **8099**) |
| WAR (bootWar) | `tcf-ui.war` → ztomcat `ui.war` (`/ui`) |

`tcf-ui`는 **Relay 서버** 역할을 합니다.

```text
브라우저 → tcf-ui (/api/relay, /api/updownload) → 업무 WAS / tcf-om
```

## 배포 모드

| 모드 | 설정 | OM Admin URL |
|------|------|--------------|
| **local (bootRun)** | `deployment-mode: bootrun` | http://localhost:8099/om/admin/login.html |
| **dev/prod (Tomcat WAR)** | `deployment-mode: tomcat` (WAR `/ui`) | http://localhost:8080/ui/om/admin/login.html |

Tomcat WAR: `application-dev.yml` — `tomcat-gateway-url: http://localhost:8080`  
운영: `application-prod.yml` — `tomcat-gateway-url: ${NSIGHT_GATEWAY_BASE_URL}`

## 실행

```bash
# bootRun
gradle :tcf-ui:bootRun
tcf-scripts/run-local.bat ui

# ztomcat
ztomcat/deploy-wars.bat ui
```

## 사전 조건

테스트 대상 업무 WAS·tcf-om이 기동되어 있어야 합니다.

```bash
gradle :sv-service:bootRun   # bootRun: SV 8086
gradle :tcf-om:bootRun       # bootRun: OM 8097
# 또는 ztomcat/deploy-wars.bat all
```

## 패키지 구조

```text
com.nh.nsight.tcf.ui
├── NsightTcfUiApplication
├── application/service/     BusinessModuleCatalog, BusinessTransactionCatalog
├── client/                  TransactionRelayService, GatewayRelayService, UpdownloadRelayService
├── config/                  TcfUiProperties, TcfUiConfiguration
├── entry/web/               TcfApiController, UpdownloadApiController, UiTomcatHtmlRewriteFilter
└── support/                 BusinessModuleDefinitions, BusinessModuleInfo, RelayResult
```

## 설정

`src/main/resources/application.yml`

```yaml
nsight:
  tcf-ui:
    deployment-mode: bootrun      # Tomcat WAR 시 application-dev.yml / prod.yml
    tomcat-gateway-url: http://localhost:8080
    bootrun-host: http://127.0.0.1

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics   # 대시보드 AP/배포 수집용
```

## 주요 API

| API | 설명 |
|-----|------|
| `GET /api/business-modules` | 업무 모듈 목록 |
| `GET /api/business-modules/{code}/target-url` | Relay 대상 URL |
| `POST /api/relay/{code}/online` | 온라인 거래 Relay (Cookie 세션) |
| `POST /api/gateway/om/online` | OM Gateway Relay (JWT Bearer) |
| `GET /api/config` | UI 설정 조회 |
| `POST /api/updownload/upload` | 파일 업로드 Relay (tcf-om) |

## 정적 리소스·공통 JS

| 파일 | 설명 |
|------|------|
| `static/_shared/ui-context.js` | bootRun/Tomcat context path (`/ui`) 자동 보정 |
| `static/_shared/om-admin.js` | OM Admin API Relay (`uiPath()`, `relayFetch()`, JWT·Gateway 분기) |
| `static/_shared/oc-capacity.js` | OC 용량 산정 화면 (메인 `index.html` · `/oc/plan.html` 리다이렉트) |
| `static/oc/plan.html` | Xpilot 용량 산정 UI → 메인 `#capacitySection`으로 이동 |

### 용량 산정 Relay

| API (tcf-ui) | 대상 (tcf-oc) |
|--------------|---------------|
| `GET /api/oc/capacity/defaults` | `GET /api/oc/capacity/defaults` |
| `POST /api/oc/capacity/calculate` | `POST /api/oc/capacity/calculate` |
| `POST /api/oc/capacity/calculate-step` | `POST /api/oc/capacity/calculate-step` |

bootRun: `http://127.0.0.1:8094` · Tomcat: `{gateway}/oc`

화면: `/index.html#capacitySection` (bootRun) · `/ui/index.html#capacitySection` (Tomcat) · `/oc/plan.html`은 메인으로 리다이렉트

Tomcat `/ui` 배포 시 API·정적 경로에 `/ui` 접두가 자동 적용됩니다.

## OM Admin 인증·Relay 분기 (`om-admin.js`)

| 로그인 유형 | OM 업무 거래 경로 | 인증 수단 |
|-------------|-------------------|-----------|
| **일반 로그인** (`OM.Auth.login`) | `/api/relay/om/online` | `JSESSIONID` 쿠키 |
| **SSO** (`OM.Auth.ssoLogin`) | 업무: `/api/gateway/om/online` | SSO 시 JWT 발급 → 업무 거래 `Authorization: Bearer` + Gateway JWT 검증 |
| **JWT 포털 로그인** | `/api/gateway/om/online` | `Authorization: Bearer` + Gateway JWT 검증 |
| **인증 거래** (`OM.Auth.*`) | `/api/relay/om/online` | 항상 직접 relay (Gateway 미경유) |

Gateway `local` 프로필에서 **OM 업무 거래**(인증 거래 제외)는 Bearer JWT **필수**입니다. Bearer 없이 쿠키만으로는 `/om/online` 관문을 통과할 수 없습니다.

`omGatewayEnabled`는 `/api/config`로 제어합니다. 기본값 `true`, `application-local.yml`은 `om-gateway-enabled: false`. JWT Gateway 모드 시 `om-gateway-enabled: true` 및 `tcf-jwt`(:8110)·`tcf-gateway`(:8100, local JWT enabled) 기동 필요.

JWT 토큰은 `sessionStorage` 키 `nsight.jwt.session`에 보관됩니다.

## 거래 테스트 화면

| URL (bootRun) | URL (ztomcat) | 설명 |
|---------------|---------------|------|
| `/index.html` | `/ui/index.html` | 업무 허브 |
| `/{code}/index.html` | `/ui/{code}/index.html` | 단일 거래 |
| `/ud/updownload.html` | `/ui/ud/updownload.html` | UD 파일 관리 |

## OM 운영관리 포털

로그인: `admin01` / `nsight01!`

| 화면 | 경로 |
|------|------|
| 대시보드 | `/om/admin/dashboard.html` |
| 거래로그 | `/om/admin/transaction-log.html` |
| ServiceId | `/om/admin/service-catalog.html` |
| 사용자 / 권한 / 메뉴 / 기능·데이터권한 | `/om/admin/user-auth.html` |
| 공통코드 | `/om/admin/common-code.html` |
| 권한이력 | `/om/admin/auth-history.html` |
| 오류코드 | `/om/admin/error-code.html` |
| Cache | `/om/admin/cache.html` |
| 세션 | `/om/admin/session.html` |
| 배치 / 배포 | `/om/admin/batch.html`, `/om/admin/deploy.html` |
| 파일 관리 | `/om/admin/file-management.html` |
| 환경설정 | `/om/admin/system-config.html` |
| 전문 조립 | `/om/admin/message-composer.html` |

`user-auth.html` 탭: **사용자 · 권한그룹 · 메뉴 · 기능권한 · 데이터권한**.  
구 `function-auth.html`, `data-auth.html`은 `user-auth.html`로 리다이렉트됩니다.

대시보드 AP/DB/세션/배포 패널은 **tcf-batch** 수집 결과를 tcf-om이 조회합니다.

## 샘플 JSON

`tcf-ui/src/main/resources/sample-requests/` — 업무별 inquiry·transactions JSON
