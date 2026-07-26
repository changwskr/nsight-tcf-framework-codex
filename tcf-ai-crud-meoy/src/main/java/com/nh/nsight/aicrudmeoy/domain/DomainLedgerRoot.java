package com.nh.nsight.aicrudmeoy.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DomainLedgerRoot {
    private String version;
    private String generatedAt;
    private String sourceNote;
    private int moduleCount;
    private int domainCount;
    private int serviceIdCount;
    private List<BusinessModuleLedger> modules = new ArrayList<>();

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(String generatedAt) { this.generatedAt = generatedAt; }
    public String getSourceNote() { return sourceNote; }
    public void setSourceNote(String sourceNote) { this.sourceNote = sourceNote; }
    public int getModuleCount() { return moduleCount; }
    public void setModuleCount(int moduleCount) { this.moduleCount = moduleCount; }
    public int getDomainCount() { return domainCount; }
    public void setDomainCount(int domainCount) { this.domainCount = domainCount; }
    public int getServiceIdCount() { return serviceIdCount; }
    public void setServiceIdCount(int serviceIdCount) { this.serviceIdCount = serviceIdCount; }
    public List<BusinessModuleLedger> getModules() { return modules; }
    public void setModules(List<BusinessModuleLedger> modules) { this.modules = modules; }
}
