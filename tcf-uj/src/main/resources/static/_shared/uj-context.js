/**
 * tcf-uj.war (/uj) 배포 시 절대 경로 API·리소스에 context 접두사를 붙입니다.
 * bootRun(루트 context)에서는 no-op 입니다.
 */
(function () {
  if (window.__NSIGHT_UJ_CONTEXT_INIT__) {
    return;
  }
  window.__NSIGHT_UJ_CONTEXT_INIT__ = true;

  const uiContext = location.pathname.startsWith('/uj/') || location.pathname === '/uj' ? '/uj' : '';
  window.__NSIGHT_UJ_CTX__ = uiContext;

  window.nsightUjUrl = function nsightUjUrl(path) {
    if (!path) {
      return uiContext;
    }
    const normalized = path.startsWith('/') ? path : '/' + path;
    if (uiContext && (normalized === uiContext || normalized.startsWith(uiContext + '/'))) {
      return normalized;
    }
    return uiContext + normalized;
  };

  function injectErrorPopup() {
    if (window.NsightErrorPopup || document.querySelector('script[data-nsight-error-popup]')) {
      return;
    }
    const script = document.createElement('script');
    script.src = uiContext + '/_shared/error-popup.js';
    script.defer = true;
    script.setAttribute('data-nsight-error-popup', '');
    document.head.appendChild(script);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', injectErrorPopup);
  } else {
    injectErrorPopup();
  }

  if (!uiContext) {
    return;
  }

  const originalFetch = window.fetch.bind(window);
  window.fetch = function patchedFetch(input, init) {
    if (typeof input === 'string' && input.startsWith('/') && !input.startsWith('//')) {
      if (input !== uiContext && !input.startsWith(uiContext + '/')) {
        input = uiContext + input;
      }
    }
    return originalFetch(input, init);
  };
})();
