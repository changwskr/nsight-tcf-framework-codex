package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina6100S0DTOout extends DataObject {
    private static final long serialVersionUID = 1L;
    private String profileId, targetTypeCd, targetId, opsHoursCd, haYn, haModeCd, clusterYn, drYn, drModeCd;
    private Integer rtoMinutes, rpoMinutes; private String backupYn, monitoringYn, remark;
    private List<String> warnings = new ArrayList<>(); private String RSLT_CD, RSLT_MSG;
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
    public List<String> getWarnings(){return warnings;} public void setWarnings(List<String> v){warnings=v!=null?v:new ArrayList<>();}
    public String getRSLT_CD(){return RSLT_CD;} public void setRSLT_CD(String v){RSLT_CD=v;}
    public String getRSLT_MSG(){return RSLT_MSG;} public void setRSLT_MSG(String v){RSLT_MSG=v;}
    @Override public Object clone(){ ifina6100S0DTOout c=new ifina6100S0DTOout(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina6100S0DTOout in=(ifina6100S0DTOout)src;
      profileId=in.profileId; targetTypeCd=in.targetTypeCd; targetId=in.targetId; opsHoursCd=in.opsHoursCd;
      haYn=in.haYn; haModeCd=in.haModeCd; clusterYn=in.clusterYn; drYn=in.drYn; drModeCd=in.drModeCd;
      rtoMinutes=in.rtoMinutes; rpoMinutes=in.rpoMinutes; backupYn=in.backupYn; monitoringYn=in.monitoringYn;
      remark=in.remark; warnings=new ArrayList<>(in.warnings); RSLT_CD=in.RSLT_CD; RSLT_MSG=in.RSLT_MSG; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("RSLT_CD", FieldProperty.builder().setPhysicalName("RSLT_CD").setLogicalName("RSLT_CD").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
