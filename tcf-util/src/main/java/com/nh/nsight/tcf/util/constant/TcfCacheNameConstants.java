package com.nh.nsight.tcf.util.constant;

import com.nh.nsight.tcf.util.meta.CopiedFrom;
import com.nh.nsight.tcf.util.meta.CopiedUtilityFlag;
import com.nh.nsight.tcf.util.meta.UtilCategory;

/**
 * tcf-cache {@code TcfCacheNames} 복사본.
 */
@CopiedFrom(module = "tcf-cache", sourceClass = "TcfCacheNames", category = UtilCategory.CONSTANT)
public final class TcfCacheNameConstants implements CopiedUtilityFlag {

    public static final String COPIED_FROM_MODULE = "tcf-cache";
    public static final String COPIED_FROM_CLASS = "TcfCacheNames";

    public static final String COMMON_CODE = "commonCode";
    public static final String SERVICE_CATALOG = "serviceCatalog";
    public static final String SESSION_REGION = "sessionRegion";

    private TcfCacheNameConstants() {
    }
}
