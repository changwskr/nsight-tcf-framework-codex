/**
 * NSIGHT OC ENV 화면 공통 레이아웃 (env-001 ~ rule-check)
 */
(function () {
    const NAV = [
        { id: 'env001', href: '/oc/env-001.html', label: 'ENV-001 대시' },
        { id: 'env002', href: '/oc/env-002.html', label: 'ENV-002 조건' },
        { id: 'env003', href: '/oc/env-003.html', label: 'ENV-003 TPS·VM' },
        { id: 'env004', href: '/oc/env-004.html', label: 'ENV-004 계층' },
        { id: 'check', href: '/oc/check.html', label: '종합 보고서' },
        { id: 'rulecheck', href: '/oc/rule-check.html', label: 'Rule 점검' }
    ];

    function navHtml(active) {
        return NAV.map(item => {
            const cls = item.id === active ? ' active' : '';
            return `<a href="${item.href}" class="env-sub-nav__link${cls}">${item.label}</a>`;
        }).join('');
    }

    function mount() {
        const body = document.body;
        const shell = document.getElementById('envPageShell');
        if (!shell) return;

        const active = body.dataset.envPage || '';
        const subtitle = body.dataset.envSubtitle || '';
        const title = body.dataset.envTitle || 'NSIGHT OC ENV';

        shell.innerHTML = `
<header class="tcf-env-topbar">
  <div class="tcf-env-topbar__brand">
    <h1>NSIGHT OC · 환경설정 산정·점검</h1>
    <p>${subtitle}</p>
  </div>
  <nav class="tcf-env-topbar__nav" aria-label="TCF UI">
    <a class="tcf-env-topbar__link" href="/index.html">TCF UI</a>
    <a class="tcf-env-topbar__link" href="/index.html#capacitySection">용량 산정</a>
    <span class="badge">ENV</span>
  </nav>
</header>
<nav class="env-sub-nav" aria-label="ENV 화면 이동">${navHtml(active)}</nav>`;

        document.title = title;
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', mount);
    } else {
        mount();
    }
})();
