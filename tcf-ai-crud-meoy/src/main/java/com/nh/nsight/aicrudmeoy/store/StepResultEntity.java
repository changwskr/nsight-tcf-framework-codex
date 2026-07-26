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

@Entity
@Table(name = "step_result", uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "step_id"}))
public class StepResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "step_id", nullable = false, length = 32)
    private String stepId;

    @Column(nullable = false, length = 32)
    private String status = "IN_PROGRESS";

    @Lob
    @Column(name = "summary_md")
    private String summaryMd;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getStepId() { return stepId; }
    public void setStepId(String stepId) { this.stepId = stepId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSummaryMd() { return summaryMd; }
    public void setSummaryMd(String summaryMd) { this.summaryMd = summaryMd; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }
}
