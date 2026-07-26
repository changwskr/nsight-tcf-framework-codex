package com.nh.nsight.auth.jwt.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

public final class JwtSupport {
    private JwtSupport() {}

    public static String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String newJti() {
        return UUID.randomUUID().toString();
    }

    public static String newRefreshTokenPlain() {
        return UUID.randomUUID().toString() + UUID.randomUUID();
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static String stringValue(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    public static void requireField(Map<String, Object> body, String field) {
        String value = stringValue(body, field);
        if (value == null || value.isBlank()) {
            throw new com.nh.nsight.tcf.core.support.error.BusinessException(
                    "E-JWT-VAL-0001", field + " 은(는) 필수입니다.");
        }
    }
}
