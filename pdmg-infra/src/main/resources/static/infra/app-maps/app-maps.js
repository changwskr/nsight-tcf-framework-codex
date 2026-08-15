const SCID='INF-230';
const $=(id)=>document.getElementById(id);
let rowsCache=[];
async function search(){
  $('resultMeta').textContent='조회 중…';
  const res=await InfraApi.postService('ifina2300S0',{
    appId:$('appId').value.trim()||null,
    mapTypeCd:$('mapTypeCd').value||null,
    keyword:$('keyword').value.trim()||null,
    pageNo:1, pageSize:100
  },SCID);
  const dto=res.dto||{};
  rowsCache=Array.isArray(dto.rows)?dto.rows:[];
  $('rowCount').textContent=`${dto.totalCount??rowsCache.length}`;
  $('rowBody').innerHTML=rowsCache.length?rowsCache.map((r,i)=>`<tr data-index="${i}">
    <td class="mono">${InfraApi.escapeHtml(r.mapId)}</td>
    <td class="mono">${InfraApi.escapeHtml(r.appId)}</td>
    <td>${InfraApi.escapeHtml(r.mapTypeCd)}</td>
    <td class="mono">${InfraApi.escapeHtml(r.refId)}</td>
    <td>${InfraApi.escapeHtml(r.roleCd||'')}</td>
    <td>${InfraApi.escapeHtml(r.remark||'')}</td>
    <td><button class="btn-icon" data-action="del" type="button">삭제</button></td></tr>`).join('')
    :'<tr><td colspan="7" class="empty">없음</td></tr>';
  $('resultMeta').textContent=`HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ifina2300S0 · total ${dto.totalCount??0}`;
}
function openCreate(){
  $('formMapId').value='AM-'+Date.now().toString().slice(-6);
  $('formAppId').value=$('appId').value.trim()||'APP-ONLINE-A';
  $('formMapType').value='GROUP'; $('formRefId').value='';
  $('formRole').value='PRIMARY'; $('formRemark').value='';
  $('editModal').hidden=false;
}
async function save(){
  const payload={
    mapId:$('formMapId').value.trim(), appId:$('formAppId').value.trim(),
    mapTypeCd:$('formMapType').value, refId:$('formRefId').value.trim(),
    roleCd:$('formRole').value||null, remark:$('formRemark').value.trim()
  };
  if(!payload.mapId||!payload.appId||!payload.refId){alert('필수값 확인');return;}
  const res=await InfraApi.postService('ifina2300C0',payload,SCID); const dto=res.dto||{};
  if(dto.RSLT_CD&&dto.RSLT_CD!=='0000'){alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG||''}`);return;}
  $('editModal').hidden=true; await search();
}
async function remove(r){
  if(!confirm(`삭제?\n${r.mapId}`))return;
  const res=await InfraApi.postService('ifina2300D0',{mapIdList:[r.mapId]},SCID); const dto=res.dto||{};
  if(dto.RSLT_CD&&dto.RSLT_CD!=='0000'){alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG||''}`);return;}
  await search();
}
$('searchBtn').onclick=()=>search().catch(console.error);
$('addBtn').onclick=openCreate;
$('saveBtn').onclick=()=>save().catch(console.error);
$('rowBody').onclick=(ev)=>{const btn=ev.target.closest('[data-action="del"]'); if(!btn)return;
  const r=rowsCache[Number(btn.closest('tr').dataset.index)]; if(r) remove(r).catch(console.error);};
$('editModal').querySelectorAll('[data-close="true"]').forEach(el=>el.onclick=()=>{$('editModal').hidden=true;});
search().catch(console.error);
