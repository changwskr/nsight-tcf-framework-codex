package com.nh.nsight.gateway.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.nsight.gateway.config.GatewayProperties;
import com.nh.nsight.gateway.support.RouteContext;
import com.nh.nsight.gateway.support.RouteResult;
import com.nh.nsight.gateway.support.GatewayCookieParser;
import com.nh.nsight.gateway.support.GatewayRequestUserReader;
import com.nh.nsight.gateway.persistence.dao.GatewayTransactionLogDao;
import com.nh.nsight.gateway.support.GatewayTransactionLogEntry;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class GatewayTransactionLogRecorder {
    private static final Logger log = LoggerFactory.getLogger(GatewayTransactionLogRecorder.class);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter TX_TIME_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final GatewayTransactionLogDao dao;
    private final GatewayProperties properties;
    private final GatewayRequestUserReader requestUserReader;
    private final ObjectMapper objectMapper;

    public GatewayTransactionLogRecorder(GatewayTransactionLogDao dao,
                                         GatewayProperties properties,
                                         GatewayRequestUserReader requestUserReader,
                                         ObjectMapper objectMapper) {
        this.dao = dao;
        this.properties = properties;
        this.requestUserReader = requestUserReader;
        this.objectMapper = objectMapper;
    }

    public void record(String businessCode,
                       String requestBody,
                       String cookieHeader,
                       RouteContext context,
                       RouteResult result,
                       String phase) {
        if (result == null) {
            return;
        }
        try {
            String bodyForParse = resolveBodyForParse(context, requestBody);
            String normalizedBusiness = resolveBusinessCode(businessCode, bodyForParse, context);
            String targetUrl = result.targetUrl();
            if (!StringUtils.hasText(targetUrl) && context != null) {
                targetUrl = context.targetUrl();
            }

            String guid = requestUserReader.guid(bodyForParse).orElse(null);
            String traceId = requestUserReader.traceId(bodyForParse).orElse(null);
            ResponseHints hints = parseResponseHints(result.responseBody(), guid, traceId);
            if (!StringUtils.hasText(guid)) {
                guid = hints.responseGuid();
            }
            if (!StringUtils.hasText(traceId)) {
                traceId = firstNonBlank(hints.responseTraceId(), guid);
            }

            GatewayTransactionLogEntry entry = new GatewayTransactionLogEntry(
                    UUID.randomUUID().toString(),
                    OffsetDateTime.now(KST).format(TX_TIME_FORMAT),
                    properties.getEnvCode(),
                    normalizedBusiness,
                    requestUserReader.serviceId(bodyForParse).orElse(null),
                    requestUserReader.transactionCode(bodyForParse).orElse(null),
                    guid,
                    traceId,
                    requestUserReader.userId(bodyForParse).orElse(null),
                    requestUserReader.branchId(bodyForParse).orElse(null),
                    GatewayCookieParser.sessionId(cookieHeader).orElse(null),
                    truncate(targetUrl, 500),
                    result.httpStatus(),
                    resolveResultStatus(result.httpStatus(), phase, hints),
                    hints.resultCode(),
                    hints.errorCode(),
                    result.elapsedMs(),
                    phase == null ? "SUCCESS" : phase
            );
            dao.insert(entry);
        } catch (Exception e) {
            log.warn("Gateway 거래로그 기록 실패: {}", e.getMessage());
        }
    }

    private String resolveBodyForParse(RouteContext context, String requestBody) {
        if (context != null && StringUtils.hasText(context.enrichedBody())) {
            return context.enrichedBody();
        }
        return requestBody;
    }

    private String resolveBusinessCode(String businessCode, String bodyForParse, RouteContext context) {
        return firstNonBlank(
                businessCode == null ? null : businessCode.toUpperCase(Locale.ROOT),
                requestUserReader.businessCode(bodyForParse).orElse(null),
                context == null ? null : context.module().code());
    }

    private String resolveResultStatus(int httpStatus, String phase, ResponseHints hints) {
        if (StringUtils.hasText(hints.resultCode())) {
            if ("S0000".equals(hints.resultCode())) {
                return "SUCCESS";
            }
            return "FAIL";
        }
        if (httpStatus >= 200 && httpStatus < 300) {
            return "SUCCESS";
        }
        if ("AUTH_FAIL".equals(phase) || httpStatus == 401 || httpStatus == 403) {
            return "FAIL";
        }
        if (httpStatus >= 500 || httpStatus == 502 || "CONNECTION_ERROR".equals(phase)) {
            return "ERROR";
        }
        if ("ROUTE_NOT_FOUND".equals(phase) || httpStatus == 404) {
            return "ERROR";
        }
        return "FAIL";
    }

    private ResponseHints parseResponseHints(String responseBody, String requestGuid, String requestTraceId) {
        if (!StringUtils.hasText(responseBody)) {
            return ResponseHints.empty();
        }
        try {
            Map<String, Object> root = objectMapper.readValue(responseBody, new TypeReference<>() {
            });
            String errorCode = firstNonBlank(
                    nestedString(root, "result", "errorCode"),
                    nestedString(root, "errorCode"),
                    nestedString(root, "code"),
                    root.get("error") instanceof String err ? err : null);
            String resultCode = firstNonBlank(
                    nestedString(root, "result", "resultCode"),
                    nestedString(root, "header", "resultCode"),
                    nestedString(root, "resultCode"));
            String responseGuid = firstNonBlank(
                    nestedString(root, "header", "guid"),
                    nestedString(root, "guid"));
            if (!StringUtils.hasText(responseGuid)) {
                responseGuid = requestGuid;
            }
            String responseTraceId = firstNonBlank(
                    nestedString(root, "header", "traceId"),
                    nestedString(root, "traceId"));
            if (!StringUtils.hasText(responseTraceId)) {
                responseTraceId = requestTraceId;
            }
            return new ResponseHints(resultCode, errorCode, responseGuid, responseTraceId);
        } catch (Exception e) {
            return ResponseHints.empty();
        }
    }

    private String nestedString(Map<String, Object> root, String... path) {
        Object current = root;
        for (String key : path) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = readMapValue(map, key);
        }
        if (current == null) {
            return null;
        }
        String text = String.valueOf(current).trim();
        return text.isEmpty() ? null : text;
    }

    private Object readMapValue(Map<?, ?> map, String key) {
        if (map.containsKey(key)) {
            return map.get(key);
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null && key.equalsIgnoreCase(String.valueOf(entry.getKey()))) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private record ResponseHints(String resultCode, String errorCode, String responseGuid, String responseTraceId) {
        static ResponseHints empty() {
            return new ResponseHints(null, null, null, null);
        }
    }
}
