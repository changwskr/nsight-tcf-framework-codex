package com.nh.nsight.marketing.eb.application.dto.user;

import com.nh.nsight.marketing.eb.persistence.dto.user.UserRow;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UserInquiryResponse {

    private final String businessCode;
    private final String serviceId;
    private final String guid;
    private final List<Map<String, Object>> rows;
    private final int totalCount;
    private final int pageNo;
    private final int pageSize;

    public UserInquiryResponse(
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

    public static UserInquiryResponse of(
            TransactionContext context,
            UserSearchCriteria criteria,
            List<UserRow> rows,
            int totalCount) {
        List<Map<String, Object>> rowMaps = new ArrayList<>();
        for (UserRow row : rows) {
            rowMaps.add(row.toMap());
        }
        return new UserInquiryResponse(
                "EB",
                context.getHeader().getServiceId(),
                context.getHeader().getGuid(),
                rowMaps,
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
        return result;
    }
}
