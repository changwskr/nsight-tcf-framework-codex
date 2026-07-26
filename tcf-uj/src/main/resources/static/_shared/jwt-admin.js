/**
 * NSIGHT JWT 인증 포털 공통 유틸 (tcf-uj)
 * tcf-jwt API를 tcf-gateway 경유 /api/relay/JWT/online 으로 호출합니다.
 */
window.JwtAdmin = (function () {
  const BUSINESS_CODE = 'JWT';
  const SESSION_KEY = 'nsight.jwt.session';
  const LOCAL_PORT = 8110;
  const CONTEXT_PATH = '/jwt';

  const NAV = [
    { id: 'token', label: '토큰 현황', href: '/jwt/admin/token.html' },
    { id: 'login-history', label: '로그인 이력', href: '/jwt/admin/login-history.html' },
    { id: 'refresh-token', label: 'Refresh Token', href: '/jwt/admin/refresh-token.html' },
    { id: 'security-policy', label: '보안정책', href: '/jwt/admin/security-policy.html' },
    { id: 'jwks', label: 'JWK 공개키', href: '/jwt/admin/jwks.html' }
  ];

  const TX = {
    authLogin: { serviceId: 'JWT.Auth.login', transactionCode: 'JWT-AUT-0001', serviceName: 'JWT 로그인' },
    authRefresh: { serviceId: 'JWT.Auth.refresh', transactionCode: 'JWT-AUT-0002', serviceName: 'Refresh 갱신' },
    authRevoke: { serviceId: 'JWT.Auth.revoke', transactionCode: 'JWT-AUT-0003', serviceName: 'Access 폐기' },
    authLogout: { serviceId: 'JWT.Auth.logout', transactionCode: 'JWT-AUT-0004', serviceName: 'JWT 로그아웃' },
    tokenInquiry: { serviceId: 'JWT.Token.inquiry', transactionCode: 'JWT-TKN-0001', serviceName: '토큰 현황 조회' },
    tokenRevoke: { serviceId: 'JWT.Token.revoke', transactionCode: 'JWT-TKN-0002', serviceName: '토큰 강제폐기' },
    loginHistory: { serviceId: 'JWT.LoginHistory.inquiry', transactionCode: 'JWT-LGH-0001', serviceName: '로그인 이력 조회' },
    refreshTokenInquiry: { serviceId: 'JWT.RefreshToken.inquiry', transactionCode: 'JWT-RTK-0001', serviceName: 'Refresh Token 조회' },
    securityPolicyInquiry: { serviceId: 'JWT.SecurityPolicy.inquiry', transactionCode: 'JWT-SCP-0001', serviceName: '보안정책 조회' },
    securityPolicyUpdate: { serviceId: 'JWT.SecurityPolicy.update', transactionCode: 'JWT-SCP-0002', serviceName: '보안정책 수정' }
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

  function resolveJwtBaseUrl() {
    if (isTomcatUiDeployment()) {
      return (config.tomcatGatewayUrl || 'http://localhost:8080').replace(/\/$/, '') + CONTEXT_PATH;
    }
    return (config.bootrunHost || 'http://127.0.0.1').replace(/\/$/, '') + ':' + LOCAL_PORT;
  }

  function resolveJwksUrl() {
    return resolveJwtBaseUrl() + '/.well-known/jwks.json';
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

  async function showErrorPopup(info) {
    const popup = await ensureErrorPopupReady();
    if (popup) popup.show(info);
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

  function getSession() {
    try {
      const raw = sessionStorage.getItem(SESSION_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch (e) {
      return null;
    }
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
    if (session && session.accessToken) return session;
    location.href = uiPath('/jwt/admin/login.html');
    return null;
  }

  function buildHeader(tx, processingType) {
    const session = getSession();
    return {
      systemId: 'NSIGHT-AUTH',
      businessCode: BUSINESS_CODE,
      serviceId: tx.serviceId,
      transactionCode: tx.transactionCode,
      serviceName: tx.serviceName || '',
      processingType: (processingType || 'INQUIRY').toUpperCase(),
      guid: newGuid(),
      traceId: '',
      channelId: 'WEBTOP',
      userId: session && session.userId ? session.userId : 'GUEST',
      branchId: session && session.branchId ? session.branchId : '',
      requestTime: nowIsoKst(),
      systemDate: todaySystemDate(),
      bizDate: todaySystemDate(),
      clientIp: '127.0.0.1'
    };
  }

  async function loadConfig() {
    const res = await fetch(uiPath('/api/config'));
    if (res.ok) {
      const data = await res.json();
      config.deploymentMode = data.deploymentMode || config.deploymentMode;
      config.bootrunHost = data.bootrunHost || config.bootrunHost;
      config.tomcatGatewayUrl = data.tomcatGatewayUrl || config.tomcatGatewayUrl;
    }
    const urlRes = await fetch(uiPath(`/api/business-modules/${BUSINESS_CODE}/target-url?${buildRelayQuery()}`));
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
          ? 'tcf-jwt(/jwt)에 연결할 수 없습니다. Tomcat 기동 상태를 확인하세요.'
          : 'tcf-jwt(8110)에 연결할 수 없습니다. tcf-jwt를 먼저 실행하세요.';
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
      location.href = uiPath('/jwt/admin/login.html');
    }
  }

  async function fetchJwks() {
    const res = await relayFetch(`/api/jwt/jwks?${buildRelayQuery()}`);
    if (!res.ok) {
      let msg = `JWK 조회 실패 (HTTP ${res.status})`;
      try {
        const err = await res.json();
        if (err.error) msg = err.error;
      } catch (e) { /* ignore */ }
      throw new Error(msg);
    }
    const payload = await res.json();
    return { url: payload.url || resolveJwksUrl(), data: payload.jwks || payload.data || payload };
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
    return s.length <= 20 ? s : `${s.slice(0, 8)}…${s.slice(-6)}`;
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
      <span style="color:var(--muted);font-size:0.85rem;margin-left:8px">${pageNo} / ${totalPages} · 총 ${totalCount}건</span>`;
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
    document.body.innerHTML = `
      <div class="om-admin">
        <aside class="om-sidebar">
          <div class="om-brand">
            <h1>NSIGHT JWT</h1>
            <p>JWT 인증·토큰 관리 · tcf-jwt</p>
          </div>
          <nav class="om-nav">
            <div class="om-nav-section">
              <div class="om-nav-label">인증</div>
              ${renderNavSection(NAV, pageId)}
            </div>
          </nav>
          <div class="om-nav-footer">
            <a href="${uiPath('/jwt/index-multi.html')}">↗ API 거래 테스트</a>
            <a href="${uiPath('/index.html')}">← TCF UI 홈</a>
          </div>
        </aside>
        <main class="om-main">
          <header class="om-topbar">
            <h2>${title}</h2>
            <div class="om-topbar-meta">
              ${userLabel ? `<span class="om-user-badge">${userLabel}</span>` : ''}
              <button type="button" class="btn-secondary om-logout-btn" id="jwtLogoutBtn">로그아웃</button>
              <span id="jwtTargetUrl" title="tcf-jwt URL">${targetUrl}</span>
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
    const hint = targetUrl && targetUrl !== '-'
      ? `릴레이 대상: <code>${targetUrl}</code>`
      : isTomcatUiDeployment()
      ? 'Tomcat(8080)에서 /jwt, /uj WAR 배포를 확인하세요.'
      : 'tcf-jwt(포트 8110)와 tcf-uj(8102)를 함께 기동했는지 확인하세요.';
    container.innerHTML = `<div class="om-alert error">${message}<br><small>${hint}</small></div>`;
  }

  async function pingBackend() {
    try {
      const res = await fetch(uiPath(`/api/business-modules/${BUSINESS_CODE}/target-url?${buildRelayQuery()}`));
      if (!res.ok) return false;
      const data = await res.json();
      targetUrl = data.targetUrl || targetUrl;
      const el = document.getElementById('jwtTargetUrl');
      if (el) el.textContent = targetUrl;
      return true;
    } catch (e) {
      return false;
    }
  }

  async function initPage(pageId, title, renderFn) {
    if (!window.__NSIGHT_UJ_CONTEXT_INIT__) {
      const script = document.createElement('script');
      script.src = uiContextPrefix() + '/_shared/uj-context.js';
      document.head.appendChild(script);
    }
    const session = await requireAuth();
    if (!session) return;
    await loadConfig();
    const container = renderShell(pageId, title);
    const ok = await pingBackend();
    if (!ok) {
      showError(container, 'tcf-uj → tcf-jwt 릴레이 URL을 확인할 수 없습니다.');
      return;
    }
    try {
      await renderFn(container);
    } catch (err) {
      showErrorPopup({
        errorMessage: err.message || String(err),
        errorDetail: err.stack || '',
        systemNote: 'JWT Admin 화면'
      });
      showError(container, err.message || String(err));
    }
  }

  return {
    NAV, TX, config, targetUrl, SESSION_KEY, BUSINESS_CODE,
    uiPath, field, getSession, setSession, clearSession, syncSessionFromBody,
    loadConfig, login, logout, refreshTokens, revokeAccess, fetchJwks,
    decodeJwtPayload, formatExpiry, maskToken, sha256Hex, formatTs, shortJti, chipForResult,
    resolveJwksUrl, resolveJwtBaseUrl,
    call, inquiry, mutate, adminRevokeByJti, initPage, renderShell, renderPagination,
    showError, showErrorPopup, pingBackend,
    isTomcatUiDeployment, buildRelayQuery
  };
})();
