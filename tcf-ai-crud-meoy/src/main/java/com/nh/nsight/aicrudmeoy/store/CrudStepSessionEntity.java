package com.nh.nsight.aicrudmeoy.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * 단계별 세션 저장 테이블 — 세션 × 단계(C-MASTER~C18) 단위.
 */
@Entity
@Table(
        name = "crud_step_session",
        uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "step_id"}))
public class CrudStepSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "session_name", length = 200)
    private String sessionName;

    @Column(name = "step_id", nullable = false, length = 32)
    private String stepId;

    @Column(name = "step_title", length = 120)
    private String stepTitle;

    @Column(name = "step_order")
    private Integer stepOrder;

    @Column(nullable = false, length = 32)
    private String status = "IN_PROGRESS";

    @Column(name = "business_code", length = 8)
    private String businessCode;

    @Column(name = "domain_code", length = 64)
    private String domainCode;

    @Lob
    @Column(name = "answers_json")
    private String answersJson;

    @Lob
    @Column(name = "summary_md")
    private String summaryMd;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getSessionName() { return sessionName; }
    public void setSessionName(String sessionName) { this.sessionName = sessionName; }
    public String getStepId() { return stepId; }
    public void setStepId(String stepId) { this.stepId = stepId; }
    public String getStepTitle() { return stepTitle; }
    public void setStepTitle(String stepTitle) { this.stepTitle = stepTitle; }
    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getBusinessCode() { return businessCode; }
    public void setBusinessCode(String businessCode) { this.businessCode = businessCode; }
    public String getDomainCode() { return domainCode; }
    public void setDomainCode(String domainCode) { this.domainCode = domainCode; }
    public String getAnswersJson() { return answersJson; }
    public void setAnswersJson(String answersJson) { this.answersJson = answersJson; }
    public String getSummaryMd() { return summaryMd; }
    public void setSummaryMd(String summaryMd) { this.summaryMd = summaryMd; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }
}
