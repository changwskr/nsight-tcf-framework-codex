package com.nh.nsight.tcf.batch.support.model;

import java.util.List;

public record SessionStatusCollectResult(
        String jobId,
        String runTime,
        String runStatus,
        long durationMs,
        int targetCount,
        int successCount,
        int failCount,
        int totalActiveCount,
        int totalExpiredCount,
        int totalUniqueUsers,
        List<SessionStatusSnapshot> snapshots,
        String message
) {
}
