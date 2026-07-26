package com.nh.nsight.marketing.oc.application.dto.env;

/** 설계서 ENV-003 TPS·VM 산정 결과 행. */
public record CapacityVmResultRow(
        int requestRatePercent,
        int timeoutSec,
        int realRequesters,
        int targetTps,
        long requiredTpmc,
        String vmProfileLabel,
        int vmTpsAtBase,
        int requiredVmSingleCenter,
        int recommendedVmActiveActive,
        /** VM 1대당 일반 AP JVM Heap (GB). */
        String jvmHeapPerVm,
        /** VM 1대당 SingleView Heap 상한. */
        String jvmHeapSvPerVm,
        /** VM 1대당 Tomcat maxThreads 권장 범위. */
        String wasThreadsPerVm,
        int dbPoolPerVm,
        String dbPoolRangeLabel,
        String dbPoolFormula,
        long dbPoolTotal,
        String status,
        String statusReason
) {
}
