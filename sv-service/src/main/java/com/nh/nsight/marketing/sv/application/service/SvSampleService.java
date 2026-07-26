package com.nh.nsight.marketing.sv.application.service;

import com.nh.nsight.marketing.sv.application.dto.sample.SampleInquiryRequest;
import com.nh.nsight.marketing.sv.application.dto.sample.SampleInquiryResponse;
import com.nh.nsight.marketing.sv.application.dto.sample.SampleListItem;
import com.nh.nsight.marketing.sv.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.sv.application.rule.SvSampleRule;
import com.nh.nsight.marketing.sv.persistence.dao.SvSampleDao;
import com.nh.nsight.marketing.sv.persistence.dto.sample.SampleRow;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SvSampleService {
    private final SvSampleRule rule;
    private final SvSampleDao dao;

    public SvSampleService(SvSampleRule rule, SvSampleDao dao) {
        this.rule = rule;
        this.dao = dao;
    }

    public SampleInquiryResponse inquiry(SampleInquiryRequest request, TransactionContext context) {
        rule.validateInquiry(request);
        SampleSearchCriteria criteria = rule.buildSearchCriteria(request);
        List<SampleRow> rows = dao.searchSamples(criteria);
        int totalCount = dao.countSamples(criteria);
        List<SampleListItem> list = rows.stream().map(SampleListItem::fromRow).toList();
        return SampleInquiryResponse.of(
                context, list, criteria.getPageNo(), criteria.getPageSize(), totalCount);
    }
}
