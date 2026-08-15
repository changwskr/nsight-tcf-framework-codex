package nhnis.infra.in.a.dto;
import java.util.ArrayList; import java.util.Collections; import java.util.LinkedHashMap; import java.util.List; import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ims.superspring.dto.DataObject; import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;
public class ifina3100D0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    @JsonProperty("assetIdList")
    private List<String> assetIdList = new ArrayList<>();
    private String hardDeleteYn;
    public List<String> getAssetIdList(){return assetIdList;}
    public void setAssetIdList(List<String> v){ assetIdList=v!=null?v:new ArrayList<>(); }
    public String getHardDeleteYn(){return hardDeleteYn;} public void setHardDeleteYn(String v){hardDeleteYn=v;}
    @Override public Object clone(){ ifina3100D0DTOin c=new ifina3100D0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina3100D0DTOin in=(ifina3100D0DTOin)src;
      assetIdList=in.assetIdList==null?new ArrayList<>():new ArrayList<>(in.assetIdList); hardDeleteYn=in.hardDeleteYn; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static {
      fieldPropertyMap.put("assetIdList", FieldProperty.builder().setPhysicalName("assetIdList").setLogicalName("assetIdList").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build());
      fieldPropertyMap.put("hardDeleteYn", FieldProperty.builder().setPhysicalName("hardDeleteYn").setLogicalName("hardDeleteYn").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build());
    }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
