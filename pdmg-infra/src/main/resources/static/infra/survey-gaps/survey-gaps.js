const SCID='INF-930';
const $=(id)=>document.getElementById(id);
async function search(){
  $('resultMeta').textContent='조회 중…';
  const res=await InfraApi.postService('ifina9300S0',{
    severityCd:$('severityCd').value||null, gapType:$('gapType').value||'ALL',
    keyword:$('keyword').value.trim()||null
  },SCID);
  const dto=res.dto||{};
  const rows=Array.isArray(dto.rows)?dto.rows:[];
  $('sumLine').textContent=`CL ${dto.checklistCount||0} · HA ${dto.haCount||0} · CAP ${dto.capacityCount||0} · WAVE ${dto.waveCount||0} · ST ${dto.statusCount||0}`;
  $('rowBody').innerHTML=rows.length?rows.map(r=>{
    const sev=r.severityCd==='P0'?'badge--fail':(r.severityCd==='P1'?'badge--warn':'');
    return `<tr>
      <td><span class="badge ${sev}">${InfraApi.escapeHtml(r.severityCd||'')}</span></td>
      <td>${InfraApi.escapeHtml(r.gapType||'')}</td>
      <td class="mono">${InfraApi.escapeHtml(r.targetTypeCd)}:${InfraApi.escapeHtml(r.targetId)}</td>
      <td>${InfraApi.escapeHtml(r.missingItem||'')}</td>
      <td>${InfraApi.escapeHtml(r.statusCd||'')}</td>
      <td>${InfraApi.escapeHtml(r.ownerOrg||'')}</td>
      <td class="mono">${InfraApi.escapeHtml(r.dueDt||'')}</td></tr>`;
  }).join(''):'<tr><td colspan="7" class="empty">없음</td></tr>';
  $('resultMeta').textContent=`HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ifina9300S0 · ${rows.length}건`;
}
$('searchBtn').onclick=()=>search().catch(console.error);
search().catch(console.error);
