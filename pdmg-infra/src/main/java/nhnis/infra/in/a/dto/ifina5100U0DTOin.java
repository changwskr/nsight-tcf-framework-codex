package nhnis.infra.in.a.dto;
import java.util.Collections; import java.util.LinkedHashMap; import java.util.Map;
import com.ims.superspring.dto.DataObject; import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;
public class ifina5100U0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String endpointId, assetId, endpointTypeCd, address, portNo, protocolCd, primaryYn, remark, chgUserId;
    public String getEndpointId(){return endpointId;} public void setEndpointId(String v){endpointId=v;}
    public String getAssetId(){return assetId;} public void setAssetId(String v){assetId=v;}
    public String getEndpointTypeCd(){return endpointTypeCd;} public void setEndpointTypeCd(String v){endpointTypeCd=v;}
    public String getAddress(){return address;} public void setAddress(String v){address=v;}
    public String getPortNo(){return portNo;} public void setPortNo(String v){portNo=v;}
    public String getProtocolCd(){return protocolCd;} public void setProtocolCd(String v){protocolCd=v;}
    public String getPrimaryYn(){return primaryYn;} public void setPrimaryYn(String v){primaryYn=v;}
    public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
    public String getChgUserId(){return chgUserId;} public void setChgUserId(String v){chgUserId=v;}
    @Override public Object clone(){ ifina5100U0DTOin c=new ifina5100U0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina5100U0DTOin in=(ifina5100U0DTOin)src;
      endpointId=in.endpointId; assetId=in.assetId; endpointTypeCd=in.endpointTypeCd; address=in.address; portNo=in.portNo; protocolCd=in.protocolCd; primaryYn=in.primaryYn; remark=in.remark; chgUserId=in.chgUserId; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { for(String n: new String[]{"endpointId","assetId","endpointTypeCd","address","portNo","protocolCd","primaryYn","remark","chgUserId"})
      fieldPropertyMap.put(n, FieldProperty.builder().setPhysicalName(n).setLogicalName(n).setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
