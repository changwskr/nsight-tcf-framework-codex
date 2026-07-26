package com.nh.nsight.tcf.core.support.metrics;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TransactionMetricService {
    private static final Logger log = LoggerFactory.getLogger(TransactionMetricService.class);

    public void record(TransactionContext context, String resultCode) {
        if (context == null) {
            return;
        }
        log.debug("TCF_METRIC serviceId={} resultCode={} elapsedMs={}",
                context.getHeader().getServiceId(), resultCode, context.elapsedMillis());
    }
}
