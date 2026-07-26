/**
 * 절별 상세 심화 오버라이드 (SECTION_OVERRIDES에 병합)
 */
module.exports = {
  // === 1. 도입 (심화) ===
  '1.1': `정보계 AP는 RDW 조회·권한·마스킹·GUID·연계가 포함되어 **단순 서버 대수 산정만으로는 운영 안정성을 보장할 수 없습니다.**

| 산정 단계 | 핵심 질문 | 산출값 |
|----------|----------|--------|
| 사용자 | 누가 사용하는가? | 21,600명, 세션 26K~28K |
| 동시 요청 | 몇 명이 동시에 요청? | 동시요청자 1,080~3,240 |
| TPS | 3초 내 몇 건? | 360 / 720 / 1,080 |
| AP | 몇 대 필요? | 8대 권장(A-A) |
| Pool | AP당 DB 연결? | 50~60 |
| DB Session | DB 총 부하? | Σ(AP×Pool) |
| 장애 | 잔여 처리량? | 잔여 TPS ≥ 720 |

계정계와 달리 **전문 조립·RDW·연계**가 TPMC를 소모하므로 사용자 수를 곧바로 TPS로 환산하면 과대·과소 산정이 발생합니다.
`,
  '1.2': `산출값(TPS, AP 대수)과 설정값(maxThreads, Pool, Timeout)은 **1:1 대응**이 아니라 산식·시험을 통해 연결합니다.

\`\`\`
21,600명 × 10% ÷ 3초 = 720 TPS
720 ÷ 250 = 3대 → A-A 6대, 권장 8대
AP당 Thread ≈ 360 → maxThreads 400~500
AP당 Pool ≈ 50 → 센터 8대 × 50 = 400
\`\`\`

환경설정은 산정표의 **물리적 구현**입니다. 산정 없이 설정만 복사하면 운영 장애 시 원인 추적이 불가능합니다.
`,
  '1.3': `핵심 원칙 5가지:

1. **피크 기준** — 평균 TPS가 아닌 720 TPS·스트레스 1,080 TPS로 설계
2. **세션 ≠ TPS** — 세션은 로그인 유지, TPS는 동시 요청자 기준
3. **Thread ≠ Pool** — 별도 산식, Pool ≤ Thread×30~40%
4. **안쪽 Timeout 짧게** — DB(3s) < Tx(5s) < Proxy(10s) < Web(15s)
5. **시험으로 확정** — 권고값은 출발점, 360/720/1080 실측 후 확정
`,
  '1.4': `| 구분 | 의미 | 예 | 변경 시점 |
|------|------|-----|----------|
| 가정값 | 산정 전제 | 동시요청률 5% | 업무 합의 시 |
| 권고값 | 표준 기준선 | maxThreads 500 | 가이드 개정 시 |
| 확정값 | 시험 실측 | Pool 48→50 | 성능시험 후 |
| 임계치 | APM Warning/Critical | CPU 70/85% | 운영 Baseline |

가정값·권고값·확정값을 혼용하면 장애 분석 시 **어느 기준으로 설계했는지** 알 수 없습니다.
`,
  '1.5': `성능시험 전후 관리 흐름:

1. **시험 전** — 권고값 적용, rule-check 통과
2. **시험 중** — 360/720/1080 TPS, p95·Thread·Pool·GC 동시 수집
3. **시험 후** — 확정값 문서화, 산정표·설정 파일 동기화
4. **운영 중** — Capacity Review(54장)로 Baseline 갱신

시험 생략 후 운영 반영은 **금지**입니다.
`,
  '1.6': `| 순서 | 부 | 내용 | 대상 독자 |
|------|-----|------|----------|
| 1 | 제2부 | 사용자→TPS→VM 산정 | PMO·아키텍처 |
| 2 | 제3부 | Timeout·분류체계 | 전체 |
| 3 | 제4부 | Tomcat·JVM·Pool·L4 | WAS·프레임워크 |
| 4 | 제5부 | 8C/16C/32C 프로파일 | 인프라·TA |
| 5 | 제7부 | 시험·검증·변경관리 | 성능시험·운영 |

[13단계 요약](./zNSIGHT-용량산정-전체-흐름.md)과 함께 CAP/ENV 화면(\`/oc/capacity.html\`, \`/oc/env-002.html\`)을 병행 활용합니다.
`,

  // === 2. 문서 개요 (심화) ===
  '2.1': '업무부하 산정에서 운영 설정·검증·변경관리까지 **하나의 가이드**로 연결. 산정표와 설정 파일이 분리되면 장애 시 원인·책임 추적이 불가능합니다.\n',
  '2.2': 'NSIGHT 마케팅 AP, SingleView AP, 연계 AP. IaaS VM **8 vCPU / 32GB** Scale-Out 표준. 16C/32C는 특수·배치용.\n',
  '2.3': 'WebTopSuite → GSLB → L4 → Apache → Tomcat → Spring Boot → HikariCP → RDW/ADW. CruzAPIM·Kafka·Gateway는 별도 Pool·Timeout.\n',
  '2.4': 'Tomcat/JVM/Pool/Timeout/L4/성능시험 — 인프라·WAS·프레임워크·DBA·운영 **역할별 절** 참조. RACI는 44장.\n',
  '2.5': '| 독자 | 주요 챕터 |\n|------|----------|\n| 아키텍처 | 7~11, 42 |\n| 인프라 | 17~19, 37~40 |\n| WAS | 22~23 |\n| 프레임워크 | 24~27, 25 |\n| DBA | 26~28 |\n| 성능시험 | 47~48 |\n| 운영 | 49~54 |\n',
  '2.6': 'NSIGHT 1차 표준(**3,600지점·21,600명·360/720/1080 TPS**) 합의 선행. 미합의 시 가정값으로 표기하고 시험 후 확정.\n',
  '2.7': 'TPS, TPMC, 세션, Pool, Timeout, Scale-Out — 제1부 및 [용량 용어](./zNSIGHT-용량산정-전체-흐름.md) 참조. **세션≠TPS** 반복 강조.\n',
  '2.8': '기준: Tomcat 10.1.x, Spring Boot 3.x, JDK 21. 버전 변경 시 Timeout·GC·Connector 호환성 **전체 재검증**(55장).\n',
  '2.9': '원본 33개: `znsight-capacity-word/_extract/*_docx.txt`. 환경 기준: `환경셋팅(최종).docx`. 부록 A~AD 템플릿.\n',
  '2.10': '버전·일자·변경 내용·승인자 기록. 설정값 변경은 **53장 절차**(요청→영향도→승인→시험→반영→이력) 필수.\n',

  // === 3. 문제 정의 (심화) ===
  '3.1': 'CPU Core·메모리만으로는 Thread(400~500), Pool(50~60), Timeout 계층, **장애 후 잔여 TPS**를 검증할 수 없습니다. VM 8대여도 maxThreads 200이면 피크 720 TPS 불가.\n',
  '3.2': 'VM 8대 배포 + maxThreads 200 + Pool 20 = **설정 병목**. 산정표(720 TPS, 8대)와 server.xml·application.yml 대조 필수.\n',
  '3.3': '| 유형 | 증상 | 위험 |\n|------|------|------|\n| 과다 Pool | DB Session 폭증 | RDW 장애 |\n| 과소 Pool | Hikari Pending | Thread 고갈 |\n| 과다 Thread | Context Switch | CPU 낭비 |\n| 과소 Thread | acceptCount 증가 | p95 급증 |\n',
  '3.4': '연쇄: AP Thread 포화 → Pool 대기 → SQL 지연 → L4 Queue → WebTop Timeout. **GUID**로 E2E 추적, 단일 계층만 튜닝 금지.\n',
  '3.5': '동일 Tomcat 다중 WAR: Connector Thread·Heap·Pool **공유**. 한 WAR Slow Transaction이 전체 포화 유발 → VM/WAR 분리·Bulkhead.\n',
  '3.6': 'A-A: **센터 장애 후 잔여 TPS ≥ 720**이 평상시 8대보다 중요. 잔여 = 잔여 AP × 250 TPS/VM.\n',
  '3.7': '캠페인·지점 증가 시 동시요청률·TPS·AP·Pool을 **동일 산식**으로 재계산. 설정만 미세 조정으로는 한계.\n',

  // === 5. 요구사항 ===
  '5.1': '**3,600지점 × 6명 = 21,600명**. 설계 세션 26,000~28,000(20~30% 여유). 세션은 Heap·복제 부담, TPS와 분리 산정.\n',
  '5.2': '목표 **p95 3초**. TPS: 기본 360, 피크 **720**, 스트레스 1,080. 평균 TPS는 참고용만.\n',
  '5.3': '2센터 Active-Active AP, DB Active-Standby. 무중단 Rolling 배포, Graceful Shutdown.\n',
  '5.4': '센터 1개 장애 시 잔여 센터가 **피크 TPS(720)** 감당. 센터당 AP 4대 권장.\n',
  '5.5': '성장률 20~30% 여유를 세션·AP N+1에 반영. CAP 화면에서 branchCount·동시요청률 시나리오 관리.\n',
  '5.6': '온라인 AP 표준: **8 vCPU / 32GB**. 16C/32C는 배치·특수 AP. Scale-Out 우선.\n',
  '5.7': 'DB max sessions ≥ Σ(온라인 Pool) + 배치 + BI + 운영 + **20% 여유**. Pool 무단 증가 금지.\n',
  '5.8': 'L4·방화벽 Idle, GSLB TTL 30초. 보안 정책으로 Timeout 단축 시 업무 SLA 재검토.\n',
  '5.9': '장시간 세션(180분) 예외 시 Heap·세션 복제·Sticky 영향 검증. Absolute 8시간 Filter 권장.\n',
  '5.10': 'Drain → Health fail → Shutdown 30s. Rollback Runbook·설정 이력(53장) 필수.\n',
  '5.11': 'APM Warning/Critical, GC Log, Hikari Metrics, GUID Access Log **운영 전** 구축.\n',

  // === 7. 연결 모델 ===
  '7.2': '| 입력값 | 예 | 출처 |\n|--------|-----|------|\n| 지점 수 | 3,600 | 업무 |\n| 지점당 사용자 | 6 | 업무 |\n| 동시요청률 | 5/10/15% | PMO 합의 |\n| 목표 응답 | 3초 | SLA |\n| DB max sessions | DBA | 인프라 |\n',
  '7.3': '| 계산값 | 산식 | 예(피크) |\n|--------|------|----------|\n| 동시요청자 | 사용자×비율 | 2,160 |\n| TPS | 동시요청자÷3 | 720 |\n| AP 대수 | ⌈TPS÷250⌉×2 | 8 |\n| Thread | TPS×1.2×1.2 | 360 |\n| Pool | TPS×0.15×1.3 | 50 |\n',
  '7.4': '| 설정값 | 위치 | 권고(8C) |\n|--------|------|----------|\n| maxThreads | server.xml | 400~500 |\n| Heap | setenv.sh | 12~14GB |\n| maximumPoolSize | application.yml | 50/60 |\n| statement-timeout | MyBatis | 3s |\n| ProxyTimeout | Apache | 10s |\n',
  '7.5': '| 임계치 | Warning | Critical |\n|--------|---------|----------|\n| CPU | 70% | 85% |\n| Heap | 70% | 85% |\n| Hikari Active | 70% | 90% |\n| GC Pause p95 | 200ms | 500ms |\n| Hikari Pending | >0 1분 | >0 5분 |\n',
  '7.6': '시험 측정: TPS, p95/p99, Busy Thread, Pool Active/Pending, SQL p95, GC Pause, 오류율, CruzAPIM CB.\n',
  '7.7': '확정값은 산정표·server.xml·application.yml·setenv.sh·L4 Profile에 **동일 버전**으로 기록. ENV 화면 rule-check로 정합성 검증.\n',

  // === 22. Tomcat (보완) ===
  '22.1': 'Connector = HTTP 입구. `protocol="Http11NioProtocol"`, port 8080(내부). Apache가 유일 외부 진입점.\n\n| 구성요소 | 역할 |\n|----------|------|\n| Acceptor | 연결 수락 |\n| Poller | NIO 이벤트 |\n| Worker Thread | 요청 처리 |\n| acceptCount | 대기 큐 |\n',
  '22.8': 'maxKeepAliveRequests: **100~500**. 무제한은 Connection 누수 위험. Apache KeepAlive와 정합.\n',
  '22.9': 'maxPostSize: **2~10MB** (업무별). 대용량은 Multipart·전용 API 분리.\n',
  '22.10': 'maxSwallowSize: maxPostSize 이상. 업로드 중단 시 연결 정리.\n',
  '22.11': 'asyncTimeout: **30초**. 비동기 Servlet 장시간 점유 방지.\n',
  '22.12': 'Executor 분리: 특수 장시간 업무만. 기본은 공유 Connector + Resilience4j Bulkhead.\n',
  '22.14': '업무별 동시처리: Resilience4j `bulkhead.maxConcurrentCalls` 또는 Semaphore. SV·연계 분리.\n',
  '22.16': '| 항목 | 8C/32G 권고 |\n|------|-------------|\n| maxThreads | 500 |\n| minSpareThreads | 100 |\n| acceptCount | 500 |\n| maxConnections | 10000 |\n| connectionTimeout | 8000ms |\n| keepAliveTimeout | 120000ms |\n',
  '22.17': '검증: 720 TPS 시 `currentThreadsBusy` < maxThreads×80%, acceptCount=0 유지. AP 1대 Down 시 L4 failover 30초 이내.\n',

  // === 24. Spring Boot ===
  '24.1': 'Profile: `dev` / `stg` / `prd`. PRD만 확정값. 민감정보는 환경변수·Vault.\n',
  '24.2': '`server.shutdown=graceful`. L4 Drain 후 Tomcat 요청 완료 대기.\n',
  '24.3': 'Shutdown phase **30초**. 미완료 요청은 로그·GUID로 추적 후 강제 종료.\n',
  '24.4': 'max-http-request-header-size 8KB. Body는 업무별 — 일반 JSON 1~5MB.\n',
  '24.5': 'multipart max-file-size / max-request-size 업무별. 대용량은 35장 전용 서버.\n',
  '24.6': 'Task Executor: core 10, max 50, queue 200. @Async 업무와 Tomcat Thread 분리.\n',
  '24.7': 'Async Executor 별도 Bean. `@Async` 남용 금지 — 연계·알림 등 제한적 사용.\n',
  '24.8': 'Scheduler pool: 5~10. 배치 트리거와 온라인 Thread 분리.\n',
  '24.9': '`@Transactional(timeout=5)` 기본. ServiceId별 4초(조회)·5초(등록) 차등.\n',
  '24.10': 'Actuator: `health,info,metrics,prometheus`만 노출. env·beans는 DEV만.\n',
  '24.11': 'Readiness: DB·Redis·연계. Liveness: ping. `/actuator/health/l4` L4 전용.\n',
  '24.12': '검증: rule-check Timeout 순서, Transaction 5s > SQL 3s, Graceful shutdown 720 TPS 중 배포.\n',

  // === 26. HikariCP (보완) ===
  '26.5': 'validationTimeout: **1초**. connectionTestQuery 또는 JDBC4 `isValid()`.\n',
  '26.8': 'keepaliveTime: **2~5분**. DB/L4 Idle보다 짧게. 방화벽 Idle drop 방지.\n',
  '26.11': 'poolName: `marketing-pool`, `sv-pool` 등 **업무·WAR별** 구분. JMX·APM 식별용.\n',
  '26.12': 'WAR별 DataSource 분리. 동일 Tomcat 내 Pool 합산 ≤ Thread×30%×WAR수.\n',
  '26.15': 'DB max sessions 대비 온라인 Pool **80% 이하** 유지. 20%는 배치·BI·운영·여유.\n',
  '26.16': '```yaml\nspring.datasource.hikari:\n  maximum-pool-size: 50\n  minimum-idle: 15\n  connection-timeout: 3000\n  idle-timeout: 600000\n  max-lifetime: 1800000\n  auto-commit: false\n```\n',
  '26.17': '검증: 720 TPS 시 Active≤70%, Pending=0. Pool Wait 발생 시 SQL p95 먼저 확인.\n',

  // === 56. 시사점 ===
  '56.1': `**아키텍처 판단**: 8C Scale-Out, 2센터 A-A, 업무별 VM 분리, FAST/DEEP 데이터 분리 유지.

| 판단 | 근거 | 용량 영향 |
|------|------|----------|
| 8C Scale-Out | 장애 격리 12.5% | VM당 250 TPS |
| A-A 2센터 | 무중단·DR | 센터당 AP×2 |
| SV·연계 VM 분리 | Bulkhead | Pool·Timeout 독립 |
| RDW/ADW 분리 | 온라인 SLA | Slow SQL 방지 |

NSIGHT 정보계는 **단순 CPU 산정이 아닌** TPMC·연계·RDW 부하를 포함합니다.
`,
  '56.2': `| 위험 | 증상 | 영향 | 대응 |
|------|------|------|------|
| Pool 과다 | DB Session 폭증 | RDW 장애 | SQL·Pool 동시 산정 |
| Thread 과다 | CPU CS 증가 | p95 급증 | 평균 응답시간 기준 |
| Timeout 역전 | 원인 불명 | 장애 확산 | rule-check |
| 세션 비대 | Full GC 반복 | Heap OOM | 세션 객체 금지 |
| 센터 미검증 | DR 시 TPS 급감 | SLA 위반 | 잔여 TPS≥720 |
| 시험 생략 | 운영 중 폭주 | 신뢰 상실 | 360/720/1080 필수 |
`,
  '56.3': `우선 보완 과제 (8주 로드맵):

| 주차 | 과제 | 산출물 | 완료 기준 |
|------|------|--------|----------|
| 1~2 | 산정표·설정 정합 | 부록 A·AD | CAP=설정 1:1 |
| 3~4 | 360/720/1080 시험 | Z 부록 | p95≤3s |
| 5~6 | ENV rule-check | Gate 통과 | Timeout·Pool |
| 7~8 | Capacity Review | 54장 리포트 | Baseline 등록 |

**즉시 착수**: 산정표와 server.xml·application.yml·setenv.sh 대조.
`,
  '56.4': `운영 안정화 체크리스트:

| 과제 | 담당 | 완료 기준 |
|------|------|----------|
| Capacity Review | 운영 | 54장 주기 준수 |
| Runbook | WAS | Thread/Heap Dump 절차 |
| 임계치 Baseline | APM | Warning/Critical 등록 |
| 변경관리 | 전체 | 53장 이력 100% |
| rule-check | 프레임워크 | ENV Gate 통과 |
`,
  '56.5': `중장기 로드맵:

1. **Config Server** — 환경별 설정 중앙화, 민감정보 분리
2. **Spring Session Redis** — DeltaManager 대규모 확장 대비
3. **Auto Scale** — 정책 검토(온라인 AP는 수동 Scale-Out 우선)
4. **ADW 분리** — RDW 온라인 부하·분석 SQL 격리 강화
5. **GUID E2E Observability** — 단말~DB~연계 통합 대시보드
`,

  // === 57. 마무리 ===
  '57.1': `최종 목적: **운영 가능한 안정성** — 피크·센터 장애·Slow SQL·연계 지연에서도 Fail-fast와 잔여 TPS 보장.

| 목표 | 측정 | 합격 기준 |
|------|------|----------|
| 피크 처리 | 720 TPS | p95≤3s |
| 센터 장애 | 잔여 TPS | ≥720 |
| DB 보호 | Session 합산 | ≤max×80% |
| 연계 지연 | CruzAPIM CB | 5s 내 실패 |

산정표·설정·시험·운영 Baseline이 **하나의 문서 체인**으로 연결될 때 비로소 용량산정이 완료됩니다.
`,
  '57.2': `권고값(maxThreads 500, Pool 50)은 **출발점**. 선도개발·운영 실측으로 지속 보정. 고정값으로 취급 금지.

| 구분 | 예 | 갱신 시점 |
|------|-----|----------|
| 가정값 | 동시요청률 10% | 업무 합의 |
| 권고값 | Pool 50 | 가이드 개정 |
| 확정값 | Pool 48→50 | 성능시험 후 |
| Baseline | CPU Warning 70% | Capacity Review |
`,
  '57.3': `360/720/1080 TPS + 센터 장애 + AP Down 시나리오 필수. p95·Busy Thread·Hikari Pending·SQL p95·GC Pause **동시 수집**. 합격 없이 운영 전환 금지.

| 시나리오 | TPS | 필수 지표 |
|----------|-----|----------|
| 기본 | 360 | CPU, p95 |
| 피크 | 720 | Thread, Pool |
| 스트레스 | 1,080 | Fail-fast, CB |
| AP Down | 720@잔여 | L4 failover |
| 센터 Down | 720@단일 | GSLB 전환 |
`,
  '57.4': `일간(지표) → 주간(병목) → 월간(Baseline) → 분기(성장률) Capacity Review. 54장 절차 준수.

| 주기 | 활동 | 산출물 |
|------|------|--------|
| 일간 | APM·Pending 추세 | 알림 |
| 주간 | Top SQL·GC | 주간 리포트 |
| 월간 | Baseline 갱신 | 54장 리포트 |
| 분기 | 성장률·재산정 | 부록 A 갱신 |
`,

  // === 55. 호환성·폐기 ===
  '55.1': '설정 항목별 **버전·적용일·환경(DEV/STG/PRD)** 기록. server.xml·application.yml·setenv.sh는 Git 태그와 연동.\n',
  '55.2': '| 업그레이드 | 영향 점검 |\n|------------|----------|\n| Tomcat 10→11 | Connector, Session |\n| Spring Boot 3.x | Jakarta, Actuator |\n| JDK 21 | GC Log, ZGC 검토 |\n| Hikari 5.x | Pool 속성명 |\n\n업그레이드 후 **360/720 TPS 재시험** 필수.\n',
  '55.3': 'Deprecated 설정 목록 유지. 예: `maxActive`→`maximumPoolSize`, Parallel GC→G1GC. 53장 변경관리와 연동.\n',
  '55.4': '이전 버전 호환 기간(예: 2개월) 정의. STG에서 구·신 설정 병행 검증 후 PRD 일괄 전환.\n',
  '55.5': '폐기 절차: 사용 여부 조사 → 영향도 → 승인 → STG 검증 → PRD 제거 → 이력 보관.\n',
  '55.6': '미사용 DataSource·WAR·JVM 옵션·L4 Pool Member 정리. 설정 파일 주석으로 Deprecated 표기 금지 — 문서화.\n',
  '55.7': '산정표·가정값·시험결과·확정값을 **동일 변경번호**로 보존. PMO·감사 대응용.\n',
};
