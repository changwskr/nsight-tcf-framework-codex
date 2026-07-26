package com.nh.nsight.tcf.util.http;

import com.nh.nsight.tcf.util.meta.CopiedFrom;
import com.nh.nsight.tcf.util.meta.CopiedUtilityFlag;
import com.nh.nsight.tcf.util.meta.UtilCategory;
import com.nh.nsight.tcf.util.string.TcfStringUtils;
import java.util.List;
import java.util.Optional;

/**
 * tcf-gateway {@code GatewayCookieParser} 복사본.
 */
@CopiedFrom(module = "tcf-gateway", sourceClass = "GatewayCookieParser", category = UtilCategory.HTTP)
public final class GatewayCookieParserUtils implements CopiedUtilityFlag {

    public static final String COPIED_FROM_MODULE = "tcf-gateway";
    public static final String COPIED_FROM_CLASS = "GatewayCookieParser";

    public static final String JSESSIONID = "JSESSIONID";
    public static final String NSIGHTSID = "NSIGHTSID";

    private GatewayCookieParserUtils() {
    }

    public static boolean hasSessionCookie(String cookieHeader) {
        return sessionId(cookieHeader).isPresent();
    }

    /** JSESSIONID 우선, 없으면 NSIGHTSID */
    public static Optional<String> sessionId(String cookieHeader) {
        if (!TcfStringUtils.hasText(cookieHeader)) {
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
        if (TcfStringUtils.hasText(jsessionId)) {
            return Optional.of(jsessionId);
        }
        if (TcfStringUtils.hasText(nsightSid)) {
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
            if (!TcfStringUtils.hasText(setCookie)) {
                continue;
            }
            String firstPart = setCookie.split(";")[0].trim();
            if (firstPart.startsWith(JSESSIONID + "=")) {
                jsessionId = valueAfter(firstPart, JSESSIONID + "=");
            } else if (firstPart.startsWith(NSIGHTSID + "=")) {
                nsightSid = valueAfter(firstPart, NSIGHTSID + "=");
            }
        }
        if (TcfStringUtils.hasText(jsessionId)) {
            return Optional.of(jsessionId);
        }
        if (TcfStringUtils.hasText(nsightSid)) {
            return Optional.of(nsightSid);
        }
        return Optional.empty();
    }

    private static String valueAfter(String part, String prefix) {
        String value = part.substring(prefix.length()).trim();
        return value.isEmpty() ? null : value;
    }
}
