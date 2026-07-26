package com.nh.nsight.tcf.core.support.control;

public final class TcfTransactionControlConstants {
    private TcfTransactionControlConstants() {}

    public static final String TABLE_NAME = "TCF_TRANSACTION_CONTROL";
    public static final String CONTROL_TYPE_GLOBAL = "GLOBAL";
    public static final String CONTROL_TYPE_BUSINESS = "BUSINESS";
    public static final String CONTROL_TYPE_SERVICE = "SERVICE";
    public static final String CONTROL_TYPE_CHANNEL = "CHANNEL";
    public static final String CONTROL_TYPE_BRANCH = "BRANCH";
    public static final String CONTROL_TYPE_USER = "USER";
    public static final String CONTROL_TYPE_IP = "IP";
    /** 레거시 7필드 일치 — 신규 등록 시 사용하지 않음 */
    @Deprecated
    public static final String CONTROL_TYPE_FULL = "FULL";
    public static final String GLOBAL_WILDCARD = "*";
    public static final String BLOCK_YES = "Y";
    public static final String BLOCK_NO = "N";
}
