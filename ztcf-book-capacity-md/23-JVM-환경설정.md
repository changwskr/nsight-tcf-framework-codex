# 23. JVM 환경설정

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


**JVM** — Heap·G1GC·GC Log·OOM.

## 원문 기반 본문

NSIGHT JVM 셋팅 가이드Java / Tomcat / Spring Boot 기반 마케팅플랫폼 운영 기준구분기준문서 목적NSIGHT 마케팅플랫폼 및 SingleView AP의 JVM Heap, GC, 로그, 장애 대응, 모니터링 기준을 표준화한다.

적용 범위IaaS VM 기반 Tomcat/Spring Boot AP, 일반 마케팅 AP, SingleView AP, 연계/비동기 처리 AP기준 환경8 vCPU / 32GB VM을 기본 Scale-Out 단위로 하고, 16/32 Core는 특수·고부하 용도로 별도 검토한다.

성능 기준기본 360 TPS, 피크 720 TPS, 스트레스 1,080 TPS, p95 3초 이하를 기준으로 검증한다.

작성 기준값은 초기 표준 기준선이며, 선도개발 성능테스트와 운영 모니터링으로 최종 보정한다.

핵심 원칙: JVM 설정은 “메모리를 크게 주면 성능이 좋아진다”는 관점이 아니라, Heap·Thread·GC·DB 대기·로그·APM 오버헤드가 응답시간 안에서 균형 있게 동작하도록 조정하는 작업이다.

목차

1. JVM 설정의 위치와 역할

## 2. 기준 용량 및 산정 전제

## 3. JVM 설정 총괄 기준표

## 4. Heap Size 설계 기준

## 5. GC 전략 및 G1GC 설정 기준

## 6. GC Log / Heap Dump / Crash Dump 기준

## 7. Thread Stack / Native Memory / Direct Memory 기준

## 8. 업무 유형별 JVM 표준 템플릿9. setenv.sh / systemd / 환경변수 적용 방식

## 10. 모니터링 임계치 및 운영 점검

## 11. 성능테스트 및 장애 검증 시나리오

## 12. 장애 유형별 조치 가이드

## 13. 최종 적용 체크리스트

## 14. 참고 자료

## 1. JVM 설정의 위치와 역할JVM은 Tomcat/Spring Boot 애플리케이션이 실제로 실행되는 런타임 공간이다. WebTopSuite, L4, Apache가 요청을 전달하더라도 최종적으로 요청을 처리하는 것은 Tomcat Worker Thread와 JVM Heap, GC, DB Connection Pool, 로그/관측성 메커니즘이다.

따라서 JVM 설정은 단순히 -Xms, -Xmx 값을 정하는 것이 아니라, 온라인 응답시간, 세션 유지, GC Pause, 장애 추적, OOM 대응, 성능검증 기준을 함께 정의하는 운영 표준이다.


| 계층 | JVM과의 관계 | 검증 포인트 |
|------|-------------|-------------|
| WebTopSuite | Timeout 기준 | Client Timeout 이전 AP 정상 실패 |
| L4/Apache | 요청 전달 | Proxy Timeout > JVM 처리시간 |
| Tomcat | Thread·세션 | Thread 포화·세션 복제 부하 |
| Spring Boot | Tx·Pool | Transaction Timeout·Pool Wait |
| JVM | Heap·GC·Dump | GC Pause·Heap·OOM |

## 2. 기준 용량 및 산정 전제NSIGHT 기준은 세션 수와 동시 요청자를 분리해서 산정한다. 전체 사용자와 세션은 로그인 유지 규모이고, TPS·Thread·DB Pool·GC 부하는 실제 동시 요청자 기준으로 산정한다.

항목기준값JVM 설정 영향지점 수3,600개전체 사용자 및 세션 설계 기준지점당 사용자6명전체 사용자 21,600명 산정설계 세션26,000~28,000 세션세션 객체 크기와 Heap 점유 검증 필요기본 운영 TPS360 TPS일반 업무시간 JVM 안정성 기준피크 설계 TPS720 TPSThread/GC/DB Pool 기준선스트레스 TPS1,080 TPS한계 검증 및 장애 전파 차단 기준목표 응답시간p95 3초 이하GC Pause도 응답시간 안에 포함됨기준 VM8 vCPU / 32GBScale-Out 기본 단위GC 정책G1GC온라인 AP 표준 권장

> **주의**: 250 TPS/VM은 모든 거래가 평균 3초 걸려도 된다는 의미가 아니다. p95는 3초 이하를 목표로 하고, 실제 평균 응답시간은 1.0~1.5초 수준으로 관리되어야 Tomcat Thread와 CPU, GC가 안정적으로 동작한다.3. JVM 설정 총괄 기준표

| 영역 | 설정 항목 | 8 vCPU/32GB | 16 vCPU/64GB | 32 vCPU/256GB |
|------|-----------|-------------|--------------|---------------|
| Heap | 일반 마케팅 AP | 12GB | 24~28GB | JVM 분리 권장 |
| Heap | SingleView AP | 14GB | 28~32GB | 선도검증 필수 |
| GC | Collector | G1GC | G1GC | G1GC(ZGC 검토) |
| GC | MaxGCPauseMillis | 200ms | 200ms | 200~300ms |
| Thread Stack | -Xss | 512k | 512k | 512k~1m |
| Metaspace | MaxMetaspaceSize | 1g | 1~2g | 2~4g |
| Code Cache | ReservedCodeCacheSize | 256m | 256~512m | 512m |
| Dump | HeapDumpOnOOM | 활성화 | 활성화 | 활성화 |
| GC Log | Xlog:gc* | 활성화 | 활성화 | 활성화 |
| OS 여유 | Heap 외 메모리 | 14~18GB | 30GB+ | 100GB+ |

## 4. Heap Size 설계 기준

### 4.1 Heap Size 기본 원칙Heap은 크게 잡는 것이 아니라, 업무 객체 생성량·세션 크기·GC Pause·OS 여유 메모리를 함께 보고 결정한다.

온라인 AP는 Xms와 Xmx를 동일하게 설정해 운영 중 Heap 확장/축소로 인한 지연을 줄인다.32GB VM에서 Heap을 24GB 이상으로 과도하게 잡으면 OS, Thread Stack, Direct Buffer, Metaspace, APM, 로그 버퍼 공간이 부족해질 수 있다.

SingleView는 조회 결과를 세션에 저장하지 않고 요청 단위 객체로 처리해야 한다. 고객 조회 결과, 캠페인 대상자 목록, 대량 리스트는 세션 저장 금지다.


| 구성 요소 | 설명 | 8C/32G 권장 |
|-----------|------|------------|
| Java Heap | DTO·세션·캐시 | 일반 12GB, SV 14GB |
| Thread Stack | Worker Stack | -Xss512k |
| Metaspace | Class Metadata | Max 1GB |
| Code Cache | JIT 코드 | 256MB |
| Direct/Native | NIO·TLS·라이브러리 | 여유 확보 |
| APM/Agent | 관측 Agent | 2~4GB |
| OS/File Cache | OS·로그 | 6~8GB+ |

### 4.2 세션 크기와 Heap 영향세션 유지시간을 늘리면 TPS가 직접 증가하는 것이 아니라 JVM 안에 오래 살아 있는 Active Session 수가 증가한다. DeltaManager를 사용하는 경우 세션 객체가 복제 대상이 되므로 세션 객체 크기를 작게 유지해야 한다.

세션 저장 항목저장 여부이유userId, branchId, roleId, authLevel, maskingLevel가능인증·인가 판단에 필요한 최소 정보centerId, loginTime, lastAccessTime가능센터 식별과 세션 정책 적용에 필요고객 Single View 조회 결과금지개인정보, 대량 객체, 복제 부하 증가캠페인 대상자 목록금지대량 List 객체로 Heap/GC 부담 증가화면 검색결과 전체금지세션 비대화와 GC Pause 증가임시 업무 상태제한Request/DB/Cache 기반으로 전환 검토

## 5. GC 전략 및 G1GC 설정 기준온라인 AP의 GC 목표는 처리량 극대화가 아니라 응답시간 안정성이다. p95 3초 기준에서는 GC Pause가 사용자의 응답시간 안에 포함되므로, GC는 반드시 운영 모니터링 대상이 되어야 한다.


| GC 종류 | 특징 | NSIGHT 판단 |
|---------|------|-------------|
| Serial GC | 단일 Thread | 온라인 AP 부적합 |
| Parallel GC | 처리량 중심 | 배치성 제한 검토 |
| **G1GC** | 응답·처리량 균형 | **표준 권장** |
| ZGC | 초저지연 | JDK21+ 특수 고부하 |

G1GC 기본 권장 옵션-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+ParallelRefProcEnabled
-XX:InitiatingHeapOccupancyPercent=45튜닝 원칙: G1GC는 기본값만으로도 적응적으로 동작하도록 설계되어 있으므로, 과도한 Young/Old 영역 수동 튜닝보다 Heap 크기, 객체 생성량, SQL/연계 대기시간, Thread 수를 먼저 안정화해야 한다.6. GC Log / Heap Dump / Crash Dump 기준

### 6.1 GC Log 표준GC Log는 장애 후 분석용이 아니라 운영 중 응답시간 악화의 원인을 사전에 확인하기 위한 핵심 자료다. 운영 AP에서는 반드시 GC Log를 활성화하고 파일 회전 정책을 적용한다.

JDK 17/21 기준 GC Log 예시-Xlog:gc*,safepoint:file=/logs/gc/${APP_NAME}-gc-%t.log:time,uptime,level,tags:filecount=10,filesize=100M

### 6.2 OOM / Heap Dump 표준OOM 및 Fatal Error 대응 옵션-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/logs/dump
-XX:+ExitOnOutOfMemoryError
-XX:ErrorFile=/logs/dump/hs_err_pid%p.log항목권장 기준운영 주의사항Heap Dump 경로/logs/dumpDump 파일은 대용량이므로 파일시스템 여유 확보GC Log 경로/logs/gcAP별 파일명에 APP_NAME, timestamp 포함hs_err 파일/logs/dump/hs_err_pid%p.logJVM Fatal Error 발생 시 원인 분석 자료OOM 발생 시 정책ExitOnOutOfMemoryError불안정 JVM을 계속 서비스하지 않도록 종료 후 재기동보관 정책최근 10개 또는 일정 기간개인정보 포함 가능성 점검 및 접근권한 통제

## 7. Thread Stack / Native Memory / Direct Memory 기준Tomcat Thread 수를 늘리면 Heap만 사용하는 것이 아니라 Thread Stack과 Context Switching 부담도 증가한다. 따라서 maxThreads와 -Xss는 함께 산정해야 한다.

항목권장값설명-Xss512k8 vCPU / 32GB 기준 Tomcat Thread 300~500 수준에서 균형Tomcat maxThreads400~500응답 대기시간 포함 동시 점유 기준Async ThreadPoolCore 20~50, Max 100~200로그/감사/비동기 연계 분리Direct Memory기본 JVM 관리, 필요 시 명시NIO, TLS, 압축, 외부 라이브러리 사용량 점검Native Memory Tracking장애 분석 시 활성화상시 활성화는 오버헤드 검토Native Memory Tracking 예시# 장애 분석 시 일시 적용 검토-XX:NativeMemoryTracking=summary8. 업무 유형별 JVM 표준 템플릿

### 8.1 일반 마케팅 AP - 8 vCPU / 32GB-Xms12g
-Xmx12g
-Xss512k
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+ParallelRefProcEnabled
-XX:InitiatingHeapOccupancyPercent=45
-XX:MetaspaceSize=256m
-XX:MaxMetaspaceSize=1g
-XX:ReservedCodeCacheSize=256m
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/logs/dump
-XX:+ExitOnOutOfMemoryError
-XX:ErrorFile=/logs/dump/hs_err_pid%p.log
-Xlog:gc*,safepoint:file=/logs/gc/marketing-ap-gc-%t.log:time,uptime,level,tags:filecount=10,filesize=100M-Dfile.encoding=UTF
-8-Duser.timezone=Asia/Seoul
-Djava.security.egd=file:/dev/./urandom
-Dspring.profiles.active=prd

### 8.2 SingleView AP - 8 vCPU / 32GB-Xms14g
-Xmx14g
-Xss512k
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+ParallelRefProcEnabled
-XX:InitiatingHeapOccupancyPercent=45
-XX:MetaspaceSize=256m
-XX:MaxMetaspaceSize=1g
-XX:ReservedCodeCacheSize=256m
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/logs/dump
-XX:+ExitOnOutOfMemoryError
-XX:ErrorFile=/logs/dump/hs_err_pid%p.log
-Xlog:gc*,safepoint:file=/logs/gc/singleview-ap-gc-%t.log:time,uptime,level,tags:filecount=10,filesize=100M-Dfile.encoding=UTF
-8-Duser.timezone=Asia/Seoul
-Djava.security.egd=file:/dev/./urandom
-Dspring.profiles.active=prd8.3 16 vCPU / 64GB 고부하 AP 검토안16 vCPU / 64GB는 8 Core VM 대비 처리량을 높일 수 있으나, 장애 영향 범위와 배포 영향 범위가 커진다. 기본 전략은 8 Core Scale-Out이며, 16 Core는 특수 업무 또는 서버 수 제한 조건에서 검토한다.-Xms28g
-Xmx28g
-Xss512k
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+ParallelRefProcEnabled
-XX:InitiatingHeapOccupancyPercent=45
-XX:MetaspaceSize=512m
-XX:MaxMetaspaceSize=2g
-XX:ReservedCodeCacheSize=512m
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/logs/dump
-XX:+ExitOnOutOfMemoryError
-Xlog:gc*,safepoint:file=/logs/gc/app16-gc-%t.log:time,uptime,level,tags:filecount=10,filesize=100M8.4 32 vCPU / 256GB 특수 AP 검토안32 vCPU / 256GB 서버는 일반 온라인 AP 기본 단위로 사용하기보다, 고부하·분석성·배치성·특수 AP 검토 기준으로 본다. 온라인 AP는 하나의 초대형 JVM보다 업무별 JVM 분리 또는 8/16 Core VM Scale-Out이 운영 안정성 측면에서 유리하다.

선택안권장 여부설명단일 JVM 128GB Heap제한적 검토GC Pause와 장애 영향 범위가 커지므로 성능테스트 필수2개 JVM × 64GB Heap권장 검토장애 격리와 GC 안정성 측면에서 단일 대형 JVM보다 유리4개 JVM × 28~32GB Heap권장8/16 Core VM과 유사한 운영 단위로 분리 가능32 Core 서버를 온라인 기본 단위로 사용비권장배포/장애 영향 범위가 커지고 Scale-Out 유연성이 낮아짐# 32 Core 서버에서 단일 JVM이 필요한 특수 케이스 예시

```shell
-Xms96g
-Xmx96g
-Xss512k
-XX:+UseG1GC
-XX:MaxGCPauseMillis=300
-XX:+ParallelRefProcEnabled
-XX:InitiatingHeapOccupancyPercent=45
-XX:MaxMetaspaceSize=4g
-XX:ReservedCodeCacheSize=512m
-Xlog:gc*,safepoint:file=/logs/gc/special32-gc-%t.log:time,uptime,level,tags:filecount=20,filesize=100M9. setenv.sh / systemd / 환경변수 적용 방식
```


### 9.1 외장 Tomcat setenv.sh 적용 예시#!/bin/shAPP_NAME=marketing-apJAVA_HOME=/app/jdk-21CATALINA_HOME=/app/tomcatCATALINA_BASE=/app/tomcat-marketingJAVA_OPTS="${JAVA_OPTS} -Xms12g"
JAVA_OPTS="${JAVA_OPTS} -Xmx12g"
JAVA_OPTS="${JAVA_OPTS} -Xss512k"JAVA_OPTS="${JAVA_OPTS} -XX:+UseG1GC"
JAVA_OPTS="${JAVA_OPTS} -XX:MaxGCPauseMillis=200"
JAVA_OPTS="${JAVA_OPTS} -XX:+ParallelRefProcEnabled"
JAVA_OPTS="${JAVA_OPTS} -XX:InitiatingHeapOccupancyPercent=45"JAVA_OPTS="${JAVA_OPTS} -XX:MetaspaceSize=256m"
JAVA_OPTS="${JAVA_OPTS} -XX:MaxMetaspaceSize=1g"
JAVA_OPTS="${JAVA_OPTS} -XX:ReservedCodeCacheSize=256m"JAVA_OPTS="${JAVA_OPTS} -XX:+HeapDumpOnOutOfMemoryError"
JAVA_OPTS="${JAVA_OPTS} -XX:HeapDumpPath=/logs/dump"
JAVA_OPTS="${JAVA_OPTS} -XX:+ExitOnOutOfMemoryError"JAVA_OPTS="${JAVA_OPTS} -XX:ErrorFile=/logs/dump/hs_err_pid%p.log"JAVA_OPTS="${JAVA_OPTS} -Xlog:gc*,safepoint:file=/logs/gc/${APP_NAME}-gc-%t.log:time,uptime,level,tags:filecount=10,filesize=100M"JAVA_OPTS="${JAVA_OPTS} -Dfile.encoding=UTF
-8 -Duser.timezone=Asia/Seoul -Djava.security.egd=file:/dev/./urandom"JAVA_OPTS="${JAVA_OPTS} -Dspring.profiles.active=prd -Dapp.name=${APP_NAME}"export JAVA_HOME CATALINA_HOME CATALINA_BASE JAVA_OPTS

### 9.2 Spring Boot Jar systemd 적용 예시

```ini
[Unit]Description=NSIGHT Marketing Spring Boot ApplicationAfter=network.target[Service]User=appuserGroup=appuserWorkingDirectory=/app/marketingEnvironment="APP_NAME=marketing-ap"Environment="JAVA_HOME=/app/jdk-21"Environment="JAVA_OPTS=-Xms12g -Xmx12g -Xss512k -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/logs/dump -XX:+ExitOnOutOfMemoryError -Xlog:gc*,safepoint:file=/logs/gc/marketing-ap-gc-%t.log:time,uptime,level,tags:filecount=10,filesize=100M -Dfile.encoding=UTF
-8 -Duser.timezone=Asia/Seoul -Dspring.profiles.active=prd"ExecStart=/app/jdk
-21/bin/java $JAVA_OPTS -jar /app/marketing/marketing-ap.jarSuccessExitStatus=143Restart=on-failureRestartSec=10LimitNOFILE=65535[Install]WantedBy=multi-user.target
```
10. 모니터링 임계치 및 운영 점검

| 모니터링 항목 | Warning | Critical | 조치 방향 |
|---------------|---------|----------|-----------|
| Heap 사용률 | 70% 이상 지속 | 85% 이상 | 객체·세션·캐시·누수 분석 |
| GC Pause p95 | 200ms 초과 | 500ms 이상 반복 | Heap/객체/SQL 대기 분석 |
| Full GC | 발생 자체 점검 | 반복 발생 | Heap Dump·GC Log |
| Tomcat Busy Thread | 70% 이상 | 90% 이상 | Thread Dump·Pool Wait |
| Hikari Active | 70% 이상 | 90% 이상 | SQL·Pool·DB Session |
| CPU 사용률 | 70% 이상 | 85% 이상 | Scale-Out·Hot Method |
| Native Memory | 지속 증가 | OOM 위험 | NMT·Direct Buffer 점검 |

JVM 운영 점검 명령# 운영 점검 명령 예시jcmd <pid> VM.flagsjcmd <pid> GC.heap_infojcmd <pid> Thread.print > /logs/thread/thread-$(date +%Y%m%d%H%M%S).logjstat -gcutil <pid> 1000 10jmap -histo:live <pid> | head -5011. 성능테스트 및 장애 검증 시나리오검증 시나리오목표합격 기준360 TPS 기본 부하일반 업무시간 안정성 확인p95 3초 이하, CPU 60~70% 이하, GC Pause p95 200ms 이하720 TPS 피크 부하업무 집중 시간 대응 확인Thread/DB Pool 고갈 없음, 오류율 기준 이하1,080 TPS 스트레스한계와 장애 전파 차단 확인Fail-fast, Circuit Breaker, Timeout 정상 작동AP 1대 장애L4 제외 후 잔여 AP 처리 확인사용자 영향 최소화, 세션 정책 정상RDW SQL 지연DB 대기 시 Thread 고갈 방지 확인SQL Timeout, Transaction Timeout 순서 정상CruzAPIM 지연외부연계 장애 전파 차단 확인Read Timeout, Bulkhead, Circuit Breaker 정상3시간 세션 검증Active Session 증가에 따른 Heap/GC 확인세션 객체 크기 제한, GC 급증 없음OOM 모의 테스트Dump/재기동 정책 확인Heap Dump 생성, 프로세스 종료, 자동 재기동

## 12. 장애 유형별 조치 가이드

| 증상 | 가능 원인 | 우선 확인 | 조치 |
|------|-----------|-----------|------|
| 응답시간 급증 | SQL·연계·GC Pause | APM·GC Log·Hikari | SQL 튜닝·Timeout·Bulkhead |
| CPU 90%+ | Thread·로깅·암호화 | top·jstack·APM | Scale-Out·로그 레벨 |
| Heap 지속 증가 | 세션 비대화·누수 | Heap Histogram | 세션 금지·캐시 제한 |
| Full GC 반복 | Old 영역 급증 | GC Log·Dump | Heap·객체 생명주기 |
| OOM | Heap/Metaspace/Direct | hs_err·Dump·NMT | 유형별 보정 |
| Thread 고갈 | Pool·연계·Deadlock | Thread Dump | Timeout·Pool 분리 |
| GC Log 미생성 | 옵션·권한 | VM.flags | setenv/systemd 수정 |

## 13. 최종 적용 체크리스트점검 항목확인 내용상태Heap Size업무 유형별 Xms/Xmx가 동일하게 설정되었는가□OS 여유 메모리Heap 외 메모리, Thread Stack, APM, File Cache 여유가 있는가□GC 정책G1GC와 Pause 목표가 적용되었는가□GC Log파일 회전 정책과 경로 권한이 정상인가□Heap DumpOOM 시 Dump 경로와 디스크 여유가 확보되었는가□Thread StackTomcat maxThreads와 -Xss 기준이 함께 산정되었는가□세션 크기세션에는 최소 정보만 저장하고 대량 객체 저장을 금지했는가□Timeout 정합성DB Query Timeout < Transaction Timeout < Client/Proxy Timeout 순서인가□모니터링Heap, GC, Thread, Hikari, CPU, Timeout 지표가 수집되는가□성능테스트360/720/1,080 TPS 시나리오를 검증했는가□장애테스트OOM, AP 장애, RDW 지연, CruzAPIM 지연을 검증했는가□운영 절차Thread Dump, Heap Dump, GC Log 수집 절차가 문서화되었는가□14. 참고 자료구분자료활용 내용NSIGHT 내부 기준환경설정 기준안 / 용량산정 기준3,600개 지점, 21,600명, 360/720/1,080 TPS 기준, 8 vCPU / 32GB VM 기준Oracle Java SE 21 문서Garbage-First Garbage Collector TuningG1GC 기본 동작, Pause 목표, Heap 크기 조정 원칙Oracle Java SE 21 문서java Command Reference-Xms, -Xmx, -Xlog, HotSpot 옵션 기준Spring Boot 문서Application Propertiesserver, session, tomcat, actuator 외부화 설정 기준Apache Tomcat 문서HTTP Connector / ClusteringThread, Connection, 세션 복제, Connector 설정 기준

## 절별 상세

### 23.1 -Xms와 -Xmx

**원칙**: `Xms` = `Xmx` 동일 설정으로 운영 중 Heap 확장/축소 지연을 방지합니다.

| 업무 | Xms / Xmx | VM |
|------|-----------|----|
| 일반 마케팅 AP | 12GB | 8 vCPU / 32GB |
| SingleView AP | 14GB | 8 vCPU / 32GB |
| 16C 고부하 검토 | 28GB | 16 vCPU / 64GB |

> 32GB VM에서 Heap 24GB+는 OS·Native·APM 여유 부족 위험.

### 23.2 Heap 산정식

Heap은 물리 메모리의 50%를 무조건 할당하지 않습니다.

```
32GB VM 메모리 배분 (8CORE 기준)
├── Java Heap     12~14GB
├── Thread Stack  ~0.5GB (500 Thread × 512KB)
├── Metaspace     ~1GB
├── Direct/Native ~2GB
├── APM/Agent     ~2GB
└── OS/Cache      6~8GB
```

객체 생성량·세션 크기·GC Pause·OS 여유를 함께 보고 결정합니다.

### 23.3 -Xss

`-Xss512k` (8CORE / maxThreads 400~500 기준).

| 항목 | 값 | 설명 |
|------|-----|------|
| -Xss | 512KB | Thread Stack |
| maxThreads | 400~500 | Tomcat Worker |
| Stack 총량 | ~250MB | Thread×Stack |

Thread 수 증가 시 Stack 메모리도 선형 증가 → maxThreads와 함께 산정.

### 23.4 Metaspace

`-XX:MetaspaceSize=256m` · `-XX:MaxMetaspaceSize=1g` (8CORE).

| VM | MaxMetaspaceSize |
|----|------------------|
| 8C/32G | 1GB |
| 16C/64G | 2GB |
| 32C/256G | 4GB |

Class Metadata 누수 시 Metaspace OOM → 재배포 전 `jcmd VM.metaspace` 확인.

### 23.5 Code Cache

본 절(**Code Cache**)은 JVM 영역에서 **Heap·GC** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

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

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: setenv.sh·systemd

### 23.6 Direct Memory

본 절(**Direct Memory**)은 JVM 영역에서 **Heap·GC** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

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

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: setenv.sh·systemd

### 23.7 G1GC 선택 기준

본 절(**G1GC 선택 기준**)은 JVM 영역에서 **Heap·GC** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

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

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: setenv.sh·systemd

### 23.8 MaxGCPauseMillis

본 절(**MaxGCPauseMillis**)은 JVM 영역에서 **Heap·GC** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

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

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: setenv.sh·systemd

### 23.9 GC Logging

본 절(**GC Logging**)은 JVM 영역에서 **Heap·GC** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

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

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: setenv.sh·systemd

### 23.10 Heap Dump

본 절(**Heap Dump**)은 JVM 영역에서 **Heap·GC** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

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

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: setenv.sh·systemd

### 23.11 OOM 처리

본 절(**OOM 처리**)은 JVM 영역에서 **Heap·GC** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | Heap·GC 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | setenv.sh·systemd |
| 핵심 | G1GC·12GB |

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

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: setenv.sh·systemd

### 23.12 Container·VM Memory 인식

Container(cgroup) 또는 VM Memory Limit 인식 필수.

| VM | Heap | OS 여유 |
|----|------|--------|
| 8C/32G | 12~14GB | 14~18GB |
| 16C/64G | 24~28GB | 30GB+ |

K8s limits 미설정 시 OOM Killer가 JVM보다 먼저 프로세스 종료 가능.

### 23.13 Timezone·Encoding

본 절(**Timezone·Encoding**)은 JVM 영역에서 **Heap·GC** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | Heap·GC 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | setenv.sh·systemd |
| 핵심 | G1GC·12GB |

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

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: setenv.sh·systemd

### 23.14 JVM 설정 예시

**일반 마케팅 AP (8C/32G)**:
```shell
-Xms12g -Xmx12g -Xss512k
-XX:+UseG1GC -XX:MaxGCPauseMillis=200
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/logs/dump
-XX:+ExitOnOutOfMemoryError
-Xlog:gc*,safepoint:file=/logs/gc/marketing-ap-gc-%t.log:time,uptime,level,tags:filecount=10,filesize=100M
-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Seoul
```

**SingleView AP**: Heap 14GB. `setenv.sh` 또는 systemd `Environment=JAVA_OPTS=...` 적용.

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

### 23.15 JVM 설정 검증

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, GC p95≤200ms |
| 720 TPS | Thread/Pool 고갈 없음 |
| 1,080 TPS | Fail-fast·Timeout 정상 |
| AP 1대 Down | L4 제외·서비스 지속 |
| RDW SQL 지연 | SQL Timeout→Thread 회수 |
| OOM 모의 | Dump·재기동 정책 확인 |

`jcmd <pid> VM.flags` · `jstat -gcutil` · `jmap -histo:live`로 검증.

---

[← 목차](./00-목차.md)
