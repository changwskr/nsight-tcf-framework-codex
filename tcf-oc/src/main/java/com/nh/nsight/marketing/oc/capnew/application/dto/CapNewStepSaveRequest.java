package com.nh.nsight.marketing.oc.capnew.application.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class CapNewStepSaveRequest {

    private Map<String, Object> payload = new LinkedHashMap<>();

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}
