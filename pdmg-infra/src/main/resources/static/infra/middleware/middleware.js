const SCID='INF-410';
let pageNo=1,totalPages=1,totalCount=0,rowsCache=[],editMode='create';
const $ = (id)=>document.getElementById(id);
function pageSize(){const n=parseInt($('pageSize').value,10);return Number.isNaN(n)||n<=0?10:Math.min(n,100);}
function updatePager(){$('pageInfo').textContent=`${pageNo} / ${Math.max(totalPages,1)} · 전체 ${totalCount}건`;$('prevPageBtn').disabled=pageNo<=1;$('nextPageBtn').disabled=pageNo>=totalPages||totalPages<=0;}
function render(rows){rowsCache=rows||[];$('resultCount').textContent=`${totalCount}건`;updatePager();
  if(!rowsCache.length){$('resultBody').innerHTML='<tr><td colspan="7" class="empty">데이터 없음</td></tr>';return;}
  $('resultBody').innerHTML=rowsCache.map((r,i)=>`<tr data-index="${i}">
    <td class="mono">${InfraApi.escapeHtml(r.mwId)}</td><td class="mono">${InfraApi.escapeHtml(r.assetId)}</td>
    <td>${InfraApi.escapeHtml(r.productName)}</td><td>${InfraApi.escapeHtml(r.versionNo)}</td>
    <td>${InfraApi.escapeHtml(r.eolDate)}</td><td>${InfraApi.statusBadge(r.statusCd)}</td>
    <td><button class="btn-icon" data-action="edit" type="button">수정</button>
        <button class="btn-icon" data-action="delete" type="button">삭제</button></td></tr>`).join('');
}
async function search(reset){if(reset)pageNo=1;$('resultMeta').textContent='조회 중…';
  const res=await InfraApi.postService('ifina4100S0',{keyword:$('keyword').value.trim()||null,assetId:$('assetId').value.trim()||null,pageNo,pageSize:pageSize()},SCID);
  const dto=res.dto||{}; const rows=Array.isArray(dto.ifina4100S0DTOSub0)?dto.ifina4100S0DTOSub0:[];
  totalCount=dto.totalCount!=null?Number(dto.totalCount):rows.length;
  totalPages=dto.totalPages!=null?Number(dto.totalPages):Math.max(1,Math.ceil(totalCount/pageSize()));
  pageNo=dto.pageNo!=null?Number(dto.pageNo):pageNo; render(rows);
  $('resultMeta').textContent=`HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ifina4100S0`;
}
function openCreate(){editMode='create';$('editTitle').textContent='Middleware 등록';$('formMwId').disabled=false;
  $('formMwId').value='';$('formAssetId').value='INF-APP-001';$('formProductName').value='';$('formVersionNo').value='';
  $('formEolDate').value='';$('formStatusCd').value='CONFIRMED';$('formRemark').value='';$('editModal').hidden=false;}
function openEdit(r){editMode='edit';$('editTitle').textContent='Middleware 수정';$('formMwId').disabled=true;
  $('formMwId').value=r.mwId||'';$('formAssetId').value=r.assetId||'';$('formProductName').value=r.productName||'';
  $('formVersionNo').value=r.versionNo||'';$('formEolDate').value=r.eolDate||'';$('formStatusCd').value=r.statusCd||'DISCOVERED';
  $('formRemark').value=r.remark||'';$('editModal').hidden=false;}
async function save(){const payload={mwId:$('formMwId').value.trim(),assetId:$('formAssetId').value.trim(),productName:$('formProductName').value.trim(),
  versionNo:$('formVersionNo').value.trim(),eolDate:$('formEolDate').value.trim(),statusCd:$('formStatusCd').value,remark:$('formRemark').value.trim()};
  if(!payload.mwId||!payload.assetId||!payload.productName){alert('MW ID/자산/제품은 필수');return;}
  const res=await InfraApi.postService(editMode==='create'?'ifina4100C0':'ifina4100U0',payload,SCID); const dto=res.dto||{};
  if(dto.RSLT_CD&&dto.RSLT_CD!=='0000'){alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG||''}`);return;}
  $('editModal').hidden=true; await search(editMode==='create');}
async function removeRow(r){if(!confirm(`삭제?\n${r.mwId}`))return; const res=await InfraApi.postService('ifina4100D0',{mwIdList:[r.mwId]},SCID); const dto=res.dto||{};
  if(dto.RSLT_CD&&dto.RSLT_CD!=='0000'){alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG||''}`);return;} await search(false);}
$('searchBtn').onclick=()=>search(true); $('addBtn').onclick=openCreate; $('saveBtn').onclick=()=>save().catch(console.error);
$('prevPageBtn').onclick=()=>{if(pageNo>1){pageNo--;search(false);}}; $('nextPageBtn').onclick=()=>{if(pageNo<totalPages){pageNo++;search(false);}};
$('keyword').onkeydown=(e)=>{if(e.key==='Enter')search(true);};
$('resultBody').onclick=(e)=>{const btn=e.target.closest('[data-action]'); if(!btn)return; const r=rowsCache[Number(btn.closest('tr').dataset.index)]; if(!r)return;
  if(btn.dataset.action==='edit')openEdit(r); if(btn.dataset.action==='delete')removeRow(r).catch(console.error);};
$('editModal').querySelectorAll('[data-close="true"]').forEach(el=>el.onclick=()=>{$('editModal').hidden=true;});
search(true).catch(console.error);
