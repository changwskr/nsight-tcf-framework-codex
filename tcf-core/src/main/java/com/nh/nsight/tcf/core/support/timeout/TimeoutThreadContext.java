package com.nh.nsight.tcf.core.support.timeout;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.context.TransactionContextHolder;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/** 온라인 timeout 워커 스레드로 Timeout·Transaction·MDC·HTTP 요청 컨텍스트를 전파한다. */
public final class TimeoutThreadContext {

    private TimeoutThreadContext() {}

    public static Snapshot capture() {
        return new Snapshot(
                TimeoutContextHolder.get(),
                TransactionContextHolder.get(),
                MDC.getCopyOfContextMap(),
                RequestContextHolder.getRequestAttributes());
    }

    public static <T> T runWithSnapshot(Snapshot snapshot, Supplier<T> action) {
        try {
            if (snapshot.timeoutPolicy() != null) {
                TimeoutContextHolder.set(snapshot.timeoutPolicy());
            }
            if (snapshot.transactionContext() != null) {
                TransactionContextHolder.set(snapshot.transactionContext());
            }
            if (snapshot.mdc() != null) {
                MDC.setContextMap(snapshot.mdc());
            }
            if (snapshot.requestAttributes() != null) {
                RequestContextHolder.setRequestAttributes(snapshot.requestAttributes(), true);
            }
            return action.get();
        } finally {
            RequestContextHolder.resetRequestAttributes();
            TimeoutContextHolder.clear();
            TransactionContextHolder.clear();
            MDC.clear();
        }
    }

    public record Snapshot(
            TimeoutPolicy timeoutPolicy,
            TransactionContext transactionContext,
            Map<String, String> mdc,
            RequestAttributes requestAttributes) {
    }
}
