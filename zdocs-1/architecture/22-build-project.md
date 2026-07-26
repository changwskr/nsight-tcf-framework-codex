# 22. Gradle 빌드·실행 아키텍처

| 항목 | 내용 |
|------|------|
| 문서 번호 | 22 |
| 제목 | Build & Run Project Architecture |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [16-deploy.md](16-deploy.md), [17-script.md](17-script.md), [20-env-spring.md](20-env-spring.md), [21-env-tomcat.md](21-env-tomcat.md), [38-script.md](38-script.md), [../manual/gradle.md](../manual/gradle.md), [../manual/artifacts.md](../manual/artifacts.md), [../manual/lib-module.md](../manual/lib-module.md), [README.md](../../README.md) |
| 대상 | 프레임워크·업무·빌드 담당자 |

---

## 1. 개요

`nsight-tcf-framework`는 **Gradle 멀티 프로젝트**로 구성된다.  
루트 `settings.gradle`에 모듈이 선언되고, `build.gradle`에서 공통 설정·집계 태스크를 정의한다.

| 항목 | 값 |
|------|-----|
| Gradle | **8.x** 권장 (스크립트 예: 8.10.1) |
| Spring Boot | **3.3.5** (BOM) |
| Java | **21** (toolchain) |
| Group / Version | `com.nh.nsight` / `1.0.0-SNAPSHOT` |
| 루트 프로젝트명 | `nsight-tcf-framework-tcfmodules` |
| Wrapper | 없음 — 시스템 `gradle` 또는 `GRADLE_HOME` 사용 |

산출물 유형:

| 유형 | 모듈 | Gradle 태스크 | 산출물 |
|------|------|---------------|--------|
| **JAR (라이브러리)** | `tcf-util`, `tcf-core`, `tcf-web`, `tcf-cache` | `build` | `build/libs/*.jar` |
| **WAR (업무·플랫폼)** | `*-service`, `tcf-om`, `tcf-batch`, `tcf-ui` | `bootWar` | `build/libs/*.war` |
| **실행 JAR** | `tcf-batch`, `tcf-ui` | `bootJar` | `build/libs/*.jar` (보조) |

---

## 2. 모듈 전체 구조

```text
nsight-tcf-framework/
├── build.gradle              루트 공통·집계 태스크
├── settings.gradle           include 24 서브프로젝트
│
├── [프레임워크 라이브러리]
│   ├── tcf-util              유틸 (Spring 없음)
│   ├── tcf-core              TCF 엔진
│   ├── tcf-web               HTTP·DS·MyBatis·AutoConfig
│   └── tcf-cache             EhCache
│
├── [실행 모듈 — Spring Boot WAR/JAR]
│   ├── tcf-om, tcf-batch, tcf-ui, tcf-uj, tcf-gateway, tcf-jwt
│   ├── ic-service … mg-service   업무 9개
│   └── om-service            레거시 OM (미배포)
│
├── tcf-scripts/              빌드·run 래퍼
└── ztomcat/                  Tomcat WAR 배포
```

### 2.1 서브프로젝트 목록 (`settings.gradle` 기준)

| # | Gradle 경로 | 유형 | bootRun 포트 | ztomcat deploy |
|---|-------------|------|-------------|----------------|
| 1–4 | `tcf-util`, `tcf-core`, `tcf-web`, `tcf-cache` | lib | — | — |
| 5 | `tcf-eai` | lib | — | — |
| 6–11 | `ic` … `mg` *-service (9개) | WAR | 8082~8096 | ● |
| 12 | `tcf-om` | WAR | 8097 | ● |
| 13 | `tcf-batch` | WAR | 8098 | ● |
| 14 | `tcf-ui` | WAR/JAR | 8099 | ● |
| 15 | `tcf-gateway` | WAR | 8100 | bootRun only |
| 16 | `tcf-uj` | WAR/JAR | 8102 | bootRun only |
| 17 | `tcf-jwt` | WAR | 8110 | ● |
| 18 | `om-service` | WAR | — | ✕ 레거시 |

> cc~ct 등 **목표 17업무** 중 미구현 모듈은 설계 문서에만 존재. 상세 포트: [docs/manual/README.md](../manual/README.md).

---

## 3. 의존 관계

```text
tcf-util
   └── tcf-core
          └── tcf-web ─────────────────────────┐
                  └── tcf-cache (선택)         │
                         └── tcf-om            │
                                               ├── *-service (×9)
tcf-core ── tcf-web ── tcf-batch               │
tcf-ui / tcf-uj / tcf-gateway / tcf-jwt       │
om-service (레거시, tcf-web)                   │
```

| 모듈 | implementation / api |
|------|----------------------|
| 업무 WAR | `tcf-util`, `tcf-core`, `tcf-web` + Spring starters |
| `tcf-om` | 위 + `tcf-cache`, `spring-session-jdbc` |
| `tcf-batch` | `tcf-core`, `tcf-web` (MyBatis 없음) |
| `tcf-ui` | `spring-boot-starter-web`, `actuator` only |

외부 Tomcat 배포: 모든 WAR에 `providedRuntime spring-boot-starter-tomcat`.

---

## 4. 루트 `build.gradle` 핵심

### 4.1 전 서브프로젝트 공통

| 설정 | 값 |
|------|-----|
| Plugin | `java-library`, `io.spring.dependency-management` |
| Java toolchain | 21 |
| BOM | `spring-boot-dependencies:3.3.5` |
| Compile | UTF-8, `-parameters` |
| Test | JUnit Platform (`spring-boot-starter-test`) |

### 4.2 `bootRun` 공통 (Spring Boot 모듈)

루트에서 모든 `bootRun`에 적용:

| 항목 | 값 |
|------|-----|
| `workingDir` | **프로젝트 루트** (`settings.gradle` 위치) |
| `nsight.txlog.path` | `{루트}/data/nsight-txlog` |
| 인코딩 | `file.encoding=UTF-8` 등 |

→ 모듈 디렉터리에서 실행해도 H2 거래로그 경로가 통일된다.

### 4.3 집계 빌드 태스크

| 태스크 | 명령 | 포함 모듈 | WAR 수 |
|--------|------|-----------|--------|
| **`buildBusinessWars`** | `gradle buildBusinessWars` | `businessModules` 17개 | **17** |
| **`buildZtomcatWars`** | `gradle buildZtomcatWars` | 위 + `tcf-batch` + `tcf-ui` | **19** |

`businessModules` (루트 `ext`):

```text
cc, ic, pc, bc, ms, sv, pd, cm, eb, ep, bp, bd, ss, cs, ct, mg, tcf-om
```

`om-service`는 **포함되지 않음** (레거시).

### 4.4 `pc-service` 루트 상속

`pc-service`만 루트 `configure(sharedBusinessModules)` 블록으로 플러그인·의존성을 일부 상속한다.  
나머지 15개 업무 모듈은 각자 `build.gradle`에 동일 패턴을 명시한다.

---

## 5. 모듈별 `build.gradle` 패턴

### 5.1 업무 WAR (표준, `sv-service` 예)

```gradle
plugins {
    id 'org.springframework.boot'
    id 'war'
}

dependencies {
    implementation project(':tcf-util')
    implementation project(':tcf-core')
    implementation project(':tcf-web')
    // web, validation, actuator, jdbc, aop
    // spring-session-jdbc, mybatis, h2
    providedRuntime 'org.springframework.boot:spring-boot-starter-tomcat'
}

bootWar { archiveFileName = 'sv.war' }
war     { archiveFileName = 'sv.war' }
```

### 5.2 `tcf-om`

- 추가: `tcf-cache`, `spring-session-jdbc`, `spring-security-crypto`
- `bootWar` → `tcf-om.war` (ztomcat에서 `om.war`로 rename 복사)

### 5.3 `tcf-batch`

```gradle
bootRun {
    systemProperty 'spring.profiles.active', 'local'
}
```

- bootRun 시 `/batch` context·소수 수집 타겟 ([25-env-profile.md](25-env-profile.md))

### 5.4 `tcf-ui`

- TCF JAR 미의존
- `bootJar` / `bootWar` → `tcf-ui.jar` / `tcf-ui.war`

---

## 6. Gradle 태스크 레퍼런스

프로젝트 루트에서 **`:모듈:태스크`** 형식으로 실행한다.

### 6.1 공통 태스크

| 태스크 | 설명 | 예 |
|--------|------|-----|
| `build` | compile + test + jar/war | `gradle :tcf-core:build` |
| `clean` | `build/` 삭제 | `gradle clean` (전체) |
| `test` | 단위·통합 테스트 | `gradle :sv-service:test` |
| `bootRun` | 내장 Tomcat 개발 실행 | `gradle :sv-service:bootRun` |
| `bootWar` | 실행 가능 WAR | `gradle :sv-service:bootWar` |
| `bootJar` | 실행 가능 JAR | `gradle :tcf-ui:bootJar` |

### 6.2 산출물 경로

```text
{module}/build/libs/
  tcf-core-1.0.0-SNAPSHOT.jar
  sv.war
  tcf-om.war
  tcf-batch.war
  tcf-ui.war
```

WAR 파일명은 `bootWar { archiveFileName = '...' }`로 고정한다 (Maven 좌표명과 무관).

### 6.3 자주 쓰는 Gradle 명령

```bash
# 프레임워크만
gradle :tcf-util:build :tcf-core:build :tcf-web:build :tcf-cache:build

# 단일 업무
gradle :sv-service:bootRun
gradle :sv-service:bootWar

# OM 스택
gradle :tcf-om:bootRun
gradle :tcf-batch:bootRun
gradle :tcf-ui:bootRun

# WAR 일괄
gradle buildBusinessWars      # 17
gradle buildZtomcatWars       # 19

# 테스트
gradle test                   # 전 모듈
gradle :tcf-core:test

# Daemon 정리 (스크립트가 선행 호출)
gradle --stop
```

---

## 7. 실행 (bootRun) 가이드

### 7.1 Gradle 직접 실행

```bash
cd nsight-tcf-framework    # 루트 (settings.gradle)

gradle :sv-service:bootRun
# → http://localhost:8086/online
```

| 모듈 | URL (bootRun) |
|------|---------------|
| `sv-service` | `http://localhost:8086/sv/online` |
| `tcf-om` | `http://localhost:8097/om/online`, `/ud/files/*` |
| `tcf-batch` | `http://localhost:8098/batch` (profile `local`) |
| `tcf-ui` | `http://localhost:8099/ui/...` |

### 7.2 `tcf-scripts/run-local` (권장 래퍼)

```bash
# Windows
tcf-scripts\run-local.bat sv
tcf-scripts\run-local.bat tcf-om batch ui
tcf-scripts\run-local.bat all

# Linux/macOS
tcf-scripts/run-local.sh sv
```

| 인자 | 동작 |
|------|------|
| **단일** (`sv`) | 현재 터미널 포그라운드 `bootRun` |
| **복수** (`sv ic`) | 모듈별 **새 창** 백그라운드 |
| **`all`** | 16 업무 + `tcf-om` 새 창 일괄 기동 |
| 별칭 | `ui`→`tcf-ui`, `om`/`ud`→`tcf-om`, `batch`→`tcf-batch` |

### 7.3 모듈별 `scripts/run-local`

```bash
sv-service/scripts/run-local.bat
tcf-om/scripts/run-local.sh
```

- `GRADLE_HOME` / `GRADLE_HOME_OVERRIDE` 지원
- 프로젝트 루트로 `cd` 후 `:모듈:bootRun` 호출

### 7.4 OM 대시보드 로컬 시나리오 (bootRun)

```bash
tcf-scripts\run-local.bat tcf-om batch ui
# 또는
gradle :tcf-om:bootRun &
gradle :tcf-batch:bootRun &
gradle :tcf-ui:bootRun
```

브라우저: `http://localhost:8099/om/admin/login.html` (`admin01` / `nsight01!`)

---

## 8. 빌드 가이드

### 8.1 `tcf-scripts/build`

```bash
tcf-scripts\build.bat all        # clean + buildBusinessWars
tcf-scripts\build.bat wars       # buildBusinessWars
tcf-scripts\build.bat tcf        # tcf-util, core, web
tcf-scripts\build.bat sv         # :sv-service:build
tcf-scripts\build.bat services   # 16 + tcf-om bootWar
tcf-scripts\build.bat ui         # tcf-ui bootJar
```

| 타겟 | Gradle 태스크 |
|------|---------------|
| `all` | `clean buildBusinessWars` |
| `wars` | `buildBusinessWars` |
| `tcf` | `:tcf-util:build :tcf-core:build :tcf-web:build` |
| `sv` 등 | `:{code}-service:build` |
| `ztomcat` | README 참고 — `gradle buildZtomcatWars` 직접 호출 |

빌드 전 `gradle --stop`으로 Daemon 정리 (스크립트 내장).

### 8.2 모듈 `scripts/build` (예: sv)

```bash
sv-service/scripts/build.bat          # tcf + sv.war
sv-service/scripts/build.bat clean
sv-service/scripts/build.bat run      # bootRun
```

의존 순서: `tcf-util` → `tcf-core` → `tcf-web` → `bootWar`.

### 8.3 WAR → Tomcat 배포

| 경로 | 설명 |
|------|------|
| `tcf-scripts/deploy.bat` | `buildBusinessWars` + `ztomcat/.../webapps` 복사 |
| `ztomcat/deploy-wars.sh` | 선택/전체 WAR 빌드·복사·context rename |

```bash
ztomcat/deploy-wars.sh all
ztomcat/deploy-wars.sh sv om ui
```

ztomcat WAR rename:

| 빌드 산출물 | webapps 배포명 |
|-------------|----------------|
| `tcf-om.war` | `om.war` |
| `tcf-batch.war` | `batch.war` |
| `tcf-ui.war` | `ui.war` |
| `sv.war` | `sv.war` |

상세: [21-env-tomcat.md](21-env-tomcat.md), [17-script.md](17-script.md).

---

## 9. bootRun vs ztomcat 빌드·실행 비교

| 단계 | bootRun | ztomcat |
|------|---------|---------|
| **빌드** | 불필요 (또는 `build`만) | `bootWar` / `buildZtomcatWars` |
| **실행** | `gradle :모듈:bootRun` | `ztomcat/start` |
| **포트** | 모듈별 8081~8099 | 통합 **8080** |
| **JDK** | toolchain 21 | Tomcat JVM 21 (`setenv`) |
| **프로파일** | **`local`** | **`dev`** (ztomcat) / **`prod`** (운영) |
| **검증** | `curl` / Actuator | `ztomcat/verify-deploy` |

동일 소스·동일 WAR 산출물 — **실행 방식만** 다르다 ([16-deploy.md](16-deploy.md)).

---

## 10. 사전 요구사항

| 항목 | 요구 |
|------|------|
| **JDK** | 21 (Temurin 등) — WAR·toolchain |
| **Gradle** | 8.x PATH 또는 `GRADLE_HOME` |
| **OS** | Windows / Linux / macOS |
| **네트워크** | 최초 빌드 시 Maven Central |
| **디스크** | `data/nsight-txlog`, `data/updownload` 쓰기 권한 |

환경 변수:

| 변수 | 용도 |
|------|------|
| `JAVA_HOME` | JDK 21 |
| `GRADLE_HOME` / `GRADLE_HOME_OVERRIDE` | Gradle 경로 (스크립트) |
| `TOMCAT_WEBAPPS` | deploy 대상 webapps 오버라이드 |
| `NSIGHT_TXLOG_PATH` | H2 file DB 루트 |

---

## 11. 개발 워크플로우 예시

### 11.1 업무 Handler 개발 (SV)

```text
1. gradle :tcf-core:build :tcf-web:build     (프레임워크 변경 시)
2. tcf-scripts\run-local.bat sv              (8086)
3. tcf-scripts\curl-sample.bat sv            (샘플 전문)
4. gradle :sv-service:test                   (테스트)
```

### 11.2 OM·UI 통합 (bootRun)

```text
1. tcf-scripts\run-local.bat tcf-om batch ui
2. http://localhost:8099/om/admin/dashboard.html
```

### 11.3 릴리즈 유사 검증 (ztomcat)

```text
1. gradle buildZtomcatWars
2. ztomcat\deploy-restart.bat
3. ztomcat\verify-deploy.ps1
4. http://localhost:8080/ui/om/admin/login.html
```

### 11.4 프레임워크만 수정

```text
1. tcf-core / tcf-web 코드 변경
2. gradle :tcf-web:build
3. 의존 모듈 bootRun 재시작 (자동 reload 없음)
```

---

## 12. 테스트

```bash
gradle :tcf-core:test
gradle :sv-service:test
gradle test                    # 전체 (시간 소요)
```

`sv-service` 등에 `SvTransactionLogIntegrationTest` — `TCF_TX_LOG` 적재 검증.

---

## 13. 트러블슈팅

| 증상 | 원인 | 조치 |
|------|------|------|
| `gradle not found` | PATH 미설정 | Gradle 8 설치, `GRADLE_HOME_OVERRIDE` |
| bootRun H2 경로 불일치 | workingDir | **루트에서** 실행 또는 `tcf-scripts` 사용 |
| `tcf-om` + `om-service` 충돌 | 동일 포트 8097 | `om-service` 기동 금지 |
| WAR 404 on Tomcat | JDK ≠ 21 | ztomcat `setenv` JDK 21 확인 |
| `buildBusinessWars` OOM | 17 WAR 동시 | `gradle --stop`, Heap 증가 또는 모듈별 `bootWar` |
| 변경 반영 안 됨 | bootRun 캐시 | 프로세스 재시작; WAR는 redeploy |
| `pc-service` 빌드 이상 | 루트 상속 + 로컬 gradle | `pc-service/build.gradle`·루트 `configure` 확인 |

---

## 14. 체크리스트

**신규 개발자**

- [ ] JDK 21, Gradle 8 설치
- [ ] 루트에서 `gradle :tcf-core:build :tcf-web:build` 성공
- [ ] `tcf-scripts\run-local.bat sv` → `8086/actuator/health` OK
- [ ] `gradle buildZtomcatWars` (선택) → ztomcat 배포

**업무 모듈 추가 시**

- [ ] `settings.gradle`에 `include`
- [ ] `build.gradle` (boot + war + archiveFileName)
- [ ] `application.yml` (port, datasource)
- [ ] 루트 `businessModules` + `ztomcat/deploy-wars` 매핑
- [ ] `tcf-ui` `BusinessModuleDefinitions` 포트

---

## 15. 참고 파일

| # | 경로 | 내용 |
|---|------|------|
| 1 | `settings.gradle` | 모듈 include |
| 2 | `build.gradle` | 공통·집계·bootRun |
| 3 | `README.md` | 빠른 시작 |
| 4 | `tcf-scripts/README.md` | 스크립트 상세 |
| 5 | `ztomcat/README.md` | Tomcat 배포 |
| 6 | `{module}/build.gradle` | 모듈별 의존·WAR명 |
| 7 | `{module}/scripts/build.bat` | 단일 모듈 빌드 |
| 8 | `docs/architecture/17-script.md` | 스크립트 아키텍처 |
