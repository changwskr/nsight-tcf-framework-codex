package nhnis.infra.in.a.dto;
import java.util.Collections; import java.util.LinkedHashMap; import java.util.Map;
import com.ims.superspring.dto.DataObject; import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;
public class ifina3110S0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String keyword, groupId, groupName, systemId, techRoleCd, envCd, tierCd, statusCd;
    private Integer pageNo, pageSize;
    public String getKeyword(){return keyword;} public void setKeyword(String v){keyword=v;}
    public String getGroupId(){return groupId;} public void setGroupId(String v){groupId=v;}
    public String getGroupName(){return groupName;} public void setGroupName(String v){groupName=v;}
    public String getSystemId(){return systemId;} public void setSystemId(String v){systemId=v;}
    public String getTechRoleCd(){return techRoleCd;} public void setTechRoleCd(String v){techRoleCd=v;}
    public String getEnvCd(){return envCd;} public void setEnvCd(String v){envCd=v;}
    public String getTierCd(){return tierCd;} public void setTierCd(String v){tierCd=v;}
    public String getStatusCd(){return statusCd;} public void setStatusCd(String v){statusCd=v;}
    public Integer getPageNo(){return pageNo;} public void setPageNo(Integer v){pageNo=v;}
    public Integer getPageSize(){return pageSize;} public void setPageSize(Integer v){pageSize=v;}
    @Override public Object clone(){ ifina3110S0DTOin c=new ifina3110S0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina3110S0DTOin in=(ifina3110S0DTOin)src;
      keyword=in.keyword; groupId=in.groupId; groupName=in.groupName; systemId=in.systemId; techRoleCd=in.techRoleCd;
      envCd=in.envCd; tierCd=in.tierCd; statusCd=in.statusCd; pageNo=in.pageNo; pageSize=in.pageSize; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { for(String n: new String[]{"keyword","groupId","groupName","systemId","techRoleCd","envCd","tierCd","statusCd"})
      fieldPropertyMap.put(n, FieldProperty.builder().setPhysicalName(n).setLogicalName(n).setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build());
      for(String n: new String[]{"pageNo","pageSize"}) fieldPropertyMap.put(n, FieldProperty.builder().setPhysicalName(n).setLogicalName(n).setType(FieldProperty.TYPE_OBJECT_INT).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
