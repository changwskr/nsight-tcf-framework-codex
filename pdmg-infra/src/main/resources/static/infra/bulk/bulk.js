const sample = [
  { serverId: 'INF-BULK-001', serverName: 'bulk-web-01', techRole: 'WEB', envCd: 'DEV', tierCd: 'TIER3', statusCd: 'DISCOVERED', groupId: 'SG-WAS-A' },
  { serverId: 'INF-BULK-002', serverName: 'bulk-app-01', techRole: 'WAS', envCd: 'DEV', tierCd: 'TIER2', statusCd: 'DISCOVERED', groupId: 'SG-WAS-A' },
  { serverId: 'INF-APP-001', serverName: 'dup-should-fail', techRole: 'WAS', envCd: 'PROD' }
];
document.getElementById('rowsJson').value = JSON.stringify(sample, null, 2);
document.getElementById('sampleBtn').onclick = () => {
  document.getElementById('rowsJson').value = JSON.stringify(sample, null, 2);
};
async function run(serviceId) {
  const meta = document.getElementById('meta');
  const summary = document.getElementById('summary');
  const pre = document.getElementById('resultPre');
  let rows;
  try {
    rows = JSON.parse(document.getElementById('rowsJson').value);
  } catch (e) {
    alert('JSON 파싱 실패');
    return;
  }
  meta.textContent = '실행 중…';
  const res = await InfraApi.postService(serviceId, {
    rows,
    applyMode: document.getElementById('applyMode').value
  }, 'INF-340');
  const dto = res.dto || {};
  summary.textContent = `ok=${dto.okCount ?? '-'} err=${dto.errorCount ?? '-'} proc=${dto.PROC_CNT ?? '-'}`;
  meta.textContent = `HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ${serviceId} · ${dto.RSLT_CD || ''} ${dto.RSLT_MSG || ''}`;
  pre.textContent = JSON.stringify(dto, null, 2);
}
document.getElementById('validateBtn').onclick = () => run('ifina3400V0').catch(console.error);
document.getElementById('applyBtn').onclick = () => run('ifina3400C0').catch(console.error);
