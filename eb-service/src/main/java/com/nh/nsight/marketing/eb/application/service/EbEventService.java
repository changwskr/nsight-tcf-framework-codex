package com.nh.nsight.marketing.eb.application.service;

import com.nh.nsight.marketing.eb.application.dto.event.EventInquiryRequest;
import com.nh.nsight.marketing.eb.application.dto.event.EventInquiryResponse;
import com.nh.nsight.marketing.eb.application.dto.event.EventSearchCriteria;
import com.nh.nsight.marketing.eb.application.rule.EbEventRule;
import com.nh.nsight.marketing.eb.persistence.dao.EbEventDao;
import com.nh.nsight.marketing.eb.persistence.dto.event.EventRow;
import com.nh.nsight.marketing.eb.persistence.dto.event.EventStatusCountRow;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EbEventService {
    private final EbEventRule rule;
    private final EbEventDao eventDao;

    public EbEventService(EbEventRule rule, EbEventDao eventDao) {
        this.rule = rule;
        this.eventDao = eventDao;
    }

    public EventInquiryResponse inquiry(EventInquiryRequest request, TransactionContext context) {
        rule.validateInquiry(request);
        EventSearchCriteria criteria = rule.buildSearchCriteria(request);
        List<EventRow> rows = eventDao.searchEvents(criteria);
        int totalCount = eventDao.countEvents(criteria);
        List<EventStatusCountRow> statusRows = eventDao.countEventsByStatus();
        return EventInquiryResponse.of(context, criteria, rows, totalCount, statusRows);
    }
}
