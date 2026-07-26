package com.nh.nsight.marketing.eb.entry.facade;

import com.nh.nsight.marketing.eb.application.dto.sample.SampleInquiryRequest;
import com.nh.nsight.marketing.eb.application.service.EbSampleService;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EbSampleFacade {
    private final EbSampleService service;

    public EbSampleFacade(EbSampleService service) {
        this.service = service;
    }

    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        SampleInquiryRequest request = SampleInquiryRequest.fromMap(body);
        return service.inquiry(request, context).toMap();
    }
}
