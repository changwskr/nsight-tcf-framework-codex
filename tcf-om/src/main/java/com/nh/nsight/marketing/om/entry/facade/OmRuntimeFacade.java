package com.nh.nsight.marketing.om.entry.facade;

import com.nh.nsight.marketing.om.application.service.OmRuntimeService;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OmRuntimeFacade {
    private final OmRuntimeService service;

    public OmRuntimeFacade(OmRuntimeService service) {
        this.service = service;
    }

    @Transactional(readOnly = true, timeout = 30)
    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        return service.inquiry(body, context);
    }
}
