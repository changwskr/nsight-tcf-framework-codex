/*
 * 이미지로그 관리 화면.
 * 브라우저 → pdmg-service 직접 호출
 * list  : POST /mgcoa8888S0
 * delete: POST /mgcoa8888D0
 */

let serviceConfig = { timeoutMs: 10000 };

const targetBaseUrlEl = document.getElementById('targetBaseUrl');
const guidEl = document.getElementById('guid');
const serviceIdEl = document.getElementById('serviceId');
const screenIdEl = document.getElementById('screenId');
const optrEnoEl = document.getElementById('optrEno');
const exceptionOnlyEl = document.getElementById('exceptionOnly');
const withinSecondsEl = document.getElementById('withinSeconds');
const withinHintEl = document.getElementById('withinHint');
const withinSecondsGroup = document.getElementById('withinSecondsGroup');
const minElapsedSecondsEl = document.getElementById('minElapsedSeconds');
const elapsedHintEl = document.getElementById('elapsedHint');
const minElapsedSecondsGroup = document.getElementById('minElapsedSecondsGroup');
const pageSizeEl = document.getElementById('pageSize');
const resultMetaEl = document.getElementById('resultMeta');
const resultCountEl = document.getElementById('resultCount');
const resultBodyEl = document.getElementById('resultBody');
const detailModalEl = document.getElementById('detailModal');
const detailBodyEl = document.getElementById('detailBody');
const checkAllEl = document.getElementById('checkAll');
const pageInfoEl = document.getElementById('pageInfo');
const prevPageBtn = document.getElementById('prevPageBtn');
const nextPageBtn = document.getElementById('nextPageBtn');

let rowsCache = [];
let pageNo = 1;
let totalPages = 1;
let totalCount = 0;

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
    return 20;
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

function currentWithinSeconds() {
  const raw = withinSecondsEl.value.trim();
  if (!raw) {
    return null;
  }
  const n = parseInt(raw, 10);
  if (Number.isNaN(n) || n <= 0) {
    return null;
  }
  return n;
}

function formatWithinLabel(seconds) {
  if (seconds == null) {
    return '시간창 미적용 · 전체 기간';
  }
  if (seconds < 60) {
    return `현재시각 기준 최근 ${seconds}초 이내 REQUEST_TIME`;
  }
  if (seconds % 60 === 0) {
    return `현재시각 기준 최근 ${seconds / 60}분 이내 REQUEST_TIME`;
  }
  return `현재시각 기준 최근 ${seconds}초 이내 REQUEST_TIME`;
}

function setWithinSeconds(seconds, { syncException } = {}) {
  const value = seconds == null || seconds === '' ? '' : String(seconds);
  withinSecondsEl.value = value;
  const n = currentWithinSeconds();
  withinHintEl.textContent = formatWithinLabel(n);

  withinSecondsGroup.querySelectorAll('.time-chip').forEach((btn) => {
    const chipSec = btn.dataset.seconds || '';
    btn.classList.toggle('active', chipSec === value);
  });

  if (syncException && n != null) {
    exceptionOnlyEl.checked = true;
  }
}

function currentMinElapsedSeconds() {
  const raw = minElapsedSecondsEl.value.trim();
  if (!raw) {
    return null;
  }
  const n = parseInt(raw, 10);
  if (Number.isNaN(n) || n <= 0) {
    return null;
  }
  return n;
}

function formatElapsedLabel(seconds) {
  if (seconds == null) {
    return '소요시간 조건 미적용';
  }
  return `응답−요청 소요 ${seconds}초 이상`;
}

function setMinElapsedSeconds(seconds) {
  const value = seconds == null || seconds === '' ? '' : String(seconds);
  minElapsedSecondsEl.value = value;
  const n = currentMinElapsedSeconds();
  elapsedHintEl.textContent = formatElapsedLabel(n);

  minElapsedSecondsGroup.querySelectorAll('.time-chip').forEach((btn) => {
    const chipSec = btn.dataset.elapsed || '';
    btn.classList.toggle('active', chipSec === value);
  });
}

function parseYmdHms(value) {
  const s = String(value || '').replace(/\D/g, '');
  if (s.length < 14) {
    return null;
  }
  const y = Number(s.slice(0, 4));
  const mo = Number(s.slice(4, 6)) - 1;
  const d = Number(s.slice(6, 8));
  const h = Number(s.slice(8, 10));
  const mi = Number(s.slice(10, 12));
  const se = Number(s.slice(12, 14));
  const ms = s.length >= 17 ? Number(s.slice(14, 17)) : 0;
  const dt = new Date(y, mo, d, h, mi, se, ms);
  return Number.isNaN(dt.getTime()) ? null : dt;
}

function elapsedLabel(row) {
  const req = parseYmdHms(row && row.requestTime);
  const res = parseYmdHms(row && row.responseTime);
  if (!req || !res) {
    return '-';
  }
  const ms = res.getTime() - req.getTime();
  if (ms < 0) {
    return '-';
  }
  if (ms < 1000) {
    return `${ms}ms`;
  }
  return `${(ms / 1000).toFixed(1)}s`;
}

function buildListBody() {
  const withinSeconds = currentWithinSeconds();
  const minElapsedSeconds = currentMinElapsedSeconds();
  return {
    hdr_nhnis: {
      sys_comm: {
        std_gbl_id: newGuid(),
        rms_svc_c: 'mgcoa8888S0',
        scid: 'mgcoa8888',
        optr_eno: 'LOCAL',
        tr_trm_ipadr: '127.0.0.1',
        tr_sysid: 'PDMG-UI',
        sync_dsc: 'S',
        std_tgrm_rqr_rsp_dsc: 'Q'
      }
    },
    dto: {
      guid: guidEl.value.trim() || null,
      serviceId: serviceIdEl.value.trim() || null,
      screenId: screenIdEl.value.trim() || null,
      optrEno: optrEnoEl.value.trim() || null,
      exceptionOnly: !!exceptionOnlyEl.checked,
      withinSeconds: withinSeconds,
      minElapsedSeconds: minElapsedSeconds,
      pageNo: pageNo,
      pageSize: currentPageSize()
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
  if (Array.isArray(dto.mgcoa8888S0DTOSub0)) {
    return dto.mgcoa8888S0DTOSub0;
  }
  if (Array.isArray(dto.records)) {
    return dto.records;
  }
  return [];
}

function statusBadge(row) {
  if (row && row.exceptionType) {
    return '<span class="badge fail">예외</span>';
  }
  if (row && row.responseTime) {
    return '<span class="badge ok">정상</span>';
  }
  return '<span class="badge">진행</span>';
}

function updatePager() {
  pageInfoEl.textContent = `${pageNo} / ${Math.max(totalPages, 1)} · 전체 ${totalCount}건`;
  prevPageBtn.disabled = pageNo <= 1;
  nextPageBtn.disabled = pageNo >= totalPages || totalPages <= 0;
}

function selectedGuids() {
  return [...resultBodyEl.querySelectorAll('input.row-check:checked')]
      .map((el) => el.value)
      .filter(Boolean);
}

function syncCheckAll() {
  const checks = [...resultBodyEl.querySelectorAll('input.row-check')];
  checkAllEl.checked = checks.length > 0 && checks.every((el) => el.checked);
  checkAllEl.indeterminate = checks.some((el) => el.checked) && !checkAllEl.checked;
}

function renderRows(rows, paging) {
  rowsCache = rows || [];
  totalCount = paging && paging.totalCount != null ? Number(paging.totalCount) : rowsCache.length;
  totalPages = paging && paging.totalPages != null
      ? Number(paging.totalPages)
      : (currentPageSize() <= 0 ? 0 : Math.ceil(totalCount / currentPageSize()));
  if (paging && paging.pageNo != null) {
    pageNo = Number(paging.pageNo);
  }
  resultCountEl.textContent = `${rowsCache.length}건`;
  updatePager();
  checkAllEl.checked = false;
  checkAllEl.indeterminate = false;

  if (!rowsCache.length) {
    resultBodyEl.innerHTML = '<tr><td colspan="10" class="empty">조회 결과가 없습니다.</td></tr>';
    return;
  }

  resultBodyEl.innerHTML = rowsCache.map((row, index) => `
    <tr class="clickable" data-index="${index}">
      <td class="col-check"><input class="row-check" type="checkbox" value="${escapeHtml(row.guid)}"></td>
      <td class="mono">${escapeHtml(row.guid)}</td>
      <td>${escapeHtml(row.serviceId)}</td>
      <td>${escapeHtml(row.screenId)}</td>
      <td>${escapeHtml(row.optrEno)}</td>
      <td>${escapeHtml(row.clientIp)}</td>
      <td class="mono">${escapeHtml(row.requestTime)}</td>
      <td class="mono">${escapeHtml(row.responseTime)}</td>
      <td class="mono">${escapeHtml(elapsedLabel(row))}</td>
      <td>${statusBadge(row)}</td>
    </tr>
  `).join('');
}

function renderDetail(row) {
  const fields = [
    ['GUID', row.guid],
    ['서비스 ID', row.serviceId],
    ['화면 ID', row.screenId],
    ['사용자', row.optrEno],
    ['클라이언트 IP', row.clientIp],
    ['요청시각', row.requestTime],
    ['응답시각', row.responseTime],
    ['소요', elapsedLabel(row)],
    ['예외타입', row.exceptionType],
    ['예외코드', row.exceptionCode],
    ['예외메시지', row.exceptionMsg],
    ['요청전문', row.requestMsg],
    ['응답전문', row.responseMsg]
  ];

  detailBodyEl.innerHTML = `
    <dl class="detail-grid">
      ${fields.map(([label, value]) => `
        <div>
          <dt>${escapeHtml(label)}</dt>
          <dd class="${label.includes('메시지') || label.includes('전문') || label === 'GUID' ? 'mono wrap' : 'mono'}">${escapeHtml(value)}</dd>
        </div>
      `).join('')}
    </dl>
  `;
  detailModalEl.hidden = false;
}

function closeDetail() {
  detailModalEl.hidden = true;
}

async function search() {
  resultMetaEl.innerHTML = '<span class="empty">조회 중...</span>';
  resultBodyEl.innerHTML = '<tr><td colspan="10" class="empty">조회 중...</td></tr>';

  let result;
  try {
    result = await PdmgServiceClient.postPath(
        targetBaseUrlEl.value,
        '/mgcoa8888S0',
        buildListBody(),
        serviceConfig.timeoutMs,
        'mgcoa8888S0');
  } catch (error) {
    resultMetaEl.innerHTML = '<span class="badge fail">호출 실패</span>';
    resultBodyEl.innerHTML = '<tr><td colspan="10" class="empty">조회 실패</td></tr>';
    PdmgErrorPopup.showSimple(error.message || String(error), '호출 오류');
    return;
  }
  const httpOk = result.httpStatus >= 200 && result.httpStatus < 300;

  let parsed = null;
  try {
    parsed = JSON.parse(result.responseBody);
  } catch (error) {
    parsed = null;
  }

  const dto = extractDto(parsed) || {};
  const rows = extractRows(parsed);
  const serviceError = PdmgErrorPopup.errorPayload(parsed);
  const ok = httpOk && !(parsed && parsed.error) && !serviceError;
  renderRows(rows, {
    pageNo: dto.pageNo,
    pageSize: dto.pageSize,
    totalCount: dto.totalCount,
    totalPages: dto.totalPages
  });

  const minElapsed = currentMinElapsedSeconds();
  resultMetaEl.innerHTML = `
    <span class="badge ${ok ? 'ok' : 'fail'}">HTTP ${result.httpStatus}</span>
    <span>${result.elapsedMs} ms</span>
    <span>Total: ${totalCount} · page ${pageNo}/${Math.max(totalPages, 1)}</span>
    <span>${escapeHtml(formatWithinLabel(currentWithinSeconds()))}</span>
    ${minElapsed != null ? `<span class="badge">${escapeHtml(formatElapsedLabel(minElapsed))}</span>` : ''}
    ${exceptionOnlyEl.checked ? '<span class="badge fail">예외만</span>' : ''}
    <span>${escapeHtml(result.targetUrl)}</span>
    ${ok ? '' : `<span class="badge fail">${escapeHtml((parsed && (parsed.error || parsed.message)) || '조회 실패')}</span>`}
  `;

  if (!ok) {
    PdmgErrorPopup.showFromResponse(
        parsed,
        result.httpStatus,
        '이미지로그 조회에 실패했습니다.',
        result.responseBody);
  }
}

async function deleteSelected() {
  const guids = selectedGuids();
  if (!guids.length) {
    PdmgErrorPopup.showSimple('삭제할 항목을 선택하세요.', '확인');
    return;
  }
  if (!confirm(`${guids.length}건을 삭제할까요?`)) {
    return;
  }

  let result;
  try {
    result = await PdmgServiceClient.postPath(
        targetBaseUrlEl.value,
        '/mgcoa8888D0',
        {
          hdr_nhnis: {
            sys_comm: {
              std_gbl_id: newGuid(),
              rms_svc_c: 'mgcoa8888D0',
              scid: 'mgcoa8888',
              optr_eno: 'LOCAL',
              tr_trm_ipadr: '127.0.0.1',
              tr_sysid: 'PDMG-UI',
              sync_dsc: 'S',
              std_tgrm_rqr_rsp_dsc: 'Q'
            }
          },
          dto: { guidList: guids, GUID_LIST: guids }
        },
        serviceConfig.timeoutMs,
        'mgcoa8888D0');
  } catch (error) {
    PdmgErrorPopup.showSimple(error.message || String(error), '호출 오류');
    return;
  }
  const httpOk = result.httpStatus >= 200 && result.httpStatus < 300;

  let parsed = null;
  try {
    parsed = JSON.parse(result.responseBody);
  } catch (error) {
    parsed = null;
  }
  const dto = extractDto(parsed) || {};
  const serviceError = PdmgErrorPopup.errorPayload(parsed);
  const ok = httpOk && !(parsed && parsed.error) && !serviceError
      && (dto.RSLT_CD == null || dto.RSLT_CD === '0000');
  if (!ok) {
    if (!PdmgErrorPopup.showFromResponse(
        parsed,
        result.httpStatus,
        dto.RSLT_MSG || '삭제 실패',
        result.responseBody)) {
      PdmgErrorPopup.showSimple(
          (parsed && (parsed.error || parsed.message)) || dto.RSLT_MSG || 'unknown',
          '삭제 실패');
    }
    return;
  }

  alert(`삭제 완료: ${dto.PROC_CNT != null ? dto.PROC_CNT : guids.length}건`);
  await search();
}

function resetFilters() {
  guidEl.value = '';
  serviceIdEl.value = '';
  screenIdEl.value = '';
  optrEnoEl.value = '';
  exceptionOnlyEl.checked = false;
  setWithinSeconds('', { syncException: false });
  setMinElapsedSeconds('');
  pageSizeEl.value = '20';
  pageNo = 1;
  totalPages = 1;
  totalCount = 0;
  resultMetaEl.innerHTML = '<span class="empty">최근 구간·소요시간을 고르고 조회하세요.</span>';
  renderRows([], { pageNo: 1, totalCount: 0, totalPages: 1 });
}

async function init() {
  const configRes = await fetch('/api/config');
  serviceConfig = await configRes.json();
  targetBaseUrlEl.value = serviceConfig.targetBaseUrl || 'http://localhost:8080';
  document.getElementById('targetInfo').textContent = `대상 pdmg-service: ${targetBaseUrlEl.value}`;

  setWithinSeconds('', { syncException: false });
  setMinElapsedSeconds('');
  withinSecondsGroup.querySelectorAll('.time-chip').forEach((btn) => {
    btn.addEventListener('click', () => {
      setWithinSeconds(btn.dataset.seconds || '', { syncException: true });
    });
  });
  minElapsedSecondsGroup.querySelectorAll('.time-chip').forEach((btn) => {
    btn.addEventListener('click', () => {
      setMinElapsedSeconds(btn.dataset.elapsed || '');
    });
  });

  document.getElementById('searchBtn').addEventListener('click', () => {
    pageNo = 1;
    search().catch((error) => PdmgErrorPopup.showSimple('조회 실패: ' + error.message));
  });
  document.getElementById('resetBtn').addEventListener('click', resetFilters);
  document.getElementById('deleteBtn').addEventListener('click', () => {
    deleteSelected().catch((error) => PdmgErrorPopup.showSimple('삭제 실패: ' + error.message));
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

  checkAllEl.addEventListener('change', () => {
    resultBodyEl.querySelectorAll('input.row-check').forEach((el) => {
      el.checked = checkAllEl.checked;
    });
  });

  resultBodyEl.addEventListener('click', (event) => {
    if (event.target.classList.contains('row-check')) {
      syncCheckAll();
      return;
    }
    const tr = event.target.closest('tr[data-index]');
    if (!tr) {
      return;
    }
    const index = Number(tr.dataset.index);
    if (!Number.isNaN(index) && rowsCache[index]) {
      renderDetail(rowsCache[index]);
    }
  });

  detailModalEl.addEventListener('click', (event) => {
    if (event.target.dataset.close === 'true') {
      closeDetail();
    }
  });

  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && !detailModalEl.hidden) {
      closeDetail();
    }
  });
}

init().catch((error) => PdmgErrorPopup.showSimple('화면 초기화 실패: ' + error.message));
