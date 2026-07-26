package com.nh.nsight.marketing.ep.persistence.dto.userevent;

import java.util.LinkedHashMap;
import java.util.Map;

public class UserEventRow {

    private String eventId;
    private String userId;
    private String eventType;
    private String receivedAt;

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

    public String getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(String receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("eventId", eventId);
        map.put("userId", userId);
        map.put("eventType", eventType);
        map.put("receivedAt", receivedAt);
        return map;
    }
}
