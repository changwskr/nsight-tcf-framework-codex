package com.nh.nsight.marketing.eb.persistence.dto.event;

import java.util.LinkedHashMap;
import java.util.Map;

public class EventRow {

    private String eventId;
    private String userId;
    private String eventType;
    private String eventStatus;
    private Integer retryCount;
    private String createdAt;
    private String sentAt;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventStatus() {
        return eventStatus;
    }

    public void setEventStatus(String eventStatus) {
        this.eventStatus = eventStatus;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getSentAt() {
        return sentAt;
    }

    public void setSentAt(String sentAt) {
        this.sentAt = sentAt;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("eventId", eventId);
        map.put("userId", userId);
        map.put("eventType", eventType);
        map.put("eventStatus", eventStatus);
        map.put("retryCount", retryCount);
        map.put("createdAt", createdAt);
        map.put("sentAt", sentAt);
        return map;
    }
}
