package com.nh.nsight.marketing.pc.application.service;

import com.nh.nsight.marketing.pc.application.dto.sample.SampleInquiryRequest;
import com.nh.nsight.marketing.pc.application.dto.sample.SampleInquiryResponse;
import com.nh.nsight.marketing.pc.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.pc.application.rule.PcSampleRule;
import com.nh.nsight.marketing.pc.persistence.dao.PcSampleDao;
import com.nh.nsight.marketing.pc.persistence.dto.sample.SampleRow;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import org.springframework.stereotype.Service;

@Service
public class PcSampleService {
    private final PcSampleRule rule;
    private final PcSampleDao dao;
    public PcSampleService(PcSampleRule rule, PcSampleDao dao) { this.rule = rule; this.dao = dao; }
    public SampleInquiryResponse inquiry(SampleInquiryRequest request, TransactionContext context) {
        rule.validateInquiry(request);
        SampleSearchCriteria criteria = rule.buildSearchCriteria(request);
        SampleRow row = dao.selectSample(criteria);
        return SampleInquiryResponse.of(context, row);
    }
}
