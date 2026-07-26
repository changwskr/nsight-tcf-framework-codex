package com.nh.nsight.marketing.eb.persistence.dto.user;

import java.util.LinkedHashMap;
import java.util.Map;

public class UserRow {

    private String userId;
    private String userName;
    private String branchId;
    private String createdAt;
    private String eventId;
    private String eventType;
    private String eventStatus;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
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

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userId", userId);
        map.put("userName", userName);
        map.put("branchId", branchId);
        map.put("createdAt", createdAt);
        map.put("eventId", eventId);
        map.put("eventType", eventType);
        map.put("eventStatus", eventStatus);
        return map;
    }
}
