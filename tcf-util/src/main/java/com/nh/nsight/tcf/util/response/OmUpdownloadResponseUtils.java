package com.nh.nsight.tcf.util.response;

import com.nh.nsight.tcf.util.DateTimeUtil;
import com.nh.nsight.tcf.util.meta.CopiedFrom;
import com.nh.nsight.tcf.util.meta.CopiedUtilityFlag;
import com.nh.nsight.tcf.util.meta.UtilCategory;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * tcf-om {@code OmUpdownloadResponseSupport} 복사본.
 */
@CopiedFrom(module = "tcf-om", sourceClass = "OmUpdownloadResponseSupport", category = UtilCategory.RESPONSE)
public final class OmUpdownloadResponseUtils implements CopiedUtilityFlag {

    public static final String COPIED_FROM_MODULE = "tcf-om";
    public static final String COPIED_FROM_CLASS = "OmUpdownloadResponseSupport";

    private OmUpdownloadResponseUtils() {
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
