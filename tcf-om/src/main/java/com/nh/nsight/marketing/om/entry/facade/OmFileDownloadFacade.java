package com.nh.nsight.marketing.om.entry.facade;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.marketing.om.application.service.OmFileDownloadService;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OmFileDownloadFacade {
    private final OmFileDownloadService service;

    public OmFileDownloadFacade(OmFileDownloadService service) {
        this.service = service;
    }

    @Transactional(readOnly = true, timeout = 10)
    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        return service.inquiry(body, context);
    }
}


