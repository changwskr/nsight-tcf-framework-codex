# tcf-cicd — NSIGHT TCF 환경 설정 (SoT)

`local` / `dev` / `prod` 프로파일별 **Spring yml·Tomcat setenv·Apache** 를 이 디렉터리에서 관리합니다.

| 프로파일 | 용도 | 주요 경로 |
|----------|------|-----------|
| `local/` | bootRun (개발자 PC) | `local/spring/`, `local/ztomcat/` |
| `dev/` | ztomcat 통합 검증 | `dev/spring/`, `dev/ztomcat/` |
| `prod/` | 운영 Tomcat + Apache | `prod/spring/`, `prod/ztomcat/`, `prod/apache/` |

상세: [docs/architecture/25-env-profile.md](../zdocs-1/architecture/25-env-profile.md)

---

## 디렉터리 구조

```text
tcf-cicd/
├── manifest.yaml           # 모듈·프로파일 매핑
├── local/ dev/ prod/
│   ├── spring/{module}/application-{profile}.yml
│   ├── ztomcat/setenv.*
│   └── env/.env.example
├── prod/apache/            # Apache reverse proxy
└── scripts/
    ├── cicd-deploy.ps1 / .bat / .sh   # CI/CD 파이프라인 (sync → build → deploy)
    ├── cicd-build.ps1 / .bat / .sh    # sync + Gradle 빌드만
    ├── cicd-common.ps1                # 공통 모듈·Gradle 유틸
    ├── pull-from-framework.ps1        # framework → tcf-cicd (bootstrap)
    ├── sync-to-framework.ps1        # tcf-cicd → framework (빌드 전)
    └── apply-tomcat-config.sh       # prod → Tomcat conf/nsight/ (runtime)
```

---

## 일반 워크플로

### 1. 최초 bootstrap (framework → tcf-cicd)

```powershell
cd tcf-cicd/scripts
.\pull-from-framework.ps1 -Profile all
```

### 2. 설정 수정 후 framework 반영 (빌드 전)

```powershell
# dev ztomcat 배포 전
.\sync-to-framework.ps1 -Profile dev

# prod 샘플·apache 반영
.\sync-to-framework.ps1 -Profile prod
```

```powershell
# dry-run
.\sync-to-framework.ps1 -Profile dev -DryRun
```

### 3. ztomcat / Gradle 빌드

**local 전체 빌드 (권장):**

```powershell
cd tcf-cicd/local/script
.\build-all.ps1              # sync local + gradle build (전 모듈)
.\build-all.ps1 -Target wars # sync + buildZtomcatWars (16 WAR)
```

**local Tomcat 배포 (16 WAR):**

```powershell
cd tcf-cicd/local/script
.\deploy-wars.ps1            # sync dev + buildZtomcatWars + webapps
.\deploy-wars.ps1 sv om      # 선택 배포
.\deploy-wars.ps1 -Restart   # 배포 후 Tomcat 기동
```

```bash
tcf-cicd/local/script/deploy-wars.sh
tcf-cicd/local/script/deploy-wars.sh sv om batch --restart
```

**local Tomcat 기동/중지:**

```powershell
cd tcf-cicd/local/ztomcat
.\start.ps1              # sync dev + apply-config + batch/ui deploy + start
.\start.ps1 -DeployAll   # 16 WAR 전체 배포 후 기동
.\stop.ps1
.\deploy-restart.ps1     # stop + 16 WAR deploy + start + health verify
.\deploy-restart.ps1 sv om
```

```bash
tcf-cicd/local/ztomcat/start.sh
tcf-cicd/local/ztomcat/stop.sh
tcf-cicd/local/ztomcat/deploy-restart.sh
```

```bash
tcf-cicd/local/script/build-all.sh
tcf-cicd/local/script/build-all.sh wars
```

```bash
# dev
cd ../..
ztomcat/start.sh

# WAR 빌드 (직접 Gradle)
gradle buildZtomcatWars
```

### 4. prod runtime mount (WAR 재빌드 없이 yml만 갱신)

Tomcat `setenv`에 추가:

```sh
CATALINA_OPTS="${CATALINA_OPTS} -Dspring.config.additional-location=file:${CATALINA_BASE}/conf/nsight/"
```

배포:

```bash
export CATALINA_BASE=/path/to/tomcat
tcf-cicd/scripts/apply-tomcat-config.sh prod
# Tomcat 재기동 또는 context reload
```

---

## 관리 대상 모듈

**플랫폼:** `tcf-web`, `tcf-cache`, `tcf-core`, `tcf-om`, `tcf-batch`, `tcf-ui`, `tcf-uj`, `tcf-gateway`, `tcf-jwt`  
**업무:** `ic-service` … `mg-service`, `eb-service`, `ep-service` (9) — **`tcf-om`이 OM·UD 담당** (`om-service`는 레거시, CI/CD 미포함)

`application.yml`(공통)은 **framework 모듈**에 유지합니다.  
`tcf-cicd`는 **`application-{local,dev,prod}.yml`** 만 관리합니다.

---

## CI/CD 배포 스크립트 (`scripts/`)

**전체 파이프라인 (권장):**

```powershell
cd tcf-cicd/scripts
.\cicd-deploy.ps1                          # dev: sync + buildZtomcatWars + webapps
.\cicd-deploy.ps1 -Profile local          # local yml sync
.\cicd-deploy.ps1 sv om -Restart          # 선택 배포 + Tomcat 기동
.\cicd-deploy.ps1 -Action build           # sync + build만
.\cicd-deploy.ps1 -Profile prod -ArtifactDir .\artifacts
```

```bash
tcf-cicd/scripts/cicd-deploy.sh
tcf-cicd/scripts/cicd-deploy.sh --profile dev sv om --restart
tcf-cicd/scripts/cicd-build.sh --target wars
```

| Action | 설명 |
|--------|------|
| `full` | sync → build → deploy (local/dev) |
| `sync` | yml/setenv만 framework 반영 |
| `build` | sync(옵션) + Gradle WAR |
| `deploy` | 기존 WAR → ztomcat webapps |
| `config` | prod runtime yml mount (`CATALINA_BASE` 필요) |

배포 대상 16 context: `ic pc ms sv pd eb ep ss mg oc om ui uj jwt gw batch`.

> `manifest.yaml`의 설정 관리 범위와 위 배포 스크립트의 WAR 범위는 다를 수 있습니다. 배포 가능 여부는 `local/script/deploy-wars.ps1`의 모듈 목록을 기준으로 합니다.

---

## CI 연동 예시

```yaml
# dev pipeline (단일 스크립트)
- run: pwsh tcf-cicd/scripts/cicd-deploy.ps1 -Profile dev

# dev pipeline (단계 분리)
- run: pwsh tcf-cicd/scripts/cicd-deploy.ps1 -Action sync -Profile dev
- run: pwsh tcf-cicd/scripts/cicd-build.ps1 -Profile dev -SkipSync
- run: pwsh tcf-cicd/local/script/deploy-wars.ps1 -SkipSync -SkipBuild

# prod pipeline
- run: pwsh tcf-cicd/scripts/cicd-deploy.ps1 -Profile prod -Action build -ArtifactDir artifacts
- upload: artifacts/*.war
- run: tcf-cicd/scripts/apply-tomcat-config.sh prod  # CATALINA_BASE on server
```

---

## 주의

- **비밀번호·키는 Git에 넣지 않음** — `prod/env/.env.example` 참고, `${NSIGHT_*}` placeholder 유지
- `local`은 개발 편의를 위해 framework와 **양방향 sync** 가능 (bootRun 즉시 반영)
- `prod` Spring yml 변경은 **apply-tomcat-config** 또는 sync 후 WAR 재배포 중 선택
