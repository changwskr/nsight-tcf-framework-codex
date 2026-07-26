package com.nh.nsight.tcf.web.support.runtime;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.runtime.ActiveTransactionRegistry;
import com.nh.nsight.tcf.core.support.runtime.SlowSqlTracker;
import com.nh.nsight.tcf.core.support.runtime.SlowTransactionTracker;
import com.nh.nsight.tcf.core.support.runtime.model.ActiveTransactionInfo;
import com.nh.nsight.tcf.core.support.runtime.model.RuntimeTransactionStep;
import com.nh.nsight.tcf.core.support.runtime.model.SlowSqlInfo;
import com.nh.nsight.tcf.core.support.runtime.model.SlowTransactionInfo;
import com.nh.nsight.tcf.core.support.runtime.model.TransactionStepEvent;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TcfRuntimeMonitor {
    private final ActiveTransactionRegistry registry;
    private final SlowTransactionTracker slowTransactionTracker;
    private final SlowSqlTracker slowSqlTracker;
    private final TcfProperties properties;
    private final DataSource dataSource;
    private final String applicationName;
    private final int serverPort;
    private final String businessCode;
    private final String version;
    private final long gcWindowStartMillis = System.currentTimeMillis();
    private long lastGcCount;
    private long lastGcTimeMs;

    public TcfRuntimeMonitor(
            ActiveTransactionRegistry registry,
            SlowTransactionTracker slowTransactionTracker,
            SlowSqlTracker slowSqlTracker,
            TcfProperties properties,
            DataSource dataSource,
            @Value("${spring.application.name:nsight-tcf}") String applicationName,
            @Value("${server.port:8080}") int serverPort,
            @Value("${nsight.tcf.runtime.business-code:}") String businessCode,
            org.springframework.beans.factory.ObjectProvider<BuildProperties> buildPropertiesProvider) {
        this.registry = registry;
        this.slowTransactionTracker = slowTransactionTracker;
        this.slowSqlTracker = slowSqlTracker;
        this.properties = properties;
        this.dataSource = dataSource;
        this.applicationName = applicationName;
        this.serverPort = serverPort;
        this.businessCode = resolveBusinessCode(applicationName, businessCode);
        this.version = buildPropertiesProvider.getIfAvailable() != null
                ? buildPropertiesProvider.getIfAvailable().getVersion()
                : "dev";
        resetGcBaseline();
    }

    public Map<String, Object> createStatusSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("instance", buildInstance());
        snapshot.put("summary", buildSummary());
        snapshot.put("thread", collectThreadStatus());
        snapshot.put("jvm", collectJvmStatus());
        snapshot.put("dbPools", collectDbPoolStatus());
        snapshot.put("activeTransactions", toActiveTransactionRows(registry.snapshot(), 50));
        snapshot.put("slowTransactions", toSlowTransactionRows(slowTransactionTracker.snapshot(20)));
        snapshot.put("slowSql", toSlowSqlRows(slowSqlTracker.snapshot(20)));
        snapshot.put("serviceCpuUsage", collectServiceCpuUsage());
        snapshot.put("runtimeConfig", buildRuntimeConfig());
        return snapshot;
    }

    public List<Map<String, Object>> getActiveTransactions() {
        return toActiveTransactionRows(registry.snapshot(), 200);
    }

    public List<Map<String, Object>> getSlowTransactions(int limit) {
        return toSlowTransactionRows(slowTransactionTracker.snapshot(limit));
    }

    public List<Map<String, Object>> getSlowSqlList(int limit) {
        return toSlowSqlRows(slowSqlTracker.snapshot(limit));
    }

    public List<Map<String, Object>> getThreadDetails() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ActiveTransactionInfo info : registry.snapshot()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("threadId", info.threadId());
            row.put("threadName", info.threadName());
            row.put("threadState", resolveThreadState(info.threadId()));
            row.put("businessCode", info.businessCode());
            row.put("serviceId", info.serviceId());
            row.put("guid", maskGuid(info.guid()));
            row.put("elapsedMs", info.elapsedMillis());
            row.put("currentStep", info.currentStep() == null ? null : info.currentStep().name());
            row.put("sqlId", info.currentSqlId());
            row.put("mapperSql", formatMapperSql(info.currentSqlId(), null));
            row.put("externalSystemCode", info.externalSystemCode());
            row.put("dbWaitMs", resolveDbWaitMillis(info));
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Object> buildInstance() {
        Map<String, Object> instance = new LinkedHashMap<>();
        instance.put("hostName", resolveHostName());
        instance.put("port", serverPort);
        instance.put("applicationName", applicationName);
        instance.put("businessCode", businessCode);
        instance.put("version", version);
        instance.put("pid", resolveProcessId());
        return instance;
    }

    private long resolveProcessId() {
        try {
            String name = ManagementFactory.getRuntimeMXBean().getName();
            int at = name.indexOf('@');
            if (at > 0) {
                return Long.parseLong(name.substring(0, at));
            }
        } catch (Exception ignored) {
            // PID 미수집
        }
        return ProcessHandle.current().pid();
    }

    private String resolveThreadState(long threadId) {
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        java.lang.management.ThreadInfo info = threads.getThreadInfo(threadId);
        if (info == null || info.getThreadState() == null) {
            return "UNKNOWN";
        }
        return info.getThreadState().name();
    }

    private Map<String, Object> buildSummary() {
        Map<String, Object> thread = collectThreadStatus();
        Map<String, Object> jvm = collectJvmStatus();
        List<Map<String, Object>> pools = collectDbPoolStatus();
        double busyRatio = toDouble(thread.get("busyRatio"));
        double heapRatio = toDouble(jvm.get("heapRatio"));
        double processCpu = toDouble(jvm.get("processCpuRatio"));
        boolean deadlock = Boolean.TRUE.equals(thread.get("deadlock"));
        int pending = sumPending(pools);
        int activeTx = registry.count();
        long oneMinuteAgo = System.currentTimeMillis() - 60_000L;
        int slowTx = slowTransactionTracker.countRecent(oneMinuteAgo);
        int slowSql = slowSqlTracker.countRecent(oneMinuteAgo);

        String causeCode = "NORMAL";
        String message = "현재 주요 병목이 없습니다.";
        String status = "NORMAL";

        if (deadlock) {
            causeCode = "THREAD_DEADLOCK";
            message = "Thread Deadlock이 발견되었습니다.";
            status = "CRITICAL";
        } else if (isPoolExhausted(pools)) {
            causeCode = "DB_POOL_EXHAUSTED";
            message = "DB Connection을 얻지 못해 거래가 대기 중입니다.";
            status = "CRITICAL";
        } else if (heapRatio >= 80 && toLong(jvm.get("gcTimeLastMinuteMs")) >= 3000) {
            causeCode = "GC_PRESSURE";
            message = "GC 증가로 JVM 응답이 지연되고 있습니다.";
            status = "WARN";
        } else if (processCpu >= 90 && pending == 0) {
            causeCode = "CPU_OVERLOAD";
            message = "JVM CPU가 과부하 상태입니다. 업무 연산 또는 과도한 Thread 실행 가능";
            status = "WARN";
        } else if (busyRatio >= 85 && slowTx >= 5) {
            causeCode = "THREAD_SATURATION";
            message = "Tomcat Thread 부족 또는 장기 거래 점유 상태입니다.";
            status = "WARN";
        } else if (slowSql >= 3) {
            causeCode = "SLOW_SQL";
            message = "특정 Mapper SQL이 장시간 실행 중입니다.";
            status = "WARN";
        } else if (countExternalWait() >= 3) {
            causeCode = "EXTERNAL_WAIT";
            message = "외부 시스템 응답을 기다리고 있습니다.";
            status = "WARN";
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("status", status);
        summary.put("primaryCauseCode", causeCode);
        summary.put("message", message);
        summary.put("activeTransactionCount", activeTx);
        summary.put("slowTransactionCount", slowTx);
        summary.put("slowSqlCount", slowSql);
        return summary;
    }

    public Map<String, Object> collectThreadStatus() {
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        int live = threads.getThreadCount();
        int activeTx = registry.count();

        Optional<TomcatThreadPoolProbe.Stats> tomcatPool = TomcatThreadPoolProbe.resolvePrimaryHttpPool();
        int max = tomcatPool.map(TomcatThreadPoolProbe.Stats::maxThreads)
                .filter(value -> value > 0)
                .orElse(Math.max(live + 50, 200));
        int busy = tomcatPool.map(TomcatThreadPoolProbe.Stats::busyThreads)
                .filter(value -> value > 0)
                .orElse(Math.max(activeTx, Math.min(live, live - threads.getDaemonThreadCount())));
        if (tomcatPool.isPresent() && tomcatPool.get().currentThreads() > 0) {
            live = tomcatPool.get().currentThreads();
        }
        double busyRatio = max > 0 ? busy * 100.0 / max : 0;
        long[] deadlocked = threads.findDeadlockedThreads();
        int deadlockCount = deadlocked != null ? deadlocked.length : 0;

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("max", max);
        row.put("current", live);
        row.put("busy", busy);
        row.put("busyRatio", round1(busyRatio));
        row.put("activeTransactionCount", activeTx);
        row.put("slowTransactionCount", slowTransactionTracker.countRecent(System.currentTimeMillis() - 60_000L));
        row.put("blocked", countBlockedHttpThreads(threads));
        row.put("deadlock", deadlockCount > 0);
        row.put("deadlockCount", deadlockCount);
        row.put("maxSource", tomcatPool.isPresent() ? "tomcat-jmx" : "estimated");
        Optional<TomcatThreadPoolProbe.AcceptQueue> acceptQueue = TomcatThreadPoolProbe.resolveAcceptQueue();
        if (acceptQueue.isPresent()) {
            row.put("acceptQueue", acceptQueue.get().current());
            row.put("acceptQueueMax", acceptQueue.get().max());
        } else {
            row.put("acceptQueue", row.get("blocked"));
            row.put("acceptQueueMax", 500);
        }
        if (tomcatPool.isPresent()) {
            row.put("poolName", tomcatPool.get().poolName());
        }
        return row;
    }

    private int countBlockedHttpThreads(ThreadMXBean threads) {
        long[] ids = threads.getAllThreadIds();
        java.lang.management.ThreadInfo[] infos = threads.getThreadInfo(ids);
        if (infos == null) {
            return 0;
        }
        int blocked = 0;
        for (java.lang.management.ThreadInfo info : infos) {
            if (info == null || info.getThreadState() != Thread.State.BLOCKED) {
                continue;
            }
            String name = info.getThreadName();
            if (name != null && name.startsWith("http-")) {
                blocked++;
            }
        }
        return blocked;
    }

    private List<Map<String, Object>> collectServiceCpuUsage() {
        ThreadMXBean threadMx = ManagementFactory.getThreadMXBean();
        if (!threadMx.isThreadCpuTimeSupported()) {
            return List.of();
        }
        if (!threadMx.isThreadCpuTimeEnabled()) {
            threadMx.setThreadCpuTimeEnabled(true);
        }

        Map<String, Integer> threadsByService = new LinkedHashMap<>();
        Map<String, Long> cpuNanosByService = new LinkedHashMap<>();
        Map<String, Long> maxElapsedByService = new LinkedHashMap<>();
        long totalCpuNanos = 0;

        for (ActiveTransactionInfo info : registry.snapshot()) {
            String serviceId = info.serviceId();
            if (serviceId == null || serviceId.isBlank()) {
                continue;
            }
            long cpuNanos = Math.max(0L, threadMx.getThreadCpuTime(info.threadId()));
            threadsByService.merge(serviceId, 1, Integer::sum);
            cpuNanosByService.merge(serviceId, cpuNanos, Long::sum);
            maxElapsedByService.merge(serviceId, info.elapsedMillis(), Math::max);
            totalCpuNanos += cpuNanos;
        }
        if (totalCpuNanos <= 0 || threadsByService.isEmpty()) {
            return List.of();
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : threadsByService.entrySet()) {
            String serviceId = entry.getKey();
            long cpuNanos = cpuNanosByService.getOrDefault(serviceId, 0L);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("serviceId", serviceId);
            row.put("threadCount", entry.getValue());
            row.put("cpuSharePct", round1(cpuNanos * 100.0 / totalCpuNanos));
            row.put("maxElapsedMs", maxElapsedByService.getOrDefault(serviceId, 0L));
            rows.add(row);
        }
        rows.sort((left, right) -> Double.compare(
                toDouble(right.get("cpuSharePct")), toDouble(left.get("cpuSharePct"))));
        return rows;
    }

    public Map<String, Object> collectJvmStatus() {
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        double heapRatio = heap.getMax() > 0 ? heap.getUsed() * 100.0 / heap.getMax() : 0;
        long gcTimeLastMinute = measureGcTimeLastMinute();

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("processCpuRatio", round1(resolveProcessCpuUsagePct()));
        row.put("heapUsedMb", round1(heap.getUsed() / 1024.0 / 1024.0));
        row.put("heapMaxMb", round1(heap.getMax() / 1024.0 / 1024.0));
        row.put("heapRatio", round1(heapRatio));
        row.put("oldGenRatio", round1(resolveOldGenRatio()));
        row.put("metaspaceMb", round1(resolveMetaspaceMb()));
        row.put("metaspaceMaxMb", round1(resolveMetaspaceMaxMb()));
        row.put("gcCountLastMinute", measureGcCountLastMinute());
        row.put("gcTimeLastMinuteMs", gcTimeLastMinute);
        row.put("liveThreadCount", ManagementFactory.getThreadMXBean().getThreadCount());
        row.put("deadlock", Boolean.TRUE.equals(collectThreadStatus().get("deadlock")));
        row.put("uptimeMs", ManagementFactory.getRuntimeMXBean().getUptime());
        return row;
    }

    public List<Map<String, Object>> collectDbPoolStatus() {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (dataSource instanceof HikariDataSource hikari) {
            HikariPoolMXBean pool = hikari.getHikariPoolMXBean();
            int max = hikari.getMaximumPoolSize();
            int active = pool == null ? 0 : pool.getActiveConnections();
            int idle = pool == null ? 0 : pool.getIdleConnections();
            int pending = pool == null ? 0 : pool.getThreadsAwaitingConnection();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("poolName", hikari.getPoolName());
            row.put("maximum", max);
            row.put("active", active);
            row.put("idle", idle);
            row.put("pending", pending);
            row.put("usageRatio", max > 0 ? round1(active * 100.0 / max) : 0);
            row.put("connectionTimeoutCount", 0);
            row.put("longUsageConnections", countLongUsageConnections());
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> toActiveTransactionRows(List<ActiveTransactionInfo> source, int limit) {
        List<Map<String, Object>> rows = new ArrayList<>();
        int index = 0;
        for (ActiveTransactionInfo info : source) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("serviceId", info.serviceId());
            row.put("businessCode", info.businessCode());
            row.put("guid", maskGuid(info.guid()));
            row.put("traceId", maskTraceId(info.traceId()));
            row.put("threadName", info.threadName());
            row.put("threadId", info.threadId());
            row.put("startTimeMillis", info.startTimeMillis());
            row.put("transactionCode", info.transactionCode());
            row.put("elapsedMs", info.elapsedMillis());
            row.put("currentStep", info.currentStep() == null ? null : info.currentStep().name());
            row.put("sqlId", info.currentSqlId());
            row.put("mapperSql", formatMapperSql(info.currentSqlId(), null));
            row.put("externalSystemCode", info.externalSystemCode());
            row.put("dbWaitMs", resolveDbWaitMillis(info));
            row.put("stepHistory", toStepHistoryRows(registry.getStepHistory(info.guid())));
            rows.add(row);
            index++;
            if (index >= limit) {
                break;
            }
        }
        return rows;
    }

    private static List<Map<String, Object>> toStepHistoryRows(List<TransactionStepEvent> events) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (TransactionStepEvent event : events) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("stepKey", event.stepKey());
            row.put("label", event.label());
            row.put("timestampMillis", event.timestampMillis());
            row.put("durationMs", event.durationMs());
            row.put("highlight", event.highlight());
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> toSlowTransactionRows(List<SlowTransactionInfo> source) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SlowTransactionInfo info : source) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("serviceId", info.serviceId());
            row.put("businessCode", info.businessCode());
            row.put("guid", maskGuid(info.guid()));
            row.put("elapsedMs", info.elapsedMillis());
            row.put("lastStep", info.lastStep() == null ? null : info.lastStep().name());
            row.put("sqlId", info.lastSqlId());
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> toSlowSqlRows(List<SlowSqlInfo> source) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SlowSqlInfo info : source) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("mapperId", info.mapperId());
            row.put("sqlId", info.sqlId());
            row.put("mapperSql", formatMapperSql(info.mapperId(), info.sqlId()));
            row.put("serviceId", info.serviceId());
            row.put("startTimeMillis", info.startTimeMillis());
            row.put("endTimeMillis", info.endTimeMillis());
            row.put("elapsedMs", info.elapsedMillis());
            row.put("success", info.success());
            row.put("status", info.success() ? "COMPLETED" : "FAILED");
            if (info.affectedRows() >= 0) {
                row.put("affectedRows", info.affectedRows());
            }
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Object> buildRuntimeConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("slowSqlThresholdMs", properties.getRuntimeSlowSqlThresholdMs());
        config.put("onlineTimeoutSec", 60);
        return config;
    }

    static String formatMapperSql(String mapperId, String sqlId) {
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

    private boolean isPoolExhausted(List<Map<String, Object>> pools) {
        for (Map<String, Object> pool : pools) {
            int maximum = (int) toLong(pool.get("maximum"));
            int active = (int) toLong(pool.get("active"));
            int idle = (int) toLong(pool.get("idle"));
            int pending = (int) toLong(pool.get("pending"));
            if (maximum > 0 && active >= maximum && idle == 0 && pending > 0) {
                return true;
            }
        }
        return false;
    }

    private int countExternalWait() {
        int count = 0;
        for (ActiveTransactionInfo info : registry.snapshot()) {
            if (info.currentStep() != null && "WAIT_EXTERNAL".equals(info.currentStep().name())) {
                count++;
            }
        }
        return count;
    }

    private int countLongUsageConnections() {
        int count = 0;
        long now = System.currentTimeMillis();
        for (ActiveTransactionInfo info : registry.snapshot()) {
            if (info.currentStep() == RuntimeTransactionStep.WAIT_DB_CONNECTION) {
                if (info.dbWaitStartMillis() > 0 && now - info.dbWaitStartMillis() >= 3_000L) {
                    count++;
                }
            } else if (info.currentStep() == RuntimeTransactionStep.EXECUTING_SQL
                    && info.elapsedMillis() >= 10_000L) {
                count++;
            }
        }
        return count;
    }

    private long resolveDbWaitMillis(ActiveTransactionInfo info) {
        if (info.currentStep() == RuntimeTransactionStep.WAIT_DB_CONNECTION && info.dbWaitStartMillis() > 0) {
            return Math.max(0L, System.currentTimeMillis() - info.dbWaitStartMillis());
        }
        return 0L;
    }

    private int sumPending(List<Map<String, Object>> pools) {
        int pending = 0;
        for (Map<String, Object> pool : pools) {
            pending += (int) toLong(pool.get("pending"));
        }
        return pending;
    }

    private long measureGcTimeLastMinute() {
        long total = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            total += bean.getCollectionTime();
        }
        long delta = Math.max(0, total - lastGcTimeMs);
        resetGcBaseline();
        return delta;
    }

    private long measureGcCountLastMinute() {
        long total = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            total += bean.getCollectionCount();
        }
        long delta = Math.max(0, total - lastGcCount);
        return delta;
    }

    private void resetGcBaseline() {
        long gcCount = 0;
        long gcTime = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCount += bean.getCollectionCount();
            gcTime += bean.getCollectionTime();
        }
        lastGcCount = gcCount;
        lastGcTimeMs = gcTime;
        // baseline timestamp kept for future trend use
        // no-op read of gcWindowStartMillis to avoid unused warning
        if (gcWindowStartMillis < 0) {
            return;
        }
    }

    private double resolveOldGenRatio() {
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            String name = pool.getName();
            if (name != null && name.contains("Old")) {
                MemoryUsage usage = pool.getUsage();
                if (usage != null && usage.getMax() > 0) {
                    return usage.getUsed() * 100.0 / usage.getMax();
                }
            }
        }
        return 0;
    }

    private double resolveMetaspaceMb() {
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if ("Metaspace".equals(pool.getName())) {
                MemoryUsage usage = pool.getUsage();
                if (usage != null) {
                    return usage.getUsed() / 1024.0 / 1024.0;
                }
            }
        }
        return 0;
    }

    private double resolveMetaspaceMaxMb() {
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if ("Metaspace".equals(pool.getName())) {
                MemoryUsage usage = pool.getUsage();
                if (usage != null && usage.getMax() > 0) {
                    return usage.getMax() / 1024.0 / 1024.0;
                }
            }
        }
        return 0;
    }

    private double resolveProcessCpuUsagePct() {
        OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
        if (bean instanceof com.sun.management.OperatingSystemMXBean sunBean) {
            double load = sunBean.getProcessCpuLoad();
            if (load >= 0) {
                return load * 100.0;
            }
        }
        double systemLoad = bean.getSystemLoadAverage();
        return systemLoad >= 0 ? Math.min(systemLoad * 10, 100) : 0;
    }

    private String resolveHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "localhost";
        }
    }

    private static String resolveBusinessCode(String applicationName, String configured) {
        if (StringUtils.hasText(configured)) {
            return configured.trim().toUpperCase();
        }
        if (!StringUtils.hasText(applicationName)) {
            return "TCF";
        }
        String normalized = applicationName.toLowerCase();
        if (normalized.contains("-om")) {
            return "OM";
        }
        if (normalized.contains("-sv") || normalized.contains("sv-service")) {
            return "SV";
        }
        if (normalized.contains("-ic") || normalized.contains("ic-service")) {
            return "IC";
        }
        if (normalized.contains("-mg") || normalized.contains("mg-service")) {
            return "MG";
        }
        if (normalized.contains("-batch")) {
            return "BATCH";
        }
        if (normalized.contains("-gateway")) {
            return "GATEWAY";
        }
        return applicationName.length() >= 2 ? applicationName.substring(0, 2).toUpperCase() : "TCF";
    }

    private static String maskGuid(String guid) {
        if (guid == null || guid.length() < 8) {
            return "masked";
        }
        return guid.substring(0, 4) + "****" + guid.substring(guid.length() - 4);
    }

    private static String maskTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            return "-";
        }
        if (traceId.length() < 8) {
            return traceId;
        }
        return traceId.substring(0, 4) + "****" + traceId.substring(traceId.length() - 4);
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
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
