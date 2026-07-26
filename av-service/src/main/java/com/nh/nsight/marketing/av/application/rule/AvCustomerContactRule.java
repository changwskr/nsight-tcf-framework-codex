package com.nh.nsight.marketing.av.application.rule;

import com.nh.nsight.marketing.av.application.dto.customercontact.CustomerContactSearchCriteria;
import com.nh.nsight.marketing.av.application.dto.customercontact.CustomerContactSelectDetailRequest;
import com.nh.nsight.marketing.av.application.dto.customercontact.CustomerContactSelectListRequest;
import com.nh.nsight.marketing.av.persistence.dto.customercontact.CustomerContactRow;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AvCustomerContactRule {
    private static final int DEFAULT_PAGE_SIZE = 15;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String MASK = "****";

    public void validateSelectList(CustomerContactSelectListRequest request) {
        CustomerContactSelectListRequest safe =
                request != null ? request : CustomerContactSelectListRequest.fromMap(Map.of());
        if (safe.getCustomerNo() == null) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR, "AV-CCT-V001: customerNo는 필수입니다.");
        }
        int pageSize = safe.getPageSize() != null ? safe.getPageSize() : DEFAULT_PAGE_SIZE;
        if (pageSize > MAX_PAGE_SIZE) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR, "pageSize는 최대 " + MAX_PAGE_SIZE + " 입니다.");
        }
        int pageNo = safe.getPageNo() != null ? safe.getPageNo() : 1;
        if (pageNo < 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "pageNo는 1 이상이어야 합니다.");
        }
    }

    public CustomerContactSearchCriteria buildSearchCriteria(CustomerContactSelectListRequest request) {
        CustomerContactSelectListRequest safe =
                request != null ? request : CustomerContactSelectListRequest.fromMap(Map.of());
        int pageNo = safe.getPageNo() != null ? safe.getPageNo() : 1;
        int pageSize = safe.getPageSize() != null ? safe.getPageSize() : DEFAULT_PAGE_SIZE;
        if (pageNo < 1) {
            pageNo = 1;
        }
        if (pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }

        CustomerContactSearchCriteria criteria = new CustomerContactSearchCriteria();
        criteria.setPageNo(pageNo);
        criteria.setPageSize(pageSize);
        criteria.setOffset((pageNo - 1) * pageSize);
        criteria.setCustomerNo(safe.getCustomerNo());
        criteria.setContactType(safe.getContactType());
        return criteria;
    }

    public void validateSelectDetail(CustomerContactSelectDetailRequest request) {
        if (request == null || request.getContactId() == null) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR, "AV-CCT-V002: contactId는 필수입니다.");
        }
    }

    public void requireExists(CustomerContactRow row, String contactId) {
        if (row == null) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR, "AV-CCT-E404: 연락처를 찾을 수 없습니다. contactId=" + contactId);
        }
    }

    /** C11 확정: 목록 응답의 CONTACT_VALUE는 개인정보이므로 마스킹해서 내린다. */
    public List<Map<String, Object>> toMaskedRows(List<CustomerContactRow> rows) {
        List<Map<String, Object>> masked = new ArrayList<>();
        if (rows == null) {
            return masked;
        }
        for (CustomerContactRow row : rows) {
            Map<String, Object> map = row.toMap();
            map.put("contactValue", maskContactValue(row.getContactValue()));
            masked.add(map);
        }
        return masked;
    }

    public String maskContactValue(String contactValue) {
        if (contactValue == null || contactValue.isBlank()) {
            return contactValue;
        }
        String value = contactValue.trim();
        int at = value.indexOf('@');
        if (at > 0) {
            String local = value.substring(0, at);
            String domain = value.substring(at);
            return local.charAt(0) + MASK + domain;
        }
        String[] parts = value.split("-");
        if (parts.length >= 3) {
            parts[parts.length - 2] = MASK;
            return String.join("-", parts);
        }
        if (value.length() <= 4) {
            return MASK;
        }
        return value.substring(0, 2) + MASK + value.substring(value.length() - 2);
    }
}
