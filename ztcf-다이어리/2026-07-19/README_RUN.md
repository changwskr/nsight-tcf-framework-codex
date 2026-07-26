# NSIGHT TCF Framework — 실행 가이드

## 개요

멀티모듈 Gradle 프로젝트의 **로컬 실행(bootRun)** · **Tomcat WAR** · **Docker** · **Kubernetes** 방법을 안내합니다.

이중 배포 모델:

| 방식 | 설명 |
|------|------|
| 개발 `bootRun` | 모듈별 포트 분리 (8082~8110, UI 8099 등) |
| 통합 `ztomcat` | Tomcat **8080**에 WAR context 배포 (`/sv`, `/eb`, `/gw` …) |

---

## 사전 요구사항

- **JDK 21** (Temurin 권장)
- **Gradle 8.x** (`GRADLE_HOME` 또는 PATH)
- (선택) Docker Desktop — 컨테이너 실행
- (선택) kubectl — K8s 배포

JDK 환경 스크립트:

```bat
scripts\env-jdk21.bat
```

```powershell
$env:GRADLE_HOME_OVERRIDE = 'C:\Programming(23-08-15)\gradle-8.10.1'
```

---

## 권장: tcf-scripts로 실행

프로젝트 **루트**에서:

```bat
tcf-scripts\run-local.bat <target>
```

```bash
tcf-scripts/run-local.sh <target>
```

| 인자 | 모듈 | 포트(local) |
|------|------|-------------|
| `ic` | ic-service | 8082 |
| `pc` | pc-service | 8083 |
| `ms` | ms-service | 8085 |
| `sv` | sv-service | 8086 |
| `pd` | pd-service | 8087 |
| `eb` | eb-service | 8089 |
| `ep` | ep-service | 8090 |
| `ss` | ss-service | 8093 |
| `mg` | mg-service | 8096 |
| `om` / `tcf-om` / `ud` | tcf-om | 8097 |
| `batch` | tcf-batch | 8098 |
| `ui` / `tcf-ui` | tcf-ui | 8099 |
| `gw` / `tcf-gateway` | tcf-gateway | 8100 |
| `uj` / `tcf-uj` | tcf-uj | 8102 |
| `jwt` / `tcf-jwt` | tcf-jwt | 8110 |
| `oc` / `tcf-oc` | tcf-oc | 8094 |
| `all` | 업무+OM 일괄 (새 창) | — |

예:

```bat
tcf-scripts\run-local.bat gw
tcf-scripts\run-local.bat ui
tcf-scripts\run-local.bat eb
tcf-scripts\run-local.bat ep
tcf-scripts\run-local.bat sv
```

상세: [tcf-scripts/README.md](../../tcf-scripts/README.md)

---

## 모듈 스크립트 직접 실행

각 모듈 `scripts/run-local.bat` / `.sh`:

```bat
eb-service\scripts\run-local.bat
sv-service\scripts\run-local.bat
ep-service\scripts\run-local.bat
tcf-ui\scripts\run-local.bat
tcf-gateway\scripts\run-local.bat
```

빌드:

```bat
eb-service\scripts\build.bat
tcf-scripts\build.bat eb
```

Tomcat WAR 배포 (모듈별):

```bat
eb-service\scripts\deploy.bat
sv-service\scripts\deploy.bat
```

---

## Gradle 수동 실행

레포 루트에서:

```bash
# 공통 모듈 빌드 후 업무 기동
./gradlew :tcf-util:build :tcf-core:build :tcf-web:build :eb-service:bootRun

./gradlew :sv-service:bootRun
./gradlew :ep-service:bootRun
./gradlew :tcf-ui:bootRun
./gradlew :tcf-gateway:bootRun
./gradlew :tcf-jwt:bootRun
./gradlew :tcf-om:bootRun
```

프로필:

```bash
# local (기본에 가깝게 application-local.yml)
./gradlew :eb-service:bootRun --args='--spring.profiles.active=local'

# Windows PowerShell
$env:SPRING_PROFILES_ACTIVE='local'
./gradlew :eb-service:bootRun
```

WAR 생성:

```bash
./gradlew :eb-service:bootWar
# 산출물: eb-service/build/libs/eb.war
```

---

## Docker 실행

이미지가 있는 모듈 예 (`eb` / `sv` / `tcf-ui`):

```bat
eb-service\scripts\docker\docker-build.bat
eb-service\scripts\docker\docker-run.bat -d

sv-service\scripts\docker\docker-build.bat
sv-service\scripts\docker\docker-run.bat -d

tcf-ui\scripts\docker\docker-build.bat
tcf-ui\scripts\docker\docker-run.bat -d
```

포트 매핑: `0.0.0.0:HOST:CONTAINER` (예: UI `8099`, SV `8086`, EB `8089`)

중앙 디렉터리: [tcf-docker/README.md](../../tcf-docker/README.md)

---

## Kubernetes 실행

```bash
cd tcf-k8s/scripts
./deploy.sh development <commit-sha> eb,sv,ui
```

Windows:

```bat
deploy.bat development abc1234 eb,sv,ui
```

사전: Deployment 매니페스트 apply (예: `eb-service/scripts/k8s/`)  
상세: [tcf-k8s/README.md](../../tcf-k8s/README.md)

---

## 접속 URL (local bootRun)

### UI / Gateway / 인증

| 용도 | URL |
|------|-----|
| tcf-ui | http://localhost:8099/ |
| OM Admin (UI) | http://localhost:8099/om/admin/login.html |
| tcf-uj | http://localhost:8102/ |
| Gateway | http://localhost:8100/ |
| Gateway 라우트 관리 | http://localhost:8100/admin/routes.html |
| JWT | http://localhost:8110/ |
| OM | http://localhost:8097/ |

### 업무 온라인 API

| 업무 | URL |
|------|-----|
| IC | `POST http://localhost:8082/ic/online` |
| SV | `POST http://localhost:8086/sv/online` |
| EB | `POST http://localhost:8089/eb/online` |
| EP | `POST http://localhost:8090/ep/online` |
| … | 포트는 ARC02 / BusinessModuleDefinitions 참고 |

### Health / H2

```text
http://localhost:{port}/actuator/health
http://localhost:{port}/h2-console   # local에서 활성화된 모듈
```

### ztomcat 통합 (8080)

```text
http://localhost:8080/sv/online
http://localhost:8080/eb/online
http://localhost:8080/ep/online
```

---

## 빠른 동작 확인

```bash
# Health
curl http://localhost:8089/actuator/health
curl http://localhost:8099/actuator/health

# EP 샘플 (파일 경로는 레포 기준)
curl -X POST http://localhost:8090/ep/online ^
  -H "Content-Type: application/json" ^
  -d @tcf-ui/src/main/resources/sample-requests/ep-sample-inquiry.json
```

통합 시험 UI: **tcf-uj** + `sample-requests` JSON 사용을 권장합니다.

---

## 환경별 설정

| Profile | 용도 | 설정 위치 |
|---------|------|-----------|
| `local` | 로컬 H2·bootRun | `*/application-local.yml`, `tcf-cicd/local` |
| `dev` | 개발 | `*/application-dev.yml`, `tcf-cicd/dev` |
| `prod` | 운영 | `*/application-prod.yml`, `tcf-cicd/prod` |

DB 상세: [DATABASE_README.md](./DATABASE_README.md)

---

## 권장 로컬 기동 순서 (최소)

1. `tcf-gateway` (8100) — 라우팅
2. `tcf-jwt` (8110) — 인증이 필요할 때
3. 업무 WAR (`sv`, `eb`, `ep` 등)
4. `tcf-ui` (8099) 또는 `tcf-uj` (8102)
5. (선택) `tcf-om` (8097)

EB→EP 이벤트 확인 시: **ep** 를 먼저 띄운 뒤 **eb** 를 기동합니다.

---

## 문제 해결

### 빌드 실패
1. `java -version` → 21인지 확인 (`scripts\env-jdk21.bat`)
2. `gradle -version` / `GRADLE_HOME_OVERRIDE`
3. `./gradlew :tcf-core:build --refresh-dependencies`

### 실행 실패 / 포트 충돌
1. 해당 모듈 포트가 사용 중인지 확인 (예: 8089, 8099, 8100)
2. 콘솔의 Spring 기동 로그·BindException 확인
3. Docker면 `docker ps` 로 이미 떠 있는 `nsight-*` 확인

### DB / 스키마
1. H2 콘솔 또는 [DATABASE_README.md](./DATABASE_README.md)
2. `schema.sql` / `data.sql` / Migration 로그 확인
3. 거래로그 파일 잠금: `./data/nsight-txlog` 동시 접속

### Gateway 404
- `TCF_GATEWAY_ROUTE`에 `ENV_CODE=LOCAL` + `BUSINESS_CODE` 등록 여부
- http://localhost:8100/admin/routes.html

### Docker localhost 접속 안 됨
- 포트 매핑 확인: `docker port nsight-ui` → `0.0.0.0:8099->8099/tcp`
- `docker-run.bat -d` 로 재생성 (매핑 없이 기동된 경우)

---

## 로그 / 종료

- 로그: 콘솔 또는 `*\scripts\docker\docker-logs.bat`
- 종료: `Ctrl+C` (bootRun) / `docker-stop.bat` / `docker rm -f nsight-*`

거래 추적 키: **GUID · TraceId · ServiceId · BUSINESS_CODE**

---

## 관련 문서

| 문서 | 내용 |
|------|------|
| [README.md](../../README.md) | 프로젝트 개요 |
| [DATABASE_README.md](./DATABASE_README.md) | DB 초기화 |
| ARC02 / ARC03 / ARC04 | 아키텍처 구성도 |
| [tcf-scripts/README.md](../../tcf-scripts/README.md) | 스크립트 맵 |

---

**작성일**: 2026-07-19  
**대상**: NSIGHT TCF Framework (`nsight-tcf-framework`)
