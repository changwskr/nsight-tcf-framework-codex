package com.nh.nsight.marketing.eb.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.nsight.marketing.eb.client.dto.ep.EpUserEventPayload;
import com.nh.nsight.tcf.util.GuidGenerator;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class EpOnlineClient {
    private static final Logger log = LoggerFactory.getLogger(EpOnlineClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public EpOnlineClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
                .build();
    }

    public boolean sendUserEvent(String epOnlineUrl, EpUserEventPayload event) {
        String eventId = event.getEventId();
        String userId = event.getUserId();
        String eventType = event.getEventType();

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("header", buildHeader(eventId, userId));
        request.put("body", Map.of(
                "eventId", eventId,
                "eventType", eventType,
                "userId", userId));

        System.out.println("    >>> [EB→EP] START HTTP POST (EpOnlineClient.sendUserEvent) eventId=" + eventId
                + " userId=" + userId + " url=" + epOnlineUrl);
        try {
            String json = objectMapper.writeValueAsString(request);
            String responseBody = restClient.post()
                    .uri(URI.create(epOnlineUrl))
                    .body(json)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody == null ? "{}" : responseBody);
            JsonNode resultCode = root.path("result").path("resultCode");
            boolean success = "S0000".equals(resultCode.asText());
            if (!success) {
                log.warn("EP call non-success eventId={} resultCode={}", eventId, resultCode.asText());
            }
            System.out.println("    >>> [EB→EP] END HTTP POST (EpOnlineClient.sendUserEvent) eventId=" + eventId
                    + " success=" + success + " resultCode=" + resultCode.asText());
            return success;
        } catch (Exception e) {
            log.warn("EP call failed eventId={} url={} message={}", eventId, epOnlineUrl, e.getMessage());
            System.out.println("    >>> [EB→EP] END HTTP POST (EpOnlineClient.sendUserEvent) eventId=" + eventId
                    + " success=false message=" + e.getMessage());
            return false;
        }
    }

    private Map<String, Object> buildHeader(String eventId, String userId) {
        OffsetDateTime now = OffsetDateTime.now();
        String systemDate = String.format("%04d%02d%02d", now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("systemId", "NSIGHT-MP");
        header.put("businessCode", "EP");
        header.put("serviceId", "EP.UserEvent.receive");
        header.put("serviceName", "EP 사용자 이벤트 수신");
        header.put("transactionCode", "EP-EVT-001");
        header.put("processingType", "EXECUTE");
        header.put("channelId", "EB-BATCH");
        header.put("guid", GuidGenerator.newGuid());
        header.put("traceId", eventId);
        header.put("userId", StringUtils.hasText(userId) ? userId : "EB-BATCH");
        header.put("branchId", "001");
        header.put("requestTime", now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        header.put("systemDate", systemDate);
        header.put("bizDate", systemDate);
        header.put("clientIp", "127.0.0.1");
        return header;
    }
}
