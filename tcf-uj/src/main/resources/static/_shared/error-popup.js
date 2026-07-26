/**
 * NSIGHT TCF UI — 공통 오류 팝업 (작은 플로팅 패널)
 */
window.NsightErrorPopup = (function () {
  let rootEl = null;

  function uiPrefix() {
    if (typeof window.nsightUjUrl === 'function') {
      return window.__NSIGHT_UJ_CTX__ || '';
    }
    if (location.pathname.startsWith('/uj/') || location.pathname === '/uj') {
      return '/uj';
    }
    return '';
  }

  function esc(value) {
    return String(value == null ? '' : value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function pick(obj, ...keys) {
    if (!obj) return '';
    for (const key of keys) {
      const v = obj[key];
      if (v != null && String(v).trim() !== '') return String(v).trim();
    }
    return '';
  }

  function ensureDom() {
    if (rootEl) return rootEl;
    rootEl = document.createElement('aside');
    rootEl.id = 'nsightErrorPopup';
    rootEl.className = 'nsight-error-popup';
    rootEl.setAttribute('role', 'alertdialog');
    rootEl.setAttribute('aria-live', 'assertive');
    rootEl.hidden = true;
    document.body.appendChild(rootEl);
    return rootEl;
  }

  function normalize(info) {
    const o = info || {};
    const result = o.result || {};
    return {
      title: o.title || '오류 발생',
      resultCode: pick(o, 'resultCode') || pick(result, 'resultCode') || '-',
      errorCode: pick(o, 'errorCode') || pick(result, 'errorCode') || '-',
      errorMessage: pick(o, 'errorMessage', 'message')
        || pick(result, 'errorMessage', 'resultMessage')
        || '알 수 없는 오류가 발생했습니다.',
      errorDetail: pick(o, 'errorDetail') || pick(result, 'errorDetail') || '',
      errorSystemId: pick(o, 'errorSystemId') || pick(result, 'errorSystemId') || '-',
      errorDateTime: pick(o, 'errorDateTime') || pick(result, 'errorDateTime') || '',
      httpStatus: o.httpStatus != null ? String(o.httpStatus) : '',
      serviceId: pick(o, 'serviceId') || pick(o.header, 'serviceId') || '',
      traceId: pick(o, 'traceId') || pick(o.header, 'traceId') || '',
      targetUrl: pick(o, 'targetUrl') || '',
      systemNote: pick(o, 'systemNote', 'systemInfo') || ''
    };
  }

  function render(info) {
    const n = normalize(info);
    const detailBlock = n.errorDetail
      ? `<div class="nsight-error-row"><span class="label">예외상세</span><div class="value"><div class="nsight-error-detail">${esc(n.errorDetail)}</div></div></div>`
      : '';
    const systemBlock = n.systemNote
      ? `<div class="nsight-error-row"><span class="label">시스템</span><span class="value message">${esc(n.systemNote)}</span></div>`
      : '';
    const meta = [
      n.httpStatus ? `HTTP ${n.httpStatus}` : '',
      n.serviceId ? `serviceId ${n.serviceId}` : '',
      n.traceId ? `trace ${n.traceId}` : ''
    ].filter(Boolean).join(' · ');

    ensureDom().innerHTML = `
      <div class="nsight-error-popup-head">
        <strong>${esc(n.title)}</strong>
        <button type="button" class="nsight-error-popup-close" aria-label="닫기">&times;</button>
      </div>
      <div class="nsight-error-popup-body">
        <div class="nsight-error-row"><span class="label">결과코드</span><span class="value">${esc(n.resultCode)}</span></div>
        <div class="nsight-error-row"><span class="label">오류코드</span><span class="value">${esc(n.errorCode)}</span></div>
        <div class="nsight-error-row"><span class="label">오류메시지</span><span class="value message">${esc(n.errorMessage)}</span></div>
        <div class="nsight-error-row"><span class="label">시스템ID</span><span class="value">${esc(n.errorSystemId)}</span></div>
        ${n.errorDateTime ? `<div class="nsight-error-row"><span class="label">발생시각</span><span class="value">${esc(n.errorDateTime)}</span></div>` : ''}
        ${meta ? `<div class="nsight-error-row"><span class="label">요청정보</span><span class="value">${esc(meta)}</span></div>` : ''}
        ${n.targetUrl ? `<div class="nsight-error-row"><span class="label">대상URL</span><span class="value">${esc(n.targetUrl)}</span></div>` : ''}
        ${detailBlock}
        ${systemBlock}
      </div>
      <div class="nsight-error-popup-foot">
        <button type="button" data-action="close">닫기</button>
        <button type="button" class="primary" data-action="detail">상세 페이지</button>
      </div>`;

    rootEl.querySelector('.nsight-error-popup-close').addEventListener('click', hide);
    rootEl.querySelector('[data-action="close"]').addEventListener('click', hide);
    rootEl.querySelector('[data-action="detail"]').addEventListener('click', () => {
      openDetailPage(n);
    });
    rootEl.hidden = false;
  }

  function hide() {
    if (rootEl) rootEl.hidden = true;
  }

  function openDetailPage(info) {
    try {
      sessionStorage.setItem('nsight.error.popup.last', JSON.stringify(info));
    } catch (e) {
      /* ignore */
    }
    const url = uiPrefix() + '/error-popup.html';
    window.open(url, 'nsightErrorDetail', 'width=520,height=640,scrollbars=yes');
  }

  function fromPayload(payload, relay, fallbackMessage) {
    const result = payload?.result || {};
    return {
      result: result,
      header: payload?.header,
      errorMessage: fallbackMessage,
      httpStatus: relay?.httpStatus,
      targetUrl: relay?.targetUrl,
      serviceId: payload?.header?.serviceId,
      traceId: payload?.header?.traceId
    };
  }

  function fromError(error, extra) {
    const e = error || {};
    return {
      ...(extra || {}),
      errorMessage: e.message || String(e),
      errorDetail: e.stack || pick(extra, 'errorDetail'),
      systemNote: pick(extra, 'systemNote') || 'JavaScript 예외'
    };
  }

  function show(info) {
    render(info);
    return info;
  }

  function loadAssets() {
    const prefix = uiPrefix();
    if (!document.querySelector('link[data-nsight-error-popup-css]')) {
      const link = document.createElement('link');
      link.rel = 'stylesheet';
      link.href = prefix + '/_shared/error-popup.css';
      link.setAttribute('data-nsight-error-popup-css', '');
      document.head.appendChild(link);
    }
  }

  loadAssets();

  return {
    show,
    hide,
    fromPayload,
    fromError,
    normalize
  };
})();
