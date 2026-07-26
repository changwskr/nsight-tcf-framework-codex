from __future__ import annotations

import csv
import io
import json
import re
import tempfile
import zipfile
from collections import defaultdict
from pathlib import Path
from typing import Any, Iterable

from validators import has_errors, validate_model, validate_workspace


JAVA_IMPORTS = {
    "BigDecimal": "java.math.BigDecimal",
    "LocalDate": "java.time.LocalDate",
    "LocalDateTime": "java.time.LocalDateTime",
}


def lower_first(value: str) -> str:
    return value[:1].lower() + value[1:] if value else value


def upper_first(value: str) -> str:
    return value[:1].upper() + value[1:] if value else value


def biz_class(code: str) -> str:
    return code[:1].upper() + code[1:].lower()


def java_path(package_name: str, class_name: str) -> str:
    return "src/main/java/" + package_name.replace(".", "/") + f"/{class_name}.java"


def test_java_path(package_name: str, class_name: str) -> str:
    return "src/test/java/" + package_name.replace(".", "/") + f"/{class_name}.java"


def indent(lines: Iterable[str], spaces: int = 4) -> str:
    prefix = " " * spaces
    return "\n".join(prefix + line if line else "" for line in lines)


def package_layout(model: dict[str, Any]) -> dict[str, str]:
    base = str(model.get("basePackage", "com.nh.nsight.marketing")).strip()
    biz = str(model.get("businessCode", "SV")).lower()
    domain = str(model.get("domainCode", "Customer"))
    domain_lower = lower_first(domain)
    profile = str(model.get("packageProfile", "CURRENT_SOURCE"))

    if profile == "DOMAIN_FIRST":
        root = f"{base}.{biz}.{domain_lower}"
        return {
            "handler": f"{root}.handler",
            "facade": f"{root}.facade",
            "service": f"{root}.service",
            "rule": f"{root}.rule",
            "app_dto": f"{root}.dto",
            "dao": f"{root}.dao",
            "row_dto": f"{root}.dto",
            "mapper": f"{root}.mapper",
        }
    root = f"{base}.{biz}"
    return {
        "handler": f"{root}.entry.handler",
        "facade": f"{root}.entry.facade",
        "service": f"{root}.application.service",
        "rule": f"{root}.application.rule",
        "app_dto": f"{root}.application.dto.{domain_lower}",
        "dao": f"{root}.persistence.dao",
        "row_dto": f"{root}.persistence.dto.{domain_lower}",
        "mapper": f"{root}.persistence.mapper",
    }


def class_names(model: dict[str, Any]) -> dict[str, str]:
    prefix = biz_class(str(model.get("businessCode", "SV")))
    domain = str(model.get("domainCode", "Customer"))
    aggregate = str(model.get("aggregateName", domain + "UseCase"))
    return {
        "handler": f"{prefix}{domain}Handler",
        "facade": f"{prefix}{domain}Facade",
        "service": f"{prefix}{domain}Service",
        "rule": f"{prefix}{domain}Rule",
        "dao": f"{prefix}{domain}Dao",
        "mapper": f"{prefix}{domain}Mapper",
        "request": f"{aggregate}Request",
        "criteria": f"{aggregate}Criteria",
        "response": f"{aggregate}Response",
        "row": f"{aggregate}Row",
    }


def _field_imports(fields: list[dict[str, Any]]) -> list[str]:
    imports = sorted({JAVA_IMPORTS[t] for f in fields if (t := str(f.get("javaType", "String"))) in JAVA_IMPORTS})
    return imports


def _getter(field_name: str) -> str:
    return "get" + upper_first(field_name)


def _setter(field_name: str) -> str:
    return "set" + upper_first(field_name)


def _java_default_conversion(java_type: str, source_expr: str) -> str:
    if java_type == "String":
        return f"stringValue({source_expr})"
    if java_type == "Integer":
        return f"integerValue({source_expr})"
    if java_type == "Long":
        return f"longValue({source_expr})"
    if java_type == "BigDecimal":
        return f"decimalValue({source_expr})"
    if java_type == "Boolean":
        return f"booleanValue({source_expr})"
    if java_type == "LocalDate":
        return f"localDateValue({source_expr})"
    if java_type == "LocalDateTime":
        return f"localDateTimeValue({source_expr})"
    return f"({java_type}) {source_expr}"


def _request_fields(model: dict[str, Any]) -> list[dict[str, Any]]:
    return [f for f in model.get("fields", []) if bool(f.get("request")) or bool(f.get("condition"))]


def _condition_fields(model: dict[str, Any]) -> list[dict[str, Any]]:
    return [f for f in model.get("fields", []) if bool(f.get("condition"))]


def _response_fields(model: dict[str, Any]) -> list[dict[str, Any]]:
    return [f for f in model.get("fields", []) if bool(f.get("response"))]


def _write_fields(model: dict[str, Any]) -> list[dict[str, Any]]:
    operation = model.get("operation")
    fields = [f for f in model.get("fields", []) if bool(f.get("request"))]
    if operation == "UPDATE":
        condition_names = {f.get("name") for f in _condition_fields(model)}
        return [f for f in fields if f.get("name") not in condition_names and not bool(f.get("pk"))]
    return fields


def generate_request_dto(model: dict[str, Any]) -> tuple[str, str]:
    layout = package_layout(model)
    names = class_names(model)
    fields = _request_fields(model)
    imports = ["java.util.Map"] + _field_imports(fields)
    package = layout["app_dto"]
    class_name = names["request"]

    field_decls = "\n".join(f"    private final {f['javaType']} {f['name']};" for f in fields)
    ctor_params = ", ".join(f"{f['javaType']} {f['name']}" for f in fields)
    ctor_assign = "\n".join(f"        this.{f['name']} = {f['name']};" for f in fields)
    from_map_values = []
    for field in fields:
        source_expr = f'body.get("{field["name"]}")'
        from_map_values.append(
            "                " + _java_default_conversion(field["javaType"], source_expr)
        )
    from_map_args = ",\n".join(from_map_values)
    getters = "\n\n".join(
        f"    public {f['javaType']} {_getter(f['name'])}() {{\n        return {f['name']};\n    }}" for f in fields
    )

    conversions = """
    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private static Integer integerValue(Object value) {
        String text = stringValue(value);
        return text == null || text.isBlank() ? null : Integer.valueOf(text);
    }

    private static Long longValue(Object value) {
        String text = stringValue(value);
        return text == null || text.isBlank() ? null : Long.valueOf(text);
    }

    private static BigDecimal decimalValue(Object value) {
        String text = stringValue(value);
        return text == null || text.isBlank() ? null : new BigDecimal(text);
    }

    private static Boolean booleanValue(Object value) {
        String text = stringValue(value);
        return text == null || text.isBlank() ? null : Boolean.valueOf(text);
    }

    private static LocalDate localDateValue(Object value) {
        String text = stringValue(value);
        return text == null || text.isBlank() ? null : LocalDate.parse(text);
    }

    private static LocalDateTime localDateTimeValue(Object value) {
        String text = stringValue(value);
        return text == null || text.isBlank() ? null : LocalDateTime.parse(text);
    }
"""
    used_types = {str(f.get("javaType")) for f in fields}
    conversion_methods = []
    for block_name, type_name in [
        ("stringValue", "String"),
        ("integerValue", "Integer"),
        ("longValue", "Long"),
        ("decimalValue", "BigDecimal"),
        ("booleanValue", "Boolean"),
        ("localDateValue", "LocalDate"),
        ("localDateTimeValue", "LocalDateTime"),
    ]:
        if type_name in used_types or block_name == "stringValue":
            pattern = re.compile(rf"\n    private static .*? {block_name}\(Object value\) \{{.*?\n    \}}\n", re.S)
            match = pattern.search(conversions)
            if match:
                conversion_methods.append(match.group(0).strip("\n"))

    imports_text = "\n".join(f"import {item};" for item in sorted(set(imports)))
    args = from_map_args if from_map_args else ""
    null_args = ", ".join("null" for _ in fields)
    code = f"""package {package};

{imports_text}

/**
 * {model.get('serviceId')} 요청 body.
 * 자동생성 파일: 업무 규칙은 Rule에 구현한다.
 */
public class {class_name} {{

{field_decls}

    public {class_name}({ctor_params}) {{
{ctor_assign}
    }}

    public static {class_name} fromMap(Map<String, Object> body) {{
        if (body == null) {{
            return new {class_name}({null_args});
        }}
        return new {class_name}(
{args});
    }}

{getters}

{indent(conversion_methods, 0)}
}}
"""
    return java_path(package, class_name), code


def generate_criteria_dto(model: dict[str, Any]) -> tuple[str, str]:
    layout = package_layout(model)
    names = class_names(model)
    fields = _condition_fields(model)
    package = layout["app_dto"]
    class_name = names["criteria"]
    imports_text = "\n".join(f"import {item};" for item in _field_imports(fields))
    field_decls = "\n".join(f"    private final {f['javaType']} {f['name']};" for f in fields)
    ctor_params = ", ".join(f"{f['javaType']} {f['name']}" for f in fields)
    ctor_assign = "\n".join(f"        this.{f['name']} = {f['name']};" for f in fields)
    getters = "\n\n".join(
        f"    public {f['javaType']} {_getter(f['name'])}() {{\n        return {f['name']};\n    }}" for f in fields
    )
    code = f"""package {package};

{imports_text}

/**
 * {model.get('serviceId')} DAO/MyBatis 조건.
 */
public class {class_name} {{

{field_decls}

    public {class_name}({ctor_params}) {{
{ctor_assign}
    }}

{getters}
}}
"""
    return java_path(package, class_name), code


def generate_row_dto(model: dict[str, Any]) -> tuple[str, str]:
    layout = package_layout(model)
    names = class_names(model)
    fields = _response_fields(model) or model.get("fields", [])
    package = layout["row_dto"]
    class_name = names["row"]
    imports = ["java.util.LinkedHashMap", "java.util.Map"] + _field_imports(fields)
    imports_text = "\n".join(f"import {item};" for item in sorted(set(imports)))
    field_decls = "\n".join(f"    private {f['javaType']} {f['name']};" for f in fields)
    empty_field = next((f for f in fields if bool(f.get("pk"))), fields[0] if fields else None)
    if empty_field:
        empty_expr = f"{empty_field['name']} == null" + (f" || {empty_field['name']}.isBlank()" if empty_field["javaType"] == "String" else "")
    else:
        empty_expr = "true"
    to_map = "\n".join(f"        map.put(\"{f['name']}\", {f['name']});" for f in fields)
    accessors = []
    for f in fields:
        name = f["name"]
        java_type = f["javaType"]
        accessors.append(
            f"    public {java_type} {_getter(name)}() {{\n        return {name};\n    }}\n\n"
            f"    public void {_setter(name)}({java_type} {name}) {{\n        this.{name} = {name};\n    }}"
        )
    accessors_text = "\n\n".join(accessors)
    code = f"""package {package};

{imports_text}

/**
 * {model.get('tableName')} 조회 Row (MyBatis result).
 */
public class {class_name} {{

{field_decls}

    public boolean isEmpty() {{
        return {empty_expr};
    }}

    public Map<String, Object> toMap() {{
        Map<String, Object> map = new LinkedHashMap<>();
{to_map}
        return map;
    }}

{accessors_text}
}}
"""
    return java_path(package, class_name), code


def generate_response_dto(model: dict[str, Any]) -> tuple[str, str]:
    layout = package_layout(model)
    names = class_names(model)
    package = layout["app_dto"]
    class_name = names["response"]
    row_import = f"{layout['row_dto']}.{names['row']}"
    operation = model.get("operation")
    imports = [
        "com.nh.nsight.tcf.core.support.context.TransactionContext",
        "java.util.LinkedHashMap",
        "java.util.Map",
    ]
    if operation in {"SELECT_ONE", "SELECT_LIST"}:
        imports.append(row_import)
    if operation == "SELECT_LIST":
        imports.extend(["java.util.List", "java.util.stream.Collectors"])
    imports_text = "\n".join(f"import {item};" for item in sorted(set(imports)))

    business = model.get("businessCode")
    if operation == "SELECT_ONE":
        payload_decl = f"    private final {names['row']} result;"
        ctor_payload = f", {names['row']} result"
        ctor_assign = "        this.result = result;"
        factory_payload = f", {names['row']} result"
        factory_call = ", result"
        map_payload = """        if (result != null) {
            body.putAll(result.toMap());
        }"""
    elif operation == "SELECT_LIST":
        payload_decl = f"    private final List<{names['row']}> results;"
        ctor_payload = f", List<{names['row']}> results"
        ctor_assign = "        this.results = results == null ? List.of() : List.copyOf(results);"
        factory_payload = f", List<{names['row']}> results"
        factory_call = ", results"
        map_payload = """        body.put("items", results.stream().map(result -> result.toMap()).collect(Collectors.toList()));
        body.put("count", results.size());"""
    else:
        payload_decl = "    private final int affectedCount;"
        ctor_payload = ", int affectedCount"
        ctor_assign = "        this.affectedCount = affectedCount;"
        factory_payload = ", int affectedCount"
        factory_call = ", affectedCount"
        map_payload = "        body.put(\"affectedCount\", affectedCount);"

    code = f"""package {package};

{imports_text}

/**
 * {model.get('serviceId')} 응답 body.
 */
public class {class_name} {{

    private final String businessCode;
    private final String serviceId;
    private final String guid;
{payload_decl}

    public {class_name}(String businessCode, String serviceId, String guid{ctor_payload}) {{
        this.businessCode = businessCode;
        this.serviceId = serviceId;
        this.guid = guid;
{ctor_assign}
    }}

    public static {class_name} of(TransactionContext context{factory_payload}) {{
        return new {class_name}("{business}", context.getHeader().getServiceId(), context.getHeader().getGuid(){factory_call});
    }}

    public Map<String, Object> toMap() {{
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("businessCode", businessCode);
        body.put("serviceId", serviceId);
        body.put("guid", guid);
{map_payload}
        return body;
    }}
}}
"""
    return java_path(package, class_name), code


def _required_validation_lines(model: dict[str, Any], request_var: str = "request") -> list[str]:
    lines: list[str] = []
    for f in _request_fields(model):
        required = not bool(f.get("nullable", True)) or "required" in str(f.get("validation", "")).lower()
        getter = f"{request_var}.{_getter(f['name'])}()"
        if not required:
            continue
        if f["javaType"] == "String":
            lines.append(f"        if (!StringUtils.hasText({getter})) {{")
        else:
            lines.append(f"        if ({getter} == null) {{")
        lines.append(f"            throw new BusinessException(\"E-{model['businessCode']}-VAL-0001\", \"{f.get('label') or f['name']}은(는) 필수입니다.\");")
        lines.append("        }")
        length = f.get("length")
        if f["javaType"] == "String" and length:
            lines.append(f"        if (StringUtils.hasText({getter}) && {getter}.length() > {int(length)}) {{")
            lines.append(f"            throw new BusinessException(\"E-{model['businessCode']}-VAL-0002\", \"{f.get('label') or f['name']} 길이가 {int(length)}자를 초과했습니다.\");")
            lines.append("        }")
    return lines


def _criteria_ctor_args(model: dict[str, Any]) -> str:
    return ", ".join(f"request.{_getter(f['name'])}()" for f in _condition_fields(model))


def generate_domain_classes(group: list[dict[str, Any]]) -> dict[str, str]:
    first = group[0]
    layout = package_layout(first)
    names = class_names(first)
    business = first["businessCode"]
    domain = first["domainCode"]
    artifacts: dict[str, str] = {}

    # Handler
    constants = []
    service_ids = []
    switch_cases = []
    for model in group:
        const_name = re.sub(r"(?<!^)(?=[A-Z])", "_", model["methodName"]).upper()
        constants.append(f"    private static final String {const_name} = \"{model['serviceId']}\";")
        service_ids.append(const_name)
        switch_cases.append(f"            case {const_name} -> facade.{model['methodName']}(request.getBody(), context);")
    constants_text = "\n".join(constants)
    service_ids_text = ", ".join(service_ids)
    switch_cases_text = "\n".join(switch_cases)
    handler_code = f"""package {layout['handler']};

import {layout['facade']}.{names['facade']};
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
 * {business} {domain} 도메인 Handler.
 * 동일 도메인의 ServiceId를 serviceIds()에 일괄 등록한다.
 */
@Component
public class {names['handler']} implements TransactionHandler {{

{constants_text}

    private final {names['facade']} facade;

    public {names['handler']}({names['facade']} facade) {{
        this.facade = facade;
    }}

    @Override
    public Collection<String> serviceIds() {{
        return List.of({service_ids_text});
    }}

    @Override
    public Object doHandle(StandardRequest<Map<String, Object>> request, TransactionContext context) {{
        String serviceId = context.getHeader().getServiceId();
        return switch (serviceId) {{
{switch_cases_text}
            default -> throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND,
                    "{names['handler']} 미지원 serviceId: " + serviceId);
        }};
    }}
}}
"""
    artifacts[java_path(layout["handler"], names["handler"])] = handler_code

    # Facade
    facade_imports = {
        f"{layout['service']}.{names['service']}",
        "com.nh.nsight.tcf.core.support.context.TransactionContext",
        "java.util.Map",
        "org.springframework.stereotype.Service",
        "org.springframework.transaction.annotation.Transactional",
    }
    facade_methods = []
    for model in group:
        local_names = class_names(model)
        facade_imports.add(f"{layout['app_dto']}.{local_names['request']}")
        read_only = "true" if model["operation"].startswith("SELECT") else "false"
        facade_methods.append(f"""    @Transactional(readOnly = {read_only}, timeout = {int(model.get('timeoutSeconds', 3))})
    public Map<String, Object> {model['methodName']}(Map<String, Object> body, TransactionContext context) {{
        {local_names['request']} request = {local_names['request']}.fromMap(body);
        return service.{model['methodName']}(request, context).toMap();
    }}""")
    facade_imports_text = "\n".join(f"import {item};" for item in sorted(facade_imports))
    facade_methods_text = "\n\n".join(facade_methods)
    facade_code = f"""package {layout['facade']};

{facade_imports_text}

@Service
public class {names['facade']} {{

    private final {names['service']} service;

    public {names['facade']}({names['service']} service) {{
        this.service = service;
    }}

{facade_methods_text}
}}
"""
    artifacts[java_path(layout["facade"], names["facade"])] = facade_code

    # Service
    service_imports = {
        f"{layout['rule']}.{names['rule']}",
        f"{layout['dao']}.{names['dao']}",
        "com.nh.nsight.tcf.core.support.context.TransactionContext",
        "org.springframework.stereotype.Service",
    }
    service_methods = []
    for model in group:
        n = class_names(model)
        service_imports.update({f"{layout['app_dto']}.{n['request']}", f"{layout['app_dto']}.{n['response']}"})
        if model["operation"] in {"SELECT_ONE", "SELECT_LIST"}:
            service_imports.add(f"{layout['row_dto']}.{n['row']}")
        if model["operation"] == "SELECT_LIST":
            service_imports.add("java.util.List")
        if model["operation"] == "SELECT_ONE":
            body = f"""        var criteria = rule.build{n['aggregate'] if 'aggregate' in n else model['aggregateName']}Criteria(request);
        {n['row']} result = dao.{model['methodName']}(criteria);
        rule.validate{model['aggregateName']}Result(result);
        return {n['response']}.of(context, result);"""
        elif model["operation"] == "SELECT_LIST":
            body = f"""        var criteria = rule.build{model['aggregateName']}Criteria(request);
        List<{n['row']}> results = dao.{model['methodName']}(criteria);
        return {n['response']}.of(context, results);"""
        else:
            body = f"""        rule.validate{model['aggregateName']}Request(request);
        int affectedCount = dao.{model['methodName']}(request);
        rule.validate{model['aggregateName']}AffectedCount(affectedCount);
        return {n['response']}.of(context, affectedCount);"""
        service_methods.append(f"""    public {n['response']} {model['methodName']}({n['request']} request, TransactionContext context) {{
{body}
    }}""")
    service_imports_text = "\n".join(f"import {item};" for item in sorted(service_imports))
    service_methods_text = "\n\n".join(service_methods)
    service_code = f"""package {layout['service']};

{service_imports_text}

@Service
public class {names['service']} {{
    private final {names['rule']} rule;
    private final {names['dao']} dao;

    public {names['service']}({names['rule']} rule, {names['dao']} dao) {{
        this.rule = rule;
        this.dao = dao;
    }}

{service_methods_text}
}}
"""
    artifacts[java_path(layout["service"], names["service"])] = service_code

    # Rule
    rule_imports = {
        "com.nh.nsight.tcf.core.support.error.BusinessException",
        "org.springframework.stereotype.Component",
        "org.springframework.util.StringUtils",
    }
    rule_methods = []
    for model in group:
        n = class_names(model)
        rule_imports.add(f"{layout['app_dto']}.{n['request']}")
        if model["operation"] in {"SELECT_ONE", "SELECT_LIST"}:
            rule_imports.add(f"{layout['app_dto']}.{n['criteria']}")
            rule_imports.add(f"{layout['row_dto']}.{n['row']}")
            validation = _required_validation_lines(model)
            criteria_args = _criteria_ctor_args(model)
            result_method = ""
            if model["operation"] == "SELECT_ONE":
                result_method = f"""

    public void validate{model['aggregateName']}Result({n['row']} result) {{
        if (result == null || result.isEmpty()) {{
            throw new BusinessException("E-{business}-BIZ-0001", "조회된 {model.get('screenName', model['aggregateName'])} 정보가 없습니다.");
        }}
    }}"""
            validation_text = "\n".join(validation)
            rule_methods.append(f"""    public {n['criteria']} build{model['aggregateName']}Criteria({n['request']} request) {{
        if (request == null) {{
            throw new BusinessException("E-{business}-VAL-0000", "요청 정보가 없습니다.");
        }}
{validation_text}
        return new {n['criteria']}({criteria_args});
    }}{result_method}""")
        else:
            validation = _required_validation_lines(model)
            validation_text = "\n".join(validation)
            rule_methods.append(f"""    public void validate{model['aggregateName']}Request({n['request']} request) {{
        if (request == null) {{
            throw new BusinessException("E-{business}-VAL-0000", "요청 정보가 없습니다.");
        }}
{validation_text}
    }}

    public void validate{model['aggregateName']}AffectedCount(int affectedCount) {{
        if (affectedCount != 1) {{
            throw new BusinessException("E-{business}-BIZ-0002", "처리 건수가 올바르지 않습니다: " + affectedCount);
        }}
    }}""")
    rule_imports_text = "\n".join(f"import {item};" for item in sorted(rule_imports))
    rule_methods_text = "\n\n".join(rule_methods)
    rule_code = f"""package {layout['rule']};

{rule_imports_text}

@Component
public class {names['rule']} {{

{rule_methods_text}
}}
"""
    artifacts[java_path(layout["rule"], names["rule"])] = rule_code

    # DAO
    dao_imports = {f"{layout['mapper']}.{names['mapper']}", "org.springframework.stereotype.Repository"}
    dao_methods = []
    for model in group:
        n = class_names(model)
        if model["operation"] in {"SELECT_ONE", "SELECT_LIST"}:
            dao_imports.add(f"{layout['app_dto']}.{n['criteria']}")
            dao_imports.add(f"{layout['row_dto']}.{n['row']}")
            if model["operation"] == "SELECT_LIST":
                dao_imports.add("java.util.List")
                return_type = f"List<{n['row']}>"
            else:
                return_type = n["row"]
            parameter_type = n["criteria"]
            parameter_name = "criteria"
        else:
            dao_imports.add(f"{layout['app_dto']}.{n['request']}")
            return_type = "int"
            parameter_type = n["request"]
            parameter_name = "request"
        dao_methods.append(f"""    public {return_type} {model['methodName']}({parameter_type} {parameter_name}) {{
        return mapper.{model['methodName']}({parameter_name});
    }}""")
    dao_imports_text = "\n".join(f"import {item};" for item in sorted(dao_imports))
    dao_methods_text = "\n\n".join(dao_methods)
    dao_code = f"""package {layout['dao']};

{dao_imports_text}

@Repository
public class {names['dao']} {{
    private final {names['mapper']} mapper;

    public {names['dao']}({names['mapper']} mapper) {{
        this.mapper = mapper;
    }}

{dao_methods_text}
}}
"""
    artifacts[java_path(layout["dao"], names["dao"])] = dao_code

    # Mapper interface
    mapper_imports = {"org.apache.ibatis.annotations.Mapper"}
    mapper_methods = []
    for model in group:
        n = class_names(model)
        if model["operation"] in {"SELECT_ONE", "SELECT_LIST"}:
            mapper_imports.add(f"{layout['app_dto']}.{n['criteria']}")
            mapper_imports.add(f"{layout['row_dto']}.{n['row']}")
            if model["operation"] == "SELECT_LIST":
                mapper_imports.add("java.util.List")
                return_type = f"List<{n['row']}>"
            else:
                return_type = n["row"]
            parameter_type = n["criteria"]
            parameter_name = "criteria"
        else:
            mapper_imports.add(f"{layout['app_dto']}.{n['request']}")
            return_type = "int"
            parameter_type = n["request"]
            parameter_name = "request"
        mapper_methods.append(f"    {return_type} {model['methodName']}({parameter_type} {parameter_name});")
    mapper_imports_text = "\n".join(f"import {item};" for item in sorted(mapper_imports))
    mapper_methods_text = "\n\n".join(mapper_methods)
    mapper_code = f"""package {layout['mapper']};

{mapper_imports_text}

@Mapper
public interface {names['mapper']} {{
{mapper_methods_text}
}}
"""
    artifacts[java_path(layout["mapper"], names["mapper"])] = mapper_code

    # Mapper XML
    statements = "\n\n".join(generate_sql_statement(model, layout, names) for model in group)
    mapper_xml = f"""<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="{layout['mapper']}.{names['mapper']}">

{statements}

</mapper>
"""
    mapper_path = f"src/main/resources/mapper/{business.lower()}/{names['mapper']}.xml"
    artifacts[mapper_path] = mapper_xml

    return artifacts


def generate_sql_statement(model: dict[str, Any], layout: dict[str, str], domain_names: dict[str, str]) -> str:
    names = class_names(model)
    operation = model["operation"]
    table = model["tableName"]
    timeout = int(model.get("timeoutSeconds", 3))
    conditions = _condition_fields(model)
    response_fields = _response_fields(model)
    request_fields = _request_fields(model)
    write_fields = _write_fields(model)

    if conditions:
        where_lines = [f"             {('WHERE' if i == 0 else 'AND  ')} {f['column']} = #{{{f['name']}}}" for i, f in enumerate(conditions)]
    else:
        where_lines = ["             /* TODO: 안전한 WHERE 조건을 정의하십시오. */"]
    where = "\n".join(where_lines)

    if operation in {"SELECT_ONE", "SELECT_LIST"}:
        columns = response_fields or model.get("fields", [])
        select_columns = "\n".join(
            f"              {f['column']:<30} AS {f['name']}" if i == 0 else f"            , {f['column']:<30} AS {f['name']}"
            for i, f in enumerate(columns)
        )
        return f"""    <!-- SQL_ID: {model['serviceId']} / 화면: {model['screenId']} / 이벤트: {model['eventId']} -->
    <select id="{model['methodName']}"
            parameterType="{layout['app_dto']}.{names['criteria']}"
            resultType="{layout['row_dto']}.{names['row']}"
            timeout="{timeout}">
        /* SQL_ID: {model['serviceId']} */
        SELECT
{select_columns}
          FROM {table}
{where}
    </select>"""
    if operation == "INSERT":
        columns = ", ".join(f["column"] for f in request_fields)
        values = ", ".join(f"#{{{f['name']}}}" for f in request_fields)
        return f"""    <!-- SQL_ID: {model['serviceId']} / 화면: {model['screenId']} / 이벤트: {model['eventId']} -->
    <insert id="{model['methodName']}"
            parameterType="{layout['app_dto']}.{names['request']}"
            timeout="{timeout}">
        /* SQL_ID: {model['serviceId']} */
        INSERT INTO {table} ({columns})
        VALUES ({values})
    </insert>"""
    if operation == "UPDATE":
        set_lines = "\n".join(
            f"              {f['column']} = #{{{f['name']}}}" if i == 0 else f"            , {f['column']} = #{{{f['name']}}}"
            for i, f in enumerate(write_fields)
        )
        if not set_lines:
            set_lines = "              /* TODO: 변경 컬럼 정의 */"
        return f"""    <!-- SQL_ID: {model['serviceId']} / 화면: {model['screenId']} / 이벤트: {model['eventId']} -->
    <update id="{model['methodName']}"
            parameterType="{layout['app_dto']}.{names['request']}"
            timeout="{timeout}">
        /* SQL_ID: {model['serviceId']} */
        UPDATE {table}
           SET
{set_lines}
{where}
    </update>"""
    return f"""    <!-- SQL_ID: {model['serviceId']} / 화면: {model['screenId']} / 이벤트: {model['eventId']} -->
    <delete id="{model['methodName']}"
            parameterType="{layout['app_dto']}.{names['request']}"
            timeout="{timeout}">
        /* SQL_ID: {model['serviceId']} */
        DELETE FROM {table}
{where}
    </delete>"""


def generate_rule_test(model: dict[str, Any]) -> tuple[str, str]:
    layout = package_layout(model)
    names = class_names(model)
    package = layout["rule"]
    class_name = f"{names['rule']}Test"
    code = f"""package {package};

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 자동생성 테스트 골격. 실제 필드별 정상/오류 시나리오를 보완한다.
 */
class {class_name} {{

    private final {names['rule']} rule = new {names['rule']}();

    @Test
    @DisplayName("Rule 인스턴스 생성")
    void createRule() {{
        assertNotNull(rule);
    }}
}}
"""
    return test_java_path(package, class_name), code


def generate_ddl(model: dict[str, Any]) -> tuple[str, str]:
    table = model["tableName"]
    fields = model.get("fields", [])
    column_lines = []
    pk_columns = []
    for f in fields:
        nullable = "" if bool(f.get("nullable", True)) else " NOT NULL"
        comment = f.get("label") or f.get("name")
        column_lines.append(f"    {f['column']} {f['dbType']}{nullable} /* {comment} */")
        if bool(f.get("pk")):
            pk_columns.append(f["column"])
    if pk_columns:
        column_lines.append(f"    CONSTRAINT PK_{table} PRIMARY KEY ({', '.join(pk_columns)})")
    column_lines_text = ",\n".join(column_lines)
    ddl = f"""-- 자동생성 DDL 초안
-- 실제 스키마·테이블스페이스·파티션·인덱스 기준은 DA/DBA 검토 후 확정한다.
CREATE TABLE {table} (
{column_lines_text}
);

COMMENT ON TABLE {table} IS '{model.get('tableComment') or model.get('screenName') or table}';
"""
    for f in fields:
        ddl += f"COMMENT ON COLUMN {table}.{f['column']} IS '{f.get('label') or f['name']}';\n"
    return f"db/ddl/{table}.sql", ddl


def generate_om_catalog(model: dict[str, Any]) -> tuple[str, str]:
    audit = "Y" if model.get("auditRequired") else "N"
    enabled = "Y"
    sql = f"""-- OM Service Catalog 등록 초안
INSERT INTO OM_SERVICE_CATALOG (
    SERVICE_ID, TRANSACTION_CODE, BUSINESS_CODE, SERVICE_NAME,
    TIMEOUT_SECONDS, AUDIT_YN, USE_YN, SCREEN_ID, EVENT_ID
) VALUES (
    '{model['serviceId']}', '{model['transactionCode']}', '{model['businessCode']}',
    '{model.get('serviceName') or model.get('eventName')}', {int(model.get('timeoutSeconds', 3))},
    '{audit}', '{enabled}', '{model['screenId']}', '{model['eventId']}'
);
"""
    safe_id = model["serviceId"].replace(".", "_")
    return f"db/om/{safe_id}_OM_SERVICE_CATALOG.sql", sql


def generate_http_request(model: dict[str, Any]) -> tuple[str, str]:
    body = {}
    for f in _request_fields(model):
        sample = f.get("sampleValue")
        if sample in (None, ""):
            sample = {
                "String": "SAMPLE",
                "Integer": 1,
                "Long": 1,
                "BigDecimal": 1000.0,
                "Boolean": True,
                "LocalDate": "2026-07-25",
                "LocalDateTime": "2026-07-25T09:00:00",
            }.get(f.get("javaType"), "SAMPLE")
        body[f["name"]] = sample
    request = {
        "header": {
            "businessCode": model["businessCode"],
            "serviceId": model["serviceId"],
            "transactionCode": model["transactionCode"],
            "screenId": model["screenId"],
            "channelId": "WEBTOP",
            "userId": "TEST_USER",
            "branchId": "000001",
        },
        "body": body,
    }
    text = f"""### {model.get('serviceName') or model['serviceId']}
POST http://localhost:8080{model.get('contextPath') or '/'}/online
Content-Type: application/json
Authorization: Bearer {{accessToken}}

{json.dumps(request, ensure_ascii=False, indent=2)}
"""
    return f"requests/{model['serviceId'].replace('.', '_')}.http", text


def generate_screen_definition(model: dict[str, Any]) -> tuple[str, str]:
    fields = model.get("fields", [])
    rows = [
        "| Java 필드 | 화면/업무명 | DB 컬럼 | Java 타입 | DB 타입 | 요청 | 조건 | 응답 | 필수 | 민감 |",
        "|---|---|---|---|---|---:|---:|---:|---:|---:|",
    ]
    for f in fields:
        rows.append(
            f"| `{f['name']}` | {f.get('label','')} | `{f['column']}` | `{f['javaType']}` | `{f['dbType']}` | "
            f"{'Y' if f.get('request') else 'N'} | {'Y' if f.get('condition') else 'N'} | "
            f"{'Y' if f.get('response') else 'N'} | {'N' if f.get('nullable', True) else 'Y'} | "
            f"{'Y' if f.get('sensitive') else 'N'} |"
        )
    rows_text = "\n".join(rows)
    text = f"""# 화면·이벤트 정의서

## 1. 기본정보

| 항목 | 값 |
|---|---|
| 화면 ID | `{model['screenId']}` |
| 화면명 | {model['screenName']} |
| 이벤트 ID | `{model['eventId']}` |
| 이벤트명 | {model['eventName']} |
| UI 객체 ID | `{model.get('uiObjectId') or ''}` |
| 호출 ServiceId | `{model['serviceId']}` |
| 거래코드 | `{model['transactionCode']}` |
| 성공 처리 | {model.get('successAction') or '처리 결과 표시 및 필요 시 재조회'} |
| 실패 처리 | {model.get('failureAction') or '표준 오류 메시지 표시, 입력 상태 유지'} |
| 중복 요청 방지 | {'적용' if model.get('idempotencyRequired') else '해당 없음/검토'} |

## 2. 필드 정의

{rows_text}

## 3. 처리 흐름

```text
화면 {model['screenId']}
  → 이벤트 {model['eventId']}
  → StandardRequest.header.serviceId = {model['serviceId']}
  → OnlineTransactionController
  → TCF / STF / TransactionDispatcher
  → 도메인 Handler / Facade / Service / Rule / DAO / Mapper
  → {model['tableName']}
  → ETF 표준 응답
```
"""
    return f"docs/screens/{model['screenId']}_{model['eventId'].split('-')[-1]}.md", text


def generate_transaction_definition(model: dict[str, Any]) -> tuple[str, str]:
    names = class_names(model)
    layout = package_layout(model)
    text = f"""# 거래설계서 — {model['serviceId']}

## 거래 식별

| 항목 | 값 |
|---|---|
| 업무코드 | `{model['businessCode']}` |
| 도메인 | `{model['domainCode']}` |
| ServiceId | `{model['serviceId']}` |
| 거래코드 | `{model['transactionCode']}` |
| 처리유형 | `{model['operation']}` |
| Timeout | `{model.get('timeoutSeconds', 3)}초` |
| 감사대상 | `{'Y' if model.get('auditRequired') else 'N'}` |
| 권한코드 | `{model.get('permissionCode') or ''}` |

## 실행 프로그램

| 계층 | 클래스/메서드 |
|---|---|
| Controller | `OnlineTransactionController.online()` |
| Handler | `{layout['handler']}.{names['handler']}` |
| Facade | `{layout['facade']}.{names['facade']}.{model['methodName']}()` |
| Service | `{layout['service']}.{names['service']}.{model['methodName']}()` |
| Rule | `{layout['rule']}.{names['rule']}` |
| DAO | `{layout['dao']}.{names['dao']}.{model['methodName']}()` |
| Mapper | `{layout['mapper']}.{names['mapper']}.{model['methodName']}` |
| Table | `{model['tableName']}` |

## 정상 흐름

```text
화면 이벤트
→ ServiceId 포함 표준전문
→ STF: Header·인증·권한·거래통제·Timeout 검증
→ Handler: ServiceId 분기
→ Facade: Transaction 경계
→ Service: 유스케이스 조립
→ Rule: 필수값·업무규칙 검증
→ DAO/Mapper: SQL 실행
→ ETF: 표준 성공 응답 및 거래로그 종료
```

## 오류·Timeout 흐름

- 필수값·업무규칙 위반: `BusinessException`으로 업무 오류 표준화
- Mapper/DB 오류: 시스템 오류로 변환하고 Rollback
- Timeout: TCF 전체 Timeout과 MyBatis Statement Timeout을 모두 적용
- 미등록 ServiceId: Dispatcher/Handler에서 실행 차단
- 변경 거래 감사로그: 사용자·지점·화면·ServiceId·변경대상·결과를 기록
"""
    return f"docs/transactions/{model['serviceId'].replace('.', '_')}.md", text


def generate_traceability_csv(models: list[dict[str, Any]]) -> tuple[str, str]:
    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow([
        "화면ID", "화면명", "이벤트ID", "이벤트명", "ServiceId", "거래코드", "Handler", "Facade",
        "Service", "Rule", "DAO", "Mapper", "SQL_ID", "Table", "Operation", "Timeout", "Audit"
    ])
    for model in models:
        n = class_names(model)
        writer.writerow([
            model["screenId"], model["screenName"], model["eventId"], model["eventName"], model["serviceId"],
            model["transactionCode"], n["handler"], n["facade"], n["service"], n["rule"], n["dao"], n["mapper"],
            model["methodName"], model["tableName"], model["operation"], model.get("timeoutSeconds", 3),
            "Y" if model.get("auditRequired") else "N"
        ])
    return "docs/TRACEABILITY_MATRIX.csv", output.getvalue()


def generate_quality_gate(models: list[dict[str, Any]]) -> tuple[str, str]:
    text = """# 자동검증 및 품질 Gate

## 생성 전 Gate

- [ ] 화면 ID·이벤트 ID·ServiceId·거래코드 형식 검증
- [ ] ServiceId·거래코드 중복 검증
- [ ] 업무코드·도메인·패키지 정합성 검증
- [ ] 조회/변경 유형과 거래코드 유형 정합성 검증
- [ ] 필수 요청·조건·응답 필드 검증
- [ ] 변경 거래 감사대상 검증
- [ ] 민감정보 마스킹 규칙 검증

## 코드 Gate

- [ ] Handler는 ServiceId 분기와 Facade 호출만 수행
- [ ] Transaction 경계는 Facade에 위치
- [ ] Service는 Mapper를 직접 호출하지 않음
- [ ] Rule은 DB/외부 시스템을 호출하지 않음
- [ ] DAO Method와 Mapper Statement ID는 1:1
- [ ] Mapper namespace와 Java Interface FQCN 일치
- [ ] SQL에 안전한 WHERE 조건 존재
- [ ] TCF Timeout ≥ DB/외부 호출 Timeout 합계 검토

## CI/CD Gate 예시

```text
1. model-validate
2. code-generate
3. compileJava
4. unitTest
5. ArchUnit 계층검사
6. ServiceId 중복검사
7. Mapper namespace/SQL ID 검사
8. DDL/SQL 정적검사
9. OM Catalog 등록정보 비교
10. 산출물 추적성 누락검사
```
"""
    return "QUALITY_GATE.md", text


def generate_manifest(models: list[dict[str, Any]], artifacts: dict[str, str]) -> tuple[str, str]:
    manifest = {
        "generator": "NSIGHT Model Studio",
        "version": "0.1.0",
        "modelCount": len(models),
        "serviceIds": [m["serviceId"] for m in models],
        "businessCodes": sorted({m["businessCode"] for m in models}),
        "files": sorted(artifacts.keys()),
    }
    return "manifest.json", json.dumps(manifest, ensure_ascii=False, indent=2)


def generate_workspace(models: list[dict[str, Any]]) -> dict[str, str]:
    if not models:
        raise ValueError("생성할 모델이 없습니다.")
    issues = []
    for model in models:
        issues.extend(validate_model(model))
    issues.extend(validate_workspace(models))
    if has_errors(issues):
        messages = "; ".join(item["message"] for item in issues if item["level"] == "ERROR")
        raise ValueError("모델 검증 실패: " + messages)

    artifacts: dict[str, str] = {}
    groups: dict[tuple[str, str, str], list[dict[str, Any]]] = defaultdict(list)
    for model in models:
        key = (model["businessCode"], model["domainCode"], model.get("packageProfile", "CURRENT_SOURCE"))
        groups[key].append(model)

    for _, group in sorted(groups.items()):
        group = sorted(group, key=lambda item: item["serviceId"])
        artifacts.update(generate_domain_classes(group))
        first = group[0]
        test_path, test_code = generate_rule_test(first)
        artifacts[test_path] = test_code

    seen_tables: set[str] = set()
    for model in models:
        generators = [generate_request_dto, generate_response_dto]
        if model["operation"] in {"SELECT_ONE", "SELECT_LIST"}:
            generators.extend([generate_criteria_dto, generate_row_dto])
        for generator in generators:
            path, content = generator(model)
            artifacts[path] = content
        if model["tableName"] not in seen_tables:
            path, content = generate_ddl(model)
            artifacts[path] = content
            seen_tables.add(model["tableName"])
        for generator in [generate_om_catalog, generate_http_request, generate_screen_definition, generate_transaction_definition]:
            path, content = generator(model)
            artifacts[path] = content

    path, content = generate_traceability_csv(models)
    artifacts[path] = content
    path, content = generate_quality_gate(models)
    artifacts[path] = content
    path, content = generate_manifest(models, artifacts)
    artifacts[path] = content
    return artifacts


def artifacts_to_zip(artifacts: dict[str, str]) -> bytes:
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w", zipfile.ZIP_DEFLATED) as archive:
        for path, content in sorted(artifacts.items()):
            archive.writestr(path, content.encode("utf-8-sig") if path.endswith(".csv") else content.encode("utf-8"))
    return buffer.getvalue()


def write_workspace(models: list[dict[str, Any]], output_dir: Path) -> dict[str, str]:
    artifacts = generate_workspace(models)
    for relative, content in artifacts.items():
        target = output_dir / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content, encoding="utf-8")
    return artifacts
