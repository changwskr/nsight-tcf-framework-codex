package com.nh.nsight.gateway.support;

public enum SessionStatus {
    ACTIVE,
    FORCED_LOGOUT,
    EXPIRED,
    LOGGED_OUT;

    public static SessionStatus fromDb(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }
        return SessionStatus.valueOf(value.trim().toUpperCase());
    }
}
