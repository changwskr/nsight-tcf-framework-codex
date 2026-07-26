# 17개 WAR 통합 배포 아키텍처

## 1. 목표 구조

```text
nh.marketing.com
   ↓
Apache HTTPD 2.4
   ↓
Tomcat Cluster
   ├─ tomcat-01, jvmRoute=tc01
   ├─ tomcat-02, jvmRoute=tc02
   └─ tomcat-03, jvmRoute=tc03
   ↓
17 Spring Boot WAR
   ↓
HikariCP / MyBatis / RDW
```

## 2. 세션 유지 방식

1. 최초 요청이 Apache로 들어옵니다.
2. Apache Balancer가 Tomcat 노드 하나를 선택합니다.
3. Tomcat은 `JSESSIONID=...tc01`처럼 `jvmRoute`가 붙은 세션 쿠키를 발급합니다.
4. 이후 Apache는 `JSESSIONID` route를 보고 같은 Tomcat 노드로 우선 라우팅합니다.
5. 해당 Tomcat이 장애나면 같은 센터 내부 다른 Tomcat으로 라우팅되고, DeltaManager 복제 세션으로 서비스가 유지됩니다.

## 3. DeltaManager 적용 범위

DeltaManager는 같은 센터 내부 Tomcat Cluster에 한정합니다. 센터 간 복제는 네트워크 지연, 복제 부하, 장애 전파 위험이 커지므로 기본 미적용입니다.

## 4. WAR 목록

| No | WAR | Context |
|---:|---|---|
| 1 | `nsight-auth.war` | `/auth` |
| 2 | `nsight-portal.war` | `/portal` |
| 3 | `nsight-customer-service.war` | `/customer-service` |
| 4 | `nsight-singleview-service.war` | `/singleview-service` |
| 5 | `nsight-campaign-service.war` | `/campaign-service` |
| 6 | `nsight-event-service.war` | `/event-service` |
| 7 | `nsight-platform-service.war` | `/platform-service` |
| 8 | `nsight-ebm-service.war` | `/ebm-service` |
| 9 | `nsight-message-service.war` | `/message-service` |
| 10 | `nsight-segment-service.war` | `/segment-service` |
| 11 | `nsight-report-service.war` | `/report-service` |
| 12 | `nsight-stat-service.war` | `/stat-service` |
| 13 | `nsight-admin-service.war` | `/admin-service` |
| 14 | `nsight-code-service.war` | `/code-service` |
| 15 | `nsight-file-service.war` | `/file-service` |
| 16 | `nsight-audit-service.war` | `/audit-service` |
| 17 | `nsight-batch-admin.war` | `/batch-admin` |


## 5. 운영 검증 포인트

| 검증 항목 | 확인 방법 |
|---|---|
| Apache Syntax | `apachectl -t` |
| Apache Balancer | `/server-status`, `/balancer-manager` 제한망에서 확인 |
| Tomcat Syntax | `catalina.sh configtest` 또는 기동 로그 확인 |
| DeltaManager | 한 노드 로그인 후 노드 장애 또는 중지 테스트 |
| Sticky Session | `JSESSIONID` route와 Apache access log 확인 |
| Hikari Pool | Actuator Metrics 또는 APM에서 Active/Pending 확인 |
| MyBatis Timeout | 느린 SQL 강제 테스트 |
| 세션 객체 직렬화 | 세션 저장 객체 `Serializable` 확인 |
