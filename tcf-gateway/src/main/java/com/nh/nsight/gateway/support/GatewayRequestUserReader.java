package com.nh.nsight.gateway.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class GatewayRequestUserReader {
    private final ObjectMapper objectMapper;

    public GatewayRequestUserReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<String> userId(String requestBody) {
        return headerField(requestBody, "userId");
    }

    public Optional<String> serviceId(String requestBody) {
        return headerField(requestBody, "serviceId");
    }

    public Optional<String> guid(String requestBody) {
        return headerField(requestBody, "guid");
    }

    public Optional<String> traceId(String requestBody) {
        return headerField(requestBody, "traceId");
    }

    public Optional<String> transactionCode(String requestBody) {
        return headerField(requestBody, "transactionCode");
    }

    public Optional<String> branchId(String requestBody) {
        return headerField(requestBody, "branchId");
    }

    public Optional<String> businessCode(String requestBody) {
        return headerField(requestBody, "businessCode");
    }

    public Optional<String> channelId(String requestBody) {
        return headerField(requestBody, "channelId");
    }

    public Optional<String> centerId(String requestBody) {
        return headerField(requestBody, "centerId");
    }

    private Optional<String> headerField(String requestBody, String fieldName) {
        if (!StringUtils.hasText(requestBody)) {
            return Optional.empty();
        }
        try {
            Map<String, Object> request = objectMapper.readValue(requestBody, new TypeReference<>() {
            });
            Object header = request.get("header");
            if (!(header instanceof Map<?, ?> headerMap)) {
                return Optional.empty();
            }
            Object value = readHeaderValue(headerMap, fieldName);
            if (value == null) {
                return Optional.empty();
            }
            String text = String.valueOf(value).trim();
            return text.isEmpty() ? Optional.empty() : Optional.of(text);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Object readHeaderValue(Map<?, ?> headerMap, String fieldName) {
        if (headerMap.containsKey(fieldName)) {
            return headerMap.get(fieldName);
        }
        for (Map.Entry<?, ?> entry : headerMap.entrySet()) {
            if (entry.getKey() != null && fieldName.equalsIgnoreCase(String.valueOf(entry.getKey()))) {
                return entry.getValue();
            }
        }
        return null;
    }
}
