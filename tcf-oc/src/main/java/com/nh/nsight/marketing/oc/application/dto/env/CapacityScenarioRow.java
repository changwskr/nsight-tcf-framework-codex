package com.nh.nsight.marketing.oc.application.dto.env;

public record CapacityScenarioRow(
        int percent,
        int actualRequestUsers,
        int peakTps,
        long tpmcTotal,
        int coresRequiredMin,
        int coresRequiredMax,
        boolean withinVmMaxTps,
        int concurrentPerAp
) {
}
