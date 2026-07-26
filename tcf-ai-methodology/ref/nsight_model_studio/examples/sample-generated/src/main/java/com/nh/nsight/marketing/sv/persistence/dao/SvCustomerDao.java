package com.nh.nsight.marketing.sv.persistence.dao;

import com.nh.nsight.marketing.sv.application.dto.customer.CustomerSummaryCriteria;
import com.nh.nsight.marketing.sv.persistence.dto.customer.CustomerSummaryRow;
import com.nh.nsight.marketing.sv.persistence.mapper.SvCustomerMapper;
import org.springframework.stereotype.Repository;

@Repository
public class SvCustomerDao {
    private final SvCustomerMapper mapper;

    public SvCustomerDao(SvCustomerMapper mapper) {
        this.mapper = mapper;
    }

    public CustomerSummaryRow selectCustomerSummary(CustomerSummaryCriteria criteria) {
        return mapper.selectCustomerSummary(criteria);
    }
}
