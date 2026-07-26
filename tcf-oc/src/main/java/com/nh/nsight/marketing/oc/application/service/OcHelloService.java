package com.nh.nsight.marketing.oc.application.service;

import com.nh.nsight.marketing.oc.application.dto.hello.HelloInquiryRequest;
import com.nh.nsight.marketing.oc.application.dto.hello.HelloInquiryResponse;
import com.nh.nsight.marketing.oc.application.dto.hello.HelloSearchCriteria;
import com.nh.nsight.marketing.oc.application.rule.OcHelloRule;
import com.nh.nsight.marketing.oc.persistence.dao.OcHelloDao;
import com.nh.nsight.marketing.oc.persistence.dto.hello.HelloRow;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import org.springframework.stereotype.Service;

@Service
public class OcHelloService {
    private final OcHelloRule rule;
    private final OcHelloDao dao;

    public OcHelloService(OcHelloRule rule, OcHelloDao dao) {
        this.rule = rule;
        this.dao = dao;
    }

    public HelloInquiryResponse inquiry(HelloInquiryRequest request, TransactionContext context) {
        rule.validateInquiry(request);
        HelloSearchCriteria criteria = rule.buildSearchCriteria(request);
        HelloRow row = dao.selectHello(criteria);
        return HelloInquiryResponse.of(context, row);
    }
}
