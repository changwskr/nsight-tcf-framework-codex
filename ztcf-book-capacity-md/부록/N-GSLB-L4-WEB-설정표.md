# 부록 N. GSLB·L4·WEB 설정표

> 원본: `znsight-capacity-word` · 23장 수준 템플릿 확장

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

> 출처: `znsight-capacity-word` · [13단계 요약](../zNSIGHT-용량산정-전체-흐름.md)


## 원문 기반 본문

NSIGHT L4 설정 가이드GSLB · L4 VIP · Pool · Health Check · Persistence · Timeout · 운영검증 기준농협상호금융 NSIGHT 정보계 / 마케팅플랫폼작성 기준: 6,000개 지점 × 지점당 6명 / 600·1,200·1,800 TPS / WebTopSuite → L4 → Apache → Tomcat → Spring Boot → DB1. 문서 목적 및 적용 범위본 문서는 NSIGHT 마케팅플랫폼의 L4 Load Balancer와 GSLB 설정을 위한 표준 가이드이다. L4는 단순히 트래픽을 분산하는 장비가 아니라 센터 선택, VIP 접근, 세션 유지, 장애 감지, 장애 전환, 접속 타임아웃, 운영 관측성의 기준점이다.

설계 메모: L4 설정은 Apache나 Tomcat 설정과 독립적으로 정하지 않는다. WebTopSuite Request Timeout, Apache Proxy Timeout, Tomcat Session Timeout, Spring Transaction Timeout, DB Query Timeout과 계층적으로 맞춰야 한다.

구분적용 범위본 문서 기준대상 시스템마케팅 AP, SingleView AP, 필요 시 업무별 AP ClusterWebTopSuite 단말에서 L4 VIP를 통해 진입대상 계층GSLB, 센터 L4, VIP, Pool, Member, Health Monitor, PersistenceHTTP/HTTPS 기반 온라인 업무대상 업무일반 마케팅 조회, Single View, 캠페인 관리, 권한/감사/로그 포함 거래온라인 동기 거래는 3초 목표, 15초 내 사용자 실패 처리제외 범위DB 내부 RAC/서비스명, Kafka/ETL 내부 Balancing별도 기술 가이드에서 관리운영 관점Fail-Over, Fail-Back, 우회, 헬스체크, 모니터링운영자가 감지·판단·전환 가능한 구조

## 2. NSIGHT L4 적용 아키텍처

### 2.1 전체 접속 흐름[WebTopSuite 단말]      │ ① GSLB DNS Lookup      ▼[GSLB]      │ ② 정상 센터 L4 VIP 반환      ▼[센터 L4 VIP]      │ ③ VIP / Persistence / Health Check 적용      ▼[Apache Reverse Proxy]      │ ④ Header 전달, Access Log, Proxy Timeout      ▼[Tomcat / Spring Boot AP]      │ ⑤ 업무 처리, 세션, 트랜잭션, DB Pool      ▼[RDW / ADW / 연계 시스템]센터 간 선택은 GSLB가 담당하고, 센터 내부의 AP 분산은 L4가 담당한다. WebTopSuite 단말은 최초 센터 선택 후 반환된 센터 L4 VIP를 기준으로 접속하며, 센터 장애 시 GSLB 재조회 또는 사전 정의된 우회 정책으로 전환한다.

### 2.2 L4의 역할 정의역할설명설정 대상센터 진입점 제공사용자 요청이 접근하는 대표 서비스 주소 제공VIP / Virtual Server부하분산정상 상태의 Apache 또는 AP 서버로 요청 분산Pool / Member / Load Balancing Method상태 감시장애 서버를 자동 제외하고 복구 시 재투입Health Monitor / Fail Count / Rise Count세션 유지동일 사용자의 요청을 일정 시간 동일 서버 또는 동일 클러스터로 유지Persistence / Sticky장애 격리죽은 서버로 요청이 가지 않도록 즉시 차단Monitor + Pool Member Disable타임아웃 통제무한 대기, 연결 고착, Half-open 연결을 방지Idle Timeout / TCP Profile운영 관측성VIP, Pool, Member, 오류율, 현재 연결 수를 감시L4 Dashboard / SNMP / Syslog3. 트래픽 및 용량 산정 기준본 가이드는 6,000개 지점, 지점당 사용자 6명, 전체 사용자 36,000명을 기준으로 한다. 세션 수와 TPS는 분리한다. 전체 사용자는 로그인 세션 또는 잠재 사용자 규모이며, L4 처리량은 실제 동시 요청자와 TPS를 기준으로 산정한다.

구분값L4 설계 의미전체 지점 수6,000개접속 지역·권역 분산 및 피크 시간대 고려지점당 사용자6명지점 단말 기반 동시 로그인 규모 산정전체 사용자36,000명세션 및 인증 기반 최대 사용자 규모여유율 포함 세션43,000~47,000 세션Persistence Table, 세션 유지, 모니터링 기준일반 기준600 TPS일상 피크 운영 기준피크 기준1,200 TPS업무 집중 시간 및 센터 장애 감당 기준스트레스 기준1,800 TPS성능시험 및 한계 검증 기준목표 응답시간p95 3초 이하L4는 업무 Timeout보다 길고, 장애 감지는 짧게

> **주의**: L4의 초당 요청 처리량은 1,200 TPS만 보지 말고 KeepAlive 연결 수, 동시 연결 수, 세션 Persistence Table 크기, Health Check 대상 수, SSL 처리 위치까지 함께 봐야 한다.4. L4 핵심 설정 기준표영역설정 항목권장값설정 위치/명칭설명

GSLBDNS TTL30초GSLB Zone / Wide IP센터 장애 시 빠른 재조회GSLB센터 장애 정책자동 전환GSLB Pool / Topology / Priority정상 센터 L4 VIP만 반환L4 VIPService Port443Virtual Server / VIPHTTPS 기준 진입점L4 VIPLoad Balancing MethodRound Robin 또는 Least ConnectionPool MethodAP 처리시간 편차가 크면 Least Connection 검토L4Health Check Interval5초Monitor Interval상태 확인 주기L4Health Check Timeout2초Monitor Timeout응답 없음 판단L4Fail Count3회Down Threshold약 15초 내 장애 판단L4Rise Count2회 이상Up ThresholdFlapping 방지를 위한 복귀 조건L4PersistenceJSESSIONID 또는 Cookie 기반Persistence Profile세션 유지 기준L4Sticky Timeout70분Persistence TimeoutSession 60분보다 길게L4Idle Timeout120초TCP / HTTP ProfileKeepAlive 고려L4SNAT환경별 결정SNAT / Source NAT서버 Return Path 보장 필요 시 사용L4X-Forwarded-For전달HTTP Profile / Header Insert실제 Client IP 보존L4Syslog/SNMP활성화Logging / Monitoring운영 감시 및 장애 분석

## 5. GSLB 설정 가이드

### 5.1 GSLB 기본 원칙GSLB는 센터 간 접근 경로를 결정하고, L4는 센터 내부 서버 분산을 담당한다.

GSLB는 장애 센터의 VIP를 응답하지 않아야 하며, 정상 센터 VIP만 반환해야 한다.

DNS TTL은 장애 전환성과 DNS 질의 부하 사이의 균형으로 30초를 기본값으로 둔다.

GSLB Health Check는 단순 ICMP가 아니라 서비스 포트 또는 상태 확인 URL을 기준으로 구성한다.


| 설정 항목 | 권장값 | 설정 예시 | 검증 방법 |
|-----------|--------|-----------|-----------|
| Wide IP / 서비스 도메인 | 업무 서비스 도메인 | mkt.nh.local → DC1/DC2 VIP | nslookup/dig 결과 확인 |
| Pool 구성 | 센터별 L4 VIP 등록 | DC1_L4_VIP, DC2_L4_VIP | 정상 센터만 응답 확인 |
| DNS TTL | 30초 | ttl=30 | 장애 시 재조회 전환시간 측정 |
| Load Balance Method | Priority 또는 Round Robin | Active-Active Round Robin | 센터별 유입 비율 확인 |
| Health Check | HTTPS 443 + URL | GET /health/l4 | 장애 VIP DNS 응답 제외 |
| Failure Policy | 정상 Pool만 반환 | return only available VIP | 강제 장애 테스트 |

GSLB 논리 예시Wide IP  : mkt.nsight.nh.localPool     : DC1_L4_VIP(10.10.10.100), DC2_L4_VIP(10.20.10.100)TTL      : 30sMonitor  : HTTPS 443 /health/l4Policy   : 정상 VIP만 DNS 응답, 장애 VIP 응답 제외

## 6. L4 VIP / Virtual Server 설정

### 6.1 Virtual Server 기본 설정항목권장 설정설명VIP Address센터별 대표 IPWebTopSuite와 Apache가 접근하는 서비스 진입점Service Port443운영은 HTTPS 기준. 내부망 정책에 따라 80→443 Redirect 가능Protocol ProfileTCP 또는 HTTP/HTTPS ProfileCookie 기반 Persistence와 Header 제어가 필요하면 HTTP 계층 기능 필요SSL 처리L4 Offload 또는 Pass-through인증서 운영 정책, 보안 정책, Header 삽입 필요성에 따라 결정Default PoolMarketing Web/Apache PoolVIP가 요청을 전달할 기본 서버 그룹Connection Limit장비/업무별 산정무제한보다 운영 임계치 기반 설정 권장SNAT필요 시 적용서버의 응답 경로가 L4를 거치도록 보장

### 6.2 Pool / Member 설정항목권장 설정설명Pool 구분업무별 분리Marketing AP, SingleView AP, EBM, 실시간처리 등 역할별 분리Member 단위Apache 또는 Tomcat 노드구성에 따라 L4→Apache 또는 L4→Tomcat 직접 분산분산 방식Round Robin / Least Connection응답시간 편차가 큰 SingleView는 Least Connection 검토Member Ratio동일 사양이면 동일 Ratio서버 사양이 다르면 가중치 조정Connection Draining활성화배포 또는 점검 시 신규 요청 중단 후 기존 연결 종료 대기Slow Ramp활성화 검토복구 서버에 트래픽이 한 번에 몰리는 것 방지Disabled vs Forced Offline구분 사용점검/배포/장애 격리 시 운영 절차에 맞게 선택

## 7. Health Check 설정Health Check는 장애 전환 품질을 결정한다. 단순히 포트가 열려 있는지만 확인하면 AP는 살아 있지만 애플리케이션이나 DB 연결은 죽은 상태를 정상으로 오판할 수 있다. 따라서 L4 Health Check는 최소 2단계로 구성한다.

구분체크 방식용도권장값1단계 Port CheckTCP 443 또는 80서버·Apache 기동 여부 확인Interval 5초 / Timeout 2초2단계 URL CheckGET /health/l4애플리케이션 라우팅 가능 여부 확인HTTP 200 + 본문 OK3단계 Deep CheckGET /actuator/health/livenessAP 상태 확인선택 적용4단계 Readiness CheckGET /actuator/health/readinessDB/외부 의존성 포함 여부 결정운영 정책에 따라 적용

### 7.1 Health Check 권장값항목권장값설명Interval5초상태 확인 주기Timeout2초응답 없음 판단 시간Fail Count3회3회 연속 실패 시 Down장애 감지 목표약 15초5초 × 3회 기준Rise Count2회 이상복구 시 2회 이상 성공 후 UpResponse Code200정상 응답 코드Response BodyOK 또는 UP단순 200 위조를 줄이기 위한 본문 확인DB 포함 여부기본 미포함, 별도 Readiness에서 판단DB 일시 지연이 곧바로 전체 AP Down으로 이어지지 않도록 주의Health Check URL 예시GET /health/l4Response: 200 OKBody    : OK

> **주의**:- /health/l4는 Apache/Tomcat 라우팅 가능성 중심- /health/readiness는 DB, 외부연계 포함 여부를 업무 정책으로 결정

## 8. Persistence / Sticky 설정NSIGHT는 WebTopSuite 단말 기반 장시간 업무가 존재하므로 세션 유지 정책을 명확히 해야 한다. 센터 내부 AP 장애는 DeltaManager 또는 세션 복제 정책으로 대응하고, 센터 장애는 기본적으로 재로그인 또는 상태조회 기반 복구 정책으로 단순화한다.

방식권장 여부장점주의사항Cookie / JSESSIONID 기반권장HTTP 세션과 연계가 명확함L4가 HTTP Cookie를 볼 수 있어야 함Source IP 기반보조설정이 단순함지점 NAT 환경에서는 한 IP에 요청이 몰릴 수 있음SSL Session ID 기반제한적TLS 기반 유지 가능TLS 재협상·Offload 구조에 민감No Persistence비권장부하분산 균등세션 기반 업무에서는 로그인/상태 불일치 위험

### 8.1 세션 관련 Timeout 정합성항목권장값설명

Tomcat Session Idle Timeout60분 기본업무 유휴 세션 만료L4 Sticky Timeout70분Tomcat 세션보다 길게 설정L4 Idle Timeout120초연결 유휴 Timeout, 세션 Timeout과 다름WebTopSuite Center 유지앱 실행 중 유지선택된 센터 L4 직접 접근 유지Absolute Session Timeout8시간애플리케이션 공통에서 강제 만료센터 장애 시재로그인 또는 상태조회센터 간 세션 복제 미적용 시 단순 정책설정 원칙: Sticky Timeout은 로그인 세션 시간보다 짧으면 안 된다. 반대로 너무 길면 장애 전환 후 오래된 Persistence 정보가 남을 수 있으므로 Tomcat Idle Timeout + 10분 정도를 기준으로 둔다.9. Timeout / Connection 설정Timeout은 계층별로 목적이 다르다. L4 Idle Timeout은 연결 유휴 상태를 정리하는 값이고, Spring Transaction Timeout은 업무 처리를 중단하는 값이며, DB Query Timeout은 SQL 수행을 제한하는 값이다. 따라서 L4 Timeout은 업무 Timeout보다 길게, 장애 감지는 짧게 가져간다.

계층설정 항목권장값설명

WebTopSuiteRequest Timeout15초사용자 최종 대기시간ApacheProxy Connect Timeout3초AP 연결 실패 판단ApacheProxy Read Timeout10초AP 응답 대기L4Idle Timeout120초유휴 TCP 연결 정리L4Persistence Timeout70분세션 유지용 Sticky TimeoutTomcatconnectionTimeout8초연결 후 요청 대기시간SpringTransaction Timeout4~5초온라인 거래 전체 수행 한도HikariconnectionTimeout2~3초DB Pool 대기시간MyBatis/JDBCQuery Timeout2~3초SQL 실행시간 제한권장 Timeout 관계DB Query Timeout        : 2~3초Hikari Connection Wait  : 2~3초Spring Transaction      : 4~5초Apache Proxy Read       : 10초WebTopSuite Request     : 15초L4 TCP Idle             : 120초L4 Persistence          : 70분

## 10. SSL/TLS 및 인증서 처리 방식방식설명장점주의사항SSL Offload at L4L4에서 TLS 종료 후 내부 HTTP 전달Header 삽입, Cookie Persistence, 암복호화 집중 관리 용이L4 SSL 성능, 인증서 보안, 내부망 암호화 정책 검토 필요SSL Re-encryptionL4에서 복호화 후 내부로 다시 HTTPS 전달보안성과 HTTP 제어 균형인증서 관리 복잡도 증가SSL Pass-throughL4는 TCP만 분산하고 TLS는 Apache/Tomcat 종료L4 부하 감소, 종단 암호화 유지Cookie 기반 Persistence와 Header 삽입 제약의사결정: NSIGHT에서 X-Forwarded-For, GUID Header, Cookie Persistence를 L4에서 제어해야 한다면 SSL Offload 또는 Re-encryption이 유리하다. 보안 정책상 종단 암호화가 필수이면 Pass-through를 선택하고 Persistence 방식을 별도 검토한다.11. Header / Client IP / GUID 전달관측 가능성을 확보하려면 L4와 Apache를 지나도 원 사용자 IP, 센터 ID, VIP, GUID가 유실되지 않아야 한다. L4는 가능한 범위에서 Header를 보존하거나 삽입하고, Apache와 Spring은 해당 값을 로그와 MDC에 연결한다.

Header설명권장 처리X-Forwarded-For원 Client IPL4 또는 Apache에서 보존/추가X-Forwarded-Proto원 요청 프로토콜https 전달X-Forwarded-Host원 Host업무 URL 생성 및 감사 추적용X-Request-Id요청 식별자없으면 Apache 또는 Spring에서 생성X-GUIDNSIGHT 거래 GUIDWebTopSuite 또는 AP에서 생성 후 전 구간 전달X-Center-Id센터 식별자GSLB/L4/Apache 로그 분석용X-L4-VIP접근 VIP장애 분석 및 라우팅 추적용Header 전달 기준 예시X-Forwarded-For   : {client_ip}X-Forwarded-Proto : httpsX-Request-Id      : {generated_or_inbound_request_id}X-GUID            : {business_guid}X-Center-Id       : DC1 또는 DC2X-L4-VIP          : 10.10.10.10012. 보안 설정 기준보안 항목권장 설정설명관리 콘솔 접근운영망/관리망 제한L4 관리 UI/CLI는 업무망에서 직접 접근 금지관리자 인증개인 계정 + MFA 권장공용 관리자 계정 사용 금지변경 승인변경관리 절차 필수VIP, Pool, Monitor, SSL 변경은 승인 후 반영취약 프로토콜TLS 1.0/

### 1.1 비활성화보안 정책에 따라 TLS

### 1.2 이상Cipher Suite강한 암호군 사용금융권 보안 기준 및 내부 표준 반영Client IP 보존X-Forwarded-For 또는 Proxy Protocol감사로그와 이상거래 추적을 위해 필수DDoS/Connection FloodConnection Limit/Rate Limit 검토업무 정상 트래픽과 구분 필요Audit Log관리자 변경 이력 보관누가 언제 어떤 설정을 바꿨는지 추적

## 13. 모니터링 및 운영 임계치모니터링 항목WarningCritical조치 기준VIP 상태Member 1대 DownPool 전체 Down 위험장애 서버 격리 및 AP 로그 확인Pool Member 상태1회 Down 발생반복 Down / FlappingHealth Check 기준 및 서버 상태 확인Active Connection평균 대비 150%평균 대비 200% 이상피크·비정상 트래픽 여부 판단New Connection Rate기준 초과급증 지속KeepAlive, 단말 재접속, 장애 루프 확인SSL TPS / CPU70%85% 이상Offload 성능 증설 또는 분산 검토Persistence Table 사용률70%85% 이상Sticky Timeout, 세션 수, 만료 정책 점검Health Check 실패율일시 실패3회 이상 연속 실패Member 자동 제외와 원인 분석Reset / Timeout증가 추세급증AP Timeout, Apache Proxy Timeout, 네트워크 확인GSLB 응답 상태한 센터 제외정상 센터 없음센터 장애 대응 절차 가동운영 대시보드는 VIP 상태, Pool Member 상태, TPS, 동시 연결 수, Sticky Table 사용률, Health Check 실패율, SSL 처리율, 에러율을 최소 지표로 구성한다.

## 14. 배포/점검/장애전환 운영 절차

### 14.1 서버 점검 시 절차단계작업확인 항목1대상 Member를 Disabled 또는 Drain 상태로 전환신규 요청이 유입되지 않는지 확인2Active Connection 감소 대기기존 요청 종료 확인3Apache/Tomcat 배포 또는 OS 점검 수행애플리케이션 정상 기동4Health Check URL 정상 확인/health/l4 200 OK5Member Enable트래픽이 천천히 복귀하는지 확인6로그·응답시간·오류율 확인배포 후 10~30분 집중 모니터링

### 14.2 센터 장애 전환 절차단계작업판단 기준1센터 VIP Health Check 실패 감지GSLB에서 장애 센터 VIP 제외2정상 센터 VIP만 DNS 응답TTL 30초 기준 전환 확인3WebTopSuite 재시도 또는 GSLB 재조회센터 L4 직접 접근 주소 갱신4AP/DB/연계 상태 점검정상 센터에서 목표 TPS 감당 여부 확인5센터 복구 후 Fail-Back 판단업무 승인 후 점진 복귀

## 15. 성능 및 장애 테스트 시나리오시나리오검증 내용성공 기준정상 부하 테스트600 TPS 처리p95 3초 이하, 오류율 기준 이하피크 부하 테스트1,200 TPS 처리CPU/L4/Apache/Tomcat/DB Pool 임계치 안정스트레스 테스트1,800 TPS 처리한계 지점 식별, 장애 없이 완만한 실패Member 장애Pool Member 1대 Down15초 내 제외, 정상 Member로 요청 분산Apache 장애Apache 프로세스 중지Health Check 실패 후 자동 제외AP 지연/health는 정상이나 업무 응답 지연Timeout 계층에 따라 실패 처리센터 장애센터 L4 VIP 장애GSLB가 장애 VIP 응답 제외Sticky 검증동일 세션 반복 요청정의된 Persistence 정책대로 유지Drain 검증배포 중 Member Drain신규 요청 차단, 기존 요청 정상 종료로그 검증GUID와 Client IP 추적WebTopSuite→L4→Apache→AP 구간 추적 가능

## 16. 장애 유형별 점검 가이드증상가능 원인1차 확인조치 방향특정 사용자만 로그인 반복 실패Sticky 불일치, 세션 복제 실패Persistence Table, JSESSIONID, Tomcat SessionSticky 방식 재확인, 세션 객체 점검전체 응답 지연AP Thread/DB Pool/SQL 지연L4 Active Connection, Apache BusyWorker, APMDB Query Timeout, Pool 대기시간 확인VIP 접속 불가Virtual Server Down, 네트워크, SSL 오류VIP 상태, ARP, Route, 인증서VIP/Pool/SSL Profile 점검간헐적 502/503Member Flapping, Health Check 불안정Pool Member 상태 변경 이력Health Check URL과 Timeout 조정센터 전환 지연DNS TTL, Client Cache, GSLB Policydig/nslookup, 단말 DNS CacheTTL, 단말 재조회 정책 점검실제 Client IP 유실SNAT/Proxy Header 누락Apache Access Log, AP MDCX-Forwarded-For 설정 추가배포 중 오류 증가Drain 미적용, 기존 연결 강제 종료Member 상태, Active Connection배포 절차에 Drain 단계 추가

## 17. 장비별 명칭 매핑표L4 장비는 벤더별로 명칭이 다르지만 개념은 유사하다. 실제 적용 시에는 고객사 표준 장비의 UI/CLI 명칭으로 변환해 반영한다.

NSIGHT 기준 용어F5 BIG-IP 계열A10 계열Citrix ADC 계열의미VIP / Virtual ServerVirtual ServerVirtual Server / VIPVirtual Server서비스 대표 IP/PortPoolPoolService GroupService Group트래픽을 받을 서버 그룹MemberPool MemberReal Server / MemberService실제 서버Health CheckMonitorHealth MonitorMonitor상태 확인PersistencePersistence ProfilePersistence TemplatePersistence세션 유지SNATSNAT / Auto MapSource NATUse Source IP / SNIP응답 경로 보장SSL ProfileClient/Server SSL ProfileSSL TemplateSSL ProfileTLS 처리 정책Connection DrainDisabled / Forced OfflineGraceful ShutdownDisable Service배포/점검 시 연결 배수

## 18. 최종 적용 체크리스트구분체크 항목완료 기준GSLBDNS TTL 30초 적용장애 전환 시 재조회 확인GSLB정상 VIP만 응답센터 VIP 강제 장애 테스트 성공VIP서비스 Port 443 구성인증서 및 SSL 정책 확인Pool업무별 Pool 분리Marketing, SingleView 등 역할별 분리Health Check/health/l4 구성200 OK + Body OK 확인Failover5초 × 3회 장애 감지약 15초 내 Member 제외PersistenceJSESSIONID/Cookie 기반 검토세션 유지 정상 확인Sticky TimeoutSession보다 길게 설정60분 세션이면 70분 적용Idle Timeout120초 적용KeepAlive와 충돌 없음HeaderX-Forwarded-For/GUID 전달AP 로그에서 확인 가능Security관리 접근 제한관리망·개인계정·변경이력 확인MonitoringVIP/Pool/Member 지표 수집대시보드와 알림 구성운영Drain 배포 절차 수립점검 중 신규 요청 차단 검증검증600/1,200/1,800 TPS 테스트성능시험 결과로 최종값 보정

## 19. L4 설정 템플릿서비스명          : NSIGHT-MARKETING-HTTPSVIP               : 10.10.10.100:443GSLB Domain       : mkt.nsight.nh.localGSLB TTL          : 30sDefault Pool      : POOL_NSIGHT_MARKETING_WEBPool Members      : WEB01:443, WEB02:443, WEB03:443, WEB04:443Load Balancing    : Least Connection 또는 Round RobinHealth Monitor    : HTTPS GET /health/l4, Interval 5s, Timeout 2s, Fail 3, Rise 2Persistence       : Cookie/JSESSIONID, Timeout 70mTCP Idle Timeout  : 120sSSL Mode          : Offload / Re-encryption / Pass-through 중 보안정책에 따라 결정Header Forwarding : X-Forwarded-For, X-Forwarded-Proto, X-GUID, X-Center-IdLogging           : VIP, Pool, Member, Client IP, Result, Reset/Timeout, Health Check Event운영정책          : 점검 시 Drain 후 배포, 장애 시 Member 자동 제외, 센터 장애 시 GSLB 전환최종 유의사항: 본 설정값은 선도개발 및 성능테스트 전 표준 기준선이다. 최종값은 실제 장비, 네트워크 구조, SSL 처리 위치, Apache/Tomcat 구성, SingleView 대표 거래 성능테스트 결과를 기준으로 보정해야 한다.

> **용도**: L4·Apache · **연관 본문**: 18~20장

## GSLB·L4·WEB 설정표 — 실무 템플릿

본 부록은 **GSLB·L4·WEB** 영역의 표준 템플릿입니다. 상단 **NSIGHT 1차 표준 전제**와 함께 사용합니다.

### GSLB·L4·WEB 설정표

| 계층 | 항목 | 권고 |
|------|------|------|
| GSLB | TTL | 30초 |
| L4 | LB Method | Round Robin |
| L4 | Sticky | JSESSIONID, 70분 |
| L4 | Health | /actuator/health/l4, 5s/2s/Fail3 |
| L4 | Idle | 120초 |
| Apache | ProxyTimeout | 10s |
| Apache | KeepAlive | 120s |
| Apache | X-GUID | Header 전달 |

### 적용 절차

| 단계 | 작업 | 담당 |
|------|------|------|
| 1 | 권고값 초안 작성 | 아키텍처·WAS |
| 2 | DEV 환경 적용·단위 검증 | 개발 |
| 3 | STG 360/720 TPS 시험 | 성능시험 |
| 4 | 확정값 PRD 반영 | 운영·TA |
| 5 | 변경관리 이력 등록(부록 AB) | 운영 |

### 환경별 설정 차이

| 항목 | DEV | STG | PRD |
|------|-----|-----|-----|
| 수치 | 완화 가능 | 권고값 | **확정값** |
| leakDetection | 60s | 60s | 선택 |
| Actuator | 전체 | metrics+health | 제한 노출 |
| 로그 레벨 | DEBUG | INFO | INFO/WARN |

### 체크리스트

| # | 확인 |
|---|------|
| 1 | NSIGHT 1차 표준(21,600명·720 TPS) 전제 반영 |
| 2 | Timeout 계층 정합 (M 부록) |
| 3 | Pool 합산 ≤ DB max (V 부록) |
| 4 | 360/720 TPS 시험 합격 (X·Z 부록) |
| 5 | ENV rule-check 통과 |

### 트러블슈팅

| 증상 | 점검 | 조치 |
|------|------|------|
| p95 급증 | Thread·Pool·SQL | GUID Trace |
| Pool Pending | SQL p95 vs Pool 크기 | SQL 튜닝 우선 |
| Timeout 다발 | 계층 역전 여부 | M 부록 대조 |
| 센터 장애 | 잔여 TPS | W 부록 |

## 산정 공식 참조

```
동시요청자 = 전체사용자 × 동시요청률
TPS = 동시요청자 ÷ 3
AP = ⌈TPS ÷ 250⌉ (A-A)
Pool = max(30, min(TPS×0.15×1.3, Thread×30%))
```

## 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70% |
| 720 TPS | Thread/Pool 고갈 없음 |
| 1,080 TPS | Fail-fast 정상 |
| 센터 장애 | 잔여 TPS≥720 |

## CAP/ENV 연동

- 용량산정: `/oc/capacity.html` · `/api/oc/capacity/*`
- 환경설정: `/oc/env-002.html` · `/api/oc/env/rule-check`

## 연관 본문

| 본문 챕터 | 내용 |
|----------|------|
| 18~20 | GSLB·L4·WEB 상세 |

### 연관 부록

| 부록 | 내용 |
|------|------|
| A~B | 산정 입력·TPS |
| G~L | 솔루션 템플릿 |
| M | Timeout 매트릭스 |
| V~W | DB·센터 장애 |
| X~Z | 시험·검증 |
| AA~AB | 전환·변경 |

### 720 TPS 실무 예시

| 항목 | 산출 | 설정 연결 |
|------|------|----------|
| 사용자 | 21,600 | — |
| 동시요청(10%) | 2,160 | — |
| TPS | 720 | — |
| AP | 8 (A-A) | 8C/32G VM |
| Thread | 400~500 | maxThreads |
| Pool/VM | 50 | HikariCP |
| DB Session | 400 | max sessions |
| 잔여(센터 Down) | 1,000 | W 부록 |

### 작성·승인

| 역할 | 담당 | 산출물 |
|------|------|--------|
| PMO·업무 | 입력값 합의 | A 부록 |
| 아키텍처 | 산정·권고값 | 본 부록 |
| 성능시험 | 실측·확정값 | Z 부록 |
| 운영·TA | PRD 반영 | AB 부록 |


## 절별 상세

### N.1 GSLB

본 절은 **부록 N** — **GSLB** (GSLB·L4·WEB 설정표) NSIGHT 1차 표준 적용 기준입니다.

| 항목 | 권고 | 비고 |
|------|------|------|
| 기준 VM | 8 vCPU / 32GB | Scale-Out |
| TPS | 360/720/1080 | 피크 720 |
| p95 | 3초 이하 | SLA |
| 검증 | 360/720/1080 | 성능시험 |

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70% |
| 720 TPS | Thread/Pool 고갈 없음 |
| 1,080 TPS | Fail-fast 정상 |
| 센터 장애 | 잔여 TPS≥720 |

#### 주의사항

- 권고값은 출발점 — 선도개발·시험 후 확정
- 세션≠TPS, Pool≠Thread

### N.2 L4 VIP

본 절은 **부록 N** — **L4 VIP** (GSLB·L4·WEB 설정표) NSIGHT 1차 표준 적용 기준입니다.

| 항목 | 권고 | 비고 |
|------|------|------|
| 기준 VM | 8 vCPU / 32GB | Scale-Out |
| TPS | 360/720/1080 | 피크 720 |
| p95 | 3초 이하 | SLA |
| 검증 | 360/720/1080 | 성능시험 |

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70% |
| 720 TPS | Thread/Pool 고갈 없음 |
| 1,080 TPS | Fail-fast 정상 |
| 센터 장애 | 잔여 TPS≥720 |

#### 주의사항

- 권고값은 출발점 — 선도개발·시험 후 확정
- 세션≠TPS, Pool≠Thread

### N.3 Sticky·Health

본 절은 **부록 N** — **Sticky·Health** (GSLB·L4·WEB 설정표) NSIGHT 1차 표준 적용 기준입니다.

| 항목 | 권고 | 비고 |
|------|------|------|
| 기준 VM | 8 vCPU / 32GB | Scale-Out |
| TPS | 360/720/1080 | 피크 720 |
| p95 | 3초 이하 | SLA |
| 검증 | 360/720/1080 | 성능시험 |

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70% |
| 720 TPS | Thread/Pool 고갈 없음 |
| 1,080 TPS | Fail-fast 정상 |
| 센터 장애 | 잔여 TPS≥720 |

#### 주의사항

- 권고값은 출발점 — 선도개발·시험 후 확정
- 세션≠TPS, Pool≠Thread

### N.4 Apache

본 절은 **부록 N** — **Apache** (GSLB·L4·WEB 설정표) NSIGHT 1차 표준 적용 기준입니다.

| 항목 | 권고 | 비고 |
|------|------|------|
| 기준 VM | 8 vCPU / 32GB | Scale-Out |
| TPS | 360/720/1080 | 피크 720 |
| p95 | 3초 이하 | SLA |
| 검증 | 360/720/1080 | 성능시험 |

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70% |
| 720 TPS | Thread/Pool 고갈 없음 |
| 1,080 TPS | Fail-fast 정상 |
| 센터 장애 | 잔여 TPS≥720 |

#### 주의사항

- 권고값은 출발점 — 선도개발·시험 후 확정
- 세션≠TPS, Pool≠Thread

### N.5 검증

본 절은 **부록 N** — **검증** (GSLB·L4·WEB 설정표) NSIGHT 1차 표준 적용 기준입니다.

| 항목 | 권고 | 비고 |
|------|------|------|
| 기준 VM | 8 vCPU / 32GB | Scale-Out |
| TPS | 360/720/1080 | 피크 720 |
| p95 | 3초 이하 | SLA |
| 검증 | 360/720/1080 | 성능시험 |

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70% |
| 720 TPS | Thread/Pool 고갈 없음 |
| 1,080 TPS | Fail-fast 정상 |
| 센터 장애 | 잔여 TPS≥720 |

#### 주의사항

- 권고값은 출발점 — 선도개발·시험 후 확정
- 세션≠TPS, Pool≠Thread


---

[← 목차](../00-목차.md)
