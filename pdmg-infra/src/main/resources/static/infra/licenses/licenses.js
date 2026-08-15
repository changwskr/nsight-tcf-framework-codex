const SCID='INF-710';
const $=(id)=>document.getElementById(id);
let rowsCache=[]; let editMode=false; let selectedId=null;
function num(id){const v=$(id).value; return v===''?null:Number(v);}
async function search(licenseId){
  $('resultMeta').textContent='조회 중…';
  const res=await InfraApi.postService('ifina7100S0',{
    keyword:$('keyword').value.trim()||null, licenseId:licenseId||null, pageNo:1, pageSize:100
  },SCID);
  const dto=res.dto||{};
  rowsCache=Array.isArray(dto.rows)?dto.rows:[];
  $('warnLine').textContent=(dto.warnings||[]).slice(0,2).join(' · ')||`${dto.totalCount??0}건`;
  $('rowBody').innerHTML=rowsCache.length?rowsCache.map((r,i)=>`<tr data-index="${i}">
    <td class="mono"><button class="btn-icon" data-action="pick" type="button">${InfraApi.escapeHtml(r.licenseId)}</button></td>
    <td>${InfraApi.escapeHtml(r.productName||'')}</td>
    <td>${InfraApi.escapeHtml(r.vendorName||'')}</td>
    <td>${InfraApi.escapeHtml(r.licenseModelCd||'')}</td>
    <td>${r.qty??''}</td><td>${r.allocatedQty??0}</td>
    <td class="mono">${InfraApi.escapeHtml(r.contractEndDt||'')}</td>
    <td><button class="btn-icon" data-action="edit" type="button">수정</button>
        <button class="btn-icon" data-action="del" type="button">삭제</button></td></tr>`).join('')
    :'<tr><td colspan="8" class="empty">없음</td></tr>';
  const allocs=Array.isArray(dto.allocations)?dto.allocations:[];
  $('allocTitle').textContent=licenseId||'선택 시 표시';
  $('allocBody').innerHTML=allocs.length?allocs.map(a=>`<tr>
    <td class="mono">${InfraApi.escapeHtml(a.assetId)}</td>
    <td class="mono">${InfraApi.escapeHtml(a.licenseId)}</td>
    <td>${a.allocatedQty??''}</td></tr>`).join('')
    :'<tr><td colspan="3" class="empty">할당 없음</td></tr>';
  $('resultMeta').textContent=`HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ifina7100S0`;
}
function openCreate(){
  editMode=false; $('modalTitle').textContent='라이선스 등록';
  $('formId').value='LIC-'+Date.now().toString().slice(-6); $('formId').readOnly=false;
  $('formProduct').value=''; $('formVendor').value=''; $('formModel').value='CORE';
  $('formQty').value=''; $('formMaint').value=''; $('formEnd').value='';
  $('formMobility').value='N'; $('formRemark').value='';
  $('editModal').hidden=false;
}
function openEdit(r){
  editMode=true; $('modalTitle').textContent='라이선스 수정';
  $('formId').value=r.licenseId; $('formId').readOnly=true;
  $('formProduct').value=r.productName||''; $('formVendor').value=r.vendorName||'';
  $('formModel').value=r.licenseModelCd||'CORE'; $('formQty').value=r.qty??'';
  $('formMaint').value=r.annualMaintAmt??''; $('formEnd').value=r.contractEndDt||'';
  $('formMobility').value=r.mobilityYn||'N'; $('formRemark').value=r.remark||'';
  $('editModal').hidden=false;
}
async function save(){
  const payload={
    licenseId:$('formId').value.trim(), productName:$('formProduct').value.trim(),
    vendorName:$('formVendor').value.trim(), licenseModelCd:$('formModel').value,
    qty:num('formQty'), annualMaintAmt:num('formMaint'), contractEndDt:$('formEnd').value.trim(),
    mobilityYn:$('formMobility').value, remark:$('formRemark').value.trim(), currencyCd:'KRW'
  };
  if(!payload.licenseId||!payload.productName){alert('필수값 확인');return;}
  const svc=editMode?'ifina7100U0':'ifina7100C0';
  const res=await InfraApi.postService(svc,payload,SCID); const dto=res.dto||{};
  if(dto.RSLT_CD&&dto.RSLT_CD!=='0000'){alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG||''}`);return;}
  if(dto.warnings&&dto.warnings.length) alert(dto.warnings.join('\n'));
  $('editModal').hidden=true; await search(selectedId);
}
async function remove(r){
  if(!confirm(`삭제?\n${r.licenseId}`))return;
  const res=await InfraApi.postService('ifina7100D0',{licenseIdList:[r.licenseId]},SCID); const dto=res.dto||{};
  if(dto.RSLT_CD&&dto.RSLT_CD!=='0000'){alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG||''}`);return;}
  selectedId=null; await search();
}
$('searchBtn').onclick=()=>search().catch(console.error);
$('addBtn').onclick=openCreate;
$('saveBtn').onclick=()=>save().catch(console.error);
$('rowBody').onclick=(ev)=>{
  const btn=ev.target.closest('[data-action]'); if(!btn)return;
  const r=rowsCache[Number(btn.closest('tr').dataset.index)]; if(!r)return;
  if(btn.dataset.action==='pick'){selectedId=r.licenseId; search(selectedId).catch(console.error);}
  if(btn.dataset.action==='edit') openEdit(r);
  if(btn.dataset.action==='del') remove(r).catch(console.error);
};
$('editModal').querySelectorAll('[data-close="true"]').forEach(el=>el.onclick=()=>{$('editModal').hidden=true;});
search().catch(console.error);
