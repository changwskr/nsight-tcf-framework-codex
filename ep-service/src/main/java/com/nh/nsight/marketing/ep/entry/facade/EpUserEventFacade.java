package com.nh.nsight.marketing.ep.entry.facade;

import com.nh.nsight.marketing.ep.application.dto.userevent.UserEventInquiryRequest;
import com.nh.nsight.marketing.ep.application.dto.userevent.UserEventReceiveRequest;
import com.nh.nsight.marketing.ep.application.service.EpUserEventService;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EpUserEventFacade {
    private final EpUserEventService service;

    public EpUserEventFacade(EpUserEventService service) {
        this.service = service;
    }

    @Transactional(timeout = 5)
    public Map<String, Object> receive(Map<String, Object> body, TransactionContext context) {
        UserEventReceiveRequest request = UserEventReceiveRequest.fromMap(body);
        return service.receive(request, context).toMap();
    }

    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        UserEventInquiryRequest request = UserEventInquiryRequest.fromMap(body);
        return service.inquiry(request, context).toMap();
    }
}
