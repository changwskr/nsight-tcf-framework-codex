package com.nh.nsight.auth.jwt.entry.handler;

import com.nh.nsight.auth.jwt.entry.facade.JwtAuthFacade;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.message.StandardRequest;
import com.nh.nsight.tcf.core.support.transaction.TransactionHandler;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthSsoIssueHandler implements TransactionHandler {
    private final JwtAuthFacade facade;

    public JwtAuthSsoIssueHandler(JwtAuthFacade facade) {
        this.facade = facade;
    }

    @Override
    public String serviceId() {
        return "JWT.Auth.ssoIssue";
    }

    @Override
    public Object doHandle(StandardRequest<Map<String, Object>> request, TransactionContext context) {
        return facade.ssoIssue(request.getBody(), context);
    }
}
