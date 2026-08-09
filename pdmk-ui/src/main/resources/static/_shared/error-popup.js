/**
 * pdmk-ui 공통 에러 팝업.
 * 서비스 오류 DTO(stdErrMsgCntn 등) · 중계 오류 · HTTP 오류를 모달로 보여준다.
 */
(function (global) {
  const MODAL_ID = 'pdmkErrorModal';

  function escapeHtml(value) {
    return String(value == null ? '' : value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function ensureModal() {
    let modal = document.getElementById(MODAL_ID);
    if (modal) {
      return modal;
    }

    modal = document.createElement('div');
    modal.id = MODAL_ID;
    modal.className = 'modal error-modal';
    modal.hidden = true;
    modal.innerHTML = `
      <div class="modal-backdrop" data-close="true"></div>
      <div class="modal-card" role="alertdialog" aria-modal="true" aria-labelledby="pdmkErrorTitle">
        <div class="panel-head">
          <h2 id="pdmkErrorTitle">오류</h2>
          <button class="btn-secondary" type="button" data-close="true">닫기</button>
        </div>
        <div class="modal-body">
          <p class="error-message" id="pdmkErrorMessage"></p>
          <dl class="detail-grid" id="pdmkErrorDetails" hidden></dl>
        </div>
        <div class="modal-actions">
          <button class="btn-primary" type="button" data-close="true">확인</button>
        </div>
      </div>
    `;
    document.body.appendChild(modal);

    modal.addEventListener('click', (event) => {
      if (event.target && event.target.getAttribute('data-close') === 'true') {
        hide();
      }
    });
    document.addEventListener('keydown', (event) => {
      if (event.key === 'Escape' && !modal.hidden) {
        hide();
      }
    });
    return modal;
  }

  function hide() {
    const modal = document.getElementById(MODAL_ID);
    if (modal) {
      modal.hidden = true;
    }
  }

  /**
   * @param {{ title?: string, message?: string, details?: Array<[string, string|number|null|undefined]> }} options
   */
  function show(options) {
    const opts = options || {};
    const modal = ensureModal();
    const titleEl = modal.querySelector('#pdmkErrorTitle');
    const messageEl = modal.querySelector('#pdmkErrorMessage');
    const detailsEl = modal.querySelector('#pdmkErrorDetails');

    titleEl.textContent = opts.title || '오류';
    messageEl.textContent = opts.message || '알 수 없는 오류가 발생했습니다.';

    const details = (opts.details || []).filter(([, value]) => value != null && String(value).trim() !== '');
    if (!details.length) {
      detailsEl.hidden = true;
      detailsEl.innerHTML = '';
    } else {
      detailsEl.hidden = false;
      detailsEl.innerHTML = details.map(([label, value]) => `
        <div>
          <dt>${escapeHtml(label)}</dt>
          <dd class="mono wrap">${escapeHtml(value)}</dd>
        </div>
      `).join('');
    }

    modal.hidden = false;
  }

  function showSimple(message, title) {
    show({ title: title || '오류', message: String(message == null ? '' : message) });
  }

  /** 응답 JSON에서 업무/시스템 오류 본문을 꺼낸다. */
  function errorPayload(parsed) {
    if (!parsed || typeof parsed !== 'object') {
      return null;
    }
    if (parsed.dto && typeof parsed.dto === 'object'
        && (parsed.dto.stdErrMsgCntn || parsed.dto.stdErrCode || parsed.dto.errType)) {
      return parsed.dto;
    }
    if (parsed.stdErrMsgCntn || parsed.stdErrCode || parsed.errType) {
      return parsed;
    }
    return null;
  }

  /**
   * 중계/서비스 응답을 해석해 오류면 팝업을 띄운다.
   * @returns {boolean} 오류 팝업을 띄웠으면 true
   */
  function showFromResponse(parsed, httpStatus, fallbackMessage) {
    const err = errorPayload(parsed);
    const relayError = parsed && (parsed.error || parsed.message);
    const failed = (httpStatus != null && (httpStatus < 200 || httpStatus >= 300))
        || !!err
        || !!relayError;

    if (!failed) {
      return false;
    }

    if (err) {
      const message = err.stdErrMsgCntn
          || err.addMsgContents
          || err.message
          || fallbackMessage
          || '서비스 오류가 발생했습니다.';
      show({
        title: err.stdErrCode ? `오류 (${err.stdErrCode})` : (err.errType ? `오류 (${err.errType})` : '서비스 오류'),
        message,
        details: [
          ['오류코드', err.stdErrCode],
          ['오류유형', err.errType],
          ['추가메시지', err.addMsgContents],
          ['클래스', err.errClassName],
          ['메서드', err.errMethodName],
          ['파일', err.errFileName],
          ['라인', err.errLineNo],
          ['HTTP', httpStatus]
        ]
      });
      return true;
    }

    const message = relayError
        || fallbackMessage
        || (httpStatus != null ? `HTTP ${httpStatus} 응답` : '요청 처리 중 오류가 발생했습니다.');
    show({
      title: '요청 오류',
      message: parsed && parsed.hint ? `${message}\n${parsed.hint}` : message,
      details: [
        ['HTTP', httpStatus],
        ['대상 URL', parsed && parsed.targetUrl]
      ]
    });
    return true;
  }

  global.PdmkErrorPopup = {
    show,
    showSimple,
    showFromResponse,
    hide,
    errorPayload
  };
})(window);
