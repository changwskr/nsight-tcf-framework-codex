package nhnis.infra.in.a.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina4300S0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String keyword, sourceCd; private Integer maxDaysLeft, pageNo, pageSize;
    public String getKeyword(){return keyword;} public void setKeyword(String v){keyword=v;}
    public String getSourceCd(){return sourceCd;} public void setSourceCd(String v){sourceCd=v;}
    public Integer getMaxDaysLeft(){return maxDaysLeft;} public void setMaxDaysLeft(Integer v){maxDaysLeft=v;}
    public Integer getPageNo(){return pageNo;} public void setPageNo(Integer v){pageNo=v;}
    public Integer getPageSize(){return pageSize;} public void setPageSize(Integer v){pageSize=v;}
    @Override public Object clone(){ ifina4300S0DTOin c=new ifina4300S0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina4300S0DTOin in=(ifina4300S0DTOin)src;
      keyword=in.keyword; sourceCd=in.sourceCd; maxDaysLeft=in.maxDaysLeft; pageNo=in.pageNo; pageSize=in.pageSize; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("keyword", FieldProperty.builder().setPhysicalName("keyword").setLogicalName("keyword").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}
