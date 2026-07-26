package com.nh.nsight.tcf.util.security;

import com.nh.nsight.tcf.util.meta.CopiedFrom;
import com.nh.nsight.tcf.util.meta.CopiedUtilityFlag;
import com.nh.nsight.tcf.util.meta.UtilCategory;
import com.nh.nsight.tcf.util.string.TcfStringUtils;

/**
 * tcf-core {@code TransactionControlExemptions} 복사본.
 */
@CopiedFrom(module = "tcf-core", sourceClass = "TransactionControlExemptions", category = UtilCategory.SECURITY)
public final class CoreTransactionControlExemptionUtils implements CopiedUtilityFlag {

    public static final String COPIED_FROM_MODULE = "tcf-core";
    public static final String COPIED_FROM_CLASS = "TransactionControlExemptions";

    private CoreTransactionControlExemptionUtils() {
    }

    public static boolean isExempt(String serviceId) {
        if (!TcfStringUtils.hasText(serviceId)) {
            return false;
        }
        String id = serviceId.trim();
        if (isAuthBootstrap(id)) {
            return true;
        }
        if (isTransactionControlAdmin(id)) {
            return true;
        }
        if (isTransactionControlPageSupport(id)) {
            return true;
        }
        if (isTimeoutPolicyAdmin(id)) {
            return true;
        }
        if (isJwtAdmin(id)) {
            return true;
        }
        return isHealthCheck(id);
    }

    public static boolean isAuthBootstrap(String serviceId) {
        return "OM.Auth.login".equals(serviceId)
                || "OM.Auth.ssoLogin".equals(serviceId)
                || "OM.Auth.logout".equals(serviceId)
                || "OM.Auth.session".equals(serviceId)
                || "JWT.Auth.login".equals(serviceId)
                || "JWT.Auth.ssoIssue".equals(serviceId)
                || "JWT.Auth.refresh".equals(serviceId)
                || "JWT.Auth.revoke".equals(serviceId)
                || "JWT.Auth.logout".equals(serviceId);
    }

    public static boolean isTransactionControlAdmin(String serviceId) {
        if (!TcfStringUtils.hasText(serviceId)) {
            return false;
        }
        return serviceId.trim().startsWith("OM.TransactionControl.");
    }

    public static boolean isTransactionControlPageSupport(String serviceId) {
        return "OM.CommonCode.inquiry".equals(TcfStringUtils.hasText(serviceId) ? serviceId.trim() : null);
    }

    public static boolean isTimeoutPolicyAdmin(String serviceId) {
        if (!TcfStringUtils.hasText(serviceId)) {
            return false;
        }
        return serviceId.trim().startsWith("OM.TimeoutPolicy.");
    }

    public static boolean isJwtAdmin(String serviceId) {
        if (!TcfStringUtils.hasText(serviceId)) {
            return false;
        }
        String id = serviceId.trim();
        return id.startsWith("JWT.Token.")
                || id.startsWith("JWT.LoginHistory.")
                || id.startsWith("JWT.RefreshToken.")
                || id.startsWith("JWT.SecurityPolicy.");
    }

    public static boolean isHealthCheck(String serviceId) {
        if (!TcfStringUtils.hasText(serviceId)) {
            return false;
        }
        String id = serviceId.trim();
        return "OM.HealthCheck.inquiry".equals(id)
                || id.endsWith(".healthCheck")
                || id.contains(".HealthCheck.");
    }
}
