package com.nh.nsight.tcf.batch.support.model;

public record DbStatusSnapshot(
        String dbId,
        String dbName,
        String healthStatus,
        double poolUsagePct,
        String checkedAt,
        boolean reachable,
        String detailMessage
) {
}
