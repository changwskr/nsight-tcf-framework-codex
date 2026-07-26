package com.nh.nsight.marketing.eb.application.rule;

import com.nh.nsight.marketing.eb.application.dto.user.UserCreateRequest;
import com.nh.nsight.marketing.eb.application.dto.user.UserInquiryRequest;
import com.nh.nsight.marketing.eb.application.dto.user.UserSearchCriteria;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class EbUserRule {
    private static final int MAX_PAGE_SIZE = 100;

    public void validateInquiry(UserInquiryRequest request) {
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

    public UserSearchCriteria buildSearchCriteria(UserInquiryRequest request) {
        UserInquiryRequest safe = request != null ? request : UserInquiryRequest.fromMap(Map.of());
        int pageNo = safe.getPageNo() != null ? safe.getPageNo() : 1;
        int pageSize = safe.getPageSize() != null ? safe.getPageSize() : 20;
        if (pageNo < 1) {
            pageNo = 1;
        }
        if (pageSize < 1) {
            pageSize = 20;
        }

        UserSearchCriteria criteria = new UserSearchCriteria();
        criteria.setPageNo(pageNo);
        criteria.setPageSize(pageSize);
        criteria.setOffset((pageNo - 1) * pageSize);
        criteria.setUserId(safe.getUserId());
        criteria.setUserName(safe.getUserName());
        criteria.setBranchId(safe.getBranchId());
        return criteria;
    }

    public void validateCreate(UserCreateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "요청 Body가 없습니다.");
        }
        if (!StringUtils.hasText(request.getUserId())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "필수 필드 누락: userId");
        }
        if (!StringUtils.hasText(request.getUserName())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "필수 필드 누락: userName");
        }
    }
}
