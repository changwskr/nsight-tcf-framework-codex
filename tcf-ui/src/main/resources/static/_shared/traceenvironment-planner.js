/**
 * 설계서 ENV-002~004 — 산정 조건 · TPS/VM 결과 · 계층별 설정 Grid
 */
(function () {
    if (typeof showStatus !== 'function') {
        window.showStatus = function showStatus(msg, type = 'info') {
            const bar = document.getElementById('statusBar');
            if (!bar) return;
            bar.textContent = msg;
            bar.className = `status-bar ${type}`;
            bar.classList.remove('hidden');
        };
    }
    const API = '/api/oc/env';
    const STORAGE_VIEW = 'nsight.env.capacityView';
    const STORAGE_REQUEST = 'nsight.env.capacityRequest';
    const STORAGE_ASSESSMENT = 'nsight.env.lastAssessment';
    const STORAGE_ENV004_TAB = 'nsight.env004.activeTab';
    const ENV_PAGE = document.body.dataset.envPage || '';
    const REF_TPMC = 3000;
    const REF_TPS_MIN = 30;
    const REF_TPS_BASE = 35;
    const REF_TPS_MAX = 40;
    const REF_CORE_TPMC = REF_TPS_BASE * REF_TPMC;
    const GB_PER_CORE_IAAS = 8;
    const VM_PROFILES = [
        { id: '8CORE-32GB', cores: 8, memoryGb: 32 },
        { id: '8CORE-64GB', cores: 8, memoryGb: 64 },
        { id: '16CORE-64GB', cores: 16, memoryGb: 64 },
        { id: '16CORE-128GB', cores: 16, memoryGb: 128 },
        { id: '32CORE-256GB', cores: 32, memoryGb: 256 }
    ];
    const LAYER_STACK_ORDER = ['UI', 'GSLB', 'L4', 'Apache', 'Tomcat', 'JVM', 'Spring Boot', 'MyBatis'];
    let capVmTouched = false;
    let analyzeTimer = null;

    function escapeHtml(t) {
        const d = document.createElement('div');
        d.textContent = t ?? '';
        return d.innerHTML;
    }

    function isSuccess(data) {
        return data?.error?.resultCode === 'SUCCESS';
    }

    function apiHeaders() {
        return {
            'Content-Type': 'application/json',
            'X-GUID': crypto.randomUUID(),
            'X-USER-ID': 'ARCHITECT'
        };
    }

    function checkedValues(name) {
        return Array.from(document.querySelectorAll(`input[name="${name}"]:checked`))
            .map(el => parseInt(el.value, 10))
            .filter(n => !Number.isNaN(n));
    }

    function selectedVm() {
        const r = document.querySelector('input[name="capVm"]:checked');
        return r ? r.value : '8CORE-32GB';
    }

    function syncTotalUsers() {
        const branches = parseInt(document.getElementById('capBranchCount').value, 10) || 0;
        const per = parseInt(document.getElementById('capUsersPerBranch').value, 10) || 0;
        const totalEl = document.getElementById('capTotalUsers');
        const hint = document.getElementById('capUsersAutoHint');
        if (branches > 0 && per > 0) {
            totalEl.value = branches * per;
            if (hint) hint.textContent = `= ${branches.toLocaleString()} × ${per}`;
        }
    }

    function toggleCustomVm() {
        const custom = selectedVm() === 'CUSTOM';
        const panel = document.getElementById('capCustomVmFields');
        if (panel) panel.classList.toggle('hidden', !custom);
        if (custom) panel?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    function isManualCoreTps() {
        return document.getElementById('capManualCoreTps')?.checked === true;
    }

    function setCoreTpsReadonly(linked) {
        ['capTpsPerCoreMin', 'capTpsPerCoreBase', 'capTpsPerCoreMax'].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.readOnly = linked;
        });
    }

    function deriveCoreTpsFromTpmc(tpmc) {
        const min = Math.max(1, Math.floor(REF_TPS_MIN * REF_TPMC / tpmc));
        const base = Math.max(1, Math.floor(REF_TPS_BASE * REF_TPMC / tpmc));
        const max = Math.max(min, Math.floor(REF_TPS_MAX * REF_TPMC / tpmc));
        return { min, base, max };
    }

    function getCoreTpsBands() {
        if (isManualCoreTps()) {
            return {
                min: parseInt(document.getElementById('capTpsPerCoreMin').value, 10) || REF_TPS_MIN,
                base: parseInt(document.getElementById('capTpsPerCoreBase').value, 10) || REF_TPS_BASE,
                max: parseInt(document.getElementById('capTpsPerCoreMax').value, 10) || REF_TPS_MAX
            };
        }
        const tpmc = parseInt(document.getElementById('capTpmcPerTps').value, 10) || REF_TPMC;
        return deriveCoreTpsFromTpmc(tpmc);
    }

    function formatVmTpsLabel(cores, bands, suffix) {
        const tMin = cores * bands.min;
        const tBase = cores * bands.base;
        const tMax = cores * bands.max;
        const extra = suffix ? ` · ${suffix}` : '';
        return `VM TPS ${tMin.toLocaleString()}~${tMax.toLocaleString()} (기준 ${tBase.toLocaleString()})${extra}`;
    }

    /** VM TPS = Core 수 × Core당 TPS — TPMC·Core TPS 변경 시 3개 VM 카드 동시 갱신 */
    function syncVmCardTps() {
        const bands = getCoreTpsBands();
        VM_PROFILES.forEach(p => {
            const el = document.querySelector(`[data-vm-tps-for="${p.id}"]`);
            if (el) {
                el.textContent = formatVmTpsLabel(
                    p.cores,
                    bands,
                    p.id === '32CORE-256GB' ? '특수 AP' : ''
                );
            }
        });
        const customEl = document.getElementById('capCustomVmTps');
        if (customEl) {
            const cores = parseInt(document.getElementById('capCustomCore').value, 10) || 8;
            customEl.textContent = formatVmTpsLabel(cores, bands, '직접 지정');
        }
    }

    /** 서버와 동일: Core TPS = (기준 TPS × 기준 TPMC) ÷ TPMC/TPS */
    function syncCoreTpsFromTpmc() {
        const tpmc = parseInt(document.getElementById('capTpmcPerTps').value, 10) || REF_TPMC;
        const { min, base, max } = deriveCoreTpsFromTpmc(tpmc);
        const coreTpmc = base * tpmc;
        if (!isManualCoreTps()) {
            document.getElementById('capTpsPerCoreMin').value = min;
            document.getElementById('capTpsPerCoreBase').value = base;
            document.getElementById('capTpsPerCoreMax').value = max;
        }
        const bands = getCoreTpsBands();
        document.getElementById('capCoreTpmcPerSec').value = (bands.base * tpmc).toLocaleString();
        const formula = document.getElementById('capTpmcCoreFormula');
        if (formula) {
            formula.textContent =
                `Core TPMC/초 ${(bands.base * tpmc).toLocaleString()} = ${bands.base} TPS/Core × ${tpmc.toLocaleString()} TPMC/TPS ` +
                `(기준 ${REF_TPS_BASE}×${REF_TPMC.toLocaleString()}=${REF_CORE_TPMC.toLocaleString()})`;
        }
        syncVmCardTps();
    }

    function collectRequest() {
        const vm = selectedVm();
        const isCustom = vm === 'CUSTOM';
        return {
            scenarioName: '화면 시나리오',
            branchCount: parseInt(document.getElementById('capBranchCount').value, 10) || 0,
            usersPerBranch: parseInt(document.getElementById('capUsersPerBranch').value, 10) || 0,
            totalUsers: parseInt(document.getElementById('capTotalUsers').value, 10) || 0,
            vmProfileId: isCustom ? null : vm,
            customVm: isCustom,
            customCore: isCustom ? (parseInt(document.getElementById('capCustomCore').value, 10) || 8) : 0,
            customMemoryGb: isCustom ? (parseInt(document.getElementById('capCustomMemory').value, 10) || 64) : 0,
            tpsPerCoreMin: parseInt(document.getElementById('capTpsPerCoreMin').value, 10) || 30,
            tpsPerCoreBase: parseInt(document.getElementById('capTpsPerCoreBase').value, 10) || 35,
            tpsPerCoreMax: parseInt(document.getElementById('capTpsPerCoreMax').value, 10) || 40,
            tpmcPerTps: parseInt(document.getElementById('capTpmcPerTps').value, 10) || 3000,
            manualCoreTps: isManualCoreTps(),
            actualRequestPercents: checkedValues('capPercent').length ? checkedValues('capPercent') : [3, 5, 10, 15],
            responseTimeoutSeconds: checkedValues('capTimeout').length ? checkedValues('capTimeout') : [3, 4, 5],
            sessionIdleMinutes: checkedValues('capSession').length ? checkedValues('capSession') : [60],
            activeActive: document.getElementById('capActiveActive').checked,
            drValidation: document.getElementById('capDrValidation').checked,
            validateDbPool: document.getElementById('capValidateDbPool').checked,
            includeSettingExamples: document.getElementById('capIncludeExamples').checked,
            hikariPoolPerVm: 0,
            dbSessionLimit: 500
        };
    }

    function applyDefaults(req) {
        if (!req) return;
        document.getElementById('capBranchCount').value = req.branchCount ?? 3600;
        document.getElementById('capUsersPerBranch').value = req.usersPerBranch ?? 6;
        document.getElementById('capTotalUsers').value = req.totalUsers ?? 21600;
        if (!capVmTouched) {
            const vm = req.customVm ? 'CUSTOM' : (req.vmProfileId || '8CORE-64GB');
            const radio = document.querySelector(`input[name="capVm"][value="${vm}"]`);
            if (radio) radio.checked = true;
        }
        if (req.customCore) document.getElementById('capCustomCore').value = req.customCore;
        if (req.customMemoryGb) document.getElementById('capCustomMemory').value = req.customMemoryGb;
        document.getElementById('capTpsPerCoreMin').value = req.tpsPerCoreMin ?? 30;
        document.getElementById('capTpsPerCoreBase').value = req.tpsPerCoreBase ?? 35;
        document.getElementById('capTpsPerCoreMax').value = req.tpsPerCoreMax ?? 40;
        document.getElementById('capTpmcPerTps').value = req.tpmcPerTps ?? 3000;
        document.getElementById('capManualCoreTps').checked = req.manualCoreTps === true;
        setCoreTpsReadonly(!req.manualCoreTps);
        syncCoreTpsFromTpmc();
        document.getElementById('capActiveActive').checked = req.activeActive !== false;
        document.getElementById('capDrValidation').checked = req.drValidation !== false;
        document.getElementById('capValidateDbPool').checked = req.validateDbPool !== false;
        document.getElementById('capIncludeExamples').checked = req.includeSettingExamples !== false;
        document.querySelectorAll('input[name="capPercent"]').forEach(cb => {
            cb.checked = (req.actualRequestPercents || [3, 5, 10, 15]).includes(parseInt(cb.value, 10));
        });
        document.querySelectorAll('input[name="capTimeout"]').forEach(cb => {
            cb.checked = (req.responseTimeoutSeconds || [3, 4, 5]).includes(parseInt(cb.value, 10));
        });
        document.querySelectorAll('input[name="capSession"]').forEach(cb => {
            cb.checked = (req.sessionIdleMinutes || [60, 90]).includes(parseInt(cb.value, 10));
        });
        syncTotalUsers();
        toggleCustomVm();
    }

    function statusPill(status, label) {
        const cls = status === 'NORMAL' ? 'ok' : status === 'CRITICAL' ? 'no' : status === 'WARN' ? '' : '';
        return `<span class="pill ${cls}">${escapeHtml(label || status)}</span>`;
    }

    function rowStatusClass(status) {
        if (status === 'CRITICAL') return 'env-row--crit';
        if (status === 'WARN') return 'env-row--warn';
        if (status === 'NORMAL') return 'env-row--ok';
        return '';
    }

    /** 2. VM 처리 능력 — 요청 1건 부하(TPMC/TPS) 가독성 블록 */
    function renderRequestLoadBlock(p) {
        const tpmc = p.tpmcPerTps ?? 0;
        const coreTpmc = p.coreTpmcPerSec ?? 0;
        const baseTps = p.tpsPerCoreBase ?? REF_TPS_BASE;
        const linked = p.coreTpsLinkedToTpmc;
        const linkHint = linked
            ? `Core당 TPS(기준) = ${coreTpmc.toLocaleString()} ÷ ${tpmc.toLocaleString()} = <strong>${baseTps}</strong> 건/초`
            : `Core당 TPS(기준) <strong>${baseTps}</strong> 건/초 — 직접 입력 (TPMC 역산 가능)`;
        return `
            <div class="env-cap-load" role="group" aria-label="요청 1건당 부하">
                <div class="env-cap-load__header">
                    <span class="env-cap-load__label">요청 1건당 부하</span>
                    <span class="env-cap-load__tag">업무 복잡도</span>
                </div>
                <p class="env-cap-load__metric">
                    <span class="env-cap-load__value">${tpmc.toLocaleString()}</span>
                    <span class="env-cap-load__unit">TPMC</span>
                    <span class="env-cap-load__per">/ 초당 1건 (TPS 1)</span>
                </p>
                <p class="env-cap-load__desc">
                    한 번의 요청을 처리할 때 드는 <strong>부하 점수</strong>입니다.
                    ENV-002의 「1 TPS당 TPMC」와 동일한 값입니다.
                </p>
                <ul class="env-cap-load__hints">
                    <li>값이 <strong>크면</strong> → DB·로직이 무거운 업무 → Core·VM TPS <em>감소</em></li>
                    <li>값이 <strong>작으면</strong> → 가벼운 업무 → Core·VM TPS <em>증가</em></li>
                </ul>
                <p class="env-cap-load__chain">${linkHint}</p>
                <p class="env-cap-load__example">
                    예) 목표 TPS 600건/초이면 전사 부하 ≈ 600 × ${tpmc.toLocaleString()} =
                    <strong>${(600 * tpmc).toLocaleString()}</strong> TPMC/초
                </p>
            </div>`;
    }

    function renderCapacitySummary(p) {
        const el = document.getElementById('capSummaryFormula');
        if (!el || !p) return;
        const link = p.coreTpsLinkedToTpmc
            ? '요청 부하(TPMC/TPS)를 바꾸면 Core·VM 초당 처리량이 함께 조정됩니다.'
            : 'Core당 TPS를 직접 입력했습니다 (TPMC 자동 연동 해제).';
        const aa = p.activeActive
            ? '적용 — 권장 VM 대수 = 단일 센터 산출 × 2'
            : '미적용 — 단일 센터 기준 VM 대수만 산출';
        el.className = 'env-cap-summary';
        el.innerHTML = `
            <h4 class="env-cap-summary__title">적용된 산정 조건 요약</h4>
            <div class="env-cap-summary__cols">
                <section class="env-cap-summary__block">
                    <h5 class="env-cap-summary__heading">1. 사용자 · 시나리오</h5>
                    <dl class="env-cap-summary__dl">
                        <div><dt>전체 사용자</dt><dd>${(p.totalUsers ?? 0).toLocaleString()}명</dd></div>
                        <div><dt>지점 × 인원</dt><dd>${(p.branchCount ?? 0).toLocaleString()} × ${p.usersPerBranch ?? 0}명</dd></div>
                        <div><dt>세션 설계</dt><dd>${(p.designSessions ?? 0).toLocaleString()}명 <span class="env-cap-summary__note">(전체×1.3)</span></dd></div>
                        <div><dt>시나리오</dt><dd>${escapeHtml(p.scenarioLabel || '—')}</dd></div>
                    </dl>
                </section>
                <section class="env-cap-summary__block env-cap-summary__block--vm">
                    <h5 class="env-cap-summary__heading">2. VM 한 대 처리 능력</h5>
                    ${renderRequestLoadBlock(p)}
                    <dl class="env-cap-summary__dl env-cap-summary__dl--vm">
                        <div><dt>선택 VM</dt><dd>${p.vmCores}코어 / ${p.vmMemoryGb}GB <span class="env-cap-summary__note">${escapeHtml(p.vmProfileId || '—')}</span></dd></div>
                        <div><dt>코어 처리 여력</dt><dd><strong>${(p.coreTpmcPerSec ?? 0).toLocaleString()}</strong> TPMC/초
                            <span class="env-cap-summary__note">코어 1개가 1초에 감당하는 부하 상한 (기준 ${p.tpsPerCoreBase} TPS × ${(p.tpmcPerTps ?? 0).toLocaleString()} TPMC)</span></dd></div>
                        <div><dt>Core당 TPS</dt><dd>${p.tpsPerCoreMin} / <strong>${p.tpsPerCoreBase}</strong> / ${p.tpsPerCoreMax} 건/초
                            <span class="env-cap-summary__note">보수 · 기준 · 여유</span></dd></div>
                        <div><dt>VM당 TPS</dt><dd>${(p.vmTpsAt30 ?? 0).toLocaleString()} / <strong>${(p.vmTpsAt35 ?? 0).toLocaleString()}</strong> / ${(p.vmTpsAt40 ?? 0).toLocaleString()} 건/초
                            <span class="env-cap-summary__note">${p.vmCores}코어 × Core당 TPS</span></dd></div>
                        <div><dt>연동</dt><dd>${escapeHtml(link)}</dd></div>
                        <div><dt>Active-Active</dt><dd>${aa}</dd></div>
                    </dl>
                </section>
            </div>
            <section class="env-cap-summary__block env-cap-summary__formulas">
                <h5 class="env-cap-summary__heading">3. 산정 식 (ENV-003 표 계산)</h5>
                <ol class="env-cap-summary__steps">
                    <li><strong>실요청자</strong> = 전체 사용자 × 동시 요청률(%)</li>
                    <li><strong>목표 TPS</strong> = 실요청자(명) ÷ 응답 Timeout(초)</li>
                    <li><strong>전사 TPMC</strong> = 목표 TPS × 요청 1건 부하 <strong>${(p.tpmcPerTps ?? 0).toLocaleString()}</strong> TPMC</li>
                    <li><strong>VM TPS(기준)</strong> = ${p.vmCores}코어 × Core당 TPS ${p.tpsPerCoreBase} = <strong>${(p.vmTpsAt35 ?? 0).toLocaleString()}</strong> 건/초</li>
                    <li><strong>필요 VM</strong> = 목표 TPS ÷ VM TPS(기준), 소수 올림${p.activeActive ? ' · A-A 권장 = 위 결과 × 2' : ''}</li>
                </ol>
            </section>`;
    }

    function isEightGbPerCore(cores, memoryGb) {
        if (!cores || !memoryGb) return false;
        return Math.abs(memoryGb - cores * GB_PER_CORE_IAAS) <= 1;
    }

    function piecewise(ram, anchors) {
        const [r0, r1, r2, r3, v0, v1, v2, v3] = anchors;
        if (ram <= r0) return v0;
        if (ram <= r1) return v0 + (v1 - v0) * (ram - r0) / (r1 - r0);
        if (ram <= r2) return v1 + (v2 - v1) * (ram - r1) / (r2 - r1);
        if (ram <= r3) return v2 + (v3 - v2) * (ram - r2) / (r3 - r2);
        return v3;
    }

    /** JvmSizingGuide.java 와 동일 — 코어·RAM 입력 기준 */
    function computeHeapGeneral(cores, memoryGb) {
        const c = Math.max(1, cores);
        const ram = memoryGb > 0 ? memoryGb : c * GB_PER_CORE_IAAS;
        if (c <= 8 && ram <= 40) return { minGb: 12, maxGb: 14 };
        if (isEightGbPerCore(c, ram)) {
            let minGb = Math.max(12, Math.round(c * 1.5));
            let maxGb = Math.max(minGb + 2, Math.round(c * 1.75));
            if (c >= 32) {
                minGb = Math.max(32, minGb);
                maxGb = Math.min(48, Math.max(minGb, maxGb));
            }
            return { minGb, maxGb };
        }
        const minGb = piecewise(ram, [32, 64, 128, 256, 12, 24, 32, 32]);
        const maxGb = piecewise(ram, [32, 64, 128, 256, 14, 28, 40, 48]);
        return { minGb: Math.min(minGb, maxGb), maxGb };
    }

    function computeHeapSvMaxGb(cores, memoryGb) {
        const c = Math.max(1, cores);
        const ram = memoryGb > 0 ? memoryGb : c * GB_PER_CORE_IAAS;
        if (c <= 8 && ram <= 40) return 14;
        return piecewise(ram, [32, 64, 128, 256, 14, 28, 40, 64]);
    }

    function jvmSizingNote(cores, memoryGb) {
        const c = Math.max(1, cores);
        const ram = memoryGb > 0 ? memoryGb : c * GB_PER_CORE_IAAS;
        if (c <= 8 && ram <= 40) return '8CORE/32GB 표준 · Scale-Out';
        if (isEightGbPerCore(c, ram)) {
            if (c >= 32) return '32Core·256GB — Heap 32~48GB, 256GB 전체 Heap 사용 금지';
            return '코어당 8GB RAM — Heap≈Core×(1.5~1.75)GB. Thread·Pool 상향 시 GC 동시 검토';
        }
        return `RAM ${ram}GB (${(ram / c).toFixed(1)}GB/Core) — 문서 앵커 보간`;
    }

    function deriveJvmSizing(planner) {
        if (!planner) return null;
        const cores = planner.vmCores || 8;
        const ramGb = planner.vmMemoryGb || cores * GB_PER_CORE_IAAS;
        const heap = computeHeapGeneral(cores, ramGb);
        const svMax = computeHeapSvMaxGb(cores, ramGb);
        const metaMb = ramGb >= 128 ? 512 : 256;
        const metaMaxMb = ramGb >= 128 ? 2048 : 512;
        const pct = (gb) => (ramGb > 0 ? Math.round(100 * gb / ramGb) : 0);
        const heapRatioNote = `VM RAM ${ramGb}GB (${jvmSizingNote(cores, ramGb)}) — 일반 Heap ${pct(heap.minGb)}~${pct(heap.maxGb)}%, SV ≤ ${pct(svMax)}%`;
        const svXms = Math.max(12, Math.min(32, svMax - 8));
        return {
            vmProfileId: planner.vmProfileId || `${cores}CORE-${ramGb}GB`,
            vmCores: cores,
            vmMemoryGb: ramGb,
            heapGeneralMinGb: heap.minGb,
            heapGeneralMaxGb: heap.maxGb,
            heapSingleViewMaxGb: svMax,
            gcAlgorithm: 'G1GC',
            maxGcPauseMillis: 200,
            threadStackSize: '512k',
            metaspaceSizeMb: metaMb,
            maxMetaspaceSizeMb: metaMaxMb,
            heapRatioNote,
            exampleOptsGeneral:
                `-Xms${heap.minGb}g -Xmx${heap.maxGb}g -Xss512k -XX:+UseG1GC -XX:MaxGCPauseMillis=200 `
                + '-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/logs/dump',
            exampleOptsSingleView:
                `-Xms${svXms}g -Xmx${svMax}g -Xss512k -XX:+UseG1GC -XX:MaxGCPauseMillis=200 `
                + `-XX:MetaspaceSize=${metaMb}m -XX:MaxMetaspaceSize=${metaMaxMb}m `
                + '-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/logs/dump',
            sizingNote: jvmSizingNote(cores, ramGb)
        };
    }

    function resolveJvmSizing(view) {
        if (!view) return null;
        if (view.jvmSizing && view.jvmSizing.heapGeneralMinGb) return view.jvmSizing;
        return view.planner ? deriveJvmSizing(view.planner) : null;
    }

    function layerGridHasJvm(grid) {
        return (grid || []).some(r => r.layer === 'JVM');
    }

    function buildJvmLayerRows(j) {
        const general = `${j.heapGeneralMinGb}~${j.heapGeneralMaxGb} GB`;
        const sv = `≤${j.heapSingleViewMaxGb} GB`;
        const gcRec = `${j.gcAlgorithm} · MaxGCPauseMillis ${j.maxGcPauseMillis}`;
        const meta = `${j.metaspaceSizeMb}~${j.maxMetaspaceSizeMb} MB`;
        const row = (label, key, rec, loc, ex, cur, guide) => ({
            layer: 'JVM',
            settingLabel: label,
            propertyKey: key,
            recommendedValue: rec,
            currentValue: cur,
            status: 'INFO',
            statusLabel: '참고',
            reason: '권장 범위 내',
            configLocation: loc,
            settingExample: ex,
            actionGuide: guide
        });
        return [
            row('Heap (일반 AP)', 'jvm.heap.general', general, 'setenv.sh · systemd · K8s env',
                j.exampleOptsGeneral, general.split('~')[0].trim(),
                'Thread·Pool 상향 시 Xmx 재검토. RAM 대비 과대 Heap 금지'),
            row('Heap (SingleView)', 'jvm.heap.singleview', sv, 'setenv.sh (SV 전용)',
                j.exampleOptsSingleView, '—', '대용량 조회·집계 AP — 일반 AP와 분리 기동'),
            row('GC', 'jvm.gc', gcRec, 'JVM 옵션', '-XX:+UseG1GC -XX:MaxGCPauseMillis=200',
                j.gcAlgorithm, 'STW·Full GC 빈도 모니터링'),
            row('Thread Stack', 'jvm.thread.stack', j.threadStackSize, 'JVM 옵션', '-Xss512k',
                j.threadStackSize, 'maxThreads 상향 시 RSS = Heap + 스택×스레드'),
            row('Metaspace', 'jvm.metaspace', meta, 'JVM 옵션',
                '-XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m', `${j.metaspaceSizeMb}m`,
                '동적 클래스·SV 모듈 증가 시 상한 조정'),
            row('HeapDump OOM', 'jvm.heapdump', 'On · /logs/dump', 'JVM 옵션',
                '-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/logs/dump', 'On',
                'OOM 시 MAT 분석 — 운영 디스크 여유 확보')
        ];
    }

    function injectJvmLayerRows(grid, j) {
        const rows = [...(grid || [])];
        if (layerGridHasJvm(rows)) return rows;
        const jvmRows = buildJvmLayerRows(j);
        let insertAt = rows.length;
        for (let i = rows.length - 1; i >= 0; i--) {
            if (rows[i].layer === 'Tomcat') {
                insertAt = i + 1;
                break;
            }
        }
        rows.splice(insertAt, 0, ...jvmRows);
        return rows;
    }

    function rebuildStackLayersFromGrid(grid) {
        const byLayer = {};
        (grid || []).forEach(r => {
            if (!byLayer[r.layer]) byLayer[r.layer] = [];
            byLayer[r.layer].push({
                settingLabel: r.settingLabel,
                propertyKey: r.propertyKey,
                configLocation: r.configLocation || '',
                actualValue: r.currentValue,
                recommendedValue: r.recommendedValue,
                status: r.status,
                statusLabel: r.statusLabel,
                reason: r.reason,
                settingExample: r.settingExample || '',
                actionGuide: r.actionGuide || ''
            });
        });
        const layerDesc = {
            UI: 'WebTopSuite · 단말/채널',
            GSLB: 'DNS TTL · Health · 센터 라우팅',
            L4: 'Sticky · Health · Idle · LB · MaxConn',
            Apache: 'Proxy · KeepAlive',
            Tomcat: 'Thread · Queue · Session',
            JVM: 'Heap · G1GC · Metaspace · OOM Dump',
            'Spring Boot': 'Tomcat · Session · Async · TX',
            MyBatis: 'SQL Timeout · Fetch'
        };
        const layerId = {
            UI: 'UI', GSLB: 'GSLB', L4: 'L4', Apache: 'APACHE', Tomcat: 'TOMCAT',
            JVM: 'JVM', 'Spring Boot': 'SPRINGBOOT', MyBatis: 'MYBATIS'
        };
        let order = 1;
        return LAYER_STACK_ORDER.filter(l => byLayer[l]).map(layerName => {
            const settings = byLayer[layerName];
            const valid = settings.every(s => s.status !== 'CRITICAL');
            return {
                order: order++,
                layerId: layerId[layerName] || layerName.toUpperCase(),
                layerName,
                description: layerDesc[layerName] || layerName,
                layerValid: valid,
                settings
            };
        });
    }

    function normalizeCapacityView(view) {
        if (!view?.planner) return view;
        const jvm = resolveJvmSizing(view);
        const layerGrid = injectJvmLayerRows(view.layerGrid, jvm);
        const stackLayers = layerGridHasJvm(layerGrid)
            ? rebuildStackLayersFromGrid(layerGrid)
            : view.stackLayers;
        return { ...view, jvmSizing: jvm, layerGrid, stackLayers };
    }

    function renderJvmSizingHtml(j, opts = {}) {
        if (!j) return '';
        const compact = opts.compact === true;
        const generalRange = `${j.heapGeneralMinGb}~${j.heapGeneralMaxGb} GB`;
        const svMax = `≤${j.heapSingleViewMaxGb} GB`;
        const examples = compact ? '' : `
            <details class="env-env004-jvm__examples">
                <summary>기동 옵션 예시 (일반 AP / SingleView)</summary>
                <pre class="env-env004-jvm__code">${escapeHtml(j.exampleOptsGeneral)}</pre>
                <pre class="env-env004-jvm__code env-env004-jvm__code--sv">${escapeHtml(j.exampleOptsSingleView)}</pre>
            </details>`;
        const wrapClass = compact ? 'env-check-report__jvm' : 'env-env004-jvm';
        return `
            <div class="${wrapClass}">
                <h3 class="env-env004-jvm__title">JVM 사이징 <span class="env-env004-jvm__profile">${escapeHtml(j.vmProfileId)} · ${j.vmCores}코어 / ${j.vmMemoryGb}GB RAM</span></h3>
                <p class="env-env004-jvm__note">${escapeHtml(j.heapRatioNote)}</p>
                <div class="env-env004-jvm__cols">
                    <section class="env-env004-jvm__block">
                        <h4 class="env-env004-jvm__heading">Heap</h4>
                        <dl class="env-env004-jvm__dl">
                            <div><dt>일반 AP</dt><dd><strong>${generalRange}</strong> <span class="env-env004-jvm__tag">Xms~Xmx</span></dd></div>
                            <div><dt>SingleView</dt><dd><strong>${svMax}</strong> <span class="env-env004-jvm__tag">SV 전용</span></dd></div>
                        </dl>
                    </section>
                    <section class="env-env004-jvm__block">
                        <h4 class="env-env004-jvm__heading">GC · 스레드 · Metaspace</h4>
                        <dl class="env-env004-jvm__dl">
                            <div><dt>GC</dt><dd>${escapeHtml(j.gcAlgorithm)} · MaxGCPauseMillis ${j.maxGcPauseMillis}ms</dd></div>
                            <div><dt>Thread Stack</dt><dd>-Xss${escapeHtml(j.threadStackSize)}</dd></div>
                            <div><dt>Metaspace</dt><dd>${j.metaspaceSizeMb}~${j.maxMetaspaceSizeMb} MB</dd></div>
                            <div><dt>OOM Dump</dt><dd>HeapDumpOnOOM → /logs/dump</dd></div>
                        </dl>
                    </section>
                </div>
                <p class="env-env004-jvm__lead">${escapeHtml(j.sizingNote)}</p>
                ${examples}
            </div>`;
    }

    function persistCapacityView(view) {
        try {
            const normalized = normalizeCapacityView(view);
            sessionStorage.setItem(STORAGE_VIEW, JSON.stringify(normalized));
            if (document.getElementById('capacityDesignForm')) {
                sessionStorage.setItem(STORAGE_REQUEST, JSON.stringify(collectRequest()));
            }
        } catch (e) {
            /* sessionStorage unavailable */
        }
    }

    function loadPersistedCapacityView() {
        try {
            const raw = sessionStorage.getItem(STORAGE_VIEW);
            return raw ? normalizeCapacityView(JSON.parse(raw)) : null;
        } catch (e) {
            return null;
        }
    }

    async function refreshCapacityViewFromServer() {
        const req = loadPersistedRequest();
        if (!req) return loadPersistedCapacityView();
        try {
            const res = await fetch(`${API}/capacity-design/analyze`, {
                method: 'POST',
                headers: apiHeaders(),
                body: JSON.stringify(req)
            });
            const data = await res.json();
            if (!isSuccess(data)) return loadPersistedCapacityView();
            const view = normalizeCapacityView(data.body?.response);
            persistCapacityView(view);
            return view;
        } catch (e) {
            return loadPersistedCapacityView();
        }
    }

    function loadPersistedRequest() {
        try {
            const raw = sessionStorage.getItem(STORAGE_REQUEST);
            return raw ? JSON.parse(raw) : null;
        } catch (e) {
            return null;
        }
    }

    function uniqueSorted(values) {
        return [...new Set(values)].filter(n => !Number.isNaN(n)).sort((a, b) => a - b);
    }

    /** ENV-004 — Core당 TPS 보수/기준/여유 중 선정(기준) 수치 강조 */
    function renderCoreTpsCriteriaHtml(p) {
        const min = p.tpsPerCoreMin ?? REF_TPS_MIN;
        const base = p.tpsPerCoreBase ?? REF_TPS_BASE;
        const max = p.tpsPerCoreMax ?? REF_TPS_MAX;
        const tpmc = p.tpmcPerTps ?? REF_TPMC;
        const coreTpmc = p.coreTpmcPerSec ?? base * tpmc;
        const linked = p.coreTpsLinkedToTpmc;
        const vmTpsBase = p.vmTpsAt35 ?? p.vmCores * base;

        const chip = (value, role, selected) => `
            <span class="env-core-tps-pick__item${selected ? ' env-core-tps-pick__item--selected' : ''}">
                <span class="env-core-tps-pick__role">${role}</span>
                <span class="env-core-tps-pick__val">${value}</span>
                <span class="env-core-tps-pick__unit">TPS</span>
                ${selected ? '<span class="env-core-tps-pick__badge">선정 · 점검 기준</span>' : ''}
            </span>`;

        const linkBlock = linked
            ? `<p class="env-core-tps-pick__link env-core-tps-pick__link--on">TPMC 연동 ON</p>
               <p class="env-core-tps-pick__formula">
                   Core TPMC/초 <strong>${coreTpmc.toLocaleString()}</strong>
                   ÷ TPMC/TPS <strong>${tpmc.toLocaleString()}</strong>
                   = <strong>${base}</strong> TPS/Core <span class="env-core-tps-pick__formula-hint">(기준 선정)</span>
               </p>
               <p class="env-core-tps-pick__formula env-core-tps-pick__formula--sub">
                   (= ${base} × ${tpmc.toLocaleString()} · 기준 ${REF_TPS_BASE}×${REF_TPMC.toLocaleString()}에서 역산)
               </p>`
            : `<p class="env-core-tps-pick__link env-core-tps-pick__link--off">TPMC 연동 OFF · Core당 TPS 직접 입력</p>
               <p class="env-core-tps-pick__formula">
                   산정·계층 Grid는 <strong>기준 ${base}</strong> TPS/Core만 사용 (보수 ${min} · 여유 ${max}는 참고)
               </p>`;

        return `
            <div class="env-core-tps-pick">
                <div class="env-core-tps-pick__chips">
                    ${chip(min, '보수 (min)', false)}
                    ${chip(base, '기준 (base)', true)}
                    ${chip(max, '여유 (max)', false)}
                </div>
                ${linkBlock}
                <p class="env-core-tps-pick__vm">
                    VM TPS(선정) = ${p.vmCores}코어 × <strong>${base}</strong> TPS/Core
                    = <strong>${vmTpsBase.toLocaleString()}</strong> 건/초
                </p>
            </div>`;
    }

    /** ENV-004 — 점검 기준값(사용자·TPS·세션·VM 등) 전체 표시 */
    function renderEnv004CriteriaPanel(view) {
        const el = document.getElementById('env004CriteriaPanel');
        if (!el || !view?.planner) return;
        const p = view.planner;
        const req = loadPersistedRequest();
        const ex = pickExampleVmRow(p);
        const percents = req?.actualRequestPercents?.length
            ? req.actualRequestPercents
            : uniqueSorted((p.vmResults || []).map(r => r.requestRatePercent));
        const timeouts = req?.responseTimeoutSeconds?.length
            ? req.responseTimeoutSeconds
            : uniqueSorted((p.vmResults || []).map(r => r.timeoutSec));
        const sessions = req?.sessionIdleMinutes?.length
            ? req.sessionIdleMinutes
            : uniqueSorted([p.primarySessionMinutes, view.activeSessionMinutes].filter(Boolean));
        const timeoutSec = view.activeResponseTimeoutSec ?? ex?.timeoutSec ?? timeouts[0] ?? 3;
        const sessionMin = view.activeSessionMinutes ?? p.primarySessionMinutes ?? sessions[0] ?? 60;
        const pctLabel = percents.map(n => n + '%').join(', ');
        const timeoutLabel = timeouts.map(n => n + '초').join(', ');
        const sessionLabel = sessions.map(n => n + '분').join(', ');
        const aa = p.activeActive ? '적용 (권장 VM ×2)' : '미적용';
        const coreTpsBlock = renderCoreTpsCriteriaHtml(p);
        const jvmCrit = resolveJvmSizing(view);
        const jvmCritBlock = jvmCrit ? `
                <section class="env-env004-criteria__block env-env004-criteria__block--jvm">
                    <h4 class="env-env004-criteria__heading">4. JVM 사이징</h4>
                    <dl class="env-env004-criteria__dl">
                        <div><dt>일반 Heap</dt><dd><strong>${jvmCrit.heapGeneralMinGb}~${jvmCrit.heapGeneralMaxGb} GB</strong></dd></div>
                        <div><dt>SingleView</dt><dd><strong>≤${jvmCrit.heapSingleViewMaxGb} GB</strong></dd></div>
                        <div><dt>GC</dt><dd>${escapeHtml(jvmCrit.gcAlgorithm)} · ${jvmCrit.maxGcPauseMillis}ms</dd></div>
                    </dl>
                    <p class="env-env004-criteria__note">상세·기동 옵션은 <strong>JVM</strong> 탭</p>
                </section>` : '';

        const repBlock = ex ? `
            <section class="env-env004-criteria__block env-env004-criteria__block--rep">
                <h4 class="env-env004-criteria__heading">대표 산정 시나리오 (${ex.requestRatePercent}% · ${ex.timeoutSec}초)</h4>
                <dl class="env-env004-criteria__dl">
                    <div><dt>동시 요청률</dt><dd>${ex.requestRatePercent}%</dd></div>
                    <div><dt>실요청자</dt><dd><strong>${ex.realRequesters.toLocaleString()}</strong>명</dd></div>
                    <div><dt>응답 Timeout</dt><dd>${ex.timeoutSec}초</dd></div>
                    <div><dt>목표 TPS</dt><dd><strong>${ex.targetTps.toLocaleString()}</strong> 건/초</dd></div>
                    <div><dt>전사 TPMC</dt><dd>${ex.requiredTpmc.toLocaleString()} /초</dd></div>
                    <div><dt>필요 VM</dt><dd>${ex.requiredVmSingleCenter}대 (A-A 권장 ${ex.recommendedVmActiveActive}대)</dd></div>
                    <div><dt>DB Pool 총량</dt><dd>${ex.dbPoolTotal.toLocaleString()}</dd></div>
                </dl>
            </section>` : '';

        el.className = 'env-env004-criteria';
        el.innerHTML = `
            <h3 class="env-env004-criteria__title">점검 기준값 <span class="env-env004-criteria__id">${escapeHtml(view.scenarioId || '')}</span></h3>
            <p class="env-env004-criteria__scenario">${escapeHtml(p.scenarioLabel || '—')}</p>
            <div class="env-env004-criteria__cols">
                <section class="env-env004-criteria__block">
                    <h4 class="env-env004-criteria__heading">1. 사용자 · 세션</h4>
                    <dl class="env-env004-criteria__dl">
                        <div><dt>지점 수</dt><dd>${(p.branchCount ?? 0).toLocaleString()}</dd></div>
                        <div><dt>지점당 사용자</dt><dd>${p.usersPerBranch ?? 0}명</dd></div>
                        <div><dt>전체 사용자</dt><dd><strong>${(p.totalUsers ?? 0).toLocaleString()}</strong>명</dd></div>
                        <div><dt>설계 세션</dt><dd>${(p.designSessions ?? 0).toLocaleString()}명 <span class="env-env004-criteria__note">(전체×1.3)</span></dd></div>
                        <div><dt>세션 Idle</dt><dd>${sessionLabel} <span class="env-env004-criteria__note">점검 기준 ${sessionMin}분</span></dd></div>
                    </dl>
                </section>
                <section class="env-env004-criteria__block">
                    <h4 class="env-env004-criteria__heading">2. 시나리오 조합 (ENV-002)</h4>
                    <dl class="env-env004-criteria__dl">
                        <div><dt>동시 요청률</dt><dd>${pctLabel || '—'}</dd></div>
                        <div><dt>응답 Timeout</dt><dd>${timeoutLabel || '—'} <span class="env-env004-criteria__note">Grid 기준 ${timeoutSec}초</span></dd></div>
                        <div><dt>세션</dt><dd>${sessionLabel || '—'}</dd></div>
                    </dl>
                </section>
                <section class="env-env004-criteria__block env-env004-criteria__block--vm">
                    <h4 class="env-env004-criteria__heading">3. VM · 부하</h4>
                    <dl class="env-env004-criteria__dl">
                        <div><dt>VM 프로파일</dt><dd>${escapeHtml(p.vmProfileId || '—')} · ${p.vmCores}코어 / ${p.vmMemoryGb}GB</dd></div>
                        <div><dt>요청 1건 부하</dt><dd><strong>${(p.tpmcPerTps ?? 0).toLocaleString()}</strong> TPMC/TPS</dd></div>
                        <div><dt>Active-Active</dt><dd>${aa}</dd></div>
                    </dl>
                    <div class="env-env004-criteria__core-tps-wrap">
                        <span class="env-env004-criteria__core-tps-label">Core당 TPS · 선정값</span>
                        ${coreTpsBlock}
                    </div>
                </section>
                ${jvmCritBlock}
            </div>
            ${repBlock}`;
    }

    /** ENV-004 — JVM Heap·GC 사이징 (VM 프로파일 연동) */
    function renderEnv004JvmPanel(view) {
        const el = document.getElementById('env004JvmPanel');
        if (!el) return;
        const j = resolveJvmSizing(view);
        if (!j) {
            el.className = 'env-env004-jvm env-env004-jvm--empty';
            el.innerHTML =
                '<p class="env-env004-jvm__placeholder">ENV-002 산정 실행 후 VM 프로파일에 맞는 JVM Heap·GC 권장이 표시됩니다.</p>';
            return;
        }
        el.className = 'env-env004-jvm';
        el.innerHTML = renderJvmSizingHtml(j);
    }

    function setNoDataBanner(visible) {
        const banner = document.getElementById('envNoDataBanner');
        if (banner) banner.classList.toggle('hidden', !visible);
    }

    function applyCapacityView(view) {
        view = normalizeCapacityView(view);
        if (!view?.planner) {
            setNoDataBanner(true);
            return;
        }
        setNoDataBanner(false);
        const p = enrichPlannerFromEnv002(view.planner);
        view = { ...view, planner: p };
        if (document.getElementById('dashScenarioId')) {
            renderDashboard(view);
        }
        if (document.getElementById('capSummaryFormula')) {
            renderCapacitySummary(p);
        }
        if (document.getElementById('env003ScenarioLabel') || document.getElementById('capVmResultBody')) {
            document.getElementById('env003ScenarioLabel') &&
                (document.getElementById('env003ScenarioLabel').textContent =
                    `${p.scenarioLabel} · VM 기준 처리량 ${(p.vmTpsAt35 ?? 0).toLocaleString()}건/초 (${p.vmCores}코어×${p.tpsPerCoreBase} TPS)`);
            renderEnv003FormulaGuide(p);
            renderVmResults(p);
        }
        if (document.getElementById('env004TabNav')) {
            renderEnv004CriteriaPanel(view);
            renderEnv004LayerTabs(view);
        } else if (document.getElementById('env004CriteriaPanel') || document.getElementById('layerGridBody')) {
            renderEnv004CriteriaPanel(view);
            renderEnv004JvmPanel(view);
            renderLayerGrid(view.layerGrid);
            renderStackLayers(view.stackLayers);
        } else if (document.getElementById('env004JvmPanel')) {
            renderEnv004JvmPanel(view);
        }
    }

    function renderDashboard(view) {
        const p = view.planner;
        const risk = p.riskSummary || {};
        const dashScenario = document.getElementById('dashScenarioId');
        if (!dashScenario) return;
        dashScenario.textContent = view.scenarioId || '—';
        document.getElementById('dashDesignSessions').textContent =
            (p.designSessions ?? '—').toLocaleString() + ' (설계)';
        document.getElementById('dashRiskNormal').textContent = risk.normal ?? 0;
        document.getElementById('dashRiskWarn').textContent = risk.warning ?? 0;
        document.getElementById('dashRiskCrit').textContent = risk.critical ?? 0;
        const badge = document.getElementById('stackValidBadge');
        if (badge) {
            badge.textContent = view.stackValid ? 'STACK OK' : 'STACK 점검';
            badge.className = 'env-score ' + (view.stackValid ? 'env-score--match' : 'env-score--warn');
        }
        const env003Label = document.getElementById('env003ScenarioLabel');
        if (env003Label) {
            env003Label.textContent =
                `${p.scenarioLabel} · VM 기준 처리량 ${(p.vmTpsAt35 ?? 0).toLocaleString()}건/초 (${p.vmCores}코어×${p.tpsPerCoreBase} TPS)`;
        }
        renderEnv004CriteriaPanel(view);
        renderCapacitySummary(p);
        renderEnv003FormulaGuide(p);
    }

    function pickExampleVmRow(planner) {
        const rows = planner?.vmResults || [];
        if (!rows.length) return null;
        return rows.find(r => r.requestRatePercent === 5 && r.timeoutSec === 3)
            || rows.find(r => r.requestRatePercent === 5)
            || rows[0];
    }

    function vmRowFor(planner, percent, timeoutSec) {
        return (planner?.vmResults || []).find(
            r => r.requestRatePercent === percent && r.timeoutSec === timeoutSec
        );
    }

    function usersAtPercent(totalUsers, percent) {
        return Math.ceil(totalUsers * (percent / 100));
    }

    /**
     * SC-002 표 — ENV-002 입력·ENV-003 산정 결과로 용량·성능 기준값 전면 연계
     */
    function buildProjectBaselineFromCapacity(serverBaseline) {
        const view = loadPersistedCapacityView();
        if (!view?.planner) return null;
        const p = view.planner;
        const req = loadPersistedRequest();
        const timeouts = (req?.responseTimeoutSeconds?.length
            ? req.responseTimeoutSeconds
            : uniqueSorted((p.vmResults || []).map(r => r.timeoutSec))
        ).slice().sort((a, b) => a - b);
        const timeoutSec = view.activeResponseTimeoutSec || timeouts[0] || 3;
        const percents = (req?.actualRequestPercents?.length
            ? req.actualRequestPercents
            : uniqueSorted((p.vmResults || []).map(r => r.requestRatePercent))
        ).slice().sort((a, b) => a - b);
        const sessions = (req?.sessionIdleMinutes?.length
            ? req.sessionIdleMinutes
            : uniqueSorted([p.primarySessionMinutes, view.activeSessionMinutes].filter(Boolean))
        ).slice().sort((a, b) => a - b);
        const sessionMin = view.activeSessionMinutes ?? p.primarySessionMinutes ?? sessions[0] ?? 60;

        const tpsAt = (pct, sec) => {
            const row = vmRowFor(p, pct, sec ?? timeoutSec);
            if (row) return row.targetTps;
            return Math.ceil(usersAtPercent(p.totalUsers, pct) / (sec ?? timeoutSec));
        };
        const usersAt = (pct, sec) => {
            const row = vmRowFor(p, pct, sec ?? timeoutSec);
            return row ? row.realRequesters : usersAtPercent(p.totalUsers, pct);
        };

        const rowPeak = pickExampleVmRow(p)
            || vmRowFor(p, percents[percents.length - 1] || 5, timeoutSec)
            || (p.vmResults || [])[0];
        const peakPct = rowPeak?.requestRatePercent ?? (percents.includes(5) ? 5 : percents[0]);
        const peakUsers = rowPeak?.realRequesters ?? usersAt(peakPct, timeoutSec);
        const peakTps = rowPeak?.targetTps ?? tpsAt(peakPct, timeoutSec);
        const designSessions = p.designSessions ?? Math.ceil(p.totalUsers * 1.3);

        const apCenterCount = p.activeActive ? 2 : 1;
        const vmPerCenter = rowPeak?.requiredVmSingleCenter ?? 1;
        const vmTotal = rowPeak?.recommendedVmActiveActive ?? vmPerCenter * apCenterCount;

        const tpsChips = percents.map(pct => {
            const t = tpsAt(pct, timeoutSec);
            const tag = pct === peakPct ? ' · 피크' : '';
            return { pct, tps: t, tag };
        });

        const baselineLinked = {
            branch: {
                value: `${p.branchCount.toLocaleString()}개`,
                hint: `ENV-002 「지점 수」 · 지점당 ${p.usersPerBranch}명`
            },
            totalUsers: {
                value: `<strong>${p.totalUsers.toLocaleString()}명</strong>`,
                hint: `ENV-002 「전체 사용자」 = ${p.branchCount.toLocaleString()} × ${p.usersPerBranch}`
            },
            actualRequest: {
                value: `<strong>${peakUsers.toLocaleString()}명</strong>`,
                hint: `ENV-003 · ${peakPct}% · Timeout ${timeoutSec}초 · TPS ${peakTps.toLocaleString()}`
            },
            sessionDesign: {
                value: `<strong>${designSessions.toLocaleString()}명</strong>`,
                hint: `ENV-002 산정 (전체 ${p.totalUsers.toLocaleString()} × 1.3)`
            },
            sessionCheck: {
                value: `<strong>${sessionMin}분</strong> Idle`,
                hint: `ENV-002 선택 ${sessions.map(m => m + '분').join(' / ')} · ENV-004 Grid 점검 기준`
            },
            tpsScenario: {
                chips: tpsChips,
                vmTps: p.vmTpsAt35 ?? p.vmCores * p.tpsPerCoreBase,
                hint: `ENV-003 · Timeout ${timeoutSec}초 · VM TPS = ${p.vmCores}코어 × ${p.tpsPerCoreBase} TPS`
            },
            apVmSpec: {
                value: `<strong>${escapeHtml(p.vmProfileId)}</strong> · ${p.vmCores}코어 / ${p.vmMemoryGb}GB`,
                hint: `ENV-002 VM 프로파일 · Core TPS 선정 ${p.tpsPerCoreBase} · TPMC/TPS ${(p.tpmcPerTps ?? 0).toLocaleString()}`
            },
            apCount: {
                value: `<strong>${apCenterCount}센터</strong> · 센터당 VM ${vmPerCenter}대 · 총 ${vmTotal}대`,
                hint: p.activeActive
                    ? `ENV-002 A-A ON · ENV-003 권장 VM ${vmTotal}대 (= ${vmPerCenter}×2)`
                    : `ENV-002 A-A OFF · 단일 센터 ${vmPerCenter}대`
            },
            targetP95: {
                value: `<strong>${timeoutSec}초</strong> <code>(${timeoutSec * 1000} ms)</code>`,
                hint: `ENV-002 Timeout ${timeouts.map(s => s + '초').join(' / ')} · 산정·Rule p95 = 최소 ${timeoutSec}초`
            }
        };

        const scenarioUsers = percents.map(pct =>
            `${usersAt(pct, timeoutSec).toLocaleString()}(${pct}%)`
        ).join(' / ');
        const scenarioTps = percents.map(pct =>
            `${tpsAt(pct, timeoutSec).toLocaleString()}(${pct}%)`
        ).join(' / ');

        const deploy = {
            ...(serverBaseline?.deploymentSummary || {}),
            '용량산정 문서': (serverBaseline?.capacityDocRef || 'ENV-002') + ` · ${view.scenarioId}`,
            '전체 사용자': baselineLinked.totalUsers.value.replace(/<[^>]+>/g, '') + ' — ' + baselineLinked.totalUsers.hint,
            '실요청 사용자': `${peakUsers.toLocaleString()}명 — ${baselineLinked.actualRequest.hint}`,
            '실요청 시나리오': scenarioUsers,
            '세션 설계': `${designSessions.toLocaleString()}명 — ${baselineLinked.sessionDesign.hint}`,
            '세션 유지': baselineLinked.sessionCheck.value + ' — ' + baselineLinked.sessionCheck.hint,
            'TPS 시나리오': scenarioTps + ` · VM ${(p.vmTpsAt35 ?? 0).toLocaleString()}`,
            'AP 구성': baselineLinked.apCount.hint,
            'VM 산정': rowPeak
                ? `TPS ${peakTps.toLocaleString()} → VM ${vmPerCenter}대/센터 · 총 ${vmTotal}대`
                : '—'
        };
        const jvm = resolveJvmSizing(view);
        if (jvm) {
            deploy['JVM Heap'] = `일반 ${jvm.heapGeneralMinGb}~${jvm.heapGeneralMaxGb}GB / SV ≤${jvm.heapSingleViewMaxGb}GB (${jvm.vmProfileId})`;
        }

        const base = serverBaseline || {};
        const tpsByPct = {};
        percents.forEach(pct => { tpsByPct[pct] = tpsAt(pct, timeoutSec); });

        return {
            ...base,
            projectId: base.projectId || 'nsight-message-mgmt',
            projectName: base.projectName || 'NSIGHT',
            envCode: base.envCode || 'local',
            hardwareProfile: p.vmProfileId || base.hardwareProfile,
            capacityDocRef: base.capacityDocRef,
            centerType: base.centerType,
            branchCount: p.branchCount,
            usersPerBranch: p.usersPerBranch,
            totalUsers: p.totalUsers,
            actualRequestUsers: peakUsers,
            actualRequestPeakPercent: peakPct,
            sessionDesignCount: designSessions,
            sessionBufferedMin: Math.floor(designSessions * 1.19),
            sessionBufferedMax: Math.ceil(designSessions * 1.31),
            baseTps: tpsByPct[3] ?? tpsAt(3, timeoutSec),
            peakTps: tpsByPct[5] ?? peakTps,
            highPeakTps: tpsByPct[10] ?? tpsAt(10, timeoutSec),
            stressTps: tpsByPct[15] ?? tpsAt(15, timeoutSec),
            vmMaxTps: p.vmTpsAt35 ?? base.vmMaxTps,
            peakConcurrentUsers: peakUsers,
            apCount: apCenterCount,
            apVmSpec: `${p.vmProfileId} · ${p.vmCores}코어 / ${p.vmMemoryGb}GB · Core ${p.tpsPerCoreBase} TPS · VM TPS ${p.vmTpsAt35}`,
            targetP95Ms: timeoutSec * 1000,
            deploymentSummary: deploy,
            _fromCapacityDesign: true,
            _scenarioId: view.scenarioId,
            _primaryTimeoutSec: timeoutSec,
            _sessionIdleMinutes: sessions,
            _linkedPercents: percents,
            _linkedTimeouts: timeouts,
            baselineLinked
        };
    }

    /** §4 Tomcat·Hikari 기준표 (VmProfile · TomcatHikariSizingGuide 와 동일) */
    const TOMCAT_HIKARI_SPEC = {
        '8CORE-32GB': {
            guideTps: 250, tomcat: '400~500', tomcatConservative: 400,
            minSpare: '100~150', accept: '200~400',
            maxConn: '8000~12000', hikari: '80~100', hikariSv: '70~80', hikariRec: 100,
            caution: 'Scale-Out · DB Session 검증 필요'
        },
        '8CORE-64GB': {
            guideTps: 250, tomcat: '400~500', tomcatConservative: 400,
            minSpare: '100~150', accept: '200~400',
            maxConn: '8000~12000', hikari: '80~100', hikariSv: '70~80', hikariRec: 100,
            caution: 'DB Session 검증 필요'
        },
        '16CORE-64GB': {
            guideTps: 500, tomcat: '800~1000', tomcatConservative: 800,
            minSpare: '150~200', accept: '300~500',
            maxConn: '12000~16000', hikari: '80~100', hikariSv: '70~80', hikariRec: 100,
            caution: 'DB Session 검증 필요'
        },
        '16CORE-128GB': {
            guideTps: 500, tomcat: '800~1000', tomcatConservative: 800,
            minSpare: '150~200', accept: '300~500',
            maxConn: '12000~16000', hikari: '80~100', hikariSv: '70~80', hikariRec: 100,
            caution: 'DB Session 검증 필요'
        },
        '32CORE-256GB': {
            guideTps: 1000, tomcat: '1200~1500', tomcatConservative: 1200,
            minSpare: '200~300', accept: '500~800',
            maxConn: '20000~30000', hikari: '120~150', hikariSv: '150~180', hikariRec: 150,
            connTimeout: '8초', keepAlive: '60초', maxKeepAliveReq: 100, xss: '512KB',
            busyFormula: 'Busy≈1000 TPS×(1.0~1.2s)×1.2 → 1200~1440',
            perfTest: '성능시험: maxThreads 1,200 vs 1,500 비교',
            caution: 'DB Session 총량 검증 없이는 적용 금지'
        }
    };
    /** ENV-002 선택 VM 프로파일 ID (planner 우선 · session 요청 보강) */
    function enrichPlannerFromEnv002(planner) {
        if (!planner) return planner;
        const req = loadPersistedRequest();
        const p = { ...planner };
        if (req?.customVm && req.customCore > 0) {
            const mem = req.customMemoryGb > 0 ? req.customMemoryGb : req.customCore * GB_PER_CORE_IAAS;
            p.vmProfileId = `${req.customCore}CORE-${mem}GB`;
            p.vmCores = req.customCore;
            p.vmMemoryGb = mem;
        } else if (req?.vmProfileId) {
            p.vmProfileId = req.vmProfileId;
        }
        return p;
    }

    /** §4 Tomcat/Hikari — ENV-002 vmProfileId 를 절대 우선 (nearest 로 프로파일 ID 덮어쓰지 않음) */
    function tomcatHikariSpecForPlanner(p) {
        const selectedId = String(p?.vmProfileId || '').trim();
        if (selectedId && TOMCAT_HIKARI_SPEC[selectedId]) {
            return { id: selectedId, ...TOMCAT_HIKARI_SPEC[selectedId] };
        }
        const nearest = nearestTomcatHikariSpec(p?.vmCores, p?.vmMemoryGb, null);
        if (selectedId) {
            return { ...nearest, id: selectedId };
        }
        return nearest;
    }

    function nearestTomcatHikariSpec(cores, memoryGb, profileId) {
        const pid = String(profileId || '').trim();
        if (pid && TOMCAT_HIKARI_SPEC[pid]) {
            return { id: pid, ...TOMCAT_HIKARI_SPEC[pid] };
        }
        const c = Math.max(1, cores);
        const ram = memoryGb > 0 ? memoryGb : c * GB_PER_CORE_IAAS;
        let best = { id: '8CORE-32GB', diff: Infinity, ...TOMCAT_HIKARI_SPEC['8CORE-32GB'] };
        Object.entries(TOMCAT_HIKARI_SPEC).forEach(([id, spec]) => {
            const prof = VM_PROFILES.find(v => v.id === id);
            if (!prof) return;
            const diff = Math.abs(prof.cores - c) * 1000 + Math.abs(prof.memoryGb - ram);
            if (diff < best.diff) best = { id, diff, ...spec };
        });
        return best;
    }

    const DB_POOL_THREAD_RATIO = 0.10;

    function tomcatMaxFromSpec(spec) {
        const part = String(spec.tomcat).includes('~') ? spec.tomcat.split('~')[1] : spec.tomcat;
        return parseInt(part, 10) || 500;
    }

    function buildDbPoolDerivationLines(spec, recommended) {
        const tomcatMax = tomcatMaxFromSpec(spec);
        const [minH, maxH] = String(spec.hikari).split('~').map(s => parseInt(s, 10));
        const candidate = Math.floor(tomcatMax * DB_POOL_THREAD_RATIO);
        const clamped = Math.min(maxH, Math.max(minH, candidate));
        return [
            `① 프로파일 = ${spec.id} · §4 Hikari 일반 ${spec.hikari}`,
            `② Tomcat maxThreads 권장 상한 = ${tomcatMax} → Pool ≤ maxThreads (업무 Thread 수 초과 금지)`,
            `③ 참고 = floor(${tomcatMax} × 10%) = ${candidate}`,
            `④ §4 범위 적용 = clamp(${candidate}, ${minH}, ${maxH}) = ${clamped}`,
            `⑤ VM당 DB Pool 권장 = ${recommended} (§4 범위 상한 · 성능시험·DB Session 검증 후 조정)`,
            `⑥ SV AP = ${spec.hikariSv} · 합계 = 권장 VM × ${recommended} ≤ DB Session 한도`
        ];
    }

    function describeDbPoolFormula(cores, memoryGb, profileId, planner) {
        const spec = planner ? tomcatHikariSpecForPlanner(planner) : nearestTomcatHikariSpec(cores, memoryGb, profileId);
        const recommended = planner?.hikariPoolPerVm > 0
            ? planner.hikariPoolPerVm
            : (planner?.vmResults?.[0]?.dbPoolPerVm ?? spec.hikariRec);
        const derivationLines = planner?.hikariPoolDerivationFormula
            ? planner.hikariPoolDerivationFormula.split('\n').map(s => s.trim()).filter(Boolean)
            : buildDbPoolDerivationLines(spec, recommended);
        const formula = planner?.hikariPoolFormula
            || `VM당 Pool = <strong>${recommended}</strong> · Hikari 일반 <strong>${spec.hikari}</strong> (${spec.id}) · 합계 = 권장VM × ${recommended}`;
        return {
            recommended,
            range: planner?.hikariPoolRangeLabel || spec.hikari,
            svRange: planner?.hikariSingleViewRange || spec.hikariSv,
            formula,
            derivationLines,
            note: `${spec.caution} · Pool ≤ Tomcat maxThreads ${tomcatMaxFromSpec(spec)}`
        };
    }

    function renderEnv003DerivationHtml(derivationLines, cssMod) {
        if (!derivationLines?.length) return '';
        const mod = cssMod ? ` env-env003-guide__derivation--${cssMod}` : '';
        const items = derivationLines.map(line => `<li>${escapeHtml(line)}</li>`).join('');
        return `<ol class="env-env003-guide__derivation${mod}">${items}</ol>`;
    }

    function renderDbPoolDerivationHtml(derivationLines) {
        return renderEnv003DerivationHtml(derivationLines, 'pool');
    }

    function splitDerivationLines(text) {
        return (text || '').split('\n').map(s => s.trim()).filter(Boolean);
    }

    function buildJvmHeapDerivationLines(cores, memoryGb, profileId, planner) {
        if (planner?.jvmHeapDerivationFormula) {
            return splitDerivationLines(planner.jvmHeapDerivationFormula);
        }
        const c = Math.max(1, cores);
        const ram = memoryGb > 0 ? memoryGb : c * GB_PER_CORE_IAAS;
        const heap = computeHeapGeneral(c, ram);
        const sv = computeHeapSvMaxGb(c, ram);
        const id = profileId || `${c}CORE-${ram}GB`;
        if (c <= 8 && ram <= 40) {
            return [
                `① VM = ${id} · ${c} vCPU / ${ram} GB RAM`,
                '② RAM 배분 = 8CORE/32GB 표준 · Scale-Out',
                '③ 일반 Heap = §4 앵커 12~14 GB (고정)',
                `④ SV Xmx ≤ ${sv} GB`,
                `⑤ VM당 JVM Heap = ${heap.minGb}~${heap.maxGb} GB`
            ];
        }
        if (isEightGbPerCore(c, ram)) {
            const rawXms = Math.max(12, Math.round(c * 1.5));
            const rawXmx = Math.max(rawXms + 2, Math.round(c * 1.75));
            const step32 = c >= 32
                ? ` → 32Core 문서 적용 후 ${heap.minGb}~${heap.maxGb} GB`
                : '';
            return [
                `① VM = ${id} · ${c} vCPU / ${ram} GB RAM`,
                `② RAM 배분 = 코어당 ${GB_PER_CORE_IAAS}GB RAM`,
                `③ Xms = max(12, round(${c}×1.5)) = ${rawXms} GB${step32}`,
                `④ Xmx = max(Xms+2, round(${c}×1.75)) = ${rawXmx} GB`,
                `⑤ VM당 JVM Heap = ${heap.minGb}~${heap.maxGb} GB`,
                `⑥ SV Xmx ≤ ${sv} GB`
            ];
        }
        return [
            `① VM = ${id} · ${c} vCPU / ${ram} GB RAM`,
            `② RAM ${ram}GB — 문서 앵커 보간`,
            `③ VM당 JVM Heap = ${heap.minGb}~${heap.maxGb} GB`,
            `④ SV Xmx ≤ ${sv} GB`
        ];
    }

    function buildWasThreadsDerivationLines(planner, profileId, cores, memoryGb) {
        if (planner?.wasThreadsDerivationFormula) {
            return splitDerivationLines(planner.wasThreadsDerivationFormula);
        }
        const spec = planner ? tomcatHikariSpecForPlanner(planner) : nearestTomcatHikariSpec(cores, memoryGb, profileId);
        const tps = spec.guideTps || 500;
        const busyLow = Math.ceil(tps * 1.0 * 1.2);
        const busyHigh = Math.ceil(tps * 1.2 * 1.2);
        const lines = [
            `① 프로파일 = ${spec.id} · §4 기준 VM TPS = ${tps}`,
            '② Busy Thread = ceil(TPS × 평균응답(1.0~1.2s) × 운영여유율 1.2)',
            `③ Busy_low = ceil(${tps} × 1.0 × 1.2) = ${busyLow}`,
            `④ Busy_high = ceil(${tps} × 1.2 × 1.2) = ${busyHigh}`,
            `⑤ maxThreads = §4 ${spec.tomcat} (Busy ${busyLow}~${busyHigh} 반영)`,
            `⑥ minSpare ${spec.minSpare} · accept ${spec.accept}`
        ];
        if (spec.id === '32CORE-256GB') {
            lines.push('⑦ 성능시험: maxThreads 1,200 vs 1,500 비교');
        }
        return lines;
    }

    function poolPerVmFromPlanner(p) {
        const ex = (p.vmResults || [])[0];
        return (ex?.dbPoolPerVm ?? p.hikariPoolPerVm ?? tomcatHikariSpecForPlanner(p).hikariRec).toLocaleString();
    }

    /** ENV-003 JVM Heap 산식 문구 (JvmSizingGuide와 동일) */
    function describeJvmHeapFormula(cores, memoryGb) {
        const c = Math.max(1, cores);
        const ram = memoryGb > 0 ? memoryGb : c * GB_PER_CORE_IAAS;
        const heap = computeHeapGeneral(c, ram);
        const sv = computeHeapSvMaxGb(c, ram);
        let formula;
        if (c <= 8 && ram <= 40) {
            formula = '8CORE/32GB 표준 → 일반 Heap <strong>12~14 GB</strong> (고정)';
        } else if (isEightGbPerCore(c, ram)) {
            const rawMin = Math.max(12, Math.round(c * 1.5));
            const rawMax = Math.max(rawMin + 2, Math.round(c * 1.75));
            if (c >= 32) {
                formula = `Xms = max(32, round(${c}×1.5)) = <strong>${heap.minGb} GB</strong> · `
                    + `Xmx = min(48, max(Xms+2, round(${c}×1.75))) = <strong>${heap.maxGb} GB</strong>`;
            } else {
                formula = `Xms = max(12, round(${c}×1.5)) = <strong>${heap.minGb} GB</strong> · `
                    + `Xmx = max(Xms+2, round(${c}×1.75)) = <strong>${heap.maxGb} GB</strong>`;
            }
        } else {
            formula = `RAM ${ram}GB 앵커 보간 → <strong>${heap.minGb}~${heap.maxGb} GB</strong>`;
        }
        return {
            formula,
            range: `${heap.minGb}~${heap.maxGb} GB`,
            sv: `≤${sv} GB`,
            note: jvmSizingNote(c, ram),
            derivationLines: buildJvmHeapDerivationLines(c, ram, null, null)
        };
    }

    /** ENV-003 표 — 목표 TPS·TPMC 등 산식 설명 */
    function renderEnv003FormulaGuide(plannerIn) {
        const el = document.getElementById('env003FormulaGuide');
        if (!el || !plannerIn) return;
        const p = enrichPlannerFromEnv002(plannerIn);
        const tpmcPerReq = p.tpmcPerTps ?? REF_TPMC;
        const ex = pickExampleVmRow(p);
        const totalUsers = p.totalUsers ?? 0;
        const usersFmt = totalUsers.toLocaleString();
        const cores = p.vmCores ?? 0;
        const ramGb = p.vmMemoryGb ?? 0;
        const vmTpsBase = p.vmTpsAt35 ?? cores * (p.tpsPerCoreBase ?? REF_TPS_BASE);
        const vmTpsMax = p.vmTpsAt40 ?? cores * (p.tpsPerCoreMax ?? REF_TPS_MAX);
        const vmProfileId = p.vmProfileId || `${cores}CORE-${ramGb}GB`;
        const aaOn = p.activeActive === true;
        const poolDesc = describeDbPoolFormula(cores, ramGb, vmProfileId, p);
        const poolPerVm = ex?.dbPoolPerVm ?? poolDesc.recommended;
        const jvmDesc = describeJvmHeapFormula(cores, ramGb);
        jvmDesc.derivationLines = buildJvmHeapDerivationLines(cores, ramGb, vmProfileId, p);
        const wasLabel = ex?.wasThreadsPerVm || p.tomcatMaxThreadsRange || '—';
        const wasDerivationLines = buildWasThreadsDerivationLines(p, vmProfileId, cores, ramGb);
        const heapLabel = ex?.jvmHeapPerVm || jvmDesc.range;
        const heapSvLabel = ex?.jvmHeapSvPerVm || jvmDesc.sv;

        const coreTpsFormula = p.coreTpsLinkedToTpmc !== false
            ? `Core TPS = floor(35×${REF_TPMC.toLocaleString()} ÷ ${tpmcPerReq.toLocaleString()}) = <strong>${p.tpsPerCoreBase}</strong> (보수 ${p.tpsPerCoreMin} ~ 여유 ${p.tpsPerCoreMax})`
            : `Core TPS 수동: ${p.tpsPerCoreMin} / <strong>${p.tpsPerCoreBase}</strong> / ${p.tpsPerCoreMax}`;

        let exampleBlock = '';
        if (ex) {
            const pct = ex.requestRatePercent;
            const sec = ex.timeoutSec;
            const real = ex.realRequesters;
            const tps = ex.targetTps;
            const tpmc = ex.requiredTpmc;
            const vmNeed = ex.requiredVmSingleCenter;
            const vmAa = ex.recommendedVmActiveActive;
            const vmTps = ex.vmTpsAtBase;
            const ceilTps = Math.ceil(real / sec);
            const ceilVm = Math.ceil(tps / Math.max(1, vmTps));
            const poolTotal = ex.dbPoolTotal ?? vmAa * poolPerVm;
            exampleBlock = `
                <section class="env-env003-guide__example">
                    <h4 class="env-env003-guide__example-title">계산 예시 (표의 ${pct}% · ${sec}초 행)</h4>
                    <ol class="env-env003-guide__example-steps">
                        <li><strong>실요청자</strong> = ceil(${usersFmt} × ${pct}%) = <strong>${real.toLocaleString()}명</strong></li>
                        <li><strong>목표 TPS</strong> = ceil(${real.toLocaleString()} ÷ ${sec}) = <strong>${ceilTps.toLocaleString()}건/초</strong>${ceilTps !== tps ? ` (표시 ${tps.toLocaleString()})` : ''}</li>
                        <li><strong>TPMC</strong> = ${tps.toLocaleString()} × ${tpmcPerReq.toLocaleString()} = <strong>${tpmc.toLocaleString()}</strong></li>
                        <li><strong>VM TPS</strong> = ${cores} × ${p.tpsPerCoreBase} = <strong>${vmTps.toLocaleString()}건/초</strong></li>
                        <li><strong>필요 VM</strong> = ceil(${tps.toLocaleString()} ÷ ${vmTps.toLocaleString()}) = <strong>${ceilVm}대</strong>${vmNeed !== ceilVm ? ` (표시 ${vmNeed}대)` : ''}</li>
                        ${aaOn ? `<li><strong>A-A 권장</strong> = ${vmNeed} × 2 = <strong>총 ${vmAa}대</strong></li>` : `<li><strong>권장 VM</strong> = <strong>${vmAa}대</strong> (A-A 미적용)</li>`}
                        <li><strong>VM당 JVM Heap</strong> = ${escapeHtml(heapLabel)}${heapSvLabel ? ` · SV ${escapeHtml(heapSvLabel)}` : ''}
                            <span class="env-env003-guide__example-sub">(${escapeHtml(jvmDesc.derivationLines?.[4] || jvmDesc.derivationLines?.[jvmDesc.derivationLines.length - 1] || '')})</span></li>
                        <li><strong>VM당 WAS Threads</strong> = <strong>${escapeHtml(wasLabel)}</strong>
                            <span class="env-env003-guide__example-sub">(${escapeHtml(wasDerivationLines?.[4] || 'Tomcat maxThreads')})</span></li>
                        <li><strong>VM당 DB Pool</strong> = <strong>${poolPerVm.toLocaleString()}</strong>
                            (${escapeHtml(poolDesc.derivationLines?.[4] || '§4 권장')})
                            · <strong>합계</strong> = ${vmAa} × ${poolPerVm} = <strong>${poolTotal.toLocaleString()}</strong></li>
                    </ol>
                </section>`;
        }

        const vmTpsCalc = `${cores} × ${p.tpsPerCoreBase} = <strong>${vmTpsBase.toLocaleString()}</strong> 건/초`;
        const aaFormula = aaOn
            ? `A-A 권장 = 필요 VM × 2 (양센터)`
            : 'A-A 미적용 → 권장 VM = 필요 VM';

        el.className = 'env-env003-guide';
        el.innerHTML = `
            <h3 class="env-env003-guide__title">표 항목 · 산식 설명</h3>
            <p class="env-env003-guide__lead">
                한 행은 「동시 요청률(%) + 응답 Timeout(초)」 조합입니다.
                현재 VM: <strong>${escapeHtml(vmProfileId)}</strong> (${cores}코어 / ${ramGb}GB) · ${coreTpsFormula}
            </p>
            <div class="env-env003-guide__grid">
                <article class="env-env003-guide__card">
                    <h4>요청률 / Timeout</h4>
                    <p class="env-env003-guide__formula">행 키 = ENV-002 선택한 (요청률%, 응답초) 조합</p>
                    <p class="env-env003-guide__mean">동일 전체 사용자·VM 조건에서 <strong>부하 시나리오</strong>만 바꿉니다.</p>
                </article>
                <article class="env-env003-guide__card">
                    <h4>실요청자</h4>
                    <p class="env-env003-guide__formula">ceil(전체 사용자 × 요청률%)</p>
                    <p class="env-env003-guide__mean">
                        예: ceil(${usersFmt} × 5%) = <strong>${ex ? ex.realRequesters.toLocaleString() : '—'}명</strong>.
                        피크 때 <strong>동시 요청 중인 사용자 수</strong> (세션 수 ≠ 실요청자).
                    </p>
                </article>
                <article class="env-env003-guide__card env-env003-guide__card--highlight">
                    <h4>목표 TPS</h4>
                    <p class="env-env003-guide__formula">ceil(실요청자 ÷ Timeout초)</p>
                    <p class="env-env003-guide__mean">
                        <strong>초당 처리 요청 건수</strong>.
                        ${ex ? `${ex.realRequesters.toLocaleString()} ÷ ${ex.timeoutSec} = ${ex.targetTps.toLocaleString()}건/초` : '실요청 ÷ 응답초'}
                    </p>
                </article>
                <article class="env-env003-guide__card env-env003-guide__card--highlight">
                    <h4>TPMC</h4>
                    <p class="env-env003-guide__formula">목표 TPS × ${tpmcPerReq.toLocaleString()} (TPMC/TPS)</p>
                    <p class="env-env003-guide__mean">
                        전사 초당 부하.
                        ${ex ? `${ex.targetTps.toLocaleString()} × ${tpmcPerReq.toLocaleString()} = <strong>${ex.requiredTpmc.toLocaleString()}</strong>` : '목표 TPS × ENV-002 TPMC/TPS'}
                    </p>
                </article>
                <article class="env-env003-guide__card">
                    <h4>VM 사양</h4>
                    <p class="env-env003-guide__formula">${escapeHtml(vmProfileId)} → ${cores} vCPU / ${ramGb} GB RAM</p>
                    <p class="env-env003-guide__mean">ENV-002 VM 프로파일. CUSTOM이면 입력 코어·RAM 기준.</p>
                </article>
                <article class="env-env003-guide__card">
                    <h4>VM TPS (기준)</h4>
                    <p class="env-env003-guide__formula">VM TPS = vCPU × Core당 TPS(선정) = ${vmTpsCalc}</p>
                    <p class="env-env003-guide__mean">
                        보수 ${(p.vmTpsAt30 ?? cores * p.tpsPerCoreMin).toLocaleString()} ·
                        여유 ${vmTpsMax.toLocaleString()} (Core ${p.tpsPerCoreMax} TPS) — VM 1대 처리량 상한 참고.
                    </p>
                </article>
                <article class="env-env003-guide__card">
                    <h4>필요 VM</h4>
                    <p class="env-env003-guide__formula">ceil(목표 TPS ÷ VM TPS)</p>
                    <p class="env-env003-guide__mean">
                        ${ex ? `ceil(${ex.targetTps.toLocaleString()} ÷ ${ex.vmTpsAtBase.toLocaleString()}) = <strong>${ex.requiredVmSingleCenter}대</strong> (센터 1곳)` : '목표 TPS를 VM 1대 TPS로 나눔 (올림)'}
                    </p>
                </article>
                <article class="env-env003-guide__card">
                    <h4>A-A 권장</h4>
                    <p class="env-env003-guide__formula">${aaOn ? '권장 VM = ceil(목표 TPS ÷ VM TPS) × 2' : '권장 VM = 필요 VM'}</p>
                    <p class="env-env003-guide__mean">
                        ${aaFormula}.
                        ${ex && aaOn ? `${ex.requiredVmSingleCenter} × 2 = <strong>총 ${ex.recommendedVmActiveActive}대</strong>` : ''}
                    </p>
                </article>
                <article class="env-env003-guide__card env-env003-guide__card--wide">
                    <h4>VM당 JVM Heap</h4>
                    <p class="env-env003-guide__formula">${jvmDesc.formula}</p>
                    ${renderEnv003DerivationHtml(jvmDesc.derivationLines, 'jvm')}
                    <p class="env-env003-guide__mean">
                        SV AP: <strong>${heapSvLabel}</strong> · ${escapeHtml(jvmDesc.note)}.
                        표 값: <strong>${escapeHtml(heapLabel)}</strong>
                    </p>
                </article>
                <article class="env-env003-guide__card env-env003-guide__card--wide">
                    <h4>VM당 WAS Threads</h4>
                    <p class="env-env003-guide__formula">maxThreads <strong>${escapeHtml(wasLabel)}</strong>${p.tomcatMinSpareRange ? ` · minSpare ${escapeHtml(p.tomcatMinSpareRange)}` : ''}</p>
                    ${renderEnv003DerivationHtml(wasDerivationLines, 'was')}
                    <p class="env-env003-guide__mean">
                        <code>server.tomcat.threads.max</code>
                        ${p.tomcatAcceptRange ? ` · accept ${escapeHtml(p.tomcatAcceptRange)}` : ''}
                        ${p.tomcatMaxConnectionsRange ? ` · maxConnections ${escapeHtml(p.tomcatMaxConnectionsRange)}` : ''}
                        · Pool ≤ maxThreads · 표 값: <strong>${escapeHtml(wasLabel)}</strong>
                    </p>
                </article>
                <article class="env-env003-guide__card env-env003-guide__card--wide">
                    <h4>VM당 DB Pool</h4>
                    <p class="env-env003-guide__formula">${poolDesc.formula}</p>
                    ${renderDbPoolDerivationHtml(poolDesc.derivationLines)}
                    <p class="env-env003-guide__mean">
                        <code>spring.datasource.hikari.maximum-pool-size</code> · SV <strong>${escapeHtml(poolDesc.svRange || p.hikariSingleViewRange || '—')}</strong>.
                        ${escapeHtml(poolDesc.note)}
                    </p>
                </article>
                <article class="env-env003-guide__card">
                    <h4>DB Pool 합계</h4>
                    <p class="env-env003-guide__formula">합계 = 권장 VM × VM당 Pool = 권장 VM × ${poolPerVm.toLocaleString()}</p>
                    <p class="env-env003-guide__mean">
                        ${ex ? `${ex.recommendedVmActiveActive} × ${poolPerVm} = <strong>${(ex.dbPoolTotal ?? 0).toLocaleString()}</strong>` : 'A-A 권장 대수 × VM당 Pool'}
                        · (VM당 Pool × 대수) ≤ DB Session 한도 ${p.dbSessionLimit ? p.dbSessionLimit.toLocaleString() : '500'} 초과 시 경고/위험.
                    </p>
                </article>
                <article class="env-env003-guide__card">
                    <h4>판정</h4>
                    <p class="env-env003-guide__formula">
                        위험: 목표 TPS &gt; ${vmTpsMax.toLocaleString()} (VM×${p.tpsPerCoreMax}) ·
                        또는 DB Pool 합계 &gt; 한도
                    </p>
                    <p class="env-env003-guide__mean">
                        경고: 목표 TPS &gt; ${Math.floor(vmTpsBase * 0.8).toLocaleString()} (VM TPS×80%) ·
                        A-A 시 센터당 VM 2대 미만 검토. 그 외 정상.
                    </p>
                </article>
            </div>
            ${exampleBlock}`;
    }

    function renderVmResults(planner) {
        const body = document.getElementById('capVmResultBody');
        if (!body) return;
        const rows = planner.vmResults || [];
        if (!rows.length) {
            body.innerHTML = '<tr><td colspan="13" class="env-empty-cell">결과 없음</td></tr>';
            return;
        }
        body.innerHTML = rows.map(r => {
            const heap = escapeHtml(r.jvmHeapPerVm || '—');
            const heapSv = r.jvmHeapSvPerVm ? escapeHtml(r.jvmHeapSvPerVm) : '';
            const was = escapeHtml(r.wasThreadsPerVm || '—');
            const poolVm = r.dbPoolPerVm != null ? r.dbPoolPerVm.toLocaleString() : '—';
            return `
            <tr class="${rowStatusClass(r.status)}">
                <td>${r.requestRatePercent}% / ${r.timeoutSec}초</td>
                <td><strong>${r.realRequesters.toLocaleString()}</strong></td>
                <td>${r.targetTps.toLocaleString()}</td>
                <td>${r.requiredTpmc.toLocaleString()}</td>
                <td>${escapeHtml(r.vmProfileLabel)}</td>
                <td>${r.vmTpsAtBase.toLocaleString()}</td>
                <td>${r.requiredVmSingleCenter}대</td>
                <td>총 ${r.recommendedVmActiveActive}대</td>
                <td class="env-vm-per-spec"><strong>${heap}</strong>${heapSv ? `<br/><span class="env-vm-per-spec__sub">SV ${heapSv}</span>` : ''}</td>
                <td><strong>${was}</strong></td>
                <td><strong>${poolVm}</strong></td>
                <td>${(r.dbPoolTotal ?? 0).toLocaleString()}</td>
                <td>${statusPill(r.status, r.status === 'NORMAL' ? '정상' : r.status === 'WARN' ? '경고' : '위험')}
                    <small>${escapeHtml(r.statusReason)}</small></td>
            </tr>`;
        }).join('');
    }

    function layerSettingToRow(s) {
        return {
            settingLabel: s.settingLabel,
            propertyKey: s.propertyKey,
            recommendedValue: s.recommendedValue,
            currentValue: s.actualValue != null ? s.actualValue : s.currentValue,
            status: s.status,
            statusLabel: s.statusLabel,
            reason: s.reason,
            configLocation: s.configLocation,
            settingExample: s.settingExample,
            actionGuide: s.actionGuide
        };
    }

    function renderLayerSettingsTableHtml(rows, opts = {}) {
        if (!rows?.length) {
            return '<p class="env-empty-cell">해당 계층 설정 항목 없음</p>';
        }
        const showLayer = opts.showLayer === true;
        const layerHead = showLayer ? '<th>계층</th>' : '';
        const body = rows.map(r => `
            <tr class="${rowStatusClass(r.status)}">
                ${showLayer ? `<td><strong>${escapeHtml(r.layer)}</strong></td>` : ''}
                <td>${escapeHtml(r.settingLabel)}</td>
                <td>${escapeHtml(r.recommendedValue)}</td>
                <td><code>${escapeHtml(r.currentValue)}</code></td>
                <td>${statusPill(r.status, r.statusLabel)}</td>
                <td class="env-cell-reason">${escapeHtml(r.reason)}</td>
                <td>${escapeHtml(r.configLocation)}</td>
                <td><code class="env-example">${escapeHtml(r.settingExample)}</code></td>
                <td>${escapeHtml(r.actionGuide)}</td>
            </tr>`).join('');
        const colSpan = showLayer ? 9 : 8;
        return `
            <table class="dump-report__table dump-report__table--data dump-report__table--layer-grid">
                <thead><tr>
                    ${layerHead}
                    <th>설정 항목</th><th>권장값</th><th>현재값</th><th>판정</th>
                    <th>판정 사유</th><th>설정 위치</th><th>설정 예시</th><th>조치 가이드</th>
                </tr></thead>
                <tbody>${body || `<tr><td colspan="${colSpan}" class="env-empty-cell">—</td></tr>`}</tbody>
            </table>`;
    }

    function renderLayerGrid(grid) {
        const body = document.getElementById('layerGridBody');
        if (!body) return;
        if (!grid?.length) {
            body.innerHTML = '<tr><td colspan="9" class="env-empty-cell">—</td></tr>';
            return;
        }
        body.innerHTML = grid.map(g => `
            <tr class="${rowStatusClass(g.status)}">
                <td><strong>${escapeHtml(g.layer)}</strong></td>
                <td>${escapeHtml(g.settingLabel)}</td>
                <td>${escapeHtml(g.recommendedValue)}</td>
                <td><code>${escapeHtml(g.currentValue)}</code></td>
                <td>${statusPill(g.status, g.statusLabel)}</td>
                <td class="env-cell-reason">${escapeHtml(g.reason)}</td>
                <td>${escapeHtml(g.configLocation)}</td>
                <td><code class="env-example">${escapeHtml(g.settingExample)}</code></td>
                <td>${escapeHtml(g.actionGuide)}</td>
            </tr>
        `).join('');
    }

    function activateEnv004Tab(tabId) {
        const nav = document.getElementById('env004TabNav');
        const panels = document.getElementById('env004TabPanels');
        if (!nav || !panels || !tabId) return;
        nav.querySelectorAll('[data-env004-tab]').forEach(btn => {
            const on = btn.dataset.env004Tab === tabId;
            btn.classList.toggle('is-active', on);
            btn.setAttribute('aria-selected', on ? 'true' : 'false');
        });
        panels.querySelectorAll('[data-env004-panel]').forEach(panel => {
            const on = panel.dataset.env004Panel === tabId;
            panel.classList.toggle('is-active', on);
            panel.hidden = !on;
        });
        try {
            sessionStorage.setItem(STORAGE_ENV004_TAB, tabId);
        } catch (e) { /* ignore */ }
    }

    function wireEnv004LayerTabs() {
        const root = document.getElementById('env004LayerTabs');
        const nav = document.getElementById('env004TabNav');
        if (!root || !nav || root.dataset.wired === '1') return;
        root.dataset.wired = '1';
        nav.addEventListener('click', e => {
            const btn = e.target.closest('[data-env004-tab]');
            if (!btn || btn.disabled) return;
            activateEnv004Tab(btn.dataset.env004Tab);
        });
    }

    /** ENV-004 — 계층별 탭 (UI → … → MyBatis) */
    function renderEnv004LayerTabs(view) {
        const nav = document.getElementById('env004TabNav');
        const panels = document.getElementById('env004TabPanels');
        if (!nav || !panels) return;

        const grid = view.layerGrid || [];
        const layers = (view.stackLayers?.length ? view.stackLayers : rebuildStackLayersFromGrid(grid))
            .slice()
            .sort((a, b) => (a.order || 0) - (b.order || 0));
        const jvm = view.jvmSizing;

        if (!grid.length || !layers.length) {
            nav.innerHTML = '';
            panels.innerHTML = '<p class="env-empty-cell">산정 실행 후 계층별 설정이 표시됩니다.</p>';
            return;
        }

        let activeIdx = 0;
        try {
            const saved = sessionStorage.getItem(STORAGE_ENV004_TAB);
            if (saved) {
                const idx = layers.findIndex(l => l.layerId === saved);
                if (idx >= 0) activeIdx = idx;
            }
        } catch (e) { /* ignore */ }

        nav.innerHTML = layers.map((layer, i) => {
            const crit = (layer.settings || []).some(s => s.status === 'CRITICAL');
            const warn = !layer.layerValid && !crit;
            const btnClass = [
                'env-env004-tabs__btn',
                i === activeIdx ? 'is-active' : '',
                crit ? 'env-env004-tabs__btn--crit' : warn ? 'env-env004-tabs__btn--warn' : ''
            ].filter(Boolean).join(' ');
            return `
                <button type="button" role="tab" class="${btnClass}"
                    id="env004-tab-${layer.layerId}"
                    data-env004-tab="${layer.layerId}"
                    aria-selected="${i === activeIdx}"
                    aria-controls="env004-panel-${layer.layerId}">
                    <span class="env-env004-tabs__label">${escapeHtml(layer.layerName)}</span>
                    <span class="env-env004-tabs__count">${(layer.settings || []).length}</span>
                </button>`;
        }).join('');

        panels.innerHTML = layers.map((layer, i) => {
            const rows = (layer.settings || []).map(layerSettingToRow);
            const jvmBlock = layer.layerName === 'JVM' && jvm ? renderJvmSizingHtml(jvm) : '';
            const active = i === activeIdx;
            return `
                <div class="env-env004-tabs__panel${active ? ' is-active' : ''}"
                    role="tabpanel"
                    id="env004-panel-${layer.layerId}"
                    data-env004-panel="${layer.layerId}"
                    aria-labelledby="env004-tab-${layer.layerId}"
                    ${active ? '' : ' hidden'}>
                    <header class="env-env004-tabs__panel-head">
                        <span class="env-env004-tabs__order">${layer.order}</span>
                        <div class="env-env004-tabs__titles">
                            <h3>${escapeHtml(layer.layerName)}</h3>
                            <p>${escapeHtml(layer.description || '')}</p>
                        </div>
                        ${statusPill(layer.layerValid ? 'NORMAL' : 'WARN', layer.layerValid ? '정상' : '점검')}
                    </header>
                    ${jvmBlock}
                    <div class="table-wrap table-wrap--sticky-head env-env004-tabs__table">
                        ${renderLayerSettingsTableHtml(rows)}
                    </div>
                </div>`;
        }).join('');

        wireEnv004LayerTabs();
    }

    function renderStackLayers(layers) {
        const root = document.getElementById('stackPipeline');
        if (!root) return;
        if (!layers?.length) {
            root.innerHTML = '<p class="env-empty-cell">—</p>';
            return;
        }
        root.innerHTML = layers.map((layer, idx) => {
            const arrow = idx < layers.length - 1 ? '<div class="env-stack-arrow">↓</div>' : '';
            const rows = (layer.settings || []).map(s => `
                <tr class="${rowStatusClass(s.status)}">
                    <td>${escapeHtml(s.settingLabel)}</td>
                    <td><code>${escapeHtml(s.propertyKey)}</code></td>
                    <td>${escapeHtml(s.actualValue)}</td>
                    <td>${escapeHtml(s.recommendedValue)}</td>
                    <td>${statusPill(s.status, s.statusLabel)}</td>
                </tr>
            `).join('');
            return `
                <article class="env-stack-layer">
                    <header class="env-stack-layer__head">
                        <span class="env-stack-layer__order">${layer.order}</span>
                        <strong>${escapeHtml(layer.layerName)}</strong>
                        ${statusPill(layer.layerValid ? 'NORMAL' : 'WARN', layer.layerValid ? '정상' : '점검')}
                    </header>
                    <table class="dump-report__table dump-report__table--stack"><tbody>${rows}</tbody></table>
                </article>${arrow}`;
        }).join('');
    }

    async function loadDefaults() {
        const res = await fetch(`${API}/capacity-design/defaults`, { headers: apiHeaders() });
        const data = await res.json();
        if (isSuccess(data)) applyDefaults(data.body?.response);
    }

    async function analyze(ev) {
        if (ev) ev.preventDefault();
        syncTotalUsers();
        const res = await fetch(`${API}/capacity-design/analyze`, {
            method: 'POST',
            headers: apiHeaders(),
            body: JSON.stringify(collectRequest())
        });
        const data = await res.json();
        if (!isSuccess(data)) {
            if (typeof showStatus === 'function') showStatus(data?.error?.resultMessage || '산정 실패', 'error');
            return;
        }
        const view = data.body?.response;
        persistCapacityView(view);
        applyCapacityView(view);
        if (typeof window.nsightRefreshCheckReport === 'function') {
            window.nsightRefreshCheckReport();
        }
        if (typeof showStatus === 'function') {
            const hint = ENV_PAGE === 'env002' ? ' — ENV-003·종합 보고서에서 확인하세요.' : '';
            showStatus(`산정 완료 ${view.scenarioId}${hint}`, 'success');
        }
    }

    function wireEnv002Form() {
        if (!document.getElementById('capacityDesignForm')) return;

    document.getElementById('capBranchCount')?.addEventListener('input', syncTotalUsers);
    document.getElementById('capUsersPerBranch')?.addEventListener('input', syncTotalUsers);
    function scheduleAnalyze() {
        if (ENV_PAGE !== 'env002') return;
        clearTimeout(analyzeTimer);
        analyzeTimer = setTimeout(() => analyze(), 350);
    }

    function onVmProfileChange() {
        capVmTouched = true;
        toggleCustomVm();
        syncVmCardTps();
        scheduleAnalyze();
    }

    document.querySelectorAll('input[name="capVm"]').forEach(r => r.addEventListener('change', onVmProfileChange));
    document.getElementById('capCustomCore')?.addEventListener('input', () => {
        syncVmCardTps();
        if (selectedVm() === 'CUSTOM') scheduleAnalyze();
    });
    document.getElementById('capCustomMemory')?.addEventListener('input', () => {
        if (selectedVm() === 'CUSTOM') scheduleAnalyze();
    });
    document.getElementById('capTpmcPerTps')?.addEventListener('input', () => {
        if (!isManualCoreTps()) syncCoreTpsFromTpmc();
        else syncVmCardTps();
    });
    document.getElementById('capManualCoreTps')?.addEventListener('change', () => {
        setCoreTpsReadonly(!isManualCoreTps());
        if (!isManualCoreTps()) syncCoreTpsFromTpmc();
        else syncVmCardTps();
    });
    ['capTpsPerCoreMin', 'capTpsPerCoreBase', 'capTpsPerCoreMax'].forEach(id => {
        document.getElementById(id)?.addEventListener('input', () => {
            if (isManualCoreTps()) {
                const base = parseInt(document.getElementById('capTpsPerCoreBase').value, 10) || REF_TPS_BASE;
                const tpmc = Math.round(REF_CORE_TPMC / base);
                document.getElementById('capCoreTpmcPerSec').value = (base * tpmc).toLocaleString();
                const formula = document.getElementById('capTpmcCoreFormula');
                if (formula) {
                    formula.textContent =
                        `직접 조정: TPMC/TPS ≈ ${REF_CORE_TPMC.toLocaleString()} ÷ ${base} = ${tpmc.toLocaleString()} (역산)`;
                }
                syncVmCardTps();
            }
        });
    });
    document.getElementById('capacityDesignForm')?.addEventListener('submit', analyze);
    document.getElementById('capResetBtn')?.addEventListener('click', async () => {
        await loadDefaults();
        if (typeof showStatus === 'function') showStatus('초기화 완료', 'info');
    });

        loadDefaults().then(() => {
            syncCoreTpsFromTpmc();
            const saved = loadPersistedCapacityView();
            if (saved) applyCapacityView(saved);
            analyze();
        });
    }

    function loadPersistedAssessment() {
        try {
            const raw = sessionStorage.getItem(STORAGE_ASSESSMENT);
            return raw ? JSON.parse(raw) : null;
        } catch (e) {
            return null;
        }
    }

    function countLayerGridStatus(grid) {
        const counts = { NORMAL: 0, WARN: 0, CRITICAL: 0 };
        (grid || []).forEach(r => {
            if (counts[r.status] !== undefined) counts[r.status]++;
        });
        return counts;
    }

    function reportHeroClass(ok) {
        return ok ? 'env-check-report__hero--ok' : 'env-check-report__hero--warn';
    }

    function reportRuleClass(run) {
        if (!run) return 'env-check-report__rule--pending';
        if (run.status === 'PASS') return 'env-check-report__rule--ok';
        return 'env-check-report__rule--warn';
    }

    function resolveCheckReportStack(view) {
        const norm = normalizeCapacityView(view);
        const grid = norm.layerGrid || [];
        const layers = (norm.stackLayers?.length ? norm.stackLayers : rebuildStackLayersFromGrid(grid))
            .slice()
            .sort((a, b) => (a.order || 0) - (b.order || 0));
        return { norm, grid, layers, jvm: norm.jvmSizing };
    }

    function renderCheckReportStackSettingRow(s) {
        return `
            <tr class="${rowStatusClass(s.status)}">
                <td>${escapeHtml(s.settingLabel)}</td>
                <td><code>${escapeHtml(s.propertyKey)}</code></td>
                <td>${escapeHtml(s.recommendedValue)}</td>
                <td><code>${escapeHtml(s.actualValue)}</code></td>
                <td>${statusPill(s.status, s.statusLabel)}</td>
                <td class="env-cell-reason">${escapeHtml(s.reason)}</td>
                <td>${escapeHtml(s.configLocation)}</td>
                <td><code class="env-example">${escapeHtml(s.settingExample)}</code></td>
                <td class="env-cell-action">${escapeHtml(s.actionGuide)}</td>
            </tr>`;
    }

    /** 종합 보고서 — UI~MyBatis 전체 환경 구성 */
    function renderCheckReportStackConfigHtml(view) {
        const { grid, layers, jvm } = resolveCheckReportStack(view);
        if (!grid.length && !layers.length) {
            return `<p class="env-check-report__placeholder">
                계층 설정 없음 — <a href="/oc/env-002">ENV-002</a> 산정 후 새로고침하세요.
            </p>`;
        }
        const flow = layers.map(l => escapeHtml(l.layerName)).join(' → ');
        const nav = layers.map(l => `
            <a href="#check-layer-${l.layerId}" class="env-check-report__stack-pill${l.layerValid ? '' : ' env-check-report__stack-pill--warn'}">
                ${escapeHtml(l.layerName)}
            </a>`).join('');
        const masterRows = (grid || []).map(g => `
            <tr class="${rowStatusClass(g.status)}">
                <td><strong>${escapeHtml(g.layer)}</strong></td>
                <td>${escapeHtml(g.settingLabel)}</td>
                <td><code>${escapeHtml(g.propertyKey)}</code></td>
                <td>${escapeHtml(g.recommendedValue)}</td>
                <td><code>${escapeHtml(g.currentValue)}</code></td>
                <td>${statusPill(g.status, g.statusLabel)}</td>
                <td>${escapeHtml(g.configLocation)}</td>
                <td><code class="env-example">${escapeHtml(g.settingExample)}</code></td>
            </tr>`).join('');
        const pipeline = layers.map((layer, idx) => {
            const arrow = idx < layers.length - 1
                ? '<div class="env-check-report__stack-arrow" aria-hidden="true">↓</div>'
                : '';
            const settings = layer.settings || [];
            const rows = settings.map(renderCheckReportStackSettingRow).join('');
            const jvmBlock = layer.layerName === 'JVM' && jvm
                ? renderJvmSizingHtml(jvm, { compact: false })
                : '';
            return `
                <article class="env-check-report__layer" id="check-layer-${layer.layerId}">
                    <header class="env-check-report__layer-head">
                        <span class="env-check-report__layer-order">${layer.order}</span>
                        <div class="env-check-report__layer-titles">
                            <strong>${escapeHtml(layer.layerName)}</strong>
                            <span class="env-check-report__layer-desc">${escapeHtml(layer.description || '')}</span>
                        </div>
                        ${statusPill(layer.layerValid ? 'NORMAL' : 'WARN', layer.layerValid ? '정상' : '점검')}
                    </header>
                    ${jvmBlock}
                    <div class="table-wrap env-check-report__layer-table">
                        <table class="dump-report__table dump-report__table--check-stack">
                            <thead><tr>
                                <th>설정</th><th>Property</th><th>권장</th><th>현재</th><th>판정</th>
                                <th>사유</th><th>위치</th><th>예시</th><th>조치</th>
                            </tr></thead>
                            <tbody>${rows || '<tr><td colspan="9" class="env-empty-cell">—</td></tr>'}</tbody>
                        </table>
                    </div>
                </article>${arrow}`;
        }).join('');
        return `
            <p class="env-check-report__lead env-check-report__stack-flow"><strong>요청 흐름</strong> ${flow}</p>
            <nav class="env-check-report__stack-nav" aria-label="계층 바로가기">${nav}</nav>
            <div class="env-check-report__pipeline">${pipeline}</div>
            <details class="env-check-report__master-grid">
                <summary>전체 설정 한눈에 보기 (Grid 통합표 · ${grid.length}항목)</summary>
                <div class="table-wrap">
                    <table class="dump-report__table dump-report__table--check-stack dump-report__table--check-master">
                        <thead><tr>
                            <th>계층</th><th>설정</th><th>Property</th><th>권장</th><th>현재</th><th>판정</th><th>위치</th><th>예시</th>
                        </tr></thead>
                        <tbody>${masterRows}</tbody>
                    </table>
                </div>
            </details>`;
    }

    /** 종합 보고서 탭 — ENV-002~004 조건·결론 + Rule 요약 */
    function renderCheckIntegratedReport() {
        const panel = document.getElementById('envCheckReportPanel');
        if (!panel) return;

        const view = loadPersistedCapacityView();
        const assessment = loadPersistedAssessment();

        if (!view?.planner) {
            panel.className = 'env-check-report env-check-report--empty';
            panel.innerHTML = `
                <p class="env-check-report__placeholder">
                    <strong>산정 데이터 없음</strong> —
                    <a href="/oc/env-002">ENV-002</a>에서 조건 입력 후 「산정 실행」을 하고 이 페이지를 새로고침하세요.
                </p>
                <p class="env-check-report__placeholder">
                    Rule Engine: <a href="/oc/rule-check">Rule 점검</a>에서 설정 업로드·「점검 실행」 후 이 보고서에 Rule 결론이 추가됩니다.
                </p>`;
            return;
        }

        const normView = normalizeCapacityView(view);
        const jvm = normView.jvmSizing;
        const p = normView.planner;
        const ex = pickExampleVmRow(p);
        const risk = p.riskSummary || {};
        const layerCounts = countLayerGridStatus(normView.layerGrid);
        const stackOk = normView.stackValid;
        const critRows = (p.vmResults || []).filter(r => r.status === 'CRITICAL');
        const warnRows = (p.vmResults || []).filter(r => r.status === 'WARN');

        const capOk = critRows.length === 0;
        const layerOk = layerCounts.CRITICAL === 0;
        const ruleOk = assessment ? assessment.status === 'PASS' : null;
        const overallOk = capOk && layerOk && (ruleOk === null || ruleOk);

        let overallText = '산정·계층 점검 기준 충족';
        if (!capOk) overallText = `VM 산정 ${critRows.length}건 위험 — ENV-003 확인`;
        else if (!layerOk) overallText = `계층 설정 ${layerCounts.CRITICAL}건 위험 — ENV-004 확인`;
        else if (ruleOk === false) overallText = `Rule Engine ${assessment.failCount}건 실패 — Rule 점검 탭에서 상세 확인`;

        const ruleBlock = assessment
            ? `<div class="env-check-report__rule ${reportRuleClass(assessment)}">
                <h4>6. Rule Engine 결론</h4>
                <p><strong>${escapeHtml(assessment.runId)}</strong> · 상태 <strong>${escapeHtml(assessment.status)}</strong>
                   · 통과 ${assessment.passCount} / 주의 ${assessment.warnCount} / 실패 ${assessment.failCount}</p>
               </div>`
            : `<div class="env-check-report__rule env-check-report__rule--pending">
                <h4>6. Rule Engine</h4>
                <p>미실행 — <a href="/oc/rule-check">Rule 점검</a>에서 「설정 업로드·파싱」 후 「점검 실행」하면 이 보고서에 Rule 결론이 반영됩니다.</p>
               </div>`;

        const ruleDetailLink = assessment
            ? `<p class="env-check-report__link"><a href="/oc/rule-check">Rule 점검 상세 →</a></p>`
            : '';

        const vmTableRows = (p.vmResults || []).slice(0, 6).map(r => `
            <tr class="${rowStatusClass(r.status)}">
                <td>${r.requestRatePercent}% / ${r.timeoutSec}초</td>
                <td>${r.realRequesters.toLocaleString()}명</td>
                <td>${r.targetTps.toLocaleString()}</td>
                <td>${r.requiredVmSingleCenter}대 (A-A ${r.recommendedVmActiveActive})</td>
                <td>${statusPill(r.status, r.status === 'NORMAL' ? '정상' : r.status === 'WARN' ? '경고' : '위험')}</td>
            </tr>`).join('');

        panel.className = 'env-check-report';
        panel.innerHTML = `
            <div class="env-check-report__hero ${reportHeroClass(overallOk)}">
                <p class="env-check-report__hero-label">종합 결론</p>
                <p class="env-check-report__hero-text">${escapeHtml(overallText)}</p>
                <p class="env-check-report__hero-meta">
                    시나리오 <code>${escapeHtml(normView.scenarioId || '')}</code>
                    · ${escapeHtml(p.scenarioLabel || '')}
                </p>
            </div>
            <div class="env-check-report__grid">
                <section class="env-check-report__sect">
                    <h4>1. ENV-002 산정 조건</h4>
                    <ul class="env-check-report__list">
                        <li>전체 사용자 <strong>${(p.totalUsers ?? 0).toLocaleString()}명</strong>
                            (${(p.branchCount ?? 0).toLocaleString()}지점 × ${p.usersPerBranch}명)</li>
                        <li>VM <strong>${escapeHtml(p.vmProfileId || '')}</strong> · ${p.vmCores}코어/${p.vmMemoryGb}GB</li>
                        <li>요청 부하 <strong>${(p.tpmcPerTps ?? 0).toLocaleString()}</strong> TPMC/TPS
                            · Core TPS 선정 <strong>${p.tpsPerCoreBase}</strong> (보수 ${p.tpsPerCoreMin} ~ 여유 ${p.tpsPerCoreMax})</li>
                        <li>세션 설계 ${(p.designSessions ?? 0).toLocaleString()}명 · Idle ${p.primarySessionMinutes}분</li>
                        <li>Active-Active ${p.activeActive ? '적용' : '미적용'}</li>
                    </ul>
                    <p class="env-check-report__link"><a href="/oc/env-002">ENV-002 상세 →</a></p>
                </section>
                <section class="env-check-report__sect">
                    <h4>2. ENV-003 TPS·VM 산정 결론</h4>
                    ${ex ? `<p class="env-check-report__lead">
                        대표 <strong>${ex.requestRatePercent}% · ${ex.timeoutSec}초</strong> —
                        실요청자 <strong>${ex.realRequesters.toLocaleString()}명</strong> →
                        목표 TPS <strong>${ex.targetTps.toLocaleString()}</strong> →
                        필요 VM <strong>${ex.requiredVmSingleCenter}대</strong>
                        (A-A 권장 ${ex.recommendedVmActiveActive}대)
                    </p>` : ''}
                    <p class="env-check-report__counts">
                        정상 <strong>${risk.normal ?? 0}</strong> ·
                        경고 <strong>${risk.warning ?? 0}</strong> ·
                        위험 <strong>${risk.critical ?? 0}</strong>
                        (시나리오 ${(p.vmResults || []).length}조합)
                    </p>
                    ${vmTableRows ? `<div class="table-wrap env-check-report__mini-table">
                        <table class="dump-report__table dump-report__table--data">
                            <thead><tr><th>요청률/초</th><th>실요청자</th><th>목표 TPS</th><th>필요 VM</th><th>판정</th></tr></thead>
                            <tbody>${vmTableRows}</tbody>
                        </table>
                    </div>` : ''}
                    <p class="env-check-report__link"><a href="/oc/env-003">ENV-003 전체 표 →</a></p>
                </section>
                <section class="env-check-report__sect env-check-report__sect--env004">
                    <h4>3. ENV-004 계층 점검 요약</h4>
                    <p class="env-check-report__lead">
                        스택 <strong class="${stackOk ? 'env-ok' : 'env-warn'}">${stackOk ? 'STACK OK' : 'STACK 점검 필요'}</strong>
                        · 계층 항목 정상 ${layerCounts.NORMAL} · 경고 ${layerCounts.WARN} · 위험 ${layerCounts.CRITICAL}
                        ${jvm ? ` · JVM Heap 일반 <strong>${jvm.heapGeneralMinGb}~${jvm.heapGeneralMaxGb}GB</strong> / SV ≤${jvm.heapSingleViewMaxGb}GB` : ''}
                    </p>
                    <p class="env-check-report__link">상세 구성은 아래 <strong>§5 환경 구성</strong> · <a href="/oc/env-004">ENV-004</a></p>
                </section>
                <section class="env-check-report__sect env-check-report__sect--core">
                    <h4>4. Core·VM 처리량 (선정값)</h4>
                    ${renderCoreTpsCriteriaHtml(p)}
                </section>
            </div>
            <section class="env-check-report__sect env-check-report__sect--stack">
                <h4>5. 환경 구성 상세 (UI → GSLB → L4 → Apache → Tomcat → JVM → Spring Boot → MyBatis)</h4>
                ${renderCheckReportStackConfigHtml(normView)}
                <p class="env-check-report__link"><a href="/oc/env-004">ENV-004 Grid · 파이프라인 →</a></p>
            </section>
            ${ruleBlock}
            ${ruleDetailLink}
            <p class="env-check-report__footer">보고서 생성: 브라우저 산정 캐시 · Rule은 <a href="/oc/rule-check">Rule 점검</a> 실행 시 갱신</p>`;
    }

    async function initCapacityViewPages() {
        let view = loadPersistedCapacityView();
        if (view?.planner && loadPersistedRequest()) {
            const stale = !view.jvmSizing?.heapGeneralMinGb || !layerGridHasJvm(view.layerGrid);
            if (stale) {
                const refreshed = await refreshCapacityViewFromServer();
                if (refreshed?.planner) view = refreshed;
            }
        }
        if (ENV_PAGE === 'env004' || ENV_PAGE === 'env001' || ENV_PAGE === 'env003') {
            applyCapacityView(view);
        }
        if (ENV_PAGE === 'check') {
            renderCheckIntegratedReport();
        }
    }

    function initCapacityPlanner() {
        if (ENV_PAGE === 'env002') {
            wireEnv002Form();
            return;
        }
        if (ENV_PAGE === 'env001' || ENV_PAGE === 'env003' || ENV_PAGE === 'env004' || ENV_PAGE === 'check') {
            initCapacityViewPages();
            return;
        }
    }

    function refreshCheckPageBaselines() {
        if (!document.getElementById('envCheckReportPanel')) return;
        renderCheckIntegratedReport();
    }

    window.nsightBuildBaselineFromCapacity = buildProjectBaselineFromCapacity;
    window.nsightRefreshCheckReport = refreshCheckPageBaselines;
    window.nsightInitCapacityView = initCapacityViewPages;
    window.nsightGetCapacityViewForExport = function () {
        return normalizeCapacityView(loadPersistedCapacityView());
    };
    window.nsightGetCapacityRequestForExport = function () {
        return loadPersistedRequest();
    };

    initCapacityPlanner();
})();
