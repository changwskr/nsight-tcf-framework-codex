package com.nh.nsight.marketing.om.entry.facade;

import com.nh.nsight.marketing.om.application.service.OmSessionService;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OmSessionFacade {
    private final OmSessionService service;

    public OmSessionFacade(OmSessionService service) {
        this.service = service;
    }

    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        return service.inquiry(body, context);
    }

    @Transactional(timeout = 5)
    public Map<String, Object> delete(Map<String, Object> body, TransactionContext context) {
        return service.delete(body, context);
    }
}
