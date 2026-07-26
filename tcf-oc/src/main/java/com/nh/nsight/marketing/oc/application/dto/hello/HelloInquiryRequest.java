package com.nh.nsight.marketing.oc.application.dto.hello;

import java.util.Map;

public class HelloInquiryRequest {

    private final String name;

    public HelloInquiryRequest(String name) {
        this.name = name;
    }

    public static HelloInquiryRequest fromMap(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object raw = body.get("name");
        String name = raw == null ? null : String.valueOf(raw).trim();
        return new HelloInquiryRequest(name == null || name.isEmpty() ? null : name);
    }

    public String getName() {
        return name;
    }
}
