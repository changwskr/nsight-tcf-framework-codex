/*
 * 런타임 진단 순서 가이드 (OM runtime-diagnosis-guide → PDMG)
 * POST /mgcoa9100S0  dto.includeDetails=Y
 */
(function () {
  const SCREENS = {
    hub: '/index.html#/rtdiag',
    statusCards: '/index.html#/rtdiag',
    cause: '/index.html#/rtdiag',
    incident: '/index.html#/rtdiag',
    thread: '/index.html#/rtdiag',
    jvm: '/index.html#/rtdiag',
    dbpool: '/index.html#/rtdiag',
    sql: '/index.html#/rtdiag',
    dominance: '/index.html#/rtdiag',
    activeTx: '/index.html#/rtdiag',
    occupancy: '/index.html#/rtdiag',
    txDetail: '/index.html#/mgcoa9100'
  };

  const STEPS = [
    { id: 0, short: '시작', title: '진단 시작', question: '순차 분석을 시작합니다' },
    { id: 1, short: '스냅샷', title: '1단계 · 30초 스냅샷', question: '지금 이상한가?' },
    { id: 2, short: '원인판정', title: '2단계 · 공식 판정', question: '원인 코드는 무엇인가?' },
    { id: 3, short: '장애흐름', title: '3단계 · 장애 패턴', question: '어떤 경로로 악화됐는가?' },
    { id: 4, short: '심층', title: '4단계 · 심층 분석', question: '왜 그런가?' },
    { id: 5, short: '거래', title: '5단계 · 현장 추적', question: '어떤 거래인가?' },
    { id: 6, short: '회고', title: '6단계 · 통합 회고', question: '나아졌는가?' }
  ];

  const DEEP_PATHS = {
    DB_POOL_EXHAUSTED: [
      { screen: 'dbpool', label: 'DB Pool 분석', preview: 'dbPool' },
      { screen: 'activeTx', label: '실행 중 거래', preview: 'activeTx' },
      { screen: 'occupancy', label: '업무 점유', preview: 'occupancy' },
      { screen: 'txDetail', label: '거래·Thread 상세', preview: 'txDetail' }
    ],
    GC_PRESSURE: [
      { screen: 'jvm', label: 'JVM 분석', preview: 'jvmGc' },
      { screen: 'statusCards', label: '핵심 상태 카드', preview: 'statusCards' },
      { screen: 'thread', label: 'Thread 분석', preview: 'thread' }
    ],
    CPU_OVERLOAD: [
      { screen: 'jvm', label: 'JVM 분석', preview: 'jvmCpu' },
      { screen: 'dominance', label: '자원 독점', preview: 'dominance' },
      { screen: 'thread', label: 'Thread 분석', preview: 'thread' }
    ],
    THREAD_DEADLOCK: [
      { screen: 'thread', label: 'Thread 분석', preview: 'thread' },
      { screen: 'dbpool', label: 'DB Pool', preview: 'dbPool' },
      { screen: 'activeTx', label: '실행 중 거래', preview: 'activeTx' }
    ],
    THREAD_SATURATION: [
      { screen: 'thread', label: 'Thread 분석', preview: 'thread' },
      { screen: 'dbpool', label: 'DB Pool', preview: 'dbPool' },
      { screen: 'activeTx', label: '실행 중 거래', preview: 'activeTx' },
      { screen: 'occupancy', label: '업무 점유', preview: 'occupancy' }
    ],
    SLOW_SQL: [
      { screen: 'sql', label: 'SQL 분석', preview: 'sql' },
      { screen: 'dbpool', label: 'DB Pool', preview: 'dbPool' },
      { screen: 'cause', label: '자동 원인판정', preview: 'cause' },
      { screen: 'txDetail', label: '거래·Thread 상세', preview: 'txDetail' }
    ],
    EXTERNAL_WAIT: [
      { screen: 'activeTx', label: '실행 중 거래', preview: 'activeTxExt' },
      { screen: 'incident', label: '장애 흐름', preview: 'incident' },
      { screen: 'txDetail', label: '거래·Thread 상세', preview: 'txDetail' }
    ],
    SERVICE_DOMINANCE: [
      { screen: 'occupancy', label: '업무 점유', preview: 'occupancy' },
      { screen: 'dominance', label: '자원 독점', preview: 'dominance' },
      { screen: 'activeTx', label: '실행 중 거래', preview: 'activeTx' },
      { screen: 'sql', label: 'SQL 분석', preview: 'sql' }
    ],
    BUSINESS_RESOURCE_DOMINANCE: [
      { screen: 'occupancy', label: '업무 점유', preview: 'occupancy' },
      { screen: 'dominance', label: '자원 독점', preview: 'dominance' },
      { screen: 'activeTx', label: '실행 중 거래', preview: 'activeTx' },
      { screen: 'dbpool', label: 'DB Pool', preview: 'dbPool' }
    ],
    BUSINESS_DOMINANCE: [
      { screen: 'occupancy', label: '업무 점유', preview: 'occupancy' },
      { screen: 'dominance', label: '자원 독점', preview: 'dominance' },
      { screen: 'activeTx', label: '실행 중 거래', preview: 'activeTx' }
    ],
    UNKNOWN: [
      { screen: 'hub', label: '런타임 진단', preview: 'hub' },
      { screen: 'statusCards', label: '핵심 상태 카드', preview: 'statusCards' },
      { screen: 'txDetail', label: '거래·Thread 상세', preview: 'txDetail' }
    ],
    NORMAL: [
      { screen: 'statusCards', label: '핵심 상태 카드', preview: 'statusCards' },
      { screen: 'hub', label: '런타임 진단', preview: 'hub' }
    ]
  };

  let state = { step: 0, maxReached: 0, deepSub: 0, body: null, relay: null };

  const targetBaseUrlEl = document.getElementById('targetBaseUrl');
  const optrEnoEl = document.getElementById('optrEno');
  const targetInfoEl = document.getElementById('targetInfo');

  function baseUrl() {
    return (targetBaseUrlEl && targetBaseUrlEl.value || 'http://localhost:8080').trim();
  }

  function buildHeader(serviceId) {
    return {
      sys_comm: {
        rms_svc_c: serviceId,
        sync_dsc: 'S',
        tr_sysid: 'PDMG-UI',
        ttl_ug_ync: 0,
        std_tgrm_rqr_rsp_dsc: 'Q',
        std_tgrm_lclc: 'KO',
        tr_trm_ipadr: '127.0.0.1',
        tr_dtm: new Date().toISOString().replace(/[-:TZ.]/g, '').slice(0, 14),
        tr_brc: '10001',
        scid: 'rtdiag',
        optr_eno: (optrEnoEl && optrEnoEl.value) || 'sysadmin'
      }
    };
  }

  function resolveDeepPath(primary) {
    if (!primary || primary === 'NORMAL') return DEEP_PATHS.NORMAL;
    if (primary.startsWith('BUSINESS')) return DEEP_PATHS.BUSINESS_RESOURCE_DOMINANCE;
    return DEEP_PATHS[primary] || DEEP_PATHS.UNKNOWN;
  }

  function statusLabel(s) {
    if (s === 'CRITICAL') return '위험';
    if (s === 'WARN') return '주의';
    if (s === 'UNKNOWN') return '미확인';
    return '정상';
  }

  function metricLevel(level) {
    if (level === 'critical') return 'critical';
    if (level === 'warn') return 'warn';
    return 'normal';
  }

  function esc(s) {
    if (s == null) return '-';
    return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  }

  function primaryOf(body) {
    return body?.primaryCauseCode || body?.causeAnalysis?.primaryCauseCode || 'NORMAL';
  }

  function parseDto(result) {
    let parsed;
    try {
      parsed = JSON.parse(result.responseBody || '{}');
    } catch (e) {
      throw new Error('응답 JSON 파싱 실패');
    }
    if (result.httpStatus >= 400 || (parsed.result && parsed.result.stdErrCode)) {
      const code = parsed.result?.stdErrCode || parsed.stdErrCode || ('HTTP ' + result.httpStatus);
      const msg = parsed.result?.stdErrMsgCntn || parsed.error || '조회 실패';
      throw new Error(code + ': ' + msg);
    }
    return parsed && parsed.dto && typeof parsed.dto === 'object' ? parsed.dto : parsed;
  }

  function renderPreview(type, body) {
    const cards = body.statusCardsAnalysis?.cards || [];
    const cause = body.causeAnalysis || {};
    switch (type) {
      case 'statusCards':
        return `<div class="dg-metric-grid">${cards.map(c =>
          `<div class="dg-metric ${metricLevel(c.level)}"><div class="lbl">${esc(c.label)}</div><div class="val">${esc(c.display)}</div></div>`
        ).join('')}</div>`;
      case 'dbPool': {
        const ex = body.dbPoolAnalysis?.exhaustion || {};
        const pools = (body.dbPoolAnalysis?.pools || []).slice(0, 4);
        const wait = body.dbPoolAnalysis?.dbWaitTransactions?.length || 0;
        const sql = body.dbPoolAnalysis?.executingSqlTransactions?.length || 0;
        return `
          <div class="dg-result-box ${ex.detected ? 'critical' : 'ok'}">${esc(ex.message)}</div>
          <div class="dg-metric-grid">
            <div class="dg-metric"><div class="lbl">Pending 합계</div><div class="val">${ex.aggregatePending ?? 0}</div></div>
            <div class="dg-metric"><div class="lbl">WAIT_DB</div><div class="val">${wait}건</div></div>
            <div class="dg-metric"><div class="lbl">EXECUTING_SQL</div><div class="val">${sql}건</div></div>
          </div>
          ${pools.length ? `<div class="dg-table-mini om-table-wrap"><table class="om-table"><thead><tr><th>업무</th><th>Active</th><th>Max</th><th>Pending</th></tr></thead><tbody>${pools.map(p => `<tr><td>${esc(p.businessCode)}</td><td>${p.active}</td><td>${p.maximum}</td><td>${p.pending}</td></tr>`).join('')}</tbody></table></div>` : ''}`;
      }
      case 'thread': {
        const sat = body.threadAnalysis?.saturation || {};
        return `
          <div class="dg-result-box ${sat.detected ? 'warn' : 'ok'}">${esc(sat.message)}</div>
          <div class="dg-metric-grid">
            <div class="dg-metric"><div class="lbl">Busy Ratio</div><div class="val">${body.threadAnalysis?.aggregateBusyRatio ?? '-'}%</div></div>
            <div class="dg-metric"><div class="lbl">Slow 거래</div><div class="val">${body.threadAnalysis?.aggregateSlowTransactionCount ?? 0}</div></div>
          </div>`;
      }
      case 'jvmGc': {
        const gc = body.jvmAnalysis?.gcPressure || {};
        const jvm = body.jvmAnalysis?.sharedJvm || {};
        return `
          <div class="dg-result-box ${gc.detected ? 'critical' : 'ok'}">${esc(gc.message)}</div>
          <div class="dg-metric-grid">
            <div class="dg-metric"><div class="lbl">Heap</div><div class="val">${jvm.heapRatio ?? '-'}%</div></div>
            <div class="dg-metric"><div class="lbl">GC (1분)</div><div class="val">${jvm.gcTimeLastMinuteMs ?? 0}ms</div></div>
          </div>`;
      }
      case 'jvmCpu': {
        const cpu = body.jvmAnalysis?.cpuOverload || {};
        const jvm = body.jvmAnalysis?.sharedJvm || {};
        return `
          <div class="dg-result-box ${cpu.detected ? 'critical' : 'ok'}">${esc(cpu.message)}</div>
          <div class="dg-metric-grid">
            <div class="dg-metric"><div class="lbl">Process CPU</div><div class="val">${jvm.processCpuRatio ?? '-'}%</div></div>
            <div class="dg-metric"><div class="lbl">DB Pending</div><div class="val">${cpu.dbPending ?? 0}</div></div>
          </div>`;
      }
      case 'sql': {
        const slow = body.sqlAnalysis?.slowSqlAlert || {};
        return `
          <div class="dg-result-box ${slow.detected ? 'warn' : 'ok'}">${esc(slow.message || body.primaryMessage)}</div>
          <div class="dg-metric-grid">
            <div class="dg-metric"><div class="lbl">Slow SQL</div><div class="val">${slow.aggregateSlowSqlCount ?? body.cards?.slowSqlCount ?? 0}</div></div>
            <div class="dg-metric"><div class="lbl">실행 중 SQL</div><div class="val">${body.sqlAnalysis?.runningSqlCount ?? 0}</div></div>
          </div>`;
      }
      case 'dominance': {
        const biz = body.dominanceAnalysis?.businessDominance || {};
        const svc = body.dominanceAnalysis?.serviceDominance || {};
        return `
          <div class="dg-result-box ${biz.detected || svc.detected ? 'warn' : 'ok'}">
            ${esc(biz.message)}<br>${esc(svc.message)}
          </div>
          <pre style="font-size:.82rem;color:var(--muted);white-space:pre-wrap;margin:0">${esc(biz.screenMessage)}</pre>`;
      }
      case 'occupancy': {
        const rows = (body.businessOccupancyAnalysis?.rows || []).slice(0, 5);
        return rows.length
          ? `<div class="dg-table-mini om-table-wrap"><table class="om-table"><thead><tr><th>업무</th><th>점유</th><th>DB</th><th>Slow</th></tr></thead><tbody>${rows.map(r => `<tr><td>${esc(r.businessCode)}</td><td>${r.ownershipPct}%</td><td>${esc(r.dbDisplay)}</td><td>${r.slowCount}</td></tr>`).join('')}</tbody></table></div>`
          : '<div class="om-empty">업무 점유 데이터 없음</div>';
      }
      case 'activeTx':
      case 'activeTxExt': {
        const rows = (body.activeTransactionListAnalysis?.rows || []).slice(0, 8);
        return rows.length
          ? `<div class="dg-table-mini om-table-wrap"><table class="om-table"><thead><tr><th>업무</th><th>ServiceId</th><th>단계</th><th>경과</th></tr></thead><tbody>${rows.map(r => `<tr><td>${esc(r.businessCode)}</td><td class="om-mono">${esc(r.serviceId)}</td><td>${esc(r.stepLabel)}</td><td>${esc(r.elapsedDisplay)}</td></tr>`).join('')}</tbody></table></div>`
          : '<div class="om-empty">실행 중 거래 없음</div>';
      }
      case 'cause': {
        const detected = (cause.causeTable || []).filter(r => r.detected).slice(0, 5);
        return detected.length
          ? `<div class="dg-table-mini om-table-wrap"><table class="om-table"><thead><tr><th>순위</th><th>Cause</th><th>메시지</th></tr></thead><tbody>${detected.map(r => `<tr${r.primary ? ' style="font-weight:600"' : ''}><td>${r.priority}</td><td class="om-mono">${esc(r.causeCode)}</td><td>${esc(r.message)}</td></tr>`).join('')}</tbody></table></div>`
          : '<div class="om-empty">징후 없음</div>';
      }
      case 'incident': {
        const flows = (body.incidentFlowAnalysis?.flows || []).filter(f => f.active || f.primary);
        return flows.length
          ? flows.map(f => {
            const steps = (f.steps || []).map(s =>
              `<span class="${s.active ? 'dg-flow-step-on' : ''}">${esc(s.label)}</span>`).join(' → ');
            return `<div class="dg-flow-mini ${f.primary ? 'primary' : (f.active ? 'active' : '')}"><strong>${esc(f.title)}</strong><div style="margin-top:6px;font-size:.82rem">${steps}</div></div>`;
          }).join('')
          : '<div class="om-empty">활성 장애 흐름 없음</div>';
      }
      case 'txDetail': {
        const detail = body.transactionDetailAnalysis || {};
        return `<div class="dg-metric-grid">
          <div class="dg-metric"><div class="lbl">실행 중 거래</div><div class="val">${detail.activeTransactions?.count ?? 0}</div></div>
          <div class="dg-metric"><div class="lbl">Slow 거래</div><div class="val">${detail.slowTransactions?.count ?? 0}</div></div>
          <div class="dg-metric"><div class="lbl">Thread 상세</div><div class="val">${detail.threads?.count ?? 0}</div></div>
        </div>`;
      }
      case 'hub':
        return `<div class="dg-metric-grid">
          <div class="dg-metric"><div class="lbl">통합 상태</div><div class="val">${statusLabel(body.overallStatus)}</div></div>
          <div class="dg-metric"><div class="lbl">PRIMARY</div><div class="val om-mono">${esc(primaryOf(body))}</div></div>
          <div class="dg-metric"><div class="lbl">Findings</div><div class="val">${(body.findings || []).length}</div></div>
        </div><div class="dg-result-box">${esc(body.primaryMessage)}</div>`;
      default:
        return '<div class="om-empty">미리보기 없음</div>';
    }
  }

  function renderStep0() {
    return `
      <div class="dg-panel-guide">
        이 가이드는 <strong>0→6단계 순차 분석</strong>으로 pdmg-service 상태를 확인합니다.
      </div>
      <ul class="dg-checklist">
        <li>1단계: 8개 핵심 지표로 이상 여부 확인</li>
        <li>2단계: PRIMARY Cause 공식 판정</li>
        <li>3단계: 장애 악화 경로 확인</li>
        <li>4단계: PRIMARY별 심층 분석</li>
        <li>5단계: 실행 중 거래·ServiceId 추적</li>
        <li>6단계: 조치 후 회고·복귀 확인</li>
      </ul>
      <div class="dg-result-box ok" style="margin-top:14px">
        Thread·JVM·Heap · Hikari DB Pool · 30초 자동 갱신 · includeDetails=Y
      </div>`;
  }

  function renderStep1(body) {
    const status = body.statusCardsAnalysis?.overallStatus || body.overallStatus || 'NORMAL';
    const abnormal = status !== 'NORMAL';
    const cards = body.statusCardsAnalysis?.cards || [];
    const hints = [];
    cards.forEach(c => {
      if (c.level === 'critical' || c.level === 'warn') hints.push(`${c.label}: ${c.display}`);
    });
    return `
      <div class="dg-panel-guide">8개 지표로 1차 이상 여부를 확인합니다.</div>
      ${renderPreview('statusCards', body)}
      ${abnormal
        ? `<div class="dg-result-box warn"><strong>이상 징후:</strong> ${hints.join(' · ') || body.primaryMessage}</div>`
        : `<div class="dg-result-box ok">현재 통합 상태 ${statusLabel(status)} — 이상 징후 없음.</div>`}`;
  }

  function renderStep2(body) {
    const cause = body.causeAnalysis || {};
    const primary = primaryOf(body);
    const status = body.overallStatus || 'NORMAL';
    const cls = status === 'CRITICAL' ? 'critical' : (status === 'NORMAL' ? 'ok' : 'warn');
    const path = resolveDeepPath(primary);
    return `
      <div class="dg-panel-guide">PRIMARY Cause를 확인합니다. 4단계에서 <strong>${path.length}개</strong> 심층 화면을 순서대로 분석합니다.</div>
      <div class="dg-result-box ${cls}">
        <strong>PRIMARY:</strong> <span class="om-mono">${esc(primary)}</span><br>
        ${esc(cause.primaryMessage || body.primaryMessage)}
      </div>
      ${renderPreview('cause', body)}
      <div style="margin-top:10px;font-size:.85rem;color:var(--muted)">
        4단계 예정: ${path.map(p => p.label).join(' → ')}
      </div>`;
  }

  function renderStep3(body) {
    const flows = body.incidentFlowAnalysis?.flows || [];
    const matched = flows.find(f => f.primary) || flows.find(f => f.active);
    return `
      <div class="dg-panel-guide">PRIMARY와 일치하는 장애 흐름의 <strong>활성 단계</strong>를 확인합니다.</div>
      ${matched ? `<div class="dg-result-box warn">매칭 흐름: ${esc(matched.section)} ${esc(matched.title)} (${esc(matched.causeCode)})</div>` : ''}
      ${flows.map(f => {
        const cls = f.primary ? 'primary' : (f.active ? 'active' : '');
        const badge = f.primary ? '<span class="om-chip critical">PRIMARY</span>' : (f.active ? '<span class="om-chip warn">징후</span>' : '');
        const steps = (f.steps || []).map(s =>
          `<div style="padding:4px 0;font-size:.84rem${s.active ? ';color:#d97706;font-weight:500' : ''}">${s.order}. ${esc(s.label)} — ${esc(s.detail)}</div>`).join('');
        return `<div class="dg-flow-mini ${cls}" style="margin-bottom:10px"><div style="display:flex;gap:8px;align-items:center;margin-bottom:6px"><strong>${esc(f.title)}</strong>${badge}</div>${steps}</div>`;
      }).join('') || renderPreview('incident', body)}`;
  }

  function renderStep4(body) {
    const primary = primaryOf(body);
    const path = resolveDeepPath(primary);
    const sub = Math.min(state.deepSub, path.length - 1);
    const item = path[sub];
    const subChips = path.map((p, i) =>
      `<span class="dg-sub-chip ${i < sub ? 'done' : ''} ${i === sub ? 'current' : ''}">${i + 1}. ${p.label}</span>`).join('');
    return `
      <div class="dg-panel-guide">
        PRIMARY <span class="om-mono">${esc(primary)}</span> 기준 심층 분석
        <strong>${sub + 1}/${path.length}</strong> — ${esc(item.label)}
      </div>
      <div class="dg-sub-progress">${subChips}</div>
      ${renderPreview(item.preview, body)}
      <div style="margin-top:12px">
        <a class="dg-link" href="${SCREENS[item.screen]}">${esc(item.label)} 관련 ↗</a>
      </div>`;
  }

  function renderStep5(body) {
    return `
      <div class="dg-panel-guide">실행 중 거래·ServiceId를 확인합니다.</div>
      ${renderPreview('activeTx', body)}
      <div style="margin-top:10px">
        <a class="dg-link" href="${SCREENS.txDetail}">전문 테스트 (원본 JSON) ↗</a>
      </div>`;
  }

  function renderStep6(body) {
    const status = body.overallStatus || 'NORMAL';
    const primary = primaryOf(body);
    const recovered = status === 'NORMAL' && primary === 'NORMAL';
    const cards = body.cards || {};
    return `
      <div class="dg-panel-guide">조치 후 상태가 개선되었는지 통합 지표로 확인합니다.</div>
      <div class="dg-result-box ${recovered ? 'ok' : 'warn'}">
        ${recovered ? '✓ PRIMARY NORMAL 복귀 — 장애 징후 해소' : `아직 ${statusLabel(status)} · PRIMARY ${esc(primary)} — 추가 조치 필요`}
      </div>
      <div class="dg-metric-grid">
        <div class="dg-metric ${cards.dbPending > 0 ? 'warn' : 'normal'}"><div class="lbl">DB Pending</div><div class="val">${cards.dbPending ?? 0}</div></div>
        <div class="dg-metric"><div class="lbl">Thread busy</div><div class="val">${cards.threadBusyRatio ?? '-'}%</div></div>
        <div class="dg-metric"><div class="lbl">JVM CPU</div><div class="val">${cards.jvmCpuRatio ?? '-'}%</div></div>
        <div class="dg-metric"><div class="lbl">Slow SQL</div><div class="val">${cards.slowSqlCount ?? 0}</div></div>
      </div>
      <ul class="dg-checklist">
        <li class="${recovered ? 'done' : ''}">PRIMARY NORMAL 복귀</li>
        <li class="${(cards.dbPending ?? 0) === 0 ? 'done' : ''}">DB Pending = 0</li>
        <li class="${(cards.threadBusyRatio ?? 0) < 85 ? 'done' : ''}">Thread busy &lt; 85%</li>
      </ul>`;
  }

  function renderPanelContent() {
    const body = state.body;
    if (!body && state.step > 0) return '<div class="om-empty">데이터 로딩 중...</div>';
    switch (state.step) {
      case 0: return renderStep0();
      case 1: return renderStep1(body);
      case 2: return renderStep2(body);
      case 3: return renderStep3(body);
      case 4: return renderStep4(body);
      case 5: return renderStep5(body);
      case 6: return renderStep6(body);
      default: return '';
    }
  }

  function renderProgress() {
    return STEPS.map(s => {
      const done = s.id < state.step || (s.id === state.step && s.id === 6);
      const current = s.id === state.step;
      const disabled = s.id > state.maxReached && s.id !== state.step;
      const cls = [done && !current ? 'done' : '', current ? 'current' : '', disabled ? 'disabled' : ''].filter(Boolean).join(' ');
      return `<div class="dg-progress-item ${cls}" data-goto="${s.id}" title="${esc(s.title)}">
        <div class="dg-progress-dot">${done && !current ? '✓' : s.id}</div>
        <div class="dg-progress-label">${s.short}</div>
      </div>`;
    }).join('');
  }

  function updateNavButtons() {
    const prev = document.getElementById('btnPrev');
    const next = document.getElementById('btnNext');
    if (!prev || !next) return;
    prev.disabled = state.step <= 0;
    if (state.step === 4 && state.body) {
      const path = resolveDeepPath(primaryOf(state.body));
      const hasMoreDeep = state.deepSub < path.length - 1;
      next.textContent = hasMoreDeep
        ? `다음 심층 (${state.deepSub + 2}/${path.length}) →`
        : '5단계 · 거래 추적 →';
      next.disabled = false;
    } else if (state.step === 6) {
      next.textContent = '진단 완료';
      next.disabled = true;
    } else {
      next.textContent = state.step === 0 ? '분석 시작 →' : '다음 단계 →';
      next.disabled = false;
    }
  }

  function renderWizard() {
    const stepMeta = STEPS[state.step];
    document.getElementById('dgProgress').innerHTML = renderProgress();
    document.getElementById('dgPanelTitle').textContent = stepMeta.title;
    document.getElementById('dgPanelQuestion').textContent = stepMeta.question;
    document.getElementById('dgPanelBody').innerHTML = renderPanelContent();
    updateNavButtons();
    document.querySelectorAll('.dg-progress-item:not(.disabled)').forEach(el => {
      el.onclick = () => goToStep(Number(el.dataset.goto));
    });
  }

  function goToStep(step, resetDeep) {
    state.step = Math.max(0, Math.min(6, step));
    state.maxReached = Math.max(state.maxReached, state.step);
    if (resetDeep !== false && step === 4) state.deepSub = 0;
    if (state.step !== 4) state.deepSub = 0;
    try {
      location.hash = `step=${state.step}`;
    } catch (e) { /* ignore */ }
    renderWizard();
  }

  function nextStep() {
    if (state.step === 4 && state.body) {
      const path = resolveDeepPath(primaryOf(state.body));
      if (state.deepSub < path.length - 1) {
        state.deepSub++;
        renderWizard();
        return;
      }
    }
    if (state.step === 0 && !state.body) {
      loadData().then(() => goToStep(1, false)).catch(showError);
      return;
    }
    if (state.step < 6) goToStep(state.step + 1, false);
  }

  function prevStep() {
    if (state.step === 4 && state.deepSub > 0) {
      state.deepSub--;
      renderWizard();
      return;
    }
    if (state.step > 0) goToStep(state.step - 1, false);
  }

  function showError(err) {
    const msg = (err && err.message) || String(err);
    document.getElementById('guideMessage').textContent = msg;
    document.getElementById('guideBanner').className = 'om-status-banner critical';
    document.getElementById('guideMain').className = 'om-status-main critical';
    document.getElementById('guideMain').textContent = '진단 조회 실패';
    document.getElementById('rtmStatus').textContent = '오류';
  }

  async function loadData() {
    const result = await PdmgServiceClient.postPath(
      baseUrl(),
      '/mgcoa9100S0',
      {
        hdr_nhnis: buildHeader('mgcoa9100S0'),
        dto: { includeDetails: 'Y' }
      },
      15000,
      'mgcoa9100S0'
    );
    const body = parseDto(result);
    state.body = body;
    state.relay = result;

    const primary = primaryOf(body);
    const status = body.overallStatus || 'NORMAL';
    const cls = status === 'CRITICAL' ? 'critical' : (status === 'WARN' ? 'warn' : 'normal');

    document.getElementById('rtmStatus').textContent =
      `${result.elapsedMs}ms · ${body.checkedAt || '-'} · 단계 ${state.step}/6`;
    document.getElementById('guideBanner').className = `om-status-banner ${cls}`;
    document.getElementById('guideMain').className = `om-status-main ${cls}`;
    document.getElementById('guideMain').textContent =
      state.step === 0
        ? '순차 진단 가이드 — 분석 시작을 누르세요'
        : `${STEPS[state.step].title} · PRIMARY ${primary}`;
    document.getElementById('guideMessage').textContent = body.primaryMessage || '-';
    if (state.step > 0) renderWizard();
  }

  async function init() {
    if (typeof window.nsightResolveTargets === 'function') {
      try {
        const t = await window.nsightResolveTargets();
        if (t && t.serviceBaseUrl && targetBaseUrlEl) targetBaseUrlEl.value = t.serviceBaseUrl;
        if (targetInfoEl) targetInfoEl.textContent = baseUrl();
      } catch (e) {
        if (targetInfoEl) targetInfoEl.textContent = baseUrl();
      }
    } else if (targetInfoEl) {
      targetInfoEl.textContent = baseUrl();
    }

    const hash = (location.hash || '').match(/step=(\d)/);
    const initialStep = hash ? Number(hash[1]) : 0;
    state.step = initialStep;
    state.maxReached = initialStep;

    document.getElementById('btnPrev').onclick = prevStep;
    document.getElementById('btnNext').onclick = nextStep;
    document.getElementById('refreshBtn').onclick = () => loadData().catch(showError);
    if (targetBaseUrlEl) {
      targetBaseUrlEl.addEventListener('change', () => {
        if (targetInfoEl) targetInfoEl.textContent = baseUrl();
      });
    }

    renderWizard();
    if (initialStep > 0) {
      await loadData().catch(showError);
    }
    window.setInterval(() => {
      if (state.step > 0) loadData().catch(() => {});
    }, 30000);
  }

  init();
})();
