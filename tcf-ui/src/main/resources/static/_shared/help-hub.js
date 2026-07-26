/**
 * HELP 허브 — help-index.json + 문서 라이브러리 진입
 */
(function () {
  function uiUrl(path) {
    return typeof window.nsightUiUrl === 'function' ? window.nsightUiUrl(path) : path;
  }

  function escapeHtml(t) {
    const d = document.createElement('div');
    d.textContent = t ?? '';
    return d.innerHTML;
  }

  document.querySelectorAll('[data-help-ui-link]').forEach(el => {
    const path = el.getAttribute('href');
    if (path) el.setAttribute('href', uiUrl(path));
  });

  async function renderHubIndex() {
    const root = document.getElementById('helpIndexRoot');
    if (!root) return;
    try {
      const res = await fetch(uiUrl('/help/help-index.json'));
      if (!res.ok) throw new Error('help-index.json');
      const index = await res.json();
      let catalogTotal = '';
      try {
        const catRes = await fetch(uiUrl('/help/doc-catalog.json'));
        if (catRes.ok) catalogTotal = (await catRes.json()).total;
      } catch (e) { /* ignore */ }

      const libraryBlock = `
        <section class="help-index-section">
          <h3 class="help-index-section__title">문서 라이브러리</h3>
          <p class="help-index-section__desc">저장소 전체 마크다운 <strong>${catalogTotal || '600+'}</strong>편 · 코퍼스별 검색</p>
          <div class="help-index-grid">
            <a class="help-index-card" href="${uiUrl('/help/library.html')}">
              <strong>전체 문서 라이브러리</strong>
              <span>검색 · 필터 · 원문 읽기</span>
            </a>
            <a class="help-index-card" href="${uiUrl('/help/view.html?doc=doc-map')}">
              <strong>문서 지도</strong>
              <span>읽는 순서 · 코퍼스 안내</span>
            </a>
          </div>
        </section>`;

      const sections = (index.sections || []).map(section => `
        <section class="help-index-section">
          <h3 class="help-index-section__title">${escapeHtml(section.title)}</h3>
          ${section.description ? `<p class="help-index-section__desc">${escapeHtml(section.description)}</p>` : ''}
          <div class="help-index-grid">
            ${(section.items || []).map(item => {
              const href = item.external
                ? uiUrl(item.external)
                : uiUrl('/help/view.html?doc=' + encodeURIComponent(item.id));
              return `
              <a class="help-index-card" href="${href}">
                <strong>${escapeHtml(item.title)}</strong>
                <span>${escapeHtml((item.tags || []).join(' · ') || item.file || '')}</span>
              </a>`;
            }).join('')}
          </div>
        </section>`).join('');

      root.innerHTML = libraryBlock + sections;
    } catch (e) {
      root.innerHTML = `<p style="color:var(--muted)">문서 목록을 불러오지 못했습니다. <code>gradle :tcf-help:exportHelp :tcf-ui:processResources</code> 실행 후 새로고침하세요.</p>`;
    }
  }

  renderHubIndex();
})();
