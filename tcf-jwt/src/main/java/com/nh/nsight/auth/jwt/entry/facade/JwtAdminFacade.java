package com.nh.nsight.auth.jwt.entry.facade;

import com.nh.nsight.auth.jwt.application.service.JwtAdminService;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JwtAdminFacade {
    private final JwtAdminService service;

    public JwtAdminFacade(JwtAdminService service) {
        this.service = service;
    }

    @Transactional(timeout = 5, readOnly = true)
    public Map<String, Object> inquiryTokens(Map<String, Object> body, TransactionContext context) {
        return service.inquiryTokens(body, context);
    }

    @Transactional(timeout = 5)
    public Map<String, Object> revokeTokenByJti(Map<String, Object> body, TransactionContext context) {
        return service.revokeTokenByJti(body, context);
    }

    @Transactional(timeout = 5, readOnly = true)
    public Map<String, Object> inquiryLoginHistory(Map<String, Object> body, TransactionContext context) {
        return service.inquiryLoginHistory(body, context);
    }

    @Transactional(timeout = 5, readOnly = true)
    public Map<String, Object> inquiryRefreshTokens(Map<String, Object> body, TransactionContext context) {
        return service.inquiryRefreshTokens(body, context);
    }

    @Transactional(timeout = 5, readOnly = true)
    public Map<String, Object> inquirySecurityPolicy(Map<String, Object> body, TransactionContext context) {
        return service.inquirySecurityPolicy(body, context);
    }

    @Transactional(timeout = 5)
    public Map<String, Object> updateSecurityPolicy(Map<String, Object> body, TransactionContext context) {
        return service.updateSecurityPolicy(body, context);
    }
}
