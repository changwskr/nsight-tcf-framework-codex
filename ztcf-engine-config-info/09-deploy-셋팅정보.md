# Deploy 셋팅정보

> 읽는 순서: **09** · 인덱스: [03-아키텍처-셋팅정보.md](./03-아키텍처-셋팅정보.md)  
> 패키지: `ztcf-engine-config-info/05-deploy/`  
> 기준: ARC05 WAR → Tomcat `webapps` · health 검증  
> 안내: [05-deploy/배포-설정.md](./05-deploy/배포-설정.md)  
> 연계: [04-inventory-셋팅정보.md](./04-inventory-셋팅정보.md)

---

## 1. 역할

빌드 산출 WAR를 모든 Tomcat 노드에 동일 배치하고, HTTPS 도메인 기준 Actuator health로 검증한다.

```text
Gradle bootWar → /app/artifacts/*.war
  → deploy-war-layout.sh
  → $CATALINA_BASE/webapps/
  → validate-endpoints.sh
```

---

## 2. 환경변수 셋팅

| 변수 | 기본값 | 의미 |
|------|--------|------|
| `CATALINA_BASE` | `/app/tomcat-nsight` | 배포 대상 인스턴스 |
| `WAR_SOURCE` | `/app/artifacts` | 산출물 디렉터리 |
| `BASE_URL` | `https://nh.marketing.com` | health 검증 기준 URL |

---

## 3. 배포 WAR 목록 셋팅

| No | WAR | Context | scope |
|---:|-----|---------|-------|
| 1 | ic.war | `/ic` | core |
| 2 | pc.war | `/pc` | core |
| 3 | ms.war | `/ms` | core |
| 4 | sv.war | `/sv` | core |
| 5 | pd.war | `/pd` | core |
| 6 | eb.war | `/eb` | core |
| 7 | ep.war | `/ep` | core |
| 8 | ss.war | `/ss` | core |
| 9 | mg.war | `/mg` | core |
| 10 | om.war | `/om` | core |
| 11 | batch.war | `/batch` | core |
| 12 | ui.war | `/ui` | core |
| 13 | jwt.war | `/jwt` | extended |

### 리네임 규칙 (Gradle 산출물 → webapps)

`deploy-war-layout.sh`가 아래 별칭을 자동 인식한다.

| 빌드 산출 (우선순위) | 배포명 |
|----------------------|--------|
| `om.war` 또는 `tcf-om.war` | `om.war` |
| `ui.war` 또는 `tcf-ui.war` | `ui.war` |
| `batch.war` 또는 `tcf-batch.war` | `batch.war` |
| `jwt.war` 또는 `tcf-jwt.war` | `jwt.war` |
| `ic.war` 등 | 동일 |

로컬 `ztomcat`은 batch를 `zz-batch.war`로 올릴 수 있으나 **context는 `/batch`**.

인벤토리: [00-inventory/war-contexts.csv](./00-inventory/war-contexts.csv)

---

## 4. 스크립트 셋팅

### `deploy-war-layout.sh`

| 동작 | 내용 |
|------|------|
| 1 | `$CATALINA_BASE/webapps` 생성 |
| 2 | WAR_SOURCE의 13개 WAR 복사 (없으면 WARN) |
| 3 | 모든 클러스터 노드에 **동일 세트** 반복 |

```bash
export CATALINA_BASE=/app/tomcat-nsight
export WAR_SOURCE=/app/artifacts/release-YYYYMMDD
./05-deploy/deploy-war-layout.sh
```

### `validate-endpoints.sh`

| 동작 | 내용 |
|------|------|
| 대상 | ic pc ms sv pd eb ep ss mg om batch ui jwt |
| URL | `$BASE_URL/{ctx}/actuator/health` |
| 실패 | 해당 ctx 메시지 후 최종 exit 1 |

```bash
export BASE_URL=https://nh.marketing.com
./05-deploy/validate-endpoints.sh
```

---

## 5. 권장 배포 순서

1. `00-inventory` IP·route 확정  
2. Apache `20-proxy-balancer-tcf.conf` / Tomcat `setenv`·`server.xml` 반영  
3. 노드별 `deploy-war-layout.sh`  
4. Tomcat 기동 (`autoDeploy=false`면 재시작 포함)  
5. `validate-endpoints.sh`  
6. Sticky·노드 장애 세션 테스트  
7. L4 health UP 확인  

로컬 개발은 저장소 `ztomcat/deploy-wars` / `verify-deploy` 사용 가능.

---

## 6. 운영 주의 셋팅

| 항목 | 설정 |
|------|------|
| 동일성 | 전 노드 WAR 세트·버전 동일 |
| autoDeploy | 운영 **false** |
| Gateway | `tcf-gateway` 사용 시 Apache 직행과 경로 중복 금지 |
| 시크릿 | DB 계정은 아티팩트에 넣지 않음 |

---

## 7. 파일 매핑

| 셋팅 | 파일 |
|------|------|
| 배포 | `05-deploy/deploy-war-layout.sh` |
| 검증 | `05-deploy/validate-endpoints.sh` |
| WAR 목록 | `00-inventory/war-contexts.csv` |
| Proxy | `01-apache/conf.d/20-proxy-balancer-tcf.conf` |
