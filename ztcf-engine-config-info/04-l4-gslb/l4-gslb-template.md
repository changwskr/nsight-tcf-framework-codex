# L4 / GSLB 템플릿 (장비 반영용)

SoT·설명: [../05-L4-GSLB-셋팅정보.md](../05-L4-GSLB-셋팅정보.md)

## 1. GSLB

| 항목 | 권장값 |
|------|-------:|
| DNS TTL | 30초 |
| Health Interval | 5초 |
| Health Timeout | 2초 |
| Fail Count | 3회 |
| 센터 장애 | DR 전환(정책) |

## 2. L4 Virtual Server

| 항목 | 권장값 |
|------|--------|
| VIP | nh.marketing.com HTTPS |
| Pool | Apache 2대 이상 |
| Persistence | Cookie / JSESSIONID |
| Sticky Timeout | **70분** |
| Idle Timeout | **120초** |
| Health URI | `/sv/actuator/health` 또는 `/apache-health` |

## 3. Timeout 정합

```text
MyBatis(3s) ≤ Hikari(3s) ≤ TX(5s) < Apache(10s) < Client(15s) < L4 Idle(120s)
Session Idle(60m) < L4 Sticky(70m)
```
