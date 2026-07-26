# 운영 모니터링 기준

SoT: [../10-operations-셋팅정보.md](../10-operations-셋팅정보.md)

| 영역 | 지표 | Warning | Critical |
|---|---|---:|---:|
| Apache | 5xx Rate | 1% | 3% |
| Apache | Proxy Error | 발생 | 지속 발생 |
| Tomcat | Busy Thread | 70% | 85% |
| Tomcat | Current Sessions | 기준 초과 | 급증 |
| JVM | Heap Usage | 70% | 85% |
| JVM | Full GC | 발생 | 반복 |
| HikariCP | Active Connection | 70% | 85% |
| HikariCP | Pending Thread | 발생 | 지속 |
| MyBatis/DB | SQL p95 | 1초 | 3초 |
| Application | p95 Response | 3초 | 5초 |
| Session | Replication Error | 발생 | 지속 |

상관 키: `GUID`, `traceId`, `JSESSIONID` route, `serviceId`, `SQL Time`, `Pool Wait`
