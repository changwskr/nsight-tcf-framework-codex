package com.nh.nsight.tcf.batch.support.model;

public record DeployStatusSnapshot(
        String businessCode,
        String warName,
        String warVersion,
        String deployedAt,
        String healthStatus,
        String checkedAt,
        boolean reachable,
        String detailMessage
) {
}
