package com.nh.nsight.aimethodology.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 업무모델 DB 행. 전체 JSON은 payload에 두고, 목록·검색용 키 컬럼을 병행한다.
 */
@Entity
@Table(name = "business_model")
public class BusinessModelEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(name = "project_name", length = 128)
    private String projectName;

    @Column(name = "service_id", length = 256)
    private String serviceId;

    @Column(name = "service_name", length = 256)
    private String serviceName;

    @Column(name = "domain_code", length = 64)
    private String domainCode;

    @Column(name = "aggregate_name", length = 128)
    private String aggregateName;

    @Column(name = "method_name", length = 128)
    private String methodName;

    @Column(length = 32)
    private String operation;

    @Column(name = "screen_id", length = 64)
    private String screenId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Lob
    @Column(nullable = false)
    private String payload;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getDomainCode() {
        return domainCode;
    }

    public void setDomainCode(String domainCode) {
        this.domainCode = domainCode;
    }

    public String getAggregateName() {
        return aggregateName;
    }

    public void setAggregateName(String aggregateName) {
        this.aggregateName = aggregateName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getScreenId() {
        return screenId;
    }

    public void setScreenId(String screenId) {
        this.screenId = screenId;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
