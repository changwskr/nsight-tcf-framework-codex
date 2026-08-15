const SCID='INF-610';
const $=(id)=>document.getElementById(id);
function setVal(id,v){$(id).value=v==null?'':v;}
async function search(){
  $('resultMeta').textContent='조회 중…';
  const res=await InfraApi.postService('ifina6100S0',{
    targetTypeCd:$('targetTypeCd').value, targetId:$('targetId').value.trim()||null
  },SCID);
  const dto=res.dto||{};
  setVal('opsHoursCd', dto.opsHoursCd||'24X365');
  setVal('haYn', dto.haYn||'N');
  setVal('haModeCd', dto.haModeCd||'');
  setVal('clusterYn', dto.clusterYn||'N');
  setVal('drYn', dto.drYn||'N');
  setVal('drModeCd', dto.drModeCd||'');
  setVal('rtoMinutes', dto.rtoMinutes);
  setVal('rpoMinutes', dto.rpoMinutes);
  setVal('backupYn', dto.backupYn||'N');
  setVal('monitoringYn', dto.monitoringYn||'N');
  setVal('remark', dto.remark||'');
  $('warnLine').textContent=(dto.warnings||[]).join(' · ')||'';
  $('resultMeta').textContent=`HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ifina6100S0 · ${dto.RSLT_CD||''}`;
}
async function save(){
  const payload={
    targetTypeCd:$('targetTypeCd').value, targetId:$('targetId').value.trim(),
    opsHoursCd:$('opsHoursCd').value, haYn:$('haYn').value, haModeCd:$('haModeCd').value.trim(),
    clusterYn:$('clusterYn').value, drYn:$('drYn').value, drModeCd:$('drModeCd').value.trim(),
    rtoMinutes:$('rtoMinutes').value===''?null:parseInt($('rtoMinutes').value,10),
    rpoMinutes:$('rpoMinutes').value===''?null:parseInt($('rpoMinutes').value,10),
    backupYn:$('backupYn').value, monitoringYn:$('monitoringYn').value, remark:$('remark').value.trim()
  };
  if(!payload.targetId){alert('대상ID 필수');return;}
  const res=await InfraApi.postService('ifina6100U0',payload,SCID); const dto=res.dto||{};
  if(dto.RSLT_CD&&dto.RSLT_CD!=='0000'){alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG||''}`);return;}
  if(dto.warnings&&dto.warnings.length) alert(dto.warnings.join('\n'));
  await search();
}
$('searchBtn').onclick=()=>search().catch(console.error);
$('saveBtn').onclick=()=>save().catch(console.error);
search().catch(console.error);
