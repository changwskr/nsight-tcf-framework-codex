package com.nh.nsight.marketing.ic.application.service;

import com.nh.nsight.marketing.ic.application.dto.sample.SampleInquiryRequest;
import com.nh.nsight.marketing.ic.application.dto.sample.SampleInquiryResponse;
import com.nh.nsight.marketing.ic.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.ic.application.rule.IcSampleRule;
import com.nh.nsight.marketing.ic.persistence.dao.IcSampleDao;
import com.nh.nsight.marketing.ic.persistence.dto.sample.SampleRow;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import org.springframework.stereotype.Service;

@Service
public class IcSampleService {
    private final IcSampleRule rule;
    private final IcSampleDao dao;

    public IcSampleService(IcSampleRule rule, IcSampleDao dao) {
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
