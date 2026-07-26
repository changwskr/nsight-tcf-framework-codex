package com.nh.nsight.marketing.om.application.service;

import com.nh.nsight.marketing.om.application.dto.sample.SampleInquiryRequest;
import com.nh.nsight.marketing.om.application.dto.sample.SampleInquiryResponse;
import com.nh.nsight.marketing.om.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.om.application.rule.OmSampleRule;
import com.nh.nsight.marketing.om.persistence.dao.OmSampleDao;
import com.nh.nsight.marketing.om.persistence.dto.sample.SampleRow;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import org.springframework.stereotype.Service;

@Service
public class OmSampleService {
    private final OmSampleRule rule;
    private final OmSampleDao dao;
    public OmSampleService(OmSampleRule rule, OmSampleDao dao) { this.rule = rule; this.dao = dao; }
    public SampleInquiryResponse inquiry(SampleInquiryRequest request, TransactionContext context) {
        rule.validateInquiry(request);
        SampleSearchCriteria criteria = rule.buildSearchCriteria(request);
        SampleRow row = dao.selectSample(criteria);
        return SampleInquiryResponse.of(context, row);
    }
}
