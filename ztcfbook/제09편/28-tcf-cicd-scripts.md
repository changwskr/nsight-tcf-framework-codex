# 제28장. tcf-cicd · tcf-scripts

| 항목 | 내용 |
| --- | --- |
| **편** | 제9편 · 모듈별 레퍼런스 (Quick Start) |
| **장** | 제28장 |
| **파일** | `제09편/28-tcf-cicd-scripts.md` |
| **상태** | 집필 완료 |
| **목차** | [00-목차](../00-목차.md) |

---

## 28.1 tcf-cicd — 환경 설정 SoT

| 항목 | 내용 |
| --- | --- |
| 유형 | 설정·스크립트 (실행 WAR 아님) |
| 역할 | `local` / `dev` / `prod` 프로파일 yml, Tomcat, Apache **Source of Truth** |

### Profile

| Profile | 용도 |
| --- | --- |
| `local/` | bootRun (개발 PC) |
| `dev/` | ztomcat 통합 검증 |
| `prod/` | 운영 Tomcat + Apache |

공통 `application.yml`은 각 framework 모듈에 유지. tcf-cicd는 **프로파일 yml만** 관리.

### 디렉터리

```text
tcf-cicd/
├── manifest.yaml
├── local/ dev/ prod/
│   ├── spring/{module}/application-{profile}.yml
│   ├── ztomcat/setenv.*
│   └── env/.env.example
├── prod/apache/
└── scripts/
    ├── sync-to-framework.ps1
    ├── pull-from-framework.ps1
    └── cicd-deploy.ps1
```

### 워크플로

```powershell
# 최초: framework → tcf-cicd
cd tcf-cicd/scripts
.\pull-from-framework.ps1 -Profile all

# 수정 후: tcf-cicd → framework
.\sync-to-framework.ps1 -Profile dev

# ztomcat 배포
.\cicd-deploy.ps1 --profile dev sv om --restart
```

---

## 28.2 tcf-scripts — 로컬 실행 스크립트

| 스크립트 | 용도 |
| --- | --- |
| `run-local.bat {module}` | 단일 WAR bootRun |
| `run-all-local.bat` | 주요 WAR 일괄 기동 |
| `build-war.bat {module}` | WAR 빌드 |

### Quick Start

```bash
# SV만 기동
tcf-scripts/run-local.bat sv

# OM + UI
tcf-scripts/run-local.bat tcf-om
tcf-scripts/run-local.bat tcf-ui
```

Windows `.bat` / Linux `.sh` 쌍 제공. Gradle `:sv-service:bootRun` 래퍼.

---

## 28.3 ztomcat 연계

| 단계 | 명령 |
| --- | --- |
| WAR 빌드 | `gradle :sv-service:bootWar` |
| 배포 | `ztomcat/deploy-wars.bat sv om` |
| 기동 | `ztomcat/start.bat` |
| 검증 | `http://localhost:8080/sv/online` |

tcf-cicd `dev/ztomcat/setenv.*`가 JVM·Profile을 통제합니다.

---

## 28.4 CI/CD Pipeline (개요)

```text
Push/MR → Build → Unit Test → SonarQube → Integration Test
       → bootWar → Nexus/Artifact → ztomcat/stg Deploy
       → Smoke → Health → (Rollback 준비)
```

상세: [제20장](../제06편/20-CICD-릴리즈-DR.md), [docs/architecture/49-release-strategy.md](../../docs/architecture/49-release-strategy.md)

---

## 장 요약

**tcf-cicd**는 local/dev/prod 환경 설정의 단일 원천이며 sync 스크립트로 framework 모듈과 동기화합니다. **tcf-scripts**는 로컬 bootRun·WAR 빌드를 단축하고, **ztomcat**과 결합해 Tomcat WAR 배포를 검증합니다. 릴리즈 Pipeline은 tcf-cicd manifest + GitLab CI/CD가 담당합니다.

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [제27장 tcf-eai · tcf-cache · tcf-batch](./27-tcf-eai-cache-batch.md) |
| → 다음 | [제29장 ic · pc · ms · sv · pd](./29-업무-WAR-ic-pc-ms-sv-pd.md) |

---

## 출처 색인

| 절 | 출처 |
| --- | --- |
| 28.1 | [zguide/tcf-cicd-개발가이드.md](../../zguide/tcf-cicd-개발가이드.md), [tcf-cicd/README.md](../../tcf-cicd/README.md) |
| 28.2 | [zguide/tcf-scripts-개발가이드.md](../../zguide/tcf-scripts-개발가이드.md) |
| 28.3 | [ztomcat/README.md](../../ztomcat/README.md), [znsight-man/10-bootRun-Tomcat-WAR-차이.md](../../znsight-man/10-bootRun-Tomcat-WAR-차이.md) |
| 28.4 | [znsight-man/65-CICD-파이프라인-기준.md](../../znsight-man/65-CICD-파이프라인-기준.md) |
