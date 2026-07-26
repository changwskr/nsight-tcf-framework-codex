package com.nh.nsight.marketing.ln.application.dto.customercontact;

import java.util.Map;

public class CustomerContactSelectListRequest {

    private final Integer pageNo;
    private final Integer pageSize;
    private final String customerNo;
    private final String contactType;

    public CustomerContactSelectListRequest(
            Integer pageNo, Integer pageSize, String customerNo, String contactType) {
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.customerNo = customerNo;
        this.contactType = contactType;
    }

    public static CustomerContactSelectListRequest fromMap(Map<String, Object> body) {
        Map<String, Object> safeBody = body != null ? body : Map.of();
        return new CustomerContactSelectListRequest(
                toInteger(safeBody.get("pageNo")),
                toInteger(safeBody.get("pageSize")),
                trimToNull(safeBody.get("customerNo")),
                trimToNull(safeBody.get("contactType")));
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public String getCustomerNo() {
        return customerNo;
    }

    public String getContactType() {
        return contactType;
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String trimToNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
