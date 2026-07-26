package com.nh.nsight.tcf.core.support.control;

import com.nh.nsight.tcf.core.support.message.StandardHeader;

public class TransactionControlHeader {
    private String serviceId;
    private String transactionCode;
    private String businessCode;
    private String serviceName;
    private String user;
    private String channelId;
    private String branch;
    private String clientIp;

    public static TransactionControlHeader from(StandardHeader source) {
        TransactionControlHeader header = new TransactionControlHeader();
        header.setServiceId(source.getServiceId());
        header.setTransactionCode(source.getTransactionCode());
        header.setBusinessCode(source.getBusinessCode());
        header.setServiceName(source.getServiceName());
        header.setUser(source.getUserId());
        header.setChannelId(source.getChannelId());
        header.setBranch(source.getBranchId());
        header.setClientIp(source.getClientIp());
        return header;
    }

    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }
    public String getTransactionCode() { return transactionCode; }
    public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }
    public String getBusinessCode() { return businessCode; }
    public void setBusinessCode(String businessCode) { this.businessCode = businessCode; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }
    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
}
