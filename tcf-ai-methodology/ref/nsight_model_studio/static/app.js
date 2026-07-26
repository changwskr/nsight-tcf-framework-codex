const state = {
  models: [],
  current: null,
  artifacts: [],
  previewPath: null,
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

async function loadModels(selectId = null) {
  const response = await api("/api/models");
  const data = await response.json();
  state.models = data.models || [];
  renderModelList();
  if (selectId) {
    const target = state.models.find(m => m.id === selectId);
    if (target) setCurrent(target);
  } else if (!state.current && state.models.length) {
    setCurrent(state.models[0]);
  }
}

function renderModelList() {
  const list = $("modelList");
  list.innerHTML = "";
  state.models.forEach(model => {
    const item = document.createElement("button");
    item.className = `model-item ${state.current?.id === model.id ? "active" : ""}`;
    item.innerHTML = `<strong>${escapeHtml(model.screenName || model.aggregateName || "미정 모델")}</strong><span>${escapeHtml(model.serviceId || "ServiceId 미정")}</span>`;
    item.addEventListener("click", () => setCurrent(model));
    list.appendChild(item);
  });
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
  if (!state.current?.id) return toast("먼저 모델을 저장하십시오.");
  try {
    const response = await api(`/api/models/${state.current.id}/duplicate`, { method: "POST", body: "{}" });
    const duplicated = await response.json();
    await loadModels(duplicated.id);
    toast("모델을 복제했습니다. 식별자를 수정하십시오.");
  } catch (error) { toast(error.message); }
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
    setCurrent(newBlankFromSample(sample));
    showStep(1);
    toast("새 업무모델 입력을 시작합니다.");
  } catch (error) { toast(error.message); }
}

function bindEvents() {
  document.querySelectorAll(".step").forEach(node => node.addEventListener("click", () => showStep(node.dataset.step)));
  scalarIds.forEach(id => $(id)?.addEventListener("change", updateDerived));
  $("newModelBtn").addEventListener("click", createNew);
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
