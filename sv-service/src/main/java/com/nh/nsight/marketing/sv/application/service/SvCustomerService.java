package com.nh.nsight.marketing.sv.application.service;

import com.nh.nsight.marketing.sv.application.dto.customer.CustomerSummaryRequest;
import com.nh.nsight.marketing.sv.application.dto.customer.CustomerSummaryResponse;
import com.nh.nsight.marketing.sv.application.rule.SvCustomerRule;
import com.nh.nsight.marketing.sv.persistence.dao.SvCustomerDao;
import com.nh.nsight.marketing.sv.persistence.dto.customer.CustomerSummaryRow;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import org.springframework.stereotype.Service;

@Service
public class SvCustomerService {
    private final SvCustomerRule rule;
    private final SvCustomerDao dao;

    public SvCustomerService(SvCustomerRule rule, SvCustomerDao dao) {
        this.rule = rule;
        this.dao = dao;
    }

    public CustomerSummaryResponse selectCustomerSummary(
            CustomerSummaryRequest request, TransactionContext context) {
        var criteria = rule.buildSummaryCriteria(request);
        CustomerSummaryRow customer = dao.selectCustomerSummary(criteria);
        rule.validateSummaryResult(customer);
        return CustomerSummaryResponse.of(context, customer);
    }
}
