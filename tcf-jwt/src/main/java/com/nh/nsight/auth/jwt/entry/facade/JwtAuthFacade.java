package com.nh.nsight.auth.jwt.entry.facade;

import com.nh.nsight.auth.jwt.application.service.JwtAuthService;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JwtAuthFacade {
    private final JwtAuthService service;

    public JwtAuthFacade(JwtAuthService service) {
        this.service = service;
    }

    @Transactional(timeout = 5)
    public Map<String, Object> login(Map<String, Object> body, TransactionContext context) {
        return service.login(body, context);
    }

    @Transactional(timeout = 5)
    public Map<String, Object> ssoIssue(Map<String, Object> body, TransactionContext context) {
        return service.ssoIssue(body, context);
    }

    @Transactional(timeout = 5)
    public Map<String, Object> refresh(Map<String, Object> body, TransactionContext context) {
        return service.refresh(body, context);
    }

    @Transactional(timeout = 5)
    public Map<String, Object> revoke(Map<String, Object> body, TransactionContext context) {
        return service.revoke(body, context);
    }

    @Transactional(timeout = 5)
    public Map<String, Object> logout(Map<String, Object> body, TransactionContext context) {
        return service.logout(body, context);
    }
}
