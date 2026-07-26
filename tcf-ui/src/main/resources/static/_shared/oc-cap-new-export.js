/**
 * NEW 용량산정 — Excel(.xlsx) 다운로드
 */
(function (global) {
    const API = '/api/oc/cap-new';

    function relayQuery() {
        if (typeof global.nsightRelayQuery === 'function') {
            return global.nsightRelayQuery();
        }
        return '';
    }

    function apiHeaders() {
        return {
            'Content-Type': 'application/json',
            'Accept': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        };
    }

    function parseFilename(contentDisposition) {
        if (!contentDisposition) return null;
        const star = /filename\*=UTF-8''([^;]+)/i.exec(contentDisposition);
        if (star) {
            try {
                return decodeURIComponent(star[1].trim());
            } catch (e) {
                return star[1].trim();
            }
        }
        const plain = /filename="?([^";]+)"?/i.exec(contentDisposition);
        return plain ? plain[1].trim() : null;
    }

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

    async function downloadBlob(res, fallbackName) {
        if (!res.ok) {
            let msg = 'Excel 생성 실패';
            try {
                const err = await res.json();
                msg = err?.message || msg;
            } catch (e) {
                const text = await res.text();
                if (text) msg = text.slice(0, 200);
            }
            throw new Error(msg);
        }
        const blob = await res.blob();
        const filename = parseFilename(res.headers.get('Content-Disposition')) || fallbackName;
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(url);
        notify('다운로드 완료: ' + filename, true);
    }

    async function exportScenario(scenarioId) {
        const res = await fetch(
            API + '/scenarios/' + encodeURIComponent(scenarioId) + '/export/excel' + relayQuery(),
            { method: 'POST', headers: apiHeaders(), body: '{}' }
        );
        await downloadBlob(res, 'NSIGHT_CAPNEW_' + scenarioId + '.xlsx');
    }

    async function exportCompare(payload) {
        const res = await fetch(API + '/export/excel' + relayQuery(), {
            method: 'POST',
            headers: apiHeaders(),
            body: JSON.stringify(Object.assign({ exportType: 'COMPARE' }, payload))
        });
        await downloadBlob(res, 'NSIGHT_CAPNEW_비교.xlsx');
    }

    function wireButtons() {
        document.addEventListener('click', async (ev) => {
            const scenarioBtn = ev.target.closest('[data-capnew-export="scenario"]');
            if (scenarioBtn) {
                const id = scenarioBtn.dataset.scenarioId;
                if (!id) return;
                ev.preventDefault();
                scenarioBtn.disabled = true;
                notify('Excel 생성 중…', true);
                try {
                    await exportScenario(id);
                } catch (e) {
                    notify(e.message || 'Excel 생성 실패', false);
                } finally {
                    scenarioBtn.disabled = false;
                }
                return;
            }
            const compareBtn = ev.target.closest('[data-capnew-export="compare"]');
            if (compareBtn) {
                ev.preventDefault();
                const ids = (compareBtn.dataset.scenarioIds || '').split(',').filter(Boolean);
                if (ids.length < 2) {
                    notify('비교 Excel은 시나리오 2개 이상이 필요합니다.', false);
                    return;
                }
                compareBtn.disabled = true;
                notify('비교 Excel 생성 중…', true);
                try {
                    await exportCompare({
                        scenarioIds: ids,
                        baselineScenarioId: compareBtn.dataset.baselineId || ids[0]
                    });
                } catch (e) {
                    notify(e.message || 'Excel 생성 실패', false);
                } finally {
                    compareBtn.disabled = false;
                }
            }
        });
    }

    global.ocCapNewExport = { exportScenario, exportCompare, wireButtons };
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', wireButtons);
    } else {
        wireButtons();
    }
})(window);
