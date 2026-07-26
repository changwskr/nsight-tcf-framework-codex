package com.nh.nsight.marketing.om.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.nsight.marketing.om.config.OmSsoProperties;
import com.nh.nsight.marketing.om.support.OmBodySupport;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.util.GuidGenerator;
import com.nh.nsight.tcf.util.security.NsightInternalCallSupport;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class OmJwtSsoClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final OmSsoProperties properties;

    public OmJwtSsoClient(ObjectMapper objectMapper, OmSsoProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.restClient = RestClient.builder()
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
                .build();
    }

    public Map<String, Object> issueSsoToken(Map<String, Object> user, Map<String, Object> ssoBody,
                                             TransactionContext context) {
        Map<String, Object> jwtBody = new LinkedHashMap<>();
        jwtBody.put("userId", OmBodySupport.stringValue(user, "userId"));
        jwtBody.put("userName", OmBodySupport.stringValue(user, "userName"));
        jwtBody.put("branchId", OmBodySupport.stringValue(user, "branchId"));
        jwtBody.put("authGroupId", OmBodySupport.stringValue(user, "authGroupId"));
        jwtBody.put("authGroupName", OmBodySupport.stringValue(user, "authGroupName"));
        jwtBody.put("issuer", "OM-SSO");
        jwtBody.put("ssoSubject", OmBodySupport.stringValue(ssoBody, "ssoSubject"));
        jwtBody.put("ssoAssertionId", resolveAssertionId(ssoBody));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("header", buildHeader(user, context));
        request.put("body", jwtBody);

        try {
            String requestJson = objectMapper.writeValueAsString(request);
            String canonical = NsightInternalCallSupport.canonicalSsoIssueBody(jwtBody);
            long timestamp = System.currentTimeMillis();
            String signature = NsightInternalCallSupport.sign(canonical, timestamp,
                    properties.getInternalSharedSecret());

            String responseBody = restClient.post()
                    .uri(URI.create(resolveOnlineUrl()))
                    .header(NsightInternalCallSupport.HEADER_INTERNAL_CALL, "true")
                    .header(NsightInternalCallSupport.HEADER_INTERNAL_SERVICE, properties.getInternalServiceName())
                    .header(NsightInternalCallSupport.HEADER_INTERNAL_TIMESTAMP, String.valueOf(timestamp))
                    .header(NsightInternalCallSupport.HEADER_INTERNAL_SIGNATURE, signature)
                    .body(requestJson)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody == null ? "{}" : responseBody);
            JsonNode resultCode = root.path("result").path("resultCode");
            if (!"S0000".equals(resultCode.asText())) {
                String message = root.path("result").path("errorMessage").asText("JWT SSO 발급에 실패했습니다.");
                throw new BusinessException("E-OM-SSO-0003", message);
            }
            JsonNode bodyNode = root.path("body");
            Map<String, Object> tokenBody = objectMapper.convertValue(bodyNode, new TypeReference<Map<String, Object>>() {});
            return tokenBody != null ? tokenBody : Map.of();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("E-OM-SSO-0003",
                    "tcf-jwt JWT.Auth.ssoIssue 호출 실패: " + e.getMessage());
        }
    }

    private String resolveOnlineUrl() {
        String base = properties.getJwtServiceUrl();
        if (!StringUtils.hasText(base)) {
            throw new BusinessException("E-OM-SSO-0003", "JWT 서비스 URL이 구성되지 않았습니다.");
        }
        String normalized = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return normalized + "/online";
    }

    private String resolveAssertionId(Map<String, Object> ssoBody) {
        String assertionId = OmBodySupport.stringValue(ssoBody, "ssoAssertionId");
        if (StringUtils.hasText(assertionId)) {
            return assertionId;
        }
        return "SSO-ASSERTION-" + GuidGenerator.newGuid();
    }

    private Map<String, Object> buildHeader(Map<String, Object> user, TransactionContext context) {
        OffsetDateTime now = OffsetDateTime.now();
        String systemDate = String.format("%04d%02d%02d", now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        String userId = OmBodySupport.stringValue(user, "userId");
        String channelId = context != null && context.getHeader() != null
                ? context.getHeader().getChannelId() : null;
        if (!StringUtils.hasText(channelId)) {
            channelId = "WEBTOP";
        }
        String guid = context != null && context.getHeader() != null
                ? context.getHeader().getGuid() : GuidGenerator.newGuid();
        String traceId = context != null && context.getHeader() != null
                ? context.getHeader().getTraceId() : guid;

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("systemId", "NSIGHT-MP");
        header.put("businessCode", "JWT");
        header.put("serviceId", "JWT.Auth.ssoIssue");
        header.put("serviceName", "JWT SSO 토큰 발급");
        header.put("transactionCode", "JWT-AUT-0005");
        header.put("processingType", "EXECUTE");
        header.put("channelId", channelId);
        header.put("guid", guid);
        header.put("traceId", traceId);
        header.put("userId", userId);
        header.put("branchId", OmBodySupport.stringValue(user, "branchId"));
        header.put("requestTime", now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        header.put("systemDate", systemDate);
        header.put("bizDate", systemDate);
        header.put("clientIp", context != null && context.getHeader() != null
                ? context.getHeader().getClientIp() : "127.0.0.1");
        return header;
    }
}
