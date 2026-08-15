package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina1300S0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String keyword, checklistId, categoryKo, activeYn;
    private Integer pageNo, pageSize;
    public String getKeyword(){return keyword;} public void setKeyword(String v){keyword=v;}
    public String getChecklistId(){return checklistId;} public void setChecklistId(String v){checklistId=v;}
    public String getCategoryKo(){return categoryKo;} public void setCategoryKo(String v){categoryKo=v;}
    public String getActiveYn(){return activeYn;} public void setActiveYn(String v){activeYn=v;}
    public Integer getPageNo(){return pageNo;} public void setPageNo(Integer v){pageNo=v;}
    public Integer getPageSize(){return pageSize;} public void setPageSize(Integer v){pageSize=v;}
    @Override public Object clone(){ ifina1300S0DTOin c=new ifina1300S0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina1300S0DTOin in=(ifina1300S0DTOin)src;
      keyword=in.keyword; checklistId=in.checklistId; categoryKo=in.categoryKo; activeYn=in.activeYn; pageNo=in.pageNo; pageSize=in.pageSize; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("keyword", FieldProperty.builder().setPhysicalName("keyword").setLogicalName("keyword").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
