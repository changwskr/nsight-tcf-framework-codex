/*
 * 이미지로그 관리 화면.
 * 필터 + 페이징 + 선택 삭제 + 상세 모달.
 * list  : /api/imagelog/list  → POST /mkcoa8888S0
 * delete: /api/imagelog/delete → POST /mkcoa8888D0
 */

const targetBaseUrlEl = document.getElementById('targetBaseUrl');
const guidEl = document.getElementById('guid');
const serviceIdEl = document.getElementById('serviceId');
const screenIdEl = document.getElementById('screenId');
const optrEnoEl = document.getElementById('optrEno');
const exceptionOnlyEl = document.getElementById('exceptionOnly');
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

function buildListBody() {
  return {
    hdr_nhnis: {
      sys_comm: {
        std_gbl_id: crypto.randomUUID().replaceAll('-', ''),
        rms_svc_c: 'mkcoa8888S0',
        scid: 'mkcoa8888',
        optr_eno: 'LOCAL',
        tr_trm_ipadr: '127.0.0.1',
        tr_sysid: 'PDMK-UI',
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
  if (Array.isArray(dto.mkcoa8888S0DTOSub0)) {
    return dto.mkcoa8888S0DTOSub0;
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
    resultBodyEl.innerHTML = '<tr><td colspan="9" class="empty">조회 결과가 없습니다.</td></tr>';
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
    ['예외타입', row.exceptionType],
    ['예외코드', row.exceptionCode],
    ['예외메시지', row.exceptionMsg]
  ];

  detailBodyEl.innerHTML = `
    <dl class="detail-grid">
      ${fields.map(([label, value]) => `
        <div>
          <dt>${escapeHtml(label)}</dt>
          <dd class="${label.includes('메시지') || label === 'GUID' ? 'mono wrap' : 'mono'}">${escapeHtml(value)}</dd>
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
  resultBodyEl.innerHTML = '<tr><td colspan="9" class="empty">조회 중...</td></tr>';

  const query = new URLSearchParams({ baseUrl: targetBaseUrlEl.value.trim() });
  let result;
  try {
    const response = await fetch(`/api/imagelog/list?${query}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(buildListBody())
    });
    result = await response.json();
  } catch (error) {
    resultMetaEl.innerHTML = '<span class="badge fail">중계 실패</span>';
    resultBodyEl.innerHTML = '<tr><td colspan="9" class="empty">조회 실패</td></tr>';
    PdmkErrorPopup.showSimple(error.message || String(error), '중계 오류');
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
  const serviceError = PdmkErrorPopup.errorPayload(parsed);
  const ok = httpOk && !(parsed && parsed.error) && !serviceError;
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
    ${ok ? '' : `<span class="badge fail">${escapeHtml((parsed && (parsed.error || parsed.message)) || '조회 실패')}</span>`}
  `;

  if (!ok) {
    PdmkErrorPopup.showFromResponse(parsed, result.httpStatus, '이미지로그 조회에 실패했습니다.');
  }
}

async function deleteSelected() {
  const guids = selectedGuids();
  if (!guids.length) {
    PdmkErrorPopup.showSimple('삭제할 항목을 선택하세요.', '확인');
    return;
  }
  if (!confirm(`${guids.length}건을 삭제할까요?`)) {
    return;
  }

  const query = new URLSearchParams({ baseUrl: targetBaseUrlEl.value.trim() });
  let result;
  try {
    const response = await fetch(`/api/imagelog/delete?${query}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        hdr_nhnis: {
          sys_comm: {
            std_gbl_id: crypto.randomUUID().replaceAll('-', ''),
            rms_svc_c: 'mkcoa8888D0',
            scid: 'mkcoa8888',
            optr_eno: 'LOCAL',
            tr_trm_ipadr: '127.0.0.1',
            tr_sysid: 'PDMK-UI',
            sync_dsc: 'S',
            std_tgrm_rqr_rsp_dsc: 'Q'
          }
        },
        dto: { guidList: guids, GUID_LIST: guids }
      })
    });
    result = await response.json();
  } catch (error) {
    PdmkErrorPopup.showSimple(error.message || String(error), '중계 오류');
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
  const serviceError = PdmkErrorPopup.errorPayload(parsed);
  const ok = httpOk && !(parsed && parsed.error) && !serviceError
      && (dto.RSLT_CD == null || dto.RSLT_CD === '0000');
  if (!ok) {
    if (!PdmkErrorPopup.showFromResponse(parsed, result.httpStatus, dto.RSLT_MSG || '삭제에 실패했습니다.')) {
      PdmkErrorPopup.showSimple(
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
  pageSizeEl.value = '20';
  pageNo = 1;
  totalPages = 1;
  totalCount = 0;
  resultMetaEl.innerHTML = '<span class="empty">조건을 입력한 뒤 조회하세요.</span>';
  renderRows([], { pageNo: 1, totalCount: 0, totalPages: 1 });
}

async function init() {
  const configRes = await fetch('/api/config');
  const config = await configRes.json();
  targetBaseUrlEl.value = config.targetBaseUrl || 'http://localhost:8080';
  document.getElementById('targetInfo').textContent = `대상 pdmk-service: ${targetBaseUrlEl.value}`;

  document.getElementById('searchBtn').addEventListener('click', () => {
    pageNo = 1;
    search().catch((error) => PdmkErrorPopup.showSimple('조회 실패: ' + error.message));
  });
  document.getElementById('resetBtn').addEventListener('click', resetFilters);
  document.getElementById('deleteBtn').addEventListener('click', () => {
    deleteSelected().catch((error) => PdmkErrorPopup.showSimple('삭제 실패: ' + error.message));
  });

  prevPageBtn.addEventListener('click', () => {
    if (pageNo <= 1) {
      return;
    }
    pageNo -= 1;
    search().catch((error) => PdmkErrorPopup.showSimple('조회 실패: ' + error.message));
  });
  nextPageBtn.addEventListener('click', () => {
    if (pageNo >= totalPages) {
      return;
    }
    pageNo += 1;
    search().catch((error) => PdmkErrorPopup.showSimple('조회 실패: ' + error.message));
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

init().catch((error) => PdmkErrorPopup.showSimple('화면 초기화 실패: ' + error.message));
