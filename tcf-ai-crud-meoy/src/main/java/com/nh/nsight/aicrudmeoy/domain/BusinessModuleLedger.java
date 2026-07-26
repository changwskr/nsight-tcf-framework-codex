package com.nh.nsight.aicrudmeoy.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BusinessModuleLedger {
    private String businessCode;
    private String moduleName;
    private String group;
    private Integer localPort;
    private String gradleModule;
    private String status;
    private int domainCount;
    private int serviceIdCount;
    private List<DomainLedgerItem> domains = new ArrayList<>();

    public String getBusinessCode() { return businessCode; }
    public void setBusinessCode(String businessCode) { this.businessCode = businessCode; }
    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }
    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }
    public Integer getLocalPort() { return localPort; }
    public void setLocalPort(Integer localPort) { this.localPort = localPort; }
    public String getGradleModule() { return gradleModule; }
    public void setGradleModule(String gradleModule) { this.gradleModule = gradleModule; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getDomainCount() { return domainCount; }
    public void setDomainCount(int domainCount) { this.domainCount = domainCount; }
    public int getServiceIdCount() { return serviceIdCount; }
    public void setServiceIdCount(int serviceIdCount) { this.serviceIdCount = serviceIdCount; }
    public List<DomainLedgerItem> getDomains() { return domains; }
    public void setDomains(List<DomainLedgerItem> domains) { this.domains = domains; }
}
