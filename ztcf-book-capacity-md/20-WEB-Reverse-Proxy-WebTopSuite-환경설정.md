# 20. WEB·Reverse Proxy·WebTopSuite 환경설정

> 제4부. 솔루션별 환경설정 가이드

## NSIGHT 1차 표준 전제

| 항목 | 기준값 |
|------|--------|
| 지점 수 | 3,600 |
| 지점당 사용자 | 6 |
| 전체 사용자 | 21,600 |
| 설계 세션 | 26,000~28,000 |
| TPS | 360 / 720 / 1,080 |
| 목표 응답 | p95 3초 이하 |
| 기준 VM | 8 vCPU / 32GB |
| VM당 TPS | 250 (보수) |
| AP 구조 | 2센터 Active-Active |

> 출처: `znsight-capacity-word` · [13단계 요약](./zNSIGHT-용량산정-전체-흐름.md)


**Apache/Proxy** — Worker, ProxyTimeout, KeepAlive, GUID Header.

## 원문 기반 본문

NSIGHT 마케팅플랫폼Apache 설정 가이드WebTopSuite · L4 · Apache HTTP Server · Tomcat 연계 기준문서 버전: v1.0작성 기준일: 2026-05-29적용 대상: NSIGHT 마케팅플랫폼 / Single View / 정보계 온라인 AP핵심 정의: 본 문서는 Apache HTTP Server를 단순 정적 WEB 서버가 아니라, L4와 Tomcat 사이에서 Reverse Proxy, SSL 종료, 접속 유지, 보안 헤더, 로그 추적, 운영 관측성을 담당하는 경계 계층으로 정의한다.

문서 개정 이력

버전일자작성/변경 내용비고v1.02026-05-29NSIGHT Apache 설정 기준 최초 작성환경설정·타임아웃·6000지점 기준 반영

목차

## 1. 문서 목적과 적용 범위

## 2. NSIGHT Apache 아키텍처 위치

## 3. 용량 및 트래픽 산정 전제

## 4. Apache 설정 원칙

## 5. 필수 모듈 구성

## 6. 설정 파일 구조

## 7. Global / Core 설정

## 8. MPM Event 설정

## 9. VirtualHost / SSL 설정

## 10. Reverse Proxy 설정

## 11. Timeout / KeepAlive 설정

## 12. Header / Client IP / GUID 전달

## 13. Access Log / Error Log 설정

## 14. 보안 설정

## 15. 정적 리소스 / 압축 / 캐시

## 16. Health Check / mod_status17. 배포 및 운영 점검 절차

## 18. 성능 테스트 검증 시나리오

## 19. 장애 대응 및 우회 기준

## 20. 최종 체크리스트

## 21. 부록: 전체 예시 설정 파일

## 1. 문서 목적과 적용 범위

본 문서는 농협상호금융 NSIGHT 마케팅플랫폼에서 Apache HTTP Server를 Reverse Proxy 및 WEB 경계 계층으로 사용할 때 필요한 설정 기준, 실제 설정 위치, 예시 설정, 운영 검증 절차를 정의한다.

구분내용문서 목적Apache 설정을 표준화하여 WebTopSuite 요청, L4 분산, Tomcat WAS 처리, Spring Boot 거래 처리, DB Timeout이 일관되게 동작하도록 한다.

적용 범위Apache HTTP Server 2.4.x, SSL/TLS, Reverse Proxy, Proxy Balancer, Access/Error Log, 보안 Header, Health Check, mod_status비적용 범위Tomcat 내부 Thread/DB Pool/JVM 튜닝 상세값은 별도 Tomcat·Spring·Hikari 가이드에서 관리한다. 단, Apache와 정합성이 필요한 항목은 본 문서에 포함한다.

설계 기준실사용 동시 요청자 기준, 600 TPS 일반 / 1,200 TPS 피크 / 1,800 TPS 스트레스 기준, 2센터 Active-Active 운영을 전제로 한다.

설정 원칙: Apache 설정의 핵심은 값을 크게 잡는 것이 아니라, WebTopSuite Timeout, Apache Timeout, L4 Idle/Sticky, Tomcat Thread, Spring Transaction, DB Query Timeout의 순서를 맞추는 것이다.2. NSIGHT Apache 아키텍처 위치Apache는 채널과 AP 사이의 경계 지점에서 HTTPS 종료, Reverse Proxy, 요청 Header 보강, 접근로그 기록, 장애 시 우회 정책을 담당한다.[WebTopSuite 단말]
↓ HTTPS / JSON 전문[GSLB / L4]
↓ 센터 및 Apache 노드 분산[Apache HTTP Server]   - SSL/TLS 종료 또는 TLS Passthrough 보조   - Reverse Proxy / ProxyPass   - X-Forwarded-* / X-GUID Header 전달   - Access/Error Log / mod_status   - 보안 Header / 요청 제한
↓[Tomcat / Spring Boot AP]
↓[RDW / CruzAPIM / Kafka / ETL / ADW]영역역할Apache 관련 설정WebTopSuite사용자 요청 발생, Timeout 체감, GUID/RequestId 보관Client Request Timeout, Retry Count, X-GUID 전달L4/GSLB센터 선택, VIP 분산, Sticky, Health CheckApache 노드 Health Check URL, Idle Timeout 정합성ApacheSSL, Proxy, Header, Log, Security, Monitoringhttpd.conf, ssl.conf, vhost.conf, proxy.conf, status.confTomcat업무 요청 처리, 세션, WAS ThreadProxy Timeout보다 짧은 Transaction/SQL Timeout 연계Spring Boot업무 트랜잭션, 오류처리, GUID/MDCX-GUID, X-Forwarded-For, X-Request-Id 수신

## 3. 용량 및 트래픽 산정 전제항목기준값Apache 설정 영향지점 수6,000개전국 단위 동시 접속 및 KeepAlive 연결 수 고려지점당 사용자6명전체 사용자 36,000명 기준전체 사용자36,000명세션 설계와 접속 유지 규모 기준여유율 포함 세션43,000~47,000 세션L4 Sticky, Apache KeepAlive, 로그량 산정 기준일반 부하동시 요청자 1,800명 / 600 TPSApache 기본 운영 기준피크 부하동시 요청자 3,600명 / 1,200 TPS피크 설계 기준스트레스동시 요청자 5,400명 / 1,800 TPS성능시험 및 한계 검증 기준목표 응답시간p95 3초 이하Apache Proxy Read/Timeout은 업무 Timeout보다 길고 사용자 Timeout보다 짧게 설정산정 기준: 전체 사용자 36,000명을 TPS로 직접 환산하지 않는다. 세션 수는 로그인 유지 규모이고, Apache Worker/Proxy/Timeout은 실사용 동시 요청자와 연결 수를 기준으로 산정한다.4. Apache 설정 원칙원칙설명적용 설정Reverse Proxy 전용화Apache를 Forward Proxy로 열지 않는다.

ProxyRequests OffTimeout 계층 정합성DB/Transaction보다 길고 Client/L4보다 짧은 WEB 대기시간을 둔다.

ProxyTimeout, Timeout, ProxyPass timeout접속 재사용전국 지점 단말의 반복 요청에 TCP 재사용을 적용한다.

KeepAlive On, MaxKeepAliveRequests장애 전파 차단Tomcat 지연이 Apache와 L4 전체 지연으로 번지지 않도록 한다.connectiontimeout, timeout, retry, failonstatus관측성 내재화GUID, Trace, Client IP, 처리시간을 로그에 남긴다.

LogFormat, CustomLog, mod_unique_id보안 기본값 강화Server 정보 노출, TRACE, 취약 Header를 차단한다.

ServerTokens, TraceEnable, Headers운영 검증 가능성설정값은 적용 후 상태, 로그, 부하 테스트로 검증한다.apachectl configtest, mod_status, access_log 분석

## 5. 필수 모듈 구성모듈필수 여부역할비고mpm_event필수KeepAlive 연결과 요청 처리 효율화대량 동시 연결 환경 권장mod_ssl필수HTTPS/TLS 처리TLS 종료 위치가 Apache인 경우 필수mod_proxy필수Proxy 기본 기능Forward Proxy는 금지mod_proxy_http필수HTTP/HTTPS Backend ProxyTomcat HTTP Connector 연계mod_proxy_balancer선택Apache 자체 부하분산L4가 분산하면 선택mod_lbmethod_byrequests선택요청 수 기반 부하분산Balancer 사용 시mod_headers필수X-Forwarded, 보안 Header 설정GUID/Proto/Client IP 전달mod_remoteip권장실제 Client IP 복원L4/Proxy Header 신뢰 범위 제한 필요mod_log_config필수Access Log 포맷 정의GUID, 시간, 상태코드 기록mod_unique_id권장요청 ID 생성GUID 미전달 시 보조 IDmod_status운영 필수Apache 상태 확인관리망 IP만 허용mod_deflate 또는 mod_brotli선택응답 압축JSON/정적 리소스에 제한 적용# RHEL 계열 예시httpd -M | egrep 'ssl|proxy|headers|remoteip|status|unique_id|mpm_event'# Debian/Ubuntu 계열 예시a2enmod ssl proxy proxy_http proxy_balancer lbmethod_byrequests headers remoteip status unique_idsystemctl reload apache26. 설정 파일 구조운영 환경에서는 모든 설정을 httpd.conf 한 파일에 직접 넣지 않고, 역할별 include 파일로 분리한다.

파일관리 내용변경 주체httpd.conf기본 ServerRoot, Listen, Include, Global 설정WEB/인프라 운영conf.modules.d/*.conf모듈 Load 설정WEB/인프라 운영conf.d/00-global.confServerTokens, Timeout, KeepAlive 등 공통 설정아키텍처/인프라conf.d/10-mpm-event.confMPM Event Worker 설정인프라/성능 담당conf.d/20-ssl.confSSLProtocol, Cipher, 인증서 경로보안/인프라conf.d/30-proxy-nsight.confProxyPass, BalancerMember, TimeoutWEB/AP 공동conf.d/40-headers.conf보안 Header, X-Forwarded Header보안/WEBconf.d/50-logging.confLogFormat, CustomLog, ErrorLog운영/관측성conf.d/60-status.confmod_status 접근통제운영/보안

## 7. Global / Core 설정설정 항목권장값설정 위치설명ServerTokensProd00-global.conf응답 Header의 서버 상세 버전 노출 최소화ServerSignatureOff00-global.conf오류 페이지 서버 정보 노출 차단TraceEnableOff00-global.confHTTP TRACE Method 비활성화Timeout1500-global.conf기본 네트워크 I/O 대기시간. Proxy 세부 Timeout과 정합 필요HostnameLookupsOff00-global.confDNS 역조회로 인한 로그 지연 방지KeepAliveOn00-global.conf연결 재사용 활성화MaxKeepAliveRequests100000-global.conf연결당 요청 수 제한KeepAliveTimeout60~120초00-global.confWebTopSuite/L4 Idle 정책과 정합. 기본 120초 적용 가능LimitRequestBody업무별 제한VirtualHost/Location대용량 업로드 또는 비정상 요청 제한# conf.d/00-global.confServerTokens ProdServerSignature OffTraceEnable OffHostnameLookups OffTimeout 15KeepAlive OnMaxKeepAliveRequests 1000KeepAliveTimeout 120FileETag None

> **주의**: KeepAliveTimeout을 너무 길게 잡으면 유휴 연결이 오래 남을 수 있다. NSIGHT는 WebTopSuite 단말 특성과 L4 Idle 120초 기준을 고려하여 60~120초 범위에서 검증 후 확정한다.8. MPM Event 설정Apache가 Tomcat 앞단에서 대량 동시 연결과 KeepAlive를 처리하려면 event MPM을 기준으로 설정한다. Worker 수는 Tomcat maxThreads와 1:1로 맞추는 값이 아니라, Apache가 처리하는 프론트 연결 및 Proxy 요청을 기준으로 별도 산정한다.

구분일반 기준피크 기준스트레스 기준비고목표 TPS600 TPS1,200 TPS1,800 TPS전체 서비스 기준Apache MaxRequestWorkers1,024~2,0482,048~4,0964,096 이상 검토노드 수와 메모리 기준 조정ThreadsPerChild64~128128128~256운영 표준에 맞춤ServerLimit163232 이상MaxRequestWorkers / ThreadsPerChild 이상MaxConnectionsPerChild10,000~50,00020,000~50,00050,000메모리 누수 예방 목적# conf.d/10-mpm-event.conf<IfModule mpm_event_module>    StartServers             4    ServerLimit             32    ThreadsPerChild        128    MaxRequestWorkers     4096    MinSpareThreads        256    MaxSpareThreads       1024    MaxConnectionsPerChild 50000</IfModule>성능 원칙: MaxRequestWorkers는 크게 잡는 것이 목적이 아니다. 요청이 Apache에 과도하게 쌓이면 Tomcat/DB 지연을 숨기는 큐가 되므로, Apache 대기열·Tomcat Thread·DB Pool Wait를 함께 모니터링해야 한다.9. VirtualHost / SSL 설정항목권장값설명SSLProtocolTLSv

### 1.2 TLSv1.3구버전 TLS 비활성화SSLCipherSuite운영 보안 기준 적용기관 보안 가이드 기준으로 관리SSLHonorCipherOrderOn서버 Cipher 우선순위 적용HSTS선택 적용운영 도메인 확정 후 적용. 테스트 환경은 신중ProxyPreserveHostOn원 Host Header 유지UseCanonicalNameOffProxy 환경에서 Host 처리 단순화# conf.d/20-ssl.conf / conf.d/30-vhost.conf<VirtualHost *:443>    ServerName nsight-marketing.nh.local    SSLEngine on    SSLProtocol -all +TLSv1.2 +TLSv1.3    SSLCertificateFile      /etc/pki/tls/certs/nsight.crt    SSLCertificateKeyFile   /etc/pki/tls/private/nsight.key    SSLCertificateChainFile /etc/pki/tls/certs/chain.crt    ProxyPreserveHost On    UseCanonicalName Off    ErrorLog  logs/nsight_ssl_error.log    CustomLog logs/nsight_ssl_access.log nsight_json    IncludeOptional conf.d/proxy/nsight-proxy.conf</VirtualHost>10. Reverse Proxy 설정Apache Reverse Proxy 구성은 “L4가 Tomcat을 분산하는 방식”과 “Apache가 Tomcat을 직접 분산하는 방식”을 구분해야 한다. 중복 Sticky와 중복 Health Check는 장애 판단을 어렵게 만들 수 있다.

구성 방식권장 상황설정 방식주의사항L4 분산 후 Apache 단일 ProxyL4가 Tomcat Cluster를 이미 분산하는 경우ProxyPass를 Tomcat L4 VIP 또는 AP VIP로 지정Apache Balancer 미사용Apache Balancer 직접 분산Apache가 여러 Tomcat을 직접 바라보는 경우balancer:// 구성, BalancerMember 사용Sticky 정책과 route 설계 필요센터별 Apache 분리AP Active-Active 센터 구조센터별 Apache → 센터 내부 Tomcat센터 간 세션 복제 미적용 시 센터 장애는 재로그인

### 10.1 단일 Upstream Proxy 예시# conf.d/proxy/nsight-proxy.confProxyRequests OffProxyPreserveHost OnProxyTimeout 10ProxyPass        /marketing/ http://10.10.20.100:8080/marketing/ connectiontimeout=3 timeout=10 retry=5ProxyPassReverse /marketing/ http://10.10.20.100:8080/marketing/ProxyPass        /singleview/ http://10.10.20.110:8080/singleview/ connectiontimeout=3 timeout=10 retry=5ProxyPassReverse /singleview/ http://10.10.20.110:8080/singleview/

### 10.2 Apache Balancer 예시# Apache가 Tomcat 노드를 직접 분산하는 경우<Proxy "balancer://marketing_ap_cluster">    BalancerMember "http://10.10.10.11:8080" route=ap01 connectiontimeout=3 timeout=10 retry=5 keepalive=On    BalancerMember "http://10.10.10.12:8080" route=ap02 connectiontimeout=3 timeout=10 retry=5 keepalive=On    BalancerMember "http://10.10.10.13:8080" route=ap03 connectiontimeout=3 timeout=10 retry=5 keepalive=On    ProxySet lbmethod=byrequests stickysession=JSESSIONID|jsessionid scolonpathdelim=On</Proxy>ProxyPass        /marketing/ balancer://marketing_ap_cluster/marketing/ acquire=3000 timeout=10ProxyPassReverse /marketing/ balancer://marketing_ap_cluster/marketing/아키텍처 결정: Tomcat DeltaManager와 L4 Sticky를 사용하는 구조라면 Apache Balancer Sticky를 추가로 켤지 반드시 결정해야 한다. Sticky가 L4와 Apache 양쪽에 있으면 장애 시 세션 이동 경로가 복잡해진다.11. Timeout / KeepAlive 설정계층설정 항목권장값설명

WebTopSuiteRequest Timeout15초사용자 체감 최종 대기시간ApacheProxy connectiontimeout3초Tomcat 연결 실패 판단ApacheProxy timeout10초Tomcat 응답 대기 한도ApacheProxyTimeout10초Proxy 공통 대기시간ApacheTimeout15초Apache 기본 I/O 대기시간ApacheKeepAliveTimeout60~120초접속 재사용 유지L4Idle Timeout120초Apache KeepAlive와 정합SpringTransaction Timeout4~5초온라인 거래 전체 한도DB/MyBatisStatement Timeout2~3초SQL 실행 한도# Timeout 정합 예시# DB Query Timeout        : 2~3초# Spring Transaction      : 4~5초# Apache Proxy timeout    : 10초# WebTopSuite Request     : 15초# L4 Idle Timeout         : 120초ProxyTimeout 10Timeout 15KeepAlive OnKeepAliveTimeout 120MaxKeepAliveRequests 1000ProxyPass /marketing/ http://10.10.20.100:8080/marketing/ connectiontimeout=3 timeout=10 retry=512. Header / Client IP / GUID 전달NSIGHT는 모든 거래를 GUID와 TraceId로 추적해야 하므로, Apache는 해당 Header를 제거하지 않고 Tomcat으로 전달해야 한다. 또한 실제 Client IP는 L4/Proxy 환경에서 X-Forwarded-For 또는 RemoteIP 기준으로 복원한다.

Header설정 목적예시X-GUIDNSIGHT 거래 추적 IDWebTopSuite 또는 AP에서 생성한 GUID 전달X-Request-IDApache 보조 요청 IDmod_unique_id 값 또는 L4 요청 IDX-Forwarded-For실제 사용자 IP 전달L4/Proxy 경유 원본 IP 추적X-Forwarded-Proto원 요청 Scheme 전달httpsX-Forwarded-Port원 요청 Port 전달443X-Forwarded-Host원 Host 전달nsight-marketing.nh.localX-Center-ID센터 식별DC1 또는 DC2X-Proxy-IDApache 노드 식별APACHE-WEB-01# conf.d/40-headers.confRequestHeader set X-Forwarded-Proto "https"RequestHeader set X-Forwarded-Port  "443"RequestHeader set X-Proxy-ID        "APACHE-WEB-01"RequestHeader set X-Center-ID       "DC1"# L4가 X-Forwarded-For를 넣는 경우 실제 Client IP 복원RemoteIPHeader X-Forwarded-ForRemoteIPInternalProxy 10.10.0.0/16RemoteIPInternalProxy 10.20.0.0/16# 보안 HeaderHeader always set X-Content-Type-Options "nosniff"Header always set X-Frame-Options "SAMEORIGIN"Header always set Referrer-Policy "strict-origin-when-cross-origin"Header always set X-XSS-Protection "0"13. Access Log / Error Log 설정Apache Access Log는 단순 접속 기록이 아니라 채널–WEB–AP–DB 거래 흐름을 연결하는 첫 번째 관측성 데이터다. GUID, 사용자/지점 식별 Header, 응답시간, Backend 처리 여부를 기록해야 한다.

로그 항목Apache 포맷의미Client IP%aRemoteIP 적용 후 실제 Client IP요청시간%t요청 시각요청라인%rMethod URI Protocol상태코드%>s최종 HTTP 응답코드응답크기%b응답 바이트처리시간%D마이크로초 단위 처리시간GUID%{X-GUID}i거래 추적 IDRequestId%{X-Request-ID}i요청 IDUser Agent%{User-Agent}iWebTopSuite/브라우저 정보Forwarded For%{X-Forwarded-For}i전달된 원 IP 목록# conf.d/50-logging.confLogFormat "{ \"time\":\"%{%Y-%m-%dT%H:%M:%S%z}t\", \"clientIp\":\"%a\", \"xff\":\"%{X-Forwarded-For}i\", \"guid\":\"%{X-GUID}i\", \"requestId\":\"%{X-Request-ID}i\", \"methodUri\":\"%r\", \"status\":%>s, \"bytes\":%b, \"durationUs\":%D, \"referer\":\"%{Referer}i\", \"userAgent\":\"%{User-Agent}i\" }" nsight_jsonCustomLog logs/nsight_access.log nsight_jsonErrorLog  logs/nsight_error.logLogLevel warn proxy:info ssl:warn운영 원칙: 운영 중 일시적 장애 분석이 필요할 때만 proxy:debug, rewrite:trace 수준을 제한적으로 올리고, 원인 분석 후 즉시 원복한다. Debug 로그를 상시 활성화하면 로그 I/O와 개인정보 노출 위험이 커진다.

## 14. 보안 설정보안 항목권장 설정설명Forward Proxy 차단ProxyRequests OffOpen Proxy 위험 제거서버정보 노출 차단ServerTokens Prod / ServerSignature Off버전 정보 노출 최소화TRACE 차단TraceEnable OffCross Site Tracing 위험 완화관리 URL 접근통제Require ip 관리망server-status 등 관리 기능 보호Request Body 제한LimitRequestBody비정상 대용량 요청 차단Header 보안X-Content-Type-Options 등기본 브라우저 보안 강화TLS 버전 통제TLS 1.2/1.3구버전 TLS 비활성화로그 개인정보 최소화업무 원문 저장 금지고객명/계좌번호/주민번호 로그 금지# 관리 URL 접근통제 예시<Location "/server-status">    SetHandler server-status    Require ip 10.10.1.0/24    Require ip 10.10.2.0/24</Location># 불필요 Method 제한 예시<Location "/">    <LimitExcept GET POST OPTIONS>        Require all denied    </LimitExcept></Location>15. 정적 리소스 / 압축 / 캐시WebTopSuite 화면 리소스, JavaScript, CSS, 이미지 등 정적 파일을 Apache가 직접 제공하는 경우, 캐시와 압축 정책을 업무 리소스 버전 정책과 함께 설계한다.

대상권장 정책설정 예시HTML/동적 업무 응답캐시 금지Cache-Control no-storeJS/CSS 버전 파일버전 기반 장기 캐시max-age=86400~604800이미지/폰트장기 캐시 가능max-age=604800 이상 검토JSON 업무 응답압축 가능하나 개인정보·성능 검증 필요AddOutputFilterByType DEFLATE application/json대용량 다운로드동기 다운로드 최소화비동기 파일 생성 후 다운로드# 정적 리소스 캐시 예시<Directory "/app/nsight/static">    Require all granted    Options -Indexes    AllowOverride None    <FilesMatch "\.(js|css|png|jpg|gif|svg|woff2?)$">        Header set Cache-Control "public, max-age=604800"    </FilesMatch></Directory># 업무 화면/JSON은 캐시 금지<LocationMatch "^/(marketing|singleview)/">    Header set Cache-Control "no-store, no-cache, must-revalidate, max-age=0"    Header set Pragma "no-cache"</LocationMatch>16. Health Check / mod_statusL4 Health Check는 Apache 프로세스 생존 여부만 보지 말고, 최소한 Apache가 요청을 받고 Proxy 경로가 정상인지 확인할 수 있어야 한다. 다만 DB까지 포함한 Deep Check는 별도 Spring Actuator 또는 운영 API로 분리하는 것이 안전하다.

URL용도응답 기준접근 범위/healthzApache 자체 생존 확인200 OK, 간단한 정적 응답L4/관리망/proxy-healthTomcat Proxy 경로 확인Tomcat health 또는 AP 응답L4/관리망/server-statusApache Worker 상태 확인mod_status HTML 또는 ?auto관리망 전용/server-status?auto모니터링 수집기계 판독 가능한 상태값모니터링망 전용# conf.d/60-status.confExtendedStatus On<Location "/server-status">    SetHandler server-status    Require ip 10.10.1.0/24    Require ip 10.10.2.0/24</Location># Apache 자체 Health Check용 정적 파일Alias /healthz /var/www/health/healthz.txt<Directory "/var/www/health">    Require all granted</Directory>17. 배포 및 운영 점검 절차단계작업명령/확인성공 기준1설정 백업cp -a /etc/httpd /backup/httpd_YYYYMMDD기존 설정 복구 가능2문법 검증apachectl configtestSyntax OK3모듈 확인httpd -M필수 모듈 로딩 확인4무중단 반영systemctl reload httpd 또는 apachectl graceful기존 연결 유지5서비스 상태systemctl status httpdactive 상태6Health Checkcurl -k https://host/healthz200 OK7Proxy 경로 확인curl -k https://host/marketing/healthTomcat 응답 확인8로그 확인tail -f logs/nsight_access.logGUID/응답시간 기록 확인9상태 확인curl http://127.0.0.1/server-status?autoBusy/Idle Worker 확인# 변경 반영 표준 명령apachectl configtestsystemctl reload httpdsystemctl status httpd --no-pager# 장애 시 즉시 원복cp -a /backup/httpd_YYYYMMDD/* /etc/httpd/apachectl configtestsystemctl reload httpd18. 성능 테스트 검증 시나리오시나리오부하 기준검증 항목합격 기준일반 부하600 TPSAccess Log, Apache Worker, Tomcat 응답p95 3초 이하, 5xx 오류율 0.1% 이하피크 부하1,200 TPSMaxRequestWorkers, Proxy Timeout, L4 IdleApache BusyWorker 70% 이하 권장스트레스1,800 TPS큐 적체, 503/504 발생 여부병목 위치 식별 및 한계값 기록Tomcat 지연Backend 응답 10초 초과 유도Proxy timeout/오류 처리Apache Worker 고갈 없이 504 또는 표준 오류Apache 노드 장애Apache 1대 중지L4 Health Check / 우회장애 감지 후 정상 노드 우회로그 추적GUID 포함 요청Access/AP 로그 연결GUID 하나로 E2E 추적 가능

## 19. 장애 대응 및 우회 기준장애 유형탐지 기준Apache 처리후속 조치Tomcat 연결 실패connectiontimeout 3초해당 Backend retry/우회Tomcat 상태 확인Tomcat 응답 지연timeout 10초Proxy 오류 반환Spring/DB 병목 확인Apache Worker 고갈BusyWorker 급증, 503L4 분산/증설Timeout 및 AP 병목 분석SSL 인증서 오류Handshake 실패서비스 영향인증서 만료/체인 확인로그 디스크 FullErrorLog 기록 실패서비스 위험logrotate/디스크 증설L4 Health Check 실패/healthz 실패해당 Apache 제외프로세스/포트/설정 확인재처리 원칙: Timeout 거래는 Apache나 Client가 단독으로 재처리 판단하지 않는다. GUID/RequestId 기준으로 서버의 거래 상태를 조회한 후 재처리 가능 여부를 결정해야 한다.

## 20. 최종 체크리스트분류점검 항목완료기본Apache 2.4.x 사용 및 보안 패치 수준 확인□모듈ssl/proxy/proxy_http/headers/remoteip/log_config/status 모듈 확인□ProxyProxyRequests Off 적용□TimeoutProxy connectiontimeout=3, timeout=10, Timeout=15 정합 확인□KeepAliveKeepAlive On, MaxKeepAliveRequests, KeepAliveTimeout 검증□SSLTLS 1.2/1.3, 인증서 체인, 만료일 확인□HeaderX-GUID, X-Forwarded-For, X-Forwarded-Proto 전달 확인□로그Access Log에 GUID, 상태코드, durationUs 기록 확인□보안ServerTokens/ServerSignature/TraceEnable 보안 설정 확인□Status/server-status 접근을 관리망으로 제한□운영apachectl configtest 후 graceful reload 절차 확정□성능600/1200/1800 TPS 시나리오로 검증□부록: 전체 예시 설정 파일# =============================================================# NSIGHT Apache 예시 설정 - 핵심 항목 샘플# 실제 IP, 도메인, 인증서 경로, 관리망 대역은 운영 환경에 맞게 변경한다.# =============================================================# 00-global.confServerTokens ProdServerSignature OffTraceEnable OffHostnameLookups OffTimeout 15KeepAlive OnMaxKeepAliveRequests 1000KeepAliveTimeout 120FileETag None# 10-mpm-event.conf<IfModule mpm_event_module>    StartServers             4    ServerLimit             32    ThreadsPerChild        128    MaxRequestWorkers     4096    MinSpareThreads        256    MaxSpareThreads       1024    MaxConnectionsPerChild 50000</IfModule># 40-headers.confRequestHeader set X-Forwarded-Proto "https"RequestHeader set X-Forwarded-Port  "443"RequestHeader set X-Proxy-ID        "APACHE-WEB-01"RequestHeader set X-Center-ID       "DC1"RemoteIPHeader X-Forwarded-ForRemoteIPInternalProxy 10.10.0.0/16Header always set X-Content-Type-Options "nosniff"Header always set X-Frame-Options "SAMEORIGIN"Header always set Referrer-Policy "strict-origin-when-cross-origin"# 50-logging.confLogFormat "{ \"time\":\"%{%Y-%m-%dT%H:%M:%S%z}t\", \"clientIp\":\"%a\", \"xff\":\"%{X-Forwarded-For}i\", \"guid\":\"%{X-GUID}i\", \"requestId\":\"%{X-Request-ID}i\", \"methodUri\":\"%r\", \"status\":%>s, \"bytes\":%b, \"durationUs\":%D, \"userAgent\":\"%{User-Agent}i\" }" nsight_jsonCustomLog logs/nsight_access.log nsight_jsonErrorLog  logs/nsight_error.logLogLevel warn proxy:info ssl:warn# 30-vhost.conf<VirtualHost *:443>    ServerName nsight-marketing.nh.local    SSLEngine on    SSLProtocol -all +TLSv1.2 +TLSv1.3    SSLCertificateFile      /etc/pki/tls/certs/nsight.crt    SSLCertificateKeyFile   /etc/pki/tls/private/nsight.key    SSLCertificateChainFile /etc/pki/tls/certs/chain.crt    ProxyRequests Off    ProxyPreserveHost On    ProxyTimeout 10    ProxyPass        /marketing/  http://10.10.20.100:8080/marketing/  connectiontimeout=3 timeout=10 retry=5    ProxyPassReverse /marketing/  http://10.10.20.100:8080/marketing/    ProxyPass        /singleview/ http://10.10.20.110:8080/singleview/ connectiontimeout=3 timeout=10 retry=5    ProxyPassReverse /singleview/ http://10.10.20.110:8080/singleview/    CustomLog logs/nsight_ssl_access.log nsight_json    ErrorLog  logs/nsight_ssl_error.log</VirtualHost># 60-status.confExtendedStatus On<Location "/server-status">    SetHandler server-status    Require ip 10.10.1.0/24    Require ip 10.10.2.0/24</Location>참고 기준구분참고 내용Apache 공식 문서Apache HTTP Server 2.4, mod_proxy, Log Files, mod_status, mod_ssl 문서 기준NSIGHT 환경설정 기준6000지점, 지점당 6명, 실사용 동시 요청자 기준 환경설정 가이드NSIGHT 타임아웃 기준WebTopSuite → Apache/Nginx Proxy → L4 → Tomcat → Spring Boot → DB 계층별 Timeout 정합 기준NSIGHT 관측성 기준GUID, 로그, 오류코드, 모니터링 지표를 통한 End-to-End 추적 원칙NSIGHT 보안 기준접근통제, 인증·인가, 마스킹, 감사로그, 보안 Header 적용 원칙

---

2. WebTopSuite / WEB / Proxy / L4 설정영역설정 항목권장값실제 설정 위치설정 예시설명

WebTopSuiteRequest Timeout15초WebTopSuite Runtime ConfigREQUEST_TIMEOUT=15000사용자 최종 대기시간WebTopSuiteConnect Timeout3초WebTopSuite Runtime ConfigCONNECT_TIMEOUT=3000AP 연결 실패 판단WebTopSuiteRead Timeout10초WebTopSuite Runtime ConfigREAD_TIMEOUT=10000AP 응답 대기WebTopSuiteRetry Count1회 이하WebTopSuite Runtime ConfigRETRY_COUNT=1무분별한 재요청 방지NginxProxy Connect Timeout3초nginx.confproxy_connect_timeout 3s;AP 연결 실패 판단NginxProxy Read Timeout10초nginx.confproxy_read_timeout 10s;AP 응답 대기NginxProxy Send Timeout10초nginx.confproxy_send_timeout 10s;요청 전송 대기ApacheProxy Timeout10초httpd.confProxyTimeout 10Proxy 전체 대기시간ApacheKeepAliveONhttpd.confKeepAlive OnTCP 연결 재사용ApacheKeepAlive Timeout120초httpd.confKeepAliveTimeout 120유휴 연결 유지L4GSLB DNS TTL30초GSLB 설정 콘솔TTL = 30s센터 장애 시 빠른 재조회L4Health Check Interval5초L4 설정 콘솔interval = 5sAP 상태 확인L4Health Check Timeout2초L4 설정 콘솔timeout = 2s응답 없

음 판단L4Fail Count3회L4 설정 콘솔fail count = 3약 15초 내 장애 판단L4Sticky / Persistence활성화L4 설정 콘솔JSESSIONID persistence세션 유지L4Sticky Timeout70분L4 설정 콘솔sticky timeout = 7

0mSession 60분보다 길게L4Idle Timeout120초L4 설정 콘솔idle timeout = 120sKeepAlive 고려Timeout 순서는 DB Query Timeout < Transaction Timeout < WebTopSuite/Proxy Timeout < L4 Timeout 구조로 맞추는 것이 안

전합니다.

## 절별 상세

### 20.1 Worker Process와 Thread

본 절(**Worker Process와 Thread**)은 Apache 영역에서 **Reverse Proxy** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 8C/32G 권고 | 산식/근거 |
|------|-------------|----------|
| maxThreads | 400~500 | 250×1.2×1.2≈360 |
| minSpareThreads | 100 | 피크 진입 지연 완화 |
| acceptCount | 300~500 | Queue 병목 은폐 금지 |
| maxConnections | 10,000 | L4 KeepAlive 정합 |

#### 설정 예시

```xml
<Connector port="8080" protocol="org.apache.coyote.http11.Http11NioProtocol"
  maxThreads="500" minSpareThreads="100" acceptCount="500"
  maxConnections="10000" connectionTimeout="8000"
  keepAliveTimeout="120000" maxKeepAliveRequests="200" />
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: httpd.conf·vhost

### 20.2 Backend Connection Pool

본 절(**Backend Connection Pool**)은 Apache 영역에서 **Reverse Proxy** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 일반 AP | SV AP | 산식 |
|------|---------|-------|------|
| maximumPoolSize | 50 | 60 | TPS×0.15×1.3 |
| minimumIdle | 15 | 15 | max의 20~30% |
| connectionTimeout | 3초 | 3초 | ≠ SQL timeout |
| maxLifetime | 30분 | 30분 | DB Idle보다 짧게 |

#### 설정 예시

```yaml
spring.datasource.hikari:
  pool-name: marketing-pool
  maximum-pool-size: 50
  minimum-idle: 15
  connection-timeout: 3000
  idle-timeout: 600000
  max-lifetime: 1800000
  auto-commit: false
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: httpd.conf·vhost

### 20.3 Proxy Connect Timeout

본 절(**Proxy Connect Timeout**)은 Apache 영역에서 **Reverse Proxy** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 계층 | 권고 | 설정 위치 |
|------|------|----------|
| SQL | 3초 | MyBatis / Mapper |
| Pool 획득 | 3초 | Hikari connectionTimeout |
| Transaction | 5초 | @Transactional |
| TCF ServiceId | 4~5초 | TCF 설정 |
| Proxy | 10초 | Apache / Gateway |
| WebTop | 15초 | 단말 |
| L4 Idle | 120초 | L4 Profile |

#### 설정 예시

| 계층 | 권고 | 설정 위치 |
|------|------|----------|
| SQL | 3초 | MyBatis / Mapper |
| Pool 획득 | 3초 | Hikari connectionTimeout |
| Transaction | 5초 | @Transactional |
| TCF ServiceId | 4~5초 | TCF 설정 |
| Proxy | 10초 | Apache / Gateway |
| WebTop | 15초 | 단말 |
| L4 Idle | 120초 | L4 Profile |

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: httpd.conf·vhost

### 20.4 Proxy Read Timeout

본 절(**Proxy Read Timeout**)은 Apache 영역에서 **Reverse Proxy** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 계층 | 권고 | 설정 위치 |
|------|------|----------|
| SQL | 3초 | MyBatis / Mapper |
| Pool 획득 | 3초 | Hikari connectionTimeout |
| Transaction | 5초 | @Transactional |
| TCF ServiceId | 4~5초 | TCF 설정 |
| Proxy | 10초 | Apache / Gateway |
| WebTop | 15초 | 단말 |
| L4 Idle | 120초 | L4 Profile |

#### 설정 예시

| 계층 | 권고 | 설정 위치 |
|------|------|----------|
| SQL | 3초 | MyBatis / Mapper |
| Pool 획득 | 3초 | Hikari connectionTimeout |
| Transaction | 5초 | @Transactional |
| TCF ServiceId | 4~5초 | TCF 설정 |
| Proxy | 10초 | Apache / Gateway |
| WebTop | 15초 | 단말 |
| L4 Idle | 120초 | L4 Profile |

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: httpd.conf·vhost

### 20.5 Request Timeout

본 절(**Request Timeout**)은 Apache 영역에서 **Reverse Proxy** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 계층 | 권고 | 설정 위치 |
|------|------|----------|
| SQL | 3초 | MyBatis / Mapper |
| Pool 획득 | 3초 | Hikari connectionTimeout |
| Transaction | 5초 | @Transactional |
| TCF ServiceId | 4~5초 | TCF 설정 |
| Proxy | 10초 | Apache / Gateway |
| WebTop | 15초 | 단말 |
| L4 Idle | 120초 | L4 Profile |

#### 설정 예시

| 계층 | 권고 | 설정 위치 |
|------|------|----------|
| SQL | 3초 | MyBatis / Mapper |
| Pool 획득 | 3초 | Hikari connectionTimeout |
| Transaction | 5초 | @Transactional |
| TCF ServiceId | 4~5초 | TCF 설정 |
| Proxy | 10초 | Apache / Gateway |
| WebTop | 15초 | 단말 |
| L4 Idle | 120초 | L4 Profile |

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: httpd.conf·vhost

### 20.6 Keep-Alive Timeout

본 절(**Keep-Alive Timeout**)은 Apache 영역에서 **Reverse Proxy** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 계층 | 권고 | 설정 위치 |
|------|------|----------|
| SQL | 3초 | MyBatis / Mapper |
| Pool 획득 | 3초 | Hikari connectionTimeout |
| Transaction | 5초 | @Transactional |
| TCF ServiceId | 4~5초 | TCF 설정 |
| Proxy | 10초 | Apache / Gateway |
| WebTop | 15초 | 단말 |
| L4 Idle | 120초 | L4 Profile |

#### 설정 예시

| 계층 | 권고 | 설정 위치 |
|------|------|----------|
| SQL | 3초 | MyBatis / Mapper |
| Pool 획득 | 3초 | Hikari connectionTimeout |
| Transaction | 5초 | @Transactional |
| TCF ServiceId | 4~5초 | TCF 설정 |
| Proxy | 10초 | Apache / Gateway |
| WebTop | 15초 | 단말 |
| L4 Idle | 120초 | L4 Profile |

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: httpd.conf·vhost

### 20.7 최대 요청 크기

본 절(**최대 요청 크기**)은 Apache 영역에서 **Reverse Proxy** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | Reverse Proxy 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | httpd.conf·vhost |
| 핵심 | ProxyTimeout 10s |

#### 설정 예시

```apache
ProxyTimeout 10
ProxyConnectTimeout 3
KeepAlive On
KeepAliveTimeout 120
RequestHeader set X-GUID %{GUID}e
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: httpd.conf·vhost

### 20.8 최대 응답 크기

본 절(**최대 응답 크기**)은 Apache 영역에서 **Reverse Proxy** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | Reverse Proxy 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | httpd.conf·vhost |
| 핵심 | ProxyTimeout 10s |

#### 설정 예시

```apache
ProxyTimeout 10
ProxyConnectTimeout 3
KeepAlive On
KeepAliveTimeout 120
RequestHeader set X-GUID %{GUID}e
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: httpd.conf·vhost

### 20.9 Upload·Download Buffer

본 절(**Upload·Download Buffer**)은 Apache 영역에서 **Reverse Proxy** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 권고 |
|------|------|
| max-file-size | 업무별 10~50MB |
| 대용량 | 전용 서버 |

#### 설정 예시

```yaml
spring.servlet.multipart.max-file-size: 10MB
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: httpd.conf·vhost

### 20.10 정적 자원 Cache

본 절(**정적 자원 Cache**)은 Apache 영역에서 **Reverse Proxy** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 유형 | TTL | 비고 |
|------|-----|------|
| Local | 1~5분 | Stampede 방지 |
| Redis | 5~30분 | fallback |

#### 설정 예시

```yaml
cache:
  ttl: 300s
  max-size: 10000
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: httpd.conf·vhost

### 20.11 압축 설정

본 절(**압축 설정**)은 Apache 영역에서 **Reverse Proxy** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | Reverse Proxy 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | httpd.conf·vhost |
| 핵심 | ProxyTimeout 10s |

#### 설정 예시

```apache
ProxyTimeout 10
ProxyConnectTimeout 3
KeepAlive On
KeepAliveTimeout 120
RequestHeader set X-GUID %{GUID}e
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: httpd.conf·vhost

### 20.12 Access Log

본 절(**Access Log**)은 Apache 영역에서 **Reverse Proxy** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 필드 | 필수 |
|------|------|
| GUID | ✓ |
| TraceId | ✓ |
| UserId | ✓ |

#### 설정 예시

```xml
<Valve className="org.apache.catalina.valves.AccessLogValve"
  pattern="%h %t \"%r\" %s %b %D %{X-GUID}i" />
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: httpd.conf·vhost

### 20.13 WEB 설정 검증

본 절(**WEB 설정 검증**)은 Apache 영역에서 **Reverse Proxy** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | Reverse Proxy 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | httpd.conf·vhost |
| 핵심 | ProxyTimeout 10s |

#### 설정 예시

```apache
ProxyTimeout 10
ProxyConnectTimeout 3
KeepAlive On
KeepAliveTimeout 120
RequestHeader set X-GUID %{GUID}e
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

검증 도구: APM, `jcmd`, `jstat`, Hikari Metrics, Access Log(GUID), ENV rule-check.

---

[← 목차](./00-목차.md)
