const SCID='INF-630';
const $=(id)=>document.getElementById(id);
const FIELDS=['securityGradeCd','personalInfoYn','creditInfoYn','financialTxnYn','adminInfoYn',
  'externalConnYn','internetConnYn','encryptionYn','kmsHsmYn','pamYn','edrYn','auditLogYn','authMethodCd','networkZoneCd','remark'];
function setVal(id,v){$(id).value=v==null?'':v;}
async function search(){
  $('resultMeta').textContent='조회 중…';
  const res=await InfraApi.postService('ifina6300S0',{
    targetTypeCd:$('targetTypeCd').value, targetId:$('targetId').value.trim()||null
  },SCID);
  const dto=res.dto||{};
  FIELDS.forEach(f=>setVal(f, dto[f]??(f.endsWith('Yn')?'N':'')));
  if(dto.securityGradeCd) setVal('securityGradeCd', dto.securityGradeCd);
  $('warnLine').textContent=(dto.warnings||[]).join(' · ')||'';
  $('resultMeta').textContent=`HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ifina6300S0 · ${dto.RSLT_CD||''}`;
}
async function save(){
  const payload={targetTypeCd:$('targetTypeCd').value, targetId:$('targetId').value.trim()};
  FIELDS.forEach(f=>payload[f]=$(f).value.trim());
  if(!payload.targetId){alert('대상ID 필수');return;}
  const res=await InfraApi.postService('ifina6300U0',payload,SCID); const dto=res.dto||{};
  if(dto.RSLT_CD&&dto.RSLT_CD!=='0000'){alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG||''}`);return;}
  if(dto.warnings&&dto.warnings.length) alert(dto.warnings.join('\n'));
  await search();
}
$('searchBtn').onclick=()=>search().catch(console.error);
$('saveBtn').onclick=()=>save().catch(console.error);
search().catch(console.error);
