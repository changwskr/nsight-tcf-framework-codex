package com.nh.nsight.aimethodology.generator;

import com.nh.nsight.aimethodology.model.BusinessModel;
import com.nh.nsight.aimethodology.model.FieldModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 패키지 레이아웃·클래스명·필드 선택 등 생성기 공통 유틸 (Python generator.py helpers).
 */
public final class PackageLayouts {

    private static final Map<String, String> JAVA_IMPORTS = Map.of(
            "BigDecimal", "java.math.BigDecimal",
            "LocalDate", "java.time.LocalDate",
            "LocalDateTime", "java.time.LocalDateTime"
    );

    private PackageLayouts() {
    }

    public static String lowerFirst(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    public static String upperFirst(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    public static String bizClass(String code) {
        if (code == null || code.isEmpty()) {
            return code;
        }
        return Character.toUpperCase(code.charAt(0)) + code.substring(1).toLowerCase();
    }

    public static String javaPath(String packageName, String className) {
        return "src/main/java/" + packageName.replace('.', '/') + "/" + className + ".java";
    }

    public static String testJavaPath(String packageName, String className) {
        return "src/test/java/" + packageName.replace('.', '/') + "/" + className + ".java";
    }

    public static Map<String, String> packageLayout(BusinessModel model) {
        String base = blankToDefault(model.getBasePackage(), "com.nh.nsight.marketing").trim();
        String biz = blankToDefault(model.getBusinessCode(), "SV").toLowerCase();
        String domain = blankToDefault(model.getDomainCode(), "Customer");
        String domainLower = lowerFirst(domain);
        String profile = blankToDefault(model.getPackageProfile(), "CURRENT_SOURCE");

        Map<String, String> layout = new LinkedHashMap<>();
        if ("DOMAIN_FIRST".equals(profile)) {
            String root = base + "." + biz + "." + domainLower;
            layout.put("handler", root + ".handler");
            layout.put("facade", root + ".facade");
            layout.put("service", root + ".service");
            layout.put("rule", root + ".rule");
            layout.put("app_dto", root + ".dto");
            layout.put("dao", root + ".dao");
            layout.put("row_dto", root + ".dto");
            layout.put("mapper", root + ".mapper");
            return layout;
        }
        String root = base + "." + biz;
        layout.put("handler", root + ".entry.handler");
        layout.put("facade", root + ".entry.facade");
        layout.put("service", root + ".application.service");
        layout.put("rule", root + ".application.rule");
        layout.put("app_dto", root + ".application.dto." + domainLower);
        layout.put("dao", root + ".persistence.dao");
        layout.put("row_dto", root + ".persistence.dto." + domainLower);
        layout.put("mapper", root + ".persistence.mapper");
        return layout;
    }

    public static Map<String, String> classNames(BusinessModel model) {
        String prefix = bizClass(blankToDefault(model.getBusinessCode(), "SV"));
        String domain = blankToDefault(model.getDomainCode(), "Customer");
        String aggregate = blankToDefault(model.getAggregateName(), domain + "UseCase");
        Map<String, String> names = new LinkedHashMap<>();
        names.put("handler", prefix + domain + "Handler");
        names.put("facade", prefix + domain + "Facade");
        names.put("service", prefix + domain + "Service");
        names.put("rule", prefix + domain + "Rule");
        names.put("dao", prefix + domain + "Dao");
        names.put("mapper", prefix + domain + "Mapper");
        names.put("request", aggregate + "Request");
        names.put("criteria", aggregate + "Criteria");
        names.put("response", aggregate + "Response");
        names.put("row", aggregate + "Row");
        return names;
    }

    public static List<String> fieldImports(List<FieldModel> fields) {
        Set<String> imports = new TreeSet<>();
        for (FieldModel field : fields) {
            String type = blankToDefault(field.getJavaType(), "String");
            String importName = JAVA_IMPORTS.get(type);
            if (importName != null) {
                imports.add(importName);
            }
        }
        return new ArrayList<>(imports);
    }

    public static String getter(String fieldName) {
        return "get" + upperFirst(fieldName);
    }

    public static String setter(String fieldName) {
        return "set" + upperFirst(fieldName);
    }

    public static String javaDefaultConversion(String javaType, String sourceExpr) {
        return switch (javaType) {
            case "String" -> "stringValue(" + sourceExpr + ")";
            case "Integer" -> "integerValue(" + sourceExpr + ")";
            case "Long" -> "longValue(" + sourceExpr + ")";
            case "BigDecimal" -> "decimalValue(" + sourceExpr + ")";
            case "Boolean" -> "booleanValue(" + sourceExpr + ")";
            case "LocalDate" -> "localDateValue(" + sourceExpr + ")";
            case "LocalDateTime" -> "localDateTimeValue(" + sourceExpr + ")";
            default -> "(" + javaType + ") " + sourceExpr;
        };
    }

    public static List<FieldModel> requestFields(BusinessModel model) {
        return model.getFields().stream()
                .filter(f -> f.isRequest() || f.isCondition())
                .collect(Collectors.toList());
    }

    public static List<FieldModel> conditionFields(BusinessModel model) {
        return model.getFields().stream()
                .filter(FieldModel::isCondition)
                .collect(Collectors.toList());
    }

    public static List<FieldModel> responseFields(BusinessModel model) {
        return model.getFields().stream()
                .filter(FieldModel::isResponse)
                .collect(Collectors.toList());
    }

    public static List<FieldModel> writeFields(BusinessModel model) {
        List<FieldModel> fields = model.getFields().stream()
                .filter(FieldModel::isRequest)
                .collect(Collectors.toList());
        if ("UPDATE".equals(model.getOperation())) {
            Set<String> conditionNames = conditionFields(model).stream()
                    .map(FieldModel::getName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            return fields.stream()
                    .filter(f -> !conditionNames.contains(f.getName()) && !f.isPk())
                    .collect(Collectors.toList());
        }
        return fields;
    }

    public static String blankToDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    public static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
