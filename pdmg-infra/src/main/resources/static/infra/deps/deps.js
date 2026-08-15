const SCID='INF-530';
const $=(id)=>document.getElementById(id);
let edgesCache=[];
async function search(){
  $('resultMeta').textContent='조회 중…';
  const res=await InfraApi.postService('ifina5300S0',{
    rootType:$('rootType').value, rootId:$('rootId').value.trim()||null,
    depth:parseInt($('depth').value,10)||2, criticalYn:$('criticalYn').value||null,
    pageNo:1, pageSize:100
  },SCID);
  const dto=res.dto||{};
  const nodes=Array.isArray(dto.nodes)?dto.nodes:[];
  edgesCache=Array.isArray(dto.edges)?dto.edges:[];
  $('nodeCount').textContent=`${nodes.length}`;
  $('edgeCount').textContent=`${edgesCache.length}`;
  $('nodeBody').innerHTML=nodes.length?nodes.map(n=>`<tr><td>${InfraApi.escapeHtml(n.nodeType)}</td><td class="mono">${InfraApi.escapeHtml(n.nodeId)}</td></tr>`).join('')
    :'<tr><td colspan="2" class="empty">없음</td></tr>';
  $('edgeBody').innerHTML=edgesCache.length?edgesCache.map((e,i)=>`<tr data-index="${i}">
    <td class="mono">${InfraApi.escapeHtml(e.fromTypeCd)}:${InfraApi.escapeHtml(e.fromId)}</td>
    <td>${InfraApi.escapeHtml(e.relationTypeCd)}</td>
    <td class="mono">${InfraApi.escapeHtml(e.toTypeCd)}:${InfraApi.escapeHtml(e.toId)}</td>
    <td>${e.criticalYn==='Y'?'<span class="badge badge--fail">Y</span>':'N'}</td>
    <td><button class="btn-icon" data-action="del" type="button">삭제</button></td></tr>`).join('')
    :'<tr><td colspan="5" class="empty">없음</td></tr>';
  $('resultMeta').textContent=`HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ifina5300S0 · depth ${dto.depth||'-'}`;
}
function openCreate(){
  $('formRelationId').value='REL-'+Date.now().toString().slice(-6);
  $('formFromType').value='APP'; $('formFromId').value='';
  $('formToType').value='ASSET'; $('formToId').value='';
  $('formRelType').value='CALLS'; $('formCritical').value='N'; $('formRemark').value='';
  $('editModal').hidden=false;
}
async function save(){
  const payload={relationId:$('formRelationId').value.trim(), fromTypeCd:$('formFromType').value,
    fromId:$('formFromId').value.trim(), toTypeCd:$('formToType').value, toId:$('formToId').value.trim(),
    relationTypeCd:$('formRelType').value, criticalYn:$('formCritical').value, remark:$('formRemark').value.trim()};
  if(!payload.relationId||!payload.fromId||!payload.toId){alert('필수값 확인');return;}
  const res=await InfraApi.postService('ifina5300C0',payload,SCID); const dto=res.dto||{};
  if(dto.RSLT_CD&&dto.RSLT_CD!=='0000'){alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG||''}`);return;}
  $('editModal').hidden=true; await search();
}
async function remove(e){
  if(!confirm(`삭제?\n${e.relationId}`))return;
  const res=await InfraApi.postService('ifina5300D0',{relationIdList:[e.relationId]},SCID); const dto=res.dto||{};
  if(dto.RSLT_CD&&dto.RSLT_CD!=='0000'){alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG||''}`);return;}
  await search();
}
$('searchBtn').onclick=()=>search().catch(console.error);
$('addBtn').onclick=openCreate;
$('saveBtn').onclick=()=>save().catch(console.error);
$('edgeBody').onclick=(ev)=>{const btn=ev.target.closest('[data-action="del"]'); if(!btn)return;
  const e=edgesCache[Number(btn.closest('tr').dataset.index)]; if(e) remove(e).catch(console.error);};
$('editModal').querySelectorAll('[data-close="true"]').forEach(el=>el.onclick=()=>{$('editModal').hidden=true;});
search().catch(console.error);
