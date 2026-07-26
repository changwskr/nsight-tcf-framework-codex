/**
 * NEW 용량산정 — VM Profile 대안 비교 (설계서 §12)
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

    function judgmentClass(judgment) {
        if (judgment === '운영 권장') return 'oc-capnew-vm--recommended';
        if (judgment === '집중 위험' || judgment === '검토 필요') return 'oc-capnew-vm--risk';
        return '';
    }

    function fmt(v) {
        if (v === undefined || v === null || v === '') return '-';
        if (typeof v === 'number') return v.toLocaleString();
        return String(v);
    }

    function renderPanel(container, data) {
        if (!container || !data) return;
        const rows = (data.alternatives || []).map(row => `
            <tr class="${judgmentClass(row.judgment)}${row.selected ? ' oc-capnew-vm--selected' : ''}">
              <td>${escapeHtml(row.vmProfileLabel || row.vmProfileCode)}${row.selected ? ' <span class="oc-capnew-badge">현재</span>' : ''}</td>
              <td>${fmt(row.vmAdjustedTps)}</td>
              <td>${escapeHtml(row.requiredApDisplay || '-')}</td>
              <td>${fmt(row.totalCores)}</td>
              <td>${escapeHtml(row.failureBlastLabel || '-')}</td>
              <td>${escapeHtml(row.judgment || '-')}</td>
            </tr>`).join('');

        container.innerHTML = `
            <div class="oc-capnew-legacy-panel oc-capnew-vm-panel">
              <h3>VM 대안 비교 — ${escapeHtml(data.baselineLabel || data.baselineCode || '')} ${fmt(data.baselineTps)} TPS</h3>
              <p>${escapeHtml(data.summary || '')}</p>
              <table class="oc-capnew-table">
                <thead><tr>
                  <th>VM Profile</th><th>VM TPS</th><th>필요 AP</th><th>전체 Core</th><th>장애범위</th><th>판단</th>
                </tr></thead>
                <tbody>${rows}</tbody>
              </table>
              <p class="oc-capnew-msg oc-capnew-msg--ok" style="margin-top:0.75rem;">${escapeHtml(data.recommendation || '')}</p>
            </div>`;
        container.hidden = false;
    }

    async function runCompare(scenarioId, panelId) {
        if (!scenarioId || !global.ocCapNewApi) {
            throw new Error('시나리오 ID가 없습니다.');
        }
        notify('VM 대안 비교 중…', true);
        const data = await global.ocCapNewApi.vmCompare(scenarioId);
        const panel = panelId ? document.getElementById(panelId) : document.getElementById('vmComparePanel');
        renderPanel(panel, data);
        notify('VM 대안 비교 완료', true);
        return data;
    }

    function wireButtons() {
        document.addEventListener('click', async (ev) => {
            const btn = ev.target.closest('[data-capnew-vm-compare]');
            if (!btn) return;
            const id = btn.dataset.scenarioId;
            if (!id) return;
            ev.preventDefault();
            btn.disabled = true;
            try {
                await runCompare(id, btn.dataset.panelId || 'vmComparePanel');
            } catch (e) {
                notify(e.message || 'VM 대안 비교 실패', false);
            } finally {
                btn.disabled = false;
            }
        });
    }

    global.ocCapNewVmCompare = { runCompare, renderPanel, wireButtons };
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', wireButtons);
    } else {
        wireButtons();
    }
})(window);
