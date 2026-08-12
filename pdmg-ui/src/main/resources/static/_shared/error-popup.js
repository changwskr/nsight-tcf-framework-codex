/**
 * pdmg-ui 공통 에러 팝업.
 * 서비스 오류는 응답 전문의 result 를 사용한다 (dto 는 업무 전용).
 * 코드별 안내 메시지 + 요약/힌트 중심 표시, 기술 로그는 접어서 제공.
 */
(function (global) {
  const MODAL_ID = 'pdmgErrorModal';

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
      <div class="modal-card error-modal-card" role="alertdialog" aria-modal="true" aria-labelledby="pdmgErrorTitle">
        <div class="error-modal-head">
          <div class="error-modal-head__main">
            <span class="error-severity" id="pdmgErrorSeverity">오류</span>
            <h2 id="pdmgErrorTitle">오류</h2>
            <p class="error-code-line" id="pdmgErrorCodeLine" hidden></p>
          </div>
          <button class="btn-secondary" type="button" data-close="true" aria-label="닫기">닫기</button>
        </div>
        <div class="modal-body">
          <p class="error-summary" id="pdmgErrorMessage"></p>
          <p class="error-hint" id="pdmgErrorHint" hidden></p>
          <dl class="error-detail-grid" id="pdmgErrorDetails" hidden></dl>
          <details class="error-tech" id="pdmgErrorTech" hidden>
            <summary>기술 정보 / 전체 로그</summary>
            <div class="error-tech-body">
              <p class="error-server-msg" id="pdmgErrorServerMsg" hidden></p>
              <div class="error-log-head">
                <span id="pdmgErrorLogLabel">전체 로그</span>
                <button class="btn-secondary btn-tiny" type="button" id="pdmgErrorLogCopy">복사</button>
              </div>
              <pre class="error-log mono" id="pdmgErrorLog"></pre>
            </div>
          </details>
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

    const copyBtn = modal.querySelector('#pdmgErrorLogCopy');
    copyBtn.addEventListener('click', async () => {
      const text = modal.querySelector('#pdmgErrorLog').textContent || '';
      try {
        await navigator.clipboard.writeText(text);
        copyBtn.textContent = '복사됨';
        setTimeout(() => { copyBtn.textContent = '복사'; }, 1200);
      } catch (_e) {
        copyBtn.textContent = '실패';
        setTimeout(() => { copyBtn.textContent = '복사'; }, 1200);
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

  function formatStackTrace(stackTrace) {
    if (stackTrace == null) {
      return '';
    }
    if (Array.isArray(stackTrace)) {
      return stackTrace.filter((line) => line != null && String(line).trim() !== '').join('\n');
    }
    return String(stackTrace);
  }

  function prettyJson(value) {
    if (value == null) {
      return '';
    }
    if (typeof value === 'string') {
      try {
        return JSON.stringify(JSON.parse(value), null, 2);
      } catch (_e) {
        return value;
      }
    }
    try {
      return JSON.stringify(value, null, 2);
    } catch (_e) {
      return String(value);
    }
  }

  /** addMsgContents 의 key=value,key=value 형태를 상세 행으로 변환 */
  function parseKvDetails(raw) {
    if (raw == null || String(raw).trim() === '') {
      return [];
    }
    const text = String(raw).trim();
    if (!text.includes('=')) {
      return [['추가정보', text]];
    }
    const labelMap = {
      serviceId: '서비스 ID',
      guid: 'GUID',
      timeoutMs: '제한시간(ms)',
      elapsedMs: '경과(ms)',
      active: '활성 스레드',
      poolSize: '풀 크기',
      queueSize: '대기 큐'
    };
    return text.split(',').map((part) => {
      const idx = part.indexOf('=');
      if (idx < 0) {
        return null;
      }
      const key = part.slice(0, idx).trim();
      const value = part.slice(idx + 1).trim();
      if (!key) {
        return null;
      }
      return [labelMap[key] || key, value];
    }).filter(Boolean);
  }

  function buildFullLog(opts) {
    const lines = [];
    const title = opts.title || '오류';
    const message = opts.message || '';
    lines.push(`[${title}]`);
    if (opts.code) {
      lines.push(`code: ${opts.code}`);
    }
    if (message) {
      lines.push(message);
    }
    if (opts.hint) {
      lines.push(`hint: ${opts.hint}`);
    }

    const details = (opts.details || []).filter(([, value]) => value != null && String(value).trim() !== '');
    if (details.length) {
      lines.push('');
      lines.push('---- 상세 ----');
      details.forEach(([label, value]) => {
        lines.push(`${label}: ${value}`);
      });
    }

    const stack = formatStackTrace(opts.stackTrace);
    if (stack) {
      lines.push('');
      lines.push('---- StackTrace ----');
      lines.push(stack);
    }

    if (opts.rawLog) {
      lines.push('');
      lines.push('---- 전체 응답 ----');
      lines.push(typeof opts.rawLog === 'string' ? opts.rawLog : prettyJson(opts.rawLog));
    }

    return lines.join('\n').trim();
  }

  function severityLabel(severity) {
    if (severity === 'warning') {
      return '주의';
    }
    if (severity === 'info') {
      return '안내';
    }
    return '오류';
  }

  function resolveFromOptions(opts) {
    const codes = global.PdmgErrorCodes;
    if (codes && typeof codes.resolveDisplay === 'function') {
      return codes.resolveDisplay(opts.code, opts.httpStatus, opts.serverMessage || opts.message);
    }
    return {
      code: opts.code || null,
      title: opts.title || '오류',
      summary: opts.message || '알 수 없는 오류가 발생했습니다.',
      catalogSummary: opts.message || '',
      hint: opts.hint || '',
      severity: opts.severity || 'error',
      serverMessage: opts.serverMessage || opts.message || ''
    };
  }

  /**
   * @param {{
   *   title?: string,
   *   message?: string,
   *   code?: string,
   *   httpStatus?: number|null,
   *   hint?: string,
   *   severity?: string,
   *   serverMessage?: string,
   *   details?: Array<[string, string|number|null|undefined]>,
   *   stackTrace?: string[]|string|null,
   *   rawLog?: string|object|null,
   *   logLabel?: string
   * }} options
   */
  function show(options) {
    const opts = options || {};
    const modal = ensureModal();
    const resolved = resolveFromOptions(opts);

    const severityEl = modal.querySelector('#pdmgErrorSeverity');
    const titleEl = modal.querySelector('#pdmgErrorTitle');
    const codeLine = modal.querySelector('#pdmgErrorCodeLine');
    const messageEl = modal.querySelector('#pdmgErrorMessage');
    const hintEl = modal.querySelector('#pdmgErrorHint');
    const detailsEl = modal.querySelector('#pdmgErrorDetails');
    const techEl = modal.querySelector('#pdmgErrorTech');
    const serverMsgEl = modal.querySelector('#pdmgErrorServerMsg');
    const logLabel = modal.querySelector('#pdmgErrorLogLabel');
    const logEl = modal.querySelector('#pdmgErrorLog');

    const severity = opts.severity || resolved.severity || 'error';
    modal.dataset.severity = severity;
    severityEl.textContent = severityLabel(severity);
    titleEl.textContent = opts.title || resolved.title || '오류';

    const code = opts.code || resolved.code;
    if (code) {
      codeLine.hidden = false;
      codeLine.innerHTML = `오류코드 <code>${escapeHtml(code)}</code>`
          + (opts.httpStatus != null ? ` · HTTP <code>${escapeHtml(opts.httpStatus)}</code>` : '');
    } else if (opts.httpStatus != null) {
      codeLine.hidden = false;
      codeLine.innerHTML = `HTTP <code>${escapeHtml(opts.httpStatus)}</code>`;
    } else {
      codeLine.hidden = true;
      codeLine.textContent = '';
    }

    messageEl.textContent = opts.message || resolved.summary || '알 수 없는 오류가 발생했습니다.';

    const hint = opts.hint != null ? opts.hint : resolved.hint;
    if (hint && String(hint).trim()) {
      hintEl.hidden = false;
      hintEl.textContent = hint;
    } else {
      hintEl.hidden = true;
      hintEl.textContent = '';
    }

    const details = (opts.details || []).filter(([, value]) => value != null && String(value).trim() !== '');
    if (!details.length) {
      detailsEl.hidden = true;
      detailsEl.innerHTML = '';
    } else {
      detailsEl.hidden = false;
      detailsEl.innerHTML = details.map(([label, value]) => `
        <div class="error-detail-item">
          <dt>${escapeHtml(label)}</dt>
          <dd class="mono wrap">${escapeHtml(value)}</dd>
        </div>
      `).join('');
    }

    const fullLog = buildFullLog({
      title: titleEl.textContent,
      code,
      message: messageEl.textContent,
      hint: hintEl.hidden ? '' : hintEl.textContent,
      details,
      stackTrace: opts.stackTrace,
      rawLog: opts.rawLog
    });

    const serverMessage = opts.serverMessage || resolved.serverMessage || '';
    const showServerMsg = serverMessage
        && serverMessage !== messageEl.textContent
        && !/\{[0-9]+\}/.test(serverMessage);

    if (fullLog || showServerMsg) {
      techEl.hidden = false;
      techEl.open = false;
      if (showServerMsg) {
        serverMsgEl.hidden = false;
        serverMsgEl.textContent = '서버 메시지: ' + serverMessage;
      } else {
        serverMsgEl.hidden = true;
        serverMsgEl.textContent = '';
      }
      logLabel.textContent = opts.logLabel || '전체 로그';
      logEl.textContent = fullLog;
    } else {
      techEl.hidden = true;
      techEl.open = false;
      logEl.textContent = '';
    }

    modal.hidden = false;
  }

  function showSimple(message, title) {
    const text = String(message == null ? '' : message);
    const looksTimeout = /시간 초과|timeout/i.test(text);
    const looksNetwork = /Failed to fetch|NetworkError|CORS|연결/i.test(text);
    show({
      title: title || undefined,
      message: text,
      code: looksTimeout ? 'UI_TIMEOUT' : (looksNetwork ? 'UI_NETWORK' : null),
      serverMessage: text,
      rawLog: text
    });
  }

  function isErrorObject(node) {
    return !!(node && typeof node === 'object'
        && (node.stdErrMsgCntn || node.stdErrCode || node.errType || node.stackTrace));
  }

  /** 응답 JSON에서 오류 본문(result)을 꺼낸다. dto 는 업무용이므로 사용하지 않는다. */
  function errorPayload(parsed) {
    if (!parsed || typeof parsed !== 'object') {
      return null;
    }
    if (isErrorObject(parsed.result)) {
      return parsed.result;
    }
    if (isErrorObject(parsed)) {
      return parsed;
    }
    return null;
  }

  /**
   * 중계/서비스 응답을 해석해 오류면 팝업을 띄운다.
   * @returns {boolean} 오류 팝업을 띄웠으면 true
   */
  function showFromResponse(parsed, httpStatus, fallbackMessage, rawBody) {
    const err = errorPayload(parsed);
    const relayError = parsed && (parsed.error || parsed.message);
    const relayCode = parsed && parsed.stdErrCode;
    const failed = (httpStatus != null && (httpStatus < 200 || httpStatus >= 300))
        || !!err
        || !!relayError;

    if (!failed) {
      return false;
    }

    const fullRaw = parsed != null ? parsed : (rawBody != null ? rawBody : null);

    if (err) {
      const serverMessage = err.stdErrMsgCntn
          || err.addMsgContents
          || err.message
          || fallbackMessage
          || '';
      const code = err.stdErrCode || relayCode || null;
      const kvDetails = parseKvDetails(err.addMsgContents);
      const display = global.PdmgErrorCodes
          ? global.PdmgErrorCodes.resolveDisplay(code, httpStatus, serverMessage)
          : null;

      show({
        title: display ? display.title : undefined,
        message: display ? display.summary : (serverMessage || '서비스 오류가 발생했습니다.'),
        code,
        httpStatus,
        hint: display ? display.hint : '',
        severity: display ? display.severity : 'error',
        serverMessage,
        details: [
          ['오류유형', err.errType],
          ...kvDetails,
          ['클래스', err.errClassName],
          ['메서드', err.errMethodName],
          ['파일', err.errFileName],
          ['라인', err.errLineNo]
        ],
        stackTrace: err.stackTrace,
        rawLog: fullRaw,
        logLabel: '전체 로그 (응답 전문 + StackTrace)'
      });
      return true;
    }

    const serverMessage = relayError
        || fallbackMessage
        || (httpStatus != null ? `HTTP ${httpStatus} 응답` : '요청 처리 중 오류가 발생했습니다.');
    const code = relayCode
        || (/시간 초과|timeout/i.test(String(serverMessage)) ? 'UI_TIMEOUT'
            : (/Failed to fetch|NetworkError|CORS/i.test(String(serverMessage)) ? 'UI_NETWORK' : null));
    const display = global.PdmgErrorCodes
        ? global.PdmgErrorCodes.resolveDisplay(code, httpStatus, serverMessage)
        : null;
    const hintParts = [];
    if (display && display.hint) {
      hintParts.push(display.hint);
    }
    if (parsed && parsed.hint) {
      hintParts.push(parsed.hint);
    }

    show({
      title: display ? display.title : '요청 오류',
      message: display ? display.summary : serverMessage,
      code,
      httpStatus,
      hint: hintParts.join(' '),
      severity: display ? display.severity : 'error',
      serverMessage,
      details: [
        ['대상 URL', parsed && parsed.targetUrl]
      ],
      rawLog: fullRaw != null ? fullRaw : serverMessage,
      logLabel: '전체 로그'
    });
    return true;
  }

  global.PdmgErrorPopup = {
    show,
    showSimple,
    showFromResponse,
    hide,
    errorPayload
  };
})(window);
