from __future__ import annotations

import re
from collections import Counter
from typing import Any

BUSINESS_CODE_RE = re.compile(r"^[A-Z]{2,3}$")
SCREEN_ID_RE = re.compile(r"^[A-Z]{2,3}-[A-Z0-9]{2,5}-\d{4}$")
EVENT_ID_RE = re.compile(r"^[A-Z]{2,3}-[A-Z0-9]{2,5}-\d{4}-E\d{2}$")
SERVICE_ID_RE = re.compile(r"^[A-Z]{2,3}\.[A-Z][A-Za-z0-9]*\.[a-z][A-Za-z0-9]*$")
TRANSACTION_CODE_RE = re.compile(r"^[A-Z]{2,3}-(INQ|REG|UPD|DEL|EXE)-\d{4}$")
TABLE_RE = re.compile(r"^[A-Z][A-Z0-9_]{2,29}$")
COLUMN_RE = re.compile(r"^[A-Z][A-Z0-9_]{1,29}$")
JAVA_FIELD_RE = re.compile(r"^[a-z][A-Za-z0-9]*$")
JAVA_CLASS_RE = re.compile(r"^[A-Z][A-Za-z0-9]*$")
JAVA_METHOD_RE = re.compile(r"^[a-z][A-Za-z0-9]*$")
BASE_PACKAGE_RE = re.compile(r"^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*)+$")

SUPPORTED_OPERATIONS = {"SELECT_ONE", "SELECT_LIST", "INSERT", "UPDATE", "DELETE"}
SUPPORTED_JAVA_TYPES = {
    "String",
    "Integer",
    "Long",
    "BigDecimal",
    "LocalDate",
    "LocalDateTime",
    "Boolean",
}
SUPPORTED_PACKAGE_PROFILES = {"CURRENT_SOURCE", "DOMAIN_FIRST"}


def issue(level: str, code: str, path: str, message: str) -> dict[str, str]:
    return {"level": level, "code": code, "path": path, "message": message}


def validate_model(model: dict[str, Any]) -> list[dict[str, str]]:
    issues: list[dict[str, str]] = []

    required = [
        ("projectName", "프로젝트명"),
        ("basePackage", "BASE 패키지"),
        ("businessCode", "업무코드"),
        ("moduleName", "업무 모듈명"),
        ("domainCode", "도메인 코드"),
        ("aggregateName", "유스케이스/DTO 기준명"),
        ("operation", "처리유형"),
        ("methodName", "메서드명"),
        ("screenId", "화면 ID"),
        ("screenName", "화면명"),
        ("eventId", "이벤트 ID"),
        ("eventName", "이벤트명"),
        ("serviceId", "ServiceId"),
        ("transactionCode", "거래코드"),
        ("tableName", "테이블명"),
    ]
    for key, label in required:
        if not str(model.get(key, "")).strip():
            issues.append(issue("ERROR", "REQ-001", key, f"{label}은(는) 필수입니다."))

    business = str(model.get("businessCode", "")).strip()
    if business and not BUSINESS_CODE_RE.fullmatch(business):
        issues.append(issue("ERROR", "NAM-001", "businessCode", "업무코드는 영문 대문자 2~3자리여야 합니다."))

    base_package = str(model.get("basePackage", "")).strip()
    if base_package and not BASE_PACKAGE_RE.fullmatch(base_package):
        issues.append(issue("ERROR", "NAM-002", "basePackage", "BASE 패키지는 소문자 점(.) 구분 Java 패키지 형식이어야 합니다."))

    screen_id = str(model.get("screenId", "")).strip()
    if screen_id and not SCREEN_ID_RE.fullmatch(screen_id):
        issues.append(issue("ERROR", "NAM-003", "screenId", "화면 ID 형식은 {업무코드}-{세구분}-{4자리}여야 합니다."))
    elif screen_id and business and not screen_id.startswith(business + "-"):
        issues.append(issue("ERROR", "TRC-001", "screenId", "화면 ID의 업무코드와 모델 업무코드가 일치하지 않습니다."))

    event_id = str(model.get("eventId", "")).strip()
    if event_id and not EVENT_ID_RE.fullmatch(event_id):
        issues.append(issue("ERROR", "NAM-004", "eventId", "이벤트 ID 형식은 {화면ID}-E{2자리}여야 합니다."))
    elif event_id and screen_id and not event_id.startswith(screen_id + "-E"):
        issues.append(issue("ERROR", "TRC-002", "eventId", "이벤트 ID는 화면 ID 하위로 구성해야 합니다."))

    service_id = str(model.get("serviceId", "")).strip()
    if service_id and not SERVICE_ID_RE.fullmatch(service_id):
        issues.append(issue("ERROR", "NAM-005", "serviceId", "ServiceId 형식은 {업무코드}.{도메인}.{행위}여야 합니다."))
    elif service_id and business and not service_id.startswith(business + "."):
        issues.append(issue("ERROR", "TRC-003", "serviceId", "ServiceId의 업무코드와 모델 업무코드가 일치하지 않습니다."))

    transaction_code = str(model.get("transactionCode", "")).strip()
    if transaction_code and not TRANSACTION_CODE_RE.fullmatch(transaction_code):
        issues.append(issue("ERROR", "NAM-006", "transactionCode", "거래코드는 {업무코드}-{INQ|REG|UPD|DEL|EXE}-{4자리} 형식이어야 합니다."))
    elif transaction_code and business and not transaction_code.startswith(business + "-"):
        issues.append(issue("ERROR", "TRC-004", "transactionCode", "거래코드의 업무코드와 모델 업무코드가 일치하지 않습니다."))

    operation = str(model.get("operation", "")).strip()
    if operation and operation not in SUPPORTED_OPERATIONS:
        issues.append(issue("ERROR", "MOD-001", "operation", f"지원하지 않는 처리유형입니다: {operation}"))

    method_name = str(model.get("methodName", "")).strip()
    if method_name and not JAVA_METHOD_RE.fullmatch(method_name):
        issues.append(issue("ERROR", "NAM-007", "methodName", "메서드명은 lowerCamelCase Java 식별자여야 합니다."))

    aggregate = str(model.get("aggregateName", "")).strip()
    if aggregate and not JAVA_CLASS_RE.fullmatch(aggregate):
        issues.append(issue("ERROR", "NAM-008", "aggregateName", "유스케이스/DTO 기준명은 UpperCamelCase Java 식별자여야 합니다."))

    domain_code = str(model.get("domainCode", "")).strip()
    if domain_code and not JAVA_CLASS_RE.fullmatch(domain_code):
        issues.append(issue("ERROR", "NAM-009", "domainCode", "도메인 코드는 UpperCamelCase Java 식별자여야 합니다."))
    if service_id and domain_code:
        parts = service_id.split(".")
        if len(parts) == 3 and parts[1] != domain_code:
            issues.append(issue("ERROR", "TRC-005", "domainCode", "ServiceId 도메인 구간과 도메인 코드가 일치하지 않습니다."))

    table_name = str(model.get("tableName", "")).strip()
    if table_name and not TABLE_RE.fullmatch(table_name):
        issues.append(issue("ERROR", "NAM-010", "tableName", "테이블명은 영문 대문자·숫자·언더스코어 3~30자여야 합니다."))

    timeout = model.get("timeoutSeconds")
    try:
        timeout_value = int(timeout)
        if timeout_value < 1 or timeout_value > 120:
            raise ValueError
    except (TypeError, ValueError):
        issues.append(issue("ERROR", "NFR-001", "timeoutSeconds", "Timeout은 1~120초 범위의 정수여야 합니다."))

    profile = str(model.get("packageProfile", "CURRENT_SOURCE"))
    if profile not in SUPPORTED_PACKAGE_PROFILES:
        issues.append(issue("ERROR", "MOD-002", "packageProfile", "지원하지 않는 패키지 프로파일입니다."))

    fields = model.get("fields") or []
    if not isinstance(fields, list) or not fields:
        issues.append(issue("ERROR", "DAT-001", "fields", "최소 1개 이상의 필드를 정의해야 합니다."))
        return issues

    field_names: list[str] = []
    columns: list[str] = []
    pk_count = 0
    condition_count = 0
    response_count = 0
    request_count = 0

    for idx, field in enumerate(fields):
        path = f"fields[{idx}]"
        name = str(field.get("name", "")).strip()
        column = str(field.get("column", "")).strip()
        java_type = str(field.get("javaType", "")).strip()
        db_type = str(field.get("dbType", "")).strip()

        if not name:
            issues.append(issue("ERROR", "DAT-002", path + ".name", "Java 필드명은 필수입니다."))
        elif not JAVA_FIELD_RE.fullmatch(name):
            issues.append(issue("ERROR", "NAM-011", path + ".name", "필드명은 lowerCamelCase Java 식별자여야 합니다."))
        field_names.append(name)

        if not column:
            issues.append(issue("ERROR", "DAT-003", path + ".column", "DB 컬럼명은 필수입니다."))
        elif not COLUMN_RE.fullmatch(column):
            issues.append(issue("ERROR", "NAM-012", path + ".column", "컬럼명은 영문 대문자·숫자·언더스코어 형식이어야 합니다."))
        columns.append(column)

        if java_type not in SUPPORTED_JAVA_TYPES:
            issues.append(issue("ERROR", "DAT-004", path + ".javaType", f"지원하지 않는 Java 타입입니다: {java_type}"))
        if not db_type:
            issues.append(issue("ERROR", "DAT-005", path + ".dbType", "DB 타입은 필수입니다."))

        if bool(field.get("pk")):
            pk_count += 1
        if bool(field.get("condition")):
            condition_count += 1
        if bool(field.get("response")):
            response_count += 1
        if bool(field.get("request")):
            request_count += 1

        if bool(field.get("sensitive")) and not str(field.get("maskingRule", "")).strip():
            issues.append(issue("WARNING", "SEC-001", path + ".maskingRule", "민감정보 필드는 마스킹 규칙을 지정하는 것이 좋습니다."))
        if not bool(field.get("nullable", True)) and not bool(field.get("request")) and operation in {"INSERT", "UPDATE"}:
            issues.append(issue("WARNING", "DAT-006", path, "NOT NULL 컬럼이 요청 필드에 포함되지 않았습니다."))

    for name, count in Counter(field_names).items():
        if name and count > 1:
            issues.append(issue("ERROR", "DAT-007", "fields", f"Java 필드명이 중복되었습니다: {name}"))
    for column, count in Counter(columns).items():
        if column and count > 1:
            issues.append(issue("ERROR", "DAT-008", "fields", f"DB 컬럼명이 중복되었습니다: {column}"))

    if pk_count == 0:
        issues.append(issue("WARNING", "DAT-009", "fields", "PK 필드가 정의되지 않았습니다. 변경·삭제·단건조회 영향분석이 어려울 수 있습니다."))
    if operation in {"SELECT_ONE", "SELECT_LIST", "UPDATE", "DELETE"} and condition_count == 0:
        issues.append(issue("ERROR", "SQL-001", "fields", "조회·변경·삭제 거래에는 최소 1개의 조회조건 필드가 필요합니다."))
    if operation in {"SELECT_ONE", "SELECT_LIST"} and response_count == 0:
        issues.append(issue("ERROR", "DTO-001", "fields", "조회 거래에는 최소 1개의 응답 필드가 필요합니다."))
    if operation in {"INSERT", "UPDATE"} and request_count == 0:
        issues.append(issue("ERROR", "DTO-002", "fields", "등록·변경 거래에는 최소 1개의 요청 필드가 필요합니다."))
    if operation in {"INSERT", "UPDATE", "DELETE"} and not bool(model.get("auditRequired", False)):
        issues.append(issue("WARNING", "SEC-002", "auditRequired", "데이터 변경 거래는 감사로그 대상으로 지정하는 것이 원칙입니다."))

    expected_code = {
        "SELECT_ONE": "INQ",
        "SELECT_LIST": "INQ",
        "INSERT": "REG",
        "UPDATE": "UPD",
        "DELETE": "DEL",
    }.get(operation)
    if expected_code and transaction_code and f"-{expected_code}-" not in transaction_code:
        issues.append(issue("ERROR", "TRC-006", "transactionCode", "처리유형과 거래코드 유형이 일치하지 않습니다."))

    if service_id and method_name:
        action = service_id.split(".")[-1]
        action_parts = re.findall(r"[A-Z]?[a-z]+|[A-Z]+(?=[A-Z]|$)|\d+", action)
        action_verb = action_parts[0].lower() if action_parts else action.lower()
        action_object = "".join(action_parts[1:]).lower()
        method_lower = method_name.lower()
        compatible = (
            action == method_name
            or method_lower.endswith(action.lower())
            or (method_lower.startswith(action_verb) and (not action_object or method_lower.endswith(action_object)))
        )
        if not compatible:
            issues.append(issue("WARNING", "TRC-007", "methodName", "ServiceId 행위명과 Facade/Service 메서드명이 다릅니다."))

    return issues


def validate_workspace(models: list[dict[str, Any]]) -> list[dict[str, str]]:
    issues: list[dict[str, str]] = []
    service_ids = Counter(str(m.get("serviceId", "")).strip() for m in models)
    transaction_codes = Counter(str(m.get("transactionCode", "")).strip() for m in models)
    screen_events = Counter((str(m.get("screenId", "")).strip(), str(m.get("eventId", "")).strip()) for m in models)

    for service_id, count in service_ids.items():
        if service_id and count > 1:
            issues.append(issue("ERROR", "WS-001", "serviceId", f"Workspace 내 ServiceId가 중복되었습니다: {service_id}"))
    for transaction_code, count in transaction_codes.items():
        if transaction_code and count > 1:
            issues.append(issue("ERROR", "WS-002", "transactionCode", f"Workspace 내 거래코드가 중복되었습니다: {transaction_code}"))
    for pair, count in screen_events.items():
        if all(pair) and count > 1:
            issues.append(issue("WARNING", "WS-003", "eventId", f"동일 화면 이벤트가 여러 모델에 중복 정의되었습니다: {pair[0]} / {pair[1]}"))

    groups: dict[tuple[str, str, str], list[dict[str, Any]]] = {}
    for model in models:
        key = (
            str(model.get("businessCode", "")),
            str(model.get("domainCode", "")),
            str(model.get("packageProfile", "CURRENT_SOURCE")),
        )
        groups.setdefault(key, []).append(model)

    for key, group in groups.items():
        aggregate_names = Counter(str(m.get("aggregateName", "")) for m in group)
        method_names = Counter(str(m.get("methodName", "")) for m in group)
        for value, count in aggregate_names.items():
            if value and count > 1:
                issues.append(issue("ERROR", "WS-004", "aggregateName", f"동일 도메인 내 DTO 기준명이 중복되었습니다: {key[0]}.{key[1]} / {value}"))
        for value, count in method_names.items():
            if value and count > 1:
                issues.append(issue("ERROR", "WS-005", "methodName", f"동일 도메인 내 메서드명이 중복되었습니다: {key[0]}.{key[1]} / {value}"))

    return issues


def has_errors(issues: list[dict[str, str]]) -> bool:
    return any(item.get("level") == "ERROR" for item in issues)
