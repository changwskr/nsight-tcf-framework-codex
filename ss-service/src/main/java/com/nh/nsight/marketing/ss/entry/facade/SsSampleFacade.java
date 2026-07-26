package com.nh.nsight.marketing.ss.entry.facade;

import com.nh.nsight.marketing.ss.application.dto.sample.SampleInquiryRequest;
import com.nh.nsight.marketing.ss.application.service.SsSampleService;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SsSampleFacade {
    private final SsSampleService service;
    public SsSampleFacade(SsSampleService service) { this.service = service; }
    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        SampleInquiryRequest request = SampleInquiryRequest.fromMap(body);
        return service.inquiry(request, context).toMap();
    }
}
