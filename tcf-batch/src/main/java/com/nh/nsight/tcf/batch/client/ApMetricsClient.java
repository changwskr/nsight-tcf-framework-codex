package com.nh.nsight.tcf.batch.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.nh.nsight.tcf.batch.config.ApStatusBatchProperties.ApTargetProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ApMetricsClient {
    private final RestTemplate restTemplate;

    public ApMetricsClient(RestTemplate batchRestTemplate) {
        this.restTemplate = batchRestTemplate;
    }

    public boolean isReachable(ApTargetProperties target) {
        try {
            JsonNode health = fetchJson(normalizeBaseUrl(target.getBaseUrl()) + "/actuator/health");
            String status = health.path("status").asText("DOWN");
            return "UP".equalsIgnoreCase(status);
        } catch (Exception e) {
            return false;
        }
    }

    public String healthStatus(ApTargetProperties target) {
        try {
            JsonNode health = fetchJson(normalizeBaseUrl(target.getBaseUrl()) + "/actuator/health");
            String status = health.path("status").asText("DOWN");
            return "UP".equalsIgnoreCase(status) ? "NORMAL" : "FAIL";
        } catch (Exception e) {
            return "FAIL";
        }
    }

    public double cpuUsagePct(ApTargetProperties target) {
        return metricValue(target, "process.cpu.usage") * 100.0;
    }

    public double heapUsagePct(ApTargetProperties target) {
        double used = metricValue(target, "jvm.memory.used", "area:heap");
        double max = metricValue(target, "jvm.memory.max", "area:heap");
        if (max <= 0) {
            return 0;
        }
        return used * 100.0 / max;
    }

    public int threadCount(ApTargetProperties target) {
        return (int) Math.round(metricValue(target, "jvm.threads.live"));
    }

    private double metricValue(ApTargetProperties target, String metricName, String... tags) {
        StringBuilder url = new StringBuilder(normalizeBaseUrl(target.getBaseUrl()))
                .append("/actuator/metrics/")
                .append(metricName);
        if (tags.length > 0) {
            url.append("?tag=").append(tags[0]);
        }
        JsonNode body = fetchJson(url.toString());
        JsonNode measurements = body.path("measurements");
        if (!measurements.isArray() || measurements.isEmpty()) {
            return 0;
        }
        return measurements.get(0).path("value").asDouble(0);
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
