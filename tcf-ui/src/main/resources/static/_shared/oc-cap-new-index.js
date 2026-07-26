(function () {
    const templateGridEl = document.getElementById('templateGrid');
    const listEl = document.getElementById('scenarioList');
    const msgEl = document.getElementById('capNewMsg');

    function showMsg(text, ok) {
        if (!msgEl) return;
        msgEl.textContent = text;
        msgEl.className = 'oc-capnew-msg ' + (ok ? 'oc-capnew-msg--ok' : 'oc-capnew-msg--error');
        msgEl.hidden = !text;
    }

    function statusClass(status) {
        const key = (status || 'DRAFT').toLowerCase();
        return 'oc-capnew-status oc-capnew-status--' + key;
    }

    function purposeLabel(purpose) {
        const map = {
            NEW_BUILD: '신규 구축',
            CONFIG_CHECK: '설정 검증',
            DR_VALIDATION: 'DR 검증',
            SCALE_OUT: '증설 검토'
        };
        return map[purpose] || purpose || '';
    }

    function envLabel(env) {
        const map = { PROD: '운영', STG: '스테이징', DR: 'DR' };
        return map[env] || env || '';
    }

    function formatNumber(value) {
        const n = Number(value);
        if (!Number.isFinite(n)) return '-';
        return n.toLocaleString('ko-KR');
    }

    function renderTemplates(items) {
        if (!templateGridEl) return;
        if (!items || items.length === 0) {
            templateGridEl.innerHTML = '<p>템플릿을 불러올 수 없습니다.</p>';
            return;
        }
        templateGridEl.innerHTML = items.map(item => `
            <article class="oc-capnew-template-card" data-template-code="${escapeHtml(item.code)}">
                <div class="oc-capnew-template-card__head">
                    <h3>${escapeHtml(item.label)}</h3>
                    <span class="oc-capnew-template-card__env">${escapeHtml(envLabel(item.targetEnv))}</span>
                </div>
                <p class="oc-capnew-template-card__desc">${escapeHtml(item.description)}</p>
                <ul class="oc-capnew-template-card__metrics">
                    <li><span>VM</span><strong>${escapeHtml(item.vmProfileCode || '-')}</strong></li>
                    <li><span>사용자</span><strong>${formatNumber(item.totalUsers)}</strong></li>
                    <li><span>설계 TPS</span><strong>${formatNumber(item.designPeakTps)}</strong></li>
                    <li><span>배포 AP</span><strong>${formatNumber(item.deploymentAp)}</strong></li>
                </ul>
                <p class="oc-capnew-template-card__purpose">${escapeHtml(purposeLabel(item.purpose))}</p>
                <button type="button" class="tcf-btn oc-capnew-template-card__btn" data-template="${escapeHtml(item.code)}">
                    이 템플릿으로 시작
                </button>
            </article>
        `).join('');

        templateGridEl.querySelectorAll('[data-template]').forEach(btn => {
            btn.addEventListener('click', () => createFromTemplate(btn.getAttribute('data-template')));
        });
    }

    function renderList(items) {
        if (!listEl) return;
        if (!items || items.length === 0) {
            listEl.innerHTML = '<p>저장된 시나리오가 없습니다. 위 템플릿에서 신규 산정을 시작하세요.</p>';
            return;
        }
        listEl.innerHTML = items.map(item => `
            <article class="oc-capnew-card">
                <div style="display:flex;justify-content:space-between;gap:0.5rem;align-items:center;">
                    <h3>${escapeHtml(item.scenarioName || item.scenarioId)}</h3>
                    <span class="${statusClass(item.status)}">${escapeHtml(item.status || 'DRAFT')}</span>
                </div>
                <p>${escapeHtml(item.projectName || item.projectId || '')} · ${escapeHtml(item.targetEnv || '')}</p>
                <p>현재 STEP ${item.currentStep || 1} · 갱신 ${escapeHtml(item.updatedAt || '-')}</p>
                <p>
                  <a href="/oc/cap-new/wizard.html?id=${encodeURIComponent(item.scenarioId)}">Wizard 열기 →</a>
                  ${item.status === 'COMPLETED' || item.status === 'APPROVED'
                    ? ` · <a href="/oc/cap-new/compare.html?id=${encodeURIComponent(item.scenarioId)}">비교</a>`
                    : ''}
                  ${item.status === 'COMPLETED'
                    ? ` · <a href="/oc/cap-new/approved.html?id=${encodeURIComponent(item.scenarioId)}">확정</a>`
                    : ''}
                </p>
            </article>
        `).join('');
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    async function loadTemplates() {
        try {
            const items = await window.ocCapNewApi.listTemplates();
            renderTemplates(items);
        } catch (e) {
            if (templateGridEl) {
                templateGridEl.innerHTML = '<p>템플릿 목록을 불러오지 못했습니다.</p>';
            }
            showMsg(e.message, false);
        }
    }

    async function loadList() {
        try {
            const items = await window.ocCapNewApi.listScenarios();
            renderList(items);
        } catch (e) {
            showMsg(e.message, false);
        }
    }

    async function createFromTemplate(templateCode) {
        if (!templateCode) return;
        showMsg('시나리오를 생성하는 중…', true);
        try {
            const created = await window.ocCapNewApi.createScenario({ templateCode });
            location.href = '/oc/cap-new/wizard.html?id=' + encodeURIComponent(created.scenarioId);
        } catch (e) {
            showMsg(e.message, false);
        }
    }

    loadTemplates();
    loadList();
})();
