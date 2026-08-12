package nhnis.mg.co.a.support;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.util.StringUtils;

/**
 * OM {@code TransactionControlRowSupport} 를 PDMG 네이밍으로 이식.
 * 통제유형 + 대상값 ↔ {@code TB_MG_TX_CONTROL} 저장 Row 변환.
 */
public final class MgTxControlRowSupport {

    public static final String TYPE_GLOBAL = "GLOBAL";
    public static final String TYPE_BUSINESS = "BUSINESS";
    public static final String TYPE_SERVICE = "SERVICE";
    public static final String TYPE_CHANNEL = "CHANNEL";
    public static final String TYPE_BRANCH = "BRANCH";
    public static final String TYPE_USER = "USER";
    public static final String TYPE_IP = "IP";
    public static final String WILDCARD = "*";
    public static final String BLOCK_YES = "Y";
    public static final String BLOCK_NO = "N";

    private MgTxControlRowSupport() {
    }

    public static Map<String, String> toStorageRow(String controlType, String targetValue, String blockYn) {
        String type = normalizeType(controlType);
        Map<String, String> row = new LinkedHashMap<>();
        row.put("serviceId", WILDCARD);
        row.put("transactionCode", WILDCARD);
        row.put("businessCode", WILDCARD);
        row.put("serviceName", WILDCARD);
        row.put("userId", WILDCARD);
        row.put("channelId", WILDCARD);
        row.put("branchId", WILDCARD);

        String target = StringUtils.hasText(targetValue) ? targetValue.trim() : WILDCARD;
        switch (type) {
            case TYPE_GLOBAL -> { /* all wildcard */ }
            case TYPE_BUSINESS -> row.put("businessCode", target);
            case TYPE_SERVICE -> row.put("serviceId", target);
            case TYPE_CHANNEL -> row.put("channelId", target);
            case TYPE_BRANCH -> row.put("branchId", target);
            case TYPE_USER -> row.put("userId", target);
            case TYPE_IP -> row.put("serviceName", target);
            default -> throw new IllegalArgumentException("Unsupported control type: " + controlType);
        }
        row.put("controlType", type);
        row.put("blockYn", StringUtils.hasText(blockYn)
                ? blockYn.trim().toUpperCase(Locale.ROOT)
                : BLOCK_YES);
        return row;
    }

    public static String extractTarget(String controlType, Map<String, ?> row) {
        if (row == null) {
            return "";
        }
        return switch (normalizeType(controlType)) {
            case TYPE_GLOBAL -> WILDCARD;
            case TYPE_BUSINESS -> stringField(row, "businessCode");
            case TYPE_SERVICE -> stringField(row, "serviceId");
            case TYPE_CHANNEL -> stringField(row, "channelId");
            case TYPE_BRANCH -> stringField(row, "branchId");
            case TYPE_USER -> stringField(row, "userId");
            case TYPE_IP -> stringField(row, "serviceName");
            default -> "";
        };
    }

    public static boolean isGlobalRow(Map<String, ?> row) {
        return TYPE_GLOBAL.equals(normalizeType(stringField(row, "controlType")))
                && WILDCARD.equals(stringField(row, "serviceId"));
    }

    private static String stringField(Map<String, ?> row, String key) {
        Object value = row.get(key);
        if (value == null) {
            for (Map.Entry<String, ?> e : row.entrySet()) {
                if (e.getKey() != null && e.getKey().equalsIgnoreCase(key)) {
                    value = e.getValue();
                    break;
                }
            }
        }
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String normalizeType(String controlType) {
        return controlType == null ? "" : controlType.trim().toUpperCase(Locale.ROOT);
    }
}
