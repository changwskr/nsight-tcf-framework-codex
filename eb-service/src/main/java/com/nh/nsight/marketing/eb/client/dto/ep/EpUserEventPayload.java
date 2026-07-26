package com.nh.nsight.marketing.eb.client.dto.ep;

import com.nh.nsight.marketing.eb.persistence.dto.event.EventRow;

public class EpUserEventPayload {

    private final String eventId;
    private final String userId;
    private final String eventType;

    public EpUserEventPayload(String eventId, String userId, String eventType) {
        this.eventId = eventId;
        this.userId = userId;
        this.eventType = eventType;
    }

    public static EpUserEventPayload fromEventRow(EventRow event) {
        return new EpUserEventPayload(event.getEventId(), event.getUserId(), event.getEventType());
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
