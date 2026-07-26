'use strict';

/** docx (N) → 출력 파일·제목·관련 Manual 장 */
const NAMING_SPECS = [
  { n: 1, slug: '총정리', title: '명명규칙 총정리', related: [14] },
  { n: 2, slug: '최상위-원칙', title: '명명규칙 최상위 원칙', related: [14] },
  { n: 3, slug: '업무코드-Context-WAR-Package', title: '업무코드 / Context / WAR / Package', related: [15] },
  { n: 4, slug: '업무코드-표준표', title: '업무코드 표준표', related: [15] },
  { n: 5, slug: '모듈-설계기준', title: 'Gradle 모듈 설계기준', related: [8] },
  { n: 6, slug: 'Package', title: 'Package 명명규칙', related: [13] },
  { n: 7, slug: 'ServiceId', title: 'ServiceId 명명규칙', related: [16] },
  { n: 8, slug: '거래코드', title: '거래코드(Transaction Code) 명명규칙', related: [17] },
  { n: 9, slug: 'Java-Class', title: 'Java Class 명명규칙', related: [14] },
  { n: 10, slug: 'Java-Method-Field', title: 'Java Method / Field 명명규칙', related: [14] },
  { n: 11, slug: 'Java-DTO', title: 'Java DTO 명명규칙', related: [18] },
  { n: 12, slug: 'MyBatis-Mapper-SQL', title: 'MyBatis Mapper / XML / SQL ID 명명규칙', related: [28] },
  { n: 13, slug: 'DB-객체', title: 'DB 객체 명명규칙', related: [14] },
  { n: 14, slug: '오류코드', title: '오류코드 명명규칙', related: [33] },
  { n: 15, slug: '화면번호', title: '화면번호 명명규칙', related: [14] },
  { n: 16, slug: '로그-감사로그', title: '로그 / 감사로그 명명규칙', related: [34, 35] },
  { n: 17, slug: '화면-ServiceId-연결', title: '화면번호와 ServiceId 연결', related: [16, 21] },
  { n: 18, slug: 'Gateway-라우팅', title: 'Gateway 라우팅 명명규칙', related: [22] },
  { n: 19, slug: 'Batch-Scheduler', title: 'Batch / Scheduler 명명규칙', related: [45] },
  { n: 20, slug: 'Cache', title: 'Cache 명명규칙', related: [43] },
  { n: 21, slug: 'Header-항목', title: 'Header 항목 명명규칙', related: [21] },
];

function pad2(n) {
  return String(n).padStart(2, '0');
}

function namingFile(n) {
  const spec = NAMING_SPECS.find((s) => s.n === n);
  if (!spec) return null;
  return `명명규칙-${pad2(n)}-${spec.slug}.md`;
}

function namingLink(n) {
  const file = namingFile(n);
  const spec = NAMING_SPECS.find((s) => s.n === n);
  if (!file || !spec) return null;
  return `[${spec.title}](./${file})`;
}

module.exports = { NAMING_SPECS, namingFile, namingLink, pad2 };
