package com.nh.nsight.marketing.ic.entry.facade;

import com.nh.nsight.marketing.ic.application.dto.sample.SampleInquiryRequest;
import com.nh.nsight.marketing.ic.application.service.IcSampleService;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IcSampleFacade {
    private final IcSampleService service;

    public IcSampleFacade(IcSampleService service) {
        this.service = service;
    }

    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        SampleInquiryRequest request = SampleInquiryRequest.fromMap(body);
        return service.inquiry(request, context).toMap();
    }
}
