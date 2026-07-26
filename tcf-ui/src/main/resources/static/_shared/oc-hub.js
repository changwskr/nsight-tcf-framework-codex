(function () {
    const CAP_API = '/api/oc/capacity/defaults';
    const STORAGE_VIEW = 'nsight.env.capacityView';
    const STORAGE_ASSESSMENT = 'nsight.env.lastAssessment';

    function uiUrl(path) {
        return typeof window.nsightUiUrl === 'function' ? window.nsightUiUrl(path) : path;
    }

    function setPill(id, text, tone) {
        const el = document.getElementById(id);
        if (!el) return;
        el.textContent = text;
        el.className = 'oc-hub-status__pill oc-hub-status__pill--' + (tone || 'muted');
        const dot = document.createElement('span');
        dot.className = 'oc-hub-status__dot';
        dot.setAttribute('aria-hidden', 'true');
        el.prepend(dot);
    }

    function hasEnvData() {
        try {
            return !!(sessionStorage.getItem(STORAGE_VIEW) || sessionStorage.getItem(STORAGE_ASSESSMENT));
        } catch (e) {
            return false;
        }
    }

    async function checkApi() {
        try {
            const res = await fetch(uiUrl(CAP_API), { method: 'GET' });
            if (!res.ok) throw new Error('HTTP ' + res.status);
            const body = await res.json();
            if (body?.success) {
                setPill('hubApiStatus', 'OC API 연결됨', 'ok');
            } else {
                throw new Error(body?.message || 'API 실패');
            }
        } catch (e) {
            setPill('hubApiStatus', 'OC API 미연결 (tcf-oc 기동 필요)', 'warn');
        }
    }

    function renderEnvStatus() {
        if (hasEnvData()) {
            setPill('hubEnvStatus', 'ENV 산정 데이터 있음 (브라우저 저장)', 'ok');
        } else {
            setPill('hubEnvStatus', 'ENV 산정 데이터 없음', 'muted');
        }
    }

    document.querySelectorAll('[data-oc-hub-link]').forEach(link => {
        const path = link.getAttribute('href');
        if (path) link.setAttribute('href', uiUrl(path));
    });

    renderEnvStatus();
    checkApi();
})();
