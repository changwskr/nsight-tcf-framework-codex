package nhnis.infra.in.a.dto;
import java.util.Collections; import java.util.LinkedHashMap; import java.util.Map;
import com.ims.superspring.dto.DataObject; import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;
public class ifina4200U0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String dbId, dbName, engineCd, versionNo, assetId, systemId, eolDate, statusCd, remark, chgUserId;
    public String getDbId(){return dbId;} public void setDbId(String v){dbId=v;}
    public String getDbName(){return dbName;} public void setDbName(String v){dbName=v;}
    public String getEngineCd(){return engineCd;} public void setEngineCd(String v){engineCd=v;}
    public String getVersionNo(){return versionNo;} public void setVersionNo(String v){versionNo=v;}
    public String getAssetId(){return assetId;} public void setAssetId(String v){assetId=v;}
    public String getSystemId(){return systemId;} public void setSystemId(String v){systemId=v;}
    public String getEolDate(){return eolDate;} public void setEolDate(String v){eolDate=v;}
    public String getStatusCd(){return statusCd;} public void setStatusCd(String v){statusCd=v;}
    public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
    public String getChgUserId(){return chgUserId;} public void setChgUserId(String v){chgUserId=v;}
    @Override public Object clone(){ ifina4200U0DTOin c=new ifina4200U0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina4200U0DTOin in=(ifina4200U0DTOin)src;
      dbId=in.dbId; dbName=in.dbName; engineCd=in.engineCd; versionNo=in.versionNo; assetId=in.assetId; systemId=in.systemId; eolDate=in.eolDate; statusCd=in.statusCd; remark=in.remark; chgUserId=in.chgUserId; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { for(String n: new String[]{"dbId","dbName","engineCd","versionNo","assetId","systemId","eolDate","statusCd","remark","chgUserId"})
      fieldPropertyMap.put(n, FieldProperty.builder().setPhysicalName(n).setLogicalName(n).setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
