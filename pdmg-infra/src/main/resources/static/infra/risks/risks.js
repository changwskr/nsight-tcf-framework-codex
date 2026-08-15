const SCID='INF-020';
const $=(id)=>document.getElementById(id);
function badge(sev){
  if(sev==='P0') return '<span class="badge badge--fail">P0</span>';
  if(sev==='P1') return '<span class="badge badge--warn">P1</span>';
  return `<span class="badge">${InfraApi.escapeHtml(sev||'')}</span>`;
}
async function search(){
  $('resultMeta').textContent='조회 중…';
  const res=await InfraApi.postService('ifina0200S0',{
    keyword:$('keyword').value.trim()||null,
    severityCd:$('severityCd').value||null,
    riskType:$('riskType').value||'ALL',
    maxDaysLeft:365
  },SCID);
  const dto=res.dto||{}; const rows=Array.isArray(dto.rows)?dto.rows:[];
  $('kpiCl').textContent=dto.checklistOpenCount!=null?dto.checklistOpenCount:'-';
  $('kpiEol').textContent=dto.eolCount!=null?dto.eolCount:'-';
  $('kpiGate').textContent=dto.gateOpenCount!=null?dto.gateOpenCount:'-';
  $('kpiRows').textContent=rows.length;
  $('resultCount').textContent=`${rows.length}건`;
  $('resultBody').innerHTML=rows.length?rows.map(r=>`<tr>
    <td>${badge(r.severityCd)}</td><td>${InfraApi.escapeHtml(r.riskType)}</td>
    <td class="mono">${InfraApi.escapeHtml(r.targetTypeCd)}:${InfraApi.escapeHtml(r.targetId)}</td>
    <td>${InfraApi.escapeHtml(r.title)}<br><small class="mono">${InfraApi.escapeHtml(r.detail||'')}</small></td>
    <td>${InfraApi.escapeHtml(r.gateId)}</td><td>${InfraApi.escapeHtml(r.remark||'')}</td></tr>`).join('')
    :'<tr><td colspan="6" class="empty">데이터 없음</td></tr>';
  $('resultMeta').textContent=`HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ifina0200S0`;
}
$('searchBtn').onclick=()=>search().catch(console.error);
$('keyword').onkeydown=(e)=>{if(e.key==='Enter')search().catch(console.error);};
search().catch(console.error);
