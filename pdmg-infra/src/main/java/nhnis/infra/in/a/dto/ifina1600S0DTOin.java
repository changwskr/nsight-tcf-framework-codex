package nhnis.infra.in.a.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina1600S0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String keyword, entityType, targetTypeCd, targetId, actionCd;
    private Integer pageNo, pageSize;
    public String getKeyword(){return keyword;} public void setKeyword(String v){keyword=v;}
    public String getEntityType(){return entityType;} public void setEntityType(String v){entityType=v;}
    public String getTargetTypeCd(){return targetTypeCd;} public void setTargetTypeCd(String v){targetTypeCd=v;}
    public String getTargetId(){return targetId;} public void setTargetId(String v){targetId=v;}
    public String getActionCd(){return actionCd;} public void setActionCd(String v){actionCd=v;}
    public Integer getPageNo(){return pageNo;} public void setPageNo(Integer v){pageNo=v;}
    public Integer getPageSize(){return pageSize;} public void setPageSize(Integer v){pageSize=v;}
    @Override public Object clone(){ ifina1600S0DTOin c=new ifina1600S0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina1600S0DTOin in=(ifina1600S0DTOin)src;
      keyword=in.keyword; entityType=in.entityType; targetTypeCd=in.targetTypeCd; targetId=in.targetId; actionCd=in.actionCd; pageNo=in.pageNo; pageSize=in.pageSize; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { for(String n: new String[]{"keyword","entityType","targetTypeCd","targetId","actionCd"})
      fieldPropertyMap.put(n, FieldProperty.builder().setPhysicalName(n).setLogicalName(n).setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
