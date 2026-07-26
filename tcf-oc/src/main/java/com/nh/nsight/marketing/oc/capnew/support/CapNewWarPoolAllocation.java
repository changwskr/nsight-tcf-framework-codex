package com.nh.nsight.marketing.oc.capnew.support;

import com.nh.nsight.marketing.oc.support.NsightDbPoolDerivation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * STEP 7 — 동일 Tomcat 내 업무 WAR별 HikariCP Pool 배분·합계 검증 (설계서 §10.4).
 */
public final class CapNewWarPoolAllocation {

    private static final List<String> DEFAULT_RECOMMENDATIONS = List.of(
            "업무 WAR별 Pool 재배분",
            "배포 AP 수와 Pool 합계 재계산",
            "조회 전용 DB 또는 업무 DB 분리 검토",
            "DB 최대 Session 증설 검토");

    private CapNewWarPoolAllocation() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> compute(
            List<Map<String, Object>> warAllocations,
            double apTps,
            int threadsPerVm,
            int deploymentAp,
            int dbSessionLimit,
            double holdSec,
            double dbUsage,
            double poolSafety,
            double threadDbRatio,
            int minPoolPerWar,
            int profilePoolCap) {

        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> enabled = filterEnabled(warAllocations);
        if (enabled.isEmpty()) {
            result.put("warPoolResults", List.of());
            result.put("warPoolTotalSessions", 0L);
            result.put("warPoolStatus", "NORMAL");
            result.put("warPoolStatusMessage", "활성 WAR 배분이 없습니다.");
            result.put("warPoolExcess", 0L);
            result.put("warPoolRecommendations", List.of());
            return result;
        }

        double weightSum = enabled.stream()
                .mapToDouble(w -> doubleVal(w.get("weightPercent"), 0))
                .sum();
        if (weightSum <= 0) {
            result.put("warPoolResults", List.of());
            result.put("warPoolTotalSessions", 0L);
            result.put("warPoolStatus", "NORMAL");
            result.put("warPoolStatusMessage", "WAR 비중 합계가 0입니다.");
            result.put("warPoolExcess", 0L);
            result.put("warPoolRecommendations", List.of());
            return result;
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        long totalSessions = 0L;
        int minWar = Math.max(1, minPoolPerWar);

        for (Map<String, Object> war : enabled) {
            double weight = doubleVal(war.get("weightPercent"), 0);
            double share = weight / weightSum;
            double warApTps = apTps * share;
            int warThreads = Math.max(1, (int) Math.ceil(threadsPerVm * share));

            var sizing = NsightDbPoolDerivation.recommend(new NsightDbPoolDerivation.Input(
                    warApTps, warThreads, holdSec, dbUsage, poolSafety, threadDbRatio, minWar, profilePoolCap));

            int poolPerVm = sizing.recommendedPool();
            long warTotal = (long) deploymentAp * poolPerVm;
            totalSessions += warTotal;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("warCode", text(war.get("warCode"), "WAR"));
            row.put("label", text(war.get("label"), text(war.get("warCode"), "WAR")));
            row.put("weightPercent", weight);
            row.put("trafficShare", Math.round(share * 1000.0) / 10.0);
            row.put("warApTps", sizing.apTpsRounded());
            row.put("warThreads", warThreads);
            row.put("poolPerVm", poolPerVm);
            row.put("deploymentAp", deploymentAp);
            row.put("totalPool", warTotal);
            row.put("judgment", "NORMAL");
            row.put("poolFormula", sizing.formulaSummary());
            rows.add(row);
        }

        long excess = Math.max(0L, totalSessions - dbSessionLimit);
        String status;
        String message;
        List<String> recommendations = new ArrayList<>();
        if (totalSessions > dbSessionLimit) {
            status = "CRITICAL";
            message = String.format(
                    "DB 허용 Session %,d — WAR 합계 %,d (초과 %,d)",
                    dbSessionLimit, totalSessions, excess);
            recommendations.addAll(DEFAULT_RECOMMENDATIONS);
        } else if (totalSessions > dbSessionLimit * 0.8) {
            status = "WARN";
            message = String.format(
                    "DB 허용 Session %,d — WAR 합계 %,d (한도 80%% 근접)",
                    dbSessionLimit, totalSessions);
            recommendations.add("업무 WAR별 Pool 재배분");
            recommendations.add("DB 최대 Session 증설 검토");
        } else {
            status = "NORMAL";
            message = String.format(
                    "DB 허용 Session %,d — WAR 합계 %,d",
                    dbSessionLimit, totalSessions);
        }

        result.put("warPoolResults", rows);
        result.put("warPoolTotalSessions", totalSessions);
        result.put("warPoolWeightSum", weightSum);
        result.put("warPoolStatus", status);
        result.put("warPoolStatusMessage", message);
        result.put("warPoolExcess", excess);
        result.put("warPoolRecommendations", recommendations);
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> filterEnabled(List<Map<String, Object>> warAllocations) {
        List<Map<String, Object>> enabled = new ArrayList<>();
        if (warAllocations == null) {
            return enabled;
        }
        for (Object item : warAllocations) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            Map<String, Object> war = (Map<String, Object>) map;
            if (boolVal(war.get("enabled"), true)) {
                enabled.add(war);
            }
        }
        return enabled;
    }

    private static String text(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? fallback : s;
    }

    private static double doubleVal(Object value, double fallback) {
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

    private static boolean boolVal(Object value, boolean fallback) {
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
