# 부록 X. 성능시험 시나리오

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

NSIGHT Tomcat 설정 가이드WebTopSuite - L4 - Apache - Tomcat - Spring Boot - DB 기준농협상호금융 NSIGHT 정보계 아키텍처

문서 유형: WAS 환경설정 작업 가이드 / 적용 대상: 마케팅 AP, SingleView AP, 업무 서비스 AP

문서 개정 이력

버전일자작성 기준주요 내용v1.02026-05-30NSIGHT 환경설정 기준안Tomcat WAS 설정 기준, 세션/클러스터, 운영검증, 설정 템플릿 작성

목차

## 1. 문서 목적 및 적용 범위

## 2. Tomcat 적용 아키텍처 위치

## 3. 기준 용량 및 트래픽 전제

## 4. Tomcat 설정 기본 원칙

## 5. 설정 파일 구조 및 적용 위치

## 6. Connector 설정 가이드

## 7. Thread, Connection, Queue 설정 가이드

## 8. Timeout 및 KeepAlive 설정 가이드

## 9. 세션 설정 가이드

## 10. Tomcat Cluster / DeltaManager 설정 가이드

## 11. Spring Boot 내장 Tomcat 설정 가이드

## 12. JVM / GC 연계 설정 가이드

## 13. 로그, Header, Remote IP 설정 가이드

## 14. 보안 설정 가이드

## 15. 운영 모니터링 및 임계치

## 16. 성능테스트 및 장애 검증 시나리오

## 17. 설정 템플릿

## 18. 최종 적용 체크리스트

## 19. 참고 문서

## 1. 문서 목적 및 적용 범위

본 문서는 NSIGHT 마케팅플랫폼과 SingleView AP를 운영하기 위한 Tomcat WAS 설정 기준을 정의한다. Tomcat은 단순히 Java 애플리케이션을 실행하는 컨테이너가 아니라, 사용자 요청을 Thread로 받아 Spring Boot 업무처리, DB Connection Pool, 세션, 로그, 장애 감지와 연결하는 핵심 Runtime 계층이다.

본 문서는 다음 흐름에서 Tomcat 계층의 설정 기준을 다룬다.[WebTopSuite 단말]
↓[GSLB / L4]
↓[Apache Reverse Proxy]
↓[Tomcat WAS]
↓[Spring Boot Application]
↓[HikariCP / MyBatis / RDW·ADW]구분적용 범위비고대상 시스템마케팅 AP, SingleView AP, 업무 서비스 APIaaS VM 기반 Tomcat WAS대상 환경개발, 검증, 성능, 운영환경별 수치는 다르되 설정 항목은 동일하게 관리대상 버전Tomcat 10.1.x 기준Spring Boot 내장 Tomcat 또는 외장 Tomcat 모두 적용 가능주요 설정Connector, Thread, Connection, Timeout, Session, Cluster, Access Log, SecurityDB Pool과 Spring Transaction은 별도 문서와 연계검증 기준p95 응답시간, TPS, Thread 사용률, Queue 대기, 오류율, GC Pause선도개발 및 성능테스트로 보정

> **주의**: Tomcat 설정값은 서버 Core 수만으로 확정하면 안 된다. 실제 평균 응답시간, DB SQL Time, 외부 연계 대기시간, GC Pause, DB Pool 대기시간을 함께 측정해야 한다.2. Tomcat 적용 아키텍처 위치Tomcat은 채널과 업무 애플리케이션 사이에서 HTTP 요청을 수신하고, Worker Thread를 통해 Spring Boot 애플리케이션으로 전달한다. 따라서 Tomcat 설정은 L4, Apache, Spring Transaction, HikariCP, MyBatis Timeout과 계층적으로 맞춰야 한다.[사용자 / WebTopSuite]   - 요청 Timeout, 재시도 정책, GUID 생성
↓[L4 / GSLB]   - Health Check, Persistence, Idle Timeout
↓[Apache]   - Proxy Timeout, KeepAlive, Header 전달
↓[Tomcat]   - Connector, Thread, Connection, Session, AccessLog
↓[Spring Boot]   - Transaction Timeout, Validation, Logging, Error Handling
↓[DB / RDW]   - Connection Pool, SQL Timeout, Query Plan, DB Session연계 계층Tomcat과 맞춰야 할 항목정합성 기준WebTopSuiteRequest Timeout, Read Timeout사용자 대기시간은 Tomcat/Spring/DB Timeout보다 길게 설정L4Sticky Timeout, Idle Timeout, Health Check세션 유지시간보다 Sticky Timeout을 약간 길게 설정ApacheProxyTimeout, KeepAliveTimeout, Header 전달Tomcat connectionTimeout/keepAliveTimeout과 충돌하지 않도록 조정Spring BootTransaction Timeout, Session CookieTomcat Thread가 장시간 점유되지 않도록 Transaction을 짧게 설정HikariCPmaximumPoolSize, connectionTimeoutThread 수보다 DB Pool을 과도하게 키우지 않음MyBatis/DBStatement Timeout, Fetch SizeSQL Timeout이 Transaction Timeout보다 먼저 동작하도록 설계

## 3. 기준 용량 및 트래픽 전제

본 문서의 기본 기준은 NSIGHT 변경 기준인 3,600개 지점, 지점당 6명, 전체 사용자 21,600명이다. 세션은 전체 로그인 유지 규모이고, TPS와 Thread는 실제 동시 요청자 기준으로 산정한다.


| 항목 | 기본 기준 | 설명 |
|------|-----------|------|
| 지점 수 | 3,600개 | NSIGHT 기본 사용자 산정 기준 |
| 지점당 사용자 | 6명 | 영업점 사용 기준 |
| 전체 사용자 | 21,600명 | 3,600 × 6명 |
| 설계 세션 | 26,000~28,000 세션 | 전체 사용자 + 20~30% 여유율 |
| 목표 응답시간 | p95 3초 이하 | 사용자 체감 기준 |
| 기본 운영 TPS | 360 TPS | 동시 요청률 5% 기준 |
| 피크 설계 TPS | 720 TPS | 동시 요청률 10% 기준 |
| 스트레스 테스트 | 1,080 TPS | 동시 요청률 15% 기준 |
| 기준 VM | 8 vCPU / 32GB | 운영 안정성 중심 Scale-Out 단위 |
| VM당 처리 기준 | 200~250 TPS | 일반 200, SingleView 250 TPS 보수 기준 |


> **주의**: Tomcat maxThreads는 TPS × p95 3초로 바로 산정하지 않는다. p95 3초는 목표 응답시간이고, Thread 산정은 실제 평균 응답시간 1.0~1.2초 기준에 여유율을 더해 잡는다.4. Tomcat 설정 기본 원칙원칙설명적용 방법Thread는 충분하되 과도하게 키우지 않는다Thread가 너무 적으면 대기열이 증가하고, 너무 많으면 Context Switching과 메모리 사용량이 증가한다.8 Core 기준 400~500을 시작값으로 하고 성능테스트로 보정DB Pool과 함께 조정한다Thread가 많아도 DB Pool이 부족하면 Hikari 대기가 발생한다.

Tomcat Active Thread, Hikari Active/Wait, SQL Time을 함께 본다Timeout 계층을 일관되게 맞춘다하위 계층이 먼저 실패해야 원인을 정확히 알 수 있다.

DB Query Timeout < Transaction Timeout < Proxy/Client Timeout세션 데이터는 작게 유지한다세션 복제 시 큰 객체는 Heap, Network, GC 부하를 증가시킨다.

로그인 사용자, 권한, 센터 정보만 세션에 저장DeltaManager는 센터 내부 소규모 클러스터에만 적용한다All-to-all 복제 방식은 노드가 많으면 비용이 커진다.

센터 내부 2~4대 수준은 DeltaManager, 대규모는 BackupManager/Spring Session 검토운영 가능한 설정만 표준화한다설정값은 운영자가 감시하고 조치할 수 있어야 한다.

JMX, AccessLog, Health Check, Thread Dump 절차 포함

## 5. 설정 파일 구조 및 적용 위치설정 대상주요 파일/위치관리 주체비고Connector / ThreadTOMCAT_HOME/conf/server.xmlTA / WAS 운영외장 Tomcat 기준세션 Timeoutconf/web.xml, WEB-INF/web.xml, context.xml, application.ymlAA / WAS 운영우선순위와 배포방식 확인 필요Cluster / DeltaManagerserver.xml, context.xml, web.xmlTA / AA 공동외장 Tomcat WAR 배포 시 명확Access Logserver.xml Valve 설정WAS 운영GUID Header 기록 필요Remote IP 처리RemoteIpValve, Apache HeaderWEB/WAS 운영X-Forwarded-For 신뢰 범위 통제Spring Boot 내장 Tomcatapplication.ymlAA / 개발 리딩내장 Tomcat 기준JVM 옵션setenv.sh, systemd unit, 배포 스크립트TA / 운영Heap, GC, 로그 경로 관리운영 모니터링JMX, APM, Actuator운영 / 공통Thread, Session, GC, Error Rate 감시

## 6. Connector 설정 가이드Connector는 Tomcat이 HTTP 요청을 받아들이는 입구다. maxThreads, maxConnections, acceptCount, connectionTimeout, keepAliveTimeout은 하나의 세트로 조정해야 한다.


| 설정 항목 | 8 Core / 32GB | 16 Core | 32 Core | 설명 |
|-------------|---------------|---------|---------|------|
| protocol | Http11NioProtocol | 동일 | 동일 | 표준 NIO Connector |
| maxThreads | 400~500 | 800~1,000 | 1,500~1,800 | Worker Thread |
| minSpareThreads | 100 | 150~200 | 300~400 | 피크 진입 전 대기 Thread |
| acceptCount | 300~500 | 500~800 | 800~1,000 | Thread 포화 시 대기 큐 |
| maxConnections | 10,000 | 16,000 | 20,000 | 동시 연결 상한 |
| connectionTimeout | 8,000ms | 8,000ms | 8,000ms | 연결 후 요청 대기 |
| keepAliveTimeout | 120,000ms | 120,000ms | 120,000ms | KeepAlive 유지 |
| maxKeepAliveRequests | 1,000 | 1,000 | 1,000 | 연결당 최대 요청 |
| URIEncoding | UTF-8 | UTF-8 | UTF-8 | 한글 파라미터 |


> **주의**: 16 Core와 32 Core 기준은 Scale-Up 검토값이다. NSIGHT 정보계 온라인 AP는 장애 격리와 무중단 운영을 위해 8 Core / 32GB Scale-Out을 기본 단위로 보는 것이 안전하다.server.xml 예시 - 8 Core / 32GB 기준

```xml
<Connector port="8080"           protocol="org.apache.coyote.http11.

Http11NioProtocol"           maxThreads="500"           minSpareThreads="100"           acceptCount="500"           maxConnections="10000"           connectionTimeout="8000"           keepAliveTimeout="120000"           maxKeepAliveRequests="1000"           URIEncoding="UTF-8"           redirectPort="8443" />
```
7. Thread, Connection, Queue 설정 가이드

### 7.1 Thread 산정 방식Tomcat Thread는 사용자의 요청이 WAS 안에서 머무는 동안 점유된다. 순수 CPU 처리시간뿐 아니라 DB 조회, 외부 연계, 로그 처리, 권한 확인, 마스킹 처리 대기시간도 포함된다.

필요 Thread 수 = 목표 TPS × 평균 응답시간 × 여유율예: 250 TPS × 1.2초 × 1.3 = 390 Thread=> 8 Core 기준 maxThreads 400~500 권장구분설정 기준설명평균 응답시간 기준1.0~1.2초

Thread 산정에 사용하는 평균값p95 응답시간 목표3초 이하SLA 및 사용자 체감 기준여유율1.2~1.5순간 피크, GC, 연계 지연, DB 지연 반영권장 maxThreads산정값 + 운영 여유8 Core 기준 400~500관측 지표currentThreadsBusy, processingTime, requestCountJMX/APM으로 측정

### 7.2 maxConnections, maxThreads, acceptCount 관계요청 유입
↓maxConnections 범위 안에서 연결 수락
↓사용 가능한 maxThreads가 있으면 즉시 처리
↓maxThreads가 모두 사용 중이면 acceptCount 대기
↓대기열도 초과하면 연결 거부 또는 Timeout 발생항목의미장애 시 관찰 증상조정 방향maxThreads동시에 요청을 처리할 수 있는 Worker Thread 수currentThreadsBusy가 maxThreads에 근접Thread 증가 또는 응답시간/DB 병목 개선maxConnectionsTomcat이 동시에 유지할 수 있는 연결 수연결 수가 높고 처리량은 낮음KeepAlive, L4/Apache Idle Timeout 조정acceptCountThread가 가득 찼을 때 대기 큐대기 후 Client Timeout 증가Thread/DB/SQL 병목 우선 분석minSpareThreads미리 생성해 두는 대기 Thread피크 진입 초기에 지연피크 패턴에 맞게 상향

> **주의**: Thread만 늘리면 성능이 늘어난다고 보면 안 된다. DB Pool, SQL 수행시간, GC Pause, 외부 연계 대기시간이 개선되지 않으면 Thread 증가는 장애 증폭 요인이 된다.8. Timeout 및 KeepAlive 설정 가이드Timeout은 장애 전파를 차단하기 위한 계층적 보호 장치다. Tomcat Timeout은 WebTopSuite, Apache, L4, Spring Transaction, DB Query Timeout과 함께 설계해야 한다.


| 계층 | 권장값 | 설정 위치 | 정합성 기준 |
|------|--------|-----------|-------------|
| DB Query Timeout | 2~3초 | MyBatis/JDBC | 가장 먼저 실패 |
| Hikari Connection Timeout | 2~3초 | application.yml | Pool 대기 제한 |
| Spring Transaction Timeout | 4~5초 |

```java
@Transactional | 업무 트랜잭션 한도 |
| Tomcat connectionTimeout | 8초 | server.xml | 연결 후 요청 대기 |
| Apache Proxy Timeout | 10초 | httpd.conf | AP 응답 대기 |
| WebTopSuite Request Timeout | 15초 | Runtime Config | 사용자 체감 한도 |
| L4 Idle Timeout | 120초 | L4 설정 | KeepAlive 유지 |
```


권장 순서DB Query Timeout < Spring Transaction Timeout < Apache/WebTopSuite Timeout예시DB Query Timeout      : 3초Spring Transaction    : 5초Apache ProxyTimeout   : 10초WebTopSuite Timeout   : 15초Tomcat 항목권장값설명connectionTimeout8000ms연결 수립 후 요청 데이터가 들어오기를 기다리는 시간keepAliveTimeout120000msKeepAlive 연결을 유휴 상태로 유지하는 시간maxKeepAliveRequests1000하나의 연결에서 처리할 수 있는 최대 요청 수disableUploadTimeouttrue 또는 기본값 검토대용량 업로드 업무가 없으면 별도 완화 불필요connectionUploadTimeout업로드 업무별 별도파일 업로드가 있는 경우 별도 설계

## 9. 세션 설정 가이드세션 정책은 Tomcat Session Idle Timeout과 Application Absolute Session Timeout을 분리해야 한다. Idle Timeout은 사용자가 일정 시간 아무 요청도 하지 않을 때 세션을 제거하는 정책이고, Absolute Timeout은 계속 사용하더라도 업무일 기준 최대 사용시간을 제한하는 정책이다.

구분의미설정 위치NSIGHT 권장Session Idle Timeout유휴 시간이 지나면 세션 종료application.yml, web.xml, context.xml60분 기본, 업무 특성상 30~180분 검토Absolute Session Timeout계속 사용해도 최대시간 도달 시 강제 종료Filter, Interceptor, Security 공통모듈8시간 기본, 12시간 예외 검토L4 Sticky Timeout동일 사용자를 같은 WAS로 라우팅L4 설정세션보다 약간 길게. 60분 세션이면 70분WebTopSuite Center 유지선택 센터 L4 정보 유지WebTopSuite Client Config앱 실행 중 유지, 센터 장애 시 재조회센터 장애 시 세션센터 간 세션 유지 정책운영 정책기본 재로그인. 센터 간 복제는 기본 미적용Spring Boot application.yml 세션/쿠키 예시server:  servlet:    session:      timeout: 60m      cookie:        http-only: true        secure: true        same-site: Laxweb.xml 세션 예시 - 분 단위

```xml
<session-config>    <session-timeout>60</session-timeout></session-config>
```


> **주의**: 세션을 3시간으로 늘리면 TPS가 직접 증가하는 것은 아니지만 Active Session 수, Heap 점유, DeltaManager 복제량, GC 리스크가 증가한다. 세션에는 인증·권한·마스킹 등 최소 정보만 저장해야 한다.10. Tomcat Cluster / DeltaManager 설정 가이드NSIGHT 세션 아키텍처는 GSLB로 센터 L4를 선택하고, WebTopSuite가 선택된 센터 L4에 직접 접근하며, 센터 내부 Tomcat Cluster에서 DeltaManager로 세션을 복제하는 구조를 기본으로 한다. 센터 간 DeltaManager 복제는 기본 적용하지 않는다.[WebTopSuite]
↓ GSLB Lookup[센터 L4]
↓ Sticky / Persistence[Tomcat AP #1] ↔ [Tomcat AP #2] ↔ [Tomcat AP #3]          센터 내부 DeltaManager 세션 복제항목권장 기준설명적용 범위센터 내부 Tomcat Cluster센터 간 복제는 기본 미적용복제 방식DeltaManager소규모 All-to-all 복제대규모 노드BackupManager 또는 Spring Session 검토노드 수가 많으면 DeltaManager 비용 증가세션 객체Serializable 필수세션에 저장되는 객체는 직렬화 가능해야 함애플리케이션 선언<distributable/>WAR 배포 시 WEB-INF/web.xml에 명시라우팅 식별자jvmRoute 설정Sticky Session과 Failover 식별에 필요시간 동기화NTP 필수세션 만료, 로그, 장애분석 정합성 확보세션 데이터최소화고객 조회 결과, 대량 리스트, 전문 원문 저장 금지jvmRoute 예시<!-- server.xml: Engine에 jvmRoute 지정 --><Engine name="Catalina" defaultHost="localhost" jvmRoute="ap01">    ...</Engine><distributable/> 예시<!-- WEB-INF/web.xml -->

```xml
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"         version="5.0">    <distributable/></web-app>
```
Serializable 세션 객체 예시

```java
public class LoginUserSession implements java.io.

Serializable {    private static final long serialVersionUID = 1L;    private String userId;    private String branchId;    private String roleId;    private String auth

Level;    private String maskingLevel;    private String centerId;}
```
세션 저장 항목저장 여부사유userId, branchId가능사용자·지점 식별 정보roleId, authLevel가능권한 판단에 필요한 최소 정보maskingLevel가능개인정보 마스킹 기준centerId가능센터 직접 접근 및 운영 추적고객 SingleView 조회 결과금지개인정보·대량 객체·복제 부하 위험캠페인 대상자 목록금지Heap 점유와 네트워크 복제량 증가전문 원문 전체금지보안·메모리·감사 리스크

> **주의**: DeltaManager XML은 네트워크 인터페이스, 포트, 멤버십 방식, 보안 정책에 따라 현장값이 달라진다. 본 문서에는 최소 안전 템플릿과 점검 기준을 제공하고, 실제 server.xml은 인프라 표준과 WAS 표준에 맞춰 별도 확정해야 한다.11. Spring Boot 내장 Tomcat 설정 가이드Spring Boot 내장 Tomcat을 사용하는 경우 server.xml 대신 application.yml에서 주요 Tomcat 속성을 관리한다. 외장 Tomcat WAR 배포와 내장 Tomcat JAR 배포 방식은 설정 위치가 다르므로 프로젝트 표준 배포 방식을 먼저 확정해야 한다.

항목application.yml 속성8 Core 권장설명maxThreadsserver.tomcat.threads.max500Worker Thread 상한minSpareThreadsserver.tomcat.threads.min-spare100대기 ThreadacceptCountserver.tomcat.accept-count500Thread 포화 시 QueuemaxConnectionsserver.tomcat.max-connections10000동시 연결 상한connectionTimeoutserver.tomcat.connection-timeout8s연결 후 요청 대기시간accesslogserver.tomcat.accesslog.enabledtrue접속 로그 활성화basedirserver.tomcat.basedir/app/tomcat임시파일/로그 기준 경로Spring Boot application.yml 예시

```yaml
server:  tomcat:    threads:      max: 500      min-spare: 100    accept-count: 500    max-connections: 10000    connection-timeout: 8s    accesslog:      enabled: true      directory: /logs/access      prefix: tomcat_access      suffix: .log      pattern: '%h %l %u %t "%r" %s %b %D %{X-GUID}i'  servlet:    session:      timeout: 60m
```


> **주의**: 내장 Tomcat에서는 server.xml의 모든 Cluster 기능을 동일하게 적용하기 어렵다. DeltaManager 기반 외장 Tomcat 클러스터를 표준으로 할지, Spring Session 기반 세션 저장소를 표준으로 할지 아키텍처 의사결정이 필요하다.12. JVM / GC 연계 설정 가이드Tomcat Thread와 세션은 JVM Heap, Thread Stack, GC Pause와 직접 연결된다. 목표 응답시간 3초 기준에서는 GC Pause도 사용자 응답시간에 포함되므로 G1GC와 GC 로그 분석이 필수다.

구분일반 마케팅 APSingleView AP설명VM8 vCPU / 32GB8 vCPU / 32GB기본 Scale-Out 단위JVM Heap12GB14GB세션/조회 특성에 따라 보정Thread Stack512KB512KBThread 수 증가 시 메모리 영향 고려GCG1GCG1GC응답시간과 처리량 균형MaxGCPauseMillis200ms200ms목표값이며 보장값은 아님GC Log필수필수성능테스트와 운영 분석용Heap 사용률70% 이하 목표70% 이하 목표초과 시 Leak 또는 세션 비대화 점검JVM 옵션 예시 - 일반 마케팅 AP

```shell
-Xms12g
-Xmx12g
-Xss512k
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/logs/dump
-Xlog:gc*:file=/logs/gc/app
-gc-%t.log:time,uptime,level,tags:filecount=10,filesize=100M-Dfile.encoding=UTF
-8-Duser.timezone=Asia/Seoul
```
점검 항목정상 기준위험 신호조치GC Pause p95200ms 이하 목표500ms 이상 반복객체 생성량, 세션 크기, Heap 조정Full GC운영 중 거의 없음Full GC 발생Heap Dump 분석, 메모리 Leak 점검Heap 사용률70% 이하80% 이상 지속세션/캐시/대량 객체 점검Thread Count설정 범위 내 안정Thread 폭증DB/연계 대기, Deadlock, Timeout 점검CPU60~70% 이하80% 이상 지속Scale-Out 또는 병목 분석

## 13. 로그, Header, Remote IP 설정 가이드Tomcat Access Log는 장애 분석과 성능 분석의 1차 자료다. NSIGHT에서는 GUID, TraceId, 사용자 IP, 응답시간, 상태코드를 반드시 추적해야 한다.

로그 항목필수 여부설명Client IP필수Apache/L4 뒤에 있는 실제 사용자 IP 식별Request URI필수업무 요청 경로HTTP Status필수오류율 분석Response Time필수Tomcat 처리시간 분석X-GUID필수End-to-End 거래 추적TraceId권장AP 내부/연계 추적User-Agent선택단말/브라우저 분석Session ID원칙적 금지보안상 원문 노출 지양. 필요 시 Hash 처리AccessLogValve 예시

```xml
<Valve className="org.apache.catalina.valves.

AccessLogValve"       directory="/logs/tomcat/access"       prefix="access"       suffix=".log"       pattern="%h %l %u %t &quot;%r&quot; %s %b %D %{X-GUID}i %{X-Forwarded-For}i" />
```
RemoteIpValve 예시

```xml
<Valve className="org.apache.catalina.valves.

RemoteIpValve"       remoteIpHeader="x-forwarded-for"       protocolHeader="x-forwarded-proto"       internalProxies="10\.0\.0\.0/8|172\.16\.0\.0/12|192\.168\.0\.0/16" />
```


> **주의**: X-Forwarded-For는 신뢰 가능한 Apache/L4에서 전달한 값만 사용해야 한다. 외부 사용자가 임의로 보낸 Header를 그대로 신뢰하면 보안·감사 로그가 오염된다.

## 14. 보안 설정 가이드보안 항목권장 기준설정 위치설명Shutdown Port-1 또는 접근 제한server.xml원격 Shutdown 포트 비활성화 또는 OS 방화벽 제한Server Header노출 최소화Connector server 속성 / ProxyTomcat 버전 정보 노출 방지TRACE Method비활성화allowTrace=falseHTTP TRACE 차단Manager App운영 미배포 또는 접근 제한webapps / tomcat-users.xml운영 접근 통제Cookie SecuretrueSpring Boot / web.xmlHTTPS 전용 쿠키Cookie HttpOnlytrueSpring Boot / web.xmlXSS 세션 탈취 완화SameSiteLax 또는 None+SecureSpring BootCSRF 완화 및 크로스도메인 정책Directory Listing금지DefaultServlet 설정정적 파일 목록 노출 차단Error Page표준 오류 페이지web.xml / Spring Error HandlingStack Trace 노출 금지보안 관련 server.xml 예시<!-- TRACE 차단 -->

```xml
<Connector port="8080"           protocol="org.apache.coyote.http11.

Http11NioProtocol"           allowTrac

> *(원문 일부 발췌 — 전체는 znsight-capacity-word 참조)*

> **용도**: 성능시험 · **연관 본문**: 47장

## 성능시험 시나리오 — 실무 템플릿

본 부록은 **시나리오** 영역의 표준 템플릿입니다. 상단 **NSIGHT 1차 표준 전제**와 함께 사용합니다.

### 성능시험 시나리오

| # | 시나리오 | 부하 | 합격 기준 |
|---|----------|------|----------|
| 1 | 기본 | 360 TPS | p95≤3s, CPU≤70% |
| 2 | 피크 | 720 TPS | Pending=0 |
| 3 | 스트레스 | 1,080 TPS | Fail-fast |
| 4 | AP 1대 Down | 720 TPS | L4 failover |
| 5 | 센터 장애 | 720 TPS | 잔여≥720 |
| 6 | Slow SQL | 720 TPS | SQL timeout→Thread 회수 |
| 7 | Pool 고갈 | 720 TPS | connectionTimeout 3s |
| 8 | CruzAPIM 지연 | 720 TPS | CB Open |
| 9 | Rolling 배포 | 720 TPS | Drain·무중단 |
| 10 | OOM 모의 | — | Dump·재기동 |

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
| 47 | 시나리오 상세 |

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

### X.1 360 TPS

본 절은 **부록 X** — **360 TPS** (성능시험 시나리오) NSIGHT 1차 표준 적용 기준입니다.

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

### X.2 720 TPS

본 절은 **부록 X** — **720 TPS** (성능시험 시나리오) NSIGHT 1차 표준 적용 기준입니다.

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

### X.3 1080 TPS

본 절은 **부록 X** — **1080 TPS** (성능시험 시나리오) NSIGHT 1차 표준 적용 기준입니다.

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

### X.4 장애 시나리오

본 절은 **부록 X** — **장애 시나리오** (성능시험 시나리오) NSIGHT 1차 표준 적용 기준입니다.

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

### X.5 합격 기준

본 절은 **부록 X** — **합격 기준** (성능시험 시나리오) NSIGHT 1차 표준 적용 기준입니다.

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
