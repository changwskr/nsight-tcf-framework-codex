# Inventory 셋팅정보

> 읽는 순서: **04** · 인덱스: [03-아키텍처-셋팅정보.md](./03-아키텍처-셋팅정보.md)  
> 패키지: `ztcf-engine-config-info/00-inventory/`  
> SoT: `tomcat-nodes.csv`, `war-contexts.csv`  
> 설명(얇은 안내): [00-inventory/인벤토리-설정.md](./00-inventory/인벤토리-설정.md)

---

## 1. 역할

Apache `BalancerMember`·Tomcat `jvmRoute`·배포 WAR 목록의 **단일 기준표**.

---

## 2. Tomcat 노드 셋팅 (`tomcat-nodes.csv`)

| node | host (예시) | http | shutdown | receiver | jvmRoute | center |
|------|-------------|-----:|---------:|---------:|----------|--------|
| tomcat-01 | 10.10.20.11 | 8080 | 8005 | 4000 | **tc01** | CENTER-A |
| tomcat-02 | 10.10.20.12 | 8080 | 8005 | 4000 | **tc02** | CENTER-A |
| tomcat-03 | 10.10.20.13 | 8080 | 8005 | 4000 | **tc03** | CENTER-A |

| 컬럼 | 의미 |
|------|------|
| jvmRoute | Apache `route=` 와 **반드시 동일** |
| cluster_receiver_port | Tribes NioReceiver (기본 4000) |

---

## 3. WAR Context 셋팅 (`war-contexts.csv`)

| No | WAR | Context | Module | Health | scope |
|---:|-----|---------|--------|--------|-------|
| 1 | ic.war | `/ic` | ic-service | `/ic/actuator/health` | core |
| 2 | pc.war | `/pc` | pc-service | `/pc/actuator/health` | core |
| 3 | ms.war | `/ms` | ms-service | `/ms/actuator/health` | core |
| 4 | sv.war | `/sv` | sv-service | `/sv/actuator/health` | core |
| 5 | pd.war | `/pd` | pd-service | `/pd/actuator/health` | core |
| 6 | eb.war | `/eb` | eb-service | `/eb/actuator/health` | core |
| 7 | ep.war | `/ep` | ep-service | `/ep/actuator/health` | core |
| 8 | ss.war | `/ss` | ss-service | `/ss/actuator/health` | core |
| 9 | mg.war | `/mg` | mg-service | `/mg/actuator/health` | core |
| 10 | om.war | `/om` | tcf-om | `/om/actuator/health` | core |
| 11 | batch.war | `/batch` | tcf-batch | `/batch/actuator/health` | core |
| 12 | ui.war | `/ui` | tcf-ui | `/ui/actuator/health` | core |
| 13 | jwt.war | `/jwt` | tcf-jwt | `/jwt/actuator/health` | extended |

선택(기본 세트 밖): `tcf-gateway`(`/gw`), `tcf-oc`(`/oc`), `tcf-uj`(`/uj`)

---

## 4. 변경 시 함께 수정

1. `01-apache/conf.d/20-proxy-balancer-tcf.conf`
2. `05-deploy/deploy-war-layout.sh` / `validate-endpoints.sh`
3. `03-spring-boot/.../application-war-contexts.yml`
4. 관련 `04`~`10` 셋팅정보.md
