# 25. 환경 프로파일 (local / dev / prod)

| 항목 | 내용 |
|------|------|
| 문서 번호 | 25 |
| 제목 | Environment Profiles |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [20-env-spring.md](20-env-spring.md), [24-env-spring-detail.md](24-env-spring-detail.md), [21-env-tomcat.md](21-env-tomcat.md), [23-env-apache.md](23-env-apache.md), [../manual/environment-variables.md](../manual/environment-variables.md) |
| 대상 | 개발·운영·인프라 담당자 |

---

## 1. 개요

NSIGHT TCF는 Spring 프로파일을 **환경 3단계**로 통일한다.

| 프로파일 | 용도 | 전형적 실행 |
|----------|------|-------------|
| **`local`** | 개발자 PC, bootRun | `gradle :sv-service:bootRun`, 모듈별 포트 |
| **`dev`** | 통합·검증 (ztomcat, 개발 서버) | ztomcat `setenv` → `-Dspring.profiles.active=dev` |
| **`prod`** | 운영 Tomcat | `-Dspring.profiles.active=prod` 또는 `SPRING_PROFILES_ACTIVE=prod` |

이전 `bootrun`·`tomcat`·`local,tomcat` 복합 프로파일은 **폐기**되었다.

**UI Relay** (`nsight.tcf-ui.deployment-mode: bootrun|tomcat`)는 Spring 프로파일과 별개로, API 호출이 **개별 포트**인지 **게이트웨이 context**인지를 나타낸다.

| Spring 프로파일 | UI `deployment-mode` | 배치 수집 URL |
|-----------------|----------------------|---------------|
| `local` | `bootrun` (기본) | `http://127.0.0.1:{port}` |
| `dev` / `prod` | `tomcat` | `${nsight.gateway.base-url}/{ctx}` |

---

## 2. 프로파일 활성화

| 실행 방식 | 설정 위치 | 값 |
|-----------|-----------|-----|
| 업무·`tcf-om` bootRun | `application.yml` `spring.profiles.default` | `local` |
| `tcf-batch` bootRun | `application.yml` + `build.gradle` | `local` |
| ztomcat WAR | `ztomcat/conf/setenv.*` | `dev` |
| 운영 Tomcat | `setenv.prod.*` / K8s env | `prod` |
| 오버라이드 | `SPRING_PROFILES_ACTIVE`, `-Dspring.profiles.active` | JVM·환경변수가 yml보다 우선 |

```sh
# ztomcat (통합 검증)
-Dspring.profiles.active=dev

# 운영
-Dspring.profiles.active=prod
```

---

## 3. `prod` 프로파일 그룹

`prod` 활성 시 `dev` 설정을 함께 로드한다 (`tcf-web.jar`):

```yaml
spring:
  profiles:
    group:
      prod:
        - dev
```

`application-prod.yml`은 게이트웨이 URL 등 **운영 오버라이드만** 담고, 수집 타겟·로깅 골격은 `application-dev.yml`을 재사용한다.

---

## 4. 프로파일별 설정 파일

모든 Spring Boot 실행 모듈·프레임워크 JAR는 **`application.yml`(공통) + `application-{local,dev,prod}.yml`** 구조를 따른다.

| 모듈 | 공통 | local | dev | prod |
|------|------|-------|-----|------|
| `tcf-util` | — | — | — | — (Spring 비의존) |
| `tcf-core` | ✅ | ✅ | ✅ | ✅ |
| `tcf-web` | ✅ | ✅ | ✅ | ✅ |
| `tcf-cache` | ✅ | ✅ | ✅ | ✅ |
| `tcf-om` | ✅ | ✅ | ✅ | ✅ |
| `tcf-batch` | ✅ | ✅ | ✅ | ✅ |
| `tcf-ui` | ✅ | ✅ | ✅ | ✅ |
| `*-service` ×16 + `om-service` | ✅ | ✅ | ✅ | ✅ |

업무 WAR 일괄 생성: `scripts/gen-business-profiles.ps1`

**환경 설정 SoT:** [`tcf-cicd/`](../../tcf-cicd/README.md) — `local`/`dev`/`prod` yml·setenv·Apache. 빌드 전 `tcf-cicd/scripts/sync-to-framework.ps1 -Profile dev|prod`.

원문: [24-env-spring-detail.md](24-env-spring-detail.md).

---

## 5. 환경별 URL·게이트웨이

### 5.1 `local` (bootRun)

| 항목 | 예시 |
|------|------|
| `tcf-om` | `http://127.0.0.1:8097` |
| `tcf-batch` | `http://127.0.0.1:8098/batch` |
| `tcf-ui` | `http://127.0.0.1:8099` |
| `tcf-gateway` | `http://127.0.0.1:8100` |
| `tcf-uj` | `http://127.0.0.1:8102` |
| `tcf-jwt` | `http://127.0.0.1:8110` |
| 업무 `sv` | `http://127.0.0.1:8086` |
| `nsight.om.batch-service-url` | `http://127.0.0.1:8098/batch` |

### 5.2 `dev` (ztomcat)

| 항목 | 예시 |
|------|------|
| 게이트웨이 | `http://127.0.0.1:8080` |
| OM | `http://127.0.0.1:8080/om` |
| 배치 | `http://127.0.0.1:8080/batch` |
| UI | `http://127.0.0.1:8080/ui` |

### 5.3 `prod` (운영)

| 환경변수 | 용도 |
|----------|------|
| `NSIGHT_GATEWAY_BASE_URL` | 공개 API·Relay 게이트웨이 (Apache 443 등) |
| `NSIGHT_BATCH_SERVICE_URL` | OM → 배치 (미지정 시 `{gateway}/batch`) |
| `NSIGHT_TXLOG_PATH` | 공유 H2·거래로그 경로 (Tomcat) |

운영 시 **환경변수 요약**: [../manual/environment-variables.md](../manual/environment-variables.md) §8–§9.

---

## 5.4 운영 Tomcat `setenv.prod.*`

운영 서버에는 `ztomcat/conf/setenv.prod.sh`(또는 `.bat`) 샘플을 참고해 Tomcat `bin/setenv.*`에 반영한다.

| 항목 | 예시 |
|------|------|
| Spring 프로파일 | `-Dspring.profiles.active=prod` |
| 게이트웨이 | `export NSIGHT_GATEWAY_BASE_URL=https://marketing.example.com` |
| Heap | `-Xms1024m -Xmx4096m` (샘플 기본값) |
| DB | `NSIGHT_OM_DB_URL`, `NSIGHT_{CODE}_DB_URL` 등 — `application-prod.yml` 참조 |

ztomcat 로컬 통합 검증은 **`conf/setenv.*`(dev)** 를 그대로 사용한다. prod 샘플은 Git 참조용이며 `apply-config` 기본 복사 대상이 아니다.

---

## 6. 이전 프로파일 매핑

| 이전 | 현재 |
|------|------|
| `local` (업무 bootRun) | **`local`** |
| `bootrun` (`tcf-batch`) | **`local`** (`application-local.yml`) |
| `local,tomcat` (ztomcat) | **`dev`** |
| — | **`prod`** (신규, 운영 전용) |

---

## 7. 체크리스트

**local 개발**

- [ ] `gradle :sv-service:bootRun` — 로그에 `The following 1 profile is active: "local"`
- [ ] `gradle :tcf-batch:bootRun` — context `/batch`, 소수 수집 타겟

**dev (ztomcat)**

- [ ] `setenv`에 `spring.profiles.active=dev`
- [ ] 기동 로그: `profile is active: "dev"`
- [ ] `http://localhost:8080/ui/om/admin/login.html` Relay 동작

**prod**

- [ ] `NSIGHT_GATEWAY_BASE_URL` 설정
- [ ] `spring.profiles.active=prod` → 활성 프로파일 `prod`, `dev` (그룹)
- [ ] `tcf-ui` `tomcat-gateway-url` = 공개 URL

---

## 8. 참고 소스

| # | 경로 |
|---|------|
| 1 | `tcf-web/.../application.yml` — `spring.profiles.group.prod` |
| 2 | `tcf-*/.../application-{local,dev,prod}.yml` |
| 3 | `ztomcat/conf/setenv.sh` — `dev` |
| 4 | `ztomcat/conf/setenv.prod.sh` — 운영 샘플 |
| 5 | `tcf-om/.../OmSystemConfigRuntimeSupport.java` — dev/prod → Tomcat WAR 모드 표시 |
