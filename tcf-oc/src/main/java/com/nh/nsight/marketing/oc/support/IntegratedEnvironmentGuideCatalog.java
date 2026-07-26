package com.nh.nsight.marketing.oc.support;



import com.nh.nsight.marketing.oc.support.EnvSettingDefinition.MatchType;



import java.util.List;



/**

 * NSIGHT 통합 환경설정 가이드 — {@link Nsight32Core256GbGuide} (32 Core / 256GB) 기준.

 */

public final class IntegratedEnvironmentGuideCatalog {



    public static final String GUIDE_TITLE = Nsight32Core256GbGuide.PROFILE_TITLE + " 환경설정 가이드";

    public static final String GUIDE_VERSION =
            "2026-05-31 (용량산정 세션60분 + 32Core 환경설정 작업가이드)";



    private IntegratedEnvironmentGuideCatalog() {

    }



    public static List<String> designRules() {

        return List.of(

                "32 Core VM은 일반 온라인 AP 표준이 아님 — 성능·SingleView·EBM·BI 등 특수 AP 전용",

                "Timeout: DB Query(2~3s) < Hikari(3s) <= Transaction(4~5s) < CruzAPIM(5s) < Proxy(10s) < WebTop(15s) < L4(70~90s)",

                "Thread·Pool·Heap·Timeout·SQL·세션·장애전파를 한 세트로 설계 (Thread만 단독 상향 금지)",

                "Tomcat maxThreads 1,500~1,800 · Hikari 일반 120~150 / SingleView 150~180 · AP Pool ≤ DB Session",

                "세션 36,000(여유 43~47K) ≠ 실요청(동시) — 실요청 1,080/1,800/3,600/5,400명(3/5/10/15%)",
                "TPS(전사) 360/600/1,200/1,800 = 실요청÷p95(3초) · 5% 피크=1,800명→600 TPS · 스트레스 TPS 1,800≠실요청 1,800명",

                "Circuit Breaker / Bulkhead / Queue 분리로 장애 전파 억제"

        );

    }

    /**
     * Rule Engine(THRESHOLD·RELATION) 점검 시 설정 조정에 참고할 중요 기준.
     */
    public static List<String> configurationCriteria() {
        return List.of(
                "[Timeout 계층 · TIMEOUT-REL-001] MyBatis Query("
                        + Nsight32Core256GbGuide.MYBATIS_STATEMENT_TIMEOUT_SEC
                        + "s) < Hikari(" + (Nsight32Core256GbGuide.HIKARI_CONNECTION_TIMEOUT_MS / 1000)
                        + "s) ≤ Transaction(" + Nsight32Core256GbGuide.TRANSACTION_TIMEOUT_SEC_MIN + "~"
                        + Nsight32Core256GbGuide.TRANSACTION_TIMEOUT_SEC_MAX + "s) < CruzAPIM Read("
                        + (Nsight32Core256GbGuide.CRUZAPIM_READ_TIMEOUT_MS / 1000) + "s) < Proxy("
                        + (Nsight32Core256GbGuide.PROXY_READ_TIMEOUT_MS / 1000) + "s) < WebTop Request("
                        + (Nsight32Core256GbGuide.WEBTOP_REQUEST_TIMEOUT_MS / 1000) + "s) < L4 Idle("
                        + Nsight32Core256GbGuide.L4_CLIENT_IDLE_TIMEOUT_SEC + "s)",
                "[실요청 · CAPACITY-REL-002] 전체 " + Nsight32Core256GbGuide.TOTAL_USERS + "명 × "
                        + Nsight32Core256GbGuide.ACTUAL_REQUEST_PERCENT_PEAK + "% = "
                        + Nsight32Core256GbGuide.ACTUAL_REQUEST_USERS_PEAK
                        + "명(실요청) → peak-tps " + Nsight32Core256GbGuide.PEAK_TPS + " = 실요청÷p95 "
                        + (Nsight32Core256GbGuide.TARGET_P95_MS / 1000) + "초 (스트레스 TPS "
                        + Nsight32Core256GbGuide.STRESS_TPS + "과 숫자만 같고 의미 다름)",
                "[동시 처리 · CONCURRENT-REL-001] 실요청 " + Nsight32Core256GbGuide.ACTUAL_REQUEST_USERS_PEAK
                        + "명(전사) ÷ AP " + Nsight32Core256GbGuide.DEFAULT_AP_COUNT + " = AP당 "
                        + NsightCapacityDerivation.concurrentPerAp(
                        Nsight32Core256GbGuide.ACTUAL_REQUEST_USERS_PEAK, Nsight32Core256GbGuide.DEFAULT_AP_COUNT)
                        + " ≤ Tomcat maxThreads " + Nsight32Core256GbGuide.TOMCAT_MAX_THREADS,
                "[용량·사용자] 전체 " + Nsight32Core256GbGuide.TOTAL_USERS + "명 · 실요청(5%) "
                        + Nsight32Core256GbGuide.ACTUAL_REQUEST_USERS_PEAK + "명 · 세션 "
                        + Nsight32Core256GbGuide.SESSION_DESIGN_COUNT + "명 — 세션≠실요청",
                "[TPS·실요청 시나리오] 3% " + Nsight32Core256GbGuide.ACTUAL_REQUEST_USERS_LOW_LOAD + "명→"
                        + Nsight32Core256GbGuide.LOW_LOAD_TPS + " TPS / 5% "
                        + Nsight32Core256GbGuide.ACTUAL_REQUEST_USERS_PEAK + "명→"
                        + Nsight32Core256GbGuide.PEAK_TPS + " TPS / 15% 스트레스 "
                        + Nsight32Core256GbGuide.ACTUAL_REQUEST_USERS_STRESS + "명→"
                        + Nsight32Core256GbGuide.STRESS_TPS + " TPS",
                "[Tomcat 32C] maxThreads " + Nsight32Core256GbGuide.TOMCAT_MIN_SPARE_THREADS + "~"
                        + Nsight32Core256GbGuide.TOMCAT_MAX_THREADS + ", acceptCount "
                        + Nsight32Core256GbGuide.TOMCAT_ACCEPT_COUNT + ", maxConnections "
                        + Nsight32Core256GbGuide.TOMCAT_MAX_CONNECTIONS + ", connectionTimeout "
                        + (Nsight32Core256GbGuide.TOMCAT_CONNECTION_TIMEOUT_MS / 1000) + "s",
                "[DB·SQL] MyBatis defaultStatementTimeout "
                        + Nsight32Core256GbGuide.MYBATIS_STATEMENT_TIMEOUT_SEC
                        + "s, defaultFetchSize 300 · 장시간 SQL·풀스캔은 Timeout만 올리지 말고 쿼리·인덱스 점검",
                "[L4·GSLB · INFRA] Idle " + Nsight32Core256GbGuide.L4_CLIENT_IDLE_TIMEOUT_SEC
                        + "s, Health " + Nsight32Core256GbGuide.L4_HEALTH_INTERVAL_SEC + "s/"
                        + Nsight32Core256GbGuide.L4_HEALTH_TIMEOUT_SEC + "s/"
                        + Nsight32Core256GbGuide.L4_HEALTH_FAIL_COUNT + "회, Sticky "
                        + Nsight32Core256GbGuide.L4_STICKY_TIMEOUT_SEC
                        + "s — WebTop·세션보다 짧게 두지 않음",
                "[JVM·Heap] 일반 AP " + Nsight32Core256GbGuide.JVM_HEAP_GENERAL_GB_MIN + "~"
                        + Nsight32Core256GbGuide.JVM_HEAP_GENERAL_GB_MAX + "GB, SingleView "
                        + Nsight32Core256GbGuide.JVM_HEAP_SINGLEVIEW_GB_MAX
                        + "GB 이내 — Thread·Pool 상향 시 Heap·GC 함께 검토",
                "[판정] PASS=유지 · WARN=계획 조정 · FAIL=즉시 조정. TIMEOUT-REL-001·CONCURRENT-REL-001 FAIL은 배포 차단 권고"
        );
    }

    public static List<EnvSettingDefinition> definitions() {

        return List.of(

                def("baseline.tps.low", "BASELINE", "용량산정 TPS", "동시요청÷p95 3초",

                        "낮은 부하 (3%)", "용량", String.valueOf(Nsight32Core256GbGuide.LOW_LOAD_TPS),

                        "nsight.env-check.base-tps", MatchType.EXACT, null, null, "application.yml"),

                def("baseline.tps.peak", "BASELINE", "용량산정 TPS", "5% 일반 피크",

                        "운영 피크 TPS", "용량", String.valueOf(Nsight32Core256GbGuide.PEAK_TPS),

                        "nsight.env-check.peak-tps", MatchType.EXACT, null, null, "application.yml"),

                def("baseline.tps.high", "BASELINE", "용량산정 TPS", "10% 높은 피크",

                        "고피크 TPS", "용량", String.valueOf(Nsight32Core256GbGuide.HIGH_PEAK_TPS),

                        "nsight.env-check.high-peak-tps", MatchType.EXACT, null, null, "application.yml"),

                def("baseline.tps.stress", "BASELINE", "용량산정 TPS", "15% 스트레스",

                        "스트레스 TPS", "용량", String.valueOf(Nsight32Core256GbGuide.STRESS_TPS),

                        "nsight.env-check.stress-tps", MatchType.EXACT, null, null, "application.yml"),

                def("baseline.tps.vm-max", "BASELINE", "용량산정 TPS", "VM 1대 상한",

                        "VM max TPS", "용량", String.valueOf(Nsight32Core256GbGuide.VM_MAX_TPS),

                        "nsight.env-check.vm-max-tps", MatchType.EXACT, null, null, "application.yml"),

                def("baseline.session.design", "BASELINE", "세션 60분", "로그인 유지 규모",

                        "세션 설계 기준", "세션", String.valueOf(Nsight32Core256GbGuide.SESSION_DESIGN_COUNT),

                        "nsight.env-check.session-design-count", MatchType.EXACT, null, null, "application.yml"),

                def("baseline.session.buffer-min", "BASELINE", "세션 60분", "여유율 20~30%",

                        "세션 여유 하한", "세션", String.valueOf(Nsight32Core256GbGuide.SESSION_BUFFERED_MIN),

                        "nsight.env-check.session-buffered-min", MatchType.EXACT, null, null, "application.yml"),

                def("baseline.session.buffer-max", "BASELINE", "세션 60분", "여유율 20~30%",

                        "세션 여유 상한", "세션", String.valueOf(Nsight32Core256GbGuide.SESSION_BUFFERED_MAX),

                        "nsight.env-check.session-buffered-max", MatchType.EXACT, null, null, "application.yml"),

                def("baseline.concurrent.peak", "BASELINE", "동시 요청자", "5% 피크 (세션 36K)",

                        "피크 동시요청자", "용량", String.valueOf(Nsight32Core256GbGuide.PEAK_CONCURRENT_USERS),

                        "nsight.env-check.peak-concurrent-users", MatchType.EXACT, null, null, "application.yml"),

                def("baseline.actual-request.peak", "BASELINE", "실요청 사용자", "운영 피크 5%",

                        "실요청 사용자", "용량", String.valueOf(Nsight32Core256GbGuide.ACTUAL_REQUEST_USERS_PEAK),

                        "nsight.env-check.actual-request-users", MatchType.EXACT, null, null, "application.yml"),

                def("baseline.branch", "BASELINE", "지점·사용자", "6,000 지점 × 6명",

                        "전체 사용자", "용량", String.valueOf(Nsight32Core256GbGuide.TOTAL_USERS),

                        "nsight.env-check.total-users", MatchType.EXACT, null, null, "application.yml"),

                def("baseline.ap.vm", "BASELINE", "기준 용량", "32Core TPS·AP 규모",

                        "AP VM 규격", "IaaS", Nsight32Core256GbGuide.VM_SPEC, null, MatchType.INFO_ONLY, null, null,

                        "특수 AP · BI/배치 검토 — 일반 8C/32G Scale-Out과 별도"),

                def("webtop.request-timeout", "WEBTOP", "WebTopSuite", "단말·채널 Runtime Timeout",

                        "Request Timeout", "WebTopSuite", Nsight32Core256GbGuide.WEBTOP_REQUEST_TIMEOUT_MS + " ms",

                        "nsight.webtop.request-timeout-ms",

                        MatchType.EXACT, null, null, "application.yml"),

                def("webtop.connect-timeout", "WEBTOP", "WebTopSuite", "단말·채널 Runtime Timeout",

                        "Connect Timeout", "WebTopSuite", Nsight32Core256GbGuide.WEBTOP_CONNECT_TIMEOUT_MS + " ms",

                        "nsight.webtop.connect-timeout-ms",

                        MatchType.EXACT, null, null, "application.yml"),

                def("webtop.read-timeout", "WEBTOP", "WebTopSuite", "단말·채널 Runtime Timeout",

                        "Read Timeout", "WebTopSuite", Nsight32Core256GbGuide.WEBTOP_READ_TIMEOUT_MS + " ms",

                        "nsight.webtop.read-timeout-ms",

                        MatchType.EXACT, null, null, "application.yml"),

                def("webtop.retry-count", "WEBTOP", "WebTopSuite", "단말·채널 Runtime Timeout",

                        "Retry Count", "WebTopSuite", "1", "nsight.webtop.retry-count",

                        MatchType.EXACT, null, null, "application.yml"),

                def("tomcat.max-threads", "TOMCAT", "Tomcat / WAS", "스레드·연결·세션 (32Core)",

                        "maxThreads", "Tomcat", "1200~1500", "server.tomcat.threads.max",

                        MatchType.RANGE, "1200", "1500", "application.yml · server.xml"),

                def("tomcat.min-spare", "TOMCAT", "Tomcat / WAS", "스레드·연결·세션 (32Core)",

                        "minSpareThreads", "Tomcat", "200~300", "server.tomcat.threads.min-spare",

                        MatchType.RANGE, "200", "300", "application.yml · server.xml"),

                def("tomcat.accept-count", "TOMCAT", "Tomcat / WAS", "스레드·연결·세션 (32Core)",

                        "acceptCount", "Tomcat", "500~800", "server.tomcat.accept-count",

                        MatchType.RANGE, "500", "800", "application.yml · server.xml"),

                def("tomcat.keep-alive-timeout", "TOMCAT", "Tomcat / WAS", "스레드·연결·세션 (32Core)",

                        "keepAliveTimeout", "Tomcat",

                        Nsight32Core256GbGuide.TOMCAT_KEEP_ALIVE_TIMEOUT_SEC + "s",

                        "server.tomcat.keep-alive-timeout",

                        MatchType.EXACT, null, null, "application.yml · L4 Idle 70~90초"),

                def("tomcat.max-connections", "TOMCAT", "Tomcat / WAS", "스레드·연결·세션 (32Core)",

                        "maxConnections", "Tomcat", "20000~30000", "server.tomcat.max-connections",

                        MatchType.RANGE, "20000", "30000", "application.yml · server.xml"),

                def("tomcat.connection-timeout", "TOMCAT", "Tomcat / WAS", "스레드·연결·세션 (32Core)",

                        "connectionTimeout", "Tomcat", "8s", "server.tomcat.connection-timeout",

                        MatchType.EXACT, null, null, "application.yml · server.xml"),

                def("spring.tomcat-max", "SPRING", "Spring Boot", "Embedded Tomcat (32Core)",

                        "server.tomcat.threads.max", "Spring", "1200~1500",

                        "server.tomcat.threads.max",

                        MatchType.RANGE, "1200", "1500", "application.yml · 성능시험 최종 보정"),

                def("spring.tomcat-min-spare", "SPRING", "Spring Boot", "Embedded Tomcat (32Core)",

                        "server.tomcat.threads.min-spare", "Spring", "200~300",

                        "server.tomcat.threads.min-spare",

                        MatchType.RANGE, "200", "300", "application.yml"),

                def("spring.tomcat-accept", "SPRING", "Spring Boot", "Embedded Tomcat (32Core)",

                        "server.tomcat.accept-count", "Spring", "500~800",

                        "server.tomcat.accept-count",

                        MatchType.RANGE, "500", "800", "application.yml"),

                def("spring.tomcat-max-conn", "SPRING", "Spring Boot", "Embedded Tomcat (32Core)",

                        "server.tomcat.max-connections", "Spring", "20000~30000",

                        "server.tomcat.max-connections",

                        MatchType.RANGE, "20000", "30000", "application.yml · OS/L4 검증"),

                def("spring.tomcat-conn-timeout", "SPRING", "Spring Boot", "Embedded Tomcat (32Core)",

                        "server.tomcat.connection-timeout", "Spring", "8s",

                        "server.tomcat.connection-timeout",

                        MatchType.EXACT, null, null, "application.yml"),

                def("spring.tomcat-keep-alive", "SPRING", "Spring Boot", "Embedded Tomcat (32Core)",

                        "server.tomcat.keep-alive-timeout", "Spring",

                        Nsight32Core256GbGuide.TOMCAT_KEEP_ALIVE_TIMEOUT_SEC + "s",

                        "server.tomcat.keep-alive-timeout",

                        MatchType.EXACT, null, null, "application.yml · L4 Idle 70~90초"),

                def("spring.session-timeout", "SPRING", "Spring Boot", "세션·트랜잭션·Async",

                        "server.servlet.session.timeout", "Spring",

                        Nsight32Core256GbGuide.SESSION_IDLE_MINUTES + "m",

                        "server.servlet.session.timeout",

                        MatchType.EXACT, null, null, "application.yml · 기존 세션 기준 유지"),

                def("spring.async-core", "SPRING", "Spring Boot", "세션·트랜잭션·Async",

                        "Async Executor Core Pool", "Spring",

                        Nsight32Core256GbGuide.ASYNC_CORE_POOL_MIN + "~"

                                + Nsight32Core256GbGuide.ASYNC_CORE_POOL_MAX,

                        "nsight.async.audit-log.core-pool-size",

                        MatchType.RANGE,

                        String.valueOf(Nsight32Core256GbGuide.ASYNC_CORE_POOL_MIN),

                        String.valueOf(Nsight32Core256GbGuide.ASYNC_CORE_POOL_MAX),

                        "application.yml · AsyncConfig"),

                def("spring.async-max", "SPRING", "Spring Boot", "세션·트랜잭션·Async",

                        "Async Executor Max Pool", "Spring",

                        Nsight32Core256GbGuide.ASYNC_MAX_POOL_MIN + "~"

                                + Nsight32Core256GbGuide.ASYNC_MAX_POOL_MAX,

                        "nsight.async.audit-log.max-pool-size",

                        MatchType.RANGE,

                        String.valueOf(Nsight32Core256GbGuide.ASYNC_MAX_POOL_MIN),

                        String.valueOf(Nsight32Core256GbGuide.ASYNC_MAX_POOL_MAX),

                        "application.yml · 무제한 증가 금지"),

                def("spring.absolute-session", "SPRING", "Spring Boot", "세션·트랜잭션·AP ID",

                        "Absolute Session", "Spring", Nsight32Core256GbGuide.ABSOLUTE_SESSION_HOURS + "h",

                        "nsight.absolute-session-timeout-hours",

                        MatchType.EXACT, null, null, "application.yml"),

                def("spring.tx-timeout", "SPRING", "Spring Boot", "세션·트랜잭션·Async",

                        "Spring Transaction Timeout", "Spring",

                        Nsight32Core256GbGuide.TRANSACTION_TIMEOUT_SEC_MIN + "~"

                                + Nsight32Core256GbGuide.TRANSACTION_TIMEOUT_SEC_MAX + " s",

                        "nsight.transaction.default-timeout-seconds",

                        MatchType.RANGE,

                        String.valueOf(Nsight32Core256GbGuide.TRANSACTION_TIMEOUT_SEC_MIN),

                        String.valueOf(Nsight32Core256GbGuide.TRANSACTION_TIMEOUT_SEC_MAX),

                        "application.yml"),

                def("spring.tx-readonly", "SPRING", "Spring Boot", "세션·트랜잭션·AP ID",

                        "Readonly Transaction", "Spring", "3 s", "nsight.transaction.readonly-timeout-seconds",

                        MatchType.EXACT, null, null, "application.yml"),

                def("spring.ap-id", "SPRING", "Spring Boot", "세션·트랜잭션·AP ID",

                        "AP ID", "Spring", "(운영별)", "nsight.ap-id",

                        MatchType.INFO_ONLY, null, null, "application.yml"),

                def("hikari.max-pool", "DB", "HikariCP / MyBatis", "DB·SQL·Pool (32Core)",

                        "maximumPoolSize (일반)", "HikariCP",

                        Nsight32Core256GbGuide.HIKARI_POOL_GENERAL + " (SingleView "

                                + Nsight32Core256GbGuide.HIKARI_POOL_SINGLEVIEW + ")",

                        "spring.datasource.hikari.maximum-pool-size",

                        MatchType.RANGE, "120", "150", "application.yml"),

                def("hikari.connection-timeout", "DB", "HikariCP / MyBatis", "DB·SQL·Pool (32Core)",

                        "connectionTimeout", "HikariCP", Nsight32Core256GbGuide.HIKARI_CONNECTION_TIMEOUT_MS + " ms",

                        "spring.datasource.hikari.connection-timeout",

                        MatchType.EXACT, null, null, "application.yml"),

                def("mybatis.statement-timeout", "DB", "HikariCP / MyBatis", "DB·SQL·Pool (32Core)",

                        "defaultStatementTimeout", "MyBatis",

                        Nsight32Core256GbGuide.MYBATIS_STATEMENT_TIMEOUT_SEC + "~3 s",

                        "mybatis.default-statement-timeout",

                        MatchType.RANGE, "2", "3", "mybatis-config.xml"),

                def("mybatis.fetch-size", "DB", "HikariCP / MyBatis", "DB·SQL·Pool",

                        "defaultFetchSize", "MyBatis", "300", "mybatis.default-fetch-size",

                        MatchType.EXACT, null, null, "mybatis-config.xml"),

                def("cruzapim.connect", "INTEGRATION", "CruzAPIM / 연계", "외부 연계 Timeout",

                        "Connect Timeout", "CruzAPIM", Nsight32Core256GbGuide.CRUZAPIM_CONNECT_TIMEOUT_MS + " ms",

                        "nsight.cruzapim.connect-timeout-ms",

                        MatchType.EXACT, null, null, "application.yml"),

                def("cruzapim.read", "INTEGRATION", "CruzAPIM / 연계", "외부 연계 Timeout",

                        "Read Timeout", "CruzAPIM", Nsight32Core256GbGuide.CRUZAPIM_READ_TIMEOUT_MS + " ms",

                        "nsight.cruzapim.read-timeout-ms",

                        MatchType.EXACT, null, null, "application.yml"),

                def("l4.idle-timeout", "L4", "L4 / 인프라", "L4·GSLB Timeout 계층",

                        "Client-L4 Idle", "L4", String.valueOf(Nsight32Core256GbGuide.L4_CLIENT_IDLE_TIMEOUT_SEC) + "s",

                        "nsight.env-check.l4-idle-timeout-ms",

                        MatchType.EXACT, null, null, "application.yml · L4 Console (70~90s)"),

                def("l4.health.interval", "L4", "L4 / 인프라", "Health Check",

                        "Health Check Interval", "L4", String.valueOf(Nsight32Core256GbGuide.L4_HEALTH_INTERVAL_SEC),

                        "nsight.env-check.l4-health-interval-sec",

                        MatchType.EXACT, null, null, "L4 Console"),

                def("l4.health.timeout", "L4", "L4 / 인프라", "Health Check",

                        "Health Check Timeout", "L4", String.valueOf(Nsight32Core256GbGuide.L4_HEALTH_TIMEOUT_SEC),

                        "nsight.env-check.l4-health-timeout-sec",

                        MatchType.EXACT, null, null, "L4 Console"),

                def("l4.health.fail-count", "L4", "L4 / 인프라", "Health Check",

                        "Health Check Fail Count", "L4", String.valueOf(Nsight32Core256GbGuide.L4_HEALTH_FAIL_COUNT),

                        "nsight.env-check.l4-health-fail-count",

                        MatchType.EXACT, null, null, "L4 Console · 3회 실패 시 제외"),

                def("l4.sticky-timeout", "L4", "L4 / 인프라", "Sticky·세션 연계",

                        "Sticky Timeout", "L4", String.valueOf(Nsight32Core256GbGuide.L4_STICKY_TIMEOUT_SEC),

                        "nsight.env-check.l4-sticky-timeout-sec",

                        MatchType.EXACT, null, null, "L4 Console · Session 60m보다 길게"),

                def("gslb.health.interval", "GSLB", "GSLB / 인프라", "Health Check",

                        "Health Check Interval", "GSLB", String.valueOf(Nsight32Core256GbGuide.GSLB_HEALTH_INTERVAL_SEC),

                        "nsight.env-check.gslb-health-interval-sec",

                        MatchType.EXACT, null, null, "GSLB Console"),

                def("gslb.health.timeout", "GSLB", "GSLB / 인프라", "Health Check",

                        "Health Check Timeout", "GSLB", String.valueOf(Nsight32Core256GbGuide.GSLB_HEALTH_TIMEOUT_SEC),

                        "nsight.env-check.gslb-health-timeout-sec",

                        MatchType.EXACT, null, null, "GSLB Console"),

                def("gslb.health.fail-count", "GSLB", "GSLB / 인프라", "Health Check",

                        "Health Check Fail Count", "GSLB", String.valueOf(Nsight32Core256GbGuide.GSLB_HEALTH_FAIL_COUNT),

                        "nsight.env-check.gslb-health-fail-count",

                        MatchType.EXACT, null, null, "GSLB Console"),

                def("gslb.sticky-timeout", "GSLB", "GSLB / 인프라", "Sticky·세션 연계",

                        "Sticky Timeout", "GSLB", String.valueOf(Nsight32Core256GbGuide.GSLB_STICKY_TIMEOUT_SEC),

                        "nsight.env-check.gslb-sticky-timeout-sec",

                        MatchType.EXACT, null, null, "GSLB Console"),

                def("proxy.apache-nginx", "NETWORK", "Proxy / L4", "인프라(참고)",

                        "Proxy Connect/Read", "Apache/Nginx", "3s / 10s", null,

                        MatchType.INFO_ONLY, null, null, "httpd.conf / nginx.conf"),

                def("actuator.health", "MONITOR", "모니터링", "Health·Actuator",

                        "Health Endpoint", "Actuator", "/actuator/health",

                        "management.endpoints.web.exposure.include",

                        MatchType.INFO_ONLY, null, null, "prometheus exposure 권장")

        );

    }



    private static EnvSettingDefinition def(

            String key,

            String categoryId,

            String categoryTitle,

            String categoryDesc,

            String label,

            String layer,

            String guideValue,

            String propertyKey,

            EnvSettingDefinition.MatchType matchType,

            String min,

            String max,

            String note

    ) {

        String source = note != null && note.contains(".") ? note.split(" · ")[0] : note;

        return new EnvSettingDefinition(

                key, categoryId, categoryTitle, categoryDesc,

                label, layer, guideValue, source, propertyKey, matchType, min, max, note

        );

    }

}


