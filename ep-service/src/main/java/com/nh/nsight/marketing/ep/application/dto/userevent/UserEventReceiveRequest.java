package com.nh.nsight.marketing.ep.application.dto.userevent;

import java.util.Map;

public class UserEventReceiveRequest {

    private final String eventId;
    private final String userId;
    private final String eventType;

    public UserEventReceiveRequest(String eventId, String userId, String eventType) {
        this.eventId = eventId;
        this.userId = userId;
        this.eventType = eventType;
    }

    public static UserEventReceiveRequest fromMap(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        return new UserEventReceiveRequest(
                text(body, "eventId"),
                text(body, "userId"),
                text(body, "eventType"));
    }

    private static String text(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value == null ? "" : String.valueOf(value).trim();
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
}
