const SCID='INF-430';
const $=(id)=>document.getElementById(id);
function badge(sev){
  if(sev==='P0') return '<span class="badge badge--fail">P0</span>';
  if(sev==='P1') return '<span class="badge badge--warn">P1</span>';
  return `<span class="badge">${InfraApi.escapeHtml(sev||'')}</span>`;
}
async function search(){
  $('resultMeta').textContent='조회 중…';
  const res=await InfraApi.postService('ifina4300S0',{
    keyword:$('keyword').value.trim()||null, sourceCd:$('sourceCd').value||null,
    maxDaysLeft:parseInt($('maxDaysLeft').value,10)||365, pageNo:1, pageSize:50
  },SCID);
  const dto=res.dto||{}; const rows=Array.isArray(dto.rows)?dto.rows:[];
  $('resultCount').textContent=`${dto.totalCount||rows.length}건`;
  $('resultBody').innerHTML=rows.length?rows.map(r=>`<tr>
    <td>${badge(r.severityCd)}</td><td>${InfraApi.escapeHtml(r.sourceCd)}</td>
    <td class="mono">${InfraApi.escapeHtml(r.objectId)}</td><td>${InfraApi.escapeHtml(r.objectName)}</td>
    <td class="mono">${InfraApi.escapeHtml(r.assetId)}</td><td class="mono">${InfraApi.escapeHtml(r.eolDate)}</td>
    <td>${InfraApi.escapeHtml(r.daysLeft)}</td></tr>`).join('')
    :'<tr><td colspan="7" class="empty">데이터 없음</td></tr>';
  $('resultMeta').textContent=`HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ifina4300S0`;
}
$('searchBtn').onclick=()=>search().catch(console.error);
$('keyword').onkeydown=(e)=>{if(e.key==='Enter')search().catch(console.error);};
search().catch(console.error);
