const SCID='INF-620';
const $=(id)=>document.getElementById(id);
function num(id){const v=$(id).value; return v===''?null:Number(v);}
function setVal(id,v){$(id).value=v==null?'':v;}
async function search(){
  $('resultMeta').textContent='조회 중…';
  const res=await InfraApi.postService('ifina6200S0',{
    targetTypeCd:$('targetTypeCd').value, targetId:$('targetId').value.trim()||null
  },SCID);
  const dto=res.dto||{};
  const rows=Array.isArray(dto.rows)?dto.rows:[];
  $('warnLine').textContent=(dto.warnings||[]).join(' · ')||'';
  $('rowBody').innerHTML=rows.length?rows.map(r=>`<tr>
    <td>${InfraApi.escapeHtml(r.metricScopeCd)}</td>
    <td>${r.cpuPct??''}</td><td>${r.memPct??''}</td><td>${r.tps??''}</td>
    <td>${r.respP95Ms??''}</td><td>${r.dbConnPeak??''}</td>
    <td class="mono">${InfraApi.escapeHtml(r.capturedAt||'')}</td></tr>`).join('')
    :'<tr><td colspan="7" class="empty">없음</td></tr>';
  $('resultMeta').textContent=`HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ifina6200S0 · ${dto.RSLT_CD||''}`;
}
async function save(){
  const payload={
    targetTypeCd:$('targetTypeCd').value, targetId:$('targetId').value.trim(),
    metricScopeCd:$('metricScopeCd').value, capturedAt:$('capturedAt').value.trim()||null,
    cpuPct:num('cpuPct'), memPct:num('memPct'), tps:num('tps'), respP95Ms:num('respP95Ms'),
    dbConnPeak:$('dbConnPeak').value===''?null:parseInt($('dbConnPeak').value,10),
    remark:$('remark').value.trim()
  };
  if(!payload.targetId){alert('대상ID 필수');return;}
  const res=await InfraApi.postService('ifina6200U0',payload,SCID); const dto=res.dto||{};
  if(dto.RSLT_CD&&dto.RSLT_CD!=='0000'){alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG||''}`);return;}
  if(dto.warnings&&dto.warnings.length) alert(dto.warnings.join('\n'));
  await search();
}
$('searchBtn').onclick=()=>search().catch(console.error);
$('saveBtn').onclick=()=>save().catch(console.error);
search().catch(console.error);
