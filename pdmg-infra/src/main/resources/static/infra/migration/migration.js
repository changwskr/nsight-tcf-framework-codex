const SCID='INF-810';
const $=(id)=>document.getElementById(id);
let rowsCache=[]; let editMode=false;
async function search(){
  $('resultMeta').textContent='조회 중…';
  const res=await InfraApi.postService('ifina8100S0',{
    waveId:$('waveId').value||null, strategy7rCd:$('strategy7rCd').value||null,
    keyword:$('keyword').value.trim()||null, pageNo:1, pageSize:100
  },SCID);
  const dto=res.dto||{};
  rowsCache=Array.isArray(dto.rows)?dto.rows:[];
  $('warnLine').textContent=(dto.warnings||[]).slice(0,3).join(' · ')||`${dto.totalCount??0}건`;
  $('rowBody').innerHTML=rowsCache.length?rowsCache.map((r,i)=>`<tr data-index="${i}">
    <td class="mono">${InfraApi.escapeHtml(r.planId)}</td>
    <td class="mono">${InfraApi.escapeHtml(r.targetTypeCd)}:${InfraApi.escapeHtml(r.targetId)}</td>
    <td>${InfraApi.escapeHtml(r.currentPlatformCd||'')}</td>
    <td>${InfraApi.escapeHtml(r.strategy7rCd)}</td>
    <td>${InfraApi.escapeHtml(r.targetPlatformCd||'')}</td>
    <td>${InfraApi.escapeHtml(r.difficultyCd||'')}</td>
    <td>${r.waveId?InfraApi.escapeHtml(r.waveId):'<span class="badge badge--warn">미배정</span>'}</td>
    <td>${InfraApi.escapeHtml(r.statusCd||'')}</td>
    <td><button class="btn-icon" data-action="edit" type="button">수정</button>
        <button class="btn-icon" data-action="del" type="button">삭제</button></td></tr>`).join('')
    :'<tr><td colspan="9" class="empty">없음</td></tr>';
  $('resultMeta').textContent=`HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ifina8100S0`;
}
function openCreate(){
  editMode=false; $('modalTitle').textContent='계획 등록';
  $('formPlanId').value='MP-'+Date.now().toString().slice(-6); $('formPlanId').readOnly=false;
  $('formTargetType').value='GROUP'; $('formTargetId').value='';
  $('form7r').value='REHOST'; $('formAsis').value='VM'; $('formTobe').value='IAAS';
  $('formDiff').value='M'; $('formWave').value=''; $('formStatus').value='TARGET_DEFINED'; $('formRemark').value='';
  $('editModal').hidden=false;
}
function openEdit(r){
  editMode=true; $('modalTitle').textContent='계획 수정';
  $('formPlanId').value=r.planId; $('formPlanId').readOnly=true;
  $('formTargetType').value=r.targetTypeCd||'GROUP'; $('formTargetId').value=r.targetId||'';
  $('form7r').value=r.strategy7rCd||'REHOST'; $('formAsis').value=r.currentPlatformCd||'';
  $('formTobe').value=r.targetPlatformCd||'IAAS'; $('formDiff').value=r.difficultyCd||'M';
  $('formWave').value=r.waveId||''; $('formStatus').value=r.statusCd||'TARGET_DEFINED';
  $('formRemark').value=r.remark||''; $('editModal').hidden=false;
}
async function save(){
  const payload={
    planId:$('formPlanId').value.trim(), targetTypeCd:$('formTargetType').value,
    targetId:$('formTargetId').value.trim(), strategy7rCd:$('form7r').value,
    currentPlatformCd:$('formAsis').value.trim(), targetPlatformCd:$('formTobe').value,
    difficultyCd:$('formDiff').value, waveId:$('formWave').value||null,
    statusCd:$('formStatus').value, remark:$('formRemark').value.trim()
  };
  if(!payload.planId||!payload.targetId||!payload.targetPlatformCd){alert('필수값 확인');return;}
  const svc=editMode?'ifina8100U0':'ifina8100C0';
  const res=await InfraApi.postService(svc,payload,SCID); const dto=res.dto||{};
  if(dto.RSLT_CD&&dto.RSLT_CD!=='0000'){alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG||''}`);return;}
  if(dto.warnings&&dto.warnings.length) alert(dto.warnings.join('\n'));
  $('editModal').hidden=true; await search();
}
async function remove(r){
  if(!confirm(`삭제?\n${r.planId}`))return;
  const res=await InfraApi.postService('ifina8100D0',{planIdList:[r.planId]},SCID); const dto=res.dto||{};
  if(dto.RSLT_CD&&dto.RSLT_CD!=='0000'){alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG||''}`);return;}
  await search();
}
$('searchBtn').onclick=()=>search().catch(console.error);
$('addBtn').onclick=openCreate;
$('saveBtn').onclick=()=>save().catch(console.error);
$('rowBody').onclick=(ev)=>{
  const btn=ev.target.closest('[data-action]'); if(!btn)return;
  const r=rowsCache[Number(btn.closest('tr').dataset.index)]; if(!r)return;
  if(btn.dataset.action==='edit') openEdit(r);
  if(btn.dataset.action==='del') remove(r).catch(console.error);
};
$('editModal').querySelectorAll('[data-close="true"]').forEach(el=>el.onclick=()=>{$('editModal').hidden=true;});
search().catch(console.error);
