package com.nh.nsight.marketing.om.entry.facade;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.marketing.om.application.service.OmDashboardService;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OmDashboardFacade {
    private final OmDashboardService service;

    public OmDashboardFacade(OmDashboardService service) {
        this.service = service;
    }

    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        return service.inquiry(body, context);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Map<String, Object> reset(Map<String, Object> body, TransactionContext context) {
        return service.reset(body, context);
    }
}


