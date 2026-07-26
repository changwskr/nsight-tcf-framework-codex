package com.nh.nsight.marketing.oc.application.service.env;

import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import com.nh.nsight.marketing.oc.support.SecureXmlDocuments;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RuntimeConfigResolver {

    private final Environment environment;

    public RuntimeConfigResolver(Environment environment) {
        this.environment = environment;
    }

    public Map<String, String> resolveAll() {
        Map<String, String> map = new LinkedHashMap<>();
        putIfPresent(map, "spring.application.name");
        putIfPresent(map, "nsight.ap-id");
        putIfPresent(map, "nsight.webtop.request-timeout-ms");
        putIfPresent(map, "nsight.webtop.connect-timeout-ms");
        putIfPresent(map, "nsight.webtop.read-timeout-ms");
        putIfPresent(map, "nsight.webtop.retry-count");
        putIfPresent(map, "nsight.transaction.default-timeout-seconds");
        putIfPresent(map, "nsight.transaction.readonly-timeout-seconds");
        putIfPresent(map, "nsight.absolute-session-timeout-hours");
        putIfPresent(map, "nsight.cruzapim.connect-timeout-ms");
        putIfPresent(map, "nsight.cruzapim.read-timeout-ms");
        putIfPresent(map, "nsight.env-check.l4-idle-timeout-ms");
        putIfPresent(map, "nsight.env-check.l4-was-idle-timeout-sec");
        putIfPresent(map, "nsight.env-check.l4-health-interval-sec");
        putIfPresent(map, "nsight.env-check.l4-health-timeout-sec");
        putIfPresent(map, "nsight.env-check.l4-health-fail-count");
        putIfPresent(map, "nsight.env-check.l4-sticky-timeout-sec");
        putIfPresent(map, "nsight.env-check.gslb-health-interval-sec");
        putIfPresent(map, "nsight.env-check.gslb-health-timeout-sec");
        putIfPresent(map, "nsight.env-check.gslb-health-fail-count");
        putIfPresent(map, "nsight.env-check.gslb-sticky-timeout-sec");
        putIfPresent(map, "nsight.env-check.proxy-read-timeout-ms");
        putIfPresent(map, "nsight.env-check.peak-tps");
        putIfPresent(map, "nsight.env-check.base-tps");
        putIfPresent(map, "nsight.env-check.high-peak-tps");
        putIfPresent(map, "nsight.env-check.stress-tps");
        putIfPresent(map, "nsight.env-check.vm-max-tps");
        putIfPresent(map, "nsight.env-check.session-design-count");
        putIfPresent(map, "nsight.env-check.total-users");
        putIfPresent(map, "nsight.env-check.actual-request-users");
        putIfPresent(map, "nsight.env-check.peak-concurrent-users");
        putIfPresent(map, "nsight.env-check.ap-count");
        putIfPresent(map, "nsight.env-check.target-p95-ms");
        putIfPresent(map, "nsight.env-check.db-session-limit");
        putIfPresent(map, "server.servlet.session.timeout");
        putIfPresent(map, "server.tomcat.threads.max");
        putIfPresent(map, "server.tomcat.threads.min-spare");
        putIfPresent(map, "server.tomcat.accept-count");
        putIfPresent(map, "server.tomcat.max-connections");
        putIfPresent(map, "server.tomcat.connection-timeout");
        putIfPresent(map, "server.tomcat.keep-alive-timeout");
        putIfPresent(map, "nsight.async.audit-log.core-pool-size");
        putIfPresent(map, "nsight.async.audit-log.max-pool-size");
        putIfPresent(map, "spring.datasource.hikari.maximum-pool-size");
        putIfPresent(map, "spring.datasource.hikari.connection-timeout");
        map.putAll(readMyBatisSettings());
        return map;
    }

    public Map<String, Long> resolveTimeoutChainMs() {
        Map<String, String> raw = resolveAll();
        Map<String, Long> chain = new LinkedHashMap<>();
        chain.put("dbQueryMs", parseMs(raw.get("mybatis.default-statement-timeout"), 3000L));
        chain.put("hikariMs", parseMs(raw.get("spring.datasource.hikari.connection-timeout"), 3000L));
        chain.put("transactionMs", parseSecondsToMs(raw.get("nsight.transaction.default-timeout-seconds"), 5000L));
        chain.put("proxyMs", parseMs(raw.get("nsight.webtop.read-timeout-ms"), 10000L));
        chain.put("clientMs", parseMs(raw.get("nsight.webtop.request-timeout-ms"), 15000L));
        return chain;
    }

    private void putIfPresent(Map<String, String> map, String key) {
        String value = environment.getProperty(key);
        if (value != null) {
            map.put(key, format(key, value));
        }
    }

    private String format(String key, String value) {
        if ("nsight.absolute-session-timeout-hours".equals(key)) {
            return value + "h";
        }
        if (key.contains("hikari") && key.contains("timeout")) {
            return value + " ms";
        }
        if (key.contains("timeout-ms")) {
            return value + " ms";
        }
        if (key.contains("timeout-seconds")) {
            return value + " s";
        }
        return value;
    }

    private Map<String, String> readMyBatisSettings() {
        Map<String, String> map = new LinkedHashMap<>();
        try (InputStream in = new ClassPathResource("mybatis/mybatis-config.xml").getInputStream()) {
            Document doc = SecureXmlDocuments.parse(in);
            NodeList settings = doc.getElementsByTagName("setting");
            for (int i = 0; i < settings.getLength(); i++) {
                var node = settings.item(i);
                String name = node.getAttributes().getNamedItem("name").getTextContent();
                String val = node.getAttributes().getNamedItem("value").getTextContent();
                if ("defaultStatementTimeout".equals(name)) {
                    map.put("mybatis.default-statement-timeout", val + " s");
                }
                if ("defaultFetchSize".equals(name)) {
                    map.put("mybatis.default-fetch-size", val);
                }
            }
        } catch (Exception ignored) {
            map.put("mybatis.default-statement-timeout", "2 s");
            map.put("mybatis.default-fetch-size", "300");
        }
        return map;
    }

    public static long parseMs(String raw, long defaultMs) {
        if (raw == null || raw.isBlank()) {
            return defaultMs;
        }
        String s = raw.trim().toLowerCase();
        try {
            if (s.endsWith("ms")) {
                return Long.parseLong(s.replace("ms", "").trim());
            }
            if (s.endsWith(" s") || s.endsWith("s")) {
                return Long.parseLong(s.replace("s", "").trim()) * 1000L;
            }
            if (s.endsWith("m") || s.endsWith("min")) {
                return Long.parseLong(s.replace("min", "").replace("m", "").trim()) * 60_000L;
            }
            return Long.parseLong(s.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return defaultMs;
        }
    }

    public static long parseSecondsToMs(String raw, long defaultMs) {
        if (raw == null || raw.isBlank()) {
            return defaultMs;
        }
        String s = raw.replace(" s", "").replace("s", "").trim();
        try {
            return (long) (Double.parseDouble(s) * 1000);
        } catch (NumberFormatException e) {
            return defaultMs;
        }
    }
}
