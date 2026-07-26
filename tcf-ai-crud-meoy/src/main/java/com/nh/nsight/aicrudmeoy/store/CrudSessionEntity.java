package com.nh.nsight.aicrudmeoy.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "crud_session")
public class CrudSessionEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "current_step_id", length = 32)
    private String currentStepId;

    @Column(name = "gate_status", length = 32, nullable = false)
    private String gateStatus = "NONE";

    @Column(name = "gate_note")
    private String gateNote;

    @Column(name = "business_code", length = 8)
    private String businessCode;

    @Column(name = "domain_code", length = 64)
    private String domainCode;

    @Column(name = "sample_flag")
    private Boolean sampleFlag = Boolean.FALSE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCurrentStepId() { return currentStepId; }
    public void setCurrentStepId(String currentStepId) { this.currentStepId = currentStepId; }
    public String getGateStatus() { return gateStatus; }
    public void setGateStatus(String gateStatus) { this.gateStatus = gateStatus; }
    public String getGateNote() { return gateNote; }
    public void setGateNote(String gateNote) { this.gateNote = gateNote; }
    public String getBusinessCode() { return businessCode; }
    public void setBusinessCode(String businessCode) { this.businessCode = businessCode; }
    public String getDomainCode() { return domainCode; }
    public void setDomainCode(String domainCode) { this.domainCode = domainCode; }
    public boolean isSampleFlag() { return Boolean.TRUE.equals(sampleFlag); }
    public void setSampleFlag(boolean sampleFlag) { this.sampleFlag = sampleFlag; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
