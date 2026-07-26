/**
 * HELP 품질 리포트 — help-link-report.json
 */
(function () {
  'use strict';

  function uiUrl(path) {
    return typeof window.nsightUiUrl === 'function' ? window.nsightUiUrl(path) : path;
  }

  function escapeHtml(t) {
    const d = document.createElement('div');
    d.textContent = t ?? '';
    return d.innerHTML;
  }

  function setNavLinks() {
    document.querySelectorAll('[data-help-ui-link]').forEach(el => {
      const path = el.getAttribute('href');
      if (path) el.setAttribute('href', uiUrl(path));
    });
  }

  function statCard(label, value, tone) {
    return `<div class="help-health-stat help-health-stat--${tone || 'neutral'}">
      <span class="help-health-stat__value">${escapeHtml(String(value))}</span>
      <span class="help-health-stat__label">${escapeHtml(label)}</span>
    </div>`;
  }

  function renderSummary(report, catalog) {
    const el = document.getElementById('helpHealthSummary');
    if (!el) return;
    const broken = report.links?.broken || 0;
    const unmapped = report.screens?.unmapped || 0;
    const linkTone = broken === 0 ? 'ok' : 'warn';
    const screenTone = unmapped === 0 ? 'ok' : 'neutral';
    el.innerHTML = `
      ${statCard('스캔 링크', report.links?.scanned || 0, 'neutral')}
      ${statCard('깨진 링크', broken, linkTone)}
      ${statCard('HTML 화면', report.screens?.totalHtml || 0, 'neutral')}
      ${statCard('HELP 매핑', report.screens?.mapped || 0, 'ok')}
      ${statCard('미매핑', unmapped, screenTone)}
      ${statCard('수동 보강', report.screens?.manualOverrides || 0, 'neutral')}
      ${catalog ? statCard('문서 카탈로그', catalog.total, 'neutral') : ''}`;
  }

  function renderBroken(report) {
    const body = document.querySelector('#helpHealthBroken tbody');
    if (!body) return;
    const rows = report.broken || [];
    if (!rows.length) {
      body.innerHTML = '<tr><td colspan="4" class="help-viewer-empty">깨진 링크 없음 ✓</td></tr>';
      return;
    }
    body.innerHTML = rows.map(r => `
      <tr>
        <td><code>${escapeHtml(r.file)}</code></td>
        <td>${r.line}</td>
        <td class="help-health-table__href"><code>${escapeHtml(r.href)}</code></td>
        <td>${escapeHtml(r.reason)}</td>
      </tr>`).join('');
  }

  function renderUnmapped(report) {
    const body = document.querySelector('#helpHealthUnmapped tbody');
    if (!body) return;
    const rows = report.unmappedScreens || [];
    if (!rows.length) {
      body.innerHTML = '<tr><td colspan="2" class="help-viewer-empty">미매핑 화면 없음 ✓</td></tr>';
      return;
    }
    body.innerHTML = rows.map(r => `
      <tr>
        <td><code>${escapeHtml(r.screen)}</code></td>
        <td>${escapeHtml(r.reason)}</td>
      </tr>`).join('');
  }

  async function init() {
    setNavLinks();
    try {
      const [reportRes, catalogRes] = await Promise.all([
        fetch(uiUrl('/help/help-link-report.json')),
        fetch(uiUrl('/help/doc-catalog.json'))
      ]);
      if (!reportRes.ok) throw new Error('help-link-report.json');
      const report = await reportRes.json();
      const catalog = catalogRes.ok ? await catalogRes.json() : null;
      const sub = document.getElementById('helpHealthSubtitle');
      if (sub) {
        sub.textContent = `갱신 ${(report.generatedAt || '').slice(0, 19).replace('T', ' ')} · gradle :tcf-help:verifyHelp`;
      }
      renderSummary(report, catalog);
      renderBroken(report);
      renderUnmapped(report);
      const foot = document.getElementById('helpHealthFooter');
      if (foot) {
        foot.textContent = report.unmappedTruncated
          ? `미매핑 ${report.screens?.unmapped}건 중 200건만 표시 · help-screen-overrides.yml 로 보강`
          : 'help-screen-overrides.yml · catalog-overrides.yml 로 수동 보강 가능';
      }
    } catch (e) {
      const body = document.querySelector('#helpHealthBroken tbody');
      if (body) {
        body.innerHTML = `<tr><td colspan="4" class="help-viewer-empty">리포트 로드 실패. gradle :tcf-help:verifyHelp 실행 후 새로고침.</td></tr>`;
      }
    }
  }

  init();
})();
