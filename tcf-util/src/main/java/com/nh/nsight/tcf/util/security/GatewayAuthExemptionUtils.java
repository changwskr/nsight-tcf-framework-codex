package com.nh.nsight.tcf.util.security;

import com.nh.nsight.tcf.util.meta.CopiedFrom;
import com.nh.nsight.tcf.util.meta.CopiedUtilityFlag;
import com.nh.nsight.tcf.util.meta.UtilCategory;
import com.nh.nsight.tcf.util.string.TcfStringUtils;

/**
 * tcf-gateway {@code GatewayAuthExemptions} 복사본.
 */
@CopiedFrom(module = "tcf-gateway", sourceClass = "GatewayAuthExemptions", category = UtilCategory.SECURITY)
public final class GatewayAuthExemptionUtils implements CopiedUtilityFlag {

    public static final String COPIED_FROM_MODULE = "tcf-gateway";
    public static final String COPIED_FROM_CLASS = "GatewayAuthExemptions";

    private GatewayAuthExemptionUtils() {
    }

    public static boolean isLoginExempt(String serviceId) {
        if (!TcfStringUtils.hasText(serviceId)) {
            return false;
        }
        String id = serviceId.trim();
        return "OM.Auth.login".equals(id)
                || "OM.Auth.ssoLogin".equals(id)
                || "OM.Auth.logout".equals(id)
                || "OM.Auth.session".equals(id)
                || "JWT.Auth.login".equals(id)
                || "JWT.Auth.refresh".equals(id)
                || "JWT.Auth.revoke".equals(id)
                || "JWT.Auth.logout".equals(id);
    }
}
