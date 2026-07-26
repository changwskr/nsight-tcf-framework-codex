# Apache 셋팅정보

> 읽는 순서: **06** · 인덱스: [03-아키텍처-셋팅정보.md](./03-아키텍처-셋팅정보.md)  
> 패키지: `ztcf-engine-config-info/01-apache/`  
> SoT: `httpd.conf`, `conf.d/*`, `conf.modules.d/*`  
> 설명(얇은 안내): [01-apache/아파치-설정.md](./01-apache/아파치-설정.md)

---

## 1. 역할

`nh.marketing.com` SSL 종료 · Reverse Proxy · Sticky(`JSESSIONID`) · Context 라우팅.

```text
Client :443 → Apache → balancer://nsight_tomcat_cluster → Tomcat :8080
```

---

## 2. httpd.conf 셋팅

| 항목 | 값 |
|------|-----|
| Listen | 80, 443 |
| ServerName | nh.marketing.com:443 |
| Timeout | **15** |
| ProxyTimeout | **10** |
| KeepAlive | On / Max 1000 / Timeout 5 |
| ServerTokens / Signature | Prod / Off |
| TraceEnable | Off |

---

## 3. MPM Event (`00-mpm-event.conf`)

| 항목 | 값 |
|------|----:|
| MaxRequestWorkers | 512 |
| ThreadsPerChild | 32 |
| MinSpareThreads | 128 |
| MaxSpareThreads | 512 |
| MaxConnectionsPerChild | 10000 |

---

## 4. SSL VH (`10-ssl-virtualhost.conf`)

| 항목 | 값 |
|------|-----|
| :80 | HTTPS 301 |
| :443 | SSLEngine on |
| TLS | 1.2+ (SSLv2/3, TLSv1/1.1 제외) |
| Forward | X-Forwarded-Proto/Port/Host |
| HSTS | max-age=31536000; includeSubDomains |
| Include | `20-proxy-balancer-tcf.conf`, `40-security-headers.conf` |

인증서: `01-apache/ssl/` placeholder 교체.

---

## 5. Balancer / Proxy (`20-proxy-balancer-tcf.conf`)

| 항목 | 값 |
|------|-----|
| Balancer | `balancer://nsight_tomcat_cluster` |
| Members (예시) | 10.10.20.11~13:8080 · route=tc01~03 |
| stickysession | `JSESSIONID\|jsessionid` |
| lbmethod | byrequests |
| scolonpathdelim | On |
| nofailover | Off |
| Member timeout / conn | 10s / 3s |
| ProxyPass timeout | 10s |
| `/` | **`/ui/`** 302 |
| ProxyPass | `/ic/` … `/ui/` `/jwt/` |
| balancer-manager | 사내망 IP만 |

**필수:** `route=` ≡ Tomcat `jvmRoute` ([04-inventory-셋팅정보.md](./04-inventory-셋팅정보.md))

---

## 6. Status / Security / Logging

| 파일 | 셋팅 |
|------|------|
| `30-status-health.conf` | `/server-status`(제한), `/apache-health` |
| `40-security-headers.conf` | Server unset, Cookie 보강, LimitRequestBody 100MB |
| `50-logging.conf` | NSIGHT access/error 포맷 |

---

## 7. Timeout 위치

| 설정 | 값 |
|------|---:|
| Timeout | 15s |
| ProxyTimeout | 10s |
| BalancerMember / ProxyPass | 10s |

→ [10-operations-셋팅정보.md](./10-operations-셋팅정보.md)

---

## 8. 검증

```bash
apachectl -t
./01-apache/scripts/check-apache.sh   # /ui/, /sv/actuator/health
```
