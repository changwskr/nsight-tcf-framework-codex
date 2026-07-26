package com.nh.nsight.marketing.oc.entry.facade;

import com.nh.nsight.marketing.oc.application.dto.hello.HelloInquiryRequest;
import com.nh.nsight.marketing.oc.application.service.OcHelloService;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OcHelloFacade {
    private final OcHelloService service;

    public OcHelloFacade(OcHelloService service) {
        this.service = service;
    }

    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        HelloInquiryRequest request = HelloInquiryRequest.fromMap(body);
        return service.inquiry(request, context).toMap();
    }
}
