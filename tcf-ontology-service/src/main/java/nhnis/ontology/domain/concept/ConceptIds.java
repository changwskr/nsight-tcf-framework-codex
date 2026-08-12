package nhnis.ontology.domain.concept;

import java.util.Locale;

/**
 * Stable concept ID factory (collision-safe across systems).
 */
public final class ConceptIds {

    private ConceptIds() {
    }

    public static String system(String code) {
        return "system:" + upper(code);
    }

    public static String business(String system, String business) {
        return "business:" + upper(system) + ":" + upper(business);
    }

    public static String function(String system, String business, String function) {
        return "function:" + upper(system) + ":" + upper(business) + ":" + upper(function);
    }

    public static String program(String system, String business, String function, String programNo) {
        return "program:" + upper(system) + ":" + upper(business) + ":" + upper(function) + ":" + programNo;
    }

    public static String programFromShortId(String programId) {
        // mgcoa8888 → program:MG:CO:A:8888
        if (programId == null || programId.length() != 9) {
            throw new IllegalArgumentException("Invalid programId: " + programId);
        }
        String p = programId.toLowerCase(Locale.ROOT);
        return program(p.substring(0, 2), p.substring(2, 4), p.substring(4, 5), p.substring(5, 9));
    }

    public static String service(String serviceId) {
        return "service:" + serviceId;
    }

    public static String component(String fqcnOrName) {
        return "component:" + fqcnOrName;
    }

    public static String mapper(String mapperKey) {
        return "mapper:" + mapperKey;
    }

    public static String sql(String sqlId) {
        return "sql:" + sqlId;
    }

    public static String table(String schemaOrSystem, String tableName) {
        return "table:" + upper(schemaOrSystem) + ":" + upper(tableName);
    }

    public static String column(String schemaOrSystem, String tableName, String column) {
        return "column:" + upper(schemaOrSystem) + ":" + upper(tableName) + ":" + upper(column);
    }

    public static String runtime(String stepId) {
        return "runtime:" + stepId;
    }

    public static String runtimeThread(String threadId) {
        return "runtime:thread:" + threadId;
    }

    public static String unitOfWork(String name) {
        return "runtime:uow:" + name;
    }

    private static String upper(String v) {
        return v == null ? "" : v.trim().toUpperCase(Locale.ROOT);
    }
}
