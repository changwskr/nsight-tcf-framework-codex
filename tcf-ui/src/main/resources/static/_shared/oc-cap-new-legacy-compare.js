/**
 * NEW 용량산정 — 기존 CAP(/api/oc/capacity) 산정 결과 대조
 */
(function (global) {
    function notify(msg, ok) {
        if (typeof global.ocCapNewShowStatus === 'function') {
            global.ocCapNewShowStatus(msg, ok);
            return;
        }
        const el = document.getElementById('capNewMsg');
        if (!el) return;
        el.textContent = msg;
        el.className = 'oc-capnew-msg ' + (ok ? 'oc-capnew-msg--ok' : 'oc-capnew-msg--error');
        el.hidden = !msg;
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function statusClass(status) {
        if (status === 'MATCH') return 'oc-capnew-legacy--match';
        if (status === 'CLOSE') return 'oc-capnew-legacy--close';
        return 'oc-capnew-legacy--diff';
    }

    function overallLabel(status) {
        if (status === 'MATCH') return '일치';
        if (status === 'CLOSE') return '근접 일치';
        if (status === 'PARTIAL') return '부분 차이';
        return '차이 있음';
    }

    function fmtValue(v, unit) {
        if (v === undefined || v === null) return '-';
        const text = typeof v === 'number' ? v.toLocaleString() : String(v);
        return unit ? text + ' ' + unit : text;
    }

    function renderPanel(container, data) {
        if (!container || !data) return;
        const notes = (data.notes || []).map(n =>
            '<li>' + escapeHtml(n) + '</li>').join('');
        const rows = (data.metrics || []).map(row => `
            <tr class="${statusClass(row.status)}">
              <td>${escapeHtml(row.metricLabel || row.metricId)}</td>
              <td>${fmtValue(row.capNewValue, row.unit)}</td>
              <td>${fmtValue(row.legacyValue, row.unit)}</td>
              <td>${fmtValue(row.diff, row.unit)}</td>
              <td>${escapeHtml(row.status || '-')}</td>
            </tr>`).join('');

        container.innerHTML = `
            <div class="oc-capnew-legacy-panel">
              <h3>기존 CAP 대조 — ${escapeHtml(data.baselineLabel || data.baselineCode || '')}</h3>
              <p><strong>${escapeHtml(overallLabel(data.overallStatus))}</strong> · ${escapeHtml(data.summary || '')}</p>
              ${notes ? '<ul class="oc-capnew-legacy-notes">' + notes + '</ul>' : ''}
              <table class="oc-capnew-table">
                <thead><tr><th>지표</th><th>cap-new</th><th>기존 CAP</th><th>차이</th><th>판정</th></tr></thead>
                <tbody>${rows}</tbody>
              </table>
            </div>`;
        container.hidden = false;
    }

    async function runCompare(scenarioId, panelId) {
        if (!scenarioId || !global.ocCapNewApi) {
            throw new Error('시나리오 ID가 없습니다.');
        }
        notify('기존 CAP 대조 중…', true);
        const data = await global.ocCapNewApi.legacyCompare(scenarioId);
        const panel = panelId ? document.getElementById(panelId) : document.getElementById('legacyComparePanel');
        renderPanel(panel, data);
        notify('기존 CAP 대조 완료', true);
        return data;
    }

    function wireButtons() {
        document.addEventListener('click', async (ev) => {
            const btn = ev.target.closest('[data-capnew-legacy-compare]');
            if (!btn) return;
            const id = btn.dataset.scenarioId;
            if (!id) return;
            ev.preventDefault();
            btn.disabled = true;
            try {
                await runCompare(id, btn.dataset.panelId || 'legacyComparePanel');
            } catch (e) {
                notify(e.message || '기존 CAP 대조 실패', false);
            } finally {
                btn.disabled = false;
            }
        });
    }

    global.ocCapNewLegacyCompare = { runCompare, renderPanel, wireButtons };
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', wireButtons);
    } else {
        wireButtons();
    }
})(window);
