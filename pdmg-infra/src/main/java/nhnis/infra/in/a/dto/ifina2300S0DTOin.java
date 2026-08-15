package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina2300S0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String keyword, appId, mapTypeCd, refId, roleCd;
    private Integer pageNo, pageSize;
    public String getKeyword(){return keyword;} public void setKeyword(String v){keyword=v;}
    public String getAppId(){return appId;} public void setAppId(String v){appId=v;}
    public String getMapTypeCd(){return mapTypeCd;} public void setMapTypeCd(String v){mapTypeCd=v;}
    public String getRefId(){return refId;} public void setRefId(String v){refId=v;}
    public String getRoleCd(){return roleCd;} public void setRoleCd(String v){roleCd=v;}
    public Integer getPageNo(){return pageNo;} public void setPageNo(Integer v){pageNo=v;}
    public Integer getPageSize(){return pageSize;} public void setPageSize(Integer v){pageSize=v;}
    @Override public Object clone(){ ifina2300S0DTOin c=new ifina2300S0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina2300S0DTOin in=(ifina2300S0DTOin)src;
      keyword=in.keyword; appId=in.appId; mapTypeCd=in.mapTypeCd; refId=in.refId; roleCd=in.roleCd;
      pageNo=in.pageNo; pageSize=in.pageSize; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("appId", FieldProperty.builder().setPhysicalName("appId").setLogicalName("appId").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
