(function () {
    const API = '/api/oc/capacity';
    let lastResult = null;
    let selectedIndex = -1;

    const form = document.getElementById('capacityForm');
    const statusBar = document.getElementById('statusBar');
    const resultsPanel = document.getElementById('resultsPanel');
    const inputPreview = document.getElementById('inputPreview');
    const summaryLabel = document.getElementById('summaryLabel');
    const summaryDetail = document.getElementById('summaryDetail');
    const riskPills = document.getElementById('riskPills');
    const kpiGrid = document.getElementById('kpiGrid');
    const detailGrid = document.getElementById('detailGrid');
    const detailTitle = document.getElementById('detailTitle');
    const tomcatSnippet = document.getElementById('tomcatSnippet');

    function $(id) { return document.getElementById(id); }

    function showStatus(msg, ok) {
        statusBar.textContent = msg;
        statusBar.className = 'status-bar ' + (ok ? 'ok' : 'err');
        statusBar.classList.remove('hidden');
    }

    function escapeHtml(s) {
        if (s == null) return '';
        return String(s)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function statusClass(s) {
        if (s === 'CRITICAL') return 'status-critical';
        if (s === 'WARN') return 'status-warn';
        return 'status-ok';
    }

    function pillHtml(st, count) {
        const cls = st === 'critical' ? 'xcap-pill--critical' : st === 'warning' ? 'xcap-pill--warn' : 'xcap-pill--ok';
        const label = st === 'critical' ? '위험' : st === 'warning' ? '주의' : '정상';
        return '<span class="xcap-pill ' + cls + '">' + label + ' ' + (count || 0) + '</span>';
    }

    function selectedVmSpec() {
        const r = document.querySelector('input[name="vmSpec"]:checked');
        return r ? r.value : '8C64G';
    }

    function selectedSessionMin() {
        const r = document.querySelector('input[name="sessionTimeout"]:checked');
        return r ? parseInt(r.value, 10) : 60;
    }

    function selectedRates() {
        return Array.from(document.querySelectorAll('input[name="rate"]:checked'))
            .map((el) => parseInt(el.value, 10) / 100);
    }

    function selectedTimeouts() {
        return Array.from(document.querySelectorAll('input[name="timeout"]:checked'))
            .map((el) => parseInt(el.value, 10));
    }

    function expectedActualRequestFromPercent(totalUsers, peakPercent) {
        return Math.ceil(totalUsers * (peakPercent / 100));
    }

    function peakTpsFromActualRequestUsers(actualRequestUsers, timeoutSec) {
        if (timeoutSec <= 0) return 0;
        return Math.ceil(actualRequestUsers / timeoutSec);
    }

    function syncDerivedFields() {
        const b = parseInt($('branchCount').value, 10) || 0;
        const u = parseInt($('userPerBranch').value, 10) || 0;
        const total = b * u;
        $('totalUsers').value = total;
        const margin = parseFloat($('sessionMarginRate').value) || 0;
        $('designedSessions').value = Math.ceil(total * (1 + margin));
        updateVmTpsLabels();
        updateTpmcConditionText();
        renderScenarioTpsPreview();
        renderInputPreview();
    }

    function renderScenarioTpsPreview() {
        const el = $('scenarioTpsPreview');
        if (!el) return;
        const total = parseInt($('totalUsers').value, 10) || 0;
        const rates = selectedRates().slice().sort((a, b) => a - b);
        const timeouts = selectedTimeouts().slice().sort((a, b) => a - b);
        const tpmcPerTps = parseInt($('tpmcPerTps').value, 10) || REF_TPMC_PER_TPS;
        const formulaHtml =
            '<p class="xcap-scenario-tps-preview__formula" role="note">' +
            '<span class="env-vm-formula-banner__part">실요청자</span> ' +
            '<span class="env-vm-formula-banner__op">=</span> ' +
            '<span class="env-vm-formula-banner__part">⌈전체×요청률%⌉</span> ' +
            '<span class="env-vm-formula-banner__op">·</span> ' +
            '<span class="env-vm-formula-banner__eq">목표 TPS</span> ' +
            '<span class="env-vm-formula-banner__op">=</span> ' +
            '<span class="env-vm-formula-banner__part">⌈실요청자÷응답(초)⌉</span>' +
            (total > 0 ? ' · 전체 <strong>' + total.toLocaleString() + '</strong>명' : '') +
            '</p>';
        if (total <= 0 || !rates.length || !timeouts.length) {
            el.innerHTML = formulaHtml +
                '<p class="env-cap-summary__placeholder">지점·사용자 수와 시나리오(요청률·응답)를 선택하면 목표 TPS가 표시됩니다.</p>';
            return;
        }
        const rows = [];
        rates.forEach((rate) => {
            const pct = Math.round(rate * 100);
            const actual = expectedActualRequestFromPercent(total, pct);
            timeouts.forEach((sec) => {
                const tps = peakTpsFromActualRequestUsers(actual, sec);
                rows.push({
                    pct, sec, actual, tps, tpmc: tps * tpmcPerTps
                });
            });
        });
        const peakTps = rows.reduce((m, r) => Math.max(m, r.tps), 0);
        const body = rows.map((r) => {
            const peak = r.tps === peakTps;
            return '<tr' + (peak ? ' class="xcap-scenario-tps-preview__row--peak"' : '') + '>' +
                '<td>' + r.pct + '%</td>' +
                '<td>' + r.sec + 's</td>' +
                '<td>' + r.actual.toLocaleString() + '</td>' +
                '<td><strong>' + r.tps + '</strong>' +
                (peak ? '<span class="xcap-tps-sub">피크</span>' : '') + '</td>' +
                '<td>' + r.tpmc.toLocaleString() + '</td></tr>';
        }).join('');
        el.innerHTML = formulaHtml +
            '<div class="table-wrap">' +
            '<table class="dump-report__table dump-report__table--data xcap-scenario-tps-preview__table">' +
            '<thead><tr><th>요청률</th><th>응답</th><th>실요청자</th><th>목표 TPS</th><th>필요 TPMC</th></tr></thead>' +
            '<tbody>' + body + '</tbody></table></div>';
    }

    const REF_TPMC_PER_TPS = 3000;
    const REF_TPS_PER_CORE = 35;

    /** 업무 유형별 TPMC · Core TPS 기준 (화면 기준표와 동일) */
    const TPMC_WORKLOAD_GUIDE = [
        { tpmc: 1500, tpsCore: 71, label: '캐시 중심 단순 조회, SQL 부하 낮음' },
        { tpmc: 2000, tpsCore: 53, label: '일반 조회, 권한·로그·단건 DB 조회' },
        { tpmc: 3000, tpsCore: 36, label: '정보계 Single View, RDW 조회·마스킹·감사로그 포함' },
        { tpmc: 4000, tpsCore: 27, label: '복합 고객조회, 다중 SQL·조인 포함' },
        { tpmc: 5000, tpsCore: 21, label: '계정계 변경성 거래, 승인·원장·대외연계 포함' }
    ];

    function linkedTpsFromTpmc(tpmcPerTps) {
        const safe = Math.max(1, tpmcPerTps);
        const base = Math.max(1, Math.floor((REF_TPS_PER_CORE * REF_TPMC_PER_TPS) / safe));
        const min = Math.max(1, Math.floor((30 * REF_TPMC_PER_TPS) / safe));
        const max = Math.max(min, Math.floor((40 * REF_TPMC_PER_TPS) / safe));
        return { min, base, max, coreTpmcPerSec: base * safe };
    }

    function updateVmTpsLabels() {
        const tpsPerCore = parseInt($('tpsPerCore').value, 10) || 35;
        document.querySelectorAll('.env-vm-card__tps[data-vm-cores]').forEach((el) => {
            const cores = parseInt(el.getAttribute('data-vm-cores'), 10);
            el.textContent = 'VM TPS(기준) = ' + (cores * tpsPerCore).toLocaleString();
        });
    }

    function findWorkloadGuide(tpmcPerTps) {
        return TPMC_WORKLOAD_GUIDE.find((g) => g.tpmc === tpmcPerTps) || null;
    }

    function applyWorkloadGuide(tpmc, tpsCore) {
        $('tpmcPerTps').value = tpmc;
        $('tpsPerCore').value = tpsCore;
        const g = findWorkloadGuide(tpmc);
        if ($('tpsPerCoreHint')) {
            $('tpsPerCoreHint').textContent = g
                ? '기준표: 약 ' + g.tpsCore + ' TPS/Core'
                : 'TPMC 연동 · 기준 35 @ 3K';
        }
        highlightTpmcGuideRow(tpmc);
        updateVmTpsLabels();
        updateTpmcConditionText();
        renderScenarioTpsPreview();
        renderInputPreview();
    }

    function highlightTpmcGuideRow(tpmcPerTps) {
        document.querySelectorAll('.xcap-tpmc-guide__row').forEach((row) => {
            const match = parseInt(row.getAttribute('data-tpmc'), 10) === tpmcPerTps;
            row.classList.toggle('xcap-tpmc-guide__row--active', match);
        });
        const g = findWorkloadGuide(tpmcPerTps);
        const sel = $('tpmcWorkloadSelected');
        if (sel) {
            if (g) {
                sel.textContent = '선택: ' + g.tpmc.toLocaleString() + ' TPMC · 약 '
                    + g.tpsCore + ' TPS/Core — ' + g.label;
            } else {
                const tps = parseInt($('tpsPerCore').value, 10) || '—';
                sel.textContent = '선택: ' + tpmcPerTps.toLocaleString() + ' TPMC · Core당 TPS '
                    + tps + ' (기준표 외 직접 입력)';
            }
        }
    }

    function initTpmcWorkloadGuide() {
        document.querySelectorAll('.xcap-tpmc-guide__row').forEach((row) => {
            const apply = () => {
                applyWorkloadGuide(
                    parseInt(row.getAttribute('data-tpmc'), 10),
                    parseInt(row.getAttribute('data-tps-core'), 10)
                );
            };
            row.addEventListener('click', apply);
            row.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    apply();
                }
            });
        });
        highlightTpmcGuideRow(parseInt($('tpmcPerTps').value, 10) || REF_TPMC_PER_TPS);
    }

    function updateTpmcConditionText() {
        const tpmcPerTps = parseInt($('tpmcPerTps').value, 10) || REF_TPMC_PER_TPS;
        const guide = findWorkloadGuide(tpmcPerTps);
        const tpsPerCore = parseInt($('tpsPerCore').value, 10)
            || (guide ? guide.tpsCore : linkedTpsFromTpmc(tpmcPerTps).base);
        $('coreTpmcPerSec').value = (tpsPerCore * tpmcPerTps).toLocaleString();
        highlightTpmcGuideRow(tpmcPerTps);
        const tabEl = $('tpsTabTpmcCondition');
        if (tabEl) {
            tabEl.textContent = 'CAP-020: 필요 TPMC = 목표 TPS × 1 TPS당 TPMC(' + tpmcPerTps.toLocaleString() + ')';
        }
    }

    function buildPayload() {
        return {
            projectName: $('projectName').value,
            branchCount: parseInt($('branchCount').value, 10),
            userPerBranch: parseInt($('userPerBranch').value, 10),
            sessionMarginRate: parseFloat($('sessionMarginRate').value),
            sessionTimeoutMin: selectedSessionMin(),
            concurrentRequestRates: selectedRates(),
            targetResponseTimes: selectedTimeouts(),
            vmSpecCode: selectedVmSpec(),
            tpsPerCore: parseInt($('tpsPerCore').value, 10),
            tpmcPerTps: parseInt($('tpmcPerTps').value, 10),
            avgThreadHoldSec: parseFloat($('avgThreadHoldSec').value),
            threadMarginRate: parseFloat($('threadMarginRate').value),
            maxThreadMarginRate: parseFloat($('maxThreadMarginRate').value),
            apType: $('apType').value,
            activeActive: $('activeActive').checked,
            drValidation: $('drValidation').checked,
            validateDbPool: $('validateDbPool').checked,
            dbSessionLimit: parseInt($('dbSessionLimit').value, 10),
            avgDbConnectionHoldSec: parseFloat($('avgDbConnectionHoldSec').value),
            dbTransactionUsageRatio: parseFloat($('dbTransactionUsageRatio').value),
            poolSafetyFactor: parseFloat($('poolSafetyFactor').value),
            threadDbUsageRatio: parseFloat($('threadDbUsageRatio').value),
            minPoolPerVm: parseInt($('minPoolPerVm').value, 10)
        };
    }

    function syncApTypeDbHold() {
        const sv = $('apType').value === 'SINGLE_VIEW';
        if (!$('avgDbConnectionHoldSec').dataset.userEdited) {
            $('avgDbConnectionHoldSec').value = sv ? '0.20' : '0.15';
        }
        syncDbPoolFormulaGuide();
    }

    function readMinPoolPerVm() {
        return Math.max(1, parseInt($('minPoolPerVm').value, 10) || 30);
    }

    function syncMinPoolFormulaDisplay() {
        const minPool = readMinPoolPerVm();
        ['minPoolFormulaValue', 'minPoolDisplayValue', 'minPoolFormulaInline'].forEach((id) => {
            const el = $(id);
            if (el) el.textContent = String(minPool);
        });
    }

    function syncDbPoolFormulaGuide() {
        syncMinPoolFormulaDisplay();
        const live = $('dbPoolFormulaLive');
        if (!live) return;
        const hold = parseFloat($('avgDbConnectionHoldSec').value) || 0.15;
        const usage = parseFloat($('dbTransactionUsageRatio').value) || 1;
        const safety = parseFloat($('poolSafetyFactor').value) || 1.3;
        const threadDb = parseFloat($('threadDbUsageRatio').value) || 0.3;
        const minPool = readMinPoolPerVm();
        const pct = Math.round(threadDb * 100);
        live.innerHTML =
            '<strong>현재 입력값으로</strong> ② 산출 = <em>AP TPS</em> × ' + hold + 's × ' + usage + ' × ' + safety +
            ' · ③ 상한 = <em>VM당 Thread</em> × ' + pct + '% · ⑤=min(②,③) · ④=max(<strong>' + minPool + '</strong>, ⑤)' +
            ' <span class="xcap-dbpool-formula__note">AP TPS·Thread는 산정 실행 후 시나리오별로 결정됩니다.</span>';
    }

    function renderMinPoolCalcDetail(peak) {
        syncMinPoolFormulaDisplay();
        const el = $('minPoolCalcDetail');
        const midLabel = $('minPoolMidLabel');
        const minPool = readMinPoolPerVm();
        if (!el) return;
        if (!peak || !peak.dbPool || poolPerVmOf(peak) == null) {
            if (midLabel) midLabel.textContent = '②, ③';
            el.innerHTML = '<p class="env-cap-summary__placeholder">CAP-050 「산정 실행」 후 피크 시나리오 기준 ②·③·④ 숫자가 채워집니다.</p>';
            return;
        }
        const db = peak.dbPool;
        const theo = db.poolTheoretical;
        const ceil = db.poolCeiling;
        const sized = db.poolSized != null ? db.poolSized : Math.min(theo, ceil);
        const deployAp = peak.deploymentApCount > 0 ? peak.deploymentApCount : peak.requiredApCount;
        const beforeProfile = Math.max(minPool, sized);
        const finalP = poolPerVmOf(peak);
        const floorApplied = db.minPoolFloorApplied || sized < minPool;
        const profileCapped = finalP < beforeProfile;
        const apNote = deployAp > peak.requiredApCount
            ? '목표 ' + peak.targetTps + ' ÷ 배포 AP ' + deployAp + ' (센터당 필요 ' + peak.requiredApCount + ')'
            : '목표 ' + peak.targetTps + ' ÷ AP ' + deployAp;
        if (midLabel) {
            midLabel.textContent = '②=' + theo + ', ③=' + ceil;
        }
        let verdict = floorApplied
            ? '⑤ 용량권장=' + sized + ' · 운영최소=' + minPool + ' → <strong>④ 배포=' + finalP + '</strong>'
            + (finalP > ceil ? ' (③ 상한 ' + ceil + '보다 큼 — 운영하한 우선)' : '')
            : '⑤=' + sized + ' ≥ 운영최소 → ④=' + finalP;
        if (profileCapped) {
            verdict += ' · 프로파일 cap ' + beforeProfile + ' → ' + finalP;
        }
        el.innerHTML =
            '<dl class="xcap-minpool-formula__dl">' +
            '<div><dt>피크 시나리오</dt><dd>' + (peak.concurrentRate * 100).toFixed(0) + '% · ' +
            peak.responseTimeSec + 's · 필요 AP ' + peak.requiredApCount + '대' +
            (deployAp > peak.requiredApCount ? ' · 배포 AP ' + deployAp + '대 (A-A)' : '') + '</dd></div>' +
            '<div><dt>① AP TPS</dt><dd><strong>' + db.apTpsPerVm + '</strong> = ' + apNote + '</dd></div>' +
            '<div><dt>② 산출 Pool</dt><dd>' + theo + ' = ⌈AP TPS×점유×비율×안전⌉</dd></div>' +
            '<div><dt>③ 상한 Pool</dt><dd>' + ceil + ' = ⌈Thread×DB%⌉</dd></div>' +
            '<div><dt>⑤ 용량 권장</dt><dd><strong>' + sized + '</strong> = min(' + theo + ', ' + ceil + ')</dd></div>' +
            '<div><dt>운영 최소</dt><dd><strong class="xcap-minpool-formula__min">' + minPool + '</strong> (입력)</dd></div>' +
            '<div><dt>④ 배포 Pool</dt><dd><strong>' + beforeProfile + '</strong> = max(' + minPool + ', ' + sized + ')</dd></div>' +
            '<div><dt>④ 최종 (Hikari)</dt><dd><strong class="xcap-final-pool-num">' + finalP + '</strong>' +
            (profileCapped ? ' (프로파일 cap)' : '') + '</dd></div>' +
            '</dl>' +
            '<p class="xcap-minpool-formula__verdict">' + verdict + '</p>';
    }

    function applyDefaults(d) {
        if (!d) return;
        $('projectName').value = d.projectName || '';
        $('branchCount').value = d.branchCount;
        $('userPerBranch').value = d.userPerBranch;
        $('sessionMarginRate').value = String(d.sessionMarginRate);
        document.querySelectorAll('input[name="sessionTimeout"]').forEach((r) => {
            r.checked = parseInt(r.value, 10) === d.sessionTimeoutMin;
        });
        const vm = d.vmSpecCode || '8C64G';
        document.querySelectorAll('input[name="vmSpec"]').forEach((r) => {
            r.checked = r.value === vm;
        });
        $('tpsPerCore').value = d.tpsPerCore;
        $('tpmcPerTps').value = d.tpmcPerTps;
        const guide = findWorkloadGuide(d.tpmcPerTps);
        if (guide) {
            $('tpsPerCore').value = guide.tpsCore;
        } else {
            $('tpsPerCore').value = d.tpsPerCore;
        }
        $('avgThreadHoldSec').value = d.avgThreadHoldSec;
        $('threadMarginRate').value = d.threadMarginRate;
        $('maxThreadMarginRate').value = d.maxThreadMarginRate;
        $('apType').value = d.apType || 'GENERAL';
        $('activeActive').checked = d.activeActive !== false;
        $('drValidation').checked = d.drValidation !== false;
        $('validateDbPool').checked = d.validateDbPool !== false;
        $('dbSessionLimit').value = d.dbSessionLimit;
        if (d.avgDbConnectionHoldSec != null) {
            $('avgDbConnectionHoldSec').value = d.avgDbConnectionHoldSec;
        } else {
            syncApTypeDbHold();
        }
        $('dbTransactionUsageRatio').value = d.dbTransactionUsageRatio != null ? d.dbTransactionUsageRatio : 1.0;
        $('poolSafetyFactor').value = d.poolSafetyFactor != null ? d.poolSafetyFactor : 1.3;
        $('threadDbUsageRatio').value = d.threadDbUsageRatio != null ? d.threadDbUsageRatio : 0.30;
        $('minPoolPerVm').value = d.minPoolPerVm != null ? d.minPoolPerVm : 30;
        const rates = (d.concurrentRequestRates || []).map((r) => (r > 1 ? r : r * 100));
        document.querySelectorAll('input[name="rate"]').forEach((cb) => {
            cb.checked = rates.includes(parseInt(cb.value, 10));
        });
        document.querySelectorAll('input[name="timeout"]').forEach((cb) => {
            cb.checked = (d.targetResponseTimes || []).includes(parseInt(cb.value, 10));
        });
        syncDerivedFields();
        syncDbPoolFormulaGuide();
    }

    function renderInputPreview() {
        const p = buildPayload();
        const vmTps = (selectedVmSpec() === '8C64G' ? 8 : selectedVmSpec() === '16C128G' ? 16 : 32) * p.tpsPerCore;
        inputPreview.classList.remove('env-cap-summary--empty');
        inputPreview.innerHTML =
            '<h3 class="env-cap-summary__title">입력 조건 미리보기</h3>' +
            '<div class="env-cap-summary__cols">' +
            '<div class="env-cap-summary__block"><h4 class="env-cap-summary__heading">사용자</h4>' +
            '<dl class="env-cap-summary__dl">' +
            '<div><dt>전체</dt><dd><strong>' + (p.branchCount * p.userPerBranch).toLocaleString() + '</strong>명</dd></div>' +
            '<div><dt>설계 세션</dt><dd>' + $('designedSessions').value + '</dd></div>' +
            '</dl></div>' +
            '<div class="env-cap-summary__block"><h4 class="env-cap-summary__heading">VM · WAS</h4>' +
            '<dl class="env-cap-summary__dl">' +
            '<div><dt>VM</dt><dd>' + escapeHtml(selectedVmSpec()) + ' · TPS ' + vmTps.toLocaleString() + '</dd></div>' +
            '<div><dt>Thread</dt><dd>점유 ' + p.avgThreadHoldSec + 's × 여유 ' + p.threadMarginRate + '</dd></div>' +
            '</dl></div></div>';
    }

    function poolPerVmOf(row) {
        const n = row && row.dbPool ? row.dbPool.poolPerVm : null;
        return typeof n === 'number' && !Number.isNaN(n) ? n : null;
    }

    function peakDbPoolRow(rows) {
        if (!rows || !rows.length) return null;
        const withPool = rows.filter((r) => poolPerVmOf(r) != null);
        if (!withPool.length) return null;
        return withPool.slice().sort((a, b) => poolPerVmOf(b) - poolPerVmOf(a))[0];
    }

    function formatFinalPoolPeakText(peak) {
        if (!peak || poolPerVmOf(peak) == null) return '—';
        const pct = (peak.concurrentRate * 100).toFixed(0);
        return poolPerVmOf(peak) + '  (피크 ' + pct + '% · ' + peak.responseTimeSec + 's)';
    }

    function setFinalPoolPeakText(text) {
        const plain = text || '—';
        ['finalPoolPerVmOut', 'finalPoolPerVmResults'].forEach((id) => {
            const el = $(id);
            if (!el) return;
            if (el.tagName === 'INPUT') {
                el.value = plain;
            } else {
                el.textContent = plain;
            }
        });
        const banner = $('finalPoolPeakBanner');
        if (banner) {
            if (plain === '—') {
                banner.classList.add('hidden');
            } else {
                banner.classList.remove('hidden');
            }
        }
    }

    function renderDbPoolCap050Summary(data) {
        const rows = data.results || [];
        const peak = peakDbPoolRow(rows);
        const out = $('dbPoolCap050Out');
        const peakCard = $('dbPoolPeakCard');

        setFinalPoolPeakText(formatFinalPoolPeakText(peak));

        renderMinPoolCalcDetail(peak);

        if (!peak || !peak.dbPool || poolPerVmOf(peak) == null) {
            if (out) {
                out.className = 'xcap-dbpool-result env-cap-summary env-cap-summary--empty';
                out.innerHTML = '<p class="env-cap-summary__placeholder">산정 결과 없음</p>';
            }
            if (peakCard) peakCard.innerHTML = '';
            return;
        }

        const db = peak.dbPool;
        const pct = (peak.concurrentRate * 100).toFixed(0);
        const minPool = readMinPoolPerVm();

        if (out) {
            out.className = 'xcap-dbpool-result env-cap-summary';
            let tableRows = rows.map((row) => {
                const d = row.dbPool || {};
                const theo = d.poolTheoretical != null ? d.poolTheoretical : 0;
                const ceil = d.poolCeiling != null ? d.poolCeiling : 0;
                const sized = d.poolSized != null ? d.poolSized
                    : (d.poolTheoretical != null && d.poolCeiling != null ? Math.min(theo, ceil) : '—');
                return '<tr><td>' + (row.concurrentRate * 100).toFixed(0) + '%</td><td>' + row.responseTimeSec + 's</td>' +
                    '<td><strong>' + minPool + '</strong></td>' +
                    '<td>' + (d.poolTheoretical != null ? d.poolTheoretical : '—') + '</td>' +
                    '<td>' + (d.poolCeiling != null ? d.poolCeiling : '—') + '</td>' +
                    '<td>' + sized + '</td>' +
                    '<td><strong>' + (poolPerVmOf(row) != null ? poolPerVmOf(row) : '—') + '</strong></td>' +
                    '<td>' + (d.totalDbSessions != null ? d.totalDbSessions : '—') + '</td></tr>';
            }).join('');
            out.innerHTML =
                '<h4 class="env-cap-summary__heading">CAP-050 최종 Pool 결과 (VM당 maximumPoolSize)</h4>' +
                '<p class="xcap-dbpool-peak-inline">피크 권장값: <strong>' + db.poolPerVm + '</strong> / VM' +
                ' · 최소 ' + minPool + ' · DB Session 합 <strong>' + db.totalDbSessions + '</strong></p>' +
                '<div class="table-wrap"><table class="dump-report__table dump-report__table--data xcap-dbpool-mini">' +
                '<thead><tr><th>요청률</th><th>응답</th><th>최소</th><th>② 산출</th><th>③ 상한</th>' +
                '<th>⑤ 용량</th><th>④ 배포</th><th>Session 합</th></tr></thead>' +
                '<tbody>' + tableRows + '</tbody></table></div>';
        }

        if (peakCard) peakCard.innerHTML =
            '<div class="env-cap-load xcap-dbpool-peak-card">' +
            '<div class="env-cap-load__header"><span class="env-cap-load__label">CAP-050 최종 Pool (피크 시나리오)</span>' +
            '<span class="env-cap-load__tag">' + pct + '% · ' + peak.responseTimeSec + 's · 필요 AP ' + peak.requiredApCount +
            (peak.deploymentApCount > peak.requiredApCount ? ' · 배포 ' + peak.deploymentApCount : '') + '대</span></div>' +
            '<p class="env-cap-load__metric"><span class="env-cap-load__value">' + db.poolPerVm + '</span>' +
            '<span class="env-cap-load__unit">/ VM (maximumPoolSize)</span></p>' +
            '<p class="env-cap-load__desc">⑤ ' + (db.poolSized != null ? db.poolSized : Math.min(db.poolTheoretical, db.poolCeiling)) +
            ' · ④ max(' + minPool + ',⑤)=' + db.poolPerVm + ' · ② ' + db.poolTheoretical + ' · ③ ' + db.poolCeiling +
            ' · AP TPS ' + db.apTpsPerVm + ' · Session 합 ' + db.totalDbSessions + '</p>' +
            '<pre class="xcap-yml-snippet">spring.datasource.hikari.maximum-pool-size=' + db.poolPerVm + '\n' +
            'spring.datasource.hikari.minimum-idle=' + Math.max(10, Math.floor(db.poolPerVm * 0.2)) + '\n' +
            'spring.datasource.hikari.connection-timeout=3000</pre></div>';
    }

    function renderKpi(data) {
        const risk = data.riskSummary || {};
        riskPills.innerHTML =
            pillHtml('normal', risk.normal) +
            pillHtml('warning', risk.warning) +
            pillHtml('critical', risk.critical);

        const rows = data.results || [];
        const maxTps = rows.reduce((m, r) => Math.max(m, r.targetTps || 0), 0);
        const maxThread = rows.reduce((m, r) => Math.max(m, (r.wasThread && r.wasThread.totalCalculatedThreads) || 0), 0);
        const peakDb = peakDbPoolRow(rows);
        const peakPool = peakDb && poolPerVmOf(peakDb) != null ? poolPerVmOf(peakDb) : '—';

        kpiGrid.innerHTML =
            '<article class="xcap-kpi"><span class="xcap-kpi__label">전체 사용자</span>' +
            '<p class="xcap-kpi__value">' + data.totalUserCount.toLocaleString() + '<span class="xcap-kpi__unit">명</span></p></article>' +
            '<article class="xcap-kpi"><span class="xcap-kpi__label">설계 세션</span>' +
            '<p class="xcap-kpi__value">' + data.designedSessionCount.toLocaleString() + '</p></article>' +
            '<article class="xcap-kpi xcap-kpi--pool"><span class="xcap-kpi__label">최종 Pool/VM (피크)</span>' +
            '<p class="xcap-kpi__value">' + peakPool + '</p>' +
            '<p class="xcap-kpi__sub">Hikari maximumPoolSize</p></article>' +
            '<article class="xcap-kpi"><span class="xcap-kpi__label">VM TPS(기준)</span>' +
            '<p class="xcap-kpi__value">' + data.vmTpsAtBase.toLocaleString() + '<span class="xcap-kpi__unit">/VM</span></p>' +
            '<p class="xcap-kpi__sub">' + escapeHtml(data.vmProfileId) + '</p></article>' +
            '<article class="xcap-kpi"><span class="xcap-kpi__label">피크 TPS</span>' +
            '<p class="xcap-kpi__value">' + maxTps.toLocaleString() + ' <span class="xcap-kpi__unit">TPS</span></p>' +
            '<p class="xcap-kpi__sub">Thread ' + maxThread.toLocaleString() + '</p></article>';
    }

    function renderSummaryDetail(data) {
        summaryLabel.textContent = (data.scenarioLabel || '') + ' · ' + (data.scenarioId || '');
        summaryDetail.innerHTML =
            '<div class="env-cap-summary__formulas">' +
            '<h4 class="env-cap-summary__heading">산정 공식</h4>' +
            '<ol class="env-cap-summary__steps"><li>' + escapeHtml(data.summaryFormula || '') + '</li></ol>' +
            '<p class="env-cap-summary__note">시나리오 ' + (data.results ? data.results.length : 0) + '건 · 행 클릭 시 Tomcat 권장 설정 표시</p></div>';
    }

    function fmtApCount(n) {
        return n != null && n > 0 ? String(n) : '—';
    }

    function worstStatus() {
        const list = Array.from(arguments).filter(Boolean);
        if (list.includes('CRITICAL')) return 'CRITICAL';
        if (list.includes('WARN')) return 'WARN';
        return list[0] || 'NORMAL';
    }

    function shortStatusReason(msg) {
        if (!msg) return '';
        const s = String(msg);
        if (s.length <= 48) return s;
        return s.substring(0, 45) + '…';
    }

    function formatPoolCell(row) {
        const db = row.dbPool || {};
        const finalP = poolPerVmOf(row);
        if (finalP == null) return '—';
        const minP = readMinPoolPerVm();
        const theo = db.poolTheoretical != null ? db.poolTheoretical : '—';
        const ceil = db.poolCeiling != null ? db.poolCeiling : '—';
        const sized = db.poolSized != null ? db.poolSized
            : ((typeof theo === 'number' && typeof ceil === 'number') ? Math.min(theo, ceil) : '—');
        const minApplied = db.minPoolFloorApplied || (typeof sized === 'number' && finalP === minP && sized < minP);
        const formula = '⑤=' + sized + ' · ④=max(' + minP + ',⑤)=' + finalP
            + (minApplied ? ' ←운영최소' : '');
        return '<strong class="xcap-final-pool-num" title="' + escapeHtml(db.poolFormula || formula) + '">'
            + finalP + '</strong>' +
            '<span class="xcap-tps-sub">⑤' + sized + (minApplied ? '·운영' + minP : '') + '</span>';
    }

    function formatJudgmentCell(row) {
        const was = row.wasThread || {};
        const db = row.dbPool || {};
        const st = worstStatus(row.tpsStatus, was.status, db.status);
        const reason = row.tpsStatusReason || db.statusMessage || was.statusMessage || '';
        return '<span class="pill ' + statusClass(st) + '" title="' + escapeHtml(reason) + '">' + st
            + '</span><span class="xcap-tps-sub xcap-judge-reason">' + escapeHtml(shortStatusReason(reason))
            + '</span>';
    }

    function stepOrder(code) {
        const order = { '020': 1, '030': 2, '040': 3, '050': 4, 'ALL': 5 };
        return order[code] || 5;
    }

    function stepAtLeast(current, min) {
        return stepOrder(current) >= stepOrder(min);
    }

    function rowCells(row, idx, totalUserCount) {
        const was = row.wasThread || {};
        const db = row.dbPool || {};
        const st = worstStatus(row.tpsStatus, was.status, db.status);
        const pct = (row.concurrentRate * 100).toFixed(0) + '%';
        const sel = idx === selectedIndex ? ' xcap-row--selected' : '';
        const click = ' data-row-index="' + idx + '" class="xcap-data-row' + sel + ' ' + statusClass(st) + '"';
        const totalUsersCell = totalUserCount != null && totalUserCount > 0
            ? totalUserCount.toLocaleString()
            : '—';
        const apReq = fmtApCount(row.requiredApCount);
        const availTps = row.availableTps != null && row.availableTps > 0 ? row.availableTps : '—';
        const tpsOverAvail = typeof availTps === 'number' && row.targetTps > availTps;
        const availCell = typeof availTps === 'number'
            ? '<strong' + (tpsOverAvail ? ' class="xcap-tps-over"' : '') + '>' + availTps + '</strong>' +
            '<span class="xcap-tps-sub">' + (tpsOverAvail ? '목표 초과' : '여유') + '</span>'
            : '—';

        return {
            all: '<tr' + click + '>' +
                '<td>' + pct + '</td><td>' + row.responseTimeSec + 's</td>' +
                '<td>' + totalUsersCell + '</td>' +
                '<td>' + row.concurrentRequestUsers.toLocaleString() + '</td>' +
                '<td><strong>' + row.targetTps + '</strong></td>' +
                '<td>' + row.requiredTpmc.toLocaleString() + '</td>' +
                '<td>' + row.requiredCore + '</td>' +
                '<td><strong>' + apReq + '</strong></td>' +
                '<td>' + availCell + '</td>' +
                '<td>' + (was.totalCalculatedThreads || '—') + '</td>' +
                '<td>' + (was.recommendedMaxThreads || '—') + '</td>' +
                '<td class="xcap-pool-cell">' + formatPoolCell(row) + '</td>' +
                '<td class="xcap-judge-cell">' + formatJudgmentCell(row) + '</td></tr>',
            tps: '<tr' + click + '>' +
                '<td>' + pct + '</td><td>' + row.responseTimeSec + '</td>' +
                '<td>' + row.concurrentRequestUsers.toLocaleString() + '</td>' +
                '<td><strong>' + row.targetTps + '</strong></td>' +
                '<td title="' + row.targetTps + ' × TPMC/TPS">' + row.requiredTpmc.toLocaleString() +
                '<span class="xcap-tps-sub">' + row.targetTps + '×' + ($('tpmcPerTps').value || '') + '</span></td>' +
                '<td>' + row.requiredCore + '</td>' +
                '<td><span class="pill ' + statusClass(st) + '">' + st + '</span></td></tr>',
            ap: '<tr' + click + '>' +
                '<td>' + pct + '</td><td>' + row.targetTps + '</td>' +
                '<td>' + row.vmTpsAtBase + '</td>' +
                '<td><strong>' + apReq + '</strong></td>' +
                '<td>' + escapeHtml(row.vmProfileId) + '</td>' +
                '<td><span class="pill ' + statusClass(st) + '">' + st + '</span></td></tr>',
            was: '<tr' + click + '>' +
                '<td>' + pct + '</td><td>' + row.targetTps + '</td>' +
                '<td>' + ($('avgThreadHoldSec').value) + '</td>' +
                '<td>' + ($('threadMarginRate').value) + '</td>' +
                '<td>' + (was.totalCalculatedThreads || '—') + '</td>' +
                '<td>' + (was.threadsPerVm || '—') + '</td>' +
                '<td>' + (was.recommendedMaxThreads || '—') + '</td>' +
                '<td>' + (was.minSpareThreads || '—') + '</td>' +
                '<td>' + (was.acceptCount || '—') + '</td>' +
                '<td><span class="pill ' + statusClass(was.status) + '">' + (was.status || '—') + '</span></td></tr>',
            db: '<tr' + click + '>' +
                '<td>' + pct + '</td><td>' + row.responseTimeSec + 's</td>' +
                '<td>' + (db.apTpsPerVm != null ? db.apTpsPerVm : '—') + '</td>' +
                '<td>' + (db.poolTheoretical != null ? db.poolTheoretical : '—') + '</td>' +
                '<td>' + (db.poolCeiling != null ? db.poolCeiling : '—') + '</td>' +
                '<td>' + (db.poolSized != null ? db.poolSized : '—') + '</td>' +
                '<td class="xcap-final-pool-cell"><strong class="xcap-final-pool-num">' +
                (poolPerVmOf(row) != null ? poolPerVmOf(row) : '—') + '</strong></td>' +
                '<td>' + (db.totalDbSessions != null ? db.totalDbSessions : '—') + '</td>' +
                '<td>' + (db.threadPoolRatio != null ? db.threadPoolRatio : '—') + '</td>' +
                '<td><span class="pill ' + statusClass(db.status) + '">' + (db.status || '—') + '</span></td></tr>'
        };
    }

    function applyStepTablePlaceholders(step) {
        const code = step || 'ALL';
        if (!stepAtLeast(code, '030')) {
            $('bodyAp').innerHTML = emptyCapTableRow(6, 'CAP-030 「산정 실행」 후 AP 결과가 표시됩니다.');
        }
        if (!stepAtLeast(code, '040')) {
            $('bodyWas').innerHTML = emptyCapTableRow(10, 'CAP-040 「산정 실행」 후 WAS Thread 결과가 표시됩니다.');
            if ($('wasCards')) $('wasCards').innerHTML = '';
        }
        if (!stepAtLeast(code, '050')) {
            $('bodyDb').innerHTML = emptyCapTableRow(10, 'CAP-050 「산정 실행」 후 DB Pool 결과가 표시됩니다.');
            if ($('dbPoolPeakCard')) $('dbPoolPeakCard').innerHTML = '';
            setFinalPoolPeakText('—');
        }
    }

    function highlightCapSection(step) {
        document.querySelectorAll('.xcap-cap-section').forEach((el) => {
            el.classList.remove('xcap-cap-section--highlight');
        });
        const id = CAP_SECTION_IDS[step];
        if (id) {
            const section = document.getElementById(id);
            if (section) {
                section.classList.add('xcap-cap-section--highlight');
                expandCapSectionOnly(step);
            }
        }
    }

    function renderTables(data) {
        const rows = data.results || [];
        const step = data.calculatedStep || 'ALL';
        if (!rows.length) {
            $('bodyAll').innerHTML = '<tr><td colspan="13" class="env-empty-cell">결과 없음</td></tr>';
            ['bodyTps', 'bodyAp', 'bodyWas'].forEach((id) => {
                $(id).innerHTML = '<tr><td colspan="12" class="env-empty-cell">결과 없음</td></tr>';
            });
            $('bodyDb').innerHTML = '<tr><td colspan="10" class="env-empty-cell">결과 없음</td></tr>';
            $('wasCards').innerHTML = '';
            return;
        }
        const totalUserCount = data.totalUserCount;
        const all = [], tps = [], ap = [], was = [], db = [];
        rows.forEach((row, i) => {
            const c = rowCells(row, i, totalUserCount);
            all.push(c.all);
            tps.push(c.tps);
            if (stepAtLeast(step, '030')) ap.push(c.ap);
            if (stepAtLeast(step, '040')) was.push(c.was);
            if (stepAtLeast(step, '050')) db.push(c.db);
        });
        $('bodyAll').innerHTML = all.join('');
        if (stepAtLeast(step, '020')) $('bodyTps').innerHTML = tps.join('');
        if (stepAtLeast(step, '030') && ap.length) $('bodyAp').innerHTML = ap.join('');
        if (stepAtLeast(step, '040') && was.length) $('bodyWas').innerHTML = was.join('');
        if (stepAtLeast(step, '050') && db.length) $('bodyDb').innerHTML = db.join('');
        applyStepTablePlaceholders(step);

        const peak = rows.slice().sort((a, b) => b.targetTps - a.targetTps)[0];
        if (stepAtLeast(step, '040') && peak && peak.wasThread) {
            const w = peak.wasThread;
            const wasCards = $('wasCards');
            if (wasCards) wasCards.innerHTML =
                '<div class="env-cap-load">' +
                '<div class="env-cap-load__header"><span class="env-cap-load__label">피크 시나리오 WAS</span>' +
                '<span class="env-cap-load__tag">' + (peak.concurrentRate * 100).toFixed(0) + '% · ' + peak.responseTimeSec + 's</span></div>' +
                '<p class="env-cap-load__metric"><span class="env-cap-load__value">' + w.totalCalculatedThreads + '</span>' +
                '<span class="env-cap-load__unit">총 Thread</span></p>' +
                '<p class="env-cap-load__desc">VM당 ' + w.threadsPerVm + ' · maxThreads ' + w.recommendedMaxThreads +
                ' · minSpare ' + w.minSpareThreads + ' · accept ' + w.acceptCount + '</p></div>';
        }

        document.querySelectorAll('.xcap-data-row').forEach((tr) => {
            tr.addEventListener('click', () => {
                selectedIndex = parseInt(tr.getAttribute('data-row-index'), 10);
                renderTables(data);
                renderDetail(data.results[selectedIndex]);
            });
        });

        if (selectedIndex < 0 && rows.length) {
            selectedIndex = 0;
            renderDetail(rows[0]);
            renderTables(data);
        }
    }

    function renderDetail(row) {
        if (!row) return;
        const was = row.wasThread || {};
        const db = row.dbPool || {};
        detailTitle.textContent = (row.concurrentRate * 100).toFixed(0) + '% · 응답 ' + row.responseTimeSec
            + 's · TPS ' + row.targetTps;
        detailGrid.innerHTML =
            '<div class="xcap-detail-col"><h4>CAP-020 TPS</h4><dl class="env-cap-summary__dl">' +
            '<div><dt>실요청</dt><dd>' + row.concurrentRequestUsers.toLocaleString() + '</dd></div>' +
            '<div><dt>TPMC</dt><dd>' + row.requiredTpmc.toLocaleString() + '</dd></div>' +
            '<div><dt>판정</dt><dd>' + escapeHtml(row.tpsStatusReason) + '</dd></div></dl></div>' +
            '<div class="xcap-detail-col"><h4>CAP-030 AP</h4><dl class="env-cap-summary__dl">' +
            '<div><dt>필요 AP</dt><dd><strong>' + row.requiredApCount + '</strong></dd></div>' +
            '<div><dt>가용 TPS</dt><dd><strong>' + (row.availableTps || '—') + '</strong>' +
            ' (AP×VM TPS)</dd></div></dl></div>' +
            '<div class="xcap-detail-col"><h4>CAP-040 WAS</h4><dl class="env-cap-summary__dl">' +
            '<div><dt>총 Thread</dt><dd>' + was.totalCalculatedThreads + '</dd></div>' +
            '<div><dt>상태</dt><dd>' + escapeHtml(was.statusMessage) + '</dd></div></dl></div>' +
            '<div class="xcap-detail-col"><h4>CAP-050 DB</h4><dl class="env-cap-summary__dl">' +
            '<div><dt>① AP TPS</dt><dd>' + (db.apTpsPerVm || '—') +
            (row.deploymentApCount > 0 ? ' (목표÷배포AP ' + row.deploymentApCount + ')' : '') + '</dd></div>' +
            '<div><dt>②③ 산출/상한</dt><dd>' + (db.poolTheoretical || '—') + ' / ' + (db.poolCeiling || '—') + '</dd></div>' +
            '<div><dt>⑤ 용량 권장</dt><dd>' + (db.poolSized != null ? db.poolSized : '—') + '</dd></div>' +
            '<div><dt>④ 배포 Pool</dt><dd><strong class="xcap-final-pool-num">' +
            (poolPerVmOf(row) != null ? poolPerVmOf(row) : '—') + '</strong> (maximumPoolSize)</dd></div>' +
            '<div><dt>Session 합</dt><dd>' + db.totalDbSessions + '</dd></div>' +
            '<div><dt>공식</dt><dd class="xcap-formula-dd">' + escapeHtml(db.poolFormula || db.statusMessage) + '</dd></div></dl></div>';

        tomcatSnippet.textContent =
            'server.tomcat.threads.max=' + (was.recommendedMaxThreads || '—') + '\n' +
            'server.tomcat.threads.min-spare=' + (was.minSpareThreads || '—') + '\n' +
            'server.tomcat.accept-count=' + (was.acceptCount || '—') + '\n' +
            '# ' + (was.statusMessage || '');
    }

    function setWizardPhase(phase) {
        document.querySelectorAll('.xcap-wizard-track__item').forEach((el) => {
            const p = parseInt(el.getAttribute('data-phase'), 10);
            el.classList.toggle('xcap-wizard-track__item--active', p === phase);
            el.classList.toggle('xcap-wizard-track__item--done', p < phase);
        });
    }

    function setCapSectionExpanded(step, expanded) {
        const id = CAP_SECTION_IDS[step];
        if (!id) return;
        const section = document.getElementById(id);
        if (!section) return;
        section.classList.toggle('xcap-cap-section--collapsed', !expanded);
        const head = section.querySelector('.xcap-cap-section__head');
        if (head) head.setAttribute('aria-expanded', expanded ? 'true' : 'false');
    }

    function expandCapSectionOnly(step) {
        Object.keys(CAP_SECTION_IDS).forEach((s) => {
            setCapSectionExpanded(s, s === step);
        });
    }

    function collapseInputPanel(collapsed) {
        const section = $('cap010Section');
        const toggleBtn = $('toggleInputBtn');
        if (section) section.classList.toggle('xcap-input-collapsed', collapsed);
        if (toggleBtn) toggleBtn.hidden = !collapsed;
    }

    function initSectionAccordion() {
        document.querySelectorAll('.xcap-cap-section__head[role="button"]').forEach((head) => {
            const section = head.closest('.xcap-cap-section');
            if (!section) return;
            const toggle = () => {
                const willExpand = section.classList.contains('xcap-cap-section--collapsed');
                if (willExpand) {
                    document.querySelectorAll('.xcap-cap-section').forEach((el) => {
                        el.classList.add('xcap-cap-section--collapsed');
                        const h = el.querySelector('.xcap-cap-section__head');
                        if (h) h.setAttribute('aria-expanded', 'false');
                    });
                }
                section.classList.toggle('xcap-cap-section--collapsed', !willExpand);
                head.setAttribute('aria-expanded', willExpand ? 'true' : 'false');
                const step = section.getAttribute('data-cap');
                if (step) setFlowActive(step);
            };
            head.addEventListener('click', toggle);
            head.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    toggle();
                }
            });
        });
        $('toggleInputBtn')?.addEventListener('click', () => {
            collapseInputPanel(false);
            setWizardPhase(1);
            $('cap010Section')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
        });
    }

    function setFlowActive(step) {
        document.querySelectorAll('.xcap-flow__item').forEach((el) => {
            el.classList.toggle('xcap-flow__item--active', el.getAttribute('data-step') === step);
        });
    }

    const CAP_SECTION_IDS = {
        '020': 'cap020Section',
        '030': 'cap030Section',
        '040': 'cap040Section',
        '050': 'cap050Section'
    };

    function scrollToCapSection(step) {
        const id = CAP_SECTION_IDS[step];
        if (!id) return;
        const el = document.getElementById(id);
        if (el) {
            el.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
    }

    function emptyCapTableRow(colspan, message) {
        return '<tr><td colspan="' + colspan + '" class="env-empty-cell">' + message + '</td></tr>';
    }

    function initEmptyCapTables() {
        $('bodyAll').innerHTML = emptyCapTableRow(13, '「산정 실행」 후 전체 요약이 표시됩니다.');
        $('bodyTps').innerHTML = emptyCapTableRow(7, '「산정 실행」 후 CAP-020 TPS 결과가 표시됩니다.');
        $('bodyAp').innerHTML = emptyCapTableRow(7, '「산정 실행」 후 CAP-030 AP 결과가 표시됩니다.');
        $('bodyWas').innerHTML = emptyCapTableRow(10, '「산정 실행」 후 CAP-040 WAS 결과가 표시됩니다.');
        $('bodyDb').innerHTML = emptyCapTableRow(10, '「산정 실행」 후 CAP-050 DB Pool 결과가 표시됩니다.');
        if ($('wasCards')) $('wasCards').innerHTML = '';
        if ($('dbPoolPeakCard')) $('dbPoolPeakCard').innerHTML = '';
    }

    function renderResult(data) {
        lastResult = data;
        selectedIndex = -1;
        const step = data.calculatedStep || 'ALL';
        resultsPanel.classList.remove('hidden');
        resultsPanel.classList.remove('xcap-results--empty');
        resultsPanel.classList.add('xcap-results--ready');
        setWizardPhase(3);
        collapseInputPanel(true);
        setFlowActive(step === 'ALL' ? '020' : step);
        if (summaryLabel && data.calculatedStepLabel) {
            summaryLabel.textContent = (data.scenarioLabel || '') + ' · ' + data.calculatedStepLabel
                + (data.scenarioId ? ' (' + data.scenarioId + ')' : '');
        }
        updateTpmcConditionText();
        const tabEl = $('tpsTabTpmcCondition');
        if (tabEl && data.results && data.results.length) {
            const tpmc = parseInt($('tpmcPerTps').value, 10) || REF_TPMC_PER_TPS;
            tabEl.textContent = 'CAP-020 산정 조건: 필요 TPMC = 목표 TPS × 1 TPS당 TPMC('
                + tpmc.toLocaleString() + ') — 시나리오 ' + data.results.length + '건';
        }
        if (stepAtLeast(step, '050')) {
            renderDbPoolCap050Summary(data);
        } else {
            setFinalPoolPeakText('—');
            renderMinPoolCalcDetail(null);
        }
        try {
            renderKpi(data);
        } catch (err) {
            console.error('renderKpi', err);
        }
        try {
            renderSummaryDetail(data);
        } catch (err) {
            console.error('renderSummaryDetail', err);
        }
        try {
            renderTables(data);
        } catch (err) {
            console.error('renderTables', err);
        }
        if (stepAtLeast(step, '050')) {
            renderDbPoolCap050Summary(data);
        }
        if (step === 'ALL') {
            document.querySelectorAll('.xcap-cap-section').forEach((el) => {
                el.classList.remove('xcap-cap-section--highlight');
            });
            expandCapSectionOnly('020');
        } else {
            highlightCapSection(step);
        }
        scrollToCapSection(step === 'ALL' ? '020' : step);
        resultsPanel.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    async function runCalculate(calculationStep) {
        const payload = buildPayload();
        if (!payload.concurrentRequestRates.length || !payload.targetResponseTimes.length) {
            showStatus('동시요청률과 응답시간을 1개 이상 선택하세요.', false);
            return;
        }
        const isStep = calculationStep && calculationStep !== 'ALL';
        if (isStep) {
            payload.calculationStep = calculationStep;
        }
        const url = isStep ? API + '/calculate-step' : API + '/calculate';
        const stepLabel = isStep ? 'CAP-' + calculationStep : '전체';
        setWizardPhase(2);
        showStatus(stepLabel + ' 산정 중…', true);
        const buttons = document.querySelectorAll('#runBtn, .xcap-step-run');
        buttons.forEach((b) => { b.disabled = true; });
        try {
            const res = await fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const json = await res.json();
            if (!json.success) {
                showStatus(json.message || '산정 실패', false);
                return;
            }
            renderResult(json.data);
            showStatus(json.message || stepLabel + ' 산정 완료', true);
        } catch (err) {
            showStatus('산정 오류: ' + err.message, false);
        } finally {
            buttons.forEach((b) => { b.disabled = false; });
        }
    }

    function initCapNavigation() {
        document.querySelectorAll('.xcap-flow__item').forEach((el) => {
            el.style.cursor = 'pointer';
            el.addEventListener('click', () => {
                const step = el.getAttribute('data-step');
                setFlowActive(step);
                if (step === '010') {
                    $('cap010Section').scrollIntoView({ behavior: 'smooth', block: 'start' });
                    return;
                }
                if (CAP_SECTION_IDS[step]) {
                    expandCapSectionOnly(step);
                    scrollToCapSection(step);
                }
            });
        });
        document.querySelectorAll('.xcap-cap-jump__link').forEach((link) => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const step = link.getAttribute('data-step');
                setFlowActive(step);
                expandCapSectionOnly(step);
                scrollToCapSection(step);
            });
        });
    }

    async function loadDefaults() {
        const res = await fetch(API + '/defaults');
        const json = await res.json();
        if (json.success && json.data) {
            applyDefaults(json.data);
            showStatus('기본 산정 조건을 불러왔습니다.', true);
        } else {
            showStatus(json.message || '기본값 로드 실패', false);
        }
    }

    form.addEventListener('submit', (e) => {
        e.preventDefault();
        runCalculate('ALL');
    });

    document.querySelectorAll('.xcap-step-run').forEach((btn) => {
        btn.addEventListener('click', () => {
            const step = btn.getAttribute('data-step');
            if (step) runCalculate(step);
        });
    });

    $('defaultsBtn').addEventListener('click', loadDefaults);
    $('resetBtn').addEventListener('click', () => loadDefaults());
    ['branchCount', 'userPerBranch', 'sessionMarginRate', 'tpsPerCore', 'tpmcPerTps'].forEach((id) => {
        $(id).addEventListener('input', syncDerivedFields);
    });
    $('tpmcPerTps').addEventListener('input', () => {
        const tpmc = parseInt($('tpmcPerTps').value, 10) || REF_TPMC_PER_TPS;
        const guide = findWorkloadGuide(tpmc);
        $('tpsPerCore').value = guide ? guide.tpsCore : linkedTpsFromTpmc(tpmc).base;
        if ($('tpsPerCoreHint')) {
            $('tpsPerCoreHint').textContent = guide
                ? '기준표: 약 ' + guide.tpsCore + ' TPS/Core'
                : 'TPMC 연동 산출';
        }
        updateTpmcConditionText();
        updateVmTpsLabels();
        renderScenarioTpsPreview();
        renderInputPreview();
    });
    document.querySelectorAll('input[name="vmSpec"]').forEach((r) => {
        r.addEventListener('change', syncDerivedFields);
    });
    document.querySelectorAll('input[name="rate"], input[name="timeout"]').forEach((el) => {
        el.addEventListener('change', () => {
            renderScenarioTpsPreview();
            renderInputPreview();
            updateTpmcConditionText();
        });
    });

    initCapNavigation();
    initSectionAccordion();
    initEmptyCapTables();
    setWizardPhase(1);
    $('apType').addEventListener('change', syncApTypeDbHold);
    $('avgDbConnectionHoldSec').addEventListener('input', () => {
        $('avgDbConnectionHoldSec').dataset.userEdited = '1';
        syncDbPoolFormulaGuide();
    });
    ['dbTransactionUsageRatio', 'poolSafetyFactor', 'threadDbUsageRatio', 'minPoolPerVm'].forEach((id) => {
        $(id).addEventListener('input', syncDbPoolFormulaGuide);
    });

    initTpmcWorkloadGuide();
    syncDerivedFields();
    syncDbPoolFormulaGuide();
    loadDefaults();
})();
