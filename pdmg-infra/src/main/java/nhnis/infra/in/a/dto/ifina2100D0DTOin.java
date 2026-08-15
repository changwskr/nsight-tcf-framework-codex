package nhnis.infra.in.a.dto;
import java.util.ArrayList; import java.util.Collections; import java.util.LinkedHashMap; import java.util.List; import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAlias; import com.fasterxml.jackson.annotation.JsonProperty;
import com.ims.superspring.dto.DataObject; import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;
public class ifina2100D0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    @JsonProperty("systemIdList") @JsonAlias({"SYSTEM_ID_LIST"})
    private List<String> systemIdList = new ArrayList<>();
    public List<String> getSystemIdList(){return systemIdList;}
    public void setSystemIdList(List<String> v){ systemIdList = v!=null?v:new ArrayList<>(); }
    @Override public Object clone(){ ifina2100D0DTOin c=new ifina2100D0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina2100D0DTOin in=(ifina2100D0DTOin)src; systemIdList=in.systemIdList==null?new ArrayList<>():new ArrayList<>(in.systemIdList); }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("systemIdList", FieldProperty.builder().setPhysicalName("systemIdList").setLogicalName("systemIdList").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
