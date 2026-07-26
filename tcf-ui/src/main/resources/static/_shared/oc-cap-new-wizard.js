(function () {
    const STEPS = [
        { n: 1, label: '기본정보' },
        { n: 2, label: '사용자' },
        { n: 3, label: 'TPS' },
        { n: 4, label: 'VM' },
        { n: 5, label: 'AP/DR' },
        { n: 6, label: 'WAS' },
        { n: 7, label: 'DB' },
        { n: 8, label: '결과' }
    ];

    const PURPOSE_LABELS = {
        NEW_BUILD: '신규 구축',
        SCALE_OUT: '증설 검토',
        CONFIG_CHECK: '설정 점검',
        DR_VALIDATION: 'DR 검증'
    };

    const ENV_LABELS = {
        DEV: '개발 DEV',
        STG: '스테이징 STG',
        PROD: '운영 PROD',
        DR: 'DR'
    };

    const STATUS_LABELS = {
        DRAFT: '작성 중',
        COMPLETED: '산정 완료',
        APPROVED: '확정'
    };

    const STEP_SUMMARY_META = [
        { step: 1, label: '프로젝트 기준' },
        { step: 2, label: '사용자·세션' },
        { step: 3, label: '설계 피크 TPS' },
        { step: 4, label: 'VM·Core' },
        { step: 5, label: 'AP·DR' },
        { step: 6, label: 'WAS·JVM' },
        { step: 7, label: 'DB Pool' }
    ];

    const params = new URLSearchParams(location.search);
    const scenarioId = params.get('id');
    let currentStep = 1;
    let defaults = null;
    let scenario = null;
    let scenarioCatalog = [];

    const trackEl = document.getElementById('wizardTrack');
    const contextBarEl = document.getElementById('wizardContextBar');
    const panelEl = document.getElementById('wizardPanel');
    const msgEl = document.getElementById('capNewMsg');
    const titleEl = document.getElementById('wizardTitle');
    const btnPrev = document.getElementById('btnPrev');
    const btnNext = document.getElementById('btnNext');
    const btnSave = document.getElementById('btnSave');

    function normalizeJudgmentCode(value) {
        const raw = String(value ?? '').trim().toUpperCase();
        if (!raw) return 'PENDING';
        if (raw === 'WARN' || raw === 'WARNING' || raw === '주의') return 'WARN';
        if (raw === 'CRITICAL' || raw === '위험') return 'CRITICAL';
        if (raw === 'NORMAL' || raw === '정상') return 'NORMAL';
        if (raw === 'COMPLETED') return 'COMPLETED';
        if (raw === 'APPROVED') return 'APPROVED';
        return raw;
    }

    function formatJudgmentLabel(value) {
        const code = normalizeJudgmentCode(value);
        if (code === 'NORMAL') return '정상';
        if (code === 'WARN') return '주의';
        if (code === 'CRITICAL') return '위험';
        if (code === 'PENDING') return '미확인';
        if (code === 'COMPLETED') return '산정 완료';
        if (code === 'APPROVED') return '확정';
        return String(value ?? '-');
    }

    function judgmentStatusClass(value) {
        const code = normalizeJudgmentCode(value);
        if (code === 'WARN') return 'warn';
        if (code === 'CRITICAL') return 'critical';
        if (code === 'NORMAL') return 'normal';
        if (code === 'PENDING') return 'pending';
        if (code === 'COMPLETED') return 'completed';
        if (code === 'APPROVED') return 'approved';
        return 'pending';
    }

    function judgmentBadge(value) {
        const label = formatJudgmentLabel(value);
        const cls = judgmentStatusClass(value);
        return `<span class="oc-capnew-status oc-capnew-status--${cls}">${escapeHtml(label)}</span>`;
    }

    function resolveTotalUsers() {
        const step2 = (scenario && scenario.stepPayload && scenario.stepPayload.step2) || {};
        if (step2.totalUsers != null) return Number(step2.totalUsers);
        if (step2.calcMode === 'DIRECT' && step2.totalUsersDirect != null) {
            return Number(step2.totalUsersDirect);
        }
        const branchCount = Number(step2.branchCount ?? (defaults?.step2?.branchCount) ?? 6000);
        const userPerBranch = Number(step2.userPerBranch ?? (defaults?.step2?.userPerBranch) ?? 6);
        const hqUsers = Number(step2.hqUsers ?? 0);
        const otherUsers = Number(step2.otherUsers ?? 0);
        return branchCount * userPerBranch + hqUsers + otherUsers;
    }

    function toConcurrentRate(value) {
        const num = Number(value);
        if (!Number.isFinite(num)) return 0;
        return num > 1 ? num / 100 : num;
    }

    function renderContextBar() {
        if (!contextBarEl || !scenario) return;
        const step1 = Object.assign({}, defaults && defaults.step1, scenario.stepPayload && scenario.stepPayload.step1, scenario);
        const env = ENV_LABELS[step1.targetEnv] || step1.targetEnv || '-';
        const purpose = PURPOSE_LABELS[step1.purpose] || step1.purpose || '-';
        const statusCode = scenario.status || 'DRAFT';
        const statusLabel = STATUS_LABELS[statusCode] || statusCode || '-';
        const statusCls = judgmentStatusClass(statusCode === 'DRAFT' ? 'PENDING' : statusCode);

        const items = [
            { label: '프로젝트', value: step1.projectName || '-', wide: true },
            { label: '시나리오', value: step1.scenarioName || scenario.scenarioId || '-', wide: true },
            { label: '환경', value: env },
            { label: '목적', value: purpose },
            { label: '상태', value: statusLabel, badge: statusCls },
            { label: '버전', value: step1.versionNo || '-' }
        ];

        contextBarEl.innerHTML = items.map(item => {
            const wideCls = item.wide ? ' oc-capnew-context-bar__item--wide' : '';
            const valueHtml = item.badge
                ? `<span class="oc-capnew-status oc-capnew-status--${item.badge}">${escapeHtml(item.value)}</span>`
                : escapeHtml(item.value);
            return `<div class="oc-capnew-context-bar__item${wideCls}">
                <span class="oc-capnew-context-bar__label">${escapeHtml(item.label)}</span>
                <span class="oc-capnew-context-bar__value">${valueHtml}</span>
            </div>`;
        }).join('');
        contextBarEl.hidden = false;
    }

    function jumpToStep(stepN) {
        const n = Number(stepN);
        if (!Number.isFinite(n) || n < 1 || n > 8) return;
        currentStep = n;
        renderPanel();
    }

    function showMsg(text, ok) {
        if (!msgEl) return;
        msgEl.textContent = text;
        msgEl.className = 'oc-capnew-msg ' + (ok ? 'oc-capnew-msg--ok' : 'oc-capnew-msg--error');
        msgEl.hidden = !text;
    }

    function showCascadeImpact(impact) {
        const panel = document.getElementById('cascadeImpactPanel');
        if (!panel) return;
        if (!impact || !impact.recalculated) {
            panel.hidden = true;
            panel.innerHTML = '';
            return;
        }
        const changes = (impact.changes || []).map(c =>
            '<li><strong>' + escapeHtml(c.label) + '</strong> '
            + escapeHtml(c.beforeValue) + ' → ' + escapeHtml(c.afterValue) + '</li>'
        ).join('');
        const steps = (impact.affectedSteps || []).join(', ');
        panel.innerHTML = `
            <div class="oc-capnew-cascade-panel">
              <p><strong>하위 단계 자동 재산정</strong></p>
              <p>${escapeHtml(impact.message || impact.summary || '')}</p>
              ${steps ? '<p style="font-size:0.85rem;color:#64748b;">영향 STEP: ' + escapeHtml(steps) + '</p>' : ''}
              ${changes ? '<ul class="oc-capnew-cascade-changes">' + changes + '</ul>' : ''}
            </div>`;
        panel.hidden = false;
    }

    function resolveTrackItem(stepN) {
        if (stepN === currentStep) {
            return { state: 'active', symbol: '●', hint: '현재 단계', issues: [] };
        }
        const tracks = scenario && scenario.stepTrack ? scenario.stepTrack : [];
        const found = tracks.find(t => t.step === stepN);
        if (found) {
            return found;
        }
        return { state: 'pending', symbol: '○', hint: '대기', issues: [] };
    }

    function hasSavedStep(stepN) {
        const raw = scenario && scenario.stepPayload && scenario.stepPayload['step' + stepN];
        return !!(raw && typeof raw === 'object' && Object.keys(raw).length > 0);
    }

    function isStepNavigable(stepN) {
        return stepN !== currentStep && hasSavedStep(stepN);
    }

    function renderTrack() {
        if (!trackEl) return;
        trackEl.innerHTML = STEPS.map(step => {
            const track = resolveTrackItem(step.n);
            const navigable = isStepNavigable(step.n);
            let cls = 'oc-capnew-wizard-track__item oc-capnew-wizard-track__item--' + track.state;
            if (navigable) cls += ' oc-capnew-wizard-track__item--clickable';
            const issues = (track.issues || []).filter(Boolean);
            const title = [track.hint, navigable ? '클릭하여 이동' : '', ...issues].filter(Boolean).join(' · ');
            const stepAttr = navigable ? ` data-step="${step.n}"` : '';
            return `<li class="${cls}"${stepAttr} title="${escapeHtml(title)}" role="listitem"${navigable ? ' tabindex="0"' : ''}>
              <span class="oc-capnew-wizard-track__mark" aria-hidden="true">${track.symbol || '○'}</span>
              <strong>${step.n}</strong><span>${step.label}</span>
            </li>`;
        }).join('');
    }

    function val(id, fallback) {
        const el = document.getElementById(id);
        if (!el) return fallback;
        if (el.type === 'checkbox') return el.checked;
        if (el.type === 'number') return Number(el.value);
        return el.value;
    }

    function setVal(id, value) {
        const el = document.getElementById(id);
        if (!el || value === undefined || value === null) return;
        if (el.type === 'checkbox') {
            el.checked = !!value;
        } else {
            el.value = value;
        }
    }

    function setWarVal(className, idx, value) {
        const el = document.querySelector('.' + className + '[data-idx="' + idx + '"]');
        if (!el || value === undefined || value === null) return;
        if (el.type === 'checkbox') el.checked = !!value;
        else el.value = value;
    }

    function purposeOptions(selected) {
        return Object.entries(PURPOSE_LABELS).map(([value, label]) => {
            const sel = value === selected ? ' selected' : '';
            return `<option value="${escapeHtml(value)}"${sel}>${escapeHtml(label)}</option>`;
        }).join('');
    }

    function envOptions(selected) {
        return (defaults.targetEnvs || []).map(code => {
            const label = ENV_LABELS[code] || code;
            const sel = code === selected ? ' selected' : '';
            return `<option value="${escapeHtml(code)}"${sel}>${escapeHtml(label)}</option>`;
        }).join('');
    }

    function applyStep1Fields(data) {
        setVal('f_projectId', data.projectId);
        setVal('f_projectName', data.projectName);
        setVal('f_scenarioName', data.scenarioName);
        setVal('f_baseDate', data.baseDate);
        setVal('f_versionNo', data.versionNo);
        setVal('f_author', data.author);
        setVal('f_description', data.description);
        const targetEl = document.getElementById('f_targetEnv');
        const purposeEl = document.getElementById('f_purpose');
        if (targetEl) targetEl.innerHTML = envOptions(data.targetEnv);
        if (purposeEl) purposeEl.innerHTML = purposeOptions(data.purpose);
    }

    function renderStep1(data) {
        const src = Object.assign({}, defaults.step1 || {}, data || {}, scenario || {});
        panelEl.innerHTML = `
            <h2>STEP 1. 프로젝트 기본정보</h2>
            <p style="font-size:0.85rem;color:#64748b;">용량산정 결과를 식별하기 위한 기본정보를 입력합니다.</p>
            <div class="oc-capnew-form-grid">
                <label>프로젝트 ID *<input id="f_projectId" required></label>
                <label>프로젝트명 *<input id="f_projectName" required></label>
                <label>시나리오명 *<input id="f_scenarioName" required></label>
                <label>대상 환경 *<select id="f_targetEnv"></select></label>
                <label>기준일<input id="f_baseDate" type="date"></label>
                <label>버전<input id="f_versionNo"></label>
                <label>작성자<input id="f_author"></label>
                <label>산정 목적 *<select id="f_purpose"></select></label>
                <label style="grid-column:1/-1">설명<textarea id="f_description" rows="2"></textarea></label>
            </div>
            <div class="oc-capnew-step1-actions">
                <button type="button" class="tcf-btn tcf-btn--ghost" id="btnShowLoadScenario">기존 시나리오 불러오기</button>
                <button type="button" class="tcf-btn tcf-btn--ghost" id="btnResetStep1">초기화</button>
            </div>
            <div id="step1LoadPanel" class="oc-capnew-step1-load" hidden>
                <label>불러올 시나리오
                    <select id="f_loadScenarioId" style="margin-left:0.35rem;padding:0.35rem 0.5rem;"></select>
                </label>
                <button type="button" class="tcf-btn tcf-btn--ghost" id="btnApplyLoadScenario">적용</button>
                <button type="button" class="tcf-btn tcf-btn--ghost" id="btnCancelLoadScenario">취소</button>
            </div>`;
        document.getElementById('f_targetEnv').innerHTML = envOptions(src.targetEnv);
        document.getElementById('f_purpose').innerHTML = purposeOptions(src.purpose);
        applyStep1Fields(src);
        wireStep1Listeners();
    }

    function wireStep1Listeners() {
        const showBtn = document.getElementById('btnShowLoadScenario');
        const panel = document.getElementById('step1LoadPanel');
        const select = document.getElementById('f_loadScenarioId');
        const applyBtn = document.getElementById('btnApplyLoadScenario');
        const cancelBtn = document.getElementById('btnCancelLoadScenario');
        const resetBtn = document.getElementById('btnResetStep1');

        if (select) {
            const others = scenarioCatalog.filter(s => s.scenarioId !== scenarioId);
            select.innerHTML = others.length
                ? others.map(s => `<option value="${escapeHtml(s.scenarioId)}">${escapeHtml(s.scenarioName || s.scenarioId)} (${escapeHtml(s.status || 'DRAFT')})</option>`).join('')
                : '<option value="">불러올 다른 시나리오 없음</option>';
        }

        if (showBtn && panel) {
            showBtn.addEventListener('click', () => {
                panel.hidden = false;
            });
        }
        if (cancelBtn && panel) {
            cancelBtn.addEventListener('click', () => {
                panel.hidden = true;
            });
        }
        if (resetBtn) {
            resetBtn.addEventListener('click', () => {
                applyStep1Fields(Object.assign({}, defaults.step1 || {}));
                showMsg('STEP 1 기본값으로 초기화했습니다. 저장 시 반영됩니다.', true);
            });
        }
        if (applyBtn && select) {
            applyBtn.addEventListener('click', async () => {
                const id = select.value;
                if (!id) {
                    showMsg('불러올 시나리오를 선택하세요.', false);
                    return;
                }
                try {
                    const loaded = await window.ocCapNewApi.getScenario(id);
                    const step1 = (loaded.stepPayload && loaded.stepPayload.step1) || loaded;
                    applyStep1Fields(Object.assign({}, defaults.step1, step1));
                    if (panel) panel.hidden = true;
                    showMsg('선택한 시나리오의 기본정보를 불러왔습니다. 저장 시 반영됩니다.', true);
                } catch (e) {
                    showMsg(e.message, false);
                }
            });
        }
    }

    function renderStep2(data) {
        const src = Object.assign({}, defaults.step2 || {}, data || {});
        const calcMode = src.calcMode || 'BRANCH';
        const timeoutMin = Number(src.sessionTimeoutMin) || 60;
        const timeoutPreset = timeoutMin === 60 ? '60' : (timeoutMin === 90 ? '90' : 'custom');
        panelEl.innerHTML = `
            <h2>STEP 2. 사용자·세션 조건</h2>
            <fieldset class="oc-capnew-fieldset">
              <legend>사용자 산정 방식</legend>
              <div class="oc-capnew-radio-row">
                <label><input type="radio" name="f_calcMode" value="BRANCH" ${calcMode !== 'DIRECT' ? 'checked' : ''}> 지점 기준 산정</label>
                <label><input type="radio" name="f_calcMode" value="DIRECT" ${calcMode === 'DIRECT' ? 'checked' : ''}> 전체 사용자 직접 입력</label>
              </div>
            </fieldset>
            <div id="step2BranchFields" class="oc-capnew-step2-branch oc-capnew-form-grid" ${calcMode === 'DIRECT' ? 'hidden' : ''}>
                <label>지점 수 *<input id="f_branchCount" type="number" min="1"></label>
                <label>지점당 사용자 *<input id="f_userPerBranch" type="number" min="1"></label>
                <label>본부·센터 사용자<input id="f_hqUsers" type="number" min="0"></label>
                <label>기타 사용자<input id="f_otherUsers" type="number" min="0"></label>
            </div>
            <div id="step2DirectFields" class="oc-capnew-step2-direct oc-capnew-form-grid" ${calcMode !== 'DIRECT' ? 'hidden' : ''}>
                <label>전체 사용자 수 *<input id="f_totalUsersDirect" type="number" min="1"></label>
            </div>
            <div class="oc-capnew-form-grid">
                <label>세션 여유율 (0~1)<input id="f_sessionMarginRate" type="number" step="0.01" min="0" max="1"></label>
            </div>
            <fieldset class="oc-capnew-fieldset">
              <legend>Session Idle Timeout</legend>
              <div class="oc-capnew-radio-row">
                <label><input type="radio" name="f_sessionTimeoutPreset" value="60" ${timeoutPreset === '60' ? 'checked' : ''}> 60분</label>
                <label><input type="radio" name="f_sessionTimeoutPreset" value="90" ${timeoutPreset === '90' ? 'checked' : ''}> 90분</label>
                <label><input type="radio" name="f_sessionTimeoutPreset" value="custom" ${timeoutPreset === 'custom' ? 'checked' : ''}> 직접 입력</label>
              </div>
              <label class="oc-capnew-session-custom" id="sessionTimeoutCustomWrap" ${timeoutPreset !== 'custom' ? 'hidden' : ''} style="margin-top:0.5rem;display:block;">
                Timeout (분)<input id="f_sessionTimeoutCustom" type="number" min="1" value="${timeoutPreset === 'custom' ? timeoutMin : 120}">
              </label>
            </fieldset>
            <div class="oc-capnew-summary">
                <div class="oc-capnew-summary__item"><strong>전체 사용자</strong><div id="sum_totalUsers">-</div></div>
                <div class="oc-capnew-summary__item"><strong>설계 세션</strong><div id="sum_designedSessions">-</div></div>
                <div class="oc-capnew-summary__item"><strong>세션 Timeout</strong><div id="sum_sessionTimeout">-</div></div>
                <div class="oc-capnew-summary__item"><strong>세션 여유</strong><div id="sum_sessionMargin">-</div></div>
            </div>
            <p id="step2CalcNote" style="font-size:0.85rem;color:#64748b;margin-top:0.5rem;"></p>`;
        setVal('f_branchCount', src.branchCount);
        setVal('f_userPerBranch', src.userPerBranch);
        setVal('f_hqUsers', src.hqUsers);
        setVal('f_otherUsers', src.otherUsers);
        setVal('f_totalUsersDirect', src.totalUsersDirect != null ? src.totalUsersDirect : src.totalUsers);
        setVal('f_sessionMarginRate', src.sessionMarginRate);
        wireStep2Listeners();
        updateStep2Summary();
    }

    function isDirectCalcMode() {
        const el = document.querySelector('input[name="f_calcMode"]:checked');
        return el && el.value === 'DIRECT';
    }

    function resolveSessionTimeoutMin() {
        const preset = document.querySelector('input[name="f_sessionTimeoutPreset"]:checked');
        if (!preset) return 60;
        if (preset.value === 'custom') {
            return Math.max(1, Number(val('f_sessionTimeoutCustom', 60)));
        }
        return Number(preset.value);
    }

    function wireStep2Listeners() {
        document.querySelectorAll('input[name="f_calcMode"]').forEach(el => {
            el.addEventListener('change', () => {
                const direct = isDirectCalcMode();
                const branch = document.getElementById('step2BranchFields');
                const directFields = document.getElementById('step2DirectFields');
                if (branch) branch.hidden = direct;
                if (directFields) directFields.hidden = !direct;
                updateStep2Summary();
            });
        });
        document.querySelectorAll('input[name="f_sessionTimeoutPreset"]').forEach(el => {
            el.addEventListener('change', () => {
                const custom = document.querySelector('input[name="f_sessionTimeoutPreset"]:checked');
                const wrap = document.getElementById('sessionTimeoutCustomWrap');
                if (wrap) wrap.hidden = !custom || custom.value !== 'custom';
                updateStep2Summary();
            });
        });
        ['f_branchCount', 'f_userPerBranch', 'f_hqUsers', 'f_otherUsers', 'f_totalUsersDirect', 'f_sessionMarginRate', 'f_sessionTimeoutCustom'].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.addEventListener('input', updateStep2Summary);
        });
    }

    function updateStep2Summary() {
        const margin = Number(val('f_sessionMarginRate', 0));
        const sessionTimeoutMin = resolveSessionTimeoutMin();
        let totalUsers = 0;
        let calcNote = '';
        if (isDirectCalcMode()) {
            totalUsers = Number(val('f_totalUsersDirect', 0));
            calcNote = `직접 입력 ${totalUsers.toLocaleString()}명`;
        } else {
            const branchCount = Number(val('f_branchCount', 0));
            const userPerBranch = Number(val('f_userPerBranch', 0));
            const hqUsers = Number(val('f_hqUsers', 0));
            const otherUsers = Number(val('f_otherUsers', 0));
            totalUsers = branchCount * userPerBranch + hqUsers + otherUsers;
            calcNote = `${branchCount.toLocaleString()}지점 × ${userPerBranch}명`
                + (hqUsers || otherUsers ? ` + 본부/기타 ${(hqUsers + otherUsers).toLocaleString()}명` : '')
                + ` = ${totalUsers.toLocaleString()}명`;
        }
        const designedSessions = Math.ceil(totalUsers * (1 + margin));
        const sessionMarginCount = designedSessions - totalUsers;
        const totalEl = document.getElementById('sum_totalUsers');
        const sessionEl = document.getElementById('sum_designedSessions');
        const timeoutEl = document.getElementById('sum_sessionTimeout');
        const marginEl = document.getElementById('sum_sessionMargin');
        const noteEl = document.getElementById('step2CalcNote');
        if (totalEl) totalEl.textContent = totalUsers.toLocaleString() + ' 명';
        if (sessionEl) sessionEl.textContent = designedSessions.toLocaleString() + ' 개';
        if (timeoutEl) timeoutEl.textContent = sessionTimeoutMin + ' 분';
        if (marginEl) marginEl.textContent = sessionMarginCount.toLocaleString() + ' 개';
        if (noteEl) noteEl.textContent = calcNote + ' · 설계 세션 = 전체 × (1 + ' + margin + ')';
    }

    function renderStep3(data) {
        const scenarios = resolveStep3ScenarioList(data);
        const perfTargets = (data && data.performanceTestTargets) || defaultPerfTestTargets(scenarios, null);
        const totalUsers = resolveTotalUsers();
        const rows = scenarios.map(s => buildTpsRowHtml(s)).join('');
        const perfHtml = buildPerfTestHtml(scenarios, perfTargets);
        panelEl.innerHTML = `
            <h2>STEP 3. 동시요청·TPS 시나리오</h2>
            <p class="oc-capnew-tps-hint">전체 사용자: <strong>${totalUsers.toLocaleString()}명</strong> (STEP 2 기준 · 입력 변경 시 아래 TPS가 즉시 갱신됩니다)</p>
            <table class="oc-capnew-table">
                <thead><tr><th>선택</th><th>시나리오</th><th>동시요청률</th><th>응답(초)</th><th>실요청자</th><th>목표TPS</th></tr></thead>
                <tbody id="tpsRows">${rows}</tbody>
            </table>
            <p style="margin-top:0.75rem;">
                <button type="button" class="tcf-btn tcf-btn--ghost" id="btnAddCustomScenario">+ 사용자 정의 시나리오 추가</button>
            </p>
            <div class="oc-capnew-form-grid" style="margin-top:1rem;">
                <label>운영 기준 시나리오 *
                    <select id="f_operatingBaseline">
                        ${scenarios.map(s => `<option value="${escapeHtml(s.code)}">${escapeHtml(s.label || s.code)}</option>`).join('')}
                    </select>
                </label>
            </div>
            <div style="margin-top:1rem;">
                <strong>성능시험 기준 시나리오</strong>
                <div id="step3PerfTests" class="oc-capnew-perf-tests">${perfHtml}</div>
            </div>`;
        setVal('f_operatingBaseline', data.operatingBaseline || 'DESIGN_PEAK');
        wireStep3Listeners();
        updateStep3Preview();
        refreshPerfTestSection();
    }

    function isCustomScenarioCode(code) {
        return String(code || '').toUpperCase().startsWith('CUSTOM');
    }

    function resolveStep3ScenarioList(data) {
        const saved = (data && data.scenarios) || [];
        const presets = (defaults && defaults.tpsPresets) || [];
        if (saved.length) {
            return saved.map(s => Object.assign({}, s));
        }
        return presets.map(s => Object.assign({}, s));
    }

    function nextCustomScenarioCode(scenarios) {
        let n = 1;
        while (scenarios.some(s => String(s.code).toUpperCase() === 'CUSTOM_' + n)) {
            n += 1;
        }
        return 'CUSTOM_' + n;
    }

    function buildTpsRowHtml(s) {
        const code = s.code || '';
        const custom = isCustomScenarioCode(code);
        const labelCell = custom
            ? `<input type="text" class="oc-capnew-tps-custom-label f_label" data-code="${escapeHtml(code)}" value="${escapeHtml(s.label || '사용자 정의')}">`
            : escapeHtml(s.label || code);
        const deleteBtn = custom
            ? ` <button type="button" class="oc-capnew-btn-link f_deleteCustom" data-code="${escapeHtml(code)}" title="삭제">×</button>`
            : '';
        return `<tr data-tps-code="${escapeHtml(code)}">
                <td><input type="checkbox" class="f_enabled" data-code="${escapeHtml(code)}" ${s.enabled ? 'checked' : ''}></td>
                <td>${labelCell}${deleteBtn}</td>
                <td><input type="number" step="0.01" min="0" max="1" class="f_rate" data-code="${escapeHtml(code)}" value="${s.concurrentRate ?? 0}"></td>
                <td><input type="number" min="1" class="f_response" data-code="${escapeHtml(code)}" value="${s.responseSec ?? 3}"></td>
                <td class="f_concurrentUsers">-</td>
                <td class="f_targetTps">-</td>
            </tr>`;
    }

    function defaultPerfTestTargets(scenarios, existing) {
        if (existing && existing.length) {
            return existing.slice();
        }
        return scenarios
            .filter(s => s.enabled !== false)
            .map(s => s.code)
            .filter(code => {
                const c = String(code).toUpperCase();
                return c !== 'SLOW_RESPONSE' && !c.startsWith('CUSTOM');
            });
    }

    function buildPerfTestHtml(scenarios, targets) {
        const selected = new Set((targets || []).map(String));
        const enabled = scenarios.filter(s => s.enabled !== false);
        if (!enabled.length) {
            return '<span style="color:#64748b;">활성 시나리오를 선택하면 성능시험 기준을 지정할 수 있습니다.</span>';
        }
        return enabled.map(s => {
            const checked = selected.has(s.code) ? ' checked' : '';
            return `<label><input type="checkbox" class="f_perfTarget" value="${escapeHtml(s.code)}"${checked}> ${escapeHtml(s.label || s.code)}</label>`;
        }).join('');
    }

    function collectStep3ScenariosFromDom() {
        const tbody = document.getElementById('tpsRows');
        if (!tbody) return [];
        const baseMap = {};
        const baseList = resolveStep3ScenarioList((scenario.stepPayload || {}).step3 || {});
        baseList.forEach(s => { baseMap[s.code] = s; });
        return Array.from(tbody.querySelectorAll('tr[data-tps-code]')).map(row => {
            const code = row.getAttribute('data-tps-code');
            const base = baseMap[code] || {};
            const enabledEl = row.querySelector('.f_enabled');
            const rateEl = row.querySelector('.f_rate');
            const responseEl = row.querySelector('.f_response');
            const labelEl = row.querySelector('.f_label');
            const label = labelEl
                ? labelEl.value
                : (base.label || code);
            return Object.assign({}, base, {
                code,
                label,
                enabled: enabledEl ? enabledEl.checked : !!base.enabled,
                concurrentRate: rateEl ? Number(rateEl.value) : base.concurrentRate,
                responseSec: responseEl ? Number(responseEl.value) : base.responseSec
            });
        });
    }

    function collectPerfTestTargetsFromDom() {
        return Array.from(document.querySelectorAll('.f_perfTarget:checked')).map(el => el.value);
    }

    function refreshOperatingBaselineSelect(scenarios) {
        const el = document.getElementById('f_operatingBaseline');
        if (!el) return;
        const current = el.value;
        el.innerHTML = scenarios.map(s =>
            `<option value="${escapeHtml(s.code)}">${escapeHtml(s.label || s.code)}</option>`
        ).join('');
        if (scenarios.some(s => s.code === current)) {
            el.value = current;
        } else {
            const design = scenarios.find(s => s.code === 'DESIGN_PEAK');
            el.value = design ? design.code : (scenarios[0] && scenarios[0].code) || '';
        }
    }

    function refreshPerfTestSection() {
        const section = document.getElementById('step3PerfTests');
        if (!section) return;
        const scenarios = collectStep3ScenariosFromDom();
        const saved = ((scenario.stepPayload || {}).step3 || {}).performanceTestTargets;
        const current = collectPerfTestTargetsFromDom();
        const targets = current.length ? current : defaultPerfTestTargets(scenarios, saved);
        section.innerHTML = buildPerfTestHtml(scenarios, targets);
    }

    function wireStep3Listeners() {
        const tbody = document.getElementById('tpsRows');
        const addBtn = document.getElementById('btnAddCustomScenario');
        const onTableChange = () => {
            updateStep3Preview();
            refreshPerfTestSection();
        };

        if (tbody) {
            tbody.addEventListener('input', (e) => {
                if (e.target.classList.contains('f_label')) {
                    refreshOperatingBaselineSelect(collectStep3ScenariosFromDom());
                }
                onTableChange();
            });
            tbody.addEventListener('change', onTableChange);
            tbody.addEventListener('click', (e) => {
                const del = e.target.closest('.f_deleteCustom');
                if (!del) return;
                const code = del.getAttribute('data-code');
                const scenarios = collectStep3ScenariosFromDom().filter(s => s.code !== code);
                const step3 = Object.assign({}, (scenario.stepPayload || {}).step3, {
                    scenarios,
                    operatingBaseline: val('f_operatingBaseline'),
                    performanceTestTargets: collectPerfTestTargetsFromDom()
                });
                renderStep3(step3);
            });
        }

        if (addBtn) {
            addBtn.addEventListener('click', () => {
                const scenarios = collectStep3ScenariosFromDom();
                const code = nextCustomScenarioCode(scenarios);
                scenarios.push({
                    code,
                    label: '사용자 정의',
                    concurrentRate: 0.05,
                    responseSec: 3,
                    enabled: true
                });
                const step3 = Object.assign({}, (scenario.stepPayload || {}).step3, {
                    scenarios,
                    operatingBaseline: val('f_operatingBaseline'),
                    performanceTestTargets: collectPerfTestTargetsFromDom()
                });
                renderStep3(step3);
            });
        }
    }

    function updateStep3Preview() {
        const totalUsers = resolveTotalUsers();
        const tbody = document.getElementById('tpsRows');
        if (!tbody) return;
        tbody.querySelectorAll('tr[data-tps-code]').forEach(row => {
            const enabledEl = row.querySelector('.f_enabled');
            const rateEl = row.querySelector('.f_rate');
            const responseEl = row.querySelector('.f_response');
            const usersCell = row.querySelector('.f_concurrentUsers');
            const tpsCell = row.querySelector('.f_targetTps');
            if (!enabledEl || !enabledEl.checked) {
                if (usersCell) usersCell.textContent = '-';
                if (tpsCell) tpsCell.textContent = '-';
                return;
            }
            const rate = toConcurrentRate(rateEl ? rateEl.value : 0);
            const responseSec = Math.max(1, Number(responseEl ? responseEl.value : 3));
            const concurrentUsers = Math.round(totalUsers * rate);
            const targetTps = Math.ceil(concurrentUsers / responseSec);
            if (usersCell) usersCell.textContent = concurrentUsers.toLocaleString() + '명';
            if (tpsCell) tpsCell.textContent = String(targetTps);
            if (rate > 1) {
                row.classList.add('oc-capnew-tps-row--warn');
            } else {
                row.classList.remove('oc-capnew-tps-row--warn');
            }
        });
    }

    function buildPickTableRows(items, selectedCode, name, columns) {
        return (items || []).map(item => {
            const code = item.code;
            const selected = code === selectedCode;
            const cells = columns.map(col => `<td>${col(item)}</td>`).join('');
            return `<tr class="oc-capnew-pick-table__row${selected ? ' oc-capnew-pick-table__row--selected' : ''}" data-pick-name="${escapeHtml(name)}" data-pick-code="${escapeHtml(code)}">
              <td><input type="radio" name="${escapeHtml(name)}" value="${escapeHtml(code)}" ${selected ? 'checked' : ''}></td>
              ${cells}
            </tr>`;
        }).join('');
    }

    function wirePickTable(name, onSelect) {
        panelEl.querySelectorAll(`tr[data-pick-name="${name}"]`).forEach(row => {
            row.addEventListener('click', (e) => {
                if (e.target.tagName === 'INPUT') return;
                const radio = row.querySelector('input[type="radio"]');
                if (radio) {
                    radio.checked = true;
                    panelEl.querySelectorAll(`tr[data-pick-name="${name}"]`).forEach(r =>
                        r.classList.toggle('oc-capnew-pick-table__row--selected', r === row));
                    if (onSelect) onSelect(radio.value);
                }
            });
            const radio = row.querySelector('input[type="radio"]');
            if (radio) {
                radio.addEventListener('change', () => {
                    panelEl.querySelectorAll(`tr[data-pick-name="${name}"]`).forEach(r =>
                        r.classList.toggle('oc-capnew-pick-table__row--selected', r.querySelector('input') === radio && radio.checked));
                    if (radio.checked && onSelect) onSelect(radio.value);
                });
            }
        });
    }

    function selectedPickValue(name, fallback) {
        const el = document.querySelector(`input[name="${name}"]:checked`);
        return el ? el.value : fallback;
    }

    function renderStep4(data) {
        const src = Object.assign({}, defaults.step4 || {}, data || {});
        const businessTypes = defaults.businessTypes || [];
        const vmProfiles = defaults.vmProfiles || [];
        const bizRows = buildPickTableRows(businessTypes, src.businessTypeCode, 'f_businessTypeCode', [
            b => escapeHtml(b.label),
            b => fmt(b.tpmcPerTps),
            b => fmt(b.tpsPerCore),
            b => '<span style="color:#64748b;">TPMC·Core 기준</span>'
        ]);
        const vmRows = buildPickTableRows(vmProfiles, src.vmProfileCode, 'f_vmProfileCode', [
            v => escapeHtml(v.label || v.code),
            v => fmt(v.cores),
            v => fmt(v.memoryGb) + 'GB',
            v => fmt(v.guideNominalTps),
            v => '<span style="color:#64748b;">Scale-Out 단위</span>'
        ]);
        panelEl.innerHTML = `
            <h2>STEP 4. 업무복잡도·CPU·VM 조건</h2>
            <h3 style="font-size:1rem;margin:1rem 0 0.5rem;">업무 유형 *</h3>
            <table class="oc-capnew-table oc-capnew-pick-table">
                <thead><tr><th></th><th>업무 유형</th><th>TPMC/TPS</th><th>TPS/Core</th><th>설명</th></tr></thead>
                <tbody>${bizRows}</tbody>
            </table>
            <h3 style="font-size:1rem;margin:1.25rem 0 0.5rem;">VM Profile *</h3>
            <table class="oc-capnew-table oc-capnew-pick-table">
                <thead><tr><th></th><th>VM 사양</th><th>Core</th><th>Memory</th><th>VM 기준 TPS</th><th>용도</th></tr></thead>
                <tbody>${vmRows}</tbody>
            </table>
            <div class="oc-capnew-form-grid" style="margin-top:1rem;">
                <label>TPMC/TPS<input id="f_tpmcPerTps" type="number" min="1"></label>
                <label>Core당 TPS<input id="f_tpsPerCore" type="number" min="1"></label>
                <label>CPU 목표 사용률<input id="f_cpuTarget" type="number" step="0.01" min="0" max="1"></label>
                <label>성능 안전계수<input id="f_perfSafety" type="number" step="0.01" min="1"></label>
                <label>가상화 보정<input id="f_virtFactor" type="number" step="0.01" min="0" max="1"></label>
                <label>운영 효율 보정<input id="f_opsFactor" type="number" step="0.01" min="0" max="1"></label>
                <label><input id="f_applyCorrection" type="checkbox"> 보정계수 적용</label>
            </div>
            <div class="oc-capnew-summary">
                <div class="oc-capnew-summary__item"><strong>이론 VM TPS</strong><div>${fmt(src.vmTheoreticalTps)}</div></div>
                <div class="oc-capnew-summary__item"><strong>운영 보정 TPS</strong><div>${fmt(src.vmAdjustedTps)}</div></div>
                <div class="oc-capnew-summary__item"><strong>설계 피크 TPS</strong><div>${fmt(src.designPeakTps)}</div></div>
                <div class="oc-capnew-summary__item"><strong>최소 필요 Core</strong><div>${fmt(src.minRequiredCores)}</div></div>
            </div>`;
        setVal('f_tpmcPerTps', src.tpmcPerTps);
        setVal('f_tpsPerCore', src.tpsPerCore);
        setVal('f_cpuTarget', src.cpuTargetUtilization);
        setVal('f_perfSafety', src.perfSafetyFactor);
        setVal('f_virtFactor', src.virtualizationFactor);
        setVal('f_opsFactor', src.opsEfficiencyFactor);
        setVal('f_applyCorrection', src.applyCorrectionFactors !== false);
        wirePickTable('f_businessTypeCode', onBusinessTypeChange);
        wirePickTable('f_vmProfileCode');
    }

    function onBusinessTypeChange(code) {
        const biz = (defaults.businessTypes || []).find(b => b.code === (code || selectedPickValue('f_businessTypeCode')));
        if (biz) {
            setVal('f_tpmcPerTps', biz.tpmcPerTps);
            setVal('f_tpsPerCore', biz.tpsPerCore);
        }
    }

    function renderStep5(data) {
        const src = Object.assign({}, defaults.step5 || {}, data || {});
        const modes = (defaults.centerModes || []).map(m =>
            `<option value="${m.code}">${escapeHtml(m.label)}</option>`
        ).join('');
        const rows = (src.scenarioResults || []).map(r => `
            <tr>
                <td>${escapeHtml(r.label || r.code)}</td>
                <td>${fmt(r.targetTps)}</td>
                <td>${fmt(r.singleCenterRequiredAp)}</td>
                <td>${fmt(r.apPerCenterNormal)}</td>
                <td>${fmt(r.apPerCenterFailover)}</td>
                <td>${judgmentBadge(r.judgment)}</td>
            </tr>
        `).join('') || '<tr><td colspan="6">저장 후 산정 결과가 표시됩니다.</td></tr>';
        panelEl.innerHTML = `
            <h2>STEP 5. AP 대수·센터·DR 조건</h2>
            <div class="oc-capnew-form-grid">
                <label>센터 구성 *<select id="f_centerMode">${modes}</select></label>
                <label>트래픽 분배<input id="f_trafficSplit" value="50:50"></label>
                <label>AP 여유(센터당)<input id="f_apMargin" type="number" min="0"></label>
                <label>센터당 최소 AP<input id="f_minAp" type="number" min="1"></label>
                <label><input id="f_drFullLoad" type="checkbox"> 1센터 장애 시 100% 수용</label>
            </div>
            <table class="oc-capnew-table" style="margin-top:1rem;">
                <thead><tr><th>시나리오</th><th>목표TPS</th><th>단일센터</th><th>정상/센터</th><th>장애/센터</th><th>판정</th></tr></thead>
                <tbody>${rows}</tbody>
            </table>
            <div class="oc-capnew-summary">
                <div class="oc-capnew-summary__item"><strong>VM당 유효 TPS</strong><div>${fmt(src.vmEffectiveTps)}</div></div>
                <div class="oc-capnew-summary__item"><strong>운영 기준 총 AP</strong><div>${fmt(src.baselineTotalAp)}대</div></div>
            </div>`;
        setVal('f_centerMode', src.centerMode);
        setVal('f_trafficSplit', src.trafficSplit);
        setVal('f_apMargin', src.apMarginPerCenter);
        setVal('f_minAp', src.minApPerCenter);
        setVal('f_drFullLoad', src.drSingleCenterFullLoad !== false);
    }

    function renderStep6(data) {
        const src = Object.assign({}, defaults.step6 || {}, data || {});
        const step4 = ((scenario.stepPayload || {}).step4) || {};
        const scenarios = ((scenario.stepPayload || {}).step3 || {}).scenarios || defaults.tpsPresets || [];
        const opts = scenarios.filter(s => s.enabled).map(s =>
            `<option value="${s.code}">${escapeHtml(s.label || s.code)} (${fmt(s.targetTps)} TPS)</option>`
        ).join('');
        const vmMemory = step4.vmProfileCode ? step4.vmProfileCode : '-';
        panelEl.innerHTML = `
            <h2>STEP 6. WAS Thread·JVM 조건</h2>
            <p style="font-size:0.85rem;color:#64748b;margin:0 0 0.75rem;">단일 Tomcat에 여러 WAR가 배포되면 Thread·Pool은 WAR별이 아니라 <strong>VM 공유</strong> 값입니다.</p>
            <div class="oc-capnew-form-grid">
                <label>기준 시나리오<select id="f_baselineScenario">${opts}</select></label>
                <label>평균 Thread 점유(초)<input id="f_avgHold" type="number" step="0.1" min="0.1"></label>
                <label>Thread 여유율<input id="f_threadMargin" type="number" step="0.1" min="1"></label>
                <label>maxThreads 배율<input id="f_maxMargin" type="number" step="0.1" min="1"></label>
                <label>JVM Xms (GB)<input id="f_jvmXms" type="number" min="1"></label>
                <label>JVM Xmx (GB)<input id="f_jvmXmx" type="number" min="1"></label>
            </div>
            <div class="oc-capnew-summary">
                <div class="oc-capnew-summary__item"><strong>기준 TPS</strong><div>${fmt(src.targetTps)}</div></div>
                <div class="oc-capnew-summary__item"><strong>배포 AP</strong><div>${fmt(src.deploymentAp)}대</div></div>
                <div class="oc-capnew-summary__item"><strong>AP당 TPS</strong><div>${fmt(src.apTps)}</div></div>
                <div class="oc-capnew-summary__item"><strong>총 Thread</strong><div>${fmt(src.totalCalculatedThreads)}</div></div>
                <div class="oc-capnew-summary__item"><strong>AP당 Thread</strong><div>${fmt(src.threadsPerVm)}</div></div>
                <div class="oc-capnew-summary__item"><strong>WAS 판정</strong><div>${judgmentBadge(src.wasStatus)}</div></div>
            </div>
            <h3 style="font-size:1rem;margin:1rem 0 0.5rem;">Tomcat Connector 권장값</h3>
            <table class="oc-capnew-table oc-capnew-jvm-table">
                <thead><tr><th>항목</th><th>자동 권장값</th><th>판정</th></tr></thead>
                <tbody>
                  <tr><td>maxThreads</td><td>${fmt(src.recommendedMaxThreads)}</td><td>${judgmentBadge(src.wasStatus)}</td></tr>
                  <tr><td>minSpareThreads</td><td>${fmt(src.minSpareThreads)}</td><td>${judgmentBadge('NORMAL')}</td></tr>
                  <tr><td>acceptCount</td><td>${fmt(src.acceptCount)}</td><td>${judgmentBadge('NORMAL')}</td></tr>
                  <tr><td>maxConnections</td><td>${fmt(src.recommendedMaxConnections)}</td><td>${judgmentBadge('NORMAL')}</td></tr>
                </tbody>
            </table>
            <h3 style="font-size:1rem;margin:1rem 0 0.5rem;">JVM 권장값</h3>
            <table class="oc-capnew-table oc-capnew-jvm-table">
                <thead><tr><th>항목</th><th>자동 권장값</th><th>적용 예정값</th><th>판정</th></tr></thead>
                <tbody>
                  <tr><td>VM Memory</td><td>${escapeHtml(vmMemory)}</td><td>${escapeHtml(vmMemory)}</td><td>${judgmentBadge('NORMAL')}</td></tr>
                  <tr><td>JVM Xms</td><td>${fmt(src.jvmXmsGb)}GB</td><td><input id="f_jvmXms" type="number" min="1" style="width:5rem;"></td><td>${judgmentBadge(src.jvmStatus)}</td></tr>
                  <tr><td>JVM Xmx</td><td>${fmt(src.jvmXmxGb)}GB</td><td><input id="f_jvmXmx" type="number" min="1" style="width:5rem;"></td><td>${judgmentBadge(src.jvmStatus)}</td></tr>
                  <tr><td>GC</td><td>${escapeHtml(src.jvmGc || 'G1GC')}</td><td>${escapeHtml(src.jvmGc || 'G1GC')}</td><td>${judgmentBadge(src.jvmStatus)}</td></tr>
                  <tr><td>MaxGCPauseMillis</td><td>${fmt(src.jvmMaxGcPauseMs)}ms</td><td>${fmt(src.jvmMaxGcPauseMs)}ms</td><td>${judgmentBadge(src.jvmStatus)}</td></tr>
                  <tr><td>Thread Stack</td><td>${escapeHtml(src.jvmThreadStack || '512KB')}</td><td>${escapeHtml(src.jvmThreadStack || '512KB')}</td><td>${judgmentBadge(src.jvmStatus)}</td></tr>
                </tbody>
            </table>
            <p class="oc-capnew-msg oc-capnew-msg--ok" style="margin-top:0.75rem;">${escapeHtml(src.wasStatusMessage || src.jvmRecommendation || '')}</p>`;
        setVal('f_baselineScenario', src.baselineScenarioCode);
        setVal('f_avgHold', src.avgThreadHoldSec);
        setVal('f_threadMargin', src.threadMarginRate);
        setVal('f_maxMargin', src.maxThreadMarginRate);
        setVal('f_jvmXms', src.jvmXmsGb);
        setVal('f_jvmXmx', src.jvmXmxGb);
    }

    function renderStep7(data) {
        const src = Object.assign({}, defaults.step7 || {}, data || {});
        const wars = src.warAllocations || (defaults.step7 && defaults.step7.warAllocations) || [];
        const warRows = wars.map((w, idx) => `
            <tr>
              <td>${escapeHtml(w.warCode || '')}</td>
              <td>${escapeHtml(w.label || w.warCode || '')}</td>
              <td><input class="f_warWeight" data-idx="${idx}" type="number" min="0" max="100" step="1" style="width:4rem;"></td>
              <td><input class="f_warEnabled" data-idx="${idx}" type="checkbox"></td>
            </tr>`).join('');
        const results = src.warPoolResults || [];
        const resultRows = results.map(r => `
            <tr>
              <td>${escapeHtml(r.warCode || '')}</td>
              <td>${fmt(r.weightPercent)}%</td>
              <td>${fmt(r.poolPerVm)}</td>
              <td>${fmt(r.deploymentAp)}</td>
              <td>${fmt(r.totalPool)}</td>
              <td>${judgmentBadge(r.judgment)}</td>
            </tr>`).join('');
        const recs = (src.warPoolRecommendations || []).map(r =>
            '<li>' + escapeHtml(r) + '</li>').join('');
        panelEl.innerHTML = `
            <h2>STEP 7. DB Pool·DB Session 조건</h2>
            <div class="oc-capnew-form-grid">
                <label>AP 유형<select id="f_apType"><option value="SINGLE_VIEW">SingleView</option><option value="GENERAL">일반</option></select></label>
                <label>DB 점유시간(초)<input id="f_dbHold" type="number" step="0.01" min="0.01"></label>
                <label>DB 사용 비율<input id="f_dbUsage" type="number" step="0.01" min="0.1" max="1"></label>
                <label>Pool 안전계수<input id="f_poolSafety" type="number" step="0.1" min="1"></label>
                <label>Thread→DB 비율<input id="f_threadDb" type="number" step="0.01" min="0.1" max="1"></label>
                <label>운영 최소 Pool<input id="f_minPool" type="number" min="1"></label>
                <label>WAR 최소 Pool<input id="f_minPoolWar" type="number" min="1"></label>
                <label>DB Session 한도<input id="f_dbLimit" type="number" min="1"></label>
                <label style="grid-column:1/-1;"><input id="f_warPoolEnabled" type="checkbox"> 업무 WAR별 Pool 배분 검증 (동일 Tomcat)</label>
            </div>
            <div class="oc-capnew-summary">
                <div class="oc-capnew-summary__item"><strong>② TPS기준 Pool</strong><div>${fmt(src.poolTheoretical)}</div></div>
                <div class="oc-capnew-summary__item"><strong>③ Thread 상한</strong><div>${fmt(src.poolCeiling)}</div></div>
                <div class="oc-capnew-summary__item"><strong>⑤ 최종 Pool/VM</strong><div>${fmt(src.poolPerVm)}</div></div>
                <div class="oc-capnew-summary__item"><strong>⑥ 단일 Pool DB Session</strong><div>${fmt(src.totalDbSessions)}</div></div>
                <div class="oc-capnew-summary__item"><strong>판정</strong><div>${judgmentBadge(src.dbStatus)}</div></div>
            </div>
            <p style="font-size:0.85rem;color:#64748b;margin-top:0.5rem;">${escapeHtml(src.poolFormula || '')}</p>
            <h3 style="margin-top:1.25rem;font-size:1rem;">업무 WAR별 Pool 배분</h3>
            <p style="font-size:0.85rem;color:#64748b;">비중 합계 100% 권장 · WAR마다 별도 HikariCP Pool이 생성되므로 합계를 DB Session 한도와 비교합니다.</p>
            <table class="oc-capnew-table">
                <thead><tr><th>WAR</th><th>라벨</th><th>비중(%)</th><th>활성</th></tr></thead>
                <tbody>${warRows}</tbody>
            </table>
            ${results.length ? `
            <table class="oc-capnew-table" style="margin-top:0.75rem;">
                <thead><tr><th>WAR</th><th>비중</th><th>Pool/VM</th><th>AP 대수</th><th>전체 Pool</th><th>판정</th></tr></thead>
                <tbody>${resultRows}
                <tr class="oc-capnew-war-total">
                  <td colspan="4"><strong>합계</strong></td>
                  <td><strong>${fmt(src.warPoolTotalSessions)}</strong></td>
                  <td>${judgmentBadge(src.warPoolStatus)}</td>
                </tr></tbody>
            </table>
            <p style="font-size:0.85rem;margin-top:0.5rem;">${escapeHtml(src.warPoolStatusMessage || '')}</p>
            ${recs ? '<ul class="oc-capnew-legacy-notes">' + recs + '</ul>' : ''}` : '<p style="font-size:0.85rem;color:#64748b;">저장 시 WAR별 Pool이 산정됩니다.</p>'}`;
        setVal('f_apType', src.apType || 'SINGLE_VIEW');
        setVal('f_dbHold', src.avgDbConnectionHoldSec);
        setVal('f_dbUsage', src.dbTransactionUsageRatio);
        setVal('f_poolSafety', src.poolSafetyFactor);
        setVal('f_threadDb', src.threadDbUsageRatio);
        setVal('f_minPool', src.minPoolPerVm);
        setVal('f_minPoolWar', src.minPoolPerWar != null ? src.minPoolPerWar : 15);
        setVal('f_dbLimit', src.dbSessionLimit);
        setVal('f_warPoolEnabled', src.warPoolEnabled !== false);
        wars.forEach((w, idx) => {
            setWarVal('f_warWeight', idx, w.weightPercent);
            setWarVal('f_warEnabled', idx, w.enabled !== false);
        });
    }

    function stepTrackJudgment(stepN) {
        const track = (scenario && scenario.stepTrack || []).find(t => t.step === stepN);
        if (!track) return 'PENDING';
        if (track.state === 'done') return 'NORMAL';
        if (track.state === 'warn') return 'WARN';
        if (track.state === 'error') return 'CRITICAL';
        return 'PENDING';
    }

    function scenarioLabel(code, payload) {
        const step3 = (payload && payload.step3) || {};
        const scenarios = step3.scenarios || [];
        const found = scenarios.find(s => String(s.code).toUpperCase() === String(code).toUpperCase());
        return (found && (found.label || found.code)) || code || '-';
    }

    function buildStepSummaryResult(stepN, payload) {
        const p = payload || {};
        const step1 = Object.assign({}, defaults && defaults.step1, p.step1, scenario);
        if (stepN === 1) {
            const env = ENV_LABELS[step1.targetEnv] || step1.targetEnv || '-';
            return `${env} / ${step1.versionNo || '-'}`;
        }
        if (stepN === 2) {
            const s2 = p.step2 || {};
            return `${fmt(s2.totalUsers)}명 / ${fmt(s2.designedSessions)}개`;
        }
        if (stepN === 3) {
            const s3 = p.step3 || {};
            const s4 = p.step4 || {};
            const label = scenarioLabel(s3.operatingBaseline, p);
            return `${label} · ${fmt(s4.designPeakTps)} TPS`;
        }
        if (stepN === 4) {
            const s4 = p.step4 || {};
            return `${s4.vmProfileCode || '-'} · 보정 ${fmt(s4.vmAdjustedTps)} TPS`;
        }
        if (stepN === 5) {
            const s5 = p.step5 || {};
            return `센터당 ${fmt(s5.baselineApPerCenter)}대 · 총 ${fmt(s5.baselineTotalAp)}대`;
        }
        if (stepN === 6) {
            const s6 = p.step6 || {};
            return `Thread ${fmt(s6.recommendedMaxThreads)} / Heap ${fmt(s6.jvmXmxGb)}GB`;
        }
        if (stepN === 7) {
            const s7 = p.step7 || {};
            let text = `Pool ${fmt(s7.poolPerVm)} / Session ${fmt(s7.totalDbSessions)}`;
            if (s7.warPoolEnabled && s7.warPoolTotalSessions != null) {
                text += ` · WAR합계 ${fmt(s7.warPoolTotalSessions)}`;
            }
            return text;
        }
        return '-';
    }

    function buildStepSummaryRows(payload) {
        const rows = STEP_SUMMARY_META.map(meta => ({
            step: meta.step,
            label: meta.label,
            result: buildStepSummaryResult(meta.step, payload),
            judgment: stepTrackJudgment(meta.step)
        }));
        const s7 = (payload && payload.step7) || {};
        if (s7.warPoolEnabled && s7.warPoolTotalSessions != null) {
            rows.push({
                step: 7,
                label: 'WAR Pool 합계',
                result: `최대 ${fmt(s7.warPoolTotalSessions)} Session`,
                judgment: s7.warPoolStatus || 'NORMAL'
            });
        }
        return rows;
    }

    function formatApDisplay(row, centerMode) {
        const perCenter = row.apPerCenterNormal;
        if (perCenter == null) return '-';
        if (centerMode === 'ACTIVE_ACTIVE' || !centerMode) {
            return `${fmt(perCenter)}+${fmt(perCenter)}`;
        }
        return `${fmt(perCenter)}대`;
    }

    function estimateMaxThreads(targetTps, deploymentAp, step6) {
        if (!targetTps || !deploymentAp) return '-';
        const avgHold = Number(step6.avgThreadHoldSec) || 1.2;
        const threadMargin = Number(step6.threadMarginRate) || 1.2;
        const maxMargin = Number(step6.maxThreadMarginRate) || 1.3;
        const total = Math.ceil(targetTps * avgHold * threadMargin);
        const perVm = Math.ceil(total / deploymentAp);
        return Math.ceil(perVm * maxMargin);
    }

    function estimatePoolPerVm(row, step6, step7) {
        const deploymentAp = Math.max(1, Number(row.totalDeploymentAp) || 1);
        const apTps = Math.ceil(Number(row.targetTps) / deploymentAp);
        const hold = Number(step7.avgDbConnectionHoldSec) || 0.2;
        const usage = Number(step7.dbTransactionUsageRatio) || 1;
        const safety = Number(step7.poolSafetyFactor) || 1.3;
        const poolTheoretical = Math.ceil(apTps * hold * usage * safety);
        const maxThreads = Number(step6.recommendedMaxThreads) || estimateMaxThreads(row.targetTps, deploymentAp, step6);
        const ceiling = Math.ceil(maxThreads * (Number(step7.threadDbUsageRatio) || 0.3));
        const capped = Math.min(poolTheoretical, ceiling);
        return Math.max(Number(step7.minPoolPerVm) || 30, capped);
    }

    function buildScenarioMatrix(payload) {
        const p = payload || {};
        const step5 = p.step5 || {};
        const step6 = p.step6 || {};
        const step7 = p.step7 || {};
        const baselineCode = (p.step3 && p.step3.operatingBaseline) || 'DESIGN_PEAK';
        return (step5.scenarioResults || []).map(row => {
            const isBaseline = String(row.code).toUpperCase() === String(baselineCode).toUpperCase();
            return {
                label: row.label || row.code,
                tps: row.targetTps,
                ap: formatApDisplay(row, step5.centerMode),
                maxThreads: isBaseline && step6.recommendedMaxThreads != null
                    ? step6.recommendedMaxThreads
                    : estimateMaxThreads(row.targetTps, row.totalDeploymentAp, step6),
                poolPerVm: isBaseline && step7.poolPerVm != null
                    ? step7.poolPerVm
                    : estimatePoolPerVm(row, step6, step7),
                judgment: row.judgment,
                isBaseline
            };
        });
    }

    function renderStep8(data) {
        const src = Object.assign({}, data || {});
        const headline = src.headline || {};
        const risk = src.riskSummary || {};
        const payload = (scenario && scenario.stepPayload) || {};
        const stepSummaries = buildStepSummaryRows(payload);
        const scenarioMatrix = buildScenarioMatrix(payload);
        const summaryRows = stepSummaries.map(r => `
            <tr>
              <td>${r.step}</td>
              <td>${escapeHtml(r.label)}</td>
              <td>${escapeHtml(r.result)}</td>
              <td>${judgmentBadge(r.judgment)}</td>
              <td><button type="button" class="oc-capnew-btn-link" data-jump-step="${r.step}">보기</button></td>
            </tr>`).join('');
        const matrixRows = scenarioMatrix.map(r => `
            <tr${r.isBaseline ? ' style="background:#f8fafc;"' : ''}>
              <td>${escapeHtml(r.label)}${r.isBaseline ? ' <span class="oc-capnew-badge">운영기준</span>' : ''}</td>
              <td>${fmt(r.tps)}</td>
              <td>${escapeHtml(r.ap)}</td>
              <td>${fmt(r.maxThreads)}</td>
              <td>${fmt(r.poolPerVm)}</td>
              <td>${judgmentBadge(r.judgment)}</td>
            </tr>`).join('') || '<tr><td colspan="6">STEP 5 저장 후 시나리오별 비교가 표시됩니다.</td></tr>';

        const canClone = scenario && (scenario.status === 'COMPLETED' || scenario.status === 'APPROVED');
        const cloneHint = canClone
            ? ''
            : ' <span style="font-size:0.8rem;color:#64748b;">(산정 완료 후 복제 가능)</span>';

        panelEl.innerHTML = `
            <h2>STEP 8. 종합 결과·비교·확정</h2>
            <div class="oc-capnew-summary">
                <div class="oc-capnew-summary__item"><strong>운영 기준</strong><div>${escapeHtml(scenarioLabel(headline.operatingBaseline, payload))}</div></div>
                <div class="oc-capnew-summary__item"><strong>설계 TPS</strong><div>${fmt(headline.designPeakTps)}</div></div>
                <div class="oc-capnew-summary__item"><strong>VM</strong><div>${escapeHtml(headline.vmProfile || '-')}</div></div>
                <div class="oc-capnew-summary__item"><strong>배포 AP</strong><div>${fmt(headline.totalDeploymentAp)}대</div></div>
                <div class="oc-capnew-summary__item"><strong>maxThreads</strong><div>${fmt(headline.maxThreads)}</div></div>
                <div class="oc-capnew-summary__item"><strong>Pool/VM</strong><div>${fmt(headline.poolPerVm)}</div></div>
                <div class="oc-capnew-summary__item"><strong>DB Session</strong><div>${fmt(headline.totalDbSessions)}</div></div>
                ${headline.warPoolTotalSessions != null ? `<div class="oc-capnew-summary__item"><strong>WAR Pool 합계</strong><div>${fmt(headline.warPoolTotalSessions)} ${judgmentBadge(headline.warPoolStatus)}</div></div>` : ''}
                <div class="oc-capnew-summary__item"><strong>종합 판정</strong><div>${judgmentBadge(headline.overallJudgment)}</div></div>
            </div>
            <p style="margin-top:1rem;">정상 ${risk.normal || 0} · 주의 ${risk.warning || 0} · 위험 ${risk.critical || 0}</p>
            <p class="oc-capnew-msg oc-capnew-msg--ok">${escapeHtml(src.conclusion || '저장 시 종합 결론이 생성됩니다.')}</p>

            <div class="oc-capnew-step8-section">
              <h3>단계별 결과</h3>
              <p>각 STEP 산정 결과와 판정입니다. [보기]로 해당 단계로 이동합니다.</p>
              <table class="oc-capnew-table">
                <thead><tr><th>단계</th><th>산정 항목</th><th>결과</th><th>판정</th><th></th></tr></thead>
                <tbody>${summaryRows}</tbody>
              </table>
            </div>

            <div class="oc-capnew-step8-section">
              <h3>시나리오별 비교</h3>
              <p>활성 TPS 시나리오별 AP·Thread·Pool 지표 (운영 기준 행은 저장값, 그 외는 추정치)</p>
              <table class="oc-capnew-table">
                <thead><tr><th>시나리오</th><th>TPS</th><th>필요 AP</th><th>maxThreads</th><th>Pool/VM</th><th>판정</th></tr></thead>
                <tbody>${matrixRows}</tbody>
              </table>
            </div>

            <p style="margin-top:0.75rem;">
                <button type="button" class="tcf-btn tcf-btn--ghost" data-capnew-clone ${canClone ? '' : 'disabled'}>시나리오 복사</button>${cloneHint}
                <button type="button" class="tcf-btn tcf-btn--ghost" data-capnew-review-request style="margin-left:0.5rem;">검토 요청</button>
                <a href="/oc/cap-new/compare.html?id=${encodeURIComponent(scenarioId)}" class="tcf-btn tcf-btn--ghost" style="margin-left:0.5rem;">다른 시나리오와 비교 →</a>
                <a href="/oc/cap-new/approved.html?id=${encodeURIComponent(scenarioId)}" class="tcf-btn tcf-btn--ghost" style="margin-left:0.5rem;">최종 확정 →</a>
            </p>
            <p style="margin-top:0.75rem;">
                <button type="button" class="tcf-btn tcf-btn--ghost" data-capnew-env-handoff data-scenario-id="${escapeHtml(scenarioId)}">ENV 점검으로 이동 →</button>
                <button type="button" class="tcf-btn tcf-btn--ghost" data-capnew-export="scenario" data-scenario-id="${escapeHtml(scenarioId)}" style="margin-left:0.5rem;">Excel 다운로드</button>
                <button type="button" class="tcf-btn tcf-btn--ghost" data-capnew-legacy-compare data-scenario-id="${escapeHtml(scenarioId)}" style="margin-left:0.5rem;">기존 CAP 대조</button>
                <button type="button" class="tcf-btn tcf-btn--ghost" data-capnew-vm-compare data-scenario-id="${escapeHtml(scenarioId)}" style="margin-left:0.5rem;">VM 대안 비교</button>
            </p>
            <div id="legacyComparePanel" class="oc-capnew-legacy-host" hidden></div>
            <div id="vmComparePanel" class="oc-capnew-legacy-host" hidden></div>
            <label style="display:block;margin-top:1rem;">검토 메모<textarea id="f_reviewNote" rows="3" style="width:100%;">${escapeHtml(src.reviewNote || '')}</textarea></label>`;
    }

    function fmt(v) {
        if (v === undefined || v === null || v === '') return '-';
        if (typeof v === 'number') return v.toLocaleString();
        return String(v);
    }

    function renderPlaceholder(step) {
        panelEl.innerHTML = `
            <h2>STEP ${step}. (Phase 2 연동 예정)</h2>
            <p>Phase 1 골격에서는 STEP 4~8 UI 껍데기만 제공합니다. 입력 임시 저장은 API에서 허용됩니다.</p>
            <textarea id="f_placeholderNote" rows="4" style="width:100%;" placeholder="메모 (선택)"></textarea>`;
        const payload = (scenario.stepPayload || {})['step' + step] || {};
        setVal('f_placeholderNote', payload.note || '');
    }

    function fillSelect(id, options, selected) {
        const el = document.getElementById(id);
        if (!el || !options) return;
        el.innerHTML = options.map(opt => {
            const value = typeof opt === 'string' ? opt : opt.value;
            const label = typeof opt === 'string' ? opt : (opt.label || opt.value);
            const sel = value === selected ? ' selected' : '';
            return `<option value="${escapeHtml(value)}"${sel}>${escapeHtml(label)}</option>`;
        }).join('');
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function collectPayload() {
        if (currentStep === 1) {
            return {
                projectId: val('f_projectId'),
                projectName: val('f_projectName'),
                scenarioName: val('f_scenarioName'),
                targetEnv: val('f_targetEnv'),
                baseDate: val('f_baseDate'),
                versionNo: val('f_versionNo'),
                author: val('f_author'),
                description: val('f_description'),
                purpose: val('f_purpose')
            };
        }
        if (currentStep === 2) {
            const direct = isDirectCalcMode();
            const payload = {
                calcMode: direct ? 'DIRECT' : 'BRANCH',
                sessionMarginRate: val('f_sessionMarginRate'),
                sessionTimeoutMin: resolveSessionTimeoutMin()
            };
            if (direct) {
                payload.totalUsersDirect = val('f_totalUsersDirect');
            } else {
                payload.branchCount = val('f_branchCount');
                payload.userPerBranch = val('f_userPerBranch');
                payload.hqUsers = val('f_hqUsers');
                payload.otherUsers = val('f_otherUsers');
            }
            return payload;
        }
        if (currentStep === 3) {
            return {
                scenarios: collectStep3ScenariosFromDom(),
                operatingBaseline: val('f_operatingBaseline'),
                performanceTestTargets: collectPerfTestTargetsFromDom()
            };
        }
        if (currentStep === 4) {
            return {
                businessTypeCode: selectedPickValue('f_businessTypeCode'),
                vmProfileCode: selectedPickValue('f_vmProfileCode'),
                tpmcPerTps: val('f_tpmcPerTps'),
                tpsPerCore: val('f_tpsPerCore'),
                cpuTargetUtilization: val('f_cpuTarget'),
                perfSafetyFactor: val('f_perfSafety'),
                virtualizationFactor: val('f_virtFactor'),
                opsEfficiencyFactor: val('f_opsFactor'),
                applyCorrectionFactors: val('f_applyCorrection')
            };
        }
        if (currentStep === 5) {
            return {
                centerMode: val('f_centerMode'),
                trafficSplit: val('f_trafficSplit'),
                drSingleCenterFullLoad: val('f_drFullLoad'),
                apMarginPerCenter: val('f_apMargin'),
                minApPerCenter: val('f_minAp')
            };
        }
        if (currentStep === 6) {
            return {
                baselineScenarioCode: val('f_baselineScenario'),
                avgThreadHoldSec: val('f_avgHold'),
                threadMarginRate: val('f_threadMargin'),
                maxThreadMarginRate: val('f_maxMargin'),
                jvmXmsGb: val('f_jvmXms'),
                jvmXmxGb: val('f_jvmXmx')
            };
        }
        if (currentStep === 7) {
            const base = ((scenario.stepPayload || {}).step7) || {};
            const wars = base.warAllocations || (defaults.step7 && defaults.step7.warAllocations) || [];
            const warAllocations = wars.map((w, idx) => {
                const weightEl = document.querySelector('.f_warWeight[data-idx="' + idx + '"]');
                const enabledEl = document.querySelector('.f_warEnabled[data-idx="' + idx + '"]');
                return Object.assign({}, w, {
                    weightPercent: weightEl ? Number(weightEl.value) : w.weightPercent,
                    enabled: enabledEl ? enabledEl.checked : w.enabled !== false
                });
            });
            return {
                apType: val('f_apType'),
                avgDbConnectionHoldSec: val('f_dbHold'),
                dbTransactionUsageRatio: val('f_dbUsage'),
                poolSafetyFactor: val('f_poolSafety'),
                threadDbUsageRatio: val('f_threadDb'),
                minPoolPerVm: val('f_minPool'),
                minPoolPerWar: val('f_minPoolWar'),
                dbSessionLimit: val('f_dbLimit'),
                warPoolEnabled: val('f_warPoolEnabled'),
                warAllocations: warAllocations
            };
        }
        if (currentStep === 8) {
            return { reviewNote: val('f_reviewNote', '') };
        }
        return { note: val('f_placeholderNote', '') };
    }

    function renderPanel() {
        const payload = (scenario.stepPayload || {})['step' + currentStep] || {};
        if (currentStep === 1) renderStep1(Object.assign({}, defaults.step1, payload, scenario));
        else if (currentStep === 2) renderStep2(payload);
        else if (currentStep === 3) renderStep3(payload);
        else if (currentStep === 4) renderStep4(payload);
        else if (currentStep === 5) renderStep5(payload);
        else if (currentStep === 6) renderStep6(payload);
        else if (currentStep === 7) renderStep7(payload);
        else if (currentStep === 8) renderStep8(payload);
        else renderPlaceholder(currentStep);
        if (titleEl) {
            titleEl.textContent = (scenario.scenarioName || scenario.scenarioId) + ' · STEP ' + currentStep;
        }
        renderTrack();
        renderContextBar();
        btnPrev.disabled = currentStep <= 1;
        btnNext.textContent = currentStep >= 8 ? '완료' : '다음 단계';
    }

    async function saveCurrentStep() {
        const saved = await window.ocCapNewApi.saveStep(scenarioId, currentStep, collectPayload());
        scenario = saved;
        showCascadeImpact(saved.cascadeImpact);
        const impact = saved.cascadeImpact;
        let msg = 'STEP ' + currentStep + ' 저장 완료';
        if (impact && impact.recalculated) {
            msg += ' · 하위 ' + (impact.affectedSteps || []).length + '단계 재산정';
        }
        if (saved.lastValidation && saved.lastValidation.warnings && saved.lastValidation.warnings.length) {
            msg += ' (' + saved.lastValidation.warnings[0] + ')';
        }
        showMsg(msg, true);
        renderPanel();
    }

    async function init() {
        if (!scenarioId) {
            location.href = '/oc/cap-new/index.html';
            return;
        }
        try {
            defaults = await window.ocCapNewApi.defaults();
            try {
                scenarioCatalog = await window.ocCapNewApi.listScenarios();
            } catch (_) {
                scenarioCatalog = [];
            }
            scenario = await window.ocCapNewApi.getScenario(scenarioId);
            currentStep = Math.max(1, Math.min(8, scenario.currentStep || 1));
            renderPanel();
        } catch (e) {
            showMsg(e.message, false);
        }
    }

    btnPrev.addEventListener('click', () => {
        if (currentStep > 1) {
            currentStep -= 1;
            renderPanel();
        }
    });

    btnSave.addEventListener('click', async () => {
        try {
            await saveCurrentStep();
        } catch (e) {
            showMsg(e.message, false);
        }
    });

    btnNext.addEventListener('click', async () => {
        try {
            await saveCurrentStep();
            if (currentStep < 8) {
                currentStep += 1;
                renderPanel();
            } else {
                showMsg('8단계 산정이 완료되었습니다. 상태: COMPLETED', true);
            }
        } catch (e) {
            showMsg(e.message, false);
        }
    });

    panelEl.addEventListener('click', async (e) => {
        const jumpBtn = e.target.closest('[data-jump-step]');
        if (jumpBtn) {
            jumpToStep(jumpBtn.getAttribute('data-jump-step'));
            return;
        }
        const cloneBtn = e.target.closest('[data-capnew-clone]');
        if (cloneBtn) {
            if (cloneBtn.disabled) {
                showMsg('산정 완료(COMPLETED) 또는 확정(APPROVED) 상태에서만 복제할 수 있습니다.', false);
                return;
            }
            try {
                const cloned = await window.ocCapNewApi.cloneVersion(scenarioId);
                showMsg('새 버전이 생성되었습니다: ' + cloned.scenarioId, true);
                location.href = '/oc/cap-new/wizard.html?id=' + encodeURIComponent(cloned.scenarioId);
            } catch (err) {
                showMsg(err.message, false);
            }
            return;
        }
        const reviewBtn = e.target.closest('[data-capnew-review-request]');
        if (reviewBtn) {
            location.href = '/oc/cap-new/approved.html?id=' + encodeURIComponent(scenarioId);
        }
    });

    if (trackEl) {
        trackEl.addEventListener('click', (e) => {
            const item = e.target.closest('[data-step]');
            if (!item) return;
            jumpToStep(item.getAttribute('data-step'));
        });
        trackEl.addEventListener('keydown', (e) => {
            if (e.key !== 'Enter' && e.key !== ' ') return;
            const item = e.target.closest('[data-step]');
            if (!item) return;
            e.preventDefault();
            jumpToStep(item.getAttribute('data-step'));
        });
    }

    init();
})();
