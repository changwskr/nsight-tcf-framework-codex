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
@Table(name = "ledger_entry", uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "entry_key"}))
public class LedgerEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "entry_key", nullable = false, length = 128)
    private String entryKey;

    @Lob
    @Column(name = "entry_value", nullable = false)
    private String value;

    @Column(name = "source_step_id", length = 32)
    private String sourceStepId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getEntryKey() { return entryKey; }
    public void setEntryKey(String entryKey) { this.entryKey = entryKey; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getSourceStepId() { return sourceStepId; }
    public void setSourceStepId(String sourceStepId) { this.sourceStepId = sourceStepId; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
