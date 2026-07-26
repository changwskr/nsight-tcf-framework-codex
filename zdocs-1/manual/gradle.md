# Gradle 명령어 사용법

| 항목 | 내용 |
|------|------|
| 대상 | NSIGHT TCF Framework 로컬 개발·빌드 |
| 실행 위치 | **프로젝트 루트** (`settings.gradle` 있는 디렉터리) |
| 관련 | [22-build-project.md](../architecture/22-build-project.md), [38-script.md](../architecture/38-script.md), [tcf-scripts/README.md](../../tcf-scripts/README.md) |

---

## 1. 사전 준비

| 항목 | 요구 |
|------|------|
| JDK | **21** (`JAVA_HOME`) |
| Gradle | **8.x** (예: 8.10.1) |
| Wrapper | **없음** — 시스템 `gradle` 또는 `GRADLE_HOME` 사용 |

```powershell
# 확인
java -version
gradle -version
```

### Windows — 경로에 괄호가 있을 때

프로젝트 경로가 `C:\Programming(23-08-15)\...` 처럼 괄호를 포함하면 PATH의 `gradle` 대신 명시 경로를 권장합니다.

```powershell
$env:GRADLE_HOME_OVERRIDE = 'C:\Programming(23-08-15)\gradle-8.10.1'
& "$env:GRADLE_HOME_OVERRIDE\bin\gradle.bat" :sv-service:bootRun
```

`tcf-scripts`·`deploy-wars`도 `GRADLE_HOME` / `GRADLE_HOME_OVERRIDE`를 읽습니다.

---

## 2. 명령 형식

Gradle 멀티 프로젝트에서는 **`:모듈명:태스크`** 형식을 사용합니다.

```bash
gradle :sv-service:bootRun
gradle :tcf-core:build
gradle buildZtomcatWars          # 루트 집계 태스크 (콜론 없음)
```

| 표기 | 의미 |
|------|------|
| `:sv-service` | `settings.gradle`의 `include 'sv-service'` |
| `bootRun` | Spring Boot 내장 Tomcat 실행 |
| `bootWar` | 실행 가능 WAR 생성 |
| `build` | compile + test + jar/war |

---

## 3. 자주 쓰는 태스크

| 태스크 | 설명 | 예 |
|--------|------|-----|
| `build` | 컴파일 + 테스트 + JAR/WAR | `gradle :tcf-core:build` |
| `clean` | `build/` 삭제 | `gradle clean` |
| `test` | JUnit 테스트 | `gradle :sv-service:test` |
| `bootRun` | 개발 서버 기동 (포트 분리) | `gradle :sv-service:bootRun` |
| `bootWar` | WAR 산출 | `gradle :sv-service:bootWar` |
| `bootJar` | JAR 산출 | `gradle :tcf-ui:bootJar` |
| `buildBusinessWars` | 업무 WAR **17개** 일괄 | `gradle buildBusinessWars` |
| `buildZtomcatWars` | Tomcat WAR **19개** 일괄 | `gradle buildZtomcatWars` |
| `--stop` | Gradle Daemon 종료 | `gradle --stop` |

### 산출물 위치

```text
{모듈}/build/libs/
  sv.war
  tcf-om.war
  tcf-core-1.0.0-SNAPSHOT.jar
```

WAR 파일명은 `bootWar { archiveFileName = 'sv.war' }`로 고정됩니다.  
물리 배치·ztomcat 경로: [artifacts.md](artifacts.md)  
lib 모듈·Gradle 캐시→WAR→Tomcat: [lib-module.md](lib-module.md)

---

## 4. 모듈·포트·산출물

### 4.1 프레임워크 라이브러리 (JAR)

| 모듈 | bootRun | 주 Gradle 명령 |
|------|---------|----------------|
| `tcf-util` | ✕ | `gradle :tcf-util:build` |
| `tcf-core` | ✕ | `gradle :tcf-core:build` |
| `tcf-web` | ✕ | `gradle :tcf-web:build` |
| `tcf-cache` | ✕ | `gradle :tcf-cache:build` |

프레임워크만 한 번에:

```bash
gradle :tcf-util:build :tcf-core:build :tcf-web:build :tcf-cache:build
```

### 4.2 업무·플랫폼 (WAR / bootRun)

| 모듈 | bootRun 포트 | WAR 파일 | ztomcat context |
|------|-------------|----------|-----------------|
| `cc-service` | 8081 | `cc.war` | `/cc` |
| `ic-service` | 8082 | `ic.war` | `/ic` |
| `pc-service` | 8083 | `pc.war` | `/pc` |
| `bc-service` | 8084 | `bc.war` | `/bc` |
| `ms-service` | 8085 | `ms.war` | `/ms` |
| `sv-service` | 8086 | `sv.war` | `/sv` |
| `pd-service` | 8087 | `pd.war` | `/pd` |
| `cm-service` | 8088 | `cm.war` | `/cm` |
| `eb-service` | 8089 | `eb.war` | `/eb` |
| `ep-service` | 8090 | `ep.war` | `/ep` |
| `bp-service` | 8091 | `bp.war` | `/bp` |
| `bd-service` | 8092 | `bd.war` | `/bd` |
| `ss-service` | 8093 | `ss.war` | `/ss` |
| `cs-service` | 8094 | `cs.war` | `/cs` |
| `ct-service` | 8095 | `ct.war` | `/ct` |
| `mg-service` | 8096 | `mg.war` | `/mg` |
| `tcf-om` | 8097 | `tcf-om.war` → Tomcat `om.war` | `/om` |
| `tcf-batch` | 8098 | `tcf-batch.war` | `/batch` |
| `tcf-ui` | 8099 | `tcf-ui.war` | `/ui` |

> `om-service`는 레거시 — `buildBusinessWars`·ztomcat 배포에 **포함되지 않음**. OM은 **`tcf-om`** 사용.

---

## 5. 실행 (bootRun)

### 5.1 단일 업무

```bash
gradle :sv-service:bootRun
```

- URL: `http://localhost:8086/sv/online` (POST JSON)
- 중지: `Ctrl+C`

### 5.2 OM · 배치 · UI

```bash
gradle :tcf-om:bootRun      # 8097
gradle :tcf-batch:bootRun   # 8098
gradle :tcf-ui:bootRun      # 8099
```

OM Admin (bootRun): `http://localhost:8099/om/admin/login.html` — UI와 batch 함께 기동 필요.

### 5.3 bootRun 공통 설정 (루트 자동)

루트 `build.gradle`이 모든 `bootRun`에 적용합니다.

| 항목 | 값 |
|------|-----|
| `workingDir` | 프로젝트 **루트** |
| `spring.profiles.active` | `local` |
| `nsight.txlog.path` | `{루트}/data/nsight-txlog` |
| 인코딩 | UTF-8 |

→ 어느 모듈에서 실행해도 거래로그 H2 경로가 동일합니다.

### 5.4 스크립트 래퍼 (동일 Gradle 호출)

```bat
tcf-scripts\run-local.bat sv
tcf-scripts\run-local.bat tcf-om batch ui
tcf-scripts\run-local.bat all
```

상세: [38-script.md](../architecture/38-script.md) §3.1

---

## 6. 빌드

### 6.1 단일 모듈

```bash
# JAR (라이브러리)
gradle :tcf-core:build

# WAR (업무)
gradle :sv-service:bootWar

# 테스트 포함 전체 build
gradle :sv-service:build
```

### 6.2 일괄 WAR

| 명령 | 포함 | WAR 수 |
|------|------|--------|
| `gradle buildBusinessWars` | 16 *-service + `tcf-om` | **17** |
| `gradle buildZtomcatWars` | 위 + `tcf-batch` + `tcf-ui` | **19** |

```bash
gradle buildZtomcatWars
# → ztomcat/deploy-wars 로 webapps 복사
```

### 6.3 clean 후 빌드

```bash
gradle clean buildBusinessWars
gradle clean :sv-service:build
```

### 6.4 테스트 생략 (빠른 컴파일)

```bash
gradle build -x test
gradle :sv-service:bootWar -x test
```

---

## 7. 테스트

```bash
# 전 모듈
gradle test

# 단일 모듈
gradle :tcf-core:test
gradle :sv-service:test

# 특정 테스트 클래스
gradle :sv-service:test --tests "com.nh.nsight.marketing.sv.SvTransactionLogIntegrationTest"
```

---

## 8. 진단·유틸리티

```bash
# 사용 가능한 태스크 목록
gradle tasks
gradle :sv-service:tasks

# 의존성 트리
gradle :sv-service:dependencies

# Daemon 정리 (빌드 스크립트가 선행 호출)
gradle --stop

# 상세 로그
gradle :sv-service:bootRun --info
gradle buildZtomcatWars --stacktrace
```

---

## 9. 시나리오별 명령 모음

### 9.1 SV 업무 개발 (bootRun)

```bash
gradle :tcf-core:build
gradle :sv-service:bootRun
```

프레임워크 수정 후:

```bash
gradle :tcf-core:build :tcf-web:build
# bootRun 재기동
```

### 9.2 ztomcat 배포용 WAR 전체

```bash
gradle --stop
gradle buildZtomcatWars
cd ztomcat
deploy-wars.bat all
```

또는 `tcf-scripts\build.bat wars` + `deploy.bat all`

### 9.3 OM 대시보드 (bootRun)

```bash
gradle :tcf-om:bootRun
gradle :tcf-batch:bootRun
gradle :tcf-ui:bootRun
```

### 9.4 CI / local 일괄 빌드

```powershell
tcf-cicd\local\script\build-all.ps1
tcf-cicd\local\script\build-all.ps1 -Target wars
tcf-cicd\local\script\build-all.ps1 -Target fast -SkipSync
```

---

## 10. 프로파일·시스템 속성 (수동 지정)

기본은 `bootRun` → `local`. 수동 오버라이드 예:

```bash
gradle :sv-service:bootRun -Dspring.profiles.active=dev
gradle :sv-service:bootRun -Dnsight.txlog.path=C:/data/nsight-txlog
```

Windows PowerShell:

```powershell
gradle :sv-service:bootRun "-Dspring.profiles.active=dev"
```

---

## 11. 트러블슈팅

| 증상 | 원인 | 해결 |
|------|------|------|
| `gradle not found` | PATH / GRADLE_HOME | `GRADLE_HOME_OVERRIDE` 설정 |
| `Could not resolve project :tcf-core` | 루트가 아닌 cwd | `settings.gradle` 있는 디렉터리에서 실행 |
| bootRun 포트 충돌 | 동일 모듈 2회 기동 | 기존 프로세스 종료 또는 `server.port` 변경 |
| WAR 빌드 후 Tomcat 404 | JDK 21 아님 | Tomcat·bootRun 모두 JDK 21 |
| 거래로그 DB 불일치 | txlog 경로 다름 | `data/nsight-txlog` 또는 `NSIGHT_TXLOG_PATH` |
| `buildBusinessWars` 느림 | 17모듈 순차 | `gradle --stop` 후 재시도, `-x test` |
| 괄호 경로 Gradle 실패 | Windows shell | `gradle.bat` 전체 경로 + `GRADLE_HOME_OVERRIDE` |

---

## 12. Gradle vs 스크립트

| 목적 | Gradle 직접 | 스크립트 |
|------|-------------|----------|
| bootRun | `gradle :sv-service:bootRun` | `tcf-scripts\run-local.bat sv` |
| WAR 17 | `gradle buildBusinessWars` | `tcf-scripts\build.bat wars` |
| WAR 19 | `gradle buildZtomcatWars` | `build-all.ps1 -Target wars` |
| Tomcat 배포 | (빌드 후 수동 복사) | `ztomcat\deploy-wars.bat sv` |

스크립트는 내부적으로 위 Gradle 태스크를 호출합니다.  
상세: [38-script.md](../architecture/38-script.md)

---

## 13. 참고

- 빌드 아키텍처: [22-build-project.md](../architecture/22-build-project.md)
- Spring Boot 기동: [30-springboot.md](../architecture/30-springboot.md)
- 모듈 레퍼런스: [28-tcf-framework-ref.md](../architecture/28-tcf-framework-ref.md)
