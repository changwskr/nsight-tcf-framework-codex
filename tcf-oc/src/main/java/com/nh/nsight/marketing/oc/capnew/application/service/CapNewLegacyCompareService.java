package com.nh.nsight.marketing.oc.capnew.application.service;

import com.nh.nsight.marketing.oc.application.dto.capacity.CapacityCalculationCDTO;
import com.nh.nsight.marketing.oc.application.dto.capacity.CapacityCalculationResultCDTO;
import com.nh.nsight.marketing.oc.application.dto.capacity.CapacityScenarioResultCDTO;
import com.nh.nsight.marketing.oc.application.dto.capacity.DbPoolResultCDTO;
import com.nh.nsight.marketing.oc.application.dto.capacity.WasThreadResultCDTO;
import com.nh.nsight.marketing.oc.application.service.ASMSC71001;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewLegacyCompareCDTO;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewScenarioCDTO;
import com.nh.nsight.marketing.oc.capnew.support.CapNewBizException;
import com.nh.nsight.marketing.oc.capnew.support.CapNewScenarioStatus;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Service;

/**
 * cap-new 시나리오를 기존 CAP({@link ASMSC71001}) 산정 결과와 대조합니다.
 */
@Service
public class CapNewLegacyCompareService {

    private final CapNewWizardService wizardService;
    private final ASMSC71001 legacyCapacityService;

    public CapNewLegacyCompareService(
            CapNewWizardService wizardService,
            ASMSC71001 legacyCapacityService) {
        this.wizardService = wizardService;
        this.legacyCapacityService = legacyCapacityService;
    }

    public CapNewLegacyCompareCDTO compare(String scenarioId) {
        CapNewScenarioCDTO scenario = wizardService.getScenario(scenarioId);
        validateComparable(scenario);

        Map<String, Object> payload = scenario.getStepPayload();
        Map<String, Object> step2 = map(payload.get("step2"));
        Map<String, Object> step3 = map(payload.get("step3"));
        Map<String, Object> step4 = map(payload.get("step4"));
        Map<String, Object> step5 = map(payload.get("step5"));
        Map<String, Object> step6 = map(payload.get("step6"));
        Map<String, Object> step7 = map(payload.get("step7"));
        Map<String, Object> step8 = map(payload.get("step8"));
        Map<String, Object> headline = map(step8.get("headline"));

        String baselineCode = text(step3.get("operatingBaseline"), "DESIGN_PEAK");
        Map<String, Object> baselineScenario = findBaselineScenario(step3, baselineCode);

        CapacityCalculationCDTO legacyRequest = toLegacyRequest(scenario, payload);
        CapacityCalculationResultCDTO legacyResult = legacyCapacityService.calculate(legacyRequest);

        double baselineRate = doubleVal(baselineScenario.get("concurrentRate"), 0.05);
        int baselineResponse = intVal(baselineScenario.get("responseSec"), 3);
        CapacityScenarioResultCDTO legacyRow = findLegacyRow(legacyResult, baselineRate, baselineResponse);
        if (legacyRow == null) {
            throw new CapNewBizException(
                    "기존 CAP 결과에서 운영 기준 시나리오(동시요청률·응답시간)를 찾을 수 없습니다.");
        }

        CapNewLegacyCompareCDTO result = new CapNewLegacyCompareCDTO();
        result.setScenarioId(scenario.getScenarioId());
        result.setScenarioName(scenario.getScenarioName());
        result.setBaselineCode(baselineCode);
        result.setBaselineLabel(text(baselineScenario.get("label"), baselineCode));
        result.setBaselineConcurrentRate(baselineRate);
        result.setBaselineResponseSec(baselineResponse);
        result.setNotes(buildNotes(step2, step4, step5));

        List<CapNewLegacyCompareCDTO.MetricRow> metrics = new ArrayList<>();
        metrics.add(metricInt("targetTps", "목표 TPS", "TPS",
                pick(headline.get("designPeakTps"), step4.get("designPeakTps"), baselineScenario.get("targetTps")),
                legacyRow.getTargetTps(), 0));
        metrics.add(metricInt("deploymentAp", "배포 AP", "대",
                pick(headline.get("totalDeploymentAp"), step5.get("baselineTotalAp")),
                legacyRow.getDeploymentApCount(), 0));
        metrics.add(metricInt("maxThreads", "maxThreads", "",
                pick(headline.get("maxThreads"), step6.get("recommendedMaxThreads")),
                threadValue(legacyRow.getWasThread()), 2));
        metrics.add(metricInt("poolPerVm", "Pool/VM", "",
                pick(headline.get("poolPerVm"), step7.get("poolPerVm")),
                poolValue(legacyRow.getDbPool()), 1));
        metrics.add(metricLong("totalDbSessions", "DB Session 합계", "",
                pick(headline.get("totalDbSessions"), step7.get("totalDbSessions")),
                sessionsValue(legacyRow.getDbPool()), 0L));

        result.setMetrics(metrics);
        result.setOverallStatus(resolveOverall(metrics));
        result.setSummary(buildSummary(result));
        return result;
    }

    private void validateComparable(CapNewScenarioCDTO scenario) {
        String status = scenario.getStatus();
        if (!CapNewScenarioStatus.COMPLETED.name().equals(status)
                && !CapNewScenarioStatus.APPROVED.name().equals(status)) {
            throw new CapNewBizException("COMPLETED 또는 APPROVED 상태만 기존 CAP 대조할 수 있습니다.");
        }
        Map<String, Object> payload = scenario.getStepPayload();
        if (payload == null || !payload.containsKey("step8")) {
            throw new CapNewBizException("STEP 8 종합 결과가 없습니다. Wizard를 완료하세요.");
        }
    }

    @SuppressWarnings("unchecked")
    private CapacityCalculationCDTO toLegacyRequest(CapNewScenarioCDTO scenario, Map<String, Object> payload) {
        Map<String, Object> step1 = map(payload.get("step1"));
        Map<String, Object> step2 = map(payload.get("step2"));
        Map<String, Object> step3 = map(payload.get("step3"));
        Map<String, Object> step4 = map(payload.get("step4"));
        Map<String, Object> step5 = map(payload.get("step5"));
        Map<String, Object> step6 = map(payload.get("step6"));
        Map<String, Object> step7 = map(payload.get("step7"));

        int branchCount = intVal(step2.get("branchCount"), 6000);
        int userPerBranch = intVal(step2.get("userPerBranch"), 6);
        int hqUsers = intVal(step2.get("hqUsers"), 0);
        int otherUsers = intVal(step2.get("otherUsers"), 0);
        int totalUsers = intVal(step2.get("totalUsers"), branchCount * userPerBranch + hqUsers + otherUsers);

        CapacityCalculationCDTO dto = new CapacityCalculationCDTO();
        dto.setProjectName(text(step1.get("projectName"), scenario.getProjectName()));
        if (hqUsers > 0 || otherUsers > 0 || totalUsers != branchCount * userPerBranch) {
            dto.setBranchCount(Math.max(1, totalUsers));
            dto.setUserPerBranch(1);
        } else {
            dto.setBranchCount(branchCount);
            dto.setUserPerBranch(userPerBranch);
        }
        dto.setSessionMarginRate(doubleVal(step2.get("sessionMarginRate"), 0.30));
        dto.setSessionTimeoutMin(intVal(step2.get("sessionTimeoutMin"), 60));
        dto.setConcurrentRequestRates(extractRates(step3));
        dto.setTargetResponseTimes(extractResponseTimes(step3));
        dto.setVmSpecCode(text(step4.get("vmProfileCode"), "16CORE-128GB"));
        dto.setTpsPerCore(intVal(step4.get("tpsPerCore"), 0));
        dto.setTpmcPerTps(intVal(step4.get("tpmcPerTps"), 3000));
        dto.setAvgThreadHoldSec(doubleVal(step6.get("avgThreadHoldSec"), 1.2));
        dto.setThreadMarginRate(doubleVal(step6.get("threadMarginRate"), 1.2));
        dto.setMaxThreadMarginRate(doubleVal(step6.get("maxThreadMarginRate"), 1.3));
        dto.setApType(mapApType(text(step7.get("apType"), "SINGLE_VIEW")));
        dto.setActiveActive("ACTIVE_ACTIVE".equalsIgnoreCase(text(step5.get("centerMode"), "ACTIVE_ACTIVE")));
        dto.setDrValidation(boolVal(step5.get("drSingleCenterFullLoad"), true));
        dto.setValidateDbPool(true);
        dto.setDbSessionLimit(intVal(step7.get("dbSessionLimit"), 500));
        dto.setAvgDbConnectionHoldSec(doubleVal(step7.get("avgDbConnectionHoldSec"), 0));
        dto.setDbTransactionUsageRatio(doubleVal(step7.get("dbTransactionUsageRatio"), 1.0));
        dto.setPoolSafetyFactor(doubleVal(step7.get("poolSafetyFactor"), 1.3));
        dto.setThreadDbUsageRatio(doubleVal(step7.get("threadDbUsageRatio"), 0.30));
        dto.setMinPoolPerVm(intVal(step7.get("minPoolPerVm"), 30));
        dto.setCalculationStep("ALL");
        return dto;
    }

    @SuppressWarnings("unchecked")
    private List<String> buildNotes(
            Map<String, Object> step2,
            Map<String, Object> step4,
            Map<String, Object> step5) {
        List<String> notes = new ArrayList<>();
        int hqUsers = intVal(step2.get("hqUsers"), 0);
        int otherUsers = intVal(step2.get("otherUsers"), 0);
        if (hqUsers > 0 || otherUsers > 0) {
            notes.add("기존 CAP은 지점×지점당 사용자만 사용합니다. 본부·기타 사용자는 전체 사용자 수로 등가 변환했습니다.");
        }
        if (boolVal(step4.get("applyCorrectionFactors"), false)) {
            notes.add("cap-new VM 보정 TPS(가상화·운영효율)는 기존 CAP 단순 VM TPS와 다를 수 있습니다.");
        }
        notes.add("cap-new AP 산정은 센터별 여유·DR 페일오버 규칙을 포함합니다. 기존 CAP AP는 단순 ceil(TPS/VM TPS)×센터 수입니다.");
        if (!"ACTIVE_ACTIVE".equalsIgnoreCase(text(step5.get("centerMode"), "ACTIVE_ACTIVE"))) {
            notes.add("센터 모드가 ACTIVE_ACTIVE가 아니면 배포 AP 대수 비교에 차이가 날 수 있습니다.");
        }
        return notes;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findBaselineScenario(Map<String, Object> step3, String baselineCode) {
        Object raw = step3.get("scenarios");
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> map)) {
                    continue;
                }
                Map<String, Object> scenario = (Map<String, Object>) map;
                if (baselineCode.equalsIgnoreCase(text(scenario.get("code"), ""))
                        && boolVal(scenario.get("enabled"), false)) {
                    return scenario;
                }
            }
        }
        throw new CapNewBizException("운영 기준 TPS 시나리오 [" + baselineCode + "]를 찾을 수 없습니다.");
    }

    private CapacityScenarioResultCDTO findLegacyRow(
            CapacityCalculationResultCDTO result, double rate, int responseSec) {
        CapacityScenarioResultCDTO best = null;
        double bestDelta = Double.MAX_VALUE;
        for (CapacityScenarioResultCDTO row : result.getResults()) {
            if (row.getResponseTimeSec() != responseSec) {
                continue;
            }
            double delta = Math.abs(row.getConcurrentRate() - rate);
            if (delta < 0.0001) {
                return row;
            }
            if (delta < bestDelta) {
                bestDelta = delta;
                best = row;
            }
        }
        return best;
    }

    private CapNewLegacyCompareCDTO.MetricRow metricInt(
            String id, String label, String unit, Object capNew, Object legacy, int closeTolerance) {
        int cap = toInt(capNew);
        int leg = toInt(legacy);
        int diff = cap - leg;
        CapNewLegacyCompareCDTO.MetricRow row = new CapNewLegacyCompareCDTO.MetricRow();
        row.setMetricId(id);
        row.setMetricLabel(label);
        row.setUnit(unit);
        row.setCapNewValue(cap);
        row.setLegacyValue(leg);
        row.setDiff(diff);
        row.setStatus(classifyInt(diff, closeTolerance));
        return row;
    }

    private CapNewLegacyCompareCDTO.MetricRow metricLong(
            String id, String label, String unit, Object capNew, Object legacy, long closeTolerance) {
        long cap = toLong(capNew);
        long leg = toLong(legacy);
        long diff = cap - leg;
        CapNewLegacyCompareCDTO.MetricRow row = new CapNewLegacyCompareCDTO.MetricRow();
        row.setMetricId(id);
        row.setMetricLabel(label);
        row.setUnit(unit);
        row.setCapNewValue(cap);
        row.setLegacyValue(leg);
        row.setDiff(diff);
        row.setStatus(classifyLong(diff, closeTolerance));
        return row;
    }

    private String classifyInt(int diff, int closeTolerance) {
        if (diff == 0) {
            return "MATCH";
        }
        if (Math.abs(diff) <= closeTolerance) {
            return "CLOSE";
        }
        return "DIFF";
    }

    private String classifyLong(long diff, long closeTolerance) {
        if (diff == 0L) {
            return "MATCH";
        }
        if (Math.abs(diff) <= closeTolerance) {
            return "CLOSE";
        }
        return "DIFF";
    }

    private String resolveOverall(List<CapNewLegacyCompareCDTO.MetricRow> metrics) {
        boolean anyDiff = false;
        boolean anyClose = false;
        for (CapNewLegacyCompareCDTO.MetricRow row : metrics) {
            if ("DIFF".equals(row.getStatus())) {
                anyDiff = true;
            } else if ("CLOSE".equals(row.getStatus())) {
                anyClose = true;
            }
        }
        if (!anyDiff && !anyClose) {
            return "MATCH";
        }
        if (anyDiff) {
            return "PARTIAL";
        }
        return "CLOSE";
    }

    private String buildSummary(CapNewLegacyCompareCDTO result) {
        long match = result.getMetrics().stream().filter(m -> "MATCH".equals(m.getStatus())).count();
        long close = result.getMetrics().stream().filter(m -> "CLOSE".equals(m.getStatus())).count();
        long diff = result.getMetrics().stream().filter(m -> "DIFF".equals(m.getStatus())).count();
        return "운영 기준 [" + result.getBaselineLabel() + "] — 일치 " + match
                + " · 근접 " + close + " · 차이 " + diff + " (" + result.getOverallStatus() + ")";
    }

    @SuppressWarnings("unchecked")
    private List<Double> extractRates(Map<String, Object> step3) {
        Set<Double> rates = new LinkedHashSet<>();
        Object raw = step3.get("scenarios");
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> map)) {
                    continue;
                }
                if (!boolVal(map.get("enabled"), false)) {
                    continue;
                }
                double rate = doubleVal(map.get("concurrentRate"), 0);
                if (rate > 0) {
                    rates.add(rate <= 1.0 ? rate : rate / 100.0);
                }
            }
        }
        if (rates.isEmpty()) {
            return List.of(0.03, 0.05, 0.10, 0.15);
        }
        return new ArrayList<>(rates);
    }

    @SuppressWarnings("unchecked")
    private List<Integer> extractResponseTimes(Map<String, Object> step3) {
        Set<Integer> times = new TreeSet<>();
        Object raw = step3.get("scenarios");
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> map)) {
                    continue;
                }
                if (!boolVal(map.get("enabled"), false)) {
                    continue;
                }
                int sec = intVal(map.get("responseSec"), 0);
                if (sec > 0) {
                    times.add(sec);
                }
            }
        }
        if (times.isEmpty()) {
            return List.of(3, 4, 5);
        }
        return new ArrayList<>(times);
    }

    private String mapApType(String apType) {
        if (apType == null) {
            return "GENERAL";
        }
        return apType.toUpperCase().contains("SINGLE") ? "SINGLE_VIEW" : "GENERAL";
    }

    private int threadValue(WasThreadResultCDTO was) {
        return was == null ? 0 : was.getRecommendedMaxThreads();
    }

    private int poolValue(DbPoolResultCDTO db) {
        return db == null ? 0 : db.getPoolPerVm();
    }

    private long sessionsValue(DbPoolResultCDTO db) {
        return db == null ? 0L : db.getTotalDbSessions();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private Object pick(Object... values) {
        for (Object value : values) {
            if (value != null && !"".equals(String.valueOf(value))) {
                return value;
            }
        }
        return 0;
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

    private long toLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private int toInt(Object value) {
        return intVal(value, 0);
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
        if ("true".equals(s) || "1".equals(s) || "y".equals(s)) {
            return true;
        }
        if ("false".equals(s) || "0".equals(s) || "n".equals(s)) {
            return false;
        }
        return fallback;
    }
}
