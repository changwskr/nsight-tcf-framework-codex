package com.nh.nsight.marketing.eb.application.dto.event;

import com.nh.nsight.marketing.eb.persistence.dto.event.EventRow;
import com.nh.nsight.marketing.eb.persistence.dto.event.EventStatusCountRow;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EventInquiryResponse {

    private final String businessCode;
    private final String serviceId;
    private final String guid;
    private final List<Map<String, Object>> rows;
    private final int totalCount;
    private final int pageNo;
    private final int pageSize;
    private final Map<String, Object> statusSummary;

    public EventInquiryResponse(
            String businessCode,
            String serviceId,
            String guid,
            List<Map<String, Object>> rows,
            int totalCount,
            int pageNo,
            int pageSize,
            Map<String, Object> statusSummary) {
        this.businessCode = businessCode;
        this.serviceId = serviceId;
        this.guid = guid;
        this.rows = rows;
        this.totalCount = totalCount;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.statusSummary = statusSummary;
    }

    public static EventInquiryResponse of(
            TransactionContext context,
            EventSearchCriteria criteria,
            List<EventRow> rows,
            int totalCount,
            List<EventStatusCountRow> statusRows) {
        List<Map<String, Object>> rowMaps = new ArrayList<>();
        for (EventRow row : rows) {
            rowMaps.add(row.toMap());
        }
        return new EventInquiryResponse(
                "EB",
                context.getHeader().getServiceId(),
                context.getHeader().getGuid(),
                rowMaps,
                totalCount,
                criteria.getPageNo(),
                criteria.getPageSize(),
                EventStatusSummary.toMap(statusRows));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", businessCode);
        result.put("serviceId", serviceId);
        result.put("guid", guid);
        result.put("rows", rows);
        result.put("totalCount", totalCount);
        result.put("pageNo", pageNo);
        result.put("pageSize", pageSize);
        result.put("statusSummary", statusSummary);
        return result;
    }
}
