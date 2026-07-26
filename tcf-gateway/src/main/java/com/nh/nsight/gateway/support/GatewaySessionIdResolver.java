package com.nh.nsight.gateway.support;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.util.StringUtils;

/** JSESSIONID 쿠키 값 ↔ SPRING_SESSION.SESSION_ID(평문 UUID) 변환 */
public final class GatewaySessionIdResolver {
    private static final Base64.Decoder BASE64_DECODER = Base64.getUrlDecoder();
    private static final Base64.Encoder BASE64_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private GatewaySessionIdResolver() {
    }

    public static List<String> lookupCandidates(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return List.of();
        }
        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(sessionId.trim());
        decodeBase64(sessionId).ifPresent(candidates::add);
        if (looksLikeUuid(sessionId)) {
            candidates.add(encodeBase64(sessionId.trim()));
        }
        return new ArrayList<>(candidates);
    }

    public static Optional<String> decodeBase64(String value) {
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        try {
            String decoded = new String(BASE64_DECODER.decode(value.trim()), StandardCharsets.UTF_8);
            return StringUtils.hasText(decoded) ? Optional.of(decoded) : Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static String encodeBase64(String uuid) {
        return BASE64_ENCODER.encodeToString(uuid.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean looksLikeUuid(String value) {
        try {
            UUID.fromString(value.trim());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
