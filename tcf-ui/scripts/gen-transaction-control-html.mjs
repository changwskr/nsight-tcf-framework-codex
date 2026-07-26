import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const out = path.join(__dirname, '../src/main/resources/static/om/admin/transaction-control.html');

const L = {
  pageTitle: '\uAC70\uB798\uD1B5\uC81C \uAD00\uB9AC',
  globalOn: '\uC804\uCCB4 \uD5C8\uC6A9 \u00B7 \uCF1C\uAE30',
  globalOff: '\uC804\uCCB4 \uD5C8\uC6A9 \u00B7 \uAE34\uC0C1\uD0DC',
  globalAllow: '\uC804\uCCB4 \uAC70\uB798 \uD5C8\uC6A9',
  enableGlobal: '\uCF1C\uAE30',
  disableGlobal: '\uB044\uAE30',
  hint: '\uD1B5\uC81C\uC720\uD615\uC744 \uC120\uD0DD\uD558\uACE0 \uB300\uC0C1\uAC12 1\uAC74\uC744 \uC785\uB825\uD55C \uB92C \uCC28\uB2E8=Y \uC774\uBA74 \uD574\uB2F9 \uAC70\uB798\uB97C \uCC28\uB2E8\uD569\uB2C8\uB2E4. \uC804\uCCB4 \uD5C8\uC6A9\uC774 \uCF1C\uC838 \uC788\uC73C\uBA74 \uAC1C\uBCC4 \uCC28\uB2E8\uC774 \uBB34\uC2DC\uB429\uB2C8\uB2E4.',
  formNew: '\uADDC\uCE59 \uB4F1\uB85D',
  formEdit: '\uADDC\uCE59 \uC218\uC815',
  lblTarget: '\uD1B5\uC81C \uB300\uC0C1',
  lblBlock: '\uCC28\uB2E8',
  lblReason: '\uBCC0\uACBD \uC0AC\uC720',
  blockYes: '\uCC28\uB2E8',
  blockNo: '\uD5C8\uC6A9',
  targetPhSvc: 'SV.Sample.inquiry',
  targetPhIp: '10.10.10.10',
  targetPhUser: 'U123456',
  reasonPh: '5\uC790 \uC774\uC0C1',
  save: '\uADDC\uCE59 \uC800\uC7A5',
  cancel: '\uCDE8\uC18C',
  del: '\uC0AD\uC81C',
  filterType: '\uD1B5\uC81C\uC720\uD785',
  filterKw: '\uAC80\uC0C9',
  all: '\uC804\uCCB4',
  search: '\uC870\uD68C',
  listHead: '\uD1B5\uC81C \uADDC\uCE59 \uBAA9\uB85D',
  thType: '\uC720\uD615',
  thTarget: '\uB300\uC0C1',
  thStatus: '\uC0C1\uD0DC',
  loading: '\uC870\uD68C \uC911...',
  noRules: '\uB4F1\uB85D\uB41C \uADDC\uCE59\uC774 \uC5C6\uC2B5\uB2C8\uB2E4.',
  targetAll: '\uC804\uCCB4',
  statusBlock: '\uCC28\uB2E8',
  statusAllow: '\uD5C8\uC6A9',
  statusGlobalAllow: '\uC804\uCCB4 \uD5C8\uC6A9',
  noGlobalRule: '\uC804\uCCB4 \uD5C8\uC6A9 \uADDC\uCE59\uC774 \uC5C6\uC2B5\uB2C8\uB2E4.',
  confirmEnable: '\uC804\uCCB4 \uAC70\uB798 \uD5C8\uC6A9\uC744 \uCF1C\uAE4C\uC694?',
  confirmDisable: '\uC804\uCCB4 \uAC70\uB798 \uD5C8\uC6A9\uC744 \uB044\uAE4C\uC694?',
  confirmDel: '\uC120\uD0DD \uADDC\uCE59\uC744 \uC0AD\uC81C\uD560\uAE4C\uC694?',
  reasonEnable: '\uC804\uCCB4 \uAC70\uB798 \uD5C8\uC6A9 \uD65C\uC131\uD654',
  reasonDisable: '\uC804\uCCB4 \uAC70\uB798 \uD5C8\uC6A9 \uBE44\uD65C\uC131\uD654',
  enabled: '\uC804\uCCB4 \uAC70\uB798 \uD5C8\uC6A9\uC774 \uCF1C\uC84C\uC2B5\uB2C8\uB2E4.',
  disabled: '\uC804\uCCB4 \uAC70\uB798 \uD5C8\uC6A9\uC774 \uAEF8\uC84C\uC2B5\uB2C8\uB2E4.',
  saved: '\uC800\uC7A5 \uC644\uB8CC',
  deleted: '\uC0AD\uC81C \uC644\uB8CC',
  pickRow: '\uBAA9\uB85D\uC5D0\uC11C \uADDC\uCE59\uC744 \uC120\uD0DD\uD558\uC138\uC694.',
  needTarget: '\uD1B5\uC81C \uB300\uC0C1\uAC12\uC744 \uC785\uB825\uD558\uC138\uC694.',
  countFmt: '\uAC74',
  T: {
    BUSINESS: '\uC5C5\uBB34\uCF54\uB4DC',
    SERVICE: '\uC11C\uBE44\uC2A4ID',
    CHANNEL: '\uCC44\uB110',
    BRANCH: '\uBE0C\uB79C\uCE58',
    USER: '\uC0AC\uC6A9\uC790',
    IP: 'IP',
    GLOBAL: '\uC804\uCCB4'
  }
};

const html = `<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>OM ${L.pageTitle}</title>
  <link rel="stylesheet" href="/_shared/online.css">
  <link rel="stylesheet" href="/_shared/om-admin.css">
  <style>
    .txc-top { display:flex; flex-wrap:wrap; align-items:center; justify-content:space-between; gap:12px;
      padding:14px 18px; margin-bottom:12px; border-radius:12px; border:1px solid var(--border); background:var(--panel); }
    .txc-top.on { border-color:rgba(52,211,153,.5); background:linear-gradient(135deg,rgba(52,211,153,.07),transparent); }
    .txc-top.off { border-color:rgba(251,191,36,.4); }
    .txc-top-left { display:flex; align-items:center; gap:10px; flex-wrap:wrap; }
    .txc-dot { width:10px; height:10px; border-radius:50%; }
    .txc-dot.on { background:#34d399; box-shadow:0 0 8px rgba(52,211,153,.6); }
    .txc-dot.off { background:#fbbf24; }
    .txc-top-title { font-weight:600; font-size:.95rem; }
    .txc-hint { color:var(--muted); font-size:.82rem; margin:0 0 14px; line-height:1.5; }
    .txc-form { background:var(--panel); border:1px solid var(--border); border-radius:12px; padding:16px 18px; margin-bottom:14px; }
    .txc-form h3 { margin:0 0 12px; font-size:.92rem; color:var(--muted); font-weight:600; }
    .txc-pills { display:flex; flex-wrap:wrap; gap:6px; margin-bottom:14px; }
    .txc-pill { border:1px solid var(--border); background:transparent; color:inherit; border-radius:999px;
      padding:7px 13px; font-size:.8rem; cursor:pointer; transition:background .15s,border-color .15s; }
    .txc-pill:hover { border-color:rgba(129,140,248,.6); }
    .txc-pill.active { border-color:rgba(129,140,248,.8); background:rgba(129,140,248,.18); font-weight:600; }
    .txc-form-row { display:grid; grid-template-columns:1fr auto auto 1fr auto; gap:10px; align-items:end; }
    @media (max-width:960px) { .txc-form-row { grid-template-columns:1fr 1fr; } }
    .txc-form-actions { display:flex; gap:8px; flex-wrap:wrap; }
    .txc-block-toggle { display:flex; gap:4px; }
    .txc-block-toggle button { border:1px solid var(--border); background:transparent; color:inherit;
      border-radius:8px; padding:8px 12px; font-size:.82rem; cursor:pointer; min-width:52px; }
    .txc-block-toggle button.active-y { border-color:rgba(248,113,113,.6); background:rgba(248,113,113,.15); color:#fca5a5; font-weight:600; }
    .txc-block-toggle button.active-n { border-color:rgba(52,211,153,.6); background:rgba(52,211,153,.12); color:#6ee7b7; font-weight:600; }
    .txc-badge-block { color:#fca5a5; font-weight:600; }
    .txc-badge-allow { color:#6ee7b7; font-weight:600; }
    .txc-badge-global { color:#7dd3fc; font-weight:600; }
  </style>
</head>
<body>
  <script src="/_shared/om-admin.js?v=20260701a"><\/script>
  <script>
    const L = ${JSON.stringify(L)};
    const PAGE_SIZE = 20, GLOBAL = 'GLOBAL', WILDCARD = '*';
    const PILL_TYPES = ['BUSINESS','SERVICE','CHANNEL','BRANCH','USER','IP','GLOBAL'];
    let currentPage = 1, selectedRow = null, editMode = false, selectedType = 'SERVICE';
    let blockYn = 'Y', controlTypes = [], globalRow = null, listRows = [];
    let businessCodes = [], channelCodes = [], branchCodes = [];

    function typeLabel(code) {
      if (L.T[code]) return L.T[code];
      const row = controlTypes.find(r => OmAdmin.field(r, 'code') === code);
      return row ? OmAdmin.formatCodeLabel(row) : code;
    }

    function isGlobalRow(row) {
      return OmAdmin.field(row, 'controlType') === GLOBAL && OmAdmin.field(row, 'serviceId') === WILDCARD;
    }
    function isGlobalUnblock(row) { return isGlobalRow(row) && OmAdmin.field(row, 'blockYn') === 'N'; }

    function targetLabel(row) {
      const tv = OmAdmin.field(row, 'targetValue', '');
      return OmAdmin.field(row, 'controlType') === GLOBAL ? L.targetAll : (tv || '-');
    }

    function statusLabel(row) {
      const ct = OmAdmin.field(row, 'controlType');
      const yn = OmAdmin.field(row, 'blockYn', 'N');
      if (ct === GLOBAL && yn === 'N') return '<span class="txc-badge-global">' + L.statusGlobalAllow + '</span>';
      return yn === 'Y'
        ? '<span class="txc-badge-block">' + L.statusBlock + '</span>'
        : '<span class="txc-badge-allow">' + L.statusAllow + '</span>';
    }

    function setSelectValue(selectEl, value, codes) {
      OmAdmin.fillCodeSelect(selectEl, codes, { selected: value || '' });
      if (value && selectEl.value !== value) {
        const opt = document.createElement('option');
        opt.value = value; opt.textContent = value;
        selectEl.appendChild(opt); selectEl.value = value;
      }
    }

    async function loadCodeCombos() {
      [businessCodes, channelCodes, branchCodes] = await Promise.all([
        OmAdmin.loadCommonCodes('BUSINESS_CODE', { useYn: 'Y' }),
        OmAdmin.loadCommonCodes('CHANNEL_CODE', { useYn: 'Y' }),
        OmAdmin.loadCommonCodes('BRANCH_CODE', { useYn: 'Y' })
      ]);
    }

    function renderPills() {
      document.getElementById('typePills').innerHTML = PILL_TYPES.map(t =>
        '<button type="button" class="txc-pill' + (t === selectedType ? ' active' : '') + '" data-type="' + t + '">' + typeLabel(t) + '</button>'
      ).join('');
      document.querySelectorAll('.txc-pill').forEach(btn => {
        btn.addEventListener('click', () => {
          if (editMode) return;
          selectedType = btn.dataset.type;
          renderPills(); syncTargetField();
        });
      });
    }

    function syncBlockToggle() {
      document.getElementById('blockY').className = blockYn === 'Y' ? 'active-y' : '';
      document.getElementById('blockN').className = blockYn === 'N' ? 'active-n' : '';
    }

    function syncTargetField() {
      const wrap = document.getElementById('targetWrap');
      const input = document.getElementById('fTargetInput');
      const select = document.getElementById('fTargetSelect');
      const isGlobal = selectedType === GLOBAL;
      wrap.hidden = isGlobal;
      if (isGlobal) return;
      input.hidden = true; select.hidden = true;
      if (selectedType === 'BUSINESS') { select.hidden = false; setSelectValue(select, 'OM', businessCodes); }
      else if (selectedType === 'CHANNEL') { select.hidden = false; setSelectValue(select, 'WEBTOP', channelCodes); }
      else if (selectedType === 'BRANCH') { select.hidden = false; setSelectValue(select, '001234', branchCodes); }
      else {
        input.hidden = false;
        input.placeholder = selectedType === 'IP' ? L.targetPhIp : selectedType === 'USER' ? L.targetPhUser : L.targetPhSvc;
      }
    }

    function readTargetValue() {
      if (selectedType === GLOBAL) return WILDCARD;
      if (['BUSINESS','CHANNEL','BRANCH'].includes(selectedType)) return document.getElementById('fTargetSelect').value.trim();
      return document.getElementById('fTargetInput').value.trim();
    }

    function setTargetValue(type, value) {
      selectedType = type; renderPills(); syncTargetField();
      if (type === GLOBAL) return;
      if (['BUSINESS','CHANNEL','BRANCH'].includes(type)) {
        const codes = type === 'BUSINESS' ? businessCodes : type === 'CHANNEL' ? channelCodes : branchCodes;
        setSelectValue(document.getElementById('fTargetSelect'), value, codes);
      } else document.getElementById('fTargetInput').value = value || '';
    }

    function clearForm() {
      selectedRow = null; editMode = false;
      selectedType = 'SERVICE'; blockYn = 'Y';
      document.getElementById('formTitle').textContent = L.formNew;
      document.getElementById('fReason').value = '';
      document.getElementById('fTargetInput').value = '';
      document.getElementById('deleteBtn').hidden = true;
      document.querySelectorAll('#ctrlBody tr.om-row-selected').forEach(r => r.classList.remove('om-row-selected'));
      renderPills(); syncTargetField(); syncBlockToggle();
      unlockPills();
    }

    function fillForm(row) {
      selectedRow = row; editMode = true;
      document.getElementById('formTitle').textContent = L.formEdit;
      const ct = OmAdmin.field(row, 'controlType', 'SERVICE');
      blockYn = OmAdmin.field(row, 'blockYn', 'Y');
      setTargetValue(ct, OmAdmin.field(row, 'targetValue', ''));
      document.getElementById('fReason').value = '';
      document.getElementById('deleteBtn').hidden = false;
      syncBlockToggle();
      document.querySelectorAll('.txc-pill').forEach(p => p.disabled = true);
    }

    function unlockPills() {
      document.querySelectorAll('.txc-pill').forEach(p => p.disabled = false);
    }

    function formPayload() {
      const payload = {
        controlType: selectedType,
        targetValue: readTargetValue(),
        blockYn: blockYn,
        changeReason: document.getElementById('fReason').value.trim()
      };
      if (editMode && selectedRow) {
        ['serviceId','transactionCode','businessCode','serviceName','userId','channelId','branchId'].forEach(k => {
          payload[k] = OmAdmin.field(selectedRow, k, '');
        });
      }
      return payload;
    }

    function rowKey(row) {
      return ['serviceId','transactionCode','businessCode','serviceName','userId','channelId','branchId']
        .map(k => OmAdmin.field(row, k)).join('|');
    }

    function updateGlobalBar() {
      const active = globalRow && isGlobalUnblock(globalRow);
      const top = document.getElementById('globalBar');
      top.className = 'txc-top ' + (active ? 'on' : 'off');
      document.getElementById('globalDot').className = 'txc-dot ' + (active ? 'on' : 'off');
      document.getElementById('globalLabel').textContent = active ? L.globalOn : L.globalOff;
      document.getElementById('enableGlobalBtn').disabled = !!active;
      document.getElementById('disableGlobalBtn').disabled = !active;
    }

    async function refreshGlobalRow() {
      const { body } = await OmAdmin.inquiry('transactionControl', { pageNo: 1, pageSize: 10, controlType: GLOBAL });
      globalRow = (body.rows || []).find(r => isGlobalRow(r) && isGlobalUnblock(r)) || null;
      updateGlobalBar();
    }

    async function enableGlobalUnblock() {
      if (!confirm(L.confirmEnable)) return;
      const payload = { controlType: GLOBAL, targetValue: WILDCARD, blockYn: 'N', changeReason: L.reasonEnable };
      try {
        const { body } = await OmAdmin.inquiry('transactionControl', { pageNo: 1, pageSize: 10, controlType: GLOBAL });
        const existing = (body.rows || []).find(isGlobalRow);
        if (existing) {
          ['serviceId','transactionCode','businessCode','serviceName','userId','channelId','branchId'].forEach(k => payload[k] = OmAdmin.field(existing, k, WILDCARD));
          await OmAdmin.mutate('transactionControlUpdate', payload, 'UPDATE');
        } else await OmAdmin.mutate('transactionControlSave', payload, 'UPDATE');
        await refreshGlobalRow(); alert(L.enabled); await loadList(1);
      } catch (e) { alert(e.message); }
    }

    async function disableGlobalUnblock() {
      const { body } = await OmAdmin.inquiry('transactionControl', { pageNo: 1, pageSize: 10, controlType: GLOBAL });
      const existing = (body.rows || []).find(r => isGlobalRow(r) && isGlobalUnblock(r));
      if (!existing) { alert(L.noGlobalRule); return; }
      if (!confirm(L.confirmDisable)) return;
      const payload = {
        controlType: GLOBAL, targetValue: WILDCARD, blockYn: 'N', changeReason: L.reasonDisable,
        serviceId: WILDCARD, transactionCode: WILDCARD, businessCode: WILDCARD,
        serviceName: WILDCARD, userId: WILDCARD, channelId: WILDCARD, branchId: WILDCARD
      };
      try {
        await OmAdmin.mutate('transactionControlDelete', payload, 'DELETE');
        globalRow = null; await refreshGlobalRow(); alert(L.disabled);
        clearForm(); unlockPills(); await loadList(1);
      } catch (e) { alert(e.message); }
    }

    async function loadList(page) {
      currentPage = page;
      const v = id => document.getElementById(id).value.trim();
      const f = { pageNo: page, pageSize: PAGE_SIZE };
      if (v('filterControlType')) f.controlType = v('filterControlType');
      if (v('filterKw')) f.targetValue = v('filterKw');
      const statusEl = document.getElementById('listStatus');
      const tbody = document.getElementById('ctrlBody');
      tbody.innerHTML = '<tr><td colspan="3" class="om-empty">' + L.loading + '</td></tr>';
      const { body, relay } = await OmAdmin.inquiry('transactionControl', f);
      listRows = body.rows || [];
      const totalCount = body.totalCount ?? 0;
      statusEl.textContent = relay.elapsedMs + 'ms \\u00B7 ' + totalCount + L.countFmt;
      await refreshGlobalRow();
      if (!listRows.length) {
        tbody.innerHTML = '<tr><td colspan="3" class="om-empty">' + L.noRules + '</td></tr>';
      } else {
        tbody.innerHTML = listRows.map(r =>
          '<tr class="om-row-selectable" data-key="' + encodeURIComponent(rowKey(r)) + '">' +
          '<td>' + typeLabel(OmAdmin.field(r, 'controlType')) + '</td>' +
          '<td class="om-mono">' + targetLabel(r) + '</td>' +
          '<td>' + statusLabel(r) + '</td></tr>'
        ).join('');
        tbody.querySelectorAll('tr.om-row-selectable').forEach(tr => {
          tr.addEventListener('click', () => {
            tbody.querySelectorAll('tr.om-row-selected').forEach(r => r.classList.remove('om-row-selected'));
            tr.classList.add('om-row-selected');
            fillForm(listRows.find(r => rowKey(r) === decodeURIComponent(tr.dataset.key)) || {});
          });
        });
      }
      OmAdmin.renderPagination(document.getElementById('pagination'), body.pageNo || page, body.pageSize || PAGE_SIZE, totalCount,
        p => loadList(p).catch(e => OmAdmin.showErrorBanner(document.getElementById('pageRoot'), e.message)), true);
    }

    OmAdmin.initPage('transaction-control', L.pageTitle, async (el) => {
      el.id = 'pageRoot';
      controlTypes = await OmAdmin.loadCommonCodes('TX_CONTROL_TYPE', { useYn: 'Y' });
      await loadCodeCombos();

      el.innerHTML = \`
        <div class="txc-top off" id="globalBar">
          <div class="txc-top-left">
            <span class="txc-dot off" id="globalDot"></span>
            <span class="txc-top-title" id="globalLabel">\${L.globalAllow}</span>
          </div>
          <div class="txc-form-actions">
            <button class="btn-primary btn-sm" id="enableGlobalBtn" type="button">\${L.enableGlobal}</button>
            <button class="btn-secondary btn-sm" id="disableGlobalBtn" type="button">\${L.disableGlobal}</button>
          </div>
        </div>
        <p class="txc-hint">\${L.hint}</p>
        <div class="txc-form">
          <h3 id="formTitle">\${L.formNew}</h3>
          <div class="txc-pills" id="typePills"></div>
          <div class="txc-form-row">
            <div class="field" id="targetWrap">
              <label for="fTargetInput">\${L.lblTarget}</label>
              <input id="fTargetInput" type="text">
              <select id="fTargetSelect" hidden></select>
            </div>
            <div class="field">
              <span class="label">\${L.lblBlock}</span>
              <div class="txc-block-toggle">
                <button type="button" id="blockY">\${L.blockYes}</button>
                <button type="button" id="blockN">\${L.blockNo}</button>
              </div>
            </div>
            <div class="field field-wide">
              <label for="fReason">\${L.lblReason}</label>
              <input id="fReason" type="text" placeholder="\${L.reasonPh}">
            </div>
            <div class="txc-form-actions">
              <button class="btn-primary" id="saveBtn" type="button">\${L.save}</button>
              <button class="btn-secondary" id="cancelBtn" type="button">\${L.cancel}</button>
              <button class="btn-secondary" id="deleteBtn" type="button" hidden>\${L.del}</button>
            </div>
          </div>
        </div>
        <section class="om-filter-bar">
          <div class="field"><label for="filterKw">\${L.filterKw}</label><input id="filterKw" type="text" placeholder="\${L.targetPhSvc}"></div>
          <div class="field"><label for="filterControlType">\${L.filterType}</label>
            <select id="filterControlType"><option value="">\${L.all}</option></select>
          </div>
          <div class="actions" style="display:flex;gap:8px;align-items:end">
            <button class="btn-primary" id="searchBtn" type="button">\${L.search}</button>
          </div>
        </section>
        <div style="margin-bottom:8px;color:var(--muted);font-size:.85rem" id="listStatus">-</div>
        <div class="om-card">
          <div class="om-card-head">\${L.listHead}</div>
          <div class="om-card-body om-table-wrap">
            <table class="om-table"><thead><tr>
              <th>\${L.thType}</th><th>\${L.thTarget}</th><th>\${L.thStatus}</th>
            </tr></thead><tbody id="ctrlBody"><tr><td colspan="3" class="om-empty">-</td></tr></tbody></table>
            <div class="om-pagination" id="pagination" hidden></div>
          </div>
        </div>\`;

      PILL_TYPES.forEach(t => {
        const opt = document.createElement('option');
        opt.value = t; opt.textContent = typeLabel(t);
        document.getElementById('filterControlType').appendChild(opt);
      });

      renderPills(); syncTargetField(); syncBlockToggle();

      document.getElementById('blockY').addEventListener('click', () => { blockYn = 'Y'; syncBlockToggle(); });
      document.getElementById('blockN').addEventListener('click', () => { blockYn = 'N'; syncBlockToggle(); });
      document.getElementById('enableGlobalBtn').addEventListener('click', enableGlobalUnblock);
      document.getElementById('disableGlobalBtn').addEventListener('click', disableGlobalUnblock);
      document.getElementById('searchBtn').addEventListener('click', () => loadList(1).catch(e => OmAdmin.showErrorBanner(el, e.message)));
      document.getElementById('filterKw').addEventListener('keydown', e => { if (e.key === 'Enter') loadList(1); });
      document.getElementById('cancelBtn').addEventListener('click', () => { clearForm(); unlockPills(); });
      document.getElementById('saveBtn').addEventListener('click', async () => {
        if (selectedType !== GLOBAL && !readTargetValue()) { alert(L.needTarget); return; }
        try {
          if (editMode) await OmAdmin.mutate('transactionControlUpdate', formPayload(), 'UPDATE');
          else await OmAdmin.mutate('transactionControlSave', formPayload(), 'UPDATE');
          alert(L.saved); clearForm(); unlockPills(); await loadList(currentPage);
        } catch (e) { alert(e.message); }
      });
      document.getElementById('deleteBtn').addEventListener('click', async () => {
        if (!editMode) { alert(L.pickRow); return; }
        if (!confirm(L.confirmDel)) return;
        try {
          await OmAdmin.mutate('transactionControlDelete', formPayload(), 'DELETE');
          alert(L.deleted); clearForm(); unlockPills(); await loadList(currentPage);
        } catch (e) { alert(e.message); }
      });

      await refreshGlobalRow();
      await loadList(1).catch(e => OmAdmin.showErrorBanner(el, e.message));
    });
  </script>
</body>
</html>`;

fs.writeFileSync(out, html, 'utf8');
console.log('Wrote ' + out);
