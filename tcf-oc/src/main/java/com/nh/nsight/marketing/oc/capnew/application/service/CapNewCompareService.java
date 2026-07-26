package com.nh.nsight.marketing.oc.capnew.application.service;

import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewCompareCDTO;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewCompareRequest;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewScenarioCDTO;
import com.nh.nsight.marketing.oc.capnew.support.CapNewBizException;
import com.nh.nsight.marketing.oc.capnew.support.CapNewScenarioStatus;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CapNewCompareService {

    private final CapNewWizardService wizardService;

    public CapNewCompareService(CapNewWizardService wizardService) {
        this.wizardService = wizardService;
    }

    public CapNewCompareCDTO compare(CapNewCompareRequest request) {
        List<String> ids = request.getScenarioIds() == null ? List.of() : request.getScenarioIds();
        if (ids.size() < 2) {
            throw new CapNewBizException("비교할 시나리오를 2개 이상 선택하세요.");
        }

        List<CapNewScenarioCDTO> scenarios = new ArrayList<>();
        for (String id : ids) {
            CapNewScenarioCDTO scenario = wizardService.getScenario(id);
            validateComparable(scenario);
            scenarios.add(scenario);
        }

        String baselineId = StringUtils.hasText(request.getBaselineScenarioId())
                ? request.getBaselineScenarioId()
                : ids.get(0);

        List<Map<String, Object>> snapshots = scenarios.stream().map(this::extractSnapshot).toList();
        CapNewCompareCDTO result = new CapNewCompareCDTO();
        result.setScenarioIds(ids);
        result.setBaselineScenarioId(baselineId);
        result.setColumns(buildColumns(snapshots));
        result.setMetricRows(buildMetricRows(snapshots));
        result.setDiffHighlights(findDiffHighlights(result.getMetricRows()));
        result.setRecommendation(buildRecommendation(snapshots, baselineId));
        result.setSummary(buildSummary(snapshots, baselineId));
        return result;
    }

    private void validateComparable(CapNewScenarioCDTO scenario) {
        String status = scenario.getStatus();
        if (!CapNewScenarioStatus.COMPLETED.name().equals(status)
                && !CapNewScenarioStatus.APPROVED.name().equals(status)) {
            throw new CapNewBizException(
                    scenario.getScenarioName() + " — COMPLETED 또는 APPROVED 상태만 비교할 수 있습니다.");
        }
        Map<String, Object> payload = scenario.getStepPayload();
        if (payload == null || !payload.containsKey("step8")) {
            throw new CapNewBizException(
                    scenario.getScenarioName() + " — STEP 8 종합 결과가 없습니다.");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractSnapshot(CapNewScenarioCDTO scenario) {
        Map<String, Object> payload = scenario.getStepPayload() == null ? Map.of() : scenario.getStepPayload();
        Map<String, Object> step2 = map(payload.get("step2"));
        Map<String, Object> step3 = map(payload.get("step3"));
        Map<String, Object> step4 = map(payload.get("step4"));
        Map<String, Object> step5 = map(payload.get("step5"));
        Map<String, Object> step6 = map(payload.get("step6"));
        Map<String, Object> step7 = map(payload.get("step7"));
        Map<String, Object> step8 = map(payload.get("step8"));
        Map<String, Object> headline = map(step8.get("headline"));

        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("scenarioId", scenario.getScenarioId());
        snap.put("scenarioName", scenario.getScenarioName());
        snap.put("projectName", scenario.getProjectName());
        snap.put("targetEnv", scenario.getTargetEnv());
        snap.put("status", scenario.getStatus());
        snap.put("versionNo", scenario.getVersionNo());
        snap.put("totalUsers", step2.get("totalUsers"));
        snap.put("designedSessions", step2.get("designedSessions"));
        snap.put("operatingBaseline", step3.get("operatingBaseline"));
        snap.put("designPeakTps", pick(headline.get("designPeakTps"), step4.get("designPeakTps")));
        snap.put("vmProfile", pick(headline.get("vmProfile"), step4.get("vmProfileCode")));
        snap.put("vmAdjustedTps", pick(headline.get("vmAdjustedTps"), step4.get("vmAdjustedTps")));
        snap.put("totalDeploymentAp", pick(headline.get("totalDeploymentAp"), step5.get("baselineTotalAp")));
        snap.put("apPerCenterFailover", step5.get("baselineApPerCenter"));
        snap.put("centerMode", step5.get("centerMode"));
        snap.put("drJudgment", step5.get("baselineJudgment"));
        snap.put("maxThreads", pick(headline.get("maxThreads"), step6.get("recommendedMaxThreads")));
        snap.put("jvmXmxGb", step6.get("jvmXmxGb"));
        snap.put("poolPerVm", pick(headline.get("poolPerVm"), step7.get("poolPerVm")));
        snap.put("totalDbSessions", pick(headline.get("totalDbSessions"), step7.get("totalDbSessions")));
        snap.put("dbStatus", step7.get("dbStatus"));
        snap.put("wasStatus", step6.get("wasStatus"));
        snap.put("overallJudgment", pick(headline.get("overallJudgment"), "NORMAL"));
        snap.put("conclusion", step8.get("conclusion"));
        snap.put("tpsScenarioResults", step5.get("scenarioResults"));
        return snap;
    }

    private List<Map<String, Object>> buildColumns(List<Map<String, Object>> snapshots) {
        List<Map<String, Object>> columns = new ArrayList<>();
        for (Map<String, Object> snap : snapshots) {
            Map<String, Object> col = new LinkedHashMap<>();
            col.put("scenarioId", snap.get("scenarioId"));
            col.put("scenarioName", snap.get("scenarioName"));
            col.put("projectName", snap.get("projectName"));
            col.put("targetEnv", snap.get("targetEnv"));
            col.put("status", snap.get("status"));
            col.put("overallJudgment", snap.get("overallJudgment"));
            columns.add(col);
        }
        return columns;
    }

    private List<Map<String, Object>> buildMetricRows(List<Map<String, Object>> snapshots) {
        List<MetricDef> defs = List.of(
                new MetricDef("totalUsers", "전체 사용자", "명"),
                new MetricDef("designedSessions", "설계 세션", "개"),
                new MetricDef("operatingBaseline", "운영 기준", ""),
                new MetricDef("designPeakTps", "설계 피크 TPS", "TPS"),
                new MetricDef("vmProfile", "VM Profile", ""),
                new MetricDef("vmAdjustedTps", "VM 보정 TPS", "TPS"),
                new MetricDef("totalDeploymentAp", "전체 배포 AP", "대"),
                new MetricDef("apPerCenterFailover", "장애 시 센터당 AP", "대"),
                new MetricDef("centerMode", "센터 구성", ""),
                new MetricDef("drJudgment", "DR 판정", ""),
                new MetricDef("maxThreads", "maxThreads", ""),
                new MetricDef("jvmXmxGb", "JVM Xmx", "GB"),
                new MetricDef("poolPerVm", "Pool/VM", ""),
                new MetricDef("totalDbSessions", "DB Session", ""),
                new MetricDef("overallJudgment", "종합 판정", "")
        );

        List<Map<String, Object>> rows = new ArrayList<>();
        for (MetricDef def : defs) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("key", def.key());
            row.put("label", def.label());
            row.put("unit", def.unit());
            List<Object> values = new ArrayList<>();
            for (Map<String, Object> snap : snapshots) {
                values.add(snap.get(def.key()));
            }
            row.put("values", values);
            rows.add(row);
        }
        return rows;
    }

    private List<String> findDiffHighlights(List<Map<String, Object>> metricRows) {
        List<String> highlights = new ArrayList<>();
        Set<String> numericKeys = Set.of(
                "totalUsers", "designedSessions", "designPeakTps", "vmAdjustedTps",
                "totalDeploymentAp", "apPerCenterFailover", "maxThreads", "poolPerVm", "totalDbSessions");
        for (Map<String, Object> row : metricRows) {
            String key = String.valueOf(row.get("key"));
            if (!numericKeys.contains(key)) {
                continue;
            }
            List<?> values = row.get("values") instanceof List<?> list ? list : List.of();
            Set<String> normalized = new LinkedHashSet<>();
            for (Object value : values) {
                normalized.add(String.valueOf(value));
            }
            if (normalized.size() > 1) {
                highlights.add(row.get("label") + " 차이: " + String.join(" / ", normalized));
            }
        }
        return highlights;
    }

    private String buildRecommendation(List<Map<String, Object>> snapshots, String baselineId) {
        Map<String, Object> baseline = snapshots.stream()
                .filter(s -> baselineId.equals(s.get("scenarioId")))
                .findFirst()
                .orElse(snapshots.get(0));

        Map<String, Object> best = null;
        int bestAp = Integer.MAX_VALUE;
        for (Map<String, Object> snap : snapshots) {
            if ("CRITICAL".equalsIgnoreCase(text(snap.get("overallJudgment")))) {
                continue;
            }
            int ap = intVal(snap.get("totalDeploymentAp"));
            if (ap > 0 && ap < bestAp) {
                bestAp = ap;
                best = snap;
            }
        }

        if (best == null) {
            return "모든 시나리오에 위험 판정이 있습니다. 조건 수정 후 재산정하세요.";
        }
        if (best.get("scenarioId").equals(baseline.get("scenarioId"))) {
            return "기준 시나리오(" + baseline.get("scenarioName") + ")가 최소 AP·양호 판정으로 운영 권장안입니다.";
        }
        return "기준 대비 " + best.get("scenarioName") + " (AP " + best.get("totalDeploymentAp")
                + "대, 판정 " + best.get("overallJudgment") + ")이 더 유리할 수 있습니다.";
    }

    private String buildSummary(List<Map<String, Object>> snapshots, String baselineId) {
        long critical = snapshots.stream()
                .filter(s -> "CRITICAL".equalsIgnoreCase(text(s.get("overallJudgment"))))
                .count();
        long warn = snapshots.stream()
                .filter(s -> "WARN".equalsIgnoreCase(text(s.get("overallJudgment"))))
                .count();
        return snapshots.size() + "개 시나리오 비교 · 기준 " + baselineId
                + " · 정상 " + (snapshots.size() - critical - warn)
                + " / 주의 " + warn + " / 위험 " + critical;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return Map.of();
    }

    private Object pick(Object primary, Object fallback) {
        return primary != null ? primary : fallback;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private int intVal(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private record MetricDef(String key, String label, String unit) {
    }
}
