# tcf-cicd 개발자 가이드

> **역할:** `local` / `dev` / `prod` **환경 설정 Source of Truth** (Spring yml, Tomcat, Apache)  
> **유형:** 설정·스크립트 (실행 WAR 아님)

---

## 1. 이 모듈이 하는 일

프로파일별 `application-{local,dev,prod}.yml`, Tomcat `setenv`, Apache reverse proxy를 **한곳에서 관리**하고 framework 모듈로 sync합니다.

| Profile | 용도 |
|---------|------|
| `local/` | bootRun (개발 PC) |
| `dev/` | ztomcat 통합 검증 |
| `prod/` | 운영 Tomcat + Apache |

공통 `application.yml`은 **각 framework 모듈**에 유지. tcf-cicd는 **프로파일 yml만** 관리.

---

## 2. 디렉터리 구조

```
tcf-cicd/
├── manifest.yaml
├── local/ dev/ prod/
│   ├── spring/{module}/application-{profile}.yml
│   ├── ztomcat/setenv.*
│   └── env/.env.example
├── prod/apache/
└── scripts/
    ├── cicd-deploy.ps1 / .sh
    ├── sync-to-framework.ps1
    └── pull-from-framework.ps1
```

---

## 3. 일반 워크플로

### 최초 bootstrap

```powershell
cd tcf-cicd/scripts
.\pull-from-framework.ps1 -Profile all
```

### 설정 수정 → framework 반영 (빌드 전)

```powershell
.\sync-to-framework.ps1 -Profile dev
.\sync-to-framework.ps1 -Profile prod -DryRun   # 미리보기
```

### ztomcat 빌드·배포

```powershell
cd tcf-cicd/local/script
.\build-all.ps1 -Target wars
.\deploy-wars.ps1 sv om -Restart
```

```bash
tcf-cicd/local/ztomcat/start.sh
tcf-cicd/scripts/cicd-deploy.sh --profile dev sv om --restart
```

---

## 4. 관리 대상 모듈

**플랫폼:** tcf-web, tcf-cache, tcf-core, tcf-om, tcf-batch, tcf-ui, tcf-uj, tcf-gateway, tcf-jwt  
**업무:** ic, pc, ms, sv, pd, eb, ep, ss, mg (9) — **om-service 레거시 제외**

---

## 5. CI/CD 스크립트

```powershell
.\cicd-deploy.ps1                          # dev full
.\cicd-deploy.ps1 -Profile local
.\cicd-deploy.ps1 sv om -Restart
.\cicd-deploy.ps1 -Action build            # sync + build만
```

| Action | 설명 |
|--------|------|
| `full` | sync → build → deploy |
| `sync` | yml/setenv만 반영 |
| `build` | Gradle WAR |
| `deploy` | WAR → ztomcat |
| `config` | prod runtime yml mount |

---

## 6. prod runtime (WAR 재빌드 없이 yml만)

Tomcat `setenv`:

```sh
CATALINA_OPTS="${CATALINA_OPTS} -Dspring.config.additional-location=file:${CATALINA_BASE}/conf/nsight/"
```

```bash
export CATALINA_BASE=/path/to/tomcat
tcf-cicd/scripts/apply-tomcat-config.sh prod
```

---

## 7. 주의

- **비밀번호·키 Git 금지** — `prod/env/.env.example`, `${NSIGHT_*}` placeholder
- `local`은 framework와 **양방향 sync** 가능 (bootRun 즉시 반영)

---

## 8. 참고

| | |
|---|---|
| [tcf-cicd/README.md](../tcf-cicd/README.md) | |
| [docs/architecture/25-env-profile.md](../docs/architecture/25-env-profile.md) | 프로파일 |
| [tcf-scripts-개발가이드.md](./tcf-scripts-개발가이드.md) | 로컬 스크립트 |
