package com.nh.nsight.marketing.oc.application.service.env;

import com.nh.nsight.marketing.oc.config.EnvCheckProperties;
import com.nh.nsight.marketing.oc.support.Nsight32Core256GbGuide;
import com.nh.nsight.marketing.oc.support.NsightCapacityDerivation;
import com.nh.nsight.marketing.oc.support.NsightCapacityDerivation.CapacityTargets;
import com.nh.nsight.marketing.oc.application.dto.env.AssessmentResultItem;
import com.nh.nsight.marketing.oc.application.dto.env.AssessmentStatus;
import com.nh.nsight.marketing.oc.application.dto.env.ConcurrentFlowMapNode;
import com.nh.nsight.marketing.oc.application.dto.env.ConcurrentFlowMapView;
import com.nh.nsight.marketing.oc.application.dto.env.ParsedConfigEntry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 실시간 동시 요청자(용량) 계층 — 실요청 사용자(5%·1,800명) 기준 AP 분산 → Tomcat → Hikari → DB Session.
 */
@Service
public class ConcurrentFlowMapService {

    public static final String CONCURRENT_REL_RULE = "CONCURRENT-REL-001";
    public static final String CAPACITY_TPS_REL_RULE = "CAPACITY-REL-002";

    private final EnvCheckProperties properties;
    private final RuntimeConfigResolver runtimeConfigResolver;

    public ConcurrentFlowMapService(EnvCheckProperties properties, RuntimeConfigResolver runtimeConfigResolver) {
        this.properties = properties;
        this.runtimeConfigResolver = runtimeConfigResolver;
    }

    public CapacityTargets resolveTargets(Map<String, String> config) {
        int totalUsers = parseIntConfig(config, "nsight.env-check.total-users", properties.getTotalUsers());
        int actualRequestUsers = parseIntConfig(config, "nsight.env-check.actual-request-users",
                properties.getActualRequestUsers());
        int peakPercent = parseIntConfig(config, "nsight.env-check.actual-request-peak-percent",
                properties.getActualRequestPeakPercent());
        int peakTps = parseIntConfig(config, "nsight.env-check.peak-tps", properties.getPeakTps());
        int p95Ms = parseIntConfig(config, "nsight.env-check.target-p95-ms", properties.getTargetP95Ms());
        int apCount = parseIntConfig(config, "nsight.env-check.ap-count", properties.getApCount());
        return NsightCapacityDerivation.resolve(totalUsers, actualRequestUsers, peakPercent, peakTps, p95Ms, apCount);
    }

    public Map<String, Long> buildCapacityChain(Map<String, String> config) {
        CapacityTargets targets = resolveTargets(config);

        Map<String, Long> chain = new LinkedHashMap<>();
        chain.put("actualRequestTotal", (long) targets.concurrentTotal());
        chain.put("targetConcurrent", (long) targets.concurrentPerAp());
        chain.put("peakTpsDerived", (long) targets.peakTpsFromActualRequest());
        chain.put("peakTpsConfigured", (long) targets.configuredPeakTps());
        chain.put("maxConnections", parseLongConfig(config, "server.tomcat.max-connections",
                (long) properties.getTomcatMaxConnections()));
        chain.put("tomcatThreads", parseLongConfig(config, "server.tomcat.threads.max",
                (long) properties.getTomcatMaxThreads()));
        chain.put("acceptQueue", parseLongConfig(config, "server.tomcat.accept-count",
                (long) properties.getTomcatAcceptCount()));
        chain.put("hikariPool", parseLongConfig(config, "spring.datasource.hikari.maximum-pool-size",
                (long) properties.getHikariPoolGeneral()));
        chain.put("dbSessionLimit", (long) parseIntConfig(config, "nsight.env-check.db-session-limit",
                properties.getDbSessionLimit()));
        return chain;
    }

    public AssessmentResultItem evaluateConcurrentRelation(Map<String, Long> chain) {
        long targetPerAp = chain.getOrDefault("targetConcurrent", 0L);
        long actualTotal = chain.getOrDefault("actualRequestTotal", 0L);
        long maxConn = chain.getOrDefault("maxConnections", 10000L);
        long tomcat = chain.getOrDefault("tomcatThreads", 500L);
        long accept = chain.getOrDefault("acceptQueue", 500L);
        long hikari = chain.getOrDefault("hikariPool", 50L);
        long dbSession = chain.getOrDefault("dbSessionLimit", 200L);

        boolean valid = targetPerAp <= tomcat
                && hikari <= tomcat
                && hikari <= dbSession
                && tomcat <= maxConn
                && (tomcat + accept) >= targetPerAp;

        String actual = String.format(
                "실요청 %d명(전사) → AP당 %d ≤ Tomcat %d, Hikari %d ≤ Tomcat, Hikari %d ≤ DB %d, "
                        + "Tomcat %d ≤ MaxConn %d, Tomcat+Accept %d ≥ AP당목표",
                actualTotal, targetPerAp, tomcat, hikari, hikari, dbSession, tomcat, maxConn, tomcat + accept
        );
        return new AssessmentResultItem(
                CONCURRENT_REL_RULE,
                "RELATION",
                "CAPACITY",
                valid ? "LOW" : "HIGH",
                "실요청 사용자 기반 동시 처리 계층 (전사 실요청 → AP당 → Tomcat → Pool → DB)",
                "AP당목표(실요청÷AP) ≤ maxThreads, pool ≤ threads ≤ maxConnections, pool ≤ DB Session",
                actual,
                valid ? AssessmentStatus.PASS : AssessmentStatus.FAIL,
                "실요청 1,800명(5%)은 AP 2대 기준 AP당 약 900 동시 처리 목표입니다. Tomcat만 올리고 실요청·TPS 불일치 시 병목이 남습니다.",
                "Rule Engine",
                "application.yml",
                "nsight.env-check.actual-request-users, nsight.env-check.ap-count, "
                        + "server.tomcat.threads.max, spring.datasource.hikari.maximum-pool-size"
        );
    }

    public AssessmentResultItem evaluatePeakTpsVsActualRequest(Map<String, String> config) {
        CapacityTargets targets = resolveTargets(config);
        int expectedFromPercent = NsightCapacityDerivation.expectedActualRequestFromPercent(
                parseIntConfig(config, "nsight.env-check.total-users", properties.getTotalUsers()),
                targets.actualRequestPeakPercent());
        boolean usersMatchPercent = targets.actualRequestUsers() == expectedFromPercent;
        boolean tpsMatch = targets.peakTpsMatchesActualRequest();

        boolean valid = usersMatchPercent && tpsMatch;
        String actual = String.format(
                "전체 %d명 × %d%% = %d명(기대) ↔ 실요청 설정 %d명 · TPS %d = 실요청 %d ÷ p95 %ds(기대 %d)",
                parseIntConfig(config, "nsight.env-check.total-users", properties.getTotalUsers()),
                targets.actualRequestPeakPercent(),
                expectedFromPercent,
                targets.actualRequestUsers(),
                targets.configuredPeakTps(),
                targets.actualRequestUsers(),
                targets.p95Ms() / 1000,
                targets.peakTpsFromActualRequest()
        );
        return new AssessmentResultItem(
                CAPACITY_TPS_REL_RULE,
                "RELATION",
                "CAPACITY",
                valid ? "LOW" : "HIGH",
                "실요청 사용자·운영 피크 TPS 정합 (5% → 1,800명 → 600 TPS)",
                "실요청 = 전체×5%, peak-tps = 실요청÷p95(3초). 스트레스 TPS 1,800과 혼동 금지",
                actual,
                valid ? AssessmentStatus.PASS : AssessmentStatus.FAIL,
                "실요청 1,800명과 peak-tps 600은 쌍으로 맞춥니다. 1,800 TPS는 15% 스트레스 시나리오입니다.",
                "Rule Engine",
                "application.yml",
                "nsight.env-check.actual-request-users, nsight.env-check.peak-tps, "
                        + "nsight.env-check.total-users, nsight.env-check.actual-request-peak-percent"
        );
    }

    public ConcurrentFlowMapView buildConcurrentFlowMap(
            String runId,
            Map<String, Long> chain,
            boolean chainValid,
            Map<String, String> config,
            List<ParsedConfigEntry> importEntries
    ) {
        List<ParsedConfigEntry> entries = importEntries != null ? importEntries : List.of();
        CapacityTargets targets = resolveTargets(config);
        int perAp = chain.getOrDefault("targetConcurrent", (long) targets.concurrentPerAp()).intValue();
        int actualTotal = chain.getOrDefault("actualRequestTotal", (long) targets.concurrentTotal()).intValue();

        List<ConcurrentFlowMapNode> nodes = new ArrayList<>();
        for (ConcurrentFlowSpec spec : CONCURRENT_FLOW_SPECS) {
            long value = chainValue(chain, spec, targets);
            String configValue = resolveConfigDisplay(config, spec, targets, perAp, actualTotal);
            nodes.add(flowNode(
                    spec.order(),
                    spec.layer(),
                    spec.label(),
                    value,
                    chainValid,
                    resolveSourceFile(entries, spec.propertyKey(), spec.defaultSourceFile()),
                    spec.propertyKey(),
                    configValue,
                    spec.guideValue(),
                    spec.note()
            ));
        }
        return new ConcurrentFlowMapView(
                runId,
                CONCURRENT_REL_RULE,
                chainValid,
                chainValid ? "동시 요청 계층 정상" : "동시 요청 병목·역전 감지 — FAIL",
                actualTotal,
                perAp,
                targets.peakTpsFromActualRequest(),
                targets.configuredPeakTps(),
                nodes
        );
    }

    private long chainValue(Map<String, Long> chain, ConcurrentFlowSpec spec, CapacityTargets targets) {
        if ("actualRequestTotal".equals(spec.chainKey())) {
            return targets.concurrentTotal();
        }
        if ("peakTpsDerived".equals(spec.chainKey())) {
            return targets.peakTpsFromActualRequest();
        }
        return chain.getOrDefault(spec.chainKey(), spec.defaultValue());
    }

    private String resolveConfigDisplay(
            Map<String, String> config,
            ConcurrentFlowSpec spec,
            CapacityTargets targets,
            int perAp,
            int actualTotal
    ) {
        return switch (spec.chainKey()) {
            case "actualRequestTotal" -> String.format(
                    "전체 %s명 × %d%% → 실요청 %s명 (동시)",
                    findOr(config, "nsight.env-check.total-users", properties.getTotalUsers()),
                    targets.actualRequestPeakPercent(),
                    findOr(config, "nsight.env-check.actual-request-users", actualTotal)
            );
            case "targetConcurrent" -> String.format(
                    "실요청 %d명 ÷ AP %d대 → AP당 %d 동시",
                    actualTotal, targets.apCount(), perAp
            );
            case "peakTpsDerived" -> String.format(
                    "실요청 %d ÷ p95 %ds → TPS %d (설정 peak-tps %d)",
                    actualTotal, targets.p95Ms() / 1000,
                    targets.peakTpsFromActualRequest(), targets.configuredPeakTps()
            );
            default -> {
                String v = findConfigValue(config, spec.propertyKey());
                yield v != null ? v : String.valueOf(spec.defaultValue());
            }
        };
    }

    private String findOr(Map<String, String> config, String key, int fallback) {
        String v = findConfigValue(config, key);
        return v != null ? v : String.valueOf(fallback);
    }

    private static final List<ConcurrentFlowSpec> CONCURRENT_FLOW_SPECS = List.of(
            new ConcurrentFlowSpec(1, "actualRequestTotal", 0L, "용량 기준", "실요청 사용자 (전사·5%)",
                    "nsight.env-check.actual-request-users", "application.yml",
                    Nsight32Core256GbGuide.ACTUAL_REQUEST_USERS_PEAK + "명 (전체 "
                            + Nsight32Core256GbGuide.TOTAL_USERS + " × "
                            + Nsight32Core256GbGuide.ACTUAL_REQUEST_PERCENT_PEAK + "%)",
                    "세션 36,000과 별도 · 동시 실요청 규모"),
            new ConcurrentFlowSpec(2, "targetConcurrent", 0L, "용량 기준", "AP당 동시 처리 목표",
                    "nsight.env-check.ap-count", "application.yml",
                    "실요청 ÷ AP", "Tomcat maxThreads와 비교"),
            new ConcurrentFlowSpec(3, "peakTpsDerived", 0L, "용량 기준", "운영 피크 TPS (전사)",
                    "nsight.env-check.peak-tps", "application.yml",
                    String.valueOf(Nsight32Core256GbGuide.PEAK_TPS),
                    "≠ 스트레스 TPS " + Nsight32Core256GbGuide.STRESS_TPS),
            new ConcurrentFlowSpec(4, "maxConnections", Nsight32Core256GbGuide.TOMCAT_MAX_CONNECTIONS, "Tomcat",
                    "maxConnections (인입)",
                    "server.tomcat.max-connections", "application.yml · server.xml", "20000~30000", null),
            new ConcurrentFlowSpec(5, "tomcatThreads", Nsight32Core256GbGuide.TOMCAT_MAX_THREADS, "Tomcat",
                    "maxThreads (동시 처리)",
                    "server.tomcat.threads.max", "application.yml · server.xml", "1200~1500",
                    "AP당 목표 ≤ maxThreads"),
            new ConcurrentFlowSpec(6, "acceptQueue", Nsight32Core256GbGuide.TOMCAT_ACCEPT_COUNT, "Tomcat",
                    "acceptCount (대기 큐)",
                    "server.tomcat.accept-count", "application.yml · server.xml", "800~1000", null),
            new ConcurrentFlowSpec(7, "hikariPool", Nsight32Core256GbGuide.HIKARI_POOL_GENERAL, "HikariCP",
                    "maximumPoolSize (일반 AP)",
                    "spring.datasource.hikari.maximum-pool-size", "application.yml",
                    String.valueOf(Nsight32Core256GbGuide.HIKARI_POOL_GENERAL), "AP Pool ≤ DB Session"),
            new ConcurrentFlowSpec(8, "dbSessionLimit", Nsight32Core256GbGuide.DB_SESSION_LIMIT_REF, "DB",
                    "DB Session 한도 (참고)",
                    "nsight.env-check.db-session-limit", "application.yml · DBA", "Pool 합산 이하", null)
    );

    private record ConcurrentFlowSpec(
            int order,
            String chainKey,
            long defaultValue,
            String layer,
            String label,
            String propertyKey,
            String defaultSourceFile,
            String guideValue,
            String note
    ) {
    }

    private ConcurrentFlowMapNode flowNode(
            int order,
            String layer,
            String label,
            long value,
            boolean chainOk,
            String sourceFile,
            String propertyKey,
            String configValue,
            String guideValue,
            String note
    ) {
        return new ConcurrentFlowMapNode(
                order,
                layer,
                label,
                value,
                formatCapacity(value, order),
                chainOk ? AssessmentStatus.PASS : AssessmentStatus.FAIL,
                note,
                sourceFile,
                propertyKey,
                configValue != null ? configValue : "—",
                guideValue
        );
    }

    private static String formatCapacity(long value, int order) {
        return switch (order) {
            case 1 -> String.format("%,d명 (전사)", value);
            case 2 -> String.format("%,d 동시/AP", value);
            case 3 -> String.format("%,d TPS", value);
            default -> value >= 1000 ? String.format("%,d", value) : String.valueOf(value);
        };
    }

    private String resolveSourceFile(List<ParsedConfigEntry> entries, String propertyKey, String defaultFile) {
        String norm = propertyKey.toLowerCase(Locale.ROOT);
        for (ParsedConfigEntry entry : entries) {
            if (norm.equals(entry.normalizedKey())
                    || norm.equals(entry.configKey())
                    || entry.configKey().equals(propertyKey)) {
                return entry.fileName();
            }
        }
        return defaultFile;
    }

    private String findConfigValue(Map<String, String> config, String propertyKey) {
        if (config.containsKey(propertyKey)) {
            return config.get(propertyKey);
        }
        String normalized = propertyKey.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> e : config.entrySet()) {
            if (e.getKey().equalsIgnoreCase(propertyKey)
                    || e.getKey().toLowerCase(Locale.ROOT).endsWith(normalized.replace("nsight.env-check.", ""))) {
                return e.getValue();
            }
        }
        return runtimeConfigResolver.resolveAll().get(propertyKey);
    }

    private int parseIntConfig(Map<String, String> config, String key, int defaultValue) {
        String raw = findConfigValue(config, key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private long parseLongConfig(Map<String, String> config, String key, long defaultValue) {
        String raw = findConfigValue(config, key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(raw.replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
