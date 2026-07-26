package com.nh.nsight.marketing.ep.application.rule;

import com.nh.nsight.marketing.ep.application.dto.userevent.UserEventInquiryRequest;
import com.nh.nsight.marketing.ep.application.dto.userevent.UserEventReceiveRequest;
import com.nh.nsight.marketing.ep.application.dto.userevent.UserEventSearchCriteria;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class EpUserEventRule {
    public static final int DEFAULT_PAGE_NO = 1;
    public static final int DEFAULT_PAGE_SIZE = 100;
    public static final int MAX_PAGE_SIZE_LOG = 1000;

    public void validateReceive(UserEventReceiveRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "요청 Body가 없습니다.");
        }
        if (!StringUtils.hasText(request.getEventId())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "필수 필드 누락: eventId");
        }
        if (!StringUtils.hasText(request.getUserId())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "필수 필드 누락: userId");
        }
        if (!StringUtils.hasText(request.getEventType())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "필수 필드 누락: eventType");
        }
    }

    public void validateInquiry(UserEventInquiryRequest request) {
        // 목록 조회 — body·검색조건 모두 선택
    }

    public UserEventSearchCriteria buildSearchCriteria(UserEventInquiryRequest request) {
        UserEventInquiryRequest safe = request != null ? request : UserEventInquiryRequest.fromMap(Map.of());
        UserEventSearchCriteria criteria = new UserEventSearchCriteria();
        normalizePaging(criteria, safe);
        criteria.setEventId(safe.getEventId());
        criteria.setUserId(safe.getUserId());
        criteria.setEventType(safe.getEventType());
        return criteria;
    }

    private void normalizePaging(UserEventSearchCriteria criteria, UserEventInquiryRequest request) {
        int pageNo = Math.max(1, toInt(request.getPageNo(), DEFAULT_PAGE_NO));
        int pageSize = toInt(request.getPageSize(), DEFAULT_PAGE_SIZE);
        if (pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        if (pageSize > MAX_PAGE_SIZE_LOG) {
            pageSize = MAX_PAGE_SIZE_LOG;
        }
        criteria.setPageNo(pageNo);
        criteria.setPageSize(pageSize);
        criteria.setOffset((pageNo - 1) * pageSize);
    }

    private int toInt(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }
}
