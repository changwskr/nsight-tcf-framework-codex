package com.nh.nsight.marketing.oc.support;

import com.nh.nsight.marketing.oc.application.dto.env.LayerGridRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 설계서 5.x 계층별 설정 Rule (권장값·위치·예시·조치). */
public final class StackLayerRuleCatalog {

    private StackLayerRuleCatalog() {
    }

    public static List<LayerGridRow> buildGrid(
            Map<String, String> config,
            int sessionMinutes,
            int timeoutSec,
            VmProfile vm,
            int vmCores,
            int vmMemoryGb
    ) {
        List<LayerGridRow> rows = new ArrayList<>();
        VmProfile profile = vm != null ? vm : VmProfile.nearest(vmCores, vmMemoryGb);
        TomcatHikariSizingGuide.ProfileSpec th = profile.getTomcatHikariSpec();
        var pool = DbPoolSizingGuide.recommend(profile, vmCores, vmMemoryGb);
        LoadBalancerSizingGuide.ProfileSpec lb = LoadBalancerSizingGuide.specFor(
                profile, sessionMinutes, Nsight32Core256GbGuide.DEFAULT_AP_COUNT, th.maxConnectionsMax());

        rows.add(row("UI", "REQUEST_TIMEOUT", "nsight.webtop.request-timeout-ms",
                "15000 ms", "application.yml", "REQUEST_TIMEOUT=15000",
                "15초", config, "사용자 최종 대기. 서버 Timeout보다 길게"));
        rows.add(row("UI", "CONNECT_TIMEOUT", "nsight.webtop.connect-timeout-ms",
                "3000 ms", "application.yml", "CONNECT_TIMEOUT=3000",
                "3초", config, "AP 연결 실패 판단"));
        rows.add(row("UI", "READ_TIMEOUT", "nsight.webtop.read-timeout-ms",
                "10000 ms", "application.yml", "READ_TIMEOUT=10000",
                "10초", config, "AP 응답 대기"));
        rows.add(row("UI", "RETRY_COUNT", "nsight.webtop.retry-count",
                "0~1", "application.yml", "RETRY_COUNT=1",
                "1", config, "무분별 재시도 금지"));

        rows.add(row("GSLB", "DNS TTL", "—",
                lb.dnsTtl(), "GSLB Policy", "ttl=30",
                "—", config, "센터 장애 시 재조회 — 짧게 유지"));
        rows.add(row("GSLB", "Health Check Interval", "nsight.env-check.gslb-health-interval-sec",
                lb.healthIntervalSec() + "초", "GSLB Monitor", "interval=" + lb.healthIntervalSec(),
                String.valueOf(lb.healthIntervalSec()), config, "대형 VM 장애 영향 — 5초 기준"));
        rows.add(row("GSLB", "Health Check Timeout", "nsight.env-check.gslb-health-timeout-sec",
                lb.healthTimeoutSec() + "초", "GSLB Monitor", "timeout=" + lb.healthTimeoutSec(),
                String.valueOf(lb.healthTimeoutSec()), config, "지연 AP 빠른 제외"));
        rows.add(row("GSLB", "Health Fail Count", "nsight.env-check.gslb-health-fail-count",
                lb.healthFailCountRange() + "회", "GSLB Monitor", "fail=" + lb.healthFailCountRange(),
                String.valueOf(lb.healthFailCountRange().contains("3") ? 3 : 2), config,
                "일시 지연과 실제 장애 구분"));
        rows.add(row("GSLB", "Routing", "—",
                "Active-Active 50:50", "GSLB Pool", "dc1:50%, dc2:50%",
                "—", config, "양센터 분산"));

        rows.add(row("L4", "Sticky / Persistence", "—",
                lb.stickyPersistence(), "L4 Persistence", "cookie · source-ip",
                "적용", config, lb.stickyPersistence()));
        rows.add(row("L4", "Sticky Timeout", "nsight.env-check.l4-sticky-timeout-sec",
                lb.stickyTimeoutRange(), "L4 Persistence",
                "sticky=" + (lb.stickyTimeoutSecDefault() / 60) + "min",
                String.valueOf(lb.stickyTimeoutSecDefault()), config,
                "세션 " + sessionMinutes + "분보다 길게 (70~80분)"));
        rows.add(row("L4", "Health Check Interval", "nsight.env-check.l4-health-interval-sec",
                lb.healthIntervalSec() + "초", "L4 Monitor", "interval=" + lb.healthIntervalSec(),
                String.valueOf(lb.healthIntervalSec()), config, "대형 VM — 5초 권장"));
        rows.add(row("L4", "Health Check Timeout", "nsight.env-check.l4-health-timeout-sec",
                lb.healthTimeoutSec() + "초", "L4 Monitor", "timeout=" + lb.healthTimeoutSec(),
                String.valueOf(lb.healthTimeoutSec()), config, "지연 AP 빠른 제외"));
        rows.add(row("L4", "Health Fail Count", "nsight.env-check.l4-health-fail-count",
                lb.healthFailCountRange() + "회", "L4 Monitor", "fail=" + lb.healthFailCountRange(),
                String.valueOf(3), config, "일시 지연과 실제 장애 구분"));
        rows.add(row("L4", "Client-L4 Idle Timeout", "nsight.env-check.l4-idle-timeout-ms",
                lb.clientL4IdleRange(), "L4 TCP Profile (Client)",
                "idle-timeout=" + lb.clientL4IdleSecDefault(),
                lb.clientL4IdleSecDefault() * 1000 + " ms", config,
                "WebTopSuite ↔ L4 유휴 연결"));
        rows.add(row("L4", "L4-WAS Idle Timeout", "nsight.env-check.l4-was-idle-timeout-sec",
                lb.l4WasIdleRange(), "L4 → WAS TCP Profile",
                "idle-timeout=" + lb.l4WasIdleSecDefault(),
                String.valueOf(lb.l4WasIdleSecDefault()), config, lb.tomcatKeepAliveNote()));
        rows.add(row("L4", "Load Balancing", "—",
                lb.loadBalancingMethod(), "L4 Virtual Server", "lb-method least-conn",
                "—", config, "32CORE VM — Round Robin보다 연결 수 기반 분산"));
        rows.add(row("L4", "Max Connection", "—",
                lb.maxConnectionGuidance(), "L4 Global Limit", "max-connections",
                "—", config, "센터 AP수 × VM maxConnections 이상 검증"));

        rows.add(row("Apache", "ProxyTimeout", "nsight.env-check.proxy-read-timeout-ms",
                "10000 ms", "httpd.conf", "ProxyTimeout 10",
                "10000 ms", config, "AP 응답 상한"));
        rows.add(row("Apache", "Timeout", "—",
                "15초", "httpd.conf", "Timeout 15",
                "—", config, "전체 요청 상한"));
        rows.add(row("Apache", "KeepAliveTimeout", "—",
                "5초", "httpd.conf", "KeepAliveTimeout 5",
                "—", config, "연결 점유 방지"));

        String maxThreadsNote = profile == VmProfile.CORE32_256
                ? TomcatWasSizingGuide.maxThreadsRationale(profile)
                : th.busyThreadFormula() + " · " + TomcatHikariSizingGuide.OPERATIONAL_NOTE;
        rows.add(row("Tomcat", "maxThreads", "server.tomcat.threads.max",
                th.tomcatMaxThreadsDisplay(), "server.xml · application.yml",
                "server.tomcat.threads.max=" + th.tomcatMaxThreadsMax(),
                String.valueOf(th.tomcatMaxThreadsMax()), config, maxThreadsNote));
        rows.add(row("Tomcat", "minSpareThreads", "server.tomcat.threads.min-spare",
                th.minSpareThreadsDisplay(), "server.xml · application.yml",
                "server.tomcat.threads.min-spare=" + th.minSpareThreadsMax(),
                String.valueOf(th.minSpareThreadsMax()), config, "피크 진입 시 준비 Thread"));
        rows.add(row("Tomcat", "acceptCount", "server.tomcat.accept-count",
                th.acceptCountDisplay(), "server.xml",
                "server.tomcat.accept-count=" + th.acceptCountMax(),
                String.valueOf(th.acceptCountMax()), config, "Thread 포화 시 대기 Queue"));
        rows.add(row("Tomcat", "maxConnections", "server.tomcat.max-connections",
                th.maxConnectionsDisplay(), "server.xml · application.yml",
                "server.tomcat.max-connections=" + th.maxConnectionsMax(),
                String.valueOf(th.maxConnectionsMax()), config,
                "OS FD·L4 Max Connection과 함께 검증"));
        rows.add(row("Tomcat", "connectionTimeout", "server.tomcat.connection-timeout",
                th.connectionTimeoutSec() + "초", "server.xml · application.yml",
                "connectionTimeout=" + (th.connectionTimeoutSec() * 1000),
                th.connectionTimeoutSec() + "s", config, "요청 수신 대기 · Client Timeout 정합"));
        rows.add(row("Tomcat", "keepAliveTimeout", "server.tomcat.keep-alive-timeout",
                th.keepAliveTimeoutSec() + "초", "server.xml · application.yml",
                "keepAliveTimeout=" + th.keepAliveTimeoutSec(),
                th.keepAliveTimeoutSec() + "s", config,
                "전용 단말 60초 · L4-WAS Idle 70~90초보다 짧게"));
        rows.add(row("Tomcat", "maxKeepAliveRequests", "server.tomcat.max-keep-alive-requests",
                String.valueOf(th.maxKeepAliveRequests()), "server.xml · application.yml",
                "maxKeepAliveRequests=" + th.maxKeepAliveRequests(),
                String.valueOf(th.maxKeepAliveRequests()), config, "장기 연결 점유 방지"));
        rows.add(row("Tomcat", "Thread Stack (Xss)", "server.tomcat.threads.max",
                th.threadStackXss(), "JVM 옵션 -Xss", "-Xss" + th.threadStackXss(),
                th.threadStackXss(), config, "Thread Stack 메모리 통제 · RSS=Heap+스택×스레드"));
        rows.add(row("Tomcat", "sessionTimeout", "server.servlet.session.timeout",
                sessionMinutes + "m", "application.yml", "server.servlet.session.timeout=" + sessionMinutes + "m",
                null, config, "세션 Idle"));

        String heapGeneral = JvmSizingGuide.heapGeneralRangeLabel(vmCores, vmMemoryGb);
        String heapSv = JvmSizingGuide.heapSingleViewLabel(vmCores, vmMemoryGb);
        var jvm = JvmSizingGuide.recommend(vmCores, vmMemoryGb, vm != null ? vm.getId() : null);
        rows.add(row("JVM", "Heap (일반 AP)", "jvm.heap.general",
                heapGeneral, "setenv.sh · systemd · K8s env", jvm.exampleOptsGeneral(),
                heapGeneral.split("~")[0].trim(), config,
                "Thread·Pool 상향 시 Xmx 재검토. RAM 대비 과대 Heap 금지"));
        rows.add(row("JVM", "Heap (SingleView)", "jvm.heap.singleview",
                heapSv, "setenv.sh (SV 전용)", jvm.exampleOptsSingleView(),
                "—", config, "대용량 조회·집계 AP — 일반 AP와 분리 기동"));
        rows.add(row("JVM", "GC", "jvm.gc",
                jvm.gcAlgorithm() + " · MaxGCPauseMillis " + jvm.maxGcPauseMillis(),
                "JVM 옵션", "-XX:+UseG1GC -XX:MaxGCPauseMillis=200",
                jvm.gcAlgorithm(), config, "STW·Full GC 빈도 모니터링"));
        rows.add(row("JVM", "Thread Stack", "jvm.thread.stack",
                jvm.threadStackSize(), "JVM 옵션", "-Xss512k",
                jvm.threadStackSize(), config, "maxThreads 상향 시 RSS = Heap + 스택×스레드"));
        rows.add(row("JVM", "Metaspace", "jvm.metaspace",
                jvm.metaspaceSizeMb() + "~" + jvm.maxMetaspaceSizeMb() + " MB",
                "JVM 옵션", "-XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m",
                jvm.metaspaceSizeMb() + "m", config, "동적 클래스·SV 모듈 증가 시 상한 조정"));
        rows.add(row("JVM", "HeapDump OOM", "jvm.heapdump",
                "On · /logs/dump", "JVM 옵션",
                "-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/logs/dump",
                "On", config, "OOM 시 MAT 분석 — 운영 디스크 여유 확보"));

        SpringBootSizingGuide.ProfileSpec spring = SpringBootSizingGuide.specFor(profile);
        String tomcatPerfNote = profile == VmProfile.CORE32_256
                ? "성능시험에서 최종 보정"
                : SpringBootSizingGuide.OPERATIONAL_NOTE;

        rows.add(row("Spring Boot", "server.tomcat.threads.max", "server.tomcat.threads.max",
                spring.tomcatMaxThreadsDisplay(), "application.yml",
                "server.tomcat.threads.max: " + th.tomcatMaxThreadsMax(),
                String.valueOf(th.tomcatMaxThreadsMax()), config, tomcatPerfNote));
        rows.add(row("Spring Boot", "server.tomcat.threads.min-spare", "server.tomcat.threads.min-spare",
                spring.minSpareThreadsRange(), "application.yml",
                "server.tomcat.threads.min-spare: " + th.minSpareThreadsMax(),
                String.valueOf(th.minSpareThreadsMax()), config, "피크 대비 준비 Thread"));
        rows.add(row("Spring Boot", "server.tomcat.accept-count", "server.tomcat.accept-count",
                spring.acceptCountRange(), "application.yml",
                "server.tomcat.accept-count: " + th.acceptCountMax(),
                String.valueOf(th.acceptCountMax()), config, "Thread 포화 시 대기 Queue"));
        rows.add(row("Spring Boot", "server.tomcat.max-connections", "server.tomcat.max-connections",
                spring.maxConnectionsRange(), "application.yml",
                "server.tomcat.max-connections: " + th.maxConnectionsMax(),
                String.valueOf(th.maxConnectionsMax()), config, "OS FD·L4와 함께 검증"));
        rows.add(row("Spring Boot", "server.tomcat.connection-timeout", "server.tomcat.connection-timeout",
                spring.connectionTimeoutSec() + "s", "application.yml",
                "server.tomcat.connection-timeout: " + spring.connectionTimeoutSec() + "s",
                spring.connectionTimeoutSec() + "s", config, "연결 후 요청 대기"));
        rows.add(row("Spring Boot", "server.tomcat.keep-alive-timeout", "server.tomcat.keep-alive-timeout",
                spring.keepAliveTimeoutSec() + "s", "application.yml",
                "server.tomcat.keep-alive-timeout: " + spring.keepAliveTimeoutSec() + "s",
                spring.keepAliveTimeoutSec() + "s", config,
                "L4 Idle 70~90초와 정합 (WAS " + spring.keepAliveTimeoutSec() + "s)"));
        rows.add(row("Spring Boot", "server.servlet.session.timeout", "server.servlet.session.timeout",
                sessionMinutes + "m", "application.yml",
                "server.servlet.session.timeout: " + sessionMinutes + "m",
                sessionMinutes + "m", config, "기존 세션 기준 유지"));
        rows.add(row("Spring Boot", "Async Executor Core Pool", spring.asyncCorePoolKey(),
                spring.asyncCorePoolRange(), "application.yml · AsyncConfig",
                spring.asyncCorePoolKey() + ": " + spring.asyncCoreMax(),
                String.valueOf(spring.asyncCoreMax()), config,
                "비동기 로그/감사/이벤트 · 업무 Thread와 분리"));
        rows.add(row("Spring Boot", "Async Executor Max Pool", spring.asyncMaxPoolKey(),
                spring.asyncMaxPoolRange(), "application.yml · AsyncConfig",
                spring.asyncMaxPoolKey() + ": " + spring.asyncMaxPoolMax(),
                String.valueOf(spring.asyncMaxPoolMax()), config, "무제한 증가 금지"));
        rows.add(row("Spring Boot", "Spring Transaction Timeout", "nsight.transaction.default-timeout-seconds",
                spring.transactionTimeoutRange(), "application.yml",
                "nsight.transaction.default-timeout-seconds: " + spring.transactionTimeoutSecMax(),
                String.valueOf(spring.transactionTimeoutSecMax()), config,
                "Core 증가와 무관 · 온라인 거래 짧게 유지"));
        rows.add(row("Spring Boot", "Hikari maximum-pool-size", "spring.datasource.hikari.maximum-pool-size",
                pool.rangeLabel() + " (일반 AP)", "application.yml",
                "spring.datasource.hikari.maximum-pool-size: " + pool.recommendedGeneral(),
                String.valueOf(pool.recommendedGeneral()), config,
                "Pool ≤ Tomcat maxThreads · SV " + th.hikariSingleViewRange() + " · " + th.cautionNote()));
        rows.add(row("Spring Boot", "Hikari connection-timeout", "spring.datasource.hikari.connection-timeout",
                timeoutSec + "s", "application.yml", "connection-timeout: " + (timeoutSec * 1000),
                "3000 ms", config, "DB Connection 획득"));
        rows.add(row("Spring Boot", "Session Cookie", "server.servlet.session.cookie.http-only",
                "HttpOnly·Secure", "application.yml", "http-only: true",
                "—", config, "쿠키 보안"));

        rows.add(row("MyBatis", "default-statement-timeout", "mybatis.default-statement-timeout",
                "2~" + timeoutSec + "s", "mybatis-config.xml", "defaultStatementTimeout: " + timeoutSec,
                "2 s", config, "SQL 수행 통제"));
        rows.add(row("MyBatis", "defaultFetchSize", "mybatis.default-fetch-size",
                "100~500", "mybatis-config.xml", "defaultFetchSize: 300",
                "300", config, "대량 조회 메모리"));
        return rows;
    }

    private static LayerGridRow row(
            String layer,
            String label,
            String key,
            String recommended,
            String location,
            String example,
            String defaultActual,
            Map<String, String> config,
            String actionGuide
    ) {
        String current = key.equals("—") ? "— (콘솔)" : resolve(config, key, defaultActual);
        String status = evaluate(current, recommended);
        return new LayerGridRow(
                layer, label, key, recommended, current, status, statusLabel(status),
                reason(status, label, current, recommended),
                location, example, actionGuide
        );
    }

    private static String resolve(Map<String, String> config, String key, String fallback) {
        if (config.containsKey(key)) {
            return config.get(key);
        }
        return fallback != null ? fallback : "—";
    }

    private static String evaluate(String current, String recommended) {
        if (current == null || current.isBlank() || current.contains("미설정") || "—".equals(current)) {
            return "CRITICAL";
        }
        if (recommended.contains("~")) {
            return "INFO";
        }
        return "NORMAL";
    }

    private static String statusLabel(String status) {
        return switch (status) {
            case "NORMAL" -> "정상";
            case "WARN" -> "경고";
            case "CRITICAL" -> "위험";
            case "EXCEPTION" -> "예외";
            default -> "참고";
        };
    }

    private static String reason(String status, String label, String current, String recommended) {
        if ("CRITICAL".equals(status)) {
            return label + " 미설정 또는 권장(" + recommended + ")과 불일치: " + current;
        }
        if ("WARN".equals(status)) {
            return label + " 개선 권고";
        }
        return "권장 범위 내";
    }
}
