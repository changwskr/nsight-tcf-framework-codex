package com.nh.nsight.aimethodology.generator;

import com.nh.nsight.aimethodology.model.BusinessModel;
import com.nh.nsight.aimethodology.model.FieldModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Handler / Facade / Service / Rule / DAO / Mapper / XML 생성.
 * SELECT_ONE criteria 메서드는 항상 model.getAggregateName() 사용.
 */
public final class DomainArtifactGenerator {

    private DomainArtifactGenerator() {
    }

    public static Map<String, String> generateDomainClasses(List<BusinessModel> group) {
        if (group == null || group.isEmpty()) {
            return Map.of();
        }
        BusinessModel first = group.get(0);
        Map<String, String> layout = PackageLayouts.packageLayout(first);
        Map<String, String> names = PackageLayouts.classNames(first);
        String business = first.getBusinessCode();
        String domain = first.getDomainCode();
        Map<String, String> artifacts = new LinkedHashMap<>();

        artifacts.put(PackageLayouts.javaPath(layout.get("handler"), names.get("handler")),
                buildHandler(group, layout, names, business, domain));
        artifacts.put(PackageLayouts.javaPath(layout.get("facade"), names.get("facade")),
                buildFacade(group, layout, names));
        artifacts.put(PackageLayouts.javaPath(layout.get("service"), names.get("service")),
                buildService(group, layout, names));
        artifacts.put(PackageLayouts.javaPath(layout.get("rule"), names.get("rule")),
                buildRule(group, layout, names, business));
        artifacts.put(PackageLayouts.javaPath(layout.get("dao"), names.get("dao")),
                buildDao(group, layout, names));
        artifacts.put(PackageLayouts.javaPath(layout.get("mapper"), names.get("mapper")),
                buildMapper(group, layout, names));

        String statements = group.stream()
                .map(model -> generateSqlStatement(model, layout, names))
                .collect(Collectors.joining("\n\n"));
        String mapperXml = """
                <?xml version="1.0" encoding="UTF-8" ?>
                <!DOCTYPE mapper
                        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
                        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="%s.%s">

                %s

                </mapper>
                """.formatted(layout.get("mapper"), names.get("mapper"), statements);
        String mapperPath = "src/main/resources/mapper/" + business.toLowerCase(Locale.ROOT)
                + "/" + names.get("mapper") + ".xml";
        artifacts.put(mapperPath, mapperXml);
        return artifacts;
    }

    private static String buildHandler(List<BusinessModel> group, Map<String, String> layout,
                                       Map<String, String> names, String business, String domain) {
        List<String> constants = new ArrayList<>();
        List<String> serviceIds = new ArrayList<>();
        List<String> switchCases = new ArrayList<>();
        for (BusinessModel model : group) {
            String constName = toConstantName(model.getMethodName());
            constants.add("    private static final String " + constName + " = \"" + model.getServiceId() + "\";");
            serviceIds.add(constName);
            switchCases.add("            case " + constName + " -> facade." + model.getMethodName()
                    + "(request.getBody(), context);");
        }
        return """
                package %s;

                import %s.%s;
                import com.nh.nsight.tcf.core.support.context.TransactionContext;
                import com.nh.nsight.tcf.core.support.error.BusinessException;
                import com.nh.nsight.tcf.core.support.error.ErrorCode;
                import com.nh.nsight.tcf.core.support.message.StandardRequest;
                import com.nh.nsight.tcf.core.support.transaction.TransactionHandler;
                import java.util.Collection;
                import java.util.List;
                import java.util.Map;
                import org.springframework.stereotype.Component;

                /**
                 * %s %s 도메인 Handler.
                 * 동일 도메인의 ServiceId를 serviceIds()에 일괄 등록한다.
                 */
                @Component
                public class %s implements TransactionHandler {

                %s

                    private final %s facade;

                    public %s(%s facade) {
                        this.facade = facade;
                    }

                    @Override
                    public Collection<String> serviceIds() {
                        return List.of(%s);
                    }

                    @Override
                    public Object doHandle(StandardRequest<Map<String, Object>> request, TransactionContext context) {
                        String serviceId = context.getHeader().getServiceId();
                        return switch (serviceId) {
                %s
                            default -> throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND,
                                    "%s 미지원 serviceId: " + serviceId);
                        };
                    }
                }
                """.formatted(
                layout.get("handler"),
                layout.get("facade"), names.get("facade"),
                business, domain,
                names.get("handler"),
                String.join("\n", constants),
                names.get("facade"),
                names.get("handler"), names.get("facade"),
                String.join(", ", serviceIds),
                String.join("\n", switchCases),
                names.get("handler")
        );
    }

    private static String buildFacade(List<BusinessModel> group, Map<String, String> layout,
                                      Map<String, String> names) {
        Set<String> imports = new TreeSet<>();
        imports.add(layout.get("service") + "." + names.get("service"));
        imports.add("com.nh.nsight.tcf.core.support.context.TransactionContext");
        imports.add("java.util.Map");
        imports.add("org.springframework.stereotype.Service");
        imports.add("org.springframework.transaction.annotation.Transactional");

        List<String> methods = new ArrayList<>();
        for (BusinessModel model : group) {
            Map<String, String> localNames = PackageLayouts.classNames(model);
            imports.add(layout.get("app_dto") + "." + localNames.get("request"));
            String readOnly = model.getOperation() != null && model.getOperation().startsWith("SELECT")
                    ? "true" : "false";
            methods.add("""
                        @Transactional(readOnly = %s, timeout = %d)
                        public Map<String, Object> %s(Map<String, Object> body, TransactionContext context) {
                            %s request = %s.fromMap(body);
                            return service.%s(request, context).toMap();
                        }""".formatted(
                    readOnly,
                    model.timeoutOrDefault(),
                    model.getMethodName(),
                    localNames.get("request"), localNames.get("request"),
                    model.getMethodName()
            ));
        }
        String importsText = imports.stream().map(i -> "import " + i + ";").collect(Collectors.joining("\n"));
        return """
                package %s;

                %s

                @Service
                public class %s {

                    private final %s service;

                    public %s(%s service) {
                        this.service = service;
                    }

                %s
                }
                """.formatted(
                layout.get("facade"),
                importsText,
                names.get("facade"),
                names.get("service"),
                names.get("facade"), names.get("service"),
                String.join("\n\n", methods)
        );
    }

    private static String buildService(List<BusinessModel> group, Map<String, String> layout,
                                       Map<String, String> names) {
        Set<String> imports = new TreeSet<>();
        imports.add(layout.get("rule") + "." + names.get("rule"));
        imports.add(layout.get("dao") + "." + names.get("dao"));
        imports.add("com.nh.nsight.tcf.core.support.context.TransactionContext");
        imports.add("org.springframework.stereotype.Service");

        List<String> methods = new ArrayList<>();
        for (BusinessModel model : group) {
            Map<String, String> n = PackageLayouts.classNames(model);
            imports.add(layout.get("app_dto") + "." + n.get("request"));
            imports.add(layout.get("app_dto") + "." + n.get("response"));
            String operation = model.getOperation();
            if ("SELECT_ONE".equals(operation) || "SELECT_LIST".equals(operation)) {
                imports.add(layout.get("row_dto") + "." + n.get("row"));
            }
            if ("SELECT_LIST".equals(operation)) {
                imports.add("java.util.List");
            }

            // Fix: always use aggregateName (Python SELECT_ONE used wrong n['aggregate'] key)
            String aggregate = model.getAggregateName();
            String body;
            if ("SELECT_ONE".equals(operation)) {
                body = """
                                var criteria = rule.build%sCriteria(request);
                                %s result = dao.%s(criteria);
                                rule.validate%sResult(result);
                                return %s.of(context, result);""".formatted(
                        aggregate, n.get("row"), model.getMethodName(), aggregate, n.get("response"));
            } else if ("SELECT_LIST".equals(operation)) {
                body = """
                                var criteria = rule.build%sCriteria(request);
                                List<%s> results = dao.%s(criteria);
                                return %s.of(context, results);""".formatted(
                        aggregate, n.get("row"), model.getMethodName(), n.get("response"));
            } else {
                body = """
                                rule.validate%sRequest(request);
                                int affectedCount = dao.%s(request);
                                rule.validate%sAffectedCount(affectedCount);
                                return %s.of(context, affectedCount);""".formatted(
                        aggregate, model.getMethodName(), aggregate, n.get("response"));
            }
            methods.add("""
                        public %s %s(%s request, TransactionContext context) {
                %s
                        }""".formatted(n.get("response"), model.getMethodName(), n.get("request"), body));
        }
        String importsText = imports.stream().map(i -> "import " + i + ";").collect(Collectors.joining("\n"));
        return """
                package %s;

                %s

                @Service
                public class %s {
                    private final %s rule;
                    private final %s dao;

                    public %s(%s rule, %s dao) {
                        this.rule = rule;
                        this.dao = dao;
                    }

                %s
                }
                """.formatted(
                layout.get("service"),
                importsText,
                names.get("service"),
                names.get("rule"),
                names.get("dao"),
                names.get("service"), names.get("rule"), names.get("dao"),
                String.join("\n\n", methods)
        );
    }

    private static String buildRule(List<BusinessModel> group, Map<String, String> layout,
                                    Map<String, String> names, String business) {
        Set<String> imports = new TreeSet<>();
        imports.add("com.nh.nsight.tcf.core.support.error.BusinessException");
        imports.add("org.springframework.stereotype.Component");
        imports.add("org.springframework.util.StringUtils");

        List<String> methods = new ArrayList<>();
        for (BusinessModel model : group) {
            Map<String, String> n = PackageLayouts.classNames(model);
            imports.add(layout.get("app_dto") + "." + n.get("request"));
            String operation = model.getOperation();
            String aggregate = model.getAggregateName();
            if ("SELECT_ONE".equals(operation) || "SELECT_LIST".equals(operation)) {
                imports.add(layout.get("app_dto") + "." + n.get("criteria"));
                imports.add(layout.get("row_dto") + "." + n.get("row"));
                String validation = String.join("\n", requiredValidationLines(model));
                String criteriaArgs = criteriaCtorArgs(model);
                String resultMethod = "";
                if ("SELECT_ONE".equals(operation)) {
                    String screenLabel = PackageLayouts.blankToDefault(model.getScreenName(), aggregate);
                    resultMethod = """


                        public void validate%sResult(%s result) {
                            if (result == null || result.isEmpty()) {
                                throw new BusinessException("E-%s-BIZ-0001", "조회된 %s 정보가 없습니다.");
                            }
                        }""".formatted(aggregate, n.get("row"), business, screenLabel);
                }
                methods.add("""
                            public %s build%sCriteria(%s request) {
                                if (request == null) {
                                    throw new BusinessException("E-%s-VAL-0000", "요청 정보가 없습니다.");
                                }
                        %s
                                return new %s(%s);
                            }%s""".formatted(
                        n.get("criteria"), aggregate, n.get("request"),
                        business,
                        validation,
                        n.get("criteria"), criteriaArgs,
                        resultMethod
                ));
            } else {
                String validation = String.join("\n", requiredValidationLines(model));
                methods.add("""
                            public void validate%sRequest(%s request) {
                                if (request == null) {
                                    throw new BusinessException("E-%s-VAL-0000", "요청 정보가 없습니다.");
                                }
                        %s
                            }

                            public void validate%sAffectedCount(int affectedCount) {
                                if (affectedCount != 1) {
                                    throw new BusinessException("E-%s-BIZ-0002", "처리 건수가 올바르지 않습니다: " + affectedCount);
                                }
                            }""".formatted(
                        aggregate, n.get("request"),
                        business,
                        validation,
                        aggregate,
                        business
                ));
            }
        }
        String importsText = imports.stream().map(i -> "import " + i + ";").collect(Collectors.joining("\n"));
        return """
                package %s;

                %s

                @Component
                public class %s {

                %s
                }
                """.formatted(
                layout.get("rule"),
                importsText,
                names.get("rule"),
                String.join("\n\n", methods)
        );
    }

    private static String buildDao(List<BusinessModel> group, Map<String, String> layout,
                                   Map<String, String> names) {
        Set<String> imports = new TreeSet<>();
        imports.add(layout.get("mapper") + "." + names.get("mapper"));
        imports.add("org.springframework.stereotype.Repository");
        List<String> methods = new ArrayList<>();
        for (BusinessModel model : group) {
            Map<String, String> n = PackageLayouts.classNames(model);
            String operation = model.getOperation();
            String returnType;
            String parameterType;
            String parameterName;
            if ("SELECT_ONE".equals(operation) || "SELECT_LIST".equals(operation)) {
                imports.add(layout.get("app_dto") + "." + n.get("criteria"));
                imports.add(layout.get("row_dto") + "." + n.get("row"));
                if ("SELECT_LIST".equals(operation)) {
                    imports.add("java.util.List");
                    returnType = "List<" + n.get("row") + ">";
                } else {
                    returnType = n.get("row");
                }
                parameterType = n.get("criteria");
                parameterName = "criteria";
            } else {
                imports.add(layout.get("app_dto") + "." + n.get("request"));
                returnType = "int";
                parameterType = n.get("request");
                parameterName = "request";
            }
            methods.add("""
                        public %s %s(%s %s) {
                            return mapper.%s(%s);
                        }""".formatted(
                    returnType, model.getMethodName(), parameterType, parameterName,
                    model.getMethodName(), parameterName));
        }
        String importsText = imports.stream().map(i -> "import " + i + ";").collect(Collectors.joining("\n"));
        return """
                package %s;

                %s

                @Repository
                public class %s {
                    private final %s mapper;

                    public %s(%s mapper) {
                        this.mapper = mapper;
                    }

                %s
                }
                """.formatted(
                layout.get("dao"),
                importsText,
                names.get("dao"),
                names.get("mapper"),
                names.get("dao"), names.get("mapper"),
                String.join("\n\n", methods)
        );
    }

    private static String buildMapper(List<BusinessModel> group, Map<String, String> layout,
                                      Map<String, String> names) {
        Set<String> imports = new TreeSet<>();
        imports.add("org.apache.ibatis.annotations.Mapper");
        List<String> methods = new ArrayList<>();
        for (BusinessModel model : group) {
            Map<String, String> n = PackageLayouts.classNames(model);
            String operation = model.getOperation();
            String returnType;
            String parameterType;
            String parameterName;
            if ("SELECT_ONE".equals(operation) || "SELECT_LIST".equals(operation)) {
                imports.add(layout.get("app_dto") + "." + n.get("criteria"));
                imports.add(layout.get("row_dto") + "." + n.get("row"));
                if ("SELECT_LIST".equals(operation)) {
                    imports.add("java.util.List");
                    returnType = "List<" + n.get("row") + ">";
                } else {
                    returnType = n.get("row");
                }
                parameterType = n.get("criteria");
                parameterName = "criteria";
            } else {
                imports.add(layout.get("app_dto") + "." + n.get("request"));
                returnType = "int";
                parameterType = n.get("request");
                parameterName = "request";
            }
            methods.add("    " + returnType + " " + model.getMethodName()
                    + "(" + parameterType + " " + parameterName + ");");
        }
        String importsText = imports.stream().map(i -> "import " + i + ";").collect(Collectors.joining("\n"));
        return """
                package %s;

                %s

                @Mapper
                public interface %s {
                %s
                }
                """.formatted(
                layout.get("mapper"),
                importsText,
                names.get("mapper"),
                String.join("\n\n", methods)
        );
    }

    public static String generateSqlStatement(BusinessModel model, Map<String, String> layout,
                                              Map<String, String> domainNames) {
        // domainNames kept for Python API parity (namespace uses per-model classNames)
        Map<String, String> names = PackageLayouts.classNames(model);
        String operation = model.getOperation();
        String table = model.getTableName();
        int timeout = model.timeoutOrDefault();
        List<FieldModel> conditions = PackageLayouts.conditionFields(model);
        List<FieldModel> responseFields = PackageLayouts.responseFields(model);
        List<FieldModel> requestFields = PackageLayouts.requestFields(model);
        List<FieldModel> writeFields = PackageLayouts.writeFields(model);

        String where;
        if (!conditions.isEmpty()) {
            where = IntStream.range(0, conditions.size())
                    .mapToObj(i -> {
                        FieldModel f = conditions.get(i);
                        String keyword = i == 0 ? "WHERE" : "AND  ";
                        return "             " + keyword + " " + f.getColumn() + " = #{" + f.getName() + "}";
                    })
                    .collect(Collectors.joining("\n"));
        } else {
            where = "             /* TODO: 안전한 WHERE 조건을 정의하십시오. */";
        }

        if ("SELECT_ONE".equals(operation) || "SELECT_LIST".equals(operation)) {
            List<FieldModel> columns = responseFields.isEmpty() ? model.getFields() : responseFields;
            String selectColumns = IntStream.range(0, columns.size())
                    .mapToObj(i -> {
                        FieldModel f = columns.get(i);
                        String padded = String.format("%-30s", f.getColumn());
                        if (i == 0) {
                            return "              " + padded + " AS " + f.getName();
                        }
                        return "            , " + padded + " AS " + f.getName();
                    })
                    .collect(Collectors.joining("\n"));
            return """
                        <!-- SQL_ID: %s / 화면: %s / 이벤트: %s -->
                        <select id="%s"
                                parameterType="%s.%s"
                                resultType="%s.%s"
                                timeout="%d">
                            /* SQL_ID: %s */
                            SELECT
                    %s
                              FROM %s
                    %s
                        </select>""".formatted(
                    model.getServiceId(), model.getScreenId(), model.getEventId(),
                    model.getMethodName(),
                    layout.get("app_dto"), names.get("criteria"),
                    layout.get("row_dto"), names.get("row"),
                    timeout,
                    model.getServiceId(),
                    selectColumns,
                    table,
                    where
            );
        }
        if ("INSERT".equals(operation)) {
            String columns = requestFields.stream().map(FieldModel::getColumn).collect(Collectors.joining(", "));
            String values = requestFields.stream().map(f -> "#{" + f.getName() + "}").collect(Collectors.joining(", "));
            return """
                        <!-- SQL_ID: %s / 화면: %s / 이벤트: %s -->
                        <insert id="%s"
                                parameterType="%s.%s"
                                timeout="%d">
                            /* SQL_ID: %s */
                            INSERT INTO %s (%s)
                            VALUES (%s)
                        </insert>""".formatted(
                    model.getServiceId(), model.getScreenId(), model.getEventId(),
                    model.getMethodName(),
                    layout.get("app_dto"), names.get("request"),
                    timeout,
                    model.getServiceId(),
                    table, columns, values
            );
        }
        if ("UPDATE".equals(operation)) {
            String setLines;
            if (writeFields.isEmpty()) {
                setLines = "              /* TODO: 변경 컬럼 정의 */";
            } else {
                setLines = IntStream.range(0, writeFields.size())
                        .mapToObj(i -> {
                            FieldModel f = writeFields.get(i);
                            if (i == 0) {
                                return "              " + f.getColumn() + " = #{" + f.getName() + "}";
                            }
                            return "            , " + f.getColumn() + " = #{" + f.getName() + "}";
                        })
                        .collect(Collectors.joining("\n"));
            }
            return """
                        <!-- SQL_ID: %s / 화면: %s / 이벤트: %s -->
                        <update id="%s"
                                parameterType="%s.%s"
                                timeout="%d">
                            /* SQL_ID: %s */
                            UPDATE %s
                               SET
                    %s
                    %s
                        </update>""".formatted(
                    model.getServiceId(), model.getScreenId(), model.getEventId(),
                    model.getMethodName(),
                    layout.get("app_dto"), names.get("request"),
                    timeout,
                    model.getServiceId(),
                    table,
                    setLines,
                    where
            );
        }
        return """
                    <!-- SQL_ID: %s / 화면: %s / 이벤트: %s -->
                    <delete id="%s"
                            parameterType="%s.%s"
                            timeout="%d">
                        /* SQL_ID: %s */
                        DELETE FROM %s
                %s
                    </delete>""".formatted(
                model.getServiceId(), model.getScreenId(), model.getEventId(),
                model.getMethodName(),
                layout.get("app_dto"), names.get("request"),
                timeout,
                model.getServiceId(),
                table,
                where
        );
    }

    static List<String> requiredValidationLines(BusinessModel model) {
        return requiredValidationLines(model, "request");
    }

    static List<String> requiredValidationLines(BusinessModel model, String requestVar) {
        List<String> lines = new ArrayList<>();
        for (FieldModel f : PackageLayouts.requestFields(model)) {
            String validation = PackageLayouts.nullToEmpty(f.getValidation()).toLowerCase(Locale.ROOT);
            boolean required = !f.isNullable() || validation.contains("required");
            String getter = requestVar + "." + PackageLayouts.getter(f.getName()) + "()";
            if (!required) {
                continue;
            }
            if ("String".equals(f.getJavaType())) {
                lines.add("        if (!StringUtils.hasText(" + getter + ")) {");
            } else {
                lines.add("        if (" + getter + " == null) {");
            }
            String label = PackageLayouts.blankToDefault(f.getLabel(), f.getName());
            lines.add("            throw new BusinessException(\"E-" + model.getBusinessCode()
                    + "-VAL-0001\", \"" + label + "은(는) 필수입니다.\");");
            lines.add("        }");
            Integer length = f.getLength();
            if ("String".equals(f.getJavaType()) && length != null) {
                lines.add("        if (StringUtils.hasText(" + getter + ") && " + getter + ".length() > "
                        + length + ") {");
                lines.add("            throw new BusinessException(\"E-" + model.getBusinessCode()
                        + "-VAL-0002\", \"" + label + " 길이가 " + length + "자를 초과했습니다.\");");
                lines.add("        }");
            }
        }
        return lines;
    }

    static String criteriaCtorArgs(BusinessModel model) {
        return PackageLayouts.conditionFields(model).stream()
                .map(f -> "request." + PackageLayouts.getter(f.getName()) + "()")
                .collect(Collectors.joining(", "));
    }

    static String toConstantName(String methodName) {
        if (methodName == null || methodName.isEmpty()) {
            return methodName;
        }
        return methodName.replaceAll("(?<!^)(?=[A-Z])", "_").toUpperCase(Locale.ROOT);
    }
}
