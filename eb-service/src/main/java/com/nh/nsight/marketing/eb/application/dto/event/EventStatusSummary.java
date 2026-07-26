package com.nh.nsight.marketing.eb.application.dto.event;

import com.nh.nsight.marketing.eb.persistence.dto.event.EventStatusCountRow;
import com.nh.nsight.marketing.eb.support.EbEventStatus;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EventStatusSummary {

    private EventStatusSummary() {
    }

    public static Map<String, Object> toMap(List<EventStatusCountRow> rows) {
        Map<String, Object> summary = new LinkedHashMap<>();
        int total = 0;
        for (EventStatusCountRow row : rows) {
            String status = row.getEventStatus() == null ? "" : row.getEventStatus();
            summary.put(status, row.getCount());
            total += row.getCount();
        }
        summary.put("TOTAL", total);
        return summary;
    }

    public static Map<String, Object> toBatchMap(List<EventStatusCountRow> rows) {
        Map<String, Object> summary = toMap(rows);
        ensureDefault(summary, EbEventStatus.READY);
        ensureDefault(summary, EbEventStatus.SENT);
        ensureDefault(summary, EbEventStatus.FAIL);
        return summary;
    }

    private static void ensureDefault(Map<String, Object> summary, String status) {
        if (!summary.containsKey(status)) {
            summary.put(status, 0);
        }
    }
}
