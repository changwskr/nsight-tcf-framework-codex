package com.nh.nsight.aicrudmeoy.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceIdLedgerItem {
    private String serviceId;
    private String action;
    private String operation;

    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
}
