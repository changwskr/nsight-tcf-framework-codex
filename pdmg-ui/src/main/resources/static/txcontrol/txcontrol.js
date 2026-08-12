/*
 * 거래통제 관리 화면 (mgcoa9001).
 * OM transaction-control.html UX + PDMG 전문 호출.
 * list   : POST /mgcoa9001S0
 * create : POST /mgcoa9001C0
 * update : POST /mgcoa9001U0
 * delete : POST /mgcoa9001D0
 */

const PAGE_SIZE_DEFAULT = 20;
const GLOBAL = 'GLOBAL';
const WILDCARD = '*';
const PILL_TYPES = ['BUSINESS', 'SERVICE', 'CHANNEL', 'BRANCH', 'USER', 'IP', 'GLOBAL'];
const TYPE_LABEL = {
  BUSINESS: '업무코드',
  SERVICE: '서비스ID',
  CHANNEL: '채널',
  BRANCH: '브랜치',
  USER: '사용자',
  IP: 'IP',
  GLOBAL: '전체'
};
const KEY_FIELDS = [
  'serviceId', 'transactionCode', 'businessCode', 'serviceName',
  'userId', 'channelId', 'branchId'
];

const TXC_PATH = {
  list: '/mgcoa9001S0',
  create: '/mgcoa9001C0',
  update: '/mgcoa9001U0',
  delete: '/mgcoa9001D0'
};

let serviceConfig = { timeoutMs: 10000 };
let pageNo = 1;
let totalPages = 1;
let totalCount = 0;
let listRows = [];
let selectedRow = null;
let editMode = false;
let selectedType = 'SERVICE';
let blockYn = 'Y';
let globalRow = null;

const targetBaseUrlEl = document.getElementById('targetBaseUrl');
const optrEnoEl = document.getElementById('optrEno');
const filterKwEl = document.getElementById('filterKw');
const filterControlTypeEl = document.getElementById('filterControlType');
const pageSizeEl = document.getElementById('pageSize');
const listStatusEl = document.getElementById('listStatus');
const resultCountEl = document.getElementById('resultCount');
const ctrlBodyEl = document.getElementById('ctrlBody');
const pageInfoEl = document.getElementById('pageInfo');
const prevPageBtn = document.getElementById('prevPageBtn');
const nextPageBtn = document.getElementById('nextPageBtn');
const targetInfoEl = document.getElementById('targetInfo');

function text(value) {
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  return String(value);
}

function escapeHtml(value) {
  return text(value)
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;');
}

function field(row, key, fallback) {
  if (!row || typeof row !== 'object') {
    return fallback === undefined ? '' : fallback;
  }
  const value = row[key];
  if (value === null || value === undefined || value === '') {
    return fallback === undefined ? '' : fallback;
  }
  return String(value);
}

function typeLabel(code) {
  return TYPE_LABEL[code] || code;
}

function currentPageSize() {
  const size = parseInt(pageSizeEl.value, 10);
  if (Number.isNaN(size) || size <= 0) {
    return PAGE_SIZE_DEFAULT;
  }
  return Math.min(size, 100);
}

function newGuid() {
  if (window.crypto && typeof window.crypto.randomUUID === 'function') {
    return window.crypto.randomUUID().replaceAll('-', '');
  }
  return 'xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx'.replace(/x/g, () =>
    ((Math.random() * 16) | 0).toString(16));
}

function buildHeader(serviceId) {
  return {
    sys_comm: {
      std_gbl_id: newGuid(),
      rms_svc_c: serviceId,
      scid: 'mgcoa9001',
      optr_eno: (optrEnoEl.value || 'sysadmin').trim() || 'sysadmin',
      tr_trm_ipadr: '127.0.0.1',
      tr_sysid: 'PDMG-UI',
      sync_dsc: 'S',
      std_tgrm_rqr_rsp_dsc: 'Q'
    }
  };
}

function extractDto(parsed) {
  return parsed && parsed.dto && typeof parsed.dto === 'object' ? parsed.dto : parsed;
}

function extractRows(parsed) {
  const dto = extractDto(parsed);
  if (!dto || typeof dto !== 'object') {
    return [];
  }
  if (Array.isArray(dto.rows)) {
    return dto.rows;
  }
  if (Array.isArray(dto.mgcoa9001S0DTOSub0)) {
    return dto.mgcoa9001S0DTOSub0;
  }
  return [];
}

function isGlobalRow(row) {
  return field(row, 'controlType') === GLOBAL && field(row, 'serviceId') === WILDCARD;
}

function isGlobalUnblock(row) {
  return isGlobalRow(row) && field(row, 'blockYn') === 'N';
}

function targetLabel(row) {
  if (field(row, 'controlType') === GLOBAL) {
    return '전체';
  }
  return field(row, 'targetValue', '-') || '-';
}

function statusHtml(row) {
  const ct = field(row, 'controlType');
  const yn = field(row, 'blockYn', 'N');
  if (ct === GLOBAL && yn === 'N') {
    return '<span class="txc-badge-global">전체 허용</span>';
  }
  return yn === 'Y'
      ? '<span class="txc-badge-block">차단</span>'
      : '<span class="txc-badge-allow">허용</span>';
}

function rowKey(row) {
  return KEY_FIELDS.map((k) => field(row, k)).join('|');
}

function formatRegDtm(value) {
  const raw = text(value);
  if (raw === '-' || raw.length < 14) {
    return raw;
  }
  return `${raw.slice(0, 4)}-${raw.slice(4, 6)}-${raw.slice(6, 8)} `
      + `${raw.slice(8, 10)}:${raw.slice(10, 12)}:${raw.slice(12, 14)}`;
}

function updateTargetInfo() {
  if (targetInfoEl) {
    targetInfoEl.textContent = (targetBaseUrlEl.value || '').replace(/\/$/, '') || 'URL 미설정';
  }
}

function syncBlockToggle() {
  document.getElementById('blockY').className = blockYn === 'Y' ? 'active-y' : '';
  document.getElementById('blockN').className = blockYn === 'N' ? 'active-n' : '';
}

function syncTargetField() {
  const wrap = document.getElementById('targetWrap');
  const input = document.getElementById('fTargetInput');
  const isGlobal = selectedType === GLOBAL;
  wrap.hidden = isGlobal;
  if (isGlobal) {
    return;
  }
  input.placeholder = selectedType === 'IP'
      ? '10.10.10.10'
      : selectedType === 'USER'
          ? 'E0000001'
          : selectedType === 'BUSINESS'
              ? 'MG'
              : selectedType === 'CHANNEL'
                  ? 'WEBTOP'
                  : selectedType === 'BRANCH'
                      ? '10001'
                      : 'mgcoa5530S0';
}

function renderPills() {
  document.getElementById('typePills').innerHTML = PILL_TYPES.map((t) =>
      '<button type="button" class="txc-pill' + (t === selectedType ? ' active' : '')
      + '" data-type="' + t + '"' + (editMode ? ' disabled' : '') + '>'
      + typeLabel(t) + '</button>'
  ).join('');
  document.querySelectorAll('.txc-pill').forEach((btn) => {
    btn.addEventListener('click', () => {
      if (editMode) {
        return;
      }
      selectedType = btn.dataset.type;
      renderPills();
      syncTargetField();
    });
  });
}

function readTargetValue() {
  if (selectedType === GLOBAL) {
    return WILDCARD;
  }
  return document.getElementById('fTargetInput').value.trim();
}

function clearForm() {
  selectedRow = null;
  editMode = false;
  selectedType = 'SERVICE';
  blockYn = 'Y';
  document.getElementById('formTitle').textContent = '규칙 등록';
  document.getElementById('fReason').value = '';
  document.getElementById('fTargetInput').value = '';
  document.getElementById('deleteBtn').hidden = true;
  ctrlBodyEl.querySelectorAll('tr.om-row-selected').forEach((r) => r.classList.remove('om-row-selected'));
  renderPills();
  syncTargetField();
  syncBlockToggle();
}

function fillForm(row) {
  selectedRow = row;
  editMode = true;
  document.getElementById('formTitle').textContent = '규칙 수정';
  selectedType = field(row, 'controlType', 'SERVICE');
  blockYn = field(row, 'blockYn', 'Y');
  document.getElementById('fTargetInput').value = field(row, 'targetValue', '');
  document.getElementById('fReason').value = '';
  document.getElementById('deleteBtn').hidden = false;
  renderPills();
  syncTargetField();
  syncBlockToggle();
}

function formPayload() {
  const payload = {
    controlType: selectedType,
    targetValue: readTargetValue(),
    blockYn: blockYn,
    changeReason: document.getElementById('fReason').value.trim()
  };
  if (editMode && selectedRow) {
    KEY_FIELDS.forEach((k) => {
      payload[k] = field(selectedRow, k, '');
    });
    payload.chgUserId = (optrEnoEl.value || 'sysadmin').trim() || 'sysadmin';
  } else {
    payload.regUserId = (optrEnoEl.value || 'sysadmin').trim() || 'sysadmin';
  }
  return payload;
}

function updateGlobalBar() {
  const active = globalRow && isGlobalUnblock(globalRow);
  const top = document.getElementById('globalBar');
  top.className = 'txc-top ' + (active ? 'on' : 'off');
  document.getElementById('globalDot').className = 'txc-dot ' + (active ? 'on' : 'off');
  document.getElementById('globalLabel').textContent = active
      ? '전체 허용 · 켜짐'
      : '전체 허용 · 꺼짐';
  document.getElementById('enableGlobalBtn').disabled = !!active;
  document.getElementById('disableGlobalBtn').disabled = !active;
}

function updatePager() {
  pageInfoEl.textContent = `${pageNo} / ${Math.max(totalPages, 1)} · 전체 ${totalCount}건`;
  prevPageBtn.disabled = pageNo <= 1;
  nextPageBtn.disabled = pageNo >= totalPages || totalPages <= 0;
}

function parseResult(result) {
  try {
    return JSON.parse(result.responseBody);
  } catch (_e) {
    return null;
  }
}

function isOk(result, parsed) {
  const httpOk = result.httpStatus >= 200 && result.httpStatus < 300;
  const serviceError = PdmgErrorPopup.errorPayload(parsed);
  const dto = extractDto(parsed) || {};
  return httpOk && !(parsed && parsed.error) && !serviceError
      && (dto.RSLT_CD == null || dto.RSLT_CD === '0000');
}

async function relay(action, body) {
  const servicePath = TXC_PATH[action];
  if (!servicePath) {
    throw new Error('unknown txcontrol action: ' + action);
  }
  return PdmgServiceClient.postPath(
      targetBaseUrlEl.value,
      servicePath,
      body,
      serviceConfig.timeoutMs,
      servicePath.replace('/', ''));
}

async function refreshGlobalRow() {
  const result = await relay('list', {
    hdr_nhnis: buildHeader('mgcoa9001S0'),
    dto: { pageNo: 1, pageSize: 10, controlType: GLOBAL }
  });
  const parsed = parseResult(result);
  if (!isOk(result, parsed)) {
    globalRow = null;
    updateGlobalBar();
    return;
  }
  const rows = extractRows(parsed);
  globalRow = rows.find((r) => isGlobalRow(r) && isGlobalUnblock(r)) || null;
  updateGlobalBar();
}

async function enableGlobalUnblock() {
  if (!confirm('전체 거래 허용을 켜까요?')) {
    return;
  }
  const payload = {
    controlType: GLOBAL,
    targetValue: WILDCARD,
    blockYn: 'N',
    changeReason: '전체 거래 허용 활성화',
    regUserId: (optrEnoEl.value || 'sysadmin').trim() || 'sysadmin'
  };
  try {
    const inquiry = await relay('list', {
      hdr_nhnis: buildHeader('mgcoa9001S0'),
      dto: { pageNo: 1, pageSize: 10, controlType: GLOBAL }
    });
    const parsed = parseResult(inquiry);
    const existing = extractRows(parsed).find(isGlobalRow);
    let result;
    if (existing) {
      KEY_FIELDS.forEach((k) => {
        payload[k] = field(existing, k, WILDCARD);
      });
      payload.chgUserId = payload.regUserId;
      delete payload.regUserId;
      result = await relay('update', {
        hdr_nhnis: buildHeader('mgcoa9001U0'),
        dto: payload
      });
    } else {
      result = await relay('create', {
        hdr_nhnis: buildHeader('mgcoa9001C0'),
        dto: payload
      });
    }
    const out = parseResult(result);
    if (!isOk(result, out)) {
      PdmgErrorPopup.showFromResponse(out, result.httpStatus, '전체 허용 활성화 실패', result.responseBody);
      return;
    }
    await refreshGlobalRow();
    alert('전체 거래 허용이 켜졌습니다.');
    await loadList(1);
  } catch (e) {
    PdmgErrorPopup.showSimple(e.message || String(e), '호출 오류');
  }
}

async function disableGlobalUnblock() {
  try {
    const inquiry = await relay('list', {
      hdr_nhnis: buildHeader('mgcoa9001S0'),
      dto: { pageNo: 1, pageSize: 10, controlType: GLOBAL }
    });
    const parsed = parseResult(inquiry);
    const existing = extractRows(parsed).find((r) => isGlobalRow(r) && isGlobalUnblock(r));
    if (!existing) {
      alert('전체 허용 규칙이 없습니다.');
      return;
    }
    if (!confirm('전체 거래 허용을 끌까요?')) {
      return;
    }
    const payload = {
      controlType: GLOBAL,
      targetValue: WILDCARD,
      blockYn: 'N',
      changeReason: '전체 거래 허용 비활성화',
      serviceId: WILDCARD,
      transactionCode: WILDCARD,
      businessCode: WILDCARD,
      serviceName: WILDCARD,
      userId: WILDCARD,
      channelId: WILDCARD,
      branchId: WILDCARD
    };
    KEY_FIELDS.forEach((k) => {
      payload[k] = field(existing, k, WILDCARD);
    });
    const result = await relay('delete', {
      hdr_nhnis: buildHeader('mgcoa9001D0'),
      dto: payload
    });
    const out = parseResult(result);
    if (!isOk(result, out)) {
      PdmgErrorPopup.showFromResponse(out, result.httpStatus, '전체 허용 비활성화 실패', result.responseBody);
      return;
    }
    globalRow = null;
    await refreshGlobalRow();
    alert('전체 거래 허용이 꺼졌습니다.');
    clearForm();
    await loadList(1);
  } catch (e) {
    PdmgErrorPopup.showSimple(e.message || String(e), '호출 오류');
  }
}

function renderRows(rows, paging) {
  listRows = Array.isArray(rows) ? rows : [];
  if (paging) {
    pageNo = paging.pageNo || pageNo;
    totalCount = paging.totalCount != null ? Number(paging.totalCount) : listRows.length;
    totalPages = paging.totalPages != null
        ? Number(paging.totalPages)
        : Math.max(1, Math.ceil(totalCount / currentPageSize()));
  }
  resultCountEl.textContent = `${totalCount}건`;
  updatePager();

  if (!listRows.length) {
    ctrlBodyEl.innerHTML = '<tr><td colspan="5" class="empty">등록된 규칙이 없습니다.</td></tr>';
    return;
  }

  ctrlBodyEl.innerHTML = listRows.map((r, index) => `
    <tr class="om-row-selectable" data-index="${index}" data-key="${encodeURIComponent(rowKey(r))}">
      <td>${escapeHtml(typeLabel(field(r, 'controlType')))}</td>
      <td class="txc-mono">${escapeHtml(targetLabel(r))}</td>
      <td>${statusHtml(r)}</td>
      <td>${escapeHtml(field(r, 'changeReason', '-'))}</td>
      <td class="txc-mono">${escapeHtml(formatRegDtm(field(r, 'regDtm')))}</td>
    </tr>
  `).join('');

  ctrlBodyEl.querySelectorAll('tr.om-row-selectable').forEach((tr) => {
    tr.addEventListener('click', () => {
      ctrlBodyEl.querySelectorAll('tr.om-row-selected').forEach((r) => r.classList.remove('om-row-selected'));
      tr.classList.add('om-row-selected');
      const idx = Number(tr.dataset.index);
      fillForm(listRows[idx] || {});
    });
  });
}

async function loadList(page) {
  pageNo = page || 1;
  updateTargetInfo();
  listStatusEl.textContent = '조회 중...';
  ctrlBodyEl.innerHTML = '<tr><td colspan="5" class="empty">조회 중...</td></tr>';

  const dto = {
    pageNo,
    pageSize: currentPageSize()
  };
  if (filterControlTypeEl.value.trim()) {
    dto.controlType = filterControlTypeEl.value.trim();
  }
  if (filterKwEl.value.trim()) {
    dto.targetValue = filterKwEl.value.trim();
  }

  let result;
  try {
    result = await relay('list', {
      hdr_nhnis: buildHeader('mgcoa9001S0'),
      dto
    });
  } catch (error) {
    listStatusEl.textContent = '호출 실패';
    ctrlBodyEl.innerHTML = '<tr><td colspan="5" class="empty">조회 실패</td></tr>';
    PdmgErrorPopup.showSimple(error.message || String(error), '호출 오류');
    return;
  }

  const parsed = parseResult(result);
  const dtoOut = extractDto(parsed) || {};
  const rows = extractRows(parsed);
  const ok = isOk(result, parsed);

  renderRows(rows, {
    pageNo: dtoOut.pageNo,
    pageSize: dtoOut.pageSize,
    totalCount: dtoOut.totalCount,
    totalPages: dtoOut.totalPages
  });

  listStatusEl.textContent = `${result.elapsedMs} ms · HTTP ${result.httpStatus} · ${escapeHtml(result.targetUrl)}`;
  try {
    await refreshGlobalRow();
  } catch (_e) {
    /* ignore global bar errors after list */
  }

  if (!ok) {
    PdmgErrorPopup.showFromResponse(
        parsed,
        result.httpStatus,
        '거래통제 조회에 실패했습니다.',
        result.responseBody);
  }
}

async function saveRule() {
  if (selectedType !== GLOBAL && !readTargetValue()) {
    alert('통제 대상값을 입력하세요.');
    return;
  }
  const reason = document.getElementById('fReason').value.trim();
  if (reason.length < 5) {
    alert('변경 사유는 5자 이상 입력하세요.');
    return;
  }

  const isUpdate = editMode;
  const body = {
    hdr_nhnis: buildHeader(isUpdate ? 'mgcoa9001U0' : 'mgcoa9001C0'),
    dto: formPayload()
  };

  let result;
  try {
    result = await relay(isUpdate ? 'update' : 'create', body);
  } catch (error) {
    PdmgErrorPopup.showSimple(error.message || String(error), '호출 오류');
    return;
  }

  const parsed = parseResult(result);
  if (!isOk(result, parsed)) {
    PdmgErrorPopup.showFromResponse(
        parsed,
        result.httpStatus,
        isUpdate ? '수정 실패' : '등록 실패',
        result.responseBody);
    return;
  }
  alert('저장 완료');
  clearForm();
  await loadList(pageNo);
}

async function deleteRule() {
  if (!editMode || !selectedRow) {
    alert('목록에서 규칙을 선택하세요.');
    return;
  }
  if (!confirm('선택 규칙을 삭제할까요?')) {
    return;
  }
  const reason = document.getElementById('fReason').value.trim();
  if (reason.length < 5) {
    alert('변경 사유는 5자 이상 입력하세요.');
    return;
  }

  let result;
  try {
    result = await relay('delete', {
      hdr_nhnis: buildHeader('mgcoa9001D0'),
      dto: formPayload()
    });
  } catch (error) {
    PdmgErrorPopup.showSimple(error.message || String(error), '호출 오류');
    return;
  }

  const parsed = parseResult(result);
  if (!isOk(result, parsed)) {
    PdmgErrorPopup.showFromResponse(parsed, result.httpStatus, '삭제 실패', result.responseBody);
    return;
  }
  alert('삭제 완료');
  clearForm();
  await loadList(pageNo);
}

function initFilterTypes() {
  PILL_TYPES.forEach((t) => {
    const opt = document.createElement('option');
    opt.value = t;
    opt.textContent = typeLabel(t);
    filterControlTypeEl.appendChild(opt);
  });
}

function bindEvents() {
  document.getElementById('blockY').addEventListener('click', () => {
    blockYn = 'Y';
    syncBlockToggle();
  });
  document.getElementById('blockN').addEventListener('click', () => {
    blockYn = 'N';
    syncBlockToggle();
  });
  document.getElementById('enableGlobalBtn').addEventListener('click', enableGlobalUnblock);
  document.getElementById('disableGlobalBtn').addEventListener('click', disableGlobalUnblock);
  document.getElementById('searchBtn').addEventListener('click', () => loadList(1));
  document.getElementById('filterKw').addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      loadList(1);
    }
  });
  document.getElementById('cancelBtn').addEventListener('click', clearForm);
  document.getElementById('saveBtn').addEventListener('click', saveRule);
  document.getElementById('deleteBtn').addEventListener('click', deleteRule);
  prevPageBtn.addEventListener('click', () => {
    if (pageNo > 1) {
      loadList(pageNo - 1);
    }
  });
  nextPageBtn.addEventListener('click', () => {
    if (pageNo < totalPages) {
      loadList(pageNo + 1);
    }
  });
  targetBaseUrlEl.addEventListener('change', updateTargetInfo);
  targetBaseUrlEl.addEventListener('blur', updateTargetInfo);
}

initFilterTypes();
renderPills();
syncTargetField();
syncBlockToggle();
bindEvents();
updateTargetInfo();
updateGlobalBar();
loadList(1);
