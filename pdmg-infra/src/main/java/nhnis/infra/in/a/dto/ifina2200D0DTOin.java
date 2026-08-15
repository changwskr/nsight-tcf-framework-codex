package nhnis.infra.in.a.dto;
import java.util.ArrayList; import java.util.Collections; import java.util.LinkedHashMap; import java.util.List; import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAlias; import com.fasterxml.jackson.annotation.JsonProperty;
import com.ims.superspring.dto.DataObject; import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;
public class ifina2200D0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    @JsonProperty("appIdList") @JsonAlias({"APP_ID_LIST"})
    private List<String> appIdList = new ArrayList<>();
    public List<String> getAppIdList(){return appIdList;}
    public void setAppIdList(List<String> v){ appIdList = v!=null?v:new ArrayList<>(); }
    @Override public Object clone(){ ifina2200D0DTOin c=new ifina2200D0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina2200D0DTOin in=(ifina2200D0DTOin)src; appIdList=in.appIdList==null?new ArrayList<>():new ArrayList<>(in.appIdList); }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("appIdList", FieldProperty.builder().setPhysicalName("appIdList").setLogicalName("appIdList").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
