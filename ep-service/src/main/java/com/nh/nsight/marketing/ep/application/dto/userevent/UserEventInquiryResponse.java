package com.nh.nsight.marketing.ep.application.dto.userevent;

import com.nh.nsight.marketing.ep.persistence.dto.userevent.UserEventRow;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UserEventInquiryResponse {

    private final String businessCode;
    private final String serviceId;
    private final String guid;
    private final List<Map<String, Object>> list;
    private final int pageNo;
    private final int pageSize;
    private final int totalCount;
    private final int totalPage;

    public UserEventInquiryResponse(
            String businessCode,
            String serviceId,
            String guid,
            List<Map<String, Object>> list,
            int pageNo,
            int pageSize,
            int totalCount,
            int totalPage) {
        this.businessCode = businessCode;
        this.serviceId = serviceId;
        this.guid = guid;
        this.list = list;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
        this.totalPage = totalPage;
    }

    public static UserEventInquiryResponse of(
            TransactionContext context,
            UserEventSearchCriteria criteria,
            List<UserEventRow> rows,
            int totalCount) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (UserEventRow row : rows) {
            list.add(row.toMap());
        }
        int pageSize = criteria.getPageSize();
        int totalPage = totalCount == 0 ? 0 : (totalCount + pageSize - 1) / pageSize;
        return new UserEventInquiryResponse(
                "EP",
                context.getHeader().getServiceId(),
                context.getHeader().getGuid(),
                list,
                criteria.getPageNo(),
                pageSize,
                totalCount,
                totalPage);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", businessCode);
        result.put("serviceId", serviceId);
        result.put("guid", guid);
        result.put("list", list);
        result.put("pageNo", pageNo);
        result.put("pageSize", pageSize);
        result.put("totalCount", totalCount);
        result.put("totalPage", totalPage);
        return result;
    }
}
