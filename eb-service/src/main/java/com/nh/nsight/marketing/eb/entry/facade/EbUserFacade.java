package com.nh.nsight.marketing.eb.entry.facade;

import com.nh.nsight.marketing.eb.application.dto.user.UserCreateRequest;
import com.nh.nsight.marketing.eb.application.dto.user.UserInquiryRequest;
import com.nh.nsight.marketing.eb.application.service.EbUserService;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EbUserFacade {
    private final EbUserService service;

    public EbUserFacade(EbUserService service) {
        this.service = service;
    }

    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        UserInquiryRequest request = UserInquiryRequest.fromMap(body);
        return service.inquiry(request, context).toMap();
    }

    @Transactional(timeout = 5)
    public Map<String, Object> create(Map<String, Object> body, TransactionContext context) {
        UserCreateRequest request = UserCreateRequest.fromMap(body);
        return service.create(request, context).toMap();
    }
}
