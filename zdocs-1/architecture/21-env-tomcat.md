# 21. Tomcat 환경·설정 파일 아키텍처

| 항목 | 내용 |
|------|------|
| 문서 번호 | 21 |
| 제목 | Tomcat Environment & Configuration Files |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [16-deploy.md](16-deploy.md), [17-script.md](17-script.md), [20-env-spring.md](20-env-spring.md), [ztomcat/README.md](../../ztomcat/README.md) |
| 대상 | 개발·운영·인프라 담당자 |

---

## 1. 개요

NSIGHT TCF의 **통합 Tomcat 배포**는 `ztomcat/` 디렉터리로 관리한다.

| 항목 | 값 |
|------|-----|
| Tomcat | Apache Tomcat **10.1.34** (Jakarta EE 10 / Servlet 6) |
| HTTP 포트 | **8080** |
| JDK | **21 필수** (WAR 바이트코드 21) |
| WAR | **13개** (`deploy-wars.sh`: 9 업무 + om + batch + ui + jwt) |
| Context | WAR 파일명 = context path (예: `sv.war` → `/sv`) |

설정 파일은 두 계층으로 나뉜다.

| 계층 | 위치 | 역할 |
|------|------|------|
| **프로젝트 관리 템플릿** | `ztomcat/conf/`, `ztomcat/setenv.local.*` | Git에 포함, `apply-config`로 Tomcat에 반영 |
| **Tomcat 설치본** | `ztomcat/apache-tomcat-10.1.34/conf/` | `server.xml`, `logging.properties` 등 (일부 패치) |

Spring 연동: Tomcat 기동 시 `-Dspring.profiles.active=dev` → [25-env-profile.md](25-env-profile.md)의 `application-dev.yml` 활성화. 운영은 `setenv.prod.*` 샘플 참고.

---

## 2. 설정 파일 전체 맵

```text
ztomcat/
├── conf/                          ← Git 관리 (소스 오브 트루스)
│   ├── setenv.sh                  JVM·Spring 프로파일 템플릿 — dev (Linux)
│   ├── setenv.bat                 JVM·Spring 프로파일 템플릿 — dev (Windows)
│   ├── setenv.prod.sh             운영 Tomcat 샘플 (Linux)
│   └── setenv.prod.bat            운영 Tomcat 샘플 (Windows)
├── setenv.local.sh                로컬 JDK·CATALINA_OPTS (Linux, gitignore 가능)
├── setenv.local.bat               로컬 JDK (Windows)
├── apply-config.sh / .ps1         conf → Tomcat 반영 + server.xml 패치
│
└── apache-tomcat-10.1.34/         install-tomcat 후 생성
    ├── bin/
    │   ├── setenv.sh / .bat       apply-config가 conf/에서 복사
    │   ├── catalina.sh / .bat     기동·중지
    │   └── ...
    ├── conf/
    │   ├── server.xml             HTTP Connector, Host, autoDeploy
    │   ├── logging.properties     JULI 핸들러 인코딩 (UTF-8 패치)
    │   ├── catalina.properties    Tomcat 기본 (미수정)
    │   ├── web.xml                글로벌 Servlet 기본 (미수정)
    │   └── context.xml            기본 Context (미수정)
    ├── webapps/                   *.war 배포 위치
    └── logs/                      catalina, access, Spring 앱 로그
```

| 파일 | 수정 주체 | 재기동 시 유지 |
|------|-----------|----------------|
| `ztomcat/conf/setenv.*` | 개발자 (Git) | `apply-config`로 매번 복사 |
| `apache-tomcat-.../conf/server.xml` | `apply-config` (1회 패치) | install 후 유지 (재설치 시 재패치) |
| `logging.properties` | `apply-config` | 동일 |
| `webapps/*.war` | `deploy-wars` | WAR 교체 시 autoDeploy |

---

## 3. `conf/setenv.*` — JVM·Spring 진입 설정

Tomcat `bin/catalina.*` 기동 직전에 `bin/setenv.*`가 자동 실행된다.  
`apply-config`가 **`ztomcat/conf/setenv.*` → `apache-tomcat-.../bin/setenv.*`** 를 덮어쓴다.

### 3.1 공통 항목

| 설정 | Linux (`setenv.sh`) | Windows (`setenv.bat`) | 설명 |
|------|---------------------|------------------------|------|
| `JAVA_HOME` | 후보 경로 탐색 | `%USERPROFILE%\.jdks\temurin-21.0.4` | **JDK 21** |
| `JAVA_OPTS` | UTF-8 인코딩 플래그 | 동일 + sun.* encoding | JVM 전역 |
| `JAVA_TOOL_OPTIONS` | UTF-8 (미정의 시) | 동일 | 자식 프로세스 |
| `NSIGHT_TXLOG_PATH` | `CATALINA_HOME/../../data/nsight-txlog` | 동일 (for 루프) | H2 공유 경로 |
| `CATALINA_OPTS` | 아래 표 | 아래 표 | Tomcat·Spring 앱 JVM |

### 3.2 `CATALINA_OPTS` 기본값

| JVM 옵션 | 값 | 목적 |
|----------|-----|------|
| `-Xms512m -Xmx1536m` | Heap | 13 WAR 동시 기동 여유 |
| `-Duser.timezone=Asia/Seoul` | 타임존 | 로그·거래 시각 KST |
| `-Dspring.profiles.active=dev` | Spring | ztomcat 통합 검증 yml 활성화 |
| `-Dnsight.txlog.path=...` | H2 디렉터리 | bootRun과 동일 거래로그 DB |
| `-Dlogging.charset.file=UTF-8` | 로그 파일 | Spring Logback |
| `-Dlogging.charset.console` | **UTF-8** (Linux) / **MS949** (Windows) | 콘솔 인코딩 OS 차이 |

### 3.3 Linux JDK 탐색 순서 (`setenv.sh`)

1. `$HOME/.jdks/temurin-21.0.4`
2. `/usr/lib/jvm/java-21-openjdk*`
3. `/usr/lib/jvm/temurin-21-jdk*`

### 3.4 `NSIGHT_TXLOG_PATH` 계산

```text
CATALINA_HOME = ztomcat/apache-tomcat-10.1.34
프로젝트 루트   = CATALINA_HOME/../..
NSIGHT_TXLOG_PATH = {프로젝트루트}/data/nsight-txlog
```

bootRun(`gradle :tcf-om:bootRun`)과 **동일 H2 파일**을 쓰려면 이 경로가 일치해야 한다.  
수동 오버라이드: 환경변수 `NSIGHT_TXLOG_PATH` 또는 `-Dnsight.txlog.path=...`.

---

## 4. `setenv.local.*` — 로컬 오버라이드

Git에 포함된 **로컬 JVM 헬퍼**. Tomcat `bin/setenv`와는 별도이다.

| 파일 | 로드 시점 | 역할 |
|------|-----------|------|
| `setenv.local.sh` | `start.sh`, `deploy-wars.sh` | `JAVA_HOME` 해석, `CATALINA_OPTS`·`NSIGHT_TXLOG_PATH` export |
| `setenv.local.bat` | 수동/문서 참고 | Windows `JAVA_HOME` 고정 |

`setenv.local.sh` JDK 후보 (추가):

- `$HOME/.sdkman/candidates/java/current`
- `/opt/homebrew/opt/openjdk@21`

환경변수 `ZTOMCAT_REQUIRE_JAVA=0`이면 JDK 미발견 시 경고만 하고 계속한다.

---

## 5. `apply-config` — Tomcat 설정 패치

`start.sh` / `start.ps1`이 **매 기동마다** 호출한다.

### 5.1 수행 작업

| # | 작업 | 대상 파일 |
|---|------|-----------|
| 1 | `conf/setenv.*` 복사 | `bin/setenv.*` |
| 2 | Handler `.encoding = UTF-8` 추가/갱신 | `conf/logging.properties` |
| 3 | 8080 Connector UTF-8 속성 추가 (없을 때만) | `conf/server.xml` |

### 5.2 `server.xml` Connector 패치

**패치 전 (Tomcat 기본)**

```xml
<Connector port="8080" protocol="HTTP/1.1"
           connectionTimeout="20000" ... />
```

**패치 후 (`apply-config` 적용)**

```xml
<Connector port="8080" protocol="HTTP/1.1"
           URIEncoding="UTF-8"
           useBodyEncodingForURI="true"
           connectionTimeout="20000" ... />
```

| 속성 | 의미 |
|------|------|
| `URIEncoding="UTF-8"` | URL path/query 디코딩 |
| `useBodyEncodingForURI="true"` | POST body charset을 URI에도 반영 |

한글 전문·파일명 깨짐 방지용. SSL(8443)·AJP(8009) Connector는 **비활성**(주석) 상태 유지.

### 5.3 `logging.properties` 패치

다음 Handler에 `.encoding = UTF-8` 보장:

- `1catalina.org.apache.juli.AsyncFileHandler`
- `2localhost.org.apache.juli.AsyncFileHandler`
- `3manager` / `4host-manager`
- `java.util.logging.ConsoleHandler`

Tomcat 자체 로그(`catalina.*.log`) 한글 처리.

### 5.4 `server.xml` Host (기본, 미수정)

```xml
<Host name="localhost" appBase="webapps"
      unpackWARs="true" autoDeploy="true">
```

| 속성 | NSIGHT TCF에서의 의미 |
|------|----------------------|
| `appBase="webapps"` | `deploy-wars`가 WAR를 복사하는 위치 |
| `unpackWARs="true"` | WAR → exploded 디렉터리 풀기 |
| `autoDeploy="true"` | WAR 교체 시 context 자동 재배포 (~15초) |

별도 `<Context>` XML은 사용하지 않는다. **WAR 파일명 = context path**.

---

## 6. WAR·Context 매핑 (`deploy-wars`)

Context path는 Tomcat 기본 규칙: `webapps/{name}.war` → `/{name}`.

| 코드 | Gradle 모듈 | 빌드 산출물 | webapps 파일명 | Context |
|------|-------------|-------------|----------------|---------|
| cc~mg | `*-service` | `{code}.war` | `{code}.war` | `/{code}` |
| om | `tcf-om` | `tcf-om.war` | **`om.war`** | `/om` |
| batch | `tcf-batch` | `tcf-batch.war` | **`batch.war`** | `/batch` |
| ui | `tcf-ui` | `tcf-ui.war` | **`ui.war`** | `/ui` |

`deploy-wars` 단건 배포 시:

1. `webapps/{code}/` exploded 디렉터리 삭제
2. 새 WAR 복사
3. Tomcat 실행 중이면 autoDeploy로 재기동 (전체 restart 불필요)

매핑 정의: `ztomcat/deploy-wars.sh`의 `ALL_MODULES` 배열  
형식: `gradle모듈:빌드war:배포war:context코드`

---

## 7. Spring Boot WAR와 Tomcat 설정 연동

### 7.1 프로파일 체인

```text
setenv (dev): -Dspring.profiles.active=dev
    │
    ├─ application.yml (각 WAR)
    ├─ tcf-web/application-dev.yml (jar, 모든 TCF WAR)
    ├─ tcf-om/application-dev.yml
    ├─ tcf-batch/application-dev.yml
    └─ tcf-ui/application-dev.yml

setenv (prod): -Dspring.profiles.active=prod  (+ group: dev)
    │
    ├─ application-dev.yml (그룹 상속)
    └─ application-prod.yml (게이트웨이·DB 오버라이드)
```

### 7.2 `tcf-web/application-dev.yml` (공통)

```yaml
logging:
  file:
    name: ${CATALINA_BASE:./logs}/logs/nsight-${spring.application.name}.log
  logback:
    rollingpolicy:
      max-file-size: 50MB
      max-history: 14
```

| 변수 | Tomcat에서의 값 |
|------|-----------------|
| `CATALINA_BASE` | `ztomcat/apache-tomcat-10.1.34` |
| 로그 예 | `logs/nsight-nsight-sv-service.log` |

### 7.3 모듈별 dev/prod yml 요약

| 모듈 | `application-dev.yml` 핵심 | `application-prod.yml` 추가 |
|------|------------------------------|------------------------------|
| `tcf-om` | `nsight.gateway.base-url: http://127.0.0.1:8080`, `batch-service-url: .../batch` | Oracle DS, `${NSIGHT_GATEWAY_BASE_URL}` |
| `tcf-batch` | `nsight.gateway.mode: dev`, **13 WAR** Actuator 수집 타겟 | `${NSIGHT_GATEWAY_BASE_URL}`, Oracle DS |
| `tcf-ui` | `nsight.tcf-ui.deployment-mode: tomcat` | `tomcat-gateway-url: ${NSIGHT_GATEWAY_BASE_URL}` |

업무 WAR는 `application-local/dev/prod.yml`만 사용. Tomcat 로깅은 `tcf-web` `application-dev.yml` 상속.

### 7.4 WAR 부트스트랩

TCF WAR는 `NsightWarBootstrap` (`SpringBootServletInitializer`)로 외부 Tomcat에 탑재된다.  
내장 Tomcat은 `providedRuntime`으로 WAR에서 제외.

---

## 8. 로그 파일

### 8.1 Tomcat (JULI)

| 파일 | 경로 | 내용 |
|------|------|------|
| `catalina.{date}.log` | `apache-tomcat-.../logs/` | Tomcat 기동·배포 |
| `localhost.{date}.log` | 동일 | context별 배포 이벤트 |
| `localhost_access_log.{date}.txt` | 동일 | HTTP Access (Valve) |

### 8.2 Spring Boot (Logback)

| 파일 | 패턴 | 예 |
|------|------|-----|
| 앱별 rolling log | `nsight-{spring.application.name}.log` | `nsight-nsight-tcf-om.log` |
| 위치 | `${CATALINA_BASE}/logs/` | |

기동 성공 확인: `Started Nsight*Application` 문자열.

### 8.3 `ztomcat/logs/`

일부 스크립트·운영 편의용 복사 로그가 있을 수 있으나, **공식 Tomcat 로그 루트**는 `apache-tomcat-10.1.34/logs/`이다.

---

## 9. 환경변수·스크립트 플래그

| 이름 | 기본 | 용도 |
|------|------|------|
| `JAVA_HOME` | JDK 21 경로 | Tomcat Java 런타임 |
| `CATALINA_HOME` / `CATALINA_BASE` | `apache-tomcat-10.1.34` | Tomcat 루트 (start 스크립트 설정) |
| `NSIGHT_TXLOG_PATH` | `{루트}/data/nsight-txlog` | H2 file DB 디렉터리 |
| `NSIGHT_GATEWAY_BASE_URL` | (prod) `https://...` | 공개 API·Relay 게이트웨이 |
| `ZTOMCAT_SKIP_DEPLOY` | `0` | `1`이면 `start` 시 batch/ui WAR 배포 생략 |
| `ZTOMCAT_REQUIRE_JAVA` | `1` | `setenv.local.sh` JDK 필수 여부 |
| `GRADLE` | PATH의 `gradle` | `deploy-wars` 빌드 명령 |

---

## 10. 기동·설정 적용 흐름

```text
install-tomcat (최초 1회)
  → apache-tomcat-10.1.34/ 압축 해제

start.sh / start.ps1
  ├─ setenv.local.* (Linux: JDK·CATALINA_OPTS)
  ├─ apply-config
  │    ├─ conf/setenv.* → bin/setenv.*
  │    ├─ logging.properties UTF-8
  │    └─ server.xml Connector UTF-8
  ├─ deploy-wars batch ui (ZTOMCAT_SKIP_DEPLOY≠1)
  └─ catalina start
       └─ bin/setenv.* → CATALINA_OPTS 적용
            └─ 각 WAR Spring Boot context 기동 (dev 또는 prod)
```

`deploy-restart`: stop → deploy-wars all → start → health 폴링 → verify-deploy.

---

## 11. 수정 가이드 (무엇을 어디서 바꿀지)

| 변경 목적 | 수정 위치 | 비고 |
|-----------|-----------|------|
| Heap 크기 | `ztomcat/conf/setenv.*` | `apply-config` 후 재기동 |
| Spring 프로파일 | `conf/setenv.*` / `setenv.prod.*` | dev(기본) / prod(운영) |
| H2 경로 | `NSIGHT_TXLOG_PATH` 또는 setenv | bootRun과 동기화 |
| HTTP 포트 | `apache-tomcat-.../conf/server.xml` | 기본 8080, 수동 편집 |
| Context path | WAR 파일명 (`deploy-wars` 매핑) | `server.xml` Context 불필요 |
| 게이트웨이·DB (prod) | `NSIGHT_GATEWAY_BASE_URL`, `NSIGHT_*_DB_URL` | `setenv.prod.*` 참고 |
| 배치 수집 타겟 | `tcf-batch/.../application-dev.yml` | Spring 쪽 |
| UI Relay 게이트웨이 | `tcf-ui/.../application-prod.yml` | Spring 쪽 |
| 로컬 JDK만 변경 | `setenv.local.sh` | Git 개인 설정 |

**주의**: `apache-tomcat-.../bin/setenv.*`를 직접 수정하지 말 것 — 다음 `apply-config`에 덮어쓴다.  
**주의**: `server.xml` UTF-8 패치는 속성이 없을 때만 적용 — 수동 편집 후에도 `URIEncoding` 유지 확인.

---

## 12. bootRun vs ztomcat 설정 대응

| 항목 | bootRun | ztomcat |
|------|---------|---------|
| HTTP | 모듈별 포트 (8081~8099) | 단일 **8080** + context |
| Spring profile | **`local`** | **`dev`** (ztomcat) / **`prod`** (운영) |
| `nsight.txlog.path` | Gradle `bootRun` systemProperty | `setenv` `CATALINA_OPTS` |
| 로그 | 콘솔 위주 | `CATALINA_BASE/logs/nsight-*.log` |
| `tcf-ui` Relay | `deployment-mode: bootrun` | `deployment-mode: tomcat` |
| `tcf-batch` context | `/batch` (local profile) | `/batch` (WAR명) |

상세: [16-deploy.md](16-deploy.md), [20-env-spring.md §3](20-env-spring.md).

---

## 13. 트러블슈팅 (설정 관점)

| 증상 | 설정 원인 | 확인·조치 |
|------|-----------|-----------|
| `/sv/online` 404, Spring 미기동 | JDK ≠ 21 | `java -version`, `setenv` `JAVA_HOME` |
| 한글 URL/전문 깨짐 | Connector encoding | `server.xml` `URIEncoding`, `apply-config` 재실행 |
| Tomcat 로그 한글 깨짐 | `logging.properties` | Handler `.encoding=UTF-8` |
| Windows 콘솔 한글 | `logging.charset.console=MS949` | 의도된 OS 차이 ([20-env-spring.md](20-env-spring.md)) |
| bootRun·Tomcat DB 불일치 | `nsight.txlog.path` | `NSIGHT_TXLOG_PATH` 동일 경로 |
| 구버전 WAR 동작 | exploded 캐시 | `deploy-wars`가 `{code}/` 삭제 후 복사 |
| `apply-config` 실패 | Tomcat 미설치 | `install-tomcat` 선행 |

---

## 14. 체크리스트

**최초 환경**

- [ ] JDK 21 설치 (`temurin-21` 등)
- [ ] `ztomcat/install-tomcat` 실행
- [ ] `deploy-wars all` + `start` + `verify-deploy` 19/19

**설정 변경 후**

- [ ] `ztomcat/conf/setenv.*` 수정 → `start` (apply-config 자동)
- [ ] `server.xml` 포트 변경 시 방화벽·8080 충돌 확인
- [ ] `NSIGHT_TXLOG_PATH` 변경 시 기존 H2 파일 경로 이전 여부 확인

**Spring 연동**

- [ ] 기동 로그에 `The following 1 profile is active: "dev"` 확인
- [ ] `http://localhost:8080/ui/om/admin/login.html` (UI tomcat 모드)
- [ ] `http://localhost:8080/om/actuator/health`

---

## 15. 참고 파일

| # | 경로 | 내용 |
|---|------|------|
| 1 | `ztomcat/conf/setenv.sh` | Linux JVM 템플릿 (dev) |
| 2 | `ztomcat/conf/setenv.bat` | Windows JVM 템플릿 (dev) |
| 3 | `ztomcat/conf/setenv.prod.sh` | Linux 운영 샘플 (prod) |
| 4 | `ztomcat/conf/setenv.prod.bat` | Windows 운영 샘플 (prod) |
| 5 | `ztomcat/setenv.local.sh` | 로컬 JDK·txlog |
| 6 | `ztomcat/apply-config.sh` | server.xml·logging 패치 |
| 7 | `ztomcat/deploy-wars.sh` | WAR·context 매핑 |
| 8 | `ztomcat/start.sh` | 기동 오케스트레이션 |
| 9 | `tcf-web/src/main/resources/application-dev.yml` | Spring 파일 로깅 |
| 10 | `ztomcat/README.md` | 운영 스크립트 전체 가이드 |
