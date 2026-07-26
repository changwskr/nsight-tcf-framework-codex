const API = '/api/admin/sessions';
const PAGE_SIZE = 20;
let currentPage = 1;
let selectedRow = null;

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

function shortId(id) {
  if (!id || id === '-') return '-';
  const s = String(id);
  return s.length <= 20 ? s : `${s.slice(0, 10)}…${s.slice(-6)}`;
}

function chipForStatus(status, activeYn) {
  const s = String(status || '').toUpperCase();
  if (activeYn === 'Y' || s === 'ACTIVE') return '<span class="tag y">ACTIVE</span>';
  if (s === 'FORCED_LOGOUT') return '<span class="tag n" style="color:#fcd34d">FORCED</span>';
  if (s === 'EXPIRED') return '<span class="tag n">EXPIRED</span>';
  if (s === 'LOGGED_OUT') return '<span class="tag n">LOGOUT</span>';
  return `<span class="tag n">${field({ v: status }, 'v')}</span>`;
}

function readFilters() {
  const params = new URLSearchParams();
  const userId = $('filterUserId').value.trim();
  if (userId) params.set('userId', userId);
  params.set('activeOnly', $('filterActive').checked ? 'Y' : 'N');
  params.set('pageNo', String(currentPage));
  params.set('pageSize', String(PAGE_SIZE));
  return params;
}

function renderDetail(row) {
  const panel = $('detailPanel');
  if (!row) {
    panel.hidden = true;
    panel.innerHTML = '<div class="card-head">세션 상세</div><div class="card-body empty-hint">목록에서 행을 선택하세요.</div>';
    return;
  }
  panel.hidden = false;
  const fields = [
    ['세션 ID', 'sessionId'], ['사용자 ID', 'userId'], ['이름', 'userName'],
    ['지점', 'branchId'], ['채널', 'channelId'], ['권한그룹', 'authGroupId'],
    ['세션 유형', 'sessionType'], ['상태', 'status'], ['활성', 'activeYn'],
    ['로그인', 'loginTime'], ['최종 접근', 'lastAccessTime'], ['절대 만료', 'absoluteExpireTime'],
    ['로그아웃', 'logoutTime'], ['종료 사유', 'logoutReason'],
    ['Client IP', 'clientIp'], ['Center', 'centerId'], ['WAS', 'wasId'],
    ['User-Agent', 'userAgent']
  ];
  panel.innerHTML = `
    <div class="card-head">세션 상세 · ${shortId(field(row, 'sessionId'))}</div>
    <div class="card-body">
      <div class="kv-list">
        ${fields.map(([label, key]) => {
          let val = field(row, key, '');
          if (key === 'status') val = chipForStatus(val, field(row, 'activeYn', ''));
          if (key === 'sessionId' || key === 'userAgent') val = `<span class="mono">${val || '-'}</span>`;
          return `<div class="kv-row"><span class="kv-label">${label}</span><span>${val || '-'}</span></div>`;
        }).join('')}
      </div>
      ${field(row, 'activeYn') === 'Y' ? `
        <div style="margin-top:16px">
          <button type="button" id="btnKillDetail" class="btn danger">세션 강제 종료</button>
        </div>` : ''}
    </div>`;
  const killBtn = $('btnKillDetail');
  if (killBtn) {
    killBtn.addEventListener('click', () => forceLogout(field(row, 'sessionId')));
  }
}

function bindRowClick(rows) {
  document.querySelectorAll('#sessionBody tr.row-selectable').forEach(tr => {
    tr.addEventListener('click', e => {
      if (e.target.closest('button')) return;
      const idx = Number(tr.dataset.idx);
      document.querySelectorAll('#sessionBody tr.row-selected').forEach(r => r.classList.remove('row-selected'));
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

async function forceLogout(sessionId) {
  const reason = prompt('세션 종료 사유 (5자 이상):');
  if (!reason || reason.trim().length < 5) {
    alert('종료 사유를 5자 이상 입력해야 합니다.');
    return;
  }
  const res = await fetch(`${API}/${encodeURIComponent(sessionId)}`, {
    method: 'DELETE',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ deleteReason: reason.trim() })
  });
  const body = await res.json();
  if (!res.ok) throw new Error(body.error || `종료 실패 (${res.status})`);
  toast(`세션 종료: ${body.userId || sessionId}`);
  await loadList(currentPage);
}

async function loadList(page = 1) {
  currentPage = page;
  selectedRow = null;
  renderDetail(null);

  const tbody = $('sessionBody');
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

  $('statActive').textContent = body.activeCount ?? 0;
  $('statTotal').textContent = body.totalCount ?? 0;
  statusEl.textContent = `${elapsedMs}ms · 페이지 ${body.pageNo || page} · 총 ${body.totalCount ?? 0}건`;

  const rows = body.rows || [];
  if (rows.length === 0) {
    tbody.innerHTML = '<tr><td colspan="10" class="empty">조건에 맞는 세션이 없습니다.<br><small>OM 로그인 후 TCF_USER_SESSION에 등록됩니다.</small></td></tr>';
  } else {
    tbody.innerHTML = rows.map((r, idx) => {
      const sid = field(r, 'sessionId');
      const active = field(r, 'activeYn') === 'Y';
      return `
      <tr class="row-selectable" data-idx="${idx}" title="클릭하여 상세 보기">
        <td class="mono" title="${sid}">${shortId(sid)}</td>
        <td>${field(r, 'userId')}</td>
        <td>${field(r, 'userName')}</td>
        <td>${field(r, 'branchId')}</td>
        <td>${field(r, 'sessionType')}</td>
        <td>${field(r, 'loginTime')}</td>
        <td>${field(r, 'lastAccessTime')}</td>
        <td>${field(r, 'absoluteExpireTime')}</td>
        <td>${chipForStatus(field(r, 'status'), field(r, 'activeYn'))}</td>
        <td>${active ? `<button type="button" class="btn danger btn-kill" data-id="${sid}">종료</button>` : '-'}</td>
      </tr>`;
    }).join('');
    bindRowClick(rows);
    tbody.querySelectorAll('.btn-kill').forEach(btn => {
      btn.addEventListener('click', e => {
        e.stopPropagation();
        forceLogout(btn.dataset.id).catch(err => toast(err.message, 'error'));
      });
    });
  }

  renderPagination(body.pageNo || page, body.pageSize || PAGE_SIZE, body.totalCount || 0);
}

async function loadMeta() {
  const res = await fetch(`${API}/meta`);
  if (!res.ok) return;
  const meta = await res.json();
  $('currentEnvBadge').textContent = meta.currentEnvCode || '-';
}

function resetFilters() {
  $('filterUserId').value = '';
  $('filterActive').checked = true;
}

document.addEventListener('DOMContentLoaded', async () => {
  $('btnSearch').addEventListener('click', () => loadList(1).catch(e => toast(e.message, 'error')));
  $('btnRefresh').addEventListener('click', () => loadList(currentPage).catch(e => toast(e.message, 'error')));
  $('btnReset').addEventListener('click', resetFilters);
  $('filterActive').addEventListener('change', () => loadList(1).catch(e => toast(e.message, 'error')));

  try {
    await loadMeta();
    await loadList(1);
  } catch (e) {
    toast(e.message, 'error');
  }
});
