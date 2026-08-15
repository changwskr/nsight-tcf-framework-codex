const SCID = 'INF-940';
const $ = (id) => document.getElementById(id);

async function search() {
  $('resultMeta').textContent = '조회 중…';
  const tableId = parseInt($('tableId').value, 10);
  const res = await InfraApi.postService('ifina9400S0', { tableId }, SCID);
  const dto = res.dto || {};
  const cols = Array.isArray(dto.columns) ? dto.columns : [];
  const rows = Array.isArray(dto.rows) ? dto.rows : [];
  $('tableTitle').textContent = `표${dto.tableId ?? tableId} · ${dto.tableName || ''}`;
  $('rowCount').textContent = `${rows.length}`;
  $('tblHead').innerHTML = cols.length
    ? `<tr>${cols.map((c) => `<th>${InfraApi.escapeHtml(c)}</th>`).join('')}</tr>`
    : '';
  $('tblBody').innerHTML = rows.length
    ? rows.map((r) => `<tr>${cols.map((c) => {
      const v = r[c];
      return `<td>${v == null ? '' : InfraApi.escapeHtml(String(v))}</td>`;
    }).join('')}</tr>`).join('')
    : '<tr><td class="empty">없음</td></tr>';
  $('resultMeta').textContent =
    `HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ifina9400S0 · ${dto.RSLT_CD || ''}`;
}

async function exportFile(formatCd, all) {
  const body = all
    ? { allTablesYn: 'Y', formatCd: formatCd || 'XLSX' }
    : { tableId: parseInt($('tableId').value, 10), formatCd };
  const res = await InfraApi.postService('ifina9400E0', body, SCID);
  const dto = res.dto || {};
  if (dto.RSLT_CD && dto.RSLT_CD !== '0000') {
    alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG || ''}`);
    return;
  }
  if (dto.downloadUri) {
    window.open(dto.downloadUri, '_blank');
    const tables = Array.isArray(dto.exportedTableIds)
      ? dto.exportedTableIds.join(',')
      : (dto.tableId ?? '');
    $('resultMeta').textContent =
      `Export OK · ${dto.formatCd || ''} · ${dto.fileName || ''} · tables=[${tables}] · ${dto.rowCount ?? 0}행`;
  }
}

$('searchBtn').onclick = () => search().catch(console.error);
$('exportCsvBtn').onclick = () => exportFile('CSV', false).catch(console.error);
$('exportXlsxBtn').onclick = () => exportFile('XLSX', false).catch(console.error);
$('exportPdfBtn').onclick = () => exportFile('PDF', false).catch(console.error);
$('exportAllBtn').onclick = () => exportFile('XLSX', true).catch(console.error);
$('tableId').onchange = () => search().catch(console.error);
search().catch(console.error);
