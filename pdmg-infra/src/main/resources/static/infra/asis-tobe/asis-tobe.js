const SCID='INF-830';
const $=(id)=>document.getElementById(id);
async function search(){
  $('resultMeta').textContent='조회 중…';
  const res=await InfraApi.postService('ifina8300S0',{
    waveId:$('waveId').value||null, strategy7rCd:$('strategy7rCd').value||null,
    keyword:$('keyword').value.trim()||null, pageNo:1, pageSize:100
  },SCID);
  const dto=res.dto||{};
  const rows=Array.isArray(dto.rows)?dto.rows:[];
  $('rowCount').textContent=`${dto.totalCount??rows.length}`;
  $('rowBody').innerHTML=rows.length?rows.map(r=>`<tr>
    <td class="mono">${InfraApi.escapeHtml(r.targetTypeCd)}:${InfraApi.escapeHtml(r.targetId)}</td>
    <td>${InfraApi.escapeHtml(r.currentPlatformCd||'')}</td>
    <td>${InfraApi.escapeHtml(r.strategy7rCd||'')}</td>
    <td>${InfraApi.escapeHtml(r.targetPlatformCd||'')}</td>
    <td>${InfraApi.escapeHtml(r.waveId||'-')} ${r.waveName?`(${InfraApi.escapeHtml(r.waveName)})`:''}</td>
    <td>${r.difficultyCd==='H'?'<span class="badge badge--fail">H</span>':InfraApi.escapeHtml(r.difficultyCd||'')}</td>
    <td>${InfraApi.escapeHtml(r.gapHint||r.gapRemark||'')}</td></tr>`).join('')
    :'<tr><td colspan="7" class="empty">없음</td></tr>';
  $('resultMeta').textContent=`HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ifina8300S0`;
}
$('searchBtn').onclick=()=>search().catch(console.error);
search().catch(console.error);
