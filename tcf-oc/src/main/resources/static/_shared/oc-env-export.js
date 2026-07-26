/**
 * ENV-003 / ENV-004 / 종합 보고서 / Rule 점검 — Excel(.xlsx) 다운로드
 */
(function () {
    const API = '/api/oc/env';
    const STORAGE_VIEW = 'nsight.env.capacityView';
    const STORAGE_REQUEST = 'nsight.env.capacityRequest';
    const STORAGE_ASSESSMENT = 'nsight.env.lastAssessment';
    const STORAGE_ASSESSMENT_FULL = 'nsight.env.lastAssessmentFull';

    function apiHeaders() {
        return {
            'Content-Type': 'application/json',
            'X-GUID': crypto.randomUUID(),
            'X-USER-ID': 'ARCHITECT'
        };
    }

    function showExportStatus(msg, type) {
        if (typeof showStatus === 'function') {
            showStatus(msg, type);
        }
    }

    function loadCapacityView() {
        if (typeof window.nsightGetCapacityViewForExport === 'function') {
            return window.nsightGetCapacityViewForExport();
        }
        try {
            const raw = sessionStorage.getItem(STORAGE_VIEW);
            return raw ? JSON.parse(raw) : null;
        } catch (e) {
            return null;
        }
    }

    function loadCapacityRequest() {
        if (typeof window.nsightGetCapacityRequestForExport === 'function') {
            return window.nsightGetCapacityRequestForExport();
        }
        try {
            const raw = sessionStorage.getItem(STORAGE_REQUEST);
            return raw ? JSON.parse(raw) : null;
        } catch (e) {
            return null;
        }
    }

    function loadAssessmentMeta() {
        try {
            const raw = sessionStorage.getItem(STORAGE_ASSESSMENT);
            return raw ? JSON.parse(raw) : null;
        } catch (e) {
            return null;
        }
    }

    function loadAssessmentFullFromCache() {
        try {
            const raw = sessionStorage.getItem(STORAGE_ASSESSMENT_FULL);
            return raw ? JSON.parse(raw) : null;
        } catch (e) {
            return null;
        }
    }

    async function fetchAssessmentFull() {
        const cached = loadAssessmentFullFromCache();
        const meta = loadAssessmentMeta();
        if (!meta?.runId) {
            return cached;
        }
        const res = await fetch(`${API}/assessments/${encodeURIComponent(meta.runId)}`, {
            headers: apiHeaders()
        });
        const data = await res.json();
        if (data?.error?.resultCode === 'SUCCESS') {
            return data.body?.response ?? cached;
        }
        return cached;
    }

    async function fetchBaseline() {
        const projectId = document.getElementById('projectId')?.value?.trim() || 'nsight-message-mgmt';
        const envCode = document.getElementById('envCode')?.value?.trim() || 'local';
        const res = await fetch(
            `${API}/projects/baseline?projectId=${encodeURIComponent(projectId)}&envCode=${encodeURIComponent(envCode)}`,
            { headers: apiHeaders() }
        );
        const data = await res.json();
        if (data?.error?.resultCode === 'SUCCESS') {
            let baseline = data.body?.response;
            if (typeof window.nsightBuildBaselineFromCapacity === 'function') {
                const merged = window.nsightBuildBaselineFromCapacity(baseline);
                if (merged) baseline = merged;
            }
            return baseline;
        }
        return null;
    }

    async function buildPayload(exportType) {
        const capacityView = loadCapacityView();
        const needsCapacity = exportType !== 'RULE_CHECK';
        if (needsCapacity && !capacityView?.planner) {
            throw new Error('산정 데이터가 없습니다. ENV-002에서 「산정 실행」 후 다시 시도하세요.');
        }
        const payload = {
            exportType,
            capacityView: capacityView || null,
            capacityRequest: loadCapacityRequest(),
            assessmentRun: null,
            baseline: null
        };
        if (exportType === 'CHECK' || exportType === 'RULE_CHECK') {
            payload.assessmentRun = await fetchAssessmentFull();
        }
        if (exportType === 'RULE_CHECK') {
            payload.baseline = await fetchBaseline();
        }
        return payload;
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

    async function downloadExcel(exportType) {
        const btn = document.querySelector(`[data-env-export="${exportType}"]`);
        if (btn) btn.disabled = true;
        showExportStatus('Excel 생성 중…', 'info');
        try {
            const payload = await buildPayload(exportType);
            if (exportType === 'RULE_CHECK' && !payload.assessmentRun) {
                const ok = window.confirm(
                    'Rule 점검 실행 기록이 없습니다. 기준정보만 포함된 Excel을 받을까요?'
                );
                if (!ok) {
                    showExportStatus('다운로드 취소', 'info');
                    return;
                }
            }
            const res = await fetch(`${API}/export/excel`, {
                method: 'POST',
                headers: apiHeaders(),
                body: JSON.stringify(payload)
            });
            if (!res.ok) {
                let msg = 'Excel 생성 실패';
                try {
                    const err = await res.json();
                    msg = err?.error?.resultMessage || err?.error?.detailMessage || msg;
                } catch (e) {
                    msg = await res.text() || msg;
                }
                throw new Error(msg);
            }
            const blob = await res.blob();
            const filename = parseFilename(res.headers.get('Content-Disposition'))
                || `NSIGHT_${exportType}_${new Date().toISOString().slice(0, 10)}.xlsx`;
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = filename;
            document.body.appendChild(a);
            a.click();
            a.remove();
            URL.revokeObjectURL(url);
            showExportStatus(`다운로드 완료: ${filename}`, 'success');
        } catch (err) {
            showExportStatus(err.message || 'Excel 다운로드 실패', 'error');
        } finally {
            if (btn) btn.disabled = false;
        }
    }

    function wireExportButtons() {
        document.querySelectorAll('[data-env-export]').forEach(btn => {
            if (btn.dataset.exportWired === '1') return;
            btn.dataset.exportWired = '1';
            btn.addEventListener('click', () => downloadExcel(btn.getAttribute('data-env-export')));
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', wireExportButtons);
    } else {
        wireExportButtons();
    }
})();
