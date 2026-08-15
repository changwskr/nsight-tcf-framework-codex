const SCID='INF-520';
const $=(id)=>document.getElementById(id);
let rowsCache=[]; let editMode=false;
async function search(){
  $('resultMeta').textContent='조회 중…';
  const res=await InfraApi.postService('ifina5200S0',{
    keyword:$('keyword').value.trim()||null,
    criticalYn:$('criticalYn').value||null,
    pageNo:1, pageSize:100
  },SCID);
  const dto=res.dto||{};
  rowsCache=Array.isArray(dto.rows)?dto.rows:[];
  $('rowCount').textContent=`${dto.totalCount??rowsCache.length}`;
  $('rowBody').innerHTML=rowsCache.length?rowsCache.map((r,i)=>`<tr data-index="${i}">
    <td class="mono">${InfraApi.escapeHtml(r.interfaceId||'')}</td>
    <td class="mono">${InfraApi.escapeHtml(r.fromAppId||'')}</td>
    <td class="mono">${InfraApi.escapeHtml(r.toAppId||'')}</td>
    <td>${InfraApi.escapeHtml(r.toExternalName||'')}</td>
    <td>${InfraApi.escapeHtml(r.protocolCd||'')}</td>
    <td>${InfraApi.escapeHtml(r.directionCd||'')}</td>
    <td>${InfraApi.escapeHtml(r.criticalYn||'')}</td>
    <td><button class="btn-icon" data-action="edit" type="button">수정</button>
        <button class="btn-icon" data-action="del" type="button">삭제</button></td></tr>`).join('')
    :'<tr><td colspan="8" class="empty">없음</td></tr>';
  $('resultMeta').textContent=`HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ifina5200S0`;
}
function openCreate(){
  editMode=false; $('modalTitle').textContent='Interface 등록';
  $('formId').value='IF-'+Date.now().toString().slice(-6); $('formId').readOnly=false;
  $('formFrom').value='APP-ONLINE-A'; $('formTo').value='APP-ONLINE-B'; $('formExt').value='';
  $('formProto').value='HTTP'; $('formDir').value='OUTBOUND'; $('formCrit').value='N'; $('formRemark').value='';
  $('editModal').hidden=false;
}
function openEdit(r){
  editMode=true; $('modalTitle').textContent='Interface 수정';
  $('formId').value=r.interfaceId||''; $('formId').readOnly=true;
  $('formFrom').value=r.fromAppId||''; $('formTo').value=r.toAppId||'';
  $('formExt').value=r.toExternalName||''; $('formProto').value=r.protocolCd||'HTTP';
  $('formDir').value=r.directionCd||'OUTBOUND'; $('formCrit').value=r.criticalYn||'N';
  $('formRemark').value=r.remark||''; $('editModal').hidden=false;
}
async function save(){
  const payload={
    interfaceId:$('formId').value.trim(), fromAppId:$('formFrom').value.trim(),
    toAppId:$('formTo').value.trim()||null, toExternalName:$('formExt').value.trim()||null,
    protocolCd:$('formProto').value, directionCd:$('formDir').value,
    criticalYn:$('formCrit').value, remark:$('formRemark').value.trim()
  };
  if(!payload.interfaceId||!payload.fromAppId){alert('필수값 확인');return;}
  if(!payload.toAppId&&!payload.toExternalName){alert('To App 또는 External 필요');return;}
  const svc=editMode?'ifina5200U0':'ifina5200C0';
  const res=await InfraApi.postService(svc,payload,SCID); const dto=res.dto||{};
  if(dto.RSLT_CD&&dto.RSLT_CD!=='0000'){alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG||''}`);return;}
  $('editModal').hidden=true; await search();
}
async function remove(r){
  if(!confirm(`삭제?\n${r.interfaceId}`))return;
  const res=await InfraApi.postService('ifina5200D0',{interfaceIdList:[r.interfaceId]},SCID);
  const dto=res.dto||{};
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
