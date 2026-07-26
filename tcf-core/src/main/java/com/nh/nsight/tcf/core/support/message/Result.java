package com.nh.nsight.tcf.core.support.message;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class Result implements Serializable {
    private String resultCode;
    private String resultMessage;
    private String errorCode;
    private String errorMessage;
    private String errorDetail;
    private String errorSystemId;
    private String errorDateTime;

    public static Result success() {
        Result result = new Result();
        result.resultCode = "S0000";
        result.resultMessage = "정상 처리되었습니다.";
        return result;
    }

    public static Result fail(String errorCode, String message, String detail) {
        Result result = new Result();
        result.resultCode = "E0001";
        result.resultMessage = "처리 중 오류가 발생했습니다.";
        result.errorCode = errorCode;
        result.errorMessage = message;
        result.errorDetail = detail;
        result.errorSystemId = "NSIGHT-MP";
        result.errorDateTime = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return result;
    }

    public String getResultCode() { return resultCode; }
    public void setResultCode(String resultCode) { this.resultCode = resultCode; }
    public String getResultMessage() { return resultMessage; }
    public void setResultMessage(String resultMessage) { this.resultMessage = resultMessage; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getErrorDetail() { return errorDetail; }
    public void setErrorDetail(String errorDetail) { this.errorDetail = errorDetail; }
    public String getErrorSystemId() { return errorSystemId; }
    public void setErrorSystemId(String errorSystemId) { this.errorSystemId = errorSystemId; }
    public String getErrorDateTime() { return errorDateTime; }
    public void setErrorDateTime(String errorDateTime) { this.errorDateTime = errorDateTime; }
}
