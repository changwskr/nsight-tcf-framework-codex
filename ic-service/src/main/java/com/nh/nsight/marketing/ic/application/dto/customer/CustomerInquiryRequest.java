package com.nh.nsight.marketing.ic.application.dto.customer;

import java.util.Map;

/**
 * IC.Customer.inquiry 요청 body.
 */
public class CustomerInquiryRequest {

    private final String customerNo;

    public CustomerInquiryRequest(String customerNo) {
        this.customerNo = customerNo;
    }

    public static CustomerInquiryRequest fromMap(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object raw = body.get("customerNo");
        String customerNo = raw == null ? "" : String.valueOf(raw).trim();
        return new CustomerInquiryRequest(customerNo);
    }

    public String getCustomerNo() {
        return customerNo;
    }
}
