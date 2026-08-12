package nhnis.mg.co.a.application.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import nhnis.fw.commons.runtime.MgRuntimeMonitor;

/**
 * 런타임 진단 조회 (OM OmRuntimeService.inquiry 축약 · 단일 pdmg-service 스냅샷).
 */
@Service
public class mgcoa9100Service {

    private static final List<String> CAUSE_PRIORITY = List.of(
            "THREAD_DEADLOCK",
            "DB_POOL_EXHAUSTED",
            "GC_PRESSURE",
            "CPU_OVERLOAD",
            "THREAD_SATURATION",
            "SLOW_SQL",
            "EXTERNAL_WAIT",
            "BUSINESS_RESOURCE_DOMINANCE",
            "SERVICE_DOMINANCE",
            "UNKNOWN",
            "NORMAL");

    private final MgRuntimeMonitor monitor;

    public mgcoa9100Service(MgRuntimeMonitor monitor) {
        this.monitor = monitor;
    }

    public Map<String, Object> inquiry(Map<String, Object> body) {
        boolean includeDetails = "Y".equalsIgnoreCase(stringValue(body == null ? null : body.get("includeDetails")));

        Map<String, Object> status = monitor.createStatusSnapshot();
        List<Map<String, Object>> activeTransactions = includeDetails
                ? monitor.getActiveTransactions()
                : List.of();
        List<Map<String, Object>> slowTransactions = List.of();
        List<Map<String, Object>> slowSql = List.of();
        List<Map<String, Object>> threads = List.of();

        Map<String, Object> target = new LinkedHashMap<>();
        target.put("businessCode", "MG");
        target.put("reachable", true);
        target.put("status", status);

        Map<String, Object> collected = new LinkedHashMap<>();
        collected.put("checkedAt", monitor.checkedAt());
        collected.put("targets", List.of(target));

        Map<String, Object> analysis = analyze(status, activeTransactions);
        Map<String, Object> threadAnalysis = buildThreadAnalysis(status, analysis);
        Map<String, Object> jvmAnalysis = buildJvmAnalysis(status, analysis);
        Map<String, Object> dbPoolAnalysis = buildDbPoolAnalysis(status, analysis, activeTransactions);
        Map<String, Object> sqlAnalysis = buildSqlAnalysis(analysis);
        Map<String, Object> dominanceAnalysis = buildDominanceAnalysis(analysis, activeTransactions);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "MG");
        result.put("screen", "런타임 진단");
        result.put("checkedAt", collected.get("checkedAt"));
        result.put("overallStatus", analysis.get("overallStatus"));
        result.put("primaryCauseCode", analysis.get("primaryCauseCode"));
        result.put("primaryMessage", analysis.get("primaryMessage"));
        result.put("dominantBusinessCode", analysis.get("dominantBusinessCode"));
        result.put("dominantServiceId", analysis.get("dominantServiceId"));
        result.put("dominantSqlId", analysis.get("dominantSqlId"));
        result.put("cards", analysis.get("cards"));
        result.put("findings", analysis.get("findings"));
        result.put("businessOwnership", analysis.get("businessOwnership"));
        result.put("targets", collected.get("targets"));
        result.put("activeTransactions", activeTransactions);
        result.put("slowTransactions", slowTransactions);
        result.put("slowSql", slowSql);
        result.put("threads", threads);
        result.put("threadAnalysis", threadAnalysis);
        result.put("jvmAnalysis", jvmAnalysis);
        result.put("dbPoolAnalysis", dbPoolAnalysis);
        result.put("sqlAnalysis", sqlAnalysis);
        result.put("dominanceAnalysis", dominanceAnalysis);
        result.put("transactionDetailAnalysis", buildTransactionDetailAnalysis(
                slowTransactions, activeTransactions, slowSql, threads));
        result.put("causeAnalysis", buildCauseAnalysis(analysis));
        result.put("statusCardsAnalysis", buildStatusCardsAnalysis(status, analysis));
        result.put("activeTransactionListAnalysis", buildActiveTransactionListAnalysis(activeTransactions));
        result.put("businessOccupancyAnalysis", buildBusinessOccupancyAnalysis(analysis));
        result.put("incidentFlowAnalysis", buildIncidentFlowAnalysis(status, analysis, activeTransactions));
        return result;
    }

    private Map<String, Object> analyze(Map<String, Object> status, List<Map<String, Object>> activeTransactions) {
        Map<String, Object> summary = castMap(status.get("summary"));
        Map<String, Object> thread = castMap(status.get("thread"));
        Map<String, Object> jvm = castMap(status.get("jvm"));
        List<Map<String, Object>> pools = castList(status.get("dbPools"));

        String primaryCauseCode = stringValue(summary.get("primaryCauseCode"));
        if (primaryCauseCode.isEmpty()) {
            primaryCauseCode = "NORMAL";
        }
        String overallStatus = stringValue(summary.get("status"));
        if (overallStatus.isEmpty()) {
            overallStatus = "NORMAL";
        }
        String primaryMessage = stringValue(summary.get("message"));
        if (primaryMessage.isEmpty()) {
            primaryMessage = "현재 주요 병목이 없습니다.";
        }

        int totalActive = activeTransactions.size();
        Map<String, Integer> byBusiness = new HashMap<>();
        Map<String, Integer> byService = new HashMap<>();
        for (Map<String, Object> tx : activeTransactions) {
            String biz = stringValue(tx.get("businessCode"));
            String svc = stringValue(tx.get("serviceId"));
            if (!biz.isEmpty()) {
                byBusiness.merge(biz, 1, Integer::sum);
            }
            if (!svc.isEmpty()) {
                byService.merge(svc, 1, Integer::sum);
            }
        }
        String dominantBusiness = byBusiness.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("MG");
        String dominantService = byService.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (totalActive > 0 && dominantBusiness != null) {
            double ownership = byBusiness.getOrDefault(dominantBusiness, 0) * 100.0 / totalActive;
            double busy = toDouble(thread.get("busyRatio"));
            if (ownership >= 60 && busy >= 80 && isHigherPriority("BUSINESS_RESOURCE_DOMINANCE", primaryCauseCode)) {
                primaryCauseCode = "BUSINESS_RESOURCE_DOMINANCE";
                primaryMessage = String.format(Locale.ROOT,
                        "%s 업무가 현재 전체 실행 거래의 %.1f%%를 점유하고 있습니다.",
                        dominantBusiness, ownership);
                if (isWorse("WARN", overallStatus)) {
                    overallStatus = "WARN";
                }
            } else if (dominantService != null) {
                double svcOwn = byService.getOrDefault(dominantService, 0) * 100.0 / totalActive;
                if (svcOwn >= 40 && isHigherPriority("SERVICE_DOMINANCE", primaryCauseCode)) {
                    primaryCauseCode = "SERVICE_DOMINANCE";
                    primaryMessage = String.format(Locale.ROOT,
                            "%s 거래가 현재 처리량의 %.1f%%를 점유하고 있습니다.",
                            dominantService, svcOwn);
                    if (isWorse("WARN", overallStatus)) {
                        overallStatus = "WARN";
                    }
                }
            }
        }

        int dbActive = 0;
        int dbMaximum = 0;
        int dbPending = 0;
        for (Map<String, Object> pool : pools) {
            dbActive = Math.max(dbActive, (int) toLong(pool.get("active")));
            dbMaximum = Math.max(dbMaximum, (int) toLong(pool.get("maximum")));
            dbPending += (int) toLong(pool.get("pending"));
        }

        Map<String, Object> cards = new LinkedHashMap<>();
        cards.put("threadBusyRatio", thread.get("busyRatio"));
        cards.put("jvmCpuRatio", jvm.get("processCpuRatio"));
        cards.put("heapRatio", jvm.get("heapRatio"));
        cards.put("dbActive", dbActive);
        cards.put("dbMaximum", dbMaximum);
        cards.put("dbPending", dbPending);
        cards.put("slowTransactionCount", summary.getOrDefault("slowTransactionCount", 0));
        cards.put("slowSqlCount", summary.getOrDefault("slowSqlCount", 0));
        cards.put("dominantBusinessCode", dominantBusiness);
        double dominantPct = totalActive <= 0 ? 0
                : byBusiness.getOrDefault(dominantBusiness, 0) * 100.0 / totalActive;
        cards.put("dominantBusinessOwnershipPct", round1(dominantPct));

        List<Map<String, Object>> findings = new ArrayList<>();
        Map<String, Object> finding = new LinkedHashMap<>();
        finding.put("businessCode", "MG");
        finding.put("causeCode", primaryCauseCode);
        finding.put("message", primaryMessage);
        finding.put("status", overallStatus);
        findings.add(finding);

        List<Map<String, Object>> ownership = new ArrayList<>();
        for (Map.Entry<String, Integer> e : byBusiness.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("businessCode", e.getKey());
            row.put("activeCount", e.getValue());
            row.put("ownershipPct", totalActive <= 0 ? 0 : round1(e.getValue() * 100.0 / totalActive));
            ownership.add(row);
        }
        ownership.sort(Comparator.comparingDouble(r -> -toDouble(r.get("ownershipPct"))));

        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("overallStatus", overallStatus);
        analysis.put("primaryCauseCode", primaryCauseCode);
        analysis.put("primaryMessage", primaryMessage);
        analysis.put("dominantBusinessCode", dominantBusiness);
        analysis.put("dominantServiceId", dominantService);
        analysis.put("dominantSqlId", null);
        analysis.put("findings", findings);
        analysis.put("cards", cards);
        analysis.put("businessOwnership", ownership);
        return analysis;
    }

    private Map<String, Object> buildThreadAnalysis(Map<String, Object> status, Map<String, Object> analysis) {
        Map<String, Object> thread = castMap(status.get("thread"));
        Map<String, Object> cards = castMap(analysis.get("cards"));
        boolean saturated = "THREAD_SATURATION".equals(stringValue(analysis.get("primaryCauseCode")));
        Map<String, Object> saturation = new LinkedHashMap<>();
        saturation.put("detected", saturated || toDouble(thread.get("busyRatio")) >= 85);
        saturation.put("primaryCauseCode", saturated ? "THREAD_SATURATION" : "NORMAL");
        saturation.put("message", saturated
                ? stringValue(analysis.get("primaryMessage"))
                : "Thread 포화 징후 없음");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sharedTomcatPool", thread);
        out.put("warMetrics", List.of(Map.of(
                "businessCode", "MG",
                "reachable", true,
                "busyRatio", thread.get("busyRatio"),
                "activeTransactionCount", thread.get("activeTransactionCount"),
                "slowTransactionCount", thread.get("slowTransactionCount"))));
        out.put("saturation", saturation);
        out.put("aggregateBusyRatio", cards.get("threadBusyRatio"));
        out.put("aggregateSlowTransactionCount", cards.get("slowTransactionCount"));
        return out;
    }

    private Map<String, Object> buildJvmAnalysis(Map<String, Object> status, Map<String, Object> analysis) {
        Map<String, Object> jvm = castMap(status.get("jvm"));
        Map<String, Object> cards = castMap(analysis.get("cards"));
        boolean cpu = "CPU_OVERLOAD".equals(stringValue(analysis.get("primaryCauseCode")));
        boolean gc = "GC_PRESSURE".equals(stringValue(analysis.get("primaryCauseCode")));
        Map<String, Object> cpuOverload = new LinkedHashMap<>();
        cpuOverload.put("detected", cpu || toDouble(jvm.get("processCpuRatio")) >= 90);
        cpuOverload.put("primaryCauseCode", cpu ? "CPU_OVERLOAD" : "NORMAL");
        cpuOverload.put("message", cpu ? stringValue(analysis.get("primaryMessage")) : "CPU 과부하 징후 없음");
        cpuOverload.put("dbPending", cards.get("dbPending"));
        Map<String, Object> gcPressure = new LinkedHashMap<>();
        gcPressure.put("detected", gc);
        gcPressure.put("primaryCauseCode", gc ? "GC_PRESSURE" : "NORMAL");
        gcPressure.put("message", gc ? stringValue(analysis.get("primaryMessage")) : "GC 압박 징후 없음");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sharedJvm", jvm);
        out.put("cpuOverload", cpuOverload);
        out.put("gcPressure", gcPressure);
        return out;
    }

    private Map<String, Object> buildDbPoolAnalysis(
            Map<String, Object> status,
            Map<String, Object> analysis,
            List<Map<String, Object>> activeTransactions) {
        List<Map<String, Object>> pools = castList(status.get("dbPools"));
        Map<String, Object> cards = castMap(analysis.get("cards"));
        boolean exhausted = "DB_POOL_EXHAUSTED".equals(stringValue(analysis.get("primaryCauseCode")));
        Map<String, Object> exhaustion = new LinkedHashMap<>();
        exhaustion.put("detected", exhausted || toLong(cards.get("dbPending")) > 0
                && toLong(cards.get("dbActive")) >= toLong(cards.get("dbMaximum"))
                && toLong(cards.get("dbMaximum")) > 0);
        exhaustion.put("primaryCauseCode", exhausted ? "DB_POOL_EXHAUSTED" : "NORMAL");
        exhaustion.put("message", exhausted
                ? stringValue(analysis.get("primaryMessage"))
                : "DB Pool 고갈 징후 없음");
        exhaustion.put("aggregatePending", cards.get("dbPending"));

        List<Map<String, Object>> waitDb = new ArrayList<>();
        List<Map<String, Object>> execSql = new ArrayList<>();
        for (Map<String, Object> tx : activeTransactions) {
            String step = stringValue(tx.get("currentStep"));
            if ("WAIT_DB_CONNECTION".equals(step)) {
                waitDb.add(tx);
            } else if ("EXECUTING_SQL".equals(step)) {
                execSql.add(tx);
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("pools", pools);
        out.put("exhaustion", exhaustion);
        out.put("dbWaitTransactions", waitDb);
        out.put("executingSqlTransactions", execSql);
        return out;
    }

    private Map<String, Object> buildSqlAnalysis(Map<String, Object> analysis) {
        Map<String, Object> cards = castMap(analysis.get("cards"));
        boolean slow = "SLOW_SQL".equals(stringValue(analysis.get("primaryCauseCode")));
        Map<String, Object> alert = new LinkedHashMap<>();
        alert.put("detected", slow || toLong(cards.get("slowSqlCount")) >= 3);
        alert.put("primaryCauseCode", slow ? "SLOW_SQL" : "NORMAL");
        alert.put("message", slow ? stringValue(analysis.get("primaryMessage")) : "Slow SQL 징후 없음");
        alert.put("aggregateSlowSqlCount", cards.get("slowSqlCount"));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("slowSqlAlert", alert);
        out.put("statusRows", List.of());
        out.put("runningSqlCount", 0);
        return out;
    }

    private Map<String, Object> buildDominanceAnalysis(
            Map<String, Object> analysis,
            List<Map<String, Object>> activeTransactions) {
        Map<String, Object> cards = castMap(analysis.get("cards"));
        boolean biz = stringValue(analysis.get("primaryCauseCode")).startsWith("BUSINESS");
        boolean svc = "SERVICE_DOMINANCE".equals(stringValue(analysis.get("primaryCauseCode")));
        Map<String, Object> businessDominance = new LinkedHashMap<>();
        businessDominance.put("detected", biz);
        businessDominance.put("primaryCauseCode", biz ? "BUSINESS_RESOURCE_DOMINANCE" : "NORMAL");
        businessDominance.put("message", biz ? stringValue(analysis.get("primaryMessage")) : "업무 독점 징후 없음");
        businessDominance.put("screenMessage", String.format(Locale.ROOT,
                "주요 업무 %s · 점유 %.1f%% · 활성거래 %d",
                stringValue(cards.get("dominantBusinessCode")),
                toDouble(cards.get("dominantBusinessOwnershipPct")),
                activeTransactions.size()));
        Map<String, Object> serviceDominance = new LinkedHashMap<>();
        serviceDominance.put("detected", svc);
        serviceDominance.put("primaryCauseCode", svc ? "SERVICE_DOMINANCE" : "NORMAL");
        serviceDominance.put("message", svc ? stringValue(analysis.get("primaryMessage")) : "서비스 독점 징후 없음");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("businessDominance", businessDominance);
        out.put("serviceDominance", serviceDominance);
        return out;
    }

    private Map<String, Object> buildTransactionDetailAnalysis(
            List<Map<String, Object>> slowTransactions,
            List<Map<String, Object>> activeTransactions,
            List<Map<String, Object>> slowSql,
            List<Map<String, Object>> threads) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("activeTransactions", Map.of("count", activeTransactions.size(), "rows", activeTransactions));
        out.put("slowTransactions", Map.of("count", slowTransactions.size(), "rows", slowTransactions));
        out.put("slowSql", Map.of("count", slowSql.size(), "rows", slowSql));
        out.put("threads", Map.of("count", threads.size(), "rows", threads));
        return out;
    }

    private Map<String, Object> buildCauseAnalysis(Map<String, Object> analysis) {
        String primary = stringValue(analysis.get("primaryCauseCode"));
        List<Map<String, Object>> causeTable = new ArrayList<>();
        int priority = 1;
        for (String code : CAUSE_PRIORITY) {
            if ("NORMAL".equals(code)) {
                continue;
            }
            boolean detected = code.equals(primary)
                    || ("BUSINESS_RESOURCE_DOMINANCE".equals(primary) && "BUSINESS_RESOURCE_DOMINANCE".equals(code));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("priority", priority++);
            row.put("causeCode", code);
            row.put("detected", detected);
            row.put("primary", code.equals(primary)
                    || ("BUSINESS_DOMINANCE".equals(code) && primary.startsWith("BUSINESS")));
            row.put("message", detected ? stringValue(analysis.get("primaryMessage")) : "-");
            causeTable.add(row);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("priorities", CAUSE_PRIORITY);
        out.put("causeTable", causeTable);
        out.put("primaryCauseCode", primary);
        out.put("primaryMessage", analysis.get("primaryMessage"));
        out.put("overallStatus", analysis.get("overallStatus"));
        out.put("warFindings", analysis.get("findings"));
        out.put("note", "pdmg-service 단일 인스턴스 판정 · OM 14장 우선순위 축약");
        return out;
    }

    private Map<String, Object> buildStatusCardsAnalysis(Map<String, Object> status, Map<String, Object> analysis) {
        Map<String, Object> cards = castMap(analysis.get("cards"));
        Map<String, Object> thread = castMap(status.get("thread"));
        Map<String, Object> jvm = castMap(status.get("jvm"));

        int threadBusy = (int) toLong(thread.get("busy"));
        int threadMax = (int) toLong(thread.get("max"));
        double threadBusyRatio = toDouble(cards.get("threadBusyRatio"));
        double jvmCpu = toDouble(cards.get("jvmCpuRatio"));
        double heapRatio = toDouble(cards.get("heapRatio"));
        long gcTimeMs = toLong(jvm.get("gcTimeLastMinuteMs"));
        int dbActive = (int) toLong(cards.get("dbActive"));
        int dbMaximum = (int) toLong(cards.get("dbMaximum"));
        int dbPending = (int) toLong(cards.get("dbPending"));
        int slowTx = (int) toLong(cards.get("slowTransactionCount"));
        int slowSql = (int) toLong(cards.get("slowSqlCount"));
        String dominantBusiness = stringValue(cards.get("dominantBusinessCode"));
        double dominantPct = toDouble(cards.get("dominantBusinessOwnershipPct"));
        boolean gcPressure = heapRatio >= 80 && gcTimeMs >= 3000;
        boolean dbExhausted = dbMaximum > 0 && dbActive >= dbMaximum && dbPending > 0;

        String threadDisplay = threadMax > 0
                ? String.format(Locale.ROOT, "%d / %d, %.1f%%", threadBusy, threadMax, threadBusyRatio)
                : String.format(Locale.ROOT, "- / -, %.1f%%", threadBusyRatio);
        String gcDisplay = gcPressure
                ? String.format(Locale.ROOT, "압박 (%.1f초)", gcTimeMs / 1000.0)
                : "정상";
        String dbDisplay = String.format(Locale.ROOT, "%d / %d, Pending %d", dbActive, dbMaximum, dbPending);
        String businessDisplay = dominantBusiness.isEmpty()
                ? "-"
                : String.format(Locale.ROOT, "%s %.1f%% 점유", dominantBusiness, dominantPct);

        List<Map<String, Object>> cardList = new ArrayList<>();
        cardList.add(statusCard("thread", "Thread", threadDisplay, threadBusyRatio >= 85 ? "warn" : "normal"));
        cardList.add(statusCard("jvmCpu", "JVM CPU", String.format(Locale.ROOT, "%.1f%%", jvmCpu),
                jvmCpu >= 90 ? "warn" : "normal"));
        cardList.add(statusCard("heap", "Heap", String.format(Locale.ROOT, "%.1f%%", heapRatio),
                heapRatio >= 80 ? "warn" : "normal"));
        cardList.add(statusCard("gc", "GC", gcDisplay, gcPressure ? "warn" : "normal"));
        cardList.add(statusCard("dbPool", "DB Pool", dbDisplay,
                dbExhausted ? "critical" : (dbPending > 0 ? "warn" : "normal")));
        cardList.add(statusCard("slowTransaction", "Slow 거래", slowTx + "건",
                slowTx >= 5 ? "warn" : (slowTx > 0 ? "info" : "normal")));
        cardList.add(statusCard("slowSql", "Slow SQL", slowSql + "건",
                slowSql >= 3 ? "warn" : (slowSql > 0 ? "info" : "normal")));
        cardList.add(statusCard("dominantBusiness", "주요 업무", businessDisplay,
                dominantPct >= 60 ? "warn" : "normal"));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("cards", cardList);
        out.put("overallStatus", analysis.get("overallStatus"));
        out.put("primaryCauseCode", analysis.get("primaryCauseCode"));
        out.put("primaryMessage", analysis.get("primaryMessage"));
        out.put("scopeNote", "pdmg-service 로컬 JVM · Tomcat · Hikari 풀 기준");
        return out;
    }

    private Map<String, Object> buildActiveTransactionListAnalysis(List<Map<String, Object>> activeTransactions) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> tx : activeTransactions) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("businessCode", tx.get("businessCode"));
            row.put("serviceId", tx.get("serviceId"));
            row.put("elapsedMs", tx.get("elapsedMs"));
            row.put("elapsedDisplay", formatElapsed(toLong(tx.get("elapsedMs"))));
            row.put("currentStep", tx.get("currentStep"));
            row.put("stepLabel", formatStep(stringValue(tx.get("currentStep"))));
            rows.add(row);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("section", "15.4");
        out.put("title", "실행 중 거래 목록");
        out.put("count", rows.size());
        out.put("rows", rows);
        return out;
    }

    private Map<String, Object> buildBusinessOccupancyAnalysis(Map<String, Object> analysis) {
        List<Map<String, Object>> ownership = castList(analysis.get("businessOwnership"));
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> cards = castMap(analysis.get("cards"));
        for (Map<String, Object> own : ownership) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("businessCode", own.get("businessCode"));
            row.put("ownershipPct", own.get("ownershipPct"));
            row.put("dbDisplay", String.format(Locale.ROOT, "%d/%d",
                    toLong(cards.get("dbActive")), toLong(cards.get("dbMaximum"))));
            row.put("slowCount", cards.get("slowTransactionCount"));
            rows.add(row);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("rows", rows);
        return out;
    }

    private Map<String, Object> buildIncidentFlowAnalysis(
            Map<String, Object> status,
            Map<String, Object> analysis,
            List<Map<String, Object>> activeTransactions) {
        Map<String, Object> cards = castMap(analysis.get("cards"));
        String primary = stringValue(analysis.get("primaryCauseCode"));
        int dbPending = (int) toLong(cards.get("dbPending"));
        int dbActive = (int) toLong(cards.get("dbActive"));
        int dbMaximum = (int) toLong(cards.get("dbMaximum"));
        double busyRatio = toDouble(cards.get("threadBusyRatio"));
        double cpuRatio = toDouble(cards.get("jvmCpuRatio"));
        boolean poolExhausted = dbMaximum > 0 && dbActive >= dbMaximum && dbPending > 0;

        int waitDb = 0;
        int execSql = 0;
        int waitExt = 0;
        for (Map<String, Object> tx : activeTransactions) {
            String step = stringValue(tx.get("currentStep"));
            if ("WAIT_DB_CONNECTION".equals(step)) {
                waitDb++;
            } else if ("EXECUTING_SQL".equals(step)) {
                execSql++;
            } else if ("WAIT_EXTERNAL".equals(step)) {
                waitExt++;
            }
        }

        List<Map<String, Object>> flows = new ArrayList<>();
        flows.add(flow(
                "dbPool", "19.1", "DB Pool 장애", "DB_POOL_EXHAUSTED",
                poolExhausted || dbPending > 0 || "DB_POOL_EXHAUSTED".equals(primary),
                "DB_POOL_EXHAUSTED".equals(primary),
                List.of(
                        flowStep(1, "Connection 부족", poolExhausted,
                                poolExhausted ? dbActive + "/" + dbMaximum : "Pool 여유"),
                        flowStep(2, "WAIT_DB_CONNECTION 증가", waitDb > 0, waitDb + "건"),
                        flowStep(3, "DB Pending 증가", dbPending > 0, "Pending " + dbPending),
                        flowStep(4, "Tomcat Busy Thread 증가", busyRatio >= 70,
                                "Busy " + round1(busyRatio) + "%"),
                        flowStep(5, "pdmg 판정: DB_POOL_EXHAUSTED",
                                "DB_POOL_EXHAUSTED".equals(primary),
                                stringValue(analysis.get("primaryMessage")))),
                List.of(
                        flowLink("진단 가이드", "/index.html#/rtdiag"),
                        flowLink("전문 테스트", "/index.html#/mgcoa9100"))));

        flows.add(flow(
                "slowSql", "19.2", "Slow SQL", "SLOW_SQL",
                toLong(cards.get("slowSqlCount")) >= 3 || execSql > 0 || "SLOW_SQL".equals(primary),
                "SLOW_SQL".equals(primary),
                List.of(
                        flowStep(1, "Connection 정상 획득", !poolExhausted && dbPending == 0,
                                "Pending " + dbPending),
                        flowStep(2, "EXECUTING_SQL 장시간", execSql > 0, execSql + "건"),
                        flowStep(3, "동일 Mapper 반복", false, "-"),
                        flowStep(4, "Pool Active 증가",
                                dbMaximum > 0 && dbActive * 100 / dbMaximum >= 70,
                                dbActive + "/" + dbMaximum),
                        flowStep(5, "pdmg 판정: SLOW_SQL", "SLOW_SQL".equals(primary),
                                stringValue(analysis.get("primaryMessage")))),
                List.of(flowLink("진단 가이드", "/index.html#/rtdiag"))));

        flows.add(flow(
                "externalWait", "19.3", "외부 시스템 지연", "EXTERNAL_WAIT",
                waitExt > 0 || "EXTERNAL_WAIT".equals(primary),
                "EXTERNAL_WAIT".equals(primary),
                List.of(
                        flowStep(1, "DB·CPU 정상", dbPending == 0 && cpuRatio < 90,
                                "Pending " + dbPending + " · CPU " + round1(cpuRatio) + "%"),
                        flowStep(2, "WAIT_EXTERNAL 증가", waitExt >= 1, waitExt + "건"),
                        flowStep(3, "외부 시스템 집중", waitExt >= 3, "-"),
                        flowStep(4, "pdmg 판정: EXTERNAL_WAIT", "EXTERNAL_WAIT".equals(primary),
                                stringValue(analysis.get("primaryMessage")))),
                List.of(flowLink("진단 가이드", "/index.html#/rtdiag"))));

        flows.add(flow(
                "cpuOverload", "19.4", "CPU 과부하", "CPU_OVERLOAD",
                cpuRatio >= 90 || "CPU_OVERLOAD".equals(primary),
                "CPU_OVERLOAD".equals(primary),
                List.of(
                        flowStep(1, "DB Pending·외부대기 없음", dbPending == 0 && waitExt == 0,
                                "Pending " + dbPending),
                        flowStep(2, "CPU 90% 이상", cpuRatio >= 90, round1(cpuRatio) + "%"),
                        flowStep(3, "ServiceId CPU 집중", false, "-"),
                        flowStep(4, "pdmg 판정: CPU_OVERLOAD", "CPU_OVERLOAD".equals(primary),
                                stringValue(analysis.get("primaryMessage")))),
                List.of(flowLink("진단 가이드", "/index.html#/rtdiag"))));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("section", "19");
        out.put("title", "장애 흐름");
        out.put("primaryCauseCode", primary);
        out.put("flows", flows);
        out.put("scopeNote", "pdmg-service 로컬 지표 기준 · PRIMARY는 mgcoa9100S0 판정");
        return out;
    }

    private static Map<String, Object> statusCard(String id, String label, String display, String level) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("id", id);
        card.put("label", label);
        card.put("display", display);
        card.put("level", level);
        return card;
    }

    private static Map<String, Object> flow(
            String id, String section, String title, String causeCode,
            boolean active, boolean primary,
            List<Map<String, Object>> steps, List<Map<String, Object>> links) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("section", section);
        row.put("title", title);
        row.put("causeCode", causeCode);
        row.put("active", active);
        row.put("primary", primary);
        row.put("steps", steps);
        row.put("links", links);
        return row;
    }

    private static Map<String, Object> flowStep(int order, String label, boolean active, String detail) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("order", order);
        step.put("label", label);
        step.put("active", active);
        step.put("detail", detail);
        return step;
    }

    private static Map<String, Object> flowLink(String label, String href) {
        return Map.of("label", label, "href", href);
    }

    private static String formatElapsed(long ms) {
        if (ms < 1000) {
            return ms + "ms";
        }
        return String.format(Locale.ROOT, "%.1fs", ms / 1000.0);
    }

    private static String formatStep(String step) {
        if (step == null || step.isBlank()) {
            return "-";
        }
        return switch (step) {
            case "WAIT_DB_CONNECTION" -> "DB 대기";
            case "EXECUTING_SQL" -> "SQL 실행";
            case "WAIT_EXTERNAL" -> "외부 대기";
            case "RUNNING" -> "실행 중";
            default -> step;
        };
    }

    private static boolean isHigherPriority(String candidate, String current) {
        return CAUSE_PRIORITY.indexOf(candidate) < CAUSE_PRIORITY.indexOf(current);
    }

    private static boolean isWorse(String candidate, String current) {
        return severity(candidate) > severity(current);
    }

    private static int severity(String status) {
        return switch (stringValue(status)) {
            case "CRITICAL" -> 3;
            case "WARN" -> 2;
            case "UNKNOWN" -> 1;
            default -> 0;
        };
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castList(Object value) {
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

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private static long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
