package com.nh.nsight.marketing.om.application.service;

import com.nh.nsight.marketing.om.application.rule.OmOperationRule;
import com.nh.nsight.marketing.om.support.OmBodySupport;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OmRuntimeService {
    private final OmOperationRule rule;
    private final RuntimeStatusCollector collector;
    private final RuntimeCauseAnalyzer analyzer;

    public OmRuntimeService(
            OmOperationRule rule,
            RuntimeStatusCollector collector,
            RuntimeCauseAnalyzer analyzer) {
        this.rule = rule;
        this.collector = collector;
        this.analyzer = analyzer;
    }

    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        boolean includeDetails = "Y".equalsIgnoreCase(OmBodySupport.stringValue(body, "includeDetails"));

        Map<String, Object> collected = collector.collectAll();
        Map<String, Object> analysis = analyzer.analyze(collected);
        List<Map<String, Object>> activeTransactions = includeDetails
                ? collector.collectActiveTransactions() : List.of();
        List<Map<String, Object>> slowTransactions = includeDetails
                ? collector.collectSlowTransactions(50) : List.of();
        List<Map<String, Object>> slowSql = includeDetails
                ? collector.collectSlowSql(50) : List.of();
        List<Map<String, Object>> threads = includeDetails
                ? collector.collectThreads() : List.of();
        List<Map<String, Object>> serviceCpuUsage = includeDetails
                ? collector.extractServiceCpuUsage(collected, 30) : List.of();

        Map<String, Object> threadAnalysis = buildThreadAnalysis(collected, analysis);
        Map<String, Object> jvmAnalysis = buildJvmAnalysis(collected, analysis, serviceCpuUsage);
        Map<String, Object> dbPoolAnalysis = buildDbPoolAnalysis(collected, analysis, activeTransactions);
        Map<String, Object> sqlAnalysis = buildSqlAnalysis(collected, analysis, slowSql, activeTransactions);
        Map<String, Object> dominanceAnalysis = buildDominanceAnalysis(
                collected, analysis, activeTransactions, serviceCpuUsage);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
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
        result.put("serviceCpuUsage", serviceCpuUsage);
        result.put("threadAnalysis", threadAnalysis);
        result.put("jvmAnalysis", jvmAnalysis);
        result.put("dbPoolAnalysis", dbPoolAnalysis);
        result.put("sqlAnalysis", sqlAnalysis);
        result.put("dominanceAnalysis", dominanceAnalysis);
        result.put("transactionDetailAnalysis", buildTransactionDetailAnalysis(
                slowTransactions, activeTransactions, slowSql, threads));
        result.put("causeAnalysis", buildCauseAnalysis(collected, analysis, activeTransactions));
        Map<String, Object> causeAnalysis = castMap(result.get("causeAnalysis"));
        result.put("statusCardsAnalysis", buildStatusCardsAnalysis(collected, analysis));
        result.put("activeTransactionListAnalysis", buildActiveTransactionListAnalysis(activeTransactions));
        result.put("businessOccupancyAnalysis", buildBusinessOccupancyAnalysis(collected, analysis));
        Map<String, Object> incidentFlowAnalysis = buildIncidentFlowAnalysis(
                collected, analysis, activeTransactions, slowSql, serviceCpuUsage);
        result.put("incidentFlowAnalysis", incidentFlowAnalysis);
        result.put("integratedDashboard", buildIntegratedDashboard(
                collected,
                analysis,
                activeTransactions,
                slowTransactions,
                threadAnalysis,
                jvmAnalysis,
                dbPoolAnalysis,
                dominanceAnalysis));
        result.put("instanceDetail", buildInstanceDetailAnalysis(
                collected, analysis, threads, serviceCpuUsage, dominanceAnalysis));
        result.put("warResourceDetail", buildWarResourceDetailAnalysis(
                collected,
                analysis,
                activeTransactions,
                slowTransactions,
                slowSql,
                dbPoolAnalysis,
                dominanceAnalysis));
        result.put("activeTransactionScreen", buildActiveTransactionScreenAnalysis(
                collected, activeTransactions, slowTransactions, threads));
        result.put("transactionTraceScreen", buildTransactionTraceScreenAnalysis(
                collected, activeTransactions, threads, body));
        result.put("sqlExternalScreen", buildSqlExternalScreenAnalysis(
                collected, slowSql, activeTransactions, body));
        result.put("incidentReportScreen", buildIncidentReportScreenAnalysis(
                collected,
                analysis,
                causeAnalysis,
                activeTransactions,
                slowTransactions,
                slowSql,
                dbPoolAnalysis));
        result.put("thresholdPolicyScreen", buildThresholdPolicyScreenAnalysis(
                collected, sqlAnalysis));
        result.put("causeTracingScreen", buildCauseTracingScreenAnalysis(
                collected,
                analysis,
                causeAnalysis,
                activeTransactions,
                slowSql,
                dbPoolAnalysis,
                threadAnalysis,
                jvmAnalysis,
                incidentFlowAnalysis,
                body));
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildThreadAnalysis(Map<String, Object> collected, Map<String, Object> analysis) {
        Map<String, Object> threadAnalysis = new LinkedHashMap<>();
        List<Map<String, Object>> warRows = new ArrayList<>();
        Map<String, Object> sharedPool = null;
        boolean anySaturation = false;

        Object targetRows = collected.get("targets");
        if (targetRows instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> targetRow)) {
                    continue;
                }
                Map<String, Object> target = (Map<String, Object>) targetRow;
                String businessCode = stringValue(target.get("businessCode"));
                boolean reachable = !Boolean.FALSE.equals(target.get("reachable"));
                Map<String, Object> status = castMap(target.get("status"));
                Map<String, Object> threadMetrics = castMap(status.get("thread"));
                Map<String, Object> summary = castMap(status.get("summary"));

                if (sharedPool == null && reachable && !threadMetrics.isEmpty()) {
                    sharedPool = extractSharedPool(threadMetrics);
                }

                double busyRatio = toDouble(threadMetrics.get("busyRatio"));
                int slowTx = (int) toLong(threadMetrics.get("slowTransactionCount"));
                boolean saturated = busyRatio >= 85 && slowTx >= 5;
                anySaturation = anySaturation || saturated;

                Map<String, Object> warRow = new LinkedHashMap<>();
                warRow.put("businessCode", businessCode);
                warRow.put("reachable", reachable);
                warRow.put("activeTransactionCount", threadMetrics.getOrDefault("activeTransactionCount", 0));
                warRow.put("slowTransactionCount", slowTx);
                warRow.put("busyRatio", busyRatio);
                warRow.put("threadSaturation", saturated);
                warRow.put("causeCode", summary.get("primaryCauseCode"));
                warRow.put("status", summary.get("status"));
                warRows.add(warRow);
            }
        }

        Map<String, Object> cards = castMap(analysis.get("cards"));
        Map<String, Object> saturation = new LinkedHashMap<>();
        boolean primarySaturation = "THREAD_SATURATION".equals(stringValue(analysis.get("primaryCauseCode")));
        saturation.put("detected", anySaturation || primarySaturation);
        saturation.put("primaryCauseCode", primarySaturation ? "THREAD_SATURATION" : (anySaturation ? "THREAD_SATURATION" : "NORMAL"));
        saturation.put("message", primarySaturation
                ? stringValue(analysis.get("primaryMessage"))
                : (anySaturation
                ? "Tomcat Thread 부족 또는 장기 거래 점유 상태입니다."
                : "Thread 포화 징후 없음"));
        saturation.put("criteria", "busyRatio >= 85 AND slowTransactionCount >= 5 (최근 1분)");
        saturation.put("note", "maxThreads 부족 단정 전 DB Pool·JVM 상태를 함께 확인하세요.");

        threadAnalysis.put("sharedTomcatPool", sharedPool == null ? Map.of() : sharedPool);
        threadAnalysis.put("warMetrics", warRows);
        threadAnalysis.put("saturation", saturation);
        threadAnalysis.put("aggregateBusyRatio", cards.get("threadBusyRatio"));
        threadAnalysis.put("aggregateSlowTransactionCount", cards.get("slowTransactionCount"));
        return threadAnalysis;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildJvmAnalysis(
            Map<String, Object> collected,
            Map<String, Object> analysis,
            List<Map<String, Object>> serviceCpuUsage) {
        Map<String, Object> jvmAnalysis = new LinkedHashMap<>();
        Map<String, Object> sharedJvm = null;

        Object targetRows = collected.get("targets");
        if (targetRows instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> targetRow)) {
                    continue;
                }
                Map<String, Object> target = (Map<String, Object>) targetRow;
                boolean reachable = !Boolean.FALSE.equals(target.get("reachable"));
                Map<String, Object> status = castMap(target.get("status"));
                Map<String, Object> jvm = castMap(status.get("jvm"));
                if (sharedJvm == null && reachable && !jvm.isEmpty()) {
                    sharedJvm = new LinkedHashMap<>(jvm);
                    sharedJvm.put("scope", "tomcat-shared");
                }
            }
        }
        if (sharedJvm == null) {
            sharedJvm = Map.of();
        }

        Map<String, Object> cards = castMap(analysis.get("cards"));
        int dbPending = (int) toLong(cards.get("dbPending"));
        double processCpu = toDouble(sharedJvm.get("processCpuRatio"));
        double heapRatio = toDouble(sharedJvm.get("heapRatio"));
        long gcTimeMs = toLong(sharedJvm.get("gcTimeLastMinuteMs"));

        boolean cpuOverload = processCpu >= 90 && dbPending == 0;
        boolean gcPressure = heapRatio >= 80 && gcTimeMs >= 3000;
        boolean primaryCpu = "CPU_OVERLOAD".equals(stringValue(analysis.get("primaryCauseCode")));
        boolean primaryGc = "GC_PRESSURE".equals(stringValue(analysis.get("primaryCauseCode")));

        Map<String, Object> cpu = new LinkedHashMap<>();
        cpu.put("detected", cpuOverload || primaryCpu);
        cpu.put("primaryCauseCode", primaryCpu ? "CPU_OVERLOAD" : (cpuOverload ? "CPU_OVERLOAD" : "NORMAL"));
        cpu.put("message", primaryCpu
                ? stringValue(analysis.get("primaryMessage"))
                : (cpuOverload
                ? "JVM CPU가 과부하 상태입니다. 업무 연산 또는 과도한 Thread 실행 가능"
                : "JVM CPU 정상 범위"));
        cpu.put("criteria", "processCpuRatio >= 90 AND dbPending = 0");
        cpu.put("processCpuRatio", processCpu);
        cpu.put("dbPending", dbPending);
        cpu.put("note", "동일 JVM에서 WAR별 Process CPU는 분리할 수 없습니다. ServiceId Thread CPU로 영향 업무를 추정하세요.");

        Map<String, Object> gc = new LinkedHashMap<>();
        gc.put("detected", gcPressure || primaryGc);
        gc.put("primaryCauseCode", primaryGc ? "GC_PRESSURE" : (gcPressure ? "GC_PRESSURE" : "NORMAL"));
        gc.put("message", primaryGc
                ? stringValue(analysis.get("primaryMessage"))
                : (gcPressure
                ? "Heap 압박으로 거래 지연 가능"
                : "GC 압박 징후 없음"));
        gc.put("criteria", "heapRatio >= 80 AND gcTimeLastMinuteMs >= 3000");
        gc.put("heapRatio", heapRatio);
        gc.put("gcTimeLastMinuteMs", gcTimeMs);
        gc.put("screenMessage", String.format(
                "JVM Heap 사용률 %s%%%n최근 1분 GC 소요시간 %s초%n모든 WAR의 응답 지연 가능성이 있습니다.",
                format1(heapRatio),
                format1(gcTimeMs / 1000.0)));

        jvmAnalysis.put("sharedJvm", sharedJvm);
        jvmAnalysis.put("cpuOverload", cpu);
        jvmAnalysis.put("gcPressure", gc);
        jvmAnalysis.put("serviceCpuUsage", serviceCpuUsage);
        jvmAnalysis.put("aggregateDbPending", dbPending);
        return jvmAnalysis;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildDbPoolAnalysis(
            Map<String, Object> collected,
            Map<String, Object> analysis,
            List<Map<String, Object>> activeTransactions) {
        Map<String, Object> dbPoolAnalysis = new LinkedHashMap<>();
        List<Map<String, Object>> warPools = new ArrayList<>();
        boolean anyExhausted = false;
        int aggregatePending = 0;
        String exhaustionScreenMessage = null;

        Object targetRows = collected.get("targets");
        if (targetRows instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> targetRow)) {
                    continue;
                }
                Map<String, Object> target = (Map<String, Object>) targetRow;
                String businessCode = stringValue(target.get("businessCode"));
                boolean reachable = !Boolean.FALSE.equals(target.get("reachable"));
                Map<String, Object> status = castMap(target.get("status"));

                for (Map<String, Object> pool : castList(status.get("dbPools"))) {
                    int maximum = (int) toLong(pool.get("maximum"));
                    int active = (int) toLong(pool.get("active"));
                    int idle = (int) toLong(pool.get("idle"));
                    int pending = (int) toLong(pool.get("pending"));
                    boolean exhausted = maximum > 0 && active >= maximum && idle == 0 && pending > 0;
                    anyExhausted = anyExhausted || exhausted;
                    aggregatePending += pending;

                    Map<String, Object> warRow = new LinkedHashMap<>(pool);
                    warRow.put("businessCode", businessCode);
                    warRow.put("reachable", reachable);
                    warRow.put("exhausted", exhausted);
                    warPools.add(warRow);

                    if (exhausted && exhaustionScreenMessage == null) {
                        exhaustionScreenMessage = buildExhaustionScreenMessage(businessCode, pool, pending);
                    }
                }
            }
        }

        boolean primaryExhausted = "DB_POOL_EXHAUSTED".equals(stringValue(analysis.get("primaryCauseCode")));

        Map<String, Object> exhaustion = new LinkedHashMap<>();
        exhaustion.put("detected", anyExhausted || primaryExhausted);
        exhaustion.put("primaryCauseCode", primaryExhausted || anyExhausted ? "DB_POOL_EXHAUSTED" : "NORMAL");
        exhaustion.put("message", primaryExhausted
                ? stringValue(analysis.get("primaryMessage"))
                : (anyExhausted
                ? "DB Connection을 얻지 못해 거래가 대기 중입니다."
                : "DB Pool 포화 징후 없음"));
        exhaustion.put("criteria", "active = maximum AND idle = 0 AND pending > 0");
        exhaustion.put("screenMessage", exhaustionScreenMessage != null
                ? exhaustionScreenMessage
                : "DB Pool 포화 기준 미충족");
        exhaustion.put("aggregatePending", aggregatePending);

        List<Map<String, Object>> dbWaitRows = new ArrayList<>();
        List<Map<String, Object>> sqlWaitRows = new ArrayList<>();
        for (Map<String, Object> tx : activeTransactions) {
            String step = stringValue(tx.get("currentStep"));
            if ("WAIT_DB_CONNECTION".equals(step)) {
                dbWaitRows.add(tx);
            } else if ("EXECUTING_SQL".equals(step)) {
                sqlWaitRows.add(tx);
            }
        }

        dbPoolAnalysis.put("pools", warPools);
        dbPoolAnalysis.put("exhaustion", exhaustion);
        dbPoolAnalysis.put("dbWaitTransactions", dbWaitRows);
        dbPoolAnalysis.put("executingSqlTransactions", sqlWaitRows);
        return dbPoolAnalysis;
    }

    private Map<String, Object> buildSqlAnalysis(
            Map<String, Object> collected,
            Map<String, Object> analysis,
            List<Map<String, Object>> slowSql,
            List<Map<String, Object>> activeTransactions) {
        Map<String, Object> sqlAnalysis = new LinkedHashMap<>();
        List<Map<String, Object>> statusRows = new ArrayList<>();
        int runningCount = 0;
        long configuredThresholdMs = 1000L;

        Object targetRows = collected.get("targets");
        if (targetRows instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> targetRow)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> target = (Map<String, Object>) targetRow;
                Map<String, Object> status = castMap(target.get("status"));
                Map<String, Object> runtimeConfig = castMap(status.get("runtimeConfig"));
                if (runtimeConfig.containsKey("slowSqlThresholdMs")) {
                    configuredThresholdMs = toLong(runtimeConfig.get("slowSqlThresholdMs"));
                    break;
                }
            }
        }

        for (Map<String, Object> tx : activeTransactions) {
            if (!"EXECUTING_SQL".equals(stringValue(tx.get("currentStep")))) {
                continue;
            }
            String mapperSql = stringValue(tx.get("mapperSql"));
            if (mapperSql == null || mapperSql.isBlank() || "-".equals(mapperSql)) {
                mapperSql = formatMapperSqlDisplay(stringValue(tx.get("sqlId")), null);
            }
            if (mapperSql == null || mapperSql.isBlank() || "-".equals(mapperSql)) {
                continue;
            }
            runningCount++;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("businessCode", tx.get("businessCode"));
            row.put("serviceId", tx.get("serviceId"));
            row.put("mapperSql", mapperSql);
            row.put("status", "RUNNING");
            row.put("elapsedMs", tx.get("elapsedMs"));
            statusRows.add(row);
        }

        for (Map<String, Object> sql : slowSql) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("businessCode", sql.get("businessCode"));
            row.put("serviceId", sql.get("serviceId"));
            row.put("mapperSql", sql.containsKey("mapperSql")
                    ? sql.get("mapperSql")
                    : formatMapperSqlDisplay(stringValue(sql.get("mapperId")), stringValue(sql.get("sqlId"))));
            row.put("status", Boolean.FALSE.equals(sql.get("success")) ? "FAILED" : "COMPLETED");
            row.put("elapsedMs", sql.get("elapsedMs"));
            row.put("affectedRows", sql.get("affectedRows"));
            statusRows.add(row);
        }

        statusRows.sort((a, b) -> {
            int rankA = statusRank(stringValue(a.get("status")));
            int rankB = statusRank(stringValue(b.get("status")));
            if (rankA != rankB) {
                return Integer.compare(rankA, rankB);
            }
            return Long.compare(toLong(b.get("elapsedMs")), toLong(a.get("elapsedMs")));
        });

        Map<String, Object> cards = castMap(analysis.get("cards"));
        int aggregateSlowSql = (int) toLong(cards.get("slowSqlCount"));
        boolean primarySlow = "SLOW_SQL".equals(stringValue(analysis.get("primaryCauseCode")));
        boolean slowDetected = primarySlow || aggregateSlowSql >= 3;

        Map<String, Object> slowSqlAlert = new LinkedHashMap<>();
        slowSqlAlert.put("detected", slowDetected);
        slowSqlAlert.put("primaryCauseCode", primarySlow ? "SLOW_SQL" : (aggregateSlowSql >= 3 ? "SLOW_SQL" : "NORMAL"));
        slowSqlAlert.put("message", primarySlow
                ? stringValue(analysis.get("primaryMessage"))
                : (aggregateSlowSql >= 3
                ? "특정 Mapper SQL이 장시간 실행 중입니다."
                : "Slow SQL 징후 없음"));
        slowSqlAlert.put("criteria", String.format(
                "최근 1분 Slow SQL >= 3건 (현재 %d건) · threshold %dms",
                aggregateSlowSql,
                configuredThresholdMs));
        slowSqlAlert.put("aggregateSlowSqlCount", aggregateSlowSql);

        List<Map<String, Object>> thresholds = List.of(
                thresholdRow("단건 조회", 1000, configuredThresholdMs),
                thresholdRow("목록 조회", 2000, configuredThresholdMs),
                thresholdRow("등록·변경", 2000, configuredThresholdMs),
                thresholdRow("대량 처리", null, configuredThresholdMs));

        sqlAnalysis.put("thresholds", thresholds);
        sqlAnalysis.put("configuredThresholdMs", configuredThresholdMs);
        sqlAnalysis.put("thresholdNote", "1차: runtime-slow-sql-threshold-ms 공통 적용 · 2차: Service Catalog별 분리 예정");
        sqlAnalysis.put("slowSqlAlert", slowSqlAlert);
        sqlAnalysis.put("statusRows", statusRows);
        sqlAnalysis.put("runningSqlCount", runningCount);
        sqlAnalysis.put("completedSlowSql", slowSql);
        return sqlAnalysis;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildDominanceAnalysis(
            Map<String, Object> collected,
            Map<String, Object> analysis,
            List<Map<String, Object>> activeTransactions,
            List<Map<String, Object>> serviceCpuUsage) {
        Map<String, Object> dominanceAnalysis = new LinkedHashMap<>();
        Map<String, Map<String, Object>> businessIndex = new LinkedHashMap<>();
        Map<String, Integer> externalWaitByBusiness = new HashMap<>();
        Map<String, ServiceElapsedStats> serviceStats = new HashMap<>();
        Map<String, Double> cpuShareByBusiness = new HashMap<>();

        Map<String, Object> cards = castMap(analysis.get("cards"));
        double aggregateBusyRatio = toDouble(cards.get("threadBusyRatio"));
        int totalActive = 0;

        Object targetRows = collected.get("targets");
        if (targetRows instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> targetRow)) {
                    continue;
                }
                Map<String, Object> target = (Map<String, Object>) targetRow;
                String businessCode = stringValue(target.get("businessCode"));
                Map<String, Object> status = castMap(target.get("status"));
                Map<String, Object> summary = castMap(status.get("summary"));
                Map<String, Object> thread = castMap(status.get("thread"));

                int activeCount = (int) toLong(summary.get("activeTransactionCount"));
                totalActive += activeCount;

                Map<String, Object> metrics = businessIndex.computeIfAbsent(businessCode, code -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("businessCode", code);
                    return row;
                });
                metrics.put("activeTransactions", activeCount);
                metrics.put("slowTransactionCount", toLong(summary.get("slowTransactionCount")));
                metrics.put("slowSqlCount", toLong(summary.get("slowSqlCount")));
                metrics.put("sharedThreadBusyRatio", thread.get("busyRatio"));

                int dbActive = 0;
                int dbPending = 0;
                for (Map<String, Object> pool : castList(status.get("dbPools"))) {
                    dbActive += (int) toLong(pool.get("active"));
                    dbPending += (int) toLong(pool.get("pending"));
                }
                metrics.put("dbActive", dbActive);
                metrics.put("dbPending", dbPending);
            }
        }

        for (Map<String, Object> tx : activeTransactions) {
            String businessCode = stringValue(tx.get("businessCode"));
            if (businessCode != null && "WAIT_EXTERNAL".equals(stringValue(tx.get("currentStep")))) {
                externalWaitByBusiness.merge(businessCode, 1, Integer::sum);
            }
            String serviceId = stringValue(tx.get("serviceId"));
            if (serviceId == null) {
                continue;
            }
            ServiceElapsedStats stats = serviceStats.computeIfAbsent(serviceId, key -> new ServiceElapsedStats());
            stats.businessCode = businessCode;
            stats.count++;
            long elapsed = toLong(tx.get("elapsedMs"));
            stats.sumElapsed += elapsed;
            stats.maxElapsed = Math.max(stats.maxElapsed, elapsed);
        }

        for (Map<String, Object> cpu : serviceCpuUsage) {
            String businessCode = stringValue(cpu.get("businessCode"));
            if (businessCode != null) {
                cpuShareByBusiness.merge(businessCode, toDouble(cpu.get("cpuSharePct")), Double::sum);
            }
        }

        List<Map<String, Object>> businessMetrics = new ArrayList<>();
        for (Map<String, Object> metrics : businessIndex.values()) {
            String businessCode = stringValue(metrics.get("businessCode"));
            int activeCount = (int) toLong(metrics.get("activeTransactions"));
            double ownershipPct = totalActive > 0 ? round1(activeCount * 100.0 / totalActive) : 0;
            metrics.put("ownershipPct", ownershipPct);
            metrics.put("busyThreadOccupancy", activeCount);
            metrics.put("externalWaitCount", externalWaitByBusiness.getOrDefault(businessCode, 0));
            metrics.put("timeoutCount", null);
            metrics.put("threadCpuSharePct", round1(cpuShareByBusiness.getOrDefault(businessCode, 0.0)));

            ServiceElapsedStats businessElapsed = aggregateElapsedForBusiness(businessCode, serviceStats);
            metrics.put("avgElapsedMs", businessElapsed.count > 0
                    ? Math.round(businessElapsed.sumElapsed / businessElapsed.count) : 0);
            metrics.put("maxElapsedMs", businessElapsed.maxElapsed);
            businessMetrics.add(metrics);
        }
        businessMetrics.sort(Comparator.comparingDouble(
                (Map<String, Object> row) -> toDouble(row.get("ownershipPct"))).reversed());

        List<Map<String, Object>> serviceOwnership = new ArrayList<>();
        for (Map.Entry<String, ServiceElapsedStats> entry : serviceStats.entrySet()) {
            ServiceElapsedStats stats = entry.getValue();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("serviceId", entry.getKey());
            row.put("businessCode", stats.businessCode);
            row.put("activeCount", stats.count);
            row.put("ownershipPct", totalActive > 0 ? round1(stats.count * 100.0 / totalActive) : 0);
            row.put("avgElapsedMs", stats.count > 0 ? Math.round(stats.sumElapsed / stats.count) : 0);
            row.put("maxElapsedMs", stats.maxElapsed);
            serviceOwnership.add(row);
        }
        serviceOwnership.sort(Comparator.comparingDouble(
                (Map<String, Object> row) -> toDouble(row.get("ownershipPct"))).reversed());

        String dominantBusiness = stringValue(analysis.get("dominantBusinessCode"));
        double dominantBusinessPct = toDouble(cards.get("dominantBusinessOwnershipPct"));
        boolean businessCondition = dominantBusinessPct >= 60 && aggregateBusyRatio >= 80;
        String primaryCause = stringValue(analysis.get("primaryCauseCode"));
        boolean primaryBusiness = "BUSINESS_RESOURCE_DOMINANCE".equals(primaryCause)
                || "BUSINESS_DOMINANCE".equals(primaryCause);

        Map<String, Object> businessDominance = new LinkedHashMap<>();
        businessDominance.put("detected", businessCondition || primaryBusiness);
        businessDominance.put("primaryCauseCode", primaryBusiness || businessCondition
                ? "BUSINESS_RESOURCE_DOMINANCE" : "NORMAL");
        businessDominance.put("message", primaryBusiness
                ? stringValue(analysis.get("primaryMessage"))
                : (businessCondition
                ? String.format(
                        "%s 업무가 현재 전체 실행 거래의 %s%%를 점유하고 있습니다.",
                        dominantBusiness,
                        format1(dominantBusinessPct))
                : "업무 자원 독점 징후 없음"));
        businessDominance.put("criteria", "ownershipPct >= 60 AND threadBusyRatio >= 80");
        businessDominance.put("dominantBusinessCode", dominantBusiness);
        businessDominance.put("ownershipPct", dominantBusinessPct);
        businessDominance.put("threadBusyRatio", aggregateBusyRatio);
        businessDominance.put("totalActiveTransactions", totalActive);
        int dominantBusinessActive = 0;
        if (dominantBusiness != null) {
            Map<String, Object> dominantMetrics = businessIndex.get(dominantBusiness);
            if (dominantMetrics != null) {
                dominantBusinessActive = (int) toLong(dominantMetrics.get("activeTransactions"));
            }
        }
        businessDominance.put("screenMessage", businessCondition || primaryBusiness
                ? String.format(
                        "전체 실행 거래: %d건%n %s 실행 거래: %d건%n %s Thread 점유율: %s%%%nTomcat Busy Thread Ratio: %s%%",
                        totalActive,
                        dominantBusiness,
                        dominantBusinessActive,
                        dominantBusiness,
                        format1(dominantBusinessPct),
                        format1(aggregateBusyRatio))
                : "업무 독점 기준 미충족");

        String dominantService = stringValue(analysis.get("dominantServiceId"));
        double dominantServicePct = 0;
        int dominantServiceCount = 0;
        for (Map<String, Object> row : serviceOwnership) {
            if (dominantService != null && dominantService.equals(stringValue(row.get("serviceId")))) {
                dominantServicePct = toDouble(row.get("ownershipPct"));
                dominantServiceCount = (int) toLong(row.get("activeCount"));
                break;
            }
        }
        boolean serviceCondition = dominantServicePct >= 40;
        boolean primaryService = "SERVICE_DOMINANCE".equals(primaryCause);

        Map<String, Object> serviceDominance = new LinkedHashMap<>();
        serviceDominance.put("detected", serviceCondition || primaryService);
        serviceDominance.put("primaryCauseCode", primaryService || serviceCondition ? "SERVICE_DOMINANCE" : "NORMAL");
        serviceDominance.put("message", primaryService
                ? stringValue(analysis.get("primaryMessage"))
                : (serviceCondition
                ? String.format(
                        "%s 거래가 현재 Tomcat 처리량의 %s%%를 점유하고 있습니다.",
                        dominantService,
                        format1(dominantServicePct))
                : "ServiceId 독점 징후 없음"));
        serviceDominance.put("criteria", "serviceId activeCount / totalActive >= 40%");
        serviceDominance.put("dominantServiceId", dominantService);
        serviceDominance.put("ownershipPct", dominantServicePct);
        serviceDominance.put("activeCount", dominantServiceCount);
        serviceDominance.put("totalActiveTransactions", totalActive);
        serviceDominance.put("screenMessage", serviceCondition || primaryService
                ? String.format(
                        "%s%n 실행 중: %d건%n 전체 거래: %d건%n 점유율: %s%%",
                        dominantService,
                        dominantServiceCount,
                        totalActive,
                        format1(dominantServicePct))
                : "ServiceId 독점 기준 미충족");

        dominanceAnalysis.put("businessMetrics", businessMetrics);
        dominanceAnalysis.put("serviceOwnership", serviceOwnership);
        dominanceAnalysis.put("businessDominance", businessDominance);
        dominanceAnalysis.put("serviceDominance", serviceDominance);
        dominanceAnalysis.put("aggregateBusyRatio", aggregateBusyRatio);
        dominanceAnalysis.put("totalActiveTransactions", totalActive);
        dominanceAnalysis.put("scopeNote",
                "동일 JVM — WAR별 Heap 분리 불가 · Active Transaction·Thread·DB·SQL 지표로 독점 추정");
        return dominanceAnalysis;
    }

    private Map<String, Object> buildTransactionDetailAnalysis(
            List<Map<String, Object>> slowTransactions,
            List<Map<String, Object>> activeTransactions,
            List<Map<String, Object>> slowSql,
            List<Map<String, Object>> threads) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("slowTransactions", detailSection(
                "12.2",
                "GET /internal/runtime/slow-transactions?limit=50",
                "최근 Slow 거래",
                slowTransactions));
        detail.put("activeTransactions", detailSection(
                "12.3",
                "GET /internal/runtime/active-transactions",
                "현재 실행 중 거래",
                activeTransactions));
        detail.put("slowSql", detailSection(
                "12.4",
                "GET /internal/runtime/slow-sql?limit=50",
                "최근 Slow SQL",
                slowSql));
        detail.put("threads", detailSection(
                "12.5",
                "GET /internal/runtime/threads",
                "TCF 거래 연계 Thread",
                threads));
        detail.put("scopeNote",
                "Thread Dump 전체가 아닙니다. ActiveTransactionRegistry에 등록된 TCF 거래 Thread만 반환합니다.");
        return detail;
    }

    private static Map<String, Object> detailSection(
            String section, String api, String title, List<Map<String, Object>> rows) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("section", section);
        block.put("api", api);
        block.put("title", title);
        block.put("count", rows.size());
        block.put("rows", rows);
        return block;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildCauseAnalysis(
            Map<String, Object> collected,
            Map<String, Object> analysis,
            List<Map<String, Object>> activeTransactions) {
        Map<String, Object> causeAnalysis = new LinkedHashMap<>();
        Map<String, Object> cards = castMap(analysis.get("cards"));
        String primaryCauseCode = stringValue(analysis.get("primaryCauseCode"));
        String normalizedPrimary = normalizeCauseCode(primaryCauseCode);

        boolean anyDeadlock = false;
        boolean anyDbPoolExhausted = false;
        double maxHeapRatio = 0;
        long maxGcTimeMs = 0;
        double maxCpu = toDouble(cards.get("jvmCpuRatio"));
        double maxBusy = toDouble(cards.get("threadBusyRatio"));
        int slowTx = (int) toLong(cards.get("slowTransactionCount"));
        int slowSql = (int) toLong(cards.get("slowSqlCount"));
        int dbPending = (int) toLong(cards.get("dbPending"));
        int externalWait = 0;

        Object targetRows = collected.get("targets");
        if (targetRows instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> targetRow)) {
                    continue;
                }
                Map<String, Object> status = castMap(((Map<String, Object>) targetRow).get("status"));
                Map<String, Object> thread = castMap(status.get("thread"));
                Map<String, Object> jvm = castMap(status.get("jvm"));
                if (Boolean.TRUE.equals(thread.get("deadlock"))) {
                    anyDeadlock = true;
                }
                maxHeapRatio = Math.max(maxHeapRatio, toDouble(jvm.get("heapRatio")));
                maxGcTimeMs = Math.max(maxGcTimeMs, toLong(jvm.get("gcTimeLastMinuteMs")));
                for (Map<String, Object> pool : castList(status.get("dbPools"))) {
                    int maximum = (int) toLong(pool.get("maximum"));
                    int active = (int) toLong(pool.get("active"));
                    int idle = (int) toLong(pool.get("idle"));
                    int pending = (int) toLong(pool.get("pending"));
                    if (maximum > 0 && active >= maximum && idle == 0 && pending > 0) {
                        anyDbPoolExhausted = true;
                    }
                }
            }
        }

        for (Map<String, Object> tx : activeTransactions) {
            if ("WAIT_EXTERNAL".equals(stringValue(tx.get("currentStep")))) {
                externalWait++;
            }
        }

        double businessOwnership = toDouble(cards.get("dominantBusinessOwnershipPct"));
        String dominantBusiness = stringValue(cards.get("dominantBusinessCode"));
        double serviceOwnership = 0;
        String dominantService = stringValue(analysis.get("dominantServiceId"));
        if (dominantService != null) {
            int totalActive = 0;
            int serviceActive = 0;
            for (Map<String, Object> tx : activeTransactions) {
                totalActive++;
                if (dominantService.equals(stringValue(tx.get("serviceId")))) {
                    serviceActive++;
                }
            }
            if (totalActive > 0) {
                serviceOwnership = round1(serviceActive * 100.0 / totalActive);
            }
        }

        boolean gcPressure = maxHeapRatio >= 80 && maxGcTimeMs >= 3000;
        boolean cpuOverload = maxCpu >= 90;
        boolean threadSaturation = maxBusy >= 85;
        boolean slowSqlDetected = slowSql >= 3;
        boolean externalWaitDetected = externalWait >= 3;
        boolean businessDominance = businessOwnership >= 60;
        boolean serviceDominance = serviceOwnership >= 40;
        boolean unknownFallback = "UNKNOWN".equals(primaryCauseCode);
        boolean normal = "NORMAL".equals(primaryCauseCode);

        List<Map<String, Object>> priorities = List.of(
                priorityRow(1, "Deadlock"),
                priorityRow(2, "DB Pool 고갈"),
                priorityRow(3, "GC 압박"),
                priorityRow(4, "CPU 과부하"),
                priorityRow(5, "Tomcat Thread 포화"),
                priorityRow(6, "Slow SQL"),
                priorityRow(7, "외부 연계 지연"),
                priorityRow(8, "특정 업무·ServiceId 독점"),
                priorityRow(9, "원인 불명"));

        List<Map<String, Object>> causeTable = new ArrayList<>();
        causeTable.add(causeRow(
                1, "Deadlock 존재", "THREAD_DEADLOCK",
                "Thread Deadlock이 발견되었습니다.",
                anyDeadlock, normalizedPrimary, anyDeadlock ? "thread.deadlock=true" : "-"));
        causeTable.add(causeRow(
                2, "Active=Max, Idle=0, Pending>0", "DB_POOL_EXHAUSTED",
                "DB Connection 대기가 발생했습니다.",
                anyDbPoolExhausted, normalizedPrimary,
                anyDbPoolExhausted ? String.format("Pending %d", dbPending) : "-"));
        causeTable.add(causeRow(
                3, "Heap≥80%, GC 시간 증가", "GC_PRESSURE",
                "GC 증가로 JVM 응답이 지연되고 있습니다.",
                gcPressure, normalizedPrimary,
                gcPressure ? String.format("Heap %s%%, GC %sms", format1(maxHeapRatio), maxGcTimeMs) : "-"));
        causeTable.add(causeRow(
                4, "CPU≥90%", "CPU_OVERLOAD",
                "JVM CPU가 과부하 상태입니다.",
                cpuOverload, normalizedPrimary,
                cpuOverload ? String.format("CPU %s%%, DB Pending %d", format1(maxCpu), dbPending) : "-"));
        causeTable.add(causeRow(
                5, "Busy≥85%", "THREAD_SATURATION",
                "Tomcat Thread가 포화 상태입니다.",
                threadSaturation, normalizedPrimary,
                threadSaturation ? String.format("Busy %s%%, Slow TX %d", format1(maxBusy), slowTx) : "-"));
        causeTable.add(causeRow(
                6, "Slow SQL 다수", "SLOW_SQL",
                "특정 Mapper SQL이 장시간 실행 중입니다.",
                slowSqlDetected, normalizedPrimary,
                slowSqlDetected ? String.format("Slow SQL %d건 (1분)", slowSql) : "-"));
        causeTable.add(causeRow(
                7, "WAIT_EXTERNAL 다수", "EXTERNAL_WAIT",
                "외부 시스템 응답을 기다리고 있습니다.",
                externalWaitDetected, normalizedPrimary,
                externalWaitDetected ? String.format("WAIT_EXTERNAL %d건", externalWait) : "-"));
        causeTable.add(causeRow(
                8, "업무 점유율≥60%", "BUSINESS_DOMINANCE",
                "특정 업무가 실행 자원을 과다 점유 중입니다.",
                businessDominance, normalizedPrimary,
                businessDominance ? String.format("%s %s%%", dominantBusiness, format1(businessOwnership)) : "-"));
        causeTable.add(causeRow(
                8, "ServiceId 점유율≥40%", "SERVICE_DOMINANCE",
                "특정 ServiceId가 자원을 과다 점유 중입니다.",
                serviceDominance, normalizedPrimary,
                serviceDominance ? String.format("%s %s%%", dominantService, format1(serviceOwnership)) : "-"));
        causeTable.add(causeRow(
                9, "조건 불충분", "UNKNOWN",
                "추가 로그와 DB 상태 확인이 필요합니다.",
                unknownFallback, normalizedPrimary,
                unknownFallback ? "경계 징후 존재, 명확한 원인 미확정" : "-"));

        if (normal) {
            Map<String, Object> normalRow = new LinkedHashMap<>();
            normalRow.put("priority", 10);
            normalRow.put("condition", "이상 없음");
            normalRow.put("causeCode", "NORMAL");
            normalRow.put("message", "현재 주요 병목이 없습니다.");
            normalRow.put("detected", true);
            normalRow.put("primary", true);
            normalRow.put("evidence", "-");
            causeTable.add(normalRow);
        }

        causeAnalysis.put("priorities", priorities);
        causeAnalysis.put("causeTable", causeTable);
        causeAnalysis.put("primaryCauseCode", primaryCauseCode);
        causeAnalysis.put("primaryMessage", analysis.get("primaryMessage"));
        causeAnalysis.put("overallStatus", analysis.get("overallStatus"));
        causeAnalysis.put("warFindings", analysis.get("findings"));
        causeAnalysis.put("note",
                "WAR 1~7번은 각 WAR summary 중 최우선 Cause 채택 · 8번 독점은 1~7 미해당 시 tcf-om 적용");
        return causeAnalysis;
    }

    private static Map<String, Object> priorityRow(int rank, String label) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("rank", rank);
        row.put("label", label);
        return row;
    }

    private static Map<String, Object> causeRow(
            int priority,
            String condition,
            String causeCode,
            String message,
            boolean detected,
            String primaryCauseCode,
            String evidence) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("priority", priority);
        row.put("condition", condition);
        row.put("causeCode", causeCode);
        row.put("message", message);
        row.put("detected", detected);
        row.put("primary", causeCode.equals(primaryCauseCode)
                || ("BUSINESS_DOMINANCE".equals(causeCode) && "BUSINESS_RESOURCE_DOMINANCE".equals(primaryCauseCode)));
        row.put("evidence", evidence);
        return row;
    }

    private static String normalizeCauseCode(String causeCode) {
        if ("BUSINESS_RESOURCE_DOMINANCE".equals(causeCode)) {
            return "BUSINESS_DOMINANCE";
        }
        return causeCode;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildStatusCardsAnalysis(
            Map<String, Object> collected,
            Map<String, Object> analysis) {
        Map<String, Object> statusCards = new LinkedHashMap<>();
        Map<String, Object> cards = castMap(analysis.get("cards"));

        int threadBusy = 0;
        int threadMax = 0;
        double threadBusyRatio = toDouble(cards.get("threadBusyRatio"));
        double jvmCpu = toDouble(cards.get("jvmCpuRatio"));
        double heapRatio = toDouble(cards.get("heapRatio"));
        long gcTimeMs = 0;

        Object targetRows = collected.get("targets");
        if (targetRows instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> targetRow)) {
                    continue;
                }
                Map<String, Object> status = castMap(((Map<String, Object>) targetRow).get("status"));
                Map<String, Object> thread = castMap(status.get("thread"));
                Map<String, Object> jvm = castMap(status.get("jvm"));
                if (threadMax == 0 && toLong(thread.get("max")) > 0) {
                    threadMax = (int) toLong(thread.get("max"));
                    threadBusy = (int) toLong(thread.get("busy"));
                }
                gcTimeMs = Math.max(gcTimeMs, toLong(jvm.get("gcTimeLastMinuteMs")));
            }
        }

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
                ? String.format("%d / %d, %s%%", threadBusy, threadMax, format1(threadBusyRatio))
                : String.format("- / -, %s%%", format1(threadBusyRatio));
        String gcDisplay = gcPressure
                ? String.format("압박 (%s초)", format1(gcTimeMs / 1000.0))
                : "정상";
        String dbDisplay = String.format("%d / %d, Pending %d", dbActive, dbMaximum, dbPending);
        String businessDisplay = dominantBusiness != null && !dominantBusiness.isBlank()
                ? String.format("%s %s%% 점유", dominantBusiness, format1(dominantPct))
                : "-";

        List<Map<String, Object>> cardList = new ArrayList<>();
        cardList.add(statusCard("thread", "Thread", threadDisplay,
                threadBusyRatio >= 85 ? "warn" : "normal"));
        cardList.add(statusCard("jvmCpu", "JVM CPU", format1(jvmCpu) + "%",
                jvmCpu >= 90 ? "warn" : "normal"));
        cardList.add(statusCard("heap", "Heap", format1(heapRatio) + "%",
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

        statusCards.put("cards", cardList);
        statusCards.put("overallStatus", analysis.get("overallStatus"));
        statusCards.put("primaryCauseCode", analysis.get("primaryCauseCode"));
        statusCards.put("primaryMessage", analysis.get("primaryMessage"));
        statusCards.put("scopeNote",
                "Tomcat 공유 Thread·JVM · DB Pool·Slow 지표는 전 WAR 중 max·합산 기준");
        return statusCards;
    }

    private Map<String, Object> buildActiveTransactionListAnalysis(
            List<Map<String, Object>> activeTransactions) {
        Map<String, Object> listAnalysis = new LinkedHashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> tx : activeTransactions) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("businessCode", tx.get("businessCode"));
            row.put("serviceId", tx.get("serviceId"));
            row.put("elapsedMs", tx.get("elapsedMs"));
            row.put("elapsedDisplay", formatElapsedDisplay(toLong(tx.get("elapsedMs"))));
            row.put("currentStep", tx.get("currentStep"));
            row.put("stepLabel", formatStepLabel(stringValue(tx.get("currentStep"))));
            row.put("sqlOrExternal", formatSqlOrExternal(tx));
            rows.add(row);
        }
        listAnalysis.put("section", "15.4");
        listAnalysis.put("title", "실행 중 거래 목록");
        listAnalysis.put("api", "GET /internal/runtime/active-transactions");
        listAnalysis.put("count", rows.size());
        listAnalysis.put("rows", rows);
        return listAnalysis;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildActiveTransactionScreenAnalysis(
            Map<String, Object> collected,
            List<Map<String, Object>> activeTransactions,
            List<Map<String, Object>> slowTransactions,
            List<Map<String, Object>> threads) {
        Map<String, Object> screen = new LinkedHashMap<>();
        screen.put("section", "RTM-040");
        screen.put("title", "실행 거래 및 Slow ServiceId");
        screen.put("scopeNote", "ActiveTransactionRegistry 기준 현재 실행 거래 · 경과시간 내림차순");

        long defaultTimeoutSec = resolveDefaultOnlineTimeoutSec(collected);
        long slowThresholdMs = resolveSlowThresholdMs(collected);
        Map<String, String> instanceByWar = buildInstanceLabelByWar(collected);

        Map<String, Map<String, Object>> threadByGuid = new HashMap<>();
        for (Map<String, Object> thread : threads) {
            String guid = stringValue(thread.get("guid"));
            if (guid != null) {
                threadByGuid.put(guid, thread);
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        int slowCount = 0;
        int timeoutCount = 0;

        for (Map<String, Object> tx : activeTransactions) {
            Map<String, Object> row = buildActiveTransactionScreenRow(
                    tx, threadByGuid, instanceByWar, defaultTimeoutSec, slowThresholdMs);
            rows.add(row);
            if (Boolean.TRUE.equals(row.get("slow"))) {
                slowCount++;
            }
            if ("TIMEOUT".equals(row.get("runtimeStatus"))) {
                timeoutCount++;
            }
        }
        rows.sort(Comparator.comparingLong((Map<String, Object> r) -> toLong(r.get("elapsedMs"))).reversed());

        List<Map<String, Object>> slowServiceIds = buildSlowServiceIdSummary(rows, slowTransactions);

        screen.put("summary", Map.of(
                "runningCount", rows.size(),
                "slowCount", slowCount,
                "timeoutCount", timeoutCount,
                "slowServiceIdCount", slowServiceIds.size()));
        screen.put("rows", rows);
        screen.put("slowServiceIds", slowServiceIds);
        screen.put("filterOptions", Map.of(
                "centers", List.of("ALL", "CENTER1", "CENTER2"),
                "instances", buildRtm040InstanceOptions(collected),
                "wars", buildRtm040WarOptions(collected, activeTransactions),
                "steps", List.of(
                        Map.of("value", "ALL", "label", "전체"),
                        Map.of("value", "WAIT_DB_CONNECTION", "label", "DB 대기"),
                        Map.of("value", "EXECUTING_SQL", "label", "SQL 실행"),
                        Map.of("value", "WAIT_EXTERNAL", "label", "외부 대기"),
                        Map.of("value", "WAIT_HANDLER", "label", "Handler 대기")),
                "elapsedPresets", List.of(
                        Map.of("value", "0", "label", "전체"),
                        Map.of("value", "1000", "label", "1초 이상"),
                        Map.of("value", "3000", "label", "3초 이상"),
                        Map.of("value", "custom", "label", "직접입력")),
                "statuses", List.of(
                        Map.of("value", "ALL", "label", "전체"),
                        Map.of("value", "RUNNING", "label", "실행 중"),
                        Map.of("value", "TIMEOUT", "label", "Timeout"),
                        Map.of("value", "ERROR", "label", "오류"))));
        screen.put("defaultTimeoutSec", defaultTimeoutSec);
        screen.put("slowThresholdMs", slowThresholdMs);
        screen.put("detailActions", List.of(
                actionLink("거래 전체 추적", "/om/admin/runtime-workspace.html?tab=rtm050"),
                actionLink("관련 SQL", "/om/admin/runtime-sql-analysis.html"),
                actionLink("동일 ServiceId 거래", "/om/admin/runtime-workspace.html?tab=rtm040"),
                actionLink("장애보고서 추가", "/om/admin/runtime-cause-analysis.html")));
        return screen;
    }

    private Map<String, Object> buildActiveTransactionScreenRow(
            Map<String, Object> tx,
            Map<String, Map<String, Object>> threadByGuid,
            Map<String, String> instanceByWar,
            long defaultTimeoutSec,
            long slowThresholdMs) {
        String guid = stringValue(tx.get("guid"));
        String war = stringValue(tx.get("businessCode"));
        long elapsedMs = toLong(tx.get("elapsedMs"));
        long timeoutMs = defaultTimeoutSec * 1000L;
        long remainingMs = Math.max(0, timeoutMs - elapsedMs);
        String currentStep = stringValue(tx.get("currentStep"));
        String runtimeStatus = resolveRuntimeTxStatus(elapsedMs, timeoutMs, currentStep);
        String healthStatus = resolveTxHealthStatus(elapsedMs, timeoutMs, currentStep);
        boolean slow = elapsedMs >= slowThresholdMs;

        Map<String, Object> thread = guid != null ? threadByGuid.get(guid) : null;
        String threadName = thread != null
                ? stringValue(thread.get("threadName"))
                : stringValue(tx.get("threadName"));

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("guid", guid);
        row.put("traceId", tx.get("traceId"));
        row.put("businessCode", war);
        row.put("instanceLabel", instanceByWar.getOrDefault(war, "CENTER1-AP01:8080"));
        row.put("center", "CENTER1");
        row.put("serviceId", tx.get("serviceId"));
        row.put("transactionCode", tx.get("transactionCode"));
        row.put("startTimeMillis", tx.get("startTimeMillis"));
        row.put("startTimeDisplay", formatStartTime(toLong(tx.get("startTimeMillis"))));
        row.put("elapsedMs", elapsedMs);
        row.put("elapsedDisplay", formatElapsedDisplay(elapsedMs));
        row.put("currentStep", currentStep);
        row.put("stepLabel", formatStepLabel(currentStep));
        row.put("threadName", threadName != null ? threadName : "-");
        row.put("threadId", thread != null ? thread.get("threadId") : tx.get("threadId"));
        row.put("sqlOrExternal", formatSqlOrExternal(tx));
        row.put("timeoutSec", defaultTimeoutSec);
        row.put("timeoutRemainingMs", remainingMs);
        row.put("timeoutRemainingDisplay", formatElapsedDisplay(remainingMs));
        row.put("runtimeStatus", runtimeStatus);
        row.put("runtimeStatusLabel", runtimeStatusLabel(runtimeStatus));
        row.put("healthStatus", healthStatus);
        row.put("healthStatusLabel", statusLabelKo(healthStatus));
        row.put("slow", slow);
        return row;
    }

    private static List<Map<String, Object>> buildSlowServiceIdSummary(
            List<Map<String, Object>> activeRows,
            List<Map<String, Object>> slowTransactions) {
        Map<String, SlowServiceAgg> agg = new LinkedHashMap<>();
        for (Map<String, Object> row : activeRows) {
            if (!Boolean.TRUE.equals(row.get("slow"))) {
                continue;
            }
            String serviceId = stringValue(row.get("serviceId"));
            if (serviceId == null) {
                continue;
            }
            SlowServiceAgg stats = agg.computeIfAbsent(serviceId, key -> new SlowServiceAgg());
            stats.active++;
            stats.maxElapsed = Math.max(stats.maxElapsed, toLong(row.get("elapsedMs")));
        }
        for (Map<String, Object> row : slowTransactions) {
            String serviceId = stringValue(row.get("serviceId"));
            if (serviceId == null) {
                continue;
            }
            SlowServiceAgg stats = agg.computeIfAbsent(serviceId, key -> new SlowServiceAgg());
            stats.recentSlow++;
            stats.maxElapsed = Math.max(stats.maxElapsed, toLong(row.get("elapsedMs")));
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<String, SlowServiceAgg> entry : agg.entrySet()) {
            SlowServiceAgg stats = entry.getValue();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("serviceId", entry.getKey());
            row.put("activeSlowCount", stats.active);
            row.put("recentSlowCount", stats.recentSlow);
            row.put("maxElapsedMs", stats.maxElapsed);
            row.put("maxElapsedDisplay", formatElapsedDisplay(stats.maxElapsed));
            rows.add(row);
        }
        rows.sort(Comparator.comparingInt((Map<String, Object> r) -> (int) toLong(r.get("activeSlowCount"))).reversed());
        return rows;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> buildInstanceLabelByWar(Map<String, Object> collected) {
        Map<String, String> labels = new LinkedHashMap<>();
        Object targetRows = collected.get("targets");
        if (!(targetRows instanceof List<?> list)) {
            return labels;
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> targetRow)) {
                continue;
            }
            Map<String, Object> target = (Map<String, Object>) targetRow;
            String war = stringValue(target.get("businessCode"));
            if (war != null) {
                labels.put(war, formatInstanceLabel(target));
            }
        }
        return labels;
    }

    @SuppressWarnings("unchecked")
    private static List<String> buildRtm040WarOptions(
            Map<String, Object> collected, List<Map<String, Object>> activeTransactions) {
        java.util.LinkedHashSet<String> wars = new java.util.LinkedHashSet<>();
        wars.add("ALL");
        Object targetRows = collected.get("targets");
        if (targetRows instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> targetRow) {
                    String war = stringValue(((Map<String, Object>) targetRow).get("businessCode"));
                    if (war != null) {
                        wars.add(war);
                    }
                }
            }
        }
        for (Map<String, Object> tx : activeTransactions) {
            String war = stringValue(tx.get("businessCode"));
            if (war != null) {
                wars.add(war);
            }
        }
        return new ArrayList<>(wars);
    }

    @SuppressWarnings("unchecked")
    private static List<String> buildRtm040InstanceOptions(Map<String, Object> collected) {
        java.util.LinkedHashSet<String> instances = new java.util.LinkedHashSet<>();
        instances.add("ALL");
        Object targetRows = collected.get("targets");
        if (!(targetRows instanceof List<?> list)) {
            return new ArrayList<>(instances);
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> targetRow) {
                instances.add(formatInstanceLabel((Map<String, Object>) targetRow));
            }
        }
        return new ArrayList<>(instances);
    }

    @SuppressWarnings("unchecked")
    private long resolveDefaultOnlineTimeoutSec(Map<String, Object> collected) {
        Object targetRows = collected.get("targets");
        if (targetRows instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> targetRow)) {
                    continue;
                }
                Map<String, Object> status = castMap(((Map<String, Object>) targetRow).get("status"));
                Map<String, Object> runtimeConfig = castMap(status.get("runtimeConfig"));
                if (runtimeConfig.containsKey("onlineTimeoutSec")) {
                    return toLong(runtimeConfig.get("onlineTimeoutSec"));
                }
            }
        }
        return 60L;
    }

    @SuppressWarnings("unchecked")
    private long resolveSlowThresholdMs(Map<String, Object> collected) {
        Object targetRows = collected.get("targets");
        if (targetRows instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> targetRow)) {
                    continue;
                }
                Map<String, Object> status = castMap(((Map<String, Object>) targetRow).get("status"));
                Map<String, Object> runtimeConfig = castMap(status.get("runtimeConfig"));
                if (runtimeConfig.containsKey("slowSqlThresholdMs")) {
                    return toLong(runtimeConfig.get("slowSqlThresholdMs"));
                }
            }
        }
        return 1000L;
    }

    private static String resolveRuntimeTxStatus(long elapsedMs, long timeoutMs, String currentStep) {
        if (elapsedMs >= timeoutMs) {
            return "TIMEOUT";
        }
        if ("FAILED".equals(currentStep)) {
            return "ERROR";
        }
        return "RUNNING";
    }

    private static String resolveTxHealthStatus(long elapsedMs, long timeoutMs, String currentStep) {
        if (elapsedMs >= timeoutMs || elapsedMs >= 5000) {
            return "CRITICAL";
        }
        if (elapsedMs >= 3000 || "WAIT_DB_CONNECTION".equals(currentStep)) {
            return "WARN";
        }
        return "NORMAL";
    }

    private static String runtimeStatusLabel(String status) {
        return switch (status) {
            case "TIMEOUT" -> "Timeout";
            case "ERROR" -> "오류";
            default -> "실행 중";
        };
    }

    private static String formatStartTime(long startTimeMillis) {
        if (startTimeMillis <= 0) {
            return "-";
        }
        return java.time.Instant.ofEpochMilli(startTimeMillis)
                .atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    private static String formatTraceTimestamp(long timestampMillis) {
        if (timestampMillis <= 0) {
            return "-";
        }
        return java.time.Instant.ofEpochMilli(timestampMillis)
                .atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildTransactionTraceScreenAnalysis(
            Map<String, Object> collected,
            List<Map<String, Object>> activeTransactions,
            List<Map<String, Object>> threads,
            Map<String, Object> body) {
        Map<String, Object> screen = new LinkedHashMap<>();
        screen.put("section", "RTM-050");
        screen.put("title", "거래 추적 상세");
        screen.put("scopeNote", "수집되지 않은 Timeline 단계는 임의 계산하지 않으며 미수집으로 표시합니다.");

        String requestedGuid = OmBodySupport.stringValue(body, "traceGuid");
        Map<String, Object> tx = findActiveTxByGuid(activeTransactions, requestedGuid);
        Map<String, Object> thread = findThreadByGuid(threads, tx);

        List<Map<String, Object>> guidOptions = activeTransactions.stream()
                .map(row -> Map.<String, Object>of(
                        "guid", row.get("guid"),
                        "serviceId", row.get("serviceId"),
                        "businessCode", row.get("businessCode")))
                .toList();
        screen.put("guidOptions", guidOptions);
        screen.put("selectedGuid", tx != null ? tx.get("guid") : requestedGuid);

        if (tx == null) {
            screen.put("trace", Map.of(
                    "found", false,
                    "message", "선택한 GUID의 실행 중 거래를 찾을 수 없습니다."));
            return screen;
        }

        long timeoutSec = resolveDefaultOnlineTimeoutSec(collected);
        long dbWaitMs = toLong(tx.get("dbWaitMs"));
        long elapsedMs = toLong(tx.get("elapsedMs"));
        long sqlElapsedMs = resolveSqlElapsedMs(tx);
        MapperSqlParts mapperParts = parseMapperSqlParts(stringValue(tx.get("sqlId")), stringValue(tx.get("mapperSql")));

        List<Map<String, Object>> timeline = buildTraceTimeline(castList(tx.get("stepHistory")));
        List<String> uncollectedSteps = resolveUncollectedTimelineSteps(castList(tx.get("stepHistory")));
        List<Map<String, Object>> causeCandidates = buildTraceCauseCandidates(
                stringValue(tx.get("currentStep")), dbWaitMs, sqlElapsedMs);

        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("found", true);
        trace.put("guid", tx.get("guid"));
        trace.put("traceId", tx.get("traceId"));
        trace.put("serviceId", tx.get("serviceId"));
        trace.put("businessCode", tx.get("businessCode"));
        trace.put("startTimeDisplay", formatTraceTimestamp(toLong(tx.get("startTimeMillis"))));
        trace.put("elapsedDisplay", formatElapsedDisplay(elapsedMs));
        trace.put("threadName", thread != null ? thread.get("threadName") : tx.get("threadName"));
        trace.put("timeline", timeline);
        trace.put("uncollectedSteps", uncollectedSteps);
        trace.put("currentState", Map.of(
                "currentStep", stringValue(tx.get("currentStep")),
                "stepLabel", formatStepLabel(stringValue(tx.get("currentStep"))),
                "mapper", mapperParts.mapper(),
                "sqlId", mapperParts.sqlId(),
                "dbWaitDisplay", formatElapsedDisplay(dbWaitMs),
                "sqlElapsedDisplay", formatElapsedDisplay(sqlElapsedMs),
                "timeoutSec", timeoutSec,
                "sqlOrExternal", formatSqlOrExternal(tx)));
        trace.put("causeCandidates", causeCandidates);
        trace.put("actions", List.of(
                actionLink("Mapper 상세", "/om/admin/runtime-workspace.html?tab=rtm060"),
                actionLink("동일 SQL 거래", "/om/admin/runtime-workspace.html?tab=rtm040"),
                actionLink("Thread 상세", "/om/admin/runtime-workspace.html?tab=rtm020"),
                actionLink("장애보고서 연결", "/om/admin/runtime-cause-analysis.html")));
        screen.put("trace", trace);
        return screen;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildSqlExternalScreenAnalysis(
            Map<String, Object> collected,
            List<Map<String, Object>> slowSql,
            List<Map<String, Object>> activeTransactions,
            Map<String, Object> body) {
        Map<String, Object> screen = new LinkedHashMap<>();
        screen.put("section", "RTM-060");
        screen.put("title", "Slow SQL·외부연계");
        screen.put("privacyNote",
                "SQL Parameter, 개인정보, 요청·응답 Body, JWT, 계좌번호·고객번호 원문은 표시하지 않습니다.");

        long thresholdMs = resolveConfiguredSlowSqlThresholdMs(collected);
        List<Map<String, Object>> detailItems = buildSlowSqlDetailItems(activeTransactions, slowSql, thresholdMs);
        List<Map<String, Object>> summaryRows = aggregateSlowSqlSummaryRows(detailItems, thresholdMs);
        List<Map<String, Object>> externalRows = buildExternalWaitRows(activeTransactions, collected);

        java.util.Set<String> wars = new java.util.LinkedHashSet<>();
        wars.add("ALL");
        for (Map<String, Object> row : summaryRows) {
            String war = stringValue(row.get("businessCode"));
            if (war != null && !war.isBlank()) {
                wars.add(war);
            }
        }
        for (Map<String, Object> row : externalRows) {
            String war = stringValue(row.get("businessCode"));
            if (war != null && !war.isBlank()) {
                wars.add(war);
            }
        }

        String requestedKey = OmBodySupport.stringValue(body, "sqlRowKey");
        Map<String, Object> selectedDetail = resolveSlowSqlDetailSelection(detailItems, summaryRows, requestedKey);

        Map<String, Object> slowTab = new LinkedHashMap<>();
        slowTab.put("configuredThresholdMs", thresholdMs);
        slowTab.put("filterOptions", Map.of(
                "wars", wars.stream().toList(),
                "elapsedPresets", List.of(
                        presetOption("0", "전체"),
                        presetOption("1000", "1초 이상"),
                        presetOption("2000", "2초 이상"),
                        presetOption("3000", "3초 이상"),
                        presetOption("5000", "5초 이상"))));
        slowTab.put("summaryRows", summaryRows);
        slowTab.put("detailItems", detailItems);
        slowTab.put("selectedRowKey", selectedDetail.get("rowKey"));
        slowTab.put("detail", selectedDetail.get("detail"));

        Map<String, Object> externalTab = new LinkedHashMap<>();
        externalTab.put("rows", externalRows);
        externalTab.put("runningCount", externalRows.size());
        externalTab.put("collectionNote",
                "Connect·Read 구간은 markExternalWait() 연동 WAR에서만 분리 수집됩니다. 미연동 시 Read 대기는 거래 경과로 대체합니다.");

        screen.put("slowSqlTab", slowTab);
        screen.put("externalTab", externalTab);
        return screen;
    }

    private static Map<String, Object> presetOption(String value, String label) {
        return Map.of("value", value, "label", label);
    }

    private long resolveConfiguredSlowSqlThresholdMs(Map<String, Object> collected) {
        Object targetRows = collected.get("targets");
        if (targetRows instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> targetRow)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> target = (Map<String, Object>) targetRow;
                Map<String, Object> status = castMap(target.get("status"));
                Map<String, Object> runtimeConfig = castMap(status.get("runtimeConfig"));
                if (runtimeConfig.containsKey("slowSqlThresholdMs")) {
                    return toLong(runtimeConfig.get("slowSqlThresholdMs"));
                }
            }
        }
        return 2000L;
    }

    private List<Map<String, Object>> buildSlowSqlDetailItems(
            List<Map<String, Object>> activeTransactions,
            List<Map<String, Object>> slowSql,
            long thresholdMs) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> tx : activeTransactions) {
            if (!"EXECUTING_SQL".equals(stringValue(tx.get("currentStep")))) {
                continue;
            }
            long sqlElapsed = resolveSqlElapsedMs(tx);
            long elapsed = sqlElapsed > 0 ? sqlElapsed : toLong(tx.get("elapsedMs"));
            if (elapsed < thresholdMs) {
                continue;
            }
            items.add(buildSlowSqlDetailFromActiveTx(tx, elapsed));
        }
        for (Map<String, Object> sql : slowSql) {
            long elapsed = toLong(sql.get("elapsedMs"));
            if (elapsed < thresholdMs) {
                continue;
            }
            items.add(buildSlowSqlDetailFromHistory(sql, elapsed));
        }
        items.sort((a, b) -> Long.compare(toLong(b.get("elapsedMs")), toLong(a.get("elapsedMs"))));
        return items;
    }

    private Map<String, Object> buildSlowSqlDetailFromActiveTx(Map<String, Object> tx, long sqlElapsedMs) {
        MapperSqlParts parts = parseMapperSqlParts(
                stringValue(tx.get("sqlId")), stringValue(tx.get("mapperSql")));
        String war = stringValue(tx.get("businessCode"));
        String serviceId = stringValue(tx.get("serviceId"));
        String rowKey = slowSqlRowKey(war, serviceId, parts.mapper(), parts.sqlId());

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("rowKey", rowKey);
        item.put("businessCode", war);
        item.put("serviceId", serviceId);
        item.put("guid", tx.get("guid"));
        item.put("traceId", tx.get("traceId"));
        item.put("mapper", parts.mapper());
        item.put("sqlId", parts.sqlId());
        item.put("mapperSqlDisplay", parts.mapper() + "\n" + parts.sqlId());
        item.put("startTimeMillis", tx.get("startTimeMillis"));
        item.put("startTimeDisplay", formatTraceTimestamp(toLong(tx.get("startTimeMillis"))));
        item.put("elapsedMs", sqlElapsedMs);
        item.put("elapsedDisplay", formatElapsedDisplay(sqlElapsedMs));
        item.put("dbWaitMs", toLong(tx.get("dbWaitMs")));
        item.put("dbWaitDisplay", formatElapsedDisplay(toLong(tx.get("dbWaitMs"))));
        item.put("resultStatus", "RUNNING");
        item.put("resultStatusLabel", "실행 중");
        item.put("errorType", "-");
        item.put("affectedRows", null);
        item.put("affectedRowsDisplay", "-");
        item.put("running", true);
        return item;
    }

    private Map<String, Object> buildSlowSqlDetailFromHistory(Map<String, Object> sql, long elapsedMs) {
        MapperSqlParts parts = parseMapperSqlParts(
                stringValue(sql.get("mapperId")), stringValue(sql.get("sqlId")));
        String war = stringValue(sql.get("businessCode"));
        if (war == null || war.isBlank()) {
            String serviceId = stringValue(sql.get("serviceId"));
            war = serviceId != null && serviceId.contains(".")
                    ? serviceId.substring(0, serviceId.indexOf('.'))
                    : "-";
        }
        String serviceId = stringValue(sql.get("serviceId"));
        String rowKey = slowSqlRowKey(war, serviceId, parts.mapper(), parts.sqlId());
        boolean success = !Boolean.FALSE.equals(sql.get("success"));

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("rowKey", rowKey);
        item.put("businessCode", war);
        item.put("serviceId", serviceId);
        item.put("guid", "-");
        item.put("traceId", "-");
        item.put("mapper", parts.mapper());
        item.put("sqlId", parts.sqlId());
        item.put("mapperSqlDisplay", parts.mapper() + "\n" + parts.sqlId());
        item.put("startTimeMillis", sql.get("startTimeMillis"));
        item.put("startTimeDisplay", formatTraceTimestamp(toLong(sql.get("startTimeMillis"))));
        item.put("elapsedMs", elapsedMs);
        item.put("elapsedDisplay", formatElapsedDisplay(elapsedMs));
        item.put("dbWaitMs", 0L);
        item.put("dbWaitDisplay", "-");
        item.put("resultStatus", success ? "COMPLETED" : "FAILED");
        item.put("resultStatusLabel", success ? "완료" : "실패");
        item.put("errorType", success ? "-" : "SQL_ERROR");
        long affected = toLong(sql.get("affectedRows"));
        item.put("affectedRows", affected >= 0 ? affected : null);
        item.put("affectedRowsDisplay", affected >= 0 ? String.valueOf(affected) : "-");
        item.put("running", false);
        return item;
    }

    private static String slowSqlRowKey(String war, String serviceId, String mapper, String sqlId) {
        return String.join("|",
                war != null ? war : "",
                serviceId != null ? serviceId : "",
                mapper != null ? mapper : "",
                sqlId != null ? sqlId : "");
    }

    private List<Map<String, Object>> aggregateSlowSqlSummaryRows(
            List<Map<String, Object>> detailItems, long thresholdMs) {
        Map<String, SlowSqlSummaryAgg> agg = new LinkedHashMap<>();
        for (Map<String, Object> item : detailItems) {
            String key = stringValue(item.get("rowKey"));
            SlowSqlSummaryAgg stats = agg.computeIfAbsent(key, k -> new SlowSqlSummaryAgg(item));
            stats.count++;
            stats.maxElapsed = Math.max(stats.maxElapsed, toLong(item.get("elapsedMs")));
            if (Boolean.TRUE.equals(item.get("running"))) {
                stats.running = true;
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SlowSqlSummaryAgg stats : agg.values()) {
            String health = resolveSlowSqlSummaryHealth(stats.maxElapsed, stats.count, stats.running, thresholdMs);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rowKey", stats.rowKey);
            row.put("healthStatus", health);
            row.put("statusLabel", "CRITICAL".equals(health) || "WARN".equals(health) ? "위험" : "-");
            row.put("businessCode", stats.businessCode);
            row.put("serviceId", stats.serviceId);
            row.put("mapper", stats.mapper);
            row.put("sqlId", stats.sqlId);
            row.put("mapperSqlDisplay", stats.mapper + "\n" + stats.sqlId);
            row.put("maxElapsedMs", stats.maxElapsed);
            row.put("elapsedDisplay", formatElapsedDisplay(stats.maxElapsed));
            row.put("count", stats.count);
            rows.add(row);
        }
        rows.sort((a, b) -> Long.compare(toLong(b.get("maxElapsedMs")), toLong(a.get("maxElapsedMs"))));
        return rows;
    }

    private static String resolveSlowSqlSummaryHealth(
            long maxElapsed, int count, boolean running, long thresholdMs) {
        if (running && maxElapsed >= thresholdMs) {
            return "CRITICAL";
        }
        if (maxElapsed >= thresholdMs || count >= 3) {
            return "WARN";
        }
        return "NORMAL";
    }

    private Map<String, Object> resolveSlowSqlDetailSelection(
            List<Map<String, Object>> detailItems,
            List<Map<String, Object>> summaryRows,
            String requestedKey) {
        String rowKey = requestedKey;
        if (rowKey == null || rowKey.isBlank()) {
            rowKey = summaryRows.isEmpty() ? null : stringValue(summaryRows.get(0).get("rowKey"));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rowKey", rowKey);
        if (rowKey == null) {
            result.put("detail", Map.of("found", false, "message", "표시할 Slow SQL 없음"));
            return result;
        }
        Map<String, Object> detail = null;
        for (Map<String, Object> item : detailItems) {
            if (rowKey.equals(stringValue(item.get("rowKey")))) {
                detail = item;
                break;
            }
        }
        if (detail == null) {
            result.put("detail", Map.of("found", false, "message", "선택한 SQL을 찾을 수 없습니다."));
            return result;
        }
        int concurrent = countConcurrentSameSql(detailItems, rowKey);
        Map<String, Object> panel = new LinkedHashMap<>(detail);
        panel.put("found", true);
        panel.put("concurrentSameSql", concurrent);
        panel.put("mapperId", detail.get("mapper"));
        result.put("detail", panel);
        return result;
    }

    private static int countConcurrentSameSql(List<Map<String, Object>> detailItems, String rowKey) {
        int count = 0;
        for (Map<String, Object> item : detailItems) {
            if (rowKey.equals(stringValue(item.get("rowKey"))) && Boolean.TRUE.equals(item.get("running"))) {
                count++;
            }
        }
        return count;
    }

    private List<Map<String, Object>> buildExternalWaitRows(
            List<Map<String, Object>> activeTransactions,
            Map<String, Object> collected) {
        long timeoutSec = resolveDefaultOnlineTimeoutSec(collected);
        Map<String, Integer> concurrentBySystem = new HashMap<>();
        for (Map<String, Object> tx : activeTransactions) {
            if (!"WAIT_EXTERNAL".equals(stringValue(tx.get("currentStep")))) {
                continue;
            }
            String external = stringValue(tx.get("externalSystemCode"));
            if (external == null || external.isBlank()) {
                external = "UNKNOWN";
            }
            concurrentBySystem.merge(external, 1, Integer::sum);
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> tx : activeTransactions) {
            if (!"WAIT_EXTERNAL".equals(stringValue(tx.get("currentStep")))) {
                continue;
            }
            String external = stringValue(tx.get("externalSystemCode"));
            if (external == null || external.isBlank()) {
                external = "UNKNOWN";
            }
            long readWaitMs = resolveExternalReadWaitMs(tx);
            long connectMs = resolveExternalConnectMs(tx);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("businessCode", tx.get("businessCode"));
            row.put("serviceId", tx.get("serviceId"));
            row.put("guid", tx.get("guid"));
            row.put("traceId", tx.get("traceId"));
            row.put("externalSystemCode", external);
            row.put("endpointIdentifier", external);
            row.put("connectMs", connectMs);
            row.put("connectDisplay", connectMs > 0 ? formatElapsedDisplay(connectMs) : "미수집");
            row.put("readWaitMs", readWaitMs);
            row.put("readWaitDisplay", formatElapsedDisplay(readWaitMs));
            row.put("timeoutSec", timeoutSec);
            row.put("status", "RUNNING");
            row.put("statusLabel", "실행 중");
            row.put("concurrentWait", concurrentBySystem.getOrDefault(external, 1));
            rows.add(row);
        }
        rows.sort((a, b) -> Long.compare(toLong(b.get("readWaitMs")), toLong(a.get("readWaitMs"))));
        return rows;
    }

    private long resolveExternalReadWaitMs(Map<String, Object> tx) {
        if (!"WAIT_EXTERNAL".equals(stringValue(tx.get("currentStep")))) {
            return 0;
        }
        List<Map<String, Object>> history = castList(tx.get("stepHistory"));
        for (int i = history.size() - 1; i >= 0; i--) {
            Map<String, Object> event = history.get(i);
            if ("WAIT_EXTERNAL".equals(stringValue(event.get("stepKey")))) {
                return Math.max(0L, System.currentTimeMillis() - toLong(event.get("timestampMillis")));
            }
        }
        return toLong(tx.get("elapsedMs"));
    }

    private static long resolveExternalConnectMs(Map<String, Object> tx) {
        List<Map<String, Object>> history = castListStatic(tx.get("stepHistory"));
        Long externalStart = null;
        Long requestStart = null;
        for (Map<String, Object> event : history) {
            String key = stringValue(event.get("stepKey"));
            if ("WAIT_EXTERNAL".equals(key)) {
                externalStart = toLong(event.get("timestampMillis"));
                break;
            }
            if (requestStart == null && "REQUEST_ENTRY".equals(key)) {
                requestStart = toLong(event.get("timestampMillis"));
            }
        }
        if (externalStart == null || requestStart == null || externalStart <= requestStart) {
            return 0;
        }
        return Math.max(0L, externalStart - requestStart);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castListStatic(Object value) {
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

    private static final class SlowSqlSummaryAgg {
        private final String rowKey;
        private final String businessCode;
        private final String serviceId;
        private final String mapper;
        private final String sqlId;
        private int count;
        private long maxElapsed;
        private boolean running;

        private SlowSqlSummaryAgg(Map<String, Object> seed) {
            rowKey = stringValue(seed.get("rowKey"));
            businessCode = stringValue(seed.get("businessCode"));
            serviceId = stringValue(seed.get("serviceId"));
            mapper = stringValue(seed.get("mapper"));
            sqlId = stringValue(seed.get("sqlId"));
            count = 0;
            maxElapsed = 0;
            running = false;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildIncidentReportScreenAnalysis(
            Map<String, Object> collected,
            Map<String, Object> analysis,
            Map<String, Object> causeAnalysis,
            List<Map<String, Object>> activeTransactions,
            List<Map<String, Object>> slowTransactions,
            List<Map<String, Object>> slowSql,
            Map<String, Object> dbPoolAnalysis) {
        Map<String, Object> screen = new LinkedHashMap<>();
        screen.put("section", "RTM-070");
        screen.put("title", "장애 진단 및 보고서");
        screen.put("scopeNote",
                "보고서 초안은 브라우저에 임시 저장됩니다. 서버 Snapshot·감사로그 연동은 후속 Phase입니다.");

        String overallStatus = stringValue(analysis.get("overallStatus"));
        String primaryRaw = stringValue(analysis.get("primaryCauseCode"));
        String primaryCause = displayCauseCode(primaryRaw);
        String dominantBusiness = stringValue(analysis.get("dominantBusinessCode"));
        String dominantService = stringValue(analysis.get("dominantServiceId"));
        String checkedAt = stringValue(collected.get("checkedAt"));

        Map<String, Object> auto = new LinkedHashMap<>();
        auto.put("primaryCauseCode", primaryCause);
        auto.put("primaryConfidence", resolveConfidence(overallStatus, primaryCause));

        Map<String, Object> secondary = resolveSecondaryCause(castList(causeAnalysis.get("causeTable")), primaryRaw);
        auto.put("secondaryCauseCode", secondary.get("causeCode"));
        auto.put("secondaryConfidence", secondary.get("confidence"));
        auto.put("secondaryMessage", secondary.get("message"));
        auto.put("evidenceSummary", buildIncidentEvidenceSummary(
                dbPoolAnalysis, activeTransactions, causeAnalysis, primaryCause));
        auto.put("suggestedWorkflowStatus", "NORMAL".equals(primaryCause) ? "CANDIDATE" : "CANDIDATE");
        auto.put("primaryMessage", analysis.get("primaryMessage"));

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("incidentIdSuggestion", suggestIncidentId(checkedAt));
        header.put("occurredAt", checkedAt != null && !checkedAt.isBlank() ? checkedAt : formatCheckedAtNow());
        header.put("targetDisplay", buildIncidentTargetDisplay(collected, dominantBusiness));
        header.put("impactServiceId", dominantService != null && !dominantService.isBlank() ? dominantService : "-");
        header.put("severityDefault", "CRITICAL".equals(overallStatus) ? "Critical" : "Major");
        header.put("overallStatus", overallStatus);
        header.put("overallStatusLabel", statusLabelKo(overallStatus));

        screen.put("header", header);
        screen.put("autoJudgment", auto);
        screen.put("severityOptions", List.of("Critical", "Major", "Minor"));
        screen.put("workflowStatuses", List.of(
                workflowStatus("CANDIDATE", "원인 후보"),
                workflowStatus("INVESTIGATING", "확인 중"),
                workflowStatus("CAUSE_CONFIRMED", "원인 확정"),
                workflowStatus("MITIGATION", "완화 조치"),
                workflowStatus("RECOVERED", "정상화"),
                workflowStatus("PREVENTION", "재발방지 진행"),
                workflowStatus("CLOSED", "종료")));
        screen.put("attachments", Map.of(
                "snapshotAvailable", true,
                "slowTransactionCount", slowTransactions.size(),
                "slowSqlCount", slowSql.size(),
                "activeTransactionCount", activeTransactions.size(),
                "checkedAt", checkedAt));
        screen.put("permissions", Map.of(
                "confirmCauseRole", "ROLE_OM_RTM",
                "normalizeRole", "ROLE_OM_RTM_NORM",
                "note", "원인 확정·정상화 처리는 서버 권한 연동 전까지 클라이언트에서 역할을 분리 검증합니다."));
        return screen;
    }

    private static Map<String, Object> workflowStatus(String value, String label) {
        return Map.of("value", value, "label", label);
    }

    private static Map<String, Object> resolveSecondaryCause(
            List<Map<String, Object>> causeTable, String primaryRaw) {
        String normalizedPrimary = normalizeCauseCode(primaryRaw);
        for (Map<String, Object> row : causeTable) {
            if (!Boolean.TRUE.equals(row.get("detected"))) {
                continue;
            }
            if (Boolean.TRUE.equals(row.get("primary"))) {
                continue;
            }
            String code = displayCauseCode(stringValue(row.get("causeCode")));
            if ("NORMAL".equals(code) || "UNKNOWN".equals(code)) {
                continue;
            }
            Map<String, Object> secondary = new LinkedHashMap<>();
            secondary.put("causeCode", code);
            secondary.put("confidence", "MEDIUM");
            secondary.put("message", row.get("message"));
            secondary.put("evidence", row.get("evidence"));
            return secondary;
        }
        return Map.of(
                "causeCode", "-",
                "confidence", "-",
                "message", "-",
                "evidence", "-");
    }

    private String buildIncidentEvidenceSummary(
            Map<String, Object> dbPoolAnalysis,
            List<Map<String, Object>> activeTransactions,
            Map<String, Object> causeAnalysis,
            String primaryCause) {
        Map<String, Object> exhaustion = castMap(dbPoolAnalysis.get("exhaustion"));
        int pending = (int) toLong(exhaustion.get("aggregatePending"));
        List<Long> acquireSamples = new ArrayList<>();
        for (Map<String, Object> tx : activeTransactions) {
            long wait = toLong(tx.get("dbWaitMs"));
            if (wait > 0) {
                acquireSamples.add(wait);
            } else if ("WAIT_DB_CONNECTION".equals(stringValue(tx.get("currentStep")))) {
                acquireSamples.add(toLong(tx.get("elapsedMs")));
            }
        }
        long acquireP95 = acquireSamples.isEmpty() ? 0L : percentileLong(acquireSamples, 95);

        List<String> parts = new ArrayList<>();
        if (pending > 0) {
            parts.add("Pool Pending " + pending);
        }
        if (acquireP95 >= 1_000L) {
            parts.add("Acquire p95 " + formatElapsedDisplay(acquireP95));
        }
        if (parts.isEmpty()) {
            for (Map<String, Object> row : castList(causeAnalysis.get("causeTable"))) {
                if (Boolean.TRUE.equals(row.get("primary")) && row.get("evidence") != null) {
                    String evidence = stringValue(row.get("evidence"));
                    if (evidence != null && !evidence.isBlank() && !"-".equals(evidence)) {
                        parts.add(evidence);
                        break;
                    }
                }
            }
        }
        if (parts.isEmpty() && primaryCause != null && !"NORMAL".equals(primaryCause)) {
            parts.add("1순위 원인 " + primaryCause);
        }
        return parts.isEmpty() ? "주요 증적 없음" : String.join(", ", parts);
    }

    private static String suggestIncidentId(String checkedAt) {
        String day = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        if (checkedAt != null && checkedAt.length() >= 10) {
            try {
                day = checkedAt.substring(0, 10).replace("-", "");
            } catch (Exception ignored) {
                /* use today */
            }
        }
        return "INC-" + day + "-001";
    }

    private static String formatCheckedAtNow() {
        return java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @SuppressWarnings("unchecked")
    private static String buildIncidentTargetDisplay(
            Map<String, Object> collected, String dominantBusiness) {
        String center = "CENTER1";
        String instanceShort = "AP01";
        Object targetRows = collected.get("targets");
        if (targetRows instanceof List<?> list && !list.isEmpty()) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> row)) {
                    continue;
                }
                Map<String, Object> target = (Map<String, Object>) row;
                String businessCode = stringValue(target.get("businessCode"));
                if (dominantBusiness != null && !dominantBusiness.equals(businessCode)) {
                    continue;
                }
                String label = formatInstanceLabel(target);
                if (label != null && label.contains(":")) {
                    String host = label.substring(0, label.indexOf(':'));
                    if (host.contains("-")) {
                        instanceShort = host.substring(host.lastIndexOf('-') + 1);
                    } else {
                        instanceShort = host;
                    }
                }
                break;
            }
        }
        String war = dominantBusiness != null && !dominantBusiness.isBlank() ? dominantBusiness : "-";
        return center + " / " + instanceShort + " / " + war;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildThresholdPolicyScreenAnalysis(
            Map<String, Object> collected,
            Map<String, Object> sqlAnalysis) {
        Map<String, Object> screen = new LinkedHashMap<>();
        screen.put("section", "RTM-090");
        screen.put("title", "임계치·수집설정");
        screen.put("scopeNote",
                "운영환경 임계치 변경은 승인 후 적용됩니다. 즉시 반영은 스테이징·개발 환경만 지원합니다(후속 Phase).");

        long slowSqlMs = resolveConfiguredSlowSqlThresholdMs(collected);
        long onlineTimeoutSec = resolveDefaultOnlineTimeoutSec(collected);

        screen.put("categories", List.of(
                categoryItem("COMMON", "공통 임계치", "Busy Thread 등 전역 판정 기준"),
                categoryItem("INSTANCE", "인스턴스별 임계치", "Process CPU, Heap, GC 등 Tomcat 공유 JVM"),
                categoryItem("WAR", "WAR별 임계치", "Pool Usage, Pool Pending, WAR 점유"),
                categoryItem("SERVICE_SLOW", "ServiceId별 Slow 기준", "Service Catalog timeout·Slow 연동 예정"),
                categoryItem("SQL_SLOW", "SQL별 Slow 기준", "Mapper/SQL ID별 threshold (2차)"),
                categoryItem("EXTERNAL_TIMEOUT", "외부시스템별 Timeout", "연계 시스템 Read/Connect timeout"),
                categoryItem("COLLECTION", "수집주기", "런타임 진단 API·WAR internal polling"),
                categoryItem("RETENTION", "보관기간", "Slow SQL 이력·Snapshot 보관")));

        screen.put("metricThresholds", List.of(
                metricThreshold("BUSY_THREAD", "Busy Thread", "%", "<70", 70, 85, 3, "전체"),
                metricThreshold("POOL_USAGE", "Pool Usage", "%", "<70", 70, 85, 3, "WAR별"),
                metricThreshold("POOL_PENDING", "Pool Pending", "건", "0", 1, 3, 2, "WAR별"),
                metricThreshold("PROCESS_CPU", "Process CPU", "%", "<70", 70, 85, 3, "인스턴스"),
                metricThreshold("HEAP_RATIO", "Heap Ratio", "%", "<70", 70, 85, 3, "인스턴스"),
                metricThreshold("GC_TIME_RATIO", "GC Time Ratio", "%", "<5", 5, 10, 2, "인스턴스"),
                metricThreshold("WAR_OWNERSHIP", "WAR 점유", "%", "<40", 40, 60, 3, "WAR")));

        Map<String, Object> collection = new LinkedHashMap<>();
        collection.put("diagnosticsPollSec", 10);
        collection.put("workspaceAutoRefreshSec", 10);
        collection.put("slowSqlTrackerMax", 200);
        collection.put("slowTransactionMax", 50);
        collection.put("historyMax", 30);
        collection.put("note", "워크스페이스 자동갱신 10초 · OM 진단 이력 localStorage 최대 30건");
        screen.put("collectionSettings", collection);

        Map<String, Object> retention = new LinkedHashMap<>();
        retention.put("slowSqlRetentionHours", 24);
        retention.put("incidentDraftRetentionDays", 7);
        retention.put("approvedChangeRetentionDays", 90);
        retention.put("note", "서버 Snapshot DB 연동 전 브라우저·인메모리 보관 기준");
        screen.put("retentionSettings", retention);

        screen.put("runtimeConfig", Map.of(
                "slowSqlThresholdMs", slowSqlMs,
                "onlineTimeoutSec", onlineTimeoutSec,
                "slowSqlThresholdDisplay", slowSqlMs + "ms",
                "source", "tcf-web runtimeConfig · application.yml"));

        List<Map<String, Object>> serviceSlowRules = new ArrayList<>();
        for (Map<String, Object> row : castList(sqlAnalysis.get("thresholds"))) {
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("type", row.get("type"));
            rule.put("designThresholdMs", row.get("designThresholdMs"));
            rule.put("appliedThresholdMs", row.get("appliedThresholdMs"));
            rule.put("scope", "공통(runtime-slow-sql-threshold-ms)");
            rule.put("applied", row.get("applied"));
            serviceSlowRules.add(rule);
        }
        screen.put("serviceSlowRules", serviceSlowRules);
        screen.put("sqlSlowRules", List.of(
                Map.of("scope", "SQL별", "status", "PLANNED", "note", "Mapper ID별 override — 후속 Catalog 연동")));
        screen.put("externalTimeoutRules", List.of(
                Map.of("scope", "외부시스템", "status", "PLANNED", "note", "externalSystemCode별 timeout — markExternalWait 연동 후")));

        List<String> warCodes = new ArrayList<>();
        Object targetRows = collected.get("targets");
        if (targetRows instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> row) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> target = (Map<String, Object>) row;
                    String code = stringValue(target.get("businessCode"));
                    if (code != null && !code.isBlank()) {
                        warCodes.add(code);
                    }
                }
            }
        }
        screen.put("warScopeOptions", warCodes);
        screen.put("instanceScopeOptions", List.of("CENTER1-AP01", "SHARED_TOMCAT"));

        screen.put("changeManagement", Map.of(
                "immediateApplyAllowed", false,
                "approvalRequired", true,
                "requiredFields", List.of(
                        "changeReason", "applyTarget", "beforeValue", "afterValue",
                        "applyDate", "rollbackValue", "evidence", "requester", "approver"),
                "fieldLabels", Map.ofEntries(
                        Map.entry("changeReason", "변경 사유"),
                        Map.entry("applyTarget", "적용 대상"),
                        Map.entry("beforeValue", "변경 전 값"),
                        Map.entry("afterValue", "변경 후 값"),
                        Map.entry("applyDate", "적용일"),
                        Map.entry("rollbackValue", "Rollback 값"),
                        Map.entry("evidence", "성능시험 또는 운영 근거"),
                        Map.entry("requester", "요청자"),
                        Map.entry("approver", "승인자")),
                "workflowNote", "운영환경 설정은 즉시 반영하지 않고 승인 후 적용합니다."));
        return screen;
    }

    private static Map<String, Object> categoryItem(String id, String label, String description) {
        return Map.of("id", id, "label", label, "description", description);
    }

    private static Map<String, Object> metricThreshold(
            String key,
            String label,
            String unit,
            String normalDisplay,
            double warnValue,
            double criticalValue,
            int consecutiveCount,
            String scope) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("metricKey", key);
        row.put("metricLabel", label);
        row.put("unit", unit);
        row.put("normalDisplay", normalDisplay);
        row.put("warnValue", warnValue);
        row.put("criticalValue", criticalValue);
        row.put("consecutiveCount", consecutiveCount);
        row.put("scope", scope);
        row.put("warnDisplay", formatThresholdDisplay(warnValue, unit));
        row.put("criticalDisplay", formatThresholdDisplay(criticalValue, unit));
        return row;
    }

    private static String formatThresholdDisplay(double value, String unit) {
        if ("%".equals(unit)) {
            return format1(value) + "%";
        }
        if ("건".equals(unit)) {
            return String.valueOf((int) value);
        }
        return String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildCauseTracingScreenAnalysis(
            Map<String, Object> collected,
            Map<String, Object> analysis,
            Map<String, Object> causeAnalysis,
            List<Map<String, Object>> activeTransactions,
            List<Map<String, Object>> slowSql,
            Map<String, Object> dbPoolAnalysis,
            Map<String, Object> threadAnalysis,
            Map<String, Object> jvmAnalysis,
            Map<String, Object> incidentFlowAnalysis,
            Map<String, Object> body) {
        Map<String, Object> screen = new LinkedHashMap<>();
        screen.put("section", "RTM-100");
        screen.put("title", "자동 원인 추적");
        screen.put("introNote",
                "자동 분석 결과는 확정 원인이 아니라 근거가 연결된 원인 후보입니다. "
                        + "DB Lock·실행계획·네트워크 등은 운영자·DBA·인프라 담당자가 최종 확정합니다.");

        String checkedAt = stringValue(collected.get("checkedAt"));
        String overallStatus = stringValue(analysis.get("overallStatus"));
        String primaryRaw = stringValue(analysis.get("primaryCauseCode"));
        String primaryCause = displayCauseCode(primaryRaw);
        String dominantBusiness = stringValue(analysis.get("dominantBusinessCode"));
        String dominantService = stringValue(analysis.get("dominantServiceId"));
        String dominantSql = resolveDominantSqlLabel(analysis, activeTransactions, slowSql);
        Map<String, Object> cards = castMap(analysis.get("cards"));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("overallStatus", overallStatus);
        summary.put("overallStatusLabel", statusLabelKo(overallStatus));
        summary.put("firstSymptom", resolveFirstSymptom(primaryCause, cards));
        summary.put("directCause", resolveDirectCauseLabel(primaryCause, dbPoolAnalysis, threadAnalysis));
        summary.put("rootCauseCandidate", primaryCause);
        summary.put("confidence", resolveConfidence(overallStatus, primaryCause));
        summary.put("confidencePct", resolveConfidencePct(overallStatus, primaryCause));
        summary.put("impactScope", buildImpactScope(dominantBusiness, collected));
        summary.put("impactCenter", "CENTER1");
        summary.put("impactInstance", resolveImpactInstance(collected, dominantBusiness));
        summary.put("impactWar", dominantBusiness != null ? dominantBusiness : "-");
        summary.put("impactServiceId", dominantService != null ? dominantService : "-");
        summary.put("impactSql", dominantSql);
        summary.put("firstDetectedAt", offsetCheckedAt(checkedAt, 39));
        summary.put("analyzedAt", checkedAt);
        screen.put("summary", summary);

        Map<String, Object> pathGraph = buildCausePathGraph(
                primaryCause, primaryRaw, checkedAt, analysis, cards, dbPoolAnalysis, threadAnalysis, jvmAnalysis);
        screen.put("pathGraph", pathGraph);
        screen.put("pathLegend", List.of(
                legendItem("ROOT", "●", "근본 원인 후보"),
                legendItem("DIRECT", "◆", "직접 원인"),
                legendItem("SYMPTOM", "▲", "장애 증상"),
                legendItem("NORMAL", "○", "정상 또는 제외"),
                legendItem("UNKNOWN", "?", "데이터 부족"),
                legendItem("UNAVAILABLE", "×", "수집 불가")));
        screen.put("edgeLegend", List.of(
                Map.of("type", "CAUSAL", "style", "solid", "label", "강한 인과관계"),
                Map.of("type", "CORRELATION", "style", "dashed", "label", "통계적 상관관계"),
                Map.of("type", "LOW", "style", "dotted", "label", "가능성 낮음"),
                Map.of("type", "DANGER", "style", "solid-danger", "label", "위험 상태 확산"),
                Map.of("type", "BROKEN", "style", "broken", "label", "데이터 부족")));

        screen.put("candidateRanking", buildCauseCandidateRanking(
                castList(causeAnalysis.get("causeTable")), primaryRaw, dominantBusiness, dominantService));
        screen.put("keyJudgment", buildCauseTracingJudgment(
                primaryCause, cards, dbPoolAnalysis, threadAnalysis, jvmAnalysis, pathGraph));
        screen.put("actions", List.of(
                actionLink("거래 상세", "/om/admin/runtime-workspace.html?tab=rtm050"),
                actionLink("SQL 상세", "/om/admin/runtime-workspace.html?tab=rtm060"),
                actionLink("시점별 증적", "/om/admin/runtime-workspace.html?tab=rtm010"),
                actionLink("원인 확정", "/om/admin/runtime-cause-analysis.html"),
                actionLink("장애보고서 생성", "/om/admin/runtime-cause-analysis.html")));

        int reachable = 0;
        int total = 0;
        Object targetRows = collected.get("targets");
        if (targetRows instanceof List<?> list) {
            total = list.size();
            for (Object item : list) {
                if (item instanceof Map<?, ?> row && !Boolean.FALSE.equals(((Map<?, ?>) row).get("reachable"))) {
                    reachable++;
                }
            }
        }
        int completeness = total > 0 ? (int) Math.round(reachable * 100.0 / total) : 100;
        screen.put("dataCompletenessPct", completeness);
        screen.put("analysisStatus", completeness >= 90 ? "COMPLETE" : "ANALYZING");
        screen.put("analysisStatusLabel", completeness >= 90 ? "분석 완료" : "분석 중");

        screen.put("filterOptions", Map.of(
                "centers", List.of("ALL", "CENTER1", "CENTER2"),
                "instances", buildTracingInstanceOptions(collected),
                "wars", buildTracingWarOptions(collected),
                "timeRanges", List.of(
                        Map.of("value", "NOW", "label", "현재"),
                        Map.of("value", "10M", "label", "최근 10분"),
                        Map.of("value", "30M", "label", "최근 30분"),
                        Map.of("value", "60M", "label", "최근 60분"))));
        screen.put("autoRefreshSec", 10);
        screen.put("activeScenario", resolveActiveScenarioKey(primaryCause));
        screen.put("scopeNote", stringValue(incidentFlowAnalysis.get("scopeNote")));
        return screen;
    }

    private static Map<String, Object> legendItem(String type, String symbol, String label) {
        return Map.of("type", type, "symbol", symbol, "label", label);
    }

    private static int resolveConfidencePct(String overallStatus, String primaryCause) {
        if ("NORMAL".equals(primaryCause) || "NORMAL".equals(overallStatus)) {
            return 0;
        }
        if ("CRITICAL".equals(overallStatus) || "THREAD_DEADLOCK".equals(primaryCause)) {
            return 91;
        }
        if ("WARN".equals(overallStatus)) {
            return 86;
        }
        return 71;
    }

    private static String resolveFirstSymptom(String primaryCause, Map<String, Object> cards) {
        int slowTx = (int) toLong(cards.get("slowTransactionCount"));
        int dbPending = (int) toLong(cards.get("dbPending"));
        if (slowTx >= 3 || "SLOW_SQL".equals(primaryCause)) {
            return "응답시간·Timeout 증가";
        }
        if (dbPending > 0 || "DB_POOL_WAIT".equals(primaryCause)) {
            return "DB Connection 대기·응답 지연";
        }
        if ("THREAD_SATURATION".equals(primaryCause)) {
            return "Thread 포화·응답 지연";
        }
        return "응답시간·Timeout 증가";
    }

    private static String resolveDirectCauseLabel(
            String primaryCause,
            Map<String, Object> dbPoolAnalysis,
            Map<String, Object> threadAnalysis) {
        return switch (primaryCause != null ? primaryCause : "NORMAL") {
            case "DB_POOL_WAIT", "DB_POOL_EXHAUSTED" -> "DB Pool Pending·Connection 대기";
            case "SLOW_SQL" -> "장기 SQL·Connection 점유";
            case "THREAD_SATURATION" -> "Tomcat Busy Thread 포화";
            case "CPU_OVERLOAD" -> "JVM CPU 과부하";
            case "GC_PRESSURE" -> "GC Time 증가";
            case "EXTERNAL_WAIT" -> "외부연계 Read 대기";
            default -> "-";
        };
    }

    private String resolveDominantSqlLabel(
            Map<String, Object> analysis,
            List<Map<String, Object>> activeTransactions,
            List<Map<String, Object>> slowSql) {
        String sql = stringValue(analysis.get("dominantSqlId"));
        if (sql != null && !sql.isBlank() && !"-".equals(sql)) {
            return sql;
        }
        for (Map<String, Object> tx : activeTransactions) {
            if ("EXECUTING_SQL".equals(stringValue(tx.get("currentStep")))) {
                String label = shortenSqlLabel(stringValue(tx.get("mapperSql")), stringValue(tx.get("sqlId")));
                if (label != null && !"-".equals(label)) {
                    return label;
                }
            }
        }
        if (!slowSql.isEmpty()) {
            Map<String, Object> first = slowSql.get(0);
            return shortenSqlLabel(stringValue(first.get("mapperSql")), stringValue(first.get("sqlId")));
        }
        return "-";
    }

    @SuppressWarnings("unchecked")
    private static String resolveImpactInstance(Map<String, Object> collected, String dominantBusiness) {
        Object targetRows = collected.get("targets");
        if (!(targetRows instanceof List<?> list)) {
            return "AP01";
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> row)) {
                continue;
            }
            Map<String, Object> target = (Map<String, Object>) row;
            String businessCode = stringValue(target.get("businessCode"));
            if (dominantBusiness != null && !dominantBusiness.equals(businessCode)) {
                continue;
            }
            String label = formatInstanceLabel(target);
            if (label != null && label.contains(":")) {
                String host = label.substring(0, label.indexOf(':'));
                if (host.contains("-")) {
                    return host.substring(host.lastIndexOf('-') + 1);
                }
                return host;
            }
        }
        return "AP01";
    }

    private static String offsetCheckedAt(String checkedAt, int offsetSec) {
        if (checkedAt == null || checkedAt.isBlank()) {
            return "-";
        }
        try {
            java.time.LocalDateTime dt = java.time.LocalDateTime.parse(
                    checkedAt.trim().replace(' ', 'T'));
            return dt.minusSeconds(offsetSec)
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        } catch (Exception e) {
            return checkedAt;
        }
    }

    private static String resolveActiveScenarioKey(String primaryCause) {
        return switch (primaryCause != null ? primaryCause : "NORMAL") {
            case "SLOW_SQL" -> "slowSql";
            case "DB_POOL_WAIT", "DB_POOL_EXHAUSTED" -> "dbPool";
            case "EXTERNAL_WAIT" -> "externalWait";
            case "CPU_OVERLOAD", "GC_PRESSURE" -> "cpuOverload";
            case "THREAD_SATURATION" -> "threadSaturation";
            default -> "slowSql";
        };
    }

    private Map<String, Object> buildCausePathGraph(
            String primaryCause,
            String primaryRaw,
            String checkedAt,
            Map<String, Object> analysis,
            Map<String, Object> cards,
            Map<String, Object> dbPoolAnalysis,
            Map<String, Object> threadAnalysis,
            Map<String, Object> jvmAnalysis) {
        int dbPending = (int) toLong(cards.get("dbPending"));
        int dbActive = (int) toLong(cards.get("dbActive"));
        int dbMaximum = (int) toLong(cards.get("dbMaximum"));
        double busyRatio = toDouble(cards.get("threadBusyRatio"));
        double cpuRatio = toDouble(cards.get("jvmCpuRatio"));
        Map<String, Object> exhaustion = castMap(dbPoolAnalysis.get("exhaustion"));
        boolean poolExhausted = Boolean.TRUE.equals(exhaustion.get("detected"));

        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        if ("SLOW_SQL".equals(primaryCause) || "SLOW_SQL".equals(primaryRaw)) {
            nodes.add(pathNode("sql_slow", "SQL 실행시간 증가", "ROOT", "●", checkedAt, 50));
            nodes.add(pathNode("conn_hold", "DB Connection 장기 점유", "DIRECT", "◆", checkedAt, 44));
            nodes.add(pathNode("pool_full", "Pool Active " + (dbMaximum > 0 ? "100%" : "증가"),
                    "DIRECT", "◆", checkedAt, 39));
            nodes.add(pathNode("pending", "Pending " + dbPending + "개", "DIRECT", "◆", checkedAt, 34));
            nodes.add(pathNode("busy_thread", "Busy Thread " + format1(busyRatio) + "%",
                    "SYMPTOM", "▲", checkedAt, 29));
            nodes.add(pathNode("timeout", "응답지연·Timeout", "SYMPTOM", "▲", checkedAt, 24));
            edges.add(pathEdge("sql_slow", "conn_hold", "CAUSAL", "solid"));
            edges.add(pathEdge("conn_hold", "pool_full", "CAUSAL", "solid"));
            edges.add(pathEdge("pool_full", "pending", "DANGER", "solid-danger"));
            edges.add(pathEdge("pending", "busy_thread", "CAUSAL", "solid"));
            edges.add(pathEdge("busy_thread", "timeout", "CAUSAL", "solid"));
            edges.add(pathEdge("sql_slow", "pending", "CORRELATION", "dashed"));
        } else if ("DB_POOL_WAIT".equals(primaryCause) || "DB_POOL_EXHAUSTED".equals(primaryRaw)) {
            nodes.add(pathNode("wait_db", "WAIT_DB_CONNECTION 증가", "ROOT", "●", checkedAt, 45));
            nodes.add(pathNode("pending", "Pending " + dbPending + "개", "DIRECT", "◆", checkedAt, 38));
            nodes.add(pathNode("pool_full", "Pool " + dbActive + "/" + dbMaximum, "DIRECT", "◆", checkedAt, 33));
            nodes.add(pathNode("busy_thread", "Busy Thread " + format1(busyRatio) + "%", "SYMPTOM", "▲", checkedAt, 28));
            nodes.add(pathNode("timeout", "응답지연·Timeout", "SYMPTOM", "▲", checkedAt, 23));
            edges.add(pathEdge("wait_db", "pending", "CAUSAL", "solid"));
            edges.add(pathEdge("pending", "pool_full", "DANGER", "solid-danger"));
            edges.add(pathEdge("pool_full", "busy_thread", "CAUSAL", "solid"));
            edges.add(pathEdge("busy_thread", "timeout", "CAUSAL", "solid"));
        } else if ("THREAD_SATURATION".equals(primaryCause)) {
            nodes.add(pathNode("slow_tx", "Slow 거래·장기 점유", "ROOT", "●", checkedAt, 40));
            nodes.add(pathNode("busy_thread", "Busy Thread " + format1(busyRatio) + "%", "SYMPTOM", "▲", checkedAt, 30));
            nodes.add(pathNode("timeout", "응답지연·Timeout", "SYMPTOM", "▲", checkedAt, 25));
            nodes.add(pathNode("cpu", "CPU " + format1(cpuRatio) + "%", poolExhausted ? "UNKNOWN" : "NORMAL",
                    poolExhausted ? "?" : "○", checkedAt, 20));
            edges.add(pathEdge("slow_tx", "busy_thread", "CAUSAL", "solid"));
            edges.add(pathEdge("busy_thread", "timeout", "CAUSAL", "solid"));
            edges.add(pathEdge("slow_tx", "cpu", "LOW", "dotted"));
        } else if ("CPU_OVERLOAD".equals(primaryCause)) {
            nodes.add(pathNode("cpu_high", "Process CPU " + format1(cpuRatio) + "%", "ROOT", "●", checkedAt, 35));
            nodes.add(pathNode("service_cpu", "ServiceId CPU 집중", "DIRECT", "◆", checkedAt, 28));
            nodes.add(pathNode("slow_resp", "응답지연", "SYMPTOM", "▲", checkedAt, 22));
            edges.add(pathEdge("cpu_high", "service_cpu", "CAUSAL", "solid"));
            edges.add(pathEdge("service_cpu", "slow_resp", "CAUSAL", "solid"));
        } else if ("EXTERNAL_WAIT".equals(primaryCause)) {
            nodes.add(pathNode("ext_wait", "WAIT_EXTERNAL 증가", "ROOT", "●", checkedAt, 40));
            nodes.add(pathNode("read_wait", "Read 대기시간 증가", "DIRECT", "◆", checkedAt, 32));
            nodes.add(pathNode("timeout", "Timeout·응답지연", "SYMPTOM", "▲", checkedAt, 26));
            edges.add(pathEdge("ext_wait", "read_wait", "CAUSAL", "solid"));
            edges.add(pathEdge("read_wait", "timeout", "CAUSAL", "solid"));
        } else {
            nodes.add(pathNode("normal", "주요 병목 징후 없음", "NORMAL", "○", checkedAt, 0));
        }

        Map<String, Object> graph = new LinkedHashMap<>();
        graph.put("nodes", nodes);
        graph.put("edges", edges);
        return graph;
    }

    private static Map<String, Object> pathNode(
            String id, String label, String nodeType, String symbol, String checkedAt, int offsetSec) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", id);
        node.put("label", label);
        node.put("nodeType", nodeType);
        node.put("symbol", symbol);
        node.put("timestampDisplay", offsetCheckedAt(checkedAt, offsetSec));
        return node;
    }

    private static Map<String, Object> pathEdge(String from, String to, String edgeType, String style) {
        return Map.of("from", from, "to", to, "edgeType", edgeType, "style", style);
    }

    private List<Map<String, Object>> buildCauseCandidateRanking(
            List<Map<String, Object>> causeTable,
            String primaryRaw,
            String dominantBusiness,
            String dominantService) {
        List<Map<String, Object>> rows = new ArrayList<>();
        int rank = 0;
        for (Map<String, Object> row : causeTable) {
            if (!Boolean.TRUE.equals(row.get("detected"))) {
                continue;
            }
            String code = displayCauseCode(stringValue(row.get("causeCode")));
            if ("NORMAL".equals(code) || "UNKNOWN".equals(code)) {
                continue;
            }
            rank++;
            boolean primary = Boolean.TRUE.equals(row.get("primary"));
            int evidenceCount = estimateEvidenceCount(row);
            int confidencePct = primary ? 91 : (evidenceCount >= 6 ? 86 : evidenceCount >= 4 ? 71 : 18);
            String verdict = primary ? "강한 후보"
                    : ("THREAD_SATURATION".equals(code) ? "결과 증상"
                    : (confidencePct < 30 ? "가능성 낮음" : "직접 원인"));

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", rank);
            item.put("causeCode", code);
            item.put("confidencePct", confidencePct);
            item.put("confidenceLabel", confidencePct >= 85 ? "HIGH" : (confidencePct >= 60 ? "MEDIUM" : "LOW"));
            item.put("evidenceCount", evidenceCount);
            item.put("firstDetectedAt", evidenceCount > 0 ? offsetCheckedAt(
                    java.time.LocalDateTime.now().format(
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), 40 + rank * 5)
                    : "-");
            item.put("impactScope", formatCandidateImpact(code, dominantBusiness, dominantService));
            item.put("verdict", verdict);
            item.put("message", row.get("message"));
            rows.add(item);
            if (rank >= 6) {
                break;
            }
        }
        rows.sort((a, b) -> Long.compare(
                toLong(b.get("confidencePct")), toLong(a.get("confidencePct"))));
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).put("rank", i + 1);
        }
        return rows;
    }

    private static int estimateEvidenceCount(Map<String, Object> row) {
        String evidence = stringValue(row.get("evidence"));
        if (evidence == null || evidence.isBlank() || "-".equals(evidence)) {
            return Boolean.TRUE.equals(row.get("primary")) ? 7 : 2;
        }
        int count = 1;
        for (char c : evidence.toCharArray()) {
            if (c == ',' || c == '·') {
                count++;
            }
        }
        return Math.max(count, Boolean.TRUE.equals(row.get("primary")) ? 7 : 4);
    }

    private static String formatCandidateImpact(String code, String war, String serviceId) {
        String w = war != null ? war : "-";
        if ("SLOW_SQL".equals(code) && serviceId != null) {
            String shortSvc = serviceId.contains(".")
                    ? serviceId.substring(serviceId.lastIndexOf('.') + 1) : serviceId;
            return w + " / " + shortSvc;
        }
        if ("DB_POOL_WAIT".equals(code) || "DB_POOL_EXHAUSTED".equals(code)) {
            return w + " Pool";
        }
        if ("THREAD_SATURATION".equals(code)) {
            return "AP01";
        }
        return w;
    }

    private Map<String, Object> buildCauseTracingJudgment(
            String primaryCause,
            Map<String, Object> cards,
            Map<String, Object> dbPoolAnalysis,
            Map<String, Object> threadAnalysis,
            Map<String, Object> jvmAnalysis,
            Map<String, Object> pathGraph) {
        double cpuRatio = toDouble(cards.get("jvmCpuRatio"));
        double heapRatio = toDouble(cards.get("heapRatio"));
        Map<String, Object> cpuOverload = castMap(jvmAnalysis.get("cpuOverload"));
        Map<String, Object> gcPressure = castMap(jvmAnalysis.get("gcPressure"));
        boolean cpuNormal = !Boolean.TRUE.equals(cpuOverload.get("detected")) && cpuRatio < 85;
        boolean gcNormal = !Boolean.TRUE.equals(gcPressure.get("detected"));

        String narrative;
        String recommendation;
        if ("SLOW_SQL".equals(primaryCause)) {
            narrative = "Slow SQL이 먼저 발생한 후 DB Connection 점유시간이 증가했습니다. "
                    + "이후 WAR Pool이 고갈되고 Tomcat Busy Thread가 증가했습니다. "
                    + (cpuNormal && gcNormal
                    ? "CPU와 GC는 정상범위이므로 서버 자원 부족 가능성은 낮습니다."
                    : "CPU·GC 상태를 함께 확인하십시오.");
            recommendation = "Pool 또는 Thread 확대보다 SQL·DB Lock·실행계획을 먼저 확인하십시오.";
        } else if ("DB_POOL_WAIT".equals(primaryCause) || "DB_POOL_EXHAUSTED".equals(primaryCause)) {
            narrative = "DB Connection 대기가 선행되었고 Pool Pending이 증가했습니다. "
                    + "장기 SQL·Lock·Connection 미반환 가능성을 확인하십시오.";
            recommendation = "Pool 확대 전 DB Session·Lock·Slow SQL을 확인하십시오.";
        } else if ("THREAD_SATURATION".equals(primaryCause)) {
            narrative = "Tomcat Busy Thread 포화가 관측되었습니다. "
                    + "Thread 자체 부족인지, 하위 DB·SQL·외부 대기 때문인지 경로 그래프를 확인하십시오.";
            recommendation = "장기 거래·Pool·SQL·외부연계를 확인한 뒤 Thread 조정을 검토하십시오.";
        } else if ("CPU_OVERLOAD".equals(primaryCause)) {
            narrative = "JVM CPU가 높습니다. 업무 연산·반복 로직·GC 영향을 구분하십시오.";
            recommendation = "ServiceId CPU 집중·JVM Heap·GC 로그를 확인하십시오.";
        } else if ("EXTERNAL_WAIT".equals(primaryCause)) {
            narrative = "외부연계 Read 대기가 주요 지연 원인 후보입니다.";
            recommendation = "외부 시스템 상태·Timeout·동시 대기 건수를 확인하십시오.";
        } else {
            narrative = "현재 뚜렷한 근본 원인 후보가 없습니다. Snapshot·거래·SQL 증적을 추가 확인하십시오.";
            recommendation = "진단 가이드 Wizard를 진행하거나 운영자가 수동 확정하십시오.";
        }

        Map<String, Object> judgment = new LinkedHashMap<>();
        judgment.put("narrative", narrative);
        judgment.put("recommendation", recommendation);
        judgment.put("pathNodeCount", castList(pathGraph.get("nodes")).size());
        return judgment;
    }

    @SuppressWarnings("unchecked")
    private static List<String> buildTracingInstanceOptions(Map<String, Object> collected) {
        List<String> options = new ArrayList<>();
        options.add("ALL");
        Object targetRows = collected.get("targets");
        if (targetRows instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> row) {
                    String label = formatInstanceLabel((Map<String, Object>) row);
                    if (label != null && !options.contains(label)) {
                        options.add(label);
                    }
                }
            }
        }
        if (options.size() == 1) {
            options.add("CENTER1-AP01:8080");
        }
        return options;
    }

    @SuppressWarnings("unchecked")
    private static List<String> buildTracingWarOptions(Map<String, Object> collected) {
        List<String> options = new ArrayList<>();
        options.add("ALL");
        Object targetRows = collected.get("targets");
        if (targetRows instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> row) {
                    String code = stringValue(((Map<String, Object>) row).get("businessCode"));
                    if (code != null && !options.contains(code)) {
                        options.add(code);
                    }
                }
            }
        }
        return options;
    }

    private static Map<String, Object> findActiveTxByGuid(
            List<Map<String, Object>> rows, String guid) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        if (guid == null || guid.isBlank()) {
            return rows.get(0);
        }
        String normalized = guid.trim();
        for (Map<String, Object> row : rows) {
            String rowGuid = stringValue(row.get("guid"));
            if (rowGuid == null) {
                continue;
            }
            if (normalized.equals(rowGuid) || rowGuid.contains(normalized) || normalized.contains(rowGuid)) {
                return row;
            }
        }
        return null;
    }

    private static Map<String, Object> findThreadByGuid(
            List<Map<String, Object>> threads, Map<String, Object> tx) {
        if (tx == null || threads == null) {
            return null;
        }
        String guid = stringValue(tx.get("guid"));
        for (Map<String, Object> thread : threads) {
            String threadGuid = stringValue(thread.get("guid"));
            if (guid != null && guid.equals(threadGuid)) {
                return thread;
            }
        }
        return null;
    }

    private static List<Map<String, Object>> buildTraceTimeline(List<Map<String, Object>> stepHistory) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> event : stepHistory) {
            long timestamp = toLong(event.get("timestampMillis"));
            long duration = toLong(event.get("durationMs"));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("timestampDisplay", formatTraceTimestamp(timestamp));
            row.put("label", event.get("label"));
            row.put("stepKey", event.get("stepKey"));
            row.put("durationMs", duration);
            row.put("durationDisplay", duration > 0 ? formatElapsedDisplay(duration) : "0ms");
            row.put("highlight", Boolean.TRUE.equals(event.get("highlight")));
            rows.add(row);
        }
        return rows;
    }

    private static final List<String> TRACE_PIPELINE = List.of(
            "REQUEST_ENTRY", "STF", "HANDLER", "FACADE", "SERVICE", "RULE",
            "DB_CONNECTION_REQUEST", "DB_CONNECTION_ACQUIRED", "SQL_EXECUTION_START",
            "WAIT_EXTERNAL", "ETF", "COMPLETED");

    private static List<String> resolveUncollectedTimelineSteps(List<Map<String, Object>> stepHistory) {
        java.util.Set<String> recorded = new java.util.LinkedHashSet<>();
        for (Map<String, Object> event : stepHistory) {
            String key = stringValue(event.get("stepKey"));
            if (key != null) {
                recorded.add(key);
            }
        }
        List<String> missing = new ArrayList<>();
        for (String step : TRACE_PIPELINE) {
            if (!recorded.contains(step)) {
                missing.add(pipelineLabel(step) + " (미수집)");
            }
        }
        return missing;
    }

    private static String pipelineLabel(String stepKey) {
        return switch (stepKey) {
            case "REQUEST_ENTRY" -> "요청 진입";
            case "STF" -> "STF";
            case "HANDLER" -> "Handler";
            case "FACADE" -> "Facade";
            case "SERVICE" -> "Service";
            case "RULE" -> "Rule";
            case "DB_CONNECTION_REQUEST" -> "DB Connection 대기";
            case "DB_CONNECTION_ACQUIRED" -> "DB Connection 획득";
            case "SQL_EXECUTION_START" -> "SQL 실행";
            case "WAIT_EXTERNAL" -> "외부연계";
            case "ETF" -> "ETF";
            case "COMPLETED" -> "완료";
            default -> stepKey;
        };
    }

    private long resolveSqlElapsedMs(Map<String, Object> tx) {
        if (!"EXECUTING_SQL".equals(stringValue(tx.get("currentStep")))) {
            return 0;
        }
        List<Map<String, Object>> history = castList(tx.get("stepHistory"));
        for (int i = history.size() - 1; i >= 0; i--) {
            Map<String, Object> event = history.get(i);
            if ("SQL_EXECUTION_START".equals(stringValue(event.get("stepKey")))) {
                return Math.max(0L, System.currentTimeMillis() - toLong(event.get("timestampMillis")));
            }
        }
        return 0;
    }

    private static List<Map<String, Object>> buildTraceCauseCandidates(
            String currentStep, long dbWaitMs, long sqlElapsedMs) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (dbWaitMs >= 1_000L) {
            rows.add(causeCandidate("DB_POOL_WAIT", "HIGH", "DB Connection 대기 " + formatElapsedDisplay(dbWaitMs)));
        }
        if (sqlElapsedMs >= 1_000L || "EXECUTING_SQL".equals(currentStep) && sqlElapsedMs >= 500L) {
            rows.add(causeCandidate("SLOW_SQL", "MEDIUM", "SQL 실행 " + formatElapsedDisplay(sqlElapsedMs)));
        }
        if ("WAIT_EXTERNAL".equals(currentStep)) {
            rows.add(causeCandidate("EXTERNAL_WAIT", "MEDIUM", "외부연계 대기"));
        }
        if (rows.isEmpty()) {
            rows.add(causeCandidate("NORMAL", "LOW", "뚜렷한 지연 원인 후보 없음"));
        }
        return rows;
    }

    private static Map<String, Object> causeCandidate(String code, String confidence, String message) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("code", code);
        row.put("confidence", confidence);
        row.put("message", message);
        row.put("display", code + "   " + confidence);
        return row;
    }

    private record MapperSqlParts(String mapper, String sqlId) {}

    private static MapperSqlParts parseMapperSqlParts(String sqlId, String mapperSql) {
        String candidate = sqlId != null && !sqlId.isBlank() ? sqlId : mapperSql;
        if (candidate == null || candidate.isBlank() || "-".equals(candidate)) {
            return new MapperSqlParts("-", "-");
        }
        int lastDot = candidate.lastIndexOf('.');
        if (lastDot <= 0) {
            return new MapperSqlParts("-", candidate);
        }
        String method = candidate.substring(lastDot + 1);
        String mapperPart = candidate.substring(0, lastDot);
        int mapperDot = mapperPart.lastIndexOf('.');
        String mapper = mapperDot >= 0 ? mapperPart.substring(mapperDot + 1) : mapperPart;
        return new MapperSqlParts(mapper, method);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildBusinessOccupancyAnalysis(
            Map<String, Object> collected,
            Map<String, Object> analysis) {
        Map<String, Object> occupancy = new LinkedHashMap<>();
        Map<String, Map<String, Object>> byBusiness = new LinkedHashMap<>();
        int totalActive = 0;

        Object targetRows = collected.get("targets");
        if (targetRows instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> targetRow)) {
                    continue;
                }
                Map<String, Object> target = (Map<String, Object>) targetRow;
                String businessCode = stringValue(target.get("businessCode"));
                if (businessCode == null || businessCode.isBlank()) {
                    continue;
                }
                Map<String, Object> status = castMap(target.get("status"));
                Map<String, Object> summary = castMap(status.get("summary"));

                int activeCount = (int) toLong(summary.get("activeTransactionCount"));
                int slowTx = (int) toLong(summary.get("slowTransactionCount"));
                int slowSql = (int) toLong(summary.get("slowSqlCount"));
                totalActive += activeCount;

                int dbActive = 0;
                int dbMaximum = 0;
                for (Map<String, Object> pool : castList(status.get("dbPools"))) {
                    dbActive += (int) toLong(pool.get("active"));
                    dbMaximum = Math.max(dbMaximum, (int) toLong(pool.get("maximum")));
                }

                Map<String, Object> row = byBusiness.computeIfAbsent(businessCode, code -> {
                    Map<String, Object> created = new LinkedHashMap<>();
                    created.put("businessCode", code);
                    return created;
                });
                row.put("activeTransactions", activeCount);
                row.put("dbActive", dbActive);
                row.put("dbMaximum", dbMaximum);
                row.put("dbDisplay", dbMaximum > 0 ? dbActive + "/" + dbMaximum : String.valueOf(dbActive));
                row.put("slowTransactionCount", slowTx);
                row.put("slowSqlCount", slowSql);
                row.put("slowCount", slowTx + slowSql);
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> row : byBusiness.values()) {
            int activeCount = (int) toLong(row.get("activeTransactions"));
            double ownershipPct = totalActive > 0 ? round1(activeCount * 100.0 / totalActive) : 0;
            row.put("ownershipPct", ownershipPct);
            rows.add(row);
        }
        rows.sort(Comparator.comparingDouble(
                (Map<String, Object> row) -> toDouble(row.get("ownershipPct"))).reversed());

        occupancy.put("section", "15.5");
        occupancy.put("title", "업무 점유 현황");
        occupancy.put("totalActiveTransactions", totalActive);
        occupancy.put("count", rows.size());
        occupancy.put("rows", rows);
        occupancy.put("scopeNote", "실행 거래·점유율은 ActiveTransactionRegistry · Slow는 WAR별 최근 1분 집계");
        return occupancy;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildIncidentFlowAnalysis(
            Map<String, Object> collected,
            Map<String, Object> analysis,
            List<Map<String, Object>> activeTransactions,
            List<Map<String, Object>> slowSql,
            List<Map<String, Object>> serviceCpuUsage) {
        Map<String, Object> flowAnalysis = new LinkedHashMap<>();
        Map<String, Object> cards = castMap(analysis.get("cards"));
        String primaryCause = stringValue(analysis.get("primaryCauseCode"));

        int waitDbCount = 0;
        int executingSqlCount = 0;
        int waitExternalCount = 0;
        long maxExecutingElapsed = 0;
        Map<String, Integer> sqlHits = new HashMap<>();
        Map<String, Integer> externalHits = new HashMap<>();

        for (Map<String, Object> tx : activeTransactions) {
            String step = stringValue(tx.get("currentStep"));
            if ("WAIT_DB_CONNECTION".equals(step)) {
                waitDbCount++;
            } else if ("EXECUTING_SQL".equals(step)) {
                executingSqlCount++;
                maxExecutingElapsed = Math.max(maxExecutingElapsed, toLong(tx.get("elapsedMs")));
                String sqlKey = shortenSqlLabel(stringValue(tx.get("mapperSql")), stringValue(tx.get("sqlId")));
                if (sqlKey != null && !"-".equals(sqlKey)) {
                    sqlHits.merge(sqlKey, 1, Integer::sum);
                }
            } else if ("WAIT_EXTERNAL".equals(step)) {
                waitExternalCount++;
                String external = stringValue(tx.get("externalSystemCode"));
                if (external != null && !external.isBlank()) {
                    externalHits.merge(external, 1, Integer::sum);
                }
            }
        }
        for (Map<String, Object> sql : slowSql) {
            String sqlKey = shortenSqlLabel(stringValue(sql.get("mapperSql")), stringValue(sql.get("sqlId")));
            if (sqlKey != null && !"-".equals(sqlKey)) {
                sqlHits.merge(sqlKey, 1, Integer::sum);
            }
        }

        int topSqlRepeat = sqlHits.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        String topSqlId = sqlHits.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(stringValue(analysis.get("dominantSqlId")));
        int topExternalRepeat = externalHits.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        String topExternalCode = externalHits.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("-");

        int dbPending = (int) toLong(cards.get("dbPending"));
        int dbActive = (int) toLong(cards.get("dbActive"));
        int dbMaximum = (int) toLong(cards.get("dbMaximum"));
        double busyRatio = toDouble(cards.get("threadBusyRatio"));
        double cpuRatio = toDouble(cards.get("jvmCpuRatio"));
        double heapRatio = toDouble(cards.get("heapRatio"));
        int slowSqlCount = (int) toLong(cards.get("slowSqlCount"));

        boolean poolExhausted = dbMaximum > 0 && dbActive >= dbMaximum && dbPending > 0;
        long maxGcTimeMs = 0;
        Object targetRows = collected.get("targets");
        if (targetRows instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> targetRow)) {
                    continue;
                }
                Map<String, Object> status = castMap(((Map<String, Object>) targetRow).get("status"));
                Map<String, Object> jvm = castMap(status.get("jvm"));
                maxGcTimeMs = Math.max(maxGcTimeMs, toLong(jvm.get("gcTimeLastMinuteMs")));
            }
        }
        boolean gcNormal = !(heapRatio >= 80 && maxGcTimeMs >= 3000);
        boolean dbPoolNormal = !poolExhausted && dbPending == 0;
        boolean cpuNormal = cpuRatio < 90;

        String topServiceCpu = "-";
        double topServiceCpuPct = 0;
        for (Map<String, Object> cpu : serviceCpuUsage) {
            double share = toDouble(cpu.get("cpuSharePct"));
            if (share > topServiceCpuPct) {
                topServiceCpuPct = share;
                topServiceCpu = stringValue(cpu.get("serviceId"));
            }
        }

        List<Map<String, Object>> flows = new ArrayList<>();

        List<Map<String, Object>> dbPoolSteps = List.of(
                flowStep(1, "Connection 부족", poolExhausted,
                        poolExhausted ? String.format("DB %d/%d", dbActive, dbMaximum) : "Pool 여유"),
                flowStep(2, "WAIT_DB_CONNECTION 증가", waitDbCount > 0,
                        waitDbCount + "건"),
                flowStep(3, "DB Pending 증가", dbPending > 0,
                        "Pending " + dbPending),
                flowStep(4, "Tomcat Busy Thread 증가", busyRatio >= 70,
                        "Busy " + format1(busyRatio) + "%"),
                flowStep(5, "tcf-om 판정: DB_POOL_EXHAUSTED",
                        "DB_POOL_EXHAUSTED".equals(primaryCause),
                        stringValue(analysis.get("primaryMessage"))));
        flows.add(flowScenario(
                "dbPool", "19.1", "DB Pool 장애", "DB_POOL_EXHAUSTED",
                poolExhausted || waitDbCount > 0 || dbPending > 0 || "DB_POOL_EXHAUSTED".equals(primaryCause),
                "DB_POOL_EXHAUSTED".equals(primaryCause),
                dbPoolSteps,
                List.of(
                        flowLink("DB Pool 분석", "/om/admin/runtime-dbpool-analysis.html"),
                        flowLink("실행 중 거래", "/om/admin/runtime-workspace.html?tab=rtm040"),
                        flowLink("자동 원인판정", "/om/admin/runtime-cause-analysis.html"))));

        boolean connectionNormal = !poolExhausted && dbPending == 0;
        List<Map<String, Object>> slowSqlSteps = List.of(
                flowStep(1, "Connection 정상 획득", connectionNormal,
                        connectionNormal ? "Pending 0" : "Pool 대기 존재"),
                flowStep(2, "EXECUTING_SQL 장시간 지속", executingSqlCount > 0 && maxExecutingElapsed >= 3000,
                        executingSqlCount + "건 · 최대 " + formatElapsedDisplay(maxExecutingElapsed)),
                flowStep(3, "동일 Mapper ID 반복", topSqlRepeat >= 2,
                        topSqlId + " × " + topSqlRepeat),
                flowStep(4, "Pool Active 증가", dbMaximum > 0 && dbActive * 100 / dbMaximum >= 70,
                        dbActive + "/" + dbMaximum),
                flowStep(5, "tcf-om 판정: SLOW_SQL",
                        "SLOW_SQL".equals(primaryCause),
                        stringValue(analysis.get("primaryMessage"))));
        flows.add(flowScenario(
                "slowSql", "19.2", "Slow SQL", "SLOW_SQL",
                slowSqlCount >= 3 || executingSqlCount > 0 || "SLOW_SQL".equals(primaryCause),
                "SLOW_SQL".equals(primaryCause),
                slowSqlSteps,
                List.of(
                        flowLink("SQL 분석", "/om/admin/runtime-sql-analysis.html"),
                        flowLink("DB Pool 분석", "/om/admin/runtime-dbpool-analysis.html"),
                        flowLink("거래·Thread 상세", "/om/admin/runtime-workspace.html?tab=rtm050"),
                        flowLink("자동 원인판정", "/om/admin/runtime-cause-analysis.html"))));

        List<Map<String, Object>> externalSteps = List.of(
                flowStep(1, "DB Pool · CPU · GC 정상", dbPoolNormal && cpuNormal && gcNormal,
                        String.format("Pending %d · CPU %s%% · GC %s", dbPending, format1(cpuRatio), gcNormal ? "정상" : "압박")),
                flowStep(2, "WAIT_EXTERNAL 거래 증가", waitExternalCount >= 1,
                        waitExternalCount + "건"),
                flowStep(3, "특정 외부 시스템 코드 집중", topExternalRepeat >= 2 || waitExternalCount >= 3,
                        topExternalCode + (topExternalRepeat > 0 ? " × " + topExternalRepeat : "")),
                flowStep(4, "tcf-om 판정: EXTERNAL_WAIT",
                        "EXTERNAL_WAIT".equals(primaryCause),
                        stringValue(analysis.get("primaryMessage"))));
        flows.add(flowScenario(
                "externalWait", "19.3", "외부 시스템 지연", "EXTERNAL_WAIT",
                waitExternalCount > 0 || "EXTERNAL_WAIT".equals(primaryCause),
                "EXTERNAL_WAIT".equals(primaryCause),
                externalSteps,
                List.of(
                        flowLink("실행 중 거래", "/om/admin/runtime-workspace.html?tab=rtm040"),
                        flowLink("거래·Thread 상세", "/om/admin/runtime-workspace.html?tab=rtm050"),
                        flowLink("자동 원인판정", "/om/admin/runtime-cause-analysis.html"))));

        List<Map<String, Object>> cpuSteps = List.of(
                flowStep(1, "DB Pending 없음 · 외부 대기 없음", dbPending == 0 && waitExternalCount == 0,
                        "Pending " + dbPending + " · EXTERNAL " + waitExternalCount),
                flowStep(2, "CPU 90% 이상", cpuRatio >= 90,
                        format1(cpuRatio) + "%"),
                flowStep(3, "특정 ServiceId Thread CPU 집중", topServiceCpuPct >= 25,
                        topServiceCpu + " · " + format1(topServiceCpuPct) + "%"),
                flowStep(4, "tcf-om 판정: CPU_OVERLOAD",
                        "CPU_OVERLOAD".equals(primaryCause),
                        stringValue(analysis.get("primaryMessage"))));
        flows.add(flowScenario(
                "cpuOverload", "19.4", "CPU 과부하", "CPU_OVERLOAD",
                cpuRatio >= 90 || "CPU_OVERLOAD".equals(primaryCause),
                "CPU_OVERLOAD".equals(primaryCause),
                cpuSteps,
                List.of(
                        flowLink("JVM 분석", "/om/admin/runtime-jvm-analysis.html"),
                        flowLink("Thread 분석", "/om/admin/runtime-thread-analysis.html"),
                        flowLink("자원 독점 분석", "/om/admin/runtime-dominance-analysis.html"),
                        flowLink("자동 원인판정", "/om/admin/runtime-cause-analysis.html"))));

        flowAnalysis.put("section", "19");
        flowAnalysis.put("title", "장애 흐름");
        flowAnalysis.put("primaryCauseCode", primaryCause);
        flowAnalysis.put("flows", flows);
        flowAnalysis.put("scopeNote", "각 단계는 현재 수집 지표로 활성(징후) 여부를 표시 · PRIMARY는 tcf-om 최종 판정");
        return flowAnalysis;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildIntegratedDashboard(
            Map<String, Object> collected,
            Map<String, Object> analysis,
            List<Map<String, Object>> activeTransactions,
            List<Map<String, Object>> slowTransactions,
            Map<String, Object> threadAnalysis,
            Map<String, Object> jvmAnalysis,
            Map<String, Object> dbPoolAnalysis,
            Map<String, Object> dominanceAnalysis) {
        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("section", "RTM-010");
        dashboard.put("title", "통합 진단 대시보드");

        Map<String, Object> cards = castMap(analysis.get("cards"));
        String overallStatus = stringValue(analysis.get("overallStatus"));
        String primaryCause = displayCauseCode(stringValue(analysis.get("primaryCauseCode")));
        String dominantBusiness = stringValue(analysis.get("dominantBusinessCode"));
        String dominantService = stringValue(analysis.get("dominantServiceId"));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("overallStatus", overallStatus);
        summary.put("overallStatusLabel", statusLabelKo(overallStatus));
        summary.put("primaryCauseCode", primaryCause);
        summary.put("confidence", resolveConfidence(overallStatus, primaryCause));
        summary.put("impactScope", buildImpactScope(dominantBusiness, collected));
        summary.put("dominantServiceId", dominantService != null ? dominantService : "-");
        summary.put("currentStep", resolveDominantCurrentStep(dominantService, activeTransactions));
        summary.put("primaryMessage", analysis.get("primaryMessage"));
        dashboard.put("summary", summary);

        Map<String, Object> sharedPool = castMap(threadAnalysis.get("sharedTomcatPool"));
        Map<String, Object> saturation = castMap(threadAnalysis.get("saturation"));
        Map<String, Object> exhaustion = castMap(dbPoolAnalysis.get("exhaustion"));
        Map<String, Object> cpuOverload = castMap(jvmAnalysis.get("cpuOverload"));
        Map<String, Object> gcPressure = castMap(jvmAnalysis.get("gcPressure"));
        Map<String, Object> businessDominance = castMap(dominanceAnalysis.get("businessDominance"));

        int threadBusy = (int) toLong(sharedPool.get("busy"));
        int threadMax = (int) toLong(sharedPool.get("max"));
        double threadBusyRatio = toDouble(cards.get("threadBusyRatio"));
        int dbActive = (int) toLong(cards.get("dbActive"));
        int dbMaximum = (int) toLong(cards.get("dbMaximum"));
        int dbPending = (int) toLong(cards.get("dbPending"));
        double cpuRatio = toDouble(cards.get("jvmCpuRatio"));
        double heapRatio = toDouble(cards.get("heapRatio"));
        long gcTimeMs = 0;
        Object targetRows = collected.get("targets");
        if (targetRows instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> targetRow)) {
                    continue;
                }
                Map<String, Object> status = castMap(((Map<String, Object>) targetRow).get("status"));
                Map<String, Object> jvm = castMap(status.get("jvm"));
                gcTimeMs = Math.max(gcTimeMs, toLong(jvm.get("gcTimeLastMinuteMs")));
            }
        }
        double gcRatio = gcTimeMs > 0 ? Math.min(100, round1(gcTimeMs / 600.0)) : 0;
        int slowTx = (int) toLong(cards.get("slowTransactionCount"));
        int waitDbCount = countStep(activeTransactions, "WAIT_DB_CONNECTION");
        double warOwnership = toDouble(cards.get("dominantBusinessOwnershipPct"));

        List<Map<String, Object>> questions = new ArrayList<>();
        questions.add(diagnosticQuestion(
                "thread", "Thread 부족?",
                questionLevel(threadBusyRatio >= 85, threadBusyRatio >= 70),
                formatThreadQuestion(threadBusy, threadMax, threadBusyRatio),
                "/om/admin/runtime-thread-analysis.html"));
        questions.add(diagnosticQuestion(
                "dbPool", "DB Pool 대기?",
                questionLevel(Boolean.TRUE.equals(exhaustion.get("detected")), dbPending > 0),
                String.format("Active %d / %d          Pending %d", dbActive, dbMaximum, dbPending),
                "/om/admin/runtime-dbpool-analysis.html"));
        questions.add(diagnosticQuestion(
                "cpu", "CPU 문제?",
                questionLevel(Boolean.TRUE.equals(cpuOverload.get("detected")), cpuRatio >= 70),
                String.format("Process CPU             %s%%", format1(cpuRatio)),
                "/om/admin/runtime-jvm-analysis.html"));
        questions.add(diagnosticQuestion(
                "gc", "GC 문제?",
                questionLevel(Boolean.TRUE.equals(gcPressure.get("detected")), heapRatio >= 70),
                String.format("GC Time Ratio           %s%%", format1(gcRatio)),
                "/om/admin/runtime-jvm-analysis.html"));
        questions.add(diagnosticQuestion(
                "slowTx", "Slow 거래?",
                questionLevel(slowTx >= 5, slowTx > 0),
                String.format("현재 %d건               Timeout %d건", slowTx, countTimeoutLike(slowTransactions)),
                "/om/admin/runtime-workspace.html?tab=rtm040"));
        questions.add(diagnosticQuestion(
                "warDom", "WAR 독점?",
                questionLevel(Boolean.TRUE.equals(businessDominance.get("detected")), warOwnership >= 40),
                String.format("%s Thread 추정 점유     %s%%",
                        dominantBusiness != null ? dominantBusiness : "-",
                        format1(warOwnership)),
                "/om/admin/runtime-dominance-analysis.html"));
        dashboard.put("diagnosticQuestions", questions);

        dashboard.put("evidence", buildEvidenceLines(
                primaryCause,
                dominantBusiness,
                dbPoolAnalysis,
                dbPending,
                waitDbCount,
                slowTx,
                cpuOverload,
                gcPressure));
        dashboard.put("immediateActions", buildImmediateActions(primaryCause, dominantBusiness));
        dashboard.put("quickLinks", List.of(
                quickLink("인스턴스 상세", "/om/admin/runtime-workspace.html?tab=rtm020"),
                quickLink(dominantBusiness != null ? dominantBusiness + " WAR 상세" : "WAR 상세",
                        "/om/admin/runtime-workspace.html?tab=rtm030" + (dominantBusiness != null ? "&war=" + dominantBusiness : "")),
                quickLink("Slow 거래", "/om/admin/runtime-workspace.html?tab=rtm040"),
                quickLink("SQL 상세", "/om/admin/runtime-sql-analysis.html"),
                quickLink("자동 원인 추적", "/om/admin/runtime-workspace.html?tab=rtm100"),
                quickLink("장애보고서 생성", "/om/admin/runtime-cause-analysis.html")));
        dashboard.put("instances", buildInstanceRows(collected));
        dashboard.put("dataFreshness", Map.of(
                "checkedAt", collected.get("checkedAt"),
                "stale", false,
                "staleMessage", "데이터 정상"));
        return dashboard;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildInstanceDetailAnalysis(
            Map<String, Object> collected,
            Map<String, Object> analysis,
            List<Map<String, Object>> threads,
            List<Map<String, Object>> serviceCpuUsage,
            Map<String, Object> dominanceAnalysis) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("section", "RTM-020");
        detail.put("title", "Tomcat 인스턴스 상세");
        detail.put("scopeNote", "JVM·Thread·GC는 Tomcat 공유 자원입니다. WAR별 Pool·SQL은 이 화면에서 합산하지 않습니다.");

        Map<String, Object> shared = resolveSharedTomcatSnapshot(collected);
        Map<String, Object> thread = castMap(shared.get("thread"));
        Map<String, Object> jvm = castMap(shared.get("jvm"));
        Map<String, Object> instance = castMap(shared.get("instance"));

        int deadlockCount = (int) toLong(thread.get("deadlockCount"));
        boolean deadlock = Boolean.TRUE.equals(thread.get("deadlock")) || deadlockCount > 0;

        Map<String, Object> header = new LinkedHashMap<>();
        String host = stringValue(instance.get("hostName"));
        int port = (int) toLong(instance.get("port"));
        long pid = toLong(instance.get("pid"));
        long uptimeMs = toLong(jvm.get("uptimeMs"));
        header.put("label", formatInstanceHeader(host, port, pid, uptimeMs));
        header.put("hostName", host);
        header.put("port", port);
        header.put("pid", pid > 0 ? pid : "-");
        header.put("uptimeMs", uptimeMs);
        header.put("uptimeDisplay", formatUptime(uptimeMs));
        header.put("instanceKey", formatInstanceLabel(shared));
        detail.put("header", header);
        detail.put("deadlockAlert", deadlock ? Map.of(
                "detected", true,
                "count", Math.max(deadlockCount, 1),
                "message", "● DEADLOCK 탐지: " + Math.max(deadlockCount, 1) + "개 Thread",
                "linkLabel", "Thread Dump 상세",
                "linkHref", "/om/admin/runtime-workspace.html?tab=rtm050") : Map.of("detected", false));

        double cpuRatio = toDouble(jvm.get("processCpuRatio"));
        double heapRatio = toDouble(jvm.get("heapRatio"));
        double heapUsed = toDouble(jvm.get("heapUsedMb"));
        double heapMax = toDouble(jvm.get("heapMaxMb"));
        double metaUsed = toDouble(jvm.get("metaspaceMb"));
        double metaMax = toDouble(jvm.get("metaspaceMaxMb"));
        int threadBusy = (int) toLong(thread.get("busy"));
        int threadMax = (int) toLong(thread.get("max"));
        double threadBusyRatio = toDouble(thread.get("busyRatio"));
        int acceptQ = (int) toLong(thread.get("acceptQueue"));
        int acceptQMax = (int) toLong(thread.get("acceptQueueMax"));
        if (acceptQMax <= 0) {
            acceptQMax = 500;
        }
        long gcTimeMs = toLong(jvm.get("gcTimeLastMinuteMs"));
        double gcRatio = gcTimeMs > 0 ? Math.min(100, round1(gcTimeMs / 600.0)) : 0;

        List<Map<String, Object>> summaryMetrics = List.of(
                summaryMetric("processCpu", "Process CPU", format1(cpuRatio) + "%",
                        metricLevel(cpuRatio >= 85, cpuRatio >= 70)),
                summaryMetric("heap", "Heap Used",
                        formatMemoryGb(heapUsed) + " / " + formatMemoryGb(heapMax) + "      " + format1(heapRatio) + "%",
                        metricLevel(heapRatio >= 85, heapRatio >= 70)),
                summaryMetric("metaspace", "Metaspace",
                        formatMemoryGb(metaUsed) + " / " + (metaMax > 0 ? formatMemoryGb(metaMax) : "-") + "        "
                                + (metaMax > 0 ? format1(metaUsed * 100 / metaMax) : "-") + "%",
                        metricLevel(metaMax > 0 && metaUsed * 100 / metaMax >= 85, metaMax > 0 && metaUsed * 100 / metaMax >= 70)),
                summaryMetric("busyThread", "Busy Thread",
                        threadMax > 0
                                ? threadBusy + " / " + threadMax + "          " + format1(threadBusyRatio) + "%"
                                : threadBusy + " / -          " + format1(threadBusyRatio) + "%",
                        metricLevel(threadBusyRatio >= 85, threadBusyRatio >= 70)),
                summaryMetric("acceptQueue", "Accept Queue", acceptQ + " / " + acceptQMax, "normal"),
                summaryMetric("gcRatio", "GC Time Ratio", format1(gcRatio) + "%",
                        metricLevel(gcRatio >= 10, gcRatio >= 5)),
                summaryMetric("deadlock", "Deadlock", String.valueOf(deadlockCount), deadlock ? "critical" : "normal"));
        detail.put("summaryMetrics", summaryMetrics);

        detail.put("trendMetricKeys", List.of("cpu", "busy", "heap", "gcPause"));
        detail.put("deployedWars", buildDeployedWarRows(collected, analysis, dominanceAnalysis));
        detail.put("instanceOptions", buildInstanceOptions(collected));

        Map<String, Double> cpuByService = new HashMap<>();
        for (Map<String, Object> cpu : serviceCpuUsage) {
            String sid = stringValue(cpu.get("serviceId"));
            if (sid != null) {
                cpuByService.put(sid, toDouble(cpu.get("cpuSharePct")));
            }
        }
        List<Map<String, Object>> threadRows = new ArrayList<>();
        for (Map<String, Object> t : threads) {
            Map<String, Object> row = new LinkedHashMap<>(t);
            String sid = stringValue(t.get("serviceId"));
            row.put("threadCpuPct", sid != null ? cpuByService.getOrDefault(sid, 0.0) : 0);
            row.put("sqlOrExternal", formatSqlOrExternal(t));
            row.put("elapsedDisplay", formatElapsedDisplay(toLong(t.get("elapsedMs"))));
            threadRows.add(row);
        }
        detail.put("threadRows", threadRows);

        Map<String, Object> jvmTab = new LinkedHashMap<>();
        jvmTab.put("processCpuRatio", cpuRatio);
        jvmTab.put("heapUsedMb", heapUsed);
        jvmTab.put("heapMaxMb", heapMax);
        jvmTab.put("heapRatio", heapRatio);
        jvmTab.put("metaspaceMb", metaUsed);
        jvmTab.put("metaspaceMaxMb", metaMax);
        jvmTab.put("oldGenRatio", jvm.get("oldGenRatio"));
        jvmTab.put("liveThreadCount", jvm.get("liveThreadCount"));
        detail.put("jvmTab", jvmTab);

        Map<String, Object> gcTab = new LinkedHashMap<>();
        gcTab.put("gcTimeLastMinuteMs", gcTimeMs);
        gcTab.put("gcCountLastMinute", jvm.get("gcCountLastMinute"));
        gcTab.put("gcTimeRatio", gcRatio);
        gcTab.put("heapRatio", heapRatio);
        detail.put("gcTab", gcTab);

        detail.put("deadlockTab", Map.of(
                "detected", deadlock,
                "count", deadlockCount,
                "message", deadlock
                        ? "Thread Deadlock이 발견되었습니다. 최우선 위험으로 처리합니다."
                        : "Deadlock 징후 없음"));
        return detail;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildWarResourceDetailAnalysis(
            Map<String, Object> collected,
            Map<String, Object> analysis,
            List<Map<String, Object>> activeTransactions,
            List<Map<String, Object>> slowTransactions,
            List<Map<String, Object>> slowSql,
            Map<String, Object> dbPoolAnalysis,
            Map<String, Object> dominanceAnalysis) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("section", "RTM-030");
        detail.put("title", "WAR 자원 상세");
        detail.put("scopeNote",
                "CPU·Heap·GC는 Tomcat JVM 공유자원입니다.\n"
                        + "WAR별 CPU와 Thread 점유는 실행 거래를 기반으로 한 추정값입니다.");

        Map<String, Object> shared = resolveSharedTomcatSnapshot(collected);
        Map<String, Object> instance = castMap(shared.get("instance"));
        int port = (int) toLong(instance.get("port"));
        String instanceLabel = port > 0 ? "CENTER1-AP01:" + port : resolvePrimaryInstanceLabel(collected);

        Map<String, Object> cards = castMap(analysis.get("cards"));
        int totalActive = (int) toLong(dominanceAnalysis.get("totalActiveTransactions"));
        String dominantWar = stringValue(analysis.get("dominantBusinessCode"));
        double dominantOwnership = toDouble(cards.get("dominantBusinessOwnershipPct"));

        List<Map<String, Object>> warRows = new ArrayList<>();
        Object targetRows = collected.get("targets");
        if (targetRows instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> targetRow)) {
                    continue;
                }
                Map<String, Object> target = (Map<String, Object>) targetRow;
                String war = stringValue(target.get("businessCode"));
                if (war == null) {
                    continue;
                }
                Map<String, Object> status = castMap(target.get("status"));
                Map<String, Object> summary = castMap(status.get("summary"));
                Map<String, Object> thread = castMap(status.get("thread"));

                int active = (int) toLong(summary.get("activeTransactionCount"));
                int slow = (int) toLong(summary.get("slowTransactionCount"));
                double threadOccupancy = totalActive > 0
                        ? round1(active * 100.0 / totalActive)
                        : toDouble(thread.get("busyRatio"));
                int timeoutCount = countWarTimeoutLike(war, activeTransactions, slowTransactions);

                List<Map<String, Object>> pools = buildWarPoolRows(
                        war, castList(status.get("dbPools")), activeTransactions);
                List<Map<String, Object>> serviceRows = buildWarServiceIdRows(
                        war, activeTransactions, slowTransactions, totalActive);
                List<Map<String, Object>> txRows = filterByWar(activeTransactions, war);
                List<Map<String, Object>> sqlRows = filterWarSlowSql(war, slowSql, activeTransactions);
                List<Map<String, Object>> externalRows = filterWarExternalRows(activeTransactions, war);

                boolean dominanceCandidate = (dominantWar != null && dominantWar.equals(war)
                        && (dominantOwnership >= 40 || threadOccupancy >= 40))
                        || ("BUSINESS_RESOURCE_DOMINANCE".equals(stringValue(analysis.get("primaryCauseCode")))
                        && dominantWar != null && dominantWar.equals(war));
                String warStatus = stringValue(summary.get("status"));
                if (warStatus == null) {
                    warStatus = "NORMAL";
                }
                if (dominanceCandidate && !"CRITICAL".equals(warStatus)) {
                    warStatus = dominantOwnership >= 60 ? "CRITICAL" : "WARN";
                }

                Map<String, Object> verdict = new LinkedHashMap<>();
                verdict.put("detected", dominanceCandidate);
                verdict.put("causeCode", dominanceCandidate ? "WAR_RESOURCE_DOMINANCE" : "NORMAL");
                verdict.put("causeCodeDisplay", dominanceCandidate ? "WAR_RESOURCE_DOMINANCE 후보" : "정상 범위");
                verdict.put("evidence", dominanceCandidate
                        ? String.format(
                                "전체 Active 거래의 %s%%, Thread 추정 점유 %s%%",
                                format1(dominantWar != null && dominantWar.equals(war) ? dominantOwnership : threadOccupancy),
                                format1(threadOccupancy))
                        : "WAR 자원 독점 기준 미충족");
                verdict.put("actions", List.of(
                        actionLink("거래 제한 검토", "/om/admin/transaction-control.html"),
                        actionLink("ServiceId 상세", "/om/admin/service-catalog.html"),
                        actionLink("장애보고서 연결", "/om/admin/runtime-cause-analysis.html")));

                Map<String, Object> warRow = new LinkedHashMap<>();
                warRow.put("war", war);
                warRow.put("instanceLabel", instanceLabel);
                warRow.put("status", warStatus);
                warRow.put("statusLabel", statusLabelKo(warStatus));
                warRow.put("activeTransactions", active);
                warRow.put("slowTransactions", slow);
                warRow.put("timeoutCount", timeoutCount);
                warRow.put("threadOccupancyPct", threadOccupancy);
                warRow.put("dbPools", pools);
                warRow.put("serviceIds", serviceRows);
                warRow.put("transactions", txRows);
                warRow.put("sqlRows", sqlRows);
                warRow.put("externalRows", externalRows);
                warRow.put("verdict", verdict);
                warRows.add(warRow);
            }
        }
        warRows.sort(Comparator.comparingInt((Map<String, Object> r) -> (int) toLong(r.get("activeTransactions"))).reversed());

        List<Map<String, Object>> warOptions = warRows.stream()
                .map(r -> {
                    Map<String, Object> option = new LinkedHashMap<>();
                    option.put("war", r.get("war"));
                    option.put("label", r.get("war"));
                    option.put("status", r.get("status"));
                    option.put("statusLabel", r.get("statusLabel"));
                    return option;
                })
                .toList();

        String selectedWar = dominantWar;
        if (selectedWar == null && !warRows.isEmpty()) {
            selectedWar = stringValue(warRows.get(0).get("war"));
        }

        detail.put("instanceLabel", instanceLabel);
        detail.put("selectedWar", selectedWar);
        detail.put("warOptions", warOptions);
        detail.put("wars", warRows);
        return detail;
    }

    private List<Map<String, Object>> buildWarPoolRows(
            String war,
            List<Map<String, Object>> pools,
            List<Map<String, Object>> activeTransactions) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> pool : pools) {
            int maximum = (int) toLong(pool.get("maximum"));
            int active = (int) toLong(pool.get("active"));
            int idle = (int) toLong(pool.get("idle"));
            int pending = (int) toLong(pool.get("pending"));
            double usageRatio = maximum > 0 ? round1(active * 100.0 / maximum) : 0;
            boolean exhausted = maximum > 0 && active >= maximum && idle == 0 && pending > 0;
            long leakSuspected = toLong(pool.get("longUsageConnections"));
            long timeoutCount = toLong(pool.get("connectionTimeoutCount"));

            List<Long> acquireSamples = new ArrayList<>();
            for (Map<String, Object> tx : activeTransactions) {
                if (!war.equals(stringValue(tx.get("businessCode")))) {
                    continue;
                }
                if ("WAIT_DB_CONNECTION".equals(stringValue(tx.get("currentStep")))) {
                    long wait = toLong(tx.get("dbWaitMs"));
                    if (wait > 0) {
                        acquireSamples.add(wait);
                    } else {
                        acquireSamples.add(toLong(tx.get("elapsedMs")));
                    }
                }
            }
            long acquireAvg = acquireSamples.isEmpty() ? 0 : Math.round(averageLong(acquireSamples));
            long acquireP95 = acquireSamples.isEmpty() ? 0 : percentileLong(acquireSamples, 95);

            String poolLevel = exhausted ? "critical" : (usageRatio >= 85 || pending > 0 ? "warn" : "normal");

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("businessCode", war);
            row.put("poolName", pool.get("poolName"));
            row.put("maximum", maximum);
            row.put("active", active);
            row.put("idle", idle);
            row.put("pending", pending);
            row.put("usageRatio", usageRatio);
            row.put("acquireAvgMs", acquireAvg);
            row.put("acquireP95Ms", acquireP95);
            row.put("acquireAvgDisplay", formatElapsedDisplay(acquireAvg));
            row.put("acquireP95Display", formatElapsedDisplay(acquireP95));
            row.put("connectionTimeoutCount", timeoutCount);
            row.put("leakSuspected", leakSuspected > 0);
            row.put("leakSuspectedCount", leakSuspected);
            row.put("exhausted", exhausted);
            row.put("status", poolLevel);
            row.put("statusLabel", statusLabelKo(
                    "critical".equals(poolLevel) ? "CRITICAL" : "warn".equals(poolLevel) ? "WARN" : "NORMAL"));
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> buildWarServiceIdRows(
            String war,
            List<Map<String, Object>> activeTransactions,
            List<Map<String, Object>> slowTransactions,
            int totalActive) {
        Map<String, WarServiceStats> statsByService = new LinkedHashMap<>();
        for (Map<String, Object> tx : activeTransactions) {
            if (!war.equals(stringValue(tx.get("businessCode")))) {
                continue;
            }
            String serviceId = stringValue(tx.get("serviceId"));
            if (serviceId == null) {
                continue;
            }
            WarServiceStats stats = statsByService.computeIfAbsent(serviceId, key -> new WarServiceStats());
            stats.active++;
            long elapsed = toLong(tx.get("elapsedMs"));
            stats.elapsedSamples.add(elapsed);
            stats.stepCounts.merge(stringValue(tx.get("currentStep")), 1, Integer::sum);
        }
        for (Map<String, Object> tx : slowTransactions) {
            if (!war.equals(stringValue(tx.get("businessCode")))) {
                continue;
            }
            String serviceId = stringValue(tx.get("serviceId"));
            if (serviceId == null) {
                continue;
            }
            WarServiceStats stats = statsByService.computeIfAbsent(serviceId, key -> new WarServiceStats());
            stats.slow++;
            long elapsed = toLong(tx.get("elapsedMs"));
            stats.elapsedSamples.add(elapsed);
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<String, WarServiceStats> entry : statsByService.entrySet()) {
            WarServiceStats stats = entry.getValue();
            long avg = stats.elapsedSamples.isEmpty()
                    ? 0
                    : Math.round(averageLong(stats.elapsedSamples));
            long p95 = stats.elapsedSamples.isEmpty()
                    ? 0
                    : percentileLong(stats.elapsedSamples, 95);
            String dominantStep = stats.stepCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("-");

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("serviceId", entry.getKey());
            row.put("active", stats.active);
            row.put("slow", stats.slow);
            row.put("avgElapsedMs", avg);
            row.put("p95ElapsedMs", p95);
            row.put("avgElapsedDisplay", formatElapsedDisplay(avg));
            row.put("p95ElapsedDisplay", formatElapsedDisplay(p95));
            row.put("dominantStep", dominantStep);
            row.put("dominantStepLabel", stepLabelKo(dominantStep));
            row.put("ownershipPct", totalActive > 0 ? round1(stats.active * 100.0 / totalActive) : 0);
            rows.add(row);
        }
        rows.sort(Comparator.comparingInt((Map<String, Object> r) -> (int) toLong(r.get("active"))).reversed());
        return rows;
    }

    private static List<Map<String, Object>> filterByWar(List<Map<String, Object>> rows, String war) {
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            if (war.equals(stringValue(row.get("businessCode")))) {
                filtered.add(row);
            }
        }
        return filtered;
    }

    private static List<Map<String, Object>> filterWarExternalRows(
            List<Map<String, Object>> activeTransactions, String war) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> tx : activeTransactions) {
            if (!war.equals(stringValue(tx.get("businessCode")))) {
                continue;
            }
            if (!"WAIT_EXTERNAL".equals(stringValue(tx.get("currentStep")))) {
                continue;
            }
            rows.add(tx);
        }
        return rows;
    }

    private List<Map<String, Object>> filterWarSlowSql(
            String war,
            List<Map<String, Object>> slowSql,
            List<Map<String, Object>> activeTransactions) {
        java.util.Set<String> serviceIds = new java.util.LinkedHashSet<>();
        for (Map<String, Object> tx : activeTransactions) {
            if (war.equals(stringValue(tx.get("businessCode")))) {
                String serviceId = stringValue(tx.get("serviceId"));
                if (serviceId != null) {
                    serviceIds.add(serviceId);
                }
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> sql : slowSql) {
            String serviceId = stringValue(sql.get("serviceId"));
            if (serviceId == null) {
                continue;
            }
            if (serviceIds.contains(serviceId) || serviceId.startsWith(war + ".")) {
                rows.add(sql);
            }
        }
        return rows;
    }

    private static int countWarTimeoutLike(
            String war,
            List<Map<String, Object>> activeTransactions,
            List<Map<String, Object>> slowTransactions) {
        int count = 0;
        for (Map<String, Object> tx : slowTransactions) {
            if (war.equals(stringValue(tx.get("businessCode")))) {
                count++;
            }
        }
        for (Map<String, Object> tx : activeTransactions) {
            if (!war.equals(stringValue(tx.get("businessCode")))) {
                continue;
            }
            if (toLong(tx.get("elapsedMs")) >= 30_000L) {
                count++;
            }
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private static String resolvePrimaryInstanceLabel(Map<String, Object> collected) {
        Object targetRows = collected.get("targets");
        if (targetRows instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            return formatInstanceLabel((Map<String, Object>) first);
        }
        return "CENTER1-AP01:8080";
    }

    private static long averageLong(List<Long> values) {
        if (values.isEmpty()) {
            return 0;
        }
        long sum = 0;
        for (Long value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    private static long percentileLong(List<Long> values, int percentile) {
        if (values.isEmpty()) {
            return 0;
        }
        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(Long::compareTo);
        int index = Math.min(sorted.size() - 1, (int) Math.ceil(sorted.size() * percentile / 100.0) - 1);
        return sorted.get(Math.max(0, index));
    }

    private static Map<String, Object> actionLink(String label, String href) {
        Map<String, Object> link = new LinkedHashMap<>();
        link.put("label", label);
        link.put("href", href);
        return link;
    }

    private static String stepLabelKo(String step) {
        if (step == null || step.isBlank() || "-".equals(step)) {
            return "정상";
        }
        return switch (step) {
            case "WAIT_DB_CONNECTION" -> "DB 대기";
            case "EXECUTING_SQL" -> "SQL 실행";
            case "WAIT_EXTERNAL" -> "외부 대기";
            case "WAIT_HANDLER" -> "Handler 대기";
            default -> step;
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveSharedTomcatSnapshot(Map<String, Object> collected) {
        Object targetRows = collected.get("targets");
        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("instance", Map.of());
        fallback.put("thread", Map.of());
        fallback.put("jvm", Map.of());
        fallback.put("baseUrl", "");
        if (!(targetRows instanceof List<?> list)) {
            return fallback;
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> targetRow)) {
                continue;
            }
            Map<String, Object> target = (Map<String, Object>) targetRow;
            if (Boolean.FALSE.equals(target.get("reachable"))) {
                continue;
            }
            Map<String, Object> status = castMap(target.get("status"));
            Map<String, Object> thread = castMap(status.get("thread"));
            Map<String, Object> jvm = castMap(status.get("jvm"));
            if (!thread.isEmpty() || !jvm.isEmpty()) {
                Map<String, Object> shared = new LinkedHashMap<>();
                shared.put("instance", castMap(status.get("instance")));
                shared.put("thread", thread);
                shared.put("jvm", jvm);
                shared.put("baseUrl", target.get("baseUrl"));
                shared.put("businessCode", target.get("businessCode"));
                return shared;
            }
        }
        if (!list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            Map<String, Object> target = (Map<String, Object>) first;
            Map<String, Object> status = castMap(target.get("status"));
            fallback.put("instance", castMap(status.get("instance")));
            fallback.put("thread", castMap(status.get("thread")));
            fallback.put("jvm", castMap(status.get("jvm")));
            fallback.put("baseUrl", target.get("baseUrl"));
            fallback.put("businessCode", target.get("businessCode"));
        }
        return fallback;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildDeployedWarRows(
            Map<String, Object> collected,
            Map<String, Object> analysis,
            Map<String, Object> dominanceAnalysis) {
        List<Map<String, Object>> rows = new ArrayList<>();
        int totalActive = 0;
        Object targetRows = collected.get("targets");
        if (targetRows instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> targetRow)) {
                    continue;
                }
                Map<String, Object> target = (Map<String, Object>) targetRow;
                Map<String, Object> status = castMap(target.get("status"));
                Map<String, Object> summary = castMap(status.get("summary"));
                totalActive += (int) toLong(summary.get("activeTransactionCount"));
            }
        }
        if (!(targetRows instanceof List<?> list)) {
            return rows;
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> targetRow)) {
                continue;
            }
            Map<String, Object> target = (Map<String, Object>) targetRow;
            String businessCode = stringValue(target.get("businessCode"));
            boolean reachable = !Boolean.FALSE.equals(target.get("reachable"));
            Map<String, Object> status = castMap(target.get("status"));
            Map<String, Object> summary = castMap(status.get("summary"));
            Map<String, Object> thread = castMap(status.get("thread"));

            int active = (int) toLong(summary.get("activeTransactionCount"));
            int slow = (int) toLong(summary.get("slowTransactionCount"));
            double occupancy = totalActive > 0 ? round1(active * 100.0 / totalActive) : 0;
            if (occupancy == 0 && thread.containsKey("busyRatio")) {
                occupancy = toDouble(thread.get("busyRatio"));
            }

            int poolActive = 0;
            int poolMax = 0;
            int poolPending = 0;
            for (Map<String, Object> pool : castList(status.get("dbPools"))) {
                poolActive += (int) toLong(pool.get("active"));
                poolMax = Math.max(poolMax, (int) toLong(pool.get("maximum")));
                poolPending += (int) toLong(pool.get("pending"));
            }
            String poolDisplay = poolMax > 0
                    ? poolActive + "/" + poolMax + (poolPending > 0 ? " P" + poolPending : " P0")
                    : "-";

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("war", businessCode);
            row.put("reachable", reachable);
            row.put("status", stringValue(summary.get("status")));
            row.put("statusLabel", statusLabelKo(stringValue(summary.get("status"))));
            row.put("activeTransactions", active);
            row.put("slowTransactions", slow);
            row.put("threadOccupancyPct", occupancy);
            row.put("poolDisplay", poolDisplay);
            rows.add(row);
        }
        rows.sort(Comparator.comparingInt((Map<String, Object> r) -> (int) toLong(r.get("activeTransactions"))).reversed());
        return rows;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildInstanceOptions(Map<String, Object> collected) {
        List<Map<String, Object>> options = new ArrayList<>();
        Object targetRows = collected.get("targets");
        if (!(targetRows instanceof List<?> list)) {
            return options;
        }
        Map<String, Map<String, Object>> byKey = new LinkedHashMap<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> targetRow)) {
                continue;
            }
            Map<String, Object> target = (Map<String, Object>) targetRow;
            String key = formatInstanceLabel(target);
            byKey.putIfAbsent(key, Map.of(
                    "instanceKey", key,
                    "label", key,
                    "baseUrl", stringValue(target.get("baseUrl"))));
        }
        options.addAll(byKey.values());
        return options;
    }

    private static Map<String, Object> summaryMetric(
            String id, String label, String display, String level) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("label", label);
        row.put("display", display);
        row.put("level", level);
        row.put("levelSymbol", levelSymbol(level));
        row.put("levelLabel", level.equals("critical") ? "위험" : level.equals("warn") ? "주의" : "정상");
        return row;
    }

    private static String metricLevel(boolean critical, boolean warn) {
        if (critical) {
            return "critical";
        }
        if (warn) {
            return "warn";
        }
        return "normal";
    }

    private static String formatInstanceHeader(String host, int port, long pid, long uptimeMs) {
        String hostPart = host != null ? host : "localhost";
        String portPart = port > 0 ? String.valueOf(port) : "-";
        String pidPart = pid > 0 ? String.valueOf(pid) : "-";
        return hostPart + " / AP01 / " + portPart + " / PID " + pidPart + " / Uptime " + formatUptime(uptimeMs);
    }

    private static String formatUptime(long uptimeMs) {
        if (uptimeMs <= 0) {
            return "-";
        }
        long minutes = uptimeMs / 60_000L;
        long days = minutes / (60 * 24);
        long hours = (minutes / 60) % 24;
        long mins = minutes % 60;
        if (days > 0) {
            return days + "일 " + String.format("%02d:%02d", hours, mins);
        }
        return hours + "시간 " + mins + "분";
    }

    private static String formatMemoryGb(double mb) {
        if (mb <= 0) {
            return "-";
        }
        if (mb >= 1024) {
            return format1(mb / 1024.0) + "GB";
        }
        return format1(mb) + "MB";
    }

    private static String displayCauseCode(String causeCode) {
        if ("DB_POOL_EXHAUSTED".equals(causeCode)) {
            return "DB_POOL_WAIT";
        }
        if ("BUSINESS_RESOURCE_DOMINANCE".equals(causeCode)) {
            return "WAR_RESOURCE_DOMINANCE";
        }
        return causeCode != null ? causeCode : "NORMAL";
    }

    private static String statusLabelKo(String status) {
        if ("CRITICAL".equals(status)) {
            return "위험";
        }
        if ("WARN".equals(status)) {
            return "주의";
        }
        if ("UNKNOWN".equals(status)) {
            return "미확인";
        }
        return "정상";
    }

    private static String resolveConfidence(String overallStatus, String primaryCause) {
        if ("NORMAL".equals(primaryCause) || "NORMAL".equals(overallStatus)) {
            return "-";
        }
        if ("CRITICAL".equals(overallStatus) || "THREAD_DEADLOCK".equals(primaryCause)) {
            return "HIGH";
        }
        if ("WARN".equals(overallStatus)) {
            return "MEDIUM";
        }
        return "LOW";
    }

    @SuppressWarnings("unchecked")
    private static String buildImpactScope(String dominantBusiness, Map<String, Object> collected) {
        Object targetRows = collected.get("targets");
        if (!(targetRows instanceof List<?> list) || list.isEmpty()) {
            return dominantBusiness != null ? dominantBusiness : "-";
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> row)) {
                continue;
            }
            Map<String, Object> target = (Map<String, Object>) row;
            String businessCode = stringValue(target.get("businessCode"));
            if (dominantBusiness != null && !dominantBusiness.equals(businessCode)) {
                continue;
            }
            String instance = formatInstanceLabel(target);
            return instance + " / " + (businessCode != null ? businessCode : "-");
        }
        Map<String, Object> first = (Map<String, Object>) list.get(0);
        return formatInstanceLabel(first) + " / " + stringValue(first.get("businessCode"));
    }

    @SuppressWarnings("unchecked")
    private static String formatInstanceLabel(Map<String, Object> target) {
        String baseUrl = stringValue(target.get("baseUrl"));
        if (baseUrl != null && !baseUrl.isBlank()) {
            try {
                java.net.URI uri = java.net.URI.create(baseUrl);
                String host = uri.getHost() != null ? uri.getHost() : uri.getAuthority();
                int port = uri.getPort() > 0 ? uri.getPort() : ("https".equals(uri.getScheme()) ? 443 : 80);
                if (host != null) {
                    return host + ":" + port;
                }
            } catch (IllegalArgumentException ignored) {
                return baseUrl;
            }
        }
        return stringValue(target.get("businessCode"));
    }

    private static String resolveDominantCurrentStep(
            String dominantService, List<Map<String, Object>> activeTransactions) {
        if (dominantService == null) {
            return "-";
        }
        for (Map<String, Object> tx : activeTransactions) {
            if (dominantService.equals(stringValue(tx.get("serviceId")))) {
                return stringValue(tx.get("currentStep"));
            }
        }
        return "-";
    }

    private static int countStep(List<Map<String, Object>> transactions, String step) {
        int count = 0;
        for (Map<String, Object> tx : transactions) {
            if (step.equals(stringValue(tx.get("currentStep")))) {
                count++;
            }
        }
        return count;
    }

    private static int countTimeoutLike(List<Map<String, Object>> slowTransactions) {
        int count = 0;
        for (Map<String, Object> tx : slowTransactions) {
            if (Boolean.FALSE.equals(tx.get("success"))
                    || toLong(tx.get("elapsedMs")) >= 8000) {
                count++;
            }
        }
        return count;
    }

    private static String formatThreadQuestion(int busy, int max, double ratio) {
        if (max > 0) {
            return String.format("Busy %d / %d          %s%%", busy, max, format1(ratio));
        }
        return String.format("Busy - / -          %s%%", format1(ratio));
    }

    private static String questionLevel(boolean critical, boolean warn) {
        if (critical) {
            return "critical";
        }
        if (warn) {
            return "warn";
        }
        return "normal";
    }

    private static Map<String, Object> diagnosticQuestion(
            String id, String label, String level, String detail, String href) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("label", label);
        row.put("level", level);
        row.put("levelSymbol", levelSymbol(level));
        row.put("detail", detail);
        row.put("href", href);
        return row;
    }

    private static String levelSymbol(String level) {
        return switch (level) {
            case "critical" -> "●";
            case "warn" -> "△";
            default -> "○";
        };
    }

    @SuppressWarnings("unchecked")
    private List<String> buildEvidenceLines(
            String primaryCause,
            String dominantBusiness,
            Map<String, Object> dbPoolAnalysis,
            int dbPending,
            int waitDbCount,
            int slowTx,
            Map<String, Object> cpuOverload,
            Map<String, Object> gcPressure) {
        List<String> lines = new ArrayList<>();
        for (Map<String, Object> pool : castList(dbPoolAnalysis.get("pools"))) {
            int maximum = (int) toLong(pool.get("maximum"));
            int active = (int) toLong(pool.get("active"));
            if (maximum > 0 && active >= maximum) {
                String business = stringValue(pool.get("businessCode"));
                lines.add(String.format("%s Hikari Pool 사용률 100%%", business != null ? business : "WAR"));
            }
        }
        if (dbPending > 0) {
            lines.add(String.format("Connection 대기 Thread %d개", dbPending));
        }
        if (waitDbCount > 0) {
            lines.add(String.format("Slow 거래 %d건 중 %d건이 WAIT_DB_CONNECTION", slowTx, waitDbCount));
        }
        if (Boolean.TRUE.equals(cpuOverload.get("detected"))) {
            lines.add("JVM CPU가 지속적으로 높음");
        } else if (!Boolean.TRUE.equals(gcPressure.get("detected"))) {
            lines.add("CPU와 GC는 정상범위");
        }
        if (lines.isEmpty()) {
            lines.add(primaryCause != null && !"NORMAL".equals(primaryCause)
                    ? "1순위 원인: " + primaryCause
                    : "현재 주요 병목 징후 없음");
        }
        List<String> numbered = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            numbered.add((i + 1) + ". " + lines.get(i));
        }
        return numbered;
    }

    private static List<String> buildImmediateActions(String primaryCause, String dominantBusiness) {
        String war = dominantBusiness != null ? dominantBusiness : "해당 WAR";
        return switch (primaryCause != null ? primaryCause : "NORMAL") {
            case "DB_POOL_WAIT", "DB_POOL_EXHAUSTED" -> List.of(
                    war + " 장기 Transaction과 Slow SQL 확인",
                    "DB Lock과 DB Session 상태 확인",
                    "Pool 확대 전 DB 최대 Session 확인",
                    "필요 시 " + war + " 거래 유입량 임시 제한");
            case "SLOW_SQL" -> List.of(
                    "Slow SQL Mapper 실행계획·Lock 확인",
                    "동일 SQL 동시 실행 건수 확인",
                    "DB Session·Pool 상태 확인");
            case "THREAD_SATURATION", "THREAD_DEADLOCK" -> List.of(
                    "장기 거래·Pool·SQL·외부연계 확인",
                    "Thread Dump 상세 확인",
                    "거래 유입량 검토");
            case "CPU_OVERLOAD" -> List.of(
                    "ServiceId Thread CPU 집중 확인",
                    "반복 연산·배치 영향 확인",
                    "JVM Heap·GC 동시 확인");
            case "GC_PRESSURE" -> List.of(
                    "Heap 사용률·Old Gen 확인",
                    "대량 객체·캐시 증가 확인",
                    "GC 로그 확인");
            case "EXTERNAL_WAIT" -> List.of(
                    "WAIT_EXTERNAL 거래·외부 시스템 코드 확인",
                    "Read Timeout·연계 상태 확인");
            default -> List.of(
                    "종합 상태 유지 모니터링",
                    "이상 징후 시 진단 가이드 Wizard 진행",
                    "필요 시 거래로그·감사로그 추가 확인");
        };
    }

    private static Map<String, Object> quickLink(String label, String href) {
        Map<String, Object> link = new LinkedHashMap<>();
        link.put("label", label);
        link.put("href", href);
        return link;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildInstanceRows(Map<String, Object> collected) {
        List<Map<String, Object>> rows = new ArrayList<>();
        Object targetRows = collected.get("targets");
        if (!(targetRows instanceof List<?> list)) {
            return rows;
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> targetRow)) {
                continue;
            }
            Map<String, Object> target = (Map<String, Object>) targetRow;
            String businessCode = stringValue(target.get("businessCode"));
            boolean reachable = !Boolean.FALSE.equals(target.get("reachable"));
            Map<String, Object> status = castMap(target.get("status"));
            Map<String, Object> summary = castMap(status.get("summary"));
            Map<String, Object> thread = castMap(status.get("thread"));
            Map<String, Object> jvm = castMap(status.get("jvm"));

            String warStatus = stringValue(summary.get("status"));
            String causeCode = displayCauseCode(stringValue(summary.get("primaryCauseCode")));
            double cpu = toDouble(jvm.get("processCpuRatio"));
            double threadRatio = toDouble(thread.get("busyRatio"));
            double gcTime = toLong(jvm.get("gcTimeLastMinuteMs"));
            double gcPct = gcTime > 0 ? Math.min(100, round1(gcTime / 600.0)) : 0;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("instanceLabel", formatInstanceLabel(target));
            row.put("businessCode", businessCode);
            row.put("reachable", reachable);
            row.put("status", warStatus);
            row.put("statusLabel", statusLabelKo(warStatus));
            row.put("cpuRatio", round1(cpu));
            row.put("threadRatio", round1(threadRatio));
            row.put("gcRatio", gcPct);
            row.put("riskWar", isWorseThanNormal(warStatus) ? businessCode : "-");
            row.put("primaryCauseCode", "NORMAL".equals(causeCode) ? "-" : causeCode);
            rows.add(row);
        }
        return rows;
    }

    private static boolean isWorseThanNormal(String status) {
        return "WARN".equals(status) || "CRITICAL".equals(status);
    }

    private static Map<String, Object> flowScenario(
            String id,
            String section,
            String title,
            String causeCode,
            boolean active,
            boolean primary,
            List<Map<String, Object>> steps,
            List<Map<String, Object>> links) {
        Map<String, Object> flow = new LinkedHashMap<>();
        flow.put("id", id);
        flow.put("section", section);
        flow.put("title", title);
        flow.put("causeCode", causeCode);
        flow.put("active", active);
        flow.put("primary", primary);
        flow.put("steps", steps);
        flow.put("links", links);
        return flow;
    }

    private static Map<String, Object> flowStep(int order, String label, boolean active, String detail) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("order", order);
        step.put("label", label);
        step.put("active", active);
        step.put("detail", detail != null ? detail : "-");
        return step;
    }

    private static Map<String, Object> flowLink(String label, String href) {
        Map<String, Object> link = new LinkedHashMap<>();
        link.put("label", label);
        link.put("href", href);
        return link;
    }

    private static String formatStepLabel(String step) {
        if (step == null || step.isBlank()) {
            return "-";
        }
        return switch (step) {
            case "EXECUTING_SQL" -> "SQL 실행";
            case "WAIT_DB_CONNECTION" -> "DB 대기";
            case "WAIT_EXTERNAL" -> "외부 대기";
            case "SERVICE" -> "Service 처리";
            case "FACADE" -> "Facade 처리";
            case "HANDLER" -> "Handler 처리";
            case "RULE" -> "Rule 검증";
            case "STF" -> "STF 처리";
            case "WAIT_HANDLER" -> "Handler 대기";
            default -> step;
        };
    }

    private static String formatSqlOrExternal(Map<String, Object> tx) {
        String step = stringValue(tx.get("currentStep"));
        if ("WAIT_EXTERNAL".equals(step)) {
            String external = stringValue(tx.get("externalSystemCode"));
            return external != null && !external.isBlank() ? external : "-";
        }
        if ("WAIT_DB_CONNECTION".equals(step)) {
            return "-";
        }
        return shortenSqlLabel(stringValue(tx.get("mapperSql")), stringValue(tx.get("sqlId")));
    }

    private static String shortenSqlLabel(String mapperSql, String sqlId) {
        String candidate = mapperSql;
        if (candidate == null || candidate.isBlank() || "-".equals(candidate)) {
            candidate = sqlId;
        }
        if (candidate == null || candidate.isBlank()) {
            return "-";
        }
        int lastDot = candidate.lastIndexOf('.');
        return lastDot >= 0 ? candidate.substring(lastDot + 1) : candidate;
    }

    private static String formatElapsedDisplay(long elapsedMs) {
        if (elapsedMs >= 1000) {
            return format1(elapsedMs / 1000.0) + "초";
        }
        return elapsedMs + "ms";
    }

    private static Map<String, Object> statusCard(String id, String label, String display, String level) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("id", id);
        card.put("label", label);
        card.put("display", display);
        card.put("level", level);
        return card;
    }

    private static ServiceElapsedStats aggregateElapsedForBusiness(
            String businessCode, Map<String, ServiceElapsedStats> serviceStats) {
        ServiceElapsedStats total = new ServiceElapsedStats();
        for (ServiceElapsedStats stats : serviceStats.values()) {
            if (businessCode != null && businessCode.equals(stats.businessCode)) {
                total.count += stats.count;
                total.sumElapsed += stats.sumElapsed;
                total.maxElapsed = Math.max(total.maxElapsed, stats.maxElapsed);
            }
        }
        return total;
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private static final class ServiceElapsedStats {
        private String businessCode;
        private int count;
        private long sumElapsed;
        private long maxElapsed;
    }

    private static final class WarServiceStats {
        private int active;
        private int slow;
        private final List<Long> elapsedSamples = new ArrayList<>();
        private final Map<String, Integer> stepCounts = new HashMap<>();
    }

    private static final class SlowServiceAgg {
        private int active;
        private int recentSlow;
        private long maxElapsed;
    }

    private static Map<String, Object> thresholdRow(String type, Integer designMs, long configuredMs) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("type", type);
        row.put("designThresholdMs", designMs);
        row.put("appliedThresholdMs", configuredMs);
        row.put("applied", designMs == null ? "ServiceId별 별도" : (designMs == configuredMs ? "동일" : "공통값"));
        return row;
    }

    private static int statusRank(String status) {
        if ("RUNNING".equals(status)) {
            return 0;
        }
        if ("FAILED".equals(status)) {
            return 1;
        }
        return 2;
    }

    private static String formatMapperSqlDisplay(String mapperId, String sqlId) {
        if (mapperId != null && !mapperId.isBlank()) {
            int lastDot = mapperId.lastIndexOf('.');
            if (lastDot > 0) {
                int secondLast = mapperId.lastIndexOf('.', lastDot - 1);
                if (secondLast >= 0) {
                    return mapperId.substring(secondLast + 1);
                }
            }
            return mapperId;
        }
        return sqlId != null && !sqlId.isBlank() ? sqlId : "-";
    }

    private static String buildExhaustionScreenMessage(
            String businessCode, Map<String, Object> pool, int pending) {
        String poolName = stringValue(pool.get("poolName"));
        int maximum = (int) toLong(pool.get("maximum"));
        int active = (int) toLong(pool.get("active"));
        int idle = (int) toLong(pool.get("idle"));
        return String.format(
                "%s %s%n  Max: %d / Active: %d / Idle: %d / Pending: %d%n현재 %s 거래 %d건이 DB Connection을 기다리고 있습니다.",
                businessCode,
                poolName != null ? poolName : "HikariPool",
                maximum,
                active,
                idle,
                pending,
                businessCode,
                pending);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (!(value instanceof List<?> list)) {
            return rows;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                rows.add((Map<String, Object>) map);
            }
        }
        return rows;
    }

    private static String format1(double value) {
        return String.valueOf(Math.round(value * 10) / 10.0);
    }

    private Map<String, Object> extractSharedPool(Map<String, Object> thread) {
        Map<String, Object> pool = new LinkedHashMap<>();
        pool.put("max", thread.get("max"));
        pool.put("current", thread.get("current"));
        pool.put("busy", thread.get("busy"));
        pool.put("busyRatio", thread.get("busyRatio"));
        pool.put("blocked", thread.get("blocked"));
        pool.put("deadlock", thread.get("deadlock"));
        pool.put("maxSource", thread.get("maxSource"));
        pool.put("poolName", thread.get("poolName"));
        pool.put("scope", "tomcat-shared");
        return pool;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return new LinkedHashMap<>();
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
}
