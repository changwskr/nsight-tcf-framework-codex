package com.nh.nsight.tcf.util.map;

import com.nh.nsight.tcf.util.meta.CopiedFrom;
import com.nh.nsight.tcf.util.meta.CopiedUtilityFlag;
import com.nh.nsight.tcf.util.meta.UtilCategory;
import java.util.Map;

/**
 * tcf-om {@code OmBodySupport} 복사본.
 */
@CopiedFrom(module = "tcf-om", sourceClass = "OmBodySupport", category = UtilCategory.MAP)
public final class OmMapBodyUtils implements CopiedUtilityFlag {

    public static final String COPIED_FROM_MODULE = "tcf-om";
    public static final String COPIED_FROM_CLASS = "OmBodySupport";

    private OmMapBodyUtils() {
    }

    public static String stringValue(Map<String, Object> body, String key) {
        if (body == null || !body.containsKey(key) || body.get(key) == null) {
            return null;
        }
        String value = String.valueOf(body.get(key)).trim();
        return value.isEmpty() ? null : value;
    }

    public static int intValue(Map<String, Object> body, String key, int defaultValue) {
        if (body == null || body.get(key) == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(body.get(key)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static Map<String, Object> searchCriteria(Map<String, Object> body) {
        return body == null ? Map.of() : body;
    }
}
