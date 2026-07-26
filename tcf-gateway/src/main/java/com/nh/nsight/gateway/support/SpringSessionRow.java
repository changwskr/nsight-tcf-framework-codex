package com.nh.nsight.gateway.support;

import java.time.Instant;

public record SpringSessionRow(
        String sessionId,
        String userId,
        long creationTime,
        long lastAccessTime,
        int maxInactiveInterval,
        long expiryTime) {

    public boolean isActive(long nowEpochMillis) {
        return expiryTime > nowEpochMillis;
    }

    public Instant expiryInstant() {
        return Instant.ofEpochMilli(expiryTime);
    }
}
