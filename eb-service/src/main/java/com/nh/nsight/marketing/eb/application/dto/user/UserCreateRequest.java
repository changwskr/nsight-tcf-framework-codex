package com.nh.nsight.marketing.eb.application.dto.user;

import java.util.Map;

public class UserCreateRequest {

    private final String userId;
    private final String userName;
    private final String branchId;

    public UserCreateRequest(String userId, String userName, String branchId) {
        this.userId = userId;
        this.userName = userName;
        this.branchId = branchId;
    }

    public static UserCreateRequest fromMap(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        return new UserCreateRequest(
                String.valueOf(body.get("userId")).trim(),
                String.valueOf(body.get("userName")).trim(),
                body.get("branchId") != null ? String.valueOf(body.get("branchId")).trim() : null);
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
}
