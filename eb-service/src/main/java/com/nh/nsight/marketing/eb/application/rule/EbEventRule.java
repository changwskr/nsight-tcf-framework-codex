package com.nh.nsight.marketing.eb.application.rule;

import com.nh.nsight.marketing.eb.application.dto.event.EventInquiryRequest;
import com.nh.nsight.marketing.eb.application.dto.event.EventSearchCriteria;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class EbEventRule {
    private static final int MAX_PAGE_SIZE = 100;

    public void validateInquiry(EventInquiryRequest request) {
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

    public EventSearchCriteria buildSearchCriteria(EventInquiryRequest request) {
        EventInquiryRequest safe = request != null ? request : EventInquiryRequest.fromMap(Map.of());
        int pageNo = safe.getPageNo() != null ? safe.getPageNo() : 1;
        int pageSize = safe.getPageSize() != null ? safe.getPageSize() : 20;
        if (pageNo < 1) {
            pageNo = 1;
        }
        if (pageSize < 1) {
            pageSize = 20;
        }

        EventSearchCriteria criteria = new EventSearchCriteria();
        criteria.setPageNo(pageNo);
        criteria.setPageSize(pageSize);
        criteria.setOffset((pageNo - 1) * pageSize);
        criteria.setEventId(safe.getEventId());
        criteria.setUserId(safe.getUserId());
        criteria.setEventType(safe.getEventType());
        criteria.setEventStatus(safe.getEventStatus());
        return criteria;
    }
}
