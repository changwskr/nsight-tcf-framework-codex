package com.nh.nsight.marketing.ep.entry.facade;

import com.nh.nsight.marketing.ep.application.dto.sample.SampleInquiryRequest;
import com.nh.nsight.marketing.ep.application.service.EpSampleService;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EpSampleFacade {
    private final EpSampleService service;

    public EpSampleFacade(EpSampleService service) {
        this.service = service;
    }

    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        SampleInquiryRequest request = SampleInquiryRequest.fromMap(body);
        return service.inquiry(request, context).toMap();
    }
}
