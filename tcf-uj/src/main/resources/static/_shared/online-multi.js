let modules = [];
let config = {};

(function bootstrapUiShared() {
  const prefix = (location.pathname.startsWith('/uj/') || location.pathname === '/uj') ? '/uj' : '';
  if (!window.__NSIGHT_UJ_CONTEXT_INIT__) {
    const script = document.createElement('script');
    script.src = prefix + '/_shared/uj-context.js';
    document.head.appendChild(script);
  }
})();

function uiPrefix() {
  return (location.pathname.startsWith('/uj/') || location.pathname === '/uj') ? '/uj' : '';
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
      systemNote: '온라인 거래 테스트 (다중)'
    });
  }
}

const businessCodeEl = document.getElementById('businessCode');
const transactionSelectEl = document.getElementById('transactionSelect');
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
    fetch('/api/multi/business-modules'),
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
    await selectBusiness(code);
  } else if (modules.length > 0) {
    businessCodeEl.value = modules[0].code;
    await selectBusiness(modules[0].code);
  }
}

function renderBusinessOptions() {
  businessCodeEl.innerHTML = modules.map(module => {
    const count = module.transactions.length;
    return `<option value="${module.code}">${module.code} - ${module.name} (${count}건)</option>`;
  }).join('');
}

function currentModule() {
  return modules.find(module => module.code === businessCodeEl.value);
}

function currentTransaction() {
  const module = currentModule();
  if (!module) return null;
  return module.transactions.find(tx => tx.id === transactionSelectEl.value) || module.transactions[0];
}

function renderTransactionOptions(module) {
  transactionSelectEl.innerHTML = module.transactions.map(tx => {
    const text = `[${tx.label}] ${tx.serviceId} / ${tx.transactionCode}`;
    return `<option value="${tx.id}">${text}</option>`;
  }).join('');
}

async function selectBusiness(code) {
  const module = modules.find(item => item.code === code);
  if (!module) return;

  document.getElementById('metaName').innerHTML =
    `${module.name}<span class="group-tag">${module.group}</span>` +
    `<span class="count-tag">${module.transactions.length} services</span>`;

  renderTransactionOptions(module);
  await selectTransaction();
}

async function selectTransaction() {
  const module = currentModule();
  const tx = currentTransaction();
  if (!module || !tx) return;

  document.getElementById('metaLabel').textContent = tx.label;
  document.getElementById('metaServiceId').textContent = tx.serviceId;
  document.getElementById('metaTransactionCode').textContent = tx.transactionCode;
  document.getElementById('metaProcessingType').textContent = tx.processingType;
  requestBodyEl.value = JSON.stringify(tx.sampleRequest, null, 2);
  await refreshTargetUrl(module.code);
}

function toggleDeploymentFields() {
  const tomcat = deploymentModeEl.value === 'tomcat';
  bootrunHostField.hidden = tomcat;
  tomcatGatewayField.hidden = !tomcat;
}

async function refreshTargetUrl(code) {
  const query = new URLSearchParams({
    deploymentMode: deploymentModeEl.value,
    bootrunHost: bootrunHostEl.value,
    tomcatGatewayUrl: tomcatGatewayUrlEl.value
  });
  const targetRes = await fetch(`/api/multi/business-modules/${code}/target-url?${query.toString()}`);
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
  const tx = currentTransaction();
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

  if (payload.header && tx) {
    payload.header.businessCode = code;
    payload.header.serviceId = tx.serviceId;
    payload.header.transactionCode = tx.transactionCode;
    payload.header.processingType = tx.processingType;
    requestBodyEl.value = JSON.stringify(payload, null, 2);
  }

  responseMetaEl.innerHTML = '<span class="empty">요청 중...</span>';
  responseBodyEl.value = '';

  const response = await fetch(`/api/multi/relay/${code}/online?${buildRelayQuery()}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  const result = await response.json();
  const ok = result.httpStatus >= 200 && result.httpStatus < 300;
  responseMetaEl.innerHTML = `
    <span class="badge ${ok ? 'ok' : 'fail'}">HTTP ${result.httpStatus}</span>
    <span>${result.elapsedMs} ms</span>
    <span>${tx ? tx.serviceId : ''}</span>
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
  refreshTargetUrl(businessCodeEl.value);
});
bootrunHostEl.addEventListener('change', () => refreshTargetUrl(businessCodeEl.value));
tomcatGatewayUrlEl.addEventListener('change', () => refreshTargetUrl(businessCodeEl.value));
businessCodeEl.addEventListener('change', () => selectBusiness(businessCodeEl.value));
transactionSelectEl.addEventListener('change', selectTransaction);
document.getElementById('reloadSampleBtn').addEventListener('click', selectTransaction);
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
