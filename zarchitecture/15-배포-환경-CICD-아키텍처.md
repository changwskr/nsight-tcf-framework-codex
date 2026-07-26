# 15. 배포·환경·CICD 아키텍처

> **범위:** tcf-cicd, tcf-scripts, ztomcat, bootRun vs Tomcat, Profile  
> **관련:** [zman/21-CICD-배포.md](../zman/21-CICD-배포.md) · [docs/architecture/25-env-profile.md](../docs/architecture/25-env-profile.md)

---

## 1. 개요

NSIGHT는 **3단계 배포 모델**과 **Profile 기반 설정**을 사용한다.

| 모드 | 용도 | 진입 |
|------|------|------|
| bootRun | 개발 PC | Gradle, 모듈별 포트 |
| ztomcat | 통합 검증 | Tomcat 8080, 다중 Context |
| 운영 Tomcat | Production | Apache + [Gateway] + WAR |

---

## 2. tcf-cicd — 설정 Source of Truth

### 2.1 Profile

| Profile | 용도 | 경로 |
|---------|------|------|
| local | bootRun | tcf-cicd/local/ |
| dev | ztomcat 검증 | tcf-cicd/dev/ |
| prod | 운영 | tcf-cicd/prod/ |

### 2.2 관리 대상

**플랫폼:** tcf-web, tcf-cache, tcf-core, tcf-om, tcf-batch, tcf-ui, tcf-uj, tcf-gateway, tcf-jwt

**업무:** ic, pc, ms, sv, pd, eb, ep, ss, mg (9)

**미포함:** om-service (레거시)

각 모듈: `application-{local,dev,prod}.yml` — 공통 `application.yml`은 framework 모듈 유지

### 2.3 워크플로

```
pull-from-framework  →  tcf-cicd 수정  →  sync-to-framework  →  Gradle build  →  deploy
```

```powershell
tcf-cicd/scripts/sync-to-framework.ps1 -Profile dev
tcf-cicd/scripts/cicd-deploy.ps1 -Profile dev
tcf-cicd/local/script/deploy-wars.ps1 sv om -Restart
```

### 2.4 prod Runtime Mount

WAR 재빌드 없이 yml만:

```sh
CATALINA_OPTS="${CATALINA_OPTS} -Dspring.config.additional-location=file:${CATALINA_BASE}/conf/nsight/"
tcf-cicd/scripts/apply-tomcat-config.sh prod
```

---

## 3. tcf-scripts — 로컬 래퍼

| 스크립트 | 역할 |
|----------|------|
| run-local.bat | bootRun (sv, ui, gw, …) |
| build.bat | all, wars, ztomcat, tcf |
| deploy.bat | WAR → ztomcat webapps |
| curl-sample.bat | 샘플 JSON curl |

12 WAR 전체: `ztomcat/deploy-wars.bat all`

---

## 4. ztomcat — 통합 Tomcat

| Context | WAR | 포트 |
|---------|-----|------|
| /ic … /mg | 업무 9 + om | 8080 |
| /batch | tcf-batch | 8080 |
| /ui | tcf-ui | 8080 |
| /uj | tcf-uj | 8080 |
| /gw | tcf-gateway | 8080 |
| /jwt | tcf-jwt | 8080 |

Profile: `spring.profiles.active=dev`

---

## 5. Gradle 빌드 타겟

| Task | 산출물 |
|------|--------|
| buildBusinessWars | 9 업무 + tcf-om (10 WAR) |
| buildZtomcatWars | 12 WAR (batch, ui 포함) |
| :sv-service:bootWar | sv.war |

---

## 6. WAR 패키징

```
sv.war
└── WEB-INF/
    ├── classes/          (업무)
    └── lib/
        ├── tcf-util.jar
        ├── tcf-core.jar
        ├── tcf-web.jar
        └── ...
```

각 WAR **독립** — lib 내장, WAR 간 공유 Tomcat lib ❌

---

## 7. 환경 변수 (prod)

`${NSIGHT_*}` placeholder — Git 미포함

예: `NSIGHT_GATEWAY_BASE_URL`, DB URL, JWT 키

참고: `docs/manual/environment-variables.md`, `tcf-cicd/prod/env/.env.example`

---

## 8. Apache (prod)

`tcf-cicd/prod/apache/` — Reverse Proxy, SSL, Sticky

```
Client → Apache → Tomcat (8080) / Gateway
```

상세: [docs/architecture/23-env-apache.md](../docs/architecture/23-env-apache.md)

---

## 9. CI/CD Pipeline 예

```yaml
# dev
- pwsh tcf-cicd/scripts/cicd-deploy.ps1 -Profile dev

# prod
- pwsh tcf-cicd/scripts/cicd-deploy.ps1 -Profile prod -Action build
- upload artifacts/*.war
- tcf-cicd/scripts/apply-tomcat-config.sh prod
```

| Action | sync | build | deploy |
|--------|------|-------|--------|
| full | ✅ | ✅ | ✅ |
| build | ✅ | ✅ | |
| config | ✅ | | (prod mount) |

---

## 10. Profile별 Gateway·Batch

| Module | local | dev |
|--------|-------|-----|
| tcf-gateway env-code | LOCAL | DEV |
| tcf-batch targets | bootRun ports | :8080/context |
| tcf-uj deployment-mode | bootrun | tomcat |

---

## 11. 관련 문서

| | |
|---|---|
| [zguide/tcf-cicd-개발가이드.md](../zguide/tcf-cicd-개발가이드.md) | |
| [zguide/tcf-scripts-개발가이드.md](../zguide/tcf-scripts-개발가이드.md) | |
| [ztomcat/README.md](../ztomcat/README.md) | |
| [zdoc/환경구성.md](../zdoc/환경구성.md) | |

---

← [14-이벤트](./14-이벤트-연계-아키텍처.md) · [16-레퍼런스 →](./16-모듈-포트-의존성-레퍼런스.md)
