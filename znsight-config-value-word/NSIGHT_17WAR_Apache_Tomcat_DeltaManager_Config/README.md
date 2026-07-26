# NSIGHT 17개 WAR + Apache + Tomcat DeltaManager + Spring Boot 환경설정 파일

> **참조 구성:** 17 WAR 목표 아키텍처를 설명하는 설정 자료이며, 현재 저장소의 ztomcat 16 WAR 배포 목록과는 별도입니다.

## 1. 구성 목적

`nh.marketing.com` 단일 도메인에서 Apache Reverse Proxy가 17개 WAR context를 동일 Tomcat Cluster로 라우팅하고, Tomcat DeltaManager로 센터 내부 세션을 복제하는 기준 환경설정 파일입니다.

```text
[Client / WebTopSuite]
        ↓ https://nh.marketing.com
[Apache HTTPD 2.4 Reverse Proxy]
        ↓ context path routing + sticky session
[Tomcat Cluster: tomcat-01 / tomcat-02 / tomcat-03]
        ↓ 17 WAR deployed on every Tomcat node
[Spring Boot WAR / HikariCP / MyBatis]
        ↓
[RDW / ADW / APPLOG DB]
```

## 2. 기준 전제

| 항목 | 기준 |
|---|---|
| 배포 방식 | 외장 Tomcat에 Spring Boot WAR 배포 |
| WAR 수 | 17개 |
| 도메인 | `nh.marketing.com` |
| Tomcat 노드 | 예시 3대: `tc01`, `tc02`, `tc03` |
| 세션 복제 | Tomcat DeltaManager, 센터 내부 Cluster 한정 |
| Sticky 기준 | Apache `JSESSIONID` route / Tomcat `jvmRoute` |
| 세션 Timeout | 기본 60분, Absolute Timeout 8시간은 애플리케이션 공통 Filter 구현 |
| Tomcat Thread | 8 vCPU / 32GB 기준 `maxThreads=500` |
| HikariCP | 일반 AP 30~40, SingleView 40~60 기준 조정 |
| MyBatis | `default-statement-timeout=3`초 기본 |

## 3. 적용 순서

1. `00-inventory/tomcat-nodes.csv`에서 실제 IP, 포트, `jvmRoute`를 확정합니다.
2. `01-apache/conf.d/20-proxy-balancer-17war.conf`의 `BalancerMember`를 실제 Tomcat 노드로 변경합니다.
3. `02-tomcat/bin/setenv.sh`에서 노드별 `JVM_ROUTE`, `CLUSTER_BIND_ADDRESS`를 변경합니다.
4. `02-tomcat/conf/server.xml`을 각 Tomcat의 `$CATALINA_BASE/conf/server.xml`에 반영합니다.
5. 각 WAR에 `03-spring-boot/src/main/webapp/WEB-INF/web.xml`의 `<distributable/>`을 반드시 포함합니다.
6. Spring Boot 설정은 `03-spring-boot/src/main/resources/application*.yml`을 기준으로 업무별 값만 분리합니다.
7. Apache/Tomcat/Spring 설정 검증 스크립트를 실행합니다.

## 4. 핵심 운영 원칙

- 17개 WAR는 모든 Tomcat 노드에 동일하게 배포합니다.
- `jvmRoute`는 Tomcat 노드마다 반드시 유일해야 합니다.
- 세션 객체는 반드시 `Serializable`이어야 합니다.
- 세션에는 인증/권한 최소 정보만 저장하고, Single View 조회 결과나 대량 DTO는 저장하지 않습니다.
- Timeout 순서는 `DB Query < Hikari Pool Wait <= Spring TX < Apache Proxy Read < Client Request < L4 Idle`로 유지합니다.
- 센터 간 세션 복제는 기본 미적용으로 보고, 센터 장애 시 재로그인을 정책으로 둡니다.
