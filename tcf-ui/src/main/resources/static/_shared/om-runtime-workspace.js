/**
 * RTM 워크스페이스 — RTM-010 ~ RTM-100 단일 화면 탭
 */
window.OmRuntimeWorkspace = (function () {
  const FILTER_KEY = 'nsight.om.rtm010.filters';
  const FILTER040_KEY = 'nsight.om.rtm040.filters';
  const TREND_KEY = 'nsight.om.rtm020.trends';
  const TREND_MAX = 36;
  const SPARK = '▁▂▃▄▅▆▇';

  const FILTER060_KEY = 'nsight.om.rtm060.filters';

  const SCREENS = [
    { id: 'rtm010', label: 'RTM-010 통합 대시보드' },
    { id: 'rtm020', label: 'RTM-020 Tomcat 인스턴스' },
    { id: 'rtm030', label: 'RTM-030 WAR 자원' },
    { id: 'rtm040', label: 'RTM-040 실행 거래' },
    { id: 'rtm050', label: 'RTM-050 거래 추적' },
    { id: 'rtm060', label: 'RTM-060 Slow SQL' },
    { id: 'rtm100', label: 'RTM-100 원인 추적' }
  ];

  let state = {
    tab: 'rtm010',
    war: null,
    innerTab: 'pool',
    filters: {},
    rtm040: load040Filters(),
    selectedTxGuid: null,
    traceGuid: null,
    sqlRowKey: null,
    rtm060: load060Filters(),
    rtm060Inner: 'slow',
    autoEnabled: true,
    autoTimer: null,
    body: null,
    relay: null
  };

  function load060Filters() {
    try { return JSON.parse(sessionStorage.getItem(FILTER060_KEY) || '{}'); } catch (e) { return {}; }
  }

  function save060Filters(f) {
    sessionStorage.setItem(FILTER060_KEY, JSON.stringify(f));
  }

  function load040Filters() {
    try { return JSON.parse(sessionStorage.getItem(FILTER040_KEY) || '{}'); } catch (e) { return {}; }
  }

  function save040Filters(f) {
    sessionStorage.setItem(FILTER040_KEY, JSON.stringify(f));
  }

  function summaryClass(level) {
    if (level === 'CRITICAL' || level === 'critical') return 'critical';
    if (level === 'WARN' || level === 'warn') return 'warn';
    return 'normal';
  }

  function loadFilters() {
    try { return JSON.parse(sessionStorage.getItem(FILTER_KEY) || '{}'); } catch (e) { return {}; }
  }

  function saveFilters(f) {
    sessionStorage.setItem(FILTER_KEY, JSON.stringify(f));
  }

  function loadTrends() {
    try { return JSON.parse(localStorage.getItem(TREND_KEY) || '[]'); } catch (e) { return []; }
  }

  function saveTrends(rows) {
    localStorage.setItem(TREND_KEY, JSON.stringify(rows.slice(-TREND_MAX)));
  }

  function appendTrend(sample) {
    const rows = loadTrends();
    rows.push({ ...sample, at: Date.now() });
    saveTrends(rows);
    return rows;
  }

  function sparkline(values) {
    if (!values.length) return '-';
    const min = Math.min(...values);
    const max = Math.max(...values);
    const range = max - min || 1;
    return values.map(v => {
      const idx = Math.min(SPARK.length - 1, Math.floor((v - min) / range * (SPARK.length - 1)));
      return SPARK[idx];
    }).join('');
  }

  function sqlOrExternal(row) {
    const sql = OmAdmin.field(row, 'mapperSql');
    if (sql && sql !== '-') return sql;
    const ext = OmAdmin.field(row, 'externalSystemCode');
    if (ext && ext !== '-') return ext;
    return OmAdmin.field(row, 'sqlId') || '-';
  }

  function parseQuery() {
    const params = new URLSearchParams(window.location.search);
    return {
      tab: params.get('tab') || 'rtm010',
      war: params.get('war'),
      serviceId: params.get('serviceId'),
      guid: params.get('guid'),
      traceGuid: params.get('traceGuid') || params.get('guid'),
      sqlRowKey: params.get('sqlRowKey')
    };
  }

  function pushQuery() {
    const params = new URLSearchParams();
    params.set('tab', state.tab);
    if (state.war) params.set('war', state.war);
    if (state.rtm040.serviceId) params.set('serviceId', state.rtm040.serviceId);
    if (state.selectedTxGuid) params.set('guid', state.selectedTxGuid);
    if (state.traceGuid) params.set('traceGuid', state.traceGuid);
    if (state.sqlRowKey) params.set('sqlRowKey', state.sqlRowKey);
    const url = `${window.location.pathname}?${params.toString()}`;
    window.history.replaceState({}, '', url);
  }

  function navigate(tab, opts = {}) {
    state.tab = tab;
    if (opts.war !== undefined) state.war = opts.war;
    if (opts.serviceId !== undefined) {
      state.rtm040 = { ...state.rtm040, serviceId: opts.serviceId };
      save040Filters(state.rtm040);
    }
    if (opts.guid !== undefined) state.selectedTxGuid = opts.guid;
    if (opts.traceGuid !== undefined) state.traceGuid = opts.traceGuid;
    if (opts.sqlRowKey !== undefined) state.sqlRowKey = opts.sqlRowKey;
    if (opts.rtm060Inner !== undefined) state.rtm060Inner = opts.rtm060Inner;
    pushQuery();
    document.querySelectorAll('.rtm-ws-screen-tab').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.screen === state.tab);
    });
    document.querySelectorAll('.rtm-ws-panel').forEach(panel => {
      panel.classList.toggle('active', panel.id === 'ws-' + state.tab);
    });
    renderCurrent();
  }

  function filterInstances(rows, filters) {
    if (!rows || !rows.length) return [];
    return rows.filter(row => {
      if (filters.instance && filters.instance !== 'ALL') {
        const label = row.instanceLabel || row.businessCode || '';
        if (label !== filters.instance) return false;
      }
      if (filters.war && filters.war !== 'ALL') {
        if (row.businessCode !== filters.war) return false;
      }
      return true;
    });
  }

  function buildInstanceOptions(rows) {
    const instances = new Set(['ALL']);
    const wars = new Set(['ALL']);
    (rows || []).forEach(r => {
      if (r.instanceLabel) instances.add(r.instanceLabel);
      if (r.businessCode) wars.add(r.businessCode);
    });
    return { instances: [...instances], wars: [...wars] };
  }

  function isStale(checkedAt) {
    if (!checkedAt) return false;
    const t = Date.parse(String(checkedAt).replace(' ', 'T'));
    if (Number.isNaN(t)) return false;
    return Date.now() - t > 30000;
  }

  function findWarDetail(body, war) {
    const detail = body.warResourceDetail || {};
    const wars = detail.wars || [];
    if (!war) return wars[0] || null;
    return wars.find(w => w.war === war) || wars[0] || null;
  }

  function renderRtm010() {
    const body = state.body;
    const relay = state.relay;
    const dash = body.integratedDashboard || {};
    const summary = dash.summary || {};
    const questions = dash.diagnosticQuestions || [];
    const evidence = dash.evidence || [];
    const actions = dash.immediateActions || [];
    const links = dash.quickLinks || [];
    const allInstances = dash.instances || [];
    const instances = filterInstances(allInstances, state.filters);
    const freshness = dash.dataFreshness || {};
    const checkedAt = body.checkedAt || freshness.checkedAt;
    const stale = isStale(checkedAt);

    const set = (id, text) => { const el = document.getElementById(id); if (el) el.textContent = text; };
    set('ws010-lastCollected', `마지막 수집: ${checkedAt || '-'}`);
    const freshEl = document.getElementById('ws010-dataFreshness');
    if (freshEl) {
      freshEl.textContent = stale ? '데이터 지연 (30초 초과)' : '데이터 정상';
      freshEl.className = 'rtm010-freshness ' + (stale ? 'stale' : 'ok');
    }
    set('ws010-relayMeta', relay.elapsedMs != null ? `API ${relay.elapsedMs}ms` : '');

    const statusLevel = summary.overallStatus || body.overallStatus || 'NORMAL';
    const sumStatus = document.getElementById('ws010-sumStatus');
    if (sumStatus) {
      sumStatus.textContent = summary.overallStatusLabel || OmRuntime.statusLabel(statusLevel);
      sumStatus.className = 'value ' + summaryClass(statusLevel);
    }
    set('ws010-sumCause', summary.primaryCauseCode || body.primaryCauseCode || '-');
    const causeEl = document.getElementById('ws010-sumCause');
    if (causeEl && (summary.primaryCauseCode || body.primaryCauseCode)) {
      const code = summary.primaryCauseCode || body.primaryCauseCode;
      causeEl.innerHTML = `<a href="#" class="rtm-ws-nav-link" data-tab="rtm100">${code}</a>`;
    }
    set('ws010-sumConfidence', summary.confidence || '-');
    set('ws010-sumScope', summary.impactScope || '-');
      document.getElementById('ws010-metaService').innerHTML = (summary.dominantServiceId || body.dominantServiceId)
        ? `<a href="#" class="rtm-ws-nav-link" data-tab="rtm040" data-service-id="${summary.dominantServiceId || body.dominantServiceId}">${summary.dominantServiceId || body.dominantServiceId}</a>`
        : '-';
      document.getElementById('ws010-metaStep').textContent = summary.currentStep || '-';

    document.getElementById('ws010-questionList').innerHTML = questions.map(q => `
      <div class="rtm010-question ${summaryClass(q.level)}">
        <div class="q-title">${q.title || q.id}</div>
        <div class="q-answer">${q.answer || '-'}</div>
        ${q.href ? `<a href="#" class="rtm-ws-link" data-tab="rtm020">${q.label || '상세'}</a>` : ''}
      </div>`).join('') || '<div class="om-empty">진단 질문 없음</div>';

    document.getElementById('ws010-evidenceList').innerHTML = evidence.length
      ? evidence.map(line => `<li>${line}</li>`).join('')
      : '<li class="om-empty">근거 없음</li>';
    document.getElementById('ws010-actionList').innerHTML = actions.length
      ? actions.map((line, i) => `<li>${i + 1}. ${line}</li>`).join('')
      : '<li class="om-empty">조치 항목 없음</li>';

    document.getElementById('ws010-quickLinks').innerHTML = links.map(l => {
      let tab = null;
      let war = null;
      if ((l.href || '').includes('tab=rtm020')) tab = 'rtm020';
      if ((l.href || '').includes('tab=rtm030')) {
        tab = 'rtm030';
        const m = (l.href || '').match(/war=([^&]+)/);
        if (m) war = decodeURIComponent(m[1]);
      }
      if ((l.href || '').includes('tab=rtm040')) tab = 'rtm040';
      if ((l.href || '').includes('tab=rtm100')) tab = 'rtm100';
      if (tab) {
        return `<a href="#" class="rtm-ws-nav-link" data-tab="${tab}" ${war ? `data-war="${war}"` : ''}>${l.label}</a>`;
      }
      return `<a href="${OmAdmin.uiPath(l.href)}">${l.label}</a>`;
    }).join('');

    const instRows = instances.map(r => `
      <tr class="${r.status === 'CRITICAL' ? 'om-row-warn' : ''}">
        <td><a href="#" class="rtm-ws-nav-link" data-tab="rtm020">${OmAdmin.field(r, 'instanceLabel')}</a></td>
        <td>${OmAdmin.chipForHealth(r.statusLabel || r.status)}</td>
        <td>${r.cpuRatio != null ? r.cpuRatio + '%' : '-'}</td>
        <td>${r.threadRatio != null ? r.threadRatio + '%' : '-'}</td>
        <td>${r.gcRatio != null ? r.gcRatio + '%' : '-'}</td>
        <td>${r.riskWar ? `<a href="#" class="rtm-ws-nav-link" data-tab="rtm030" data-war="${r.riskWar}">${r.riskWar}</a>` : '-'}</td>
        <td class="om-mono">${OmAdmin.field(r, 'primaryCauseCode')}</td>
      </tr>`).join('');
    document.getElementById('ws010-instanceBody').innerHTML = instRows
      || '<tr><td colspan="7" class="om-empty">인스턴스 데이터 없음</td></tr>';

    const opts = buildInstanceOptions(allInstances);
    const instSel = document.getElementById('ws010-filterInstance');
    const warSel = document.getElementById('ws010-filterWar');
    if (instSel) {
      const cur = state.filters.instance || 'ALL';
      instSel.innerHTML = opts.instances.map(v =>
        `<option value="${v}" ${v === cur ? 'selected' : ''}>${v === 'ALL' ? '전체' : v}</option>`).join('');
    }
    if (warSel) {
      const cur = state.filters.war || 'ALL';
      warSel.innerHTML = opts.wars.map(v =>
        `<option value="${v}" ${v === cur ? 'selected' : ''}>${v === 'ALL' ? '전체' : v}</option>`).join('');
    }
  }

  function renderRtm020() {
    const detail = state.body.instanceDetail || {};
    const header = detail.header || {};
    const metrics = detail.summaryMetrics || [];
    const wars = detail.deployedWars || [];
    const threads = detail.threadRows || [];
    const jvm = detail.jvmTab || {};
    const gc = detail.gcTab || {};
    const deadlock = detail.deadlockTab || {};
    const alert = detail.deadlockAlert || {};

    const set = (id, text) => { const el = document.getElementById(id); if (el) el.textContent = text; };
    set('ws020-scopeNote', detail.scopeNote || '');
    set('ws020-instanceHeader', header.label || '-');

    const banner = document.getElementById('ws020-deadlockBanner');
    if (banner) {
      if (alert.detected) {
        banner.hidden = false;
        banner.innerHTML = `
          <strong>${alert.message || 'DEADLOCK 탐지'}</strong>
          <a href="#" class="btn-secondary rtm-ws-nav-link" data-tab="rtm020">${alert.linkLabel || 'Thread Dump 상세'}</a>`;
      } else {
        banner.hidden = true;
      }
    }

    document.getElementById('ws020-summaryMetrics').innerHTML = metrics.map(m => `
      <div class="rtm020-metric-row">
        <span class="label">${m.label}</span>
        <span class="value">${m.display || '-'}</span>
        <span class="badge">${m.levelSymbol || '○'} ${m.levelLabel || ''}</span>
      </div>`).join('');

    const cpu = Number(jvm.processCpuRatio) || 0;
    const busy = metrics.find(m => m.id === 'busyThread');
    const busyPct = busy ? parseFloat(String(busy.display).match(/(\d+(\.\d+)?)%/)?.[1] || 0) : 0;
    const heap = Number(jvm.heapRatio) || 0;
    const gcPause = Number(gc.gcTimeRatio) || 0;
    const trends = appendTrend({ cpu, busy: busyPct, heap, gcPause });
    document.getElementById('ws020-trendBlock').innerHTML = [
      { label: 'CPU', field: 'cpu' },
      { label: 'Busy', field: 'busy' },
      { label: 'Heap', field: 'heap' },
      { label: 'GC Pause', field: 'gcPause' }
    ].map(k => `
      <div class="rtm020-trend-row">
        <span>${k.label}</span>
        <span class="rtm020-spark">${sparkline(trends.map(t => t[k.field] || 0))}</span>
      </div>`).join('');

    const warHtml = wars.length ? wars.map(w => `
      <tr>
        <td><a href="#" class="rtm-ws-nav-link" data-tab="rtm030" data-war="${OmAdmin.field(w, 'war')}"><strong>${OmAdmin.field(w, 'war')}</strong></a></td>
        <td>${OmAdmin.chipForHealth(w.statusLabel || w.status)}</td>
        <td>${OmAdmin.field(w, 'activeTransactions')}</td>
        <td>${OmAdmin.field(w, 'slowTransactions')}</td>
        <td>${w.threadOccupancyPct != null ? w.threadOccupancyPct + '%' : '-'}</td>
        <td class="om-mono">${OmAdmin.field(w, 'poolDisplay')}</td>
      </tr>`).join('') : '<tr><td colspan="6" class="om-empty">배포 WAR 없음</td></tr>';
    document.getElementById('ws020-warBody').innerHTML = warHtml;
    const warBody2 = document.getElementById('ws020-warBody2');
    if (warBody2) warBody2.innerHTML = warHtml;

    document.getElementById('ws020-threadBody').innerHTML = threads.length ? threads.map(t => `
      <tr>
        <td class="om-mono">${OmAdmin.field(t, 'threadName')}</td>
        <td>${OmAdmin.field(t, 'threadId')}</td>
        <td>${OmAdmin.field(t, 'threadState')}</td>
        <td>${OmAdmin.field(t, 'businessCode')}</td>
        <td class="om-mono">${OmAdmin.field(t, 'serviceId')}</td>
        <td>${t.elapsedDisplay || OmAdmin.field(t, 'elapsedMs') + 'ms'}</td>
        <td>${OmAdmin.field(t, 'currentStep')}</td>
        <td class="om-mono">${sqlOrExternal(t)}</td>
        <td>${t.threadCpuPct != null ? t.threadCpuPct + '%' : '-'}</td>
      </tr>`).join('') : '<tr><td colspan="9" class="om-empty">TCF 거래 연계 Thread 없음</td></tr>';

    document.getElementById('ws020-jvmPanel').innerHTML = `
      <div class="rtm020-metric-grid">
        <div class="rtm020-metric-row"><span class="label">Process CPU</span><span class="value">${jvm.processCpuRatio ?? '-'}%</span></div>
        <div class="rtm020-metric-row"><span class="label">Heap Used</span><span class="value">${jvm.heapUsedMb ?? '-'} MB / ${jvm.heapMaxMb ?? '-'} MB (${jvm.heapRatio ?? '-'}%)</span></div>
        <div class="rtm020-metric-row"><span class="label">Metaspace</span><span class="value">${jvm.metaspaceMb ?? '-'} MB / ${jvm.metaspaceMaxMb || '-'} MB</span></div>
        <div class="rtm020-metric-row"><span class="label">Old Gen</span><span class="value">${jvm.oldGenRatio ?? '-'}%</span></div>
        <div class="rtm020-metric-row"><span class="label">Live Threads</span><span class="value">${jvm.liveThreadCount ?? '-'}</span></div>
      </div>`;

    document.getElementById('ws020-gcPanel').innerHTML = `
      <div class="rtm020-metric-grid">
        <div class="rtm020-metric-row"><span class="label">GC Time (1분)</span><span class="value">${gc.gcTimeLastMinuteMs ?? '-'} ms</span></div>
        <div class="rtm020-metric-row"><span class="label">GC Count (1분)</span><span class="value">${gc.gcCountLastMinute ?? '-'}</span></div>
        <div class="rtm020-metric-row"><span class="label">GC Time Ratio</span><span class="value">${gc.gcTimeRatio ?? '-'}%</span></div>
        <div class="rtm020-metric-row"><span class="label">Heap Ratio</span><span class="value">${gc.heapRatio ?? '-'}%</span></div>
      </div>`;

    document.getElementById('ws020-deadlockPanel').innerHTML = deadlock.detected
      ? `<p class="om-alert error">${deadlock.message}</p><p>탐지 Thread 수: <strong>${deadlock.count}</strong></p>`
      : `<p class="om-muted">Deadlock이 발견되지 않았습니다.</p>`;
  }

  function renderRtm030() {
    const detail = state.body.warResourceDetail || {};
    const warSel = document.getElementById('ws030-warSelect');
    const options = detail.warOptions || [];
    const selectedWar = state.war || detail.selectedWar || (options[0] && options[0].war);
    state.war = selectedWar;

    if (warSel) {
      warSel.innerHTML = options.map(o =>
        `<option value="${o.war}" ${o.war === selectedWar ? 'selected' : ''}>${o.label}</option>`).join('');
    }

    const war = findWarDetail(state.body, selectedWar);
    if (!war) {
      document.getElementById('ws030-content').innerHTML = '<p class="om-empty">WAR 데이터 없음</p>';
      return;
    }

    const verdict = war.verdict || {};
    document.getElementById('ws030-scopeNote').textContent = detail.scopeNote || '';
    document.getElementById('ws030-header').innerHTML = `
      <span>인스턴스 <strong>${war.instanceLabel || detail.instanceLabel || '-'}</strong></span>
      <span>WAR <strong>${war.war}</strong></span>
      <span>상태 ${OmAdmin.chipForHealth(war.statusLabel || war.status)}</span>`;

    document.getElementById('ws030-kpi').innerHTML = `
      <div class="rtm030-kpi"><label>Active 거래</label><div class="value">${war.activeTransactions ?? 0}</div></div>
      <div class="rtm030-kpi"><label>Slow 거래</label><div class="value">${war.slowTransactions ?? 0}</div></div>
      <div class="rtm030-kpi"><label>Timeout</label><div class="value">${war.timeoutCount ?? 0}</div></div>
      <div class="rtm030-kpi"><label>Thread 추정 점유</label><div class="value">${war.threadOccupancyPct != null ? war.threadOccupancyPct + '%' : '-'}</div></div>`;

    const pools = war.dbPools || [];
    document.getElementById('ws030-poolBody').innerHTML = pools.length ? pools.map(p => `
      <tr class="${p.exhausted ? 'om-row-critical' : ''}">
        <td class="om-mono">${OmAdmin.field(p, 'poolName')}</td>
        <td>${OmAdmin.field(p, 'maximum')}</td>
        <td>${OmAdmin.field(p, 'active')}</td>
        <td>${OmAdmin.field(p, 'idle')}</td>
        <td>${OmAdmin.field(p, 'pending')}</td>
        <td>${p.acquireP95Display || '-'}</td>
        <td>${OmAdmin.chipForHealth(p.statusLabel || p.status)}</td>
      </tr>`).join('') : '<tr><td colspan="7" class="om-empty">DB Pool 없음</td></tr>';

    document.getElementById('ws030-poolDetailBody').innerHTML = pools.length ? pools.map(p => `
      <tr>
        <td class="om-mono">${OmAdmin.field(p, 'poolName')}</td>
        <td>${OmAdmin.field(p, 'maximum')}</td>
        <td>${OmAdmin.field(p, 'active')}</td>
        <td>${OmAdmin.field(p, 'idle')}</td>
        <td>${OmAdmin.field(p, 'pending')}</td>
        <td>${p.usageRatio != null ? p.usageRatio + '%' : '-'}</td>
        <td>${p.acquireAvgDisplay || '-'}</td>
        <td>${p.acquireP95Display || '-'}</td>
        <td>${OmAdmin.field(p, 'connectionTimeoutCount')}</td>
        <td>${p.leakSuspected ? '<span class="om-chip warn">의심</span>' : '-'}</td>
        <td>${OmAdmin.chipForHealth(p.statusLabel || p.status)}</td>
      </tr>`).join('') : '<tr><td colspan="11" class="om-empty">DB Pool 없음</td></tr>';

    const services = war.serviceIds || [];
    document.getElementById('ws030-serviceBody').innerHTML = services.length ? services.map(s => `
      <tr>
        <td class="om-mono"><a href="#" class="rtm-ws-nav-link" data-tab="rtm040" data-service-id="${OmAdmin.field(s, 'serviceId')}">${OmAdmin.field(s, 'serviceId')}</a></td>
        <td>${OmAdmin.field(s, 'active')}</td>
        <td>${OmAdmin.field(s, 'slow')}</td>
        <td>${s.avgElapsedDisplay || '-'}</td>
        <td>${s.p95ElapsedDisplay || '-'}</td>
        <td>${s.dominantStepLabel || OmAdmin.field(s, 'dominantStep')}</td>
      </tr>`).join('') : '<tr><td colspan="6" class="om-empty">ServiceId 없음</td></tr>';

    const txs = war.transactions || [];
    document.getElementById('ws030-txBody').innerHTML = txs.length ? txs.map(t => `
      <tr>
        <td class="om-mono">${OmAdmin.field(t, 'serviceId')}</td>
        <td class="om-mono">${OmAdmin.field(t, 'guid')}</td>
        <td>${OmAdmin.field(t, 'currentStep')}</td>
        <td>${t.elapsedMs != null ? t.elapsedMs + 'ms' : '-'}</td>
        <td class="om-mono">${sqlOrExternal(t)}</td>
      </tr>`).join('') : '<tr><td colspan="5" class="om-empty">실행 중 거래 없음</td></tr>';

    const sqlRows = war.sqlRows || [];
    document.getElementById('ws030-sqlBody').innerHTML = sqlRows.length ? sqlRows.map(s => `
      <tr>
        <td class="om-mono">${OmAdmin.field(s, 'serviceId')}</td>
        <td class="om-mono">${OmAdmin.field(s, 'mapperSql') || OmAdmin.field(s, 'sqlId')}</td>
        <td>${s.elapsedMs != null ? s.elapsedMs + 'ms' : '-'}</td>
        <td>${OmAdmin.field(s, 'status')}</td>
      </tr>`).join('') : '<tr><td colspan="4" class="om-empty">Slow SQL 없음</td></tr>';

    const extRows = war.externalRows || [];
    document.getElementById('ws030-extBody').innerHTML = extRows.length ? extRows.map(t => `
      <tr>
        <td class="om-mono">${OmAdmin.field(t, 'serviceId')}</td>
        <td>${OmAdmin.field(t, 'externalSystemCode')}</td>
        <td>${t.elapsedMs != null ? t.elapsedMs + 'ms' : '-'}</td>
        <td class="om-mono">${OmAdmin.field(t, 'guid')}</td>
      </tr>`).join('') : '<tr><td colspan="4" class="om-empty">외부연계 대기 없음</td></tr>';

    const verdictEl = document.getElementById('ws030-verdict');
    verdictEl.className = 'rtm030-verdict' + (verdict.detected ? ' detected' : '');
    const actions = verdict.actions || [];
    verdictEl.innerHTML = `
      <div><strong>${verdict.detected ? '●' : '○'} ${verdict.causeCodeDisplay || verdict.causeCode || '판정'}</strong></div>
      <div class="om-muted" style="margin-top:6px">근거: ${verdict.evidence || '-'}</div>
      <div class="rtm030-verdict-actions">${actions.map(a =>
        `<a class="btn-secondary" href="${OmAdmin.uiPath(a.href)}">${a.label}</a>`).join('')}</div>`;

    switchInnerTab(state.innerTab);
  }

  function read040FiltersFromDom() {
    return {
      center: document.getElementById('ws040-filterCenter')?.value || 'ALL',
      instance: document.getElementById('ws040-filterInstance')?.value || 'ALL',
      war: document.getElementById('ws040-filterWar')?.value || 'ALL',
      serviceId: document.getElementById('ws040-filterServiceId')?.value || '',
      currentStep: document.getElementById('ws040-filterStep')?.value || 'ALL',
      elapsedPreset: document.getElementById('ws040-filterElapsed')?.value || '0',
      elapsedCustomMs: document.getElementById('ws040-filterElapsedCustom')?.value || '',
      status: document.getElementById('ws040-filterStatus')?.value || 'ALL',
      guidTraceId: document.getElementById('ws040-filterGuid')?.value || ''
    };
  }

  function apply040Filters(rows, f) {
    return rows.filter(r => {
      if (f.war && f.war !== 'ALL' && r.businessCode !== f.war) return false;
      if (f.instance && f.instance !== 'ALL' && r.instanceLabel !== f.instance) return false;
      if (f.center && f.center !== 'ALL' && r.center !== f.center) return false;
      if (f.serviceId && !String(r.serviceId || '').toLowerCase().includes(f.serviceId.toLowerCase())) return false;
      if (f.currentStep && f.currentStep !== 'ALL' && r.currentStep !== f.currentStep) return false;
      if (f.status && f.status !== 'ALL' && r.runtimeStatus !== f.status) return false;
      if (f.guidTraceId) {
        const q = f.guidTraceId.trim().toLowerCase();
        const guid = String(r.guid || '').toLowerCase();
        const trace = String(r.traceId || '').toLowerCase();
        if (!guid.includes(q) && !trace.includes(q)) return false;
      }
      let minMs = 0;
      if (f.elapsedPreset === '1000') minMs = 1000;
      else if (f.elapsedPreset === '3000') minMs = 3000;
      else if (f.elapsedPreset === 'custom') minMs = Number(f.elapsedCustomMs) || 0;
      if (minMs > 0 && Number(r.elapsedMs) < minMs) return false;
      return true;
    });
  }

  function txRowClass(health) {
    if (health === 'CRITICAL') return 'om-row-tx-critical';
    if (health === 'WARN') return 'om-row-tx-warn';
    return '';
  }

  function renderSelectedTx(row) {
    const panel = document.getElementById('ws040-selected');
    if (!panel) return;
    if (!row) {
      panel.innerHTML = '<p class="om-muted">목록에서 거래를 선택하세요.</p>';
      return;
    }
    panel.innerHTML = `
      <div>선택 거래: GUID <strong class="om-mono">${row.guid || '-'}</strong> / Thread <strong class="om-mono">${row.threadName || '-'}</strong></div>
      <div class="rtm040-selected-actions">
        <a href="#" class="btn-secondary rtm-ws-nav-link" data-tab="rtm050" data-trace-guid="${row.guid || ''}">거래 전체 추적</a>
        <a href="#" class="btn-secondary rtm-ws-nav-link" data-tab="rtm060" data-service-id="${row.serviceId || ''}">관련 SQL</a>
        <a href="#" class="btn-secondary rtm-ws-nav-link" data-tab="rtm040" data-service-id="${row.serviceId || ''}">동일 ServiceId 거래</a>
        <a class="btn-secondary" href="${OmAdmin.uiPath('/om/admin/runtime-cause-analysis.html')}">장애보고서 추가</a>
      </div>`;
  }

  function renderRtm040() {
    const screen = state.body.activeTransactionScreen || {};
    const allRows = screen.rows || [];
    const summary = screen.summary || {};
    const opts = screen.filterOptions || {};

    const f = { ...state.rtm040, ...read040FiltersFromDom() };
    state.rtm040 = f;
    save040Filters(f);

    const warSel = document.getElementById('ws040-filterWar');
    const instSel = document.getElementById('ws040-filterInstance');
    if (warSel) {
      const wars = opts.wars || ['ALL'];
      warSel.innerHTML = wars.map(w => `<option value="${w}">${w === 'ALL' ? '전체' : w}</option>`).join('');
      warSel.value = f.war || 'ALL';
    }
    if (instSel) {
      const instances = opts.instances || ['ALL'];
      instSel.innerHTML = instances.map(v => `<option value="${v}">${v === 'ALL' ? '전체' : v}</option>`).join('');
      instSel.value = f.instance || 'ALL';
    }
    if (document.getElementById('ws040-filterCenter')) document.getElementById('ws040-filterCenter').value = f.center || 'ALL';
    if (document.getElementById('ws040-filterServiceId')) document.getElementById('ws040-filterServiceId').value = f.serviceId || '';
    if (document.getElementById('ws040-filterStep')) document.getElementById('ws040-filterStep').value = f.currentStep || 'ALL';
    if (document.getElementById('ws040-filterElapsed')) document.getElementById('ws040-filterElapsed').value = f.elapsedPreset || '0';
    if (document.getElementById('ws040-filterElapsedCustom')) document.getElementById('ws040-filterElapsedCustom').value = f.elapsedCustomMs || '';
    if (document.getElementById('ws040-filterStatus')) document.getElementById('ws040-filterStatus').value = f.status || 'ALL';
    if (document.getElementById('ws040-filterGuid')) document.getElementById('ws040-filterGuid').value = f.guidTraceId || '';

    const customWrap = document.getElementById('ws040-elapsedCustomWrap');
    if (customWrap) customWrap.style.display = f.elapsedPreset === 'custom' ? '' : 'none';

    const filtered = apply040Filters(allRows, f);
    document.getElementById('ws040-kpiBar').innerHTML = `
      <span>현재 실행<strong>${summary.runningCount ?? filtered.length}</strong>건</span>
      <span>Slow<strong>${summary.slowCount ?? 0}</strong>건</span>
      <span>Timeout<strong>${summary.timeoutCount ?? 0}</strong>건</span>
      <span class="om-muted" style="margin-left:auto">표시 ${filtered.length}건</span>`;

    document.getElementById('ws040-txBody').innerHTML = filtered.length ? filtered.map(r => `
      <tr class="${txRowClass(r.healthStatus)} ${state.selectedTxGuid === r.guid ? 'rtm040-row-selected' : ''}" data-guid="${r.guid || ''}">
        <td>${OmAdmin.chipForHealth(r.healthStatusLabel || r.healthStatus)}</td>
        <td>${OmAdmin.field(r, 'businessCode')}</td>
        <td class="om-mono">${OmAdmin.field(r, 'serviceId')}</td>
        <td>${r.elapsedDisplay || '-'}</td>
        <td>${r.stepLabel || OmAdmin.field(r, 'currentStep')}</td>
        <td><button type="button" class="btn-secondary btn-sm ws040-select-btn" data-guid="${r.guid || ''}">보기</button></td>
      </tr>`).join('') : '<tr><td colspan="6" class="om-empty">조건에 맞는 실행 거래 없음</td></tr>';

    const slowSvc = screen.slowServiceIds || [];
    document.getElementById('ws040-slowSvcBody').innerHTML = slowSvc.length ? slowSvc.map(s => `
      <tr>
        <td class="om-mono"><a href="#" class="rtm-ws-nav-link" data-tab="rtm040" data-service-id="${OmAdmin.field(s, 'serviceId')}">${OmAdmin.field(s, 'serviceId')}</a></td>
        <td>${OmAdmin.field(s, 'activeSlowCount')}</td>
        <td>${OmAdmin.field(s, 'recentSlowCount')}</td>
        <td>${s.maxElapsedDisplay || '-'}</td>
      </tr>`).join('') : '<tr><td colspan="4" class="om-empty">Slow ServiceId 없음</td></tr>';

    const detailRows = filtered;
    document.getElementById('ws040-detailBody').innerHTML = detailRows.length ? detailRows.map(r => `
      <tr class="${txRowClass(r.healthStatus)}">
        <td>${OmAdmin.chipForHealth(r.healthStatusLabel || r.healthStatus)}</td>
        <td>${OmAdmin.field(r, 'businessCode')}</td>
        <td class="om-mono">${OmAdmin.field(r, 'serviceId')}</td>
        <td class="om-mono">${OmAdmin.field(r, 'guid')}</td>
        <td class="om-mono">${OmAdmin.field(r, 'traceId')}</td>
        <td>${r.startTimeDisplay || '-'}</td>
        <td>${r.elapsedDisplay || '-'}</td>
        <td>${r.stepLabel || '-'}</td>
        <td class="om-mono">${OmAdmin.field(r, 'threadName')}</td>
        <td class="om-mono">${OmAdmin.field(r, 'sqlOrExternal')}</td>
        <td>${r.timeoutSec != null ? r.timeoutSec + '초' : '-'}</td>
        <td>${r.timeoutRemainingDisplay || '-'}</td>
      </tr>`).join('') : '<tr><td colspan="12" class="om-empty">상세 데이터 없음</td></tr>';

    let selected = filtered.find(r => r.guid === state.selectedTxGuid);
    if (!selected && filtered.length) {
      selected = filtered[0];
      state.selectedTxGuid = selected.guid;
    }
    renderSelectedTx(selected || null);
  }

  function renderRtm050() {
    const screen = state.body.transactionTraceScreen || {};
    const trace = screen.trace || {};
    const options = screen.guidOptions || [];

    const sel = document.getElementById('ws050-guidSelect');
    if (sel) {
      sel.innerHTML = options.length
        ? options.map(o => `<option value="${OmAdmin.field(o, 'guid')}">${OmAdmin.field(o, 'guid')} · ${OmAdmin.field(o, 'serviceId')}</option>`).join('')
        : '<option value="">실행 거래 없음</option>';
      const cur = state.traceGuid || screen.selectedGuid;
      if (cur) sel.value = cur;
    }

    document.getElementById('ws050-scopeNote').textContent = screen.scopeNote || '';

    if (!trace.found) {
      document.getElementById('ws050-header').innerHTML = '<span class="om-muted">' + (trace.message || '거래 없음') + '</span>';
      document.getElementById('ws050-timeline').innerHTML = '';
      document.getElementById('ws050-current').innerHTML = '';
      document.getElementById('ws050-causes').innerHTML = '';
      document.getElementById('ws050-uncollected').innerHTML = '';
      document.getElementById('ws050-actions').innerHTML = '';
      return;
    }

    const stateBlock = trace.currentState || {};
    document.getElementById('ws050-header').innerHTML = `
      <div><strong>GUID</strong> <span class="om-mono">${OmAdmin.field(trace, 'guid')}</span>
        <span style="margin-left:16px"><strong>TraceId</strong> <span class="om-mono">${OmAdmin.field(trace, 'traceId')}</span></div>
      <div style="margin-top:6px"><strong>ServiceId</strong> <span class="om-mono">${OmAdmin.field(trace, 'serviceId')}</span>
        <span style="margin-left:16px"><strong>WAR</strong> ${OmAdmin.field(trace, 'businessCode')}</span></div>
      <div style="margin-top:6px"><strong>시작</strong> ${trace.startTimeDisplay || '-'}
        <span style="margin-left:16px"><strong>현재 경과</strong> ${trace.elapsedDisplay || '-'}</span></div>`;

    const timeline = trace.timeline || [];
    document.getElementById('ws050-timeline').innerHTML = timeline.length
      ? timeline.map(t => `
        <div class="rtm050-timeline-row ${t.highlight ? 'highlight' : ''}">
          <span class="ts">${t.timestampDisplay || '-'}</span>
          <span class="label">${t.label || '-'}</span>
          <span class="dur">${t.durationDisplay || '-'}</span>
          ${t.highlight ? '<span class="mark">●</span>' : ''}
        </div>`).join('')
      : '<p class="om-muted">수집된 Timeline 단계 없음</p>';

    document.getElementById('ws050-current').innerHTML = `
      <div class="rtm020-metric-grid">
        <div class="rtm020-metric-row"><span class="label">단계</span><span class="value">${stateBlock.stepLabel || stateBlock.currentStep || '-'}</span></div>
        <div class="rtm020-metric-row"><span class="label">Mapper</span><span class="value om-mono">${stateBlock.mapper || '-'}</span></div>
        <div class="rtm020-metric-row"><span class="label">SQL ID</span><span class="value om-mono">${stateBlock.sqlId || '-'}</span></div>
        <div class="rtm020-metric-row"><span class="label">Connection 대기</span><span class="value">${stateBlock.dbWaitDisplay || '-'}</span></div>
        <div class="rtm020-metric-row"><span class="label">SQL 실행시간</span><span class="value">${stateBlock.sqlElapsedDisplay || '-'}</span></div>
        <div class="rtm020-metric-row"><span class="label">Timeout 기준</span><span class="value">${stateBlock.timeoutSec != null ? stateBlock.timeoutSec + '초' : '-'}</span></div>
        <div class="rtm020-metric-row"><span class="label">Thread</span><span class="value om-mono">${OmAdmin.field(trace, 'threadName')}</span></div>
        <div class="rtm020-metric-row"><span class="label">SQL·외부</span><span class="value om-mono">${stateBlock.sqlOrExternal || '-'}</span></div>
      </div>`;

    const causes = trace.causeCandidates || [];
    document.getElementById('ws050-causes').innerHTML = causes.length
      ? '<ol>' + causes.map((c, i) => `<li><strong>${c.code || '-'}</strong> ${c.confidence || ''} — ${c.message || ''}</li>`).join('') + '</ol>'
      : '<p class="om-muted">원인 후보 없음</p>';

    const uncollected = trace.uncollectedSteps || [];
    document.getElementById('ws050-uncollected').innerHTML = uncollected.length
      ? '<p class="om-muted" style="font-size:0.85rem">미수집 단계: ' + uncollected.join(' · ') + '</p>'
      : '';

    const actions = trace.actions || [];
    document.getElementById('ws050-actions').innerHTML = actions.map(a =>
      `<a class="btn-secondary" href="${OmAdmin.uiPath(a.href)}">${a.label}</a>`).join('');
  }

  function read060FiltersFromDom() {
    return {
      war: document.getElementById('ws060-filterWar')?.value || 'ALL',
      serviceId: document.getElementById('ws060-filterServiceId')?.value?.trim() || '',
      elapsedPreset: document.getElementById('ws060-filterElapsed')?.value || '2000'
    };
  }

  function apply060SummaryFilters(rows, f) {
    return (rows || []).filter(r => {
      if (f.war && f.war !== 'ALL' && r.businessCode !== f.war) return false;
      if (f.serviceId) {
        const sid = String(r.serviceId || '');
        if (!sid.includes(f.serviceId)) return false;
      }
      const minMs = Number(f.elapsedPreset) || 0;
      if (minMs > 0 && Number(r.maxElapsedMs) < minMs) return false;
      return true;
    });
  }

  function mapperSqlHtml(text) {
    if (!text) return '-';
    const esc = s => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    return String(text).split('\n').map(l => esc(l)).join('<br>');
  }

  function renderRtm060() {
    const screen = state.body.sqlExternalScreen || {};
    const slowTab = screen.slowSqlTab || {};
    const externalTab = screen.externalTab || {};
    const f = { ...state.rtm060, ...read060FiltersFromDom() };
    state.rtm060 = f;
    save060Filters(f);

    document.getElementById('ws060-privacyNote').textContent = screen.privacyNote || '';

    const warSel = document.getElementById('ws060-filterWar');
    const opts = slowTab.filterOptions || {};
    if (warSel) {
      const wars = opts.wars || ['ALL'];
      warSel.innerHTML = wars.map(w => `<option value="${w}">${w === 'ALL' ? '전체' : w}</option>`).join('');
      warSel.value = f.war || 'ALL';
    }
    if (document.getElementById('ws060-filterServiceId')) {
      document.getElementById('ws060-filterServiceId').value = f.serviceId || '';
    }
    const elapsedSel = document.getElementById('ws060-filterElapsed');
    if (elapsedSel) {
      const presets = opts.elapsedPresets || [
        { value: '0', label: '전체' },
        { value: '2000', label: '2초 이상' }
      ];
      elapsedSel.innerHTML = presets.map(p => `<option value="${p.value}">${p.label}</option>`).join('');
      elapsedSel.value = f.elapsedPreset || '2000';
    }

    document.querySelectorAll('.rtm060-inner-tab').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.inner === state.rtm060Inner);
    });
    document.querySelectorAll('.rtm060-inner-panel').forEach(panel => {
      panel.classList.toggle('active', panel.id === 'ws060-inner-' + state.rtm060Inner);
    });

    const summaryAll = slowTab.summaryRows || [];
    const filtered = apply060SummaryFilters(summaryAll, f);
    const selectedKey = state.sqlRowKey || slowTab.selectedRowKey;
    document.getElementById('ws060-slowBody').innerHTML = filtered.length ? filtered.map(r => `
      <tr class="${r.healthStatus === 'CRITICAL' ? 'om-row-tx-critical' : (r.healthStatus === 'WARN' ? 'om-row-tx-warn' : '')} ${selectedKey === r.rowKey ? 'rtm060-row-selected' : ''}" data-row-key="${r.rowKey || ''}">
        <td>${r.statusLabel || '-'}</td>
        <td>${OmAdmin.field(r, 'businessCode')}</td>
        <td class="om-mono">${OmAdmin.field(r, 'serviceId')}</td>
        <td class="om-mono">${mapperSqlHtml(r.mapperSqlDisplay || (r.mapper + '\n' + r.sqlId))}</td>
        <td>${r.elapsedDisplay || '-'}</td>
        <td>${r.count != null ? r.count : '-'}</td>
        <td><button type="button" class="btn-secondary btn-sm ws060-select-btn" data-row-key="${r.rowKey || ''}">상세</button></td>
      </tr>`).join('') : '<tr><td colspan="7" class="om-empty">조건에 맞는 Slow SQL 없음</td></tr>';

    const detail = slowTab.detail || {};
    const detailEl = document.getElementById('ws060-slowDetail');
    if (!detail.found) {
      detailEl.innerHTML = `<p class="om-muted">${detail.message || 'SQL을 선택하세요.'}</p>`;
    } else {
      detailEl.innerHTML = `
        <div class="rtm020-metric-grid">
          <div class="rtm020-metric-row"><span class="label">WAR</span><span class="value">${OmAdmin.field(detail, 'businessCode')}</span></div>
          <div class="rtm020-metric-row"><span class="label">ServiceId</span><span class="value om-mono">${OmAdmin.field(detail, 'serviceId')}</span></div>
          <div class="rtm020-metric-row"><span class="label">GUID·TraceId</span><span class="value om-mono">${OmAdmin.field(detail, 'guid')} · ${OmAdmin.field(detail, 'traceId')}</span></div>
          <div class="rtm020-metric-row"><span class="label">Mapper ID</span><span class="value om-mono">${OmAdmin.field(detail, 'mapper')}</span></div>
          <div class="rtm020-metric-row"><span class="label">SQL ID</span><span class="value om-mono">${OmAdmin.field(detail, 'sqlId')}</span></div>
          <div class="rtm020-metric-row"><span class="label">실행 시작시각</span><span class="value">${detail.startTimeDisplay || '-'}</span></div>
          <div class="rtm020-metric-row"><span class="label">실행시간</span><span class="value">${detail.elapsedDisplay || '-'}</span></div>
          <div class="rtm020-metric-row"><span class="label">Connection 대기</span><span class="value">${detail.dbWaitDisplay || '-'}</span></div>
          <div class="rtm020-metric-row"><span class="label">결과상태</span><span class="value">${detail.resultStatusLabel || detail.resultStatus || '-'}</span></div>
          <div class="rtm020-metric-row"><span class="label">오류유형</span><span class="value">${OmAdmin.field(detail, 'errorType')}</span></div>
          <div class="rtm020-metric-row"><span class="label">처리건수</span><span class="value">${detail.affectedRowsDisplay || '-'}</span></div>
          <div class="rtm020-metric-row"><span class="label">동일 SQL 동시 실행</span><span class="value">${detail.concurrentSameSql != null ? detail.concurrentSameSql + '건' : '-'}</span></div>
        </div>`;
    }

    const extRows = externalTab.rows || [];
    const extFiltered = extRows.filter(r => {
      if (f.war && f.war !== 'ALL' && r.businessCode !== f.war) return false;
      if (f.serviceId) {
        const sid = String(r.serviceId || '');
        if (!sid.includes(f.serviceId)) return false;
      }
      return true;
    });
    document.getElementById('ws060-externalNote').textContent = externalTab.collectionNote || '';
    document.getElementById('ws060-externalBody').innerHTML = extFiltered.length ? extFiltered.map(r => `
      <tr class="${Number(r.readWaitMs) >= 3000 ? 'om-row-tx-warn' : ''}">
        <td>${OmAdmin.field(r, 'businessCode')}</td>
        <td class="om-mono">${OmAdmin.field(r, 'serviceId')}</td>
        <td class="om-mono">${OmAdmin.field(r, 'externalSystemCode')}</td>
        <td>${OmAdmin.field(r, 'endpointIdentifier')}</td>
        <td>${r.connectDisplay || '-'}</td>
        <td>${r.readWaitDisplay || '-'}</td>
        <td>${r.timeoutSec != null ? r.timeoutSec + '초' : '-'}</td>
        <td>${r.statusLabel || r.status || '-'}</td>
        <td>${r.concurrentWait != null ? r.concurrentWait : '-'}</td>
      </tr>`).join('')       : '<tr><td colspan="9" class="om-empty">외부연계 대기 거래 없음</td></tr>';
  }

  function nodeTypeClass(nodeType) {
    if (nodeType === 'ROOT') return 'root';
    if (nodeType === 'DIRECT') return 'direct';
    if (nodeType === 'SYMPTOM') return 'symptom';
    if (nodeType === 'NORMAL') return 'normal';
    if (nodeType === 'UNKNOWN') return 'unknown';
    return 'unavailable';
  }

  function renderCausePath(graph) {
    const nodes = graph.nodes || [];
    const edges = graph.edges || [];
    if (!nodes.length) return '<p class="om-muted">원인 경로 데이터 없음</p>';

    const branchEdges = edges.filter(e => e.style === 'dashed' || e.edgeType === 'CORRELATION');
    const mainNodes = nodes;

    let html = '<div class="rtm100-path-main">';
    mainNodes.forEach((n, i) => {
      html += `
        <div class="rtm100-node ${nodeTypeClass(n.nodeType)}">
          <span class="sym">${n.symbol || '○'}</span>
          <span class="label">${n.label || '-'}</span>
          <span class="ts">${n.timestampDisplay || '-'}</span>
        </div>`;
      if (i < mainNodes.length - 1) {
        html += '<span class="rtm100-arrow">──▶</span>';
      }
    });
    html += '</div>';

    if (branchEdges.length) {
      html += '<div class="rtm100-path-branch">';
      branchEdges.forEach(e => {
        const from = nodes.find(n => n.id === e.from);
        const to = nodes.find(n => n.id === e.to);
        if (from && to) {
          html += `<div class="rtm100-branch-line">
            <span class="om-muted">${from.label}</span>
            <span class="rtm100-arrow dashed">- - ▶</span>
            <span class="om-muted">${to.label}</span>
            <span class="edge-tag">${e.edgeType || 'CORRELATION'}</span>
          </div>`;
        }
      });
      html += '</div>';
    }
    return html;
  }

  function read100FiltersFromDom() {
    return {
      center: document.getElementById('ws100-filterCenter')?.value || 'ALL',
      instance: document.getElementById('ws100-filterInstance')?.value || 'ALL',
      war: document.getElementById('ws100-filterWar')?.value || 'ALL',
      timePoint: document.getElementById('ws100-filterTime')?.value || '30M'
    };
  }

  function renderRtm100() {
    const screen = state.body.causeTracingScreen || {};
    const summary = screen.summary || {};
    const graph = screen.pathGraph || {};
    const candidates = screen.candidateRanking || [];
    const judgment = screen.keyJudgment || {};
    const opts = screen.filterOptions || {};
    const f = { ...state.filters, ...read100FiltersFromDom() };
    state.filters = f;
    saveFilters(f);

    el('ws100-introNote').textContent = screen.introNote || '';
    el('ws100-analyzedAt').textContent = summary.analyzedAt || '-';
    el('ws100-completeness').textContent = `데이터 완전성 ${screen.dataCompletenessPct != null ? screen.dataCompletenessPct : '-'}%`;
    const statusLabel = screen.analysisStatusLabel || '-';
    el('ws100-statusBadge').innerHTML = screen.analysisStatus === 'ANALYZING'
      ? `<span class="rtm100-analyzing">분석 중 ●</span>`
      : `<span class="om-muted">${statusLabel}</span>`;

    const instSel = document.getElementById('ws100-filterInstance');
    const warSel = document.getElementById('ws100-filterWar');
    if (instSel) {
      const instances = opts.instances || ['ALL'];
      instSel.innerHTML = instances.map(v => `<option value="${v}">${v === 'ALL' ? '전체' : v}</option>`).join('');
      instSel.value = f.instance || 'ALL';
    }
    if (warSel) {
      const wars = opts.wars || ['ALL'];
      warSel.innerHTML = wars.map(v => `<option value="${v}">${v === 'ALL' ? '전체' : v}</option>`).join('');
      warSel.value = f.war || 'ALL';
    }
    if (document.getElementById('ws100-filterCenter')) {
      document.getElementById('ws100-filterCenter').value = f.center || 'ALL';
    }
    if (document.getElementById('ws100-filterTime')) {
      document.getElementById('ws100-filterTime').value = f.timePoint || '30M';
    }

    const statusLevel = summary.overallStatus || 'NORMAL';
    el('ws100-sumStatus').innerHTML = OmAdmin.chipForHealth(summary.overallStatusLabel || OmRuntime.statusLabel(statusLevel));
    el('ws100-sumSymptom').textContent = summary.firstSymptom || '-';
    el('ws100-sumRoot').textContent = summary.rootCauseCandidate || '-';
    el('ws100-sumConfidence').textContent = `${summary.confidencePct != null ? summary.confidencePct + '% ' : ''}${summary.confidence || '-'}`;
    el('ws100-sumScope').textContent = summary.impactScope || '-';
    el('ws100-sumService').innerHTML = summary.impactServiceId && summary.impactServiceId !== '-'
      ? `<a href="#" class="rtm-ws-nav-link" data-tab="rtm040" data-service-id="${summary.impactServiceId}">${summary.impactServiceId}</a>`
      : '-';
    el('ws100-sumSql').innerHTML = summary.impactSql && summary.impactSql !== '-'
      ? `<a href="#" class="rtm-ws-nav-link" data-tab="rtm060">${summary.impactSql}</a>`
      : '-';
    el('ws100-sumDetected').textContent = summary.firstDetectedAt || '-';

    el('ws100-pathGraph').innerHTML = renderCausePath(graph);

    const legend = screen.pathLegend || [];
    el('ws100-pathLegend').innerHTML = legend.map(l =>
      `<span class="rtm100-legend-item"><span class="sym">${l.symbol}</span> ${l.label}</span>`).join('');

    el('ws100-candidateBody').innerHTML = candidates.length ? candidates.map(c => `
      <tr class="${c.rank === 1 ? 'om-row-tx-warn' : ''}">
        <td>${c.rank}</td>
        <td class="om-mono">${c.causeCode || '-'}</td>
        <td>${c.confidencePct != null ? c.confidencePct + '%' : '-'} ${c.confidenceLabel || ''}</td>
        <td>${c.evidenceCount != null ? c.evidenceCount + '개' : '-'}</td>
        <td>${c.firstDetectedAt || '-'}</td>
        <td>${c.impactScope || '-'}</td>
        <td>${c.verdict || '-'}</td>
      </tr>`).join('') : '<tr><td colspan="7" class="om-empty">원인 후보 없음</td></tr>';

    el('ws100-judgmentNarrative').textContent = judgment.narrative || '-';
    el('ws100-judgmentReco').textContent = judgment.recommendation || '-';

    const actions = screen.actions || [];
    el('ws100-actions').innerHTML = actions.map(a => {
      let tab = null;
      if ((a.href || '').includes('tab=rtm050')) tab = 'rtm050';
      if ((a.href || '').includes('tab=rtm060')) tab = 'rtm060';
      if ((a.href || '').includes('tab=rtm010')) tab = 'rtm010';
      if (tab) return `<a href="#" class="btn-secondary rtm-ws-nav-link" data-tab="${tab}">${a.label}</a>`;
      return `<a class="btn-secondary" href="${OmAdmin.uiPath(a.href)}">${a.label}</a>`;
    }).join('') + `
      <button type="button" class="btn-secondary" id="ws100-rejectBtn">기각</button>`;
  }

  function el(id) {
    return document.getElementById(id);
  }

  function switchRtm060Inner(tabId) {
    state.rtm060Inner = tabId;
    document.querySelectorAll('.rtm060-inner-tab').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.inner === tabId);
    });
    document.querySelectorAll('.rtm060-inner-panel').forEach(panel => {
      panel.classList.toggle('active', panel.id === 'ws060-inner-' + tabId);
    });
  }

  function switchInnerTab(tabId) {
    state.innerTab = tabId;
    document.querySelectorAll('.rtm030-inner-tab').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.inner === tabId);
    });
    document.querySelectorAll('.rtm030-inner-panel').forEach(panel => {
      panel.classList.toggle('active', panel.id === 'ws030-inner-' + tabId);
    });
  }

  function switchRtm020Tab(tabId) {
    document.querySelectorAll('#ws-rtm020 .om-tab').forEach(t => {
      t.classList.toggle('active', t.dataset.tab === tabId);
    });
    document.querySelectorAll('#ws-rtm020 .rtm020-tab-panel').forEach(p => {
      p.classList.toggle('active', p.id === 'ws020-panel-' + tabId);
    });
  }

  function renderCurrent() {
    if (!state.body) return;
    if (state.tab === 'rtm010') renderRtm010();
    else if (state.tab === 'rtm020') renderRtm020();
    else if (state.tab === 'rtm030') renderRtm030();
    else if (state.tab === 'rtm040') renderRtm040();
    else if (state.tab === 'rtm050') renderRtm050();
    else if (state.tab === 'rtm060') renderRtm060();
    else if (state.tab === 'rtm100') renderRtm100();
  }

  async function loadAll() {
    const payload = { includeDetails: 'Y' };
    if (state.traceGuid) payload.traceGuid = state.traceGuid;
    if (state.sqlRowKey) payload.sqlRowKey = state.sqlRowKey;
    const result = await OmRuntime.loadDiagnostics({ body: payload });
    state.body = result.body;
    state.relay = result.relay;
    const statusEl = document.getElementById('ws-status');
    if (statusEl) {
      statusEl.textContent = `${state.relay.elapsedMs}ms · 점검 ${state.body.checkedAt || '-'}`;
    }
    renderCurrent();
  }

  function bindDelegates(root, reload) {
    root.addEventListener('click', e => {
      const nav = e.target.closest('.rtm-ws-nav-link');
      if (nav) {
        e.preventDefault();
        const traceGuid = nav.dataset.traceGuid || undefined;
        navigate(nav.dataset.tab, {
          war: nav.dataset.war || null,
          serviceId: nav.dataset.serviceId || undefined,
          traceGuid
        });
        if (nav.dataset.serviceId && state.tab === 'rtm040') {
          const el = document.getElementById('ws040-filterServiceId');
          if (el) el.value = nav.dataset.serviceId;
          renderRtm040();
        }
        if (nav.dataset.serviceId && nav.dataset.tab === 'rtm060') {
          state.rtm060 = { ...state.rtm060, serviceId: nav.dataset.serviceId };
          save060Filters(state.rtm060);
        }
        if (traceGuid) reload().catch(() => {});
        return;
      }
      const sel = e.target.closest('.ws040-select-btn');
      if (sel) {
        state.selectedTxGuid = sel.dataset.guid;
        state.traceGuid = sel.dataset.guid;
        pushQuery();
        navigate('rtm050', { traceGuid: sel.dataset.guid });
        reload().catch(() => {});
        return;
      }
      const sqlSel = e.target.closest('.ws060-select-btn');
      if (sqlSel) {
        state.sqlRowKey = sqlSel.dataset.rowKey;
        pushQuery();
        reload().catch(() => {});
        return;
      }
      const reject = e.target.closest('#ws100-rejectBtn');
      if (reject) {
        if (confirm('현재 자동 원인 후보를 기각하고 수동 분석으로 전환할까요?')) {
          alert('기각 처리되었습니다. RTM-070 보고서에서 확정 원인을 기록하십시오.');
        }
        return;
      }
    });
  }

  function stopAuto() {
    if (state.autoTimer) {
      clearInterval(state.autoTimer);
      state.autoTimer = null;
    }
  }

  function startAuto(run) {
    stopAuto();
    if (!state.autoEnabled) return;
    state.autoTimer = setInterval(() => run().catch(() => {}), 10000);
  }

  function buildShell(el) {
    el.innerHTML = `
      <div class="rtm010-toolbar">
        <div class="rtm010-toolbar-title">런타임·장애진단 워크스페이스</div>
        <div class="rtm010-auto-refresh">
          <span>자동갱신 10초</span>
          <button type="button" class="btn-secondary" id="ws-autoToggleBtn">중지</button>
        </div>
      </div>
      <section class="om-filter-bar">
        <button type="button" class="btn-primary" id="ws-refreshBtn">조회</button>
        <span id="ws-status" class="om-muted" style="font-size:0.85rem"></span>
      </section>
      <nav class="rtm-ws-screen-tabs" id="ws-screenTabs"></nav>

      <div id="ws-rtm010" class="rtm-ws-panel">
        <section class="om-filter-bar">
          <label class="field">센터<select id="ws010-filterCenter"><option value="ALL">전체</option><option value="CENTER1">CENTER1</option><option value="CENTER2">CENTER2</option></select></label>
          <label class="field">환경<select id="ws010-filterEnv"><option value="PROD">운영</option><option value="DR">DR</option><option value="STG">스테이징</option></select></label>
          <label class="field">인스턴스<select id="ws010-filterInstance"><option value="ALL">전체</option></select></label>
          <label class="field">WAR<select id="ws010-filterWar"><option value="ALL">전체</option></select></label>
          <label class="field">조회시점<select id="ws010-filterTime"><option value="NOW">현재</option><option value="10M">최근 10분</option><option value="30M">최근 30분</option></select></label>
          <span class="rtm010-freshness ok" id="ws010-dataFreshness">데이터 정상</span>
          <span id="ws010-lastCollected" class="om-muted" style="font-size:0.85rem"></span>
          <span id="ws010-relayMeta" class="om-muted" style="font-size:0.85rem"></span>
        </section>
        <div class="rtm010-panel"><div class="rtm010-panel-body">
          <div class="rtm010-summary-grid">
            <div class="rtm010-summary-item"><label>종합상태</label><div class="value" id="ws010-sumStatus">-</div></div>
            <div class="rtm010-summary-item"><label>1순위 원인</label><div class="value" id="ws010-sumCause">-</div></div>
            <div class="rtm010-summary-item"><label>신뢰도</label><div class="value" id="ws010-sumConfidence">-</div></div>
            <div class="rtm010-summary-item"><label>영향범위</label><div class="value" id="ws010-sumScope">-</div></div>
          </div>
          <div class="rtm010-meta-line"><span>영향 ServiceId:</span><strong id="ws010-metaService">-</strong></div>
          <div class="rtm010-meta-line"><span>현재 단계:</span><strong id="ws010-metaStep">-</strong></div>
        </div></div>
        <div class="rtm010-panel"><div class="rtm010-panel-head">핵심 진단 질문</div><div class="rtm010-panel-body"><div class="rtm010-questions" id="ws010-questionList"></div></div></div>
        <div class="om-grid-2">
          <div class="rtm010-panel"><div class="rtm010-panel-head">원인판정 근거</div><div class="rtm010-panel-body"><ul class="rtm010-evidence" id="ws010-evidenceList"></ul></div></div>
          <div class="rtm010-panel"><div class="rtm010-panel-head">즉시 확인·조치</div><div class="rtm010-panel-body"><ol class="rtm010-actions" id="ws010-actionList"></ol></div></div>
        </div>
        <div class="rtm010-panel"><div class="rtm010-panel-head">바로가기</div><div class="rtm010-panel-body rtm010-quick-links" id="ws010-quickLinks"></div></div>
        <div class="rtm010-panel"><div class="rtm010-panel-head">인스턴스별 상태</div><div class="rtm010-panel-body om-table-wrap">
          <table class="om-table"><thead><tr><th>인스턴스</th><th>상태</th><th>CPU</th><th>Thread</th><th>GC</th><th>위험 WAR</th><th>1순위 원인</th></tr></thead>
          <tbody id="ws010-instanceBody"><tr><td colspan="7" class="om-empty">조회 중...</td></tr></tbody></table>
        </div></div>
      </div>

      <div id="ws-rtm020" class="rtm-ws-panel">
        <p class="rtm-ws-scope-note" id="ws020-scopeNote"></p>
        <div class="rtm020-deadlock-banner" id="ws020-deadlockBanner" hidden></div>
        <div class="rtm020-header" id="ws020-instanceHeader">-</div>
        <nav class="om-tabs" id="ws020-tabNav">
          <button type="button" class="om-tab active" data-tab="summary">요약</button>
          <button type="button" class="om-tab" data-tab="thread">Thread</button>
          <button type="button" class="om-tab" data-tab="jvm">JVM·메모리</button>
          <button type="button" class="om-tab" data-tab="gc">GC</button>
          <button type="button" class="om-tab" data-tab="deadlock">Deadlock</button>
          <button type="button" class="om-tab" data-tab="wars">배포 WAR</button>
        </nav>
        <div id="ws020-panel-summary" class="rtm020-tab-panel active">
          <div class="rtm010-panel"><div class="rtm010-panel-head">인스턴스 요약</div><div class="rtm010-panel-body rtm020-metric-grid" id="ws020-summaryMetrics"></div></div>
          <div class="rtm010-panel"><div class="rtm010-panel-head">최근 추세 (로컬 샘플)</div><div class="rtm010-panel-body rtm020-trends" id="ws020-trendBlock"></div></div>
          <div class="rtm010-panel"><div class="rtm010-panel-head">배포 WAR</div><div class="rtm010-panel-body om-table-wrap">
            <table class="om-table"><thead><tr><th>WAR</th><th>상태</th><th>Active</th><th>Slow</th><th>Thread 점유</th><th>Pool</th></tr></thead><tbody id="ws020-warBody"></tbody></table>
          </div></div>
        </div>
        <div id="ws020-panel-thread" class="rtm020-tab-panel"><div class="rtm010-panel"><div class="rtm010-panel-head">Thread</div><div class="rtm010-panel-body om-table-wrap">
          <table class="om-table"><thead><tr><th>Thread Name</th><th>ID</th><th>상태</th><th>WAR</th><th>ServiceId</th><th>실행시간</th><th>Step</th><th>SQL·외부</th><th>CPU</th></tr></thead><tbody id="ws020-threadBody"></tbody></table>
        </div></div></div>
        <div id="ws020-panel-jvm" class="rtm020-tab-panel"><div class="rtm010-panel"><div class="rtm010-panel-head">JVM·메모리</div><div class="rtm010-panel-body" id="ws020-jvmPanel"></div></div></div>
        <div id="ws020-panel-gc" class="rtm020-tab-panel"><div class="rtm010-panel"><div class="rtm010-panel-head">GC</div><div class="rtm010-panel-body" id="ws020-gcPanel"></div></div></div>
        <div id="ws020-panel-deadlock" class="rtm020-tab-panel"><div class="rtm010-panel"><div class="rtm010-panel-head">Deadlock</div><div class="rtm010-panel-body" id="ws020-deadlockPanel"></div></div></div>
        <div id="ws020-panel-wars" class="rtm020-tab-panel"><div class="rtm010-panel"><div class="rtm010-panel-head">배포 WAR</div><div class="rtm010-panel-body om-table-wrap">
          <table class="om-table"><thead><tr><th>WAR</th><th>상태</th><th>Active</th><th>Slow</th><th>Thread 점유</th><th>Pool</th></tr></thead><tbody id="ws020-warBody2"></tbody></table>
        </div></div></div>
      </div>

      <div id="ws-rtm030" class="rtm-ws-panel">
        <div id="ws030-content">
          <p class="rtm-ws-scope-note" id="ws030-scopeNote"></p>
          <div class="rtm030-header" id="ws030-header">-</div>
          <label class="field">WAR 선택 <select id="ws030-warSelect"></select></label>
          <div class="rtm030-kpi-row" id="ws030-kpi"></div>
          <nav class="rtm030-inner-tabs" id="ws030-innerTabs">
            <button type="button" class="rtm030-inner-tab active" data-inner="pool">DB Pool</button>
            <button type="button" class="rtm030-inner-tab" data-inner="service">ServiceId</button>
            <button type="button" class="rtm030-inner-tab" data-inner="tx">거래</button>
            <button type="button" class="rtm030-inner-tab" data-inner="sql">SQL</button>
            <button type="button" class="rtm030-inner-tab" data-inner="external">외부연계</button>
          </nav>
          <div id="ws030-inner-pool" class="rtm030-inner-panel active">
            <div class="rtm010-panel"><div class="rtm010-panel-head">DB Pool 상태</div><div class="rtm010-panel-body om-table-wrap">
              <table class="om-table"><thead><tr><th>Pool</th><th>Max</th><th>Active</th><th>Idle</th><th>Pending</th><th>획득 p95</th><th>상태</th></tr></thead><tbody id="ws030-poolBody"></tbody></table>
            </div></div>
            <div class="rtm010-panel"><div class="rtm010-panel-head">DB Pool 상세 (§6.3)</div><div class="rtm010-panel-body om-table-wrap">
              <table class="om-table"><thead><tr><th>Pool</th><th>Max</th><th>Active</th><th>Idle</th><th>Pending</th><th>Usage</th><th>Avg</th><th>p95</th><th>Timeout</th><th>Leak</th><th>상태</th></tr></thead><tbody id="ws030-poolDetailBody"></tbody></table>
            </div></div>
          </div>
          <div id="ws030-inner-service" class="rtm030-inner-panel">
            <div class="rtm010-panel"><div class="rtm010-panel-head">주요 ServiceId</div><div class="rtm010-panel-body om-table-wrap">
              <table class="om-table"><thead><tr><th>ServiceId</th><th>Active</th><th>Slow</th><th>평균</th><th>p95</th><th>주요 단계</th></tr></thead><tbody id="ws030-serviceBody"></tbody></table>
            </div></div>
          </div>
          <div id="ws030-inner-tx" class="rtm030-inner-panel"><div class="rtm010-panel"><div class="rtm010-panel-body om-table-wrap">
            <table class="om-table"><thead><tr><th>ServiceId</th><th>GUID</th><th>Step</th><th>경과</th><th>SQL·외부</th></tr></thead><tbody id="ws030-txBody"></tbody></table>
          </div></div></div>
          <div id="ws030-inner-sql" class="rtm030-inner-panel"><div class="rtm010-panel"><div class="rtm010-panel-body om-table-wrap">
            <table class="om-table"><thead><tr><th>ServiceId</th><th>SQL</th><th>경과</th><th>상태</th></tr></thead><tbody id="ws030-sqlBody"></tbody></table>
          </div></div></div>
          <div id="ws030-inner-external" class="rtm030-inner-panel"><div class="rtm010-panel"><div class="rtm010-panel-body om-table-wrap">
            <table class="om-table"><thead><tr><th>ServiceId</th><th>외부시스템</th><th>경과</th><th>GUID</th></tr></thead><tbody id="ws030-extBody"></tbody></table>
          </div></div></div>
          <div class="rtm030-verdict" id="ws030-verdict"></div>
        </div>
      </div>

      <div id="ws-rtm040" class="rtm-ws-panel">
        <p class="rtm-ws-scope-note" id="ws040-scopeNote">현재 실행 중인 거래와 느린 ServiceId를 확인합니다.</p>
        <section class="om-filter-bar">
          <label class="field">센터<select id="ws040-filterCenter"><option value="ALL">전체</option><option value="CENTER1">CENTER1</option><option value="CENTER2">CENTER2</option></select></label>
          <label class="field">인스턴스<select id="ws040-filterInstance"><option value="ALL">전체</option></select></label>
          <label class="field">WAR<select id="ws040-filterWar"><option value="ALL">전체</option></select></label>
          <label class="field">단계<select id="ws040-filterStep"><option value="ALL">전체</option><option value="WAIT_DB_CONNECTION">DB 대기</option><option value="EXECUTING_SQL">SQL 실행</option><option value="WAIT_EXTERNAL">외부 대기</option><option value="WAIT_HANDLER">Handler 대기</option></select></label>
          <label class="field">실행시간<select id="ws040-filterElapsed"><option value="0">전체</option><option value="1000">1초 이상</option><option value="3000">3초 이상</option><option value="custom">직접입력</option></select></label>
          <span id="ws040-elapsedCustomWrap" style="display:none"><label class="field">ms<input type="number" id="ws040-filterElapsedCustom" min="0" placeholder="5000" style="width:90px"></label></span>
          <label class="field">상태<select id="ws040-filterStatus"><option value="ALL">전체</option><option value="RUNNING">실행 중</option><option value="TIMEOUT">Timeout</option><option value="ERROR">오류</option></select></label>
          <label class="field">ServiceId<input type="text" id="ws040-filterServiceId" placeholder="부분 검색"></label>
          <label class="field">GUID/TraceId<input type="text" id="ws040-filterGuid" placeholder="정확·부분 조회" style="min-width:180px"></label>
          <button type="button" class="btn-primary" id="ws040-searchBtn">조회</button>
        </section>
        <div class="rtm040-kpi-bar" id="ws040-kpiBar">-</div>
        <div class="rtm010-panel">
          <div class="rtm010-panel-head">실행 거래 목록</div>
          <div class="rtm010-panel-body om-table-wrap">
            <table class="om-table">
              <thead><tr><th>상태</th><th>WAR</th><th>ServiceId</th><th>실행시간</th><th>현재 단계</th><th>상세</th></tr></thead>
              <tbody id="ws040-txBody"></tbody>
            </table>
          </div>
        </div>
        <div class="rtm010-panel">
          <div class="rtm010-panel-head">목록 상세 (§7.4)</div>
          <div class="rtm010-panel-body om-table-wrap">
            <table class="om-table">
              <thead><tr><th>상태</th><th>WAR</th><th>ServiceId</th><th>GUID</th><th>TraceId</th><th>시작</th><th>실행시간</th><th>Step</th><th>Thread</th><th>SQL·외부</th><th>Timeout</th><th>잔여</th></tr></thead>
              <tbody id="ws040-detailBody"></tbody>
            </table>
          </div>
        </div>
        <div class="rtm010-panel">
          <div class="rtm010-panel-head">Slow ServiceId</div>
          <div class="rtm010-panel-body om-table-wrap">
            <table class="om-table">
              <thead><tr><th>ServiceId</th><th>Active Slow</th><th>최근 Slow</th><th>최대 경과</th></tr></thead>
              <tbody id="ws040-slowSvcBody"></tbody>
            </table>
          </div>
        </div>
        <div class="rtm040-selected" id="ws040-selected"><p class="om-muted">목록에서 거래를 선택하세요.</p></div>
      </div>

      <div id="ws-rtm050" class="rtm-ws-panel">
        <p class="rtm-ws-scope-note" id="ws050-scopeNote"></p>
        <section class="om-filter-bar">
          <label class="field">GUID<select id="ws050-guidSelect" style="min-width:280px"></select></label>
          <button type="button" class="btn-primary" id="ws050-searchBtn">조회</button>
          <a href="#" class="btn-secondary rtm-ws-nav-link" data-tab="rtm040">← 실행 거래</a>
        </section>
        <div class="rtm050-header" id="ws050-header">-</div>
        <div class="rtm010-panel">
          <div class="rtm010-panel-head">처리 Timeline</div>
          <div class="rtm010-panel-body rtm050-timeline" id="ws050-timeline"></div>
        </div>
        <div class="rtm010-panel">
          <div class="rtm010-panel-head">현재 상태</div>
          <div class="rtm010-panel-body" id="ws050-current"></div>
        </div>
        <div class="rtm010-panel">
          <div class="rtm010-panel-head">원인 후보</div>
          <div class="rtm010-panel-body" id="ws050-causes"></div>
          <div class="rtm010-panel-body" id="ws050-uncollected"></div>
        </div>
        <div class="rtm050-actions" id="ws050-actions"></div>
      </div>

      <div id="ws-rtm060" class="rtm-ws-panel">
        <p class="rtm-ws-scope-note" id="ws060-privacyNote"></p>
        <nav class="rtm060-inner-tabs" id="ws060-innerTabs">
          <button type="button" class="rtm060-inner-tab active" data-inner="slow">Slow SQL</button>
          <button type="button" class="rtm060-inner-tab" data-inner="external">외부연계 대기</button>
        </nav>
        <div id="ws060-inner-slow" class="rtm060-inner-panel active">
          <section class="om-filter-bar">
            <label class="field">WAR<select id="ws060-filterWar"><option value="ALL">전체</option></select></label>
            <label class="field">ServiceId<input id="ws060-filterServiceId" type="text" placeholder="부분 일치" style="min-width:200px"></label>
            <label class="field">실행시간<select id="ws060-filterElapsed"><option value="2000">2초 이상</option></select></label>
            <button type="button" class="btn-primary" id="ws060-searchBtn">조회</button>
            <a href="#" class="btn-secondary rtm-ws-nav-link" data-tab="rtm040">← 실행 거래</a>
          </section>
          <div class="rtm010-panel">
            <div class="rtm010-panel-head">Slow SQL 조회</div>
            <div class="rtm010-panel-body om-table-wrap">
              <table class="om-table">
                <thead><tr><th>상태</th><th>WAR</th><th>ServiceId</th><th>Mapper / SQL ID</th><th>시간</th><th>건수</th><th>상세</th></tr></thead>
                <tbody id="ws060-slowBody"></tbody>
              </table>
            </div>
          </div>
          <div class="rtm010-panel">
            <div class="rtm010-panel-head">SQL 상세</div>
            <div class="rtm010-panel-body" id="ws060-slowDetail"><p class="om-muted">목록에서 SQL을 선택하세요.</p></div>
          </div>
        </div>
        <div id="ws060-inner-external" class="rtm060-inner-panel">
          <p class="om-muted" style="font-size:0.85rem;margin:8px 0" id="ws060-externalNote"></p>
          <div class="rtm010-panel">
            <div class="rtm010-panel-head">외부연계 대기</div>
            <div class="rtm010-panel-body om-table-wrap">
              <table class="om-table">
                <thead><tr><th>WAR</th><th>ServiceId</th><th>외부 시스템</th><th>Endpoint 식별명</th><th>Connect</th><th>Read 대기</th><th>Timeout</th><th>상태</th><th>동시 대기</th></tr></thead>
                <tbody id="ws060-externalBody"></tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <div id="ws-rtm100" class="rtm-ws-panel">
        <p class="rtm-ws-scope-note" id="ws100-introNote"></p>
        <section class="om-filter-bar">
          <label class="field">센터<select id="ws100-filterCenter"><option value="ALL">전체</option><option value="CENTER1">CENTER1</option><option value="CENTER2">CENTER2</option></select></label>
          <label class="field">인스턴스<select id="ws100-filterInstance"><option value="ALL">전체</option></select></label>
          <label class="field">WAR<select id="ws100-filterWar"><option value="ALL">전체</option></select></label>
          <label class="field">기간<select id="ws100-filterTime"><option value="NOW">현재</option><option value="10M">최근 10분</option><option value="30M" selected>최근 30분</option><option value="60M">최근 60분</option></select></label>
          <button type="button" class="btn-primary" id="ws100-searchBtn">조회</button>
          <a href="#" class="btn-secondary rtm-ws-nav-link" data-tab="rtm010">← 통합 진단</a>
          <span class="om-muted" style="font-size:0.85rem;margin-left:auto">
            분석시각 <span id="ws100-analyzedAt">-</span> · <span id="ws100-completeness">-</span> · <span id="ws100-statusBadge">-</span>
          </span>
        </section>

        <div class="rtm010-panel">
          <div class="rtm010-panel-head">장애 요약</div>
          <div class="rtm010-panel-body">
            <div class="rtm100-summary-grid">
              <div class="rtm100-sum-item"><label>상태</label><div id="ws100-sumStatus">-</div></div>
              <div class="rtm100-sum-item"><label>최초 증상</label><div id="ws100-sumSymptom">-</div></div>
              <div class="rtm100-sum-item"><label>1순위 근본원인</label><div class="om-mono" id="ws100-sumRoot">-</div></div>
              <div class="rtm100-sum-item"><label>신뢰도</label><div id="ws100-sumConfidence">-</div></div>
              <div class="rtm100-sum-item"><label>영향 범위</label><div id="ws100-sumScope">-</div></div>
              <div class="rtm100-sum-item"><label>영향 ServiceId</label><div id="ws100-sumService">-</div></div>
              <div class="rtm100-sum-item"><label>영향 SQL</label><div class="om-mono" id="ws100-sumSql">-</div></div>
              <div class="rtm100-sum-item"><label>최초 탐지</label><div id="ws100-sumDetected">-</div></div>
            </div>
          </div>
        </div>

        <div class="rtm010-panel">
          <div class="rtm010-panel-head">자동 원인 추적 경로</div>
          <div class="rtm010-panel-body rtm100-path-wrap" id="ws100-pathGraph"></div>
          <div class="rtm010-panel-body rtm100-path-legend" id="ws100-pathLegend"></div>
        </div>

        <div class="rtm010-panel">
          <div class="rtm010-panel-head">원인 후보 순위</div>
          <div class="rtm010-panel-body om-table-wrap">
            <table class="om-table">
              <thead><tr><th>순위</th><th>Cause Code</th><th>신뢰도</th><th>근거 수</th><th>최초시각</th><th>영향범위</th><th>판정</th></tr></thead>
              <tbody id="ws100-candidateBody"></tbody>
            </table>
          </div>
        </div>

        <div class="rtm010-panel">
          <div class="rtm010-panel-head">핵심 판단</div>
          <div class="rtm010-panel-body">
            <p id="ws100-judgmentNarrative" style="margin:0 0 10px;line-height:1.6">-</p>
            <p id="ws100-judgmentReco" class="om-muted" style="margin:0;font-size:0.9rem">-</p>
          </div>
        </div>
        <div class="rtm100-actions" id="ws100-actions"></div>
      </div>`;

    const tabsEl = document.getElementById('ws-screenTabs');
    tabsEl.innerHTML = SCREENS.map(s =>
      `<button type="button" class="rtm-ws-screen-tab" data-screen="${s.id}">${s.label}</button>`).join('');
  }

  async function init(el) {
    const q = parseQuery();
    state.tab = SCREENS.some(s => s.id === q.tab) ? q.tab : 'rtm010';
    state.war = q.war;
    if (q.serviceId) state.rtm040 = { ...state.rtm040, serviceId: q.serviceId };
    if (q.guid) state.selectedTxGuid = q.guid;
    if (q.traceGuid) state.traceGuid = q.traceGuid;
    if (q.sqlRowKey) state.sqlRowKey = q.sqlRowKey;
    if (q.war) state.rtm040 = { ...state.rtm040, war: q.war };
    state.filters = loadFilters();

    buildShell(el);

    const run = () => loadAll().catch(err => OmAdmin.showErrorBanner(el, err.message));
    bindDelegates(el, run);

    document.getElementById('ws-screenTabs').addEventListener('click', e => {
      const btn = e.target.closest('.rtm-ws-screen-tab');
      if (btn) navigate(btn.dataset.screen);
    });

    document.getElementById('ws020-tabNav').addEventListener('click', e => {
      const tab = e.target.closest('.om-tab');
      if (tab) switchRtm020Tab(tab.dataset.tab);
    });

    document.getElementById('ws030-innerTabs').addEventListener('click', e => {
      const btn = e.target.closest('.rtm030-inner-tab');
      if (btn) switchInnerTab(btn.dataset.inner);
    });

    document.getElementById('ws030-warSelect').addEventListener('change', e => {
      state.war = e.target.value;
      pushQuery();
      renderRtm030();
    });

    document.getElementById('ws040-searchBtn').addEventListener('click', () => {
      state.rtm040 = read040FiltersFromDom();
      save040Filters(state.rtm040);
      renderRtm040();
    });
    document.getElementById('ws040-filterElapsed').addEventListener('change', () => {
      const wrap = document.getElementById('ws040-elapsedCustomWrap');
      if (wrap) wrap.style.display = document.getElementById('ws040-filterElapsed').value === 'custom' ? '' : 'none';
    });

    document.getElementById('ws050-searchBtn').addEventListener('click', () => {
      state.traceGuid = document.getElementById('ws050-guidSelect').value;
      pushQuery();
      run();
    });
    document.getElementById('ws050-guidSelect').addEventListener('change', e => {
      state.traceGuid = e.target.value;
      pushQuery();
      run();
    });

    document.getElementById('ws060-innerTabs').addEventListener('click', e => {
      const btn = e.target.closest('.rtm060-inner-tab');
      if (btn) switchRtm060Inner(btn.dataset.inner);
    });
    document.getElementById('ws060-searchBtn').addEventListener('click', () => {
      state.rtm060 = read060FiltersFromDom();
      save060Filters(state.rtm060);
      renderRtm060();
    });
    ['ws060-filterWar', 'ws060-filterElapsed'].forEach(id => {
      const node = document.getElementById(id);
      if (node) node.addEventListener('change', () => {
        state.rtm060 = read060FiltersFromDom();
        save060Filters(state.rtm060);
        renderRtm060();
      });
    });

    document.getElementById('ws100-searchBtn').addEventListener('click', () => {
      state.filters = read100FiltersFromDom();
      saveFilters(state.filters);
      renderRtm100();
    });
    ['ws100-filterCenter', 'ws100-filterInstance', 'ws100-filterWar', 'ws100-filterTime'].forEach(id => {
      const node = document.getElementById(id);
      if (node) node.addEventListener('change', () => {
        state.filters = read100FiltersFromDom();
        saveFilters(state.filters);
        renderRtm100();
      });
    });

    const read010Filters = () => ({
      center: document.getElementById('ws010-filterCenter').value,
      env: document.getElementById('ws010-filterEnv').value,
      instance: document.getElementById('ws010-filterInstance').value,
      war: document.getElementById('ws010-filterWar').value,
      timePoint: document.getElementById('ws010-filterTime').value
    });

    ['ws010-filterCenter', 'ws010-filterEnv', 'ws010-filterInstance', 'ws010-filterWar', 'ws010-filterTime'].forEach(id => {
      const node = document.getElementById(id);
      if (!node) return;
      if (state.filters[id.replace('ws010-filter', '').toLowerCase()] || state.filters[id.replace('ws010-filter', '')]) {
        /* restored below */
      }
      node.addEventListener('change', () => {
        state.filters = read010Filters();
        saveFilters(state.filters);
        renderRtm010();
      });
    });

    if (state.filters.center) document.getElementById('ws010-filterCenter').value = state.filters.center;
    if (state.filters.env) document.getElementById('ws010-filterEnv').value = state.filters.env;
    if (state.filters.timePoint) document.getElementById('ws010-filterTime').value = state.filters.timePoint;

    document.getElementById('ws-refreshBtn').addEventListener('click', () => run());
    document.getElementById('ws-autoToggleBtn').addEventListener('click', () => {
      state.autoEnabled = !state.autoEnabled;
      document.getElementById('ws-autoToggleBtn').textContent = state.autoEnabled ? '중지' : '시작';
      if (state.autoEnabled) startAuto(run);
      else stopAuto();
    });

    navigate(state.tab, {
      war: state.war,
      serviceId: state.rtm040.serviceId,
      guid: state.selectedTxGuid,
      traceGuid: state.traceGuid,
      sqlRowKey: state.sqlRowKey
    });
    await run();
    startAuto(run);
  }

  return { init, navigate, SCREENS };
})();
