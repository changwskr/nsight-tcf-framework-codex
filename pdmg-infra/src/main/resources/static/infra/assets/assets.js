/*
 * INF-320 서버 자산 관리 (파일럿 ifina1999)
 * list   : POST /ifina1999S0
 * create : POST /ifina1999C0
 * update : POST /ifina1999U0
 * delete : POST /ifina1999D0
 */

const SCID = 'INF-320';
let pageNo = 1;
let totalPages = 1;
let totalCount = 0;
let rowsCache = [];
let editMode = 'create';

const keywordEl = document.getElementById('keyword');
const techRoleEl = document.getElementById('techRole');
const envCdEl = document.getElementById('envCd');
const statusCdEl = document.getElementById('statusCd');
const pageSizeEl = document.getElementById('pageSize');
const resultMetaEl = document.getElementById('resultMeta');
const resultCountEl = document.getElementById('resultCount');
const resultBodyEl = document.getElementById('resultBody');
const pageInfoEl = document.getElementById('pageInfo');
const prevPageBtn = document.getElementById('prevPageBtn');
const nextPageBtn = document.getElementById('nextPageBtn');
const editModalEl = document.getElementById('editModal');
const editTitleEl = document.getElementById('editTitle');
const formServerIdEl = document.getElementById('formServerId');
const formServerNameEl = document.getElementById('formServerName');
const formTechRoleEl = document.getElementById('formTechRole');
const formEnvCdEl = document.getElementById('formEnvCd');
const formTierCdEl = document.getElementById('formTierCd');
const formStatusCdEl = document.getElementById('formStatusCd');
const formRemarkEl = document.getElementById('formRemark');

function currentPageSize() {
  const size = parseInt(pageSizeEl.value, 10);
  return Number.isNaN(size) || size <= 0 ? 10 : Math.min(size, 100);
}

function updatePager() {
  pageInfoEl.textContent = `${pageNo} / ${Math.max(totalPages, 1)} · 전체 ${totalCount}건`;
  prevPageBtn.disabled = pageNo <= 1;
  nextPageBtn.disabled = pageNo >= totalPages || totalPages <= 0;
}

function renderRows(rows) {
  rowsCache = Array.isArray(rows) ? rows : [];
  resultCountEl.textContent = `${totalCount}건`;
  updatePager();
  if (!rowsCache.length) {
    resultBodyEl.innerHTML = '<tr><td colspan="9" class="empty">데이터 없음</td></tr>';
    return;
  }
  resultBodyEl.innerHTML = rowsCache.map((row, index) => `
    <tr data-index="${index}">
      <td class="mono">${InfraApi.escapeHtml(row.serverId)}</td>
      <td>${InfraApi.escapeHtml(row.serverName)}</td>
      <td>${InfraApi.escapeHtml(row.techRole)}</td>
      <td>${InfraApi.escapeHtml(row.envCd)}</td>
      <td>${InfraApi.escapeHtml(row.tierCd)}</td>
      <td>${InfraApi.statusBadge(row.statusCd)}</td>
      <td>${InfraApi.escapeHtml(row.remark)}</td>
      <td class="mono">${InfraApi.escapeHtml(row.chgDtm || row.regDtm)}</td>
      <td>
        <button class="btn-icon" type="button" data-action="edit">수정</button>
        <button class="btn-icon" type="button" data-action="delete">삭제</button>
      </td>
    </tr>
  `).join('');
}

async function search(resetPage) {
  if (resetPage) {
    pageNo = 1;
  }
  resultMetaEl.textContent = '조회 중…';
  const res = await InfraApi.postService('ifina1999S0', {
    keyword: keywordEl.value.trim() || null,
    techRole: techRoleEl.value || null,
    envCd: envCdEl.value || null,
    statusCd: statusCdEl.value || null,
    pageNo,
    pageSize: currentPageSize()
  }, SCID);

  const dto = res.dto || {};
  const rows = Array.isArray(dto.ifina1999S0DTOSub0) ? dto.ifina1999S0DTOSub0 : [];
  totalCount = dto.totalCount != null ? Number(dto.totalCount) : rows.length;
  totalPages = dto.totalPages != null
    ? Number(dto.totalPages)
    : Math.max(1, Math.ceil(totalCount / currentPageSize()));
  pageNo = dto.pageNo != null ? Number(dto.pageNo) : pageNo;
  renderRows(rows);
  resultMetaEl.textContent = `HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ifina1999S0`;
  if (res.httpStatus >= 400) {
    resultMetaEl.textContent += ` · 오류: ${res.responseBody.slice(0, 120)}`;
  }
}

function openCreateModal() {
  editMode = 'create';
  editTitleEl.textContent = '서버 자산 등록';
  formServerIdEl.disabled = false;
  formServerIdEl.value = '';
  formServerNameEl.value = '';
  formTechRoleEl.value = 'WAS';
  formEnvCdEl.value = 'PROD';
  formTierCdEl.value = 'TIER2';
  formStatusCdEl.value = 'CONFIRMED';
  formRemarkEl.value = '';
  editModalEl.hidden = false;
}

function openEditModal(row) {
  editMode = 'edit';
  editTitleEl.textContent = '서버 자산 수정';
  formServerIdEl.disabled = true;
  formServerIdEl.value = row.serverId || '';
  formServerNameEl.value = row.serverName || '';
  formTechRoleEl.value = row.techRole || 'OTHER';
  formEnvCdEl.value = row.envCd || 'DEV';
  formTierCdEl.value = row.tierCd || 'TIER3';
  formStatusCdEl.value = row.statusCd || 'DISCOVERED';
  formRemarkEl.value = row.remark || '';
  editModalEl.hidden = false;
}

function closeModal() {
  editModalEl.hidden = true;
}

async function save() {
  const payload = {
    serverId: formServerIdEl.value.trim(),
    serverName: formServerNameEl.value.trim(),
    techRole: formTechRoleEl.value,
    envCd: formEnvCdEl.value,
    tierCd: formTierCdEl.value,
    statusCd: formStatusCdEl.value,
    remark: formRemarkEl.value.trim()
  };
  if (!payload.serverId || !payload.serverName) {
    alert('Server ID와 서버명은 필수입니다.');
    return;
  }
  const serviceId = editMode === 'create' ? 'ifina1999C0' : 'ifina1999U0';
  const res = await InfraApi.postService(serviceId, payload, SCID);
  const dto = res.dto || {};
  const cd = dto.RSLT_CD || dto.rslt_cd || (res.httpStatus < 400 ? '0000' : '0009');
  if (String(cd) !== '0000' && res.httpStatus >= 400) {
    alert(`저장 실패: ${dto.RSLT_MSG || dto.rslt_msg || res.responseBody}`);
    return;
  }
  if (String(cd) !== '0000' && dto.RSLT_MSG) {
    alert(`${cd}: ${dto.RSLT_MSG}`);
    if (String(cd) !== '0000') {
      return;
    }
  }
  closeModal();
  await search(editMode === 'create');
}

async function removeRow(row) {
  if (!confirm(`삭제할까요?\n${row.serverId} ${row.serverName}`)) {
    return;
  }
  const res = await InfraApi.postService('ifina1999D0', {
    serverIdList: [row.serverId]
  }, SCID);
  const dto = res.dto || {};
  if (dto.RSLT_CD && dto.RSLT_CD !== '0000') {
    alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG || ''}`);
    return;
  }
  await search(false);
}

document.getElementById('searchBtn').addEventListener('click', () => search(true));
document.getElementById('addBtn').addEventListener('click', openCreateModal);
document.getElementById('migrateBtn').addEventListener('click', () => migrateAll().catch(console.error));
document.getElementById('saveBtn').addEventListener('click', () => save().catch(console.error));

async function migrateAll() {
  if (!confirm('파일럿 전체 → 정규(ifina3100) 이관할까요?')) {
    return;
  }
  const res = await InfraApi.postService('ifina1999E0', {
    defaultSystemId: 'SYS-ONLINE',
    dryRunYn: 'N'
  }, SCID);
  const dto = res.dto || {};
  alert(`이관 결과\nmigrated=${dto.migratedCount||0}, skipped=${dto.skippedCount||0}, error=${dto.errorCount||0}\n${dto.RSLT_MSG||''}`);
  window.location.href = '/infra/server-assets/index.html';
}
prevPageBtn.addEventListener('click', () => {
  if (pageNo > 1) {
    pageNo -= 1;
    search(false);
  }
});
nextPageBtn.addEventListener('click', () => {
  if (pageNo < totalPages) {
    pageNo += 1;
    search(false);
  }
});
keywordEl.addEventListener('keydown', (e) => {
  if (e.key === 'Enter') {
    search(true);
  }
});

resultBodyEl.addEventListener('click', (e) => {
  const btn = e.target.closest('[data-action]');
  if (!btn) {
    return;
  }
  const tr = btn.closest('tr[data-index]');
  if (!tr) {
    return;
  }
  const row = rowsCache[Number(tr.dataset.index)];
  if (!row) {
    return;
  }
  if (btn.dataset.action === 'edit') {
    openEditModal(row);
  } else if (btn.dataset.action === 'delete') {
    removeRow(row).catch(console.error);
  }
});

editModalEl.querySelectorAll('[data-close="true"]').forEach((el) => {
  el.addEventListener('click', closeModal);
});

search(true).catch(console.error);
