package com.nh.nsight.auth.jwt.support;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import org.springframework.util.StringUtils;

public final class JwtClientContext {
    private JwtClientContext() {}

    public static String clientIp(TransactionContext context) {
        if (context == null || context.getHeader() == null) {
            return null;
        }
        return context.getHeader().getClientIp();
    }

    public static String channelId(TransactionContext context) {
        if (context == null || context.getHeader() == null
                || !StringUtils.hasText(context.getHeader().getChannelId())) {
            return "WEBTOP";
        }
        return context.getHeader().getChannelId();
    }
}
