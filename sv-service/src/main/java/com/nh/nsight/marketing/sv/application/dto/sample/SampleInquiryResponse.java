package com.nh.nsight.marketing.sv.application.dto.sample;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SV.Sample.inquiry 응답 body.
 */
public class SampleInquiryResponse {

    private final String businessCode;
    private final String serviceId;
    private final String guid;
    private final List<SampleListItem> list;
    private final int pageNo;
    private final int pageSize;
    private final int totalCount;
    private final int totalPage;

    public SampleInquiryResponse(
            String businessCode,
            String serviceId,
            String guid,
            List<SampleListItem> list,
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

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", businessCode);
        result.put("serviceId", serviceId);
        result.put("guid", guid);
        List<Map<String, Object>> listMaps = new ArrayList<>();
        for (SampleListItem item : list) {
            listMaps.add(item.toMap());
        }
        result.put("list", listMaps);
        result.put("pageNo", pageNo);
        result.put("pageSize", pageSize);
        result.put("totalCount", totalCount);
        result.put("totalPage", totalPage);
        return result;
    }

    public static SampleInquiryResponse of(
            TransactionContext context,
            List<SampleListItem> list,
            int pageNo,
            int pageSize,
            int totalCount) {
        int totalPage = totalCount == 0 ? 0 : (totalCount + pageSize - 1) / pageSize;
        return new SampleInquiryResponse(
                "SV",
                context.getHeader().getServiceId(),
                context.getHeader().getGuid(),
                list,
                pageNo,
                pageSize,
                totalCount,
                totalPage);
    }
}
