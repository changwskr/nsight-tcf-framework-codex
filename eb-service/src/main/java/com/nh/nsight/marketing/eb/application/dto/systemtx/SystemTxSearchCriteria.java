package com.nh.nsight.marketing.eb.application.dto.systemtx;

public class SystemTxSearchCriteria {

    private int pageNo;
    private int pageSize;
    private int offset;
    private String txDateFrom;
    private String txDateTo;
    private String txType;
    private String txSeqNo;
    private String empNo;
    private String screenId;
    private String serviceId;

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public String getTxDateFrom() {
        return txDateFrom;
    }

    public void setTxDateFrom(String txDateFrom) {
        this.txDateFrom = txDateFrom;
    }

    public String getTxDateTo() {
        return txDateTo;
    }

    public void setTxDateTo(String txDateTo) {
        this.txDateTo = txDateTo;
    }

    public String getTxType() {
        return txType;
    }

    public void setTxType(String txType) {
        this.txType = txType;
    }

    public String getTxSeqNo() {
        return txSeqNo;
    }

    public void setTxSeqNo(String txSeqNo) {
        this.txSeqNo = txSeqNo;
    }

    public String getEmpNo() {
        return empNo;
    }

    public void setEmpNo(String empNo) {
        this.empNo = empNo;
    }

    public String getScreenId() {
        return screenId;
    }

    public void setScreenId(String screenId) {
        this.screenId = screenId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }
}
