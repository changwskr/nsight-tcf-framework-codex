package com.nh.nsight.gateway.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** SESSIONDB 사용자 정보로 header.userId 등을 보정 — Gateway 관문 역할 */
@Component
public class GatewaySessionRequestEnricher {
    private static final String PHASE = "GatewaySessionRequestEnricher.enrich";

    private final ObjectMapper objectMapper;

    public GatewaySessionRequestEnricher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String enrich(String requestBody, GatewaySessionContext sessionContext) {
        if (!StringUtils.hasText(requestBody) || sessionContext == null
                || !StringUtils.hasText(sessionContext.userId())) {
            return requestBody;
        }
        try {
            Map<String, Object> request = objectMapper.readValue(requestBody, new TypeReference<>() {
            });
            @SuppressWarnings("unchecked")
            Map<String, Object> header = (Map<String, Object>) request.computeIfAbsent("header", key -> new LinkedHashMap<>());
            String beforeUserId = header.get("userId") == null ? null : String.valueOf(header.get("userId"));
            putIfPresent(header, "userId", sessionContext.userId());
            putIfPresent(header, "branchId", sessionContext.branchId());
            putIfPresent(header, "channelId", sessionContext.channelId());
            if (StringUtils.hasText(beforeUserId) && !beforeUserId.equals(sessionContext.userId())) {
                GatewayProxyTrace.log(PHASE, "header.userId corrected from=" + beforeUserId
                        + " to=" + sessionContext.userId());
            }
            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            GatewayProxyTrace.log(PHASE, "skip reason=" + e.getClass().getSimpleName());
            return requestBody;
        }
    }

    private void putIfPresent(Map<String, Object> header, String key, String value) {
        if (StringUtils.hasText(value)) {
            header.put(key, value);
        }
    }
}
