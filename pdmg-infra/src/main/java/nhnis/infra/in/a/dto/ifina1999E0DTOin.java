package nhnis.infra.in.a.dto;
import java.util.ArrayList; import java.util.Collections; import java.util.LinkedHashMap; import java.util.List; import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ims.superspring.dto.DataObject; import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;
public class ifina1999E0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    @JsonProperty("serverIdList")
    private List<String> serverIdList = new ArrayList<>();
    private String defaultSystemId; private String defaultGroupId; private String dryRunYn;
    public List<String> getServerIdList(){return serverIdList;}
    public void setServerIdList(List<String> v){ serverIdList=v!=null?v:new ArrayList<>(); }
    public String getDefaultSystemId(){return defaultSystemId;} public void setDefaultSystemId(String v){defaultSystemId=v;}
    public String getDefaultGroupId(){return defaultGroupId;} public void setDefaultGroupId(String v){defaultGroupId=v;}
    public String getDryRunYn(){return dryRunYn;} public void setDryRunYn(String v){dryRunYn=v;}
    @Override public Object clone(){ ifina1999E0DTOin c=new ifina1999E0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina1999E0DTOin in=(ifina1999E0DTOin)src;
      serverIdList=in.serverIdList==null?new ArrayList<>():new ArrayList<>(in.serverIdList);
      defaultSystemId=in.defaultSystemId; defaultGroupId=in.defaultGroupId; dryRunYn=in.dryRunYn; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static {
      fieldPropertyMap.put("serverIdList", FieldProperty.builder().setPhysicalName("serverIdList").setLogicalName("serverIdList").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build());
    }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
