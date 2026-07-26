package com.nh.nsight.marketing.eb.entry.facade;

import com.nh.nsight.marketing.eb.application.service.EbBatchService;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EbBatchFacade {
    private final EbBatchService service;

    public EbBatchFacade(EbBatchService service) {
        this.service = service;
    }

    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        return service.inquiry(context).toMap();
    }
}
