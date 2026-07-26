package com.nh.nsight.marketing.oc.capnew.application.rule;

import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewStepValidationCDTO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CapNewStepRule {

    public CapNewStepValidationCDTO validateStep1(Map<String, Object> step1) {
        CapNewStepValidationCDTO result = new CapNewStepValidationCDTO();
        result.setValid(true);

        String projectId = text(step1.get("projectId"));
        if (!StringUtils.hasText(projectId)) {
            result.addError("프로젝트 ID는 필수입니다.");
        } else if (!projectId.matches("^[A-Za-z0-9-]+$")) {
            result.addError("프로젝트 ID는 영문·숫자·하이픈만 허용합니다.");
        }

        requireText(step1.get("projectName"), "프로젝트명", result);
        requireText(step1.get("scenarioName"), "시나리오명", result);
        requireText(step1.get("targetEnv"), "대상 환경", result);
        requireText(step1.get("purpose"), "산정 목적", result);
        return result;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> enrichStep2(Map<String, Object> step2) {
        Map<String, Object> enriched = new LinkedHashMap<>(step2);
        String calcMode = text(step2.get("calcMode"));
        if (!StringUtils.hasText(calcMode)) {
            calcMode = "BRANCH";
        }
        double marginRate = doubleValue(step2.get("sessionMarginRate"), 0.30);

        int totalUsers;
        if ("DIRECT".equalsIgnoreCase(calcMode)) {
            totalUsers = intValue(step2.get("totalUsersDirect"), intValue(step2.get("totalUsers"), 0));
            enriched.put("branchCount", intValue(step2.get("branchCount"), 0));
            enriched.put("userPerBranch", intValue(step2.get("userPerBranch"), 0));
            enriched.put("hqUsers", intValue(step2.get("hqUsers"), 0));
            enriched.put("otherUsers", intValue(step2.get("otherUsers"), 0));
        } else {
            int branchCount = intValue(step2.get("branchCount"), 6000);
            int userPerBranch = intValue(step2.get("userPerBranch"), 6);
            int hqUsers = intValue(step2.get("hqUsers"), 0);
            int otherUsers = intValue(step2.get("otherUsers"), 0);
            totalUsers = branchCount * userPerBranch + hqUsers + otherUsers;
            enriched.put("branchCount", branchCount);
            enriched.put("userPerBranch", userPerBranch);
            enriched.put("hqUsers", hqUsers);
            enriched.put("otherUsers", otherUsers);
            calcMode = "BRANCH";
        }

        int designedSessions = (int) Math.ceil(totalUsers * (1.0 + marginRate));
        int sessionMarginCount = designedSessions - totalUsers;
        int sessionTimeoutMin = intValue(step2.get("sessionTimeoutMin"), 60);

        enriched.put("calcMode", calcMode);
        enriched.put("sessionMarginRate", marginRate);
        enriched.put("sessionTimeoutMin", sessionTimeoutMin);
        enriched.put("totalUsers", totalUsers);
        enriched.put("designedSessions", designedSessions);
        enriched.put("sessionMarginCount", sessionMarginCount);
        if ("DIRECT".equalsIgnoreCase(calcMode)) {
            enriched.put("totalUsersDirect", totalUsers);
        }
        return enriched;
    }

    public CapNewStepValidationCDTO validateStep2(Map<String, Object> step2) {
        CapNewStepValidationCDTO result = new CapNewStepValidationCDTO();
        result.setValid(true);

        String calcMode = text(step2.get("calcMode"));
        double marginRate = doubleValue(step2.get("sessionMarginRate"), -1);
        int sessionTimeoutMin = intValue(step2.get("sessionTimeoutMin"), 0);

        if ("DIRECT".equalsIgnoreCase(calcMode)) {
            int totalUsers = intValue(step2.get("totalUsersDirect"), intValue(step2.get("totalUsers"), 0));
            if (totalUsers < 1) {
                result.addError("전체 사용자 수는 1 이상이어야 합니다.");
            }
        } else {
            int branchCount = intValue(step2.get("branchCount"), 0);
            int userPerBranch = intValue(step2.get("userPerBranch"), 0);
            if (branchCount < 1) {
                result.addError("지점 수는 1 이상이어야 합니다.");
            }
            if (userPerBranch < 1) {
                result.addError("지점당 사용자 수는 1 이상이어야 합니다.");
            }
            if (intValue(step2.get("hqUsers"), 0) < 0 || intValue(step2.get("otherUsers"), 0) < 0) {
                result.addError("본부·기타 사용자는 0 이상이어야 합니다.");
            }
        }
        if (marginRate < 0 || marginRate > 1.0) {
            result.addError("세션 여유율은 0~100% 범위여야 합니다.");
        }
        if (sessionTimeoutMin < 1) {
            result.addError("Session Timeout은 1분 이상이어야 합니다.");
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> enrichStep3(Map<String, Object> step3, int totalUsers) {
        Map<String, Object> enriched = new LinkedHashMap<>(step3);
        List<Map<String, Object>> scenarios = step3.get("scenarios") instanceof List<?> list
                ? (List<Map<String, Object>>) list
                : defaultTpsScenarios(totalUsers);

        List<Map<String, Object>> computed = new ArrayList<>();
        for (Map<String, Object> scenario : scenarios) {
            Map<String, Object> row = new LinkedHashMap<>(scenario);
            boolean enabled = boolValue(scenario.get("enabled"), true);
            double rate = percentToRatio(doubleValue(scenario.get("concurrentRate"), 0.05));
            int responseSec = intValue(scenario.get("responseSec"), 3);
            int concurrentUsers = (int) Math.round(totalUsers * rate);
            int targetTps = responseSec > 0 ? (int) Math.ceil((double) concurrentUsers / responseSec) : 0;

            row.put("enabled", enabled);
            row.put("concurrentRate", rate);
            row.put("responseSec", responseSec);
            row.put("concurrentUsers", concurrentUsers);
            row.put("targetTps", targetTps);
            computed.add(row);
        }

        enriched.put("scenarios", computed);
        if (!StringUtils.hasText(text(step3.get("operatingBaseline")))) {
            enriched.put("operatingBaseline", "DESIGN_PEAK");
        }
        if (!(step3.get("performanceTestTargets") instanceof List<?>)) {
            List<String> defaults = new ArrayList<>();
            for (Map<String, Object> scenario : computed) {
                if (boolValue(scenario.get("enabled"), false)) {
                    String code = text(scenario.get("code"));
                    if (!"SLOW_RESPONSE".equalsIgnoreCase(code) && !code.startsWith("CUSTOM")) {
                        defaults.add(code);
                    }
                }
            }
            enriched.put("performanceTestTargets", defaults);
        }
        return enriched;
    }

    @SuppressWarnings("unchecked")
    public CapNewStepValidationCDTO validateStep3(Map<String, Object> step3) {
        CapNewStepValidationCDTO result = new CapNewStepValidationCDTO();
        result.setValid(true);

        List<Map<String, Object>> scenarios = step3.get("scenarios") instanceof List<?> list
                ? (List<Map<String, Object>>) list
                : List.of();

        long enabledCount = scenarios.stream()
                .filter(s -> boolValue(s.get("enabled"), false))
                .count();
        if (enabledCount < 1) {
            result.addError("최소 하나의 TPS 시나리오를 선택해야 합니다.");
        }

        String baseline = text(step3.get("operatingBaseline"));
        if (!StringUtils.hasText(baseline)) {
            result.addError("운영 기준 시나리오를 지정해야 합니다.");
        }

        int baselineTps = findTpsByCode(scenarios, baseline);
        int stressTps = findTpsByCode(scenarios, "STRESS");
        if (baselineTps > 0 && stressTps > 0 && stressTps < baselineTps) {
            result.addError("스트레스 TPS는 운영 기준 TPS보다 낮을 수 없습니다.");
        }

        for (Map<String, Object> scenario : scenarios) {
            if (!boolValue(scenario.get("enabled"), false)) {
                continue;
            }
            double rate = percentToRatio(doubleValue(scenario.get("concurrentRate"), 0));
            if (rate > 1.0) {
                result.addWarning(text(scenario.get("code")) + " 시나리오 동시요청률이 100%를 초과합니다.");
            }
            if (intValue(scenario.get("responseSec"), 0) <= 0) {
                result.addError("응답시간은 0보다 커야 합니다.");
            }
        }
        return result;
    }

    public CapNewStepValidationCDTO validateStep4(Map<String, Object> step4) {
        CapNewStepValidationCDTO result = new CapNewStepValidationCDTO();
        result.setValid(true);
        if (!StringUtils.hasText(text(step4.get("businessTypeCode")))) {
            result.addError("업무 유형을 선택하세요.");
        }
        if (!StringUtils.hasText(text(step4.get("vmProfileCode")))) {
            result.addError("VM Profile을 선택하세요.");
        }
        if (intValue(step4.get("tpsPerCore"), 0) <= 0 && intValue(step4.get("tpmcPerTps"), 0) <= 0) {
            result.addError("TPMC/TPS 또는 Core당 TPS가 필요합니다.");
        }
        if (intValue(step4.get("vmAdjustedTps"), 0) <= 0) {
            result.addError("VM 보정 TPS가 0 이하입니다. 보정계수를 확인하세요.");
        }
        return result;
    }

    public CapNewStepValidationCDTO validateStep5(Map<String, Object> step5) {
        CapNewStepValidationCDTO result = new CapNewStepValidationCDTO();
        result.setValid(true);
        if (!StringUtils.hasText(text(step5.get("centerMode")))) {
            result.addError("센터 구성을 선택하세요.");
        }
        if (intValue(step5.get("minApPerCenter"), 0) < 1) {
            result.addError("센터당 최소 AP는 1 이상이어야 합니다.");
        }
        List<?> rows = step5.get("scenarioResults") instanceof List<?> list ? list : List.of();
        if (rows.isEmpty()) {
            result.addError("AP 산정 결과가 없습니다. STEP 3·4를 먼저 저장하세요.");
        }
        for (Object row : rows) {
            if (row instanceof Map<?, ?> map && "CRITICAL".equalsIgnoreCase(text(map.get("judgment")))) {
                result.addWarning(text(map.get("label")) + ": DR/용량 위험 — AP 증설 검토");
            }
        }
        return result;
    }

    public CapNewStepValidationCDTO validateStep6(Map<String, Object> step6) {
        CapNewStepValidationCDTO result = new CapNewStepValidationCDTO();
        result.setValid(true);
        if (intValue(step6.get("targetTps"), 0) <= 0) {
            result.addError("기준 TPS가 없습니다.");
        }
        if (intValue(step6.get("deploymentAp"), 0) <= 0) {
            result.addError("배포 AP 대수가 없습니다. STEP 5를 먼저 저장하세요.");
        }
        String wasStatus = text(step6.get("wasStatus"));
        if ("CRITICAL".equalsIgnoreCase(wasStatus)) {
            result.addWarning("WAS Thread 위험: " + text(step6.get("wasStatusMessage")));
        } else if ("WARN".equalsIgnoreCase(wasStatus)) {
            result.addWarning("WAS Thread 주의: " + text(step6.get("wasStatusMessage")));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public CapNewStepValidationCDTO validateStep7(Map<String, Object> step7) {
        CapNewStepValidationCDTO result = new CapNewStepValidationCDTO();
        result.setValid(true);
        if (intValue(step7.get("poolPerVm"), 0) <= 0) {
            result.addError("DB Pool 산정 결과가 없습니다.");
        }
        if (boolValue(step7.get("warPoolEnabled"), false)) {
            List<Map<String, Object>> wars = step7.get("warAllocations") instanceof List<?> list
                    ? (List<Map<String, Object>>) list
                    : List.of();
            double weightSum = 0;
            int enabledCount = 0;
            for (Object item : wars) {
                if (!(item instanceof Map<?, ?> map)) {
                    continue;
                }
                if (!boolValue(map.get("enabled"), true)) {
                    continue;
                }
                enabledCount++;
                weightSum += doubleValue(map.get("weightPercent"), 0);
            }
            if (enabledCount < 1) {
                result.addError("WAR Pool 배분: 활성 WAR를 1개 이상 지정하세요.");
            } else if (Math.abs(weightSum - 100.0) > 1.0) {
                result.addWarning(String.format(
                        "WAR 비중 합계가 %.1f%%입니다. 100%%에 맞추면 배분이 정확해집니다.", weightSum));
            }
            String warStatus = text(step7.get("warPoolStatus"));
            if ("CRITICAL".equalsIgnoreCase(warStatus)) {
                result.addWarning("WAR Pool 합계 위험: " + text(step7.get("warPoolStatusMessage")));
            } else if ("WARN".equalsIgnoreCase(warStatus)) {
                result.addWarning("WAR Pool 합계 주의: " + text(step7.get("warPoolStatusMessage")));
            }
        }
        String dbStatus = text(step7.get("dbStatus"));
        if ("CRITICAL".equalsIgnoreCase(dbStatus)) {
            result.addWarning("DB Session 위험: " + text(step7.get("dbStatusMessage")));
        } else if ("WARN".equalsIgnoreCase(dbStatus)) {
            result.addWarning("DB Pool 주의: " + text(step7.get("dbStatusMessage")));
        }
        return result;
    }

    public CapNewStepValidationCDTO validateStep8(Map<String, Object> step8) {
        CapNewStepValidationCDTO result = new CapNewStepValidationCDTO();
        result.setValid(true);
        if (!StringUtils.hasText(text(step8.get("conclusion")))) {
            result.addError("종합 결론이 생성되지 않았습니다.");
        }
        return result;
    }

    public List<Map<String, Object>> defaultTpsScenarios(int totalUsers) {
        List<Map<String, Object>> presets = new ArrayList<>();
        presets.add(preset("NORMAL", "평시", 0.03, 3, true));
        presets.add(preset("PEAK", "정상 피크", 0.05, 3, true));
        presets.add(preset("DESIGN_PEAK", "설계 피크", 0.10, 3, true));
        presets.add(preset("STRESS", "스트레스", 0.15, 3, true));
        presets.add(preset("SLOW_RESPONSE", "응답지연", 0.10, 5, false));
        presets.add(preset("DR_FAULT", "DR 장애", 0.10, 3, false));
        return enrichStep3(Map.of("scenarios", presets), totalUsers).get("scenarios") instanceof List<?> list
                ? (List<Map<String, Object>>) list
                : presets;
    }

    private Map<String, Object> preset(String code, String label, double rate, int responseSec, boolean enabled) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("code", code);
        row.put("label", label);
        row.put("concurrentRate", rate);
        row.put("responseSec", responseSec);
        row.put("enabled", enabled);
        return row;
    }

    private int findTpsByCode(List<Map<String, Object>> scenarios, String code) {
        if (!StringUtils.hasText(code)) {
            return 0;
        }
        for (Map<String, Object> scenario : scenarios) {
            if (code.equalsIgnoreCase(text(scenario.get("code"))) && boolValue(scenario.get("enabled"), false)) {
                return intValue(scenario.get("targetTps"), 0);
            }
        }
        return 0;
    }

    private void requireText(Object value, String label, CapNewStepValidationCDTO result) {
        if (!StringUtils.hasText(text(value))) {
            result.addError(label + "은(는) 필수입니다.");
        }
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
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
            String trimmed = str.trim().replace("%", "");
            try {
                double parsed = Double.parseDouble(trimmed);
                if (str.contains("%") || parsed > 1.0) {
                    return parsed / 100.0;
                }
                return parsed;
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private double percentToRatio(double value) {
        return value > 1.0 ? value / 100.0 : value;
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
