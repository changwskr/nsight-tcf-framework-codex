package nhnis.infra.in.a.dto;
import java.util.Collections; import java.util.LinkedHashMap; import java.util.Map;
import com.ims.superspring.dto.DataObject; import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;
public class ifina3110U0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String groupId, groupName, systemId, techRoleCd, envCd, tierCd, statusCd, remark, chgUserId;
    private Integer activeNodes, standbyNodes, drNodes;
    public String getGroupId(){return groupId;} public void setGroupId(String v){groupId=v;}
    public String getGroupName(){return groupName;} public void setGroupName(String v){groupName=v;}
    public String getSystemId(){return systemId;} public void setSystemId(String v){systemId=v;}
    public String getTechRoleCd(){return techRoleCd;} public void setTechRoleCd(String v){techRoleCd=v;}
    public String getEnvCd(){return envCd;} public void setEnvCd(String v){envCd=v;}
    public String getTierCd(){return tierCd;} public void setTierCd(String v){tierCd=v;}
    public String getStatusCd(){return statusCd;} public void setStatusCd(String v){statusCd=v;}
    public Integer getActiveNodes(){return activeNodes;} public void setActiveNodes(Integer v){activeNodes=v;}
    public Integer getStandbyNodes(){return standbyNodes;} public void setStandbyNodes(Integer v){standbyNodes=v;}
    public Integer getDrNodes(){return drNodes;} public void setDrNodes(Integer v){drNodes=v;}
    public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
    public String getChgUserId(){return chgUserId;} public void setChgUserId(String v){ chgUserId=v; }
    @Override public Object clone(){ ifina3110U0DTOin c=new ifina3110U0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina3110U0DTOin in=(ifina3110U0DTOin)src;
      groupId=in.groupId; groupName=in.groupName; systemId=in.systemId; techRoleCd=in.techRoleCd; envCd=in.envCd; tierCd=in.tierCd; statusCd=in.statusCd;
      activeNodes=in.activeNodes; standbyNodes=in.standbyNodes; drNodes=in.drNodes; remark=in.remark; chgUserId=in.chgUserId; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { for(String n: new String[]{"groupId","groupName","systemId","techRoleCd","envCd","tierCd","statusCd","remark","chgUserId"})
      fieldPropertyMap.put(n, FieldProperty.builder().setPhysicalName(n).setLogicalName(n).setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build());
      for(String n: new String[]{"activeNodes","standbyNodes","drNodes"})
      fieldPropertyMap.put(n, FieldProperty.builder().setPhysicalName(n).setLogicalName(n).setType(FieldProperty.TYPE_OBJECT_INT).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
