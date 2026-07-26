package com.nh.nsight.tcf.web.entry.web;

import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import com.nh.nsight.tcf.core.support.message.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalStandardExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public StandardResponse<Object> handleBusiness(BusinessException e) {
        return StandardResponse.fail(null, e.getErrorCode(), e.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public StandardResponse<Object> handleValidation(MethodArgumentNotValidException e) {
        return StandardResponse.fail(null, ErrorCode.INVALID_HEADER, "요청 Header가 올바르지 않습니다.", e.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.OK)
    public StandardResponse<Object> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return StandardResponse.fail(null, ErrorCode.INVALID_HEADER,
                "POST /online 으로 JSON 전문을 전송해야 합니다. (브라우저 주소창 GET 접근 불가)",
                e.getClass().getSimpleName());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public StandardResponse<Object> handleSystem(Exception e) {
        return StandardResponse.fail(null, ErrorCode.SYSTEM_ERROR, "시스템 오류가 발생했습니다.", e.getClass().getSimpleName());
    }
}
