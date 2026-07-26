package com.nh.nsight.marketing.av.entry.facade;

import com.nh.nsight.marketing.av.application.dto.sample.SampleInquiryRequest;
import com.nh.nsight.marketing.av.application.service.AvSampleService;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvSampleFacade {
    private final AvSampleService service;

    public AvSampleFacade(AvSampleService service) {
        this.service = service;
    }

    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        SampleInquiryRequest request = SampleInquiryRequest.fromMap(body);
        return service.inquiry(request, context).toMap();
    }
}
