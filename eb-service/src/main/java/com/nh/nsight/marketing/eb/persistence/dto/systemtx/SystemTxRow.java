package com.nh.nsight.marketing.eb.persistence.dto.systemtx;

import java.util.LinkedHashMap;
import java.util.Map;

public class SystemTxRow {

    private Long rowNo;
    private String txSeqNo;
    private String txDate;
    private String screenId;
    private String serviceId;
    private String globalId;
    private String requestAt;
    private String responseAt;
    private Integer elapsedSec;
    private String inputContent;
    private String empNo;
    private String branchCode;
    private String terminalIp;
    private String txType;

    public Long getRowNo() {
        return rowNo;
    }

    public void setRowNo(Long rowNo) {
        this.rowNo = rowNo;
    }

    public String getTxSeqNo() {
        return txSeqNo;
    }

    public void setTxSeqNo(String txSeqNo) {
        this.txSeqNo = txSeqNo;
    }

    public String getTxDate() {
        return txDate;
    }

    public void setTxDate(String txDate) {
        this.txDate = txDate;
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

    public String getGlobalId() {
        return globalId;
    }

    public void setGlobalId(String globalId) {
        this.globalId = globalId;
    }

    public String getRequestAt() {
        return requestAt;
    }

    public void setRequestAt(String requestAt) {
        this.requestAt = requestAt;
    }

    public String getResponseAt() {
        return responseAt;
    }

    public void setResponseAt(String responseAt) {
        this.responseAt = responseAt;
    }

    public Integer getElapsedSec() {
        return elapsedSec;
    }

    public void setElapsedSec(Integer elapsedSec) {
        this.elapsedSec = elapsedSec;
    }

    public String getInputContent() {
        return inputContent;
    }

    public void setInputContent(String inputContent) {
        this.inputContent = inputContent;
    }

    public String getEmpNo() {
        return empNo;
    }

    public void setEmpNo(String empNo) {
        this.empNo = empNo;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public String getTerminalIp() {
        return terminalIp;
    }

    public void setTerminalIp(String terminalIp) {
        this.terminalIp = terminalIp;
    }

    public String getTxType() {
        return txType;
    }

    public void setTxType(String txType) {
        this.txType = txType;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("rowNo", rowNo);
        map.put("txSeqNo", txSeqNo);
        map.put("txDate", txDate);
        map.put("screenId", screenId);
        map.put("serviceId", serviceId);
        map.put("globalId", globalId);
        map.put("requestAt", requestAt);
        map.put("responseAt", responseAt);
        map.put("elapsedSec", elapsedSec);
        map.put("inputContent", inputContent);
        map.put("empNo", empNo);
        map.put("branchCode", branchCode);
        map.put("terminalIp", terminalIp);
        map.put("txType", txType);
        return map;
    }
}
