package com.nh.nsight.marketing.eb.entry.facade;

import com.nh.nsight.marketing.eb.application.dto.systemtx.SystemTxInquiryRequest;
import com.nh.nsight.marketing.eb.application.service.EbSystemTxService;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EbSystemTxFacade {
    private final EbSystemTxService service;

    public EbSystemTxFacade(EbSystemTxService service) {
        this.service = service;
    }

    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        SystemTxInquiryRequest request = SystemTxInquiryRequest.fromMap(body);
        return service.inquiry(request, context).toMap();
    }
}
