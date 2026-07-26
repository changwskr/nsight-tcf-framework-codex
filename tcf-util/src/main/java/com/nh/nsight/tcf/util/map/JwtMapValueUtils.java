package com.nh.nsight.tcf.util.map;

import com.nh.nsight.tcf.util.meta.CopiedFrom;
import com.nh.nsight.tcf.util.meta.CopiedUtilityFlag;
import com.nh.nsight.tcf.util.meta.UtilCategory;
import java.util.Map;

/**
 * tcf-jwt {@code JwtSupport.stringValue} 복사본.
 */
@CopiedFrom(module = "tcf-jwt", sourceClass = "JwtSupport", category = UtilCategory.MAP)
public final class JwtMapValueUtils implements CopiedUtilityFlag {

    public static final String COPIED_FROM_MODULE = "tcf-jwt";
    public static final String COPIED_FROM_CLASS = "JwtSupport";

    private JwtMapValueUtils() {
    }

    public static String stringValue(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
