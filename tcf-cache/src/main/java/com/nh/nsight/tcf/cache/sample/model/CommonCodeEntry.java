package com.nh.nsight.tcf.cache.sample.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 공통코드 샘플 모델 — OM {@code OM_COMMON_CODE} 행과 동일한 필드 구성.
 * 업무 모듈에서 복사·확장해 사용합니다.
 */
public record CommonCodeEntry(
        String codeGroup,
        String code,
        String codeName,
        int sortOrder,
        String useYn,
        String description) {

    public CommonCodeEntry {
        if (codeGroup == null || codeGroup.isBlank()) {
            throw new IllegalArgumentException("codeGroup is required");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code is required");
        }
        if (codeName == null) {
            codeName = "";
        }
        if (useYn == null || useYn.isBlank()) {
            useYn = "Y";
        }
        if (description == null) {
            description = "";
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("codeGroup", codeGroup);
        row.put("code", code);
        row.put("codeName", codeName);
        row.put("sortOrder", sortOrder);
        row.put("useYn", useYn);
        row.put("description", description);
        return row;
    }

    public static CommonCodeEntry fromMap(Map<String, Object> row) {
        return new CommonCodeEntry(
                stringValue(row, "codeGroup"),
                stringValue(row, "code"),
                stringValue(row, "codeName"),
                intValue(row, "sortOrder"),
                stringValue(row, "useYn"),
                stringValue(row, "description"));
    }

    private static String stringValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static int intValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
