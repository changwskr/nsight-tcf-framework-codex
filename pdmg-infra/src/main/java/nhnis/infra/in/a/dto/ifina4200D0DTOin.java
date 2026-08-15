package nhnis.infra.in.a.dto;
import java.util.ArrayList; import java.util.Collections; import java.util.LinkedHashMap; import java.util.List; import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ims.superspring.dto.DataObject; import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;
public class ifina4200D0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    @JsonProperty("dbIdList")
    private List<String> dbIdList = new ArrayList<>();
    public List<String> getDbIdList(){return dbIdList;}
    public void setDbIdList(List<String> v){ dbIdList=v!=null?v:new ArrayList<>(); }
    @Override public Object clone(){ ifina4200D0DTOin c=new ifina4200D0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina4200D0DTOin in=(ifina4200D0DTOin)src; dbIdList=in.dbIdList==null?new ArrayList<>():new ArrayList<>(in.dbIdList); }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("dbIdList", FieldProperty.builder().setPhysicalName("dbIdList").setLogicalName("dbIdList").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
