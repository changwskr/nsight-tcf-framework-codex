package nhnis.infra.in.a.dto;
import java.util.ArrayList; import java.util.Collections; import java.util.LinkedHashMap; import java.util.List; import java.util.Map;
import com.ims.superspring.dto.DataObject; import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;
public class ifina3100U0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String assetId, assetName, groupId, systemId, assetKindCd, techRoleCd, envCd, tierCd, serviceModelCd, deployModelCd, statusCd,
            osName, osVersion, osEolDate, remark, chgUserId;
    private List<Map<String, Object>> attrs = new ArrayList<>();
    public String getAssetId(){return assetId;} public void setAssetId(String v){assetId=v;}
    public String getAssetName(){return assetName;} public void setAssetName(String v){assetName=v;}
    public String getGroupId(){return groupId;} public void setGroupId(String v){groupId=v;}
    public String getSystemId(){return systemId;} public void setSystemId(String v){systemId=v;}
    public String getAssetKindCd(){return assetKindCd;} public void setAssetKindCd(String v){assetKindCd=v;}
    public String getTechRoleCd(){return techRoleCd;} public void setTechRoleCd(String v){techRoleCd=v;}
    public String getEnvCd(){return envCd;} public void setEnvCd(String v){envCd=v;}
    public String getTierCd(){return tierCd;} public void setTierCd(String v){tierCd=v;}
    public String getServiceModelCd(){return serviceModelCd;} public void setServiceModelCd(String v){serviceModelCd=v;}
    public String getDeployModelCd(){return deployModelCd;} public void setDeployModelCd(String v){deployModelCd=v;}
    public String getStatusCd(){return statusCd;} public void setStatusCd(String v){statusCd=v;}
    public String getOsName(){return osName;} public void setOsName(String v){osName=v;}
    public String getOsVersion(){return osVersion;} public void setOsVersion(String v){osVersion=v;}
    public String getOsEolDate(){return osEolDate;} public void setOsEolDate(String v){osEolDate=v;}
    public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
    public String getChgUserId(){return chgUserId;} public void setChgUserId(String v){chgUserId=v;}
    public List<Map<String, Object>> getAttrs(){return attrs;} public void setAttrs(List<Map<String, Object>> v){attrs=v!=null?v:new ArrayList<>();}
    @Override public Object clone(){ ifina3100U0DTOin c=new ifina3100U0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina3100U0DTOin in=(ifina3100U0DTOin)src;
      assetId=in.assetId; assetName=in.assetName; groupId=in.groupId; systemId=in.systemId; assetKindCd=in.assetKindCd;
      techRoleCd=in.techRoleCd; envCd=in.envCd; tierCd=in.tierCd; serviceModelCd=in.serviceModelCd; deployModelCd=in.deployModelCd;
      statusCd=in.statusCd; osName=in.osName; osVersion=in.osVersion; osEolDate=in.osEolDate; remark=in.remark; chgUserId=in.chgUserId;
      attrs=in.attrs==null?new ArrayList<>():new ArrayList<>(in.attrs); }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { for(String n: new String[]{"assetId", "assetName", "groupId", "systemId", "assetKindCd", "techRoleCd", "envCd", "tierCd", "serviceModelCd", "deployModelCd", "statusCd", "osName", "osVersion", "osEolDate", "remark", "chgUserId"})
      fieldPropertyMap.put(n, FieldProperty.builder().setPhysicalName(n).setLogicalName(n).setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
