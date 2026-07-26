package com.nh.nsight.marketing.oc.application.dto.hello;

import com.nh.nsight.marketing.oc.persistence.dto.hello.HelloRow;
import java.util.LinkedHashMap;
import java.util.Map;

public class HelloData {

    private final String name;
    private final String message;
    private final String module;
    private final String createdAt;

    public HelloData(String name, String message, String module, String createdAt) {
        this.name = name;
        this.message = message;
        this.module = module;
        this.createdAt = createdAt;
    }

    public static HelloData fromRow(HelloRow row) {
        return new HelloData(row.getName(), row.getMessage(), row.getModule(), row.getCreatedAt());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("message", message);
        map.put("module", module);
        map.put("createdAt", createdAt);
        return map;
    }
}
