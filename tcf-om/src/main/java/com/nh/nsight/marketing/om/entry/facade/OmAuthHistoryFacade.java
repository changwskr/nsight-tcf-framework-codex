package com.nh.nsight.marketing.om.entry.facade;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.marketing.om.application.service.OmAuthHistoryService;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OmAuthHistoryFacade {
    private final OmAuthHistoryService service;

    public OmAuthHistoryFacade(OmAuthHistoryService service) {
        this.service = service;
    }

    @Transactional(readOnly = true, timeout = 10)
    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        return service.inquiry(body, context);
    }

    @Transactional(timeout = 30)
    public Map<String, Object> deleteAll(Map<String, Object> body, TransactionContext context) {
        return service.deleteAll(body, context);
    }
}


