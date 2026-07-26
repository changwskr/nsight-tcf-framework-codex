let modules = [];
let config = {};

(function bootstrapUiShared() {
  const prefix = (location.pathname.startsWith('/ui/') || location.pathname === '/ui') ? '/ui' : '';
  if (!window.__NSIGHT_UI_CONTEXT_INIT__) {
    const script = document.createElement('script');
    script.src = prefix + '/_shared/ui-context.js';
    document.head.appendChild(script);
  }
})();

function uiPrefix() {
  return (location.pathname.startsWith('/ui/') || location.pathname === '/ui') ? '/ui' : '';
}

function ensureErrorPopupReady() {
  if (window.NsightErrorPopup) {
    return Promise.resolve(window.NsightErrorPopup);
  }
  return new Promise(resolve => {
    const finish = () => resolve(window.NsightErrorPopup || null);
    const existing = document.querySelector('script[data-nsight-error-popup]');
    if (existing) {
      const wait = setInterval(() => {
        if (window.NsightErrorPopup) {
          clearInterval(wait);
          finish();
        }
      }, 30);
      setTimeout(() => {
        clearInterval(wait);
        finish();
      }, 3000);
      return;
    }
    const script = document.createElement('script');
    script.src = uiPrefix() + '/_shared/error-popup.js';
    script.setAttribute('data-nsight-error-popup', '');
    script.onload = finish;
    script.onerror = finish;
    document.head.appendChild(script);
  });
}

async function showRelayError(result, parsed, message) {
  const popup = await ensureErrorPopupReady();
  if (!popup) {
    alert(message || '거래 처리 중 오류가 발생했습니다.');
    return;
  }
  if (parsed && parsed.result) {
    popup.show(popup.fromPayload(parsed, result, message));
  } else {
    popup.show({
      errorMessage: message || `HTTP ${result.httpStatus}`,
      httpStatus: result.httpStatus,
      targetUrl: result.targetUrl,
      systemNote: '온라인 거래 테스트'
    });
  }
}

const businessCodeEl = document.getElementById('businessCode');
const deploymentModeEl = document.getElementById('deploymentMode');
const bootrunHostEl = document.getElementById('bootrunHost');
const tomcatGatewayUrlEl = document.getElementById('tomcatGatewayUrl');
const bootrunHostField = document.getElementById('bootrunHostField');
const tomcatGatewayField = document.getElementById('tomcatGatewayField');
const requestBodyEl = document.getElementById('requestBody');
const responseBodyEl = document.getElementById('responseBody');
const responseMetaEl = document.getElementById('responseMeta');

function defaultBusinessCode() {
  return document.documentElement.dataset.defaultBusinessCode || 'SV';
}

async function init() {
  const [moduleRes, configRes] = await Promise.all([
    fetch('/api/business-modules'),
    fetch('/api/config')
  ]);
  modules = await moduleRes.json();
  config = await configRes.json();
  deploymentModeEl.value = config.deploymentMode || 'bootrun';
  bootrunHostEl.value = config.bootrunHost || 'http://localhost';
  tomcatGatewayUrlEl.value = config.tomcatGatewayUrl || 'http://localhost:8080';
  renderBusinessOptions();
  toggleDeploymentFields();
  const code = defaultBusinessCode();
  if (modules.some(module => module.code === code)) {
    businessCodeEl.value = code;
    await selectModule(code);
  } else if (modules.length > 0) {
    businessCodeEl.value = modules[0].code;
    await selectModule(modules[0].code);
  }
}

function renderBusinessOptions() {
  businessCodeEl.innerHTML = modules.map(module => {
    return `<option value="${module.code}">${module.code} - ${module.name}</option>`;
  }).join('');
}

function toggleDeploymentFields() {
  const tomcat = deploymentModeEl.value === 'tomcat';
  bootrunHostField.hidden = tomcat;
  tomcatGatewayField.hidden = !tomcat;
}

async function selectModule(code) {
  const module = modules.find(item => item.code === code);
  if (!module) return;

  document.getElementById('metaName').innerHTML =
    `${module.name}<span class="group-tag">${module.group}</span>`;
  document.getElementById('metaServiceId').textContent = module.serviceId;
  document.getElementById('metaTransactionCode').textContent = module.transactionCode;
  requestBodyEl.value = JSON.stringify(module.sampleRequest, null, 2);
  await refreshTargetUrl();
}

async function refreshTargetUrl() {
  const code = businessCodeEl.value;
  const query = new URLSearchParams({
    deploymentMode: deploymentModeEl.value,
    bootrunHost: bootrunHostEl.value,
    tomcatGatewayUrl: tomcatGatewayUrlEl.value
  });
  const targetRes = await fetch(`/api/business-modules/${code}/target-url?${query.toString()}`);
  if (!targetRes.ok) {
    document.getElementById('metaTargetUrl').textContent = 'URL 계산 실패';
    return;
  }
  const target = await targetRes.json();
  document.getElementById('metaTargetUrl').textContent = target.targetUrl;
}

function buildRelayQuery() {
  return new URLSearchParams({
    deploymentMode: deploymentModeEl.value,
    bootrunHost: bootrunHostEl.value.trim(),
    tomcatGatewayUrl: tomcatGatewayUrlEl.value.trim()
  }).toString();
}

async function sendRequest() {
  const code = businessCodeEl.value;
  let payload;
  try {
    payload = JSON.parse(requestBodyEl.value);
  } catch (error) {
    ensureErrorPopupReady().then(popup => {
      if (popup) {
        popup.show(popup.fromError(error, { systemNote: '요청 JSON 파싱' }));
      } else {
        alert('요청 JSON 형식이 올바르지 않습니다.');
      }
    });
    return;
  }

  if (payload.header) {
    payload.header.businessCode = code;
    requestBodyEl.value = JSON.stringify(payload, null, 2);
  }

  responseMetaEl.innerHTML = '<span class="empty">요청 중...</span>';
  responseBodyEl.value = '';

  const response = await fetch(`/api/relay/${code}/online?${buildRelayQuery()}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  const result = await response.json();
  const ok = result.httpStatus >= 200 && result.httpStatus < 300;
  responseMetaEl.innerHTML = `
    <span class="badge ${ok ? 'ok' : 'fail'}">HTTP ${result.httpStatus}</span>
    <span>${result.elapsedMs} ms</span>
    <span>${result.targetUrl}</span>
  `;
  let parsed = null;
  try {
    parsed = JSON.parse(result.responseBody);
    responseBodyEl.value = JSON.stringify(parsed, null, 2);
  } catch (error) {
    responseBodyEl.value = result.responseBody || '';
  }
  if (!ok) {
    await showRelayError(result, parsed, `HTTP ${result.httpStatus} 응답`);
    return;
  }
  if (parsed?.result?.resultCode && parsed.result.resultCode !== 'S0000') {
    await showRelayError(result, parsed);
  }
}

deploymentModeEl.addEventListener('change', () => {
  toggleDeploymentFields();
  refreshTargetUrl();
});
bootrunHostEl.addEventListener('change', refreshTargetUrl);
tomcatGatewayUrlEl.addEventListener('change', refreshTargetUrl);
businessCodeEl.addEventListener('change', () => selectModule(businessCodeEl.value));
document.getElementById('reloadSampleBtn').addEventListener('click', () => selectModule(businessCodeEl.value));
document.getElementById('sendBtn').addEventListener('click', sendRequest);

init().catch(error => {
  ensureErrorPopupReady().then(popup => {
    if (popup) {
      popup.show(popup.fromError(error, { systemNote: '화면 초기화' }));
    } else {
      alert('화면 초기화 실패: ' + error.message);
    }
  });
});
