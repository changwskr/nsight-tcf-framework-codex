/**
 * nsight-tcf-framework 실코드(Handler + schema.sql) 분석 기반 Model Studio 시드 생성.
 * 실행: node generate-domain-models.js
 */
const fs = require("fs");
const path = require("path");

const BASE = {
  projectName: "NSIGHT 마케팅플랫폼",
  basePackage: "com.nh.nsight.marketing",
  packageProfile: "CURRENT_SOURCE",
};

const MODULES = {
  SV: { name: "Single View", module: "sv-service", ctx: "/sv" },
  IC: { name: "Integrated Customer", module: "ic-service", ctx: "/ic" },
  EB: { name: "Event Bridge", module: "eb-service", ctx: "/eb" },
  EP: { name: "Event Processor", module: "ep-service", ctx: "/ep" },
  PC: { name: "Private Customer", module: "pc-service", ctx: "/pc" },
  MS: { name: "Marketing Strategy", module: "ms-service", ctx: "/ms" },
  PD: { name: "Product", module: "pd-service", ctx: "/pd" },
  SS: { name: "Self Service", module: "ss-service", ctx: "/ss" },
  MG: { name: "Marketing Gateway", module: "mg-service", ctx: "/mg" },
  OM: { name: "Operations Management", module: "tcf-om", ctx: "/om" },
};

const OP_TX = {
  SELECT_ONE: "INQ",
  SELECT_LIST: "INQ",
  INSERT: "REG",
  UPDATE: "UPD",
  DELETE: "DEL",
};

const txUsed = {};
function nextTx(bc) {
  txUsed[bc] = (txUsed[bc] || 0) + 1;
  return String(txUsed[bc]).padStart(4, "0");
}

function f(p) {
  return {
    nullable: true,
    pk: false,
    request: false,
    condition: false,
    response: false,
    validation: "",
    sensitive: false,
    maskingRule: "",
    sampleValue: "",
    length: 50,
    javaType: "String",
    dbType: "VARCHAR2(50)",
    ...p,
  };
}

function pk(name, column, label, sample, o = {}) {
  return f({
    name, column, label, sampleValue: sample,
    length: o.length || 20,
    dbType: o.dbType || `VARCHAR2(${o.length || 20})`,
    javaType: o.javaType || "String",
    nullable: false, pk: true,
    request: o.request !== false,
    condition: o.condition !== false,
    response: o.response !== false,
    validation: o.validation || "required",
    sensitive: !!o.sensitive,
    maskingRule: o.maskingRule || "",
  });
}

function cond(name, column, label, sample, o = {}) {
  return f({
    name, column, label, sampleValue: sample,
    length: o.length || 20,
    dbType: o.dbType || `VARCHAR2(${o.length || 20})`,
    javaType: o.javaType || "String",
    request: true, condition: true,
    response: !!o.response,
    validation: o.validation || "",
    ...o,
  });
}

function resp(name, column, label, sample, o = {}) {
  return f({
    name, column, label, sampleValue: sample,
    length: o.length || 50,
    dbType: o.dbType || `VARCHAR2(${o.length || 50})`,
    javaType: o.javaType || "String",
    response: true,
    sensitive: !!o.sensitive,
    maskingRule: o.maskingRule || "",
    ...o,
  });
}

function req(name, column, label, sample, o = {}) {
  return f({
    name, column, label, sampleValue: sample,
    length: o.length || 50,
    dbType: o.dbType || `VARCHAR2(${o.length || 50})`,
    javaType: o.javaType || "String",
    nullable: o.nullable !== undefined ? o.nullable : false,
    request: true,
    response: !!o.response,
    validation: o.validation || "required",
    sensitive: !!o.sensitive,
    maskingRule: o.maskingRule || "",
    ...o,
  });
}

function inferOp(action) {
  if (/^(selectSummary|detail|session|healthCheck)$/i.test(action)) return "SELECT_ONE";
  if (/^(inquiry|history|buildStatus|logInquiry|frameworkInquiry)$/i.test(action)) return "SELECT_LIST";
  if (/^(create|save|receive|register)$/i.test(action)) return "INSERT";
  if (/^(update|icSample)$/i.test(action)) return action === "icSample" ? "SELECT_ONE" : "UPDATE";
  if (/^(delete|deleteAll|logout)$/i.test(action)) return "DELETE";
  if (/^(login|ssoLogin|execute|approve|reset)$/i.test(action)) return "INSERT";
  return "SELECT_LIST";
}

function inferMethod(action, domain) {
  if (action === "selectSummary") return "selectCustomerSummary";
  if (action === "icSample") return "selectIcSample";
  if (action === "inquiry") return `inquiry${domain}`;
  if (action === "detail") return `select${domain}Detail`;
  if (action === "create") return `create${domain}`;
  if (action === "receive") return `receive${domain}`;
  if (action === "save") return `save${domain}`;
  if (action === "update") return `update${domain}`;
  if (action === "delete") return `delete${domain}`;
  if (action === "deleteAll") return `deleteAll${domain}`;
  if (action === "login") return "login";
  if (action === "ssoLogin") return "ssoLogin";
  if (action === "logout") return "logout";
  if (action === "session") return "selectSession";
  if (action === "execute") return `execute${domain}`;
  if (action === "history") return `inquiry${domain}History`;
  return action;
}

function screenSeg(domain) {
  const map = {
    Customer: "CUS", Sample: "SMP", Integration: "INT", User: "USR", Event: "EVT",
    Batch: "BAT", SystemTx: "STX", UserEvent: "UEV", Menu: "MNU", AuthGroup: "AGR",
    ServiceCatalog: "SVC", CommonCode: "COD", ErrorCode: "ERR", TransactionLog: "TXL",
    AuditLog: "AUD", Session: "SES", Auth: "ATH", Dashboard: "DSH", SystemConfig: "CFG",
    FunctionAuth: "FNA", HealthCheck: "HLT", Cache: "CCH", MessageStructure: "MSG",
    TimeoutPolicy: "TMO", TransactionControl: "TXC", Deploy: "DEP", DataAuth: "DTA",
    FileDownload: "FIL", Runtime: "RUN", Strategy: "STR", Product: "PRD", Support: "SUP",
    Message: "MSG",
  };
  return map[domain] || domain.slice(0, 3).toUpperCase();
}

/** @type {Record<string, number>} */
const screenSeq = {};

function nextScreen(bc, domain) {
  const seg = screenSeg(domain);
  const key = `${bc}-${seg}`;
  screenSeq[key] = (screenSeq[key] || 0) + 1;
  return `${bc}-${seg}-${String(screenSeq[key]).padStart(4, "0")}`;
}

function make(def) {
  const [bc, domain, action] = def.serviceId.split(".");
  const mod = MODULES[bc];
  if (!mod) throw new Error("unknown BC " + bc);
  const operation = def.operation || inferOp(action);
  const method = def.method || inferMethod(action, domain);
  const screen = def.screen || nextScreen(bc, domain);
  const txNo = nextTx(bc);
  return {
    ...BASE,
    id: def.id || `${bc.toLowerCase()}-${domain.toLowerCase()}-${action.toLowerCase()}`,
    businessCode: bc,
    businessName: mod.name,
    moduleName: mod.module,
    contextPath: mod.ctx,
    domainCode: domain,
    domainName: def.domainName || domain,
    aggregateName: def.aggregate || `${domain}${action[0].toUpperCase()}${action.slice(1)}`,
    operation,
    methodName: method,
    screenId: screen,
    screenName: def.screenName || `${def.serviceName || def.serviceId}`,
    eventId: `${screen}-E01`,
    eventName: def.eventName || def.serviceName || action,
    uiObjectId: def.ui || (operation === "SELECT_LIST" || operation === "SELECT_ONE" ? "btnSearch" : "btnSave"),
    serviceId: def.serviceId,
    serviceName: def.serviceName || def.serviceId,
    transactionCode: `${bc}-${OP_TX[operation]}-${txNo}`,
    permissionCode: def.permission || `${bc}_${domain.toUpperCase()}_${OP_TX[operation]}`,
    timeoutSeconds: def.timeout || 3,
    auditRequired: def.audit !== undefined ? def.audit : ["INSERT", "UPDATE", "DELETE"].includes(operation),
    idempotencyRequired: !!def.idempotent,
    tableName: def.table,
    tableComment: def.tableComment || def.table,
    successAction: def.success || "결과 표시",
    failureAction: def.failure || "오류 메시지 표시",
    fields: def.fields,
  };
}

// —— 필드 세트 (schema.sql / DTO 기준) ——
const SV_CUSTOMER_FIELDS = [
  pk("customerNo", "CUSTOMER_NO", "고객번호", "C0000000000000000001", {
    sensitive: true, maskingRule: "뒤 4자리 마스킹",
  }),
  cond("baseDate", "BASE_DATE", "기준일자", "20260725", {
    length: 8, dbType: "CHAR(8)", validation: "date:yyyyMMdd", condition: false, response: false,
  }),
  resp("customerName", "CUSTOMER_NAME", "고객명", "홍길동", {
    length: 50, sensitive: true, maskingRule: "가운데 글자 마스킹",
  }),
  resp("customerGrade", "CUSTOMER_GRADE", "고객등급", "VIP", { length: 10 }),
  resp("branchCode", "BRANCH_CODE", "영업점코드", "000001", { length: 10 }),
  resp("branchName", "BRANCH_NAME", "영업점명", "본점", { length: 50 }),
  resp("totalBalance", "TOTAL_BALANCE", "총수신잔액", 1000000, {
    javaType: "Long", dbType: "NUMBER(18)", length: 18, sensitive: true, maskingRule: "금액권한 적용",
  }),
  resp("loanBalance", "LOAN_BALANCE", "여신잔액", 200000, {
    javaType: "Long", dbType: "NUMBER(18)", length: 18, sensitive: true, maskingRule: "금액권한 적용",
  }),
  resp("productCount", "PRODUCT_COUNT", "보유상품수", 3, {
    javaType: "Integer", dbType: "NUMBER(10)", length: 10,
  }),
  resp("lastTransactionDate", "LAST_TRANSACTION_DATE", "최종거래일", "20260720", {
    length: 8, dbType: "CHAR(8)",
  }),
];

const SV_SAMPLE_FIELDS = [
  cond("sampleKey", "SAMPLE_KEY", "샘플키", "S001", { length: 30 }),
  resp("sampleKeyOut", "SAMPLE_KEY_R", "샘플키", "S001", { length: 30, pk: true }),
  resp("sampleName", "SAMPLE_NAME", "샘플명", "데모샘플", { length: 100 }),
  resp("createdAt", "CREATED_AT", "생성일시", "2026-07-25T10:00:00", {
    javaType: "LocalDateTime", dbType: "TIMESTAMP", length: 30,
  }),
];

const IC_CUSTOMER_FIELDS = [
  pk("customerNo", "CUSTOMER_NO", "고객번호", "C0000000000000000001", {
    sensitive: true, maskingRule: "뒤 4자리 마스킹",
  }),
  resp("customerName", "CUSTOMER_NAME", "고객명", "홍길동", {
    length: 100, sensitive: true, maskingRule: "가운데 마스킹",
  }),
  resp("customerStatus", "CUSTOMER_STATUS", "고객상태", "ACTIVE", { length: 20 }),
];

const EB_USER_FIELDS_LIST = [
  cond("userId", "USER_ID", "사용자ID", "user01", { length: 50 }),
  resp("userIdOut", "USER_ID_R", "사용자ID", "user01", { length: 50, pk: true }),
  resp("userName", "USER_NAME", "사용자명", "홍길동", { length: 100 }),
  resp("branchId", "BRANCH_ID", "영업점", "000001", { length: 20 }),
];

const EB_USER_FIELDS_CREATE = [
  pk("userId", "USER_ID", "사용자ID", "user02", { length: 50, condition: false }),
  req("userName", "USER_NAME", "사용자명", "김영희", { length: 100 }),
  req("branchId", "BRANCH_ID", "영업점", "000002", { length: 20, nullable: true, validation: "" }),
];

const EB_EVENT_FIELDS = [
  cond("eventStatus", "EVENT_STATUS", "이벤트상태", "READY", { length: 20 }),
  resp("eventId", "EVENT_ID", "이벤트ID", "EVT-001", { length: 50, pk: true }),
  resp("userId", "USER_ID", "사용자ID", "user01", { length: 50 }),
  resp("eventType", "EVENT_TYPE", "이벤트유형", "USER_CREATED", { length: 30 }),
  resp("eventStatusOut", "EVENT_STATUS_R", "상태", "READY", { length: 20 }),
  resp("retryCount", "RETRY_COUNT", "재시도", 0, { javaType: "Integer", dbType: "NUMBER(10)", length: 10 }),
];

const EB_SYSTEM_TX_FIELDS = [
  cond("screenId", "SCREEN_ID", "화면ID", "19410", { length: 20 }),
  resp("txSeqNo", "TX_SEQ_NO", "거래일련번호", "TX202607250001", { length: 40, pk: true }),
  resp("txDate", "TX_DATE", "거래일자", "2026-07-25", { length: 10 }),
  resp("serviceId", "SERVICE_ID", "ServiceId", "EB.SystemTx.inquiry", { length: 80 }),
  resp("elapsedSec", "ELAPSED_SEC", "소요초", 1, { javaType: "Integer", dbType: "NUMBER(10)", length: 10 }),
  resp("txType", "TX_TYPE", "거래유형", "ONLINE", { length: 20 }),
];

const EP_USER_EVENT_LIST = [
  cond("userId", "USER_ID", "사용자ID", "user01", { length: 50 }),
  resp("eventId", "EVENT_ID", "이벤트ID", "UE-001", { length: 50, pk: true }),
  resp("eventType", "EVENT_TYPE", "유형", "LOGIN", { length: 30 }),
  resp("receivedAt", "RECEIVED_AT", "수신시각", "2026-07-25T10:00:00", {
    javaType: "LocalDateTime", dbType: "TIMESTAMP", length: 30,
  }),
];

const EP_USER_EVENT_RECV = [
  pk("eventId", "EVENT_ID", "이벤트ID", "UE-100", { length: 50, condition: false }),
  req("userId", "USER_ID", "사용자ID", "user01", { length: 50 }),
  req("eventType", "EVENT_TYPE", "유형", "LOGIN", { length: 30 }),
];

const OM_USER_LIST = [
  cond("userId", "USER_ID", "사용자ID", "admin", { length: 50 }),
  resp("userIdOut", "USER_ID_R", "사용자ID", "admin", { length: 50, pk: true }),
  resp("userName", "USER_NAME", "사용자명", "시스템관리자", { length: 100 }),
  resp("authGroupId", "AUTH_GROUP_ID", "권한그룹", "ADMIN", { length: 50 }),
  resp("useYn", "USE_YN", "사용여부", "Y", { length: 1, dbType: "CHAR(1)" }),
];

const OM_USER_DETAIL = [
  pk("userId", "USER_ID", "사용자ID", "admin", { length: 50 }),
  resp("userName", "USER_NAME", "사용자명", "시스템관리자", { length: 100 }),
  resp("branchId", "BRANCH_ID", "영업점", "000001", { length: 20 }),
  resp("authGroupId", "AUTH_GROUP_ID", "권한그룹", "ADMIN", { length: 50 }),
  resp("useYn", "USE_YN", "사용여부", "Y", { length: 1, dbType: "CHAR(1)" }),
];

const OM_USER_SAVE = [
  pk("userId", "USER_ID", "사용자ID", "ops01", { length: 50, condition: false }),
  req("userName", "USER_NAME", "사용자명", "운영자1", { length: 100 }),
  req("authGroupId", "AUTH_GROUP_ID", "권한그룹", "OPERATOR", { length: 50 }),
];

const OM_USER_UPDATE = [
  pk("userId", "USER_ID", "사용자ID", "ops01", { length: 50 }),
  req("userName", "USER_NAME", "사용자명", "운영자1-수정", { length: 100 }),
  req("useYn", "USE_YN", "사용여부", "Y", { length: 1, dbType: "CHAR(1)" }),
];

const OM_USER_DELETE = [
  pk("userId", "USER_ID", "사용자ID", "ops01", { length: 50, response: false }),
];

const OM_MENU = [
  cond("menuId", "MENU_ID", "메뉴ID", "M001", { length: 50 }),
  resp("menuIdOut", "MENU_ID_R", "메뉴ID", "M001", { length: 50, pk: true }),
  resp("menuName", "MENU_NAME", "메뉴명", "사용자관리", { length: 100 }),
  resp("menuUrl", "MENU_URL", "URL", "/om/user", { length: 200 }),
  resp("parentMenuId", "PARENT_MENU_ID", "상위메뉴", "ROOT", { length: 50 }),
];

const OM_AUTH_GROUP = [
  cond("authGroupId", "AUTH_GROUP_ID", "권한그룹ID", "ADMIN", { length: 50 }),
  resp("authGroupIdOut", "AUTH_GROUP_ID_R", "권한그룹ID", "ADMIN", { length: 50, pk: true }),
  resp("authGroupName", "AUTH_GROUP_NAME", "권한그룹명", "관리자", { length: 100 }),
];

const OM_SVC_CATALOG = [
  cond("serviceId", "SERVICE_ID", "ServiceId", "SV.Customer", { length: 100 }),
  resp("catalogId", "CATALOG_ID", "카탈로그ID", "CAT-001", { length: 64, pk: true }),
  resp("serviceIdOut", "SERVICE_ID_R", "ServiceId", "SV.Customer.selectSummary", { length: 100 }),
  resp("transactionCode", "TRANSACTION_CODE", "거래코드", "SV-INQ-0001", { length: 50 }),
  resp("timeoutSec", "TIMEOUT_SEC", "Timeout", 3, { javaType: "Integer", dbType: "NUMBER(5)", length: 5 }),
];

const OM_COMMON_CODE = [
  cond("codeGroup", "CODE_GROUP", "코드그룹", "CUSTOMER_GRADE", { length: 50 }),
  resp("codeGroupOut", "CODE_GROUP_R", "코드그룹", "CUSTOMER_GRADE", { length: 50, pk: true }),
  resp("codeValue", "CODE_VALUE", "코드값", "VIP", { length: 30 }),
  resp("codeName", "CODE_NAME", "코드명", "VIP고객", { length: 100 }),
];

const OM_ERROR_CODE = [
  cond("errorCode", "ERROR_CODE", "오류코드", "E0001", { length: 50 }),
  resp("errorCodeOut", "ERROR_CODE_R", "오류코드", "E0001", { length: 50, pk: true }),
  resp("errorCategory", "ERROR_CATEGORY", "분류", "BIZ", { length: 20 }),
  resp("userMessage", "USER_MESSAGE", "사용자메시지", "처리 중 오류", { length: 500 }),
];

const OM_TX_LOG = [
  cond("serviceId", "SERVICE_ID", "ServiceId", "SV.Customer", { length: 100 }),
  resp("logId", "LOG_ID", "로그ID", "LOG-001", { length: 64, pk: true }),
  resp("guid", "GUID", "GUID", "g-001", { length: 64 }),
  resp("resultStatus", "RESULT_STATUS", "결과", "SUCCESS", { length: 20 }),
  resp("elapsedTimeMs", "ELAPSED_TIME_MS", "소요ms", 120, {
    javaType: "Long", dbType: "NUMBER(18)", length: 18,
  }),
];

const OM_AUDIT = [
  cond("userId", "USER_ID", "사용자ID", "admin", { length: 50 }),
  resp("auditId", "AUDIT_ID", "감사ID", "AUD-001", { length: 64, pk: true }),
  resp("auditTime", "AUDIT_TIME", "감사시각", "20260725100000", { length: 40 }),
  resp("serviceId", "SERVICE_ID", "ServiceId", "OM.User.save", { length: 100 }),
];

const OM_BATCH = [
  cond("jobId", "JOB_ID", "잡ID", "JOB-SYNC", { length: 50 }),
  resp("jobIdOut", "JOB_ID_R", "잡ID", "JOB-SYNC", { length: 50, pk: true }),
  resp("jobName", "JOB_NAME", "잡명", "이벤트동기화", { length: 100 }),
  resp("cronExpr", "CRON_EXPR", "Cron", "0 0 * * * *", { length: 50 }),
];

const OM_SESSION = [
  cond("userId", "USER_ID", "사용자ID", "admin", { length: 50 }),
  resp("sessionId", "SESSION_ID", "세션ID", "SES-001", { length: 64, pk: true }),
  resp("userIdOut", "USER_ID_R", "사용자ID", "admin", { length: 50 }),
];

const OM_AUTH_LOGIN = [
  pk("userId", "USER_ID", "사용자ID", "admin", { length: 50, condition: false }),
  req("password", "PASSWORD_HASH", "비밀번호", "******", {
    length: 200, sensitive: true, maskingRule: "전부 마스킹",
  }),
];

const OM_DASHBOARD = [
  cond("businessCode", "BUSINESS_CODE", "업무코드", "SV", { length: 10 }),
  resp("metricCode", "METRIC_CODE", "지표코드", "TX_COUNT", { length: 30, pk: true }),
  resp("metricValue", "METRIC_VALUE", "지표값", 120, {
    javaType: "Long", dbType: "NUMBER(18)", length: 18,
  }),
];

const OM_SYS_CFG = [
  cond("configKey", "CONFIG_KEY", "설정키", "TIMEOUT_DEFAULT", { length: 50 }),
  resp("configKeyOut", "CONFIG_KEY_R", "설정키", "TIMEOUT_DEFAULT", { length: 50, pk: true }),
  resp("configValue", "CONFIG_VALUE", "설정값", "5", { length: 200 }),
];

const OM_FUNC_AUTH = [
  cond("authGroupId", "AUTH_GROUP_ID", "권한그룹", "ADMIN", { length: 50 }),
  resp("functionId", "FUNCTION_ID", "기능ID", "FN-USER", { length: 50, pk: true }),
  resp("functionName", "FUNCTION_NAME", "기능명", "사용자관리", { length: 100 }),
];

const OM_HEALTH = [
  cond("component", "COMPONENT", "구성요소", "DB", { length: 30 }),
  resp("componentOut", "COMPONENT_R", "구성요소", "DB", { length: 30, pk: true }),
  resp("statusCode", "STATUS_CODE", "상태", "UP", { length: 20 }),
];

const OM_CACHE = [
  cond("cacheName", "CACHE_NAME", "캐시명", "serviceCatalog", { length: 50 }),
  resp("cacheNameOut", "CACHE_NAME_R", "캐시명", "serviceCatalog", { length: 50, pk: true }),
  resp("entryCount", "ENTRY_COUNT", "건수", 120, {
    javaType: "Integer", dbType: "NUMBER(10)", length: 10,
  }),
];

const SAMPLE_GENERIC = (prefix) => [
  cond("sampleKey", "SAMPLE_KEY", "샘플키", `${prefix}-001`, { length: 30 }),
  resp("sampleKeyOut", "SAMPLE_KEY_R", "샘플키", `${prefix}-001`, { length: 30, pk: true }),
  resp("sampleName", "SAMPLE_NAME", "샘플명", `${prefix} 샘플`, { length: 100 }),
];

/** 프레임워크 Handler에 존재하는 ServiceId 카탈로그 */
const CATALOG = [
  // SV (실구현)
  {
    serviceId: "SV.Customer.selectSummary",
    domainName: "고객",
    serviceName: "고객 종합정보 조회",
    screenName: "고객 종합정보 조회",
    aggregate: "CustomerSummary",
    method: "selectCustomerSummary",
    table: "SV_CUSTOMER",
    tableComment: "고객 종합정보 (로컬 H2 / 운영 RDW JOIN)",
    fields: SV_CUSTOMER_FIELDS,
    source: "sv-service/.../SvCustomerHandler.java + schema.sql",
  },
  {
    serviceId: "SV.Sample.inquiry",
    domainName: "샘플",
    serviceName: "SV 샘플 조회",
    aggregate: "SampleInquiry",
    table: "SV_SAMPLE",
    fields: SV_SAMPLE_FIELDS,
  },
  {
    serviceId: "SV.Integration.icSample",
    domainName: "연동",
    serviceName: "IC 연동 샘플 조회",
    aggregate: "IntegrationIcSample",
    table: "SV_INTEGRATION_LOG",
    fields: [
      pk("customerNo", "CUSTOMER_NO", "고객번호", "C0000000000000000001", {
        sensitive: true, maskingRule: "뒤 4자리 마스킹",
      }),
      resp("icResultCode", "IC_RESULT_CODE", "IC결과코드", "0000", { length: 10 }),
      resp("icResultMessage", "IC_RESULT_MESSAGE", "IC결과메시지", "정상", { length: 200 }),
    ],
  },
  // IC
  {
    serviceId: "IC.Customer.inquiry",
    domainName: "고객",
    serviceName: "통합고객 조회",
    aggregate: "CustomerInquiry",
    table: "IC_CUSTOMER",
    fields: IC_CUSTOMER_FIELDS,
  },
  {
    serviceId: "IC.Sample.inquiry",
    domainName: "샘플",
    serviceName: "IC 샘플 조회",
    aggregate: "SampleInquiry",
    table: "IC_SAMPLE",
    fields: SAMPLE_GENERIC("IC"),
  },
  // EB
  {
    serviceId: "EB.User.inquiry",
    domainName: "사용자",
    serviceName: "EB 사용자 목록 조회",
    aggregate: "UserInquiry",
    table: "EB_USER",
    fields: EB_USER_FIELDS_LIST,
  },
  {
    serviceId: "EB.User.create",
    domainName: "사용자",
    serviceName: "EB 사용자 등록",
    aggregate: "UserCreate",
    table: "EB_USER",
    idempotent: true,
    fields: EB_USER_FIELDS_CREATE,
  },
  {
    serviceId: "EB.Event.inquiry",
    domainName: "이벤트",
    serviceName: "Outbox 이벤트 조회",
    aggregate: "EventInquiry",
    table: "EB_EVENT",
    fields: EB_EVENT_FIELDS,
  },
  {
    serviceId: "EB.Batch.inquiry",
    domainName: "배치",
    serviceName: "배치/이벤트 상태 조회",
    operation: "SELECT_ONE",
    aggregate: "BatchInquiry",
    table: "EB_EVENT",
    tableComment: "배치 상태(이벤트 집계)",
    fields: [
      pk("statusCode", "EVENT_STATUS", "상태코드", "READY", { length: 20 }),
      resp("readyCount", "READY_COUNT", "READY건수", 3, {
        javaType: "Integer", dbType: "NUMBER(10)", length: 10,
      }),
      resp("sentCount", "SENT_COUNT", "SENT건수", 10, {
        javaType: "Integer", dbType: "NUMBER(10)", length: 10,
      }),
    ],
  },
  {
    serviceId: "EB.SystemTx.inquiry",
    domainName: "시스템거래",
    serviceName: "시스템 거래 현황 조회",
    screen: "EB-STX-0001",
    screenName: "시스템 거래 현황",
    aggregate: "SystemTxInquiry",
    table: "EB_SYSTEM_TX",
    fields: EB_SYSTEM_TX_FIELDS,
  },
  {
    serviceId: "EB.Sample.inquiry",
    domainName: "샘플",
    serviceName: "EB 샘플 조회",
    aggregate: "SampleInquiry",
    table: "EB_SAMPLE",
    fields: SAMPLE_GENERIC("EB"),
  },
  // EP
  {
    serviceId: "EP.UserEvent.inquiry",
    domainName: "사용자이벤트",
    serviceName: "사용자 이벤트 목록 조회",
    aggregate: "UserEventInquiry",
    table: "EP_USER_EVENT",
    fields: EP_USER_EVENT_LIST,
  },
  {
    serviceId: "EP.UserEvent.receive",
    domainName: "사용자이벤트",
    serviceName: "사용자 이벤트 수신",
    aggregate: "UserEventReceive",
    table: "EP_USER_EVENT",
    idempotent: true,
    fields: EP_USER_EVENT_RECV,
  },
  {
    serviceId: "EP.Sample.inquiry",
    domainName: "샘플",
    serviceName: "EP 샘플 조회",
    aggregate: "SampleInquiry",
    table: "EP_SAMPLE",
    fields: SAMPLE_GENERIC("EP"),
  },
  // 스캐폴드 Sample
  ...["PC", "MS", "PD", "SS", "MG"].map((bc) => ({
    serviceId: `${bc}.Sample.inquiry`,
    domainName: "샘플",
    serviceName: `${bc} 샘플 조회`,
    aggregate: "SampleInquiry",
    table: `${bc}_SAMPLE`,
    fields: SAMPLE_GENERIC(bc),
  })),
  // OM 핵심
  {
    serviceId: "OM.User.inquiry",
    domainName: "운영사용자",
    serviceName: "운영 사용자 목록",
    aggregate: "UserInquiry",
    table: "OM_USER",
    fields: OM_USER_LIST,
  },
  {
    serviceId: "OM.User.detail",
    domainName: "운영사용자",
    serviceName: "운영 사용자 상세",
    aggregate: "UserDetail",
    table: "OM_USER",
    fields: OM_USER_DETAIL,
  },
  {
    serviceId: "OM.User.save",
    domainName: "운영사용자",
    serviceName: "운영 사용자 등록",
    aggregate: "UserSave",
    table: "OM_USER",
    fields: OM_USER_SAVE,
  },
  {
    serviceId: "OM.User.update",
    domainName: "운영사용자",
    serviceName: "운영 사용자 변경",
    aggregate: "UserUpdate",
    table: "OM_USER",
    fields: OM_USER_UPDATE,
  },
  {
    serviceId: "OM.User.delete",
    domainName: "운영사용자",
    serviceName: "운영 사용자 삭제",
    aggregate: "UserDelete",
    table: "OM_USER",
    fields: OM_USER_DELETE,
  },
  {
    serviceId: "OM.Menu.inquiry",
    domainName: "메뉴",
    serviceName: "메뉴 목록 조회",
    aggregate: "MenuInquiry",
    table: "OM_MENU",
    fields: OM_MENU,
  },
  {
    serviceId: "OM.AuthGroup.inquiry",
    domainName: "권한그룹",
    serviceName: "권한그룹 목록 조회",
    aggregate: "AuthGroupInquiry",
    table: "OM_AUTH_GROUP",
    fields: OM_AUTH_GROUP,
  },
  {
    serviceId: "OM.ServiceCatalog.inquiry",
    domainName: "서비스카탈로그",
    serviceName: "서비스 카탈로그 조회",
    aggregate: "ServiceCatalogInquiry",
    table: "OM_SERVICE_CATALOG",
    fields: OM_SVC_CATALOG,
  },
  {
    serviceId: "OM.CommonCode.inquiry",
    domainName: "공통코드",
    serviceName: "공통코드 조회",
    aggregate: "CommonCodeInquiry",
    table: "OM_COMMON_CODE",
    fields: OM_COMMON_CODE,
  },
  {
    serviceId: "OM.ErrorCode.inquiry",
    domainName: "오류코드",
    serviceName: "오류코드 조회",
    aggregate: "ErrorCodeInquiry",
    table: "OM_ERROR_CODE",
    fields: OM_ERROR_CODE,
  },
  {
    serviceId: "OM.TransactionLog.inquiry",
    domainName: "거래로그",
    serviceName: "거래 로그 조회",
    aggregate: "TransactionLogInquiry",
    table: "TCF_TX_LOG",
    fields: OM_TX_LOG,
  },
  {
    serviceId: "OM.AuditLog.inquiry",
    domainName: "감사로그",
    serviceName: "감사 로그 조회",
    aggregate: "AuditLogInquiry",
    table: "OM_AUDIT_LOG",
    fields: OM_AUDIT,
  },
  {
    serviceId: "OM.Batch.inquiry",
    domainName: "배치",
    serviceName: "배치 잡 조회",
    aggregate: "BatchInquiry",
    table: "OM_BATCH_JOB",
    fields: OM_BATCH,
  },
  {
    serviceId: "OM.Session.inquiry",
    domainName: "세션",
    serviceName: "세션 조회",
    aggregate: "SessionInquiry",
    table: "SPRING_SESSION",
    fields: OM_SESSION,
  },
  {
    serviceId: "OM.Auth.login",
    domainName: "인증",
    serviceName: "OM 로그인",
    aggregate: "AuthLogin",
    method: "login",
    table: "OM_USER",
    fields: OM_AUTH_LOGIN,
  },
  {
    serviceId: "OM.Auth.session",
    domainName: "인증",
    serviceName: "세션 조회",
    aggregate: "AuthSession",
    method: "selectSession",
    table: "SPRING_SESSION",
    fields: OM_SESSION,
  },
  {
    serviceId: "OM.Dashboard.inquiry",
    domainName: "대시보드",
    serviceName: "운영 대시보드 조회",
    aggregate: "DashboardInquiry",
    table: "OM_AP_STATUS",
    fields: OM_DASHBOARD,
  },
  {
    serviceId: "OM.SystemConfig.inquiry",
    domainName: "시스템설정",
    serviceName: "시스템 설정 조회",
    aggregate: "SystemConfigInquiry",
    table: "OM_SYSTEM_CONFIG",
    fields: OM_SYS_CFG,
  },
  {
    serviceId: "OM.FunctionAuth.inquiry",
    domainName: "기능권한",
    serviceName: "기능 권한 조회",
    aggregate: "FunctionAuthInquiry",
    table: "OM_FUNCTION_AUTH",
    fields: OM_FUNC_AUTH,
  },
  {
    serviceId: "OM.HealthCheck.inquiry",
    domainName: "헬스체크",
    serviceName: "헬스체크 조회",
    aggregate: "HealthCheckInquiry",
    table: "OM_AP_STATUS",
    fields: OM_HEALTH,
  },
  {
    serviceId: "OM.Cache.inquiry",
    domainName: "캐시",
    serviceName: "캐시 상태 조회",
    aggregate: "CacheInquiry",
    table: "OM_CACHE_STATUS",
    fields: OM_CACHE,
  },
  {
    serviceId: "OM.Sample.inquiry",
    domainName: "샘플",
    serviceName: "OM 샘플 조회",
    aggregate: "SampleInquiry",
    table: "OM_SAMPLE",
    fields: SAMPLE_GENERIC("OM"),
  },
];

const models = CATALOG.map(make);

// 컬럼 중복 자동 보정
for (const m of models) {
  const seen = new Map();
  for (const field of m.fields) {
    if (!field.column) continue;
    if (seen.has(field.column)) {
      if (field.response && !field.condition && !field.request) {
        let col = `${field.column}_R`;
        if (col.length > 30) col = col.slice(0, 30);
        field.column = col;
      }
    } else {
      seen.set(field.column, true);
    }
  }
}

const outDir = path.join(__dirname, "src", "main", "resources", "data");
const seedPath = path.join(outDir, "models-seed.json");
const inventoryPath = path.join(__dirname, "docs", "DOMAIN_MODEL_INVENTORY.md");

fs.writeFileSync(seedPath, JSON.stringify(models, null, 2), "utf8");

// sample_model = SV Customer 기준
const sample = models.find((m) => m.serviceId === "SV.Customer.selectSummary");
fs.writeFileSync(path.join(outDir, "sample_model.json"), JSON.stringify(sample, null, 2), "utf8");

const byBc = {};
for (const m of models) {
  byBc[m.businessCode] = byBc[m.businessCode] || [];
  byBc[m.businessCode].push(m);
}

const md = [
  "# NSIGHT 업무모델 인벤토리 (코드 분석 기반)",
  "",
  `생성일: 2026-07-25 · 총 **${models.length}**건`,
  "",
  "출처: `*-service` / `tcf-om` Handler의 ServiceId + `schema.sql` 컬럼.",
  "",
  "| BC | 모듈 | 건수 | 대표 ServiceId |",
  "|----|------|------|----------------|",
  ...Object.keys(byBc).sort().map((bc) => {
    const list = byBc[bc];
    const mod = MODULES[bc];
    return `| ${bc} | ${mod.module} | ${list.length} | ${list.slice(0, 3).map((m) => `\`${m.serviceId}\``).join(", ")} |`;
  }),
  "",
  "## 전체 ServiceId",
  "",
  ...models.map((m) => `- \`${m.serviceId}\` · ${m.operation} · \`${m.tableName}\` · ${m.screenId}`),
  "",
  "## 재생성",
  "",
  "```bash",
  "node tcf-ai-methodology/generate-domain-models.js",
  "```",
  "",
  "DB 반영: `POST /api/models/reseed`",
  "",
].join("\n");

fs.mkdirSync(path.dirname(inventoryPath), { recursive: true });
fs.writeFileSync(inventoryPath, md, "utf8");

console.log(`Wrote ${models.length} models -> ${seedPath}`);
console.log(`Inventory -> ${inventoryPath}`);
console.log(Object.entries(byBc).map(([k, v]) => `${k}:${v.length}`).join(" "));
