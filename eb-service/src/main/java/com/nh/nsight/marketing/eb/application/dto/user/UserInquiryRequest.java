package com.nh.nsight.marketing.eb.application.dto.user;

import java.util.Map;

public class UserInquiryRequest {

    private final Integer pageNo;
    private final Integer pageSize;
    private final String userId;
    private final String userName;
    private final String branchId;

    public UserInquiryRequest(Integer pageNo, Integer pageSize, String userId, String userName, String branchId) {
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.userId = userId;
        this.userName = userName;
        this.branchId = branchId;
    }

    public static UserInquiryRequest fromMap(Map<String, Object> body) {
        Map<String, Object> safeBody = body != null ? body : Map.of();
        return new UserInquiryRequest(
                toInteger(safeBody.get("pageNo")),
                toInteger(safeBody.get("pageSize")),
                trimToNull(safeBody.get("userId")),
                trimToNull(safeBody.get("userName")),
                trimToNull(safeBody.get("branchId")));
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getBranchId() {
        return branchId;
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String trimToNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
