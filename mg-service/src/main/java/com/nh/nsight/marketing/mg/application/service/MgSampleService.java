package com.nh.nsight.marketing.mg.application.service;

import com.nh.nsight.marketing.mg.application.dto.sample.SampleInquiryRequest;
import com.nh.nsight.marketing.mg.application.dto.sample.SampleInquiryResponse;
import com.nh.nsight.marketing.mg.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.mg.application.rule.MgSampleRule;
import com.nh.nsight.marketing.mg.persistence.dao.MgSampleDao;
import com.nh.nsight.marketing.mg.persistence.dto.sample.SampleRow;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import org.springframework.stereotype.Service;

@Service
public class MgSampleService {
    private final MgSampleRule rule;
    private final MgSampleDao dao;
    public MgSampleService(MgSampleRule rule, MgSampleDao dao) { this.rule = rule; this.dao = dao; }
    public SampleInquiryResponse inquiry(SampleInquiryRequest request, TransactionContext context) {
        rule.validateInquiry(request);
        SampleSearchCriteria criteria = rule.buildSearchCriteria(request);
        SampleRow row = dao.selectSample(criteria);
        return SampleInquiryResponse.of(context, row);
    }
}
