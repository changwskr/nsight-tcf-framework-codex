package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina8100C0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String planId, waveId, targetTypeCd, targetId, strategy7rCd, currentPlatformCd, targetPlatformCd, difficultyCd, statusCd, remark;
    public String getPlanId(){return planId;} public void setPlanId(String v){planId=v;}
    public String getWaveId(){return waveId;} public void setWaveId(String v){waveId=v;}
    public String getTargetTypeCd(){return targetTypeCd;} public void setTargetTypeCd(String v){targetTypeCd=v;}
    public String getTargetId(){return targetId;} public void setTargetId(String v){targetId=v;}
    public String getStrategy7rCd(){return strategy7rCd;} public void setStrategy7rCd(String v){strategy7rCd=v;}
    public String getCurrentPlatformCd(){return currentPlatformCd;} public void setCurrentPlatformCd(String v){currentPlatformCd=v;}
    public String getTargetPlatformCd(){return targetPlatformCd;} public void setTargetPlatformCd(String v){targetPlatformCd=v;}
    public String getDifficultyCd(){return difficultyCd;} public void setDifficultyCd(String v){difficultyCd=v;}
    public String getStatusCd(){return statusCd;} public void setStatusCd(String v){statusCd=v;}
    public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
    @Override public Object clone(){ ifina8100C0DTOin c=new ifina8100C0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina8100C0DTOin in=(ifina8100C0DTOin)src;
      planId=in.planId; waveId=in.waveId; targetTypeCd=in.targetTypeCd; targetId=in.targetId; strategy7rCd=in.strategy7rCd;
      currentPlatformCd=in.currentPlatformCd; targetPlatformCd=in.targetPlatformCd; difficultyCd=in.difficultyCd;
      statusCd=in.statusCd; remark=in.remark; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("planId", FieldProperty.builder().setPhysicalName("planId").setLogicalName("planId").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
