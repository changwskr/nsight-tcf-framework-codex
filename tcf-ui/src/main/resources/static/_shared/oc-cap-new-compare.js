(function () {
    const pickListEl = document.getElementById('pickList');
    const baselineSelect = document.getElementById('baselineSelect');
    const btnCompare = document.getElementById('btnCompare');
    const msgEl = document.getElementById('capNewMsg');
    const resultEl = document.getElementById('compareResult');
    const summaryEl = document.getElementById('compareSummary');
    const recommendationEl = document.getElementById('compareRecommendation');
    const diffEl = document.getElementById('diffHighlights');
    const tableEl = document.getElementById('compareTable');

    let comparable = [];

    function showMsg(text, ok) {
        if (!msgEl) return;
        msgEl.textContent = text;
        msgEl.className = 'oc-capnew-msg ' + (ok ? 'oc-capnew-msg--ok' : 'oc-capnew-msg--error');
        msgEl.hidden = !text;
    }

    function escapeHtml(v) {
        return String(v ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    function fmt(v, unit) {
        if (v === undefined || v === null || v === '') return '-';
        const s = typeof v === 'number' ? v.toLocaleString() : String(v);
        return unit ? s + ' ' + unit : s;
    }

    function selectedIds() {
        return Array.from(document.querySelectorAll('.cmp-pick:checked')).map(el => el.value);
    }

    function renderPickList(items) {
        comparable = items.filter(i => i.status === 'COMPLETED' || i.status === 'APPROVED');
        if (comparable.length === 0) {
            pickListEl.innerHTML = '<p>비교 가능한 시나리오가 없습니다. Wizard STEP 8까지 완료하세요.</p>';
            baselineSelect.innerHTML = '';
            return;
        }

        const params = new URLSearchParams(location.search);
        const preselected = params.getAll('id');

        pickListEl.innerHTML = comparable.map(item => {
            const checked = preselected.includes(item.scenarioId) ? ' checked' : '';
            return `
                <label class="oc-capnew-card" style="display:flex;gap:0.75rem;align-items:flex-start;cursor:pointer;">
                    <input type="checkbox" class="cmp-pick" value="${escapeHtml(item.scenarioId)}"${checked}>
                    <span>
                        <strong>${escapeHtml(item.scenarioName)}</strong>
                        <span class="oc-capnew-status oc-capnew-status--${(item.status || 'draft').toLowerCase()}">${escapeHtml(item.status)}</span><br>
                        ${escapeHtml(item.projectName || '')} · ${escapeHtml(item.targetEnv || '')} · STEP ${item.currentStep || '-'}
                    </span>
                </label>`;
        }).join('');

        baselineSelect.innerHTML = comparable.map(item =>
            `<option value="${escapeHtml(item.scenarioId)}">${escapeHtml(item.scenarioName)}</option>`
        ).join('');

        if (preselected.length >= 2) {
            runCompare();
        }
    }

    let lastCompareIds = [];
    let lastBaselineId = '';

    function renderResult(data) {
        resultEl.style.display = 'block';
        summaryEl.textContent = data.summary || '';
        recommendationEl.textContent = data.recommendation || '';
        lastCompareIds = data.scenarioIds || selectedIds();
        lastBaselineId = data.baselineScenarioId || baselineSelect.value || lastCompareIds[0] || '';

        if (data.diffHighlights && data.diffHighlights.length) {
            diffEl.innerHTML = '<strong>차이 요약</strong><ul>' +
                data.diffHighlights.map(h => `<li>${escapeHtml(h)}</li>`).join('') + '</ul>';
        } else {
            diffEl.innerHTML = '';
        }

        const exportBar = document.getElementById('compareExportBar');
        if (exportBar) {
            exportBar.innerHTML = `
                <button type="button" class="tcf-btn tcf-btn--ghost" data-capnew-export="compare"
                    data-scenario-ids="${escapeHtml(lastCompareIds.join(','))}"
                    data-baseline-id="${escapeHtml(lastBaselineId)}">비교 Excel 다운로드</button>`;
        }

        const columns = data.columns || [];
        const rows = data.metricRows || [];
        const baselineId = data.baselineScenarioId;

        let html = '<thead><tr><th>지표</th>';
        columns.forEach(col => {
            const isBase = col.scenarioId === baselineId;
            html += `<th${isBase ? ' class="oc-capnew-compare-baseline"' : ''}>${escapeHtml(col.scenarioName)}${isBase ? ' (기준)' : ''}<br><small>${escapeHtml(col.overallJudgment || '')}</small></th>`;
        });
        html += '</tr></thead><tbody>';

        rows.forEach(row => {
            html += `<tr><th scope="row">${escapeHtml(row.label)}</th>`;
            (row.values || []).forEach((val, idx) => {
                const isBase = columns[idx] && columns[idx].scenarioId === baselineId;
                html += `<td${isBase ? ' class="oc-capnew-compare-baseline"' : ''}>${escapeHtml(fmt(val, row.unit))}</td>`;
            });
            html += '</tr>';
        });
        html += '</tbody>';
        tableEl.innerHTML = html;
    }

    async function runCompare() {
        const ids = selectedIds();
        if (ids.length < 2) {
            showMsg('2개 이상의 시나리오를 선택하세요.', false);
            return;
        }
        try {
            showMsg('', true);
            const data = await window.ocCapNewApi.compare({
                scenarioIds: ids,
                baselineScenarioId: baselineSelect.value || ids[0]
            });
            renderResult(data);
            showMsg('비교가 완료되었습니다.', true);
        } catch (e) {
            showMsg(e.message, false);
        }
    }

    async function init() {
        try {
            const items = await window.ocCapNewApi.listScenarios();
            renderPickList(items);
        } catch (e) {
            showMsg(e.message, false);
        }
    }

    btnCompare.addEventListener('click', runCompare);
    init();
})();
