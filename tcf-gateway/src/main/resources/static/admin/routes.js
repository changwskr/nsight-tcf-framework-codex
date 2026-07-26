const API = '/api/admin/routes';
let allRoutes = [];
let editMode = false;
let routeGroupCodes = [];
let businessCodes = [];

const $ = id => document.getElementById(id);

function envLabel(code) {
  if (code === 'PRD') return '운용';
  if (code === 'DEV') return '개발';
  if (code === 'LOCAL') return '로컬';
  return code;
}

function envClass(code) {
  return `env-${(code || '').toLowerCase()}`;
}

function assembleUrl(base, context, online) {
  const trim = v => (v || '').replace(/\/+$/, '');
  const ctx = context || '';
  const on = online || '/online';
  const normalizedCtx = ctx && !ctx.startsWith('/') ? `/${ctx}` : ctx;
  const normalizedOn = on.startsWith('/') ? on : `/${on}`;
  if (!normalizedCtx) {
    return trim(base) + normalizedOn;
  }
  return trim(base) + normalizedCtx + normalizedOn;
}

function updatePreview() {
  $('fTargetUrlPreview').textContent = assembleUrl(
    $('fTargetBaseUrl').value,
    $('fContextPath').value,
    $('fOnlinePath').value
  );
}

function toast(msg, type = 'success') {
  const el = $('toast');
  el.textContent = msg;
  el.className = `toast ${type}`;
  setTimeout(() => el.classList.add('hidden'), 2800);
}

function findGroup(code) {
  return routeGroupCodes.find(g => g.code === code);
}

function findBusiness(code) {
  return businessCodes.find(b => b.code === code);
}

function fillSelect(selectEl, items, options = {}) {
  const { includeAll = false, allLabel = '전체', valueKey = 'code', labelFn, selected = '' } = options;
  selectEl.innerHTML = '';
  if (includeAll) {
    const opt = document.createElement('option');
    opt.value = '';
    opt.textContent = allLabel;
    selectEl.appendChild(opt);
  }
  items.forEach(item => {
    const opt = document.createElement('option');
    opt.value = item[valueKey];
    opt.textContent = labelFn ? labelFn(item) : item[valueKey];
    selectEl.appendChild(opt);
  });
  setSelectValue(selectEl, selected);
}

function setSelectValue(selectEl, value) {
  selectEl.value = value || '';
  if (value && selectEl.value !== value) {
    const opt = document.createElement('option');
    opt.value = value;
    opt.textContent = `${value} (미등록)`;
    selectEl.appendChild(opt);
    selectEl.value = value;
  }
}

function initCodeSelects() {
  fillSelect($('filterGroup'), routeGroupCodes, {
    includeAll: true,
    labelFn: g => `${g.code} — ${g.name}`
  });
  fillSelect($('filterBusiness'), businessCodes, {
    includeAll: true,
    labelFn: b => `${b.code} — ${b.name}`
  });
  fillSelect($('fRouteGroupCode'), routeGroupCodes, {
    labelFn: g => `${g.code} — ${g.name}`
  });
  fillSelect($('fBusinessCode'), businessCodes, {
    labelFn: b => `${b.code} — ${b.name}`
  });
}

function applyRouteGroupSelection(code) {
  const group = findGroup(code);
  $('fRouteGroupName').value = group ? group.name : '';
}

function applyBusinessSelection(code) {
  const business = findBusiness(code);
  if (!business) {
    return;
  }
  $('fBusinessName').value = business.name;
  setSelectValue($('fRouteGroupCode'), business.routeGroupCode);
  applyRouteGroupSelection(business.routeGroupCode);
  $('fContextPath').value = business.contextPath ?? '';
  const healthPath = business.localHealthCheckPath || business.healthCheckPath;
  if (healthPath) {
    $('fHealthCheckPath').value = healthPath;
  }
  if (business.sortOrder != null) {
    $('fSortOrder').value = business.sortOrder;
  }
  if ($('fEnvCode').value === 'LOCAL' && business.localTargetBaseUrl) {
    $('fTargetBaseUrl').value = business.localTargetBaseUrl;
    if (business.code === 'JWT') {
      $('fOnlinePath').value = '/online';
    }
  }
  if (!editMode) {
    suggestRouteId();
  }
  updatePreview();
}

function suggestRouteId() {
  const env = $('fEnvCode').value;
  const business = $('fBusinessCode').value;
  if (env && business && !$('fRouteId').readOnly) {
    $('fRouteId').value = `${env}-GW-${business}`;
  }
}

async function loadMeta() {
  const res = await fetch(`${API}/meta`);
  const meta = await res.json();
  routeGroupCodes = meta.routeGroupCodes || [];
  businessCodes = meta.businessCodes || [];
  initCodeSelects();
  $('currentEnvBadge').textContent = `실행 환경: ${meta.currentEnvCode} (${envLabel(meta.currentEnvCode)})`;
  $('filterEnv').value = meta.currentEnvCode || 'LOCAL';
}

function applyFilters(rows) {
  const group = $('filterGroup').value;
  const business = $('filterBusiness').value;
  const useYn = $('filterUseYn').value;
  return rows.filter(r => {
    if (group && r.routeGroupCode !== group) return false;
    if (business && r.businessCode !== business) return false;
    if (useYn && r.useYn !== useYn) return false;
    return true;
  });
}

function renderTable(rows) {
  const body = $('routeBody');
  if (!rows.length) {
    body.innerHTML = '<tr><td colspan="9" class="empty">조회 결과가 없습니다.</td></tr>';
    return;
  }
  body.innerHTML = rows.map(r => `
    <tr>
      <td>${escapeHtml(r.routeId)}</td>
      <td class="${envClass(r.envCode)}">${r.envCode}<br><small>${envLabel(r.envCode)}</small></td>
      <td>${escapeHtml(r.routeGroupCode)}<br><small>${escapeHtml(r.routeGroupName || '')}</small></td>
      <td><strong>${escapeHtml(r.businessCode)}</strong><br><small>${escapeHtml(r.businessName || '')}</small></td>
      <td class="url-cell">${escapeHtml(r.targetUrl)}</td>
      <td>${r.connectTimeoutMs}/${r.readTimeoutMs}</td>
      <td><span class="tag ${r.useYn === 'Y' ? 'y' : 'n'}">${r.useYn}</span></td>
      <td>${r.sortOrder ?? ''}</td>
      <td class="row-actions">
        <button type="button" class="btn" data-edit="${escapeAttr(r.routeId)}">수정</button>
      </td>
    </tr>
  `).join('');
  body.querySelectorAll('[data-edit]').forEach(btn => {
    btn.addEventListener('click', () => openEdit(btn.dataset.edit));
  });
}

function escapeHtml(v) {
  return String(v ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function escapeAttr(v) {
  return escapeHtml(v).replace(/'/g, '&#39;');
}

async function search() {
  const env = $('filterEnv').value;
  $('routeBody').innerHTML = '<tr><td colspan="9" class="empty">조회 중...</td></tr>';
  try {
    const res = await fetch(`${API}?envCode=${encodeURIComponent(env)}`);
    if (!res.ok) throw new Error('조회 실패');
    allRoutes = await res.json();
    renderTable(applyFilters(allRoutes));
  } catch (e) {
    $('routeBody').innerHTML = `<tr><td colspan="9" class="empty">${escapeHtml(e.message)}</td></tr>`;
  }
}

function clearForm() {
  editMode = false;
  $('dialogTitle').textContent = '라우팅 등록';
  $('btnDelete').classList.add('hidden');
  $('fRouteId').readOnly = false;
  ['fRouteId', 'fTargetBaseUrl', 'fContextPath', 'fHealthCheckPath', 'fDescription', 'fSortOrder'].forEach(id => {
    $(id).value = '';
  });
  $('fRouteGroupName').value = '';
  $('fBusinessName').value = '';
  $('fEnvCode').value = $('filterEnv').value || 'LOCAL';
  $('fRouteGroupCode').selectedIndex = 0;
  $('fBusinessCode').selectedIndex = 0;
  $('fOnlinePath').value = '/online';
  $('fConnectTimeoutMs').value = '3000';
  $('fReadTimeoutMs').value = '5000';
  $('fUseYn').value = 'Y';
  updatePreview();
}

function fillForm(r) {
  editMode = true;
  $('dialogTitle').textContent = '라우팅 수정';
  $('btnDelete').classList.remove('hidden');
  $('fRouteId').value = r.routeId;
  $('fRouteId').readOnly = true;
  $('fEnvCode').value = r.envCode;
  setSelectValue($('fRouteGroupCode'), r.routeGroupCode);
  $('fRouteGroupName').value = r.routeGroupName || (findGroup(r.routeGroupCode)?.name ?? '');
  setSelectValue($('fBusinessCode'), r.businessCode);
  $('fBusinessName').value = r.businessName || (findBusiness(r.businessCode)?.name ?? '');
  $('fTargetBaseUrl').value = r.targetBaseUrl;
  $('fContextPath').value = r.contextPath;
  $('fOnlinePath').value = r.onlinePath;
  $('fHealthCheckPath').value = r.healthCheckPath || '';
  $('fConnectTimeoutMs').value = r.connectTimeoutMs;
  $('fReadTimeoutMs').value = r.readTimeoutMs;
  $('fUseYn').value = r.useYn;
  $('fSortOrder').value = r.sortOrder ?? '';
  $('fDescription').value = r.description || '';
  updatePreview();
}

function openNew() {
  clearForm();
  $('routeDialog').showModal();
}

function openEdit(routeId) {
  const row = allRoutes.find(r => r.routeId === routeId);
  if (!row) return;
  fillForm(row);
  $('routeDialog').showModal();
}

function readForm() {
  const groupCode = $('fRouteGroupCode').value;
  const businessCode = $('fBusinessCode').value;
  const group = findGroup(groupCode);
  const business = findBusiness(businessCode);
  return {
    routeId: $('fRouteId').value.trim(),
    envCode: $('fEnvCode').value,
    routeGroupCode: groupCode,
    routeGroupName: $('fRouteGroupName').value.trim() || group?.name || '',
    businessCode,
    businessName: $('fBusinessName').value.trim() || business?.name || '',
    targetBaseUrl: $('fTargetBaseUrl').value.trim(),
    contextPath: $('fContextPath').value.trim(),
    onlinePath: $('fOnlinePath').value.trim() || '/online',
    healthCheckPath: $('fHealthCheckPath').value.trim() || null,
    connectTimeoutMs: Number($('fConnectTimeoutMs').value || 3000),
    readTimeoutMs: Number($('fReadTimeoutMs').value || 5000),
    useYn: $('fUseYn').value,
    sortOrder: $('fSortOrder').value ? Number($('fSortOrder').value) : null,
    description: $('fDescription').value.trim() || null
  };
}

async function saveRoute(ev) {
  ev.preventDefault();
  const payload = readForm();
  if (!payload.routeId) {
    toast('라우팅 ID는 필수입니다.', 'error');
    return;
  }
  if (!payload.routeGroupCode || !payload.businessCode) {
    toast('업무그룹과 업무코드를 선택하세요.', 'error');
    return;
  }
  const url = editMode ? `${API}/${encodeURIComponent(payload.routeId)}` : API;
  const method = editMode ? 'PUT' : 'POST';
  try {
    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    if (!res.ok) throw new Error('저장 실패');
    $('routeDialog').close();
    toast('저장되었습니다.');
    await search();
  } catch (e) {
    toast(e.message, 'error');
  }
}

async function deleteRoute() {
  const routeId = $('fRouteId').value.trim();
  if (!routeId || !confirm(`라우팅 ${routeId} 을(를) 삭제하시겠습니까?`)) return;
  try {
    const res = await fetch(`${API}/${encodeURIComponent(routeId)}`, { method: 'DELETE' });
    if (!res.ok && res.status !== 204) throw new Error('삭제 실패');
    $('routeDialog').close();
    toast('삭제되었습니다.');
    await search();
  } catch (e) {
    toast(e.message, 'error');
  }
}

function bindEvents() {
  $('btnSearch').addEventListener('click', search);
  $('btnRefresh').addEventListener('click', search);
  $('btnNew').addEventListener('click', openNew);
  $('btnCloseDialog').addEventListener('click', () => $('routeDialog').close());
  $('btnCancel').addEventListener('click', () => $('routeDialog').close());
  $('btnDelete').addEventListener('click', deleteRoute);
  $('routeForm').addEventListener('submit', saveRoute);
  $('fRouteGroupCode').addEventListener('change', () => applyRouteGroupSelection($('fRouteGroupCode').value));
  $('fBusinessCode').addEventListener('change', () => applyBusinessSelection($('fBusinessCode').value));
  $('fEnvCode').addEventListener('change', suggestRouteId);
  ['fTargetBaseUrl', 'fContextPath', 'fOnlinePath'].forEach(id => {
    $(id).addEventListener('input', updatePreview);
  });
  ['filterGroup', 'filterBusiness', 'filterUseYn'].forEach(id => {
    $(id).addEventListener('change', () => renderTable(applyFilters(allRoutes)));
  });
}

async function init() {
  bindEvents();
  await loadMeta();
  await search();
}

init();
