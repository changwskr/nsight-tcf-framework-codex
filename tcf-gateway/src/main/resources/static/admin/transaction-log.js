const API = '/api/admin/transaction-log';
const PAGE_SIZE = 20;
let currentPage = 1;
let selectedRow = null;
let businessCodes = [];

const $ = id => document.getElementById(id);

function toast(msg, type = 'success') {
  const el = $('toast');
  el.textContent = msg;
  el.className = `toast ${type}`;
  setTimeout(() => el.classList.add('hidden'), 2800);
}

function field(row, key, fallback = '-') {
  if (!row) return fallback;
  const direct = row[key];
  if (direct != null && direct !== '') return direct;
  const lower = row[key.toLowerCase()];
  if (lower != null && lower !== '') return lower;
  return fallback;
}

function chipForResult(status) {
  const s = String(status || '').toUpperCase();
  if (s === 'SUCCESS') return '<span class="tag y">SUCCESS</span>';
  if (s === 'ERROR') return '<span class="tag n" style="color:#fca5a5">ERROR</span>';
  if (s === 'FAIL') return '<span class="tag n" style="color:#fcd34d">FAIL</span>';
  return field({ v: status }, 'v', '-');
}

function readFilters() {
  const params = new URLSearchParams();
  const add = (key, id) => {
    const v = $(id).value.trim();
    if (v) params.set(key, v);
  };
  add('businessCode', 'filterBusinessCode');
  add('guid', 'filterGuid');
  add('traceId', 'filterTraceId');
  add('serviceId', 'filterServiceId');
  add('transactionCode', 'filterTransactionCode');
  add('userId', 'filterUserId');
  add('branchId', 'filterBranchId');
  add('resultStatus', 'filterResultStatus');
  add('errorCode', 'filterErrorCode');
  add('fromDate', 'filterFromDate');
  add('toDate', 'filterToDate');
  params.set('pageNo', String(currentPage));
  params.set('pageSize', String(PAGE_SIZE));
  return params;
}

function renderSummary(summary) {
  const s = summary || {};
  const total = Number(s.totalCount ?? 0);
  const success = Number(s.successCount ?? 0);
  const error = Number(s.errorCount ?? 0);
  const timeout = Number(s.timeoutCount ?? 0);
  const avg = Math.round(Number(s.avgElapsedMs ?? 0));
  const rate = total > 0 ? ((success / total) * 100).toFixed(1) : '0.0';
  $('summaryBar').innerHTML = `
    <span class="stat-pill">총 ${total}건</span>
    <span class="stat-pill">성공 ${success}건 (${rate}%)</span>
    <span class="stat-pill">오류 ${error}건</span>
    <span class="stat-pill">Timeout ${timeout}건</span>
    <span class="stat-pill">평균 ${avg}ms</span>`;
}

function renderDetail(row) {
  const panel = $('detailPanel');
  if (!row) {
    panel.hidden = true;
    panel.innerHTML = '<div class="card-head">거래 상세</div><div class="card-body empty-hint">목록에서 행을 선택하세요.</div>';
    return;
  }
  panel.hidden = false;
  const fields = [
    ['LogId', 'logId'], ['일시', 'txTime'], ['환경', 'envCode'], ['업무코드', 'businessCode'],
    ['ServiceId', 'serviceId'], ['거래코드', 'transactionCode'],
    ['GUID', 'guid'], ['TraceId', 'traceId'],
    ['사용자', 'userId'], ['지점', 'branchId'], ['세션', 'sessionId'],
    ['Target URL', 'targetUrl'], ['HTTP', 'httpStatus'],
    ['결과', 'resultStatus'], ['결과코드', 'resultCode'],
    ['오류코드', 'errorCode'], ['Phase', 'phase'], ['처리시간', 'elapsedTimeMs']
  ];
  panel.innerHTML = `
    <div class="card-head">거래 상세 · ${field(row, 'logId')}</div>
    <div class="card-body">
      <div class="kv-list">
        ${fields.map(([label, key]) => {
          let val = field(row, key, '');
          if (key === 'elapsedTimeMs' && val !== '' && val !== '-') val = `${val}ms`;
          if (key === 'resultStatus') val = chipForResult(val);
          if (key === 'targetUrl') val = `<span class="url-cell">${val}</span>`;
          if (key === 'sessionId' || key === 'guid' || key === 'traceId' || key === 'logId') {
            val = `<span class="mono">${val}</span>`;
          }
          return `<div class="kv-row"><span class="kv-label">${label}</span><span>${val}</span></div>`;
        }).join('')}
      </div>
    </div>`;
}

function bindRowClick(rows) {
  document.querySelectorAll('#txTableBody tr.row-selectable').forEach(tr => {
    tr.addEventListener('click', () => {
      const idx = Number(tr.dataset.idx);
      document.querySelectorAll('#txTableBody tr.row-selected').forEach(r => r.classList.remove('row-selected'));
      tr.classList.add('row-selected');
      selectedRow = rows[idx];
      renderDetail(selectedRow);
    });
  });
}

function renderPagination(pageNo, pageSize, totalCount) {
  const el = $('pagination');
  const totalPages = Math.max(1, Math.ceil(totalCount / pageSize));
  if (totalPages <= 1) {
    el.hidden = true;
    return;
  }
  el.hidden = false;
  const pages = [];
  const start = Math.max(1, pageNo - 2);
  const end = Math.min(totalPages, pageNo + 2);
  if (pageNo > 1) pages.push(`<button type="button" data-page="${pageNo - 1}">‹</button>`);
  for (let p = start; p <= end; p++) {
    pages.push(`<button type="button" class="${p === pageNo ? 'active' : ''}" data-page="${p}">${p}</button>`);
  }
  if (pageNo < totalPages) pages.push(`<button type="button" data-page="${pageNo + 1}">›</button>`);
  el.innerHTML = pages.join('');
  el.querySelectorAll('button[data-page]').forEach(btn => {
    btn.addEventListener('click', () => loadList(Number(btn.dataset.page)));
  });
}

async function loadList(page = 1) {
  currentPage = page;
  selectedRow = null;
  renderDetail(null);

  const tbody = $('txTableBody');
  const statusEl = $('listStatus');
  tbody.innerHTML = '<tr><td colspan="10" class="empty">조회 중...</td></tr>';
  statusEl.textContent = '조회 중...';

  const started = performance.now();
  const res = await fetch(`${API}?${readFilters().toString()}`);
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.error || `조회 실패 (${res.status})`);
  }
  const body = await res.json();
  const elapsedMs = Math.round(performance.now() - started);

  renderSummary(body.summary);
  const rows = body.rows || [];
  statusEl.textContent = `${elapsedMs}ms · 페이지 ${body.pageNo || page} · 총 ${body.totalCount ?? 0}건`;

  if (rows.length === 0) {
    tbody.innerHTML = '<tr><td colspan="10" class="empty">조건에 맞는 거래로그가 없습니다.</td></tr>';
  } else {
    tbody.innerHTML = rows.map((r, idx) => `
      <tr class="row-selectable" data-idx="${idx}" title="클릭하여 상세 보기">
        <td>${field(r, 'txTime')}</td>
        <td>${field(r, 'businessCode')}</td>
        <td class="mono">${field(r, 'serviceId')}</td>
        <td class="mono">${field(r, 'transactionCode')}</td>
        <td class="mono">${field(r, 'guid')}</td>
        <td class="mono">${field(r, 'traceId')}</td>
        <td>${field(r, 'userId')}</td>
        <td>${chipForResult(field(r, 'resultStatus'))}</td>
        <td>${field(r, 'elapsedTimeMs')}ms</td>
        <td class="mono">${field(r, 'errorCode')}</td>
      </tr>`).join('');
    bindRowClick(rows);
  }

  renderPagination(body.pageNo || page, body.pageSize || PAGE_SIZE, body.totalCount || 0);
}

function fillBusinessSelect() {
  const select = $('filterBusinessCode');
  businessCodes.forEach(item => {
    const opt = document.createElement('option');
    opt.value = item.code;
    opt.textContent = `${item.code} — ${item.name}`;
    select.appendChild(opt);
  });
}

async function loadMeta() {
  const res = await fetch(`${API}/meta`);
  if (!res.ok) return;
  const meta = await res.json();
  $('currentEnvBadge').textContent = meta.currentEnvCode || '-';
  businessCodes = meta.businessCodes || [];
  fillBusinessSelect();
}

function resetFilters() {
  document.querySelectorAll('.toolbar input[type="text"]').forEach(i => { i.value = ''; });
  $('filterBusinessCode').value = '';
  $('filterResultStatus').value = '';
}

async function deleteAll() {
  if (!confirm('TCF_GATEWAY_TX_LOG 테이블의 거래로그를 전부 삭제합니다.\n되돌릴 수 없습니다. 계속하시겠습니까?')) {
    return;
  }
  const deleteReason = prompt('삭제 사유 (5자 이상):');
  if (!deleteReason || deleteReason.trim().length < 5) {
    alert('삭제 사유를 5자 이상 입력해야 합니다.');
    return;
  }
  const res = await fetch(API, {
    method: 'DELETE',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ confirmCode: 'DELETE_ALL', deleteReason: deleteReason.trim() })
  });
  const body = await res.json();
  if (!res.ok) {
    throw new Error(body.error || `삭제 실패 (${res.status})`);
  }
  toast(`전체 로그 ${body.deletedCount ?? 0}건 삭제`);
  await loadList(1);
}

document.addEventListener('DOMContentLoaded', async () => {
  $('btnSearch').addEventListener('click', () => loadList(1).catch(e => toast(e.message, 'error')));
  $('btnRefresh').addEventListener('click', () => loadList(currentPage).catch(e => toast(e.message, 'error')));
  $('btnReset').addEventListener('click', resetFilters);
  $('btnDeleteAll').addEventListener('click', () => deleteAll().catch(e => toast(e.message, 'error')));

  try {
    await loadMeta();
    await loadList(1);
  } catch (e) {
    toast(e.message, 'error');
  }
});
