const SCID='INF-110';
let selectedSet='', rowsCache=[], editMode='create', codeSets=[];
const $ = (id)=>document.getElementById(id);
function renderSets(){
  if(!codeSets.length){$('codeSetList').innerHTML='<p class="empty">코드셋 없음</p>';return;}
  $('codeSetList').innerHTML=codeSets.map(s=>{
    const id=s.codeSetId||s.CODE_SET_ID; const name=s.codeSetName||s.CODE_SET_NAME||id;
    const active=id===selectedSet?' style="background:rgba(0,0,0,.06);font-weight:600"':'';
    return `<button type="button" class="btn-icon" data-set="${InfraApi.escapeHtml(id)}" style="display:block;width:100%;text-align:left;padding:.5rem;border:0;background:transparent;cursor:pointer"${active}>${InfraApi.escapeHtml(id)}<br><small>${InfraApi.escapeHtml(name)}</small></button>`;
  }).join('');
}
function render(rows){rowsCache=rows||[];$('resultCount').textContent=`${rowsCache.length}건`;
  $('valueTitle').textContent=selectedSet?`코드값 · ${selectedSet}`:'코드값';
  if(!rowsCache.length){$('resultBody').innerHTML='<tr><td colspan="5" class="empty">데이터 없음</td></tr>';return;}
  $('resultBody').innerHTML=rowsCache.map((r,i)=>`<tr data-index="${i}">
    <td class="mono">${InfraApi.escapeHtml(r.codeValue)}</td><td>${InfraApi.escapeHtml(r.nameKo)}</td>
    <td>${InfraApi.escapeHtml(r.sortOrder)}</td><td>${InfraApi.escapeHtml(r.activeYn)}</td>
    <td><button class="btn-icon" data-action="edit" type="button">수정</button>
        <button class="btn-icon" data-action="off" type="button">비활성</button></td></tr>`).join('');
}
async function search(){
  $('resultMeta').textContent='조회 중…';
  const res=await InfraApi.postService('ifina1100S0',{
    codeSetId:selectedSet||null, keyword:$('keyword').value.trim()||null,
    activeYn:$('activeYn').value||null, pageNo:1, pageSize:200
  },SCID);
  const dto=res.dto||{};
  codeSets=Array.isArray(dto.codeSets)?dto.codeSets:[];
  renderSets();
  const rows=Array.isArray(dto.ifina1100S0DTOSub0)?dto.ifina1100S0DTOSub0:[];
  render(rows);
  $('resultMeta').textContent=`HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ifina1100S0 · 전체 ${dto.totalCount||0}건`;
}
function openCreate(){if(!selectedSet){alert('코드셋을 먼저 선택하세요');return;}
  editMode='create';$('editTitle').textContent='코드값 등록';$('formCodeValue').disabled=false;
  $('formCodeSetId').value=selectedSet;$('formCodeValue').value='';$('formNameKo').value='';
  $('formSortOrder').value='0';$('formActiveYn').value='Y';$('formRemark').value='';$('editModal').hidden=false;}
function openEdit(r){editMode='edit';$('editTitle').textContent='코드값 수정';$('formCodeValue').disabled=true;
  $('formCodeSetId').value=r.codeSetId||selectedSet;$('formCodeValue').value=r.codeValue||'';
  $('formNameKo').value=r.nameKo||'';$('formSortOrder').value=r.sortOrder!=null?r.sortOrder:0;
  $('formActiveYn').value=r.activeYn||'Y';$('formRemark').value=r.remark||'';$('editModal').hidden=false;}
async function save(){const payload={codeSetId:$('formCodeSetId').value.trim(),codeValue:$('formCodeValue').value.trim(),
  nameKo:$('formNameKo').value.trim(),sortOrder:parseInt($('formSortOrder').value,10)||0,
  activeYn:$('formActiveYn').value,remark:$('formRemark').value.trim()};
  if(!payload.codeSetId||!payload.codeValue||!payload.nameKo){alert('코드셋/코드값/표시명은 필수');return;}
  const res=await InfraApi.postService(editMode==='create'?'ifina1100C0':'ifina1100U0',payload,SCID); const dto=res.dto||{};
  if(dto.RSLT_CD&&dto.RSLT_CD!=='0000'){alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG||''}`);return;}
  $('editModal').hidden=true; await search();}
async function deactivate(r){if(!confirm(`비활성?\n${r.codeSetId}.${r.codeValue}`))return;
  const res=await InfraApi.postService('ifina1100U0',{codeSetId:r.codeSetId,codeValue:r.codeValue,nameKo:r.nameKo,sortOrder:r.sortOrder||0,activeYn:'N',remark:r.remark||''},SCID);
  const dto=res.dto||{}; if(dto.RSLT_CD&&dto.RSLT_CD!=='0000'){alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG||''}`);return;} await search();}
$('searchBtn').onclick=()=>search(); $('addBtn').onclick=openCreate; $('saveBtn').onclick=()=>save().catch(console.error);
$('keyword').onkeydown=(e)=>{if(e.key==='Enter')search();};
$('codeSetList').onclick=(e)=>{const btn=e.target.closest('[data-set]'); if(!btn)return; selectedSet=btn.dataset.set; search().catch(console.error);};
$('resultBody').onclick=(e)=>{const btn=e.target.closest('[data-action]'); if(!btn)return; const r=rowsCache[Number(btn.closest('tr').dataset.index)]; if(!r)return;
  if(btn.dataset.action==='edit')openEdit(r); if(btn.dataset.action==='off')deactivate(r).catch(console.error);};
$('editModal').querySelectorAll('[data-close="true"]').forEach(el=>el.onclick=()=>{$('editModal').hidden=true;});
search().catch(console.error);
