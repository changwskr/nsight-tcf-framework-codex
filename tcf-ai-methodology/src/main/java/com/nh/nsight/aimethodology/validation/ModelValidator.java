package com.nh.nsight.aimethodology.validation;

import com.nh.nsight.aimethodology.model.BusinessModel;
import com.nh.nsight.aimethodology.model.FieldModel;
import com.nh.nsight.aimethodology.model.ValidationIssue;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Python validators.py 포팅. validateModel / validateWorkspace / hasErrors.
 */
@Component
public class ModelValidator {

    private static final Pattern BUSINESS_CODE_RE = Pattern.compile("^[A-Z]{2,3}$");
    private static final Pattern SCREEN_ID_RE = Pattern.compile("^[A-Z]{2,3}-[A-Z0-9]{2,5}-\\d{4}$");
    private static final Pattern EVENT_ID_RE = Pattern.compile("^[A-Z]{2,3}-[A-Z0-9]{2,5}-\\d{4}-E\\d{2}$");
    private static final Pattern SERVICE_ID_RE = Pattern.compile("^[A-Z]{2,3}\\.[A-Z][A-Za-z0-9]*\\.[a-z][A-Za-z0-9]*$");
    private static final Pattern TRANSACTION_CODE_RE = Pattern.compile("^[A-Z]{2,3}-(INQ|REG|UPD|DEL|EXE)-\\d{4}$");
    private static final Pattern TABLE_RE = Pattern.compile("^[A-Z][A-Z0-9_]{2,29}$");
    private static final Pattern COLUMN_RE = Pattern.compile("^[A-Z][A-Z0-9_]{1,29}$");
    private static final Pattern JAVA_FIELD_RE = Pattern.compile("^[a-z][A-Za-z0-9]*$");
    private static final Pattern JAVA_CLASS_RE = Pattern.compile("^[A-Z][A-Za-z0-9]*$");
    private static final Pattern JAVA_METHOD_RE = Pattern.compile("^[a-z][A-Za-z0-9]*$");
    private static final Pattern BASE_PACKAGE_RE = Pattern.compile("^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)+$");
    private static final Pattern ACTION_PARTS_RE = Pattern.compile("[A-Z]?[a-z]+|[A-Z]+(?=[A-Z]|$)|\\d+");

    private static final Set<String> SUPPORTED_OPERATIONS = Set.of(
            "SELECT_ONE", "SELECT_LIST", "INSERT", "UPDATE", "DELETE");
    private static final Set<String> SUPPORTED_JAVA_TYPES = Set.of(
            "String", "Integer", "Long", "BigDecimal", "LocalDate", "LocalDateTime", "Boolean");
    private static final Set<String> SUPPORTED_PACKAGE_PROFILES = Set.of("CURRENT_SOURCE", "DOMAIN_FIRST");

    public List<ValidationIssue> validateModel(BusinessModel model) {
        List<ValidationIssue> issues = new ArrayList<>();
        if (model == null) {
            issues.add(err("REQ-001", "model", "모델은 필수입니다."));
            return issues;
        }

        require(issues, model.getProjectName(), "projectName", "프로젝트명");
        require(issues, model.getBasePackage(), "basePackage", "BASE 패키지");
        require(issues, model.getBusinessCode(), "businessCode", "업무코드");
        require(issues, model.getModuleName(), "moduleName", "업무 모듈명");
        require(issues, model.getDomainCode(), "domainCode", "도메인 코드");
        require(issues, model.getAggregateName(), "aggregateName", "유스케이스/DTO 기준명");
        require(issues, model.getOperation(), "operation", "처리유형");
        require(issues, model.getMethodName(), "methodName", "메서드명");
        require(issues, model.getScreenId(), "screenId", "화면 ID");
        require(issues, model.getScreenName(), "screenName", "화면명");
        require(issues, model.getEventId(), "eventId", "이벤트 ID");
        require(issues, model.getEventName(), "eventName", "이벤트명");
        require(issues, model.getServiceId(), "serviceId", "ServiceId");
        require(issues, model.getTransactionCode(), "transactionCode", "거래코드");
        require(issues, model.getTableName(), "tableName", "테이블명");

        String business = trim(model.getBusinessCode());
        if (StringUtils.hasText(business) && !BUSINESS_CODE_RE.matcher(business).matches()) {
            issues.add(err("NAM-001", "businessCode", "업무코드는 영문 대문자 2~3자리여야 합니다."));
        }
        String basePackage = trim(model.getBasePackage());
        if (StringUtils.hasText(basePackage) && !BASE_PACKAGE_RE.matcher(basePackage).matches()) {
            issues.add(err("NAM-002", "basePackage", "BASE 패키지는 소문자 점(.) 구분 Java 패키지 형식이어야 합니다."));
        }
        String screenId = trim(model.getScreenId());
        if (StringUtils.hasText(screenId) && !SCREEN_ID_RE.matcher(screenId).matches()) {
            issues.add(err("NAM-003", "screenId", "화면 ID 형식은 {업무코드}-{세구분}-{4자리}여야 합니다."));
        } else if (StringUtils.hasText(screenId) && StringUtils.hasText(business) && !screenId.startsWith(business + "-")) {
            issues.add(err("TRC-001", "screenId", "화면 ID의 업무코드와 모델 업무코드가 일치하지 않습니다."));
        }
        String eventId = trim(model.getEventId());
        if (StringUtils.hasText(eventId) && !EVENT_ID_RE.matcher(eventId).matches()) {
            issues.add(err("NAM-004", "eventId", "이벤트 ID 형식은 {화면ID}-E{2자리}여야 합니다."));
        } else if (StringUtils.hasText(eventId) && StringUtils.hasText(screenId) && !eventId.startsWith(screenId + "-E")) {
            issues.add(err("TRC-002", "eventId", "이벤트 ID는 화면 ID 하위로 구성해야 합니다."));
        }
        String serviceId = trim(model.getServiceId());
        if (StringUtils.hasText(serviceId) && !SERVICE_ID_RE.matcher(serviceId).matches()) {
            issues.add(err("NAM-005", "serviceId", "ServiceId 형식은 {업무코드}.{도메인}.{행위}여야 합니다."));
        } else if (StringUtils.hasText(serviceId) && StringUtils.hasText(business) && !serviceId.startsWith(business + ".")) {
            issues.add(err("TRC-003", "serviceId", "ServiceId의 업무코드와 모델 업무코드가 일치하지 않습니다."));
        }
        String transactionCode = trim(model.getTransactionCode());
        if (StringUtils.hasText(transactionCode) && !TRANSACTION_CODE_RE.matcher(transactionCode).matches()) {
            issues.add(err("NAM-006", "transactionCode",
                    "거래코드는 {업무코드}-{INQ|REG|UPD|DEL|EXE}-{4자리} 형식이어야 합니다."));
        } else if (StringUtils.hasText(transactionCode) && StringUtils.hasText(business)
                && !transactionCode.startsWith(business + "-")) {
            issues.add(err("TRC-004", "transactionCode", "거래코드의 업무코드와 모델 업무코드가 일치하지 않습니다."));
        }
        String operation = trim(model.getOperation());
        if (StringUtils.hasText(operation) && !SUPPORTED_OPERATIONS.contains(operation)) {
            issues.add(err("MOD-001", "operation", "지원하지 않는 처리유형입니다: " + operation));
        }
        String methodName = trim(model.getMethodName());
        if (StringUtils.hasText(methodName) && !JAVA_METHOD_RE.matcher(methodName).matches()) {
            issues.add(err("NAM-007", "methodName", "메서드명은 lowerCamelCase Java 식별자여야 합니다."));
        }
        String aggregate = trim(model.getAggregateName());
        if (StringUtils.hasText(aggregate) && !JAVA_CLASS_RE.matcher(aggregate).matches()) {
            issues.add(err("NAM-008", "aggregateName", "유스케이스/DTO 기준명은 UpperCamelCase Java 식별자여야 합니다."));
        }
        String domainCode = trim(model.getDomainCode());
        if (StringUtils.hasText(domainCode) && !JAVA_CLASS_RE.matcher(domainCode).matches()) {
            issues.add(err("NAM-009", "domainCode", "도메인 코드는 UpperCamelCase Java 식별자여야 합니다."));
        }
        if (StringUtils.hasText(serviceId) && StringUtils.hasText(domainCode)) {
            String[] parts = serviceId.split("\\.");
            if (parts.length == 3 && !parts[1].equals(domainCode)) {
                issues.add(err("TRC-005", "domainCode", "ServiceId 도메인 구간과 도메인 코드가 일치하지 않습니다."));
            }
        }
        String tableName = trim(model.getTableName());
        if (StringUtils.hasText(tableName) && !TABLE_RE.matcher(tableName).matches()) {
            issues.add(err("NAM-010", "tableName", "테이블명은 영문 대문자·숫자·언더스코어 3~30자여야 합니다."));
        }
        Integer timeout = model.getTimeoutSeconds();
        if (timeout == null || timeout < 1 || timeout > 120) {
            issues.add(err("NFR-001", "timeoutSeconds", "Timeout은 1~120초 범위의 정수여야 합니다."));
        }
        String profile = StringUtils.hasText(model.getPackageProfile()) ? model.getPackageProfile() : "CURRENT_SOURCE";
        if (!SUPPORTED_PACKAGE_PROFILES.contains(profile)) {
            issues.add(err("MOD-002", "packageProfile", "지원하지 않는 패키지 프로파일입니다."));
        }

        List<FieldModel> fields = model.getFields();
        if (fields == null || fields.isEmpty()) {
            issues.add(err("DAT-001", "fields", "최소 1개 이상의 필드를 정의해야 합니다."));
            return issues;
        }

        Map<String, Integer> fieldNames = new HashMap<>();
        Map<String, Integer> columns = new HashMap<>();
        int pkCount = 0;
        int conditionCount = 0;
        int responseCount = 0;
        int requestCount = 0;

        for (int idx = 0; idx < fields.size(); idx++) {
            FieldModel field = fields.get(idx);
            String path = "fields[" + idx + "]";
            String name = trim(field.getName());
            String column = trim(field.getColumn());
            String javaType = trim(field.getJavaType());
            String dbType = trim(field.getDbType());

            if (!StringUtils.hasText(name)) {
                issues.add(err("DAT-002", path + ".name", "Java 필드명은 필수입니다."));
            } else if (!JAVA_FIELD_RE.matcher(name).matches()) {
                issues.add(err("NAM-011", path + ".name", "필드명은 lowerCamelCase Java 식별자여야 합니다."));
            }
            fieldNames.merge(name, 1, Integer::sum);

            if (!StringUtils.hasText(column)) {
                issues.add(err("DAT-003", path + ".column", "DB 컬럼명은 필수입니다."));
            } else if (!COLUMN_RE.matcher(column).matches()) {
                issues.add(err("NAM-012", path + ".column", "컬럼명은 영문 대문자·숫자·언더스코어 형식이어야 합니다."));
            }
            columns.merge(column, 1, Integer::sum);

            if (!SUPPORTED_JAVA_TYPES.contains(javaType)) {
                issues.add(err("DAT-004", path + ".javaType", "지원하지 않는 Java 타입입니다: " + javaType));
            }
            if (!StringUtils.hasText(dbType)) {
                issues.add(err("DAT-005", path + ".dbType", "DB 타입은 필수입니다."));
            }
            if (field.isPk()) {
                pkCount++;
            }
            if (field.isCondition()) {
                conditionCount++;
            }
            if (field.isResponse()) {
                responseCount++;
            }
            if (field.isRequest()) {
                requestCount++;
            }
            if (field.isSensitive() && !StringUtils.hasText(field.getMaskingRule())) {
                issues.add(warn("SEC-001", path + ".maskingRule", "민감정보 필드는 마스킹 규칙을 지정하는 것이 좋습니다."));
            }
            if (!field.isNullable() && !field.isRequest()
                    && ("INSERT".equals(operation) || "UPDATE".equals(operation))) {
                issues.add(warn("DAT-006", path, "NOT NULL 컬럼이 요청 필드에 포함되지 않았습니다."));
            }
        }

        fieldNames.forEach((name, count) -> {
            if (StringUtils.hasText(name) && count > 1) {
                issues.add(err("DAT-007", "fields", "Java 필드명이 중복되었습니다: " + name));
            }
        });
        columns.forEach((column, count) -> {
            if (StringUtils.hasText(column) && count > 1) {
                issues.add(err("DAT-008", "fields", "DB 컬럼명이 중복되었습니다: " + column));
            }
        });

        if (pkCount == 0) {
            issues.add(warn("DAT-009", "fields",
                    "PK 필드가 정의되지 않았습니다. 변경·삭제·단건조회 영향분석이 어려울 수 있습니다."));
        }
        if (Set.of("SELECT_ONE", "SELECT_LIST", "UPDATE", "DELETE").contains(operation) && conditionCount == 0) {
            issues.add(err("SQL-001", "fields", "조회·변경·삭제 거래에는 최소 1개의 조회조건 필드가 필요합니다."));
        }
        if (Set.of("SELECT_ONE", "SELECT_LIST").contains(operation) && responseCount == 0) {
            issues.add(err("DTO-001", "fields", "조회 거래에는 최소 1개의 응답 필드가 필요합니다."));
        }
        if (Set.of("INSERT", "UPDATE").contains(operation) && requestCount == 0) {
            issues.add(err("DTO-002", "fields", "등록·변경 거래에는 최소 1개의 요청 필드가 필요합니다."));
        }
        if (Set.of("INSERT", "UPDATE", "DELETE").contains(operation) && !model.isAuditRequired()) {
            issues.add(warn("SEC-002", "auditRequired", "데이터 변경 거래는 감사로그 대상으로 지정하는 것이 원칙입니다."));
        }

        String expectedCode = Map.of(
                "SELECT_ONE", "INQ",
                "SELECT_LIST", "INQ",
                "INSERT", "REG",
                "UPDATE", "UPD",
                "DELETE", "DEL").get(operation);
        if (expectedCode != null && StringUtils.hasText(transactionCode)
                && !transactionCode.contains("-" + expectedCode + "-")) {
            issues.add(err("TRC-006", "transactionCode", "처리유형과 거래코드 유형이 일치하지 않습니다."));
        }

        if (StringUtils.hasText(serviceId) && StringUtils.hasText(methodName)) {
            String action = serviceId.substring(serviceId.lastIndexOf('.') + 1);
            List<String> actionParts = splitCamel(action);
            String actionVerb = actionParts.isEmpty() ? action.toLowerCase() : actionParts.get(0).toLowerCase();
            String actionObject = actionParts.size() <= 1 ? ""
                    : String.join("", actionParts.subList(1, actionParts.size())).toLowerCase();
            String methodLower = methodName.toLowerCase();
            boolean compatible = action.equals(methodName)
                    || methodLower.endsWith(action.toLowerCase())
                    || (methodLower.startsWith(actionVerb)
                    && (actionObject.isEmpty() || methodLower.endsWith(actionObject)));
            if (!compatible) {
                issues.add(warn("TRC-007", "methodName", "ServiceId 행위명과 Facade/Service 메서드명이 다릅니다."));
            }
        }
        return issues;
    }

    public List<ValidationIssue> validateWorkspace(List<BusinessModel> models) {
        List<ValidationIssue> issues = new ArrayList<>();
        if (models == null) {
            return issues;
        }
        Map<String, Integer> serviceIds = new HashMap<>();
        Map<String, Integer> transactionCodes = new HashMap<>();
        Map<String, Integer> screenEvents = new HashMap<>();
        for (BusinessModel model : models) {
            serviceIds.merge(trim(model.getServiceId()), 1, Integer::sum);
            transactionCodes.merge(trim(model.getTransactionCode()), 1, Integer::sum);
            String pair = trim(model.getScreenId()) + "|" + trim(model.getEventId());
            screenEvents.merge(pair, 1, Integer::sum);
        }
        serviceIds.forEach((id, count) -> {
            if (StringUtils.hasText(id) && count > 1) {
                issues.add(err("WS-001", "serviceId", "Workspace 내 ServiceId가 중복되었습니다: " + id));
            }
        });
        transactionCodes.forEach((code, count) -> {
            if (StringUtils.hasText(code) && count > 1) {
                issues.add(err("WS-002", "transactionCode", "Workspace 내 거래코드가 중복되었습니다: " + code));
            }
        });
        screenEvents.forEach((pair, count) -> {
            String[] parts = pair.split("\\|", -1);
            if (parts.length == 2 && StringUtils.hasText(parts[0]) && StringUtils.hasText(parts[1]) && count > 1) {
                issues.add(warn("WS-003", "eventId",
                        "동일 화면 이벤트가 여러 모델에 중복 정의되었습니다: " + parts[0] + " / " + parts[1]));
            }
        });

        Map<String, List<BusinessModel>> groups = new HashMap<>();
        for (BusinessModel model : models) {
            String key = Objects.toString(model.getBusinessCode(), "") + "|"
                    + Objects.toString(model.getDomainCode(), "") + "|"
                    + (StringUtils.hasText(model.getPackageProfile())
                    ? model.getPackageProfile() : "CURRENT_SOURCE");
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(model);
        }
        for (Map.Entry<String, List<BusinessModel>> entry : groups.entrySet()) {
            Map<String, Integer> aggregates = new HashMap<>();
            Map<String, Integer> methods = new HashMap<>();
            for (BusinessModel model : entry.getValue()) {
                aggregates.merge(trim(model.getAggregateName()), 1, Integer::sum);
                methods.merge(trim(model.getMethodName()), 1, Integer::sum);
            }
            String[] keyParts = entry.getKey().split("\\|", -1);
            String prefix = keyParts[0] + "." + keyParts[1];
            aggregates.forEach((value, count) -> {
                if (StringUtils.hasText(value) && count > 1) {
                    issues.add(err("WS-004", "aggregateName",
                            "동일 도메인 내 DTO 기준명이 중복되었습니다: " + prefix + " / " + value));
                }
            });
            methods.forEach((value, count) -> {
                if (StringUtils.hasText(value) && count > 1) {
                    issues.add(err("WS-005", "methodName",
                            "동일 도메인 내 메서드명이 중복되었습니다: " + prefix + " / " + value));
                }
            });
        }
        return issues;
    }

    public boolean hasErrors(List<ValidationIssue> issues) {
        if (issues == null) {
            return false;
        }
        return issues.stream().anyMatch(i -> "ERROR".equals(i.getLevel()));
    }

    private static void require(List<ValidationIssue> issues, String value, String path, String label) {
        if (!StringUtils.hasText(value)) {
            issues.add(err("REQ-001", path, label + "은(는) 필수입니다."));
        }
    }

    private static ValidationIssue err(String code, String path, String message) {
        return ValidationIssue.of("ERROR", code, path, message);
    }

    private static ValidationIssue warn(String code, String path, String message) {
        return ValidationIssue.of("WARNING", code, path, message);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static List<String> splitCamel(String value) {
        List<String> parts = new ArrayList<>();
        Matcher matcher = ACTION_PARTS_RE.matcher(value);
        while (matcher.find()) {
            parts.add(matcher.group());
        }
        return parts;
    }
}
