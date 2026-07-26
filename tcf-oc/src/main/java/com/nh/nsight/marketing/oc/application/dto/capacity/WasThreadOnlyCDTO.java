package com.nh.nsight.marketing.oc.application.dto.capacity;

public class WasThreadOnlyCDTO {

    private int targetTps;
    private int apCount = 1;
    private double avgThreadHoldSec = 1.2;
    private double threadMarginRate = 1.2;
    private double maxThreadMarginRate = 1.3;
    private String vmSpecCode = "8C64G";

    public int getTargetTps() {
        return targetTps;
    }

    public void setTargetTps(int targetTps) {
        this.targetTps = targetTps;
    }

    public int getApCount() {
        return apCount;
    }

    public void setApCount(int apCount) {
        this.apCount = apCount;
    }

    public double getAvgThreadHoldSec() {
        return avgThreadHoldSec;
    }

    public void setAvgThreadHoldSec(double avgThreadHoldSec) {
        this.avgThreadHoldSec = avgThreadHoldSec;
    }

    public double getThreadMarginRate() {
        return threadMarginRate;
    }

    public void setThreadMarginRate(double threadMarginRate) {
        this.threadMarginRate = threadMarginRate;
    }

    public double getMaxThreadMarginRate() {
        return maxThreadMarginRate;
    }

    public void setMaxThreadMarginRate(double maxThreadMarginRate) {
        this.maxThreadMarginRate = maxThreadMarginRate;
    }

    public String getVmSpecCode() {
        return vmSpecCode;
    }

    public void setVmSpecCode(String vmSpecCode) {
        this.vmSpecCode = vmSpecCode;
    }
}
