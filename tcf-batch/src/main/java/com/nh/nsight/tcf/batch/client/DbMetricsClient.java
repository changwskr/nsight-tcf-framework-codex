package com.nh.nsight.tcf.batch.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.nh.nsight.tcf.batch.config.DbStatusBatchProperties.DbTargetProperties;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class DbMetricsClient {
    private final RestTemplate restTemplate;

    public DbMetricsClient(RestTemplate batchRestTemplate) {
        this.restTemplate = batchRestTemplate;
    }

    public boolean pingJdbc(DbTargetProperties target) {
        try (Connection connection = DriverManager.getConnection(
                target.getJdbcUrl(), target.getJdbcUser(), target.getJdbcPassword());
             Statement statement = connection.createStatement()) {
            statement.executeQuery("SELECT 1");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String dbHealthFromActuator(String baseUrl) {
        try {
            JsonNode health = fetchJson(normalizeBaseUrl(baseUrl) + "/actuator/health");
            JsonNode components = health.path("components");
            for (String component : List.of("db", "jdbc", "database", "r2dbc")) {
                JsonNode node = components.path(component);
                if (!node.isMissingNode()) {
                    return toHealthStatus(node.path("status").asText("DOWN"));
                }
            }
            return toHealthStatus(health.path("status").asText("DOWN"));
        } catch (Exception e) {
            return "FAIL";
        }
    }

    public double poolUsagePctFromActuator(String baseUrl) {
        double active = metricValue(baseUrl, "hikaricp.connections.active");
        double max = metricValue(baseUrl, "hikaricp.connections.max");
        if (max <= 0) {
            active = metricValue(baseUrl, "jdbc.connections.active");
            max = metricValue(baseUrl, "jdbc.connections.max");
        }
        if (max <= 0) {
            return 0;
        }
        return active * 100.0 / max;
    }

    public String resolveHealthStatus(double poolUsagePct, boolean reachable) {
        if (!reachable) {
            return "FAIL";
        }
        if (poolUsagePct >= 95) {
            return "FAIL";
        }
        if (poolUsagePct >= 80) {
            return "WARN";
        }
        return "NORMAL";
    }

    private double metricValue(String baseUrl, String metricName) {
        try {
            JsonNode body = fetchJson(normalizeBaseUrl(baseUrl) + "/actuator/metrics/" + metricName);
            JsonNode measurements = body.path("measurements");
            if (!measurements.isArray() || measurements.isEmpty()) {
                return 0;
            }
            double max = 0;
            for (JsonNode measurement : measurements) {
                max = Math.max(max, measurement.path("value").asDouble(0));
            }
            return max;
        } catch (Exception e) {
            return 0;
        }
    }

    private String toHealthStatus(String actuatorStatus) {
        if ("UP".equalsIgnoreCase(actuatorStatus)) {
            return "NORMAL";
        }
        if ("OUT_OF_SERVICE".equalsIgnoreCase(actuatorStatus) || "UNKNOWN".equalsIgnoreCase(actuatorStatus)) {
            return "WARN";
        }
        return "FAIL";
    }

    private JsonNode fetchJson(String url) {
        return restTemplate.getForObject(url, JsonNode.class);
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl is required");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
