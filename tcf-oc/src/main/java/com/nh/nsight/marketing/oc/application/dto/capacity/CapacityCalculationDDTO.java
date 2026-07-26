package com.nh.nsight.marketing.oc.application.dto.capacity;

import java.util.List;

public record CapacityCalculationDDTO(
        String projectName,
        int branchCount,
        int userPerBranch,
        int totalUsers,
        int designedSessions,
        double sessionMarginRate,
        int sessionTimeoutMin,
        List<Double> concurrentRequestRates,
        List<Integer> targetResponseTimes,
        String vmProfileId,
        int vmCores,
        int vmMemoryGb,
        int vmTpsAtBase,
        int tpsPerCore,
        int tpmcPerTps,
        double avgThreadHoldSec,
        double threadMarginRate,
        double maxThreadMarginRate,
        boolean singleViewAp,
        boolean activeActive,
        boolean drValidation,
        boolean validateDbPool,
        int dbSessionLimit,
        double avgDbConnectionHoldSec,
        double dbTransactionUsageRatio,
        double poolSafetyFactor,
        double threadDbUsageRatio,
        int minPoolPerVm,
        int profilePoolCap
) {
}
