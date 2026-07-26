'use strict';

/** docx plain-text → readable Markdown (tables, code blocks, headings) */

const HEADER_WORDS = new Set([
  'No', '구분', '항목', '번호', '업무', '업무구분', '업무코드', 'ServiceId', '설명', '예시', '원칙',
  '필드', 'Layer', '계층', '역할', '책임', '대상', '유형', '상태', '값', '기준', '방식', 'Module',
  'Package', 'Context', 'WAR', 'Gradle', '처리', '검증', '단계', '확인', '조치', '증상', '원인',
  '개발', '통신', 'Method', 'Endpoint', '영문', '영문명', '처리유형', 'Action', 'SQL', 'Mapper',
  'Handler', 'Facade', 'Service', 'Rule', 'DAO', '오류', '로그', '환경', '브랜치', '작업', 'Gate',
  '필수', '선택', '장', '제목', '주요', '내용', '개발 원칙', '핵심 의미', '개발 기준', '의미',
  '업무 설명', 'Gradle Module', 'Java Prefix', 'Package', 'Java Package', 'Mapper Path',
  '온라인 Endpoint', '거래코드', '로그 경로', '표준', '항목', '예시', '설명', 'Endpoint',
  '원칙', '기준', '표기 기준', '설계 기준', '문제', '표준', '역할', '영역', 'Prefix',
  '처리행위', '업무대상', '업무', '의미', 'TTL', 'Evict', 'Cache Name', '권장 업무명',
  'Java Prefix', 'ServiceId Prefix', '업무 설명', 'Gradle Module', 'Online Endpoint',
  'transactionCode', 'businessCode', 'serviceId', 'WAR', 'Context', 'Module',
  'DB', '모듈', '식별자', '구현 대상', '금지 영역', '금지 사항', 'OM 관리 항목',
  '영향', '확인 항목', '추적 항목', '금지 패턴', '담당 영역', '공통 처리',
  'Package', '처리유형', '관리 항목', '로그 항목', '보안 영역', '효과',
  '책임 영역', '시사점', 'RACI', '역할', '목표', '확인 목적', '테스트 내용', '금지사항',
  '핵심 기준', 'Git Commit 가능 여부', '로컬 Port 예시', '로컬 URL', '사용 시점',
  '설치 항목', '확인 명령', '정상 예시', '주의사항', '순서', '방식', '테스트 대상', '로컬 기준',
  'Type', 'Scope', '변경 영역', 'Merge', '삭제', 'Plugin', '파일', '패키지', '디렉터리',
  'WAR', 'Tag', 'Claim', 'Token', 'Level', 'Method', '컬럼', '파라미터', '태그',
  '증상', '원인', 'Timeout', 'Rollback', '장점', '환경', '증상', '리뷰', '대상',
]);

const EXACT_HEADER_LABELS = new Set([
  'Gradle Module', 'Java Prefix', '업무 설명', '표기 기준', '설계 기준', '권장 업무명',
  'ServiceId Prefix', '잘못된 예', 'Cache Name', 'Online Endpoint', 'transactionCode',
  'businessCode', 'serviceId', '처리행위', '업무대상', '구성요소', 'TTL 권장',
  'Evict 시점', 'Key 예시', '주요 Key 예시', '예시 ServiceId', '형식',
  '추적 기준', '반드시 포함할 값', '중복 표현', '표준화 기준', '명명 기준', '사용 위치', '사용 방식',
  '표준 테이블명', '모듈 영역', '모듈명', '질문', '위치', '한 줄 기준',
  '개발자가 알아야 할 내용', '개발자 관점', '개발자 관점 역할', '개발자 주의사항',
  '개발자와의 관계', '작성 기준', '구현 대상', '금지 영역', 'OM 관리 항목', '구성 요소',
  '확인 항목', '추적 항목', '업무 개발자 기준', '개발자 책임', 'Timeout 구분', '담당 영역',
  '공통 처리', '금지 패턴', '영향', '표준 규칙', '표준 기준', '표준',
  '작성 기준', '처리유형', '권장 행위명', '관리 항목', 'DTO 유형',
  'SQL ID 예시', '로그 항목', '보안 영역', '거래 유형', 'Cache 대상',
  '캐시 여부', '테스트 유형', '효과', '나쁜 예',
  '책임 영역', '주요 책임', '테스트 영역', '확인 목적', '주요 산출물', '완료 책임',
  '시사점', 'RACI', '목표', '핵심 기준', '금지사항', '테스트 대상', '테스트 내용',
  '로컬 기준', '주의사항', '사용 시점', '설치 항목', '확인 명령', '정상 예시',
  '로컬 Port 예시', '로컬 URL', 'Git Commit 가능 여부', '순서', '방식',
  '관리 기준', '보호 수준', 'Merge 대상', '삭제 여부', '생성 기준', '브랜치 표기',
  '좋은 Commit', '나쁜 Commit', '변경 영역', '승인 기준', 'Type', 'Scope',
  '대표 클래스', '명명 기준', 'DTO 유형', '검증 위치', '검증 대상', '검증 유형',
  '허용 여부', '작성 기준', '적용 Plugin', '산출물', 'Plugin', 'WAR 파일명',
  'Base Package', 'Context Path', 'Gradle Module', 'Blocker', '리뷰 범위', '리뷰 항목',
  '확인 기준', '사용 기준', '사용 여부', '관리 주체', '등록 주체', '테스트 목적',
  'bootRun', 'Tomcat WAR', 'Tomcat WAR 배포', '개발 관점', '처리 내용', '적용 게이트',
  '검증 대상', '금지 Endpoint', '권장 Endpoint', '문제점', '금지 Method', '문제점',
  '호출 관계', '분리 기준', '명명 예시', '권장 위치', '검증 단계', '배포 방식', '빌드 범위',
  '파일 유형', 'Cache Name', '금지 기준', '금지 의존성', '설정 파일', 'WAR 파일',
  '처리 유형', 'Action', '사용 예시', 'idempotencyKey 기준', '거래 유형', '대상 업무 WAR',
  'Gateway 내부 라우팅', '외부 호출', '적용 기준', '역할', '비고', '예시', '의미', '기준',
  '설명', '용도', 'Plugin', '산출물', '대상', '판단', '확인', '검증', '내용', '사유',
  '형식', '표준', '규칙', '원칙', '방식', '유형', '구분', '항목', '영역', '필드', '모듈',
  '패키지', '파일', 'Tag', 'Claim', 'Token', 'Level', 'Method', 'URL', '컬럼', '파라미터',
  '태그', '증상', '원인', 'Timeout', 'Rollback', '장점', '환경', 'TYPE', 'Step', 'Category',
]);

function decodeEntities(text) {
  return text
    .replace(/&quot;/g, '"')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&#(\d+);/g, (_, n) => String.fromCharCode(Number(n)));
}

function escapeCell(s) {
  return String(s ?? '')
    .replace(/\|/g, '\\|')
    .replace(/\n/g, ' ')
    .trim();
}

function mdTable(headers, rows) {
  const h = headers.map(escapeCell);
  const sep = h.map(() => '---');
  return [
    `| ${h.join(' | ')} |`,
    `| ${sep.join(' | ')} |`,
    ...rows.map((r) => `| ${r.map(escapeCell).join(' | ')} |`),
  ].join('\n');
}

function isSectionHeading(line) {
  const t = line.trim();
  return (
    /^(\d+\.)+\s+\S/.test(t) ||
    /^\d+\.\d+\s+\S/.test(t) ||
    /^[A-J]\.\d+/.test(t) ||
    /^부록\s+[A-J]\./.test(t) ||
    /^[A-J]\.\s+\S/.test(t)
  );
}

function isProseLine(line) {
  const t = line.trim();
  if (!t) return false;
  if (isSectionHeading(t)) return false;
  if (t.length <= 45) return false;
  if (t.length > 55 && /[다요]\.$/.test(t)) return true;
  if (t.length > 90) return true;
  if (/^(본 장|따라서|NSIGHT TCF Framework는|TCF Framework는|개발자는 업무|일반 REST|그러나 NSIGHT)/.test(t)) return true;
  return false;
}

function isStrictHeaderCell(line) {
  const t = line.trim();
  if (!t || t.length > 40) return false;
  if (EXACT_HEADER_LABELS.has(t)) return true;
  if (isSectionHeading(t)) return false;
  if (/^[\d]+$/.test(t)) return false;
  if (/^[{}[\]"]/.test(t)) return false;
  if (/HTTP|POST|GET|https?:|\/\{|\//.test(t)) return false;
  if (/^[a-z.]+\.[a-z]+/.test(t)) return false;
  if (/^[A-Z]{2,3}$/.test(t)) {
    return HEADER_WORDS.has(t) || t === 'WAR';
  }
  if (HEADER_WORDS.has(t)) return true;
  if (/^[A-Za-z][A-Za-z\s]{0,18}$/.test(t)) return true;
  if (/^[가-힣A-Za-z\s]{2,16}$/.test(t) && !/[다요]\.$/.test(t)) return true;
  return false;
}

const TWO_COL_HEADERS = {
  구분: ['표준', '개발 기준', '설계 기준', '설명', '예시', '값', '기준', 'bootRun', 'Tomcat WAR 배포', '내용', '테스트 기준', '표준 예시', '표준 기준', '표준 규칙', '역할', '적용 범위', '개발자가 알아야 할 내용', '작성 기준'],
  항목: ['예시', '설명', '값', '기준', '개발 기준', '표준', '필수'],
  목적: ['설명'],
  Layer: ['Package', '역할', '설명'],
  계층: ['역할', '책임', '설명'],
  ServiceId: ['의미'],
  'SQL ID': ['설명'],
  '잘못된 예': ['문제'],
  검증: ['기준'],
  테스트: ['목적', '대상'],
  MDC: ['Key'],
  파일: ['역할', '유형'],
  패키지: ['역할', '명명 기준', 'DTO 유형'],
  모듈: ['적용 Plugin', '로컬 실행'],
  영역: ['역할', '관리 기준', '설명'],
  WAR: ['파일', 'ServiceId'],
  Tag: ['의미'],
  Type: ['의미'],
  Scope: ['의미'],
  컬럼: ['설명'],
  Method: ['URL', '설명'],
  Cache: ['Name', '대상'],
  Level: ['사용 기준'],
  Token: ['용도'],
  Claim: ['설명'],
  파라미터: ['설명'],
  태그: ['사용 기준'],
  역할: ['책임'],
  대상: ['리뷰 범위', '예시'],
  리뷰: ['항목'],
  확인: ['기준'],
  증상: ['예시'],
  원인: ['설명'],
  Timeout: ['설명'],
  장점: ['설명'],
  환경: ['기준'],
  호출: ['관계', '허용 여부'],
  분리: ['기준'],
  명명: ['예시'],
  권장: ['위치'],
  배포: ['방식'],
  빌드: ['범위'],
  적용: ['게이트', 'Plugin'],
  금지: ['의존성', 'Method', 'Endpoint', '기준'],
  처리: ['유형', '위치'],
  순서: ['작업', '처리', '판단', '책임', '설치 항목'],
  TYPE: ['의미'],
  Log: ['역할'],
  Profile: ['용도'],
  Evict: ['대상'],
  Rollback: ['기준'],
  사용: ['기준'],
  시사점: ['개발 관점'],
  브랜치: ['용도', '보호 수준'],
  업무: ['도메인', 'ServiceId'],
  Handler: ['역할'],
  메서드: ['설명'],
  거래: ['유형'],
  변환: ['권장 위치'],
};

/** 2열 표 헤더 쌍 (h0|h1) */
const TWO_COL_PAIRS = new Set([
  '영역|관리 기준', '파일|역할', '패키지|역할', '패키지|명명 기준', '설정 파일|역할',
  'WAR 파일|Context', '금지 의존성|사유', 'Tag|의미', '역할|책임', '리뷰 항목|기준',
  '대상|리뷰 범위', '확인 기준|확인', '컬럼|설명', 'Method|URL', '파라미터|설명',
  'Cache Name|용도', '금지 기준|사유', 'Level|사용 기준', '적용 게이트|설명', '환경|기준',
  'Token|용도', 'Claim|설명', '테스트 항목|기준', '로그|역할', '대상|예시',
  '위치|작성 기준', '태그|사용 기준', '시사점|개발 관점', '증상|예시', '주요 원인|확인',
  '원인|설명', '장점|설명', 'Rollback 기준|설명', '사용 기준|설명', '해야 하는 일|설명',
  '금지 Method|문제점', '호출 관계|허용 여부', '분리 기준|설명', '명명 예시|설명',
  '권장 위치|설명', '검증 단계|위치', '배포 방식|기준', '빌드 범위|이유', '파일 유형|예시',
  '검증 항목|기준', '처리|위치', '메서드|설명', '업무|도메인', '업무|ServiceId',
  'Handler|역할', 'TYPE|의미', 'Type|의미', 'Scope|의미', '구분|기준', '구분|의미',
  '항목|기준', '항목|설명', '항목|값', '항목|필수', '원칙|설명', '기준|설명',
  '기준|역할', '기준|예시', '기준|위치', '효과|설명', '설명|예시', '의미|예시',
  '금지사항|사유', '점검 항목|확인', '변경 영역|승인 기준', '좋은 Commit|나쁜 Commit',
  '브랜치|보호 수준', '업무코드|브랜치 표기', 'Tag|의미', '전체 업무 WAR|세션 검증 위치',
  '필요한 이유|bootRun', '디렉터리|역할', 'DOMAIN|의미', 'CATEGORY|의미',
  '공통 패키지|역할', '금지 Import|사유', '금지 항목|사유', '확인 항목|기준',
  '가능 원인|확인', '가능 원인|설명', '패턴|기준', '조합|기준', '이유|설명',
  'Gate|기준', '판정|기준', '성능 항목|기준', '데이터 유형|기준', '목표|합격 기준',
  'DB|규칙', 'SQL ID|예시', 'Annotation|용도', '사용 예|개발 기준',
  '권한코드|설명', '권한 유형|검증 위치', '오류코드 형식|예시', '테스트 구분|테스트 내용',
  'Timeout 구분|기준', '변경 유형|처리 기준', '조건|예시', '절차|주의사항',
  '단계|점검명', '생성 주체|기준', '개발자 기준|설명', '처리 항목|기준',
  '보안 항목|기준', '로그|동기/비동기 기준', '조회 기준|설명', '테스트 대상|위치',
  '유형|Blocker / Critical 사례', '금지 패턴|문제', '방식|판단', '주의 항목|설명',
  '단계|정상 기준', 'Endpoint|사용 목적', '사용 목적|주의사항',
  'Header businessCode|판단', '금지 Endpoint|문제점', 'REMARK|비고',
  '계층|트랜잭션 기준', '계층|예외 처리 기준', '계층|로그 기준',
  '항목|처리 기준', '거래 유형|트랜잭션 기준',
]);

function isTwoColHeaderPair(h0, h1) {
  if (h0 === '↓' || h1 === '↓' || h0 === '→' || h1 === '→') return false;
  const FLOW_LAYER = new Set([
    'Handler', 'Facade', 'Service', 'Rule', 'DAO', 'Mapper', 'Gateway', 'STF', 'ETF',
    'Controller', 'Filter', 'Client', '화면', 'DB', 'MyBatis XML', 'Online Endpoint',
  ]);
  if (FLOW_LAYER.has(h0)) {
    if (!isStrictHeaderCell(h1) && !/^[A-Z]{2,3}\./.test(h1) && !['역할', '설명', '↓'].includes(h1)) {
      return false;
    }
  }
  if (TWO_COL_PAIRS.has(`${h0}|${h1}`)) return true;
  if (h0 === '잘못된 예' && h1 === '문제') return true;
  if (h0 === '항목' && h1 === '표준') return true;
  if (h0 === '원칙' && h1 === '기준') return true;
  if (h0 === '원칙' && h1 === '한 줄 기준') return true;
  if (h0 === '원칙' && h1 === '설계 기준') return true;
  if (h0 === '추적 기준' && h1 === '반드시 포함할 값') return true;
  if (h0 === '중복 표현' && h1 === '표준화 기준') return true;
  if (h0 === '사용 위치' && h1 === '사용 방식') return true;
  if (h0 === '구분' && h1 === '적용 범위') return true;
  if (h0 === '대상' && h1 === '설명') return true;
  if (h0 === '문제' && h1 === '설명') return true;
  if (h0 === '산출물' && h1 === '설명') return true;
  if (h0 === '용어' && h1 === '설명') return true;
  if (h0 === '단계' && h1 === '활용 방법') return true;
  if (h0 === '제외 항목' && h1 === '별도 기준 문서') return true;
  if (h0 === '원칙' && h1 === '설명') return true;
  if (h0 === '질문' && h1 === '위치') return true;
  if (h0 === 'Header 항목' && h1 === '정합성 기준') return true;
  if (h0 === '거래통제 항목' && h1 === '예시') return true;
  if (h0 === '테스트 항목' && h1 === '검증 기준') return true;
  if (h0 === '점검 항목' && (h1 === '확인' || h1 === '판단')) return true;
  if (h0 === '금지 사항' && h1 === '사유') return true;
  if (h0 === 'Handler 역할' && h1 === '설명') return true;
  if (h0 === '변경 유형' && h1 === '기준') return true;
  if (h0 === '구분' && h1 === '개발자가 알아야 할 내용') return true;
  if (h0 === '금지 영역' && h1 === '이유') return true;
  if (h0 === '모듈' && h1 === '개발자 관점 역할') return true;
  if (h0 === 'OM 관리 항목' && h1 === '개발자와의 관계') return true;
  if (h0 === 'DB' && h1 === '용도') return true;
  if (h0 === '항목' && h1 === '개발 기준') return true;
  if (h0 === '확인 항목' && h1 === '설명') return true;
  if (h0 === '문제' && h1 === '영향') return true;
  if (h0 === '금지 패턴' && h1 === '이유') return true;
  if (h0 === '구분' && h1 === '표준 기준') return true;
  if (h0 === '구분' && h1 === '표준') return true;
  if (h0 === '관리 항목' && h1 === '설명') return true;
  if (h0 === '유형' && h1 === 'SQL ID 예시') return true;
  if (h0 === '오류코드' && h1 === '의미') return true;
  if (h0 === '로그 항목' && h1 === '설명') return true;
  if (h0 === '보안 영역' && h1 === '개발 기준') return true;
  if (h0 === '거래 유형' && h1 === '트랜잭션 기준') return true;
  if (h0 === '효과' && h1 === '설명') return true;
  if (h0 === '나쁜 예' && h1 === '문제') return true;
  if (h0 === '책임 영역' && h1 === '주요 책임') return true;
  if (h0 === '테스트 영역' && h1 === '주요 책임') return true;
  if (h0 === '질문' && h1 === '확인 목적') return true;
  if (h0 === '역할' && h1 === '주요 산출물') return true;
  if (h0 === '역할' && h1 === '완료 책임') return true;
  if (h0 === '기준' && h1 === '설명') return true;
  if (h0 === 'RACI' && h1 === '의미') return true;
  if (h0 === '시사점' && h1 === '설명') return true;
  if (h0 === '목표' && h1 === '설명') return true;
  if (h0 === '로그 항목' && h1 === '확인 목적') return true;
  if (h0 === '테스트 대상' && h1 === '테스트 내용') return true;
  if (h0 === '금지사항' && h1 === '사유') return true;
  if (h0 === '핵심 기준' && h1 === '의미') return true;
  if (h0 === '영역' && h1 === '관리 기준') return true;
  if (h0 === '브랜치' && h1 === '보호 수준') return true;
  if (h0 === '업무코드' && h1 === '브랜치 표기') return true;
  if (h0 === '좋은 Commit' && h1 === '나쁜 Commit') return true;
  if (h0 === 'Scope' && h1 === '의미') return true;
  if (h0 === '변경 영역' && h1 === '승인 기준') return true;
  if (h0 === '방식' && h1 === '설명') return false;
  if (!isStrictHeaderCell(h0) || !isStrictHeaderCell(h1)) return false;
  const allowed = TWO_COL_HEADERS[h0];
  if (allowed?.includes(h1)) return true;
  if (h0 === '구분') {
    return TWO_COL_H1.has(h1) || (allowed?.includes(h1) ?? false);
  }
  if (h0 === '항목' || h0 === '목적') return true;
  return false;
}

const TWO_COL_H1 = new Set([
  '표준', '개발 기준', '설계 기준', '테스트 기준', 'SQL 작성 기준', '권장 기준', '완료 기준',
  '처리 방식', '확인 기준', '통과 기준', '기준', 'bootRun', 'Tomcat WAR 배포', '내용', '구조',
  'Cache 대상', 'Key 예시', 'Module', '생성 산출물', '포함 여부', '생성 위치', '준수사항',
  '표준 예시', '표준 기준', '표준 규칙', '작성 기준', 'SQL 작성 기준', 'Cache 여부',
]);

const COMPARE_3COL = [
  ['단위 테스트', '통합 테스트'],
  ['REST 방식', 'NSIGHT Online Endpoint 방식'],
  ['Session Cookie', 'JWT'],
  ['세션', 'JWT'],
  ['좋은 방식', '나쁜 방식'],
  ['좋은 Commit', '나쁜 Commit'],
  ['좋은 리뷰 의견', '나쁜 리뷰 의견'],
  ['나쁜 SQL', '좋은 SQL'],
  ['직접 호출', 'Gateway 경유'],
  ['공통 Validation', '업무 Validation'],
  ['거래로그', '감사로그'],
  ['Facade', 'Service'],
  ['Service', 'Rule'],
  ['RDW Mapper', 'ADW Mapper'],
  ['사용자 메시지', '운영 로그'],
  ['사용자 메시지', '운영자 메시지'],
  ['사용자 응답', '운영 로그'],
  ['#{}', '${}'],
  ['통합 테스트', 'TCF 거래 테스트'],
  ['일반 목록 조회', '대량 다운로드'],
  ['OM 배포관리', 'CI/CD 파이프라인'],
  ['품질 게이트', '통과 기준'],
  ['Liveness', 'Readiness'],
  ['사용자 안내', '장애 분석'],
  ['나쁜 리뷰 의견', '좋은 리뷰 의견'],
  ['bootRun', 'Tomcat WAR'],
  ['bootRun', 'Tomcat WAR 배포'],
  ['로컬 bootRun', '운영 Tomcat WAR'],
];

function isThreeColCompare(h1, h2) {
  return COMPARE_3COL.some(([a, b]) => (a === h1 && b === h2) || (a === h2 && b === h1));
}

function isThreeColHeaderSet(h1, h2) {
  if (isThreeColCompare(h1, h2)) return true;
  if (TWO_COL_H1.has(h1)) return false;
  if (h2 === '예시' || h2 === '설명') return isStrictHeaderCell(h1) && h1.length <= 22;
  if (h1 === '역할' && h2 === '예시') return true;
  if (h1 === '설계 기준' && h2 === '예시') return true;
  if (h1 === 'Endpoint' && h2 === '설명') return true;
  if (h1 === 'ServiceId' && h2 === '거래코드') return true;
  if (['대표 증상', '점검 영역', '점검 항목', '측정 항목', '담당자', '모듈'].includes(h1)) {
    return isStrictHeaderCell(h2) && h2.length <= 22;
  }
  return false;
}

function isTableRowBreak(line) {
  const t = line.trim();
  if (!t) return true;
  if (isSectionHeading(t) || isProseLine(t)) return true;
  // 표 1열 라벨(「운영 추적성 확보」 등)과 본문을 구분 — 짧은 라벨은 행 단절하지 않음
  if (t.length <= 50 && !/[다요]\.$/.test(t)) return false;
  if (/^(핵심|따라서|권장|예시는|표준은|핵심 기준|핵심은|정규식|전문|Handler 역할|금지 사항|Handler에서|본 개발|본 문서|본 장)/.test(t)) return true;
  if (/^(NSIGHT TCF|NSIGHT Java|TCF Framework|TCF 설계)/.test(t)) return true;
  if (/=/.test(t) && /^(ServiceId|TransactionCode|BusinessCode)\s*=/.test(t)) return true;
  if (/[다]\.$/.test(t) && t.length >= 12) return true;
  return false;
}

function isFixedTableRowBreak(line, headers) {
  const t = line.trim();
  if (!t) return true;
  if (isSectionHeading(t)) return true;
  if (isProseLine(t)) return true;
  if (/작성 기준은 다음|예시는 다음|다음과 같다|테스트 예시는|예시는 가장/.test(t)) return true;
  if (/^로그에는 다음 정보/.test(t)) return true;
  if (/거래로그 구조는 다음|개발자는 최소한|공통 처리를 우회하면/.test(t)) return true;
  if (headers[0] === 'ServiceId' && !/^[A-Z]{2,3}\./.test(t) && !t.includes('.')) return true;
  if (t === headers[0]) return true;
  if (/^본 (개발|문서|장|표준)/.test(t)) return true;
  if (/^(핵심 원칙은|이 선이|즉,|따라서|기본 원칙은|권장 행위명은|SQL ID 예시는|오류코드 형식은|금지 예시는|DTO 작성 원칙은|트랜잭션 적용 기준은|배포 흐름은|거래코드는|거래 상태는|개발 완료 기준은|최종 문장은|정리하면|테스트 완료 기준은|역할별|로컬 설정에서|Windows 환경|Linux,|bootRun과|테스트 실행|특정 모듈|특정 업무|WAR 생성|빌드 산출물|권장 방식|application-local\.yml에서는)/.test(t)) return true;
  // 역할|주요 산출물·완료 책임 표 1열(OM 개발자 등)은 데이터 행 — 대상|설명 표(1.4)도 동일
  if (
    headers[0] !== '역할' &&
    headers[0] !== '대상' &&
    /^(업무 개발자가|공통 프레임워크|OM 개발자|Gateway 개발|JWT \/ SSO|Batch 개발|UI 개발|SQL \/ DB|DevOps 담당|리뷰어가|PMO 관점)/.test(t)
  ) {
    return true;
  }
  if (/^로컬 개발에서 운영 DB|^로그에는 개인정보|^업무코드는 대문자|^Scope는 업무코드|^Commit 단위는 다음|^보호 브랜치 기준은|^예를 들어 |^MR 설명에는|^Tag에는|^운영 배포 시|^Windows 환경|^Linux,|^bootRun과|^Gradle 멀티|^NSIGHT TCF|^NSIGHT Tomcat|^개발자는 develop|^로컬 설정에서|^단순한 업무에서는|^복잡한 업무에서는/.test(t)) return true;
  if (/개발자 체크리스트$|^DevOps 체크리스트$/.test(t)) return true;
  if (t === 'RACI') return true;
  if (['시사점', '효과', '문제', '원칙', '목표', '핵심 기준'].includes(headers[0]) && /[다]\.$/.test(t) && t.length < 45) return false;
  if (/^→/.test(t)) return true;
  if (/^POST\s+\//.test(t) && headers[0] !== '점검 항목') return true;
  if (/^[\d]+\.\s+\S/.test(t) && headers[0] !== 'No' && headers[0] !== '단계') return true;
  if (/다\.$/.test(t) && t.length >= 38 && /^(개발자가|따라서|이 선|핵심|OM은|거래로그|Timeout|개발 시|공통 처리를|운영 추적|NSIGHT)/.test(t)) return true;
  if (/^\d+\.\s+\S/.test(t) && !/^\d+$/.test(t) && headers[0] !== 'No' && headers[0] !== '단계' && headers[0] !== '순서') return true;
  return false;
}

function extractFixedColTable(lines, start, cols, headers) {
  const rows = [];
  let i = start + cols;
  while (i + cols - 1 < lines.length) {
    const row = lines.slice(i, i + cols).map((l) => l.trim());
    const r0 = row[0];
    if (!r0 || isFixedTableRowBreak(r0, headers)) break;
    const skipProseBreak =
      headers[0] === '시사점' ||
      (headers[0] === '역할' && (headers[1] === '주요 산출물' || headers[1] === '완료 책임'));
    if (!skipProseBreak && isProseLine(r0)) break;
    if (row.some((c) => isSectionHeading(c))) break;
    if (r0.length > 120) break;
    rows.push(row);
    i += cols;
  }
  if (rows.length >= 1) {
    return { next: i, table: mdTable(headers, rows) };
  }
  return null;
}

function isStrictHeaderRow(headers) {
  if (headers.some((h) => !h.trim())) return false;
  const ok = headers.filter((h) => isStrictHeaderCell(h)).length;
  return ok === headers.length;
}

function validateRow(row, cols, headers, rowIdx) {
  if (row.some((c) => isSectionHeading(c))) return false;
  if (isProseLine(row[0])) return false;

  const CODE_CELL = /^(public |private |protected |@|import |package |plugins \{|dependencies \{|implements |class |interface |<\?xml|<mapper|<select|<\/mapper>|spring:|server:|mybatis:|logging:|nsight:|void |return |if \(|for \(|while \()/;
  if (row.some((c) => CODE_CELL.test(c.trim()))) return false;
  if (row.some((c) => /^\d+\.\d+\.\d+/.test(c.trim()) && c.includes('예시'))) return false;
  if (row.some((c) => /작성 기준은 다음|예시는 다음|다음과 같다|다\.$/.test(c))) return false;
  if (row.some((c) => c.trim() === '↓' || c.trim() === '→')) return false;
  if (row.some((c) => /다음 순서로|다음과 같다|다\.$/.test(c) && c.length > 25)) return false;
  if (row[0].length > 90) return false;

  const h0 = headers[0];
  const c0 = row[0].trim();

  if (h0 === 'No' || h0 === '번호') {
    return /^\d+$/.test(c0) && row.slice(1).every((c) => c.trim().length > 0);
  }

  if (cols === 2 && headers[0] === '구분') {
    return c0.length > 0 && c0.length <= 45 && row[1].trim().length > 0;
  }

  if (cols >= 6) {
    return /^\d+$/.test(c0) && row.slice(1).filter((c) => c.trim()).length >= cols - 2;
  }

  if (cols === 3 && rowIdx >= 0) {
    if (/^\d+$/.test(c0)) return row[1].length < 120 && row[2].length < 200;
    return row.every((c) => c.length > 0 && c.length < 200);
  }

  if (cols === 2) {
    return c0.length > 0 && c0.length <= 120 && row[1].trim().length > 0 && row[1].length <= 200;
  }

  if (cols === 5 && headers[0] === '업무코드') {
    return /^[A-Z]{2,3}$/.test(c0) && row.slice(1).every((c) => c.trim().length > 0 && c.length < 120);
  }

  if (cols === 4) {
    return row.every((c) => c.trim().length > 0 && c.length < 120);
  }

  return row.every((c) => c.trim().length > 0 && c.length < 150);
}

const LAYER_ROLE_ROW =
  /^(GSLB|L4|Apache|tcf-|Handler|Facade|Service|Rule|DAO\/Mapper|RDW|ADW|SESSIONDB|LOGDB|OMDB)/;

/** 2.5 계층별 역할 — docx 추출 시 4번째 헤더(개발자 관점)가 1행 1열과 합쳐짐 */
function tryLayerRoleTable(lines, start) {
  if (lines[start]?.trim() !== '계층') return null;
  if (lines[start + 1]?.trim() !== '구성 요소') return null;
  if (lines[start + 2]?.trim() !== '역할') return null;

  const headers = ['계층', '구성 요소', '역할', '개발자 관점'];
  const rows = [];
  let i = start + 3;

  if (lines[i]?.trim() === '개발자 관점') {
    i++;
  }

  while (i + 3 < lines.length) {
    const r0 = lines[i]?.trim() ?? '';
    if (!r0 || isFixedTableRowBreak(r0, headers) || isSectionHeading(r0)) break;
    if (rows.length > 0 && !LAYER_ROLE_ROW.test(r0)) break;
    rows.push(lines.slice(i, i + 4).map((l) => l.trim()));
    i += 4;
  }

  if (rows.length >= 1) return { next: i, table: mdTable(headers, rows) };
  return null;
}

/** 명명규칙 docx 등 고정 헤더 다열 표 */
const FIXED_HEADER_TABLES = [
  // 7장 Git 브랜치 — 5·4열 패턴을 2열(브랜치|보호 수준 등)보다 앞에 둠
  ['브랜치', '용도', '생성 기준', 'Merge 대상', '삭제 여부'],
  ['항목', 'main', 'develop', 'release/*'],
  ['Type', '의미', '예시'],
  ['유형', '용도', '예시'],
  ['영역', '관리 기준'],
  ['업무코드', '브랜치 표기'],
  ['좋은 Commit', '나쁜 Commit'],
  ['브랜치', '보호 수준'],
  ['Scope', '의미'],
  ['변경 영역', '승인 기준'],
  // 8-78장 공통 3·4·5열
  ['영역', '역할', '작성 기준'],
  ['영역', '로컬 bootRun', '운영 Tomcat WAR'],
  ['호출', '허용 여부', '설명'],
  ['패키지', '역할', '대표 클래스'],
  ['패키지', 'DTO 유형', '설명'],
  ['패키지', '명명 기준', 'Handler'],
  ['검증 유형', '검증 대상', '검증 위치'],
  ['처리 유형', 'Action', '사용 예시'],
  ['구분', '의미', '업무코드'],
  ['구분', '의미', '예시'],
  ['구분', '기준', '등록 주체'],
  ['구분', '기준', '관리 주체'],
  ['구분', 'bootRun', 'Tomcat WAR'],
  ['구분', 'bootRun', 'Tomcat WAR 배포'],
  ['점검 항목', 'bootRun', 'Tomcat WAR'],
  ['TYPE', '의미', '예시'],
  ['업무코드', 'Gradle Module', '설명'],
  ['업무코드', 'Gradle Module', 'WAR 파일명'],
  ['업무코드', 'Base Package', '설명'],
  ['업무코드', 'Context Path', 'WAR'],
  ['항목', '기준', '역할'],
  ['항목', '기준', '설명'],
  ['항목', '권장 기준', '설명'],
  ['설정', '기준', '설명'],
  ['설정', '의미'],
  ['Timeout 구분', '기준', '설명'],
  ['유형', '표준 동사', '예시'],
  ['나쁜 예', '문제', '좋은 예'],
  ['금지 이름', '문제점', '권장 이름'],
  ['잘못된 이름', '문제점', '권장 이름'],
  ['대상', '테스트 클래스'],
  ['개발 작업', 'bootRun에서 확인', 'Tomcat WAR에서 확인'],
  ['운영 항목', 'bootRun', 'Tomcat WAR'],
  ['단계', 'bootRun', 'Tomcat WAR'],
  ['DB', '사용 목적', '주의사항'],
  ['Health Check', '설명'],
  ['로그', '설명'],
  ['배치 방식', '설명', '장점', '주의사항'],
  ['호출 관계', '허용 여부', '설명'],
  ['구분', '기준', '예시'],
  ['금지 항목', '나쁜 예', '표준'],
  ['환경', '기준 브랜치', '배포 방식'],
  ['실행 방식', '필요한 이유'],
  ['OM 도메인', '설명'],
  ['업무', 'Context Path', 'Endpoint'],
  ['WAR', 'ServiceId', '거래코드', '설명'],
  ['ServiceId', 'Handler', 'Facade Method'],
  ['업무', '도메인', 'Facade명'],
  ['필드', '설명'],
  ['Header 항목', '설명'],
  ['항목', '기준', 'Plugin'],
  ['모듈', '적용 Plugin', '산출물'],
  ['순서', '작업', '설명'],
  ['순서', '처리', '설명'],
  ['순서', '판단', '설명'],
  ['순서', '책임', '설명'],
  ['계층', '검증 대상', '설명'],
  ['계층', '역할', '개발자 관점 설명'],
  ['계층', '책임', '하면 안 되는 일'],
  ['파일', '역할', '적용 기준'],
  ['구분', '설계 기준', '배포 단위', '업무별 WAR', 'Context 기준'],
  ['업무코드', 'Gradle Module', 'WAR 파일명', 'SV', 'sv-service'],
  ['WAR', 'ServiceId', '거래코드'],
  ['공통 모듈', '업무 WAR에서의 역할', 'tcf-util'],
  ['변경 모듈', '영향 범위', '권장 빌드'],
  ['Context Path', '온라인 Endpoint', 'SV'],
  ['구분', '기준', '테스트 목적'],
  ['영역', '항목', '검색조건'],
  ['항목', '기준', '적용 대상'],
  ['구분', '의미', '사용 기준'],
  ['관점', '시사점', '개발 관점'],
  ['Tag', '의미'],
  ['파일', '역할'],
  ['패키지', '역할'],
  ['설정 파일', '역할'],
  ['WAR 파일', 'Context'],
  ['금지 의존성', '사유'],
  ['역할', '책임'],
  ['리뷰 항목', '기준'],
  ['대상', '리뷰 범위'],
  ['확인 기준', '확인'],
  ['Cache Name', '용도'],
  ['금지 기준', '사유'],
  ['적용 게이트', '설명'],
  ['환경', '기준'],
  ['Token', '용도'],
  ['Claim', '설명'],
  ['Level', '사용 기준'],
  ['Method', 'URL'],
  ['파라미터', '설명'],
  ['태그', '사용 기준'],
  ['시사점', '개발 관점'],
  ['증상', '예시'],
  ['주요 원인', '확인'],
  ['원인', '설명'],
  ['장점', '설명'],
  ['Rollback 기준', '설명'],
  ['사용 기준', '설명'],
  ['해야 하는 일', '설명'],
  ['금지 Method', '문제점'],
  ['호출 관계', '허용 여부'],
  ['분리 기준', '설명'],
  ['명명 예시', '설명'],
  ['권장 위치', '설명'],
  ['검증 단계', '위치'],
  ['배포 방식', '기준'],
  ['빌드 범위', '이유'],
  ['파일 유형', '예시'],
  ['검증 항목', '기준'],
  ['메서드', '설명'],
  ['업무', '도메인'],
  ['Handler', '역할'],
  ['처리', '위치'],
  ['위치', '작성 기준'],
  ['로그', '역할'],
  ['테스트 항목', '기준'],
  ['구분', '기준'],
  ['구분', '의미'],
  ['항목', '기준'],
  ['항목', '설명'],
  ['항목', '값'],
  ['디렉터리', '역할'],
  ['DOMAIN', '의미'],
  ['CATEGORY', '의미'],
  ['공통 패키지', '역할'],
  ['금지 Import', '사유'],
  ['금지 항목', '사유'],
  ['확인 항목', '기준'],
  ['가능 원인', '확인'],
  ['가능 원인', '설명'],
  ['패턴', '기준'],
  ['조합', '기준'],
  ['이유', '설명'],
  ['Gate', '기준'],
  ['판정', '기준'],
  ['성능 항목', '기준'],
  ['데이터 유형', '기준'],
  ['목표', '합격 기준'],
  ['DB', '규칙'],
  ['SQL ID', '예시'],
  ['Annotation', '용도'],
  ['사용 예', '개발 기준'],
  ['권한코드', '설명'],
  ['권한 유형', '검증 위치'],
  ['오류코드 형식', '예시'],
  ['테스트 구분', '테스트 내용'],
  ['Timeout 구분', '기준'],
  ['변경 유형', '처리 기준'],
  ['조건', '예시'],
  ['절차', '주의사항'],
  ['단계', '점검명'],
  ['생성 주체', '기준'],
  ['개발자 기준', '설명'],
  ['처리 항목', '기준'],
  ['보안 항목', '기준'],
  ['로그', '동기/비동기 기준'],
  ['조회 기준', '설명'],
  ['테스트 대상', '위치'],
  ['유형', 'Blocker / Critical 사례'],
  ['금지 패턴', '문제'],
  ['방식', '판단'],
  ['주의 항목', '설명'],
  ['단계', '정상 기준'],
  ['Endpoint', '사용 목적'],
  ['사용 목적', '주의사항'],
  ['Header businessCode', '판단'],
  ['금지 Endpoint', '문제점', '권장 Endpoint'],
  ['외부 호출', 'Gateway 내부 라우팅', '대상 업무 WAR'],
  ['Tag', '의미'],
  // 6장 로컬 개발환경 — 3·4열 패턴을 2열(구분|표준 기준 등)보다 앞에 둠
  ['순서', '설치 항목', '확인 명령', '정상 예시'],
  ['업무', 'Context', '로컬 Port 예시', '로컬 URL'],
  ['구분', '표준 기준', '설명'],
  ['구분', 'Git Commit 가능 여부', '설명'],
  ['항목', '로컬 기준', '주의사항'],
  ['방식', '설명', '사용 시점'],
  ['목표', '설명'],
  ['로그 항목', '확인 목적'],
  ['테스트 대상', '테스트 내용'],
  ['금지사항', '사유'],
  ['핵심 기준', '의미'],
  ['점검 항목', '확인'],
  ['활동', '업무 개발자', '공통 개발자', 'OM 개발자', 'Gateway/JWT/Batch', 'SQL/DB', '테스트', 'DevOps', '아키텍트', 'PMO'],
  ['책임 영역', '주요 책임'],
  ['테스트 영역', '주요 책임'],
  ['질문', '확인 목적'],
  ['역할', '주요 산출물'],
  ['역할', '완료 책임'],
  ['기준', '설명'],
  ['RACI', '의미'],
  ['시사점', '설명'],
  ['구분', '표준 규칙', '예시'],
  ['구분', '작성 기준', '예시'],
  ['처리유형', '권장 행위명', '예시'],
  ['처리유형', '의미', '예시'],
  ['Package', '역할', '예시'],
  ['DTO 유형', '용도', '예시'],
  ['Cache 대상', '캐시 여부', '설명'],
  ['테스트 유형', '필수 여부', '설명'],
  ['테스트 유형', '대상', '기준'],
  ['계층', '책임', '금지 사항'],
  ['계층', '명명 규칙', '예시'],
  ['구분', '표준 기준'],
  ['구분', '표준'],
  ['관리 항목', '설명'],
  ['유형', 'SQL ID 예시'],
  ['오류코드', '의미'],
  ['로그 항목', '설명'],
  ['보안 영역', '개발 기준'],
  ['거래 유형', '트랜잭션 기준'],
  ['효과', '설명'],
  ['나쁜 예', '문제'],
  ['공통 처리', '담당 영역', '업무 개발자 기준'],
  ['추적 항목', '설명', '개발자 책임'],
  ['Timeout 구분', '설명', '개발 기준'],
  ['확인 항목', '설명'],
  ['문제', '영향'],
  ['금지 패턴', '이유'],
  ['식별자', '예시', '의미', '사용 위치'],
  ['구현 대상', '설명', '작성 기준'],
  ['구분', '개발자가 알아야 할 내용'],
  ['DB', '용도', '개발자 주의사항'],
  ['모듈', '개발자 관점 역할'],
  ['OM 관리 항목', '개발자와의 관계'],
  ['금지 영역', '이유'],
  ['목적', '설명'],
  ['문제', '설명'],
  ['대상', '설명'],
  ['구분', '적용 범위'],
  ['제외 항목', '별도 기준 문서'],
  ['단계', '활용 방법'],
  ['산출물', '설명'],
  ['원칙', '설명'],
  ['용어', '설명'],
  ['구성요소', '표기 기준', '설명', '예시'],
  ['Prefix', '영역', '설명', '예시'],
  ['원칙', '설계 기준', '예시'],
  ['원칙', '기준', '예시'],
  ['업무', 'ServiceId', '의미'],
  ['No', '원칙', '설명'],
  ['No', '개발 원칙', '핵심 의미'],
  ['No', '표준 영역', '핵심 기준'],
  ['No', '원칙', '기준'],
  ['구분', '역할', '예시'],
  ['구분', '형식', '예시'],
  ['구분', '명명 기준', '예시'],
  ['사용 위치', '사용 방식', '예시'],
  ['목적', '표준 테이블명', '설명'],
  ['구분', '모듈 영역', '모듈명', '역할'],
  ['질문', '위치'],
  ['업무코드', 'ServiceId Prefix', '거래코드 Prefix', '오류코드 Prefix', '예시 ServiceId'],
  ['잘못된 예', '문제'],
  ['항목', '표준'],
  ['구분', '예시'],
  ['원칙', '설계 기준'],
  ['원칙', '기준'],
  ['원칙', '한 줄 기준'],
  ['추적 기준', '반드시 포함할 값'],
  ['중복 표현', '표준화 기준'],
  ['Cache Name', '의미', '주요 Key 예시', 'TTL 권장', 'Evict 시점'],
  ['No', '구분', '업무코드', '영문명', '업무 설명', 'Context', 'WAR', 'Gradle Module', 'Package'],
  ['No', '업무코드', '권장 업무명', 'Context', 'WAR', 'Gradle Module', 'Java Prefix', 'Package'],
  ['No', '업무코드', '업무명', 'ServiceId Prefix', '예시'],
  ['분류', '항목', '설명'],
  ['항목', '필수', '설명', '예시'],
  ['Header 항목', '설명', '예시'],
  ['화면', '기능', 'ServiceId'],
  ['검증 항목', '기준', '오류 예시'],
  ['No', '논리명', '개발 표준 필드명', '설명'],
  ['의미', '표준 Field', '비고'],
  ['전문 Header', '거래로그 컬럼', '설명'],
  ['FunctionCode', '의미', '대표 ServiceId 예시'],
  ['MDC Key', '의미', '예시'],
  ['Header Field', 'MDC Key', '설명'],
  ['테스트 항목', '검증 기준'],
  ['오류 상황', '오류코드', '설명'],
  ['처리유형', '값', '거래코드 예시', '설명'],
  ['채널', 'channelId 예시', '설명'],
  ['금지 사항', '사유', '권장 위치'],
  ['금지 사항', '사유'],
  ['오류코드', '오류명', '발생 위치', '사용자 메시지', '운영자 메시지'],
  ['컬럼', '설명', '예시'],
  ['Field', '사용 기준', '예시'],
  ['Method', '사용 기준', '예시'],
  ['유형', '표준 Method', '예시'],
  ['계층', '테스트 대상', '검증 기준'],
  ['단계', '산출물'],
  ['매핑 방식', '설명', '예시', '권장'],
  ['Header 항목', '정합성 기준'],
  ['거래통제 항목', '예시'],
  ['오류 상황', '오류코드 예시', '설명'],
];

function headersMatch(actual, expected) {
  if (actual.length !== expected.length) return false;
  return actual.every((a, i) => a === expected[i]);
}

/** 7.22 Git 시사점 — docx 추출 시 3번째 헤더가 첫 행 역할명(개발 통제) */
function tryRoleMeaningShiftedTable(lines, start) {
  if (lines[start]?.trim() !== '역할') return null;
  if (lines[start + 1]?.trim() !== '의미') return null;
  const firstRole = lines[start + 2]?.trim() ?? '';
  if (firstRole !== '개발 통제') return null;

  const headers = ['역할', '의미'];
  const rows = [];
  let i = start + 3;
  const firstMeaning = lines[i]?.trim() ?? '';
  if (!firstMeaning) return null;
  rows.push([firstRole, firstMeaning]);
  i++;

  while (i + 1 < lines.length) {
    const r0 = lines[i]?.trim() ?? '';
    const r1 = lines[i + 1]?.trim() ?? '';
    if (!r0 || !r1) break;
    if (isProseLine(r0) || isSectionHeading(r0)) break;
    if (/^(결론|최종|NSIGHT|모든 개발|Git은)/.test(r0)) break;
    rows.push([r0, r1]);
    i += 2;
  }

  if (rows.length >= 2) return { next: i, table: mdTable(headers, rows) };
  return null;
}

function tryFixedHeaderTable(lines, start) {
  for (const headers of FIXED_HEADER_TABLES) {
    const cols = headers.length;
    if (start + cols > lines.length) continue;
    const actual = lines.slice(start, start + cols).map((l) => l.trim());
    if (!headersMatch(actual, headers)) continue;
    const result = extractFixedColTable(lines, start, cols, headers);
    if (result && result.next > start + cols) return result;
  }
  return null;
}

/** No 로 시작하는 가변 열 표 (헤더 줄 수 자동 감지) */
function tryNoVariableTable(lines, start) {
  if (lines[start]?.trim() !== 'No') return null;
  const headers = [lines[start].trim()];
  let i = start + 1;
  while (i < lines.length) {
    const t = lines[i]?.trim() ?? '';
    if (/^\d+$/.test(t)) break;
    if (!t || isProseLine(t) || isSectionHeading(t)) return null;
    if (t.length > 45 && !EXACT_HEADER_LABELS.has(t) && !HEADER_WORDS.has(t)) return null;
    headers.push(t);
    i++;
    if (headers.length > 12) return null;
  }
  const cols = headers.length;
  if (cols < 3 || i >= lines.length) return null;
  if (!headers.slice(1).every((h) => isStrictHeaderCell(h) || EXACT_HEADER_LABELS.has(h))) return null;

  const rows = [];
  while (i + cols <= lines.length) {
    const row = lines.slice(i, i + cols).map((l) => l.trim());
    if (!/^\d+$/.test(row[0])) break;
    if (!validateRow(row, cols, headers, rows.length)) break;
    rows.push(row);
    i += cols;
  }
  if (rows.length >= 1) return { next: i, table: mdTable(headers, rows) };
  return null;
}

function collectBracketBlock(lines, start) {
  const t = lines[start]?.trim() ?? '';
  if (!/^\[[^\]]+\]$/.test(t)) return null;
  const buf = [t];
  let i = start + 1;
  while (i < lines.length) {
    const raw = lines[i];
    const s = raw.trim();
    if (!s) {
      if (buf.length > 1) break;
      i++;
      continue;
    }
    if (/^\[\S/.test(s)) break;
    if (isSectionHeading(s)) break;
    if (isProseLine(s) && s.length > 80) break;
    buf.push(raw.trimEnd());
    i++;
    if (buf.length > 24) break;
  }
  if (buf.length >= 2) {
    return { next: i, content: buf.join('\n') };
  }
  return null;
}

/** "로그에는 다음 정보…" 뒤 필드명 나열 (GUID, TraceId, …) */
function tryLogFieldList(lines, start) {
  const t = lines[start]?.trim() ?? '';
  if (!/^로그에는 다음 정보/.test(t)) return null;
  const fields = [];
  let i = start + 1;
  while (i < lines.length) {
    const f = lines[i]?.trim() ?? '';
    if (!f) break;
    if (isSectionHeading(f) || isProseLine(f)) break;
    if (!/^[A-Za-z][\w]*$/.test(f)) break;
    fields.push(f);
    i++;
  }
  if (fields.length < 2) return null;
  return {
    next: i,
    block: [t, '', fields.join(', '), ''].join('\n'),
  };
}

function tryExtractTable(lines, start) {
  if (isProseLine(lines[start] ?? '')) return null;

  const layerRole = tryLayerRoleTable(lines, start);
  if (layerRole) return layerRole;

  const roleMeaning = tryRoleMeaningShiftedTable(lines, start);
  if (roleMeaning) return roleMeaning;

  const fixed = tryFixedHeaderTable(lines, start);
  if (fixed) return fixed;

  const noVar = tryNoVariableTable(lines, start);
  if (noVar) return noVar;

  // "No" 단독 줄 + 다열 헤더 (docx 표 추출 특성) — 레거시 6·7열
  if (lines[start]?.trim() === 'No') {
    for (const cols of [7, 6]) {
      if (start + cols > lines.length) continue;
      const headers = [lines[start].trim(), ...lines.slice(start + 1, start + cols).map((l) => l.trim())];
      if (!isStrictHeaderRow(headers.slice(1))) continue;
      const rows = [];
      let i = start + cols;
      while (i + cols <= lines.length) {
        const row = lines.slice(i, i + cols).map((l) => l.trim());
        if (!validateRow(row, cols, headers, rows.length)) break;
        rows.push(row);
        i += cols;
      }
      if (rows.length >= 1) {
        return { next: i, table: mdTable(headers, rows) };
      }
    }
  }

  // 3-column fixed headers (항목·업무코드 등)
  if (start + 3 <= lines.length) {
    const h0 = lines[start].trim();
    const h1 = lines[start + 1].trim();
    const h2 = lines[start + 2].trim();
    const fixed3 = [
      ['항목', '예시', '설명'],
      ['업무코드', 'Java Prefix', '예시 Class'],
      ['단계', '역할', '개발자 관점'],
    ];
    for (const [a, b, c] of fixed3) {
      if (h0 === a && h1 === b && h2 === c) {
        const result = extractFixedColTable(lines, start, 3, [a, b, c]);
        if (result && result.next > start + 3) return result;
      }
    }
    if (h0 === '변경 유형' && h1 === '기준') {
      const result = extractFixedColTable(lines, start, 2, [h0, h1]);
      if (result && result.next > start + 2) return result;
    }
    if (h0 === 'Handler 역할' && h1 === '설명') {
      const result = extractFixedColTable(lines, start, 2, [h0, h1]);
      if (result && result.next > start + 2) return result;
    }
    if (h0 === '금지 사항' && h1 === '이유') {
      const result = extractFixedColTable(lines, start, 2, [h0, h1]);
      if (result && result.next > start + 2) return result;
    }
    if (h0 === '구현 대상' && h1 === '설명' && h2 === '작성 기준') {
      const result = extractFixedColTable(lines, start, 3, [h0, h1, h2]);
      if (result && result.next > start + 3) return result;
    }
  }

  if (start + 2 <= lines.length) {
    const h0 = lines[start].trim();
    const h1 = lines[start + 1].trim();
    if (isStrictHeaderCell(h0) && isStrictHeaderCell(h1) && isTwoColHeaderPair(h0, h1)) {
      const headers = [h0, h1];
      const rows = [];
      let i = start + 2;
      while (i + 1 < lines.length) {
        const r0 = lines[i].trim();
        const r1 = lines[i + 1].trim();
        if (!r0 || !r1 || isFixedTableRowBreak(r0, headers)) break;
        if (r0.length > 120 || r1.length > 200 || r0 === h0 || r0 === h1 || /^\d+\.\d+/.test(r0)) break;
        rows.push([r0, r1]);
        i += 2;
      }
      if (rows.length >= 1) {
        return { next: i, table: mdTable(headers, rows) };
      }
    }
  }

  // 3-column (구분 | A | B) — 비교·표기·Endpoint 등 명시 패턴만
  if (start + 3 <= lines.length && lines[start].trim() === '구분') {
    const h1 = lines[start + 1].trim();
    const h2 = lines[start + 2].trim();
    if (isThreeColHeaderSet(h1, h2)) {
      const result = extractFixedColTable(lines, start, 3, ['구분', h1, h2]);
      if (result && result.next > start + 3) return result;
    }
  }

  // 4-column (구성요소 | 자리수 | 설명 | 예시)
  if (start + 4 <= lines.length) {
    const h0 = lines[start].trim();
    const h1 = lines[start + 1].trim();
    const h2 = lines[start + 2].trim();
    const h3 = lines[start + 3].trim();
    if (h0 === '구성요소' && h1 === '자리수' && h2 === '설명' && h3 === '예시') {
      const result = extractFixedColTable(lines, start, 4, [h0, h1, h2, h3]);
      if (result) return result;
    }
    if (h0 === '구성요소' && h1 === '표기 기준' && h2 === '설명' && h3 === '예시') {
      const result = extractFixedColTable(lines, start, 4, [h0, h1, h2, h3]);
      if (result) return result;
    }
  }

  // 4-column (구분 | 점검 영역 | 완료 기준 | …)
  if (start + 4 <= lines.length && lines[start].trim() === '구분') {
    const h1 = lines[start + 1].trim();
    const h2 = lines[start + 2].trim();
    const h3 = lines[start + 3].trim();
    const ok4 =
      (h1 === '점검 영역' && h2 === '완료 기준') ||
      (h1 === '점검 항목' && h2 === '확인 기준' && h3 === '확인') ||
      (h1 === '점검 항목' && h2 === '확인' && h3 === '판단') ||
      (h1 === '담당자' && h2 === '승인 기준' && h3 === '서명/확인');
    if (ok4) {
      const result = extractFixedColTable(lines, start, 4, ['구분', h1, h2, h3]);
      if (result) return result;
    }
  }

  // 3-column without leading 구분 (구성요소 | 자리수 | 설명)
  if (start + 3 <= lines.length) {
    const h0 = lines[start].trim();
    const h1 = lines[start + 1].trim();
    const h2 = lines[start + 2].trim();
    const LAYER_CRITERIA_H1 = new Set(['트랜잭션 기준', '예외 처리 기준', '로그 기준']);
    if (h0 === '계층' && LAYER_CRITERIA_H1.has(h1)) {
      // 2열 계층|…기준 표 — 3번째 줄(Handler 등)은 데이터 행
    } else if (h0 === '거래 유형' && h1 === '트랜잭션 기준') {
      // 2열 거래 유형|트랜잭션 기준 표
    } else if (
      ['구성요소', '필드', 'Layer', '계층', '목적', '영역', '패키지', '호출', '검증', '파일', '모듈', 'WAR', '순서', '변환', '공통 모듈', 'Context Path', '거래 유형', '관점', '구분', '점검 항목', '처리 유형', '업무코드', 'TYPE', 'Type'].includes(h0) &&
      isStrictHeaderCell(h1) &&
      isStrictHeaderCell(h2)
    ) {
      const result = extractFixedColTable(lines, start, 3, [h0, h1, h2]);
      if (result && result.next > start + 3) return result;
    }
  }

  for (const cols of [7, 6, 5, 4, 3]) {
    if (start + cols > lines.length) continue;

    const headers = lines.slice(start, start + cols).map((l) => l.trim());
    if (!isStrictHeaderRow(headers)) continue;
    if (cols >= 6 && headers[0] !== 'No') continue;
    if (cols === 3 && headers[0] === '구분') continue;
    if (cols === 4 && headers[0] === '구분') continue;
    if (cols === 3 && headers[0] === '계층' && headers[1] === '구성 요소') continue;
    if (cols === 3 && !['No', '항목', 'ServiceId', 'Layer', '계층', '목적', '분류', 'Header 항목', '화면', '검증 항목', '의미', '전문 Header', 'FunctionCode', 'MDC Key', '오류 상황', '컬럼', 'Field', 'Method', '유형', '계층', '매핑 방식', '채널', '구현 대상', 'Package', 'DTO 유형', '처리유형', 'Cache 대상', '테스트 유형', '구분', '영역', '패키지', '호출', '검증', '파일', '모듈', 'WAR', '순서', '변환', '공통 모듈', 'Context Path', '거래 유형', '관점', '점검 항목', '처리 유형', '업무코드', 'TYPE', 'Type', 'Tag', 'Claim', 'Token', 'Level', 'Step', 'Category', 'Audit', 'Profile', 'Secret', 'Evict', '설정', '금지', '적용', '배포', '빌드', '환경', 'Timeout', 'Rollback', '장점', '사용', '명명', '권장', '분리', '호출', '테스트', '로그', '리뷰', '대상', '확인', '원인', '증상', '주요', '시사점', '브랜치', '업무', 'Handler', '메서드', '거래', '변경', '디렉터리', 'Clean', 'JAR', '핵심', '목표', '원칙', '기준', '효과', '설명', '의미', '예시', '역할', '책임', '용도', 'Plugin', '산출물', '대상', '판단', '확인', '검증', '내용', '사유', '형식', '표준', '규칙', '방식', '유형', '구분', '항목', '영역', '필드', '모듈', '패키지', '파일'].includes(headers[0])) continue;
    if (cols === 4 && !['No', '항목', 'ServiceId', '업무코드', '처리유형', '오류코드', '식별자', '구분', '영역', '변경', '점검', '업무', '모듈', '순서', '디렉터리', 'Clean', 'JAR', '설정', 'Profile', 'Secret', 'Cache', 'Header', 'Field', 'Method', 'Step', 'Category', 'Audit', 'Evict', '금지', '적용', '배포', '빌드', '환경', 'Timeout', 'Rollback', '장점', '사용', '명명', '권장', '분리', '호출', '테스트', '로그', '리뷰', '대상', '확인', '원인', '증상', '주요', '시사점', '브랜치', '업무', 'Handler', '메서드', '거래', '변경', '핵심', '목표', '원칙', '기준', '효과', '설명', '의미', '예시', '역할', '책임', '용도', 'Plugin', '산출물', '대상', '판단', '확인', '검증', '내용', '사유', '형식', '표준', '규칙', '방식', '유형', '구분', '항목', '영역', '필드', '모듈', '패키지', '파일', 'main'].includes(headers[0])) continue;
    if (cols === 5 && headers[0] !== 'No' && headers[0] !== '업무구분' && headers[0] !== '브랜치' && headers[0] !== '순서' && headers[0] !== '모듈' && headers[0] !== '디렉터리' && headers[0] !== '구분' && headers[0] !== '업무코드' && headers[0] !== 'JAR' && headers[0] !== '기준' && headers[0] !== '설명' && headers[0] !== 'Clean' && headers[0] !== '변경' && headers[0] !== '오류' && headers[0] !== '중앙') continue;

    const rows = [];
    let i = start + cols;

    while (i + cols <= lines.length) {
      const row = lines.slice(i, i + cols).map((l) => l.trim());
      if (!validateRow(row, cols, headers, rows.length)) break;
      rows.push(row);
      i += cols;
    }

    const minRows = 1;
    if (rows.length >= minRows) {
      return { next: i, table: mdTable(headers, rows) };
    }
  }
  return null;
}

function isDiagramLine(line, inDiagram) {
  const t = line.trim();
  if (!t) return false;
  if (/^[├└│─]/.test(t)) return true;
  if (/^\s*↓\s*$/.test(t) || /^↓/.test(t)) return true;
  if (inDiagram && /^[A-Za-z][\w.]*\s*$/.test(t) && t.length < 40) return true;
  return false;
}

function collectDiagramBlock(lines, start) {
  let i = start;
  const buf = [];
  // tree: SV + ├─ lines
  if (/^[A-Z]{2,3}$/.test(lines[start]?.trim()) && /^[├└]/.test(lines[start + 1]?.trim() ?? '')) {
    buf.push(lines[start].trim());
    i = start + 1;
  }
  let inDiagram = buf.length > 0;
  let blanks = 0;
  while (i < lines.length) {
    const raw = lines[i];
    const t = raw.trim();
    if (!t) {
      blanks++;
      if (inDiagram && blanks >= 2) break;
      i++;
      continue;
    }
    blanks = 0;
    if (isFlowLine(raw) || isDiagramLine(raw, inDiagram)) {
      if (inDiagram && (t === '구분' || (HEADER_WORDS.has(t) && !/[↓├└]/.test(t)))) break;
      buf.push(raw.trimEnd());
      inDiagram = true;
      i++;
      continue;
    }
    break;
  }
  if (buf.length >= 2 || buf.some((l) => /[├└↓]/.test(l))) {
    return { next: i, content: buf.join('\n') };
  }
  return null;
}

function isFlowLine(line) {
  const t = line.trim();
  if (!t) return false;
  if (/^POST\s+\/[a-z]/i.test(t)) return true;
  if (/^GET\s+\//i.test(t)) return true;
  if (/^curl\s/i.test(t)) return true;
  if (/^\.?\/gradlew\b|^gradlew\.bat\b|^gradle\s+:/i.test(t)) return true;
  if (/^[↓→←]\s*$/.test(t)) return true;
  if (/^[├└│]/.test(t)) return true;
  if (/^header\.serviceId\s*=/i.test(t)) return true;
  if (/^\s+↓/.test(line)) return true;
  return false;
}

function isJsonStart(lines, start) {
  const t = lines[start]?.trim() ?? '';
  if (t !== '{') return false;
  const sample = lines.slice(start, start + 8).join(' ');
  return /"header"|"body"|"result"/.test(sample);
}

function collectJsonBlock(lines, start) {
  const buf = [];
  let depth = 0;
  let i = start;
  for (; i < lines.length; i++) {
    const t = lines[i].trimEnd();
    buf.push(t);
    depth += (t.match(/{/g) || []).length;
    depth -= (t.match(/}/g) || []).length;
    if (depth <= 0 && t.includes('}')) {
      i++;
      break;
    }
    if (i - start > 60) break;
  }
  return { next: i, content: buf.join('\n') };
}

function isCodeBlockStart(lines, start) {
  const t = lines[start]?.trim() ?? '';
  if (!t) return false;
  if (/^(public |private |protected |@Component|@Service|@Repository|@Mapper|@RequiredArgsConstructor|@Slf4j|@Transactional|@SpringBootApplication|@Test|@Override|if\s*\(|try\s*\{|plugins \{|dependencies \{|import |package |<\?xml|<mapper|<select|spring:|server:|mybatis:|logging:|nsight:|void |@Bean|@Value|@Autowired)/.test(t)) {
    return true;
  }
  if (t === 'bootWar {' || t === 'bootRun {') return true;
  return false;
}

function detectCodeLang(buf) {
  const s = buf.join('\n');
  if (/^<\?xml|^<mapper|^<select|^<\/mapper>/.test(s.trim())) return 'xml';
  if (/^plugins \{|^dependencies \{|^bootWar \{|^bootRun \{/.test(s.trim())) return 'gradle';
  if (/^(spring:|server:|mybatis:|logging:|nsight:)/m.test(s)) return 'yaml';
  return 'java';
}

function collectCodeBlock(lines, start) {
  const first = lines[start]?.trim() ?? '';
  const buf = [];
  let i = start;

  if (/^(spring:|server:|mybatis:|logging:|nsight:)/.test(first)) {
    while (i < lines.length) {
      const raw = lines[i];
      const t = raw.trim();
      if (!t) {
        if (buf.length > 0) {
          const n = lines[i + 1]?.trim() ?? '';
          if (!n || isSectionHeading(n) || isProseLine(n)) break;
          if (!/^[a-z][\w.-]*:/.test(n) && !/^  /.test(lines[i + 1] ?? '')) break;
        }
        i++;
        continue;
      }
      if (isSectionHeading(t) || (isProseLine(t) && buf.length > 0)) break;
      if (buf.length === 0 && isStrictHeaderCell(t)) {
        const n = lines[i + 1]?.trim() ?? '';
        if (isStrictHeaderCell(n) || isTwoColHeaderPair(t, n)) break;
      }
      buf.push(raw.trimEnd());
      i++;
      if (i - start > 100) break;
    }
    if (buf.length >= 2) return { next: i, content: buf.join('\n'), lang: detectCodeLang(buf) };
    return null;
  }

  let depth = 0;
  while (i < lines.length) {
    const raw = lines[i];
    const t = raw.trim();
    if (!t) {
      if (buf.length > 0 && depth <= 0) break;
      i++;
      continue;
    }
    if (buf.length > 0 && depth <= 0) {
      if (isSectionHeading(t)) break;
      if (isProseLine(t) && !/[;{}]$/.test(t)) break;
      const n = lines[i + 1]?.trim() ?? '';
      if (isStrictHeaderCell(t) && (isStrictHeaderCell(n) || isTwoColHeaderPair(t, n))) break;
    }
    buf.push(raw.trimEnd());
    depth += (raw.match(/{/g) || []).length - (raw.match(/}/g) || []).length;
    i++;
    if (depth <= 0 && buf.length > 1 && buf.some((l) => l.trim() === '}')) {
      i++;
      break;
    }
    if (i - start > 150) break;
  }
  if (buf.length >= 2 && (buf.some((l) => /[{;}]/.test(l)) || buf[0].startsWith('@'))) {
    return { next: i, content: buf.join('\n'), lang: detectCodeLang(buf) };
  }
  return null;
}

function collectArchitectureFlowBlock(lines, start) {
  const s0 = lines[start]?.trim() ?? '';
  if (!isArchFlowLabel(s0)) return null;

  const s1 = lines[start + 1]?.trim() ?? '';
  if (isStrictHeaderCell(s0) && isStrictHeaderCell(s1) && isTwoColHeaderPair(s0, s1)) return null;
  if (isStrictHeaderCell(s0) && isStrictHeaderCell(s1) && isStrictHeaderCell(lines[start + 2]?.trim() ?? '')) {
    return null;
  }

  const buf = [];
  let i = start;
  let sawArrow = false;

  while (i < lines.length) {
    const raw = lines[i] ?? '';
    const s = raw.trim();

    if (!s) {
      i++;
      if (buf.length >= 4 && sawArrow) break;
      continue;
    }

    if (buf.length > 0 && (/^핵심 문장은|^핵심은/.test(s) || isSectionHeading(s))) break;
    if (buf.length > 0 && s === '구분' && ['기준', '의미', '표준'].includes(lines[i + 1]?.trim() ?? '')) break;
    if (buf.length > 0 && ['점검 항목', '확인 항목', '금지 사항', '테스트 항목'].includes(s)) {
      const n = lines[i + 1]?.trim() ?? '';
      if (isStrictHeaderCell(n) || ['확인', '기준', '설명', '사유'].includes(n)) break;
    }

    if (isArchFlowDetailLine(raw)) {
      if (/↓/.test(raw)) sawArrow = true;
      buf.push(raw.trimEnd());
      i++;
      continue;
    }

    if (isArchFlowLabel(s)) {
      buf.push(s);
      i++;
      continue;
    }

    if (buf.length > 0 && sawArrow && /^\s{2,}/.test(raw)) {
      buf.push(raw.trimEnd());
      i++;
      continue;
    }

    break;
  }

  if (buf.length >= 4 && sawArrow) {
    return { next: i, content: buf.join('\n') };
  }
  return null;
}

const ARCH_FLOW_LABEL =
  /^(화면|Handler|Facade|Service|Rule|DAO|MyBatis XML|DB|Gateway|STF|ETF|Mapper|Client|Controller|Filter|요청 수신|업무 처리 중|BusinessException|SystemException|GlobalExceptionHandler|StandardResponse|거래로그|MDC|GUID|SQL|Integration|Batch Job|Scheduler|OM|DevOps|Online Endpoint|TransactionContext|HTTP Client|External|TCF \/ ETF|화면에|Integration Service|MyBatis|Batch|Job|Screen|Message|Payload|Schema|Audit|Metric|Health|Swagger|Aspect|Listener|Event|Worker|Pool|Queue|Store|Entity|Model|Command|Query|Criteria|Result|Entry|Trace|Resource|DevOps|Pool|Queue)/;

/** label+↓ 흐름도 라벨 (한글 연속 포함) */
const FLOW_LABEL_RE = /^[A-Za-z가-힣][A-Za-z가-힣\w/ .·()-]+$/;

function isArchFlowLabel(s) {
  if (!s || s.length > 50) return false;
  if (/[다요]\.$/.test(s)) return false;
  if (isSectionHeading(s) || isProseLine(s)) return false;
  if (ARCH_FLOW_LABEL.test(s)) return true;
  if (FLOW_LABEL_RE.test(s) && !isStrictHeaderCell(s)) return true;
  return false;
}

function isArchFlowDetailLine(raw) {
  const s = raw.trim();
  if (!s) return false;
  if (/^\s{2,}/.test(raw)) return true;
  if (/^\s*↓/.test(raw) || /^↓/.test(s)) return true;
  if (s === 'OFFSET/FETCH') return true;
  return false;
}

function collectNumberedFlowBlock(lines, start) {
  const s0 = lines[start]?.trim() ?? '';
  if (!/^\d+\.\s+\S/.test(s0) || /^\d+\.\d+/.test(s0)) return null;
  const buf = [];
  let i = start;
  while (i < lines.length) {
    const raw = lines[i] ?? '';
    const s = raw.trim();
    if (!s) {
      i++;
      continue;
    }
    if (/^\d+\.\s+\S/.test(s) && !/^\d+\.\d+/.test(s)) {
      buf.push(s);
      i++;
      continue;
    }
    if (/^\s*↓/.test(raw) || s === '↓') {
      buf.push(raw.trimEnd());
      i++;
      continue;
    }
    break;
  }
  if (buf.length >= 4 && buf.some((l) => /↓/.test(l))) {
    return { next: i, content: buf.join('\n') };
  }
  return null;
}

function collectBracketArchFlow(lines, start) {
  const s0 = lines[start]?.trim() ?? '';
  if (!/^\[.+\]/.test(s0)) return null;
  const buf = [];
  let i = start;
  let sawArrow = false;
  while (i < lines.length) {
    const raw = lines[i] ?? '';
    const s = raw.trim();
    if (!s) {
      i++;
      const next = lines[i]?.trim() ?? '';
      if (buf.length >= 2 && /^\[.+\]/.test(next)) continue;
      if (buf.length >= 4 && sawArrow && !/^\[.+\]/.test(next)) break;
      continue;
    }
    if (/^NSIGHT 통합|^TCF 설계|^핵심 문장|^2\.\d+/.test(s) || isSectionHeading(s)) break;
    if (/^\[.+\]/.test(s) || /^\s*↓/.test(raw) || s === '↓' || /선택 적용$/.test(s)) {
      if (/↓/.test(raw)) sawArrow = true;
      buf.push(raw.trimEnd());
      i++;
      continue;
    }
    if (buf.length > 0 && !/^\[/.test(s)) break;
    break;
  }
  if (buf.length >= 4 && sawArrow) {
    return { next: i, content: buf.join('\n') };
  }
  return null;
}

function collectFlowBlock(lines, start) {
  const diagram = collectDiagramBlock(lines, start);
  if (diagram) return diagram;

  const buf = [];
  let i = start;
  while (i < lines.length) {
    const t = lines[i].trim();
    if (!t) break;
    if (isFlowLine(lines[i]) || (buf.length && /^[A-Za-z][\w.]*Handler/.test(t))) {
      buf.push(lines[i].trimEnd());
      i++;
      continue;
    }
    break;
  }
  return { next: i, content: buf.join('\n') };
}

function formatHeading(line) {
  const t = line.trim();
  let m;
  if ((m = t.match(/^([A-J])\.(\d+)\.(\d+)\s+(.+)$/))) return `#### ${m[1]}.${m[2]}.${m[3]} ${m[4]}`;
  if ((m = t.match(/^([A-J])\.(\d+)\s+(.+)$/))) return `### ${m[1]}.${m[2]} ${m[3]}`;
  if ((m = t.match(/^([A-J])\.\s+(.+)$/))) return `## ${m[1]}. ${m[2]}`;
  if ((m = t.match(/^(\d+)\.(\d+)\.(\d+)\s+(.+)$/))) return `#### ${m[1]}.${m[2]}.${m[3]} ${m[4]}`;
  if ((m = t.match(/^(\d+)\.(\d+)\s+(.+)$/))) return `### ${m[1]}.${m[2]} ${m[3]}`;
  if ((m = t.match(/^(\d+)\.\s+(.+)$/))) return `## ${m[1]}. ${m[2]}`;
  return null;
}

function shouldSkipLine(line) {
  return /^아래 내용은 NSIGHT TCF|^아래 내용을 NSIGHT TCF|^아래처럼 NSIGHT|^NSIGHT TCF Framework 개발자 가이드 목차/.test(
    line.trim()
  );
}

function formatDocxMarkdown(raw) {
  if (!raw) return '';
  const lines = decodeEntities(raw).split(/\r?\n/);
  const out = [];
  let i = 0;

  while (i < lines.length) {
    const t = lines[i].trim();

    if (!t) {
      if (out.length && out[out.length - 1] !== '') out.push('');
      i++;
      continue;
    }

    if (shouldSkipLine(t)) {
      i++;
      continue;
    }

    const bracketOnly = collectBracketBlock(lines, i);
    if (bracketOnly) {
      out.push('```text', bracketOnly.content, '```', '');
      i = bracketOnly.next;
      continue;
    }

    const numberedFlow = collectNumberedFlowBlock(lines, i);
    if (numberedFlow) {
      out.push('```text', numberedFlow.content, '```', '');
      i = numberedFlow.next;
      continue;
    }

    const bracketFlow = collectBracketArchFlow(lines, i);
    if (bracketFlow) {
      out.push('```text', bracketFlow.content, '```', '');
      i = bracketFlow.next;
      continue;
    }

    const heading = formatHeading(t);
    if (heading) {
      out.push(heading, '');
      i++;
      continue;
    }

    if (isJsonStart(lines, i)) {
      const { next, content } = collectJsonBlock(lines, i);
      out.push('```json', content.trim(), '```', '');
      i = next;
      continue;
    }

    if (isCodeBlockStart(lines, i)) {
      const code = collectCodeBlock(lines, i);
      if (code?.content?.trim()) {
        out.push('```' + code.lang, code.content.trim(), '```', '');
        i = code.next;
        continue;
      }
    }

    const archFlow = collectArchitectureFlowBlock(lines, i);
    if (archFlow) {
      out.push('```text', archFlow.content, '```', '');
      i = archFlow.next;
      continue;
    }

    const nextTrim = lines[i + 1]?.trim() ?? '';
    const flowCandidate =
      (FLOW_LABEL_RE.test(t) && /^↓/.test(nextTrim)) ||
      isFlowLine(lines[i]) ||
      (/^[A-Z]{2,3}$/.test(t) && /^[├└]/.test(nextTrim));
    if (flowCandidate) {
      // 연속 label+↓ 블록을 한 diagram으로 (업무코드 ↓ URL Context ↓ ...)
      const parts = [];
      let j = i;
      while (j < lines.length) {
        while (j < lines.length && !lines[j]?.trim()) j++;
        const a = lines[j]?.trim() ?? '';
        const b = lines[j + 1]?.trim() ?? '';
        if (a && /^↓/.test(b)) {
          parts.push(a, b);
          j += 2;
          continue;
        }
        if (FLOW_LABEL_RE.test(a) && !isProseLine(a) && parts.length && /^↓/.test(lines[j - 1]?.trim() ?? '')) {
          parts.push(a);
          j++;
          continue;
        }
        break;
      }
      while (j < lines.length && !lines[j]?.trim()) j++;
      const tail = lines[j]?.trim() ?? '';
      if (parts.length >= 2 && parts[parts.length - 1] === '↓' && FLOW_LABEL_RE.test(tail) && !isProseLine(tail)) {
        parts.push(tail);
        j++;
      }
      if (parts.length >= 2) {
        out.push('```text', parts.join('\n'), '```', '');
        i = j;
        continue;
      }
      const { next, content } = collectFlowBlock(lines, i);
      if (content.trim()) {
        out.push('```text', content, '```', '');
        i = next;
      } else {
        i++;
      }
      continue;
    }

    const logFields = tryLogFieldList(lines, i);
    if (logFields) {
      out.push(logFields.block);
      i = logFields.next;
      continue;
    }

    const table = tryExtractTable(lines, i);
    if (table) {
      out.push(table.table, '');
      i = table.next;
      continue;
    }

    if (isProseLine(t) || t.length > 0) {
      out.push(lines[i].trimEnd());
    }
    i++;
  }

  return out.join('\n').replace(/\n{3,}/g, '\n\n').trim();
}

function mergeAdjacentTextBlocks(md) {
  return md.replace(
    /```text\n([\s\S]*?)```\n\n```text\n([\s\S]*?)```/g,
    (all, a, b) => {
      const tail = a.trimEnd();
      const head = b.trimStart();
      if (/↓/.test(tail) || /↓/.test(head) || /[├└]/.test(tail) || /[├└]/.test(head)) {
        return '```text\n' + tail + '\n' + head + '\n```';
      }
      return all;
    }
  );
}

function formatDocxMarkdownFinal(raw) {
  return mergeAdjacentTextBlocks(formatDocxMarkdown(raw));
}

module.exports = { formatDocxMarkdown: formatDocxMarkdownFinal, decodeEntities };
