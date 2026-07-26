package com.nh.nsight.marketing.sv.application.dto.customer;



/**
 * SV.Customer.selectSummary DAO/MyBatis 조건.
 */
public class CustomerSummaryCriteria {

    private final String customerNo;

    public CustomerSummaryCriteria(String customerNo) {
        this.customerNo = customerNo;
    }

    public String getCustomerNo() {
        return customerNo;
    }
}
