package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina1300C0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String checklistId, itemName, categoryKo, severityCd, activeYn, remark;
    private Integer sortNo;
    public String getChecklistId(){return checklistId;} public void setChecklistId(String v){checklistId=v;}
    public String getItemName(){return itemName;} public void setItemName(String v){itemName=v;}
    public String getCategoryKo(){return categoryKo;} public void setCategoryKo(String v){categoryKo=v;}
    public String getSeverityCd(){return severityCd;} public void setSeverityCd(String v){severityCd=v;}
    public String getActiveYn(){return activeYn;} public void setActiveYn(String v){activeYn=v;}
    public Integer getSortNo(){return sortNo;} public void setSortNo(Integer v){sortNo=v;}
    public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
    @Override public Object clone(){ ifina1300C0DTOin c=new ifina1300C0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina1300C0DTOin in=(ifina1300C0DTOin)src;
      checklistId=in.checklistId; itemName=in.itemName; categoryKo=in.categoryKo; severityCd=in.severityCd;
      activeYn=in.activeYn; sortNo=in.sortNo; remark=in.remark; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("checklistId", FieldProperty.builder().setPhysicalName("checklistId").setLogicalName("checklistId").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
