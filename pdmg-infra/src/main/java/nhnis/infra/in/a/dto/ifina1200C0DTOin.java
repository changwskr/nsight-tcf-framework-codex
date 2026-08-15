package nhnis.infra.in.a.dto;
import java.util.Collections; import java.util.LinkedHashMap; import java.util.Map;
import com.ims.superspring.dto.DataObject; import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;
public class ifina1200C0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String entityType, templateId, templateName, techRoleCd, itemId, itemName, itemTypeCd, requiredYn, activeYn, remark, regUserId;
    private Integer sortNo;
    public String getEntityType(){return entityType;} public void setEntityType(String v){entityType=v;}
    public String getTemplateId(){return templateId;} public void setTemplateId(String v){templateId=v;}
    public String getTemplateName(){return templateName;} public void setTemplateName(String v){templateName=v;}
    public String getTechRoleCd(){return techRoleCd;} public void setTechRoleCd(String v){techRoleCd=v;}
    public String getItemId(){return itemId;} public void setItemId(String v){itemId=v;}
    public String getItemName(){return itemName;} public void setItemName(String v){itemName=v;}
    public String getItemTypeCd(){return itemTypeCd;} public void setItemTypeCd(String v){itemTypeCd=v;}
    public String getRequiredYn(){return requiredYn;} public void setRequiredYn(String v){requiredYn=v;}
    public String getActiveYn(){return activeYn;} public void setActiveYn(String v){activeYn=v;}
    public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
    public String getRegUserId(){return regUserId;} public void setRegUserId(String v){regUserId=v;}
    public Integer getSortNo(){return sortNo;} public void setSortNo(Integer v){sortNo=v;}
    @Override public Object clone(){ ifina1200C0DTOin c=new ifina1200C0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina1200C0DTOin in=(ifina1200C0DTOin)src; entityType=in.entityType; templateId=in.templateId; templateName=in.templateName; techRoleCd=in.techRoleCd; itemId=in.itemId; itemName=in.itemName; itemTypeCd=in.itemTypeCd; requiredYn=in.requiredYn; activeYn=in.activeYn; remark=in.remark; regUserId=in.regUserId; sortNo=in.sortNo; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { for(String n: new String[]{"entityType", "templateId", "templateName", "techRoleCd", "itemId", "itemName", "itemTypeCd", "requiredYn", "activeYn", "remark", "regUserId"})
      fieldPropertyMap.put(n, FieldProperty.builder().setPhysicalName(n).setLogicalName(n).setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build());
      for(String n: new String[]{"sortNo"}) fieldPropertyMap.put(n, FieldProperty.builder().setPhysicalName(n).setLogicalName(n).setType(FieldProperty.TYPE_OBJECT_INT).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
