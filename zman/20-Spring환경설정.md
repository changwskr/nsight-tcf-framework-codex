# 20장. Spring 환경설정 구조 — 설명

## 설계서 절 목차

20.1~20.4 개요·Profile · 20.5~20.6 application·Context · 20.7~20.11 DataSource·Session·MyBatis·Tx · 20.12~20.18 TCF·Gateway·JWT·Cache·Log·Actuator · 20.19~20.27 Secret·변경관리·운영

---

## 핵심 결론

> `application.yml` = **운영 기준 파일** (Port, DB, Pool, Session, TCF, Security, Log)

Git 형상관리, 서버 직접 수정 ❌, Secret = 환경변수.

---

## 파일 구조 (20.2)

```
src/main/resources/
├── application.yml
├── application-local.yml | -dev | -stg | -prd
├── application-datasource.yml   (일부 모듈)
├── application-tcf.yml          (일부 모듈)
├── application-gateway.yml
├── application-jwt.yml
├── application-cache.yml
├── application-batch.yml
├── logback-spring.xml
└── mapper/{업무}/
```

```yaml
spring.profiles.include: datasource, session, tcf, cache
```

## Profile (20.4)

| Profile | 용도 |
|---------|------|
| local | PC, bootRun |
| dev | 개발 서버 |
| stg | 통합·성능 |
| prd | 운영 |

Gateway `ENV_CODE`와 Route 연동 (9장).

## Context Path (20.6)

| WAR | context-path |
|-----|--------------|
| sv-service | /sv |
| ic-service | /ic |
| tcf-om | /om |
| tcf-jwt | /jwt |

→ `POST /{context}/online`

## DataSource·HikariCP (20.7~20.8)

- RDW / ADW / SESSIONDB **Pool 분리**  
- maximum-pool-size, connection-timeout — DBA 합의  
- Pool = **자원 통제**  

## Spring Session JDBC (20.9)

`tcf-om/application.yml` — `spring.session.store-type: jdbc`, `spring.session.timeout: 60m`

## MyBatis (20.10)

- mapper-locations: `classpath:mapper/**/*.xml`  
- type-aliases-package  
- configuration: map-underscore-to-camel-case  

## Transaction (20.11)

- Facade `@Transactional`  
- timeout 속성 ↔ 14장  

## TCF (20.12)

`application-tcf.yml` — GUID, Idempotency, 거래로그, 정책

## Logging (20.16)

`logback-spring.xml` — MDC: guid, traceId, serviceId, userId

## Actuator (20.17)

`/actuator/health` — CI/CD Health Check (21장)

## Secret (20.19)

- DB password, JWT secret, Internal signature  
- **환경변수 / Vault** — repo 커밋 ❌  

## 설정 오류 장애 (20.24)

| 오류 | 증상 |
|------|------|
| Pool 0 | Connection timeout |
| Session JDBC 미설정 | 매 요청 로그인 |
| Context path 불일치 | 404 |

## 코드베이스

- 각 `*-service/src/main/resources/application*.yml`
- `tcf-gateway`, `tcf-om` 동일 패턴

## 관련 문서

- `zdoc/환경구성.md`, `zdoc/솔루션환경구성.md`

## 이전 · 다음

← [19장 DB](./19-DB-테이블.md) · [21장 CI/CD](./21-CICD-배포.md) →
