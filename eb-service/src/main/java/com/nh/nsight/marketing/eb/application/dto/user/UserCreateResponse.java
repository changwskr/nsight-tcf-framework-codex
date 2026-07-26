package com.nh.nsight.marketing.eb.application.dto.user;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.LinkedHashMap;
import java.util.Map;

public class UserCreateResponse {

    private final String businessCode;
    private final String serviceId;
    private final String guid;
    private final String userId;
    private final String userName;
    private final String branchId;
    private final String eventId;
    private final String eventType;
    private final String eventStatus;

    public UserCreateResponse(
            String businessCode,
            String serviceId,
            String guid,
            String userId,
            String userName,
            String branchId,
            String eventId,
            String eventType,
            String eventStatus) {
        this.businessCode = businessCode;
        this.serviceId = serviceId;
        this.guid = guid;
        this.userId = userId;
        this.userName = userName;
        this.branchId = branchId;
        this.eventId = eventId;
        this.eventType = eventType;
        this.eventStatus = eventStatus;
    }

    public static UserCreateResponse of(
            TransactionContext context,
            String userId,
            String userName,
            String branchId,
            String eventId,
            String eventType,
            String eventStatus) {
        return new UserCreateResponse(
                "EB",
                context.getHeader().getServiceId(),
                context.getHeader().getGuid(),
                userId,
                userName,
                branchId,
                eventId,
                eventType,
                eventStatus);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", businessCode);
        result.put("serviceId", serviceId);
        result.put("guid", guid);
        result.put("userId", userId);
        result.put("userName", userName);
        result.put("branchId", branchId);
        result.put("eventId", eventId);
        result.put("eventType", eventType);
        result.put("eventStatus", eventStatus);
        return result;
    }
}
