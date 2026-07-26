package com.nh.nsight.marketing.oc.capnew.application.dto;

public class CapNewApproveRequest {

    private String approver;
    private String reviewer;
    private String approvalNote;
    private boolean criticalOverride;

    public String getApprover() {
        return approver;
    }

    public void setApprover(String approver) {
        this.approver = approver;
    }

    public String getReviewer() {
        return reviewer;
    }

    public void setReviewer(String reviewer) {
        this.reviewer = reviewer;
    }

    public String getApprovalNote() {
        return approvalNote;
    }

    public void setApprovalNote(String approvalNote) {
        this.approvalNote = approvalNote;
    }

    public boolean isCriticalOverride() {
        return criticalOverride;
    }

    public void setCriticalOverride(boolean criticalOverride) {
        this.criticalOverride = criticalOverride;
    }
}
