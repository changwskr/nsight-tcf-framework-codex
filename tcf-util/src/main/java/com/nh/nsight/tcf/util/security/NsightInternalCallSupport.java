package com.nh.nsight.tcf.util.security;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** tcf-om → tcf-jwt 등 모듈 간 내부 호출 HMAC 서명·검증 */
public final class NsightInternalCallSupport {
    public static final String HEADER_INTERNAL_CALL = "X-NSIGHT-Internal-Call";
    public static final String HEADER_INTERNAL_SERVICE = "X-NSIGHT-Internal-Service";
    public static final String HEADER_INTERNAL_TIMESTAMP = "X-NSIGHT-Internal-Timestamp";
    public static final String HEADER_INTERNAL_SIGNATURE = "X-NSIGHT-Internal-Signature";

    private NsightInternalCallSupport() {
    }

    public static String sign(String payload, long timestampMillis, String sharedSecret) {
        String canonical = payload + "|" + timestampMillis;
        return hmacSha256Hex(canonical, sharedSecret);
    }

    public static boolean verify(String payload, long timestampMillis, String signature, String sharedSecret) {
        if (signature == null || signature.isBlank() || sharedSecret == null || sharedSecret.isBlank()) {
            return false;
        }
        String expected = sign(payload, timestampMillis, sharedSecret);
        return constantTimeEquals(expected, signature.trim());
    }

    public static String canonicalSsoIssueBody(java.util.Map<String, Object> body) {
        return String.join("|",
                value(body, "userId"),
                value(body, "issuer"),
                value(body, "ssoSubject"),
                value(body, "ssoAssertionId"));
    }

    private static String value(java.util.Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return "";
        }
        return String.valueOf(body.get(key)).trim();
    }

    private static String hmacSha256Hex(String value, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 not available", e);
        }
    }

    private static boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null || left.length() != right.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < left.length(); i++) {
            result |= left.charAt(i) ^ right.charAt(i);
        }
        return result == 0;
    }
}
