package com.nh.nsight.marketing.om.support;

import com.nh.nsight.tcf.util.DateTimeUtil;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OmUpdownloadResponseSupport {
    private OmUpdownloadResponseSupport() {
    }

    public static Map<String, Object> success(Map<String, Object> body) {
        Map<String, Object> response = new LinkedHashMap<>();
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("businessCode", "UD");
        header.put("requestTime", DateTimeUtil.nowKst());
        response.put("header", header);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("resultCode", "S0000");
        result.put("resultMessage", "정상 처리되었습니다.");
        response.put("result", result);
        response.put("body", body);
        return response;
    }

    public static Map<String, Object> fail(String errorCode, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("resultCode", errorCode);
        result.put("resultMessage", message);
        result.put("status", "ERROR");
        response.put("result", result);
        response.put("body", Map.of("error", message));
        return response;
    }
}
