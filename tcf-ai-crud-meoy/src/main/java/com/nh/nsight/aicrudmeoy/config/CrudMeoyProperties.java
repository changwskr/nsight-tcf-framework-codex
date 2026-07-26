package com.nh.nsight.aicrudmeoy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nsight.crud-meoy")
public class CrudMeoyProperties {

    private String version = "0.1.0";
    private String catalogResource = "classpath:prompts/catalog.json";
    private String promptLocation = "classpath:prompts/";
    private String domainLedgerResource = "classpath:data/domain-ledger.json";
    /** 관련 소스 조회의 루트. 비어 있으면 작업 디렉터리에서 settings.gradle 위치를 자동 탐지. */
    private String repoRoot = "";

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCatalogResource() {
        return catalogResource;
    }

    public void setCatalogResource(String catalogResource) {
        this.catalogResource = catalogResource;
    }

    public String getPromptLocation() {
        return promptLocation;
    }

    public void setPromptLocation(String promptLocation) {
        this.promptLocation = promptLocation;
    }

    public String getDomainLedgerResource() {
        return domainLedgerResource;
    }

    public void setDomainLedgerResource(String domainLedgerResource) {
        this.domainLedgerResource = domainLedgerResource;
    }

    public String getRepoRoot() {
        return repoRoot;
    }

    public void setRepoRoot(String repoRoot) {
        this.repoRoot = repoRoot;
    }
}