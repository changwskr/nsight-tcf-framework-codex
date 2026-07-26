package com.nh.nsight.tcf.core.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TCF 파이프라인 디버그 출력 — SLF4J logger {@code tcf.console}.
 */
public final class TcfConsoleLog {

    private static final Logger LOG = LoggerFactory.getLogger("tcf.console");

    /** 모든 TCF 콘솔 로그 줄 앞에 붙는 공통 구분선. */
    private static final String SEPARATOR = "==========================================";

    private TcfConsoleLog() {
    }

    /** 단일 줄 출력 (레거시·자유 형식). */
    public static void println(String message) {
        emit(message);
    }

    /** 구분선만 단독 출력 (빈 줄 대신 시각적 경계). */
    public static void separator() {
        LOG.info(SEPARATOR);
    }

    /** 단계 경계 — START / END / END (사유). */
    public static void boundary(String component, String method, String phase) {
        emit("[" + component + "] " + method + " | " + phase);
    }

    public static void boundary(String component, String method, String phase, String detail) {
        emit("[" + component + "] " + method + " | " + phase + " — " + detail);
    }

    /** 파이프라인 내부 단계. */
    public static void step(String component, String method, String action) {
        emit("[" + component + "] " + method + " → " + action);
    }

    public static void step(String component, String method, String action, String detail) {
        emit("[" + component + "] " + method + " → " + action + " (" + detail + ")");
    }

    /** 들여쓰기 상세 (전문 header/body 등). */
    public static void detail(String message) {
        emit("    " + message);
    }

    private static void emit(String message) {
        LOG.info("{} {}", SEPARATOR, message);
    }
}
