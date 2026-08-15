package nhnis.infra.in.a.dto;
import java.util.ArrayList; import java.util.Collections; import java.util.LinkedHashMap; import java.util.List; import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ims.superspring.dto.DataObject; import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;
public class ifina4100D0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    @JsonProperty("mwIdList")
    private List<String> mwIdList = new ArrayList<>();
    public List<String> getMwIdList(){return mwIdList;}
    public void setMwIdList(List<String> v){ mwIdList=v!=null?v:new ArrayList<>(); }
    @Override public Object clone(){ ifina4100D0DTOin c=new ifina4100D0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina4100D0DTOin in=(ifina4100D0DTOin)src; mwIdList=in.mwIdList==null?new ArrayList<>():new ArrayList<>(in.mwIdList); }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("mwIdList", FieldProperty.builder().setPhysicalName("mwIdList").setLogicalName("mwIdList").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
