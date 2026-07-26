# NSIGHT TCF Framework

NSIGHT HTTP/JSON **표준 전문**을 **TCF(Transaction Control Framework)** 로 처리하는 멀티 모듈 Gradle 프로젝트입니다.

> 공식 설계안: [zdocs-1/설계자료/README.md](zdocs-1/설계자료/README.md)  
> 구현 아키텍처: [zdocs-1/architecture/architecture.md](zdocs-1/architecture/architecture.md)

## 핵심 설계 원칙

1. **Handler 중심** — 업무 개발자는 `TransactionHandler` + `serviceId` 등록에 집중 (도메인당 핸들러 1개, `serviceIds()`로 다중 거래 처리)
2. **공통 파이프라인** — Header 검증·세션·권한·거래통제·Timeout·로깅·응답 조립은 STF/TCF/ETF가 담당
3. **업무 독립 WAR** — 9개 업무 + OM·OC는 동일 패턴의 Spring Boot WAR
4. **이중 배포** — 개발 `bootRun`(포트 분리) · 통합 `ztomcat`(8080 게이트웨이)

## 모듈 구조

```text
nsight-tcf-framework
├─ tcf-util              공통 유틸 (Spring 없음)
├─ tcf-core              TCF 엔진 (STF/TCF/ETF, Dispatcher, 거래통제·Timeout)
├─ tcf-web               HTTP (/online, TcfGateway, AutoConfiguration, 거래로그 DB)
├─ tcf-eai               업무 간 표준 전문 연동 공통 모듈 (EAI, serviceId 기반 호출)
├─ tcf-cache             EhCache / Spring Cache
├─ tcf-om                운영관리 OM + 파일 업·다운로드 UD
├─ tcf-oc                용량 산정(CAP)·통합 환경설정(ENV) (bootRun :8094, WAR /oc)
├─ tcf-batch             AP/DB/세션/배포 수집 · 운영 대시보드 데이터
├─ tcf-ui                거래 테스트 UI · OM Admin Relay (bootRun :8099)
├─ tcf-uj                gateway 경유 테스트 UI (bootRun :8102)
├─ tcf-gateway           API Gateway · SESSIONDB 관문 (bootRun :8100)
├─ tcf-jwt               JWT 발급·JWKS (bootRun :8110, WAR /jwt) — 검증은 Gateway
├─ tcf-help              HELP 마크다운 문서 리소스 (Java 없음)
├─ tcf-ai-methodology    업무모델 자동화 Model Studio (bootRun :8787)
├─ tcf-scripts           빌드·실행·배포 스크립트
├─ tcf-cicd              local/dev/prod 설정 SoT
├─ ztomcat               로컬 Tomcat 8080 (16 context)
└─ ic-service … mg-service   9개 업무 WAR
```

| 모듈 | 산출물 | README |
|------|--------|--------|
| `tcf-core` | JAR | [tcf-core/README.md](tcf-core/README.md) |
| `tcf-web` | JAR | [tcf-web/README.md](tcf-web/README.md) |
| `tcf-eai` | JAR | — |
| `tcf-cache` | JAR | [tcf-cache/README.md](tcf-cache/README.md) |
| `tcf-om` | WAR `/om` | [tcf-om/README.md](tcf-om/README.md) |
| `tcf-oc` | WAR `/oc` | [tcf-oc/README.md](tcf-oc/README.md) |
| `tcf-batch` | WAR `/batch` | [tcf-batch/README.md](tcf-batch/README.md) |
| `tcf-ui` | WAR `/ui` | [tcf-ui/README.md](tcf-ui/README.md) |
| `tcf-uj` | WAR `/uj` | [tcf-uj/README.md](tcf-uj/README.md) |
| `tcf-gateway` | WAR `/gw` | [tcf-gateway/README.md](tcf-gateway/README.md) |
| `tcf-jwt` | WAR `/jwt` | [tcf-jwt/README.md](tcf-jwt/README.md) |
| `tcf-help` | 문서 리소스 | [tcf-help/README.md](tcf-help/README.md) |
| `tcf-ai-methodology` | JAR/WAR | [tcf-ai-methodology/README.md](tcf-ai-methodology/README.md) |
| `tcf-scripts` | 스크립트 | [tcf-scripts/README.md](tcf-scripts/README.md) |
| `tcf-cicd` | 설정 | [tcf-cicd/README.md](tcf-cicd/README.md) |
| `ztomcat` | Tomcat | [ztomcat/README.md](ztomcat/README.md) |

### 업무 WAR (`*-service`)

| 모듈 | 업무코드 | README |
|------|----------|--------|
| `ic-service` | IC | [ic-service/README.md](ic-service/README.md) |
| `pc-service` | PC | [pc-service/README.md](pc-service/README.md) |
| `ms-service` | MS | [ms-service/README.md](ms-service/README.md) |
| `sv-service` | SV | [sv-service/README.md](sv-service/README.md) |
| `pd-service` | PD | [pd-service/README.md](pd-service/README.md) |
| `eb-service` | EB | [eb-service/README.md](eb-service/README.md) |
| `ep-service` | EP | [ep-service/README.md](ep-service/README.md) |
| `ss-service` | SS | [ss-service/README.md](ss-service/README.md) |
| `mg-service` | MG | [mg-service/README.md](mg-service/README.md) |

레거시 `om-service`는 [om-service/README.md](om-service/README.md) 참고 — 배포·개발은 **`tcf-om`** 사용.

**의존 방향:** `tcf-util → tcf-core → tcf-web → tcf-cache(선택) → 업무 서비스 / tcf-om / tcf-batch / tcf-jwt / tcf-gateway`

## 업무 WAR 패키지 규약 (6계층)

업무 모듈(`*-service`, `tcf-om`, `tcf-jwt` 등)은 eb-service 기준 **6계층** 패키지를 사용합니다.

```text
com.nh.nsight.marketing.{업무}   (또는 com.nh.nsight.auth.jwt, com.nh.nsight.gateway)
├── application/
│   ├── service/       업무 Service
│   ├── rule/          업무 Rule
│   └── scheduler/     @Scheduled (해당 시)
├── client/            외부 WAS·API Client (해당 시)
├── config/            Spring 설정·Properties
├── entry/
│   ├── handler/       TransactionHandler (도메인당 1개, serviceIds 등록)
│   ├── facade/        Handler → Service 위임
│   └── web/           REST Controller·Filter (해당 시)
├── persistence/
│   ├── dao/           JDBC DAO
│   └── mapper/        MyBatis Mapper
└── support/           상수·도메인 헬퍼

처리 흐름: entry/handler → entry/facade → application/service → application/rule → persistence/dao|mapper
```

WAR 메인 클래스는 `com.nh.nsight.tcf.web.support.NsightWarBootstrap`를 상속합니다 (`tcf-web`, 구 `boot` 패키지).

## TCF 처리 흐름

```text
Client / tcf-ui / REST API
   ↓  POST /{businessCode}/online  (StandardRequest: header + body)
OnlineTransactionController / TcfGateway (entry/web · entry/facade)
   ↓
TCF.process()
   ├─ STF.preProcess()
   │     Header 검증 · GUID/TraceId · 세션/권한 · 멱등성
   │     거래통제(TCF_TRANSACTION_CONTROL) · Timeout 정책 조회
   │     거래로그 INSERT (PROCESSING)
   ├─ OnlineTransactionTimeoutExecutor
   │     └─ TransactionDispatcher → TransactionHandler (serviceId)
   │           entry/facade → application/service → application/rule → persistence/dao|mapper
   │           · TX timeout AOP · MyBatis query timeout
   └─ ETF.success / businessFail / systemError
         StandardResponse · 감사·메트릭 · 거래로그 UPDATE
```

**ServiceId 형식:** `{업무코드}.{업무명}.{처리유형}` — 예) `SV.Customer.selectSummary`, `OM.User.inquiry`

## 프레임워크 역량

| 영역 | 테이블 / 구성 | 적용 시점 | 설계안 |
|------|---------------|-----------|--------|
| 거래통제 | `TCF_TRANSACTION_CONTROL` | STF — 7필드 + `BLOCK_YN=Y` 차단 | [거래통제](zdocs-1/설계자료/NSIGHT%20거래통제%20설계안.docx) |
| Timeout | `TCF_SERVICE_TIMEOUT_POLICY` | STF 조회 → Online/TX/MyBatis 적용 | [서비스별 Timeout](zdocs-1/설계자료/서비스별%20Timeout%20설계안.docx) |
| 거래로그 | `TCF_TRANSACTION_LOG` | STF PROCESSING → ETF SUCCESS/FAIL | [거래로그](zdocs-1/설계자료/NSIGHT%20거래로그%20관리%20설계안.docx) |
| 세션 | `SPRING_SESSION` (JDBC) | STF SessionValidator | [세션관리](zdocs-1/설계자료/NSIGHT%20TCF%20Framework%20세션관리%20설계안.docx) |
| Cache | EhCache (`tcf-cache`) | 공통코드·ServiceId 등 | [Cache 관리](zdocs-1/설계자료/NSIGHT%20TCF%20Framework%20Cache%20관리%20설계안.docx) |
| 오류코드 | `OM_ERROR_CODE` | ETF — `E-{DOMAIN}-{CATEGORY}-{NNNN}` | [오류코드·메시지](zdocs-1/설계자료/NSIGHT%20오류코드·메시지%20설계안.docx) |
| ServiceId | `OM_SERVICE_CATALOG` | Dispatcher Handler 매핑 | [서비스 ID](zdocs-1/설계자료/NSIGHT%20서비스%20ID%20관리%20설계안.docx) |

### Timeout 적용 (구현)

| 정책 컬럼 | 적용 지점 |
|-----------|-----------|
| `ONLINE_TIMEOUT_SEC` | `OnlineTransactionTimeoutExecutor` — dispatch 구간 |
| `TX_TIMEOUT_SEC` | `@Transactional` AOP |
| `DB_QUERY_TIMEOUT_SEC` | MyBatis interceptor |

```yaml
nsight.tcf:
  transaction-control-enabled: true
  timeout-policy-enabled: true
```

상세: [40-header-7-transaction-control.md](zdocs-1/architecture/40-header-7-transaction-control.md), [41-service-timeout-policy.md](zdocs-1/architecture/41-service-timeout-policy.md)

## 배포 모드

| 모드 | 설명 | 대표 URL |
|------|------|----------|
| **bootRun** | 모듈별 독립 JVM | `http://localhost:8086/sv/online`, OM UI `:8099` |
| **ztomcat** | Tomcat 8080 · WAR 16개 | `http://localhost:8080/sv/online`, OM UI `/ui` |

```text
ztomcat (8080)              bootRun (별도 프로세스)
────────────────            ─────────────────────
/ic … /mg  업무 9           8082–8096  *-service
/om        tcf-om           8097       tcf-om
/oc        tcf-oc           8094       tcf-oc
/batch     tcf-batch        8098       tcf-batch
/ui        tcf-ui           8099       tcf-ui
```

WAR 패키징: 각 WAR의 `WEB-INF/lib`에 `tcf-*` JAR 포함 (Tomcat `lib/` 공유 배치 아님).  
상세: [배포관리 설계안](zdocs-1/설계자료/NSIGHT%20TCF%20Framework%20배포관리%20설계안.docx), [ztomcat/README.md](ztomcat/README.md)

## 빠른 시작

### bootRun

```bash
gradle :sv-service:bootRun          # SV 업무
gradle :tcf-om:bootRun              # OM (8097)
gradle :tcf-batch:bootRun           # 배치 (8098)
gradle :tcf-ui:bootRun              # UI (8099)

tcf-scripts\run-local.bat sv
tcf-scripts\run-local.bat tcf-om batch ui
```

### ztomcat

```bat
cd ztomcat
h2-txlog.ps1 start
deploy-wars.bat all
start.bat
verify-deploy.ps1
```

## 빌드

```bash
gradle buildBusinessWars    # 업무 11 WAR (9 *-service + tcf-om + tcf-oc)
gradle buildZtomcatWars     # 16 WAR (+ tcf-batch, tcf-ui, tcf-uj, tcf-jwt, tcf-gateway)
tcf-scripts\build.bat ztomcat
```

## 포트 (bootRun)

| 포트 | 모듈 | 포트 | 모듈 |
|------|------|------|------|
| 8082 | ic | 8089 | eb |
| 8083 | pc | 8090 | ep |
| 8085 | ms | 8093 | ss |
| 8086 | sv | 8096 | mg |
| 8087 | pd | 8094 | **tcf-oc** |
| | | 8097 | **tcf-om** |
| | | 8098 | **tcf-batch** |
| | | 8099 | **tcf-ui** |
| | | 8100 | **tcf-gateway** |
| | | 8102 | **tcf-uj** |
| | | 8110 | **tcf-jwt** |

Tomcat 모드: 모든 context **8080**

## OM 관리 포털

| 모드 | 로그인 URL | 계정 |
|------|------------|------|
| bootRun | http://localhost:8099/om/admin/login.html | `admin01` / `nsight01!` |
| ztomcat | http://localhost:8080/ui/om/admin/login.html | 동일 |

### 화면 (ztomcat 기준)

| 구분 | 화면 | 경로 |
|------|------|------|
| **운영** | 대시보드 | `/ui/om/admin/dashboard.html` |
| | 거래로그 | `/ui/om/admin/transaction-log.html` |
| | 배치 | `/ui/om/admin/batch.html` |
| | 배포 | `/ui/om/admin/deploy.html` |
| | Cache | `/ui/om/admin/cache.html` |
| | 세션 | `/ui/om/admin/session.html` |
| | 환경설정 | `/ui/om/admin/system-config.html` |
| **통제·정책** | ServiceId | `/ui/om/admin/service-catalog.html` |
| | 거래통제 | `/ui/om/admin/transaction-control.html` |
| | Timeout 정책 | `/ui/om/admin/timeout-policy.html` |
| **권한·마스터** | 사용자/권한/메뉴/기능·데이터권한 | `/ui/om/admin/user-auth.html` |
| | 공통코드 | `/ui/om/admin/common-code.html` |
| | 오류코드 | `/ui/om/admin/error-code.html` |
| | 권한이력 | `/ui/om/admin/auth-history.html` |
| **파일** | 파일 관리 | `/ui/om/admin/file-management.html` |

`user-auth.html` 탭: 사용자 · 권한그룹 · 메뉴 · 기능권한 · 데이터권한

## 샘플 호출

```bash
curl -X POST http://localhost:8080/sv/online \
  -H "Content-Type: application/json" \
  -d @tcf-ui/src/main/resources/sample-requests/sv-sample-inquiry.json
```

## 공유 H2 (거래로그 · OM · SESSION)

| 환경 | 설정 |
|------|------|
| H2 TCP | `ztomcat/h2-txlog.ps1 start` → TCP **9092** |
| JDBC (dev) | `jdbc:h2:tcp://127.0.0.1:9092/./nsight_om` |
| bootRun | `nsight.txlog.path` = `{프로젝트}/data/nsight-txlog` |
| ztomcat | `ztomcat/conf/setenv.bat` → `-Dnsight.txlog.path=...` |

기능권한 시드: OM 기동 시 `OmDatabaseMigration` 자동 MERGE, 또는 `tcf-cicd\local\script\seed-function-auth.bat`

## 요구 사항

- Java 21
- Gradle 8.x

## 문서

### 전체 문서 목차·요약

| 문서 | 경로 |
|------|------|
| 전체 문서 읽기 순서 (A~J 경로) | [ztcf-다이어리/2026-07-25 전체 파일 목차/NSIGHT-TCF-전체문서-읽기순서.md](ztcf-다이어리/2026-07-25%20전체%20파일%20목차/NSIGHT-TCF-전체문서-읽기순서.md) |
| 목차별 핵심 내용 요약 | [ztcf-다이어리/2026-07-25 전체 파일 목차/NSIGHT-TCF-목차순서별-핵심내용.md](ztcf-다이어리/2026-07-25%20전체%20파일%20목차/NSIGHT-TCF-목차순서별-핵심내용.md) |
| 개발자 핵심 지식 | [ztcf-다이어리/2026-07-25 전체 파일 목차/NSIGHT-TCF-개발자-핵심지식.md](ztcf-다이어리/2026-07-25%20전체%20파일%20목차/NSIGHT-TCF-개발자-핵심지식.md) |
| 목차 안내 (진입점) | [ztcf-다이어리/2026-07-25 전체 파일 목차/README.md](ztcf-다이어리/2026-07-25%20전체%20파일%20목차/README.md) |

### 방법론 설계서 (ztcf-methodology)

| 문서 | 경로 |
|------|------|
| EB UI 레이아웃 설계서 | [ztcf-methodology/EB-UI-레이아웃-설계서.md](ztcf-methodology/EB-UI-레이아웃-설계서.md) |
| EB 프로그램 설계서 | [ztcf-methodology/EB-프로그램-설계서.md](ztcf-methodology/EB-프로그램-설계서.md) |

### 설계자료 (Word)

| | |
|--|--|
| **설계안 전체 목록** | [zdocs-1/설계자료/README.md](zdocs-1/설계자료/README.md) |

### 아키텍처 (Markdown)

| 문서 | 경로 |
|------|------|
| 아키텍처 정의서 | [zdocs-1/architecture/architecture.md](zdocs-1/architecture/architecture.md) |
| TCF 개발 가이드 | [zdocs-1/TCF_FRAMEWORK_GUIDE.md](zdocs-1/TCF_FRAMEWORK_GUIDE.md) |
| 빌드·모듈 | [zdocs-1/architecture/22-build-project.md](zdocs-1/architecture/22-build-project.md) |
| 거래통제 (7필드) | [zdocs-1/architecture/40-header-7-transaction-control.md](zdocs-1/architecture/40-header-7-transaction-control.md) |
| Timeout 정책 | [zdocs-1/architecture/41-service-timeout-policy.md](zdocs-1/architecture/41-service-timeout-policy.md) |
| 스크립트 | [zdocs-1/architecture/38-script.md](zdocs-1/architecture/38-script.md) |

### 운영 매뉴얼

| 문서 | 경로 |
|------|------|
| Gradle 명령 | [zdocs-1/manual/gradle.md](zdocs-1/manual/gradle.md) |
| 환경변수 | [zdocs-1/manual/environment-variables.md](zdocs-1/manual/environment-variables.md) |
| 산출물·기동 | [zdocs-1/manual/artifacts.md](zdocs-1/manual/artifacts.md) |
| CI/CD SoT | [tcf-cicd/README.md](tcf-cicd/README.md) |

### 교재·매뉴얼 아카이브

| 폴더 | 내용 |
|------|------|
| `ztcf-개발북/` | NSIGHT TCF 개발 입문서 제1~20부 (docx 원본) |
| `znsight-man/` | 개발 입문서·개발 매뉴얼·구축 방법론 마크다운 변환본 |
| `znsight-guide-word/` | TCF 개발 매뉴얼 78~87 등 (docx 원본) |
| `znsight-구축방법론/` | 아키텍처 구축 방법론 1~3 (docx 원본) |
| `zdocs-2/` | 주제별 개발 노트 (AOP·페이징·세션·SSO 등) |
| `ztcf-집필본/`, `ztcf-집필본-md/` | TCF 집필본 (원본·마크다운 변환본) |
| `ztcfbook/`, `ztcfbook-h/`, `ztcfbook-m/` | TCF 북 시리즈 (원본·HTML·마크다운) |
| `ztcf-book-capacity-md/`, `znsight-capacity-word/` | 용량 산정 교재 (마크다운·docx) |
| `ztcf-engine-config-info/`, `znsight-config-info/` | 엔진·환경설정 정보 |
| `ztcf-다이어리/` | 개발 다이어리·일자별 작업 기록 |
