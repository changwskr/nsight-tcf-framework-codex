/**
 * OM 런타임·장애진단 공통 유틸 (tcf-ui)
 * OM.Runtime.inquiry 응답을 화면 간 공유·캐시합니다.
 */
window.OmRuntime = (function () {
  const CACHE_KEY = 'nsight.om.runtimeDiagnostics';
  const HISTORY_KEY = 'nsight.om.runtimeHistory';
  const HISTORY_MAX = 30;

  function statusLabel(overallStatus) {
    if (overallStatus === 'CRITICAL') return '위험';
    if (overallStatus === 'WARN') return '주의';
    if (overallStatus === 'UNKNOWN') return '미확인';
    return '정상';
  }

  function statusClass(overallStatus) {
    const s = String(overallStatus || 'NORMAL').toUpperCase();
    if (s === 'CRITICAL') return 'critical';
    if (s === 'WARN' || s === 'WARNING') return 'warn';
    if (s === 'NORMAL' || s === 'OK') return 'normal';
    return 'unknown';
  }

  function formatPct(value) {
    if (value == null || value === '') return '-';
    return `${value}%`;
  }

  function formatElapsed(ms) {
    const value = Number(ms);
    if (Number.isNaN(value)) return '-';
    if (value >= 1000) return `${(value / 1000).toFixed(1)}초`;
    return `${value}ms`;
  }

  function readCache() {
    try {
      const raw = sessionStorage.getItem(CACHE_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch (e) {
      return null;
    }
  }

  function writeCache(payload) {
    try {
      sessionStorage.setItem(CACHE_KEY, JSON.stringify({
        savedAt: new Date().toISOString(),
        body: payload.body,
        relay: payload.relay
      }));
    } catch (e) {
      /* ignore quota */
    }
  }

  function appendHistory(body, relay) {
    try {
      const raw = localStorage.getItem(HISTORY_KEY);
      const rows = raw ? JSON.parse(raw) : [];
      rows.unshift({
        checkedAt: body.checkedAt || new Date().toISOString(),
        overallStatus: body.overallStatus || 'NORMAL',
        primaryCauseCode: body.primaryCauseCode || '-',
        primaryMessage: body.primaryMessage || '-',
        dominantBusinessCode: body.dominantBusinessCode || '-',
        elapsedMs: relay && relay.elapsedMs != null ? relay.elapsedMs : null
      });
      localStorage.setItem(HISTORY_KEY, JSON.stringify(rows.slice(0, HISTORY_MAX)));
    } catch (e) {
      /* ignore */
    }
  }

  function readHistory() {
    try {
      const raw = localStorage.getItem(HISTORY_KEY);
      return raw ? JSON.parse(raw) : [];
    } catch (e) {
      return [];
    }
  }

  function clearHistory() {
    localStorage.removeItem(HISTORY_KEY);
  }

  async function loadDiagnostics(options) {
    const opts = options || {};
    const body = opts.body || { includeDetails: opts.includeDetails === false ? 'N' : 'Y' };
    if (opts.useCache) {
      const cached = readCache();
      if (cached && cached.body) {
        return { body: cached.body, relay: cached.relay || {}, fromCache: true };
      }
    }
    const result = await OmAdmin.inquiry('runtimeDiagnostics', body);
    writeCache(result);
    appendHistory(result.body, result.relay);
    return { ...result, fromCache: false };
  }

  function renderStatusBanner(ids, body) {
    const status = statusClass(body.overallStatus);
    const banner = document.getElementById(ids.banner);
    const main = document.getElementById(ids.main);
    const message = document.getElementById(ids.message);
    const meta = document.getElementById(ids.meta);
    if (banner) banner.className = `om-status-banner ${status}`;
    if (main) {
      main.className = `om-status-main ${status}`;
      main.textContent = `현재 상태: ${statusLabel(body.overallStatus)}`;
    }
    if (message) message.textContent = body.primaryMessage || '현재 주요 병목이 없습니다.';
    if (meta) {
      meta.textContent =
        `원인 코드 ${body.primaryCauseCode || '-'} · 영향 업무 ${body.dominantBusinessCode || '-'}`
        + ` · ServiceId ${body.dominantServiceId || '-'} · SQL ${body.dominantSqlId || '-'}`;
    }
  }

  function setCheckedAt(elId, body, relay) {
    const el = document.getElementById(elId);
    if (!el) return;
    const ms = relay && relay.elapsedMs != null ? `${relay.elapsedMs}ms · ` : '';
    el.textContent = `${ms}점검 ${body.checkedAt || '-'}`;
  }

  const SCREEN_LINKS = [
    { id: 'RTM-WS', label: '런타임 워크스페이스', href: '/om/admin/runtime-workspace.html' },
    { id: 'RTM-010', label: '통합 진단 대시보드', href: '/om/admin/runtime-workspace.html?tab=rtm010' },
    { id: 'RTM-020', label: 'Tomcat 인스턴스 상세', href: '/om/admin/runtime-workspace.html?tab=rtm020' },
    { id: 'RTM-030', label: 'WAR 자원 상세', href: '/om/admin/runtime-workspace.html?tab=rtm030' },
    { id: 'RTM-060', label: 'Slow SQL·외부연계', href: '/om/admin/runtime-workspace.html?tab=rtm060' },
    { id: 'RTM-040', label: '실행 거래', href: '/om/admin/runtime-workspace.html?tab=rtm040' },
    { id: 'RTM-050', label: '거래 추적 상세', href: '/om/admin/runtime-workspace.html?tab=rtm050' },
    { id: 'RTM-070', label: '장애 진단 및 보고서', href: '/om/admin/runtime-cause-analysis.html' },
    { id: 'RTM-080', label: '장애 이력', href: '/om/admin/runtime-incident-history.html' },
    { id: 'RTM-090', label: '임계치·수집설정', href: '/om/admin/runtime-threshold-policy.html' },
    { id: 'RTM-100', label: '자동 원인 추적', href: '/om/admin/runtime-workspace.html?tab=rtm100' }
  ];

  return {
    CACHE_KEY,
    HISTORY_KEY,
    HISTORY_MAX,
    statusLabel,
    statusClass,
    formatPct,
    formatElapsed,
    readCache,
    writeCache,
    loadDiagnostics,
    appendHistory,
    readHistory,
    clearHistory,
    renderStatusBanner,
    setCheckedAt,
    SCREEN_LINKS
  };
})();
