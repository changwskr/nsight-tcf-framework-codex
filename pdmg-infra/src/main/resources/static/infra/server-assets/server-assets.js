const SCID='INF-320';
let pageNo=1,totalPages=1,totalCount=0,rowsCache=[],editMode='create',detailAttrs=[];
const $=(id)=>document.getElementById(id);
function pageSize(){const n=parseInt($('pageSize').value,10);return Number.isNaN(n)||n<=0?10:Math.min(n,100);}
function updatePager(){$('pageInfo').textContent=`${pageNo} / ${Math.max(totalPages,1)} · 전체 ${totalCount}건`;$('prevPageBtn').disabled=pageNo<=1;$('nextPageBtn').disabled=pageNo>=totalPages||totalPages<=0;}
function render(rows){rowsCache=rows||[];$('resultCount').textContent=`${totalCount}건`;updatePager();
  if(!rowsCache.length){$('resultBody').innerHTML='<tr><td colspan="8" class="empty">데이터 없음</td></tr>';return;}
  $('resultBody').innerHTML=rowsCache.map((r,i)=>`<tr data-index="${i}">
    <td class="mono">${InfraApi.escapeHtml(r.assetId)}</td><td>${InfraApi.escapeHtml(r.assetName)}</td>
    <td>${InfraApi.escapeHtml(r.assetKindCd)}</td><td>${InfraApi.escapeHtml(r.techRoleCd)}</td>
    <td class="mono">${InfraApi.escapeHtml(r.systemId)}</td><td class="mono">${InfraApi.escapeHtml(r.groupId)}</td>
    <td>${InfraApi.statusBadge(r.statusCd)}</td>
    <td><button class="btn-icon" data-action="detail" type="button">상세</button>
        <button class="btn-icon" data-action="edit" type="button">수정</button>
        <button class="btn-icon" data-action="retire" type="button">폐기</button></td></tr>`).join('');
}
function renderAttrFields(attrs, editable){
  const list=attrs||[];
  if(!list.length){return '<p class="empty">확장항목 템플릿 없음 (TECH_ROLE 기준)</p>';}
  return list.map((a,i)=>{
    const req=a.requiredYn==='Y'?' *':'';
    const val=a.attrValue==null?'':String(a.attrValue);
    if(!editable){
      return `<div class="field"><label>${InfraApi.escapeHtml(a.itemName||a.itemId)}${req}</label>
        <div class="mono">${InfraApi.escapeHtml(val)||'—'}</div>
        <div class="hint">${InfraApi.escapeHtml(a.templateId)} · ${InfraApi.escapeHtml(a.itemId)}</div></div>`;
    }
    return `<div class="field"><label for="attr_${i}">${InfraApi.escapeHtml(a.itemName||a.itemId)}${req}</label>
      <input id="attr_${i}" data-item-id="${InfraApi.escapeHtml(a.itemId)}" value="${InfraApi.escapeHtml(val)}"
        placeholder="${InfraApi.escapeHtml(a.itemTypeCd||'TEXT')}"></div>`;
  }).join('');
}
function collectAttrsFromForm(){
  return Array.from(document.querySelectorAll('#editAttrs [data-item-id]')).map(el=>({
    itemId: el.getAttribute('data-item-id'),
    attrValue: el.value
  }));
}
async function search(reset){if(reset)pageNo=1;$('resultMeta').textContent='조회 중…';
  const res=await InfraApi.postService('ifina3100S0',{keyword:$('keyword').value.trim()||null,systemId:$('systemId').value.trim()||null,statusCd:$('statusCd').value||null,pageNo,pageSize:pageSize()},SCID);
  const dto=res.dto||{}; const rows=Array.isArray(dto.ifina3100S0DTOSub0)?dto.ifina3100S0DTOSub0:[];
  totalCount=dto.totalCount!=null?Number(dto.totalCount):rows.length;
  totalPages=dto.totalPages!=null?Number(dto.totalPages):Math.max(1,Math.ceil(totalCount/pageSize()));
  pageNo=dto.pageNo!=null?Number(dto.pageNo):pageNo; render(rows);
  $('resultMeta').textContent=`HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ifina3100S0`;
}
function openCreate(){editMode='create';$('editTitle').textContent='자산 등록';$('formAssetId').disabled=false;
  $('formAssetId').value='';$('formAssetName').value='';$('formAssetKindCd').value='VM';$('formTechRoleCd').value='WAS';
  $('formEnvCd').value='PROD';$('formTierCd').value='TIER2';$('formSystemId').value='SYS-ONLINE';$('formGroupId').value='SG-WAS-A';
  $('formStatusCd').value='CONFIRMED';$('formOsName').value='';$('formOsVersion').value='';$('formOsEolDate').value='';$('formRemark').value='';
  detailAttrs=[]; $('editAttrs').innerHTML='<p class="empty">저장 후 상세에서 템플릿 항목을 채울 수 있습니다. 역할 변경 시 수정 화면에서 로드됩니다.</p>';
  $('editModal').hidden=false;}
async function openEdit(r){editMode='edit';$('editTitle').textContent='자산 수정';$('formAssetId').disabled=true;
  $('formAssetId').value=r.assetId||'';$('formAssetName').value=r.assetName||'';$('formAssetKindCd').value=r.assetKindCd||'VM';
  $('formTechRoleCd').value=r.techRoleCd||'WAS';$('formEnvCd').value=r.envCd||'DEV';$('formTierCd').value=r.tierCd||'TIER3';
  $('formSystemId').value=r.systemId||'';$('formGroupId').value=r.groupId||'';$('formStatusCd').value=r.statusCd||'DISCOVERED';
  $('formOsName').value=r.osName||'';$('formOsVersion').value=r.osVersion||'';$('formOsEolDate').value=r.osEolDate||'';
  $('formRemark').value=r.remark||'';
  $('editAttrs').innerHTML='<p class="empty">확장항목 로딩…</p>';
  $('editModal').hidden=false;
  const res=await InfraApi.postService('ifina3100S1',{assetId:r.assetId},SCID);
  const dto=res.dto||{};
  const base=dto.base||r;
  if(base.osName!=null)$('formOsName').value=base.osName||'';
  if(base.osVersion!=null)$('formOsVersion').value=base.osVersion||'';
  if(base.osEolDate!=null)$('formOsEolDate').value=base.osEolDate||'';
  detailAttrs=Array.isArray(dto.attrs)?dto.attrs:[];
  $('editAttrs').innerHTML=renderAttrFields(detailAttrs,true);
}
async function save(){const payload={assetId:$('formAssetId').value.trim(),assetName:$('formAssetName').value.trim(),assetKindCd:$('formAssetKindCd').value,
  techRoleCd:$('formTechRoleCd').value,envCd:$('formEnvCd').value,tierCd:$('formTierCd').value,systemId:$('formSystemId').value.trim(),
  groupId:$('formGroupId').value.trim(),statusCd:$('formStatusCd').value,
  osName:$('formOsName').value.trim(),osVersion:$('formOsVersion').value.trim(),osEolDate:$('formOsEolDate').value.trim(),
  remark:$('formRemark').value.trim(),
  serviceModelCd:'IAAS',deployModelCd:'ON_PREMISE',attrs:editMode==='edit'?collectAttrsFromForm():[]};
  if(!payload.assetId||!payload.assetName){alert('Asset ID/명은 필수');return;}
  const res=await InfraApi.postService(editMode==='create'?'ifina3100C0':'ifina3100U0',payload,SCID); const dto=res.dto||{};
  if(dto.RSLT_CD&&dto.RSLT_CD!=='0000'){alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG||''}`);return;}
  if(dto.RSLT_MSG&&dto.RSLT_MSG!=='OK'){console.info(dto.RSLT_MSG);}
  $('editModal').hidden=true; await search(editMode==='create');}
async function retire(r){if(!confirm(`폐기(RETIRED)?\n${r.assetId}`))return;
  const res=await InfraApi.postService('ifina3100D0',{assetIdList:[r.assetId]},SCID); const dto=res.dto||{};
  if(dto.RSLT_CD&&dto.RSLT_CD!=='0000'){alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG||''}`);return;} await search(false);}
async function detail(r){const res=await InfraApi.postService('ifina3100S1',{assetId:r.assetId},SCID);
  const dto=res.dto||{};
  $('detailPanel').hidden=false;
  const base=dto.base||{};
  const attrs=Array.isArray(dto.attrs)?dto.attrs:[];
  const warn=Array.isArray(dto.warnings)?dto.warnings:[];
  $('detailSummary').innerHTML=`<div><strong>${InfraApi.escapeHtml(base.assetId||'')}</strong> ${InfraApi.escapeHtml(base.assetName||'')}
    · ${InfraApi.escapeHtml(base.techRoleCd||'')} · OS ${InfraApi.escapeHtml(base.osName||'-')} ${InfraApi.escapeHtml(base.osVersion||'')}
    · EOL ${InfraApi.escapeHtml(base.osEolDate||'-')} · EP ${dto.endpointCount||0} · MW ${dto.mwCount||0} · DB ${dto.dbCount||0} · ATTR ${dto.attrCount||0}</div>
    ${warn.length?`<div class="hint">${warn.map(w=>InfraApi.escapeHtml(w)).join('<br>')}</div>`:''}`;
  $('detailAttrs').innerHTML=renderAttrFields(attrs,false);
  $('detailBody').textContent=JSON.stringify(dto,null,2);}
$('searchBtn').onclick=()=>search(true); $('addBtn').onclick=openCreate; $('saveBtn').onclick=()=>save().catch(console.error);
$('closeDetailBtn').onclick=()=>{$('detailPanel').hidden=true;};
$('prevPageBtn').onclick=()=>{if(pageNo>1){pageNo--;search(false);}}; $('nextPageBtn').onclick=()=>{if(pageNo<totalPages){pageNo++;search(false);}};
$('keyword').onkeydown=(e)=>{if(e.key==='Enter')search(true);};
$('resultBody').onclick=(e)=>{const btn=e.target.closest('[data-action]'); if(!btn)return; const r=rowsCache[Number(btn.closest('tr').dataset.index)]; if(!r)return;
  if(btn.dataset.action==='edit')openEdit(r).catch(console.error); if(btn.dataset.action==='retire')retire(r).catch(console.error); if(btn.dataset.action==='detail')detail(r).catch(console.error);};
$('editModal').querySelectorAll('[data-close="true"]').forEach(el=>el.onclick=()=>{$('editModal').hidden=true;});
search(true).catch(console.error);
