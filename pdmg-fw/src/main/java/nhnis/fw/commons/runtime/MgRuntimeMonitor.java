package nhnis.fw.commons.runtime;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

/**
 * pdmg-service 단일 인스턴스 런타임 스냅샷 수집기 (OM TcfRuntimeMonitor 축약).
 */
@Component
public class MgRuntimeMonitor {

    private static final DateTimeFormatter CHECKED_AT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final MgActiveTransactionRegistry registry;
    private final DataSource dataSource;
    private final String applicationName;
    private final int serverPort;

    private long lastGcCount;
    private long lastGcTimeMs;
    private long gcWindowStartMillis = System.currentTimeMillis();

    public MgRuntimeMonitor(
            MgActiveTransactionRegistry registry,
            DataSource dataSource,
            @Value("${spring.application.name:pdmg}") String applicationName,
            @Value("${server.port:8080}") int serverPort) {
        this.registry = registry;
        this.dataSource = dataSource;
        this.applicationName = applicationName;
        this.serverPort = serverPort;
        resetGcBaseline();
    }

    public Map<String, Object> createStatusSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("instance", buildInstance());
        Map<String, Object> thread = collectThreadStatus();
        Map<String, Object> jvm = collectJvmStatus();
        List<Map<String, Object>> pools = collectDbPoolStatus();
        Map<String, Object> summary = buildSummary(thread, jvm, pools);
        snapshot.put("summary", summary);
        snapshot.put("thread", thread);
        snapshot.put("jvm", jvm);
        snapshot.put("dbPools", pools);
        snapshot.put("activeTransactions", registry.snapshot(50));
        snapshot.put("slowTransactions", List.of());
        snapshot.put("slowSql", List.of());
        return snapshot;
    }

    public List<Map<String, Object>> getActiveTransactions() {
        return registry.snapshot(200);
    }

    public String checkedAt() {
        return CHECKED_AT.format(Instant.now());
    }

    private Map<String, Object> buildInstance() {
        Map<String, Object> instance = new LinkedHashMap<>();
        instance.put("hostName", resolveHostName());
        instance.put("port", serverPort);
        instance.put("applicationName", applicationName);
        instance.put("businessCode", "MG");
        instance.put("pid", ProcessHandle.current().pid());
        return instance;
    }

    private Map<String, Object> buildSummary(
            Map<String, Object> thread,
            Map<String, Object> jvm,
            List<Map<String, Object>> pools) {
        double busyRatio = toDouble(thread.get("busyRatio"));
        double heapRatio = toDouble(jvm.get("heapRatio"));
        double processCpu = toDouble(jvm.get("processCpuRatio"));
        boolean deadlock = Boolean.TRUE.equals(thread.get("deadlock"));
        int pending = sumPending(pools);
        int activeTx = registry.count();
        int slowTx = 0;
        int slowSql = 0;

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
            message = "JVM CPU가 과부하 상태입니다.";
            status = "WARN";
        } else if (busyRatio >= 85 && activeTx >= 5) {
            causeCode = "THREAD_SATURATION";
            message = "Tomcat Thread 부족 또는 장기 거래 점유 상태입니다.";
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
        Optional<MgTomcatThreadPoolProbe.Stats> tomcatPool = MgTomcatThreadPoolProbe.resolvePrimaryHttpPool();
        int max = tomcatPool.map(MgTomcatThreadPoolProbe.Stats::maxThreads)
                .filter(v -> v > 0)
                .orElse(Math.max(live + 50, 200));
        int busy = tomcatPool.map(MgTomcatThreadPoolProbe.Stats::busyThreads)
                .filter(v -> v > 0)
                .orElse(Math.max(activeTx, Math.min(live, live - threads.getDaemonThreadCount())));
        if (tomcatPool.isPresent() && tomcatPool.get().currentThreads() > 0) {
            live = tomcatPool.get().currentThreads();
        }
        double busyRatio = max > 0 ? busy * 100.0 / max : 0;
        long[] deadlocked = threads.findDeadlockedThreads();

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("max", max);
        row.put("current", live);
        row.put("busy", busy);
        row.put("busyRatio", round1(busyRatio));
        row.put("activeTransactionCount", activeTx);
        row.put("slowTransactionCount", 0);
        row.put("deadlock", deadlocked != null && deadlocked.length > 0);
        row.put("deadlockCount", deadlocked == null ? 0 : deadlocked.length);
        row.put("maxSource", tomcatPool.isPresent() ? "tomcat-jmx" : "estimated");
        return row;
    }

    public Map<String, Object> collectJvmStatus() {
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        long used = memory.getHeapMemoryUsage().getUsed();
        long max = memory.getHeapMemoryUsage().getMax();
        if (max <= 0) {
            max = memory.getHeapMemoryUsage().getCommitted();
        }
        double heapRatio = max > 0 ? used * 100.0 / max : 0;

        long gcCount = 0;
        long gcTime = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCount += Math.max(0, gc.getCollectionCount());
            gcTime += Math.max(0, gc.getCollectionTime());
        }
        long windowMs = Math.max(1L, System.currentTimeMillis() - gcWindowStartMillis);
        long deltaCount = Math.max(0, gcCount - lastGcCount);
        long deltaTime = Math.max(0, gcTime - lastGcTimeMs);
        if (windowMs >= 60_000L) {
            lastGcCount = gcCount;
            lastGcTimeMs = gcTime;
            gcWindowStartMillis = System.currentTimeMillis();
        }

        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        double processCpu = -1;
        if (os instanceof com.sun.management.OperatingSystemMXBean sunOs) {
            processCpu = sunOs.getProcessCpuLoad() * 100.0;
        }

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("heapUsedMb", round1(used / (1024.0 * 1024.0)));
        row.put("heapMaxMb", round1(max / (1024.0 * 1024.0)));
        row.put("heapRatio", round1(heapRatio));
        row.put("processCpuRatio", processCpu < 0 ? 0 : round1(processCpu));
        row.put("gcCountLastMinute", deltaCount);
        row.put("gcTimeLastMinuteMs", deltaTime);
        row.put("systemLoadAverage", os.getSystemLoadAverage());
        return row;
    }

    public List<Map<String, Object>> collectDbPoolStatus() {
        List<Map<String, Object>> pools = new ArrayList<>();
        HikariDataSource hikari = unwrapHikari(dataSource);
        if (hikari == null) {
            return pools;
        }
        HikariPoolMXBean mx = hikari.getHikariPoolMXBean();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("businessCode", "MG");
        row.put("poolName", hikari.getPoolName());
        if (mx != null) {
            row.put("active", mx.getActiveConnections());
            row.put("idle", mx.getIdleConnections());
            row.put("pending", mx.getThreadsAwaitingConnection());
            row.put("total", mx.getTotalConnections());
        } else {
            row.put("active", 0);
            row.put("idle", 0);
            row.put("pending", 0);
            row.put("total", 0);
        }
        row.put("maximum", hikari.getMaximumPoolSize());
        pools.add(row);
        return pools;
    }

    private void resetGcBaseline() {
        long gcCount = 0;
        long gcTime = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCount += Math.max(0, gc.getCollectionCount());
            gcTime += Math.max(0, gc.getCollectionTime());
        }
        lastGcCount = gcCount;
        lastGcTimeMs = gcTime;
        gcWindowStartMillis = System.currentTimeMillis();
    }

    private static HikariDataSource unwrapHikari(DataSource dataSource) {
        if (dataSource == null) {
            return null;
        }
        if (dataSource instanceof HikariDataSource hikari) {
            return hikari;
        }
        try {
            if (dataSource.isWrapperFor(HikariDataSource.class)) {
                return dataSource.unwrap(HikariDataSource.class);
            }
        } catch (Exception ignored) {
            // ignore
        }
        return null;
    }

    private static boolean isPoolExhausted(List<Map<String, Object>> pools) {
        for (Map<String, Object> pool : pools) {
            int active = (int) toLong(pool.get("active"));
            int maximum = (int) toLong(pool.get("maximum"));
            int pending = (int) toLong(pool.get("pending"));
            if (maximum > 0 && active >= maximum && pending > 0) {
                return true;
            }
        }
        return false;
    }

    private static int sumPending(List<Map<String, Object>> pools) {
        int sum = 0;
        for (Map<String, Object> pool : pools) {
            sum += (int) toLong(pool.get("pending"));
        }
        return sum;
    }

    private static String resolveHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "localhost";
        }
    }

    private static double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
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
        if (value == null) {
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
