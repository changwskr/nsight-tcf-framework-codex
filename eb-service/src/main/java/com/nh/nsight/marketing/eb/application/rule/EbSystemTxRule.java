package com.nh.nsight.marketing.eb.application.rule;

import com.nh.nsight.marketing.eb.application.dto.systemtx.SystemTxInquiryRequest;
import com.nh.nsight.marketing.eb.application.dto.systemtx.SystemTxSearchCriteria;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class EbSystemTxRule {
    private static final int MAX_PAGE_SIZE = 100;

    public void validateInquiry(SystemTxInquiryRequest request) {
        if (request == null) {
            return;
        }
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;
        if (pageSize > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "pageSize는 최대 " + MAX_PAGE_SIZE + " 입니다.");
        }
        int pageNo = request.getPageNo() != null ? request.getPageNo() : 1;
        if (pageNo < 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "pageNo는 1 이상이어야 합니다.");
        }
    }

    public SystemTxSearchCriteria buildSearchCriteria(SystemTxInquiryRequest request) {
        SystemTxInquiryRequest safe = request != null ? request : SystemTxInquiryRequest.fromMap(Map.of());
        int pageNo = safe.getPageNo() != null ? safe.getPageNo() : 1;
        int pageSize = safe.getPageSize() != null ? safe.getPageSize() : 20;
        if (pageNo < 1) {
            pageNo = 1;
        }
        if (pageSize < 1) {
            pageSize = 20;
        }

        SystemTxSearchCriteria criteria = new SystemTxSearchCriteria();
        criteria.setPageNo(pageNo);
        criteria.setPageSize(pageSize);
        criteria.setOffset((pageNo - 1) * pageSize);
        criteria.setTxDateFrom(normalizeDateTime(safe.getTxDateFrom(), true));
        criteria.setTxDateTo(normalizeDateTime(safe.getTxDateTo(), false));
        criteria.setTxType(safe.getTxType());
        criteria.setTxSeqNo(safe.getTxSeqNo());
        criteria.setEmpNo(safe.getEmpNo());
        criteria.setScreenId(safe.getScreenId());
        criteria.setServiceId(safe.getServiceId());
        return criteria;
    }

    /**
     * UI datetime-local / 날짜만 입력된 값을 TIMESTAMP 비교용 문자열로 보정한다.
     */
    private String normalizeDateTime(String value, boolean startOfDay) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim().replace('T', ' ');
        if (text.length() == 10) {
            return text + (startOfDay ? " 00:00:00" : " 23:59:59");
        }
        if (text.length() == 16) {
            return text + ":00";
        }
        return text;
    }
}
