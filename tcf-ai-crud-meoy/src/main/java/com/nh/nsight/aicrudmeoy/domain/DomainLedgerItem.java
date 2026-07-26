package com.nh.nsight.aicrudmeoy.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DomainLedgerItem {
    private String domainCode;
    private String domainName;
    private String handler;
    private List<ServiceIdLedgerItem> serviceIds = new ArrayList<>();

    public String getDomainCode() { return domainCode; }
    public void setDomainCode(String domainCode) { this.domainCode = domainCode; }
    public String getDomainName() { return domainName; }
    public void setDomainName(String domainName) { this.domainName = domainName; }
    public String getHandler() { return handler; }
    public void setHandler(String handler) { this.handler = handler; }
    public List<ServiceIdLedgerItem> getServiceIds() { return serviceIds; }
    public void setServiceIds(List<ServiceIdLedgerItem> serviceIds) { this.serviceIds = serviceIds; }
}
