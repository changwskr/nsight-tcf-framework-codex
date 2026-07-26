package com.nh.nsight.gateway.support;

public enum SessionType {
    BUSINESS,
    OM,
    API;

    public static SessionType fromDb(String value) {
        if (value == null || value.isBlank()) {
            return BUSINESS;
        }
        return SessionType.valueOf(value.trim().toUpperCase());
    }

    public static SessionType fromBusinessCode(String businessCode) {
        if (businessCode == null) {
            return BUSINESS;
        }
        return switch (businessCode.trim().toUpperCase()) {
            case "OM" -> OM;
            case "JWT" -> API;
            default -> BUSINESS;
        };
    }
}
