package com.nh.nsight.marketing.oc.application.dto.hello;

import com.nh.nsight.marketing.oc.persistence.dto.hello.HelloRow;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.LinkedHashMap;
import java.util.Map;

public class HelloInquiryResponse {

    private final String businessCode;
    private final String serviceId;
    private final String guid;
    private final HelloData data;

    public HelloInquiryResponse(String businessCode, String serviceId, String guid, HelloData data) {
        this.businessCode = businessCode;
        this.serviceId = serviceId;
        this.guid = guid;
        this.data = data;
    }

    public static HelloInquiryResponse of(TransactionContext context, HelloRow row) {
        return new HelloInquiryResponse(
                "OC",
                context.getHeader().getServiceId(),
                context.getHeader().getGuid(),
                HelloData.fromRow(row));
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
