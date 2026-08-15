const SCID = 'INF-210';
let pageNo = 1, totalPages = 1, totalCount = 0, rowsCache = [], editMode = 'create';

const keywordEl = document.getElementById('keyword');
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
const formSystemIdEl = document.getElementById('formSystemId');
const formSystemNameEl = document.getElementById('formSystemName');
const formOwnerOrgEl = document.getElementById('formOwnerOrg');
const formStatusCdEl = document.getElementById('formStatusCd');
const formRemarkEl = document.getElementById('formRemark');

function pageSize() {
  const n = parseInt(pageSizeEl.value, 10);
  return Number.isNaN(n) || n <= 0 ? 10 : Math.min(n, 100);
}
function updatePager() {
  pageInfoEl.textContent = `${pageNo} / ${Math.max(totalPages, 1)} · 전체 ${totalCount}건`;
  prevPageBtn.disabled = pageNo <= 1;
  nextPageBtn.disabled = pageNo >= totalPages || totalPages <= 0;
}
function render(rows) {
  rowsCache = rows || [];
  resultCountEl.textContent = `${totalCount}건`;
  updatePager();
  if (!rowsCache.length) {
    resultBodyEl.innerHTML = '<tr><td colspan="7" class="empty">데이터 없음</td></tr>';
    return;
  }
  resultBodyEl.innerHTML = rowsCache.map((row, i) => `
    <tr data-index="${i}">
      <td class="mono">${InfraApi.escapeHtml(row.systemId)}</td>
      <td>${InfraApi.escapeHtml(row.systemName)}</td>
      <td>${InfraApi.escapeHtml(row.ownerOrg)}</td>
      <td>${InfraApi.statusBadge(row.statusCd)}</td>
      <td>${InfraApi.escapeHtml(row.remark)}</td>
      <td class="mono">${InfraApi.escapeHtml(row.regDtm)}</td>
      <td>
        <button class="btn-icon" type="button" data-action="edit">수정</button>
        <button class="btn-icon" type="button" data-action="delete">삭제</button>
      </td>
    </tr>`).join('');
}
async function search(reset) {
  if (reset) pageNo = 1;
  resultMetaEl.textContent = '조회 중…';
  const res = await InfraApi.postService('ifina2100S0', {
    keyword: keywordEl.value.trim() || null,
    statusCd: statusCdEl.value || null,
    pageNo, pageSize: pageSize()
  }, SCID);
  const dto = res.dto || {};
  const rows = Array.isArray(dto.ifina2100S0DTOSub0) ? dto.ifina2100S0DTOSub0 : [];
  totalCount = dto.totalCount != null ? Number(dto.totalCount) : rows.length;
  totalPages = dto.totalPages != null ? Number(dto.totalPages) : Math.max(1, Math.ceil(totalCount / pageSize()));
  pageNo = dto.pageNo != null ? Number(dto.pageNo) : pageNo;
  render(rows);
  resultMetaEl.textContent = `HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ifina2100S0`;
}
function openCreate() {
  editMode = 'create';
  editTitleEl.textContent = '시스템 등록';
  formSystemIdEl.disabled = false;
  formSystemIdEl.value = '';
  formSystemNameEl.value = '';
  formOwnerOrgEl.value = '';
  formStatusCdEl.value = 'CONFIRMED';
  formRemarkEl.value = '';
  editModalEl.hidden = false;
}
function openEdit(row) {
  editMode = 'edit';
  editTitleEl.textContent = '시스템 수정';
  formSystemIdEl.disabled = true;
  formSystemIdEl.value = row.systemId || '';
  formSystemNameEl.value = row.systemName || '';
  formOwnerOrgEl.value = row.ownerOrg || '';
  formStatusCdEl.value = row.statusCd || 'DISCOVERED';
  formRemarkEl.value = row.remark || '';
  editModalEl.hidden = false;
}
async function save() {
  const payload = {
    systemId: formSystemIdEl.value.trim(),
    systemName: formSystemNameEl.value.trim(),
    ownerOrg: formOwnerOrgEl.value.trim(),
    statusCd: formStatusCdEl.value,
    remark: formRemarkEl.value.trim()
  };
  if (!payload.systemId || !payload.systemName) {
    alert('System ID와 시스템명은 필수입니다.');
    return;
  }
  const sid = editMode === 'create' ? 'ifina2100C0' : 'ifina2100U0';
  const res = await InfraApi.postService(sid, payload, SCID);
  const dto = res.dto || {};
  if (dto.RSLT_CD && dto.RSLT_CD !== '0000') {
    alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG || ''}`);
    return;
  }
  editModalEl.hidden = true;
  await search(editMode === 'create');
}
async function removeRow(row) {
  if (!confirm(`삭제할까요?\n${row.systemId}`)) return;
  const res = await InfraApi.postService('ifina2100D0', { systemIdList: [row.systemId] }, SCID);
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
prevPageBtn.addEventListener('click', () => { if (pageNo > 1) { pageNo -= 1; search(false); } });
nextPageBtn.addEventListener('click', () => { if (pageNo < totalPages) { pageNo += 1; search(false); } });
keywordEl.addEventListener('keydown', (e) => { if (e.key === 'Enter') search(true); });
resultBodyEl.addEventListener('click', (e) => {
  const btn = e.target.closest('[data-action]');
  if (!btn) return;
  const row = rowsCache[Number(btn.closest('tr').dataset.index)];
  if (!row) return;
  if (btn.dataset.action === 'edit') openEdit(row);
  if (btn.dataset.action === 'delete') removeRow(row).catch(console.error);
});
editModalEl.querySelectorAll('[data-close="true"]').forEach((el) => el.addEventListener('click', () => { editModalEl.hidden = true; }));
search(true).catch(console.error);
