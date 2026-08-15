const SCID='INF-130';
const $=(id)=>document.getElementById(id);
let rowsCache=[]; let editMode=false;
async function search(){
  const res=await InfraApi.postService('ifina1300S0',{
    keyword:$('keyword').value.trim()||null, activeYn:$('activeYn').value||null, pageNo:1, pageSize:100
  },SCID);
  const dto=res.dto||{}; rowsCache=Array.isArray(dto.rows)?dto.rows:[];
  $('rowCount').textContent=`${dto.totalCount??rowsCache.length}`;
  $('rowBody').innerHTML=rowsCache.length?rowsCache.map((r,i)=>`<tr data-i="${i}">
    <td class="mono">${InfraApi.escapeHtml(r.checklistId||'')}</td>
    <td>${InfraApi.escapeHtml(r.itemName||'')}</td>
    <td>${InfraApi.escapeHtml(r.categoryKo||'')}</td>
    <td>${InfraApi.escapeHtml(r.severityCd||'')}</td>
    <td>${r.sortNo??''}</td><td>${InfraApi.escapeHtml(r.activeYn||'')}</td>
    <td><button class="btn-icon" data-a="edit" type="button">수정</button>
        <button class="btn-icon" data-a="off" type="button">비활성</button></td></tr>`).join('')
    :'<tr><td colspan="7" class="empty">없음</td></tr>';
  $('resultMeta').textContent=`HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ifina1300S0`;
}
function openCreate(){
  editMode=false; $('modalTitle').textContent='Checklist 등록';
  $('formId').value='CL-'+Date.now().toString().slice(-6); $('formId').readOnly=false;
  $('formName').value=''; $('formCat').value='서버'; $('formSev').value='P2';
  $('formSort').value='10'; $('formActive').value='Y'; $('formRemark').value='';
  $('editModal').hidden=false;
}
function openEdit(r){
  editMode=true; $('modalTitle').textContent='Checklist 수정';
  $('formId').value=r.checklistId||''; $('formId').readOnly=true;
  $('formName').value=r.itemName||''; $('formCat').value=r.categoryKo||'';
  $('formSev').value=r.severityCd||'P2'; $('formSort').value=r.sortNo??10;
  $('formActive').value=r.activeYn||'Y'; $('formRemark').value=r.remark||'';
  $('editModal').hidden=false;
}
async function save(payload){
  const body=payload||{
    checklistId:$('formId').value.trim(), itemName:$('formName').value.trim(),
    categoryKo:$('formCat').value.trim(), severityCd:$('formSev').value,
    sortNo:Number($('formSort').value)||99, activeYn:$('formActive').value, remark:$('formRemark').value.trim()
  };
  if(!body.checklistId||!body.itemName){alert('필수값');return;}
  const svc=editMode||payload?'ifina1300U0':'ifina1300C0';
  const res=await InfraApi.postService(svc,body,SCID); const dto=res.dto||{};
  if(dto.RSLT_CD&&dto.RSLT_CD!=='0000'){alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG||''}`);return;}
  $('editModal').hidden=true; await search();
}
$('searchBtn').onclick=()=>search().catch(console.error);
$('addBtn').onclick=openCreate;
$('saveBtn').onclick=()=>save().catch(console.error);
$('rowBody').onclick=(ev)=>{
  const btn=ev.target.closest('[data-a]'); if(!btn)return;
  const r=rowsCache[Number(btn.closest('tr').dataset.i)]; if(!r)return;
  if(btn.dataset.a==='edit'){editMode=true; openEdit(r);}
  if(btn.dataset.a==='off'){
    editMode=true;
    save({...r, activeYn:'N'}).catch(console.error);
  }
};
$('editModal').querySelectorAll('[data-close="true"]').forEach(el=>el.onclick=()=>{$('editModal').hidden=true;});
search().catch(console.error);
