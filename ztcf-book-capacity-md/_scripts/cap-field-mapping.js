/**
 * CAP-010(capacity.html) · ENV-002(env-002.html) ↔ 부록 A 양식 1:1 필드 매핑
 * DTO: CapacityCalculationCDTO · CapacityPlannerRequest (tcf-oc)
 */

const CAP_010_FIELDS = [
  { appendix: 'projectName', dto: 'projectName', uiId: 'projectName', uiLabel: '프로젝트명', type: 'string', required: false, default: '6,000지점 표준', api: 'POST /api/oc/capacity/calculate' },
  { appendix: 'branchCount', dto: 'branchCount', uiId: 'branchCount', uiLabel: '지점 수', type: 'int', required: true, default: '6000', note: '부록 A·ENV 기본 3600 — 화면 기본값 상이' },
  { appendix: 'userPerBranch', dto: 'userPerBranch', uiId: 'userPerBranch', uiLabel: '지점당 사용자', type: 'int', required: true, default: '6' },
  { appendix: 'totalUsers', dto: 'resolvedTotalUsers()', uiId: 'totalUsers', uiLabel: '전체 사용자', type: 'int (readonly)', required: true, default: 'branchCount×userPerBranch', formula: 'branchCount × userPerBranch' },
  { appendix: 'designedSessions', dto: '(산출)', uiId: 'designedSessions', uiLabel: '설계 세션 수', type: 'int (readonly)', required: false, default: 'totalUsers×(1+sessionMarginRate)', formula: 'CAP-020 세션 산출' },
  { appendix: 'sessionMarginRate', dto: 'sessionMarginRate', uiId: 'sessionMarginRate', uiLabel: '세션 여유율', type: 'double', required: false, default: '0.30', uiControl: 'select 20%/30%' },
  { appendix: 'sessionTimeoutMin', dto: 'sessionTimeoutMin', uiId: 'sessionTimeout', uiLabel: '세션 타임아웃', type: 'int', required: false, default: '60', uiControl: 'radio name=sessionTimeout 60|90' },
  { appendix: 'concurrentRequestRates', dto: 'concurrentRequestRates', uiId: 'rate', uiLabel: '동시요청률', type: 'List<Double>', required: true, default: '0.03,0.05,0.10,0.15', uiControl: 'checkbox name=rate 3|5|10|15%', note: 'UI는 % 정수, API는 소수' },
  { appendix: 'targetResponseTimes', dto: 'targetResponseTimes', uiId: 'timeout', uiLabel: '목표 응답(초)', type: 'List<Integer>', required: true, default: '3,4,5', uiControl: 'checkbox name=timeout' },
  { appendix: 'vmSpecCode', dto: 'vmSpecCode', uiId: 'vmSpec', uiLabel: 'VM 사양', type: 'string', required: false, default: '8C64G', uiControl: 'radio name=vmSpec 2C16G|4C32G|8C64G|16C128G|32C256G' },
  { appendix: 'tpsPerCore', dto: 'tpsPerCore', uiId: 'tpsPerCore', uiLabel: 'Core당 TPS', type: 'int', required: false, default: '35', note: 'TPMC 가이드 행 클릭 시 연동' },
  { appendix: 'tpmcPerTps', dto: 'tpmcPerTps', uiId: 'tpmcPerTps', uiLabel: '1 TPS당 TPMC', type: 'int', required: false, default: '3000' },
  { appendix: 'avgThreadHoldSec', dto: 'avgThreadHoldSec', uiId: 'avgThreadHoldSec', uiLabel: '평균 Thread 점유(초)', type: 'double', required: false, default: '1.2' },
  { appendix: 'threadMarginRate', dto: 'threadMarginRate', uiId: 'threadMarginRate', uiLabel: 'Thread 여유율', type: 'double', required: false, default: '1.2' },
  { appendix: 'maxThreadMarginRate', dto: 'maxThreadMarginRate', uiId: 'maxThreadMarginRate', uiLabel: 'maxThreads 배율', type: 'double', required: false, default: '1.3' },
  { appendix: 'apType', dto: 'apType', uiId: 'apType', uiLabel: 'AP 유형', type: 'enum', required: false, default: 'GENERAL', values: 'GENERAL|SINGLE_VIEW' },
  { appendix: 'avgDbConnectionHoldSec', dto: 'avgDbConnectionHoldSec', uiId: 'avgDbConnectionHoldSec', uiLabel: 'DB Connection 점유(초)', type: 'double', required: false, default: '0.15/0.20', note: 'AP 유형별 기본' },
  { appendix: 'dbTransactionUsageRatio', dto: 'dbTransactionUsageRatio', uiId: 'dbTransactionUsageRatio', uiLabel: 'DB 사용 거래 비율', type: 'double', required: false, default: '1.0' },
  { appendix: 'poolSafetyFactor', dto: 'poolSafetyFactor', uiId: 'poolSafetyFactor', uiLabel: 'Pool 안전계수', type: 'double', required: false, default: '1.3' },
  { appendix: 'threadDbUsageRatio', dto: 'threadDbUsageRatio', uiId: 'threadDbUsageRatio', uiLabel: 'Thread→DB 사용 비율', type: 'double', required: false, default: '0.30' },
  { appendix: 'minPoolPerVm', dto: 'minPoolPerVm', uiId: 'minPoolPerVm', uiLabel: '최소 Pool/VM', type: 'int', required: false, default: '30' },
  { appendix: 'activeActive', dto: 'activeActive', uiId: 'activeActive', uiLabel: '2센터 Active-Active', type: 'boolean', required: false, default: 'true' },
  { appendix: 'drValidation', dto: 'drValidation', uiId: 'drValidation', uiLabel: 'DR·잔여 TPS', type: 'boolean', required: false, default: 'true' },
  { appendix: 'validateDbPool', dto: 'validateDbPool', uiId: 'validateDbPool', uiLabel: 'DB Session 한도 검증', type: 'boolean', required: false, default: 'true' },
  { appendix: 'dbSessionLimit', dto: 'dbSessionLimit', uiId: 'dbSessionLimit', uiLabel: 'DB Session 한도', type: 'int', required: false, default: '500' },
  { appendix: 'calculationStep', dto: 'calculationStep', uiId: '(내부)', uiLabel: '산정 단계', type: 'string', required: false, default: 'ALL', values: '020|030|040|050|ALL', api: 'POST /api/oc/capacity/calculate-step' },
];

const ENV_002_FIELDS = [
  { appendix: 'scenarioName', dto: 'scenarioName', uiId: '(자동)', uiLabel: '시나리오명', type: 'string', required: false, default: 'ENV-002 산정' },
  { appendix: 'branchCount', dto: 'branchCount', uiId: 'capBranchCount', uiLabel: '지점 수', type: 'int', required: true, default: '3600' },
  { appendix: 'usersPerBranch', dto: 'usersPerBranch', uiId: 'capUsersPerBranch', uiLabel: '지점당 사용자', type: 'int', required: true, default: '6', note: 'CAP DTO는 userPerBranch(단수)' },
  { appendix: 'totalUsers', dto: 'totalUsers', uiId: 'capTotalUsers', uiLabel: '전체 사용자', type: 'int', required: true, default: '21600', formula: 'branchCount × usersPerBranch' },
  { appendix: 'vmProfileId', dto: 'vmProfileId', uiId: 'capVm', uiLabel: 'VM 프로파일', type: 'string', required: true, default: '8CORE-32GB', uiControl: 'radio name=capVm' },
  { appendix: 'customVm', dto: 'customVm', uiId: 'capVm=CUSTOM', uiLabel: '커스텀 VM', type: 'boolean', required: false, default: 'false' },
  { appendix: 'customCore', dto: 'customCore', uiId: 'capCustomCore', uiLabel: '커스텀 Core', type: 'int', required: false, default: '8' },
  { appendix: 'customMemoryGb', dto: 'customMemoryGb', uiId: 'capCustomMemory', uiLabel: '커스텀 메모리(GB)', type: 'int', required: false, default: '64' },
  { appendix: 'tpsPerCoreMin', dto: 'tpsPerCoreMin', uiId: 'capTpsPerCoreMin', uiLabel: 'Core TPS Min', type: 'int', required: false, default: '30' },
  { appendix: 'tpsPerCoreBase', dto: 'tpsPerCoreBase', uiId: 'capTpsPerCoreBase', uiLabel: 'Core TPS Base', type: 'int', required: false, default: '35' },
  { appendix: 'tpsPerCoreMax', dto: 'tpsPerCoreMax', uiId: 'capTpsPerCoreMax', uiLabel: 'Core TPS Max', type: 'int', required: false, default: '40' },
  { appendix: 'tpmcPerTps', dto: 'tpmcPerTps', uiId: 'capTpmcPerTps', uiLabel: '1 TPS당 TPMC', type: 'int', required: true, default: '3000' },
  { appendix: 'manualCoreTps', dto: 'manualCoreTps', uiId: 'capManualCoreTps', uiLabel: 'Core TPS 수동', type: 'boolean', required: false, default: 'false' },
  { appendix: 'actualRequestPercents', dto: 'actualRequestPercents', uiId: 'capPercent', uiLabel: '동시요청률', type: 'List<Integer>', required: true, default: '3,5,10,15', uiControl: 'checkbox name=capPercent' },
  { appendix: 'responseTimeoutSeconds', dto: 'responseTimeoutSeconds', uiId: 'capTimeout', uiLabel: '목표 응답(초)', type: 'List<Integer>', required: true, default: '3,4,5', uiControl: 'checkbox name=capTimeout' },
  { appendix: 'sessionIdleMinutes', dto: 'sessionIdleMinutes', uiId: 'capSession', uiLabel: '세션 Idle(분)', type: 'List<Integer>', required: false, default: '60', uiControl: 'checkbox name=capSession' },
  { appendix: 'activeActive', dto: 'activeActive', uiId: 'capActiveActive', uiLabel: 'Active-Active', type: 'boolean', required: false, default: 'true' },
  { appendix: 'drValidation', dto: 'drValidation', uiId: 'capDrValidation', uiLabel: 'DR 검증', type: 'boolean', required: false, default: 'true' },
  { appendix: 'validateDbPool', dto: 'validateDbPool', uiId: 'capValidateDbPool', uiLabel: 'DB Pool 검증', type: 'boolean', required: false, default: 'true' },
  { appendix: 'includeSettingExamples', dto: 'includeSettingExamples', uiId: 'capIncludeExamples', uiLabel: '설정 예시 포함', type: 'boolean', required: false, default: 'true' },
  { appendix: 'hikariPoolPerVm', dto: 'hikariPoolPerVm', uiId: '(산출 입력)', uiLabel: 'Hikari Pool/VM', type: 'int', required: false, default: '0', note: 'ENV-003/004 Grid 연동' },
  { appendix: 'dbSessionLimit', dto: 'dbSessionLimit', uiId: '(baseline)', uiLabel: 'DB Session 한도', type: 'int', required: false, default: '500' },
];

const APPENDIX_A_CORE = [
  'branchCount', 'userPerBranch', 'totalUsers', 'sessionMarginRate', 'sessionTimeoutMin',
  'concurrentRequestRates', 'targetResponseTimes', 'vmSpecCode', 'tpsPerCore', 'tpmcPerTps',
  'avgThreadHoldSec', 'activeActive', 'drValidation', 'validateDbPool', 'dbSessionLimit',
  'avgDbConnectionHoldSec', 'poolSafetyFactor', 'minPoolPerVm',
];

function mdRow(f, screen) {
  const note = f.note ? ` · ${f.note}` : '';
  return `| ${f.appendix} | ${f.dto} | \`${f.uiId}\` | ${f.uiLabel} | ${f.type} | ${f.default} |${note}`;
}

function buildCap010MappingTable() {
  const header = '| 부록 A 필드 | DTO (CapacityCalculationCDTO) | UI id (capacity.html) | 화면 라벨 | 타입 | 기본값 |';
  const sep = '|-------------|------------------------------|------------------------|----------|------|--------|';
  return [header, sep, ...CAP_010_FIELDS.map((f) => mdRow(f, 'CAP'))].join('\n');
}

function buildEnv002MappingTable() {
  const header = '| 부록 A 필드 | DTO (CapacityPlannerRequest) | UI id (env-002.html) | 화면 라벨 | 타입 | 기본값 |';
  const sep = '|-------------|-------------------------------|----------------------|----------|------|--------|';
  return [header, sep, ...ENV_002_FIELDS.map((f) => mdRow(f, 'ENV'))].join('\n');
}

function buildAppendixAUnifiedTable() {
  const lines = [
    '| 부록 A | CAP-010 DTO | CAP UI | ENV-002 DTO | ENV UI | 비고 |',
    '|--------|-------------|--------|-------------|--------|------|',
  ];
  const capByAppendix = Object.fromEntries(CAP_010_FIELDS.map((f) => [f.appendix, f]));
  const envByAppendix = Object.fromEntries(ENV_002_FIELDS.map((f) => [f.appendix, f]));
  const keys = [...new Set([...APPENDIX_A_CORE, ...CAP_010_FIELDS.map((f) => f.appendix), ...ENV_002_FIELDS.map((f) => f.appendix)])];
  for (const k of keys) {
    const c = capByAppendix[k];
    const e = envByAppendix[k];
    if (!c && !e) continue;
    const note = [];
    if (c && e && c.dto !== e.dto) note.push(`DTO명: ${c.dto} / ${e.dto}`);
    if (k === 'branchCount') note.push('CAP 기본 6000, ENV 3600');
    if (k === 'userPerBranch' || k === 'usersPerBranch') note.push('단수 userPerBranch vs 복수 usersPerBranch');
    lines.push(`| ${k} | ${c?.dto ?? '—'} | ${c?.uiId ?? '—'} | ${e?.dto ?? '—'} | ${e?.uiId ?? '—'} | ${note.join('; ') || '—'} |`);
  }
  return lines.join('\n');
}

function buildAppendixARichBlock() {
  return `### CAP-010 · ENV-002 필드 1:1 매핑 (코드 기준)

> 화면: \`/oc/capacity.html\` (CAP-010~050) · \`/oc/env-002.html\` (ENV-002)  
> API: \`POST /api/oc/capacity/calculate\` · \`POST /api/oc/env/analyze\`

#### CAP-010 (CapacityCalculationCDTO)

${buildCap010MappingTable()}

#### ENV-002 (CapacityPlannerRequest)

${buildEnv002MappingTable()}

#### 부록 A 통합 대조

${buildAppendixAUnifiedTable()}

#### 명칭 차이 (작성 시 주의)

| 부록 A 관례 | CAP DTO | ENV DTO | UI |
|-------------|---------|---------|-----|
| userPerBranch | userPerBranch | usersPerBranch | capUsersPerBranch |
| concurrentRate | concurrentRequestRates (0.03…) | actualRequestPercents (3…) | rate / capPercent |
| targetResponseSec | targetResponseTimes | responseTimeoutSeconds | timeout / capTimeout |
| vmSpec | vmSpecCode (8C64G) | vmProfileId (8CORE-32GB) | vmSpec / capVm |
| sessionTimeoutMin | sessionTimeoutMin | sessionIdleMinutes | sessionTimeout / capSession |

#### 산출 필드 (부록 A 산출 항목 ↔ API 응답)

| 부록 A 산출 | CapacityCalculationDDTO | 화면 위치 |
|-------------|-------------------------|----------|
| totalUsers | totalUsers | #totalUsers (readonly) |
| designedSessions | designedSessions | #designedSessions |
| vmTpsAtBase | vmTpsAtBase | VM 카드 TPS 배너 |
| profilePoolCap | profilePoolCap | CAP-050 결과 |
`;
}

module.exports = {
  CAP_010_FIELDS,
  ENV_002_FIELDS,
  APPENDIX_A_CORE,
  buildCap010MappingTable,
  buildEnv002MappingTable,
  buildAppendixAUnifiedTable,
  buildAppendixARichBlock,
};
