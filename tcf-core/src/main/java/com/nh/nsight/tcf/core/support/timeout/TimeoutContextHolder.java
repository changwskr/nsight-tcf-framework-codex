package com.nh.nsight.tcf.core.support.timeout;

/** STF에서 조회한 TimeoutPolicy를 Facade/Service에서 참조하기 위한 ThreadLocal */
public final class TimeoutContextHolder {
    private static final ThreadLocal<TimeoutPolicy> HOLDER = new ThreadLocal<>();

    private TimeoutContextHolder() {}

    public static void set(TimeoutPolicy policy) {
        HOLDER.set(policy);
    }

    public static TimeoutPolicy get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
