package com.nh.nsight.marketing.oc.application.service.env;

import com.nh.nsight.marketing.oc.config.EnvCheckProperties;
import com.nh.nsight.marketing.oc.support.Nsight32Core256GbGuide;
import com.nh.nsight.marketing.oc.support.EnvSettingDefinition;
import com.nh.nsight.marketing.oc.support.IntegratedEnvironmentGuideCatalog;
import com.nh.nsight.marketing.oc.application.dto.env.AssessmentResultItem;
import com.nh.nsight.marketing.oc.application.dto.env.AssessmentStatus;
import com.nh.nsight.marketing.oc.application.dto.env.SettingMatchStatus;
import com.nh.nsight.marketing.oc.application.dto.env.ParsedConfigEntry;
import com.nh.nsight.marketing.oc.application.dto.env.TimeoutMapNode;
import com.nh.nsight.marketing.oc.application.dto.env.TimeoutMapView;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class EnvironmentRuleEngineService {

    private static final String TIMEOUT_REL_RULE = "TIMEOUT-REL-001";

    private final EnvCheckProperties properties;
    private final RuntimeConfigResolver runtimeConfigResolver;

    public EnvironmentRuleEngineService(EnvCheckProperties properties, RuntimeConfigResolver runtimeConfigResolver) {
        this.properties = properties;
        this.runtimeConfigResolver = runtimeConfigResolver;
    }

    public List<AssessmentResultItem> evaluateThresholdRules(
            Map<String, String> config,
            List<ParsedConfigEntry> importEntries
    ) {
        List<ParsedConfigEntry> entries = importEntries != null ? importEntries : List.of();
        List<AssessmentResultItem> results = new ArrayList<>();
        for (EnvSettingDefinition def : IntegratedEnvironmentGuideCatalog.definitions()) {
            if (def.matchType() == EnvSettingDefinition.MatchType.INFO_ONLY || def.propertyKey() == null) {
                continue;
            }
            String actual = findConfigValue(config, def.propertyKey());
            SettingMatchStatus match = def.evaluate(actual);
            AssessmentStatus status = toAssessmentStatus(match);
            String severity = status == AssessmentStatus.FAIL || status == AssessmentStatus.WARN ? "HIGH" : "LOW";
            if (def.key().contains("session") || def.key().contains("hikari")) {
                severity = status == AssessmentStatus.FAIL ? "CRITICAL" : severity;
            }
            String configFile = ConfigFileResolver.resolveFile(entries, def.propertyKey(), def.source());
            String ruleType = "L4".equals(def.categoryId()) || "GSLB".equals(def.categoryId())
                    ? "INFRA" : "THRESHOLD";
            results.add(new AssessmentResultItem(
                    def.key().toUpperCase().replace('.', '-'),
                    ruleType,
                    def.categoryId(),
                    severity,
                    def.label() + " 가이드 대비 점검",
                    def.guideValue(),
                    actual != null ? actual : "—",
                    status,
                    def.note() != null ? def.note() : "가이드 권장값에 맞게 조정하세요.",
                    def.source(),
                    configFile,
                    def.propertyKey()
            ));
        }
        return results;
    }

    public AssessmentResultItem evaluateTimeoutRelation(Map<String, Long> chain) {
        long db = chain.getOrDefault("dbQueryMs", 3000L);
        long hikari = chain.getOrDefault("hikariMs", 3000L);
        long tx = chain.getOrDefault("transactionMs", 5000L);
        long proxy = chain.getOrDefault("proxyMs", (long) properties.getProxyReadTimeoutMs());
        long client = chain.getOrDefault("clientMs", 15000L);
        long l4 = properties.getL4IdleTimeoutMs();

        boolean valid = db < hikari
                && hikari <= tx
                && tx < proxy
                && proxy < client
                && client < l4;

        String actual = String.format(
                "DB %dms < Hikari %dms <= Tx %dms < Proxy %dms < Client %dms < L4 %dms",
                db, hikari, tx, proxy, client, l4
        );
        return new AssessmentResultItem(
                TIMEOUT_REL_RULE,
                "RELATION",
                "TIMEOUT",
                valid ? "LOW" : "HIGH",
                "Timeout 계층 일관성 (DB → Hikari → Transaction → Proxy → Client → L4)",
                "DB < Hikari <= Transaction < Proxy < Client < L4 Idle",
                actual,
                valid ? AssessmentStatus.PASS : AssessmentStatus.FAIL,
                "Timeout 역전 시 장시간 대기·좀비 트랜잭션 위험. 가이드 계층 순서를 유지하세요.",
                "Rule Engine",
                "application.yml, mybatis-config.xml",
                "mybatis.default-statement-timeout, spring.datasource.hikari.connection-timeout, "
                        + "nsight.transaction.default-timeout-seconds, nsight.webtop.read-timeout-ms, "
                        + "nsight.webtop.request-timeout-ms, nsight.env-check.l4-idle-timeout-ms"
        );
    }

    public TimeoutMapView buildTimeoutMap(
            String runId,
            Map<String, Long> chain,
            boolean chainValid,
            Map<String, String> config,
            List<ParsedConfigEntry> importEntries
    ) {
        List<ParsedConfigEntry> entries = importEntries != null ? importEntries : List.of();
        List<TimeoutMapNode> nodes = new ArrayList<>();
        for (TimeoutChainSpec spec : TIMEOUT_CHAIN_SPECS) {
            long ms = spec.chainKey() != null
                    ? chain.getOrDefault(spec.chainKey(), spec.defaultMs())
                    : properties.getL4IdleTimeoutMs();
            String configValue = findConfigValue(config, spec.propertyKey());
            if (configValue == null && spec.chainKey() != null) {
                configValue = formatMs(ms);
            }
            if (configValue == null && spec.chainKey() == null) {
                configValue = properties.getL4IdleTimeoutMs() + " ms";
            }
            nodes.add(node(
                    spec.order(),
                    spec.layer(),
                    spec.label(),
                    ms,
                    chainValid,
                    resolveSourceFile(entries, spec.propertyKey(), spec.defaultSourceFile()),
                    spec.propertyKey(),
                    configValue,
                    spec.guideValue(),
                    spec.note()
            ));
        }
        return new TimeoutMapView(
                runId,
                TIMEOUT_REL_RULE,
                chainValid,
                chainValid ? "Timeout 계층 정상" : "Timeout 역전 감지 — FAIL",
                nodes
        );
    }

    private static final List<TimeoutChainSpec> TIMEOUT_CHAIN_SPECS = List.of(
            new TimeoutChainSpec(1, "dbQueryMs", 2000L, "DB/SQL", "DB Query Timeout",
                    "mybatis.default-statement-timeout", "mybatis-config.xml",
                    Nsight32Core256GbGuide.MYBATIS_STATEMENT_TIMEOUT_SEC + "~3 s", null),
            new TimeoutChainSpec(2, "hikariMs", Nsight32Core256GbGuide.HIKARI_CONNECTION_TIMEOUT_MS, "HikariCP",
                    "Pool connectionTimeout",
                    "spring.datasource.hikari.connection-timeout", "application.yml",
                    Nsight32Core256GbGuide.HIKARI_CONNECTION_TIMEOUT_MS + " ms", null),
            new TimeoutChainSpec(3, "transactionMs", 5000L, "Spring", "Transaction Timeout",
                    "nsight.transaction.default-timeout-seconds", "application.yml",
                    Nsight32Core256GbGuide.TRANSACTION_TIMEOUT_SEC_MIN + "~"
                            + Nsight32Core256GbGuide.TRANSACTION_TIMEOUT_SEC_MAX + " s", null),
            new TimeoutChainSpec(4, "proxyMs", Nsight32Core256GbGuide.PROXY_READ_TIMEOUT_MS, "Proxy",
                    "Proxy Read Timeout",
                    "nsight.webtop.read-timeout-ms", "application.yml · nginx.conf",
                    Nsight32Core256GbGuide.PROXY_READ_TIMEOUT_MS + " ms", null),
            new TimeoutChainSpec(5, "clientMs", Nsight32Core256GbGuide.WEBTOP_REQUEST_TIMEOUT_MS, "WebTopSuite",
                    "Request Timeout",
                    "nsight.webtop.request-timeout-ms", "application.yml",
                    Nsight32Core256GbGuide.WEBTOP_REQUEST_TIMEOUT_MS + " ms", null),
            new TimeoutChainSpec(6, null, Nsight32Core256GbGuide.L4_CLIENT_IDLE_TIMEOUT_SEC * 1000L, "L4", "Idle Timeout (참고)",
                    "nsight.env-check.l4-idle-timeout-ms", "application.yml · L4 Console",
                    "80 s", "Client-L4 70~90s · Sticky 70~80m · Session 60m")
    );

    private record TimeoutChainSpec(
            int order,
            String chainKey,
            long defaultMs,
            String layer,
            String label,
            String propertyKey,
            String defaultSourceFile,
            String guideValue,
            String note
    ) {
    }

    private TimeoutMapNode node(
            int order,
            String layer,
            String label,
            long ms,
            boolean chainOk,
            String sourceFile,
            String propertyKey,
            String configValue,
            String guideValue,
            String note
    ) {
        return new TimeoutMapNode(
                order,
                layer,
                label,
                ms,
                formatMs(ms),
                chainOk ? AssessmentStatus.PASS : AssessmentStatus.FAIL,
                note,
                sourceFile,
                propertyKey,
                configValue != null ? configValue : "—",
                guideValue
        );
    }

    private static String formatMs(long ms) {
        return ms >= 1000 ? (ms / 1000) + " s" : ms + " ms";
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
        String normalized = propertyKey.toLowerCase();
        for (Map.Entry<String, String> e : config.entrySet()) {
            if (e.getKey().toLowerCase().contains(normalized)
                    || normalized.contains(e.getKey().toLowerCase())) {
                return e.getValue();
            }
        }
        return runtimeConfigResolver.resolveAll().get(propertyKey);
    }

    private AssessmentStatus toAssessmentStatus(SettingMatchStatus status) {
        return switch (status) {
            case MATCH -> AssessmentStatus.PASS;
            case WARN -> AssessmentStatus.WARN;
            case INFO -> AssessmentStatus.INFO;
            default -> AssessmentStatus.FAIL;
        };
    }
}
