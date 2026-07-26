package com.nh.nsight.marketing.ms.application.service;

import com.nh.nsight.marketing.ms.application.dto.sample.SampleInquiryRequest;
import com.nh.nsight.marketing.ms.application.dto.sample.SampleInquiryResponse;
import com.nh.nsight.marketing.ms.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.ms.application.rule.MsSampleRule;
import com.nh.nsight.marketing.ms.persistence.dao.MsSampleDao;
import com.nh.nsight.marketing.ms.persistence.dto.sample.SampleRow;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import org.springframework.stereotype.Service;

@Service
public class MsSampleService {
    private final MsSampleRule rule;
    private final MsSampleDao dao;
    public MsSampleService(MsSampleRule rule, MsSampleDao dao) { this.rule = rule; this.dao = dao; }
    public SampleInquiryResponse inquiry(SampleInquiryRequest request, TransactionContext context) {
        rule.validateInquiry(request);
        SampleSearchCriteria criteria = rule.buildSearchCriteria(request);
        SampleRow row = dao.selectSample(criteria);
        return SampleInquiryResponse.of(context, row);
    }
}
