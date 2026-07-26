# 50. 장애·Timeout·자원고갈 흐름

> 제7부. 성능검증과 운영 전환

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


**장애·Timeout·자원고갈** — 증상·원인·조치.

## 원문 기반 본문

## 19. 타임아웃 설정 검증 시나리오

| 검증 영역 | 검증 시나리오 | 성공 기준 | 확인 지표 |
|-----------|---------------|-----------|-----------|
| 정상 조회 | 일반 조회 360/720 TPS 부하 | p95 3초 이하, Timeout 급증 없음 | TPS, p95, CPU, SQL Time |
| DB 지연 | RDW SQL 지연 유도 | DB Query Timeout 후 AP Thread 회수 | SQL Timeout, Thread Active |
| Pool 고갈 | Hikari Pool 제한 후 부하 | 3초 내 Connection Timeout, 장애 전파 없음 | Hikari Pending, 오류코드 |
| 외부연계 지연 | CruzAPIM 응답 지연 | 5초 내 실패, Circuit Breaker 동작 | CB 상태, 연계 오류율 |
| 단말 Timeout | Client Read Timeout 유도 | 상태조회 API로 결과 확인 | GUID, 거래상태 |
| 세션 만료 | Idle Timeout 경과 | 재로그인 유도, 업무데이터 노출 없음 | 세션 로그, 보안 로그 |

---

## 16. 장애 유형별 점검 가이드증상가능 원인1차 확인조치 방향특정 사용자만 로그인 반복 실패Sticky 불일치, 세션 복제 실패Persistence Table, JSESSIONID, Tomcat SessionSticky 방식 재확인, 세션 객체 점검전체 응답 지연AP Thread/DB Pool/SQL 지연L4 Active Connection, Apache BusyWorker, APMDB Query Timeout, Pool 대기시간 확인VIP 접속 불가Virtual Server Down, 네트워크, SSL 오류VIP 상태, ARP, Route, 인증서VIP/Pool/SSL Profile 점검간헐적 502/503Member Flapping, Health Check 불안정Pool Member 상태 변경 이력Health Check URL과 Timeout 조정센터 전환 지연DNS TTL, Client Cache, GSLB Policydig/nslookup, 단말 DNS CacheTTL, 단말 재조회 정책 점검실제 Client IP 유실SNAT/Proxy Header 누락Apache Access Log, AP MDCX-Forwarded-For 설정 추가배포 중 오류 증가Drain 미적용, 기존 연결 강제 종료Member 상태, Active Connection배포 절차에 Drain 단계 추가

---

NSIGHT OOM 발생원인·진단·해결 가이드Java / Spring Boot / Tomcat / JVM 운영 장애 대응 기준구분내용문서 목적OOM 발생 원인을 Heap 부족으로 단순화하지 않고, Java Heap, Metaspace, Direct Memory, Native Thread, 세션, 대량 조회, 캐시, 큐 적체까지 분리하여 진단·해결 기준을 제공한다.

적용 대상NSIGHT 마케팅 AP, SingleView AP, 실시간처리 AP, EBM 엔진 AP, 배치성 Java 프로세스, 운영/개발/성능테스트 환경기준 환경IaaS VM 8 vCPU / 32GB, JVM Heap 일반 AP 12GB, SingleView AP 12~14GB, G1GC, p95 3초 응답 목표핵심 원칙OOM은 “메모리 부족”이 아니라 “메모리를 사용한 주체가 회수되지 않거나, 제한된 메모리 영역을 초과한 현상”으로 보아야 한다.

목차

1. OOM 정의와 NSIGHT 적용 관점

## 2. OOM 발생 구조와 장애 전파 흐름

## 3. OOM 유형별 원인·증상·해결 기준

## 4. NSIGHT 기준 메모리 예산 설계

## 5. 진단 절차: 증거 수집부터 원인 확정까지

## 6. JVM 설정 가이드

## 7. Spring Boot / Tomcat / Hikari / MyBatis 조치 기준

## 8. 개발자가 반드시 지켜야 할 메모리 안전 개발 기준

## 9. OOM 발생 시 운영 Runbook10. 성능테스트와 재발 방지 체크리스트

## 1. OOM 정의와 NSIGHT 적용 관점

### 1.1 OOM의 기본 정의OOM, OutOfMemoryError는 JVM이 객체·클래스 메타데이터·Direct Buffer·Thread Stack·Native 영역 등에 필요한 메모리를 더 이상 할당할 수 없을 때 발생한다. 운영에서는 “Heap이 부족하다”는 한 문장으로 결론내리면 안 된다. 같은 OOM이라도 원인 영역과 해결 방법이 완전히 다르기 때문이다.

구분의미운영 해석Java Heap OOM객체를 저장하는 Heap 영역에서 할당 실패대량 조회, 세션 비대화, 캐시 누수, 컬렉션 누수 가능성 확인Metaspace OOM클래스 메타데이터 영역 부족동적 클래스 생성, ClassLoader 누수, 과도한 프록시/리로드 확인Direct Memory OOMNIO/Netty/압축/파일 I/O Buffer 영역 부족파일 업로드·다운로드, 외부 연계, Kafka/HTTP Client Buffer 확인Native Thread OOMThread 생성 실패Tomcat Thread, Async ThreadPool, Scheduler, Xss, OS ulimit 확인Container/OS OOM KillJVM 내부 오류 없이 OS가 프로세스 종료RSS, cgroup limit, Direct/Thread/Metaspace 포함 Native 메모리 확인

### 1.2 NSIGHT에서 OOM을 중요하게 보는 이유NSIGHT 마케팅플랫폼은 단순 API 서버가 아니라 RDW 조회, 권한·마스킹, GUID 로그, 표준 전문, 오류코드, 감사로그, CruzAPIM 연계가 함께 동작하는 정보계 온라인 AP이다. 따라서 OOM은 단순 프로세스 장애가 아니라 사용자의 조회 지연, Timeout, L4 Health Check 실패, 센터 내 AP 제외, 장애 전환 부담으로 이어질 수 있다.

NSIGHT 요소OOM과의 관계관리 포인트장시간 세션Active Session 수가 증가하면 Heap 점유와 DeltaManager 복제량이 증가세션에는 ID·권한·센터 정보만 저장하고 조회 결과 저장 금지Single View 조회고객 기본/거래/실적/접촉 이력을 한 화면에서 조회하여 결과 객체가 커질 수 있음페이징, 조회 범위 제한, Fetch Size, DTO 경량화 적용RDW 조회대량 결과를 AP Heap에 모두 올리면 Heap OOM 가능SQL 제한, Row Limit, Streaming/ResultHandler 검토CruzAPIM 연계외부 연계 지연 시 Thread와 Buffer가 오래 점유될 수 있음Timeout, Circuit Breaker, Bulkhead, 제한된 Retry 적용감사·거래로그동기 로그 저장 또는 무제한 Queue는 메모리 적체 위험비동기 로그 Queue 크기 제한 및 Drop/Backpressure 정책 필요

### 1.3 OOM 판단의 최상위 원칙OOM은 “Heap을 늘리면 해결된다”로 판단하지 않는다. 먼저 어떤 메모리 영역에서 발생했는지 분리한다.

Heap Dump, GC Log, Thread Dump, Class Histogram, Native Memory, OS RSS를 동시에 수집한다.

OOM 발생 직후 무조건 재기동하지 않는다. 가능하면 L4에서 해당 AP를 제외하고 증거를 먼저 보존한다.

재기동은 복구 조치일 뿐 원인 해결이 아니다. 재발 방지 조치가 없으면 동일 시간대·동일 업무에서 반복된다.

NSIGHT는 8 vCPU / 32GB VM 기준에서 Heap과 Native 영역을 균형 있게 배분해야 한다.2. OOM 발생 구조와 장애 전파 흐름

### 2.1 OOM 발생 전 일반적인 징후징후관측 지표의미즉시 조치Old Gen 사용률 지속 상승Heap Used After GC 75~85% 이상 지속회수되지 않는 객체 증가 또는 캐시·세션 증가Heap Dump 예약 수집, 최근 배포/업무 집중 확인Full GC 빈도 증가Full GC 1분 내 반복 또는 Pause 장기화JVM이 메모리 회수에 실패하고 있음L4 Drain 검토, Class Histogram 수집응답시간 급증p95 3초 초과, Timeout 증가GC Pause 또는 Thread/DB Pool 대기 증가AP/DB/연계 구간별 소요시간 확인Thread 증가Thread Count 급증, unable to create native thread 전조무제한 ThreadPool 또는 외부 연계 대기Thread Dump와 ThreadPool 설정 확인RSS가 Heap보다 과도하게 큼OS RSS가 Xmx보다 훨씬 높음Direct Memory, Metaspace, Thread Stack, Native Leak 가능NMT, pmap, smaps 확인

### 2.2 장애 전파 흐름[사용자 요청 증가 / 대량 조회 / 세션 증가 / 캐시 누수]
↓[JVM Heap 또는 Native Memory 압박]
↓[GC 증가 / Thread 대기 / DB Pool 대기]
↓[응답시간 증가 / Client Timeout / Hikari Timeout]
↓[L4 Health Check 실패 또는 AP 장애 인지]
↓[트래픽이 잔여 AP로 이동]
↓

```
[잔여 AP 부하 증가 → 연쇄 OOM 가능]
```

↓[센터 단위 장애 또는 DR 전환 부담 증가]따라서 OOM 대응은 단일 JVM 재기동이 아니라 L4 트래픽 제외, 잔여 AP 처리량 확인, DB Session 증가 여부 확인, 동일 업무 요청 차단, 재발 원인 제거까지 포함해야 한다.3. OOM 유형별 원인·증상·해결 기준

### 3.1 Java Heap Space주요 원인NSIGHT 발생 예확인 방법해결 가이드대량 조회 결과를 한 번에 메모리에 적재Single View에서 거래이력·상담이력·캠페인이력 전체 조회Heap Dump, Dominator Tree, 큰 List/Map 확인조회기간 제한, 페이징, Row Limit, DTO 경량화, Streaming 처리세션 비대화HttpSession에 고객 조회 결과, 권한 목록, 캠페인 대상자 목록 저장Heap Dump에서 StandardSession, ConcurrentHashMap 확인세션에는 userId, branchId, roleId, maskingLevel, centerId 수준만 저장무제한 캐시static Map, Caffeine/Guava 최대 크기 미설정Heap Dump에서 Cache 객체, Map Entry 증가 확인maximumSize, expireAfterWrite, 업무별 캐시 기준 수립비동기 Queue 적체로그·감사·이벤트 Queue가 소비보다 빠르게 증가Thread Dump, Queue size metric 확인Queue 크기 제한, Reject/Drop/Backpressure 정책 적용ThreadLocal 누수요청 종료 후 사용자/전문/권한 객체가 ThreadLocal에 남음Heap Dump에서 ThreadLocalMap 확인Filter/Interceptor finally에서 remove() 강제

### 3.2 GC Overhead Limit Exceeded이 유형은 JVM이 대부분의 시간을 GC에 쓰지만 회수되는 메모리가 매우 적을 때 발생한다. 실제 장애에서는 OOM 직전 Full GC 반복, 응답시간 급증, CPU 상승, Tomcat Thread 적체로 나타난다.

판단 포인트확인 명령해석조치GC 시간이 급증하는가GC Log, jstat -gcutilOld 영역이 줄지 않으면 메모리 누수 가능Heap Dump 분석객체가 계속 증가하는가jcmd <pid> GC.class_histogram동일 클래스 객체 수가 계속 증가해당 업무·캐시·세션·Queue 추적최근 배포 후 발생했는가배포 이력, 트래픽 변화신규 기능의 객체 보관 가능성즉시 Rollback 또는 기능 차단

### 3.3 Metaspace OOM원인증상확인 방법해결 가이드ClassLoader 누수재배포 후 Metaspace 지속 증가jcmd VM.classloader_stats, GC Log Metaspace운영 중 반복 재배포 금지, 누수 라이브러리 확인동적 프록시 과다 생성런타임에 클래스가 계속 생성됨Class Histogram, Loaded Class Count동적 클래스 생성 캐시화, 프록시 생성 지점 점검MaxMetaspaceSize 과소기동 직후 또는 초기 부하에서 OOMJVM 옵션, Metaspace Used 확인MaxMetaspaceSize 1GB 기준, 필요 시 실측 보정

### 3.4 Direct Buffer Memory / Native Memory OOM원인NSIGHT 발생 예확인 방법해결 가이드Direct Buffer 과다대용량 파일 업로드/다운로드, 압축, NIO ClientNMT, RSS, DirectBuffer class 확인MaxDirectMemorySize 설정, Streaming 처리, 파일 전용 WAS 분리 검토HTTP Client Buffer 누수CruzAPIM 연계 응답 Body 미해제 또는 대기Netty/HTTP Client metric, NMT응답 Body close, Connection Pool 제한, Read Timeout 적용Thread Stack 증가Tomcat maxThreads 과대, Async Thread 무제한Thread Count, Xss, ulimit 확인Thread 수 제한, Xss 512k 기준, 비동기 Pool bounded 적용APM/Native Agent 메모리APM Agent 또는 보안 Agent Native 사용량 증가RSS와 Heap 차이, NMT 기타 영역Agent 버전 확인, Agent 메모리 제한, 벤더 점검

### 3.5 Unable to Create Native Thread이 오류는 Heap 부족이 아니라 OS 또는 JVM Native 영역에서 새 Thread Stack을 만들 수 없을 때 발생한다. Tomcat maxThreads, Async Executor, Scheduler, Kafka Consumer, Batch 병렬도, Xss, OS ulimit을 함께 확인해야 한다.

점검 항목권장 기준설정 위치비고Tomcat maxThreads8 Core 기준 300~500 범위에서 검증server.xml 또는 application.ymlp95 3초가 아니라 평균 응답시간 기준으로 산정Thread Stack -Xss512k 기준 검토setenv.sh / JVM 옵션Thread 수가 많을수록 Native 메모리 증가Async Executorcore/max/queue 명시ThreadPoolConfig무제한 Executor 금지OS ulimit프로세스/사용자별 nproc 확인/etc/security/limits.confThread 생성 실패 시 필수 확인

## 4. NSIGHT 기준 메모리 예산 설계4.1 8 vCPU / 32GB VM 메모리 배분 원칙32GB VM에서 Heap을 무조건 크게 잡으면 안정적인 것이 아니다. Heap 외에도 OS, Metaspace, Code Cache, Thread Stack, Direct Memory, APM Agent, File Cache, Native Library가 필요하다. 따라서 NSIGHT 기준에서는 일반 AP 12GB, SingleView AP 12~14GB를 기준으로 하고, 성능테스트에서 보정한다.

메모리 영역일반 AP 기준SingleView AP 기준설계 설명Java Heap12GB12~14GB업무 객체, DTO, 세션, 캐시, 요청/응답 객체 저장Metaspace최대 1GB최대 1GBSpring Bean, 프록시, MyBatis, 라이브러리 클래스 메타데이터Code Cache256MB256MBJIT 컴파일 코드 영역Thread Stack약 200~300MB약 150~300MBXss 512k × 활성 Thread 수 기준Direct Memory512MB~2GB512MB~2GBNIO, HTTP Client, 파일/압축 BufferOS/APM/Native/여유12~16GB10~14GBLinux, APM Agent, 보안 Agent, 파일 캐시, 장애 시 여유

### 4.2 세션 크기와 Heap 영향 산정세션당 평균 크기28,000 세션 기준 Heap 점유센터 내 2노드 복제 고려판단2KB약 56MB약 112MB매우 안정적5KB약 140MB약 280MB권장 범위20KB약 560MB약 1.1GB주의 필요50KB약 1.4GB약 2.8GB위험. 조회 결과 저장 의심100KB약 2.8GB약 5.6GB금지. 세션 비대화로 OOM 가능세션은 TPS를 직접 증가시키지는 않지만 Active Session 수와 세션 객체 크기를 증가시켜 Heap과 DeltaManager 복제량을 증가시킨다. 특히 장시간 세션을 적용할 경우 세션 객체 크기 통제가 OOM 예방의 핵심이다.

## 5. 진단 절차: 증거 수집부터 원인 확정까지

### 5.1 OOM 발생 직후 수집해야 할 증거증거목적수집 명령/위치주의사항Heap DumpHeap 객체 보유자 분석-XX:+HeapDumpOnOutOfMemoryError 또는 jcmd GC.heap_dump대용량 파일이므로 저장공간 확보GC LogOOM 전 메모리 변화와 Full GC 확인-Xlog:gc*:file=...

배포 시점부터 누적 보관Thread DumpThread 대기, Deadlock, 외부 연계 대기 확인jcmd <pid> Thread.print3회 이상 간격 수집 권장Class Histogram큰 객체 유형 빠른 확인jcmd <pid> GC.class_histogramHeap Dump 전 빠른 판단에 유용Native MemoryHeap 외 메모리 확인jcmd <pid> VM.native_memory summaryNMT 사전 활성화 필요OS 로그OOM Killer 여부 확인dmesg, /var/log/messagesJVM 로그 없이 죽으면 필수AP 로그/GUID어떤 거래에서 폭증했는지 확인거래로그, 오류로그, APM TraceGUID, serviceId, interfaceId 기준 검색

### 5.2 진단 명령 템플릿# 1) Java 프로세스 확인jps -lvps -ef | grep java# 2) Heap 사용 현황jcmd <pid> GC.heap_infojstat -gcutil <pid> 1000 10# 3) Class Histogram 빠른 확인jcmd <pid> GC.class_histogram > /logs/dump/class-histo-$(date +%Y%m%d%H%M).txt# 4) Heap Dump 수동 생성jcmd <pid> GC.heap_dump /logs/dump/heap-$(date +%Y%m%d%H%M).hprof# 5) Thread Dumpjcmd <pid> Thread.print > /logs/dump/thread-$(date +%Y%m%d%H%M).txt# 6) Native Memory Tracking, 사전 옵션 필요jcmd <pid> VM.native_memory summary > /logs/dump/nmt-summary.txtjcmd <pid> VM.native_memory detail > /logs/dump/nmt-detail.txt# 7) OS 관점 메모리pmap -x <pid> | tail -20cat /proc/<pid>/status | egrep 'VmRSS|VmSize|Threads'dmesg -T | egrep -i 'killed process|out of memory|oom' | tail -5

### 05.3 Heap Dump 분석 절차단계분석 내용판단 기준조치 연결1단계Dominator Tree 상위 객체 확인상위 객체가 List/Map/Session/Cache/DTO인지 확인업무 기능 또는 공통모듈 식별2단계Retained Heap 기준 정렬객체가 실제로 붙잡고 있는 전체 메모리 크기 확인누수 주체 확정3단계Path to GC Roots 확인누가 객체를 참조하고 있는지 확인static Map, ThreadLocal, Session, Queue 판별4단계Class Histogram 비교정상 시점과 장애 시점 객체 수 비교증가 클래스 추적5단계GUID/서비스 로그 연결OOM 직전 대량 호출된 serviceId 확인업무별 조회 제한 또는 코드 수정

## 6. JVM 설정 가이드

### 6.1 공통 JVM 옵션 표준OOM은 발생 후 분석 가능성이 중요하므로 Heap Dump, GC Log, Error File은 운영 배포 표준에 포함해야 한다. 단, Heap Dump 저장 경로는 충분한 디스크 공간과 접근권한을 사전에 확보해야 한다.# 일반 마케팅 AP 예시, 8 vCPU / 32GB

```shell
-Xms12g
-Xmx12g
-Xss512k
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:MetaspaceSize=256m
-XX:MaxMetaspaceSize=1g
-XX:ReservedCodeCacheSize=256m
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/logs/dump
-XX:ErrorFile=/logs/dump/hs_err_pid%p.log
-XX:+ExitOnOutOfMemoryError
-Xlog:gc*:file=/logs/gc/nsight
-gc-%t.log:time,uptime,level,tags:filecount=20,filesize=100M-Dfile.encoding=UTF
-8-Duser.timezone=Asia/Seoul
-Djava.security.egd=file:/dev/./urandom
```


### 6.2 Native Memory Tracking 적용 기준Direct Memory, Thread Stack, Metaspace, APM Agent 등 Heap 외 메모리 문제를 분석하려면 Native Memory Tracking을 사전에 켜야 한다. 운영 전체 상시 적용은 오버헤드를 검토하고, 성능테스트·선도개발·장애 재현 환경에서는 적용을 권장한다.# 성능테스트/장애 재현 환경 권장-XX:NativeMemoryTracking=summary# 상세 분석이 필요한 경우-XX:NativeMemoryTracking=detail# 기동 후 기준점 저장jcmd <pid> VM.native_memory baseline# 일정 시간 후 증가분 확인jcmd <pid> VM.native_memory summary.diffjcmd <pid> VM.native_memory detail.diff

### 6.3 옵션별 의미와 운영 판단옵션권장값의미운영 판단-Xms / -Xmx동일값 권장Heap 초기/최대 크기운영 중 Heap 확장/축소 비용 제거-Xss512k 기준Thread Stack 크기Thread가 많을수록 Native 메모리 증가UseG1GC활성화대용량 Heap과 Pause 목표 관리온라인 AP 기본 기준MaxGCPauseMillis200ms 기준GC Pause 목표값목표일 뿐 보장값 아님. GC Log로 검증HeapDumpOnOutOfMemoryError활성화OOM 시 Heap Dump 생성디스크 용량 필수ExitOnOutOfMemoryError활성화 검토OOM 발생 시 JVM 종료비정상 상태 지속보다 빠른 재기동 유도. HA 전제 필요MaxMetaspaceSize1GB 기준Metaspace 상한ClassLoader 누수 조기 감지 가능MaxDirectMemorySize필요 시 512m~2gDirect Buffer 상한파일/연계 많은 AP는 실측 후 지정

## 7. Spring Boot / Tomcat / Hikari / MyBatis 조치 기준

### 7.1 Tomcat Thread와 OOMTomcat Thread를 늘리면 동시 처리량이 늘어나는 것처럼 보이지만, 실제로는 Thread Stack과 요청 객체, DB Connection 대기가 함께 증가한다. Thread 증가는 Heap과 Native Memory를 동시에 압박할 수 있다.

설정8 Core 기준OOM 관점 위험가이드server.tomcat.threads.max300~500과대 설정 시 Thread Stack·요청 객체 증가평균 응답시간과 DB Pool을 함께 보고 조정acceptCount300~500과대 설정 시 대기 요청 증가사용자 Timeout보다 서버 Queue가 길어지지 않게 관리maxConnections적정 상한 관리연결만 많고 Thread/DB가 부족하면 적체L4/Apache KeepAlive와 함께 조정connection-timeout10~15초 이하느린 연결이 Thread를 오래 점유상위 Web/L4 Timeout과 정합성 유지

### 7.2 HikariCP와 DB PoolHikari connectionTimeout은 SQL 실행시간이 아니라 Pool에서 DB Connection을 얻기 위해 기다리는 시간이다. DB Pool이 고갈되면 Tomcat Thread가 대기하고, 대기 요청 객체가 Heap에 쌓이면서 OOM으로 이어질 수 있다.

항목일반 APSingleView APOOM 예방 기준maximumPoolSize30~5040~60DB 세션 총량을 AP 수량과 곱해서 검증minimumIdle10~1515~20피크 진입 지연 방지connectionTimeout2~3초2~3초Pool 고갈을 빠르게 실패 처리leakDetectionThreshold성능테스트에서 검토성능테스트에서 검토Connection 반환 누락 탐지SQL Timeout2~5초2~5초오래 걸리는 SQL이 Pool을 붙잡지 않게 함

### 7.3 MyBatis / SQL 조회 결과 메모리 통제위험 패턴문제표준 조치selectList()로 대량 결과 전체 적재ResultSet 전체가 List/DTO로 Heap에 올라감페이징, Row Limit, 조회기간 제한, fetchSize 적용엑셀 다운로드 전체 메모리 생성Workbook/byte[]가 Heap을 크게 점유Streaming Excel, 파일 임시저장, 다운로드 전용 AP 검토N+1 조회DTO와 SQL 호출이 과도하게 증가Join/Batch 조회 최적화, Mapper 리뷰불필요한 컬럼 조회객체 크기 증가화면 표시 컬럼 중심 DTO 설계분석성 SQL을 RDW에서 실행온라인 RDW 자원 경합 및 AP 대기 증가ADW 전용 처리 또는 비동기 전환

### 7.4 Spring Boot 설정 예시server:  servlet:    session:      timeout: 60m      cookie:        http-only: true        secure: true        same-site: Lax  tomcat:    threads:      max: 400      min-spare: 100    accept-count: 300    connection-timeout: 15000spring:  datasource:    rdw:      hikari:        maximum-pool-size: 50        minimum-idle: 15        connection-timeout: 3000        validation-timeout: 3000        idle-timeout: 600000        max-lifetime: 1800000  transaction:    default-timeout: 5

```yaml
mybatis:
  configuration:    default-statement-timeout: 3    default-fetch-size: 2008. 개발자가 반드시 지켜야 할 메모리 안전 개발 기준

### 8.1 금지 패턴과 대체 패턴금지 패턴왜 위험한가대체 기준HttpSession에 조회 결과 저장세션 수만큼 결과 객체가 장시간 생존세션에는 식별자·권한·마스킹 수준만 저장static Map 캐시 무제한 사용GC Root에 걸려 회수되지 않음Caffeine/Ehcache 등 size/TTL 있는 캐시 사용ThreadLocal remove 누락Tomcat Thread 재사용 시 객체가 계속 남음try/finally에서 remove() 강제전체 목록 조회 후 Java 필터링DB에서 줄일 수 있는 데이터를 Heap에 올림SQL 조건, 페이징, Row Limit 적용대용량 파일을 byte[]로 한 번에 생성Heap 또는 Direct Memory 급증Streaming, 임시파일, 전용 다운로드 서버 검토무제한 Executor/Queue처리 지연 시 Queue가 Heap을 점유bounded queue, rejection policy, backpressure 적용

### 8.2 코드 예시: ThreadLocal 정리// Bad: 요청 종료 후 ThreadLocal이 남을 수 있음MdcContext.set(context);service.execute();// Good: 반드시 finally에서 제거try {    MdcContext.set(context);    service.execute();} finally {    MdcContext.clear();    MDC.clear();}

### 8.3 코드 예시: 세션 저장 기준// Bad: 조회 결과를 세션에 저장 금지session.setAttribute("singleViewResult", resultList);// Good: 최소 인증·권한 정보만 저장session.setAttribute("loginUser", new LoginUserSession(    userId,    branchId,    roleId,    maskingLevel,    centerId));

### 8.4 코드 예시: 캐시 크기 제한// Bad: 무제한 static Mapprivate static final Map<String, Object> CACHE = new ConcurrentHashMap<>();// Good: 크기와 만료시간을 가진 캐시Cache<String, CustomerSummary> customerCache = Caffeine.new

Builder()    .maximumSize(100_000)    .expireAfterWrite(Duration.ofMinutes(10))    .recordStats()    .build();

### 8.5 코드 리뷰 질문검토 질문합격 기준이 기능이 조회 결과를 세션이나 static 변수에 저장하는가?저장하지 않는다. 필요 시 키 또는 최소 상태만 저장한다.

목록 조회는 최대 건수와 기간 제한이 있는가?pageSize, 기간, 업무별 상한이 명확하다.

캐시는 size와 TTL이 있는가?maximumSize/TTL/eviction metric이 있다.

ThreadLocal은 반드시 제거되는가?try/finally 또는 공통 Filter에서 remove 처리한다.

대용량 파일은 Streaming 처리하는가?byte[] 전체 생성 없이 Stream 또는 임시파일 방식이다.

비동기 Queue는 유한한가?Queue size와 rejection policy가 명시되어 있다.9. OOM 발생 시 운영 Runbook

### 9.1 초동 조치 원칙순서조치목적담당1장애 AP를 L4 Pool에서 Drain 또는 Disable신규 요청 차단 및 연쇄 장애 방지인프라/운영2현재 프로세스가 살아 있으면 Heap/Thread/Class/NMT 수집원인 분석 증거 확보AP/운영3GC Log, AP Log, hs_err, hprof 파일 보존재발 분석 근거 확보AP/운영4잔여 AP CPU/Heap/Thread/DB Pool 확인트래픽 이동에 따른 2차 장애 예방운영/인프라5필요 시 해당 기능 또는 대량 조회 차단원인 업무의 부하 유입 차단업무/AP6재기동 후 정상 여부 확인서비스 복구운영7원인 분석 및 재발 방지 조치 등록동일 장애 반복 방지AP/AA/운영

### 9.2 OOM 유형별 긴급 조치유형긴급 조치영구 조치Heap OOMHeap Dump 확보 후 재기동, 대량 조회/기능 차단세션/캐시/조회 결과 원인 제거, 페이징·제한 적용Metaspace OOMClassLoader 통계 확보 후 재기동반복 재배포 정책 개선, 동적 클래스/프록시 누수 수정Direct Memory OOMNMT/RSS 확인, 파일/연계 기능 차단Streaming 적용, Direct Memory 상한/Buffer 정책 조정Native Thread OOMThread Dump/Thread Count 확인, 신규 요청 차단ThreadPool 제한, Xss 조정, ulimit 확인OS OOM Killdmesg와 hs_err 확인, 프로세스 재기동Heap+Native 메모리 예산 재조정, cgroup/VM 메모리 검증

### 9.3 장애 보고서 템플릿항목작성 내용장애 발생 시각최초 알림, 사용자 영향 시작, 복구 완료 시각영향 범위센터, AP 인스턴스, 업무 서비스, 사용자 영향, 오류 건수오류 메시지java.lang.

OutOfMemoryError 세부 메시지와 로그 위치수집 증거Heap Dump, GC Log, Thread Dump, Class Histogram, NMT, OS 로그1차 원인Heap/Metaspace/Direct/Thread/OS 중 어느 유형인지상세 원인세션, 캐시, 대량 조회, ThreadLocal, Queue, 파일, 외부 연계 등임시 조치L4 제외, 재기동, 기능 차단, Rollback 등영구 조치코드 수정, 설정 조정, 모니터링 추가, 테스트 보강재발 방지 검증성능테스트, Heap 증가 추세, GC Pause, 오류율 재확인

## 10. 성능테스트와 재발 방지 체크리스트

### 10.1 성능테스트 시 OOM 검증 시나리오시나리오목적성공 기준360 TPS 기본 부하 1시간기본 운영 부하 안정성 확인Heap After GC 안정, Full GC 없음 또는 극소, 오류율 기준 이하720 TPS 피크 부하 1시간피크 설계 기준 검증p95 3초 이내, GC Pause p95 200ms 근접 관리1,080 TPS 스트레스한계 부하와 장애 전파 확인OOM 없이 임계치 초과 구간 식별, L4/Runbook 검증장시간 세션 유지26,000~28,000 세션 규모 확인세션 크기 통제, Heap 증가 추세 안정대량 조회 방어조회기간/건수 제한 검증무제한 조회 차단, 오류코드 정상 반환파일 다운로드대용량 다운로드 메모리 영향 확인Heap 급증 없이 Streaming 처리

### 10.2 운영 모니터링 임계치지표WarningCritical조치Heap Used After GC70% 이상 지속85% 이상 지속Heap Dump 예약, 기능별 객체 증가 확인Full GC10분 내 반복1분 내 반복 또는 장기 PauseL4 Drain, Heap/Class Histogram 수집GC Pausep95 200ms 초과p95 500ms 초과GC Log 분석, 객체 생성량 확인Thread Count평상시 대비 30% 증가maxThreads 근접Thread Dump, 외부 연계 대기 확인Hikari ActivePool의 70% 이상Pool 고갈 또는 timeoutSQL/DB 병목, Connection 반환 누락 확인RSS - Heap 차이정상 대비 증가VM 메모리 한계 근접NMT/Direct/Thread/Agent 확인Session Count평상시 대비 30% 증가설계 세션 초과세션 Timeout/크기/DeltaManager 영향 확인

### 10.3 최종 체크리스트구분체크 항목완료JVMHeap Dump, GC Log, Error File 경로가 운영 표준에 반영되었는가□JVMXmx가 VM 메모리 전체를 과점하지 않고 Native 영역 여유를 남겼는가□TomcatmaxThreads가 DB Pool, 평균 응답시간, Xss와 함께 산정되었는가□Session세션 객체 크기와 저장 항목이 표준화되었는가□DB/MyBatis대량 조회 제한, Fetch Size, Query Timeout이 적용되었는가□Cache모든 캐시에 maximumSize와 TTL이 설정되었는가□Async모든 Queue와 ThreadPool이 bounded 구조인가□운영OOM Runbook과 L4 제외 절차가 정의되었는가□모니터링Heap After GC, Full GC, RSS, Thread, Pool, Session 지표가 대시보드에 있는가□검증360/720/1,080 TPS와 장시간 세션 테스트에서 OOM이 재현되지 않는가□마무리말OOM은 단순히 메모리가 부족해서 생기는 장애가 아니라, 업무 특성·조회 방식·세션 정책·캐시 정책·Thread 설계·DB Pool·연계 Timeout·운영 모니터링이 함께 실패했을 때 드러나는 런타임 장애이다. 따라서 NSIGHT에서는 OOM 대응을 JVM 옵션 조정으로 끝내지 않고, 설계 단계에서 대량 조회를 제한하고, 세션을 작게 유지하며, 캐시와 Queue를 유한하게 만들고, 운영 단계에서 Heap Dump와 GC Log를 반드시 수집할 수 있는 구조로 만들어야 한다.

핵심은 “Heap을 키우는 것”이 아니라 “무엇이 메모리를 붙잡고 있는지 증명하고, 그 객체가 더 이상 오래 살아남지 않도록 구조를 바꾸는 것”이다.

참고 기준구분참고 내용Oracle Java SE 21 Troubleshooting GuideOutOfMemoryError, Heap Dump, Native Memory, 진단 도구 기준Oracle jcmd Command 문서jcmd를 통한 JVM 진단 명령 수행 기준Oracle Java SE 21 G1GC Tuning GuideG1GC 기본 튜닝과 Pause 목표 설정 기준NSIGHT 용량산정 기준3,600개 지점, 21,600명, 26,000~28,000 세션, 360/720/1,080 TPS 기준NSIGHT 환경설정 기준안JVM, Tomcat, Spring Boot, HikariCP, MyBatis, Timeout 설정 기준NSIGHT 개발표준정의서계층구조, 표준 전문, 오류처리, 로그/GUID, 개발 표준 기준

## 절별 상세

### 50.1 Tomcat Thread 포화

본 절(**Tomcat Thread 포화**)은 장애 흐름 영역에서 **장애 대응** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

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
- 운영 반영 전 **ENV rule-check** 및 성능시험 합격 필수

#### 운영 참고

장애 분석: GUID E2E Trace → Thread Dump → Hikari Pending → SQL p95 → GC Log 순.

### 50.2 DB Pool 고갈

본 절(**DB Pool 고갈**)은 장애 흐름 영역에서 **장애 대응** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

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
- 운영 반영 전 **ENV rule-check** 및 성능시험 합격 필수

#### 운영 참고

장애 분석: GUID E2E Trace → Thread Dump → Hikari Pending → SQL p95 → GC Log 순.

### 50.3 DB Connection Timeout

본 절(**DB Connection Timeout**)은 장애 흐름 영역에서 **장애 대응** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

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
- 운영 반영 전 **ENV rule-check** 및 성능시험 합격 필수

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: Runbook

### 50.4 Slow SQL·DB Lock

본 절(**Slow SQL·DB Lock**)은 장애 흐름 영역에서 **장애 대응** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 등급 | 기준 | 조치 |
|------|------|------|
| 정상 | p95 ≤1s | 유지 |
| 주의 | p95 1~2s | 튜닝 |
| 위험 | p95 >2s | 즉시 튜닝·ADW |

#### 설정 예시

APM Slow SQL · SQL_ID · GUID 연계 수집.

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
- 운영 반영 전 **ENV rule-check** 및 성능시험 합격 필수

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: Runbook

### 50.5 CPU 포화

본 절(**CPU 포화**)은 장애 흐름 영역에서 **장애 대응** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 장애 대응 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | Runbook |
| 핵심 | 증상·원인·조치 |

#### 설정 예시

**설정 파일**: `Runbook` · **핵심 항목**: 증상·원인·조치

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
- 운영 반영 전 **ENV rule-check** 및 성능시험 합격 필수

#### 운영 참고

장애 분석: GUID E2E Trace → Thread Dump → Hikari Pending → SQL p95 → GC Log 순.

### 50.6 GC 압박

본 절(**GC 압박**)은 장애 흐름 영역에서 **장애 대응** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 일반 AP | SV AP | VM |
|------|---------|-------|----|
| Heap (Xms=Xmx) | 12GB | 14GB | 8C/32G |
| -Xss | 512KB | 512KB | 500 Thread 기준 |
| GC | G1GC | G1GC | Pause 목표 200ms |
| Metaspace Max | 1GB | 1GB | 8C/32G |

#### 설정 예시

```shell
-Xms12g -Xmx12g -Xss512k
-XX:+UseG1GC -XX:MaxGCPauseMillis=200
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/logs/dump
-XX:+ExitOnOutOfMemoryError
-Xlog:gc*,safepoint:file=/logs/gc/ap-gc-%t.log:time,uptime,level,tags:filecount=10,filesize=100M
-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Seoul
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
- 운영 반영 전 **ENV rule-check** 및 성능시험 합격 필수

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: Runbook

### 50.7 외부 API 장애

본 절(**외부 API 장애**)은 장애 흐름 영역에서 **장애 대응** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 장애 대응 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | Runbook |
| 핵심 | 증상·원인·조치 |

#### 설정 예시

**설정 파일**: `Runbook` · **핵심 항목**: 증상·원인·조치

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
- 운영 반영 전 **ENV rule-check** 및 성능시험 합격 필수

#### 운영 참고

장애 분석: GUID E2E Trace → Thread Dump → Hikari Pending → SQL p95 → GC Log 순.

### 50.8 Gateway 장애

본 절(**Gateway 장애**)은 장애 흐름 영역에서 **장애 대응** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 장애 대응 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | Runbook |
| 핵심 | 증상·원인·조치 |

#### 설정 예시

**설정 파일**: `Runbook` · **핵심 항목**: 증상·원인·조치

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
- 운영 반영 전 **ENV rule-check** 및 성능시험 합격 필수

#### 운영 참고

검증 도구: APM, `jcmd`, `jstat`, Hikari Metrics, Access Log(GUID), ENV rule-check.

장애 분석: GUID E2E Trace → Thread Dump → Hikari Pending → SQL p95 → GC Log 순.

### 50.9 Cache 장애

본 절(**Cache 장애**)은 장애 흐름 영역에서 **장애 대응** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

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
- 운영 반영 전 **ENV rule-check** 및 성능시험 합격 필수

#### 운영 참고

장애 분석: GUID E2E Trace → Thread Dump → Hikari Pending → SQL p95 → GC Log 순.

### 50.10 Kafka Lag 증가

본 절(**Kafka Lag 증가**)은 장애 흐름 영역에서 **장애 대응** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 지표 | Warning | Critical |
|------|---------|----------|
| Lag | 업무별 | DLQ |
| poll interval | ≥처리시간 | — |

#### 설정 예시

```yaml
spring.kafka.consumer.max-poll-interval-ms: 300000
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
- 운영 반영 전 **ENV rule-check** 및 성능시험 합격 필수

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: Runbook

### 50.11 특정 WAR 자원 독점

본 절(**특정 WAR 자원 독점**)은 장애 흐름 영역에서 **장애 대응** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 장애 대응 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | Runbook |
| 핵심 | 증상·원인·조치 |

#### 설정 예시

**설정 파일**: `Runbook` · **핵심 항목**: 증상·원인·조치

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
- 운영 반영 전 **ENV rule-check** 및 성능시험 합격 필수

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: Runbook

### 50.12 센터 장애

본 절(**센터 장애**)은 장애 흐름 영역에서 **장애 대응** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| TPS | AP 권장 | 잔여(1센터 Down) |
|-----|----------|------------------|
| 720 | 8대 | 4대×250=1000 |

#### 설정 예시

잔여 TPS ≥ **720** 필수.

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
- 운영 반영 전 **ENV rule-check** 및 성능시험 합격 필수

#### 운영 참고

장애 분석: GUID E2E Trace → Thread Dump → Hikari Pending → SQL p95 → GC Log 순.

### 50.13 복구 후 설정값 재검증

본 절(**복구 후 설정값 재검증**)은 장애 흐름 영역에서 **장애 대응** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 장애 대응 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | Runbook |
| 핵심 | 증상·원인·조치 |

#### 설정 예시

**설정 파일**: `Runbook` · **핵심 항목**: 증상·원인·조치

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
- 운영 반영 전 **ENV rule-check** 및 성능시험 합격 필수

#### 운영 참고

검증 도구: APM, `jcmd`, `jstat`, Hikari Metrics, Access Log(GUID), ENV rule-check.

장애 분석: GUID E2E Trace → Thread Dump → Hikari Pending → SQL p95 → GC Log 순.

---

[← 목차](./00-목차.md)
