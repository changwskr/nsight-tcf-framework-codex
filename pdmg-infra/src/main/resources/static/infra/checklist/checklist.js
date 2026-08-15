const SCID = 'INF-910';
let rowsCache = [];

async function search() {
  const targetType = document.getElementById('targetType').value;
  const targetId = document.getElementById('targetId').value.trim();
  document.getElementById('resultMeta').textContent = '조회 중…';
  const res = await InfraApi.postService('ifina9100S0', { targetType, targetId }, SCID);
  const dto = res.dto || {};
  rowsCache = Array.isArray(dto.ifina9100S0DTOSub0) ? dto.ifina9100S0DTOSub0 : [];
  document.getElementById('resultCount').textContent = `${rowsCache.length}건`;
  document.getElementById('progressBadge').textContent = `${dto.progressPct ?? 0}% (${dto.checkedCount ?? 0}/${dto.totalItems ?? 0})`;
  document.getElementById('resultMeta').textContent = `HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ifina9100S0`;
  if (!rowsCache.length) {
    document.getElementById('resultBody').innerHTML = '<tr><td colspan="5" class="empty">데이터 없음</td></tr>';
    return;
  }
  document.getElementById('resultBody').innerHTML = rowsCache.map((r, i) => `
    <tr data-index="${i}">
      <td><input type="checkbox" data-check ${r.checkedYn === 'Y' ? 'checked' : ''}></td>
      <td class="mono">${InfraApi.escapeHtml(r.checklistId)}</td>
      <td>${InfraApi.escapeHtml(r.itemName)}</td>
      <td><span class="badge">${InfraApi.escapeHtml(r.severityCd)}</span></td>
      <td><input type="text" data-remark value="${InfraApi.escapeHtml(r.remark === '-' ? '' : (r.remark || ''))}" style="width:100%"></td>
    </tr>`).join('');
}

async function save() {
  const targetType = document.getElementById('targetType').value;
  const targetId = document.getElementById('targetId').value.trim();
  const items = [];
  document.querySelectorAll('#resultBody tr[data-index]').forEach((tr) => {
    const idx = Number(tr.dataset.index);
    const row = rowsCache[idx];
    if (!row) return;
    items.push({
      checklistId: row.checklistId,
      checkedYn: tr.querySelector('[data-check]').checked ? 'Y' : 'N',
      remark: tr.querySelector('[data-remark]').value.trim()
    });
  });
  const res = await InfraApi.postService('ifina9100U0', { targetType, targetId, items }, SCID);
  const dto = res.dto || {};
  if (dto.RSLT_CD && dto.RSLT_CD !== '0000') {
    alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG || ''}`);
    return;
  }
  await search();
}

document.getElementById('searchBtn').onclick = () => search().catch(console.error);
document.getElementById('saveBtn').onclick = () => save().catch(console.error);
search().catch(console.error);
