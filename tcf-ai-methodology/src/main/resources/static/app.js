const state = {
  models: [],
  filteredSidebar: [],
  browseResults: [],
  current: null,
  artifacts: [],
  previewPath: null,
  view: "editor", // editor | browse | guide
  sidebarQuery: "",
};

const $ = (id) => document.getElementById(id);
const scalarIds = [
  "projectName", "basePackage", "packageProfile", "businessCode", "businessName", "moduleName", "contextPath",
  "domainCode", "domainName", "aggregateName", "screenId", "screenName", "uiObjectId", "eventId", "eventName",
  "successAction", "failureAction", "serviceId", "serviceName", "transactionCode", "operation", "methodName",
  "permissionCode", "timeoutSeconds", "tableName", "tableComment", "auditRequired", "idempotencyRequired"
];

async function api(path, options = {}) {
  const response = await fetch(path, {
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options,
  });
  if (!response.ok) {
    let message = `${response.status} ${response.statusText}`;
    try { message = (await response.json()).error || message; } catch (_) {}
    throw new Error(message);
  }
  return response;
}

function toast(message) {
  const node = $("toast");
  node.textContent = message;
  node.classList.add("show");
  setTimeout(() => node.classList.remove("show"), 2600);
}

function status(message) { $("statusText").textContent = message; }

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>'"]/g, (ch) => ({"&":"&amp;","<":"&lt;",">":"&gt;","'":"&#39;",'"':"&quot;"}[ch]));
}

function newBlankFromSample(sample) {
  return {
    ...structuredClone(sample),
    id: "",
    projectName: sample.projectName,
    serviceId: "",
    transactionCode: "",
    methodName: "",
    aggregateName: "",
    screenId: "",
    screenName: "",
    eventId: "",
    eventName: "",
    serviceName: "",
    fields: []
  };
}

/** 조회·선택 모델을 '신규 업무모델' 초안으로 쓴다. DB 저장 없이 필드·도메인을 유지한다. */
function newFromTemplate(source) {
  if (!source) return null;
  const model = structuredClone(source);
  model.id = "";
  model.serviceId = "";
  model.serviceName = "";
  model.transactionCode = "";
  model.methodName = "";
  model.aggregateName = "";
  model.screenId = "";
  model.screenName = (source.screenName || source.aggregateName || "신규 화면") + " (신규)";
  model.eventId = "";
  model.eventName = "";
  model.uiObjectId = source.uiObjectId || "btnSearch";
  return model;
}

async function loadModels(selectId = null) {
  const response = await api("/api/models");
  const data = await response.json();
  state.models = data.models || [];
  renderModelList();
  refreshBrowseFilters();
  if (state.view === "browse") renderBrowseTable();
  if (selectId) {
    const target = state.models.find(m => m.id === selectId);
    if (target) setCurrent(target);
  } else if (!state.current && state.models.length) {
    setCurrent(state.models[0]);
  }
  $("sidebarCount").textContent = String(state.models.length);
}

function renderModelList() {
  const list = $("modelList");
  list.innerHTML = "";
  const q = (state.sidebarQuery || "").trim().toLowerCase();
  const models = !q ? state.models : state.models.filter(m => {
    const hay = [m.screenName, m.serviceId, m.businessCode, m.domainCode, m.aggregateName]
      .map(v => String(v || "").toLowerCase()).join(" ");
    return hay.includes(q);
  });
  state.filteredSidebar = models;
  if (!models.length) {
    list.innerHTML = `<div class="model-empty">저장된 모델이 없습니다.</div>`;
    return;
  }
  models.forEach(model => {
    const item = document.createElement("button");
    item.className = `model-item ${state.current?.id === model.id ? "active" : ""}`;
    item.innerHTML = `<strong>${escapeHtml(model.screenName || model.aggregateName || "미정 모델")}</strong>
      <span>${escapeHtml(model.businessCode || "-")} · ${escapeHtml(model.serviceId || "ServiceId 미정")}</span>`;
    item.addEventListener("click", () => {
      showEditorView();
      setCurrent(model);
    });
    list.appendChild(item);
  });
}

function refreshBrowseFilters() {
  const bcSelect = $("browseBusinessCode");
  const domainSelect = $("browseDomain");
  const currentBc = bcSelect.value;
  const currentDomain = domainSelect.value;
  const codes = [...new Set(state.models.map(m => m.businessCode).filter(Boolean))].sort();
  const domains = [...new Set(state.models.map(m => m.domainCode).filter(Boolean))].sort();
  bcSelect.innerHTML = `<option value="">전체</option>` + codes.map(c => `<option value="${escapeHtml(c)}">${escapeHtml(c)}</option>`).join("");
  domainSelect.innerHTML = `<option value="">전체</option>` + domains.map(d => `<option value="${escapeHtml(d)}">${escapeHtml(d)}</option>`).join("");
  bcSelect.value = codes.includes(currentBc) ? currentBc : "";
  domainSelect.value = domains.includes(currentDomain) ? currentDomain : "";
}

function operationLabel(op) {
  return ({
    SELECT_ONE: "단건",
    SELECT_LIST: "목록",
    INSERT: "등록",
    UPDATE: "변경",
    DELETE: "삭제",
  })[op] || op || "-";
}

async function searchBrowse() {
  try {
    status("저장 모델 조회 중...");
    const q = ($("browseQuery").value || "").trim();
    const url = q ? `/api/models?q=${encodeURIComponent(q)}` : "/api/models";
    const response = await api(url);
    const data = await response.json();
    let models = data.models || [];
    const bc = $("browseBusinessCode").value;
    const op = $("browseOperation").value;
    const domain = $("browseDomain").value;
    if (bc) models = models.filter(m => m.businessCode === bc);
    if (op) models = models.filter(m => m.operation === op);
    if (domain) models = models.filter(m => m.domainCode === domain);
    state.browseResults = models;
    renderBrowseTable();
    status(`조회 완료: ${models.length}건`);
  } catch (error) {
    toast(error.message);
    status("조회 실패");
  }
}

function renderBrowseTable() {
  const rows = $("browseRows");
  const empty = $("browseEmpty");
  const list = state.browseResults;
  $("browseTotalBadge").textContent = `${list.length}건`;
  renderBrowseMetrics(list);
  rows.innerHTML = "";
  if (!list.length) {
    empty.classList.remove("hidden");
    return;
  }
  empty.classList.add("hidden");
  list.forEach(model => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td><span class="bc-chip">${escapeHtml(model.businessCode || "-")}</span></td>
      <td>${escapeHtml(model.domainCode || "-")}<div class="sub">${escapeHtml(model.domainName || "")}</div></td>
      <td class="mono">${escapeHtml(model.serviceId || "-")}<div class="sub">${escapeHtml(model.serviceName || "")}</div></td>
      <td>${escapeHtml(model.screenId || "-")}<div class="sub">${escapeHtml(model.screenName || "")}</div></td>
      <td><span class="op-chip">${escapeHtml(operationLabel(model.operation))}</span></td>
      <td class="mono">${escapeHtml(model.tableName || "-")}</td>
      <td class="mono">${escapeHtml(model.transactionCode || "-")}</td>
      <td>
        <button type="button" class="ghost small open-model">열기</button>
        <button type="button" class="ghost small template-model">템플릿으로 신규</button>
      </td>`;
    tr.querySelector(".open-model").addEventListener("click", () => {
      showEditorView();
      setCurrent(model);
      showStep(1);
    });
    tr.querySelector(".template-model").addEventListener("click", () => startFromTemplate(model));
    rows.appendChild(tr);
  });
}

function renderBrowseMetrics(models) {
  const byBc = {};
  const byOp = {};
  models.forEach(m => {
    byBc[m.businessCode || "?"] = (byBc[m.businessCode || "?"] || 0) + 1;
    byOp[m.operation || "?"] = (byOp[m.operation || "?"] || 0) + 1;
  });
  const bcText = Object.entries(byBc).sort((a, b) => b[1] - a[1]).slice(0, 6)
    .map(([k, v]) => `${k} ${v}`).join(" · ") || "-";
  const opText = Object.entries(byOp).map(([k, v]) => `${operationLabel(k)} ${v}`).join(" · ") || "-";
  $("browseMetrics").innerHTML = `
    <div class="metric"><span>조회 결과</span><strong>${models.length}</strong></div>
    <div class="metric"><span>전체 저장</span><strong>${state.models.length}</strong></div>
    <div class="metric"><span>업무코드별</span><strong class="metric-text">${escapeHtml(bcText)}</strong></div>
    <div class="metric"><span>처리유형별</span><strong class="metric-text">${escapeHtml(opText)}</strong></div>`;
}

function showBrowseView() {
  state.view = "browse";
  if (!state.browseResults.length) state.browseResults = [...state.models];
  $("browseView").classList.remove("hidden");
  $("editorView").classList.add("hidden");
  $("guideView")?.classList.add("hidden");
  $("editorToolbar").classList.add("hidden");
  $("browseToolbar").classList.remove("hidden");
  $("guideToolbar")?.classList.add("hidden");
  $("pageTitle").textContent = "저장된 업무모델 조회";
  renderBrowseTable();
  status(`조회 화면 · 저장 ${state.models.length}건`);
}

function showEditorView() {
  state.view = "editor";
  $("browseView").classList.add("hidden");
  $("guideView")?.classList.add("hidden");
  $("editorView").classList.remove("hidden");
  $("editorToolbar").classList.remove("hidden");
  $("browseToolbar").classList.add("hidden");
  $("guideToolbar")?.classList.add("hidden");
  if (state.current) {
    $("pageTitle").textContent = state.current.screenName || state.current.aggregateName || "업무모델 정의";
  } else {
    $("pageTitle").textContent = "업무모델 정의";
  }
}

function setCurrent(model) {
  state.current = structuredClone(model);
  scalarIds.forEach(id => {
    const node = $(id);
    if (!node) return;
    const value = state.current[id];
    if (id === "auditRequired" || id === "idempotencyRequired") node.value = String(Boolean(value));
    else node.value = value ?? "";
  });
  renderFields(state.current.fields || []);
  updateDerived();
  $("pageTitle").textContent = state.current.screenName || state.current.aggregateName || "업무모델 정의";
  renderModelList();
  clearValidation();
  state.artifacts = [];
  renderArtifacts();
}

function collectModel() {
  const model = { ...(state.current || {}) };
  scalarIds.forEach(id => {
    const node = $(id);
    if (!node) return;
    if (id === "auditRequired" || id === "idempotencyRequired") model[id] = node.value === "true";
    else if (id === "timeoutSeconds") model[id] = Number(node.value || 0);
    else model[id] = node.value.trim();
  });
  model.fields = [...document.querySelectorAll("#fieldRows tr")].map(row => ({
    name: row.querySelector('[data-key="name"]').value.trim(),
    label: row.querySelector('[data-key="label"]').value.trim(),
    column: row.querySelector('[data-key="column"]').value.trim(),
    javaType: row.querySelector('[data-key="javaType"]').value,
    dbType: row.querySelector('[data-key="dbType"]').value.trim(),
    length: Number(row.querySelector('[data-key="length"]').value || 0),
    nullable: row.querySelector('[data-key="nullable"]').checked,
    pk: row.querySelector('[data-key="pk"]').checked,
    request: row.querySelector('[data-key="request"]').checked,
    condition: row.querySelector('[data-key="condition"]').checked,
    response: row.querySelector('[data-key="response"]').checked,
    sensitive: row.querySelector('[data-key="sensitive"]').checked,
    validation: row.querySelector('[data-key="validation"]').value.trim(),
    maskingRule: row.querySelector('[data-key="maskingRule"]').value.trim(),
    sampleValue: row.querySelector('[data-key="sampleValue"]').value.trim(),
  }));
  state.current = model;
  return model;
}

function fieldRow(field = {}) {
  const tr = document.createElement("tr");
  tr.innerHTML = `
    <td><input class="mini-text" data-key="name" value="${escapeHtml(field.name || "")}" placeholder="customerNo"></td>
    <td><input class="mini-text" data-key="label" value="${escapeHtml(field.label || "")}" placeholder="고객번호"></td>
    <td><input class="mini-text" data-key="column" value="${escapeHtml(field.column || "")}" placeholder="CUSTOMER_NO"></td>
    <td><select data-key="javaType">
      ${["String","Integer","Long","BigDecimal","LocalDate","LocalDateTime","Boolean"].map(t => `<option value="${t}" ${field.javaType === t ? "selected" : ""}>${t}</option>`).join("")}
    </select></td>
    <td><input class="wide-text" data-key="dbType" value="${escapeHtml(field.dbType || "VARCHAR2(100)")}"></td>
    <td><input data-key="length" type="number" value="${field.length || 0}" min="0"></td>
    ${["nullable","pk","request","condition","response","sensitive"].map(key => `<td class="checkbox-cell"><input data-key="${key}" type="checkbox" ${field[key] ? "checked" : ""}></td>`).join("")}
    <td>
      <input class="wide-text" data-key="validation" value="${escapeHtml(field.validation || "")}" placeholder="required|maxLength:20">
      <input class="wide-text" data-key="maskingRule" value="${escapeHtml(field.maskingRule || "")}" placeholder="마스킹 규칙">
      <input class="wide-text" data-key="sampleValue" value="${escapeHtml(field.sampleValue ?? "")}" placeholder="샘플값">
    </td>
    <td><button type="button" class="remove-field">×</button></td>`;
  tr.querySelector(".remove-field").addEventListener("click", () => tr.remove());
  tr.querySelectorAll("input,select").forEach(node => node.addEventListener("change", updateDerived));
  return tr;
}

function renderFields(fields) {
  const body = $("fieldRows");
  body.innerHTML = "";
  fields.forEach(field => body.appendChild(fieldRow(field)));
}

function addField() {
  $("fieldRows").appendChild(fieldRow({ javaType: "String", dbType: "VARCHAR2(100)", nullable: true }));
}

function bizClass(code) {
  if (!code) return "Biz";
  return code[0].toUpperCase() + code.slice(1).toLowerCase();
}

function packageLayout(model) {
  const base = model.basePackage || "com.nh.nsight.marketing";
  const biz = (model.businessCode || "sv").toLowerCase();
  const domain = model.domainCode || "Domain";
  const domainLower = domain[0]?.toLowerCase() + domain.slice(1);
  if (model.packageProfile === "DOMAIN_FIRST") {
    return `${base}.${biz}.${domainLower}`;
  }
  return `${base}.${biz}`;
}

function updateDerived() {
  const model = collectModel();
  const prefix = bizClass(model.businessCode);
  const domain = model.domainCode || "Domain";
  const root = packageLayout(model);
  const classes = [
    ["Handler", `${prefix}${domain}Handler`],
    ["Facade", `${prefix}${domain}Facade`],
    ["Service", `${prefix}${domain}Service`],
    ["Rule", `${prefix}${domain}Rule`],
    ["DAO", `${prefix}${domain}Dao`],
    ["Mapper", `${prefix}${domain}Mapper`],
    ["Request DTO", `${model.aggregateName || "UseCase"}Request`],
    ["패키지 Root", root],
  ];
  $("namingPreview").innerHTML = classes.map(([label, value]) => `<div class="name-chip"><small>${label}</small><code>${escapeHtml(value)}</code></div>`).join("");
  $("traceFlow").innerHTML = [
    model.screenId || "화면", model.eventId || "이벤트", model.serviceId || "ServiceId",
    `${prefix}${domain}Handler`, `${prefix}${domain}Facade`, `${prefix}${domain}Service`, `${prefix}${domain}Rule`,
    `${prefix}${domain}Dao`, `${prefix}${domain}Mapper.${model.methodName || "sqlId"}`, model.tableName || "TABLE"
  ].map((value, i, arr) => `<span class="trace-node">${escapeHtml(value)}</span>${i < arr.length - 1 ? '<span class="trace-arrow">→</span>' : ''}`).join("");
  if (state.view === "editor") {
    const title = model.screenName || model.aggregateName || "업무모델 정의";
    $("pageTitle").textContent = title;
  }
}

async function saveModel() {
  try {
    status("저장 중...");
    const model = collectModel();
    const method = model.id ? "PUT" : "POST";
    const path = model.id ? `/api/models/${model.id}` : "/api/models";
    const response = await api(path, { method, body: JSON.stringify(model) });
    const saved = await response.json();
    state.current = saved;
    await loadModels(saved.id);
    toast("업무모델을 저장했습니다.");
    status("저장 완료");
  } catch (error) { toast(error.message); status("저장 실패"); }
}

async function validateCurrent(switchStep = true) {
  try {
    status("모델 검증 중...");
    const model = collectModel();
    const response = await api("/api/validate", { method: "POST", body: JSON.stringify({ model }) });
    const result = await response.json();
    renderValidation(result);
    if (switchStep) showStep(5);
    status(`검증 완료: 오류 ${result.errorCount}, 경고 ${result.warningCount}`);
    return result;
  } catch (error) { toast(error.message); status("검증 실패"); throw error; }
}

async function validateAll() {
  try {
    showEditorView();
    status("Workspace 검증 중...");
    const response = await api("/api/validate-workspace", { method: "POST", body: JSON.stringify({}) });
    const result = await response.json();
    renderValidation(result);
    showStep(5);
    status(`전체 검증 완료: 오류 ${result.errorCount}, 경고 ${result.warningCount}`);
  } catch (error) { toast(error.message); }
}

function renderValidation(result) {
  $("errorCount").textContent = result.errorCount || 0;
  $("warningCount").textContent = result.warningCount || 0;
  const box = $("validationResults");
  const issues = result.issues || [];
  if (!issues.length) {
    box.className = "validation-results empty";
    box.textContent = "모든 자동검증 항목을 통과했습니다.";
    return;
  }
  box.className = "validation-results";
  box.innerHTML = issues.map(item => `<div class="issue ${item.level.toLowerCase()}">
    <span class="level">${escapeHtml(item.level)}</span><code>${escapeHtml(item.code)}</code><span class="path">${escapeHtml(item.path)}</span><span>${escapeHtml(item.message)}</span>
  </div>`).join("");
}

function clearValidation() {
  $("errorCount").textContent = "0";
  $("warningCount").textContent = "0";
  $("validationResults").className = "validation-results empty";
  $("validationResults").textContent = "검증을 실행하면 결과가 표시됩니다.";
}

async function previewCurrent(path = null, useSaved = false) {
  try {
    status("생성 산출물 미리보기 중...");
    let models = [collectModel()];
    if (useSaved && state.models.length) models = state.models;
    const response = await api("/api/preview", { method: "POST", body: JSON.stringify({ models, path }) });
    const result = await response.json();
    state.artifacts = result.paths;
    state.previewPath = result.path;
    $("previewPath").textContent = result.path;
    $("previewCode").textContent = result.content;
    renderArtifacts();
    showStep(6);
    status(`미리보기 완료: ${result.paths.length}개 산출물`);
  } catch (error) { toast(error.message); status("미리보기 실패"); }
}

function renderArtifacts() {
  const list = $("artifactList");
  if (!state.artifacts.length) {
    list.innerHTML = `<div class="artifact-item">검증 후 미리보기를 실행하십시오.</div>`;
    return;
  }
  list.innerHTML = "";
  state.artifacts.forEach(path => {
    const item = document.createElement("button");
    item.className = `artifact-item ${state.previewPath === path ? "active" : ""}`;
    item.textContent = path;
    item.addEventListener("click", () => previewCurrent(path, false));
    list.appendChild(item);
  });
}

async function downloadZip(savedAll = false) {
  try {
    status("ZIP 생성 중...");
    let response;
    if (savedAll) {
      response = await api("/api/generate-saved", { method: "POST", body: JSON.stringify({}) });
    } else {
      const model = collectModel();
      response = await api("/api/generate", { method: "POST", body: JSON.stringify({ models: [model], filename: `${model.businessCode || 'NSIGHT'}-${model.domainCode || 'model'}-generated.zip` }) });
    }
    const blob = await response.blob();
    const disposition = response.headers.get("Content-Disposition") || "";
    const match = disposition.match(/filename="?([^";]+)"?/);
    const filename = match ? match[1] : "nsight-generated.zip";
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url; a.download = filename; document.body.appendChild(a); a.click(); a.remove();
    URL.revokeObjectURL(url);
    toast(`${filename} 생성 완료`);
    status("ZIP 생성 완료");
  } catch (error) { toast(error.message); status("ZIP 생성 실패"); }
}

async function duplicateCurrent() {
  if (!state.current?.id) {
    return toast("저장된 모델을 연 다음 「복제 저장」을 사용하십시오. 미저장 초안은 「저장」을 먼저 하세요.");
  }
  try {
    showEditorView();
    const response = await api(`/api/models/${state.current.id}/duplicate`, { method: "POST", body: "{}" });
    const duplicated = await response.json();
    await loadModels(duplicated.id);
    showStep(1);
    toast("DB에 복제본을 저장했습니다. ServiceId·화면ID를 업무에 맞게 수정하세요.");
    status("복제 저장 완료");
  } catch (error) { toast(error.message); }
}

function startFromTemplate(source) {
  const base = source || state.current;
  if (!base) return toast("템플릿으로 쓸 모델을 먼저 조회·선택하십시오.");
  showEditorView();
  setCurrent(newFromTemplate(base));
  showStep(1);
  toast("선택 모델을 템플릿으로 신규 작성합니다. ServiceId·화면·거래를 채운 뒤 저장하세요.");
  status("템플릿으로 신규 작성 중 (미저장)");
}

async function deleteCurrent() {
  if (!state.current?.id) return toast("저장되지 않은 모델입니다.");
  if (!confirm("현재 업무모델을 삭제하시겠습니까?")) return;
  try {
    await api(`/api/models/${state.current.id}`, { method: "DELETE" });
    state.current = null;
    await loadModels();
    toast("모델을 삭제했습니다.");
  } catch (error) { toast(error.message); }
}

function showStep(number) {
  document.querySelectorAll(".step").forEach(node => node.classList.toggle("active", Number(node.dataset.step) === Number(number)));
  document.querySelectorAll(".step-panel").forEach(node => node.classList.toggle("active", node.id === `step${number}`));
  if (number === 5) updateDerived();
}

async function createNew() {
  try {
    const response = await api("/api/sample");
    const sample = await response.json();
    showEditorView();
    setCurrent(newBlankFromSample(sample));
    showStep(1);
    toast("새 업무모델 입력을 시작합니다.");
  } catch (error) { toast(error.message); }
}

function bindEvents() {
  document.querySelectorAll(".step").forEach(node => node.addEventListener("click", () => showStep(node.dataset.step)));
  scalarIds.forEach(id => {
    $(id)?.addEventListener("change", updateDerived);
    $(id)?.addEventListener("input", updateDerived);
  });
  $("newModelBtn").addEventListener("click", createNew);
  $("browseBtn").addEventListener("click", () => {
    state.browseResults = [...state.models];
    showBrowseView();
  });
  $("guideBtn")?.addEventListener("click", () => {
    if (typeof showGuideView === "function") showGuideView();
  });
  $("reloadGuideBtn")?.addEventListener("click", () => {
    if (typeof reloadGuideContent === "function") reloadGuideContent();
  });
  $("backFromGuideBtn")?.addEventListener("click", showEditorView);
  $("backToEditorBtn").addEventListener("click", showEditorView);
  $("refreshBrowseBtn").addEventListener("click", async () => {
    await loadModels(state.current?.id);
    await searchBrowse();
  });
  $("browseSearchBtn").addEventListener("click", searchBrowse);
  $("browseResetBtn").addEventListener("click", () => {
    $("browseQuery").value = "";
    $("browseBusinessCode").value = "";
    $("browseOperation").value = "";
    $("browseDomain").value = "";
    state.browseResults = [...state.models];
    renderBrowseTable();
    status(`조회 초기화 · ${state.models.length}건`);
  });
  $("browseQuery").addEventListener("keydown", (e) => {
    if (e.key === "Enter") searchBrowse();
  });
  ["browseBusinessCode", "browseOperation", "browseDomain"].forEach(id => {
    $(id).addEventListener("change", searchBrowse);
  });
  $("sidebarSearch").addEventListener("input", (e) => {
    state.sidebarQuery = e.target.value;
    renderModelList();
  });
  $("addFieldBtn").addEventListener("click", addField);
  $("saveBtn").addEventListener("click", saveModel);
  $("validateBtn").addEventListener("click", () => validateCurrent(true));
  $("runValidationBtn").addEventListener("click", () => validateCurrent(false));
  $("validateAllBtn").addEventListener("click", validateAll);
  $("previewBtn").addEventListener("click", () => previewCurrent(null, false));
  $("refreshPreviewBtn").addEventListener("click", () => previewCurrent(null, false));
  $("generateBtn").addEventListener("click", () => downloadZip(false));
  $("generateAllBtn").addEventListener("click", () => downloadZip(true));
  $("duplicateBtn").addEventListener("click", duplicateCurrent);
  $("newFromTemplateBtn").addEventListener("click", () => startFromTemplate(state.current));
  $("deleteBtn").addEventListener("click", deleteCurrent);
  $("copyCodeBtn").addEventListener("click", async () => {
    await navigator.clipboard.writeText($("previewCode").textContent);
    toast("코드를 복사했습니다.");
  });
}

(async function init() {
  bindEvents();
  renderArtifacts();
  await loadModels();
  status("준비");
})().catch(error => toast(error.message));
