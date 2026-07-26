package com.nh.nsight.tcf.util.logging;

import com.nh.nsight.tcf.util.meta.CopiedFrom;
import com.nh.nsight.tcf.util.meta.CopiedUtilityFlag;
import com.nh.nsight.tcf.util.meta.UtilCategory;

/**
 * tcf-gateway {@code GatewayProxyTrace} 복사본.
 */
@CopiedFrom(module = "tcf-gateway", sourceClass = "GatewayProxyTrace", category = UtilCategory.LOGGING)
public final class GatewayProxyTraceUtils implements CopiedUtilityFlag {

    public static final String COPIED_FROM_MODULE = "tcf-gateway";
    public static final String COPIED_FROM_CLASS = "GatewayProxyTrace";

    private GatewayProxyTraceUtils() {
    }

    public static void start(String phase) {
        System.out.println("\n ======================================================================[" + phase + "] start");
    }

    public static void log(String phase, String detail) {
        System.out.println(" ======================================================================[" + phase + "] " + detail);
    }

    public static void end(String phase) {
        System.out.println(" ======================================================================[" + phase + "] end");
    }
}
