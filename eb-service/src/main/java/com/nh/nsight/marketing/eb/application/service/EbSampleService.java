package com.nh.nsight.marketing.eb.application.service;

import com.nh.nsight.marketing.eb.application.dto.sample.SampleInquiryRequest;
import com.nh.nsight.marketing.eb.application.dto.sample.SampleInquiryResponse;
import com.nh.nsight.marketing.eb.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.eb.application.rule.EbSampleRule;
import com.nh.nsight.marketing.eb.persistence.dao.EbSampleDao;
import com.nh.nsight.marketing.eb.persistence.dto.sample.SampleRow;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import org.springframework.stereotype.Service;

@Service
public class EbSampleService {
    private final EbSampleRule rule;
    private final EbSampleDao dao;

    public EbSampleService(EbSampleRule rule, EbSampleDao dao) {
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
