package com.nh.nsight.marketing.oc.application.service.env;

import com.nh.nsight.marketing.oc.application.dto.env.ParsedConfigEntry;
import com.nh.nsight.marketing.oc.support.SecureXmlDocuments;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

@Service
public class ConfigParserService {

    private static final List<String> ALLOWED_EXT = List.of(
            ".yml", ".yaml", ".properties", ".xml", ".conf", ".sh", ".json"
    );

    public List<ParsedConfigEntry> parse(String fileName, byte[] content) throws IOException {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (!isAllowed(lower)) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식: " + fileName);
        }
        String text = new String(content, StandardCharsets.UTF_8);
        if (lower.endsWith(".yml") || lower.endsWith(".yaml")) {
            return parseYaml(fileName, text);
        }
        if (lower.endsWith(".properties")) {
            return parseProperties(fileName, text);
        }
        if (lower.endsWith(".xml")) {
            if (isTomcatServerXml(fileName, text)) {
                return parseTomcatServerXml(fileName, text);
            }
            if (isMyBatisConfig(fileName, text)) {
                return parseMyBatisSettingsXml(fileName, text);
            }
            if (text.contains("<setting")) {
                return parseSettingsXml(fileName, text);
            }
        }
        return parseFlatLines(fileName, text);
    }

    private boolean isTomcatServerXml(String fileName, String text) {
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        return lowerName.contains("server") || text.contains("<Connector");
    }

    private boolean isMyBatisConfig(String fileName, String text) {
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        return lowerName.contains("mybatis")
                || text.contains("mybatis.org")
                || text.contains("defaultStatementTimeout");
    }

    private boolean isAllowed(String lower) {
        return ALLOWED_EXT.stream().anyMatch(lower::endsWith);
    }

    /**
     * Spring Boot application.yml 은 {@code ---} 로 프로파일 문서가 구분되는 multi-document YAML 입니다.
     */
    private List<ParsedConfigEntry> parseYaml(String fileName, String text) {
        LoaderOptions options = new LoaderOptions();
        options.setMaxAliasesForCollections(50);
        Yaml yaml = new Yaml(new Constructor(options));
        Map<String, Object> merged = new LinkedHashMap<>();
        for (Object loaded : yaml.loadAll(text)) {
            if (loaded instanceof Map<?, ?> root) {
                deepMerge(merged, castMap(root));
            }
        }
        List<ParsedConfigEntry> entries = new ArrayList<>();
        if (!merged.isEmpty()) {
            flattenMap(fileName, "", merged, entries, 1);
        }
        return entries;
    }

    private void deepMerge(Map<String, Object> target, Map<String, Object> source) {
        for (Map.Entry<String, Object> e : source.entrySet()) {
            String key = e.getKey();
            Object val = e.getValue();
            if (val instanceof Map<?, ?> nested) {
                Object existing = target.get(key);
                Map<String, Object> targetChild = existing instanceof Map<?, ?> existingMap
                        ? castMap(existingMap)
                        : new LinkedHashMap<>();
                deepMerge(targetChild, castMap(nested));
                target.put(key, targetChild);
            } else {
                target.put(key, val);
            }
        }
    }

    private void flattenMap(String fileName, String prefix, Map<String, Object> map,
                            List<ParsedConfigEntry> out, int lineHint) {
        for (Map.Entry<String, Object> e : map.entrySet()) {
            String key = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
            Object val = e.getValue();
            if (val instanceof Map<?, ?> nested) {
                flattenMap(fileName, key, castMap(nested), out, lineHint);
            } else if (val != null) {
                out.add(new ParsedConfigEntry(
                        fileName, key, String.valueOf(val), normalizeKey(key), lineHint
                ));
            }
        }
    }

    private Map<String, Object> castMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((k, v) -> result.put(String.valueOf(k), v));
        return result;
    }

    private List<ParsedConfigEntry> parseProperties(String fileName, String text) throws IOException {
        Properties props = new Properties();
        props.load(new java.io.StringReader(text));
        List<ParsedConfigEntry> entries = new ArrayList<>();
        int line = 1;
        for (String name : props.stringPropertyNames()) {
            entries.add(new ParsedConfigEntry(
                    fileName, name, props.getProperty(name), normalizeKey(name), line++
            ));
        }
        return entries;
    }

    private List<ParsedConfigEntry> parseTomcatServerXml(String fileName, String text) {
        List<ParsedConfigEntry> entries = new ArrayList<>();
        try {
            Document doc = SecureXmlDocuments.parseUtf8(text);
            NodeList connectors = doc.getElementsByTagName("Connector");
            for (int i = 0; i < connectors.getLength(); i++) {
                if (!(connectors.item(i) instanceof Element connector)) {
                    continue;
                }
                addTomcatAttr(entries, fileName, connector, "maxThreads", "server.tomcat.threads.max");
                addTomcatAttr(entries, fileName, connector, "minSpareThreads", "server.tomcat.threads.min-spare");
                addTomcatAttr(entries, fileName, connector, "acceptCount", "server.tomcat.accept-count");
                addTomcatAttr(entries, fileName, connector, "maxConnections", "server.tomcat.max-connections");
                addTomcatAttr(entries, fileName, connector, "connectionTimeout", "server.tomcat.connection-timeout");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("server.xml 파싱 실패: " + e.getMessage());
        }
        if (entries.isEmpty()) {
            return parseFlatLines(fileName, text);
        }
        return entries;
    }

    private List<ParsedConfigEntry> parseMyBatisSettingsXml(String fileName, String text) {
        List<ParsedConfigEntry> entries = parseSettingsXml(fileName, text);
        if (entries.isEmpty()) {
            return parseFlatLines(fileName, text);
        }
        return entries;
    }

    private List<ParsedConfigEntry> parseSettingsXml(String fileName, String text) {
        List<ParsedConfigEntry> entries = new ArrayList<>();
        try {
            Document doc = SecureXmlDocuments.parseUtf8(text);
            NodeList settings = doc.getElementsByTagName("setting");
            for (int i = 0; i < settings.getLength(); i++) {
                Node node = settings.item(i);
                if (!(node instanceof Element setting) || setting.getAttributes() == null) {
                    continue;
                }
                Node nameAttr = setting.getAttributes().getNamedItem("name");
                Node valueAttr = setting.getAttributes().getNamedItem("value");
                if (nameAttr == null || valueAttr == null) {
                    continue;
                }
                String name = nameAttr.getTextContent();
                String value = valueAttr.getTextContent();
                String configKey = myBatisSettingKey(name);
                entries.add(new ParsedConfigEntry(
                        fileName, configKey, value, normalizeKey(configKey), i + 1
                ));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("XML setting 파싱 실패: " + e.getMessage());
        }
        return entries;
    }

    private String myBatisSettingKey(String settingName) {
        return switch (settingName) {
            case "defaultStatementTimeout" -> "mybatis.default-statement-timeout";
            case "defaultFetchSize" -> "mybatis.default-fetch-size";
            case "mapUnderscoreToCamelCase" -> "mybatis.map-underscore-to-camel-case";
            default -> "mybatis." + camelToKebab(settingName);
        };
    }

    private String camelToKebab(String camel) {
        return camel.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase(Locale.ROOT);
    }

    private void addTomcatAttr(
            List<ParsedConfigEntry> entries,
            String fileName,
            Element connector,
            String attr,
            String springKey
    ) {
        if (connector.hasAttribute(attr)) {
            String value = connector.getAttribute(attr);
            entries.add(new ParsedConfigEntry(fileName, springKey, value, normalizeKey(springKey), 1));
        }
    }

    private List<ParsedConfigEntry> parseFlatLines(String fileName, String text) {
        List<ParsedConfigEntry> entries = new ArrayList<>();
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("<!--")) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq > 0) {
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                entries.add(new ParsedConfigEntry(fileName, key, value, normalizeKey(key), i + 1));
            }
        }
        return entries;
    }

    public String normalizeKey(String key) {
        return key.replace('-', '.').toLowerCase(Locale.ROOT);
    }

    public void mergeInto(Map<String, String> target, List<ParsedConfigEntry> entries) {
        for (ParsedConfigEntry entry : entries) {
            target.put(entry.normalizedKey(), entry.configValue());
            target.put(entry.configKey(), entry.configValue());
        }
    }
}
