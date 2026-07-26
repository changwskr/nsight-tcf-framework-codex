(function () {
  const STORAGE_KEY = 'tcf-cache.sample.commonCode.db';
  const CACHE_KEY = 'tcf-cache.sample.commonCode.ehcache';
  const ALL_GROUPS = '__ALL_GROUPS__';

  const defaultRows = [
    { codeGroup: 'CHANNEL_CODE', code: 'WEB', codeName: '웹 채널', sortOrder: 1, useYn: 'Y', description: '브라우저·모바일 웹' },
    { codeGroup: 'CHANNEL_CODE', code: 'MOBILE', codeName: '모바일 앱', sortOrder: 2, useYn: 'Y', description: '네이티브 앱' },
    { codeGroup: 'CHANNEL_CODE', code: 'BRANCH', codeName: '영업점', sortOrder: 3, useYn: 'Y', description: '창구 채널' },
    { codeGroup: 'SAMPLE_STATUS', code: 'ACTIVE', codeName: '사용', sortOrder: 1, useYn: 'Y', description: '샘플 활성' },
    { codeGroup: 'SAMPLE_STATUS', code: 'INACTIVE', codeName: '중지', sortOrder: 2, useYn: 'Y', description: '샘플 비활성' }
  ];

  let db = loadDb();
  let cache = loadCache();
  let logs = [];

  function uiPath(path) {
    if (window.OmAdmin && typeof OmAdmin.uiPath === 'function') {
      return OmAdmin.uiPath(path);
    }
    const prefix = location.pathname.startsWith('/ui/') ? '/ui' : '';
    return `${prefix}${path}`;
  }

  function loadDb() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) return JSON.parse(raw);
    } catch (_) { /* ignore */ }
    return defaultRows.slice();
  }

  function saveDb() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(db));
  }

  function loadCache() {
    try {
      const raw = localStorage.getItem(CACHE_KEY);
      if (raw) return JSON.parse(raw);
    } catch (_) { /* ignore */ }
    return {};
  }

  function saveCache() {
    localStorage.setItem(CACHE_KEY, JSON.stringify(cache));
  }

  function log(message, type) {
    const ts = new Date().toLocaleTimeString();
    logs.unshift(`[${ts}] ${type ? '[' + type + '] ' : ''}${message}`);
    logs = logs.slice(0, 40);
    const el = document.getElementById('eventLog');
    if (el) el.textContent = logs.join('\n');
  }

  function readForm() {
    return {
      codeGroup: document.getElementById('fGroup').value.trim(),
      code: document.getElementById('fCode').value.trim(),
      codeName: document.getElementById('fName').value.trim(),
      sortOrder: Number(document.getElementById('fSort').value || 0),
      useYn: document.getElementById('fUse').value,
      description: document.getElementById('fDesc').value.trim(),
      changeReason: 'tcf-cache sample'
    };
  }

  function clearForm() {
    ['fGroup', 'fCode', 'fName', 'fDesc'].forEach(id => { document.getElementById(id).value = ''; });
    document.getElementById('fSort').value = '0';
    document.getElementById('fUse').value = 'Y';
  }

  function queryDb(codeGroup) {
    return db
      .filter(r => r.codeGroup === codeGroup)
      .sort((a, b) => a.sortOrder - b.sortOrder || a.code.localeCompare(b.code));
  }

  function distinctGroups() {
    return [...new Set(db.map(r => r.codeGroup))].sort();
  }

  function cacheGet(key) {
    return Object.prototype.hasOwnProperty.call(cache, key) ? cache[key] : null;
  }

  function cachePut(key, value) {
    cache[key] = value;
    saveCache();
  }

  function cacheEvictGroup(codeGroup) {
    delete cache[codeGroup];
    delete cache[ALL_GROUPS];
    saveCache();
  }

  function loadByCodeGroup(codeGroup, forceMiss) {
    if (!codeGroup) {
      log('codeGroup을 입력하세요.', 'WARN');
      return [];
    }
    if (!forceMiss) {
      const hit = cacheGet(codeGroup);
      if (hit) {
        log(`EhCache HIT commonCode/${codeGroup} (${hit.length}건)`, 'HIT');
        renderCacheTable();
        renderDbTable(codeGroup);
        return hit;
      }
    }
    const rows = queryDb(codeGroup);
    cachePut(codeGroup, rows);
    log(`EhCache MISS commonCode/${codeGroup} -> DB 조회 ${rows.length}건`, 'MISS');
    renderCacheTable();
    renderDbTable(codeGroup);
    return rows;
  }

  function saveEntry(entry) {
    if (!entry.codeGroup || !entry.code) {
      log('코드그룹·코드는 필수입니다.', 'WARN');
      return;
    }
    const idx = db.findIndex(r => r.codeGroup === entry.codeGroup && r.code === entry.code);
    if (idx >= 0) db[idx] = entry;
    else db.push(entry);
    saveDb();
    cacheEvictGroup(entry.codeGroup);
    log(`DB 저장 + EhCache EVICT commonCode/${entry.codeGroup}`, 'EVICT');
    renderDbTable(entry.codeGroup);
    renderCacheTable();
    renderGroupSelect();
  }

  async function saveViaOm(entry) {
    if (!window.OmAdmin) {
      log('OmAdmin 없음 — 로컬 시뮬레이션 저장', 'WARN');
      saveEntry(entry);
      return;
    }
    await OmAdmin.mutate('commonCodeSave', entry, 'UPDATE');
    if (typeof OmAdmin.invalidateCommonCodeCache === 'function') {
      OmAdmin.invalidateCommonCodeCache(entry.codeGroup);
    }
    log(`OM.CommonCode.save + EhCache evict (${entry.codeGroup})`, 'EVICT');
  }

  async function loadViaOm(codeGroup) {
    if (!window.OmAdmin) {
      loadByCodeGroup(codeGroup, false);
      return;
    }
    const rows = await OmAdmin.loadCommonCodes(codeGroup, { useYn: 'Y', forceRefresh: true });
    log(`OM.CommonCode.inquiry -> EhCache warm (${codeGroup}, ${rows.length}건)`, 'MISS');
    renderDbTable(codeGroup);
  }

  async function loadOmCacheStatus() {
    if (!window.OmAdmin) {
      log('OmAdmin 없음 — 로컬 캐시 테이블만 표시', 'WARN');
      renderCacheTable();
      return;
    }
    const { body } = await OmAdmin.inquiry('cache', { cacheName: 'commonCode' });
    const tbody = document.getElementById('cacheBody');
    const rows = body.rows || [];
    document.getElementById('cacheStatus').textContent =
      `EhCache commonCode · ${rows.length} entry · ${body.fromEhCache ? 'EhCache' : 'DB'}`;
    if (!rows.length) {
      tbody.innerHTML = '<tr><td colspan="3" style="text-align:center;color:var(--muted)">캐시 비어 있음</td></tr>';
      return;
    }
    tbody.innerHTML = rows.map(r => `
      <tr>
        <td class="mono">${OmAdmin.field(r, 'cacheName')}</td>
        <td class="mono">${OmAdmin.field(r, 'cacheKey')}</td>
        <td>${OmAdmin.field(r, 'entryCount')}</td>
      </tr>`).join('');
    log(`OM.Cache.inquiry commonCode ${rows.length}건`, 'HIT');
  }

  function renderDbTable(codeGroup) {
    const tbody = document.getElementById('dbBody');
    const rows = codeGroup ? queryDb(codeGroup) : db.slice().sort((a, b) =>
      a.codeGroup.localeCompare(b.codeGroup) || a.sortOrder - b.sortOrder);
    document.getElementById('dbStatus').textContent = codeGroup
      ? `DB · ${codeGroup} · ${rows.length}건`
      : `DB · 전체 ${rows.length}건`;
    if (!rows.length) {
      tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;color:var(--muted)">데이터 없음</td></tr>';
      return;
    }
    tbody.innerHTML = rows.map(r => `
      <tr data-group="${r.codeGroup}" data-code="${r.code}">
        <td class="mono">${r.codeGroup}</td>
        <td class="mono">${r.code}</td>
        <td>${r.codeName}</td>
        <td>${r.sortOrder}</td>
        <td>${r.useYn}</td>
        <td>${r.description || ''}</td>
      </tr>`).join('');
    tbody.querySelectorAll('tr').forEach(tr => {
      tr.addEventListener('click', () => {
        document.getElementById('fGroup').value = tr.dataset.group;
        document.getElementById('fCode').value = tr.dataset.code;
        const row = db.find(r => r.codeGroup === tr.dataset.group && r.code === tr.dataset.code);
        if (!row) return;
        document.getElementById('fName').value = row.codeName;
        document.getElementById('fSort').value = row.sortOrder;
        document.getElementById('fUse').value = row.useYn;
        document.getElementById('fDesc').value = row.description || '';
        document.getElementById('qGroup').value = row.codeGroup;
      });
    });
  }

  function renderCacheTable() {
    const tbody = document.getElementById('cacheBody');
    const keys = Object.keys(cache).sort();
    document.getElementById('cacheStatus').textContent = `EhCache commonCode · ${keys.length} entry (로컬 시뮬)`;
    if (!keys.length) {
      tbody.innerHTML = '<tr><td colspan="3" style="text-align:center;color:var(--muted)">캐시 비어 있음</td></tr>';
      return;
    }
    tbody.innerHTML = keys.map(key => {
      const value = cache[key];
      const count = Array.isArray(value) ? value.length : 1;
      return `<tr>
        <td class="mono">commonCode</td>
        <td class="mono">${key}</td>
        <td>${count}건</td>
      </tr>`;
    }).join('');
  }

  function renderGroupSelect() {
    const select = document.getElementById('qGroup');
    const current = select.value;
    const groups = distinctGroups();
    select.innerHTML = groups.map(g => `<option value="${g}">${g}</option>`).join('');
    if (groups.includes(current)) select.value = current;
    else if (groups.length) select.value = groups[0];
  }

  function resetAll() {
    db = defaultRows.slice();
    cache = {};
    saveDb();
    saveCache();
    logs = [];
    log('샘플 DB·EhCache 초기화', 'EVICT');
    renderGroupSelect();
    renderDbTable(document.getElementById('qGroup').value);
    renderCacheTable();
    clearForm();
  }

  function bindHandlers() {
    document.getElementById('btnLoad').addEventListener('click', () => {
      loadByCodeGroup(document.getElementById('qGroup').value, false);
    });
    document.getElementById('btnForceMiss').addEventListener('click', () => {
      const group = document.getElementById('qGroup').value;
      cacheEvictGroup(group);
      log(`EhCache EVICT commonCode/${group}`, 'EVICT');
      renderCacheTable();
      loadByCodeGroup(group, true);
    });
    document.getElementById('btnSave').addEventListener('click', () => saveEntry(readForm()));
    document.getElementById('btnEvict').addEventListener('click', () => {
      const group = document.getElementById('qGroup').value;
      cacheEvictGroup(group);
      log(`EhCache EVICT commonCode/${group}`, 'EVICT');
      renderCacheTable();
    });
    document.getElementById('btnReset').addEventListener('click', resetAll);
    document.getElementById('qGroup').addEventListener('change', e => renderDbTable(e.target.value));

    const btnOmSave = document.getElementById('btnOmSave');
    const btnOmLoad = document.getElementById('btnOmLoad');
    const btnOmCache = document.getElementById('btnOmCache');
    if (btnOmSave) {
      btnOmSave.addEventListener('click', () => {
        saveViaOm(readForm()).catch(err => log(err.message, 'WARN'));
      });
    }
    if (btnOmLoad) {
      btnOmLoad.addEventListener('click', () => {
        loadViaOm(document.getElementById('qGroup').value).catch(err => log(err.message, 'WARN'));
      });
    }
    if (btnOmCache) {
      btnOmCache.addEventListener('click', () => {
        loadOmCacheStatus().catch(err => log(err.message, 'WARN'));
      });
    }
  }

  window.CacheCommonCodeSample = {
    init() {
      renderGroupSelect();
      renderDbTable(document.getElementById('qGroup').value);
      renderCacheTable();
      log('샘플 준비 — localStorage DB + EhCache(commonCode) 시뮬레이션');
      bindHandlers();

      const omPanel = document.getElementById('omPanel');
      if (omPanel && window.OmAdmin) {
        omPanel.hidden = false;
        log('OM Relay 연동 가능 — OM 저장·실제 EhCache 조회 버튼 사용', 'HIT');
      }
    },
    uiPath
  };
})();
