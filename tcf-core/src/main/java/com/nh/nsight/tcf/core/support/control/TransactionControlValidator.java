package com.nh.nsight.tcf.core.support.control;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import com.nh.nsight.tcf.core.support.TcfConsoleLog;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TransactionControlValidator {

    private final TcfProperties properties;

    public TransactionControlValidator(TcfProperties properties) {
        this.properties = properties;
    }

    public void validateRequired(TransactionControlHeader header) {
        if (!properties.isTransactionControlEnabled()) {
            return;
        }
        TcfConsoleLog.boundary("TransactionControlValidator", "validateRequired", "START");
        required(header.getServiceId(), ErrorCode.TXCTRL_HDR_SERVICE_ID, "serviceId가 없습니다.");
        required(header.getTransactionCode(), ErrorCode.TXCTRL_HDR_TRANSACTION_CODE, "transactionCode가 없습니다.");
        required(header.getBusinessCode(), ErrorCode.TXCTRL_HDR_BUSINESS_CODE, "businessCode가 없습니다.");
        required(header.getServiceName(), ErrorCode.TXCTRL_HDR_SERVICE_NAME, "serviceName이 없습니다.");
        required(header.getUser(), ErrorCode.TXCTRL_HDR_USER, "user가 없습니다.");
        required(header.getChannelId(), ErrorCode.TXCTRL_HDR_CHANNEL_ID, "channelId가 없습니다.");
        required(header.getBranch(), ErrorCode.TXCTRL_HDR_BRANCH, "branch가 없습니다.");
        TcfConsoleLog.boundary("TransactionControlValidator", "validateRequired", "END");
    }

    private void required(String value, String errorCode, String message) {
        if (!StringUtils.hasText(value)) {
            TcfConsoleLog.step("TransactionControlValidator", "validateRequired", "FAIL", "errorCode=" + errorCode);
            throw new BusinessException(errorCode, message);
        }
    }
}
