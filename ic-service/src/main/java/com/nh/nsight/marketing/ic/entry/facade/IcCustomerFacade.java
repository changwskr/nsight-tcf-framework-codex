package com.nh.nsight.marketing.ic.entry.facade;

import com.nh.nsight.marketing.ic.application.dto.customer.CustomerInquiryRequest;
import com.nh.nsight.marketing.ic.application.service.IcCustomerService;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * IC 고객 상세 조회 유스케이스 조립(트랜잭션 경계).
 */
@Service
public class IcCustomerFacade {

    private final IcCustomerService service;

    public IcCustomerFacade(IcCustomerService service) {
        this.service = service;
    }

    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> inquiryCustomerDetail(Map<String, Object> body, TransactionContext context) {
        CustomerInquiryRequest request = CustomerInquiryRequest.fromMap(body);
        return service.inquiryCustomerDetail(request, context).toMap();
    }
}
