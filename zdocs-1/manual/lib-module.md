# 라이브러리 모듈·의존 JAR 참조

| 항목 | 내용 |
|------|------|
| 대상 | 프레임워크·업무 개발자, 빌드·배포 담당자 |
| 관련 | [artifacts.md](artifacts.md), [gradle.md](gradle.md), [22-build-project.md](../architecture/22-build-project.md) |

NSIGHT TCF에서 **“lib”** 이란 두 종류입니다.

| 구분 | 예 | 출처 | Maven Central |
|------|-----|------|:-------------:|
| **NSIGHT lib 모듈** | `tcf-util`, `tcf-core`, `tcf-web`, `tcf-cache` | 같은 Git repo — `implementation project(...)` | ✕ |
| **외부 lib (Maven)** | `spring-boot-3.3.5.jar`, `mybatis-3.5.14.jar`, `h2-2.2.224.jar` | Maven Central — Gradle `implementation` | ● |

실행 시 JVM classpath에는 **두 종류가 함께** 올라갑니다.  
물리적으로 **어느 폴더에 있는 JAR를 읽는지**는 bootRun / bootWar / ztomcat 단계에 따라 달라집니다.

**24 모듈 전체 물리 디렉터리·경로 일람:** §2.

---

## 1. JAR가 PC에 저장되는 경로 — 전체 흐름

### 1.1 Maven Central lib (외부 의존)

`spring-boot`, `mybatis`, `h2` 등 **서드파티 JAR**의 이동 경로입니다.

```text
Maven Central
    │  gradle build / bootRun / bootWar (최초 1회 다운로드)
    ▼
① Gradle 캐시
    C:\Users\{사용자}\.gradle\caches\modules-2\files-2.1\...
    │  bootWar 시 복사·압축
    ▼
② WAR 안 (압축)
    {프로젝트}\sv-service\build\libs\sv.war  →  WEB-INF/lib/*.jar
    │  deploy-wars + Tomcat autoDeploy (압축 해제)
    ▼
③ Tomcat exploded (풀린 폴더)
    ztomcat\apache-tomcat-10.1.34\webapps\sv\WEB-INF\lib\*.jar
```

| 단계 | bootRun | bootWar | ztomcat 기동 |
|:----:|:-------:|:-------:|:------------:|
| ① Gradle 캐시 | **● JVM이 여기서 읽음** | 빌드 입력 | (직접 사용 안 함) |
| ② WAR 내부 | — | 산출물 | deploy-wars가 복사 |
| ③ exploded `WEB-INF/lib` | — | — | **● JVM이 여기서 읽음** |

### 1.2 NSIGHT lib 모듈 (프로젝트 내부)

`tcf-*` JAR는 **Maven Central에 없습니다.** repo 안에서 빌드됩니다.

```text
소스  tcf-util/ → tcf-core/ → tcf-web/ → tcf-cache/
    │  gradle :tcf-*:build
    ▼
⓪ 프로젝트 build/  (Maven 캐시와 별도)
    tcf-core/build/libs/tcf-core-1.0.0-SNAPSHOT.jar
    tcf-core/build/classes/java/main/...
    │  bootWar 시 WEB-INF/lib/ 로 복사
    ▼
② WAR / ③ exploded 와 동일하게
    WEB-INF/lib/tcf-core-1.0.0-SNAPSHOT.jar
```

| 단계 | bootRun | bootWar / ztomcat |
|------|---------|-------------------|
| ⓪ `{모듈}/build/` | **●** classes + libs JAR | 빌드 입력 |
| ① Gradle 캐시 | ✕ (해당 없음) | ✕ |
| ②③ WAR·exploded | bootRun은 WAR 미사용 | **●** `WEB-INF/lib/tcf-*.jar` |

---

## 2. 전체 물리 디렉터리·모듈 일람 (24개)

`settings.gradle`의 **24 서브프로젝트** 전체를, PC에 실제로 생기는 경로 기준으로 정리합니다.

### 2.0 기준 경로 (Windows)

| 기호 | 실제 예 (본 문서 PC) |
|------|----------------------|
| `{PROJECT}` | `C:\Programming(23-08-15)\nsight-tcf-framework` |
| `{GRADLE_USER}` | `C:\Users\{Windows로그인}\.gradle` (상세 §2.6) |
| `{TOMCAT}` | `{PROJECT}\ztomcat\apache-tomcat-10.1.34` |

Linux/macOS: `{PROJECT}` = `settings.gradle` 있는 clone 루트, `{GRADLE_USER}` = `~/.gradle`.

---

### 2.1 프로젝트 루트 — 전체 물리 트리

```text
{PROJECT}/
│
├── settings.gradle                    ← 24 모듈 include
├── build.gradle                       ← BOM 3.3.5, buildZtomcatWars
│
├── [NSIGHT lib — JAR만, 단독 기동 ✕]
│   ├── tcf-util/
│   │   ├── src/main/java/...
│   │   └── build/
│   │       ├── classes/java/main/     ← bootRun classpath (⓪)
│   │       └── libs/
│   │           └── tcf-util-1.0.0-SNAPSHOT.jar
│   ├── tcf-core/
│   │   └── build/libs/tcf-core-1.0.0-SNAPSHOT.jar
│   ├── tcf-web/
│   │   └── build/libs/tcf-web-1.0.0-SNAPSHOT.jar
│   └── tcf-cache/
│       └── build/libs/tcf-cache-1.0.0-SNAPSHOT.jar
│
├── [실행 모듈 — WAR / bootJar]
│   ├── ic-service/ … mg-service/      ← 업무 9 (각 {code}.war)
│   │   ├── src/main/java/...
│   │   ├── src/main/resources/application-*.yml
│   │   └── build/
│   │       ├── classes/java/main/
│   │       ├── resources/main/
│   │       └── libs/
│   │           ├── {code}.war              ← ② 배포용 fat WAR
│   │           └── {module}-1.0.0-SNAPSHOT-plain.war   (배포 ✕)
│   ├── tcf-om/
│   │   └── build/libs/tcf-om.war
│   ├── tcf-batch/
│   │   └── build/libs/
│   │       ├── tcf-batch.war
│   │       └── tcf-batch.jar               ← bootJar (선택)
│   ├── tcf-ui/
│   │   └── build/libs/
│   │       ├── tcf-ui.war
│   │       └── tcf-ui.jar
│   └── om-service/                    ← 레거시 (deploy-wars ✕)
│       └── build/libs/om.war
│
├── data/
│   └── nsight-txlog/                  ← H2 거래로그 루트 (런타임 생성)
│       └── nsight_om/                 ← H2 file DB (거래·batch 후)
│
└── ztomcat/
    ├── conf/
    │   ├── setenv.bat | setenv.sh
    │   └── Catalina/localhost/
    │       └── batch.xml              ← docBase → wars/zz-batch.war
    ├── wars/
    │   └── zz-batch.war               ← ③ batch WAR (deploy-wars 복사)
    └── apache-tomcat-10.1.34/         ← install-tomcat 후 (~150MB)
        ├── bin/setenv.*               ← apply-config 복사본
        ├── lib/                       ← Tomcat·Servlet만 (Spring ✕)
        ├── logs/
        └── webapps/                   ← ③ WAR·exploded (18 context + ROOT)
            ├── cc.war | cc/
            ├── … (업무 9)
            ├── om.war | om/
            ├── ui.war | ui/
            └── (legacy zz-batch.war 있을 수 있음 — deploy-wars가 정리)
```

**PC 밖 (Gradle Maven lib — ①)**

```text
{GRADLE_USER}/
└── caches/
    └── modules-2/
        └── files-2.1/
            ├── com.h2database/h2/2.2.224/{해시}/h2-2.2.224.jar
            ├── org.springframework.boot/spring-boot/3.3.5/{해시}/spring-boot-3.3.5.jar
            ├── org.mybatis/mybatis/3.5.14/{해시}/mybatis-3.5.14.jar
            └── … (Spring·Jackson·Logback 등 수백 개 artifact)
```

---

### 2.2 24 모듈 — 물리 경로·산출물·배포 일람표

| # | Gradle 모듈 | 유형 | `build/libs/` 배포 파일 | ztomcat 물리 경로 (③) | Context | bootRun 포트 | NSIGHT lib |
|---|-------------|------|-------------------------|------------------------|---------|:------------:|:----------:|
| 1 | `tcf-util` | lib | `tcf-util-1.0.0-SNAPSHOT.jar` | — (WAR에 포함) | — | ✕ | — |
| 2 | `tcf-core` | lib | `tcf-core-1.0.0-SNAPSHOT.jar` | — | — | ✕ | util |
| 3 | `tcf-web` | lib | `tcf-web-1.0.0-SNAPSHOT.jar` | — | — | ✕ | util, core |
| 4 | `tcf-cache` | lib | `tcf-cache-1.0.0-SNAPSHOT.jar` | — | — | ✕ | core |
| 5 | `cc-service` | WAR | `cc.war` | `{TOMCAT}\webapps\cc.war` · `webapps\cc\` | `/cc` | 8081 | util, core, web |
| 6 | `ic-service` | WAR | `ic.war` | `webapps\ic.war` · `webapps\ic\` | `/ic` | 8082 | util, core, web |
| 7 | `pc-service` | WAR | `pc.war` | `webapps\pc.war` · `webapps\pc\` | `/pc` | 8083 | util, core, web |
| 8 | `bc-service` | WAR | `bc.war` | `webapps\bc.war` · `webapps\bc\` | `/bc` | 8084 | util, core, web |
| 9 | `ms-service` | WAR | `ms.war` | `webapps\ms.war` · `webapps\ms\` | `/ms` | 8085 | util, core, web |
| 10 | `sv-service` | WAR | `sv.war` | `webapps\sv.war` · `webapps\sv\` | `/sv` | 8086 | util, core, web |
| 11 | `pd-service` | WAR | `pd.war` | `webapps\pd.war` · `webapps\pd\` | `/pd` | 8087 | util, core, web |
| 12 | `cm-service` | WAR | `cm.war` | `webapps\cm.war` · `webapps\cm\` | `/cm` | 8088 | util, core, web |
| 13 | `eb-service` | WAR | `eb.war` | `webapps\eb.war` · `webapps\eb\` | `/eb` | 8089 | util, core, web |
| 14 | `ep-service` | WAR | `ep.war` | `webapps\ep.war` · `webapps\ep\` | `/ep` | 8090 | util, core, web |
| 15 | `bp-service` | WAR | `bp.war` | `webapps\bp.war` · `webapps\bp\` | `/bp` | 8091 | util, core, web |
| 16 | `bd-service` | WAR | `bd.war` | `webapps\bd.war` · `webapps\bd\` | `/bd` | 8092 | util, core, web |
| 17 | `ss-service` | WAR | `ss.war` | `webapps\ss.war` · `webapps\ss\` | `/ss` | 8093 | util, core, web |
| 18 | `cs-service` | WAR | `cs.war` | `webapps\cs.war` · `webapps\cs\` | `/cs` | 8094 | util, core, web |
| 19 | `ct-service` | WAR | `ct.war` | `webapps\ct.war` · `webapps\ct\` | `/ct` | 8095 | util, core, web |
| 20 | `mg-service` | WAR | `mg.war` | `webapps\mg.war` · `webapps\mg\` | `/mg` | 8096 | util, core, web |
| 21 | `tcf-om` | WAR | `tcf-om.war` | `webapps\om.war` · `webapps\om\` | `/om` | 8097 | core, web, **cache** |
| 22 | `tcf-batch` | WAR/JAR | `tcf-batch.war` | `{PROJECT}\ztomcat\wars\zz-batch.war` · `webapps\batch\` | `/batch` | 8098 | core, web |
| 23 | `tcf-ui` | WAR/JAR | `tcf-ui.war` | `webapps\ui.war` · `webapps\ui\` | `/ui` | 8099 | **없음** |
| 24 | `om-service` | WAR | `om.war` | **미배포** (레거시) | — | 8097 | util, core, web |

- **② 빌드 산출물** 전체 경로 예: `{PROJECT}\sv-service\build\libs\sv.war`
- **Maven lib** (`spring-boot`, `h2`, …): 모듈마다 `WEB-INF/lib/`에 **약 45~55개** JAR (업무 WAR 기준, `sv` exploded 실측 **50개**)
- `tcf-om.war` → deploy-wars가 **`om.war`로 rename** 복사
- `tcf-batch.war` → **`ztomcat/wars/zz-batch.war`** (`batch.xml`의 `docBase`)

---

### 2.3 모듈 `build/` 내부 — lib·클래스가 생기는 위치

#### NSIGHT lib 모듈 (4개)

```text
{PROJECT}/tcf-core/build/
├── classes/java/main/                 ← bootRun·compile 출력 (⓪)
│   └── com/nh/nsight/tcf/core/...
├── libs/
│   └── tcf-core-1.0.0-SNAPSHOT.jar    ← bootWar 입력 (⓪)
└── tmp/compileJava/...
```

| 모듈 | JAR (⓪) | classes (bootRun) |
|------|---------|-------------------|
| `tcf-util` | `{PROJECT}\tcf-util\build\libs\tcf-util-1.0.0-SNAPSHOT.jar` | `tcf-util\build\classes\java\main\` |
| `tcf-core` | `tcf-core\build\libs\tcf-core-1.0.0-SNAPSHOT.jar` | `tcf-core\build\classes\java\main\` |
| `tcf-web` | `tcf-web\build\libs\tcf-web-1.0.0-SNAPSHOT.jar` | `tcf-web\build\classes\java\main\` |
| `tcf-cache` | `tcf-cache\build\libs\tcf-cache-1.0.0-SNAPSHOT.jar` | `tcf-cache\build\classes\java\main\` |

#### 실행 WAR 모듈 (예: sv-service)

```text
{PROJECT}/sv-service/build/
├── classes/java/main/                 ← 업무 Java (⓪ bootRun)
├── resources/main/                    ← application-local.yml 등
└── libs/
    ├── sv.war                         ← ② fat WAR (NSIGHT + Maven lib 전부 내장)
    ├── sv-service.war                 ← (중간 산출, 배포 ✕)
    └── sv-service-1.0.0-SNAPSHOT-plain.war   ← lib 미포함 (배포 ✕)
```

`sv.war` 내부 (②):

```text
WEB-INF/
├── classes/                           ← sv-service + 리소스
└── lib/                               ← tcf-*-1.0.0-SNAPSHOT.jar + Maven lib ~50개
    ├── tcf-util-1.0.0-SNAPSHOT.jar
    ├── tcf-core-1.0.0-SNAPSHOT.jar
    ├── tcf-web-1.0.0-SNAPSHOT.jar
    ├── spring-boot-3.3.5.jar
    ├── h2-2.2.224.jar
    └── …
```

---

### 2.4 ztomcat 배포 후 — 실제 물리 트리 (③)

`deploy-wars` + Tomcat autoDeploy 이후 **본 PC 실측** 구조입니다.

```text
{PROJECT}/ztomcat/
├── wars/
│   └── zz-batch.war                   ← tcf-batch/build/libs/tcf-batch.war 복사
├── conf/Catalina/localhost/
│   └── batch.xml                      ← docBase=".../ztomcat/wars/zz-batch.war"
└── apache-tomcat-10.1.34/
    ├── lib/                           ← catalina.jar, servlet-api.jar … (Tomcat 전용)
    ├── logs/
    │   ├── catalina.{date}.log
    │   └── localhost.{date}.log
    └── webapps/
        ├── cc.war    cc/WEB-INF/lib/  ← exploded (~50 jar)
        ├── ic.war    ic/
        ├── pc.war    pc/
        ├── bc.war    bc/
        ├── ms.war    ms/
        ├── sv.war    sv/WEB-INF/lib/  ← 예: h2-2.2.224.jar, spring-boot-3.3.5.jar
        ├── pd.war    pd/
        ├── cm.war    cm/
        ├── eb.war    eb/
        ├── ep.war    ep/
        ├── bp.war    bp/
        ├── bd.war    bd/
        ├── ss.war    ss/
        ├── cs.war    cs/
        ├── ct.war    ct/
        ├── mg.war    mg/
        ├── om.war    om/WEB-INF/lib/  ← + tcf-cache-1.0.0-SNAPSHOT.jar
        ├── ui.war    ui/WEB-INF/lib/  ← TCF lib 없음, Spring만
        ├── batch/                     ← zz-batch.war exploded (/batch)
        └── ROOT/                      ← Tomcat 기본
```

**context 하나(`sv`)의 runtime lib 전체 경로 (③ — 탐색기에서 확인)**

```text
{PROJECT}\ztomcat\apache-tomcat-10.1.34\webapps\sv\WEB-INF\lib\
  tcf-util-1.0.0-SNAPSHOT.jar
  tcf-core-1.0.0-SNAPSHOT.jar
  tcf-web-1.0.0-SNAPSHOT.jar
  spring-boot-3.3.5.jar
  spring-boot-autoconfigure-3.3.5.jar
  h2-2.2.224.jar
  mybatis-3.5.14.jar
  mybatis-spring-boot-starter-3.0.3.jar
  jackson-databind-2.17.2.jar
  logback-classic-1.5.11.jar
  … (총 ~50개)
```

업무 9 context는 **동일한 NSIGHT lib 3개 + 유사한 Maven lib 세트**입니다.  
`om`은 `tcf-cache` 추가, `ui`는 TCF lib 없음, `batch`는 MyBatis·session 일부 생략.

---

### 2.5 런타임 데이터 디렉터리

| 경로 | 생성 | 용도 |
|------|------|------|
| `{PROJECT}\data\nsight-txlog\` | bootRun / Tomcat 첫 txlog | H2 루트 (`nsight.txlog.path`) |
| `{PROJECT}\data\nsight-txlog\nsight_om\` | H2 file DB | OM·batch·거래로그 **공유** |
| `{TOMCAT}\logs\` | Tomcat 기동 | catalina·localhost 로그 |
| `{TOMCAT}\webapps\{code}\WEB-INF\classes\` | autoDeploy | 배포된 `application-dev.yml` 등 |

---

### 2.6 ① Gradle Maven 캐시 — `{GRADLE_USER}` 물리 위치

> **`{GRADLE_USER}/caches/`는 `nsight-tcf-framework` 프로젝트 폴더 안이 아닙니다.**  
> Gradle이 Maven Central에서 받은 JAR(`spring-boot`, `h2`, `mybatis` 등)를 모아 두는 **Windows 사용자 홈 아래 전역 캐시**입니다.

#### `{GRADLE_USER}`가 정해지는 규칙

| 우선순위 | 조건 | `{GRADLE_USER}` 실제 경로 |
|:--------:|------|---------------------------|
| 1 | `GRADLE_USER_HOME` 환경변수 **있음** | 환경변수 값 (예: `D:\gradle-home`) |
| 2 | **없음** (일반 로컬 PC) | `%USERPROFILE%\.gradle` → `C:\Users\{로그인계정}\.gradle` |

문서·대화에서 `{GRADLE_USER}` = Gradle 공식 **`GRADLE_USER_HOME`** 과 동일 개념입니다.

```powershell
# 내 PC에서 {GRADLE_USER} 확인
if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { "$env:USERPROFILE\.gradle" }
```

#### 실제 물리 위치 (Windows — 본 PC 예)

| 기호 | 의미 | 본 PC 예 |
|------|------|----------|
| `{GRADLE_USER}` | Gradle 홈 | `C:\Users\chang.JWS\.gradle` |
| `{GRADLE_USER}/caches/` | Gradle 캐시 루트 | `C:\Users\chang.JWS\.gradle\caches\` |
| `files-2.1/` | **Maven lib JAR 실체** (①) | `C:\Users\chang.JWS\.gradle\caches\modules-2\files-2.1\` |

`{GRADLE_USER}/caches/` 아래에서 **runtime JAR가 있는 곳**은 주로 `modules-2/files-2.1/` 입니다.  
`metadata-2.*`, `jars-9/`, `daemon/` 등은 POM·Gradle 내부용 — **애플리케이션 classpath JAR 아님**.

#### JAR 파일 예 (`files-2.1` 아래)

```text
C:\Users\chang.JWS\.gradle\caches\modules-2\files-2.1\
  com\h2database\h2\2.2.224\{해시}\h2-2.2.224.jar
  org\springframework\boot\spring-boot\3.3.5\{해시}\spring-boot-3.3.5.jar
  org\mybatis\mybatis\3.5.14\{해시}\mybatis-3.5.14.jar
  org\mybatis.spring.boot\mybatis-spring-boot-starter\3.0.3\{해시}\mybatis-spring-boot-starter-3.0.3.jar
  …
```

`{해시}` 폴더명은 Gradle 내부 SHA1 — **버전 폴더(`2.2.224`, `3.3.5`)까지 내려가 `*.jar` 검색**하면 됩니다.

#### 프로젝트 폴더 vs `{GRADLE_USER}` — lib가 있는 세 위치

| 위치 | 물리 경로 (본 PC 예) | lib 종류 | 사용 시점 |
|------|----------------------|----------|-----------|
| **⓪② 프로젝트** | `C:\Programming(23-08-15)\nsight-tcf-framework\` | 소스, `tcf-*.jar`, `*.war` | bootRun·bootWar·deploy |
| **① Gradle 캐시** | `C:\Users\chang.JWS\.gradle\caches\modules-2\files-2.1\` | Maven Central lib | **bootRun** classpath |
| **③ ztomcat exploded** | `{PROJECT}\ztomcat\...\webapps\sv\WEB-INF\lib\` | NSIGHT + Maven lib (WAR에서 풀림) | **ztomcat** 기동 |

```text
C:\Programming(23-08-15)\nsight-tcf-framework\     ← 프로젝트 (⓪②)
C:\Users\chang.JWS\.gradle\caches\                 ← Gradle 전역 (①) — 프로젝트 밖!
{PROJECT}\ztomcat\...\webapps\sv\WEB-INF\lib\      ← Tomcat runtime (③)
```

| 실행 | Maven lib (`spring-boot`, `h2`, …) 읽는 곳 |
|------|---------------------------------------------|
| **`gradle :sv-service:bootRun`** | ① `{GRADLE_USER}\caches\modules-2\files-2.1\` |
| **ztomcat `/sv` 기동** | ③ `webapps\sv\WEB-INF\lib\` (bootWar 때 ①→②→③으로 복사됨) |

#### `{GRADLE_USER}` 디렉터리 트리

```text
{GRADLE_USER}/                       ← 예: C:\Users\chang.JWS\.gradle
├── caches/
│   ├── modules-2/
│   │   ├── files-2.1/                 ← ★ Maven JAR 실체 (artifact 버전별)
│   │   │   ├── com/
│   │   │   │   └── h2database/h2/2.2.224/{sha1}/h2-2.2.224.jar
│   │   │   ├── org/
│   │   │   │   ├── springframework/...
│   │   │   │   └── mybatis/...
│   │   │   └── org/springframework/boot/spring-boot/3.3.5/{sha1}/spring-boot-3.3.5.jar
│   │   └── metadata-2.106/            ← POM·해석 메타 (JAR 아님)
│   └── jars-9/                        ← Gradle 자체 캐시
├── daemon/                            ← Gradle Daemon
└── wrapper/                           ← (본 프로젝트는 gradlew 미사용)
```

**bootRun** 시 JVM classpath의 Maven lib는 **전부 ① `files-2.1`** 에서 읽습니다.  
**ztomcat** 기동 시에는 ①을 직접 쓰지 않고 **③ exploded `WEB-INF/lib`** 만 사용합니다 (내용은 bootWar 때 ①→②로 복사된 동일 버전).

`gradle clean`은 프로젝트 `{모듈}/build/`만 지웁니다 — **① `{GRADLE_USER}/caches/`는 그대로** 남습니다.

---

## 3. 경로 규칙 (Windows)

### 3.1 ① Gradle Maven 캐시

> **`{GRADLE_USER}`가 프로젝트 밖 어디인지** — 전체 설명·본 PC 예: **§2.6**.

```text
{GRADLE_USER_HOME}\caches\modules-2\files-2.1\
  {groupId를 / 로 변환}/{artifactId}/{version}/{SHA1 해시}/{파일명}.jar
```

| 항목 | 값 |
|------|-----|
| `GRADLE_USER_HOME` 기본 | `%USERPROFILE%\.gradle` → `C:\Users\{로그인}\.gradle` |
| Spring Boot BOM | `3.3.5` (루트 `build.gradle`) |
| H2 고정 버전 | `2.2.224` |

**실제 예 (본 PC — `C:\Users\chang.JWS\.gradle\...`)**

| lib | ① 캐시 경로 |
|-----|------------|
| H2 | `...\files-2.1\com.h2database\h2\2.2.224\{해시}\h2-2.2.224.jar` |
| Spring Boot | `...\org.springframework.boot\spring-boot\3.3.5\{해시}\spring-boot-3.3.5.jar` |
| MyBatis | `...\org.mybatis\mybatis\3.5.14\{해시}\mybatis-3.5.14.jar` |
| MyBatis Spring Boot | `...\org.mybatis.spring.boot\mybatis-spring-boot-starter\3.0.3\{해시}\mybatis-spring-boot-starter-3.0.3.jar` |

`{해시}` 폴더는 Gradle 내부용 — **버전 폴더까지 내려가 `*.jar` 검색**하면 됩니다.

```powershell
dir $env:USERPROFILE\.gradle\caches\modules-2\files-2.1\com.h2database\h2\2.2.224\*\h2-2.2.224.jar
```

### 3.2 ⓪ NSIGHT lib — 프로젝트 `build/`

| 모듈 | JAR 산출물 | bootRun 시 추가 경로 |
|------|------------|---------------------|
| `:tcf-util` | `tcf-util/build/libs/tcf-util-1.0.0-SNAPSHOT.jar` | `tcf-util/build/classes/java/main/` |
| `:tcf-core` | `tcf-core/build/libs/tcf-core-1.0.0-SNAPSHOT.jar` | `tcf-core/build/classes/java/main/` |
| `:tcf-web` | `tcf-web/build/libs/tcf-web-1.0.0-SNAPSHOT.jar` | `tcf-web/build/classes/java/main/` |
| `:tcf-cache` | `tcf-cache/build/libs/tcf-cache-1.0.0-SNAPSHOT.jar` | `tcf-cache/build/classes/java/main/` |

- Maven 좌표: `com.nh.nsight` / `1.0.0-SNAPSHOT`
- **Tomcat `lib/`에 넣지 않음** — WAR `WEB-INF/lib`에만 포함

### 3.3 ② WAR (압축 상태)

```text
C:\Programming(23-08-15)\nsight-tcf-framework\sv-service\build\libs\sv.war
  └── WEB-INF/lib/   ← NSIGHT lib + Maven lib 전부 (수십 개)
```

### 3.4 ③ ztomcat exploded

```text
C:\Programming(23-08-15)\nsight-tcf-framework\ztomcat\apache-tomcat-10.1.34\webapps\sv\WEB-INF\lib\
  tcf-util-1.0.0-SNAPSHOT.jar
  tcf-core-1.0.0-SNAPSHOT.jar
  tcf-web-1.0.0-SNAPSHOT.jar
  spring-boot-3.3.5.jar
  h2-2.2.224.jar
  mybatis-3.5.14.jar
  …
```

탐색기에서 **runtime lib 목록을 바로 보려면 ③** 이 가장 편합니다.

---

## 4. NSIGHT lib 모듈 참조

### 4.1 의존 방향

```text
tcf-util                    (Spring 없음)
   └── tcf-core             STF / ETF / TCF, Handler
          └── tcf-web       /online, Filter, AutoConfig, MyBatis
                 └── tcf-cache   EhCache (tcf-om 전용)
```

| Gradle 모듈 | 역할 | 직접 의존 | 빌드 |
|-------------|------|-----------|------|
| `:tcf-util` | 공통 유틸 | — | `gradle :tcf-util:build` |
| `:tcf-core` | TCF 엔진 | `:tcf-util`, Jackson, Spring Boot starter | `gradle :tcf-core:build` |
| `:tcf-web` | HTTP·DS·MyBatis | `:tcf-core`, `:tcf-util`, Spring Web/JDBC | `gradle :tcf-web:build` |
| `:tcf-cache` | EhCache | `:tcf-core`, spring-cache | `gradle :tcf-cache:build` |

### 4.2 실행 모듈별 — NSIGHT lib 포함 여부

| 실행 모듈 | `tcf-util` | `tcf-core` | `tcf-web` | `tcf-cache` |
|-----------|:----------:|:----------:|:---------:|:-----------:|
| 업무 `*-service` (16) | ● | ● | ● | ✕ |
| `tcf-om` | (transitive) | ● | ● | ● |
| `tcf-batch` | (transitive) | ● | ● | ✕ |
| `tcf-ui` | ✕ | ✕ | ✕ | ✕ |
| `om-service` (레거시) | ● | ● | ● | ✕ |

WAR `WEB-INF/lib`에 들어가는 파일명: `tcf-{name}-1.0.0-SNAPSHOT.jar`

---

## 5. Maven lib (외부) — 대표 의존 참조

버전은 Spring Boot BOM **`3.3.5`** + 루트 `dependencyManagement` 기준.  
업무 WAR(`sv-service`) `runtimeClasspath`에 올라오는 **대표 lib**입니다.

### 5.1 Spring Boot·웹·검증

| artifactId (예) | 용도 | ① 캐시 group 경로 |
|-----------------|------|-------------------|
| `spring-boot` | Boot 코어 | `org.springframework.boot/spring-boot/3.3.5/` |
| `spring-boot-autoconfigure` | AutoConfiguration | `.../spring-boot-autoconfigure/3.3.5/` |
| `spring-web`, `spring-webmvc` | MVC | `org.springframework/...` |
| `spring-boot-starter-web` | (starter — transitive) | 여러 JAR로 풀림 |
| `hibernate-validator` | Bean Validation | `org.hibernate.validator/...` |

### 5.2 데이터·MyBatis·H2

| artifactId | 버전 | 용도 |
|------------|------|------|
| `mybatis` | 3.5.14 | SQL 매퍼 |
| `mybatis-spring` | 3.0.3 | Spring 연동 |
| `mybatis-spring-boot-starter` | 3.0.3 | Boot starter |
| `HikariCP` | BOM | Connection pool |
| `h2` | **2.2.224** | 로컬·dev file/in-memory DB |

### 5.3 기타 (업무 WAR 공통)

| artifactId | 용도 |
|------------|------|
| `jackson-databind`, `jackson-core` | JSON |
| `logback-classic`, `slf4j-api` | 로깅 |
| `micrometer-core` | Actuator metrics |
| `spring-session-jdbc` | 세션 (업무·tcf-om) |
| `aspectjweaver` | AOP |

### 5.4 모듈별 Maven lib 차이

| 실행 모듈 | H2 | MyBatis | spring-session-jdbc | EhCache |
|-----------|:--:|:-------:|:-------------------:|:-------:|
| `*-service` | ● | ● | ● | ✕ |
| `tcf-om` | ● | ● | ● | ● (`tcf-cache`) |
| `tcf-batch` | ● | (tcf-web transitive) | ✕ | ✕ |
| `tcf-ui` | ✕ | ✕ | ✕ | ✕ |

### 5.5 운영 Oracle (저장소에 없음)

ojdbc 등 **Oracle JDBC는 Maven Central 캐시에 기본으로 없습니다.**  
prod 배포 시 WAR `WEB-INF/lib/` 또는 Tomcat `lib/`에 **수동 추가** — [environment-variables.md](environment-variables.md) §9.

---

## 6. 실행 방식별 — JVM이 lib를 읽는 위치

| lib 종류 | bootRun | ztomcat (Tomcat 8080) |
|----------|---------|------------------------|
| **Maven lib** | ① `%USERPROFILE%\.gradle\caches\...` | ③ `webapps/{code}/WEB-INF/lib/` |
| **NSIGHT lib** | ⓪ `{모듈}/build/classes` + `build/libs/tcf-*.jar` | ③ `WEB-INF/lib/tcf-*.jar` |
| **업무 클래스** | ⓪ `sv-service/build/classes/` | ③ `WEB-INF/classes/` |
| **설정 yml** | `src/main/resources/` → `build/resources/` | ③ `WEB-INF/classes/application*.yml` |

Tomcat 공통 `apache-tomcat-10.1.34/lib/` — **Tomcat·Servlet API만**. NSIGHT·Spring JAR는 **context별 `WEB-INF/lib`** (WAR마다 독립).

---

## 7. lib 목록 확인 명령

```powershell
# 24 모듈 build/libs 일괄 확인
@('tcf-util','tcf-core','tcf-web','tcf-cache','tcf-ui','tcf-batch','tcf-om',
  'cc-service','ic-service','pc-service','bc-service','ms-service','sv-service',
  'pd-service','cm-service','eb-service','ep-service','bp-service','bd-service',
  'ss-service','cs-service','ct-service','mg-service','om-service') |
  ForEach-Object { Get-ChildItem "$_\build\libs" -ErrorAction SilentlyContinue | ForEach-Object { "$_ : $($_.Name)" } }

# Maven + NSIGHT lib 좌표 (bootRun classpath)
gradle :sv-service:dependencies --configuration runtimeClasspath

# WAR 안 lib (압축)
jar tf sv-service\build\libs\sv.war | findstr WEB-INF/lib

# ztomcat 배포 후 (풀린 파일 — ③)
dir ztomcat\apache-tomcat-10.1.34\webapps\sv\WEB-INF\lib\*.jar

# NSIGHT lib만
dir ztomcat\apache-tomcat-10.1.34\webapps\sv\WEB-INF\lib\tcf-*.jar

# Maven 캐시에서 H2 찾기 (①)
dir $env:USERPROFILE\.gradle\caches\modules-2\files-2.1\com.h2database\h2\2.2.224\*\h2-2.2.224.jar
```

---

## 8. 빌드 순서 (lib → WAR)

단일 업무 WAR(`sv.war`) 배포 스크립트가 호출하는 전형적 순서:

```text
gradle :tcf-util:build :tcf-core:build :tcf-web:build :sv-service:bootWar
         ⓪ tcf JAR 생성          ⓪+① Maven resolve → ② sv.war 패키징
```

ztomcat 전체:

```text
gradle buildZtomcatWars    # 15 WAR 빌드 — 각 WAR마다 위와 동일하게 WEB-INF/lib 구성
```

---

## 9. 자주 하는 질문

| 질문 | 답 |
|------|-----|
| `spring-boot-*.jar`가 프로젝트 폴더에 없어요 | 정상 — ① Gradle 캐시 또는 ③ exploded에 있음 |
| `tcf-core.jar`를 `%USERPROFILE%\.gradle`에서 못 찾아요 | NSIGHT lib는 ⓪ `{모듈}/build/libs/` 또는 ③ WAR 안 |
| bootRun은 ①, ztomcat은 ③ — 같은 버전인가? | **같은 `build.gradle` 좌표** — bootWar 시 ①→②로 복사, deploy 시 ③ |
| Tomcat `lib/`에 Spring을 넣어야 하나? | **✕** — WAR `WEB-INF/lib` per context |
| `gradle clean` 하면 ①도 지워지나? | **✕** — ⓪ `build/`만 삭제. ①은 `%USERPROFILE%\.gradle`에 유지 |

---

## 10. 관련 문서

| 문서 | 내용 |
|------|------|
| [artifacts.md](artifacts.md) | WAR·ztomcat 배치, 전체 산출물 |
| [gradle.md](gradle.md) | `bootWar`, `buildZtomcatWars` |
| [22-build-project.md](../architecture/22-build-project.md) | Gradle 멀티 모듈 구조 |
| [31-autoconfiguration.md](../architecture/31-autoconfiguration.md) | `tcf-web` AutoConfig |
