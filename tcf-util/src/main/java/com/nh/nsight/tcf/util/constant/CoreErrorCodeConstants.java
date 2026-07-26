package com.nh.nsight.tcf.util.constant;

import com.nh.nsight.tcf.util.meta.CopiedFrom;
import com.nh.nsight.tcf.util.meta.CopiedUtilityFlag;
import com.nh.nsight.tcf.util.meta.UtilCategory;

/**
 * tcf-core {@code ErrorCode} 복사본 (상수만).
 */
@CopiedFrom(module = "tcf-core", sourceClass = "ErrorCode", category = UtilCategory.CONSTANT)
public final class CoreErrorCodeConstants implements CopiedUtilityFlag {

    public static final String COPIED_FROM_MODULE = "tcf-core";
    public static final String COPIED_FROM_CLASS = "ErrorCode";

    public static final String INVALID_HEADER = "E-COM-VALID-0001";
    public static final String SERVICE_NOT_FOUND = "E-COM-DISP-0001";
    public static final String SESSION_INVALID = "E-COM-AUTH-0001";
    public static final String AUTHORIZATION_DENIED = "E-COM-AUTH-0002";
    public static final String DUPLICATE_REQUEST = "E-COM-IDEMP-0001";
    public static final String TXCTRL_HDR_SERVICE_ID = "E-TCF-HDR-001";
    public static final String TXCTRL_HDR_TRANSACTION_CODE = "E-TCF-HDR-002";
    public static final String TXCTRL_HDR_BUSINESS_CODE = "E-TCF-HDR-003";
    public static final String TXCTRL_HDR_SERVICE_NAME = "E-TCF-HDR-004";
    public static final String TXCTRL_HDR_USER = "E-TCF-HDR-005";
    public static final String TXCTRL_HDR_CHANNEL_ID = "E-TCF-HDR-006";
    public static final String TXCTRL_HDR_BRANCH = "E-TCF-HDR-007";
    public static final String TXCTRL_NOT_ALLOWED = "E-TCF-CTL-001";
    public static final String TXCTRL_DUPLICATE = "E-TCF-CTL-002";
    public static final String TXCTRL_UNAVAILABLE = "E-TCF-CTL-003";
    public static final String TIMEOUT_ONLINE = "E-TCF-TIME-001";
    public static final String TIMEOUT_TRANSACTION = "E-TCF-TIME-002";
    public static final String TIMEOUT_DB_QUERY = "E-TCF-TIME-003";
    public static final String TIMEOUT_DB_CONNECTION = "E-TCF-TIME-004";
    public static final String TIMEOUT_EXTERNAL_CONNECT = "E-TCF-TIME-005";
    public static final String TIMEOUT_EXTERNAL_READ = "E-TCF-TIME-006";
    public static final String BUSINESS_ERROR = "E-COM-BIZ-0001";
    public static final String SYSTEM_ERROR = "E-COM-SYS-0001";

    private CoreErrorCodeConstants() {
    }
}
