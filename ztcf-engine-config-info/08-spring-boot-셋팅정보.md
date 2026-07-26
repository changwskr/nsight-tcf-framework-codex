# Spring Boot 셋팅정보

> 읽는 순서: **08** · 인덱스: [03-아키텍처-셋팅정보.md](./03-아키텍처-셋팅정보.md)  
> 패키지: `ztcf-engine-config-info/03-spring-boot/`  
> 기준: Spring Boot 3 / Jakarta · JDK **21** · Gradle `bootWar` · 외장 Tomcat  
> 안내: [03-spring-boot/스프링부트-설정.md](./03-spring-boot/스프링부트-설정.md)  
> 연계: [04-inventory-셋팅정보.md](./04-inventory-셋팅정보.md) · [10-operations-셋팅정보.md](./10-operations-셋팅정보.md)

---

## 1. 역할

업무 WAR 공통 설정·세션·Timeout·Hikari/MyBatis 기준값을 정의한다.  
임베디드 Tomcat이 아니라 **provided Tomcat + `<distributable/>` WAR** 전제.

```text
Gradle bootWar (sv.war …)
  → Tomcat webapps
  → context /sv (파일명 기준)
  → application.yml + application-prod.yml
```

---

## 2. 빌드 셋팅

| 항목 | 값 | 파일 |
|------|-----|------|
| JDK | **21** | `gradle-bootWar-fragment.gradle` |
| packaging | war / bootWar | 각 모듈 `build.gradle` |
| Tomcat starter | **providedRuntime** | |
| archiveFileName | `sv.war`, `eb.war`, `om.war` … | 모듈별 |
| Maven fragment | 레거시만 | `pom-war-fragment.xml` |

로컬·운영 Context는 WAR 파일명이 결정한다 (`sv.war` → `/sv`).  
앱 `server.servlet.context-path`는 보통 `/`.

---

## 3. WAR Context 맵

| Module | WAR | Context | Health |
|--------|-----|---------|--------|
| ic-service | ic.war | `/ic` | `/ic/actuator/health` |
| pc-service | pc.war | `/pc` | `/pc/actuator/health` |
| ms-service | ms.war | `/ms` | `/ms/actuator/health` |
| sv-service | sv.war | `/sv` | `/sv/actuator/health` |
| pd-service | pd.war | `/pd` | `/pd/actuator/health` |
| eb-service | eb.war | `/eb` | `/eb/actuator/health` |
| ep-service | ep.war | `/ep` | `/ep/actuator/health` |
| ss-service | ss.war | `/ss` | `/ss/actuator/health` |
| mg-service | mg.war | `/mg` | `/mg/actuator/health` |
| tcf-om | om.war | `/om` | `/om/actuator/health` |
| tcf-batch | batch.war | `/batch` | `/batch/actuator/health` |
| tcf-ui | ui.war | `/ui` | `/ui/actuator/health` |
| tcf-jwt | jwt.war | `/jwt` | `/jwt/actuator/health` |

Online 예: `POST /sv/online`  
맵 파일: `03-spring-boot/src/main/resources/application-war-contexts.yml`

---

## 4. application.yml 셋팅 (공통)

| 영역 | 키 | 값 |
|------|-----|-----|
| Session idle | `server.servlet.session.timeout` | **60m** |
| Cookie name | `…cookie.name` | JSESSIONID |
| Cookie | http-only / secure / same-site | true / true / Lax |
| (참고) embed threads.max | `server.tomcat.threads.max` | 500 — 외장 Connector 우선 |
| TX | `spring.transaction.default-timeout` | **5** 초 |
| Jackson TZ | `spring.jackson.time-zone` | Asia/Seoul |
| MyBatis statement | `default-statement-timeout` | **3** 초 |
| MyBatis | map-underscore-to-camel-case | true |
| MyBatis | default-fetch-size | 100 |
| Actuator | exposure | health, info, metrics, prometheus |
| Health details | show-details | when_authorized |
| Profile | active | prod (템플릿) |

---

## 5. application-prod.yml 셋팅 (운영)

### 5.1 DataSource / Hikari

| DS | JDBC (예시) | pool-name | max | min-idle | connection-timeout |
|----|-------------|-----------|----:|---------:|-------------------:|
| RDW | `rdw-vip.nh.local:1521/RDW` | RDW-HIKARI | **40** | 10 | 3000 ms |
| APPLOG | `applog-vip.nh.local:1521/APPLOG` | APPLOG-HIKARI | **10** | 3 | 3000 ms |

공통 Hikari:

| 항목 | 값 |
|------|-----|
| validation-timeout | 2000 ms |
| idle-timeout | 600000 ms |
| max-lifetime | 1800000 ms |
| keepalive-time | 300000 ms |
| auto-commit | false |
| test-query (RDW) | `SELECT 1 FROM DUAL` |

계정: `${RDW_DB_USER}` / `${RDW_DB_PASSWORD}` 등 **환경변수·Vault**  
`/sv` 조회 부하 시 RDW max **40~60** 업무별 상향.

### 5.2 nsight.* 커스텀

| 키 | 값 |
|----|-----|
| `session.absolute-timeout-minutes` | **480** (8시간) |
| `session.max-session-size-kb` | **5** |
| `timeout.db-query-seconds` | 3 |
| `timeout.transaction-seconds` | 5 |
| `timeout.cruzapim-connect-ms` | 2000 |
| `timeout.cruzapim-read-ms` | 5000 |
| `cruzapim.base-url` | `https://cruzapim.nh.local` |
| `cruzapim.retry-count` | 0 |

---

## 6. WAR web.xml 셋팅

| 항목 | 값 |
|------|-----|
| Jakarta web-app | 5.0 |
| `<distributable/>` | **필수** (클러스터) |
| session-timeout | 60분 |
| cookie | JSESSIONID, HttpOnly, Secure |

파일: `03-spring-boot/src/main/webapp/WEB-INF/web.xml`

---

## 7. 공통 컴포넌트 셋팅

| 클래스 | 역할 |
|--------|------|
| `AbsoluteSessionTimeoutFilter` | Idle와 별도 Absolute 8시간 |
| `LoginUserSession` | 인증 최소 정보, **Serializable** |
| `DataSourceConfig` | RDW/APPLOG 다중 DS 샘플 |

세션 금지: Single View 결과·대량 DTO·파일 바이트 (권장 ≤2KB, 상한 5KB)

---

## 8. Timeout 위치 (App)

| 계층 | 값 |
|------|---:|
| MyBatis statement | 3초 |
| Hikari connection | 3초 |
| Spring TX | 5초 |
| CruzAPIM connect/read | 2초 / 5초 |
| Session idle / absolute | 60분 / 8시간 |

전체 체인: [10-operations-셋팅정보.md](./10-operations-셋팅정보.md)

---

## 9. 파일 매핑

| 셋팅 | 파일 |
|------|------|
| 공통 yml | `03-spring-boot/src/main/resources/application.yml` |
| 운영 yml | `…/application-prod.yml` |
| Context 맵 | `…/application-war-contexts.yml` |
| Gradle 조각 | `03-spring-boot/gradle-bootWar-fragment.gradle` |
| web.xml | `…/webapp/WEB-INF/web.xml` |
| 세션 Filter | `…/session/AbsoluteSessionTimeoutFilter.java` |
