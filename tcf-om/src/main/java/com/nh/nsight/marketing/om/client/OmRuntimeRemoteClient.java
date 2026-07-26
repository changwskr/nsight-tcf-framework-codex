package com.nh.nsight.marketing.om.client;

import com.nh.nsight.marketing.om.config.OmRuntimeDiagnosticsProperties;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class OmRuntimeRemoteClient {
    private static final Logger log = LoggerFactory.getLogger(OmRuntimeRemoteClient.class);

    private final RestTemplate restTemplate;
    private final OmRuntimeDiagnosticsProperties properties;

    public OmRuntimeRemoteClient(
            RestTemplateBuilder builder,
            OmRuntimeDiagnosticsProperties properties) {
        this.properties = properties;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(Math.max(properties.getConnectTimeoutMs(), 500)))
                .setReadTimeout(Duration.ofMillis(Math.max(properties.getReadTimeoutMs(), 1000)))
                .build();
    }

    public Map<String, Object> fetchStatus(OmRuntimeDiagnosticsProperties.Target target) {
        return fetch(target, "/internal/runtime/status");
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchSlowTransactions(
            OmRuntimeDiagnosticsProperties.Target target, int limit) {
        Map<String, Object> response = fetch(target, "/internal/runtime/slow-transactions?limit=" + limit);
        Object rows = response.get("rows");
        if (rows instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchActiveTransactions(OmRuntimeDiagnosticsProperties.Target target) {
        Map<String, Object> response = fetch(target, "/internal/runtime/active-transactions");
        Object rows = response.get("rows");
        if (rows instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchSlowSql(OmRuntimeDiagnosticsProperties.Target target, int limit) {
        Map<String, Object> response = fetch(target, "/internal/runtime/slow-sql?limit=" + limit);
        Object rows = response.get("rows");
        if (rows instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchThreads(OmRuntimeDiagnosticsProperties.Target target) {
        Map<String, Object> response = fetch(target, "/internal/runtime/threads");
        Object rows = response.get("rows");
        if (rows instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetch(OmRuntimeDiagnosticsProperties.Target target, String path) {
        String baseUrl = normalizeBaseUrl(target.getBaseUrl());
        try {
            if (path.endsWith("/status")) {
                Map<String, Object> body = restTemplate.getForObject(baseUrl + path, Map.class);
                return body != null ? body : unknownStatus(target, "empty response");
            }
            Object body = restTemplate.getForObject(baseUrl + path, Object.class);
            if (body instanceof List<?> list) {
                Map<String, Object> wrapped = new LinkedHashMap<>();
                wrapped.put("rows", list);
                return wrapped;
            }
            if (body instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
            return Map.of("rows", List.of());
        } catch (RestClientException e) {
            log.debug("runtime diagnostics fetch failed. businessCode={} url={}{} reason={}",
                    target.getBusinessCode(), baseUrl, path, e.getMessage());
            if (path.endsWith("/status")) {
                return unknownStatus(target, e.getMessage());
            }
            return Map.of("rows", List.of());
        }
    }

    private Map<String, Object> unknownStatus(OmRuntimeDiagnosticsProperties.Target target, String reason) {
        Map<String, Object> instance = new LinkedHashMap<>();
        instance.put("businessCode", target.getBusinessCode());
        instance.put("applicationName", target.getBusinessCode());
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("status", "UNKNOWN");
        summary.put("primaryCauseCode", "UNKNOWN");
        summary.put("message", "진단 API 조회 실패: " + reason);
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("instance", instance);
        status.put("summary", summary);
        status.put("reachable", false);
        status.put("errorMessage", reason);
        return status;
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
