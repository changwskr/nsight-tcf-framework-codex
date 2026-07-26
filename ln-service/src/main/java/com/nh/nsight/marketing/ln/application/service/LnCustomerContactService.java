package com.nh.nsight.marketing.ln.application.service;

import com.nh.nsight.marketing.ln.application.dto.customercontact.CustomerContactSearchCriteria;
import com.nh.nsight.marketing.ln.application.dto.customercontact.CustomerContactSelectDetailRequest;
import com.nh.nsight.marketing.ln.application.dto.customercontact.CustomerContactSelectDetailResponse;
import com.nh.nsight.marketing.ln.application.dto.customercontact.CustomerContactSelectListRequest;
import com.nh.nsight.marketing.ln.application.dto.customercontact.CustomerContactSelectListResponse;
import com.nh.nsight.marketing.ln.application.rule.LnCustomerContactRule;
import com.nh.nsight.marketing.ln.persistence.dao.LnCustomerContactDao;
import com.nh.nsight.marketing.ln.persistence.dto.customercontact.CustomerContactRow;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LnCustomerContactService {
    private final LnCustomerContactRule rule;
    private final LnCustomerContactDao dao;

    public LnCustomerContactService(LnCustomerContactRule rule, LnCustomerContactDao dao) {
        this.rule = rule;
        this.dao = dao;
    }

    public CustomerContactSelectListResponse selectList(
            CustomerContactSelectListRequest request, TransactionContext context) {
        rule.validateSelectList(request);
        CustomerContactSearchCriteria criteria = rule.buildSearchCriteria(request);
        List<CustomerContactRow> rows = dao.searchContacts(criteria);
        int totalCount = dao.countContacts(criteria);
        return CustomerContactSelectListResponse.of(context, criteria, rows, totalCount);
    }

    public CustomerContactSelectDetailResponse selectDetail(
            CustomerContactSelectDetailRequest request, TransactionContext context) {
        rule.validateSelectDetail(request);
        CustomerContactRow row = dao.selectByContactId(request.getContactId());
        rule.requireExists(row, request.getContactId());
        return CustomerContactSelectDetailResponse.of(context, row);
    }
}
