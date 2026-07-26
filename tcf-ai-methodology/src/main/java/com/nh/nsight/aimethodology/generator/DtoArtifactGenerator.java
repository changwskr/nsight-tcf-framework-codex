package com.nh.nsight.aimethodology.generator;

import com.nh.nsight.aimethodology.model.BusinessModel;
import com.nh.nsight.aimethodology.model.FieldModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Request / Criteria / Row / Response DTO 생성 (Python DTO generators).
 */
public final class DtoArtifactGenerator {

    private DtoArtifactGenerator() {
    }

    public static Map.Entry<String, String> generateRequestDto(BusinessModel model) {
        Map<String, String> layout = PackageLayouts.packageLayout(model);
        Map<String, String> names = PackageLayouts.classNames(model);
        List<FieldModel> fields = PackageLayouts.requestFields(model);
        Set<String> imports = new TreeSet<>();
        imports.add("java.util.Map");
        imports.addAll(PackageLayouts.fieldImports(fields));

        String packageName = layout.get("app_dto");
        String className = names.get("request");

        String fieldDecls = fields.stream()
                .map(f -> "    private final " + f.getJavaType() + " " + f.getName() + ";")
                .collect(Collectors.joining("\n"));
        String ctorParams = fields.stream()
                .map(f -> f.getJavaType() + " " + f.getName())
                .collect(Collectors.joining(", "));
        String ctorAssign = fields.stream()
                .map(f -> "        this." + f.getName() + " = " + f.getName() + ";")
                .collect(Collectors.joining("\n"));

        List<String> fromMapValues = new ArrayList<>();
        for (FieldModel field : fields) {
            String sourceExpr = "body.get(\"" + field.getName() + "\")";
            fromMapValues.add("                "
                    + PackageLayouts.javaDefaultConversion(field.getJavaType(), sourceExpr));
        }
        String fromMapArgs = String.join(",\n", fromMapValues);
        String getters = fields.stream()
                .map(f -> "    public " + f.getJavaType() + " " + PackageLayouts.getter(f.getName())
                        + "() {\n        return " + f.getName() + ";\n    }")
                .collect(Collectors.joining("\n\n"));

        Set<String> usedTypes = fields.stream()
                .map(FieldModel::getJavaType)
                .collect(Collectors.toSet());
        String conversionMethods = buildConversionMethods(usedTypes);

        String importsText = imports.stream()
                .map(item -> "import " + item + ";")
                .collect(Collectors.joining("\n"));
        String nullArgs = fields.stream().map(f -> "null").collect(Collectors.joining(", "));

        StringBuilder fromMap = new StringBuilder();
        fromMap.append("    public static ").append(className).append(" fromMap(Map<String, Object> body) {\n");
        fromMap.append("        if (body == null) {\n");
        fromMap.append("            return new ").append(className).append("(").append(nullArgs).append(");\n");
        fromMap.append("        }\n");
        fromMap.append("        return new ").append(className).append("(\n");
        if (!fromMapArgs.isEmpty()) {
            fromMap.append(fromMapArgs);
        }
        fromMap.append(");\n");
        fromMap.append("    }");

        String code = "package " + packageName + ";\n\n"
                + importsText + "\n\n"
                + "/**\n"
                + " * " + PackageLayouts.nullToEmpty(model.getServiceId()) + " 요청 body.\n"
                + " * 자동생성 파일: 업무 규칙은 Rule에 구현한다.\n"
                + " */\n"
                + "public class " + className + " {\n\n"
                + fieldDecls + "\n\n"
                + "    public " + className + "(" + ctorParams + ") {\n"
                + ctorAssign + "\n"
                + "    }\n\n"
                + fromMap + "\n\n"
                + getters + "\n\n"
                + conversionMethods + "\n"
                + "}\n";

        return Map.entry(PackageLayouts.javaPath(packageName, className), code);
    }

    public static Map.Entry<String, String> generateCriteriaDto(BusinessModel model) {
        Map<String, String> layout = PackageLayouts.packageLayout(model);
        Map<String, String> names = PackageLayouts.classNames(model);
        List<FieldModel> fields = PackageLayouts.conditionFields(model);
        String packageName = layout.get("app_dto");
        String className = names.get("criteria");
        String importsText = PackageLayouts.fieldImports(fields).stream()
                .map(item -> "import " + item + ";")
                .collect(Collectors.joining("\n"));
        String fieldDecls = fields.stream()
                .map(f -> "    private final " + f.getJavaType() + " " + f.getName() + ";")
                .collect(Collectors.joining("\n"));
        String ctorParams = fields.stream()
                .map(f -> f.getJavaType() + " " + f.getName())
                .collect(Collectors.joining(", "));
        String ctorAssign = fields.stream()
                .map(f -> "        this." + f.getName() + " = " + f.getName() + ";")
                .collect(Collectors.joining("\n"));
        String getters = fields.stream()
                .map(f -> "    public " + f.getJavaType() + " " + PackageLayouts.getter(f.getName())
                        + "() {\n        return " + f.getName() + ";\n    }")
                .collect(Collectors.joining("\n\n"));

        StringBuilder code = new StringBuilder();
        code.append("package ").append(packageName).append(";\n\n");
        if (!importsText.isEmpty()) {
            code.append(importsText).append("\n\n");
        }
        code.append("/**\n");
        code.append(" * ").append(PackageLayouts.nullToEmpty(model.getServiceId())).append(" DAO/MyBatis 조건.\n");
        code.append(" */\n");
        code.append("public class ").append(className).append(" {\n\n");
        code.append(fieldDecls).append("\n\n");
        code.append("    public ").append(className).append("(").append(ctorParams).append(") {\n");
        code.append(ctorAssign).append("\n");
        code.append("    }\n\n");
        code.append(getters).append("\n");
        code.append("}\n");
        return Map.entry(PackageLayouts.javaPath(packageName, className), code.toString());
    }

    public static Map.Entry<String, String> generateRowDto(BusinessModel model) {
        Map<String, String> layout = PackageLayouts.packageLayout(model);
        Map<String, String> names = PackageLayouts.classNames(model);
        List<FieldModel> fields = PackageLayouts.responseFields(model);
        if (fields.isEmpty()) {
            fields = new ArrayList<>(model.getFields());
        }
        String packageName = layout.get("row_dto");
        String className = names.get("row");
        Set<String> imports = new TreeSet<>();
        imports.add("java.util.LinkedHashMap");
        imports.add("java.util.Map");
        imports.addAll(PackageLayouts.fieldImports(fields));
        String importsText = imports.stream()
                .map(item -> "import " + item + ";")
                .collect(Collectors.joining("\n"));
        String fieldDecls = fields.stream()
                .map(f -> "    private " + f.getJavaType() + " " + f.getName() + ";")
                .collect(Collectors.joining("\n"));

        FieldModel emptyField = fields.stream().filter(FieldModel::isPk).findFirst()
                .orElse(fields.isEmpty() ? null : fields.get(0));
        String emptyExpr;
        if (emptyField == null) {
            emptyExpr = "true";
        } else {
            emptyExpr = emptyField.getName() + " == null";
            if ("String".equals(emptyField.getJavaType())) {
                emptyExpr += " || " + emptyField.getName() + ".isBlank()";
            }
        }
        String toMap = fields.stream()
                .map(f -> "        map.put(\"" + f.getName() + "\", " + f.getName() + ");")
                .collect(Collectors.joining("\n"));
        String accessors = fields.stream()
                .map(f -> "    public " + f.getJavaType() + " " + PackageLayouts.getter(f.getName())
                        + "() {\n        return " + f.getName() + ";\n    }\n\n"
                        + "    public void " + PackageLayouts.setter(f.getName()) + "("
                        + f.getJavaType() + " " + f.getName() + ") {\n"
                        + "        this." + f.getName() + " = " + f.getName() + ";\n    }")
                .collect(Collectors.joining("\n\n"));

        String code = "package " + packageName + ";\n\n"
                + importsText + "\n\n"
                + "/**\n"
                + " * " + PackageLayouts.nullToEmpty(model.getTableName()) + " 조회 Row (MyBatis result).\n"
                + " */\n"
                + "public class " + className + " {\n\n"
                + fieldDecls + "\n\n"
                + "    public boolean isEmpty() {\n"
                + "        return " + emptyExpr + ";\n"
                + "    }\n\n"
                + "    public Map<String, Object> toMap() {\n"
                + "        Map<String, Object> map = new LinkedHashMap<>();\n"
                + toMap + "\n"
                + "        return map;\n"
                + "    }\n\n"
                + accessors + "\n"
                + "}\n";
        return Map.entry(PackageLayouts.javaPath(packageName, className), code);
    }

    public static Map.Entry<String, String> generateResponseDto(BusinessModel model) {
        Map<String, String> layout = PackageLayouts.packageLayout(model);
        Map<String, String> names = PackageLayouts.classNames(model);
        String packageName = layout.get("app_dto");
        String className = names.get("response");
        String rowImport = layout.get("row_dto") + "." + names.get("row");
        String operation = model.getOperation();

        Set<String> imports = new TreeSet<>();
        imports.add("com.nh.nsight.tcf.core.support.context.TransactionContext");
        imports.add("java.util.LinkedHashMap");
        imports.add("java.util.Map");
        if ("SELECT_ONE".equals(operation) || "SELECT_LIST".equals(operation)) {
            imports.add(rowImport);
        }
        if ("SELECT_LIST".equals(operation)) {
            imports.add("java.util.List");
            imports.add("java.util.stream.Collectors");
        }
        String importsText = imports.stream()
                .map(item -> "import " + item + ";")
                .collect(Collectors.joining("\n"));

        String business = PackageLayouts.nullToEmpty(model.getBusinessCode());
        String payloadDecl;
        String ctorPayload;
        String ctorAssign;
        String factoryPayload;
        String factoryCall;
        String mapPayload;

        if ("SELECT_ONE".equals(operation)) {
            payloadDecl = "    private final " + names.get("row") + " result;";
            ctorPayload = ", " + names.get("row") + " result";
            ctorAssign = "        this.result = result;";
            factoryPayload = ", " + names.get("row") + " result";
            factoryCall = ", result";
            mapPayload = """
                        if (result != null) {
                            body.putAll(result.toMap());
                        }""";
        } else if ("SELECT_LIST".equals(operation)) {
            payloadDecl = "    private final List<" + names.get("row") + "> results;";
            ctorPayload = ", List<" + names.get("row") + "> results";
            ctorAssign = "        this.results = results == null ? List.of() : List.copyOf(results);";
            factoryPayload = ", List<" + names.get("row") + "> results";
            factoryCall = ", results";
            mapPayload = """
                        body.put("items", results.stream().map(result -> result.toMap()).collect(Collectors.toList()));
                        body.put("count", results.size());""";
        } else {
            payloadDecl = "    private final int affectedCount;";
            ctorPayload = ", int affectedCount";
            ctorAssign = "        this.affectedCount = affectedCount;";
            factoryPayload = ", int affectedCount";
            factoryCall = ", affectedCount";
            mapPayload = "        body.put(\"affectedCount\", affectedCount);";
        }

        String code = """
                package %s;

                %s

                /**
                 * %s 응답 body.
                 */
                public class %s {

                    private final String businessCode;
                    private final String serviceId;
                    private final String guid;
                %s

                    public %s(String businessCode, String serviceId, String guid%s) {
                        this.businessCode = businessCode;
                        this.serviceId = serviceId;
                        this.guid = guid;
                %s
                    }

                    public static %s of(TransactionContext context%s) {
                        return new %s("%s", context.getHeader().getServiceId(), context.getHeader().getGuid()%s);
                    }

                    public Map<String, Object> toMap() {
                        Map<String, Object> body = new LinkedHashMap<>();
                        body.put("businessCode", businessCode);
                        body.put("serviceId", serviceId);
                        body.put("guid", guid);
                %s
                        return body;
                    }
                }
                """.formatted(
                packageName,
                importsText,
                PackageLayouts.nullToEmpty(model.getServiceId()),
                className,
                payloadDecl,
                className,
                ctorPayload,
                ctorAssign,
                className,
                factoryPayload,
                className,
                business,
                factoryCall,
                mapPayload
        );
        return Map.entry(PackageLayouts.javaPath(packageName, className), code);
    }

    private static String buildConversionMethods(Set<String> usedTypes) {
        Map<String, String> blocks = new LinkedHashMap<>();
        blocks.put("String", """
                    private static String stringValue(Object value) {
                        return value == null ? null : String.valueOf(value).trim();
                    }""");
        blocks.put("Integer", """
                    private static Integer integerValue(Object value) {
                        String text = stringValue(value);
                        return text == null || text.isBlank() ? null : Integer.valueOf(text);
                    }""");
        blocks.put("Long", """
                    private static Long longValue(Object value) {
                        String text = stringValue(value);
                        return text == null || text.isBlank() ? null : Long.valueOf(text);
                    }""");
        blocks.put("BigDecimal", """
                    private static BigDecimal decimalValue(Object value) {
                        String text = stringValue(value);
                        return text == null || text.isBlank() ? null : new BigDecimal(text);
                    }""");
        blocks.put("Boolean", """
                    private static Boolean booleanValue(Object value) {
                        String text = stringValue(value);
                        return text == null || text.isBlank() ? null : Boolean.valueOf(text);
                    }""");
        blocks.put("LocalDate", """
                    private static LocalDate localDateValue(Object value) {
                        String text = stringValue(value);
                        return text == null || text.isBlank() ? null : LocalDate.parse(text);
                    }""");
        blocks.put("LocalDateTime", """
                    private static LocalDateTime localDateTimeValue(Object value) {
                        String text = stringValue(value);
                        return text == null || text.isBlank() ? null : LocalDateTime.parse(text);
                    }""");

        List<String> methods = new ArrayList<>();
        methods.add(blocks.get("String"));
        for (String type : List.of("Integer", "Long", "BigDecimal", "Boolean", "LocalDate", "LocalDateTime")) {
            if (usedTypes.contains(type)) {
                methods.add(blocks.get(type));
            }
        }
        return String.join("\n\n", methods);
    }
}
