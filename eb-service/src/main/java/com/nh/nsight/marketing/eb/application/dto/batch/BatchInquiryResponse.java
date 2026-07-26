package com.nh.nsight.marketing.eb.application.dto.batch;

import com.nh.nsight.marketing.eb.application.dto.event.EventStatusSummary;
import com.nh.nsight.marketing.eb.config.EbEventPublishProperties;
import com.nh.nsight.marketing.eb.persistence.dto.event.EventStatusCountRow;
import com.nh.nsight.marketing.eb.support.EbEventStatus;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BatchInquiryResponse {

    private final String businessCode;
    private final String serviceId;
    private final String guid;
    private final boolean enabled;
    private final long fixedDelayMs;
    private final int batchSize;
    private final String epOnlineUrl;
    private final String schedulerClass;
    private final String schedulerMethod;
    private final Object readyCount;
    private final Object sentCount;
    private final Object failCount;
    private final Object totalCount;
    private final Map<String, Object> statusSummary;

    public BatchInquiryResponse(
            String businessCode,
            String serviceId,
            String guid,
            boolean enabled,
            long fixedDelayMs,
            int batchSize,
            String epOnlineUrl,
            String schedulerClass,
            String schedulerMethod,
            Object readyCount,
            Object sentCount,
            Object failCount,
            Object totalCount,
            Map<String, Object> statusSummary) {
        this.businessCode = businessCode;
        this.serviceId = serviceId;
        this.guid = guid;
        this.enabled = enabled;
        this.fixedDelayMs = fixedDelayMs;
        this.batchSize = batchSize;
        this.epOnlineUrl = epOnlineUrl;
        this.schedulerClass = schedulerClass;
        this.schedulerMethod = schedulerMethod;
        this.readyCount = readyCount;
        this.sentCount = sentCount;
        this.failCount = failCount;
        this.totalCount = totalCount;
        this.statusSummary = statusSummary;
    }

    public static BatchInquiryResponse of(
            TransactionContext context,
            EbEventPublishProperties properties,
            List<EventStatusCountRow> statusRows) {
        Map<String, Object> statusSummary = EventStatusSummary.toBatchMap(statusRows);
        return new BatchInquiryResponse(
                "EB",
                context.getHeader().getServiceId(),
                context.getHeader().getGuid(),
                properties.isEnabled(),
                properties.getFixedDelayMs(),
                properties.getBatchSize(),
                properties.getEpOnlineUrl(),
                "EbEventPublishScheduler",
                "publishUserEvents",
                statusSummary.getOrDefault(EbEventStatus.READY, 0),
                statusSummary.getOrDefault(EbEventStatus.SENT, 0),
                statusSummary.getOrDefault(EbEventStatus.FAIL, 0),
                statusSummary.getOrDefault("TOTAL", 0),
                statusSummary);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", businessCode);
        result.put("serviceId", serviceId);
        result.put("guid", guid);
        result.put("enabled", enabled);
        result.put("fixedDelayMs", fixedDelayMs);
        result.put("batchSize", batchSize);
        result.put("epOnlineUrl", epOnlineUrl);
        result.put("schedulerClass", schedulerClass);
        result.put("schedulerMethod", schedulerMethod);
        result.put("readyCount", readyCount);
        result.put("sentCount", sentCount);
        result.put("failCount", failCount);
        result.put("totalCount", totalCount);
        result.put("statusSummary", statusSummary);
        return result;
    }
}
