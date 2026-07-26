package com.nh.nsight.marketing.oc.support;

/**
 * 32 Core / 256GB 기준 상수.
 * <ul>
 *   <li>NSIGHT_32Core_256GB_환경설정_작업가이드.docx (2026-05-29)</li>
 *   <li>NSIGHT_용량산정_세션60분_32core_256G_기준.docx (2026-05-31)</li>
 * </ul>
 */
public final class Nsight32Core256GbGuide {

    public static final String PROFILE_ID = "nsight-32core-256gb";
    public static final String PROFILE_TITLE = "NSIGHT 32 Core / 256GB";
    public static final String VM_SPEC = "32 vCPU / 256GB";
    public static final String CAPACITY_DOC = "NSIGHT_용량산정_세션60분_32core_256G_기준.docx";

    /** 지점 6,000 × 지점당 6명 */
    public static final int BRANCH_COUNT = 6_000;
    public static final int USERS_PER_BRANCH = 6;
    public static final int TOTAL_USERS = 36_000;

    /** 실요청 사용자 — 전체 사용자 대비 동시 실요청 (용량산정 시나리오) */
    public static final int ACTUAL_REQUEST_USERS_LOW_LOAD = 1_080;
    public static final int ACTUAL_REQUEST_USERS_PEAK = 1_800;
    public static final int ACTUAL_REQUEST_USERS_HIGH_PEAK = 3_600;
    public static final int ACTUAL_REQUEST_USERS_STRESS = 5_400;
    public static final int ACTUAL_REQUEST_PERCENT_PEAK = 5;

    /** 세션(로그인 유지) — 동시 요청자(TPS)와 별도 */
    public static final int SESSION_DESIGN_COUNT = 36_000;
    public static final int SESSION_BUFFERED_MIN = 43_000;
    public static final int SESSION_BUFFERED_MAX = 47_000;
    public static final int SESSION_IDLE_MINUTES = 60;
    public static final int SESSION_SIZE_TARGET_KB = 2;
    public static final int SESSION_SIZE_MAX_KB = 5;

    public static final int VM_MAX_TPS = 1_000;

    /** 동시요청자÷3초 — 용량산정 시나리오 (전체 세션 36,000 기준) */
    public static final int LOW_LOAD_TPS = 360;
    /** 실요청(5%)과 동일 — peak-concurrent-users 설정값 */
    public static final int PEAK_CONCURRENT_USERS = ACTUAL_REQUEST_USERS_PEAK;
    public static final int PEAK_TPS = 600;
    public static final int HIGH_PEAK_CONCURRENT_USERS = 3_600;
    public static final int HIGH_PEAK_TPS = 1_200;
    public static final int STRESS_CONCURRENT_USERS = 5_400;
    public static final int STRESS_TPS = 1_800;

    public static final int TARGET_P95_MS = 3_000;
    /** 운영 권장 최소 AP 대수 (이중화·DR) */
    public static final int DEFAULT_AP_COUNT = 2;

    public static final int JVM_HEAP_GENERAL_GB_MIN = 32;
    public static final int JVM_HEAP_GENERAL_GB_MAX = 48;
    public static final int JVM_HEAP_SINGLEVIEW_GB_MAX = 64;

    /** §4 성능시험 1차 — maxThreads 1,200~1,500 (운영 시 Busy Thread·CPU 등으로 보정). */
    public static final int TOMCAT_MAX_THREADS_MIN = 1_200;
    public static final int TOMCAT_MAX_THREADS = 1_500;
    public static final int TOMCAT_MIN_SPARE_THREADS_MIN = 200;
    public static final int TOMCAT_MIN_SPARE_THREADS = 300;
    public static final int TOMCAT_ACCEPT_COUNT_MIN = 500;
    public static final int TOMCAT_ACCEPT_COUNT = 800;
    public static final int TOMCAT_MAX_CONNECTIONS_MIN = 20_000;
    public static final int TOMCAT_MAX_CONNECTIONS = 30_000;
    public static final int TOMCAT_CONNECTION_TIMEOUT_MS = 8_000;
    public static final int TOMCAT_KEEP_ALIVE_TIMEOUT_SEC = TomcatWasSizingGuide.KEEP_ALIVE_TIMEOUT_SEC;
    public static final int TOMCAT_MAX_KEEP_ALIVE_REQUESTS = TomcatWasSizingGuide.MAX_KEEP_ALIVE_REQUESTS;

    public static final int HIKARI_POOL_GENERAL = 150;
    public static final int HIKARI_POOL_SINGLEVIEW = 180;
    public static final int HIKARI_CONNECTION_TIMEOUT_MS = 3_000;

    public static final int MYBATIS_STATEMENT_TIMEOUT_SEC = 2;
    public static final int TRANSACTION_TIMEOUT_SEC_MIN = 4;
    public static final int TRANSACTION_TIMEOUT_SEC_MAX = 5;

    /** 비동기 로그/감사/이벤트 — 업무 Tomcat Thread와 분리 */
    public static final int ASYNC_CORE_POOL_MIN = 50;
    public static final int ASYNC_CORE_POOL_MAX = 100;
    public static final int ASYNC_MAX_POOL_MIN = 100;
    public static final int ASYNC_MAX_POOL_MAX = 200;

    public static final int WEBTOP_REQUEST_TIMEOUT_MS = 15_000;
    public static final int WEBTOP_CONNECT_TIMEOUT_MS = 3_000;
    public static final int WEBTOP_READ_TIMEOUT_MS = 10_000;

    public static final int CRUZAPIM_CONNECT_TIMEOUT_MS = 3_000;
    public static final int CRUZAPIM_READ_TIMEOUT_MS = 5_000;

    /** Client–L4 / L4–WAS 유휴 (32CORE 권장 70~90초, 기본 80초). */
    public static final int L4_CLIENT_IDLE_TIMEOUT_SEC = 80;
    public static final int L4_WAS_IDLE_TIMEOUT_SEC = 80;
    /** @deprecated {@link #L4_CLIENT_IDLE_TIMEOUT_SEC} 기준 ms */
    public static final int L4_IDLE_TIMEOUT_MS = L4_CLIENT_IDLE_TIMEOUT_SEC * 1000;
    public static final int L4_HEALTH_INTERVAL_SEC = 5;
    public static final int L4_HEALTH_TIMEOUT_SEC = 2;
    public static final int L4_HEALTH_FAIL_COUNT_MIN = 2;
    public static final int L4_HEALTH_FAIL_COUNT = 3;
    public static final int L4_STICKY_TIMEOUT_MIN = 70;
    public static final int L4_STICKY_TIMEOUT_MAX = 80;
    public static final int L4_STICKY_TIMEOUT_SEC = 70;

    public static final int GSLB_HEALTH_INTERVAL_SEC = 5;
    public static final int GSLB_HEALTH_TIMEOUT_SEC = 2;
    public static final int GSLB_HEALTH_FAIL_COUNT = 3;
    public static final int GSLB_STICKY_TIMEOUT_SEC = 70;

    public static final int PROXY_READ_TIMEOUT_MS = 10_000;
    public static final int ABSOLUTE_SESSION_HOURS = 8;

    public static final int DB_SESSION_LIMIT_REF = 500;

    private Nsight32Core256GbGuide() {
    }
}
