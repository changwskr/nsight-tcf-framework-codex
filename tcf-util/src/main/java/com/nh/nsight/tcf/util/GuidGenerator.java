package com.nh.nsight.tcf.util;

import com.nh.nsight.tcf.util.meta.CopiedFrom;
import com.nh.nsight.tcf.util.meta.CopiedUtilityFlag;
import com.nh.nsight.tcf.util.meta.UtilCategory;
import java.util.UUID;

@CopiedFrom(module = "tcf-util", sourceClass = "GuidGenerator", category = UtilCategory.ID, nativeUtility = true)
public final class GuidGenerator implements CopiedUtilityFlag {

    public static final String COPIED_FROM_MODULE = "tcf-util";
    public static final String COPIED_FROM_CLASS = "GuidGenerator";
    private GuidGenerator() {}

    public static String newGuid() {
        return UUID.randomUUID().toString();
    }

    public static String newTraceId() {
        return "trc-" + UUID.randomUUID();
    }
}
