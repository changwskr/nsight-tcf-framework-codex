# 23. Apache HTTP Server 환경 아키텍처

| 항목 | 내용 |
|------|------|
| 문서 번호 | 23 |
| 제목 | Apache Web Server Environment (Reverse Proxy) |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [16-deploy.md](16-deploy.md), [21-env-tomcat.md](21-env-tomcat.md), [18-fileupdownload.md](18-fileupdownload.md), [20-env-spring.md](20-env-spring.md) |
| 설정 샘플 | [tcf-cicd/prod/apache/nsight-marketing-routing.conf](../../tcf-cicd/prod/apache/nsight-marketing-routing.conf) |
| 대상 | 인프라·운영·보안 담당자 |

---

## 1. 개요

운영 환경에서 NSIGHT TCF는 일반적으로 **Apache HTTP Server(역방향 프록시)** 앞단 + **Tomcat(WAS)** 백엔드 구조를 사용한다.

| 계층 | 역할 | 로컬 대응 |
|------|------|-----------|
| **Apache** | 80/443 종단, SSL, 라우팅, 접근제어, 로드밸런싱 | (선택) 개발 검증 |
| **Tomcat** | Spring Boot WAR 19 context | `ztomcat` (:8080) |
| **Spring Boot WAR** | 온라인·OM·UI·배치 | [21-env-tomcat.md](21-env-tomcat.md) |

로컬 `ztomcat`은 Apache **없이** Tomcat 8080에 직접 접속한다.  
운영 연동 설계·검증 시 Apache 설정은 `tcf-cicd/prod/apache/` 샘플을 기준으로 한다.

---

## 2. 토폴로지

### 2.1 표준 (단일 Tomcat 게이트웨이)

```text
                    ┌─────────────────────────────────────┐
  Client / Channel  │  Apache HTTP Server                  │
  (브라우저·채널)    │  :443 (HTTPS) / :80 → redirect       │
                    │  SSL 종단, X-Forwarded-* 헤더        │
                    └──────────────────┬──────────────────┘
                                       │ ProxyPass /
                                       ▼
                    ┌─────────────────────────────────────┐
                    │  Tomcat :8080                        │
                    │  /ic … /mg  (업무 9)                 │
                    │  /om  /batch  /ui                    │
                    └─────────────────────────────────────┘
```

외부 URL 예:

```text
https://marketing.example.com/sv/online
https://marketing.example.com/ui/om/admin/login.html
https://marketing.example.com/om/ud/files/upload
```

Tomcat context path는 ztomcat과 **동일** ([16-deploy.md](16-deploy.md)).

### 2.2 확장 (Tomcat 클러스터)

```text
Apache (mod_proxy_balancer)
   ├─► Tomcat-1 :8080  (/sv, /cc, … 일부 context)
   ├─► Tomcat-2 :8080
   └─► Tomcat-3 :8080
```

- 업무별 WAR 분산·수평 확장 시 context 단위 `ProxyPass` 또는 balancer 멤버 분리
- **세션 고정**(sticky session) 또는 Spring Session JDBC(`tcf-om`) 공유 필요

### 2.3 bootRun 다중 포트 (비권장·개발 전용)

Apache가 `8086`, `8097` 등 **모듈별 포트**로 분기하는 구성은 가능하나, 운영 표준이 아니다.  
통합 게이트웨이는 **Tomcat 단일 8080 + context** 모델을 따른다 ([22-build-project.md](22-build-project.md)).

---

## 3. Context·URL 매핑

Apache는 경로를 **그대로** Tomcat에 전달한다 (`ProxyPass / http://backend:8080/`).

| Context | 백엔드 WAR | 대표 URL |
|---------|------------|----------|
| `/cc` … `/mg` | `{code}.war` | `POST /{code}/online` |
| `/om` | `om.war` (`tcf-om`) | `POST /om/online`, `/om/ud/files/*` |
| `/batch` | `batch.war` | `/batch/actuator/health`, 수집 API |
| `/ui` | `ui.war` | `/ui/om/admin/*`, `/ui/api/relay/*` |

### 3.1 tcf-ui Relay와 Apache

`application-dev.yml`·`application-prod.yml`에서 `nsight.tcf-ui.deployment-mode: tomcat`, `tomcat-gateway-url`은 **브라우저가 접속하는 외부 URL**과 일치해야 한다.

| 환경 | `tomcat-gateway-url` 예 |
|------|------------------------|
| ztomcat 직접 | `http://localhost:8080` |
| Apache HTTPS | `https://marketing.example.com` |

Relay가 호출하는 업무 API: `{gateway}/{code}/online`  
UD API: `{gateway}/om/ud/files/*` ([18-fileupdownload.md](18-fileupdownload.md))

---

## 4. 필수 Apache 모듈

| 모듈 | 용도 |
|------|------|
| `mod_proxy` | 역방향 프록시 |
| `mod_proxy_http` | HTTP 백엔드 연동 |
| `mod_headers` | `X-Forwarded-Proto`, `X-Forwarded-For` |
| `mod_rewrite` | HTTP→HTTPS 리다이렉트 (선택) |
| `mod_ssl` | HTTPS 종단 (운영) |
| `mod_remoteip` | 접근 로그·실 IP 복원 (권장) |
| `mod_proxy_balancer` | 다중 Tomcat (확장 시) |

확인:

```bash
httpd -M | egrep 'proxy|headers|ssl|remoteip'
```

---

## 5. 설정 파일 구조

```text
tcf-cicd/prod/apache/
└── nsight-marketing-routing.conf    # VirtualHost·ProxyPass 샘플 (Git)

/etc/httpd/  (RHEL) 또는 /etc/apache2/ (Debian)
├── conf/httpd.conf                  # Include deploy 샘플
├── conf.d/nsight-marketing.conf       # 운영 배포본 (환경별 복사·수정)
└── logs/
    ├── nsight-marketing-access.log
    └── nsight-marketing-error.log
```

| 파일 | 관리 |
|------|------|
| `tcf-cicd/prod/apache/*.conf` | **템플릿** — 버전 관리 |
| `/etc/httpd/conf.d/` | **운영** — 호스트·인증서·ACL 환경별 |

---

## 6. 핵심 설정 항목

### 6.1 Reverse Proxy (단일 Tomcat)

```apache
Define NSIGHT_TOMCAT_HOST 127.0.0.1
Define NSIGHT_TOMCAT_PORT 8080

ProxyPreserveHost On
ProxyRequests Off

ProxyPass        / http://${NSIGHT_TOMCAT_HOST}:${NSIGHT_TOMCAT_PORT}/
ProxyPassReverse / http://${NSIGHT_TOMCAT_HOST}:${NSIGHT_TOMCAT_PORT}/
```

- **`ProxyPreserveHost On`**: `Host` 헤더 유지 → 가상 호스트·쿠키 도메인 일치
- **`ProxyRequests Off`**: Forward Proxy 비활성 (보안)
- trailing slash: `ProxyPass /` ↔ Tomcat 루트 매핑 시 context path 보존

### 6.2 SSL 종단 + 리다이렉트

```apache
<VirtualHost *:80>
    RewriteEngine On
    RewriteRule ^ https://%{HTTP_HOST}%{REQUEST_URI} [R=301,L]
</VirtualHost>

<VirtualHost *:443>
    SSLEngine on
    # SSLCertificateFile / SSLCertificateKeyFile ...
    RequestHeader set X-Forwarded-Proto "https"
    RequestHeader set X-Forwarded-Port "443"
    # ProxyPass ...
</VirtualHost>
```

Spring·Tomcat은 `X-Forwarded-Proto`로 HTTPS 여부를 인지할 수 있다 (쿠키 `secure` 정책 등).

### 6.3 클라이언트 IP (`X-Forwarded-For`)

TCF `OnlineTransactionController`는 Header `clientIp`가 비어 있을 때:

1. `X-Forwarded-For` 첫 번째 홉
2. 없으면 `request.getRemoteAddr()`

```java
// tcf-web — resolveClientIp()
String forwardedFor = request.getHeader("X-Forwarded-For");
```

Apache에서 권장:

```apache
RemoteIPHeader X-Forwarded-For
RemoteIPInternalProxy 10.0.0.0/8 172.16.0.0/12 192.168.0.0/16 127.0.0.0/8

RequestHeader append X-Forwarded-For %{REMOTE_ADDR}s
```

→ 거래로그·감사로그(`clientIp`)에 **실 클라이언트 IP** 반영.

### 6.4 업로드·타임아웃 (UD·온라인)

| 항목 | 권장 | 근거 |
|------|------|------|
| `LimitRequestBody` | `52428800` (50MB) | `tcf-om` multipart 상한 |
| `ProxyTimeout` | `60` (초) | 온라인 5초 + 프록시 여유 |
| `ProxyPass` 배치 Location | `300`+ | `OM.Batch.execute` 등 장거래 |

UD 다운로드는 바이너리 스트림 — `ProxyPass` 기본 버퍼링으로 충분하나, 대용량 시 `flushpackets=on` 검토.

### 6.5 Actuator 접근 제한

```apache
<LocationMatch "^/(cc|ic|...|ui)/actuator">
    Require ip 10.0.0.0/8 172.16.0.0/12 192.168.0.0/16 127.0.0.1
</LocationMatch>
```

`/actuator/health`는 모니터링망에서만 허용하고, 외부 인터넷에는 차단한다.

---

## 7. Context별 분리 라우팅 (확장 예)

단일 Tomcat 대신 **업무군별 WAS**로 분리할 때:

```apache
ProxyPass        /sv   http://sv-was.internal:8080/sv
ProxyPassReverse /sv   http://sv-was.internal:8080/sv
ProxyPass        /om   http://om-was.internal:8080/om
ProxyPassReverse /om   http://om-was.internal:8080/om
# …
```

Balancer 예:

```apache
<Proxy balancer://nsight-cluster>
    BalancerMember http://tomcat1:8080 route=tomcat1
    BalancerMember http://tomcat2:8080 route=tomcat2
    ProxySet stickysession=JSESSIONID|jsessionid
</Proxy>
ProxyPass /sv balancer://nsight-cluster/sv
```

OM Spring Session JDBC 사용 시 sticky 없이도 OM 포털 세션 공유 가능 ([10-session.md](10-session.md)).

---

## 8. 인코딩·HTTP 메서드

| 항목 | 설정 |
|------|------|
| 요청 charset | Tomcat `URIEncoding=UTF-8` ([21-env-tomcat.md](21-env-tomcat.md)) — Apache는 body 그대로 전달 |
| 온라인 API | **POST only** `application/json` |
| UD 업로드 | `multipart/form-data` — `mod_proxy` 기본 지원 |

Apache `AddDefaultCharset Off` — JSON UTF-8을 Tomcat이 처리하도록 불필요한 charset 헤더 추가 방지.

---

## 9. 배포·연동 절차

### 9.1 로컬 Apache + ztomcat 검증

```text
1. ztomcat/install-tomcat + deploy-wars all + start
2. verify-deploy → 19/19 health (localhost:8080)
3. Apache에 tcf-cicd/prod/apache/nsight-marketing-routing.conf Include
4. httpd -t && systemctl reload httpd
5. curl -k https://localhost/sv/actuator/health
6. curl -k -X POST https://localhost/sv/online -H "Content-Type: application/json" -d @sample.json
```

개발용 HTTP-only VirtualHost는 샘플 conf 하단 주석 참고.

### 9.2 운영 반영 체크리스트

- [ ] Tomcat 8080(또는 내부 LB) 기동·19 context health
- [ ] Apache `NSIGHT_TOMCAT_HOST` 방화벽 허용
- [ ] SSL 인증서·체인 설치
- [ ] `X-Forwarded-For` / `RemoteIP` 설정
- [ ] Actuator·Tomcat Manager 외부 차단
- [ ] `tcf-ui` `tomcat-gateway-url` = 공개 URL
- [ ] `tcf-batch` `application-prod.yml` 수집 `base-url` = 내부·공개 게이트웨이

---

## 10. 로그·모니터링

| 로그 | 위치 | 내용 |
|------|------|------|
| Apache access | `CustomLog` (샘플: `nsight-marketing-access.log`) | 클라이언트 요청 |
| Apache error | `ErrorLog` | 프록시 연결 실패·SSL |
| Tomcat access | `localhost_access_log.*` | context별 HTTP |
| Spring | `${CATALINA_BASE}/logs/nsight-*.log` | 거래·앱 로그 |

프록시 502/503 시: Tomcat 기동 여부 → `ProxyPass` URL → 방화벽 순으로 확인.

---

## 11. ztomcat / bootRun / Apache 비교

| 항목 | bootRun | ztomcat | Apache + Tomcat |
|------|---------|---------|-----------------|
| 클라이언트 포트 | 8081~8099 | 8080 | **443** (표준) |
| SSL | 없음 | 없음 | Apache 종단 |
| Context | 포트 분리 | path 분리 | path 분리 (동일) |
| 실 IP | 직접 | 직접 | `X-Forwarded-For` |
| 용도 | 개발 | 통합 테스트 | **운영** |

---

## 12. 트러블슈팅

| 증상 | 원인 | 조치 |
|------|------|------|
| 502 Bad Gateway | Tomcat 미기동 | `8080` health, `catalina.log` |
| `/sv/online` 404 | context 미배포 | `deploy-wars`, WAR명 확인 |
| `clientIp`가 Apache IP | `X-Forwarded-For` 미설정 | `RemoteIP` / `RequestHeader` |
| UI Relay 실패 | gateway URL 불일치 | `tomcat-gateway-url` = Apache URL |
| 업로드 413 | `LimitRequestBody` | 50MB 이상으로 조정 |
| 한글 URL 깨짐 | Tomcat encoding | [21-env-tomcat.md](21-env-tomcat.md) `URIEncoding` |
| 세션 끊김 | 도메인·path 불일치 | `ProxyPreserveHost`, 쿠키 `path` |
| HTTPS mixed content | UI가 http 호출 | `X-Forwarded-Proto`, UI gateway https |

---

## 13. 보안 권장사항

1. **Tomcat 8080** — 외부 직접 노출 금지, Apache·내부망만 허용
2. **Actuator / manager** — IP 화이트리스트
3. **TLS 1.2+** — 구형 cipher 비활성
4. **Request smuggling** — Apache·Tomcat 최신 패치 유지
5. **헤더 신뢰** — `X-Forwarded-For`는 **신뢰 프록시**에서만 수락 (`RemoteIPInternalProxy`)

---

## 14. 참고 파일

| # | 경로 |
|---|------|
| 1 | [tcf-cicd/prod/apache/nsight-marketing-routing.conf](../../tcf-cicd/prod/apache/nsight-marketing-routing.conf) |
| 2 | [ztomcat/README.md](../../ztomcat/README.md) |
| 3 | [21-env-tomcat.md](21-env-tomcat.md) |
| 4 | [16-deploy.md](16-deploy.md) |
| 5 | `tcf-web/.../OnlineTransactionController.java` — `X-Forwarded-For` |
| 6 | `tcf-ui/.../application-prod.yml` — gateway URL |
| 7 | `tcf-batch/.../application-prod.yml` — 수집 타겟 URL |
