package nhnis.infra.in.a.dto;
import java.util.Collections; import java.util.LinkedHashMap; import java.util.Map;
import com.ims.superspring.dto.DataObject; import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;
public class ifina4100C0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String mwId, assetId, productName, versionNo, eolDate, statusCd, remark, regUserId;
    public String getMwId(){return mwId;} public void setMwId(String v){mwId=v;}
    public String getAssetId(){return assetId;} public void setAssetId(String v){assetId=v;}
    public String getProductName(){return productName;} public void setProductName(String v){productName=v;}
    public String getVersionNo(){return versionNo;} public void setVersionNo(String v){versionNo=v;}
    public String getEolDate(){return eolDate;} public void setEolDate(String v){eolDate=v;}
    public String getStatusCd(){return statusCd;} public void setStatusCd(String v){statusCd=v;}
    public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
    public String getRegUserId(){return regUserId;} public void setRegUserId(String v){regUserId=v;}
    @Override public Object clone(){ ifina4100C0DTOin c=new ifina4100C0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina4100C0DTOin in=(ifina4100C0DTOin)src;
      mwId=in.mwId; assetId=in.assetId; productName=in.productName; versionNo=in.versionNo; eolDate=in.eolDate; statusCd=in.statusCd; remark=in.remark; regUserId=in.regUserId; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { for(String n: new String[]{"mwId","assetId","productName","versionNo","eolDate","statusCd","remark","regUserId"})
      fieldPropertyMap.put(n, FieldProperty.builder().setPhysicalName(n).setLogicalName(n).setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
