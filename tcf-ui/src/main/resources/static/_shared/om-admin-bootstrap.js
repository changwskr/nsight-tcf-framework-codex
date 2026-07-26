/**
 * OM Admin 구버전 om-admin.js 호환 패치 (캐시된 JS 대응)
 * om-admin.js 로드 전에 실행되어 window.OmAdmin 할당 시 export를 보완합니다.
 */
(function () {
  function patch(oa) {
    if (!oa || typeof oa.isTomcatUiDeployment === 'function') {
      return;
    }
    oa.isTomcatUiDeployment = function () {
      const cfg = oa.config || {};
      return cfg.deploymentMode === 'tomcat'
          || location.pathname.startsWith('/ui/') || location.pathname === '/ui';
    };
    if (typeof oa.resolveBatchServiceUrl !== 'function') {
      oa.resolveBatchServiceUrl = function () {
        if (oa.isTomcatUiDeployment()) {
          const gateway = ((oa.config && oa.config.tomcatGatewayUrl) || 'http://localhost:8080').replace(/\/$/, '');
          return gateway + '/batch';
        }
        const host = ((oa.config && oa.config.bootrunHost) || 'http://127.0.0.1').replace(/\/$/, '');
        return host + ':8098/batch';
      };
    }
    if (typeof oa.resolveBatchLabel !== 'function') {
      oa.resolveBatchLabel = function () {
        if (oa.isTomcatUiDeployment()) {
          try {
            const u = new URL(((oa.config && oa.config.tomcatGatewayUrl) || 'http://localhost:8080').replace(/\/$/, ''));
            const port = u.port || (u.protocol === 'https:' ? '443' : '80');
            return 'Tomcat /batch (' + port + ')';
          } catch (e) {
            return 'Tomcat /batch (8080)';
          }
        }
        return 'tcf-batch (:8098)';
      };
    }
  }

  patch(window.OmAdmin);

  let omAdminRef = window.OmAdmin;
  try {
    Object.defineProperty(window, 'OmAdmin', {
      configurable: true,
      enumerable: true,
      get: function () {
        return omAdminRef;
      },
      set: function (value) {
        omAdminRef = value;
        patch(value);
      }
    });
  } catch (e) {
    /* defineProperty 실패 시 기존 객체에만 패치 */
  }
})();
