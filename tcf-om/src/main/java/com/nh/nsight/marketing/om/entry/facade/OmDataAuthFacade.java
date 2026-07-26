package com.nh.nsight.marketing.om.entry.facade;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.marketing.om.application.service.OmDataAuthService;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OmDataAuthFacade {
    private final OmDataAuthService service;

    public OmDataAuthFacade(OmDataAuthService service) {
        this.service = service;
    }

    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        return service.inquiry(body, context);
    }
}


