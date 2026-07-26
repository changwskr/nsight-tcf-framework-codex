# tcf-scripts 개발자 가이드

> **역할:** 로컬 개발용 **Gradle 래퍼 스크립트** (기동·빌드·배포·curl)  
> **위치:** `tcf-scripts/` · **프로젝트 루트**에서 실행

---

## 1. 이 모듈이 하는 일

매번 긴 Gradle 명령을 외우지 않도록 **run-local**, **build**, **deploy**, **curl-sample** 스크립트를 제공합니다.

---

## 2. Gradle 경로 (Windows)

```powershell
$env:GRADLE_HOME_OVERRIDE = 'C:\Programming(23-08-15)\gradle-8.10.1'
```

탐색 순서: `GRADLE_HOME_OVERRIDE` → `GRADLE_HOME` → PATH `gradle`

---

## 3. run-local — 서비스 기동

```bash
tcf-scripts\run-local.bat <target>
tcf-scripts/run-local.sh <target>
```

| 인자 | 대상 | 포트 |
|------|------|------|
| `sv`, `ic`, `pc`, … | 업무 WAR | 각 README 참고 |
| `tcf-om`, `om` | tcf-om | 8097 |
| `ui`, `tcf-ui` | tcf-ui | 8099 |
| `uj`, `tcf-uj` | tcf-uj | 8102 |
| `gw`, `tcf-gateway` | tcf-gateway | 8100 |
| `jwt`, `tcf-jwt` | tcf-jwt | 8110 |
| `batch`, `tcf-batch` | tcf-batch | 8098 |
| `all` | 9 *-service + tcf-om (각 새 창) | |

단일 인자: 포그라운드. 복수: 백그라운드(새 창).

---

## 4. build — 모듈 빌드

```bash
tcf-scripts\build.bat <target>
```

| 인자 | 설명 |
|------|------|
| `all` | clean + buildBusinessWars (10 WAR) |
| `wars` | buildBusinessWars |
| `ztomcat` | buildZtomcatWars (12 WAR) |
| `tcf` | tcf-util, tcf-core, tcf-web |
| `ui` | tcf-ui bootJar |
| `sv`, `ic`, … | 개별 모듈 |

---

## 5. curl-sample — 샘플 거래

```bash
tcf-scripts\curl-sample.bat sv
```

JSON: `tcf-ui/src/main/resources/sample-requests/{code}-sample-inquiry.json`

---

## 6. deploy — ztomcat 배포

```bash
tcf-scripts\deploy.bat sv ic om
tcf-scripts\deploy.bat batch ui
```

| 인자 | 산출물 |
|------|--------|
| `all` | 업무 10 WAR |
| `batch` | `zz-batch.war` |
| `ui` | `ui.war` |

**12 WAR 전체:** `ztomcat/deploy-wars.bat all`

---

## 7. 포트 맵 (bootRun)

```
8082 ic  8083 pc  8085 ms  8086 sv  8087 pd
8089 eb  8090 ep  8093 ss  8096 mg
8097 om  8098 batch  8099 ui  8100 gw  8102 uj  8110 jwt
8080 ztomcat (통합)
```

---

## 8. Gradle 직접 호출

```bash
gradle buildBusinessWars
gradle buildZtomcatWars
gradle :sv-service:bootRun
```

---

## 9. 참고

| | |
|---|---|
| [tcf-scripts/README.md](../tcf-scripts/README.md) | |
| [docs/architecture/38-script.md](../docs/architecture/38-script.md) | 스크립트 맵 |
| [docs/manual/gradle.md](../docs/manual/gradle.md) | Gradle |
| [ztomcat/README.md](../ztomcat/README.md) | Tomcat |
