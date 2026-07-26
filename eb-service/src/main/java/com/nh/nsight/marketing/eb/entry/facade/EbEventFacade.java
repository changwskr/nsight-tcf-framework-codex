package com.nh.nsight.marketing.eb.entry.facade;

import com.nh.nsight.marketing.eb.application.dto.event.EventInquiryRequest;
import com.nh.nsight.marketing.eb.application.service.EbEventService;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EbEventFacade {
    private final EbEventService service;

    public EbEventFacade(EbEventService service) {
        this.service = service;
    }

    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        EventInquiryRequest request = EventInquiryRequest.fromMap(body);
        return service.inquiry(request, context).toMap();
    }
}
