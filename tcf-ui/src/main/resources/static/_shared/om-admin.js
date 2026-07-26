/**
 * NSIGHT OM 운영관리 포털 공통 유틸 (tcf-ui)
 * OM 업무 거래: accessToken 보유 시 /api/gateway/om/online (Bearer + Gateway JWT 검증)
 * OM 인증 거래(OM.Auth.*): /api/relay/OM/online (쿠키 직접 relay)
 */
window.OmAdmin = (function () {
  const BUSINESS_CODE = 'OM';
  const SESSION_KEY = 'nsight.om.session';
  const JWT_SESSION_KEY = 'nsight.jwt.session';
  const NAV_PRIMARY = [
    { id: 'dashboard', label: '운영 대시보드', href: '/om/admin/dashboard.html' },
    { id: 'transaction-log', label: '거래로그 조회', href: '/om/admin/transaction-log.html' },
    { id: 'transaction-control', label: '거래통제 관리', href: '/om/admin/transaction-control.html' },
    { id: 'timeout-policy', label: 'Timeout 정책', href: '/om/admin/timeout-policy.html' },
    { id: 'service-catalog', label: 'ServiceId 관리', href: '/om/admin/service-catalog.html' },
    { id: 'message-structure', label: '전문구조 관리', href: '/om/admin/message-structure.html' },
    { id: 'message-composer', label: '공통 전문 조립', href: '/om/admin/message-composer.html' },
    { id: 'user-auth', label: '사용자 / 권한 / 메뉴 / 기능·데이터권한', href: '/om/admin/user-auth.html' },
    { id: 'session', label: '세션 관리', href: '/om/admin/session.html' },
    { id: 'audit-log', label: '감사로그 조회', href: '/om/admin/audit-log.html' }
  ];

  /** 설계서 §2.1 런타임·장애진단 — RTM-010/020/030 워크스페이스 단일 진입 */
  const NAV_RUNTIME = [
    { id: 'runtime-diagnosis-guide', label: '진단 순서 가이드', href: '/om/admin/runtime-diagnosis-guide.html' },
    { id: 'runtime-workspace', label: '런타임·장애진단', href: '/om/admin/runtime-workspace.html' },
    { id: 'runtime-diagnostics', label: '런타임 진단 (상세 raw)', href: '/om/admin/runtime-diagnostics.html' },
    { id: 'runtime-status-cards', label: '핵심 상태 카드', href: '/om/admin/runtime-status-cards.html' },
    { id: 'runtime-thread-analysis', label: 'Thread 분석', href: '/om/admin/runtime-thread-analysis.html' },
    { id: 'runtime-jvm-analysis', label: 'JVM 분석 (RTM-020)', href: '/om/admin/runtime-jvm-analysis.html' },
    { id: 'runtime-dbpool-analysis', label: 'DB Pool 분석', href: '/om/admin/runtime-dbpool-analysis.html' },
    { id: 'runtime-dominance-analysis', label: 'WAR·자원 독점', href: '/om/admin/runtime-dominance-analysis.html' },
    { id: 'runtime-business-occupancy', label: '업무 점유 현황', href: '/om/admin/runtime-business-occupancy.html' },
    { id: 'runtime-active-transactions', label: '실행 중 거래 (RTM-040)', href: '/om/admin/runtime-active-transactions.html' },
    { id: 'runtime-sql-analysis', label: 'Slow SQL·외부연계 (RTM-060)', href: '/om/admin/runtime-workspace.html?tab=rtm060' },
    { id: 'runtime-transaction-detail', label: '거래 추적 상세 (RTM-050)', href: '/om/admin/runtime-workspace.html?tab=rtm050' },
    { id: 'runtime-cause-analysis', label: '장애 진단 및 보고서 (RTM-070)', href: '/om/admin/runtime-cause-analysis.html' },
    { id: 'runtime-incident-flow', label: '장애 흐름', href: '/om/admin/runtime-incident-flow.html' },
    { id: 'runtime-incident-history', label: '장애 이력 (RTM-080)', href: '/om/admin/runtime-incident-history.html' },
    { id: 'runtime-threshold-policy', label: '임계치·수집설정 (RTM-090)', href: '/om/admin/runtime-threshold-policy.html' }
  ];

  const NAV_SECONDARY = [
    { id: 'error-code', label: '오류코드 / 메시지', href: '/om/admin/error-code.html' },
    { id: 'batch', label: '배치 / 스케줄', href: '/om/admin/batch.html' },
    { id: 'deploy', label: '배포 관리', href: '/om/admin/deploy.html' },
    { id: 'health-check', label: 'Health Check', href: '/om/admin/health-check.html' },
    { id: 'system-config', label: '환경설정 조회', href: '/om/admin/system-config.html' },
    { id: 'file-management', label: '파일 관리', href: '/om/admin/file-management.html' }
  ];

  const NAV_TERTIARY = [
    { id: 'common-code', label: '공통코드 관리', href: '/om/admin/common-code.html' },
    { id: 'auth-history', label: '권한이력', href: '/om/admin/auth-history.html' },
    { id: 'cache', label: 'Cache 관리', href: '/om/admin/cache.html' }
  ];

  const NAV = [...NAV_PRIMARY, ...NAV_RUNTIME, ...NAV_SECONDARY, ...NAV_TERTIARY];

  const TX = {
    authLogin: { serviceId: 'OM.Auth.login', transactionCode: 'OM-AUT-0002' },
    authSsoLogin: { serviceId: 'OM.Auth.ssoLogin', transactionCode: 'OM-AUT-0005' },
    authLogout: { serviceId: 'OM.Auth.logout', transactionCode: 'OM-AUT-0003' },
    authSession: { serviceId: 'OM.Auth.session', transactionCode: 'OM-AUT-0004' },
    dashboard: { serviceId: 'OM.Dashboard.inquiry', transactionCode: 'OM-DSH-0001' },
    dashboardReset: { serviceId: 'OM.Dashboard.reset', transactionCode: 'OM-DSH-0002' },
    transactionLog: { serviceId: 'OM.TransactionLog.inquiry', transactionCode: 'OM-TXL-0001' },
    transactionLogDeleteAll: { serviceId: 'OM.TransactionLog.deleteAll', transactionCode: 'OM-TXL-0002' },
    serviceCatalog: { serviceId: 'OM.ServiceCatalog.inquiry', transactionCode: 'OM-SVC-0001' },
    serviceCatalogDetail: { serviceId: 'OM.ServiceCatalog.detail', transactionCode: 'OM-SVC-0003' },
    serviceCatalogSave: { serviceId: 'OM.ServiceCatalog.save', transactionCode: 'OM-SVC-0002' },
    serviceCatalogUpdate: { serviceId: 'OM.ServiceCatalog.update', transactionCode: 'OM-SVC-0004' },
    serviceCatalogDelete: { serviceId: 'OM.ServiceCatalog.delete', transactionCode: 'OM-SVC-0005' },
    messageStructure: { serviceId: 'OM.MessageStructure.inquiry', transactionCode: 'OM-MSG-0001' },
    messageStructureDetail: { serviceId: 'OM.MessageStructure.detail', transactionCode: 'OM-MSG-0002' },
    messageStructureFramework: { serviceId: 'OM.MessageStructure.frameworkInquiry', transactionCode: 'OM-MSG-0003' },
    messageStructureSave: { serviceId: 'OM.MessageStructure.save', transactionCode: 'OM-MSG-0004' },
    messageStructureUpdate: { serviceId: 'OM.MessageStructure.update', transactionCode: 'OM-MSG-0005' },
    messageStructureDelete: { serviceId: 'OM.MessageStructure.delete', transactionCode: 'OM-MSG-0006' },
    user: { serviceId: 'OM.User.inquiry', transactionCode: 'OM-USR-0001' },
    userDetail: { serviceId: 'OM.User.detail', transactionCode: 'OM-USR-0002' },
    userSave: { serviceId: 'OM.User.save', transactionCode: 'OM-USR-0003' },
    userUpdate: { serviceId: 'OM.User.update', transactionCode: 'OM-USR-0004' },
    userDelete: { serviceId: 'OM.User.delete', transactionCode: 'OM-USR-0005' },
    menu: { serviceId: 'OM.Menu.inquiry', transactionCode: 'OM-MNU-0001' },
    menuSave: { serviceId: 'OM.Menu.save', transactionCode: 'OM-MNU-0002' },
    menuDetail: { serviceId: 'OM.Menu.detail', transactionCode: 'OM-MNU-0003' },
    menuUpdate: { serviceId: 'OM.Menu.update', transactionCode: 'OM-MNU-0004' },
    menuDelete: { serviceId: 'OM.Menu.delete', transactionCode: 'OM-MNU-0005' },
    authGroup: { serviceId: 'OM.AuthGroup.inquiry', transactionCode: 'OM-AUT-0001' },
    authGroupSave: { serviceId: 'OM.AuthGroup.save', transactionCode: 'OM-AGP-0001' },
    authGroupDetail: { serviceId: 'OM.AuthGroup.detail', transactionCode: 'OM-AGP-0002' },
    authGroupUpdate: { serviceId: 'OM.AuthGroup.update', transactionCode: 'OM-AGP-0003' },
    authGroupDelete: { serviceId: 'OM.AuthGroup.delete', transactionCode: 'OM-AGP-0004' },
    auditLog: { serviceId: 'OM.AuditLog.inquiry', transactionCode: 'OM-AUD-0001' },
    auditLogDeleteAll: { serviceId: 'OM.AuditLog.deleteAll', transactionCode: 'OM-AUD-0002' },
    errorCode: { serviceId: 'OM.ErrorCode.inquiry', transactionCode: 'OM-ERR-0001' },
    batch: { serviceId: 'OM.Batch.inquiry', transactionCode: 'OM-BAT-0001' },
    healthCheck: { serviceId: 'OM.HealthCheck.inquiry', transactionCode: 'OM-HLT-0001' },
    runtimeDiagnostics: { serviceId: 'OM.Runtime.inquiry', transactionCode: 'OM-RTM-0001' },
    systemConfig: { serviceId: 'OM.SystemConfig.inquiry', transactionCode: 'OM-CFG-0001' },
    fileDownload: { serviceId: 'OM.FileDownload.inquiry', transactionCode: 'OM-FIL-0001' },
    commonCode: { serviceId: 'OM.CommonCode.inquiry', transactionCode: 'OM-CDC-0001' },
    commonCodeDetail: { serviceId: 'OM.CommonCode.detail', transactionCode: 'OM-CDC-0003' },
    commonCodeSave: { serviceId: 'OM.CommonCode.save', transactionCode: 'OM-CDC-0002' },
    commonCodeUpdate: { serviceId: 'OM.CommonCode.update', transactionCode: 'OM-CDC-0004' },
    commonCodeDelete: { serviceId: 'OM.CommonCode.delete', transactionCode: 'OM-CDC-0005' },
    errorCodeSave: { serviceId: 'OM.ErrorCode.save', transactionCode: 'OM-ERR-0002' },
    errorCodeDetail: { serviceId: 'OM.ErrorCode.detail', transactionCode: 'OM-ERR-0003' },
    errorCodeUpdate: { serviceId: 'OM.ErrorCode.update', transactionCode: 'OM-ERR-0004' },
    errorCodeDelete: { serviceId: 'OM.ErrorCode.delete', transactionCode: 'OM-ERR-0005' },
    batchExecute: { serviceId: 'OM.Batch.execute', transactionCode: 'OM-BAT-0002' },
    batchHistoryDeleteAll: { serviceId: 'OM.Batch.deleteAll', transactionCode: 'OM-BAT-0003' },
    deployHistory: { serviceId: 'OM.Deploy.history', transactionCode: 'OM-DPL-0006' },
    deployBuildRequest: { serviceId: 'OM.Deploy.buildRequest', transactionCode: 'OM-DPL-0001' },
    deployDeployRequest: { serviceId: 'OM.Deploy.deployRequest', transactionCode: 'OM-DPL-0003' },
    deployApprove: { serviceId: 'OM.Deploy.approve', transactionCode: 'OM-DPL-0004' },
    deployExecute: { serviceId: 'OM.Deploy.execute', transactionCode: 'OM-DPL-0005' },
    deployRollback: { serviceId: 'OM.Deploy.rollbackRequest', transactionCode: 'OM-DPL-0008' },
    deployLog: { serviceId: 'OM.Deploy.logInquiry', transactionCode: 'OM-DPL-0007' },
    deployHealthCheck: { serviceId: 'OM.Deploy.healthCheck', transactionCode: 'OM-DPL-0009' },
    deployDeleteAll: { serviceId: 'OM.Deploy.deleteAll', transactionCode: 'OM-DPL-0010' },
    functionAuth: { serviceId: 'OM.FunctionAuth.inquiry', transactionCode: 'OM-FAU-0001' },
    functionAuthDetail: { serviceId: 'OM.FunctionAuth.detail', transactionCode: 'OM-FAU-0003' },
    functionAuthSave: { serviceId: 'OM.FunctionAuth.save', transactionCode: 'OM-FAU-0002' },
    functionAuthUpdate: { serviceId: 'OM.FunctionAuth.update', transactionCode: 'OM-FAU-0004' },
    functionAuthDelete: { serviceId: 'OM.FunctionAuth.delete', transactionCode: 'OM-FAU-0005' },
    dataAuth: { serviceId: 'OM.DataAuth.inquiry', transactionCode: 'OM-DAU-0001' },
    authHistory: { serviceId: 'OM.AuthHistory.inquiry', transactionCode: 'OM-AHT-0001' },
    authHistoryDeleteAll: { serviceId: 'OM.AuthHistory.deleteAll', transactionCode: 'OM-AHT-0002' },
    cache: { serviceId: 'OM.Cache.inquiry', transactionCode: 'OM-CCH-0001' },
    cacheDelete: { serviceId: 'OM.Cache.delete', transactionCode: 'OM-CCH-0002' },
    session: { serviceId: 'OM.Session.inquiry', transactionCode: 'OM-SES-0001' },
    sessionDelete: { serviceId: 'OM.Session.delete', transactionCode: 'OM-SES-0002' },
    transactionControl: { serviceId: 'OM.TransactionControl.inquiry', transactionCode: 'OM-TXC-0001' },
    transactionControlSave: { serviceId: 'OM.TransactionControl.save', transactionCode: 'OM-TXC-0002' },
    transactionControlDelete: { serviceId: 'OM.TransactionControl.delete', transactionCode: 'OM-TXC-0003' },
    transactionControlUpdate: { serviceId: 'OM.TransactionControl.update', transactionCode: 'OM-TXC-0004' },
    timeoutPolicy: { serviceId: 'OM.TimeoutPolicy.inquiry', transactionCode: 'OM-TMO-0001' },
    timeoutPolicySave: { serviceId: 'OM.TimeoutPolicy.save', transactionCode: 'OM-TMO-0002' },
    timeoutPolicyUpdate: { serviceId: 'OM.TimeoutPolicy.update', transactionCode: 'OM-TMO-0003' },
    timeoutPolicyDelete: { serviceId: 'OM.TimeoutPolicy.delete', transactionCode: 'OM-TMO-0004' }
  };

  const TX_SERVICE_NAME = {
    'OM.Auth.login': 'OM 로그인',
    'OM.Auth.ssoLogin': 'OM SSO 로그인',
    'OM.Auth.logout': 'OM 로그아웃',
    'OM.Auth.session': 'OM 세션 조회',
    'OM.Dashboard.inquiry': '운영 대시보드',
    'OM.Dashboard.reset': '대시보드 스냅샷 DB 초기화',
    'OM.TransactionLog.inquiry': '거래로그 조회',
    'OM.TransactionLog.deleteAll': '거래로그 전체 삭제',
    'OM.TransactionControl.inquiry': '거래통제 조회',
    'OM.TransactionControl.save': '거래통제 등록',
    'OM.TransactionControl.delete': '거래통제 삭제',
    'OM.TransactionControl.update': '거래통제 수정',
    'OM.TimeoutPolicy.inquiry': 'Timeout 정책 조회',
    'OM.TimeoutPolicy.save': 'Timeout 정책 등록',
    'OM.TimeoutPolicy.update': 'Timeout 정책 수정',
    'OM.TimeoutPolicy.delete': 'Timeout 정책 삭제',
    'OM.ServiceCatalog.inquiry': 'ServiceId 카탈로그',
    'OM.ServiceCatalog.save': 'ServiceId 등록',
    'OM.ServiceCatalog.detail': 'ServiceId 상세',
    'OM.ServiceCatalog.update': 'ServiceId 수정',
    'OM.ServiceCatalog.delete': 'ServiceId 삭제',
    'OM.MessageStructure.inquiry': '전문구조 조회',
    'OM.MessageStructure.detail': '전문구조 상세',
    'OM.MessageStructure.frameworkInquiry': 'TCF 표준 전문 템플릿 (tcf-core catalog)',
    'OM.MessageStructure.save': '전문구조 등록',
    'OM.MessageStructure.update': '전문구조 수정',
    'OM.MessageStructure.delete': '전문구조 삭제',
    'OM.User.inquiry': '사용자 조회',
    'OM.User.detail': '사용자 상세',
    'OM.User.save': '사용자 등록',
    'OM.User.update': '사용자 수정',
    'OM.User.delete': '사용자 삭제',
    'OM.Menu.inquiry': '메뉴 조회',
    'OM.Menu.save': '메뉴 등록',
    'OM.Menu.detail': '메뉴 상세',
    'OM.Menu.update': '메뉴 수정',
    'OM.Menu.delete': '메뉴 삭제',
    'OM.AuthGroup.inquiry': '권한그룹 조회',
    'OM.AuthGroup.save': '권한그룹 등록',
    'OM.AuthGroup.detail': '권한그룹 상세',
    'OM.AuthGroup.update': '권한그룹 수정',
    'OM.AuthGroup.delete': '권한그룹 삭제',
    'OM.AuditLog.inquiry': '감사로그 조회',
    'OM.AuditLog.deleteAll': '감사로그 전체 삭제',
    'OM.ErrorCode.inquiry': '오류코드 조회',
    'OM.ErrorCode.save': '오류코드 등록',
    'OM.ErrorCode.detail': '오류코드 상세',
    'OM.ErrorCode.update': '오류코드 수정',
    'OM.ErrorCode.delete': '오류코드 삭제',
    'OM.Batch.inquiry': '배치/스케줄 조회',
    'OM.Batch.execute': '배치 재실행',
    'OM.Batch.deleteAll': '배치 실행이력 전체 삭제',
    'OM.HealthCheck.inquiry': 'Health Check 조회',
    'OM.Runtime.inquiry': '런타임 진단',
    'OM.SystemConfig.inquiry': '환경설정 조회',
    'OM.FileDownload.inquiry': '파일 다운로드 이력',
    'OM.CommonCode.inquiry': '공통코드 목록 조회',
    'OM.CommonCode.save': '공통코드 등록',
    'OM.CommonCode.detail': '공통코드 단건 조회',
    'OM.CommonCode.update': '공통코드 수정',
    'OM.CommonCode.delete': '공통코드 삭제',
    'OM.FunctionAuth.inquiry': '기능권한 조회',
    'OM.FunctionAuth.save': '기능권한 등록',
    'OM.FunctionAuth.detail': '기능권한 상세',
    'OM.FunctionAuth.update': '기능권한 수정',
    'OM.FunctionAuth.delete': '기능권한 삭제',
    'OM.DataAuth.inquiry': '데이터권한 조회',
    'OM.AuthHistory.inquiry': '권한이력 조회',
    'OM.AuthHistory.deleteAll': '권한이력 전체 삭제',
    'OM.Cache.inquiry': 'Cache 조회',
    'OM.Cache.delete': 'Cache 삭제',
    'OM.Session.inquiry': '세션 목록 조회',
    'OM.Session.delete': '세션 강제 종료',
    'OM.Deploy.buildRequest': '배포 Gradle 빌드 요청',
    'OM.Deploy.buildStatus': '배포 빌드 상태 조회',
    'OM.Deploy.deployRequest': '배포 요청 등록',
    'OM.Deploy.approve': '배포 승인',
    'OM.Deploy.execute': '배포 실행',
    'OM.Deploy.history': '배포 이력 조회',
    'OM.Deploy.logInquiry': '배포 로그 조회',
    'OM.Deploy.rollbackRequest': '배포 롤백 요청',
    'OM.Deploy.healthCheck': '배포 Health Check',
    'OM.Deploy.deleteAll': '배포 요청·이력 초기화'
  };

  Object.keys(TX).forEach(key => {
    const tx = TX[key];
    if (tx && tx.serviceId && TX_SERVICE_NAME[tx.serviceId]) {
      tx.serviceName = TX_SERVICE_NAME[tx.serviceId];
    }
  });

  let config = { deploymentMode: 'bootrun', bootrunHost: 'http://127.0.0.1', tomcatGatewayUrl: 'http://localhost:8080', omGatewayEnabled: true };
  let targetUrl = '-';

  function getJwtSession() {
    try {
      const raw = sessionStorage.getItem(JWT_SESSION_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch (e) {
      return null;
    }
  }

  function isAuthTransaction(tx) {
    return tx && tx.serviceId && String(tx.serviceId).startsWith('OM.Auth.');
  }

  function buildOmAuthHeaders() {
    const jwt = getJwtSession();
    const headers = { 'Content-Type': 'application/json' };
    if (jwt && jwt.accessToken) {
      const tokenType = jwt.tokenType || 'Bearer';
      headers.Authorization = `${tokenType} ${jwt.accessToken}`;
    }
    return headers;
  }

  function shouldUseOmGateway(tx) {
    if (!config.omGatewayEnabled || isAuthTransaction(tx)) {
      return false;
    }
    const jwt = getJwtSession();
    return !!(jwt && jwt.accessToken);
  }

  function todayIsoDate() {
    const now = new Date();
    const y = now.getFullYear();
    const m = String(now.getMonth() + 1).padStart(2, '0');
    const d = String(now.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  function todaySystemDate() {
    return todayIsoDate().replace(/-/g, '');
  }

  function newGuid() {
    if (window.crypto && crypto.randomUUID) return crypto.randomUUID();
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
      const r = Math.random() * 16 | 0;
      return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
    });
  }

  function nowIsoKst() {
    const now = new Date();
    const offset = now.getTime() + (9 * 60 - now.getTimezoneOffset()) * 60000;
    return new Date(offset).toISOString().replace('Z', '+09:00');
  }

  function buildRelayQuery() {
    const mode = isTomcatUiDeployment() ? 'tomcat' : (config.deploymentMode || 'bootrun');
    return new URLSearchParams({
      deploymentMode: mode,
      bootrunHost: config.bootrunHost,
      tomcatGatewayUrl: config.tomcatGatewayUrl || 'http://localhost:8080'
    }).toString();
  }

  function uiContextPrefix() {
    if (window.__NSIGHT_UI_CTX__) {
      return window.__NSIGHT_UI_CTX__;
    }
    if (location.pathname.startsWith('/ui/') || location.pathname === '/ui') {
      return '/ui';
    }
    return '';
  }

  function uiPath(path) {
    if (typeof window.nsightUiUrl === 'function') {
      return window.nsightUiUrl(path);
    }
    const normalized = path.startsWith('/') ? path : '/' + path;
    const prefix = uiContextPrefix();
    if (prefix && (normalized === prefix || normalized.startsWith(prefix + '/'))) {
      return normalized;
    }
    return prefix + normalized;
  }

  function relayFetch(path, init) {
    return fetch(uiPath(path), init);
  }

  let errorPopupReady = null;
  function ensureErrorPopupReady() {
    if (window.NsightErrorPopup) {
      return Promise.resolve(window.NsightErrorPopup);
    }
    if (!errorPopupReady) {
      errorPopupReady = new Promise(resolve => {
        const finish = () => resolve(window.NsightErrorPopup || null);
        if (document.querySelector('script[data-nsight-error-popup]')) {
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
        script.src = uiContextPrefix() + '/_shared/error-popup.js';
        script.setAttribute('data-nsight-error-popup', '');
        script.onload = finish;
        script.onerror = finish;
        document.head.appendChild(script);
      });
    }
    return errorPopupReady;
  }

  async function showErrorPopup(info) {
    const popup = await ensureErrorPopupReady();
    if (popup) popup.show(info);
  }

  async function notifyTransactionError(payload, relay, fallbackMessage) {
    const popup = await ensureErrorPopupReady();
    if (popup) popup.show(popup.fromPayload(payload, relay, fallbackMessage));
  }

  async function parseRelayResponse(res) {
    const text = await res.text();
    let relay;
    try {
      relay = text ? JSON.parse(text) : {};
    } catch (e) {
      throw new Error(`릴레이 응답 파싱 실패 (HTTP ${res.status})`);
    }
    if (relay.responseBody == null || relay.responseBody === '') {
      const status = relay.httpStatus != null ? relay.httpStatus : (relay.status != null ? relay.status : res.status);
      if (status === 404 || relay.error === 'Not Found') {
        throw new Error('tcf-ui API(/ui/api/relay)를 찾을 수 없습니다. Tomcat /ui 배포와 ui-context.js 로드를 확인하세요.');
      }
      throw new Error(relay.errorMessage || relay.message || `HTTP ${status}: 응답 없음`);
    }
    return relay;
  }

  function getSession() {
    try {
      const raw = sessionStorage.getItem(SESSION_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch (e) {
      return null;
    }
  }

  function setSession(session) {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
  }

  function clearSession() {
    sessionStorage.removeItem(SESSION_KEY);
  }

  function syncSessionFromBody(body) {
    if (!body || !body.loggedIn) {
      return null;
    }
    const session = {
      userId: body.userId,
      userName: body.userName,
      branchId: body.branchId,
      authGroupId: body.authGroupId,
      authGroupName: body.authGroupName,
      sessionId: body.sessionId,
      lastLoginTime: body.lastLoginTime,
      loginType: body.loginType || 'PASSWORD'
    };
    setSession(session);
    if (body.accessToken) {
      sessionStorage.setItem(JWT_SESSION_KEY, JSON.stringify({
        userId: body.userId,
        userName: body.userName,
        branchId: body.branchId,
        authGroupId: body.authGroupId,
        authGroupName: body.authGroupName,
        accessToken: body.accessToken,
        refreshToken: body.refreshToken,
        tokenType: body.tokenType || 'Bearer',
        expiresIn: body.expiresIn,
        loginType: body.loginType || 'SSO'
      }));
    }
    return session;
  }

  async function requireAuth() {
    if (location.pathname.endsWith('login.html')) {
      return null;
    }
    await loadConfig();
    const jwt = getJwtSession();
    if (config.omGatewayEnabled && jwt && jwt.accessToken) {
      setSession({
        userId: jwt.userId,
        userName: jwt.userName,
        branchId: jwt.branchId,
        authGroupId: jwt.authGroupId,
        authGroupName: jwt.authGroupName,
        authType: 'jwt',
        loginType: jwt.loginType || 'JWT'
      });
      return getSession();
    }
    try {
      const { body } = await call('authSession', {}, 'INQUIRY');
      if (body.loggedIn) {
        return syncSessionFromBody(body);
      }
    } catch (e) {
      /* 서버 세션 없음 */
    }
    clearSession();
    location.href = uiPath('/om/admin/login.html');
    return null;
  }

  async function logout() {
    try {
      await mutate('authLogout', {}, 'EXECUTE');
    } catch (e) {
      /* ignore */
    } finally {
      clearSession();
      location.href = uiPath('/om/admin/login.html');
    }
  }

  function buildStandardHeader(options) {
    const session = getSession();
    const systemDate = todaySystemDate();
    const o = options || {};
    return {
      systemId: o.systemId || 'NSIGHT-MP',
      businessCode: (o.businessCode || BUSINESS_CODE).toUpperCase(),
      serviceId: o.serviceId || '',
      transactionCode: o.transactionCode || '',
      serviceName: o.serviceName != null ? o.serviceName : '',
      processingType: (o.processingType || 'INQUIRY').toUpperCase(),
      guid: o.guid || newGuid(),
      traceId: o.traceId != null ? o.traceId : '',
      channelId: o.channelId || 'WEBTOP',
      userId: o.userId != null ? o.userId : (session && session.userId ? session.userId : 'GUEST'),
      branchId: o.branchId != null ? o.branchId : (session && session.branchId ? session.branchId : ''),
      centerId: o.centerId || 'DC1',
      requestTime: o.requestTime || nowIsoKst(),
      transactionIntime: o.transactionIntime || nowIsoKst(),
      systemDate: o.systemDate || systemDate,
      bizDate: o.bizDate || systemDate,
      clientIp: o.clientIp || '127.0.0.1',
      idempotencyKey: o.idempotencyKey != null ? o.idempotencyKey : ''
    };
  }

  function resolveServiceName(tx) {
    if (!tx) {
      return '';
    }
    return tx.serviceName || TX_SERVICE_NAME[tx.serviceId] || '';
  }

  function buildHeader(tx, processingType) {
    return buildStandardHeader({
      businessCode: BUSINESS_CODE,
      serviceId: tx.serviceId,
      transactionCode: tx.transactionCode,
      serviceName: resolveServiceName(tx),
      processingType: processingType || 'INQUIRY'
    });
  }

  function apiUrl(path) {
    return typeof window.nsightUiUrl === 'function' ? window.nsightUiUrl(path) : path;
  }

  function isTomcatUiDeployment() {
    return config.deploymentMode === 'tomcat'
        || location.pathname.startsWith('/ui/') || location.pathname === '/ui';
  }

  function resolveBatchServiceUrl() {
    if (isTomcatUiDeployment()) {
      const gateway = (config.tomcatGatewayUrl || 'http://localhost:8080').replace(/\/$/, '');
      return `${gateway}/batch`;
    }
    const host = (config.bootrunHost || 'http://127.0.0.1').replace(/\/$/, '');
    return `${host}:8098/batch`;
  }

  function resolveBatchLabel() {
    if (isTomcatUiDeployment()) {
      try {
        const u = new URL((config.tomcatGatewayUrl || 'http://localhost:8080').replace(/\/$/, ''));
        const port = u.port || (u.protocol === 'https:' ? '443' : '80');
        return `Tomcat /batch (${port})`;
      } catch (e) {
        return 'Tomcat /batch (8080)';
      }
    }
    return 'tcf-batch (:8098)';
  }

  async function loadConfig() {
    const res = await fetch(uiPath('/api/config'));
    if (res.ok) {
      const data = await res.json();
      config.deploymentMode = data.deploymentMode || config.deploymentMode;
      config.bootrunHost = data.bootrunHost || config.bootrunHost;
      config.tomcatGatewayUrl = data.tomcatGatewayUrl || config.tomcatGatewayUrl;
      config.omGatewayEnabled = data.omGatewayEnabled !== false;
      config.gatewayOmUrl = data.gatewayOmUrl || config.gatewayOmUrl;
    }
    const jwt = getJwtSession();
    const targetPath = (config.omGatewayEnabled && jwt && jwt.accessToken)
      ? `/api/gateway/om/target-url?${buildRelayQuery()}`
      : `/api/business-modules/${BUSINESS_CODE}/target-url?${buildRelayQuery()}`;
    const urlRes = await fetch(uiPath(targetPath));
    if (urlRes.ok) {
      const data = await urlRes.json();
      targetUrl = data.targetUrl || targetUrl;
    }
    return config;
  }

  async function login(userId, password) {
    const tx = TX.authLogin;
    const request = {
      header: {
        ...buildHeader(tx, 'EXECUTE'),
        userId: userId || 'GUEST',
        branchId: '',
        serviceName: resolveServiceName(tx) || 'OM 로그인'
      },
      body: { userId, password }
    };
    const res = await relayFetch(`/api/relay/${BUSINESS_CODE}/online?${buildRelayQuery()}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(request)
    });
    const relay = await parseRelayResponse(res);
    let payload;
    try {
      payload = JSON.parse(relay.responseBody);
    } catch (e) {
      throw new Error('응답 JSON 파싱 실패');
    }
    if (relay.httpStatus >= 400) {
      let msg = payload.result?.errorMessage || payload.result?.message || payload.error;
      if (!msg && relay.httpStatus === 502) {
        msg = isTomcatUiDeployment()
          ? 'tcf-om(/om)에 연결할 수 없습니다. Tomcat 기동 상태를 확인하세요.'
          : 'tcf-om(8097)에 연결할 수 없습니다. tcf-om을 먼저 실행하세요.';
      }
      throw new Error(msg || `HTTP ${relay.httpStatus}`);
    }
    if (payload.result && payload.result.resultCode && payload.result.resultCode !== 'S0000') {
      throw new Error(payload.result.errorMessage || payload.result.resultMessage || '로그인에 실패했습니다.');
    }
    const body = payload.body || {};
    if (!body.loggedIn) {
      throw new Error('로그인에 실패했습니다.');
    }
    syncSessionFromBody(body);
    return body;
  }

  async function ssoLogin(ssoToken, ssoSubject, options) {
    const opts = options || {};
    const tx = TX.authSsoLogin;
    const request = {
      header: {
        ...buildHeader(tx, 'EXECUTE'),
        userId: ssoSubject || opts.userId || 'GUEST',
        branchId: opts.branchId || '',
        channelId: opts.channelId || 'WEBTOP',
        serviceName: resolveServiceName(tx) || 'OM SSO 로그인'
      },
      body: {
        ssoToken,
        ssoSubject,
        userId: opts.userId || ssoSubject,
        ssoAssertionId: opts.ssoAssertionId,
        channelId: opts.channelId || 'WEBTOP'
      }
    };
    const res = await relayFetch(`/api/relay/${BUSINESS_CODE}/online?${buildRelayQuery()}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(request)
    });
    const relay = await parseRelayResponse(res);
    let payload;
    try {
      payload = JSON.parse(relay.responseBody);
    } catch (e) {
      throw new Error('응답 JSON 파싱 실패');
    }
    if (relay.httpStatus >= 400) {
      const msg = payload.result?.errorMessage || payload.result?.message || payload.error
          || `HTTP ${relay.httpStatus}`;
      throw new Error(msg);
    }
    if (payload.result && payload.result.resultCode && payload.result.resultCode !== 'S0000') {
      throw new Error(payload.result.errorMessage || payload.result.resultMessage || 'SSO 로그인에 실패했습니다.');
    }
    const body = payload.body || {};
    if (!body.loggedIn) {
      throw new Error('SSO 로그인에 실패했습니다.');
    }
    syncSessionFromBody(body);
    return body;
  }

  /** local/dev: IdP 없이 mock SSO token으로 OM.Auth.ssoLogin 호출 */
  async function mockSsoLogin(userId, options) {
    const subject = (userId || 'admin01').trim();
    const assertionId = 'SSO-ASSERTION-' + newGuid();
    const ssoToken = 'SSO-MOCK-TOKEN-' + Date.now() + '-' + subject;
    return ssoLogin(ssoToken, subject, {
      ...(options || {}),
      userId: subject,
      ssoAssertionId: assertionId,
      channelId: (options && options.channelId) || 'WEBTOP'
    });
  }

  async function relayMessage(businessCode, request) {
    const code = (businessCode || BUSINESS_CODE).toUpperCase();
    const res = await relayFetch(`/api/relay/${code}/online?${buildRelayQuery()}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(request)
    });
    const relay = await parseRelayResponse(res);
    let payload = null;
    if (relay.responseBody) {
      try {
        payload = JSON.parse(relay.responseBody);
      } catch (e) {
        payload = null;
      }
    }
    return { payload, relay };
  }

  async function call(txKey, body, processingType) {
    const tx = typeof txKey === 'string' ? TX[txKey] : txKey;
    if (!tx || !tx.serviceId) {
      throw new Error(`거래 정의를 찾을 수 없습니다: ${txKey}. 브라우저 강력 새로고침(Ctrl+F5) 후 다시 시도하세요.`);
    }
    if (shouldUseOmGateway(tx)) {
      return callViaGateway(tx, body, processingType);
    }
    const request = { header: buildHeader(tx, processingType), body: body || {} };
    const res = await relayFetch(`/api/relay/${BUSINESS_CODE}/online?${buildRelayQuery()}`, {
      method: 'POST',
      headers: buildOmAuthHeaders(),
      credentials: 'include',
      body: JSON.stringify(request)
    });
    const relay = await parseRelayResponse(res);
    let payload;
    try {
      payload = JSON.parse(relay.responseBody);
    } catch (e) {
      throw new Error('응답 JSON 파싱 실패');
    }
    if (relay.httpStatus >= 400) {
      let msg = payload.result?.errorMessage || payload.result?.message || payload.error;
      if (!msg && relay.httpStatus === 502) {
        msg = isTomcatUiDeployment()
          ? 'tcf-om(/om)에 연결할 수 없습니다. Tomcat 기동 상태를 확인하세요.'
          : 'tcf-om(8097)에 연결할 수 없습니다. tcf-om을 먼저 실행하세요.';
      }
      await notifyTransactionError(payload, relay, msg || `HTTP ${relay.httpStatus}`);
      throw new Error(msg || `HTTP ${relay.httpStatus}`);
    }
    if (payload.result && payload.result.resultCode && payload.result.resultCode !== 'S0000') {
      const detail = payload.result.errorDetail ? ` (${payload.result.errorDetail})` : '';
      const msg = (payload.result.errorMessage || payload.result.resultMessage || '거래 오류') + detail;
      await notifyTransactionError(payload, relay, msg);
      throw new Error(msg);
    }
    return { payload, relay, body: payload.body || {} };
  }

  async function callViaGateway(tx, body, processingType) {
    const request = { header: buildHeader(tx, processingType), body: body || {} };
    const res = await relayFetch(`/api/gateway/om/online?${buildRelayQuery()}`, {
      method: 'POST',
      headers: buildOmAuthHeaders(),
      credentials: 'include',
      body: JSON.stringify(request)
    });
    const relay = await parseRelayResponse(res);
    let payload;
    try {
      payload = JSON.parse(relay.responseBody);
    } catch (e) {
      throw new Error('응답 JSON 파싱 실패');
    }
    if (relay.httpStatus >= 400) {
      let msg = payload.result?.errorMessage || payload.result?.message || payload.error;
      if (!msg && relay.httpStatus === 401) {
        msg = 'JWT 인증에 실패했습니다. JWT 포털에서 다시 로그인하세요.';
      }
      if (!msg && relay.httpStatus === 502) {
        msg = isTomcatUiDeployment()
          ? 'tcf-gateway(/gw) 또는 tcf-om(/om)에 연결할 수 없습니다.'
          : 'tcf-gateway(8101) 또는 tcf-om(8097)에 연결할 수 없습니다.';
      }
      await notifyTransactionError(payload, relay, msg || `HTTP ${relay.httpStatus}`);
      throw new Error(msg || `HTTP ${relay.httpStatus}`);
    }
    if (payload.result && payload.result.resultCode && payload.result.resultCode !== 'S0000') {
      const detail = payload.result.errorDetail ? ` (${payload.result.errorDetail})` : '';
      const msg = (payload.result.errorMessage || payload.result.resultMessage || '거래 오류') + detail;
      await notifyTransactionError(payload, relay, msg);
      throw new Error(msg);
    }
    return { payload, relay, body: payload.body || {} };
  }

  async function inquiry(txKey, body) {
    return call(txKey, body, 'INQUIRY');
  }

  async function mutate(txKey, body, processingType) {
    return call(txKey, body, processingType);
  }

  function field(row, name, fallback) {
    if (!row) return fallback !== undefined ? fallback : '-';
    const raw = row[name];
    if (raw != null && raw !== '' && raw !== 'null') return raw;
    const upper = name.toUpperCase();
    if (row[upper] != null && row[upper] !== '' && row[upper] !== 'null') return row[upper];
    const key = Object.keys(row).find(k => k.toUpperCase() === upper);
    const val = key ? row[key] : undefined;
    if (val != null && val !== '' && val !== 'null') return val;
    return fallback !== undefined ? fallback : '-';
  }

  function chipForHealth(status) {
    const s = String(status || '').toUpperCase();
    if (['NORMAL', 'UP', 'SUCCESS', 'OK'].some(v => s.includes(v))) {
      return `<span class="om-chip ok">${status || 'OK'}</span>`;
    }
    if (['WARN', 'WARNING', '주의'].some(v => s.includes(v))) {
      return `<span class="om-chip warn">${status}</span>`;
    }
    if (['FAIL', 'ERROR', 'DOWN', '장애'].some(v => s.includes(v))) {
      return `<span class="om-chip fail">${status}</span>`;
    }
    return `<span class="om-chip muted">${status || '-'}</span>`;
  }

  function chipForResult(status) {
    const s = String(status || '').toUpperCase();
    if (s.includes('SUCCESS') || s === 'OK') return `<span class="om-chip ok">${status}</span>`;
    if (s.includes('FAIL') || s.includes('ERROR') || s.includes('위반')) {
      return `<span class="om-chip fail">${status}</span>`;
    }
    return `<span class="om-chip muted">${status || '-'}</span>`;
  }

  function renderNavSection(items, pageId) {
    return items.map(item =>
      `<a href="${uiPath(item.href)}" class="${item.id === pageId ? 'active' : ''}">${item.label}</a>`
    ).join('');
  }

  function renderShell(pageId, title) {
    const session = getSession();
    const userLabel = session
      ? `${session.userName || session.userId} (${session.userId})`
      : '';
    document.body.innerHTML = `
      <div class="om-admin">
        <aside class="om-sidebar">
          <div class="om-brand">
            <h1>NSIGHT OM</h1>
            <p>운영관리 포털 · Operation Management</p>
          </div>
          <nav class="om-nav">
            <div class="om-nav-section">
              <div class="om-nav-label">1차 운영관리</div>
              ${renderNavSection(NAV_PRIMARY, pageId)}
            </div>
            <div class="om-nav-section om-nav-runtime">
              <div class="om-nav-label">런타임·장애진단</div>
              ${renderNavSection(NAV_RUNTIME, pageId)}
            </div>
            <div class="om-nav-section">
              <div class="om-nav-label">2차 운영관리</div>
              ${renderNavSection(NAV_SECONDARY, pageId)}
            </div>
            <div class="om-nav-section">
              <div class="om-nav-label">3차 운영관리</div>
              ${renderNavSection(NAV_TERTIARY, pageId)}
            </div>
          </nav>
          <div class="om-nav-footer">
            <a href="${uiPath('/om/index-multi.html')}">↗ API 거래 테스트</a>
            <a href="${uiPath('/index.html')}">← TCF UI 홈</a>
          </div>
        </aside>
        <main class="om-main">
          <header class="om-topbar">
            <h2>${title}</h2>
            <div class="om-topbar-meta">
              ${userLabel ? `<span class="om-user-badge">${userLabel}</span>` : ''}
              <button type="button" class="btn-secondary om-logout-btn" id="omLogoutBtn">로그아웃</button>
              <span id="omTargetUrl" title="tcf-om URL">${targetUrl}</span>
            </div>
          </header>
          <div class="om-content" id="omContent">
            <div class="om-empty">불러오는 중...</div>
          </div>
        </main>
      </div>`;
    const logoutBtn = document.getElementById('omLogoutBtn');
    if (logoutBtn) {
      logoutBtn.addEventListener('click', () => logout());
    }
    return document.getElementById('omContent');
  }

  function renderPagination(container, pageNo, pageSize, totalCount, onPage, prevNextOnly) {
    const totalPages = Math.max(1, Math.ceil(totalCount / pageSize));
    if (totalCount === 0) {
      container.innerHTML = '';
      container.hidden = true;
      return;
    }
    container.hidden = false;
    let nums = '';
    if (!prevNextOnly) {
      for (let i = 1; i <= totalPages; i += 1) {
        nums += `<button type="button" class="om-page-btn ${i === pageNo ? 'active' : ''}" data-page="${i}">${i}</button>`;
      }
    }
    container.innerHTML = `
      <button type="button" class="om-page-btn" data-page="prev" ${pageNo <= 1 ? 'disabled' : ''}>PREV</button>
      ${nums}
      <button type="button" class="om-page-btn" data-page="next" ${pageNo >= totalPages ? 'disabled' : ''}>NEXT</button>
      <span style="color:var(--muted);font-size:0.85rem;margin-left:8px">${pageNo} / ${totalPages} · 총 ${totalCount}건</span>`;
    container.querySelectorAll('[data-page]').forEach(btn => {
      btn.addEventListener('click', () => {
        const v = btn.getAttribute('data-page');
        if (v === 'prev' && pageNo > 1) onPage(pageNo - 1);
        else if (v === 'next' && pageNo < totalPages) onPage(pageNo + 1);
        else if (v !== 'prev' && v !== 'next') onPage(Number(v));
      });
    });
  }

  function showError(container, message) {
    const hint = targetUrl && targetUrl !== '-'
      ? `릴레이 대상: <code>${targetUrl}</code>`
      : config.deploymentMode === 'tomcat'
      ? 'Tomcat(8080)에서 /om, /ui WAR 배포를 확인하세요.'
      : 'tcf-om(포트 8097)와 tcf-ui(8099)를 함께 기동했는지 확인하세요.';
    container.innerHTML = `<div class="om-alert error">${message}<br><small>${hint}</small></div>`;
  }

  function showErrorBanner(container, message) {
    const existing = container.querySelector('.om-load-error');
    if (existing) existing.remove();
    const banner = document.createElement('div');
    banner.className = 'om-alert error om-load-error';
    banner.innerHTML = `${message}<br><small>${hintForOmError(message)}</small>`;
    container.prepend(banner);
  }

  function hintForOmError(message) {
    const msg = String(message || '');
    if (msg.includes('isTomcatUiDeployment is not a function') || msg.includes('거래 정의를 찾을 수 없습니다')) {
      return 'om-admin.js가 오래된 버전일 수 있습니다. Ctrl+F5로 강력 새로고침하세요.';
    }
    if (msg.includes('등록되지 않은 serviceId') || msg.includes('OM.Dashboard.reset')
        || msg.includes('OM.AuditLog.deleteAll') || msg.includes('OM.AuthHistory.deleteAll')
        || msg.includes('OM.TimeoutPolicy.')) {
      return 'gradle :tcf-om:bootRun 으로 tcf-om을 재기동하세요. (신규 Handler 등록 필요)';
    }
    const hint = targetUrl && targetUrl !== '-' ? ` (${targetUrl})` : '';
    return `tcf-om을 재빌드 후 NsightTcfOmApplication을 재시작하세요.${hint}`;
  }

  async function pingBackend() {
    try {
      const jwt = getJwtSession();
      const targetPath = (config.omGatewayEnabled && jwt && jwt.accessToken)
        ? `/api/gateway/om/target-url?${buildRelayQuery()}`
        : `/api/business-modules/${BUSINESS_CODE}/target-url?${buildRelayQuery()}`;
      const res = await fetch(uiPath(targetPath));
      if (!res.ok) return false;
      const data = await res.json();
      targetUrl = data.targetUrl || targetUrl;
      const el = document.getElementById('omTargetUrl');
      if (el) el.textContent = targetUrl;
      return true;
    } catch (e) {
      return false;
    }
  }

  function resolveUpdownloadDeploymentMode() {
    if (isTomcatUiDeployment()) return 'tomcat';
    if (config.omGatewayEnabled !== false) return 'tomcat';
    return config.deploymentMode || 'bootrun';
  }

  function updownloadQuery(extra) {
    const params = new URLSearchParams({
      deploymentMode: resolveUpdownloadDeploymentMode(),
      bootrunHost: config.bootrunHost,
      tomcatGatewayUrl: config.tomcatGatewayUrl || 'http://localhost:8080'
    });
    if (extra) {
      Object.entries(extra).forEach(([k, v]) => {
        if (v != null && v !== '') params.set(k, v);
      });
    }
    return params.toString();
  }

  async function updownloadBaseUrl() {
    const res = await fetch(uiPath(`/api/updownload/base-url?${updownloadQuery()}`));
    if (!res.ok) throw new Error('UD 서비스 URL을 확인할 수 없습니다.');
    return res.json();
  }

  async function updownloadList(filters) {
    const res = await fetch(uiPath(`/api/updownload/files?${updownloadQuery(filters)}`));
    const text = await res.text();
    let payload;
    try {
      payload = JSON.parse(text);
    } catch (e) {
      throw new Error('파일 목록 응답 파싱 실패');
    }
    if (payload.body && payload.body.error) {
      throw new Error(payload.body.hint || payload.body.error);
    }
    if (payload.result && (payload.result.resultCode === 'E0001' || payload.result.status === 'ERROR')) {
      throw new Error(payload.result.resultMessage || payload.result.errorMessage || '목록 조회 실패');
    }
    return payload.body || {};
  }

  async function updownloadUpload(file, description, businessCode) {
    const session = getSession();
    const formData = new FormData();
    formData.append('file', file);
    formData.append('userId', session && session.userId ? session.userId : 'GUEST');
    if (description) formData.append('description', description);
    if (businessCode) formData.append('businessCode', businessCode);
    const res = await fetch(uiPath(`/api/updownload/upload?${updownloadQuery()}`), { method: 'POST', body: formData });
    const text = await res.text();
    let payload;
    try {
      payload = JSON.parse(text);
    } catch (e) {
      throw new Error(text || '업로드 실패');
    }
    if (payload.result && payload.result.resultCode && payload.result.resultCode.startsWith('E')) {
      throw new Error(payload.result.resultMessage || '업로드 실패');
    }
    if (!payload.body || !payload.body.file) {
      throw new Error('업로드 응답이 올바르지 않습니다.');
    }
    return payload.body.file;
  }

  async function updownloadDelete(fileId) {
    const res = await fetch(uiPath(`/api/updownload/files/${encodeURIComponent(fileId)}?${updownloadQuery()}`), { method: 'DELETE' });
    const payload = await res.json();
    if (!payload.body || !payload.body.deleted) {
      throw new Error(payload.result?.resultMessage || '삭제 실패');
    }
    return payload.body;
  }

  function updownloadDownloadUrl(fileId) {
    const session = getSession();
    const userId = session && session.userId ? session.userId : 'GUEST';
    return uiPath(`/api/updownload/files/${encodeURIComponent(fileId)}/download?${updownloadQuery({ userId })}`);
  }

  async function updownloadDetail(fileId) {
    const res = await fetch(uiPath(`/api/updownload/files/${encodeURIComponent(fileId)}?${updownloadQuery()}`));
    const payload = await res.json();
    if (payload.body && payload.body.error) {
      throw new Error(payload.body.error);
    }
    if (payload.result && payload.result.status === 'ERROR') {
      throw new Error(payload.result.resultMessage || '상세 조회 실패');
    }
    if (!payload.body || !payload.body.file) {
      throw new Error('파일 정보를 찾을 수 없습니다.');
    }
    return payload.body.file;
  }

  async function updownloadUpdate(fileId, description) {
    const res = await fetch(
      uiPath(`/api/updownload/files/${encodeURIComponent(fileId)}?${updownloadQuery({ description: description || '' })}`),
      { method: 'PUT' }
    );
    const payload = await res.json();
    if (payload.body && payload.body.error) {
      throw new Error(payload.body.error);
    }
    if (payload.result && payload.result.status === 'ERROR') {
      throw new Error(payload.result.resultMessage || '수정 실패');
    }
    if (!payload.body || !payload.body.file) {
      throw new Error('수정 응답이 올바르지 않습니다.');
    }
    return payload.body.file;
  }

  async function loadCommonCodes(codeGroup, options) {
    const opts = options || {};
    if (opts.forceRefresh) {
      invalidateCommonCodeCache(codeGroup);
    }
    const memKey = commonCodeCacheKey(codeGroup, opts.useYn);
    if (!opts.forceRefresh && commonCodeMemoryCache.has(memKey)) {
      return commonCodeMemoryCache.get(memKey);
    }
    const body = {
      codeGroup,
      pageNo: 1,
      pageSize: opts.pageSize || 500
    };
    if (opts.useYn != null && opts.useYn !== '') {
      body.useYn = opts.useYn;
    }
    const { body: data } = await inquiry('commonCode', body);
    const rows = sortCommonCodeRows(data.rows || []);
    if (data.fromCache !== false) {
      commonCodeMemoryCache.set(memKey, rows);
    }
    return rows;
  }

  async function loadCodeGroups(options) {
    const opts = options || {};
    if (opts.forceRefresh) {
      commonCodeMemoryCache.delete('__groups__');
    }
    if (!opts.forceRefresh && commonCodeMemoryCache.has('__groups__')) {
      return commonCodeMemoryCache.get('__groups__');
    }
    const body = { pageNo: 1, pageSize: 500 };
    if (opts.useYn != null && opts.useYn !== '') {
      body.useYn = opts.useYn;
    }
    const { body: data } = await inquiry('commonCode', body);
    const groups = [];
    (data.rows || []).forEach(row => {
      const code = field(row, 'codeGroup', '') || field(row, 'code', '');
      if (!code) return;
      groups.push({ code, codeName: field(row, 'codeName', code) });
    });
    const sorted = groups.sort((a, b) => a.code.localeCompare(b.code));
    if (data.fromCache) {
      commonCodeMemoryCache.set('__groups__', sorted);
    }
    return sorted;
  }

  const commonCodeMemoryCache = new Map();
  const DEFAULT_PREFETCH_CODE_GROUPS = ['BUSINESS_CODE', 'AUTH_CODE', 'CACHE_NAME'];

  function commonCodeCacheKey(codeGroup, useYn) {
    const yn = useYn != null && useYn !== '' ? useYn : 'ALL';
    return `${codeGroup}|${yn}`;
  }

  function sortCommonCodeRows(rows) {
    return rows.slice().sort((a, b) => {
      const sa = Number(field(a, 'sortOrder', 0));
      const sb = Number(field(b, 'sortOrder', 0));
      if (sa !== sb) return sa - sb;
      return String(field(a, 'code', '')).localeCompare(String(field(b, 'code', '')));
    });
  }

  function invalidateCommonCodeCache(codeGroup) {
    if (codeGroup) {
      [...commonCodeMemoryCache.keys()]
        .filter(k => k.startsWith(`${codeGroup}|`))
        .forEach(k => commonCodeMemoryCache.delete(k));
    } else {
      commonCodeMemoryCache.clear();
    }
    commonCodeMemoryCache.delete('__groups__');
  }

  async function prefetchCommonCodes(codeGroups, options) {
    const groups = codeGroups && codeGroups.length ? codeGroups : DEFAULT_PREFETCH_CODE_GROUPS;
    await Promise.all(groups.map(g => loadCommonCodes(g, options).catch(() => [])));
  }

  function fillCodeSelect(selectEl, codes, options) {
    if (!selectEl) return;
    const opts = options || {};
    const includeAll = !!opts.includeAll;
    const allLabel = opts.allLabel || '전체';
    const selected = opts.selected != null ? opts.selected : '';
    const parts = [];
    if (includeAll) {
      parts.push(`<option value="">${allLabel}</option>`);
    }
    (codes || []).forEach(row => {
      const code = field(row, 'code', '');
      const name = field(row, 'codeName', '');
      const label = name && name !== '-' ? `${code} · ${name}` : code;
      parts.push(`<option value="${code}">${label}</option>`);
    });
    selectEl.innerHTML = parts.join('');
    if (selected !== '') {
      selectEl.value = selected;
    }
    if (selected && selectEl.value !== selected && codes && codes.length) {
      selectEl.selectedIndex = includeAll ? 1 : 0;
    }
  }

  function formatCodeLabel(codes, code) {
    if (!code || code === '-') return '-';
    const row = (codes || []).find(r => field(r, 'code') === code);
    if (!row) return code;
    const name = field(row, 'codeName', '');
    return name && name !== '-' ? `${code} (${name})` : code;
  }

  async function initPage(pageId, title, renderFn) {
    if (!window.__NSIGHT_UI_CONTEXT_INIT__) {
      const script = document.createElement('script');
      script.src = uiContextPrefix() + '/_shared/ui-context.js';
      document.head.appendChild(script);
    }
    const session = await requireAuth();
    if (!session) {
      return;
    }
    await loadConfig();
    const container = renderShell(pageId, title);
    const ok = await pingBackend();
    if (!ok) {
      showError(container, 'tcf-ui → tcf-om 릴레이 URL을 확인할 수 없습니다.');
      return;
    }
    try {
      prefetchCommonCodes(DEFAULT_PREFETCH_CODE_GROUPS, { useYn: 'Y' }).catch(() => {});
      await renderFn(container);
    } catch (err) {
      showErrorPopup({
        errorMessage: err.message || String(err),
        errorDetail: err.stack || '',
        systemNote: 'OM Admin 화면'
      });
      showError(container, err.message || String(err));
    }
  }

  return {
    NAV, NAV_RUNTIME, TX, config, targetUrl, SESSION_KEY,
    todayIsoDate, todaySystemDate, newGuid, nowIsoKst, field, uiPath,
    buildStandardHeader, relayMessage,
    chipForHealth, chipForResult,
    getSession, setSession, clearSession, syncSessionFromBody, requireAuth, logout, login, ssoLogin, mockSsoLogin,
    inquiry, mutate, call, initPage, renderPagination, showError, showErrorBanner, showErrorPopup, loadConfig,
    isTomcatUiDeployment, resolveBatchServiceUrl, resolveBatchLabel,
    updownloadQuery, updownloadBaseUrl, updownloadList, updownloadUpload, updownloadDelete, updownloadDownloadUrl,
    updownloadDetail, updownloadUpdate,
    loadCommonCodes, loadCodeGroups, fillCodeSelect, formatCodeLabel,
    invalidateCommonCodeCache, prefetchCommonCodes
  };
})();

(function patchOmAdminExports() {
  const oa = window.OmAdmin;
  if (!oa) return;
  if (typeof oa.isTomcatUiDeployment !== 'function') {
    oa.isTomcatUiDeployment = function () {
      const cfg = oa.config || {};
      return cfg.deploymentMode === 'tomcat'
          || location.pathname.startsWith('/ui/') || location.pathname === '/ui';
    };
  }
  if (typeof oa.resolveBatchServiceUrl !== 'function') {
    oa.resolveBatchServiceUrl = function () {
      if (oa.isTomcatUiDeployment()) {
        const gateway = ((oa.config && oa.config.tomcatGatewayUrl) || 'http://localhost:8080').replace(/\/$/, '');
        return `${gateway}/batch`;
      }
      const host = ((oa.config && oa.config.bootrunHost) || 'http://127.0.0.1').replace(/\/$/, '');
      return `${host}:8098/batch`;
    };
  }
  if (typeof oa.resolveBatchLabel !== 'function') {
    oa.resolveBatchLabel = function () {
      if (oa.isTomcatUiDeployment()) {
        try {
          const u = new URL(((oa.config && oa.config.tomcatGatewayUrl) || 'http://localhost:8080').replace(/\/$/, ''));
          const port = u.port || (u.protocol === 'https:' ? '443' : '80');
          return `Tomcat /batch (${port})`;
        } catch (e) {
          return 'Tomcat /batch (8080)';
        }
      }
      return 'tcf-batch (:8098)';
    };
  }
})();
