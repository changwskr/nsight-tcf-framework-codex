package com.nh.nsight.aimethodology.model;

/**
 * 모델/워크스페이스 검증 이슈.
 */
public class ValidationIssue {

    private String level;
    private String code;
    private String path;
    private String message;

    public ValidationIssue() {
    }

    public ValidationIssue(String level, String code, String path, String message) {
        this.level = level;
        this.code = code;
        this.path = path;
        this.message = message;
    }

    public static ValidationIssue of(String level, String code, String path, String message) {
        return new ValidationIssue(level, code, path, message);
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
