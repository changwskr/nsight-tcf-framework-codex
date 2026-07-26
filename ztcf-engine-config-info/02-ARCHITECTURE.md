# TCF WAR 통합 배포 아키텍처 (ARC05)

수치·타임아웃 상세 → [03-아키텍처-셋팅정보.md](./03-아키텍처-셋팅정보.md) 및 `04`~`10` 계층 셋팅정보


## 1. 목표 구조

```text
nh.marketing.com
   ↓
Apache HTTPD 2.4 (SSL + Sticky JSESSIONID)
   ↓
Tomcat Cluster (jvmRoute=tc01|tc02|tc03, DeltaManager)
   ↓
Spring Boot WAR (JDK 21 / Gradle bootWar)
   ↓
HikariCP / MyBatis / RDW
```

## 2. 세션 유지

1. Apache Balancer가 Tomcat 선택  
2. `JSESSIONID=….tc01` 발급 (`jvmRoute`)  
3. Sticky로 동일 노드 우선  
4. 노드 장애 시 센터 내 다른 노드 + DeltaManager 복제 세션  

DeltaManager는 **센터 내부만**. 센터 간은 재로그인.

## 3. WAR 목록

| No | WAR | Context | Module | scope |
|---:|-----|---------|--------|-------|
| 1 | ic.war | `/ic` | ic-service | core |
| 2 | pc.war | `/pc` | pc-service | core |
| 3 | ms.war | `/ms` | ms-service | core |
| 4 | sv.war | `/sv` | sv-service | core |
| 5 | pd.war | `/pd` | pd-service | core |
| 6 | eb.war | `/eb` | eb-service | core |
| 7 | ep.war | `/ep` | ep-service | core |
| 8 | ss.war | `/ss` | ss-service | core |
| 9 | mg.war | `/mg` | mg-service | core |
| 10 | om.war | `/om` | tcf-om | core |
| 11 | batch.war | `/batch` | tcf-batch | core |
| 12 | ui.war | `/ui` | tcf-ui | core |
| 13 | jwt.war | `/jwt` | tcf-jwt | extended |

- Online: `POST /sv/online` · Health: `GET /sv/actuator/health`  
- `/` → `/ui/`  
- 선택: gateway / oc / uj (기본 세트 밖)

## 4. 검증 포인트

| 항목 | 방법 |
|------|------|
| Apache | `apachectl -t`, `/balancer-manager`(제한망) |
| Tomcat | `catalina.sh configtest`, health |
| Sticky | `JSESSIONID` route ↔ access log |
| DeltaManager | 로그인 후 노드 중지 테스트 |
| 배포 | `05-deploy/validate-endpoints.sh` |
