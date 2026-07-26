package com.nh.nsight.aimethodology.model;

import java.util.ArrayList;
import java.util.List;

/**
 * NSIGHT Model Studio 업무 모델. sample_model.json 스키마와 대응한다.
 */
public class BusinessModel {

    private String id;
    private String projectName;
    private String basePackage;
    private String packageProfile;
    private String businessCode;
    private String businessName;
    private String moduleName;
    private String contextPath;
    private String domainCode;
    private String domainName;
    private String aggregateName;
    private String operation;
    private String methodName;
    private String screenId;
    private String screenName;
    private String eventId;
    private String eventName;
    private String uiObjectId;
    private String serviceId;
    private String serviceName;
    private String transactionCode;
    private String permissionCode;
    private Integer timeoutSeconds;
    private Boolean auditRequired;
    private Boolean idempotencyRequired;
    private String tableName;
    private String tableComment;
    private String successAction;
    private String failureAction;
    private List<FieldModel> fields = new ArrayList<>();

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

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public String getPackageProfile() {
        return packageProfile;
    }

    public void setPackageProfile(String packageProfile) {
        this.packageProfile = packageProfile;
    }

    public String getBusinessCode() {
        return businessCode;
    }

    public void setBusinessCode(String businessCode) {
        this.businessCode = businessCode;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public String getDomainCode() {
        return domainCode;
    }

    public void setDomainCode(String domainCode) {
        this.domainCode = domainCode;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getAggregateName() {
        return aggregateName;
    }

    public void setAggregateName(String aggregateName) {
        this.aggregateName = aggregateName;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getScreenId() {
        return screenId;
    }

    public void setScreenId(String screenId) {
        this.screenId = screenId;
    }

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getUiObjectId() {
        return uiObjectId;
    }

    public void setUiObjectId(String uiObjectId) {
        this.uiObjectId = uiObjectId;
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

    public String getTransactionCode() {
        return transactionCode;
    }

    public void setTransactionCode(String transactionCode) {
        this.transactionCode = transactionCode;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int timeoutOrDefault() {
        return timeoutSeconds == null ? 3 : timeoutSeconds;
    }

    public Boolean getAuditRequired() {
        return auditRequired;
    }

    public void setAuditRequired(Boolean auditRequired) {
        this.auditRequired = auditRequired;
    }

    public boolean isAuditRequired() {
        return Boolean.TRUE.equals(auditRequired);
    }

    public Boolean getIdempotencyRequired() {
        return idempotencyRequired;
    }

    public void setIdempotencyRequired(Boolean idempotencyRequired) {
        this.idempotencyRequired = idempotencyRequired;
    }

    public boolean isIdempotencyRequired() {
        return Boolean.TRUE.equals(idempotencyRequired);
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTableComment() {
        return tableComment;
    }

    public void setTableComment(String tableComment) {
        this.tableComment = tableComment;
    }

    public String getSuccessAction() {
        return successAction;
    }

    public void setSuccessAction(String successAction) {
        this.successAction = successAction;
    }

    public String getFailureAction() {
        return failureAction;
    }

    public void setFailureAction(String failureAction) {
        this.failureAction = failureAction;
    }

    public List<FieldModel> getFields() {
        if (fields == null) {
            fields = new ArrayList<>();
        }
        return fields;
    }

    public void setFields(List<FieldModel> fields) {
        this.fields = fields;
    }
}
