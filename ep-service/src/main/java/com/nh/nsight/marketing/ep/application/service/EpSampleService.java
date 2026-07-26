package com.nh.nsight.marketing.ep.application.service;

import com.nh.nsight.marketing.ep.application.dto.sample.SampleInquiryRequest;
import com.nh.nsight.marketing.ep.application.dto.sample.SampleInquiryResponse;
import com.nh.nsight.marketing.ep.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.ep.application.rule.EpSampleRule;
import com.nh.nsight.marketing.ep.persistence.dao.EpSampleDao;
import com.nh.nsight.marketing.ep.persistence.dto.sample.SampleRow;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import org.springframework.stereotype.Service;

@Service
public class EpSampleService {
    private final EpSampleRule rule;
    private final EpSampleDao dao;

    public EpSampleService(EpSampleRule rule, EpSampleDao dao) {
        this.rule = rule;
        this.dao = dao;
    }

    public SampleInquiryResponse inquiry(SampleInquiryRequest request, TransactionContext context) {
        rule.validateInquiry(request);
        SampleSearchCriteria criteria = rule.buildSearchCriteria(request);
        SampleRow row = dao.selectSample(criteria);
        return SampleInquiryResponse.of(context, row);
    }
}
