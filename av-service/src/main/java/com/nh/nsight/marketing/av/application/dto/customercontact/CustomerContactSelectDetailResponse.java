package com.nh.nsight.marketing.av.application.dto.customercontact;

import com.nh.nsight.marketing.av.persistence.dto.customercontact.CustomerContactRow;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.LinkedHashMap;
import java.util.Map;

public class CustomerContactSelectDetailResponse {

    private final String businessCode;
    private final String serviceId;
    private final String guid;
    private final Map<String, Object> item;

    public CustomerContactSelectDetailResponse(
            String businessCode, String serviceId, String guid, Map<String, Object> item) {
        this.businessCode = businessCode;
        this.serviceId = serviceId;
        this.guid = guid;
        this.item = item;
    }

    public static CustomerContactSelectDetailResponse of(
            TransactionContext context, CustomerContactRow row) {
        return new CustomerContactSelectDetailResponse(
                "AV",
                context.getHeader().getServiceId(),
                context.getHeader().getGuid(),
                row.toMap());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", businessCode);
        result.put("serviceId", serviceId);
        result.put("guid", guid);
        result.putAll(item);
        return result;
    }
}
