package com.nh.nsight.marketing.ln.application.dto.customercontact;

import java.util.Map;

public class CustomerContactSelectDetailRequest {

    private final String contactId;

    public CustomerContactSelectDetailRequest(String contactId) {
        this.contactId = contactId;
    }

    public static CustomerContactSelectDetailRequest fromMap(Map<String, Object> body) {
        Map<String, Object> safeBody = body != null ? body : Map.of();
        return new CustomerContactSelectDetailRequest(trimToNull(safeBody.get("contactId")));
    }

    public String getContactId() {
        return contactId;
    }

    private static String trimToNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
