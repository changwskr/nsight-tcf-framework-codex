/**
 * pdmg-infra 내장 UI → 동일 오리진 TCF 거래 호출.
 * POST /{serviceId} + { hdr_nhnis, dto }
 */
(function (global) {
  function newGuid() {
    if (window.crypto && typeof window.crypto.randomUUID === 'function') {
      return window.crypto.randomUUID().replaceAll('-', '');
    }
    return 'xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx'.replace(/x/g, () =>
      ((Math.random() * 16) | 0).toString(16));
  }

  function baseUrl() {
    const el = document.getElementById('targetBaseUrl');
    if (el && el.value && el.value.trim()) {
      return el.value.trim().replace(/\/$/, '');
    }
    return '';
  }

  function buildHeader(serviceId, scid) {
    const role = getOperator();
    return {
      sys_comm: {
        std_gbl_id: newGuid(),
        rms_svc_c: serviceId,
        scid: scid || serviceId,
        optr_eno: role.optrEno,
        tr_optrnm: role.label,
        tr_trm_ipadr: '127.0.0.1',
        tr_sysid: 'PDMG-INFRA',
        sync_dsc: 'S',
        std_tgrm_rqr_rsp_dsc: 'Q',
        tr_brc: '10001'
      }
    };
  }

  const ROLE_PRESETS = [
    { optrEno: 'E0000001', role: 'ARCH', label: 'Arch(E0000001)' },
    { optrEno: 'E0000002', role: 'OPS', label: 'Ops(E0000002)' },
    { optrEno: 'E0000003', role: 'SEC', label: 'Sec(E0000003)' },
    { optrEno: 'E0000004', role: 'PMO', label: 'PMO(E0000004)' },
    { optrEno: 'E0000005', role: 'ADMIN', label: 'Admin(E0000005)' },
    { optrEno: 'E0000006', role: 'DBA', label: 'DBA(E0000006)' },
    { optrEno: 'E0000007', role: 'MW', label: 'MW(E0000007)' }
  ];

  function getOperator() {
    try {
      const raw = localStorage.getItem('pdmg-infra-optr');
      if (raw) {
        const parsed = JSON.parse(raw);
        if (parsed && parsed.optrEno) return parsed;
      }
    } catch (_) { /* ignore */ }
    return ROLE_PRESETS[0];
  }

  function setOperator(optrEno) {
    const found = ROLE_PRESETS.find((r) => r.optrEno === optrEno) || ROLE_PRESETS[0];
    localStorage.setItem('pdmg-infra-optr', JSON.stringify(found));
    paintRoleBar();
    return found;
  }

  function paintRoleBar() {
    let bar = document.getElementById('infraRoleBar');
    if (!bar) {
      bar = document.createElement('div');
      bar.id = 'infraRoleBar';
      bar.style.cssText = 'position:fixed;right:12px;bottom:12px;z-index:9999;background:#1f2937;color:#fff;padding:8px 10px;border-radius:8px;font:12px/1.4 sans-serif;box-shadow:0 4px 16px rgba(0,0,0,.25)';
      document.body.appendChild(bar);
    }
    const cur = getOperator();
    bar.innerHTML = `역할 <select id="infraRoleSelect" style="margin-left:6px">${ROLE_PRESETS.map((r) =>
      `<option value="${r.optrEno}" ${r.optrEno === cur.optrEno ? 'selected' : ''}>${r.label}</option>`
    ).join('')}</select>`;
    const sel = document.getElementById('infraRoleSelect');
    if (sel) sel.onchange = () => setOperator(sel.value);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', paintRoleBar);
  } else {
    paintRoleBar();
  }

  async function postService(serviceId, dto, scid, timeoutMs) {
    const started = performance.now();
    const url = `${baseUrl()}/${serviceId}`;
    const body = {
      hdr_nhnis: buildHeader(serviceId, scid),
      dto: dto == null ? {} : dto
    };
    const controller = new AbortController();
    const ms = timeoutMs == null ? 8000 : Number(timeoutMs);
    const timer = ms > 0 ? setTimeout(() => controller.abort(), ms) : null;
    let httpStatus = 0;
    let responseBody = '';
    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json;charset=UTF-8',
          Accept: 'application/json'
        },
        body: JSON.stringify(body),
        signal: controller.signal
      });
      httpStatus = response.status;
      responseBody = await response.text();
    } catch (error) {
      const aborted = !!(error && error.name === 'AbortError');
      httpStatus = aborted ? 504 : 502;
      responseBody = JSON.stringify({
        error: aborted ? `요청 시간 초과 (${ms} ms)` : (error && error.message) || String(error)
      });
    } finally {
      if (timer) {
        clearTimeout(timer);
      }
    }

    let parsed = null;
    try {
      parsed = responseBody ? JSON.parse(responseBody) : null;
    } catch (_) {
      parsed = null;
    }

    return {
      serviceId,
      url,
      httpStatus,
      elapsedMs: Math.round(performance.now() - started),
      responseBody,
      parsed,
      dto: parsed && parsed.dto && typeof parsed.dto === 'object' ? parsed.dto : parsed
    };
  }

  function text(value) {
    if (value === null || value === undefined || value === '') {
      return '-';
    }
    return String(value);
  }

  function escapeHtml(value) {
    return text(value)
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;');
  }

  function statusBadge(status) {
    const s = text(status);
    let cls = 'badge';
    if (s === 'CONFIRMED' || s === 'MIGRATED' || s === 'PASS') {
      cls += ' badge--ok';
    } else if (s === 'VALIDATING' || s === 'TARGET_DEFINED' || s === 'CONDITIONAL') {
      cls += ' badge--warn';
    } else if (s === 'RETIRED' || s === 'FAIL') {
      cls += ' badge--fail';
    }
    return `<span class="${cls}">${escapeHtml(s)}</span>`;
  }

  global.InfraApi = {
    newGuid,
    baseUrl,
    buildHeader,
    postService,
    text,
    escapeHtml,
    statusBadge,
    getOperator,
    setOperator,
    ROLE_PRESETS
  };
})(window);
