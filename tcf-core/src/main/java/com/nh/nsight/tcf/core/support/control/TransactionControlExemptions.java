package com.nh.nsight.tcf.core.support.control;

import org.springframework.util.StringUtils;

/** 거래통제 검사에서 제외하는 serviceId (로그인·헬스체크·거래통제 관리 등) */
public final class TransactionControlExemptions {
    private TransactionControlExemptions() {}

    public static boolean isExempt(String serviceId) {
        if (!StringUtils.hasText(serviceId)) {
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

    /** OM.TransactionControl.inquiry/save/update/delete — 거래통제 관리 화면 */
    public static boolean isTransactionControlAdmin(String serviceId) {
        if (!StringUtils.hasText(serviceId)) {
            return false;
        }
        return serviceId.trim().startsWith("OM.TransactionControl.");
    }

    /** 거래통제 관리 화면 콤보(TX_CONTROL_TYPE, BUSINESS_CODE 등) 조회 */
    public static boolean isTransactionControlPageSupport(String serviceId) {
        return "OM.CommonCode.inquiry".equals(StringUtils.hasText(serviceId) ? serviceId.trim() : null);
    }

    /** OM.TimeoutPolicy.inquiry/save/update/delete — Timeout 정책 관리 화면 */
    public static boolean isTimeoutPolicyAdmin(String serviceId) {
        if (!StringUtils.hasText(serviceId)) {
            return false;
        }
        return serviceId.trim().startsWith("OM.TimeoutPolicy.");
    }

    /** JWT.Token / LoginHistory / RefreshToken / SecurityPolicy — JWT 관리 화면 */
    public static boolean isJwtAdmin(String serviceId) {
        if (!StringUtils.hasText(serviceId)) {
            return false;
        }
        String id = serviceId.trim();
        return id.startsWith("JWT.Token.")
                || id.startsWith("JWT.LoginHistory.")
                || id.startsWith("JWT.RefreshToken.")
                || id.startsWith("JWT.SecurityPolicy.");
    }

    /** OM.HealthCheck.inquiry, OM.Deploy.healthCheck 및 *.HealthCheck.* 패턴 */
    public static boolean isHealthCheck(String serviceId) {
        if (!StringUtils.hasText(serviceId)) {
            return false;
        }
        String id = serviceId.trim();
        return "OM.HealthCheck.inquiry".equals(id)
                || id.endsWith(".healthCheck")
                || id.contains(".HealthCheck.");
    }
}
