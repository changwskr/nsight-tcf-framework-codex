/**
 * RTM-070 장애 진단 및 보고서 — 브라우저 초안·이력
 */
window.OmRuntimeIncident = (function () {
  const DRAFT_KEY = 'nsight.om.rtm070.draft';
  const SEQ_KEY = 'nsight.om.rtm070.seq';
  const ARCHIVE_KEY = 'nsight.om.rtm070.archive';
  const ARCHIVE_MAX = 50;

  const WORKFLOW = [
    { value: 'CANDIDATE', label: '원인 후보' },
    { value: 'INVESTIGATING', label: '확인 중' },
    { value: 'CAUSE_CONFIRMED', label: '원인 확정' },
    { value: 'MITIGATION', label: '완화 조치' },
    { value: 'RECOVERED', label: '정상화' },
    { value: 'PREVENTION', label: '재발방지 진행' },
    { value: 'CLOSED', label: '종료' }
  ];

  function workflowLabel(value) {
    const row = WORKFLOW.find(w => w.value === value);
    return row ? row.label : value || '-';
  }

  function todayKey() {
    const d = new Date();
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}${m}${day}`;
  }

  function nextIncidentId(suggestion) {
    if (suggestion && /^INC-\d{8}-\d{3}$/.test(suggestion)) {
      return suggestion;
    }
    try {
      const raw = localStorage.getItem(SEQ_KEY);
      const seq = raw ? JSON.parse(raw) : {};
      const key = todayKey();
      const next = (seq[key] || 0) + 1;
      seq[key] = next;
      localStorage.setItem(SEQ_KEY, JSON.stringify(seq));
      return `INC-${key}-${String(next).padStart(3, '0')}`;
    } catch (e) {
      return `INC-${todayKey()}-001`;
    }
  }

  function loadDraft() {
    try {
      const raw = localStorage.getItem(DRAFT_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch (e) {
      return null;
    }
  }

  function saveDraft(draft) {
    draft.updatedAt = new Date().toISOString();
    localStorage.setItem(DRAFT_KEY, JSON.stringify(draft));
    return draft;
  }

  function clearDraft() {
    localStorage.removeItem(DRAFT_KEY);
  }

  function archiveReport(draft) {
    try {
      const raw = localStorage.getItem(ARCHIVE_KEY);
      const rows = raw ? JSON.parse(raw) : [];
      rows.unshift({ ...draft, archivedAt: new Date().toISOString() });
      localStorage.setItem(ARCHIVE_KEY, JSON.stringify(rows.slice(0, ARCHIVE_MAX)));
    } catch (e) {
      /* ignore */
    }
  }

  function readArchive() {
    try {
      const raw = localStorage.getItem(ARCHIVE_KEY);
      return raw ? JSON.parse(raw) : [];
    } catch (e) {
      return [];
    }
  }

  function getOperator(session) {
    if (!session) return 'GUEST';
    return session.userName || session.userId || 'GUEST';
  }

  function canConfirmCause(session) {
    return Boolean(session && session.userId);
  }

  function canNormalize(session) {
    if (!session || !session.userId) return false;
    const group = String(session.authGroupId || session.authGroupName || '').toUpperCase();
    const name = String(session.authGroupName || '');
    return group.includes('ADMIN')
      || group.includes('NORM')
      || group.includes('MANAGER')
      || name.includes('관리')
      || name.includes('운영관리');
  }

  function createDraftFromScreen(screen, session) {
    const header = screen.header || {};
    const auto = screen.autoJudgment || {};
    const id = nextIncidentId(header.incidentIdSuggestion);
    return {
      incidentId: id,
      occurredAt: header.occurredAt || '',
      targetDisplay: header.targetDisplay || '',
      impactServiceId: header.impactServiceId || '',
      severity: header.severityDefault || 'Major',
      workflowStatus: auto.suggestedWorkflowStatus || 'CANDIDATE',
      autoJudgment: {
        primaryCauseCode: auto.primaryCauseCode || '-',
        primaryConfidence: auto.primaryConfidence || '-',
        secondaryCauseCode: auto.secondaryCauseCode || '-',
        secondaryConfidence: auto.secondaryConfidence || '-',
        evidenceSummary: auto.evidenceSummary || '-',
        primaryMessage: auto.primaryMessage || ''
      },
      operator: {
        confirmedCause: '',
        directCause: '',
        spreadCause: '',
        tempAction: '',
        permanentAction: '',
        retestPlan: ''
      },
      actionHistory: [],
      attachments: {
        snapshot: null,
        slowTransactions: null,
        slowSql: null
      },
      createdBy: getOperator(session),
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };
  }

  function mergeDraftWithScreen(draft, screen) {
    if (!draft) return createDraftFromScreen(screen, OmAdmin.getSession());
    const auto = screen.autoJudgment || {};
    draft.autoJudgment = {
      primaryCauseCode: auto.primaryCauseCode || draft.autoJudgment?.primaryCauseCode,
      primaryConfidence: auto.primaryConfidence || draft.autoJudgment?.primaryConfidence,
      secondaryCauseCode: auto.secondaryCauseCode || draft.autoJudgment?.secondaryCauseCode,
      secondaryConfidence: auto.secondaryConfidence || draft.autoJudgment?.secondaryConfidence,
      evidenceSummary: auto.evidenceSummary || draft.autoJudgment?.evidenceSummary,
      primaryMessage: auto.primaryMessage || draft.autoJudgment?.primaryMessage
    };
    if (!draft.incidentId) {
      draft.incidentId = nextIncidentId(screen.header?.incidentIdSuggestion);
    }
    return draft;
  }

  function attachSnapshot(cache) {
    if (!cache || !cache.body) return null;
    const b = cache.body;
    return {
      type: 'snapshot',
      attachedAt: new Date().toISOString(),
      checkedAt: b.checkedAt,
      overallStatus: b.overallStatus,
      primaryCauseCode: b.primaryCauseCode,
      primaryMessage: b.primaryMessage,
      dominantBusinessCode: b.dominantBusinessCode,
      dominantServiceId: b.dominantServiceId
    };
  }

  function attachSlowTransactions(cache) {
    if (!cache || !cache.body) return null;
    const rows = (cache.body.slowTransactions || []).slice(0, 20);
    return {
      type: 'slowTransactions',
      attachedAt: new Date().toISOString(),
      count: rows.length,
      rows: rows.map(r => ({
        businessCode: r.businessCode,
        serviceId: r.serviceId,
        guid: r.guid,
        elapsedMs: r.elapsedMs,
        lastStep: r.lastStep
      }))
    };
  }

  function attachSlowSql(cache) {
    if (!cache || !cache.body) return null;
    const rows = (cache.body.slowSql || []).slice(0, 20);
    return {
      type: 'slowSql',
      attachedAt: new Date().toISOString(),
      count: rows.length,
      rows: rows.map(r => ({
        serviceId: r.serviceId,
        mapperSql: r.mapperSql || r.mapperId,
        elapsedMs: r.elapsedMs,
        success: r.success
      }))
    };
  }

  function addActionRow(draft, row) {
    const list = draft.actionHistory || [];
    list.push({
      seq: list.length + 1,
      action: row.action || '',
      owner: row.owner || '',
      startedAt: row.startedAt || '',
      completedAt: row.completedAt || '',
      result: row.result || ''
    });
    draft.actionHistory = list;
    return draft;
  }

  function nowTimeShort() {
    const d = new Date();
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  }

  return {
    DRAFT_KEY,
    WORKFLOW,
    workflowLabel,
    nextIncidentId,
    loadDraft,
    saveDraft,
    clearDraft,
    archiveReport,
    readArchive,
    canConfirmCause,
    canNormalize,
    createDraftFromScreen,
    mergeDraftWithScreen,
    attachSnapshot,
    attachSlowTransactions,
    attachSlowSql,
    addActionRow,
    nowTimeShort,
    getOperator
  };
})();
