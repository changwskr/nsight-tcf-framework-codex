package com.nh.nsight.marketing.av.application.service;

import com.nh.nsight.marketing.av.application.dto.sample.SampleInquiryRequest;
import com.nh.nsight.marketing.av.application.dto.sample.SampleInquiryResponse;
import com.nh.nsight.marketing.av.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.av.application.rule.AvSampleRule;
import com.nh.nsight.marketing.av.persistence.dao.AvSampleDao;
import com.nh.nsight.marketing.av.persistence.dto.sample.SampleRow;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AvSampleService {
    private final AvSampleRule rule;
    private final AvSampleDao dao;

    public AvSampleService(AvSampleRule rule, AvSampleDao dao) {
        this.rule = rule;
        this.dao = dao;
    }

    public SampleInquiryResponse inquiry(SampleInquiryRequest request, TransactionContext context) {
        rule.validateInquiry(request);
        SampleSearchCriteria criteria = rule.buildSearchCriteria(request);
        List<SampleRow> rows = dao.searchSamples(criteria);
        int totalCount = dao.countSamples(criteria);
        return SampleInquiryResponse.of(context, criteria, rows, totalCount);
    }
}
