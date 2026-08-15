package nhnis.infra.in.a.dto;
import java.util.ArrayList; import java.util.Collections; import java.util.LinkedHashMap; import java.util.List; import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ims.superspring.dto.DataObject; import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;
public class ifina1200D0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String entityType; private String templateId;
    @JsonProperty("itemIdList")
    private List<String> itemIdList = new ArrayList<>();
    @JsonProperty("templateIdList")
    private List<String> templateIdList = new ArrayList<>();
    public String getEntityType(){return entityType;} public void setEntityType(String v){entityType=v;}
    public String getTemplateId(){return templateId;} public void setTemplateId(String v){templateId=v;}
    public List<String> getItemIdList(){return itemIdList;}
    public void setItemIdList(List<String> v){ itemIdList=v!=null?v:new ArrayList<>(); }
    public List<String> getTemplateIdList(){return templateIdList;}
    public void setTemplateIdList(List<String> v){ templateIdList=v!=null?v:new ArrayList<>(); }
    @Override public Object clone(){ ifina1200D0DTOin c=new ifina1200D0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina1200D0DTOin in=(ifina1200D0DTOin)src;
      entityType=in.entityType; templateId=in.templateId;
      itemIdList=in.itemIdList==null?new ArrayList<>():new ArrayList<>(in.itemIdList);
      templateIdList=in.templateIdList==null?new ArrayList<>():new ArrayList<>(in.templateIdList); }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static {
      fieldPropertyMap.put("entityType", FieldProperty.builder().setPhysicalName("entityType").setLogicalName("entityType").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build());
    }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
