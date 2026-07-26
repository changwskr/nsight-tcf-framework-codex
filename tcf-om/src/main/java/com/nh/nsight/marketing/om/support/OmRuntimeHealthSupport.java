package com.nh.nsight.marketing.om.support;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OmRuntimeHealthSupport {
    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final String applicationName;
    private final int serverPort;

    public OmRuntimeHealthSupport(JdbcTemplate jdbcTemplate, DataSource dataSource,
                                  @Value("${spring.application.name:nsight-tcf-om}") String applicationName,
                                  @Value("${server.port:8097}") int serverPort) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.applicationName = applicationName;
        this.serverPort = serverPort;
    }

    public Map<String, Object> snapshotLocalAp(String checkedAt) {
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        double heapPct = heap.getMax() > 0 ? heap.getUsed() * 100.0 / heap.getMax() : 0;
        double cpuPct = resolveProcessCpuUsagePct();

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("apId", "om-local");
        row.put("apName", applicationName + " (:" + serverPort + ")");
        row.put("healthStatus", resolveHealthStatus(heapPct, cpuPct, true));
        row.put("cpuUsagePct", round1(cpuPct));
        row.put("heapUsagePct", round1(heapPct));
        row.put("threadCount", threads.getThreadCount());
        row.put("checkedAt", checkedAt);
        return row;
    }

    public Map<String, Object> snapshotPrimaryDb(String checkedAt) {
        double poolPct = 0;
        int activeConnections = 0;
        int maxPoolSize = 0;
        if (dataSource instanceof HikariDataSource hikari) {
            maxPoolSize = hikari.getMaximumPoolSize();
            HikariPoolMXBean pool = hikari.getHikariPoolMXBean();
            if (pool != null) {
                activeConnections = pool.getActiveConnections();
                poolPct = maxPoolSize > 0 ? activeConnections * 100.0 / maxPoolSize : 0;
            }
        }

        boolean dbReachable = pingDatabase();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("dbId", "OM-DS");
        row.put("dbName", "nsight_om (Primary H2)");
        row.put("healthStatus", !dbReachable ? "FAIL" : resolveHealthStatus(poolPct, 0, false));
        row.put("poolUsagePct", round1(poolPct));
        row.put("checkedAt", checkedAt);
        row.put("activeConnections", activeConnections);
        row.put("maxPoolSize", maxPoolSize);
        return row;
    }

    public void upsertApStatus(Map<String, Object> row) {
        jdbcTemplate.update("""
                MERGE INTO OM_AP_STATUS (AP_ID, AP_NAME, HEALTH_STATUS, CPU_USAGE_PCT, HEAP_USAGE_PCT,
                                         THREAD_COUNT, CHECKED_AT)
                KEY (AP_ID)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                row.get("apId"),
                row.get("apName"),
                row.get("healthStatus"),
                row.get("cpuUsagePct"),
                row.get("heapUsagePct"),
                row.get("threadCount"),
                row.get("checkedAt"));
    }

    public void upsertDbStatus(Map<String, Object> row) {
        jdbcTemplate.update("""
                MERGE INTO OM_DB_STATUS (DB_ID, DB_NAME, HEALTH_STATUS, POOL_USAGE_PCT, CHECKED_AT)
                KEY (DB_ID)
                VALUES (?, ?, ?, ?, ?)
                """,
                row.get("dbId"),
                row.get("dbName"),
                row.get("healthStatus"),
                row.get("poolUsagePct"),
                row.get("checkedAt"));
    }

    private boolean pingDatabase() {
        try {
            Integer value = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return value != null && value == 1;
        } catch (Exception e) {
            return false;
        }
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

    private String resolveHealthStatus(double primaryPct, double secondaryPct, boolean apNode) {
        if (primaryPct >= 95 || secondaryPct >= 95) {
            return "FAIL";
        }
        double warnThreshold = apNode ? 85 : 80;
        if (primaryPct >= warnThreshold || secondaryPct >= warnThreshold) {
            return "WARN";
        }
        return "NORMAL";
    }

    private double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
