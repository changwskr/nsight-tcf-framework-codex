const SCID='INF-640';
const $=(id)=>document.getElementById(id);
async function search(){
  $('resultMeta').textContent='조회 중…';
  const ids=$('targetIds').value.split(/[,\s]+/).map(s=>s.trim()).filter(Boolean);
  const res=await InfraApi.postService('ifina6400S0',{
    targetTypeCd:'GROUP', metricScopeCd:$('metricScopeCd').value, targetIdList:ids
  },SCID);
  const dto=res.dto||{};
  const targetIds=Array.isArray(dto.targetIds)?dto.targetIds:ids;
  const metrics=Array.isArray(dto.metrics)?dto.metrics:[];
  $('scopeTag').textContent=dto.metricScopeCd||'';
  $('cmpHead').innerHTML=`<tr><th>Metric</th>${targetIds.map(t=>`<th class="mono">${InfraApi.escapeHtml(t)}</th>`).join('')}</tr>`;
  $('cmpBody').innerHTML=metrics.length?metrics.map(m=>{
    const values=m.values||{};
    return `<tr><td>${InfraApi.escapeHtml(m.metricLabel||m.metricKey)}</td>${
      targetIds.map(t=>{
        const v=values[t];
        const hi=m.maxTargetId===t?' style="font-weight:700"':'';
        return `<td${hi}>${v==null?'-':v}</td>`;
      }).join('')
    }</tr>`;
  }).join(''):'<tr><td class="empty" colspan="99">데이터 없음</td></tr>';
  $('resultMeta').textContent=`HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ifina6400S0 · ${dto.RSLT_CD||''}`;
}
$('searchBtn').onclick=()=>search().catch(console.error);
search().catch(console.error);
