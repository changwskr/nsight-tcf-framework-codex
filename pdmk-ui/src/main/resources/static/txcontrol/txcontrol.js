/*
 * 거래통제 UI — 설계서 01.UI설계서-거래통제UI설계.md
 * Catalog CRUD(S0/I0/U0/D0) + 평가(E0) + 집계(S2) + 상태변경(U1)
 */

const targetBaseUrlEl = document.getElementById('targetBaseUrl');
const qServiceCodeEl = document.getElementById('qServiceCode');
const qBusinessCodeEl = document.getElementById('qBusinessCode');
const qStatusEl = document.getElementById('qStatus');
const qEnabledEl = document.getElementById('qEnabled');
const pageSizeEl = document.getElementById('pageSize');
const resultMetaEl = document.getElementById('resultMeta');
const resultCountEl = document.getElementById('resultCount');
const resultBodyEl = document.getElementById('resultBody');
const pageInfoEl = document.getElementById('pageInfo');
const prevPageBtn = document.getElementById('prevPageBtn');
const nextPageBtn = document.getElementById('nextPageBtn');
const editModalEl = document.getElementById('editModal');
const editTitleEl = document.getElementById('editTitle');
const evalResultEl = document.getElementById('evalResult');

const STEP_LABEL = {
  3: '필수값',
  4: 'std_gbl_id',
  5: '요청/응답 구분',
  6: '거래시간 형식',
  8: '인증 사용자 정합',
  9: 'Catalog 조회',
  10: '사용상태',
  11: 'tr_sysid',
  12: '지점',
  13: '단말',
  14: 'sync_dsc',
  16: '거래가능시간',
  21: 'ALLOW'
};

let rowsCache = [];
let pageNo = 1;
let totalPages = 1;
let totalCount = 0;
let editMode = 'insert';

function text(v) {
  return v === null || v === undefined || v === '' ? '-' : String(v);
}
function escapeHtml(v) {
  return text(v).replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;');
}
function currentPageSize() {
  const size = parseInt(pageSizeEl.value, 10);
  return Number.isNaN(size) || size <= 0 ? 20 : Math.min(size, 100);
}
function valOrNull(el) {
  const v = el.value.trim();
  return v || null;
}
function numOrNull(el) {
  const v = el.value.trim();
  if (!v) return null;
  const n = Number(v);
  return Number.isNaN(n) ? null : n;
}
function hdr(rms) {
  return {
    hdr_nhnis: {
      sys_comm: {
        std_gbl_id: crypto.randomUUID().replaceAll('-', ''),
        rms_svc_c: rms,
        scid: 'mkcoa6666',
        optr_eno: 'LOCAL',
        tr_trm_ipadr: '127.0.0.1',
        tr_sysid: 'PDMK-UI',
        sync_dsc: 'S',
        std_tgrm_rqr_rsp_dsc: 'Q',
        std_tgrm_lclc: 'KO',
        tr_dtm: '20260808120000',
        tr_brc: '10001',
        trmno: 'LOCAL01',
        trm_kdc: '01'
      }
    }
  };
}
function extractDto(parsed) {
  return parsed && parsed.dto && typeof parsed.dto === 'object' ? parsed.dto : parsed;
}
function extractRows(parsed) {
  const dto = extractDto(parsed);
  if (!dto) return [];
  if (Array.isArray(dto.mkcoa6666S0DTOSub0)) return dto.mkcoa6666S0DTOSub0;
  return [];
}
function statusBadge(status) {
  const s = String(status || '').toUpperCase();
  if (s === 'STOP') return '<span class="badge stop">STOP</span>';
  if (s === 'MAINTENANCE' || s === 'MAINT') return '<span class="badge maint">MAINT</span>';
  return '<span class="badge normal">NORMAL</span>';
}
function resultBadge(r) {
  const s = String(r || '').toUpperCase();
  if (s === 'ALLOW') return '<span class="badge allow">ALLOW</span>';
  if (s === 'THROTTLE') return '<span class="badge throttle">THROTTLE</span>';
  if (s === 'REJECT') return '<span class="badge reject">REJECT</span>';
  return '<span class="badge block">BLOCK</span>';
}

async function relay(apiPath, body) {
  const query = new URLSearchParams({ baseUrl: targetBaseUrlEl.value.trim() });
  const response = await fetch(`${apiPath}?${query}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  return response.json();
}

function buildListBody() {
  return {
    ...hdr('mkcoa6666S0'),
    dto: {
      serviceCode: valOrNull(qServiceCodeEl),
      businessCode: valOrNull(qBusinessCodeEl),
      status: valOrNull(qStatusEl),
      enabled: valOrNull(qEnabledEl),
      pageNo,
      pageSize: currentPageSize()
    }
  };
}

function buildSaveBody() {
  return {
    ...hdr(editMode === 'update' ? 'mkcoa6666U0' : 'mkcoa6666I0'),
    dto: {
      serviceCode: document.getElementById('fServiceCode').value.trim(),
      serviceName: document.getElementById('fServiceName').value.trim() || null,
      businessCode: document.getElementById('fBusinessCode').value.trim() || 'mk',
      scid: document.getElementById('fScid').value.trim() || null,
      enabled: document.getElementById('fEnabled').value,
      status: document.getElementById('fStatus').value,
      allowedSystemIds: document.getElementById('fAllowedSystemIds').value.trim() || '*',
      allowedTerminalTypes: document.getElementById('fAllowedTerminalTypes').value.trim() || '*',
      allowedBranches: document.getElementById('fAllowedBranches').value.trim() || '*',
      requiredAuthorities: document.getElementById('fRequiredAuthorities').value.trim() || null,
      syncType: document.getElementById('fSyncType').value,
      allowedStartTime: document.getElementById('fAllowedStartTime').value.trim() || '0000',
      allowedEndTime: document.getElementById('fAllowedEndTime').value.trim() || '2400',
      timeoutMs: numOrNull(document.getElementById('fTimeoutMs')) ?? 3000,
      maxTps: numOrNull(document.getElementById('fMaxTps')),
      maxConcurrent: numOrNull(document.getElementById('fMaxConcurrent')),
      duplicateWindowSec: numOrNull(document.getElementById('fDupWin')) ?? 0,
      auditLevel: document.getElementById('fAuditLevel').value.trim() || 'NORMAL',
      reason: document.getElementById('fReason').value.trim() || null
    }
  };
}

function buildEvalBody() {
  return {
    ...hdr('mkcoa6666E0'),
    dto: {
      stdGblId: document.getElementById('eStdGblId').value.trim(),
      rmsSvcC: document.getElementById('eRmsSvcC').value.trim(),
      syncDsc: document.getElementById('eSyncDsc').value.trim(),
      trSysid: document.getElementById('eTrSysid').value.trim(),
      stdTgrmRqrRspDsc: document.getElementById('eRqrRsp').value.trim(),
      stdTgrmLclc: document.getElementById('eLclc').value.trim(),
      trTrmIpadr: document.getElementById('eIp').value.trim(),
      trDtm: document.getElementById('eTrDtm').value.trim(),
      trBrc: document.getElementById('eTrBrc').value.trim(),
      trmno: document.getElementById('eTrmno').value.trim(),
      trmKdc: document.getElementById('eTrmKdc').value.trim(),
      scid: document.getElementById('eScid').value.trim(),
      optrEno: document.getElementById('eOptrEno').value.trim(),
      authUserId: valOrNull(document.getElementById('eAuthUserId'))
    }
  };
}

function validateSaveClient() {
  const serviceCode = document.getElementById('fServiceCode').value.trim();
  if (!serviceCode) return 'serviceCode(rms_svc_c) 필수';
  const timeoutMs = numOrNull(document.getElementById('fTimeoutMs')) ?? 3000;
  if (timeoutMs <= 0) return 'timeoutMs 는 0보다 커야 함';
  const start = document.getElementById('fAllowedStartTime').value.trim() || '0000';
  const end = document.getElementById('fAllowedEndTime').value.trim() || '2400';
  const from = parseInt(start.replace(':', ''), 10);
  const to = end === '2400' ? 2400 : parseInt(end.replace(':', ''), 10);
  if (!Number.isNaN(from) && !Number.isNaN(to) && from > to) {
    return 'allowedStartTime 이 allowedEndTime 보다 큼';
  }
  const status = document.getElementById('fStatus').value;
  const enabled = document.getElementById('fEnabled').value;
  const reason = document.getElementById('fReason').value.trim();
  if ((status === 'STOP' || status === 'MAINTENANCE' || enabled === 'N') && !reason) {
    return 'STOP/MAINTENANCE/비활성 저장 시 reason 필수';
  }
  return null;
}

function updatePager() {
  pageInfoEl.textContent = `${pageNo} / ${Math.max(totalPages, 1)} · 전체 ${totalCount}건`;
  prevPageBtn.disabled = pageNo <= 1;
  nextPageBtn.disabled = pageNo >= totalPages || totalPages <= 0;
}

function renderRows(rows, paging) {
  rowsCache = rows || [];
  totalCount = paging && paging.totalCount != null ? Number(paging.totalCount) : rowsCache.length;
  totalPages = paging && paging.totalPages != null
      ? Number(paging.totalPages)
      : Math.ceil(totalCount / currentPageSize());
  if (paging && paging.pageNo != null) pageNo = Number(paging.pageNo);
  resultCountEl.textContent = `${rowsCache.length}건`;
  updatePager();
  if (!rowsCache.length) {
    resultBodyEl.innerHTML = '<tr><td colspan="13" class="empty">조회 결과가 없습니다.</td></tr>';
    return;
  }
  resultBodyEl.innerHTML = rowsCache.map((row, index) => `
    <tr>
      <td class="mono">${escapeHtml(row.serviceCode)}</td>
      <td>${escapeHtml(row.serviceName)}</td>
      <td>${escapeHtml(row.businessCode)}</td>
      <td>${statusBadge(row.status)}</td>
      <td>${escapeHtml(row.enabled)}</td>
      <td class="mono">${escapeHtml(row.allowedSystemIds)}</td>
      <td class="mono">${escapeHtml(row.allowedTerminalTypes)}</td>
      <td class="mono">${escapeHtml(row.allowedBranches)}</td>
      <td>${escapeHtml(row.syncType)}</td>
      <td class="mono">${escapeHtml(row.allowedStartTime)}~${escapeHtml(row.allowedEndTime)}</td>
      <td>${escapeHtml(row.timeoutMs)}</td>
      <td>${escapeHtml(row.maxTps)}</td>
      <td class="row-actions">
        <button class="btn-secondary btn-tiny" type="button" data-edit="${index}">수정</button>
        <button class="btn-secondary btn-tiny" type="button" data-status="STOP" data-idx="${index}">중지</button>
        <button class="btn-secondary btn-tiny" type="button" data-status="MAINTENANCE" data-idx="${index}">점검</button>
        <button class="btn-secondary btn-tiny" type="button" data-status="RESUME" data-idx="${index}">재개</button>
        <button class="btn-danger btn-tiny" type="button" data-del="${index}">삭제</button>
      </td>
    </tr>`).join('');
}

function renderSummary(dto) {
  document.getElementById('cardTotal').textContent = dto.totalCount ?? '-';
  document.getElementById('cardNormal').textContent = dto.normalCount ?? '-';
  document.getElementById('cardMaint').textContent = dto.maintenanceCount ?? '-';
  document.getElementById('cardStop').textContent = dto.stopCount ?? '-';
  document.getElementById('cardDisabled').textContent = dto.disabledCount ?? '-';
}

async function refreshSummary() {
  try {
    const result = await relay('/api/txcontrol/summary', { ...hdr('mkcoa6666S2'), dto: {} });
    const parsed = JSON.parse(result.responseBody);
    renderSummary(extractDto(parsed) || {});
  } catch (_e) {
    /* ignore summary failure */
  }
}

async function search() {
  resultMetaEl.innerHTML = '<span class="empty">조회 중...</span>';
  let result;
  try {
    result = await relay('/api/txcontrol/list', buildListBody());
  } catch (error) {
    PdmkErrorPopup.showSimple(error.message || String(error), '중계 오류');
    return;
  }
  const httpOk = result.httpStatus >= 200 && result.httpStatus < 300;
  let parsed = null;
  try { parsed = JSON.parse(result.responseBody); } catch (_e) { parsed = null; }
  const dto = extractDto(parsed) || {};
  const serviceError = PdmkErrorPopup.errorPayload(parsed);
  const ok = httpOk && !(parsed && parsed.error) && !serviceError;
  renderRows(extractRows(parsed), {
    pageNo: dto.pageNo, pageSize: dto.pageSize, totalCount: dto.totalCount, totalPages: dto.totalPages
  });
  resultMetaEl.innerHTML = `
    <span class="badge ${ok ? 'ok' : 'fail'}">HTTP ${result.httpStatus}</span>
    <span>${result.elapsedMs} ms</span>
    <span>Total: ${totalCount}</span>
    <span>${escapeHtml(result.targetUrl)}</span>`;
  if (!ok) {
    PdmkErrorPopup.showFromResponse(parsed, result.httpStatus, 'Catalog 조회 실패', result.responseBody);
  }
  await refreshSummary();
}

async function evaluate() {
  evalResultEl.className = 'sim-result';
  evalResultEl.textContent = '평가 중...';
  let result;
  try {
    result = await relay('/api/txcontrol/evaluate', buildEvalBody());
  } catch (error) {
    evalResultEl.textContent = error.message || String(error);
    return;
  }
  let parsed = null;
  try { parsed = JSON.parse(result.responseBody); } catch (_e) { parsed = null; }
  const dto = extractDto(parsed) || {};
  if (!(result.httpStatus >= 200 && result.httpStatus < 300) || (parsed && parsed.error)) {
    PdmkErrorPopup.showFromResponse(parsed, result.httpStatus, '평가 실패', result.responseBody);
    evalResultEl.innerHTML = `중계/서비스 오류 HTTP ${result.httpStatus}`;
    return;
  }
  const step = dto.checkStep;
  const stepLabel = STEP_LABEL[step] || '';
  evalResultEl.innerHTML = `
    ${resultBadge(dto.controlResult)}
    step=<code>${escapeHtml(step)}</code> ${escapeHtml(stepLabel)}
    ${dto.errorCode ? `· <code>${escapeHtml(dto.errorCode)}</code> ${escapeHtml(dto.errorName)}` : ''}
    <br>${escapeHtml(dto.message || '')}
    <br>service=<code>${escapeHtml(dto.serviceCode)}</code>
    timeoutMs=<code>${escapeHtml(dto.timeoutMs)}</code>
    maxTps=<code>${escapeHtml(dto.maxTps)}</code>
    maxConcurrent=<code>${escapeHtml(dto.maxConcurrent)}</code>
    dupWin=<code>${escapeHtml(dto.duplicateWindowSec)}</code>
  `;
}

function setEvalFields(map) {
  Object.entries(map).forEach(([id, value]) => {
    const el = document.getElementById(id);
    if (el) el.value = value;
  });
}

function applyPreset(name) {
  const base = {
    eStdGblId: '992674f81e9d4762b0d56a7fb38a1cc0',
    eRmsSvcC: 'mkcoa5530S0',
    eSyncDsc: 'S',
    eTrSysid: 'PDMK',
    eRqrRsp: 'Q',
    eLclc: 'KO',
    eIp: '127.0.0.1',
    eTrDtm: '20260808120000',
    eTrBrc: '10001',
    eTrmno: 'LOCAL01',
    eTrmKdc: '01',
    eScid: 'mkcoa5530',
    eOptrEno: 'E0000001',
    eAuthUserId: ''
  };
  const presets = {
    allow: base,
    unreg: { ...base, eRmsSvcC: 'unknown.svc.S0', eScid: 'unknown' },
    stop: { ...base, eRmsSvcC: 'mkcoa8888S0', eScid: 'mkcoa8888' },
    maint: { ...base, eRmsSvcC: 'demo.maint.S0', eScid: 'demo' },
    sys: { ...base, eTrSysid: 'OTHER' },
    brc: { ...base, eRmsSvcC: 'mkcoa9999S0', eScid: 'mkcoa9999', eTrBrc: '10002' },
    time: { ...base, eRmsSvcC: 'mkcoa9999S0', eScid: 'mkcoa9999', eTrDtm: '20260808080000' },
    rsp: { ...base, eRqrRsp: 'R' }
  };
  setEvalFields(presets[name] || base);
}

function activateTab(name) {
  document.querySelectorAll('#editTabs .tab-btn').forEach((btn) => {
    btn.classList.toggle('active', btn.dataset.tab === name);
  });
  document.querySelectorAll('.tab-panel').forEach((panel) => {
    panel.hidden = panel.dataset.panel !== name;
  });
}

function openInsert(seed) {
  editMode = 'insert';
  editTitleEl.textContent = 'Service Catalog 등록';
  const fServiceCode = document.getElementById('fServiceCode');
  fServiceCode.readOnly = false;
  fServiceCode.value = (seed && seed.serviceCode) || '';
  document.getElementById('fServiceName').value = (seed && seed.serviceName) || '';
  document.getElementById('fBusinessCode').value = (seed && seed.businessCode) || 'mk';
  document.getElementById('fScid').value = (seed && seed.scid) || '';
  document.getElementById('fEnabled').value = 'Y';
  document.getElementById('fStatus').value = 'NORMAL';
  document.getElementById('fAllowedSystemIds').value = 'PDMK,PDMK-UI';
  document.getElementById('fAllowedTerminalTypes').value = '*';
  document.getElementById('fAllowedBranches').value = '*';
  document.getElementById('fSyncType').value = 'S';
  document.getElementById('fAllowedStartTime').value = '0000';
  document.getElementById('fAllowedEndTime').value = '2400';
  document.getElementById('fTimeoutMs').value = '3000';
  document.getElementById('fMaxTps').value = '100';
  document.getElementById('fMaxConcurrent').value = '50';
  document.getElementById('fDupWin').value = '0';
  document.getElementById('fAuditLevel').value = 'NORMAL';
  document.getElementById('fReason').value = '';
  document.getElementById('fRequiredAuthorities').value = '';
  activateTab('basic');
  editModalEl.hidden = false;
}

function openEdit(row) {
  editMode = 'update';
  editTitleEl.textContent = 'Service Catalog 수정';
  const fServiceCode = document.getElementById('fServiceCode');
  fServiceCode.readOnly = true;
  fServiceCode.value = row.serviceCode || '';
  document.getElementById('fServiceName').value = row.serviceName || '';
  document.getElementById('fBusinessCode').value = row.businessCode || '';
  document.getElementById('fScid').value = row.scid || '';
  document.getElementById('fEnabled').value = row.enabled || 'Y';
  document.getElementById('fStatus').value = row.status || 'NORMAL';
  document.getElementById('fAllowedSystemIds').value = row.allowedSystemIds || '*';
  document.getElementById('fAllowedTerminalTypes').value = row.allowedTerminalTypes || '*';
  document.getElementById('fAllowedBranches').value = row.allowedBranches || '*';
  document.getElementById('fSyncType').value = row.syncType || 'S';
  document.getElementById('fAllowedStartTime').value = row.allowedStartTime || '0000';
  document.getElementById('fAllowedEndTime').value = row.allowedEndTime || '2400';
  document.getElementById('fTimeoutMs').value = row.timeoutMs != null ? row.timeoutMs : 3000;
  document.getElementById('fMaxTps').value = row.maxTps != null ? row.maxTps : '';
  document.getElementById('fMaxConcurrent').value = row.maxConcurrent != null ? row.maxConcurrent : '';
  document.getElementById('fDupWin').value = row.duplicateWindowSec != null ? row.duplicateWindowSec : 0;
  document.getElementById('fAuditLevel').value = row.auditLevel || 'NORMAL';
  document.getElementById('fReason').value = row.reason || '';
  document.getElementById('fRequiredAuthorities').value = row.requiredAuthorities || '';
  activateTab('basic');
  editModalEl.hidden = false;
}

function closeEdit() { editModalEl.hidden = true; }

async function save() {
  const clientError = validateSaveClient();
  if (clientError) {
    PdmkErrorPopup.showSimple(clientError, '저장 검증');
    return;
  }
  const api = editMode === 'update' ? '/api/txcontrol/update' : '/api/txcontrol/insert';
  let result;
  try {
    result = await relay(api, buildSaveBody());
  } catch (error) {
    PdmkErrorPopup.showSimple(error.message || String(error), '중계 오류');
    return;
  }
  let parsed = null;
  try { parsed = JSON.parse(result.responseBody); } catch (_e) { parsed = null; }
  const dto = extractDto(parsed) || {};
  const ok = result.httpStatus >= 200 && result.httpStatus < 300
      && !PdmkErrorPopup.errorPayload(parsed)
      && (dto.RSLT_CD == null || dto.RSLT_CD === '0000');
  if (!ok) {
    PdmkErrorPopup.showFromResponse(parsed, result.httpStatus, dto.RSLT_MSG || '저장 실패', result.responseBody);
    return;
  }
  closeEdit();
  alert(`저장 완료: ${dto.serviceCode || ''}`);
  await search();
}

async function changeStatus(row, action) {
  if (!row || !row.serviceCode) return;
  let status = 'NORMAL';
  let enabled = 'Y';
  let reason = row.reason || '';
  if (action === 'STOP') {
    status = 'STOP';
    enabled = 'N';
    reason = prompt('중지 사유 (필수)', reason || '운영 중지') || '';
    if (!reason.trim()) return;
  } else if (action === 'MAINTENANCE') {
    status = 'MAINTENANCE';
    enabled = 'Y';
    reason = prompt('점검 사유 (필수)', reason || '점검') || '';
    if (!reason.trim()) return;
  } else {
    reason = prompt('재개 사유', reason || '재개') || '재개';
  }
  let result;
  try {
    result = await relay('/api/txcontrol/status', {
      ...hdr('mkcoa6666U1'),
      dto: { serviceCode: row.serviceCode, status, enabled, reason }
    });
  } catch (error) {
    PdmkErrorPopup.showSimple(error.message || String(error), '중계 오류');
    return;
  }
  let parsed = null;
  try { parsed = JSON.parse(result.responseBody); } catch (_e) { parsed = null; }
  const dto = extractDto(parsed) || {};
  const ok = result.httpStatus >= 200 && result.httpStatus < 300
      && (dto.RSLT_CD == null || dto.RSLT_CD === '0000');
  if (!ok) {
    PdmkErrorPopup.showFromResponse(parsed, result.httpStatus, dto.RSLT_MSG || '상태 변경 실패', result.responseBody);
    return;
  }
  await search();
}

async function removeRow(row) {
  if (!row || !row.serviceCode) return;
  if (!confirm(`삭제할까요?\n${row.serviceCode}`)) return;
  let result;
  try {
    result = await relay('/api/txcontrol/delete', {
      ...hdr('mkcoa6666D0'),
      dto: { serviceCode: row.serviceCode }
    });
  } catch (error) {
    PdmkErrorPopup.showSimple(error.message || String(error), '중계 오류');
    return;
  }
  let parsed = null;
  try { parsed = JSON.parse(result.responseBody); } catch (_e) { parsed = null; }
  const dto = extractDto(parsed) || {};
  const ok = result.httpStatus >= 200 && result.httpStatus < 300
      && (dto.RSLT_CD == null || dto.RSLT_CD === '0000');
  if (!ok) {
    PdmkErrorPopup.showFromResponse(parsed, result.httpStatus, dto.RSLT_MSG || '삭제 실패', result.responseBody);
    return;
  }
  await search();
}

function resetFilters() {
  qServiceCodeEl.value = '';
  qBusinessCodeEl.value = '';
  qStatusEl.value = '';
  qEnabledEl.value = '';
  pageSizeEl.value = '20';
  pageNo = 1;
  search().catch((e) => PdmkErrorPopup.showSimple(e.message));
}

async function init() {
  const configRes = await fetch('/api/config');
  const config = await configRes.json();
  const configured = (config.targetBaseUrl || '').trim();
  targetBaseUrlEl.value = configured.includes(':8081') ? configured : 'http://localhost:8081';
  document.getElementById('targetInfo').textContent = `대상 pdmk-om: ${targetBaseUrlEl.value}`;

  document.getElementById('searchBtn').addEventListener('click', () => {
    pageNo = 1;
    search().catch((e) => PdmkErrorPopup.showSimple(e.message));
  });
  document.getElementById('resetBtn').addEventListener('click', resetFilters);
  document.getElementById('newBtn').addEventListener('click', () => openInsert());
  document.getElementById('saveBtn').addEventListener('click', () => save().catch((e) => PdmkErrorPopup.showSimple(e.message)));
  document.getElementById('evaluateBtn').addEventListener('click', () => evaluate().catch((e) => PdmkErrorPopup.showSimple(e.message)));
  document.getElementById('evalToNewBtn').addEventListener('click', () => {
    openInsert({
      serviceCode: document.getElementById('eRmsSvcC').value.trim(),
      scid: document.getElementById('eScid').value.trim(),
      businessCode: 'mk'
    });
  });
  document.getElementById('evalPresets').addEventListener('click', (event) => {
    const chip = event.target.closest('[data-preset]');
    if (!chip) return;
    applyPreset(chip.dataset.preset);
    evaluate().catch((e) => PdmkErrorPopup.showSimple(e.message));
  });
  document.getElementById('editTabs').addEventListener('click', (event) => {
    const btn = event.target.closest('[data-tab]');
    if (btn) activateTab(btn.dataset.tab);
  });

  prevPageBtn.addEventListener('click', () => {
    if (pageNo <= 1) return;
    pageNo -= 1;
    search().catch((e) => PdmkErrorPopup.showSimple(e.message));
  });
  nextPageBtn.addEventListener('click', () => {
    if (pageNo >= totalPages) return;
    pageNo += 1;
    search().catch((e) => PdmkErrorPopup.showSimple(e.message));
  });

  resultBodyEl.addEventListener('click', (event) => {
    const editBtn = event.target.closest('[data-edit]');
    if (editBtn) {
      const idx = Number(editBtn.dataset.edit);
      if (rowsCache[idx]) openEdit(rowsCache[idx]);
      return;
    }
    const statusBtn = event.target.closest('[data-status]');
    if (statusBtn) {
      const idx = Number(statusBtn.dataset.idx);
      if (rowsCache[idx]) {
        changeStatus(rowsCache[idx], statusBtn.dataset.status)
            .catch((e) => PdmkErrorPopup.showSimple(e.message));
      }
      return;
    }
    const delBtn = event.target.closest('[data-del]');
    if (delBtn) {
      const idx = Number(delBtn.dataset.del);
      if (rowsCache[idx]) removeRow(rowsCache[idx]).catch((e) => PdmkErrorPopup.showSimple(e.message));
    }
  });

  editModalEl.addEventListener('click', (event) => {
    if (event.target.dataset.close === 'true') closeEdit();
  });
  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && !editModalEl.hidden) closeEdit();
  });

  await search();
}

init().catch((error) => PdmkErrorPopup.showSimple('화면 초기화 실패: ' + error.message));
