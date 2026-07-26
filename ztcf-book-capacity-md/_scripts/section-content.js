/**
 * znsight-capacity-word 기반 절(섹션) 본문
 * 키: "챕터.절" (예: "50.1")
 */
module.exports.SECTION_OVERRIDES = {
  // === 1. 도입 ===
  '1.1': '정보계 AP는 RDW 조회·권한·마스킹·GUID·연계가 포함되어 **단순 서버 대수 산정만으로는 운영 안정성을 보장할 수 없습니다.**\n',
  '1.2': '산출값(TPS, AP 대수)과 설정값(maxThreads, Pool, Timeout)은 **1:1 대응**이 아니라 산식·시험을 통해 연결합니다.\n',
  '1.3': '핵심: 피크 기준, 세션≠TPS, Thread≠Pool, 안쪽 Timeout이 짧게, 성능시험으로 확정.\n',
  '1.4': '| 구분 | 예 |\n|------|----|\n| 가정값 | 동시요청률 5% |\n| 권고값 | maxThreads 500 |\n| 확정값 | 선도개발 실측 |\n',
  '1.5': '시험 전 권고값 적용 → 시험 중 실측 → 확정값 문서화 → 운영 Capacity Review로 갱신.\n',
  '1.6': '제2부(산정) → 제3~4부(설정) → 제5부(프로파일) → 제7부(검증·전환) 순으로 활용.\n',

  // === 3. 문제 정의 ===
  '3.1': 'CPU Core 수·메모리 용량만으로는 Thread, Pool, Timeout, 장애 잔여량을 검증할 수 없습니다.\n',
  '3.2': 'VM 8대 배포했으나 maxThreads·Pool이 산정과 불일치하면 **설정 병목**이 발생합니다.\n',
  '3.3': '과다 설정: DB Session 폭증. 과소 설정: Pool Wait·Thread 고갈.\n',
  '3.4': 'AP Thread 포화 → DB Pool 대기 → SQL 지연 → L4 Queue → 단말 Timeout으로 연쇄 전파.\n',
  '3.5': '동일 Tomcat의 다중 WAR는 Thread·Heap·Pool을 **공유**합니다. WAR별 Bulkhead 검토.\n',
  '3.6': 'A-A 배포 시 **센터 장애 후 잔여 TPS**가 평상시 TPS보다 중요합니다.\n',
  '3.7': '사용자·캠페인 증가 시 동시요청률·TPS·AP·Pool을 **동일 산식**으로 재계산.\n',

  // === 6. 설계 원칙 ===
  '6.1': '입력(지점 수)부터 설정(maxThreads)까지 **추적 가능한 산정표**를 유지합니다.\n',
  '6.2': '평균 TPS가 아닌 **피크 720 TPS·스트레스 1,080 TPS**로 설계.\n',
  '6.3': '정상(360)·피크(720)·스트레스(1080)·장애(센터 Down) 시나리오 분리 검증.\n',
  '6.4': '세션 26,000~28,000은 로그인 유지 규모. TPS는 **동시요청자** 기준.\n',
  '6.5': 'maxThreads 500이어도 Pool 50이면 DB 대기가 병목. **별도 산정**.\n',
  '6.6': '32GB VM에서 Heap 24GB+는 OS·Native 부족 위험. Heap 12~14GB 권장.\n',
  '6.7': '공유: Tomcat Connector. 업무별: Pool Name, Bulkhead, 별도 DataSource.\n',
  '6.8': 'DB Query(3s) < Transaction(5s) < Proxy(10s) < WebTop(15s) < L4(120s).\n',
  '6.9': '| 구분 | 의미 |\n|------|------|\n| 상한 | DB max sessions |\n| 권고 | 표준 기준선 |\n| 임계치 | APM Warning |\n',
  '6.10': '벤치마크 수치는 **가정값**. 360/720/1080 시험 후 확정값으로 갱신.\n',
  '6.11': 'ENV rule-check, Timeout 순서 검사, Pool 합산 검사 자동화.\n',
  '6.12': '가정·산식·설정·시험결과를 **동일 문서 체인**으로 보관.\n',

  // === 8. 사용자 모델 ===
  '8.1': '```\n전체 사용자 = 3,600 × 6 = 21,600\n```\n',
  '8.2': '- **로그인 사용자**: 세션 규모\n- **활성 사용자**: 실제 요청 발생\n',
  '8.3': '동시접속 = TCP/HTTP 연결 유지. `maxConnections`, KeepAlive와 연관.\n',
  '8.4': '| 시나리오 | 비율 | 동시 요청자 |\n|----------|------|-------------|\n| 기본 | 5% | 1,080 |\n| 피크 | 10% | 2,160 |\n| 스트레스 | 15% | 3,240 |\n',
  '8.5': '설계 세션 = 26,000~28,000. **세션 증가 ≠ TPS 증가** (Heap·복제 부담은 증가).\n',
  '8.6': '피크 집중계수: 캠페인·마감 시간대 동시요청률 상향 보정.\n',
  '8.7': '성장률·여유율 20~30%를 세션·AP N+1에 반영.\n',
  '8.8': '조회·등록·SV·연계 비율은 APM으로 선도개발 후 보정.\n',
  '8.9': '조회 70% / 등록·변경 20% / 대량·배치 10% 등 업무 비율 가정 후 TPMC 보정.\n',
  '8.10': 'CAP-010 입력 예: branchCount=3600, userPerBranch=6 → totalUsers 자동계산.\n',

  // === 9. TPS ===
  '9.1': 'TPS = 초당 완료 거래 수. 동시요청자와 목표 응답시간으로 산정.\n',
  '9.2': '평균 TPS는 참고. 설계·운영 기준은 **피크 TPS**.\n',
  '9.3': '```\n피크 TPS = 2,160 ÷ 3 = 720\n```\n',
  '9.4': '스트레스 1,080 TPS는 한계·Fail-fast 검증용.\n',
  '9.5': '```\n동시 처리량 = TPS × 평균 점유시간(초)\n```\n',
  '9.6': 'Thread 산정: 평균 1.0~1.2초. p95 3초는 SLA 목표.\n',
  '9.7': 'p95 3초 이하가 사용자 체감 SLA. GC Pause도 포함.\n',
  '9.8': '| 시나리오 | TPS |\n|----------|-----|\n| 기본 | 360 |\n| 피크 | 720 |\n| 스트레스 | 1,080 |\n',
  '9.9': '1,200~1,800 TPS는 확장 검증 시나리오(인프라·DB 한도 확인).\n',
  '9.10': '재시도·Timeout은 부하를 증폭. 동기 거래 **재시도 기본 금지**.\n',

  // === 10. TPMC ===
  '10.1': '1 TPS = 60 TPMC는 단순 벤치마크. 정보계는 **2,500~3,500 TPMC/TPS**.\n',
  '10.2': '전문 파싱·인증·로깅·RDW·마스킹·조립이 TPMC를 소모.\n',
  '10.3': 'Core당 이론: 106,932÷60 ≈ 1,782 TPS/Core (참고만).\n',
  '10.4': '**실효: Core당 30~40 TPS** → 8Core 250 TPS/VM.\n',
  '10.5': 'CPU 목표 이용률: 운영 60~70%, 시험 피크 70% 이하.\n',
  '10.6': '일반 AP: 1,500~2,000 TPMC/TPS. SingleView: 2,000~3,500.\n',
  '10.7': 'TPMC↑ → Core당 TPS↓. ENV-001 위험 건수 증가 원인 중 하나.\n',
  '10.8': '성능시험 후 TPMC/TPS·VM TPS 보정계수 적용.\n',

  // === 11. VM ===
  '11.1': '**8 vCPU / 32GB** = 온라인 AP Scale-Out 표준 단위.\n',
  '11.2': '16C/64G: 배치·BI 일부. 온라인 기본 단위 비권장.\n',
  '11.3': '16C/128G: Heap·Cache 여유. 선도검증 필수.\n',
  '11.4': '32C/256G: 배치·ETL·특수 AP. 온라인 단일 JVM 비권장.\n',
  '11.5': '8CORE: **250 TPS/VM** (보수). 16CORE: ~500 TPS 검토.\n',
  '11.6': '기본 360 TPS → AP 4대(A-A). 피크 720 → **8대 권장**.\n',
  '11.7': '피크 시간대 센터당 3~4대 운영.\n',
  '11.8': '센터 장애 후 잔여 TPS ≥ 목표 TPS. 720 TPS → 센터당 4대 권장.\n',
  '11.9': 'N+1: 피크 대비 1대 여유. Rolling 배포 시 1대 Drain.\n',
  '11.10': '배포 중 L4 Drain → Health Check fail → Graceful shutdown.\n',
  '11.11': '최종안: 센터별 AP·Pool·L4 VIP·GSLB 비율 문서화.\n',

  // === 16. Timeout ===
  '16.1': '```\nDB(3s) < Pool(3s) < Tx(5s) < Proxy(10s) < Web(15s) < L4(120s)\n```\n',
  '16.2': 'MyBatis `default-statement-timeout: 3`. RDW 온라인 2~3초.\n',
  '16.3': 'Hikari `connection-timeout: 3000` — Pool 획득 대기, SQL 시간 아님.\n',
  '16.4': '`@Transactional(timeout=5)`. Facade/Service 계층 적용.\n',
  '16.5': 'TCF ServiceId별 Timeout. 온라인 4~5초 이내.\n',
  '16.6': 'CruzAPIM Connect 3초.\n',
  '16.7': 'CruzAPIM Read 5초. Circuit Breaker 연동.\n',
  '16.8': 'Gateway route timeout ≤ Proxy.\n',
  '16.9': 'nginx `proxy_read_timeout 10s`.\n',
  '16.10': 'L4 Idle 120초. Sticky = Session + 10분.\n',
  '16.11': '10초 이상 동기 거래 **금지** → 비동기·결과조회 API.\n',
  '16.12': 'rule-check: Timeout 계층 순서 자동 검사.\n',

  // === 22. Tomcat ===
  '22.2': 'maxThreads **400~500** (8CORE). `250×1.2×1.2≈360`.\n',
  '22.3': 'minSpareThreads 100 — 피크 진입 초기 지연 완화.\n',
  '22.4': 'acceptCount 300~500. Queue로 병목을 숨기지 말 것.\n',
  '22.5': 'maxConnections 10,000. L4 KeepAlive와 정합.\n',
  '22.6': 'connectionTimeout 8초 — 연결 후 요청 대기.\n',
  '22.7': 'keepAliveTimeout 120초.\n',
  '22.13': '다중 WAR: 동일 Connector Thread 경쟁. 업무별 VM 분리 검토.\n',
  '22.15': '```yaml\nserver.tomcat.threads.max: 500\n```\n',

  // === 23. JVM (전 절 상세) ===
  '23.1': '**원칙**: `Xms` = `Xmx` 동일 설정으로 운영 중 Heap 확장/축소 지연을 방지합니다.\n\n| 업무 | Xms / Xmx | VM |\n|------|-----------|----|\n| 일반 마케팅 AP | 12GB | 8 vCPU / 32GB |\n| SingleView AP | 14GB | 8 vCPU / 32GB |\n| 16C 고부하 검토 | 28GB | 16 vCPU / 64GB |\n\n> 32GB VM에서 Heap 24GB+는 OS·Native·APM 여유 부족 위험.\n',
  '23.2': 'Heap은 물리 메모리의 50%를 무조건 할당하지 않습니다.\n\n```\n32GB VM 메모리 배분 (8CORE 기준)\n├── Java Heap     12~14GB\n├── Thread Stack  ~0.5GB (500 Thread × 512KB)\n├── Metaspace     ~1GB\n├── Direct/Native ~2GB\n├── APM/Agent     ~2GB\n└── OS/Cache      6~8GB\n```\n\n객체 생성량·세션 크기·GC Pause·OS 여유를 함께 보고 결정합니다.\n',
  '23.3': '`-Xss512k` (8CORE / maxThreads 400~500 기준).\n\n| 항목 | 값 | 설명 |\n|------|-----|------|\n| -Xss | 512KB | Thread Stack |\n| maxThreads | 400~500 | Tomcat Worker |\n| Stack 총량 | ~250MB | Thread×Stack |\n\nThread 수 증가 시 Stack 메모리도 선형 증가 → maxThreads와 함께 산정.\n',
  '23.4': '`-XX:MetaspaceSize=256m` · `-XX:MaxMetaspaceSize=1g` (8CORE).\n\n| VM | MaxMetaspaceSize |\n|----|------------------|\n| 8C/32G | 1GB |\n| 16C/64G | 2GB |\n| 32C/256G | 4GB |\n\nClass Metadata 누수 시 Metaspace OOM → 재배포 전 `jcmd VM.metaspace` 확인.\n',
  '23.5': '`-XX:ReservedCodeCacheSize=256m` (8CORE). 16C: 512m.\n\nJIT 컴파일 코드 캐시. Hot Method가 많은 SingleView AP는 256m 이상 권장.\n',
  '23.6': 'NIO Buffer·TLS·압축·외부 라이브러리가 사용. 상시 NMT 활성화는 오버헤드 → **장애 분석 시** `-XX:NativeMemoryTracking=summary` 적용.\n\nDirect Memory 고갈 시 `OutOfMemoryError: Direct buffer memory` 발생.\n',
  '23.7': '온라인 AP **G1GC 표준**. ZGC는 JDK 21+ 특수 고부하에서 별도 검토.\n\n| GC | NSIGHT 판단 |\n|----|-------------|\n| Serial GC | 부적합 |\n| Parallel GC | 배치성 제한 검토 |\n| **G1GC** | **표준 권장** |\n| ZGC | 초대형 Heap 특수 |\n',
  '23.8': '`-XX:MaxGCPauseMillis=200` — **목표값**(보장 아님). p95 3초 SLA에 GC Pause 포함.\n\nG1GC 기본 옵션:\n```shell\n-XX:+UseG1GC\n-XX:MaxGCPauseMillis=200\n-XX:+ParallelRefProcEnabled\n-XX:InitiatingHeapOccupancyPercent=45\n```\n',
  '23.9': 'JDK 17/21 GC Log 필수:\n```shell\n-Xlog:gc*,safepoint:file=/logs/gc/${APP_NAME}-gc-%t.log:time,uptime,level,tags:filecount=10,filesize=100M\n```\n\n운영 중 GC Pause 악화를 사전 감지. 파일 회전·경로 권한 필수.\n',
  '23.10': '```shell\n-XX:+HeapDumpOnOutOfMemoryError\n-XX:HeapDumpPath=/logs/dump\n-XX:+ExitOnOutOfMemoryError\n-XX:ErrorFile=/logs/dump/hs_err_pid%p.log\n```\n\nDump 파일 대용량 → 디스크 여유·접근권한·보관 정책(최근 10개) 수립.\n',
  '23.11': 'OOM 발생 시: HeapDump 생성 → JVM 종료(`ExitOnOutOfMemoryError`) → **자동 재기동**(systemd `Restart=on-failure`).\n\n불안정 JVM을 계속 서비스하지 않음. 재기동 후 GUID 기준 미완료 거래 확인.\n',
  '23.12': 'Container(cgroup) 또는 VM Memory Limit 인식 필수.\n\n| VM | Heap | OS 여유 |\n|----|------|--------|\n| 8C/32G | 12~14GB | 14~18GB |\n| 16C/64G | 24~28GB | 30GB+ |\n\nK8s limits 미설정 시 OOM Killer가 JVM보다 먼저 프로세스 종료 가능.\n',
  '23.13': '```shell\n-Dfile.encoding=UTF-8\n-Duser.timezone=Asia/Seoul\n-Djava.security.egd=file:/dev/./urandom\n-Dspring.profiles.active=prd\n```\n\n로그·거래시간·세션 만료 정합성에 Timezone 필수.\n',
  '23.14': '**일반 마케팅 AP (8C/32G)**:\n```shell\n-Xms12g -Xmx12g -Xss512k\n-XX:+UseG1GC -XX:MaxGCPauseMillis=200\n-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/logs/dump\n-XX:+ExitOnOutOfMemoryError\n-Xlog:gc*,safepoint:file=/logs/gc/marketing-ap-gc-%t.log:time,uptime,level,tags:filecount=10,filesize=100M\n-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Seoul\n```\n\n**SingleView AP**: Heap 14GB. `setenv.sh` 또는 systemd `Environment=JAVA_OPTS=...` 적용.\n',
  '23.15': '| 시나리오 | 합격 기준 |\n|----------|----------|\n| 360 TPS | p95≤3s, CPU≤70%, GC p95≤200ms |\n| 720 TPS | Thread/Pool 고갈 없음 |\n| 1,080 TPS | Fail-fast·Timeout 정상 |\n| AP 1대 Down | L4 제외·서비스 지속 |\n| RDW SQL 지연 | SQL Timeout→Thread 회수 |\n| OOM 모의 | Dump·재기동 정책 확인 |\n\n`jcmd <pid> VM.flags` · `jstat -gcutil` · `jmap -histo:live`로 검증.\n',

  // === 19. L4 ===
  '19.1': '센터별 L4 VIP를 WebTopSuite·Apache 진입점으로 구성. VIP Address = 서비스 진입 IP.\n',
  '19.2': 'Pool Member = Tomcat/Apache 서버. 센터당 2~4대. Health Check 연동 필수.\n',
  '19.3': 'Active-Active: **Round Robin**. DR: Priority. 센터별 유입 비율 모니터링.\n',
  '19.4': 'Sticky: **JSESSIONID** persistence. Sticky Timeout = Session(60분) + 10분 = **70분**.\n',
  '19.5': 'Health Check: `GET /health/l4` 또는 `/actuator/health`. Interval 5초, Timeout 2초, Fail 3회.\n',
  '19.6': 'Connect Timeout: **2~3초**. AP 미응답 시 빠른 실패.\n',
  '19.7': 'Idle Timeout: **120초**. Tomcat keepAliveTimeout(120s)과 정합.\n',
  '19.8': 'Connection Limit: VIP당 동시 연결 상한. 비정상 Client 폭주 방어.\n',
  '19.9': '배포 시 **Drain** → Active Connection 소진 → Member Disable. Graceful Removal.\n',
  '19.10': '센터 장애 시 GSLB가 장애 VIP 제외. WebTopSuite 센터 재조회·재로그인 정책.\n',
  '19.11': '360/720 TPS 부하 + AP 1대 Down + Sticky Failover 시험. VIP·Pool·SSL Profile 점검.\n',

  // === 14. 분류체계 ===
  '14.1': 'VM Spec, OS Kernel, Disk, Network — IaaS 표준(8C/32G) 기준.\n',
  '14.2': 'Tomcat, Apache, L4, GSLB, Gateway — Connector·Proxy·VIP 설정.\n',
  '14.3': 'Spring Boot application.yml, WAR web.xml — Tx·Session·Resilience4j.\n',
  '14.4': 'HikariCP, MyBatis timeout, DB max sessions — Pool·SQL 분리.\n',
  '14.5': 'Proxy Timeout, L4 Idle/Sticky, GSLB TTL — 계층 정합.\n',
  '14.6': 'Cookie Secure/HttpOnly, TRACE 차단, X-Forwarded-For 신뢰 범위.\n',
  '14.7': 'APM, Actuator, GC Log, Access Log, 임계치 — Warning/Critical.\n',
  '14.8': 'DEV/STG/PRD 분리. PRD만 확정값. 동일 항목·다른 수치 금지(혼선).\n',
  '14.9': '공통: maxThreads·GC·Timeout 계층. 업무별: Pool Name·Bulkhead·DataSource.\n',
  '14.10': '정적: server.xml·setenv.sh. 동적: Actuator refresh(제한적). Config Server 검토.\n',

  // === 2. 문서 개요 ===
  '2.1': '업무부하 산정에서 운영 설정·검증·변경관리까지 **하나의 가이드**로 연결.\n',
  '2.2': 'NSIGHT 마케팅 AP, SingleView AP, 연계 AP. IaaS VM 8C/32G Scale-Out.\n',
  '2.3': 'WebTopSuite·GSLB·L4·Apache·Tomcat·Spring Boot·RDW/ADW·CruzAPIM·Kafka.\n',
  '2.4': 'Tomcat/JVM/Pool/Timeout/L4/성능시험 — 인프라·WAS·프레임워크·DBA·운영.\n',
  '2.5': '아키텍처·인프라·WAS·프레임워크·업무·DBA·성능시험·운영 담당자.\n',
  '2.6': 'NSIGHT 1차 표준(3,600지점·21,600명·360/720/1080 TPS) 합의 선행.\n',
  '2.7': 'TPS, TPMC, 세션, Pool, Timeout, Scale-Out — 제1부 용어 정의 참조.\n',
  '2.8': '기준일·Tomcat 10.1.x·Spring Boot 3.x·JDK 21. 변경 시 전체 재검증.\n',
  '2.9': '`znsight-capacity-word` 33개 원본, 환경셋팅(최종), 13단계 흐름 요약.\n',
  '2.10': '버전·일자·변경 내용·승인자. 설정값 변경은 53장 절차 준수.\n',

  // === 4. 현행 구조 ===
  '4.5': 'maxThreads·acceptCount·maxConnections — `환경셋팅(최종)` Tomcat 표준과 대조.\n',
  '4.6': 'Heap 12~14GB, G1GC, GC Log — 산정값과 실측 JVM 옵션 비교.\n',
  '4.7': 'Pool 50/60, connectionTimeout 3초 — AP당·센터 전체 Pool 합산 검증.\n',
  '4.9': 'Timeout 계층 순서 정합성 — DB(3s) < Tx(5s) < Proxy(10s) < Web(15s).\n',
  '4.11': 'Thread↑ Pool↓ Timeout역전 등 **설정값 간 불일치**가 병목·장애 원인.\n',

  // === 6·7 연결 ===
  '7.1': '```\n사용자 → 동시요청자 → TPS → AP대수 → Pool → Timeout → 임계치\n```\n',

  '42.1': '2센터 Active-Active AP + DB Active-Standby. GSLB→센터 L4→Apache→Tomcat.\n',
  '42.2': '센터당 AP 4대(720 TPS 권장). VIP·Pool·GSLB Pool 분리.\n',
  '42.3': '8C/32G VM 1대 = 1 Tomcat(또는 1 Spring Boot JAR). 다중 WAR는 Thread 공유.\n',
  '42.4': '마케팅 AP / SingleView AP / 연계 AP VM 분리. Bulkhead 적용.\n',
  '42.5': '일반 Pool 50, SV 60/VM. 센터 전체 Pool ≤ DB max sessions.\n',
  '42.6': 'Gateway·OM·JWT·Batch는 온라인 AP와 **VM/Pool 분리**.\n',
  '42.7': '정상(360)→피크(720)→장애(센터 Down) 시 Thread·Pool·잔여 TPS 흐름 검증.\n',

  // === 26. HikariCP ===
  '26.1': '```\nPool = max(30, min(TPS×DB점유×1.3, Thread×30%))\n```\n',
  '26.2': 'maximumPoolSize: 일반 **50**, SingleView **60** (8CORE/250TPS).\n',
  '26.4': 'connectionTimeout 3초 — SQL 실행시간과 별개.\n',
  '26.13': 'Tomcat 전체 Pool = Σ(WAR Pool × DataSource).\n',
  '26.14': '센터 전체 = AP수 × Pool. DBA max sessions 검증.\n',
  '26.3': 'minimumIdle: **10~15**. maximumPoolSize의 20~30%.\n',
  '26.6': 'idleTimeout: **10분**. maxLifetime보다 짧게.\n',
  '26.7': 'maxLifetime: **30분 이하**. DB/L4 Idle보다 짧게.\n',
  '26.9': 'leakDetectionThreshold: 개발·검증 60s. 운영은 선택.\n',
  '26.10': 'autoCommit: **false**. Spring `@Transactional`과 정합.\n',
  '27.1': 'default-statement-timeout: **3초**. RDW 온라인 기준.\n',
  '27.2': 'default-fetch-size: **100~500**. 대량 조회 제어.\n',
  '27.9': '대량 조회: 페이징·조건 제한. Full Scan RDW 금지.\n',
  '28.1': 'DB max sessions ≥ Σ(AP×Pool) + 배치 + BI + 운영 + 여유(20%).\n',
  '29.2': 'Session Idle: **60분** 기본. 상담 예외 180분 검토.\n',
  '29.3': 'Absolute Timeout: **8시간**. Filter/Interceptor 구현.\n',
  '29.6': 'DeltaManager: 센터 내부 2~4노드. 세션 객체 Serializable·최소화.\n',
  '32.5': 'Connect Timeout: **3초**.\n',
  '32.6': 'Read Timeout: **5초** (CruzAPIM). Retry 기본 금지.\n',
  '33.11': '배치 Window 00:00~06:00. 온라인 Pool·CPU와 분리.\n',
  '34.11': 'Consumer Lag Warning/Critical 업무별 정의. DLQ 필수.\n',
  '45.1': '21,600명 × 10% = 2,160 → TPS 720.\n',
  '45.2': '⌈720÷250⌉ = 3 → A-A 6대, **권장 8대**.\n',
  '45.3': 'Thread ≈ 360, maxThreads 400~500.\n',
  '45.4': 'Pool = 50/VM, 총 400 (8대).\n',
  '45.7': '센터 장애 후 4대 × 250 = **1,000 TPS**.\n',

  // === 37. 8C 프로파일 ===
  '37.2': '250 TPS/VM.\n',
  '37.3': 'maxThreads 500, acceptCount 500.\n',
  '37.4': 'Heap 12~14GB, G1GC.\n',
  '37.5': 'Pool 50/60.\n',
  '37.6': 'Tx 5s, SQL 3s, Proxy 10s.\n',
  '37.8': 'CPU 70%, Heap 70%, Hikari Active 70%.\n',

  // === 47. 성능시험 ===
  '47.4': '360 TPS: p95≤3s, CPU≤70%, GC p95≤200ms.\n',
  '47.5': '720 TPS: Thread/Pool 고갈 없음.\n',
  '47.6': '1,080 TPS: 한계점·CB·Fail-fast 확인.\n',
  '47.9': '센터 장애: 잔여 TPS ≥ 720.\n',
  '47.10': 'AP 1대 Down: L4 제외, 서비스 지속.\n',
  '47.11': 'Pool 고갈 시험: 3초 내 connectionTimeout.\n',
  '47.12': 'Slow SQL: statement timeout → Thread 회수.\n',
  '47.13': 'CruzAPIM 5초 Read, CB Open 확인.\n',

  // === 49. 임계치 ===
  '49.1': 'CPU Warning 70%, Critical 85%.\n',
  '49.2': 'Heap Warning 70%, Critical 85%.\n',
  '49.3': 'GC Pause p95 Warning 200ms, Critical 500ms.\n',
  '49.5': 'Hikari Active 70%/90%. **Pending > 0 지속** 시 즉시 분석.\n',
  '49.6': 'TPS·p95·오류율 동시 관측. 평균 TPS만 보면 안 됨.\n',
  '49.12': '임계치 초과 시 Scale-Out·SQL 튜닝·Pool 보정 Trigger.\n',

  // === 50. 장애 흐름 ===
  '50.1': '**증상**: currentThreadsBusy ≈ maxThreads, acceptCount 증가, p95 급증.\n\n**원인**: DB Pool 대기, Slow SQL, 외부연계, GC Pause.\n\n**조치**: Thread Dump → Hikari Active/Pending → APM Trace(GUID). Thread만 늘리지 말 것.\n',
  '50.2': '**증상**: Hikari `connections.pending` > 0, Active ≈ maximumPoolSize.\n\n**원인**: SQL 지연, Pool 과소, AP 증설로 Pool 총량 폭증.\n\n**조치**: SQL p95 확인 → Pool 보정 vs SQL 튜닝 판단. Pool 무조건 증가 금지.\n',
  '50.3': '**증상**: `SQLTransientConnectionException`, connectionTimeout 3초 초과.\n\n**구분**: Pool 고갈 vs DB 장애 vs 방화벽 Idle.\n\n**조치**: Pool 사용률·DB Health·maxLifetime(30분) 점검.\n',
  '50.4': '**증상**: SQL timeout 증가, DB Lock wait.\n\n**조치**: statement timeout 3초로 Thread 회수. RDW Full Scan·분석 SQL 차단.\n',
  '50.5': '**증상**: CPU 85%+, 처리량 정체.\n\n**조치**: Scale-Out, Hot Method 분석, 로그 레벨·암호화 부하 점검.\n',
  '50.6': '**증상**: GC Pause p95 > 200ms, Full GC.\n\n**조치**: Heap Histogram, 세션 비대화·캐시 누수, Heap 12~14GB 유지.\n',
  '50.7': '**증상**: CruzAPIM Read timeout, CB Open.\n\n**조치**: Connect 3s/Read 5s, Bulkhead, **재시도 금지** → GUID 상태조회.\n',
  '50.8': '**증상**: Gateway 504, route timeout.\n\n**조치**: Backend Pool·CB·Rate Limit. AP 장애와 구분.\n',
  '50.9': '**증상**: Cache miss 급증, Redis timeout.\n\n**조치**: TTL·Stampede 방지, Cache 장애 시 fallback 정책.\n',
  '50.10': '**증상**: Consumer Lag 증가, 이벤트 지연.\n\n**조치**: DLQ, partition·consumer 수, max.poll.interval.ms.\n',
  '50.11': '**증상**: 특정 WAR만 CPU·Thread·Pool 독점.\n\n**조치**: Bulkhead, WAR별 VM 분리, Slow Transaction 추적.\n',
  '50.12': '**증상**: GSLB 재조회, 센터 전환, 재로그인.\n\n**검증**: 잔여 AP × 250 ≥ 목표 TPS. 센터 간 세션 복제 미적용 시 정책 공지.\n',
  '50.13': '복구 후 **설정값 재검증**: Pool 총량, Timeout 계층, 임계치 Baseline 갱신.\n',

  // === 51. 자동검증 ===
  '51.4': 'DB Query < Transaction < Proxy < Web < L4 순서 검사.\n',
  '51.5': 'maxThreads 대비 Pool 비율 (Pool ≤ Thread×30~40%).\n',
  '51.6': 'Σ(AP×Pool×DS) ≤ DB max sessions.\n',
  '51.12': '360/720/1080 시험 합격 없이 운영 전환 금지.\n',

  // === 52. 운영 전환 ===
  '52.1': '산정표(PMO·인프라·DBA 합의) 승인.\n',
  '52.7': 'Timeout 계층 rule-check 통과.\n',
  '52.11': '성능시험 결과서(Z 부록) 첨부.\n',
  '52.14': 'Thread Dump·Heap Dump·Rollback Runbook 확인.\n',

  // === 57. 마무리 ===
  '57.1': '최종 목적: **운영 가능한 안정성**과 장애 격리 검증.\n',
  '57.2': '권고값은 출발점. 선도개발·운영 실측으로 **지속 보정**.\n',
  '57.4': '일간·주간·월간 Capacity Review로 Baseline 갱신.\n',
};

/** 제목 키워드 기반 기본 절 본문 */
module.exports.genericSection = function genericSection(chapterNum, sectionTitle) {
  const t = sectionTitle;
  const lines = [];

  if (/산정|계산|공식/.test(t)) {
    lines.push('NSIGHT 1차 표준(21,600명·360/720/1080 TPS·250 TPS/VM) 산식을 적용합니다.');
    lines.push('```');
    lines.push('동시요청자 = 전체사용자 × 동시요청률');
    lines.push('TPS = 동시요청자 ÷ 목표응답시간(초)');
    lines.push('```');
  } else if (/Timeout|타임아웃/.test(t)) {
    lines.push('안쪽 자원(DB·Pool)부터 짧게 실패. 계층 순서 정합성 필수.');
    lines.push('참고: DB 3s · Transaction 4~5s · Proxy 10s · WebTop 15s · L4 120s.');
  } else if (/Thread|쓰레드/.test(t)) {
    lines.push('8CORE: maxThreads 400~500. 산정: AP TPS × 평균점유(1.2s) × 여유(1.2).');
    lines.push('p95 3초를 직접 곱하지 않음 — 평균 응답시간 기준.');
  } else if (/Pool|Hikari|Connection/.test(t)) {
    lines.push('Pool = AP TPS × DB점유(0.15s) × 1.3. 상한 = Thread × 30%.');
    lines.push('일반 50 / SV 60. connectionTimeout 3초 ≠ SQL timeout.');
  } else if (/Heap|JVM|GC|Memory|메모리/.test(t)) {
    lines.push('8CORE/32GB: Heap 12~14GB, G1GC, Pause 목표 200ms.');
    lines.push('세션·캐시에 대량 객체 저장 금지.');
  } else if (/검증|시험|Gate|체크/.test(t)) {
    lines.push('360/720/1080 TPS 및 장애 시나리오로 검증.');
    lines.push('p95·Busy Thread·Hikari Pending·SQL p95·GC Pause 동시 수집.');
  } else if (/장애|복구|고갈|포화/.test(t)) {
    lines.push('GUID 기준 E2E 추적: 단말 → Proxy → AP → DB → 연계.');
    lines.push('Thread·Pool·JVM을 분리 보지 말고 동시 분석.');
  } else if (/설정|환경|프로파일/.test(t)) {
    lines.push('권고값은 `환경셋팅(최종).docx` 기준. 성능시험 후 확정.');
  } else if (/모니터|임계|지표/.test(t)) {
    lines.push('Warning/Critical 이중 임계치. 평균이 아닌 p95·Pending 추세 관측.');
  } else if (/Kafka|CDC|배치|Batch|Scheduler/.test(t)) {
    lines.push('온라인 SLA와 별도 기준. Lag·DLQ·배치 Window 관리.');
  } else if (/센터|DR|장애 수용/.test(t)) {
    lines.push('잔여 TPS = 잔여 AP × 250. 피크 720 → 센터당 4대 권장.');
  } else if (/RACI|책임|승인/.test(t)) {
    lines.push('아키텍처·인프라·프레임워크·업무·DBA·운영·성능시험 역할 분리.');
  } else {
    lines.push('`znsight-capacity-word` 및 NSIGHT 1차 표준(3,600지점·21,600명·8CORE-32GB)을 따릅니다.');
    lines.push('최종값은 선도개발·성능시험 실측으로 보정합니다.');
  }
  return lines.join('\n\n') + '\n';
};

const { enrichSection, enrichByTitle } = require('./section-enrichment');
const { buildRichSection, verifyTable } = require('./section-rich-builder');

// 심화 오버라이드 병합
Object.assign(module.exports.SECTION_OVERRIDES, require('./section-overrides-deep'));

/** 23장 수준 기준: 수동 오버라이드 최소 길이 */
const RICH_OVERRIDE_MIN = 280;

module.exports.getSectionText = function getSectionText(key, chapterNum, sectionTitle) {
  const override = module.exports.SECTION_OVERRIDES[key];
  const rich = buildRichSection(chapterNum, key, sectionTitle);

  // 도입·마무리 등 절 수 적은 장: rich 본문 우선 (23장 구조)
  if ([1, 56, 57].includes(chapterNum)) {
    const minLen = chapterNum === 1 ? RICH_OVERRIDE_MIN : 200;
    if (override && override.trim().length >= minLen) {
      return override.trim() + '\n\n' + rich;
    }
    return rich;
  }

  if (override && override.trim().length >= RICH_OVERRIDE_MIN) {
    const body = override.trim();
    if (!/검증 기준|합격 기준/.test(body)) {
      return body + '\n\n#### 검증 기준\n\n' + verifyTable() + '\n';
    }
    return body + '\n';
  }

  if (override && override.trim().length > 200) {
    return override.trim() + '\n';
  }

  return rich;
};
