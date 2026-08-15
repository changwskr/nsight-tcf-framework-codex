package nhnis.infra.in.a.dto;
import java.util.Collections; import java.util.LinkedHashMap; import java.util.Map;
import com.ims.superspring.dto.DataObject; import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;
public class ifina1500S0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String keyword, entityType, orgId, personId, activeYn;
    private Integer pageNo, pageSize;
    public String getKeyword(){return keyword;} public void setKeyword(String v){keyword=v;}
    public String getEntityType(){return entityType;} public void setEntityType(String v){entityType=v;}
    public String getOrgId(){return orgId;} public void setOrgId(String v){orgId=v;}
    public String getPersonId(){return personId;} public void setPersonId(String v){personId=v;}
    public String getActiveYn(){return activeYn;} public void setActiveYn(String v){activeYn=v;}
    public Integer getPageNo(){return pageNo;} public void setPageNo(Integer v){pageNo=v;}
    public Integer getPageSize(){return pageSize;} public void setPageSize(Integer v){pageSize=v;}
    @Override public Object clone(){ ifina1500S0DTOin c=new ifina1500S0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina1500S0DTOin in=(ifina1500S0DTOin)src; keyword=in.keyword; entityType=in.entityType; orgId=in.orgId; personId=in.personId; activeYn=in.activeYn; pageNo=in.pageNo; pageSize=in.pageSize; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { for(String n: new String[]{"keyword", "entityType", "orgId", "personId", "activeYn"})
      fieldPropertyMap.put(n, FieldProperty.builder().setPhysicalName(n).setLogicalName(n).setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build());
      for(String n: new String[]{"pageNo", "pageSize"}) fieldPropertyMap.put(n, FieldProperty.builder().setPhysicalName(n).setLogicalName(n).setType(FieldProperty.TYPE_OBJECT_INT).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
