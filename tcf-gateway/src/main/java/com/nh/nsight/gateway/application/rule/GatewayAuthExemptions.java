package com.nh.nsight.gateway.application.rule;

import org.springframework.util.StringUtils;

/** gateway 세션 검증 제외 serviceId */
public final class GatewayAuthExemptions {
    private GatewayAuthExemptions() {
    }

    public static boolean isLoginExempt(String serviceId) {
        if (!StringUtils.hasText(serviceId)) {
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
