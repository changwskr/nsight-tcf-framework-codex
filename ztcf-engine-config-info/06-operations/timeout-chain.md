# Timeout Chain 기준

SoT: [../10-operations-셋팅정보.md](../10-operations-셋팅정보.md)

| 계층 | 설정 | 권장값 |
|------|------|-------:|
| MyBatis | default-statement-timeout | **3초** |
| HikariCP | connection-timeout | **3초** |
| Spring | Transaction Timeout | **5초** |
| CruzAPIM | Connect / Read | **2초 / 5초** |
| Apache | ProxyTimeout / worker | **10초** |
| Client / WebTopSuite | Request Timeout | **15초** |
| L4 | Idle Timeout | **120초** |
| Session Idle | Tomcat / Spring | **60분** |
| L4 Sticky | Persistence | **70분** |
| Absolute Session | App Filter | **8시간** |

```text
DB(3) ≤ Hikari(3) ≤ TX(5) < Apache(10) < Client(15) < L4 Idle(120)
Session Idle(60m) < L4 Sticky(70m) ≤ Absolute(8h)
```

원칙: 세션은 길게 유지해도 개별 거래는 짧게 끊는다.
