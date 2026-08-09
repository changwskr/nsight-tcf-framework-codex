/*
 * pdmg-service 전문 테스트용 단일 거래 화면 스크립트.
 * 전문 형식: { "hdr_nhnis": { "sys_comm": { ... } }, "dto": { ... } }
 */

let transactions = [];
let config = {};

const targetBaseUrlEl = document.getElementById('targetBaseUrl');
const transactionIdEl = document.getElementById('transactionId');
const requestBodyEl = document.getElementById('requestBody');
const pageFieldsEl = document.getElementById('pageFields');
const pageNoEl = document.getElementById('pageNo');
const pageSizeEl = document.getElementById('pageSize');
const responseBodyEl = document.getElementById('responseBody');
const responseMetaEl = document.getElementById('responseMeta');

function defaultTransactionId() {
  return document.documentElement.dataset.defaultTransactionId || '';
}

function programFilter() {
  return document.documentElement.dataset.programId || '';
}

function newGuid() {
  if (window.crypto && typeof window.crypto.randomUUID === 'function') {
    return window.crypto.randomUUID().replace(/-/g, '');
  }
  return 'xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx'.replace(/x/g, () =>
    ((Math.random() * 16) | 0).toString(16));
}

function emptySysComm(guid) {
  return {
    std_gbl_id: guid || newGuid(),
    rms_svc_c: null,
    orgtr_gbl_id: null,
    trz_gbl_id: null,
    sync_dsc: null,
    async_attr_c: null,
    tr_sysid: null,
    ttl_ug_ync: 0,
    std_tgrm_rqr_rsp_dsc: null,
    std_tgrm_lclc: null,
    tr_trm_ipadr: '127.0.0.1',
    tr_dtm: null,
    tr_brc: '10001',
    naac_dsc: null,
    trmn_naac_dsc: null,
    trmno: null,
    trm_kdc: null,
    scid: null,
    optr_eno: 'E0000001',
    tr_optrnm: null,
    optr_pzcc: null
  };
}

/** 샘플/요청 전문에 hdr_nhnis 를 맞추고 GUID를 새로 채번한다. */
function ensureHdrNhnis(payload, refreshGuid) {
  if (!payload || typeof payload !== 'object' || Array.isArray(payload)) {
    payload = { dto: {} };
  }
  if (!payload.hdr_nhnis || typeof payload.hdr_nhnis !== 'object') {
    payload.hdr_nhnis = {};
  }
  if (!payload.hdr_nhnis.sys_comm || typeof payload.hdr_nhnis.sys_comm !== 'object') {
    payload.hdr_nhnis.sys_comm = emptySysComm();
  } else if (refreshGuid || !payload.hdr_nhnis.sys_comm.std_gbl_id) {
    payload.hdr_nhnis.sys_comm.std_gbl_id = newGuid();
  }
  if (!payload.dto || typeof payload.dto !== 'object') {
    payload.dto = {};
  }
  return payload;
}

function formatSample(sampleRequest) {
  return ensureHdrNhnis(
    sampleRequest && typeof sampleRequest === 'object'
      ? JSON.parse(JSON.stringify(sampleRequest))
      : { dto: {} },
    true
  );
}

async function init() {
  const [txRes, configRes] = await Promise.all([
    fetch('/api/transactions'),
    fetch('/api/config')
  ]);
  const all = await txRes.json();
  const programId = programFilter();
  transactions = programId ? all.filter(tx => tx.programId === programId) : all;
  config = await configRes.json();

  targetBaseUrlEl.value = config.targetBaseUrl || 'http://localhost:8080';
  renderTransactionOptions();

  const id = defaultTransactionId();
  const selected = transactions.some(tx => tx.id === id) ? id : transactions[0]?.id;
  if (selected) {
    transactionIdEl.value = selected;
    await selectTransaction(selected);
  }
}

function renderTransactionOptions() {
  transactionIdEl.innerHTML = transactions
    .map(tx => `<option value="${tx.id}">${tx.id} - ${tx.name}</option>`)
    .join('');
}

async function selectTransaction(id) {
  const tx = transactions.find(item => item.id === id);
  if (!tx) return;

  document.getElementById('metaName').innerHTML =
    `${tx.name}<span class="group-tag">${tx.programId}</span>`;
  document.getElementById('metaEndpoint').textContent = `${tx.method} ${tx.path}`;
  document.getElementById('metaDescription').textContent = tx.description;

  const sample = formatSample(tx.sampleRequest);
  requestBodyEl.value = JSON.stringify(sample, null, 2);
  updatePagingFields(sample);
  await refreshTargetUrl();
}

function dtoOf(sampleRequest) {
  return sampleRequest && sampleRequest.dto && typeof sampleRequest.dto === 'object'
      ? sampleRequest.dto
      : {};
}

function updatePagingFields(sampleRequest) {
  if (!pageFieldsEl || !pageNoEl || !pageSizeEl) {
    return;
  }

  const dto = dtoOf(sampleRequest);
  const hasPaging = Object.prototype.hasOwnProperty.call(dto, 'pageNo')
      || Object.prototype.hasOwnProperty.call(dto, 'pageSize');
  pageFieldsEl.style.display = hasPaging ? 'grid' : 'none';

  pageNoEl.value = dto.pageNo != null ? dto.pageNo : '1';
  pageSizeEl.value = dto.pageSize != null ? dto.pageSize : '20';
}

function mergePagingIntoPayload(payload) {
  if (!pageNoEl || !pageSizeEl || !pageFieldsEl || pageFieldsEl.style.display === 'none') {
    return payload;
  }

  if (!payload.dto || typeof payload.dto !== 'object') {
    payload.dto = {};
  }

  const pageNo = parseInt(pageNoEl.value, 10);
  if (!Number.isNaN(pageNo)) {
    payload.dto.pageNo = pageNo;
  }

  const pageSize = parseInt(pageSizeEl.value, 10);
  if (!Number.isNaN(pageSize)) {
    payload.dto.pageSize = pageSize;
  }

  return payload;
}

async function refreshTargetUrl() {
  const query = new URLSearchParams({ baseUrl: targetBaseUrlEl.value.trim() });
  const res = await fetch(`/api/transactions/${transactionIdEl.value}/target-url?${query}`);
  document.getElementById('metaTargetUrl').textContent =
    res.ok ? (await res.json()).targetUrl : 'URL 계산 실패';
}

function describeError(parsed, httpStatus) {
  if (parsed && parsed.error) {
    return parsed.hint ? `${parsed.error} · ${parsed.hint}` : parsed.error;
  }
  if (parsed && parsed.message) {
    return parsed.message;
  }
  return `HTTP ${httpStatus} 응답`;
}

function guidOf(parsed) {
  return parsed?.hdr_nhnis?.sys_comm?.std_gbl_id || '-';
}

function dtoSummary(parsed) {
  const dto = parsed?.dto;
  if (!dto || typeof dto !== 'object') {
    return '';
  }
  if (dto.totalCount != null) {
    return `Total: ${dto.totalCount}`;
  }
  if (Array.isArray(dto.records)) {
    return `records: ${dto.records.length}`;
  }
  if (Array.isArray(dto.mgcoa8888S0DTOSub0)) {
    return `Total: ${dto.size != null ? dto.size : dto.mgcoa8888S0DTOSub0.length}`;
  }
  if (Array.isArray(dto.mgcoa9999S0DTOSub0)) {
    return `Total: ${dto.size != null ? dto.size : dto.mgcoa9999S0DTOSub0.length}`;
  }
  if (dto.size != null) {
    return `Total: ${dto.size}`;
  }
  return '';
}

function refreshGuidInEditor() {
  let payload;
  try {
    payload = JSON.parse(requestBodyEl.value);
  } catch (error) {
    alert('요청 JSON 형식이 올바르지 않습니다.\n' + error.message);
    return;
  }
  payload = ensureHdrNhnis(payload, true);
  requestBodyEl.value = JSON.stringify(payload, null, 2);
}

async function sendRequest() {
  let payload;
  try {
    payload = JSON.parse(requestBodyEl.value);
  } catch (error) {
    alert('요청 JSON 형식이 올바르지 않습니다.\n' + error.message);
    return;
  }

  payload = ensureHdrNhnis(payload, false);
  payload = mergePagingIntoPayload(payload);
  requestBodyEl.value = JSON.stringify(payload, null, 2);

  responseMetaEl.innerHTML = '<span class="empty">요청 중...</span>';
  responseBodyEl.value = '';

  const query = new URLSearchParams({ baseUrl: targetBaseUrlEl.value.trim() });
  const response = await fetch(`/api/relay/${transactionIdEl.value}?${query}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  const result = await response.json();
  const httpOk = result.httpStatus >= 200 && result.httpStatus < 300;

  let parsed = null;
  try {
    parsed = JSON.parse(result.responseBody);
    responseBodyEl.value = JSON.stringify(parsed, null, 2);
  } catch (error) {
    responseBodyEl.value = result.responseBody || '';
  }

  const ok = httpOk && !(parsed && parsed.error);
  const summary = dtoSummary(parsed);
  responseMetaEl.innerHTML = `
    <span class="badge ${ok ? 'ok' : 'fail'}">HTTP ${result.httpStatus}</span>
    <span>${result.elapsedMs} ms</span>
    <span>GUID: ${guidOf(parsed)}</span>
    ${summary ? `<span>${summary}</span>` : ''}
    <span>${result.targetUrl}</span>
    ${ok ? '' : `<span class="badge fail">${describeError(parsed, result.httpStatus)}</span>`}
  `;
}

targetBaseUrlEl.addEventListener('change', refreshTargetUrl);
transactionIdEl.addEventListener('change', () => selectTransaction(transactionIdEl.value));
document.getElementById('reloadSampleBtn').addEventListener('click', () => selectTransaction(transactionIdEl.value));
const refreshGuidBtn = document.getElementById('refreshGuidBtn');
if (refreshGuidBtn) {
  refreshGuidBtn.addEventListener('click', refreshGuidInEditor);
}
document.getElementById('sendBtn').addEventListener('click', sendRequest);

init().catch(error => alert('화면 초기화 실패: ' + error.message));
