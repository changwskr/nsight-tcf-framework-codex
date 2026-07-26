package com.nh.nsight.marketing.oc.capnew.application.service;

import com.nh.nsight.marketing.oc.application.dto.capacity.WasThreadResultCDTO;
import com.nh.nsight.marketing.oc.application.dto.env.JvmSizingRecommendation;
import com.nh.nsight.marketing.oc.application.service.DCCapacity;
import com.nh.nsight.marketing.oc.capnew.support.CapNewBizException;
import com.nh.nsight.marketing.oc.capnew.support.CapNewWarPoolAllocation;
import com.nh.nsight.marketing.oc.support.JvmSizingGuide;
import com.nh.nsight.marketing.oc.support.NsightCapacityDerivation;
import com.nh.nsight.marketing.oc.support.NsightDbPoolDerivation;
import com.nh.nsight.marketing.oc.support.VmProfile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * NEW 용량산정 STEP 4~8 산정 — 기존 support/DCCapacity 참조만 (수정 없음).
 */
@Service
public class CapNewDerivationService {

    private final DCCapacity dcCapacity;

    public CapNewDerivationService(DCCapacity dcCapacity) {
        this.dcCapacity = dcCapacity;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> enrichStep4(Map<String, Object> step4, Map<String, Object> allPayload) {
        Map<String, Object> enriched = new LinkedHashMap<>(step4);
        VmProfile profile = resolveVmProfile(text(step4.get("vmProfileCode"), "16CORE-128GB"));
        int tpmcPerTps = intValue(step4.get("tpmcPerTps"), 3000);
        int tpsPerCore = intValue(step4.get("tpsPerCore"), 0);
        if (tpsPerCore <= 0) {
            tpsPerCore = NsightCapacityDerivation.coreTpsFromTpmc(tpmcPerTps).tpsPerCoreBase();
        }

        double cpuTarget = doubleValue(step4.get("cpuTargetUtilization"), 0.70);
        double perfSafety = doubleValue(step4.get("perfSafetyFactor"), 1.20);
        double virtFactor = doubleValue(step4.get("virtualizationFactor"), 0.90);
        double opsFactor = doubleValue(step4.get("opsEfficiencyFactor"), 0.85);
        boolean applyCorrection = boolValue(step4.get("applyCorrectionFactors"), true);

        int vmTheoreticalTps = profile.getCores() * tpsPerCore;
        int vmAdjustedTps = vmTheoreticalTps;
        if (applyCorrection) {
            vmAdjustedTps = (int) Math.floor(vmTheoreticalTps * cpuTarget * virtFactor * opsFactor / perfSafety);
            vmAdjustedTps = Math.max(1, vmAdjustedTps);
        }

        int designPeakTps = resolveBaselineTps(allPayload);
        int minRequiredAp = vmAdjustedTps > 0 ? (int) Math.ceil((double) designPeakTps / vmAdjustedTps) : 0;
        int minRequiredCores = (int) Math.ceil((double) designPeakTps / Math.max(1, tpsPerCore));

        enriched.put("businessTypeCode", text(step4.get("businessTypeCode"), "SINGLE_VIEW"));
        enriched.put("vmProfileCode", profile.getId());
        enriched.put("vmCores", profile.getCores());
        enriched.put("vmMemoryGb", profile.getMemoryGb());
        enriched.put("tpmcPerTps", tpmcPerTps);
        enriched.put("tpsPerCore", tpsPerCore);
        enriched.put("vmTheoreticalTps", vmTheoreticalTps);
        enriched.put("vmAdjustedTps", vmAdjustedTps);
        enriched.put("designPeakTps", designPeakTps);
        enriched.put("minRequiredAp", minRequiredAp);
        enriched.put("minRequiredCores", minRequiredCores);
        enriched.put("cpuTargetUtilization", cpuTarget);
        enriched.put("perfSafetyFactor", perfSafety);
        enriched.put("virtualizationFactor", virtFactor);
        enriched.put("opsEfficiencyFactor", opsFactor);
        enriched.put("applyCorrectionFactors", applyCorrection);
        return enriched;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> enrichStep5(Map<String, Object> step5, Map<String, Object> allPayload) {
        Map<String, Object> enriched = new LinkedHashMap<>(step5);
        Map<String, Object> step4 = mapValue(allPayload.get("step4"));
        int vmEffectiveTps = intValue(step4.get("vmAdjustedTps"), intValue(step4.get("vmTheoreticalTps"), 1));

        String centerMode = text(step5.get("centerMode"), "ACTIVE_ACTIVE");
        int apMargin = intValue(step5.get("apMarginPerCenter"), 1);
        int minApPerCenter = intValue(step5.get("minApPerCenter"), 2);
        boolean drFullLoad = boolValue(step5.get("drSingleCenterFullLoad"), true);
        int centerCount = centerCount(centerMode);

        List<Map<String, Object>> scenarioRows = new ArrayList<>();
        List<Map<String, Object>> scenarios = enabledScenarios(allPayload);
        for (Map<String, Object> scenario : scenarios) {
            int targetTps = intValue(scenario.get("targetTps"), 0);
            int singleCenterAp = vmEffectiveTps > 0 ? (int) Math.ceil((double) targetTps / vmEffectiveTps) : 0;
            int normalPerCenterAp = "ACTIVE_ACTIVE".equals(centerMode)
                    ? (int) Math.ceil((double) targetTps / 2.0 / Math.max(1, vmEffectiveTps)) + apMargin
                    : singleCenterAp + apMargin;
            normalPerCenterAp = Math.max(minApPerCenter, normalPerCenterAp);
            int failoverAp = drFullLoad
                    ? Math.max(minApPerCenter, singleCenterAp + apMargin)
                    : normalPerCenterAp;
            int totalAp = normalPerCenterAp * centerCount;
            String judgment = classifyAp(targetTps, vmEffectiveTps, failoverAp, drFullLoad);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("code", scenario.get("code"));
            row.put("label", scenario.get("label"));
            row.put("targetTps", targetTps);
            row.put("singleCenterRequiredAp", singleCenterAp);
            row.put("apPerCenterNormal", normalPerCenterAp);
            row.put("apPerCenterFailover", failoverAp);
            row.put("totalDeploymentAp", totalAp);
            row.put("judgment", judgment);
            scenarioRows.add(row);
        }

        Map<String, Object> baselineRow = scenarioRows.stream()
                .filter(r -> {
                    Map<String, Object> step3 = mapValue(allPayload.get("step3"));
                    return text(step3.get("operatingBaseline")).equalsIgnoreCase(text(r.get("code")));
                })
                .findFirst()
                .orElse(scenarioRows.isEmpty() ? Map.of() : scenarioRows.get(0));

        enriched.put("centerMode", centerMode);
        enriched.put("centerCount", centerCount);
        enriched.put("trafficSplit", text(step5.get("trafficSplit"), "50:50"));
        enriched.put("drSingleCenterFullLoad", drFullLoad);
        enriched.put("apMarginPerCenter", apMargin);
        enriched.put("minApPerCenter", minApPerCenter);
        enriched.put("vmEffectiveTps", vmEffectiveTps);
        enriched.put("scenarioResults", scenarioRows);
        enriched.put("baselineTotalAp", intValue(baselineRow.get("totalDeploymentAp"), 0));
        enriched.put("baselineApPerCenter", intValue(baselineRow.get("apPerCenterFailover"), minApPerCenter));
        enriched.put("baselineJudgment", text(baselineRow.get("judgment"), "NORMAL"));
        return enriched;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> enrichStep6(Map<String, Object> step6, Map<String, Object> allPayload) {
        Map<String, Object> enriched = new LinkedHashMap<>(step6);
        Map<String, Object> step3 = mapValue(allPayload.get("step3"));
        Map<String, Object> step4 = mapValue(allPayload.get("step4"));
        Map<String, Object> step5 = mapValue(allPayload.get("step5"));
        VmProfile profile = resolveVmProfile(text(step4.get("vmProfileCode"), "16CORE-128GB"));

        String operatingBaseline = text(step3.get("operatingBaseline"), "DESIGN_PEAK");
        String requestedBaseline = text(step6.get("baselineScenarioCode"));
        String baselineCode = operatingBaseline;
        int targetTps = resolveTpsByCode(allPayload, baselineCode);

        if (targetTps <= 0 && StringUtils.hasText(requestedBaseline)
                && !requestedBaseline.equalsIgnoreCase(baselineCode)) {
            baselineCode = requestedBaseline;
            targetTps = resolveTpsByCode(allPayload, baselineCode);
        }
        if (targetTps <= 0) {
            baselineCode = operatingBaseline;
            targetTps = resolveBaselineTps(allPayload);
        }
        if (targetTps <= 0) {
            baselineCode = findPrimaryEnabledScenarioCode(step3);
            targetTps = resolveTpsByCode(allPayload, baselineCode);
        }
        int deploymentAp = intValue(step5.get("baselineTotalAp"), Math.max(2, intValue(step5.get("baselineApPerCenter"), 2)));
        if (deploymentAp <= 0) {
            deploymentAp = 2;
        }

        double avgHold = doubleValue(step6.get("avgThreadHoldSec"), 1.2);
        double threadMargin = doubleValue(step6.get("threadMarginRate"), 1.2);
        double maxMargin = doubleValue(step6.get("maxThreadMarginRate"), 1.3);

        WasThreadResultCDTO was = dcCapacity.calculateWasThreadOnly(
                targetTps, deploymentAp, avgHold, threadMargin, maxMargin, profile);
        JvmSizingRecommendation jvm = JvmSizingGuide.recommend(profile);

        int apTps = (int) Math.ceil((double) targetTps / deploymentAp);
        enriched.put("baselineScenarioCode", baselineCode);
        enriched.put("targetTps", targetTps);
        enriched.put("deploymentAp", deploymentAp);
        enriched.put("apTps", apTps);
        enriched.put("avgThreadHoldSec", avgHold);
        enriched.put("threadMarginRate", threadMargin);
        enriched.put("maxThreadMarginRate", maxMargin);
        enriched.put("totalCalculatedThreads", was.getTotalCalculatedThreads());
        enriched.put("threadsPerVm", was.getThreadsPerVm());
        enriched.put("recommendedMaxThreads", was.getRecommendedMaxThreads());
        enriched.put("minSpareThreads", was.getMinSpareThreads());
        enriched.put("acceptCount", was.getAcceptCount());
        enriched.put("recommendedMaxConnections", profile.getTomcatHikariSpec().maxConnectionsMax());
        enriched.put("wasStatus", was.getStatus());
        enriched.put("wasStatusMessage", was.getStatusMessage());
        enriched.put("jvmXmsGb", intValue(step6.get("jvmXmsGb"), jvm.heapGeneralMinGb()));
        enriched.put("jvmXmxGb", intValue(step6.get("jvmXmxGb"), jvm.heapGeneralMaxGb()));
        enriched.put("jvmGc", text(step6.get("jvmGc"), jvm.gcAlgorithm()));
        enriched.put("jvmMaxGcPauseMs", intValue(step6.get("jvmMaxGcPauseMs"), jvm.maxGcPauseMillis()));
        enriched.put("jvmThreadStack", text(step6.get("jvmThreadStack"), jvm.threadStackSize()));
        enriched.put("jvmStatus", "NORMAL");
        enriched.put("jvmRecommendation", jvm.heapRatioNote());
        return enriched;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> enrichStep7(Map<String, Object> step7, Map<String, Object> allPayload) {
        Map<String, Object> enriched = new LinkedHashMap<>(step7);
        Map<String, Object> step4 = mapValue(allPayload.get("step4"));
        Map<String, Object> step5 = mapValue(allPayload.get("step5"));
        Map<String, Object> step6 = mapValue(allPayload.get("step6"));
        VmProfile profile = resolveVmProfile(text(step4.get("vmProfileCode"), "16CORE-128GB"));

        int targetTps = intValue(step6.get("targetTps"), resolveBaselineTps(allPayload));
        int deploymentAp = intValue(step6.get("deploymentAp"), intValue(step5.get("baselineTotalAp"), 2));
        int threadsPerVm = intValue(step6.get("threadsPerVm"), 200);
        double apTps = deploymentAp > 0 ? (double) targetTps / deploymentAp : targetTps;

        boolean singleView = "SINGLE_VIEW".equalsIgnoreCase(text(step7.get("apType"), "SINGLE_VIEW"));
        double holdSec = doubleValue(step7.get("avgDbConnectionHoldSec"),
                NsightDbPoolDerivation.defaultHoldSec(singleView));
        double dbUsage = doubleValue(step7.get("dbTransactionUsageRatio"), 1.0);
        double poolSafety = doubleValue(step7.get("poolSafetyFactor"), 1.3);
        double threadDbRatio = doubleValue(step7.get("threadDbUsageRatio"), 0.30);
        int minPool = intValue(step7.get("minPoolPerVm"), 30);
        int dbSessionLimit = intValue(step7.get("dbSessionLimit"), 800);
        var spec = profile.getTomcatHikariSpec();
        int profileCap = singleView ? spec.hikariSingleViewMax() : spec.hikariGeneralMax();

        var sizing = NsightDbPoolDerivation.recommend(new NsightDbPoolDerivation.Input(
                apTps, threadsPerVm, holdSec, dbUsage, poolSafety, threadDbRatio, minPool, profileCap));

        int poolPerVm = sizing.recommendedPool();
        long totalSessions = (long) deploymentAp * poolPerVm;
        String status = totalSessions > dbSessionLimit ? "CRITICAL"
                : totalSessions > dbSessionLimit * 0.8 ? "WARN" : "NORMAL";
        String message = sizing.formulaSummary();
        if (sizing.theoreticalPool() > sizing.ceilingPool()) {
            status = "WARN";
            message = "TPS 산출 > Thread 상한 — SQL/점유시간 개선 검토 · " + message;
        }

        enriched.put("apType", singleView ? "SINGLE_VIEW" : "GENERAL");
        enriched.put("apTps", sizing.apTpsRounded());
        enriched.put("deploymentAp", deploymentAp);
        enriched.put("threadsPerVm", threadsPerVm);
        enriched.put("avgDbConnectionHoldSec", holdSec);
        enriched.put("dbTransactionUsageRatio", dbUsage);
        enriched.put("poolSafetyFactor", poolSafety);
        enriched.put("threadDbUsageRatio", threadDbRatio);
        enriched.put("minPoolPerVm", minPool);
        enriched.put("dbSessionLimit", dbSessionLimit);
        enriched.put("poolTheoretical", sizing.theoreticalPool());
        enriched.put("poolCeiling", sizing.ceilingPool());
        enriched.put("poolSized", sizing.sizedPool());
        enriched.put("poolPerVm", poolPerVm);
        enriched.put("totalDbSessions", totalSessions);
        enriched.put("threadPoolRatio", sizing.threadPoolRatio());
        enriched.put("poolFormula", sizing.formulaSummary());
        enriched.put("dbStatus", status);
        enriched.put("dbStatusMessage", message);

        if (boolValue(step7.get("warPoolEnabled"), true)) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> warAllocations = step7.get("warAllocations") instanceof List<?> list
                    ? (List<Map<String, Object>>) list
                    : List.of();
            int minPoolPerWar = intValue(step7.get("minPoolPerWar"), 15);
            Map<String, Object> warPool = CapNewWarPoolAllocation.compute(
                    warAllocations,
                    sizing.apTpsRounded(),
                    threadsPerVm,
                    deploymentAp,
                    dbSessionLimit,
                    holdSec,
                    dbUsage,
                    poolSafety,
                    threadDbRatio,
                    minPoolPerWar,
                    profileCap);
            enriched.put("warPoolEnabled", true);
            enriched.put("minPoolPerWar", minPoolPerWar);
            enriched.put("warAllocations", warAllocations);
            enriched.putAll(warPool);

            String warStatus = text(warPool.get("warPoolStatus"), "NORMAL");
            if ("CRITICAL".equalsIgnoreCase(warStatus)) {
                status = "CRITICAL";
                message = text(warPool.get("warPoolStatusMessage"), message);
            } else if ("WARN".equalsIgnoreCase(warStatus) && !"CRITICAL".equalsIgnoreCase(status)) {
                status = "WARN";
                message = text(warPool.get("warPoolStatusMessage"), message);
            }
            enriched.put("dbStatus", status);
            enriched.put("dbStatusMessage", message);
        } else {
            enriched.put("warPoolEnabled", false);
            enriched.remove("warPoolResults");
            enriched.remove("warPoolTotalSessions");
            enriched.remove("warPoolStatus");
            enriched.remove("warPoolStatusMessage");
            enriched.remove("warPoolExcess");
            enriched.remove("warPoolRecommendations");
        }

        return enriched;
    }

    public Map<String, Object> enrichStep8(Map<String, Object> step8, Map<String, Object> allPayload) {
        Map<String, Object> enriched = new LinkedHashMap<>(step8);
        Map<String, Object> step3 = mapValue(allPayload.get("step3"));
        Map<String, Object> step4 = mapValue(allPayload.get("step4"));
        Map<String, Object> step5 = mapValue(allPayload.get("step5"));
        Map<String, Object> step6 = mapValue(allPayload.get("step6"));
        Map<String, Object> step7 = mapValue(allPayload.get("step7"));

        Map<String, Integer> risk = new LinkedHashMap<>();
        risk.put("normal", 0);
        risk.put("warning", 0);
        risk.put("critical", 0);

        List<Map<String, Object>> apResults = listValue(step5.get("scenarioResults"));
        for (Map<String, Object> row : apResults) {
            bumpRisk(risk, text(row.get("judgment"), "NORMAL"));
        }
        bumpRisk(risk, normalizeStatus(text(step6.get("wasStatus"), "NORMAL")));
        bumpRisk(risk, normalizeStatus(text(step7.get("dbStatus"), "NORMAL")));
        if (boolValue(step7.get("warPoolEnabled"), false)) {
            bumpRisk(risk, normalizeStatus(text(step7.get("warPoolStatus"), "NORMAL")));
        }

        String overall = risk.get("critical") > 0 ? "CRITICAL"
                : risk.get("warning") > 0 ? "WARN" : "NORMAL";

        Map<String, Object> headline = new LinkedHashMap<>();
        headline.put("operatingBaseline", step3.get("operatingBaseline"));
        headline.put("designPeakTps", step4.get("designPeakTps"));
        headline.put("vmProfile", step4.get("vmProfileCode"));
        headline.put("vmAdjustedTps", step4.get("vmAdjustedTps"));
        headline.put("totalDeploymentAp", step5.get("baselineTotalAp"));
        headline.put("maxThreads", step6.get("recommendedMaxThreads"));
        headline.put("poolPerVm", step7.get("poolPerVm"));
        headline.put("totalDbSessions", step7.get("totalDbSessions"));
        if (boolValue(step7.get("warPoolEnabled"), false)) {
            headline.put("warPoolTotalSessions", step7.get("warPoolTotalSessions"));
            headline.put("warPoolStatus", step7.get("warPoolStatus"));
        }

        enriched.put("riskSummary", risk);
        headline.put("overallJudgment", overall);
        enriched.put("headline", headline);
        enriched.put("conclusion", buildConclusion(headline, overall));
        enriched.put("readyForCompare", true);
        return enriched;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> enabledScenarios(Map<String, Object> allPayload) {
        Map<String, Object> step3 = mapValue(allPayload.get("step3"));
        Object raw = step3.get("scenarios");
        if (!(raw instanceof List<?> list)) {
            throw new CapNewBizException("STEP 3 TPS 시나리오가 없습니다. STEP 3을 먼저 저장하세요.");
        }
        List<Map<String, Object>> enabled = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> scenario = (Map<String, Object>) map;
                if (boolValue(scenario.get("enabled"), false)) {
                    enabled.add(scenario);
                }
            }
        }
        if (enabled.isEmpty()) {
            throw new CapNewBizException("활성화된 TPS 시나리오가 없습니다.");
        }
        return enabled;
    }

    private int resolveBaselineTps(Map<String, Object> allPayload) {
        Map<String, Object> step3 = mapValue(allPayload.get("step3"));
        String baseline = text(step3.get("operatingBaseline"), "DESIGN_PEAK");
        return resolveTpsByCode(allPayload, baseline);
    }

    @SuppressWarnings("unchecked")
    private int resolveTpsByCode(Map<String, Object> allPayload, String code) {
        Map<String, Object> step3 = mapValue(allPayload.get("step3"));
        Object raw = step3.get("scenarios");
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Map<String, Object> scenario = (Map<String, Object>) map;
                    if (code.equalsIgnoreCase(text(scenario.get("code"))) && boolValue(scenario.get("enabled"), false)) {
                        return intValue(scenario.get("targetTps"), 0);
                    }
                }
            }
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    private String findPrimaryEnabledScenarioCode(Map<String, Object> step3) {
        String operatingBaseline = text(step3.get("operatingBaseline"));
        if (StringUtils.hasText(operatingBaseline)) {
            return operatingBaseline;
        }
        Object raw = step3.get("scenarios");
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Map<String, Object> scenario = (Map<String, Object>) map;
                    if (boolValue(scenario.get("enabled"), false)) {
                        return text(scenario.get("code"), "DESIGN_PEAK");
                    }
                }
            }
        }
        return "DESIGN_PEAK";
    }

    private String classifyAp(int targetTps, int vmEffectiveTps, int failoverAp, boolean drFullLoad) {
        if (vmEffectiveTps <= 0) {
            return "CRITICAL";
        }
        int capacity = failoverAp * vmEffectiveTps;
        if (!drFullLoad) {
            return targetTps <= capacity ? "NORMAL" : "WARN";
        }
        if (targetTps > capacity) {
            return "CRITICAL";
        }
        if (targetTps > capacity * 0.85) {
            return "WARN";
        }
        return "NORMAL";
    }

    private int centerCount(String centerMode) {
        return switch (centerMode) {
            case "SINGLE" -> 1;
            default -> 2;
        };
    }

    private VmProfile resolveVmProfile(String id) {
        String normalized = VmProfile.normalizeProfileId(id);
        if (normalized != null) {
            for (VmProfile profile : VmProfile.values()) {
                if (profile.getId().equalsIgnoreCase(normalized)) {
                    return profile;
                }
            }
        }
        return VmProfile.CORE16_128;
    }

    private String buildConclusion(Map<String, Object> headline, String overall) {
        return "운영 기준 " + headline.get("operatingBaseline")
                + " · 설계 TPS " + headline.get("designPeakTps")
                + " · VM " + headline.get("vmProfile")
                + " · 배포 AP " + headline.get("totalDeploymentAp") + "대"
                + " · maxThreads " + headline.get("maxThreads")
                + " · Pool/VM " + headline.get("poolPerVm")
                + " · DB Session " + headline.get("totalDbSessions")
                + " → 종합판정 " + overall;
    }

    private void bumpRisk(Map<String, Integer> risk, String status) {
        String key = switch (normalizeStatus(status)) {
            case "WARN", "WARNING" -> "warning";
            case "CRITICAL" -> "critical";
            default -> "normal";
        };
        risk.merge(key, 1, Integer::sum);
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            return "NORMAL";
        }
        return status.toUpperCase();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listValue(Object value) {
        if (value instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    result.add((Map<String, Object>) map);
                }
            }
            return result;
        }
        return List.of();
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String text(Object value, String defaultValue) {
        String t = text(value);
        return StringUtils.hasText(t) ? t : defaultValue;
    }

    private int intValue(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str && StringUtils.hasText(str)) {
            try {
                return Integer.parseInt(str.trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private double doubleValue(Object value, double defaultValue) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String str && StringUtils.hasText(str)) {
            try {
                return Double.parseDouble(str.trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private boolean boolValue(Object value, boolean defaultValue) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String str) {
            return Boolean.parseBoolean(str);
        }
        return defaultValue;
    }
}
