package nhnis.infra.in.a.dto;
import java.util.ArrayList; import java.util.Collections; import java.util.LinkedHashMap; import java.util.List; import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ims.superspring.dto.DataObject; import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;
public class ifina5100D0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    @JsonProperty("endpointIdList")
    private List<String> endpointIdList = new ArrayList<>();
    public List<String> getEndpointIdList(){return endpointIdList;}
    public void setEndpointIdList(List<String> v){ endpointIdList=v!=null?v:new ArrayList<>(); }
    @Override public Object clone(){ ifina5100D0DTOin c=new ifina5100D0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina5100D0DTOin in=(ifina5100D0DTOin)src; endpointIdList=in.endpointIdList==null?new ArrayList<>():new ArrayList<>(in.endpointIdList); }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("endpointIdList", FieldProperty.builder().setPhysicalName("endpointIdList").setLogicalName("endpointIdList").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
