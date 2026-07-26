package com.nh.nsight.marketing.sv.entry.facade;

import com.nh.nsight.marketing.sv.application.dto.customer.CustomerSummaryRequest;
import com.nh.nsight.marketing.sv.application.service.SvCustomerService;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SvCustomerFacade {

    private final SvCustomerService service;

    public SvCustomerFacade(SvCustomerService service) {
        this.service = service;
    }

    @Transactional(readOnly = true, timeout = 3)
    public Map<String, Object> selectCustomerSummary(Map<String, Object> body, TransactionContext context) {
        CustomerSummaryRequest inquiryRequest = CustomerSummaryRequest.fromMap(body);
        return service.selectCustomerSummary(inquiryRequest, context).toMap();
    }
}
