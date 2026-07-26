package com.nh.nsight.tcf.util.string;

import com.nh.nsight.tcf.util.meta.CopiedFrom;
import com.nh.nsight.tcf.util.meta.CopiedUtilityFlag;
import com.nh.nsight.tcf.util.meta.UtilCategory;

/**
 * Spring {@code StringUtils.hasText} 대체 — tcf-util 전용 (Spring 의존 없음).
 */
@CopiedFrom(module = "tcf-util", sourceClass = "TcfStringUtils", category = UtilCategory.STRING, nativeUtility = true)
public final class TcfStringUtils implements CopiedUtilityFlag {

    public static final String COPIED_FROM_MODULE = "tcf-util";
    public static final String COPIED_FROM_CLASS = "TcfStringUtils";

    private TcfStringUtils() {
    }

    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
