package com.nh.nsight.marketing.sv.application.dto.customer;

import java.util.Map;

/**
 * SV.Customer.selectSummary 요청 body.
 */
public class CustomerSummaryRequest {

    private final String customerNo;
    private final String baseDate;

    public CustomerSummaryRequest(String customerNo, String baseDate) {
        this.customerNo = customerNo;
        this.baseDate = baseDate;
    }

    public static CustomerSummaryRequest fromMap(Map<String, Object> body) {
        if (body == null) {
            return new CustomerSummaryRequest(null, null);
        }
        return new CustomerSummaryRequest(
                stringValue(body.get("customerNo")),
                stringValue(body.get("baseDate")));
    }

    public String getCustomerNo() {
        return customerNo;
    }

    public String getBaseDate() {
        return baseDate;
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }
}
