/*
 * 거래 파라미터 관리 화면 (mgcoa9000).
 * 브라우저 → pdmg-service 직접 호출
 * list   : POST /mgcoa9000S0
 * create : POST /mgcoa9000C0
 * update : POST /mgcoa9000U0
 * delete : POST /mgcoa9000D0
 */

let serviceConfig = { timeoutMs: 10000 };

const TXPARAM_PATH = {
  list: '/mgcoa9000S0',
  create: '/mgcoa9000C0',
  update: '/mgcoa9000U0',
  delete: '/mgcoa9000D0'
};

const targetBaseUrlEl = document.getElementById('targetBaseUrl');
const keywordEl = document.getElementById('keyword');
const pageSizeEl = document.getElementById('pageSize');
const resultMetaEl = document.getElementById('resultMeta');
const resultCountEl = document.getElementById('resultCount');
const resultBodyEl = document.getElementById('resultBody');
const pageInfoEl = document.getElementById('pageInfo');
const prevPageBtn = document.getElementById('prevPageBtn');
const nextPageBtn = document.getElementById('nextPageBtn');
const editModalEl = document.getElementById('editModal');
const editTitleEl = document.getElementById('editTitle');
const formTxNameEl = document.getElementById('formTxName');
const formTxIdEl = document.getElementById('formTxId');
const formAppIdEl = document.getElementById('formAppId');
const formPathUrlEl = document.getElementById('formPathUrl');
const formHttpMethodEl = document.getElementById('formHttpMethod');
const formUserIdEl = document.getElementById('formUserId');

let rowsCache = [];
let pageNo = 1;
let totalPages = 1;
let totalCount = 0;
let editMode = 'create';

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

function currentPageSize() {
  const size = parseInt(pageSizeEl.value, 10);
  if (Number.isNaN(size) || size <= 0) {
    return 10;
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

function formatRegDtm(value) {
  const raw = text(value);
  if (raw === '-' || raw.length < 14) {
    return raw;
  }
  return `${raw.slice(0, 4)}-${raw.slice(4, 6)}-${raw.slice(6, 8)} `
      + `${raw.slice(8, 10)}:${raw.slice(10, 12)}:${raw.slice(12, 14)}`;
}

function buildHeader(serviceId) {
  return {
    sys_comm: {
      std_gbl_id: newGuid(),
      rms_svc_c: serviceId,
      scid: 'mgcoa9000',
      optr_eno: (formUserIdEl.value || 'sysadmin').trim() || 'sysadmin',
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
  if (Array.isArray(dto.mgcoa9000S0DTOSub0)) {
    return dto.mgcoa9000S0DTOSub0;
  }
  if (Array.isArray(dto.records)) {
    return dto.records;
  }
  return [];
}

function updatePager() {
  pageInfoEl.textContent = `${pageNo} / ${Math.max(totalPages, 1)} · 전체 ${totalCount}건`;
  prevPageBtn.disabled = pageNo <= 1;
  nextPageBtn.disabled = pageNo >= totalPages || totalPages <= 0;
}

function closeOpenMenus() {
  resultBodyEl.querySelectorAll('.row-menu.open').forEach((menu) => {
    menu.classList.remove('open');
  });
}

function renderRows(rows, paging) {
  rowsCache = Array.isArray(rows) ? rows : [];
  if (paging) {
    pageNo = paging.pageNo || pageNo;
    totalCount = paging.totalCount != null ? Number(paging.totalCount) : rowsCache.length;
    totalPages = paging.totalPages != null
        ? Number(paging.totalPages)
        : Math.max(1, Math.ceil(totalCount / currentPageSize()));
  }
  resultCountEl.textContent = `${totalCount}건`;
  updatePager();

  if (!rowsCache.length) {
    resultBodyEl.innerHTML = '<tr><td colspan="8" class="empty">데이터 없음</td></tr>';
    return;
  }

  resultBodyEl.innerHTML = rowsCache.map((row, index) => `
    <tr data-index="${index}">
      <td>${escapeHtml(row.txName)}</td>
      <td class="mono">${escapeHtml(row.txId)}</td>
      <td>${escapeHtml(row.appId)}</td>
      <td class="mono">${escapeHtml(row.pathUrl)}</td>
      <td><span class="badge">${escapeHtml(row.httpMethod)}</span></td>
      <td>${escapeHtml(row.regUserId)}</td>
      <td class="mono">${escapeHtml(formatRegDtm(row.regDtm))}</td>
      <td class="col-actions">
        <div class="row-menu">
          <button class="btn-icon menu-toggle" type="button" title="메뉴" aria-label="행 메뉴">⋮</button>
          <div class="row-menu-panel">
            <button type="button" data-action="edit">수정</button>
            <button type="button" data-action="delete" class="danger">삭제</button>
          </div>
        </div>
      </td>
    </tr>
  `).join('');
}

function openCreateModal() {
  editMode = 'create';
  editTitleEl.textContent = '거래 파라미터 추가';
  formTxIdEl.disabled = false;
  formTxNameEl.value = '';
  formTxIdEl.value = '';
  formAppIdEl.value = 'pdmg-service';
  formPathUrlEl.value = '';
  formHttpMethodEl.value = 'POST';
  formUserIdEl.value = 'sysadmin';
  editModalEl.hidden = false;
}

function openEditModal(row) {
  editMode = 'update';
  editTitleEl.textContent = '거래 파라미터 수정';
  formTxIdEl.disabled = true;
  formTxNameEl.value = row.txName || '';
  formTxIdEl.value = row.txId || '';
  formAppIdEl.value = row.appId || '';
  formPathUrlEl.value = row.pathUrl || '';
  formHttpMethodEl.value = (row.httpMethod || 'POST').toUpperCase();
  formUserIdEl.value = row.regUserId || 'sysadmin';
  editModalEl.hidden = false;
}

function closeEditModal() {
  editModalEl.hidden = true;
}

async function relay(path, body) {
  const servicePath = TXPARAM_PATH[path];
  if (!servicePath) {
    throw new Error('unknown txparam action: ' + path);
  }
  return PdmgServiceClient.postPath(
      targetBaseUrlEl.value,
      servicePath,
      body,
      serviceConfig.timeoutMs,
      servicePath.replace('/', ''));
}

function parseResult(result) {
  let parsed = null;
  try {
    parsed = JSON.parse(result.responseBody);
  } catch (_e) {
    parsed = null;
  }
  return parsed;
}

function isOk(result, parsed) {
  const httpOk = result.httpStatus >= 200 && result.httpStatus < 300;
  const serviceError = PdmgErrorPopup.errorPayload(parsed);
  const dto = extractDto(parsed) || {};
  return httpOk && !(parsed && parsed.error) && !serviceError
      && (dto.RSLT_CD == null || dto.RSLT_CD === '0000');
}

async function search() {
  resultMetaEl.innerHTML = '<span class="empty">조회 중...</span>';
  resultBodyEl.innerHTML = '<tr><td colspan="8" class="empty">조회 중...</td></tr>';

  let result;
  try {
    result = await relay('list', {
      hdr_nhnis: buildHeader('mgcoa9000S0'),
      dto: {
        keyword: keywordEl.value.trim() || null,
        pageNo,
        pageSize: currentPageSize()
      }
    });
  } catch (error) {
    resultMetaEl.innerHTML = '<span class="badge fail">호출 실패</span>';
    resultBodyEl.innerHTML = '<tr><td colspan="8" class="empty">조회 실패</td></tr>';
    PdmgErrorPopup.showSimple(error.message || String(error), '호출 오류');
    return;
  }

  const parsed = parseResult(result);
  const dto = extractDto(parsed) || {};
  const rows = extractRows(parsed);
  const ok = isOk(result, parsed) || (result.httpStatus >= 200 && result.httpStatus < 300
      && !(parsed && parsed.error) && !PdmgErrorPopup.errorPayload(parsed));

  renderRows(rows, {
    pageNo: dto.pageNo,
    pageSize: dto.pageSize,
    totalCount: dto.totalCount,
    totalPages: dto.totalPages
  });

  resultMetaEl.innerHTML = `
    <span class="badge ${ok ? 'ok' : 'fail'}">HTTP ${result.httpStatus}</span>
    <span>${result.elapsedMs} ms</span>
    <span>Total: ${totalCount} · page ${pageNo}/${Math.max(totalPages, 1)}</span>
    <span>${escapeHtml(result.targetUrl)}</span>
  `;

  if (!ok) {
    PdmgErrorPopup.showFromResponse(
        parsed,
        result.httpStatus,
        '거래 파라미터 조회에 실패했습니다.',
        result.responseBody);
  }
}

async function saveForm() {
  const txId = formTxIdEl.value.trim();
  const txName = formTxNameEl.value.trim();
  if (!txId || !txName) {
    PdmgErrorPopup.showSimple('거래 ID와 거래 명은 필수입니다.', '입력 오류');
    return;
  }

  const isCreate = editMode === 'create';
  const body = {
    hdr_nhnis: buildHeader(isCreate ? 'mgcoa9000C0' : 'mgcoa9000U0'),
    dto: {
      txId,
      txName,
      appId: formAppIdEl.value.trim(),
      pathUrl: formPathUrlEl.value.trim(),
      httpMethod: formHttpMethodEl.value,
      ...(isCreate
          ? { regUserId: formUserIdEl.value.trim() || 'sysadmin' }
          : { chgUserId: formUserIdEl.value.trim() || 'sysadmin' })
    }
  };

  let result;
  try {
    result = await relay(isCreate ? 'create' : 'update', body);
  } catch (error) {
    PdmgErrorPopup.showSimple(error.message || String(error), '중계 오류');
    return;
  }

  const parsed = parseResult(result);
  const dto = extractDto(parsed) || {};
  if (!isOk(result, parsed)) {
    if (!PdmgErrorPopup.showFromResponse(
        parsed,
        result.httpStatus,
        dto.RSLT_MSG || (isCreate ? '등록 실패' : '수정 실패'),
        result.responseBody)) {
      PdmgErrorPopup.showSimple(dto.RSLT_MSG || '처리 실패', isCreate ? '등록 실패' : '수정 실패');
    }
    return;
  }

  closeEditModal();
  await search();
}

async function deleteRow(row) {
  if (!row || !row.txId) {
    return;
  }
  if (!confirm(`거래 [${row.txId}] 를 삭제할까요?`)) {
    return;
  }

  let result;
  try {
    result = await relay('delete', {
      hdr_nhnis: buildHeader('mgcoa9000D0'),
      dto: { txIdList: [row.txId] }
    });
  } catch (error) {
    PdmgErrorPopup.showSimple(error.message || String(error), '중계 오류');
    return;
  }

  const parsed = parseResult(result);
  const dto = extractDto(parsed) || {};
  if (!isOk(result, parsed)) {
    if (!PdmgErrorPopup.showFromResponse(
        parsed,
        result.httpStatus,
        dto.RSLT_MSG || '삭제 실패',
        result.responseBody)) {
      PdmgErrorPopup.showSimple(dto.RSLT_MSG || '삭제 실패', '삭제 실패');
    }
    return;
  }
  await search();
}

async function init() {
  serviceConfig = await fetch('/api/config').then((r) => r.json());
  targetBaseUrlEl.value = serviceConfig.targetBaseUrl || targetBaseUrlEl.value || 'http://localhost:8080';
  document.getElementById('targetInfo').textContent = targetBaseUrlEl.value;

  document.getElementById('searchBtn').addEventListener('click', () => {
    pageNo = 1;
    search().catch((error) => PdmgErrorPopup.showSimple('조회 실패: ' + error.message));
  });
  document.getElementById('refreshBtn').addEventListener('click', () => {
    search().catch((error) => PdmgErrorPopup.showSimple('조회 실패: ' + error.message));
  });
  document.getElementById('addBtn').addEventListener('click', openCreateModal);
  document.getElementById('saveBtn').addEventListener('click', () => {
    saveForm().catch((error) => PdmgErrorPopup.showSimple('저장 실패: ' + error.message));
  });
  keywordEl.addEventListener('keydown', (event) => {
    if (event.key === 'Enter') {
      pageNo = 1;
      search().catch((error) => PdmgErrorPopup.showSimple('조회 실패: ' + error.message));
    }
  });
  pageSizeEl.addEventListener('change', () => {
    pageNo = 1;
    search().catch((error) => PdmgErrorPopup.showSimple('조회 실패: ' + error.message));
  });

  prevPageBtn.addEventListener('click', () => {
    if (pageNo <= 1) {
      return;
    }
    pageNo -= 1;
    search().catch((error) => PdmgErrorPopup.showSimple('조회 실패: ' + error.message));
  });
  nextPageBtn.addEventListener('click', () => {
    if (pageNo >= totalPages) {
      return;
    }
    pageNo += 1;
    search().catch((error) => PdmgErrorPopup.showSimple('조회 실패: ' + error.message));
  });

  resultBodyEl.addEventListener('click', (event) => {
    const toggle = event.target.closest('.menu-toggle');
    if (toggle) {
      const menu = toggle.closest('.row-menu');
      const opened = menu.classList.contains('open');
      closeOpenMenus();
      if (!opened) {
        menu.classList.add('open');
      }
      return;
    }

    const actionBtn = event.target.closest('[data-action]');
    if (!actionBtn) {
      return;
    }
    const tr = actionBtn.closest('tr[data-index]');
    if (!tr) {
      return;
    }
    const row = rowsCache[Number(tr.dataset.index)];
    closeOpenMenus();
    if (actionBtn.dataset.action === 'edit') {
      openEditModal(row);
    } else if (actionBtn.dataset.action === 'delete') {
      deleteRow(row).catch((error) => PdmgErrorPopup.showSimple('삭제 실패: ' + error.message));
    }
  });

  editModalEl.addEventListener('click', (event) => {
    if (event.target && event.target.getAttribute('data-close') === 'true') {
      closeEditModal();
    }
  });
  document.addEventListener('click', (event) => {
    if (!event.target.closest('.row-menu')) {
      closeOpenMenus();
    }
  });

  await search();
}

init().catch((error) => PdmgErrorPopup.showSimple('화면 초기화 실패: ' + error.message));
