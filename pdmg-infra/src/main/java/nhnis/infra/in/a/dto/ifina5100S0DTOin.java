package nhnis.infra.in.a.dto;
import java.util.Collections; import java.util.LinkedHashMap; import java.util.Map;
import com.ims.superspring.dto.DataObject; import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;
public class ifina5100S0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String keyword, endpointId, assetId, address, endpointTypeCd, primaryYn;
    private Integer pageNo, pageSize;
    public String getKeyword(){return keyword;} public void setKeyword(String v){keyword=v;}
    public String getEndpointId(){return endpointId;} public void setEndpointId(String v){endpointId=v;}
    public String getAssetId(){return assetId;} public void setAssetId(String v){assetId=v;}
    public String getAddress(){return address;} public void setAddress(String v){address=v;}
    public String getEndpointTypeCd(){return endpointTypeCd;} public void setEndpointTypeCd(String v){endpointTypeCd=v;}
    public String getPrimaryYn(){return primaryYn;} public void setPrimaryYn(String v){primaryYn=v;}
    public Integer getPageNo(){return pageNo;} public void setPageNo(Integer v){pageNo=v;}
    public Integer getPageSize(){return pageSize;} public void setPageSize(Integer v){pageSize=v;}
    @Override public Object clone(){ ifina5100S0DTOin c=new ifina5100S0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina5100S0DTOin in=(ifina5100S0DTOin)src;
      keyword=in.keyword; endpointId=in.endpointId; assetId=in.assetId; address=in.address; endpointTypeCd=in.endpointTypeCd; primaryYn=in.primaryYn; pageNo=in.pageNo; pageSize=in.pageSize; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { for(String n: new String[]{"keyword","endpointId","assetId","address","endpointTypeCd","primaryYn"})
      fieldPropertyMap.put(n, FieldProperty.builder().setPhysicalName(n).setLogicalName(n).setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build());
      for(String n: new String[]{"pageNo","pageSize"}) fieldPropertyMap.put(n, FieldProperty.builder().setPhysicalName(n).setLogicalName(n).setType(FieldProperty.TYPE_OBJECT_INT).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
