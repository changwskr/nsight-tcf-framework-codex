package com.nh.nsight.tcf.core.support.timeout;

/** TCF_SERVICE_TIMEOUT_POLICY 테이블 및 기본 Timeout 정책 상수 */
public final class TcfServiceTimeoutConstants {
    public static final String TABLE_NAME = "TCF_SERVICE_TIMEOUT_POLICY";

    public static final String TIMEOUT_ACTION_FAIL = "FAIL";
    public static final String TIMEOUT_ACTION_UNKNOWN = "UNKNOWN";
    public static final String TIMEOUT_ACTION_STATUS_CHECK = "STATUS_CHECK";

    public static final int DEFAULT_ONLINE_TIMEOUT_SEC = 5;
    public static final int DEFAULT_TX_TIMEOUT_SEC = 5;
    public static final int DEFAULT_DB_QUERY_TIMEOUT_SEC = 3;
    public static final int DEFAULT_EXTERNAL_CONNECT_TIMEOUT_MS = 3000;
    public static final int DEFAULT_EXTERNAL_READ_TIMEOUT_MS = 5000;

    public static final String CONTEXT_ATTR = "timeoutPolicy";

    private TcfServiceTimeoutConstants() {}
}
