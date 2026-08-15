package nhnis.infra.in.a.dto;
import java.util.Collections; import java.util.LinkedHashMap; import java.util.Map;
import com.ims.superspring.dto.DataObject; import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;
public class ifina4200S0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String keyword, dbId, dbName, assetId, systemId, engineCd, statusCd;
    private Integer pageNo, pageSize;
    public String getKeyword(){return keyword;} public void setKeyword(String v){keyword=v;}
    public String getDbId(){return dbId;} public void setDbId(String v){dbId=v;}
    public String getDbName(){return dbName;} public void setDbName(String v){dbName=v;}
    public String getAssetId(){return assetId;} public void setAssetId(String v){assetId=v;}
    public String getSystemId(){return systemId;} public void setSystemId(String v){systemId=v;}
    public String getEngineCd(){return engineCd;} public void setEngineCd(String v){engineCd=v;}
    public String getStatusCd(){return statusCd;} public void setStatusCd(String v){statusCd=v;}
    public Integer getPageNo(){return pageNo;} public void setPageNo(Integer v){pageNo=v;}
    public Integer getPageSize(){return pageSize;} public void setPageSize(Integer v){pageSize=v;}
    @Override public Object clone(){ ifina4200S0DTOin c=new ifina4200S0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina4200S0DTOin in=(ifina4200S0DTOin)src;
      keyword=in.keyword; dbId=in.dbId; dbName=in.dbName; assetId=in.assetId; systemId=in.systemId; engineCd=in.engineCd; statusCd=in.statusCd; pageNo=in.pageNo; pageSize=in.pageSize; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { for(String n: new String[]{"keyword","dbId","dbName","assetId","systemId","engineCd","statusCd"})
      fieldPropertyMap.put(n, FieldProperty.builder().setPhysicalName(n).setLogicalName(n).setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build());
      for(String n: new String[]{"pageNo","pageSize"}) fieldPropertyMap.put(n, FieldProperty.builder().setPhysicalName(n).setLogicalName(n).setType(FieldProperty.TYPE_OBJECT_INT).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
