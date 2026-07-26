package com.nh.nsight.marketing.mg.entry.facade;

import com.nh.nsight.marketing.mg.application.dto.sample.SampleInquiryRequest;
import com.nh.nsight.marketing.mg.application.service.MgSampleService;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MgSampleFacade {
    private final MgSampleService service;
    public MgSampleFacade(MgSampleService service) { this.service = service; }
    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        SampleInquiryRequest request = SampleInquiryRequest.fromMap(body);
        return service.inquiry(request, context).toMap();
    }
}
