package com.nh.nsight.marketing.sv.persistence.dto.customer;

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
    private String branchName;
    private Object totalBalance;
    private Object loanBalance;
    private Object productCount;
    private String lastTransactionDate;

    public boolean isEmpty() {
        return customerNo == null || customerNo.isBlank();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("customerNo", customerNo);
        map.put("customerName", customerName);
        map.put("customerGrade", customerGrade);
        map.put("branchCode", branchCode);
        map.put("branchName", branchName);
        map.put("totalBalance", totalBalance);
        map.put("loanBalance", loanBalance);
        map.put("productCount", productCount);
        map.put("lastTransactionDate", lastTransactionDate);
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

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public Object getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(Object totalBalance) {
        this.totalBalance = totalBalance;
    }

    public Object getLoanBalance() {
        return loanBalance;
    }

    public void setLoanBalance(Object loanBalance) {
        this.loanBalance = loanBalance;
    }

    public Object getProductCount() {
        return productCount;
    }

    public void setProductCount(Object productCount) {
        this.productCount = productCount;
    }

    public String getLastTransactionDate() {
        return lastTransactionDate;
    }

    public void setLastTransactionDate(String lastTransactionDate) {
        this.lastTransactionDate = lastTransactionDate;
    }
}
