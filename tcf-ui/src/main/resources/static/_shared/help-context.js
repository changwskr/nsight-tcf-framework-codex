/**
 * 화면별 HELP 컨텍스트 — data-help-doc / screen-map / business-map 딥링크
 */
(function () {
  'use strict';

  const SKIP_BODY = new Set(['tcf-help-viewer', 'tcf-help-library', 'tcf-help-health']);

  function uiUrl(path) {
    return typeof window.nsightUiUrl === 'function' ? window.nsightUiUrl(path) : path;
  }

  function currentPath() {
    let p = location.pathname || '/';
    if (p.startsWith('/ui/')) p = p.slice(3) || '/';
    else if (p === '/ui') p = '/';
    return p;
  }

  let screenMap = null;
  let businessMap = null;
  let cachedDoc = undefined;

  async function loadScreenMap() {
    if (screenMap) return screenMap;
    try {
      const res = await fetch(uiUrl('/help/help-screen-map.json'));
      screenMap = res.ok ? await res.json() : { screens: {} };
    } catch {
      screenMap = { screens: {} };
    }
    return screenMap;
  }

  async function loadBusinessMap() {
    if (businessMap) return businessMap;
    try {
      const res = await fetch(uiUrl('/help/help-business-map.json'));
      businessMap = res.ok ? await res.json() : { screens: {}, patterns: [] };
    } catch {
      businessMap = { screens: {}, patterns: [] };
    }
    return businessMap;
  }

  function resolveFromAttr() {
    const body = document.body;
    if (!body) return null;
    const docId = body.dataset.helpDoc;
    const src = body.dataset.helpSrc;
    const title = body.dataset.helpTitle;
    if (src) {
      return { src, title: title || '관련 문서', source: 'attr-src' };
    }
    if (docId) {
      return { docId, title: title || '화면 도움말', source: 'attr-doc' };
    }
    return null;
  }

  function matchBusinessPattern(path, patterns) {
    for (const p of patterns || []) {
      try {
        if (new RegExp(p.match).test(path)) {
          return {
            docId: p.docId,
            title: p.title,
            src: p.src || null,
            source: 'business-pattern'
          };
        }
      } catch { /* ignore bad regex */ }
    }
    return null;
  }

  async function resolveContextDoc() {
    if (cachedDoc !== undefined) return cachedDoc;

    const explicit = resolveFromAttr();
    if (explicit) {
      cachedDoc = explicit;
      return cachedDoc;
    }

    const path = currentPath();
    const native = await loadScreenMap();
    const nativeHit = native.screens?.[path];
    if (nativeHit) {
      cachedDoc = { ...nativeHit, source: 'screen' };
      return cachedDoc;
    }

    const business = await loadBusinessMap();
    const bizHit = business.screens?.[path];
    if (bizHit) {
      cachedDoc = { ...bizHit, source: 'business' };
      return cachedDoc;
    }

    const patHit = matchBusinessPattern(path, business.patterns);
    cachedDoc = patHit || null;
    return cachedDoc;
  }

  function contextualHelpUrl(doc) {
    if (doc.src) {
      return uiUrl('/help/view.html?src=' + encodeURIComponent(doc.src));
    }
    if (doc.external) {
      return uiUrl(doc.external);
    }
    if (doc.docId) {
      return uiUrl('/help/view.html?doc=' + encodeURIComponent(doc.docId));
    }
    return uiUrl('/help.html');
  }

  function createContextButton(doc) {
    const a = document.createElement('a');
    a.className = 'help-context-btn';
    a.href = contextualHelpUrl(doc);
    a.setAttribute('aria-label', (doc.title || '화면 도움말') + ' 열기');
    a.title = doc.title || '이 화면 도움말';
    a.innerHTML = '<span class="help-context-btn__icon" aria-hidden="true">?</span><span class="help-context-btn__label">도움말</span>';
    return a;
  }

  function mountInNav(navSelector) {
    resolveContextDoc().then(doc => {
      if (!doc) return;
      const nav = typeof navSelector === 'string'
        ? document.querySelector(navSelector)
        : navSelector;
      if (!nav || nav.querySelector('.help-context-btn')) return;
      const btn = createContextButton(doc);
      const helpLink = nav.querySelector('a[href="/help.html"], a[href$="/help.html"]');
      if (helpLink) {
        nav.insertBefore(btn, helpLink);
      } else {
        nav.appendChild(btn);
      }
      upgradeHelpLinks(doc, nav);
    });
  }

  function upgradeHelpLinks(doc, root) {
    const scope = root || document;
    const url = contextualHelpUrl(doc);
    scope.querySelectorAll('a[href="/help.html"]').forEach(link => {
      if (link.classList.contains('help-context-btn')) return;
      if (link.closest('.help-context-skip')) return;
      if (!link.dataset.helpContextApplied) {
        link.href = uiUrl(url);
        link.dataset.helpContextApplied = '1';
        link.classList.add('help-context-link');
        if (link.textContent.trim() === 'HELP') {
          link.textContent = '화면 도움말';
        }
      }
    });
  }

  function mountFloating(doc) {
    if (document.querySelector('.help-context-btn--float')) return;
    const btn = createContextButton(doc);
    btn.classList.add('help-context-btn--float');
    document.body.appendChild(btn);
  }

  const NAV_SELECTORS = [
    '.tcf-topbar__nav',
    '.tcf-env-topbar__nav',
    '.om-topbar-meta',
    '.nav-links'
  ];

  function autoMount() {
    const body = document.body;
    if (!body) return;
    for (const cls of SKIP_BODY) {
      if (body.classList.contains(cls)) return;
    }
    let attempts = 0;
    function tryMount() {
      for (const sel of NAV_SELECTORS) {
        const nav = document.querySelector(sel);
        if (nav) {
          mountInNav(nav);
          return;
        }
      }
      if (attempts >= 24) {
        resolveContextDoc().then(doc => {
          if (doc) mountFloating(doc);
        });
        return;
      }
      attempts += 1;
      setTimeout(tryMount, 50);
    }
    tryMount();
  }

  window.NsightHelpContext = {
    resolveContextDoc,
    contextualHelpUrl,
    mountInNav,
    autoMount
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', autoMount);
  } else {
    autoMount();
  }
})();
