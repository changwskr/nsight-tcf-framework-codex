package com.nh.nsight.tcf.batch.support.model;

public record SessionStatusSnapshot(
        String scopeId,
        String scopeName,
        int activeCount,
        int expiredCount,
        int totalCount,
        int uniqueUserCount,
        String healthStatus,
        String checkedAt,
        boolean reachable,
        String detailMessage
) {
}
