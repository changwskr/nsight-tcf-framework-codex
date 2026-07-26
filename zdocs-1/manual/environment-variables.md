# 환경변수·시스템 속성 가이드

| 항목 | 내용 |
|------|------|
| 대상 | 로컬 개발·ztomcat·운영 배포 담당자 |
| 관련 | [20-env-spring.md](../architecture/20-env-spring.md), [25-env-profile.md](../architecture/25-env-profile.md), [21-env-tomcat.md](../architecture/21-env-tomcat.md), [gradle.md](gradle.md) |

---

## 1. 개요 — 세 가지 설정 계층

NSIGHT TCF에서 “환경”은 아래 **세 계층**으로 나뉩니다.

| 계층 | 예 | 설정 위치 |
|------|-----|-----------|
| **OS 환경변수** | `NSIGHT_TXLOG_PATH`, `JAVA_HOME` | shell / Windows 시스템 |
| **JVM 시스템 속성** | `-Dnsight.txlog.path=...`, `-Dspring.profiles.active=dev` | `setenv.*`, Gradle `bootRun`, `CATALINA_OPTS` |
| **Spring placeholder** | `${NSIGHT_SV_DB_URL:...}` | `application-*.yml` |

Spring Boot는 **환경변수와 시스템 속성을 동일 키로 바인딩**합니다.

```text
NSIGHT_TXLOG_PATH  (OS env)  ↔  nsight.txlog.path  (property)
SPRING_PROFILES_ACTIVE       ↔  spring.profiles.active
```

**우선순위 (높음 → 낮음):** JVM `-D` > OS 환경변수 > `application-{profile}.yml` > `application.yml` > AutoConfiguration 기본값

---

## 2. 빠른 참조 — 자주 쓰는 변수

### 2.1 로컬 개발 (bootRun)

| 변수/속성 | 필수 | 기본·자동 | 용도 |
|-----------|:----:|-----------|------|
| `JAVA_HOME` | ● | Temurin 21 | JDK 21 |
| `GRADLE_HOME` / `GRADLE_HOME_OVERRIDE` | ● | PATH | Gradle 8.x |
| `nsight.txlog.path` | | bootRun 자동 | 거래로그 H2 디렉터리 |
| `spring.profiles.active` | | `local` (bootRun) | Spring 프로파일 |

bootRun 시 루트 `build.gradle`이 자동 주입:

```text
-Dspring.profiles.active=local
-Dnsight.txlog.path={프로젝트루트}/data/nsight-txlog
-Dfile.encoding=UTF-8
```

### 2.2 ztomcat (dev 통합)

| 변수/속성 | 필수 | 기본·자동 | 용도 |
|-----------|:----:|-----------|------|
| `JAVA_HOME` | ● | `setenv.local.*` | JDK 21 |
| `NSIGHT_TXLOG_PATH` | | `{프로젝트}/data/nsight-txlog` | H2 공유 경로 |
| `CATALINA_HOME` / `CATALINA_BASE` | | `ztomcat/apache-tomcat-10.1.34` | Tomcat |
| `-Dspring.profiles.active` | | **`dev`** (`conf/setenv.*`) | ztomcat WAR |

### 2.3 운영 (prod)

| 변수 | 필수 | 용도 |
|------|:----:|------|
| `NSIGHT_GATEWAY_BASE_URL` | ● | 공개 API·UI Relay 게이트웨이 |
| `NSIGHT_{CODE}_DB_URL` 등 | ● | 업무·OM Oracle 접속 |
| `NSIGHT_TXLOG_JDBC_URL` | ○ | 거래로그 DB (운영 Oracle 등) |
| `NSIGHT_TXLOG_PATH` | ○ | Tomcat file H2 사용 시 |
| `NSIGHT_BATCH_SERVICE_URL` | ○ | OM → batch (미설정 시 `{gateway}/batch`) |
| `NSIGHT_UPDOWNLOAD_PATH` | ○ | OM 파일 저장 경로 |

---

## 3. 빌드·Gradle

| 변수 | OS | 설명 | 예 |
|------|-----|------|-----|
| `JAVA_HOME` | Win/Linux | JDK 21 루트 | `C:\Program Files\Eclipse Adoptium\jdk-21` |
| `GRADLE_HOME` | Win/Linux | Gradle 설치 경로 | `C:\gradle-8.10.1` |
| `GRADLE_HOME_OVERRIDE` | Win/Linux | 괄호 경로 등 **우선** Gradle | 스크립트·IDE |

**사용처:** `tcf-scripts/*`, `{module}/scripts/*`, `deploy-wars.ps1`, `build-all.ps1`

```powershell
$env:GRADLE_HOME_OVERRIDE = 'C:\Programming(23-08-15)\gradle-8.10.1'
```

Gradle Wrapper는 **없음** — 위 변수 또는 PATH `gradle` 필요.  
상세: [gradle.md](gradle.md)

---

## 4. JDK·JVM (Tomcat / bootRun)

| 변수 | 설명 |
|------|------|
| `JAVA_HOME` | Tomcat·Gradle이 사용하는 JDK 21 |
| `JAVA_TOOL_OPTIONS` | Tomcat `setenv.*`에서 UTF-8 기본 (`-Dfile.encoding=UTF-8` 등) |
| `JAVA_OPTS` | Tomcat 공통 JVM 옵션 |
| `CATALINA_OPTS` | Tomcat 인스턴스 옵션 — **NSIGHT 핵심 설정 대부분 여기** |

### `CATALINA_OPTS`에 주입되는 대표 `-D` (ztomcat)

| 시스템 속성 | dev (`conf/setenv.*`) | prod (`setenv.prod.*`) |
|-------------|----------------------|------------------------|
| `spring.profiles.active` | `dev` | `prod` |
| `nsight.txlog.path` | `NSIGHT_TXLOG_PATH` → `-D` | 동일 |
| `user.timezone` | `Asia/Seoul` | `Asia/Seoul` |
| `logging.charset.console` | `MS949` (Windows dev) | `UTF-8` |
| `logging.charset.file` | `UTF-8` | `UTF-8` |

**파일:** `ztomcat/conf/setenv.bat`, `setenv.prod.sh`, `setenv.local.bat|sh`

---

## 5. Tomcat·ztomcat

| 변수 | 설명 | 기본 |
|------|------|------|
| `CATALINA_HOME` | Tomcat 설치 디렉터리 | `ztomcat/apache-tomcat-10.1.34` |
| `CATALINA_BASE` | 인스턴스 base (로그·conf) | 보통 `CATALINA_HOME`과 동일 |
| `TOMCAT_WEBAPPS` | WAR 복사 대상 | `{CATALINA_HOME}/webapps` |
| `NSIGHT_TXLOG_PATH` | 공유 H2 루트 → `-Dnsight.txlog.path` | `{프로젝트}/data/nsight-txlog` |
| `ZTOMCAT_SKIP_DEPLOY` | `1`이면 start 시 deploy-wars skip | `0` |
| `ZTOMCAT_REQUIRE_JAVA` | `0`이면 stop 시 JAVA 검증 skip | `1` |

**로컬 오버라이드:** `ztomcat/setenv.local.bat` / `setenv.local.sh`  
**운영 외부 mount:** `apply-tomcat-config.sh` → `$CATALINA_BASE/conf/nsight/` (prod yml)

---

## 6. Spring 프로파일

| 변수 / 속성 | 값 | 활성화 시점 |
|-------------|-----|-------------|
| `SPRING_PROFILES_ACTIVE` | `local` / `dev` / `prod` | OS env (Spring Boot 표준) |
| `-Dspring.profiles.active` | 동일 | JVM (Tomcat setenv, Gradle) |

| 프로파일 | 전형적 실행 | setenv / Gradle |
|----------|-------------|-----------------|
| **`local`** | `gradle :sv-service:bootRun` | `build.gradle` bootRun |
| **`dev`** | ztomcat 8080 | `conf/setenv.*` |
| **`prod`** | 운영 Tomcat | `setenv.prod.*` |

`prod`는 `dev` yml을 **그룹으로 함께 로드** (`spring.profiles.group.prod`).  
상세: [25-env-profile.md](../architecture/25-env-profile.md)

```powershell
# 수동 오버라이드
$env:SPRING_PROFILES_ACTIVE = 'dev'
gradle :sv-service:bootRun "-Dspring.profiles.active=dev"
```

---

## 7. NSIGHT 공통 — 거래로그·H2

### 7.1 `nsight.txlog.path` / `NSIGHT_TXLOG_PATH`

| 항목 | 내용 |
|------|------|
| **역할** | `TCF_TX_LOG` H2 파일 디렉터리 (`nsight_om`) |
| **공유** | bootRun · ztomcat · tcf-batch · tcf-om |
| **물리 경로** | `{nsight.txlog.path}/nsight_om` (H2 file) |

**설정 경로**

| 실행 | 설정 방법 |
|------|-----------|
| bootRun | Gradle `systemProperty 'nsight.txlog.path'` + `NsightTxlogPathEnvironmentPostProcessor` |
| ztomcat | `NSIGHT_TXLOG_PATH` env → `CATALINA_OPTS -Dnsight.txlog.path=...` |
| 수동 | OS env `NSIGHT_TXLOG_PATH` 또는 `-Dnsight.txlog.path=...` |

**자동 해석 (`NsightTxlogPathEnvironmentPostProcessor`):**

1. 이미 `nsight.txlog.path` / `-D` / `NSIGHT_TXLOG_PATH` 있으면 **skip**
2. 없으면 `settings.gradle` 있는 프로젝트 루트 → `data/nsight-txlog`

```powershell
# Windows — Tomcat·bootRun 공유 경로 맞추기
$env:NSIGHT_TXLOG_PATH = 'D:\data\nsight-txlog'
```

### 7.2 거래로그 DB (운영 Oracle 등)

`application-prod.yml` placeholder:

| 변수 | 용도 | 기본 (로컬 H2) |
|------|------|----------------|
| `NSIGHT_TXLOG_JDBC_URL` | 거래로그 JDBC URL | file H2 `nsight_om` |
| `NSIGHT_TXLOG_DB_USER` | 사용자 | `sa` |
| `NSIGHT_TXLOG_DB_PASSWORD` | 비밀번호 | (빈) |
| `NSIGHT_TXLOG_DB_DRIVER` | 드라이버 | `org.h2.Driver` |

---

## 8. NSIGHT 운영 — 게이트웨이·OM·배치

| 변수 | yml 키 | 설명 | prod 기본 |
|------|--------|------|-----------|
| `NSIGHT_GATEWAY_BASE_URL` | `nsight.gateway.base-url`, `nsight.tcf-ui.tomcat-gateway-url` | Apache 443 등 **공개 URL** | `https://marketing.example.com` |
| `NSIGHT_BATCH_SERVICE_URL` | `nsight.om.batch-service-url` | OM → tcf-batch | `{gateway}/batch` |
| `NSIGHT_UPDOWNLOAD_PATH` | `nsight.om.storage-path` | OM 파일 업·다운로드 | `/var/nsight/updownload` |

**설정 파일:** `tcf-om/application-prod.yml`, `tcf-ui/application-prod.yml`, `tcf-batch/application-prod.yml`

```bash
# Linux 운영 Tomcat setenv.sh
export NSIGHT_GATEWAY_BASE_URL=https://marketing.company.com
export NSIGHT_BATCH_SERVICE_URL=https://marketing.company.com/batch
export NSIGHT_UPDOWNLOAD_PATH=/var/nsight/updownload
```

---

## 9. NSIGHT 업무 DB (prod)

업무 WAR `application-prod.yml` 패턴 — **업무 코드 `{CODE}`** 대문자:

| 변수 | 설명 |
|------|------|
| `NSIGHT_{CODE}_DB_URL` | JDBC URL |
| `NSIGHT_{CODE}_DB_USER` | 사용자 |
| `NSIGHT_{CODE}_DB_PASSWORD` | 비밀번호 |
| `NSIGHT_{CODE}_DB_DRIVER` | 드라이버 (기본 `oracle.jdbc.OracleDriver`) |

### 9.1 업무 코드 목록

| CODE | 모듈 | 예시 SID |
|------|------|----------|
| CC | cc-service | nsight_cc |
| IC | ic-service | nsight_ic |
| PC | pc-service | nsight_pc |
| BC | bc-service | nsight_bc |
| MS | ms-service | nsight_ms |
| SV | sv-service | nsight_sv |
| PD | pd-service | nsight_pd |
| CM | cm-service | nsight_cm |
| EB | eb-service | nsight_eb |
| EP | ep-service | nsight_ep |
| BP | bp-service | nsight_bp |
| BD | bd-service | nsight_bd |
| SS | ss-service | nsight_ss |
| CS | cs-service | nsight_cs |
| CT | ct-service | nsight_ct |
| MG | mg-service | nsight_mg |
| OM | tcf-om | nsight_om |
| BATCH | tcf-batch | nsight_om (대시보드 DB) |

```bash
export NSIGHT_SV_DB_URL=jdbc:oracle:thin:@//dbhost:1521/nsight_sv
export NSIGHT_SV_DB_USER=nsight_sv
export NSIGHT_SV_DB_PASSWORD=********
export NSIGHT_OM_DB_URL=jdbc:oracle:thin:@//dbhost:1521/nsight_om
```

로컬 `local`/`dev`는 H2 in-memory 또는 file — **위 Oracle 변수 불필요**.

---

## 10. 로깅

| 변수/속성 | 설명 |
|-----------|------|
| `CATALINA_BASE` | dev yml `logging.file.name`: `${CATALINA_BASE}/logs/nsight-*.log` |
| `logging.charset.console` | Tomcat 콘솔 인코딩 (Windows dev: MS949) |
| `logging.charset.file` | 파일 로그 UTF-8 |

bootRun: `logback-nsight-base.xml` — `logging.file.name` 기본 `{CATALINA_BASE:-.}/logs/nsight-app.log`

---

## 11. 환경별 설정 흐름

```text
┌─ local (bootRun) ─────────────────────────────────────────┐
│  build.gradle → spring.profiles.active=local              │
│              → nsight.txlog.path (자동)                    │
│  application-local.yml → server.port, H2 in-memory         │
└───────────────────────────────────────────────────────────┘

┌─ dev (ztomcat) ───────────────────────────────────────────┐
│  conf/setenv.* → spring.profiles.active=dev               │
│  NSIGHT_TXLOG_PATH → nsight.txlog.path                     │
│  application-dev.yml → gateway 8080, batch 수집 URL        │
└───────────────────────────────────────────────────────────┘

┌─ prod (운영 Tomcat) ──────────────────────────────────────┐
│  setenv.prod.* → spring.profiles.active=prod               │
│  NSIGHT_GATEWAY_BASE_URL, NSIGHT_*_DB_*                    │
│  application-prod.yml → Oracle·게이트웨이 오버라이드        │
└───────────────────────────────────────────────────────────┘
```

**설정 SoT:** `tcf-cicd/{local,dev,prod}/` → `sync-to-framework.ps1`로 framework yml 반영.

---

## 12. 설정 예시

### 12.1 Windows — ztomcat + 공유 H2

```bat
set JAVA_HOME=C:\Users\me\.jdks\temurin-21.0.4
set NSIGHT_TXLOG_PATH=C:\Programming(23-08-15)\nsight-tcf-framework\data\nsight-txlog
cd ztomcat
start.bat
```

### 12.2 Windows — Gradle bootRun

```powershell
$env:GRADLE_HOME_OVERRIDE = 'C:\gradle-8.10.1'
$env:NSIGHT_TXLOG_PATH = 'C:\work\nsight-tcf-framework\data\nsight-txlog'
gradle :sv-service:bootRun
```

### 12.3 Linux — 운영 Tomcat (발췌)

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export NSIGHT_GATEWAY_BASE_URL=https://marketing.example.com
export NSIGHT_OM_DB_URL=jdbc:oracle:thin:@//db:1521/nsight_om
export NSIGHT_TXLOG_PATH=/var/nsight/data/nsight-txlog
export CATALINA_OPTS="$CATALINA_OPTS -Dspring.profiles.active=prod"
```

---

## 13. `nsight.tcf.*` (application.yml — 환경변수 아님)

Spring 설정 키 — **JVM/env로 오버라이드 가능**하지만 보통 yml에 고정:

| 키 | 기본 | 설명 |
|----|------|------|
| `nsight.tcf.transaction-log-enabled` | `true` | `TCF_TX_LOG` 적재 |
| `nsight.tcf.audit-enabled` | `true` | audit.log |
| `nsight.tcf.transaction-log-datasource.separate` | `true` | 거래로그 DS 분리 |

상세: [37-transaction-log.md](../architecture/37-transaction-log.md), [31-autoconfiguration.md](../architecture/31-autoconfiguration.md)

---

## 14. 체크리스트

**local bootRun**

- [ ] `java -version` → 21
- [ ] 로그: `The following 1 profile is active: "local"`
- [ ] `data/nsight-txlog/nsight_om` 생성 (거래 후)

**dev ztomcat**

- [ ] `JAVA_HOME` JDK 21
- [ ] `NSIGHT_TXLOG_PATH` bootRun과 동일 (OM·batch DB 일치)
- [ ] 로그: `spring.profiles.active=dev`

**prod**

- [ ] `NSIGHT_GATEWAY_BASE_URL` 실제 URL
- [ ] 업무별 `NSIGHT_{CODE}_DB_*` 설정
- [ ] `NSIGHT_TXLOG_JDBC_URL` (Oracle 전환 시)
- [ ] `setenv.prod.*` → Tomcat `bin/setenv.*` 반영

---

## 15. 관련 문서·파일

| 문서/파일 | 내용 |
|-----------|------|
| [25-env-profile.md](../architecture/25-env-profile.md) | local/dev/prod 프로파일 |
| [20-env-spring.md](../architecture/20-env-spring.md) | Spring yml 구조 |
| [21-env-tomcat.md](../architecture/21-env-tomcat.md) | Tomcat setenv |
| [37-transaction-log.md](../architecture/37-transaction-log.md) | txlog 경로·DS |
| `NsightTxlogPathEnvironmentPostProcessor.java` | txlog 자동 경로 |
| `ztomcat/conf/setenv.bat` | dev Tomcat JVM |
| `tcf-cicd/prod/ztomcat/setenv.sh` | prod 샘플 |
| `scripts/gen-business-profiles.ps1` | yml 템플릿 생성 |
