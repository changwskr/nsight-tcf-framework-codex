package com.nh.nsight.marketing.oc.application.dto.env;

import com.nh.nsight.marketing.oc.support.NsightCapacityDerivation;
import com.nh.nsight.marketing.oc.support.VmProfile;

import java.util.List;

/** 설계서 ENV-002 산정 조건 입력. */
public record CapacityPlannerRequest(
        String scenarioName,
        int branchCount,
        int usersPerBranch,
        int totalUsers,
        String vmProfileId,
        boolean customVm,
        int customCore,
        int customMemoryGb,
        int tpsPerCoreMin,
        int tpsPerCoreBase,
        int tpsPerCoreMax,
        int tpmcPerTps,
        boolean manualCoreTps,
        List<Integer> actualRequestPercents,
        List<Integer> responseTimeoutSeconds,
        List<Integer> sessionIdleMinutes,
        boolean activeActive,
        boolean drValidation,
        boolean validateDbPool,
        boolean includeSettingExamples,
        int hikariPoolPerVm,
        int dbSessionLimit
) {
    public CapacityPlannerRequest {
        if (actualRequestPercents == null || actualRequestPercents.isEmpty()) {
            actualRequestPercents = List.of(3, 5, 10, 15);
        }
        if (responseTimeoutSeconds == null || responseTimeoutSeconds.isEmpty()) {
            responseTimeoutSeconds = List.of(3, 4, 5);
        }
        if (sessionIdleMinutes == null || sessionIdleMinutes.isEmpty()) {
            sessionIdleMinutes = List.of(60);
        }
        if (tpsPerCoreMin <= 0) {
            tpsPerCoreMin = 30;
        }
        if (tpsPerCoreBase <= 0) {
            tpsPerCoreBase = 35;
        }
        if (tpsPerCoreMax <= 0) {
            tpsPerCoreMax = 40;
        }
        if (tpmcPerTps <= 0) {
            tpmcPerTps = 3000;
        }
        if (!manualCoreTps) {
            var linked = NsightCapacityDerivation.coreTpsFromTpmc(tpmcPerTps);
            tpsPerCoreMin = linked.tpsPerCoreMin();
            tpsPerCoreBase = linked.tpsPerCoreBase();
            tpsPerCoreMax = linked.tpsPerCoreMax();
        }
        if (hikariPoolPerVm < 0) {
            hikariPoolPerVm = 0;
        }
        if (dbSessionLimit <= 0) {
            dbSessionLimit = 500;
        }
        if (totalUsers <= 0 && branchCount > 0 && usersPerBranch > 0) {
            totalUsers = branchCount * usersPerBranch;
        }
        if (customVm && customCore > 0) {
            int mem = customMemoryGb > 0 ? customMemoryGb : 64;
            vmProfileId = customCore + "CORE-" + mem + "GB";
        } else {
            customVm = false;
            customCore = 0;
            customMemoryGb = 0;
            String resolved = VmProfile.normalizeProfileId(vmProfileId);
            vmProfileId = resolved != null ? resolved : VmProfile.defaultProfile().getId();
        }
    }
}
