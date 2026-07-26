package com.nh.nsight.marketing.ep.application.dto.userevent;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.LinkedHashMap;
import java.util.Map;

public class UserEventReceiveResponse {

    private final String businessCode;
    private final String serviceId;
    private final String guid;
    private final String eventId;
    private final String userId;
    private final String eventType;
    private final boolean received;

    public UserEventReceiveResponse(
            String businessCode,
            String serviceId,
            String guid,
            String eventId,
            String userId,
            String eventType,
            boolean received) {
        this.businessCode = businessCode;
        this.serviceId = serviceId;
        this.guid = guid;
        this.eventId = eventId;
        this.userId = userId;
        this.eventType = eventType;
        this.received = received;
    }

    public static UserEventReceiveResponse of(TransactionContext context, UserEventReceiveRequest request) {
        return new UserEventReceiveResponse(
                "EP",
                context.getHeader().getServiceId(),
                context.getHeader().getGuid(),
                request.getEventId(),
                request.getUserId(),
                request.getEventType(),
                true);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", businessCode);
        result.put("serviceId", serviceId);
        result.put("guid", guid);
        result.put("eventId", eventId);
        result.put("userId", userId);
        result.put("eventType", eventType);
        result.put("received", received);
        return result;
    }
}
