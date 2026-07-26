package com.nh.nsight.marketing.om.application.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RuntimeCauseAnalyzer {

    public Map<String, Object> analyze(Map<String, Object> collected) {
        List<Map<String, Object>> targets = castList(collected.get("targets"));
        List<Map<String, Object>> findings = new ArrayList<>();
        String overallStatus = "NORMAL";
        String primaryCauseCode = "NORMAL";
        String primaryMessage = "현재 주요 병목이 없습니다.";
        String dominantBusinessCode = null;
        String dominantServiceId = null;
        String dominantSqlId = null;

        int totalActive = 0;
        Map<String, Integer> activeByBusiness = new HashMap<>();
        Map<String, Integer> activeByService = new HashMap<>();
        Map<String, Integer> sqlHits = new HashMap<>();

        for (Map<String, Object> targetRow : targets) {
            String businessCode = stringValue(targetRow.get("businessCode"));
            Map<String, Object> status = castMap(targetRow.get("status"));
            if (status.isEmpty()) {
                continue;
            }
            Map<String, Object> summary = castMap(status.get("summary"));
            String causeCode = stringValue(summary.get("primaryCauseCode"));
            String message = stringValue(summary.get("message"));
            String statusValue = stringValue(summary.get("status"));
            if (isWorse(statusValue, overallStatus)) {
                overallStatus = statusValue;
            }
            if (isHigherPriority(causeCode, primaryCauseCode)) {
                primaryCauseCode = causeCode;
                primaryMessage = message;
                dominantBusinessCode = businessCode;
            }
            findings.add(buildFinding(businessCode, causeCode, message, status));

            int activeCount = (int) toLong(summary.get("activeTransactionCount"));
            totalActive += activeCount;
            activeByBusiness.merge(businessCode, activeCount, Integer::sum);

            for (Map<String, Object> tx : castList(status.get("activeTransactions"))) {
                String serviceId = stringValue(tx.get("serviceId"));
                if (serviceId != null) {
                    activeByService.merge(serviceId, 1, Integer::sum);
                }
                String sqlId = stringValue(tx.get("sqlId"));
                if (sqlId != null) {
                    sqlHits.merge(sqlId, 1, Integer::sum);
                }
            }
            for (Map<String, Object> sql : castList(status.get("slowSql"))) {
                String sqlId = stringValue(sql.get("sqlId"));
                if (sqlId != null) {
                    sqlHits.merge(sqlId, 1, Integer::sum);
                }
            }
        }

        dominantBusinessCode = resolveDominantBusiness(activeByBusiness, totalActive, dominantBusinessCode);
        dominantServiceId = resolveDominantService(activeByService, totalActive);
        dominantSqlId = resolveDominantSql(sqlHits);

        Map<String, Object> cards = buildCards(targets, totalActive, activeByBusiness);
        AnalysisState resultHolder = new AnalysisState();
        applyDominanceCause(
                totalActive,
                activeByBusiness,
                activeByService,
                dominantBusinessCode,
                dominantServiceId,
                cards,
                overallStatus,
                primaryCauseCode,
                primaryMessage,
                resultHolder);
        applyUnknownFallback(resultHolder, cards, totalActive);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("overallStatus", resultHolder.overallStatus);
        result.put("primaryCauseCode", resultHolder.primaryCauseCode);
        result.put("primaryMessage", resultHolder.primaryMessage);
        result.put("dominantBusinessCode", dominantBusinessCode);
        result.put("dominantServiceId", dominantServiceId);
        result.put("dominantSqlId", dominantSqlId);
        result.put("findings", findings);
        result.put("cards", cards);
        result.put("businessOwnership", toOwnershipRows(activeByBusiness, totalActive));
        return result;
    }

    private void applyDominanceCause(
            int totalActive,
            Map<String, Integer> activeByBusiness,
            Map<String, Integer> activeByService,
            String dominantBusinessCode,
            String dominantServiceId,
            Map<String, Object> cards,
            String overallStatus,
            String primaryCauseCode,
            String primaryMessage,
            AnalysisState state) {
        state.overallStatus = overallStatus;
        state.primaryCauseCode = primaryCauseCode;
        state.primaryMessage = primaryMessage;

        if (totalActive <= 0) {
            return;
        }
        double threadBusyRatio = toDouble(cards.get("threadBusyRatio"));
        if (dominantBusinessCode != null) {
            double businessOwnership = activeByBusiness.getOrDefault(dominantBusinessCode, 0) * 100.0 / totalActive;
            if (businessOwnership >= 60
                    && threadBusyRatio >= 80
                    && isHigherPriority("BUSINESS_RESOURCE_DOMINANCE", state.primaryCauseCode)) {
                state.primaryCauseCode = "BUSINESS_RESOURCE_DOMINANCE";
                state.primaryMessage = String.format(
                        "%s 업무가 현재 전체 실행 거래의 %s%%를 점유하고 있습니다.",
                        dominantBusinessCode,
                        round1(businessOwnership));
                if (isWorse("WARN", state.overallStatus)) {
                    state.overallStatus = "WARN";
                }
                return;
            }
        }
        if (dominantServiceId != null) {
            double serviceOwnership = activeByService.getOrDefault(dominantServiceId, 0) * 100.0 / totalActive;
            if (serviceOwnership >= 40 && isHigherPriority("SERVICE_DOMINANCE", state.primaryCauseCode)) {
                state.primaryCauseCode = "SERVICE_DOMINANCE";
                state.primaryMessage = String.format(
                        "%s 거래가 현재 Tomcat 처리량의 %s%%를 점유하고 있습니다.",
                        dominantServiceId,
                        round1(serviceOwnership));
                if (isWorse("WARN", state.overallStatus)) {
                    state.overallStatus = "WARN";
                }
            }
        }
    }

    private void applyUnknownFallback(AnalysisState state, Map<String, Object> cards, int totalActive) {
        if (!"NORMAL".equals(state.primaryCauseCode)) {
            return;
        }
        int slowTx = (int) toLong(cards.get("slowTransactionCount"));
        int slowSql = (int) toLong(cards.get("slowSqlCount"));
        double maxBusy = toDouble(cards.get("threadBusyRatio"));
        if (slowTx > 0 || slowSql > 0 || (totalActive > 0 && maxBusy >= 70)) {
            state.primaryCauseCode = "UNKNOWN";
            state.primaryMessage = "추가 로그와 DB 상태 확인이 필요합니다.";
            state.overallStatus = "UNKNOWN";
        }
    }

    private static final class AnalysisState {
        private String overallStatus;
        private String primaryCauseCode;
        private String primaryMessage;
    }

    private Map<String, Object> buildCards(List<Map<String, Object>> targets, int totalActive,
                                             Map<String, Integer> activeByBusiness) {
        double maxBusyRatio = 0;
        double maxCpu = 0;
        double maxHeap = 0;
        int maxDbActive = 0;
        int maxDbMax = 0;
        int maxPending = 0;
        int slowTx = 0;
        int slowSql = 0;

        for (Map<String, Object> targetRow : targets) {
            Map<String, Object> status = castMap(targetRow.get("status"));
            Map<String, Object> thread = castMap(status.get("thread"));
            Map<String, Object> jvm = castMap(status.get("jvm"));
            Map<String, Object> summary = castMap(status.get("summary"));
            maxBusyRatio = Math.max(maxBusyRatio, toDouble(thread.get("busyRatio")));
            maxCpu = Math.max(maxCpu, toDouble(jvm.get("processCpuRatio")));
            maxHeap = Math.max(maxHeap, toDouble(jvm.get("heapRatio")));
            slowTx += (int) toLong(summary.get("slowTransactionCount"));
            slowSql += (int) toLong(summary.get("slowSqlCount"));
            for (Map<String, Object> pool : castList(status.get("dbPools"))) {
                maxDbActive = Math.max(maxDbActive, (int) toLong(pool.get("active")));
                maxDbMax = Math.max(maxDbMax, (int) toLong(pool.get("maximum")));
                maxPending = Math.max(maxPending, (int) toLong(pool.get("pending")));
            }
        }

        String dominantBusiness = null;
        double dominantOwnership = 0;
        if (totalActive > 0) {
            for (Map.Entry<String, Integer> entry : activeByBusiness.entrySet()) {
                double ownership = entry.getValue() * 100.0 / totalActive;
                if (ownership > dominantOwnership) {
                    dominantOwnership = ownership;
                    dominantBusiness = entry.getKey();
                }
            }
        }

        Map<String, Object> cards = new LinkedHashMap<>();
        cards.put("threadBusyRatio", round1(maxBusyRatio));
        cards.put("jvmCpuRatio", round1(maxCpu));
        cards.put("heapRatio", round1(maxHeap));
        cards.put("dbActive", maxDbActive);
        cards.put("dbMaximum", maxDbMax);
        cards.put("dbPending", maxPending);
        cards.put("slowTransactionCount", slowTx);
        cards.put("slowSqlCount", slowSql);
        cards.put("dominantBusinessCode", dominantBusiness);
        cards.put("dominantBusinessOwnershipPct", round1(dominantOwnership));
        return cards;
    }

    private List<Map<String, Object>> toOwnershipRows(Map<String, Integer> activeByBusiness, int totalActive) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : activeByBusiness.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("businessCode", entry.getKey());
            row.put("activeTransactions", entry.getValue());
            row.put("ownershipPct", totalActive > 0
                    ? round1(entry.getValue() * 100.0 / totalActive) : 0);
            rows.add(row);
        }
        rows.sort(Comparator.comparingDouble((Map<String, Object> row) ->
                toDouble(row.get("ownershipPct"))).reversed());
        return rows;
    }

    private Map<String, Object> buildFinding(String businessCode, String causeCode, String message,
                                               Map<String, Object> status) {
        Map<String, Object> finding = new LinkedHashMap<>();
        finding.put("businessCode", businessCode);
        finding.put("causeCode", causeCode);
        finding.put("message", message);
        finding.put("status", castMap(status.get("summary")).get("status"));
        finding.put("thread", status.get("thread"));
        finding.put("dbPools", status.get("dbPools"));
        return finding;
    }

    private String resolveDominantBusiness(Map<String, Integer> activeByBusiness, int totalActive,
                                           String fallback) {
        if (totalActive <= 0) {
            return fallback;
        }
        String dominant = fallback;
        double maxOwnership = 0;
        for (Map.Entry<String, Integer> entry : activeByBusiness.entrySet()) {
            double ownership = entry.getValue() * 100.0 / totalActive;
            if (ownership >= 60 && ownership > maxOwnership) {
                maxOwnership = ownership;
                dominant = entry.getKey();
            }
        }
        return dominant;
    }

    private String resolveDominantService(Map<String, Integer> activeByService, int totalActive) {
        if (totalActive <= 0) {
            return null;
        }
        String dominant = null;
        double maxOwnership = 0;
        for (Map.Entry<String, Integer> entry : activeByService.entrySet()) {
            double ownership = entry.getValue() * 100.0 / totalActive;
            if (ownership >= 40 && ownership > maxOwnership) {
                maxOwnership = ownership;
                dominant = entry.getKey();
            }
        }
        return dominant;
    }

    private String resolveDominantSql(Map<String, Integer> sqlHits) {
        return sqlHits.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private boolean isHigherPriority(String candidate, String current) {
        return priority(candidate) < priority(current);
    }

    private int priority(String causeCode) {
        return switch (causeCode == null ? "UNKNOWN" : causeCode) {
            case "THREAD_DEADLOCK" -> 1;
            case "DB_POOL_EXHAUSTED" -> 2;
            case "GC_PRESSURE" -> 3;
            case "CPU_OVERLOAD" -> 4;
            case "THREAD_SATURATION" -> 5;
            case "SLOW_SQL" -> 6;
            case "EXTERNAL_WAIT" -> 7;
            case "BUSINESS_RESOURCE_DOMINANCE", "BUSINESS_DOMINANCE" -> 8;
            case "SERVICE_DOMINANCE" -> 8;
            case "UNKNOWN" -> 9;
            case "NORMAL" -> 99;
            default -> 50;
        };
    }

    private boolean isWorse(String candidate, String current) {
        int candidateScore = severity(candidate);
        int currentScore = severity(current);
        return candidateScore > currentScore;
    }

    private int severity(String status) {
        return switch (status == null ? "NORMAL" : status) {
            case "CRITICAL" -> 3;
            case "WARN" -> 2;
            default -> 1;
        };
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        if (value instanceof List<?> list) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    rows.add((Map<String, Object>) map);
                }
            }
            return rows;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static double toDouble(Object value) {
        if (value == null) {
            return 0;
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private static long toLong(Object value) {
        if (value == null) {
            return 0;
        }
        return Long.parseLong(String.valueOf(value));
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
