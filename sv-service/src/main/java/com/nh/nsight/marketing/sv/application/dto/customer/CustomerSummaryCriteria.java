package com.nh.nsight.marketing.sv.application.dto.customer;

/**
 * 고객 요약 조회 DAO/MyBatis 조건.
 */
public class CustomerSummaryCriteria {

    private final String customerNo;
    private final String baseDate;

    public CustomerSummaryCriteria(String customerNo, String baseDate) {
        this.customerNo = customerNo;
        this.baseDate = baseDate;
    }

    public String getCustomerNo() {
        return customerNo;
    }

    public String getBaseDate() {
        return baseDate;
    }
}
