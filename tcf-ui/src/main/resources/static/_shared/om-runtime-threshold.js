/**
 * RTM-090 임계치·수집설정 — 로컬 초안·변경요청·승인 큐
 */
window.OmRuntimeThreshold = (function () {
  const EFFECTIVE_KEY = 'nsight.om.rtm090.effective';
  const PENDING_KEY = 'nsight.om.rtm090.pending';
  const APPROVED_KEY = 'nsight.om.rtm090.approved';
  const PENDING_MAX = 100;

  function readJson(key, fallback) {
    try {
      const raw = localStorage.getItem(key);
      return raw ? JSON.parse(raw) : fallback;
    } catch (e) {
      return fallback;
    }
  }

  function writeJson(key, value) {
    localStorage.setItem(key, JSON.stringify(value));
  }

  function loadEffective(defaults) {
    const saved = readJson(EFFECTIVE_KEY, null);
    if (!saved || !saved.metrics) {
      return cloneMetrics(defaults || []);
    }
    return mergeMetrics(defaults || [], saved.metrics);
  }

  function cloneMetrics(rows) {
    return (rows || []).map(r => ({ ...r }));
  }

  function mergeMetrics(defaults, saved) {
    const byKey = {};
    (saved || []).forEach(r => { if (r.metricKey) byKey[r.metricKey] = r; });
    return (defaults || []).map(d => ({ ...d, ...(byKey[d.metricKey] || {}) }));
  }

  function saveEffective(metrics) {
    writeJson(EFFECTIVE_KEY, { metrics, updatedAt: new Date().toISOString() });
  }

  function loadPending() {
    return readJson(PENDING_KEY, []);
  }

  function loadApproved() {
    return readJson(APPROVED_KEY, []);
  }

  function addPendingRequest(req) {
    const rows = loadPending();
    rows.unshift({
      ...req,
      id: 'CHG-' + Date.now(),
      status: 'PENDING',
      requestedAt: new Date().toISOString()
    });
    writeJson(PENDING_KEY, rows.slice(0, PENDING_MAX));
    return rows[0];
  }

  function canApprove(session) {
    if (!session || !session.userId) return false;
    const group = String(session.authGroupId || session.authGroupName || '').toUpperCase();
    const name = String(session.authGroupName || '');
    return group.includes('ADMIN') || name.includes('관리') || name.includes('승인');
  }

  function approveRequest(id, approver) {
    const pending = loadPending();
    const idx = pending.findIndex(r => r.id === id);
    if (idx < 0) return null;
    const item = pending[idx];
    item.status = 'APPROVED';
    item.approvedAt = new Date().toISOString();
    item.approver = approver || item.approver;
    pending.splice(idx, 1);
    writeJson(PENDING_KEY, pending);

    const approved = loadApproved();
    approved.unshift(item);
    writeJson(APPROVED_KEY, approved.slice(0, PENDING_MAX));

    if (item.metricKey && item.afterValue != null) {
      applyMetricChange(item);
    }
    return item;
  }

  function rejectRequest(id) {
    const pending = loadPending();
    const idx = pending.findIndex(r => r.id === id);
    if (idx < 0) return;
    pending[idx].status = 'REJECTED';
    pending.splice(idx, 1);
    writeJson(PENDING_KEY, pending);
  }

  function applyMetricChange(req) {
    const effective = readJson(EFFECTIVE_KEY, { metrics: [] });
    const metrics = effective.metrics || [];
    const row = metrics.find(m => m.metricKey === req.metricKey);
    if (row) {
      if (req.afterWarn != null) row.warnValue = req.afterWarn;
      if (req.afterCritical != null) row.criticalValue = req.afterCritical;
      if (req.afterConsecutive != null) row.consecutiveCount = req.afterConsecutive;
    } else if (req.metricSnapshot) {
      metrics.push(req.metricSnapshot);
    }
    writeJson(EFFECTIVE_KEY, { metrics, updatedAt: new Date().toISOString() });
  }

  function formatMetricValues(row) {
    const unit = row.unit || '';
    const w = row.warnValue;
    const c = row.criticalValue;
    if (unit === '%') return `주의 ${w}% / 위험 ${c}% / 연속 ${row.consecutiveCount}`;
    if (unit === '건') return `주의 ${w} / 위험 ${c} / 연속 ${row.consecutiveCount}`;
    return `${w} / ${c}`;
  }

  function getRequester(session) {
    if (!session) return 'GUEST';
    return session.userName || session.userId || 'GUEST';
  }

  return {
    EFFECTIVE_KEY,
    PENDING_KEY,
    loadEffective,
    saveEffective,
    loadPending,
    loadApproved,
    addPendingRequest,
    approveRequest,
    rejectRequest,
    canApprove,
    formatMetricValues,
    getRequester
  };
})();
