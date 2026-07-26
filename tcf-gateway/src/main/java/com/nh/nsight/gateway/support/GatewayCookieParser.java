package com.nh.nsight.gateway.support;

import java.util.List;
import java.util.Optional;
import org.springframework.util.StringUtils;

public final class GatewayCookieParser {
    public static final String JSESSIONID = "JSESSIONID";
    public static final String NSIGHTSID = "NSIGHTSID";

    private GatewayCookieParser() {
    }

    public static boolean hasSessionCookie(String cookieHeader) {
        return sessionId(cookieHeader).isPresent();
    }

    /** JSESSIONID 우선, 없으면 NSIGHTSID */
    public static Optional<String> sessionId(String cookieHeader) {
        if (!StringUtils.hasText(cookieHeader)) {
            return Optional.empty();
        }
        String jsessionId = null;
        String nsightSid = null;
        for (String part : cookieHeader.split(";")) {
            String trimmed = part.trim();
            if (trimmed.startsWith(JSESSIONID + "=")) {
                jsessionId = valueAfter(trimmed, JSESSIONID + "=");
            } else if (trimmed.startsWith(NSIGHTSID + "=")) {
                nsightSid = valueAfter(trimmed, NSIGHTSID + "=");
            }
        }
        if (StringUtils.hasText(jsessionId)) {
            return Optional.of(jsessionId);
        }
        if (StringUtils.hasText(nsightSid)) {
            return Optional.of(nsightSid);
        }
        return Optional.empty();
    }

    /** downstream Set-Cookie 헤더에서 세션 ID 추출 (JSESSIONID 우선) */
    public static Optional<String> sessionIdFromSetCookies(List<String> setCookies) {
        if (setCookies == null || setCookies.isEmpty()) {
            return Optional.empty();
        }
        String jsessionId = null;
        String nsightSid = null;
        for (String setCookie : setCookies) {
            if (!StringUtils.hasText(setCookie)) {
                continue;
            }
            String firstPart = setCookie.split(";")[0].trim();
            if (firstPart.startsWith(JSESSIONID + "=")) {
                jsessionId = valueAfter(firstPart, JSESSIONID + "=");
            } else if (firstPart.startsWith(NSIGHTSID + "=")) {
                nsightSid = valueAfter(firstPart, NSIGHTSID + "=");
            }
        }
        if (StringUtils.hasText(jsessionId)) {
            return Optional.of(jsessionId);
        }
        if (StringUtils.hasText(nsightSid)) {
            return Optional.of(nsightSid);
        }
        return Optional.empty();
    }

    private static String valueAfter(String part, String prefix) {
        String value = part.substring(prefix.length()).trim();
        return value.isEmpty() ? null : value;
    }
}
