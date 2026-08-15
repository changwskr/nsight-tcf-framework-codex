const SCID='INF-730';
const $=(id)=>document.getElementById(id);
function fmt(n){ if(n==null||n==='') return ''; return Number(n).toLocaleString('ko-KR'); }

async function search(){
  $('resultMeta').textContent='조회 중…';
  const res=await InfraApi.postService('ifina7300S0',{
    targetTypeCd:'SYSTEM',
    targetId:$('targetId').value.trim()||'SYS-ONLINE',
    periodYm:$('periodYm').value.trim()||'202608',
    years:Number($('years').value)||5
  },SCID);
  const dto=res.dto||{};
  $('sumTag').textContent=`${dto.years||5}Y · ${dto.periodYm||''}`;
  $('sumBody').innerHTML=`<tr>
    <td>${fmt(dto.asisAnnual)}</td><td>${fmt(dto.tobeAnnual)}</td><td>${fmt(dto.migrationOnce)}</td>
    <td>${fmt(dto.asisTco)}</td><td>${fmt(dto.tobeTco)}</td><td>${fmt(dto.deltaTco)}</td></tr>`;
  const rows=Array.isArray(dto.rows)?dto.rows:[];
  $('rowBody').innerHTML=rows.length?rows.map(r=>`<tr>
    <td class="mono">${InfraApi.escapeHtml(r.costId||'')}</td>
    <td>${InfraApi.escapeHtml(r.scenarioCd||'')}</td>
    <td>${InfraApi.escapeHtml(r.costTypeCd||'')}</td>
    <td>${fmt(r.amount)}</td>
    <td>${InfraApi.escapeHtml(r.remark||'')}</td></tr>`).join('')
    :'<tr><td colspan="5" class="empty">없음</td></tr>';
  $('resultMeta').textContent=`HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ifina7300S0`;
}

function openCreate(){
  $('formId').value='CST-'+Date.now().toString().slice(-8);
  $('formScenario').value='ASIS'; $('formType').value='OPS';
  $('formAmount').value=''; $('formRemark').value='';
  $('editModal').hidden=false;
}

async function save(){
  const payload={
    costId:$('formId').value.trim(),
    targetTypeCd:'SYSTEM',
    targetId:$('targetId').value.trim()||'SYS-ONLINE',
    periodYm:$('periodYm').value.trim()||'202608',
    scenarioCd:$('formScenario').value,
    costTypeCd:$('formType').value,
    amount:Number($('formAmount').value),
    currencyCd:'KRW',
    remark:$('formRemark').value.trim()
  };
  if(!payload.costId||!payload.amount&&payload.amount!==0){alert('필수값 확인');return;}
  const res=await InfraApi.postService('ifina7300C0',payload,SCID);
  const dto=res.dto||{};
  if(dto.RSLT_CD&&dto.RSLT_CD!=='0000'){alert(`${dto.RSLT_CD}: ${dto.RSLT_MSG||''}`);return;}
  $('editModal').hidden=true;
  await search();
}

$('searchBtn').onclick=()=>search().catch(console.error);
$('addBtn').onclick=openCreate;
$('saveBtn').onclick=()=>save().catch(console.error);
$('editModal').querySelectorAll('[data-close="true"]').forEach(el=>el.onclick=()=>{$('editModal').hidden=true;});
search().catch(console.error);
