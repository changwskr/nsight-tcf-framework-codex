package com.nh.nsight.marketing.eb.application.dto.systemtx;

import com.nh.nsight.marketing.eb.persistence.dto.systemtx.SystemTxRow;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SystemTxInquiryResponse {

    private final String businessCode;
    private final String serviceId;
    private final String guid;
    private final String screenNo;
    private final List<Map<String, Object>> rows;
    private final int totalCount;
    private final int pageNo;
    private final int pageSize;

    public SystemTxInquiryResponse(
            String businessCode,
            String serviceId,
            String guid,
            String screenNo,
            List<Map<String, Object>> rows,
            int totalCount,
            int pageNo,
            int pageSize) {
        this.businessCode = businessCode;
        this.serviceId = serviceId;
        this.guid = guid;
        this.screenNo = screenNo;
        this.rows = rows;
        this.totalCount = totalCount;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
    }

    public static SystemTxInquiryResponse of(
            TransactionContext context,
            SystemTxSearchCriteria criteria,
            List<SystemTxRow> rows,
            int totalCount) {
        List<Map<String, Object>> rowMaps = new ArrayList<>();
        long base = (long) (criteria.getPageNo() - 1) * criteria.getPageSize();
        for (int i = 0; i < rows.size(); i++) {
            SystemTxRow row = rows.get(i);
            if (row.getRowNo() == null) {
                row.setRowNo(base + i + 1);
            }
            rowMaps.add(row.toMap());
        }
        return new SystemTxInquiryResponse(
                "EB",
                context.getHeader().getServiceId(),
                context.getHeader().getGuid(),
                "19410",
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
        result.put("screenNo", screenNo);
        result.put("rows", rows);
        result.put("totalCount", totalCount);
        result.put("pageNo", pageNo);
        result.put("pageSize", pageSize);
        return result;
    }
}
