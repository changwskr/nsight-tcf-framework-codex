package com.nh.nsight.aimethodology.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nsight.model-studio")
public class ModelStudioProperties {

    private String version = "0.1.0";
    private String dataFile = "";
    private String sampleResource = "classpath:data/sample_model.json";
    private String seedResource = "classpath:data/models-seed.json";

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDataFile() {
        return dataFile;
    }

    public void setDataFile(String dataFile) {
        this.dataFile = dataFile;
    }

    public String getSampleResource() {
        return sampleResource;
    }

    public void setSampleResource(String sampleResource) {
        this.sampleResource = sampleResource;
    }

    public String getSeedResource() {
        return seedResource;
    }

    public void setSeedResource(String seedResource) {
        this.seedResource = seedResource;
    }
}
