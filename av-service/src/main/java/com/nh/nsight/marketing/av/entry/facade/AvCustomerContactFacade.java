package com.nh.nsight.marketing.av.entry.facade;

import com.nh.nsight.marketing.av.application.dto.customercontact.CustomerContactSelectDetailRequest;
import com.nh.nsight.marketing.av.application.dto.customercontact.CustomerContactSelectListRequest;
import com.nh.nsight.marketing.av.application.service.AvCustomerContactService;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvCustomerContactFacade {
    private final AvCustomerContactService service;

    public AvCustomerContactFacade(AvCustomerContactService service) {
        this.service = service;
    }

    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> selectList(Map<String, Object> body, TransactionContext context) {
        CustomerContactSelectListRequest request = CustomerContactSelectListRequest.fromMap(body);
        return service.selectList(request, context).toMap();
    }

    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> selectDetail(Map<String, Object> body, TransactionContext context) {
        CustomerContactSelectDetailRequest request = CustomerContactSelectDetailRequest.fromMap(body);
        return service.selectDetail(request, context).toMap();
    }
}
