package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina5200S0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String keyword, interfaceId, fromAppId, toAppId, protocolCd, directionCd, criticalYn;
    private Integer pageNo, pageSize;
    public String getKeyword(){return keyword;} public void setKeyword(String v){keyword=v;}
    public String getInterfaceId(){return interfaceId;} public void setInterfaceId(String v){interfaceId=v;}
    public String getFromAppId(){return fromAppId;} public void setFromAppId(String v){fromAppId=v;}
    public String getToAppId(){return toAppId;} public void setToAppId(String v){toAppId=v;}
    public String getProtocolCd(){return protocolCd;} public void setProtocolCd(String v){protocolCd=v;}
    public String getDirectionCd(){return directionCd;} public void setDirectionCd(String v){directionCd=v;}
    public String getCriticalYn(){return criticalYn;} public void setCriticalYn(String v){criticalYn=v;}
    public Integer getPageNo(){return pageNo;} public void setPageNo(Integer v){pageNo=v;}
    public Integer getPageSize(){return pageSize;} public void setPageSize(Integer v){pageSize=v;}
    @Override public Object clone(){ ifina5200S0DTOin c=new ifina5200S0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina5200S0DTOin in=(ifina5200S0DTOin)src;
      keyword=in.keyword; interfaceId=in.interfaceId; fromAppId=in.fromAppId; toAppId=in.toAppId;
      protocolCd=in.protocolCd; directionCd=in.directionCd; criticalYn=in.criticalYn; pageNo=in.pageNo; pageSize=in.pageSize; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("keyword", FieldProperty.builder().setPhysicalName("keyword").setLogicalName("keyword").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
