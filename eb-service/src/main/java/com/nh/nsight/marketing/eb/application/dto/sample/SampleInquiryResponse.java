package com.nh.nsight.marketing.eb.application.dto.sample;

import com.nh.nsight.marketing.eb.persistence.dto.sample.SampleRow;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.LinkedHashMap;
import java.util.Map;

public class SampleInquiryResponse {

    private final String businessCode;
    private final String serviceId;
    private final String guid;
    private final SampleData data;

    public SampleInquiryResponse(String businessCode, String serviceId, String guid, SampleData data) {
        this.businessCode = businessCode;
        this.serviceId = serviceId;
        this.guid = guid;
        this.data = data;
    }

    public static SampleInquiryResponse of(TransactionContext context, SampleRow row) {
        return new SampleInquiryResponse(
                "EB",
                context.getHeader().getServiceId(),
                context.getHeader().getGuid(),
                SampleData.fromRow(row));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", businessCode);
        result.put("serviceId", serviceId);
        result.put("guid", guid);
        result.put("data", data.toMap());
        return result;
    }
}
