package com.nh.nsight.marketing.eb.persistence.dto.event;

public class EventStatusCountRow {

    private String eventStatus;
    private int count;

    public String getEventStatus() {
        return eventStatus;
    }

    public void setEventStatus(String eventStatus) {
        this.eventStatus = eventStatus;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
