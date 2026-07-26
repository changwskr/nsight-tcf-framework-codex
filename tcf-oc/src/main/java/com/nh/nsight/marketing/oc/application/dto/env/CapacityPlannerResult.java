package com.nh.nsight.marketing.oc.application.dto.env;

import java.util.List;
import java.util.Map;

public record CapacityPlannerResult(
        String scenarioLabel,
        int branchCount,
        int usersPerBranch,
        int totalUsers,
        int designSessions,
        String vmProfileId,
        int vmCores,
        int vmMemoryGb,
        int vmTpsAt30,
        int vmTpsAt35,
        int vmTpsAt40,
        int tpsPerCoreMin,
        int tpsPerCoreBase,
        int tpsPerCoreMax,
        int tpmcPerTps,
        int coreTpmcPerSec,
        boolean coreTpsLinkedToTpmc,
        int primarySessionMinutes,
        boolean activeActive,
        int hikariPoolPerVm,
        String hikariPoolRangeLabel,
        String hikariPoolFormula,
        String hikariPoolDerivationFormula,
        int hikariPoolMaxSingleView,
        int hikariPoolMinSingleView,
        String jvmHeapDerivationFormula,
        String tomcatMaxThreadsRange,
        String tomcatBusyThreadFormula,
        String tomcatSizingNote,
        String wasThreadsDerivationFormula,
        String tomcatMinSpareRange,
        String tomcatAcceptRange,
        String tomcatMaxConnectionsRange,
        String hikariSingleViewRange,
        String tomcatHikariCaution,
        String tomcatHikariOperationalNote,
        int dbSessionLimit,
        List<CapacityVmResultRow> vmResults,
        List<CapacityTimeoutMatrixRow> timeoutMatrix,
        Map<String, Integer> riskSummary,
        String summaryFormula
) {
}
