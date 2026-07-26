package com.nh.nsight.marketing.oc.capnew.application.service;

import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewScenarioCDTO;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewVmCompareCDTO;
import com.nh.nsight.marketing.oc.capnew.support.CapNewBizException;
import com.nh.nsight.marketing.oc.capnew.support.CapNewScenarioStatus;
import com.nh.nsight.marketing.oc.support.NsightCapacityDerivation;
import com.nh.nsight.marketing.oc.support.VmProfile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 운영 기준 TPS에 대해 VM Profile 대안(8C/16C/32C 등)을 비교합니다 (설계서 §12).
 */
@Service
public class CapNewVmCompareService {

    private static final List<String> DEFAULT_PROFILES = List.of(
            "8CORE-64GB", "16CORE-128GB", "32CORE-256GB");

    private final CapNewWizardService wizardService;

    public CapNewVmCompareService(CapNewWizardService wizardService) {
        this.wizardService = wizardService;
    }

    public CapNewVmCompareCDTO compare(String scenarioId, List<String> profileCodes) {
        CapNewScenarioCDTO scenario = wizardService.getScenario(scenarioId);
        validateComparable(scenario);

        Map<String, Object> payload = scenario.getStepPayload();
        Map<String, Object> step3 = map(payload.get("step3"));
        Map<String, Object> step4 = map(payload.get("step4"));
        Map<String, Object> step5 = map(payload.get("step5"));

        String baselineCode = text(step3.get("operatingBaseline"), "DESIGN_PEAK");
        int baselineTps = resolveBaselineTps(payload, baselineCode);
        if (baselineTps <= 0) {
            throw new CapNewBizException("운영 기준 TPS가 없습니다. STEP 3·4를 저장하세요.");
        }

        String currentVm = text(step4.get("vmProfileCode"), "16CORE-128GB");
        List<String> targets = resolveProfileCodes(profileCodes, currentVm);

        String centerMode = text(step5.get("centerMode"), "ACTIVE_ACTIVE");
        int apMargin = intVal(step5.get("apMarginPerCenter"), 1);
        int minApPerCenter = intVal(step5.get("minApPerCenter"), 2);
        boolean drFullLoad = boolVal(step5.get("drSingleCenterFullLoad"), true);
        int centerCount = centerCount(centerMode);

        List<CapNewVmCompareCDTO.VmAlternativeRow> rows = new ArrayList<>();
        for (String code : targets) {
            VmProfile profile = resolveVmProfile(code);
            int vmAdjustedTps = computeVmAdjustedTps(profile, step4);
            ApSizing ap = computeApSizing(
                    baselineTps, vmAdjustedTps, centerMode, apMargin, minApPerCenter, drFullLoad, centerCount);

            CapNewVmCompareCDTO.VmAlternativeRow row = new CapNewVmCompareCDTO.VmAlternativeRow();
            row.setVmProfileCode(profile.getId());
            row.setVmProfileLabel(formatVmLabel(profile));
            row.setVmCores(profile.getCores());
            row.setVmMemoryGb(profile.getMemoryGb());
            row.setVmTheoreticalTps(computeVmTheoreticalTps(profile, step4));
            row.setVmAdjustedTps(vmAdjustedTps);
            row.setApPerCenterNormal(ap.normalPerCenterAp);
            row.setApPerCenterFailover(ap.failoverAp);
            row.setRequiredApDisplay(formatApDisplay(centerMode, centerCount, ap));
            row.setTotalDeploymentAp(ap.totalAp);
            row.setTotalCores(ap.totalAp * profile.getCores());
            row.setFailureBlastRadius(blastRadius(profile.getCores()));
            row.setFailureBlastLabel(blastLabel(row.getFailureBlastRadius()));
            row.setJudgment(vmJudgment(profile.getCores(), ap.failoverAp, ap.apJudgment));
            row.setApJudgment(ap.apJudgment);
            row.setSelected(profile.getId().equalsIgnoreCase(currentVm)
                    || normalizeProfileId(currentVm).equalsIgnoreCase(profile.getId()));
            rows.add(row);
        }

        rows.sort(Comparator.comparingInt(CapNewVmCompareCDTO.VmAlternativeRow::getVmCores));

        CapNewVmCompareCDTO result = new CapNewVmCompareCDTO();
        result.setScenarioId(scenario.getScenarioId());
        result.setScenarioName(scenario.getScenarioName());
        result.setBaselineCode(baselineCode);
        result.setBaselineLabel(resolveBaselineLabel(payload, baselineCode));
        result.setBaselineTps(baselineTps);
        result.setCurrentVmProfileCode(currentVm);
        result.setAlternatives(rows);
        result.setRecommendation(buildRecommendation(rows, centerMode));
        result.setSummary(buildSummary(result));
        return result;
    }

    private void validateComparable(CapNewScenarioCDTO scenario) {
        String status = scenario.getStatus();
        if (!CapNewScenarioStatus.COMPLETED.name().equals(status)
                && !CapNewScenarioStatus.APPROVED.name().equals(status)
                && !CapNewScenarioStatus.DRAFT.name().equals(status)) {
            throw new CapNewBizException("시나리오를 조회할 수 없습니다.");
        }
        Map<String, Object> payload = scenario.getStepPayload();
        if (payload == null || !payload.containsKey("step4")) {
            throw new CapNewBizException("STEP 4 VM 조건이 없습니다. Wizard STEP 4를 먼저 저장하세요.");
        }
    }

    private List<String> resolveProfileCodes(List<String> requested, String currentVm) {
        Set<String> codes = new LinkedHashSet<>();
        if (requested != null) {
            for (String code : requested) {
                if (StringUtils.hasText(code)) {
                    codes.add(code.trim());
                }
            }
        }
        if (codes.isEmpty()) {
            codes.addAll(DEFAULT_PROFILES);
        }
        if (StringUtils.hasText(currentVm)) {
            codes.add(currentVm.trim());
        }
        return new ArrayList<>(codes);
    }

    private int computeVmTheoreticalTps(VmProfile profile, Map<String, Object> step4) {
        return profile.getCores() * resolveTpsPerCore(step4);
    }

    private int computeVmAdjustedTps(VmProfile profile, Map<String, Object> step4) {
        int theoretical = computeVmTheoreticalTps(profile, step4);
        if (!boolVal(step4.get("applyCorrectionFactors"), true)) {
            return Math.max(1, theoretical);
        }
        double cpuTarget = doubleVal(step4.get("cpuTargetUtilization"), 0.70);
        double perfSafety = doubleVal(step4.get("perfSafetyFactor"), 1.20);
        double virtFactor = doubleVal(step4.get("virtualizationFactor"), 0.90);
        double opsFactor = doubleVal(step4.get("opsEfficiencyFactor"), 0.85);
        int adjusted = (int) Math.floor(theoretical * cpuTarget * virtFactor * opsFactor / perfSafety);
        return Math.max(1, adjusted);
    }

    private int resolveTpsPerCore(Map<String, Object> step4) {
        int tpsPerCore = intVal(step4.get("tpsPerCore"), 0);
        if (tpsPerCore > 0) {
            return tpsPerCore;
        }
        return NsightCapacityDerivation.coreTpsFromTpmc(intVal(step4.get("tpmcPerTps"), 3000)).tpsPerCoreBase();
    }

    private ApSizing computeApSizing(
            int targetTps,
            int vmEffectiveTps,
            String centerMode,
            int apMargin,
            int minApPerCenter,
            boolean drFullLoad,
            int centerCount) {
        int singleCenterAp = vmEffectiveTps > 0
                ? (int) Math.ceil((double) targetTps / vmEffectiveTps)
                : 0;
        int normalPerCenterAp = "ACTIVE_ACTIVE".equals(centerMode)
                ? (int) Math.ceil((double) targetTps / 2.0 / Math.max(1, vmEffectiveTps)) + apMargin
                : singleCenterAp + apMargin;
        normalPerCenterAp = Math.max(minApPerCenter, normalPerCenterAp);
        int failoverAp = drFullLoad
                ? Math.max(minApPerCenter, singleCenterAp + apMargin)
                : normalPerCenterAp;
        int totalAp = normalPerCenterAp * centerCount;
        String apJudgment = classifyAp(targetTps, vmEffectiveTps, failoverAp, drFullLoad);
        return new ApSizing(normalPerCenterAp, failoverAp, totalAp, apJudgment);
    }

    private String formatApDisplay(String centerMode, int centerCount, ApSizing ap) {
        if ("ACTIVE_ACTIVE".equals(centerMode) && centerCount >= 2) {
            return ap.normalPerCenterAp + "+" + ap.normalPerCenterAp;
        }
        if (centerCount >= 2) {
            return ap.normalPerCenterAp + "+" + ap.failoverAp;
        }
        return String.valueOf(ap.normalPerCenterAp);
    }

    private String blastRadius(int cores) {
        if (cores <= 8) {
            return "SMALL";
        }
        if (cores >= 32) {
            return "LARGE";
        }
        return "MEDIUM";
    }

    private String blastLabel(String radius) {
        return switch (radius) {
            case "SMALL" -> "작음";
            case "LARGE" -> "큼";
            default -> "중간";
        };
    }

    private String vmJudgment(int cores, int apPerCenter, String apJudgment) {
        if ("CRITICAL".equalsIgnoreCase(apJudgment)) {
            return "검토 필요";
        }
        if (cores <= 8) {
            return "확장성 우수";
        }
        if (cores >= 32) {
            return "집중 위험";
        }
        return "운영 권장";
    }

    private String buildRecommendation(List<CapNewVmCompareCDTO.VmAlternativeRow> rows, String centerMode) {
        CapNewVmCompareCDTO.VmAlternativeRow preferred = rows.stream()
                .filter(r -> "운영 권장".equals(r.getJudgment()))
                .filter(r -> "NORMAL".equalsIgnoreCase(r.getApJudgment()))
                .findFirst()
                .orElseGet(() -> rows.stream()
                        .filter(r -> "NORMAL".equalsIgnoreCase(r.getApJudgment()))
                        .findFirst()
                        .orElse(rows.isEmpty() ? null : rows.get(0)));

        if (preferred == null) {
            return "VM 대안 비교 결과가 없습니다.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("● ").append(preferred.getVmProfileLabel())
                .append(" × 센터당 ").append(preferred.getApPerCenterFailover()).append("대");
        if ("ACTIVE_ACTIVE".equals(centerMode)) {
            sb.append(" (전체 ").append(preferred.getTotalDeploymentAp()).append("대)");
        }
        sb.append(" — ").append(preferred.getJudgment());
        if ("운영 권장".equals(preferred.getJudgment())) {
            sb.append(" · 장애 시 잔여 센터 처리·Scale-Out 균형");
        }
        return sb.toString();
    }

    private String buildSummary(CapNewVmCompareCDTO result) {
        return "운영 기준 [" + result.getBaselineLabel() + "] "
                + result.getBaselineTps() + " TPS · VM 대안 "
                + result.getAlternatives().size() + "건 비교";
    }

    @SuppressWarnings("unchecked")
    private int resolveBaselineTps(Map<String, Object> payload, String baselineCode) {
        Map<String, Object> step4 = map(payload.get("step4"));
        int fromStep4 = intVal(step4.get("designPeakTps"), 0);
        Map<String, Object> step3 = map(payload.get("step3"));
        Object raw = step3.get("scenarios");
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> m)) {
                    continue;
                }
                Map<String, Object> scenario = (Map<String, Object>) m;
                if (baselineCode.equalsIgnoreCase(text(scenario.get("code"), ""))
                        && boolVal(scenario.get("enabled"), false)) {
                    return intVal(scenario.get("targetTps"), fromStep4);
                }
            }
        }
        return fromStep4;
    }

    @SuppressWarnings("unchecked")
    private String resolveBaselineLabel(Map<String, Object> payload, String baselineCode) {
        Map<String, Object> step3 = map(payload.get("step3"));
        Object raw = step3.get("scenarios");
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> m)) {
                    continue;
                }
                Map<String, Object> scenario = (Map<String, Object>) m;
                if (baselineCode.equalsIgnoreCase(text(scenario.get("code"), ""))) {
                    return text(scenario.get("label"), baselineCode);
                }
            }
        }
        return baselineCode;
    }

    private String formatVmLabel(VmProfile profile) {
        return profile.getCores() + "C / " + profile.getMemoryGb() + "GB";
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
        return "SINGLE".equals(centerMode) ? 1 : 2;
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

    private String normalizeProfileId(String id) {
        String normalized = VmProfile.normalizeProfileId(id);
        return normalized != null ? normalized : id;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private String text(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? fallback : s;
    }

    private int intVal(Object value, int fallback) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private double doubleVal(Object value, double fallback) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private boolean boolVal(Object value, boolean fallback) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value == null) {
            return fallback;
        }
        String s = String.valueOf(value).trim().toLowerCase();
        if ("true".equals(s) || "1".equals(s)) {
            return true;
        }
        if ("false".equals(s) || "0".equals(s)) {
            return false;
        }
        return fallback;
    }

    private record ApSizing(int normalPerCenterAp, int failoverAp, int totalAp, String apJudgment) {
    }
}
