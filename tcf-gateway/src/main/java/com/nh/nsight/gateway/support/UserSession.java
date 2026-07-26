package com.nh.nsight.gateway.support;

import java.time.Instant;

public record UserSession(
        String sessionId,
        String userId,
        String userName,
        String branchId,
        String channelId,
        String authGroupId,
        SessionType sessionType,
        Instant loginTime,
        Instant lastAccessTime,
        Instant absoluteExpireTime,
        String clientIp,
        String userAgent,
        String centerId,
        String wasId,
        SessionStatus status,
        Instant logoutTime,
        String logoutReason) {

    public boolean isActive(Instant now) {
        return status == SessionStatus.ACTIVE
                && absoluteExpireTime != null
                && absoluteExpireTime.isAfter(now);
    }
}
