package com.nh.nsight.marketing.sv.persistence.dto.customer;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SV_CUSTOMER 조회 Row (MyBatis result).
 */
public class CustomerSummaryRow {

    private String customerNo;
    private String customerName;
    private String customerGrade;
    private String branchCode;
    private BigDecimal totalBalance;

    public boolean isEmpty() {
        return customerNo == null || customerNo.isBlank();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("customerNo", customerNo);
        map.put("customerName", customerName);
        map.put("customerGrade", customerGrade);
        map.put("branchCode", branchCode);
        map.put("totalBalance", totalBalance);
        return map;
    }

    public String getCustomerNo() {
        return customerNo;
    }

    public void setCustomerNo(String customerNo) {
        this.customerNo = customerNo;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerGrade() {
        return customerGrade;
    }

    public void setCustomerGrade(String customerGrade) {
        this.customerGrade = customerGrade;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(BigDecimal totalBalance) {
        this.totalBalance = totalBalance;
    }
}
