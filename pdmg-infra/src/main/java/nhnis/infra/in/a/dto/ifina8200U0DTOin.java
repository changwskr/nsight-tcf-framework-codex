package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina8200U0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String waveId, waveName, plannedStart, plannedEnd, statusCd, remark;
    private Integer sequenceNo;
    public String getWaveId(){return waveId;} public void setWaveId(String v){waveId=v;}
    public String getWaveName(){return waveName;} public void setWaveName(String v){waveName=v;}
    public Integer getSequenceNo(){return sequenceNo;} public void setSequenceNo(Integer v){sequenceNo=v;}
    public String getPlannedStart(){return plannedStart;} public void setPlannedStart(String v){plannedStart=v;}
    public String getPlannedEnd(){return plannedEnd;} public void setPlannedEnd(String v){plannedEnd=v;}
    public String getStatusCd(){return statusCd;} public void setStatusCd(String v){statusCd=v;}
    public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
    @Override public Object clone(){ ifina8200U0DTOin c=new ifina8200U0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina8200U0DTOin in=(ifina8200U0DTOin)src;
      waveId=in.waveId; waveName=in.waveName; sequenceNo=in.sequenceNo; plannedStart=in.plannedStart;
      plannedEnd=in.plannedEnd; statusCd=in.statusCd; remark=in.remark; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("waveId", FieldProperty.builder().setPhysicalName("waveId").setLogicalName("waveId").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
