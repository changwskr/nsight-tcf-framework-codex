package com.nh.nsight.tcf.core.support.control;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

/** 통제유형 + 대상값 ↔ TCF_TRANSACTION_CONTROL 저장 Row 변환 */
public final class TransactionControlRowSupport {
    private TransactionControlRowSupport() {}

    public static Map<String, String> toStorageRow(String controlType, String targetValue, String blockYn) {
        String type = normalizeType(controlType);
        String wildcard = TcfTransactionControlConstants.GLOBAL_WILDCARD;
        Map<String, String> row = new LinkedHashMap<>();
        row.put("serviceId", wildcard);
        row.put("transactionCode", wildcard);
        row.put("businessCode", wildcard);
        row.put("serviceName", wildcard);
        row.put("userId", wildcard);
        row.put("channelId", wildcard);
        row.put("branchId", wildcard);

        String target = StringUtils.hasText(targetValue) ? targetValue.trim() : wildcard;
        switch (type) {
            case TcfTransactionControlConstants.CONTROL_TYPE_GLOBAL -> { /* all wildcard */ }
            case TcfTransactionControlConstants.CONTROL_TYPE_BUSINESS -> row.put("businessCode", target);
            case TcfTransactionControlConstants.CONTROL_TYPE_SERVICE -> row.put("serviceId", target);
            case TcfTransactionControlConstants.CONTROL_TYPE_CHANNEL -> row.put("channelId", target);
            case TcfTransactionControlConstants.CONTROL_TYPE_BRANCH -> row.put("branchId", target);
            case TcfTransactionControlConstants.CONTROL_TYPE_USER -> row.put("userId", target);
            case TcfTransactionControlConstants.CONTROL_TYPE_IP -> row.put("serviceName", target);
            default -> throw new IllegalArgumentException("Unsupported control type: " + controlType);
        }
        row.put("controlType", type);
        row.put("blockYn", StringUtils.hasText(blockYn)
                ? blockYn.trim().toUpperCase()
                : TcfTransactionControlConstants.BLOCK_YES);
        return row;
    }

    @SuppressWarnings("deprecation")
    public static String extractTarget(String controlType, Map<String, ?> row) {
        if (row == null) {
            return "";
        }
        String type = normalizeType(controlType);
        return switch (type) {
            case TcfTransactionControlConstants.CONTROL_TYPE_GLOBAL -> TcfTransactionControlConstants.GLOBAL_WILDCARD;
            case TcfTransactionControlConstants.CONTROL_TYPE_BUSINESS -> stringField(row, "businessCode");
            case TcfTransactionControlConstants.CONTROL_TYPE_SERVICE -> stringField(row, "serviceId");
            case TcfTransactionControlConstants.CONTROL_TYPE_CHANNEL -> stringField(row, "channelId");
            case TcfTransactionControlConstants.CONTROL_TYPE_BRANCH -> stringField(row, "branchId");
            case TcfTransactionControlConstants.CONTROL_TYPE_USER -> stringField(row, "userId");
            case TcfTransactionControlConstants.CONTROL_TYPE_IP -> stringField(row, "serviceName");
            case TcfTransactionControlConstants.CONTROL_TYPE_FULL -> summarizeFull(row);
            default -> "";
        };
    }

    public static boolean isGlobalRow(Map<String, ?> row) {
        return TcfTransactionControlConstants.CONTROL_TYPE_GLOBAL.equals(normalizeType(stringField(row, "controlType")))
                && TcfTransactionControlConstants.GLOBAL_WILDCARD.equals(stringField(row, "serviceId"));
    }

    private static String summarizeFull(Map<String, ?> row) {
        return stringField(row, "serviceId") + " · " + stringField(row, "userId");
    }

    private static String stringField(Map<String, ?> row, String key) {
        Object value = row.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String normalizeType(String controlType) {
        return controlType == null ? "" : controlType.trim().toUpperCase();
    }
}
