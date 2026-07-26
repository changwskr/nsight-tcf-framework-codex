#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Scan Handlers + BusinessModuleDefinitions → domain-ledger.json"""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

MODULES_META = [
    ("CC", "Common", "공통", 8081, None),
    ("IC", "Integration Customer", "고객", 8082, "ic-service"),
    ("PC", "Private Customer", "고객", 8083, "pc-service"),
    ("BC", "Business Customer", "고객", 8084, None),
    ("MS", "Mini Single View", "고객", 8085, "ms-service"),
    ("SV", "Single View", "마케팅", 8086, "sv-service"),
    ("PD", "Product", "마케팅", 8087, "pd-service"),
    ("CM", "Campaign", "마케팅", 8088, None),
    ("EB", "EBM", "마케팅", 8089, "eb-service"),
    ("AV", "AV Service", "마케팅", 8101, "av-service"),
    ("LN", "Loan / Customer Contact (시범)", "마케팅", 8103, "ln-service"),
    ("EP", "Event Processing", "실시간", 8090, "ep-service"),
    ("BP", "Behavior Processing", "실시간", 8091, None),
    ("BD", "Behavior Data", "데이터", 8092, None),
    ("SS", "Sales Support", "지원", 8093, "ss-service"),
    ("OC", "Operation Capacity (tcf-oc)", "운영", 8094, "tcf-oc"),
    ("CS", "Common Service", "지원", 8094, None),
    ("CT", "Contents", "지원", 8095, None),
    ("MG", "Message", "지원", 8096, "mg-service"),
    ("OM", "Operation Management (tcf-om)", "운영", 8097, "tcf-om"),
    ("UD", "Common UpDownload (tcf-om)", "공통", 8097, "tcf-om"),
    ("JWT", "JWT Auth (tcf-jwt)", "인증", 8110, "tcf-jwt"),
]

DOMAIN_NAMES = {
    "Sample": "샘플",
    "Customer": "고객",
    "CustomerContact": "고객연락처",
    "User": "사용자",
    "Event": "이벤트",
    "Batch": "배치",
    "SystemTx": "시스템거래",
    "UserEvent": "사용자이벤트",
    "Integration": "연계",
    "Menu": "메뉴",
    "AuthGroup": "권한그룹",
    "ServiceCatalog": "서비스카탈로그",
    "CommonCode": "공통코드",
    "ErrorCode": "오류코드",
    "TransactionLog": "거래로그",
    "AuditLog": "감사로그",
    "Session": "세션",
    "Auth": "인증",
    "Dashboard": "대시보드",
    "SystemConfig": "시스템설정",
    "FunctionAuth": "기능권한",
    "HealthCheck": "헬스체크",
    "Cache": "캐시",
    "TimeoutPolicy": "Timeout정책",
    "TransactionControl": "거래통제",
    "Runtime": "런타임",
    "Deploy": "배포",
    "FileDownload": "파일다운로드",
    "MessageStructure": "메시지구조",
    "DataAuth": "데이터권한",
    "AuthHistory": "인증이력",
    "RefreshToken": "Refresh토큰",
    "Token": "토큰",
    "SecurityPolicy": "보안정책",
    "LoginHistory": "로그인이력",
    "Hello": "Hello",
}

PAT = re.compile(r'"([A-Z]{2,3})\.([A-Za-z]+)\.([A-Za-z]+)"')


def operation(action: str) -> str:
    a = action.lower()
    if a in {
        "inquiry",
        "selectlist",
        "selectsummary",
        "detail",
        "selectdetail",
        "session",
        "history",
        "buildstatus",
        "healthcheck",
        "loginhistory",
        "frameworkinquiry",
        "loginquiry",
        "loginquiry",
    }:
        if "list" in a or a in {
            "inquiry",
            "history",
            "loginhistory",
            "frameworkinquiry",
            "loginquiry",
            "loginquiry",
        }:
            return "SELECT_LIST"
        return "SELECT_ONE"
    if a == "update":
        return "UPDATE"
    if a in {"delete", "deleteall", "revoke", "logout"}:
        return "DELETE"
    if a in {
        "create",
        "save",
        "receive",
        "execute",
        "deployrequest",
        "buildrequest",
        "rollbackrequest",
        "approve",
        "ssoissue",
        "ssologin",
        "login",
        "reset",
    }:
        return "INSERT"
    return "OTHER"


def main() -> None:
    by: dict[str, dict[str, set[str]]] = {}
    handlers: dict[tuple[str, str], str] = {}
    for path in ROOT.rglob("*Handler.java"):
        if "test" in path.as_posix().lower():
            continue
        text = path.read_text(encoding="utf-8")
        for m in PAT.finditer(text):
            bc, dom, act = m.group(1), m.group(2), m.group(3)
            by.setdefault(bc, {}).setdefault(dom, set()).add(f"{bc}.{dom}.{act}")
            handlers[(bc, dom)] = path.name

    modules = []
    for code, name, group, port, gradle in MODULES_META:
        domains = []
        for dom, sids in sorted(by.get(code, {}).items()):
            domains.append(
                {
                    "domainCode": dom,
                    "domainName": DOMAIN_NAMES.get(dom, dom),
                    "handler": handlers.get((code, dom)),
                    "serviceIds": sorted(
                        [
                            {
                                "serviceId": s,
                                "action": s.split(".")[-1],
                                "operation": operation(s.split(".")[-1]),
                            }
                            for s in sids
                        ],
                        key=lambda x: x["serviceId"],
                    ),
                }
            )
        if not gradle:
            status = "CATALOG_ONLY"
        elif not domains:
            status = "MODULE_EMPTY"
        else:
            status = "ACTIVE"
        modules.append(
            {
                "businessCode": code,
                "moduleName": name,
                "group": group,
                "localPort": port,
                "gradleModule": gradle,
                "status": status,
                "domainCount": len(domains),
                "serviceIdCount": sum(len(d["serviceIds"]) for d in domains),
                "domains": domains,
            }
        )

    out = {
        "version": "0.1.0",
        "generatedAt": "2026-07-26",
        "sourceNote": "BusinessModuleDefinitions + *Handler.java ServiceId scan [실제 소스 확인]",
        "moduleCount": len(modules),
        "domainCount": sum(m["domainCount"] for m in modules),
        "serviceIdCount": sum(m["serviceIdCount"] for m in modules),
        "modules": modules,
    }
    dest = ROOT / "tcf-ai-crud-meoy" / "src" / "main" / "resources" / "data" / "domain-ledger.json"
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8")
    print(
        f"wrote {dest} modules={out['moduleCount']} "
        f"domains={out['domainCount']} sids={out['serviceIdCount']}"
    )


if __name__ == "__main__":
    main()
