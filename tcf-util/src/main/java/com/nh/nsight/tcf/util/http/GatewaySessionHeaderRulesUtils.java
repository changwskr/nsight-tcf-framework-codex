package com.nh.nsight.tcf.util.http;

import com.nh.nsight.tcf.util.meta.CopiedFrom;
import com.nh.nsight.tcf.util.meta.CopiedUtilityFlag;
import com.nh.nsight.tcf.util.meta.UtilCategory;
import com.nh.nsight.tcf.util.string.TcfStringUtils;
import java.util.Set;

/**
 * tcf-gateway {@code GatewaySessionHeaderRules} 복사본.
 */
@CopiedFrom(module = "tcf-gateway", sourceClass = "GatewaySessionHeaderRules", category = UtilCategory.HTTP)
public final class GatewaySessionHeaderRulesUtils implements CopiedUtilityFlag {

    public static final String COPIED_FROM_MODULE = "tcf-gateway";
    public static final String COPIED_FROM_CLASS = "GatewaySessionHeaderRules";

    private static final Set<String> PLACEHOLDER_USER_IDS = Set.of(
            "GUEST", "guest", "ANONYMOUS", "anonymous", "SYSTEM", "system");

    private GatewaySessionHeaderRulesUtils() {
    }

    public static boolean isPlaceholderUserId(String userId) {
        if (!TcfStringUtils.hasText(userId)) {
            return true;
        }
        return PLACEHOLDER_USER_IDS.contains(userId.trim());
    }
}
