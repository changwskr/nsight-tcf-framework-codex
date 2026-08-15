const SCID = 'INF-160';
const $ = (id) => document.getElementById(id);

function isEvidence() {
  return ($('entityType').value || 'CHANGE').toUpperCase() === 'EVIDENCE';
}

function renderHead() {
  if (isEvidence()) {
    $('tableTitle').textContent = '증빙파일';
    $('resultHead').innerHTML = '<tr><th>Evidence ID</th><th>대상유형</th><th>대상ID</th><th>Gate</th><th>파일명</th><th>URI</th><th>업로더</th><th>시각</th></tr>';
  } else {
    $('tableTitle').textContent = '변경로그';
    $('resultHead').innerHTML = '<tr><th>Log ID</th><th>대상유형</th><th>대상ID</th><th>Action</th><th>변경자</th><th>시각</th><th>비고</th><th>After</th></tr>';
  }
}

function render(rows) {
  const list = rows || [];
  $('resultCount').textContent = `${list.length}건`;
  if (!list.length) {
    $('resultBody').innerHTML = `<tr><td colspan="8" class="empty">데이터 없음</td></tr>`;
    return;
  }
  if (isEvidence()) {
    $('resultBody').innerHTML = list.map((r) => `<tr>
      <td class="mono">${InfraApi.escapeHtml(r.evidenceId)}</td>
      <td>${InfraApi.escapeHtml(r.targetTypeCd)}</td>
      <td class="mono">${InfraApi.escapeHtml(r.targetId)}</td>
      <td>${InfraApi.escapeHtml(r.gateId)}</td>
      <td>${InfraApi.escapeHtml(r.fileName)}</td>
      <td class="mono">${r.fileUri ? `<a href="${InfraApi.escapeHtml(r.fileUri)}" target="_blank">${InfraApi.escapeHtml(r.fileUri)}</a>` : ''}</td>
      <td>${InfraApi.escapeHtml(r.uploadedBy)}</td>
      <td class="mono">${InfraApi.escapeHtml(r.uploadedAt)}</td>
    </tr>`).join('');
  } else {
    $('resultBody').innerHTML = list.map((r) => {
      const after = (r.afterJson || '').length > 48 ? (r.afterJson || '').slice(0, 48) + '…' : (r.afterJson || '');
      return `<tr>
      <td class="mono">${InfraApi.escapeHtml(r.logId)}</td>
      <td>${InfraApi.escapeHtml(r.targetTypeCd)}</td>
      <td class="mono">${InfraApi.escapeHtml(r.targetId)}</td>
      <td>${InfraApi.escapeHtml(r.actionCd)}</td>
      <td>${InfraApi.escapeHtml(r.changedBy)}</td>
      <td class="mono">${InfraApi.escapeHtml(r.changedAt)}</td>
      <td>${InfraApi.escapeHtml(r.remark)}</td>
      <td class="mono" title="${InfraApi.escapeHtml(r.afterJson || '')}">${InfraApi.escapeHtml(after)}</td>
    </tr>`;
    }).join('');
  }
}

async function search() {
  renderHead();
  $('resultMeta').textContent = '조회 중…';
  const res = await InfraApi.postService('ifina1600S0', {
    entityType: $('entityType').value || 'CHANGE',
    keyword: $('keyword').value.trim() || null,
    targetTypeCd: $('targetTypeCd').value.trim() || null,
    targetId: $('targetId').value.trim() || null,
    actionCd: isEvidence() ? null : ($('actionCd').value.trim() || null),
    pageNo: 1,
    pageSize: 50
  }, SCID);
  const dto = res.dto || {};
  render(Array.isArray(dto.rows) ? dto.rows : []);
  $('resultMeta').textContent =
    `HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ifina1600S0 · ${dto.entityType || ''} · 전체 ${dto.totalCount || 0}건`;
}

function readFileAsBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result || ''));
    reader.onerror = () => reject(reader.error || new Error('read failed'));
    reader.readAsDataURL(file);
  });
}

async function registerEvidence() {
  const targetId = $('regTargetId').value.trim();
  const fileInput = $('regFile');
  const file = fileInput && fileInput.files && fileInput.files[0] ? fileInput.files[0] : null;
  const fileName = file ? file.name : $('regFileName').value.trim();
  if (!targetId || !fileName) {
    $('registerMeta').textContent = '필수: 대상ID, 파일(또는 파일명)';
    return;
  }
  $('registerMeta').textContent = '등록 중…';
  const payload = {
    targetTypeCd: $('regTargetType').value.trim() || 'GROUP',
    targetId,
    gateId: $('regGateId').value.trim() || null,
    fileName,
    remark: $('regRemark').value.trim() || null
  };
  if (file) {
    if (file.size > 2 * 1024 * 1024) {
      $('registerMeta').textContent = '파일 크기 제한: 2MB';
      return;
    }
    payload.fileContentBase64 = await readFileAsBase64(file);
  }
  const res = await InfraApi.postService('ifina1600C0', payload, SCID);
  const dto = res.dto || {};
  $('registerMeta').textContent =
    `HTTP ${res.httpStatus} · ${dto.RSLT_CD || ''} · ${dto.RSLT_MSG || ''} · evidenceId=${dto.evidenceId || ''} · ${dto.fileUri || ''}`;
  if (dto.RSLT_CD === '0000') {
    $('entityType').value = 'EVIDENCE';
    $('targetId').value = targetId;
    await search();
  }
}

$('searchBtn').onclick = () => search().catch(console.error);
$('registerBtn').onclick = () => registerEvidence().catch(console.error);
$('entityType').onchange = () => search().catch(console.error);
$('keyword').onkeydown = (e) => { if (e.key === 'Enter') search().catch(console.error); };
$('regFile').onchange = () => {
  const f = $('regFile').files && $('regFile').files[0];
  if (f) $('regFileName').value = f.name;
};
search().catch(console.error);
