/**
 * NEW 용량산정 → ENV-002 handoff (sessionStorage 연동)
 */
(function (global) {
    const STORAGE_VIEW = 'nsight.env.capacityView';
    const STORAGE_REQUEST = 'nsight.env.capacityRequest';
    const STORAGE_SOURCE = 'nsight.capnew.handoffSource';

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

    async function handoffToEnv(scenarioId) {
        if (!scenarioId) {
            throw new Error('시나리오 ID가 없습니다.');
        }
        const data = await global.ocCapNewApi.envHandoff(scenarioId);
        if (!data?.capacityRequest || !data?.capacityView) {
            throw new Error('ENV 연동 데이터가 비어 있습니다.');
        }
        try {
            sessionStorage.setItem(STORAGE_REQUEST, JSON.stringify(data.capacityRequest));
            sessionStorage.setItem(STORAGE_VIEW, JSON.stringify(data.capacityView));
            sessionStorage.setItem(STORAGE_SOURCE, JSON.stringify({
                scenarioId: data.capNewScenarioId,
                scenarioName: data.capNewScenarioName,
                handoffAt: data.handoffAt
            }));
        } catch (e) {
            throw new Error('sessionStorage 저장에 실패했습니다.');
        }
        notify('ENV-002로 이동합니다…', true);
        location.href = data.envPageUrl || '/oc/env-002.html';
    }

    function wireButtons() {
        document.addEventListener('click', async (ev) => {
            const btn = ev.target.closest('[data-capnew-env-handoff]');
            if (!btn) return;
            const id = btn.dataset.scenarioId || btn.getAttribute('data-capnew-env-handoff');
            if (!id) return;
            ev.preventDefault();
            btn.disabled = true;
            notify('ENV 연동 데이터 준비 중…', true);
            try {
                await handoffToEnv(id);
            } catch (e) {
                notify(e.message || 'ENV 연동 실패', false);
                btn.disabled = false;
            }
        });
    }

    global.ocCapNewEnvHandoff = { go: handoffToEnv, wireButtons };
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', wireButtons);
    } else {
        wireButtons();
    }
})(window);
