package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina6100U0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String profileId, targetTypeCd, targetId, opsHoursCd, haYn, haModeCd, clusterYn, drYn, drModeCd, backupYn, monitoringYn, remark;
    private Integer rtoMinutes, rpoMinutes;
    public String getProfileId(){return profileId;} public void setProfileId(String v){profileId=v;}
    public String getTargetTypeCd(){return targetTypeCd;} public void setTargetTypeCd(String v){targetTypeCd=v;}
    public String getTargetId(){return targetId;} public void setTargetId(String v){targetId=v;}
    public String getOpsHoursCd(){return opsHoursCd;} public void setOpsHoursCd(String v){opsHoursCd=v;}
    public String getHaYn(){return haYn;} public void setHaYn(String v){haYn=v;}
    public String getHaModeCd(){return haModeCd;} public void setHaModeCd(String v){haModeCd=v;}
    public String getClusterYn(){return clusterYn;} public void setClusterYn(String v){clusterYn=v;}
    public String getDrYn(){return drYn;} public void setDrYn(String v){drYn=v;}
    public String getDrModeCd(){return drModeCd;} public void setDrModeCd(String v){drModeCd=v;}
    public Integer getRtoMinutes(){return rtoMinutes;} public void setRtoMinutes(Integer v){rtoMinutes=v;}
    public Integer getRpoMinutes(){return rpoMinutes;} public void setRpoMinutes(Integer v){rpoMinutes=v;}
    public String getBackupYn(){return backupYn;} public void setBackupYn(String v){backupYn=v;}
    public String getMonitoringYn(){return monitoringYn;} public void setMonitoringYn(String v){monitoringYn=v;}
    public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
    @Override public Object clone(){ ifina6100U0DTOin c=new ifina6100U0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina6100U0DTOin in=(ifina6100U0DTOin)src;
      profileId=in.profileId; targetTypeCd=in.targetTypeCd; targetId=in.targetId; opsHoursCd=in.opsHoursCd;
      haYn=in.haYn; haModeCd=in.haModeCd; clusterYn=in.clusterYn; drYn=in.drYn; drModeCd=in.drModeCd;
      rtoMinutes=in.rtoMinutes; rpoMinutes=in.rpoMinutes; backupYn=in.backupYn; monitoringYn=in.monitoringYn; remark=in.remark; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("targetId", FieldProperty.builder().setPhysicalName("targetId").setLogicalName("targetId").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
