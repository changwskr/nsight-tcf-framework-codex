package com.nh.nsight.tcf.util.logging;

import com.nh.nsight.tcf.util.meta.CopiedFrom;
import com.nh.nsight.tcf.util.meta.CopiedUtilityFlag;
import com.nh.nsight.tcf.util.meta.UtilCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * tcf-core {@code TcfConsoleLog} 복사본.
 * TCF 파이프라인 디버그 출력 — SLF4J logger {@code tcf.console}.
 */
@CopiedFrom(module = "tcf-core", sourceClass = "TcfConsoleLog", category = UtilCategory.LOGGING)
public final class CoreTcfConsoleLog implements CopiedUtilityFlag {

    public static final String COPIED_FROM_MODULE = "tcf-core";
    public static final String COPIED_FROM_CLASS = "TcfConsoleLog";

    private static final Logger LOG = LoggerFactory.getLogger("tcf.console");
    private static final String SEPARATOR = "==========================================";

    private CoreTcfConsoleLog() {
    }

    public static void println(String message) {
        emit(message);
    }

    public static void separator() {
        LOG.info(SEPARATOR);
    }

    public static void boundary(String component, String method, String phase) {
        emit("[" + component + "] " + method + " | " + phase);
    }

    public static void boundary(String component, String method, String phase, String detail) {
        emit("[" + component + "] " + method + " | " + phase + " — " + detail);
    }

    public static void step(String component, String method, String action) {
        emit("[" + component + "] " + method + " → " + action);
    }

    public static void step(String component, String method, String action, String detail) {
        emit("[" + component + "] " + method + " → " + action + " (" + detail + ")");
    }

    public static void detail(String message) {
        emit("    " + message);
    }

    private static void emit(String message) {
        LOG.info("{} {}", SEPARATOR, message);
    }
}
