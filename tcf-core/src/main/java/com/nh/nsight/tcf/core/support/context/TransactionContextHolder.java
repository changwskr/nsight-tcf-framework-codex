package com.nh.nsight.tcf.core.support.context;

public final class TransactionContextHolder {
    private static final ThreadLocal<TransactionContext> HOLDER = new ThreadLocal<>();

    private TransactionContextHolder() {}

    public static void set(TransactionContext context) { HOLDER.set(context); }
    public static TransactionContext get() { return HOLDER.get(); }
    public static void clear() { HOLDER.remove(); }
}
