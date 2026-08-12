/**
 * 브라우저에서 pdmg-service 온라인 거래를 직접 호출한다.
 * 반환 형태는 구 RelayResult 와 동일해 기존 화면 로직을 재사용한다.
 */
(function (global) {
  function trimTrailingSlash(baseUrl) {
    const value = (baseUrl || '').trim();
    return value.endsWith('/') ? value.slice(0, -1) : value;
  }

  function joinUrl(baseUrl, path) {
    const base = trimTrailingSlash(baseUrl || 'http://localhost:8080');
    let p = path == null ? '/' : String(path).trim();
    if (!p.startsWith('/')) {
      p = '/' + p;
    }
    return base + p;
  }

  /**
   * @param {string} targetUrl 절대 URL (예: http://localhost:8080/mgcoa8888S0)
   * @param {string|object|null} body JSON 문자열 또는 객체
   * @param {number} [timeoutMs] AbortController 제한(ms). 0 이하면 미사용
   * @param {string} [transactionId]
   * @returns {Promise<{transactionId:string,targetUrl:string,httpStatus:number,elapsedMs:number,responseBody:string}>}
   */
  async function post(targetUrl, body, timeoutMs, transactionId) {
    const started = performance.now();
    const controller = new AbortController();
    const ms = timeoutMs == null ? 0 : Number(timeoutMs);
    const timer = ms > 0 ? setTimeout(() => controller.abort(), ms) : null;

    let responseBody = '';
    let httpStatus = 0;
    try {
      const response = await fetch(targetUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json;charset=UTF-8',
          Accept: 'application/json'
        },
        body: typeof body === 'string' ? (body || '{}') : JSON.stringify(body == null ? {} : body),
        signal: controller.signal
      });
      httpStatus = response.status;
      responseBody = await response.text();
    } catch (error) {
      const aborted = !!(error && error.name === 'AbortError');
      const message = aborted
          ? `요청 시간 초과 (${ms} ms)`
          : (error && error.message) || String(error);
      httpStatus = aborted ? 504 : 502;
      responseBody = JSON.stringify({
        stdErrCode: aborted ? 'UI_TIMEOUT' : 'UI_NETWORK',
        error: message,
        targetUrl,
        hint: 'pdmg-service가 기동 중인지, CORS·대상 URL이 맞는지 확인하세요.'
      });
    } finally {
      if (timer) {
        clearTimeout(timer);
      }
    }

    return {
      transactionId: transactionId || '',
      targetUrl,
      httpStatus,
      elapsedMs: Math.round(performance.now() - started),
      responseBody
    };
  }

  async function postPath(baseUrl, path, body, timeoutMs, transactionId) {
    return post(joinUrl(baseUrl, path), body, timeoutMs, transactionId);
  }

  global.PdmgServiceClient = {
    joinUrl,
    post,
    postPath
  };
})(window);
