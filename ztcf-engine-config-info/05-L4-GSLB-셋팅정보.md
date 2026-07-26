# L4 / GSLB 셋팅정보

> 읽는 순서: **05** · 인덱스: [03-아키텍처-셋팅정보.md](./03-아키텍처-셋팅정보.md)  
> 패키지: `ztcf-engine-config-info/04-l4-gslb/`  
> 대상: VIP → Apache Pool · Persistence(JSESSIONID) · Health  
> 안내: [04-l4-gslb/L4-GSLB-설정.md](./04-l4-gslb/L4-GSLB-설정.md)  
> 템플릿: [04-l4-gslb/l4-gslb-template.md](./04-l4-gslb/l4-gslb-template.md)

---

## 1. 역할

센터 앞단에서 HTTPS VIP를 Apache 풀로 분산하고, 세션 sticky·헬스체크·유휴 타임아웃을 정의한다.

```text
Client
  → GSLB / DNS
  → L4 VIP (HTTPS :443)
  → Apache Pool (노드별)
  → Tomcat Cluster
```

---

## 2. GSLB / DNS 셋팅

| 항목 | 권장값 | 비고 |
|------|--------|------|
| DNS TTL | **30초** | 장애 전환 민첩도 |
| 헬스 대상 | Apache VIP 또는 `/apache-health` | 장비별 정책 |
| Failover | Active-Active 또는 Active-Standby | 센터 정책 |

---

## 3. L4 셋팅

| 항목 | 권장값 | 비고 |
|------|--------|------|
| VIP | HTTPS 443 | 실 VIP로 교체 |
| Backend | Apache :443 (또는 :80→리다이렉트 후) | 템플릿 기준 Apache SSL 종료 |
| Health Interval | **5초** | |
| Health Timeout | **2초** | |
| Health Fail | **3회** | |
| Health URI | `/sv/actuator/health` 또는 `/apache-health` | Apache 경유 가능 시 |
| L4 Idle Timeout | **120초** | Timeout 체인 최상위 |
| Persistence | Cookie / **JSESSIONID** | Tomcat sticky와 정합 |
| Sticky Timeout | **70분** | Session Idle 60분보다 **길게** |

상세 템플릿: [04-l4-gslb/l4-gslb-template.md](./04-l4-gslb/l4-gslb-template.md)

---

## 4. Sticky 정합

```text
L4 Persistence (JSESSIONID, 70분)
    ↔
Apache stickysession=JSESSIONID route=tc0x
    ↔
Tomcat jvmRoute=tc0x
```

| 주의 | 내용 |
|------|------|
| Idle vs Sticky | L4 Sticky(70분) ≥ Session Idle(60분) |
| Idle vs Proxy | L4 Idle(120초) ≫ Apache Proxy(10초) |
| 센터 간 | L4/GSLB로 센터 전환 시 **재로그인** 전제(세션 미복제) |

---

## 5. Health 권장 URI

| 우선 | URI | 용도 |
|-----:|-----|------|
| 1 | `/apache-health` | L4가 Apache만 볼 때 |
| 2 | `/sv/actuator/health` | 앱 기동까지 확인 |
| 3 | `/ui/actuator/health` | UI WAR 확인 |

관리 URL(`/server-status`, `/balancer-manager`)은 L4 health에 쓰지 않는다.

---

## 6. Timeout 체인에서의 위치

| 계층 | 값 |
|------|---:|
| … | … |
| Apache Proxy | 10초 |
| Client Request | 15초 |
| **L4 Idle** | **120초** |
| Session Idle | 60분 |
| **L4 Sticky** | **70분** |
| Absolute Session | 8시간 |

→ [10-operations-셋팅정보.md](./10-operations-셋팅정보.md)

---

## 7. 배포 시 교체 항목

| 구분 | 템플릿 | 조치 |
|------|--------|------|
| VIP / 풀 멤버 | 예시 | 실 IP·포트 |
| Health URI | `/sv/…` 또는 `/apache-health` | 망 정책에 맞게 |
| Persistence 이름 | JSESSIONID | 앱 쿠키명과 동일 유지 |
| Sticky / Idle | 70분 / 120초 | 용량·정책 재산정 |

---

## 8. 파일 매핑

| 셋팅 | 파일 |
|------|------|
| 템플릿 | `04-l4-gslb/l4-gslb-template.md` |
| 설명 | `04-l4-gslb/L4-GSLB-설정.md` |
| Apache health | `01-apache/conf.d/30-status-health.conf` |
