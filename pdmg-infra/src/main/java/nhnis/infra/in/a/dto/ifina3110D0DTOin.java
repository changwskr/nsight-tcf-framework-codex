package nhnis.infra.in.a.dto;
import java.util.ArrayList; import java.util.Collections; import java.util.LinkedHashMap; import java.util.List; import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAlias; import com.fasterxml.jackson.annotation.JsonProperty;
import com.ims.superspring.dto.DataObject; import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;
public class ifina3110D0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    @JsonProperty("groupIdList") @JsonAlias({"GROUP_ID_LIST"})
    private List<String> groupIdList = new ArrayList<>();
    public List<String> getGroupIdList(){return groupIdList;}
    public void setGroupIdList(List<String> v){ groupIdList = v!=null?v:new ArrayList<>(); }
    @Override public Object clone(){ ifina3110D0DTOin c=new ifina3110D0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina3110D0DTOin in=(ifina3110D0DTOin)src; groupIdList=in.groupIdList==null?new ArrayList<>():new ArrayList<>(in.groupIdList); }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("groupIdList", FieldProperty.builder().setPhysicalName("groupIdList").setLogicalName("groupIdList").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
