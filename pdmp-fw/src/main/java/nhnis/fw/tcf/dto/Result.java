package nhnis.fw.tcf.dto;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 표준 전문 처리 결과. 성공·실패는 HTTP 상태가 아니라 {@link #resultCode}로 판별한다.
 */
public class Result implements Serializable {

    /** 성공 결과코드 */
    public static final String SUCCESS_CODE = "S0000";

    /** 실패 결과코드 */
    public static final String FAIL_CODE = "E0001";

    private String resultCode;
    private String resultMessage;
    private String errorCode;
    private String errorMessage;
    private String errorDetail;
    private String errorSystemId;
    private String errorDateTime;

    public static Result success() {
        Result result = new Result();
        result.resultCode = SUCCESS_CODE;
        result.resultMessage = "정상 처리되었습니다.";
        return result;
    }

    public static Result fail(String errorCode, String message, String detail) {
        Result result = new Result();
        result.resultCode = FAIL_CODE;
        result.resultMessage = "처리 중 오류가 발생했습니다.";
        result.errorCode = errorCode;
        result.errorMessage = message;
        result.errorDetail = detail;
        result.errorSystemId = StandardHeaderDto.DEFAULT_SYSTEM_ID;
        result.errorDateTime = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return result;
    }

    /** 성공 여부. 전문 필드가 아니라 호출측 편의 메서드라 직렬화하지 않는다. */
    @JsonIgnore
    public boolean isSuccess() {
        return SUCCESS_CODE.equals(resultCode);
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
