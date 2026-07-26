# NSIGHT TCF — 설계자료

본 디렉터리는 NSIGHT TCF Framework의 **공식 설계안(Word)** 을 보관합니다.  
구현·운영 시에는 아래 설계안과 `docs/architecture/` Markdown 문서를 함께 참고하세요.

> **패키지 규약:** 업무 WAR(`*-service`, `tcf-om`, `tcf-jwt`, `tcf-gateway` 등)는 eb-service 기준 **6계층** 패키지를 사용합니다.  
> 상세: [README.md](../../README.md#업무-war-패키지-규약-6계층), [01-application-layer.md](../architecture/01-application-layer.md)

## 플랫폼 모듈 (구현 참조)

| 모듈 | 역할 | README |
|------|------|--------|
| `tcf-util` | Spring 비의존 공통 유틸 | [tcf-util/README.md](../../tcf-util/README.md) |
| `tcf-core` | STF/TCF/ETF, Dispatcher | [tcf-core/README.md](../../tcf-core/README.md) |
| `tcf-web` | `/online`, TcfGateway, WAR Bootstrap | [tcf-web/README.md](../../tcf-web/README.md) |
| `tcf-cache` | EhCache / Spring Cache | [tcf-cache/README.md](../../tcf-cache/README.md) |
| `tcf-om` | OM Admin + UD (`/ud/files`) | [tcf-om/README.md](../../tcf-om/README.md) |
| `tcf-batch` | AP/DB/세션/배포 수집 | [tcf-batch/README.md](../../tcf-batch/README.md) |
| `tcf-ui` | 거래 테스트 UI · OM Relay (`/ui`) | [tcf-ui/README.md](../../tcf-ui/README.md) |
| `tcf-uj` | gateway 경유 테스트 UI · JWT Admin (`/uj`) | [tcf-uj/README.md](../../tcf-uj/README.md) |
| `tcf-gateway` | API Gateway · SESSIONDB 관문 (`/gw`) | [tcf-gateway/README.md](../../tcf-gateway/README.md) |
| `tcf-jwt` | JWT 발급·갱신·폐기 (`/jwt`) | [tcf-jwt/README.md](../../tcf-jwt/README.md) |
| `tcf-eai` | 서비스 간 HTTP/JSON 연동 Client | [tcf-eai/](../../tcf-eai/) |
| `tcf-cicd` | local/dev/prod 설정 SoT | [tcf-cicd/README.md](../../tcf-cicd/README.md) |

> **OM 배포:** `om-service`는 레거시 샘플 모듈입니다. CI/CD·ztomcat 배포는 **`tcf-om`** 을 사용합니다.

## 설계안 목록

| 설계안 | 주제 | 구현 모듈 | 아키텍처 문서 |
|--------|------|-----------|---------------|
| [NSIGHT TCF Framework 설계안](NSIGHT%20TCF%20Framework%20설계안.docx) | 프레임워크 전체·모듈·TCF 흐름 | `tcf-util`, `tcf-core`, `tcf-web`, `*-service`, `tcf-om` | [architecture.md](../architecture/architecture.md), [33-TCF.md](../architecture/33-TCF.md) |
| [NSIGHT 거래통제 설계안](NSIGHT%20거래통제%20설계안.docx) | Header 7필드 Allow-List | `tcf-core`, `tcf-web`, `tcf-om` | [40-header-7-transaction-control.md](../architecture/40-header-7-transaction-control.md) |
| [서비스별 Timeout 설계안](서비스별%20Timeout%20설계안.docx) | Timeout 종류·기본값·처리 기준 | `tcf-core`, `tcf-web` | [41-service-timeout-policy.md](../architecture/41-service-timeout-policy.md) |
| [NSIGHT TCF Framework 타임아웃 관리 설계안](NSIGHT%20TCF%20Framework%20타임아웃%20관리%20설계안.docx) | Timeout 적용 흐름·OM 화면 | `tcf-core`, `tcf-web`, `tcf-om`, `tcf-ui` | [41-service-timeout-policy.md](../architecture/41-service-timeout-policy.md), [08-timeout.md](../architecture/08-timeout.md) |
| [NSIGHT 거래로그 관리 설계안](NSIGHT%20거래로그%20관리%20설계안.docx) | PROCESSING→SUCCESS/FAIL, LOGDB | `tcf-core`, `tcf-web`, `tcf-gateway` | [37-transaction-log.md](../architecture/37-transaction-log.md) |
| [NSIGHT 공통전문조립 설계안](NSIGHT%20공통전문조립%20설계안.docx) | StandardResponse 조립·마스킹 | `tcf-core` (ETF) | [04-messaging.md](../architecture/04-messaging.md), [36-ETF.md](../architecture/36-ETF.md) |
| [NSIGHT 오류코드·메시지 설계안](NSIGHT%20오류코드·메시지%20설계안.docx) | `E-{DOMAIN}-{CATEGORY}-{NNNN}` | `tcf-core`, `tcf-om` | [05-exception.md](../architecture/05-exception.md) |
| [NSIGHT TCF Framework 세션관리 설계안](NSIGHT%20TCF%20Framework%20세션관리%20설계안.docx) | Spring Session JDBC · SESSIONDB | `tcf-core`, `tcf-web`, `tcf-om`, `tcf-gateway` | [10-session.md](../architecture/10-session.md), [11-login.md](../architecture/11-login.md) |
| [NSIGHT TCF Framework Cache 관리 설계안](NSIGHT%20TCF%20Framework%20Cache%20관리%20설계안.docx) | EhCache·Spring Cache·OM Cache | `tcf-cache`, `tcf-om`, `tcf-ui` | [12-cache.md](../architecture/12-cache.md) |
| [NSIGHT 서비스 ID 관리 설계안](NSIGHT%20서비스%20ID%20관리%20설계안.docx) | ServiceId·Handler Registry | `tcf-core`, `tcf-om` | [03-transaction.md](../architecture/03-transaction.md) |
| [NSIGHT 사용자 정보관리 설계안](NSIGHT%20사용자%20정보관리%20설계안.docx) | 사용자·권한그룹·세션 | `tcf-om`, `tcf-ui`, `tcf-uj` | [11-login.md](../architecture/11-login.md) |
| [NSIGHT 기능권한 설계안](NSIGHT%20기능권한%20설계안.docx) | CRUD·다운로드 버튼 권한 | `tcf-om`, `tcf-ui`, `tcf-uj` | — |
| [NSIGHT 메뉴관리 설계안](NSIGHT%20메뉴관리%20설계안.docx) | 메뉴 트리·화면 내비게이션 | `tcf-om`, `tcf-ui`, `tcf-uj` | — |
| [NSIGHT 공통코드 관리 설계안](NSIGHT%20공통코드%20관리%20설계안.docx) | 공통코드·채널·캐시 | `tcf-cache`, `tcf-om` | — |
| [NSIGHT TCF Framework 파일관리 설계안](NSIGHT%20TCF%20Framework%20파일관리%20설계안.docx) | UD 업·다운로드 | `tcf-om` (`entry/web`, `/ud/files`) | [18-fileupdownload.md](../architecture/18-fileupdownload.md) |
| [NSIGHT 환경설정 조회 설계안](NSIGHT%20환경설정%20조회%20설계안.docx) | Runtime·Profile 조회(읽기 전용) | `tcf-om` | [25-env-profile.md](../architecture/25-env-profile.md) |
| [NSIGHT TCF Framework 운영 대시보드 설계안](NSIGHT%20TCF%20Framework%20운영%20대시보드%20설계안-대쉬보드.docx) | AP/DB/세션/배포·TPS·Timeout | `tcf-om`, `tcf-batch`, `tcf-ui`, `tcf-uj` | [13-batch.md](../architecture/13-batch.md) |
| [NSIGHT TCF Framework 헬스체크 설계안](NSIGHT%20TCF%20Framework%20헬스체크%20설계안.docx) | Liveness/Readiness/Deep/Smoke | `tcf-web`, `tcf-batch` | — |
| [NSIGHT TCF Framework 배포관리 설계안](NSIGHT%20TCF%20Framework%20배포관리%20설계안.docx) | CI/CD·WAR·Rolling·Rollback | `tcf-cicd`, `ztomcat` | [16-deploy.md](../architecture/16-deploy.md) |
| [NSIGHT 배치·스케줄러 설계안](NSIGHT%20배치·스케줄러%20설계안.docx) | Scheduler·Job·Execution | `tcf-batch`, `tcf-om` | [13-batch.md](../architecture/13-batch.md), [15-schedule.md](../architecture/15-schedule.md) |

## 아키텍처 Markdown (42~52, 운영·플랫폼)

| 문서 | 주제 | 구현 모듈 |
|------|------|-----------|
| [42-jwt.md](../architecture/42-jwt.md) | JWT 발급·갱신·폐기·JWKS | `tcf-jwt`, `tcf-gateway` |
| [43-security-operations.md](../architecture/43-security-operations.md) | 보안 운영·Denylist·감사 | `tcf-jwt`, `tcf-om` |
| [44-observability.md](../architecture/44-observability.md) | 로그·메트릭·헬스·대시보드 | `tcf-batch`, `tcf-om` |
| [45-disaster-recovery.md](../architecture/45-disaster-recovery.md) | DR·백업·복구 절차 | `tcf-cicd`, `ztomcat` |
| [46-service-integration-contract.md](../architecture/46-service-integration-contract.md) | 업무 간 연동 Contract | `tcf-eai`, `*-service` |
| [47-data-governance.md](../architecture/47-data-governance.md) | 데이터 분류·마스킹·보존 | `tcf-core`, `tcf-om` |
| [48-multi-module-dependencies.md](../architecture/48-multi-module-dependencies.md) | Gradle 모듈 의존 그래프 | 전 모듈 |
| [49-release-strategy.md](../architecture/49-release-strategy.md) | 릴리즈·브랜치·CI/CD | `tcf-cicd` |
| [50-test-architecture.md](../architecture/50-test-architecture.md) | 단위·통합·품질 게이트 | 전 모듈 |
| [51-api-gateway.md](../architecture/51-api-gateway.md) | API Gateway·라우팅·GRF/GSF/GEF | `tcf-gateway` |
| [52-om-operations.md](../architecture/52-om-operations.md) | OM Handler·카탈로그·배포 통제 | `tcf-om`, `tcf-ui` |
| [53-naming-conventions.md](../architecture/53-naming-conventions.md) | 명명규칙 통합 정의서 | 전 모듈 |

개발 매뉴얼 Markdown: [znsight-man/README.md](../../znsight-man/README.md) · Word 원본: [znsight-guide-word](../../znsight-guide-word/)

## 설계안 ↔ OM 관리 화면

| OM 화면 | serviceId (예) | 설계안 |
|---------|----------------|--------|
| `…/om/admin/dashboard.html` | `OM.Dashboard.inquiry` | 운영 대시보드 |
| `…/om/admin/transaction-log.html` | `OM.TransactionLog.inquiry` | 거래로그 관리 |
| `…/om/admin/service-catalog.html` | `OM.ServiceCatalog.*` | 서비스 ID 관리 |
| `…/om/admin/transaction-control.html` | `OM.TransactionControl.*` | 거래통제 |
| `…/om/admin/timeout-policy.html` | `OM.TimeoutPolicy.*` | Timeout / 타임아웃 관리 |
| `…/om/admin/user-auth.html` | `OM.User.*`, `OM.AuthGroup.*`, … | 사용자 정보·기능·메뉴·데이터권한 |
| `…/om/admin/common-code.html` | `OM.CommonCode.*` | 공통코드 관리 |
| `…/om/admin/error-code.html` | `OM.ErrorCode.*` | 오류코드·메시지 |
| `…/om/admin/auth-history.html` | `OM.AuthHistory.inquiry` | 권한이력 |
| `…/om/admin/cache.html` | `OM.Cache.*` | Cache 관리 |
| `…/om/admin/session.html` | `OM.Session.*` | 세션관리 |
| `…/om/admin/batch.html` | `OM.Batch.*` | 배치·스케줄러 |
| `…/om/admin/deploy.html` | `OM.Deploy.*` | 배포관리 |
| `…/om/admin/file-management.html` | `/ud/files` (REST) | 파일관리 |
| `…/om/admin/system-config.html` | `OM.SystemConfig.inquiry` | 환경설정 조회 |

**UI 접두 (ztomcat / bootRun)**

| 모드 | OM Admin 로그인 예 |
|------|-------------------|
| tcf-ui (bootRun) | http://localhost:8099/om/admin/login.html |
| tcf-ui (ztomcat) | http://localhost:8080/ui/om/admin/login.html |
| tcf-uj (bootRun) | http://localhost:8102/om/admin/login.html |
| tcf-uj (ztomcat) | http://localhost:8080/uj/om/admin/login.html |

**JWT Admin (tcf-ui 8099 / tcf-uj 8102):** `/jwt/admin/*` — `tcf-jwt` (bootRun **8110**) 기동 필요

## 업무 WAR 패키지 (6계층 요약)

```text
com.nh.nsight.marketing.{code}   (또는 auth.jwt, gateway)
├── application/service|rule|scheduler
├── client/            (해당 시)
├── config/
├── entry/handler|facade|web
├── persistence/dao|mapper
└── support/

처리 흐름: entry/handler → entry/facade → application/service → application/rule → persistence
```

WAR Bootstrap: `com.nh.nsight.tcf.web.support.NsightWarBootstrap` (`tcf-web`)

## 핵심 테이블 (설계 공통)

| 테이블 | 역할 |
|--------|------|
| `TCF_TRANSACTION_CONTROL` | Header 7필드 거래 차단 (`BLOCK_YN=Y`) |
| `TCF_SERVICE_TIMEOUT_POLICY` | 서비스별 Online/TX/DB Query Timeout |
| `TCF_TRANSACTION_LOG` | 거래 상태 (PROCESSING → SUCCESS/FAIL/TIMEOUT/UNKNOWN) |
| `TCF_GATEWAY_ROUTE` | Gateway downstream 라우팅 (ENV + BUSINESS_CODE) |
| `TCF_GATEWAY_TX_LOG` | Gateway 프록시 거래로그 |
| `OM_SERVICE_CATALOG` | ServiceId 마스터·Handler·감사·Timeout 기본값 |
| `OM_FUNCTION_AUTH` | 권한그룹별 메뉴 CRUD·다운로드 |
| `SPRING_SESSION` / `SPRING_SESSION_ATTRIBUTES` | Spring Session JDBC (공유 SESSIONDB) |
| `TCF_JWT_TOKEN` / `TCF_REFRESH_TOKEN` / `TCF_TOKEN_DENYLIST` | JWT 토큰·폐기 (tcf-jwt) |

## 읽는 순서 (권장)

1. **NSIGHT TCF Framework 설계안** — 전체 구조·모듈·처리 흐름
2. **거래통제** + **서비스별 Timeout** — STF 전처리 정책
3. **거래로그** + **공통전문조립** + **오류코드·메시지** — ETF 후처리
4. **사용자·기능·메뉴·공통코드** — OM 마스터
5. **운영 대시보드** + **헬스체크** + **배포·배치** — 운영·인프라
6. **세션관리** + **tcf-gateway** — SESSIONDB·Gateway 관문 (tcf-uj 연동)

Markdown 구현 문서는 [docs/architecture/architecture.md](../architecture/architecture.md)에서 전체 목차를 확인할 수 있습니다.
