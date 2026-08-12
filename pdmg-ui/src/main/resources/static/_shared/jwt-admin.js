/**
 * PDMG JWT 인증 포털 공통 유틸
 * 브라우저가 pdmg-jwt(:8110) 를 POST /online · JWKS 로 직접 호출한다.
 */
window.JwtAdmin = (function () {
  const BUSINESS_CODE = 'JWT';
  const SESSION_KEY = 'pdmg.jwt.session';
  const DEFAULT_JWT_BASE = 'http://localhost:8110';

  const NAV = [
    { id: 'token', label: 'mgjwa1001S0 · 토큰 현황', href: '/jwt/admin/token.html' },
    { id: 'login-history', label: 'mgjwa1002S0 · 로그인 이력', href: '/jwt/admin/login-history.html' },
    { id: 'refresh-token', label: 'mgjwa1003S0 · Refresh Token', href: '/jwt/admin/refresh-token.html' },
    { id: 'security-policy', label: 'mgjwa1004S0 · 보안정책', href: '/jwt/admin/security-policy.html' },
    { id: 'jwks', label: 'jwks · JWK 공개키', href: '/jwt/admin/jwks.html' }
  ];

  const TX = {
    authLogin: { serviceId: 'mgjwa1000C0', transactionCode: 'JWT-AUT-0001', serviceName: 'JWT 로그인' },
    authRefresh: { serviceId: 'mgjwa1000U0', transactionCode: 'JWT-AUT-0002', serviceName: 'Refresh 갱신' },
    authRevoke: { serviceId: 'mgjwa1000D0', transactionCode: 'JWT-AUT-0003', serviceName: 'Access 폐기' },
    authLogout: { serviceId: 'mgjwa1000D1', transactionCode: 'JWT-AUT-0004', serviceName: 'JWT 로그아웃' },
    tokenInquiry: { serviceId: 'mgjwa1001S0', transactionCode: 'JWT-TKN-0001', serviceName: '토큰 현황 조회' },
    tokenRevoke: { serviceId: 'mgjwa1001D0', transactionCode: 'JWT-TKN-0002', serviceName: '토큰 강제폐기' },
    loginHistory: { serviceId: 'mgjwa1002S0', transactionCode: 'JWT-LGH-0001', serviceName: '로그인 이력 조회' },
    refreshTokenInquiry: { serviceId: 'mgjwa1003S0', transactionCode: 'JWT-RTK-0001', serviceName: 'Refresh Token 조회' },
    securityPolicyInquiry: { serviceId: 'mgjwa1004S0', transactionCode: 'JWT-SCP-0001', serviceName: '보안정책 조회' },
    securityPolicyUpdate: { serviceId: 'mgjwa1004U0', transactionCode: 'JWT-SCP-0002', serviceName: '보안정책 수정' }
  };

  let config = { jwtBaseUrl: DEFAULT_JWT_BASE, timeoutMs: 10000 };
  let targetUrl = DEFAULT_JWT_BASE;

  function todaySystemDate() {
    const now = new Date();
    return `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`;
  }

  function newGuid() {
    if (window.crypto && crypto.randomUUID) return crypto.randomUUID();
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
      const r = Math.random() * 16 | 0;
      return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
    });
  }

  function uiPath(path) {
    if (typeof window.nsightUiUrl === 'function') return window.nsightUiUrl(path);
    return path.startsWith('/') ? path : '/' + path;
  }

  function resolveJwtBaseUrl() {
    return (config.jwtBaseUrl || DEFAULT_JWT_BASE).replace(/\/$/, '');
  }

  function resolveJwksUrl() {
    return resolveJwtBaseUrl() + '/.well-known/jwks.json';
  }

  let errorPopupReady = null;
  function ensureErrorPopupReady() {
    if (window.PdmgErrorPopup) return Promise.resolve(window.PdmgErrorPopup);
    if (window.NsightErrorPopup) return Promise.resolve(window.NsightErrorPopup);
    if (!errorPopupReady) {
      errorPopupReady = new Promise(resolve => {
        const finish = () => resolve(window.PdmgErrorPopup || window.NsightErrorPopup || null);
        const existing = document.querySelector('script[data-pdmg-error-popup], script[data-nsight-error-popup]');
        if (existing) {
          const wait = setInterval(() => {
            if (window.PdmgErrorPopup || window.NsightErrorPopup) { clearInterval(wait); finish(); }
          }, 30);
          setTimeout(() => { clearInterval(wait); finish(); }, 2000);
          return;
        }
        const script = document.createElement('script');
        script.src = '/_shared/error-popup.js';
        script.setAttribute('data-pdmg-error-popup', '');
        script.onload = finish;
        script.onerror = finish;
        document.head.appendChild(script);
      });
    }
    return errorPopupReady;
  }

  async function showErrorPopup(info) {
    const popup = await ensureErrorPopupReady();
    if (!popup) return;
    const opts = info && typeof info === 'object' ? info : { message: String(info || '') };

    // 응답 전문(result)이 있으면 공통 파서로 코드·상세·원문을 채운다.
    if (opts.payload != null && typeof popup.showFromResponse === 'function') {
      popup.showFromResponse(
        opts.payload,
        opts.httpStatus,
        opts.fallbackMessage || opts.message || opts.errorMessage,
        opts.rawBody
      );
      return;
    }

    // 구 API(errorMessage/systemNote) → PdmgErrorPopup.show 필드명으로 정규화
    if (typeof popup.show === 'function') {
      popup.show({
        title: opts.title,
        message: opts.message || opts.errorMessage || '알 수 없는 오류가 발생했습니다.',
        code: opts.code,
        httpStatus: opts.httpStatus,
        hint: opts.hint || opts.systemNote || '',
        severity: opts.severity || 'error',
        serverMessage: opts.serverMessage || opts.errorMessage || opts.message || '',
        details: opts.details,
        stackTrace: opts.stackTrace,
        rawLog: opts.rawLog || opts.errorDetail || null
      });
      return;
    }
    if (typeof popup.showSimple === 'function') {
      popup.showSimple(opts.message || opts.errorMessage || opts);
    }
  }

  function extractErrorMessage(payload, httpStatus, fallback) {
    const result = payload && payload.result;
    return (result && (result.stdErrMsgCntn || result.errorMessage))
      || (payload && (payload.stdErrMsgCntn || payload.error || payload.message))
      || fallback
      || (httpStatus ? `HTTP ${httpStatus}` : '거래 오류');
  }

  function getSession() {
    try {
      const raw = sessionStorage.getItem(SESSION_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch (e) {
      return null;
    }
  }

  const AUTH_SKEW_MS = 30000;

  function accessTokenExpiresAt(session) {
    const s = session || getSession();
    if (!s || !s.accessToken) return null;
    const claims = decodeJwtPayload(s.accessToken);
    if (claims && claims.exp != null && !Number.isNaN(Number(claims.exp))) {
      return Number(claims.exp) * 1000;
    }
    if (s.loggedInAt != null && s.expiresIn != null) {
      const loggedInAt = Number(s.loggedInAt);
      const expiresIn = Number(s.expiresIn);
      if (!Number.isNaN(loggedInAt) && !Number.isNaN(expiresIn)) {
        return loggedInAt + expiresIn * 1000;
      }
    }
    return null;
  }

  function isAccessTokenValid(session) {
    if (window.PdmgServiceClient && typeof window.PdmgServiceClient.isAccessTokenValid === 'function') {
      return window.PdmgServiceClient.isAccessTokenValid(session === undefined ? undefined : session);
    }
    const s = session === undefined ? getSession() : session;
    if (!s || !s.accessToken) return false;
    const expAt = accessTokenExpiresAt(s);
    if (expAt == null) return true;
    return Date.now() < (expAt - AUTH_SKEW_MS);
  }

  function setSession(session) {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
  }

  function clearSession() {
    sessionStorage.removeItem(SESSION_KEY);
  }

  function syncSessionFromBody(body) {
    if (!body || !body.accessToken) return null;
    const session = {
      userId: body.userId,
      userName: body.userName,
      branchId: body.branchId,
      authGroupId: body.authGroupId,
      authGroupName: body.authGroupName,
      accessToken: body.accessToken,
      refreshToken: body.refreshToken,
      tokenType: body.tokenType || 'Bearer',
      expiresIn: body.expiresIn,
      jti: body.jti,
      issuer: body.issuer,
      audience: body.audience,
      loggedInAt: Date.now()
    };
    setSession(session);
    return session;
  }

  async function requireAuth() {
    if (location.pathname.endsWith('login.html')) return null;
    const session = getSession();
    if (isAccessTokenValid(session)) return session;
    if (window.PdmgServiceClient && typeof window.PdmgServiceClient.redirectToLogin === 'function') {
      window.PdmgServiceClient.redirectToLogin();
      return null;
    }
    clearSession();
    location.href = uiPath('/jwt/admin/login.html');
    return null;
  }

  function buildRequest(tx, dto) {
    const session = getSession();
    return {
      hdr_nhnis: {
        sys_comm: {
          std_gbl_id: newGuid(),
          rms_svc_c: tx.serviceId,
          scid: 'WEBTOP',
          optr_eno: (session && session.userId) || 'GUEST',
          tr_trm_ipadr: '127.0.0.1',
          tr_sysid: 'PDMG-UI',
          sync_dsc: 'S',
          std_tgrm_rqr_rsp_dsc: 'Q',
          tr_dtm: todaySystemDate()
        }
      },
      dto: dto || {}
    };
  }

  function extractBody(payload) {
    if (!payload || typeof payload !== 'object') return {};
    if (payload.dto && typeof payload.dto === 'object') return payload.dto;
    if (payload.body && typeof payload.body === 'object') return payload.body;
    return payload;
  }

  async function loadConfig() {
    try {
      const res = await fetch(uiPath('/api/config'));
      if (res.ok) {
        const data = await res.json();
        if (data.jwtBaseUrl) config.jwtBaseUrl = data.jwtBaseUrl;
        if (data.timeoutMs != null) config.timeoutMs = data.timeoutMs;
      }
    } catch (e) { /* default */ }
    targetUrl = resolveJwtBaseUrl();
    return config;
  }

  async function call(txKey, body, processingType) {
    const tx = typeof txKey === 'string' ? TX[txKey] : txKey;
    if (!tx || !tx.serviceId) {
      throw new Error(`거래 정의를 찾을 수 없습니다: ${txKey}`);
    }
    const base = resolveJwtBaseUrl();
    targetUrl = base;
    const request = buildRequest(tx, body || {});
    const controller = new AbortController();
    const ms = Number(config.timeoutMs) || 0;
    const timer = ms > 0 ? setTimeout(() => controller.abort(), ms) : null;
    const started = performance.now();
    let httpStatus = 0;
    let responseBody = '';
    try {
      const res = await fetch(base + '/online', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json;charset=UTF-8', Accept: 'application/json' },
        body: JSON.stringify(request),
        signal: controller.signal
      });
      httpStatus = res.status;
      responseBody = await res.text();
    } catch (error) {
      const aborted = !!(error && error.name === 'AbortError');
      httpStatus = aborted ? 504 : 502;
      responseBody = JSON.stringify({
        stdErrCode: aborted ? 'UI_TIMEOUT' : 'UI_NETWORK',
        error: aborted ? `요청 시간 초과 (${ms} ms)` : ((error && error.message) || String(error))
      });
    } finally {
      if (timer) clearTimeout(timer);
    }

    const elapsedMs = Math.round(performance.now() - started);
    const relay = { elapsedMs, httpStatus, targetUrl: base + '/online' };

    let payload;
    try {
      payload = responseBody ? JSON.parse(responseBody) : {};
    } catch (e) {
      throw new Error('응답 JSON 파싱 실패');
    }

    const hasBizError = !!(payload.result && payload.result.stdErrCode);
    if (httpStatus >= 400 || hasBizError) {
      let fallback = null;
      if (httpStatus === 502 && !hasBizError) {
        fallback = 'pdmg-jwt(8110)에 연결할 수 없습니다. pdmg-jwt를 먼저 실행하세요.';
      }
      const msg = extractErrorMessage(payload, httpStatus, fallback);
      await showErrorPopup({
        payload,
        httpStatus,
        rawBody: responseBody,
        fallbackMessage: msg,
        systemNote: 'pdmg-jwt 직접 호출'
      });
      throw new Error(msg);
    }

    const dto = extractBody(payload);
    return { payload, body: dto, httpStatus, elapsedMs, relay };
  }

  async function login(userId, password) {
    const { body } = await call('authLogin', { userId, password }, 'LOGIN');
    if (!body.accessToken) throw new Error('토큰 발급에 실패했습니다.');
    syncSessionFromBody(body);
    return body;
  }

  async function refreshTokens() {
    const session = getSession();
    if (!session || !session.refreshToken) {
      throw new Error('Refresh Token이 없습니다. 다시 로그인하세요.');
    }
    const { body } = await call('authRefresh', { refreshToken: session.refreshToken }, 'EXECUTE');
    syncSessionFromBody(body);
    return body;
  }

  async function revokeAccess(reason) {
    const session = getSession();
    if (!session || !session.accessToken) {
      throw new Error('Access Token이 없습니다.');
    }
    const token = session.tokenType ? `${session.tokenType} ${session.accessToken}` : session.accessToken;
    const { body } = await call('authRevoke', { accessToken: token, reason: reason || 'REVOKE' }, 'EXECUTE');
    return body;
  }

  async function logout() {
    const session = getSession();
    try {
      if (session && (session.accessToken || session.refreshToken)) {
        const token = session.accessToken
          ? (session.tokenType ? `${session.tokenType} ${session.accessToken}` : session.accessToken)
          : null;
        await call('authLogout', {
          accessToken: token,
          refreshToken: session.refreshToken
        }, 'EXECUTE');
      }
    } catch (e) {
      /* ignore */
    } finally {
      clearSession();
      if (isEmbedded()) {
        try {
          window.top.location.hash = '#/jwt';
          return;
        } catch (e) { /* fall through */ }
      }
      location.href = uiPath('/jwt/admin/login.html');
    }
  }

  async function fetchJwks() {
    const url = resolveJwksUrl();
    const res = await fetch(url);
    if (!res.ok) {
      throw new Error(`JWK 조회 실패 (HTTP ${res.status})`);
    }
    const data = await res.json();
    return { url, data };
  }

  function decodeJwtPayload(token) {
    if (!token) return null;
    try {
      const parts = token.split('.');
      if (parts.length < 2) return null;
      let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      const pad = base64.length % 4;
      if (pad) base64 += '='.repeat(4 - pad);
      const binary = atob(base64);
      const bytes = Uint8Array.from(binary, c => c.charCodeAt(0));
      const json = new TextDecoder('utf-8').decode(bytes);
      return JSON.parse(json);
    } catch (e) {
      return null;
    }
  }

  function formatExpiry(session) {
    if (!session || !session.loggedInAt || !session.expiresIn) return '-';
    const exp = session.loggedInAt + session.expiresIn * 1000;
    const d = new Date(exp);
    const pad = v => String(v).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  }

  async function inquiry(txKey, body) {
    return call(txKey, body, 'INQUIRY');
  }

  async function mutate(txKey, body, processingType) {
    return call(txKey, body, processingType || 'EXECUTE');
  }

  async function adminRevokeByJti(jti, reason) {
    const { body } = await mutate('tokenRevoke', { jti, reason: reason || 'ADMIN_REVOKE' }, 'EXECUTE');
    return body;
  }

  function chipForResult(status) {
    const s = String(status || '').toUpperCase();
    if (s.includes('SUCCESS') || s === 'OK' || s === 'Y') return `<span class="om-chip ok">${status}</span>`;
    if (s.includes('FAIL') || s.includes('ERROR') || s === 'N') {
      return `<span class="om-chip fail">${status}</span>`;
    }
    return `<span class="om-chip muted">${status || '-'}</span>`;
  }

  function formatTs(value) {
    if (value == null || value === '' || value === '-') return '-';
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) return String(value);
    const pad = v => String(v).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  }

  function shortJti(jti) {
    if (!jti || jti === '-') return '-';
    const s = String(jti);
    return s.length <= 20 ? s : `${s.slice(0, 8)}??{s.slice(-6)}`;
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
      nums += `<button type="button" class="om-page-btn ${i === pageNo ? 'active' : ''}" data-page="${i}">${i}</button>`;
    }
    container.innerHTML = `
      <button type="button" class="om-page-btn" data-page="prev" ${pageNo <= 1 ? 'disabled' : ''}>PREV</button>
      ${nums}
      <button type="button" class="om-page-btn" data-page="next" ${pageNo >= totalPages ? 'disabled' : ''}>NEXT</button>
      <span style="color:var(--muted);font-size:0.85rem;margin-left:8px">${pageNo} / ${totalPages} 쨌 珥?${totalCount}嫄?/span>`;
    container.querySelectorAll('[data-page]').forEach(btn => {
      btn.addEventListener('click', () => {
        const v = btn.getAttribute('data-page');
        if (v === 'prev' && pageNo > 1) onPage(pageNo - 1);
        else if (v === 'next' && pageNo < totalPages) onPage(pageNo + 1);
        else if (v !== 'prev' && v !== 'next') onPage(Number(v));
      });
    });
  }

  async function sha256Hex(value) {
    if (!value || !crypto.subtle) return null;
    const buf = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(String(value)));
    return Array.from(new Uint8Array(buf)).map(b => b.toString(16).padStart(2, '0')).join('');
  }

  function maskToken(token) {
    if (!token) return '-';
    const s = String(token);
    if (s.length <= 24) return s;
    return `${s.slice(0, 12)}…${s.slice(-8)}`;
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

  function isEmbedded() {
    try {
      return window.self !== window.top;
    } catch (e) {
      return true;
    }
  }

  function renderNavSection(items, pageId) {
    return items.map(item =>
      `<a href="${uiPath(item.href)}" class="${item.id === pageId ? 'active' : ''}">${item.label}</a>`
    ).join('');
  }

  function renderShell(pageId, title) {
    const session = getSession();
    const userLabel = session
      ? `${session.userName || session.userId} (${session.userId})`
      : '';
    const embed = isEmbedded();
    document.body.innerHTML = `
      <div class="om-admin${embed ? ' om-admin--embed' : ''}">
        ${embed ? '' : `
        <aside class="om-sidebar">
          <div class="om-brand">
            <h1>PDMG JWT</h1>
            <p>JWT 인증·토큰 관리 · pdmg-jwt</p>
          </div>
          <nav class="om-nav">
            <div class="om-nav-section">
              <div class="om-nav-label">인증</div>
              ${renderNavSection(NAV, pageId)}
            </div>
          </nav>
          <div class="om-nav-footer">
            <a href="${uiPath('/index.html')}#/home">← PDMG UI</a>
          </div>
        </aside>`}
        <main class="om-main">
          <header class="om-topbar">
            <h2>${title}</h2>
            <div class="om-topbar-meta">
              ${userLabel ? `<span class="om-user-badge">${userLabel}</span>` : ''}
              <button type="button" class="btn-secondary om-logout-btn" id="jwtLogoutBtn">로그아웃</button>
              <span id="jwtTargetUrl" title="pdmg-jwt URL">${targetUrl}</span>
            </div>
          </header>
          <div class="om-content" id="jwtContent">
            <div class="om-empty">불러오는 중...</div>
          </div>
        </main>
      </div>`;
    const logoutBtn = document.getElementById('jwtLogoutBtn');
    if (logoutBtn) logoutBtn.addEventListener('click', () => logout());
    return document.getElementById('jwtContent');
  }

  function showError(container, message) {
    container.innerHTML = `<div class="om-alert error">${message}<br><small>대상: <code>${targetUrl || resolveJwtBaseUrl()}</code></small></div>`;
  }

  async function pingBackend() {
    try {
      const res = await fetch(resolveJwksUrl());
      targetUrl = resolveJwtBaseUrl();
      const el = document.getElementById('jwtTargetUrl');
      if (el) el.textContent = targetUrl;
      return res.ok;
    } catch (e) {
      return false;
    }
  }

  async function initPage(pageId, title, renderFn) {
    const session = await requireAuth();
    if (!session) return;
    await loadConfig();
    const container = renderShell(pageId, title);
    const ok = await pingBackend();
    if (!ok) {
      showError(container, 'pdmg-jwt JWKS에 연결할 수 없습니다. 포트 8110 기동을 확인하세요.');
      return;
    }
    try {
      await renderFn(container);
    } catch (err) {
      await showErrorPopup({
        message: err.message || String(err),
        errorDetail: err.stack || '',
        systemNote: 'JWT Admin 화면'
      });
      showError(container, err.message || String(err));
    }
  }

  return {
    NAV, TX, config, targetUrl, SESSION_KEY, BUSINESS_CODE,
    uiPath, field, getSession, setSession, clearSession, syncSessionFromBody,
    isAccessTokenValid, accessTokenExpiresAt,
    loadConfig, login, logout, refreshTokens, revokeAccess, fetchJwks,
    decodeJwtPayload, formatExpiry, maskToken, sha256Hex, formatTs, shortJti, chipForResult,
    resolveJwksUrl, resolveJwtBaseUrl,
    call, inquiry, mutate, adminRevokeByJti, initPage, renderShell, renderPagination,
    showError, showErrorPopup, pingBackend
  };
})();
