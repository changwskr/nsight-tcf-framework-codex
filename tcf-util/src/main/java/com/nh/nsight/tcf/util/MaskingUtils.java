package com.nh.nsight.tcf.util;

import com.nh.nsight.tcf.util.meta.CopiedFrom;
import com.nh.nsight.tcf.util.meta.CopiedUtilityFlag;
import com.nh.nsight.tcf.util.meta.UtilCategory;

@CopiedFrom(module = "tcf-util", sourceClass = "MaskingUtils", category = UtilCategory.MASKING, nativeUtility = true)
public final class MaskingUtils implements CopiedUtilityFlag {

    public static final String COPIED_FROM_MODULE = "tcf-util";
    public static final String COPIED_FROM_CLASS = "MaskingUtils";
    private MaskingUtils() {}

    public static String maskCustomerId(String value) {
        if (value == null || value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }

    public static String maskAccountNo(String value) {
        if (value == null || value.length() <= 6) {
            return "****";
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 3);
    }
}
