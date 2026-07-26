package com.nh.nsight.marketing.oc.application.dto.env;

public record CapacityTimeoutMatrixRow(
        int percent,
        int actualRequestUsers,
        int responseTimeoutSec,
        int peakTps,
        long tpmcTotal
) {
}
