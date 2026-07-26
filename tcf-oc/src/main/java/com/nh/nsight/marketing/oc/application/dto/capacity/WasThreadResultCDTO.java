package com.nh.nsight.marketing.oc.application.dto.capacity;

public class WasThreadResultCDTO {

    private int totalCalculatedThreads;
    private int threadsPerVm;
    private int recommendedMaxThreads;
    private int minSpareThreads;
    private int acceptCount;
    private String status;
    private String statusMessage;

    public int getTotalCalculatedThreads() {
        return totalCalculatedThreads;
    }

    public void setTotalCalculatedThreads(int totalCalculatedThreads) {
        this.totalCalculatedThreads = totalCalculatedThreads;
    }

    public int getThreadsPerVm() {
        return threadsPerVm;
    }

    public void setThreadsPerVm(int threadsPerVm) {
        this.threadsPerVm = threadsPerVm;
    }

    public int getRecommendedMaxThreads() {
        return recommendedMaxThreads;
    }

    public void setRecommendedMaxThreads(int recommendedMaxThreads) {
        this.recommendedMaxThreads = recommendedMaxThreads;
    }

    public int getMinSpareThreads() {
        return minSpareThreads;
    }

    public void setMinSpareThreads(int minSpareThreads) {
        this.minSpareThreads = minSpareThreads;
    }

    public int getAcceptCount() {
        return acceptCount;
    }

    public void setAcceptCount(int acceptCount) {
        this.acceptCount = acceptCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}
