package com.nh.nsight.marketing.om.entry.facade;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.marketing.om.application.service.OmTimeoutPolicyService;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OmTimeoutPolicyFacade {
    private final OmTimeoutPolicyService service;

    public OmTimeoutPolicyFacade(OmTimeoutPolicyService service) {
        this.service = service;
    }

    @Transactional(readOnly = true, timeout = 10)
    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        return service.inquiry(body, context);
    }

    @Transactional(timeout = 10)
    public Map<String, Object> save(Map<String, Object> body, TransactionContext context) {
        return service.save(body, context);
    }

    @Transactional(timeout = 10)
    public Map<String, Object> update(Map<String, Object> body, TransactionContext context) {
        return service.update(body, context);
    }

    @Transactional(timeout = 10)
    public Map<String, Object> delete(Map<String, Object> body, TransactionContext context) {
        return service.delete(body, context);
    }
}
