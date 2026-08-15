package nhnis.infra.in.a.dto;
import java.util.Collections; import java.util.LinkedHashMap; import java.util.Map;
import com.ims.superspring.dto.DataObject; import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;
public class ifina3100S1DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String assetId;
    public String getAssetId(){return assetId;} public void setAssetId(String v){assetId=v;}
    @Override public Object clone(){ ifina3100S1DTOin c=new ifina3100S1DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina3100S1DTOin in=(ifina3100S1DTOin)src; assetId=in.assetId; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { for(String n: new String[]{"assetId"})
      fieldPropertyMap.put(n, FieldProperty.builder().setPhysicalName(n).setLogicalName(n).setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
