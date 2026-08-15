const SCID = 'INF-310';
let pageNo = 1, totalPages = 1, totalCount = 0, rowsCache = [], editMode = 'create';

const els = {
  keyword: document.getElementById('keyword'),
  techRoleCd: document.getElementById('techRoleCd'),
  envCd: document.getElementById('envCd'),
  pageSize: document.getElementById('pageSize'),
  resultMeta: document.getElementById('resultMeta'),
  resultCount: document.getElementById('resultCount'),
  resultBody: document.getElementById('resultBody'),
  pageInfo: document.getElementById('pageInfo'),
  prev: document.getElementById('prevPageBtn'),
  next: document.getElementById('nextPageBtn'),
  modal: document.getElementById('editModal'),
  title: document.getElementById('editTitle'),
  groupId: document.getElementById('formGroupId'),
  groupName: document.getElementById('formGroupName'),
  systemId: document.getElementById('formSystemId'),
  techRole: document.getElementById('formTechRoleCd'),
  env: document.getElementById('formEnvCd'),
  tier: document.getElementById('formTierCd'),
  active: document.getElementById('formActiveNodes'),
  standby: document.getElementById('formStandbyNodes'),
  dr: document.getElementById('formDrNodes'),
  status: document.getElementById('formStatusCd'),
  remark: document.getElementById('formRemark')
};

function pageSize() {
  const n = parseInt(els.pageSize.value, 10);
  return Number.isNaN(n) || n <= 0 ? 10 : Math.min(n, 100);
}
function updatePager() {
  els.pageInfo.textContent = `${pageNo} / ${Math.max(totalPages, 1)} · 전체 ${totalCount}건`;
  els.prev.disabled = pageNo <= 1;
  els.next.disabled = pageNo >= totalPages || totalPages <= 0;
}
function render(rows) {
  rowsCache = rows || [];
  els.resultCount.textContent = `${totalCount}건`;
  updatePager();
  if (!rowsCache.length) {
    els.resultBody.innerHTML = '<tr><td colspan="11" class="empty">데이터 없음</td></tr>';
    return;
  }
  els.resultBody.innerHTML = rowsCache.map((row, i) => `
    <tr data-index="${i}">
      <td class="mono">${InfraApi.escapeHtml(row.groupId)}</td>
      <td>${InfraApi.escapeHtml(row.groupName)}</td>
      <td class="mono">${InfraApi.escapeHtml(row.systemId)}</td>
      <td>${InfraApi.escapeHtml(row.techRoleCd)}</td>
      <td>${InfraApi.escapeHtml(row.envCd)}</td>
      <td>${InfraApi.escapeHtml(row.tierCd)}</td>
      <td>${InfraApi.escapeHtml(row.activeNodes)}</td>
      <td>${InfraApi.escapeHtml(row.standbyNodes)}</td>
      <td>${InfraApi.escapeHtml(row.drNodes)}</td>
      <td>${InfraApi.statusBadge(row.statusCd)}</td>
      <td>
        <button class="btn-icon" type="button" data-action="edit">수정</button>
        <button class="btn-icon" type="button" data-action="delete">삭제</button>
      </td>
    </tr>`).join('');
}
async function search(reset) {
  if (reset) pageNo = 1;
  els.resultMeta.textContent = '조회 중…';
  const res = await InfraApi.postService('ifina3110S0', {
    keyword: els.keyword.value.trim() || null,
    techRoleCd: els.techRoleCd.value || null,
    envCd: els.envCd.value || null,
    pageNo, pageSize: pageSize()
  }, SCID);
  const dto = res.dto || {};
  const rows = Array.isArray(dto.ifina3110S0DTOSub0) ? dto.ifina3110S0DTOSub0 : [];
  totalCount = dto.totalCount != null ? Number(dto.totalCount) : rows.length;
  totalPages = dto.totalPages != null ? Number(dto.totalPages) : Math.max(1, Math.ceil(totalCount / pageSize()));
  pageNo = dto.pageNo != null ? Number(dto.pageNo) : pageNo;
  render(rows);
  els.resultMeta.textContent = `HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ifina3110S0`;
}
function openCreate() {
  editMode = 'create';
  els.title.textContent = '서버군 등록';
  els.groupId.disabled = false;
  els.groupId.value = '';
  els.groupName.value = '';
  els.systemId.value = 'SYS-ONLINE';
  els.techRole.value = 'WAS';
  els.env.value = 'PROD';
  els.tier.value = 'TIER1';
  els.active.value = 4;
  els.standby.value = 0;
  els.dr.value = 0;
  els.status.value = 'CONFIRMED';
  els.remark.value = '';
  els.modal.hidden = false;
}
function openEdit(row) {
  editMode = 'edit';
  els.title.textContent = '서버군 수정';
  els.groupId.disabled = true;
  els.groupId.value = row.groupId || '';
  els.groupName.value = row.groupName || '';
  els.systemId.value = row.systemId || '';
  els.techRole.value = row.techRoleCd || 'WAS';
  els.env.value = row.envCd || 'PROD';
  els.tier.value = row.tierCd || 'TIER3';
  els.active.value = row.activeNodes ?? 0;
  els.standby.value = row.standbyNodes ?? 0;
  els.dr.value = row.drNodes ?? 0;
  els.status.value = row.statusCd || 'DISCOVERED';
  els.remark.value = row.remark || '';
  els.modal.hidden = false;
}
async function save() {
  const payload = {
    groupId: els.groupId.value.trim(),
    groupName: els.groupName.value.trim(),
    systemId: els.systemId.value.trim(),
    techRoleCd: els.techRole.value,
    envCd: els.env.value,
    tierCd: els.tier.value,
    statusCd: els.status.value,
    activeNodes: Number(els.active.value || 0),
    standbyNodes: Number(els.standby.value || 0),
    drNodes: Number(els.dr.value || 0),
    remark: els.remark.value.trim()
  };
  if (!payload.groupId || !payload.groupName) {
    alert('Group ID와 서버군명은 필수입니다.');
    return;
  }
  const sid = editMode === 'create' ? 'ifina3110C0' : 'ifina3110U0';
  const res = await InfraApi.postService(sid, payload, SCID);
  const dto = res.dto || {};
  if (dto.RSLT_CD && dto.RSLT_CD !== '0000') {
    alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG || ''}`);
    return;
  }
  els.modal.hidden = true;
  await search(editMode === 'create');
}
async function removeRow(row) {
  if (!confirm(`삭제할까요?\n${row.groupId}`)) return;
  const res = await InfraApi.postService('ifina3110D0', { groupIdList: [row.groupId] }, SCID);
  const dto = res.dto || {};
  if (dto.RSLT_CD && dto.RSLT_CD !== '0000') {
    alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG || ''}`);
    return;
  }
  await search(false);
}
document.getElementById('searchBtn').addEventListener('click', () => search(true));
document.getElementById('addBtn').addEventListener('click', openCreate);
document.getElementById('saveBtn').addEventListener('click', () => save().catch(console.error));
els.prev.addEventListener('click', () => { if (pageNo > 1) { pageNo -= 1; search(false); } });
els.next.addEventListener('click', () => { if (pageNo < totalPages) { pageNo += 1; search(false); } });
els.keyword.addEventListener('keydown', (e) => { if (e.key === 'Enter') search(true); });
els.resultBody.addEventListener('click', (e) => {
  const btn = e.target.closest('[data-action]');
  if (!btn) return;
  const row = rowsCache[Number(btn.closest('tr').dataset.index)];
  if (!row) return;
  if (btn.dataset.action === 'edit') openEdit(row);
  if (btn.dataset.action === 'delete') removeRow(row).catch(console.error);
});
els.modal.querySelectorAll('[data-close="true"]').forEach((el) => el.addEventListener('click', () => { els.modal.hidden = true; }));
search(true).catch(console.error);
