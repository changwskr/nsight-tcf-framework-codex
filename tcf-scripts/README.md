# tcf-scripts — 빌드·실행 스크립트

로컬 개발용 Gradle 래퍼 스크립트 모음입니다. **프로젝트 루트**에서 실행합니다.

> 전체 스크립트 맵: [docs/architecture/38-script.md](../zdocs-1/architecture/38-script.md)
> Gradle·산출물·lib 경로: [docs/manual/gradle.md](../zdocs-1/manual/gradle.md), [docs/manual/lib-module.md](../zdocs-1/manual/lib-module.md)

## 공통 — Gradle 경로

`GRADLE_HOME_OVERRIDE` → `GRADLE_HOME` → PATH `gradle` 순으로 탐색합니다.

```powershell
$env:GRADLE_HOME_OVERRIDE = 'C:\Programming(23-08-15)\gradle-8.10.1'
```

## run-local — 서비스 기동

```bash
# Windows
tcf-scripts\run-local.bat <target>

# Linux/macOS
tcf-scripts/run-local.sh <target>
```

| 인자 | 대상 모듈 |
|------|-----------|
| `sv`, `sv-service` | sv-service |
| `tcf-ui`, `ui` | tcf-ui (8099) |
| `uj`, `tcf-uj` | tcf-uj (8102) |
| `gw`, `tcf-gateway` | tcf-gateway (8100) |
| `jwt`, `tcf-jwt` | tcf-jwt (8110) |
| `tcf-om`, `om`, `ud` | tcf-om (8097, UD API 내장) |
| `batch`, `tcf-batch` | tcf-batch (8098) |
| `all` | 9개 *-service + tcf-om 일괄 기동 (각각 새 창) |

단일 인자: 포그라운드 실행. 복수 인자: 백그라운드(새 창) 실행.

## build — 모듈 빌드

```bash
tcf-scripts\build.bat <target>
tcf-scripts/build.sh <target>
```

| 인자 | 설명 |
|------|------|
| `all` | `clean` + `buildBusinessWars` (11 WAR) |
| `wars` | `buildBusinessWars` (9 *-service + tcf-om + tcf-oc) |
| `ztomcat` | `buildZtomcatWars` (16 WAR — batch + ui + uj + jwt + gateway 포함) |
| `tcf` | `tcf-util`, `tcf-core`, `tcf-web` |
| `ui` | `tcf-ui` bootJar |
| `batch` | `tcf-batch` bootWar |
| `tcf-om`, `om` | `tcf-om` bootWar |
| `services` | 9 *-service `:build` + `tcf-om:bootWar` |
| `sv`, `ic`, … | 개별 모듈 `:build` |

`build-all.bat` / `build-all.sh` → `build all` 호출.

## curl-sample — 샘플 거래 호출

```bash
tcf-scripts\curl-sample.bat sv
tcf-scripts/curl-sample.sh sv
```

샘플 JSON: `tcf-ui/src/main/resources/sample-requests/{code}-sample-inquiry.json`

## deploy — Tomcat webapps 배포

WAR 빌드 후 [ztomcat](../ztomcat/README.md) `webapps`(및 batch는 `wars/`)로 복사합니다.

```bash
tcf-scripts\deploy.bat sv ic om
tcf-scripts/deploy.sh sv ic om
tcf-scripts\deploy.bat batch ui
```

| 인자 | 설명 |
|------|------|
| (없음) / `all` | 업무 11 WAR (`buildBusinessWars`) |
| `sv`, `ic`, … | 선택 업무 — `tcf-*` lib 선행 빌드 + `bootWar` |
| `ud`, `tcf-om`, `om` | `tcf-om.war` → `om.war` |
| `batch`, `tcf-batch` | `tcf-batch.war` → `ztomcat/wars/zz-batch.war` + `batch.xml` |
| `ui`, `tcf-ui` | `tcf-ui.war` → `ui.war` |

환경 변수: `TOMCAT_WEBAPPS`, `GRADLE_HOME` / `GRADLE_HOME_OVERRIDE`

> **16 WAR 전체 배포**: `ztomcat/deploy-wars.bat all`. 현재 `ztomcat/verify-deploy.ps1`은 이 중 13개 context를 검증합니다.

## 포트 참고 (bootRun)

| 모듈 | 포트 |
|------|------|
| ic-service | 8082 |
| pc-service | 8083 |
| ms-service | 8085 |
| sv-service | 8086 |
| pd-service | 8087 |
| eb-service | 8089 |
| ep-service | 8090 |
| ss-service | 8093 |
| mg-service | 8096 |
| tcf-om | 8097 |
| tcf-batch | 8098 |
| tcf-ui | 8099 |
| tcf-uj | 8102 |
| tcf-gateway | 8100 |
| tcf-jwt | 8110 |

Tomcat(ztomcat) 모드: 모든 context **8080** — `/ic` … `/mg`, `/om`, `/batch`, `/ui`, `/uj`, `/gw`, `/jwt`

## Gradle 직접 호출

```bash
gradle buildBusinessWars     # 11 WAR
gradle buildZtomcatWars      # 16 WAR
gradle :tcf-om:bootRun
gradle :tcf-batch:bootRun
gradle :tcf-ui:bootRun
```
