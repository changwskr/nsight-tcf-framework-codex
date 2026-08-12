/**
 * pdmg-ui context path helper (tcf-ui ui-context.js 정렬).
 * bootRun(루트)에서는 no-op, /ui 컨텍스트 배포 시 접두사를 붙인다.
 */
(function () {
  if (window.__NSIGHT_UI_CONTEXT_INIT__) {
    return;
  }
  window.__NSIGHT_UI_CONTEXT_INIT__ = true;

  const uiContext = location.pathname.startsWith('/ui/') || location.pathname === '/ui' ? '/ui' : '';
  window.__NSIGHT_UI_CTX__ = uiContext;

  window.nsightUiUrl = function nsightUiUrl(path) {
    if (!path) {
      return uiContext || '/';
    }
    const normalized = path.startsWith('/') ? path : '/' + path;
    if (uiContext && (normalized === uiContext || normalized.startsWith(uiContext + '/'))) {
      return normalized;
    }
    return uiContext + normalized;
  };

  /* 셸 iframe 안에서는 임베드 스타일 적용 */
  if (window.self !== window.top) {
    document.documentElement.classList.add('pdmg-embed');
    document.addEventListener('click', (event) => {
      const anchor = event.target.closest('a[href]');
      if (!anchor) {
        return;
      }
      const href = anchor.getAttribute('href') || '';
      const goesHome = href === '/'
          || href === './'
          || /(^|\/)index\.html(#|$)/.test(href);
      const staysInApp = /\/(mgcoa|imagelog|txparam)\//.test(href);
      if (goesHome && !staysInApp) {
        event.preventDefault();
        try {
          window.parent.location.assign(nsightUiUrl('/index.html') + '#view=dashboard');
        } catch (_e) {
          /* ignore */
        }
      }
    }, true);
    return;
  }

  /* 단독 진입 시 좌측 메뉴 셸로 합류 */
  const path = location.pathname || '';
  const viewMap = {
    '/mgcoa5530/index.html': 'mgcoa5530',
    '/mgcoa8888/index.html': 'mgcoa8888',
    '/mgcoa9000/index.html': 'mgcoa9000',
    '/mgcoa9999/index.html': 'mgcoa9999',
    '/imagelog/index.html': 'imagelog',
    '/txparam/index.html': 'txparam'
  };
  const matched = Object.keys(viewMap).find((key) => path.endsWith(key) || path.endsWith(key.replace('/index.html', '')));
  if (matched) {
    const home = (uiContext || '') + '/index.html#view=' + viewMap[matched];
    location.replace(home);
  }
})();
