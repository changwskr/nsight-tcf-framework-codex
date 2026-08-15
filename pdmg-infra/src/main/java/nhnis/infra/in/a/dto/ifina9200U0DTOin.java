package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina9200U0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String gateId, targetTypeCd, targetId, resultCd, evidence, remark, checkedBy;
    public String getGateId(){return gateId;} public void setGateId(String v){gateId=v;}
    public String getTargetTypeCd(){return targetTypeCd;} public void setTargetTypeCd(String v){targetTypeCd=v;}
    public String getTargetId(){return targetId;} public void setTargetId(String v){targetId=v;}
    public String getResultCd(){return resultCd;} public void setResultCd(String v){resultCd=v;}
    public String getEvidence(){return evidence;} public void setEvidence(String v){evidence=v;}
    public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
    public String getCheckedBy(){return checkedBy;} public void setCheckedBy(String v){checkedBy=v;}
    @Override public Object clone(){ ifina9200U0DTOin c=new ifina9200U0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina9200U0DTOin in=(ifina9200U0DTOin)src;
      gateId=in.gateId; targetTypeCd=in.targetTypeCd; targetId=in.targetId; resultCd=in.resultCd;
      evidence=in.evidence; remark=in.remark; checkedBy=in.checkedBy; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("gateId", FieldProperty.builder().setPhysicalName("gateId").setLogicalName("gateId").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
