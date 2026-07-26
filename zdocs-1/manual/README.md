# NSIGHT TCF — 운영·개발 매뉴얼

| 항목 | 내용 |
|------|------|
| 대상 | 로컬 개발·빌드·배포 담당자 |
| 관련 | [README.md](../../README.md), [docs/architecture/22-build-project.md](../architecture/22-build-project.md), [environment-variables.md](environment-variables.md), [artifacts.md](artifacts.md), [lib-module.md](lib-module.md), [docs/architecture/38-script.md](../architecture/38-script.md) |

---

## 문서 목록

| 문서 | 설명 |
|------|------|
| [gradle.md](gradle.md) | **Gradle 명령어** — bootRun, bootWar, 테스트, 일괄 빌드 |
| [environment-variables.md](environment-variables.md) | **환경변수·JVM 속성** — JAVA_HOME, NSIGHT_*, Spring 프로파일 |
| [artifacts.md](artifacts.md) | **빌드 산출물·기동 파일** — JAR/WAR 물리 경로, ztomcat 배치 |
| [lib-module.md](lib-module.md) | **라이브러리 모듈 참조** — Gradle 모듈 물리 경로, Gradle 캐시→WAR→Tomcat |

### 아키텍처·규약 (패키지·네이밍)

| 문서 | 설명 |
|------|------|
| [01-application-layer.md](../architecture/01-application-layer.md) | **어플리케이션 6계층** — entry/application/persistence 패키지·Handler→Facade→Service 흐름 |
| [06-naming.md](../architecture/06-naming.md) | **네이밍 표준** — serviceId, 클래스 접미사, 패키지 규칙 |
| [22-build-project.md](../architecture/22-build-project.md) | **빌드·모듈 구조** — Gradle 멀티 모듈, WAR 산출물 |
| [TCF_MODULE_RESTRUCTURE.md](../TCF_MODULE_RESTRUCTURE.md) | common-core/web → tcf-core/web 재구성 이력 |

공식 설계안(Word): [설계자료/README.md](../설계자료/README.md)  
스크립트 래퍼(`tcf-scripts`, `ztomcat`): [38-script.md](../architecture/38-script.md)

---

## Gradle 모듈 (settings.gradle)

| 구분 | 모듈 |
|------|------|
| **프레임워크 lib** | `tcf-util`, `tcf-core`, `tcf-web`, `tcf-eai`, `tcf-cache` |
| **플랫폼 WAR/JAR** | `tcf-om`, `tcf-oc`, `tcf-batch`, `tcf-ui`, `tcf-uj`, `tcf-gateway`, `tcf-jwt`, `tcf-help`, `tcf-ai-methodology` |
| **업무 WAR** | `ic-service`, `pc-service`, `ms-service`, `sv-service`, `pd-service`, `eb-service`, `ep-service`, `ss-service`, `mg-service` |
| **레거시 (CI/CD 제외)** | `om-service` → **`tcf-om` 사용** |

---

## bootRun 포트 (로컬)

| 포트 | 모듈 |
|------|------|
| 8082–8087, 8089–8090, 8093, 8096 | `ic` … `mg` (*-service) |
| 8097 | `tcf-om` |
| 8098 | `tcf-batch` |
| 8099 | `tcf-ui` |
| 8100 | `tcf-gateway` |
| 8102 | `tcf-uj` |
| 8110 | `tcf-jwt` |

Tomcat(ztomcat): `deploy-wars.sh` 기준 **16 WAR** on **8080** — `/ic` … `/mg`, `/oc`, `/om`, `/batch`, `/ui`, `/uj`, `/jwt`, `/gw`

---

## 업무 WAR 패키지 규약 (요약)

업무·플랫폼 WAR는 **6계층** 패키지를 사용합니다. WAR 메인 클래스는 `com.nh.nsight.tcf.web.support.NsightWarBootstrap`를 상속합니다.

```text
com.nh.nsight.marketing.{code}
├── application/service|rule|scheduler
├── client/          (해당 시)
├── config/
├── entry/handler|facade|web
├── persistence/dao|mapper
└── support/
```

모듈별 README: [README.md](../../README.md#업무-war-service)

---

## 빠른 실행

```bash
# 업무 + OM + UI
gradle :sv-service:bootRun
gradle :tcf-om:bootRun
gradle :tcf-ui:bootRun

# gateway 경유 UI (tcf-uj)
gradle :tcf-gateway:bootRun    # 8100
gradle :tcf-uj:bootRun         # 8102

# JWT
gradle :tcf-jwt:bootRun        # 8110
```

통합 스크립트: `tcf-scripts/run-local.bat <target>` — [tcf-scripts/README.md](../../tcf-scripts/README.md)
