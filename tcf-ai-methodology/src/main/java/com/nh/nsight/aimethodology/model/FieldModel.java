package com.nh.nsight.aimethodology.model;

/**
 * 업무 모델 필드 정의. sample_model.json fields[] 항목과 대응한다.
 */
public class FieldModel {

    private String name;
    private String column;
    private String label;
    private String javaType;
    private String dbType;
    private Integer length;
    private Boolean nullable = Boolean.TRUE;
    private Boolean pk = Boolean.FALSE;
    private Boolean request = Boolean.FALSE;
    private Boolean condition = Boolean.FALSE;
    private Boolean response = Boolean.FALSE;
    private String validation;
    private Boolean sensitive = Boolean.FALSE;
    private String maskingRule;
    private Object sampleValue;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getJavaType() {
        return javaType;
    }

    public void setJavaType(String javaType) {
        this.javaType = javaType;
    }

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public Boolean getNullable() {
        return nullable;
    }

    public void setNullable(Boolean nullable) {
        this.nullable = nullable;
    }

    public boolean isNullable() {
        return nullable == null || nullable;
    }

    public Boolean getPk() {
        return pk;
    }

    public void setPk(Boolean pk) {
        this.pk = pk;
    }

    public boolean isPk() {
        return Boolean.TRUE.equals(pk);
    }

    public Boolean getRequest() {
        return request;
    }

    public void setRequest(Boolean request) {
        this.request = request;
    }

    public boolean isRequest() {
        return Boolean.TRUE.equals(request);
    }

    public Boolean getCondition() {
        return condition;
    }

    public void setCondition(Boolean condition) {
        this.condition = condition;
    }

    public boolean isCondition() {
        return Boolean.TRUE.equals(condition);
    }

    public Boolean getResponse() {
        return response;
    }

    public void setResponse(Boolean response) {
        this.response = response;
    }

    public boolean isResponse() {
        return Boolean.TRUE.equals(response);
    }

    public String getValidation() {
        return validation;
    }

    public void setValidation(String validation) {
        this.validation = validation;
    }

    public Boolean getSensitive() {
        return sensitive;
    }

    public void setSensitive(Boolean sensitive) {
        this.sensitive = sensitive;
    }

    public boolean isSensitive() {
        return Boolean.TRUE.equals(sensitive);
    }

    public String getMaskingRule() {
        return maskingRule;
    }

    public void setMaskingRule(String maskingRule) {
        this.maskingRule = maskingRule;
    }

    public Object getSampleValue() {
        return sampleValue;
    }

    public void setSampleValue(Object sampleValue) {
        this.sampleValue = sampleValue;
    }
}
