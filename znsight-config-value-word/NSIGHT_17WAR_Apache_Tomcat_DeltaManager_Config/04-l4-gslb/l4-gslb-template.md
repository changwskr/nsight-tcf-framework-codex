# L4 / GSLB 설정 템플릿

## 1. GSLB

| 항목 | 권장값 |
|---|---:|
| DNS TTL | 30초 |
| Health Check Interval | 5초 |
| Health Check Timeout | 2초 |
| Fail Count | 3회 |
| 센터 장애 정책 | DR 센터 자동 전환 |

## 2. L4 Virtual Server

| 항목 | 권장값 |
|---|---:|
| VIP | nh.marketing.com HTTPS VIP |
| Pool Member | Apache 2대 이상 또는 Tomcat 직접 라우팅 시 Tomcat 노드 |
| Persistence | Cookie 또는 JSESSIONID 기반 |
| Sticky Timeout | 70분, Tomcat Session 60분보다 길게 |
| Idle Timeout | 120초 |
| Health Check URI | `/portal/actuator/health` 또는 `/apache-health` |

## 3. Timeout 정합성

```text
DB Query 2~3s < Spring TX 4~5s < Apache Proxy Read 10s < Client 15s < L4 Idle 120s
```
