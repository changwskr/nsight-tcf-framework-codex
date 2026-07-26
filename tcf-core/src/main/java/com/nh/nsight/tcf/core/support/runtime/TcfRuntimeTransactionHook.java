package com.nh.nsight.tcf.core.support.runtime;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.message.StandardHeader;
import com.nh.nsight.tcf.core.support.runtime.model.ActiveTransactionInfo;
import com.nh.nsight.tcf.core.support.runtime.model.RuntimeTransactionStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TcfRuntimeTransactionHook {
    private static final Logger log = LoggerFactory.getLogger(TcfRuntimeTransactionHook.class);

    private final TcfProperties properties;
    private final ActiveTransactionRegistry registry;
    private final SlowTransactionTracker slowTransactionTracker;

    public TcfRuntimeTransactionHook(
            TcfProperties properties,
            ActiveTransactionRegistry registry,
            SlowTransactionTracker slowTransactionTracker) {
        this.properties = properties;
        this.registry = registry;
        this.slowTransactionTracker = slowTransactionTracker;
    }

    public void onTransactionStart(TransactionContext context) {
        if (!properties.isRuntimeMonitorEnabled() || context == null) {
            return;
        }
        try {
            StandardHeader header = context.getHeader();
            if (header == null) {
                return;
            }
            Thread current = Thread.currentThread();
            registry.register(new ActiveTransactionInfo(
                    header.getGuid(),
                    header.getTraceId(),
                    header.getBusinessCode(),
                    header.getServiceId(),
                    header.getTransactionCode(),
                    current.getName(),
                    current.threadId(),
                    System.currentTimeMillis(),
                    RuntimeTransactionStep.STF,
                    null,
                    0L,
                    null));
            registry.updateStep(header.getGuid(), RuntimeTransactionStep.HANDLER);
        } catch (Exception e) {
            log.debug("runtime monitor register skipped: {}", e.getMessage());
        }
    }

    public void onTransactionEnd(TransactionContext context) {
        if (!properties.isRuntimeMonitorEnabled() || context == null) {
            return;
        }
        try {
            StandardHeader header = context.getHeader();
            if (header == null || header.getGuid() == null) {
                return;
            }
            String guid = header.getGuid();
            registry.updateStep(guid, RuntimeTransactionStep.COMPLETED);
            for (ActiveTransactionInfo info : registry.snapshot()) {
                if (guid.equals(info.guid())) {
                    slowTransactionTracker.onComplete(info);
                    break;
                }
            }
            registry.remove(guid);
        } catch (Exception e) {
            log.debug("runtime monitor unregister skipped: {}", e.getMessage());
        }
    }
}
