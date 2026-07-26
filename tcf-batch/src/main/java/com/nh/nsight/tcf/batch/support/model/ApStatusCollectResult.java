package com.nh.nsight.tcf.batch.support.model;

import java.util.List;

public record ApStatusCollectResult(
        String jobId,
        String runTime,
        String runStatus,
        long durationMs,
        int targetCount,
        int successCount,
        int failCount,
        List<ApStatusSnapshot> snapshots,
        String message
) {
}
