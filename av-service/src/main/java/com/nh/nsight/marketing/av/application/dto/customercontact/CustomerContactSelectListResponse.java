package com.nh.nsight.marketing.av.application.dto.customercontact;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CustomerContactSelectListResponse {

    private final String businessCode;
    private final String serviceId;
    private final String guid;
    private final List<Map<String, Object>> rows;
    private final int totalCount;
    private final int pageNo;
    private final int pageSize;

    public CustomerContactSelectListResponse(
            String businessCode,
            String serviceId,
            String guid,
            List<Map<String, Object>> rows,
            int totalCount,
            int pageNo,
            int pageSize) {
        this.businessCode = businessCode;
        this.serviceId = serviceId;
        this.guid = guid;
        this.rows = rows;
        this.totalCount = totalCount;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
    }

    /** rows는 C11 확정에 따라 CONTACT_VALUE 마스킹이 적용된 맵 목록이다. */
    public static CustomerContactSelectListResponse of(
            TransactionContext context,
            CustomerContactSearchCriteria criteria,
            List<Map<String, Object>> rows,
            int totalCount) {
        return new CustomerContactSelectListResponse(
                "AV",
                context.getHeader().getServiceId(),
                context.getHeader().getGuid(),
                rows,
                totalCount,
                criteria.getPageNo(),
                criteria.getPageSize());
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
        result.put("maskedColumns", List.of("contactValue"));
        return result;
    }
}
