/*
 * tcf-ui/_shared/online-single.js 를 pdmp-service 전문 테스트용으로 옮긴 것.
 * tcf-ui는 업무코드별 /{bc}/online 단일 창구로 보내지만, pdmp-service는 거래마다
 * 엔드포인트가 다르므로 업무코드 대신 거래 ID를 고른다.
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

async function init() {
  const [txRes, configRes] = await Promise.all([
    fetch('/api/transactions'),
    fetch('/api/config')
  ]);
  transactions = await txRes.json();
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
  requestBodyEl.value = JSON.stringify(tx.sampleRequest, null, 2);
  updatePagingFields(tx.sampleRequest);
  await refreshTargetUrl();
}

function updatePagingFields(sampleRequest) {
  if (!pageFieldsEl || !pageNoEl || !pageSizeEl) {
    return;
  }

  const body = sampleRequest && sampleRequest.body ? sampleRequest.body : {};
  const hasPaging = body.hasOwnProperty('pageNo') || body.hasOwnProperty('pageSize');
  pageFieldsEl.style.display = hasPaging ? 'grid' : 'none';

  if (body.pageNo != null) {
    pageNoEl.value = body.pageNo;
  } else {
    pageNoEl.value = '1';
  }
  if (body.pageSize != null) {
    pageSizeEl.value = body.pageSize;
  } else {
    pageSizeEl.value = '20';
  }
}

function mergePagingIntoPayload(payload) {
  if (!pageNoEl || !pageSizeEl || !pageFieldsEl || pageFieldsEl.style.display === 'none') {
    return payload;
  }

  if (!payload.body || typeof payload.body !== 'object') {
    payload.body = {};
  }

  const pageNo = parseInt(pageNoEl.value, 10);
  if (!Number.isNaN(pageNo)) {
    payload.body.pageNo = pageNo;
  }

  const pageSize = parseInt(pageSizeEl.value, 10);
  if (!Number.isNaN(pageSize)) {
    payload.body.pageSize = pageSize;
  }

  return payload;
}

async function refreshTargetUrl() {
  const query = new URLSearchParams({ baseUrl: targetBaseUrlEl.value.trim() });
  const res = await fetch(`/api/transactions/${transactionIdEl.value}/target-url?${query}`);
  document.getElementById('metaTargetUrl').textContent =
    res.ok ? (await res.json()).targetUrl : 'URL 계산 실패';
}

// 표준 전문은 실패도 HTTP 200으로 내려온다. 성공 여부는 result.resultCode로 판별한다.
function transactionResult(parsed) {
  return parsed && parsed.result ? parsed.result : null;
}

function describeError(parsed, httpStatus) {
  const result = transactionResult(parsed);
  if (result && result.errorCode) {
    return `${result.errorCode} · ${result.errorMessage}`;
  }
  if (parsed && parsed.error) {
    return parsed.hint ? `${parsed.error} · ${parsed.hint}` : parsed.error;
  }
  return `HTTP ${httpStatus} 응답`;
}

async function sendRequest() {
  let payload;
  try {
    payload = JSON.parse(requestBodyEl.value);
  } catch (error) {
    alert('요청 JSON 형식이 올바르지 않습니다.\n' + error.message);
    return;
  }

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

  const txResult = transactionResult(parsed);
  const ok = httpOk && !(txResult && txResult.errorCode);
  const resultCode = txResult ? txResult.resultCode : null;

  responseMetaEl.innerHTML = `
    <span class="badge ${ok ? 'ok' : 'fail'}">HTTP ${result.httpStatus}</span>
    ${resultCode ? `<span class="badge ${ok ? 'ok' : 'fail'}">${resultCode}</span>` : ''}
    <span>${result.elapsedMs} ms</span>
    <span>${result.targetUrl}</span>
    ${ok ? '' : `<span class="badge fail">${describeError(parsed, result.httpStatus)}</span>`}
  `;
}

targetBaseUrlEl.addEventListener('change', refreshTargetUrl);
transactionIdEl.addEventListener('change', () => selectTransaction(transactionIdEl.value));
document.getElementById('reloadSampleBtn').addEventListener('click', () => selectTransaction(transactionIdEl.value));
document.getElementById('sendBtn').addEventListener('click', sendRequest);

init().catch(error => alert('화면 초기화 실패: ' + error.message));
