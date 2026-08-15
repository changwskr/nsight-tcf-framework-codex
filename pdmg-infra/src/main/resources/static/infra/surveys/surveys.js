const SCID='INF-120';
let selectedTmpl='', templates=[], items=[], tmplMode='create', itemMode='create';
const $=(id)=>document.getElementById(id);
function renderTmpls(){
  if(!templates.length){$('tmplList').innerHTML='<p class="empty">템플릿 없음</p>';return;}
  $('tmplList').innerHTML=templates.map(t=>{
    const id=t.templateId; const active=id===selectedTmpl?' style="background:rgba(0,0,0,.06);font-weight:600"':'';
    return `<div style="display:flex;justify-content:space-between;gap:.4rem;padding:.4rem 0;border-bottom:1px solid rgba(0,0,0,.06)">
      <button type="button" data-tmpl="${InfraApi.escapeHtml(id)}" style="border:0;background:transparent;cursor:pointer;text-align:left;flex:1"${active}>
        <span class="mono">${InfraApi.escapeHtml(id)}</span><br><small>${InfraApi.escapeHtml(t.templateName||'')} · ${InfraApi.escapeHtml(t.techRoleCd||'-')}</small>
      </button>
      <button class="btn-icon" type="button" data-edit-tmpl="${InfraApi.escapeHtml(id)}">수정</button>
    </div>`;
  }).join('');
}
function renderItems(){$('resultCount').textContent=`${items.length}건`;
  $('itemTitle').textContent=selectedTmpl?`조사 항목 · ${selectedTmpl}`:'조사 항목';
  if(!items.length){$('resultBody').innerHTML='<tr><td colspan="7" class="empty">데이터 없음</td></tr>';return;}
  $('resultBody').innerHTML=items.map((r,i)=>`<tr data-index="${i}">
    <td class="mono">${InfraApi.escapeHtml(r.itemId)}</td><td>${InfraApi.escapeHtml(r.itemName)}</td>
    <td>${InfraApi.escapeHtml(r.itemTypeCd)}</td><td>${InfraApi.escapeHtml(r.requiredYn)}</td>
    <td>${InfraApi.escapeHtml(r.sortNo)}</td><td>${InfraApi.escapeHtml(r.activeYn)}</td>
    <td><button class="btn-icon" data-action="edit" type="button">수정</button>
        <button class="btn-icon" data-action="delete" type="button">삭제</button></td></tr>`).join('');
}
async function load(){
  $('resultMeta').textContent='조회 중…';
  const res=await InfraApi.postService('ifina1200S0',{
    entityType:'ITEM', templateId:selectedTmpl||null, keyword:$('keyword').value.trim()||null, pageNo:1, pageSize:200
  },SCID);
  const dto=res.dto||{};
  templates=Array.isArray(dto.templates)?dto.templates:[];
  items=Array.isArray(dto.ifina1200S0DTOSub0)?dto.ifina1200S0DTOSub0:[];
  renderTmpls(); renderItems();
  $('resultMeta').textContent=`HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ifina1200S0 · ${dto.totalCount||0}건`;
}
function openTmplCreate(){tmplMode='create';$('tmplTitle').textContent='템플릿 등록';$('formTemplateId').disabled=false;
  $('formTemplateId').value='';$('formTemplateName').value='';$('formTechRoleCd').value='';$('formTmplActiveYn').value='Y';$('formTmplRemark').value='';$('tmplModal').hidden=false;}
function openTmplEdit(t){tmplMode='edit';$('tmplTitle').textContent='템플릿 수정';$('formTemplateId').disabled=true;
  $('formTemplateId').value=t.templateId||'';$('formTemplateName').value=t.templateName||'';
  $('formTechRoleCd').value=t.techRoleCd||'';$('formTmplActiveYn').value=t.activeYn||'Y';$('formTmplRemark').value=t.remark||'';$('tmplModal').hidden=false;}
function openItemCreate(){if(!selectedTmpl){alert('템플릿을 먼저 선택하세요');return;}
  itemMode='create';$('itemEditTitle').textContent='항목 등록';$('formItemId').disabled=false;
  $('formItemTemplateId').value=selectedTmpl;$('formItemId').value='';$('formItemName').value='';
  $('formItemTypeCd').value='TEXT';$('formRequiredYn').value='N';$('formSortNo').value='0';
  $('formItemActiveYn').value='Y';$('formItemRemark').value='';$('itemModal').hidden=false;}
function openItemEdit(r){itemMode='edit';$('itemEditTitle').textContent='항목 수정';$('formItemId').disabled=true;
  $('formItemTemplateId').value=r.templateId||selectedTmpl;$('formItemId').value=r.itemId||'';
  $('formItemName').value=r.itemName||'';$('formItemTypeCd').value=r.itemTypeCd||'TEXT';
  $('formRequiredYn').value=r.requiredYn||'N';$('formSortNo').value=r.sortNo!=null?r.sortNo:0;
  $('formItemActiveYn').value=r.activeYn||'Y';$('formItemRemark').value=r.remark||'';$('itemModal').hidden=false;}
async function saveTmpl(){const payload={entityType:'TEMPLATE',templateId:$('formTemplateId').value.trim(),templateName:$('formTemplateName').value.trim(),
  techRoleCd:$('formTechRoleCd').value.trim(),activeYn:$('formTmplActiveYn').value,remark:$('formTmplRemark').value.trim()};
  if(!payload.templateId||!payload.templateName){alert('템플릿 ID/명은 필수');return;}
  const res=await InfraApi.postService(tmplMode==='create'?'ifina1200C0':'ifina1200U0',payload,SCID); const dto=res.dto||{};
  if(dto.RSLT_CD&&dto.RSLT_CD!=='0000'){alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG||''}`);return;}
  $('tmplModal').hidden=true; selectedTmpl=payload.templateId; await load();}
async function saveItem(){const payload={entityType:'ITEM',templateId:$('formItemTemplateId').value.trim(),itemId:$('formItemId').value.trim(),
  itemName:$('formItemName').value.trim(),itemTypeCd:$('formItemTypeCd').value,requiredYn:$('formRequiredYn').value,
  sortNo:parseInt($('formSortNo').value,10)||0,activeYn:$('formItemActiveYn').value,remark:$('formItemRemark').value.trim()};
  if(!payload.templateId||!payload.itemId||!payload.itemName){alert('템플릿/Item ID/항목명은 필수');return;}
  const res=await InfraApi.postService(itemMode==='create'?'ifina1200C0':'ifina1200U0',payload,SCID); const dto=res.dto||{};
  if(dto.RSLT_CD&&dto.RSLT_CD!=='0000'){alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG||''}`);return;}
  $('itemModal').hidden=true; await load();}
async function removeItem(r){if(!confirm(`삭제?\n${r.templateId}.${r.itemId}`))return;
  const res=await InfraApi.postService('ifina1200D0',{entityType:'ITEM',templateId:r.templateId,itemIdList:[r.itemId]},SCID); const dto=res.dto||{};
  if(dto.RSLT_CD&&dto.RSLT_CD!=='0000'){alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG||''}`);return;} await load();}
$('searchBtn').onclick=()=>load(); $('addTmplBtn').onclick=openTmplCreate; $('addItemBtn').onclick=openItemCreate;
$('saveTmplBtn').onclick=()=>saveTmpl().catch(console.error); $('saveItemBtn').onclick=()=>saveItem().catch(console.error);
$('keyword').onkeydown=(e)=>{if(e.key==='Enter')load();};
$('tmplList').onclick=(e)=>{
  const edit=e.target.closest('[data-edit-tmpl]'); if(edit){const t=templates.find(x=>x.templateId===edit.dataset.editTmpl); if(t)openTmplEdit(t); return;}
  const btn=e.target.closest('[data-tmpl]'); if(!btn)return; selectedTmpl=btn.dataset.tmpl; load().catch(console.error);
};
$('resultBody').onclick=(e)=>{const btn=e.target.closest('[data-action]'); if(!btn)return; const r=items[Number(btn.closest('tr').dataset.index)]; if(!r)return;
  if(btn.dataset.action==='edit')openItemEdit(r); if(btn.dataset.action==='delete')removeItem(r).catch(console.error);};
$('tmplModal').querySelectorAll('[data-close-tmpl="true"]').forEach(el=>el.onclick=()=>{$('tmplModal').hidden=true;});
$('itemModal').querySelectorAll('[data-close-item="true"]').forEach(el=>el.onclick=()=>{$('itemModal').hidden=true;});
load().catch(console.error);
