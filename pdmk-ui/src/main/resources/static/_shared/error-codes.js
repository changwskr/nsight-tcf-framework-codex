/**
 * pdmk-ui 오류코드 사전.
 * 서버 exceptionCode.yml / GlobalExceptionHandler / 브라우저 중계 오류를 코드별로 정리한다.
 */
(function (global) {
  /** @type {Record<string, {title:string, summary:string, hint?:string, severity?:'error'|'warning'|'info'}>} */
  const BY_CODE = {
    FW0001: {
      title: '일반 오류',
      summary: '요청 처리 중 오류가 발생했습니다.',
      hint: '입력값을 확인한 뒤 다시 시도하세요.',
      severity: 'error'
    },
    FW0002: {
      title: '테스트 예외',
      summary: '테스트용 예외가 발생했습니다.',
      hint: '개발/검증 목적의 예외입니다.',
      severity: 'info'
    },
    FW0003: {
      title: '예외 문의 필요',
      summary: '해당 예외는 안내된 문의처로 확인이 필요합니다.',
      hint: '화면/로그의 상세 메시지를 함께 전달하세요.',
      severity: 'warning'
    },
    FW0401: {
      title: '인증 실패',
      summary: '인증에 실패했습니다. 다시 로그인해 주세요.',
      hint: '세션이 만료되었거나 토큰이 유효하지 않을 수 있습니다.',
      severity: 'warning'
    },
    FW0403: {
      title: '권한 없음',
      summary: '요청한 거래에 대한 권한이 없습니다.',
      hint: '권한 부여 여부를 관리자에게 확인하세요.',
      severity: 'warning'
    },
    FW9999: {
      title: '시스템 오류',
      summary: '예기치 않은 시스템 오류가 발생했습니다.',
      hint: '잠시 후 다시 시도하고, 반복되면 시스템 관리자에게 문의하세요.',
      severity: 'error'
    },
    FW_TIMEOUT: {
      title: '처리 시간 초과',
      summary: '온라인 거래 처리 허용 시간을 초과했습니다.',
      hint: '조건을 줄이거나 잠시 후 다시 시도하세요. (서버 타임아웃)',
      severity: 'warning'
    },
    FW_OVERLOADED: {
      title: '서버 혼잡',
      summary: '온라인 거래 요청이 일시적으로 많습니다.',
      hint: '잠시 후 다시 시도하세요. (서버 과부하)',
      severity: 'warning'
    },
    MP0404: {
      title: '데이터 없음',
      summary: '요청한 영업팁 실적을 찾을 수 없습니다.',
      hint: '조회 조건을 확인하세요.',
      severity: 'info'
    },
    MP0409: {
      title: '중복 데이터',
      summary: '동일한 기본키의 영업팁 실적이 이미 존재합니다.',
      hint: '기존 데이터를 확인한 뒤 다시 등록하세요.',
      severity: 'warning'
    },
    E9999: {
      title: '서비스 미등록',
      summary: '요청한 서비스 핸들러를 찾을 수 없습니다.',
      hint: '거래 ID(serviceId)와 배포 구성을 확인하세요.',
      severity: 'error'
    },
    UI_TIMEOUT: {
      title: '브라우저 요청 시간 초과',
      summary: '브라우저에서 응답 대기 시간을 초과했습니다.',
      hint: 'pdmk-service 기동 여부와 UI 타임아웃(ms) 설정을 확인하세요.',
      severity: 'warning'
    },
    UI_NETWORK: {
      title: '네트워크 오류',
      summary: 'pdmk-service에 연결하지 못했습니다.',
      hint: '서비스 기동, URL, CORS 허용 여부를 확인하세요.',
      severity: 'error'
    },
    HTTP_401: {
      title: '인증 필요',
      summary: '로그인이 필요하거나 인증이 만료되었습니다.',
      severity: 'warning'
    },
    HTTP_403: {
      title: '접근 거부',
      summary: '요청이 거부되었습니다.',
      severity: 'warning'
    },
    HTTP_404: {
      title: '리소스 없음',
      summary: '요청한 주소 또는 거래를 찾을 수 없습니다.',
      hint: '대상 URL과 거래 ID를 확인하세요.',
      severity: 'info'
    },
    HTTP_500: {
      title: '서버 내부 오류',
      summary: '서버에서 요청을 처리하지 못했습니다.',
      hint: '서버 로그와 이미지로그를 확인하세요.',
      severity: 'error'
    },
    HTTP_502: {
      title: '게이트웨이 오류',
      summary: '상위/대상 서버로부터 유효한 응답을 받지 못했습니다.',
      severity: 'error'
    },
    HTTP_503: {
      title: '서비스 일시 불가',
      summary: '서비스가 일시적으로 사용할 수 없습니다.',
      hint: '잠시 후 다시 시도하세요.',
      severity: 'warning'
    },
    HTTP_504: {
      title: '게이트웨이 시간 초과',
      summary: '서버 응답 대기 시간이 초과되었습니다.',
      hint: '서버 타임아웃 또는 브라우저 Abort일 수 있습니다.',
      severity: 'warning'
    }
  };

  const DEFAULT = {
    title: '오류',
    summary: '요청 처리 중 오류가 발생했습니다.',
    hint: '상세 정보와 전체 로그를 확인해 주세요.',
    severity: 'error'
  };

  function normalizeCode(code) {
    if (code == null) {
      return '';
    }
    return String(code).trim();
  }

  function lookup(code, httpStatus) {
    const normalized = normalizeCode(code);
    if (normalized && BY_CODE[normalized]) {
      return Object.assign({ code: normalized }, BY_CODE[normalized]);
    }
    if (httpStatus != null && Number.isFinite(Number(httpStatus))) {
      const httpKey = 'HTTP_' + Number(httpStatus);
      if (BY_CODE[httpKey]) {
        return Object.assign({ code: httpKey }, BY_CODE[httpKey]);
      }
    }
    return Object.assign({ code: normalized || null }, DEFAULT);
  }

  /**
   * 서버 메시지에 {0}/{1} 플레이스홀더가 남아 있으면 카탈로그 summary를 우선한다.
   */
  function resolveDisplay(code, httpStatus, serverMessage) {
    const entry = lookup(code, httpStatus);
    const server = serverMessage == null ? '' : String(serverMessage).trim();
    const looksUnresolved = /\{[0-9]+\}/.test(server) || server === '';
    return {
      code: entry.code || normalizeCode(code) || null,
      title: entry.title,
      summary: looksUnresolved ? entry.summary : server,
      catalogSummary: entry.summary,
      hint: entry.hint || '',
      severity: entry.severity || 'error',
      serverMessage: server
    };
  }

  global.PdmkErrorCodes = {
    BY_CODE,
    DEFAULT,
    lookup,
    resolveDisplay
  };
})(window);
