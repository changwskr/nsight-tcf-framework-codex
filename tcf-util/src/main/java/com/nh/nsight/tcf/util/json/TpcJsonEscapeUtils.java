package com.nh.nsight.tcf.util.json;

import com.nh.nsight.tcf.util.meta.CopiedFrom;
import com.nh.nsight.tcf.util.meta.CopiedUtilityFlag;
import com.nh.nsight.tcf.util.meta.UtilCategory;

/**
 * tcf-util {@code tpcutil.escapeJson} 복사본.
 */
@CopiedFrom(module = "tcf-util", sourceClass = "tpcutil", category = UtilCategory.JSON)
public final class TpcJsonEscapeUtils implements CopiedUtilityFlag {

    public static final String COPIED_FROM_MODULE = "tcf-util";
    public static final String COPIED_FROM_CLASS = "tpcutil";

    private TpcJsonEscapeUtils() {
    }

    public static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
