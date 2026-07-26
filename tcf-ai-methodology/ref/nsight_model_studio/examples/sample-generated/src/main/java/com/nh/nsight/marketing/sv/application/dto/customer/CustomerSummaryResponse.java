package com.nh.nsight.marketing.sv.application.dto.customer;

import com.nh.nsight.marketing.sv.persistence.dto.customer.CustomerSummaryRow;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SV.Customer.selectSummary 응답 body.
 */
public class CustomerSummaryResponse {

    private final String businessCode;
    private final String serviceId;
    private final String guid;
    private final CustomerSummaryRow result;

    public CustomerSummaryResponse(String businessCode, String serviceId, String guid, CustomerSummaryRow result) {
        this.businessCode = businessCode;
        this.serviceId = serviceId;
        this.guid = guid;
        this.result = result;
    }

    public static CustomerSummaryResponse of(TransactionContext context, CustomerSummaryRow result) {
        return new CustomerSummaryResponse("SV", context.getHeader().getServiceId(), context.getHeader().getGuid(), result);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("businessCode", businessCode);
        body.put("serviceId", serviceId);
        body.put("guid", guid);
        if (result != null) {
            body.putAll(result.toMap());
        }
        return body;
    }
}
