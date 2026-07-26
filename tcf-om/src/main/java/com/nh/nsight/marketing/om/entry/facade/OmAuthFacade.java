package com.nh.nsight.marketing.om.entry.facade;

import com.nh.nsight.marketing.om.application.service.OmAuthService;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OmAuthFacade {
    private final OmAuthService service;

    public OmAuthFacade(OmAuthService service) {
        this.service = service;
    }

    @Transactional(timeout = 5)
    public Map<String, Object> login(Map<String, Object> body, TransactionContext context) {
        return service.login(body, context);
    }

    @Transactional(timeout = 5)
    public Map<String, Object> ssoLogin(Map<String, Object> body, TransactionContext context) {
        return service.ssoLogin(body, context);
    }

    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> session(Map<String, Object> body, TransactionContext context) {
        return service.session(context);
    }

    @Transactional(timeout = 5)
    public Map<String, Object> logout(Map<String, Object> body, TransactionContext context) {
        return service.logout(context);
    }
}
