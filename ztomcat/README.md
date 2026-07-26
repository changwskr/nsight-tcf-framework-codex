# ztomcat — NSIGHT 로컬 Tomcat WAR 배포

Spring Boot 3 **WAR** 16개를 **Apache Tomcat 10.1.34**에 올려 로컬에서 운영 환경과 동일한 context path로 테스트하기 위한 도구 모음입니다.

| 항목 | 값 |
|------|-----|
| Tomcat | 10.1.34 (Jakarta EE 10 / Servlet 6) |
| 포트 | **8080** (Tomcat), **9092** (H2 TCP — 공유 `nsight_om`) |
| JDK | **21 필수** (WAR가 Java 21로 빌드됨) |
| WAR 개수 | **12** (업무 9 + tcf-om + tcf-batch + tcf-ui) |
| Gradle | 8.x (`bootWar` 빌드) |

> WAR는 **JDK 21**로 컴파일됩니다. Tomcat을 JDK 18 등으로 기동하면 Spring Boot가 뜨지 않아 **`/sv/online` 404**가 납니다. `start.ps1` / `start.sh`가 JDK 21을 고정합니다.

---

## 목차

1. [빠른 시작](#1-빠른-시작)
2. [디렉터리 구조](#2-디렉터리-구조)
3. [스크립트 목록](#3-스크립트-목록)
4. [Tomcat 설치](#4-tomcat-설치)
5. [기동·중지](#5-기동중지)
6. [WAR 배포 (deploy-wars)](#6-war-배포-deploy-wars)
7. [배포 검증 (verify-deploy)](#7-배포-검증-verify-deploy)
8. [원클릭 재배포 (deploy-restart)](#8-원클릭-재배포-deploy-restart)
9. [설정 (UTF-8·JVM)](#9-설정-utf-8jvm)
10. [배포 URL·Context](#10-배포-urlcontext)
11. [ztomcat vs bootRun (tcf-ui · tcf-batch · tcf-om)](#11-ztomcat-vs-bootrun-tcf-ui--tcf-batch--tcf-om)
12. [트러블슈팅](#12-트러블슈팅)
13. [관련 문서](#13-관련-문서)

---

## 1. 빠른 시작

### Windows

```bat
cd ztomcat
h2-txlog.ps1 start
install-tomcat.bat
deploy-wars.bat all
start.bat
verify-deploy.ps1
```

### Linux / macOS / Git Bash

```bash
cd ztomcat
chmod +x *.sh
./install-tomcat.sh
./deploy-wars.sh all
./start.sh
./verify-deploy.sh
```

### API 호출 확인

```bash
curl http://localhost:8080/sv/actuator/health
curl -X POST http://localhost:8080/sv/online \
  -H "Content-Type: application/json" \
  -d @tcf-ui/src/main/resources/sample-requests/sv-sample-inquiry.json
```

---

## 2. 디렉터리 구조

```text
ztomcat/
├── apache-tomcat-10.1.34/   # install 후 생성 (.gitignore 제외)
│   ├── bin/setenv.bat|sh    # apply-config가 conf/에서 복사
│   ├── conf/server.xml      # UTF-8 Connector 패치 대상
│   └── webapps/             # *.war 배포 위치
├── conf/
│   ├── setenv.bat           # Windows JVM·JDK 21 템플릿 (dev)
│   ├── setenv.sh            # Linux JVM·JDK 21 템플릿 (dev)
│   ├── setenv.prod.bat      # Windows 운영 샘플 (prod)
│   └── setenv.prod.sh       # Linux 운영 샘플 (prod)
├── setenv.local.bat|sh      # 로컬 JAVA_HOME·CATALINA_OPTS 오버라이드
├── install-tomcat.*         # Tomcat 다운로드·압축 해제
├── deploy-wars.*            # Gradle bootWar + webapps 복사
├── start.* / stop.*         # Tomcat 기동·중지
├── apply-config.*           # setenv 복사 + server.xml UTF-8
├── verify-deploy.*          # 주요 13 context health check
├── deploy-restart.*         # stop → deploy all → start → verify
├── h2-txlog.ps1             # 공유 nsight_om H2 TCP 9092
└── README.md
```

| 경로 | 설명 |
|------|------|
| `apache-tomcat-10.1.34/` | Tomcat 설치본 (약 150MB, `install-tomcat` 실행 후 생성) |
| `webapps/{code}.war` | WAR 파일 (예: `sv.war`) |
| `webapps/{code}/` | Tomcat autoDeploy로 풀린 exploded 디렉터리 |

---

## 3. 스크립트 목록

### Windows

| 스크립트 | 설명 |
|----------|------|
| `install-tomcat.bat` | Tomcat 10.1.34 Windows zip 다운로드·압축 해제 |
| `deploy-wars.bat [코드…]` | WAR 빌드·배포 |
| `deploy-wars.bat all` | 16개 전체 (인자 없음과 동일) |
| `deploy-wars.bat sv` | SV만 빌드·배포 |
| `deploy-wars.bat sv cc om` | 복수 선택 배포 |
| `start.bat` | Tomcat 기동 → 내부 `start.ps1` |
| `stop.bat` | Tomcat 중지 → 내부 `stop.ps1` |
| `apply-config.ps1` | UTF-8·setenv 적용 (`start` 시 자동 호출) |
| `verify-deploy.ps1` | 주요 13 context `/actuator/health` 검증(전체 배포 목록과 범위가 다름) |
| `deploy-restart.ps1` | stop → deploy all → start → health 대기 → verify |

> 프로젝트 경로에 괄호 `(23-08-15)`가 있어 `start.bat`/`stop.bat`은 **PowerShell 래퍼**(`start.ps1`/`stop.ps1`)를 사용합니다.

### Linux / macOS

| 스크립트 | 설명 |
|----------|------|
| `install-tomcat.sh` | tar.gz 다운로드·압축 해제 (`curl` 또는 `wget`) |
| `deploy-wars.sh [코드…]` | Windows `.bat`과 동일 시맨틱 |
| `start.sh` | `setenv.local.sh` + `apply-config.sh` + `catalina.sh start` |
| `stop.sh` | `catalina.sh stop` |
| `apply-config.sh` | setenv 복사 + `server.xml` UTF-8 |
| `verify-deploy.sh` | health check (curl) |
| `deploy-restart.sh` | 원클릭 전체 재배포 |

---

## 4. Tomcat 설치

`install-tomcat`은 **최초 1회**만 실행하면 됩니다. 이미 설치되어 있으면 스킵합니다.

| OS | 다운로드 | 설치 경로 |
|----|----------|-----------|
| Windows | `apache-tomcat-10.1.34-windows-x64.zip` | `ztomcat/apache-tomcat-10.1.34/` |
| Linux/macOS | `apache-tomcat-10.1.34.tar.gz` | 동일 |

소스: [Apache Tomcat Archive](https://archive.apache.org/dist/tomcat/tomcat-10/v10.1.34/bin/)

---

## 5. 기동·중지

### 기동

```bat
start.bat          rem Windows
./start.sh         rem Linux
```

기동 시 자동 수행:

1. **JDK 21** 설정 (`%USERPROFILE%\.jdks\temurin-21.0.4` 또는 `JAVA_HOME`)
2. `apply-config` — `conf/setenv.*` → `bin/setenv.*` 복사, `server.xml` UTF-8
3. `catalina start`

접속: `http://localhost:8080`

### 중지

```bat
stop.bat
./stop.sh
```

---

## 6. WAR 배포 (deploy-wars)

Gradle `bootWar`로 WAR를 빌드한 뒤 `webapps/`에 복사합니다.

### 사용법

```bash
deploy-wars.sh              # 16개 전체
deploy-wars.sh all          # 동일
deploy-wars.sh sv           # SV만
deploy-wars.sh sv cc om     # 복수
deploy-wars.sh help         # 도움말
```

### 지원 코드 (16개)

```text
ic pc ms sv pd eb ep ss mg oc om batch ui uj jwt gw
```

| 코드 | Gradle 모듈 | WAR (webapps) | Context |
|------|-------------|---------------|---------|
| ic | `ic-service` | `ic.war` | `/ic` |
| pc | `pc-service` | `pc.war` | `/pc` |
| ms | `ms-service` | `ms.war` | `/ms` |
| sv | `sv-service` | `sv.war` | `/sv` |
| pd | `pd-service` | `pd.war` | `/pd` |
| eb | `eb-service` | `eb.war` | `/eb` |
| ep | `ep-service` | `ep.war` | `/ep` |
| ss | `ss-service` | `ss.war` | `/ss` |
| mg | `mg-service` | `mg.war` | `/mg` |
| oc | `tcf-oc` | `oc.war` | `/oc` |
| om | `tcf-om` | `tcf-om.war` → `om.war` | `/om` |
| batch | `tcf-batch` | `tcf-batch.war` → `zz-batch.war` | `/batch` |
| ui | `tcf-ui` | `tcf-ui.war` → `ui.war` | `/ui` |
| uj | `tcf-uj` | `tcf-uj.war` → `uj.war` | `/uj` |
| jwt | `tcf-jwt` | `jwt.war` | `/jwt` |
| gw | `tcf-gateway` | `gw.war` | `/gw` |

### 전체 vs 단건 배포

| 모드 | Tomcat 재기동 | 소요 시간 | 비고 |
|------|:-------------:|-----------|------|
| `all` (16개) | **권장** (실행 중이면 restart) | 빌드 2~3분 + 기동·배포 3~5분 | `deploy-restart` 사용 |
| 단건 (`sv` 등) | **불필요** | 빌드 ~30초 + autoDeploy ~15초 | WAR 교체 후 context 자동 재배포 |

단건 배포 시 스크립트가 해당 `webapps/{code}/` exploded 디렉터리를 삭제한 뒤 WAR를 복사합니다.

---

## 7. 배포 검증 (verify-deploy)

현재 스크립트에 등록된 주요 13개 context에 대해 `GET /{code}/actuator/health`를 호출합니다. 전체 16개 WAR 배포 여부와 검증 범위는 동일하지 않습니다.

```powershell
verify-deploy.ps1
```

```bash
./verify-deploy.sh
```

출력 예:

```text
  OK   sv -> 200
  OK   om -> 200
[ztomcat] Result: 12 OK, 0 FAIL (total 12)
```

- 타임아웃: 요청당 30초
- 1개라도 FAIL이면 exit code 1 (shell)

---

## 8. 원클릭 재배포 (deploy-restart)

전체 WAR를 깨끗이 다시 올릴 때 사용합니다.

```powershell
deploy-restart.ps1
```

```bash
./deploy-restart.sh
```

순서:

1. `stop`
2. 3초 대기
3. `deploy-wars` (전체)
4. `start`
5. health 12/12 될 때까지 최대 ~5분 폴링 (15초 간격)
6. `verify-deploy`

---

## 9. 설정 (UTF-8·JVM)

### apply-config

`start`마다 실행됩니다.

| 작업 | 내용 |
|------|------|
| setenv 복사 | `ztomcat/conf/setenv.*` → `apache-tomcat-.../bin/setenv.*` |
| server.xml | 8080 Connector에 `URIEncoding="UTF-8" useBodyEncodingForURI="true"` |

### JVM 옵션 (기본)

| 옵션 | 값 |
|------|-----|
| Heap | `-Xms512m -Xmx1536m` |
| Encoding | `-Dfile.encoding=UTF-8` |
| Timezone | `-Duser.timezone=Asia/Seoul` |
| H2 공유 경로 | `-Dnsight.txlog.path={프로젝트루트}/data/nsight-txlog` |
| H2 TCP | `h2-txlog.ps1 start` → JDBC `jdbc:h2:tcp://127.0.0.1:9092/./nsight_om` |

`nsight.txlog.path`는 `conf/setenv.*`에서 `CATALINA_HOME/../../data/nsight-txlog`(프레임워크 루트)로 자동 계산합니다. **H2 TCP 서버**(`h2-txlog.ps1`)를 Tomcat보다 먼저 기동하세요. 로컬 오버라이드: 환경변수 `NSIGHT_TXLOG_PATH`.

Windows 추가: `-Dsun.stdout.encoding=UTF-8`, `-Dsun.stderr.encoding=UTF-8`

### 로컬 오버라이드

| 파일 | 용도 |
|------|------|
| `setenv.local.bat` | Windows — `JAVA_HOME`, `CATALINA_OPTS`, `NSIGHT_TXLOG_PATH` |
| `setenv.local.sh` | Linux — `start.sh`/`deploy-wars.sh`에서 source |

Linux JDK 21 후보 경로 (`setenv.local.sh`):

- `~/.jdks/temurin-21.0.4`
- `/usr/lib/jvm/java-21-openjdk*`
- `/opt/homebrew/opt/openjdk@21`

---

## 10. 배포 URL·Context

### Health

```text
http://localhost:8080/{code}/actuator/health
```

### 온라인 거래 (POST JSON)

```text
POST http://localhost:8080/sv/online
POST http://localhost:8080/sv/SV/online
POST http://localhost:8080/eb/online
```

- **Method:** POST only (`GET /online` → 404/405)
- **Content-Type:** `application/json`
- 샘플: [`docs/sample-requests/`](../zdocs-1/sample-requests/)

### tcf-ui 연동 (Tomcat 모드)

WAR `ui.war` 배포 시 `application-dev.yml`(ztomcat) 또는 `application-prod.yml`(운영)이 적용됩니다. Spring 프로파일은 `conf/setenv.*`(dev) 또는 `conf/setenv.prod.*`(prod) 참고.

```text
OM Admin:  http://localhost:8080/ui/om/admin/login.html
거래 UI:   http://localhost:8080/ui/sv/index.html
```

Relay 대상 업무 API: `http://localhost:8080/{code}/online` (게이트웨이)

---

## 11. ztomcat vs bootRun (tcf-ui · tcf-batch · tcf-om)

### 역할 구분

| 도구 / 모듈 | 포트 | 배포 | 역할 |
|-------------|------|------|------|
| **`ztomcat/`** | 8080 | Tomcat WAR **16개** | 업무 + tcf-oc + tcf-om + batch/ui/uj/jwt/gateway 통합 테스트 |
| [`tcf-scripts/`](../tcf-scripts/README.md) | — | Gradle 빌드·배포 단축 | `buildZtomcatWars`, `deploy.bat` → ztomcat webapps |
| **`tcf-om`** | 8097 (bootRun) 또는 `/om` (Tomcat) | `bootWar` / `bootRun` | OM 운영 API·대시보드·세션·파일(UD) — **현행 모듈** |
| **`om-service`** | 8097 | WAR (레거시) | 샘플 OM만 포함 — **deploy-wars 미사용**, `tcf-om` 사용 |
| **`tcf-ui`** | 8099 (bootRun) 또는 `/ui` | `bootJar` / `bootWar` | 브라우저 Relay·OM Admin UI |
| **`tcf-batch`** | 8098 (bootRun) 또는 `/batch` | `bootJar` / `bootWar` | 대시보드 AP/DB/세션/배포 수집 → 공유 H2 |

```text
Tomcat (ztomcat)              bootRun (별도 프로세스)
────────────────              ───────────────────────
8080 /ic … /mg           8082–8096  *-service
8080 /om      tcf-om          8097       tcf-om
8080 /batch   tcf-batch       8098       tcf-batch
8080 /ui      tcf-ui          8099       tcf-ui
```

### tcf-om vs om-service

| | `tcf-om` | `om-service` |
|---|----------|--------------|
| 상태 | **권장** | 레거시 (settings에만 잔존) |
| WAR | `tcf-om.war` → Tomcat `om.war` | `om.war` (deploy-wars **미사용**) |
| DB | 파일 H2 `data/nsight-txlog/nsight_om` | 인메모리 H2 |
| 기능 | OM Admin 22 서비스, UD 내장 | 샘플 조회만 |

`deploy-wars om` / `buildBusinessWars`는 **`tcf-om:bootWar`** 만 빌드합니다.

### 로컬 OM 대시보드 전체 테스트 (ztomcat 권장)

1. `ztomcat/deploy-wars.bat all` + `start.bat` (또는 `deploy-restart.bat`)
2. `curl -X POST http://localhost:8080/batch/jobs/ap-status/run` (수집 즉시 실행, 선택)
3. http://localhost:8080/ui/om/admin/dashboard.html

### 로컬 OM 대시보드 (bootRun)

1. `tcf-scripts/run-local.bat tcf-om batch ui`
2. http://localhost:8099/om/admin/dashboard.html

### ztomcat 작업 흐름

```text
WAR 빌드·배포  →  ztomcat/deploy-wars  또는  tcf-scripts/deploy.bat
Tomcat 기동    →  ztomcat/start
검증           →  ztomcat/verify-deploy
```

---

## 12. 트러블슈팅

| 증상 | 원인 | 해결 |
|------|------|------|
| `/sv/online` 404, Spring 로그 없음 | Tomcat이 **JDK 18** 등으로 기동 | `start.ps1`/`start.sh`로 JDK 21 확인 |
| health 타임아웃 (검증 대상 일부) | 16 WAR 순차 autoDeploy 중 | `deploy-restart` 후 3~5분 대기 |
| `GET /online` 404 | POST만 지원 | curl `-X POST` 또는 tcf-ui |
| 8080 포트 충돌 | 다른 로컬 서버가 8080 사용 | 해당 프로세스 중지 또는 Tomcat 포트 변경 |
| 한글 깨짐 | Connector encoding | `start` 재실행 (`apply-config`), 로그 UTF-8로 열기 |
| `gradle not found` | Gradle 미설치 | Gradle 8.x PATH 또는 `deploy-wars.bat`의 `GRADLE` 경로 수정 |
| 대시보드 AP/DB 패널 비어 있음 | `tcf-batch` 미기동 또는 수집 전 | `/batch` health 확인, 수집 API 수동 실행 |
| Tomcat·bootRun DB 불일치 | `nsight.txlog.path` 다름 | setenv `-Dnsight.txlog.path` 또는 `NSIGHT_TXLOG_PATH` 확인 |
| 단건 배포 후에도 구버전 | exploded 캐시 | deploy-wars가 `{code}/` 삭제 후 WAR 복사 — 15초 대기 |

### 로그 확인

```text
ztomcat/apache-tomcat-10.1.34/logs/catalina.*.log
ztomcat/apache-tomcat-10.1.34/logs/localhost.*.log
```

Spring Boot 기동 성공 시 `Started *Application` 로그가 context별로 출력됩니다.

---

## 13. 관련 문서

- [프로젝트 README](../README.md) — 전체 가이드·모듈·포트
- [tcf-scripts/README.md](../tcf-scripts/README.md) — Gradle 빌드·Tomcat 배포
- [docs/architecture/38-script.md](../zdocs-1/architecture/38-script.md) — 전체 스크립트 맵·시나리오
- [tcf-ui/README.md](../tcf-ui/README.md) — 브라우저 Relay·OM Admin UI
- [tcf-batch/README.md](../tcf-batch/README.md) — 대시보드 수집 배치
- [tcf-om/README.md](../tcf-om/README.md) — OM 운영 API (tcf-om vs om-service)
- [tcf-web/README.md](../tcf-web/README.md) — `/online` Controller·STF/ETF
- [tcf-cicd/prod/apache/nsight-marketing-routing.conf](../tcf-cicd/prod/apache/nsight-marketing-routing.conf) — Apache ProxyPass 예시
