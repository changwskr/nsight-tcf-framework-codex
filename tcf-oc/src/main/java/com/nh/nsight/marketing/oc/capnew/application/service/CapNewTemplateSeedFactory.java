package com.nh.nsight.marketing.oc.capnew.application.service;

import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewDefaultsCDTO;
import com.nh.nsight.marketing.oc.capnew.application.rule.CapNewStepRule;
import com.nh.nsight.marketing.oc.capnew.support.CapNewBizException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CapNewTemplateSeedFactory {

    private final CapNewStepRule stepRule;

    public CapNewTemplateSeedFactory(CapNewStepRule stepRule) {
        this.stepRule = stepRule;
    }

    public List<TemplateCatalog> catalogs() {
        return List.of(
                catalog("PROD_STANDARD", "표준 운영 (16C)",
                        "6,000지점·설계 피크 10%·16C/128G·Active-Active 운영 기준안",
                        "NEW_BUILD", "PROD", "16CORE-128GB",
                        36000, 1200, 8, 192, 45, 10, this::prodStandard),
                catalog("PEAK_OPS", "정상 피크 운영",
                        "평시·정상 피크 중심 TPS, 스트레스 비활성·운영 피크 기준",
                        "NEW_BUILD", "PROD", "16CORE-128GB",
                        36000, 600, 4, 120, 35, 20, this::peakOps),
                catalog("PERF_STRESS", "성능시험 (스트레스)",
                        "스트레스 15%·성능시험 대상 다중·한계 검증용",
                        "CONFIG_CHECK", "STG", "16CORE-128GB",
                        36000, 1800, 12, 260, 55, 30, this::perfStress),
                catalog("DR_FAULT", "DR 장애 수용",
                        "DR 단일 센터 전부하·DR_FAULT 시나리오·페일오버 AP",
                        "DR_VALIDATION", "DR", "16CORE-128GB",
                        36000, 1200, 10, 200, 48, 40, this::drFault),
                catalog("SCALE_8C", "증설 검토 (8C)",
                        "8C/64G VM·Scale-Out 증설 대안·AP·Core 비교",
                        "SCALE_OUT", "PROD", "8CORE-64GB",
                        36000, 1200, 15, 220, 40, 50, this::scale8c),
                catalog("STG_SMALL", "스테이징 소형",
                        "1,000지점 축소·4C/32G·개발·스테이징 검증",
                        "CONFIG_CHECK", "STG", "4CORE-32GB",
                        6000, 100, 2, 40, 20, 60, this::stgSmall)
        );
    }

    public Map<String, Object> buildSeed(String code, CapNewDefaultsCDTO defaults) {
        TemplateCatalog catalog = findCatalog(code);
        return catalog.seedBuilder().apply(defaults);
    }

    private TemplateCatalog findCatalog(String code) {
        if (!StringUtils.hasText(code)) {
            throw new CapNewBizException("템플릿 코드가 필요합니다.");
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        for (TemplateCatalog catalog : catalogs()) {
            if (catalog.code().equalsIgnoreCase(normalized)) {
                return catalog;
            }
        }
        throw new CapNewBizException("알 수 없는 시나리오 템플릿입니다: " + code);
    }

    private static TemplateCatalog catalog(
            String code,
            String label,
            String description,
            String purpose,
            String targetEnv,
            String vmProfileCode,
            int totalUsers,
            int designPeakTps,
            int deploymentAp,
            int maxThreads,
            int poolPerVm,
            int sortOrder,
            Function<CapNewDefaultsCDTO, Map<String, Object>> seedBuilder) {
        return new TemplateCatalog(
                code, label, description, purpose, targetEnv,
                vmProfileCode, totalUsers, designPeakTps, deploymentAp, maxThreads, poolPerVm,
                sortOrder, seedBuilder);
    }

    private Map<String, Object> prodStandard(CapNewDefaultsCDTO defaults) {
        Map<String, Object> seed = baseSeed(defaults);
        putStep1(seed, "표준 운영 용량 기준안", "NEW_BUILD", "PROD",
                "6,000지점 표준 운영·16C VM·설계 피크 10% 기준");
        putStep3Standard(seed, "DESIGN_PEAK", true, true, true, true, false);
        return seed;
    }

    private Map<String, Object> peakOps(CapNewDefaultsCDTO defaults) {
        Map<String, Object> seed = baseSeed(defaults);
        putStep1(seed, "정상 피크 운영 기준안", "NEW_BUILD", "PROD",
                "평시·정상 피크 중심 운영 TPS 산정");
        putStep3Standard(seed, "PEAK", true, true, false, false, false);
        Map<String, Object> step3 = map(seed.get("step3"));
        disableScenario(step3, "STRESS");
        disableScenario(step3, "DR_FAULT");
        disableScenario(step3, "SLOW_RESPONSE");
        seed.put("step3", step3);
        syncStep6Baseline(seed);
        return seed;
    }

    private Map<String, Object> perfStress(CapNewDefaultsCDTO defaults) {
        Map<String, Object> seed = baseSeed(defaults);
        putStep1(seed, "성능시험 스트레스 기준안", "CONFIG_CHECK", "STG",
                "스트레스 15%·성능시험 시나리오 다중 선택");
        putStep3Standard(seed, "STRESS", true, true, true, true, true);
        Map<String, Object> step3 = map(seed.get("step3"));
        step3.put("performanceTestTargets", List.of("NORMAL", "PEAK", "DESIGN_PEAK", "STRESS"));
        enableScenario(step3, "STRESS");
        seed.put("step3", step3);
        Map<String, Object> step6 = map(seed.get("step6"));
        step6.put("threadMarginRate", 1.3);
        step6.put("maxThreadMarginRate", 1.4);
        seed.put("step6", step6);
        syncStep6Baseline(seed);
        return seed;
    }

    private Map<String, Object> drFault(CapNewDefaultsCDTO defaults) {
        Map<String, Object> seed = baseSeed(defaults);
        putStep1(seed, "DR 장애 수용 기준안", "DR_VALIDATION", "DR",
                "한 센터 장애 시 DR_FAULT 시나리오·단일 센터 전부하");
        putStep3Standard(seed, "DR_FAULT", true, true, true, false, true);
        Map<String, Object> step3 = map(seed.get("step3"));
        enableScenario(step3, "DR_FAULT");
        step3.put("operatingBaseline", "DR_FAULT");
        step3.put("performanceTestTargets", List.of("DESIGN_PEAK", "DR_FAULT"));
        seed.put("step3", step3);
        Map<String, Object> step5 = map(seed.get("step5"));
        step5.put("drSingleCenterFullLoad", true);
        step5.put("apMarginPerCenter", 2);
        seed.put("step5", step5);
        syncStep6Baseline(seed);
        return seed;
    }

    private Map<String, Object> scale8c(CapNewDefaultsCDTO defaults) {
        Map<String, Object> seed = baseSeed(defaults);
        putStep1(seed, "8C 증설 검토 기준안", "SCALE_OUT", "PROD",
                "16C 대비 8C VM Scale-Out 증설 대안");
        putStep3Standard(seed, "DESIGN_PEAK", true, true, true, true, false);
        Map<String, Object> step4 = map(seed.get("step4"));
        step4.put("vmProfileCode", "8CORE-64GB");
        seed.put("step4", step4);
        Map<String, Object> step5 = map(seed.get("step5"));
        step5.put("apMarginPerCenter", 2);
        seed.put("step5", step5);
        return seed;
    }

    private Map<String, Object> stgSmall(CapNewDefaultsCDTO defaults) {
        Map<String, Object> seed = baseSeed(defaults);
        putStep1(seed, "스테이징 소형 기준안", "CONFIG_CHECK", "STG",
                "축소 지점·4C VM·스테이징 검증");
        Map<String, Object> step2 = map(seed.get("step2"));
        step2.put("branchCount", 1000);
        step2.put("userPerBranch", 6);
        step2.put("sessionMarginRate", 0.20);
        step2.put("sessionTimeoutMin", 60);
        seed.put("step2", step2);
        enrichStep2(seed);
        putStep3Standard(seed, "PEAK", true, true, false, false, false);
        Map<String, Object> step3 = map(seed.get("step3"));
        disableScenario(step3, "STRESS");
        disableScenario(step3, "DR_FAULT");
        seed.put("step3", step3);
        Map<String, Object> step4 = map(seed.get("step4"));
        step4.put("vmProfileCode", "4CORE-32GB");
        step4.put("applyCorrectionFactors", false);
        seed.put("step4", step4);
        Map<String, Object> step5 = map(seed.get("step5"));
        step5.put("centerMode", "SINGLE");
        step5.put("minApPerCenter", 1);
        step5.put("apMarginPerCenter", 0);
        seed.put("step5", step5);
        Map<String, Object> step7 = map(seed.get("step7"));
        step7.put("dbSessionLimit", 200);
        step7.put("minPoolPerVm", 20);
        seed.put("step7", step7);
        syncStep6Baseline(seed);
        return seed;
    }

    private Map<String, Object> baseSeed(CapNewDefaultsCDTO defaults) {
        Map<String, Object> seed = new LinkedHashMap<>();
        seed.put("step1", copyMap(defaults.getStep1()));
        seed.put("step2", copyMap(defaults.getStep2()));
        Map<String, Object> step3 = new LinkedHashMap<>();
        step3.put("scenarios", copyList(defaults.getTpsPresets()));
        step3.put("operatingBaseline", "DESIGN_PEAK");
        seed.put("step3", step3);
        seed.put("step4", copyMap(defaults.getStep4()));
        seed.put("step5", copyMap(defaults.getStep5()));
        seed.put("step6", copyMap(defaults.getStep6()));
        seed.put("step7", copyMap(defaults.getStep7()));
        return seed;
    }

    private void putStep1(
            Map<String, Object> seed,
            String scenarioName,
            String purpose,
            String targetEnv,
            String description) {
        Map<String, Object> step1 = map(seed.get("step1"));
        step1.put("scenarioName", LocalDate.now().getYear() + " " + scenarioName);
        step1.put("purpose", purpose);
        step1.put("targetEnv", targetEnv);
        step1.put("description", description);
        step1.put("baseDate", LocalDate.now().toString());
        seed.put("step1", step1);
    }

    private void enrichStep2(Map<String, Object> seed) {
        Map<String, Object> step2 = map(seed.get("step2"));
        Map<String, Object> enriched = stepRule.enrichStep2(step2);
        seed.put("step2", enriched);
    }

    private void putStep3Standard(
            Map<String, Object> seed,
            String baseline,
            boolean normal,
            boolean peak,
            boolean design,
            boolean stress,
            boolean drFault) {
        Map<String, Object> step2 = map(seed.get("step2"));
        int totalUsers = intValue(step2.get("totalUsers"), 36000);
        List<Map<String, Object>> scenarios = copyList(stepRule.defaultTpsScenarios(totalUsers));
        for (Map<String, Object> row : scenarios) {
            String code = text(row.get("code"));
            boolean enabled = switch (code) {
                case "NORMAL" -> normal;
                case "PEAK" -> peak;
                case "DESIGN_PEAK" -> design;
                case "STRESS" -> stress;
                case "DR_FAULT" -> drFault;
                default -> false;
            };
            row.put("enabled", enabled);
        }
        Map<String, Object> step3 = map(seed.get("step3"));
        step3.put("scenarios", scenarios);
        step3.put("operatingBaseline", baseline);
        List<String> perfTargets = new ArrayList<>();
        for (Map<String, Object> row : scenarios) {
            if (bool(row.get("enabled")) && !"SLOW_RESPONSE".equals(text(row.get("code")))
                    && !text(row.get("code")).startsWith("CUSTOM")) {
                perfTargets.add(text(row.get("code")));
            }
        }
        step3.put("performanceTestTargets", perfTargets);
        seed.put("step3", step3);
        syncStep6Baseline(seed);
    }

    private void syncStep6Baseline(Map<String, Object> seed) {
        Map<String, Object> step3 = map(seed.get("step3"));
        Map<String, Object> step6 = map(seed.get("step6"));
        String baseline = text(step3.get("operatingBaseline"));
        if (baseline.isBlank()) {
            baseline = "DESIGN_PEAK";
        }
        step6.put("baselineScenarioCode", baseline);
        seed.put("step6", step6);
    }

    @SuppressWarnings("unchecked")
    private static void enableScenario(Map<String, Object> step3, String code) {
        List<Map<String, Object>> scenarios = (List<Map<String, Object>>) step3.get("scenarios");
        if (scenarios == null) {
            return;
        }
        for (Map<String, Object> row : scenarios) {
            if (code.equalsIgnoreCase(text(row.get("code")))) {
                row.put("enabled", true);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void disableScenario(Map<String, Object> step3, String code) {
        List<Map<String, Object>> scenarios = (List<Map<String, Object>>) step3.get("scenarios");
        if (scenarios == null) {
            return;
        }
        for (Map<String, Object> row : scenarios) {
            if (code.equalsIgnoreCase(text(row.get("code")))) {
                row.put("enabled", false);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> copyMap(Map<String, Object> source) {
        if (source == null) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> map) {
                copy.put(entry.getKey(), copyMap((Map<String, Object>) map));
            } else if (value instanceof List<?> list) {
                copy.put(entry.getKey(), copyList(list));
            } else {
                copy.put(entry.getKey(), value);
            }
        }
        return copy;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> copyList(List<?> source) {
        List<Map<String, Object>> copy = new ArrayList<>();
        if (source == null) {
            return copy;
        }
        for (Object item : source) {
            if (item instanceof Map<?, ?> map) {
                copy.add(copyMap((Map<String, Object>) map));
            }
        }
        return copy;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object raw) {
        if (raw instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return new LinkedHashMap<>();
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static int intValue(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }

    private static boolean bool(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            return "true".equalsIgnoreCase(s);
        }
        return false;
    }

    public record TemplateCatalog(
            String code,
            String label,
            String description,
            String purpose,
            String targetEnv,
            String vmProfileCode,
            int totalUsers,
            int designPeakTps,
            int deploymentAp,
            int maxThreads,
            int poolPerVm,
            int sortOrder,
            Function<CapNewDefaultsCDTO, Map<String, Object>> seedBuilder) {
    }
}
