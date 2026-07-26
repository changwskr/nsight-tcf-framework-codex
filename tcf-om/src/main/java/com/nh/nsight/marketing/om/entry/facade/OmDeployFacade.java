package com.nh.nsight.marketing.om.entry.facade;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.marketing.om.application.service.OmBuildService;
import com.nh.nsight.marketing.om.application.service.OmDeployService;
import com.nh.nsight.marketing.om.application.service.OmRollbackService;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OmDeployFacade {
    private final OmBuildService buildService;
    private final OmDeployService deployService;
    private final OmRollbackService rollbackService;

    public OmDeployFacade(OmBuildService buildService, OmDeployService deployService,
                          OmRollbackService rollbackService) {
        this.buildService = buildService;
        this.deployService = deployService;
        this.rollbackService = rollbackService;
    }

    @Transactional(timeout = 10)
    public Map<String, Object> buildRequest(Map<String, Object> body, TransactionContext context) {
        return buildService.buildRequest(body, context);
    }

    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> buildStatus(Map<String, Object> body, TransactionContext context) {
        return buildService.buildStatus(body, context);
    }

    @Transactional(timeout = 10)
    public Map<String, Object> deployRequest(Map<String, Object> body, TransactionContext context) {
        return deployService.deployRequest(body, context);
    }

    @Transactional(timeout = 10)
    public Map<String, Object> approve(Map<String, Object> body, TransactionContext context) {
        return deployService.approve(body, context);
    }

    @Transactional(timeout = 300)
    public Map<String, Object> execute(Map<String, Object> body, TransactionContext context) {
        return deployService.execute(body, context);
    }

    @Transactional(readOnly = true, timeout = 10)
    public Map<String, Object> history(Map<String, Object> body, TransactionContext context) {
        return deployService.history(body, context);
    }

    @Transactional(readOnly = true, timeout = 10)
    public Map<String, Object> logInquiry(Map<String, Object> body, TransactionContext context) {
        return deployService.logInquiry(body, context);
    }

    @Transactional(timeout = 10)
    public Map<String, Object> rollbackRequest(Map<String, Object> body, TransactionContext context) {
        return rollbackService.rollbackRequest(body, context);
    }

    @Transactional(readOnly = true, timeout = 10)
    public Map<String, Object> healthCheck(Map<String, Object> body, TransactionContext context) {
        return deployService.healthCheck(body, context);
    }

    @Transactional(timeout = 30)
    public Map<String, Object> deleteAll(Map<String, Object> body, TransactionContext context) {
        return deployService.deleteAll(body, context);
    }
}
