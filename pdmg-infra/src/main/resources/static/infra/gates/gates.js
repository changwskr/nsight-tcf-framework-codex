const SCID = 'INF-920';
const $ = (id) => document.getElementById(id);
let gatesCache = [];

function badge(rc) {
  if (rc === 'PASS') return '<span class="badge badge--ok">PASS</span>';
  if (rc === 'CONDITIONAL') return '<span class="badge badge--warn">CONDITIONAL</span>';
  if (rc === 'FAIL') return '<span class="badge badge--fail">FAIL</span>';
  return `<span class="badge">${InfraApi.escapeHtml(rc || 'NOT_READY')}</span>`;
}

function readFileAsBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result || ''));
    reader.onerror = () => reject(reader.error || new Error('read failed'));
    reader.readAsDataURL(file);
  });
}

async function search() {
  $('resultMeta').textContent = '조회 중…';
  const res = await InfraApi.postService('ifina9200S0', {
    targetTypeCd: $('targetTypeCd').value,
    targetId: $('targetId').value.trim() || null
  }, SCID);
  const dto = res.dto || {};
  gatesCache = Array.isArray(dto.gates) ? dto.gates : [];
  $('kpiPass').textContent = dto.passCount != null ? dto.passCount : '-';
  $('kpiCond').textContent = dto.conditionalCount != null ? dto.conditionalCount : '-';
  $('kpiFail').textContent = dto.failCount != null ? dto.failCount : '-';
  $('kpiNr').textContent = dto.notReadyCount != null ? dto.notReadyCount : '-';
  $('hintLine').textContent = (dto.hints || []).join(' · ') || '';
  $('resultBody').innerHTML = gatesCache.length
    ? gatesCache.map((g, i) => `<tr data-index="${i}">
    <td class="mono">${InfraApi.escapeHtml(g.gateId)}</td>
    <td>${InfraApi.escapeHtml(g.nameKo)}</td>
    <td>${badge(g.resultCd)}</td>
    <td class="mono">${InfraApi.escapeHtml(g.evidence || '')}</td>
    <td>${InfraApi.escapeHtml(g.remark || '')}</td>
    <td><button class="btn-icon" data-action="judge" type="button">판정</button></td></tr>`).join('')
    : '<tr><td colspan="6" class="empty">없음</td></tr>';
  $('resultMeta').textContent = `HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ifina9200S0`;
}

function openJudge(g) {
  $('judgeTitle').textContent = `판정 · ${g.gateId}`;
  $('formGateId').value = g.gateId;
  $('formResultCd').value = g.resultCd && g.resultCd !== 'NOT_READY' ? g.resultCd : 'CONDITIONAL';
  $('formEvidence').value = g.evidence || '';
  $('formRemark').value = g.remark || '';
  $('formFile').value = '';
  $('uploadMeta').textContent = 'CONDITIONAL/FAIL 시 파일 업로드 후 판정하세요.';
  $('judgeModal').hidden = false;
}

async function uploadEvidenceIfNeeded() {
  const file = $('formFile').files && $('formFile').files[0] ? $('formFile').files[0] : null;
  if (!file) return null;
  if (file.size > 2 * 1024 * 1024) {
    throw new Error('파일 크기 제한: 2MB');
  }
  $('uploadMeta').textContent = '증적 업로드 중…';
  const res = await InfraApi.postService('ifina1600C0', {
    targetTypeCd: $('targetTypeCd').value,
    targetId: $('targetId').value.trim(),
    gateId: $('formGateId').value,
    fileName: file.name,
    fileContentBase64: await readFileAsBase64(file),
    remark: $('formRemark').value.trim() || null
  }, 'INF-160');
  const dto = res.dto || {};
  if (dto.RSLT_CD && dto.RSLT_CD !== '0000') {
    throw new Error(`${dto.RSLT_CD}: ${dto.RSLT_MSG || 'upload failed'}`);
  }
  $('uploadMeta').textContent = `업로드 OK · ${dto.evidenceId} · ${dto.fileUri || ''}`;
  if (!$('formEvidence').value.trim()) {
    $('formEvidence').value = dto.evidenceId || dto.fileUri || file.name;
  }
  return dto;
}

async function save() {
  try {
    await uploadEvidenceIfNeeded();
  } catch (err) {
    alert(String(err.message || err));
    return;
  }
  const payload = {
    gateId: $('formGateId').value,
    targetTypeCd: $('targetTypeCd').value,
    targetId: $('targetId').value.trim(),
    resultCd: $('formResultCd').value,
    evidence: $('formEvidence').value.trim(),
    remark: $('formRemark').value.trim()
  };
  const res = await InfraApi.postService('ifina9200U0', payload, SCID);
  const dto = res.dto || {};
  if (dto.RSLT_CD && dto.RSLT_CD !== '0000') {
    alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG || ''}`);
    return;
  }
  if (dto.warnings && dto.warnings.length) alert(dto.warnings.join('\n'));
  $('judgeModal').hidden = true;
  await search();
}

$('searchBtn').onclick = () => search().catch(console.error);
$('saveBtn').onclick = () => save().catch(console.error);
$('resultBody').onclick = (e) => {
  const btn = e.target.closest('[data-action="judge"]');
  if (!btn) return;
  const g = gatesCache[Number(btn.closest('tr').dataset.index)];
  if (g) openJudge(g);
};
$('judgeModal').querySelectorAll('[data-close="true"]').forEach((el) => {
  el.onclick = () => { $('judgeModal').hidden = true; };
});
search().catch(console.error);
