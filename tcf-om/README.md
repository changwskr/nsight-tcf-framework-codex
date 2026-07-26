# tcf-om — 운영관리 (OM) TCF 서비스

기존 `om-service`를 TCF 프레임워크(`TransactionHandler`, `tcf-core`) 기반으로 마이그레이션한 운영관리 독립 실행 모듈입니다. **파일 업·다운로드(UD)** API도 내장합니다.

| 항목 | 값 |
|------|-----|
| Gradle 모듈 | `tcf-om` |
| 업무코드 | `OM` (UD: `UD`) |
| 메인 클래스 | `com.nh.nsight.marketing.om.NsightTcfOmApplication` |
| bootRun 포트 | **8097** |
| WAR (bootWar) | `tcf-om.war` → ztomcat `om.war` (`/om`) |

> `om-service`와 동일 포트(8097)를 사용합니다. 동시 기동 시 포트 충돌이 발생하므로 **tcf-om 하나만** 실행하세요.

## 주요 기능

- OM 운영 도메인 Handler **24개** (`OM.*` serviceId 80+, **도메인(Service)당 핸들러 1개** — `serviceIds()`로 다중 거래 처리)
- 로그인·세션·권한 (`OM.Auth.*`, `OM.Session.*`)
- **거래통제** CRUD (`OM.TransactionControl.*`) — Header 7필드 차단 규칙
- **Timeout 정책** CRUD (`OM.TimeoutPolicy.*`) — 서비스별 timeout 설정
- 마스터 CRUD: 사용자, 권한그룹, 메뉴, **기능권한**, ServiceId, 공통코드, 오류코드
- **데이터권한** 조회 (`OM.DataAuth.inquiry`) — user-auth 탭 통합
- EhCache 공통코드 캐싱 (`tcf-cache` 연동)
- 거래로그·감사로그·Health Check·배치·Cache·배포 관리
- **런타임·장애진단** (`OM.Runtime.inquiry`) — WAR별 Thread/JVM/DB Pool/SQL 수집·자동 원인판정
- 운영 대시보드 (tcf-batch 수집 데이터 조회)
- 환경설정 (Tomcat/bootRun 배포 모드별 런타임 값 표시)
- 파일 업·다운로드 REST (`/ud/files`, `entry/web/OmUpdownloadFileController`, TcfGateway 경유)
- 기동 시 `OmDatabaseMigration` — 스키마·시드·기능권한 MERGE

## TCF Handler 예시

> CRUD 등 같은 도메인의 거래는 도메인 핸들러 1개가 `serviceIds()` + `doHandle` 분기로 처리합니다.
> 예: `OM.User.*` → `OmUserHandler`, `OM.Deploy.*` → `OmDeployHandler`.

| serviceId | 설명 |
|-----------|------|
| `OM.Auth.login` | OM 로그인 |
| `OM.Dashboard.inquiry` | 운영 대시보드 |
| `OM.SystemConfig.inquiry` | 환경설정 조회 |
| `OM.User.inquiry` / `.save` / `.update` / `.delete` | 사용자 CRUD |
| `OM.AuthGroup.inquiry` ~ `.delete` | 권한그룹 CRUD |
| `OM.Menu.inquiry` ~ `.delete` | 메뉴 CRUD |
| `OM.FunctionAuth.inquiry` ~ `.delete` | 기능권한 CRUD |
| `OM.TransactionControl.inquiry` ~ `.delete` | 거래통제 CRUD |
| `OM.TimeoutPolicy.inquiry` ~ `.delete` | Timeout 정책 CRUD |
| `OM.DataAuth.inquiry` | 데이터권한 조회 |
| `OM.ServiceCatalog.inquiry` ~ `.delete` | ServiceId CRUD |
| `OM.CommonCode.inquiry` ~ `.delete` | 공통코드 CRUD |
| `OM.ErrorCode.inquiry` ~ `.delete` | 오류코드 CRUD |
| `OM.Cache.inquiry` / `.delete` | EhCache 조회·삭제 |
| `OM.Batch.execute` | 배치 실행 |
| `OM.Runtime.inquiry` | 런타임·장애진단 (RTM-010~090) |

**런타임·장애진단 설계:** [`docs/runtime-diagnostics-design.md`](docs/runtime-diagnostics-design.md)

전체 목록: `tcf-ui/src/main/resources/sample-requests/om-transactions.json`

## 실행

```bash
# bootRun
gradle :tcf-om:bootRun
tcf-scripts/run-local.bat tcf-om

# ztomcat
ztomcat/deploy-wars.bat om
```

ztomcat WAR: `spring.profiles.active=dev` — `application-dev.yml` ([25-env-profile.md](../zdocs-1/architecture/25-env-profile.md))

## API

```bash
# bootRun
curl -X POST http://localhost:8097/om/online \
  -H "Content-Type: application/json" \
  -d @tcf-ui/src/main/resources/sample-requests/om-sample-inquiry.json

# ztomcat
curl -X POST http://localhost:8080/om/online \
  -H "Content-Type: application/json" \
  -d @tcf-ui/src/main/resources/sample-requests/om-sample-inquiry.json
```

## tcf-batch 연동

| 모드 | `nsight.om.batch-service-url` |
|------|-------------------------------|
| bootRun | `http://127.0.0.1:8098` |
| ztomcat | `http://127.0.0.1:8080/batch` |

## tcf-ui 연동

| 모드 | OM Admin URL |
|------|--------------|
| bootRun | http://localhost:8099/om/admin/login.html |
| ztomcat | http://localhost:8080/ui/om/admin/login.html |

런타임·장애진단: `/om/admin/runtime-diagnostics.html` (사이드바 **런타임·장애진단** 메뉴)

관련 설계안: [docs/설계자료/README.md](../zdocs-1/설계자료/README.md) · [docs/runtime-diagnostics-design.md](docs/runtime-diagnostics-design.md)

## 패키지 구조

```text
com.nh.nsight.marketing.om
├── NsightTcfOmApplication              # extends NsightWarBootstrap
├── application/
│   ├── service/       OmUserService, OmAuthService, …
│   ├── rule/          OmOperationRule, OmDeployApprovalRule, …
│   └── scheduler/     OmSessionCleanupScheduler
├── client/            OmBatchRemoteClient, OmCicdClientService
├── config/            Spring Session, 스케줄, UD Properties
├── entry/
│   ├── handler/       OM.* 도메인 핸들러 (24개, serviceId 80+, 도메인당 1개)
│   ├── facade/        업무 Facade
│   └── web/           OmUpdownloadFileController (UD REST)
├── persistence/
│   ├── dao/           JDBC/MyBatis DAO
│   └── mapper/        MyBatis Mapper
└── support/           OmDatabaseMigration, 시드·Health

처리 흐름: entry/handler → entry/facade → application/service → application/rule → persistence
```

## om-service와의 관계

| 모듈 | 설명 |
|------|------|
| `tcf-om` | TCF 마이그레이션 완료본 (**권장**) |
| `om-service` | 레거시 WAR — `buildBusinessWars`·`deploy-wars`에 **미포함** |

## 의존성

`tcf-core`, `tcf-web`, `tcf-cache`, Spring Session JDBC, MyBatis, H2
