package com.nh.nsight.gateway.application.rule;

import java.util.Set;
import org.springframework.util.StringUtils;

public final class GatewaySessionHeaderRules {
    private static final Set<String> PLACEHOLDER_USER_IDS = Set.of(
            "GUEST", "guest", "ANONYMOUS", "anonymous", "SYSTEM", "system");

    private GatewaySessionHeaderRules() {
    }

    public static boolean isPlaceholderUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            return true;
        }
        return PLACEHOLDER_USER_IDS.contains(userId.trim());
    }
}
