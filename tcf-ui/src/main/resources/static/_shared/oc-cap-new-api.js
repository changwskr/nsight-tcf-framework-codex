/**
 * NEW 용량산정 API 클라이언트 (tcf-ui → /api/oc/cap-new relay)
 */
(function (global) {
    const API = '/api/oc/cap-new';

    function relayQuery() {
        if (typeof global.nsightRelayQuery === 'function') {
            return global.nsightRelayQuery();
        }
        return '';
    }

    async function request(method, path, body) {
        const options = {
            method,
            headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' }
        };
        if (body !== undefined) {
            options.body = typeof body === 'string' ? body : JSON.stringify(body);
        }
        const url = API + path + relayQuery();
        const response = await fetch(url, options);
        const text = await response.text();
        let json;
        try {
            json = JSON.parse(text);
        } catch (e) {
            throw new Error('API 응답을 해석할 수 없습니다: ' + text.slice(0, 200));
        }
        if (!response.ok || json.success === false) {
            throw new Error(json.message || 'API 요청에 실패했습니다.');
        }
        return json.data;
    }

    global.ocCapNewApi = {
        defaults: () => request('GET', '/defaults'),
        listTemplates: () => request('GET', '/templates'),
        getTemplate: (code) => request('GET', '/templates/' + encodeURIComponent(code)),
        listScenarios: (status) => request('GET', status ? '/scenarios?status=' + encodeURIComponent(status) : '/scenarios'),
        getScenario: (id) => request('GET', '/scenarios/' + encodeURIComponent(id)),
        createScenario: (payload) => request('POST', '/scenarios', payload),
        saveStep: (id, step, payload) => request('PUT', '/scenarios/' + encodeURIComponent(id) + '/step/' + step, { payload }),
        deleteScenario: (id) => request('DELETE', '/scenarios/' + encodeURIComponent(id)),
        compare: (payload) => request('POST', '/compare', payload),
        listApprovals: () => request('GET', '/approvals'),
        listScenarioApprovals: (id) => request('GET', '/scenarios/' + encodeURIComponent(id) + '/approvals'),
        approve: (id, payload) => request('POST', '/scenarios/' + encodeURIComponent(id) + '/approve', payload),
        revoke: (id, payload) => request('POST', '/scenarios/' + encodeURIComponent(id) + '/revoke', payload),
        cloneVersion: (id) => request('POST', '/scenarios/' + encodeURIComponent(id) + '/clone', {}),
        envHandoff: (id) => request('GET', '/scenarios/' + encodeURIComponent(id) + '/env-handoff'),
        legacyCompare: (id) => request('GET', '/scenarios/' + encodeURIComponent(id) + '/legacy-compare'),
        vmCompare: (id, profiles) => {
            let path = '/scenarios/' + encodeURIComponent(id) + '/vm-compare';
            if (profiles && profiles.length) {
                path += '?profiles=' + encodeURIComponent(profiles.join(','));
            }
            return request('GET', path);
        }
    };
})(window);
