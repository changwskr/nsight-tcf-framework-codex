package com.nh.nsight.tcf.core.support.timeout;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.message.StandardHeader;
import com.nh.nsight.tcf.core.support.TcfConsoleLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TimeoutPolicyService {
    private static final Logger log = LoggerFactory.getLogger(TimeoutPolicyService.class);

    private final TcfProperties properties;
    private final TimeoutPolicyResolver resolver;

    public TimeoutPolicyService(TcfProperties properties, TimeoutPolicyResolver resolver) {
        this.properties = properties;
        this.resolver = resolver;
    }

    public TimeoutPolicy resolveAndApply(StandardHeader header, TransactionContext context) {
        if (!properties.isTimeoutPolicyEnabled()) {
            TimeoutPolicy defaults = resolver.resolve(header);
            bindContext(context, defaults);
            return defaults;
        }
        TcfConsoleLog.boundary("TimeoutPolicyService", "resolve", "START");
        TimeoutPolicy policy = resolver.resolve(header);
        bindContext(context, policy);
        log.info("TIMEOUT_POLICY serviceId={} transactionCode={} businessCode={} onlineSec={} txSec={} dbQuerySec={} action={}",
                policy.getServiceId(), policy.getTransactionCode(), policy.getBusinessCode(),
                policy.getOnlineTimeoutSec(), policy.getTxTimeoutSec(),
                policy.getDbQueryTimeoutSec(), policy.getTimeoutAction());
        TcfConsoleLog.boundary("TimeoutPolicyService", "resolve", "END");
        return policy;
    }

    private void bindContext(TransactionContext context, TimeoutPolicy policy) {
        TimeoutContextHolder.set(policy);
        if (context != null) {
            context.put(TcfServiceTimeoutConstants.CONTEXT_ATTR, policy);
        }
    }
}
