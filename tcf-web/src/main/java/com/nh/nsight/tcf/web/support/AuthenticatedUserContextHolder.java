package com.nh.nsight.tcf.web.support;

public final class AuthenticatedUserContextHolder {
    private static final ThreadLocal<AuthenticatedUserContext> HOLDER = new ThreadLocal<>();

    private AuthenticatedUserContextHolder() {
    }

    public static void set(AuthenticatedUserContext context) {
        HOLDER.set(context);
    }

    public static AuthenticatedUserContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
