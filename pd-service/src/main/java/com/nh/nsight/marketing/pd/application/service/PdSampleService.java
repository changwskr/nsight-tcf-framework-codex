package com.nh.nsight.marketing.pd.application.service;

import com.nh.nsight.marketing.pd.application.dto.sample.SampleInquiryRequest;
import com.nh.nsight.marketing.pd.application.dto.sample.SampleInquiryResponse;
import com.nh.nsight.marketing.pd.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.pd.application.rule.PdSampleRule;
import com.nh.nsight.marketing.pd.persistence.dao.PdSampleDao;
import com.nh.nsight.marketing.pd.persistence.dto.sample.SampleRow;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import org.springframework.stereotype.Service;

@Service
public class PdSampleService {
    private final PdSampleRule rule;
    private final PdSampleDao dao;
    public PdSampleService(PdSampleRule rule, PdSampleDao dao) { this.rule = rule; this.dao = dao; }
    public SampleInquiryResponse inquiry(SampleInquiryRequest request, TransactionContext context) {
        rule.validateInquiry(request);
        SampleSearchCriteria criteria = rule.buildSearchCriteria(request);
        SampleRow row = dao.selectSample(criteria);
        return SampleInquiryResponse.of(context, row);
    }
}
