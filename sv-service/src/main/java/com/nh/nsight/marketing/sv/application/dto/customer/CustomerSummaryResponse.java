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
    private final CustomerSummaryRow customer;

    public CustomerSummaryResponse(
            String businessCode, String serviceId, String guid, CustomerSummaryRow customer) {
        this.businessCode = businessCode;
        this.serviceId = serviceId;
        this.guid = guid;
        this.customer = customer;
    }

    public static CustomerSummaryResponse of(
            TransactionContext context, CustomerSummaryRow customer) {
        return new CustomerSummaryResponse(
                "SV",
                context.getHeader().getServiceId(),
                context.getHeader().getGuid(),
                customer);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", businessCode);
        result.put("serviceId", serviceId);
        result.put("guid", guid);
        if (customer != null) {
            result.putAll(customer.toMap());
        }
        return result;
    }
}
