package com.nh.nsight.marketing.oc.application.dto.env;

import java.util.Map;

public record ProjectBaselineView(
        String projectId,
        String projectName,
        String envCode,
        String hardwareProfile,
        String capacityDocRef,
        String centerType,
        int branchCount,
        int usersPerBranch,
        int totalUsers,
        int actualRequestUsers,
        int actualRequestPeakPercent,
        int sessionDesignCount,
        int sessionBufferedMin,
        int sessionBufferedMax,
        int baseTps,
        int peakTps,
        int highPeakTps,
        int stressTps,
        int vmMaxTps,
        int peakConcurrentUsers,
        int apCount,
        String apVmSpec,
        int targetP95Ms,
        Map<String, String> deploymentSummary
) {
}
