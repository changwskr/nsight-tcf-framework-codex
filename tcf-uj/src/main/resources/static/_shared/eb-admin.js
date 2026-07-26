/**
 * NSIGHT EB 관리 공통 유틸 (tcf-uj)
 */
window.EbAdmin = (function () {
  const BUSINESS_CODE = 'EB';
  const LOCAL_PORT = 8089;
  const CONTEXT_PATH = '/eb';

  const TX = {
    userInquiry: { serviceId: 'EB.User.inquiry', transactionCode: 'EB-USR-0002', serviceName: 'EB 사용자 조회' },
    userCreate: { serviceId: 'EB.User.create', transactionCode: 'EB-USR-0001', serviceName: 'EB 사용자 등록' },
    eventInquiry: { serviceId: 'EB.Event.inquiry', transactionCode: 'EB-EVT-0001', serviceName: 'EB 이벤트 조회' },
    batchInquiry: { serviceId: 'EB.Batch.inquiry', transactionCode: 'EB-BAT-0001', serviceName: 'EB 배치 상태 조회' }
  };

  let config = { deploymentMode: 'bootrun', bootrunHost: 'http://127.0.0.1', tomcatGatewayUrl: 'http://localhost:8080' };
  let targetUrl = '-';

  function todaySystemDate() {
    const now = new Date();
    const y = now.getFullYear();
    const m = String(now.getMonth() + 1).padStart(2, '0');
    const d = String(now.getDate()).padStart(2, '0');
    return `${y}${m}${d}`;
  }

  function newGuid() {
    if (window.crypto && crypto.randomUUID) return crypto.randomUUID();
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
      const r = Math.random() * 16 | 0;
      return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
    });
  }

  function nowIsoKst() {
    const now = new Date();
    const offset = now.getTime() + (9 * 60 - now.getTimezoneOffset()) * 60000;
    return new Date(offset).toISOString().replace('Z', '+09:00');
  }

  function isTomcatUiDeployment() {
    return config.deploymentMode === 'tomcat'
        || location.pathname.startsWith('/uj/') || location.pathname === '/uj';
  }

  function buildRelayQuery() {
    const mode = isTomcatUiDeployment() ? 'tomcat' : (config.deploymentMode || 'bootrun');
    return new URLSearchParams({
      deploymentMode: mode,
      bootrunHost: config.bootrunHost,
      tomcatGatewayUrl: config.tomcatGatewayUrl || 'http://localhost:8080'
    }).toString();
  }

  function uiContextPrefix() {
    if (window.__NSIGHT_UJ_CTX__) return window.__NSIGHT_UJ_CTX__;
    if (location.pathname.startsWith('/uj/') || location.pathname === '/uj') return '/uj';
    return '';
  }

  function uiPath(path) {
    if (typeof window.nsightUjUrl === 'function') return window.nsightUjUrl(path);
    const normalized = path.startsWith('/') ? path : '/' + path;
    const prefix = uiContextPrefix();
    if (prefix && (normalized === prefix || normalized.startsWith(prefix + '/'))) return normalized;
    return prefix + normalized;
  }

  function relayFetch(path, init) {
    return fetch(uiPath(path), init);
  }

  function resolveEbBaseUrl() {
    if (isTomcatUiDeployment()) {
      return (config.tomcatGatewayUrl || 'http://localhost:8080').replace(/\/$/, '') + CONTEXT_PATH;
    }
    return (config.bootrunHost || 'http://127.0.0.1').replace(/\/$/, '') + ':' + LOCAL_PORT;
  }

  let errorPopupReady = null;
  function ensureErrorPopupReady() {
    if (window.NsightErrorPopup) return Promise.resolve(window.NsightErrorPopup);
    if (!errorPopupReady) {
      errorPopupReady = new Promise(resolve => {
        const finish = () => resolve(window.NsightErrorPopup || null);
        if (document.querySelector('script[data-nsight-error-popup]')) {
          const wait = setInterval(() => {
            if (window.NsightErrorPopup) { clearInterval(wait); finish(); }
          }, 30);
          setTimeout(() => { clearInterval(wait); finish(); }, 3000);
          return;
        }
        const script = document.createElement('script');
        script.src = uiContextPrefix() + '/_shared/error-popup.js';
        script.setAttribute('data-nsight-error-popup', '');
        script.onload = finish;
        script.onerror = finish;
        document.head.appendChild(script);
      });
    }
    return errorPopupReady;
  }

  async function notifyTransactionError(payload, relay, fallbackMessage) {
    const popup = await ensureErrorPopupReady();
    if (popup) popup.show(popup.fromPayload(payload, relay, fallbackMessage));
  }

  async function parseRelayResponse(res) {
    const text = await res.text();
    let relay;
    try {
      relay = text ? JSON.parse(text) : {};
    } catch (e) {
      throw new Error(`릴레이 응답 파싱 실패 (HTTP ${res.status})`);
    }
    if (relay.responseBody == null || relay.responseBody === '') {
      const status = relay.httpStatus != null ? relay.httpStatus : (relay.status != null ? relay.status : res.status);
      throw new Error(relay.errorMessage || relay.message || `HTTP ${status}: 응답 없음`);
    }
    return relay;
  }

  function buildHeader(tx, processingType) {
    return {
      systemId: 'NSIGHT-MP',
      businessCode: BUSINESS_CODE,
      serviceId: tx.serviceId,
      transactionCode: tx.transactionCode,
      serviceName: tx.serviceName || '',
      processingType: (processingType || 'INQUIRY').toUpperCase(),
      guid: newGuid(),
      traceId: '',
      channelId: 'WEBTOP',
      userId: 'ADMIN',
      branchId: '001',
      requestTime: nowIsoKst(),
      systemDate: todaySystemDate(),
      bizDate: todaySystemDate(),
      clientIp: '127.0.0.1'
    };
  }

  async function loadConfig() {
    const res = await relayFetch('/api/config');
    if (res.ok) {
      const data = await res.json();
      config.deploymentMode = data.deploymentMode || config.deploymentMode;
      config.bootrunHost = data.bootrunHost || config.bootrunHost;
      config.tomcatGatewayUrl = data.tomcatGatewayUrl || config.tomcatGatewayUrl;
    }
    const urlRes = await relayFetch(`/api/business-modules/${BUSINESS_CODE}/target-url?${buildRelayQuery()}`);
    if (urlRes.ok) {
      const data = await urlRes.json();
      targetUrl = data.targetUrl || targetUrl;
    }
    return config;
  }

  async function call(txKey, body, processingType) {
    const tx = typeof txKey === 'string' ? TX[txKey] : txKey;
    if (!tx || !tx.serviceId) {
      throw new Error(`거래 정의를 찾을 수 없습니다: ${txKey}`);
    }
    const request = { header: buildHeader(tx, processingType), body: body || {} };
    const res = await relayFetch(`/api/relay/${BUSINESS_CODE}/online?${buildRelayQuery()}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(request)
    });
    const relay = await parseRelayResponse(res);
    let payload;
    try {
      payload = JSON.parse(relay.responseBody);
    } catch (e) {
      throw new Error('응답 JSON 파싱 실패');
    }
    if (relay.httpStatus >= 400) {
      let msg = payload.result?.errorMessage || payload.result?.message || payload.error;
      if (!msg && relay.httpStatus === 502) {
        msg = isTomcatUiDeployment()
          ? 'eb-service(/eb)에 연결할 수 없습니다. Tomcat Gateway 기동 상태를 확인하세요.'
          : 'eb-service(8089)에 연결할 수 없습니다. eb-service를 먼저 실행하세요.';
      }
      await notifyTransactionError(payload, relay, msg || `HTTP ${relay.httpStatus}`);
      throw new Error(msg || `HTTP ${relay.httpStatus}`);
    }
    if (payload.result && payload.result.resultCode && payload.result.resultCode !== 'S0000') {
      const detail = payload.result.errorDetail ? ` (${payload.result.errorDetail})` : '';
      const msg = (payload.result.errorMessage || payload.result.resultMessage || '거래 오류') + detail;
      await notifyTransactionError(payload, relay, msg);
      throw new Error(msg);
    }
    return { payload, relay, body: payload.body || {} };
  }

  async function inquiry(txKey, body) {
    return call(txKey, body, 'INQUIRY');
  }

  async function mutate(txKey, body, processingType) {
    return call(txKey, body, processingType || 'CREATE');
  }

  function field(row, name, fallback) {
    if (!row) return fallback !== undefined ? fallback : '-';
    const raw = row[name];
    if (raw != null && raw !== '' && raw !== 'null') return raw;
    const upper = name.toUpperCase();
    if (row[upper] != null && row[upper] !== '' && row[upper] !== 'null') return row[upper];
    const key = Object.keys(row).find(k => k.toUpperCase() === upper);
    const val = key ? row[key] : undefined;
    if (val != null && val !== '' && val !== 'null') return val;
    return fallback !== undefined ? fallback : '-';
  }

  function formatTs(value) {
    if (value == null || value === '' || value === '-') return '-';
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) return String(value);
    const pad = v => String(v).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  }

  function chipForEventStatus(status) {
    const s = String(status || '').toUpperCase();
    if (s === 'SENT') return `<span class="eb-chip ok">${status}</span>`;
    if (s === 'READY') return `<span class="eb-chip warn">${status}</span>`;
    if (s === 'FAIL') return `<span class="eb-chip fail">${status}</span>`;
    return `<span class="eb-chip muted">${status || '-'}</span>`;
  }

  function formatEnabled(value) {
    return value === true || value === 'true' || value === 'Y' ? '활성' : '비활성';
  }

  function formatMs(ms) {
    const n = Number(ms);
    if (Number.isNaN(n)) return '-';
    if (n >= 60000 && n % 60000 === 0) return `${n / 60000}분 (${n}ms)`;
    return `${n}ms`;
  }

  function renderStatusSummary(container, summary) {
    if (!container) return;
    const ready = summary?.READY ?? summary?.ready ?? 0;
    const sent = summary?.SENT ?? summary?.sent ?? 0;
    const fail = summary?.FAIL ?? summary?.fail ?? 0;
    const total = summary?.TOTAL ?? summary?.total ?? (Number(ready) + Number(sent) + Number(fail));
    container.innerHTML = `
      <div class="eb-stat-card warn"><div class="label">READY</div><div class="value">${ready}</div></div>
      <div class="eb-stat-card ok"><div class="label">SENT</div><div class="value">${sent}</div></div>
      <div class="eb-stat-card fail"><div class="label">FAIL</div><div class="value">${fail}</div></div>
      <div class="eb-stat-card"><div class="label">TOTAL</div><div class="value">${total}</div></div>`;
  }

  function renderPagination(container, pageNo, pageSize, totalCount, onPage) {
    const totalPages = Math.max(1, Math.ceil(totalCount / pageSize));
    if (totalCount === 0) {
      container.innerHTML = '';
      container.hidden = true;
      return;
    }
    container.hidden = false;
    let nums = '';
    for (let i = 1; i <= totalPages; i += 1) {
      nums += `<button type="button" class="eb-page-btn ${i === pageNo ? 'active' : ''}" data-page="${i}">${i}</button>`;
    }
    container.innerHTML = `
      <button type="button" class="eb-page-btn" data-page="prev" ${pageNo <= 1 ? 'disabled' : ''}>PREV</button>
      ${nums}
      <button type="button" class="eb-page-btn" data-page="next" ${pageNo >= totalPages ? 'disabled' : ''}>NEXT</button>
      <span class="eb-page-meta">${pageNo} / ${totalPages} · 총 ${totalCount}건</span>`;
    container.querySelectorAll('[data-page]').forEach(btn => {
      btn.addEventListener('click', () => {
        const v = btn.getAttribute('data-page');
        if (v === 'prev' && pageNo > 1) onPage(pageNo - 1);
        else if (v === 'next' && pageNo < totalPages) onPage(pageNo + 1);
        else if (v !== 'prev' && v !== 'next') onPage(Number(v));
      });
    });
  }

  function showAlert(el, message, type) {
    if (!el) return;
    el.hidden = false;
    el.className = `eb-alert ${type || 'info'}`;
    el.textContent = message;
  }

  function hideAlert(el) {
    if (!el) return;
    el.hidden = true;
    el.textContent = '';
  }

  return {
    TX,
    loadConfig,
    inquiry,
    mutate,
    field,
    formatTs,
    chipForEventStatus,
    formatEnabled,
    formatMs,
    renderStatusSummary,
    renderPagination,
    showAlert,
    hideAlert,
    uiPath,
    getTargetUrl: () => targetUrl,
    resolveEbBaseUrl
  };
})();
