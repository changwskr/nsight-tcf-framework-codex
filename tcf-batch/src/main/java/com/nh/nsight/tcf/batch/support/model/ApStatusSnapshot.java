package com.nh.nsight.tcf.batch.support.model;

import java.util.Map;

public record ApStatusSnapshot(
        String apId,
        String apName,
        String healthStatus,
        double cpuUsagePct,
        double heapUsagePct,
        int threadCount,
        String checkedAt,
        boolean reachable,
        String detailMessage
) {
    public Map<String, Object> toRow() {
        return Map.of(
                "apId", apId,
                "apName", apName,
                "healthStatus", healthStatus,
                "cpuUsagePct", cpuUsagePct,
                "heapUsagePct", heapUsagePct,
                "threadCount", threadCount,
                "checkedAt", checkedAt
        );
    }
}
