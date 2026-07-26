package com.nh.nsight.marketing.om.entry.facade;

import com.nh.nsight.marketing.om.application.service.OmServiceCatalogService;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OmServiceCatalogFacade {
    private final OmServiceCatalogService service;

    public OmServiceCatalogFacade(OmServiceCatalogService service) {
        this.service = service;
    }

    @Transactional(readOnly = true, timeout = 10)
    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        return service.inquiry(body, context);
    }

    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> detail(Map<String, Object> body, TransactionContext context) {
        return service.detail(body, context);
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
