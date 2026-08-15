package nhnis.infra.in.a.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina5300S0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String keyword, rootType, rootId, fromTypeCd, fromId, toTypeCd, toId, relationTypeCd, criticalYn;
    private Integer depth, pageNo, pageSize;
    public String getKeyword(){return keyword;} public void setKeyword(String v){keyword=v;}
    public String getRootType(){return rootType;} public void setRootType(String v){rootType=v;}
    public String getRootId(){return rootId;} public void setRootId(String v){rootId=v;}
    public String getFromTypeCd(){return fromTypeCd;} public void setFromTypeCd(String v){fromTypeCd=v;}
    public String getFromId(){return fromId;} public void setFromId(String v){fromId=v;}
    public String getToTypeCd(){return toTypeCd;} public void setToTypeCd(String v){toTypeCd=v;}
    public String getToId(){return toId;} public void setToId(String v){toId=v;}
    public String getRelationTypeCd(){return relationTypeCd;} public void setRelationTypeCd(String v){relationTypeCd=v;}
    public String getCriticalYn(){return criticalYn;} public void setCriticalYn(String v){criticalYn=v;}
    public Integer getDepth(){return depth;} public void setDepth(Integer v){depth=v;}
    public Integer getPageNo(){return pageNo;} public void setPageNo(Integer v){pageNo=v;}
    public Integer getPageSize(){return pageSize;} public void setPageSize(Integer v){pageSize=v;}
    @Override public Object clone(){ ifina5300S0DTOin c=new ifina5300S0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina5300S0DTOin in=(ifina5300S0DTOin)src;
      keyword=in.keyword; rootType=in.rootType; rootId=in.rootId; fromTypeCd=in.fromTypeCd; fromId=in.fromId;
      toTypeCd=in.toTypeCd; toId=in.toId; relationTypeCd=in.relationTypeCd; criticalYn=in.criticalYn;
      depth=in.depth; pageNo=in.pageNo; pageSize=in.pageSize; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("keyword", FieldProperty.builder().setPhysicalName("keyword").setLogicalName("keyword").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
