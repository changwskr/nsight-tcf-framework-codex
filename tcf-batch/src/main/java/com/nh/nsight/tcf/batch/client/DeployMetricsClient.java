package com.nh.nsight.tcf.batch.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.nh.nsight.tcf.batch.config.DeployStatusBatchProperties.DeployTargetProperties;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
public class DeployMetricsClient {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final RestTemplate restTemplate;

    public DeployMetricsClient(RestTemplate batchRestTemplate) {
        this.restTemplate = batchRestTemplate;
    }

    public DeployProbe collect(DeployTargetProperties target, String defaultVersion) {
        if ("http".equalsIgnoreCase(target.getSourceType())) {
            return collectHttp(target, defaultVersion);
        }
        return collectActuator(target, defaultVersion);
    }

    private DeployProbe collectActuator(DeployTargetProperties target, String defaultVersion) {
        String baseUrl = normalizeBaseUrl(target.getBaseUrl());
        try {
            JsonNode health = fetchJson(baseUrl + "/actuator/health");
            String healthStatus = mapHealthStatus(health.path("status").asText("DOWN"));
            boolean reachable = "UP".equalsIgnoreCase(health.path("status").asText("DOWN"));
            if (!reachable) {
                return DeployProbe.unreachable(healthStatus, "Actuator health DOWN");
            }

            String version = resolveVersion(baseUrl, target, defaultVersion);
            String deployedAt = resolveProcessStartTime(baseUrl);
            return new DeployProbe(true, healthStatus, version, deployedAt, "OK");
        } catch (Exception e) {
            return DeployProbe.unreachable("DOWN", e.getMessage());
        }
    }

    private DeployProbe collectHttp(DeployTargetProperties target, String defaultVersion) {
        String baseUrl = normalizeBaseUrl(target.getBaseUrl());
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/", String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                String version = StringUtils.hasText(target.getDefaultVersion())
                        ? target.getDefaultVersion() : defaultVersion;
                return new DeployProbe(true, "UP", version, null, "HTTP 200");
            }
            return DeployProbe.unreachable("DOWN", "HTTP " + response.getStatusCode().value());
        } catch (Exception e) {
            return DeployProbe.unreachable("DOWN", e.getMessage());
        }
    }

    private String resolveVersion(String baseUrl, DeployTargetProperties target, String defaultVersion) {
        if (StringUtils.hasText(target.getDefaultVersion())) {
            return target.getDefaultVersion();
        }
        try {
            JsonNode info = fetchJson(baseUrl + "/actuator/info");
            String version = textAt(info, "build", "version");
            if (!StringUtils.hasText(version)) {
                version = textAt(info, "app", "version");
            }
            if (StringUtils.hasText(version)) {
                return version;
            }
        } catch (Exception ignored) {
            // info endpoint optional
        }
        return defaultVersion;
    }

    private String resolveProcessStartTime(String baseUrl) {
        try {
            JsonNode body = fetchJson(baseUrl + "/actuator/metrics/process.start.time");
            JsonNode measurements = body.path("measurements");
            if (!measurements.isArray() || measurements.isEmpty()) {
                return null;
            }
            double epochSec = measurements.get(0).path("value").asDouble(0);
            if (epochSec <= 0) {
                return null;
            }
            long epochMillis = (long) (epochSec * 1000);
            return OffsetDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), KST).toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String mapHealthStatus(String actuatorStatus) {
        if ("UP".equalsIgnoreCase(actuatorStatus)) {
            return "UP";
        }
        if ("OUT_OF_SERVICE".equalsIgnoreCase(actuatorStatus)) {
            return "DOWN";
        }
        return actuatorStatus == null || actuatorStatus.isBlank() ? "DOWN" : actuatorStatus.toUpperCase();
    }

    private String textAt(JsonNode node, String... paths) {
        JsonNode current = node;
        for (String path : paths) {
            current = current.path(path);
        }
        return current.isMissingNode() || current.isNull() ? null : current.asText(null);
    }

    private JsonNode fetchJson(String url) {
        return restTemplate.getForObject(url, JsonNode.class);
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalArgumentException("baseUrl is required");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public record DeployProbe(
            boolean reachable,
            String healthStatus,
            String warVersion,
            String deployedAt,
            String detailMessage
    ) {
        public static DeployProbe unreachable(String healthStatus, String detail) {
            return new DeployProbe(false, healthStatus, null, null, detail);
        }
    }
}
