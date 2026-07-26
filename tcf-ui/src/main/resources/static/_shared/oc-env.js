const API = '/api/oc/env';
let lastRunId = null;

function showStatus(msg, type = 'info') {
    const bar = document.getElementById('statusBar');
    bar.textContent = msg;
    bar.className = `status-bar ${type}`;
    bar.classList.remove('hidden');
}

function escapeHtml(t) {
    const d = document.createElement('div');
    d.textContent = t ?? '';
    return d.innerHTML;
}

function isSuccess(data) {
    return data?.error?.resultCode === 'SUCCESS';
}

function errMsg(data) {
    return data?.error?.resultMessage || data?.error?.detailMessage || '오류';
}

function apiHeaders() {
    return { 'X-GUID': crypto.randomUUID(), 'X-USER-ID': 'ARCHITECT' };
}

function assessLabel(status) {
    const map = { PASS: '통과', WARN: '주의', FAIL: '실패', INFO: '참고', EXCEPTION: '오류' };
    return map[status] || status;
}

function assessClass(status) {
    if (status === 'PASS') return 'ok';
    if (status === 'FAIL' || status === 'EXCEPTION') return 'no';
    if (status === 'WARN') return '';
    return '';
}

function settingLabel(status) {
    const map = { MATCH: '일치', WARN: '주의', INFO: '참고' };
    return map[status] || status;
}

function settingClass(status) {
    if (status === 'MATCH') return 'ok';
    if (status === 'WARN') return 'no';
    return '';
}

function settingRowClass(status) {
    const s = (status || 'info').toLowerCase();
    if (s === 'match') return 'match';
    if (s === 'warn') return 'warn';
    return 'info';
}

function assessRowClass(status) {
    const s = (status || '').toLowerCase();
    return s === 'pass' ? 'pass' : s === 'warn' ? 'warn' : s === 'fail' || s === 'exception' ? 'fail' : 'info';
}

function sevClass(severity) {
    const s = (severity || '').toUpperCase();
    if (s === 'CRITICAL' || s === 'HIGH') return 'env-sev--high';
    return '';
}

/** SC-007·Rule Engine 표 — 계층(스택) 정렬 순서 */
const SC007_LAYER_ORDER = [
    '용량산정 TPS', '세션 60분', '동시 요청자', '실요청 사용자', '지점·사용자', '기준 용량',
    'WebTopSuite',
    'GSLB / 인프라',
    'L4 / 인프라',
    'Proxy / L4',
    'Tomcat / WAS',
    'Actuator',
    'Spring Boot',
    'HikariCP / MyBatis',
    'CruzAPIM / 연계',
    '모니터링',
    'Timeout 계층',
    '실요청·TPS',
    '기타'
];

const ASSESS_DOMAIN_LAYER = {
    BASELINE: '용량산정 기준',
    CAPACITY: '실요청·TPS',
    WEBTOP: 'WebTopSuite',
    TOMCAT: 'Tomcat / WAS',
    SPRING: 'Spring Boot',
    DB: 'HikariCP / MyBatis',
    INTEGRATION: 'CruzAPIM / 연계',
    L4: 'L4 / 인프라',
    GSLB: 'GSLB / 인프라',
    NETWORK: 'Proxy / L4',
    MONITOR: '모니터링',
    TIMEOUT: 'Timeout 계층'
};

function layerSortIndex(layer) {
    const idx = SC007_LAYER_ORDER.indexOf(layer);
    return idx >= 0 ? idx : SC007_LAYER_ORDER.length - 1;
}

function groupItemsByLayer(items, layerKey = 'layer') {
    const map = new Map();
    (items || []).forEach(item => {
        const layer = item[layerKey] || '기타';
        if (!map.has(layer)) map.set(layer, []);
        map.get(layer).push(item);
    });
    return Array.from(map.entries())
        .sort((a, b) => layerSortIndex(a[0]) - layerSortIndex(b[0]))
        .map(([layer, groupItems]) => ({ layer, items: groupItems }));
}

function sc007GroupStatusBadges(items) {
    const match = items.filter(i => i.status === 'MATCH').length;
    const warn = items.filter(i => i.status === 'WARN').length;
    const info = items.length - match - warn;
    const parts = [];
    if (match) parts.push(`<span class="pill ok">일치 ${match}</span>`);
    if (warn) parts.push(`<span class="pill no">주의 ${warn}</span>`);
    if (info) parts.push(`<span class="pill">참고 ${info}</span>`);
    return parts.join(' ') || '';
}

function renderSc007LayerGroupHead(layer, items, colSpan = 5) {
    return `
        <tr class="env-sc007-group-head">
            <td colspan="${colSpan}">
                <div class="env-sc007-group-head__inner">
                    <span class="env-sc007-group-head__layer">${escapeHtml(layer)}</span>
                    <span class="env-sc007-group-head__count">${items.length}항목</span>
                    <span class="env-sc007-group-head__badges">${sc007GroupStatusBadges(items)}</span>
                </div>
            </td>
        </tr>`;
}

function renderSc007ItemRow(item) {
    return `
        <tr class="env-row env-row--${settingRowClass(item.status)}">
            <td class="env-cell--label" data-label="항목">
                <strong>${escapeHtml(item.label)}</strong>
                ${item.note ? `<p class="env-note">${escapeHtml(item.note)}</p>` : ''}
            </td>
            <td class="env-cell--guide" data-label="가이드 권장"><code>${escapeHtml(item.guideValue)}</code></td>
            <td class="env-cell--actual" data-label="현재 설정"><code>${escapeHtml(item.actualValue)}</code></td>
            <td class="env-cell--src" data-label="출처">${escapeHtml(item.source)}</td>
            <td class="env-cell--status" data-label="판정"><span class="pill ${settingClass(item.status)}">${settingLabel(item.status)}</span></td>
        </tr>`;
}

function renderSc007CategoryRows(items) {
    const groups = groupItemsByLayer(items, 'layer');
    if (!groups.length) {
        return '<tr><td colspan="5" class="env-empty-cell">비교 항목 없음</td></tr>';
    }
    return groups.map(g =>
        renderSc007LayerGroupHead(g.layer, g.items) +
        g.items.map(renderSc007ItemRow).join('')
    ).join('');
}

function assessDomainLayer(domain) {
    return ASSESS_DOMAIN_LAYER[domain] || domain || '기타';
}

function assessGroupStatusBadges(items) {
    const pass = items.filter(i => i.status === 'PASS').length;
    const warn = items.filter(i => i.status === 'WARN').length;
    const fail = items.filter(i => i.status === 'FAIL' || i.status === 'EXCEPTION').length;
    const parts = [];
    if (pass) parts.push(`<span class="pill ok">통과 ${pass}</span>`);
    if (warn) parts.push(`<span class="pill">주의 ${warn}</span>`);
    if (fail) parts.push(`<span class="pill no">실패 ${fail}</span>`);
    return parts.join(' ') || '';
}

function renderAssessLayerGroupHead(layer, items, colSpan = 9) {
    return `
        <tr class="env-assess-group-head">
            <td colspan="${colSpan}">
                <div class="env-assess-group-head__inner">
                    <span class="env-assess-group-head__layer">${escapeHtml(layer)}</span>
                    <span class="env-assess-group-head__count">${items.length}규칙</span>
                    <span class="env-assess-group-head__badges">${assessGroupStatusBadges(items)}</span>
                </div>
            </td>
        </tr>`;
}

function renderAssessmentResultRow(r) {
    const configFile = r.configFile || r.source || '—';
    const propertyKey = r.propertyKey || '—';
    return `
        <tr class="env-row env-row--${assessRowClass(r.status)}">
            <td class="env-cell--rule" data-label="Rule"><code>${escapeHtml(r.ruleId)}</code></td>
            <td class="env-cell--type" data-label="유형">${escapeHtml(r.ruleType)}</td>
            <td class="env-cell--sev" data-label="심각도"><span class="pill ${assessClass(r.status)} ${sevClass(r.severity)}">${escapeHtml(r.severity)}</span></td>
            <td class="env-cell--file" data-label="설정 파일">${escapeHtml(configFile)}</td>
            <td class="env-cell--param" data-label="설정 파라미터"><code>${escapeHtml(propertyKey)}</code></td>
            <td class="env-cell--desc" data-label="설명"><p class="env-desc-text">${escapeHtml(r.description)}</p></td>
            <td class="env-cell--expected" data-label="가이드 권장"><code>${escapeHtml(r.expectedValue)}</code></td>
            <td class="env-cell--actual" data-label="실측 값"><code>${escapeHtml(r.actualValue)}</code></td>
            <td class="env-cell--status" data-label="판정"><span class="pill ${assessClass(r.status)}">${assessLabel(r.status)}</span></td>
        </tr>`;
}

function renderAssessmentResultRows(results) {
    const enriched = (results || []).map(r => ({
        ...r,
        _layer: assessDomainLayer(r.domain)
    }));
    const groups = groupItemsByLayer(enriched, '_layer');
    if (!groups.length) {
        return '<tr><td colspan="9" class="env-empty-cell">점검 결과 없음</td></tr>';
    }
    return groups.map(g =>
        renderAssessLayerGroupHead(g.layer, g.items) +
        g.items.map(renderAssessmentResultRow).join('')
    ).join('');
}

function baselineRow(category, item, valueHtml, highlight) {
    const cls = highlight ? ' env-baseline-row--highlight' : '';
    return `
        <tr class="env-baseline-row${cls}">
            <td class="env-baseline-cat">${escapeHtml(category)}</td>
            <td class="env-baseline-item"><strong>${escapeHtml(item)}</strong></td>
            <td class="env-baseline-value">${valueHtml}</td>
        </tr>`;
}

function renderProjectBaseline(b) {
    if (!b) return;
    const fromCap = b._fromCapacityDesign === true;
    const capBadge = fromCap
        ? `<span class="pill ok">ENV-002·003 산정 반영</span> `
        : `<span class="pill">application.yml 기준</span> `;
    const scenarioNote = fromCap && b._scenarioId
        ? ` · 시나리오 <code>${escapeHtml(b._scenarioId)}</code>`
        : '';
    document.getElementById('projectSummary').innerHTML =
        `${capBadge}<strong>${escapeHtml(b.projectName)}</strong> · <code>${escapeHtml(b.hardwareProfile || 'nsight-32core-256gb')}</code> · 환경 <code>${escapeHtml(b.envCode)}</code>${scenarioNote}`;

    const link = b.baselineLinked;
    let tpsHtml;
    if (fromCap && link?.tpsScenario?.chips) {
        tpsHtml = link.tpsScenario.chips.map(c =>
            `<span class="env-tps-chip" title="ENV-003 ${c.pct}%">${c.pct}% <strong>${c.tps.toLocaleString()}</strong>${c.tag || ''}</span>`
        ).join('') + `<span class="env-tps-chip env-tps-chip--vm" title="VM 처리량">VM <strong>${link.tpsScenario.vmTps.toLocaleString()}</strong></span>`;
    } else {
        tpsHtml = `
        <span class="env-tps-chip">3% <strong>${b.baseTps}</strong></span>
        <span class="env-tps-chip">5% <strong>${b.peakTps}</strong></span>
        <span class="env-tps-chip">10% <strong>${b.highPeakTps ?? '—'}</strong></span>
        <span class="env-tps-chip">15% <strong>${b.stressTps}</strong></span>
        <span class="env-tps-chip">VM <strong>${b.vmMaxTps ?? 1000}</strong></span>`;
    }

    function capRow(cat, item, linked, fallbackValue, fallbackHint, highlight) {
        if (fromCap && linked) {
            return baselineRow(cat, item, `${linked.value}<span class="env-baseline-hint">${escapeHtml(linked.hint)}</span>`, highlight);
        }
        const hint = fallbackHint ? `<span class="env-baseline-hint">${fallbackHint}</span>` : '';
        return baselineRow(cat, item, `${fallbackValue}${hint}`, highlight);
    }

    let html = '';
    html += baselineRow('식별', '프로젝트 ID', `<code>${escapeHtml(b.projectId)}</code>`, false);
    html += baselineRow('식별', '프로젝트명', escapeHtml(b.projectName), false);
    html += baselineRow('식별', '환경 코드', `<code>${escapeHtml(b.envCode)}</code>`, false);
    html += baselineRow('식별', '하드웨어 프로파일', `<code>${escapeHtml(b.hardwareProfile || 'nsight-32core-256gb')}</code>`, true);
    html += baselineRow('식별', '용량산정 문서', escapeHtml(b.capacityDocRef || '—'), false);
    html += baselineRow('식별', '센터 유형', escapeHtml(b.centerType), false);

    const perBranch = b.usersPerBranch ?? 6;
    const totalUsers = b.totalUsers ?? 36000;
    const actualUsers = b.actualRequestUsers ?? b.peakConcurrentUsers ?? 1800;
    const actualPct = b.actualRequestPeakPercent ?? 5;

    html += capRow('용량', '지점', link?.branch,
        `${(b.branchCount ?? 0).toLocaleString()}개 (지점당 ${perBranch}명)`, '', false);
    html += capRow('용량', '전체 사용자', link?.totalUsers,
        `<strong>${totalUsers.toLocaleString()}명</strong>`, `(${perBranch}×${(b.branchCount ?? 0).toLocaleString()}지점)`, true);
    html += capRow('용량', '실요청 사용자', link?.actualRequest,
        `<strong>${actualUsers.toLocaleString()}명</strong>`, `(피크 ${actualPct}%)`, true);
    html += capRow('용량', '세션 설계 (등록)', link?.sessionDesign,
        `${(b.sessionDesignCount ?? 0).toLocaleString()}명`,
        `(여유 ${(b.sessionBufferedMin ?? 0).toLocaleString()}~${(b.sessionBufferedMax ?? 0).toLocaleString()})`, false);
    html += capRow('용량', '세션 Idle (점검)', link?.sessionCheck,
        `${(b._sessionIdleMinutes || [60]).join('/')}분`, 'ENV-004·Tomcat 세션', false);
    const tpsHint = fromCap && link?.tpsScenario
        ? `<span class="env-baseline-hint">${escapeHtml(link.tpsScenario.hint)}</span>` : '';
    html += baselineRow('용량', 'TPS 시나리오', `<div class="env-tps-group">${tpsHtml}</div>${tpsHint}`, true);
    html += capRow('용량', 'AP VM 규격', link?.apVmSpec, escapeHtml(b.apVmSpec), '', false);
    html += capRow('용량', 'AP 대수', link?.apCount,
        `${b.apCount}센터`, '', false);
    html += capRow('성능', '목표 응답 (p95)', link?.targetP95,
        `<code>${b.targetP95Ms} ms</code> (${(b.targetP95Ms || 0) / 1000}초)`, '', true);

    const deploy = b.deploymentSummary || {};
    const deployKeys = Object.keys(deploy);
    deployKeys.forEach((k, i) => {
        html += baselineRow(
            i === 0 ? '배포' : '',
            k,
            escapeHtml(deploy[k]),
            false
        );
    });

    document.getElementById('projectBaselineBody').innerHTML = html || `
        <tr><td colspan="3" class="env-empty-cell">기준정보 없음</td></tr>
    `;
}

function baselineQueryParams() {
    const pid = document.getElementById('projectId');
    const env = document.getElementById('envCode');
    return {
        projectId: (pid?.value || 'nsight-message-mgmt').trim(),
        envCode: (env?.value || 'local').trim()
    };
}

async function loadBaseline() {
    const body = document.getElementById('projectBaselineBody');
    if (!body) return;
    const { projectId, envCode } = baselineQueryParams();
    const res = await fetch(`${API}/projects/baseline?projectId=${encodeURIComponent(projectId)}&envCode=${encodeURIComponent(envCode)}`, {
        headers: apiHeaders()
    });
    const data = await res.json();
    if (!isSuccess(data)) {
        body.innerHTML =
            '<tr><td colspan="3" class="env-empty-cell">기준정보를 불러오지 못했습니다.</td></tr>';
        return;
    }
    let baseline = data.body?.response;
    if (typeof window.nsightBuildBaselineFromCapacity === 'function') {
        const merged = window.nsightBuildBaselineFromCapacity(baseline);
        if (merged) baseline = merged;
    }
    renderProjectBaseline(baseline);
}

window.nsightReloadProjectBaseline = loadBaseline;

function renderRules(rules) {
    const el = document.getElementById('envRules');
    if (!el) return;
    el.innerHTML = (rules || []).map(r =>
        `<li>${escapeHtml(r)}</li>`
    ).join('');
}

function renderAssessmentCriteria(criteria, guideVersion) {
    const list = document.getElementById('assessmentCriteriaList');
    if (!list) return;
    const items = criteria || [];
    list.innerHTML = items.length
        ? items.map(c => `<li>${escapeHtml(c)}</li>`).join('')
        : '<li class="env-empty-cell">기준 정보 없음</li>';
    const ver = document.getElementById('assessmentCriteriaVersion');
    if (ver) {
        ver.textContent = guideVersion ? `(${guideVersion})` : '';
    }
}

function renderCategories(categories) {
    const root = document.getElementById('envCategories');
    if (!root) return;
    root.innerHTML = (categories || []).map(cat => {
        const layerGroups = groupItemsByLayer(cat.items || [], 'layer');
        const layerSummary = layerGroups.length
            ? `<p class="env-sc007-layer-summary">${layerGroups.length}개 계층 · ${(cat.items || []).length}항목</p>`
            : '';
        return `
        <section class="card env-category-card">
            <div class="card-title">
                <h2>${escapeHtml(cat.title)} (SC-007)</h2>
                <p>${escapeHtml(cat.description)}${layerSummary}</p>
            </div>
            <div class="table-wrap table-wrap--sticky-head env-sc007-table-wrap">
                <table class="dump-report__table dump-report__table--data dump-report__table--env-settings env-sc007-table">
                    <colgroup>
                        <col class="col-env-label"/>
                        <col class="col-env-guide"/>
                        <col class="col-env-actual"/>
                        <col class="col-env-src"/>
                        <col class="col-env-status"/>
                    </colgroup>
                    <thead>
                    <tr>
                        <th scope="col">항목</th>
                        <th scope="col">가이드 권장</th>
                        <th scope="col">현재 설정</th>
                        <th scope="col">출처</th>
                        <th scope="col">판정</th>
                    </tr>
                    </thead>
                    <tbody>
                    ${renderSc007CategoryRows(cat.items)}
                    </tbody>
                </table>
            </div>
        </section>`;
    }).join('');
}

function applySettingsView(view) {
    if (!view) return;
    const matchBadge = document.getElementById('envMatchBadge');
    const warnBadge = document.getElementById('envWarnBadge');
    if (matchBadge) matchBadge.textContent = `일치 ${view.matchCount}`;
    if (warnBadge) warnBadge.textContent = `주의 ${view.warnCount} / ${view.totalCompared}`;
    renderRules(view.designRules);
    renderAssessmentCriteria(view.configurationCriteria, view.guideVersion);
    renderCategories(view.categories);
}

function renderFlowMapCards(containerId, flowMap, chainLabel) {
    const chain = document.getElementById(containerId);
    if (!chain) {
        return;
    }
    chain.innerHTML = (flowMap.nodes || []).map((n, i, arr) => {
        const arrow = i < arr.length - 1
            ? `<span class="env-timeout-arrow ${n.status === 'FAIL' ? 'fail' : ''}">→</span>` : '';
        return `
            <div class="env-timeout-node env-timeout-node--${(n.status || '').toLowerCase()}">
                <span class="env-timeout-order">${n.order}</span>
                <div class="env-timeout-body">
                    <strong>${escapeHtml(n.layer)}</strong> · ${escapeHtml(n.label)}
                    <code class="env-timeout-chain-ms">${escapeHtml(chainLabel)} ${escapeHtml(n.displayValue)}</code>
                    <dl class="env-timeout-meta">
                        <div><dt>관련 파일</dt><dd>${escapeHtml(n.sourceFile || '—')}</dd></div>
                        <div><dt>설정 키</dt><dd><code>${escapeHtml(n.propertyKey || '—')}</code></dd></div>
                        <div><dt>설정값</dt><dd><strong>${escapeHtml(n.configValue || '—')}</strong></dd></div>
                        <div><dt>가이드</dt><dd>${escapeHtml(n.guideValue || '—')}</dd></div>
                    </dl>
                    ${n.note ? `<small class="env-timeout-note">${escapeHtml(n.note)}</small>` : ''}
                </div>
            </div>${arrow}`;
    }).join('');
}

function renderTimeoutMap(timeoutMap) {
    const section = document.getElementById('timeoutMapSection');
    section.classList.remove('hidden');
    document.getElementById('timeoutChainSummary').textContent =
        `${timeoutMap.chainRuleId}: ${timeoutMap.chainSummary}`;
    const badge = document.getElementById('timeoutChainBadge');
    badge.textContent = timeoutMap.chainValid ? 'CHAIN OK' : 'CHAIN FAIL';
    badge.className = 'env-score ' + (timeoutMap.chainValid ? 'env-score--match' : 'env-score--warn');
    renderFlowMapCards('timeoutChain', timeoutMap, '체인');
}

function renderConcurrentFlowMap(flowMap) {
    const section = document.getElementById('concurrentFlowSection');
    section.classList.remove('hidden');
    document.getElementById('concurrentFlowSummary').textContent =
        `${flowMap.chainRuleId}: ${flowMap.chainSummary}`;
    const badge = document.getElementById('concurrentFlowBadge');
    badge.textContent = flowMap.chainValid ? 'FLOW OK' : 'FLOW FAIL';
    badge.className = 'env-score ' + (flowMap.chainValid ? 'env-score--match' : 'env-score--warn');
    const est = document.getElementById('concurrentFlowEstimate');
    if (est) {
        const total = flowMap.actualRequestUsersTotal ?? '—';
        const perAp = flowMap.estimatedConcurrentPerAp ?? '—';
        const tpsDer = flowMap.peakTpsFromActualRequest ?? '—';
        const tpsCfg = flowMap.configuredPeakTps ?? '—';
        est.textContent =
            `실요청 ${total}명(전사, 5%) → AP당 ${perAp} 동시 · TPS ${tpsDer}(실요청÷3초) · peak-tps 설정 ${tpsCfg}`;
    }
    renderFlowMapCards('concurrentFlowChain', flowMap, '용량');
}

function persistAssessmentRun(run) {
    if (!run) return;
    try {
        sessionStorage.setItem('nsight.env.lastAssessment', JSON.stringify({
            runId: run.runId,
            status: run.status,
            passCount: run.passCount,
            warnCount: run.warnCount,
            failCount: run.failCount,
            savedAt: new Date().toISOString()
        }));
        sessionStorage.setItem('nsight.env.lastAssessmentFull', JSON.stringify(run));
    } catch (e) {
        /* ignore */
    }
    if (typeof window.nsightRefreshCheckReport === 'function') {
        window.nsightRefreshCheckReport();
    }
}

function renderAssessmentResults(run) {
    persistAssessmentRun(run);
    const tableWrap = document.getElementById('assessmentResultsTableWrap');
    if (tableWrap) tableWrap.classList.remove('hidden');
    const criteria = run.configurationCriteria
        || run.settingsSnapshot?.configurationCriteria;
    const guideVer = run.settingsSnapshot?.guideVersion;
    renderAssessmentCriteria(criteria, guideVer);
    document.getElementById('assessmentMeta').textContent =
        `Run ${run.runId} · ${run.status} · 통과 ${run.passCount} / 주의 ${run.warnCount} / 실패 ${run.failCount}`;

    const runBadge = document.getElementById('runStatusBadge');
    runBadge.classList.remove('hidden');
    runBadge.textContent = run.status;
    runBadge.className = 'env-score ' + (run.status === 'PASS' ? 'env-score--match' : 'env-score--warn');

    document.getElementById('assessmentResultsBody').innerHTML = renderAssessmentResultRows(run.results);

    if (run.timeoutMap) {
        renderTimeoutMap(run.timeoutMap);
    }
    if (run.concurrentFlowMap) {
        renderConcurrentFlowMap(run.concurrentFlowMap);
    }
    if (run.settingsSnapshot) {
        applySettingsView(run.settingsSnapshot);
    }
}

async function loadSettings() {
    const res = await fetch(`${API}/settings`, { headers: apiHeaders() });
    const data = await res.json();
    if (!isSuccess(data)) {
        showStatus(errMsg(data), 'error');
        return;
    }
    applySettingsView(data.body?.response);
}

async function runAssessment(mergeUploaded) {
    const { projectId, envCode } = baselineQueryParams();
    showStatus('점검 실행 중…', 'info');
    const params = new URLSearchParams({
        projectId,
        envCode,
        mergeUploaded: String(mergeUploaded)
    });
    const res = await fetch(`${API}/assessments?${params}`, {
        method: 'POST',
        headers: apiHeaders()
    });
    const data = await res.json();
    if (!isSuccess(data)) {
        showStatus(errMsg(data), 'error');
        return;
    }
    const run = data.body?.response;
    lastRunId = run.runId;
    renderAssessmentResults(run);
    showStatus(`점검 완료: ${run.runId} (${run.status})`, run.status === 'PASS' ? 'success' : 'error');
}

function syncProfileFileLabels() {
    const code = baselineQueryParams().envCode || 'local';
    const nameEl = document.getElementById('profileFileNameLabel');
    const pathEl = document.getElementById('profileFilePathLabel');
    if (nameEl) {
        nameEl.textContent = `application-${code}.yml`;
    }
    if (pathEl) {
        pathEl.textContent = `src/main/resources/application-${code}.yml`;
    }
}

function updatePickedFileLabel(input) {
    const slot = input.dataset.slot;
    const label = document.querySelector(`.env-file-picked[data-for="${slot}"]`);
    if (!label) {
        return;
    }
    const file = input.files?.[0];
    if (!file) {
        label.textContent = '미선택';
        label.className = 'env-file-picked';
        return;
    }
    label.textContent = file.name;
    label.className = 'env-file-picked is-selected';
    const expected = expectedNameForSlot(slot);
    if (expected && file.name !== expected) {
        label.className = 'env-file-picked is-warn';
        label.title = `권장 파일명: ${expected}`;
    } else {
        label.title = '';
    }
}

function expectedNameForSlot(slot) {
    const code = baselineQueryParams().envCode || 'local';
    const map = {
        'application-yml': 'application.yml',
        'tomcat-yml': 'application.yml',
        'tomcat-server-xml': 'server.xml',
        'l4-gslb-yml': 'application.yml',
        'mybatis-config': 'mybatis-config.xml',
        'application-profile': `application-${code}.yml`,
        'application-properties': 'application.properties',
        'logback-spring': 'logback-spring.xml',
        'bootstrap-yml': 'bootstrap.yml'
    };
    return map[slot] || null;
}

function collectConfigUploadFiles() {
    const files = [];
    const missingRequired = [];
    document.querySelectorAll('.env-config-file-input').forEach((input) => {
        const file = input.files?.[0];
        if (file) {
            files.push(file);
            return;
        }
        if (input.dataset.slot === 'application-yml' || input.dataset.slot === 'mybatis-config') {
            missingRequired.push(expectedNameForSlot(input.dataset.slot));
        }
    });
    return { files, missingRequired };
}

function bindRuleCheckPage() {
    if (!document.getElementById('uploadForm')) return;

    document.querySelectorAll('.env-config-file-input').forEach((input) => {
        input.addEventListener('change', () => updatePickedFileLabel(input));
    });

    const envCodeEl = document.getElementById('envCode');
    envCodeEl?.addEventListener('input', syncProfileFileLabels);
    envCodeEl?.addEventListener('change', syncProfileFileLabels);
    syncProfileFileLabels();

    document.getElementById('uploadForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const { files, missingRequired } = collectConfigUploadFiles();
        if (!files.length) {
            showStatus('업로드할 파일을 한 개 이상 선택하세요.', 'error');
            return;
        }
        if (missingRequired.length) {
            const ok = window.confirm(
                `필수 파일이 비어 있습니다: ${missingRequired.join(', ')}\n선택한 파일만 업로드할까요?`
            );
            if (!ok) {
                return;
            }
        }
        const fd = new FormData();
        for (const f of files) {
            fd.append('files', f);
        }
        showStatus('파싱 중…', 'info');
        const res = await fetch(`${API}/config-files/upload`, {
            method: 'POST',
            headers: apiHeaders(),
            body: fd
        });
        const data = await res.json();
        if (!isSuccess(data)) {
            showStatus(errMsg(data), 'error');
            return;
        }
        const imp = data.body?.response;
        const meta = document.getElementById('importMeta');
        if (meta) {
            meta.textContent =
                `Import ${imp.importId} · 파일 ${imp.fileCount}개 · 항목 ${imp.entryCount}건`;
        }
        showStatus('파싱 완료. 점검 실행을 눌러 Rule Engine을 수행하세요.', 'success');
    });

    document.getElementById('runAssessmentBtn')?.addEventListener('click', () => runAssessment(true));
    document.getElementById('reloadSettingsBtn')?.addEventListener('click', async () => {
        await loadSettings();
        showStatus('런타임 설정을 조회했습니다.', 'success');
    });
}

(async function init() {
    const page = document.body.dataset.envPage || '';
    try {
        if (page === 'check') {
            if (typeof window.nsightInitCapacityView === 'function') {
                await window.nsightInitCapacityView();
            } else if (typeof window.nsightRefreshCheckReport === 'function') {
                window.nsightRefreshCheckReport();
            }
            showStatus('종합 보고서 준비 완료', 'success');
            return;
        }
        if (page === 'rulecheck') {
            bindRuleCheckPage();
            await loadBaseline();
            await loadSettings();
            showStatus('Rule 점검 화면 준비 완료', 'success');
            return;
        }
    } catch (err) {
        showStatus('초기화 실패: ' + err.message, 'error');
    }
})();
