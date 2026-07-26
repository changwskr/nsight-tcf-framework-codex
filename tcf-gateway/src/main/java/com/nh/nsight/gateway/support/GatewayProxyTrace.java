package com.nh.nsight.gateway.support;

public final class GatewayProxyTrace {
    private GatewayProxyTrace() {
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
