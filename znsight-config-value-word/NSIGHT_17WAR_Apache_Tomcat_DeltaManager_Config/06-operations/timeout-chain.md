# Timeout Chain 기준

| 계층 | 설정 | 권장값 |
|---|---|---:|
| MyBatis | default-statement-timeout | 3초 |
| HikariCP | connection-timeout | 3초 |
| Spring | Transaction Timeout | 5초 |
| CruzAPIM | Connect / Read | 2초 / 5초 |
| Apache | ProxyTimeout / worker timeout | 10초 |
| WebTopSuite | Request Timeout | 15초 |
| L4 | Idle Timeout | 120초 |
| Spring/Tomcat | Session Idle Timeout | 60분 |
| Application | Absolute Session Timeout | 8시간 |

핵심 원칙: 세션은 길게 유지해도 개별 거래 트랜잭션은 짧게 끊는다.
