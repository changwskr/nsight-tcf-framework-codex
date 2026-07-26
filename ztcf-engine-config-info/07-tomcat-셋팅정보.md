# Tomcat 셋팅정보

> 읽는 순서: **07** · 인덱스: [03-아키텍처-셋팅정보.md](./03-아키텍처-셋팅정보.md)  
> 패키지: `ztcf-engine-config-info/02-tomcat/`  
> 기준: ARC05 / ztomcat · JDK **21** · DeltaManager(센터 내부)  
> 안내: [02-tomcat/톰캣-설정.md](./02-tomcat/톰캣-설정.md)  
> 연계: [06-apache-셋팅정보.md](./06-apache-셋팅정보.md) · [04-inventory-셋팅정보.md](./04-inventory-셋팅정보.md)

---

## 1. 역할

외장 Tomcat에 Spring Boot WAR를 배포하고, `jvmRoute` + DeltaManager로 센터 내부 세션 sticky/복제를 수행한다.

```text
Apache (route=tc0x)
    → Tomcat Connector :8080 (proxy https)
    → Engine jvmRoute=tc0x
    → Cluster DeltaManager
    → webapps/{ic,pc,ms,sv,...}.war
```

---

## 2. 노드 인벤토리 셋팅

| node | host (예시) | http | shutdown | receiver | jvmRoute | center |
|------|-------------|-----:|---------:|---------:|----------|--------|
| tomcat-01 | 10.10.20.11 | 8080 | 8005 | 4000 | **tc01** | CENTER-A |
| tomcat-02 | 10.10.20.12 | 8080 | 8005 | 4000 | **tc02** | CENTER-A |
| tomcat-03 | 10.10.20.13 | 8080 | 8005 | 4000 | **tc03** | CENTER-A |

출처: `00-inventory/tomcat-nodes.csv`  
**필수:** Apache `BalancerMember route` ≡ Tomcat `jvmRoute`

---

## 3. JVM / setenv 셋팅 (`bin/setenv.sh`)

| 항목 | 값 | 비고 |
|------|-----|------|
| JAVA_HOME | `/usr/lib/jvm/java-21` | 프레임워크 toolchain과 동일 |
| CATALINA_BASE | `/app/tomcat-nsight` | 인스턴스 |
| CATALINA_HOME | `/opt/tomcat` | 설치 홈 |
| JVM_ROUTE | tc01 / tc02 / tc03 | 노드별 유일 |
| HTTP_PORT | 8080 | |
| SHUTDOWN_PORT | 8005 | |
| CLUSTER_BIND_ADDRESS | 노드 IP | Tribes Receiver |
| CLUSTER_RECEIVER_PORT | 4000 | |
| Heap | `-Xms12g -Xmx12g` | 실용량 재산정 |
| GC | G1, `MaxGCPauseMillis=200` | |
| OOM | HeapDump + ExitOnOutOfMemoryError | |
| encoding / TZ | UTF-8 / Asia/Seoul | |
| -D 전달 | `jvmRoute`, `http.port`, `shutdown.port`, `cluster.bind.address`, `cluster.receiver.port` | server.xml |

---

## 4. Connector 셋팅 (`conf/server.xml`)

| 항목 | 값 |
|------|-----|
| protocol | Http11NioProtocol |
| maxThreads | **500** |
| minSpareThreads | 100 |
| acceptCount | 500 |
| maxConnections | 10000 |
| connectionTimeout | 8000 ms |
| keepAliveTimeout | 5000 ms |
| maxKeepAliveRequests | 100 |
| URIEncoding | UTF-8 |
| proxyName / proxyPort | `nh.marketing.com` / 443 |
| scheme / secure | https / true |
| Engine jvmRoute | `${jvmRoute}` |

---

## 5. Cluster / DeltaManager 셋팅

| 항목 | 값 |
|------|-----|
| Cluster | SimpleTcpCluster |
| channelSendOptions | 8 |
| Manager | **DeltaManager** |
| expireSessionsOnShutdown | false |
| Membership | Mcast `228.0.0.4:45564` (frequency 500, dropTime 3000) |
| Receiver | NioReceiver, port `${cluster.receiver.port}`, maxThreads 6 |
| Sender | PooledParallelSender |
| Valve | ReplicationValve |
| 복제 범위 | **센터 내부만** (센터 간 미적용) |

멀티캐스트 불가 망: Static Membership으로 변경 검토.

---

## 6. Context / Cookie 셋팅

### `conf/context.xml`

| 항목 | 값 |
|------|-----|
| sessionCookieName | JSESSIONID |
| useHttpOnly | true |
| Manager pathname | `""` (파일 영속화 OFF) |
| sameSiteCookies | lax |
| JarScanner scanManifest | false |

### WAR `WEB-INF/web.xml` (각 WAR)

| 항목 | 값 |
|------|-----|
| `<distributable/>` | **필수** |
| session-timeout | 60 (분) |
| cookie | JSESSIONID, HttpOnly, Secure |

---

## 7. Host / 배포 셋팅

| 항목 | 값 |
|------|-----|
| appBase | webapps |
| unpackWARs | true |
| autoDeploy | **false** (운영) |
| AccessLog pattern | `%h … %D %{JSESSIONID}c %{X-GUID}i` |

배포 WAR(core+jwt): `ic pc ms sv pd eb ep ss mg om batch ui jwt`  
→ [00-inventory/war-contexts.csv](./00-inventory/war-contexts.csv)

---

## 8. systemd 셋팅 (`systemd/tomcat-nsight.service`)

| 항목 | 값 |
|------|-----|
| Type | forking |
| User | tomcat |
| LimitNOFILE | 65535 |
| Restart | on-failure |
| Environment | JVM_ROUTE, HTTP_PORT, CLUSTER_*, CATALINA_* (노드별) |

---

## 9. 검증

```bash
./02-tomcat/scripts/check-tomcat.sh
# → /sv/actuator/health, /ui/actuator/health
$CATALINA_HOME/bin/catalina.sh configtest
```

| 점검 | 기대 |
|------|------|
| Sticky | `JSESSIONID=….tc0x` |
| Failover | 노드 중지 후 세션 유지 |
| Health | `/sv/actuator/health` UP |

---

## 10. 파일 매핑

| 셋팅 | 파일 |
|------|------|
| JVM | `02-tomcat/bin/setenv.sh` |
| Connector·Cluster | `02-tomcat/conf/server.xml` |
| Cookie | `02-tomcat/conf/context.xml` |
| 서비스 | `02-tomcat/systemd/tomcat-nsight.service` |
| 노드 CSV | `00-inventory/tomcat-nodes.csv` |
