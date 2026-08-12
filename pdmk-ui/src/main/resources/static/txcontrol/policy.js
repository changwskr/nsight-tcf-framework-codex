/*
 * 서비스별 온라인 거래통제 화면 (02.거래통제UI화면.md)
 */

const state = {
  list: [],
  current: null,
  allowBranches: [],
  denyBranches: [],
  allowIps: [],
  denyIps: [],
  allowUsers: [],
  denyUsers: []
};

const $ = (id) => document.getElementById(id);

function hdr(rms) {
  return {
    hdr_nhnis: {
      sys_comm: {
        std_gbl_id: crypto.randomUUID().replaceAll('-', ''),
        rms_svc_c: rms,
        scid: 'mkcoa6666',
        optr_eno: 'LOCAL',
        tr_trm_ipadr: '127.0.0.1',
        tr_sysid: 'PDMK-UI',
        sync_dsc: 'S',
        std_tgrm_rqr_rsp_dsc: 'Q',
        std_tgrm_lclc: 'KO',
        tr_dtm: '20260808120000',
        tr_brc: '10001',
        trmno: 'LOCAL01',
        trm_kdc: '01'
      }
    }
  };
}

function extractDto(parsed) {
  return parsed && parsed.dto && typeof parsed.dto === 'object' ? parsed.dto : parsed;
}

function text(v) { return v == null || v === '' ? '-' : String(v); }
function escapeHtml(v) {
  return text(v).replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');
}

async function relay(path, body) {
  const q = new URLSearchParams({ baseUrl: $('targetBaseUrl').value.trim() });
  const res = await fetch(`${path}?${q}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  return res.json();
}

async function search() {
  $('meta').innerHTML = '<span class="empty">조회 중...</span>';
  const result = await relay('/api/txcontrol/list', {
    ...hdr('mkcoa6666S0'),
    dto: {
      businessCode: $('qBusinessCode').value || null,
      serviceCode: $('qServiceCode').value.trim() || null,
      serviceName: $('qServiceName').value.trim() || null,
      status: $('qStatus').value || null,
      onlineForceYn: $('qForce').value || null,
      pageNo: 1,
      pageSize: 50
    }
  });
  let parsed;
  try { parsed = JSON.parse(result.responseBody); } catch (_e) { parsed = null; }
  const dto = extractDto(parsed) || {};
  state.list = Array.isArray(dto.mkcoa6666S0DTOSub0) ? dto.mkcoa6666S0DTOSub0 : [];
  const pick = $('servicePick');
  pick.innerHTML = state.list.length
      ? state.list.map((r) => `<option value="${escapeHtml(r.serviceCode)}">${escapeHtml(r.serviceCode)} — ${escapeHtml(r.serviceName)}</option>`).join('')
      : '<option value="">결과 없음</option>';
  $('meta').innerHTML = `<span class="badge ${result.httpStatus < 300 ? 'ok' : 'fail'}">HTTP ${result.httpStatus}</span> ${state.list.length}건`;
  if (state.list.length) {
    await loadDetail(state.list[0].serviceCode);
  }
}

async function loadDetail(serviceCode) {
  if (!serviceCode) return;
  const result = await relay('/api/txcontrol/detail', {
    ...hdr('mkcoa6666S1'),
    dto: { serviceCode }
  });
  let parsed;
  try { parsed = JSON.parse(result.responseBody); } catch (_e) { parsed = null; }
  const dto = extractDto(parsed);
  if (!dto || !dto.serviceCode) {
    PdmkErrorPopup.showSimple('상세 조회 실패', '오류');
    return;
  }
  state.current = dto;
  fillBasic(dto);
  fillPolicy(dto);
  await loadResults(serviceCode);
}

function fillBasic(dto) {
  $('bServiceCode').textContent = text(dto.serviceCode);
  $('bServiceName').textContent = text(dto.serviceName);
  $('bBusinessCode').textContent = text(dto.businessCode);
  const st = String(dto.status || 'NORMAL').toUpperCase();
  const cls = st === 'NORMAL' ? 'dot-on' : (st === 'STOP' || st === 'EMERGENCY' ? 'dot-stop' : 'dot-off');
  $('bStatus').innerHTML = `<span class="${cls}">● ${escapeHtml(st)}</span>`;
  $('bEnabled').textContent = text(dto.enabled);
  $('bForce').innerHTML = dto.onlineForceState === 'ON' || dto.onlineForceYn === 'Y'
      ? '<span class="dot-stop">● ON</span>' : '<span class="dot-off">● OFF</span>';
  $('bTimeout').textContent = `${Number(dto.timeoutMs || 0).toLocaleString()} ms`;
  $('bTps').textContent = `${text(dto.currentTps)} / ${text(dto.maxTps)}`;
  $('bConcurrent').textContent = `${text(dto.currentConcurrent)} / ${text(dto.maxConcurrent)}`;
  $('bBlocks').textContent = text(dto.recentBlockCount);
}

function defaultPolicy() {
  return {
    serviceCtrlUse: 'Y',
    branchMode: 'ALL',
    allowBranches: [],
    denyBranches: [],
    terminalTypes: { '01': 'Y', '02': 'Y', '03': 'Y', '04': 'N', '05': 'N' },
    unknownTerminalAction: 'BLOCK',
    ipCtrlUse: 'N',
    allowIps: [],
    denyIps: [],
    unknownIpAction: 'BLOCK',
    loopbackPolicy: 'DEV_ONLY',
    timeWindows: {
      weekday: { start: '0800', end: '2200', allow: 'Y' },
      saturday: { start: '0900', end: '1800', allow: 'Y' },
      sunday: { start: '0000', end: '0000', allow: 'N' },
      holiday: { start: '0000', end: '0000', allow: 'N' }
    },
    blockWindows: [],
    outOfHoursAction: 'BLOCK',
    userCtrlUse: 'N',
    userDefaultPolicy: 'ALLOW_AUTH',
    allowUsers: [],
    denyUsers: [],
    authMatchRequired: 'Y',
    onlineForce: { active: 'N', scope: 'SERVICE', mode: 'IMMEDIATE', reason: '', startDtm: '', endDtm: '' }
  };
}

function parsePolicy(dto) {
  if (dto.policyJson) {
    try { return { ...defaultPolicy(), ...JSON.parse(dto.policyJson) }; } catch (_e) { /* fallthrough */ }
  }
  const p = defaultPolicy();
  p.terminalTypes = {};
  String(dto.allowedTerminalTypes || '01,02,03').split(',').forEach((t) => {
    if (t.trim()) p.terminalTypes[t.trim()] = 'Y';
  });
  ['01', '02', '03', '04', '05'].forEach((k) => {
    if (!p.terminalTypes[k]) p.terminalTypes[k] = 'N';
  });
  return p;
}

function fillPolicy(dto) {
  const p = parsePolicy(dto);
  $('pServiceCtrlUse').value = p.serviceCtrlUse || 'Y';
  $('pStatus').value = dto.status || 'NORMAL';
  $('pEnabled').value = dto.enabled || 'Y';
  $('pTimeoutMs').value = dto.timeoutMs != null ? dto.timeoutMs : 3000;
  $('pMaxTps').value = dto.maxTps != null ? dto.maxTps : 100;
  $('pMaxConcurrent').value = dto.maxConcurrent != null ? dto.maxConcurrent : 50;
  $('pBranchMode').value = p.branchMode || 'ALL';
  state.allowBranches = [...(p.allowBranches || [])];
  state.denyBranches = [...(p.denyBranches || [])];
  renderBranchChips();
  const tt = p.terminalTypes || {};
  $('trm01').checked = tt['01'] === 'Y';
  $('trm02').checked = tt['02'] === 'Y';
  $('trm03').checked = tt['03'] === 'Y';
  $('trm04').checked = tt['04'] === 'Y';
  $('trm05').checked = tt['05'] === 'Y';
  $('pUnknownTrm').value = p.unknownTerminalAction || 'BLOCK';
  $('pIpCtrlUse').value = p.ipCtrlUse || 'N';
  $('pUnknownIp').value = p.unknownIpAction || 'BLOCK';
  $('pLoopback').value = p.loopbackPolicy || 'DEV_ONLY';
  state.allowIps = [...(p.allowIps || [])];
  state.denyIps = [...(p.denyIps || [])];
  renderIpChips();
  const tw = p.timeWindows || defaultPolicy().timeWindows;
  $('twWeekStart').value = tw.weekday?.start || '0800';
  $('twWeekEnd').value = tw.weekday?.end || '2200';
  $('twWeekAllow').value = tw.weekday?.allow || 'Y';
  $('twSatStart').value = tw.saturday?.start || '0900';
  $('twSatEnd').value = tw.saturday?.end || '1800';
  $('twSatAllow').value = tw.saturday?.allow || 'Y';
  $('twSunStart').value = tw.sunday?.start || '0000';
  $('twSunEnd').value = tw.sunday?.end || '0000';
  $('twSunAllow').value = tw.sunday?.allow || 'N';
  $('twHolStart').value = tw.holiday?.start || '0000';
  $('twHolEnd').value = tw.holiday?.end || '0000';
  $('twHolAllow').value = tw.holiday?.allow || 'N';
  $('pBlockWindows').value = JSON.stringify(p.blockWindows || [], null, 0);
  $('pOutOfHours').value = p.outOfHoursAction || 'BLOCK';
  $('pUserCtrlUse').value = p.userCtrlUse || 'N';
  $('pUserDefault').value = p.userDefaultPolicy || 'ALLOW_AUTH';
  $('pAuthMatch').value = p.authMatchRequired || 'Y';
  state.allowUsers = [...(p.allowUsers || [])];
  state.denyUsers = [...(p.denyUsers || [])];
  renderUsers();
  const force = p.onlineForce || {};
  $('pForceActive').value = force.active || dto.onlineForceYn || 'N';
  $('pForceScope').value = force.scope || 'SERVICE';
  $('pForceMode').value = force.mode || 'IMMEDIATE';
  $('pForceReason').value = force.reason || '';
  $('pForceStart').value = force.startDtm || '';
  $('pForceEnd').value = force.endDtm || '';
}

function renderBranchChips() {
  $('allowBranchChips').innerHTML = state.allowBranches.map((b, i) =>
      `<span class="chip">${escapeHtml(b.code)} ${escapeHtml(b.name || '')} Y <button type="button" data-rm-ab="${i}">×</button></span>`).join('') || '<span class="empty">없음</span>';
  $('denyBranchChips').innerHTML = state.denyBranches.map((b, i) =>
      `<span class="chip">${escapeHtml(b.code)} ${escapeHtml(b.name || '')} N <button type="button" data-rm-db="${i}">×</button></span>`).join('') || '<span class="empty">없음</span>';
}

function renderIpChips() {
  $('allowIpChips').innerHTML = state.allowIps.map((ip, i) =>
      `<span class="chip">${escapeHtml(ip)} Y <button type="button" data-rm-ai="${i}">×</button></span>`).join('') || '<span class="empty">없음</span>';
  $('denyIpChips').innerHTML = state.denyIps.map((ip, i) =>
      `<span class="chip">${escapeHtml(ip)} N <button type="button" data-rm-di="${i}">×</button></span>`).join('') || '<span class="empty">없음</span>';
}

function renderUsers() {
  const rows = [
    ...state.allowUsers.map((u) => ({ ...u, allow: 'Y' })),
    ...state.denyUsers.map((u) => ({ ...u, allow: 'N' }))
  ];
  if (!rows.length) {
    $('userBody').innerHTML = '<tr><td colspan="6" class="empty">없음</td></tr>';
    return;
  }
  $('userBody').innerHTML = rows.map((u, idx) => `
    <tr>
      <td class="mono">${escapeHtml(u.userId)}</td>
      <td>${escapeHtml(u.userName)}</td>
      <td>${escapeHtml(u.branch)}</td>
      <td>${escapeHtml(u.status || (u.allow === 'Y' ? '정상' : '차단'))}</td>
      <td>${escapeHtml(u.allow)}</td>
      <td><button type="button" class="btn-secondary btn-tiny" data-rm-user="${u.allow}:${u.userId}">삭제</button></td>
    </tr>`).join('');
}

function collectPolicyJson() {
  let blockWindows = [];
  try { blockWindows = JSON.parse($('pBlockWindows').value || '[]'); } catch (_e) { blockWindows = []; }
  return {
    serviceCtrlUse: $('pServiceCtrlUse').value,
    branchMode: $('pBranchMode').value,
    allowBranches: state.allowBranches,
    denyBranches: state.denyBranches,
    terminalTypes: {
      '01': $('trm01').checked ? 'Y' : 'N',
      '02': $('trm02').checked ? 'Y' : 'N',
      '03': $('trm03').checked ? 'Y' : 'N',
      '04': $('trm04').checked ? 'Y' : 'N',
      '05': $('trm05').checked ? 'Y' : 'N'
    },
    unknownTerminalAction: $('pUnknownTrm').value,
    ipCtrlUse: $('pIpCtrlUse').value,
    allowIps: state.allowIps,
    denyIps: state.denyIps,
    unknownIpAction: $('pUnknownIp').value,
    loopbackPolicy: $('pLoopback').value,
    timeWindows: {
      weekday: { start: $('twWeekStart').value, end: $('twWeekEnd').value, allow: $('twWeekAllow').value },
      saturday: { start: $('twSatStart').value, end: $('twSatEnd').value, allow: $('twSatAllow').value },
      sunday: { start: $('twSunStart').value, end: $('twSunEnd').value, allow: $('twSunAllow').value },
      holiday: { start: $('twHolStart').value, end: $('twHolEnd').value, allow: $('twHolAllow').value }
    },
    blockWindows,
    outOfHoursAction: $('pOutOfHours').value,
    userCtrlUse: $('pUserCtrlUse').value,
    userDefaultPolicy: $('pUserDefault').value,
    allowUsers: state.allowUsers,
    denyUsers: state.denyUsers,
    authMatchRequired: $('pAuthMatch').value,
    onlineForce: {
      active: $('pForceActive').value,
      scope: $('pForceScope').value,
      mode: $('pForceMode').value,
      reason: $('pForceReason').value.trim(),
      startDtm: $('pForceStart').value.trim(),
      endDtm: $('pForceEnd').value.trim()
    }
  };
}

function terminalCsv(policy) {
  return Object.entries(policy.terminalTypes || {})
      .filter(([, v]) => v === 'Y')
      .map(([k]) => k)
      .join(',') || '*';
}

function branchCsv(policy) {
  if (policy.branchMode === 'ALL') return '*';
  const codes = (policy.allowBranches || []).map((b) => b.code).filter(Boolean);
  return codes.length ? codes.join(',') : '*';
}

async function save() {
  if (!state.current) {
    PdmkErrorPopup.showSimple('먼저 서비스를 조회하세요.', '확인');
    return;
  }
  const policy = collectPolicyJson();
  const status = $('pStatus').value;
  const enabled = $('pEnabled').value;
  const reason = $('pForceReason').value.trim() || state.current.reason || '정책 저장';
  if ((status === 'STOP' || status === 'MAINTENANCE' || status === 'EMERGENCY' || enabled === 'N'
      || policy.onlineForce.active === 'Y') && !reason) {
    PdmkErrorPopup.showSimple('STOP/MAINT/EMERGENCY/강제통제 시 사유 필수', '검증');
    return;
  }
  const body = {
    ...hdr('mkcoa6666U0'),
    dto: {
      serviceCode: state.current.serviceCode,
      serviceName: state.current.serviceName,
      businessCode: state.current.businessCode,
      scid: state.current.scid,
      enabled,
      status,
      allowedSystemIds: state.current.allowedSystemIds || '*',
      allowedTerminalTypes: terminalCsv(policy),
      allowedBranches: branchCsv(policy),
      syncType: state.current.syncType || 'S',
      allowedStartTime: policy.timeWindows.weekday.start,
      allowedEndTime: policy.timeWindows.weekday.end,
      timeoutMs: Number($('pTimeoutMs').value) || 3000,
      maxTps: Number($('pMaxTps').value) || null,
      maxConcurrent: Number($('pMaxConcurrent').value) || null,
      duplicateWindowSec: state.current.duplicateWindowSec || 0,
      auditLevel: state.current.auditLevel || 'NORMAL',
      reason,
      onlineForceYn: policy.onlineForce.active === 'Y' || status === 'EMERGENCY' ? 'Y' : 'N',
      policyJson: JSON.stringify(policy)
    }
  };
  const result = await relay('/api/txcontrol/update', body);
  let parsed;
  try { parsed = JSON.parse(result.responseBody); } catch (_e) { parsed = null; }
  const dto = extractDto(parsed) || {};
  if (!(result.httpStatus < 300) || (dto.RSLT_CD && dto.RSLT_CD !== '0000')) {
    PdmkErrorPopup.showFromResponse(parsed, result.httpStatus, dto.RSLT_MSG || '저장 실패', result.responseBody);
    return;
  }
  alert('저장 완료');
  await loadDetail(state.current.serviceCode);
}

async function validatePolicy() {
  if (!state.current) return;
  await save();
  const result = await relay('/api/txcontrol/evaluate', {
    ...hdr('mkcoa6666E0'),
    dto: {
      stdGblId: crypto.randomUUID().replaceAll('-', ''),
      rmsSvcC: state.current.serviceCode,
      syncDsc: state.current.syncType || 'S',
      trSysid: 'PDMK-UI',
      stdTgrmRqrRspDsc: 'Q',
      stdTgrmLclc: 'KO',
      trTrmIpadr: '10.10.10.11',
      trDtm: '20260808120000',
      trBrc: (state.allowBranches[0] && state.allowBranches[0].code) || '10001',
      trmno: 'LOCAL01',
      trmKdc: '01',
      scid: state.current.scid || 'mkcoa6666',
      optrEno: (state.allowUsers[0] && state.allowUsers[0].userId) || 'E0000001'
    }
  });
  let parsed;
  try { parsed = JSON.parse(result.responseBody); } catch (_e) { parsed = null; }
  const dto = extractDto(parsed) || {};
  alert(`정책검증 결과: ${dto.controlResult || '-'}\n${dto.errorCode || ''} ${dto.message || ''}`);
  await loadResults(state.current.serviceCode);
}

async function loadResults(serviceCode) {
  const result = await relay('/api/txcontrol/results', {
    ...hdr('mkcoa6666S3'),
    dto: { serviceCode, pageNo: 1, pageSize: 20 }
  });
  let parsed;
  try { parsed = JSON.parse(result.responseBody); } catch (_e) { parsed = null; }
  const dto = extractDto(parsed) || {};
  const rows = Array.isArray(dto.mkcoa6666S3DTOSub0) ? dto.mkcoa6666S3DTOSub0 : [];
  if (!rows.length) {
    $('resultBody').innerHTML = '<tr><td colspan="9" class="empty">데이터 없음</td></tr>';
    return;
  }
  $('resultBody').innerHTML = rows.map((r) => `
    <tr>
      <td class="mono">${escapeHtml(r.regDtm)}</td>
      <td class="mono">${escapeHtml(r.stdGblId)}</td>
      <td class="mono">${escapeHtml(r.serviceCode)}</td>
      <td>${escapeHtml(r.optrEno)}</td>
      <td>${escapeHtml(r.trBrc)}</td>
      <td>${escapeHtml(r.trmKdc)}</td>
      <td>${escapeHtml(r.trTrmIpadr)}</td>
      <td><span class="badge ${String(r.controlResult).toUpperCase() === 'ALLOW' ? 'allow' : 'block'}">${escapeHtml(r.controlResult)}</span></td>
      <td>${escapeHtml(r.reason || r.errorCode || '-')}</td>
    </tr>`).join('');
}

async function setForce(on) {
  if (!state.current) return;
  const reason = on
      ? (prompt('강제통제 사유', $('pForceReason').value || '긴급 통제') || '')
      : (prompt('해제 사유', '강제통제 해제') || '강제통제 해제');
  if (!reason.trim()) return;
  $('pForceActive').value = on ? 'Y' : 'N';
  $('pForceReason').value = reason;
  if (on) {
    $('pStatus').value = 'EMERGENCY';
    $('pForceStart').value = new Date().toISOString().replace(/[-:TZ.]/g, '').slice(0, 14);
  } else if ($('pStatus').value === 'EMERGENCY') {
    $('pStatus').value = 'NORMAL';
  }
  await save();
}

function activateTab(name) {
  document.querySelectorAll('#policyTabs .tab-btn').forEach((btn) => {
    btn.classList.toggle('active', btn.dataset.tab === name);
  });
  document.querySelectorAll('.tab-panel').forEach((panel) => {
    panel.hidden = panel.dataset.panel !== name;
  });
  if (name === 'results' && state.current) {
    loadResults(state.current.serviceCode).catch(() => {});
  }
}

async function init() {
  const config = await (await fetch('/api/config')).json();
  const configured = (config.targetBaseUrl || '').trim();
  $('targetBaseUrl').value = configured.includes(':8081') ? configured : 'http://localhost:8081';
  $('targetInfo').textContent = `대상 pdmk-om: ${$('targetBaseUrl').value}`;

  document.querySelectorAll('#policyTabs .tab-btn').forEach((btn) => {
    btn.addEventListener('click', () => activateTab(btn.dataset.tab));
  });

  $('searchBtn').addEventListener('click', () => search().catch((e) => PdmkErrorPopup.showSimple(e.message)));
  $('resetBtn').addEventListener('click', () => {
    $('qBusinessCode').value = '';
    $('qServiceCode').value = '';
    $('qServiceName').value = '';
    $('qStatus').value = '';
    $('qForce').value = '';
  });
  $('servicePick').addEventListener('change', () => {
    loadDetail($('servicePick').value).catch((e) => PdmkErrorPopup.showSimple(e.message));
  });
  $('saveBtn').addEventListener('click', () => save().catch((e) => PdmkErrorPopup.showSimple(e.message)));
  $('validateBtn').addEventListener('click', () => validatePolicy().catch((e) => PdmkErrorPopup.showSimple(e.message)));
  $('reloadResultsBtn').addEventListener('click', () => {
    if (state.current) loadResults(state.current.serviceCode).catch((e) => PdmkErrorPopup.showSimple(e.message));
  });
  $('forceOnBtn').addEventListener('click', () => setForce(true).catch((e) => PdmkErrorPopup.showSimple(e.message)));
  $('forceOffBtn').addEventListener('click', () => setForce(false).catch((e) => PdmkErrorPopup.showSimple(e.message)));
  $('copyBtn').addEventListener('click', () => {
    if (!state.current) return;
    const json = JSON.stringify(collectPolicyJson(), null, 2);
    navigator.clipboard.writeText(json).then(() => alert('정책 JSON을 클립보드에 복사했습니다.'));
  });
  $('historyBtn').addEventListener('click', () => alert('변경이력은 P2 예정입니다. 현재는 최근 통제 결과를 사용하세요.'));

  $('allowBranchAdd').addEventListener('click', () => {
    const code = $('branchAddCode').value.trim();
    if (!code) return;
    state.allowBranches.push({ code, name: $('branchAddName').value.trim(), enabled: 'Y' });
    renderBranchChips();
  });
  $('denyBranchAdd').addEventListener('click', () => {
    const code = $('branchAddCode').value.trim();
    if (!code) return;
    state.denyBranches.push({ code, name: $('branchAddName').value.trim(), enabled: 'N' });
    renderBranchChips();
  });
  $('allowBranchChips').addEventListener('click', (e) => {
    const btn = e.target.closest('[data-rm-ab]');
    if (!btn) return;
    state.allowBranches.splice(Number(btn.dataset.rmAb), 1);
    renderBranchChips();
  });
  $('denyBranchChips').addEventListener('click', (e) => {
    const btn = e.target.closest('[data-rm-db]');
    if (!btn) return;
    state.denyBranches.splice(Number(btn.dataset.rmDb), 1);
    renderBranchChips();
  });
  $('allowIpAdd').addEventListener('click', () => {
    const ip = $('ipAdd').value.trim();
    if (!ip) return;
    state.allowIps.push(ip);
    renderIpChips();
  });
  $('denyIpAdd').addEventListener('click', () => {
    const ip = $('ipAdd').value.trim();
    if (!ip) return;
    state.denyIps.push(ip);
    renderIpChips();
  });
  $('allowIpChips').addEventListener('click', (e) => {
    const btn = e.target.closest('[data-rm-ai]');
    if (!btn) return;
    state.allowIps.splice(Number(btn.dataset.rmAi), 1);
    renderIpChips();
  });
  $('denyIpChips').addEventListener('click', (e) => {
    const btn = e.target.closest('[data-rm-di]');
    if (!btn) return;
    state.denyIps.splice(Number(btn.dataset.rmDi), 1);
    renderIpChips();
  });
  $('allowUserAdd').addEventListener('click', () => {
    const userId = $('userIdAdd').value.trim();
    if (!userId) return;
    state.allowUsers.push({
      userId,
      userName: $('userNameAdd').value.trim(),
      branch: $('userBranchAdd').value.trim(),
      status: 'NORMAL',
      allow: 'Y'
    });
    renderUsers();
  });
  $('denyUserAdd').addEventListener('click', () => {
    const userId = $('userIdAdd').value.trim();
    if (!userId) return;
    state.denyUsers.push({
      userId,
      userName: $('userNameAdd').value.trim(),
      branch: $('userBranchAdd').value.trim(),
      status: 'BLOCK',
      allow: 'N'
    });
    renderUsers();
  });
  $('userBody').addEventListener('click', (e) => {
    const btn = e.target.closest('[data-rm-user]');
    if (!btn) return;
    const [allow, userId] = btn.dataset.rmUser.split(':');
    if (allow === 'Y') state.allowUsers = state.allowUsers.filter((u) => u.userId !== userId);
    else state.denyUsers = state.denyUsers.filter((u) => u.userId !== userId);
    renderUsers();
  });

  await search();
}

init().catch((e) => PdmkErrorPopup.showSimple('초기화 실패: ' + e.message));
