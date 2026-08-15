const SCID='INF-150';
let selectedOrg='', orgs=[], persons=[], orgMode='create', personMode='create';
const $ = (id)=>document.getElementById(id);
function renderOrgs(){
  if(!orgs.length){$('orgList').innerHTML='<p class="empty">조직 없음</p>';return;}
  $('orgList').innerHTML=orgs.map(o=>{
    const active=o.orgId===selectedOrg?' style="background:rgba(0,0,0,.06);font-weight:600"':'';
    const indent=o.parentOrgId?'&nbsp;&nbsp;└ ':'';
    return `<div style="display:flex;justify-content:space-between;align-items:center;padding:.35rem 0;border-bottom:1px solid rgba(0,0,0,.06)">
      <button type="button" data-org="${InfraApi.escapeHtml(o.orgId)}" style="border:0;background:transparent;cursor:pointer;text-align:left;flex:1"${active}>${indent}<span class="mono">${InfraApi.escapeHtml(o.orgId)}</span> ${InfraApi.escapeHtml(o.orgName)} <small>${InfraApi.escapeHtml(o.orgTypeCd||'')}</small></button>
      <button class="btn-icon" type="button" data-edit-org="${InfraApi.escapeHtml(o.orgId)}">수정</button>
    </div>`;
  }).join('');
}
function renderPersons(){$('resultCount').textContent=`${persons.length}건`;
  if(!persons.length){$('resultBody').innerHTML='<tr><td colspan="7" class="empty">데이터 없음</td></tr>';return;}
  $('resultBody').innerHTML=persons.map((r,i)=>`<tr data-index="${i}">
    <td class="mono">${InfraApi.escapeHtml(r.personId)}</td><td>${InfraApi.escapeHtml(r.personName)}</td>
    <td class="mono">${InfraApi.escapeHtml(r.orgId)}</td><td>${InfraApi.escapeHtml(r.roleCd||'')}</td><td>${InfraApi.escapeHtml(r.email)}</td>
    <td>${InfraApi.escapeHtml(r.activeYn)}</td>
    <td><button class="btn-icon" data-action="edit" type="button">수정</button>
        <button class="btn-icon" data-action="off" type="button">비활성</button></td></tr>`).join('');
}
async function loadOrgs(){
  const res=await InfraApi.postService('ifina1500S0',{entityType:'ORG',pageNo:1,pageSize:200},SCID);
  orgs=Array.isArray(res.dto&&res.dto.ifina1500S0DTOSub0)?res.dto.ifina1500S0DTOSub0:[];
  renderOrgs();
}
async function loadPersons(){
  $('resultMeta').textContent='조회 중…';
  const res=await InfraApi.postService('ifina1500S0',{
    entityType:'PERSON', keyword:$('keyword').value.trim()||null,
    orgId:selectedOrg||null, activeYn:$('activeYn').value||null, pageNo:1, pageSize:200
  },SCID);
  persons=Array.isArray(res.dto&&res.dto.ifina1500S0DTOSub0)?res.dto.ifina1500S0DTOSub0:[];
  renderPersons();
  $('resultMeta').textContent=`HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ifina1500S0 · ${res.dto&&res.dto.totalCount||0}건`;
}
function openOrgCreate(){orgMode='create';$('orgTitle').textContent='조직 등록';$('formOrgId').disabled=false;
  $('formOrgId').value='';$('formOrgName').value='';$('formParentOrgId').value=selectedOrg||'';
  $('formOrgTypeCd').value='OPS';$('formOrgActiveYn').value='Y';$('formOrgRemark').value='';$('orgModal').hidden=false;}
function openOrgEdit(o){orgMode='edit';$('orgTitle').textContent='조직 수정';$('formOrgId').disabled=true;
  $('formOrgId').value=o.orgId||'';$('formOrgName').value=o.orgName||'';$('formParentOrgId').value=o.parentOrgId||'';
  $('formOrgTypeCd').value=o.orgTypeCd||'OPS';$('formOrgActiveYn').value=o.activeYn||'Y';$('formOrgRemark').value=o.remark||'';$('orgModal').hidden=false;}
function openPersonCreate(){personMode='create';$('personTitle').textContent='담당자 등록';$('formPersonId').disabled=false;
  $('formPersonId').value='';$('formPersonName').value='';$('formPersonOrgId').value=selectedOrg||'ORG-INFRA';
  $('formEmail').value='';$('formRoleCd').value='OPS';$('formPersonActiveYn').value='Y';$('formPersonRemark').value='';$('personModal').hidden=false;}
function openPersonEdit(r){personMode='edit';$('personTitle').textContent='담당자 수정';$('formPersonId').disabled=true;
  $('formPersonId').value=r.personId||'';$('formPersonName').value=r.personName||'';$('formPersonOrgId').value=r.orgId||'';
  $('formEmail').value=r.email||'';$('formRoleCd').value=r.roleCd||'OPS';$('formPersonActiveYn').value=r.activeYn||'Y';$('formPersonRemark').value=r.remark||'';$('personModal').hidden=false;}
async function saveOrg(){const payload={entityType:'ORG',orgId:$('formOrgId').value.trim(),orgName:$('formOrgName').value.trim(),
  parentOrgId:$('formParentOrgId').value.trim(),orgTypeCd:$('formOrgTypeCd').value,activeYn:$('formOrgActiveYn').value,remark:$('formOrgRemark').value.trim()};
  if(!payload.orgId||!payload.orgName){alert('조직 ID/명은 필수');return;}
  const res=await InfraApi.postService(orgMode==='create'?'ifina1500C0':'ifina1500U0',payload,SCID); const dto=res.dto||{};
  if(dto.RSLT_CD&&dto.RSLT_CD!=='0000'){alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG||''}`);return;}
  $('orgModal').hidden=true; await loadOrgs();}
async function savePerson(){const payload={entityType:'PERSON',personId:$('formPersonId').value.trim(),personName:$('formPersonName').value.trim(),
  orgId:$('formPersonOrgId').value.trim(),email:$('formEmail').value.trim(),roleCd:$('formRoleCd').value,activeYn:$('formPersonActiveYn').value,remark:$('formPersonRemark').value.trim()};
  if(!payload.personId||!payload.personName){alert('사번/이름은 필수');return;}
  const res=await InfraApi.postService(personMode==='create'?'ifina1500C0':'ifina1500U0',payload,SCID); const dto=res.dto||{};
  if(dto.RSLT_CD&&dto.RSLT_CD!=='0000'){alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG||''}`);return;}
  $('personModal').hidden=true; await loadPersons();}
async function deactivatePerson(r){if(!confirm(`비활성?\n${r.personId}`))return;
  const res=await InfraApi.postService('ifina1500U0',{entityType:'PERSON',personId:r.personId,personName:r.personName,orgId:r.orgId||'',email:r.email||'',roleCd:r.roleCd||'OPS',activeYn:'N',remark:r.remark||''},SCID);
  const dto=res.dto||{}; if(dto.RSLT_CD&&dto.RSLT_CD!=='0000'){alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG||''}`);return;} await loadPersons();}
$('searchBtn').onclick=()=>loadPersons(); $('addOrgBtn').onclick=openOrgCreate; $('addPersonBtn').onclick=openPersonCreate;
$('saveOrgBtn').onclick=()=>saveOrg().catch(console.error); $('savePersonBtn').onclick=()=>savePerson().catch(console.error);
$('keyword').onkeydown=(e)=>{if(e.key==='Enter')loadPersons();};
$('orgList').onclick=(e)=>{
  const edit=e.target.closest('[data-edit-org]'); if(edit){const o=orgs.find(x=>x.orgId===edit.dataset.editOrg); if(o)openOrgEdit(o); return;}
  const btn=e.target.closest('[data-org]'); if(!btn)return; selectedOrg=btn.dataset.org===selectedOrg?'':btn.dataset.org; renderOrgs(); loadPersons().catch(console.error);
};
$('resultBody').onclick=(e)=>{const btn=e.target.closest('[data-action]'); if(!btn)return; const r=persons[Number(btn.closest('tr').dataset.index)]; if(!r)return;
  if(btn.dataset.action==='edit')openPersonEdit(r); if(btn.dataset.action==='off')deactivatePerson(r).catch(console.error);};
$('orgModal').querySelectorAll('[data-close-org="true"]').forEach(el=>el.onclick=()=>{$('orgModal').hidden=true;});
$('personModal').querySelectorAll('[data-close-person="true"]').forEach(el=>el.onclick=()=>{$('personModal').hidden=true;});
(async()=>{await loadOrgs(); await loadPersons();})().catch(console.error);
