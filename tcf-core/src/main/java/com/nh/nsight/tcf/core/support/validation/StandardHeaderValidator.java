package com.nh.nsight.tcf.core.support.validation;

import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import com.nh.nsight.tcf.core.support.message.StandardHeader;
import com.nh.nsight.tcf.core.support.message.StandardRequest;
import com.nh.nsight.tcf.core.support.message.catalog.TcfStandardMessageCatalog;
import com.nh.nsight.tcf.core.support.TcfConsoleLog;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class StandardHeaderValidator {
    public void validate(StandardRequest<Map<String, Object>> request) {
        TcfConsoleLog.boundary("HeaderValidator", "validate", "START");
        if (request == null || request.getHeader() == null) {
            TcfConsoleLog.boundary("HeaderValidator", "validate", "END", "header missing");
            throw new BusinessException(ErrorCode.INVALID_HEADER, "표준 Header가 없습니다.");
        }
        StandardHeader header = request.getHeader();
        TcfConsoleLog.step("HeaderValidator", "validate", "header.normalize");
        header.normalize();
        for (String fieldKey : TcfStandardMessageCatalog.requiredRequestHeaderFieldKeys()) {
            TcfConsoleLog.step("HeaderValidator", "validate", "required " + fieldKey);
            required(TcfStandardMessageCatalog.readHeaderField(header, fieldKey), fieldKey);
        }
        TcfConsoleLog.boundary("HeaderValidator", "validate", "END");
    }

    private void required(String value, String name) {
        if (!StringUtils.hasText(value)) {
            TcfConsoleLog.step("HeaderValidator", "validate", "FAIL", "field=" + name);
            throw new BusinessException(ErrorCode.INVALID_HEADER, "필수 Header 누락: " + name);
        }
    }
}
