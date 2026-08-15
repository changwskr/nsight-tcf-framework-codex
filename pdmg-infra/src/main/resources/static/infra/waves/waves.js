const SCID='INF-820';
const $=(id)=>document.getElementById(id);
function setVal(id,v){$(id).value=v==null?'':v;}
async function search(){
  $('resultMeta').textContent='조회 중…';
  const res=await InfraApi.postService('ifina8200S0',{},SCID);
  const dto=res.dto||{};
  const rows=Array.isArray(dto.rows)?dto.rows:[];
  $('warnLine').textContent=(dto.warnings||[]).slice(0,2).join(' · ')||'';
  $('rowBody').innerHTML=rows.length?rows.map(r=>`<tr data-id="${InfraApi.escapeHtml(r.waveId)}">
    <td>${r.sequenceNo??''}</td>
    <td class="mono"><button class="btn-icon" data-action="pick" type="button">${InfraApi.escapeHtml(r.waveId)}</button></td>
    <td>${InfraApi.escapeHtml(r.waveName||'')}</td>
    <td class="mono">${InfraApi.escapeHtml(r.plannedStart||'')}</td>
    <td class="mono">${InfraApi.escapeHtml(r.plannedEnd||'')}</td>
    <td>${InfraApi.escapeHtml(r.statusCd||'')}</td>
    <td>${r.planCnt??0}</td>
    <td>${InfraApi.escapeHtml(r.remark||'')}</td></tr>`).join('')
    :'<tr><td colspan="8" class="empty">없음</td></tr>';
  $('resultMeta').textContent=`HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ifina8200S0 · ${(dto.warnings||[]).length} warnings`;
  window.__waves=rows;
}
function pick(id){
  const r=(window.__waves||[]).find(x=>x.waveId===id); if(!r)return;
  setVal('waveId', r.waveId); setVal('waveName', r.waveName);
  setVal('sequenceNo', r.sequenceNo); setVal('plannedStart', r.plannedStart);
  setVal('plannedEnd', r.plannedEnd); setVal('statusCd', r.statusCd||'PLANNED');
  setVal('remark', r.remark);
}
async function save(){
  const payload={
    waveId:$('waveId').value.trim(), waveName:$('waveName').value.trim(),
    sequenceNo:$('sequenceNo').value===''?null:parseInt($('sequenceNo').value,10),
    plannedStart:$('plannedStart').value.trim(), plannedEnd:$('plannedEnd').value.trim(),
    statusCd:$('statusCd').value, remark:$('remark').value.trim()
  };
  if(!payload.waveId||!payload.waveName){alert('Wave ID/Name 필수');return;}
  const res=await InfraApi.postService('ifina8200U0',payload,SCID); const dto=res.dto||{};
  if(dto.RSLT_CD&&dto.RSLT_CD!=='0000'){alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG||''}`);return;}
  await search();
}
$('searchBtn').onclick=()=>search().catch(console.error);
$('saveBtn').onclick=()=>save().catch(console.error);
$('rowBody').onclick=(ev)=>{const btn=ev.target.closest('[data-action="pick"]'); if(!btn)return; pick(btn.closest('tr').dataset.id);};
search().catch(console.error);
