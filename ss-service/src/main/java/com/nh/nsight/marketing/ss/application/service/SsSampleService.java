package com.nh.nsight.marketing.ss.application.service;

import com.nh.nsight.marketing.ss.application.dto.sample.SampleInquiryRequest;
import com.nh.nsight.marketing.ss.application.dto.sample.SampleInquiryResponse;
import com.nh.nsight.marketing.ss.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.ss.application.rule.SsSampleRule;
import com.nh.nsight.marketing.ss.persistence.dao.SsSampleDao;
import com.nh.nsight.marketing.ss.persistence.dto.sample.SampleRow;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import org.springframework.stereotype.Service;

@Service
public class SsSampleService {
    private final SsSampleRule rule;
    private final SsSampleDao dao;
    public SsSampleService(SsSampleRule rule, SsSampleDao dao) { this.rule = rule; this.dao = dao; }
    public SampleInquiryResponse inquiry(SampleInquiryRequest request, TransactionContext context) {
        rule.validateInquiry(request);
        SampleSearchCriteria criteria = rule.buildSearchCriteria(request);
        SampleRow row = dao.selectSample(criteria);
        return SampleInquiryResponse.of(context, row);
    }
}
