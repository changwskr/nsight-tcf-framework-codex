# Operations 셋팅정보

> 읽는 순서: **10** · 인덱스: [03-아키텍처-셋팅정보.md](./03-아키텍처-셋팅정보.md)  
> 패키지: `ztcf-engine-config-info/06-operations/`  
> 대상: Timeout · 세션복제 · 보안 · 모니터링  
> 안내: [06-operations/운영-설정.md](./06-operations/운영-설정.md)

---

## 1. 역할

배포 후 운영 기준값(타임아웃 체인, sticky/복제, 보안, 알람 임계치)을 한곳에 정의한다.

원칙: **세션은 길게, 개별 거래는 짧게.**

---

## 2. Timeout 체인 셋팅

| 계층 | 설정 | 권장값 |
|------|------|-------:|
| MyBatis | default-statement-timeout | **3초** |
| HikariCP | connection-timeout | **3초** |
| Spring | transaction default-timeout | **5초** |
| CruzAPIM | connect / read | **2초 / 5초** |
| Apache | ProxyTimeout / ProxyPass timeout | **10초** |
| Client / WebTopSuite | Request Timeout | **15초** |
| L4 | Idle Timeout | **120초** |
| Session Idle | Tomcat / Spring | **60분** |
| Session Absolute | App Filter | **8시간** |
| L4 Sticky | Persistence | **70분** |

```text
DB(3) ≤ Hikari(3) ≤ TX(5) < CruzAPIM Read(5) < Apache(10) < Client(15) < L4 Idle(120)
Session Idle(60m) < L4 Sticky(70m) ≤ Absolute(8h)
```

상세: [06-operations/timeout-chain.md](./06-operations/timeout-chain.md)

---

## 3. 세션·복제 셋팅

| 정책 | 값 | 위치 |
|------|-----|------|
| Idle | 60분 | Spring / WAR web.xml |
| Absolute | 8시간 | `AbsoluteSessionTimeoutFilter` |
| Cookie | JSESSIONID HttpOnly Secure SameSite=Lax | yml / Apache headers |
| Sticky | Apache route ↔ jvmRoute | balancer / server.xml |
| Manager | DeltaManager | 센터 내부만 |
| 세션 크기 | 권장 ≤2KB, 상한 5KB | 운영 원칙 |
| 금지 | `/sv` 조회결과·대량 DTO 세션 저장 | |

### 체크리스트 (요약)

- [ ] core(+jwt) WAR `<distributable/>`
- [ ] 세션 객체 `Serializable`
- [ ] `jvmRoute` 유일 (tc01~03)
- [ ] Apache `route` ≡ `jvmRoute`
- [ ] 센터 간 복제 미적용 · 장애 시 재로그인
- [ ] 노드 중지 후 세션 유지 테스트

상세: [06-operations/session-replication-checklist.md](./06-operations/session-replication-checklist.md)

---

## 4. 보안 셋팅

| 항목 | 기준 |
|------|------|
| Cookie | HttpOnly / Secure / SameSite=Lax |
| TLS | TLS1.2+ (1.0/1.1 Off) |
| ServerTokens | Prod |
| 관리 URL | `/server-status`, `/balancer-manager` 사내망만 |
| 로그 | 주민·카드·비밀번호 원문 금지 |
| 시크릿 | DB·키는 Vault/환경변수 |

상세: [06-operations/security-checklist.md](./06-operations/security-checklist.md)

---

## 5. 모니터링 임계치 셋팅

| 지표 | Warning | Critical |
|------|--------:|---------:|
| Apache 5xx Rate | 1% | 3% |
| Tomcat Busy Thread | 70% | 85% |
| JVM Heap | 70% | 85% |
| Hikari Active | 70% | 85% |
| SQL p95 | 1초 | 3초 |
| App p95 | 3초 | 5초 |

### 상관 키

`GUID`, `traceId`, `JSESSIONID` route, `serviceId`, `SQL Time`, `Pool Wait`

상세: [06-operations/monitoring-metrics.md](./06-operations/monitoring-metrics.md)

---

## 6. 용량 기준 셋팅 (참고)

| 계층 | 가정 | 셋팅 |
|------|------|------|
| Tomcat | 8 vCPU / 32GB | maxThreads 500, Heap 12g |
| Apache | MPM Event | MaxRequestWorkers 512 |
| Hikari RDW | 일반 | max 30~40 (템플릿 40) |
| Hikari `/sv` | 조회 | 40~60 |
| Hikari APPLOG | 로그 | max 10 |

---

## 7. 파일 매핑

| 셋팅 | 파일 |
|------|------|
| Timeout | `06-operations/timeout-chain.md` |
| 세션복제 | `06-operations/session-replication-checklist.md` |
| 보안 | `06-operations/security-checklist.md` |
| 모니터링 | `06-operations/monitoring-metrics.md` |
| Tomcat | [07-tomcat-셋팅정보.md](./07-tomcat-셋팅정보.md) |
| Spring | [08-spring-boot-셋팅정보.md](./08-spring-boot-셋팅정보.md) |
| L4 | [05-L4-GSLB-셋팅정보.md](./05-L4-GSLB-셋팅정보.md) |
| Deploy | [09-deploy-셋팅정보.md](./09-deploy-셋팅정보.md) |
