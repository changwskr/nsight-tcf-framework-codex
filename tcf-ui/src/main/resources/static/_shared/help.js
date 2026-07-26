/**
 * NSIGHT TCF UI — HELP 페이지 (오류 상세 #errors)
 */
(function () {
  const STORAGE_KEY = 'nsight.error.popup.last';

  function uiUrl(path) {
    return typeof window.nsightUiUrl === 'function' ? window.nsightUiUrl(path) : path;
  }

  function formatErrorDetail(info) {
    if (!window.NsightErrorPopup) return '표시할 오류 정보가 없습니다.';
    const n = NsightErrorPopup.normalize(info);
    return [
      `결과코드: ${n.resultCode}`,
      `오류코드: ${n.errorCode}`,
      `오류메시지: ${n.errorMessage}`,
      `시스템ID: ${n.errorSystemId}`,
      n.errorDateTime ? `발생시각: ${n.errorDateTime}` : '',
      n.httpStatus ? `HTTP: ${n.httpStatus}` : '',
      n.serviceId ? `serviceId: ${n.serviceId}` : '',
      n.traceId ? `traceId: ${n.traceId}` : '',
      n.targetUrl ? `targetUrl: ${n.targetUrl}` : '',
      n.errorDetail ? `\n[예외 상세]\n${n.errorDetail}` : '',
      n.systemNote ? `\n[시스템]\n${n.systemNote}` : ''
    ].filter(Boolean).join('\n');
  }

  function renderDetail(info) {
    const el = document.getElementById('helpErrorDetail');
    if (!el) return;
    el.textContent = formatErrorDetail(info);
  }

  function saveAndRender(info) {
    try {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(info));
    } catch (e) {
      /* ignore */
    }
    renderDetail(info);
  }

  function loadStoredError() {
    try {
      const raw = sessionStorage.getItem(STORAGE_KEY);
      if (raw) renderDetail(JSON.parse(raw));
    } catch (e) {
      /* ignore */
    }
  }

  function ensureErrorPopup(cb) {
    if (window.NsightErrorPopup) {
      cb();
      return;
    }
    const script = document.createElement('script');
    script.src = uiUrl('/_shared/error-popup.js');
    script.onload = cb;
    document.body.appendChild(script);
  }

  document.querySelectorAll('[data-help-link], [data-help-ui-link]').forEach(link => {
    const path = link.getAttribute('href');
    if (path) link.setAttribute('href', uiUrl(path));
  });

  const bizBtn = document.getElementById('helpSampleBizBtn');
  const sysBtn = document.getElementById('helpSampleSysBtn');
  const popupBtn = document.getElementById('helpPopupPreviewBtn');

  if (bizBtn) {
    bizBtn.addEventListener('click', () => {
      saveAndRender({
        resultCode: 'E0001',
        errorCode: 'E-OM-VAL-0001',
        errorMessage: '입력값을 확인해 주세요.',
        errorDetail: 'changeReason 필드 5자 미만',
        errorSystemId: 'NSIGHT-MP',
        errorDateTime: new Date().toISOString(),
        serviceId: 'OM.CommonCode.save',
        httpStatus: 200
      });
    });
  }

  if (sysBtn) {
    sysBtn.addEventListener('click', () => {
      saveAndRender({
        resultCode: 'E9999',
        errorCode: 'E-SYS-0001',
        errorMessage: '시스템 처리 중 예외가 발생했습니다.',
        errorDetail: 'java.lang.NullPointerException: criteria is null\n\tat com.nh.nsight...',
        errorSystemId: 'NSIGHT-MP',
        systemNote: 'Tomcat /om WAR · JDBC Timeout',
        httpStatus: 500
      });
    });
  }

  if (popupBtn) {
    popupBtn.addEventListener('click', () => {
      ensureErrorPopup(() => {
        try {
          const raw = sessionStorage.getItem(STORAGE_KEY);
          NsightErrorPopup.show(raw ? JSON.parse(raw) : {
            errorCode: 'E-DEMO-0001',
            errorMessage: '데모용 오류 알림입니다.',
            errorDetail: 'HELP 페이지에서 미리보기',
            errorSystemId: 'NSIGHT-MP'
          });
        } catch (e) {
          NsightErrorPopup.show({ errorMessage: e.message });
        }
      });
    });
  }

  loadStoredError();

  if (location.hash === '#errors') {
    const section = document.getElementById('helpErrors');
    if (section) section.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
})();
