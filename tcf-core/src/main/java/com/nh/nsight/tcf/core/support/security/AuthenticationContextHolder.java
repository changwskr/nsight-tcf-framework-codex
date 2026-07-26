package com.nh.nsight.tcf.core.support.security;

public final class AuthenticationContextHolder {
    private static final ThreadLocal<AuthenticationContext> HOLDER = new ThreadLocal<>();

    private AuthenticationContextHolder() {
    }

    public static void set(AuthenticationContext context) {
        HOLDER.set(context);
    }

    public static AuthenticationContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
