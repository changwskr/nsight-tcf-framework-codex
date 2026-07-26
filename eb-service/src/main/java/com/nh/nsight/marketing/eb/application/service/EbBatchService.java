package com.nh.nsight.marketing.eb.application.service;

import com.nh.nsight.marketing.eb.application.dto.batch.BatchInquiryResponse;
import com.nh.nsight.marketing.eb.config.EbEventPublishProperties;
import com.nh.nsight.marketing.eb.persistence.dao.EbEventDao;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import org.springframework.stereotype.Service;

@Service
public class EbBatchService {
    private final EbEventPublishProperties properties;
    private final EbEventDao eventDao;

    public EbBatchService(EbEventPublishProperties properties, EbEventDao eventDao) {
        this.properties = properties;
        this.eventDao = eventDao;
    }

    public BatchInquiryResponse inquiry(TransactionContext context) {
        return BatchInquiryResponse.of(context, properties, eventDao.countEventsByStatus());
    }
}
