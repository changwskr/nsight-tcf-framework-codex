package com.nh.nsight.marketing.sv.persistence.mapper;

import com.nh.nsight.marketing.sv.application.dto.customer.CustomerSummaryCriteria;
import com.nh.nsight.marketing.sv.persistence.dto.customer.CustomerSummaryRow;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SvCustomerMapper {
    CustomerSummaryRow selectCustomerSummary(CustomerSummaryCriteria criteria);
}
