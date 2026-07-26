package com.nh.nsight.marketing.oc.capnew.application.dto;

public class CapNewRevokeRequest {

    private String revoker;
    private String revokeNote;

    public String getRevoker() {
        return revoker;
    }

    public void setRevoker(String revoker) {
        this.revoker = revoker;
    }

    public String getRevokeNote() {
        return revokeNote;
    }

    public void setRevokeNote(String revokeNote) {
        this.revokeNote = revokeNote;
    }
}
