package com.nh.nsight.marketing.eb.application.dto.event;

import java.util.Map;

public class EventInquiryRequest {

    private final Integer pageNo;
    private final Integer pageSize;
    private final String eventId;
    private final String userId;
    private final String eventType;
    private final String eventStatus;

    public EventInquiryRequest(
            Integer pageNo,
            Integer pageSize,
            String eventId,
            String userId,
            String eventType,
            String eventStatus) {
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.eventId = eventId;
        this.userId = userId;
        this.eventType = eventType;
        this.eventStatus = eventStatus;
    }

    public static EventInquiryRequest fromMap(Map<String, Object> body) {
        Map<String, Object> safeBody = body != null ? body : Map.of();
        return new EventInquiryRequest(
                toInteger(safeBody.get("pageNo")),
                toInteger(safeBody.get("pageSize")),
                trimToNull(safeBody.get("eventId")),
                trimToNull(safeBody.get("userId")),
                trimToNull(safeBody.get("eventType")),
                trimToNull(safeBody.get("eventStatus")));
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public String getEventId() {
        return eventId;
    }

    public String getUserId() {
        return userId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEventStatus() {
        return eventStatus;
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String trimToNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
